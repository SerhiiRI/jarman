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
        jarman.logic.data)
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

;; ================================================VVVVVVVVVV Table in database view

;; (defn set-row
;;   ([txt w h] (label :text txt
;;                     :size [w :by (- h 1)]
;;                     :background "#fff"
;;                     :border (line-border :top 1 :color "#000")))
;;   ([txt w h header?] (label :text txt
;;                             :size [w :by h]
;;                             :background "#666"
;;                             :foreground "#fff")))

;; (defn prepare-table
;;   [x y table-name & columns-name]
;;   (let [w 150
;;         row-h 30
;;         rows (map (fn [col] (set-row col w row-h)) columns-name)
;;         items (conj rows (set-row table-name w row-h true))
;;         h (* (count items) row-h)
;;         bg-c "#fff"]
;;    ;;  (println items)
;;     (vertical-panel
;;      :border (line-border :thickness 1 :color "#000")
;;      :bounds [x y w h]
;;      :background bg-c
;;      :items items)))

;; (def dbmap (list
;;             {:id 29
;;              :table "service_contract"
;;              :prop
;;              {:table
;;               {:frontend-name "service_contract"
;;                :is-system? false
;;                :is-linker? false
;;                :allow-modifing? true
;;                :allow-deleting? true
;;                :allow-linking? true}
;;               :columns
;;               [{:field "id_point_of_sale"
;;                 :representation "id_point_of_sale"
;;                 :description nil
;;                 :component-type "l"
;;                 :column-type "bigint(20) unsigned"
;;                 :private? false
;;                 :editable? true
;;                 :key-table "point_of_sale"}
;;                ]}}
;;             {:id 30
;;              :table "user"
;;              :prop
;;              {:table
;;               {:frontend-name "user"
;;                :is-system? false
;;                :is-linker? false
;;                :allow-modifing? true
;;                :allow-deleting? true
;;                :allow-linking? true}
;;               :columns
;;               [{:field "login"
;;                 :representation "login"
;;                 :description nil
;;                 :component-type "i"
;;                 :column-type "varchar(100)"
;;                 :private? false
;;                 :editable? true}
;;                ]}}))


;; (map (fn [tab] (conj {:bounds [0 0 0 0]} tab)) dbmap)


;; (prepare-table 10 10 "Users" "FName" "LName" "LOGIN")

;; (getset)

(def dbmap (getset))
;; 
(defn calculate-tables-size-with-tabs
  [db-data]
  (let [tabs-with-size (doall (map (fn [tab] (vec (list (vec (list (* 30 (+ 1 (count (get-in tab [:prop :columns]))))
                                                                   (+ 50 (* 6 (last (sort (concat (map (fn [col] (count (get col :representation))) (get-in tab [:prop :columns])) (list (count (get tab :table))))))))))
                                                        tab
                                                        )))db-data))]
    tabs-with-size))

(defn calculate-tables-size
  [db-data]
  (let [tabs-with-size (doall (map (fn [tab] (vec (list
                                                   (* 30 (+ 1 (count (get-in tab [:prop :columns]))))
                                                   (+ 50 (* 6 (last (sort (concat (map (fn [col] (count (get col :representation))) (get-in tab [:prop :columns])) (list (count (get tab :table))))))))))
                                     )db-data))]
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
                                     (vec (concat bounds (reverse (first tabs))))
                                     ))
         calculated-bounds tables)
    ))


;;  (calculate-bounds 10 4)

 
(defn set-col-as-row
  [data] (label :text (get data :name)
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
                          :else                         (compound-border (empty-border :thickness 4) (line-border :top 1 :color "#000")))
                :listen [:mouse-entered (fn [e] (do
                                                  (cond
                                                    (= (get data :type) "header") (config! e :cursor :move))))
                         :mouse-clicked (fn [e] (do
                                                    ;; (println (.getX (config e :bounds)))
                                                    ;; (println (.getY (config e :bounds)))
                                                    ;; (println (.getWidth (config e :bounds)))
                                                    ;; (println (.getHeight (config e :bounds)))
                                                    ;; (println (.getX e))
                                                  ))
                         :mouse-dragged (fn [e] (cond
                                                  (= (get data :type) "header") (let [bounds (config (getParent e) :bounds)
                                                                                      x (- (+ (.getX bounds) (.getX e)) (/ (.getWidth (getParent e)) 2))
                                                                                      y (- (+ (.getY bounds) (.getY e)) 15)
                                                                                      w (.getWidth  bounds)
                                                                                      h (.getHeight bounds)]
                                                                                  (do
                                                    ;; (println [x y w h])
                                                                                    (config! (getParent e) :bounds [x y w h])))))
                        ;;  :mouse-dragged (fn [e] (config! (getParent e) :bounds []))
                         ]))


(defn prepare-table-with-map
  [bounds data]
  (let [bg-c "#fff"
        line-size-hover 2  ;; zwiekszenie bordera dla eventu najechania mysza
        border (line-border :thickness 1 :color "#000")
        border-hover (line-border :thickness line-size-hover :color "#000")
        x (nth bounds 0)
        y (nth bounds 1)
        w (nth bounds 2)
        row-h 30  ;; wysokosc wiersza w tabeli reprezentujacego kolumne
        col-in-rows (map (fn [col] (set-col-as-row {:name (get col :field) :width w :height row-h :type (if (contains? col :key-table) "key" "row")})) (get-in data [:prop :columns]))  ;; przygotowanie tabeli bez naglowka
        camplete-table (conj col-in-rows (set-col-as-row {:name (get data :table) :width w :height row-h :type "header"}))  ;; dodanie naglowka i finalizacja widoku tabeli
        h (* (count camplete-table) row-h)  ;; podliczenie wysokosci gotowej tabeli
        ]
    (vertical-panel
     :tip "Double click to show relation."
     :id (get data :table)
     :border border
     :bounds [x y w h]
     :background bg-c
     :items camplete-table
    ;;  :listen [:mouse-entered (fn [e] (config! e :border border-hover :bounds [(- x (/ line-size-hover 2)) (- y (/ line-size-hover 2)) (+ w line-size-hover) (+ h line-size-hover)]))
    ;;           :mouse-exited  (fn [e] (config! e :border border :bounds [x y w h]))]
     )))

(def complete-db-view (atom (calculate-bounds 20 5)))

(doall (map (fn [tab-data]
              (.add layered-for-tabs (prepare-table-with-map (get-in tab-data [:prop :bounds] [10 10 100 100]) tab-data) (new Integer 5)))
            @complete-db-view))



;; ============================================^^^^^^^^^ Table in databse view



(defn refresh-layered-for-tables
  [] (do (if (> (count (seesaw.util/children layered-for-tabs)) 0)
           (let [max-w (apply max (map (fn [item]  (+ (.getX (config item :bounds)) (.getWidth  (config item :bounds)))) (seesaw.util/children layered-for-tabs)))
                 parent-w (getWidth (.getParent layered-for-tabs))
                 max-h (apply max (map (fn [item]  (+ (.getY (config item :bounds)) (.getHeight  (config item :bounds)))) (seesaw.util/children layered-for-tabs)))
                 parent-h (getHeight (.getParent layered-for-tabs))]
             (do (.setPreferredSize layered-for-tabs (new Dimension (if (> parent-w max-w) parent-w max-w) (if (> parent-h max-h) parent-h max-h)))
                 (.setSize layered-for-tabs (new Dimension (if (> parent-w max-w) parent-w max-w) (if (> parent-h max-h) parent-h max-h)))))

           (.setPreferredSize layered-for-tabs (new Dimension
                                                    (getWidth  (.getParent layered-for-tabs))
                                                    (getHeight (.getParent layered-for-tabs)))))))

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