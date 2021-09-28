;; -*- mode: clojure; mode: rainbow;  mode: yafolding; -*-
(ns plugin.jarman-almost-white.gray)
(require '[plugin.jarman-almost-white.jarman-almost-white :refer
           [with-colors theme-binder]])
(require '[jarman.external :refer
           [register-custom-theme-plugin
            custom-theme-set-faces]])

(def ^:private grey-scheme
  {"black"              "#333333"
   "white"              "#ffffff"
   "grey-light"         "#eeeeee"
   "grey-strong"        "#dddddd"
   "cyan-light"         "#E2FBDE"
   "yellow"             "#f1fa8c"
   "green"              "#00ff00"
   "blue-light"         "#3d85c6"
   "blue-dark"          "#206494"
   "purple"             "#5f5faf"
   "hightlight"         "#fda50f"
   "orange"             "#ffb86c"
   "red-strong"         "#ff0000"
   "font-size"             14.0
   "icon-size"             20
   "underline-size"        2
   "underline-tabbar-size" 1
   "timelife-alert-popup"  3
   "expand-btn-colors" [["#dddddd" "#dddddd" "#eeeeee"]
                        ["#cccccc" "#bcbcbc" "#bbbbbb"]]})

(register-custom-theme-plugin
 :name "Almost White Grey"
 :description  "Jarman Almost White with Grey theme"
 :loader
 (with-colors grey-scheme
   (custom-theme-set-faces theme-binder)))


