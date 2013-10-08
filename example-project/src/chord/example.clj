(ns chord.example
  (:require [ring.util.response :refer [response]]
            [compojure.core :refer [defroutes GET]]
            [compojure.route :refer [resources]]
            [chord.http-kit :refer [with-channel]]
            [clojure.core.async :refer [<! >! put! close! go]]
            [hiccup.page :refer [html5 include-js]]))

(defn page-frame []
  (html5
   [:head
    [:title "Chord Example"]
    (include-js "/js/chord-example.js")]
   [:body [:div#content]]))

(defn ws-handler [req]
  (with-channel req ws
    (go
     (let [{:keys [message]} (<! ws)]
       (println "Message received:" message)
       (>! ws (format "You said: '%s'" message))
       (Thread/sleep 500)
       (close! ws)))))

(defroutes app
  (GET "/" [] (response (page-frame)))
  (GET "/ws" [] ws-handler)
  (resources "/js" {:root "js"}))
