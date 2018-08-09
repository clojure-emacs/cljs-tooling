(ns cljs-tooling.test-env
  (:require [clojure.test :as test #?(:clj :refer :cljs :refer-macros) [deftest is testing]]
            [clojure.set :as set]
            [cljs-tooling.util.analysis :as a]
            [cljs.env :as env]
            #?@(:clj [[cljs.analyzer.api :as ana]
                      [cljs.build.api :as build]
                      [cljs.compiler.api :as comp]
                      [clojure.java.io :as io]]
                :cljs [[lumo.repl :as repl]])))

(defn create-test-env []
  #?(:clj
     (let [opts (build/add-implicit-options {:cache-analysis true, :output-dir "target/out"})
           env (env/default-compiler-env opts)]
       (comp/with-core-cljs env opts
         (fn []
           (ana/analyze-file env (io/resource "cljs_tooling/test_ns.cljs") opts)))
       @env)

     :cljs
     (do (repl/eval '(require (quote cljs-tooling.test-ns)) 'cljs-tooling.test-env)
         @env/*compiler*)))

(def ^:dynamic *env*)

(defn wrap-test-env
  [f]
  (binding [*env* (create-test-env)]
    (f)))

(deftest test-env
  (let [env (create-test-env)]
    (testing "Test environment"
      (is (set/subset? '#{cljs-tooling.test-ns
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
                          mount.core
                          mount.tools.logger
                          mount.tools.macro}
                       (set (keys (a/all-ns env))))))))
