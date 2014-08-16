(ns chord.http
  (:refer-clojure :exclude [get])
  (:require [chord.http-format :refer [with-formatted-body with-parsed-body]]
            [cljs.core.async :as a]
            [clojure.string :as s]
            [goog.events :as e]
            [goog.net.EventType :as et]
            [goog.crypt.base64 :as b64]
            [cemerick.url :refer [url]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:import [goog.net XhrIo]))

(defn with-default [default]
  (fn [s]
    (if (or (s/blank? s) (= -1 s))
      default
      s)))

(defn full-url [url-or-path]
  (let [default-protocol (subs js/location.protocol 0 (dec (count js/location.protocol)))]
    (-> (url url-or-path)
        (update-in [:protocol] (with-default default-protocol))
        (update-in [:host] (with-default js/location.hostname))
        (update-in [:port] (with-default js/location.port)))))

(defn with-query-params [{:keys [url query-params] :as req}]
  (assoc req
    :url (-> (full-url url)
             (update-in [:query] merge query-params))))

(defn with-headers [{:keys [xhr headers] :as req}]
  (update-in req [:headers] clj->js))

(defn resp-headers [xhr]
  (->> (for [[k v] (js->clj (.getResponseHeaders xhr))]
         [(keyword (s/lower-case k)) v])
       (into {})))

(defn with-basic-auth [{:keys [basic-auth] :as req}]
  (if-let [[user pass] basic-auth]
    (-> req
        (assoc-in [:headers :authorization] (str "Basic "
                                                 (b64/encodeString (str user ":" pass)))))
    req))

(defn request [req]
  (let [resp-ch (a/chan)
        {:keys [xhr url method body headers]} (-> req
                                                  (assoc :xhr (goog.net.XhrIo.))
                                                  with-query-params
                                                  with-formatted-body
                                                  with-basic-auth
                                                  with-headers)]
    (e/listen xhr et/COMPLETE
              (fn [e]
                (let [resp (-> {:status (.getStatus xhr)
                                :headers (resp-headers xhr)
                                :body (.getResponseText xhr)}
                               
                               (with-parsed-body req))]
                  (go
                    (a/>! resp-ch resp)
                    (a/close! resp-ch)))))

    (js/console.log body)
    (.send xhr url (s/upper-case (name method)) body headers)
    resp-ch))

(defn get [url & [opts]]
  (request (assoc opts
             :method :get
             :url url)))

(defn post [url & [opts]]
  (request (assoc opts
             :method :post
             :url url)))

(defn put [url & [opts]]
  (request (assoc opts
             :method :put
             :url url)))

(defn delete [url & [opts]]
  (request (assoc opts
             :method :delete
             :url url)))

(defn head [url & [opts]]
  (request (assoc opts
             :method :head
             :url url)))
