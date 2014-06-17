(ns cljs-tooling.complete
  "Standalone auto-complete library based on cljs analyzer state"
  (:require [cljs-tooling.util.analysis :as a]
            [cljs-tooling.util.misc :as u]))

;;; TODO
(defn ns-classes
  "Returns a list of potential class name completions for a given namespace"
  [env ns]

  ;;(map name (keys (ns-imports ns)))
  )

(def special-forms
  '[def if do let quote var fn loop recur throw try dot new set!])

(defn prefix-completions
  [prefix completions]
  (map #(symbol (str prefix "/" %)) completions))

(defn ns-completions
  "Returns a list of public vars in the given namespace."
  ([env ns] (keys (a/public-vars env ns)))
  ([env ns prefix] (prefix-completions prefix (ns-completions env ns))))

(defn macro-ns-completions
  "Returns a list of macro names in the given namespace."
  ([ns] (keys (a/public-macros ns)))
  ([ns prefix] (prefix-completions prefix (macro-ns-completions ns))))

(defn scoped-completions
  [env sym context-ns]
  (let [scope (symbol (namespace sym))
        ns (if (a/find-ns env scope)
             scope
             (a/to-ns env scope context-ns))
        macro-ns (if (= scope 'cljs.core)
                   scope
                   (a/to-macro-ns env scope context-ns))]
    (concat (ns-completions env ns scope)
            (macro-ns-completions macro-ns scope))))

(defn unscoped-completions
  [env context-ns]
  (concat special-forms
          (keys (a/all-ns env))
          (keys (a/ns-aliases env context-ns))
          (keys (a/macro-ns-aliases env context-ns))
          (keys (a/referred-vars env context-ns))
          (keys (a/referred-macros env context-ns))
          (keys (a/ns-vars env context-ns true))
          (keys (a/core-macros env context-ns))
          (keys (a/imports env context-ns))
          (vals (a/imports env context-ns))
          (ns-classes env context-ns)))

(defn potential-completions
  [env sym context-ns]
  (if (namespace sym)
    (scoped-completions env sym context-ns)
    (unscoped-completions env context-ns)))

(defn completions
  "Return a sequence of matching completions given current namespace and a prefix string"
  ([env prefix] (completions env prefix nil))
  ([env prefix context-ns]
     (->> (potential-completions env (u/as-sym prefix) (u/as-sym context-ns))
          distinct
          (map str)
          (filter #(.startsWith % prefix))
          sort)))


