;; Description:
;;   Module allow to create
;;   Redis-like table manipulation
;;   with quick and easy to
;;   understand interface.
;;
;; Example: 
;; (getset :user)
;;     get list of values
;; (getset :user :Adam)
;;     get value by key "Adam"
;; (getset :user :Adam 24) 
;;     set value "24" by key "Adam"

(ns jarman.key-value-tool
  (:require
   [clojure.string :as string]
   [jarman.sql-tool :as toolbox :include-macros true :refer :all]
   [clojure.java.jdbc :as jdbc]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; configuration variable ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def ^:dynamic *table-val-type*   "default data type for key-value-table column 'value'"   [:text :default :null])
(def ^:dynamic *table-key-type*   "default data type for key-value-table column 'key'"     [:varchar-100 :nnull])
(def sql-connection {:dbtype   "mysql"
                     :host     "127.0.0.1"
                     :port     3306
                     :dbname   "ekka-test"
                     :user     "root"
                     :password "123"})

(defn- table-exist? [table-name]
  {:pre [(keyword? table-name)]}
  (some #(= (second (first (seq %))) (name table-name)) (jdbc/query sql-connection "SHOW TABLES")))

(defn create
  "Description:
     Create key-value table(it literally contain only `key` and `val` column).
     Type of key column declareted by `*table-key-type*` dynamic variable, so
     `*table-val-type*` define type for val column
   Example:
     (create :some-table-name)" 
  [table-name]{:pre [(keyword? table-name)]}
  (let [key-type (if (or (not (seqable? *table-key-type*)) (string? *table-key-type*))
                 [*table-key-type*] *table-key-type*)
      val-type (if (or (not (seqable? *table-val-type*)) (string? *table-val-type*))
                 [*table-val-type*] *table-val-type*)]
    (jdbc/execute! sql-connection
                   (create-table table-name
                                 :table-config {:engine "InnoDB"}
                                 :columns [{:key key-type} {:val val-type}]))))

(defn drop
  "Description:
     Delete table by keyword-type name
   Example:
     (drop :some-table)"
  [table-name] {:pre [(keyword? table-name)]}
  (jdbc/execute! sql-connection (drop-table table-name)))


(defn getset
  "Descriptions
     Is getter and setter function for table.
  Example: 
     (getset :user) ;; get all data from table
     (getset :user :Adam) ;; get value of table `user` by key `Adam`. If key does not exist, or table to, function create table and key with \"null\" value in table. 
     (getset :user :Adam 24) ;; set value `24` for table `user` by key `Adam`. If key does not exit, or table to, function automatic create table and set key value to table."
  ([table-name] {:pre [(keyword? table-name)]}
   (jdbc/query sql-connection (select table-name)))
  ([table-name key] {:pre [(keyword? table-name) (keyword? key)]}
   (if-not (table-exist? table-name) (do (create table-name) (getset table-name key))
     (let [r (jdbc/query sql-connection (select table-name :where (= :key (name key))))]
       (if (empty? r)
         (do (jdbc/execute! sql-connection (insert table-name :values [{:id nil :key (name key) :val nil}])) nil)
         (:val (first r))))))
  ([table-name key value] {:pre [(keyword? table-name) (keyword? key)]}
   (if-not (table-exist? table-name) (do (create table-name) (getset table-name key value))
     (let [r (jdbc/query sql-connection (select table-name :where (= :key (name key))))]
       (if (empty? r)
         (do (jdbc/execute! sql-connection (insert table-name :values [{:id nil :key (name key) :val value}])) value)
         (do (jdbc/execute! sql-connection (update table-name :set {:val value} :where (= :key (name key)))) value))))))

