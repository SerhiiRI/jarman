(package-initialize)
(require 'org)
(require 'ox)
(require 'ox-publish)

(setq org-publish-project-alist
      '(("jarman-documentation-index"
         :base-directory "~/programs/jarman/docs/page"
	 :base-extension "org"
	 :exclude ".*"
	 :include ["manual.org" "changelog.org" "index.org"]
         :publishing-function org-html-publish-to-html
         :publishing-directory "~/programs/jarman/docs/public/site"
         :style "<link rel=\"stylesheet\" type=\"text/css\" href=\"../themes/org.css\"/>")
	("jarman-org-style"
         :base-directory "~/programs/jarman/docs/themes"
	 :exclude ".*"
	 :include ["org.css"]
         :publishing-function org-publish-attachment
         :publishing-directory "~/programs/jarman/docs/public/site/style")
	("jarman-org-images"
         :base-directory "~/programs/jarman/docs/images"
	 :base-extension "jpg\\|png\\|jpeg"
         :publishing-function org-publish-attachment
         :publishing-directory "~/programs/jarman/docs/public/site/images")
	;; CHANGELOG
	("jarman-all" :components ("jarman-documentation-index" "jarman-org-style" "jarman-org-images"))))

(org-publish "jarman-all" t)
