(ns plugin.jarman-twilight.jarman-twilight)
(require '[jarman.plugin.plugin :refer [register-custom-theme-plugin]])
(require '[jarman.gui.faces-system :refer [custom-theme-set-faces]])


(def flatui-colors-alist
  "List of FlatUI colors. Each element has the form (NAME . HEX). "
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

(defmacro with-twilight-color-variables
  "`let' bind all colors defined in `flatui-colors-alist' around BODY.
  Also bind `class' to ((class color) (min-colors 89))."
  [& body]
  `(do
     ~@(map (fn [[colr hex]] (list 'def (symbol colr) hex)) (seq flatui-colors-alist))
     ~@body))


(register-custom-theme-plugin
 :name "Twilight Bright "
 :description "Jarman Twilight Bright theme"
 :loader
 (with-twilight-color-variables
   (custom-theme-set-faces
    '(;; -- FACE BINDINGS --
      underscore-panel     clouds
      button-border-top    silver
      button-border-bottom underscore-panel
      button-border-right   silver))))


