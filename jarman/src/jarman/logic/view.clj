(ns jarman.logic.view
  (:refer-clojure :exclude [update])
  (:require
   ;; Clojure toolkit 
   [clojure.data :as data]
   [clojure.string :as string]
   [seesaw.util :as u]
   ;; Seesaw components
   [seesaw.core :as c]
   [seesaw.border :as sborder]
   [seesaw.dev :as sdev]
   [seesaw.mig :as smig]
   [seesaw.swingx :as swingx]
   [seesaw.chooser :as chooser]
   ;; Jarman toolkit
   [jarman.logic.document-manager :as doc]
   [jarman.logic.connection :as db]
   [jarman.config.config-manager :as cm]
   [jarman.tools.lang :refer :all :as lang]
   [jarman.gui.gui-tools :refer :all :as gtool]
   [jarman.resource-lib.icon-library :as ico]
   [jarman.tools.swing :as stool]
   [jarman.gui.gui-components :refer :all :as gcomp]
   [jarman.gui.gui-calendar :as calendar]
   [jarman.config.storage :as storage]
   [jarman.config.environment :as env]
   [jarman.logic.sql-tool :as toolbox :include-macros true :refer :all]
   [jarman.logic.metadata :as mt])
  (:import (java.util Date)
           (java.text SimpleDateFormat)))


(defn- tf-t-f [table-field]
  (let [t-f (string/split (name table-field) #"\.")]
    (mapv keyword t-f)))

(defn- t-f-tf [table field]
  (keyword (str (name table) "." (name (name field)))))

;;; CONSTRUCTORS ;;;
(defn metadata-toolkit-constructor [configuration toolkit-map]
  (if-let [table-metadata (first (mt/getset! (:table-name configuration)))]
    {:table-meta   ((comp :table :prop) table-metadata)
     :columns-meta ((comp :columns :prop) table-metadata)}))

(defn sql-crud-toolkit-constructor [configuration toolkit-map]
  (let [m (first (mt/getset! (:table-name configuration)))
        ;; relations (recur-find-path m)
        id_column (t-f-tf (:table-name configuration) :id)
        table-name ((comp :field :table :prop) m)
        columns (map :field ((comp :columns :prop) m))
        update-expression (fn [entity] (if (id_column entity)  (update table-name :set entity :where (=-v id_column (id_column entity)))))
        insert-expression (fn [entity] (if (nil? (id_column entity)) (insert table-name :set entity)))
        delete-expression (fn [entity] (if (id_column entity) (delete table-name :where (=-v id_column (id_column entity)))))
        select-expression (fn [& {:as args}]
                            (apply (partial select-builder (:table-name configuration))
                                   (mapcat vec (into (:query configuration) args))))]
    {:update-expression update-expression
     :insert-expression insert-expression
     :delete-expression delete-expression
     :select-expression select-expression
     :update (fn [e] (db/exec (update-expression e)))
     :insert (fn [e] (db/exec (insert-expression e)))
     :delete (fn [e] (db/exec (delete-expression e)))
     :select (fn [ ] (db/query (select-expression)))
     :model-id id_column}))

(defn export-toolkit-constructor [configuration toolkit-map]
  (if-let [select-expression (:select-expression toolkit-map)]
    {:export-select-expression (fn [] (select-expression :column nil :inner-join nil :where nil))
     :export-select (fn [] (db/query (select-expression :column nil :inner-join nil :where nil))) }))

(defn document-toolkit-constructor [configuration toolkit-map]
  (let [table-name (:table-name configuration)]
    (doc/select-documents-by-table table-name)))

(defn data-toolkit-pipeline [configuration]
  (let [rule-react-on (fn [f & ks] (fn [m] (if (every? (fn [k] (some? (k configuration))) ks) (into m (f configuration m)) m)))
        sql-crud-toolkit (rule-react-on sql-crud-toolkit-constructor :query)
        metadata-toolkit (rule-react-on metadata-toolkit-constructor :table-name)
        export-sql-toolkit (rule-react-on export-toolkit-constructor :query)
        document-toolkit (rule-react-on document-toolkit-constructor :table-name)]
    (-> {} sql-crud-toolkit metadata-toolkit export-sql-toolkit document-toolkit)))

(defmacro defview [table-model-name & body]
  (let [configurations
        (reduce into 
                (for [form body]
                  (if (sequential? form)
                    `{~(keyword (first form)) (hash-map ~@(rest form) :table-name ~(keyword table-model-name))})))]
    `(do ~@(for [form body :let [f (first form)]]
             `(~f
               (get ~configurations ~(keyword f))
                 ;; (data-toolkit-pipeline ~(get configurations (keyword f)))
               (data-toolkit-pipeline (get ~configurations ~(keyword f)))))
         nil)))

;;; helpers ;;; 

(defn as-is [& column-list]
  (map #(if (keyword? %) {% %} %) column-list))

;;; gui declarations ;;;

(defview permission
  (jarman-table
   :name "Table"
   :place nil
   :tables [:permission]
   :view   [:permission.permission_name]
   :query   {:column (as-is :permission.id :permission.permission_name :permission.configuration)}))

(defview documents
  (jarman-table
   :name nil
   :place nil
   :tables [:documents]
   :view   [:documents.table :documents.name :documents.prop]
   :query  {:column (as-is :documents.id :documents.table :documents.name :documents.prop)}))

(defview user
  (jarman-table
   :name nil
   :place nil
   :tables [:user :permission]
   :view   [:user.first_name :user.last_name :user.login :permission.permission_name]
   :query  {:inner-join [:permission]
            :column (as-is :user.id :user.login :user.password :user.first_name :user.last_name :permission.permission_name :permission.configuration :user.id_permission)}))

(defview enterpreneur
  (jarman-table
   :name nil
   :place nil
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
   :query  {:column (as-is
                     :enterpreneur.id
                     :enterpreneur.ssreou
                     :enterpreneur.ownership_form
                     :enterpreneur.vat_certificate
                     :enterpreneur.individual_tax_number
                     :enterpreneur.director
                     :enterpreneur.accountant
                     :enterpreneur.legal_address
                     :enterpreneur.physical_address
                     :enterpreneur.contacts_information)}))



(defview point_of_sale
  (jarman-table
   :name nil
   :place nil
   :tables [:point_of_sale :enterpreneur]
   :view   [:point_of_sale.name :point_of_sale.physical_address :point_of_sale.telefons
            :enterpreneur.ssreou :enterpreneur.ownership_form]
   :query  {:inner-join [:enterpreneur]
            :column (as-is :point_of_sale.id :point_of_sale.name
                           :point_of_sale.physical_address :point_of_sale.telefons :enterpreneur.id
                           :enterpreneur.ssreou :enterpreneur.ownership_form)}))

(defview cache_register
  (jarman-table
   :name nil
   :place nil
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
   :query {:inner-join [:point_of_sale]
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
                    :cache_register.id_point_of_sale)}))

(defview point_of_sale_group
  (jarman-table
   :name nil
   :place nil
   :tables [:point_of_sale_group]
   :view [:point_of_sale_group.group_name :point_of_sale_group.information]
   :query {:column (as-is :point_of_sale_group.id :point_of_sale_group.group_name :point_of_sale_group.information)}))

(defview point_of_sale_group_links
  (jarman-table
   :name nil
   :place nil
   :tables [:point_of_sale_group_links
            :point_of_sale_group
            :point_of_sale]
   :view [:point_of_sale.name
          :point_of_sale.physical_address
          :point_of_sale_group.group_name
          :point_of_sale_group.information]
   :query {:inner-join [:point_of_sale :point_of_sale_group]
           :column (as-is
                    :point_of_sale_group_links.id
                    :point_of_sale_group_links.id_point_of_sale_group
                    :point_of_sale_group_links.id_point_of_sale
                    :point_of_sale.name
                    :point_of_sale.physical_address
                    :point_of_sale_group.group_name
                    :point_of_sale_group.information)}))

(defview seal
  (jarman-table
   :name nil
   :place nil
   :tables [:seal]
   :view [:seal.seal_number
          :seal.to_date]
   :query {:column (as-is :seal.id :seal.seal_number :seal.to_date)}))

(defview service_contract
  (jarman-table
   :name nil
   :place nil
   :tables [:service_contract :point_of_sale]
   :view [:service_contract.register_contract_date
          :service_contract.contract_term_date
          :service_contract.money_per_month
          :point_of_sale.name
          :point_of_sale.physical_address]
   :query {:inner-join [:point_of_sale]
           :column (as-is
                    :service_contract.id
                    :service_contract.id_point_of_sale
                    :service_contract.register_contract_date
                    :service_contract.contract_term_date
                    :service_contract.money_per_month
                    :point_of_sale.name
                    :point_of_sale.physical_address)}))

(defview repair_contract
  (jarman-table
   :name nil
   :place nil
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
   :query{:inner-join [:point_of_sale :cache_register]
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
                   :repair_contract.active_seal)}))
