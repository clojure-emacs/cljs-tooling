(ns cljs-tooling.test-info
  (:require [clojure.tools.reader.edn :as edn]
            [clojure.walk :as walk]
            [clojure.string :as s]
            [clojure.test :as test #?(:clj :refer :cljs :refer-macros) [deftest is testing use-fixtures]]
            [cljs-tooling.info :as info]
            [cljs-tooling.test-env :as test-env]
            [cljs-tooling.util.misc :as u]))

(use-fixtures :once test-env/wrap-test-env)

(defn info
  [& args]
  (apply info/info test-env/*env* args))

(deftest unquote-test
  (is (= [1 2 3] (#'info/unquote-1 '(quote [1 2 3]))))
  (is (= [1 2 3] (#'info/unquote-1 [1 2 3])))
  (is (= nil (#'info/unquote-1 nil))))

(deftest info-test
  (testing "Resolution from current namespace"
    (let [plus (info '+ 'cljs.core)]
      (is (= (:name plus) (-> #'+ meta :name)))
      (is (= (:ns plus) 'cljs.core))))

  (testing "Resolution from current namespace - issue #28"
    (let [res (info 'issue-28 'cljs-tooling.test-ns)]
      (is (= (select-keys res [:arglists :line :column :ns :name])
             '{:arglists ([])
               :line 13
               :column 1
               :ns cljs-tooling.test-ns
               :name issue-28}))
      (is (.endsWith (:file res) "cljs_tooling/test_ns.cljs"))))

  (testing "Resolution from other namespaces"
    (let [plus (info '+ 'cljs.core.async)]
      (is (= (:name plus) (-> #'+ meta :name)))
      (is (= (:ns plus) 'cljs.core))))

  (testing "Namespace itself"
    (let [res (info 'cljs-tooling.test-ns)]
      (is (= (select-keys res [:line :doc :name :ns])
             '{:ns cljs-tooling.test-ns
               :name cljs-tooling.test-ns
               :doc nil
               :line 1}))
      (is (.endsWith (:file res) "cljs_tooling/test_ns.cljs"))))

  (testing "Namespace itself with context - this is how the info middleware sends it"
    (let [res (info 'cljs-tooling.test-ns 'cljs-tooling.test-ns)]
      (is (= (select-keys res [:line :doc :name :ns])
             '{:ns cljs-tooling.test-ns
               :name cljs-tooling.test-ns
               :doc nil
               :line 1}))
      (is (.endsWith (:file res) "cljs_tooling/test_ns.cljs"))))

  (testing "Namespace dependency"
    (let [res (info 'cljs-tooling.test-ns-dep)]
      (is (= (select-keys res [:line :doc :name :ns])
             '{:ns cljs-tooling.test-ns-dep
               :name cljs-tooling.test-ns-dep
               :doc nil
               :line 1}))
      (is (.endsWith (:file res) "cljs_tooling/test_ns_dep.cljs"))))

  (testing "Namespace dependency with context - this is how the info middleware sends it"
    (let [res (info 'cljs-tooling.test-ns-dep 'cljs-tooling.test-ns)]
      (is (= (select-keys res [:line :doc :name :ns])
             '{:ns cljs-tooling.test-ns-dep
               :name cljs-tooling.test-ns-dep
               :doc nil
               :line 1}))
      (is (.endsWith (:file res) "cljs_tooling/test_ns_dep.cljs"))))

  (testing "Namespace itself but cljs.core"
    (is (= (-> (info 'cljs.core) keys sort)
           (sort '(:ns :name :line :file :doc)))))

  (testing "Aliased var"
    (let [res (info 'dispatch/process-messages 'cljs.core.async)]
      (is (= (select-keys res [:ns :name :arglists :line])
             '{:ns cljs.core.async.impl.dispatch
               :name process-messages
               :arglists ([])
               :line 13}))
      (is (.endsWith (:file res) "cljs/core/async/impl/dispatch.cljs"))))

  (testing "Special forms"
    (let [res (info 'def)]
      (is (= res
             '{:ns cljs.core,
               :name def,
               :doc
               "Creates and interns a global var with the name\n  of symbol in the current namespace (*ns*) or locates such a var if\n  it already exists.  If init is supplied, it is evaluated, and the\n  root binding of the var is set to the resulting value.  If init is\n  not supplied, the root binding of the var is unaffected.",
               :arglists nil}))))

  (testing "Fully-qualified var"
    (let [res (info 'clojure.string/trim 'cljs-tooling.test-ns)]
      (is (= (select-keys res [:ns :name :arglists :doc])
             '{:ns clojure.string
               :name trim
               :arglists ([s])
               :doc "Removes whitespace from both ends of string."}))))

  (testing "Namespace alias"
    (let [res (info 'dispatch 'cljs.core.async)]
      (is (= (select-keys res [:ns :name :doc :arglists :line])
             '{:ns cljs.core.async.impl.dispatch
               :name cljs.core.async.impl.dispatch
               :doc nil
               :line 1}))
      (is (.endsWith (:file res) "cljs/core/async/impl/dispatch.cljs"))))

  (testing "Macro namespace"
    (is (apply = '{:ns cljs.core.async.impl.ioc-macros
                   :file "cljs/core/async/impl/ioc_macros.clj"
                   :name cljs.core.async.impl.ioc-macros
                   :doc nil
                   :author nil}
               (map #(select-keys % [:ns :name :file :doc :author])
                    [(info 'cljs.core.async.impl.ioc-macros)
                     (info 'cljs.core.async.impl.ioc-macros 'cljs.core.async)
                     (info 'cljs.core.async.impl.ioc-macros 'cljs-tooling.test-ns)]))))

  (testing "Macro namespace alias"
    (is (= (info 'ioc)
           (info 'ioc 'om.core)
           nil))
    (is (= (info 'ioc 'cljs.core.async)
           '{:author nil
             :doc nil
             :file "cljs/core/async/impl/ioc_macros.clj"
             :line 1
             :name cljs.core.async.impl.ioc-macros
             :ns cljs.core.async.impl.ioc-macros})))

  (testing "cljs.core macro"
    (is (apply = '{:ns cljs.core
                   :doc "Evaluates the exprs in a lexical context in which the symbols in\n  the binding-forms are bound to their respective init-exprs or parts\n  therein. Acts as a recur target."
                   :name loop
                   :arglists ([bindings & body])}
               (map #(select-keys % [:ns :name :doc :arglists])
                    [(info 'loop)
                     (info 'cljs.core/loop)
                     (info 'cljs.core/loop 'cljs.core.async)
                     (info 'loop 'cljs.core)]))))

  (testing "Macro"
    (is (= (info 'go)
           (info 'go 'mount.core)
           nil))
    (is (apply = '{:ns cljs.core.async
                   :name go
                   :file #?(:clj "cljs/core/async.clj"
                            :cljs "cljs/core/async.cljc")
                   :arglists ([& body])}
               (map #(select-keys % [:ns :name :file :arglists])
                    [(info 'cljs.core.async/go)
                     (info 'go 'cljs.core.async)]))))

  (testing "Macro - referred"
    (is (= (info 'go)
           (info 'go 'mount.core)
           nil))
    (is (apply = '{:name on-error
                   :ns mount.tools.macro
                   :arglists ([msg f & {:keys [fail?] :or {fail? true}}])
                   :file "mount/tools/macro.cljc"}
               (map #(select-keys % [:ns :name :file :arglists])
                    [(info 'mount.core/on-error)
                     (info 'on-error 'mount.core)])))))
