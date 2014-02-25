(ns chord.example
  (:require [ring.util.response :refer [response]]
            [compojure.core :refer [defroutes GET]]
            [compojure.route :refer [resources]]
            [chord.http-kit :refer [with-channel]]
            [clojure.core.async :refer [<! >! put! close! go-loop]]
            [hiccup.page :refer [html5 include-js]]))

(defn page-frame []
  (html5
   [:head
    [:title "Chord Example"]
    (include-js "/js/chord-example.js")]
   [:body [:div#content]]))

(defn ws-handler [req]
  (with-channel req ws
    (println "Opened connection from" (:remote-addr req))
    (go-loop []
      (when-let [{:keys [message error] :as msg} (<! ws)]
        (prn "Message received:" msg)
        (>! ws (if error
                 (format "Error: '%s'." (pr-str msg))
                 (format "You passed: '%s' at %s." (pr-str message) (java.util.Date.))))
        (recur)))))

(defroutes app-routes
  (GET "/" [] (response (page-frame)))
  (GET "/ws" [] ws-handler)
  (resources "/js" {:root "js"}))

(def app
  #'app-routes)
