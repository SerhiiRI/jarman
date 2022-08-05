;; -*- mode: clojure; mode: rainbow;  mode: yafolding; -*-
;;  _____  _    ____  _     _____
;; |_   _|/ \  | __ )| |   | ____|
;;   | | / _ \ |  _ \| |   |  _|
;;   | |/ ___ \| |_) | |___| |___
;;   |_/_/   \_\____/|_____|_____|
;;
;; -------------------------------
;; Term description
;; `model` - is vector of `metafield`, or `metafield-uuid`.
;;     In case of first declaration user implement metafield in model place.
;;     If user use metafiled-uuid instead, all meta will taked from cache of
;;     big metadata table.
;; `metafield-uuid` - is unique name for metadata column which represent column in database
;; `metafield` - one column in metadata, which represent one or more column's in database
;;     Example:
;;      {:field :seal_number     .---------------`metafield-uuid`
;;                              V
;;       :field-qualified :seal.datetime_of_remove
;;       :representation "Seal Number"
;;           .------------------------------`ui-component`
;;          V
;;       :component {:type :jsgl-text  <-------- `component-id`
;;                   :value ""
;;                   :font-size 14
;;                   :char-limit 0
;;                   :placeholder ""
;;                   :border [10 10 5 5 2]       .`action-event`
;;                        .---------------------'
;;                       V
;;                   :on-change (fn [e] (seesaw.core/text e))
;;                   :start-underline nil
;;                   :args []}}
;; `ui-component` - map structure that describe UI component in low-level way as posible
;; `action-event` - this lamda work in low-level component layer. In moment when metafield
;;     transforming to the real UI component, every :on-<action> event cover over the
;;     `dispatch` function which send result of event to global state Map.
;;
(ns plugin.table.table
  (:require
   ;; Clojure toolkit
   ;; [clojure.data :as data], [clojure.string :as string], [clojure.spec.alpha :as s]
   [clojure.pprint :refer [cl-format]]
   [clojure.java.jdbc :as jdbc]
   [clojure.data.csv]
   [dk.ative.docjure.spreadsheet :as excel]
   [clojure.java.io]
   ;; Seesaw components
   [seesaw.core    :as c]
   [seesaw.mig     :as smig]
   [seesaw.border  :as b]
   ;; Jarman styles
   [jarman.faces                :as face]
   ;; Jarman tools
   [jarman.lang :refer :all]
   [jarman.org  :refer :all]
   ;; [jarman.tools.swing :as stool]
   ;; [jarman.resource-lib.icon-library :as icon]
   ;; [jarman.gui.gui-style      :as gs]
   [jarman.gui.core          :refer [satom register! cursor fe]]
   [jarman.gui.gui-tools        :as gtool]
   [jarman.gui.gui-components   :as gcomp] ;; should be deprecated
   [jarman.gui.gui-components2  :as gui-component]
   [jarman.gui.components.swing :as swing]
   [jarman.gui.components.error :as error]
   [jarman.gui.components.swing-context :as swing-context]
   [jarman.gui.components.swing-keyboards :as swing-keyboards]
   [jarman.gui.components.component-reciever :as reciever]
   [jarman.logic.metadata   :as metadata]
   [jarman.logic.sql-tool   :as sql-tool]
   [jarman.logic.connection :as db]
   [jarman.logic.view-manager :as view-manager]
   ;; Jarman interaction
   [jarman.interaction :as interaction]
   ;; Local imports
   [plugins.table.helpers :as helpers]
   [plugins.table.table-socket :as table-socket])
  #_(:import
     (jiconfont.icons.google_material_design_icons GoogleMaterialDesignIcons)))

;; state actions
(declare create-dispatcher)
(declare action-handler)
;; meta transformers
(declare meta-to-component)
(declare metafield->swing-component)
(declare metacomponent-with-action->swing-component)
;; components
(declare table-ui-expand-metafield)

(def ^:dynamic *active-state* nil)
(def ^:dynamic *active-dispatch* nil)
(defn active-state! [] *active-state*)
(defn active-dispatch! [] *active-dispatch*)
(defmacro with-active-state! [& body]
  `(binding [*active-state* ~'state!
             *active-dispatch* ~'dispatch!]
     ~@body))

;;;;;;;;;;;;;;;;;;;;;;;;
;;; STATE AND EVENTS ;;;
;;;;;;;;;;;;;;;;;;;;;;;;

(defn create-state-template [plugin-path]
  (let [plugin  (view-manager/plugin-link plugin-path)
        toolkit (.return-toolkit plugin)
        config  (.return-config  plugin)]
    ;; ---- Guards ----
    (when-not (.exists? plugin)
      (throw (ex-info "Plugin by the link doesn't exist"
              {:plugin-link plugin-path})))
    (when (nil? config)
      (throw (ex-info "Empty plugin config"
              {:type :empty-plugin-config
               :plugin :table
               :message-head [:header :plugin-name]
               :message-body [:alerts :empty-plugin-config]})))
    (when (nil? toolkit)
      (throw (ex-info "Empty plugin toolkit"
              {:type :empty-plugin-toolkit :plugin :table
               :message-head [:header :plugin-name]
               :message-body [:alerts :empty-plugin-toolkit]})))
    ;; Structure of State atom
    {:component-router  {}
     ;; --------------------------
     :plugin-path       plugin-path
     :plugin-config     config
     :plugin-toolkit    toolkit
     ;; --------------------------
     :plugin-ui-mode    {:mode (or (first (first (get ~'config :model-configurations))) :insert) :time (quick-timestamp)}
     :model-selected    {}
     :model-changes     {}}))

(defmacro with-state [& body]
  `(if ~'state!
     (let [{~'plugin-path       :plugin-path
            ~'plugin-config     :plugin-config
            ~'plugin-toolkit    :plugin-toolkit
            ~'plugin-ui-mode    :plugin-ui-mode
            ~'model-selected    :model-selected
            ~'model-changes     :model-changes}  (~'state!)
           ~'dispatch!                           (~'state! {:action :dispatch})
           ~'table-meta (:meta-obj ~'plugin-toolkit)]
       ~@body)
     (throw (ex-info "Internal error, couldn't find `state!`"
              {:type :empty-plugin-toolkit :plugin :table
               :message-head [:header :plugin-name]
               :message-body [:alerts :empty-plugin-toolkit]}))))

(defmacro with-external-plugin [plugin-path & body]
  `(do
     (when-not (.exists? (jarman.logic.view-manager/plugin-link ~plugin-path))
       (throw (ex-info "Plugin by the link doesn't exist" {:plugin-link ~plugin-path})))
     (let [~'plugin (jarman.logic.view-manager/plugin-link ~plugin-path)
           ~'plugin-toolkit (.return-toolkit ~'plugin)
           ~'plugin-config  (.return-config  ~'plugin)
           ~'table-meta     (get ~'plugin-toolkit :meta-obj)]
       ~@body)))

(defn create-state-connector [central-satom]
  (fn
    ([] (deref central-satom))
    ([{:keys [action path]}]
     (case action
       :atom central-satom
       :cursor (cursor path central-satom)
       :dispatch (create-dispatcher central-satom)))))

(defmacro with-test-environ [plugin-path & body]
  `(let [~'plugin  (jarman.logic.view-manager/plugin-link ~plugin-path)
         ~'toolkit (.return-toolkit ~'plugin)
         ~'config  (.return-config  ~'plugin)]
     (when-not (.exists? (jarman.logic.view-manager/plugin-link ~plugin-path))
       (throw (ex-info "Plugin by the link doesn't exist" {:plugin-link ~plugin-path})))
     (let [~'plugin-path       ~plugin-path
           ~'plugin-config     ~'config
           ~'plugin-toolkit    ~'toolkit
           ~'table-meta        (:meta-obj ~'toolkit)
           ~'state
           (satom
            {:component-router  {}
             :plugin-path       ~plugin-path
             :plugin-config     ~'config
             :plugin-toolkit    ~'toolkit
             :plugin-ui-mode    {:mode (or (first (first (get ~'config :model-configurations))) :insert) :time (quick-timestamp)}
             :model-selected    {}
             :model-changes     {}})
           ~'state!    (create-state-connector ~'state)
           ~'dispatch! (~'state! {:action :dispatch})
           ~'changes!  (fn [] (:model-changes (~'state!)))
           ~'selected! (fn [] (:model-selected (~'state!)))]
       ~@body)))

;;;;;;;;;;;;;;
;;; HELPER ;;;
;;;;;;;;;;;;;;

(defn- help-return-id-column-for-table [^jarman.logic.metadata_core.TableMetadata table-meta]
  (keyword (format "%s.id" (.return-table_name table-meta))))

;;;;;;;;;;;;;;;;;;;;
;;; EVENT SYSTEM ;;;
;;;;;;;;;;;;;;;;;;;;

(declare jack-in-socket)
(declare action-handler-route-invoker)
(declare event-switch-plugin-ui-mode)
(declare event-pprint-state)
(declare table-export-csv)
(declare table-export-excel)

(defn action-handler [state payload]
  (where
   ((action (get payload :action nil))
    (route  (get payload :route nil))
    (debug? (get payload :debug? false)))
   (when debug? (print-line
                 (cl-format nil ";; --- DISPATCH! ~A ~A ~%;; ~A" action (quick-timestamp)
                   (pr-str
                     (-> payload
                       (dissoc :debug?)
                       (update :model-value (fn [v] (if-not (instance? Exception v) v
                                                     (cl-format nil "EXCEPTION: ~A"
                                                       (get (ex-data v) :message-body))))))))))
   (if route
     (action-handler-route-invoker route state payload)
     (case action
       :jack-in-socket        (jack-in-socket state payload)
       :table-export-csv      (table-export-csv state payload)
       :table-export-excel    (table-export-excel state payload)
       ;; ---
       :set-model-changes     (assoc-in state [:model-changes]  (get payload :value))
       :set-model-selected    (assoc-in state [:model-selected] (get payload :value))
       :update-model-changes  (assoc-in state [:model-changes  (get payload :model-field)] (get payload :model-value))
       :update-model-selected (assoc-in state [:model-selected (get payload :model-field)] (get payload :model-value))
       :reset-model-changes   (assoc-in state [:model-changes]  {})
       :reset-model-selected  (assoc-in state [:model-selected] {})
       :switch-plugin-ui-mode (event-switch-plugin-ui-mode state payload)
       ;; ---
       :pprint-state          (event-pprint-state state)))))

(defn action-handler-route-invoker [route state payload]
  (cond
    ;; -----------------
    (keyword? route)
    (let [{:keys [socket receiver]} (get-in state [:component-router route])]
      (if receiver
        (receiver (-> payload (dissoc :route)))
        (print-line (format "Action handler doesn't know `%s` route connection" (str route)))))
    ;; -----------------
    (fn? route)
    (if-let [socket-receiver-list (not-empty (map :receiver (route (vals (get-in state [:component-router])))))]
      (doall (map (fn [f] (f payload)) socket-receiver-list))
      (print-line (format "Action handler. apply 'route' fn return empty sockets" (str route))))
    ;; -----------------
    :else (print-line (format "Action handler. Undefinied type of router `%s`" (str route))))
  state)

(defn- jack-in-socket [state {:keys [socket]}]
  (assert (some? socket))
  (let [s-id (socket :socket-id)]
    (update state :component-router
            assoc s-id {:socket socket
                        :receiver (socket :socket-receiver)})))

(defn table-model-by-metadata-information
  [^clojure.lang.PersistentVector model-tables-v
   ^clojure.lang.PersistentVector model-field-qualified-v]
  (as-> model-tables-v MX
    (apply metadata/return-metadata MX)
    (mapcat (fn [M] (-> M  metadata/->TableMetadata .return-columns-flatten)) MX)
    (map    (fn [M] (-> M .to-primive)) MX)
    (group-by-apply :field-qualified MX :apply-group first)
    (for [col-qualified model-field-qualified-v
          :let [col-metadata (if (map? col-qualified) col-qualified (get MX col-qualified nil))]]
      (do (assert (some? col-metadata) (cl-format nil "Column `~A` not exist in any of selected tables: ~{`~A`~^, ~}."  col-qualified model-tables-v))
          {:key  (:field-qualified col-metadata)
           :text (:representation col-metadata)}))))

(defn- table-export-csv [state {:keys [socket-id]}]
  (assert (some? socket-id))
  (let [component-router (get state :component-router)
        socket (get-in state [:component-router socket-id :socket])]
    (print-header "Export table in CSV"
     (if-let [storage (socket :socket-storage)]
       (let [msg-header "Table header"
             query               (get storage :socket-refreshable-query)
             csv-custom-renderer (eval (get storage :csv-custom-renderer []))
             tables              (get storage :tables)
             columns             (get storage :columns)]
         (when (empty? columns)
           (throw (ex-info (format "%s. `:columns` are empty" msg-header) {})))
         (when (empty? tables)
           (throw (ex-info (format "%s. `:tables` are empty" msg-header) {})))
         (when (nil? query)
           (throw (ex-info (format "%s. `:socket-refreshable-query` are empty" msg-header) {})))
         (let [result (-> query (sql-tool/select!) (db/query))
               file-name (format "%s.csv" (get-in state [:plugin-config :name] "export"))]
           (if-let [f-path (gui-component/file-dialog :file file-name :title "Choose input file" :mode :save)]
             (with-open [writer (clojure.java.io/writer f-path)]
               (clojure.data.csv/write-csv writer
                 (conj
                   (map
                     (fn [item]
                       (as-> item $
                         (reduce
                           (fn [acc-item [k r]]
                             (update acc-item k r))
                           $ csv-custom-renderer)
                         (select-keys $ columns)
                         (vals $)))
                     result)
                   ((apply juxt columns)
                    (group-by-apply :key (table-model-by-metadata-information tables columns)
                      :apply-group first
                      :apply-item :text))))
               (print-line (format "file `%s` was created" file-name)))
             (print-line "CSV exporting is cencelled"))))))) state)

(defn- table-export-excel [state {:keys [socket-id]}]
  (assert (some? socket-id))
  (let [component-router (get state :component-router)
        socket (get-in state [:component-router socket-id :socket])]
    (print-header "Export table in XLSX"
     (if-let [storage (socket :socket-storage)]
       (let [msg-header "Table header"
             query               (get storage :socket-refreshable-query)
             exl-custom-renderer (eval (get storage :excel-custom-renderer []))
             tables              (get storage :tables)
             columns             (get storage :columns)]
         (when (empty? columns)
           (throw (ex-info (format "%s. `:columns` are empty" msg-header) {})))
         (when (empty? tables)
           (throw (ex-info (format "%s. `:tables` are empty" msg-header) {})))
         (when (nil? query)
           (throw (ex-info (format "%s. `:socket-refreshable-query` are empty" msg-header) {})))
         (let [file-name (format "%s.xlsx" (get-in state [:plugin-config :name] "export"))]
           (if-let [f-path (gui-component/file-dialog :file file-name :title "Choose input file" :mode :save)]
             (let [result (-> query (sql-tool/select!) (db/query))
                   wb (excel/create-workbook (get-in state [:plugin-config :name] "Export")
                        (conj
                          (map
                            (fn [item]
                              (as-> item $
                                (reduce
                                  (fn [acc-item [k r]]
                                    (update acc-item k r))
                                  $ exl-custom-renderer)
                                (select-keys $ columns)
                                (vals $)))
                            result)
                          ((apply juxt columns)
                           (group-by-apply :key (table-model-by-metadata-information tables columns)
                             :apply-group first
                             :apply-item :text))))
                   sheet (excel/select-sheet (get-in state [:plugin-config :name] "Export") wb)
                   header-row (first (excel/row-seq sheet))]
               (excel/set-row-style! header-row (excel/create-cell-style! wb {:background :yellow, :font {:bold true}}))
               (excel/save-workbook! f-path wb)
               (print-line (format "file `%s` was created" file-name))))))))) state)

;; fixme: alert message
(defn- event-switch-plugin-ui-mode [state {:keys [plugin-ui-mode model erease-selected?] :or {model {} erease-selected? false}}]
  (if (#{:insert, :update, :export} plugin-ui-mode)
    (do
      (if (and (#{:update} plugin-ui-mode) (empty? model) (empty? (:model-selected state)))
        (do (c/alert "event-switch-plugin-ui-mode: cannot switch, because selected element not exist")
            (assoc state :plugin-ui-mode {:mode :insert :time (get-in state [:plugin-ui-mode :mode])}))
        (case plugin-ui-mode
          ;; ----------
          :insert
          (-> state
              (assoc :plugin-ui-mode {:mode :insert :time (quick-timestamp)})
              (assoc :model-changes model)
              (update :model-selected (fn [m] (if erease-selected? model m))))
          ;; ----------
          :update
          (let [m (or (not-empty model) (not-empty (:model-selected state)))]
           (if m
             (-> state
                 (assoc :plugin-ui-mode {:mode :update :time (quick-timestamp)})
                 (assoc :model-selected m)
                 (assoc :model-changes  m))
             (do
               (print-line "event-switch-plugin-ui-mode: empty model")
               state)))
          ;; ----------
          :export
          (-> state
              (assoc :plugin-ui-mode {:mode :export :time (quick-timestamp)})))))
    (do
      (throw (Exception. (format "event-switch-plugin-ui-mode: unknown `%s` mode " plugin-ui-mode)))
      state)))

(defn- event-pprint-state [state]
  (print-header
   "STATE DUMP"
   (print-src
    "clojure"
    (with-out-str
      (clojure.pprint/pprint (dissoc state :plugin-path :plugin-config :plugin-toolkit :dispatch-history)))))
  state)

(defn- create-dispatcher [atom-var]
  (fn [action-m]
    (swap! atom-var (fn [state] (action-handler state action-m)))))

;;;;;;;;;;;;;;;;;;;;;;;;
;;; METADATA BUILDER ;;;
;;;;;;;;;;;;;;;;;;;;;;;;

(defn- meta-to-component
  ([^clojure.lang.PersistentArrayMap metafield]
   (jarman.gui.gui-components2/border-panel
     :north (jarman.gui.gui-components2/label
              :value (get metafield :representation)
              :foreground "#696969"
              :border (swing/border {:b 3}))
     :south (-> (interaction/metacomponent->component (get metafield :component))
              (swing/wrapp-tooltip (swing/tooltip<-metadata metafield)))))
  ([^clojure.lang.PersistentArrayMap metafield component]
   (jarman.gui.gui-components2/border-panel
     :north (jarman.gui.gui-components2/label
              :value (get metafield :representation)
              :foreground "#696969"
              :border (swing/border {:b 3}))
     :south (-> component
              (swing/wrapp-tooltip (swing/tooltip<-metadata metafield))))))

(defn- guard-component!? [^clojure.lang.PersistentArrayMap metafield]
  (when-not (and (map?     (get-in metafield [:component]))
               (keyword? (get-in metafield [:component :type])))
    (throw (ex-info (format "Field %s metadata was corrupted" (get metafield :field-qualified))
                    {:metafield metafield})))
  metafield)

(defn- populate-dispatch! [dispatch! components meta-field]
  (let [{:keys [type] :as comp-meta} (get meta-field :component)]
    (reduce
      (fn [acc-m event-keyword]
        (if (contains? comp-meta event-keyword)
          (update-in acc-m [:component event-keyword]
            (fn [f]
              (fn [e]
                (do;; let [x (.getSource e)
                 ;;      f (javax.swing.SwingUtilities/getRoot x)
                 ;;      z (.getContentPane f)]
                  (swing-context/with-active-event e
                    (dispatch!
                      {:debug? true
                       :action :update-model-changes
                       :model-field (get meta-field :field-qualified)
                       :model-value (f e)})))))) acc-m))
      meta-field (get-in components [type :actions]))))

(comment
  ;; ---------
  (with-test-environ [:user :table :user]
    (swing/quick-frame
      [(build-layout state! dispatch!)]))
  ;; ---------
  )

(defn- populate-value! [model meta-field]
  (if (empty? (get meta-field [:columns]))
    ;; simple metafield
    (if-let [value (get model (get meta-field :field-qualified))]
      (assoc-in meta-field [:component :value] value)
      meta-field)
    ;; composite metafield
    (if-let [value (get model (get meta-field :field-qualified))]
      (let [meta-composite (metadata/to-field-composite meta-field)]
        (assoc-in meta-field [:component :value] value))
      (let [meta-composite (metadata/to-field-composite meta-field)]
        (assoc-in meta-field [:component :value]
          (->> (.return-columns meta-composite)
            (map (fn [f] {(.return-field-qualified f) (:value (.return-component f))}))
            (into {}) (.group meta-composite)))))))

(defn metafield->swing-component
  "Description
     Switch fn to convert by map or keyword"
  [state! dispatch! model-defview]
  (with-state
    (let [_ model-selected
          ;;
          ;; {:jsgl-text #<Component{...}
          ;;  :jsgl-selectbox #<Com...}
          ;;
          components (interaction/metacomponents-get)
          ;;
          ;; [{:field-qualified :jarman_user.login :description "login"... }
          ;;  {:field-qualified :jarman_user.password ... }
          ;;  {...}...]
          ;;
          meta-columns (map #(.to-primive %) (.return-columns table-meta))
          ;;
          ;; For comfort of usage, group meta-table vector
          ;; by the :field-qualified keyword.
          ;; Convert [{:field A ...} {:field B ...}]
          ;;        => {:A {:field A ...} :B {:field B ...}}
          ;;
          meta-columns-m (group-by-apply (comp keyword :field-qualified) meta-columns :apply-group first)]
      ;; --------------------
      ;; Brief description of logic sequense
      ;; for metafield transformation
      ;; 1. `guard-component!?` check if structurarly is enaugh for building component
      ;; 2. `pre-populate-action!` apply some transformation for metadata(for example replace or add some key)
      ;; 3. `populate-dispatch!` cover on-<action> over the dispatch functionality.
      ;; 4. `meta-to-component` in final step transform metadata to gui component.
      (doall
       (for [k-or-m-or-v model-defview]
         (where
           ;; -----
           ((meta-information
              (cond
                (map? k-or-m-or-v) (eval k-or-m-or-v)
                (keyword? k-or-m-or-v) (k-or-m-or-v meta-columns-m)
                (vector? k-or-m-or-v) (let [[kwd fn-applyers] k-or-m-or-v]
                                        (reduce (fn [acc f] ((eval f) acc)) (kwd meta-columns-m) fn-applyers))
                :else (throw (Exception. (format "In function `metafield->swing-component`. undefineid structure for `metafield`:" (str k-or-m-or-v))))))
            (component-type (get-in meta-information [:component :type])))
           ;; ------
           (with-active-state!
             (->> meta-information
               (guard-component!?)
               (populate-dispatch! dispatch! components)
               (populate-value! model-selected)
               (meta-to-component)))))))))

#_(comment
    (with-test-environ [:jarman_user :table :jarman_user]
      (dispatch! {:debug true :action :set-model-selected
                  :value
                  {:jarman_user.first_name "Ivan"
                   :jarman_user.last_name "Popov"}})
      (dispatch! {:debug true :action :set-model-changes
                  :value
                  {:jarman_user.first_name "Ivan"
                   :jarman_user.last_name "Popov"}})
      (println (:model-changes (state!)))
      (println (:model-selected (state!)))
      (swing/quick-frame
        (let [{:keys [fields additional]} (get-in plugin-config [:model-configurations :update])]
          (metafield->swing-component state! dispatch! fields)))))

#_(swing/quick-frame
    [(->
       (gui-component/text :value "SSSSSSSSSSSSSSSSSS")
       (swing/wrapp-tooltip "AAAAAAAAAAAAAAAAAAAAAAAAAAA")
       (swing-keyboards/wrapp-keymap
         (-> (swing-context/active-keymap)
           (swing-keyboards/define-key (swing-keyboards/kbd "C-h m") (fn [] (c/alert "SOME"))))))])

(defn metacomponent-with-action->swing-component
  "Description:
     Get buttons and actions from defview and create clickable button."
  [state! dispatch! additional-buttons]
  (with-state
    (where
      ((components (interaction/metacomponents-get))
       (plugin-actions (:actions plugin-config)))
      (wlet
        (->> additional-buttons
          (map populate-custom-action)
          (map interaction/metacomponent->component)
          (map panel-container)
          (doall))
        ((populate-custom-action
           (fn [metacomponent]
             (reduce
               (fn [acc-m event-keyword]
                 (if-not (contains? metacomponent event-keyword)
                   acc-m
                   (update acc-m event-keyword
                     (fn [action-multitype-fn]
                       (let [action-multitype-fn (eval action-multitype-fn)]
                         (cond
                           ;; ---
                           (keyword? action-multitype-fn)
                           (if-let [action-fn (get plugin-actions action-multitype-fn)]
                             (fn [e] (action-fn state! dispatch!))
                             (throw (ex-info (format "generate-custom-buttons: Action by id `%s` doesn't exist"
                                               action-multitype-fn) {})))
                           ;; ---
                           (ifn? action-multitype-fn)
                           (fn [e] (action-multitype-fn state! dispatch!))))))))
               metacomponent (get-in components [(:type metacomponent) :actions]))))
         (panel-container
           (fn [component]
             (gui-component/border-panel
               :border (swing/border {:t 5})
               :center component))))))))

;; (defn eval-or-exception [something-to-eval]
;;   `(try (eval something-to-eval)
;;         (catch Exception e
;;           (throw (ex-info "Error on compiling code part" {:code something-to-eval})))))

;;;;;;;;;;;;;;;;;;;;;;
;;; GUI components ;;;
;;;;;;;;;;;;;;;;;;;;;;

(defn dialot-expand-segment-panel
  [id & {:keys [table columns]}]
  (if-not id
    (gui-component/label :value "Empty")
    (let [{:keys [model run-sql-fn]} (helpers/build-select-simple-data-by-the-id table)]
      (c/label :text
        (clojure.pprint/cl-format nil "<html>~{~A~^~}</html>"
          (map (fn [[field value]] (str "<b>" field "</b><br>" " <p style='margin-left: 5px;'>" value "<p>"))
            (seq (clojure.set/rename-keys (if columns
                                            (select-keys (run-sql-fn id) columns)
                                            (run-sql-fn id)) model))))
        :border (swing/border
                  {:a 4}
                  {:a 1 :color "#999999"})))))

(defn- dialog-table [& {:keys [plugin-path on-select] :or {on-select identity}}]
  (with-external-plugin plugin-path
    (let [kwd-model-id-column (help-return-id-column-for-table table-meta)
          swing-dialog (if-let [event (swing-context/active-event)] 
                         (let [component (c/to-widget event)
                               l (.getLocationOnScreen component)]
                           (doto (seesaw.core/custom-dialog
                                   :modal? true :width 600 :height 400 :title "Select component")
                             (.setLocation l))))
          label-text (gui-component/label :halign :center :value (str "Return from " (-> table-meta .return-table :representation)))]
      ;; Add additional keyboard shourtcuts
      ;; ------------------------------
      (swing-context/with-keymap
        (-> (swing-context/active-keymap)
            (swing-keyboards/define-key (swing-keyboards/kbd "escape")
              (fn [] (on-select nil) (seesaw.core/return-from-dialog swing-dialog nil))))
        ;; ----------------------------
        (seesaw.core/config!
         swing-dialog
         :title "Chooser"
         :content
         (as-> (-> plugin-config :view-configurations eval
                 (update :data apply [])
                 (assoc  :on-select (fn [e]
                                      (on-select (get e kwd-model-id-column nil))
                                      (seesaw.core/return-from-dialog swing-dialog e)))
                 interaction/metacomponent->component
                 error/catch-error-panel) $
           (c/scrollable $ :hscroll :as-needed :vscroll :as-needed)
           (gui-component/border-panel :north label-text :center $)
           (swing-keyboards/wrapp-keymap $ (swing-context/active-keymap))
           (gcomp/min-scrollbox $ :hscroll :never :border nil))))
      (seesaw.core/show! swing-dialog))))

(defn dialog-external-table
  [& {:keys [value
             on-select
             expand-table
             expand-columns
             dialog-plugin-path]
      :or {on-select (fn [e] e)}}]
  {:pre [(some? dialog-plugin-path)]}
  (where
    ;; -----------------------------
    ;; SETTINGS
    ((lstate (satom {:value value}))
     (loading-title
       (fn []
         (if (nil? (:value (deref lstate)))
           (gtool/get-lang :basic :empty)
           (gtool/get-lang :basic :selected))))
     (loading-dialog
       (fn [e]
         (print-line "table-ui-expand-metafield: open dialog")
         (swing-context/with-active-event e
           (dialog-table
             :plugin-path dialog-plugin-path
             :on-select
             (fn [e]
               (swap! lstate assoc :value e)
               (on-select e)))))))
    ;; -----------------------------
    ;; GUI COMPOSE
    ;; (println "STATE:" (active-state!))
    (gui-component/vertical-panel
      :items
      [(gcomp/expand-input
         {:title (loading-title)
          :panel (dialot-expand-segment-panel value
                   :table expand-table
                   :columns expand-columns)
          :onClick loading-dialog})]
      :event-hook
      {:some-id
       {:atom lstate
        :hook (fn [panel a old-s new-s]
                (c/config! panel :items
                  [(gcomp/expand-input
                     {:title (loading-title)
                      :onClick loading-dialog
                      :panel (dialot-expand-segment-panel (:value new-s)
                               :table expand-table
                               :columns expand-columns)})]))}})))

(comment
  (swing/quick-frame
    [(c/label "DJSKJDSLKJDLSJLDJLSJLDLKDJSKLDL")
     (with-test-environ [:card :table :card]
       (with-active-state!
         (dialog-external-table
           :value nil
           :expand-table :user
           :expand-columns [:user.first_name :user.last_name]
           :dialog-plugin-path [:user :dialog-table :user-dialog]
           :on-select (fn [e] (println e)))))]))

;;;;;;;;;;;;;;;;;;
;; Form Builder ;;
;;;;;;;;;;;;;;;;;;

;; rework 
#_(defn- custom-icon-bar
  [state! dispatch! & {:keys [more-front]}]
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

(declare information-header)
(declare build-insert-layout)
(declare build-update-layout)
(declare build-export-layout)


(defn- information-header
  "Description:
    Header in expand panel."
  [state!]
  (with-state
    (gui-component/border-panel :center
      (gui-component/label-h3
        :value (->> table-meta .return-table :representation)
        :halign :left
        :foreground face/c-foreground-title
        :border
        (swing/border
          {:b 10 :t 10 :l 5 :r 5 :color nil}
          {:b 1 :t 1 :color face/c-green}
          {:b 3})))))

(defn- information-header
  "Description:
    Header in expand panel."
  [state!]
  (with-state
    (gui-component/border-panel :north
      (gui-component/label
        :hscroll :never
        :value
        (clojure.pprint/cl-format nil "<html>~{~A~^~}</html>"
          (map (fn [[field value]] (str "<b>" field "</b><br>" " <p style='margin-left: 10px;'>" value "<p>"))
            {"Tabelka" "<i>User</i>"
             ;; "Opis Intefejsu" "interfejs używany dla dodawania procowników do globalnego systemu rejestracji. Po tym etapie można przypisać użytkownika do karty"
             ;; "Instrukcja" "Poniżej zlokalizowane pola do obowiązkowego wypełniania. Informacje odnośnie można przeczytać podczas naprowadzenia myszy na pole"
             }))
        ;; :halign :left
        ;; :foreground face/c-foreground-title
        ;; :border
        ;; (swing/border
        ;;   {:b 10 :t 10 :l 5 :r 5 :color nil}          
        ;;   {:b 3})
        ))))

(defn build-insert-layout [state! dispatch! model-configuration]
  (with-state
    (let [{:keys [fields additional]} model-configuration]
      (where
       ((clean-button-on-click
         (fe [state! dispatch!]
            (do
              (print-line "build-insert-layout: clear")
              (dispatch! {:action :switch-plugin-ui-mode
                          :debug? true :plugin-ui-mode :insert
                          :model (metadata/convert_metadata->model
                                  (.return-table_name table-meta))}))))
        (components-from-metadata #(metafield->swing-component state! dispatch! fields))
        (addtional-buttons        (metacomponent-with-action->swing-component state! dispatch! additional))
        #_(insert-only-component    (metacomponent-with-action->swing-component
                                   state! dispatch!
                                   [{:type :jsgl-button
                                     :value "To defaults" :on-click clean-button-on-click}])))
       #_(gui-component/mig-panel
        :constraints ["wrap 1" "0px[grow, fill]0px" "0px[fill, top]0px"]
        :border (b/empty-border :thickness 10)
        :items (vec (concat [[(information-header state!)]]
                            (map vector (components-from-metadata))
                            [[(gcomp/hr 10)]]
                            (map vector addtional-buttons)
                            (comment
                             [[(gcomp/hr 10)]]
                             (map vector insert-only-component)))))
       ;; ---------------
       (gui-component/vertical-panel
         :border (swing/border {:a 5})
         :items (-> []
                  (conj (gui-component/border-panel
                          :center (information-header state!)
                          
                          :border (swing/border {:a 5}
                                    {:type :title :title "Information"
                                     :title-color "#Bebebe"
                                     :color "#Bebebe"})))
                  (conj (gui-component/border-panel
                          :center (gui-component/vertical-panel
                                    :items (components-from-metadata))
                          :border (swing/border {:a 5}
                                    {:type :title :title "Inputs"
                                     :title-color "#Bebebe"
                                     :color "#Bebebe"})))
                  (conj (gui-component/border-panel
                          :center (gui-component/vertical-panel
                                    :items addtional-buttons)
                          :border (swing/border {:a 5}
                                    {:type :title :title "Control"
                                     :title-color "#Bebebe"
                                     :color "#Bebebe"})))))))))

(comment
  ;; ---------
  (with-test-environ [:user :table :user]
    (swing/quick-frame
      [(build-layout state! dispatch!)
       (seesaw.widgets.rounded-label/rounded-label :border 5
         :background :darkgrey
         :text "I'm a rounded label")]))
  ;; ---------
  )

;; add expand segment which show selected element
(defn build-update-layout [state! dispatch! model-configuration]
  (with-state
   (let [{:keys [fields additional]} model-configuration]
     (where
       ((state (state! {:action :cursor :path [:model-selected]}))
        (components-from-metadata #(metafield->swing-component state! dispatch! fields))
        (input-panel (gui-component/vertical-panel :items []))
        (update-input-panel
          (fn [] (c/config! input-panel :items (components-from-metadata)) input-panel))
        (addtional-buttons
          (metacomponent-with-action->swing-component state! dispatch! additional))
        (update-only-component
          (metacomponent-with-action->swing-component
            state! dispatch!
            [{:type :jsgl-button
              :value "Reset Selected"
              :on-click (fn [state! dispatch!]
                          (print-line "build-update-layout: reset model-selected->model-changes")
                          (dispatch! {:debug? true :action :set-model-changes :value (get (state!) :model-selected)})
                          (update-input-panel))}]))
        (panel-content-fn
          #(-> []
             ;; (conj (gui-component/border-panel
             ;;        :center (information-header state!)
             ;;        :border (swing/border {:a 5}
             ;;                  {:type :title :title "Information"
             ;;                   :title-color "#Bebebe"
             ;;                   :color "#Bebebe"})))
            (conj (gui-component/border-panel
                    :center (update-input-panel)
                    :border (swing/border {:a 5}
                              {:type :title :title "Inputs"
                               :title-color "#Bebebe"
                               :color "#Bebebe"})))
            (conj (gui-component/border-panel
                    :center (gui-component/vertical-panel :items addtional-buttons)
                    :border (swing/border {:a 5}
                              {:type :title :title "Control"
                               :title-color "#Bebebe"
                               :color "#Bebebe"})))
            ;; (conj (gui-component/border-panel
            ;;         :center update-only-component
            ;;         :border (swing/border {:a 5}
            ;;                   {:type :title :title "Update only"
            ;;                    :title-color "#Bebebe"
            ;;                    :color "#Bebebe"})))
            )))
       #_(gui-component/mig-panel
         :constraints ["wrap 1" "0px[grow, fill]0px" "0px[fill, top]0px"]
         :border (b/empty-border :thickness 10)
         :items (panel-content-fn)
         :event-hook
         {:update-layout-renderer
          {:atom state
           :hook
           (fn [panel state-atom old-state new-state]
             (print-line (format "build-update-layout: update model-selected %s" (pr-str new-state)))
             (c/config! panel :items (panel-content-fn)))}})
       (gui-component/vertical-panel
          :border (swing/border {:a 5})
          :items (panel-content-fn)
          :event-hook
          {:update-layout-renderer
           {:atom state
            :hook
            (fn [panel state-atom old-state new-state]
              (print-line (format "build-update-layout: update model-selected %s" (pr-str new-state)))
              (c/config! panel :items (panel-content-fn)))}})))))

;; add expand segment which show selected element
(defn build-export-layout [state! dispatch! model-configuration]
  (with-state
    (let [{:keys [filters additional]} model-configuration
          components (interaction/metacomponents-get)]
      (let [state (satom {})]
        (register! state :some
          (fn [_ _ values]
            (print-line (format "dispatch! / main-table / Filters: %s" (str values)))
            (dispatch! {:route :main-table :action :filter :filter-list (vals values)})))
        (gui-component/vertical-panel
          :border (swing/border {:a 5})
          :items
          [(gui-component/border-panel
             :border (swing/border
                       {:a 5}
                       {:type :title :title "Fields"
                        :title-color "#Bebebe"
                        :color "#Bebebe"})
             :center
             (gui-component/vertical-panel :items
               (for [meta-information filters]
                 (with-active-state!
                   (->> meta-information
                     (guard-component!?)
                     (eval)
                     ((fn [{:keys [field-qualified] :as metafield}]
                        (update-in metafield [:component :on-change]
                          (fn [f] (fn [e]
                                   (let [s (f e)]
                                     (swap! state assoc field-qualified s)))))))
                     (meta-to-component))))))
           (gui-component/border-panel
             :border (swing/border
                       {:a 5}
                       {:type :title :title "Control"
                        :title-color "#Bebebe"
                        :color "#Bebebe"})
             :center
             (gui-component/vertical-panel :items
               (metacomponent-with-action->swing-component state! dispatch! additional)))])))))

(comment
  ;; ---------
  (with-test-environ [:registration :table :registration]
    (swing/quick-frame
      [(build-layout state! dispatch!)
       ]))
  (with-test-environ [:user :table :user]
    (swing/quick-frame
      [(build-layout state! dispatch!)]))
  ;; ---------
  )

(defn build-input-form [state! dispatch!]
  (with-state
    (where
     ((current-model-stategy (get-in (state!) [:plugin-ui-mode :mode]))
      (state (state! {:action :cursor :path [:plugin-ui-mode]}))
      (*-ui-layout-border (swing/border {:a 10}))
      (*-ui-layout-border-hover (swing/border {:a 9} {:type :stroke :thickness 1.0 :color "#696969" :pattern :stroke-line}))
      ;; ----------------------------------(0)----
      (selected-label #(gui-component/label :value % :font (swing/font face/f-mono-bold) :background "#F09090" :border *-ui-layout-border))
      (input-bar-button-list
        #(where
           ((inform-ui-layout-label
              (gui-component/label :value "MODE" :tooltip "Selected editor UI for modification"
                :font (swing/font face/f-mono-bold)
                :background "#C0ff3e"
                :border *-ui-layout-border))
            (insert-ui-layout-button
              (when (contains? (get plugin-config :model-configurations) :insert)
                (if (= % :insert)
                  (selected-label "insert")
                  (gui-component/button
                    :font (swing/font face/f-mono-bold)
                    :value "insert" :tooltip "Insert ui layout"
                    :border *-ui-layout-border
                    :border-focus *-ui-layout-border-hover
                    :on-click (fn [e] (dispatch! {:action :switch-plugin-ui-mode :plugin-ui-mode :insert :debug? true
                                                 :model (metadata/convert_metadata->model (.return-table_name table-meta))}))))))
            (update-ui-layout-button
              (when (contains? (get plugin-config :model-configurations) :update)
                (if (= % :update)
                  (selected-label "update")
                  (gui-component/button-stroke
                    :font (swing/font face/f-mono-bold)
                    :value "update" :tooltip "Update ui layout"
                    :border *-ui-layout-border
                    :border-focus *-ui-layout-border-hover
                    :on-click (fn [e] (dispatch! {:action :switch-plugin-ui-mode :plugin-ui-mode :update :debug? true}))))))
            (export-ui-layout-button
              (when (contains? (get plugin-config :model-configurations) :export)
                (if (= % :export)
                  (selected-label "export")
                  (gui-component/button-stroke
                    :font (swing/font face/f-mono-bold)
                    :value "export" :tooltip "Export ui layout"
                    :border *-ui-layout-border
                    :border-focus *-ui-layout-border-hover
                    :on-click (fn [e] (dispatch! {:action :switch-plugin-ui-mode :plugin-ui-mode :export :debug? true}))))))
            (print-state-debug
              (gui-component/button-stroke :value "PP!" :font (swing/font face/f-mono-bold) :on-click (fn [e] (dispatch! {:action :pprint-state}))
                :border *-ui-layout-border :border-focus *-ui-layout-border-hover)))
           (keep identity
             [inform-ui-layout-label
              insert-ui-layout-button
              update-ui-layout-button
              export-ui-layout-button
              print-state-debug])))
      ;; ----------------------------------(1)----
      (input-panel
       (gui-component/border-panel
         :center
         (case current-model-stategy
           :insert (build-insert-layout state! dispatch! (get-in plugin-config [:model-configurations current-model-stategy]))
           :update (build-update-layout state! dispatch! (get-in plugin-config [:model-configurations current-model-stategy]))
           :export (build-export-layout state! dispatch! (get-in plugin-config [:model-configurations current-model-stategy]))
           (throw (ex-info "Udefinied mode!" {:plugin-ui-mode current-model-stategy})))))
      (input-bar
       (gui-component/horizontal-panel
         :background "#ffffff"
         :border (swing/border {:b 2 :color "#A9a9a9"})
         :items (input-bar-button-list current-model-stategy))))
     (c/scrollable
      (gui-component/border-panel
        :center (gui-component/vertical-panel :items [input-panel])
        :north input-bar
        :event-hook
        {:panel-tab-switching-event
         {:atom state
          :hook
          (fn [panel state-atom old-state new-state]
            (case (:mode new-state)
              :insert (c/config! input-panel :center (build-insert-layout state! dispatch! (get-in plugin-config [:model-configurations (:mode new-state)])))
              :update (c/config! input-panel :center (build-update-layout state! dispatch! (get-in plugin-config [:model-configurations (:mode new-state)])))
              :export (c/config! input-panel :center (build-export-layout state! dispatch! (get-in plugin-config [:model-configurations (:mode new-state)])))
              (throw (ex-info "Udefinied mode!" {:plugin-ui-mode new-state})))
            (c/config! input-bar :items (input-bar-button-list (:mode new-state))))}})))))

(defn build-layout [state! dispatch!]
  (with-state
    (where
      ((update-table-config
         (as-> (plugin-config :view-configurations) *t
           ;; 1.
           (if (:custom-renderers *t)
             (update *t :custom-renderers eval) *t)
           ;; 2.
           (update *t :data (fn [data]
                              (if data
                                (eval data)
                                ((comp vec jarman.logic.connection/query jarman.logic.sql-tool/select!)
                                 (get *t :socket-refreshable-query)))))
           ;; 3.
           (if (nil? (get *t :on-select))
             (do (interaction/info "Table" "Empty `:on-select` event for table. Please select event") *t)
             (update *t :on-select (fn [f] (fn [e] (println "on-select event!")
                                            ((eval f) state! dispatch! e)))))))
       (table (interaction/metacomponent->component update-table-config))
       (scroll (-> table
                 (c/scrollable :hscroll :as-needed :vscroll :as-needed)
                 (swing/wrapp-adjustment-listener
                   (fn [current max]
                     (when (= max current)
                       (dispatch! {:route :main-table :action :load-more :loading-range 50}))))))
       (main-table-socket (table-socket/create-table-socket (:socket-id update-table-config) table update-table-config)))
      (dispatch! {:action :jack-in-socket :socket main-table-socket})
      (c/left-right-split
        (build-input-form state! dispatch!)
        (gui-component/border-panel :center scroll)
        :divider-location 300))))

(comment
  ;; ------- ;;
  ;; jjjjjjjjj
  (with-test-environ [:registration :table :registration]
    (swing/quick-frame
      [(build-layout state! dispatch!)]))
  ;; ------- ;;
  comment)

;;;;;;;;;;;;;;;;;;;;;;;;;
;;; PLUGIN DEFINITION ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;

(defn common-toolkit-pipeline [configuration]
  (wlet
   ;; ------------
   ;; Constructor
   (cond-> {}
     ;; Jack in TableMetadata Object if it exist
     table-metadata (assoc :meta-obj (metadata/->TableMetadata table-metadata)))
   ;; ------------
   ;; Settings
   ((table-metadata (some-> configuration (get :table_name) (metadata/return-metadata) (first))))))

(defn table-entry [plugin-path]
  (where
   ((state!    (create-state-connector (satom (create-state-template plugin-path))))
    (dispatch! (state! {:action :dispatch})))
   (print-header
    (format "Open 'table' plugin by path '%s'" ;; (->> (state!) :plugin-toolkit :meta-obj .return-table_name)
            (str plugin-path))
    (build-layout state! dispatch!))))

(defn dialog-table-entry [plugin-path]
  (print-header (format "Open 'dialog-table' plugin for '%s'" (str plugin-path))))

(interaction/register-metacomponent :id :dialog-external-table :component dialog-external-table :actions #{:on-select})

(jarman.interaction/register-view-plugin
 :name 'table
 :description "Plugin allow to editing One table from database"
 :entry table-entry
 :toolkit common-toolkit-pipeline
 :spec-list [])

(jarman.interaction/register-view-plugin
 :name 'dialog-table
 :description "Dialog table"
 :entry dialog-table-entry
 :toolkit common-toolkit-pipeline
 :spec-list [])

;;  _____ _____ ____ _____ _
;; |_   _| ____/ ___|_   _( )___
;;   | | |  _| \___ \ | | |// __|
;;   | | | |___ ___) || |   \__ \
;;   |_| |_____|____/ |_|   |___/
;;
(comment
  ;; DIALOG TABLE
  (swing/quick-frame
    [(gui-component/button :value "DEBUG"
       :on-click (fn [e]
                   (swing-context/with-active-event e
                     (dialog-table :plugin-path [:user :dialog-table :user-dialog]
                       :on-select (fn [e] (println "ID :+> "e))))))])
  ;;
  (with-test-environ [:jarman_user :table :jarman_user]
    (swing/quick-frame
     [(table-ui-expand-metafield
       state! dispatch!
       {:description nil,
        :private? false,
        :default-value nil,
        :editable? true,
        :field :id_jarman_profile,
        :column-type
        {:stype "bigint(120) unsigned",
         :tname :bigint,
         :tspec :unsigned,
         :tlimit 120,
         :tnull false,
         :textra "",
         :tdefault nil,
         :tkey "MUL"},
        :component {:type :jsgl-link
                    :plugin-path [:profile :dialog-table :profile-dialog]},
        :foreign-keys
        [{:id_jarman_profile :profile} {:delete :cascade, :update :cascade}],
        :representation "Id profile",
        :field-qualified :jarman_user.id_jarman_profile,
        :key-table :profile})]))
  ;; description panel
  (with-test-environ [:jarman_user :table :jarman_user]
    (swing/quick-frame [(relation-modal-helper-panel :profile 1)]))

  (with-test-environ [:jarman_user :table :jarman_user]
    (swing/quick-frame [(relation-modal-helper-panel :profile nil)]))

  (with-test-environ [:jarman_user :table :jarman_user]
    (dispatch! {:debug true :action :set-model-selected
                :value
                {:jarman_user.first_name "Ivan"
                 :jarman_user.last_name "Popov"}})
    (dispatch! {:debug true :action :set-model-changes
                :value
                {:jarman_user.first_name "Ivan"
                 :jarman_user.last_name "Popov"}})
    (println (:model-changes (state!)))
    (println (:model-selected (state!)))
    (swing/quick-frame
     (let [{:keys [fields additional]} (get-in plugin-config [:model-configurations :update])]
       (metafield->swing-component state! dispatch! fields))))

  (with-test-environ [:jarman_user :table :jarman_user]
    (dispatch! {:debug true :action :set-model-selected
                :value
                {:jarman_user.first_name "Ivan"
                 :jarman_user.last_name "Popov"}})
    (dispatch! {:debug true :action :set-model-changes
                :value
                {:jarman_user.first_name "Ivan"
                 :jarman_user.last_name "Popov"}})
    #_(dispatch! {:action :switch-plugin-ui-mode :plugin-ui-mode :update
                  :debug true
                  :model {:jarman_user.login "A", :jarman_user.password "B", :jarman_user.first_name "C", :jarman_user.last_name "A", :jarman_user.configuration "'{}'", :jarman_user.id_jarman_profile 2, :jarman_user.id 1}})
    (swing/quick-frame
      [(build-input-form state! dispatch!)]))
  
  (with-test-environ [:user :table :user]
    (swing/quick-frame
      [(build-layout state! dispatch!)]))

  (with-test-environ [:card :table :card]
    (swing/quick-frame
      [(build-layout state! dispatch!)]))

  (with-test-environ [:jarman_user :table :jarman_user]
    (swing/quick-frame
      [(build-layout state! dispatch!)]))

  (with-test-environ [:jarman_profile :table :jarman_profile]
    (swing/quick-frame
     [(build-layout state! dispatch!)]))

  (with-test-environ [:seal :table :seal]
    (swing/quick-frame
     [(build-layout state! dispatch!)]))

  (swing/quick-frame
   [(table-entry [:jarman_user :table :jarman_user])])

  (with-test-environ [:jarman_user :table :jarman_user]
    (swing/quick-frame
     [(meta-to-component
       {:field :date-label,
        :field-qualified :jarman_user.date-label
        :representation "Date label"
        :component {:type :jsgl-calendar-label,
                    :on-change (fn [e] (println e))
                    :value nil}})]))

  (with-test-environ [:seal :table :seal]
    (dispatch! {:debug true :action :update-model-changes :path [:a] :value "A"})
    (dispatch! {:debug true :action :update-model-changes :path [:b] :value "B"})
    (dispatch! {:debug true :action :update-model-changes :path [:c] :value "C"})
    (dispatch! {:debug true :action :update-model-changes :path [:e] :value "E"})
    (state!))

  (with-test-environ [:seal :table :seal]
    (dispatch! {:action :update-model-changes, :path [:seal.seal_number], :value "seal seal_number"})
    (dispatch! {:action :update-model-changes, :path [:seal.datetime_of_use], :value "seal datetime_of_use"})
    (dispatch! {:action :update-model-changes, :path [:seal.datetime_of_remove], :value "seal datetime_of_remove"})
    (dispatch! {:action :update-model-changes, :path [:seal.site], :value "seal site"})
    (dispatch! {:action :update-model-changes, :path [:seal.loc_file], :value "seal loc_file"})
    (dispatch! {:action :update-model-changes, :path [:seal.ftp_file], :value "seal ftp_file"})
    (changes!))

  ;; lookup for all View plugins
  (jarman.logic.view-manager/plugin-paths)
  (def -test-table- (jarman.logic.view-manager/plugin-link [:seal :table :seal]))
  (.exists? (jarman.logic.view-manager/plugin-link [:profile :dialog-table :profile-dialog]))
  (.return-path -test-table-)
  (.return-entry -test-table-)
  (.return-permission -test-table-)
  (.return-title -test-table-)
  (.return-config -test-table-)
  (.return-toolkit -test-table-)
  (.exists? -test-table-)
  (table-entry (.return-path -test-table-))
  (table-entry [:seal :table :seal]))
