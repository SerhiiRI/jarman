;; -*- mode: clojure; mode: rainbow;  mode: yafolding; -*-
;; add-file-local-variable-prop-line -> mode -> some mode
(ns jarman.gui.builtin-themes.jarman-ligth
  (:require [jarman.interaction]))

;;;;;;;;;;;;;;;;;;;;;;;;;
;;; COLOR DEFINITIONS ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;

(def ^:private jarman-light-theme
  "Standart jarman color preset"
  {"font-light"               "Ubuntu Light"
   "font-regular"             "Ubuntu Regular"
   "font-medium"              "Ubuntu Medium"
   "font-bold"                "Ubuntu Bold"
   "font-italic-light"        "Ubuntu Light Italic"
   "font-italic-regular"      "Ubuntu Italic"
   "font-italic-medium"       "Ubuntu Medium Italic"
   "font-italic-bold"         "Ubuntu Bold Italic"
   "font-mono-light"          "JetBrains Mono Light"
   "font-mono-regular"        "JetBrains Mono Regular"
   "font-mono-medium"         "JetBrains Mono Medium"
   "font-mono-bold"           "JetBrains Mono Bold"
   "font-mono-italic-light"   "JetBrains Mono Light Italic"
   "font-mono-italic-regular" "JetBrains Mono Italic"
   "font-mono-italic-medium"  "JetBrains Mono Medium Italic"
   "font-mono-italic-bold"    "JetBrains Mono Bold Italic"

   "clouds"             "#ecf0f1"
   "silver"             "#dfe4ea"
   "smoke-white"        "#f5f5f5"
   "concrete"           "#95a5a6"
   "asbestos"           "#7f8c8d"
   "wet-asphalt"        "#34495e"
   "midnight-blue"      "#2c3e50"

   "turquoise"          "#1abc9c"
   "green-sea"          "#16a085"

   "emerald"            "#2ecc71"
   "nephritis"          "#27ae60"
   "cyan-light"         "#E2FBDE"
   "peter-river"        "#2492db"
   "belize-hole"        "#0a74b9"

   "amethyst"           "#9b59b6"
   "wisteria"           "#8e44ad"

   "sun-flower"         "#f1c40f"
   "orange"             "#d98c10"

   "carrot"             "#e67e22"
   "pumpkin"            "#d35400"

   "alizarin"           "#e74c3c"
   "pomegranate"        "#c0392b"

   "gray"               "#C7C9D1"
   "gray-dark"          "#bbbbbb"
   "gray-dark-2"        "#aaaaaa"
   "gray-dark-light"    "#dddddd"
   "gray-light-2"       "#e8e9ec"
   "gray-light-3"       "#f8f9fb"
   "gray-super-light"   "#fefefe"
   "white"              "#ffffff"
   "jarman-blue"        "#3d85c6"
   "jarman-blue-green"  "#2c7375"
   "jarman-blue-dark"   "#206494"
   "jarman-blue-light"  "#96c1ea"
   "jarman-blue-strong" "#0a2436"
   "jarman-super-dark"  "#020020"

   "font-h1"                34
   "font-h2"                30
   "font-h3"                26
   "font-h4"                22
   "font-h5"                19
   "font-h6"                17
   "font-size"              14.0
   "icon-size"              20
   "underline-size"         2
   "underline-tabbar-size"  1
   "timelife-alert-popup"   3
   "expand-btn-colors" [["#e8e9ec" "#e8e9ec" "#f1f1f3"] ;; ["#ececec" "#ececec" "#f7f7f7"]
                        ["#e0e0e0" "#e0e0e0" "#efefef"]
                        ["#d5d5d5" "#d5d5d5" "#dfdfdf"]
                        ["#c5c5c5" "#c5c5c5" "#cfcfcf"]]})

;;;;;;;;;;;;;;
;;; HELPER ;;;
;;;;;;;;;;;;;;

(defmacro ^:private with-jarman-ligth-vars
  [& body]
  `(do
     ~@(map (fn [[colr hex]] (list 'def (symbol colr) hex)) (seq jarman-light-theme))
     ~@body))

;;;;;;;;;;;;;;;;;;;;
;;; REGISTRATION ;;;
;;;;;;;;;;;;;;;;;;;;

(jarman.interaction/register-theme
  :name "Jarman Light"
  :description "Built-in jarman default light theme"
  :loader
  (with-jarman-ligth-vars
    (jarman.interaction/custom-theme-set-faces
      '(;; -- Theme binder --

        ;; Fonts
        f-light                     font-light
        f-regular                   font-regular
        f-medium                    font-medium
        f-bold                      font-bold
        f-italic-light              font-italic-light
        f-italic-regular            font-italic-regular
        f-italic-medium             font-italic-medium
        f-italic-bold               font-italic-bold
        f-mono-light                font-mono-light
        f-mono-regular              font-mono-regular
        f-mono-medium               font-mono-medium
        f-mono-bold                 font-mono-bold
        f-mono-italic-light         font-mono-italic-light
        f-mono-italic-regular       font-mono-italic-regular
        f-mono-italic-medium        font-mono-italic-medium
        f-mono-italic-bold          font-mono-italic-bold

        ;; Default colors
        c-white                     white
        c-black                     jarman-blue-strong
        c-red                       pomegranate
        c-orange                    carrot
        c-yellow                    sun-flower
        c-green                     green-sea
        c-light-blue                jarman-blue
        c-strong-blue               jarman-blue-dark
        c-purple                    wisteria

        ;; Default styles  !!! Do not using in code, do not add new here !!!
        s-foreground-h1            font-h1
        s-foreground-h2            font-h2
        s-foreground-h3            font-h3
        s-foreground-h4            font-h4
        s-foreground-h5            font-h5
        s-foreground-h6            font-h6
        s-foreground               font-size
        c-foreground               jarman-blue-strong
        c-foreground-title         jarman-blue-strong
        c-caret                    jarman-blue-dark

        c-layout-background        gray-light-2 ;; "#e8e9ec"
        c-layout-background-light  gray-light-3 ;; "#f8f9fb"

        c-compos-background        white
        c-compos-background-light  gray-super-light
        c-compos-background-dark   gray
        c-compos-background-darker gray-dark-light

        c-background-detail        c-layout-background

        c-border                   jarman-blue-light
        c-border-warning           sun-flower
        c-border-danger            alizarin
        c-border-success           emerald

        c-on-focus                 jarman-blue-light
        c-on-focus-light           gray-super-light
        c-on-focus-dark            gray-dark-light
        c-on-focus-detail          c-layout-background

        c-underline                gray
        c-underline-detail         c-layout-background
        c-underline-light          gray-light-3
        c-underline-on-focus       c-border
        c-underline-on-focus-light jarman-blue-light
        s-underline                underline-size

        ;; Icon color
        c-icon                     jarman-blue-dark
        s-icon                     icon-size
        c-icon-focus               belize-hole
        c-icon-close-focus         c-border-danger
        c-icon-info                c-border
        c-icon-warning             c-border-warning
        c-icon-danger              c-border-danger
        c-icon-success             c-border-success

        cvv-button-expand          expand-btn-colors ;; expand button lvls colors

        ;;----------------------------------
        ;;Custom elements  !!! Do not repeat in code! Create new per component !!!

        ;; main menu
        c-main-menu-bg        c-layout-background-light
        c-main-menu-vhr       c-main-menu-bg

        ;; menu bars
        c-icon-btn-focus      c-on-focus-dark
        c-menu-bar-on-focus   c-on-focus

        ;; button
        c-btn-bg              c-compos-background-light
        c-btn-bg-focus        smoke-white
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
        c-tab-active          c-background-detail
        c-tabbar-bg           c-on-focus-light
        c-tab-underline       c-underline-light
        s-tab-underline       underline-tabbar-size

        ;; table
        c-table-select-row-fg c-foreground
        c-table-select-row-bg c-on-focus
        c-table-select-cell   c-foreground

        ;; table header
        c-table-header-bg     c-background-detail
        c-table-header-fg     c-foreground
        c-table-header-border c-compos-background

        ;; inputs
        c-input-bg            c-compos-background
        c-input-header        c-foreground

        ;; expand item in iputs
        c-item-expand-left    c-compos-background-darker
        c-item-expand-right   cyan-light

        ;; min-scrollbox
        c-min-scrollbox-bg    gray

        ;; expand button
        c-btn-expand-bg       c-compos-background
        c-btn-expand-fg       c-foreground-title
        c-btn-expand-offset   c-compos-background

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

        ;; popup box
        c-popup-body-background c-compos-background
        c-popup-head-background c-compos-background-darker
        c-popup-border          c-border))))

