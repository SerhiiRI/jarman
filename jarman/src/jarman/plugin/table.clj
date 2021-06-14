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
        columns (concat (list 'as-is) ;; (list (read-string (format "%s.id" table-name)))
                        (mapcat identity (for [meta-debug full-meta-debug]
                                           (mapv :field-column (second meta-debug)))))
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
                         {:inner-join joines :columns columns}
                         {:column columns})))))


;;(create-table-plugin :enterpreneur)
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
    (if (and view tables)
      (let [model-columns (gui-table-model-columns tables view)
            table-model (gui-table-model model-columns (:select toolkit-map))]
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


(defn input-related-popup-table
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


;; ┌───────────────────┐
;; │                   │
;; │ Single Components │
;; │                   │
;; └───────────────────┘

(defn button-insert
  [data-toolkit local-changes table-model]
  [(gcomp/hr 2)
   (gcomp/button-basic
    "Insert new data"
    :onClick (fn [e] (println "Insert but Locla changes: " @local-changes)
               (println "\nInsert Exp: \n" ((:insert-expression data-toolkit)
                                            (merge {(keyword (str (:field (:table-meta data-toolkit)) ".id")) nil}
                                                   (first (merge table-model @local-changes)))) "\n")
               ;;  (println "Expression insert" ((:insert data-toolkit) (merge {(keyword (str (get (:table-meta data-toolkit) :field) ".id")) nil} (first (merge table-model @local-changes)))))
               ;;  ((@gseed/jarman-views-service :reload))
               ))])

(defn button-update
  [data-toolkit local-changes]
  [(gcomp/hr 2)
   (gcomp/button-basic
    "Update row"
    :onClick (fn [e] (println "Update")
      ;;  (println "Expression update" ((:update data-toolkit) (merge table-model @local-changes))))
              ;;  ((@gseed/jarman-views-service :reload))
               ))])

(defn button-delete
  [data-toolkit]
  [(gcomp/hr 2)
   (gcomp/button-basic
    "Delete row"
    :onClick (fn [e] (println "Delete")
      ;;  (println "Expression delete" ((:delete data-toolkit) {(keyword (str (get (:table-meta data-toolkit) :field) ".id")) (get table-model (keyword (str (get (:table-meta data-toolkit) :field) ".id")))}))
              ;;  ((@gseed/jarman-views-service :reload))
               ))])

(defn button-export
  [data-toolkit]
  [(gcomp/hr 2)
   (gcomp/button-basic
    "Export"
    :onClick (fn [e] (println "Export")))])

;; ((resolve 'gcomp/input-int))

(defn get-missed-props
  "Description
   Return not binded map, just cut this what exist.
   (get-missed-key {:a a} {:a a :b c :d e}) => {:b c, :d e}
   "
  [binded-map orgin-map]
  (->> (map #(first %) orgin-map)
       (filter (fn [orgin-key] (not (in? (map #(first %) binded-map) orgin-key))))
       (into {} (map #(into {% (% orgin-map)})))))

(defn merge-binded-props
  "Description
     Get map where are binded keys, get properties for component and create new map with properties.
   Example:
     (merge-binded-props {:title \"mytitle\" :value \"pepe\"} {:title :custom-key}) => {:custom-key \"mytitle\" :value \"pepe\"}
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
          pre-comp (rift (comp-fn props) (c/label "Can not invoke component from defview."))
          comp (gcomp/inpose-label title pre-comp)]
      ;; (println "Props: " props)
      ;; (println "\nComplete-----------")
      comp)))


(defn convert-key-to-component
  "Description
     Convert to component automaticly by keyword.
     k is an key from model in defview.
   "
  [global-configuration local-changes meta-data table-model key]
  (let [meta            (key meta-data)
        field-qualified (:field-qualified meta)
        title           (:representation  meta)
        editable?       (:editable?       meta)
        comp-type       (:component-type  meta)
        key-table       (->> (rift (:key-table meta) nil) (#(if (keyword? %) % (keyword %))))
        val             (rift (str (key table-model)) "")
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
  [local-changes configuration]
  (let [button-fn (fn [title action]
                    ;; (println "\nTitle " title "\nAction: "  action)
                    (if (fn? action)
                      [(gcomp/hr 10)
                       (gcomp/button-basic title :onClick (fn [e] (action {:user-start 0 :user-end 1})))]))]
    (doall (->> (:buttons configuration)
                (map #(button-fn (:title %) (get (:actions configuration) (:action %))))
                (filter #(not (nil? %)))))))


;; ┌───────────────┐
;; │               │
;; │ Docs exporter |
;; │               │
;; └───────────────┘
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
                                                                                             :onClick (fn [e]
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
  (fn [data-toolkit configuration global-configuration
       & {:keys [table-model more-comps]
          :or {table-model [] more-comps []}}]
    (let [local-changes (atom {})
          meta-data (convert-metadata-vec-to-map (:columns-meta data-toolkit))
          components (convert-model-to-components-list global-configuration local-changes meta-data table-model (:model configuration))
          panel (smig/mig-panel :constraints ["wrap 1" "0px[grow, fill]0px" "0px[fill]0px"]
                                :border (sborder/empty-border :thickness 10)
                                :items [[(c/label)]])
          components (join-mig-items
                      components
                      (generate-custom-buttons local-changes configuration)
                      (if (empty? table-model)
                        (if-not  (= false (:insert-button configuration)) [(gcomp/hr 10) (button-insert data-toolkit local-changes table-model) more-comps] [more-comps])
                        [(if-not (= false (:update-button configuration)) [(gcomp/hr 10) (button-update data-toolkit local-changes)] [])
                         (if-not (= false (:delete-button configuration)) [(button-delete data-toolkit)] [])
                         more-comps
                         (button-export data-toolkit)]))
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
          insert-form   (fn [] (build-input-form data-toolkit configuration global-configuration))
          view-layout   (smig/mig-panel :constraints ["" "0px[shrink 0, fill]0px[grow, fill]0px" "0px[grow, fill]0px"])
          table         (fn [] (second (u/children view-layout)))
          header        (fn [] (c/label :text (get (:table-meta data-toolkit) :representation) :halign :center :border (sborder/empty-border :top 10)))
          update-form   (fn [table-model return] (gcomp/expand-form-panel view-layout [(header) (build-input-form data-toolkit configuration global-configuration :table-model table-model :more-comps [(return)])]))
          x nil ;;------------ Build
          expand-insert-form (gcomp/min-scrollbox (gcomp/expand-form-panel view-layout [(header) (insert-form)]) ;;:hscroll :never
                                                  )
          back-to-insert     (fn [] [(gcomp/hr 2) (gcomp/button-basic "<< Return to Insert Form" :onClick (fn [e] (c/config! view-layout :items [[expand-insert-form] [(table)]])))])
          expand-update-form (fn [model return] (c/config! view-layout :items [[(gcomp/min-scrollbox (update-form model return))] [(table)]]))
          table              (fn [] ((:table (create-table configuration data-toolkit)) (fn [model] (expand-update-form model back-to-insert))))
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
  ;; (println "Loading table plugin")
  (let [get-from-global #(->> % (join-vec plugin-path) (get-in (global-configuration)))
        data-toolkit  (get-from-global [:toolkit])
        configuration (get-from-global [:config])
        ;; title (get-in data-toolkit [:table-meta :representation])
        title (:name configuration)
        space (c/select @jarman.gui.gui-seed/app (:plug-place configuration))
        atm (:atom-expanded-items (c/config space :user-data))]
    ;; (println "Allow Permission: " (session/allow-permission? (:permission configuration)))

    (if (false? (spec/test-keys-jtable configuration spec-map))
      (println "[ Warning ] plugin/table: Error in spec")
      (if (session/allow-permission? (:permission configuration))
        (swap! atm (fn [inserted]
                     (conj inserted
                           (gcomp/button-expand-child
                            title
                            :onClick (fn [e]
                                       (println "\nplugin-path\n" plugin-path title)
                                       (@gseed/jarman-views-service
                                        :set-view
                                        :view-id (str "auto-" title)
                                        :title title
                                        :scrollable? false
                                        :component-fn (fn [] (auto-builder--table-view
                                                              (global-configuration)
                                                              data-toolkit
                                                              configuration))))))))))
    (.revalidate space)))



;; DATA TOOLKIT
;; {:model-id :user.id, 
;;  :insert #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/fn--16726], 
;;  :delete-expression #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/delete-expression--16720], 
;;  :select-expression #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/select-expression--16723], 
;;  :update #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/fn--16728], 
;;  :delete #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/fn--16730], 
;;  :update-expression #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/update-expression--16716], 
;;  :table-meta {:description nil, 
;;               :ref {:front-references [:permission], 
;;                     :back-references nil}, 
;;               :allow-linking? true, 
;;               :field user, 
;;               :representation user, 
;;               :is-linker? false, 
;;               :allow-modifing? true, 
;;               :allow-deleting? true, 
;;               :is-system? false}, 
;;  :insert-expression #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/insert-expression--16718], 
;;  :columns-meta [{:description nil, 
;;                  :private? false, 
;;                  :default-value nil, 
;;                  :editable? true, 
;;                  :field :login, 
;;                  :column-type [:varchar-100 :nnull], 
;;                  :component-type [:text], 
;;                  :representation login, 
;;                  :field-qualified :user.login} 
;;                 {:description nil, :private? false, :default-value nil, :editable? true, :field :password, :column-type [:varchar-100 :nnull], :component-type [:text], :representation password, :field-qualified :user.password} 
;;                 {:description nil, :private? false, :default-value nil, :editable? true, :field :first_name, :column-type [:varchar-100 :nnull], :component-type [:text], :representation first_name, :field-qualified :user.first_name} 
;;                 {:description nil, :private? false, :default-value nil, :editable? true, :field :last_name, :column-type [:varchar-100 :nnull], :component-type [:text], :representation last_name, :field-qualified :user.last_name} 
;;                 {:description nil, :private? false, :default-value nil, :editable? true, :field :id_permission, :column-type [:bigint-120-unsigned :nnull], :foreign-keys [{:id_permission :permission} {:delete :cascade, :update :cascade}], :component-type [:link], :representation id_permission, :field-qualified :user.id_permission, :key-table :permission}], 
;;  :select #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/fn--16732]}


;; Configuration defview
;; {:plug-place [:#tables-view-plugin], 
;;  :buttons [{:action :add-multiply-users-insert, :title Auto generate users}], 
;;  :permission [:user], 
;;  :name user-override, :tables [:user :permission], 
;;  :plugin-config-path [:user :table :plugin-19409], 
;;  :actions {:add-multiply-users-insert (fn [state] (let [{user-start :user-start, user-end :user-end} (clojure.core/deref state)] (println (map (fn* [p1__19371#] (hash-map :user.login (str user p1__19371#) :user.password 1234 :user.last_name (str user p1__19371#) :user.first_name (str user p1__19371#) :user.id_permission 2)) (range user-start (+ 1 user-end))))))}, 
;;  :insert-button true, 
;;  :id :plugin-19409, 
;;  :plugin-name table, 
;;  :delete-button false, 
;;  :query {:inner-join [:user->permission], :column [{:user.id :user.id} {:user.login :user.login} {:user.password :user.password} {:user.first_name :user.first_name} {:user.last_name :user.last_name} {:user.id_permission :user.id_permission} {:permission.id :permission.id} {:permission.permission_name :permission.permission_name} {:permission.configuration :permission.configuration}]}, 
;;  :table-name :user, 
;;  :view-columns [:user.login 
;;                 :user.first_name 
;;                 :user.last_name 
;;                 :permission.permission_name], 
;;  :model [{:model-reprs Login, :model-param :user.login, :bind-args {:title :title}, :model-comp jarman.gui.gui-components/input-text-with-atom} 
;;          :user.password 
;;          :user.first_name 
;;          :user.last_name 
;;          :user.id_permission 
;;          {:model-reprs Start user, :model-param :user-start, :model-comp jarman.gui.gui-components/input-int} 
;;          {:model-reprs End user, :model-param :user-end, :model-comp jarman.gui.gui-components/input-int}]}


;; GLOBAL
;;  {
;;   :permission {:table {:p-1 {:config {:plug-place [:#tables-view-plugin], :permission [:user], :view-columns [:permission.permission_name :permission.configuration], :name permission, :tables [:permission], :plugin-config-path [:permission :table :p-1], :id :p-1, :plugin-name table, :query {:column [{:permission.id :permission.id} {:permission.permission_name :permission.permission_name} {:permission.configuration :permission.configuration}]}, :table-name :permission, :model [{:model-reprs Permision name, :model-param :permission.permission_name, :model-comp jarman.gui.gui-components/input-text-with-atom} :permission.configuration]}, :toolkit {:model-id :permission.id, :insert #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/fn--16726], :delete-expression #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/delete-expression--16720], :select-expression #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/select-expression--16723], :update #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/fn--16728], :delete #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/fn--16730], :update-expression #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/update-expression--16716], :table-meta {:description nil, :ref {:front-references nil, :back-references nil}, :allow-linking? true, :field permission, :representation permission, :is-linker? false, :allow-modifing? true, :allow-deleting? true, :is-system? false}, :insert-expression #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/insert-expression--16718], :columns-meta [{:description nil, :private? false, :default-value nil, :editable? true, :field :permission_name, :column-type [:varchar-20 :default :null], :component-type [:text], :representation permission_name, :field-qualified :permission.permission_name} {:description nil, :private? false, :default-value nil, :editable? true, :field :configuration, :column-type [:tinytext :nnull :default '{}'], :component-type [:textarea], :representation configuration, :field-qualified :permission.configuration}], :select #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/fn--16732]}}}}, 
;;   :enterpreneur {:table {:plugin-17192 
;;                          {:config {:plug-place [:#tables-view-plugin], :buttons [], :permission [:user], :view-columns [:enterpreneur.ssreou :enterpreneur.ownership_form :enterpreneur.vat_certificate :enterpreneur.individual_tax_number :enterpreneur.director :enterpreneur.accountant :enterpreneur.legal_address :enterpreneur.physical_address :enterpreneur.contacts_information], :name enterpreneur, :tables [:enterpreneur], :plugin-config-path [:enterpreneur :table :plugin-17192], :actions [], :insert-button true, :id :plugin-17192, :plugin-name table, :delete-button true, :query {:column [{:enterpreneur.id :enterpreneur.id} {:enterpreneur.ssreou :enterpreneur.ssreou} {:enterpreneur.ownership_form :enterpreneur.ownership_form} {:enterpreneur.vat_certificate :enterpreneur.vat_certificate} {:enterpreneur.individual_tax_number :enterpreneur.individual_tax_number} {:enterpreneur.director :enterpreneur.director} {:enterpreneur.accountant :enterpreneur.accountant} {:enterpreneur.legal_address :enterpreneur.legal_address} {:enterpreneur.physical_address :enterpreneur.physical_address} {:enterpreneur.contacts_information :enterpreneur.contacts_information}]}, :table-name :enterpreneur, :model [:enterpreneur.ssreou :enterpreneur.ownership_form :enterpreneur.vat_certificate :enterpreneur.individual_tax_number :enterpreneur.director :enterpreneur.accountant :enterpreneur.legal_address :enterpreneur.physical_address :enterpreneur.contacts_information]}, 
;;                           :toolkit {:model-id :enterpreneur.id, :insert #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/fn--16726], :delete-expression #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/delete-expression--16720], :select-expression #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/select-expression--16723], :update #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/fn--16728], :delete #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/fn--16730], :update-expression #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/update-expression--16716], :table-meta {:description nil, :ref {:front-references nil, :back-references nil}, :allow-linking? true, :field enterpreneur, :representation enterpreneur, :is-linker? false, :allow-modifing? true, :allow-deleting? true, :is-system? false}, :insert-expression #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/insert-expression--16718], :columns-meta [{:description nil, :private? false, :default-value nil, :editable? true, :field :ssreou, :column-type [:tinytext :nnull], :component-type [:textarea], :representation ssreou, :field-qualified :enterpreneur.ssreou} {:description nil, :private? false, :default-value nil, :editable? true, :field :ownership_form, :column-type [:varchar-100 :default :null], :component-type [:text], :representation ownership_form, :field-qualified :enterpreneur.ownership_form} {:description nil, :private? false, :default-value nil, :editable? true, :field :vat_certificate, :column-type [:tinytext :default :null], :component-type [:textarea], :representation vat_certificate, :field-qualified :enterpreneur.vat_certificate} {:description nil, :private? false, :default-value nil, :editable? true, :field :individual_tax_number, :column-type [:varchar-100 :default :null], :component-type [:text], :representation individual_tax_number, :field-qualified :enterpreneur.individual_tax_number} {:description nil, :private? false, :default-value nil, :editable? true, :field :director, :column-type [:varchar-100 :default :null], :component-type [:text], :representation director, :field-qualified :enterpreneur.director} {:description nil, :private? false, :default-value nil, :editable? true, :field :accountant, :column-type [:varchar-100 :default :null], :component-type [:text], :representation accountant, :field-qualified :enterpreneur.accountant} {:description nil, :private? false, :default-value nil, :editable? true, :field :legal_address, :column-type [:varchar-100 :default :null], :component-type [:text], :representation legal_address, :field-qualified :enterpreneur.legal_address} {:description nil, :private? false, :default-value nil, :editable? true, :field :physical_address, :column-type [:varchar-100 :default :null], :component-type [:text], :representation physical_address, :field-qualified :enterpreneur.physical_address} {:description nil, :private? false, :default-value nil, :editable? true, :field :contacts_information, :column-type [:mediumtext :default :null], :component-type [:textarea], :representation contacts_information, :field-qualified :enterpreneur.contacts_information}], :select #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/fn--16732]}}, 
;;                          :plugin-19246 {:config {:plug-place [:#tables-view-plugin], :buttons [], :permission [:user], :view-columns [:enterpreneur.ssreou :enterpreneur.ownership_form :enterpreneur.vat_certificate :enterpreneur.individual_tax_number :enterpreneur.director :enterpreneur.accountant :enterpreneur.legal_address :enterpreneur.physical_address :enterpreneur.contacts_information], :name enterpreneur, :tables [:enterpreneur], :plugin-config-path [:enterpreneur :table :plugin-19246], :actions [], :insert-button true, :id :plugin-19246, :plugin-name table, :delete-button true, :query {:column [{:enterpreneur.id :enterpreneur.id} {:enterpreneur.ssreou :enterpreneur.ssreou} {:enterpreneur.ownership_form :enterpreneur.ownership_form} {:enterpreneur.vat_certificate :enterpreneur.vat_certificate} {:enterpreneur.individual_tax_number :enterpreneur.individual_tax_number} {:enterpreneur.director :enterpreneur.director} {:enterpreneur.accountant :enterpreneur.accountant} {:enterpreneur.legal_address :enterpreneur.legal_address} {:enterpreneur.physical_address :enterpreneur.physical_address} {:enterpreneur.contacts_information :enterpreneur.contacts_information}]}, :table-name :enterpreneur, :model [:
;; enterpreneur.ssreou :enterpreneur.ownership_form :enterpreneur.vat_certificate :enterpreneur.individual_tax_number :enterpreneur.director :enterpreneur.accountant :enterpreneur.legal_address :enterpreneur.physical_address :enterpreneur.contacts_information]}, :toolkit {:model-id :enterpreneur.id, :insert #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/fn--16726], :delete-expression #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/delete-expression--16720], :select-expression #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/select-expression--16723], :update #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/fn--16728], :delete #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/fn--16730], :update-expression #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/update-expression--16716], :table-meta {:description nil, :ref {:front-references nil, :back-references nil}, :allow-linking? true, :field enterpreneur, :representation enterpreneur, :is-linker? false, :allow-modifing? true, :allow-deleting? true, :is-system? false}, :insert-expression #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/insert-expression--16718], :columns-meta [{:description nil, :private? false, :default-value nil, :editable? true, :field :ssreou, :column-type [:tinytext :nnull], :component-type [:textarea], :representation ssreou, :field-qualified :enterpreneur.ssreou} {:description nil, :private? false, :default-value nil, :editable? true, :field :ownership_form, :column-type [:varchar-100 :default :null], :component-type [:text], :representation ownership_form, :field-qualified :enterpreneur.ownership_form} {:description nil, :private? false, :default-value nil, :editable? true, :field :vat_certificate, :column-type [:tinytext :default :null], :component-type [:textarea], :representation vat_certificate, :field-qualified :enterpreneur.vat_certificate} {:description nil, :private? false, :default-value nil, :editable? true, :field :individual_tax_number, :column-type [:varchar-100 :default :null], :component-type [:text], :representation individual_tax_number, :field-qualified :enterpreneur.individual_tax_number} {:description nil, :private? false, :default-value nil, :editable? true, :field :director, :column-type [:varchar-100 :default :null], :component-type [:text], :representation director, :field-qualified :enterpreneur.director} {:description nil, :private? false, :default-value nil, :editable? true, :field :accountant, :column-type [:varchar-100 :default :null], :component-type [:text], :representation accountant, :field-qualified :enterpreneur.accountant} {:description nil, :private? false, :default-value nil, :editable? true, :field :legal_address, :column-type [:varchar-100 :default :null], :component-type [:text], :representation legal_address, :field-qualified :enterpreneur.legal_address} {:description nil, :private? false, :default-value nil, :editable? true, :field :physical_address, :column-type [:varchar-100 :default :null], :component-type [:text], :representation physical_address, :field-qualified :enterpreneur.physical_address} {:description nil, :private? false, :default-value nil, :editable? true, :field :contacts_information, :column-type [:mediumtext :default :null], :component-type [:textarea], :representation contacts_information, :field-qualified :enterpreneur.contacts_information}], :select #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/fn--16732]}}, 
;;                          :plugin-19320 {:config {:plug-place [:#tables-view-plugin], :buttons [], :permission [:user], :view-columns [:enterpreneur.ssreou :enterpreneur.ownership_form :enterpreneur.vat_certificate :enterpreneur.individual_tax_number :enterpreneur.director :enterpreneur.accountant :enterpreneur.legal_address :enterpreneur.physical_address :enterpreneur.contacts_information], :name enterpreneur, :tables [:enterpreneur], :plugin-config-path [:enterpreneur :table :plugin-19320], :actions [], :insert-button true, :id :plugin-19320, :plugin-name table, :delete-button true, :query {:column [{:enterpreneur.id :enterpreneur.id} {:enterpreneur.ssreou :enterpreneur.ssreou} {:enterpreneur.ownership_form :enterpreneur.ownership_form} {:enterpreneur.vat_certificate :enterpreneur.vat_certificate} {:enterpreneur.individual_tax_number :enterpreneur.individual_tax_number} {:enterpreneur.director :enterpreneur.director} {:enterpreneur.accountant :enterpreneur.accountant} {:enterpreneur.legal_address :enterpreneur.legal_address} {:enterpreneur.physical_address :enterpreneur.physical_address} {:enterpreneur.contacts_information :enterpreneur.contacts_information}]}, :table-name :enterpreneur, :model [:enterpreneur.ssreou :enterpreneur.ownership_form :enterpreneur.vat_certificate :enterpreneur.individual_tax_number :enterpreneur.director :enterpreneur.accountant :enterpreneur.legal_address :enterpreneur.physical_address :enterpreneur.contacts_information]}, :toolkit {:model-id :enterpreneur.id, :insert #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/fn--16726], :delete-expression #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/delete-expression--16720], :select-expression #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/select-expression--16723], :update #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/fn--16728], :delete #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/fn--16730], :update-expression #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/update-expression--16716], :table-meta {:description nil, :ref {:front-references nil, :back-references nil}, :allow-linking? true, :field enterpreneur, :representation enterpreneur, :is-linker? false, :allow-modifing? true, :allow-deleting? true, :is-system? false}, :insert-expression #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/insert-expression--16718], :columns-meta [{:description nil, :private? false, :default-value nil, :editable? true, :field :ssreou, :column-type [:tinytext :nnull], :component-type [:textarea], :representation ssreou, :field-qualified :enterpreneur.ssreou} {:description nil, :private? false, :default-value nil, :editable? true, :field :ownership_form, :column-type [:varchar-100 :default :null], :component-type [:text], :representation ownership_form, :field-qualified :enterpreneur.ownership_form} {:description nil, :private? false, :default-value nil, :editable? true, :field :vat_certificate, :column-type [:tinytext :default :null], :component-type [:textarea], :representation vat_certificate, :field-qualified :enterpreneur.vat_certificate} {:description nil, :private? false, :default-value nil, :editable? true, :field :individual_tax_number, :column-type [:varchar-100 :default :null], :component-type [:text], :representation individual_tax_number, :field-qualified :enterpreneur.individual_tax_number} {:description nil, :private? false, :default-value nil, :editable? true, :field :director, :column-type [:varchar-100 :default :null], :component-type [:text], :representation director, :field-qualified :enterpreneur.director} {:description nil, :private? false, :default-value nil, :editable? true, :field :accountant, :column-type [:varchar-100 :default :null], :component-type [:text], :representation accountant, :field-qualified :enterpreneur.accountant} {:description nil, :private? false, :default-value nil, :editable? true, :field :legal_address, :column-type [:varchar-100 :default :null], :component-type [:text], :representation legal_address, :field-qualified :enterpreneur.legal_address} {:description nil, :private? false, :default-value nil, :editable? true, :field :physical_address, :column-type [:varchar-100 :default :null], :component-type [:text], :representation physical_address, :field-qualified :enterpreneur.physical_address} {:description nil, :private? false, :default-value nil, :editable? true, :field :contacts_information, :column-type [:mediumtext :default :null], :component-type [:textarea], :representation contacts_information, :field-qualified :enterpreneur.contacts_information}], :s
;; elect #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/fn--16732]}}, 
;;                          :plugin-19390 {:config {:plug-place [:#tables-view-plugin], :buttons [], :permission [:user], :view-columns [:enterpreneur.ssreou :enterpreneur.ownership_form :enterpreneur.vat_certificate :enterpreneur.individual_tax_number :enterpreneur.director :enterpreneur.accountant :enterpreneur.legal_address :enterpreneur.physical_address :enterpreneur.contacts_information], :name enterpreneur, :tables [:enterpreneur], :plugin-config-path [:enterpreneur :table :plugin-19390], :actions [], :insert-button true, :id :plugin-19390, :plugin-name table, :delete-button true, :query {:column [{:enterpreneur.id :enterpreneur.id} {:enterpreneur.ssreou :enterpreneur.ssreou} {:enterpreneur.ownership_form :enterpreneur.ownership_form} {:enterpreneur.vat_certificate :enterpreneur.vat_certificate} {:enterpreneur.individual_tax_number :enterpreneur.individual_tax_number} {:enterpreneur.director :enterpreneur.director} {:enterpreneur.accountant :enterpreneur.accountant} {:enterpreneur.legal_address :enterpreneur.legal_address} {:enterpreneur.physical_address :enterpreneur.physical_address} {:enterpreneur.contacts_information :enterpreneur.contacts_information}]}, :table-name :enterpreneur, :model [:enterpreneur.ssreou :enterpreneur.ownership_form :enterpreneur.vat_certificate :enterpreneur.individual_tax_number :enterpreneur.director :enterpreneur.accountant :enterpreneur.legal_address :enterpreneur.physical_address :enterpreneur.contacts_information]}, :toolkit {:model-id :enterpreneur.id, :insert #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/fn--16726], :delete-expression #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/delete-expression--16720], :select-expression #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/select-expression--16723], :update #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/fn--16728], :delete #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/fn--16730], :update-expression #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/update-expression--16716], :table-meta {:description nil, :ref {:front-references nil, :back-references nil}, :allow-linking? true, :field enterpreneur, :representation enterpreneur, :is-linker? false, :allow-modifing? true, :allow-deleting? true, :is-system? false}, :insert-expression #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/insert-expression--16718], :columns-meta [{:description nil, :private? false, :default-value nil, :editable? true, :field :ssreou, :column-type [:tinytext :nnull], :component-type [:textarea], :representation ssreou, :field-qualified :enterpreneur.ssreou} {:description nil, :private? false, :default-value nil, :editable? true, :field :ownership_form, :column-type [:varchar-100 :default :null], :component-type [:text], :representation ownership_form, :field-qualified :enterpreneur.ownership_form} {:description nil, :private? false, :default-value nil, :editable? true, :field :vat_certificate, :column-type [:tinytext :default :null], :component-type [:textarea], :representation vat_certificate, :field-qualified :enterpreneur.vat_certificate} {:description nil, :private? false, :default-value nil, :editable? true, :field :individual_tax_number, :column-type [:varchar-100 :default :null], :component-type [:text], :representation individual_tax_number, :field-qualified :enterpreneur.individual_tax_number} {:description nil, :private? false, :default-value nil, :editable? true, :field :director, :column-type [:varchar-100 :default :null], :component-type [:text], :representation director, :field-qualified :enterpreneur.director} {:description nil, :private? false, :default-value nil, :editable? true, :field :accountant, :column-type [:varchar-100 :default :null], :component-type [:text], :representation accountant, :field-qualified :enterpreneur.accountant} {:description nil, :private? false, :default-value nil, :editable? true, :field :legal_address, :column-type [:varchar-100 :default :null], :component-type [:text], :representation legal_address, :field-qualified :enterpreneur.legal_address} {:description nil, :private? false, :default-value nil, :editable? true, :field :physical_address, :column-type [:varchar-100 :default :null], :component-type [:text], :representation physical_address, :field-qualified :enterpreneur.physical_address} {:description nil, :private? false, :default-value nil, :editable? true, :field :contacts_information, :column-type [:mediumtext :default :null], :component-type [:textarea], :representation contacts_information, :field-qualified :enterpreneur.contacts_information}], :select #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/fn--16732]}}, 
;;                          :plugin-19458 {:config {:plug-place [:#tables-view-plugin], :buttons [], :permission [:user], :view-columns [:enterpreneur.ssreou :enterpreneur.ownership_form :enterpreneur.vat_certificate :enterpreneur.individual_tax_number :enterpreneur.director :enterpreneur.accountant :enterpreneur.legal_address :enterpreneur.physical_address :enterpreneur.contacts_information], :name enterpreneur, :tables [:enterpreneur], :plugin-config-path [:enterpreneur :table :plugin-19458], :actions [], :insert-button true, :id :plugin-19458, :plugin-name table, :delete-button true, :query {:column [{:enterpreneur.id :enterpreneur.id} {:enterpreneur.ssreou :enterpreneur.ssreou} {:enterpreneur.ownership_form :enterpreneur.ownership_form} {:enterpreneur.vat_certificate :enterpreneur.vat_certificate} {:enterpreneur.individual_tax_number :enterpreneur.individual_tax_number} {:enterpreneur.director :enterpreneur.director} {:enterpreneur.accountant :enterpreneur.accountant} {:enterpreneur.legal_address :enterpreneur.legal_address} {:enterpreneur.physical_address :enterpreneur.physical_address} {:enterpreneur.contacts_information :enterpreneur.contacts_information}]}, :table-name :enterpreneur, :model [:enterpreneur.ssreou :enterpreneur.ownership_form :enterpreneur.vat_certificate :enterpreneur.individual_tax_number :enterpreneur.director :enterpreneur.accountant :enterpreneur.legal_address :enterpreneur.physical_address :enterpreneur.contacts_information]}, :toolkit {:model-id :enterpreneur.id, :insert #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/fn--16726], :delete-expression #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/delete-expression--16720], :select-expression #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/select-expression--16723], :update #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/fn--16728], :delete #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/fn--16730], :update-expression #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/update-expression--16716], :table-meta {:description nil, :ref {:front-references nil, :back-references nil}, :allow-linking? true, :field enterpreneur, :representation enterpreneur, :is-linker? false, :allow-modifing? true, :allow-deleting? true, :is-system? false}, :insert-expression #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/insert-expression--16718], :columns-meta [{:description nil, :private? false, :default-value nil, :editable? true, :field :ssreou, :column-type [:tinytext :nnull], :component-type [:textarea], :representation ssreou, :field-qualified :enterpreneur.ssreou} {:description nil, :private? false, :default-value nil, :editable? true, :field :ownership_form, :column-type [:varchar-100 :default :null], :component-type [:text], :representation ownership_form, :field-qualified :enterpreneur.ownership_form} {:description nil, :private? false, :default-value nil, :editable? true, :field :vat_certificate, :column-type [:tinytext :default :null], :component-type [:textarea], :representation vat_certificate, :field-qualified :enterpreneur.vat_certificate} {:description nil, :private? false, :default-value nil, :editable? true, :field :individual_tax_number, :column-type [:varchar-100 :default :null], :component-type [:text], :representation individual_tax_number,
;;  :field-qualified :enterpreneur.individual_tax_number} {:description nil, :private? false, :default-value nil, :editable? true, :field :director, :column-type [:varchar-100 :default :null], :component-type [:text], :representation director, :field-qualified :enterpreneur.director} {:description nil, :private? false, :default-value nil, :editable? true, :field :accountant, :column-type [:varchar-100 :default :null], :component-type [:text], :representation accountant, :field-qualified :enterpreneur.accountant} {:description nil, :private? false, :default-value nil, :editable? true, :field :legal_address, :column-type [:varchar-100 :default :null], :component-type [:text], :representation legal_address, :field-qualified :enterpreneur.legal_address} {:description nil, :private? false, :default-value nil, :editable? true, :field :physical_address, :column-type [:varchar-100 :default :null], :component-type [:text], :representation physical_address, :field-qualified :enterpreneur.physical_address} {:description nil, :private? false, :default-value nil, :editable? true, :field :contacts_information, :column-type [:mediumtext :default :null], :component-type [:textarea], :representation contacts_information, :field-qualified :enterpreneur.contacts_information}], :select #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/fn--16732]}}, 
;;                          :plugin-19526 {:config {:plug-place [:#tables-view-plugin], :buttons [], :permission [:user], :view-columns [:enterpreneur.ssreou :enterpreneur.ownership_form :enterpreneur.vat_certificate :enterpreneur.individual_tax_number :enterpreneur.director :enterpreneur.accountant :enterpreneur.legal_address :enterpreneur.physical_address :enterpreneur.contacts_information], :name enterpreneur, :tables [:enterpreneur], :plugin-config-path [:enterpreneur :table :plugin-19526], :actions [], :insert-button true, :id :plugin-19526, :plugin-name table, :delete-button true, :query {:column [{:enterpreneur.id :enterpreneur.id} {:enterpreneur.ssreou :enterpreneur.ssreou} {:enterpreneur.ownership_form :enterpreneur.ownership_form} {:enterpreneur.vat_certificate :enterpreneur.vat_certificate} {:enterpreneur.individual_tax_number :enterpreneur.individual_tax_number} {:enterpreneur.director :enterpreneur.director} {:enterpreneur.accountant :enterpreneur.accountant} {:enterpreneur.legal_address :enterpreneur.legal_address} {:enterpreneur.physical_address :enterpreneur.physical_address} {:enterpreneur.contacts_information :enterpreneur.contacts_information}]}, :table-name :enterpreneur, :model [:enterpreneur.ssreou :enterpreneur.ownership_form :enterpreneur.vat_certificate :enterpreneur.individual_tax_number :enterpreneur.director :enterpreneur.accountant :enterpreneur.legal_address :enterpreneur.physical_address :enterpreneur.contacts_information]}, :toolkit {:model-id :enterpreneur.id, :insert #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/fn--16726], :delete-expression #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/delete-expression--16720], :select-expression #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/select-expression--16723], :update #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/fn--16728], :delete #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/fn--16730], :update-expression #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/update-expression--16716], :table-meta {:description nil, :ref {:front-references nil, :back-references nil}, :allow-linking? true, :field enterpreneur, :representation enterpreneur, :is-linker? false, :allow-modifing? true, :allow-deleting? true, :is-system? false}, :insert-expression #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/insert-expression--16718], :columns-meta [{:description nil, :private? false, :default-value nil, :editable? true, :field :ssreou, :column-type [:tinytext :nnull], :component-type [:textarea], :representation ssreou, :field-qualified :enterpreneur.ssreou} {:description nil, :private? false, :default-value nil, :editable?
;;  true, :field :ownership_form, :column-type [:varchar-100 :default :null], :component-type [:text], :representation ownership_form, :field-qualified :enterpreneur.ownership_form} {:description nil, :private? false, :default-value nil, :editable? true, :field :vat_certificate, :column-type [:tinytext :default :null], :component-type [:textarea], :representation vat_certificate, :field-qualified :enterpreneur.vat_certificate} {:description nil, :private? false, :default-value nil, :editable? true, :field :individual_tax_number, :column-type [:varchar-100 :default :null], :component-type [:text], :representation individual_tax_number, :field-qualified :enterpreneur.individual_tax_number} {:description nil, :private? false, :default-value nil, :editable? true, :field :director, :column-type [:varchar-100 :default :null], :component-type [:text], :representation director, :field-qualified :enterpreneur.director} {:description nil, :private? false, :default-value nil, :editable? true, :field :accountant, :column-type [:varchar-100 :default :null], :component-type [:text], :representation accountant, :field-qualified :enterpreneur.accountant} {:description nil, :private? false, :default-value nil, :editable? true, :field :legal_address, :column-type [:varchar-100 :default :null], :component-type [:text], :representation legal_address, :field-qualified :enterpreneur.legal_address} {:description nil, :private? false, :default-value nil, :editable? true, :field :physical_address, :column-type [:varchar-100 :default :null], :component-type [:text], :representation physical_address, :field-qualified :enterpreneur.physical_address} {:description nil, :private? false, :default-value nil, :editable? true, :field :contacts_information, :column-type [:mediumtext :default :null], :component-type [:textarea], :representation contacts_information, :field-qualified :enterpreneur.contacts_information}], :select #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/fn--16732]}}}}, 

;;   :user {:table {:plugin-17211 
;;                  {:config {:plug-place [:#tables-view-plugin], :buttons [{:action :add-multiply-users-insert, :title Auto generate users}], :permission [:user], :view-columns [:user.login :user.first_name :user.last_name :permission.permission_name], :name user-override, :tables [:user :permission], :plugin-config-path [:user :table :plugin-17211], :actions {:add-multiply-users-insert (fn [state] (let [{user-start :user-start, user-end :user-end} (clojure.core/deref state)] (println (map (fn* [p1__17173#] (hash-map :user.login (str user p1__17173#) :user.password 1234 :user.last_name (str user p1__17173#) :user.first_name (str user p1__17173#) :user.id_permission 2)) (range user-start (+ 1 user-end))))))}, :insert-button true, :id :plugin-17211, :plugin-name table, :delete-button false, :query {:inner-join [:user->permission], :column [{:user.id :user.id} {:user.login :user.login} {:user.password :user.password} {:user.first_name :user.first_name} {:user.last_name :user.last_name} {:user.id_permission :user.id_permission} {:permission.id :permission.id} {:permission.permission_name :permission.permission_name} {:permission.configuration :permission.configuration}]}, :table-name :user, :model [{:model-reprs Login, :model-param :user.login, :bind-args {:title :title}, :model-comp jarman.gui.gui-components/input-text-with-atom} :user.password :user.first_name :user.last_name :user.id_permission {:model-reprs Start user, :model-param :user-start, :model-comp jarman.gui.gui-components/input-int} {:model-reprs End user, :model-param :user-end, :model-comp jarman.gui.gui-components/input-int}]}, 
;;                   :toolkit {:model-id :user.id, :insert #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/fn--16726], :delete-expression #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/delete-expression--16720], :select-expression #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/select-expression--16723], :update #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/fn--16728], :delete #function[jarman.plugin.data-
;; toolkit/sql-crud-toolkit-constructor/fn--16730], :update-expression #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/update-expression--16716], :table-meta {:description nil, :ref {:front-references [:permission], :back-references nil}, :allow-linking? true, :field user, :representation user, :is-linker? false, :allow-modifing? true, :allow-deleting? true, :is-system? false}, :insert-expression #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/insert-expression--16718], :columns-meta [{:description nil, :private? false, :default-value nil, :editable? true, :field :login, :column-type [:varchar-100 :nnull], :component-type [:text], :representation login, :field-qualified :user.login} {:description nil, :private? false, :default-value nil, :editable? true, :field :password, :column-type [:varchar-100 :nnull], :component-type [:text], :representation password, :field-qualified :user.password} {:description nil, :private? false, :default-value nil, :editable? true, :field :first_name, :column-type [:varchar-100 :nnull], :component-type [:text], :representation first_name, :field-qualified :user.first_name} {:description nil, :private? false, :default-value nil, :editable? true, :field :last_name, :column-type [:varchar-100 :nnull], :component-type [:text], :representation last_name, :field-qualified :user.last_name} {:description nil, :private? false, :default-value nil, :editable? true, :field :id_permission, :column-type [:bigint-120-unsigned :nnull], :foreign-keys [{:id_permission :permission} {:delete :cascade, :update :cascade}], :component-type [:link], :representation id_permission, :field-qualified :user.id_permission, :key-table :permission}], :select #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/fn--16732]}}, 
;;                  :plugin-19265 
;;                  {:config {:plug-place [:#tables-view-plugin], :buttons [{:action :add-multiply-users-insert, :title Auto generate users}], :permission [:user], :view-columns [:user.login :user.first_name :user.last_name :permission.permission_name], :name user-override, :tables [:user :permission], :plugin-config-path [:user :table :plugin-19265], :actions {:add-multiply-users-insert (fn [state] (let [{user-start :user-start, user-end :user-end} (clojure.core/deref state)] (println (map (fn* [p1__19227#] (hash-map :user.login (str user p1__19227#) :user.password 1234 :user.last_name (str user p1__19227#) :user.first_name (str user p1__19227#) :user.id_permission 2)) (range user-start (+ 1 user-end))))))}, :insert-button true, :id :plugin-19265, :plugin-name table, :delete-button false, :query {:inner-join [:user->permission], :column [{:user.id :user.id} {:user.login :user.login} {:user.password :user.password} {:user.first_name :user.first_name} {:user.last_name :user.last_name} {:user.id_permission :user.id_permission} {:permission.id :permission.id} {:permission.permission_name :permission.permission_name} {:permission.configuration :permission.configuration}]}, :table-name :user, :model [{:model-reprs Login, :model-param :user.login, :bind-args {:title :title}, :model-comp jarman.gui.gui-components/input-text-with-atom} :user.password :user.first_name :user.last_name :user.id_permission {:model-reprs Start user, :model-param :user-start, :model-comp jarman.gui.gui-components/input-int} {:model-reprs End user, :model-param :user-end, :model-comp jarman.gui.gui-components/input-int}]}, :toolkit {:model-id :user.id, :insert #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/fn--16726], :delete-expression #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/delete-expression--16720], :select-expression #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/select-expression--16723], :update #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/fn--16728], :delete #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/fn--16730], :update-expression #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/update-expression--16716], :table-meta {:description nil, :ref {:front-references [:permission], :back-references nil}, :allow-linking? true, :field user, :representation user, :is-linker? false, :allow-modifing? true, :allow-deleting? true, :is-system? false}, :insert-expression #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/insert-expression--16718], :columns-meta [{:description nil, :private? false, :default-value nil, :editable? true, :field :login, :column-type [:varchar-100 :nnull], :component-type [:text], :representation login, :field-qualified :user.login} {:description nil, :private? false, :default-value nil, :editable? true, :field :password, :column-type [:varchar-100 :nnull], :component-type [:text], :representation password, :field-qualified :user.password} {:description nil, :private? false, :default-value nil, :editable? true, :field :first_name, :column-type [:varchar-100 :nnull], :component-type [:text], :representation first_name, :field-qualified :user.first_name} {:description nil, :private? false, :default-value nil, :editable? true, :field :last_name, :column-type [:varchar-100 :nnull], :component-type [:text], :representation last_name, :field-qualified :user.last_name} {:description nil, :private? false, :default-value nil, :editable? true, :field :id_permission, :column-type [:bigint-120-unsigned :nnull], :foreign-keys [{:id_permission :permission} {:delete :cascade, :update :cascade}], :component-type [:link], :representation id_permission, :field-qualified :user.id_permission, :key-table :permission}], :select #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/fn--16732]}}, 
;;                  :plugin-19339 
;;                  {:config {:plug-place [:#tables-view-plugin], :buttons [{:action :add-multiply-users-insert, :title Auto generate users}], :permission [:user], :view-columns [:user.login :user.first_name :user.last_name :permission.permission_name], :name user-override, :tables [:user :permission], :plugin-config-path [:user :table :plugin-19339], :actions {:add-multiply-users-insert (fn [state] (let [{user-start :user-start, user-end :user-end} (clojure.core/deref state)] (println (map (fn* [p1__19301#] (hash-map :user.login (str user p1__19301#) :user.password 1234 :user.last_name (str user p1__19301#) :user.first_name (str user p1__19301#) :user.id_permission 2)) (range user-start (+ 1 user-end))))))}, :insert-button true, :id :plugin-19339, :plugin-name table, :delete-button false, :query {:inner-join [:user->permission], :column [{:user.id :user.id} {:user.login :user.login} {:user.password :user.password} {:user.first_name :user.first_name} {:user.last_name :user.last_name} {:user.id_permission :user.id_permission} {:permission.id :permission.id} {:permission.permission_name :permission.permission_name} {:permission.configuration :permission.configuration}]}, :table-name :user, :model [{:model-reprs Login, :model-param :user.login, :bind-args {:title :title}, :model-comp jarman.gui.gui-components/input-text-with-atom} :user.password :user.first_name :user.last_name :user.id_permission {:model-reprs Start user, :model-param :user-start, :model-comp jarman.gui.gui-components/input-int} {:model-reprs End user, :model-param :user-end, :model-comp jarman.gui.gui-components/input-int}]}, :toolkit {:model-id :user.id, :insert #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/fn--16726], :delete-expression #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/delete-expression--16720], :select-expression #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/select-expression--16723], :update #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/fn--16728], :delete #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/fn--16730], :update-expression #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/update-expression--16716], :table-meta {:description nil, :ref {:front-references [:permission], :back-references nil}, :allow-linking? true, :field user, :representation user, :is-linker? false, :allow-modifing? true, :allow-deleting? true, :is-system? false}, :insert-expression #function[jarman.plugin.data-
;; toolkit/sql-crud-toolkit-constructor/insert-expression--16718], :columns-meta [{:description nil, :private? false, :default-value nil, :editable? true, :field :login, :column-type [:varchar-100 :nnull], :component-type [:text], :representation login, :field-qualified :user.login} {:description nil, :private? false, :default-value nil, :editable? true, :field :password, :column-type [:varchar-100 :nnull], :component-type [:text], :representation password, :field-qualified :user.password} {:description nil, :private? false, :default-value nil, :editable? true, :field :first_name, :column-type [:varchar-100 :nnull], :component-type [:text], :representation first_name, :field-qualified :user.first_name} {:description nil, :private? false, :default-value nil, :editable? true, :field :last_name, :column-type [:varchar-100 :nnull], :component-type [:text], :representation last_name, :field-qualified :user.last_name} {:description nil, :private? false, :default-value nil, :editable? true, :field :id_permission, :column-type [:bigint-120-unsigned :nnull], :foreign-keys [{:id_permission :permission} {:delete :cascade, :update :cascade}], :component-type [:link], :representation id_permission, :field-qualified :user.id_permission, :key-table :permission}], :select #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/fn--16732]}}, 
;;                  :plugin-19409 {:config {:plug-place [:#tables-view-plugin], :buttons [{:action :add-multiply-users-insert, :title Auto generate users}], :permission [:user], :view-columns [:user.login :user.first_name :user.last_name :permission.permission_name], :name user-override, :tables [:user :permission], :plugin-config-path [:user :table :plugin-19409], :actions {:add-multiply-users-insert (fn [state] (let [{user-start :user-start, user-end :user-end} (clojure.core/deref state)] (println (map (fn* [p1__19371#] (hash-map :user.login (str user p1__19371#) :user.password 1234 :user.last_name (str user p1__19371#) :user.first_name (str user p1__19371#) :user.id_permission 2)) (range user-start (+ 1 user-end))))))}, :insert-button true, :id :plugin-19409, :plugin-name table, :delete-button false, :query {:inner-join [:user->permission], :column [{:user.id :user.id} {:user.login :user.login} {:user.password :user.password} {:user.first_name :user.first_name} {:user.last_name :user.last_name} {:user.id_permission :user.id_permission} {:permission.id :permission.id} {:permission.permission_name :permission.permission_name} {:permission.configuration :permission.configuration}]}, :table-name :user, :model [{:model-reprs Login, :model-param :user.login, :bind-args {:title :title}, :model-comp jarman.gui.gui-components/input-text-with-atom} :user.password :user.first_name :user.last_name :user.id_permission {:model-reprs Start user, :model-param :user-start, :model-comp jarman.gui.gui-components/input-int} {:model-reprs End user, :model-param :user-end, :model-comp jarman.gui.gui-components/input-int}]},
;;                                 :toolkit {:model-id :user.id, :insert #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/fn--16726], :delete-expression #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/delete-expression--16720], :select-expression #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/select-expression--16723], :update #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/fn--16728], :delete #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/fn--16730], :update-expression #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/update-expression--16716], :table-meta {:description nil, :ref {:front-references [:permission], :back-references nil}, :allow-linking? true, :field user, :representation user, :is-linker? false, :allow-modifing? true, :allow-deleting? true, :is-system? false}, :insert-expression #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/insert-expression--16718], :columns-meta [{:description nil, :private? false, :default-value nil, :editable? true, :field :login, :column-type [:varchar-100 :nnull], :component-type [:text], :representation login, :field-qualified :user.login} {:description nil, :private? false, :default-value nil, :editable? true, :field :password, :column-type [:varchar-100 :nnull], :component-type [:text], :representation password, :field-qualified :user.password} {:description nil, :private? false, :default-value nil, :editable? true, :field :first_name, :column-type [:varchar-100 :nnull], :component-type [:text], :representation first_name, :field-qualified :user.first_name} {:description nil, :private? false, :default-value nil, :editable? true, :field :last_name, :column-type [:varchar-100 :nnull], :component-type [:text], :representation last_name, :field-qualified :user.last_name} {:description nil, :private? false, :default-value nil, :editable? true, :field :id_permission, :column-type [:bigint-120-unsigned :nnull], :foreign-keys [{:id_permission :permission} {:delete :cascade, :update :cascade}], :component-type [:link], :representation id_permission, :field-qualified :user.id_permission, :key-table :permission}], :select #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/fn--16732]}}, 
;;                  :plugin-19477 
;;                  {:config {:plug-place [:#tables-view-plugin], :buttons [{:action :add-multiply-users-insert, :title Auto generate users}]
;;                            :permission [:user], :view-columns [:user.login :user.first_name :user.last_name :permission.permission_name], :name user-override, :tables [:user :permission], :plugin-config-path [:user :table :plugin-19477], :actions {:add-multiply-users-insert (fn [state] (let [{user-start :user-start, user-end :user-end} (clojure.core/deref state)] (println (map (fn* [p1__19439#] (hash-map :user.login (str user p1__19439#) :user.password 1234 :user.last_name (str user p1__19439#) :user.first_name (str user p1__19439#) :user.id_permission 2)) (range user-start (+ 1 user-end))))))}, :insert-button true, :id :plugin-19477, :plugin-name table, :delete-button false, :query {:inner-join [:user->permission], :column [{:user.id :user.id} {:user.login :user.login} {:user.password :user.pas
;;                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               sword} {:user.first_name :user.first_name} {:user.last_name :user.last_name} {:user.id_permission :user.id_permission} {:permission.id :permission.id} {:permission.permission_name :permission.permission_name} {:permission.configuration :permission.configuration}]}, :table-name :user, :model [{:model-reprs Login, :model-param :user.login, :bind-args {:title :title}, :model-comp jarman.gui.gui-components/input-text-with-atom} :user.password :user.first_name :user.last_name :user.id_permission {:model-reprs Start user, :model-param :user-start, :model-comp jarman.gui.gui-components/input-int} {:model-reprs End user, :model-param :user-end, :model-comp jarman.gui.gui-components/input-int}]}, 
;;                   :toolkit {:model-id :user.id, :insert #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/fn--16726], :delete-expression #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/delete-expression--16720], :select-expression #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/select-expression--16723], :update #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/fn--16728], :delete #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/fn--16730], :update-expression #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/update-expression--16716], :table-meta {:description nil, :ref {:front-references [:permission], :back-references nil}, :allow-linking? true, :field user, :representation user, :is-linker? false, :allow-modifing? true, :allow-deleting? true, :is-system? false}, :insert-expression #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/insert-expression--16718], :columns-meta [{:description nil, :private? false, :default-value nil, :editable? true, :field :login, :column-type [:varchar-100 :nnull], :component-type [:text], :representation login, :field-qualified :user.login} {:description nil, :private? false, :default-value nil, :editable? true, :field :password, :column-type [:varchar-100 :nnull], :component-type [:text], :representa
;; tion password, :field-qualified :user.password} {:description nil, :private? false, :default-value nil, :editable? true, :field :first_name, :column-type [:varchar-100 :nnull], :component-type [:text], :representation first_name, :field-qualified :user.first_name} {:description nil, :private? false, :default-value nil, :editable? true, :field :last_name, :column-type [:varchar-100 :nnull], :component-type [:text], :representation last_name, :field-qualified :user.last_name} {:description nil, :private? false, :default-value nil, :editable? true, :field :id_permission, :column-type [:bigint-120-unsigned :nnull], :foreign-keys [{:id_permission :permission} {:delete :cascade, :update :cascade}], :component-type [:link], :representation id_permission, :field-qualified :user.id_permission, :key-table :permission}], :select #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/fn--16732]}}, :plugin-19545 {:config {:plug-place [:#tables-view-plugin], :buttons [{:action :add-multiply-users-insert, :title Auto generate users}], :permission [:user], :view-columns [:user.login :user.first_name :user.last_name :permission.permission_name], :name user-override, :tables [:user :permission], :plugin-config-path [:user :table :plugin-19545], :actions {:add-multiply-users-insert (fn [state] (let [{user-start :user-start, user-end :user-end} (clojure.core/deref state)] (println (map (fn* [p1__19507#] (hash-map :user.login (str user p1__19507#) :user.password 1234 :user.last_name (str user p1__19507#) :user.first_name (str user p1__19507#) :user.id_permission 2)) (range user-start (+ 1 user-end))))))}, :insert-button true, :id :plugin-19545, :plugin-name table, :delete-button false, :query {:inner-join [:user->permission], :column [{:user.id :user.id} {:user.login :user.login} {:user.password :user.password} {:user.first_name :user.first_name} {:user.last_name :user.last_name} {:user.id_permission :user.id_permission} {:permission.id :permission.id} {:permission.permission_name :permission.permission_name} {:permission.configuration :permission.configuration}]}, :table-name :user, :model [{:model-reprs Login, :model-param :user.login, :bind-args {:title :title}, :model-comp jarman.gui.gui-components/input-text-with-atom} :user.password :user.first_name :user.last_name :user.id_permission {:model-reprs Start user, :model-param :user-start, :model-comp jarman.gui.gui-components/input-int} {:model-reprs End user, :model-param :user-end, :model-comp jarman.gui.gui-components/input-int}]}, :toolkit {:model-id :user.id, :insert #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/fn--16726], :delete-expression #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/delete-expression--16720], :select-expression #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/select-expression--16723], :update #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/fn--16728], :delete #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/fn--16730], :update-expression #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/update-expression--16716], :table-meta {:description nil, :ref {:front-references [:permission], :back-references nil}, :allow-linking? true, :field user, :representation user, :is-linker? false, :allow-modifing? true, :allow-deleting? true, :is-system? false}, :insert-expression #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/insert-expression--16718], :columns-meta [{:description nil, :private? false, :default-value nil, :editable? true, :field :login, :column-type [:varchar-100 :nnull], :component-type [:text], :representation login, :field-qualified :user.login} {:description nil, :private? false, :default-value nil, :editable? true, :field :password, :column-type [:varchar-100 :nnull], :component-type [:text], :representation password, :field-qualified :user.password} {:description nil, :private? false, :default-value nil, :editable? true, :field :first_name, :column-type [:varchar-100 :nnull], :component-type [:text], :representation firs
;; t_name, :field-qualified :user.first_name} {:description nil, :private? false, :default-value nil, :editable? true, :field :last_name, :column-type [:varchar-100 :nnull], :component-type [:text], :representation last_name, :field-qualified :user.last_name} {:description nil, :private? false, :default-value nil, :editable? true, :field :id_permission, :column-type [:bigint-120-unsigned :nnull], :foreign-keys [{:id_permission :permission} {:delete :cascade, :update :cascade}], :component-type [:link], :representation id_permission, :field-qualified :user.id_permission, :key-table :permission}], :select #function[jarman.plugin.data-toolkit/sql-crud-toolkit-constructor/fn--16732]}}}}}