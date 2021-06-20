(ns jarman.managment.db-managment
  (:refer-clojure :exclude [update])
  (:require
   [clojure.data :as data]
   [clojure.java.jdbc :as jdbc]   
   [clojure.string :as string]
   [jarman.logic.sql-tool :as toolbox :include-macros true :refer :all]
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
(def table-key nil)
;; "e:\\repo\\jarman-test\\jarman\\jarman\\src\\jarman\\managment\\db.clj"

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



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; FUNCTIONS FOR DELETE TABLES ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn delete-scheme []
  (doall (for [t (reverse all-tables)]
           (db/exec (drop-table (:table_name t)))))
  (println "[i] Tables deleted successfuly"))

(defn delete-one-table [scm]
  (try
    (db/exec (drop-table (keyword scm)))
    (println (format "[i] Table %s deleted successfuly" (name scm)))
    (catch Exception e (println "Cannot delete or update a parent row: a foreign key constraint fails"))))


;;;;;;;;;;;;;;;
;;; SCRIPTS ;;;
;;;;;;;;;;;;;;;


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




