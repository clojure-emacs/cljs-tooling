(defproject cljs-tooling "0.3.1"
  :description "Tooling support for ClojureScript"
  :url "https://github.com/clojure-emacs/cljs-tooling"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :scm {:name "git" :url "https://github.com/clojure-emacs/cljs-tooling"}

  :deploy-repositories [["clojars" {:url "https://clojars.org/repo"
                                    :username :env/clojars_username
                                    :password :env/clojars_password
                                    :sign-releases false}]]

  :profiles {:provided [:1.8]

             :dev {:global-vars {*assert* true}}

             ;; TODO: remove all test-time dependencies
             :test {:dependencies [[mount "0.1.15"]]
                    :resource-paths ["test-resources"]}

             :1.8 {:dependencies [[org.clojure/clojure "1.8.0"]
                                  [org.clojure/clojurescript "1.8.51"]
                                  [javax.xml.bind/jaxb-api "2.3.1"]]}

             :1.9 {:dependencies [[org.clojure/clojure "1.9.0"]
                                  [org.clojure/clojurescript "1.9.946"]
                                  [javax.xml.bind/jaxb-api "2.3.1"]]}

             :1.10 {:dependencies [[org.clojure/clojure "1.10.0"]
                                   [org.clojure/clojurescript "1.10.63"]]}

             :master {:repositories [["snapshots" "https://oss.sonatype.org/content/repositories/snapshots"]]
                      :dependencies [[org.clojure/clojure "1.11.0-master-SNAPSHOT"]
                                     [org.clojure/clojurescript "1.10.439"]]}

             :jvm {:dependencies [[org.clojure/core.async "0.4.490"]]}

             :lumo {:dependencies [[andare "0.10.0"]]
                    :exclusions [org.clojure/clojure org.clojure/clojurescript]}})
