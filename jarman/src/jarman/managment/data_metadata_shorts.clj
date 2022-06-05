(ns jarman.managment.data-metadata-shorts
  (:require
   [clojure.string :as string]
   [jarman.org :refer :all]))

(defn check-metafield-standart-version-01
  "Example:  
  (check-metafield-standart-version-01
    {:field :table_name
     :field-qualified :seal.table_name
     :representation \"Table name\"
     :component
     {:type :jsgl-text
      :value \"0\"
      :font-size 14
      :char-limit 0
      :placeholder \"\"
      :border [10 10 5 5 2]
      :on-change (fn [e] (c/text e))
      :start-underline nil
      :args []}})"
  [m] (if-let
          [text
           (some->> 
            (cond-> []
              (nil? (:field m)) (conj "Critical. `:field` MUST NOT be empty")
              (nil? (:field-qualified m)) (conj "Critical. `:field-qualified` MUST NOT be empty")
              (nil? (:component m)) (conj "Critical. `:component` MUST NOT be empty")
              (not-empty (:component-type m)) (conj "Attension. `:component-type` are deprecated in current version")
              (not-empty (:component m))
              (cond->
                  (not (keyword? (get-in m [:component :type]))) (conj "Critical. `:component :type` should be keyword type")))
            (seq)
            (clojure.string/join "\n"))]
        (print-header
         "Metafield version 001 missurements:"
         (print-multiline text))))

(comment
  ;; Exmaple fo component declaration
  {:type :jsgl-text
   :value "0"
   :font-size 14
   :char-limit 0
   :placeholder ""
   :border [10 10 5 5 2]
   :on-change (fn [e] (c/text e))
   :start-underline nil
   :args []})

(defn f [structure]
  (let [args (rest structure)
        c (first structure)]
    (apply concat
           [(symbol c)]
           (map #(if (= (first %) :component-type)
                   (list :component
                         (case (second %)
                           [:text] {:type :jsgl-text :value ""}
                           [:blob] {:type :jsgl-file-dialog}
                           [:textarea] {:type :jsgl-textarea :value ""}
                           [:link] {:type :jsgl-link}
                           [:date] {:type :jsgl-calendar-label}
                           [:datetime] {:type :jsgl-datetime-label}
                           [:prop] {:type :jsgl-codearea :value "{}"}
                           [:datetime :date :text] {:type :jsgl-datetime-label}
                           :else nil))
                   %)
                (partition-all 2 args)))))

;; (defn column-mapper [component-type]
;;   (case (first component-type)
;;     nil nil
;;     :date      [:date      :default :null]
;;     :datetime  [:datetime  :null]
;;     :time      [:time      :null]
;;     :link      [:bigint-20-unsigned :default :null]
;;     :number    [:bigint-20    :default 0]
;;     :float     [:float :nnull :default 0]
;;     :boolean   [:bool  :default 0]
;;     :textarea  [:text  :default :null]
;;     :blob      [:blob  :default :null]
;;     :prop      [:text :nnull :default "'{}'"]
;;     :text      [:varchar-120 :default :null]
;;     :filepath  [:varchar-360 :default :null]
;;     :url       nil
;;     true       nil))

;; (defn field [& {:keys [field
;;                        field-qualified
;;                        representation
;;                        description
;;                        private?
;;                        default-value
;;                        editable?
;;                        column-type
;;                        component-type
;;                        constructor-var]
;;                 :or   {description nil private? false default-value nil editable? true}}]
;;   {:pre [(some? field) ;; (some? field-qualified)
;;          (some? component-type)]}
;;   {:description (if description description (name field))
;;    :private? private?
;;    :default-value default-value
;;    :editable? editable?
;;    :field field
;;    :column-type (if column-type column-type (column-mapper component-type)) 
;;    :component-type component-type
;;    :representation (if representation representation (name field))
;;    :field-qualified field-qualified
;;    :constructor-var constructor-var})

(defn field
  [& {:keys [field
             field-qualified
             representation
             description
             private?
             default-value
             editable?
             column-type
             component
             constructor-var]
      :or   {description nil private? false default-value nil editable? true}}]
  {:pre [(some? field) ;; (some? field-qualified)
         (some? component)]}
  {:field field
   :field-qualified field-qualified
   :representation (if representation representation (name field))
   :description (if description description (name field))
   :column-type column-type
   :component component
   :private? private?
   :default-value default-value
   :editable? editable?
   :constructor-var constructor-var})

(defn field-composite
  [& {:keys [column-type
             columns
             component
             default-value
             description
             editable?
             field
             field-qualified
             private?
             representation]}]
  {:pre [(some? field)]}
  (if (nil? field)
    (assert false (format "Coposite column has bad declaration. `:field` cannot be nil")))
  ;; (if (nil? field-qualified)
  ;;   (assert false (format "Coposite column `%s` has bad declaration. `:field-qualified` cannot be nil" (str field))))
  ;; (if (nil? constructor)
  ;;   (assert false (format "Coposite column `%s` has bad declaration. `:constructor` cannot be nil" (str field-qualified))))
  (if (not (every? #(keyword? (:constructor-var %)) columns))
    (assert false (format "Coposite column `%s` has bad declaration. One or more embaded columns not contain `:constructor-var` param" (str field-qualified))))
  {:field field
   :field-qualified field-qualified
   :representation (if representation representation (name field))
   :description (if description description (name field))
   :column-type nil 
   :component component
   :columns columns
   :private? private?
   :default-value default-value
   :editable? editable?})

(defn field-link [& {:keys [field-qualified key-table description private? editable? field column-type foreign-keys component representation]
                     :or {private? false, default-value nil, editable? true}}]
  {:pre [(some? field) ;; (some? field-qualified)
         (some? foreign-keys)
         (some? component)]}
  {:field field
   :field-qualified field-qualified
   ;; ---
   :representation (if representation representation (name field))
   :description (if description description (name field))
   ;; ---
   :column-type nil 
   :component component
   ;; --
   :foreign-keys foreign-keys
   :key-table key-table
   ;; --
   :private? private?
   :editable? editable?
   ;; :default-value nil
   ;; :columns columns
  })

(defn table [&  {:keys [is-system? description allow-linking? field representation is-linker? allow-modifing? allow-deleting?]
                 :or   {is-system? false, description nil, allow-linking? true, is-linker? false, allow-modifing? true, allow-deleting? true}}]
  {:field field
   :representation (if representation representation (name field))
   :description    (if description     description   (name field))

   :is-system?      is-system?
   :is-linker?      is-linker?
   :allow-linking?  allow-linking?
   :allow-modifing? allow-modifing?
   :allow-deleting? allow-deleting?})

(defn table-link [& {:keys [is-system? description field representation allow-modifing? allow-deleting?]
                       :or {is-system? false description nil allow-modifing? false allow-deleting? false}}]
  {:field field
   :description    (if description description (name field))
   :representation (if representation representation (name field))
   
   :is-linker?      true
   :is-system?      is-system?
   :allow-modifing? allow-modifing?
   :allow-deleting? allow-deleting?
   :allow-linking?  false})


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

