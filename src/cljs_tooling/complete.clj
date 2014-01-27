(ns cljs-tooling.complete
  "Standalone auto-complete library based on cljs analyzer state"
  (:require [cljs-tooling.util.analysis :as a]))

(defn namespaces
  "Returns a list of potential namespace completions for a given namespace"
  [env ns]
  (map name (concat
             ;; All the namespaces
             (a/get-all-nses env)
             ;; all the aliases 
             (keys (a/aliased-nses env ns)))))

;;; TODO
(defn ns-classes
  "Returns a list of potential class name completions for a given namespace"
  [env ns]

  ;;(map name (keys (ns-imports ns)))
  )

(def special-forms
  (map name '[def if do let quote var fn loop recur throw try monitor-enter monitor-exit dot new set!]))


(defn potential-completions-dispatch
  [env prefix ns]
  (cond (.contains prefix "/") :scoped
        (.contains prefix ".") :class
        :else :var))

(defmulti potential-completions #'potential-completions-dispatch)

(defmethod potential-completions :scoped
  [env prefix ns]
  (let [scope (symbol (first (.split prefix "/")))]
    (map #(str scope "/" %)
         (a/get-vars-list env ns scope))))

(defmethod potential-completions :class
  [env prefix ns]
  (namespaces env ns))

(defmethod potential-completions :var
  [env _ ns]
  (map str (concat special-forms
                   (namespaces env ns)
                   (keys (a/ns-vars env ns true))
                   (ns-classes env ns))))

(defn completions
  "Return a sequence of matching completions given current namespace and a prefix string"
  ([env prefix] (completions env prefix nil))
  ([env prefix ns]
     (->> (potential-completions env prefix ns)
          distinct
          (filter #(.startsWith % prefix))
          sort)))


