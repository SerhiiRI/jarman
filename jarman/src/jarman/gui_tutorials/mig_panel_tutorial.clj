(ns jarman.gui-tutorials.mig-panel-tutorial
  (:use
   seesaw.core
   seesaw.mig
   seesaw.style
   seesaw.border
   seesaw.dev))

;; ****************************
;; *
;; *  Mig-panel in quick tip
;; *
;; ****************************

;; (show-options (label))


;; mig-panel example
;; ["wrap 2" "10px[grow, center]5px[grow, fill]10px" "10px[]0px"]
;; wrap 2 - two elements in row
;; 10px[]0px - 10px margin on left and 0px margin on right but for all columns
;; 10px[]0px[]20px - 10px margin on left, 0px margin between, 20px on right and for all columns repeate last definition (I mean 0px[]20px)
;; [grow] - columns in mig should have width or heigh same as parent component, e.g. JFrame can be parent. 
;;          So if JFrame have 100px width, then column in mig will have 100px width, but if inside mig is second column then both will have 50px (100px / 2 = 50px)
;; [fill] - column and component inside mig will have width as fill to value inside component. So if label have text "Hello" then width will be 20px (or close).
;; [200]  - column width
;; [200:300]  - column min-width : max-width
;; [grow, center] - fill to parent and set component to center
;; [(label) "split, span, gap"] - parameters for row inside mig, not for all mig
(def mig (mig-panel
          :constraints ["wrap 2" "10px[grow, center]5px[grow, fill]10px" "10px[]0px"] ;; definition of appearance some like flex in css
          :border (line-border :thickness 3 :color "#f00") ;; border around first mig-panel
          :items [ ;; items vector
                  [(label :text "Tekst w labelu") "gaptop 10"] ;; "gaptop 10" is a margin-top 10px for row
                  ["Po prostu tekst"]
                  [(label :text "Tekst w labelu zajmujący dwie kolumny" :background "#bca") "span 2"] ;; span 2 mean use two cell as one
                  [(label :text "Element w split 2" :background "#abc") "split 2"] ;; split column to two space for component
                  [(label :text "Drugi element w split 2" :background "#abc")]  ;; this label go to upper split 
                  [(mig-panel :constraints ["" "[grow, center]" ""] ;; mig-panel as one component for first mig, but inside have two another component
                              :border (line-border :thickness 3 :color "#666") ;; border around mig-panel inside migpanel
                              :items [;; items vector
                                      [(label :text "Element w mig 2" :background "#a9a")] ;; simple label
                                      [(label :text "Drugi element w mig 2" :background "#a9a")]  ;; simple label
                                      ])]]))

;; create jframe
(def app (seesaw.core/frame
          :title "title"
          :undecorated? false
          :minimum-size [600 :by 200]
          :content mig))

;; run app on middle screen
(-> (doto app (.setLocationRelativeTo nil) pack! show!))
