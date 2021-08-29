# COLORS
CYAN=\033[96m\033[1m
GREEN=\033[32m\033[1m
RED=\033[31m\033[1m
NC=\033[0m
BOLD=\033[1m

help:
	@echo -e "${GREEN}${BOLD}NAME${NC}"
	@echo -e "\tJarman Publication Manager"
	@echo -e "${GREEN}${BOLD}DESCRIBE${NC}"
	@echo -e "\tPublication manager help to create documentation\n\tto the jarman architecutre like static site\n\tgenerator, help to push on web info like changelog,\n\t news and developer manual"
	@echo -e "${GREEN}${BOLD}TARGETS${NC}"
	@echo -e "\tmanual/build-local - building developer manual book to /public/doc"
	@echo -e "\tsite/build-local - build site and push to local /public/site dir."
	@echo -e "\tsite/build-remote - build site and push to remote server"
	@echo -e "\tsite/clean - clean /public/site directory"
	@echo -e "\thelp - print this message"

manual/build-local:
	emacs --batch -l ../docs/el/publish-manual-local.el

site/build-local:
	emacs --batch -l ../docs/el/publish-site-local.el

site/build-remote:
	emacs --batch -l ../docs/el/publish-site-remote.el

site/clean:
	rm -rf ../docs/public/site

