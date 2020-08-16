;: FIRST gui_tools.clj
;; PREV gui_alerts_service.clj
;; NOW COMPILATION FILE 3/3
;; STOP
;; LAST gui_app.clj 
(ns jarman.gui-app
  (:use seesaw.core
        seesaw.border
        seesaw.dev
        seesaw.mig
        jarman.tools.dev-tools
        jarman.gui-tools
        jarman.gui-alerts-service)
  (:require [jarman.resource-lib.icon-library :as icon]
            [clojure.string :as string]))

(import javax.swing.JLayeredPane)
(import java.awt.Color)
(import java.awt.Dimension)


;; ---------------------------------------------------- APP STARTER v
(def app-width 1000)
(def app-height 600)
(def app-bounds [0 0 app-width app-height])

;; Prepare operative layer
(def app
  "Description:
       Create panel for absolute position elements. In real it is GUI.
   "
  (new JLayeredPane))

;; Start app window
(-> (doto (seesaw.core/frame
           :title "DEBUG WINDOW" :undecorated? false
           :minimum-size [1000 :by 600]
           :content app)
      (.setLocationRelativeTo nil) pack! show!))

;; Start message service
(def alerts-s (message-server-creator app))
;; ---------------------------------------------------- APP STARTER ^



(def mig-app-left-f
  "Description:
      Vertical layout of elements, left part of app for functions
   Example:
      (mig-app-left-f  [(expand-btn 'Ukryte opcje 1' (some-button))] [(expand-btn 'Ukryte opcje 2')])
   Needed:
      expand-btn component is needed to corectly work
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
      (mig-app-right-f [(tab-btn 'Tab 1' true) (tab-btn 'Tab 2' false)] [(label-fn :text 'GRID')])
   Needed:
      tab-btn component is needed to corectly work
   "
  (fn [tabs array] (let [bg-color "#fff"]
                     (mig-panel
                      :background "#fff"
                      :constraints ["wrap 1" "0px[fill, grow]0px" "0px[30]0px[fill,grow]0px"]
                      :background "#eee"
                      :items [[(horizontal-panel
                                :background bg-color
                                :items tabs)]
                              [(vertical-panel
                                :background (new Color 0 0 0 0)
                                :items array)]]))))

(def layered-for-tabs (new JLayeredPane))

(defn set-row
  ([txt w h] (label :text txt
                    :size [w :by (- h 1)]
                    :background "#fff"
                    :border (line-border :top 1 :color "#000")))
  ([txt w h header?] (label :text txt
                            :size [w :by h]
                            :background "#666"
                            :foreground "#fff")))

(defn prepare-table
  [x y table-name & columns-name]
  (let [w 150
        item-h 30
        rows (map (fn [col] (set-row col w item-h)) columns-name)
        items (conj rows (set-row table-name w item-h true))
        h (* (count items) item-h)
        bg-c "#fff"]
   ;;  (println items)
    (vertical-panel
     :border (line-border :thickness 1 :color "#000")
     :bounds [x y w h]
     :background bg-c
     :items items)))

;; (prepare-table 10 10 "Users" "FName" "LName" "LOGIN")

(do
  (.add layered-for-tabs (prepare-table 10 10 "Persona" "Fname" "Lname" "Addres") (new Integer 0))
  (.add layered-for-tabs (prepare-table 180 60 "Users" "Login" "Password" "Status") (new Integer 0))
  (.add layered-for-tabs (prepare-table 580 60 "Users" "Login" "Password" "Status") (new Integer 0))
  (.add layered-for-tabs (prepare-table 180 460 "Users" "Login" "Password" "Status") (new Integer 0))
  )

(defn refresh-layered-for-tables
  [] (do (if (> (count (seesaw.util/children layered-for-tabs)) 0)
           (.setPreferredSize layered-for-tabs (new Dimension
                                                    (let [max-w (apply max (map (fn [item]  (+ (.getX (config item :bounds)) (.getWidth  (config item :bounds)))) (seesaw.util/children layered-for-tabs)))
                                                          parent-w (getWidth (.getParent layered-for-tabs))]
                                                      (if (> parent-w max-w) parent-w max-w))
                                                    (let [max-h (apply max (map (fn [item]  (+ (.getY (config item :bounds)) (.getHeight  (config item :bounds)))) (seesaw.util/children layered-for-tabs)))
                                                          parent-h (getHeight (.getParent layered-for-tabs))]
                                                      (if (> parent-h max-h) parent-h max-h))))

           (.setPreferredSize layered-for-tabs (new Dimension
                                                    (getWidth  (.getParent layered-for-tabs))
                                                    (getHeight (.getParent layered-for-tabs)))))
         ))

(def jarmanapp
  "Description:
      Main space for app inside JLayeredPane. There is menu with expand btns and space for tables with tab btns.
   "
  (grid-panel
   :bounds app-bounds
   :items [(mig-panel
            :constraints [""
                          "0px[50, fill]0px[200, fill]0px[fill, grow]15px"
                          "0px[fill, grow]39px"]
            :items [[(label-fn :background "#eee" :size [50 :by 50])]
                    [(mig-app-left-f  [(expand-btn "Ukryte opcje 1"
                                                   (label-fn :text "Opcja 1" :background "#fff" :size [200 :by 25]
                                                             :listen [:mouse-clicked (fn [e] (alerts-s :set {:header "Test" :body "Bardzo dluga testowa wiadomość, która nie jest taka prosta do ogarnięcia w seesaw."} (message alerts-s) 3))])
                                                   (label-fn :text "Opcja 2" :background "#fff" :size [200 :by 25]
                                                             :listen [:mouse-clicked (fn [e] (alerts-s :set {:header "Witaj" :body "Świecie"} (message alerts-s) 5))]))]
                                      [(expand-btn "Ukryte opcje 2")])]
                    [(mig-app-right-f [(tab-btn "Tab 1" true [70 30]) (tab-btn "Tab 2" false [70 30])]
                                      [(scrollable layered-for-tabs :border nil :id :layered-for-tables)])]])]))



(def onresize-f
  "Description:
      Resize component inside JLayeredPane on main frame resize event.
   "
  (fn [e] (do
            (refresh-layered-for-tables)
            (template-resize jarmanapp)
            (alerts-rebounds-f e))))


(defn app-build
  "Description:
      Change starter window. Add prepare components and functions.
   Example:
      (app-build)
   Neede:
      Function need jarmanapp with app content
      Function need btn-icon-f function for category buttons
   "
  [] (let [menu-icon-size 50]
       (do
         (.add app jarmanapp (new Integer 5))
         (.add app (slider-ico-btn (image-scale icon/user-64x64-2-png menu-icon-size) 0 menu-icon-size "Klienci" {}) (new Integer 10))
         (.add app (slider-ico-btn (image-scale icon/settings-64x64-png menu-icon-size) 1 menu-icon-size "Konfiguracja" {}) (new Integer 10))
         (.add app (slider-ico-btn (image-scale icon/I-64-png menu-icon-size) 2 menu-icon-size "Powiadomienia" {:onclick (fn [e] (alerts-s :show))}) (new Integer 10))
         (config! (to-root app) :listen [:component-resized (fn [e] (onresize-f e))])
         (onresize-f app)
         (.repaint app))))

;; Complete window
(app-build)
;; (refresh-layered-for-tables)
;; (if (> (count (seesaw.util/children layered-for-tabs)) 0) (.setSize layered-for-tabs (new Dimension 730 550)))


;; (alerts-s :show)
;; (alerts-s :hide)
;; (alerts-s :count-all)
;; (alerts-s :count-active)
;; (alerts-s :count-hidden)
;; (alerts-s :clear)