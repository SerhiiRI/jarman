(ns jarman.gui.components.swing
  (:require
   [seesaw.mig]
   [seesaw.core]
   [seesaw.border]
   [seesaw.color]
   [jarman.lang :refer :all]
   [jarman.faces :as face]
   [clojure.java.io])
  (:import
   [java.awt GraphicsEnvironment GraphicsDevice Font MouseInfo]
   [jiconfont.swing IconFontSwing]
   ;; [jiconfont IconFont DefaultIconCode]
   ;; [jiconfont.icons.google_material_design_icons GoogleMaterialDesignIcons]
   ))

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
  (seesaw.core/frame
   :content
   (seesaw.core/scrollable
    (seesaw.mig/mig-panel
     :background  "#dddddd"
     :constraints ["wrap 1" "0px[grow, fill]0px" "0px[fill, top]0px"]
     :border (seesaw.border/empty-border :thickness 10)
     :items (mapv vector items)))
   :title "Jarman" :size [1000 :by 800])
  (seesaw.core/pack!)
  (jarman.gui.components.swing/choose-screen! 0)
  (seesaw.core/show!)))

;;;;;;;;;;;;;;;;;;;;;;;;;
;;; AWT/Swing helpers ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;

;; cutted from 'jarman.gui.components.swing
(defn ^sun.awt.image.ToolkitImage image-scale
  "Function scale image by percent size.
  Return `sun.awt.image.ToolkitImage` type.

  Example:
    (image-scale \"/path/to/photo\" 100)

  See more:
    javadoc `sun.awt.image.ToolkitImage`
    javadoc `sun.awt.Image`"
  ([image-path]
   (if (instance? javax.swing.ImageIcon image-path) image-path
     (seesaw.icon/icon (clojure.java.io/file image-path))))
  ([image-path percent]
   (if (instance? javax.swing.ImageIcon image-path) image-path
    (let [image (.getImage (seesaw.icon/icon (clojure.java.io/file image-path)))
          scaler (comp int #(Math/ceil %) #(* % (/ percent 100.0)))]
      (doto (javax.swing.ImageIcon.)
        (.setImage (.getScaledInstance image
                                       (scaler (.getWidth image))
                                       (scaler (.getHeight image))
                                       java.awt.Image/SCALE_SMOOTH)))))))

(defn register-fonts
  "Description
    Register system font
  
  Example
    (register-fonts
     [(clojure.java.io/resource \"fonts/ubuntu/Ubuntu-MediumItalic.ttf\")
      (clojure.java.io/resource \"fonts/ubuntu/Ubuntu-Medium.ttf\")
      (clojure.java.io/resource \"fonts/ubuntu/Ubuntu-Regular.ttf\")])"
  [font-files]
  (let [^GraphicsEnvironment ge (java.awt.GraphicsEnvironment/getLocalGraphicsEnvironment)]
    (doall
      (doseq [font font-files]
        (.registerFont ge
          (java.awt.Font/createFont java.awt.Font/TRUETYPE_FONT
            (clojure.java.io/input-stream font)))))))

(defn font
  ([font-name]
   (Font. font-name Font/PLAIN face/s-foreground))
  ([font-name size-num]
   (Font. font-name Font/PLAIN size-num)))

(defn icon
  ([ico]       (icon ico face/c-icon 20))
  ([ico color] (icon ico color 20))
  ([ico color size]
   (IconFontSwing/buildIcon ico size (seesaw.color/color color))))

(defn get-mouse-pos
  "Description:
     Return mouse position on screen, x and y.
  Example:
     (get-mouse-pos) => [800.0 600.0]" []
  (let [mouse-pos (.getLocation (MouseInfo/getPointerInfo))
        screen-x  (.getX mouse-pos)
        screen-y  (.getY mouse-pos)]
    [screen-x screen-y]))
