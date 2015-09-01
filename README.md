# cljs-tooling

[![Continuous Integration status](https://travis-ci.org/clojure-emacs/cljs-tooling.svg)](http://travis-ci.org/clojure-emacs/cljs-tooling)

A Clojure library designed to provide tooling support for ClojureScript.
Currently it provides var info and auto-completion based on compiler state.

It is the basis for clojurescript features in [clojure-emacs/cider-nrepl](https://github.com/clojure-emacs/cider-nrepl) including source navigation and autocompletion, but is separate for the sake of test-harnesses, decoupled development.

## Artifacts

With leiningen:

     [cljs-tooling "0.1.7"]

## Usage

```clojure
;; env is pulled from cljs compiler state
=> (completions @cljs.env/*compiler* "al" 'cljs.core)
("alength" "alter-meta!")
```

## Contributors
* Gary Trakhman
* Michael Griffiths (@cichli)
* Juho Teperi (@Deraen)

## License

Copyright © 2014 Gary Trakhman

Distributed under the Eclipse Public License, the same as Clojure.
