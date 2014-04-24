# cljs-tooling

[![Continuous Integration status](https://secure.travis-ci.org/gtrak/cljs-tooling.png)](http://travis-ci.org/gtrak/cljs-tooling)

A Clojure library designed to provide tooling support for ClojureScript.
Currently it provides var info and auto-completion based on compiler state. The completion is a direct port of ninjudd/clojure-complete.

TODO:
imports

## Artifacts

With leiningen:

     [cljs-tooling "0.1.2"]

## Usage

```clojure
;; env is pulled from cljs compiler state
=> (completions @cljs.env/*compiler* "al" 'cljs.core)
("alength" "alter-meta!")
```

## License

Copyright © 2014 Gary Trakhman

Distributed under the Eclipse Public License, the same as Clojure.
