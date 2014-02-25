(ns cljs-tooling.complete
  "Standalone auto-complete library based on cljs analyzer state"
  (:require [cljs-tooling.util.analysis :as a]
            [cljs-tooling.util.misc :as u]))

(defn namespaces
  "Returns a list of potential namespace completions for a given namespace"
  [env context-ns]
  (map name (concat
             ;; All the namespaces
             (a/get-all-nses env)
             ;; all the aliases 
             (keys (a/aliased-nses env context-ns)))))

;;; TODO
(defn ns-classes
  "Returns a list of potential class name completions for a given namespace"
  [env ns]

  ;;(map name (keys (ns-imports ns)))
  )

(def special-forms
  (map name '[def if do let quote var fn loop recur throw try dot new set!]))

(defn scoped-completions
  [env sym context-ns]
  (let [scope (symbol (namespace sym))
        new-context-ns (or
                        ;; absolute
                        (if (a/find-ns env scope)
                          scope)
                        ;; alias
                        (-> (a/aliased-nses env context-ns)
                            (get scope)))]
    (map #(str scope "/" %) (keys (a/ns-vars env new-context-ns)))))

(defn potential-completions
  [env sym context-ns]
  (if (namespace sym)
    (scoped-completions env sym context-ns)
    (map str (concat special-forms
                     (namespaces env context-ns)
                     (keys (a/ns-vars env context-ns true))
                     (ns-classes env context-ns)))))

(defn completions
  "Return a sequence of matching completions given current namespace and a prefix string"
  ([env prefix] (completions env prefix nil))
  ([env prefix context-ns]
     (->> (potential-completions env (u/as-sym prefix) (u/as-sym context-ns))
          distinct
          (filter #(.startsWith % prefix))
          sort)))


