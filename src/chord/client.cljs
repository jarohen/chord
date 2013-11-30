(ns chord.client
  (:require [cljs.core.async :refer [chan <! >! put! close! sliding-buffer dropping-buffer]]
            [cljs.core.async.impl.protocols :as p])
  (:require-macros [cljs.core.async.macros :refer (go)]))

(defn- make-channel [{:keys [type size]
                      :or {type :unbuffered}}]
  (case type
    :fixed (chan size)
    :sliding (chan (sliding-buffer size))
    :dropping (chan (dropping-buffer size))
    :unbuffered (chan)))

(defn- make-read-ch [ws opts]
  (let [ch (make-channel opts)]
    (set! (.-onmessage ws)
          (fn [ev]
            (let [message (.-data ev)]
              (put! ch {:message message}))))
    ch))

(defn- make-write-ch [ws opts]
  (let [ch (make-channel opts)]
    (go
     (loop []
       (let [msg (<! ch)]
         (when msg
           (.send ws msg)
           (recur)))))
    ch))

(defn- make-open-ch [ws v]
  (let [ch (make-channel nil)]
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

(defn ws-ch
  "Creates websockets connection and returns 2-sided channel.
   Arguments:
    ws-url           - (required) link to websocket service
    :reading-buffer  - (optional) hash-map with settings for reading channel
    :writing-buffer  - (optional) hash-map with settings for writing channel

    supported keys for channel's options:

    * type - type of channel's buffer [:fixed :sliding :dropping :unbuffered]
    * size - size of buffer, default core.async.impl/MAX-QUEUE-SIZE

   Usage:
    (ws-ch \"ws://127.0.0.1:6437\")
    (ws-ch \"ws://127.0.0.1:6437\" {:reading-buffer {:type :sliding}})
    (ws-ch \"ws://127.0.0.1:6437\" {:reading-buffer {:type :sliding}
                                    :writing-buffer {:type :dropping :size 10}})"
  
  [ws-url & [{:keys [reading-buffer writing-buffer]}]]
  
  (let [web-socket (js/WebSocket. ws-url)
        read-ch (make-read-ch web-socket reading-buffer)
        write-ch (make-write-ch web-socket writing-buffer)
        combined-ch (combine-chs web-socket read-ch write-ch)
        socket-ch (make-open-ch web-socket combined-ch)]

    (on-error web-socket read-ch)
    (on-close web-socket read-ch write-ch)
    socket-ch))
