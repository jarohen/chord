(ns chord.example.front-end
  (:require [chord.client :refer [ws-ch]]
            [chord.example.message-list :refer [message-component]]
            [cljs.core.async :refer [chan <! >! put! close! timeout]]
            [cljs.reader :as edn]
            [clojure.string :as s]
            [flow.core :as f :include-macros true]
            [chord.http :as ajax]
            simple-brepl.client)
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(enable-console-print!)

(defn add-msg [msgs new-msg]
  ;; we keep the most recent 10 messages
  (->> (cons new-msg msgs)
       (take 10)))

(defn receive-msgs! [!msgs server-ch]
  ;; every time we get a message from the server, add it to our list
  (go-loop []
    (when-let [msg (<! server-ch)]
      (swap! !msgs add-msg msg)
      (recur))))

(defn send-msgs! [new-msg-ch server-ch]
  ;; send all the messages to the server
  (go-loop []
    (when-let [msg (<! new-msg-ch)]
      (>! server-ch msg)
      (recur))))

(set! (.-onload js/window)
      (fn []
        (go
          (-> (<! (ajax/post "/ajax"
                             {:query-params {:a 1 :b 2}
                              :req-format :json-kw
                              :body {:a 3 :b 4}
                              :basic-auth ["james" "password-123"]
                              :headers {:authorization "abc123"}}))
              clj->js
              js/console.log))
        
        (go
          (let [{:keys [ws-channel error]} (<! (ws-ch "ws://localhost:3000/ws"
                                                      {:format :transit-json}))]

            (if error
              ;; connection failed, print error
              (f/root js/document.body
                (f/el
                  [:div
                   "Couldn't connect to websocket: "
                   (pr-str error)]))

              (let [ ;; !msgs is a shared atom between the model (above,
                    ;; handling the WS connection) and the view
                    ;; (message-component, handling how it's rendered)
                    !msgs (doto (atom [])
                            (receive-msgs! ws-channel))

                    ;; new-msg-ch is the feedback loop from the view -
                    ;; any messages that the view puts on here are to
                    ;; be sent to the server
                    new-msg-ch (doto (chan)
                                 (send-msgs! ws-channel))]

                ;; show the message component
                (f/root js/document.body
                  (f/el
                    [message-component !msgs new-msg-ch]))))))))

