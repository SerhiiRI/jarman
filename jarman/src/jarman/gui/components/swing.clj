(ns jarman.gui.components.swing
  (:require
   [seesaw.core]
   [clojure.pprint :refer [cl-format]]
   [jarman.tools.lang :refer :all])
  (:import
   [javax.swing Action KeyStroke]
   [javax.swing AbstractAction InputMap ActionMap]
   [java.awt.event KeyEvent InputEvent ActionEvent]))

;;;;;;;;;;;;;;;;;;;;;;;
;;; SCREEN SETTINGS ;;;
;;;;;;;;;;;;;;;;;;;;;;;

(defn choose-screen! [^javax.swing.JFrame frame ^Number screen-n]
  ;; S(Screen), F(Frame), H(Height), W(Width)
  ;; +------------------[screen-n]---
  ;; | 
  ;; |    V
  ;; |     x=S-X+(S-W/2)-(F-W/2)
  ;; |     y=F-H+(S-H/2)-(F-H/2)
  ;; |
  (where
   ((^java.awt.GraphicsEnvironment ge (java.awt.GraphicsEnvironment/getLocalGraphicsEnvironment))
    (^"[Ljava.awt.GraphicsDevice;" gd (.getScreenDevices ge))
    (screen screen-n if2 #(< -1 % (alength gd)) screen-n 0)
    (S-X (.. (aget gd screen) getDefaultConfiguration getBounds -x))
    (S-Y (.. frame getY))
    (S-H (.. (aget gd screen) getDefaultConfiguration getBounds -height))
    (S-W (.. (aget gd screen) getDefaultConfiguration getBounds -width))
    (F-H (.  frame getWidth))
    (F-W (.  frame getHeight))
    (relative-y S-H do #(- (/ % 2) (/ F-H 2)) do long)
    (relative-x S-W do #(- (/ % 2) (/ F-W 2)) do long))
   (doto frame
     (.setLocation
      (+ relative-x S-X)
      (+ relative-y S-Y)))))




