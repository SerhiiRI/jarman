(ns jarman.gui.gui-app
  (:import (javax.swing JLayeredPane JLabel JTable JComboBox DefaultCellEditor JCheckBox)
           (javax.swing.table TableCellRenderer TableColumn)
           (java.awt.event MouseEvent)
           (jarman.test DateTime)
           (java.awt Color Component))
  (:require [clojure.string :as string]
            ;; resource 
            [jarman.resource-lib.icon-library :as icon]
            [clojure.pprint :as pp]
            ;; logics
            [seesaw.util :as u]
            [jarman.config.config-manager :as cm]
            [jarman.gui.gui-tools :refer :all :as gtool]
            [jarman.gui.gui-components :refer :all :as gcomp]
            [jarman.gui.gui-alerts-service :refer :all]
            [jarman.gui.gui-views-service :refer :all :as vs]
            ;; deverloper tools 
            [jarman.tools.swing :as stool]
            [jarman.config.spec :as sspec]
            [jarman.config.init :as iinit]
            [jarman.logic.metadata :as mmeta]
            [jarman.tools.lang :refer :all :as lang]
            [jarman.gui.gui-seed :refer :all]
            [jarman.gui.gui-config-generator :refer :all :as cg]
            ;; [jarman.logic.view :as view]
            [jarman.gui.gui-docs :as docs]
            [jarman.gui.gui-seed :as gseed]
            [jarman.plugin.table :as gtable]
            [jarman.logic.view-manager :as vmg]
            [jarman.logic.session :as session]
            ;; [jarman.logic.view :refer :all] 
            ;; TEMPORARY!!!! MUST BE REPLACED BY CONFIG_MANAGER
            ))


;; ┌────────────────────────────┐
;; │                            │
;; │ JLayeredPane Popup Service │
;; │                            │
;; └────────────────────────────┘



(def popup-menager (atom nil))


(defn ontop-panel
  [popup-menager storage-id z-index & {:keys [size title body] :or {size [600 400] title "Popup" body (label)}}]
  (let [last-x (atom 0)
        last-y (atom 0)
        w (first size)
        h (second size)]
    (mig-panel
     :constraints ["wrap 1" "0px[fill, grow]0px" "0px[fill, center]0px[fill, grow]0px"]
     :bounds (middle-bounds (first @atom-app-size) (second @atom-app-size) w h)
     :border (line-border :thickness 2 :color (get-color :decorate :gray-underline))
     :background "#fff"
     :id storage-id
     :visible? true
     :items (join-mig-items (mig-panel
                             :constraints ["" "10px[grow]0px[30, center]0px" "5px[]5px"]
                             :background "#eee"
                             :items (join-mig-items
                                     (label :text title)
                                     (label :icon (stool/image-scale icon/up-grey1-64-png 25)
                                            :listen [:mouse-clicked (fn [e] (cond (> (.getHeight (getParent (getParent e))) 50)
                                                                                  (config! (getParent (getParent e)) :bounds  [(.getX (getParent (getParent e)))
                                                                                                                               (.getY (getParent (getParent e)))
                                                                                                                               w 30])
                                                                                  :else (config! (getParent (getParent e)) :bounds [(.getX (getParent (getParent e)))
                                                                                                                                    (.getY (getParent (getParent e)))
                                                                                                                                    w h])))
                                                     :mouse-entered (fn [e] (config! e :cursor :hand))])
                                     (label :icon (stool/image-scale icon/x-blue2-64-png 20)
                                            :listen [:mouse-clicked (fn [e] ((@popup-menager :remove) storage-id))
                                                     :mouse-entered (fn [e] (config! e :cursor :hand))])))
                            (mig-panel
                             :constraints ["wrap 1" "0px[grow, center]0px" "0px[grow, center]0px"]
                             :background "#fff"
                             :items (join-mig-items body)))
     :listen [:mouse-clicked (fn [e] ((@popup-menager :move-to-top) storage-id))
              :mouse-dragged (fn [e] (do
                                       (if (= @last-x 0) (reset! last-x (.getX e)))
                                       (if (= @last-y 0) (reset! last-y (.getY e)))
                                       (let [bounds (config e :bounds)
                                             pre-x (- (+ (.getX bounds) (.getX e)) @last-x)
                                             pre-y (- (+ (.getY bounds) (.getY e)) @last-y)
                                             x (if (> pre-x 0) pre-x 0)
                                             y (if (> pre-y 0) pre-y 0)
                                             w (.getWidth  bounds)
                                             h (.getHeight bounds)]
                                         (config! e :bounds [x y w h]))))])))

(def atom-popup-hook (atom (label :visible? false)))

;; ┌──────────────┐
;; │              │
;; │ Dialog popup │
;; │              │
;; └──────────────┘

(def create-dialog--answer-btn
  (fn [txt func]
    (label
     :text txt
     :halign :center
     :listen [:mouse-clicked func
              :mouse-entered (fn [e] (hand-hover-on e) (button-hover e))
              :mouse-exited  (fn [e] (button-hover e (get-color :background :button_main)))]
     :background (get-color :background :button_main)
     :border (compound-border (empty-border :bottom 10 :top 10)
                              (line-border :bottom 2 :color (get-color :decorate :gray-underline))))))

(def create-dialog-yesno
  (fn [title ask size]
    (-> (custom-dialog
         :title title
         :modal? true
         :resizable? false
         :content (mig-panel
                   :size [(first size) :by (second size)]
                   :constraints ["" "0px[fill, grow]0px" "0px[grow, center]0px"]
                   :items (join-mig-items
                           (mig-panel
                            :constraints ["wrap 1" "0px[fill, grow]0px" "0px[]0px"]
                            :items (join-mig-items (textarea ask :halign :center :font (getFont 14) :border (empty-border :thickness 12))
                                                   (mig-panel
                                                    :constraints ["" "10px[fill, grow]10px" "10px[]10px"]
                                                    :items (join-mig-items (create-dialog--answer-btn (get-lang-btns :yes) (fn [e] (return-from-dialog e "yes")))
                                                                           (create-dialog--answer-btn (get-lang-btns :no)  (fn [e] (return-from-dialog e "no")))))))))
         :parent (getParent @atom-popup-hook))
        pack! show!)))

(def create-dialog-ok
  (fn [title ask size]
    (-> (custom-dialog
         :title title
         :modal? true
         :resizable? false
         :content (mig-panel
                   :size [(first size) :by (second size)]
                   :constraints ["" "0px[fill, grow]0px" "0px[grow, center]0px"]
                   :items (join-mig-items
                           (mig-panel
                            :constraints ["wrap 1" "0px[fill, grow]0px" "0px[]0px"]
                            :items (join-mig-items (textarea ask :halign :center :font (getFont 14) :border (empty-border :thickness 12))
                                                   (join-mig-items (create-dialog--answer-btn "OK" (fn [e] (return-from-dialog e "OK"))))))))
         :parent (getParent @atom-popup-hook))
        pack! show!)))

;; ┌───────────────┐
;; │               │
;; │ Popup service │
;; │               │
;; └───────────────┘

(defn create-popup-service
  [atom-popup-hook]
  (let [atom-popup-storage (atom {})
        z-index (atom 1000)
        JLP (getParent @atom-popup-hook)]
    (fn [action & {:keys [title, body, size] :or {title "Popup" body (label :text "Popup") size [600 400]}}]
      (let [unique-id (keyword (random-unique-id))
            template (fn [component] ;; Set to main JLayeredPane new popup panel
                       (do (swap! atom-popup-storage (fn [popups] (merge popups {unique-id component}))) ;; add new component to list with auto id
                           (swap! z-index inc)
                           (.add JLP (get-in @atom-popup-storage [unique-id]) (new Integer @z-index))
                           (.repaint JLP)))]
        (cond
          (= action :remove) (fn [id]
                               (let [elems-in-JLP (seesaw.util/children JLP)
                                     elems-count  (count elems-in-JLP)]
                                 (reset! atom-popup-storage (dissoc @atom-popup-storage id)) ;; Remove popup from storage
                                 (doall (map (fn [index] ;; remove popup from main JLayeredPane
                                               (cond (= (config (nth elems-in-JLP index) :id) id) (.remove JLP index)))
                                             (range elems-count)))
                                 (.repaint JLP)))  ;; Refresh GUI
          (= action :move-to-top) (fn [id] ;; Popup order, popup on top
                                    (let [elems-in-JLP (seesaw.util/children JLP)
                                          elems-count  (count elems-in-JLP)]
                                      (doall (map (fn [index] ;; Change popup order on JLayeredPane 
                                                    (cond (= (config (nth elems-in-JLP index) :id) id)
                                                          (.setLayer JLP (nth elems-in-JLP index) @z-index 0)))
                                                  (range elems-count)))
                                      (.repaint JLP)) ;; Refresh GUI
                                    )
          (= action :new-test)    (template (ontop-panel popup-menager unique-id z-index))
          (= action :new-message) (template (ontop-panel popup-menager unique-id z-index :title title :body body :size size))
          (= action :show)        (println @atom-popup-storage)
          (= action :get-atom-storage) atom-popup-storage
          (= action :ok)          (create-dialog-ok title body size)
          (= action :yesno)       (create-dialog-yesno title body size))))))

;; (new-layered-id-for-tables-visualizer)
;; (new-test-for-tabs)
;; (new-layered-id-for-tables-visualizer)
;; @views
;; ================================================VVVVVVVVVV Table in database view
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
  [meta offset max-tabs-inline]
  (let [sizes (partition-all max-tabs-inline (calculate-tables-size meta))
        tables (calculate-tables-size-with-tabs meta)
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

(defn table-editor--element--header
  "Create header GUI component in editor for separate section"
  [name] (label :text name :font (getFont 14 :bold)
                :foreground dark-grey-color
                :border (line-border :bottom 2 :color (get-color :background :header))))

(defn table-editor--component--column-picker-btn
  "Description:
     Select button who table-editor--component--column-picker, can work with another same buttons"
  [txt func] (let [color "#eee";; (get-color :group-buttons :background)
                   color-hover (get-color :group-buttons :background-hover)
                   color-clicked (get-color :group-buttons :clicked)
                   bg-btn (atom color)
                   id-btn :table-editor--component--column-picker-btn]
               (mig-panel
                :constraints ["" "15px[100:, fill ]15px" "5px[fill]5px"]
                :border (line-border :left 0 :right 0 :top 1 :bottom 1 :color "#eee";;(get-color :border "#fff")
                                     );;heyyy grey
                :id id-btn
                :background color
                :user-data bg-btn
                :listen [:mouse-entered (fn [e] (config! e :cursor :hand :background color-hover))
                         :mouse-exited  (fn [e] (config! e :background @bg-btn))
                         :mouse-clicked  (fn [e] (println "color picker")
                                           (cond
                                                   (= @bg-btn color)
                                                   (do ;; reset bg and atom inside all buttons in parent if id is ok
                                                     (doall (map (fn [b] (cond (= (config b :id) id-btn)
                                                                               (do (config! b :background color)
                                                                                   (reset! (config b :user-data) color))))
                                                                 (seesaw.util/children (.getParent (seesaw.core/to-widget e)))))
                                                ;; reset atom with color
                                                     (reset! bg-btn color-clicked)
                                                ;; update atom with color
                                                     (config! e :background @bg-btn)
                                                     (func e))))]
                :items [[(label :text txt
                                :maximum-size  [100 :by 100]
                                :font (getFont 12))]])))

(def table-editor--element--header-view
  (fn [value]
    (text :text value
          :font (getFont 14)
          :background (get-color :background :input)
          :editable? false
          :border (compound-border (empty-border :left 10 :right 10 :top 5 :bottom 5)
                                   (line-border :bottom 2 :color (get-color :decorate :gray-underline))))))

(defn switch-column-to-editing
  [work-mode local-changes path-to-value column]
  (let [param-to-edit (fn [param enabled]
                        (cond
                          (in? [true false] (get column param))
                          (do
                            (config!
                             (gcomp/input-checkbox
                              :txt (lang/convert-key-to-title (str param))
                              :local-changes local-changes
                              :store-id (lang/join-vec column [param])
                              :val (get column param)
                              :enabled? enabled)
                             :background gcomp/light-light-grey-color))
                          :else
                          (config! (gcomp/inpose-label (lang/convert-key-to-title (str param))
                                                       (gcomp/input-text-with-atom
                                                        :local-changes local-changes
                                                        :store-id (lang/join-vec path-to-value [param])
                                                        :val (str (get column param))
                                                        :enabled? enabled)
                                                       :font-color dark-grey-color
                                                       :vtop 10)
                                   :background gcomp/light-light-grey-color)))]
    (mig-panel ;; Editable parameters list
     :constraints ["wrap 1" "20px[250:,fill]5px" "0px[fill]0px"]
     :preferred-size [910 :by 360]
     :background gcomp/light-light-grey-color
     :items (join-mig-items
             (filter
              #(not (nil? %))
              (cond
                (= work-mode "developer")
                (map
                 (fn [column-parameter]
                   (let [key-param (first column-parameter)
                         path-to-value (lang/join-vec path-to-value [key-param])]
                     (param-to-edit key-param true))) ;; Parameter value
                 column)
                (= work-mode "admin")
                (list
                 (param-to-edit :representation true)
                 (param-to-edit :description true)
                 (param-to-edit :default-value true)
                 (param-to-edit :private? false)
                 (param-to-edit :editable? false))
                (= work-mode "user")
                (list
                 (param-to-edit :representation false)
                 (param-to-edit :description false)
                 (param-to-edit :default-value false)
                 (param-to-edit :private? false)
                 (param-to-edit :editable? false))))))))


(defn- get-component-add-column [data]
  (condp = data
    "date" ["a" "b" "c"]
    "time" ["a"]
    "date-time" ["s" "c" "r"]
    "simple-number" ["c" "r"]
    "float-number" ["k" "r" "l" "p"]
    "boolean" ["q" "r"]
    "linking-table" ["w" "w"]
    "big-text" ["c" "r"]
    "short-text" ["c" "r"]
    ["default"]))

(defn panel-for-input
  [name description comp-key cmpts-atom]
  (let [check (checkbox :text name :background gcomp/light-light-grey-color :font (getFont 12))
        sel-comp-type (DateTime/getBar (into-array ["date" "time" "date-time"
                                                    "simple-number" "float-number"
                                                    "boolean" "linking-table"
                                                    "big-text" "short-text"]))
        sel-col-type (DateTime/getBar (into-array [" "]))
        model-sel (.getModel sel-col-type)]
    (config! check  :listen [:action-performed (fn [e]
                                                 (swap! cmpts-atom assoc (keyword name) (.isSelected check)))])
    (mig-panel
     :constraints ["wrap 1" "0px[grow, left]0px" "0px[]px"]
     ;;:preferred-size [900 :by 400]
     :background gcomp/light-light-grey-color
     :items [[(mig-panel
               :constraints ["wrap 1" "20px[250:, fill, grow, left]0px" "10px[]10px"]
               :background gcomp/light-light-grey-color
               :items [[(label :text description
                               :foreground dark-grey-color
                             ;;  :font (getFont 14)
                               :halign :left)]
                       [(cond 
                          (= comp-key :check) check
                          (= comp-key :select-comp-type) sel-comp-type
                          (= comp-key :select-col-type) sel-col-type
                          :else
                          (config! (gcomp/input-text :placeholder name
                                                     :style [:halign :left])
                                   :listen [:caret-update
                                            (fn [e] (do (swap! cmpts-atom assoc (keyword name) (text e))))]))]])]])))

;;  heyyyy
(defn table-editor--element--btn-add-column
  [func]
  (mig-panel ;; Button for add column to table
   :constraints ["wrap 1" "10px[grow, fill, center]0px" "0px[fill]0px"]
   :items [[(flow-panel :items (list (label :text "add" :font (getFont 12)))
                        :align :left
                        :listen [:mouse-clicked (fn [e] (do (func e)))
                                 :mouse-entered (fn [e] (config! e :cursor :hand))])]]))

(defn- add-column-panel [table-name]
  (let [cmpts-atom (atom {:editable? false :private? false})
        inp-name (panel-for-input "field" "database column name" :field cmpts-atom)
        inp-repr (panel-for-input "representation" "name for end-user, by default = field" :field cmpts-atom)
        inp-descr (panel-for-input "description" "some description information, used for UI" :field cmpts-atom)
        inp-def (panel-for-input "default-value" "default value" :field cmpts-atom)
        inp-selct (panel-for-input "component-type" "database type of column" :select-comp-type cmpts-atom)
        inp-db (panel-for-input "column-type" "type of data to db" :select-col-type cmpts-atom)
        inp-pr (panel-for-input "private?" "true if column must be hided for user" :check cmpts-atom)
        inp-ed (panel-for-input "editable?" "symbol for representation information by UI" :check cmpts-atom)
        selct-comp (second (.getComponents (first (.getComponents inp-selct)))) 
        selct-col (second (.getComponents (first (.getComponents inp-db))))
        main-panel (mig-panel :constraints ["wrap 1" "0px[100:,fill]0px" "0px[fill]10px"])]
    (config! selct-comp
             :listen [:action-performed (fn [e]
                                          (swap! cmpts-atom assoc :component-type [(if (= (.toString (.getSelectedItem selct-comp))
                                                                                          "date-time") "dt" 
                                                                                       (.toString (get (.toString (.getSelectedItem selct-comp)) 0)))])
                                          (.removeAllItems selct-col)
                                          (doall (map (fn [x] (.addItem selct-col x))
                                                      (get-component-add-column (.toString (.getSelectedItem selct-comp))))))])
    (config! selct-col :listen [:action-performed (fn [e]
                                                    (swap! cmpts-atom assoc :column-type (.toString (.getSelectedItem selct-col))))])
    (config! main-panel :items [[(label :text "Adding column" :font (getFont 14 :bold) :foreground blue-color)]
                                [(gcomp/multi-panel [(vertical-panel :items (list inp-name inp-repr inp-descr inp-def))
                                                     (vertical-panel :items (list inp-selct inp-db))
                                                     (vertical-panel :items (list inp-pr inp-ed))]
                                                    cmpts-atom
                                                    table-name
                                                    "" 0)]])))
(defn delete-column [column l-delete]
  (config! l-delete 
           :listen [:mouse-entered (fn [e] (config! e :cursor :hand))
                    :mouse-clicked (fn [e] (create-dialog-yesno "Delete"
                                                                (str
                                                                 "Delete column "
                                                                 (name (:field column))
                                                                 "?")
                                                                [180 140]))]
           :visible? true))

;;heyyy
(defn table-editor--component--column-picker
  "Create left table editor view to select column which will be editing on right table editor view"
  [work-mode local-changes column-editor-id columns path-to-value table-name]
  (let [l-delete (label :text "delete" :visible? false
                        :font (getFont 12)
                        :border (empty-border :left 15))]
    (mig-panel :constraints ["wrap 1" "0px[100:, fill]0px" "0px[fill]0px"]
               :items
               (join-mig-items
                (label :text "Columns" :border (empty-border :top 5 :bottom 5) :foreground blue-color)
                (map (fn [column index]
                       (let [path-to-value (join-vec path-to-value [index])
                             meta-panel (mig-panel
                                         :constraints ["wrap 1" "grow, fill" ""]
                                         :items [[(label :text (name (:field column))
                                                         :border (empty-border :bottom 5)
                                                         :font (getFont 14 :bold) :foreground blue-color)]
                                                 [(config! (switch-column-to-editing work-mode local-changes path-to-value column))]])]
                         (table-editor--component--column-picker-btn
                         (get-in column [:representation])
                          (fn [e]
                            (config! l-delete 
                                     :listen [:mouse-entered (fn [e] (config! e :cursor :hand))
                                              :mouse-clicked (fn [e] (delete-column column l-delete))]
                                     :visible? true)
                            (config! (select (to-root @app) [(convert-str-to-hashkey column-editor-id)])
                                     :items (gtool/join-mig-items meta-panel))))))
                     columns (range (count columns)))
                (label :text "Actions" :border (empty-border :top 5 :bottom 5)  :foreground blue-color)
                (table-editor--element--btn-add-column (fn [e]                                              
                                                         (config! l-delete :visible? false)
                                                         (config! (select (to-root @app) [(convert-str-to-hashkey column-editor-id)])
                                                                  :items (gtool/join-mig-items (add-column-panel table-name)))))
                l-delete))))

(defn table-editor--component--space-for-column-editor
  "Create right table editor view to editing selected column in left table editor view"
  [column-editor-id] (mig-panel :constraints ["wrap 1" "0px[grow, fill]0px" "0px[grow, fill]0px"]
                                :id (keyword column-editor-id)
                               ;; :items [[(label)]]
                               ;; :border (line-border :left 4 :color (get-color :border :dark-gray))
                                ))

(def get-table-configuration-from-list-by-table-id
  (fn [tables-configurations table-id]
    (let [map (first (filter (fn [item] (= table-id (get item :id))) tables-configurations))]
      (do ;;(println map)
        map))))

(defn table-editor--element--btn-save
  "Description:
     Invoker-id is a parent whos invoke editor. I mean DB Visualizer."
  [local-changes table invoker-id]
  (table-editor--component--bar-btn :edit-view-save-btn (get-lang-btns :save) icon/agree-grey-64-png icon/agree-blue-64-png
                                    (fn [e]
                                      (let [new-table-meta (atom table)]
                                        (doall
                                         (map
                                          (fn [change]
                                            (let [path (first change)
                                                  valu (second change)]
                                              (println (str "<br/>" path " \"" (get-in table path) "\" -> \"" valu "\""))
                                              (swap! new-table-meta (fn [atom-table] (assoc-in atom-table (first change) (second change))))))
                                          @local-changes))
                                        ;; (println "New table meta: " @new-table-meta)
                                        (mmeta/do-change
                                         (mmeta/apply-table table @new-table-meta)
                                         table @new-table-meta)
                                        (cm/swapp)
                                        (println "reload invoker" invoker-id)
                                        (if-not (nil? invoker-id) ((@gseed/jarman-views-service :reload) invoker-id))
                                        ((@gseed/jarman-views-service :reload))
                                        (@gseed/alert-manager :set {:header (gtool/get-lang-alerts :success) :body (gtool/get-lang-alerts :changes-saved)} (@gseed/alert-manager :message gseed/alert-manager) 5)))))


(defn table-editor--element--btn-show-changes
  [local-changes table] (table-editor--component--bar-btn :edit-view-back-btn (get-lang-btns :show-changes) icon/refresh-grey-64-png icon/refresh-blue-64-png
                                                          (fn [e] (@popup-menager :new-message
                                                                                  :title "Changes list"
                                                                                  :body (textarea (let [changes (atom "")]
                                                                                                    (doall
                                                                                                     (map
                                                                                                      (fn [change]
                                                                                                        (let [path (first change)
                                                                                                              valu (second change)]
                                                                                                          (swap! changes (fn [txt] (str txt "<br/>" path " \"" (get-in table path) "\" -> \"" valu "\"")))))
                                                                                                      @local-changes))
                                                                                                    @changes))
                                                                                  :size [400 300]))))

(defn create-view--table-editor
  "Description:
     Create view for table editor. Marge all component to one big view.
   "
  [view-id work-mode tables-configurations table-id invoker-id]
  (let [local-changes  (atom nil)
        table          (get-table-configuration-from-list-by-table-id (tables-configurations) table-id)
        table-name (get-in table [:prop :table :representation])
        col-path-to-value [:prop :columns]
        tab-path-to-value [:prop :table]
        columns        (get-in table col-path-to-value) ;; Get columns list
        table-property (get-in table tab-path-to-value) ;; Get table property
        elems          (join-vec
                    ;; Table info
                        [[(mig-panel :constraints ["" "0px[grow, fill]5px[]0px" "10px[fill]10px"] ;; menu bar for editor
                                     :items (join-mig-items
                                             [(table-editor--element--header-view (str "Edit table: \""table-name"\""))]
                                             (cond (in? ["developer" "admin"] work-mode)
                                                   (list [(table-editor--element--btn-save local-changes table invoker-id)]
                                                         [(table-editor--element--btn-show-changes local-changes table)])
                                                   :else [])))]]
                        [[(scrollbox ;; Scroll section bottom title and button save/reload bar
                           (mig-panel
                            :constraints ["wrap 1" "[grow, fill]" "[fill]"]
                            :items (join-mig-items  ;; Table properties 
                                    (table-editor--element--header "Table configuration")
                                    (vec (let [table-property-count (count table-property)
                                               txtsize [150 :by 25]]
                                           [(mig-panel
                                             :constraints ["wrap 3" "2%[30%, fill]0px" "0px[grow, fill]5px"]
                                             ;;:background gcomp/light-light-grey-color
                                             :items (gtool/join-mig-items ;;here
                                                     (let [param-to-edit (fn [param enabled]
                                                                           (cond
                                                                             (in? [true false] (get table-property param))
                                                                             (do
                                                                               (config! (gcomp/input-checkbox
                                                                                         :txt (lang/convert-key-to-title (str param))
                                                                                         :local-changes local-changes
                                                                                         :store-id (lang/join-vec tab-path-to-value [param])
                                                                                         :val (get table-property param)
                                                                                         :enabled? enabled)))
                                                                             :else
                                                                             (config!
                                                                              (gcomp/inpose-label
                                                                               (lang/convert-key-to-title (str param))
                                                                                                  (gcomp/input-text-with-atom
                                                                                                   :local-changes local-changes
                                                                                                   :store-id (lang/join-vec tab-path-to-value [param])
                                                                                                   :val (str (get table-property param))
                                                                                                   :enabled? enabled)
                                                                                                  :vtop 10
                                                                                                  :font-color gcomp/blue-color))))
                                                           meta-params [:representation :description :field :is-system?
                                                                        :is-linker? :allow-linking? :allow-modifing? :allow-deleting?]]
                                                       (cond
                                                         (= work-mode "developer")
                                                         (concat (map #(param-to-edit % true) meta-params)
                                                                 [(param-to-edit :ref true)])
                                                         (= work-mode "admin")
                                                         (conj (map #(param-to-edit % false) (drop 2 meta-params))
                                                               (param-to-edit (first  meta-params) true)
                                                               (param-to-edit (second meta-params) true))
                                                         :else
                                                         (map #(param-to-edit % false) meta-params)))))]))
                                    (gcomp/hr 15);; Columns properties
                                    (table-editor--element--header "Column configuration")
                                    (gcomp/hr 15)
                                    (let [column-editor-id "table-editor--component--space-for-column-editor"]
                                      (mig-panel ;; Left and Right functional space
                                       :constraints ["wrap 2" "0px[fill]0px" "0px[grow, fill]0px"]
                                       :items [[(table-editor--component--column-picker work-mode local-changes column-editor-id columns col-path-to-value table-name)] ;; Left part for columns to choose for doing changes.
                                               [(table-editor--component--space-for-column-editor column-editor-id)] ;; Space for components. Using to editing columns.
                                               ])))))]])
        component (cond
                    (> (count table) 0) (do
                                          (mig-panel
                                           :constraints ["wrap 1" "20px[grow, fill]20px" "0px[fill]0px[grow, fill]0px"]
                                          ;;  :border (line-border :thicness 1 :color "#f00")
                                           :items elems
                                           :border (empty-border :thickness 0)))
                    :else (label :text "Table not found inside metadata :c"))]
    (@gtool/changes-service :add-controller :view-id view-id :local-changes local-changes)
    component))

(defn add-to-view-service--table-editor
  ([tables-configurations table-id]
   (let [table          (get-table-configuration-from-list-by-table-id (tables-configurations) table-id)
         view-id        (keyword (get table :table))  ;; Get name of table and create keyword to check tabs bar (opens views)
         invoker-id     (@gseed/jarman-views-service :get-my-view-id)]
     (do
       (@gseed/jarman-views-service :set-view
                                    :view-id view-id
                                    :title (str "Edit: " (get-in table [:prop :table :representation]))
                                    :tab-tip (str "Edit panel with \"" (get-in table [:prop :table :representation]) "\" table.")
                                    :component-fn (fn [] (create-view--table-editor view-id (session/user-get-permission) tables-configurations table-id invoker-id))
                                    :scrollable? false)))))


;; ┌─────────┐
;; │         │
;; │ DB View │
;; │         │
;; └─────────┘

;; (def show-data-in-table
;;   (fn [table-id-as-key]
;;     (println "Table" table-id-as-key)
;;     (let [table (jarman.logic.metadata/defview user
;;                   :tables [:user :permission]
;;                   :view   [:first_name :last_name :login :permission_name]
;;                   :data   {:inner-join [:permission]
;;                            :column [{:user.id :id} :login :password :first_name :last_name :permission_name :configuration :id_permission]})]
;;       (mig-panel
;;        :constraints ["wrap 1" "0px[grow, fill]0px" "0px[grow, fill]0px"]
;;        :items [[table]]))))


;;  (calculate-bounds 10 4)
(def db-view--apsolute-pop--rmb-table-actions
  "Description:
       Right Mouse Click Menu on table to control clicked table"
  (fn [JLP atom--tables-configurations table-id x y]
    (let [border-c "#bbb"
          selected-tab (filter (fn [tab-conf] (= (second (first tab-conf)) table-id)) @atom--tables-configurations)
          table-name (get (first selected-tab) :table)
          rm-menu (fn [e] (let [popup-menu (seesaw.core/to-widget (.getParent (seesaw.core/to-widget e)))]
                            (.remove  JLP popup-menu)
                            (.repaint JLP)))
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
       :id :db-viewer--component--menu-bar
       :bounds [x y 150 80]
       :background (new Color 0 0 0 0)
       :border (line-border :thickness 1 :color border-c)
       :constraints ["wrap 1" "0px[150, fill]0px" "0px[30px, fill]0px"]
       :items [[(btn "Edit table" icon/pen-blue-64-png (fn [e] (do (rm-menu e)
                                                                   (add-to-view-service--table-editor mmeta/getset table-id))))]
               [(btn "Delete table" icon/basket-blue1-64-png (fn [e]))]
               [(btn "Show relations" icon/refresh-connection-blue-64-png (fn [e]))]]))))

(defn table-visualizer--element--col-as-row
  "Description:
      Create a primary or special row that represents a column in the table"
  [data
   & {:keys [debug]
      :or {debug false}}]
  (if debug (println "--Column as row\n--Data: " data))
  (let [last-x (atom 0)
        last-y (atom 0)
        component (label :text (str (get data :representation))
                         :size [(get data :width) :by (cond
                                                        (= (get data :type) "header") (- (get data :height) 2)
                                                        :else                         (- (get data :height) 0))]
                         :icon (cond
                                 (= (get data :type) "connection")
                                 (stool/image-scale icon/refresh-connection-blue-64-png (/ (+ 8 (get data :height)) 1))
                                 (= (get data :type) "key")
                                 (stool/image-scale icon/refresh-connection-blue-64-png (/ (+ 8 (get data :height)) 1)) ;;(stool/image-scale icon/key-blue-64-png (/ (get data :height) 1))
                                 :else nil)
                         :background (cond
                                       (= (get data :type) "header")     gcomp/light-light-grey-color ;;"#666"
                                       (= (get data :type) "key")        "#e2fbde";;"#e4f8e1" ;;"#ace8a7" ;;"#f7d67c"
                                       (= (get data :type) "connection") "#e2fbde";;"#ace8a7"
                                       :else "#fff")
                         :font (cond
                                       (= (get data :type) "header")     (getFont 12 :bold)
                                       (= (get data :type) "key")        (getFont 12)
                                       (= (get data :type) "connection") (getFont 12)
                                       :else "#fff")
                         :foreground (cond
                                       (= (get data :type) "header") gcomp/blue-color ;;"#fff"
                                       :else "#000"
                                       )
                         :border (cond
                                   (= (get data :type) "header") (compound-border (empty-border :thickness 4))
                                   :else                         (compound-border (empty-border :thickness 4)
                                                                                  ;;(line-border :top 1 :color (get data :border-c))
                                                                                  ))
                         :listen [:mouse-entered (fn [e] (do
                                                           (cond
                                                             (= (get data :type) "header") (config! e :cursor :move))))
                                  :mouse-clicked (fn [e]
                                                   (let [table-id (get (config (getParent e) :user-data) :id)]
                                                     (cond (= (.getButton e) MouseEvent/BUTTON3)
                                                           (let [scrol (select (@gseed/jarman-views-service :get-component :view-id :Database) [:#JLP-DB-Visualizer])
                                                                 JLP (first (seesaw.util/children (first (seesaw.util/children scrol))))]
                                                             (.add JLP
                                                                   (db-view--apsolute-pop--rmb-table-actions ;; Open popup menu for table
                                                                    JLP
                                                                    (mmeta/getset) ;; forward list of table configuration
                                                                    table-id ;; Get table id
                                                                    (- (+ (.getX e) (.getX (config (getParent e) :bounds))) 15) ;; calculate popup position
                                                                    (- (+ (+ (.getY e) (.getY (config e :bounds))) (.getY (config (getParent e) :bounds))) 10))
                                                                   (new Integer 999) ;; z-index
                                                                   ))
                                                           (= (.getClickCount e) 2) ;; Open table editor by duble click
                                                           (add-to-view-service--table-editor mmeta/getset table-id))))
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
                                                              (reset! last-y 0)))])]
    (if debug (println "--Column as row: OK"))
    component))

(defn db-viewer--component--table
  "Description:
     Create one table on JLayeredPane using database map
  "
  [bounds data]
;; (if (get-in data [:prop :table :is-linker?]) (println "data " data))
  (let [bg-c "#fff"
        line-size-hover 2  ;; zwiekszenie bordera dla eventu najechania mysza
        border-c "#aaa"
        border (line-border :thickness 1 :color border-c)
        border-hover (line-border :thickness line-size-hover :color "#000")
        x (nth bounds 0)
        y (+ 0 (nth bounds 1))
        w (nth bounds 2)
        row-h 30  ;; wysokosc wiersza w tabeli reprezentujacego kolumne
        col-in-rows (map (fn [col]
                           (table-visualizer--element--col-as-row {:representation (cond
                                                                                     (and (get-in data [:prop :table :is-linker?]) (contains? col :key-table))
                                                                                     (do ;; Get represetation of linked table
                                                                                       (let [search (filter (fn [table-meta] (= (get-in table-meta [:table]) (get col :key-table))) (mmeta/getset))]
                                                                                         (get-in (first search) [:prop :table :representation])))
                                                                                     :else (get col :representation))
                                                                   :width w :height row-h
                                                                   :type (cond
                                                                           (and (get-in data [:prop :table :is-linker?]) (contains? col :key-table)) "connection"
                                                                           (contains? col :key-table) "key"
                                                                           :else "row")
                                                                   :border-c border-c}))
                         (get-in data [:prop :columns]))  ;; przygotowanie tabeli bez naglowka
        camplete-table (conj col-in-rows (table-visualizer--element--col-as-row {:representation (get-in data [:prop :table :representation]) :width w :height row-h :type "header" :border-c border-c}))  ;; dodanie naglowka i finalizacja widoku tabeli
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
;; (@startup)


;; ((@gseed/jarman-views-service :reload))


(defn create-view--db-view
  "Description:
     Create component and set to @views atom to use in functional space. 
     Add open tab for db-view to open tabs bar.
     Set prepare view from @views to functional space.
   "
  ([]
   (let [rootJLP (new JLayeredPane)
         JLP (new JLayeredPane)
         JLP-bounds (atom {})]
     (doall (map (fn [tab-data]
                   (.add JLP (db-viewer--component--table (get-in tab-data [:prop :bounds] [0 0 100 100]) tab-data) (new Integer 5)))
                 (calculate-bounds (mmeta/getset) 20 5)))
     (.add rootJLP (vertical-panel :items [JLP]
                                   :id :JLP-DB-Visualizer
                                   :border nil
                                   :bounds [0 0 10000 10000]
                                   :listen [:mouse-released (fn [e] (config! e :cursor :default))
                                            :mouse-pressed (fn [e]
                                                             (config! e :cursor :move)
                                                             (let [x (.getX (config e :bounds))
                                                                   y (.getY (config e :bounds))]
                                                               (reset! JLP-bounds {:x (.getX e) :y (.getY e) :x-x (- (.getX e) x) :y-y (- (.getY e) y)})))
                                            :mouse-dragged (fn [e]
                                                        ;;  (println "Drag JLP: " (.getX e) (.getY e))
                                                             (let [new-x (- (.getX e) (get @JLP-bounds :x-x))
                                                                   new-y (- (.getY e) (get @JLP-bounds :y-y))]
                                                               (config! e :bounds [new-x new-y 10000 10000] ;; TODO: need to change width and height on dynamic 
                                                                        )))])
           (new Integer 1))
    ;;  (.setMaximumSize JLP (java.awt.Dimension. 300 300))
    ;;  (.setSize JLP (java.awt.Dimension. 300 300))
     (mig-panel
      :border nil
      :constraints ["wrap 1" "0px[grow, fill]0px" "5px[fill]5px[grow, fill]0px"]
      :items [[(gcomp/menu-bar
                :id :db-viewer--component--menu-bar
                :buttons [["Show all relation" icon/refresh-connection-blue-64-png (fn [e])]
                          ["Save view" icon/agree-grey-64-png (fn [e])]
                          ["Reset view" icon/arrow-blue-left-64-png (fn [e])]
                          ["Reloade view" icon/refresh-blue-64-png (fn [e] ((@gseed/jarman-views-service :reload)))]])]
              [rootJLP]]))))


;; ┌─────────────────────────────────────────┐
;; │                                         │
;; │ Create expand btns for config generator │
;; │                                         │
;; └─────────────────────────────────────────┘


(def create-expand-btns--confgen
  "Discription
     Return expand button with config generator GUI
     Complete component
   "
  (fn [] (button-expand
          (gtool/get-lang-btns :settings)
          (let [current-theme (str (first (cm/get-in-value [:themes :theme_config.edn :selected-theme])) ".edn")
                config-file-list-as-keyword (map #(first %) (cm/get-in-segment []))
                config-file-list-as-keyword-to-display (filter #(let [map-part (cm/get-in-segment (if (vector? %) % [%]))]
                                                                  (and (= :file (get map-part :type))
                                                                       (= :edit (get map-part :display))))
                                                               config-file-list-as-keyword)
                restore-button (button-expand-child (get-lang-btns :restore-last-configuration)
                                                    :onClick (fn [e] (do
                                                                       (if-not (nil? (cm/restore-config)) (@gseed/alert-manager :set {:header "Success!" :body (get-lang-alerts :restore-configuration-ok)} (@gseed/alert-manager :message gseed/alert-manager) 5)))))]
            (reverse
             (conj
              (map (fn [p]
                     (let [path (if (vector? p) p [p])
                           title (get (cm/get-in-segment path) :name)
                           view-id (last path)]
                       (button-expand-child title :onClick (fn [e]
                                                             (@gseed/jarman-views-service
                                                              :set-view
                                                              :view-id view-id
                                                              :title title
                                                              :scrollable? false
                                                              :component-fn (fn [] (cg/create-view--confgen path :message-ok (fn [txt] (@gseed/alert-manager :set {:header "Success!" :body (gtool/get-lang-alerts :changes-saved)} (@gseed/alert-manager :message @gseed/alert-manager) 5)))))))))
                   config-file-list-as-keyword-to-display)
              (let [path [:themes :theme_config.edn]
                    title (get (cm/get-in-segment path) :name)
                    view-id :theme_config.edn]
                (button-expand-child title :onClick (fn [e]
                                                      (@gseed/jarman-views-service
                                                       :set-view
                                                       :view-id view-id
                                                       :title title
                                                       :scrollable? false
                                                       :component-fn (fn [] (cg/create-view--confgen path :message-ok (fn [txt] (@gseed/alert-manager :set {:header "Success!" :body (gtool/get-lang-alerts :changes-saved)} ((@gseed/alert-manager :message) @gseed/alert-manager) 5))))))))
              (let [path [:themes (keyword current-theme)] 
                    title (if (nil? (get (cm/get-in-segment path) :name)) "NIL" (get (cm/get-in-segment path) :name))
                    view-id :current-theme]
                (button-expand-child title :onClick (fn [e]
                                                      (@gseed/jarman-views-service
                                                       :set-view
                                                       :view-id view-id
                                                       :title title
                                                       :scrollable? false
                                                       :component-fn (fn [] (cg/create-view--confgen path :message-ok (fn [txt] (@gseed/alert-manager :set {:header "Success!" :body (str (gtool/get-lang-alerts :changes-saved))} (@gseed/alert-manager :message gseed/alert-manager) 5)))))
                                                      ;; (try
                                                        
                                                      ;;   (catch Exception e (do
                                                      ;;                        (@gseed/alert-manager :set {:header "Warning!" :body (str (gtool/get-lang-alerts :configuration-corrupted) "Exception: " e)} (@gseed/alert-manager :message gseed/alert-manager) 5))))
                                                      )))
              ;; restore-button
              ))))))

;; ┌──────────────────────────┐
;; │                          │
;; │ App layout and GUI build │
;; │                          │
;; └──────────────────────────┘

(def mig-app-left-f
  "Description:
      Vertical layout of elements, left part of app for functions
   Example:
      (mig-app-left-f  [(button-expand 'Ukryte opcje 1' [(some-button)])] [(button-expand 'Ukryte opcje 2')])
   Needed:
      button-expand component is needed to corectly work
   "
  (fn [& args] (scrollbox (mig-panel
                           :id :expand-menu-space
                           :background "#fff"
                           :border (line-border :left 4 :right 4 :color "#fff")
                           :constraints ["wrap 1" "0px[fill, grow]0px" "0px[fill]0px"]
                           :items (vec args))
                          :args [:hscroll :never])))

(def right-part-of-jarman-as-space-for-views-service
  "Description: 
      Vertical layout for tabs and table on right part of app. 
      Tabs are inside horizontal panel on top.
   Example: 
      tabs  -> mig vector with elements    -> [(tab1) (tab2) (tab3)]
      array -> table like rows and columns -> [(table)]  
      (right-part-of-jarman-as-space-for-views-service [(tab-btn 'Tab 1' true) (tab-btn 'Tab 2' false)] [(label-fn :text 'GRID')])
   Needed:
      tab-btn component is needed to corectly work
   "
  (fn [tabs array]
    (let [bg-color "#fff"
          tabs-space (mig-panel
                      :constraints ["" "0px[fill]0px" "0px[]0px"]
                      :id :app-tabs-space
                      :background bg-color
                      :items (join-mig-items tabs))
          views-space (mig-panel
                       :constraints ["wrap 1" "0px[grow, fill]0px" "0px[grow, fill]0px"]
                       :id :app-functional-space
                       :background (new Color 0 0 0 0)
                       :items (join-mig-items array))]
      (reset! gseed/jarman-views-service (vs/new-views-service tabs-space views-space))
      (mig-panel
       :id :operation-space
       :background "#fff"
       :constraints ["wrap 1" "0px[grow, fill]0px" "0px[28, shrink 0]0px[grow, fill]0px"]
       :background "#eee"
       :border (line-border :left 1 :color "#999")
       :items [[(scrollbox tabs-space :hbar-size 3 :args [:vscroll :never])]
               [views-space]]))))

;; (@gseed/jarman-views-service :reload :view-id (keyword "DB Visualiser"))
;; (@gseed/jarman-views-service :get-all-view) 
(defn create-period--period-form
  []
  (vmig
   :vrules "[fill][100, shrink 0, fill][grow, fill]"
   :items [[(gcomp/header-basic "Okresy")]
           [(gcomp/scrollbox
             (mig-panel :constraints ["wrap 4" "10px[fill][fill]50px[fill][fill]10px" "10px[fill]10px"]
                        :items [[(label :text "Organization:")]
                                [(label :text "Frank & Franky Co." :border (line-border :bottom 1 :color "#494949"))]
                                [(label :text "Time:")]
                                [(label :text "12/03/2021 - 11/03/2022"  :border (line-border :bottom 1 :color "#494949"))]
                                [(label :text "Customer:")]
                                [(label :text "Franklyn Badabumc" :border (line-border :bottom 1 :color "#494949"))]
                                [(label :text "Full amount:")]
                                [(label :text "7000,-" :border (line-border :bottom 1 :color "#494949"))]
                                [(label :text "Service:")]
                                [(label :text "Mr. Jarman" :border (line-border :bottom 1 :color "#494949"))]])
             :args [:vscroll :never])]
           [(vmig
             :vrules "[fill]0px[grow, fill]"
             :items [[(gcomp/menu-bar-right :buttons [["Export" icon/excel-64-png (fn [e])]])]
                     [(scrollable (seesaw.swingx/table-x :model [:columns ["Servise month" "Amount" "Payment status"] :rows [["03/2021" "2500,-" "FV: 042/03/2021"]
                                                                                                                             ["04/2021" "2000,-" "FV: 042/04/2021"]
                                                                                                                             ["05/2021" "2500,-" "Expected payment"]]]))]])]]))

(defn get-period-list
  [company-id]
  (cond
    (= company-id 1) {}))

(defn create-period--period-list
  [list-space view-space return-fn company-id]
  (let [period-list (get-period-list company-id)]
    (expand-form-panel
     list-space
     [(gcomp/scrollbox
       (gcomp/vmig
        :items (gtool/join-mig-items
                (gcomp/vmig
                 :items (gtool/join-mig-items
                         (gcomp/button-slim "01/01/2021 - 31/12/2021" :onClick (fn [e] (config! view-space :items (gtool/join-mig-items (create-period--period-form)))))
                         (gcomp/button-slim "01/01/2021 - 31/12/2021" :onClick (fn [e]))
                         (gcomp/button-slim "01/01/2021 - 31/12/2021" :onClick (fn [e]))
                         (gcomp/button-slim "01/01/2021 - 31/12/2021" :onClick (fn [e]))
                         (gcomp/button-slim "01/01/2021 - 31/12/2021" :onClick (fn [e]))
                         (gcomp/button-slim "01/01/2021 - 31/12/2021" :onClick (fn [e]))
                         (gcomp/button-slim "01/01/2021 - 31/12/2021" :onClick (fn [e]))
                         (gcomp/button-slim "01/01/2021 - 31/12/2021" :onClick (fn [e]))
                         (gcomp/button-slim "01/01/2021 - 31/12/2021" :onClick (fn [e]))
                         (gcomp/button-slim "01/01/2021 - 31/12/2021" :onClick (fn [e]))
                         (gcomp/button-slim "01/01/2021 - 31/12/2021" :onClick (fn [e]))
                         (gcomp/button-slim "01/01/2021 - 31/12/2021" :onClick (fn [e]))
                         (gcomp/button-slim "01/01/2021 - 31/12/2021" :onClick (fn [e]))
                         (gcomp/button-slim "01/01/2021 - 31/12/2021" :onClick (fn [e]))
                         (gcomp/button-slim "01/01/2021 - 31/12/2021" :onClick (fn [e]))
                         (gcomp/button-slim "01/01/2021 - 31/12/2021" :onClick (fn [e]))
                         (gcomp/button-slim "01/01/2021 - 31/12/2021" :onClick (fn [e])))))))
      (gcomp/button-return "<< Companys" (fn [e] (invoke-later (config! list-space :items (gtool/join-mig-items (return-fn list-space view-space return-fn))))))]))) 

(defn get-company-list
  [] [{:name "Trashpanda-Team" :id 1} {:name "Frank & Franky" :id 3}
      {:name "Trashpanda-Team" :id 1} {:name "Frank & Franky" :id 3}
      {:name "Trashpanda-Team" :id 1} {:name "Frank & Franky" :id 3}
      {:name "Trashpanda-Team" :id 1} {:name "Frank & Franky" :id 3}
      {:name "Trashpanda-Team" :id 1} {:name "Frank & Franky" :id 3}])

(defn create-period--period-companys-list
  [list-space view-space return-fn] ;; [{:name "Frank & Franky" :id 3}]
  (let 
   [model (get-company-list)]
    (expand-form-panel
     list-space
     (gcomp/vmig
      :items (gtool/join-mig-items
              (map (fn [company]
                     (gcomp/button-slim (str (get company :name))
                                         :onClick (fn [e] (invoke-later (config! list-space :items (gtool/join-mig-items (create-period--period-list list-space view-space return-fn (get company :id))))))))
                   model))))))

(defn create-period-view
  []
  (let [list-space (gcomp/vmig)
        view-space (gcomp/vmig)
        list-space (config! list-space :items (gtool/join-mig-items (create-period--period-companys-list list-space view-space create-period--period-companys-list)))]
    (gcomp/hmig
     :hrules "[shrink 0, fill]0px[grow, fill]"
     :items [[list-space]
             [view-space]]
     :args [:background "#fff"])))


(def jarmanapp
  "Description:
      Main space for app inside JLayeredPane. There is menu with expand btns and space for tables with tab btns.
   "
  (fn [& {:keys [margin-left]
          :or {margin-left 0}}]
    (let [bg-color "#eee"
          vhr-color "#999"]
      (mig-panel
       :id :rebound-layer
       :constraints [""
                     "0px[shrink 0, fill]0px[grow, fill]0px"
                     "0px[grow, fill]0px"]
       :border (line-border :left margin-left :color bg-color)
       :items [;; [(label-fn :background "#eee" :size [50 :by 50])]
               [(mig-app-left-f  [(button-expand "Database"
                                                 [(button-expand-child "DB Visualiser" :onClick (fn [e] (@gseed/jarman-views-service :set-view :view-id "DB Visualiser" :title "DB Visualiser" :component-fn create-view--db-view)))
                                                          ;;  (button-expand-child "Users table" :onClick (fn [e] (@gseed/jarman-views-service :set-view :view-id "tab-user" :title "User" :scrollable? false :component (jarman.logic.view/auto-builder--table-view nil))))
                                                  ])]
                                 [(button-expand "Tables" [] :id :tables-view-plugin :expand :yes)]
                                 [(button-expand "Okresy" [(button-expand-child "Okresy" :onClick (fn [e] (@gseed/jarman-views-service :set-view :view-id "okresy" :title "Okresy" :component-fn create-period-view)))])]
                                 [(create-expand-btns--confgen)]
                                 [(button-expand "Debug items"
                                                 [(button-expand-child "Popup" :onClick (fn [e] (@popup-menager :new-message :title "Hello popup panel" :body (label "Hello popup!") :size [400 200])))
                                                  (button-expand-child "Dialog" :onClick (fn [e] (println (str "Result = " (@popup-menager :yesno :title "Ask dialog" :body "Do you wona some QUASĄĄĄĄ?" :size [300 100])))))
                                                  (button-expand-child "alert" :onClick (fn [e] (@gseed/alert-manager :set {:header "Hello World" :body "Some body once told me..."} (@gseed/alert-manager :message gseed/alert-manager) 5)))])])]
               [(right-part-of-jarman-as-space-for-views-service []
                                                                 [])]]))))

;; (jarman.logic.metadata/getset)
;; (@startup)

;; ┌─────────────┐
;; │             │
;; │ App starter │
;; │             │
;; └─────────────┘

(def startup (atom nil))

(def run-me
  (fn []
    (let [relative (atom nil)]
      (cm/swapp)
      (try
        (println "last pos" [(.x (.getLocationOnScreen (seesaw.core/to-frame @app))) (.y (.getLocationOnScreen (seesaw.core/to-frame @app)))])
        (reset! relative [(.x (.getLocationOnScreen (seesaw.core/to-frame @app))) (.y (.getLocationOnScreen (seesaw.core/to-frame @app)))])
        (.dispose (seesaw.core/to-frame @app))
        (catch Exception e (println "Last pos is nil")))
      (gseed/build :items (let [img-scale 35]
                            (list
                             (jarmanapp :margin-left img-scale)
                             (slider-ico-btn (stool/image-scale icon/scheme-grey-64-png img-scale) 0 img-scale "DB Visualiser" {:onclick (fn [e] (@gseed/jarman-views-service :set-view :view-id "DB Visualiser" :title "DB Visualiser" :component-fn create-view--db-view))})
                             (slider-ico-btn (stool/image-scale icon/I-64-png img-scale) 1 img-scale "Message Store" {:onclick (fn [e] (@gseed/alert-manager :show))})
                             (slider-ico-btn (stool/image-scale icon/key-blue-64-png img-scale) 2 img-scale "Change work mode" {:onclick (fn [e]
                                                                                                                                           (cond (= "user"      (session/user-get-permission)) (session/user-set-permission "admin")
                                                                                                                                                 (= "admin"     (session/user-get-permission)) (session/user-set-permission "developer")
                                                                                                                                                 (= "developer" (session/user-get-permission)) (session/user-set-permission "user"))
                                                                                                                                           (@gseed/alert-manager :set {:header "Work mode" :body (str "Switched to: " (session/user-get-permission))} (@gseed/alert-manager :message gseed/alert-manager) 5)
                                                                                                                                           (gseed/extend-frame-title (str ", " (session/user-get-login) "@" (session/user-get-permission))))})
                             (slider-ico-btn (stool/image-scale icon/pen-64-png img-scale) 3 img-scale "Docs Templates" {:onclick (fn [e] (@gseed/jarman-views-service :set-view :view-id :docstemplates :title "Docs Templates" :scrollable? false :component-fn (fn [] (docs/auto-builder--table-view nil :alerts gseed/alert-manager))))})
                             (slider-ico-btn (stool/image-scale icon/refresh-blue1-64-png img-scale) 4 img-scale "Reload active view" {:onclick (fn [e] (try
                                                                                                                                                          ((@gseed/jarman-views-service :reload))
                                                                                                                                                          (catch Exception e (str "Can not reload. Storage is empty."))))})
                             (slider-ico-btn (stool/image-scale icon/refresh-blue-64-png img-scale) 5 img-scale "Restart" {:onclick (fn [e] (@startup))})

                             @atom-popup-hook)))
      (reset! popup-menager (create-popup-service atom-popup-hook))
      (if-not (nil? @relative) (.setLocation (to-frame @app) (first @relative) (second @relative))))
    (gseed/extend-frame-title (str ", " (session/user-get-login) "@" (session/user-get-permission)))))

;; (@startup)

(reset! startup
        (fn []
          (cond (= (iinit/validate-configuration-files) true)
                (run-me)
                :else (cond (= (iinit/restore-backup-configuration) false)
                            (do
                              (reset! popup-menager (create-popup-service atom-popup-hook))
                              (@popup-menager :ok :title "App start failed" :body "Cennot end restore task." :size [300 100]))
                            :else (do
                                    (= (iinit/validate-configuration-files) true)
                                    (run-me)
                                    :else (do
                                            (reset! popup-menager (create-popup-service atom-popup-hook))
                                            (@popup-menager :ok :title "App start failed" :body "Restor failed. Some files are missing." :size [300 100])))))))


;;(@startup)



;; (@gseed/alert-manager :set {:header "Hello World" :body "Some body once told me..."} (@gseed/alert-manager :message gseed/alert-manager) 5)

;; (mmeta/getset)
;; (@gseed/jarman-views-service :get-all-view)

;; (config! (to-frame @app) :size [1000 :by 800])


;; ┌──────────────────────────────────────┐
;; │                                      │
;; │ Example of use for simple components │
;; │                                      │
;; └──────────────────────────────────────┘

;; (def example-text-password-button
;;   "Description
;;       Create simple jframe with simple components
;;    "
;;   (fn []
;;     (build
;;      :undecorated? false
;;      :size [400 400]
;;      :items (list
;;              (text
;;               :class :input
;;               :text "Just text input component"
;;               :halign :center
;;               :bounds [100 50 200 30])
;;              (jarman.gui.gui-tools/text-input :placeholder "Login"
;;                                                   :style [:class :input
;;                                                           :halign :center
;;                                                           :bounds [100 100 200 30]])
;;              (jarman.gui.gui-tools/password-input :placeholder "Password component"
;;                                                   :style [:class :input
;;                                                           :halign :center
;;                                                           :bounds [100 150 200 30]])
;;              (jarman.gui.gui-tools/simple-button "Simple button" (fn [e] (println (map #(str "Value: " (if (map? (get-user-data %)) ;; if component have map inside :user-data
;;                                                                                                          (if (= (get-user-data % :type) :password) ;; if component type inside :user-data is password
;;                                                                                                            (get-user-data % :value) ;; return value from :user-data
;;                                                                                                            (value %)) ;; else return value from component
;;                                                                                                          (value %)))  ;; return value from component
;;                                                                                        (select (to-root e) [:.input]) ;; Get all component with class "input"
;;                                                                                        )))
;;                                                  :style [:bounds [100 200 200 30]])))))

;; (example-text-password-button)


