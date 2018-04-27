(ns cljs-tooling.test-runner
  (:require [cljs.test :as test :include-macros true]
            cljs-tooling.test-info
            cljs-tooling.test-env
            cljs-tooling.test-complete
            cljs-tooling.test-util))

;; setting *assert* dynamically like this only works in self-host
(set! cljs.core/*assert* true)

(defmethod cljs.test/report [:cljs.test/default :end-run-tests] [m]
  (when-not (cljs.test/successful? m)
    (.exit js/process 1)))

(defmethod cljs.test/report [:cljs.test/default :error] [m]
  (cljs.test/inc-report-counter! :error)
  (println "\nERROR in" (cljs.test/testing-vars-str m))
  (when (seq (:testing-contexts (cljs.test/get-current-env)))
    (println (cljs.test/testing-contexts-str)))
  (when-let [message (:message m)] (println message))
  (let [actual (:actual m)
        ex-data (ex-data actual)]
    (if (:cljs.spec.alpha/failure ex-data)
      (do (println "expected:" (pr-str (:expected m)))
          (print "  actual:\n")
          (println (.-message actual)))
      (cljs.test/print-comparison m))
    (when (.-stack actual)
      (println (.-stack actual)))))

(defmethod cljs.test/report [:cljs.test/default :fail] [m]
  (cljs.test/inc-report-counter! :error)
  (println "\nERROR in" (cljs.test/testing-vars-str m))
  (when (seq (:testing-contexts (cljs.test/get-current-env)))
    (println (cljs.test/testing-contexts-str)))
  (when-let [message (:message m)] (println message))
  (let [actual (:actual m)
        ex-data (ex-data actual)]
    (if (:cljs.spec.alpha/failure ex-data)
      (do (println "expected:" (pr-str (:expected m)))
          (print "  actual:\n")
          (println (.-message actual)))
      (cljs.test/print-comparison m))
    (when (.-stack actual)
      (println (.-stack actual)))))

(defn -main [& args]
  (test/run-tests 'cljs-tooling.test-env
                  'cljs-tooling.test-util
                  'cljs-tooling.test-info
                  'cljs-tooling.test-complete))

(set! *main-cli-fn* -main)
