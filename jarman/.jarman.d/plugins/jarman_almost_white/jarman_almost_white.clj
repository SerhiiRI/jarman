;; -*- mode: clojure; mode: rainbow;  mode: yafolding; -*-
;; add-file-local-variable-prop-line -> mode -> some mode
(ns plugin.jarman-almost-white.jarman-almost-white)

(def theme-binder
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

(defmacro with-colors
  [color-scheme & body]
  `(do
     ~@(map (fn [[colr hex]] (list 'def (symbol colr) hex)) (seq (eval `~color-scheme)))
     ~@body))

