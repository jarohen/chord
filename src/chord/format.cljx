(ns chord.format
  (:require #+clj [clojure.core.async :as a :refer [chan <! >! put! close! go-loop]]
            #+cljs [cljs.core.async :as a :refer [chan put! close! <! >!]]

            #+clj [clojure.tools.reader.edn :as edn]
            #+cljs [cljs.reader :as edn]

            [clojure.walk :refer [keywordize-keys]]

            #+clj [cheshire.core :as json])

  #+cljs (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(defn try-read [read-fn]
  (fn [{:keys [error message] :as data}]
    (if error
      data

      (try
        {:message (read-fn message)}
        (catch #+clj Exception, #+cljs js/Error e
          {:error :invalid-format
           :cause e
           :invalid-msg message})))))

(defmulti wrap-format
  (fn [chs format]
    format))

(defmethod wrap-format :edn [{:keys [read-ch write-ch]} _]
  {:read-ch (a/map< (try-read edn/read-string) read-ch)
   :write-ch (a/map> pr-str write-ch)})

(defmethod wrap-format :json [{:keys [read-ch write-ch]} _]
  {:read-ch (a/map< (try-read #+clj json/decode
                              #+cljs (comp js->clj js/JSON.parse))
                    read-ch)
   
   :write-ch (a/map> #+clj json/encode
                     #+cljs (comp js/JSON.stringify clj->js)
                     
                     write-ch)})

(defmethod wrap-format :json-kw [chs _]
  (update-in (wrap-format chs :json) [:read-ch] #(a/map< keywordize-keys %)))

(defmethod wrap-format :str [chs _]
  chs)

(defmethod wrap-format nil [chs _]
  (wrap-format chs :edn))

(defmethod wrap-format :default [chs format]
  (throw (str "ERROR: Invalid Chord channel format: " format)))
