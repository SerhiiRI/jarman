;; -*- mode: clojure; mode: rainbow;  mode: yafolding; -*-
;; add-file-local-variable-prop-line -> mode -> some mode

(ns jarman.gui.builtin-themes.jarman-light)
(require 'jarman.plugin.plugin)
(require 'jarman.gui.faces-system)

;;;;;;;;;;;;;;;;;;;;;;;;;
;;; COLOR DEFINITIONS ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;

(def ^:private jarman-light-theme
  "Standart jarman color preset"
  {"clouds"           "#ecf0f1"
   "silver"           "#dfe4ea"
   "concrete"         "#95a5a6"
   "asbestos"         "#7f8c8d"
   "wet-asphalt"      "#34495e"
   "midnight-blue"    "#2c3e50"

   "turquoise"        "#1abc9c"
   "green-sea"        "#16a085"

   "emerald"          "#2ecc71"
   "nephritis"        "#27ae60"

   "peter-river"      "#2492db"
   "belize-hole"      "#0a74b9"

   "amethyst"         "#9b59b6"
   "wisteria"         "#8e44ad"

   "sun-flower"       "#f1c40f"
   "orange"           "#d98c10"

   "carrot"           "#e67e22"
   "pumpkin"          "#d35400"

   "alizarin"         "#e74c3c"
   "pomegranate"      "#c0392b"

   "gray"              "#cccccc"
   "gray-light"        "#eeeeee"
   "gray-light-2"      "#efefef"
   "gray-light-3"      "#f7f7f7"
   "gray-super-light"  "#fefefe"
   "white"             "#ffffff"
   "jarman-blue"       "#96c1ea"
   "jarman-blue-dark"  "#29295e"
   "jarman-blue-light" "#d9ecff"
   "jarman-super-dark" "#020020"

   "underline-size"          2
   "expand-btn-colors" [["#f7f7f7" "#fafafa"]
                        ["#f0f6fa" "#f0f6fa"]
                        ["#ebf7ff" "#ebf7ff"]
                        ["#daeaf5" "#daeaf5"]
                        ["#bfd3e0" "#bfd3e0"]]})

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

(jarman.plugin.plugin/register-custom-theme-plugin
  :name "Jarman Light"
  :description "Built-in jarman default light theme"
  :loader
  (with-jarman-ligth-vars
    (jarman.gui.faces-system/custom-theme-set-faces
     '( ;; -- Theme binder --
       c-foreground         jarman-super-dark
       c-layout-background  gray-light-2
       c-compos-background  white
       c-on-focus           jarman-blue-light

       c-main-menu-bg       gray-light-3
       c-main-menu-vhr      c-main-menu-bg

       cvv-button-expand    expand-btn-colors

       c-btn-bg             gray-super-light
       c-btn-bg-focus       jarman-blue-light
       c-btn-foreground     jarman-super-dark

       c-underline          gray
       c-underline-on-focus jarman-blue
       c-underline-on-mouse jarman-blue

       c-slider-bg          white
       c-slider-underline   c-layout-background
       c-slider-underline-on-focus jarman-blue-light

       c-menu-bar-on-focus  jarman-blue-light
       c-tab-active         gray-super-light
       
       c-input-bg           white
       c-input-header       jarman-super-dark
       
       s-underline          underline-size
       ))))

;; First example
(comment
 (jarman.plugin.plugin/register-custom-theme-plugin
  :name "Jarman Light"
  :description "Built-in jarman default light theme"
  :loader
  (with-jarman-ligth-vars
    (jarman.gui.faces-system/custom-theme-set-faces
     '( ;; -- Theme binder -- 
       underscore-panel     clouds
       button-border-top    underscore-panel
       button-border-bottom underscore-panel
       ;;button-border-left   silver
       )))))
