(ns cljs-tooling.test-complete
  (:require [clojure.tools.reader.edn :as edn]
            [clojure.java.io :as io]
            [clojure.walk :as walk]
            [clojure.string :as s]
            [clojure.test :refer :all]
            [cljs-tooling.complete :as cc]
            [cljs.core]
            [cljs.core.async.macros]
            [cljs.core.async.impl.ioc-macros]
            [om.core]))

;;; NS metadata

(defn read-analysis
  "Returns a data structure matching a dump of a compiler's env."
  [input]
  (edn/read-string (slurp input)))

(defn test-env
  ;; TODO: dynamically create this from cljs compiler once dumps are supported
  []
  (read-analysis (io/resource "analysis.edn")))

(def env (test-env))

(def completions (partial cc/completions env))

(deftest namespace-completions
  (testing "Namespace"
    (is (= '("cljs.core.async.impl.timers")
           (completions "cljs.core.async.impl.t")
           (completions "cljs.core.async.impl.t" "om.core")
           (completions "cljs.core.async.impl.t" "cljs.core.async"))))

  (testing "Namespace alias"
    (is (= '()
           (completions "timers")
           (completions "timers" "om.core")))
    (is (= '("timers")
           (completions "timers" "cljs.core.async")))))

(deftest macro-namespace-completions
  (testing "Macro namespace"
    (is (= '()
           (completions "cljs.core.async.macros")
           (completions "cljs.core.async.macros" "om.core")))
    (is (= '("cljs.core.async.macros")
           (completions "cljs.core.async.macros" "cljs.core.async"))))

  (testing "Macro namespace alias"
    (is (= '()
           (completions "ioc")
           (completions "ioc" "cljs.core.async")))
    (is (= '("ioc" "ioc-alts!")
           (completions "io" "cljs.core.async.impl.ioc-helpers")))))

(deftest var-completions
  (testing "cljs.core var"
    (is (= '("unchecked-add" "unchecked-add-int")
           (completions "unchecked-a")
           (completions "unchecked-a" "cljs.core.async")))
    (is (= '("cljs.core/unchecked-add" "cljs.core/unchecked-add-int")
           (completions "cljs.core/unchecked-a")
           (completions "cljs.core/unchecked-a" "cljs.core.async"))))

  ;; TODO: Excludes test case.
  #_(testing "Excluded cljs.core var")

  (testing "Namespace-qualified var"
    (is (= '("cljs.core.async/sliding-buffer")
           (completions "cljs.core.async/sli")
           (completions "cljs.core.async/sli" "om.core")
           (completions "cljs.core.async/sli" "cljs.core.async"))))

  (testing "Referred var"
    (is (= '()
           (completions "sli")
           (completions "sli" "om.core")))
    (is (= '("sliding-buffer")
           (completions "sli" "cljs-app.view"))))

  (testing "Local var"
    (is (= '("sliding-buffer")
           (completions "sli" "cljs.core.async")))))

(deftest macro-completions
  (testing "cljs.core macro"
    (is (= '("cond" "cond->" "cond->>" "condp")
           (completions "cond")
           (completions "cond" "om.core")))
    (is (= '("cljs.core/cond" "cljs.core/cond->" "cljs.core/cond->>" "cljs.core/condp")
           (completions "cljs.core/cond")
           (completions "cljs.core/cond" "om.core"))))

  ;; TODO: Excludes test case.
  (testing "Excluded cljs.core macro")

  (testing "Namespace-qualified macro"
    (is (= '()
           (completions "cljs.core.async.macros/go")
           (completions "cljs.core.async.macros/go" "om.core")))
    (is (= '("cljs.core.async.macros/go" "cljs.core.async.macros/go-loop")
           (completions "cljs.core.async.macros/go" "cljs.core.async"))))

  (testing "Referred macro"
    (is (= '()
           (completions "go-")
           (completions "go-" "om.core")))
    (is (= '("go-loop")
           (completions "go-" "cljs.core.async"))))

  (testing "Import"
    (is (= '()
           (completions "IdGen")
           (completions "IdGen" "cljs.core.async")))
    (is (= '("IdGenerator")
           (completions "IdGen" "om.core"))))

  (testing "Namespace-qualified import"
    (is (= '()
           (completions "goog.ui.IdGen")
           (completions "goog.ui.IdGen" "cljs.core.async")))
    (is (= '("goog.ui.IdGenerator")
           (completions "goog.ui.IdGen" "om.core")))))
