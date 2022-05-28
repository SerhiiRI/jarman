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
	@echo -e "\tbuild/jarman-client - building uberjar for central GUI client"
	@echo -e "\thelp - print this message"

project/gen-font-lib: project.clj
	@lein jarman structure --refresh-fonts

project/gen-icon-lib: project.clj
	@lein jarman structure --refresh-icons

build/jarman-client:
	lein with-profile client uberjar

build/jarman-client+cli:
	lein with-profile client+cli uberjar

test:
	lein test


cli/jarman/pkg/list:
	@lein jarman pkg --list
cli/jarman/pkg/list-repo:
	@lein jarman pkg --list-repo
cli/jarman/pkg/update-package:
	@lein jarman pkg --update-package
cli/jarman/pkg/build:
	@lein jarman pkg --build
cli/jarman/pkg/help:
	@lein jarman pkg --help

cli/jarman/structure/refresh-fonts:
	@lein jarman structure --refresh-fonts
cli/jarman/structure/refresh-icons:
	@lein jarman structure --refresh-icons
cli/jarman/structure/help:
	@lein jarman structure --help

cli/jarman/data/install:
	@lein jarman data --install
cli/jarman/data/delete:
	@lein jarman data --delete
cli/jarman/data/backup:
	@lein jarman data --backup
cli/jarman/data/app-start:
	@lein jarman data --app-start
cli/jarman/data/app-close:
	@lein jarman data --app-close
cli/jarman/data/crash:
	@lein jarman data --crash
cli/jarman/data/log:
	@lein jarman data --log
cli/jarman/data/clear:
	@lein jarman data --clear
cli/jarman/data/info:
	@lein jarman data --info
cli/jarman/data/meta-clean:
	@lein jarman data --meta-clean
cli/jarman/data/meta-persist:
	@lein jarman data --meta-persist
cli/jarman/data/meta-refresh:
	@lein jarman data --meta-refresh
cli/jarman/data/view-clean:
	@lein jarman data --view-clean
cli/jarman/data/view-persist:
	@lein jarman data --view-persist
cli/jarman/data/view-refresh:
	@lein jarman data --view-refresh
cli/jarman/data/help:
	@lein jarman data --help


