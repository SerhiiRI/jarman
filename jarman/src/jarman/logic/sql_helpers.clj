(ns jarman.logic.sql-helpers
  (:require [jarman.logic.metadata   :as metadata]
            [jarman.logic.sql-tool   :as sql-tool]
            [jarman.tools.org :refer :all]
            [jarman.logic.connection :as db]
            [jarman.config.environment :as env])
  (:import (java.sql PreparedStatement Connection Types)
           (java.io FileInputStream File IOException)
           (java.sql ResultSet SQLException)))

(defn- help-return-id-column-for-table [^jarman.logic.metadata_core.IMetadata table-meta]
  (keyword (format "%s.id" (.return-table_name table-meta))))

(defn build-select-simple-data-by-the-id [table_name]
  (let [m (metadata/return-metadata table_name)
        id-column (keyword (format "%s.id" (get-in m [0 :prop :table :field])))
        metadata (->> (get-in m [0 :prop :columns])
                      (filter #(not (:foreign-keys %)))
                      (map :field-qualified))
        cfg (fn [id]
              {:table_name table_name
               :where [:= id-column id]
               :column (conj metadata :#as_is)})]
    {;; :metafields metadata
     :run-sql-fn (fn [id] (db/query (sql-tool/select! (cfg id))))
     :expression-sql-fn (fn [id] (sql-tool/select! (cfg id)))
     :expression-cfg-fn (fn [id] (cfg id))}))

;; (build-select-simple-data-by-the-id :user)


(defn date-formatter [^java.util.Date date]
  (.format (java.text.SimpleDateFormat. "yyyy-MM-dd") date))
(defn datetime-formatter [^java.util.Date date]
  (.format (java.text.SimpleDateFormat. "yyyy-MM-dd HH:mm") date))
(defn result-transformer-f [m tranformer-m]
  (persistent!
   (reduce (fn [acc [k transformer]]
             (assoc! acc k (transformer (get acc k))))
           (transient m) tranformer-m)))
(defn result-transformer-l [tranformer-m m]
  (persistent!
   (reduce (fn [acc [k transformer]]
             (assoc! acc k (transformer (get acc k))))
           (transient m) tranformer-m)))
(defn result-transformer-fn [tranformer-m]
  (fn [m]
   (persistent!
    (reduce (fn [acc [k transformer]]
              (assoc! acc k (transformer (get acc k))))
            (transient m) tranformer-m))))

(defn upload-blob!
  [{:keys [table_name file column where]}]
  (let [^java.sql.Connection
        connection (clojure.java.jdbc/get-connection (db/connection-get))

        blob-input-stream (FileInputStream. (File. file))

        s-sql-expr {:table_name table_name :set {column :?}
                    :where where}

        ^java.sql.PreparedStatement
        statement (.prepareStatement connection (sql-tool/update! s-sql-expr))]
    (try
      (do
       (.setBinaryStream statement 1 blob-input-stream)
       (.executeUpdate statement))
      (catch SQLException e (print-error e))
      (catch IOException  e (print-error e))
      (finally
        (.close connection)
        (.close blob-input-stream)))))

(defn select-blob!
  [{:keys [table_name column-name column-blob file-path where]}]
  (let [^java.sql.Connection
        connection (clojure.java.jdbc/get-connection (db/connection-get))

        sql-select-query-map
        {:table_name table_name
         :column [:#as_is column-name column-blob]
         :where where}

        ;; make classical JDBC query statement 
        ^java.sql.PreparedStatement
        statement (.prepareStatement connection (sql-tool/select! sql-select-query-map))

        ;; prepare result query hash-map-like container
        ^java.sql.ResultSet
        res-set (.executeQuery statement)]
    (try (while (.next res-set)
           (let [^java.io.File
                 file (clojure.java.io/file file-path (.getString res-set (name column-name)))
                 ^java.io.FileInputStream
                 fileStream (java.io.FileOutputStream. file)
                 ^java.io.InputStream
                 input (.getBinaryStream res-set (name column-blob))
                 ^"[B"
                 buffer (byte-array 1024)]
             (do (while (> (.read input buffer) 0)
                   (.write fileStream buffer))
                 (.close input))))
         (catch SQLException e (print-error e))
         (catch IOException  e (print-error e))
         (finally (try (.close res-set)
                       (catch SQLException e (print-error e)))))))

(comment
 (upload-blob!
  {:table_name :seal
   :column :seal.file
   :file "/home/serhii/programs/jarman/jarman/src/jarman/logic/julia_playground.clj"
   :where [:= :seal.id 14]})
 (select-blob!
  {:table_name  :seal
   :column-name :seal.file_name
   :column-blob :seal.file
   :file-path   "/home/serhii/programs/jarman/jarman/src/jarman/logic/"
   :where       [:= :seal.id 14]}))

(comment
  (map #(result-transformer %
                            {:date date-formatter
                             :datetime datetime-formatter})
       [{:date (java.util.Date.)
         :datetime (java.util.Date.)}])

  (->> (db/query
        (sql-tool/select!
         {:table_name :seal,
          :column
          [:#as_is
           :seal.id
           :seal.seal_number
           :seal.datetime_of_use
           :seal.datetime_of_remove
           :seal.site_name
           :seal.site_url
           :seal.file_name
           ;; :seal.file
           :seal.ftp_file_name
           ;; :seal.ftp_file_path
           ]}))
       (map (result-transformer-fn
             {:seal.datetime_of_use datetime-formatter
              :seal.datetime_of_remove datetime-formatter}))))


