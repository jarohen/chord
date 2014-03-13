(ns chord.http-kit
  (:require [org.httpkit.server :as http]
            [clojure.core.async :as a :refer [chan <! >! put! close! go-loop]]
            [clojure.core.async.impl.protocols :as p]
            [clojure.tools.reader.edn :as edn]
            [cheshire.core :as json]
            [clojure.set :refer [rename-keys]]))

(defn- read-from-ch! [ch ws]
  (http/on-receive ws #(put! ch {:message %})))

(defn- write-to-ch! [ch ws]
  (go-loop []
    (let [msg (<! ch)]
      (when msg
        (http/send! ws msg)
        (recur)))))

(defn- on-close [ws read-ch write-ch]
  (http/on-close ws
                 (fn [_]
                   ;; TODO support status?
                   (close! read-ch)
                   (close! write-ch))))

(defn- combine-chs [ws read-ch write-ch]
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
      (http/close ws))))

(defmulti wrap-format
  (fn [chs format] format))

(defn try-read-edn [{:keys [message]}]
  (try
    {:message (edn/read-string message)}
    (catch Exception e
      {:error :invalid-edn
       :invalid-msg message})))

(defmethod wrap-format :edn [{:keys [read-ch write-ch]} _]
  {:read-ch (a/map< try-read-edn read-ch)
   :write-ch (a/map> pr-str write-ch)})

(defn try-read-json
  [{:keys [message]}]
  (try
    {:message (json/parse-string message)}
    (catch Exception e
      {:error :invalid-json
       :invalid-msg message})))

(defmethod wrap-format :json
  [{:keys [read-ch write-ch]} _]
  {:read-ch (a/map< try-read-json read-ch)
   :write-ch (a/map> json/generate-string write-ch)})

(defmethod wrap-format :str [chs _]
  chs)

(defmethod wrap-format nil [chs _]
  (wrap-format chs :edn))

(defn core-async-ch [httpkit-ch {:keys [read-ch write-ch format]}]
  (let [{:keys [read-ch write-ch]} (-> {:read-ch (or read-ch (chan))
                                        :write-ch (or write-ch (chan))}
                                       (wrap-format format))]

    (read-from-ch! read-ch httpkit-ch)
    (write-to-ch! write-ch httpkit-ch)
    (on-close httpkit-ch read-ch write-ch)
    
    (combine-chs httpkit-ch read-ch write-ch)))

(defmacro with-channel
  "Extracts the websocket from the request and binds it to 'ch-name' in the body
   Arguments:
    req         - (required) HTTP-kit request map
    ch-name     - (required) variable to bind the channel to in the body
    opts        - (optional) map to configure reading/writing channels
      :read-ch  - (optional) (possibly buffered) channel to use for reading the websocket
      :write-ch - (optional) (possibly buffered) channel to use for writing to the websocket
      :format   - (optional, default :edn) data format to use on the channel, (at the moment) either :edn, :json or :str.

   Usage:
    (require '[clojure.core.async :as a])

    (with-channel req the-ws
      (a/go-loop []
        (when-let [msg (a/<! the-ws)]
          (println msg)
          (recur))))

    (with-channel req the-ws
      {:read-ch (a/chan (a/sliding-buffer 10))
       :write-ch (a/chan (a/dropping-buffer 5))}

      (go-loop []
        (when-let [msg (<! the-ws)]
          (println msg)
          (recur))))"
  
  [req ch-name & [opts & body]]
  (let [opts? (and (map? opts) (seq body))
        body (cond->> body
               (not opts?) (cons opts))
        opts (when opts? opts)]
    `(http/with-channel ~req httpkit-ch#
       (let [~ch-name (core-async-ch httpkit-ch# ~opts)]
         ~@body))))

(defn wrap-websocket-handler
  "Middleware that puts a :ws-channel key on the request map for
   websocket requests.

   Arguments:
    handler - (required) Ring-compatible handler
    opts    - (optional) Options for the WebSocket channel - same options as for `with-channel`"
  [handler & [opts]]
  
  (fn [req]
    (if (:websocket? req)
      (with-channel req ws-conn
        (or opts {})
        (handler (assoc req :ws-channel ws-conn)))
      (handler req))))

(comment
  (defn handler [{:keys [ws-channel] :as req}]
    (go-loop []
      (if-let [{:keys [message]} (<! ws-channel)]
        (do
          (prn {:message message})
          (>! ws-channel (str "You said: " message))
          (recur))
        (prn "closed."))))

  (server)

  (def server (http/run-server (-> #'handler wrap-websocket-handler) {:port 3000})))
