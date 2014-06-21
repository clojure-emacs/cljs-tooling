(ns cljs-tooling.test-env
  (:require [cemerick.austin :refer [exec-env]]
            [clojure.test :refer :all]
            [cljs.analyzer :refer [*cljs-ns*]]
            [cljs.env :refer [with-compiler-env]]
            [cljs.repl :refer [-setup analyze-source load-namespace]]))

(defn- create-env []
  (let [env (exec-env)]
    (with-compiler-env (:cljs.env/compiler env)
      (analyze-source "test-resources")
      (-setup env))
    env))

(defonce env (-> (create-env)
                 :cljs.env/compiler
                 deref))

(deftest test-env
  (testing "Test environment"
    (is (= '(cljs-tooling.test-ns
             cljs.core
             cljs.core.async
             cljs.core.async.impl.buffers
             cljs.core.async.impl.channels
             cljs.core.async.impl.dispatch
             cljs.core.async.impl.ioc-helpers
             cljs.core.async.impl.protocols
             cljs.core.async.impl.timers
             clojure.browser.event
             clojure.browser.net
             clojure.browser.repl
             clojure.browser.repl.client
             clojure.string
             om.core
             om.dom)
           (-> env :cljs.analyzer/namespaces keys sort)))))
