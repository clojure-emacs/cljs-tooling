(ns cljs-tooling.test-ns
  (:refer-clojure :exclude [unchecked-byte while])
  (:require [cljs.core.async :refer [sliding-buffer] :include-macros true]
            [clojure.string]
            [mount.core :as mount :include-macros true]
            [cljs-tooling.test-ns-dep :as dep])
  (:import [goog.ui IdGenerator]))

(defrecord TestRecord [a b c])

(def x ::some-namespaced-keyword)

(defn issue-28
  []
  (println "https://github.com/clojure-emacs/cljs-tooling/issues/28"))
