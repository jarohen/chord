(defproject jarohen/chord.example ""
  :description "An example project to show Chord in action"
  :url "https://github.com/james-henderson/chord/example-project"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [jarohen/chord "0.2.1-SNAPSHOT"]

                 [ring/ring-core "1.2.0"]
                 [compojure "1.1.5"]
                 [hiccup "1.0.4"]

                 [org.clojure/clojurescript "0.0-1913"]
                 [org.clojure/tools.reader "0.7.8"]

                 [prismatic/dommy "0.1.2"]]

  :plugins [[lein-pdo "0.1.1"]
            [jarohen/lein-frodo "0.2.0"]
            [lein-cljsbuild "0.3.3"]]

  :frodo/config-resource "chord-example.edn"

  :aliases {"dev" ["pdo" "cljsbuild" "auto," "frodo"]}

  :resource-paths ["resources" "target/resources"]

  :cljsbuild {:builds [{:source-paths ["src"]
                        :compiler {:output-to "target/resources/js/chord-example.js"
                                   :optimizations :whitespace
                                   :pretty-print true}}]})
