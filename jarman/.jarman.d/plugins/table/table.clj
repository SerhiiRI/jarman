(do (load-file (str (clojure.java.io/file ".jarman.d" "plugins" "table" "composite_components.clj"))) nil)

(ns jarman.plugins.table
  (:require
   ;; Clojure toolkit 
   [clojure.data :as data]
   [clojure.string :as string]
   [clojure.spec.alpha :as s]
   [clojure.pprint :as pprint]
   ;; Seesaw components
   [seesaw.core   :as c]
   [seesaw.dev :as sdev]
   [seesaw.mig :as smig]
   [seesaw.swingx  :as swingx]
   [seesaw.chooser :as chooser]
   [seesaw.border  :as b]
   ;; Jarman toolkit
   [jarman.tools.lang :refer :all]
   [jarman.tools.swing :as stool]
   [jarman.resource-lib.icon-library :as icon]
   [jarman.gui.gui-tools      :as gtool]
   [jarman.gui.gui-editors    :as gedit]
   [jarman.gui.gui-components :as gcomp]
   [jarman.gui.gui-calendar   :as calendar]
   [jarman.gui.popup :as popup]
   [jarman.plugins.composite-components :as ccomp]
   [jarman.logic.composite-components :as lcomp]
   [jarman.logic.state :as state]
   [jarman.logic.metadata :as mt]
   [jarman.logic.document-manager :as doc]
   [jarman.plugin.spec :as spec]
   [jarman.plugin.data-toolkit :as query-toolkit]
   [jarman.plugin.gui-table :as gtable]
   [jarman.plugin.plugin :refer :all])
  (:import
   (java.awt Dimension BorderLayout)
   (java.util Date)
   (java.text SimpleDateFormat)))

(defn grouping-model [state table-model]
  (let [group-fn (:columns-group (:plugin-toolkit state))]
    (group-fn table-model)))

(defn ungrouping-model [state table-model]
  (let [ungroup-fn (:columns-ungroup (:plugin-toolkit state))]
    (ungroup-fn table-model)))

(defn action-handler
  "Description:
    Invoke fn using dispatch!.
  Example:
    (@state {:action :test})"
  [state action-m]
  (case (:action action-m)
    ;;; add switch for insert update
    :table-render   (assoc-in state [:table-render] (:value action-m))
    :add-missing    (assoc-in state (:path action-m) nil)
    :pepe-model     (assoc-in state [:model] {:pepe :pepe-was-here})
    :clear-model    (assoc-in state [:model] {})
    :clear-changes  (assoc-in state [:model-changes] {})
    :update-changes (assoc-in state (join-vec [:model-changes] (:path action-m)) (:value action-m))
    :set-model      (assoc-in state [:model] (grouping-model state (:value action-m)))
    :state-update   (assoc-in state (:path action-m) (:value action-m))
    :update-export-path (assoc-in state [:export-path] (:value action-m))
    :test           (do (println "\nTest") state)))

(defn- create-header
  "Description:
    Header in expand panel."
  [state!]
  (gcomp/vmig
   :hrules "[grow, center]"
   :items [[(c/label
             :text (:representation (:table-meta (:plugin-toolkit (state!)))) 
             :halign :center
             :font (gtool/getFont 15 :bold)
             :foreground "#2c7375"
             :border (b/compound-border (b/line-border :bottom 1 :color "#2c7375")
                                        (b/empty-border :top 10)))]]))

(defn- form-type
  "Description:
    Check "
  [state!]
  (if (nil? (:model-update (:plugin-config (state!))))
    :model-insert
    :model-update))

(defn- set-state-watcher
  "Description:
    Add watcher to component. If state was changed then rerender components in root using render-fn.
  Example:
    (set-state-watcher state! dispatch! container (fn [] component) [:path :to :state])"
  [state! dispatch! root render-fn watch-path]
  (if (nil? (get-in (state!) watch-path))
    (dispatch! {:action :add-missing
                :path   watch-path}))
  (add-watch (state! :atom) :watcher
             (fn [id-key state old-m new-m]
               (let [[left right same] (clojure.data/diff (get-in new-m watch-path) (get-in old-m watch-path))]
                 (if (not (and (nil? left) (nil? right)))
                   (let [root (if (fn? root) (root) root)]
                     (try
                       (c/config! root :items (render-fn))
                       (catch Exception e (println "\n" (str "Rerender exception:\n" (.getMessage e))) ;; If exeption is nil object then is some prolem with nechw component inserting
                              ))))))))

(defn- jvpanel
  "Description:
    Vertical panel with use watcher on state. Panel can rerender components inside when state was changed.
  Exception:
    (jvpanel state! dispatch! (fn [] component) [:path-to-state])"
  [state! dispatch! render-fn watch-path & props]
  (let [props (rift props [])
        root (apply
              c/vertical-panel
              props)]
    (set-state-watcher state!
                       dispatch!
                       root
                       render-fn
                       watch-path)
    (c/config! root :items (render-fn))))

(defn- create-expand
  [state! dispatch! render-fn]
  (jvpanel state! dispatch! render-fn [:model]))

(defn show-table-in-expand
  "Description
     Get model-data for build view, return mig-panel with labels (representation and value of column)
   Example
     (show-table-in-expand \"Permission name\" \"user\", Configuration \"{}\"} 2)
     => object, JPanel"
  [model-data scale] 
  (let [border      (b/compound-border (b/empty-border :left 4)) 
        font-size   (* 11 scale)
        width       (* 20 scale)
        mig         (seesaw.mig/mig-panel :constraints ["wrap 2" "0px[:25% , grow, fill]0px[:60%, grow, fill]0px"  (str "0px[" width ":, fill, top]0px")] :size [240 :by (* 20 (+ (count model-data) 1))])
        col-label   (fn [color text]
                      (let [l (seesaw.core/label :background color :text text
                                                 :font (gtool/getFont :size font-size) :border border)]
                        (.add mig l)))]
    (doall (map (fn [[k v]] (do (col-label "#E2FBDE" (name k))
                                (col-label "#fff" (str v)))) model-data))
    (.repaint mig) mig))

(defn refresh-panel
  "Description
     Function for refresh content of expand-panel with columns
   Example
     (refresh-panel colmn-panel build-expand-fn 23 2)"
  [colmn-panel build-expand-fn id scale]
  (.removeAll colmn-panel)
  (.add colmn-panel (build-expand-fn id scale))
  (.revalidate colmn-panel)
  (.repaint colmn-panel))

(defn input-related-popup-table
  "Description:
     Component for dialog window with related table. Returning selected table model (row)."
  [{:keys [state! dispatch! val field-qualified]}]
  (let [;; Current table plugin
        {{{dialog-path field-qualified} :dialog} :plugin-config
         plugin-global-getter                    :plugin-global-config} (state!)
        ;; Related dialog plugin
        {{dialog-tables  :tables} :config
         {dialog-model-id  :model-id
          dialog-component :dialog
          dialog-select    :select} :toolkit}
        (get-in (plugin-global-getter) dialog-path)
        model-to-repre   (fn [list-tables model-colmns]
                           (let [maps-repr (gtable/gui-table-model-columns list-tables (keys model-colmns))
                                 list-repr (into {} (map (fn [model] {(:key model)(:text model)}) maps-repr))]
                             ;;list-repr  {:permission.permission_name Permission name, :permission.configuration Configuration}
                             (into {} (map (fn [[field-qualified representation]]
                                             {representation (field-qualified model-colmns)}) list-repr))))
        build-expand-fn  (fn [id scale] (show-table-in-expand  
                                         (model-to-repre dialog-tables
                                                         (first (dialog-select {:where [:= dialog-model-id id]}))) scale))
        scale            1.4
        not-scaled       1
        colmn-panel      (seesaw.core/flow-panel
                          :hgap 0 :vgap 0
                          :cursor :hand
                          :listen [:mouse-clicked
                                   (fn [e] (popup/build-popup
                                            {:title "Show columns"
                                             :comp-fn (fn []
                                                        (gcomp/min-scrollbox 
                                                         (build-expand-fn (field-qualified (:model-changes (state!))) scale)))}))])
        update-changes (fn [val]
                         (dispatch!   
                          {:action :update-changes
                           :path   [(rift field-qualified :unqualifited)]
                           :value  val}))]
    (if-not (nil? (:model (state!)))
      (do
        (update-changes (rift (field-qualified (:model (state!))) (field-qualified (:model-changes (state!)))))
        (refresh-panel colmn-panel build-expand-fn (field-qualified (:model-changes (state!))) not-scaled)))
    (let [exi (gcomp/expand-input 
               {:title (if (nil? (get-in (state!) [:model-changes field-qualified]))
                         (gtool/get-lang :basic :empty)
                         (gtool/get-lang :basic :selected))
                :panel colmn-panel
                :onClick (fn [e]
                           (let [dialog  (dialog-model-id (dialog-component (field-qualified (:model-changes (state!)))))]
                             (refresh-panel colmn-panel build-expand-fn dialog not-scaled)
                             (update-changes dialog)
                             (c/config! (c/to-widget e)
                                        :text (if (nil? (get-in (state!) [:model-changes field-qualified]))
                                                (gtool/get-lang :basic :empty)
                                                (gtool/get-lang :basic :selected)))
                             (.repaint (c/to-root e))))})]
      exi)))


;; ┌───────────────┐
;; │               │
;; │ Docs exporter |
;; │               │
;; └───────────────┘

(defn- document-exporter
  "Description:
     Panel with input path and buttons for export."
  [state! dispatch!]
  (let [{model-changes  :model-changes
         plugin-config  :plugin-config
         plugin-toolkit :plugin-toolkit} (state!)
        select-file (gcomp/state-input-file
                     (fn [e] (dispatch!
                              {:action :update-export-path
                               :value  (c/value (c/to-widget e))}))
                     nil)
        table-id    (keyword (format "%s.id" (:field (:table-meta plugin-toolkit))))
        id          (table-id (:model (state!)))]
    (smig/mig-panel
     :constraints ["wrap 1" "0px[grow, fill]0px" "0px[fill]0px[grow]0px[fill]0px"]
     :background "#eee"
     :items (gtool/join-mig-items
             [select-file]
             (rift (doall
                    (map
                     (fn [doc-model]
                       [(gcomp/button-basic
                         (get doc-model :name)
                         :onClick (fn [e]
                                    (try
                                      ((doc/prepare-export-file
                                        (:->table-name plugin-config) doc-model) id (:file-path model-changes))
                                      ((state/state :alert-manager)
                                       :set {:header (gtool/get-lang-alerts :success)
                                             :body (gtool/get-lang-alerts :export-doc-ok)}  7)
                                      (catch Exception e ((state/state :alert-manager)
                                                          :set {:header (gtool/get-lang-alerts :faild)
                                                                :body (gtool/get-lang-alerts :export-doc-faild)}  7))))
                         :args [:halign :left])])
                     (:->documents plugin-config)))
                   (c/label))
             (gcomp/button-basic
              "Export"
              :onClick (fn [e] (println "Path to file: " (rift (:file-path model-changes) "No file selected")))
              :flip-border true)))))

(defn- export-button
  "Description:
     Export panel invoker. Invoke as popup window."
  [state! dispatch!]
  (let [{plugin-toolkit :plugin-toolkit
         table-model    :model} (state!)]
    (gcomp/button-basic
     "Document export"
     :font (gtool/getFont 13 :bold)
     :onClick (fn [e]
                [(gcomp/popup-window
                  {:window-title "Documents export"
                   :view (document-exporter (state!) dispatch!)
                   :size [300 300]
                   :relative (c/to-widget e)})]))))
;;(:model-id)
;; ┌───────────────────┐
;; │                   │
;; │ Single Components │
;; │                   │
;; └───────────────────┘


(defn default-buttons
  "Description:
     Create default buttons as insert, update, delete row.
     type - :insert, :update, :delete"
  [state! dispatch! type]
  (let [{plugin-toolkit :plugin-toolkit
         table-model    :model} (state!)]
    (gcomp/button-basic
     (type {:insert "Insert new data"  :update "Update row"  :delete "Delete row"
            :export "Documents export" :changes "Form state" :clear "Clear form"})
     :font (gtool/getFont 13)
     :onClick (fn [e]
                (cond
                  (= type :insert)
                  (if-not (empty? (:model-changes (state!)))
                    (let [insert-m (ungrouping-model (state!)(:model-changes (state!)))]
                      (println "INSERT MODEL CHANGES ___"  (ungrouping-model (state!)(:model-changes (state!))))
                      (println "\nRun Insert\n" ((:insert plugin-toolkit) insert-m) "\n")
                      ;; (dispatch! {:action :clear-changes})
                      ))
                  (= type :clear) (do (dispatch! {:action :clear-model})
                                      (dispatch! {:action :clear-changes}))
                  (= type :update)
                  (do
                    (let [table-id (first (:model-columns plugin-toolkit))
                          update-m (into {table-id (table-id table-model)} (ungrouping-model (state!) (:model-changes (state!))))]
                      (try
                        ((:update plugin-toolkit) update-m)
                        (catch Exception e (popup/build-popup {:title "Warning" :size [300 200] :comp-fn (fn [] (c/label :text "Wrong data to update!"))})))
                      (dispatch! {:action :clear-model})
                      ;; (dispatch! {:action :clear-changes})
                      ))
                  (= type :delete)
                  (let [to-delete {(first (:model-columns plugin-toolkit))
                                   (get table-model (first (:model-columns plugin-toolkit)))}]
                    (println "\nRun Delete: \n" ((:delete plugin-toolkit) to-delete) "\n")
                    (dispatch! {:action :clear-model}))
                  (= type :changes)
                  (do
                    (println "\nLooks on chages: " (:model-changes (state!)))
                    (gcomp/popup-info-window "Changes" (str (:model-changes (state!))) (state/state :app))))
                (if-not (= type :changes) ((:table-render (state!)))))))) 

(defn get-missed-props
  "Description:
     Return not binded map, just cut this what exist.
   Example:
     (get-missed-key {:a a} {:a a :b c :d e}) 
       => {:b c, :d e}"
  [binded-map orgin-map]
  (->> (map #(first %) orgin-map)
       (filter (fn [orgin-key] (not (in? (map #(first %) binded-map) orgin-key))))
       (into {} (map #(into {% (% orgin-map)})))))

(defn merge-binded-props
  "Description:
     Get map where are binded keys, get properties for component and create new map with properties.
   Example:
     (merge-binded-props {:title \"mytitle\" :value \"pepe\"} {:title :custom-key}) 
       => {:custom-key \"mytitle\" :value \"pepe\"}
   "
  [props-map binded-list]
  (let [binded (doall
                (into {} (map #(let [orginal-key (first %)
                                     binded-key  (second %)]
                                 {binded-key (orginal-key props-map)})
                              binded-list)))]
    (into binded (get-missed-props binded props-map))))


;; ┌──────────────────────────┐
;; │                          │
;; │ To Components Converters │
;; │                          │
;; └──────────────────────────┘

(defn convert-map-to-component
  "Description
     Convert to component manual by map with overriding"
  [state! dispatch! panel meta-data m]
  ;; (println "\nOverride")
  (let [k           (:model-param m)
        meta        (k meta-data)
        table-model (:model (state!))]
    (cond
      ;; Overrided componen
      (symbol? (:model-comp m))
      (let [comp-fn         (resolve (symbol (:model-comp m)))
            title           (rift (:model-reprs m) "")
            field-qualified (:model-param m)
            val             (if (empty? table-model) "" (field-qualified table-model))
            func            (rift (:model-action m)
                                  (fn [e]
                                    (dispatch!
                                     {:action :update-changes
                                      :path   [field-qualified]
                                      :value  (c/value (c/to-widget e))})))
            pre-comp  (comp-fn {:func      func
                                :val       val
                                :state!    state!
                                :dispatch! dispatch!
                                :action    :update-changes
                                :path      [field-qualified]})
            comp      (gcomp/inpose-label title pre-comp)]
        (.add panel comp))
      :else (.add panel (c/label :text "Wrong overrided component")))))


;; :model-insert
;;   [{:model-reprs "Table",
;;     :model-param :documents.table_name,
;;     :model-comp jarman.gui.gui-components/state-table-list
;;     :model-action :my-fn}
;;    :documents.name
;;    :documents.prop
;;    {:model-reprs "Path to file",
;;     :model-param :documents.document,
;;     :model-comp jarman.gui.gui-components/state-input-file}]

;; :model-update
;;   [{:model-reprs "Table",
;;     :model-param :documents.table_name,
;;     :model-comp jarman.gui.gui-components/state-table-list}
;;    :documents.name
;;    :documents.prop]

(defn isComponent? [val]
  (some #(% val) lcomp/component-list))

(defn convert-key-to-component
  "Description
     Convert to component automaticly by keyword.
     key is an key from model in defview as :user.name."
  [state! dispatch! panel meta-data key]
  
  (let [meta            (key meta-data)
        ;;  meta            (meta/.return-columns-join meta)
        field-qualified (:field-qualified meta)
        title           (:representation  meta)
        editable?       (:editable?       meta)
        comp-types      (:component-type  meta)
        val             (cond
                          (not (nil? (key (:model (state!))))) (key (:model (state!)))
                          (not (nil? (key (:model-changes (state!))))) (key (:model-changes (state!))))
        val             (if (isComponent? val)
                          val (str val))
        func            (fn [e] (dispatch!
                                 {:action :update-changes
                                  :path   [(rift field-qualified :unqualifited)]
                                  :value  (c/value (c/to-widget e))}))
        comp-func       (fn [e col-key] 
                          (dispatch!
                           {:action :update-changes
                            ;;:state-update
                            :path   [(rift field-qualified :unqualifited)]
                            :value (assoc (key (:model-changes (state!))) col-key (c/value (c/to-widget e)))}))
        comp (gcomp/inpose-label
              ;;seesaw.mig/mig-panel :constraints ["wrap 1" "0px[fill, grow]0px" "5px[]5px"]
              ;;:items
              ;; (c/label :text title
              ;;          :font (gtool/getFont 13))
              ;;"align l"
              
              title
              (cond
                (= mt/column-type-linking (first comp-types))
                (input-related-popup-table {:val val :state! state! :field-qualified field-qualified :dispatch! dispatch!})
                
                (or (= mt/column-type-data (first comp-types))
                    (= mt/column-type-datatime (first comp-types)))
                (calendar/state-input-calendar {:func func :val val})

                (= mt/column-comp-url (first comp-types))
                (ccomp/url-panel {:func comp-func
                                  :val val})

                (= mt/column-comp-file (first comp-types))
                (ccomp/file-panel {:func comp-func
                                   :val val})

                (= mt/column-comp-ftp-file (first comp-types))
                (ccomp/ftp-panel {:func comp-func
                                  :val val})
                
                (= mt/column-type-textarea (first comp-types))
                (gcomp/state-input-text-area {:func func :val val})

                (= mt/column-type-prop (first comp-types))
                (gedit/state-code-area {:func func :val val})

                (= mt/column-type-boolean (first comp-types))
                (gcomp/state-input-checkbox {:func func :val val})
                
                :else
                (gcomp/state-input-text {:func func :val val})))]
    (.add panel comp)))


(defn convert-metadata-vec-to-map
  "Description:
     Convert [{:x a :field-qualified b}{:d w :field-qualified f}] => {:b {:x a :field-qualified b} :f {:d w :field-qualified f}}"
  [coll] (into {} (doall (map (fn [m] {(keyword (:field-qualified m)) m}) coll))))


(defn convert-model-to-components-list
  "Description
     Switch fn to convert by map or keyword"
  [state! dispatch! panel model-defview]
  ;; (println (format "\nmeta-data %s\ntable-model %s\nmodel-defview %s\n" meta-data table-model model-defview))
  (let [meta-data (convert-metadata-vec-to-map (get-in (state!) [:plugin-toolkit :columns-meta-join]))]
    (doall (->> model-defview
                (map #(cond
                        (map? %) (convert-map-to-component state! dispatch! panel meta-data %)
                        (keyword? %) (convert-key-to-component state! dispatch! panel meta-data %)))))))


(defn generate-custom-buttons
  "Description:
     Get buttons and actions from defview and create clickable button."
  [state! dispatch! current-model]
  (let [{plugin-config :plugin-config} (state!)]
    (let [button-fn (fn [title action]
                      (if (fn? action)
                        (gcomp/button-basic title :onClick (fn [e] (action state! dispatch!)))))]
      
      (doall (->> (:buttons plugin-config)
                  (map (fn [btn-model]
                         (if (= current-model (:form-model btn-model))
                           (let []
                             (println "\nCurrent model " current-model
                                      "\nBtn-model     " (:form-model btn-model))
                             (button-fn (:title btn-model) (get (:actions plugin-config) (:action btn-model)))) [])))
                  (filter-nil))))))

;; (seesaw.dev/show-options (seesaw.core/border-panel))

;; ┌──────────────┐
;; │              │
;; │ Form Builder │
;; │              │
;; └──────────────┘


(defn- custom-icon-bar
  [state! dispatch!
   & {:keys [more-front]}]
  (let [icos [{:icon-off icon/eraser-blue-64-png
               :icon-on  icon/eraser-blue-64-png
               :tip      "Clear state and form"
               :func     (fn [e]
                           ((:table-render (state!)))
                           (dispatch! {:action :pepe-model})
                           (dispatch! {:action :clear-changes})
                           (dispatch! {:action :clear-model}))}
              {:icon-on  icon/loupe-blue1-64-png
               :tip      "Display state"
               :func     (fn [e] (gcomp/popup-info-window
                                  "Changes"
                                  (str (:model-changes (state!)))
                                  (state/state :app)))}
              {:icon-on  icon/refresh-blue-64-png
               :tip      "Refresh table"
               :func     (fn [e] ((:table-render (state!))))}]
        icos (if (nil? more-front) icos (concat more-front icos))]
    (gcomp/icon-bar
     :size 35
     :align :right
     :margin [5 0 10 10]
     :items icos)))


(def build-input-form
  "Description:
     Marge all components to one form "
  (fn [state! dispatch!]
    ;; (println "\ndata-toolkit\n" data-toolkit "\nconfiguration\n" configuration)
    (let [plugin-toolkit (:plugin-toolkit (state!))
          plugin-config  (:plugin-config (state!))
          plugin-global-config (:plugin-global-config (state!))
          current-model (if (empty? (:model (state!)))
                          :model-insert
                          (if (nil? (:model-update plugin-config))
                            :model-insert
                            :model-update))
          table-id (keyword (format "%s.id" (:field (:table-meta plugin-toolkit))))
          model-defview (current-model plugin-config)
          panel  (smig/mig-panel :constraints ["wrap 1" "0px[grow, fill]0px" "0px[fill, top]0px"]
                                 :border (b/empty-border :thickness 10)
                                 :items [[(c/label)]])
          active-buttons (:active-buttons plugin-config)
          return-button  (if (empty? (:model (state!)))
                           []
                           [{:icon-off icon/pen-blue-64-png
                             :icon-on  icon/pen-blue-64-png
                             :tip      "Return to insert"
                             :func     (fn [e]
                                         (dispatch! {:action :set-model :value {}})
                                         ((:table-render (state!))))}])
          components (filter-nil
                      (flatten
                       (list
                        (gcomp/hr 5)
                        (custom-icon-bar
                         state! dispatch!
                         :more-front return-button)
                        
                        (gcomp/hr 10)
                        (generate-custom-buttons state! dispatch! current-model)
                        (gcomp/hr 5)

                        ;; (if (in? active-buttons :changes)
                        ;;   (default-buttons state! dispatch! :changes) nil)
                        
                        ;; (if (in? active-buttons :clear)
                        ;;      (default-buttons state! dispatch! :clear) nil)

                        (if (empty? (:model (state!)))
                          (if (in? active-buttons :insert)
                            (default-buttons state! dispatch! :insert) nil)
                          
                          [(if (in? active-buttons :update)
                             (default-buttons state! dispatch! :update) nil)
                           (if (in? active-buttons :delete)
                             (default-buttons state! dispatch! :delete) nil)
                           ;;(gcomp/button-basic "Back to Insert" :onClick (fn [e] (dispatch! {:action :set-model :value {}})))
                           (gcomp/hr 10)
                           (if (in? active-buttons :export)
                             (export-button state! dispatch!) nil)]))))]
      (convert-model-to-components-list state! dispatch! panel model-defview)
      (if (not (empty? components))
        (doall
         (map
          #(.add panel %)
          components)))
      panel)))


(def build-plugin-gui
  "Description
     Prepare and merge complete big parts"
  (fn [state! dispatch!]
    (let [main-layout  (smig/mig-panel :constraints ["" "0px[shrink 0, fill]0px[grow, fill]0px" "0px[grow, fill]0px"])
          
          table-fn     (fn [] ((:table (gtable/create-table (:plugin-config  (state!))
                                                            (:plugin-toolkit (state!))))
                               (fn [model-table]
                                 (println "MODEL" (:model (state!)))
                                 (println "MODEL-changes" (:model-changes (state!)))
                                 (if-not (= false (:update-mode (:plugin-config (state!))))
                                   (dispatch! {:action :set-model :value model-table})))))
          table-container (c/vertical-panel)
          table-render (fn []
                         (try
                           (c/config! table-container :items [(table-fn)])
                           (catch Exception e
                             (c/label :text (str "Problem with table model: " (.getMessage e))))))
          main-layout (c/config!
                       main-layout
                       :items [[(create-expand
                                 state!
                                 dispatch!
                                 (fn []
                                   [(gcomp/min-scrollbox
                                     (gcomp/expand-form-panel
                                      main-layout
                                      [(create-header state!)
                                       (build-input-form state! dispatch!)]
                                      :icon-open (stool/image-scale icon/left-blue-64-png 20)) ;;heyy
                                     :hscroll :never)]))]
                               [table-container]])]
      (table-render)
      (dispatch! {:action :set-model :value {}})
      (dispatch! {:action :table-render
                  :value table-render})
      main-layout)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; SPEC AND DECLARATION ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; structural SPEC pattern ;;

(s/def ::keyword-list (s/and sequential? #(every? keyword? %)))
;; (s/valid? ::keyword-list [:suka :bliat :dsaf])
;; (s/valid? ::keyword-list [:suka :bliat 32])
;; (s/valid? ::keyword-list 3)

;; plugin SPEC patterns ;;

;;; button list
(s/def ::form-model #{:model-insert :model-update :model-delete :model-select})
(s/def ::override (s/and map? not-empty))
(s/def ::column keyword?)
(s/def ::action keyword?)
(s/def ::title  string?)
(s/def ::func   fn?)

(s/def ::active-buttons ;; Check patern [:some :keys]
  (s/and vector?
         (s/coll-of #{:insert :update :delete :clear :changes})))

(s/def ::model-insert ;; Check patern [:user {}]
  (s/or
   :empty-v (s/and vector? empty?)
   :model   (s/and vector? not-empty
                   (s/coll-of
                    (s/or :key ::column
                          :map ::override)))))

(s/def ::model-update ;; Check patern [:user {}]
  ::model-insert)

(s/def ::actions ;; Check patern [{:key (fn [])}]
  (s/and vector?
         (s/coll-of
          (s/and map?
                 (s/coll-of
                  #(and (keyword? (first %))
                        (fn? (last  %))))))))


(defn- verify-buttons [m-valid]
  (v-tim
   {:form-model keyword?
    :action keyword?
    :title string?}
   m-valid))

(s/def ::buttons ;; Check patern [{:form-model keyword? :action keyword? :title string?}]
  (s/and
   vector?
   (s/coll-of verify-buttons))) ;; TODO: can we valid key value?


;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; EXTERNAL INTERFAISE ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn table-toolkit-pipeline [configuration]
  (query-toolkit/data-toolkit-pipeline configuration {}))


(defn create-state-template [plugin-path global-configuration-getter]
  (atom {:plugin-path          plugin-path
         :plugin-global-config global-configuration-getter
         :plugin-config        (get-in (global-configuration-getter) (conj plugin-path :config) {})
         :plugin-toolkit       (get-in (global-configuration-getter) (conj plugin-path :toolkit) {}) 
         :history              []
         :model                {}
         :model-changes        {}}))


(defn- create-disptcher [atom-var]
  (fn [action-m]
    (swap! atom-var (fn [state] (action-handler state action-m)))))


(defn table-entry [plugin-path global-configuration]
  (let [state (create-state-template plugin-path global-configuration)
        dispatch! (create-disptcher state)
        state!     (fn [& prop]
                     (cond (= :atom (first prop)) state
                           :else (deref state)))]
    (println "\nBuilding plugin")
    (build-plugin-gui state! dispatch!)))


(defplugin table 
  "Plugin allow to editing One table from database"
  ;; DATATOOLKIT SPEC
  [:id
   {:spec [:jarman.plugins.spec/keyword :opt-un]
    :doc "Custom plugin ID"}]
  [:name
   {:spec [:jarman.plugins.spec/name :req-un]
    :doc "Plugin name"
    :example ":name \"Some str\""}]
  [:tables
   {:spec [:jarman.plugins.data-toolkit/tables :req-un],
    :doc "list of used tables"
    :examples "[:permission]"}]
  [:view-columns
   {:spec [:jarman.plugins.data-toolkit/view-columns :req-un],
    :doc "Columns which must be represented in table on right side"
    :examples "[:permission.permission_name 
                :permission.configuration]"}]
  [:query
   {:spec [:jarman.plugins.data-toolkit/query :req-un],
    :examples "{:table_name :permission, :column [:#as_is ...]...}",
    :doc "SQL syntax for `select!` query"}]
  [:model-insert
   {:spec [:jarman.plugins.table/model-insert :req-un],
    :doc "Columns which represent model keys or map with overriding.
          * Bind-args is a overriding key name. 
            On left it's orginal key and you can set your own if you using another in component.
          * model-param - if you want to override component, use orgin column key. It will be 
            id in state too. If you want to add new component, set another model-param, some like
            my-comp-1."
    :examples "[:permission.permission_name 
                :permission.configuration]
                {:model-reprs \"Table\"
                 :model-param :documents.table_name
                 :bind-args {:store_id :state_is}
                 :model-comp jarman.gui.gui-components/select-box-table-list}"}]
  [:model-update
   {:spec [:jarman.plugins.table/model-update :opt-un],
    :doc "Columns which represent model keys or map with overriding.
          * Bind-args is a overriding key name. 
            On left it's orginal key and you can set your own if you using another in component.
          * model-param - if you want to override component, use orgin column key. It will be 
            id in state too. If you want to add new component, set another model-param, some like
            my-comp-1."
    :examples "[:permission.permission_name 
                :permission.configuration]
                {:model-reprs \"Table\"
                 :model-param :documents.table_name
                 :bind-args {:store_id :state_is}
                 :model-comp jarman.gui.gui-components/select-box-table-list}"}]
  [:active-buttons
   {:spec [:jarman.plugins.table/active-buttons :req-un]
    :doc "Select buttons who should be display."
    :examples ":active-buttons [:insert :update :delete :clear :changes]"}]
  [:actions
   {:spec [:jarman.plugins.table/actions :opt-un],
    :doc "Realise additional logic to standart CRUD operation. Set key as id and some fn with state as arg.
          \"{:some-action-keys (fn [state! dispatch!]...)
          :some-another.... }\""}]
  [:buttons
   {:spec [:jarman.plugins.table/buttons :opt-un],
    :examples "[{:form-model :model-insert, 
                 :action :upload-docs-to-db, 
                 :title \"Upload document\"}
               {:form-model :model-update...}...]"
    :doc "This is an vector of optional buttons which do some logic bainded by acition key, discribed in `:action`"}])



