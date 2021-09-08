(ns plugin.dracula.dracula)
(require 'jarman.plugin.plugin)
(require 'jarman.gui.faces-system)


'(;; Upstream theme color
  (dracula-bg      "#282a36" "unspecified-bg" "unspecified-bg") ; official background
  (dracula-fg      "#f8f8f2" "#ffffff" "brightwhite") ; official foreground
  (dracula-current "#44475a" "#303030" "brightblack") ; official current-line/selection
  (dracula-comment "#6272a4" "#5f5faf" "blue")        ; official comment
  (dracula-cyan    "#8be9fd" "#87d7ff" "brightcyan")  ; official cyan
  (dracula-green   "#50fa7b" "#5fff87" "green")       ; official green
  (dracula-orange  "#ffb86c" "#ffaf5f" "brightred")   ; official orange
  (dracula-pink    "#ff79c6" "#ff87d7" "magenta")     ; official pink
  (dracula-purple  "#bd93f9" "#af87ff" "brightmagenta") ; official purple
  (dracula-red     "#ff5555" "#ff8787" "red")         ; official red
  (dracula-yellow  "#f1fa8c" "#ffff87" "yellow")      ; official yellow
  ;; Other colors
  (bg2             "#373844" "#121212" "brightblack")
  (bg3             "#464752" "#262626" "brightblack")
  (bg4             "#565761" "#444444" "brightblack")
  (fg2             "#e2e2dc" "#e4e4e4" "brightwhite")
  (fg3             "#ccccc7" "#c6c6c6" "white")
  (fg4             "#b6b6b2" "#b2b2b2" "white")
  (other-blue      "#0189cc" "#0087ff" "brightblue"))

(defmacro ^:private with-dracula-vars
  [& body]
  `(do
     ~@(map (fn [[colr hex]] (list 'def (symbol colr) hex)) (seq jarman-light-theme))
     ~@body))

(jarman.plugin.plugin/register-custom-theme-plugin
 :name "Dracula"
 :description "Classical dracula view"
 :loader
 (with-jarman-ligth-vars
   (jarman.gui.faces-system/custom-theme-set-faces
    '( ;; -- Theme binder -- 
      underscore-panel     clouds
      button-border-top    underscore-panel
      button-border-bottom underscore-panel
      ;;button-border-left   silver
      ))))
