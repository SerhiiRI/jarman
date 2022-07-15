(ns jarman.logic.structural-initializer
  (:refer-clojure :exclude [update])
  (:require
   [clojure.data :as data]
   [clojure.string :as string]
   [jarman.logic.connection :as db]
   [jarman.logic.sql-tool :refer [select! update! insert! alter-table! create-table! delete! drop-table show-table-columns show-tables]]
   ;; [jarman.logic.metadata :as mt]
   [jarman.logic.metadata :refer [do-create-meta-for-existing-tables]]
   [jarman.config.storage :as storage]
   [jarman.config.environment :as env]
   [jarman.lang :refer :all]))

(def system-tables ["metadata" "jarman_documents" "jarman_profile" "jarman_user" "jarman_view" "jarman_system_session" "jarman_system_props"])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; SYSTEM TABLES SCHEMA ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def metadata-cols [:table_name :prop])
(def metadata
  (create-table! {:table_name :metadata
                  :columns [{:table_name [:varchar-100 :default :null]}
                            {:prop [:text :default :null]}]}))

(def system-session-cols [:suuid])
(def system-session
  (create-table! {:table_name :jarman_system_session
                  :columns [{:suuid [:varchar-400 :default :null]}]}))

(def system-props-cols [:name :value])
(def system-props
  (create-table! {:table_name :jarman_system_props
                  :columns [{:name [:varchar-256 :default :null]}
                            {:value [:text :default :null]}]}))

(def profile-cols [:name :configuration])
(def profile
  (create-table! {:table_name :jarman_profile
                  :columns [{:name [:varchar-20 :default :null]}
                            {:configuration [:tinytext :nnull :default "\"{}\""]}]}))

(def user-cols [:login :password :first_name :last_name :id_jarman_profile :configuration])
(def user
  (create-table! {:table_name :jarman_user
                  :columns [{:login [:varchar-100 :nnull]}
                            {:password [:varchar-100 :nnull]}
                            {:first_name [:varchar-100 :nnull]}
                            {:last_name [:varchar-100 :nnull]}
                            {:configuration [:text :nnull :default "'{}'"]}
                            {:id_jarman_profile [:bigint-120-unsigned :nnull]}]
                  :foreign-keys [{:id_jarman_profile :jarman_profile} {:delete :cascade :update :cascade}]}))

(def view-cols [:table_name :jarman_view])
(def view
  (create-table! {:table_name :jarman_view
                  :columns [{:table_name [:varchar-100 :default :null]}
                            {:jarman_view [:text :nnull :default "\"{}\""]}]}))

(def documents-cols [:table_name :name :document :prop])
(def documents
  (create-table! {:table_name :jarman_documents
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
  (verify-table-columns :jarman_user user-cols)
  (verify-table-columns :jarman_profile profile-cols)
  (verify-table-columns :jarman_documents documents-cols)
  (verify-table-columns :metadata metadata-cols)
  (verify-table-columns :jarman_view view-cols)
  (verify-table-columns :jarman_system_session system-session-cols)
  (verify-table-columns :jarman_system_props system-props-cols))

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

;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; FILL DATA FOR TABLE ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn fill-metadata []
  (doall (do-create-meta-for-existing-tables)))

(defn hard-reload-struct []
  ;; for make it uncoment section belove
  ;; map db/exec
  [(drop-table :jarman_user) user
   (drop-table :jarman_profile) profile
   (drop-table :jarman_view) view
   (drop-table :metadata) metadata
   (drop-table :jarman_documents) documents])

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

(defn procedure-test-profile [tables-list]
  (if (verify-table-exists :jarman_profile tables-list)
    (if (verify-table-columns :jarman_profile profile-cols)
      true ;; (if (test-profile) true (do (fill-profile) true))
      {:valid? false :output "Profile table not compatible with Jarman" :table :jarman_profile})
    (do (db/exec profile) ;; (fill-profile)
        true)))

(defn procedure-test-user [tables-list]
  (if (verify-table-exists :jarman_user tables-list)
    (if (verify-table-columns :jarman_user user-cols)
      true ;; (if (test-user) true (do (fill-user) true))
      {:valid? false :output "User table not compatible with Jarman" :table :jarman_user})
    (do (db/exec user) ;; (fill-user)
        true)))

(defn procedure-test-metadata [tables-list]
  (if (verify-table-exists :metadata tables-list)
    (if (verify-table-columns :metadata metadata-cols)
      (if-not (test-metadata) (do (do-create-meta-for-existing-tables) true) true)
      {:valid? false :output "Metadata table not compatible with Jarman" :table :metadata})
    (do (db/exec metadata) ;; (fill-metadata)
        true)))

(defn procedure-test-documents [tables-list]
  (if (verify-table-exists :jarman_documents tables-list)
    (if (verify-table-columns :jarman_documents documents-cols)
      true {:valid? false :output "Documents table not compatible with Jarman" :table :jarman_documents})
    (do (db/exec documents) true)))

(defn procedure-test-view [tables-list]
  (if (verify-table-exists :jarman_view tables-list)
    (if (verify-table-columns :jarman_view view-cols)
      true {:valid? false :output "View table not compatible with Jarman" :table :jarman_view})
    (do (db/exec view) true)))

(defn procedure-test-system-session [tables-list]
  (if (verify-table-exists :jarman_system_session tables-list)
    (if (verify-table-columns :jarman_system_session system-session-cols)
      true {:valid? false :output "System table not compatible with Jarman" :table :jarman_system_session})
    (do (db/exec system-session) true)))

(defn procedure-test-system-props [tables-list]
  (if (verify-table-exists :jarman_system_props tables-list)
    (if (verify-table-columns :jarman_system_props system-props-cols)
      true {:valid? false :output "System table not compatible with Jarman" :table :jarman_system_props})
    (do (db/exec system-props) true)))

(defn procedure-create-all-structure []
  (db/exec profile)
  (db/exec user)
  (db/exec metadata)
  (db/exec documents)
  (db/exec view)
  (db/exec system-session)
  (db/exec system-props))

(defn procedure-delete-all-structure []
  (db/exec (drop-table :jarman_user))
  (db/exec (drop-table :jarman_profile))
  (db/exec (drop-table :metadata))
  (db/exec (drop-table :jarman_documents))
  (db/exec (drop-table :jarman_view))
  (db/exec (drop-table :jarman_system_session))
  (db/exec (drop-table :jarman_system_props)))

(defn procedure-test-all []
  ;; if some tables exist?
  (if-let [tables-list (not-empty (mapv (comp second first) (db/query (show-tables))))]
    ;; if table exists
    (filter map?
            [(procedure-test-system-session tables-list)
             (procedure-test-system-props tables-list)
             (procedure-test-profile tables-list)
             (procedure-test-user tables-list)
             (procedure-test-metadata tables-list)
             (procedure-test-documents tables-list)
             (procedure-test-view tables-list)])
    ;; create whole jarman infrastructure
    (procedure-create-all-structure)))

