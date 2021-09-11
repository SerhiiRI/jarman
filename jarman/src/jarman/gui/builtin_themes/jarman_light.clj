;; -*- mode: clojure; mode: rainbow; -*-
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
   "pomegranate"      "#c0392b"})

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
      underscore-panel     clouds
      button-border-top    underscore-panel
      button-border-bottom underscore-panel
      ;;button-border-left   silver
      ))))



