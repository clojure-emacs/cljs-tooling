(ns cljs-tooling.test-complete
  (:require [clojure.tools.reader.edn :as edn]
            [clojure.java.io :as io]
            [clojure.walk :as walk]
            [clojure.string :as s]
            [clojure.test :refer :all]
            [cljs-tooling.complete :as cc]
            [cljs-tooling.test-env :as test-env]
            [cljs.core]
            [cljs.core.async.macros]
            [cljs.core.async.impl.ioc-macros]
            [om.core]))

(use-fixtures :once test-env/wrap-test-env)

(defn completions
  [& args]
  (apply cc/completions test-env/*env* args))

(deftest sanity
  (testing "Empty string args equivalent"
    (is (= (completions "")
           (completions "" ""))))

  (testing "Nothing returned for non-existent prefix"
    (is (= '()
           (completions "abcdefghijk")
           (completions "abcdefghijk" "cljs-tooling.test-ns"))))

  (testing "cljs.core candidates returned for new namespaces"
    (is (= (completions "")
           (completions "" "abcdefghijk"))))

  (let [all-candidates (completions "" "cljs-tooling.test-ns")]
    (testing "All candidates have a string for :candidate"
      (is (every? (comp string? :candidate) all-candidates)))

    (testing "All candidates that should have a symbol for :ns, do"
      (let [filter-fn #(not (#{:import :keyword :namespace} (:type %)))
            filtered-candidates (filter filter-fn all-candidates)]
        (is (every? (comp symbol? :ns) filtered-candidates))))

    (testing "All candidates have a valid :type"
      (let [valid-types #{:function
                          :class
                          :keyword
                          :macro
                          :namespace
                          :protocol
                          :protocol-function
                          :record
                          :special-form
                          :type
                          :var}]
        (is (every? (comp valid-types :type) all-candidates))))))

(deftest special-form-completions
  (testing "Special form"
    (is (= '({:candidate "throw" :ns cljs.core :type :special-form})
           (completions "thr")))))

(deftest namespace-completions
  (testing "Namespace"
    (is (= '({:candidate "cljs.core.async.impl.timers" :type :namespace})
           (completions "cljs.core.async.impl.t")
           (completions "cljs.core.async.impl.t" "om.core")
           (completions "cljs.core.async.impl.t" "cljs.core.async"))))

  (testing "Namespace alias"
    (is (= '()
           (completions "timers")
           (completions "timers" "om.core")))
    (is (= '({:candidate "timers" :ns cljs.core.async.impl.timers :type :namespace})
           (completions "timers" "cljs.core.async")))))

(deftest macro-namespace-completions
  (testing "Macro namespace"
    (is (= '()
           (completions "cljs.core.async.macros")
           (completions "cljs.core.async.macros" "om.core")))
    (is (= '({:candidate "cljs.core.async.macros" :type :namespace})
           (completions "cljs.core.async.macros" "cljs.core.async"))))

  (testing "Macro namespace alias"
    (is (= '()
           (completions "ioc")))
    (is (= '({:candidate "ioc" :ns cljs.core.async.impl.ioc-macros :type :namespace})
           (completions "io" "cljs.core.async.impl.ioc-helpers")))
    (is (= '({:candidate "ioc" :ns cljs.core.async.impl.ioc-macros :type :namespace}
             {:candidate "ioc-alts!" :ns cljs.core.async :type :function})
           (completions "ioc" "cljs.core.async")))))

(deftest fn-completions
  (testing "cljs.core fn"
    (is (= '({:candidate "unchecked-add" :ns cljs.core :type :function}
             {:candidate "unchecked-add-int" :ns cljs.core :type :function})
           (completions "unchecked-a")
           (completions "unchecked-a" "cljs.core.async")))
    (is (= '({:candidate "cljs.core/unchecked-add" :ns cljs.core :type :function}
             {:candidate "cljs.core/unchecked-add-int" :ns cljs.core :type :function})
           (completions "cljs.core/unchecked-a")
           (completions "cljs.core/unchecked-a" "cljs.core.async"))))

  (testing "Excluded cljs.core fn"
    (is (= '()
           (completions "unchecked-b" "cljs-tooling.test-ns")))
    (is (= '({:candidate "cljs.core/unchecked-byte" :ns cljs.core :type :function})
           (completions "cljs.core/unchecked-b" "cljs-tooling.test-ns"))))

  (testing "Namespace-qualified fn"
    (is (= '({:candidate "cljs.core.async/sliding-buffer" :ns cljs.core.async :type :function})
           (completions "cljs.core.async/sli")
           (completions "cljs.core.async/sli" "om.core")
           (completions "cljs.core.async/sli" "cljs.core.async"))))

  (testing "Referred fn"
    (is (= '()
           (completions "sli")
           (completions "sli" "om.core")))
    (is (= '({:candidate "sliding-buffer" :ns cljs.core.async :type :function})
           (completions "sli" "cljs-tooling.test-ns"))))

  (testing "Local fn"
    (is (= '({:candidate "sliding-buffer" :ns cljs.core.async :type :function})
           (completions "sli" "cljs.core.async"))))

  (testing "Private fn"
    (is (= '()
           (completions "cljs.core.async/fhno")
           (completions "cljs.core.async/fhno" "om.core")))
    (is (= '({:candidate "fhnop" :ns cljs.core.async :type :var})
           (completions "fhno" "cljs.core.async"))))

  (testing "Anonymous fn"
    (is (= '()
           (completions "cljs.core.async/->t")
           (completions "->t" "cljs.core.async"))))

  (testing "Fn shadowing macro with same name"
    (is (= '({:candidate "identical?" :ns cljs.core :type :function})
           (completions "identical?")))))

(deftest macro-completions
  (testing "cljs.core macro"
    (is (= '({:candidate "cond->" :ns cljs.core :type :macro}
             {:candidate "cond->>" :ns cljs.core :type :macro})
           (completions "cond-")
           (completions "cond-" "om.core")))
    (is (= '({:candidate "cljs.core/cond->" :ns cljs.core :type :macro}
             {:candidate "cljs.core/cond->>" :ns cljs.core :type :macro})
           (completions "cljs.core/cond-")
           (completions "cljs.core/cond-" "om.core"))))

  (testing "Excluded cljs.core macro"
    (is (= '()
           (completions "whil" "cljs-tooling.test-ns")))
    (is (= '({:candidate "cljs.core/while" :ns cljs.core :type :macro})
           (completions "cljs.core/whil" "cljs-tooling.test-ns"))))

  (testing "Namespace-qualified macro"
    (is (= '()
           (completions "cljs.core.async.macros/go")
           (completions "cljs.core.async.macros/go" "om.core")))
    (is (= '({:candidate "cljs.core.async.macros/go" :ns cljs.core.async.macros :type :macro}
             {:candidate "cljs.core.async.macros/go-loop" :ns cljs.core.async.macros :type :macro})
           (completions "cljs.core.async.macros/go" "cljs.core.async"))))

  (testing "Referred macro"
    (is (= '()
           (completions "go-")
           (completions "go-" "om.core")))
    (is (= '({:candidate "go-loop" :ns cljs.core.async.macros :type :macro})
           (completions "go-" "cljs.core.async")))))

(deftest import-completions
  (testing "Import"
    (is (= '()
           (completions "IdGen")
           (completions "IdGen" "cljs.core.async")))
    (is (= '({:candidate "IdGenerator" :type :class})
           (completions "IdGen" "om.core"))))

  (testing "Namespace-qualified import"
    (is (= '()
           (completions "goog.ui.IdGen")
           (completions "goog.ui.IdGen" "cljs.core.async")))
    (is (= '({:candidate "goog.ui.IdGenerator" :type :class})
           (completions "goog.ui.IdGen" "om.core")))))

(deftest keyword-completions
  (testing "Keyword"
    (is (= '({:candidate ":getDisplayName" :type :keyword}
             {:candidate ":getInitialState" :type :keyword})
           (completions ":get"))))

  (testing "Local namespaced keyword"
    (is (= '({:candidate "::some-namespaced-keyword" :ns cljs-tooling.test-ns :type :keyword})
           (completions "::so" "cljs-tooling.test-ns")))

    (is (= '()
           (completions "::i" "cljs-tooling.test-ns"))))

  (testing "Referred namespaced keyword"
    (is (= '({:candidate "::om/id" :ns om.core :type :keyword}
             {:candidate "::om/index" :ns om.core :type :keyword}
             {:candidate "::om/invalid" :ns om.core :type :keyword})
           (completions "::om/i" "cljs-tooling.test-ns")))))

(deftest protocol-completions
  (testing "Protocol"
    (is (= '({:candidate "IIndexed" :ns cljs.core :type :protocol}
             {:candidate "IIterable" :ns cljs.core :type :protocol})
           (completions "II"))))

  (testing "Protocol fn"
    (is (= '({:candidate "-with-meta" :ns cljs.core :type :protocol-function}
             {:candidate "-write" :ns cljs.core :type :protocol-function})
           (completions "-w")))))

(deftest record-completions
  (testing "Record"
    (is (= '({:candidate "TestRecord" :ns cljs-tooling.test-ns :type :record})
           (completions "Te" "cljs-tooling.test-ns")))))

(deftest type-completions
  (testing "Type"
    (is (= '({:candidate "ES6Iterator" :ns cljs.core :type :type}
             {:candidate "ES6IteratorSeq" :ns cljs.core :type :type})
           (completions "ES6I")))))

(deftest options-map-test
  (testing "options-map"
    (is (= '({:candidate "unchecked-add" :ns cljs.core :type :function}
             {:candidate "unchecked-add-int" :ns cljs.core :type :function})
           (completions "unchecked-a")
           (completions "unchecked-a" "cljs.core.async")
           (completions "unchecked-a" {:context-ns "cljs.core.async"})))))

(deftest extra-metadata
  (testing ":arglists"
    (is (= '({:candidate "unchecked-add" :ns cljs.core :type :function :arglists ("[]" "[x]" "[x y]" "[x y & more]")}
             {:candidate "unchecked-add-int" :ns cljs.core :type :function :arglists ("[]" "[x]" "[x y]" "[x y & more]")})
           (completions "unchecked-a" {:context-ns "cljs.core.async", :extra-metadata #{:arglists}}))))

  (testing ":doc"
    (is (= '({:candidate "unchecked-add" :ns cljs.core :type :function :doc "Returns the sum of nums. (+) returns 0."}
             {:candidate "unchecked-add-int" :ns cljs.core :type :function :doc "Returns the sum of nums. (+) returns 0."})
           (completions "unchecked-a" {:context-ns "cljs.core.async", :extra-metadata #{:doc}}))))

  (testing "macro metadata"
    (is (= '({:candidate "defprotocol", :ns cljs.core, :type :macro, :arglists ("[psym & doc+methods]")})
           (completions "defproto" {:context-ns "cljs.user", :extra-metadata #{:arglists}})))))
