(ns chord.node
  (:require [cljs.nodejs :as nodejs]
            [chord.channels :as channels]
            [chord.format :as format]))

(defn ws-server
  "Arguments:
    handler: a 2-arg fn accepting the ws-channel and details of the request
    opts:
     - read-ch         - channel to use for reading the websocket
     - write-ch        - channel to use for writing to the websocket
     - format          - data format to use on the channel
                         either :edn (default), :json, :json-kw, :transit-json, or :str.
     - ws-lib-opts     - options to pass to 'ws', the underlying node library.
                         see https://github.com/websockets/ws for more details"
  [handler {:keys [read-ch write-ch ws-lib-opts] :as opts}]

  (if-let [ws (nodejs/require "ws")]
    (let [server (ws.Server. (clj->js ws-lib-opts))]
      (.on server "connection"
           (fn [socket request]
             (let [ws-ch (channels/wrap-websocket socket opts)]
               (handler ws-ch request))))
      server)

    (throw (js/Error. "Failed to load ws (nodejs websocket library). Make sure it's installed."))))
