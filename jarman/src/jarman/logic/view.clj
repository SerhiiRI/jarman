{:host "trashpanda-team.ddns.net", :port 3307, :dbname "jarman"}

(in-ns 'jarman.logic.view-manager)

(defview
  permission
  (fff
   :id :fff
   :permission [:admin :developer :user]
   :name "FFF"
   :plug-place [:#tables-view-plugin])
  (table
   :id :permission
   :name "permission"
   :plug-place [:#tables-view-plugin]
   :tables [:permission]
   :view-columns [:permission.permission_name :permission.configuration]
   :model-insert [:permission.permission_name :permission.configuration]
   :active-buttons [:insert :update :delete :clear :changes]
   :permission [:user :developer]
   :actions []
   :buttons []
   :query
   {:table_name :permission,
    :column
    [:#as_is
     :permission.id
     :permission.permission_name
     :permission.configuration]})
  (dialog-table
   :id :permission-table
   :name "permission dialog"
   :permission [:admin :user :developer]
   :tables [:permission]
   :view-columns [:permission.permission_name :permission.configuration]
   :query
   {:table_name :permission,
    :column
    [:#as_is
     :permission.id
     :permission.permission_name
     :permission.configuration]}))

(defview 
  user
  (user-managment
   :id :user
   :name "user"
   :permission [:user :admin :developer]
   :plug-place [:#tables-view-plugin]
   :tables [:user :permission]
   :view-columns [:user.login
                  :user.first_name
                  :user.last_name
                  :permission.permission_name]
   :dialog {:user.id_permission
            [:permission :dialog-table :permission-table]}
   :query
   {:table_name :user,
    :inner-join [:user->permission],
    :column
    [:#as_is
     :user.id
     :user.login
     :user.password
     :user.first_name
     :user.last_name
     :user.id_permission
     :user.configuration
     :permission.id
     :permission.permission_name
     :permission.configuration]})
  (table
   :id :user
   :name "user"
   :plug-place [:#tables-view-plugin]
   :tables [:user :permission]
   :view-columns [:user.login
                  :user.password
                  :user.first_name
                  :user.last_name
                  :permission.permission_name]
   :model-insert [:user.login
                  :user.password
                  :user.first_name
                  :user.last_name
                  :user.id_permission
                  :user.configuration]
   :active-buttons [:insert :update :delete :clear :changes]
   :permission [:user :developer]
   :dialog {:user.id_permission
            [:permission :dialog-table :permission-table]}
   :actions []
   :buttons []
   :query
   {:table_name :user,
    :inner-join [:user->permission],
    :column
    [:#as_is
     :user.id
     :user.login
     :user.password
     :user.first_name
     :user.last_name
     :user.id_permission
     :permission.id
     :permission.permission_name
     :permission.configuration]}))

(defview service_contract
  (service-period
   :id :service_contract
   :permission [:admin :developer :user]
   :name "service_contract"
   :plug-place [:#tables-view-plugin]))

(defview
  cache_register
  (table
   :id :cache_register
   :name "cache_register"
   :plug-place [:#tables-view-plugin]
   :tables [:cache_register :point_of_sale :enterpreneur]
   :view-columns [:cache_register.id_point_of_sale
                  :cache_register.name
                  :cache_register.serial_number
                  :cache_register.fiscal_number
                  :cache_register.manufacture_date
                  :cache_register.first_registration_date
                  :cache_register.is_working
                  :cache_register.version
                  :cache_register.dev_id
                  :cache_register.producer
                  :cache_register.modem
                  :cache_register.modem_model
                  :cache_register.modem_serial_number
                  :cache_register.modem_phone_number]
   :model-insert [:cache_register.id_point_of_sale
                  :cache_register.name
                  :cache_register.serial_number
                  :cache_register.fiscal_number
                  :cache_register.manufacture_date
                  :cache_register.first_registration_date
                  :cache_register.is_working
                  :cache_register.version
                  :cache_register.dev_id
                  :cache_register.producer
                  :cache_register.modem
                  :cache_register.modem_model
                  :cache_register.modem_serial_number
                  :cache_register.modem_phone_number]
   :active-buttons [:insert :update :delete :clear :changes]
   :permission [:admin :user :developer]
   :dialog {:cache_register.id_point_of_sale
            [:point_of_sale :dialog-table :point_of_sale-table]}
   :actions []
   :buttons []
   :query
   {:table_name :cache_register,
    :inner-join
    [:cache_register->point_of_sale :point_of_sale->enterpreneur],
    :column
    [:#as_is
     :cache_register.id
     :cache_register.id_point_of_sale
     :cache_register.name
     :cache_register.serial_number
     :cache_register.fiscal_number
     :cache_register.manufacture_date
     :cache_register.first_registration_date
     :cache_register.is_working
     :cache_register.version
     :cache_register.dev_id
     :cache_register.producer
     :cache_register.modem
     :cache_register.modem_model
     :cache_register.modem_serial_number
     :cache_register.modem_phone_number
     :point_of_sale.id
     :point_of_sale.id_enterpreneur
     :point_of_sale.name
     :point_of_sale.physical_address
     :point_of_sale.telefons
     :enterpreneur.id
     :enterpreneur.ssreou
     :enterpreneur.ownership_form
     :enterpreneur.vat_certificate
     :enterpreneur.individual_tax_number
     :enterpreneur.director
     :enterpreneur.accountant
     :enterpreneur.legal_address
     :enterpreneur.physical_address
     :enterpreneur.contacts_information]})
  (dialog-table
   :id :cache_register-table
   :name "cache_register dialog"
   :permission [:admin :user :developer]
   :tables [:cache_register :point_of_sale :enterpreneur]
   :view-columns [:cache_register.id_point_of_sale
                  :cache_register.name
                  :cache_register.serial_number
                  :cache_register.fiscal_number
                  :cache_register.manufacture_date
                  :cache_register.first_registration_date
                  :cache_register.is_working
                  :cache_register.version
                  :cache_register.dev_id
                  :cache_register.producer
                  :cache_register.modem
                  :cache_register.modem_model
                  :cache_register.modem_serial_number
                  :cache_register.modem_phone_number]
   :query
   {:table_name :cache_register,
    :inner-join
    [:cache_register->point_of_sale :point_of_sale->enterpreneur],
    :column
    [:#as_is
     :cache_register.id
     :cache_register.id_point_of_sale
     :cache_register.name
     :cache_register.serial_number
     :cache_register.fiscal_number
     :cache_register.manufacture_date
     :cache_register.first_registration_date
     :cache_register.is_working
     :cache_register.version
     :cache_register.dev_id
     :cache_register.producer
     :cache_register.modem
     :cache_register.modem_model
     :cache_register.modem_serial_number
     :cache_register.modem_phone_number
     :point_of_sale.id
     :point_of_sale.id_enterpreneur
     :point_of_sale.name
     :point_of_sale.physical_address
     :point_of_sale.telefons
     :enterpreneur.id
     :enterpreneur.ssreou
     :enterpreneur.ownership_form
     :enterpreneur.vat_certificate
     :enterpreneur.individual_tax_number
     :enterpreneur.director
     :enterpreneur.accountant
     :enterpreneur.legal_address
     :enterpreneur.physical_address
     :enterpreneur.contacts_information]}))

(defview
  documents
  (table
   :id :documents
   :name "documents"
   :plug-place [:#tables-view-plugin]
   :tables [:documents]
   :view-columns [:documents.table_name
                  :documents.name
                  :documents.document
                  :documents.prop]
   :model-insert [:documents.table_name
                  :documents.name
                  :documents.document
                  :documents.prop]
   :active-buttons [:insert :update :delete :clear :changes]
   :permission [:admin :user :developer]
   :actions []
   :buttons []
   :query
   {:table_name :documents,
    :column
    [:#as_is
     :documents.id
     :documents.table_name
     :documents.name
     :documents.document
     :documents.prop]}))

(defview
  enterpreneur
  (table
   :id :enterpreneur
   :name "enterpreneur"
   :plug-place [:#tables-view-plugin]
   :tables [:enterpreneur]
   :view-columns [:enterpreneur.ssreou
                  :enterpreneur.ownership_form
                  :enterpreneur.vat_certificate
                  :enterpreneur.individual_tax_number
                  :enterpreneur.director
                  :enterpreneur.accountant
                  :enterpreneur.legal_address
                  :enterpreneur.physical_address
                  :enterpreneur.contacts_information]
   :model-insert [:enterpreneur.ssreou
                  :enterpreneur.ownership_form
                  :enterpreneur.vat_certificate
                  :enterpreneur.individual_tax_number
                  :enterpreneur.director
                  :enterpreneur.accountant
                  :enterpreneur.legal_address
                  :enterpreneur.physical_address
                  :enterpreneur.contacts_information]
   :active-buttons [:insert :update :delete :clear :changes]
   :permission [:admin :user :developer]
   :actions []
   :buttons []
   :query
   {:table_name :enterpreneur,
    :column
    [:#as_is
     :enterpreneur.id
     :enterpreneur.ssreou
     :enterpreneur.ownership_form
     :enterpreneur.vat_certificate
     :enterpreneur.individual_tax_number
     :enterpreneur.director
     :enterpreneur.accountant
     :enterpreneur.legal_address
     :enterpreneur.physical_address
     :enterpreneur.contacts_information]})
  (dialog-table
   :id :enterpreneur-table
   :name "enterpreneur dialog"
   :permission [:admin :user :developer]
   :tables [:enterpreneur]
   :view-columns [:enterpreneur.ssreou
                  :enterpreneur.ownership_form
                  :enterpreneur.vat_certificate
                  :enterpreneur.individual_tax_number
                  :enterpreneur.director
                  :enterpreneur.accountant
                  :enterpreneur.legal_address
                  :enterpreneur.physical_address
                  :enterpreneur.contacts_information]
   :query
   {:table_name :enterpreneur,
    :column
    [:#as_is
     :enterpreneur.id
     :enterpreneur.ssreou
     :enterpreneur.ownership_form
     :enterpreneur.vat_certificate
     :enterpreneur.individual_tax_number
     :enterpreneur.director
     :enterpreneur.accountant
     :enterpreneur.legal_address
     :enterpreneur.physical_address
     :enterpreneur.contacts_information]}))

(defview
  point_of_sale
  (table
   :id :point_of_sale
   :name "point_of_sale"
   :plug-place [:#tables-view-plugin]
   :tables [:point_of_sale :enterpreneur]
   :view-columns [:point_of_sale.id_enterpreneur
                  :point_of_sale.name
                  :point_of_sale.physical_address
                  :point_of_sale.telefons]
   :model-insert [:point_of_sale.id_enterpreneur
                  :point_of_sale.name
                  :point_of_sale.physical_address
                  :point_of_sale.telefons]
   :active-buttons [:insert :update :delete :clear :changes]
   :permission [:admin :user :developer]
   :dialog {:point_of_sale.id_enterpreneur
            [:enterpreneur :dialog-table :enterpreneur-table]}
   :actions []
   :buttons []
   :query
   {:table_name :point_of_sale,
    :inner-join [:point_of_sale->enterpreneur],
    :column
    [:#as_is
     :point_of_sale.id
     :point_of_sale.id_enterpreneur
     :point_of_sale.name
     :point_of_sale.physical_address
     :point_of_sale.telefons
     :enterpreneur.id
     :enterpreneur.ssreou
     :enterpreneur.ownership_form
     :enterpreneur.vat_certificate
     :enterpreneur.individual_tax_number
     :enterpreneur.director
     :enterpreneur.accountant
     :enterpreneur.legal_address
     :enterpreneur.physical_address
     :enterpreneur.contacts_information]})
  (dialog-table
   :id :point_of_sale-table
   :name "point_of_sale dialog"
   :permission [:admin :user :developer]
   :tables [:point_of_sale :enterpreneur]
   :view-columns [:point_of_sale.id_enterpreneur
                  :point_of_sale.name
                  :point_of_sale.physical_address
                  :point_of_sale.telefons]
   :query
   {:table_name :point_of_sale,
    :inner-join [:point_of_sale->enterpreneur],
    :column
    [:#as_is
     :point_of_sale.id
     :point_of_sale.id_enterpreneur
     :point_of_sale.name
     :point_of_sale.physical_address
     :point_of_sale.telefons
     :enterpreneur.id
     :enterpreneur.ssreou
     :enterpreneur.ownership_form
     :enterpreneur.vat_certificate
     :enterpreneur.individual_tax_number
     :enterpreneur.director
     :enterpreneur.accountant
     :enterpreneur.legal_address
     :enterpreneur.physical_address
     :enterpreneur.contacts_information]}))

(defview
  point_of_sale_group
  (table
   :id :point_of_sale_group
   :name "point_of_sale_group"
   :plug-place [:#tables-view-plugin]
   :tables [:point_of_sale_group]
   :view-columns [:point_of_sale_group.group_name :point_of_sale_group.information]
   :model-insert [:point_of_sale_group.group_name :point_of_sale_group.information]
   :active-buttons [:insert :update :delete :clear :changes]
   :permission [:admin :user :developer]
   :actions []
   :buttons []
   :query
   {:table_name :point_of_sale_group,
    :column
    [:#as_is
     :point_of_sale_group.id
     :point_of_sale_group.group_name
     :point_of_sale_group.information]})
  (dialog-table
   :id :point_of_sale_group-table
   :name "point_of_sale_group dialog"
   :permission [:admin :user :developer]
   :tables [:point_of_sale_group]
   :view-columns [:point_of_sale_group.group_name :point_of_sale_group.information]
   :query
   {:table_name :point_of_sale_group,
    :column
    [:#as_is
     :point_of_sale_group.id
     :point_of_sale_group.group_name
     :point_of_sale_group.information]}))

(defview
  point_of_sale_group_links
  (table
   :id :point_of_sale_group_links
   :name "point_of_sale_group_links"
   :plug-place [:#tables-view-plugin]
   :tables [:point_of_sale_group_links
            :point_of_sale_group
            :point_of_sale
            :enterpreneur]
   :view-columns [:point_of_sale_group_links.id_point_of_sale_group
                  :point_of_sale_group_links.id_point_of_sale]
   :model-insert [:point_of_sale_group_links.id_point_of_sale_group
                  :point_of_sale_group_links.id_point_of_sale]
   :active-buttons [:insert :update :delete :clear :changes]
   :permission [:admin :user :developer]
   :dialog {:point_of_sale_group_links.id_point_of_sale_group
            [:point_of_sale_group :dialog-table :point_of_sale_group-table],
            :point_of_sale_group_links.id_point_of_sale
            [:point_of_sale :dialog-table :point_of_sale-table]}
   :actions []
   :buttons []
   :query
   {:table_name :point_of_sale_group_links,
    :inner-join
    [:point_of_sale_group_links->point_of_sale_group
     :point_of_sale_group_links->point_of_sale
     :point_of_sale->enterpreneur],
    :column
    [:#as_is
     :point_of_sale_group_links.id
     :point_of_sale_group_links.id_point_of_sale_group
     :point_of_sale_group_links.id_point_of_sale
     :point_of_sale_group.id
     :point_of_sale_group.group_name
     :point_of_sale_group.information
     :point_of_sale.id
     :point_of_sale.id_enterpreneur
     :point_of_sale.name
     :point_of_sale.physical_address
     :point_of_sale.telefons
     :enterpreneur.id
     :enterpreneur.ssreou
     :enterpreneur.ownership_form
     :enterpreneur.vat_certificate
     :enterpreneur.individual_tax_number
     :enterpreneur.director
     :enterpreneur.accountant
     :enterpreneur.legal_address
     :enterpreneur.physical_address
     :enterpreneur.contacts_information]})
  (dialog-table
   :id :point_of_sale_group_links-table
   :name "point_of_sale_group_links dialog"
   :permission [:admin :user :developer]
   :tables [:point_of_sale_group_links
            :point_of_sale_group
            :point_of_sale
            :enterpreneur]
   :view-columns [:point_of_sale_group_links.id_point_of_sale_group
                  :point_of_sale_group_links.id_point_of_sale]
   :query
   {:table_name :point_of_sale_group_links,
    :inner-join
    [:point_of_sale_group_links->point_of_sale_group
     :point_of_sale_group_links->point_of_sale
     :point_of_sale->enterpreneur],
    :column
    [:#as_is
     :point_of_sale_group_links.id
     :point_of_sale_group_links.id_point_of_sale_group
     :point_of_sale_group_links.id_point_of_sale
     :point_of_sale_group.id
     :point_of_sale_group.group_name
     :point_of_sale_group.information
     :point_of_sale.id
     :point_of_sale.id_enterpreneur
     :point_of_sale.name
     :point_of_sale.physical_address
     :point_of_sale.telefons
     :enterpreneur.id
     :enterpreneur.ssreou
     :enterpreneur.ownership_form
     :enterpreneur.vat_certificate
     :enterpreneur.individual_tax_number
     :enterpreneur.director
     :enterpreneur.accountant
     :enterpreneur.legal_address
     :enterpreneur.physical_address
     :enterpreneur.contacts_information]}))

(defview
  repair_contract
  (table
   :id :repair_contract
   :name "repair_contract"
   :plug-place [:#tables-view-plugin]
   :tables [:repair_contract
            :cache_register
            :point_of_sale
            :enterpreneur
            :old_seal
            :new_seal
            :repair_reasons
            :repair_technical_issue
            :repair_nature_of_problem]
   :view-columns [:repair_contract.id_cache_register
                  :repair_contract.id_old_seal
                  :repair_contract.id_new_seal
                  :repair_contract.id_repair_reasons
                  :repair_contract.id_repair_technical_issue
                  :repair_contract.id_repair_nature_of_problem
                  :repair_contract.repair_date
                  :repair_contract.cache_register_register_date]
   :model-insert [:repair_contract.id_cache_register
                  :repair_contract.id_old_seal
                  :repair_contract.id_new_seal
                  :repair_contract.id_repair_reasons
                  :repair_contract.id_repair_technical_issue
                  :repair_contract.id_repair_nature_of_problem
                  :repair_contract.repair_date
                  :repair_contract.cache_register_register_date]
   :active-buttons [:insert :update :delete :clear :changes]
   :permission [:admin :user :developer]
   :dialog {:repair_contract.id_cache_register
            [:cache_register :dialog-table :cache_register-table],
            :repair_contract.id_old_seal
            [:seal :dialog-table :seal-table],
            :repair_contract.id_new_seal
            [:seal :dialog-table :seal-table],
            :repair_contract.id_repair_reasons
            [:repair_reasons :dialog-bigstring :repair_reasons-bigstring],
            :repair_contract.id_repair_technical_issue
            [:repair_technical_issue
             :dialog-bigstring
             :repair_technical_issue-bigstring],
            :repair_contract.id_repair_nature_of_problem
            [:repair_nature_of_problem
             :dialog-bigstring
             :repair_nature_of_problem-bigstring]}
   :actions []
   :buttons []
   :query
   {:table_name :repair_contract,
    :inner-join
    [:repair_contract->cache_register
     :cache_register->point_of_sale
     :point_of_sale->enterpreneur
     :repair_contract.id_old_seal->seal*old_seal.id
     :repair_contract.id_new_seal->seal*new_seal.id
     :repair_contract->repair_reasons
     :repair_contract->repair_technical_issue
     :repair_contract->repair_nature_of_problem],
    :column
    [:#as_is
     :repair_contract.id
     :repair_contract.id_cache_register
     :repair_contract.id_old_seal
     :repair_contract.id_new_seal
     :repair_contract.id_repair_reasons
     :repair_contract.id_repair_technical_issue
     :repair_contract.id_repair_nature_of_problem
     :repair_contract.repair_date
     :repair_contract.cache_register_register_date
     :cache_register.id
     :cache_register.id_point_of_sale
     :cache_register.name
     :cache_register.serial_number
     :cache_register.fiscal_number
     :cache_register.manufacture_date
     :cache_register.first_registration_date
     :cache_register.is_working
     :cache_register.version
     :cache_register.dev_id
     :cache_register.producer
     :cache_register.modem
     :cache_register.modem_model
     :cache_register.modem_serial_number
     :cache_register.modem_phone_number
     :point_of_sale.id
     :point_of_sale.id_enterpreneur
     :point_of_sale.name
     :point_of_sale.physical_address
     :point_of_sale.telefons
     :enterpreneur.id
     :enterpreneur.ssreou
     :enterpreneur.ownership_form
     :enterpreneur.vat_certificate
     :enterpreneur.individual_tax_number
     :enterpreneur.director
     :enterpreneur.accountant
     :enterpreneur.legal_address
     :enterpreneur.physical_address
     :enterpreneur.contacts_information
     :old_seal.id
     :old_seal.seal_number
     :old_seal.datetime_of_use
     :old_seal.datetime_of_remove
     :new_seal.id
     :new_seal.seal_number
     :new_seal.datetime_of_use
     :new_seal.datetime_of_remove
     :repair_reasons.id
     :repair_reasons.description
     :repair_technical_issue.id
     :repair_technical_issue.description
     :repair_nature_of_problem.id
     :repair_nature_of_problem.description]})
  (dialog-table
   :id :repair_contract-table
   :name "repair_contract dialog"
   :permission [:admin :user :developer]
   :tables [:repair_contract
            :cache_register
            :point_of_sale
            :enterpreneur
            :old_seal
            :new_seal
            :repair_reasons
            :repair_technical_issue
            :repair_nature_of_problem]
   :view-columns [:repair_contract.id_cache_register
                  :repair_contract.id_old_seal
                  :repair_contract.id_new_seal
                  :repair_contract.id_repair_reasons
                  :repair_contract.id_repair_technical_issue
                  :repair_contract.id_repair_nature_of_problem
                  :repair_contract.repair_date
                  :repair_contract.cache_register_register_date]
   :query
   {:table_name :repair_contract,
    :inner-join
    [:repair_contract->cache_register
     :cache_register->point_of_sale
     :point_of_sale->enterpreneur
     :repair_contract.id_old_seal->seal.id
     :repair_contract.id_new_seal->seal.id
     :repair_contract->repair_reasons
     :repair_contract->repair_technical_issue
     :repair_contract->repair_nature_of_problem],
    :column
    [:#as_is
     :repair_contract.id
     :repair_contract.id_cache_register
     :repair_contract.id_old_seal
     :repair_contract.id_new_seal
     :repair_contract.id_repair_reasons
     :repair_contract.id_repair_technical_issue
     :repair_contract.id_repair_nature_of_problem
     :repair_contract.repair_date
     :repair_contract.cache_register_register_date
     :cache_register.id
     :cache_register.id_point_of_sale
     :cache_register.name
     :cache_register.serial_number
     :cache_register.fiscal_number
     :cache_register.manufacture_date
     :cache_register.first_registration_date
     :cache_register.is_working
     :cache_register.version
     :cache_register.dev_id
     :cache_register.producer
     :cache_register.modem
     :cache_register.modem_model
     :cache_register.modem_serial_number
     :cache_register.modem_phone_number
     :point_of_sale.id
     :point_of_sale.id_enterpreneur
     :point_of_sale.name
     :point_of_sale.physical_address
     :point_of_sale.telefons
     :enterpreneur.id
     :enterpreneur.ssreou
     :enterpreneur.ownership_form
     :enterpreneur.vat_certificate
     :enterpreneur.individual_tax_number
     :enterpreneur.director
     :enterpreneur.accountant
     :enterpreneur.legal_address
     :enterpreneur.physical_address
     :enterpreneur.contacts_information
     :old_seal.id
     :old_seal.seal_number
     :old_seal.datetime_of_use
     :old_seal.datetime_of_remove
     :new_seal.id
     :new_seal.seal_number
     :new_seal.datetime_of_use
     :new_seal.datetime_of_remove
     :repair_reasons.id
     :repair_reasons.description
     :repair_technical_issue.id
     :repair_technical_issue.description
     :repair_nature_of_problem.id
     :repair_nature_of_problem.description]}))

(defview
  repair_nature_of_problem
  (table
   :id  :repair_nature_of_problem
   :name "repair_nature_of_problem"
   :plug-place [:#tables-view-plugin]
   :tables [:repair_nature_of_problem]
   :view-columns [:repair_nature_of_problem.description]
   :model-insert [:repair_nature_of_problem.description]
   :active-buttons [:insert :update :delete :clear :changes]
   :permission [:admin :user :developer]
   :actions []
   :buttons []
   :query
   {:table_name :repair_nature_of_problem,
    :column
    [:#as_is
     :repair_nature_of_problem.id
     :repair_nature_of_problem.description]})
  (dialog-bigstring
   :id :repair_nature_of_problem-bigstring
   :name "repair_nature_of_problem dialog"
   :permission [:admin :user :developer]
   :item-columns
   :repair_nature_of_problem.description
   :query
   {:table_name :repair_nature_of_problem,
    :column
    [:repair_nature_of_problem.id
     :repair_nature_of_problem.description]}))

(defview
  repair_reasons
  (table
   :id :repair_reasons
   :name "repair_reasons"
   :plug-place [:#tables-view-plugin]
   :tables [:repair_reasons]
   :view-columns [:repair_reasons.description]
   :model-insert [:repair_reasons.description]
   :active-buttons [:insert :update :delete :clear :changes]
   :permission [:admin :user :developer]
   :dialog {}
   :actions []
   :buttons []
   :query
   {:table_name :repair_reasons,
    :column
    [:#as_is :repair_reasons.id :repair_reasons.description]})
  (dialog-bigstring
   :id :repair_reasons-bigstring
   :name "repair_reasons dialog"
   :permission [:admin :user :developer]
   :item-columns
   :repair_reasons.description
   :query
   {:table_name :repair_reasons,
    :column [:repair_reasons.id :repair_reasons.description]}))

(defview
  repair_technical_issue
  (table
   :id :repair_technical_issue
   :name "repair_technical_issue"
   :plug-place [:#tables-view-plugin]
   :tables [:repair_technical_issue]
   :view-columns [:repair_technical_issue.description]
   :model-insert [:repair_technical_issue.description]
   :active-buttons [:insert :update :delete :clear :changes]
   :permission [:admin :user :developer]
   :actions []
   :buttons []
   :query
   {:table_name :repair_technical_issue,
    :column
    [:#as_is
     :repair_technical_issue.id
     :repair_technical_issue.description]})
  (dialog-bigstring
   :id :repair_technical_issue-bigstring
   :name "repair_technical_issue dialog"
   :permission [:admin :user :developer]
   :item-columns
   :repair_technical_issue.description
   :query
   {:table_name :repair_technical_issue,
    :column
    [:repair_technical_issue.id
     :repair_technical_issue.description]}))

(defview
  seal
  (table
   :id :seal
   :name "seal"
   :plug-place [:#tables-view-plugin]
   :tables [:seal]
   :view-columns [:seal.seal_number :seal.datetime_of_use :seal.datetime_of_remove]
   :model-insert [:seal.seal_number :seal.datetime_of_use :seal.datetime_of_remove]
   :active-buttons [:insert :update :delete :clear :changes]
   :permission [:admin :user :developer]
   :dialog {}
   :actions []
   :buttons []
   :query
   {:table_name :seal,
    :column
    [:#as_is
     :seal.id
     :seal.seal_number
     :seal.datetime_of_use
     :seal.datetime_of_remove]})
  (dialog-table
   :id :seal-table
   :name "seal dialog"
   :permission [:admin :user :developer]
   :tables [:seal]
   :view-columns [:seal.seal_number :seal.datetime_of_use :seal.datetime_of_remove]
   :query
   {:table_name :seal,
    :column
    [:#as_is
     :seal.id
     :seal.seal_number
     :seal.datetime_of_use
     :seal.datetime_of_remove]}))

(defview
  service_contract
  (table
   :id :service_contract
   :name "service_contract"
   :plug-place [:#tables-view-plugin]
   :tables [:service_contract :enterpreneur]
   :view-columns [:service_contract.id_enterpreneur
                  :service_contract.contract_start_term
                  :service_contract.contract_end_term
                  :service_contract.money_per_month]
   :model-insert [:service_contract.id_enterpreneur
                  :service_contract.contract_start_term
                  :service_contract.contract_end_term
                  :service_contract.money_per_month]
   :active-buttons [:insert :update :delete :clear :changes]
   :permission [:admin :user :developer]
   :dialog {:service_contract.id_enterpreneur
            [:enterpreneur :dialog-table :enterpreneur-table]}
   :actions []
   :buttons []
   :query
   {:table_name :service_contract,
    :inner-join [:service_contract->enterpreneur],
    :column
    [:#as_is
     :service_contract.id
     :service_contract.id_enterpreneur
     :service_contract.contract_start_term
     :service_contract.contract_end_term
     :service_contract.money_per_month
     :enterpreneur.id
     :enterpreneur.ssreou
     :enterpreneur.ownership_form
     :enterpreneur.vat_certificate
     :enterpreneur.individual_tax_number
     :enterpreneur.director
     :enterpreneur.accountant
     :enterpreneur.legal_address
     :enterpreneur.physical_address
     :enterpreneur.contacts_information]})
  (dialog-table
   :id :service_contract-table
   :name "service_contract dialog"
   :permission [:admin :user :developer]
   :tables [:service_contract :enterpreneur]
   :view-columns [:service_contract.id_enterpreneur
                  :service_contract.contract_start_term
                  :service_contract.contract_end_term
                  :service_contract.money_per_month]
   :query
   {:table_name :service_contract,
    :inner-join [:service_contract->enterpreneur],
    :column
    [:#as_is
     :service_contract.id
     :service_contract.id_enterpreneur
     :service_contract.contract_start_term
     :service_contract.contract_end_term
     :service_contract.money_per_month
     :enterpreneur.id
     :enterpreneur.ssreou
     :enterpreneur.ownership_form
     :enterpreneur.vat_certificate
     :enterpreneur.individual_tax_number
     :enterpreneur.director
     :enterpreneur.accountant
     :enterpreneur.legal_address
     :enterpreneur.physical_address
     :enterpreneur.contacts_information]}))

(defview
  service_contract_month
  (table
   :id :service_contract_month
   :name "service_contract_month"
   :plug-place [:#tables-view-plugin]
   :tables [:service_contract_month :service_contract :enterpreneur]
   :view-columns [:service_contract_month.id_service_contract
                  :service_contract_month.service_month_date
                  :service_contract_month.money_per_month]
   :model-insert [:service_contract_month.id_service_contract
                  :service_contract_month.service_month_date
                  :service_contract_month.money_per_month]
   :active-buttons [:insert :update :delete :clear :changes]
   :permission [:admin :user :developer]
   :dialog {:service_contract_month.id_service_contract
            [:service_contract :dialog-table :service_contract-table]}
   :actions []
   :buttons []
   :query
   {:table_name :service_contract_month,
    :inner-join
    [:service_contract_month->service_contract
     :service_contract->enterpreneur],
    :column
    [:#as_is
     :service_contract_month.id
     :service_contract_month.id_service_contract
     :service_contract_month.service_month_date
     :service_contract_month.money_per_month
     :service_contract.id
     :service_contract.id_enterpreneur
     :service_contract.contract_start_term
     :service_contract.contract_end_term
     :service_contract.money_per_month
     :enterpreneur.id
     :enterpreneur.ssreou
     :enterpreneur.ownership_form
     :enterpreneur.vat_certificate
     :enterpreneur.individual_tax_number
     :enterpreneur.director
     :enterpreneur.accountant
     :enterpreneur.legal_address
     :enterpreneur.physical_address
     :enterpreneur.contacts_information]})
  (dialog-table
   :id :service_contract_month-table
   :name "service_contract_month dialog"
   :permission [:admin :user :developer]
   :tables [:service_contract_month :service_contract :enterpreneur]
   :view-columns [:service_contract_month.id_service_contract
                  :service_contract_month.service_month_date
                  :service_contract_month.money_per_month]
   :query
   {:table_name :service_contract_month,
    :inner-join
    [:service_contract_month->service_contract
     :service_contract->enterpreneur],
    :column
    [:#as_is
     :service_contract_month.id
     :service_contract_month.id_service_contract
     :service_contract_month.service_month_start
     :service_contract_month.service_month_end
     :service_contract_month.money_per_month
     :service_contract_month.was_payed
     :service_contract.id
     :service_contract.id_enterpreneur
     :service_contract.contract_start_term
     :service_contract.contract_end_term
     :service_contract.money_per_month
     :enterpreneur.id
     :enterpreneur.ssreou
     :enterpreneur.ownership_form
     :enterpreneur.vat_certificate
     :enterpreneur.individual_tax_number
     :enterpreneur.director
     :enterpreneur.accountant
     :enterpreneur.legal_address
     :enterpreneur.physical_address
     :enterpreneur.contacts_information
     ]}))
