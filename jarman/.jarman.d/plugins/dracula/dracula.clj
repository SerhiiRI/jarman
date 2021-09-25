;; -*- mode: clojure; mode: rainbow;  mode: yafolding; -*-
;; add-file-local-variable-prop-line -> mode -> some mode
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
   "orange-light-2"     "#ffc58d"
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
   "bg4-medium"         "#616161"
   "bg4-strong"         "#444444"
   "fg2-light"          "#e2e2dc"
   "fg2-strong"         "#e4e4e4"
   "fg3-light"          "#ccccc7"
   "fg3-strong"         "#c6c6c6"
   "fg4-light"          "#b6b6b2"
   "fg4-strong"         "#b2b2b2"
   "font-size"             14.0
   "icon-size"             20
   "underline-size"        2
   "underline-tabbar-size" 1
   "timelife-alert-popup"  3
   "expand-btn-colors" [["#303030" "#505050" "#616161"]
                        ["#556b2f" "#667c3f" "#778d4f"]]})

(defmacro ^:private with-dracula-colors
  [& body]
  `(do
     ~@(map (fn [[colr hex]] (list 'def (symbol colr) hex)) (seq dracula-color-scheme))
     ~@body))


(register-custom-theme-plugin
  :name "Dracula"
  :description "Dracula theme"
  :loader
  (with-dracula-colors
    (custom-theme-set-faces
     '( ;; -- Theme binder --
       ;; Default styles !!! Do not using in code, do not add new here !!!
       c-foreground                orange-strong
       s-foreground                font-size
       c-caret                     orange-light-2

       c-layout-background         bg2-light
       c-layout-background-light   bg3-light
       
       c-compos-background         bg4-light
       c-compos-background-light   bg3-strong
       c-compos-background-dark    bg4-strong
       c-compos-background-darker  hightlight-strong

       c-background-detail         c-layout-background
       
       c-border                    hightlight-strong
       c-border-warning            orange-light
       c-border-danger             red-light
       c-border-success            green-light
       
       c-on-focus                  hightlight-light
       c-on-focus-light            bg3-light
       c-on-focus-dark             c-on-focus-light
       c-on-focus-detail           c-layout-background
       
       c-underline                 bg2-light
       c-underline-detail          c-layout-background
       c-underline-light           bg3-light
       c-underline-on-focus        orange-strong
       c-underline-on-focus-light  orange-strong
       s-underline                 underline-size
       
       ;; Icon color
       c-icon                     c-foreground
       s-icon                     icon-size
       c-icon-focus               c-caret
       c-icon-close-focus         c-border-danger
       c-icon-info                cyan-light
       c-icon-warning             c-border-warning
       c-icon-danger              c-border-danger
       c-icon-success             c-border-success
       
       cvv-button-expand        expand-btn-colors ;; expand button lvls colors
       
       ;;----------------------------------
       ;;Custom elements  !!! Do not repeat in code! Create new per component !!!
       
       ;; main menu
       c-main-menu-bg        c-layout-background-light
       c-main-menu-vhr       c-main-menu-bg       

       ;; menu bars
       c-icon-btn-focus      c-on-focus-dark
       c-menu-bar-on-focus   c-on-focus
       
       ;; basic button
       c-btn-bg              c-compos-background-dark
       c-btn-bg-focus        c-on-focus
       c-btn-foreground      c-foreground
       c-btn-underline       c-underline
       c-btn-underline-on-focus c-underline-on-focus
       s-btn-underline       s-underline
       
       ;; slider menu
       c-slider-bg                 c-layout-background
       c-slider-fg                 c-foreground
       c-slider-bg-on-focus        c-compos-background
       c-slider-underline          c-underline-detail
       c-slider-underline-on-focus c-underline-on-focus-light

       ;; view service
       c-tab-active          c-on-focus-light
       c-tabbar-bg           c-background-detail
       c-tab-underline       c-underline-light
       s-tab-underline       underline-tabbar-size
       
       ;; table
       c-table-select-row-fg c-foreground
       c-table-select-row-bg c-on-focus
       c-table-select-cell   c-foreground

       ;; table header
       c-table-header-bg     c-compos-background-light
       c-table-header-fg     c-foreground
       c-table-header-border c-compos-background

       ;; inputs
       c-input-bg            c-compos-background
       c-input-header        c-foreground

       ;; expand button
       c-btn-expand-bg       c-compos-background
       c-btn-expand-fg       c-foreground
       c-btn-expand-offset   c-compos-background

       ;; expand item in iputs
       c-item-expand-left    bg4-strong
       c-item-expand-right   bg4-medium
 
       ;; min-scrollbox
       c-min-scrollbox-bg    bg4-medium

       ;; alert
       c-alert-bg             c-compos-background-light
       c-alert-fg             c-foreground
       c-alert-alert-border   c-border
       c-alert-warning-border c-border-warning
       c-alert-danger-border  c-border-danger
       c-alert-timelife       timelife-alert-popup
       c-alert-success-border c-border-success
       c-alert-history-focus  c-on-focus-light
       
       ;; Popup box
       c-popup-body-background c-compos-background
       c-popup-head-background c-compos-background-darker
       c-popup-border          c-border
       
       ))))


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


;; First example
(comment
  (register-custom-theme-plugin
  :name "Dracula"
  :description "Dracula theme"
  :loader
  (with-dracula-colors
    (custom-theme-set-faces
     '( ;; -- FACE BINDINGS --
       underscore-panel     blue-light
       button-border-top    yellow-light
       button-border-bottom underscore-panel)))))
