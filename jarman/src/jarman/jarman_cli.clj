(ns jarman.jarman-cli
  (:gen-class)
  (:refer-clojure :exclude [update])
  (:require
   [clojure.tools.cli :refer [parse-opts]]
   [clojure.string :as string]
   [clojure.java.jdbc :as jdbc]
   
   [jarman.tools.ftp-toolbox.clj :as ftp]
   [jarman.logic.sql-tool :as toolbox :include-macros true :refer :all]
   [jarman.tools.dev-tools :as dt]))

;;;-DOC-;;;
;; To run this tool using next cli notation
;; $ lein lets-scheme -h
;; $ lein run -m jarman.schema-builder -h
;; $ java -jar target/uberjar/lets-scheme -h

;; (def ^:dynamic sql-connection {:dbtype "mysql"
;;                                :host "127.0.0.1"
;;                                :port 3306
;;                                :dbname "jarman"
;;                                :user "root"
;;                                :password "1234"})

(def ^:dynamic sql-connection {:dbtype "mysql"
                               :host "192.168.1.69"
                               :port 3306
                               :dbname "jarman"
                               :user "jarman"
                               :password "dupa"})

(def available-scheme ["service_contract"
                       "seal"
                       "repair_contract"
                       "point_of_sale_group_links"
                       "point_of_sale_group"
                       "cache_register"
                       "point_of_sale"
                       "enterpreneur"
                       "user"
                       "permission"
                       "METADATA"])

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
                :columns [{:id_point_of_sale [:bigint-20 :unsigned :default :null]}
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
(def meta-in?   (-entity-in? (map :table (jdbc/query sql-connection (select :METADATA :column ["`table`"])))))
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


;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Database manager  ;;; 
;;;;;;;;;;;;;;;;;;;;;;;;;

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


(def data-cli-options
  [["-c" "--create SCHEME" "Create table from scheme, use <all> for all. Automatic generate meta for structure(--create-meta)"
    :parse-fn #(str %)
    :validate [#(or (= % "all") (scheme-in? %)) "Scheme not found"]]
   ["-d" "--delete TABLE" "Delete table by name, use <all> for all"
    :parse-fn #(str %)
    :validate [#(or (= % "all") (table-in? %)) "Table not found"]]
   ;; todo
   [nil "--create-meta TABLE" "Delete table by name, use <all> for all"
    :parse-fn #(str %)
    :validate [#(or (= % "all") (table-in? %)) "Table not found"]]
   ;; todo
   [nil "--delete-meta TABLE" "Delete table by name, use <all> for all"
    :parse-fn #(str %)
    :validate [#(or (= % "all") (table-in? %)) "Table not found"]]
   ;; todo
   [nil "--reset-meta TABLE" "Delete and create meta information about table"
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

(defn data-cli [& args]
  (let [cli-opt (parse-opts args data-cli-options)
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
        (= k1 :view-scheme)  (cli-scheme-view  cli-opt)
        (= k1 :dummy-data)   (println "[!] Excuse me, functionality not implemented")
        :else (print-helpr cli-opt)))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Project structure manger ;;; 
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def structure-cli-options
  [[nil "--refresh-icons" "Regenerate icon assets library"]
   [nil "--refresh-fonts" "Regenerate font assets library"]
   ["-h" "--help"]])
(defn structure-cli [& args]
  (let [cli-opt (parse-opts args structure-cli-options)
        opts (get cli-opt :options)
        args (get cli-opt :arguments)
        o1 (first (seq opts))
        k1 (first o1) v1 (second o1)]
    (if-let [es (get cli-opt :errors)]
      (doall (for [e es] (println (format "[!] %s" e))))
      (cond 
        (= k1 :refresh-icons)(do(dt/refresh-icon-lib)(println(format "[ok] library by path %s was generated" dt/*icon-library*)))
        (= k1 :refresh-fonts)(do(dt/refresh-font-lib)(println(format "[ok] library by path %s was generated" dt/*font-library*)))
        (= k1 :help)(print-helpr structure-cli-options)
        :else (print-helpr cli-opt)))))


;;;;;;;;;;;;;;;;;;;
;;; FTP Manager ;;; 
;;;;;;;;;;;;;;;;;;;

(defn cli-ftp-deploy-package [cli-opt]
  (if-let [target-package (get-in cli-opt [:options :deploy] nil)]
    (do (ftp/ftp-put-file (ftp/construct-ftp-url) target-package)
        (println (format "Succesfuly uplaod file %s to repository" target-package)))))
(defn cli-ftp-deploy-all []
  (apply (partial ftp/ftp-put-file (ftp/construct-ftp-url))
         (filter (every-pred ftp/zip?) (ftp/ftp-list-files (ftp/construct-ftp-url))))
  (println (format "Succesuly uplaod files")))
(defn cli-ftp-list-packages-remote []
  (let [packages (filter (every-pred ftp/zip?) (ftp/ftp-list-files (ftp/construct-ftp-url)))]
    (if (empty? packages)
      (println "Repository is empty")
      (map println packages))))
(defn cli-ftp-list-files-remote []
  (let [files (ftp/ftp-list-files (ftp/construct-ftp-url))]
    (if (empty? files)
      (println "Repository is empty")
      (map println files))))
(defn cli-ftp-list-packages-local  []
  (let [packages (map #(.getName %) (ftp/list-available-packages))]
    (if (empty? packages)
      (println "Repository is empty")
      (map println packages))))


;; (-main "ftp" "--list-packages-server")
;; (-main "ftp" "--list-files-server")
;; (-main "ftp" "--list-packages-target")
;; (-main "ftp" "--deploy" "target/hrtime-1.0.4.zip")
;; (-main "ftp" "--deploy-all-packages")



(def ftp-cli-options
  [["-d" "--deploy PACKAGE" "Upload builded package to the repository host"
    :parse-fn #(str %)
    :validate [#(.exists (clojure.java.io/file %)) "Package not found"]]
   ["-a" "--deploy-all-packages" "Upload all builded packages to repository"]
   ["-l" "--list-packages-server" "list server packages"]
   [nil  "--list-files-server" "list files in repository"]
   ["-t" "--list-packages-target" "list local packages"]])
(defn ftp-cli [& args]
  (let [cli-opt (parse-opts args ftp-cli-options)
        opts (get cli-opt :options)
        args (get cli-opt :arguments)
        o1 (first (seq opts))
        k1 (first o1) v1 (second o1)]
    (if-let [es (get cli-opt :errors)]
      (doall (for [e es] (println (format "[!] %s" e))))
      (cond 
        (= k1 :deploy)               (cli-ftp-deploy-package cli-opt)
        (= k1 :deploy-all-packages)  (cli-ftp-deploy-all)
        (= k1 :list-packages-server) (cli-ftp-list-packages-remote)
        (= k1 :list-files-server)    (cli-ftp-list-files-remote)
        (= k1 :list-packages-target) (cli-ftp-list-packages-local)
        (= k1 :help)                 (print-helpr ftp-cli-options)
        :else (print-helpr cli-opt)))))


(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn usage []
  (->> ["lets-scheme - jarman CLI tool, which do controlling jarman environment easyest ."
        ""
        "Usage: lets-scheme [action]"
        ""
        "Actions:"
        "  data       Database and data manager. Use for building or debuging project data structure"
        "  config     -- not yet implemented --"
        "  structure  Manager of project structure and resources"
        "  ftp        Ftp tools "
        "  help       Documentation"
        ""
        "Please refer to the manual page for more information."]
       (string/join \newline)))

(defn -main [& args]
  (if (empty? args)
    (println (usage))
    (let [[action & rest-arguments] args]
      (if action
        (case action
          "data"     (apply data-cli rest-arguments)
          "structure"(apply structure-cli rest-arguments)
          "config"   (exit 0 "[!] config action not yet implemented ")
          "ftp"      (apply ftp-cli rest-arguments)
          "help"     (println (usage))
          (println (usage)))))))

