[![Continuous Integration status](https://travis-ci.org/clojure-emacs/cljs-tooling.svg)](http://travis-ci.org/clojure-emacs/cljs-tooling)
[![Dependencies Status](https://versions.deps.co/clojure-emacs/cljs-tooling/status.svg)](https://versions.deps.co/clojure-emacs/cljs-tooling)

# cljs-tooling

A Clojure library designed to provide tooling support for ClojureScript.
Currently it provides var info and auto-completion based on compiler state.

It is the basis for ClojureScript features in
[cider-nrepl](https://github.com/clojure-emacs/cider-nrepl) (and in
CIDER respectively), including source navigation and auto-completion,
but is separate for the sake of test-harnesses and decoupled
development.

## Artifacts

With leiningen:

     [cljs-tooling "0.2.0"]

## Usage

### Var info

```clojure
cljs-tooling.info> (info @cljs.env/*compiler* 'go 'cljs.core.async)
=> {:ns cljs.core.async.macros
    :doc "Asynchronously executes the body, returning immediately to the\n  calling thread. Additionally, any visible calls to <!, >! and alt!/alts!\n  channel operations within the body will block (if necessary) by\n  'parking' the calling thread rather than tying up an OS thread (or\n  the only JS thread when in ClojureScript). Upon completion of the\n  operation, the body will be resumed.\n\n  Returns a channel which will receive the result of the body when\n  completed"
    :file "cljs/core/async/macros.clj"
    :column 1
    :line 4
    :name go
    :arglists ([& body])}
```

### Completion

```clojure
;; env is pulled from cljs compiler state
cljs-tooling.complete> (completions @cljs.env/*compiler* "al" 'cljs.core)
=> ("alength" "alter-meta!")
```

### Self-host ClojureScript

This library is compatible with self-host ClojureScript. In order to try it out in `lumo`, for instance, just do:

```shell
lumo -c $(clojure -Sdeps '{:deps {cljs-tooling {:mvn/version "X.Y.Z"}}}' -Spath)
```

```clojure
cljs.user=> (require '[cljs-tooling.complete :as ctc])
nil
cljs.user=> (ctc/completions @cljs.env/*compiler* "al" 'cljs.core)
cljs.user=> (ctc/completions @cljs.env/*compiler* "al" 'cljs.core)
=> ({:candidate "alength", :type :function, :ns cljs.core}
    {:candidate "alter-meta!", :type :function, :ns cljs.core})
```

## Contributors

* [Gary Trakhman](http://github.com/gtrak)
* [Michael Griffiths](http://github.com/cichli)
* [Juho Teperi](http://github.com/Deraen)
* [Andrea Richiardi](http://github.com/arichiardi)

## License

Copyright © 2014-2018 Gary Trakhman & contributors

Distributed under the Eclipse Public License, the same as Clojure.
