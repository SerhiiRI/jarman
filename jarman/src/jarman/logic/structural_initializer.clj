(ns jarman.logic.structural-initializer
  (:refer-clojure :exclude [update])
  (:require
   [clojure.data :as data]
   [clojure.string :as string]
   [jarman.logic.connection :as db]
   [jarman.logic.sql-tool :refer [select! update! insert! alter-table! create-table! delete! drop-table show-table-columns show-tables]]
   [jarman.logic.metadata :as mt]
   [jarman.config.storage :as storage]
   [jarman.config.environment :as env]
   [jarman.tools.lang :refer :all]))

(def *system-tables* ["documents" "permission" "user" "metadata" "view"])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; SYSTEM TABLES SCHEMA ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def metadata-cols [:table_name :prop])
(def metadata
  (create-table! {:table_name :metadata
                  :columns [{:table_name [:varchar-100 :default :null]}
                            {:prop [:text :default :null]}]}))

(def permission-cols [:permission_name :configuration])
(def permission
  (create-table! {:table_name :permission
                  :columns [{:permission_name [:varchar-20 :default :null]}
                            {:configuration [:tinytext :nnull :default "\"{}\""]}]}))

(def user-cols [:login :password :first_name :last_name :id_permission])
(def user
  (create-table! {:table_name :user
                  :columns [{:login [:varchar-100 :nnull]}
                            {:password [:varchar-100 :nnull]}
                            {:first_name [:varchar-100 :nnull]}
                            {:last_name [:varchar-100 :nnull]}
                            {:id_permission [:bigint-120-unsigned :nnull]}]
                  :foreign-keys [{:id_permission :permission} {:delete :cascade :update :cascade}]}))

(def view-cols [:table_name :view])
(def view
  (create-table! {:table_name :view
                  :columns [{:table_name [:varchar-100 :default :null]}
                            {:view [:text :nnull :default "\"{}\""]}]}))

(def documents-cols [:table_name :name :document :prop])
(def documents
  (create-table! {:table_name :documents
                  :columns [{:table_name [:varchar-100 :default :null]}
                            {:name [:varchar-200 :default :null]}
                            {:document [:blob :default :null]}
                            {:prop [:text :nnull :default "\"{}\""]}]}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; VALIDATOR MECHANISM ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn verify-table-columns [table table-columns]
  (if-let [column-list (not-empty (map :field (db/query (show-table-columns table))))]
    (reduce #(and %1 (in? column-list %2)) true (mapv name table-columns))))

(defn verify-tables []
  (verify-table-columns :user user-cols)
  (verify-table-columns :permission permission-cols)
  (verify-table-columns :documents documents-cols)
  (verify-table-columns :metadata metadata-cols)
  (verify-table-columns :view view-cols))

(defn test-permission []
  (letfn [(on-pred-permission [permission_name pred]
            (if-let [permission-m (not-empty (db/query (select! {:table_name :permission :where [:= :permission_name (name permission_name)]})))]
              (pred permission-m)))]
    (and (on-pred-permission :admin some?)
       (on-pred-permission :developer some?)
       (on-pred-permission :user some?))))

(defn test-user []
  (letfn [(on-test-exist [permission_name pred]
            (if-let [permission-m (not-empty (db/query (select! {:table_name :user :where [:= :login (name permission_name)]})))]
              (pred permission-m)))]
    (and (on-test-exist :adm some?)
       (on-test-exist :dev some?)
       (on-test-exist :user some?))))

(defn test-metadata []
  (if-let [tables-list (not-empty (mapv (comp second first) (db/query (show-tables))))]
    (let [sql-test (eduction
                    (comp (remove (fn [table] (in? ["metadata" "view"] table)))
                       (map (fn [table] [:= :table_name table])))
                    tables-list)]
      (= (count (remove (fn [table] (in? ["metadata" "view"] table)) tables-list))
         (count (db/query (select! {:table_name :metadata
                                    :column [:id :table_name]
                                    :where (concat [:or] sql-test)})))))))

;; (defn test-permission [permission_name pred]
;;   (if-let [permission-m (not-empty (db/query (select :permission :where [:= :permission_name (name permission_name)])))]
;;     (pred permission-m)))
;; ( (on-pred-permission :admin some?)
;;      (on-pred-permission :developer some?)
;;      (on-pred-permission :user some?))

;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; FILL DATA FOR TABLE ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn fill-permission []
  (db/exec (delete! {:table_name :permission}))
  (db/exec
   (insert! {:table_name :permission
             :column-list [:permission_name :configuration]
             :values [["admin" "{}"]
                      ["user" "{}"]
                      ["developer" "{}"]]})))

(defn fill-user []
  (if-let [perm (first (db/query (select! {:table_name :permission :column [:id] :where [:= :permission_name "admin"]})))]
    (if (empty? (db/query (select! {:table_name :user :where [:= :login "admin"]})))
      (db/exec
       (insert! {:table_name :user
                 :column-list [:login :password :first_name :last_name :id_permission]
                 :values [["admin" "admin" "admin" "admin" (:id perm)]]}))))
  (if-let [perm (first (db/query (select! {:table_name :permission :column [:id] :where [:= :permission_name "developer"]})))]
    (if (empty? (db/query (select! {:table_name :user :where [:= :login "dev"]})))
      (db/exec
       (insert! {:table_name :user
                 :column-list [:login :password :first_name :last_name :id_permission]
                 :values [["dev" "dev" "dev" "dev" (:id perm)]]}))))
  (if-let [perm (first (db/query (select! {:table_name :permission :column [:id] :where [:= :permission_name "user"]})))]
    (if (empty? (db/query (select! {:table_name :user :where [:= :login "user"]})))
      (db/exec
       (insert! {:table_name :user
                 :column-list [:login :password :first_name :last_name :id_permission]
                 :values [["user" "user" "user" "user" (:id perm)]]})))))

(defn fill-metadata []
  (doall (mt/do-create-meta))
  (doall (mt/do-create-references)))

(defn hard-reload-struct []
  ;; for make it uncoment section belove
  ;; map db/exec
  [(drop-table :user) user
   (drop-table :permission) permission
   (drop-table :view) view
   (drop-table :metadata) metadata
   (drop-table :documents) documents])

;;;;;;;;;;;;;;;;
;;; STRATEGY ;;;
;;;;;;;;;;;;;;;;

;; if table exists
;;    |f> create-table!
;;    |t> if table-columns is verified
;;           |f> raise error, that DB currently have
;;               user-bank and ask if user want to
;;               reset whole Structural Table
;;                  |t> (hard-reload-struct)
;;                  |f> Show error and purpose to exit from jarman
;;           |t> (fill-table) if needed. 

;; (db/exec (drop-table :permission))
;; (db/exec (delete :permission))

;; (table-verification-mechanism [& {:keys [on-table-exist-false
;;                                          on-table-verify-columns-false]}])

;; (defn table-verification-mechanism [table-list table-name table-columns
;;                                     & {:keys [:on-table-not-exist
;;                                               :on-table-not-exist]}]
;;   (if (verify-table-exists :permission d)
;;     (if (verify-table-columns :permission permission-cols)
;;       (if (test-permission) true (do (fill-permission) true))
;;       {:valid? false :output "Permission table not compatible with Jarman"})
;;     (do (db/exec permission) (fill-permission) true)))

(defn verify-table-exists [table-name table-list]
  (in? table-list (name table-name)))

(defn procedure-test-permission [tables-list]
  (if (verify-table-exists :permission tables-list)
    (if (verify-table-columns :permission permission-cols)
      (if (test-permission) true (do (fill-permission) true))
      {:valid? false :output "Permission table not compatible with Jarman" :table :permission})
    (do (db/exec permission) (fill-permission) true)))

(defn procedure-test-user [tables-list]
  (if (verify-table-exists :user tables-list)
    (if (verify-table-columns :user user-cols)
      (if (test-user) true (do (fill-user) true))
      {:valid? false :output "User table not compatible with Jarman" :table :user})
    (do (db/exec user) (fill-user) true)))

(defn procedure-test-metadata [tables-list]
  (if (verify-table-exists :metadata tables-list)
    (if (verify-table-columns :metadata metadata-cols)
      (if-not (test-metadata) (do (mt/do-create-meta) true) true)
      {:valid? false :output "Metadata table not compatible with Jarman" :table :metadata})
    (do (db/exec metadata) (fill-metadata) true)))

(defn procedure-test-documents [tables-list]
  (if (verify-table-exists :documents tables-list)
    (if (verify-table-columns :documents documents-cols)
      true {:valid? false :output "Documents table not compatible with Jarman" :table :documents})
    (do (db/exec documents) true)))

(defn procedure-test-view [tables-list]
  (if (verify-table-exists :view tables-list)
    (if (verify-table-columns :view view-cols)
      true {:valid? false :output "View table not compatible with Jarman" :table :view})
    (do (db/exec view) true)))

(defn procedure-create-all-structure []
  (db/exec permission) (fill-permission) 
  (db/exec user)       (fill-user)
  (db/exec metadata)   (fill-metadata) 
  (db/exec documents)
  (db/exec view))

(defn procedure-delete-all-structure []
  (db/exec (drop-table :user))
  (db/exec (drop-table :permission))
  (db/exec (drop-table :metadata))
  (db/exec (drop-table :documents))
  (db/exec (drop-table :view)))

;; (procedure-test-all)
(defn procedure-test-all []
  ;; if some tables exist?
  (if-let [tables-list (not-empty (mapv (comp second first) (db/query (show-tables))))]
    ;; if table exists
    (filter map?
     [(procedure-test-permission tables-list)
      (procedure-test-user tables-list)
      (procedure-test-metadata tables-list)
      (procedure-test-documents tables-list)
      (procedure-test-view tables-list)])
    ;; create whole jarman infrastructure
    (procedure-create-all-structure)))



