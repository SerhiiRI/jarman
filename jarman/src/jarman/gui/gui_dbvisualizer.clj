(ns jarman.gui.gui-dbvisualizer
  (:use seesaw.dev
        seesaw.style
        seesaw.mig
        seesaw.font)
  (:import (javax.swing JLayeredPane)
           (jarman.jarmanjcomp DateTime)
           (java.awt.event MouseEvent)
           (jiconfont.icons.google_material_design_icons GoogleMaterialDesignIcons))  
  (:require [seesaw.core :as c]
            [seesaw.border :as b]
            [clojure.string :as string]
            ;; resource 
            [jarman.resource-lib.icon-library :as icon]
            [jarman.gui.gui-style             :as gs]
            [jarman.faces :as face]

            ;; logics
            [seesaw.util :as u]
            [jarman.gui.gui-tools :as gtool]
            [jarman.gui.gui-components :as gcomp]
            [jarman.gui.gui-style :as gs]
            
            ;; deverloper tools 
            [jarman.tools.swing :as stool]
            [jarman.logic.metadata :as mt]
            [jarman.tools.lang :refer :all]
           
            ;; [jarman.logic.view :as view]
            ;; [jarman.gui.gui-docs :as docs]
            [jarman.logic.state :as state]
            [jarman.plugin.plugin :as plug]
            [jarman.logic.view-manager :as vmg]
            [jarman.logic.session :as session]
            [jarman.interaction :as i]))

(def dark-grey-color "#676d71")
(def blue-color "#256599")
(def row-height-in-table 40)
;; ================================================VVVVVVVVVV Table in database view
;; ┌─────────────────────────────┐
;; │                             │
;; │ Resize components functions │
;; │                             │
;; └─────────────────────────────┘

;; (state/set-state :dbv-bounds {7 [301.0 322.0], 1 [650.0 120.0], 4 [421.0 2.0], 15 [265.0 723.0], 13 [265.0 442.0], 6 [14.0 343.0], 3 [562.0 446.0], 12 [14.0 245.0], 2 [138.0 246.0], 11 [301.0 247.0], 9 [561.0 638.0], 5 [650.0 283.0], 14 [212.0 51.0], 10 [11.0 14.0], 8 [417.0 342.0]})

(defn- calculate-tables-size
  "Description:
     Prepare list with vectors, in vectors as first is vector with width and height of table and metadata segment.
   Example:
     (calculate-tables-size metadata) => ([[100 100] {:metadata :segment}] ...)
  "
  [metadata]
  (let [size-and-meta
        (doall
         (map
          (fn [table]
            (let [columns-count (count (get-in table [:prop :columns]))
                  letters-in-column-title (map
                                           #(count (:representation %))
                                           (get-in table [:prop :columns]))
                  letters-in-table-title (list
                                          (count
                                           (:table table)))
                  size [(* 10 (last
                               (sort
                                (concat
                                 letters-in-column-title
                                 letters-in-table-title))))
                        (* row-height-in-table (+ 1 columns-count))]]
              ;;(println "\nSize " size)
              [size table]))
          metadata))]
  ;; (println "\nSAM" size-and-meta)
    size-and-meta))


(defn- calculate-bounds-and-tables
  "Description:
     Calculate where on JLayeredPane should be rendered visualized table.
  "
  [meta offset max-tabs-inline]
  (let [size-and-meta-in-rows (partition-all max-tabs-inline (calculate-tables-size meta)) 
        tables-sizes (doall (map #(map (fn [x] (first x)) %) size-and-meta-in-rows))
        tables-heights (into
                        (doall (map #(map (fn [size] (second size)) %) tables-sizes))
                        (list (repeat max-tabs-inline 0)))
        y-bound (atom 10)
        x-bound (atom 10)
        
        pre-bounds (doall
                    (map
                     (fn [row]
                       (reset! x-bound 10)
                       (let [bounds-list-x (doall
                                            (map
                                             (fn [size]
                                               (let [current-x-bound @x-bound
                                                     table-width (first (first size))]
                                                 (swap! x-bound #(+ % offset table-width))
                                                 ;;(println "\nCXB" current-x-bound)
                                                 current-x-bound))
                                             row))]
                         bounds-list-x))
                     size-and-meta-in-rows))
        
        calculate-xy (doall ;; => ([x y] ...)
                      (map
                       (fn [bx by]
                         (map
                          (fn [x y]
                            (vec (list x y)))
                          bx by))
                       pre-bounds tables-heights))
        
        bounds-and-meta (doall
                         (map ;; => (((([x y w h] {:meta :data}) ...table) ...row))
                          (fn [row bounds]
                            (map
                             (fn [table xy]
                               (let [bound-xy (rift (get-in (second table) [:prop :table :bounds]) xy)
                                     [width height] (first table)
                                     width-height [width (+ height offset)]
                                     meta-data    (second table)]
                                 ;; (println "\nTID" (second table))
                                 (list
                                  (vec (concat bound-xy width-height))
                                  meta-data)))
                             row bounds))
                          size-and-meta-in-rows calculate-xy))]
    ;;(println "\nBM" bounds-and-meta) 
    bounds-and-meta))
;; (state/state :dbv-bounds)
;; (state/set-state :dbv-bounds {})
;; (calculate-bounds (mt/getset) 10 5)

;; ┌──────────────┐
;; │              │
;; │ Table editor │
;; │              │
;; └──────────────┘  

(defn table-editor--element--header
  "Create header GUI component in editor for separate section"
  [name]
  (c/label
   :text name
   :font (gs/getFont :bold)
   :foreground dark-grey-color
   :border (b/line-border :bottom 2
                          :color "#ccc"))) ;; TODO: to face

(defn table-editor--component--column-picker-btn
  "Description:
     c/select button who table-editor--component--column-picker, can work with another same buttons"
  [txt func]
  (let [color face/c-main-menu-bg
        color-hover   "#91c8ff" ;; TODO: face
        color-clicked "#d9ecff" ;; TODO: face
        bg-btn (atom color)
        id-btn :table-editor--component--column-picker-btn]
    (mig-panel
     :constraints ["" "15px[100:, fill ]15px" "5px[fill]5px"]
     :border (b/line-border :left 0 :right 0 :top 1 :bottom 1 :color "#eee")
     :id id-btn
     :background color
     :user-data bg-btn
     :listen [:mouse-entered (fn [e] (c/config! e :cursor :hand :background color-hover))
              :mouse-exited  (fn [e] (c/config! e :background @bg-btn))
              :mouse-clicked  (fn [e] ;;(println "column picker")
                                (cond
                                  (= @bg-btn color)
                                  (do ;; reset bg and atom inside all buttons in parent if id is ok
                                    (doall (map (fn [b] (cond (= (c/config b :id) id-btn)
                                                              (do (c/config! b :background color)
                                                                  (reset! (c/config b :user-data) color))))
                                                (seesaw.util/children (.getParent (seesaw.core/to-widget e)))))
                                    ;; reset atom with color
                                    (reset! bg-btn color-clicked)
                                    ;; update atom with color
                                    (c/config! e :background @bg-btn)
                                    (func e))))]
     :items [[(c/label :text txt
                       :maximum-size  [100 :by 100]
                       ;; :font (gtool/getFont 12)
                       )]])))

(def table-editor--element--header-view
  (fn [value]
    (c/text :text value
          ;; :font (gtool/getFont 14)
          :background face/c-compos-background
          :editable? false
          :border (b/compound-border (b/empty-border :left 10 :right 10 :top 5 :bottom 5)
                                   (b/line-border :bottom 2 :color face/c-underline)))))

(defn delete-column [column]
  (let [btns (c/config!
              (gcomp/menu-bar
               {:id :db-viewer--component--menu-bar
                :buttons [["Delete"
                           (gs/icon GoogleMaterialDesignIcons/DELETE face/c-icon)
                           ""
                           (fn [e]
                             ;; TODO: Delete column with ask
                             ;; (create-dialog-yesno
                             ;;  "Delete"
                             ;;  (str
                             ;;   "Delete column"
                             ;;   (name (:field column))
                             ;;   "?")
                             ;;  [180 140])
                             )
                           ]]})
              :border (b/empty-border :left -10 :top 15))]
    (c/config! (first (.getComponents btns))
             :border (b/compound-border
                      (b/empty-border :left 5 :right 10 :top 5 :bottom 5)
                      (b/line-border :thickness 1 :color "#bbb")))
    btns))

;; heyyy
(defn switch-column-to-editing
  [work-mode local-changes path-to-value column]
  (let [param-to-edit (fn [param enabled]
                        (cond
                          (in? [true false] (get column param))
                          (do
                            (c/config!
                             (gcomp/input-checkbox
                              :txt (gtool/convert-key-to-title (str param))
                              :local-changes local-changes
                              :store-id (join-vec column [param])
                              :val (get column param)
                              :enabled? enabled)
                             :background face/c-main-menu-bg))
                          :else
                          (c/config! (gcomp/inpose-label (gtool/convert-key-to-title (str param))
                                                       (gcomp/input-text-with-atom
                                                        {:local-changes local-changes
                                                         :store-id (join-vec path-to-value [param])
                                                         :val (str (get column param))
                                                         :enabled? enabled})
                                                       :font-color dark-grey-color
                                                       :vtop 10)
                                   :background face/c-main-menu-bg)))]
    (mig-panel ;; Editable parameters list
     :constraints ["wrap 1" "20px[250:,fill]5px" "0px[fill]0px"]
     :preferred-size [910 :by 360]
     :background face/c-main-menu-bg
     :items (gtool/join-mig-items
             (filter
              #(not (nil? %))
              (cond
                (= work-mode "developer")
                (map
                 (fn [column-parameter]
                   (let [key-param (first column-parameter)
                         path-to-value (join-vec path-to-value [key-param])]
                     (param-to-edit key-param true))) ;; Parameter value
                 column)
                (= work-mode "admin")
                (list
                 (param-to-edit :representation true)
                 (param-to-edit :description true)
                 (param-to-edit :default-value true)
                 (param-to-edit :private? false)
                 (param-to-edit :editable? false)
                 (delete-column column))
                (= work-mode "user")
                (list
                 (param-to-edit :representation false)
                 (param-to-edit :description false)
                 (param-to-edit :default-value false)
                 (param-to-edit :private? false)
                 (param-to-edit :editable? false)
                 (delete-column column))))))))

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
  (let [check (c/checkbox :text name :background face/c-main-menu-bg)
        sel-comp-type (DateTime/getBar (into-array ["date" "time" "date-time"
                                                    "simple-number" "float-number"
                                                    "boolean" "linking-table"
                                                    "big-text" "short-text"]))
        sel-col-type (DateTime/getBar (into-array [" "]))
        model-sel (.getModel sel-col-type)]
    (c/config! check  :listen [:action-performed (fn [e]
                                                 (swap! cmpts-atom assoc (keyword name) (.isSelected check)))])
    (mig-panel
     :constraints ["wrap 1" "0px[grow, left]0px" "0px[]px"]
     ;;:preferred-size [900 :by 400]
     :background face/c-main-menu-bg
     :items [[(mig-panel
               :constraints ["wrap 1" "20px[250:, fill, grow, left]0px" "10px[]10px"]
               :background face/c-main-menu-bg
               :items [[(c/label :text description
                               :foreground dark-grey-color
                             ;;  :font (gtool/getFont 14)
                               :halign :left)]
                       [(cond
                          (= comp-key :check) check
                          (= comp-key :select-comp-type) sel-comp-type
                          (= comp-key :select-col-type) sel-col-type
                          :else
                          (c/config! (gcomp/input-text :placeholder name
                                                     :style [:halign :left])
                                   :listen [:caret-update
                                            (fn [e] (do (swap! cmpts-atom assoc (keyword name) (c/text e))))]))]])]])))

;;  heyyyy
(defn table-editor--element--btn-add-column
  [func]
  (mig-panel ;; Button for add column to table
   :constraints ["wrap 1" "10px[grow, fill, center]0px" "0px[fill]0px"]
   :items [[(c/flow-panel :items (list (c/label :text "add" ;; :font (gtool/getFont 12)
                                                ))
                        :align :left
                        :listen [:mouse-clicked (fn [e] (do (func e)))
                                 :mouse-entered (fn [e] (c/config! e :cursor :hand))])]]))

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
    (c/config! selct-comp
             :listen [:action-performed (fn [e]
                                          (swap! cmpts-atom assoc :component-type [(.getSelectedItem selct-comp)])
                                          (c/config! selct-col :model (get-component-add-column (.getSelectedItem selct-comp)))
                                          ;;(.addItem selct-col "heyy")
                                          )])
    (c/config! selct-col :listen [:action-performed (fn [e]
                                                    (swap! cmpts-atom assoc :column-type (.toString (.getSelectedItem selct-col))))])
    (c/config! main-panel :items [[(c/label :text "Adding column" :font (gs/getFont :bold) :foreground blue-color)]
                                [(gcomp/multi-panel [(c/vertical-panel :items (list inp-name inp-repr inp-descr inp-def))
                                                     (c/vertical-panel :items (list inp-selct inp-db))
                                                     (c/vertical-panel :items (list inp-pr inp-ed))]
                                                    cmpts-atom
                                                    table-name
                                                    "" 0)]])))


;; (@startup)
;;heyyy
(defn table-editor--component--column-picker
  "Create left table editor view to c/select column which will be editing on right table editor view"
  [work-mode local-changes column-editor-id columns path-to-value table-name]
  (mig-panel :constraints ["wrap 1" "0px[100:, fill]0px" "0px[fill]0px"]
             :items
             (gtool/join-mig-items
              (c/label :text "Columns" :border (b/empty-border :top 5 :bottom 5) :foreground blue-color)
              (map (fn [column index]
                     (let [path-to-value (join-vec path-to-value [index])
                           meta-panel (mig-panel
                                       :constraints ["wrap 1" "grow, fill" ""]
                                       :items [[(c/label :text (name (:field column))
                                                       :border (b/empty-border :bottom 5)
                                                       :font (gs/getFont :bold) :foreground blue-color)]
                                               [(c/config! (switch-column-to-editing work-mode local-changes path-to-value column))]])]
                       (table-editor--component--column-picker-btn
                        (get-in column [:representation])
                        (fn [e]
                          (c/config! (c/select (c/to-root (state/state :app)) [(to-hashkey column-editor-id)])
                                   :items (gtool/join-mig-items meta-panel))))))
                   columns (range (count columns)))
              (c/label :text "Actions" :border (b/empty-border :top 5 :bottom 5) :foreground blue-color)
              (table-editor--element--btn-add-column (fn [e]
                                                       (c/config! (c/select (c/to-root (state/state :app)) [(to-hashkey column-editor-id)])
                                                                :items (gtool/join-mig-items (add-column-panel table-name))))))))


(defn table-editor--component--space-for-column-editor
  "Create right table editor view to editing selected column in left table editor view"
  [column-editor-id] (mig-panel :constraints ["wrap 1" "0px[grow, fill]0px" "0px[grow, fill]0px"]
                                :id (keyword column-editor-id) 
                                ))


(def get-table-configuration-from-list-by-table-id
  (fn [meta table-id]
    (let [map (first (filter (fn [item] (= table-id (get item :id))) meta))]
      map)))


(defn table-editor--element--btn-save
  "Description:
     Invoker-id is a parent whos invoke editor. I mean DB Visualizer."
  [local-changes table invoker-id]
  (gtool/table-editor--component--bar-btn
   :edit-view-save-btn
   (gtool/get-lang-btns :save)
   (gs/icon GoogleMaterialDesignIcons/DONE face/c-icon)
   (gs/icon GoogleMaterialDesignIcons/DONE face/c-icon)
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
       (mt/do-change
        (mt/apply-table table @new-table-meta)
        table @new-table-meta)
       (println "reloadinvoker" invoker-id)
       (if-not (nil? invoker-id) (((state/state :jarman-views-service) :reload) invoker-id))
       (((state/state :jarman-views-service) :reload))
       (i/info (gtool/get-lang-alerts :success) (gtool/get-lang-alerts :changes-saved))))))


(defn table-editor--element--btn-show-changes
  [local-changes table]
  (let [w 550
        h 400]
    (gtool/table-editor--component--bar-btn
     :edit-view-back-btn
     (gtool/get-lang-btns :show-changes)
     (gs/icon GoogleMaterialDesignIcons/AUTORENEW face/c-icon)
     (gs/icon GoogleMaterialDesignIcons/AUTORENEW face/c-icon)
     (fn [e] (gcomp/popup-window
              {:window-title (gtool/get-lang-btns :show-changes)
               :size [w h]
               :view (let [bpnl (mig-panel
                                 :constraints ["wrap 1" "0px[grow, fill]0px" "5px[top]0px"]
                                 :background "#fff"
                                 ;;:preferred-size [400 :by 400]
                                 )
                           scr (c/scrollable bpnl
                                             :hscroll :never)
                           path-groups (group-by second (map (fn [x] (conj (subvec (first x) 1) (second x))) @local-changes))]
                       ;; (println @local-changes)
                       ;; (println path-groups)
                       (.add bpnl (gcomp/menu-bar {:buttons [["Close" (gs/icon GoogleMaterialDesignIcons/EXIT_TO_APP face/c-icon) (fn [e] (.dispose (c/to-frame e)))]]}))
                       (doall
                        (map
                         (fn [[group-name paths]]
                           (.add bpnl
                                 (c/label
                                  :foreground gcomp/blue-color
                                  :border (b/empty-border :left 20)
                                  :text (str (name (rift (get-in table [:prop :columns group-name :field]) "")))))
                           (doall (map
                                   (fn [path]
                                     (.add
                                      bpnl
                                      (let [a (c/label
                                               :border (b/empty-border :left 10)
                                               :background "#fff"
                                               ;; :font (gtool/getFont 12)
                                               :text (str
                                                      (name (last (butlast path)))
                                                      ": "))
                                            b (c/label
                                               :background "#ffb8bf"
                                               ;; :font (gtool/getFont 12)
                                               :text (str (get-in table (flatten [:prop (vec (butlast path))])) ""))
                                            c (c/label
                                               :background "#fff"
                                               ;; :font (gtool/getFont 12)
                                               :text " to ")
                                            d (c/label
                                               :background "#d1ffd9"
                                               ;; :font (gtool/getFont 12)
                                               :text (str (last path)))]
                                        (c/horizontal-panel
                                         :background "#fff"
                                         :border (b/line-border :left 30 :color "#fff")
                                         :items (list a b c d))))
                                     )
                                   paths))) path-groups))
                       (.repaint bpnl)
                       (.setUnitIncrement (.getVerticalScrollBar scr) 20)
                       (.setPreferredSize (.getVerticalScrollBar scr) (java.awt.Dimension. 0 0))
                       (.setBorder scr nil)
                       scr)
               })
       ))))

(defn table-editor--element--btn-open-code-editor
  "Description:
    Open code editor and edit metadata for table as text.
  Example:
    (table-editor--element--btn-open-code-editor :user"
  [selected-tab]
  (gtool/table-editor--component--bar-btn
   :open-code-editor-for-table
   "Editor"
   (gs/icon GoogleMaterialDesignIcons/DESCRIPTION face/c-icon)
   (gs/icon GoogleMaterialDesignIcons/DESCRIPTION face/c-icon)
   (fn [e]
     (c/label) ;; TODO
     ;; (gcomp/view-metadata-editor
     ;;  (keyword (:table_name selected-tab)))
     )))


(def create-view--table-editor
  "Description:
     Create view for table editor. Marge all component to one big view.
   "
  (fn [view-id work-mode table-id invoker-id meta]
    (let [meta           (mt/getset)
          local-changes  (atom nil)
          table          (get-table-configuration-from-list-by-table-id meta table-id)
          table-name     (get-in table [:prop :table :representation])
          col-path-to-value [:prop :columns]
          tab-path-to-value [:prop :table]
          columns        (get-in table col-path-to-value) ;; Get columns list
          table-property (get-in table tab-path-to-value) ;; Get table property
          elems          (join-vec
                          ;; Table info
                          [[(mig-panel :constraints ["" "0px[grow, fill]5px[]0px" "10px[fill]10px"] ;; menu bar for editor
                                       :items (gtool/join-mig-items
                                               [(table-editor--element--header-view (str "Edit table: \"" table-name "\""))]
                                               (cond
                                                 (session/allow-permission? [:developer])
                                                 (list [(table-editor--element--btn-save local-changes table invoker-id)]
                                                       [(table-editor--element--btn-show-changes local-changes table)]
                                                       [(table-editor--element--btn-open-code-editor table)])
                                                 (session/allow-permission? [:admin])
                                                 (list [(table-editor--element--btn-save local-changes table invoker-id)]
                                                       [(table-editor--element--btn-show-changes local-changes table)])
                                                 :else [])))]]
                          [[(gcomp/min-scrollbox ;; Scroll section bottom title and button save/reload bar
                             (mig-panel
                              :constraints ["wrap 1" "[grow, fill]" "[fill]"]
                              :items (gtool/join-mig-items ;; Table properties 
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
                                                                                 (c/config! (gcomp/input-checkbox
                                                                                             :txt (gtool/convert-key-to-title (str param))
                                                                                             :local-changes local-changes
                                                                                             :store-id (join-vec tab-path-to-value [param])
                                                                                             :val (get table-property param)
                                                                                             :enabled? enabled)))
                                                                               :else
                                                                               (c/config!
                                                                                (gcomp/inpose-label
                                                                                 (gtool/convert-key-to-title (str param))
                                                                                 (gcomp/input-text-with-atom
                                                                                  {:local-changes local-changes
                                                                                   :store-id (join-vec tab-path-to-value [param])
                                                                                   :val (str (get table-property param))
                                                                                   :enabled? enabled})
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
                                      (gcomp/hr 15) ;; Columns properties
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
                                             :border (b/empty-border :thickness 0)))
                      :else (c/label :text "Table not found inside metadata :c"))]
      ;; (@gseed/changes-service :add-controller :view-id view-id :local-changes local-changes)
      component)))

(defn add-to-view-service--table-editor
  ([table-id meta]
      (let [table          (get-table-configuration-from-list-by-table-id meta table-id)
         view-id        (keyword (get table :table))  ;; Get name of table and create keyword to check tabs bar (opens views)
         invoker-id     ((state/state :jarman-views-service) :get-my-view-id)]
     (do
       ((state/state :jarman-views-service)
        :set-view
        :view-id view-id
        :title (str "Edit: " (get-in table [:prop :table :representation]))
        :tab-tip (str "Edit panel with \"" (get-in table [:prop :table :representation]) "\" table.")
        :component-fn (fn [] (create-view--table-editor view-id (session/get-user-permission) table-id invoker-id meta))
        :scrollable? false)))))



;; ┌─────────┐
;; │         │
;; │ DB View │
;; │         │
;; └─────────┘


(def popup-menu-for-table
  "Description:
       Right Mouse Click Menu on table to control clicked table"
  (fn [JLP meta table-id x y]
    (let [border-c "#bbb"
          selected-tab (filter (fn [tab-conf] (= (second (first tab-conf)) table-id)) meta)
          table-name (get (first selected-tab) :table_name)
          
          mig (mig-panel
               :id :db-viewer--component--menu-bar
               :background (gtool/opacity-color)
               :border (b/line-border :thickness 1 :color border-c)
               :constraints ["wrap 1" "0px[150, fill]0px" "0px[30px, fill]0px"]
               :items [[(c/label)]])
          
          rm-menu (fn [e] (.remove JLP mig) (.repaint JLP))
          
          btn (fn [txt ico onclick] (c/label
                                     ;; :font (gtool/getFont 13)
                                     :text txt
                                     :icon (stool/image-scale ico 30)
                                     :background "#fff"
                                     :foreground "#000"
                                     :border (b/compound-border (b/empty-border :left 10 :right 15)
                                                                (b/line-border :bottom 1 :color border-c))
                                     :listen [:mouse-clicked onclick
                                              :mouse-entered (fn [e] (c/config! e :background "#d9ecff" :foreground "#000" :cursor :hand))
                                              :mouse-exited  (fn [e] (do
                                                                       (c/config! e :background "#fff" :foreground "#000")
                                                                       (let [bounds (c/config mig :bounds)
                                                                             mouse-y (+ (+ (.getY e) (.getY (c/config e :bounds))) (.getY bounds))
                                                                             mouse-x (.getX e)]
                                                                         (if (or (< mouse-x 5)
                                                                                 (> mouse-x (- (.getWidth bounds) 5))
                                                                                 (< mouse-y (+ (.getY bounds) 5))
                                                                                 (> mouse-y (- (+ (.getHeight bounds) (.getY bounds)) 5)))
                                                                           (rm-menu e)))))]))
          items (gtool/join-mig-items
                 ;; Popup menu buttons
                 (btn "Edit table" (gs/icon GoogleMaterialDesignIcons/EDIT face/c-icon)
                      (fn [e] (do (rm-menu e)
                                  (add-to-view-service--table-editor table-id meta))))
                 (btn "Delete table"  (gs/icon GoogleMaterialDesignIcons/DELETE face/c-icon) (fn [e]))
                 (btn "Show relations"  (gs/icon GoogleMaterialDesignIcons/AUTORENEW face/c-icon) (fn [e]))
                 (if (session/allow-permission? [:developer])
                   (btn "Metadata"
                        (gs/icon GoogleMaterialDesignIcons/DESCRIPTION face/c-icon)
                        (fn [e]
                          (do (rm-menu e)
                              ;; (gcomp/view-metadata-editor (keyword (:table_name (first selected-tab))))
                              )))
                   [])
                 (if (session/allow-permission? [:developer])
                   (btn "Defview"
                        (gs/icon GoogleMaterialDesignIcons/EDIT face/c-icon)
                        (fn [e]
                          (do (rm-menu e)
                              ((get (state/state :defview-editors) (keyword (:table_name (first selected-tab)))) e))))
                   []))]
      (c/config! mig
                 :bounds [x y 150 (* 30 (count items))]
                 :items items)
      mig)))

(defn table-visualizer--element--col-as-row
  "Description:
      Create a primary or special row that represents a column in the table"
  [meta JLP data
   & {:keys [debug]
      :or {debug false}}]
  (if debug (println "--Column as row\n--Data: " data))
  (let [last-x (atom 0)
        last-y (atom 0)
        component (c/label
                   :text (str (:representation data))
                   
                   :size [(:width data) :by (if (= (:type data) "header")
                                              (- (:height data) 2)
                                              (- (:height data) 0))]

                   :icon (cond
                           (= (:type data) "connection")
                           (gs/icon GoogleMaterialDesignIcons/SWAP_HORIZ face/c-icon (/ (+ 8 (:height data)) 1))
                           (= (:type data) "key")
                           (gs/icon GoogleMaterialDesignIcons/SWAP_HORIZ face/c-icon (/ (+ 8 (:height data)) 1))
                           :else nil)
                   
                   :background (cond
                                 (= (:type data) "header")     face/c-main-menu-bg
                                 (= (:type data) "key")        face/c-item-expand-right;;"#e2fbde"
                                 (= (:type data) "connection") face/c-item-expand-right;;"#e2fbde"
                                 :else face/c-compos-background)
                   
                   :font (cond
                           (= (:type data) "header")     (gs/getFont :bold)
                           (= (:type data) "key")        (gs/getFont 12)
                           (= (:type data) "connection") (gs/getFont 12)
                           :else "#fff")
                   
                   :foreground (cond
                                 (= (get data :type) "header") face/c-foreground-title
                                 :else face/c-foreground)
                   
                   :border (cond
                             (= (get data :type) "header") (b/compound-border (b/empty-border :thickness 4))
                             :else                         (b/compound-border (b/empty-border :thickness 4)))
                   
                   :listen [:mouse-entered (fn [e] (if (= (:type data) "header") (c/config! e :cursor :move)))
                            :mouse-clicked
                            (fn [e]
                              (let [[start-x start-y] (gtool/get-mouse-pos)]
                                (reset! last-x start-x)
                                (reset! last-y start-y)
                                (c/move! (:vpanel data) :to-front))
                              (c/invoke-later
                               (let [table-id (:id data)]
                                 (cond (= (.getButton e) MouseEvent/BUTTON3)
                                       (.add JLP
                                             (popup-menu-for-table ;; Open popup menu for table
                                              JLP
                                              meta  ;; forward list of table configuration
                                              table-id
                                              (-> (.getX e) (+ (.getX (c/config (:vpanel data) :bounds))) (- 15))
                                              (-> (.getY e) (+ (.getY (c/config e :bounds))) (+ (.getY (c/config (:vpanel data) :bounds))) (- 10)))
                                             (new Integer 999) ;; z-index
                                             )
                                       (= (.getClickCount e) 2) ;; Open table editor by double click
                                       (add-to-view-service--table-editor table-id meta)))))
                            
                            :mouse-dragged
                            (fn [e]
                              (let [vpanel (:vpanel data)]
                                (if (not (nil? vpanel))
                                  (do
                                    
                                    (if (or (= -1 @last-x) (= -1 @last-y))
                                      (let [[start-x start-y] (gtool/get-mouse-pos)]
                                        (reset! last-x start-x)
                                        (reset! last-y start-y)))
                                    
                                    (c/config! e :cursor :move)
                                    (c/move! vpanel :to-front)
                                    
                                    (let [old-bounds (c/config vpanel :bounds)
                                          [old-x old-y] [(.getX old-bounds) (.getY old-bounds)]
                                          [new-x new-y] (gtool/get-mouse-pos)
                                          move-x  (if (= 0 @last-x) 0 (- new-x @last-x))
                                          move-y  (if (= 0 @last-y) 0 (- new-y @last-y))]
                                      (reset! last-x new-x)
                                      (reset! last-y new-y)
                                      
                                      (let [next-x (+ old-x move-x)
                                            next-y (+ old-y move-y)
                                            next-x (if (< next-x 0) 0 next-x)
                                            next-y (if (< next-y 0) 0 next-y)]
                                        (c/config! vpanel :bounds [next-x next-y :* :*])))))))
                            
                            :mouse-released
                            (fn [e]
                              (reset! last-x -1)
                              (reset! last-y -1)
                              (let [dbv-bounds    (rift (state/state :dbv-bounds) {}) ;;(rift (state/state :dbv-bounds) {})
                                    vpanel-bounds (c/config (:vpanel data) :bounds)
                                    [x y] [(.getX vpanel-bounds) (.getY vpanel-bounds)]]
                                ;;(println "\nID" (:id data))
                                ;; (println "\nNew XY: " (:table-name data) [x y])
                                (state/set-state :dbv-bounds (assoc dbv-bounds (:table-name data) [x y])))
                              (c/config! e :cursor :default)
                              (.repaint (c/to-root e)))])]
    
    (if debug (println "--Column as row: OK"))
    component))

;; (state/state :dbv-bounds)
;; => {7 [301.0 322.0], 1 [650.0 120.0], 4 [421.0 2.0], 15 [265.0 723.0], 13 [265.0 442.0], 6 [14.0 343.0], 3 [562.0 446.0], 12 [14.0 245.0], 2 [138.0 246.0], 11 [301.0 247.0], 9 [561.0 638.0], 5 [650.0 283.0], 14 [212.0 51.0], 10 [11.0 14.0], 8 [417.0 342.0]}
;; (state/set-state :dbv-bounds {})

(defn db-viewer--component--table
  "Description:
     Create one table on JLayeredPane using database map
  "
  [bounds table-meta meta JLP]
  (let [bg-c "#fff"
        line-size-hover 2  ;; zwiekszenie bordera dla eventu najechania mysza
        border-c face/c-on-focus-light
        border (b/line-border :thickness 1 :color border-c)
        border-hover (b/line-border :thickness line-size-hover :color "#000")
        x (nth bounds 0)
        y (+ 0 (nth bounds 1))
        w (nth bounds 2)
        row-h 30  ;; wysokosc wiersza w tabeli reprezentujacego kolumne
        table-name (:table_name table-meta)
        table-id   (:id table-meta)

        vpanel (c/vertical-panel
                :id         (:id table-meta)
                :tip        "PPM to show more function."
                :border     border
                :background bg-c
                :user-data  {:table-name table-name})
        
        col-in-rows (map (fn [col]
                           (table-visualizer--element--col-as-row
                            meta
                            JLP
                            {:representation
                             (cond
                               (and (get-in table-meta [:prop :table :is-linker?])
                                    (contains? col :key-table))
                               (do ;; Get represetation of linked table
                                 (let [search (filter (fn [table-meta] (= (get-in table-meta [:table]) (get col :key-table))) meta)]
                                   (get-in (first search) [:prop :table :representation])))
                               :else (get col :representation))
                             
                             :width      w
                             :height     row-h
                             :border-c   border-c
                             :id         table-id
                             :table-name table-name
                             :vpanel     vpanel
                             
                             :type
                             (cond
                               (and (get-in table-meta [:prop :table :is-linker?])
                                    (contains? col :key-table)) "connection"
                               (contains? col :key-table) "key"
                               :else "row")}))
                         (get-in table-meta [:prop :columns]))  ;; przygotowanie tabeli bez naglowka
        
        header-row (table-visualizer--element--col-as-row
                    meta
                    JLP
                    {:representation (get-in table-meta [:prop :table :representation])
                     :vpanel     vpanel
                     :table-name table-name
                     :id         table-id
                     :width      w
                     :height     row-h
                     :type       "header"
                     :border-c   border-c})
        
        camplete-table (conj col-in-rows header-row)  ;; dodanie naglowka i finalizacja widoku tabeli
        
        h (* (count camplete-table) row-h)  ;; podliczenie wysokosci gotowej tabeli
        ]
    
    (c/config! vpanel
               :bounds [x y w h]
               :items  camplete-table)
    vpanel))


;; (println MouseEvent/BUTTON3)
;; (@startup)
;; (((state/state :jarman-views-service) :reload))


(def create-view--db-view
  "Descriptionci:
     Create component and set to @views atom to use in functional space. 
     Add open tab for db-view to open tabs bar.
     Set prepare view from @views to functional space.
   "
  (fn []
   (let [rootJLP (new JLayeredPane)
         JLP     (new JLayeredPane)
         last-x  (atom 0)
         last-y  (atom 0)
         meta    (mt/getset)]
     (doall (map
             (fn [tables]
               (doall
                (map
                 (fn [table-props]
                   (let [bounds (first table-props)
                         table-meta  (second table-props)]
                     ;;(println "\nTable" table-props) [bounds] {:table :meta}
                     
                     (.add JLP (db-viewer--component--table
                                bounds
                                table-meta
                                meta
                                JLP)
                           (new Integer 5))))
                 
                 tables)))
             (calculate-bounds-and-tables meta 20 5)))
     (.add rootJLP (c/vertical-panel
                    :items [JLP]
                    :id :JLP-DB-Visualizer
                    :border nil
                    :bounds [0 0 10000 10000]
                    :listen [:mouse-pressed
                             (fn [e]
                               (let [[start-x start-y] (gtool/get-mouse-pos)]
                                 (reset! last-x start-x)
                                 (reset! last-y start-y)
                                 (c/move! (c/to-widget e) :to-front)))
                             
                             :mouse-dragged
                             (fn [e]
                               (c/config! e :cursor :move)
                               (let [old-bounds (c/config (c/to-widget e) :bounds)
                                     [old-x old-y] [(.getX old-bounds) (.getY old-bounds)]
                                     [new-x new-y] (gtool/get-mouse-pos)
                                     move-x  (if (= 0 @last-x) 0 (- new-x @last-x))
                                     move-y  (if (= 0 @last-y) 0 (- new-y @last-y))]
                                 (reset! last-x new-x)
                                 (reset! last-y new-y)
                                 (c/config! (c/to-widget e) :bounds [(+ old-x move-x) (+ old-y move-y) :* :*])))
                             
                             :mouse-released
                             (fn [e]
                               (c/config! e :cursor :default)
                               (.repaint (c/to-root e)))])
           (new Integer 1))
    ;;  (.setMaximumSize JLP (java.awt.Dimension. 300 300))
    ;;  (.setSize JLP (java.awt.Dimension. 300 300))
     (mig-panel
      :border nil
      :constraints ["wrap 1" "0px[grow, fill]0px" "5px[fill]5px[grow, fill]0px"]
      :items [[(gcomp/menu-bar
                {:id :db-viewer--component--menu-bar
                 :buttons [["Show all relation" (gs/icon GoogleMaterialDesignIcons/AUTORENEW face/c-icon) "" (fn [e])]
                           ["Save view" (gs/icon GoogleMaterialDesignIcons/DONE face/c-icon) "" (fn [e] (doall
                                                                                    (map
                                                                                     (fn [save-xy]
                                                                                       (if-not (and (nil? (first  save-xy))
                                                                                                    (nil? (second save-xy)))
                                                                                         (let [table-name (first save-xy)
                                                                                               table-name (if (nil? table-name) nil (keyword table-name))
                                                                                               xy         (rift (second save-xy) nil)]
                                                                                           (let [meta (first (mt/getset! table-name))
                                                                                                 new-meta (assoc-in meta [:prop :table :bounds] xy)]
                                                                                             (mt/update-meta new-meta)))))
                                                                                     (state/state :dbv-bounds)))
                                                                             (i/info (gtool/get-lang-alerts :success)
                                                                                     (gtool/get-lang-alerts :changes-saved))
                                                                             (state/set-state :dbv-bounds {}))]
                           ["Reset view" (gs/icon GoogleMaterialDesignIcons/VIEW_QUILT face/c-icon) "" (fn [e]
                                                                             (state/set-state :dbv-bounds {})
                                                                             (((state/state :jarman-views-service) :reload)))]
                           ["Reloade view" (gs/icon GoogleMaterialDesignIcons/AUTORENEW face/c-icon) "" (fn [e] (((state/state :jarman-views-service) :reload)))]]})]
              [rootJLP]]))))


;; meta (mt/metadata-get table-keyword)
;; (mt/metadata-set (assoc meta :prop (read-string (c/config (:code state) :text))))

(state/state :dbv-bounds)

;; => {7 [301.0 322.0], 1 [650.0 120.0], 4 [421.0 2.0], 15 [265.0 723.0], 13 [265.0 442.0], 6 [14.0 343.0], 3 [562.0 446.0], 12 [14.0 245.0], 2 [138.0 246.0], 11 [301.0 247.0], 9 [561.0 638.0], 5 [650.0 283.0], 14 [212.0 51.0], 10 [11.0 14.0], 8 [417.0 342.0]}
;; (state/set-state :dbv-bounds {})





;;(mt/getset)
