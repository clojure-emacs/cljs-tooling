(ns cljs-tooling.complete
  "Standalone auto-complete library based on cljs analyzer state"
  (:require [cljs-tooling.util.analysis :as a]
            [cljs-tooling.util.misc :as u]))

(defn- candidate-data
  "Returns a map of candidate data for the given arguments."
  [candidate ns type]
  (merge {:candidate (name candidate)
          :type type}
         (when ns {:ns (symbol ns)})))

(defn- var->type
  "Returns the candidate type corresponding to the given metadata map."
  [var]
  (condp #(get %2 %1) var
    :protocol :protocol-function
    :fn-var :function
    :record :record
    :protocols :type
    :protocol-symbol :protocol
    :var))

(def special-forms
  '#{& . case* catch def defrecord* deftype* do finally fn* if js* let*
     letfn* loop* new ns quote recur set! throw try})

(def special-form-candidates
  "Candidate data for all special forms."
  (for [form special-forms]
    (candidate-data form 'cljs.core :special-form)))

(defn all-ns-candidates
  "Returns candidate data for all namespaces in the environment."
  [env]
  (for [[ns _] (a/all-ns env)]
    (candidate-data ns nil :namespace)))

(defn ns-candidates
  "Returns candidate data for all referred namespaces (and their aliases) in context-ns."
  [env context-ns]
  (for [[alias ns] (a/ns-aliases env context-ns)
        :let [ns (when-not (= alias ns) ns)]]
    (candidate-data alias ns :namespace)))

(defn macro-ns-candidates
  "Returns candidate data for all referred macro namespaces (and their aliases) in
  context-ns."
  [env context-ns]
  (for [[alias ns] (a/macro-ns-aliases env context-ns)
        :let [ns (when-not (= alias ns) ns)]]
    (candidate-data alias ns :namespace)))

(defn referred-var-candidates
  "Returns candidate data for all referred vars in context-ns."
  [env context-ns]
  (for [[refer qualified-name] (a/referred-vars env context-ns)
        :let [ns (namespace qualified-name)
              type (var->type (a/find-var env qualified-name))]]
    (candidate-data refer ns type)))

(defn referred-macro-candidates
  "Returns candidate data for all referred macros in context-ns."
  [env context-ns]
  (for [[refer qualified-name] (a/referred-macros env context-ns)
        :let [ns (namespace qualified-name)]]
    (candidate-data refer ns :macro)))

(defn- var-candidates
  [vars]
  (for [[name meta] vars
        :let [qualified-name (:name meta)
              ns (namespace qualified-name)
              type (var->type meta)]]
    (candidate-data name ns type)))

(defn ns-var-candidates
  "Returns candidate data for all vars defined in ns."
  [env ns]
  (var-candidates (a/ns-vars env ns)))

(defn core-var-candidates
  "Returns candidate data for all cljs.core vars visible in context-ns."
  [env context-ns]
  (var-candidates (a/core-vars env context-ns)))

(defn macro-candidates
  [macros]
  (for [[name var] macros
        :let [var-meta (meta var)
              ns (ns-name (:ns var-meta))]]
    (candidate-data name ns :macro)))

(defn core-macro-candidates
  [env ns]
  "Returns candidate data for all cljs.core macros visible in ns."
  (macro-candidates (a/core-macros env ns)))

(defn import-candidates
  "Returns candidate data for all imports in context-ns."
  [env context-ns]
  (flatten
   (for [[import qualified-name] (a/imports env context-ns)]
     [(candidate-data import nil :import)
      (candidate-data qualified-name nil :import)])))

(defn unscoped-candidates
  "Returns all non-namespace-qualified potential candidates in context-ns."
  [env context-ns]
  (concat special-form-candidates
          (all-ns-candidates env)
          (ns-candidates env context-ns)
          (macro-ns-candidates env context-ns)
          (referred-var-candidates env context-ns)
          (referred-macro-candidates env context-ns)
          (ns-var-candidates env context-ns)
          (core-var-candidates env context-ns)
          (core-macro-candidates env context-ns)
          (import-candidates env context-ns)))

(defn- prefix-candidate
  [prefix candidate-data]
  (let [candidate (:candidate candidate-data)
        prefixed-candidate (str prefix "/" candidate)]
    (assoc candidate-data :candidate prefixed-candidate)))

(defn- prefix-candidates
  [prefix candidates]
  (map #(prefix-candidate prefix %) candidates))

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

(defn ns-public-var-candidates
  "Returns candidate data for all public vars defined in ns."
  [env ns]
  (var-candidates (a/public-vars env ns)))

(defn ns-macro-candidates
  "Returns candidate data for all macros defined in ns."
  [env ns]
  (macro-candidates (a/public-macros ns)))

(defn scoped-candidates
  "Returns all candidates for the namespace of sym. Sym must be
  namespace-qualified. Macro candidates are included if the namespace has its
  macros required in context-ns."
  [env sym context-ns]
  (let [scope (symbol (namespace sym))
        ns (scope->ns env scope context-ns)
        macro-ns (scope->macro-ns env scope context-ns)]
    (mapcat #(prefix-candidates scope %)
            [(ns-public-var-candidates env ns)
             (ns-macro-candidates env macro-ns)])))

(defn potential-candidates
  "Returns all candidates for sym. If sym is namespace-qualified, the candidates
  for that namespace will be returned (including macros if the namespace has its
  macros required in context-ns). Otherwise, all non-namespace-qualified
  candidates for context-ns will be returned."
  [env sym context-ns]
  (if (namespace sym)
    (scoped-candidates env sym context-ns)
    (unscoped-candidates env context-ns)))

(defn- distinct-candidates
  "Filters candidates to have only one entry for each value of :candidate. If
  multiple such entries do exist, the first occurrence is used."
  [candidates]
  (map first (vals (group-by :candidate candidates))))

(defn- candidate-match?
  [candidate prefix]
  (.startsWith ^String (:candidate candidate) (str prefix)))

(defn completions
  "Returns a sequence of candidate data for completions matching the given
  prefix string and (optionally) the current namespace."
  ([env prefix] (completions env prefix nil))
  ([env prefix context-ns]
   (let [prefix (u/as-sym prefix)
         context-ns (u/as-sym context-ns)]
     (->> (potential-candidates env prefix context-ns)
          distinct-candidates
          (filter #(candidate-match? % prefix))
          (sort-by :candidate)))))
