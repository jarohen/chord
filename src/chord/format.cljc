(ns chord.format
  (:require #?(:clj [cheshire.core :as json])

            #?(:clj [clojure.core.async :as a]
               :cljs [cljs.core.async :as a])

            #?(:clj [clojure.java.io :as io])

            #?(:clj [clojure.tools.reader.edn :as edn]
               :cljs [cljs.reader :as edn])

            [clojure.walk :refer [keywordize-keys]]
            [cognitect.transit :as transit])

  #?(:clj (:import [java.io ByteArrayOutputStream ByteArrayInputStream])))

(defprotocol ChordFormatter
  (freeze [_ obj])
  (thaw [_ s]))

(defmulti formatter* :format)

(defmethod formatter* :edn [_]
  (reify ChordFormatter
    (freeze [_ obj]
      (pr-str obj))

    (thaw [_ s]
      (edn/read-string s))))

(defmethod formatter* :json [_]
  (reify ChordFormatter
    (freeze [_ obj]
      #?(:clj (json/encode obj))
      #?(:cljs (js/JSON.stringify (clj->js obj))))

    (thaw [this s]
      #?(:clj (json/decode s))
      #?(:cljs (js->clj (js/JSON.parse s))))))

(defmethod formatter* :json-kw [opts]
  (let [json-formatter (formatter* (assoc opts :format :json))]
    (reify ChordFormatter
      (freeze [_ obj]
        (freeze json-formatter obj))

      (thaw [_ s]
        (keywordize-keys (thaw json-formatter s))))))

(defmethod formatter* :transit-json [_]
  (reify ChordFormatter
    (freeze [_ obj]
      #?(:clj
       (let [baos (ByteArrayOutputStream.)]
         (transit/write (transit/writer baos :json) obj)
         (.toString baos)))

      #?(:cljs
       (transit/write (transit/writer :json) obj)))

    (thaw [_ s]
      #?(:clj
       (let [bais (ByteArrayInputStream. (.getBytes s))]
         (transit/read (transit/reader bais :json))))

      #?(:cljs
       (transit/read (transit/reader :json) s)))))

(defmethod formatter* :str [_]
  (reify ChordFormatter
    (freeze [_ obj]
      obj)

    (thaw [_ s]
      s)))

(defn formatter [opts]
  (formatter* (if (keyword? opts)
                {:format opts}
                opts)))

(defn wrap-format [{:keys [read-ch write-ch]} {:keys [format] :as opts}]
  (let [fmtr (formatter (if format
                          opts
                          {:format :edn}))]

    ;; TODO need to replace a/map< etc with transducers when 1.7.0 is
    ;; released

    {:read-ch (a/map< (fn [{:keys [message]}]
                        (try
                          (when message
                            {:message (thaw fmtr message)})
                          (catch #?(:clj Exception, :cljs js/Error) e
                                 {:error :invalid-format
                                  :cause e
                                  :invalid-msg message})))
                      read-ch)

     :write-ch (a/map> #(when %
                          (freeze fmtr %))
                       write-ch)}))
