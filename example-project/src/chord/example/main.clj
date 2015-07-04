(ns chord.example.main
  (:require [chord.example.handler :refer [make-handler]]
            [nrepl.embed :as nrepl]
            [yoyo :as y]
            [yoyo.cljs :as cljs]
            [yoyo.system :as ys]
            [yoyo.http-kit :as http-kit]))

(def cljs-opts
  {:source-paths ["ui-src"]

   :main 'chord.example.front-end

   :web-context-path "/js"

   :output-dir "target/cljs/"

   :dev {:optimizations :none
         :pretty-print? true}

   :build {:optimizations :advanced
           :pretty-print? false
           :classpath-prefix "js"}})

(defn with-web-server [{:keys [handler]} f]
  (http-kit/with-webserver {:handler handler
                            :port 3000}
    f))

(defn with-cljs-compiler [{:keys [cljs-opts]} f]
  (cljs/with-cljs-compiler cljs-opts f))

(def make-system
  (-> (ys/make-system (fn []
                        {:cljs-opts cljs-opts
                         :handler (-> make-handler
                                      (ys/using {:cljs-compiler [:cljs-compiler]}))
                         :cljs-compiler (-> with-cljs-compiler
                                            (ys/using {:cljs-opts [:cljs-opts]}))
                         :web-server (-> with-web-server
                                         (ys/using {:handler [:handler]}))}))
      (ys/with-system-put-to 'user/system)))

(defn -main []
  (nrepl/start-nrepl! {:port 7888})

  (y/set-system-fn! 'chord.example.main/make-system)

  (y/start!))
