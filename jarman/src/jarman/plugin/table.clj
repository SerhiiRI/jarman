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
  [state]
  (c/label :text (:representation (:table-meta (:plugin-toolkit @state))) 
           :halign :center
           :border (sborder/empty-border :top 10)))

(defn- form-type [state]
  (if (nil? (:model-update (:plugin-config @state)))
    :model-insert
    :model-update))

(defn- set-state-watcher
  [state root render-fn watch-path]
  (if (nil? (get-in @state watch-path))
    (swap! state #(assoc-in % watch-path nil)))
  (add-watch state :watcher
   (fn [id-key state old-m new-m]
     (let [[left right same] (clojure.data/diff (get-in new-m watch-path) (get-in old-m watch-path))]
       (if (not (and (nil? left) (nil? right)))
         (let [root (if (fn? root) (root) root)]
           (try
             (c/config! root :items (render-fn))
             (catch Exception e (println "\n" (str "Rerender exception:\n" (.getMessage e))) ;; If exeption is nil object then is some prolem with new component inserting
                    ))))))))

(defn- jvpanel
  [state render-fn watch-path & props]
  (let [props (rift props [])
        root (apply
              c/vertical-panel
              props)]
    (set-state-watcher state
                       root
                       render-fn
                       watch-path)
    (c/config! root :items (render-fn))))

(defn- create-expand
  [state action]
  (jvpanel state action [:model]))

(defn jmig
  [state render-fn watch-path & props]
  (let [props (rift props [])
        root (apply
              smig/mig-panel
              :constraints ["" "0px[grow]0px" "0px[50, fill]0px"]
              props)]
    (set-state-watcher state
                       root
                       render-fn
                       watch-path)
    (c/config! root :items [[(render-fn)]])))

(defn jlabel
  [state watch-path]
  (jmig state
        (fn [] [(c/label :text (str (get-in @state watch-path)))])
        watch-path))

(defn jtext
  [action]
  (c/text :listen [:caret-update action]))


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
     Component for dialog window with related table. Returning selected table model (row). "
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
    (if-not (nil? (field-qualified table-model))
      (swap! local-changes (fn [storage]
                             (assoc storage
                                    field-qualified
                                    (field-qualified table-model)))))
    (gcomp/input-text-with-atom
     {:local-changes local-changes
      :editable? false
      :val (model-to-repre (:view-columns ct-conf) table-model)
      :onClick (fn [e]
                 (let [selected-model (popup-table (:table (gtable/create-table ct-conf ct-data))
                                                   field-qualified
                                                   (c/to-frame e))]
                   (if-not (nil? (get selected-model (:model-id ct-data)))
                     (do (c/config! e :text (model-to-repre (:view-columns ct-conf) selected-model))
                         (swap! local-changes (fn [storage]
                                                (assoc storage
                                                       field-qualified
                                                       (get selected-model (:model-id ct-data)))))))))})))



;; ┌───────────────┐
;; │               │
;; │ Docs exporter |
;; │               │
;; └───────────────┘

(defn- document-exporter
  "Description:
     Panel with input path and buttons for export."
  [state dispatch!]
  (let [{model-changes  :model-changes
         plugin-config  :plugin-config
         plugin-toolkit :plugin-toolkit} @state
        select-file (gcomp/state-input-file
                     (fn [e] (dispatch!
                              {:action :update-export-path
                               :value  (c/value (c/to-widget e))}))
                     nil)
        table-id    (keyword (format "%s.id" (:field (:table-meta plugin-toolkit))))
        id          (table-id (:model @state))]
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
  [state dispatch!]
  (let [{plugin-toolkit :plugin-toolkit
         table-model    :model} @state]
    (gcomp/button-basic
          "Document export"
          :font (getFont 13 :bold)
          :onClick (fn [e]
                     [(gcomp/popup-window
                       {:window-title "Documents export"
                        :view (document-exporter state dispatch!)
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
  [state dispatch! type]
  (let [{plugin-toolkit :plugin-toolkit
         table-model    :model
         model-changes  :model-changes} @state]
    (gcomp/button-basic
     (type {:insert "Insert new data"  :update "Update row"  :delete "Delete row"
            :export "Documents export" :changes "Form state" :clear "Clear form"})
     :font (getFont 13)
     :onClick (fn [e]
                (cond
                  (= type :insert)
                  (if-not (empty? (:model-changes @state))
                    (let [insert-m (:model-changes @state)]
                      (println "\nRun Insert\n" ((:insert plugin-toolkit) insert-m) "\n")
                      (dispatch! {:action :clear-changes})))
                  (= type :clear) (do (dispatch! {:action :clear-model})
                                      (dispatch! {:action :clear-changes}))
                  (= type :update) ;; TODO: Turn on update fn after added empty key map template, without throw exception, too may value in query, get permission_name
                  (do
                    (let [table-id (first (:model-columns plugin-toolkit))
                          update-m (into {table-id (table-id table-model)} (:model-changes @state))]
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
                    (println "\nLooks on chages: " model-changes)
                    (gcomp/popup-info-window "Changes" (str model-changes) (state/state :app))))
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
  [state dispatch! panel meta-data m]
  (println "\nOverride")
  (let [k           (:model-param m)
        meta        (k meta-data)
        table-model (:model @state)]
    (cond
      ;; Overrided componen
      (symbol? (:model-comp m))
      (let [comp-fn         (resolve (symbol (:model-comp m)))
            title           (rift (:model-reprs m) "")
            field-qualified (:model-param m)
            val             (if (empty? table-model) "" (field-qualified table-model))
            action          (:model-action m);; (rift (:model-action m)
                            ;;       nil
                            ;;       (fn [e state dispatch! action-k state-path]
                            ;;         (dispatch!
                            ;;          {:action action-k
                            ;;           :path   state-path
                            ;;           :value  (c/value (c/to-widget e))}))
                            ;;       )
            pre-comp  (if (or (nil? action) (empty? action))
                        (comp-fn state dispatch! :update-changes [field-qualified])
                        (rift
                         (comp-fn
                          (fn [e] (action e state dispatch! :update-changes [field-qualified])) val)
                         (c/label "Can not invoke component from defview.")))
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


(defn convert-key-to-component
  "Description
     Convert to component automaticly by keyword.
     key is an key from model in defview as :user.name.
   "
  [state dispatch! panel meta-data key]
  (let [meta            (key meta-data)
        field-qualified (:field-qualified meta)
        title           (:representation  meta)
        editable?       (:editable?       meta)
        comp-types      (:component-type  meta)
        val             (cond
                          (not (nil? (key (:model @state))))   (str (key (:model @state)))
                          (not (nil? (key (:model-changes @state)))) (str (key (:model-changes @state)))
                          :else "")
        action (fn [e]
                 (dispatch!
                  {:action :update-changes
                   :path   [field-qualified]
                   :value  (c/value (c/to-widget e))}))
        comp (gcomp/inpose-label title
                                 (cond
                                   (mt/column-type-linking (first comp-types))
                                   (gcomp/state-input-text action val)
                                   
                                   (or (= mt/column-type-data (first comp-types))
                                       (= mt/column-type-datatime (first comp-types)))
                                   (calendar/state-input-calendar action val)
                                   
                                   (= mt/column-type-textarea (first comp-types))
                                   (gcomp/state-input-text-area action val)

                                   :else
                                   (gcomp/state-input-text action val)))]
    (.add panel comp)))


(defn convert-model-to-components-list
  "Description
     Switch fn to convert by map or keyword
   "
  [state dispatch! panel meta-data model-defview]
  ;; (println (format "\nmeta-data %s\ntable-model %s\nmodel-defview %s\n" meta-data table-model model-defview))
  (doall (->> model-defview
              (map #(cond
                      (map? %)     (convert-map-to-component state dispatch! panel meta-data %)
                      (keyword? %) (convert-key-to-component state dispatch! panel meta-data %)))
              ;; (filter #(not (nil? %)))
              )))


(defn convert-metadata-vec-to-map
  "Description:
     Convert [{:x a :field-qualified b}{:d w :field-qualified f}] => {:b {:x a :field-qualified b} :f {:d w :field-qualified f}}"
  [coll]  (into {} (doall (map (fn [m] {(keyword (:field-qualified m)) m}) coll))))


(defn generate-custom-buttons
  "Description:
     Get buttons and actions from defview and create clickable button."
  [state dispatch! form-model]
  (let [{model-changes :model-changes
         plugin-config :plugin-config} @state]
    (let [button-fn (fn [title action]
                      (if (fn? action)
                        [(gcomp/button-basic title :onClick (fn [e] (action model-changes)))]))]
      (doall (->> (:buttons plugin-config)
                  (map (fn [btn-model]
                         (if (= form-model (:form-model btn-model))
                           (button-fn (:title btn-model) (get (:actions plugin-config) (:action btn-model))) [])))
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
  (fn [state dispatch!]
    ;; (println "\ndata-toolkit\n" data-toolkit "\nconfiguration\n" configuration)
    (let [plugin-toolkit (:plugin-toolkit @state)
          plugin-config  (:plugin-config @state)
          plugin-global-config (:plugin-global-config @state)
          form-model (if (nil? (:model-update plugin-config))
                       :model-insert
                       :model-update)
          table-id (keyword (format "%s.id" (:field (:table-meta plugin-toolkit))))
          meta-data (convert-metadata-vec-to-map (:columns-meta plugin-toolkit))
          model-defview (form-model plugin-config)
          panel (smig/mig-panel :constraints ["wrap 1" "0px[grow, fill]0px" "0px[fill]0px"]
                                :border (sborder/empty-border :thickness 10)
                                :items [[(c/label)]])
          components (filter-nil
                      (flatten
                       (list
                        (gcomp/hr 10)
                        (rift (generate-custom-buttons state dispatch! form-model) nil)
                        (gcomp/hr 5)

                        (if (= true (:changes-button plugin-config))
                          (default-buttons state dispatch! :changes) nil)
                        
                        (if-not  (= false (:clear-button plugin-config))
                             (default-buttons state dispatch! :clear) nil)

                        (if (empty? (:model @state))
                          (if-not  (= false (:insert-button plugin-config))
                            (default-buttons state dispatch! :insert) nil)
                          
                          [(if-not (= false (:update-button plugin-config))
                             (default-buttons state dispatch! :update) nil)
                           (if-not (= false (:delete-button plugin-config))
                             (default-buttons state dispatch! :delete) nil)
                           (gcomp/button-basic "Back to Insert" :onClick (fn [e] (dispatch! {:action :set-model :value {}})))
                           (gcomp/hr 10)
                           (if-not (= false (:export-button plugin-config))
                             (export-button state dispatch!) nil)]))))]
      (convert-model-to-components-list state dispatch! panel meta-data model-defview)
      (if (not (empty? components))
        (doall
         (map
          #(.add panel %)
          components)))
      panel)))

(def build-plugin-gui
  "Description
     Prepare and merge complete big parts"
  (fn [state dispatch!]
    (let [main-layout (smig/mig-panel :constraints ["" "0px[shrink 0, fill]0px[grow, fill]0px" "0px[grow, fill]0px"])
          table       ((:table (gtable/create-table (:plugin-config  @state)
                                                    (:plugin-toolkit @state)))
                       (fn [model-table]
                         (if-not (= false (:update-mode (:plugin-config @state)))
                           (dispatch! {:action :set-model :value model-table}))))
          main-layout (c/config!
                       main-layout
                       :items [[(create-expand
                                 state (fn []
                                         [(gcomp/min-scrollbox
                                           (gcomp/expand-form-panel
                                            main-layout
                                            [(create-header state)
                                             (build-input-form state dispatch!)])
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


;;; component
(defn table-entry [plugin-path global-configuration]
  (let [state (create-state-template plugin-path global-configuration)
        dispatch! (create-disptcher state)
        state!    #(deref state)]
    (let [;; Destruction state component
          {{plugin-title       :name
            plugin-plug-place  :plug-place
            plugin-permission  :permission
            :as plugin-config} :plugin-config
           plugin-global-config    :plugin-global-config
           plugin-toolkit          :plugin-toolkit}
          (state!)
          
          space (c/select (state/state :app) plugin-plug-place)
          atm (:atom-expanded-items (c/config space :user-data))]
      (if (s/valid? :jarman.plugin.spec/table plugin-config)
        (if (session/allow-permission? plugin-permission)
          (swap!
           atm
           (fn [inserted]
             (conj inserted
                   (gcomp/button-expand-child
                    plugin-title
                    :onClick
                    (fn [e]
                      ((state/state :jarman-views-service)
                       :set-view
                       :view-id (str "auto-" plugin-title)
                       :title plugin-title
                       :scrollable? false
                       :component-fn
                       (fn [] (build-plugin-gui state dispatch!)))))))))
        ((state/state :alert-manager)
         :set {:header "Error"
               :body (str (name (:table_name plugin-config)) "  "
                          (s/explain-str :jarman.plugin.spec/table plugin-title))}))
      (.revalidate space))))



