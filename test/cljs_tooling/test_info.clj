(ns cljs-tooling.test-info
  (:require [clojure.tools.reader.edn :as edn]
            [clojure.java.io :as io]
            [clojure.walk :as walk]
            [clojure.string :as s]
            [clojure.test :refer :all]
            [cljs-tooling.info :as info]
            [cljs-tooling.util.misc :as u]))

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
    ;; test resolution from current ns
    (let [plus (info '+ 'cljs.core )]
      (is (= (:name plus) (-> #'+ meta :name)))
      (is (= (:ns plus) 'cljs.core)))

    ;; test resolution from other ns's
    (let [plus (info '+ 'cljs.core.async)]
      (is (= (:name plus) (-> #'+ meta :name)))
      (is (= (:ns plus) 'cljs.core)))

    ;; test ns itself
    (is (= (-> (info 'cljs.core) keys sort)
           (sort '(:ns :name :line :file :doc))))

    ;; test var through alias
    (is (= (info 'dispatch/process-messages 'cljs.core.async)
           '{:ns cljs.core.async.impl.dispatch,
             :file "/home/gary/dev/personal/quewww/target/cljsbuild-compiler-0/cljs/core/async/impl/dispatch.cljs"
             :column 1,
             :line 13,
             :name process-messages,
             :arglists (quote ([]))}))

    ;; test ns alias
    (is (= (info 'dispatch 'cljs.core.async) 
           '{:ns cljs.core.async.impl.dispatch
             :name cljs.core.async.impl.dispatch
             :line 1
             :file "/home/gary/dev/personal/quewww/target/cljsbuild-compiler-0/cljs/core/async/impl/dispatch.cljs"
             :doc nil}))

    (is (= (info 'clojure.string/trim 'cljs.core.async)
           '{:ns clojure.string, :doc "Removes whitespace from both ends of string.", :file "/home/gary/dev/personal/quewww/target/cljsbuild-compiler-0/clojure/string.cljs", :column 1, :line 132, :name trim, :arglists (quote ([s]))}))
    ))


