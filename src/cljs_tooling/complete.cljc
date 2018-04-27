(ns cljs-tooling.complete
  "Standalone auto-complete library based on cljs analyzer state"
  (:require [cljs-tooling.util.analysis :as a]
            [cljs-tooling.util.misc :as u]
            [cljs-tooling.info :as i]
            [clojure.set :as set]))

(defn- candidate-data
  "Returns a map of candidate data for the given arguments."
  [candidate ns type]
  (merge {:candidate (str candidate)
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
              type (var->type (a/find-symbol-meta env qualified-name))]]
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
              ns (some-> qualified-name namespace)
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
        :let [var-meta (a/var-meta var)
              ns (:ns var-meta)]]
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
     [(candidate-data import nil :class)
      (candidate-data qualified-name nil :class)])))

(defn keyword-candidates
  "Returns candidate data for all keyword constants in the environment."
  [env]
  (map #(candidate-data % nil :keyword) (a/keyword-constants env)))

(defn namespaced-keyword-candidates
  "Returns all namespaced keywords defined in context-ns."
  [env context-ns]
  (when context-ns
    (for [kw (a/keyword-constants env)
          :when (= context-ns (u/as-sym (namespace kw)))]
      (candidate-data (str "::" (name kw)) context-ns :keyword))))

(defn referred-namespaced-keyword-candidates
  "Returns all namespaced keywords referred in context-ns."
  [env context-ns]
  (when context-ns
    (let [aliases (->> (a/ns-aliases env context-ns)
                       (filter (fn [[k v]] (not= k v)))
                       (into {})
                       (set/map-invert))]
      (for [kw (a/keyword-constants env)
            :let [ns (u/as-sym (namespace kw))
                  alias (get aliases ns)]
            :when alias]
        (candidate-data (str "::" alias "/" (name kw)) ns :keyword)))))

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
          (import-candidates env context-ns)
          (keyword-candidates env)
          (namespaced-keyword-candidates env context-ns)
          (referred-namespaced-keyword-candidates env context-ns)))

(defn- prefix-candidate
  [prefix candidate-data]
  (assert (string? prefix) (str "prefix not a string but " (type prefix)))
  (let [candidate (:candidate candidate-data)
        prefixed-candidate (str prefix "/" candidate)]
    (assoc candidate-data :candidate prefixed-candidate)))

(defn- prefix-candidates
  [prefix candidates]
  (map #(prefix-candidate prefix %) candidates))

(defn- ->ns
  [env symbol-ns context-ns]
  (assert (symbol? symbol-ns) (str "symbol-ns not a symbol but " (type symbol-ns)))
  (assert (or (nil? context-ns) (symbol? context-ns)) (str "context-ns not a (nilable) symbol but " (type context-ns)))
  (if (a/find-ns env symbol-ns)
    symbol-ns
    (a/ns-alias env symbol-ns context-ns)))

(defn- ->macro-ns
  [env symbol-ns context-ns]
  (assert (symbol? symbol-ns) (str "symbol-ns not a symbol but " (type symbol-ns)))
  (assert (or (nil? context-ns) (symbol? context-ns)) (str "context-ns not a (nilable) symbol but " (type context-ns)))
  (if (= symbol-ns 'cljs.core)
    symbol-ns
    (a/macro-ns-alias env symbol-ns context-ns)))

(defn ns-public-var-candidates
  "Returns candidate data for all public vars defined in ns."
  [env ns]
  (var-candidates (a/public-vars env ns)))

(defn ns-macro-candidates
  "Returns candidate data for all macros defined in ns."
  [env ns]
  (-> env
      (a/public-macros #?(:clj ns :cljs (u/add-ns-macros ns)))
      macro-candidates))

(defn scoped-candidates
  "Returns all candidates for the namespace of sym. Sym must be
  namespace-qualified. Macro candidates are included if the namespace has its
  macros required in context-ns."
  [env sym context-ns]
  (assert (string? sym) (str "sym must be a string - likely a cljs-tooling bug, please report it - " sym " was a " (type sym)))
  (let [sym-ns (-> sym u/as-sym u/namespace-sym)
        computed-ns (->ns env sym-ns context-ns)
        macro-ns (->macro-ns env sym-ns context-ns)
        sym-ns-as-string (str sym-ns)]
    (mapcat #(prefix-candidates sym-ns-as-string %)
            [(ns-public-var-candidates env computed-ns)
             (when macro-ns
               (ns-macro-candidates env macro-ns))])))

(defn potential-candidates
  "Returns all candidates for sym. If sym is namespace-qualified, the candidates
  for that namespace will be returned (including macros if the namespace has its
  macros required in context-ns). Otherwise, all non-namespace-qualified
  candidates for context-ns will be returned."
  [env sym context-ns]
  (assert (string? sym) (str "sym must be a string - likely a cljs-tooling bug, please report it - " sym " was a " (type sym)))
  (if (or (= (.indexOf ^String sym "/") -1) (.startsWith ^String sym ":"))
    (unscoped-candidates env context-ns)
    (scoped-candidates env sym context-ns)))

(defn- distinct-candidates
  "Filters candidates to have only one entry for each value of :candidate. If
  multiple such entries do exist, the first occurrence is used."
  [candidates]
  (map first (vals (group-by :candidate candidates))))

(defn- candidate-match?
  [candidate prefix]
  (assert (string? prefix) (str "prefix must be a string - likely a cljs-tooling bug, please report it - " prefix " was " (type prefix)))
  (.startsWith ^String (:candidate candidate) prefix))

(defn- enrich-candidate [candidate env {:keys [context-ns extra-metadata]}]
  (if (seq extra-metadata)
    (let [var-meta (i/info env (symbol (str (:ns candidate)) (:candidate candidate)) context-ns)]
      (cond-> candidate
        (and (:arglists extra-metadata) (:arglists var-meta))
        (assoc :arglists (apply list (map pr-str (:arglists var-meta))))

        (and (:doc extra-metadata) (:doc var-meta))
        (assoc :doc (:doc var-meta))))
    candidate))

#?(:cljs
   (defn- remove-candidate-macros
     [candidate]
     (if (= :namespace (:type candidate))
       (update candidate :candidate (comp str u/remove-macros))
       candidate)))

(defn completions
  "Returns a sequence of candidate data for completions matching the given
  prefix string and options. If the third parameter is a string it's used
  as :context-ns option.

  - :context-ns - (optional) the current namespace;
  - :extra-metadata - set of additional fields (:arglists, :doc) to add to the response maps."
  ([env prefix] (completions env prefix nil))
  ([env prefix options-map]
   (let [{:keys [context-ns] :as options-map} (if (string? options-map) {:context-ns options-map} options-map)
         context-ns (u/as-sym context-ns)]
     (->> (potential-candidates env prefix context-ns)
          #?(:cljs (map remove-candidate-macros))
          distinct-candidates
          (filter #(candidate-match? % prefix))
          (map #(enrich-candidate % env options-map))
          (sort-by :candidate)))))

(comment
  (require '[cljs-tooling.test-env :as tenv])
  (def env (tenv/create-test-env))
  (completions env "cljs.core.async.impl.ioc-macros" "cljs.core.async")
  )
