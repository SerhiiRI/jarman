(ns plugin.dracula.dracula)
(require '[jarman.plugin.plugin :refer [register-custom-theme-plugin]])
(require '[jarman.gui.faces-system :refer [custom-theme-set-faces]])

(def ^:private dracula-color-scheme
  {"black"              "#282a36"
   "white"              "#ffffff"
   "white-strong"       "#f8f8f2"
   "hightlight-light"   "#44475a"
   "hightlight-strong"  "#303030"
   "blue-light"         "#6272a4"
   "blue-strong"        "#5f5faf"
   "blue2-light"        "#0189cc" 
   "blue2-strong"       "#0087ff"
   "cyan-light"         "#8be9fd"
   "cyan-strong"        "#87d7ff"
   "green-light"        "#50fa7b"
   "green-strong"       "#5fff87"
   "orange-light"       "#ffb86c"
   "orange-strong"      "#ffaf5f"
   "magenta-light"      "#ff79c6"
   "magenta-strong"     "#ff87d7"
   "purple-light"       "#bd93f9"
   "purple-strong"      "#af87ff"
   "red-light"          "#ff5555"
   "red-strong"         "#ff8787"
   "yellow-light"       "#f1fa8c"
   "yellow-strong"      "#ffff87"
   ;; "Other" colors
   "bg2-light"          "#373844"
   "bg2-strong"         "#121212"
   "bg3-light"          "#464752"
   "bg3-strong"         "#262626"
   "bg4-light"          "#565761"
   "bg4-strong"         "#444444"
   "fg2-light"          "#e2e2dc"
   "fg2-strong"         "#e4e4e4"
   "fg3-light"          "#ccccc7"
   "fg3-strong"         "#c6c6c6"
   "fg4-light"          "#b6b6b2"
   "fg4-strong"         "#b2b2b2"})

(defmacro ^:private with-dracula-colors
  [& body]
  `(do
     ~@(map (fn [[colr hex]] (list 'def (symbol colr) hex)) (seq dracula-color-scheme))
     ~@body))

;;;;;;;;;;;;;;;;;;;;
;;; REGISTRATION ;;;
;;;;;;;;;;;;;;;;;;;;

;; Explaination
;; Any of this aproach, must to do one thing, is just
;; recompiling some `face` variable with color you want
;; and wrapp that recompiling to funciton
;; like:
;;
;; 1. You define your variable
;;   (def local-color-variable "#fff111"
;; 
;; 2. Create Function which replace this value by recompiling
;;   (defn my-theme-loader [] 
;;     (def some-system-faces local-color-variable))
;;
;; 3. Register as plugin
;;   (register-custom-theme-plugin
;;     :name "My theme" :description "Some description"
;;     :loader my-theme-loader )

;; Here you can saw theme bindign by my Helper toolkit
;; Where you discribe variable name and your colors
;; also use as variable
;;
;; Example:
;;    (def automatic-binder
;;     (with-dracula-colors
;;       (custom-theme-set-faces
;;        '( ;;    -- FACE BINDINGS --
;;          underscore-panel     blue-light
;;          button-border-top    yellow-light
;;          button-border-bottom underscore-panel))))

;; But if you do not want to going this way, you can
;; also write yourself implemnetation for binding,
;; or for color scheme.
;; 
;; Example: 
;;    (defn manual-binder []
;;      (println "** Refresh sequence")
;;      (def jarman.gui.faces/underscore-panel    (get dracula-color-scheme "blue-light"))
;;      (def jarman.gui.faces/button-border-top   (get dracula-color-scheme "yellow-light"))
;;      (def jarman.gui.faces/button-border-bottom jarman.gui.faces/underscore-panel ))

;; The main aim is gave you freedom to implementaion
;; your customization in whay you beter choose for
;; self. 
;; But better aproach is use a `custom-theme-set-faces`
;; which also control and test some variable's, also
;; make some logs

(register-custom-theme-plugin
 :name "Dracula"
 :description "Dracula theme"
 :loader
 (with-dracula-colors
   (custom-theme-set-faces
    '(;; -- FACE BINDINGS --
      underscore-panel     blue-light
      button-border-top    yellow-light
      button-border-bottom underscore-panel))))
