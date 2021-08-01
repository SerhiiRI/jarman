;; Description
;;   In this file realized helper functions for our cli-toolkit.
;;   Most functions get argument cli-opt-map with configurations.
;; Example
;;   cli-opt-map
;;     {:options
;;       {:create-table user,
;;        :path e:\repo\jarman-test\jarman\jarman\src\jarman\managment\db_meta.clj}
;;        :arguments [],
;;        :summary   " --create-meta TABLE   Create meta to table metadata by name, use <all> for all
;;                     -d, --delete-table TABLE  Delete table by name, use <all> for all
;;                     -p, --path PATH   Add path to file db, use this key with --create-... alse use this key for view
;;                     -h, --help"
;;        :errors  nil}}
;; Location
;;   -- top -- private, helper functions (like (get-value-cli ...)
;;   for parsing cli-optional-map)
;;   -- center-top -- functions for create and delete tables
;;   -- center-bottom -- scripts (reset db, reset metadata)
;;   -- bottom -- functions for printing data

(ns jarman.cli.cli-tool
  (:refer-clojure :exclude [update])
  (:require
   ;; resource 
   [clojure.tools.cli :refer [parse-opts]]
   [clojure.string :as string]
   [clojure.java.jdbc :as jdbc]
   ;; logic
   [jarman.logic.metadata :as metadata]
   [jarman.logic.sql-tool :refer :all]
   [jarman.managment.db-managment :refer :all]
   ;; developer tools
   [jarman.logic.structural-initializer :as sinit]
   [jarman.logic.connection :as db]
   [jarman.tools.swing :as stool]   
   [jarman.tools.lang :refer :all]))

;;;;;;;;;;;;;;;;;;;;;;;;
;;; HELPER FUNCTIONS ;;;
;;;;;;;;;;;;;;;;;;;;;;;;
(defn- get-options-cli [cli-opt-map]
  (first (keys (:options cli-opt-map))))

(defn- get-value-cli [cli-opt-map]
  (first (vals (:options cli-opt-map))))

(defn- get-path-cli [cli-opt-map]
  (second (vals (:options cli-opt-map))))

(defn- choose-option
  "Description
    this func get option from cli options map,
    - return false if you want add ALL tables
    - return true if you want add one table"
  [scm]
  (if (and (not= "all" scm) (some? scm))
    true false))

(defn- messege-table [ch scm]
  (if (valid-tables)
        (if ch
          (println (format "[i] Table %s created successfuly" (name scm)))
          (println "[i] Table structure created successfuly"))
        (println "[!] Table structure is not valid, check this")))

(defn swap-tables [cli-opt-m]
  (let [path (get-path-cli cli-opt-m)]
    (if (nil? path)
      (println "[!] You don't entered path to file with db")
      (do (get-tables path)
          (db/connection-set db-connection)))))

;;;;;;;;;;;;;;;;;;;;;;;;;
;;; CREATE and DELETE ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;
(defmulti create-table-multi (fn [params]
                               (if (= "all" (.toUpperCase (get-value-cli params)))
                                 (get-options-cli params)
                                 (str (name (get-options-cli params) "-"
                                            (get-value-cli params))))))

(defmethod create-table-multi "create-table" [params]
  (if (table-in-file-db? (get-value-cli params))
    (create-one-table (get-value-cli params))
    (println "[i] Scheme for table not found")))

(defmethod create-table-multi "create-table-all" [params]
  (create-all-tables))

(defmethod create-table-multi "create-meta" [params]
  (let [tbl (get-value-cli params)]
    (if (table-in-file-db? tbl)
      (do (metadata/create-one-meta (get-table-file-db (get-value-cli tbl)) tbl)
          (println (format "[i] Meta for %s created successfuly" (get-value-cli params))))
      (println "[!] Table not found"))))

(defmethod create-table-multi "create-meta-all" [params]
  (do (metadata/do-create-meta-database)
      (println "[!] Metadata created successfuly")))

(defmethod create-table-multi :default [params]
  (println "[!] Invalid option"))

(defn cli-create-table
  "Description
      this func get cli options map and
      sent to defmulti for choose option
      (create one table or all tables,
       create one table in metadata or all)
    -> :create-table -> create table from db_meta,clj
    -> :create-meta -> create meta to table metadata"
  [cli-opt-map]
  (println cli-opt-map)
  (let [path (get-path-cli cli-opt-map)
        scm (get-value-cli cli-opt-map)]
    (if (nil? path)
      (println "[!] You don't entered path to file with db")
      (do (get-tables path)
          (db/connection-set db-connection)
          (create-table-multi cli-opt-map)
          (if-not (= (get-options-cli cli-opt-map) :create-meta)
            (messege-table (choose-option scm) scm))))))

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
                      (delete-all-tables))
      :delete-meta (if ch
                     (do (metadata/delete-one-meta scm)
                         (println (format "[i] Meta for %s deleted successfuly" (name scm))))
                      (do (metadata/do-clear-meta)
                           (println "[!] Metadata deleted successfuly")))
      (println "[!] You entered invalid operation (key)"))))

;;;;;;;;;;;;;;;
;;; SCRIPTS ;;;
;;;;;;;;;;;;;;;
(defn reset-db [cli-opt-m]
  (let [opt (get-options-cli cli-opt-m)
        scm (get-value-cli cli-opt-m)
        path (get-path-cli cli-opt-m)]
    (if (nil? path)
      (println "[!] You don't entered path to file with db")
      (do (get-tables path)
          (db/connection-set db-connection)
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
(defn view-all-tables
  "Description
    show all structure of tables of db file (db_meta.clj or db_ssql.clj)"
  [cli-opt-m]
  (let [path (get-path-cli cli-opt-m)
        tbl  (get-value-cli cli-opt-m)
        ch   (choose-option tbl)]
    (if (nil? path)
      (println "[!] You don't entered path to file with db")
      (do (get-tables path)
          (db/connection-set db-connection)))
    (if ch
      (if (table-in-file-db? tbl)
        (println (pp-str (get-table-file-db tbl)))
        (println "[!] Table not found in db"))
      (println (if-not (nil? all-tables)
                 (pp-str all-tables))))))

(defn print-helpr
  "Description
    show info about keys"
  [cli-opt-m]
  (println (get cli-opt-m :summary "[!] Helper not implemented")))

(defn print-list-tbls-jarman
  "Description
    show all tables from db jarman"
  [cli-opt-m]
  (println (format "Available tables:\n\n%s" (string/join ", \n" (map (comp second first seq)
                                                                    (db/query (show-tables)))))))

(defn print-list-tbls-file
  "Description
    show all tables from db file (db_meta.clj or db_ssql.clj)"
  [cli-opt-m]
  (let [path (get-path-cli cli-opt-m)
        scm (get-value-cli cli-opt-m)]
    (if (nil? path)
      (println "[!] You don't entered path to file with db")
      (do (get-tables path)
          (db/connection-set db-connection)
          (println (format "Available schemas:\n\n%s" (string/join ",\n " (get-list-tables-file-db))))))) )

(defn print-table [cli-opt-m]
  (let [table (get-value-cli cli-opt-m)]
    (let [reslt (db/query (select! {:table_name table}))]
      (if (empty? reslt)
        (println "[i] Table not contain data")
        (if (nil? (get-in cli-opt-m [:options :csv-like] nil))
          (map println reslt)
          (do (println (string/join "," (map name (keys (first reslt)))))
              (for [row (map vals reslt)]
                (println (string/join "," row)))))))))

(comment (db/connection-set {:dbtype "mysql",
                             :host  "trashpanda-team.ddns.net",
                             :port 3307,
                             :dbname "jarman-test",
                             :user "root",
                             :password "1234"}))







