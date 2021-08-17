(ns jarman.managment.data-metadata-shorts
  (:require
   [clojure.string :as string]))

(defn column-mapper [component-type]
  (case (first component-type)
    nil nil
    :date      [:date      :default :null]
    :datetime  [:datetime  :null]
    :time      [:time      :null]
    :link      [:bigint-20-unsigned :default :null]
    :number    [:bigint-20    :default 0]
    :float     [:float :nnull :default 0]
    :boolean   [:bool  :default 0]
    :textarea  [:text  :default :null]
    :blob      [:blob  :default :null]
    :prop      [:text :nnull :default "'{}'"]
    :text      [:varchar-120 :default :null]
    :filepath  [:varchar-360 :default :null]
    :url       [:varchar-360 :default :null]
    true       nil))

(defn field [& {:keys [field
                       field-qualified
                       representation
                       description
                       private?
                       default-value
                       editable?
                       column-type
                       component-type
                       constructor-var]
                :or   {description nil private? false default-value nil editable? true}}]
  {:pre [(some? field) ;; (some? field-qualified)
         (some? component-type)]}
  {:description (if description description (name field))
   :private? private?
   :default-value default-value
   :editable? editable?
   :field field
   :column-type (if column-type column-type (column-mapper component-type)) 
   :component-type component-type
   :representation (if representation representation (name field))
   :field-qualified field-qualified
   :constructor-var constructor-var})

(defn field-composite [& {:keys [field
                                 field-qualified
                                 description
                                 representation
                                 column-type
                                 component-type
                                 default-value
                                 constructor
                                 columns
                                 private?
                                 editable?]}]
  {:pre [(some? field)]}
  (if (nil? field)
    (assert false (format "Coposite column has bad declaration. `:field` cannot be nil")))
  ;; (if (nil? field-qualified)
  ;;   (assert false (format "Coposite column `%s` has bad declaration. `:field-qualified` cannot be nil" (str field))))
  (if (nil? constructor)
    (assert false (format "Coposite column `%s` has bad declaration. `:constructor` cannot be nil" (str field-qualified))))
  (if (not (every? #(keyword? (:constructor-var %)) columns))
    (assert false (format "Coposite column `%s` has bad declaration. One or more embaded columns not contain `:constructor-var` param" (str field-qualified))))
  {:description (if description description (name field))
   :private? private?
   :default-value default-value
   :editable? editable?
   :field field
   :column-type (if column-type column-type (column-mapper component-type)) 
   :component-type component-type
   :representation (if representation representation (name field))
   :field-qualified field-qualified
   :constructor constructor
   :columns columns})

(defn field-link [& {:keys [field-qualified key-table description private? default-value editable? field column-type foreign-keys component-type representation]
                     :or {private? false, default-value nil, editable? true, component-type [:link]}}]
  {:pre [(some? field) ;; (some? field-qualified)
         (some? foreign-keys)]}
  {:description (if description description (name field))
   :private? private?,
   :default-value default-value,
   :editable? editable?,
   :field field,
   :column-type (if column-type column-type (column-mapper component-type))
   :component-type component-type
   :foreign-keys foreign-keys,
   :representation (if representation representation (name field))
   :field-qualified field-qualified
   :key-table key-table})

(defn table [&  {:keys [is-system? description allow-linking? field representation is-linker? allow-modifing? allow-deleting?]
                 :or   {is-system? false, description nil, allow-linking? true, is-linker? false, allow-modifing? true, allow-deleting? true}}]
  {:description (if description description (name field))
   :allow-linking? allow-linking?,
   :field field,
   :representation (if representation representation (name field))
   :is-linker? is-linker?,
   :allow-modifing? allow-modifing?,
   :allow-deleting? allow-deleting?,
   :is-system? is-system?})

(defn table-link [& {:keys [is-system? description allow-linking? field representation is-linker? allow-modifing? allow-deleting?]
                       :or {is-system? false description nil allow-linking? false is-linker? true allow-modifing? false allow-deleting? false}}]
    {:description (if description description (name field))
     :allow-linking? allow-linking?
     :field field
     :representation (if representation representation (name field))
     :is-linker? is-linker?
     :allow-modifing? allow-modifing?
     :allow-deleting? allow-deleting?
     :is-system? is-system?})


(defn- do-field-qualified [table field]
  (keyword (format "%s.%s" (name table) (name field))))
(defn- create-field-qualified [table field]
  (assoc field :field-qualified (do-field-qualified table (:field field))))

(defn prop [& {:keys [table columns columns-composite]}]
  (if (nil? (:field table))
    (assert false (format "Bad table declaration, `:field` cannot be nil '%s'. " (str table))))
  (let [table-name (:field table)
        add-field-qualified (partial create-field-qualified table-name)]
    {:table table 
     :columns (mapv add-field-qualified columns)
     :columns-composite
     (mapv (fn [comp-column]
             (-> comp-column
                 (update :columns #(mapv add-field-qualified %))
                 add-field-qualified))columns-composite)}))

(comment
  (table      :field :documents)
  (table-link :field :point_of_sale_group_links)
  (field
   :component-type [:text]
   :field :table
   :field-qualified :documents.table)
  (field-link
   :field           :id_permission
   :field-qualified :user.id_permission
   :component-type [:link]
   :foreign-keys [{:id_permission :permission} {:delete :cascade, :update :cascade}]
   :key-table :permission))

;;;;;;;;;;;;;;;;;;
;;; ALPHA TOOL ;;;
;;;;;;;;;;;;;;;;;;

(defn tc [m-list]
    (vec
     (for [col m-list]
       (if-not (:key-table col)
         (concat (list 'field)      (mapcat identity (seq (select-keys col [:field :field-qualified :component-type]))))
         (concat (list 'field-link) (mapcat identity (seq (select-keys col [:field :field-qualified :component-type :foreign-keys :key-table]))))))))

(defn tt [m]
    (if-not (:is-linker? m)
      (concat (list 'table)      (mapcat identity (seq (select-keys m [:field :representation]))))
      (concat (list 'table-link) (mapcat identity (seq (select-keys m [:field :representation]))))))

(defn gen-kwargs [f-name m]
 (let [kv (apply hash-map (mapcat vector 
                                  (map symbol (keys m))
                                  (vals m)))
       fkk (apply hash-map (mapcat vector 
                                   (keys m)
                                   (map symbol (keys m))))]
   (list 'defn f-name ['& {:keys (vec (keys kv))
                           :or kv}]
         fkk)))

(comment
 (gen-kwargs
  'field-link
  '{:description nil,
    :private? false,
    :default-value nil,
    :editable? true,
    :field :id_enterpreneur,
    :column-type [:bigint-20-unsigned :default :null],
    :foreign-keys
    [{:id_enterpreneur :enterpreneur}
     {:delete :cascade, :update :cascade}],
    :component-type [:link],
    :representation "Enterpreneur",
    :field-qualified :service_contract.id_enterpreneur,
    :key-table :enterpreneur}))




