;;; Jarman Standart Plugin Library
;;; This is proxy file to pinning all plugins to view.clj render
(ns jarman.plugin.jspl
  (:require
   [clojure.spec.alpha :as s]
   [jarman.gui.gui-tools :as tool]
   [jarman.config.environment :as env]
   [jarman.tools.lang :refer :all]
   [jarman.plugin.plugin :refer :all]
   ;; plugins included in global scope
   [jarman.plugin.table]
   [jarman.plugin.dialog-table]))

(defplugin table jarman.plugin.table
  "Plugin allow to editing One table from database"
  [:tables
   {:spec [:jarman.plugin.table/tables :req-un],
    :doc "list of used tables"
    :examples "[:permission]"}]
  [:view-columns
   {:spec [:jarman.plugin.table/view-columns :req-un],
    :doc "Columns which must be represented in table on right side"
    :examples "[:permission.permission_name 
                :permission.configuration]"}]
  [:model-insert
   {;;:spec [:jarman.plugin.table/model-insert :req-un],
    :doc "Columns which represent model keys or map with overriding.
          * Bind-args is a overriding key name. 
            On left it's orginal key and you can set your own if you using another in component.
          * model-param - if you want to override component, use orgin column key. It will be 
            id in state too. If you want to add new component, set another model-param, some like
            my-comp-1."
    :examples "[:permission.permission_name 
                :permission.configuration]
                {:model-reprs \"Table\"
                 :model-param :documents.table_name
                 :bind-args {:store_id :state_is}
                 :model-comp jarman.gui.gui-components/select-box-table-list}"}]
  [:model-update
   {;;:spec [:jarman.plugin.table/model-insert :req-un],
    :doc "Columns which represent model keys or map with overriding.
          * Bind-args is a overriding key name. 
            On left it's orginal key and you can set your own if you using another in component.
          * model-param - if you want to override component, use orgin column key. It will be 
            id in state too. If you want to add new component, set another model-param, some like
            my-comp-1."
    :examples "[:permission.permission_name 
                :permission.configuration]
                {:model-reprs \"Table\"
                 :model-param :documents.table_name
                 :bind-args {:store_id :state_is}
                 :model-comp jarman.gui.gui-components/select-box-table-list}"}]
  [:insert-button
   {:spec [:jarman.plugin.table/insert-button :opt-un],
    :doc "Set button with insert new data fn from form."
    :examples "true"}]
  [:update-button
   {:spec [:jarman.plugin.table/update-button :opt-un],
    :doc "Set button with update selected row fn from form."
    :examples "true"}]
  [:delete-button
   {:spec [:jarman.plugin.table/delete-button :opt-un],
    :doc "Set button with delete row fn."
    :examples "true"}]
  [:export-button
   {:spec [:jarman.plugin.table/export-button :opt-un],
    :doc "Set button which open popup window with export to document."
    :examples "true"}]
  [:changes-button
   {:spec [:jarman.plugin.table/chenges-button :opt-un],
    :doc "Set debug button. Can display in popup window changes to insert or update. Actually it showing state."
    :examples "true"}]  
  [:actions
   {:spec [:jarman.plugin.jspl/actions :req-un],
    :doc "Realise additional logic to standart CRUD operation. Set key as id and some fn with state as arg.
          \"{:some-action-keys (fn [state]...)
          :some-another.... }\""}]
  [:buttons
   {:spec [:jarman.plugin.jspl/buttons :opt-un],
    :examples "[{:form-model :model-insert, 
                 :action :upload-docs-to-db, 
                 :title \"Upload document\"}
               {:form-model :model-update...}...]"
    :doc "This is an vector of optional buttons which do some logic bainded by acition key, discribed in `:action`"}]
  [:query
   {:spec [:jarman.plugin.jspl/query :req-un],
    :examples "{:table_name :permission, :column [:#as_is ...]...}",
    :doc "SQL syntax for `select!` query"}])


(defplugin dialog-table jarman.plugin.dialog-table
  "Plugin allow to editing One table from database"
  ;; [:view-columns
  ;;  {:spec [:jarman.plugin.table/view-columns :req-un],
  ;;   :doc "Columns which must be represented in table on right side"
  ;;   :examples "[:permission.permission_name 
  ;;               :permission.configuration]"}]
  ;; [:actions
  ;;  {:spec [:jarman.plugin.jspl/actions :opt-un],
  ;;   :doc "Realise additional logic to standart CRUD operation. Set key as id and some fn with state as arg.
  ;;         \"{:some-action-keys (fn [state]...)
  ;;         :some-another.... }\""}]
  ;; [:query
  ;;  {:spec [:jarman.plugin.jspl/query :opt-un],
  ;;   :examples "{:table_name :permission, :column [:#as_is ...]...}",
  ;;   :doc "SQL syntax for `select!` query"}]
  )







