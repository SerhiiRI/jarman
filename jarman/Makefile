SHELL := /bin/bash
REPO_SCM_DIRECTORY = $(shell pwd)

# COLORS
CYAN=\033[96m\033[1m
GREEN=\033[32m\033[1m
RED=\033[31m\033[1m
NC=\033[0m
BOLD=\033[1m

help:
	@echo -e "${GREEN}${BOLD}NAME${NC}"
	@echo -e "\tJarman Project Manager"
	@echo -e "${GREEN}${BOLD}DESCRIBE${NC}"
	@echo -e "\tJPM is simple GNU/Makefile file created to keeping\n\tclean of all build and project configuration processes\n\tin one place, which can build distributive package\n\tor update some parts of code, or even run some"
	@echo -e "${GREEN}${BOLD}TARGETS${NC}"
	@echo -e "\tproject/gen-font-lib - generating clojure font lib"
	@echo -e "\tproject/gen-icon-lib - generating clojure icon lib"
	@echo -e "\tbuild/jarman-cli - building 'jarman-cli' uberjar"
	@echo -e "\tbuild/jarman-client - building uberjar for central GUI client"
	@echo -e "\tjarman-box - build box with jarmna cli and different tools"
	@echo -e "\thelp - print this message"

project/gen-font-lib: project.clj
	@lein jarman structure --refresh-fonts

project/gen-icon-lib: project.clj
	@lein jarman structure --refresh-icons

build/jarman-cli:
	lein with-profile cli uberjar

build/jarman-client:
	lein with-profile client uberjar

test:
	lein test

jarman-box: build/jarman-cli
	@bash -e "./bin/build-jarman-cli.sh"
