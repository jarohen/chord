(defproject jarohen/chord "0.1.0"
  :description "A library to bridge the gap between CLJS, web-sockets and core.async"
  :url "https://github.com/james-henderson/chord.git"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  ;; TODO when a core.async stable is released, remove this!
  :repositories {"sonatype-oss-public"
                 "https://oss.sonatype.org/content/groups/public/"}
  
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/core.async "0.1.0-20130802.160123-63"]])
