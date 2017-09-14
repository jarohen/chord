(ns chord.channels
  (:require #?(:clj [org.httpkit.server :as http])

            #?(:clj
               [clojure.core.async :refer [chan <! >! put! close! go-loop]]
               :cljs
               [cljs.core.async :refer [chan put! close! <! >!]])

            #?(:clj
               [clojure.core.async.impl.protocols :as p]
               :cljs
               [cljs.core.async.impl.protocols :as p])

            [chord.format :as cf])

  #?(:cljs (:require-macros [cljs.core.async.macros :refer [go-loop]])))

(defn read-from-ws! [ws ch]
  #?(:clj
     (http/on-receive ws #(put! ch {:message %}))

     :cljs
     (set! (.-onmessage ws)
           (fn [ev]
             (let [message (.-data ev)]
               (put! ch {:message message}))))))

(defn write-to-ws! [ws ch]
  (go-loop []
    (let [msg (<! ch)]
      (when msg
        #?(:clj
           (http/send! ws msg)

           :cljs
           (.send ws msg))
        (recur)))))

(defn bidi-ch [read-ch write-ch & [{:keys [on-close]}]]
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
      (when on-close
        (on-close)))))

(defn- on-close [ws ws-ch]
  #?(:clj  (http/on-close ws (fn [_] (close! ws-ch)))
     :cljs (.on ws "close" #(close! ws-ch))))

(defn wrap-websocket [socket {:keys [read-ch write-ch] :as opts} & [cleanup]]
  (let [{:keys [read-ch write-ch]}
        (-> {:read-ch (or read-ch (chan))
             :write-ch (or write-ch (chan))}
            (cf/wrap-format (dissoc opts :read-ch :write-ch)))]

    (read-from-ws! socket read-ch)
    (write-to-ws! socket write-ch)

    (let [ws-ch (bidi-ch read-ch write-ch {:on-close cleanup})]
      (on-close socket ws-ch)
      ws-ch)))
