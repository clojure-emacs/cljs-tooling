(ns cljs-tooling.info
  (:require [cljs-tooling.util.analysis :as a]
            [cljs-tooling.util.misc :as u]))

;;; TODO
(defn format-ns
  [ns]
  (merge (select-keys ns [:doc :author])
         {:file (-> ns :defs first second :file)
          :line 1
          :name (:name ns)
          :ns (:name ns)}))

(defn format-macro-ns
  [ns]
  (let [ns (find-ns ns)]
    {:author (:author (meta ns))
     :doc (:doc (meta ns))
     :file (-> ns ns-interns first val meta :file)
     :line 1
     :name (ns-name ns)
     :ns (ns-name ns)}))

(defn- unquote-1
  "Handles some weird double-quoting in the analyzer"
  [[fst & more :as form]]
  (if (= fst 'quote)
    (first more)
    form))

(defn format-var
  "Format it similarly to metadata on a var"
  [context-ns var]
  (-> (select-keys var [:arglists :name :line :column :file :doc])
      (merge {:name (-> var :name name u/as-sym)
              :ns (-> var :name namespace u/as-sym)})
      (update-in [:arglists] unquote-1)))

(defn format-macro
  [macro]
  (merge (select-keys macro [:arglists :name :line :column :file :doc])
         {:ns (-> macro :ns ns-name)}))

(defn scoped-var-info
  [env sym & [context-ns]]
  (or (a/find-var env sym)
      (let [scope (u/as-sym (namespace sym))
            ns (a/to-ns env scope context-ns)
            sym (symbol (str ns "/" (name sym)))]
        (a/find-var env sym))))

(defn scoped-macro-info
  [env sym & [context-ns]]
  (let [scope (u/as-sym (namespace sym))
        ns (or (a/to-macro-ns env scope context-ns)
               scope)
        sym (symbol (str ns "/" (name sym)))]
    (if (and ns (find-ns ns))
      (->> (name sym)
           (str ns "/")
           symbol
           find-var
           meta))))

(defn info
  "Returns an info map on the symbol in the context of the namespace, resolving aliases.
'sym' can refer to a top-level var, a namespace, or an alias, the context-ns is optional"
  [env sym & [context-ns]]
  (let [sym (u/as-sym sym)
        context-ns (u/as-sym context-ns)]
    (u/cond-let
     ;; an NS
     [ns (a/find-ns env sym)] (format-ns ns)

     ;; ns alias
     [ns-alias (a/to-ns env sym context-ns)] (format-ns (a/find-ns env ns-alias))

     ;; macro ns
     [macro-ns (find-ns sym)] (format-macro-ns sym)

     ;; macro ns alias
     [macro-ns-alias (a/to-macro-ns env sym context-ns)] (format-macro-ns macro-ns-alias)

     ;; referred var
     [var (get (a/referred-vars env context-ns) sym)] (format-var context-ns (a/find-var env var))

     ;; referred macro
     [macro (get (a/referred-macros env context-ns) sym)] (format-macro (-> macro find-var meta))

     ;; var in ns
     [context-var (get (a/ns-vars env context-ns true) sym)] (format-var context-ns context-var)

     ;; macro in cljs.core
     [macro (get (a/public-macros 'cljs.core) sym)] (format-macro (meta macro))

     ;; scoped var
     [var (scoped-var-info env sym context-ns)] (format-var context-ns var)

     ;; scoped macro
     [macro (scoped-macro-info env sym context-ns)] (format-macro macro))))
