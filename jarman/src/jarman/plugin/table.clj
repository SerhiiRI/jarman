(ns jarman.plugin.table
  (:require
   ;; Clojure toolkit 
   [clojure.data :as data]
   [clojure.string :as string]
   [seesaw.util :as u]
   ;; Seesaw components
   [seesaw.core :as c]
   [seesaw.border :as sborder]
   [seesaw.dev :as sdev]
   [seesaw.mig :as smig]
   [seesaw.swingx :as swingx]
   [seesaw.chooser :as chooser]
   ;; Jarman toolkit
   [jarman.logic.document-manager :as doc]
   [jarman.tools.lang :refer :all :as lang]
   [jarman.gui.gui-tools :refer :all :as gtool]
   [jarman.gui.gui-seed :as gseed]
   [jarman.resource-lib.icon-library :as ico]
   [jarman.tools.swing :as stool]
   [jarman.gui.gui-components :refer :all :as gcomp]
   [jarman.gui.gui-calendar :as calendar]
   [jarman.logic.metadata :as mt])
  (:import (java.util Date)
           (java.text SimpleDateFormat)))



;;; helper functions ;;;

(defn- get-reference-col-table-alias [referenced-column] (second (re-matches #"id_(.+)" (name (:field referenced-column)))))
(defn- get-reference-qualified-field [referenced-column table-meta]
  (if-not referenced-column
    (keyword ((comp :field :table :prop) table-meta))
    (let [referenced-qualified-name (get-reference-col-table-alias referenced-column)]
      (keyword referenced-qualified-name))))

(defn keyword-column-formatter [referenced-column table-meta column]
  (keyword (format "%s.%s" (name (get-reference-qualified-field referenced-column table-meta)) (name (:field column)))))

;;; Applyer functions ;;;

(defn add-to-m-boolean-column-type [m column] (if (in? (get column :component-type) mt/column-type-boolean) (into m {:class java.lang.Boolean}) m))
(defn add-to-m-number-column-type  [m column] (if (in? (get column :component-type) mt/column-type-number)  (into m {:class java.lang.Number})  m))
(defn add-to-m-class               [m column] (-> m (add-to-m-number-column-type  column) (add-to-m-boolean-column-type column)))
(defn add-to-m-representation      [m referenced-column column]
  (into m {:representation
           (str (if-let [tmp (:representation referenced-column)] (str tmp " "))
                (:representation column))}))
(defn add-to-m-join-rules          [m table-meta referenced-column referenced-table]
  (let [table-or-alias (get-reference-qualified-field referenced-column table-meta)]
    (if-not (and referenced-table  referenced-column) m
            (into m {:join-rule
                     (if (= (keyword ((comp :field :table :prop) table-meta)) table-or-alias)
                       (keyword (format "%s->%s" (name ((comp :field :table :prop) referenced-table)) (name table-or-alias)))
                       (keyword (format "%s->%s.id" (name (:field-qualified referenced-column)) (name ((comp :field :table :prop) table-meta)))))}))))

(defn jarman-table-k-v-formater
  "Description
    This is central logistics function which take
     - table from which recursion is performed
     - the column on which the linking is made
     - and table on which refer linking colum 

     referenced-table(table) -> referenced-column(column) -> table-meta(table)
  
  Example
    In this example `:repair_contract` make recusion jump to `:seal`
    through the `:id_new_seal` column in first table. Column have
    meta description to which table they must be linked  
      (jarman-table-k-v-formater
       (first (getset! :repair_contract))
       {:description nil,
        :private? false,
        :default-value nil,
        :editable? true,
        :field :id_new_seal,
        :column-type [:bigint-20-unsigned :default :null],
        :foreign-keys
        [{:id_new_seal :seal} {:delete :null, :update :null}],
        :component-type [\"l\"],
        :representation \"Id new seal\",
        :field-qualified :repair_contract.id_new_seal},
       (first (getset! :seal)))
          => [{:new_seal.seal_number {:representation \"Id new seal seal_number\"}}
              {:new_seal.datetime_of_use {:representation \"Id new seal datetime_of_use\"}}
              {:new_seal.datetime_of_remove {:representation \"Id new seal datetime_of_remove\"}}]"
  [referenced-table referenced-column table-meta]
  (mapv (fn [column]
          {(keyword-column-formatter referenced-column table-meta column)
           (-> {}
               (add-to-m-number-column-type column)
               (add-to-m-boolean-column-type column)
               (add-to-m-representation referenced-column column))})
        ((comp :columns :prop) table-meta)))

;;; Meta logistic functions 

(defn make-recur-meta [m-pipeline]
  (fn recur-meta [table-list]
    (if-let [table-meta (first (mt/getset! (first table-list)))]
      (let [table-list-atom (atom table-list) result (atom {})]
        (mt/--do-table-frontend-recursion
         table-meta
         (fn [referenced-table referenced-column table-meta]
           (swap! result (fn [m] {:pre [(map? m)] :post [map?]} (into m (m-pipeline referenced-table referenced-column table-meta))))
           (swap! table-list-atom #(filter (fn [t-name] (not= (keyword ((comp :field :table :prop) table-meta)) (keyword t-name))) %))))
        (into @result (if (not-empty @table-list-atom)
                        (recur-meta @table-list-atom)))))))

(def take-column-for-recur-table (make-recur-meta jarman-table-k-v-formater))
(let [table-list [:repair_contract :seal]]
  (take-column-for-recur-table table-list))

(defn jarman-table-columns-list
  "Example
    (jarman-table-columns-list [:repair_contract :seal]
                            [:repair_contract.id_cache_register
                            :repair_contract.id_old_seal
                            :repair_contract.id_new_seal
                            :repair_contract.repair_date
                            :repair_contract.cause_of_removing_seal
                            :repair_contract.tech_problem_description
                            :repair_contract.tech_problem_type
                            :repair_contract.cache_register_register_date
                            :seal.seal_number
                            :old_seal.seal_number
                            :new_seal.seal_number
                            :telefon.nubmer])
    ;;=> {:repair_contract.id_cache_register {:representation \"id_cache_register\"}, :repair_contract.id_old_seal {:representation \"id_old_seal\"}, :repair_contract.id_new_seal {:representation \"id_new_seal\"}, :repair_contract.repair_date {:representation \"repair_date\"}, :repair_contract.cause_of_removing_seal {:representation \"cause_of_removing_seal\"}, :repair_contract.tech_problem_description {:representation \"tech_problem_description\"}, :repair_contract.tech_problem_type {:representation \"tech_problem_type\"}, :repair_contract.cache_register_register_date {:representation \"cache_register_register_date\"}, :old_seal.seal_number {:representation \"id_old_seal seal_number\"}, :new_seal.seal_number {:representation \"id_new_seal seal_number\"}}"
  [table-list column-vector-list]
  (->> (reduce (fn [map-acc [k v]]
                 (if (contains? map-acc k)
                   (assoc map-acc k v)
                   map-acc))
               (apply array-map (mapcat vector  column-vector-list (repeat nil)))
               (take-column-for-recur-table table-list))
       (sequence (comp (remove (fn [[k v]] (nil? v)))
                    (mapcat identity)))
       (apply array-map)))


;;; ------------------------
;;; for system requirements

(defn conj-table-meta [referenced-table referenced-column table-meta]
    [(-> {:field-refer (get-reference-qualified-field referenced-column table-meta)
          :field-table (keyword ((comp :field :table :prop) table-meta))
          :representatoin ((comp :representation :table :prop) table-meta)}
         (add-to-m-join-rules table-meta referenced-column referenced-table))
     (mapv (fn [column]
             (-> {:field-column (keyword-column-formatter referenced-column table-meta column)}
                 (add-to-m-representation referenced-column column)))
           ((comp :columns :prop) table-meta))])

(defn make-recur-meta-one-table [m-pipeline]
  (fn [table-name]
    (if-let [table-metadata (first (mt/getset! table-name))]
      (let [result (atom [])]
        (mt/--do-table-frontend-recursion
         table-metadata
         (fn [referenced-table referenced-column table-meta]
           (swap! result (fn [m] (conj m (m-pipeline referenced-table referenced-column table-meta))))))
        @result))))  
(def take-meta-for-view (make-recur-meta-one-table conj-table-meta))

(defn create-jarman-table-plugin [table-name]
  (let [meta-table (first (mt/getset! :repair_contract))
    name-of-table ((comp :representation :table :prop) meta-table)
    full-meta-debug (take-meta-for-view table-name)
    tables (vec (for [meta-debug full-meta-debug]
                  (:field-refer (first meta-debug))))
    joines (vec (for [meta-debug full-meta-debug
                      :when (keyword? (:join-rule (first meta-debug)))]
                  (:join-rule (first meta-debug))))
    joines (vec (for [meta-debug full-meta-debug
                      :when (keyword? (:join-rule (first meta-debug)))]
                  (:join-rule (first meta-debug))))
    columns (concat (list 'as-is) (mapcat identity (for [meta-debug full-meta-debug]
                                                     (mapv :field-column (second meta-debug)))))]
    (list 'defview (symbol table-name)
          (list 'jarman-table
                :name name-of-table
                :tables tables
                :view columns
                :select (if (empty? joines)
                          {:inner-join joines :columns columns}
                          {:columns columns})))))

;; (mapv create-jarman-table-plugin [:permission :user :enterpreneur :point_of_sale :cache_register :point_of_sale_group :point_of_sale_group_links :seal :repair_contract :service_contract :service_contract_month])


;;; ------------------------
;;; for system requirements

;;; WARNING
;;; DEPRECATED
;; (defn get-view-column-meta [table-list column-list]
;;   (->> table-list
;;        (mapcat (fn [t] (vec ((comp :columns :prop) (first (mt/getset! t))))))
;;        (filter (fn [c] (in? column-list (keyword (:field-qualified c)))))))

;;; WARNING
;;; DEPRECATED
;; (defn- model-column [col-representation column]
;;   (let [component-type (:component-type column)
;;         on-boolean (fn [m] (if (in? component-type "b") (into m {:class java.lang.Boolean}) m))
;;         on-number  (fn [m] (if (in? component-type "n") (into m {:class java.lang.Number})  m))]
;;     (-> {:key (keyword (:field-qualified column)) :text (:representation column)}
;;         on-number
;;         on-boolean)))

;;; WARNING
;;; DEPRECATED
;; (defn- model-column [col-representation column]
;;   (let [component-type (:component-type column)
;;         on-boolean (fn [m] (if (in? component-type "b") (into m {:class java.lang.Boolean}) m))
;;         on-number  (fn [m] (if (in? component-type "n") (into m {:class java.lang.Number})  m))]
;;     (-> {:key (keyword (:field-qualified column)) :text (:representation column)}
;;         on-number
;;         on-boolean)))

;;; WARNING
;;; DEPRECATED
;; (defn gui-table-model-columns [table-list column-list col-representation]
;;   (mapv (partial model-column col-representation) (get-view-column-meta table-list column-list)))

;; (let [on-text  (fn [m v] (into m {:text (:representation v)}))
;;       on-class (fn [m v] (if (contains? v :class) (into m {:class (:class v)}) m))]
;;   (map (fn [[k v]] (-> {:key k} (on-class v) (on-text v)))
;;        (jarman-table-columns-list [:repair_contract :seal]
;;                                   [:repair_contract.id_cache_register
;;                                    :repair_contract.id_old_seal
;;                                    :repair_contract.id_new_seal
;;                                    :repair_contract.repair_date
;;                                    :repair_contract.cause_of_removing_seal
;;                                    :repair_contract.tech_problem_description
;;                                    :repair_contract.tech_problem_type
;;                                    :repair_contract.cache_register_register_date
;;                                    :seal.seal_number
;;                                    :old_seal.seal_number
;;                                    :new_seal.seal_number
;;                                    :telefon.nubmer])))


(defn gui-table-model-columns [table-list table-column-list]
  (let [on-text  (fn [m v] (into m {:text (:representation v)}))
        on-class (fn [m v] (if (contains? v :class) (into m {:class (:class v)}) m))]
    (mapv (fn [[k v]] (-> {:key k} (on-class v) (on-text v)))
          (jarman-table-columns-list table-list table-column-list))))

(defn gui-table-model [model-columns data-loader]
  (fn [] [:columns model-columns :rows (data-loader)]))

(defn gui-table [model]
  (fn [listener-fn]
    (let [TT (swingx/table-x :model (model))]
      (c/listen TT :selection (fn [e] (listener-fn (seesaw.table/value-at TT (c/selection TT)))))
      (c/config! TT :horizontal-scroll-enabled? true)
      (c/config! TT :show-grid? false)
      (c/config! TT :show-horizontal-lines? true)
      (c/scrollable TT :hscroll :as-needed :vscroll :as-needed))))

(defn create-table [configuration toolkit-map]
  (let [view (:view configuration) tables (:tables configuration)]
    (if (and view tables)
      (let [model-columns (gui-table-model-columns tables view)
            table-model   (gui-table-model model-columns (:select toolkit-map))]
        {:table-model table-model
         :table (gui-table table-model)}))))

(defn construct-dialog [table-fn selected frame]
  (let [dialog (seesaw.core/custom-dialog :modal? true :width 400 :height 500 :title "Select component")
        table (table-fn (fn [model] (seesaw.core/return-from-dialog dialog model)))
        dialog (seesaw.core/config!
                dialog
                :content (seesaw.mig/mig-panel
                          :constraints ["wrap 1" "0px[grow, fill]0px" "5px[grow, fill]0px"]
                          :items [[table]]))]
    (.setLocationRelativeTo dialog frame)
    (seesaw.core/show! dialog)))

(def build-input-form
  (fn [data-toolkit
       global-configuration
       & {:keys [model
                 more-comps
                 button-template
                 start-focus
                 export-comp
                 alerts]
          :or {model []
               more-comps [(c/label)]
               button-template (fn [title f] (gcomp/button-basic title f))
               start-focus nil
               export-comp nil
               alerts nil}}]
    (let [complete (atom {})
          metadata (:columns-meta data-toolkit)
          inser-or-update (if (empty? model) "Insert new data" "Update record")
          delete "Remove selected record"
          vgap (fn [size] (c/label :border (sborder/empty-border :top size)))
          panel (smig/mig-panel :constraints ["wrap 1" "0px[grow, fill]0px" "0px[fill]0px"]
                                :border (sborder/empty-border :thickness 10)
                                :items [[(c/label)]])
          components
          (concat
           (filter
            #(not (nil? %))
            (map (fn [meta]
                   (let [field-qualified (get meta :field-qualified)
                         title (get meta :representation)
                         editable? (get meta :editable?)
                         ;; field (get meta :field)
                         v (str (get-in model [(keyword field-qualified)]))
                         v (if (empty? v) "" v)]
                     (cond
                       (lang/in? (get meta :component-type) "d")
                       (do
                         (if (empty? model)
                           (do ;;Create calendar input
                             (gcomp/inpose-label title (calendar/calendar-with-atom :store-id field-qualified
                                                                                    :local-changes complete)))
                           (do ;; Create update calenda input
                             (gcomp/inpose-label title (calendar/calendar-with-atom :store-id field-qualified
                                                                                    :local-changes complete
                                                                                    :set-date (if (empty? v) nil v))))))

                       (lang/in? (get meta :component-type) "l")
                       (do ;; Add label with enable false input-text. Can run micro window with table to choose some record and retunr id.
                         (let [key-table  (keyword (get meta :key-table))
                               connected-table-conf  (get-in global-configuration [key-table :plug/jarman-table :configuration])
                               connected-table-data  (get-in global-configuration [key-table :plug/jarman-table :data-toolkit])
                               selected-representation (fn [dialog-model-view returned-from-dialog]
                                                         (->> (get dialog-model-view :view)
                                                              (map #(get-in returned-from-dialog [%]))
                                                              (filter some?)
                                                              (string/join ", ")))
                               v (selected-representation connected-table-conf model)]
                           (if-not (nil? (get model field-qualified)) (swap! complete (fn [storage] (assoc storage field-qualified (get-in model [field-qualified])))))
                           (gcomp/inpose-label title (gcomp/input-text-with-atom :local-changes complete :editable? false :val v
                                                                                 :onClick (fn [e]
                                                                                            (let [selected (construct-dialog (get (create-table connected-table-conf connected-table-data) :table) field-qualified (c/to-frame e))]
                                                                                              (if-not (nil? (get selected (get connected-table-data :model-id)))
                                                                                                (do (c/config! e :text (selected-representation connected-table-conf selected))
                                                                                                    (swap! complete (fn [storage] (assoc storage field-qualified (get selected (get connected-table-data :model-id)))))))))))))
                       (lang/in? (get meta :component-type) "a")
                       (do
                         (if (empty? model)
                           (do
                             (gcomp/inpose-label title (gcomp/input-text-area :store-id field-qualified
                                                                              :local-changes complete)))
                           (do
                             (gcomp/inpose-label title (gcomp/input-text-area :store-id field-qualified
                                                                              :local-changes complete
                                                                              :val v)))))
                       (lang/in? (get meta :component-type) "n")
                       (do
                         (if (empty? model)
                           (do
                             (gcomp/inpose-label title (gcomp/input-int :store-id field-qualified
                                                                        :local-changes complete)))
                           (do
                             (gcomp/inpose-label title (gcomp/input-int :store-id field-qualified
                                                                        :local-changes complete
                                                                        :val v)))))
                       (lang/in? (get meta :component-type) "i")
                       (do ;; Add input-text with label
                         (if (empty? model)
                           (do ;;Create insert input
                             (gcomp/inpose-label title (gcomp/input-text-with-atom :store-id field-qualified
                                                                                   :local-changes complete
                                                                                   :editable? editable?)))
                           (do ;; Create update input
                             (gcomp/inpose-label title (gcomp/input-text-with-atom :store-id field-qualified
                                                                                   :local-changes complete
                                                                                   :editable? editable?
                                                                                   :val v))))))))
                 metadata))
           [(vgap 20)]
           [(button-template inser-or-update (fn [e]
                                               (if (empty? model)
                                                 (do
                                                   (println "Expression insert" ((:insert data-toolkit) (merge {(keyword (str (get (:table-meta data-toolkit) :field) ".id")) nil} (first (merge model @complete))))))
                                                 (do
                                                   (println "Expression update" ((:update data-toolkit) (merge model @complete)))))
                                               ((@gseed/jarman-views-service :reload))
                                               ))]
           (if (empty? model) [] [(button-template delete (fn [e]
                                                            (println "Expression delete" ((:delete data-toolkit) {(keyword (str (get (:table-meta data-toolkit) :field) ".id")) (get model (keyword (str (get (:table-meta data-toolkit) :field) ".id")))}))
                                                            ((@gseed/jarman-views-service :reload))
                                                            ))])
           [(vgap 10)]
           [more-comps]
           [(if (nil? export-comp) (c/label) (export-comp (get model (:model-id data-toolkit))))])
          builded (c/config! panel :items (gtool/join-mig-items components))]
      (if-not (nil? start-focus) (reset! start-focus (last (u/children (first components)))))
      builded)))

;; {:permision {:jarman-table {:configuration {}
;;                             :data-toolkit  {}}}
;;  :user      {:jarman-table {:configuration {}
;;                             :data-toolkit  {}}}}

;;  (run user-view)
(defn export-print-doc
  [controller id alerts]
  (let [;;radio-group (c/button-group)
        panel-bg "#eee"
        expor-gb "#95dec9"
        focus-gb "#5ee6bf"
        input-text (gcomp/input-text :args [:text (str jarman.config.environment/user-home "\\Documents") :font (gtool/getFont  :name "Monospaced")])
        icon (c/label :icon (jarman.tools.swing/image-scale ico/enter-64-png 30)
                      :border (sborder/empty-border :thickness 8)
                      :listen [:mouse-clicked (fn [e] (let [new-path (chooser/choose-file :success-fn  (fn [fc file] (.getAbsolutePath file)))]
                                                        (c/config! input-text :text new-path)))])
        panel (smig/mig-panel
               :constraints ["" "0px[fill]0px[grow, fill]0px" "0px[fill]0px"]
               :items [[icon] [input-text]])]
    (smig/mig-panel
     :constraints ["wrap 1" "0px[grow, fill]0px" "0px[grow]0px"]
     :border (sborder/compound-border (sborder/empty-border :top 5)
                                      (sborder/line-border :top 2 :color "#999")
                                      (sborder/empty-border :top 50))
     :items [[(gcomp/button-expand "Export by template" (smig/mig-panel
                                                         :constraints ["wrap 1" "5px[grow, fill]5px" "0px[fill]0px"]
                                                         :background panel-bg
                                                         :items (gtool/join-mig-items
                                                                 [(gcomp/hr 2 "#ccc")]
                                                                 [(gcomp/hr 10)]
                                                                 [panel]
                                                                 [(gcomp/hr 10)]
                                                                 (map (fn [doc-model]
                                                                        [(gcomp/button-basic (get doc-model :name)
                                                                                             (fn [e]
                                                                                              ;; do
                                                                                               (try
                                                                                                 ((doc/prepare-export-file (:->table-name controller) doc-model) id (c/config input-text :text))
                                                                                                 (@jarman.gui.gui-seed/alert-manager :set {:header (gtool/get-lang-alerts :success) :body (gtool/get-lang-alerts :export-doc-ok)} (@jarman.gui.gui-seed/alert-manager :message jarman.gui.gui-seed/alert-manager) 7)
                                                                                                 (catch Exception e (@jarman.gui.gui-seed/alert-manager :set {:header (gtool/get-lang-alerts :faild) :body (gtool/get-lang-alerts :export-doc-faild)} (@jarman.gui.gui-seed/alert-manager :message jarman.gui.gui-seed/alert-manager) 7))))
                                                                                             :args [:halign :left])])
                                                                      (:->documents controller))
                                                                 [(gcomp/hr 10)]
                                                                 [(gcomp/hr 2 "#95dec9")]))
                                   :background "#95dec9"
                                   :border (sborder/compound-border (sborder/empty-border :left 10 :right 10)))]])))


(def auto-builder--table-view
  (fn [plugin-path global-configuration
       & {:keys [start-focus
                 alerts]
          :or {start-focus nil
               alerts nil}}]
    (let [x nil ;;------------ Prepare toolkits
          data-toolkit  (get-in global-configuration (lang/join-vec plugin-path [:data-toolkit]))
          configuration  (get-in global-configuration (lang/join-vec plugin-path [:configuration]))
          x nil ;;------------ Prepare components
          expand-export (fn [id] (export-print-doc (get-in global-configuration plugin-path) id alerts))
          insert-form   (fn [] (build-input-form data-toolkit global-configuration :start-focus start-focus :alerts alerts))
          view-layout   (smig/mig-panel :constraints ["" "0px[shrink 0, fill]0px[grow, fill]0px" "0px[grow, fill]0px"])
          table         (fn [] (second (u/children view-layout)))
          header        (fn [] (c/label :text (get (:table-meta data-toolkit) :representation) :halign :center :border (sborder/empty-border :top 10)))
          update-form   (fn [model return] (gcomp/expand-form-panel view-layout [(header) (build-input-form data-toolkit global-configuration :model model :export-comp expand-export :more-comps [(return)])]))
          x nil ;;------------ Build
          expand-insert-form (gcomp/scrollbox (gcomp/expand-form-panel view-layout [(header) (insert-form)]) :hscroll :never)
          back-to-insert     (fn [] (gcomp/button-basic "<< Return to Insert Form" (fn [e] (c/config! view-layout :items [[expand-insert-form] [(table)]]))))
          expand-update-form (fn [model return] (c/config! view-layout :items [[(gcomp/scrollbox (update-form model return) :hscroll :never)] [(table)]]))
          table              (fn [] ((get (create-table configuration data-toolkit) :table) (fn [model] (expand-update-form model back-to-insert)))) ;; TODO: set try
          x nil ;;------------ Finish
          view-layout        (c/config! view-layout :items [[(c/vertical-panel :items [expand-insert-form])] [(c/vertical-panel :items [(table)])]])]
      view-layout)))

;; (let [my-frame (-> (doto (c/frame
;;                           :title "test"
;;                           :size [300 :by 800]
;;                           :content vp)
;;                      (.setLocationRelativeTo nil) c/pack! c/show!))]
;;   (c/config! my-frame :size [300 :by 800]))



;;; PLUGINS ;;;
(defn jarman-table-proxy [plugin-path global-configuration]
  (let [vp (c/select @jarman.gui.gui-seed/app [:#tables-view-plugin])
        atm (get (c/config vp :user-data) :atom-expanded-items)]
    (swap! atm (fn [inserted] (conj inserted (let [data-toolkit  (get-in (global-configuration) (lang/join-vec plugin-path [:data-toolkit]))
                                                   title (get (:table-meta data-toolkit) :representation)]
                                              ;;  (println "Repre " title)
                                               (button-expand-child
                                                title
                                                :onClick (fn [e] (@gseed/jarman-views-service :set-view
                                                                                              :view-id (str "auto-" title)
                                                                                              :title title
                                                                                              :scrollable? false
                                                                                              :component-fn (fn [] (auto-builder--table-view plugin-path (global-configuration))))))))))
    (.revalidate vp)))




