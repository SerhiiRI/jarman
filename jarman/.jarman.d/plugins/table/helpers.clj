(ns plugins.table.helpers
  (:require [jarman.logic.metadata   :as metadata]
            [jarman.logic.sql-tool   :as sql-tool]
            [jarman.logic.connection :as db]))

(defn build-select-simple-data-by-the-id [table_name]
  (let [m (metadata/return-metadata table_name)
        id-column (keyword (format "%s.id" (get-in m [0 :prop :table :field])))
        m-columns (->> (get-in m [0 :prop :columns]) (filter #(not (:foreign-keys %))))
        m-fields  (->> m-columns (map :field-qualified))
        m-model   (->> m-columns (map (juxt :field-qualified :representation)) (into {}))
        cfg (fn [id]
              {:table_name table_name
               :where [:= id-column id]
               :column (conj m-fields :#as_is)})]
    {:model m-model 
     :run-sql-fn (fn [id] (first (db/query (sql-tool/select! (cfg id)))))
     :expression-sql-fn (fn [id] (sql-tool/select! (cfg id)))
     :expression-cfg-fn (fn [id] (cfg id))}))
