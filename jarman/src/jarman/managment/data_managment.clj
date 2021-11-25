(ns jarman.managment.data-managment
  (:require
   ;; -----------
   [clojure.data      :as data]
   [clojure.java.jdbc :as jdbc]   
   [clojure.string    :as string]
   [clojure.pprint    :refer [pprint cl-format]]
   ;; -----------
   [jarman.tools.lang            :refer :all]
   [jarman.config.storage        :as storage]
   [jarman.config.environment    :as env]
   [jarman.logic.sql-tool        :as sql]
   [jarman.logic.metadata        :as metadata]
   [jarman.logic.connection      :as db]
   [jarman.logic.view-manager    :as view-manager]
   [jarman.logic.structural-initializer :as sinit])
  (:import (java.util Date)
           (java.text SimpleDateFormat)))

;;;;;;;;;;;;;;;
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

;; (map (fn [[l v]]
;;        [(clojure.string/upper-case (str l)) v])
;;      (sort-by first (seq (group-by first (database-list-all-tables)))))

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
     (metadata/create-one-meta m (:table_name m)))))

(defn metadata-info [metadata-v]
  (let [f-offset #(cl-format nil "~,,v<~A~>" %1 %2)]
    (println "Metadata structure")
    (doall
     (for [{{tabl      :table
             cols      :columns
             comp-cols :columns-composite} :prop} metadata-v]
       (do
         (println (f-offset 3 (str (keyword (:field tabl)))))
         (println (f-offset 3 (format "#+TITLE: \"%s\"" (:representation tabl))))
         (println (f-offset 3 "#+COLUMS:"))
         (doall
          (for [{field :field
                 repr :representation
                 comp-type :component-type} cols]
            (println (format "%-35s %s" (f-offset 6 (str (keyword field))) (str comp-type)))
            ))
         (when (seq comp-cols)
           (println (f-offset 3 "#+COMPOSITE-COLUMNS:"))
           (doall
            (for [{field  :field,
                   constr :constructor
                   cols   :columns} comp-cols]
              (do (println (format "%-35s %s" (f-offset 6 (str (keyword field))) (str constr)))
                  (println (f-offset 6 "#+COLUMS:"))
                  (doall
                   (for [{field :field
                          comp-type :component-type} cols]
                     (println (format "%-35s %s" (f-offset 9 (str (keyword field))) (str comp-type))))))))))))nil))

(defn database-info []
  (let [table-off "   "
      chr-off "  "
      structurized-data
      (->> (database-list-all-tables)
           (group-by first)
           (sort-by first)
           (map (fn [[l v]] (vector (clojure.string/upper-case (str l)) v))))]
    (println "Database tables")
    (doall (for [[chr tbls] structurized-data]
             (do (println (format "%s%s" chr-off chr))
                 (doall (for [tbl tbls]
                          (println (format "%s%s" table-off
                                           (if (in? sinit/system-tables tbl)
                                             (str tbl " [system]")
                                             tbl)))))))) nil))

(defn view-info [view-list]
  (println "Views:")
  (doall
   (for [table-view view-list]
     (let [table-map  (coll-to-map (drop 1 (first (filter #(sequential? %) table-view))))
           view-plugins (map (comp str first) (drop 2 table-view))
           table-name (:name table-map)]
       (println (cl-format nil "~70<  ~A~;~{~A~^, ~}~>" table-name view-plugins))))) true)

(defn views-persist-into-database [view-list]
  (let []
    (assert
     (every? #(= 'defview (first %)) view-list)
     "Everything(omiting connection map and `in-ns`) in view.clj MUST be only `defview`")
    (view-manager/view-clean)
    (doall
     (for [table-view view-list]
          (let [table-map  (coll-to-map (drop 1 (first (filter #(sequential? %) table-view))))
                table-name (:name table-map)]
            (view-manager/view-set {:table_name table-name :view (str table-view)}))))))

(comment
  (views-persist-into-database)
  (database-info)
  (metadata-info all-tables)
  (database-recreate-metadata-to-db)
  (database-recreate-metadata-to-file "some.edn")
  (metadata-persist-into-database all-tables)
  (metadata-get-tables all-tables)
  (database-verify-system-tables)
  (database-clear-metadata)
  (database-delete-business-scheme all-tables)
  (database-delete-scheme all-tables)
  (database-create-scheme all-tables))


