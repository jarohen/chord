(defproject jarohen/chord "0.5.0"
  :description "A library to bridge the gap between CLJ/CLJS, web-sockets and core.async"
  :url "https://github.com/james-henderson/chord.git"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/core.async "0.1.301.0-deb34a-alpha"]
                 [http-kit "2.1.18"]
                 [org.clojure/tools.reader "0.8.3"]

                 [com.cemerick/url "0.1.1"]
                 [cheshire "5.3.1"]

                 [com.cognitect/transit-clj "0.8.259"]
                 [com.cognitect/transit-cljs "0.8.192"]

                 [org.clojure/data.fressian "0.2.0"]
                 [net.unit8/fressian-cljs "0.1.0"]]

  :plugins [[com.keminglabs/cljx "0.5.0"]]

  :prep-tasks [["cljx" "once"]]

  :source-paths ["src" "target/generated/clj"]

  :filespecs [{:type :path
               :path "target/generated/cljs"}]

  :cljx {:builds [{:source-paths ["src"]
                   :output-path "target/generated/clj"
                   :rules :clj}

                  {:source-paths ["src"]
                   :output-path "target/generated/cljs"
                   :rules :cljs}]})
