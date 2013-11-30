(ns chord.http-kit
  (:require [org.httpkit.server :as http]
            [clojure.core.async :refer [chan <! >! put! close! go-loop]]
            [clojure.core.async.impl.protocols :as p]))

(defn- make-read-ch [ws]
  (let [ch (chan)]
    (http/on-receive ws #(put! ch {:message %}))
    ch))

(defn- make-write-ch [ws]
  (let [ch (chan)]
    (go
     (loop []
       (let [msg (<! ch)]
         (when msg
           (http/send! ws msg)
           (recur)))))
    ch))

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

(defn core-async-ch [httpkit-ch]
  (let [read-ch (make-read-ch httpkit-ch)
        write-ch (make-write-ch httpkit-ch)
        combined-ch (combine-chs httpkit-ch read-ch write-ch)]
     
    (on-close httpkit-ch read-ch write-ch)
    combined-ch))

(defmacro with-channel [req ch-name & body]
  `(http/with-channel ~req httpkit-ch#
     (let [~ch-name (core-async-ch httpkit-ch#)]
       ~@body)))

(comment
  (defn handler [req]
    (with-channel req ch
      (go-loop []
        (if-let [{:keys [message]} (<! ch)]
          (do
            (prn {:message message})
            (>! ch (str "You said: " message))
            (recur))
          (prn "closed.")))))

  (defonce server (http/run-server #'handler {:port 3000})))
