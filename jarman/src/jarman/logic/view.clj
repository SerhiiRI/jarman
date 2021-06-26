{:host "trashpanda-team.ddns.net", :port 3307, :dbname "jarman"}

(in-ns 'jarman.logic.view-manager)

(defview permission
  (table
   :name "permission"
   :plug-place [:#tables-view-plugin]
   :tables [:permission]
   :view-columns [:permission.permission_name :permission.configuration]
   :model-insert [:permission.permission_name :permission.configuration]
   :insert-button true
   :delete-button true
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
   :id :my-custom-dialog
   :name "My dialog box"
   :permission [:user]))

(defview user
  (table
   :name
   "user"
   :plug-place [:#tables-view-plugin]
   :tables [:user :permission]
   :view-columns [:user.login
                  :user.password
                  :user.first_name
                  :user.last_name
                  :permission.name]
   :model-insert [:user.login
                  :user.password
                  :user.first_name
                  :user.last_name
                  :user.id_permission]
   :insert-button true
   :delete-button true
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


(defview
  cache_register
  (table
   :name
   "cache_register"
   :plug-place
   [:#tables-view-plugin]
   :tables
   [:cache_register :point_of_sale :enterpreneur]
   :view-columns
   [:cache_register.id_point_of_sale
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
   :model-insert
   [:cache_register.id_point_of_sale
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
   :insert-button
   true
   :delete-button
   true
   :actions
   []
   :buttons
   []
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
(defview documents
  (table
   :name "Documnets import"
   :changes-button true
   :insert-button false
   :delete-button false
   :update-button false
   :export-button false
   :plug-place
   [:#tables-view-plugin]
   :tables
   [:documents]
   :view-columns
   [:documents.table_name
    :documents.name
    :documents.document
    :documents.prop]
   :model-insert [:documents.id
                  {:model-reprs "Table"
                   :model-param :documents.table_name
                   :model-comp jarman.gui.gui-components/select-box-table-list}
                  :documents.name
                  :documents.prop
                  {:model-reprs "Path to file"
                   :model-param :documents.document
                   :model-comp jarman.gui.gui-components/input-file}]
   :model-update [:documents.id
                  {:model-reprs "Table"
                   :model-param :documents.table_name
                   :model-comp jarman.gui.gui-components/select-box-table-list}
                  :documents.name
                  :documents.prop
                  ]
   :query {:column
           [{:documents.id :documents.id}
            {:documents.table_name  :documents.table_name}
            {:documents.name :documents.name}
            {:documents.prop :documents.prop}]}
   :actions {:upload-docs-to-db (fn [state]
                                  (let [insert-meta {:table    (first (:documents.table_name @state))
                                                     :name     (:documents.name @state)
                                                     :document (:documents.document @state)
                                                     :prop     (:documents.prop @state)}]
                                    (println "to save" insert-meta)
                                    (jarman.logic.document-manager/insert-document insert-meta)
                                    (((jarman.logic.state/state :jarman-views-service) :reload))))
             :update-docs-in-db (fn [state]
                                  (println "\nState" @state)
                                  (let [insert-meta {:id       (:selected-id @state)
                                                     :table    (first (:documents.table_name @state))
                                                     :name     (:documents.name @state)
                                                     :prop     (:documents.prop @state)
                                                     }]
                                    (println "to save" insert-meta)
                                    (jarman.logic.document-manager/insert-document insert-meta)
                                    (((jarman.logic.state/state :jarman-views-service) :reload))))
             :delete-doc-from-db (fn [state]
                                   (let [insert-meta {:id (:selected-id @state)}]
                                     (println "to delete" insert-meta)
                                     (jarman.logic.document-manager/delete-document insert-meta)
                                     (((jarman.logic.state/state :jarman-views-service) :reload))))}
   :buttons [{:form-model :model-insert
              :action :upload-docs-to-db
              :title "Upload document"}
             {:form-model :model-update
              :action :update-docs-in-db
              :title "Update document info"}
             {:form-model :model-update
              :action :delete-doc-from-db
              :title "Delete row"}
             ]))

(defview
  enterpreneur
  (table
   :name
   "enterpreneur"
   :plug-place
   [:#tables-view-plugin]
   :tables
   [:enterpreneur]
   :view-columns
   [:enterpreneur.ssreou
    :enterpreneur.ownership_form
    :enterpreneur.vat_certificate
    :enterpreneur.individual_tax_number
    :enterpreneur.director
    :enterpreneur.accountant
    :enterpreneur.legal_address
    :enterpreneur.physical_address
    :enterpreneur.contacts_information]
   :model-insert
   [:enterpreneur.ssreou
    :enterpreneur.ownership_form
    :enterpreneur.vat_certificate
    :enterpreneur.individual_tax_number
    :enterpreneur.director
    :enterpreneur.accountant
    :enterpreneur.legal_address
    :enterpreneur.physical_address
    :enterpreneur.contacts_information]
   :insert-button
   true
   :delete-button
   true
   :actions
   []
   :buttons
   []
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
   :name
   "point_of_sale"
   :plug-place
   [:#tables-view-plugin]
   :tables
   [:point_of_sale :enterpreneur]
   :view-columns
   [:point_of_sale.id_enterpreneur
    :point_of_sale.name
    :point_of_sale.physical_address
    :point_of_sale.telefons]
   :model-insert
   [:point_of_sale.id_enterpreneur
    :point_of_sale.name
    :point_of_sale.physical_address
    :point_of_sale.telefons]
   :insert-button
   true
   :delete-button
   true
   :actions
   []
   :buttons
   []
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
   :name
   "point_of_sale_group"
   :plug-place
   [:#tables-view-plugin]
   :tables
   [:point_of_sale_group]
   :view-columns
   [:point_of_sale_group.group_name :point_of_sale_group.information]
   :model-insert
   [:point_of_sale_group.group_name :point_of_sale_group.information]
   :insert-button
   true
   :delete-button
   true
   :actions
   []
   :buttons
   []
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
   :name
   "point_of_sale_group_links"
   :plug-place
   [:#tables-view-plugin]
   :tables
   [:point_of_sale_group_links
    :point_of_sale_group
    :point_of_sale
    :enterpreneur]
   :view-columns
   [:point_of_sale_group_links.id_point_of_sale_group
    :point_of_sale_group_links.id_point_of_sale]
   :model-insert
   [:point_of_sale_group_links.id_point_of_sale_group
    :point_of_sale_group_links.id_point_of_sale]
   :insert-button
   true
   :delete-button
   true
   :actions
   []
   :buttons
   []
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
   :name
   "repair_contract"
   :plug-place
   [:#tables-view-plugin]
   :tables
   [:repair_contract
    :cache_register
    :point_of_sale
    :enterpreneur
    :old_seal
    :new_seal
    :repair_reasons
    :repair_technical_issue
    :repair_nature_of_problem]
   :view-columns
   [:repair_contract.id_cache_register
    :repair_contract.id_old_seal
    :repair_contract.id_new_seal
    :repair_contract.id_repair_reasons
    :repair_contract.id_repair_technical_issue
    :repair_contract.id_repair_nature_of_problem
    :repair_contract.repair_date
    :repair_contract.cache_register_register_date]
   :model-insert
   [:repair_contract.id_cache_register
    :repair_contract.id_old_seal
    :repair_contract.id_new_seal
    :repair_contract.id_repair_reasons
    :repair_contract.id_repair_technical_issue
    :repair_contract.id_repair_nature_of_problem
    :repair_contract.repair_date
    :repair_contract.cache_register_register_date]
   :insert-button
   true
   :delete-button
   true
   :actions
   []
   :buttons
   []
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
   :name
   "repair_nature_of_problem"
   :plug-place
   [:#tables-view-plugin]
   :tables
   [:repair_nature_of_problem]
   :view-columns
   [:repair_nature_of_problem.description]
   :model-insert
   [:repair_nature_of_problem.description]
   :insert-button
   true
   :delete-button
   true
   :actions
   []
   :buttons
   []
   :query
   {:table_name :repair_nature_of_problem,
    :column
    [:#as_is
     :repair_nature_of_problem.id
     :repair_nature_of_problem.description]}))
(defview
  repair_reasons
  (table
   :name
   "repair_reasons"
   :plug-place
   [:#tables-view-plugin]
   :tables
   [:repair_reasons]
   :view-columns
   [:repair_reasons.description]
   :model-insert
   [:repair_reasons.description]
   :insert-button
   true
   :delete-button
   true
   :actions
   []
   :buttons
   []
   :query
   {:table_name :repair_reasons,
    :column
    [:#as_is :repair_reasons.id :repair_reasons.description]}))
(defview
  repair_technical_issue
  (table
   :name
   "repair_technical_issue"
   :plug-place
   [:#tables-view-plugin]
   :tables
   [:repair_technical_issue]
   :view-columns
   [:repair_technical_issue.description]
   :model-insert
   [:repair_technical_issue.description]
   :insert-button
   true
   :delete-button
   true
   :actions
   []
   :buttons
   []
   :query
   {:table_name :repair_technical_issue,
    :column
    [:#as_is
     :repair_technical_issue.id
     :repair_technical_issue.description]}))
(defview
  seal
  (table
   :name
   "seal"
   :plug-place
   [:#tables-view-plugin]
   :tables
   [:seal]
   :view-columns
   [:seal.seal_number :seal.datetime_of_use :seal.datetime_of_remove]
   :model-insert
   [:seal.seal_number :seal.datetime_of_use :seal.datetime_of_remove]
   :insert-button
   true
   :delete-button
   true
   :actions
   []
   :buttons
   []
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
   :name
   "service_contract"
   :plug-place
   [:#tables-view-plugin]
   :tables
   [:service_contract :enterpreneur]
   :view-columns
   [:service_contract.id_enterpreneur
    :service_contract.contract_start_term
    :service_contract.contract_end_term
    :service_contract.money_per_month]
   :model-insert
   [:service_contract.id_enterpreneur
    :service_contract.contract_start_term
    :service_contract.contract_end_term
    :service_contract.money_per_month]
   :insert-button
   true
   :delete-button
   true
   :actions
   []
   :buttons
   []
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
   :name
   "service_contract_month"
   :plug-place
   [:#tables-view-plugin]
   :tables
   [:service_contract_month :service_contract :enterpreneur]
   :view-columns
   [:service_contract_month.id_service_contract
    :service_contract_month.service_month_date
    :service_contract_month.money_per_month]
   :model-insert
   [:service_contract_month.id_service_contract
    :service_contract_month.service_month_date
    :service_contract_month.money_per_month]
   :insert-button
   true
   :delete-button
   true
   :actions
   []
   :buttons
   []
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
     :enterpreneur.contacts_information]}))
