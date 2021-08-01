(ns jarman.managment.data-managment
  (:require
   [clojure.data :as data]
   [clojure.java.jdbc :as jdbc]   
   [clojure.string :as string]
   [jarman.logic.sql-tool :as sql]
   [jarman.logic.metadata :as metadata]
   [jarman.config.storage :as storage]
   [jarman.config.environment :as env]
   [jarman.logic.structural-initializer :as sinit]
   [jarman.tools.lang :refer :all]
   [jarman.logic.connection :as db])
  (:import (java.util Date)
           (java.text SimpleDateFormat)))


(def all-tables [{:id nil,
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
                 {:id nil,
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
                 {:id nil,
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
                 {:id nil,
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
                 {:id nil,
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
                 {:id nil,
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
                     :component-type [:date :datetime :text],
                     :representation "manufacture_date",
                     :field-qualified :cache_register.manufacture_date}
                    {:description nil,
                     :private? false,
                     :default-value nil,
                     :editable? true,
                     :field :first_registration_date,
                     :column-type [:date :default :null],
                     :component-type [:date :datetime :text],
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
                 {:id nil,
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
                 {:id nil,
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
                 {:id nil,
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
                     :component-type [:datetime :date :text],
                     :representation "datetime_of_use",
                     :field-qualified :seal.datetime_of_use}
                    {:description nil,
                     :private? false,
                     :default-value nil,
                     :editable? true,
                     :field :datetime_of_remove,
                     :column-type [:datetime :default :null],
                     :component-type [:datetime :date :text],
                     :representation "datetime_of_remove",
                     :field-qualified :seal.datetime_of_remove}]}}
                 {:id nil,
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
                 {:id nil,
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
                 {:id nil,
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
                 {:id nil,
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
                     :component-type [:date :datetime :text],
                     :representation "repair_date",
                     :field-qualified :repair_contract.repair_date}
                    {:description nil,
                     :private? false,
                     :default-value nil,
                     :editable? true,
                     :field :cache_register_register_date,
                     :column-type [:date :default :null],
                     :component-type [:date :datetime :text],
                     :representation "cache_register_register_date",
                     :field-qualified :repair_contract.cache_register_register_date}]}}
                 {:id nil,
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
                     :component-type [:date],
                     :representation "Contract start term",
                     :field-qualified :service_contract.contract_start_term}
                    {:description nil,
                     :private? false,
                     :default-value nil,
                     :editable? true,
                     :field :contract_end_term,
                     :column-type [:date :default :null],
                     :component-type [:date],
                     :representation "Contract end term",
                     :field-qualified :service_contract.contract_end_term}]}}
                 {:id nil,
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
                     :component-type [:date],
                     :representation "Service month start",
                     :field-qualified :service_contract_month.service_month_start}
                    {:description nil,
                     :private? false,
                     :default-value nil,
                     :editable? true,
                     :field :service_month_end,
                     :column-type [:date :default :null],
                     :component-type [:date],
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

;;;;;;;;;;;;;;;;
;;; METADATA ;;;
;;;;;;;;;;;;;;;;

(defn metadata-get-tables
  [metadata-v] (map (fn [scheme] (:table_name scheme)) metadata-v))

;;;;;;;;;;;;;;;;
;;; DATABASE ;;;
;;;;;;;;;;;;;;;;


;;; if you want create metadata from current database scheme
;;; - into `metadata` table use `database-recreate-metadata-to-db` func
;;; - into local file `f` use `database-recreate-metadata-to-file` func

(defn database-recreate-metadata-to-db
  [] (metadata/do-create-meta-database))

(defn database-recreate-metadata-to-file
  "Descriptions
    Create metadata maps and put it to file
  Example 
    (database-recreate-metadata-to-file \"/home/serhii/dupa.edn\")"
  ([f] (spit f (with-out-str (clojure.pprint/pprint (metadata/do-create-meta-snapshot))))))


;;; clearing metadata mean deleting all raws
;;; in `METADATA` table in database

(defn database-clear-metadata
  ([] (metadata/do-clear-meta)))

;;; Function test all system tabeles, which declarated in `*system-tables`
;;; Under 'testing' mean next algrithm:
;;; 
;;; if table not exists
;;;    => create it, and fill values if need
;;;    => all columns alright?
;;;       => fill data
;;;       => throw exception that is not compatible with jarman-client database
;;; 
;;; See
;;;  `jarman.logic.structural-initializer/*system-tables*`

(defn database-verify-system-tables []
  (sinit/procedure-test-all))

;;; information functions

(defn- database-list-all-tables []
  (mapv (comp second first) (db/query (sql/show-tables))))

;;; scheme up/down functionality

(defn database-create-scheme [metadata-v]
  ;; (sinit/procedure-test-all)
  (doall (for [m metadata-v]
           (do ;; (metadata/update-meta metadata)
               (db/exec (metadata/create-table-by-meta m))))))

(defn database-delete-business-scheme [metadata-v]
  (doall
  ;; (delete business logic table)
  (for [table (reverse (metadata-get-tables metadata-v))]
    (db/exec
     (sql/drop-table table)))))

(defn database-delete-scheme [metadata-v]
  (doall
   ;; (delete business logic table)
   (for [table (reverse (metadata-get-tables metadata-v))]
     (db/exec
      (sql/drop-table table))))
  (doall
   ;; (delete rest system tables)
   (for [table (mapv (comp second first) (db/query (sql/show-tables)))]
     (db/exec
      (sql/drop-table table)))))

;;; make insert or update metadata into database,
;;; decision about inserting or deleting depends
;;; have one metadata table `:id` on 'nil or not
;;; if nil - make insert, and oposite

(defn metadata-persist-into-database [metadata-v]
  (doall
   (for [m metadata-v]
     (metadata/create-one-meta-force m (:table_name m)))))

(comment
  (database-recreate-metadata-to-db)
  (database-recreate-metadata-to-file "some.edn")
  (metadata-persist-into-database all-tables)
  (metadata-get-tables all-tables)
  (database-verify-system-tables)
  (database-clear-metadata)
  (database-delete-business-scheme all-tables)
  (database-delete-scheme all-tables)
  (database-create-scheme all-tables))


