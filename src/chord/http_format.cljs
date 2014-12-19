(ns chord.http-format
  (:require [clojure.walk :refer [keywordize-keys]]
            [clojure.string :as s]
            [cljs.reader :refer [read-string]]
            [chord.format :as cf]))

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

(defn with-parsed-body [resp req]
  (let [fmtr (cf/formatter (or (:resp-format req)
                               (-> (mime-type resp)
                                   mime-type->format)
                               {:format :str}))]
    (update-in resp [:body] #(cf/thaw fmtr %) fmtr)))

(defn with-formatted-body [req]
  (let [fmt (or (:req-format req) :str)
        fmtr (cf/formatter fmt)]
    (-> req
        (assoc-in [:headers :content-type] (or (format->mime-type fmt) fmt))
        (update-in [:body] #(cf/freeze fmtr %) fmt))))
