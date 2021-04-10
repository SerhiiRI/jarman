(ns jarman.gui-tutorials.forms-tutorial
  (:use [seesaw [core :exclude (separator)] forms]
        seesaw.dev))

;; **********************************
;; *
;; *  forms-panel as simple table
;; *
;; **********************************

;; create forms panel
(def myForm (forms-panel
           "pref,3dlu,100dlu,8dlu,pref,4dlu,100dlu" ;; Columns size
           :items [(separator "General") ;; Separate with title
                   "Company" (text) ;; components in row
                   "Contact" (text) ;; components in row
                   (separator "Propeller") ;; Separate with title
                   "PTI/kW"  (text) "Power/kW" (text) ;; components in row
                   "R/mm"    (text) "D/mm"     (text) ;; components in row
                   ]
           :default-dialog-border? true))

;; create jframe 
(def myFrame (seesaw.core/frame
              :title "title"
              :undecorated? false
              :minimum-size [600 :by 400]
              :content myForm))

;; run app
(-> (doto myFrame (.setLocationRelativeTo nil) seesaw.core/pack! seesaw.core/show!))