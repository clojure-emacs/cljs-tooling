.PHONY: test-jvm install-lumo test-lumo repl-lumo

CLOJURE_VERSION ?= 1.9

test-jvm :
	lein with-profile +$(CLOJURE_VERSION),+jvm test

install-lumo :
	npm list -g lumo-cljs || npm install -g lumo-cljs
	lumo --version

LUMO_CLASSPATH = $(shell lein with-profile +lumo,+test classpath)

test-lumo : install-lumo
	lumo -sf -c $(LUMO_CLASSPATH) -m cljs-tooling.test-runner

repl-lumo : install-lumo
	lumo -sfK -c $(LUMO_CLASSPATH) -n 5044
