(ns chord.example-client
  (:require [chord.client :refer [ws-ch]]
            [cljs.core.async :refer [chan <! >! put! close! timeout]]
            [dommy.core :as d]
            [cljs.reader :as edn]
            [clidget.widget :refer-macros [defwidget]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [dommy.macros :refer [node sel1]]))

(enable-console-print!)

(defn try-read-edn [s]
  (try
    (let [edn (edn/read-string s)]
      (if (symbol? edn)
        s
        edn))
    (catch js/Error _ s)))

(defn with-enter-handler [$text-box new-msg-ch]
  (d/listen! $text-box :keyup
    (fn [e]
      (when (= 13 (.-keyCode e))
        (put! new-msg-ch (try-read-edn (d/value $text-box)))
        (d/set-value! $text-box "")))))

(defn message-box [new-msg-ch]
  (node
   [:div
    [:h3 "Send a message to the server: (either EDN or raw string)"]
    (-> (node [:input {:type :text, :size 50, :autofocus true}])
        (with-enter-handler new-msg-ch))]))

(defwidget message-list [{:keys [msgs]}]
  (node
   [:div
    [:h3 "Messages from the server:"]
    [:ul
     (if (seq msgs)
       (for [msg msgs]
         [:li (pr-str msg)])
       [:li "None yet."])]]))

(defn add-msg [msgs new-msg]
  (->> (cons new-msg msgs)
       (take 10)))

(defn receive-msgs! [!msgs server-ch]
  (go-loop []
    (when-let [msg (<! server-ch)]
      (swap! !msgs add-msg msg)
      (recur))))

(defn send-msgs! [new-msg-ch server-ch]
  (go-loop []
    (when-let [msg (<! new-msg-ch)]
      (>! server-ch msg)
      (recur))))

(set! (.-onload js/window)
      (fn []
        (go
          (let [server-ch (<! (ws-ch "ws://localhost:3000/ws" {:format :json-kw}))
              
                !msgs (doto (atom [])
                        (receive-msgs! server-ch))
              
                new-msg-ch (doto (chan)
                             (send-msgs! server-ch))]
          
            (d/replace-contents! (sel1 :#content)
                                 (node
                                  [:div
                                   (message-box new-msg-ch)
                                   (message-list {:!msgs !msgs})]))))))




