(package-initialize)
(require 'org)
(require 'ox)
(require 'ox-org)
(require 'ox-latex)
(require 'ox-publish)

(setq org-publish-project-alist
      '(("developer-manual-pdf"
         :base-directory "~/programs/jarman/docs/page"
	 :base-extension "org"
	 :exclude ".*"
	 :include ["developer-manual.org"]
         :publishing-function org-latex-publish-to-pdf
         :publishing-directory "~/programs/jarman/docs/public/doc")
	("developer-manual-org"
         :base-directory "~/programs/jarman/docs/page"
	 :base-extension "org"
	 :exclude ".*"
	 :include ["developer-manual.org"]
         :publishing-function org-org-publish-to-org
         :publishing-directory "~/programs/jarman/docs/public/doc")
	("developer-manual" :components ("developer-manual-org" "developer-manual-pdf"))))

(org-publish "developer-manual" t)

