(defproject cljs-tooling "0.2.0"
  :description "Tooling support for cljs"
  :url "https://github.com/clojure-emacs/cljs-tooling"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies []
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.7.0"]
                                  [org.clojure/clojurescript "1.7.189"]
                                  [org.clojure/core.async "0.2.374"]
                                  [org.omcljs/om "1.0.0-alpha28"]]}})
