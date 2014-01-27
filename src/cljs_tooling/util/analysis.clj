(ns cljs-tooling.util.analysis)

(defn as-sym [x]
  (if x (symbol x)))

(def NSES :cljs.analyzer/namespaces)

(defn get-all-nses
  [env]
  (keys (NSES env)))

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
    (let [ns-info (get-in env [NSES (as-sym ns)])]
      (->> (select-keys ns-info [:use-macros :requires :require-macros :uses])
           vals
           (mapcat collect-aliases)
           (into {})))))

(defn ns-vars
  "Returns a list of all potential var name completions for a given namespace"
  ([env ns] (ns-vars env ns false))
  ([env ns include-core?]
     (concat (get-in env [NSES (as-sym ns) :defs])
             (if include-core? (get-in env [NSES 'cljs.core :defs])))))

(defn public-vars
  [env ns]
  (let [public? identity] ;; TODO filter public
    (filter public? (ns-vars env ns))))

(defn get-vars-list
  [env current-ns scope]
  (let [alias->ns (aliased-nses env current-ns)]
    (mapcat keys [(ns-vars env scope)
                  (ns-vars env (alias->ns scope))])))
