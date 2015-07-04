(ns chord.channels
  (:require #?(:clj [org.httpkit.server :as http])

            #?(:clj
               [clojure.core.async :refer [chan <! >! put! close! go-loop]]
               :cljs
               [cljs.core.async :refer [chan put! close! <! >!]])

            #?(:clj
               [clojure.core.async.impl.protocols :as p]
               :cljs
               [cljs.core.async.impl.protocols :as p]))

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
