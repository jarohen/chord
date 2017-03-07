(defproject example-node "0.1.0-SNAPSHOT"
  :description "FIXME: write this!"
  :url "http://example.com/FIXME"

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.495"]
                 [org.clojure/core.async "0.3.441"]

                 [com.cognitect/transit-clj "0.8.297"]
                 [com.cognitect/transit-cljs "0.8.239"]

                 [com.cemerick/piggieback "0.2.1"]
                 [jarohen/chord "0.8.1"]]

  :plugins [[lein-cljsbuild "1.1.5"]
            [lein-npm "0.6.2"]]

  :npm {:dependencies [[ws "2.2.0"]]}

  :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}

  :source-paths ["src"]

  :cljsbuild {
    :builds [{:source-paths ["src"]
              :compiler {
                :target :nodejs
                :output-to "example_node.js"
                :optimizations :simple}}]})
