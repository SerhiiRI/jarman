;; Description
;;    This file have all recipes for work with DB.
;; Location
;;   -- top -- private, helper functions (like (get-tables ...)
;;   for parsing download data from file db_meta)
;;   -- center-top -- functions for validate data
;;   -- center-bottom -- functions for create and delete tables
;;   -- bottom -- scripts for generate or reset DB-jarman, metadata

(ns jarman.managment.db-managment
  (:refer-clojure :exclude [update])
  (:require
   [clojure.data :as data]
   [clojure.java.jdbc :as jdbc]   
   [clojure.string :as string]
   [jarman.logic.sql-tool :refer :all]
   [jarman.logic.metadata :as metadata]
   [jarman.config.storage :as storage]
   [jarman.config.environment :as env]
   [jarman.logic.structural-initializer :as sinit]
   [jarman.tools.lang :refer :all]
   [jarman.logic.connection :as db])
  (:import (java.util Date)
           (java.text SimpleDateFormat)))

(def all-tables nil)
(def db-connection nil)
(def table-key :table_name)

;;;;;;;;;;;;;;;;;;;;;;;;
;;; HELPER FUNCTIONS ;;;
;;;;;;;;;;;;;;;;;;;;;;;;
(defn get-tables
  "Description
    - get path to file with db (db_meta or db_ssql)
    - read and eval this file, so write data to variables
    (def all-tables, def db-conecction, def table-key)"
  [path]
  (try
    (binding [*ns* (find-ns 'jarman.managment.db-managment)]
      (load-file path))
    (catch java.io.FileNotFoundException e
      (println (str "[e] File not found" (.toString e))))))

(get-tables "src\\jarman\\managment\\db_meta.clj")

(defn get-list-tables-file-db
  "Description
    get list name of tables from db-file (db_meta.clj or db_ssql) "
  [](map (fn [scheme] (table-key scheme)) all-tables))

(defn get-list-tables-jarman-db
  "Description
    get list name of tables from db-jarman"
  [] (map (comp second first)(db/query (show-tables))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; FUNCTIONS FOR VALIDATE ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def file-exists?
  (fn [file-path]
    (do (binding [*ns* (find-ns 'jarman.managment.db-managment)]  
          (.exists (clojure.java.io/file file-path))))))

(defn get-table-file-db
  [table-name]
  (first (filter (fn [x] (= (table-key x) table-name))
                 all-tables)))

(defn -entity-in? [entity-list]
  (fn [t] (if (nil? (some #(= (string/lower-case t) (string/lower-case %)) entity-list)) false true)))

(def table-in-file-db?
  "Description
    check if table-scheme is in db"
  (fn [table-name]
    (let [resault (if (nil? (some (fn [x]
                                    (= (table-key x) table-name)) all-tables))
                    false true)] resault)))

(def table-in-jarman-db?
  "Description
    check if table is in DB-jarman"
  (binding [*ns* (find-ns 'jarman.managment.db-managment)]  
    (-entity-in? (get-list-tables-jarman-db))))

(defn valid-tables
  "Description
    check whether all tables were written in db"
  [] (let [l-schemas (get-list-tables-file-db)
           l-tables (get-list-tables-jarman-db)
           len (count l-schemas)]
       (if (empty? l-schemas) (println "[!] Tables for db in file (db_meta.clj) is empty"))
       (if (empty? l-tables) (println "[!] DB - jarman is empty"))
       (if (= len (count (for [a l-schemas b l-tables 
                               :when
                               (= (name a) b)]
                           a))) true false)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; FUNCTIONS FOR CREATE TABLES ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn create-all-meta
  "Description
    write all metadata to DB"
  [] (doall (for [table (metadata/show-tables-not-meta)]
              (metadata/create-one-meta (get-table-file-db table) table))))

(defn create-one-table[metadata]
  (try (do (metadata/update-meta metadata)
           (db/exec (metadata/create-table-by-meta metadata)))
       (catch Exception e (println "[!] Problem with " (:table_name metadata)))))

(defn create-all-tables []
  ;; (sinit/procedure-test-all)
  (doall (for [metadata all-tables]
           (create-one-table metadata))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; FUNCTIONS FOR DELETE TABLES ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn delete-one-table [tbl]
  (try
    (db/exec (drop-table tbl))
    (println (format "[i] Table %s deleted successfuly" (name tbl)))
    (catch Exception e (println "Cannot delete or update a parent row: a foreign key constraint fails"))))

(defn delete-all-tables []
  (doall (for [t (reverse all-tables)]
           (db/exec (drop-table (:table_name t)))))
  (println "[i] Tables deleted successfuly"))

;;;;;;;;;;;;;;;
;;; SCRIPTS ;;;
;;;;;;;;;;;;;;;
(defn reset-one-db [tbl]
  (do (delete-one-table tbl)
      (sinit/procedure-test-all)
      (create-one-table tbl)
      (metadata/delete-one-meta tbl)
      (metadata/create-one-meta (get-table-file-db tbl) tbl)
      (println (format "[i] Table %s was reset successufuly" (name tbl)))))

 (defn reset-all-db []
   (do (delete-all-tables)
       (println "[i] DB was deleted")
      (sinit/procedure-test-all)
      (println "[i] Created and validated jarman-tables")
      (create-all-tables)
      (println "[i] Tables from scheme created")
      (create-all-meta)
      (println "[i] Created metadata")
      ;; (metadata/do-create-references)
      ;; (println "[i] Created references")
      (if (valid-tables)
        (println "[i] DB was reset successufuly")
        (println "[i] You have problem with table's structure"))))

(defn reset-one-meta [tbl]
  (do (metadata/delete-one-meta tbl)
      (metadata/create-one-meta (get-table-file-db tbl) tbl)
      (println (format "[i] Metadata for %s was reset successufuly" (name tbl)))))

(defn reset-all-meta []
  (do (metadata/do-clear-meta)
      (create-all-meta)
      ;;(metadata/do-create-references)
      (println "[i] Metadata was reset")))

(comment
  (db/connection-set
   {:dbtype "mysql",
    :host  "trashpanda-team.ddns.net",
    :port 3307,
    :dbname "jarman-test",
    :user "root",
    :password "1234"})
  (db/connection-get))














