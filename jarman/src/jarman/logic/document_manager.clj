(ns jarman.logic.document-manager
  (:import (java.sql PreparedStatement Connection Types)
           (java.io FileInputStream File IOException)
           (java.sql ResultSet SQLException))
  (:require
   ;; Clojure toolkit 
   [clojure.string :as string]
   [clojure.java.io :as io]
   [clojure.java.jdbc :as jdbc]
   [kaleidocs.merge :refer [merge-doc]]
   ;; Jarman toolkit
   [jarman.logic.connection :as db]
   [jarman.tools.lang :refer :all]
   [jarman.config.storage :as storage]
   [jarman.config.environment :as env]
   [jarman.logic.sql-tool :refer [select! update! insert! delete!]])
  (:import (java.util Date)
           (java.text SimpleDateFormat)))

(defn select-documents []
  (db/query (select! {:table_name :documents
                      :column [:id :table_name :name :prop]})))

(defn update-document [document-map]
  (if (:id document-map)
    (let [m (update-in document-map [:prop] pr-str)]
      (db/exec (update! {:table_name :documents
                         :set m
                         :where [:= :id (:id document-map)]})))))

(defn delete-document [document-map]
  (if (:id document-map)
    (db/exec (delete! {:table_name :documents
                       :where [:= :id (:id document-map)]}))))

(defn- -update-document-jdbc [document-map]
 (let [^java.sql.Connection
       connection (clojure.java.jdbc/get-connection (db/connection-get))

       ^java.io.FileInputStream
       stream-f (FileInputStream. (File. (:document document-map)))

       ^java.sql.PreparedStatement
       statement
       (.prepareStatement
        connection
        "UPDATE `documents` SET `table_name`=?, `name`=?, `document`=?, `prop`=? WHERE `id`=?")]
   (try (do
          (.executeUpdate
           (doto statement
             (.setString 1 (:table_name document-map))
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
             (.setString 2 (:table_name document-map))
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
                  "SELECT `id`, `table_name`, `name`, `document`, `prop` FROM documents WHERE `id` = ?")

       ^java.sql.ResultSet
       res-set (.executeQuery (do (.setLong statement 1 (:id document-map)) statement))

       temporary-result (ref [])
       temporary-push
       (fn [i t n d p]
         (dosync (alter temporary-result
                        #(conj % {:id i :table_name  t :name n :document d :prop p}))))]
   (try (while (.next res-set)
          (let [^java.lang.String
                file-name (format "%s.odt" (string/trim (.getString res-set "name")))
                ^java.io.File
                file (clojure.java.io/file (storage/document-templates-dir) file-name)
                ^java.io.FileInputStream
                fileStream (java.io.FileOutputStream. file)
                ^java.io.InputStream
                input (.getBinaryStream res-set "document")
                buffer (byte-array 1024)]
            (while (> (.read input buffer) 0)
              (.write fileStream buffer))
            (.close input)
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

;; {:id 3
;;  :table_name "service_contract"
;;  :name "Vat za ten rok"
;;  :document "./some/file.odt"
;;  :prop {...}}

(defn insert-document [document-map]
  (if (and (:document document-map)
         (.exists (File. (:document document-map))))
    (if (some? (:id document-map))
      (-update-document-jdbc document-map)
      (-insert-document-jdbc document-map))
    (update-document document-map)))

;; (.exists (clojure.java.io/file "templates\\dovidka.odt"))

(defn download-document [document-map]
  (if (some? (:id document-map))
    (-download-to-storaget-document-jdbc document-map)))



;; (storage/document-templates-spit "some file with space.txt" "suka")
;; (storage/document-templates-list)
;; (storage/document-templates-clean)
;; (clojure.java.io/file (storage/document-templates-dir) "temp.odt")

;;; TEST SEGMENT

;;(db/connection-get)

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
       (db/query (select! {:table_name :documents :column [:id :name :prop]
                           :where [:= :documents.table (name table)]}))))

(defn prepare-export-file [table document-to-export]
  (let [file-name (format "%s.odt" (name (:name document-to-export)))
        path-to-template (clojure.java.io/file (storage/document-templates-dir) file-name)
        id-table-field (keyword (format "%s.id"(name table)))]
    (if (map? (:prop document-to-export))
      (fn [model-id export-directory]
        (if (not (.exists path-to-template))
          (download-document document-to-export)
          ;; (println "DOWNLOADING FILE>>>>>")
          )
        ;; (println "EXPORT DOCUMENT: ")
        (if-let [founded (first (db/query (select!
                                           (merge {:table_name table}
                                                  (:prop document-to-export)
                                                  {:where [:= id-table-field model-id]}))))]
          (merge-doc
           path-to-template
           (clojure.java.io/file export-directory file-name)
           founded)
          (println "this string shouldn't ever be printed, fatal error")))
      (fn [& body]
        (println "Exporter property is not map-type")))))

(defn- kwds-pair-list? [col]
  (every? #(every? keyword? %) col))

(defn update-blob!
  "Description
    Upload some blob content to the database  
  
  Warning!
    not add :id into `column-list`
  
  Example
    (update-blob! {:table_name :documents
                   :column-list [[:table_name :string] [:name :string] [:document :blob] [:prop :string]]
                   :values {:id 18,
                            :table_name \"some-table\"
                            :name \"Export month operations\"
                            :document \"./tmp2.txt\"
                            :prop (pr-str {})}})"
  [{table_name :table_name col-list :column-list m :values}]
  {:pre [(some? col-list) (kwds-pair-list? col-list)]}
  (let [^java.sql.Connection
       connection (clojure.java.jdbc/get-connection (db/connection-get))

       col-list (doall (map (fn [[col-name col-type]] [col-name col-type (if (= :blob col-type) (FileInputStream. (File. (col-name m))))]) col-list))

       ^java.sql.PreparedStatement
       statement
       (.prepareStatement
        connection
        (update! {:table_name table_name
                  :set (reduce (fn [acc [col-name col-type]]
                                 (into acc {col-name :?})) {} col-list)
                  :where [:= :id (:id m)]}))]
   (try (do
          (doall
           (map-indexed
            (fn [i [col-name col-type maybe-stream]]
              (let [i (inc i)]
                (case col-type
                  nil       (.setNull statement i java.sql.Types/BIGINT)
                  :null     (.setNull statement i java.sql.Types/BIGINT)
                  :string   (.setString statement i (col-name m))
                  :blob     (.setBinaryStream statement i maybe-stream)
                  :bool     (.setBoolean statement i (col-name m))
                  :int      (.setInt statement i (col-name m))
                  :double   (.setDouble statement i (col-name m))
                  :long     (.setLong statement i (col-name m))
                  :float    (.setFloat statement i (col-name m))
                  :date     (.setDate statement i (col-name m))
                  :datetime (.setTimestamp statement i (col-name m))
                  (.setNull statement i java.sql.Types/BIGINT)))) col-list))
          (.executeUpdate statement))
        (finally
          (.close connection)
          (doall (map (fn [[col-name col-type maybe-stream]]
                        (if (and (some? maybe-stream) (instance? java.io.FileInputStream maybe-stream))
                          (.close maybe-stream))) col-list))))))

(defn insert-blob!
  "Description
    insert some query with blob into the database
  
  Example
    (insert-blob! {:table_name :documents
                   :column-list [[:id :null] [:table_name :string] [:name :string] [:document :blob] [:prop :string]]
                   :values {:id 1,
                            :table_name \"some-table\"
                            :name \"Export month operations\"
                            :document \"./tmp.txt\"
                            :prop (pr-str {})}})"
  [{table_name :table_name col-list :column-list m :values}]
  {:pre [(some? col-list) (kwds-pair-list? col-list)]}
  (let [^java.sql.Connection
        connection (clojure.java.jdbc/get-connection (db/connection-get))

        col-list (doall (map (fn [[col-name col-type]] [col-name col-type (if (= :blob col-type) (FileInputStream. (File. (col-name m))))]) col-list))

        ^java.sql.PreparedStatement
        statement
        (.prepareStatement
         connection
         (insert! {:table_name table_name
                   :column-list (mapv first col-list)
                   :values (vec (take (count col-list) (repeat :?)))}))]
    (try (do
           (doall
            (map-indexed
             (fn [i [col-name col-type maybe-stream]]
               (let [i (inc i)]
                 (case col-type
                   nil       (.setNull statement i java.sql.Types/BIGINT)
                   :null     (.setNull statement i java.sql.Types/BIGINT)
                   :string   (.setString statement i (col-name m))
                   :blob     (.setBinaryStream statement i maybe-stream)
                   :bool     (.setBoolean statement i (col-name m))
                   :int      (.setInt statement i (col-name m))
                   :double   (.setDouble statement i (col-name m))
                   :long     (.setLong statement i (col-name m))
                   :float    (.setFloat statement i (col-name m))
                   :date     (.setDate statement i (col-name m))
                   :datetime (.setTimestamp statement i (col-name m))
                   (.setNull statement i java.sql.Types/BIGINT)))) col-list))
           (.executeUpdate statement))
         (finally
           (.close connection)
           (doall (map (fn [[col-name col-type maybe-stream]]
                         (if (and (some? maybe-stream) (instance? java.io.FileInputStream maybe-stream))
                           (.close maybe-stream))) col-list))))))

(defn select-blob!
  "Description
    Allow getting :blob documents directly from db
  
    Example
    (select-blob! {:table_nae :documents
                   :column-list [[:id :null] [:table_name :string] [:name :string] [:document :blob] [:prop :string]]
                   :doc-column [[:document {:document-name :table_name :document-place \"/home/serhii\"}]]
                   :where [:= :id 18]})
       => [{:id nil, :table_name \"some-table\", :name \"Export month operations\", :document \"/home/serhii/some-table\", :prop \"{}\"}]"
  [query-map]
  {:pre [(some? (:column-list query-map))
         (kwds-pair-list? (:column-list query-map))
         (contains? query-map :doc-column)]}
  (let [^java.sql.Connection
        connection (clojure.java.jdbc/get-connection (db/connection-get))

        ;; Transform to normal select
        ;; column [[col-name col-type]..] to [col-name, col-name
        ;; change :column-list on :column
        ;; delete unnessesary select! keys
        ^clojure.lang.PersistentArrayMap
        sql-select-query-map
        (-> query-map
            (assoc-in [:column] (mapv first (:column-list query-map)))
            (dissoc :column-list)
            (dissoc :doc-column))

        ;; make classical JDBC query statement 
        ^java.sql.PreparedStatement
        statement (.prepareStatement connection (select! sql-select-query-map))

        ;; prepare result query hash-map-like container
        ^java.sql.ResultSet
        res-set (.executeQuery statement)

        ;; really returning value from Quering
        temporary-result (ref [])
        temporary-push
        (fn [& args]
          (dosync (alter temporary-result #(conj % (apply array-map (mapcat (comp vector) (:column sql-select-query-map) args))))))]
    (try (while (.next res-set)
           (doall (for [[document
                        {:keys [document-name  document-place]
                         :or   {document-place env/user-home}}]
                       (:doc-column query-map)
                       :let [^java.io.File
                             file (clojure.java.io/file document-place (.getString res-set (name document-name)))
                             ^java.io.FileInputStream
                             fileStream (java.io.FileOutputStream. file)
                             ^java.io.InputStream
                             input  (.getBinaryStream res-set (name document))
                             buffer (byte-array 1024)]]
                   (do (while (> (.read input buffer) 0)
                         (.write fileStream buffer))
                       (.close input))))
           (apply temporary-push
                  (for [[col-name-k col-type] (:column-list query-map)
                        :let [col-name (name col-name-k)]]
                    (case col-type
                      :null     nil
                      :string   (.getString res-set col-name)
                      :blob     (if-let [[document
                                          {:keys [document-name  document-place]
                                           :or   {document-place env/user-home}}]
                                         (not-empty
                                          (first
                                           (filter
                                            #(= col-name-k (first %))
                                            (:doc-column query-map))))]
                                  (.getAbsolutePath
                                   (clojure.java.io/file
                                    document-place
                                    (.getString res-set (name document-name))))
                                  "bad paths")
                      :bool     (.getBoolean res-set col-name)
                      :int      (.getInt res-set col-name)
                      :double   (.getDouble res-set col-name)
                      :long     (.getLong res-set col-name)
                      :float    (.getFloat res-set col-name)
                      :date     (.getDate res-set col-name)
                      :datetime (.getString res-set col-name)
                      nil))))
         (catch SQLException e (println e))
         (catch IOException  e (println e))
         (finally (try (.close res-set)
                       (catch SQLException e (println e))))) @temporary-result))




(comment
  ;; query all
  (select-documents)
  ;; or
  (take-last 10 (db/query (select! {:table_name :documents
                                    :column [:id :table_name :name :prop]})))
  ;; delete by id
  (db/exec (delete! {:table_name :documents
                     :where [:= :id 17]}))
  ;; update 
  (update-blob! {:table_name :documents
                :column-list [[:table_name :string] [:name :string] [:document :blob] [:prop :string]]
                :values {:id 18,
                         :table_name "some-table"
                         :name "Export month operations"
                         :document "./tmp2.txt"
                         :prop (pr-str {})}})
  ;; delete
  (insert-blob! {:table_name :documents
                 :column-list [[:id :null] [:table_name :string] [:name :string] [:document :blob] [:prop :string]]
                 :values {:id 1,
                          :table_name "some-table"
                          :name "Export month operations"
                          :document "/home/julia/test.txt"
                          :prop (pr-str {})}})
  ;; query
  (select-blob! {:table_name :documents
                 :column-list [[:id :null] [:table_name :string] [:name :string] [:document :blob] [:prop :string]]
                 :doc-column [[:document {:document-name :table_name :document-place "/home/julia/a"}]]
                 :where [:= :id 1]})

  (select-blob! {:table_name :seal
                 :column-list [[:file_name :string]
                               [:file :blob]]
                 :doc-column [[:file {:document-name :file_name :document-place "/home/julia/a"}]]
                 :where [:= :id 79]}))


(defn convert-to-jdbc-types [component-type]
  (case (first component-type)
    nil        :null
    :date      :date
    :datetime  :datetime
    :time      :datetime   
    :link      (throw (ex-info (format "Component type `%s` not supported by `document-manager.clj`. Please change type for metadata composite column where does it put from" (first component-type))
                       {:type :unsupported-jdbc-type}))
    :number    :int
    :float     :float
    :boolean   :bool
    :textarea  :string
    :blob      :blob
    :prop      :string
    :text      :string
    :filepath  :string
    :url       nil
    true       nil))


(defn get-columns-types [comp-name meta-obj]
  (first (filter (comp not nil?) (map (fn [item] (if (= comp-name (:field-qualified item))
                                             (vec (map (fn [column] [(:field-qualified column)
                                                                    (convert-to-jdbc-types(:component-type column))])
                                                       (:columns item))) nil)) (.return-columns-composite meta-obj)))))


(comment
  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

  MU6PPUBYDrRos3qb3DRbUwP6ZRHFgI0zXWBs)
