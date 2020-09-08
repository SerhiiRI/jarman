;; 
;; Compilation: dev_tool.clj -> data.clj -> gui_tools.clj -> gui_alerts_service.clj -> gui_app.clj
;; 
(ns jarman.gui-app
  (:use seesaw.core
        seesaw.border
        seesaw.dev
        seesaw.mig
        jarman.tools.dev-tools
        jarman.gui-tools
        jarman.gui-alerts-service
        jarman.logic.metadata)
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

(def views (atom {}))
;; (reset! views nil)
;; @views
;; (swap! views (fn [storage] (conj storage (new JLayeredPane))))
;; (swap! views (fn [storage] (conj storage (label))))
(def new-layered-for-tabs (fn [] (swap! views (fn [storage] (merge storage {:layered-for-tabs {:component (new JLayeredPane) :id "layered-for-tables" :title "DB View"}})))))
(def new-test-for-tabs (fn [] (swap! views (fn [storage] (merge storage {:test {:component (label :text "Test") :id "test" :title "Test"}})))))
(new-layered-for-tabs)
(new-test-for-tabs)

;; (new-layered-for-tabs)
;; @views

;; ================================================VVVVVVVVVV Table in database view


(def dbmap (list
            {:id 29
             :table "service_contract"
             :prop
             {:table
              {:frontend-name "service_contract"
               :is-system? false
               :is-linker? false
               :allow-modifing? true
               :allow-deleting? true
               :allow-linking? true}
              :columns
              [{:field "id_point_of_sale"
                :representation "id_point_of_sale"
                :description nil
                :component-type "l"
                :column-type "bigint(20) unsigned"
                :private? false
                :editable? true
                :key-table "point_of_sale"}
               ]}}
            {:id 30
             :table "user"
             :prop
             {:table
              {:frontend-name "user"
               :is-system? false
               :is-linker? false
               :allow-modifing? true
               :allow-deleting? true
               :allow-linking? true}
              :columns
              [{:field "login"
                :representation "login"
                :description nil
                :component-type "i"
                :column-type "varchar(100)"
                :private? false
                :editable? true}
               ]}}))


;; (map (fn [tab] (conj {:bounds [0 0 0 0]} tab)) dbmap)


;; (prepare-table 10 10 "Users" "FName" "LName" "LOGIN")

;; (getset)

;; (def dbmap (getset))

(defn calculate-tables-size-with-tabs
  [db-data]
  (let [tabs-with-size (doall (map (fn [tab] (vec (list (vec (list (* 30 (+ 1 (count (get-in tab [:prop :columns]))))
                                                                   (+ 50 (* 6 (last (sort (concat (map (fn [col] (count (get col :representation))) (get-in tab [:prop :columns])) (list (count (get tab :table))))))))))
                                                        tab))) db-data))]
    tabs-with-size))

(defn calculate-tables-size
  [db-data]
  (let [tabs-with-size (doall (map (fn [tab] (vec (list
                                                   (* 30 (+ 1 (count (get-in tab [:prop :columns]))))
                                                   (+ 50 (* 6 (last (sort (concat (map (fn [col] (count (get col :representation))) (get-in tab [:prop :columns])) (list (count (get tab :table))))))))))) db-data))]
    tabs-with-size))


(defn calculate-bounds
  [offset max-tabs-inline]
  (let [sizes (partition-all max-tabs-inline (calculate-tables-size dbmap))
        tables (calculate-tables-size-with-tabs dbmap)
        y-bound (atom 10)
        pre-bounds (map (fn [row] (let [x-bounds (atom 10)
                                        bounds-x (map (fn [size] (do
                                                                   (def xbound @x-bounds)
                                                                   (swap! x-bounds (fn [last-x] (+ last-x offset (last size))))
                                                                   xbound))
                                                      row)]
                                    (do
                                      (def lista (list bounds-x @y-bound))
                                      (swap! y-bound (fn [yb] (+ yb offset (first (first row)))))
                                      lista)))
                        sizes)
        calculated-bounds (apply concat (map (fn [row] (let [x-list  (first row)
                                                             y       (last row)]
                                                         (map (fn [x] (vec (list x y))) x-list)))
                                             pre-bounds))]
    (map (fn [bounds tabs] (assoc-in (last tabs)
                                     [:prop :bounds]
                                     (vec (concat bounds (reverse (first tabs))))))
         calculated-bounds tables)))


;;  (calculate-bounds 10 4)
(defn table-funcs-menu
  "Description:
       Menu to control view with tables in database"
  [id x y] (let [border-c "#bbb"
                 btn (fn [txt ico] (label
                                    :font (getFont 13)
                                    :text txt
                                    :icon (image-scale ico 30)
                                    :background "#fff"
                                    :foreground "#000"
                                    :border (compound-border (empty-border :left 10 :right 15) (line-border :bottom 1 :color border-c))
                                    :listen [:mouse-entered (fn [e] (config! e :background "#d9ecff" :foreground "#000" :cursor :hand))
                                             :mouse-exited  (fn [e] (do
                                                                      (config! e :background "#fff" :foreground "#000")
                                                                      (let [bounds (config (seesaw.core/to-widget (.getParent (seesaw.core/to-widget e))) :bounds)
                                                                            mouse-y (+ (+ (.getY e) (.getY (config e :bounds))) (.getY bounds))
                                                                            mouse-x (.getX e)]
                                                                        (if (or (< mouse-x 5)
                                                                                (> mouse-x (- (.getWidth bounds) 5))
                                                                                (< mouse-y (+ (.getY bounds) 5))
                                                                                (> mouse-y (- (+ (.getHeight bounds) (.getY bounds)) 5)))
                                                                          (do
                                                                            (.remove (get-in @views [:layered-for-tabs :component]) (seesaw.core/to-widget (.getParent (seesaw.core/to-widget e))))
                                                                            (.repaint (get-in @views [:layered-for-tabs :component])))))))]))]
             (mig-panel
              :id :db-viewer-menu
              :bounds [x y 150 90]
              :background (new Color 0 0 0 0)
              :border (line-border :thickness 1 :color border-c)
              :constraints ["wrap 1" "0px[150, fill]0px" "0px[30px, fill]0px"]
              :items [[(btn "Edit table" icon/pen-blue-grey-64-png)]
                      [(btn "Delete table" icon/basket-blue1-64x64-png)]
                      [(btn "Show relations" icon/connection-blue-64-png)]])))


(defn set-col-as-row
  "Description:
      Create row who represetive column in table"
  [data] (let [last-x (atom 0)
               last-y (atom 0)]
           (label :text (get data :name)
                  :size [(get data :width) :by (cond
                                                 (= (get data :type) "header") (- (get data :height) 2)
                                                 :else                         (- (get data :height) 0))]
                  :icon (cond
                          (= (get data :type) "key") (image-scale icon/key-blue-64-png (/ (get data :height) 1))
                          :else nil)
                  :background (cond
                                (= (get data :type) "header") "#666"
                                (= (get data :type) "key")    "#f7d67c"
                                :else                         "#fff")
                  :foreground (cond
                                (= (get data :type) "header") "#fff"
                                :else                         "#000")
                  :border (cond
                            (= (get data :type) "header") (compound-border (empty-border :thickness 4))
                            :else                         (compound-border (empty-border :thickness 4) (line-border :top 1 :color (get data :border-c))))
                  :listen [:mouse-entered (fn [e] (do
                                                    (cond
                                                      (= (get data :type) "header") (config! e :cursor :move))))
                           :mouse-clicked (fn [e]
                                            (if (= (.getButton e) MouseEvent/BUTTON3)
                                              (.add (get-in @views [:layered-for-tabs :component])
                                                    (table-funcs-menu (config (getParent e) :id)
                                                                      (- (+ (.getX e) (.getX (config (getParent e) :bounds))) 15)
                                                                      (- (+ (+ (.getY e) (.getY (config e :bounds))) (.getY (config (getParent e) :bounds))) 10))
                                                    (new Integer 999))))
                           :mouse-dragged (fn [e]
                                            (do
                                              (if (= @last-x 0) (reset! last-x (.getX e)))
                                              (if (= @last-y 0) (reset! last-y (.getY e)))
                                              (cond
                                                (= (get data :type) "header") (let [bounds (config (getParent e) :bounds)
                                                                                    ;; x (- (+ (.getX bounds) (.getX e)) (/ (.getWidth (getParent e)) 2))
                                                                                    ;; y (- (+ (.getY bounds) (.getY e)) 15)
                                                                                    pre-x (- (+ (.getX bounds) (.getX e)) @last-x)
                                                                                    pre-y (- (+ (.getY bounds) (.getY e)) @last-y)
                                                                                    x (if (> pre-x 0) pre-x 0)
                                                                                    y (if (> pre-y 0) pre-y 0)
                                                                                    w (.getWidth  bounds)
                                                                                    h (.getHeight bounds)]
                                                                                (do
                                                                                  ;; (println [@last-x @last-y])
                                                                                  (config! (getParent e) :bounds [x y w h]))))))
                           :mouse-released (fn [e] (do (reset! last-x 0)
                                                       (reset! last-y 0)))])))


(defn prepare-table-with-map
  "Description:
     Create tables view on JLayeredPane using map of database
  "
  [bounds data]
  (let [bg-c "#fff"
        line-size-hover 2  ;; zwiekszenie bordera dla eventu najechania mysza
        border-c "#aaa"
        border (line-border :thickness 1 :color border-c)
        border-hover (line-border :thickness line-size-hover :color "#000")
        x (nth bounds 0)
        y (+ 30 (nth bounds 1))
        w (nth bounds 2)
        row-h 30  ;; wysokosc wiersza w tabeli reprezentujacego kolumne
        col-in-rows (map (fn [col] (set-col-as-row {:name (get col :field) :width w :height row-h :type (if (contains? col :key-table) "key" "row") :border-c border-c})) (get-in data [:prop :columns]))  ;; przygotowanie tabeli bez naglowka
        camplete-table (conj col-in-rows (set-col-as-row {:name (get data :table) :width w :height row-h :type "header" :border-c border-c}))  ;; dodanie naglowka i finalizacja widoku tabeli
        h (* (count camplete-table) row-h)  ;; podliczenie wysokosci gotowej tabeli
        ]
    (vertical-panel
     :id (get data :table)
     :tip "Double click to show relation. PPM to show more function."
     :border border
     :bounds [x y w h]
     :background bg-c
     :items camplete-table)))


;; (println MouseEvent/BUTTON3)


(defn db-viewer-menu
  "Description:
       Menu to control view with tables in database"
  [] (let [btn (fn [txt ico & args] (let
                                     [border-c "#bbb"]
                                      (label
                                       :font (getFont 13)
                                       :text txt
                                       :icon (image-scale ico 30)
                                       :background "#fff"
                                       :foreground "#000"
                                       :border (compound-border (empty-border :left 10 :right 15) (line-border :thickness 1 :color border-c))
                                       :listen [:mouse-entered (fn [e] (config! e :background "#d9ecff" :foreground "#000" :cursor :hand))
                                                :mouse-exited  (fn [e] (config! e :background "#fff" :foreground "#000"))])))]
       (mig-panel
        :id :db-viewer-menu
        :bounds [0 5 1000 30]
        :background (new Color 0 0 0 0)
        :constraints ["" "5px[fill]0px" "0px[30px, fill]0px"]
        :items [[(btn "Show all relation" icon/refresh-connection-blue-64-png)]
                [(btn "Save view" icon/agree-64-png true)]
                [(btn "Reset view" icon/arrow-blue-left-64-png)]
                [(btn "Reloade view" icon/refresh-blue-64-png)]])))



;; ============================================^^^^^^^^^ Table in databse view





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
                    [(mig-app-right-f [;;(label)
                                      ;;  (tab-btn "Tab 1" true [70 30]) 
                                      ;;  (tab-btn "Tab 2" false [70 30])
                                       (label)
                                       ]
                                      [
                                       (label)
                                      ;;  (label)
                                      ;;  (scrollable (label) :border nil :id :layered-for-tables)
                                       ])]])]))





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
                                   {:onclick (fn [e] (if (not (identical? (config (first (seesaw.util/children (select (to-root e) [:#app-functional-space]))) :id) (keyword (get-in @views [:layered-for-tabs :id]))))
                                                       (do
                                                         (doall (map (fn [tab] (config! tab :background "#ccc")) (seesaw.util/children (select (to-root (seesaw.core/to-widget e)) [:#app-tabs-space]))))
                                                         (.add (get-in @views [:layered-for-tabs :component]) (db-viewer-menu) (new Integer 1000))
                                                         (doall (map (fn [tab-data]
                                                                       (.add (get-in @views [:layered-for-tabs :component]) (prepare-table-with-map (get-in tab-data [:prop :bounds] [10 10 100 100]) tab-data) (new Integer 5)))
                                                                     (calculate-bounds 20 5)))
                                                         (config! (select (to-root (seesaw.core/to-widget e)) [:#app-functional-space]) :items [(scrollable (get-in @views [:layered-for-tabs :component]) :border nil :id (keyword (get-in @views [:layered-for-tabs :id])))])
                                                        ;;  (println (conj (seesaw.util/children (select (to-root (seesaw.core/to-widget e)) [:#app-tabs-space])) (label)))
                                                         (config! (select (to-root (seesaw.core/to-widget e)) [:#app-tabs-space]) :items  (vec (conj
                                                                                                                                                (seesaw.util/children (select (to-root (seesaw.core/to-widget e)) [:#app-tabs-space]))
                                                                                                                                                (tab-btn (get-in @views [:layered-for-tabs :title])
                                                                                                                                                         (get-in @views [:layered-for-tabs :title])
                                                                                                                                                         true
                                                                                                                                                         [70 30]
                                                                                                                                                         (fn [e] (do
                                                                                                                                                                   (.removeAll (get-in @views [:layered-for-tabs :component]))
                                                                                                                                                                   (config! (select (to-root (seesaw.core/to-widget e)) [:#app-functional-space]) :items [(label)])
                                                                                                                                                                   (config! (select (to-root (seesaw.core/to-widget e)) [:#app-tabs-space]) :items [(label)])))
                                                                                                                                                         (fn [e] (do
                                                                                                                                                                   (doall (map (fn [tab] (config! tab :background "#ccc")) (seesaw.util/children (select (to-root (seesaw.core/to-widget e)) [:#app-tabs-space]))))
                                                                                                                                                                   (config! (getParent e) :background "#eee")
                                                                                                                                                                   (config! (select (to-root (seesaw.core/to-widget e)) [:#app-functional-space]) :items [(scrollable (get-in @views [:layered-for-tabs :component]) :border nil :id (keyword (get-in @views [:layered-for-tabs :id])))]))))))))))}) (new Integer 10))
                                                                                                                                          
         (.add app (slider-ico-btn (image-scale icon/settings-64x64-png menu-icon-size) 1 menu-icon-size "Konfiguracja"
                                   {:onclick (fn [e] (if (not (identical? (config (first (seesaw.util/children (select (to-root e) [:#app-functional-space]))) :id) (keyword (get-in @views [:test :id]))))
                                                       (do
                                                         (doall (map (fn [tab] (config! tab :background "#ccc")) (seesaw.util/children (select (to-root (seesaw.core/to-widget e)) [:#app-tabs-space]))))
                                                         (config! (select (to-root (seesaw.core/to-widget e)) [:#app-functional-space]) :items [(scrollable (get-in @views [:test :component]) :border nil :id (keyword (get-in @views [:test :id])))])
                                                         (config! (select (to-root (seesaw.core/to-widget e)) [:#app-tabs-space]) :items (vec (conj
                                                                                                                                               (seesaw.util/children (select (to-root (seesaw.core/to-widget e)) [:#app-tabs-space]))
                                                                                                                                               (tab-btn (get-in @views [:test :title]) 
                                                                                                                                                        (get-in @views [:test :title])
                                                                                                                                                        true 
                                                                                                                                                        [70 30]
                                                                                                                                                        (fn [e] (do
                                                                                                                                                                  (config! (select (to-root (seesaw.core/to-widget e)) [:#app-functional-space]) :items [(label)])
                                                                                                                                                                  (config! (select (to-root (seesaw.core/to-widget e)) [:#app-tabs-space]) :items [(label)])
                                                                                                                                                                  ))
                                                                                                                                                        (fn [e] (do
                                                                                                                                                                  (doall (map (fn [tab] (config! tab :background "#ccc")) (seesaw.util/children (select (to-root (seesaw.core/to-widget e)) [:#app-tabs-space]))))
                                                                                                                                                                  (config! (getParent e) :background "#eee")
                                                                                                                                                                  (config! (select (to-root (seesaw.core/to-widget e)) [:#app-functional-space]) :items [(scrollable (get-in @views [:test :component]) :border nil :id (keyword (get-in @views [:test :id])))])
                                                                                                                                                                  )))))))))}) (new Integer 10))
         (.add app (slider-ico-btn (image-scale icon/I-64-png menu-icon-size) 2 menu-icon-size "Powiadomienia" {:onclick (fn [e] (alerts-s :show))}) (new Integer 10))
         
        ;;  (onresize-f app)
         (.repaint app))))

(defn refresh-layered-for-tables
  [] (do (if (> (count (seesaw.util/children (get-in @views [:layered-for-tabs :component]))) 0)
           (let [max-w (apply max (map (fn [item]  (+ (.getX (config item :bounds)) (.getWidth  (config item :bounds)))) (seesaw.util/children (get-in @views [:layered-for-tabs :component]))))
                 parent-w (getWidth (.getParent (get-in @views [:layered-for-tabs :component])))
                 max-h (apply max (map (fn [item]  (+ (.getY (config item :bounds)) (.getHeight  (config item :bounds)))) (seesaw.util/children (get-in @views [:layered-for-tabs :component]))))
                 parent-h (getHeight (.getParent (get-in @views [:layered-for-tabs :component])))]
             (do (.setPreferredSize (get-in @views [:layered-for-tabs :component]) (new Dimension (if (> parent-w max-w) parent-w max-w) (if (> parent-h max-h) parent-h max-h)))
                 (.setSize (get-in @views [:layered-for-tabs :component]) (new Dimension (if (> parent-w max-w) parent-w max-w) (if (> parent-h max-h) parent-h max-h)))))

           (.setPreferredSize (get-in @views [:layered-for-tabs :component]) (new Dimension
                                                    (getWidth  (.getParent (get-in @views [:layered-for-tabs :component])))
                                                    (getHeight (.getParent (get-in @views [:layered-for-tabs :component]))))))))

(def onresize-f
  "Description:
      Resize component inside JLayeredPane on main frame resize event.
   "
  (fn [e] (do
            (refresh-layered-for-tables)
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