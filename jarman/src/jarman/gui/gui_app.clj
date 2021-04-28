;; 
;; Compilation: dev_tool.clj -> metadata.clj -> gui_tools.clj -> gui_alerts_service.clj -> gui_app.clj
;; 
(ns jarman.gui.gui-app
  (:use seesaw.core
        seesaw.border
        seesaw.dev
        seesaw.mig
        seesaw.swingx)
  (:import (javax.swing JLayeredPane JLabel JTable JComboBox DefaultCellEditor JCheckBox)
           (javax.swing.table TableCellRenderer TableColumn)
           (java.awt.event MouseEvent)
           (java.awt Color Component))
  (:require [clojure.string :as string]
            ;; resource 
            [jarman.resource-lib.icon-library :as icon]
            ;; logics
            [jarman.config.config-manager :as c]
            [jarman.gui.gui-tools :refer :all :as gtool]
            [jarman.gui.gui-components :refer :all]
            [jarman.gui.gui-alerts-service :refer :all]
            [jarman.gui.gui-views-service :refer :all]
            ;; deverloper tools 
            [jarman.tools.swing :as stool]
            [jarman.config.spec :as sspec]
            [jarman.config.init :as iinit]
            [jarman.logic.metadata :as mmeta]
            [jarman.tools.lang :refer :all :as lang]
            [jarman.gui.gui-seed :refer :all]
            [jarman.gui.gui-config-generator :refer :all :as cg]
            ;; [jarman.logic.view :refer :all] 
            ;; TEMPORARY!!!! MUST BE REPLACED BY CONFIG_MANAGER

            ))


(def jarman-views-service (atom nil))


;; ┌────────────────────┐
;; │                    │
;; │ Changes controller │
;; │                    │
;; └────────────────────┘

(def work-mode (atom :dev-mode))
(def storage-with-changes (atom {})) ;; Store view id as key and component id which is path in config map too {:view link-to-atom}

(def add-changes-controller
  "Description:
       Add changes controller for views. Storage only stores atoms from bigest view component.
   Example:
       (add-changes-controller view-id changing-list) where changing-list is an atom inside view/biggest component.
   "
  (fn [view-id changing-list]
    (swap! storage-with-changes (fn [changes-atoms] (merge changes-atoms {view-id changing-list})))))

(def get-changes-atom
  "Description:
       Function return atom address to list of changes for view by view-id.
   Example:
       (get-changes-atom :init.edn) => {:init.edn-value-lang [[:init.edn :value :lang] EN]}
   "
  (fn [view-id]
    (get-in @storage-with-changes [view-id])))

(def set-change-to-view-atom
  (fn [changing-list path new-value]
    (swap! changing-list (fn [changes] (merge changes {(convert-mappath-to-key path) [path new-value]})))))

(def remove-change-from-view-atom
  (fn [changing-list path]
    (swap! changing-list (fn [changes] (dissoc changes (convert-mappath-to-key path))))))

(def track-changes-used-components
  (fn [changing-list value-path event component-key value]
    (cond
      ;; if something was change
      (not (= (config event component-key) value)) (set-change-to-view-atom changing-list value-path (config event component-key))
      ;; if back to orginal value
      (not (nil? (get-in @changing-list [(convert-mappath-to-key value-path)]))) (remove-change-from-view-atom changing-list value-path))))

(def track-changes
  (fn [changing-list value-path orginal new-value]
    (cond
      ;; if something was change
      (not (= orginal new-value)) (set-change-to-view-atom changing-list value-path new-value)
      ;; if back to orginal value
      (not (nil? (get-in @changing-list [(convert-mappath-to-key value-path)]))) (remove-change-from-view-atom changing-list value-path))))




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



;; (def dbmap (atom (list
;;                   {:id 29
;;                    :table "service_contract"
;;                    :prop
;;                    {:table
;;                     {:frontend-name "service_contract"
;;                      :is-system? false
;;                      :is-linker? false
;;                      :allow-modifing? true
;;                      :allow-deleting? true
;;                      :allow-linking? true}
;;                     :columns
;;                     [{:field "id_point_of_sale"
;;                       :representation "id_point_of_sale"
;;                       :description nil
;;                       :component-type "l"
;;                       :column-type "bigint(20) unsigned"
;;                       :private? false
;;                       :editable? true
;;                       :key-table "point_of_sale"}
;;                      {:field "name_of_sale"
;;                       :representation "Miejsce sprzedaży"
;;                       :description "Opisuje miejsce sprzedaży"
;;                       :component-type "l"
;;                       :column-type "bigint(20) unsigned"
;;                       :private? false
;;                       :editable? true}
;;                      {:field "some_poop"
;;                       :representation "Nazwa nazw"
;;                       :description "Opisuje nazw"
;;                       :component-type "1"
;;                       :column-type "bigint(20) unsigned"
;;                       :private? false
;;                       :editable? true}]}}
;;                   {:id 30
;;                    :table "user"
;;                    :prop
;;                    {:table
;;                     {:frontend-name "user"
;;                      :is-system? false
;;                      :is-linker? false
;;                      :allow-modifing? true
;;                      :allow-deleting? true
;;                      :allow-linking? true}
;;                     :columns
;;                     [{:field "login"
;;                       :representation "login"
;;                       :description nil
;;                       :component-type "i"
;;                       :column-type "varchar(100)"
;;                       :private? false
;;                       :editable? true}]}})))


;; (map (fn [tab] (conj {:bounds [0 0 0 0]} tab)) dbmap)
;; (prepare-table 10 10 "Users" "FName" "LName" "LOGIN")
;; (getset)
(def dbmap (atom (mmeta/getset)))



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
  [dbmap offset max-tabs-inline]
  (let [sizes (partition-all max-tabs-inline (calculate-tables-size @dbmap))
        tables (calculate-tables-size-with-tabs @dbmap)
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
  [name] (label :text name :font (getFont 14 :bold) :border (line-border :bottom 2 :color (get-color :background :header))))

(defn table-editor--component--column-picker-btn
  "Description:
     Select button who table-editor--component--column-picker, can work with another same buttons"
  [txt func] (let [color (get-color :group-buttons :background)
                   color-hover (get-color :group-buttons :background-hover)
                   color-clicked (get-color :group-buttons :clicked)
                   bg-btn (atom color)
                   id-btn :table-editor--component--column-picker-btn]
               (mig-panel
                :constraints ["" "15px[100, fill]15px" "10px[fill]10px"]
                :border (line-border :left 0 :right 0 :top 1 :bottom 1 :color (get-color :border :gray))
                :id id-btn
                :background color
                :user-data bg-btn
                :listen [:mouse-entered (fn [e] (config! e :cursor :hand :background color-hover))
                         :mouse-exited  (fn [e] (config! e :background @bg-btn))
                         :mouse-clicked  (fn [e] (cond
                                                   (= @bg-btn color)
                                                   (do
                                                ;; reset bg and atom inside all buttons in parent if id is ok
                                                     (doall (map (fn [b] (cond (= (config b :id) id-btn)
                                                                               (do (config! b :background color)
                                                                                   (reset! (config b :user-data) color))))
                                                                 (seesaw.util/children (.getParent (seesaw.core/to-widget e)))))
                                                ;; reset atom with color
                                                     (reset! bg-btn color-clicked)
                                                ;; update atom with color
                                                     (config! e :background @bg-btn)
                                                     (func e))))]
                :items [[(label :text txt)]])))

(def table-editor--element--header-view
  (fn [value]
    (text :text value
          :font (getFont 14)
          :background (get-color :background :input)
          :editable? false
          :border (compound-border (empty-border :left 10 :right 10 :top 5 :bottom 5)
                                   (line-border :bottom 2 :color (get-color :decorate :gray-underline))))))

(def table-editor--element--small-input
  (fn [enable changing-list value-path value]
    (text :text value :font (getFont 12)
          :background (get-color :background :input)
          :enabled? enable
          :border (compound-border (empty-border :left 10 :right 10 :top 5 :bottom 5)
                                   (line-border :bottom 2 :color (get-color :decorate :gray-underline)))
          :listen [:caret-update (fn [event] (track-changes-used-components changing-list value-path event :text value))])))


(def table-editor--element--input
  (fn [enable changing-list value-path value]
    (text :text value :font (getFont 12)
          :background (get-color :background :input)
          :enabled? enable
          :border (compound-border (empty-border :left 15 :right 20 :top 8 :bottom 8)
                                   (line-border :bottom 2 :color (get-color :decorate :gray-underline)))
          :listen [:caret-update (fn [e] (track-changes changing-list value-path value (config e :text)))])))



(defn table-editor--element--btn-add-col-prop
  []
  (mig-panel ;; Button for add parameter to column 
   :constraints ["" "0px[grow, center]0px" "5px[fill]5px"]
   :items [[(label :icon (stool/image-scale icon/plus-128-png 15)
                   :listen [:mouse-clicked (fn [e] (println "Add parameter"))
                            :mouse-entered (fn [e] (config! e :cursor :hand))])]]
   :border (compound-border
            (line-border :top 2 :color (get-color :decorate :gray-underline))
            (empty-border :left 15 :right 15 :top 5 :bottom 8))))

(def switch-column-to-editing
  (fn [mode changing-list value-path event column column-editor-id]
    (config! (select (to-root event) [(convert-str-to-hashkey column-editor-id)])
             ;; Right space for column parameters
             :items [[(mig-panel ;; Editable parameters list
                       :constraints ["wrap 2" "20px[100, fill]10px[150:,fill]5px" "5px[fill]5px"]
                       :items (join-mig-items
                               (map
                                (fn [column-parameter]
                                  (let [key-param (first column-parameter)]
                                    (list
                                     (label :text (str key-param)) ;; Parameter name
                                     (table-editor--element--input (= mode :dev-mode) changing-list (join-vec value-path [key-param]) (str (second column-parameter)))))) ;; Parameter value
                                column)))]])))


(def table-editor--element--btn-add-column
  (fn [] (mig-panel ;; Button for add column to table
          :constraints ["" "0px[grow, center]0px" "5px[fill]5px"]
          :items [[(label :icon (stool/image-scale icon/plus-128-png 15))]]
          :border (compound-border (empty-border :left 15 :right 15 :top 5 :bottom 8))
          :listen [:mouse-clicked (fn [e] (println "Add column"))
                   :mouse-entered (fn [e] (config! e :cursor :hand))])))

(defn table-editor--component--column-picker
  "Create left table editor view to select column which will be editing on right table editor view"
  [mode changing-list column-editor-id columns value-path]
  (mig-panel :constraints ["wrap 1" "0px[100:,fill]0px" "0px[fill]0px"]
             :items
             (join-mig-items
              (map (fn [column index]
                     (let [value-path (join-vec value-path [index])]
                       (table-editor--component--column-picker-btn
                        (get-in column [:representation])
                        (fn [event] (switch-column-to-editing mode changing-list value-path event column column-editor-id)))))
                   columns (range (count columns)))
              (table-editor--element--btn-add-column))))

(defn table-editor--component--space-for-column-editor
  "Create right table editor view to editing selected column in left table editor view"
  [column-editor-id] (mig-panel :constraints ["wrap 1" "0px[fill]0px" "0px[fill]0px"]
                                :id (keyword column-editor-id)
                                :items [[(label)]]
                                :border (line-border :left 4 :color (get-color :border :dark-gray))))

(def get-table-configuration-from-list-by-table-id
  (fn [atom--tables-configurations table-id]
    (let [map (first (filter (fn [item] (= table-id (get item :id))) @atom--tables-configurations))]
      (do ;;(println map)
        map))))

(def meta-copy (atom {}))
(defn table-editor--element--btn-save
  [changing-list table-id]
  (table-editor--component--bar-btn :edit-view-save-btn (get-lang-btns :save) icon/agree-grey-64-png icon/agree-blue-64-png
                                    (fn [e] ;; TODO
                                      ;; save meta configuration chenages
                                      ;; (prn "Changes" @changing-list)
                                      (let [orginal-table (get-table-configuration-from-list-by-table-id dbmap table-id)]
                                        (reset! meta-copy orginal-table)
                                        (doall
                                         (map
                                          (fn [new-value] ;; (println (first (second new-value)) (second (second new-value)))
                                            (prn "Changes" (first (second new-value)) (second (second new-value)))
                                            (swap! meta-copy (fn [changes] (assoc-in changes (first (second new-value)) (second (second new-value))))))
                                          @changing-list))
                                        (let [out (mmeta/validate-all @meta-copy)]
                                          (cond (= (get-in out [:valid?]) true)
                                                (doall
                                                 (mmeta/do-change
                                                  (mmeta/apply-table orginal-table @meta-copy)
                                                  orginal-table @meta-copy)
                                                 (reset! dbmap (mmeta/getset))
                                                 (@alert-manager :set {:header "Success!" :body "Changes were saved successfully!"} (message alert-manager) 5))
                                                :else (@popup-menager :ok :title "Valid faild!" :body (string/join "<br>" ["Validation ended with faild." (get-in out [:output])]))))))))

;; @dbmap
;; @meta-copy
;; @storage-with-changes
;; @meta-copy
;; (mmeta/validate-all {:id 23
;;   :table "permission"
;;   :prop
;;   {:table
;;    {:frontend-name "permission"
;;     :is-system? false
;;     :is-linker? false
;;     :allow-modifing? true
;;     :allow-deleting? true
;;     :allow-linking? true}
;;    :columns
;;    [{:field "permission_name"
;;      :representation "123"
;;      :description nil
;;      :component-type "i"
;;      :column-type "varchar(20)"
;;      :private? 123
;;      :editable? true}
;;     {:field "configuration"
;;      :representation "-=0=-"
;;      :description "-908-98-098"
;;      :component-type "a"
;;      :column-type "tinytext"
;;      :private? false
;;      :editable? true}]}})


(defn table-editor--element--btn-show-changes
  [changing-list] (table-editor--component--bar-btn :edit-view-back-btn (get-lang-btns :show-changes) icon/refresh-grey-64-png icon/refresh-blue-64-png
                                                    (fn [e] (@popup-menager :new-message :title "Changes list" :body (textarea (str @changing-list)) :size [400 300]))))



(def table-editor--element--checkbox
  (fn [enable changing-list value-path value]
    (checkbox :selected? value
              :enabled? enable
              :listen [:state-changed (fn [event] (track-changes-used-components changing-list value-path event :selected? value))])))


;; (show-events (checkbox))


(def table-editor--element--table-parameter-name
  (fn [table-property index] (label :text (str (first (nth (vec table-property) index))))))

(def table-editor--element--table-parameter-value
  (fn [mode changing-list table-property tab-value-path index txtsize]
    (let [param-name  (first  (nth (vec table-property) index))
          param-value (second (nth (vec table-property) index))
          value-path (join-vec tab-value-path [(keyword param-name)])]
      (cond
        (string?  param-value) (table-editor--element--small-input (= mode :dev-mode) changing-list value-path (str param-value))
        (boolean? param-value) (table-editor--element--checkbox    (= mode :dev-mode) changing-list value-path param-value)
        :else (label :size txtsize :text (str param-value))))))

(defn create-view--table-editor
  "Description:
     Create view for table editor. Marge all component to one big view.
   "
  [mode atom--tables-configurations table-id]
  (let [changing-list (atom {})
        table          (get-table-configuration-from-list-by-table-id atom--tables-configurations table-id)
        col-value-path [:prop :columns]
        tab-value-path [:prop :table]
        columns        (get-in table col-value-path) ;; Get columns list
        table-property (get-in table tab-value-path) ;; Get table property
        view-id        (keyword (get table :table))  ;; Get name of table and create keyword to check tabs bar (opens views)
        elems          (join-vec
                    ;; Table info
                        [[(mig-panel :constraints ["" "0px[grow, fill]5px[]0px" "0px[fill]0px"] ;; menu bar for editor
                                     :items (join-mig-items
                                             [(table-editor--element--header-view (string/join "" [">_ Edit table: " (get-in table [:prop :table :frontend-name])]))]
                                             (cond (= mode :dev-mode)
                                                   (list [(table-editor--element--btn-save changing-list table-id)]
                                                         [(table-editor--element--btn-show-changes changing-list)])
                                                   :else [])))]]
                    ;; Table properties 
                        [[(table-editor--element--header "Table configuration")]]
                        [(vec (let [table-property-count (count table-property)
                                    txtsize [150 :by 25]]
                                [(mig-panel
                                  :constraints ["wrap 3" "0px[32%, fill]0px" "0px[fill]0px"]
                                  :items (vec (for [index (range table-property-count)]
                                                [(mig-panel
                                                  :border (line-border :left 4 :color "#ccc")
                                                  :constraints ["" "10px[100:]0px[grow, fill]10px" "0px[fill]10px"]
                                                  :items [[(table-editor--element--table-parameter-name  table-property index)]
                                                          [(table-editor--element--table-parameter-value mode changing-list table-property tab-value-path index txtsize)]])])))]))]
                    ;; Columns properties
                        [[(table-editor--element--header "Column configuration")]]
                        [[(let [column-editor-id "table-editor--component--space-for-column-editor"]
                            (mig-panel ;; Left and Right functional space
                             :constraints ["wrap 2" "0px[fill]0px" "0px[fill]0px"]
                             :items [[(table-editor--component--column-picker mode changing-list column-editor-id columns col-value-path)] ;; Left part for columns to choose for doing changes.
                                     [(table-editor--component--space-for-column-editor column-editor-id)] ;; Space for components. Using to editing columns.
                                     ]))]])
        component (cond
                    (> (count table) 0) (do
                                          (mig-panel
                                           :constraints ["wrap 1" "20px[grow, fill]20px" "20px[]20px"]
                                           :items elems
                                           :border (empty-border :thickness 0)))
                    :else (label :text "Table not found inside metadata :c"))]
    (do
      (add-changes-controller view-id changing-list)
      (@jarman-views-service :set-view :view-id view-id :title (get table :table) :component component))))


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
       :bounds [x y 150 120]
       :background (new Color 0 0 0 0)
       :border (line-border :thickness 1 :color border-c)
       :constraints ["wrap 1" "0px[150, fill]0px" "0px[30px, fill]0px"]
       :items [[(btn "Edit table" icon/pen-blue-64-png (fn [e] (do (rm-menu e)
                                                                   (create-view--table-editor @work-mode atom--tables-configurations table-id))))]
               [(btn "Delete table" icon/basket-blue1-64-png (fn [e]))]
               [(btn "Show relations" icon/refresh-connection-blue-64-png (fn [e]))]]))))

(defn table-visualizer--element--col-as-row
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
                                            (let [table-id (get (config (getParent e) :user-data) :id)]
                                              (cond (= (.getButton e) MouseEvent/BUTTON3)
                                                    (let [scrol (select (@jarman-views-service :get-component :view-id :Database) [:#JLP-DB-Visualizer])
                                                          JLP (first (seesaw.util/children (first (seesaw.util/children scrol))))]
                                                      (.add JLP
                                                            (db-view--apsolute-pop--rmb-table-actions ;; Open popup menu for table
                                                             JLP
                                                             dbmap ;; forward list of table configuration
                                                             table-id ;; Get table id
                                                             (- (+ (.getX e) (.getX (config (getParent e) :bounds))) 15) ;; calculate popup position
                                                             (- (+ (+ (.getY e) (.getY (config e :bounds))) (.getY (config (getParent e) :bounds))) 10))
                                                            (new Integer 999) ;; z-index
                                                            ))
                                                    (= (.getClickCount e) 2) ;; Open table editor by duble click
                                                    (create-view--table-editor @work-mode dbmap table-id))))
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


(defn db-viewer--component--table
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
        y (+ 0 (nth bounds 1))
        w (nth bounds 2)
        row-h 30  ;; wysokosc wiersza w tabeli reprezentujacego kolumne
        col-in-rows (map (fn [col] (table-visualizer--element--col-as-row {:name (get col :field) :width w :height row-h :type (if (contains? col :key-table) "key" "row") :border-c border-c})) (get-in data [:prop :columns]))  ;; przygotowanie tabeli bez naglowka
        camplete-table (conj col-in-rows (table-visualizer--element--col-as-row {:name (get data :table) :width w :height row-h :type "header" :border-c border-c}))  ;; dodanie naglowka i finalizacja widoku tabeli
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


(defn db-viewer--component--menu-bar
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
                                       :border (compound-border (empty-border :left 15 :right 15 :top 5 :bottom 5) (line-border :thickness 1 :color border-c))
                                       :listen [:mouse-entered (fn [e] (config! e :background "#d9ecff" :foreground "#000" :cursor :hand))
                                                :mouse-exited  (fn [e] (config! e :background "#fff" :foreground "#000"))])))]
       (mig-panel
        :id :db-viewer--component--menu-bar
        :background (new Color 0 0 0 0)
        :constraints ["" "5px[fill]0px" "5px[fill]5px"]
        :items [[(btn "Show all relation" icon/refresh-connection-blue-64-png)]
                [(btn "Save view" icon/agree-grey-64-png true)]
                [(btn "Reset view" icon/arrow-blue-left-64-png)]
                [(btn "Reloade view" icon/refresh-blue-64-png)]])))



(def create-view--db-view
  "Description:
     Create component and set to @views atom to use in functional space. 
     Add open tab for db-view to open tabs bar.
     Set prepare view from @views to functional space.
   "
  (let [JLP (to-widget (new JLayeredPane))]
    (doall (map (fn [tab-data]
                  (.add JLP (db-viewer--component--table (get-in tab-data [:prop :bounds] [0 0 100 100]) tab-data) (new Integer 5)))
                (calculate-bounds dbmap 20 5)))
    (doseq [i [(label :text "A" :bounds [50 50 200 200]) (label :text "B" :bounds [100 100 200 200])]]
      (.add JLP i (new Integer 5)))
    (.setMaximumSize JLP (java.awt.Dimension. 300 300))
    (.setSize JLP (java.awt.Dimension. 300 300))
    (mig-panel
     :border nil
     :constraints ["wrap 1" "0px[grow, fill]0px" "5px[fill]5px[grow, fill]0px"]
     :items [[(db-viewer--component--menu-bar)]
             [(scrollable JLP :id :JLP-DB-Visualizer :border nil)]])))


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
          (let [config-file-list-as-keyword (map #(first %) (c/get-in-segment []))
                config-file-list-as-keyword-to-display (filter #(let [map-part (c/get-in-segment (if (vector? %) % [%]))]
                                                                  (and (= :file (get map-part :type))
                                                                       (= :edit (get map-part :display))))
                                                               config-file-list-as-keyword)]
            (reverse
             (conj
              (map (fn [p]
                     (let [path (if (vector? p) p [p])
                           title (get (c/get-in-segment path) :name)
                           view-id (last path)]
                       (button-expand-child title :onClick (fn [e]
                                                             (@jarman-views-service
                                                              :set-view
                                                              :view-id view-id
                                                              :title title
                                                              :component (cg/create-view--confgen path))))))
                   config-file-list-as-keyword-to-display)

              (let [path [:themes :theme_config.edn]
                    title (get (c/get-in-segment path) :name)
                    view-id :theme_config.edn]
                (button-expand-child title :onClick (fn [e]
                                                      (@jarman-views-service
                                                       :set-view
                                                       :view-id view-id
                                                       :title title
                                                       :component (cg/create-view--confgen path)))))
              (let [path [:themes :current-theme]
                    title (get (c/get-in-segment path) :name)
                    view-id :current-theme]
                (button-expand-child title :onClick (fn [e]
                                                      (@jarman-views-service
                                                       :set-view
                                                       :view-id view-id
                                                       :title title
                                                       :component (cg/create-view--confgen path)))))))))))


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
  (fn [& args] (mig-panel
                :id :expand-menu-space
                :background "#fff"
                :border (line-border :left 4 :right 4 :color "#fff")
                :constraints ["wrap 1" "0px[fill, grow]0px" "0px[]0px"]
                :items (vec args))))

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
          tabs-space (horizontal-panel
                      :id :app-tabs-space
                      :background bg-color
                      :items tabs)
          views-space (vertical-panel
                       :id :app-functional-space
                       :background (new Color 0 0 0 0)
                       :border (empty-border :right 1)
                       :items array)]
      (reset! jarman-views-service (new-views-service tabs-space views-space))
      (mig-panel
       :id :operation-space
       :background "#fff"
       :constraints ["wrap 1" "0px[fill, grow]0px" "0px[25]0px[fill, grow]0px"]
       :background "#eee"
       :border (line-border :left 1 :color "#999")
       :items [[tabs-space]
               [views-space]]))))



(def jarmanapp
  "Description:
      Main space for app inside JLayeredPane. There is menu with expand btns and space for tables with tab btns.
   "
  (fn [& {:keys [margin-left]
          :or {margin-left 0}}]
    (let [bg-color "#ddd"
          vhr-color "#999"]
      (grid-panel
       :id :rebound-layer
       :items [(mig-panel
                :constraints [""
                              "0px[200]0px[grow, fill]15px"
                              "0px[grow, fill]38px"]
                :border (line-border :left margin-left :color bg-color)
                :items [;; [(label-fn :background "#eee" :size [50 :by 50])]
                        [(mig-app-left-f  [(button-expand "Alerty"
                                                          [(button-expand-child "Alert 1 \"Test\""  :onClick (fn [e] (@alert-manager :set {:header "Test" :body "Bardzo dluga testowa wiadomość, która nie jest taka prosta do ogarnięcia w seesaw."} (message alert-manager) 3)))
                                                           (button-expand-child "Alert 2 \"Witaj\"" :onClick (fn [e] (@alert-manager :set {:header "Witaj" :body "Świecie"} (message alert-manager) 5)))])]
                                          [(button-expand "Widoki"
                                                          [(button-expand-child "Test 1"    :onClick (fn [e] (@jarman-views-service :set-view :view-id "test1" :title "Test 1" :component (label :text "Test 1"))))
                                                           (button-expand-child "Test 2"    :onClick (fn [e] (@jarman-views-service :set-view :view-id "test2" :title "Test 2" :component (label :text "Test 2"))))
                                                           (button-expand-child "Test 3"    :onClick (fn [e] (@jarman-views-service :set-view :view-id "test3" :title "Test 3" :component (vertical-panel :items [(label :text "Test 3")]))))
                                                           (button-expand-child "DB View" :onClick (fn [e] (@jarman-views-service :set-view :view-id "Database" :title "Database" :component create-view--db-view)))
                                                          ;;  (button-expand-child "Users table" :onClick (fn [e] (@jarman-views-service :set-view :view-id "tab-user" :title "User" :scrollable? false :component (jarman.logic.view/auto-builder--table-view nil))))
                                                           ])]
                                          [(create-expand-btns--confgen)])]
                        [(right-part-of-jarman-as-space-for-views-service []
                                                                          [])]])]))))




;; ┌─────────────┐
;; │             │
;; │ App starter │
;; │             │
;; └─────────────┘

(def startup (atom nil))

(def run
  (fn []
    (do
      (c/swapp)
      (try (.dispose (seesaw.core/to-frame @app)) (catch Exception e (println "Exception: " (.getMessage e))))
      (build :items (let [img-scale 40]
                      (list
                       (jarmanapp :margin-left img-scale)
                       (slider-ico-btn (stool/image-scale icon/scheme-grey-64-png img-scale) 0 img-scale "DB View" {:onclick (fn [e] (@jarman-views-service :set-view :view-id "Database" :title "Database" :component create-view--db-view))})
                       (slider-ico-btn (stool/image-scale icon/I-64-png img-scale) 1 img-scale "Powiadomienia" {:onclick (fn [e] (@alert-manager :show))})
                       (slider-ico-btn (stool/image-scale icon/alert-64-png img-scale) 2 img-scale "Popup" {:onclick (fn [e] (@popup-menager :new-message :title "Hello popup panel" :body (label "Hello popup!") :size [400 200]))})
                       (slider-ico-btn (stool/image-scale icon/agree-grey-64-png img-scale) 3 img-scale "Dialog" {:onclick (fn [e] (println (str "Result = " (@popup-menager :yesno :title "Ask dialog" :body "Do you wona some QUASĄĄĄĄ?" :size [300 100]))))})
                       (slider-ico-btn (stool/image-scale icon/a-blue-64-png img-scale) 4 img-scale "alert" {:onclick (fn [e] (@alert-manager :set {:header "Witaj<br>World" :body "Alllle<br>Luja"} (message alert-manager) 5))})
                       (slider-ico-btn (stool/image-scale icon/refresh-blue-64-png img-scale) 5 img-scale "Restart" {:onclick (fn [e] (@startup))})
                       (slider-ico-btn (stool/image-scale icon/key-blue-64-png img-scale) 6 img-scale "Change work mode" {:onclick (fn [e]
                                                                                                                                     (cond (= @work-mode :user-mode)
                                                                                                                                           (do
                                                                                                                                             (reset! work-mode :dev-mode)
                                                                                                                                             (@alert-manager :set {:header "Work mode" :body "Dev mode activated."} (message alert-manager) 5))
                                                                                                                                           :else (do
                                                                                                                                                   (reset! work-mode :user-mode)
                                                                                                                                                   (@alert-manager :set {:header "Work mode" :body "Dev mode deactivated."} (message alert-manager) 5))))})
                      ;;  (slider-ico-btn (stool/image-scale icon/pen-64-png img-scale) 7 img-scale "Table Auto Generator" {:onclick (fn [e] (@jarman-views-service :set-view :view-id "tab-user" :title "User" :scrollable? false :component (jarman.logic.view/auto-builder--table-view nil)))})
                       @atom-popup-hook)))
      (reset! popup-menager (create-popup-service atom-popup-hook)))))


(reset! startup
        (fn []
          (cond (= (iinit/validate-configuration-files) true)
                (run)
                :else (cond (= (iinit/restore-backup-configuration) false)
                            (do
                              (reset! popup-menager (create-popup-service atom-popup-hook))
                              (@popup-menager :ok :title "App start failed" :body "Cennot end restore task." :size [300 100]))
                            :else (do
                                    (= (iinit/validate-configuration-files) true)
                                    (run)
                                    :else (do
                                            (reset! popup-menager (create-popup-service atom-popup-hook))
                                            (@popup-menager :ok :title "App start failed" :body "Restor failed. Some files are missing." :size [300 100])))))))

(@startup)

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


