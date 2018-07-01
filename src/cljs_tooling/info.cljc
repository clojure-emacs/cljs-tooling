(ns cljs-tooling.info
  (:require [cljs-tooling.util.analysis :as a #?@(:cljs [:include-macros true])]
            [cljs-tooling.util.misc :as u #?@(:cljs [:include-macros true])]
            [clojure.pprint :refer [pprint]]))

(defn format-ns-meta
  [meta]
  (merge (select-keys meta [:doc :author])
         {:file (-> meta :defs first second :file)
          :line 1
          :name (:name meta)
          :ns (:name meta)}))

(defn format-macro-ns
  [env var]
  (let [meta (a/ns-meta var)]
    {:author (:author meta)
     :doc (:doc meta)
     :file #?(:clj (some-> var ns-interns first val a/var-meta :file)
              :cljs (some-> env
                            (a/ns-interns-from-env (u/add-ns-macros (:ns meta)))
                            first
                            val
                            :file))
     :line 1
     :name (:name meta)
     :ns (:ns meta)}))

(defn- unquote-1
  "Handles some weird double-quoting in the analyzer"
  [[fst & more :as form]]
  (if (= fst 'quote)
    (first more)
    form))

(defn format-var-meta
  "Format it similarly to metadata on a var"
  [context-ns meta]
  (some-> meta
          (select-keys [:arglists :line :column :ns :name :file :doc])
          (update :arglists unquote-1)))

(defn format-macro-meta
  [env meta]
  (-> meta
      (merge (:meta meta))
      (merge (select-keys meta [:file :ns :name])) ;; :file is more accurate than in :meta
      (select-keys [:file :ns :name :arglists :line :column :doc])
      (update :arglists unquote-1)))

(defn scoped-var-meta
  [env sym & [context-ns]]
  (or (a/find-symbol-meta env sym)
      (let [scope (u/namespace-sym sym)
            ns (a/ns-alias env scope context-ns)
            sym (symbol (str ns "/" (u/name-sym sym)))]
        (a/find-symbol-meta env sym))))

(defn macro-namespace
  "Compute the namespace of a macro symbol."
  [env sym & [context-ns]]
  {:pre [(symbol? sym)]}
  (let [ns-from-sym (u/as-sym (namespace sym))]
    (or (a/macro-ns-alias env ns-from-sym context-ns)
        ns-from-sym
        context-ns)))

(defn scoped-macro-meta
  [env sym & [context-ns]]
  (let [ns (or context-ns (macro-namespace env sym context-ns))
        sym (symbol (name sym))]
    (when (and ns (find-ns ns))
      (some-> env
              (a/public-macros #?(:clj ns
                                  :cljs (u/add-ns-macros ns)))
              (get sym)
              a/var-meta))))

(defn referred-macro-meta
  [env sym & [context-ns]]
  (let [ns (macro-namespace env sym context-ns)
        sym (symbol (name sym))]
    (when-let [referred (get (a/referred-macros env ns) sym)]
      #?(:clj (-> referred
                  find-var
                  a/var-meta)
         :cljs (let [referred-ns (symbol (namespace referred))
                     referred-sym (symbol (name referred))]
                 (-> env
                     (a/ns-interns-from-env (u/add-ns-macros referred-ns))
                     (get referred-sym)
                     a/var-meta))))))

(defn aliased-macro-var
  [env sym & [context-ns]]
  (let [ns (macro-namespace env sym context-ns)]
    (some-> env
            (a/macro-ns-alias sym ns)
            #?(:cljs u/add-ns-macros)
            find-ns)))

(defn info
  "Returns an info map on the symbol in the context of the namespace, resolving aliases.
  'sym' can refer to a top-level var, a namespace, or an alias, the context-ns is optional"
  [env sym & [context-ns]]
  (let [sym (u/as-sym sym)
        context-ns (u/as-sym context-ns)]
    (u/cond-let

     ;; a special symbol
     [special-meta (a/special-meta env sym)] (format-var-meta context-ns special-meta)

     ;; an NS
     [ns (a/find-ns env sym)] (format-ns-meta ns)

     ;; ns alias
     [ns-alias (a/ns-alias env sym context-ns)] (format-ns-meta (a/find-ns env ns-alias))

     ;; macro ns
     #?@(:clj
         [[macro-ns-var (find-ns sym)] (format-macro-ns env macro-ns-var)]
         :cljs
         [[macro-ns-var (a/find-ns env (u/add-ns-macros sym))] (format-macro-ns env macro-ns-var)])

     ;; macro ns alias
     [macro-ns-alias (aliased-macro-var env sym context-ns)] (format-macro-ns env macro-ns-alias)

     ;; referred var
     [var (get (a/referred-vars env context-ns) sym)] (format-var-meta context-ns (a/find-symbol-meta env var))

     ;; referred macro
     [referred-meta (referred-macro-meta env sym context-ns)] (format-macro-meta env referred-meta)

     ;; scoped var
     [scoped-meta (scoped-var-meta env sym context-ns)] (format-var-meta context-ns scoped-meta)

     ;; scoped macro
     [macro-meta (scoped-macro-meta env sym context-ns)] (format-macro-meta env macro-meta)

     ;; var in cljs.core
     [var (get (a/core-vars env context-ns) sym)] (format-var-meta context-ns (a/var-meta var))

     ;; macro in cljs.core
     [macro-meta (scoped-macro-meta env sym 'cljs.core)] (format-macro-meta env macro-meta)

     ;; var in ns
     [var-meta (scoped-var-meta env sym context-ns)] (format-var-meta context-ns var-meta))))

(comment
  (require '[cljs-tooling.test-env :as tenv])
  (def env (tenv/create-test-env))
  (info env 'mount.core/on-error)
  )
