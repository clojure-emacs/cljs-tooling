(ns cljs-tooling.test-info
  (:require [clojure.tools.reader.edn :as edn]
            [clojure.java.io :as io]
            [clojure.walk :as walk]
            [clojure.string :as s]
            [clojure.test :refer :all]
            [cljs-tooling.info :as info]))

;;; NS metadata

(defn read-analysis
  "Returns a data structure matching a dump of a compiler's env."
  [input]
  (edn/read-string (slurp input)))

(defn test-env
  ;; TODO: dynamically create this from cljs compiler once dumps are supported
  []
  (read-analysis (io/resource "analysis.edn")))


(deftest info-test
  (let [env (test-env)
        info (partial info/info env)]

    (is (info '+ 'cljs.core ))

    (info 'cljs.core)
    (info 'dispatch/process-messages 'cljs.core.async)
    (info 'dispatch 'cljs.core.async)
))


