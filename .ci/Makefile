.DEFAULT_GOAL:=main
SHELL:=/bin/bash
# -----
FOLDER_HASH:=$(shell echo $RANDOM | md5sum | head -c 20)
FOLDER_PREFIX:=jarman
FOLDER:=${FOLDER_PREFIX}${FOLDER_HASH}
WD:=${FOLDER}/jarman
GITHUB_PASS:=$(shell cat "github_auth" | head -2 | tail -1)
GITHUB_USER:=$(shell cat "github_auth" | head -1)
GITHUB_REPO:=github.com/SerhiiRI/jarman.git
PROJECT_BRANCH:=master
# -----
CYAN=\033[96m\033[1m
GREEN=\033[32m\033[1m
RED=\033[31m\033[1m
NC=\033[0m
BOLD=\033[1m


props:
	@echo "CI Folder: " ${FOLDER}
	@echo "Working directory" ${WD}
	@echo "GitHub user:" ${GITHUB_USER}
	@echo "GitHub pass:" ${GITHUB_PASS}
	@echo "Branch: " ${PROJECT_BRANCH}

clone:
	@echo "Clone repository"
	git clone https://${GITHUB_USER}:${GITHUB_PASS}@${GITHUB_REPO} ${FOLDER}
	cd ./${FOLDER}; git switch ${PROJECT_BRANCH}

step/leiningen/deps:
	@echo "--------------------"
	@echo "Install dependencies"
	cd ./${WD}; lein deps

step/project/gen-font-lib:
	@echo "--------------------"
	@echo "Generate font lib"
	cd ./${WD}; make project/gen-font-lib

step/project/gen-icon-lib:
	@echo "--------------------"
	@echo "Generate icons lib"
	cd ./${WD}; make project/gen-icon-lib

step/test:
	@echo "--------------------"
	@echo "Run tests"
	cd ./${WD}; make test

step/build-client:
	@echo "--------------------"
	@echo "Build Jarman client"
	cd ./${WD}; make build/jarman-client+cli

step/clean-storage:
	@echo "--------------------"
	@echo "Clean folder storage"
	rm -rf ${FOLDER}

step/finish:
	@echo "--------------------"
	@echo "Successfull build!"

main: props\
	clone\
	step/leiningen/deps\
	step/project/gen-font-lib\
	step/project/gen-icon-lib\
	step/test\
	step/build-client\
	step/clean-storage\
	step/finish

help:
	@echo -e "${GREEN}${BOLD}NAME${NC}"
	@echo -e "\tJarman CI/CD script"
	@echo -e "${GREEN}${BOLD}DESCRIBE${NC}"
	@echo -e "\tWith reason that most CI/CD environment not support\n\tx-org desktop environment building process will never\n\tbeing complete. So for checking and building you need\n\tto run whole ci/cd process localy or server with x-org\n\tenvironment"
	@echo -e "${GREEN}${BOLD}INSTALLATION${NC}"
	@echo -e "\tMake some folder and move this Makefile to it. After\n\tcreate neer a file 'github_auth' which must contain\n\tonly two lines:\n\t\t+-------------\n\t\t|<github username>\\\\n\n\t\t|<github token or pass>\\\\n"
	@echo -e "${GREEN}${BOLD}TARGETS${NC}"
	@echo -e "\tprops - show properties for script"
	@echo -e "\tmain(default) - central entrypoint to run building"
	@echo -e "\tstep/**/* - some building sequence step"
	@echo -e "\thelp - print this message"


