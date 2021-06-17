 (ns jarman.managment.db-managment
  (:refer-clojure :exclude [update])
  (:require
   [clojure.data :as data]
   [clojure.java.jdbc :as jdbc]   
   [clojure.string :as string]
   [jarman.logic.connection :as db]
   [jarman.logic.sql-tool :as toolbox :include-macros true :refer :all]
   [jarman.logic.metadata :as metadata]
   [jarman.config.storage :as storage]
   [jarman.config.environment :as env]
   [jarman.logic.structural-initializer :as sinit]
   [jarman.tools.lang :refer :all])
  (:import (java.util Date)
           (java.text SimpleDateFormat)))

(def all-tables nil)
(def db-connection nil)
(def table-key nil)
;; "e:\\repo\\jarman-test\\jarman\\jarman\\src\\jarman\\managment\\db.clj"

;;;;;;;;;;;;;;;;;;;;;;;;
;;; HELPER FUNCTIONS ;;;
;;;;;;;;;;;;;;;;;;;;;;;;
(defn get-options-cli [cli-opt-map]
  (first (keys (:options cli-opt-map))))

(defn get-value-cli [cli-opt-map]
  (first (vals (:options cli-opt-map))))

(defn get-path-cli [cli-opt-map]
  (second (vals (:options cli-opt-map))))

(defn get-tables
  "Description
    - get path to file with db (db_meta or db_ssql)
    - read and eval this file, so write data to variables
    (def all-tables, def db-conecction, def table-key)"
  [path]
  (try
    (binding [*ns* (find-ns 'jarman.managment.db-managment)]  
      (load-file path)
      (db/connection-set db-connection))
    (catch java.io.FileNotFoundException e
      (println (str "[e] File not found" (.toString e))))))

(defn get-list-schemas
  "Description
    get list name of tables from db-file (db_meta.clj or db_ssql) "
  [](map (fn [scheme] (table-key scheme)) all-tables))

(defn get-list-tables
  "Description
    get list name of tables from db-jarman"
  [] (map (comp second first)(db/query (show-tables))))

(defn choose-option
  "Description
    this func get option from cli options map,
    - return false if you want add ALL tables
    - return true if you want add one table"
  [scm]
  (if (and (not= "all" scm) (some? scm))
    true false))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; FUNCTIONS FOR VALIDATE ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def file-exists?
  (fn [file-path]
    (do (binding [*ns* (find-ns 'jarman.managment.db-managment)]  
          (.exists (clojure.java.io/file file-path))))))

(defn get-one-scheme
  [table-name]
  (first (filter (fn [x] (= (table-key x)
                            (if (= table-key :table)
                              table-name
                              (keyword table-name))))
                 all-tables)))

(defn -entity-in? [entity-list]
  (fn [t] (if (nil? (some #(= (string/lower-case t) (string/lower-case %)) entity-list)) false true)))

(def scheme-in?
  "Description
    check if table-scheme is in db"
  (fn [table-name]
    (let [resault (if (nil? (some (fn [x]
                                    (= (table-key x)
                                       (if (= table-key :table)
                                         table-name
                                         (keyword table-name)))) all-tables))
                    false true)] resault)))

(def table-in?
  "Description
    check if table is in DB-jarman"
  (binding [*ns* (find-ns 'jarman.managment.db-managment)]  
    (-entity-in? (get-list-tables))))

(defn valid-tables
  "Description
    check whether all tables were written in db"
  []
  (let [l-schemas (get-list-schemas)
        l-tables (get-list-tables)
        len (count l-schemas)]
    (if (empty? l-schemas) (println "[!] Schemas for db in file (db_meta.clj or db_ssql.clj) is empty"))
    (if (empty? l-tables) (println "[!] DB - jarman is empty"))
    (if (= len (count (for [a l-schemas b l-tables 
                            :when
                            (= (name a) b)]
                        a))) true false)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; FUNCTIONS FOR CREATE TABLES ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn create-scheme []
  (doall (for [t all-tables]
           (db/exec (create-table! t)))))

(defn create-one-table [scm]
  (let [scheme (get-one-scheme scm)]
    (if (empty? scheme) (println "[i] Scheme is not found")
        (do (db/exec (create-table! scheme))))))

(defn create-one-table-by-meta [metadata]
  (try (do (metadata/update-meta metadata)
           (db/exec (metadata/create-table-by-meta metadata)))
       (catch Exception e (println "[!] Problem with " (:table metadata)))))

(defn create-all-by-meta []
  (sinit/procedure-test-all)
  (doall (for [metadata all-tables]
           (create-one-table-by-meta metadata))))

(defn messege-table [ch scm]
  (if (valid-tables)
        (if ch
          (println (format "[i] Table %s created successfuly" (name scm)))
          (println "[i] Table structure created successfuly"))
          (println "[!] Table structure is not valid, check this")))

(defn cli-create-table
  "Description
    this func get cli options map and
    choose functions for create tables (create by ssql,
    create by meta, or create just metadata
    -> :create-by-meta -> create table from db_meta,clj
    -> :create-ssql -> create table from db_ssql.clj
    -> :create-meta -> create meta to table metadata"
  [cli-opt-m]
  (let [opt (get-options-cli cli-opt-m)
        scm (get-value-cli cli-opt-m)
        path (get-path-cli cli-opt-m)
        ch (choose-option scm)]
    (if (nil? path)
      (println "[!] You don't entered path to file with db")
      (get-tables path))
    (condp = opt
      :create-by-meta (if ch
                        (if (scheme-in? scm)
                          (do (sinit/procedure-test-all)
                              (create-one-table-by-meta (get-one-scheme scm)))
                          (println "[i] Scheme for table not found"))
                        (create-all-by-meta))
      :create-by-ssql (if ch
                        (if (scheme-in? scm)
                          (create-one-table scm)
                          (println "[!] Scheme for table not found"))
                        (create-scheme))
      :create-meta (if ch
                     (if (table-in? scm)
                       (do (metadata/create-one-meta scm)
                           (metadata/do-create-references)
                           (println (format "[i] Meta for %s created successfuly" (name scm))))
                       (println "[!] Table not found"))
                      (do (metadata/do-create-meta)
                           (metadata/do-create-references)
                           (println "[!] Metadata created successfuly")))
      (println "[!] You entered invalid operation (key)"))
    (if-not (= opt :create-meta)
      (messege-table ch scm))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; FUNCTIONS FOR DELETE TABLES ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn delete-scheme []
  (doall (for [t (reverse all-tables)]
           (db/exec (drop-table (:table-name t)))))
  (println "[i] Tables deleted successfuly"))

(defn delete-one-table [scm]
  (try
    (db/exec (drop-table (keyword scm)))
    (println (format "[i] Table %s deleted successfuly" (name scm)))
    (catch Exception e (println "Cannot delete or update a parent row: a foreign key constraint fails"))))

(defn cli-delete-table
  "Description
    this func get cli options map and
    choose functions for delete tables"
  [cli-opt-m]
  (let [opt (get-options-cli cli-opt-m)
        scm (get-value-cli cli-opt-m)
        ch (choose-option scm)]
    (condp = opt
      :delete-table (if ch
                        (delete-one-table scm)
                        (delete-scheme))
      :delete-meta (if ch
                     (do (metadata/delete-one-meta scm)
                         (println (format "[i] Meta for %s deleted successfuly" (name scm))))
                      (do (metadata/do-clear-meta)
                           (println "[!] Metadata deleted successfuly")))
      (println "[!] You entered invalid operation (key)"))))

;;;;;;;;;;;;;;;
;;; SCRIPTS ;;;
;;;;;;;;;;;;;;;
(defn valid-all-tables
  [cli-opt-m]
  (let [opt (get-options-cli cli-opt-m)
        path (get-path-cli cli-opt-m)]
    (if (nil? path)
      (println "[!] You don't entered path to file with db")
      (get-tables path))
    (if (valid-tables)
      (println "[i] All structure of tables is good")
      (println "[!] Problem with table structure, check-this"))))

(defn reset-one-db [scm]
  (do (delete-one-table scm)
      (create-one-table scm)
      (metadata/delete-one-meta scm)
      (metadata/create-one-meta scm)
      (println (format "[i] Table %s was reset successufuly" (name scm)))))

(defn reset-all-db []
  (do (sinit/procedure-test-all)
      (delete-scheme)
      (println "[i] DB was deleted")
      (create-scheme)
      (println "[i] Tables from scheme created")
      (metadata/do-create-meta)
      (println "[i] Created metadata")
      (metadata/do-create-references)
      (println "[i] Created references")
      (println "[i] DB was reset successufuly")))

(defn reset-db [cli-opt-m]
  (let [opt (get-options-cli cli-opt-m)
        scm (get-value-cli cli-opt-m)
        path (get-path-cli cli-opt-m)]
    (if (nil? path)
      (println "[!] You don't entered path to file with db")
      (do (get-tables path)
          (if (choose-option scm)
            (reset-one-db scm) 
            (reset-all-db))))))

(defn reset-one-meta [scm]
  (do (metadata/delete-one-meta scm)
      (metadata/create-one-meta scm)
      (println (format "[i] Metadata for %s was reset successufuly" (name scm)))))

(defn reset-all-meta []
  (do (metadata/do-clear-meta)
      (metadata/do-create-meta)
      (metadata/do-create-references)
      (println "[i] Metadata was reset")))

(defn reset-meta [cli-opt-m]
   (let [scm (get-in cli-opt-m [:options :print] nil)]
    (if (choose-option scm)
      (reset-one-meta scm) 
      (reset-all-meta))))

;;;;;;;;;;;;
;;; VIEW ;;;
;;;;;;;;;;;;
(defn view-scheme
  "Description
    show all structure of tables of db file (db_meta.clj or db_ssql.clj)"
  [cli-opt-m]
  (let [path (get-path-cli cli-opt-m)
        scm (get-value-cli cli-opt-m)
        ch (choose-option scm)]
    (if (nil? path)
      (println "[!] You don't entered path to file with db")
      (get-tables path))
    (if ch
      (if (scheme-in? scm)
        (println (pp-str (get-one-scheme scm)))
        (println "[!] Scheme not found in db"))
      (println (if-not (nil? all-tables)
                 (pp-str all-tables))))))

(defn print-helpr
  "Description
    show info about keys"
  [cli-opt-m]
  (println (get cli-opt-m :summary "[!] Helper not implemented")))

(defn print-list-tbls
  "Description
    show all tables from db jarman"
  [cli-opt-m]
  (println (format "Available tables:\n\n%s" (string/join ", \n" (map (comp second first seq)
                                                                    (db/query (show-tables)))))))

(defn print-list-schm
  "Description
    show all schemes from db file (db_meta.clj or db_ssql.clj)"
  [cli-opt-m]
  (let [path (get-path-cli cli-opt-m)
        scm (get-value-cli cli-opt-m)]
    (if (nil? path)
      (println "[!] You don't entered path to file with db")
      (do
        (get-tables path)
         (println (format "Available schemas:\n\n%s" (string/join ",\n " (get-list-schemas))))))) )

(defn print-table [cli-opt-m]
  (let [table (get-value-cli cli-opt-m)]
    (let [reslt (db/query (select table))]
      (if (empty? reslt)
        (println "[i] Table not contain data")
        (if (nil? (get-in cli-opt-m [:options :csv-like] nil))
          (map println reslt)
          (do (println (string/join "," (map name (keys (first reslt)))))
              (for [row (map vals reslt)]
                (println (string/join "," row)))))))))


;;  {:dbtype "mysql",
;;   :host  "trashpanda-team.ddns.net",
;;   :port 3307,
;;   :dbname "jarman-test",
;;   :user "root",
;;   :password "1234"})









