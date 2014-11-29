(ns chord.http-format
  (:require [clojure.walk :refer [keywordize-keys]]
            [clojure.string :as s]
            [cljs.reader :refer [read-string]]))

(def mime-type->format
  {"application/edn" :edn
   "application/json" :json-kw})

(def format->mime-type
  {:edn "application/edn"
   :json-kw "application/json"
   :json "application/json"
   :str "text/plain"})

(defn mime-type [resp]
  (when-let [content-type (get-in resp [:headers :content-type])]
    (first (s/split content-type #";"))))

(defmulti parse-body
  (fn [body fmt]
    fmt))

(defmethod parse-body :edn [body _]
  (read-string body))

(defmethod parse-body :json-kw [body _]
  (-> (parse-body body :json)
      keywordize-keys))

(defmethod parse-body :json [body _]
  (js/JSON.parse body))

(defmethod parse-body :str [body _]
  body)

(defmethod parse-body :default [body _]
  body)

(defn with-parsed-body [resp req]
  (let [fmt (or (:resp-format req)
                (-> (mime-type resp)
                    mime-type->format)
                :str)]
    (update-in resp [:body] parse-body fmt)))

(defmulti format-body
  (fn [body fmt]
    fmt))

(defmethod format-body :edn [body _]
  (pr-str body))

(defmethod format-body :json [body _]
  (js/JSON.stringify (clj->js body)))

(defmethod format-body :json-kw [body _]
  (js/JSON.stringify (clj->js body)))

(defmethod format-body :default [body _]
  body)

(defn with-formatted-body [req]
  (let [fmt (or (:req-format req) :str)]
    (-> req
        (assoc-in [:headers :content-type] (or (format->mime-type fmt) fmt))
        (update-in [:body] format-body fmt))))
