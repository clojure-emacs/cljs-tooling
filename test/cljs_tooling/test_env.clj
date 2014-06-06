(ns cljs-tooling.test-env
  (:require [cljs.analyzer :refer [*cljs-ns*]]
            [cljs.env :refer [with-compiler-env]]
            [cljs.repl :refer [-setup analyze-source load-namespace]]))

(defn- create-env []
  (let [exec-env (cemerick.austin/exec-env)]
    (with-compiler-env (:cljs.env/compiler exec-env)
      (analyze-source "test-resources")
      (-setup exec-env))
    exec-env))

(defonce env (-> (create-env)
                 :cljs.env/compiler
                 deref))

