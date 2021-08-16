(ns jarman.logic.metadata-backup
  (:require
   
   ;; [clojure.data :as data]
   ;; [clojure.string :as string]
   ;; [jarman.config.storage :as storage]
   ;; [jarman.config.environment :as env]
   ;; [jarman.tools.lang :refer :all]
   ;; [jarman.logic.connection :as db]
   ;; [datascript.core :as d]
   ;; [jarman.logic.sql-tool :refer [select! update! insert!
   ;;                                alter-table! create-table! delete!
   ;;                                show-table-columns ssql-type-parser]]
   )
  ;; (:import (java.util Date)
  ;;          (java.text SimpleDateFormat))
  )

;;;;;;;;;;;;;;;;;;;;;;;
;;; METADATA BACKUP ;;;
;;;;;;;;;;;;;;;;;;;;;;;

(def ^:private backup-name "metadata")
(def ^:private backup-file-name (format "%s.edn" backup-name))
;; (defn- backup-keep-10-last-modified
;;   "Remove 10 last modified backup files, when new backups being created"[]
;;   (let [max-bkp 10 l-files (storage/user-metadata-list) c-files (count l-files)]
;;     (if (> c-files max-bkp) 
;;       (doall (map #(-> % .getName storage/user-metadata-delete)
;;                   (take (- c-files max-bkp) 
;;                         (sort-by #(-> % .lastModified Date.)
;;                                  #(.before %1 %2)
;;                                  (map clojure.java.io/file l-files))))))))

(defn make-backup-metadata
  "Description
    Make backup files 'metadata.edn', if file was created
    before, rename and add timestamp to name in format
    YYYY-MM-dd_HHmmss 'metadata_2021-03-22_004353.edn'

  Example
    (make-backup-metadata)
  
  Warning!
    Timestamp related to event when your backup became
    a old, and replace to new. Timestamp not mean Time
    of backup creation

  See
    `backup-keep-10-last-modified` function which delete
      oldest file from all backup snapshots, if number
      of backups will reach 10 files" []
  (blet
   ;; if exist backup file, please make new one
   (if (.exists (clojure.java.io/file (storage/user-metadata-dir) backup-file-name))
     (blet (storage/user-metadata-rename backup-file-name old-backup-file)
           [old-backup-file (format "%s_%s.edn" backup-name (.format (SimpleDateFormat. "YYYY-MM-dd_HHmmss") (Date.)))]))
   ;; Store the metainformation
   (storage/user-metadata-put backup-file-name (str backup-metadata))
   ;; clean-up oldest files 
   ;; (backup-keep-10-last-modified)
   ;;; make a backup
   [metadata-list (vec (getset!))
    tables-list (map :table metadata-list)
    date-format "YYYY-MM-dd HH:mm:ss"
    backup-metadata
    {:info {:date (.format (SimpleDateFormat. date-format) (Date.))
            :program-dir env/user-dir}
     :table (vec (map :table metadata-list))
     :backup metadata-list}]))

(defn- default-backup-loader []
  (if-let [_TMP0 (storage/user-metadata-get backup-file-name)] _TMP0
    (try (slurp (clojure.java.io/file env/user-dir backup-file-name))
         (catch Exception e nil))))

;; (do-clear-meta)
;; (do-create-meta-database)
;; (make-backup-metadata)
;; (restore-backup-metadata)

;; (do-clear-meta)
;; (do-create-meta-database)
;; (getset :user)

;; (update-meta {:id 188, :table "user", :prop {:table {:field "user", :representation "Користувач", :is-system? false, :is-linker? false, :description nil, :allow-modifing? true, :allow-deleting? true, :allow-linking? true}, :columns [{:field :login, :field-qualified :user.login, :representation "login", :description nil, :component-type ["i"], :column-type [:varchar-100 :nnull], :private? false, :editable? true} {:field :password, :field-qualified :user.password, :representation "password", :description nil, :component-type ["i"], :column-type [:varchar-100 :nnull], :private? false, :editable? true} {:field :first_name, :field-qualified :user.first_name, :representation "first_name", :description nil, :component-type ["i"], :column-type [:varchar-100 :nnull], :private? false, :editable? true} {:field :last_name, :field-qualified :user.last_name, :representation "last_name", :description nil, :component-type ["i"], :column-type [:varchar-100 :nnull], :private? false, :editable? true} {:description nil, :private? false, :editable? true, :field :id_permission, :column-type [:bigint-120-unsigned :nnull], :foreign-keys [{:id_permission :permission} {:delete :cascade, :update :cascade}], :component-type ["l"], :representation "id_permission", :field-qualified :user.id_permission, :key-table "permission"}]}})


(defn restore-backup-metadata
  "Description
    Restore all backups from user-stored buffer

  Example
    (restore-backup-metadata)
    (restore-backup-metadata default-backup-loader)"
  ([] (restore-backup-metadata default-backup-loader))
  ([f-backup-loader]
   (if-let [backup (f-backup-loader)]
     (try (let [backup-swapped (read-string backup)
                table-list     (:table backup-swapped)
                metadata-list  (map #(assoc % :id nil) (:backup backup-swapped))
                info           (:info backup-swapped)]
            (do-clear-meta)
            (map #(db/exec (update-sql-by-id-template "metadata" %)) metadata-list))))))



(defn metadata-get [table]
  (first (getset! table)))

(defn metadata-set [metadata]
  (update-meta metadata))


;;;;;;;;;;;;;;;;;;;;;;;;;
;;; METABASED TOOLKIT ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;

;;; !!!- DEPRECATED NOT USED -!!!!
;;; !!!- UNSTABLE -!!!

(defn- --get-foreight-table-by-column [metadata-colum]
  ((:field metadata-colum) (first (:foreign-keys metadata-colum))))

(defn recur-find-front-path [ml]
  {:tbl ((comp :field :table :prop) ml)
   :ref (if ((comp :front-references :ref :table :prop) ml)
          (mapv #(recur-find-front-path (first (getset! %)))
                ((comp :front-references :ref :table :prop) ml)))})

(defn recur-find-columns-path [ml & {:keys [table-name]}]
  (into {(if table-name table-name (keyword ((comp :field :table :prop) ml))) ((comp :columns :prop) ml)}
        (if ((comp :front-references :ref :table :prop) ml)
          ;; reduce #(if (some? ) into) {}
          (map #(recur-find-columns-path (first (getset! %)) :table_name ((comp :front-references :ref :table :prop) ml)) 
               ((comp :front-references :ref :table :prop) ml)))))

;; (recur-find-columns-path (first (getset! :repair_contract)))
;; (defn make-column-list [table-name]
;;   (if-let [table-meta (first (getset! table-name))]
;;     (->> (recur-find-columns-path table-meta)
;;          (map (fn [[table-name columns]]
;;                 (println table-name)
;;                 (doall (map (comp (partial println "\t") :field-qualified) columns)))))))

;; (make-column-list :repair_contract)
;; (quick-front-paths :repair_contract)
(defn quick-front-paths [table-name]
  (if-let [table-meta (first (getset! table-name))]
    (recur-find-front-path table-meta)))

;; (recur-find-columns-path (first (getset! :repair_contract)))
;; (recur-find-columns-path (first (getset! :repair_contract)))
;; (map :field-qualified ((comp :columns :prop)(first (getset! :repair_contract))))
;; :cache_register->cache_register->point_of_sale->enterpreneur
;; (clojure.pprint/pprint (first(getset! :user)))
