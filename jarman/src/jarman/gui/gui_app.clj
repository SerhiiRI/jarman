;; 
;; Compilation: dev_tool.clj -> metadata.clj -> gui_tools.clj -> gui_alerts_service.clj -> gui_app.clj
;; 
(ns jarman.gui.gui-app
  (:use seesaw.core
        seesaw.border
        seesaw.dev
        seesaw.mig)
  (:require [clojure.string :as string]
            ;; resource 
            [jarman.resource-lib.icon-library :as icon]
            ;; logics
            [jarman.config.config-manager :refer :all]
            [jarman.gui.gui-tools :refer :all]
            [jarman.gui.gui-alerts-service :refer :all]
            ;; deverloper tools 
            [jarman.tools.swing :as stool]
            [jarman.tools.lang :refer :all]
            ;; TEMPORARY!!!! MUST BE REPLACED BY CONFIG_MANAGER
            [jarman.config.init :refer [configuration language]]))

;;  (get-color :jarman :bar)

(import javax.swing.JLayeredPane)
(import javax.swing.JLabel)
(import java.awt.Color)
(import java.awt.Dimension)
(import java.awt.event.MouseEvent)


;; ┌────────────────────────┐
;; │                        │
;; │ Start empty App layout │
;; │                        │
;; └────────────────────────┘

(def app-width (get-frame :width))
(def app-height (get-frame :heigh))
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
           :size [app-width :by app-height]
           :content app)
      (.setLocationRelativeTo nil) pack! show!))

;; Start message service
(def alerts-s (message-server-creator app))



;; ┌───────────────────┐
;; │                   │
;; │ Prepare app atoms │
;; │                   │
;; └───────────────────┘

(def right-size (atom [0 0]))
(def views (atom {}))
(def active-tab (atom :none))
(def last-active-tab (atom :none))
(def table-in-editor (atom :none))
;; (reset! views nil)
;; @views
;; (swap! views (fn [storage] (conj storage (new JLayeredPane))))
;; (swap! views (fn [storage] (conj storage (label))))
;; (def new-layered-for-tabs (fn [] (swap! views (fn [storage] (merge storage {:layered-for-tabs {:component (new JLayeredPane) :id "layered-for-tables" :title "DB View"}})))))

(defn create-view
  "Description
     Quick tab template for view component. Just set id, title and component.
     Next view will be added to @views with tab to open tabs bar.
   "
  [id title view]
  (let [id-key (keyword id)]
    (do
      (if (= (contains? @views id-key) false) (swap! views (fn [storage] (merge storage {id-key {:component view :id id :title title}}))))
      (reset! active-tab id-key))))


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
               {:field "name_of_sale"
                :representation "Miejsce sprzedaży"
                :description "Opisuje miejsce sprzedaży"
                :component-type "l"
                :column-type "bigint(20) unsigned"
                :private? false
                :editable? true}]}}
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
                :editable? true}]}}))


;; (map (fn [tab] (conj {:bounds [0 0 0 0]} tab)) dbmap)
;; (prepare-table 10 10 "Users" "FName" "LName" "LOGIN")
;; (getset)
;; (def dbmap (getset))



;; ┌─────────────────────────────┐
;; │                             │
;; │ Resize components functions │
;; │                             │
;; └─────────────────────────────┘

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




;; ┌──────────────┐
;; │              │
;; │ Table editor │
;; │              │
;; └──────────────┘


(defn create-save-btn-for-table-editor
  [] (edit-table-btn :edit-view-save-btn (get-lang-btns :save) icon/agree-grey-64-png icon/agree-blue-64-png true
                     (fn [e] (if (get (config e :user-data) :active)
                               (do
                                 (println "Przycisk aktywny")
                                 (config! e :user-data {:active false}))
                               (do
                                 (println "Przycisk nieaktywny")
                                 (config! e :user-data {:active true}))))))

(defn create-restore-btn-for-table-editor
  [] (edit-table-btn :edit-view-back-btn (get-lang-btns :remove) icon/refresh-grey-64-png icon/refresh-blue-64-png false
                     (fn [e] (if (get (config e :user-data) :active)
                               (do
                                 (println "Przycisk aktywny")
                                 (config! e :user-data {:active false}))
                               (do
                                 (println "Przycisk nieaktywny")
                                 (config! e :user-data {:active true}))))))

(defn create-editor-section-header
  "Create header GUI component in editor for separate section"
  [name] (label :text name :font (getFont 14 :bold) :border (line-border :bottom 2 :color (get-color :background :header))))

(defn create-editor-column-selector-btn
  "Description:
     Select button who create-editor-column-selector, can work with another same buttons"
  [txt func] (let [color (get-color :group-buttons :background)
                   color-hover (get-color :group-buttons :background-hover)
                   color-clicked (get-color :group-buttons :clicked)
                   bg-btn (atom color)]
               (mig-panel
                :constraints ["" "15px[100, fill]15px" "10px[fill]10px"]
                :border (line-border :left 4 :right 4 :top 1 :bottom 1 :color (get-color :background :header))
                :id :create-editor-column-selector-btn
                :user-data bg-btn
                :listen [:mouse-entered (fn [e] (config! e :cursor :hand :background color-hover))
                         :mouse-exited  (fn [e] (config! e :background @bg-btn))
                         :mouse-clicked  (fn [e] (cond
                                                   (= @bg-btn color)
                                                   (do
                                                ;; reset bg and atom inside all buttons in parent
                                                     (doall (map (fn [b] (do (config! b :background color)
                                                                             (reset! (config b :user-data) color)))
                                                                 (seesaw.util/children (.getParent (seesaw.core/to-widget e)))))
                                                ;; reset atom with color
                                                     (reset! bg-btn color-clicked)
                                                ;; update atom with color
                                                     (config! e :background @bg-btn)
                                                     (func e))))]
                :items [[(label :text txt)]])))


(defn create-editor-column-selector
  "Create left table editor view to select column which will be editing on right table editor view"
  [] (mig-panel :constraints ["wrap 1" "0px[fill]0px" "0px[fill]0px"]
                :id :create-editor-column-selector
                :items [[(create-editor-column-selector-btn "Test 1" (fn [e] (let [value [[(label :text "Selected column: ")]
                                                                                          [(label :text "Test 1")]
                                                                                          [(label :text "Value: ")]
                                                                                          [(label :text "I'm blue dabi dibu du bai")]]]
                                                                               (config! (select (to-root e) [:#create-editor-column-editor]) :items value))))]
                        [(create-editor-column-selector-btn "Dłuższy Test 2" (fn [e] (let [value [[(label :text "Selected column: ")]
                                                                                                  [(label :text "Test 2")]
                                                                                                  [(label :text "Value: ")]
                                                                                                  [(label :text "Tylko jedno w głowie maaam, koksu 5 gramm...")]
                                                                                                  [(label :text "Trashpanda")]
                                                                                                  [(label :text "Team")]]]
                                                                                       (config! (select (to-root e) [:#create-editor-column-editor]) :items value))))]]))

(defn create-editor-column-editor
  "Create right table editor view to editing selected column in left table editor view"
  [] (mig-panel :constraints ["wrap 2" "20px[100, fill]10px[fill]5px" "5px[fill]5px"]
                :id :create-editor-column-editor
                :items [[(label :text "Selected column: ")]
                        [(label :text "")]]))

(defn create-view-table-editor
  "Description:
     Create view for table editor.
   "
  [map id]
  (let [table      (first (filter (fn [item] (= id (get item :id))) map)) ;; Get table by id
        table-info (get-in table [:prop :table]) ;; Get table property
        id-key     (keyword (get table :table))  ;; Get name of table and create keyword to check tabs bar (opens views)
        columns    (get-in table [:prop :columns])
        elems      (vec (concat
                         [[(mig-panel :constraints ["" "0px[grow, fill]5px[]0px" "0px[fill]0px"] ;; menu bar for editor
                                      :items [[(label-fn :text (string/join "" [">_ Edit table: " (get table :table)])
                                                         :font (getFont 14 :bold)
                                                         :background "#ccc"
                                                         :border (empty-border :left 15))]
                                              [(create-save-btn-for-table-editor)]
                                              [(create-restore-btn-for-table-editor)]])]]
                         [[(create-editor-section-header "Table configuration")]] ;; Header of next section
                         [(vec (let [mp (vec table-info)  ;; Params for table config
                                     mpc (count mp)
                                     txtsize [150 :by 25]]
                                 [(mig-panel
                                   :constraints ["wrap 3" "0px[32%]0px" "0px[fill]0px"]
                                   :items (vec (for [x (range mpc)]
                                                 [(mig-panel
                                                   :border (line-border :left 4 :color "#ccc")
                                                   :constraints ["" "10px[130px]10px[200px]10px" "0px[fill]10px"]
                                                   :items [[(cond
                                                              (string? (first (nth mp x))) (text :text (str (first (nth mp x))))
                                                              (boolean? (first (nth mp x))) (checkbox :selected? (first (nth mp x)))
                                                              :else (label :text (str (first (nth mp x)))))]
                                                           [(cond
                                                              (string? (second (nth mp x))) (text :size txtsize :text (str (second (nth mp x))))
                                                              (boolean? (second (nth mp x))) (checkbox :selected? (second (nth mp x)))
                                                              :else (label :size txtsize :text (str (second (nth mp x)))))]])])))]))]
                         [[(create-editor-section-header "Column configuration")]]
                         [[(mig-panel
                            :constraints ["wrap 2" "0px[fill]0px" "0px[fill]0px"]
                            :items [[(create-editor-column-selector)] ;; Left part for kolumns to choose to changing
                                    [(create-editor-column-editor)] ;; Space for components, using to editing columns
                                    ])]]
                        ;;  todonow
                         ))
        view   (cond
                 (> (count table) 0) (do
                                       (scrollable (mig-panel
                                                    :constraints ["wrap 1" "20px[grow, fill]20px" "20px[]20px"]
                                                    :items elems)
                                                   :border (empty-border :thickness 0)))
                 :else (label :text "Table not found inside metadata :c"))]
    (do
        ;; (println)
        ;; (println "info")
        ;; (println (str table-info))
        ;; (println (str elemensts))
      (if (= (contains? @views id-key) false) (swap! views (fn [storage] (merge storage {id-key {:component view :id id :title (string/join "" ["Edit: " (get table :table)])}}))))
      (reset! active-tab id-key))))


;;  (calculate-bounds 10 4)
(def table-funcs-menu
  "Description:
       Right Mouse Click Menu on table to control clicked table"
  (fn [id x y] (let [border-c "#bbb"
                     rm-menu (fn [e] (do
                                       (.remove (get-in @views [:layered-for-tabs :component]) (seesaw.core/to-widget (.getParent (seesaw.core/to-widget e))))
                                       (.repaint (get-in @views [:layered-for-tabs :component]))))
                     btn (fn [txt ico onclick] (label
                                                :font (getFont 13)
                                                :text txt
                                                :icon (stool/image-scale ico 30)
                                                :background "#fff"
                                                :foreground "#000"
                                                :border (compound-border (empty-border :left 10 :right 15) (line-border :bottom 1 :color border-c))
                                                :listen [:mouse-clicked onclick
                                                         :mouse-entered (fn [e] (config! e :background "#d9ecff" :foreground "#000" :cursor :hand))
                                                         :mouse-exited  (fn [e] (do
                                                                                  (config! e :background "#fff" :foreground "#000")
                                                                                  (let [bounds (config (seesaw.core/to-widget (.getParent (seesaw.core/to-widget e))) :bounds)
                                                                                        mouse-y (+ (+ (.getY e) (.getY (config e :bounds))) (.getY bounds))
                                                                                        mouse-x (.getX e)]
                                                                                    (if (or (< mouse-x 5)
                                                                                            (> mouse-x (- (.getWidth bounds) 5))
                                                                                            (< mouse-y (+ (.getY bounds) 5))
                                                                                            (> mouse-y (- (+ (.getHeight bounds) (.getY bounds)) 5)))
                                                                                      (rm-menu e)))))]))]
                 (mig-panel
                  :id :db-viewer-menu
                  :bounds [x y 150 90]
                  :background (new Color 0 0 0 0)
                  :border (line-border :thickness 1 :color border-c)
                  :constraints ["wrap 1" "0px[150, fill]0px" "0px[30px, fill]0px"]
                  :items [[(btn "Edit table" icon/pen-blue-64-png (fn [e] (do (rm-menu e)
                                                                              (create-view-table-editor dbmap id))))]
                          [(btn "Delete table" icon/basket-blue1-64-png (fn [e]))]
                          [(btn "Show relations" icon/refresh-connection-blue-64-png (fn [e]))]]))))


(defn set-col-as-row
  "Description:
      Create a primary or special row that represents a column in the table"
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
                                                    (table-funcs-menu (get (config (getParent e) :user-data) :id)
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
     Create one table on JLayeredPane using database map
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
     :user-data {:id (get data :id)}
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
                                       :font (getFont 13)
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
                [(btn "Save view" icon/agree-grey-64-png true)]
                [(btn "Reset view" icon/arrow-blue-left-64-png)]
                [(btn "Reloade view" icon/refresh-blue-64-png)]])))


(defn create-view-db-view
  "Description:
     Create component and set to @views atom to use in functional space. 
     Add open tab for db-view to open tabs bar.
     Set prepare view from @views to functional space.
   "
  []
  (let [id-txt "layered-for-tabs"
        id-key (keyword id-txt)
        title "DB View"]
    (do
      (if (= (contains? @views id-key) false)
        (do
          (swap! views (fn [storage] (merge storage {id-key {:component (new JLayeredPane) :id id-txt :title title}})))
          (.add (get-in @views [id-key :component]) (db-viewer-menu) (new Integer 1000))
          (doall (map (fn [tab-data]
                        (.add (get-in @views [id-key :component]) (prepare-table-with-map (get-in tab-data [:prop :bounds] [10 10 100 100]) tab-data) (new Integer 5)))
                      (calculate-bounds 20 5)))))

      (reset! active-tab id-key))))



;; ┌──────────────────┐
;; │                  │
;; │ Config generator │
;; │                  │
;; └──────────────────┘


(def set-confgen-header-file (fn [title] (mig-panel
                                 :constraints ["" "0px[grow, center]0px" "0px[]0px"]
                                 :items [[(label :text title :font (getFont 16) :foreground (get-color :foreground :dark-header))]]
                                 :background (get-color :background :dark-header)
                                 :border (line-border :thickness 10 :color (get-color :background :dark-header)))))

(def set-confgen-header-block (fn [title] (label :text title :font (getFont 16 :bold) 
                                        :border (compound-border  (line-border :bottom 2 :color (get-color :decorate :underline)) (empty-border :bottom 5)))))

(def set-confgen-header-param (fn [title] (label :text title :font (getFont 14 :bold))))

(def cg-combobox (fn [model] (mig-panel
                              :constraints ["" "0px[200:, fill, grow]0px" "0px[30:, fill, grow]0px"]
                              :items [[(combobox :model model :font (getFont 14) 
                                                 :background (get-color :background :combobox) 
                                                 :size [200 :by 30])]])))

(def cg-input (fn [value] (mig-panel
                         :constraints ["" "0px[200:, fill, grow]0px" "0px[30:, fill, grow]0px"]
                         :items [[(text :text value :font (getFont 14) 
                                        :background (get-color :background :input)
                                        :border (compound-border (empty-border :left 10 :right 10 :top 5 :bottom 5) 
                                                                 (line-border :bottom 2 :color (get-color :decorate :gray-underline))))]])))



(def set-confgen-component-block
  (fn [start-key]
    (let [param (fn [key] (get-in @configuration (join-vec start-key key)))
          type? (fn [key] (= (param [:type]) key))
          comp? (fn [key] (= (param [:component]) key))
          name (if (nil? (param [:name])) (key-to-title (last start-key)) (str (param [:name])))]
      (cond (= (param [:display]) :edit)
            (mig-panel
             :constraints ["wrap 1" "20px[]50px" "5px[]0px"]
             :border (cond (type? :block) (empty-border :bottom 10)
                           :else nil)
             :items (join-mig-items
                     (cond  (type? :block) (set-confgen-header-block name)
                            (type? :param) (set-confgen-header-param name))
                     (if-not (nil? (param [:doc])) (textarea (str (param [:doc])) :font (getFont 14)) ())
                     (if (and (type? :block) (not (nil? (param [:doc])))) (label :border (empty-border :top 10)) ())
                     (cond (comp? :selectbox) (cg-combobox (cond (= (last start-key) :lang) (map #(txt-to-UP %1) (join-vec (list (param [:value])) [:en]))
                                                                (vector? (param [:value])) [(txt-to-title (first (param [:value])))]
                                                                :else (vec (list (param [:value])))))
                           (comp? :checkbox) (cg-combobox (if (= (param [:value]) true) [true false] [false true]))
                           (or (comp? :textlist) (comp? :text) (comp? :textnumber) (comp? :textcolor)) (cg-input (str (param [:value])))
                           (map? (param [:value])) (map (fn [next-param]
                                                          (set-confgen-component-block (join-vec start-key [:value] (list (first next-param)))))
                                                        (param [:value]))
                           :else (textarea (str (param [:value])) :font (getFont 12)))))
            :else ()))))


;; (get-in @configuration [:resource.edn])
;; ;; => {:configuration-path {:type :param, :display :edit, :component :text, :value "config"}}
;; (get-in language [:pl :value :configuration-attribute])
;; ;; => {:type :param, :display :edit, :component :text, :value "config"}


(def create-config-gen
  "Description
     Join config generator parts
   "
  (fn [start-key] (let [map-part (get-in @configuration start-key)]
                    (cond (= (get-in map-part [:display]) :edit)
                          (mig-panel
                            ;; :size [(first @right-size) :by (second @right-size)]
                           :border (line-border :bottom 50 :color (get-color :background :main))
                           :constraints ["wrap 1" (string/join "" ["20px[:" (first @right-size) ", grow, fill]20px"]) "20px[]20px"]
                           :items (join-mig-items
                                   ;; Header of section/config file
                                   (set-confgen-header-file (get-in map-part [:name]))
                                   ;; Foreach on init values and create configuration blocks
                                   (map
                                    (fn [param]
                                      (set-confgen-component-block (join-vec start-key [:value] (list (first param)))))
                                    (get-in map-part [:value]))))))))


;; (get-in @configuration [:init.edn])

(def search-config-files
  "Description
     Search files and return paths
   "
  (fn [global-config path]
    (let [root (get-in @global-config path)
          type (get-in root [:type])]
      (cond (= type :file) path
            (= type :directory) (search-config-files global-config (join-vec path [:value])) 
            (nil? type) (map
                         (fn [leaf]
                           (search-config-files global-config (join-vec path [(first leaf)])))
                         root)))))

(def prepare-config-paths
  "Search and finde file fragemnt in config map, next return paths list to this fragments"
  (fn [conf] (all-vec-to-floor
              (map (fn [option]
                     (search-config-files conf [(first option)]))
                   @conf))))

(def create-view-conf-gen
  "Discription
     Return expand button with config generator GUI
     Complete component
   "
  (fn [] (expand-btn (get-lang-btns :settings)
                     (map (fn [path]
                            (let [name (get-in @configuration (join-vec path [:name]))
                                  id (last path)]
                              (expand-child-btn name (fn [e] (create-view id name (create-config-gen path))))))
                          (prepare-config-paths configuration)))))



;; ┌──────────────────────────┐
;; │                          │
;; │ App layout and GUI build │
;; │                          │
;; └──────────────────────────┘

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
                      :constraints ["wrap 1" "0px[fill, grow]0px" "0px[25]0px[fill,grow]0px"]
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
   ;; TODO: Components are writing to output, they can not
   :bounds app-bounds
   :items [(mig-panel
            :constraints [""
                          "0px[50, fill]0px[200, fill]0px[fill, grow]15px"
                          "0px[fill, grow]39px"]
            :items [[(label-fn :background "#eee" :size [50 :by 50])]
                    [(mig-app-left-f  [(expand-btn "Alerty"
                                                   (expand-child-btn "Alert 1 \"Test\""  (fn [e] (alerts-s :set {:header "Test" :body "Bardzo dluga testowa wiadomość, która nie jest taka prosta do ogarnięcia w seesaw."} (message alerts-s) 3)))
                                                   (expand-child-btn "Alert 2 \"Witaj\"" (fn [e] (alerts-s :set {:header "Witaj" :body "Świecie"} (message alerts-s) 5))))]
                                      [(expand-btn "Widoki"
                                                   (expand-child-btn "DB View" (fn [e] (create-view-db-view)))
                                                   (expand-child-btn "Test"    (fn [e] (create-view "test1" "Test 1" (label :text "Test 1"))))
                                                   (expand-child-btn "Test 2"  (fn [e] (create-view "test2" "Test 2" (label :text "Test 2")))))]
                                      [(create-view-conf-gen)])]
                    [(mig-app-right-f [(label)]
                                      [(label)])]])]))


(defn create-bar-with-open-tabs
  "Description
    This function create bar with tabs who linking to open views.
   "
  [id-key]
  (cond
    (> (count @views) 0) (vec (map (fn [item]
                                     (let [item-key (get (second item) :title)]
                                       (tab-btn item-key item-key (if (identical? (first item) id-key) true false) [100 25]
                                                (fn [e] (do
                                                          (reset! views (dissoc @views (first item)))
                                                          (if (get (config (.getParent (seesaw.core/to-widget e)) :user-data) :active)
                                                            (reset! active-tab @last-active-tab)
                                                            (reset! active-tab @active-tab))))
                                                (fn [e] (do
                                                          (reset! active-tab (first item))
                                                          (config! (select (getRoot app) [:#app-functional-space])
                                                                   :items [(scrollable (get (second item) :component)
                                                                                       :border nil
                                                                                       :id (keyword (get (second item) :id)))]))))))
                                   @views))
    :else [(label)]))




;; ┌──────────────────────────────────────┐
;; │                                      │
;; │ Supervisor for open views in tab bar │
;; │                                      │
;; └──────────────────────────────────────┘
(add-watch active-tab :refresh (fn [key atom old-state new-state]
                                 (do
                                   (reset! last-active-tab old-state)
                                   (cond
                                     (contains? @views new-state) (do
                                                                    (config! (select (getRoot app) [:#app-functional-space]) :items [(scrollable (get-in @views [new-state :component]) :border nil :id (keyword (get-in @views [new-state :id])))])
                                                                    (config! (select (getRoot app) [:#app-tabs-space]) :items (create-bar-with-open-tabs new-state)))
                                     (> (count @views) 0) (reset! active-tab (first (first @views)))
                                     :else (do
                                             (reset! last-active-tab :none)
                                             (config! (select (getRoot app) [:#app-functional-space]) :items [(label)])
                                             (config! (select (getRoot app) [:#app-tabs-space]) :items [(label)]))))
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
         (.add app (slider-ico-btn (stool/image-scale icon/scheme-grey-64-png menu-icon-size) 0 menu-icon-size "DB View"
                                   {:onclick (fn [e] (create-view-db-view))}) (new Integer 10))
         (.add app (slider-ico-btn (stool/image-scale icon/I-64-png menu-icon-size) 1 menu-icon-size "Powiadomienia" {:onclick (fn [e] (alerts-s :show))}) (new Integer 10))

        ;;  (onresize-f app)
         (.repaint app))))



(def refresh-layered-for-tables
  "Description
     Refresh bounds of DB View JLayredPane.
   "
  (fn [] (do (if (contains? @views :layered-for-tabs)
               (let [max-w (apply max (map (fn [item]  (+ (.getX (config item :bounds)) (.getWidth  (config item :bounds)))) (seesaw.util/children (get-in @views [:layered-for-tabs :component]))))
                     parent-w (getWidth (.getParent (get-in @views [:layered-for-tabs :component])))
                     max-h (apply max (map (fn [item]  (+ (.getY (config item :bounds)) (.getHeight  (config item :bounds)))) (seesaw.util/children (get-in @views [:layered-for-tabs :component]))))
                     parent-h (getHeight (.getParent (get-in @views [:layered-for-tabs :component])))]
                 (do (.setPreferredSize (get-in @views [:layered-for-tabs :component]) (new Dimension (if (> parent-w max-w) parent-w max-w) (if (> parent-h max-h) parent-h max-h)))
                     (.setSize (get-in @views [:layered-for-tabs :component]) (new Dimension (if (> parent-w max-w) parent-w max-w) (if (> parent-h max-h) parent-h max-h)))))

               (.setPreferredSize (get-in @views [:layered-for-tabs :component]) (new Dimension
                                                                                      (getWidth  (.getParent (get-in @views [:layered-for-tabs :component])))
                                                                                      (getHeight (.getParent (get-in @views [:layered-for-tabs :component])))))))))


(def app-functional-space-resize
  "Description
     Resize app functional space (this space on right).
   "
  (fn [e] (let [AFS (firstToWidget (getChildren (findByID e :#app-functional-space)))
                leaf (try (seesaw.util/children AFS) (catch Exception e (str e)))
                w (- (getWidth AFS) 20)
                h (- (getHeight AFS) 20)]
            (if (= leaf nil) (fn []) (do
                                       ;; Refresh size of app functional space (space on right)
                                       (reset! right-size [w h])
                                      ;;  (config! (seesaw.core/to-widget (get-in @views [@active-tab :component])) :size [w :by h])
                                       )))))

(def onresize-f
  "Description:
      Resize component inside JLayeredPane on main frame resize event.
   "
  (fn [e] (do
            refresh-layered-for-tables
            (template-resize jarmanapp)
            (alerts-rebounds-f e)
            (app-functional-space-resize e)
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
