(ns cljs-tooling.complete
  "Standalone auto-complete library based on cljs analyzer state"
  (:require [cljs-tooling.util.analysis :as a]
            [cljs-tooling.util.misc :as u]))

(def special-forms
  '#{& . case* catch def defrecord* deftype* do finally fn* if js* let*
     letfn* loop* new ns quote recur set! throw try})

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

(defn- scope->ns
  [env scope context-ns]
  (if (a/find-ns env scope)
    scope
    (a/to-ns env scope context-ns)))

(defn- scope->macro-ns
  [env scope context-ns]
  (if (= scope 'cljs.core)
    scope
    (a/to-macro-ns env scope context-ns)))

(defn scoped-completions
  [env sym context-ns]
  (let [scope (symbol (namespace sym))
        ns (scope->ns scope)
        macro-ns (scope->macro-ns scope)]
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
          (vals (a/imports env context-ns))))

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


