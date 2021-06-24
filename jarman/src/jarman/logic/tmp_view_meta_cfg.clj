(ns jarman.logic.tmp-view-meta-cfg
  (:require
   ;; Clojure toolkit 
   [clojure.string :as string]
   [clojure.java.io :as io]
   [seesaw.core :as c]
   ;; ;; Jarman toolkit
   [jarman.gui.gui-seed :as gseed]
   [jarman.logic.connection :as db]
   [jarman.tools.lang :include-macros true :refer :all]
   [jarman.config.environment :as env]
   [jarman.plugin.jspl :refer :all :as jspl]
   [jarman.plugin.table :as plug]
   [jarman.logic.sql-tool :refer [select! update! insert!]]
   [jarman.logic.metadata]
   [jarman.logic.view]
   [jarman.logic.state :as state]
   [jarman.plugin.data-toolkit :refer [data-toolkit-pipeline]]))


(defn- metadata-get [table]
  (first (jarman.logic.metadata/getset! table)))

(defn- metadata-set [view]
  (jarman.logic.metadata/update-meta metadata))

(defn- view-get [table]
  (first (jarman.logic.metadata/getset! table)))

(defn- view-set [view]
  (jarman.logic.metadata/update-meta metadata))

(metadata-get :user)
(defn- ^clojure.lang.PersistentList update-sql-by-id-template
  [table m]
  (letfn [(serialize [m] (update m :prop #(str %)))]
    (if (:id m)
      (update! {:table_name table :set (serialize (dissoc m :id)) :where [:= :id (:id m)]})
      (insert! {:table_name table :values (vals (serialize m))}))))


(let [table-name (str (second (first data)))
                   table-data (str (first data))
                   id-t (:id (first (db/query
                                     (select! {:table_name :view :where [:= :table_name table-name]}))))]   
               (if-not (= s 0)
                 (if (nil? id-t)
                   (db/exec (insert! {:table_name :view :set {:table_name table-name, :view table-data}}))
                   (db/exec (update
                             :view
                             :where [:= :id id-t]
                             :set {:view table-data})))))

(let [table-name (str (second (first data)))
                   table-data (str (first data))
                   id-t (:id (first (db/query
                                     (select! {:table_name :view :where [:= :table_name table-name]}))))]   
               (if-not (= s 0)
                 (if (nil? id-t)
                   (db/exec (insert! {:table_name :view :set {:table_name table-name, :view table-data}}))
                   (db/exec (update
                             :view
                             :where [:= :id id-t]
                             :set {:view table-data})))))
