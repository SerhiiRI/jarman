(ns jarman.gui.gui-login
  (:use seesaw.core
        seesaw.border
        seesaw.dev
        seesaw.mig
        seesaw.font)
  (:require [clojure.string :as string]))



(def lbl (label "TRASHPANDA"))
(def lbl2 (label "Heyyy"))

(def field (text "This is text"))

(defn convert-panel [] (vertical-panel :items field))


(def f (frame :title "Jarman-login"
              :width 1000
              :height 800))


(defn display [content]
  (config! f :content content)
  content)

(display (border-panel
          :north (vertical-panel :items (list lbl lbl2))
        
          :vgap 5 :hgap 5 :border 5))

(-> f show!)
;;(move! f :by [100 300])



;;;;;;;;;;




