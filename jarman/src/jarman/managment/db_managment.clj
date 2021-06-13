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

(defn get-tables []
  (try
    (binding [*ns* (find-ns 'jarman.managment.db-managment)]  
    (load-file (storage/db-managment-dir-path)))
    (catch java.io.FileNotFoundException e
      (println (str "[e] File not found" (.toString e)))))
  all-tables)



(defn create-scheme []
  (for [t (get-tables)]
    (db/exec (create-table! t))))

(defn delete-scheme []
  (for [t (reverse (get-tables))]
    (db/exec (drop-table (:table-name t)))))

(defn get-one-scheme [table-name]
  (first (filter (fn [x] (= (:table-name x) (keyword table-name))) (get-tables))))

(defn -entity-in? [entity-list]
  (fn [t] (if (nil? (some #(= (string/lower-case t) (string/lower-case %)) entity-list)) false true)))

(def scheme-in? (fn [table-name]
                  (let [resault (if (nil? (some (fn [x] (= (:table-name x) (keyword table-name))) (get-tables)))
                                  false true)]
                    resault)))

;;(def meta-in?   (-entity-in? (map :table (jdbc/query sql-connection (select :METADATA :column ["`table`"])))))
(def table-in?  (-entity-in? (let [entity-list (db/query (show-tables))]
                               (if (not-empty entity-list) (map (comp second first) entity-list)))))

(defn create-one-table [scm]
  (let [scheme (get-one-scheme scm)]
    (println scheme)
    (println scm)
    (if (empty? scheme) (println "[i] Scheme is not found")
        (do
          (println "heyy")
          (db/exec (create-table! scheme))
          (println (format "[i] Table by scheme %s created successfuly" (name scm)))))))

(defn delete-one-table [scm]
  (db/exec (drop-table (keyword scm))))

(defn choose-option [scm]
  (if (and (not= "all" scm) (some? scm))
    true false))

(defn cli-create-table [cli-opt-m]
  (let [opt (first (keys (:options cli-opt-m)))
        scm (first (vals (:options cli-opt-m)))]
    (if (choose-option scm)
      (if (= opt :create-meta)
        (do
          (metadata/create-one-meta scm)
          (metadata/do-create-references)
          (println (format "[i] Meta for %s created successfuly" (name scm))))
        (create-one-table scm))
      (if (= opt :create-meta)
        (do (metadata/do-create-meta)
            (metadata/do-create-references)
            (println "[!] Metadata created successfuly"))
        (do (create-scheme)
          (println "[!] Table structure created successfuly"))))))

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

(reset-all-db)

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
  (println (get-tables)))

;;(create-one-table "user")
;;(delete-one-table "user")

(get-tables)

(db/query (show-tables))






