(ns jarman.cli.cli-tool
  (:refer-clojure :exclude [update])
  (:require
   ;; resource 
   [clojure.tools.cli :refer [parse-opts]]
   [clojure.string :as string]
   [clojure.java.jdbc :as jdbc]
   ;; logic
   [jarman.logic.metadata :as metadata]
   [jarman.logic.sql-tool :as toolbox :include-macros true :refer :all]
   [jarman.managment.db-managment :refer :all]
   ;; developer tools
   [jarman.logic.structural-initializer :as sinit]
   [jarman.logic.connection :as db]
   [jarman.tools.swing :as stool]   
   [jarman.tools.lang :refer :all]))


;;;;;;;;;;;;;;;;;;;;;;;;
;;; HELPER FUNCTIONS ;;;
;;;;;;;;;;;;;;;;;;;;;;;;
(defn get-options-cli [cli-opt-map]
  (first (keys (:options cli-opt-map))))

(defn get-value-cli [cli-opt-map]
  (first (vals (:options cli-opt-map))))

(defn get-path-cli [cli-opt-map]
  (second (vals (:options cli-opt-map))))

(defn choose-option
  "Description
    this func get option from cli options map,
    - return false if you want add ALL tables
    - return true if you want add one table"
  [scm]
  (if (and (not= "all" scm) (some? scm))
    true false))

(defn messege-table [ch scm]
  (if (valid-tables)
        (if ch
          (println (format "[i] Table %s created successfuly" (name scm)))
          (println "[i] Table structure created successfuly"))
        (println "[!] Table structure is not valid, check this")))

;;;;;;;;;;;;;;;;;;;;;;;;;
;;; CREATE and DELETE ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;
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






