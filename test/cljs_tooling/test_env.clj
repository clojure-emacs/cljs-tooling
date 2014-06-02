(ns cljs-tooling.test-env
  (:require [cljs.analyzer :refer [*cljs-ns*]]
            [cljs.env :refer [with-compiler-env]]
            [cljs.repl :refer [-setup analyze-source load-namespace]]))

(def exec-env (cemerick.austin/exec-env))

(with-compiler-env (:cljs.env/compiler exec-env)
  (binding [*cljs-ns* 'cljs.user]
    (analyze-source "test-resources")
    (-setup exec-env)))

(def env (-> exec-env
             :cljs.env/compiler
             deref))
