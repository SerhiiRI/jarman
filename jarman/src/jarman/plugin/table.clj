(ns jarman.plugin.table
  (:require
   ;; Clojure toolkit 
   [clojure.data :as data]
   [clojure.string :as string]
   [clojure.spec.alpha :as s]
   [seesaw.util :as u]
   ;; Dev tools
   [jarman.logic.session :as session]
   ;; Seesaw components
   [seesaw.core :as c]
   [seesaw.border :as sborder]
   [seesaw.dev :as sdev]
   [seesaw.mig :as smig]
   [seesaw.swingx :as swingx]
   [seesaw.chooser :as chooser]
   ;; Jarman toolkit
   [jarman.logic.document-manager :as doc]
   [jarman.tools.lang :refer :all]
   [jarman.gui.gui-tools :refer :all :as gtool]
   [jarman.gui.gui-seed :as gseed]
   [jarman.resource-lib.icon-library :as ico]
   [jarman.tools.swing :as stool]
   [jarman.logic.state :as state]
   [jarman.gui.gui-components :refer :all :as gcomp]
   [jarman.gui.gui-calendar :as calendar]
   [jarman.logic.metadata :as mt]
   [jarman.plugin.spec :as spec]
   [jarman.gui.gui-tutorials.key-dispacher-tutorial :as key-tut])
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

(defn table-k-v-formater
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
      (table-k-v-formater
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

(def take-column-for-recur-table (make-recur-meta table-k-v-formater))
;; (let [table-list [:repair_contract :seal]]
;;   (take-column-for-recur-table table-list))

(defn table-columns-list
  "Example
    (table-columns-list [:repair_contract :seal]
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


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; METADATA TABLE MODEL CREATOR ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- conj-table-meta [referenced-table referenced-column table-meta]
  [(-> {:field-refer (get-reference-qualified-field referenced-column table-meta)
        :field-table (keyword ((comp :field :table :prop) table-meta))
        :representatoin ((comp :representation :table :prop) table-meta)}
       (add-to-m-join-rules table-meta referenced-column referenced-table))
   (conj (mapv (fn [column]
                 (-> {:field-column (keyword-column-formatter referenced-column table-meta column)}
                     (add-to-m-representation referenced-column column)))
               (cons {:description nil, :field :id, :column-type [:varchar-100 :nnull], :component-type ["n"], :representation "_", :field-qualified (keyword (format "%s.id" ((comp :field :table :prop) table-meta)))}
                     ((comp :columns :prop) table-meta))))])


(defn- make-recur-meta-one-table [m-pipeline]
  (fn [table-name]
    (if-let [table-metadata (first (mt/getset! table-name))]
      (let [result (atom [])]
        (mt/--do-table-frontend-recursion
         table-metadata
         (fn [referenced-table referenced-column table-meta]
           (swap! result (fn [m] (conj m (m-pipeline referenced-table referenced-column table-meta))))))
        @result))))
(def ^:private take-meta-for-view (make-recur-meta-one-table conj-table-meta))


(defn- create-table-plugin [table-name]
  (let [meta-table (first (mt/getset! table-name))
        table-model (mapv :field-qualified ((comp :columns :prop) meta-table))
        name-of-table ((comp :representation :table :prop) meta-table)
        full-meta-debug (take-meta-for-view table-name)
        tables (vec (for [meta-debug full-meta-debug]
                      (:field-refer (first meta-debug))))
        joines (vec (for [meta-debug full-meta-debug
                          :when (keyword? (:join-rule (first meta-debug)))]
                      (:join-rule (first meta-debug))))
        columns (vec (concat (list :#as_is) ;; (list (read-string (format "%s.id" table-name)))
                         (mapcat identity (for [meta-debug full-meta-debug]
                                            (mapv :field-column (second meta-debug))))))
        models (vec (concat ;; (list (read-string (format "%s.id" table-name)))
                     (mapcat identity (for [meta-debug full-meta-debug]
                                        (mapv :field-column (second meta-debug))))))]
    (list 'defview (symbol table-name)
          (list 'table
                :name name-of-table
                :plug-place [:#tables-view-plugin]
                :tables tables
                :view-columns table-model
                :model table-model
                :insert-button true
                :delete-button true
                :actions []
                :buttons []
                :query (if-not (empty? joines)
                         {:table_name (keyword name-of-table)
                          :inner-join joines :columns columns}
                         {:table_name (keyword name-of-table)
                          :column columns})))))

;; (create-table-plugin :user)
;; (mapv create-table-plugin [:permission :user :enterpreneur :point_of_sale :cache_register :point_of_sale_group :point_of_sale_group_links :seal :repair_contract :service_contract :service_contract_month])

(defn- gui-table-model-columns [table-list table-column-list]
  (let
      [on-text  (fn [m v] (into m {:text (:representation v)}))
       on-class (fn [m v] (if (contains? v :class) (into m {:class (:class v)}) m))]
      (mapv (fn [[k v]] (-> {:key k} (on-class v) (on-text v)))
            (table-columns-list table-list table-column-list))))

(defn- gui-table-model [model-columns data-loader]
  (fn [] [:columns model-columns :rows (data-loader)]))

(defn- gui-table [table-model]
  (fn [listener-fn]
    (let [TT (swingx/table-x :model (table-model))]
      (c/listen TT :selection (fn [e] (listener-fn (seesaw.table/value-at TT (c/selection TT)))))
      (c/config! TT :horizontal-scroll-enabled? true)
      (c/config! TT :show-grid? false)
      (c/config! TT :show-horizontal-lines? true)
      (c/scrollable TT :hscroll :as-needed :vscroll :as-needed :border nil))))

(defn- create-table [configuration toolkit-map]
  (let [view (:view-columns configuration) tables (:tables configuration)]
    ;; (println "\nView table\n" view)
    (if (and view tables)
      (let [model-columns (gui-table-model-columns tables view)
            table-model (gui-table-model model-columns (:select toolkit-map))
            ;; x (println "\ntable-model " (table-model))
            ]
        {:table-model table-model
         :table (gui-table table-model)}))))

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
                 (let [selected-model (popup-table (:table (create-table ct-conf ct-data)) field-qualified (c/to-frame e))]
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
              (println "\nModel ID:" (first (:model-columns data-toolkit)))
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
      comp)))


(defn convert-key-to-component
  "Description
     Convert to component automaticly by keyword.
     key is an key from model in defview.
   "
  [global-configuration local-changes meta-data table-model key]
  
  (let [meta            (key meta-data)
        x (println "\nMetadata for key" meta)
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
     Prepare and merge complete big parts
   "
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
          table              (fn [] ((:table (create-table configuration data-toolkit))
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

(defn table-toolkit-pipeline [configuration datatoolkit]
  datatoolkit)

;;;PLUGINS ;;;        
(defn table-component [plugin-path global-configuration spec-map]
  (println "Loading table plugin")
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
    ;; TODO: Set invoker expand button if not exist add child invokers
    (if (false? (spec/test-keys-jtable configuration spec-map))
      (println "[ Warning ] plugin/table: Error in spec")
      (if (session/allow-permission? (:permission configuration))
        (do
          (swap! atm (fn [inserted]
                       (conj inserted
                             (gcomp/button-expand-child
                              title
                              :onClick (fn [e]
                                        ;;  (println "\nplugin-path\n" plugin-path title)
                                         ((state/state :jarman-views-service)
                                          :set-view
                                          :view-id (str "auto-" title)
                                          :title title
                                          :scrollable? false
                                          :component-fn (fn [] (auto-builder--table-view
                                                                (global-configuration)
                                                                data-toolkit
                                                                configuration)))))))))))
    (.revalidate space)))



