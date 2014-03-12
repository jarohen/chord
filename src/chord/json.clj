(ns chord.json
  "Extends chord.http-kit serialisation to JSON"
  (:require [cheshire.core :as json]
            chord.http-kit
            [clojure.core.async :as a]))

(defn try-read-json
  [{:keys [message]}]
  (try
    {:message (json/parse-string message)}
    (catch Exception e
      {:error :invalid-json
       :invalid-msg message})))

(defmethod chord.http-kit/wrap-format :json
  [{:keys [read-ch write-ch]} _]
  {:read-ch (a/map< try-read-json read-ch)
   :write-ch (a/map> json/generate-string write-ch)})
