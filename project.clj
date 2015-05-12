(defproject cljs-tooling "0.1.7"
  :description "Tooling support for cljs"
  :url "https://github.com/gtrak/cljs-tooling"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies []
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.6.0"]
                                  [org.clojure/clojurescript "0.0-2760"]
                                  [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                                  [om "0.6.2"]]}})
