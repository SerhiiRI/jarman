;; -*- mode: clojure; mode: rainbow;  mode: yafolding; -*-
(ns plugin.jarman-almost-white.blue)
(require '[plugin.jarman-almost-white.jarman-almost-white :refer [with-colors theme-binder]])
(require '[jarman.plugin.plugin :refer [register-custom-theme-plugin]])
(require '[jarman.gui.faces-system :refer [custom-theme-set-faces]])

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


(register-custom-theme-plugin
 :name "Almost White Blue"
 :description  "Jarman Almost White with Blue theme"
 :loader
 (with-colors blue-scheme
   (custom-theme-set-faces theme-binder)))
