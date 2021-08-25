;; 
;; Compilation: dev_tool.clj -> metadata.clj -> gui_tools.clj -> gui_alerts_service.clj -> gui_app.clj
;; 
(ns jarman.gui.gui-app
  (:use seesaw.core
        seesaw.border
        seesaw.dev
        seesaw.mig)
  (:require [clojure.string                   :as string]
            [jarman.resource-lib.icon-library :as icon]
            [jarman.gui.gui-tools             :as gtool]
            [jarman.tools.swing               :as stool]
            [jarman.gui.gui-alerts-service    :refer :all]))

(import javax.swing.JLayeredPane)
(import javax.swing.JLabel)
(import java.awt.Color)
(import java.awt.Dimension)
(import java.awt.event.MouseEvent)


(def views (atom {}))
(def active-tab (atom :none))
(def last-active-tab (atom :none))
;; (reset! views nil)
;; @views
;; (swap! views (fn [storage] (conj storage (new JLayeredPane))))
;; (swap! views (fn [storage] (conj storage (label))))
;; (def new-layered-for-tabs (fn [] (swap! views (fn [storage] (merge storage {:layered-for-tabs {:component (new JLayeredPane) :id "layered-for-tables" :title "DB View"}})))))
(def new-test-for-tabs (fn [] (do
                                (swap! views (fn [storage] (merge storage {:test {:component (label :text "Test") :id "test" :title "Test"}})))
                                (reset! active-tab :test)
                                )))
;; (new-layered-for-tabs)
;; (new-test-for-tabs)

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
       Right Mouse Click Menu on table to control clicked table"
  [id x y] (let [border-c "#bbb"
                 btn (fn [txt ico] (label
                                    :text txt
                                    :icon (stool/image-scale ico 30)
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
              :items [[(btn "Edit table" icon/pen-blue-64-png)]
                      [(btn "Delete table" icon/basket-blue1-64-png)]
                      [(btn "Show relations" icon/refresh-connection-blue-64-png)]])))


(defn set-col-as-row
  "Description:
      Create basic or special row who represetive column in table"
  [data] (let [last-x (atom 0)
               last-y (atom 0)]
           (label :text (get data :name)
                  :size [(get data :width) :by (cond
                                                 (= (get data :type) "header") (- (get data :height) 2)
                                                 :else                         (- (get data :height) 0))]
                  :icon (cond
                          (= (get data :type) "key") (stool/image-scale icon/key-blue-64-png (/ (get data :height) 1))
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
                                                    (table-funcs-menu (config (gtool/getParent e) :id)
                                                                      (- (+ (.getX e) (.getX (config (gtool/getParent e) :bounds))) 15)
                                                                      (- (+ (+ (.getY e) (.getY (config e :bounds))) (.getY (config (gtool/getParent e) :bounds))) 10))
                                                    (new Integer 999))))
                           :mouse-dragged (fn [e]
                                            (do
                                              (if (= @last-x 0) (reset! last-x (.getX e)))
                                              (if (= @last-y 0) (reset! last-y (.getY e)))
                                              (cond
                                                (= (get data :type) "header") (let [bounds (config (gtool/getParent e) :bounds)
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
                                                                                  (config! (gtool/getParent e) :bounds [x y w h]))))))
                           :mouse-released (fn [e] (do (reset! last-x 0)
                                                       (reset! last-y 0)))])))


(defn prepare-table-with-map
  "Description:
     Create one table on JLayeredPane using database map"
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
       Menu to control tables view (functionality for db-view)"
  [] (let [btn (fn [txt ico & args] (let
                                     [border-c "#bbb"]
                                      (label
                                       :text txt
                                       :icon (stool/image-scale ico 30)
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
                [(btn "Save view" icon/agree-blue-64-png true)]
                [(btn "Reset view" icon/arrow-blue-left-64-png)]
                [(btn "Reloade view" icon/refresh-blue-64-png)]])))


(defn create-layered-db-view 
  "Description:
     Create component and set to @views atom to use in functional space. 
     Add open tab for db-view to open tabs bar.
     Set prepare view from @views to functional space.
   "
  []
  (let [id-key :layered-for-tabs]
   (do
     (if (= (contains? @views id-key) false)
       (do
         (swap! views (fn [storage] (merge storage {id-key {:component (new JLayeredPane) :id "layered-for-tables" :title "DB View"}})))
         (.add (get-in @views [id-key :component]) (db-viewer-menu) (new Integer 1000))
         (doall (map (fn [tab-data]
                       (.add (get-in @views [id-key :component]) (prepare-table-with-map (get-in tab-data [:prop :bounds] [10 10 100 100]) tab-data) (new Integer 5)))
                     (calculate-bounds 20 5)))))
     (reset! active-tab id-key)
     )))

