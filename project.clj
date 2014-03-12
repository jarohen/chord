(defproject jarohen/chord "0.3.1-rc1"
  :description "A library to bridge the gap between CLJ/CLJS, web-sockets and core.async"
  :url "https://github.com/james-henderson/chord.git"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/core.async "0.1.242.0-44b1e3-alpha"]
                 [http-kit "2.1.10"]
                 [org.clojure/tools.reader "0.8.3"]
                 [cheshire "5.3.1"]])
