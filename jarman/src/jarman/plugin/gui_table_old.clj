(ns jarman.plugin.gui-table-old
  (:require
   ;; Clojure toolkit 
   [clojure.string :as string]
   ;; Dev tools
   [seesaw.core :as c]
   [seesaw.swingx :as swingx]
   ;; Jarman toolkit
   [jarman.tools.lang :refer :all]
   [jarman.logic.metadata :as mt]))

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
;; uuuu
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
                :model-insert table-model
                :insert-button true
                :delete-button true
                :actions []
                :buttons []
                :query (if-not (empty? joines)
                         {:table_name (keyword name-of-table)
                          :inner-join joines :column columns}
                         {:table_name (keyword name-of-table)
                          :column columns})))))

;; (create-table-plugin :user)

;; (mapv create-table-plugin
;;       [;; :permission :user
;;        :cache_register :documents :enterpreneur :point_of_sale :point_of_sale_group :point_of_sale_group_links :repair_contract :repair_nature_of_problem :repair_reasons :repair_technical_issue :seal :service_contract :service_contract_month]
;;       ;; [:permission :user :enterpreneur :point_of_sale :cache_register :point_of_sale_group :point_of_sale_group_links :seal :repair_contract :service_contract :service_contract_month]
;;       )

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

(defn create-table [configuration toolkit-map]
  (let [view (:view-columns configuration) tables (:tables configuration)]
    ;; (println "\nView table\n" view)
    (if (and view tables)
      (let [model-columns (gui-table-model-columns tables view)
            table-model (gui-table-model model-columns (:select toolkit-map))
            ;; x (println "\ntable-model " (table-model))
            ]
        {:table-model table-model
         :table (gui-table table-model)}))))


(count (gui-table-model-columns
       [:user :permission] [:user.id :user.login :user.password :user.first_name :user.last_name :user.id_permission :permission.id :permission.permission_name :permission.configuration]))
;; => [{:key :user.login, :text "login"} {:key :user.password, :text "password"} {:key :user.first_name, :text "first_name"} {:key :user.last_name, :text "last_name"} {:key :user.id_permission, :text "id_permission"} {:key :permission.permission_name, :text "id_permission Permission name"} {:key :permission.configuration, :text "id_permission Configuration"}]

