# cljs-complete

[![Continuous Integration status](https://secure.travis-ci.org/gtrak/cljs-complete.png)](http://travis-ci.org/gtrak/cljs-complete)

A Clojure library designed to auto-complete clojurescript based on compiler state.  It is a direct port from ninjudd/clojure-complete.

TODO:
imports

## Artifacts
With leiningen:

     [cljs-complete "0.1.0"]


## Usage

;; env is pulled from cljs compiler state
=> (completions @cljs.env/*compiler* "al" 'cljs.core)
("alength" "alter-meta!")

## License

Copyright © 2014 Gary Trakhman

Distributed under the Eclipse Public License, the same as Clojure.
