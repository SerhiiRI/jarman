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

(build-select-simple-data-by-the-id :user)


