(ns chord.example.handler
  (:require [yoyo.cljs :as cljs]
            [ring.util.response :refer [response]]
            [compojure.core :refer [routes GET ANY]]
            [compojure.route :refer [resources]]
            [chord.http-kit :refer [wrap-websocket-handler]]
            [clojure.core.async :as a :refer [go-loop go]]
            [clj-uuid :as uuid]
            [hiccup.page :refer [html5 include-js]]
            [ring.middleware.format :refer [wrap-restful-format]]
            [ring.middleware.basic-authentication :refer [wrap-basic-authentication]]))

(defn page-frame [{:keys [cljs-compiler]}]
  (html5
   [:head
    [:title "Chord Example"]
    (include-js (cljs/path-for-js cljs-compiler))]
   [:body [:div#content]]))

(defn ws-handler [{:keys [ws-channel] :as req} {:keys [chat-ch chat-mult]}]

  (let [tapped-ch (a/chan)
        user-id (uuid/v4)]

    (a/tap chat-mult tapped-ch)

    (println (format "Opened connection from %s, user-id %s."
                     (:remote-addr req)
                     user-id))

    (go
      (a/>! chat-ch {:type :user-joined
                     :user-id user-id})

      (loop []
        (a/alt!
          tapped-ch ([message] (if message
                                 (do
                                   (a/>! ws-channel message)
                                   (recur))

                                 (a/close! ws-channel)))

          ws-channel ([ws-message] (if ws-message
                                     (do
                                       (a/>! chat-ch {:type :message
                                                      :message (:message ws-message)
                                                      :user-id user-id})
                                       (recur))

                                     (do
                                       (a/untap chat-mult tapped-ch)
                                       (a/>! chat-ch {:type :user-left
                                                      :user-id user-id})))))))))

(defn with-chat-chs [f]
  (let [chat-ch (a/chan)
        chat-mult (a/mult chat-ch)]
    (try
      (f {:chat-ch chat-ch
          :chat-mult chat-mult})

      (finally
        (a/close! chat-ch)))))

(defn make-handler [{:keys [cljs-compiler] :as app} f]
  (with-chat-chs
    (fn [chat-chs]
      (f (routes
           (GET "/" [] (response (page-frame app)))
           (GET "/ws" [] (-> #(ws-handler % chat-chs)
                             (wrap-websocket-handler {:format :transit-json})))

           (cljs/cljs-handler cljs-compiler))))))
