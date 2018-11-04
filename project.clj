(defproject cljs-tooling "0.3.0"
  :description "Tooling support for ClojureScript"
  :url "https://github.com/clojure-emacs/cljs-tooling"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :scm {:name "git" :url "https://github.com/clojure-emacs/cljs-tooling"}
  :dependencies []
  :global-vars {*assert* false}

  :deploy-repositories [["clojars" {:url "https://clojars.org/repo"
                                    :username :env/clojars_username
                                    :password :env/clojars_password
                                    :sign-releases false}]]

  :profiles {:dev {:dependencies [[org.clojure/clojure "1.9.0" :scope "test"]
                                  [org.clojure/clojurescript "1.10.439" :scope "test"]
                                  [org.clojure/core.async "0.4.474" :scope "test"]
                                  ;; mount is self-host compatible so better for testing
                                  [mount "0.1.13" :scope "test"]]
                   :resource-paths ["test-resources"]
                   :global-vars {*assert* true}}
             :self-host {:resource-paths ["src" "test" "test-resources"]
                         :exclusions [org.clojure/clojure org.clojure/clojurescript]
                         :dependencies [[andare "0.9.0" :scope "test"]
                                        ;; mount is self-host compatible so better for testing
                                        [mount "0.1.13" :scope "test"]]
                         :global-vars {*assert* true}}})
