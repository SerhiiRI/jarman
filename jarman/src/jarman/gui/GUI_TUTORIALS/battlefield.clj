(ns jarman.gui.gui-tutorials.battlefield
  (:use seesaw.core
        seesaw.border
        seesaw.dev
        seesaw.mig
        seesaw.swingx
        seesaw.color
   seesaw.style
        )
  (:import (javax.swing JLayeredPane)))


;; New jframe
;; (do (doto (seesaw.core/frame
;;            :title "title"
;;            :undecorated? false
;;            :minimum-size [600 :by 400]
;;            :content (label))
;;       (.setLocationRelativeTo nil) seesaw.core/pack! seesaw.core/show!))

(def my_stylesheet {[:.label] {:background "#4ca"
                               :text "Chuj"
                               :listen [:mouse-clicked (fn [e] (alert "Dupa"))]}})


(def lbl
  (fn [](label
         :class :label
         :text "Test")))

(def inp
  (fn [](text
       :size [200 :by 20])))

(def mig
  (fn [] (mig-panel
       :constraints ["" "[grow, center]" ""]
       :items [[(vertical-panel :items [(lbl) (inp)])]])))

(def frm (seesaw.core/frame
          :title "title"
          :undecorated? false
          :minimum-size [600 :by 400]
          :content (mig)))


;; Set styles
(apply-stylesheet (to-root frm) my_stylesheet)

;; New jframe
(do (doto frm
      (.setLocationRelativeTo nil) seesaw.core/pack! seesaw.core/show!))

;; (.invalidate frm)
;; (.validate frm)
;; (.repaint frm)


(def JLP (let [JLP (new JLayeredPane)]
           (.setBackground JLP (new java.awt.Color 0 0 0 0))
           (doseq [i [(label :text "A" :bounds [0 0 200 200]) (label :text "B" :bounds [100 100 200 200])]]
             (do
               (.add JLP i (new Integer 1))))
           JLP))

(def mig (mig-panel
          :constraints ["" "" ""]
          :items [[JLP]]))

(do (doto (seesaw.core/frame
           :title "title"
           :undecorated? false
           :minimum-size [600 :by 400]
           :content (vertical-panel :items [(scrollable (mig-panel 
                                                         :constraints ["" "[fill, grow]" "[fill, grow]"]
                                                         :items [[JLP]]))]))
      (.setLocationRelativeTo nil) seesaw.core/pack! seesaw.core/show!))