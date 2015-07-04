(ns chord.example.message-list
  (:require [cljs.core.async :refer [put!]]
            [cljs.reader :as edn]
            [reagent.core :as r]))

(defn try-read-edn [s]
  (try
    (let [edn (edn/read-string s)]
      (if (symbol? edn)
        s
        edn))
    (catch js/Error _ s)))

(defn message-box [new-msg-ch]
  (let [!input-value (atom nil)]
    [:div
     [:h3 "Send a message to the server: (either EDN or raw string)"]
     [:input {:type "text",
              :size 50,
              :autofocus true
              :value @!input-value
              :on-key-down (juxt (fn [e]
                                   (reset! !input-value (.-value (.-target e))))
                                 (fn [e]
                                   (when (= 13 (.-keyCode e))
                                     (put! new-msg-ch (try-read-edn @!input-value))
                                     (reset! !input-value ""))))}]]))

(defn message-list [!msgs]
  [:div
   [:h3 "Messages from the server:"]
   [:ul
    (if-let [msgs (seq @!msgs)]
      (for [msg msgs]
        ^{:key msg} [:li (pr-str msg)])

      [:li "None yet."])]])

(defn message-component [!msgs new-msg-ch]
  [:div
   [message-box new-msg-ch]
   [message-list !msgs]])
