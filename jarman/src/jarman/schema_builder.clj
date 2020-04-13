(ns jarman.schema-builder
  (:gen-class)
  (:refer-clojure :exclude [update])
  (:require
   [jarman.sql-tool :as toolbox :include-macros true :refer :all]
   [clojure.tools.cli :refer [parse-opts]]
   [clojure.string :as string]
   [clojure.java.jdbc :as jdbc]))

;;; DOC ;;;
;; To run this tool using next cli notation
;; $ lein lets-scheme -h
;; $ lein run -m jarman.schema-builder -h
;; $ java -jar target/uberjar/lets-scheme -h

(def ^:dynamic sql-connection {:dbtype "mysql" :host "127.0.0.1" :port 3306 :dbname "ekka-test" :user "root" :password "123"})
(def available-scheme ["service_contract" "seal" "repair_contract" "point_of_sale_group_links" "point_of_sale_group" "cache_register" "point_of_sale" "enterpreneur" "user" "permission" "METADATA"])


(def METADATA
  (create-table :METADATA
                :columns [{:table [:varchar-100 :default :null]}
                          {:prop [:text :default :null]}]))

(def permission
  (create-table :permission
                :columns [{:permission_name [:varchar-20 :default :null]}
                          {:configuration [:tinytext :nnull :default "'{}'"]}]))

(def user
  (create-table :user
                :columns [{:login [:varchar-100 :nnull]}
                          {:password [:varchar-100 :nnull]}
                          {:first_name [:varchar-100 :nnull]}
                          {:last_name [:varchar-100 :nnull]}
                          {:id_permission [:bigint-120-unsigned :nnull]}]
                :foreign-keys [{:id_permission :permission} {:delete :cascade :update :cascade}]))

(def enterpreneur
  (create-table :enterpreneur
                :columns [{:ssreou [:tinytext :nnull]}
                          {:ownership_form [:varchar-100 :default :null]}
                          {:vat_certificate [:tinytext :default :null]}
                          {:individual_tax_number [:varchar-100 :default :null]}
                          {:director [:varchar-100 :default :null]}
                          {:accountant [:varchar-100 :default :null]}
                          {:legal_address [:varchar-100 :default :null]}
                          {:physical_address [:varchar-100 :default :null]}
                          {:contacts_information [:mediumtext :default :null]}]))

(def point_of_sale
  (create-table :point_of_sale
                :columns [{:id_enterpreneur [:bigint-20-unsigned :default :null]}
                          {:name [:varchar-100 :default :null]}
                          {:physical_address  [:varchar-100 :default :null]}
                          {:telefons  [:varchar-100 :default :null]}]
                :foreign-keys [{:id_enterpreneur :enterpreneur} {:update :cascade}]))

(def cache_register
  (create-table :cache_register
                :columns [ {:id_point_of_sale [:bigint-20 :unsigned :default :null]}
                          {:name [:varchar-100 :default :null]}
                          {:serial_number [:varchar-100 :default :null]}
                          {:fiscal_number [:varchar-100 :default :null]}
                          {:manufacture_date [:date :default :null]}
                          {:first_registration_date [:date :default :null]}
                          {:is_working [:tinyint-1 :default :null]}
                          {:version [:varchar-100 :default :null]}
                          {:id_dev [:varchar-100 :default :null]}
                          {:producer [:varchar-100 :default :null]}
                          {:modem [:varchar-100 :default :null]}
                          {:modem_model [:varchar-100 :default :null]}
                          {:modem_serial_number [:varchar-100 :default :null]}
                          {:modem_phone_number [:varchar-100 :default :null]}]
                :foreign-keys [{:id_point_of_sale :point_of_sale} {:delete :cascade :update :cascade}]))

(def point_of_sale_group
  (create-table :point_of_sale_group
                :columns [{:group_name [:varchar-100 :default :null]}
                          {:metadata [:mediumtext :default :null]}]))

(def point_of_sale_group_links
  (create-table :point_of_sale_group_links
                :columns [{:id_point_of_sale_group [:bigint-20-unsigned :default :null]}
                          {:id_point_of_sale [:bigint-20-unsigned :default :null]}]
                :foreign-keys [[{:id_point_of_sale_group :point_of_sale_group} {:delete :cascade :update :cascade}]
                               [{:id_point_of_sale :point_of_sale}]]))

(def seal
  (create-table :seal
                :columns [{:seal_number [:varchar-100 :default :null]}
                          {:to_date [:date :default :null]}]))

(def service_contract
  (create-table :service_contract
                :columns [{:id_point_of_sale [:bigint-20 :unsigned :default :null]}
                          {:register_contract_date [:date :default :null]}
                          {:contract_term_date [:date :default :null]}
                          {:money_per_month [:int-11 :default :null]}]
                :foreign-keys [{:id_point_of_sale :point_of_sale} {:delete :cascade :update :cascade}]))

(def repair_contract
  (create-table :repair_contract
                :columns [{:id_cache_register [:bigint-20 :unsigned :default :null]}
                          {:id_point_of_sale [:bigint-20 :unsigned :default :null]}
                          {:creation_contract_date [:date :default :null]}
                          {:last_change_contract_date [:date :default :null]}
                          {:contract_terms_date [:date :default :null]}
                          {:cache_register_register_date [:date :default :null]}
                          {:remove_security_seal_date [:datetime :default :null]}
                          {:cause_of_removing_seal [:mediumtext :default :null]}
                          {:technical_problem [:mediumtext :default :null]}
                          {:active_seal [:mediumtext :default :null]}]
                :foreign-keys [[{:id_cache_register :cache_register} {:delete :cascade :update :cascade}]
                               [{:id_point_of_sale :point_of_sale} {:delete :cascade :update :cascade}]]))


(defmacro create-tabels [& tables]
  `(do ~@(for [t tables]
           `(jdbc/execute! sql-connection ~t))))
(defmacro delete-tabels [& tables]
  `(do ~@(for [t tables]
           `(jdbc/execute! sql-connection (drop-table (keyword '~t))))))

(defn create-scheme-one [scheme]
  (eval `(jdbc/execute! sql-connection ~(symbol (string/join "/" ["jarman.schema-builder" (symbol scheme)])))))
(defn create-scheme []
  (create-tabels METADATA
                 permission
                 user
                 enterpreneur
                 point_of_sale
                 cache_register
                 point_of_sale_group
                 point_of_sale_group_links
                 repair_contract
                 seal
                 service_contract))

(defn delete-scheme-one [scheme]
  (eval `(jdbc/execute! sql-connection (drop-table ~(keyword scheme)))))
(defn delete-scheme []
  (delete-tabels service_contract
                 seal
                 repair_contract
                 point_of_sale_group_links
                 point_of_sale_group
                 cache_register
                 point_of_sale
                 enterpreneur
                 user
                 permission
                 METADATA))

(defn regenerate-scheme []
  (delete-scheme)
  (create-scheme))

(defn fill-random-scheme []
  (create-scheme))

(defn -entity-in? [entity-list]
  (fn [t] (some #(= (string/lower-case t) (string/lower-case %)) entity-list)))
(def scheme-in? (-entity-in? available-scheme))
(def table-in?  (-entity-in? (let [entity-list (jdbc/query sql-connection "SHOW TABLES")]
                               (if (not-empty entity-list) (map (comp second first) entity-list)))))

(defn print-table [cli-opt-m]
  (if-let [table (get-in cli-opt-m [:options :print] nil)]
    (let [reslt (jdbc/query sql-connection (select table))]
      (if (empty? reslt)
        (println "[i] Table not contain data")
        (if (nil? (get-in cli-opt-m [:options :csv-like] nil))
          (map println reslt)
          (do (println (string/join "," (map name (keys (first reslt)))))
              (for [row (map vals reslt)]
                (println (string/join "," row)))))))
    (println "[i] Maybe table is empty. Maybe the problem locate between chair and monitor.")))

(defn print-helpr [cli-opt-m]
  (println (get cli-opt-m :summary "[!] Helper not implemented")))

(defn print-ltbls [cli-opt-m]
  (println (format "Available tables:\n\t%s" (string/join ", " (map (comp second first seq) (jdbc/query sql-connection "SHOW TABLES"))))))

(defn print-lschm [cli-opt-m]
  (println (format "Available schemes:\n\t%s" (string/join ", " available-scheme))))

(defn cli-create-table [cli-opt-m]
  (let [scm (get-in cli-opt-m [:options :create] nil)]
    (if (and (not= "all" scm) (some? scm))
      (do (create-scheme-one scm)
          (println (format "[i] Table by scheme %s created successfuly" (name scm))))
      (do (create-scheme)
          (println "[!] Table structure created successfuly")))))

(defn cli-delete-table [cli-opt-m]
  (let [scm (get-in cli-opt-m [:options :delete] nil)]
    (if (and (not= "all" scm) (some? scm))
      (do (delete-scheme-one scm)
          (println (format "[i] Table %s deleted successufuly" (name scm))))
      (do (delete-scheme)
          (println "[i] Whole DB scheme was erased")))))

(defn cli-scheme-view [cli-opt-m]
  (if-let [scheme (get-in cli-opt-m [:options :view-scheme] nil)]
    (println (eval `~(symbol (string/join "/" ["jarman.schema-builder" (name scheme)]))))
    ;; (println (eval `~(symbol scheme)))
    (println "[!] (cli-scheme-view): internal error" )))


(def cli-options
  [["-c" "--create SCHEME" "Create table from scheme, use <all> for all"
    :parse-fn #(str %)
    :validate [#(or (= % "all") (scheme-in? %)) "Scheme not found"]]
   ["-d" "--delete TABLE" "Delete table by name, use <all> for all"
    :parse-fn #(str %)
    :validate [#(or (= % "all") (table-in? %)) "Table not found"]]
   [nil "--dummy-data TABLE" "Generate dummy data for table, use <all> for all"
    :parse-fn #(str %)
    :validate [#(or (= % "all") (table-in? %)) "Table not found"]]
   [nil "--dummy-size SIZE" "Dummy data size"]
   [nil "--view-scheme SCHEME"
    :parse-fn #(str %)
    :validate [#(scheme-in? %) "Scheme not found"]]
   [nil  "--list-schemas" "List available table schemas"]
   ["-l" "--list-tables" "List available table"]
   ["-p" "--print TABLE" "Print table"
    :parse-fn #(str %)
    :validate [#(table-in? %) "Table not found"]]
   [nil "--csv-like" "combine with --print key"]
   ["-h" "--help"]])

;; Quick debug
;; (-main "-p" "user" "--csv-like")
;; (-main "-d")
;; (-main "--list-schemas")
;; (-main "--list-tables")
;; (-main "--dummy-data")
;; (-main "--view-scheme" "user")
;; (-main "-h")
;; (-main "-d" "*")
;; (-main "-d" "user")
;; (-main "-c" "*")
;; (-main "-c" "METADATA")
;; (-main "-d" "METADATA")

(defn -main [& args]
  (let [cli-opt (parse-opts args cli-options)
        opts (get cli-opt :options)
        args (get cli-opt :arguments)
        o1 (first (seq opts))
        k1 (first o1) v1 (second o1)]
    (if-let [es (get cli-opt :errors)]
      (doall (for [e es] (println (format "[!] %s" e))))
      (cond
        (= k1 :print)        (print-table cli-opt)
        (= k1 :help)         (print-helpr cli-opt)
        (= k1 :list-schemas) (print-lschm cli-opt)
        (= k1 :list-tables)  (print-ltbls cli-opt)
        (= k1 :create)       (cli-create-table cli-opt)
        (= k1 :delete)       (cli-delete-table cli-opt)
        (= k1 :view-scheme)  (cli-scheme-view cli-opt)
        (= k1 :dummy-data)   (println "[!] Excuse me, functionality not implemented")
        :else (print-helpr cli-opt)))))

