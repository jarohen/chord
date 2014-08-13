(ns chord.ajax
  (:require [cljs.core.async :as a]
            [clojure.string :as s]
            [goog.events :as e]
            [goog.net.EventType :as et])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:import [goog.net XhrIo]))

(defn request [url & [{:keys [method]}]]
  (let [resp-ch (a/chan)]
    (go
      (let [xhr (goog.net.XhrIo.)]

        (e/listen xhr et/COMPLETE
                  (fn [e]
                    (let [resp {:status (.getStatus xhr)
                                :headers (->> (for [[k v] (js->clj (.getResponseHeaders xhr))]
                                                [(keyword (s/lower-case k)) v])
                                              (into {}))
                                :body (.getResponseText xhr)}]
                      (go
                        (a/>! resp-ch resp)
                        (a/close! resp-ch)))))
        
        (.send xhr url (s/upper-case (name method)))))
    resp-ch))

(defn get [url & [opts]]
  (request url (assoc opts :method :get)))

(defn post [url & [opts]]
  (request url (assoc opts :method :post)))

(defn put [url & [opts]]
  (request url (assoc opts :method :put)))

(defn delete [url & [opts]]
  (request url (assoc opts :method :delete)))

(defn head [url & [opts]]
  (request url (assoc opts :method :head)))
