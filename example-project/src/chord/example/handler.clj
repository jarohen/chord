(ns chord.example.handler
  (:require [yoyo.cljs :as cljs]
            [ring.util.response :refer [response]]
            [compojure.core :refer [routes GET ANY]]
            [compojure.route :refer [resources]]
            [chord.http-kit :refer [wrap-websocket-handler]]
            [clojure.core.async :refer [<! >! put! close! go go-loop timeout]]
            [hiccup.page :refer [html5 include-js]]
            [ring.middleware.format :refer [wrap-restful-format]]
            [ring.middleware.basic-authentication :refer [wrap-basic-authentication]]))

(defn page-frame [{:keys [cljs-compiler]}]
  (html5
   [:head
    [:title "Chord Example"]
    (include-js (cljs/path-for-js cljs-compiler))]
   [:body [:div#content]]))

(defn ws-handler [{:keys [ws-channel] :as req}]
  (println "Opened connection from" (:remote-addr req))
  (go-loop []
    (when-let [{:keys [message error] :as msg} (<! ws-channel)]
      (prn "Message received:" msg)
      (>! ws-channel (if error
                       (format "Error: '%s'." (pr-str msg))
                       {:received (format "You passed: '%s' at %s." (pr-str message) (java.util.Date.))}))
      (recur))))

(defn make-handler [{:keys [cljs-compiler] :as app}]
  (routes
    (GET "/" [] (response (page-frame app)))
    (GET "/ws" [] (-> ws-handler
                      (wrap-websocket-handler {:format :transit-json})))
    (ANY "/ajax" []
      (-> (fn [{:keys [body-params] :as req}]
            (response {:you-said body-params
                       :req (dissoc req :async-channel :body)}))

          (wrap-restful-format :formats [:edn :json-kw])
          (wrap-basic-authentication #(do
                                        (prn %&)
                                        true))))

    (cljs/cljs-handler cljs-compiler)))
