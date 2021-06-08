{:host "trashpanda-team.ddns.net", :port 3307, :dbname "jarman"}


(defview
  permission
  (jarman-table
   :name
   "permission"
   :plug-place
   [:#tables-view-plugin]
   :tables
   [:permission]
   :model
   [:permission.id
    :permission.permission_name
    :permission.configuration]
   :query
   {:column
    (as-is
     :permission.id
     :permission.permission_name
     :permission.configuration)}))
;; (defview
;;   user
;;   (jarman-table
;;    :name
;;    "user"
;;    :plug-place
;;    [:#tables-view-plugin]
;;    :tables
;;    [:user :permission]
;;    :model
;;    [:user.login
;;     :user.password
;;     :user.first_name
;;     :user.last_name
;;     :user.id_permission]
;;    :query
;;    {:inner-join [:user->permission]
;;     :columns
;;     (as-is
;;      :user.id
;;      :user.login
;;      :user.password
;;      :user.first_name
;;      :user.last_name
;;      :user.id_permission
;;      :permission.id
;;      :permission.permission_name
;;      :permission.configuration)}))
;; (defview
;;   enterpreneur
;;   (jarman-table
;;    :name
;;    "enterpreneur"
;;    :plug-place
;;    [:#tables-view-plugin]
;;    :tables
;;    [:enterpreneur]
;;    :model
;;    [:enterpreneur.id
;;     :enterpreneur.ssreou
;;     :enterpreneur.ownership_form
;;     :enterpreneur.vat_certificate
;;     :enterpreneur.individual_tax_number
;;     :enterpreneur.director
;;     :enterpreneur.accountant
;;     :enterpreneur.legal_address
;;     :enterpreneur.physical_address
;;     :enterpreneur.contacts_information]
;;    :query
;;    {:column
;;     (as-is
;;      :enterpreneur.id
;;      :enterpreneur.ssreou
;;      :enterpreneur.ownership_form
;;      :enterpreneur.vat_certificate
;;      :enterpreneur.individual_tax_number
;;      :enterpreneur.director
;;      :enterpreneur.accountant
;;      :enterpreneur.legal_address
;;      :enterpreneur.physical_address
;;      :enterpreneur.contacts_information)}))
;; (defview
;;   point_of_sale
;;   (jarman-table
;;    :name
;;    "point_of_sale"
;;    :plug-place
;;    [:#tables-view-plugin]
;;    :tables
;;    [:point_of_sale :enterpreneur]
;;    :model
;;    [:point_of_sale.id
;;     :point_of_sale.id_enterpreneur
;;     :point_of_sale.name
;;     :point_of_sale.physical_address
;;     :point_of_sale.telefons
;;     :enterpreneur.id
;;     :enterpreneur.ssreou
;;     :enterpreneur.ownership_form
;;     :enterpreneur.vat_certificate
;;     :enterpreneur.individual_tax_number
;;     :enterpreneur.director
;;     :enterpreneur.accountant
;;     :enterpreneur.legal_address
;;     :enterpreneur.physical_address
;;     :enterpreneur.contacts_information]
;;    :query
;;    {:inner-join [:point_of_sale->enterpreneur],
;;     :columns
;;     (as-is
;;      :point_of_sale.id
;;      :point_of_sale.id_enterpreneur
;;      :point_of_sale.name
;;      :point_of_sale.physical_address
;;      :point_of_sale.telefons
;;      :enterpreneur.id
;;      :enterpreneur.ssreou
;;      :enterpreneur.ownership_form
;;      :enterpreneur.vat_certificate
;;      :enterpreneur.individual_tax_number
;;      :enterpreneur.director
;;      :enterpreneur.accountant
;;      :enterpreneur.legal_address
;;      :enterpreneur.physical_address
;;      :enterpreneur.contacts_information)}))
;; (defview
;;   cache_register
;;   (jarman-table
;;    :name
;;    "cache_register"
;;    :plug-place
;;    [:#tables-view-plugin]
;;    :tables
;;    [:cache_register :point_of_sale :enterpreneur]
;;    :model
;;    [:cache_register.id
;;     :cache_register.id_point_of_sale
;;     :cache_register.name
;;     :cache_register.serial_number
;;     :cache_register.fiscal_number
;;     :cache_register.manufacture_date
;;     :cache_register.first_registration_date
;;     :cache_register.is_working
;;     :cache_register.version
;;     :cache_register.dev_id
;;     :cache_register.producer
;;     :cache_register.modem
;;     :cache_register.modem_model
;;     :cache_register.modem_serial_number
;;     :cache_register.modem_phone_number
;;     :point_of_sale.id
;;     :point_of_sale.id_enterpreneur
;;     :point_of_sale.name
;;     :point_of_sale.physical_address
;;     :point_of_sale.telefons
;;     :enterpreneur.id
;;     :enterpreneur.ssreou
;;     :enterpreneur.ownership_form
;;     :enterpreneur.vat_certificate
;;     :enterpreneur.individual_tax_number
;;     :enterpreneur.director
;;     :enterpreneur.accountant
;;     :enterpreneur.legal_address
;;     :enterpreneur.physical_address
;;     :enterpreneur.contacts_information]
;;    :query
;;    {:inner-join
;;     [:cache_register->point_of_sale :point_of_sale->enterpreneur],
;;     :columns
;;     (as-is
;;      :cache_register.id
;;      :cache_register.id_point_of_sale
;;      :cache_register.name
;;      :cache_register.serial_number
;;      :cache_register.fiscal_number
;;      :cache_register.manufacture_date
;;      :cache_register.first_registration_date
;;      :cache_register.is_working
;;      :cache_register.version
;;      :cache_register.dev_id
;;      :cache_register.producer
;;      :cache_register.modem
;;      :cache_register.modem_model
;;      :cache_register.modem_serial_number
;;      :cache_register.modem_phone_number
;;      :point_of_sale.id
;;      :point_of_sale.id_enterpreneur
;;      :point_of_sale.name
;;      :point_of_sale.physical_address
;;      :point_of_sale.telefons
;;      :enterpreneur.id
;;      :enterpreneur.ssreou
;;      :enterpreneur.ownership_form
;;      :enterpreneur.vat_certificate
;;      :enterpreneur.individual_tax_number
;;      :enterpreneur.director
;;      :enterpreneur.accountant
;;      :enterpreneur.legal_address
;;      :enterpreneur.physical_address
;;      :enterpreneur.contacts_information)}))
;; (defview
;;   point_of_sale_group
;;   (jarman-table
;;    :name
;;    "point_of_sale_group"
;;    :plug-place
;;    [:#tables-view-plugin]
;;    :tables
;;    [:point_of_sale_group]
;;    :model
;;    [:point_of_sale_group.id
;;     :point_of_sale_group.group_name
;;     :point_of_sale_group.information]
;;    :query
;;    {:column
;;     (as-is
;;      :point_of_sale_group.id
;;      :point_of_sale_group.group_name
;;      :point_of_sale_group.information)}))
;; (defview
;;   point_of_sale_group_links
;;   (jarman-table
;;    :name
;;    "point_of_sale_group_links"
;;    :plug-place
;;    [:#tables-view-plugin]
;;    :tables
;;    [:point_of_sale_group_links
;;     :point_of_sale_group
;;     :point_of_sale
;;     :enterpreneur]
;;    :model
;;    [:point_of_sale_group_links.id
;;     :point_of_sale_group_links.id_point_of_sale_group
;;     :point_of_sale_group_links.id_point_of_sale
;;     :point_of_sale_group.id
;;     :point_of_sale_group.group_name
;;     :point_of_sale_group.information
;;     :point_of_sale.id
;;     :point_of_sale.id_enterpreneur
;;     :point_of_sale.name
;;     :point_of_sale.physical_address
;;     :point_of_sale.telefons
;;     :enterpreneur.id
;;     :enterpreneur.ssreou
;;     :enterpreneur.ownership_form
;;     :enterpreneur.vat_certificate
;;     :enterpreneur.individual_tax_number
;;     :enterpreneur.director
;;     :enterpreneur.accountant
;;     :enterpreneur.legal_address
;;     :enterpreneur.physical_address
;;     :enterpreneur.contacts_information]
;;    :query
;;    {:inner-join
;;     [:point_of_sale_group_links->point_of_sale_group
;;      :point_of_sale_group_links->point_of_sale
;;      :point_of_sale->enterpreneur],
;;     :columns
;;     (as-is
;;      :point_of_sale_group_links.id
;;      :point_of_sale_group_links.id_point_of_sale_group
;;      :point_of_sale_group_links.id_point_of_sale
;;      :point_of_sale_group.id
;;      :point_of_sale_group.group_name
;;      :point_of_sale_group.information
;;      :point_of_sale.id
;;      :point_of_sale.id_enterpreneur
;;      :point_of_sale.name
;;      :point_of_sale.physical_address
;;      :point_of_sale.telefons
;;      :enterpreneur.id
;;      :enterpreneur.ssreou
;;      :enterpreneur.ownership_form
;;      :enterpreneur.vat_certificate
;;      :enterpreneur.individual_tax_number
;;      :enterpreneur.director
;;      :enterpreneur.accountant
;;      :enterpreneur.legal_address
;;      :enterpreneur.physical_address
;;      :enterpreneur.contacts_information)}))
;; (defview
;;   seal
;;   (jarman-table
;;    :name
;;    "seal"
;;    :plug-place
;;    [:#tables-view-plugin]
;;    :tables
;;    [:seal]
;;    :model
;;    [:seal.id
;;     :seal.seal_number
;;     :seal.datetime_of_use
;;     :seal.datetime_of_remove]
;;    :query
;;    {:column
;;     (as-is
;;      :seal.id
;;      :seal.seal_number
;;      :seal.datetime_of_use
;;      :seal.datetime_of_remove)}))
;; (defview
;;   repair_contract
;;   (jarman-table
;;    :name
;;    "repair_contract"
;;    :plug-place
;;    [:#tables-view-plugin]
;;    :tables
;;    [:repair_contract
;;     :cache_register
;;     :point_of_sale
;;     :enterpreneur
;;     :old_seal
;;     :new_seal
;;     :repair_reasons
;;     :repair_technical_issue
;;     :repair_nature_of_problem]
;;    :model
;;    [:repair_contract.id
;;     :repair_contract.id_cache_register
;;     :repair_contract.id_old_seal
;;     :repair_contract.id_new_seal
;;     :repair_contract.id_repair_reasons
;;     :repair_contract.id_repair_technical_issue
;;     :repair_contract.id_repair_nature_of_problem
;;     :repair_contract.repair_date
;;     :repair_contract.cache_register_register_date
;;     :cache_register.id
;;     :cache_register.id_point_of_sale
;;     :cache_register.name
;;     :cache_register.serial_number
;;     :cache_register.fiscal_number
;;     :cache_register.manufacture_date
;;     :cache_register.first_registration_date
;;     :cache_register.is_working
;;     :cache_register.version
;;     :cache_register.dev_id
;;     :cache_register.producer
;;     :cache_register.modem
;;     :cache_register.modem_model
;;     :cache_register.modem_serial_number
;;     :cache_register.modem_phone_number
;;     :point_of_sale.id
;;     :point_of_sale.id_enterpreneur
;;     :point_of_sale.name
;;     :point_of_sale.physical_address
;;     :point_of_sale.telefons
;;     :enterpreneur.id
;;     :enterpreneur.ssreou
;;     :enterpreneur.ownership_form
;;     :enterpreneur.vat_certificate
;;     :enterpreneur.individual_tax_number
;;     :enterpreneur.director
;;     :enterpreneur.accountant
;;     :enterpreneur.legal_address
;;     :enterpreneur.physical_address
;;     :enterpreneur.contacts_information
;;     :old_seal.id
;;     :old_seal.seal_number
;;     :old_seal.datetime_of_use
;;     :old_seal.datetime_of_remove
;;     :new_seal.id
;;     :new_seal.seal_number
;;     :new_seal.datetime_of_use
;;     :new_seal.datetime_of_remove
;;     :repair_reasons.id
;;     :repair_reasons.description
;;     :repair_technical_issue.id
;;     :repair_technical_issue.description
;;     :repair_nature_of_problem.id
;;     :repair_nature_of_problem.description]
;;    :query
;;    {:inner-join
;;     [:repair_contract->cache_register
;;      :cache_register->point_of_sale
;;      :point_of_sale->enterpreneur
;;      :repair_contract.id_old_seal->seal.id
;;      :repair_contract.id_new_seal->seal.id
;;      :repair_contract->repair_reasons
;;      :repair_contract->repair_technical_issue
;;      :repair_contract->repair_nature_of_problem],
;;     :columns
;;     (as-is
;;      :repair_contract.id
;;      :repair_contract.id_cache_register
;;      :repair_contract.id_old_seal
;;      :repair_contract.id_new_seal
;;      :repair_contract.id_repair_reasons
;;      :repair_contract.id_repair_technical_issue
;;      :repair_contract.id_repair_nature_of_problem
;;      :repair_contract.repair_date
;;      :repair_contract.cache_register_register_date
;;      :cache_register.id
;;      :cache_register.id_point_of_sale
;;      :cache_register.name
;;      :cache_register.serial_number
;;      :cache_register.fiscal_number
;;      :cache_register.manufacture_date
;;      :cache_register.first_registration_date
;;      :cache_register.is_working
;;      :cache_register.version
;;      :cache_register.dev_id
;;      :cache_register.producer
;;      :cache_register.modem
;;      :cache_register.modem_model
;;      :cache_register.modem_serial_number
;;      :cache_register.modem_phone_number
;;      :point_of_sale.id
;;      :point_of_sale.id_enterpreneur
;;      :point_of_sale.name
;;      :point_of_sale.physical_address
;;      :point_of_sale.telefons
;;      :enterpreneur.id
;;      :enterpreneur.ssreou
;;      :enterpreneur.ownership_form
;;      :enterpreneur.vat_certificate
;;      :enterpreneur.individual_tax_number
;;      :enterpreneur.director
;;      :enterpreneur.accountant
;;      :enterpreneur.legal_address
;;      :enterpreneur.physical_address
;;      :enterpreneur.contacts_information
;;      :old_seal.id
;;      :old_seal.seal_number
;;      :old_seal.datetime_of_use
;;      :old_seal.datetime_of_remove
;;      :new_seal.id
;;      :new_seal.seal_number
;;      :new_seal.datetime_of_use
;;      :new_seal.datetime_of_remove
;;      :repair_reasons.id
;;      :repair_reasons.description
;;      :repair_technical_issue.id
;;      :repair_technical_issue.description
;;      :repair_nature_of_problem.id
;;      :repair_nature_of_problem.description)}))
;; (defview
;;   service_contract
;;   (jarman-table
;;    :name
;;    "service_contract"
;;    :plug-place
;;    [:#tables-view-plugin]
;;    :tables
;;    [:service_contract :enterpreneur]
;;    :model
;;    [:service_contract.id
;;     :service_contract.id_enterpreneur
;;     :service_contract.contract_start_term
;;     :service_contract.contract_end_term
;;     :service_contract.money_per_month
;;     :enterpreneur.id
;;     :enterpreneur.ssreou
;;     :enterpreneur.ownership_form
;;     :enterpreneur.vat_certificate
;;     :enterpreneur.individual_tax_number
;;     :enterpreneur.director
;;     :enterpreneur.accountant
;;     :enterpreneur.legal_address
;;     :enterpreneur.physical_address
;;     :enterpreneur.contacts_information]
;;    :query
;;    {:inner-join [:service_contract->enterpreneur],
;;     :columns
;;     (as-is
;;      :service_contract.id
;;      :service_contract.id_enterpreneur
;;      :service_contract.contract_start_term
;;      :service_contract.contract_end_term
;;      :service_contract.money_per_month
;;      :enterpreneur.id
;;      :enterpreneur.ssreou
;;      :enterpreneur.ownership_form
;;      :enterpreneur.vat_certificate
;;      :enterpreneur.individual_tax_number
;;      :enterpreneur.director
;;      :enterpreneur.accountant
;;      :enterpreneur.legal_address
;;      :enterpreneur.physical_address
;;      :enterpreneur.contacts_information)}))
;; (defview
;;   service_contract_month
;;   (jarman-table
;;    :name
;;    "service_contract_month"
;;    :plug-place
;;    [:#tables-view-plugin]
;;    :tables
;;    [:service_contract_month :service_contract :enterpreneur]
;;    :model
;;    [:service_contract_month.id
;;     :service_contract_month.id_service_contract
;;     :service_contract_month.service_month_date
;;     :service_contract_month.money_per_month
;;     :service_contract.id
;;     :service_contract.id_enterpreneur
;;     :service_contract.contract_start_term
;;     :service_contract.contract_end_term
;;     :service_contract.money_per_month
;;     :enterpreneur.id
;;     :enterpreneur.ssreou
;;     :enterpreneur.ownership_form
;;     :enterpreneur.vat_certificate
;;     :enterpreneur.individual_tax_number
;;     :enterpreneur.director
;;     :enterpreneur.accountant
;;     :enterpreneur.legal_address
;;     :enterpreneur.physical_address
;;     :enterpreneur.contacts_information]
;;    :query
;;    {:inner-join
;;     [:service_contract_month->service_contract
;;      :service_contract->enterpreneur],
;;     :columns
;;     (as-is
;;      :service_contract_month.id
;;      :service_contract_month.id_service_contract
;;      :service_contract_month.service_month_date
;;      :service_contract_month.money_per_month
;;      :service_contract.id
;;      :service_contract.id_enterpreneur
;;      :service_contract.contract_start_term
;;      :service_contract.contract_end_term
;;      :service_contract.money_per_month
;;      :enterpreneur.id
;;      :enterpreneur.ssreou
;;      :enterpreneur.ownership_form
;;      :enterpreneur.vat_certificate
;;      :enterpreneur.individual_tax_number
;;      :enterpreneur.director
;;      :enterpreneur.accountant
;;      :enterpreneur.legal_address
;;      :enterpreneur.physical_address
;;      :enterpreneur.contacts_information)}))

;; Overriding and component custom adding
(defview
  user
  (jarman-table
   :name
   "user"
   :plug-place [:#tables-view-plugin]
   :tables [:user :permission]
   :view-columns [:user.login
                  :user.first_name
                  :user.last_name
                  :permission.permission_name]
   :model [{:model-reprs "Login"
            :model-param :user.login
            :bind-args {:title :title
                        :store-id :store-id
                        :local-changes :local-changes
                        :val :val}
            :model-comp 'gcomp/input-text-area-label}
           :user.password
           :user.first_name
           :user.last_name
           :user.id_permission
           {:model-reprs "Start user"
            :model-param :user-start
            :model-comp 'gcomp/input-int}
           {:model-reprs "End user"
            :model-param :user-end
            :model-comp 'gcomp/input-int}]
   :query {:inner-join [:user->permission]
           :columns
           (as-is
            :user.id
            :user.login
            :user.password
            :user.first_name
            :user.last_name
            :user.id_permission
            :permission.id
            :permission.permission_name
            :permission.configuration)}
   :actions {:add-multiply-users-insert
             (fn [state]
               (let [{{user-start :user-start user-end :user-end} :model} state]
                 {:table-name :user :set (map #(hash-map :user.login      (str "user" %)
                                                         :user.password   "1234"
                                                         :user.last_name  (str "user" %)
                                                         :user.first_name (str "user" %)
                                                         :user.id_permission 2)
                                              (range user-start user-end))}))}
   :buttons [{:action :add-multiply-users-insert
              :text "Auto generate users"}]))

