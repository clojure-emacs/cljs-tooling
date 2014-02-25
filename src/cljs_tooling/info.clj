(ns cljs-tooling.info
  (:require [cljs-tooling.util.analysis :as a]
            [cljs-tooling.util.misc :as u]))

;;; TODO
(defn format-ns
  [ns]
  (-> (select-keys ns [:doc :author])
      (merge {:file (-> ns :defs first second :file)
              :line 1
              :name (:name ns)
              :ns (:name ns)})))

(defn format-var
  "Format it similarly to metadata on a var"
  [context-ns var]
  (-> (select-keys var [:arglists :name :line :column :file :doc])
      (merge {:name (-> var :name name u/as-sym)
              :ns (-> var :name namespace u/as-sym)})))

(defn info
  "Returns an info map on the symbol in the context of the namespace, resolving aliases.
'sym' can refer to a top-level var, a namespace, or an alias, the context-ns is optional"
  [env sym & [context-ns]]
  (u/cond-let
   ;; an NS
   [ns (a/find-ns env sym)] (format-ns ns)

   ;; ns alias
   [ns-alias (-> (a/aliased-nses env context-ns) (get sym))] (format-ns (a/find-ns env ns-alias))

   ;; var in ns
   [context-var (get (a/ns-vars env context-ns true) sym)] (format-var context-ns context-var)

   ;; scoped var
   [scoped-context (or (get (a/aliased-nses env context-ns) (u/as-sym (namespace sym)))
                       (->> (namespace sym)
                            u/as-sym
                            (a/find-ns env)
                            :name))] (info env (u/as-sym (name sym)) scoped-context)))

