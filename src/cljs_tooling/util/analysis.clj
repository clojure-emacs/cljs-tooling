(ns cljs-tooling.util.analysis
  (:require [cljs-tooling.util.misc :as u])
  (:refer-clojure :exclude [find-ns]))


(def NSES :cljs.analyzer/namespaces)

(defn get-all-nses
  [env]
  (keys (NSES env)))

(defn find-ns
  [env sym]
  (get-in env [NSES sym]))

;; Code adapted from clojure-complete (http://github.com/ninjudd/clojure-complete)


(defn collect-aliases
  [ns-m]
  (for [[k v] ns-m
        :when (not= k v)]
    [k v]))

(defn aliased-nses
  "Returns a map of aliases in the namespace"
  [env ns]
  (if ns
    (let [ns-info (get-in env [NSES (u/as-sym ns)])]
      (->> (select-keys ns-info [:use-macros :requires :require-macros :uses])
           vals
           (mapcat collect-aliases)
           (into {})))))

(defn ns-vars
  ([env ns] (ns-vars env ns false))
  ([env ns include-core?]
     (merge (get-in env [NSES (u/as-sym ns) :defs])
            (if include-core? (get-in env [NSES 'cljs.core :defs])))))

(defn public-vars
  [env ns]
  (let [public? identity] ;; TODO filter public
    (filter public? (ns-vars env ns))))

