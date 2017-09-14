(ns chord.node
  (:require [cljs.nodejs :as nodejs]
            [chord.channels :as channels]
            [chord.format :as format]))

(defn ws-server [ws-opts {:keys [read-ch write-ch :as chord-opts]} handler]
  (when-let [ws (nodejs/require "ws")]
    (let [server (ws.Server. (clj->js ws-opts))]
      (.on server "connection"
           (fn [socket request]
             (let [ws-ch (channels/wrap-websocket socket chord-opts #(.close socket))]
               (handler ws-ch request))))
      server)))

(comment
  "Plain example"

  (defn handler [ws-ch req]
    (if (authorized? req)
      (async/put! ws-ch "Welcome!")
      (do
        (async/put! ws-ch "Denied!")
        (async/close! ws-ch)))
    (async/go
      (println (async/<! ws-ch))
      '...))

  (def server (chord.node/ws-server {:port 8080} {:format :edn}) handler)

  "Express Example"

  (def express (nodejs/require "express"))
  (def https (nodejs/require "https"))
  (def app (express))
  '...

  ;; Note: SSL is inherited from the webserver, so using http gives you ws, and
  ;; https gives you wss.
  (def server (.createServer https app))

  (def ws-server
    (chord.node/ws-server server {:format :json} handler)))
