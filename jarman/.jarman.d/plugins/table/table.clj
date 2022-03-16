(ns plugin.table.table
  (:require
   ;; Clojure toolkit 
   [clojure.data :as data]
   [clojure.string :as string]
   [clojure.spec.alpha :as s]
   [clojure.pprint :refer [cl-format]]
   [clojure.java.jdbc :as jdbc]
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
   [jarman.tools.org  :refer :all]
   [jarman.resource-lib.icon-library :as icon]
   [jarman.faces              :as face]
   [jarman.gui.gui-style      :as gs]
   [jarman.gui.gui-tools      :as gtool]
   [jarman.gui.gui-editors    :as gedit]
   [jarman.gui.gui-components :as gcomp]
   [jarman.gui.gui-migrid     :as gmg]
   [jarman.gui.gui-calendar   :as calendar]
   [jarman.gui.popup :as popup]
   [jarman.logic.state :as state]
   [jarman.logic.metadata :as mt]
   [jarman.logic.document-manager :as doc]
   [jarman.plugin.spec :as spec]
   [jarman.plugin.data-toolkit :as query-toolkit]
   [jarman.plugin.gui-table :as gtable]
   [jarman.logic.view-manager :as view-manager]
   ;; external toolkit
   [jarman.interaction :as i]
   [jarman.external :refer [register-custom-view-plugin]]
   ;; locals 
   [plugin.table.composite-components :as ccomp])
  (:import
   (java.awt Dimension BorderLayout)
   (java.util Date)
   (java.text SimpleDateFormat)
   (jiconfont.icons.google_material_design_icons GoogleMaterialDesignIcons)))

(def ^:dynamic *debug* "enable debugin printing" false)
(def ^:dynamic *debug-prefix* "some ID for you know about name of used this plugin" "<empty>")

;;;;;;;;;;;;;;;;;;;;;;;;
;;; helper functions ;;;
;;;;;;;;;;;;;;;;;;;;;;;;

(defn create-state-template [plugin-path]
  (let [plugin (view-manager/plugin-link plugin-path)
        toolkit (.return-toolkit plugin)
        config  (.return-config  plugin)]
    ;; ---- Guards ----
    (when (nil? config)
      (throw (ex-info
              "Empty plugin config"
              {:type :empty-plugin-config
               :plugin :table
               :message-head [:header :plugin-name]
               :message-body [:alerts :empty-plugin-config]})))
    (when (nil? toolkit)
      (throw (ex-info
              "Empty plugin toolkit"
              {:type :empty-plugin-toolkit
               :plugin :table
               :message-head [:header :plugin-name]
               :message-body [:alerts :empty-plugin-toolkit]})))
    ;; Structure of State atom
    {:plugin-path       plugin-path
     :plugin-config     config
     :plugin-toolkit    toolkit
     :insert-mode       true
     :history           []
     :model             {}
     :model-changes     {}}))

(defmacro with-state [& body]
  `(let [{~'plugin-path       :plugin-path
          ~'plugin-config     :plugin-config
          ~'plugin-toolkit    :plugin-toolkit
          ~'model             :model
          ~'model-changes     :model-changes}  (~'state!)
         ~'table-meta (:meta-obj ~'plugin-toolkit)]
     ~@body))

(defmacro with-test-environ [plugin-path & body]
  `(let [~'plugin  (jarman.logic.view-manager/plugin-link ~plugin-path)
         ~'toolkit (.return-toolkit ~'plugin)
         ~'config  (.return-config  ~'plugin)]
     (let [~'plugin-path       ~plugin-path
           ~'plugin-config     ~'config
           ~'plugin-toolkit    ~'toolkit
           ~'table-meta        (:meta-obj ~'toolkit)
           ~'state
           (atom
            {:plugin-path       ~plugin-path
             :plugin-config     ~'config
             :plugin-toolkit    ~'toolkit
             :insert-mode       true
             :history           []
             :model             {}
             :model-changes     {}})
           ~'state!   (fn [] (deref ~'state))
           ~'model!   (fn [] (:model (deref ~'state)))
           ~'changes! (fn [] (:model-changes (deref ~'state)))
           ~'dispatch! (create-dispatcher ~'state)]
       ~@body)))

(defn grouping-model 
  "Description:
    get all columns, if columns belongs to composite components,
    it groups them
  Example:
   (grouping-model
     state {:seal.id 83,
            :seal.datetime_of_remove #inst \"2021-09-22T21:00:00.000000000-00:00\", 
            :seal.site_name \"pop\",
            :seal.ftp_file_path \"/home/julia/test.txt\"
            :seal.ftp_login nil})
   ;;=>
    '{:seal.id 82,
      :seal.datetime_of_remove #inst \"2021-09-22T21:00:00.000000000-00:00\",
      :seal.site     #Link{:text \"pop\", :link nil}
      :seal.ftp_file #FtpFile{:login nil,
                              :password nil,
                              :file-name \"test.txt\",
                              :file-path \"/home/julia/test.txt\"}}"
  [state table-model] (-> state :plugin-toolkit :meta-obj (.group table-model)))

(comment
  (with-test-environ [:seal :table :seal]
   (let [x
         (grouping-model
          (state!) {:seal.id 83,
                    :seal.datetime_of_remove #inst "2021-09-22T21:00:00.000000000-00:00", 
                    :seal.site_name "pop",
                    :seal.ftp_file_path "/home/julia/test.txt"
                    :seal.ftp_login nil})]
     (ungrouping-model (state!) x))))

(defn ungrouping-model
  "Description:
    ungroup columns of composite components
  Example:
    (ungrouping-model state {:seal.loc_file #jarman.logic.composite_components.File{:file-name \"test.txt\", :file nil}})
    => {:seal.file_name \"test.txt\", :seal.file nil}"
  [state table-model] (-> state :plugin-toolkit :meta-obj (.ungroup table-model)))

(defn- update-comp-changes 
  "Description:
    Prepare data for update column of composite component
   Example:
    (update-comp-changes state [:seal.ftp_file] {:file-name test.txt})
      => #FtpFile{:login nil, :password nil, :file-name test.txt, :file-path /home/julia/test.txt}"
  [state k-path value]
  (let [meta-obj (:meta-obj (:plugin-toolkit state))
        k-field  (first k-path)]
    (k-field (.group (.find-field-qualified meta-obj k-field)
                     (merge (reduce (fn [acc [k v]] (if (nil? v) acc (conj acc {k v}))) {}
                                    (ungrouping-model state  {k-field (get-in state (join-vec [:model-changes] k-path))}))
                            (first (map (fn [[k v]] {(keyword (str (first (string/split (name k-field) #"\.")) "."
                                                                  (name (first (.find-field-by-comp-var meta-obj k k-field))))) v})
                                        value)))))))

(comment
 (defn- update-comp-changes
   "Description:
    Prepare data for update column of composite component
   Example:
    (update-comp-changes state [:seal.ftp_file] {:file-name test.txt})
    => #jarman.logic.composite_components.FtpFile{:login nil, :password nil, :file-name test.txt, :file-path /home/julia/test.txt}"
   [state k-path value]
   (let [meta-obj (:meta-obj (:plugin-toolkit state))
         k-field  (first k-path)
         ^jarman.logic.metadata.FieldComposite composite-field (.find-field-qualified meta-obj k-field)]
     (k-field (.group composite-field
                      (merge (reduce (fn [acc [k v]] (if (nil? v) acc (conj acc {k v}))) {}
                                     (ungrouping-model state  {k-field (get-in state (join-vec [:model-changes] k-path))}))
                             (first (map (fn [[k v]] {(keyword (str (first (string/split (name k-field) #"\.")) "."
                                                                   (name (first (.find-field-by-comp-var meta-obj k k-field))))) v})
                                         value))))))))

(comment
  (with-test-environ [:seal :table :seal]
    (.group (.find-field-qualified table-meta :seal.site))))

;;;;;;;;;;;;;;;;
;;; dispatch ;;;
;;;;;;;;;;;;;;;;

(defn action-handler
  [state action-m]
  (let [meta-obj (:meta-obj (:plugin-toolkit state))
        action   (:action action-m)
        value    (:value action-m)
        k-path   (:path action-m)
        k-field  (first k-path)]
    (when (:debug? action-m) (cl-format *out* "AH:~@[ (act ~A)~]~@[ (pth ~A)~]~@[ (val ~A)~]~%" action k-path (pr-str value)))
    (case action
      :refresh-state        (merge state {:insert-mode value :model {} :model-changes {}})
      :switch-insert-update (assoc-in state [:insert-mode] value)
      :table-render         (merge state {:table-render value :model (grouping-model state {})})
      :add-missing          (assoc-in state k-path nil)
      :state-update         (assoc-in state k-path value)
      :update-changes       (assoc-in state (join-vec [:model-changes] k-path) value)
      :update-comps-changes (assoc-in state (join-vec [:model-changes] k-path) (update-comp-changes state k-path value))
      :download-comp        (do (.download (:v-obj action-m) {:local-path value :table_name (.return-table_name meta-obj)
                                                              :model-data (ungrouping-model state (:model state))}) state)
      :clear-state          (merge state {:model-changes {} :model {:temp "temp"}})
      :set-model            (assoc-in state [:model] (grouping-model state value))
      :update-export-path   (assoc-in state [:export-path] value)
      state)))


(defn action-print [m]
  (cl-format *out* "Action Hendler(~A):~%  Action~20T~A~%  --------~20T--------~%~{  ~A~20T~A~%~}" 
             (.getTime (java.util.Date.))
             (get m :action)
             (as-> m $
               (dissoc $ :debug? :action)
               (mapcat (fn [[k v]] (vector (name k) (pr-str v))) $))))

(defn debug-print [m]
  (cl-format *out* "=======DEBUG=======================~%")
  (action-print m))

(defn error-print [m reason]
  (cl-format *out* "=======ERROR=======================~%")
  (cl-format *out* "")
  (action-print))

(defn action-reset-model [state] {})
(defn action-assoc-in-model [state {:keys [path k value]}] (assoc-in state [:model-changes k] value))
(defn action-update-in-model [state k func] (update-in state [:model-changes k] func))
(defn action-handler [state event-map]
  ;; (let [meta-obj (:meta-obj (:plugin-toolkit state))
  ;;       ;; k-field  (first k-path)
  ;;       ]
  ;;   (debug-print event-map)
  ;;   (case action
  ;;     :model-update            (action-update-in-model state event-map)
  ;;     ;; :refresh-state        (merge state {:insert-mode value :model {} :model-changes {}})
  ;;     ;; :switch-insert-update (assoc-in state [:insert-mode] value)
  ;;     ;; :table-render         (merge state {:table-render value :model (grouping-model state {})})
  ;;     ;; :add-missing          (assoc-in state k-path nil)
  ;;     ;; :state-update         (assoc-in state k-path value)
  ;;     ;; :update-changes       (assoc-in state (join-vec [:model-changes] k-path) value)
  ;;     ;; :update-comps-changes (assoc-in state (join-vec [:model-changes] k-path) (update-comp-changes state k-path value))
  ;;     ;; :download-comp        (do (.download (:v-obj action-m) {:local-path value :table_name (.return-table_name meta-obj)
  ;;     ;;                                                         :model-data (ungrouping-model state (:model state))}) state)
  ;;     ;; :clear-state          (merge state {:model-changes {} :model {:temp "temp"}})
  ;;     ;; :set-model            (assoc-in state [:model] (grouping-model state value))
  ;;     ;; :update-export-path   (assoc-in state [:export-path] value)
  ;;     state))
  )


(defn- create-dispatcher [atom-var]
  (fn [action-m]
    (swap! atom-var (fn [state] (action-handler state action-m)))))

(defn- set-state-watcher
  "Description:
    Add watcher to component. If state was changed then rerender components in root using render-fn.
  Example:
    (set-state-watcher state! dispatch! container (fn [] component) [:path :to :state])"
  [state! dispatch! root render-fn watch-path]
  (if (nil? (get-in (state!) watch-path))
    (dispatch! {:action :add-missing
                :path   watch-path}))
  (add-watch (state! :obtain-atom) :watcher
             (fn [id-key state old-m new-m]
               (let [[left right same] (clojure.data/diff (get-in new-m watch-path) (get-in old-m watch-path))]
                 (if (not (and (nil? left) (nil? right)))
                   (let [root (if (fn? root) (root) root)]
                     (try
                       (c/config! root :items (render-fn)) ;;;render-fn
                       (catch Exception e
                         (println 
                          (format "Plugin `table`. Problem with registation watcher by path `%s` in state. \n"
                                  (str watch-path)) (.getMessage e))))))))))

;;;;;;;;;;;;;;;;;;;;;;
;;; GUI components ;;;
;;;;;;;;;;;;;;;;;;;;;;

(defn- jvpanel
  "Description:
    Vertical panel with use watcher on state. Panel can rerender components inside when state was changed.
  Exception:
    (jvpanel state! dispatch! (fn [] component) [:path-to-state])"
  [state! dispatch! render-fn watch-path & props]
  (let [props (rift props [])
        root (apply c/vertical-panel props)]
    (set-state-watcher state! dispatch! root render-fn watch-path)
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
        mig         (seesaw.mig/mig-panel :constraints ["wrap 2" "0px[:25% , grow, fill]0px[:60%, grow, fill]0px"  (str "0px[" width ":, fill, top]0px")]
                                          :size [240 :by (* 20 (+ (count model-data) 1))])
        col-label   (fn [color text]
                      (let [l (seesaw.core/label :background color :text text
                                                 :font (gtool/getFont :size font-size) :border border)]
                        (.add mig l)))]
    (doall (map (fn [[k v]] (do (col-label face/c-item-expand-left (name k))
                                (col-label face/c-item-expand-right (str v)))) model-data))
    (.repaint mig) mig))

(defn refresh-panel
  "Description
    Function for refresh content of expand-panel with columns
   Example
    (refresh-panel colmn-panel build-expand-fn 23 2)
  "
  [colmn-panel build-expand-fn id scale]
  (.removeAll colmn-panel)
  (.add colmn-panel (build-expand-fn id scale))
  (.revalidate colmn-panel)
  (.repaint colmn-panel))

;; (defn input-related-popup-table
;;   "Description:
;;     Component for dialog window with related table. Returning selected table model (row)."
;;   [{:keys [state! dispatch! val field-qualified]}]
;;   (let [;; Current table plugin
;;         {{{dialog-path field-qualified} :dialog} :plugin-config
;;          plugin-global-getter                    :plugin-global-config} (state!)
;;         ;; Related dialog plugin
;;         {{dialog-tables    :tables} :config
;;          {dialog-model-id  :model-id
;;           dialog-component :dialog
;;           dialog-select    :select} :toolkit}
;;         (get-in (plugin-global-getter) dialog-path)
;;         model-to-repre   (fn [list-tables model-colmns]
;;                            (let [maps-repr (gtable/gui-table-model-columns list-tables (keys model-colmns))
;;                                  list-repr (into {} (map (fn [model] {(:key model)(:text model)}) maps-repr))]
;;                              ;;list-repr  {:permission.permission_name Permission name, :permission.configuration Configuration}
;;                              (into {} (map (fn [[field-qualified representation]]
;;                                              {representation (field-qualified model-colmns)}) list-repr))))
;;         build-expand-fn  (fn [id scale]
;;                            (println (format "Selected id from dialog box '%d'" id))
;;                            (show-table-in-expand  
;;                                          (model-to-repre dialog-tables
;;                                                          (first (dialog-select {:where [:= dialog-model-id id]}))) scale))
;;         scale            1.4
;;         not-scaled       1
;;         colmn-panel      (seesaw.core/flow-panel
;;                           :hgap 0 :vgap 0
;;                           :cursor :hand
;;                           :listen [:mouse-clicked
;;                                    (fn [e] (popup/build-popup
;;                                             {:title "Show columns"
;;                                              :comp-fn (fn [] (gcomp/min-scrollbox 
;;                                                               (build-expand-fn (field-qualified (:model-changes (state!))) scale)))}))])
;;         update-changes (fn [val]
;;                          (dispatch!   
;;                           {:action :update-changes
;;                            :path   [(rift field-qualified :unqualifited)]
;;                            :value  val}))]
;;     (if-not (nil? (:model (state!)))
;;       (do
;;         (update-changes (rift (field-qualified (:model (state!))) (field-qualified (:model-changes (state!)))))
;;         (refresh-panel colmn-panel build-expand-fn (field-qualified (:model-changes (state!))) not-scaled)))
;;     (let [exi (gcomp/expand-input 
;;                {:title (if (nil? (get-in (state!) [:model-changes field-qualified]))
;;                          (gtool/get-lang :basic :empty)
;;                          (gtool/get-lang :basic :selected))
;;                 :panel colmn-panel
;;                 :onClick (fn [e]
;;                            (let [dialog  (dialog-model-id (dialog-component (field-qualified (:model-changes (state!)))))]
;;                              (refresh-panel colmn-panel build-expand-fn dialog not-scaled)
;;                              (update-changes dialog)
;;                              (c/config! (c/to-widget e)
;;                                         :text (if (nil? (get-in (state!) [:model-changes field-qualified]))
;;                                                 (gtool/get-lang :basic :empty)
;;                                                 (gtool/get-lang :basic :selected)))
;;                              (.repaint (c/to-root e))))})]
;;       exi)))

(defn input-related-popup-table
  "Description:
    Component for dialog window with related table. Returning selected table model (row)."
  [{:keys [state! dispatch! val field-qualified]}]
  (let [;; Current table plugin
        {{{dialog-path field-qualified} :dialog} :plugin-config
         plugin-global-getter                    :plugin-global-config} (state!)
        ;; Related dialog plugin
        {{dialog-tables    :tables} :config
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
        build-expand-fn  (fn [id scale]
                           (println (format "Selected id from dialog box '%d'" id))
                           (show-table-in-expand  
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
                                             :comp-fn (fn [] (gcomp/min-scrollbox 
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
    Panel with input path and buttons for export.
  "
  [state! dispatch!]
  (let [{model-changes  :model-changes
         plugin-config  :plugin-config
         plugin-toolkit :plugin-toolkit} (state!)
        select-file (gcomp/state-input-file
                     (fn [e] (dispatch!
                              {:action :update-export-path
                               :value  (c/value (c/to-widget e))}))
                     nil)
        table-id    (keyword (format "%s.id" (:field (.return-table (:meta-obj plugin-toolkit)))))
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
                                      (i/info (gtool/get-lang-alerts :success) (gtool/get-lang-alerts :export-doc-ok))
                                      (catch Exception e
                                        (i/warning (gtool/get-lang-alerts :failed) (gtool/get-lang-alerts :export-doc-failed)))))
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

;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Actions for buttons ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn group-clms
  "Description:
    group columns on two groups: composite columns (filter ComponentFile) and normal columns with others composite clms
   Example:
    (group-clms {:seal.loc_file #jarman.logic.composite_components.File{:file-name \"test.txt\", :file \"/home/julia/test.txt\"}....)
      => {true [[:seal.loc_file #jarman.logic...]], nil [[:sea;.seal_number 2324][:seal.site #jarman...]]}"
  ([data-model] (group-by (fn [[k v]] (mt/isComponentFiles? v)) data-model)))

(defn update-comp-col 
  "Description:
    update or insert columns of composite components, do method upload on record"
  ([state! id model-data]
   (let [meta-obj       (:meta-obj (:plugin-toolkit (state!)))
         col-model      (ungrouping-model (state!) (into {} model-data))
         all-colmns-nil (fn [col-model] (into {} (map (fn [[k v]] [k nil]) col-model)))]
     (doall (map (fn [[k v]]
                   (let [columns-types (doc/get-columns-types k meta-obj)]
                     (.upload v {:id id
                                 :table_name  (.return-table_name meta-obj)
                                 :column-list columns-types
                                 :values (merge (all-colmns-nil columns-types)
                                                (into {} col-model))}))) model-data)))))

(defn insert-data 
  "Description:
    insert to db all columns from model-changes in state
  Attention:
    we can not add record (composite component) without main-model(simple columns, not composite)"
  ([state!]
   (let [{plugin-toolkit :plugin-toolkit
          model-changes  :model-changes} (state!)]
     (if-not (empty? model-changes)
       (let [grouped-model  (group-clms model-changes)
             fcomps-colmns  (get grouped-model true)
             sm-colmns      (get grouped-model nil) 
             id-insert      (if (empty? sm-colmns)
                              (do (i/info (gtool/get-lang-alerts :success)  "Model can not be empty, please enter at least one simple field"))
                              (:generated_key
                               (try (jdbc/execute! @jarman.logic.connection/*connection*
                                                   ((:insert-expression plugin-toolkit)
                                                    (ungrouping-model (state!) (apply hash-map
                                                                                      (apply concat sm-colmns))))
                                                   {:return-keys true})
                                    (catch Exception e (i/warning (gtool/get-lang-alerts :error) (.getMessage e))))))]
         (if-not (nil? id-insert)
           (update-comp-col state! id-insert fcomps-colmns))
         (println "INSERT MODEL CHANGES ___" model-changes ">>>" id-insert))))))

(defn update-data [state! dispatch!]
  (let [{plugin-toolkit :plugin-toolkit
         table-model    :model
         model-changes  :model-changes} (state!)]
    (if-not (empty? model-changes) 
      (let [grouped-model  (group-clms model-changes)
            fcomps-colmns  (get grouped-model true)
            sm-colmns      (get grouped-model nil)
            table-id (first (:model-columns plugin-toolkit))
            update-m (into {table-id (table-id table-model)} (ungrouping-model (state!) (apply hash-map
                                                                                               (apply concat sm-colmns))))]
        ((:update plugin-toolkit) update-m)
          (update-comp-col state! table-id fcomps-colmns)
          ;; (try
          ;;   ((:update plugin-toolkit) update-m)
          ;;   (update-comp-col state! table-id fcomps-colmns)
          ;;   (catch Exception e (popup/build-popup {:title "Warning" :size [300 200] :comp-fn (fn [] (c/label :text "Wrong data to update!"))})))
          (dispatch! {:action :refresh-state :value true})))))

(defn delete-data [state! dispatch!]
  (let [{plugin-toolkit :plugin-toolkit
         table-model    :model} (state!)
        meta-obj        (:meta-obj plugin-toolkit)
        table_name      (.return-table_name meta-obj)
        column-name     (keyword (str table_name  ".id"))
        to-delete       {column-name
                         (column-name table-model)}]
    (println "Run Delete:" ((:delete plugin-toolkit) to-delete))
    (doall (map (fn [[k v]] (if (mt/isComponentFiles? v) (.remove-data v table_name))) table-model))
    (dispatch! {:action :refresh-state :value true})
    ((:table-render (state!)))))

(defn default-buttons
  "Description:
     Create default buttons as insert, update, delete row.
     type - :insert, :update, :delete"
  [state! dispatch! type]
  (gcomp/button-basic
   (type {:insert "Insert new data"  :update "Update row"  :delete "Delete row"
          :export "Documents export" :changes "Form state" :clear "Clear form"})
   :font (gtool/getFont 13)
   :onClick (fn [e]
              (cond
                (= type :insert)
                (insert-data state!)
                (= type :clear) (dispatch! {:action :refresh-state :value true})
                (= type :update)
                (update-data state! dispatch!)
                (= type :delete)
                (delete-data state! dispatch!)
                (= type :changes)
                (do
                  (println "\nLooks on chages: " (:model-changes (state!)))
                  (gcomp/popup-info-window "Changes" (str (:model-changes (state!))) (state/state :app))))
              (if-not (= type :changes) ((:table-render (state!))))))) 

;; (defn get-missed-props
;;   "Description:
;;      Return not binded map, just cut this what exist.
;;    Example:
;;      (get-missed-key {:a a} {:a a :b c :d e}) 
;;        => {:b c, :d e}"
;;   [binded-map orgin-map]
;;   (->> (map #(first %) orgin-map)
;;        (filter (fn [orgin-key] (not (in? (map #(first %) binded-map) orgin-key))))
;;        (into {} (map #(into {% (% orgin-map)})))))

;; (defn merge-binded-props
;;   "Description:
;;      Get map where are binded keys, get properties for component and create new map with properties.
;;    Example:
;;      (merge-binded-props {:title \"mytitle\" :value \"pepe\"} {:title :custom-key}) 
;;        => {:custom-key \"mytitle\" :value \"pepe\"}
;;    "
;;   [props-map binded-list]
;;   (let [binded (doall
;;                 (into {} (map #(let [orginal-key (first %)
;;                                      binded-key  (second %)]
;;                                  {binded-key (orginal-key props-map)})
;;                               binded-list)))]
;;     (into binded (get-missed-props binded props-map))))


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

(defn convert-key-to-component
  "Description
     Convert to component automaticly by keyword.
     key is an key from model in defview as :user.name."
  [state! dispatch! panel meta-field-information]
  (with-state
   (let [table-name      (.return-table_name table-meta)
         ;;  meta            (meta/.return-columns-join meta)
         field-qualified (:field-qualified meta-field-information)
         title           (:representation  meta-field-information)
         editable?       (:editable?       meta-field-information)
         comp-types      (:component-type  meta-field-information)
         val             (cond
                           (not (nil? (field-qualified (:model         (state!))))) (field-qualified (:model         (state!)))
                           (not (nil? (field-qualified (:model-changes (state!))))) (field-qualified (:model-changes (state!))))
         val             (if (mt/isComponent? val) val (str val))
         func            (fn [e]
                           (dispatch!
                            {:action :update-changes
                             :path   [(rift field-qualified :unqualifited)]
                             :value  (c/value (c/to-widget e))}))
         comp-func       (fn [e col-key]
                           (do (dispatch!
                                {:action :update-comps-changes
                                 ;; :state-update
                                 :compn-obj val
                                 :path   [(rift field-qualified :unqualifited)]
                                 :value ;; (assoc (key (:model-changes (state!))) col-key (c/value (c/to-widget e)))
                                 {col-key (c/value (c/to-widget e))}})))
         comp-func-save  (fn [e] (dispatch!
                                 {:action :download-comp
                                  :value (c/value (c/to-widget e))
                                  :v-obj val}))
         comp (gcomp/inpose-label
            title
            (cond
              (= mt/column-type-linking (first comp-types))
              (input-related-popup-table {:val val
                                          :state! state!
                                          :field-qualified field-qualified
                                          :dispatch! dispatch!})
             
              (or (= mt/column-type-data (first comp-types))
                 (= mt/column-type-datatime (first comp-types)))
              (calendar/state-input-calendar {:func func :val val})

              (= mt/column-comp-url (first comp-types))
              (ccomp/url-panel {:func comp-func
                                :val val})

              (= mt/column-comp-file (first comp-types))
              (ccomp/file-panel {:func comp-func
                                 :func-save comp-func-save
                                 :mode (:insert-mode (state!))
                                 :val val})

              (= mt/column-comp-ftp-file (first comp-types))
              (ccomp/ftp-panel {:func comp-func
                                :func-save comp-func-save
                                :mode (:insert-mode (state!))
                                :val val})
             
              (= mt/column-type-textarea (first comp-types))
              (gcomp/state-input-text-area {:func func :val val})

              (= mt/column-type-prop (first comp-types))
              (gedit/state-code-area {:func func :val val})

              (= mt/column-type-boolean (first comp-types))
              (gcomp/state-input-checkbox {:func func :val val})
             
              :else
              (gcomp/state-input-text {:func func :val val})))]
     (.add panel comp))))

(comment
  (with-test-environ [:seal :table :seal]
    (let [on-change
          (fn [field]
            (fn [v]
              (do (dispatch!
                   {:action :update-changes
                    :path   [(rift field :unqualifited)]
                    :value  v
                    :debug? true}))))
          on-downld
          (fn [field]
            (fn [v]
              (do (dispatch!
                   {:action :download-chuj
                    :path   [(rift field :unqualifited)]
                    :value  v
                    :debug? true}))))]
      (doto (c/frame
             :content
             (c/scrollable
              (seesaw.mig/mig-panel
               :background  face/c-compos-background-darker
               :constraints ["wrap 1" "0px[grow, fill]0px" "0px[fill, top]0px"]
               :border (b/empty-border :thickness 10)
               :items [[(seesaw.core/label :text "site" :font (gtool/getFont :bold 20))]
                       [(ccomp/url-panel :on-change (on-change :seal.site) :default {})]
                       [(seesaw.core/label :text "file" :font (gtool/getFont :bold 20))]
                       [(ccomp/file-panel :on-change (on-change :seal.file) :on-download (on-downld :seal.file) :default {} :selection-mode :load-nothing)]
                       ;; [(seesaw.core/label :text "ftpf" :font (gtool/getFont :bold 20))]
                       ;; [(ccomp/ftp-panel    {:on-change (on-change :seal.ftp-file) :on-download (on-downld :seal.ftp-file) :default {} :mode false ;;  (:insert-mode (state!))
                       ;;                       })]
                       ])
              ;; :vscroll false
              )
             :title "Jarman" :size [1000 :by 800])
        (.setLocationRelativeTo nil) c/pack! c/show!)))
  
  (with-test-environ [:seal :table :seal]
    (let [val nil
          comp-func
          (fn [e col-key]
            (do (dispatch!
                 {:action :update-comps-changes
                  ;; :state-update
                  :compn-obj val
                  :path   [:seal.ftp_file]
                  :value ;; (assoc (key (:model-changes (state!))) col-key (c/value (c/to-widget e)))
                  {col-key (c/value (c/to-widget e))}})))]
      (-> (doto (c/frame
                 :title "Jarman"
                 :size [1000 :by 800]
                 :content (ccomp/url-panel {:func comp-func
                                            :val val}))
            (.setLocationRelativeTo nil) c/pack! c/show!))))
  


  (with-test-environ [:seal :table :seal]
    (let [component-container
          (smig/mig-panel :constraints ["wrap 1" "0px[grow, fill]0px" "0px[fill, top]0px"]
                          :border (b/empty-border :thickness 10)
                          :items [[(c/label)]])]
      (convert-model-to-components-list state! dispatch! component-container (:model-insert plugin-config))))
  
  (view-manager/defview seal
    (table
     :id :seal
     :name "seal"
     :tables [:seal]
     :view-columns [:seal.seal_number :seal.datetime_of_use :seal.datetime_of_remove]
     :model-insert [:seal.seal_number               
                    :seal.datetime_of_use
                    :seal.datetime_of_remove
                    :seal.site
                    :seal.loc_file
                    :seal.ftp_file]
     :active-buttons [:insert :update :delete :clear :changes]
     :permission :ekka-all
     :dialog {}
     :actions {:upload-docs-to-db
               (fn [state! dispatch!] (println (-> (state!) :plugin-toolkit :meta-obj .return-table_name)))}
     :buttons [{:form-model :model-insert, 
                :action :upload-docs-to-db, 
                :title "Upload document"}]
     :query {:table_name :seal,
             :column
             [:#as_is
              :seal.id
              :seal.seal_number
              :seal.datetime_of_use
              :seal.datetime_of_remove
              :seal.site_name
              :seal.site_url
              :seal.file_name
              :seal.file
              :seal.ftp_file_name
              :seal.ftp_file_path]}))

  (view-manager/defview seal
    (table
     ;; Plugin configuration
     :id :seal
     :name :seal
     :permission :ekka-all
     ;; Meta/Model information 
     :tables          [:seal]
     :active-buttons  [:insert :update :delete :clear :changes]
     :model           [:seal.seal_number :seal.datetime_of_use :seal.datetime_of_remove :seal.site :seal.loc_file :seal.ftp_file]
     :dialogs {:user.id_permission [:permission :dialog-table :permission-table]}
     ;; Plugin-customization
     :custom-configs {:layout 3/10}
     :custom-actions {:upload-docs-to-db  (fn [state! dispatch!] (println (-> (state!) :plugin-toolkit :meta-obj .return-table_name)))}
     :custom-buttons [{:form-model :model-insert :action :upload-docs-to-db :title "Upload document"}]
     :custom-queries
     {:default
      {:table-columns [:seal.seal_number :seal.datetime_of_use :seal.datetime_of_remove]
       :table-query 
       {:table_name :seal,
        :column
        [:#as_is
         :seal.id
         :seal.seal_number
         :seal.datetime_of_use
         :seal.datetime_of_remove
         :seal.site_name
         :seal.site_url
         :seal.file_name
         :seal.file
         :seal.ftp_file_name
         :seal.ftp_file_path]}}}))
  
  (with-test-environ [:seal :table :seal]
    ;; (-> plugin-config :view-columns)
    (generate-custom-buttons state! dispatch! :model-insert)))

(defn group-m-vec-by-key
  "Description:
     Convert [{:x a :field-qualified b}{:d w :field-qualified f}] => {:b {:x a :field-qualified b} :f {:d w :field-qualified f}}"
  [coll] (into {} (doall (map (fn [m] {(keyword (:field-qualified m)) m}) coll))))

(defn convert-model-to-components-list
  "Description
     Switch fn to convert by map or keyword"
  [state! dispatch! panel model-defview]
  (with-state
   (let [;;
         ;; [{:field-qualified :user.login :description "login"... }
         ;;  {:field-qualified :user.password ... }
         ;;  {...}...]
         ;;
         meta-columns (.return-columns-join table-meta)
         ;;
         ;; For comfort of usage, group meta-table vector
         ;; by the :field-qualified keyword. 
         ;; Convert [{:field A ...} {:field B ...}]
         ;;        => {:A {:field A ...} :B {:field B ...}}
         ;;
         meta-columns-m (into {} (doall (map (fn [m] {(keyword (:field-qualified m)) m}) meta-columns)))]
     (doall (for [k-or-m model-defview
                  :let [meta-information (if (keyword? k-or-m) (k-or-m meta-columns-m) k-or-m)]]
              (cond
                (map?     k-or-m) (convert-map-to-component state! dispatch! panel meta-information)
                (keyword? k-or-m) (convert-key-to-component state! dispatch! panel meta-information)))))))

(defn meta-to-component [fm]
  (jarman.gui.gui-components2/border-panel
   :north (jarman.gui.gui-components2/label :value (get fm :representation))
   :south (jarman.gui.managment/transform-to-object (get fm :component-type))))

;; (comment
;;   (with-test-environ [:seal :table :seal]
;;     (let [component-container
;;           (smig/mig-panel :constraints ["wrap 1" "0px[grow, fill]0px" "0px[fill, top]0px"]
;;                           :border (b/empty-border :thickness 10)
;;                           :items [[(c/label)]])]
;;       (convert-model-to-components-list state! dispatch! component-container (:model-insert plugin-config))))
  
;;   (view-manager/defview seal
;;     (table
;;      :id :seal
;;      :name "seal"
;;      :tables [:seal]
;;      :view-columns [:seal.seal_number :seal.datetime_of_use :seal.datetime_of_remove]
;;      :model-insert [:seal.seal_number               
;;                     :seal.datetime_of_use
;;                     :seal.datetime_of_remove
;;                     :seal.site
;;                     :seal.loc_file
;;                     :seal.ftp_file]
;;      :active-buttons [:insert :update :delete :clear :changes]
;;      :permission :ekka-all
;;      :dialog {}
;;      :actions {:upload-docs-to-db
;;                (fn [state! dispatch!] (println (-> (state!) :plugin-toolkit :meta-obj .return-table_name)))}
;;      :buttons [{:form-model :model-insert, 
;;                 :action :upload-docs-to-db, 
;;                 :title "Upload document"}]
;;      :query {:table_name :seal,
;;              :column
;;              [:#as_is
;;               :seal.id
;;               :seal.seal_number
;;               :seal.datetime_of_use
;;               :seal.datetime_of_remove
;;               :seal.site_name
;;               :seal.site_url
;;               :seal.file_name
;;               :seal.file
;;               :seal.ftp_file_name
;;               :seal.ftp_file_path]}))
  
;;   (with-test-environ [:seal :table :seal]
;;     ;; (-> plugin-config :view-columns)
;;     (generate-custom-buttons state! dispatch! :model-insert))

;;   jjjjjjjjjjjjjjjjjjjjjjjjjjjjjjj

  
;;   (c/frame
;;    :content
;;    (c/scrollable
;;     (seesaw.mig/mig-panel
;;      :background  face/c-compos-background-darker
;;      :constraints ["wrap 1" "0px[grow, fill]0px" "0px[fill, top]0px"]
;;      :border (b/empty-border :thickness 10)
;;      :items [[(seesaw.core/label :text "site" :font (gtool/getFont :bold 20))]
;;              ]))
;;    :title "Jarman" :size [1000 :by 800])
  
;;   (with-test-environ [:seal :table :seal]
;;     (jarman.gui.managment/transform-to-object
;;      ({:type :jsgl-text
;;        :value "0"
;;        :font-size 14
;;        :char-limit 0
;;        :placeholder ""
;;        :border [10 10 5 5 2]
;;        :on-click (fn [e] e)
;;        ;; :border-color-unfocus face/c-underline
;;        ;; :border-color-focus face/c-underline-on-focus
;;        :start-underline nil
;;        :args []}))))

(comment 
 (let [components (jarman.gui.managment/system-Components-list-group-get)
      
       model-change
       (list (fn [m]
               (if (= (:field-qualified m) :seal.table_name)
                 (deep-merge-with (comp second list) m {:component-type {:on-change (fn [e] (str "CHUJ" (c/text e)))}})
                 m)))]
   (defn populate-dispatch! [meta-field]
     (let [{:keys [type] :as comp-meta} (get meta-field :component-type)]
       (reduce (fn [acc-m event-keyword]
                 (if (contains? comp-meta event-keyword)
                   (update-in acc-m [:component-type :on-change] (fn [f] (wrapp-dispatch! f meta-field))) acc-m))
               meta-field (get-in components [type :actions]))))
   (defn pre-populate-action! [meta-field]
     (reduce (fn [acc-m f] (f acc-m)) meta-field model-change))
   (quick-frame
    (->> [{:field :table_name
           :field-qualified :seal.table_name
           :representation "Table name"
           :component-type
           {:type :jsgl-text
            :value "0"
            :font-size 14
            :char-limit 0
            :placeholder ""
            :border [10 10 5 5 2]
            :on-change (fn [e] (c/text e))
            :start-underline nil
            :args []}}]
         (reduce
          (fn [acc field]
            (->> field
                 (pre-populate-action!)
                 (populate-dispatch!)
                 (conj acc))) [])))))

(defn convert-model-to-components-list2
  "Description
     Switch fn to convert by map or keyword"
  [state! dispatch! model-defview]
  (with-state
    (let [model-changes (or (get plugin-config :model-change) [])
          ;;
          ;; {:jsgl-text #<Component{...}
          ;;  :jsgl-selectbox #<Com...}
          ;;
          components (jarman.gui.managment/system-Components-list-group-get)
          ;;
          ;; [{:field-qualified :user.login :description "login"... }
          ;;  {:field-qualified :user.password ... }
          ;;  {...}...]
          ;;
          meta-columns (.return-columns-join table-meta)
          ;;
          ;; For comfort of usage, group meta-table vector
          ;; by the :field-qualified keyword. 
          ;; Convert [{:field A ...} {:field B ...}]
          ;;        => {:A {:field A ...} :B {:field B ...}}
          ;;
          ;; meta-columns-m (into {} (doall (map (fn [m] {(keyword (:field-qualified m)) m}) meta-columns)))
          meta-columns-m (group-by-apply (comp keyword :field-qualified) meta-columns :apply-group first)]
      ;; --------------------
      (wlet
       (doall (for [k-or-m model-defview]
                (let [meta-information (if (keyword? k-or-m) (k-or-m meta-columns-m) k-or-m)]
                  (-> meta-information
                      pre-populate-action!
                      populate-dispatch!
                      meta-to-component))))
       ;; -------------------
       ((populate-dispatch!
         (fn [meta-field]
           (let [{:keys [type] :as comp-meta} (get meta-field :component-type)]
             (reduce
              (fn [acc-m event-keyword]
                (if (contains? comp-meta event-keyword)
                  (update-in acc-m [:component-type event-keyword]
                             (fn [f]
                               (fn [e]
                                 (dispatch!
                                  {:debug? true
                                   :type :model-change
                                   :model-field (get meta-field :field-qualified)
                                   :model-value ((eval f) e)})))) acc-m))
              meta-field (get-in components [type :actions])))))
        (pre-populate-action!
         (fn [meta-field]
           (reduce (fn [acc-m f] (f acc-m)) meta-field model-changes))))))))
;; jjjjjjjjjjjjjjj
;; (with-test-environ [:seal :table :seal]
;;     ;; (-> plugin-config :view-columns)
;;   (let
;;       [;; Define strategy for generated form
;;        ;; fixme:aleks doc
;;        ;;  `:model-insert` - fixme:aleks doc
;;        ;;  `:model-update` - fixme:aleks doc
;;        current-model-stategy (if (or (:insert-mode (state!)) (nil? (:model-update plugin-config))) :model-insert :model-update)
;;        ;; return :field-qualified for fields, that should be used in some way.
;;        model-defview (current-model-stategy plugin-config)]
;;     (quick-frame
;;      (convert-model-to-components-list2 state! dispatch! model-defview))))






(defn choose-screen! [^javax.swing.JFrame frame ^Number screen-n]
  ;; S(Screen), F(Frame), H(Height), W(Width)
  ;; +------------------[screen-n]---
  ;; | 
  ;; |    V
  ;; |     x=S-X+(S-W/2)-(F-W/2)
  ;; |     y=F-H+(S-H/2)-(F-H/2)
  ;; |
  (where
   ((^java.awt.GraphicsEnvironment ge (java.awt.GraphicsEnvironment/getLocalGraphicsEnvironment))
    (^"[Ljava.awt.GraphicsDevice;" gd (.getScreenDevices ge))
    (screen screen-n if2 #(< -1 % (alength gd)) screen-n 0)
    (S-X (.. (aget gd screen) getDefaultConfiguration getBounds -x))
    (S-Y (.. frame getY))
    (S-H (.. (aget gd screen) getDefaultConfiguration getBounds -height))
    (S-W (.. (aget gd screen) getDefaultConfiguration getBounds -width))
    (F-H (.  frame getWidth))
    (F-W (.  frame getHeight))
    (relative-y S-H do #(- (/ % 2) (/ F-H 2)) do long)
    (relative-x S-W do #(- (/ % 2) (/ F-W 2)) do long))
   (doto frame
     (.setLocation
      (+ relative-x S-X)
      (+ relative-y S-Y)))))



(defn quick-frame [items]
 (-> 
  (c/frame
   :content
   (c/scrollable
    (seesaw.mig/mig-panel
     :background  face/c-compos-background-darker
     :constraints ["wrap 1" "0px[grow, fill]0px" "0px[fill, top]0px"]
     :border (b/empty-border :thickness 10)
     :items (mapv vector items)))
   :title "Jarman" :size [1000 :by 800])
  (c/pack!)
  (choose-screen! 1)
  (c/show!)))

;; (map
;;  (comp
;;   vector
;;   jarman.gui.managment/transform-to-object
;;   :component-type
;;   wrapp-into-state)
;;  [{:description "Table name",
;;    :private? false,
;;    :default-value nil,
;;    :editable? true,
;;    :field :table_name,
;;    :column-type [:varchar-120 :default :null],
;;    :component-type {:type :jsgl-text
;;                     :value "0"
;;                     :font-size 14
;;                     :char-limit 0
;;                     :placeholder ""
;;                     :border [10 10 5 5 2]
;;                     :on-click (fn [e] e)
;;                     :start-underline nil
;;                     :args []},
;;    :representation "Table name",
;;    :constructor-var nil,
;;    :field-qualified :seal.table_name}])

(defn generate-custom-buttons
  "Description:
     Get buttons and actions from defview and create clickable button."
  [state! dispatch! current-model]
  (with-state
   (wlet 
    (->> additional-buttons
         (filterv (fn [btn-model] (= current-model (:form-model btn-model))))
         (mapv    (fn [btn-model]
                    (custom-button-builder-fn
                     :title      (:title btn-model)
                     :action-key (:action btn-model)
                     :action-fn  (get plugin-actions (:action btn-model)))))
         (doall))
    ;; Settings
    ((additional-buttons (:buttons plugin-config))
     (plugin-actions     (:actions plugin-config))
     (custom-button-builder-fn
      (fn [& {:keys [title action-key action-fn]}]
        (if (fn? action-fn)
          (gcomp/button-basic title :onClick (fn [e] (action-fn state! dispatch!)))
          (throw
           (ex-info
            "Declared action in `%s` is not a function type" action-key
            {:type :table-config-error :plugin :table
             :message-head [:header :plugin-name]
             :message-body [:alerts :overriding-action-undef]})))))))))


(comment
  (with-test-environ [:seal :table :seal]
    ;; (-> plugin-config :view-columns)
    (generate-custom-buttons
     state! dispatch! :model-insert))
  
  (view-manager/defview seal
    (table
     :id :seal
     :name "seal"
     :tables [:seal]
     :view-columns [:seal.seal_number :seal.datetime_of_use :seal.datetime_of_remove]
     ;; :model-insert [:seal.seal_number :seal.datetime_of_use :seal.datetime_of_remove :seal.site :seal.loc_file :seal.ftp_file]
     :model-insert [{:field :seal_number
                     :field-qualified :seal.seal_number
                     :representation "Seal Number"
                     :component-type
                     {:type :jsgl-text
                      :value "0"
                      :font-size 14
                      :char-limit 0
                      :placeholder ""
                      :border [10 10 5 5 2]
                      :on-change (fn [e] (seesaw.core/text e))
                      :start-underline nil
                      :args []}}]
     :model-change []
     :active-buttons [:insert :update :delete :clear :changes]
     :permission :ekka-all
     :dialog {}
     :actions {:upload-docs-to-db
               (fn [state! dispatch!] (println (-> (state!) :plugin-toolkit :meta-obj .return-table_name)))}
     :buttons [{:form-model :model-insert, 
                :action :upload-docs-to-db, 
                :title "Upload document"}]
     :query {:table_name :seal,
             :column
             [:#as_is
              :seal.id
              :seal.seal_number
              :seal.datetime_of_use
              :seal.datetime_of_remove
              :seal.site_name
              :seal.site_url
              :seal.file_name
              :seal.file
              :seal.ftp_file_name
              :seal.ftp_file_path]}))
  
  (with-test-environ [:seal :table :seal]
    ;; (-> plugin-config :view-columns)
    (generate-custom-buttons
     state! dispatch! :model-insert))

  (with-test-environ [:seal :table :seal]
    ;; (-> plugin-config :view-columns)
    (:model-change plugin-config))
  )


;;;;;;;;;;;;;;;;;;
;; Form Builder ;;
;;;;;;;;;;;;;;;;;;

(defn- create-header
  "Description:
    Header in expand panel."
  [state!]
  (gmg/migrid :v "[grow, center]"
              [(c/label
                :text (:representation (.return-table (:meta-obj (:plugin-toolkit (state!))))) 
                :halign :center
                :font (gtool/getFont 15 :bold)
                :foreground face/c-foreground-title
                :border (b/compound-border (b/line-border :bottom 1 :color face/c-underline)
                                           (b/empty-border :top 10)))]))

(defn- custom-icon-bar
  [state! dispatch!
   & {:keys [more-front]}]
  (let [icos [{:icon-off (gs/icon GoogleMaterialDesignIcons/CLEAR_ALL face/c-icon)
               :icon-on  (gs/icon GoogleMaterialDesignIcons/CLEAR_ALL face/c-icon)
               :tip      "Clear state and form"
               :func     (fn [e]
                           ;; TO DO, try to fix, panel rentr two times, because we must change key :model for rerender
                           (let [model  (:model (state!))]
                             (dispatch! {:action :clear-state})
                             (dispatch! {:action :state-update :path [:model] :value model})))}
              {:icon-on  (gs/icon GoogleMaterialDesignIcons/SEARCH face/c-icon) 
               :tip      "Display state"
               :func     (fn [e] (gcomp/popup-info-window
                                  "Changes"
                                  (str (:model-changes (state!)))
                                  (state/state :app)))}
              {:icon-on  (gs/icon GoogleMaterialDesignIcons/AUTORENEW face/c-icon)
               :tip      "Refresh table"
               :func     (fn [e] ((:table-render (state!))))}]
        icos (if (nil? more-front) icos (concat more-front icos))]
    (gcomp/icon-bar
     :size 35
     :align :right
     :margin [5 0 10 10]
     :items icos)))

;; fixme: current-model
;; 1. fix logic. or add `:model-update` to req. params in configurations
(def build-input-form
  "Description:
     Marge all components to one form "
  (fn [state! dispatch!]
    (with-state
     (let[;; Configuration
          active-buttons (:active-buttons plugin-config) ;; [:update :delete]
          ;; Define strategy for generated form
          ;; fixme:aleks doc
          ;;  `:model-insert` - fixme:aleks doc
          ;;  `:model-update` - fixme:aleks doc
          current-model-stategy (if (or (:insert-mode (state!)) (nil? (:model-update plugin-config))) :model-insert :model-update)
          ;; return :field-qualified for fields, that should be used in some way.
          model-defview (current-model-stategy plugin-config)

          ;; ---------------------
          ;; Container for inputs, buttons, and another components
          component-container
          (smig/mig-panel :constraints ["wrap 1" "0px[grow, fill]0px" "0px[fill, top]0px"]
                          :border (b/empty-border :thickness 10)
                          :items [[(c/label)]])
         
          return-button
          (if (empty? (:model (state!)))
            [] [{:icon-off (gs/icon GoogleMaterialDesignIcons/MODE_EDIT face/c-icon)
                 :icon-on  (gs/icon GoogleMaterialDesignIcons/MODE_EDIT face/c-icon)
                 :tip      "Return to insert"
                 :func     (fn [e]
                             (dispatch! {:action :refresh-state :value true})
                             ((:table-render (state!))))}])
          components
          (filter-nil
           (flatten
            (list
             (gcomp/hr 5)
             (custom-icon-bar
              state! dispatch!
              :more-front return-button)
             (gcomp/hr 10)
             (generate-custom-buttons state! dispatch! current-model-stategy)
             (gcomp/hr 5)
             (if (:insert-mode (state!))
               (when (in? active-buttons :insert)
                 (default-buttons state! dispatch! :insert))
               [(when (in? active-buttons :update)
                  (default-buttons state! dispatch! :update))
                (when (in? active-buttons :delete)
                  (default-buttons state! dispatch! :delete))
                ;;(gcomp/button-basic "Back to Insert" :onClick (fn [e] (dispatch! {:action :set-model :value {}})))
                (gcomp/hr 10)
                (if (in? active-buttons :export)
                  (export-button state! dispatch!) nil)]))))]
       ;; Logic
       (convert-model-to-components-list state! dispatch! component-container model-defview)
       (when (seq components)
         (doall (map #(.add component-container %) components)))
       component-container))))

(comment
  (with-test-environ [:seal :table :seal]
    (generate-custom-buttons state! dispatch! :model-insert))

  (with-test-environ [:seal :table :seal]
    (build-input-form state! dispatch!)
    (let [model-defview (-> (state!) :plugin-config :model-insert)]
      (generate-custom-buttons state! dispatch! model-defview)))

  (with-test-environ [:seal :table :seal]
    (dispatch! {:action :update-changes, :path [:seal.seal_number], :value "seal seal_number"})
    (dispatch! {:action :update-changes, :path [:seal.datetime_of_use], :value "seal datetime_of_use"})
    (dispatch! {:action :update-changes, :path [:seal.datetime_of_remove], :value "seal datetime_of_remove"})
    (dispatch! {:action :update-changes, :path [:seal.site], :value "seal site"})
    (dispatch! {:action :update-changes, :path [:seal.loc_file], :value "seal loc_file"})
    (dispatch! {:action :update-changes, :path [:seal.ftp_file], :value "seal ftp_file"})
    (changes!)))

;; fixme:serhii `gtable/create-table`.
;; 1. Table should supply more then one event
;; 2. Consider to rewrite lambda-generation aproach
;; 3. fix dispatching shit on-action
(def build-plugin-gui
  "Description
     Prepare and merge complete big parts"
  (fn [state! dispatch!]
    (print-line "constructing layout of plugin.")
    (print-line "building left sidebar and right table view")
    (where
     (;; Certain layout GUI container
      (main-layout (smig/mig-panel :constraints ["" "0px[shrink 0, fill]0px[grow, fill]0px" "0px[grow, fill]0px"]))
      ;; Right side of plugin, which used by the table representation
      (table-container (c/vertical-panel))
      ;; -----------
      ;; Function render GUI table element
      ;; use only `:view-columns` and `:tables` parameters. 
      ;; which describe how to build view and from which columns
      (table-fn
       (fn []
         (print-line "build table gui component")
         ((:table (gtable/create-table (:plugin-config (state!)) (:plugin-toolkit (state!))))
          ;; supply function for on-select
          (fn [model-table]
            (dispatch! {:action :switch-insert-update :value false}) 
            (if-not (= false (:update-mode (:plugin-config (state!))))
              (dispatch! {:action :set-model
                          :value (reduce (fn [acc [k v]]
                                           (if (nil? v) (into acc {k ""})
                                               (into acc {k v}))) {} model-table)}))))))
      ;; ------------
      ;; Security container which should
      ;; to note user about bad implementation or 
      ;; configuration plugin configuration
      (table-render
       (fn []
         (print-line "try to render table")
         (try
           (c/config! table-container :items [(table-fn)])
           (catch Exception e
             (print-error e)
             (throw
              (ex-info
               "problem with table model: " (.getMessage e)
               {:type :table-model-error
                :plugin :table
                :message-head [:header :plugin-name]
                :message-body [:alerts :table-model-error]}))))))
      ;; -------------
      (main-layout
       (c/config!
        main-layout :items
        [;; Left 'side' , where button and UI components
         ;; should be after will built from autogeneration
         [(create-expand
           state!
           dispatch! 
           (fn []
             [
              (gcomp/min-scrollbox
               (gcomp/expand-form-panel
                main-layout
                ;; certain part of Left-side bar
                [(create-header state!)
                 (build-input-form state! dispatch!)]
                :icon-open (gs/icon GoogleMaterialDesignIcons/ARROW_BACK face/c-icon)) 
               :hscroll :never)]))]
         ;; Right part of layout, is Table view.
         [table-container]])))
      (table-render)
      (dispatch! {:action :table-render :value table-render})
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
(s/def ::bool   boolean?)

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

;; (s/def ::actions ;; Check patern [{:key (fn [])}]
;;   (s/and vector?
;;          (s/coll-of
;;           (s/and map?
;;                  (s/coll-of
;;                   (fn [[action-kwd function]]
;;                     (and (keyword? action-kwd) (fn? function))))))))
(s/def ::actions ;; Check patern {:key (fn [])...}
  (s/and
   map?
   (s/conformer
    (s/coll-of
     (fn [[action-kwd function]]
       (and (keyword? action-kwd) (fn? function)))))))

(s/def ::buttons ;; Check patern [{:form-model keyword? :action keyword? :title string?}]
  (s/and
   vector?
   (s/coll-of (fn [m-valid]
                (verify-types-in-map
                 {:form-model keyword?
                  :action keyword?
                  :title string?}
                 m-valid))))) ;; TODO: can we valid key value?

(comment
  ;; (s/check-asserts?) shuld be true.
  (s/conform ::form-model :model-insert)  ;; => :model-insert
  (s/conform ::form-model :chuj-bliat)    ;; => :clojure.spec.alpha/invalid
  (s/assert  ::form-model :model-insert)   ;; => :model-insert
  (s/assert  ::form-model :chuj-bliat)     ;; => ExceptionInfo should to be
  (s/assert  :plugin.table.table/buttons [{:suka 123}])
  (s/assert  :jarman.plugin.data-toolkit/tables "sdjaaa")
  (wlet
   (testing-function
    {:id :permission
     :name "sdjaaa"
     :plug-place [:#tables-view-plugin]
     :tables [:permission]
     :view-columns [:permission.permission_name :permission.configuration]
     :model-insert [:permission.permission_name :permission.configuration]
     :active-buttons [:insert :update :delete :clear :changes]
     :permission :ekka-all
     :actions {:some-action-keys (fn [state! dispatch!] "dufak")}
     :buttons []
     :query
     {:table_name :permission,
      :column
      [:#as_is
       :permission.id
       :permission.permission_name
       :permission.configuration]}})
   ((testing-function
     (jarman.plugin.plugin/generate-dynamic-spec
      "table"
      [[:id             {:spec [:jarman.plugin.spec/keyword :opt-un]}]
       [:debug          {:spec [:jarman.plugin.spec/bool :opt-un]}]
       [:name           {:spec [:jarman.plugin.spec/name :req-un]}]
       [:tables         {:spec [:jarman.plugin.data-toolkit/tables :req-un]}]
       [:view-columns   {:spec [:jarman.plugin.data-toolkit/view-columns :req-un]}]
       [:query          {:spec [:jarman.plugin.data-toolkit/query :req-un]}]
       [:model-insert   {:spec [:plugin.table.table/model-insert :req-un]}]
       [:model-update   {:spec [:plugin.table.table/model-update :opt-un]}]
       [:active-buttons {:spec [:plugin.table.table/active-buttons :req-un]}]
       [:actions        {:spec [:plugin.table.table/actions :opt-un]}]
       [:buttons        {:spec [:plugin.table.table/buttons :opt-un]}]])))))

;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; EXTERNAL INTERFEIS ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn table-toolkit-pipeline [configuration]
  (query-toolkit/data-toolkit-pipeline configuration {}))

;; (defn create-state-template [plugin-path global-configuration-getter]
;;   (atom {:plugin-path          plugin-path
;;          :plugin-global-config global-configuration-getter
;;          :plugin-config        (get-in (global-configuration-getter) (conj plugin-path :config)  {})
;;          :plugin-toolkit       (get-in (global-configuration-getter) (conj plugin-path :toolkit) {})
;;          :insert-mode          true
;;          :history              []
;;          :model                {}
;;          :model-changes        {}}))


(with-test-environ [:seal :table :seal]
  (dispatch! {:action :update-changes :path [:a] :value "A"})
  (dispatch! {:action :update-changes :path [:b] :value "B"})
  (dispatch! {:action :update-changes :path [:c] :value "C"})
  (dispatch! {:action :update-changes :path [:e] :value "E"})
  (state!))

;; (map
;;  (fn [z]
;;    (read-string (format "(dispatch! {:action :update-changes :path [%s] :value \"%s\"})" (str z) (clojure.string/replace (name z) #"\." " "))))
;;  [:seal.seal_number :seal.datetime_of_use :seal.datetime_of_remove :seal.site :seal.loc_file :seal.ftp_file])

;; (defn table-entry [plugin-path global-configuration]
;;   (let [state      (create-state-template plugin-path global-configuration)
;;         dispatch!  (create-dispatcher state)
;;         state!     (fn [& prop]
;;                      (cond (= :atom (first prop)) state
;;                            :else (deref state)))
;;         table-name (.return-table_name (:meta-obj (:plugin-toolkit (state!))))]
;;     (print-line (format "Open 'table' plugin for '%s'" table-name))
;;     (build-plugin-gui state! dispatch!)))

(defn table-entry [plugin-path]
  ;; (let [state      (atom (create-state-template plugin-path))
  ;;       dispatch!  (create-dispatcher state)
  ;;       state!     (fn [& [special-key]]
  ;;                    (case special-key
  ;;                      :obtain-atom state
  ;;                      (deref state)))]
  ;;   (print-header (format "Open 'table' plugin for '%s'" (->> (state!) :plugin-toolkit :meta-obj .return-table_name)))
  ;;   (build-plugin-gui state! dispatch!))
  )

(comment
  ;; lookup for all View plugins
  (jarman.logic.view-manager/plugin-paths)
  (def -test-table- (jarman.logic.view-manager/plugin-link [:seal :table :seal]))
  (.return-path -test-table-)
  (.return-entry -test-table-)
  (.return-permission -test-table-)
  (.return-title -test-table-)
  (.return-config -test-table-)
  (.return-toolkit -test-table-)
  (.exists? -test-table-)
  (table-entry (.return-path -test-table-))
  (table-entry [:seal :table :seal]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; PLUGIN CONFIG DECLARATION ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(register-custom-view-plugin
 :name 'table
 :description "Plugin allow to editing One table from database"
 :entry table-entry
 :toolkit table-toolkit-pipeline
 :spec-list
 [[:id
   {:spec [:jarman.plugins.spec/keyword :opt-un]
    :doc "Custom plugin ID"}]
  [:debug
   {:spec [:jarman.plugins.spec/bool :opt-un]
    :doc "Debug allow to control output of testable plugin"}]
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
   {:spec [:plugin.table.table/model-insert :req-un],
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
   {:spec [:plugin.table.table/model-update :opt-un],
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
   {:spec [:plugin.table.table/active-buttons :req-un]
    :doc "Select buttons who should be display."
    :examples ":active-buttons [:insert :update :delete :clear :changes]"}]
  [:actions
   {:spec [:plugin.table.table/actions :opt-un],
    :doc "Realise additional logic to standart CRUD operation. Set key as id and some fn with state as arg.
          \"{:some-action-keys (fn [state! dispatch!]...)
          :some-another.... }\""}]
  [:buttons
   {:spec [:plugin.table.table/buttons :opt-un],
    :examples "[{:form-model :model-insert, 
                 :action :upload-docs-to-db, 
                 :title \"Upload document\"}
               {:form-model :model-update...}...]"
    :doc "This is an vector of optional buttons which do some logic bainded by acition key, discribed in `:action`"}]])

