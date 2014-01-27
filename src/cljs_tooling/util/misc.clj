(ns cljs-tooling.util.misc)

;; from http://www.learningclojure.com/2010/09/clojure-macro-tutorial-part-i-getting.html
(defmacro dbg [x] `(let [x# ~x] (println '~x "=" x#) x#))

;; from https://github.com/flatland/useful/blob/develop/src/flatland/useful/experimental.clj#L31
(defmacro cond-let
  "An implementation of cond-let that is as similar as possible to if-let. Takes multiple
  test-binding/then-form pairs and evalutes the form if the binding is true. Also supports
  :else in the place of test-binding and always evaluates the form in that case.

  Example:
   (cond-let [b (bar 1 2 3)] (println :bar b)
             [f (foo 3 4 5)] (println :foo f)
             [b (baz 6 7 8)] (println :baz b)
             :else           (println :no-luck))"
  [test-binding then-form & more]
  (let [test-binding (if (= :else test-binding) `[t# true] test-binding)
        else-form    (when (seq more) `(cond-let ~@more))]
    `(if-let ~test-binding
       ~then-form
       ~else-form)))

(defn as-sym [x]
  (if x (symbol x)))

