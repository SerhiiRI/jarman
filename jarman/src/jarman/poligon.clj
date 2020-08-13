(ns jarman.poligon
  (:use seesaw.core
        seesaw.border
        seesaw.dev
        seesaw.mig
        ;; jarman.ftoolth
        jarman.dev-tools
        jarman.alerts-service
        )
  (:require [jarman.icon-library :as icon]
            [clojure.string :as string]))


;; (import javax.swing.JInternalFrame)
;; (import javax.swing.BorderFactory)
;; (import javax.swing.JDesktopPane)
;; (import javax.swing.JButton)
;; (import javax.swing.JFrame)
;; (import javax.swing.JLabel)
;; (import java.awt.Dimension)
;; (import java.awt.Component)
;; (import java.awt.Point)
(import javax.swing.JLayeredPane)
(import java.awt.Color)


(def hand-hover-on  (fn [e] (config! e :cursor :hand)))
(def hand-hover-off (fn [e] (config! e :cursor :hand)))

(def func-ico-f (fn [ic & args] (label :icon (image-scale ic (if (> (count args) 0) (first args) 28))
                                       :background (new Color 0 0 0 0)
                                       :border (empty-border :left 3 :right 3)
                                       :listen [:mouse-entered hand-hover-on 
                                                :mouse-clicked (if (> (count args) 1) (second args) (fn [e]))])))

(def alert-ico-f (fn [ic] (label :icon (image-scale ic 28)
                                 :background (new Color 0 0 0 0)
                                 :border (empty-border :left 3 :right 3))))

(def func-header-f (fn [txt] (label :text txt
                                    :font {:size 14 :style :bold}
                                    :background (new Color 0 0 0 0))))

(def func-body-f (fn [txt] (label :text txt
                                  :font {:size 13}
                                  :background (new Color 0 0 0 0)
                                  :border (empty-border :left 5 :right 5 :bottom 2))))

(def template-resize
  (fn [app-template]
    (let [v-size (.getSize    (to-root app-template))
          vw     (.getWidth   v-size)
          vh     (.getHeight  v-size)]
      (config! app-template  :bounds [0 0 vw vh]))))


;; ---------------------------------------------------- APP STARTER v
(def app-width 1000)
(def app-height 600)
(def app-bounds [0 0 app-width app-height])

(def app
  "Description:
       Create panel for absolute position elements
   "
  (new JLayeredPane))


(-> (doto (seesaw.core/frame
           :title "DEBUG WINDOW" :undecorated? false
           :minimum-size [1000 :by 600]
           :content app
           )
      (.setLocationRelativeTo nil) pack! show!))

(def alerts-s (message-server-creator app))
;; ---------------------------------------------------- APP STARTER ^



(defn message
  "Description:
      Template for messages. Using in JLayeredPanel.
      X icon remove and rebound displayed message.
   Example:
      (message alerts-controller 'header' 'body')
   Needed:
      Service from 'message-server-creator' function
   "
  [alerts-controller]
  (fn [data]
    (let [font-c "#000"
          back-c "#fff"
          close [(func-ico-f icon/X-64x64-png 23 (fn [e] (let [to-del (.getParent (.getParent (seesaw.core/to-widget e)))] (alerts-controller :rm-obj to-del))))]]
      (mig-panel
       :id :alert-box
       :constraints ["wrap 1" "0px[fill, grow]0px" "0px[20]0px[30]0px[20]0px"]
       :background back-c
       :border nil
       :bounds [680 480 300 75]
       :items [[(flow-panel
                 :align :left
                 :background (new Color 0 0 0 0)
                 :items [(alert-ico-f icon/alert-64-png)
                         (func-header-f (if (= (contains? data :header) true) (get data :header) "Information"))])]
               [(func-body-f (if (= (contains? data :body) true) (get data :body) "Template of information..."))]
               [(flow-panel
                 :align :right
                 :background (new Color 0 0 0 1)
                 :items (if (= (contains? data :btns) true) (concat close (get data :btns)) close))]]))))



(def btn-icon-f 
  "Description:
      Slide buttons used in JLayeredPanel. Norlam state is small square with icon 
      but on hover it will be wide and text will be inserted.
   "
  (fn [ico order size txt]
                  (let [bg-color "#ddd"
                        ;; color-onenter "#29295e"
                        color-hover-margin "#bbb"
                        bg-color-hover "#d9ecff"
                        size size
                        y (if (> (* size order) 0) (+ 2 (* size order)) (* size order))]
                    (label
                     :halign :center
                     :icon ico
                     :bounds [0 y size size]
                     :background bg-color
                     :border (line-border :left 4 :color bg-color)
                     :listen [:mouse-entered (fn [e] (config! e
                                                              :cursor :hand
                                                              :border (line-border :right 4 :color color-hover-margin)
                                                              :background bg-color-hover
                                                              :bounds [0 y (+ 200 size 8) size]
                                                              :text txt))
                              :mouse-exited  (fn [e] (config! e
                                                              :bounds [0 y size size]
                                                              :border (line-border :left 4 :color bg-color)
                                                              :background bg-color
                                                              :text ""
                                                              :cursor :default))]))))


(def btn-summer-f 
  "Description:
      It's a space for main button with more option. 
      Normal state is one button but after on click 
      space will be bigger and another buttons will be added.
      If button don't have any function, can not be expanded.
   Using Example:
      (btn-summer-f 'Button name' (Element or Component))
      (btn-summer-f 'Settings' (button 'Do something'))
      "
  (fn [txt & funcs]
                    (let [bg-color "#eee"
                          margin-color "#fff"
                          border (compound-border (line-border :left 6 :color bg-color) (line-border :bottom 2 :color margin-color))
                          vsize 35
                          hsize 200
                          ico (if (> (count funcs) 0) (image-scale icon/plus-64-png 25))
                          ico-hover (image-scale icon/minus-grey-64-png 20)]
                      (mig-panel
                       :constraints ["wrap 1" "0px[fill]0px" "0px[fill]0px"]
                       :listen [:mouse-entered (fn [e] (config! e :cursor :hand))
                                :mouse-entered (fn [e] (config! e :cursor :default))
                                :mouse-clicked (fn [e]
                                                 (if (> (count funcs) 0)
                                                   (if (== (count (seesaw.util/children (seesaw.core/to-widget e))) 1)
                                                     (do
                                                       (config! e :items (vec (map (fn [i] (vec (list i))) (concat (vec (seesaw.util/children (seesaw.core/to-widget e))) (vec funcs)))))
                                                       (config! (last (seesaw.util/children (first (seesaw.util/children (seesaw.core/to-widget e))))) :icon ico-hover))
                                                     (do
                                                       (config! e :items [(vec (list (first (seesaw.util/children (seesaw.core/to-widget e)))))])
                                                       (config! (last (seesaw.util/children (first (seesaw.util/children (seesaw.core/to-widget e))))) :icon ico)))))]
                       :items [[(mig-panel
                                 :constraints ["" "0px[fill]0px" "0px[fill]0px"]
                                 :background (new Color 0 0 0 0)
                                 :items [[(label
                                           :text txt
                                           :size [(- hsize vsize) :by vsize]
                                           :background bg-color
                                           :border border)]
                                         [(label
                                           :size [vsize :by vsize]
                                           :halign :center
                                           :background bg-color
                                           :border border
                                           :icon ico)]])]]))))



(def btn-tab-f
  "Description:
      Buttons for changing opened tables or functions in right part of app.
   "
  (fn [txt active]
    (let [bg-color (if (== active 1) "#eee" "#ccc")
          border "#fff"
          hsize 70
          vsize 30]
      (horizontal-panel
       :background bg-color
       :items [(label
                :text txt
                :halign :center
                :size [hsize :by vsize])
               (label
                :icon (image-scale icon/x-grey2-64-png 15)
                :halign :center
                :border (line-border :right 2 :color border)
                :size [vsize :by vsize]
                :listen [:mouse-entered (fn [e] (config! e :cursor :hand :icon (image-scale icon/x-blue1-64-png 15)))
                         :mouse-exited  (fn [e] (config! e :cursor :default :icon (image-scale icon/x-grey2-64-png 15)))])]
       :listen [:mouse-entered (fn [e] (config! e :cursor :hand))
                :mouse-exited  (fn [e] (config! e :cursor :default))]))))


(def mig-app-left-f
  "Description:
      Vertical layout of elements, left part of app for functions)
   Example:
      (mig-app-left-f [(btn-summer-f 'Option 1')] [(btn-summer-f 'Option 2')])
   "
  (fn [& args] (mig-panel
                :background "#fff"
                :border (line-border :left 4 :right 4 :color "#fff")
                :constraints ["wrap 1" "0px[fill, grow]0px" "0px[]0px"]
                :items (vec args))))

(def mig-app-right-f
  "Description: 
      Vertical layout for tabs and table on right part of app. 
      Tabs are inside horizontal panel on top.
    
   Example: 
      tabs  -> mig vector with elements    -> [(tab1) (tab2) (tab3)]
      array -> table like rows and columns -> [(table)]  
      (mig-menu-lvl-3-f [(tab1) (tab2) (tab3)] 
                        [(table)])"
  (fn [tabs array] (let [bg-color "#fff"]
                     (mig-panel
                      :background "#fff"
                      :constraints ["wrap 1" "0px[fill, grow]0px" "0px[]0px"]
                      :background "#eee"
                      :items [[(horizontal-panel
                                :background bg-color
                                :items tabs)]
                              [(vertical-panel
                                :background (new Color 0 0 0 0)
                                :items array)]]))))

(def jarmanapp
  "Description:
      Main space for app components inside JLayeredPanel
      Bottom layer"
  (grid-panel
   :bounds app-bounds
   :items [(mig-panel
            :constraints [""
                          "0px[50, fill]0px[200, fill]0px[fill, grow]0px"
                          "0px[fill, grow]0px"]
            :items [[(label :background "#eee")]
                    [(mig-app-left-f  [(btn-summer-f "Ukryte opcje 1"
                                                     (label :text "Opcja 1" :background "#fff" :size [200 :by 25]
                                                            :listen [:mouse-clicked (fn [e] (alerts-s :set {:header "Test" :body "Testowa wiadomosc"} (message alerts-s) 3))])
                                                     (label :text "Opcja 2" :background "#fff" :size [200 :by 25]
                                                            :listen [:mouse-clicked (fn [e] (alerts-s :set {:header "Witaj" :body "Świecie"} (message alerts-s) 5))]))]
                                      [(btn-summer-f "Ukryte opcje 2")])]
                    [(mig-app-right-f [(btn-tab-f "Tab 1" 1) (btn-tab-f "Tab 2" 0)]
                                      [(label :text "GRID")])]])]))



(def onresize-f
  "Description:
      This what should be resized with app window.
   "
  (fn [e] (do
            (template-resize jarmanapp)
            (alerts-rebounds-f e))))

(defn app-build
  "Description:
      Componenets to set to app
      Top layer.
   Example:
      Just use it (app-build)"
  [] (let [menu-icon-size 50]
       (do
         (.add app jarmanapp (new Integer 5))
         (.add app (btn-icon-f (image-scale icon/user-64x64-2-png menu-icon-size) 0 menu-icon-size "Klienci") (new Integer 10))
         (.add app (btn-icon-f (image-scale icon/settings-64x64-png menu-icon-size) 1 menu-icon-size "Konfiguracja") (new Integer 10))
         (config! (to-root app) :listen [:component-resized (fn [e] (onresize-f e))])
         (.repaint app))))

(app-build)
(alerts-s :show)
(alerts-s :hide)
;; (alerts-s :count-all)
;; (alerts-s :count-active)
;; (alerts-s :count-hidden)
;; (alerts-s :clear)

;; (alerts-s :set (message alerts-s "FFFFFFUCKQ!") 2)
;; (alerts-s :set (message alerts-s "Hell YEAH!") 0)
;; (alerts-s :rmall)

