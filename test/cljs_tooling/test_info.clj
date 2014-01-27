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


;; TODO: :imports

(deftest info-test
  (let [env (test-env)
        completions (partial cc/completions env)]
    (is (= '("alength" "alter-meta!")
           (completions "al" 'cljs.core)))

    ;; make sure clojure refers work
    (is (= '("alength" "alter-meta!")
           (completions "al" 'not-cljs.core)))
    
    (is (= '("dispatch/process-messages")
           (completions "dispatch/p" 'cljs.core.async)))

    (is (= '("cljs.core/alter-meta!")
           (completions "cljs.core/alt" 'cljs.core)))

    (is (= '("cljs-app.core" "cljs-app.js-prot" "cljs-app.node-bits" "cljs-app.view" "cljs-app.websockets" "cljs.core" "cljs.core.async" "cljs.core.async.impl.buffers" "cljs.core.async.impl.channels" "cljs.core.async.impl.dispatch" "cljs.core.async.impl.ioc-helpers" "cljs.core.async.impl.protocols" "cljs.core.async.impl.timers" "cljs.reader")
           (completions "cljs")))

    (is (= '("cljs.core.async" "cljs.core.async.impl.buffers" "cljs.core.async.impl.channels" "cljs.core.async.impl.dispatch" "cljs.core.async.impl.ioc-helpers" "cljs.core.async.impl.protocols" "cljs.core.async.impl.timers")
           (completions "cljs.core.async")))))
