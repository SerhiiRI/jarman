(ns jarman.logic.document-manager
  (:refer-clojure :exclude [update])
  (:import (java.sql PreparedStatement Connection Types)
           (java.io FileInputStream File IOException)
           (java.sql ResultSet SQLException))
  (:require
   ;; Clojure toolkit 
   [clojure.data :as data]
   [clojure.string :as string]
   

   ;; Seesaw components
   ;; [seesaw.util :as u]
   ;; [seesaw.core :as c]
   ;; [seesaw.border :as sborder]
   ;; [seesaw.dev :as sdev]
   ;; [seesaw.mig :as smig]
   ;; [seesaw.swingx :as swingx]

   ;; Jarman toolkit
   [jarman.logic.connection :as db]
   [jarman.config.config-manager :as cm]
   [jarman.tools.lang :refer :all]
   [jarman.gui.gui-tools :refer :all :as gtool]
   [jarman.resource-lib.icon-library :as ico]
   [jarman.tools.swing :as stool]
   [jarman.gui.gui-components :refer :all :as gcomp]
   [jarman.config.storage :as storage]
   [jarman.config.environment :as env]
   [jarman.logic.sql-tool :as toolbox :include-macros true :refer :all]
   [jarman.logic.metadata :as mt])
  (:import (java.util Date)
           (java.text SimpleDateFormat)))


(defn select-documents []
  (db/query (select :documents
                    :column [:id {:documents.table :table} :name :prop])))

(defn update-document [document-map]
  (if (:id document-map)
    (let [m (update-in document-map [:prop] pr-str)]
      (db/exec (update :documents
                       :set m
                       :where [:= :id (:id document-map)])))))

(defn delete-document [document-map]
  (if (:id document-map)
   (db/exec (delete :documents
                    :where [:= :id (:id document-map)]))))



(defn- -update-document-jdbc [document-map]
 (let [^java.sql.Connection
       connection (clojure.java.jdbc/get-connection (db/connection-get))

       ^java.io.FileInputStream
       stream-f (FileInputStream. (File. (:document document-map)))

       ^java.sql.PreparedStatement
       statement
       (.prepareStatement
        connection
        "UPDATE `documents` SET `table`=?, `name`=?, `document`=?, `prop`=? WHERE `id`=?")]
   (try (do
          (.executeUpdate
           (doto statement
             (.setString 1 (:table document-map))
             (.setString 2 (:name document-map))
             (.setBinaryStream 3 stream-f)
             (.setString 4 (pr-str (:prop document-map)))
             (.setLong 5 (:id document-map)))))
        (finally
          (.close connection)
          (.close stream-f)))))

(defn- -insert-document-jdbc [document-map]
 (let [^java.sql.Connection
       connection (clojure.java.jdbc/get-connection (db/connection-get))

       ^java.io.FileInputStream
       stream-f (FileInputStream. (File. (:document document-map)))

       ^java.sql.PreparedStatement
       statement
       (.prepareStatement
        connection
        "INSERT INTO documents (`id`, `table`, `name`, `document`, `prop`) values (?,?,?,?,?)")]
   (try (do
          (.executeUpdate
           (doto statement
             (.setNull 1 java.sql.Types/BIGINT)
             (.setString 2 (:table document-map))
             (.setString 3 (:name document-map))
             (.setBinaryStream 4 stream-f)
             (.setString 5 (pr-str (:prop document-map))))))
        (finally
          (.close connection)
          (.close stream-f)))))

(defn- -download-to-storaget-document-jdbc [document-map]
 (let [^java.sql.Connection
       connection (clojure.java.jdbc/get-connection (db/connection-get))

       ^java.sql.PreparedStatement
       statement (.prepareStatement
                  connection
                  "SELECT `id`, `table`, `name`, `document`, `prop` FROM documents WHERE `id` = ?")

       ^java.sql.ResultSet
       res-set (.executeQuery (do (.setLong statement 1 (:id document-map)) statement))

       temporary-result (ref [])
       temporary-push
       (fn [i t n d p]
         (dosync (alter temporary-result
                        #(conj % {:id i :table t :name n :document d :prop p}))))]
   (try (while (.next res-set)
          (let [^java.lang.String
                file-name (format "%s.odt" (string/trim (.getString res-set "name")))
                ^java.io.File
                file (clojure.java.io/file (storage/document-templates-dir) file-name)
                ^java.io.FileOutputStream
                fileStream (java.io.FileOutputStream. file)
                ^java.io.InputStream
                input (.getBinaryStream res-set "document")
                buffer (byte-array 1024)]
            (while (> (.read input buffer) 0)
              (.write fileStream buffer))
            (.close input)
            (.flush fileStream)
            (.close fileStream)
            (temporary-push
             (.getLong res-set "id")
             (.getString res-set "table")
             (.getString res-set "name")
             (.getAbsolutePath file)
             (read-string (.getString res-set "prop")))))
        (catch SQLException e (println e))
        (catch IOException e (println e))
        (finally (try (.close res-set)
                      (catch SQLException e (println e))))) @temporary-result))

(defn insert-document [document-map]
  (if (and (:document document-map)
         (.exists (File. (:document document-map))))
    (if (some? (:id document-map))
      (-update-document-jdbc document-map)
      (-insert-document-jdbc document-map))
    (update-document document-map)))

(defn download-document [document-map]
  (if (some? (:id document-map))
    (-download-to-storaget-document-jdbc document-map)))

;; (storage/document-templates-spit "some file with space.txt" "suka")
;; (storage/document-templates-list)
;; (storage/document-templates-clean)
;; (clojure.java.io/file (storage/document-templates-dir) "temp.odt")

;;; TEST SEGMENT 
;; (insert-document
;;  {:table "-----", :name "also-test",
;;   :document "templates\\dovidka.odt"
;;   :prop {:suak [:bliat [:ello]]}})

;; (insert-document
;;  {:id 7, :table "-----", :name "also-test",
;;   ;; :document "templates\\dovfdsaidka.odt"
;;   :prop {:suak [:bliat [:ello]]}})

;; (delete-document
;;  {:id 7})

;; (download-document
;;  {:id 17})


(defn select-documents-by-table [table]
  (map #(update-in % [:prop] read-string)
       (db/query (select! :documents :column [:id :name :prop]
                          :where [:= :documents.table (name table)]))))

;; (select-documents-by-table :service_contract)

(defn prepare-export-file [table document-to-export]
  (let [file-name (format "%s.odt" (name (:name document-to-export)))
        path-to-template (clojure.java.io/file (storage/document-templates-dir) file-name)
        id-table-field (keyword (format "%s.id"(name table)))]
    (if (map? (:prop document-to-export))
      (fn [model-id export-directory]
        (if (not (.exists path-to-template))
          ;; (doc/download-document document-to-export)
          (println "DOWNLOADING FILE>>>>>"))
        (println "EXPORT DOCUMENT: ")
        (println ;;merge-doc
         "\nFROM: "
         (str path-to-template)
         "\nTO: "
         (str (clojure.java.io/file export-directory file-name))
         "\nSQL: "
         (select!
          (merge {:table-name table}
                 (:prop document-to-export)
                 {:where [:= id-table-field model-id]}))))
      (fn [& body]
        (println "Exporter property is not map-type")))))
