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
   [jarman.logic.view-manager :as view]
   [jarman.logic.state :as state]
   [jarman.plugin.data-toolkit :refer [data-toolkit-pipeline]]))


(defn- metadata-get [table]
  (first (jarman.logic.metadata/getset! table)))

(defn- metadata-set [metadata]
  (jarman.logic.metadata/update-meta metadata))

(defn- view-get
  "Description
    get view from db by table-name
  Example
    (view-get \"user\")
    {:id 2, :table_name \"user\", :view   \"(defview user (table :name \"user\"......))})"
  [table-name]
  (first (db/query
          (select! {:table_name :view :where [:= :table_name table-name]}))))

(defn- view-set
  "Description
    get view-map, write to db, rewrite file view.clj
  Example
    (view-set {:id 2, :table_name \"user\", :view \"(defview user (table :name \"user\"......))} )"
  [view]
  (let [table-name (:table_name view)
        table-view (:view view)
        id-t       (:id (first (db/query (select! {:table_name :view :where [:= :table_name table-name]}))))]   
    (if (nil? id-t)
      (db/exec (insert! {:table_name :view :set {:table_name table-name, :view table-view}}))
      (db/exec (update! {:table_name :view :set {:view table-view} :where [:= :id id-t]})))
    (view/loader-from-db)))




(view-set {:id 2, :table_name "user", :view "(defview user (table :name \"user\" :plug-place [:#tables-view-plugin] :tables [:user :permission] :view-columns [:user.login :user.password :user.first_name :user.last_name :permission.name] :model-insert [:user.login :user.password :user.first_name :user.last_name :user.id_permission] :insert-button true :delete-button true :actions [] :buttons [] :query {:table_name :user, :inner-join [:user->permission], :column [:#as_is :user.id :user.login :user.password :user.first_name :user.last_name :user.id_permission :permission.id :permission.permission_name :permission.configuration]}))"} )
;; => {:id 2, :table_name "user", :view "(defview user (table :name \"user\" :plug-place [:#tables-view-plugin] :tables [:user :permission] :view-columns [:user.login :user.password :user.first_name :user.last_name :permission.name] :model-insert [:user.login :user.password :user.first_name :user.last_name :user.id_permission] :insert-button true :delete-button true :actions [] :buttons [] :query {:table_name :user, :inner-join [:user->permission], :column [:#as_is :user.id :user.login :user.password :user.first_name :user.last_name :user.id_permission :permission.id :permission.permission_name :permission.configuration]}))"}
;; => {:id 2, :table_name "user", :view "(defview user (table :name \"user\" :plug-place [:#tables-view-plugin] :tables [:user :permission] :view-columns [:user.login :user.password :user.first_name :user.last_name :permission.name] :model-insert [:user.login :user.password :user.first_name :user.last_name :user.id_permission] :insert-button true :delete-button true :actions [] :buttons [] :query {:table_name :user, :inner-join [:user->permission], :column [:#as_is :user.id :user.login :user.password :user.first_name :user.last_name :user.id_permission :permission.id :permission.permission_name :permission.configuration]}))"}




