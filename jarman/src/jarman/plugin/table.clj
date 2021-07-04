(ns jarman.plugin.table
  (:require
   ;; Clojure toolkit 
   [clojure.data :as data]
   [clojure.string :as string]
   [clojure.spec.alpha :as s]
   ;; Seesaw components
   [seesaw.util :as u]
   [seesaw.core :as c]
   [seesaw.border :as sborder]
   [seesaw.dev :as sdev]
   [seesaw.mig :as smig]
   [seesaw.swingx :as swingx]
   [seesaw.chooser :as chooser]
   ;; Jarman toolkit
   [jarman.tools.swing :as stool]
   [jarman.tools.lang :refer :all]
   [jarman.resource-lib.icon-library :as ico]
   
   [jarman.gui.gui-tools :refer :all :as gtool]
   [jarman.gui.gui-seed :as gseed]
   [jarman.gui.gui-components :refer :all :as gcomp]
   [jarman.gui.gui-calendar :as calendar]
   [jarman.gui.gui-tutorials.key-dispacher-tutorial :as key-tut]
   
   [jarman.logic.session :as session]
   [jarman.logic.state :as state]
   [jarman.logic.metadata :as mt]
   [jarman.logic.document-manager :as doc]
   
   [jarman.plugin.spec :as spec]
   [jarman.plugin.data-toolkit :as query-toolkit]
   [jarman.plugin.gui-table :as gtable])
  (:import (java.util Date)
           (java.text SimpleDateFormat)))

(defn- popup-table [table-fn selected frame]
  (let [dialog (seesaw.core/custom-dialog :modal? true :width 600 :height 400 :title "Select component")
        table (table-fn (fn [table-model] (seesaw.core/return-from-dialog dialog table-model)))
        key-p (seesaw.mig/mig-panel
               :constraints ["wrap 1" "0px[grow, fill]0px" "5px[fill]5px[grow, fill]0px"]
              ;;  :border (sborder/line-border :color "#888" :bottom 1 :top 1 :left 1 :right 1)
               :items [[(c/label :text (gtool/get-lang :tips :press-to-search) :halign :center)]
                      ;;  [(seesaw.core/label
                      ;;    :icon (stool/image-scale ico/left-blue-64-png 30)
                      ;;    :listen [:mouse-entered (fn [e] (gtool/hand-hover-on e))
                      ;;             :mouse-exited (fn [e] (gtool/hand-hover-off e))
                      ;;             :mouse-clicked (fn [e] (.dispose (seesaw.core/to-frame e)))])]
                       [table]])
        key-p (key-tut/get-key-panel \q (fn [jpan] (.dispose (seesaw.core/to-frame jpan))) key-p)]
    (seesaw.core/config! dialog :content key-p :title (gtool/get-lang :tips :related-popup-table))
    ;; (.setUndecorated dialog true)
    (.setLocationRelativeTo dialog frame)
    (seesaw.core/show! dialog)))


(defn input-related-popup-table ;; TODO: Auto choosing component inside popup window
  "Description:
     Component for dialog window with related table. Returning selected table model (row).
   "
  [{:keys [global-configuration local-changes field-qualified table-model key-table]}]
  (let
   [connected-table (last (first (get-in global-configuration [key-table :table]))) ;; TODO: Set dedicate path to related table form data-toolkit
    ct-conf         (:config  connected-table)
    ct-data         (:toolkit connected-table)
    model-to-repre  (fn [view-columns table-model]
                      (->> view-columns
                           (map #(% table-model))
                           (filter some?)
                           (string/join ", ")))]
    (if-not (nil? (field-qualified table-model)) (swap! local-changes (fn [storage] (assoc storage field-qualified (field-qualified table-model)))))
    (gcomp/input-text-with-atom
     {:local-changes local-changes
      :editable? false
      :val (model-to-repre (:view-columns ct-conf) table-model)
      :onClick (fn [e]
                 (let [selected-model (popup-table (:table (gtable/create-table ct-conf ct-data)) field-qualified (c/to-frame e))]
                   (if-not (nil? (get selected-model (:model-id ct-data)))
                     (do (c/config! e :text (model-to-repre (:view-columns ct-conf) selected-model))
                         (swap! local-changes (fn [storage] (assoc storage field-qualified (get selected-model (:model-id ct-data)))))))))})))


;; ┌───────────────┐
;; │               │
;; │ Docs exporter |
;; │               │
;; └───────────────┘

(defn- document-exporter
  "Description:
     Panel with input path and buttons for export."
  [local-changes controller id]
  (let [select-file (gcomp/input-file {:store-id :file-path :local-changes local-changes})
        panel-bg "#eee"]
    (smig/mig-panel
     :constraints ["wrap 1" "0px[grow, fill]0px" "0px[fill]0px[grow]0px[fill]0px"]
     :background panel-bg
     :items (gtool/join-mig-items
             [select-file]
             (rift (map (fn [doc-model]
                          [(gcomp/button-basic
                            (get doc-model :name)
                            :onClick (fn [e]
                                       (try
                                         ((doc/prepare-export-file (:->table-name controller) doc-model) id (:file-path @local-changes))
                                         ((state/state :alert-manager) :set {:header (gtool/get-lang-alerts :success) :body (gtool/get-lang-alerts :export-doc-ok)}  7)
                                         (catch Exception e ((state/state :alert-manager) :set {:header (gtool/get-lang-alerts :faild) :body (gtool/get-lang-alerts :export-doc-faild)}  7))))
                            :args [:halign :left])])
                        (:->documents controller))
                   (c/label))
             (gcomp/button-basic
              "Export"
              :onClick (fn [e] (println "Path to file: " (rift (:file-path @local-changes) "No file selected")))
              :flip-border true)))))

(defn- export-button
  "Description:
     Export panel invoker. Invoke as popup window.
   "
  [data-toolkit configuration local-changes table-model]
  (gcomp/button-basic
   "Document export"
   :font (getFont 13 :bold)
   :onClick (fn [e]
              (gcomp/popup-window {:window-title "Documents export"
                                         :view (let [table-id (keyword (format "%s.id" (:field (:table-meta data-toolkit))))]
                                                 (document-exporter local-changes configuration (table-id table-model)))
                                         :size [300 300]
                                         :relative (c/to-widget e)}))))
;;(:model-id)
;; ┌───────────────────┐
;; │                   │
;; │ Single Components │
;; │                   │
;; └───────────────────┘

(defn default-buttons
  "Description:
     Create default buttons as insert, update, delete row.
     type - :insert, :update, :delete
   "
  [data-toolkit local-changes table-model type]
  (gcomp/button-basic
   (type {:insert "Insert new data" :update "Update row" :delete "Delete row" :export "Documents export" :changes "Form state"})
   :font (getFont 13)
   :onClick (fn [e]
              ;;  (println "Insert but Locla changes: " @local-changes)
              ;; (println "\nModel ID:" (first (:model-columns data-toolkit)))
              (cond
                (= type :insert)
                (let [from-meta-data (vemap (map #(:field-qualified %) (:columns-meta data-toolkit)))
                      update-list (cnmap (left-merge from-meta-data @local-changes))]
                  (println "\nRun Insert\n"
                           ((:insert data-toolkit) update-list) "\n"))
                (= type :update) ;; TODO: Turn on update fn after added empty key map template, without throw exception, too may value in query, get permission_name
                (do
                  (let [from-meta-data (vemap (map #(:field-qualified %) (:columns-meta data-toolkit)))
                        update-list (cnmap (left-merge from-meta-data @local-changes))
                        table-id (first (:model-columns data-toolkit))]
                    (println "\nRun Update: \n"
                             ((:update data-toolkit)
                              (into {table-id (table-id table-model)} update-list))
                             "\n")))
                (= type :delete)
                (println "\nRun Delete: \n"
                         ((:delete data-toolkit)
                          {(first (:model-columns data-toolkit))
                           (get table-model (first (:model-columns data-toolkit)))}) "\n")
                (= type :changes)
                (do
                  (println "\nLooks on chages: " @local-changes)
                  (gcomp/popup-info-window "Changes" (str @local-changes) (state/state :app))))
              (if-not (= type :changes)(((state/state :jarman-views-service) :reload))))))

(defn get-missed-props
  "Description:
     Return not binded map, just cut this what exist.
   Example:
     (get-missed-key {:a a} {:a a :b c :d e}) 
       => {:b c, :d e}
   "
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

;; ┌─────────────────────────┐
;; │                         │
;; │ Tabel Component Chooser │
;; │                         │
;; └─────────────────────────┘

(def form-components ;; List of functions creating components 
  {mt/column-type-data     #(calendar/calendar-with-atom %)
   mt/column-type-input    #(gcomp/input-text-with-atom  %)
   mt/column-type-number   #(gcomp/input-int             %)
   mt/column-type-textarea #(gcomp/input-text-area       %)
   mt/column-type-linking  #(input-related-popup-table   %)})

(defn- get-first-available-comp
  "Set types list, set components list and choose first if is inside. 
   Can be more than one type and more than one component"
  [type-coll comps-coll]
  (let [type-coll     (if (keyword? type-coll) [type-coll] type-coll)
        choosed-coll  (doall (map #(get comps-coll %) type-coll))
        choosed-first (first choosed-coll)]
    choosed-first))

(defn- choose-component-fn 
  "Invoke fn with components list into some var and using this var as fn.
   Var FN - Choose component send to fn types list and props map."[comps]
  (fn [type-coll props-coll]
    (let [props-coll (into props-coll (if (:val props-coll) {:val (:val props-coll)} {}))
          selected-comp-fn  (get-first-available-comp type-coll comps)]
      ;; (println "\nColumn type\n" type-coll)
      (if (nil? selected-comp-fn)
        (println (format "Component %s not exist." type))
        (gcomp/inpose-label (:title props-coll) (selected-comp-fn props-coll))))))

(def choose-component (choose-component-fn form-components)) ;; component chooser

;; ┌──────────────────────────┐
;; │                          │
;; │ To Components Converters │
;; │                          │
;; └──────────────────────────┘

(defn convert-map-to-component
  "Description
     Convert to component manual by map with overriding
   "
  [local-changes meta-data model-defview m]
  (let [k    (:model-param m)
        meta (k meta-data)]
    ;; (println "M" model-defview)
    ;; (println "map component\n" k "\n" m "\n" model-defview "\n" meta)
    ;; (println "\nComp" (:model-comp m))
    ;; (println "\nType" (type (:model-comp m)))
    (cond
      ;; Overrided component
      (symbol? (:model-comp m))
      (let [comp-fn (:model-comp m)
            comp-fn (resolve (symbol comp-fn))
            title   (rift (:model-reprs m) "")
            qualified (:model-param m)
            val     (if (empty? model-defview) "" (qualified model-defview))
            binded  (rift (:bind-args m) {})
            props {:title title :store-id qualified :local-changes local-changes :val val}
            props (if (empty? binded) props (merge-binded-props props binded))
            x     (if (nil? comp-fn) ((state/state :alert-manager) :set {:header (format "[ Warning %s ]" k) 
                                                                         :body (format "Function fron defview looks like nil. Probably syntax error. Key %s" k)} 5))
            pre-comp (rift (comp-fn props) (c/label "Can not invoke component from defview."))
            comp (gcomp/inpose-label title pre-comp)]
        ;; (println "Props: " props)
        ;; (println "\nComplete-----------")
        comp)

      ;; Plugin as popup component
      (vector? (:model-comp m))
      (let [title   (rift (:model-reprs m) "")
            qualified (:model-param m)
            val     (if (empty? model-defview) "" (qualified model-defview))
            binded  (rift (:bind-args m) {})
            props {:title title :store-id qualified :local-changes local-changes :val val}
            props (if (empty? binded) props (merge-binded-props props binded))
            comp (c/label :text "Plugin component here")] ;; TODO: Implement plugin invoker

        ;; (println "Props: " props)
        ;; (println "\nComplete-----------")
        comp)

      :else
      (c/label :text "Wrong overrided component")
      )))


(defn convert-key-to-component
  "Description
     Convert to component automaticly by keyword.
     key is an key from model in defview.
   "
  [global-configuration local-changes meta-data table-model key]
  
  (let [meta            (key meta-data)
        ;;x (println "\nMetadata for key" meta)
        field-qualified (:field-qualified meta)
        title           (:representation  meta)
        editable?       (:editable?       meta)
        comp-type       (:component-type  meta)
        key-table       (->> (rift (:key-table meta) nil) (#(if (keyword? %) % (keyword %))))
        val             (rift (str (key table-model)) "")
        ;; x               (println "\nMeta data\n" meta-data "\nMeta\n" meta "\nComp type\n" comp-type "\nKey\n" key)
        props {:title title :store-id field-qualified  :field-qualified field-qualified  :local-changes local-changes  :editable? editable?  :val val}
        comp  (if (in? comp-type mt/column-type-linking) ;; If linker add more keys to props map
                (choose-component comp-type (into props {:key-table key-table :table-model table-model :global-configuration global-configuration}))
                (choose-component comp-type props))]
    ;; (println "\nComplete-----------")
    comp))


(defn convert-model-to-components-list
  "Description
     Switch fn to convert by map or keyword
   "
  [global-configuration local-changes meta-data table-model model-defview]
  ;; (println (format "\nmeta-data %s\ntable-model %s\nmodel-defview %s\n" meta-data table-model model-defview))
  (doall (->> model-defview
              (map #(cond
                      (map? %)     (convert-map-to-component local-changes meta-data table-model %)
                      (keyword? %) (convert-key-to-component global-configuration local-changes meta-data table-model %)))
              (filter #(not (nil? %))))))


(defn convert-metadata-vec-to-map
  "Description:
     Convert [{:x a :field-qualified b}{:d w :field-qualified f}] => {:b {:x a :field-qualified b} :f {:d w :field-qualified f}}
   "
  [coll]  (into {} (doall (map (fn [m] {(keyword (:field-qualified m)) m}) coll))))

(defn generate-custom-buttons
  "Description:
     Get buttons and actions from defview and create clickable button.
   "
  [local-changes configuration form-model]
  (let [button-fn (fn [title action]
                    ;; (println "\nTitle " title "\nAction: "  action)
                    (if (fn? action)
                      [(gcomp/button-basic title :onClick (fn [e] (action local-changes)))]))]
    ;; (println @local-changes)
    (doall (->> (:buttons configuration)
                (map (fn [btn-model] (if (= form-model (:form-model btn-model)) (button-fn (:title btn-model) (get (:actions configuration) (:action btn-model))) [])))
                (filter-nil)))))


;; (defn- upload-doc ;; TODO: Move to defview when actions start working
;;   [state]
;;   (let [func (fn [state]
;;                (let [insert-meta {:table (first (:documents.table @state))
;;                                   :name (:documents.name @state)
;;                                   :document (:documents.document @state)
;;                                   :prop (:documents.prop @state)}]
;;                  (println "to save" insert-meta)
;;                  (jarman.logic.document-manager/insert-document insert-meta)
;;                  (((jarman.logic.state/state :jarman-views-service) :reload))
;;                  ))]
;;     (gcomp/button-basic
;;      "Upload doc"
;;      :onClick (fn [e] (func state))
;;      :args [:font (gtool/getFont :bold)])))


;; ┌──────────────┐
;; │              │
;; │ Form Builder │
;; │              │
;; └──────────────┘

;; TODO: Spec dla meta-data
(def build-input-form
  "Description:
     Marge all components to one form
   "
  (fn [data-toolkit configuration global-configuration form-model
       & {:keys [table-model more-comps]
          :or {table-model {} more-comps []}}]
    ;; (println "\ndata-toolkit\n" data-toolkit "\nconfiguration\n" configuration)
    (let [table-id (keyword (format "%s.id" (:field (:table-meta data-toolkit))))
          local-changes (atom {:selected-id (table-id table-model)})
          meta-data (convert-metadata-vec-to-map (:columns-meta data-toolkit))
          components (convert-model-to-components-list global-configuration local-changes meta-data table-model (form-model configuration))
          panel (smig/mig-panel :constraints ["wrap 1" "0px[grow, fill]0px" "0px[fill]0px"]
                                :border (sborder/empty-border :thickness 10)
                                :items [[(c/label)]])
          components (join-mig-items
                      components
                      (gcomp/hr 10)
                      (generate-custom-buttons local-changes configuration form-model)
                      (gcomp/hr 5)
                      (if (= true (:changes-button configuration)) (default-buttons data-toolkit local-changes table-model :changes) [])
                      (if (empty? table-model)
                        (if-not  (= false (:insert-button configuration))
                          (default-buttons data-toolkit local-changes table-model :insert) [])
                        [(if-not (= false (:update-button configuration))
                           (default-buttons data-toolkit local-changes table-model :update) [])
                         (if-not (= false (:delete-button configuration))
                           (default-buttons data-toolkit local-changes table-model :delete) [])
                         (gcomp/hr 10)
                         (if-not (= false (:export-button configuration)) (export-button data-toolkit configuration local-changes table-model) [])])
                      ;; (do (println "Field" (:field (:table-meta data-toolkit))) [])
                      ;; (if (= "documents" (:field (:table-meta data-toolkit))) (upload-doc local-changes) [])
                      [more-comps])
          builded (c/config! panel :items (gtool/join-mig-items components))]
      builded)))


(def auto-builder--table-view
  "Description
     Prepare and merge complete big parts"
  (fn [global-configuration
       data-toolkit
       configuration]
    (let [x nil ;;------------ Prepare components
          insert-form   (fn [] (build-input-form data-toolkit configuration global-configuration :model-insert))
          view-layout   (smig/mig-panel :constraints ["" "0px[shrink 0, fill]0px[grow, fill]0px" "0px[grow, fill]0px"])
          table         (fn [] (second (u/children view-layout)))
          header        (fn [] (c/label :text (:representation (:table-meta data-toolkit)) 
                                        :halign :center :border (sborder/empty-border :top 10)))
          update-form   (fn [table-model return] (gcomp/expand-form-panel
                                                  view-layout
                                                  [(header)
                                                   (build-input-form
                                                    data-toolkit
                                                    configuration
                                                    global-configuration 
                                                    (doto (if (nil? (:model-update configuration))
                                                            :model-insert
                                                            :model-update)
                                                      println) 
                                                    :table-model table-model
                                                    :more-comps [(return)])]))
          x nil ;;------------ Build
          expand-insert-form (gcomp/min-scrollbox (gcomp/expand-form-panel view-layout [(header) (insert-form)]) :hscroll :never)
          back-to-insert     (fn [] [(gcomp/button-basic "<< Return to Insert Form" :onClick (fn [e] (c/config! view-layout :items [[expand-insert-form] [(table)]])))])
          expand-update-form (fn [model-table return] (c/config! view-layout :items [[(gcomp/min-scrollbox (update-form model-table return))] [(table)]]))
          table              (fn [] ((:table (gtable/create-table configuration data-toolkit))
                                     (fn [model-table]
                                       (if-not (= false (:update-mode configuration))
                                         (expand-update-form model-table back-to-insert)))))
          x nil ;;------------ Finish
          view-layout        (c/config! view-layout :items [[(c/vertical-panel :items [expand-insert-form])]
                                                            [(try
                                                               (c/vertical-panel :items [(table)])
                                                               (catch Exception e (c/label :text (str "Problem with table model: " (.getMessage e)))))]])]
      view-layout)
    ;; (c/label :text "Testing mode")
    ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; SPEC AND DECLARATION ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(s/def :jarman.plugin.table/keyword-list (s/and sequential? #(every? keyword? %)))
;; (s/valid? :jarman.plugin.table/keyword-list [:suka :bliat :dsaf])
;; (s/valid? :jarman.plugin.table/keyword-list [:suka :bliat 32])
;; (s/valid? :jarman.plugin.table/keyword-list 3)
(s/def :jarman.plugin.table/tables :jarman.plugin.table/keyword-list)
(s/def :jarman.plugin.table/view-columns :jarman.plugin.table/keyword-list)
(s/def :jarman.plugin.table/model-insert :jarman.plugin.table/keyword-list)
(s/def :jarman.plugin.table/insert-button boolean?)
(s/def :jarman.plugin.table/delete-button boolean?)
(s/def :jarman.plugin.table/actions map?)
;;; button list
(s/def :jarman.plugin.table/form-model #{:model-insert :model-update :model-delete :model-select})
(s/def :jarman.plugin.table/action keyword?)
(s/def :jarman.plugin.table/title string?)
(s/def :jarman.plugin.table/one-button
  (s/keys :req-un [:jarman.plugin.table/form-model
                   :jarman.plugin.table/action
                   :jarman.plugin.table/title]))
(s/def :jarman.plugin.table/buttons (s/coll-of :jarman.plugin.table/one-button))
;; (s/valid? :jarman.plugin.table/buttons
;;           [{:form-model :model-insert, :action :upload-docs-to-db, :title "Upload document"}
;;            {:form-model :model-update, :action :update-docs-in-db, :title "Update document info"}
;;            {:form-model :model-update, :action :delete-doc-from-db, :title "Delete row"}])
(s/def :jarman.plugin.table/query map?)

;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; EXTERNAL INTERFAISE ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn table-toolkit-pipeline [configuration]
 (query-toolkit/data-toolkit-pipeline configuration {}))

;;; component
(defn table-entry [plugin-path global-configuration]
  (let [get-from-global #(->> % (join-vec plugin-path) (get-in (global-configuration)))
        data-toolkit  (get-from-global [:toolkit])
        configuration (get-from-global [:config])
        ;; title (get-in data-toolkit [:table-meta :representation])
        title (:name configuration)
        space (c/select (state/state :app) (:plug-place configuration))
        ;; x (println "\nplug-place"(:plug-place configuration) "\nspace"space)
        atm (:atom-expanded-items (c/config space :user-data))]
    ;; (println "\nData toolkit" data-toolkit)
    ;; (println "Allow Permission: " (session/allow-permission? (:permission configuration)))
    (println "\nPlugin path" plugin-path)
    ;; TODO: Set invoker expand button if not exist add child invokers
    (if (s/valid? :jarman.plugin.spec/table  configuration)
      (if (session/allow-permission? (:permission configuration))
        (do (swap! atm (fn [inserted]
                         (conj inserted
                               (gcomp/button-expand-child
                                title
                                :onClick (fn [e] ((state/state :jarman-views-service)
                                                  :set-view
                                                  :view-id (str "auto-" title)
                                                  :title title
                                                  :scrollable? false
                                                  :component-fn (fn [] (auto-builder--table-view
                                                                        (global-configuration)
                                                                        data-toolkit
                                                                        configuration))))))))))
      ((state/state :alert-manager) :set {:header "Error"
                                          :body (str (name (:table_name configuration)) "  "
                                                     (s/explain-str :jarman.plugin.spec/table configuration))} 5))
    (.revalidate space)))

