(def db-connection
  {:dbtype "mysql",
   :host  "trashpanda-team.ddns.net",
   :port 3307,
   :dbname "jarman-test",
   :user "root",
   :password "1234"})


#_(defn gen-kwargs [f-name m]
 (let [kv (apply hash-map (mapcat vector 
                                  (map symbol (keys m))
                                  (vals m)))
       fkk (apply hash-map (mapcat vector 
                                   (keys m)
                                   (map symbol (keys m))))]
   (list 'defn f-name ['& {:keys (vec (keys kv))
                          :or kv}]
         fkk)))

#_(gen-kwargs
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
   :key-table :enterpreneur})

(defn column-mapper [component-type]
  (case (first component-type)
    :data      [:date      :default :null]
    :datatime  [:datatime  :default :null]
    :time      [:time      :default :null]
    :link      [:bigint-20-unsigned :nnull]
    :number    [:bigint-20 :default 0]
    :float     [:float :nnull :default 0]
    :boolean   [:bool  :default 0]
    :textarea  [:text  :default :null]
    :blob      [:blob  :default :null]
    :prop      [:text  :nnull :default "'{}'"]
    :text      [:varchar-120 :default :null]
    :filepath  [:varchar-360 :default :null]
    :url       [:varchar-360 :default :null]))

(defn field [& {:keys [field-qualified description private? default-value editable? field column-type component-type representation]
                :or   {description nil private? false default-value nil editable? true}}]
  {:pre [(some? field) (some? field-qualified) (some? component-type)]}
  {:description (if description description (name field))
   :private? private?
   :default-value default-value
   :editable? editable?
   :field field
   :column-type (if column-type column-type (column-mapper component-type)) 
   :component-type component-type
   :representation (if representation representation (name field))
   :field-qualified field-qualified})

(defn field-link [& {:keys [field-qualified key-table description private? default-value editable? field column-type foreign-keys component-type representation]
                     :or {key-table :enterpreneur, private? false, default-value nil, editable? true, component-type [:link]}}]
  {:pre [(some? field) (some? field-qualified) (some? foreign-keys)]}
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


;; (table      :field :documents)
;; (table-link :field :point_of_sale_group_links)
;; (field
;;  :component-type [:text]
;;  :field :table
;;  :field-qualified :documents.table)
;; (field-link
;;  :field           :id_permission
;;  :field-qualified :user.id_permission
;;  :component-type [:link]
;;  :foreign-keys [{:id_permission :permission} {:delete :cascade, :update :cascade}]
;;  :key-table :permission)

#_(defn tc [m-list]
  (vec
   (for [col m-list]
     (if-not (:key-table col)
       (concat (list 'field)      (mapcat identity (seq (select-keys col [:field :field-qualified :component-type]))))
       (concat (list 'field-link) (mapcat identity (seq (select-keys col [:field :field-qualified :component-type :foreign-keys :key-table]))))))))

#_(defn tt [m]
  (if-not (:is-linker? m)
    (concat (list 'table)      (mapcat identity (seq (select-keys m [:field :representation]))))
    (concat (list 'table-link) (mapcat identity (seq (select-keys m [:field :representation]))))))



(def all-tables
  [{:id 1, :table_name "documents",
    :prop
    {:table (table :field :documents :representation "Documents")
     :columns
     [(field :field :table :field-qualified :documents.table :component-type [:text])
      (field :field :name :field-qualified :documents.name :component-type [:text])
      (field :field :document :field-qualified :documents.document :component-type [:blob])
      (field :field :prop :field-qualified :documents.prop :component-type [:textarea])]}}
   {:id 2, :table_name "permission",
    :prop
    {:table (table :field :permission :representation "Permission")
     :columns
     [(field :field :permission_name :field-qualified :permission.permission_name :column-type [:varchar-20 :default :null] :component-type [:text])
      (field :field :configuration :field-qualified :permission.configuration :column-type [:tinytext :nnull :default "'{}'"] :component-type [:textarea])]}}
   {:id 3, :table_name "user",
    :prop
    {:table (table :field :user :representation "User"),
     :columns
     [(field :field :login :field-qualified :user.login :component-type [:text])
      (field :field :password :field-qualified :user.password :component-type [:text])
      (field :field :first_name :field-qualified :user.first_name :component-type [:text])
      (field :field :last_name :field-qualified :user.last_name :component-type [:text])
      (field-link :field :id_permission :field-qualified :user.id_permission :component-type [:link]
                  :foreign-keys [{:id_permission :permission} {:delete :cascade, :update :cascade}] :key-table :permission)]}}
   {:id 4, :table_name "enterpreneur",
    :prop
    {:table (table :field :enterpreneur :representation "Enterpreneur"),
     :columns
     [(field :field :ssreou :field-qualified :enterpreneur.ssreou :representation "number of SSREOU" :component-type [:text])
      (field :field :name :field-qualified :enterpreneur.name :component-type [:text])
      (field :field :ownership_form :field-qualified :enterpreneur.ownership_form :component-type [:text])
      (field :field :vat_certificate :field-qualified :enterpreneur.vat_certificate :component-type [:textarea])
      (field :field :individual_tax_number :field-qualified :enterpreneur.individual_tax_number :component-type [:text])
      (field :field :director :field-qualified :enterpreneur.director :component-type [:text])
      (field :field :accountant :field-qualified :enterpreneur.accountant :component-type [:text])
      (field :field :legal_address :field-qualified :enterpreneur.legal_address :component-type [:text])
      (field :field :physical_address :field-qualified :enterpreneur.physical_address :component-type [:text])
      (field :field :contacts_information :field-qualified :enterpreneur.contacts_information
             :column-type [:mediumtext :default :null] :component-type [:textarea])]}}
   {:id 5, :table_name "point_of_sale",
    :prop
    {:table (table :field :point_of_sale :representation "point_of_sale"),
     :columns
     [(field-link :field :id_enterpreneur :field-qualified :point_of_sale.id_enterpreneur :component-type [:link]
                  :foreign-keys [{:id_enterpreneur :enterpreneur} {:update :cascade}] :key-table :enterpreneur)
      (field :field :name :field-qualified :point_of_sale.name :component-type [:text])
      (field :field :physical_address :field-qualified :point_of_sale.physical_address :component-type [:text])
      (field :field :telefons :field-qualified :point_of_sale.telefons :component-type [:text])]}}
   {:id 6, :table_name "cache_register",
    :prop
    {:table (table :field :cache_register :representation "cache_register"),
     :columns
     [(field-link :field :id_point_of_sale :field-qualified :cache_register.id_point_of_sale :component-type [:link]
                  :foreign-keys [{:id_point_of_sale :point_of_sale} {:delete :cascade, :update :cascade}] :key-table :point_of_sale)
      (field :field :name :field-qualified :cache_register.name :component-type [:text])
      (field :field :serial_number :field-qualified :cache_register.serial_number :component-type [:text])
      (field :field :fiscal_number :field-qualified :cache_register.fiscal_number :component-type [:text])
      (field :field :manufacture_date :field-qualified :cache_register.manufacture_date :component-type [:data :datatime :text])
      (field :field :first_registration_date :field-qualified :cache_register.first_registration_date
             :component-type [:data :datatime :text])
      (field :field :is_working :field-qualified :cache_register.is_working :component-type [:boolean :number :text])
      (field :field :version :field-qualified :cache_register.version :component-type [:text])
      (field :field :dev_id :field-qualified :cache_register.dev_id :component-type [:text])
      (field :field :producer :field-qualified :cache_register.producer :component-type [:text])
      (field :field :modem :field-qualified :cache_register.modem :component-type [:text])
      (field :field :modem_model :field-qualified :cache_register.modem_model :component-type [:text])
      (field :field :modem_serial_number :field-qualified :cache_register.modem_serial_number :component-type [:text])
      (field :field :modem_phone_number :field-qualified :cache_register.modem_phone_number :component-type [:text])]}}
   {:id 7, :table_name "point_of_sale_group",
    :prop
    {:table (table :field "point_of_sale_group" :representation "point_of_sale_group"),
     :columns
     [(field :field :group_name :field-qualified :point_of_sale_group.group_name :component-type [:text])
      (field :field :information :field-qualified :point_of_sale_group.information :component-type [:textarea])]}}
   {:id 8, :table_name "point_of_sale_group_links",
    :prop
    {:table (table-link :field :point_of_sale_group_links :representation "Point of sale gruop selection"),
     :columns
     [(field-link :field :id_point_of_sale_group
                  :field-qualified :point_of_sale_group_links.id_point_of_sale_group :component-type [:link]
                  :foreign-keys [{:id_point_of_sale_group :point_of_sale_group} {:delete :cascade, :update :cascade}]
                  :key-table :point_of_sale_group)
      (field-link :field :id_point_of_sale
                  :field-qualified :point_of_sale_group_links.id_point_of_sale :component-type [:link]
                  :foreign-keys [{:id_point_of_sale :point_of_sale}]
                  :key-table :point_of_sale)]}}
   {:id 9, :table_name "seal",
    :prop
    {:table (table :field "seal" :representation "seal"),
     :columns
     [(field :field :seal_number :field-qualified :seal.seal_number :component-type [:text])
      (field :field :datetime_of_use :field-qualified :seal.datetime_of_use :component-type [:datatime :data :text])
      (field :field :datetime_of_remove :field-qualified :seal.datetime_of_remove :component-type [:datatime :data :text])]}}
   {:id 10, :table_name "repair_reasons",
    :prop
    {:table (table :field "repair_reasons" :representation "repair_reasons"),
     :columns
     [(field :field :description :field-qualified :repair_reasons.description :component-type [:text])]}}
   {:id 11,
    :table_name "repair_technical_issue",
    :prop
    {:table (table :field "repair_technical_issue" :representation "repair_technical_issue"),
     :columns [(field :field :description :field-qualified :repair_technical_issue.description :component-type [:text])]}}
   {:id 12,
    :table_name "repair_nature_of_problem",
    :prop
    {:table (table :field "repair_nature_of_problem" :representation "repair_nature_of_problem"),
     :columns
     [(field :field :description :field-qualified :repair_nature_of_problem.description :component-type [:text])]}}
   {:id 13,
    :table_name "repair_contract",
    :prop
    {:table (table :field "repair_contract" :representation "repair_contract"),
     :columns
     [(field-link :field :id_cache_register :field-qualified :repair_contract.id_cache_register :component-type [:link]
                  :foreign-keys [{:id_cache_register :cache_register} {:delete :cascade, :update :cascade}] :key-table :cache_register)
      (field-link :field :id_old_seal :field-qualified :repair_contract.id_old_seal :component-type [:link]
                  :foreign-keys [{:id_old_seal :seal} {:delete :null, :update :null}] :key-table :seal)
      (field-link :field :id_new_seal :field-qualified :repair_contract.id_new_seal :component-type [:link]
                  :foreign-keys [{:id_new_seal :seal} {:delete :null, :update :null}] :key-table :seal)
      (field-link :field :id_repair_reasons :field-qualified :repair_contract.id_repair_reasons :component-type [:link]
                  :foreign-keys [{:id_repair_reasons :repair_reasons} {:delete :null, :update :null}] :key-table :repair_reasons)
      (field-link :field :id_repair_technical_issue :field-qualified :repair_contract.id_repair_technical_issue :component-type [:link]
                  :foreign-keys [{:id_repair_technical_issue :repair_technical_issue} {:delete :null, :update :null}] :key-table :repair_technical_issue)
      (field-link :field :id_repair_nature_of_problem :field-qualified :repair_contract.id_repair_nature_of_problem :component-type [:link]
                  :foreign-keys [{:id_repair_nature_of_problem :repair_nature_of_problem} {:delete :null, :update :null}] :key-table :repair_nature_of_problem)
      (field :field :repair_date :field-qualified :repair_contract.repair_date :component-type [:data :datatime :text])
      (field :field :cache_register_register_date :field-qualified :repair_contract.cache_register_register_date :component-type [:data :datatime :text])]}}
   {:id 13,
    :table_name "service_contract",
    :prop
    {:table (table :field "service_contract" :representation "Service contracts"),
     :columns
     [(field-link :field :id_enterpreneur :field-qualified :service_contract.id_enterpreneur :component-type [:link]
                  :foreign-keys [{:id_enterpreneur :enterpreneur} {:delete :cascade, :update :cascade}] :key-table :enterpreneur)
      (field :field :contract_start_term :field-qualified :service_contract.contract_start_term :component-type [:data])
      (field :field :contract_end_term :field-qualified :service_contract.contract_end_term :component-type [:data])]}}
   {:id 14,
    :table_name "service_contract_month",
    :prop
    {:table (table :field "service_contract_month" :representation "Service Contract Month"),
     :columns
     [(field-link :field :id_service_contract :field-qualified :service_contract_month.id_service_contract :component-type [:link]
                  :foreign-keys [{:id_service_contract :service_contract} {:delete :cascade, :update :cascade}] :key-table :service_contract)
      (field :field :service_month_start :field-qualified :service_contract_month.service_month_start :component-type [:data])
      (field :field :service_month_end :field-qualified :service_contract_month.service_month_end :component-type [:data])
      (field :field :money_per_month :field-qualified :service_contract_month.money_per_month :component-type [:float :text])
      (field :field :was_payed :field-qualified :service_contract_month.was_payed :component-type [:boolean])]}}])

#_(def all-tables [{:id 1,
                  :table_name "documents",
                  :prop
                  {:table
                   {:description nil,
                    :allow-linking? true,
                    :field "documents",
                    :representation "Documents",
                    :is-linker? false,
                    :allow-modifing? true,
                    :allow-deleting? true,
                    :is-system? false},
                   :columns
                   [{:description nil,
                     :private? false,
                     :default-value nil,
                     :editable? true,
                     :field :table,
                     :column-type [:varchar-100 :default :null],
                     :component-type [:text],
                     :representation "table",
                     :field-qualified :documents.table}
                    {:description nil,
                     :private? false,
                     :default-value nil,
                     :editable? true,
                     :field :name,
                     :column-type [:varchar-200 :default :null],
                     :component-type [:text],
                     :representation "name",
                     :field-qualified :documents.name}
                    {:description nil,
                     :private? false,
                     :default-value nil,
                     :editable? true,
                     :field :document,
                     :column-type [:blob :default :null],
                     :component-type nil,
                     :representation "document",
                     :field-qualified :documents.document}
                    {:description nil,
                     :private? false,
                     :default-value nil,
                     :editable? true,
                     :field :prop,
                     :column-type [:text :nnull :default "'{}'"],
                     :component-type [:textarea],
                     :representation "prop",
                     :field-qualified :documents.prop}]}}
                 {:id 2,
                  :table_name "permission",
                  :prop
                  {:table
                   {:description nil,
                    :allow-linking? true,
                    :field "permission",
                    :representation "permission",
                    :is-linker? false,
                    :allow-modifing? true,
                    :allow-deleting? true,
                    :is-system? false},
                   :columns
                   [{:description nil,
                     :private? false,
                     :default-value nil,
                     :editable? true,
                     :field :permission_name,
                     :column-type [:varchar-20 :default :null],
                     :component-type [:text],
                     :representation "permission_name",
                     :field-qualified :permission.permission_name}
                    {:description nil,
                     :private? false,
                     :default-value nil,
                     :editable? true,
                     :field :configuration,
                     :column-type [:tinytext :nnull :default "'{}'"],
                     :component-type [:textarea],
                     :representation "configuration",
                     :field-qualified :permission.configuration}]}}
                 {:id 3,
                  :table_name "user",
                  :prop
                  {:table
                   {:description nil,
                    :allow-linking? true,
                    :field "user",
                    :representation "user",
                    :is-linker? false,
                    :allow-modifing? true,
                    :allow-deleting? true,
                    :is-system? false},
                   :columns
                   [{:description nil,
                     :private? false,
                     :default-value nil,
                     :editable? true,
                     :field :login,
                     :column-type [:varchar-100 :nnull],
                     :component-type [:text],
                     :representation "login",
                     :field-qualified :user.login}
                    {:description nil,
                     :private? false,
                     :default-value nil,
                     :editable? true,
                     :field :password,
                     :column-type [:varchar-100 :nnull],
                     :component-type [:text],
                     :representation "password",
                     :field-qualified :user.password}
                    {:description nil,
                     :private? false,
                     :default-value nil,
                     :editable? true,
                     :field :first_name,
                     :column-type [:varchar-100 :nnull],
                     :component-type [:text],
                     :representation "first_name",
                     :field-qualified :user.first_name}
                    {:description nil,
                     :private? false,
                     :default-value nil,
                     :editable? true,
                     :field :last_name,
                     :column-type [:varchar-100 :nnull],
                     :component-type [:text],
                     :representation "last_name",
                     :field-qualified :user.last_name}
                    {:description nil,
                     :private? false,
                     :default-value nil,
                     :editable? true,
                     :field :id_permission,
                     :column-type [:bigint-20-unsigned :nnull],
                     :foreign-keys
                     [{:id_permission :permission}
                      {:delete :cascade, :update :cascade}],
                     :component-type [:link],
                     :representation "id_permission",
                     :field-qualified :user.id_permission,
                     :key-table :permission}]}}
                 {:id 4,
                  :table_name "enterpreneur",
                  :prop
                  {:table
                   {:description nil,
                    :allow-linking? true,
                    :field :enterpreneur,
                    :representation "Enterpreneur",
                    :is-linker? false,
                    :allow-modifing? true,
                    :allow-deleting? true,
                    :is-system? false},
                   :columns
                   [{:description nil,
                     :private? false,
                     :default-value nil,
                     :editable? true,
                     :field :ssreou,
                     :column-type [:text :nnull],
                     :component-type [:text],
                     :representation "number of SSREOU",
                     :field-qualified :enterpreneur.ssreou}
                    {:description nil,
                     :private? false,
                     :default-value nil,
                     :editable? true,
                     :field :name,
                     :column-type [:varchar-120 :default :null],
                     :component-type [:text],
                     :representation "Name",
                     :field-qualified :enterpreneur.name}
                    {:description nil,
                     :private? false,
                     :default-value nil,
                     :editable? true,
                     :field :ownership_form,
                     :column-type [:varchar-100 :default :null],
                     :component-type [:text],
                     :representation "ownership_form",
                     :field-qualified :enterpreneur.ownership_form}
                    {:description nil,
                     :private? false,
                     :default-value nil,
                     :editable? true,
                     :field :vat_certificate,
                     :column-type [:tinytext :default :null],
                     :component-type [:textarea],
                     :representation "vat_certificate",
                     :field-qualified :enterpreneur.vat_certificate}
                    {:description nil,
                     :private? false,
                     :default-value nil,
                     :editable? true,
                     :field :individual_tax_number,
                     :column-type [:varchar-100 :default :null],
                     :component-type [:text],
                     :representation "individual_tax_number",
                     :field-qualified :enterpreneur.individual_tax_number}
                    {:description nil,
                     :private? false,
                     :default-value nil,
                     :editable? true,
                     :field :director,
                     :column-type [:varchar-100 :default :null],
                     :component-type [:text],
                     :representation "director",
                     :field-qualified :enterpreneur.director}
                    {:description nil,
                     :private? false,
                     :default-value nil,
                     :editable? true,
                     :field :accountant,
                     :column-type [:varchar-100 :default :null],
                     :component-type [:text],
                     :representation "accountant",
                     :field-qualified :enterpreneur.accountant}
                    {:description nil,
                     :private? false,
                     :default-value nil,
                     :editable? true,
                     :field :legal_address,
                     :column-type [:varchar-100 :default :null],
                     :component-type [:text],
                     :representation "legal_address",
                     :field-qualified :enterpreneur.legal_address}
                    {:description nil,
                     :private? false,
                     :default-value nil,
                     :editable? true,
                     :field :physical_address,
                     :column-type [:varchar-100 :default :null],
                     :component-type [:text],
                     :representation "physical_address",
                     :field-qualified :enterpreneur.physical_address}
                    {:description nil,
                     :private? false,
                     :default-value nil,
                     :editable? true,
                     :field :contacts_information,
                     :column-type [:mediumtext :default :null],
                     :component-type [:textarea],
                     :representation "contacts_information",
                     :field-qualified :enterpreneur.contacts_information}]}}
                 {:id 5,
                  :table_name "point_of_sale",
                  :prop
                  {:table
                   {:description nil,
                    :allow-linking? true,
                    :field "point_of_sale",
                    :representation "point_of_sale",
                    :is-linker? false,
                    :allow-modifing? true,
                    :allow-deleting? true,
                    :is-system? false},
                   :columns
                   [{:description nil,
                     :private? false,
                     :default-value nil,
                     :editable? true,
                     :field :id_enterpreneur,
                     :column-type [:bigint-20-unsigned :default :null],
                     :foreign-keys
                     [{:id_enterpreneur :enterpreneur} {:update :cascade}],
                     :component-type [:link],
                     :representation "id_enterpreneur",
                     :field-qualified :point_of_sale.id_enterpreneur,
                     :key-table :enterpreneur}
                    {:description nil,
                     :private? false,
                     :default-value nil,
                     :editable? true,
                     :field :name,
                     :column-type [:varchar-100 :default :null],
                     :component-type [:text],
                     :representation "name",
                     :field-qualified :point_of_sale.name}
                    {:description nil,
                     :private? false,
                     :default-value nil,
                     :editable? true,
                     :field :physical_address,
                     :column-type [:varchar-100 :default :null],
                     :component-type [:text],
                     :representation "physical_address",
                     :field-qualified :point_of_sale.physical_address}
                    {:description nil,
                     :private? false,
                     :default-value nil,
                     :editable? true,
                     :field :telefons,
                     :column-type [:varchar-100 :default :null],
                     :component-type [:text],
                     :representation "telefons",
                     :field-qualified :point_of_sale.telefons}]}}
                 {:id 6,
                  :table_name "cache_register",
                  :prop
                  {:table
                   {:description nil,
                    :allow-linking? true,
                    :field "cache_register",
                    :representation "cache_register",
                    :is-linker? false,
                    :allow-modifing? true,
                    :allow-deleting? true,
                    :is-system? false},
                   :columns
                   [{:description nil,
                     :private? false,
                     :default-value nil,
                     :editable? true,
                     :field :id_point_of_sale,
                     :column-type [:bigint-20-unsigned :default :null],
                     :foreign-keys
                     [{:id_point_of_sale :point_of_sale}
                      {:delete :cascade, :update :cascade}],
                     :component-type [:link],
                     :representation "id_point_of_sale",
                     :field-qualified :cache_register.id_point_of_sale,
                     :key-table :point_of_sale}
                    {:description nil,
                     :private? false,
                     :default-value nil,
                     :editable? true,
                     :field :name,
                     :column-type [:varchar-100 :default :null],
                     :component-type [:text],
                     :representation "name",
                     :field-qualified :cache_register.name}
                    {:description nil,
                     :private? false,
                     :default-value nil,
                     :editable? true,
                     :field :serial_number,
                     :column-type [:varchar-100 :default :null],
                     :component-type [:text],
                     :representation "serial_number",
                     :field-qualified :cache_register.serial_number}
                    {:description nil,
                     :private? false,
                     :default-value nil,
                     :editable? true,
                     :field :fiscal_number,
                     :column-type [:varchar-100 :default :null],
                     :component-type [:text],
                     :representation "fiscal_number",
                     :field-qualified :cache_register.fiscal_number}
                    {:description nil,
                     :private? false,
                     :default-value nil,
                     :editable? true,
                     :field :manufacture_date,
                     :column-type [:date :default :null],
                     :component-type [:data :datatime :text],
                     :representation "manufacture_date",
                     :field-qualified :cache_register.manufacture_date}
                    {:description nil,
                     :private? false,
                     :default-value nil,
                     :editable? true,
                     :field :first_registration_date,
                     :column-type [:date :default :null],
                     :component-type [:data :datatime :text],
                     :representation "first_registration_date",
                     :field-qualified :cache_register.first_registration_date}
                    {:description nil,
                     :private? false,
                     :default-value nil,
                     :editable? true,
                     :field :is_working,
                     :column-type [:bool :default :null],
                     :component-type [:boolean :number :text],
                     :representation "is_working",
                     :field-qualified :cache_register.is_working}
                    {:description nil,
                     :private? false,
                     :default-value nil,
                     :editable? true,
                     :field :version,
                     :column-type [:varchar-100 :default :null],
                     :component-type [:text],
                     :representation "version",
                     :field-qualified :cache_register.version}
                    {:description nil,
                     :private? false,
                     :default-value nil,
                     :editable? true,
                     :field :dev_id,
                     :column-type [:varchar-100 :default :null],
                     :component-type [:text],
                     :representation "dev_id",
                     :field-qualified :cache_register.dev_id}
                    {:description nil,
                     :private? false,
                     :default-value nil,
                     :editable? true,
                     :field :producer,
                     :column-type [:varchar-100 :default :null],
                     :component-type [:text],
                     :representation "producer",
                     :field-qualified :cache_register.producer}
                    {:description nil,
                     :private? false,
                     :default-value nil,
                     :editable? true,
                     :field :modem,
                     :column-type [:varchar-100 :default :null],
                     :component-type [:text],
                     :representation "modem",
                     :field-qualified :cache_register.modem}
                    {:description nil,
                     :private? false,
                     :default-value nil,
                     :editable? true,
                     :field :modem_model,
                     :column-type [:varchar-100 :default :null],
                     :component-type [:text],
                     :representation "modem_model",
                     :field-qualified :cache_register.modem_model}
                    {:description nil,
                     :private? false,
                     :default-value nil,
                     :editable? true,
                     :field :modem_serial_number,
                     :column-type [:varchar-100 :default :null],
                     :component-type [:text],
                     :representation "modem_serial_number",
                     :field-qualified :cache_register.modem_serial_number}
                    {:description nil,
                     :private? false,
                     :default-value nil,
                     :editable? true,
                     :field :modem_phone_number,
                     :column-type [:varchar-100 :default :null],
                     :component-type [:text],
                     :representation "modem_phone_number",
                     :field-qualified :cache_register.modem_phone_number}]}}
                 {:id 7,
                  :table_name "point_of_sale_group",
                  :prop
                  {:table
                   {:description nil,
                    :allow-linking? true,
                    :field "point_of_sale_group",
                    :representation "point_of_sale_group",
                    :is-linker? false,
                    :allow-modifing? true,
                    :allow-deleting? true,
                    :is-system? false},
                   :columns
                   [{:description nil,
                     :private? false,
                     :default-value nil,
                     :editable? true,
                     :field :group_name,
                     :column-type [:varchar-100 :default :null],
                     :component-type [:text],
                     :representation "group_name",
                     :field-qualified :point_of_sale_group.group_name}
                    {:description nil,
                     :private? false,
                     :default-value nil,
                     :editable? true,
                     :field :information,
                     :column-type [:mediumtext :default :null],
                     :component-type [:textarea],
                     :representation "information",
                     :field-qualified :point_of_sale_group.information}]}}
                 {:id 8,
                  :table_name "point_of_sale_group_links",
                  :prop
                  {:table
                   {:field "point_of_sale_group_links",
                    :representation "point_of_sale_group_links",
                    :is-system? true,
                    :is-linker? true,
                    :allow-modifing? false,
                    :allow-deleting? false,
                    :allow-linking? false,
                    :ref
                    {:front-references [:point_of_sale_group :point_of_sale],
                     :back-references nil}},
                   :columns
                   [{:description nil,
                     :private? false,
                     :default-value nil,
                     :editable? true,
                     :field :id_point_of_sale_group,
                     :column-type [:bigint-20-unsigned :default :null],
                     :foreign-keys
                     [{:id_point_of_sale_group :point_of_sale_group}
                      {:delete :cascade, :update :cascade}],
                     :component-type [:link],
                     :representation "id_point_of_sale_group",
                     :field-qualified :point_of_sale_group_links.id_point_of_sale_group,
                     :key-table :point_of_sale_group}
                    {:description nil,
                     :private? false,
                     :default-value nil,
                     :editable? true,
                     :field :id_point_of_sale,
                     :column-type [:bigint-20-unsigned :default :null],
                     :foreign-keys [{:id_point_of_sale :point_of_sale}],
                     :component-type [:link],
                     :representation "id_point_of_sale",
                     :field-qualified :point_of_sale_group_links.id_point_of_sale,
                     :key-table :point_of_sale}]}}
                 {:id 9,
                  :table_name "seal",
                  :prop
                  {:table
                   {:description nil,
                    :allow-linking? true,
                    :field "seal",
                    :representation "seal",
                    :is-linker? false,
                    :allow-modifing? true,
                    :allow-deleting? true,
                    :is-system? false},
                   :columns
                   [{:description nil,
                     :private? false,
                     :default-value nil,
                     :editable? true,
                     :field :seal_number,
                     :column-type [:varchar-100 :default :null],
                     :component-type [:text],
                     :representation "seal_number",
                     :field-qualified :seal.seal_number}
                    {:description nil,
                     :private? false,
                     :default-value nil,
                     :editable? true,
                     :field :datetime_of_use,
                     :column-type [:datetime :default :null],
                     :component-type [:datatime :data :text],
                     :representation "datetime_of_use",
                     :field-qualified :seal.datetime_of_use}
                    {:description nil,
                     :private? false,
                     :default-value nil,
                     :editable? true,
                     :field :datetime_of_remove,
                     :column-type [:datetime :default :null],
                     :component-type [:datatime :data :text],
                     :representation "datetime_of_remove",
                     :field-qualified :seal.datetime_of_remove}]}}
                 {:id 10,
                  :table_name "repair_reasons",
                  :prop
                  {:table
                   {:description nil,
                    :allow-linking? true,
                    :field "repair_reasons",
                    :representation "repair_reasons",
                    :is-linker? false,
                    :allow-modifing? true,
                    :allow-deleting? true,
                    :is-system? false},
                   :columns
                   [{:description nil,
                     :private? false,
                     :default-value nil,
                     :editable? true,
                     :field :description,
                     :column-type [:varchar-255 :default :null],
                     :component-type [:text],
                     :representation "description",
                     :field-qualified :repair_reasons.description}]}}
                 {:id 11,
                  :table_name "repair_technical_issue",
                  :prop
                  {:table
                   {:description nil,
                    :allow-linking? true,
                    :field "repair_technical_issue",
                    :representation "repair_technical_issue",
                    :is-linker? false,
                    :allow-modifing? true,
                    :allow-deleting? true,
                    :is-system? false},
                   :columns
                   [{:description nil,
                     :private? false,
                     :default-value nil,
                     :editable? true,
                     :field :description,
                     :column-type [:varchar-255 :default :null],
                     :component-type [:text],
                     :representation "description",
                     :field-qualified :repair_technical_issue.description}]}}
                 {:id 12,
                  :table_name "repair_nature_of_problem",
                  :prop
                  {:table
                   {:description nil,
                    :allow-linking? true,
                    :field "repair_nature_of_problem",
                    :representation "repair_nature_of_problem",
                    :is-linker? false,
                    :allow-modifing? true,
                    :allow-deleting? true,
                    :is-system? false},
                   :columns
                   [{:description nil,
                     :private? false,
                     :default-value nil,
                     :editable? true,
                     :field :description,
                     :column-type [:varchar-255 :default :null],
                     :component-type [:text],
                     :representation "description",
                     :field-qualified :repair_nature_of_problem.description}]}}
                 {:id 13,
                  :table_name "repair_contract",
                  :prop
                  {:table
                   {:description nil,
                    :ref
                    {:front-references
                     [:cache_register
                      :seal
                      :seal
                      :repair_reasons
                      :repair_technical_issue
                      :repair_nature_of_problem],
                     :back-references nil},
                    :allow-linking? true,
                    :field "repair_contract",
                    :representation "repair_contract",
                    :is-linker? false,
                    :allow-modifing? true,
                    :allow-deleting? true,
                    :is-system? false},
                   :columns
                   [{:description nil,
                     :private? false,
                     :default-value nil,
                     :editable? true,
                     :field :id_cache_register,
                     :column-type [:bigint-20-unsigned :default :null],
                     :foreign-keys
                     [{:id_cache_register :cache_register}
                      {:delete :cascade, :update :cascade}],
                     :component-type [:link],
                     :representation "id_cache_register",
                     :field-qualified :repair_contract.id_cache_register,
                     :key-table :cache_register}
                    {:description nil,
                     :private? false,
                     :default-value nil,
                     :editable? true,
                     :field :id_old_seal,
                     :column-type [:bigint-20-unsigned :default :null],
                     :foreign-keys
                     [{:id_old_seal :seal} {:delete :null, :update :null}],
                     :component-type [:link],
                     :representation "id_old_seal",
                     :field-qualified :repair_contract.id_old_seal,
                     :key-table :seal}
                    {:description nil,
                     :private? false,
                     :default-value nil,
                     :editable? true,
                     :field :id_new_seal,
                     :column-type [:bigint-20-unsigned :default :null],
                     :foreign-keys
                     [{:id_new_seal :seal} {:delete :null, :update :null}],
                     :component-type [:link],
                     :representation "id_new_seal",
                     :field-qualified :repair_contract.id_new_seal,
                     :key-table :seal}
                    {:description nil,
                     :private? false,
                     :default-value nil,
                     :editable? true,
                     :field :id_repair_reasons,
                     :column-type [:bigint-20-unsigned :default :null],
                     :foreign-keys
                     [{:id_repair_reasons :repair_reasons}
                      {:delete :null, :update :null}],
                     :component-type [:link],
                     :representation "id_repair_reasons",
                     :field-qualified :repair_contract.id_repair_reasons,
                     :key-table :repair_reasons}
                    {:description nil,
                     :private? false,
                     :default-value nil,
                     :editable? true,
                     :field :id_repair_technical_issue,
                     :column-type [:bigint-20-unsigned :default :null],
                     :foreign-keys
                     [{:id_repair_technical_issue :repair_technical_issue}
                      {:delete :null, :update :null}],
                     :component-type [:link],
                     :representation "id_repair_technical_issue",
                     :field-qualified :repair_contract.id_repair_technical_issue,
                     :key-table :repair_technical_issue}
                    {:description nil,
                     :private? false,
                     :default-value nil,
                     :editable? true,
                     :field :id_repair_nature_of_problem,
                     :column-type [:bigint-20-unsigned :default :null],
                     :foreign-keys
                     [{:id_repair_nature_of_problem :repair_nature_of_problem}
                      {:delete :null, :update :null}],
                     :component-type [:link],
                     :representation "id_repair_nature_of_problem",
                     :field-qualified :repair_contract.id_repair_nature_of_problem,
                     :key-table :repair_nature_of_problem}
                    {:description nil,
                     :private? false,
                     :default-value nil,
                     :editable? true,
                     :field :repair_date,
                     :column-type [:date :default :null],
                     :component-type [:data :datatime :text],
                     :representation "repair_date",
                     :field-qualified :repair_contract.repair_date}
                    {:description nil,
                     :private? false,
                     :default-value nil,
                     :editable? true,
                     :field :cache_register_register_date,
                     :column-type [:date :default :null],
                     :component-type [:data :datatime :text],
                     :representation "cache_register_register_date",
                     :field-qualified :repair_contract.cache_register_register_date}]}}
                 {:id 13,
                  :table_name "service_contract",
                  :prop
                  {:table
                   {:description nil,
                    :allow-linking? true,
                    :field "service_contract",
                    :representation "service_contract",
                    :is-linker? false,
                    :allow-modifing? true,
                    :allow-deleting? true,
                    :is-system? false},
                   :columns
                   [{:description nil,
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
                     :key-table :enterpreneur}
                    {:description nil,
                     :private? false,
                     :default-value nil,
                     :editable? true,
                     :field :contract_start_term,
                     :column-type [:date :default :null],
                     :component-type [:data],
                     :representation "Contract start term",
                     :field-qualified :service_contract.contract_start_term}
                    {:description nil,
                     :private? false,
                     :default-value nil,
                     :editable? true,
                     :field :contract_end_term,
                     :column-type [:date :default :null],
                     :component-type [:data],
                     :representation "Contract end term",
                     :field-qualified :service_contract.contract_end_term}]}}
                 {:id 14,
                  :table_name "service_contract_month",
                  :prop
                  {:table
                   {:description nil,
                    :allow-linking? true,
                    :field "service_contract_month",
                    :representation "Service Contract Month",
                    :is-linker? false,
                    :allow-modifing? true,
                    :allow-deleting? true,
                    :is-system? false},
                   :columns
                   [{:description nil,
                     :private? false,
                     :default-value nil,
                     :editable? true,
                     :field :id_service_contract,
                     :column-type [:bigint-20-unsigned :default :null],
                     :foreign-keys
                     [{:id_service_contract :service_contract}
                      {:delete :cascade, :update :cascade}],
                     :component-type [:link],
                     :representation "Service contract",
                     :field-qualified :service_contract_month.id_service_contract,
                     :key-table :service_contract}
                    {:description nil,
                     :private? false,
                     :default-value nil,
                     :editable? true,
                     :field :service_month_start,
                     :column-type [:date :default :null],
                     :component-type [:data],
                     :representation "Service month start",
                     :field-qualified :service_contract_month.service_month_start}
                    {:description nil,
                     :private? false,
                     :default-value nil,
                     :editable? true,
                     :field :service_month_end,
                     :column-type [:date :default :null],
                     :component-type [:data],
                     :representation "Service month end",
                     :field-qualified :service_contract_month.service_month_end}
                    {:description nil,
                     :private? false,
                     :default-value nil,
                     :editable? true,
                     :field :money_per_month,
                     :column-type [:float :nnull :default 0],
                     :component-type [:float :text],
                     :representation "Money per month",
                     :field-qualified :service_contract_month.money_per_month}
                    {:description nil,
                     :private? false,
                     :default-value nil,
                     :editable? true,
                     :field :was_payed,
                     :column-type [:boolean :default 0],
                     :component-type [:boolean],
                     :representation "Payed?",
                     :field-qualified :service_contract_month.was_payed}]}}])

