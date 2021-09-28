;; -*- mode: clojure; mode: rainbow;  mode: yafolding; -*-
;; add-file-local-variable-prop-line -> mode -> some mode
(ns plugin.jarman-almost-white.jarman-almost-white)

(def theme-binder
  '( ;; -- Theme binder --
       ;; base color-palette
       c-white                     white
       c-black                     black
       c-red                       red-strong
       c-orange                    orange
       c-yellow                    yellow
       c-green                     green
       c-light-blue                blue-light
       c-strong-blue               blue-dark
       c-purple                    purple
     
       ;; Default styles !!! Do not using in code, do not add new here !!!
       c-foreground                black
       c-foreground-title          c-foreground
       s-foreground                font-size
       c-caret                     hightlight

       c-layout-background         white
       c-layout-background-light   grey-light
       
       c-compos-background         c-layout-background
       c-compos-background-light   grey-strong
       c-compos-background-dark    c-layout-background-light
       c-compos-background-darker  c-compos-background-light

       c-background-detail         c-layout-background
       
       c-border                    c-layout-background-light
       c-border-warning            hightlight
       c-border-danger             red-strong
       
       c-on-focus                  c-compos-background-light
       c-on-focus-light            c-layout-background-light
       c-on-focus-dark             c-compos-background-light
       c-on-focus-detail           c-compos-background-light
       
       c-underline                 c-compos-background-light
       c-underline-detail          c-layout-background
       c-underline-light           c-compos-background-light
       c-underline-on-focus        hightlight
       c-underline-on-focus-light  hightlight
       s-underline                 underline-size
       
       ;; Icon color
       c-icon                      c-foreground
       s-icon                      icon-size
       c-icon-focus                c-caret
       c-icon-close-focus          c-border-danger
       c-icon-info                 cyan-light
       c-icon-warning              c-border-warning
       c-icon-danger               c-border-danger
       
       cvv-button-expand           expand-btn-colors ;; expand button lvls colors
       
       ;;----------------------------------
       ;;Custom elements  !!! Do not repeat in code! Create new per component !!!
       
       ;; main menu  
       c-main-menu-bg              c-layout-background-light
       c-main-menu-vhr             c-main-menu-bg       

       ;; menu bars
       c-icon-btn-focus            c-on-focus-dark
       c-menu-bar-on-focus         c-on-focus
       
       ;; basic button
       c-btn-bg                    c-compos-background-dark
       c-btn-bg-focus              c-on-focus
       c-btn-foreground            c-foreground
       c-btn-underline             c-compos-background-dark
       c-btn-underline-on-focus    c-underline-on-focus
       s-btn-underline             s-underline
       
       ;; slider menu
       c-slider-bg                 c-layout-background
       c-slider-fg                 c-foreground
       c-slider-bg-on-focus        c-compos-background
       c-slider-underline          c-underline-detail
       c-slider-underline-on-focus c-underline-on-focus-light

       ;; view service
       c-tab-active                c-on-focus-light
       c-tabbar-bg                 c-background-detail
       c-tab-underline             c-underline-light
       s-tab-underline             underline-tabbar-size
       
       ;; table
       c-table-select-row-fg       c-foreground
       c-table-select-row-bg       c-on-focus
       c-table-select-cell         c-foreground

       ;; table header
       c-table-header-bg           c-compos-background-light
       c-table-header-fg           c-foreground
       c-table-header-border       c-compos-background

       ;; inputs
       c-input-bg                  c-layout-background 
       c-input-header              c-foreground

       ;; expand button
       c-btn-expand-bg             c-layout-background-light
       c-btn-expand-fg             c-foreground
       c-btn-expand-offset         c-compos-background
       
       ;; expand item in iputs
       c-item-expand-left          cyan-light
       c-item-expand-right         c-layout-background

       ;; min-scrollbox
       c-min-scrollbox-bg          c-layout-background-light

       ;; alert
       c-alert-bg                  c-compos-background-light
       c-alert-fg                  c-foreground
       c-alert-alert-border        c-border
       c-alert-warning-border      c-border-warning
       c-alert-danger-border       c-border-danger
       c-alert-timelife            timelife-alert-popup

       ;; Popup box
       c-popup-body-background     c-compos-background
       c-popup-head-background     c-compos-background-darker
       c-popup-border              c-border))

(defmacro with-colors
  [color-scheme & body]
  `(do
     ~@(map (fn [[colr hex]] (list 'def (symbol colr) hex)) (seq (eval `~color-scheme)))
     ~@body))

