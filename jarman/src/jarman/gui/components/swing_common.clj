(ns jarman.gui.components.swing-common
  (:require [jarman.faces :as face]))

(defn dimension
  ([^java.awt.Dimension dimension]
   (java.awt.Dimension. dimension))
  ([^Long width, ^Long height]
   (java.awt.Dimension. width height)))

(defn point
  ([^java.awt.Point point]
   (java.awt.Point. point))
  ([^Long x, ^Long y]
   (java.awt.Point. x y)))

(defn font
  ([font-name]
   (java.awt.Font. font-name java.awt.Font/PLAIN face/s-foreground))
  ([font-name size-num]
   (java.awt.Font. font-name java.awt.Font/PLAIN size-num)))

(defn icon
  ([ico]       (icon ico face/c-icon 20))
  ([ico color] (icon ico color 20))
  ([ico color size]
   (jiconfont.swing.IconFontSwing/buildIcon ico size (seesaw.color/color color))))
