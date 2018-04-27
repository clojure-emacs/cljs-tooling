(ns cljs-tooling.test-util
  (:require [clojure.test :as test #?(:clj :refer :cljs :refer-macros) [deftest is testing]]
            [cljs-tooling.util.misc :as u]))

(deftest macros-suffix-add-remove
  (testing "add-ns-macros"
    (is (nil? (u/add-ns-macros nil)))
    (is (= 'mount.tools.macro$macros (u/add-ns-macros 'mount.tools.macro))))

  (testing "remove-macros"
    (is (nil? (u/remove-macros nil)) "it should return nil if input is nil")
    (is (= 'mount.tools.macro (u/remove-macros 'mount.tools.macro)) "it should not change the input if no $macros")
    (is (= 'cljs.core.async/go (u/remove-macros 'cljs.core.async$macros/go)) "it should remove $macros from a namespaced var")
    (is (= 'mount.tools.macro (u/remove-macros 'mount.tools.macro$macros)) "it should remove $macros from a namespace")))

(deftest name-sym
  (is (nil? (u/name-sym nil)))
  (is (= 'unqualified (u/name-sym 'unqualified)))
  (is (= 'sym (u/name-sym 'qualified/sym))))

(deftest namespace-sym
  (is (nil? (u/namespace-sym nil)))
  (is (= 'unqualified (u/namespace-sym 'unqualified)))
  (is (= 'qualified (u/namespace-sym 'qualified/sym))))
