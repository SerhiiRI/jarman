(ns jarman.gui-tutorials.stylesheet-tutorial
  (:use
   seesaw.core
   seesaw.mig
   seesaw.style
   seesaw.border
   seesaw.dev))

;; ****************************
;; *
;; *  Style sheets like a css
;; *
;; ****************************

;; (show-options (label))

;; create styles sheet
(def my_stylesheet {[:.css1] {:background "#4ca"} ;; Style by class name 
                    [:.css2] {:foreground "#fff"} ;; Style by class name 
                    [:#css3] {:foreground "#fff" :background "#000"} ;; Style by id 
                    [:JLabel] {:border (compound-border (empty-border :thickness 5) (line-border :thickness 3 :color "#a4c"))} ;; Style by component type
                    })

;; simlpe mig with few labels
(def b (mig-panel
        :constraints ["" "10px[grow, fill]10px" "10px[grow, center]10px"]
        :items [[(label :text "Style by one class"   :class :css1)]
                [(label :text "Style by multi class" :class #{:css1 :css2})]
                [(label :text "Style by id" :id :css3)]
                [(label :text "Style by type")]]))

;; create jframe
(def app (seesaw.core/frame
          :title "title"
          :undecorated? false
          :minimum-size [600 :by 200]
          :content b))

;; Set styles
(apply-stylesheet app my_stylesheet)

;; run app on middle screen
(-> (doto app (.setLocationRelativeTo nil) pack! show!))
