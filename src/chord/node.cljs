(ns chord.node
  (:require [cljs.nodejs :as nodejs]
            [chord.channels :as channels]
            [chord.format :as format]))

(defn ws-server [handler {:keys [read-ch write-ch ws-opts] :as opts}]
  (if-let [ws (nodejs/require "ws")]
    (let [server (ws.Server. (clj->js ws-opts))]
      (.on server "connection"
           (fn [socket request]
             (let [ws-ch (channels/wrap-websocket socket opts)]
               (handler ws-ch request))))
      server)

    (throw (js/Error. "Failed to load ws (nodejs websocket library). Make sure it's installed."))))
