(require '[jarman.logic.connection :as connection])
(require '[clojure.set :refer [rename-keys]])
(require '[jarman.managment.data-metadata-shorts :refer [table field table-link field-link field-composite]])
(require '[jarman.logic.composite-components])

#_(connection/connection-set
   ;; set selected
   (:dell-test
    ;;------------
    {:localhost
     {:dbtype "mysql",
      :host "127.0.0.1",
      :port 3306,
      :dbname "jarman",
      :user "root",
      :password "1234"},
     :raspberry
     {:dbtype "mysql",
      :host "trashpanda-team.ddns.net",
      :port 3306,
      :dbname "jarman",
      :user "jarman",
      :password "dupa"}
     :dell
     {:dbtype "mysql",
      :host "trashpanda-team.ddns.net",
      :port 3307,
      :dbname "jarman",
      :user "root",
      :password "1234"}
     :dell-test
     {:dbtype "mysql",
      :host "trashpanda-team.ddns.net",
      :port 3307,
      :dbname "jarman-test",
      :user "root",
      :password "1234"}}))

(def metadata-list
  [{:id nil, :table_name "documents",
    :prop
    {:table (table :field :documents :representation "Documents")
     :columns
     [(field :field :table_name :field-qualified :documents.table_name :component-type [:text])
      (field :field :name :field-qualified :documents.name :component-type [:text])
      (field :field :document :field-qualified :documents.document :component-type [:blob])
      (field :field :prop :field-qualified :documents.prop :component-type [:textarea])]}}
   ;; ----------------------------------------------------
   {:id nil, :table_name "permission",
    :prop
    {:table (table :field :permission :representation "Permission")
     :columns
     [(field :field :permission_name :field-qualified :permission.permission_name :column-type [:varchar-20 :default :null] :component-type [:text])
      (field :field :configuration :field-qualified :permission.configuration :column-type [:tinytext :nnull :default "'{}'"] :component-type [:textarea])]}}
   ;; ----------------------------------------------------
   {:id nil, :table_name "user",
    :prop
    {:table (table :field :user :representation "User"),
     :columns
     [(field :field :login :field-qualified :user.login :component-type [:text])
      (field :field :password :field-qualified :user.password :component-type [:text])
      (field :field :first_name :field-qualified :user.first_name :component-type [:text])
      (field :field :last_name :field-qualified :user.last_name :component-type [:text])
      (field :field :configuration :field-qualified :user.configuration :component-type [:prop])
      (field-link :field :id_permission :field-qualified :user.id_permission :component-type [:link]
                  :foreign-keys [{:id_permission :permission} {:delete :cascade, :update :cascade}] :key-table :permission)]}}
   ;; ----------------------------------------------------
   {:id nil, :table_name "enterpreneur",
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
   ;; ----------------------------------------------------
   {:id nil, :table_name "point_of_sale",
    :prop
    {:table (table :field :point_of_sale :representation "point_of_sale"),
     :columns
     [(field-link :field :id_enterpreneur :field-qualified :point_of_sale.id_enterpreneur :component-type [:link]
                  :foreign-keys [{:id_enterpreneur :enterpreneur} {:update :cascade}] :key-table :enterpreneur)
      (field :field :name :field-qualified :point_of_sale.name :component-type [:text])
      (field :field :physical_address :field-qualified :point_of_sale.physical_address :component-type [:text])
      (field :field :telefons :field-qualified :point_of_sale.telefons :component-type [:text])]}}
   ;; ----------------------------------------------------
   {:id nil, :table_name "cache_register",
    :prop
    {:table (table :field :cache_register :representation "cache_register"),
     :columns
     [(field-link :field :id_point_of_sale :field-qualified :cache_register.id_point_of_sale :component-type [:link]
                  :foreign-keys [{:id_point_of_sale :point_of_sale} {:delete :cascade, :update :cascade}] :key-table :point_of_sale)
      (field :field :name :field-qualified :cache_register.name :component-type [:text])
      (field :field :serial_number :field-qualified :cache_register.serial_number :component-type [:text])
      (field :field :fiscal_number :field-qualified :cache_register.fiscal_number :component-type [:text])
      (field :field :manufacture_date :field-qualified :cache_register.manufacture_date :component-type [:date :datetime :text])
      (field :field :first_registration_date :field-qualified :cache_register.first_registration_date
             :component-type [:date :datetime :text])
      (field :field :is_working :field-qualified :cache_register.is_working :component-type [:boolean :number :text])
      (field :field :version :field-qualified :cache_register.version :component-type [:text])
      (field :field :dev_id :field-qualified :cache_register.dev_id :component-type [:text])
      (field :field :producer :field-qualified :cache_register.producer :component-type [:text])
      (field :field :modem :field-qualified :cache_register.modem :component-type [:text])
      (field :field :modem_model :field-qualified :cache_register.modem_model :component-type [:text])
      (field :field :modem_serial_number :field-qualified :cache_register.modem_serial_number :component-type [:text])
      (field :field :modem_phone_number :field-qualified :cache_register.modem_phone_number :component-type [:text])]}}
   ;; ----------------------------------------------------
   {:id nil, :table_name "point_of_sale_group",
    :prop
    {:table (table :field "point_of_sale_group" :representation "point_of_sale_group"),
     :columns
     [(field :field :group_name :field-qualified :point_of_sale_group.group_name :component-type [:text])
      (field :field :information :field-qualified :point_of_sale_group.information :component-type [:textarea])]}}
   ;; ----------------------------------------------------
   {:id nil, :table_name "point_of_sale_group_links",
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
   ;; ----------------------------------------------------
   {:id nil, :table_name "seal",
    :prop
    {:table (table :field "seal" :representation "seal"),
     :columns
     [(field :field :seal_number :field-qualified :seal.seal_number :component-type [:text])
      (field :field :datetime_of_use :field-qualified :seal.datetime_of_use :component-type [:datetime :date :text])
      (field :field :datetime_of_remove :field-qualified :seal.datetime_of_remove :component-type [:datetime :date :text])]
     :columns-composite
     [(field-composite :field :site :field-qualified :seal.site :component-type [:url] :constructor #'jarman.logic.composite-components/map->Link
                       :columns [(field :field :site_name :field-qualified :seal.site_name :constructor-var :text :component-type [:text])
                                 (field :field :site_url :field-qualified :seal.site_url :constructor-var :link :component-type [:text])])
      (field-composite :field :file :field-qualified :seal.file :component-type [:file] :constructor #'jarman.logic.composite-components/map->File
                       :columns [(field :field :file_name :field-qualified :seal.file_name :constructor-var :file-name :component-type [:text])
                                 (field :field :file :field-qualified :seal.file :constructor-var :file  :component-type [:blob])])
      (field-composite :field :ftp_file :field-qualified :seal.ftp_file :component-type [:ftp] :constructor #'jarman.logic.composite-components/map->FtpFile
                       :columns [(field :field :ftp_file_name :field-qualified :seal.ftp_file_name :constructor-var :file-name :component-type [:text])
                                 (field :field :ftp_file :field-qualified :seal.ftp_file :constructor-var :file  :component-type [:blob])
                                 ;; Add also login field, password field, file-name, file-path
                                 ;; also host
                                 ])]}}
   ;; ----------------------------------------------------
   {:id nil, :table_name "repair_reasons",
    :prop
    {:table (table :field "repair_reasons" :representation "repair_reasons"),
     :columns
     [(field :field :description :field-qualified :repair_reasons.description :component-type [:text])]}}
   ;; ----------------------------------------------------
   {:id nil,
    :table_name "repair_technical_issue",
    :prop
    {:table (table :field "repair_technical_issue" :representation "repair_technical_issue"),
     :columns [(field :field :description :field-qualified :repair_technical_issue.description :component-type [:text])]}}
   ;; ----------------------------------------------------
   {:id nil,
    :table_name "repair_nature_of_problem",
    :prop
    {:table (table :field "repair_nature_of_problem" :representation "repair_nature_of_problem"),
     :columns
     [(field :field :description :field-qualified :repair_nature_of_problem.description :component-type [:text])]}}
   ;; ----------------------------------------------------
   {:id nil,
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
      (field :field :repair_date :field-qualified :repair_contract.repair_date :component-type [:date :datetime :text])
      (field :field :cache_register_register_date :field-qualified :repair_contract.cache_register_register_date :component-type [:date :datetime :text])]}}
   ;; ----------------------------------------------------
   {:id nil,
    :table_name "service_contract",
    :prop
    {:table (table :field "service_contract" :representation "Service contracts"),
     :columns
     [(field-link :field :id_enterpreneur :field-qualified :service_contract.id_enterpreneur :component-type [:link]
                  :foreign-keys [{:id_enterpreneur :enterpreneur} {:delete :cascade, :update :cascade}] :key-table :enterpreneur)
      (field :field :contract_start_term :field-qualified :service_contract.contract_start_term :component-type [:date])
      (field :field :contract_end_term :field-qualified :service_contract.contract_end_term :component-type [:date])]}}
   ;; ----------------------------------------------------
   {:id nil,
    :table_name "service_contract_month",
    :prop
    {:table (table :field "service_contract_month" :representation "Service Contract Month"),
     :columns
     [(field-link :field :id_service_contract :field-qualified :service_contract_month.id_service_contract :component-type [:link]
                  :foreign-keys [{:id_service_contract :service_contract} {:delete :cascade, :update :cascade}] :key-table :service_contract)
      (field :field :service_month_start :field-qualified :service_contract_month.service_month_start :component-type [:date])
      (field :field :service_month_end :field-qualified :service_contract_month.service_month_end :component-type [:date])
      (field :field :money_per_month :field-qualified :service_contract_month.money_per_month :component-type [:float :text])
      (field :field :was_payed :field-qualified :service_contract_month.was_payed :component-type [:boolean])]}}])

;;;;;;;;;;;;;;
;;; Events ;;;
;;;;;;;;;;;;;;

(require 'jarman.managment.data-managment)

;;; You not need to implement all, 
;;; only those you need for managment

(defn on-install []
  (println "Installing jarman schemas, it gonna take some time...")
  (println "cleaning all tables.")
  (jarman.managment.data-managment/database-delete-scheme metadata-list)
  (println "creating system tables.")
  (jarman.managment.data-managment/database-verify-system-tables)
  (println "creating business tables.")
  (jarman.managment.data-managment/database-create-scheme metadata-list)
  (println "persisting metadata.")
  (jarman.managment.data-managment/metadata-persist-into-database metadata-list)
  (println "done."))

(defn on-delete []
  (println "Deleting all jarman system/business shemas.")
  (jarman.managment.data-managment/database-delete-scheme metadata-list)
  (println "done."))

(defn on-backup []
  (println "`on-backup` not yet impelemented"))

(defn on-app-start []
  (println "Starting verifing system tables.")
  (jarman.managment.data-managment/database-verify-system-tables)
  (println "done."))

(defn on-update-meta []
  (println "Persisting metadata to database.")
  (jarman.managment.data-managment/metadata-persist-into-database metadata-list)
  (println "done."))

(defn on-app-close []
  (println "`on-app-close` not yet impelemented"))

(defn on-crash []
  (println "`on-crash` not yet impelemented"))

(defn on-log []
  (println "`on-log` not yet impelemented"))

(defn on-clear []
  (println "`on-clear` not yet impelemented"))

(defn on-info []
  (jarman.managment.data-managment/database-info)
  (jarman.managment.data-managment/metadata-info metadata-list))

(comment
  (on-install)
  (on-delete)
  (on-backup)
  (on-app-start)
  (on-app-close)
  (on-metadata-update)
  (on-crash)
  (on-log)
  (on-clear)
  (on-info))

(comment
  (jarman.managment.data-managment/database-info)
  (jarman.managment.data-managment/metadata-info metadata-list)
  (jarman.managment.data-managment/database-recreate-metadata-to-db)
  (jarman.managment.data-managment/database-recreate-metadata-to-file "some.edn")
  (jarman.managment.data-managment/metadata-persist-into-database metadata-list)
  (jarman.managment.data-managment/metadata-get-tables metadata-list)
  (jarman.managment.data-managment/database-verify-system-tables)
  (jarman.managment.data-managment/database-clear-metadata)
  (jarman.managment.data-managment/database-delete-business-scheme metadata-list)
  (jarman.managment.data-managment/database-delete-scheme metadata-list)
  (jarman.managment.data-managment/database-create-scheme metadata-list))

