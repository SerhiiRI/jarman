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

(defn path-to-db [cli-opt-map]
  (def path-to-db-clj (get-value-cli cli-opt-map))
  (println "you add path successfully"))

(defn get-tables [path]
  (try
    (binding [*ns* (find-ns 'jarman.managment.db-managment)]  
      (load-file path)
      (db/connection-set db-connection))
    (catch java.io.FileNotFoundException e
      (println (str "[e] File not found" (.toString e))))))

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
    (do 
        (binding [*ns* (find-ns 'jarman.managment.db-managment)]  
          (.exists (clojure.java.io/file file-path))))))

(defn get-one-scheme [table-name]
  (first (filter (fn [x] (= (:table-name x) (keyword table-name)))
                 all-tables)))

(defn get-one-meta [table-name]
  (first (filter (fn [x] (= (:table x) table-name))
                 all-tables)))

(defn -entity-in? [entity-list]
  (fn [t] (if (nil? (some #(= (string/lower-case t) (string/lower-case %)) entity-list)) false true)))

(def scheme-in? (fn [table-name]
                  (let [resault
                        (if (nil? (some
                                   (fn [x] (= (:table-name x)
                                              (keyword table-name)))
                                  all-tables))
                                  false true)] resault)))

;; (def meta-in?   (-entity-in?
;;                  (map :table (db/query sql-connection (select :METADATA :column ["`table`"])))))

(def table-in?  (binding [*ns* (find-ns 'jarman.managment.db-managment)]  
                  (-entity-in? (let [entity-list (db/query (show-tables))]
                               (println (map (comp second first) entity-list) )
                               (if (not-empty entity-list) (map (comp second first) entity-list)))) ))


;;(db/query (select :metadata :column [:table]))

(table-in? "user")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; FUNCTIONS FOR CREATE TABLES ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn create-scheme []
  (if-not (empty?
           (for [t all-tables]
             (db/exec (create-table! t))))
    (println "[!] Table structure created successfuly")))

(defn create-one-table [scm]
  (let [scheme (get-one-scheme scm)]
    (if (empty? scheme) (println "[i] Scheme is not found")
        (do
          (db/exec (create-table! scheme))
          (println (format "[i] Table by scheme %s created successfuly" (name scm)))))))

(defn write-meta-to-db []
  (sinit/procedure-test-all)
  (for [metadata all-tables]
    (metadata/update-meta metadata)))

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
    (get-tables path)
    (condp = opt
      :create-by-meta (do
                        (write-meta-to-db)
                        (if ch
                           (metadata/create-table-by-meta (get-one-meta scm))
                           (for [metadata all-tables]
                             (metadata/create-table-by-meta metadata))))
      :create-by-ssql (if ch
                        (if (scheme-in? scm)
                          (create-one-table scm)
                          (println "[!] Scheme for table not found"))
                        (create-scheme))
      :create-meta (if ch
                     (do
                       (metadata/create-one-meta scm)
                       (metadata/do-create-references)
                       (println (format "[i] Meta for %s created successfuly" (name scm))))
                     (do (metadata/do-create-meta)
                         (metadata/do-create-references)
                         (println "[!] Metadata created successfuly")))
      (println "[!] You entered invalid operation (key)"))))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; FUNCTIONS FOR DELETE TABLES ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn delete-scheme []
  (for [t (reverse all-tables)]
    (db/exec (drop-table (:table-name t)))))

(defn delete-one-table [scm]
  (db/exec (drop-table (keyword scm))))

(defn cli-delete-table [cli-opt-m]
  (let [opt (first (keys (:options cli-opt-m)))
        scm (first (vals (:options cli-opt-m)))]
    (if (choose-option scm)
      (if (= opt :delete-meta)
        (do (metadata/delete-one-meta scm)
            (println (format "[i] Meta  %s cleared successfuly" (name scm))))
        (do (delete-one-table scm)
            (println (format "[i] Table %s deleted successufuly" (name scm)))))
      (if (= opt :delete-meta)
        (do (metadata/do-clear-meta)
            (println "[!] Metadata was clear"))
        (do (delete-scheme)
            (println "[i] Whole DB scheme was erased"))))))

(defn reset-one-db [scm]
  (do (delete-one-table scm)
      (create-one-table scm)
      (metadata/delete-one-meta scm)
      (metadata/create-one-meta scm)
      (println (format "[i] Table %s was reset successufuly" (name scm)))))

(defn reset-all-db []
  (do (sinit/procedure-test-all)
      (delete-scheme)
      (println "hey1")
      (create-scheme)
      (println "hey2")
      (metadata/do-create-meta)
      (println "hey3")
      (metadata/do-create-references)
      (println "[i] DB was reset")))

(defn reset-db [cli-opt-m]
  (let [scm (get-in cli-opt-m [:options :print] nil)]
    (if (choose-option scm)
      (reset-one-db scm) 
      (reset-all-db))))

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

(defn cli-scheme-view [cli-opt-m]
  (let [scheme (get-in cli-opt-m [:options :view-scheme] nil)
        data (get-one-scheme scheme)]
    (if (empty? data)
      (println "[!] (cli-scheme-view): internal error" )
      (do
        (println (string/join "/" ["jarman.schema-builder" (name scheme)]))
        (println (pp-str data))))))

(defn view-tables []
  (println all-tables))

;;(create-one-table "user")
;;(delete-one-table "user")



(db/connection-get)

(db/query (show-tables))






