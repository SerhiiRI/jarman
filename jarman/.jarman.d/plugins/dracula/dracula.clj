;; -*- mode: clojure; mode: rainbow;  mode: yafolding; -*-
;; add-file-local-variable-prop-line -> mode -> some mode
(ns plugin.dracula.dracula)
(require '[jarman.external :refer
           [register-custom-theme-plugin
            custom-theme-set-faces]])

(def ^:private dracula-color-scheme
  {"black-strong"       "#20222b"
   "black-darker"       "#282a36"
   "black-medium"       "#242630"
   "black-light"        "#393c4b"
   "white"              "#ffffff"
   "white-strong"       "#f8f8f2"
   "hightlight-light"   "#44475a"
   "hightlight-strong"  "#323444"
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
   "font-size"             14.0
   "icon-size"             20
   "underline-size"        2
   "underline-tabbar-size" 1
   "timelife-alert-popup"  3
   "expand-btn-colors" [["#282a36" "#393c4b" "#242630"]
                        ["#20222b" "#44475a" "#1B1C24"]
;;                        ["#44475a" "#5f5faf" "#6272a4"]
                       ;; ["#556b2f" "#667c3f" "#778d4f"]
                        ]})

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
       ;; base color-palette
       c-white                     white
       c-black                     black-strong
       c-red                       red-strong
       c-orange                    orange-strong
       c-yellow                    yellow-strong
       c-green                     green-strong
       c-light-blue                blue2-light
       c-strong-blue               blue-strong
       c-purple                    purple-strong
       
       ;; Default styles !!! Do not using in code, do not add new here !!!
       c-foreground                white
       c-foreground-title          purple-light
       s-foreground                font-size
       c-caret                     yellow-light

       c-layout-background         black-strong
       c-layout-background-light   hightlight-strong
       
       c-compos-background         hightlight-light 
       c-compos-background-light   hightlight-light 
       c-compos-background-dark    black-darker
       c-compos-background-darker  hightlight-strong

       c-background-detail         black-medium
       
       c-border                    hightlight-strong
       c-border-warning            orange-light
       c-border-danger             red-light
       c-border-success            green-light
       
       c-on-focus                  hightlight-strong
       c-on-focus-light            black-light
       c-on-focus-dark             c-on-focus-light
       c-on-focus-detail           black-strong
       
       c-underline                 black-strong
       c-underline-detail          black-strong
       c-underline-light           hightlight-light 
       c-underline-on-focus        green-strong
       c-underline-on-focus-light  green-strong
       s-underline                 underline-size
       
       ;; Icon color
       c-icon                     magenta-strong
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
       c-btn-bg              black-darker
       c-btn-bg-focus        c-on-focus
       c-btn-foreground      c-foreground
       c-btn-underline       c-underline
       c-btn-underline-on-focus c-underline-on-focus
       s-btn-underline       s-underline
       
       ;; slider menu
       c-slider-bg                 black-strong
       c-slider-fg                 c-foreground
       c-slider-bg-on-focus        c-compos-background
       c-slider-underline          black-strong
       c-slider-underline-on-focus c-underline-on-focus-light

       ;; view service
       c-tab-active          c-layout-background
       c-tabbar-bg           c-on-focus-light
       c-tab-underline       c-underline-light
       s-tab-underline       underline-tabbar-size
       
       ;; table
       c-table-select-row-fg c-foreground
       c-table-select-row-bg c-on-focus
       c-table-select-cell   c-foreground

       ;; table header
       c-table-header-bg     black-strong
       c-table-header-fg     c-foreground
       c-table-header-border c-compos-background

       ;; inputs
       c-input-bg            c-compos-background
       c-input-header        c-foreground

       ;; expand button
       c-btn-expand-bg       c-compos-background
       c-btn-expand-fg       c-foreground-title
       c-btn-expand-offset   c-main-menu-bg

       ;; expand item in iputs
       c-item-expand-left    c-compos-background-darker
       c-item-expand-right   blue-light
 
       ;; min-scrollbox
       c-min-scrollbox-bg    c-compos-background-darker

       ;; alert
       c-alert-bg             c-compos-background-light
       c-alert-fg             c-foreground
       c-alert-alert-border   c-border
       c-alert-warning-border c-border-warning
       c-alert-danger-border  c-border-danger
       c-alert-timelife       timelife-alert-popup
       c-alert-success-border c-border-success
       c-alert-history-focus  c-on-focus-light
       c-alert-btn-bg         c-layout-background-light
       c-alert-btn-bg-focus   c-layout-background
       
       ;; Popup box
       c-popup-body-background c-compos-background
       c-popup-head-background c-compos-background-darker
       c-popup-border          c-border))))


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
