(ns chord.example
  (:require [ring.util.response :refer [response]]))

(defn app [req]
  (response "Hello world!"))
