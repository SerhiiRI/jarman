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
   [jarman.tools.lang :refer :all :as l]
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


(defn- create-jarman-table-plugin [table-name]
  (let [meta-table (first (mt/getset! table-name))
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
          (list 'jarman-table
                :name name-of-table
                :plug-place [:#tables-view-plugin]
                :tables tables
                :model models
                :query (if-not (empty? joines)
                         {:inner-join joines :columns columns}
                         {:column columns})))))

;; (create-jarman-table-plugin :seal)
(mapv create-jarman-table-plugin [:permission :user :enterpreneur :point_of_sale :cache_register :point_of_sale_group :point_of_sale_group_links :seal :repair_contract :service_contract :service_contract_month])

(defn- gui-table-model-columns [table-list table-column-list]
  (let
   [on-text  (fn [m v] (into m {:text (:representation v)}))
    on-class (fn [m v] (if (contains? v :class) (into m {:class (:class v)}) m))]
    (mapv (fn [[k v]] (-> {:key k} (on-class v) (on-text v)))
          (jarman-table-columns-list table-list table-column-list))))

(defn- gui-table-model [model-columns data-loader]
  (fn [] [:columns model-columns :rows (data-loader)]))

(defn- gui-table [model]
  (fn [listener-fn]
    (let [TT (swingx/table-x :model (model))]
      (c/listen TT :selection (fn [e] (listener-fn (seesaw.table/value-at TT (c/selection TT)))))
      (c/config! TT :horizontal-scroll-enabled? true)
      (c/config! TT :show-grid? false)
      (c/config! TT :show-horizontal-lines? true)
      (c/scrollable TT :hscroll :as-needed :vscroll :as-needed :border nil))))

(defn- create-table [configuration toolkit-map]
  (let [view (:model configuration) tables (:tables configuration)]
    (if (and view tables)
      (let [model-columns (gui-table-model-columns tables view)
            table-model (gui-table-model model-columns (:select toolkit-map))]
        {:table-model table-model
         :table (gui-table table-model)}))))

(defn- construct-dialog [table-fn selected frame]
  (let [dialog (seesaw.core/custom-dialog :modal? true :width 600 :height 400 :title "Select component")
        table (table-fn (fn [model] (seesaw.core/return-from-dialog dialog model)))
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


(defn- d-component
  "Set default component to form or override it"
  [coll]
  (let [default (gcomp/inpose-label (:title coll) (apply calendar/calendar-with-atom :store-id (:store-id coll) :local-changes (:local-changes coll) (if (:val coll) [:set-date (:val coll)] [])))]
    (try
      (if (l/in? (map #(:column %) (:override coll)) (:store-id coll)) ((:fun (first (:override coll))) coll))
      (catch Exception e (do (println "Can not override d-component:" (:title coll)) default)))
    default))


(defn- a-component
  "Set default component to form or override it"
  [coll]
  (let [default (gcomp/inpose-label (:title coll) (apply gcomp/input-text-area :store-id (:store-id coll) :local-changes (:local-changes coll) :editable? (:editable? coll) (if (:val coll) [:val (:val coll)] [])))]
    (try
      (if (l/in? (map #(:column %) (:override coll)) (:store-id coll)) ((:fun (first (:override coll))) coll))
      (catch Exception e (do (println "Can not override a-component:" (:title coll)) default)))
    default))


(defn- n-component
  "Set default component to form or override it"
  [coll]
  (let [default (gcomp/inpose-label (:title coll) (apply gcomp/input-int :store-id (:store-id coll) :local-changes (:local-changes coll) (if (:val coll) [:val (:val coll)] [])))]
    (try
      (if (l/in? (map #(:column %) (:override coll)) (:store-id coll)) ((:fun (first (:override coll))) coll))
      (catch Exception e (do (println "Can not override n-component:" (:title coll)) default)))
    default))

(defn- i-component
  "Set default component to form or override it"
  [coll]
  (let [default (gcomp/inpose-label (:title coll) (apply gcomp/input-text-with-atom :store-id (:store-id coll) :local-changes (:local-changes coll) :editable? (:editable? coll) (if (:val coll) [:val (:val coll)] [])))]
    (if (l/in? (map #(:column %) (:override coll)) (:store-id coll))
      (try
        ((:fun (first (:override coll))) coll)
        (catch Exception e (do (println "Can not override i-component:" (:title coll)) default)))
      default)))




(defn convert-map-to-component [m]
  (println m) "map")

(defn convert-key-to-component [local-changes meta-data model k]
  (let [meta            (k meta-data)
        field-qualified (:field-qualified meta)
        title           (:representation  meta)
        editable?       (:editable?       meta)
        comp-type       (:component-type  meta)
        val             (l/rift (str (k model)) "")
        map-coll {:title title :store-id field-qualified :local-changes local-changes :editable? editable? :val val}]
    (println k meta comp-type)
    (cond
      (l/in? comp-type "d")
      (if (empty? model) (d-component map-coll) (d-component (conj map-coll {:val (if (empty? val) nil val)})))
      (l/in? comp-type "a") ;; Text area
      (if (empty? model) (a-component map-coll) (a-component (conj map-coll {:val val})))
      (l/in? comp-type "n") ;; Numbers Int
      (if (empty? model) (n-component map-coll) (n-component (conj map-coll {:val val})))
      (l/in? comp-type "i") ;; Basic Input
      (if (empty? model) (i-component map-coll) (i-component (conj map-coll {:val val})))
      ;; (l/in? comp-type "l")
      ;; (do ;; Add label with enable false input-text. Can run micro window with table to choose some record and retunr id.
      ;;   (let [key-table  (keyword (get meta :key-table))
      ;;         connected-table-conf  (get-in global-configuration [key-table :plug/jarman-table :configuration])
      ;;         connected-table-data  (get-in global-configuration [key-table :plug/jarman-table :data-toolkit])
      ;;         selected-representation (fn [dialog-model-view returned-from-dialog]
      ;;                                   (->> (get dialog-model-view :model)
      ;;                                        (map #(get-in returned-from-dialog [%]))
      ;;                                        (filter some?)
      ;;                                        (string/join ", ")))
      ;;         v (selected-representation connected-table-conf k)]
      ;;     (if-not (nil? (get k field-qualified)) (swap! local-changes (fn [storage] (assoc storage field-qualified (get-in model [field-qualified])))))
      ;;     (gcomp/inpose-label title (gcomp/input-text-with-atom :local-changes local-changes :editable? false :val v
      ;;                                                           :onClick (fn [e]
      ;;                                                                      (let [selected (construct-dialog (get (create-table connected-table-conf connected-table-data) :table) field-qualified (c/to-frame e))]
      ;;                                                                        (if-not (nil? (get selected (get connected-table-data :model-id)))
      ;;                                                                          (do (c/config! e :text (selected-representation connected-table-conf selected))
      ;;                                                                              (swap! local-changes (fn [storage] (assoc storage field-qualified (get selected (get connected-table-data :model-id)))))))))))))
      )
      ))


(defn convert-model-to-components-list [local-changes meta-data model coll]
  (doall (->> coll
              (map #(cond
                      (map? %)     (convert-map-to-component %)
                      (keyword? %) (convert-key-to-component local-changes meta-data model %)))
              (filter #(not (nil? %)))
              )))

(defn convert-metadata-vec-to-map
  "Description;
     Convert [{:x a :field-qualified b}{:d w :field-qualified f}] => {:b {:x a :field-qualified b} :f {:d w :field-qualified f}}
   "
  [coll]  (into {} (doall (map (fn [m] {(keyword (:field-qualified m)) m}) coll))))

(def build-input-form
  (fn [data-toolkit
       configuration
       global-configuration
       & {:keys [model
                 more-comps
                 button-template
                 start-focus
                 export-comp
                 update
                 alerts]
          :or {model []
               more-comps [(c/label)]
               button-template (fn [title f] (gcomp/button-basic title :onClick f))
               start-focus nil
               export-comp nil
               update false
               alerts nil}}]
    ;; (println)
    ;; (println "config: " data-toolkit)
    ;; (println)
    (let [local-changes (atom {})
          meta-data (convert-metadata-vec-to-map (:columns-meta data-toolkit))
          components (convert-model-to-components-list local-changes meta-data model (:model configuration))
          panel (smig/mig-panel :constraints ["wrap 1" "0px[grow, fill]0px" "0px[fill]0px"]
                                :border (sborder/empty-border :thickness 10)
                                :items [[(c/label)]])]
      (println)
      (println "Comps: " components)
      (println)
      )

    ;; (let [local-changes (atom {})
    ;;       metadata (:columns-meta data-toolkit)
    ;;       inser-or-update (if (empty? model) "Insert new data" "Update record")
    ;;       delete "Remove selected record"
    ;;       vgap (fn [size] (c/label :border (sborder/empty-border :top size)))

          ;; components (concat
          ;;             (filter #(not (nil? %))
          ;;                     (map (fn [meta]
          ;;                            )
          ;;                          metadata))
          ;;             [(vgap 20)]
          ;;             [(button-template inser-or-update (fn [e]
          ;;                                                 (if (empty? model)
          ;;                                                   (do
          ;;                                                     (println "Expression insert" ((:insert data-toolkit) (merge {(keyword (str (get (:table-meta data-toolkit) :field) ".id")) nil} (first (merge model @local-changes))))))
          ;;                                                   (do
          ;;                                                     (println "Expression update" ((:update data-toolkit) (merge model @local-changes)))))
          ;;                                                 ((@gseed/jarman-views-service :reload))))]
          ;;             (if (empty? model) [] [(button-template delete (fn [e]
          ;;                                                              (println "Expression delete" ((:delete data-toolkit) {(keyword (str (get (:table-meta data-toolkit) :field) ".id")) (get model (keyword (str (get (:table-meta data-toolkit) :field) ".id")))}))
          ;;                                                              ((@gseed/jarman-views-service :reload))))])
          ;;             [(vgap 10)]
          ;;             [more-comps]
          ;;             [(if (nil? export-comp) (c/label) (export-comp (get model (:model-id data-toolkit))))])
      ;;     builded (c/config! panel :items (gtool/join-mig-items components))]
      ;; ;; (println "Build prepare components")
      ;; (if-not (nil? start-focus) (reset! start-focus (last (u/children (first components)))))
      ;; builded)
    (c/label :text "Testing")))


;; [{:description nil, 
;;   :private? false, 
;;   :default-value nil, 
;;   :editable? true, 
;;   :field :login, 
;;   :column-type [:varchar-100 :nnull], 
;;   :component-type [i], 
;;   :representation login, 
;;   :field-qualified :user.login}
;;  {:description nil, :private? false, :default-value nil, :editable? true, :field :password, :column-type [:varchar-100 :nnull], :component-type [i], :representation password, :field-qualified :user.password} {:description nil, :private? false, :default-value nil, :editable? true, :field :first_name, :column-type [:varchar-100 :nnull], :component-type [i], :representation first_name, :field-qualified :user.first_name} {:description nil, :private? false, :default-value nil, :editable? true, :field :last_name, :column-type [:varchar-100 :nnull], :component-type [i], :representation last_name, :field-qualified :user.last_name} {:description nil, :private? false, :default-value nil, :editable? true, :field :id_permission, :column-type [:bigint-120-unsigned :nnull], :foreign-keys [{:id_permission :permission} {:delete :cascade, :update :cascade}], :component-type [l], :representation id_permission, :field-qualified :user.id_permission, :key-table :permission}]


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

;;########################################
;; ### Defview configuration for user ###
;;########################################
;; {:plug-place [:#tables-view-plugin], ;; Space in jarman were can be inject plugin's view invoker
;;  :buttons [{:action :add-multiply-users-insert, 
;;             :text "Auto generate users"}], ;; Add custom buttons
;;  :permission [:user], ;; Who have access and who can display plugin
;;  :view-columns [:user.login 
;;                 :user.first_name 
;;                 :user.last_name 
;;                 :permission.permission_name], ;; Avaliable column to view in table
;;  :name user, 
;;  :tables [:user :permission], 
;;  :actions {:add-multiply-users-insert #function[jarman.logic.view-manager/eval29892/fn--29893]}, 
;;  :query {:inner-join [:user->permission]
;;          :columns ({:user.id :user.id} 
;;                    {:user.login :user.login} 
;;                    {:user.password :user.password} 
;;                    {:user.first_name :user.first_name} 
;;                    {:user.last_name :user.last_name} 
;;                    {:user.id_permission :user.id_permission} 
;;                    {:permission.id :permission.id} 
;;                    {:permission.permission_name :permission.permission_name} 
;;                    {:permission.configuration :permission.configuration})}, 
;;  :table-name :user, 
;;  :model [{:model-reprs Login, 
;;           :model-param :user.login, 
;;           :bind-args {:title :title, :store-id :store-id, :local-changes :local-changes, :val :val}, 
;;           :model-comp 'gcomp/input-text-area-label} ;; Overriding component syntax
;;          :user.password 
;;          :user.first_name 
;;          :user.last_name 
;;          :user.id_permission 
;;          {:model-reprs Start user, 
;;           :model-param :user-start, 
;;           :model-comp 'gcomp/input-int} ;; Add custom component syntax
;;          {:model-reprs End user, 
;;           :model-param :user-end, 
;;           :model-comp 'gcomp/input-int}]} ;; Add custom component syntax


(def auto-builder--table-view
  (fn [plugin-path
       global-configuration
       data-toolkit
       configuration
       & {:keys [start-focus
                 alerts]
          :or {start-focus nil
               alerts nil}}]
    (let [x nil ;;------------ Prepare components
          expand-export (fn [id] (export-print-doc (get-in global-configuration plugin-path) id alerts))
          insert-form   (fn [] (build-input-form data-toolkit configuration global-configuration :start-focus start-focus :alerts alerts))
          view-layout   (smig/mig-panel :constraints ["" "0px[shrink 0, fill]0px[grow, fill]0px" "0px[grow, fill]0px"])
          table         (fn [] (second (u/children view-layout)))
          header        (fn [] (c/label :text (get (:table-meta data-toolkit) :representation) :halign :center :border (sborder/empty-border :top 10)))
          update-form   (fn [model return] (gcomp/expand-form-panel view-layout [(header) (build-input-form data-toolkit configuration global-configuration :update true :model model :export-comp expand-export :more-comps [(return)])]))
          x nil ;;------------ Build
          expand-insert-form (gcomp/min-scrollbox (gcomp/expand-form-panel view-layout [(header) (insert-form)]) ;;:hscroll :never
                                                  )
          back-to-insert     (fn [] (gcomp/button-basic "<< Return to Insert Form" :onClick (fn [e] (c/config! view-layout :items [[expand-insert-form] [(table)]]))))
          expand-update-form (fn [model return] (c/config! view-layout :items [[(gcomp/scrollbox (update-form model return) :hscroll :never)] [(table)]]))
          table              (fn [] ((get (create-table configuration data-toolkit) :table) (fn [model] (expand-update-form model back-to-insert)))) ;; TODO: set try
          x nil ;;------------ Finish
          view-layout        (c/config! view-layout :items [[(c/vertical-panel :items [expand-insert-form])] [(try
                                                                                                                (c/vertical-panel :items [(table)])
                                                                                                                (catch Exception e (c/label :text (str "Problem with table model: " (.getMessage e)))))]])]
      view-layout)
    ;; (c/label :text "Testing mode")
    ))


;; (let [my-frame (-> (doto (c/frame
;;                           :title "test"
;;                           :size [300 :by 800]
;;                           :content vp)
;;                      (.setLocationRelativeTo nil) c/pack! c/show!))]
;;   (c/config! my-frame :size [300 :by 800]))

(defn jarman-table-toolkit-pipeline [configuration datatoolkit]
  datatoolkit)

;;;PLUGINS ;;;        
(defn jarman-table-component [plugins-paths global-configuration spec-map]
  (let [plugins-paths (if (vector? (first plugins-paths)) plugins-paths [plugins-paths])]
    (doall
     (->> plugins-paths
          (map (fn [plugin-path]
                 (let [get-from-global #(->> % (l/join-vec plugin-path) (get-in (global-configuration)))
                       data-toolkit  (get-from-global [:data-toolkit])
                       configuration (get-from-global [:configuration])
                       title (get-in data-toolkit [:table-meta :representation])
                       space (c/select @jarman.gui.gui-seed/app (:plug-place configuration))
                       atm (:atom-expanded-items (c/config space :user-data))]
                   (if (false? (spec/test-keys-jtable configuration spec-map))
                     (println "[ Warning ] plugin/table: Error in spec")
                     (if (l/in? (:permission configuration) (keyword (session/user-get-permission)))
                       (swap! atm (fn [inserted]
                                    (conj inserted
                                          (gcomp/button-expand-child
                                           title
                                           :onClick (fn [e] (@gseed/jarman-views-service
                                                             :set-view
                                                             :view-id (str "auto-" title)
                                                             :title title
                                                             :scrollable? false
                                                             :component-fn (fn [] (auto-builder--table-view
                                                                                   plugin-path
                                                                                   (global-configuration)
                                                                                   data-toolkit
                                                                                   configuration))))))))))
                   (.revalidate space))))))))



