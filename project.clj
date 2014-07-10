(defproject jarohen/chord "0.4.2-SNAPSHOT"
  :description "A library to bridge the gap between CLJ/CLJS, web-sockets and core.async"
  :url "https://github.com/james-henderson/chord.git"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/core.async "0.1.301.0-deb34a-alpha"]
                 [http-kit "2.1.18"]
                 [org.clojure/tools.reader "0.8.3"]
                 [cheshire "5.3.1"]]

  :plugins [[com.keminglabs/cljx "0.3.2"]]

  :hooks [cljx.hooks]

  :source-paths ["src" "target/generated/clj"]

  :filespecs [{:type :path
               :path "target/generated/cljs"}]

  :cljx {:builds [{:source-paths ["src"]
                   :output-path "target/generated/clj"
                   :rules :clj}

                  {:source-paths ["src"]
                   :output-path "target/generated/cljs"
                   :rules :cljs}]})
