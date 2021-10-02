(ns jarman.icon-test
  (:require [seesaw.core :as c])
  (:import
   (jiconfont IconFont DefaultIconCode)
   (jiconfont.icons.google_material_design_icons GoogleMaterialDesignIcons)
   (jiconfont.swing IconFontSwing)
   (java.io InputStream)
   (java.awt Color)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; USING GOOGLE MATERIAL ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(IconFontSwing/register (GoogleMaterialDesignIcons/getIconFont))
(def gamepad (IconFontSwing/buildIcon GoogleMaterialDesignIcons/GAMEPAD 40 (Color. 0 150 0)))
;; JLabel label = new JLabel(icon);

(comment
  (-> (c/frame :content (c/label :text icon))
      c/pack!
      c/show!))

(defmacro icon
  ([icon-symbol]
   `(icon-function ~icon-symbol 20))
  ([icon-symbol size]
   `(icon-function ~(symbol "GoogleMaterialDesignIcons" (name icon-symbol)) ~size)))

(icon GAMEPAD 10)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; BUT IF YOU REGISTRATE CUSTOM FONT DO NEXT ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment
  (IconFontSwing/register
   (proxy [IconFont] []
     (^String getFontFamily []
      "GoogleMaterial")
     (^InputStream getFontInputStream []
      "./mmm.ttf")))

  (def iconCode (DefaultIconCode. "GoogleMaterial" \u0047))
  (def icon (IconFontSwing/buildIcon iconCode 40 (Color. 0 150 0))))
;; JLabel label = new JLabel(icon);

