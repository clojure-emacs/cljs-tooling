(ns cljs-tooling.test-info
  (:require [clojure.tools.reader.edn :as edn]
            [clojure.java.io :as io]
            [clojure.walk :as walk]
            [clojure.string :as s]
            [clojure.test :refer :all]
            [cljs-tooling.info :as info]
            [cljs-tooling.test-env :refer [env]]
            [cljs-tooling.util.misc :as u]
            [cljs.core]
            [cljs.core.async.macros]
            [cljs.core.async.impl.ioc-macros]
            [om.core]))

(deftest unquote-test
  (is (= [1 2 3] (#'info/unquote-1 '(quote [1 2 3]))))
  (is (= [1 2 3] (#'info/unquote-1 [1 2 3])))
  (is (= nil (#'info/unquote-1 nil))))

(deftest info-test
  (let [info (partial info/info env)]
    ;; test resolution from current ns
    (let [plus (info '+ 'cljs.core )]
      (is (= (:name plus) (-> #'+ meta :name)))
      (is (= (:ns plus) 'cljs.core)))

    ;; test resolution from other ns's
    (let [plus (info '+ 'cljs.core.async)]
      (is (= (:name plus) (-> #'+ meta :name)))
      (is (= (:ns plus) 'cljs.core)))

    ;; test ns itself
    (is (= (-> (info 'cljs.core) keys sort)
           (sort '(:ns :name :line :file :doc))))

    ;; test var through alias
    (let [res (info 'dispatch/process-messages 'cljs.core.async)]
      (is (= (:ns res) 'cljs.core.async.impl.dispatch))
      (is (.endsWith (:file res) "cljs/core/async/impl/dispatch.cljs"))
      (is (= (:column res) 1))
      (is (= (:line res) 13))
      (is (= (:name res) 'process-messages))
      (is (= (:arglists res) '([]))))

    ;; test ns alias
    (let [res (info 'dispatch 'cljs.core.async)]
      (is (= (:ns res) 'cljs.core.async.impl.dispatch))
      (is (= (:name res) 'cljs.core.async.impl.dispatch))
      (is (= (:line res) 1))
      (is (.endsWith (:file res) "cljs/core/async/impl/dispatch.cljs"))
      (is (nil? (:doc res))))

    (let [res (info 'clojure.string/trim 'cljs.core.async)]
      (is (= (:ns res) 'clojure.string))
      (is (= (:doc res) "Removes whitespace from both ends of string."))
      (is (.endsWith (:file res) "clojure/string.clj"))
      (is (:column res) 1)
      (is (:line res) 132)
      (is (:name res) 'trim)
      (is (:arglists res) '([s])))

    ;; test macro ns
    (is (= (info 'cljs.core.async.macros)
           (info 'cljs.core.async.macros 'cljs.core.async)
           '{:author nil,
             :doc nil,
             :file "cljs/core/async/macros.clj",
             :line 1,
             :name cljs.core.async.macros,
             :ns cljs.core.async.macros}))

    ;; test macro ns alias
    (is (= (info 'ioc)
           (info 'ioc 'cljs.core.async)
           nil))
    (is (= (info 'ioc 'cljs.core.async.impl.ioc-helpers)
           '{:author nil,
             :doc nil,
             :file "cljs/core/async/impl/ioc_macros.clj",
             :line 1,
             :name cljs.core.async.impl.ioc-macros,
             :ns cljs.core.async.impl.ioc-macros}))

    ;; test cljs.core macro
    (is (= (info 'loop)
           (info 'cljs.core/loop)
           (info 'loop 'cljs.core.async)
           (info 'cljs.core/loop 'cljs.core.async)
           '{:ns cljs.core,
             :doc
             "Evaluates the exprs in a lexical context in which the symbols in\n  the binding-forms are bound to their respective init-exprs or parts\n  therein. Acts as a recur target.",
             :file "cljs/core.clj",
             :column 1,
             :line 154,
             :name loop,
             :arglists ([bindings & body])}))
    
    ;; test macro
    (is (= (info 'go)
           (info 'go 'om.core)
           nil))
    (is (= (info 'cljs.core.async.macros/go)
           (info 'go 'cljs.core.async))
        '{:ns cljs.core.async.macros,
          :doc
          "Asynchronously executes the body, returning immediately to the\n  calling thread. Additionally, any visible calls to <!, >! and alt!/alts!\n  channel operations within the body will block (if necessary) by\n  'parking' the calling thread rather than tying up an OS thread (or\n  the only JS thread when in ClojureScript). Upon completion of the\n  operation, the body will be resumed.\n\n  Returns a channel which will receive the result of the body when\n  completed",
          :file "cljs/core/async/macros.clj",
          :column 1,
          :line 4,
          :name go,
          :arglists ([& body])})))
