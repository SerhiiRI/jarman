(package-initialize)
(require 'org)
(require 'ox)
(require 'ox-publish)

(setq org-publish-project-alist
      '(("agenda-org"
         :base-directory "~/programs/jarman"
	 :base-extension "org"
	 :exclude ".*"
	 :include ["agenda.org"]
         :publishing-function org-html-publish-to-html
         :publishing-directory "/ssh:root@trashpanda-team.ddns.net:/var/www/html/agenda"
         :style "<link rel=\"stylesheet\" type=\"text/css\" href=\"../themes/org.css\"/>")
	("agenda-issues-org"
         :base-directory "~/programs/jarman"
	 :base-extension "org"
         :publishing-function org-html-publish-to-html
         :publishing-directory "/ssh:root@trashpanda-team.ddns.net:/var/www/html/agenda/issues"
         :style "<link rel=\"stylesheet\" type=\"text/css\" href=\"../themes/org.css\"/>")
	("agenda-org-style"
         :base-directory "~/programs/jarman/docs/themes"
	 :exclude ".*"
	 :include ["org.css"]
         :publishing-function org-publish-attachment
	 :publishing-directory "/ssh:root@trashpanda-team.ddns.net:/var/www/html/agenda/style")	
	;; CHANGELOG
	("agenda-all" :components ("agenda-org" "agenda-issues-org" "agenda-org-style"))))

;; (load-them 'tango)
;; (org-mode)
(org-publish "agenda-all" t)

