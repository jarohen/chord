(ns chord.format.fressian
  (:require [chord.format :as f]
            #?(:clj [clojure.data.fressian :as fressian]
               :cljs [fressian-cljs.core :as fressian]))
  #?(:clj
      (:import [org.fressian.impl ByteBufferInputStream]
               [java.io ByteArrayOutputStream ByteArrayInputStream])))

(defmethod f/formatter* :fressian [_]
  (reify f/ChordFormatter
    (freeze [_ obj]
      #?(:clj (ByteBufferInputStream. (fressian/write obj))
         :cljs (fressian/write obj)))

    (thaw [_ s]
      (fressian/read s))))

