(ns cljs-tooling.util.analysis
  (:require [cljs-tooling.util.misc :as u])
  (:refer-clojure :exclude [find-ns find-var all-ns ns-aliases]))


(def NSES :cljs.analyzer/namespaces)

(defn all-ns
  [env]
  (NSES env))

(defn find-ns
  [env ns]
  (get-in env [NSES (u/as-sym ns)]))

(defn find-var
  "Given a namespace-qualified var name, gets the analyzer metadata for that
  var."
  [env sym]
  (let [sym (u/as-sym sym)
        ns (find-ns env (namespace sym))]
    (get (:defs ns) (-> sym name symbol))))

;; Code adapted from clojure-complete (http://github.com/ninjudd/clojure-complete)


(defn ns-aliases
  "Returns a map of [ns-name-or-alias] to [ns-name] for the given namespace."
  [env ns]
  (:requires (find-ns env ns)))

(defn macro-ns-aliases
  "Returns a map of [macro-ns-name-or-alias] to [macro-ns-name] for the given namespace."
  [env ns]
  (:require-macros (find-ns env ns)))

(defn- expand-refer-map
  [m]
  (into {} (for [[k v] m] [k (symbol (str v "/" k))])))

(defn referred-vars
  "Returns a map of [var-name] to [ns-qualified-var-name] for all referred vars
  in the given namespace."
  [env ns]
  (->> (find-ns env ns)
       :uses
       expand-refer-map))

(defn referred-macros
  "Returns a map of [macro-name] to [ns-qualified-macro-name] for all referred
  macros in the given namespace."
  [env ns]
  (->> (find-ns env ns)
       :use-macros
       expand-refer-map))

(defn imports
  "Returns a map of [import-name] to [ns-qualified-import-name] for all imports
  in the given namespace."
  [env ns]
  (:imports (find-ns env ns)))

(defn to-ns
  "If sym is an alias to, or the name of, a namespace referred to in ns, returns
  the name of the namespace; else returns nil."
  [env sym ns]
  (get (ns-aliases env ns) (u/as-sym sym)))

(defn to-macro-ns
  "If sym is an alias to, or the name of, a macro namespace referred to in ns,
  returns the name of the macro namespace; else returns nil."
  [env sym ns]
  (get (macro-ns-aliases env ns) (u/as-sym sym)))

(defn- public?
  [var]
  ((complement :private) (val var)))

(defn public-vars
  "Returns a list of the public vars declared in the ns."
  [env ns]
  (let [vars (:defs (find-ns env ns))]
    (into {} (filter public? vars))))

(defn- macro?
  [var]
  (-> (val var)
      meta
      :macro))

(defn public-macros
  "Returns a list of the public macros declared in the ns."
  [ns]
  (if (and ns (clojure.core/find-ns ns))
    (->> (ns-publics ns)
         (filter macro?)
         (into {}))))

(defn core-vars
  "Returns a list of cljs.core vars visible to the ns."
  [env ns]
  (let [vars (public-vars env 'cljs.core)
        excludes (:excludes (find-ns env ns))]
    (apply dissoc vars excludes)))

(defn ns-vars
  "Vars visible to the ns"
  ([env ns] (ns-vars env ns false))
  ([env ns include-core?]
     (merge (:defs (find-ns env ns))
            (if include-core? (core-vars env ns)))))

(defn core-macros
  "Returns a list of cljs.core macros visible to the ns."
  [env ns]
  (let [macros (public-macros 'cljs.core)
        excludes (:excludes (find-ns env ns))]
    (apply dissoc macros excludes)))
