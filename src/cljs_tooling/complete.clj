(ns cljs-tooling.complete
  "Standalone auto-complete library based on cljs analyzer state")

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



(defn namespaces
  "Returns a list of potential namespace completions for a given namespace"
  [env ns]
  (map name (concat
             ;; All the namespaces
             (get-all-nses env)
             ;; all the aliases 
             (keys (aliased-nses env ns)))))

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
         (get-vars-list env ns scope))))

(defmethod potential-completions :class
  [env prefix ns]
  (namespaces env ns))

(defmethod potential-completions :var
  [env _ ns]
  (map str (concat special-forms
                   (namespaces env ns)
                   (keys (ns-vars env ns true))
                   (ns-classes env ns))))

(defn completions
  "Return a sequence of matching completions given current namespace and a prefix string"
  ([env prefix] (completions env prefix nil))
  ([env prefix ns]
     (sort (for [completion (distinct (potential-completions env prefix ns))
                 :when (.startsWith completion prefix)]
             completion))))


