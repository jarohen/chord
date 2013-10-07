(ns chord.client
  (:require [cljs.core.async :refer [chan <! >! put! close!]]
            [cljs.core.async.impl.protocols :as p])
  (:require-macros [cljs.core.async.macros :refer (go)]))

(defn- make-read-ch [ws]
  (let [ch (chan)]
    (set! (.-onmessage ws)
          (fn [ev]
            (let [message (.-data ev)]
              (put! ch {:message message}))))
    ch))

(defn- make-write-ch [ws]
  (let [ch (chan)]
    (go
     (loop []
       (let [msg (<! ch)]
         (when msg
           (.send ws msg)
           (recur)))))
    ch))

(defn- make-open-ch [ws v]
  (let [ch (chan)]
    (set! (.-onopen ws)
          #(do
             (put! ch v)
             (close! ch)))
    ch))

(defn- on-error [ws read-ch]
  (set! (.-onerror ws)
        (fn [ev]
          (let [error (.-data ev)]
            (put! read-ch {:error error})))))

(defn- on-close [ws read-ch write-ch]
  (set! (.-onclose ws)
        (fn []
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
      (.close ws))))

(defn ws-ch [ws-url]
  (let [web-socket (js/WebSocket. ws-url)
        read-ch (make-read-ch web-socket)
        write-ch (make-write-ch web-socket)
        combined-ch (combine-chs web-socket read-ch write-ch)
        socket-ch (make-open-ch web-socket combined-ch)]
    
    (on-error web-socket read-ch)
    (on-close web-socket read-ch write-ch)
    socket-ch))


