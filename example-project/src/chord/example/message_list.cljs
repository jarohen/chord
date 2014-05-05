(ns chord.example.message-list
  (:require [cljs.core.async :refer [put!]]
            [dommy.core :as d]
            [cljs.reader :as edn]
            [clidget.widget :refer-macros [defwidget]])
  (:require-macros [dommy.macros :refer [node]]))

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
    (-> (node [:input {:type "text", :size 50, :autofocus true}])
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

(defn message-component [!msgs new-msg-ch]
  (node
   [:div
    (message-box new-msg-ch)
    (message-list {:!msgs !msgs})]))
