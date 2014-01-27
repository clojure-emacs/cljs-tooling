(ns cljs-tooling.info)

(defn info
  "Returns an info map on the symbol in the context of the namespace, resolving aliases.  'sym' can refer to a top-level var, a namespace, or an alias"
  [ns sym])
