(ns cljs-tooling.test-env
  (:require [cljs-tooling.util.analysis :as a]
            [cljs.analyzer.api :as ana]
            [cljs.build.api :as build]
            [cljs.compiler.api :as comp]
            [cljs.env :as env]
            [clojure.java.io :as io]
            [clojure.test :refer :all]))

(defn create-test-env []
  (let [opts (build/add-implicit-options {:cache-analysis true, :output-dir "target/out"})
        env (env/default-compiler-env opts)]
    (comp/with-core-cljs env opts
      (fn []
        (ana/analyze-file env (io/file "test-resources/test_ns.cljs") opts)))

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
               cljs.user
               clojure.string
               om.core
               om.dom
               om.util)
             (sort (keys (a/all-ns env))))))))
