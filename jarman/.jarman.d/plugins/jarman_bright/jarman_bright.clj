;; (ns jarman.themes.jarman-bright
;;   (:require
;;    [jarman.tools.lang :refer :all] 
;;    [clojure.spec.alpha :as s]
;;    [jarman.gui.gui-tools :as gtool]
;;    [jarman.gui.gui-components :as gcomp]
;;    [jarman.gui.gui-calendar :as calndr]
;;    [jarman.logic.sql-tool :refer [select!]]
;;    [jarman.logic.connection :as db]
;;    [jarman.logic.state :as state]
;;    [jarman.plugin.spec :as spec]
;;    [jarman.plugin.data-toolkit :as query-toolkit]
;;    [jarman.resource-lib.icon-library :as icon]
;;    [jarman.config.dot-jarman-param :refer [setq]]))

;; (def blue-dark  "#7ac5cd")
;; (def blue-light "#009acd")
;; (def white-perl "#F5f5f5")
;; (def gray-light "#Dcdcdc")

;; (defn jarman-light-theme []
;;   (setq button {:background white-perl :foreground gray-light}
;;         central {:background blue-light :foreground gray-light}
;;         cursor {:background cursor}
;;         region {:background selection}
;;         highlight {:foreground blue-2 :background blue-2bg}
;;         hl-line {:background hl-line}
;;         minibuffer-prompt {:foreground orange-1 :background orange-1bg}
;;         escape-glyph {:foreground purple-1 :background  purple-1bg}))

;; (deftheme jarman-light-theme
;;   "Jarman light themes")



;; (defvar component-border-bottom-only )
;; (defvar component-border-standart )
;; (defvar component-border-hovere )




(defvar flatui-colors-alist
  '(("clouds"          . "#ecf0f1")
    ("silver"          . "#dfe4ea")
    ("concrete"        . "#95a5a6")
    ("asbestos"        . "#7f8c8d")
    ("wet-asphalt"     . "#34495e")
    ("midnight-blue"   . "#2c3e50")

    ("turquoise"       . "#1abc9c")
    ("green-sea"       . "#16a085")

    ("emerald"         . "#2ecc71")
    ("nephritis"       . "#27ae60")

    ("peter-river"     . "#2492db")
    ("belize-hole"     . "#0a74b9")

    ("amethyst"        . "#9b59b6")
    ("wisteria"        . "#8e44ad")

    ("sun-flower"      . "#f1c40f")
    ("orange"          . "#d98c10")

    ("carrot"          . "#e67e22")
    ("pumpkin"         . "#d35400")

    ("alizarin"        . "#e74c3c")
    ("pomegranate"     . "#c0392b"))
  "List of FlatUI colors.
Each element has the form (NAME . HEX). ")

(defmacro flatui/with-color-variables (&rest body)
  "`let' bind all colors defined in `flatui-colors-alist' around BODY.
Also bind `class' to ((class color) (min-colors 89))."
  (declare (indent 0))
  `(let ((class '((class color) (min-colors 89)))
         ,@(mapcar (lambda (cons)
                     (list (intern (car cons)) (cdr cons)))
                   flatui-colors-alist))
     ,@body))
