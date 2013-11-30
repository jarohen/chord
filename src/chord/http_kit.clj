(ns chord.http-kit
  (:require [org.httpkit.server :as http]
            [clojure.core.async :refer [chan <! >! put! close! go-loop]]
            [clojure.core.async.impl.protocols :as p]))

(defn- read-from-ch! [ch ws]
  (http/on-receive ws #(put! ch {:message %})))

(defn- write-to-ch! [ch ws]
  (go-loop []
    (let [msg (<! ch)]
      (when msg
        (http/send! ws msg)
        (recur)))))

(defn- on-close [ws read-ch write-ch]
  (http/on-close ws
                 (fn [_]
                   ;; TODO support status?
                   (close! read-ch)
                   (close! write-ch))))

(defn- combine-chs [ws read-ch write-ch]
  (reify
    p/ReadPort
    (take! [_ handler]
      (p/take! read-ch handler))

    p/WritePort
    (put! [_ msg handler]
      (p/put! write-ch msg handler))

    p/Channel
    (close! [_]
      (p/close! read-ch)
      (p/close! write-ch)
      (http/close ws))))

(defn core-async-ch [httpkit-ch {:keys [read-ch write-ch]}]
  (let [read-ch (doto (or read-ch (chan))
                  (read-from-ch! httpkit-ch))
        write-ch (doto (or write-ch (chan))
                  (write-to-ch! httpkit-ch))
        combined-ch (combine-chs httpkit-ch read-ch write-ch)]
     
    (on-close httpkit-ch read-ch write-ch)
    combined-ch))

(defmacro with-channel
  "Extracts the websocket from the request and binds it to 'ch-name' in the body
   Arguments:
    req         - (required) HTTP-kit request map
    ch-name     - (required) variable to bind the channel to in the body
    opts        - (optional) map to configure reading/writing channels
      :read-ch  - (optional) (possibly buffered) channel to use for reading the websocket
      :write-ch - (optional) (possibly buffered) channel to use for writing to the websocket

   Usage:
    (require '[clojure.core.async :as a])

    (with-channel req the-ws
      (a/go-loop []
        (when-let [msg (a/<! the-ws)]
          (println msg)
          (recur))))

    (with-channel req the-ws
      {:read-ch (a/chan (a/sliding-buffer 10))
       :write-ch (a/chan (a/dropping-buffer 5))}

      (go-loop []
        (when-let [msg (<! the-ws)]
          (println msg)
          (recur))))"
  
  [req ch-name & [opts & body]]
  (let [opts? (and (map? opts) (seq body))
        body (cond->> body
               (not opts?) (cons opts))
        opts (when opts? opts)]
    `(http/with-channel ~req httpkit-ch#
       (let [~ch-name (core-async-ch httpkit-ch# ~opts)]
         ~@body))))

(comment
  (defn handler [req]
    (with-channel req ch
      (go-loop []
        (if-let [{:keys [message]} (<! ch)]
          (do
            (prn {:message message})
            (>! ch (str "You said: " message))
            (recur))
          (prn "closed.")))))

  (defonce server (http/run-server #'handler {:port 3000})))
