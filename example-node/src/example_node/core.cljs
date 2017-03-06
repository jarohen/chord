(ns example-node.core
  (:require-macros
      [cljs.core.async.macros :refer (go go-loop)])
  (:require [cljs.nodejs :as nodejs]
            [chord.client :refer [ws-ch]]
            [cljs.core.async :refer [take! put! chan <! >! timeout close!]]))

(nodejs/enable-util-print!)

(defn echo-test []
  (go
    (let [{:keys [ws-channel error]} (<! (ws-ch "wss://echo.websocket.org"))]
      (if-not error
        (do (>! ws-channel "Hello server from client!")
            (let [{:keys [message error]} (<! ws-channel)]
              (if-not error
                (do
                  (println "Got:" message)
                  (close! ws-channel))
                (println "Error:" (pr-str error)))))
        (println "Error:" (pr-str error))))))

(defn -main [& args]
  (println "Starting test")
  (echo-test))

(set! *main-cli-fn* -main)
