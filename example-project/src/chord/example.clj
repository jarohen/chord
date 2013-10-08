(ns chord.example
  (:require [ring.util.response :refer [response]]
            [compojure.core :refer [defroutes GET]]
            [compojure.route :refer [resources]]))

(defroutes app
  (GET "/" [] (response "Hello world!"))
  (resources "/js" {:root "js"}))

(clojure.java.io/resource "js/chord-example.js")
