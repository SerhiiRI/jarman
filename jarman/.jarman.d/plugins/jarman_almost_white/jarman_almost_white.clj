;; -*- mode: clojure; mode: rainbow;  mode: yafolding; -*-
;; add-file-local-variable-prop-line -> mode -> some mode
(ns plugin.jarman-almost-white.jarman-almost-white)
(require '[jarman.plugin.plugin :refer [register-custom-theme-plugin]])
(require '[jarman.gui.faces-system :refer [custom-theme-set-faces]])

(def ^:private grey-scheme
  {"black"              "#333333"
   "white"              "#ffffff"
   "grey-light"         "#eeeeee"
   "grey-strong"        "#dddddd"
   "cyan-light"         "#E2FBDE"
   "orange-light"       "#fda50f" 
   "red-strong"         "#ff0000"
   "font-size"             14.0
   "icon-size"             20
   "underline-size"        2
   "underline-tabbar-size" 1
   "timelife-alert-popup"  3
   "expand-btn-colors" [["#dddddd" "#dddddd" "#eeeeee"]
                        ["#cccccc" "#bcbcbc" "#bbbbbb"]]})

(def ^:private blue-scheme
  {"black"              "#29295e"
   "white"              "#ffffff"
   "grey-light"         "#eef3f7"
   "grey-strong"        "#dce7ef"
   "cyan-light"         "#E2FBDE"
   "orange-light"       "#2aa1ae"  
   "red-strong"         "#ff0000"
   "font-size"             14.0
   "icon-size"             20
   "underline-size"        2
   "underline-tabbar-size" 1
   "timelife-alert-popup"  3
   "expand-btn-colors" [["#dce7ef" "#dce7ef" "#eef3f7"]
                        ["#cbdbe7" "#bacfdf" "#b9cfdf"]]})

(def theme-faces
  '( ;; -- Theme binder --
       ;; base color-palette
       c-white                     white
       c-black                     black
       c-red                       red-strong
       c-orange                    orange-light
       c-yellow                    orange-light
       c-green                     cyan-light
       c-light-blue                grey-light
       c-strong-blue               grey-strong
       c-purple                    grey-strong
     
       ;; Default styles !!! Do not using in code, do not add new here !!!
       c-foreground                c-black
       s-foreground                font-size
       c-caret                     c-orange

       c-layout-background         c-white
       c-layout-background-light   c-light-blue
       
       c-compos-background         c-white
       c-compos-background-light   c-strong-blue
       c-compos-background-dark    c-light-blue
       c-compos-background-darker  c-strong-blue

       c-background-detail         c-white
       
       c-border                    c-light-blue
       c-border-warning            c-orange
       c-border-danger             c-red
       
       c-on-focus                  c-strong-blue
       c-on-focus-light            c-light-blue
       c-on-focus-dark             c-strong-blue
       c-on-focus-detail           c-strong-blue
       
       c-underline                 c-strong-blue
       c-underline-detail          c-white
       c-underline-light           c-strong-blue
       c-underline-on-focus        c-orange
       c-underline-on-focus-light  c-orange
       s-underline                 underline-size
       
       ;; Icon color
       c-icon                     c-foreground
       s-icon                     icon-size
       c-icon-focus               c-caret
       c-icon-close-focus         c-border-danger
       c-icon-info                c-green
       c-icon-warning             c-border-warning
       c-icon-danger              c-border-danger
       
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
       c-btn-underline       c-compos-background-dark
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
       c-input-bg            c-white 
       c-input-header        c-foreground

       ;; expand button
       c-btn-expand-bg       c-light-blue
       c-btn-expand-fg       c-foreground
       c-btn-expand-offset   c-compos-background
       
       ;; expand item in iputs
       c-item-expand-left    c-green
       c-item-expand-right   c-white

       ;; min-scrollbox
       c-min-scrollbox-bg    c-light-blue

       ;; alert
       c-alert-bg             c-compos-background-light
       c-alert-fg             c-foreground
       c-alert-alert-border   c-border
       c-alert-warning-border c-border-warning
       c-alert-danger-border  c-border-danger
       c-alert-timelife       timelife-alert-popup

       ;; Popup box
       c-popup-body-background c-compos-background
       c-popup-head-background c-compos-background-darker
       c-popup-border          c-border))

(defmacro ^:private with-colors
  [color-scheme & body]
  `(do
     ~@(map (fn [[colr hex]] (list 'def (symbol colr) hex)) (seq (eval `~color-scheme)))
     ~@body))

(register-custom-theme-plugin
 :name "Almost White Grey"
 :description  "Jarman Almost White with Grey theme"
 :loader
 (with-colors grey-scheme
   (custom-theme-set-faces theme-faces)))

(do
 (jarman.plugin.plugin/do-load-theme "Jarman Almost White with Blue")
 (jarman.gui.faces-system/faces-list-out-all-with-values))

(register-custom-theme-plugin
 :name "Almost White Blue"
 :description  "Jarman Almost White with Blue theme"
 :loader
 (with-colors blue-scheme
   (custom-theme-set-faces theme-faces)))

