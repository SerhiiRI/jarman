(def all-tables
  [{:table-name :documents
    :columns [{:table [:varchar-100 :default :null]}
              {:name [:varchar-200 :default :null]}
              {:document [:blob :default :null]}
              {:prop [:text :nnull :default "\"{}\""]}]}
   {:table-name :view
    :columns [{:table-name [:varchar-100 :default :null]}
              {:view [:text :nnull :default "\"{}\""]}]}
   {:table-name :metadata
    :columns [{:table [:varchar-100 :default :null]}
              {:prop [:text :default :null]}]}
   {:table-name :permission
    :columns [{:permission_name [:varchar-20 :default :null]}
              {:configuration [:tinytext :nnull :default "\"{}\""]}]}
   {:table-name :user
    :columns [{:login [:varchar-100 :nnull]}
              {:password [:varchar-100 :nnull]}
              {:first_name [:varchar-100 :nnull]}
              {:last_name [:varchar-100 :nnull]}
              {:id_permission [:bigint-20-unsigned :nnull]}]
    :foreign-keys [{:id_permission :permission} {:delete :cascade :update :cascade}]}
   {:table-name :enterpreneur
    :columns [{:ssreou [:tinytext :nnull]}
              {:ownership_form [:varchar-100 :default :null]}
              {:vat_certificate [:tinytext :default :null]}
              {:individual_tax_number [:varchar-100 :default :null]}
              {:director [:varchar-100 :default :null]}
              {:accountant [:varchar-100 :default :null]}
              {:legal_address [:varchar-100 :default :null]}
              {:physical_address [:varchar-100 :default :null]}
              {:contacts_information [:mediumtext :default :null]}]}
   {:table-name :point_of_sale
    :columns [{:id_enterpreneur [:bigint-20-unsigned :default :null]}
              {:name [:varchar-100 :default :null]}
              {:physical_address  [:varchar-100 :default :null]}
              {:telefons  [:varchar-100 :default :null]}]
    :foreign-keys [{:id_enterpreneur :enterpreneur} {:update :cascade}]}
   {:table-name :cache_register
    :columns [{:id_point_of_sale [:bigint-20 :unsigned :default :null]}
              {:name [:varchar-100 :default :null]}
              {:serial_number [:varchar-100 :default :null]}
              {:fiscal_number [:varchar-100 :default :null]}
              {:manufacture_date [:date :default :null]}
              {:first_registration_date [:date :default :null]}
              {:is_working [:tinyint-1 :default :null]}
              {:version [:varchar-100 :default :null]}
              {:dev_id [:varchar-100 :default :null]}
              {:producer [:varchar-100 :default :null]}
              {:modem [:varchar-100 :default :null]}
              {:modem_model [:varchar-100 :default :null]}
              {:modem_serial_number [:varchar-100 :default :null]}
              {:modem_phone_number [:varchar-100 :default :null]}]
    :foreign-keys [{:id_point_of_sale :point_of_sale} {:delete :cascade :update :cascade}]}
   {:table-name :point_of_sale_group
    :columns [{:group_name [:varchar-100 :default :null]}
              {:information [:mediumtext :default :null]}]}
   {:table-name :point_of_sale_group_links
    :columns [{:id_point_of_sale_group [:bigint-20-unsigned :default :null]}
              {:id_point_of_sale [:bigint-20-unsigned :default :null]}]
    :foreign-keys [[{:id_point_of_sale_group :point_of_sale_group} {:delete :cascade :update :cascade}]
                   [{:id_point_of_sale :point_of_sale}]]}
   {:table-name :seal
    :columns [{:seal_number [:varchar-100 :default :null]}
              {:datetime_of_use [:datetime :default :null]}
              {:datetime_of_remove [:datetime :default :null]}]}
   {:table-name :service_contract
    :columns [{:id_enterpreneur     [:bigint-20 :unsigned :default :null]}
              {:contract_start_term [:date :default :null]}
              {:contract_end_term   [:date :default :null]}
              {:money_per_month     [:float-2 :nnull :default 0]}]
    :foreign-keys [{:id_enterpreneur :enterpreneur} {:delete :cascade :update :cascade}]}
   {:table-name :service_contract_month
    :columns [{:id_service_contract [:bigint-20 :unsigned :default :null]}
              {:service_month_date  [:date :default :null]}
              {:money_per_month     [:float-2 :nnull :default 0]}]
    :foreign-keys [{:id_service_contract :service_contract} {:delete :cascade :update :cascade}]}
   {:table-name
    :repair_reasons
    :columns [{:description [:varchar-255 :default :null]}]}
   {:table-name
    :repair_technical_issue
    :columns [{:description [:varchar-255 :default :null]}]}
   {:table-name
    :repair_nature_of_problem
    :columns [{:description [:varchar-255 :default :null]}]}
   {:table-name :repair_contract
    :columns [{:id_cache_register[:bigint-20 :unsigned :default :null]}
              {:id_old_seal      [:bigint-20 :unsigned :default :null]}
              {:id_new_seal      [:bigint-20 :unsigned :default :null]}
              {:id_repair_reasons[:bigint-20 :unsigned :default :null]}
              {:id_repair_technical_issue[:bigint-20 :unsigned :default :null]}
              {:id_repair_nature_of_problem[:bigint-20 :unsigned :default :null]}
              {:repair_date    [:date :default :null]}
              {:cache_register_register_date [:date :default :null]}]
    :foreign-keys [[{:id_cache_register :cache_register} {:delete :cascade :update :cascade}]
                   [{:id_old_seal :seal} {:delete :null :update :null}]
                   [{:id_new_seal :seal} {:delete :null :update :null}]
                   [{:id_repair_reasons :repair_reasons}
                    {:delete :null :update :null}]
                   [{:id_repair_technical_issue :repair_technical_issue}
                    {:delete :null :update :null}]
                   [{:id_repair_nature_of_problem :repair_nature_of_problem}
                    {:delete :null :update :null}]]}])




