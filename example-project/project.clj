(defproject jarohen/chord.example ""
  :description "An example project to show Chord in action"
  :url "https://github.com/james-henderson/chord/example-project"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/tools.reader "0.9.2"]

                 [jarohen/chord "0.7.0-SNAPSHOT"]

                 [ring/ring-core "1.3.2"]
                 [compojure "1.3.4"]
                 [hiccup "1.0.5"]
                 [ring-middleware-format "0.5.0"]
                 [ring-basic-authentication "1.0.5"]

                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]

                 [jarohen/nomad "0.8.0-beta3"]
                 [jarohen/yoyo "0.0.4"]
                 [jarohen/yoyo.system "0.0.1-20150704.122931-4"]
                 [jarohen/yoyo.cljs "0.0.3"]
                 [jarohen/yoyo.http-kit "0.0.2"]
                 [jarohen/embed-nrepl "0.1.1"]
                 [danlentz/clj-uuid "0.1.6"]]

  :exclusions [org.clojure/clojure
               org.clojure/clojurescript]

  :main chord.example.main

  :aliases {"dev" "run"}

  :source-paths ["src"]

  :profiles {:dev {:dependencies [[org.clojure/clojurescript "0.0-3308"]
                                  [reagent "0.5.0"]
                                  [weasel "0.7.0"]
                                  [com.cemerick/piggieback "0.2.1"]]}})
