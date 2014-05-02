(ns chord.client
  (:require [cljs.core.async :as a :refer [chan <! >! put! close!]]
            [cljs.core.async.impl.protocols :as p]
            [cljs.reader :as edn]
            [clojure.walk :refer [keywordize-keys]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(defn- read-from-ch! [ch ws]
  (set! (.-onmessage ws)
        (fn [ev]
          (let [message (.-data ev)]
            (put! ch {:message message})))))

(defn- write-to-ch! [ch ws]
  (go-loop []
    (let [msg (<! ch)]
      (when msg
        (.send ws msg)
        (recur)))))

(defn- combine-chs [read-ch write-ch & [close-fn]]
  (reify
    p/ReadPort
    (take! [_ handler]
      (p/take! read-ch handler))

    p/WritePort
    (put! [_ msg handler]
      (p/put! write-ch msg handler))

    p/Channel
    (close! [_]
      (p/close! read-ch)
      (p/close! write-ch)
      (when close-fn
        (close-fn)))))

(defn- on-error [ws]
  (set! (.-onerror ws)
        (fn [ev]
          (set! (.-error-seen ws) (or (.-data ev) true)))))

(defn- on-close [ws read-ch write-ch & [err-meta-channel]]
  (set! (.-onclose ws)
        (fn [ev]
          (go ;; using a go block to defer close until put completes
            (let [error-seen? (.-error-seen ws)]
              (when (or error-seen?
                        (not (.-wasClean ev)))
                (let [error-desc {:error (.-reason ev)
                                  :code (.-code ev)
                                  :wasClean (.-wasClean ev)}]
                  (when err-meta-channel
                    (>! err-meta-channel
                        (combine-chs
                         (go error-desc)
                         (doto (chan) (close!)))))
                  (>! read-ch error-desc)))
              (close! read-ch)
              (close! write-ch))))))

(defn- make-open-ch [ws read-ch write-ch v]
  (let [ch (chan)]
    (on-error ws)
    (on-close ws read-ch write-ch ch)
    (set! (.-onopen ws)
          #(go ;; using a go block to defer close until put completes
             (>! ch v)
             (close! ch)))
    ch))

(defmulti wrap-format
  (fn [chs format] format))

(defn try-read-edn [{:keys [error message] :as data}]
  (if error
    data
    (try
      {:message (edn/read-string message)}
      (catch js/Error e
        {:error :invalid-edn
         :invalid-msg message}))))

(defmethod wrap-format :edn [{:keys [read-ch write-ch]} _]
  {:read-ch (a/map< try-read-edn read-ch)
   :write-ch (a/map> pr-str write-ch)})

(defn try-read-json [{:keys [error message] :as data}]
  (if error
    data
    (try
      {:message (-> message js/JSON.parse js->clj)}
      (catch js/Error e
        {:error :invalid-json
         :invalid-msg message}))))

(defmethod wrap-format :json [{:keys [read-ch write-ch]} _]
  {:read-ch (a/map< try-read-json read-ch)
   :write-ch (a/map> (comp js/JSON.stringify clj->js) write-ch)})

(defmethod wrap-format :json-kw [chs _]
  (update-in (wrap-format chs :json) [:read-ch] #(a/map< keywordize-keys %)))

(defmethod wrap-format :str [chs _]
  chs)

(defmethod wrap-format nil [chs _]
  (wrap-format chs :edn))

(defmethod wrap-format :default [chs format]
  (throw (str "ERROR: Invalid Chord channel format: " format)))

(defn ws-ch
  "Creates websockets connection and returns a 2-sided channel when the websocket is opened.
   Arguments:
    ws-url           - (required) link to websocket service
    opts             - (optional) map to configure reading/writing channels
      :read-ch       - (optional) (possibly buffered) channel to use for reading the websocket
      :write-ch      - (optional) (possibly buffered) channel to use for writing to the websocket
      :format        - (optional) data format to use on the channel, (at the moment)
                                  either :edn (default), :json, :json-kw or :str.

   Usage:
    (:require [cljs.core.async :as a])


    (a/<! (ws-ch \"ws://127.0.0.1:6437\"))

    (a/<! (ws-ch \"ws://127.0.0.1:6437\" {:read-ch (a/chan (a/sliding-buffer 10))}))

    (a/<! (ws-ch \"ws://127.0.0.1:6437\" {:read-ch (a/chan (a/sliding-buffer 10))
                                          :write-ch (a/chan (a/dropping-buffer 10))}))"
  
  [ws-url & [{:keys [read-ch write-ch format]}]]
  
  (let [web-socket (js/WebSocket. ws-url)
        {:keys [read-ch write-ch]} (-> {:read-ch (or read-ch (chan))
                                        :write-ch (or write-ch (chan))}
                                       (wrap-format format))]
    (read-from-ch! read-ch web-socket)
    (write-to-ch! write-ch web-socket)
    (->> (combine-chs read-ch write-ch #(.close web-socket))
         (make-open-ch web-socket read-ch write-ch))))
