(ns jarman.plugin.table
  (:require
   ;; Clojure toolkit 
   [clojure.data :as data]
   [clojure.string :as string]
   [clojure.spec.alpha :as s]
   [clojure.pprint :as pprint]
   ;; Seesaw components
   [seesaw.util :as u]
   [seesaw.core :as c]
   [seesaw.border :as sborder]
   [seesaw.dev :as sdev]
   [seesaw.mig :as smig]
   [seesaw.swingx :as swingx]
   [seesaw.chooser :as chooser]
   [seesaw.border  :as b]
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

;; {:model-reprs "Config",
;;     :model-param :permission.configuration,
;;     :model-comp jarman.gui.gui-components/state-input-text}

(defn action-handler [state action-m]
  (case (:action action-m)
          :clear-model    (assoc-in state [:model] {})
          :clear-changes  (assoc-in state [:model-changes] {})
          :update-changes (assoc-in state (join-vec [:model-changes] (:path action-m)) (:value action-m))
          :set-model      (assoc-in state [:model] (:value action-m))
          :state-update   (assoc-in state (:path action-m) (:value action-m))
          :update-export-path (assoc-in state [:export-path] (:value action-m))
          :test           (do (println "\nTest") state)))

(defn- create-header
  [state!]
  (c/label :text (:representation (:table-meta (:plugin-toolkit (state!)))) 
           :halign :center
           :border (sborder/empty-border :top 10)))

(defn- form-type [state!]
  (if (nil? (:model-update (:plugin-config (state!))))
    :model-insert
    :model-update))

(defn- set-state-watcher
  [state! root render-fn watch-path]
  (if (nil? (get-in (state!) watch-path))
    (swap! (state! :atom) #(assoc-in % watch-path nil)))
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
  [state! render-fn watch-path & props]
  (let [props (rift props [])
        root (apply
              c/vertical-panel
              props)]
    (set-state-watcher state!
                       root
                       render-fn
                       watch-path)
    (c/config! root :items (render-fn))))

(defn- create-expand
  [state! render-fn]
  (jvpanel state! render-fn [:model]))


(defn show-table-in-expand [model-data]
  (let [mig (seesaw.mig/mig-panel :constraints ["wrap 2" "0px[grow, fill]0px" "0px[fill, top]0px"])
        border (b/compound-border (b/empty-border :left 4))]
    (doall (map (fn [[k v]] (do (.add mig (seesaw.core/label :background "#E2FBDE" :size [100 :by 20] :text (name k) :font (gtool/getFont :size 11) :border border))
                                (.add mig (seesaw.core/label :background "#fff" :size [140 :by 20] :text (str v) :font (gtool/getFont :size 11) :border border)))) model-data))
    (.repaint mig) mig))



(defn refresh-panel [colmn-panel ct-conf ct-data dialog-model-id id]
  (let [model-to-repre (fn [list-tables model-colmns]
                         (let [model-col  (gtable/gui-table-model-columns list-tables (keys model-colmns))
                                 list-repr (into {} (map (fn [model] {(:key model)(:text model)})  model-col))]  {"repr" "kjj"}
                           (into {} (map (fn [a b] {(second a) ((first a) model-colmns)}) list-repr model-colmns))))]
    (.removeAll colmn-panel)
    (.add colmn-panel (show-table-in-expand  
                       (model-to-repre (:tables ct-conf)
                                       (do (println "SELECT >>>")
                                           (first ((:select ct-data) {:where [:= dialog-model-id id]}))))))
    (.revalidate colmn-panel)
    (.repaint colmn-panel)))

(defn input-related-popup-table
  "Description:
     Component for dialog window with related table. Returning selected table model (row)."
  [{:keys [val state field-qualified dispatch!]}]
  (let [dialog-path      (field-qualified (:dialog (:plugin-config @state)))
        ct-conf          (get-in ((:plugin-global-config @state)) (vec (concat dialog-path [:config])))
        ct-data          (get-in ((:plugin-global-config @state)) (vec (concat dialog-path [:toolkit])))
        dialog-model-id  (:model-id ct-data) ;;:permission.id
        dialog-fn        (:dialog ct-data) 
        key-column       (:model-id (:plugin-toolkit @state)) ;;user.id
        colmn-panel      (seesaw.core/flow-panel :hgap 0 :vgap 0)
        component        (gcomp/expand-input 
                          {:panel colmn-panel
                           :onClick (fn [e]
                                      (let [state @state
                                            dialog  (dialog-model-id (dialog-fn (dialog-model-id (:model state))))]
                                        (refresh-panel colmn-panel ct-conf ct-data dialog-model-id dialog)
                                        (dispatch!   
                                         {:action :update-changes
                                          :path   [(rift field-qualified :unqualifited)]
                                          :value dialog})))})]
    (if-not (nil? (:model @state))
      (do (println "MODEL" (:model @state))
          (println "ID"    (dialog-model-id (:model @state)))
          (refresh-panel colmn-panel ct-conf ct-data dialog-model-id (dialog-model-id (:model @state)))))
    component))

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
     Export panel invoker. Invoke as popup window.
   "
  [state! dispatch!]
  (let [{plugin-toolkit :plugin-toolkit
         table-model    :model} (state!)]
    (gcomp/button-basic
          "Document export"
          :font (getFont 13 :bold)
          :onClick (fn [e]
                     [(gcomp/popup-window
                       {:window-title "Documents export"
                        :view (document-exporter state! dispatch!)
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
     :font (getFont 13)
     :onClick (fn [e]
                (cond
                  (= type :insert)
                  (if-not (empty? (:model-changes (state!)))
                    (let [insert-m (:model-changes (state!))]
                      (println "\nRun Insert\n" ((:insert plugin-toolkit) insert-m) "\n")
                      (dispatch! {:action :clear-changes})))
                  (= type :clear) (do (dispatch! {:action :clear-model})
                                      (dispatch! {:action :clear-changes}))
                  (= type :update) ;; TODO: Turn on update fn after added empty key map template, without throw exception, too may value in query, get permission_name
                  (do
                    (let [table-id (first (:model-columns plugin-toolkit))
                          update-m (into {table-id (table-id table-model)} (:model-changes (state!)))]
                      (println "\nRun Update: \n" ((:update plugin-toolkit) update-m) "\n")
                      (dispatch! {:action :clear-model})
                      (dispatch! {:action :clear-changes})
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
                (if-not (= type :changes)(((state/state :jarman-views-service) :reload)))))))

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
     Convert to component manual by map with overriding
   "
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
                                      :value  (c/value (c/to-widget e))}))
                                  )
            pre-comp  (comp-fn {:func      func
                                :val       val
                                :state     state!
                                :dispatch! dispatch!
                                :action    :update-changes
                                :path      [field-qualified]})
            comp      (gcomp/inpose-label title pre-comp)]
        (.add panel comp))

      ;; Plugin as popup component
      ;; (vector? (:model-comp m))
      ;; (let [title     (rift (:model-reprs m) "")
      ;;       qualified (:model-param m)
      ;;       val       (if (empty? table-model) "" (qualified table-model))
      ;;       binded    (rift (:bind-args m) {})
      ;;       props     {:state state :dispatch! dispatch! :title title :val val}
      ;;       props     (if (empty? binded) props (merge-binded-props props binded))
      ;;       comp      (c/label :text "Plugin component here")] ;; TODO: Implement plugin invoker
      ;;   comp)
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


;; :model-update
;;   [:documents.id
;;    {:model-reprs "Table",
;;     :model-param :documents.table_name,
;;     :model-comp jarman.gui.gui-components/state-table-list}
;;    :documents.name
;;    :documents.prop]

(defn convert-key-to-component
  "Description
     Convert to component automaticly by keyword.
     key is an key from model in defview as :user.name.
   "
  [state! dispatch! panel meta-data key]
  (let [meta            (key meta-data)
        field-qualified (:field-qualified meta)
        title           (:representation  meta)
        editable?       (:editable?       meta)
        comp-types      (:component-type  meta)
        val             (cond
                          (not (nil? (key (:model-changes (state!))))) (str (key (:model-changes (state!))))
                          (not (nil? (key (:model (state!)))))   (str (key (:model (state!))))
                          :else "")
        func            (fn [e] (dispatch!
                                 {:action :update-changes
                                  :path   [(rift field-qualified :unqualifited)]
                                  :value  (c/value (c/to-widget e))}))
        comp (gcomp/inpose-label title
                                 (cond
                                   (= mt/column-type-linking (first comp-types))
                                   (input-related-popup-table {:val val :state (state! :atom) :field-qualified field-qualified :dispatch! dispatch!})
                                  ;; (gcomp/state-input-text {:func func :val val})
                                   
                                   (or (= mt/column-type-data (first comp-types))
                                       (= mt/column-type-datatime (first comp-types)))
                                   (calendar/state-input-calendar {:func func :val val})
                                   
                                   (= mt/column-type-textarea (first comp-types))
                                   (gcomp/state-input-text-area {:func func :val val})

                                   :else
                                   (gcomp/state-input-text {:func func :val val})))]
    (.add panel comp)))


(defn convert-metadata-vec-to-map
  "Description:
     Convert [{:x a :field-qualified b}{:d w :field-qualified f}] => {:b {:x a :field-qualified b} :f {:d w :field-qualified f}}"
  [coll]  (into {} (doall (map (fn [m] {(keyword (:field-qualified m)) m}) coll))))


(defn convert-model-to-components-list
  "Description
     Switch fn to convert by map or keyword
   "
  [state! dispatch! panel model-defview]
  ;; (println (format "\nmeta-data %s\ntable-model %s\nmodel-defview %s\n" meta-data table-model model-defview))
  (let [meta-data (convert-metadata-vec-to-map (get-in (state!) [:plugin-toolkit :columns-meta]))]
    (doall (->> model-defview
                     (map #(cond
                             (map? %)     (convert-map-to-component state! dispatch! panel meta-data %)
                             (keyword? %) (convert-key-to-component state! dispatch! panel meta-data %)))))))


(defn generate-custom-buttons
  "Description:
     Get buttons and actions from defview and create clickable button."
  [state! dispatch! current-model]
  (let [{model-changes :model-changes
         plugin-config :plugin-config} (state!)]
    (let [button-fn (fn [title action]
                      (if (fn? action)
                        [(gcomp/button-basic title :onClick (fn [e] (action model-changes)))]))]
      (doall (->> (:buttons plugin-config)
                  (map (fn [btn-model]
                         (if (= current-model (:form-model btn-model))
                           (let []
                             (println "\nCurrent model " current-model
                                      "\nBtn-model     " (:form-model btn-model))
                             (button-fn (:title btn-model) (get (:actions plugin-config) (:action btn-model)))) [])))
                  (filter-nil))))))


;; ┌──────────────┐
;; │              │
;; │ Form Builder │
;; │              │
;; └──────────────┘

;; TODO: Spec dla meta-data
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
          panel (smig/mig-panel :constraints ["wrap 1" "0px[grow, fill]0px" "0px[fill]0px"]
                                :border (sborder/empty-border :thickness 10)
                                :items [[(c/label)]])
          active-buttons (:active-buttons plugin-config)
          components (filter-nil
                      (flatten
                       (list
                        (gcomp/hr 10)
                        (rift (generate-custom-buttons state! dispatch! current-model) nil)
                        (gcomp/hr 5)

                        (if (in? active-buttons :changes)
                          (default-buttons state! dispatch! :changes) nil)
                        
                        (if (in? active-buttons :clear)
                             (default-buttons state! dispatch! :clear) nil)

                        (if (empty? (:model (state!)))
                          (if (in? active-buttons :insert)
                            (default-buttons state! dispatch! :insert) nil)
                          
                          [(if (in? active-buttons :update)
                             (default-buttons state! dispatch! :update) nil)
                           (if (in? active-buttons :delete)
                             (default-buttons state! dispatch! :delete) nil)
                           (gcomp/button-basic "Back to Insert" :onClick (fn [e] (dispatch! {:action :set-model :value {}})))
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
    (let [main-layout (smig/mig-panel :constraints ["" "0px[shrink 0, fill]0px[grow, fill]0px" "0px[grow, fill]0px"])
          table       ((:table (gtable/create-table (:plugin-config  (state!))
                                                    (:plugin-toolkit (state!))))
                       (fn [model-table]
                         (if-not (= false (:update-mode (:plugin-config (state!))))
                           (dispatch! {:action :set-model :value model-table}))))
          main-layout (c/config!
                       main-layout
                       :items [[(create-expand
                                 state! (fn []
                                         [(gcomp/min-scrollbox
                                           (gcomp/expand-form-panel
                                            main-layout
                                            [(create-header state!)
                                             (build-input-form state! dispatch!)])
                                           :hscroll :never)]))]
                               [(try
                                  (c/vertical-panel :items [table])
                                  (catch Exception e
                                    (c/label :text (str "Problem with table model: " (.getMessage e)))))]])]
      main-layout)
    ;; (c/label :text "Testing mode")
    ))

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
(s/def ::action keyword?)
(s/def ::title string?)
(s/def ::one-button
  (s/keys :req-un [::form-model
                   ::action
                   ::title]))


;;; * Task list for morfeu5z
;;; - [ ] TODO Repalce all `any?` predicate by writing for it reall spec
;;; - [ ] TODO make big structural spec for `model-insert` and `actions`
;;; - [ ] MAYBE change `:insert-button`... and same keys, for some DSL declaration
;;;       for example `:display-buttons` with value `:tft` where you mean
;;;       combination of tree keys t(true) and f(false) in order `insert` `update` `delete`.
;;;       By the way it's really expandable pattern :)

(s/def ::model-insert any?)
(s/def ::model-update any?)
(s/def ::actions any?)
(s/def ::insert-button boolean?)
(s/def ::update-button boolean?)
(s/def ::delete-button boolean?)
(s/def ::export-button any?)
(s/def ::chenges-button any?)
(s/def ::buttons (s/coll-of ::one-button))


;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; EXTERNAL INTERFAISE ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn table-toolkit-pipeline [configuration]
 (query-toolkit/data-toolkit-pipeline configuration {}))


(defn- create-state-template [plugin-path global-configuration-getter]
  (atom {:plugin-path          plugin-path
         :plugin-global-config global-configuration-getter
         :plugin-config        (get-in (global-configuration-getter) (conj plugin-path :config) {})
         :plugin-toolkit       (get-in (global-configuration-getter) (conj plugin-path :toolkit) {}) 
         :history              []
         :model                {}
         :model-changes        {}}))


(defn- create-disptcher [atom-var]
  (fn [action-m]
    (swap! atom-var (fn [state] (action-handler state action-m)))
    ;; (println "\nModel")
    ;; (pprint/pprint (:model @atom-var))
    ;; (println "\nChange")
    ;; (pprint/pprint (:model-changes @atom-var))
    ;; (println "\nExport path")
    ;; (pprint/pprint (:export-path @atom-var))
    ))



(defn table-entry [plugin-path global-configuration]
  (let [state (create-state-template plugin-path global-configuration)
        dispatch! (create-disptcher state)
        state!    (fn [& prop]
                    (cond (= :atom (first prop)) state
                          :else (deref state)))]
    (println "\nBuilding plugin")
    (build-plugin-gui state! dispatch!)))


;;; ALEKS DONT TOUCH 
;;; is JULIA EXAMPLE CODE HOW TO CONNECT DIALOG 
(comment
  (defn show-table-in-expand [model-data]
  (let [mig (seesaw.mig/mig-panel :constraints ["wrap 2" "0px[grow, fill]0px" "0px[:20, grow, fill, top]0px"])
        border (b/compound-border (b/empty-border :left 4))]
    (doall (map (fn [[k v]] (do (.add mig (seesaw.core/label :background "#E2FBDE" :text (name k) :font (gtool/getFont) :border border))
                                (.add mig (seesaw.core/label :background "#fff" :text (str v) :font (gtool/getFont) :border border)))) model-data))
    (.repaint mig) mig))



;; (defn input-related-popup-table
;;   "Description:
;;      Component for dialog window with related table. Returning selected table model (row)."
;;   [{:keys [global-configuration local-changes field-qualified table-model key-table plugin-toolkit plugin-config]}]
;;   (let 
;;       [connected-table ((comp first vals :table key-table) (global-configuration))
;;        ct-conf         (:config  connected-table)
;;        ct-data         (:toolkit connected-table)
;;        dialog-path     (field-qualified (:dialog plugin-config))
;;        dialog-fn       (get-in (global-configuration) (vec (concat dialog-path [:toolkit :dialog])))
;;        key-column      (read-string (str key-table ".id"))
;;        model-to-repre  (fn [list-tables model-colmns]
;;                          (let [model-col (gtable/gui-table-model-columns list-tables (keys model-colmns))
;;                                list-repr (into {} (map (fn [model] {(:key model)(:text model)})  model-col))]
;;                            (into {} (map (fn [a b] {(second a) ((first a) model-colmns)}) list-repr model-colmns))))
;;        colmn-panel
;;       ;; (seesaw.core/vertical-panel)
;;        (seesaw.core/flow-panel :hgap 0 :vgap 0)
;;        component       (gcomp/expand-input 
;;                         {:local-changes local-changes
;;                          :panel colmn-panel
;;                          :onClick (fn [e] (reset! local-changes (assoc @local-changes
;;                                                                        field-qualified
;;                                                                        (key-column (dialog-fn (key-column table-model)))))
;;                                     (.removeAll colmn-panel)
;;                                     (.add colmn-panel (show-table-in-expand
;;                                                        (let [id-column (field-qualified @local-changes)]
;;                                                          (if (nil? id-column) {}
;;                                                              (model-to-repre (:tables ct-conf)
;;                                                                              (first (filter (fn [column] (= (key-column column) id-column))
;;                                                                                             ((:select ct-data)))))))))
;;                                     (.revalidate colmn-panel)
;;                                     (.repaint colmn-panel))})]
;;     component))
)




