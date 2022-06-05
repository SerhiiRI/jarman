;; -*- mode: clojure; mode: yafolding; -*-
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
   ;; Seesaw components
   [seesaw.core    :as c]
   [seesaw.mig     :as smig]
   [seesaw.border  :as b]
   ;; Jarman styles
   [jarman.faces                :as face]
   ;; Jarman tools
   [jarman.lang :refer :all]
   [jarman.tools.org  :refer :all]
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
     :plugin-ui-mode    {:mode :insert :time (quick-timestamp)}
     :model-selected    {}
     :model-changes     {}}))

(defmacro with-state [& body]
  `(let [{~'plugin-path       :plugin-path
          ~'plugin-config     :plugin-config
          ~'plugin-toolkit    :plugin-toolkit
          ~'plugin-ui-mode    :plugin-ui-mode
          ~'model-selected    :model-selected
          ~'model-changes     :model-changes}  (~'state!)
         ~'table-meta (:meta-obj ~'plugin-toolkit)]
     ~@body))

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
             :plugin-ui-mode    {:mode :insert :time (quick-timestamp)}
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

(defn action-handler [state payload]
  (where
   ((action (get payload :action nil))
    (route  (get payload :route nil))
    (debug? (get payload :debug? false)))
   (when debug? (print-line
                 (cl-format nil ";; --- DISPATCH! ~A ~A ~%;; ~A" action (quick-timestamp)
                            (pr-str (dissoc payload :debug?)))))
   (if route
     (action-handler-route-invoker route state payload)
     (case action
       :jack-in-socket        (jack-in-socket state payload)
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
;; fixme: alert message
(defn- event-switch-plugin-ui-mode [state {:keys [plugin-ui-mode model erease-selected?] :or {model {} erease-selected? false}}]
  (if (#{:insert, :update, :export} plugin-ui-mode)
    (do
      (if (and (#{:update :export} plugin-ui-mode) (empty? model) (empty? (:model-selected state)))
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
    :north (jarman.gui.gui-components2/label :value (get metafield :representation))
    :south (interaction/metacomponent->component (get metafield :component))))
  ([^clojure.lang.PersistentArrayMap metafield component]
   (jarman.gui.gui-components2/border-panel
    :north (jarman.gui.gui-components2/label :value (get metafield :representation))
    :south component)))

(defn- guard-component!? [^clojure.lang.PersistentArrayMap metafield]
  (when-not (and (map?     (get-in metafield [:component]))
               (keyword? (get-in metafield [:component :type])))
    (throw (ex-info (format "Field %s metadata was corrupted" (get metafield :field-qualified))
                    {:metafield metafield})))
  metafield)

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
          ;; [{:field-qualified :user.login :description "login"... }
          ;;  {:field-qualified :user.password ... }
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
      (wlet
       (doall (for [k-or-m-or-v model-defview]
                (let [meta-information (cond
                                         (map? k-or-m-or-v) k-or-m-or-v
                                         (keyword? k-or-m-or-v) (k-or-m-or-v meta-columns-m)
                                         (vector? k-or-m-or-v) (let [[kwd fn-applyers] k-or-m-or-v]
                                                                 (reduce (fn [acc f] ((eval f) acc)) (kwd meta-columns-m) fn-applyers))
                                         :else (throw (Exception. (format "metafield->swing-component: undefineid structure of metafield:" (str k-or-m-or-v)))))
                      component-type (get-in meta-information [:component :type])
                      fn-pipeline (cond
                                    ;; ------
                                    (= :jsgl-link component-type)
                                    [guard-component!? (partial table-ui-expand-metafield state! dispatch!)]
                                    ;; ------
                                    (some? (get-in components [component-type :constructor]))
                                    [guard-component!? populate-composite-value! populate-dispatch! meta-to-component]
                                    ;; ------
                                    :else
                                    [guard-component!? populate-value! populate-dispatch! meta-to-component])]
                  (reduce (fn [acc fn-meta-transformer]
                            (fn-meta-transformer acc)
                            ;; (catch Exception e
                            ;;   (jarman.tools.org/print-error e)
                            ;;   (jarman.tools.org/print-error "PROBLEM WITH =>" meta-information)
                            ;;   e)
                            )
                          meta-information fn-pipeline))))
       ;; -------------------
       ((pin-functions!
         (fn [meta-field]
           (let [{:keys [type] :as comp-meta} (get meta-field :component)]
             (reduce
              (fn [acc-m event-keyword]
                (if (contains? comp-meta event-keyword)
                  (update-in acc-m [:component event-keyword]
                             (fn [f] (eval f))) acc-m))
              meta-field (get-in components [type :actions])))))
        (populate-dispatch!
         (fn [meta-field]
           (let [{:keys [type] :as comp-meta} (get meta-field :component)]
             (reduce
              (fn [acc-m event-keyword]
                (if (contains? comp-meta event-keyword)
                  (update-in acc-m [:component event-keyword]
                             (fn [f]
                               (fn [e]
                                 (dispatch!
                                  {:debug? true
                                   :action :update-model-changes
                                   :model-field (get meta-field :field-qualified)
                                   :model-value ((if (fn? f) f (eval f)) e)})))) acc-m))
              meta-field (get-in components [type :actions])))))
        (populate-value!
         (fn [meta-field]
           (if-let [value (get model-changes (get meta-field :field-qualified))]
             (assoc-in meta-field [:component :value] value)
             meta-field)))
        (populate-composite-value!
         (fn [meta-field]
           (if-let [value (get model-selected (get meta-field :field-qualified))]
             (let [meta-composite (metadata/to-field-composite meta-field)]
               (assoc-in meta-field [:component :value] value))
             (let [meta-composite (metadata/to-field-composite meta-field)]
               (assoc-in meta-field [:component :value]
                         (->> (.return-columns meta-composite)
                              (map (fn [f] {(.return-field-qualified f) (:value (.return-component f))}))
                              (into {}) (.group meta-composite))))))))))))

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
           (doall))
      ((populate-custom-action
        (fn [metacomponent]
          (let [type (:type metacomponent)]
            (reduce
             (fn [acc-m event-keyword]
               (if (contains? metacomponent event-keyword)
                 (update acc-m event-keyword
                         (fn [action-multitype-fn]
                           (cond
                             ;; ---
                             (keyword? action-multitype-fn)
                             (if-let [action-fn (get plugin-actions action-multitype-fn)]
                               (fn [e] (action-fn state! dispatch!))
                               (throw (ex-info (format "generate-custom-buttons: Action by id `%s` doesn't exist"
                                                       action-multitype-fn) {})))
                             ;; ---
                             (list? action-multitype-fn)
                             (let [action-fn (eval action-multitype-fn)] (fn [e] (action-fn state! dispatch!)))
                             ;; ---
                             (ifn? action-multitype-fn)
                             (fn [e] (action-multitype-fn state! dispatch!)))))
                 acc-m))
             metacomponent (get-in components [type :actions]))))))))))

;;;;;;;;;;;;;;;;;;;;;;
;;; GUI components ;;;
;;;;;;;;;;;;;;;;;;;;;;

(defn relation-modal-helper-panel
  [table-name-kwd id]
  (if-not id
    (gui-component/label :value "Empty")
    (c/scrollable
     (let [{:keys [model run-sql-fn]} (helpers/build-select-simple-data-by-the-id table-name-kwd)
           rows (seq (clojure.set/rename-keys (run-sql-fn id) model))
           t (c/table :model [:columns [{:key :representation :text "FField"} {:key :value :text "VValue"}] :rows rows])]
       (c/config! t :show-horizontal-lines? false)
       (c/config! t :show-vertical-lines? false)
       t))))

(defn- dialog-table [& {:keys [plugin-path on-select] :or {on-select identity}}]
  (with-external-plugin plugin-path
    (let [kwd-model-id-column (help-return-id-column-for-table table-meta)
          swing-dialog (seesaw.core/custom-dialog :modal? true :width 600 :height 400 :title "Select component")
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

(defn table-ui-expand-metafield [state! dispatch! metafield]
  (where
   ;; -----------------------------
   ;; SETTINGS
   ((field-qualified (:field-qualified metafield))
    (key-table       (:key-table       metafield))
    (plugin-path     (-> metafield :component :plugin-path))
    (load-value-from-model
     (fn [] (get-in (state!) [:model-changes field-qualified])))
    (loading-title
     (fn []
       (if (nil? (load-value-from-model))
         (gtool/get-lang :basic :empty)
         (gtool/get-lang :basic :selected))))
    (loading-dialog
     (fn []
       (fn [e]
         (print-line "table-ui-expand-metafield: open dialog")
         (dialog-table
          :plugin-path plugin-path
          :on-select
          (fn [id-of-model]
            (dispatch!
             {:debug? true
              :action :update-model-changes
              :model-field field-qualified
              :model-value id-of-model})))))))
   ;; -----------------------------
   ;; TESTS
   (when-not (some? plugin-path)
     (throw (ex-info (format "Empty `:plugin-path` config for metafield `%s`" field-qualified) metafield)))
   ;; -----------------------------
   ;; GUI COMPOSE
   (jarman.gui.gui-components2/border-panel
    :north (jarman.gui.gui-components2/label :value (get metafield :representation))
    :south
    (gui-component/vertical-panel
     :event-hook-id :suka
     :event-hook-atom (state! {:action :cursor :path [:model-changes field-qualified]})
     :items
     [(gcomp/expand-input
       {:title (loading-title)
        :panel (relation-modal-helper-panel key-table (load-value-from-model))
        :onClick (loading-dialog)})]
     :event-hook
     (fn [panel a old-s new-s]
       (c/config! panel :items
                  [(gcomp/expand-input
                    {:title (loading-title)
                     :panel (relation-modal-helper-panel key-table new-s)
                     :onClick (loading-dialog)})]))))))

;;;;;;;;;;;;;;;;;;
;; Form Builder ;;
;;;;;;;;;;;;;;;;;;

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
    (;; gmg/migrid :v "[grow, center]"
     gui-component/border-panel
     :center
     (gui-component/label
      :value (->> table-meta .return-table :representation)
      :halign :left
      :args [:font (gtool/getFont 15 :bold)]
      :foreground face/c-foreground-title
      :border (b/compound-border
               (b/line-border :bottom 1 :color face/c-underline)
               (b/empty-border :top 10))))))

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
        (insert-only-component    (metacomponent-with-action->swing-component
                                   state! dispatch!
                                   [{:type :jsgl-button, :value "To defaults", :on-click clean-button-on-click}])))
       (gui-component/mig-panel
        :constraints ["wrap 1" "0px[grow, fill]0px" "0px[fill, top]0px"]
        :border (b/empty-border :thickness 10)
        :items (vec (concat [[(information-header state!)]]
                            (map vector (components-from-metadata))
                            [[(gcomp/hr 10)]]
                            (map vector addtional-buttons)
                            [[(gcomp/hr 10)]]
                            (map vector insert-only-component))))))))

;; add expand segment which show selected element
(defn build-update-layout [state! dispatch! model-configuration]
  (with-state
   (let [{:keys [fields additional]} model-configuration]
     (where
      ((state (state! {:action :cursor :path [:model-selected]}))
       (components-from-metadata #(metafield->swing-component state! dispatch! fields))
       (input-panel
        (gui-component/mig-panel
         :constraints ["wrap 1" "0px[grow, fill]0px" "0px[fill, top]0px"]
         :items []))
       (update-input-panel
        (fn [] (c/config! input-panel :items (map vector (components-from-metadata))) input-panel))
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
        (fn [] (vec
               (concat [[(information-header state!)]]
                       [[(update-input-panel)]]
                       [[(gcomp/hr 10)]]
                       (map vector addtional-buttons)
                       [[(gcomp/hr 10)]]
                       (map vector update-only-component))))))
      (gui-component/mig-panel
       :constraints ["wrap 1" "0px[grow, fill]0px" "0px[fill, top]0px"]
       :border (b/empty-border :thickness 10)
       :items (panel-content-fn)
       :event-hook
       {:update-layout-renderer
        {:atom state
         :hook
         (fn [panel state-atom old-state new-state]
           (print-line (format "build-update-layout: update model-selected %s" (pr-str new-state)))
           (c/config! panel :items (panel-content-fn)))}})))))

;; add expand segment which show selected element
(defn build-export-layout [state! dispatch! _]
  (gui-component/label :value "TODO: credate custom and dedicated UI for that aims"))

(defn build-input-form [state! dispatch!]
  (with-state
    (where
     ((current-model-stategy (get-in (state!) [:plugin-ui-mode :mode]))
      (state (state! {:action :cursor :path [:plugin-ui-mode]}))
      ;; ----------------------------------(0)----
      (update-ui-layout-button
       (when (contains? (get plugin-config :model-configurations) :update)
        (gui-component/button
         :value "Update" :tip "Update ui layout" :tgap 2 :lgap 10 :bgap 0 :rgap 10 :on-click
         (fn [e] (dispatch! {:action :switch-plugin-ui-mode :plugin-ui-mode :update :debug? true})))))
      (export-ui-layout-button
       (when (contains? (get plugin-config :model-configurations) :export)
        (gui-component/button
         :value "Export" :tip "Export ui layout" :tgap 2 :lgap 10 :bgap 0 :rgap 10 :on-click
         (fn [e] (dispatch! {:action :switch-plugin-ui-mode :plugin-ui-mode :export :debug? true})))))
      (insert-ui-layout-button
       (when (contains? (get plugin-config :model-configurations) :insert)
         (gui-component/button
          :value "Insert" :tip "Insert ui layout" :tgap 2 :lgap 10 :bgap 0 :rgap 10 :on-click
          (fn [e] (dispatch! {:action :switch-plugin-ui-mode :plugin-ui-mode :insert :debug? true
                             :model (metadata/convert_metadata->model (.return-table_name table-meta))})))))
      ;; ----------------------------------(1)----
      (print-state-debug
       (gui-component/button
        :value "State!" :tip "Print state"
        :tgap 2 :lgap 10 :bgap 0 :rgap 10
        :on-click (fn [e] (dispatch! {:action :pprint-state}))))
      (input-panel
       (smig/mig-panel
        :constraints ["wrap 1" "0px[grow, fill]0px" "0px[fill, top]0px"]
        :items [[(build-insert-layout state! dispatch! (get-in plugin-config [:model-configurations current-model-stategy]))]])))
     (gui-component/border-panel
      :south input-panel
      :north (gui-component/horizontal-panel
              :items (keep identity
                           [insert-ui-layout-button
                            update-ui-layout-button
                            export-ui-layout-button
                            print-state-debug]))
      :event-hook
      {:panel-tab-switching-event
       {:atom state
        :hook
        (fn [panel state-atom old-state new-state]
          (case (:mode new-state)
            :insert (c/config! input-panel :items [[(build-insert-layout state! dispatch! (get-in plugin-config [:model-configurations (:mode new-state)]))]])
            :update (c/config! input-panel :items [[(build-update-layout state! dispatch! (get-in plugin-config [:model-configurations (:mode new-state)]))]])
            :export (c/config! input-panel :items [[(build-export-layout state! dispatch! (get-in plugin-config [:model-configurations (:mode new-state)]))]])
            (throw (ex-info "Udefinied mode!" {:plugin-ui-mode new-state}))))}}))))

(defn build-layout [state! dispatch!]
  (with-state
    (where
     ((table-config (plugin-config :view-configurations))
      (table (interaction/metacomponent->component
              (as-> table-config *t
                (update *t :data (fn [data]
                                   (if data
                                     (eval data)
                                     ((comp vec jarman.logic.connection/query jarman.logic.sql-tool/select!)
                                      (get *t :socket-refreshable-query)))))
                (if (nil? (get table-config :on-select))
                  (do (interaction/info "Table" "Empty `:on-select` event for table. Please select event") *t)
                  (update *t :on-select (fn [f] (fn [e]
                                                 (println "on-select event!")
                                                 ((eval f) state! dispatch! e))))))))
      (main-table-socket (table-socket/create-table-socket (:socket-id table-config) table table-config)))
     (dispatch! {:action :jack-in-socket :socket main-table-socket})
     (smig/mig-panel
      :constraints ["" "0px[shrink 0, fill]0px[grow, fill]0px" "0px[grow, fill]0px"]
      :items [[(c/left-right-split
                (build-input-form state! dispatch!)
                (c/scrollable
                 table
                 :hscroll :as-needed
                 :vscroll :as-needed
                 :border (seesaw.border/line-border :thickness 0 :color "#fff"))
                :divider-location 200)]]))))

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

(print-header
  "SOme header"
  (print-line "some event 1")

  (print-line "some event 2")
  (print-src "clojure"
    (pr-str "fdsajlkdfjsa")))

(defn dialog-table-entry [plugin-path]
  (print-header (format "Open 'dialog-table' plugin for '%s'" (str plugin-path))))

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
  (dialog-table :plugin-path [:profile :dialog-table :profile-dialog]
                :on-select (fn [e] (println "ID :+> "e)))
  ;;
  (with-test-environ [:user :table :user]
    (swing/quick-frame
     [(table-ui-expand-metafield
       state! dispatch!
       {:description nil,
        :private? false,
        :default-value nil,
        :editable? true,
        :field :id_profile,
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
        [{:id_profile :profile} {:delete :cascade, :update :cascade}],
        :representation "Id profile",
        :field-qualified :user.id_profile,
        :key-table :profile})]))
  ;; description panel
  (with-test-environ [:user :table :user]
    (swing/quick-frame [(relation-modal-helper-panel :profile 1)]))

  (with-test-environ [:user :table :user]
    (swing/quick-frame [(relation-modal-helper-panel :profile nil)]))

  (with-test-environ [:user :table :user]
    (dispatch! {:debug true :action :set-model-selected
                :value
                {:user.first_name "Ivan"
                 :user.last_name "Popov"}})
    (dispatch! {:debug true :action :set-model-changes
                :value
                {:user.first_name "Ivan"
                 :user.last_name "Popov"}})
    (println (:model-changes (state!)))
    (println (:model-selected (state!)))
    (swing/quick-frame
     (let [{:keys [fields additional]} (get-in plugin-config [:model-configurations :update])]
       (metafield->swing-component state! dispatch! fields))))

  (with-test-environ [:user :table :user]
    (dispatch! {:debug true :action :set-model-selected
                :value
                {:user.first_name "Ivan"
                 :user.last_name "Popov"}})
    (dispatch! {:debug true :action :set-model-changes
                :value
                {:user.first_name "Ivan"
                 :user.last_name "Popov"}})
    #_(dispatch! {:action :switch-plugin-ui-mode :plugin-ui-mode :update
                  :debug true
                  :model {:user.login "A", :user.password "B", :user.first_name "C", :user.last_name "A", :user.configuration "'{}'", :user.id_profile 2, :user.id 1}})
    (swing/quick-frame
      [(build-input-form state! dispatch!)]))

  (with-test-environ [:user :table :user]
    (swing/quick-frame
     [(build-layout state! dispatch!)]))

  (with-test-environ [:profile :table :profile]
    (swing/quick-frame
     [(build-layout state! dispatch!)]))

  (with-test-environ [:seal :table :seal]
    (swing/quick-frame
     [(build-layout state! dispatch!)]))

  (swing/quick-frame
   [(table-entry [:user :table :user])])

  (with-test-environ [:user :table :user]
    (swing/quick-frame
     [(meta-to-component
       {:field :date-label,
        :field-qualified :user.date-label
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
