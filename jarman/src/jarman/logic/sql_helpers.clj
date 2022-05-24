(ns jarman.logic.sql-helpers
  (:require [jarman.logic.metadata   :as metadata]
            [jarman.logic.sql-tool   :as sql-tool]
            [jarman.logic.connection :as db]))

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

