(ns cljs-tooling.test-env
  (:require [clojure.test :refer :all]
            [cljs.analyzer :as ana :refer [*cljs-ns*]]
            [cljs.env :as env]
            [cljs.closure :as closure]
            [cljs.repl :as repl]
            [cljs.repl.rhino :as rhino]))

(defn create-test-env []
  (let [env (env/default-compiler-env)]
    (closure/build "test-resources/test_ns.cljs"
                   {:output-dir "target/out"}
                   env)
    @env))

(def ^:dynamic *env*)

(defn wrap-test-env
  [f]
  (binding [*env* (create-test-env)]
    (f)))

(deftest test-env
  (let [env (create-test-env)]
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
               clojure.string
               om.core
               om.dom)
             (sort (keys (::ana/namespaces env))))))))
