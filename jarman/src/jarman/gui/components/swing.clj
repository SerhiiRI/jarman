(ns jarman.gui.components.swing
  (:require
   [seesaw.core :as c]
   [seesaw.border  :as b]
   [seesaw.mig]
   [jarman.faces              :as face]
   ;; --
   [clojure.pprint :refer [cl-format]]
   [jarman.lang :refer :all])
  (:import
   [java.awt GraphicsEnvironment GraphicsDevice]
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

(defn active-screens []
 (let [env (GraphicsEnvironment/getLocalGraphicsEnvironment)
       device-count (count (.getScreenDevices env))]
   (for [[idx ^GraphicsDevice device] (map-indexed vector (.getScreenDevices env))]
     {:idx idx
      :width        (.. device getDisplayMode getWidth)
      :height       (.. device getDisplayMode getHeight)
      :refresh-rate (.. device getDisplayMode getRefreshRate)
      :bit-depth    (.. device getDisplayMode getBitDepth)
      :idSting      (.. device getIDstring)
      :bound-height (.. device getDefaultConfiguration getBounds -height)
      :bound-width  (.. device getDefaultConfiguration getBounds -width)})))

(defn quick-frame [items]
 (-> 
  (c/frame
   :content
   (c/scrollable
    (seesaw.mig/mig-panel
     :background  face/c-compos-background-darker
     :constraints ["wrap 1" "0px[grow, fill]0px" "0px[fill, top]0px"]
     :border (b/empty-border :thickness 10)
     :items (mapv vector items)))
   :title "Jarman" :size [1000 :by 800])
  (c/pack!)
  (jarman.gui.components.swing/choose-screen! 0)
  (c/show!)))

