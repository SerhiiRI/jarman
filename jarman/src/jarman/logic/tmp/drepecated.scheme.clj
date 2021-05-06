(defview permission
  :tables [:permission]
  :view   [:permission.permission_name]
  :data   {:column (as-is :permission.id :permission.permission_name :permission.configuration)})

(defview user
  :tables [:user :permission]
  :view   [:user.first_name :user.last_name :user.login :permission.permission_name]
  :data   {:inner-join [:permission]
           :column (as-is :user.id :user.login :user.password :user.first_name :user.last_name :permission.permission_name :permission.configuration :user.id_permission)})

(defview enterpreneur
  :tables [:enterpreneur]
  :view   [:enterpreneur.ssreou
           :enterpreneur.ownership_form
           :enterpreneur.vat_certificate
           :enterpreneur.individual_tax_number
           :enterpreneur.director
           :enterpreneur.accountant
           :enterpreneur.legal_address
           :enterpreneur.physical_address
           :enterpreneur.contacts_information]
  :data   {:column (as-is
                    :enterpreneur.id
                    :enterpreneur.ssreou
                    :enterpreneur.ownership_form 
                    :enterpreneur.vat_certificate
                    :enterpreneur.individual_tax_number
                    :enterpreneur.director
                    :enterpreneur.accountant
                    :enterpreneur.legal_address
                    :enterpreneur.physical_address
                    :enterpreneur.contacts_information)})

(defview point_of_sale
  :tables [:point_of_sale :enterpreneur]
  :view   [:point_of_sale.name :point_of_sale.physical_address :point_of_sale.telefons
           :enterpreneur.ssreou :enterpreneur.ownership_form]
  :data   {:inner-join [:enterpreneur]
           :column (as-is :point_of_sale.id :point_of_sale.name :point_of_sale.physical_address :point_of_sale.telefons :enterpreneur.id :enterpreneur.ssreou :enterpreneur.ownership_form)})

(defview cache_register
  :tables [:cache_register :point_of_sale]
  :view [:cache_register.is_working
         :cache_register.modem_serial_number
         :cache_register.modem_phone_number
         :cache_register.producer
         :cache_register.first_registration_date
         :cache_register.modem_model
         :cache_register.name
         :cache_register.fiscal_number
         :cache_register.dev_id
         :cache_register.manufacture_date
         :cache_register.modem
         :cache_register.version
         :cache_register.serial_number]
  :data {:inner-join [:point_of_sale]
         :column (as-is
                  :cache_register.id
                  :cache_register.is_working
                  :cache_register.modem_serial_number
                  :cache_register.modem_phone_number
                  :cache_register.producer
                  :cache_register.first_registration_date
                  :cache_register.modem_model
                  :cache_register.name
                  :cache_register.fiscal_number
                  :cache_register.dev_id
                  :cache_register.manufacture_date
                  :cache_register.modem
                  :cache_register.version
                  :cache_register.serial_number
                  :cache_register.id_point_of_sale)})

(defview point_of_sale_group
  :tables [:point_of_sale_group]
  :view [:point_of_sale_group.group_name :point_of_sale_group.information]
  :data {:column (as-is :point_of_sale_group.id :point_of_sale_group.group_name :point_of_sale_group.information)})

(defview point_of_sale_group_links
  :tables [:point_of_sale_group_links
           :point_of_sale_group
           :point_of_sale]
  :view [:point_of_sale.name
         :point_of_sale.physical_address
         :point_of_sale_group.group_name
         :point_of_sale_group.information]
  :data {:inner-join [:point_of_sale :point_of_sale_group]
         :column (as-is
                   :point_of_sale_group_links.id
                   :point_of_sale_group_links.id_point_of_sale_group
                   :point_of_sale_group_links.id_point_of_sale
                   :point_of_sale.name
                   :point_of_sale.physical_address
                   :point_of_sale_group.group_name
                   :point_of_sale_group.information)})

(defview seal
  :tables [:seal]
  :view [:seal.seal_number
         :seal.to_date]
  :data {:column (as-is :seal.id :seal.seal_number :seal.to_date)})

(defview service_contract
  :tables [:service_contract :point_of_sale]
  :view [:service_contract.register_contract_date
         :service_contract.contract_term_date
         :service_contract.money_per_month
         :point_of_sale.name
         :point_of_sale.physical_address]
  :data {:inner-join [:point_of_sale]
         :column (as-is
                  :service_contract.id
                  :service_contract.id_point_of_sale
                  :service_contract.register_contract_date
                  :service_contract.contract_term_date
                  :service_contract.money_per_month
                  :point_of_sale.name
                  :point_of_sale.physical_address)})

(defview repair_contract
  :tables [:repair_contract :cache_register :point_of_sale]
  :view [:cache_register.modem_serial_number
                  :cache_register.modem_phone_number
                  :cache_register.producer
                  :point_of_sale.name
                  :point_of_sale.physical_address
                  :repair_contract.creation_contract_date
                  :repair_contract.last_change_contract_date
                  :repair_contract.contract_terms_date
                  :repair_contract.cache_register_register_date
                  :repair_contract.remove_security_seal_date
                  :repair_contract.cause_of_removing_seal
                  :repair_contract.technical_problem
                  :repair_contract.active_seal]
  :data {:inner-join [:point_of_sale :cache_register]
         :column (as-is
                  :cache_register.modem_serial_number
                  :cache_register.modem_phone_number
                  :cache_register.producer
                  :point_of_sale.name
                  :point_of_sale.physical_address
                  :repair_contract.id
                  :repair_contract.id_cache_register
                  :repair_contract.id_point_of_sale
                  :repair_contract.creation_contract_date
                  :repair_contract.last_change_contract_date
                  :repair_contract.contract_terms_date
                  :repair_contract.cache_register_register_date
                  :repair_contract.remove_security_seal_date
                  :repair_contract.cause_of_removing_seal
                  :repair_contract.technical_problem
                  :repair_contract.active_seal)})

