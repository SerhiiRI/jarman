;; 
;; Compilation: dev_tool.clj -> metadata.clj -> gui_tools.clj -> gui_alerts_service.clj -> gui_app.clj
;; 
(ns jarman.gui-app
  (:use seesaw.core
        seesaw.border
        seesaw.dev
        seesaw.mig
        jarman.tools.dev-tools
        jarman.gui-tools
        jarman.gui-alerts-service
        jarman.logic.metadata
        ;; jarman.gui.gui-db-view
        )
  (:require [jarman.resource-lib.icon-library :as icon]
            [clojure.string :as string]))

(import javax.swing.JLayeredPane)
(import javax.swing.JLabel)
(import java.awt.Color)
(import java.awt.Dimension)
(import java.awt.event.MouseEvent)



;; ---------------------------------------------------- APP STARTER v
(def app-width 1500)
(def app-height 900)
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
           :minimum-size [app-width :by app-height]
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
                      :id :operation-space
                      :background "#fff"
                      :constraints ["wrap 1" "0px[fill, grow]0px" "0px[30]0px[fill,grow]0px"]
                      :background "#eee"
                      :items [[(horizontal-panel
                                :id :app-tabs-space
                                :background bg-color
                                :items tabs)]
                              [(vertical-panel
                                :id :app-functional-space
                                :background (new Color 0 0 0 0)
                                :items array)]]))))



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
                    [(mig-app-right-f [(label)]
                                      [(label)])]])]))


(defn create-bar-with-open-tabs
  [id-key]
  (cond
    (> (count @views) 0) (vec (map (fn [item]
                                     (let [item-key (get (second item) :title)]
                                       (tab-btn item-key item-key (if (identical? (first item) id-key) true false) [70 30]
                                                (fn [e] (do
                                                          (config! (select (getRoot e) [:#app-functional-space]) :items [(label)])
                                                          (reset! views (dissoc @views (first item)))
                                                          (if (contains? @views @last-active-tab) (reset! active-tab @last-active-tab) (do (reset! active-tab :none) (reset! last-active-tab :none)))))
                                                (fn [e] (do
                                                          (reset! active-tab (first item))
                                                          (config! (select (getRoot app) [:#app-functional-space]) :items [(scrollable (get (second item) :component) :border nil :id (keyword (get (second item) :id)))]))))))
                                   @views))
    :else [(label)]))

;; (create-bar-with-open-tabs)

;; Supervisior for open tabs bar
;; (add-watch views :refresh (fn [key atom old-state new-state]
;;                             (do 
;;                               (config! (select (getRoot app) [:#app-tabs-space]) :items (create-bar-with-open-tabs :none))
;;                               (.repaint app))))

;; Supervisior for open tabs clicked
(add-watch active-tab :refresh (fn [key atom old-state new-state]
                                 (do
                                   (cond
                                     (contains? @views new-state) (do
                                                                    (doall (map (fn [tab] (config! tab :background "#ccc")) (seesaw.util/children (select (to-root app) [:#app-tabs-space]))))
                                                                    (config! (select (getRoot app) [:#app-functional-space]) :items [(scrollable (get-in @views [new-state :component]) :border nil :id (keyword (get-in @views [new-state :id])))])
                                                                    (config! (select (getRoot app) [:#app-tabs-space]) :items (create-bar-with-open-tabs new-state)))
                                     :else (do
                                             (config! (select (getRoot app) [:#app-functional-space]) :items [(label)])
                                             (config! (select (getRoot app) [:#app-tabs-space]) :items [(label)]))))
                                 (reset! last-active-tab old-state)
                                 (.repaint app)))


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
         (.add app (slider-ico-btn (image-scale icon/scheme-grey-64x64-png menu-icon-size) 0 menu-icon-size "DB View"
                                   {:onclick (fn [e] (create-layered-db-view))}) (new Integer 10))
                                                                                                                                          
         (.add app (slider-ico-btn (image-scale icon/settings-64-png menu-icon-size) 1 menu-icon-size "Konfiguracja"
                                   {:onclick (fn [e] (do (new-test-for-tabs)))}) (new Integer 10))
         (.add app (slider-ico-btn (image-scale icon/I-64-png menu-icon-size) 2 menu-icon-size "Powiadomienia" {:onclick (fn [e] (alerts-s :show))}) (new Integer 10))
         
        ;;  (onresize-f app)
         (.repaint app))))

(def refresh-layered-for-tables
  (fn [] (do (if (= (contains? @views :layered-for-tabs) true)
               (let [max-w (apply max (map (fn [item]  (+ (.getX (config item :bounds)) (.getWidth  (config item :bounds)))) (seesaw.util/children (get-in @views [:layered-for-tabs :component]))))
                     parent-w (getWidth (.getParent (get-in @views [:layered-for-tabs :component])))
                     max-h (apply max (map (fn [item]  (+ (.getY (config item :bounds)) (.getHeight  (config item :bounds)))) (seesaw.util/children (get-in @views [:layered-for-tabs :component]))))
                     parent-h (getHeight (.getParent (get-in @views [:layered-for-tabs :component])))]
                 (do (.setPreferredSize (get-in @views [:layered-for-tabs :component]) (new Dimension (if (> parent-w max-w) parent-w max-w) (if (> parent-h max-h) parent-h max-h)))
                     (.setSize (get-in @views [:layered-for-tabs :component]) (new Dimension (if (> parent-w max-w) parent-w max-w) (if (> parent-h max-h) parent-h max-h)))))

               (.setPreferredSize (get-in @views [:layered-for-tabs :component]) (new Dimension
                                                                                      (getWidth  (.getParent (get-in @views [:layered-for-tabs :component])))
                                                                                      (getHeight (.getParent (get-in @views [:layered-for-tabs :component])))))))))

(def onresize-f
  "Description:
      Resize component inside JLayeredPane on main frame resize event.
   "
  (fn [e] (do
            refresh-layered-for-tables
            (template-resize jarmanapp)
            (alerts-rebounds-f e)
            (.repaint app))))

(config! (to-root app) :listen [:component-resized (fn [e] (onresize-f e))])


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