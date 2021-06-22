;;; Jarman Standart Plugin Library
;;; This is proxy file to pinning all plugins to view.clj render
(ns jarman.plugin.jspl
  (:require
   [clojure.spec.alpha :as s]
   [jarman.plugin.table]
   [jarman.gui.gui-tools :as tool]
   [jarman.config.environment :as env]
   [jarman.tools.lang :refer :all]
   [jarman.plugin.plugin :refer :all]))

(defplugin table jarman.plugin.table
  "Plugin allow to editing One table from database"
  [:tables
   {:spec [:jarman.plugin.table/tables :req-un],
    :doc "list of used tables"
    :examples "[:permission]"}]
  [:view-columns
   {:spec [:jarman.plugin.table/view-columns :req-un],
    :doc "Columns which must be represented in table on right side"
    :examples "[:permission.permission_name :permission.configuration]"}]
  [:model-insert
   {:spec [:jarman.plugin.table/model-insert :req-un],
    :doc "Columns which represent model keys"
    :examples "[:permission.permission_name :permission.configuration]"}]
  [:insert-button
   {:spec [:jarman.plugin.table/insert-button :opt-un],
    :doc "Controll over insert view buttons"
    :examples "true"}]
  [:delete-button
   {:spec [:jarman.plugin.table/delete-button :opt-un],
    :doc "Controll over delete view buttons"
    :examples "true"}]
  [:actions
   {:spec [:jarman.plugin.jspl/actions :req-un],
    :doc "Realise additional logic to standart CRUD operation
          \"{:some-action-keys (fn [state]...)
          :some-another.... }\""}]
  [:buttons
   {:spec [:jarman.plugin.jspl/buttons :opt-un],
    :examples "[{:form-model :model-insert, :action :upload-docs-to-db, :title \"Upload document\"}
               {:form-model :model-update...}...]"
    :doc "is vector of optional buttons which do some logic, discribed in `:action`"}]
  [:query
   {:spec [:jarman.plugin.jspl/query :req-un],
    :examples "{:table_name :permission, :column [:#as_is ...]...}",
    :doc "SQL syntax for `select!` query"}])

