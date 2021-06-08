;;; Jarman Standart Plugin Library
;;; This is proxy file to pinning all plugins to view.clj render
(ns jarman.plugin.jspl
  (:require
   [jarman.plugin.table :as jarman-table-plugin]
   [jarman.gui.gui-tools :as tool]
   [jarman.config.environment :as env]
   [jarman.tools.lang :refer :all]
   [jarman.plugin.plugin :refer :all]))


(macroexpand-1
 (defplugin jarman-table
   "Plugin for generate table-data-manager"
   [:permission
    {:spec :global-plugin/permission
     :examples [:user :admin]
     :doc "Key to select of possible permissions, put this key in vec (if you don't enter this key, you will have global key from defview, in another way you will have [:user])"}]
   [:name
    {:spec :global-plugin/name
     :examples "repair_contract"
     :doc "Name of table"}]
   [:plug-place
    {:spec :global-plugin/plug-place
     :examples [:#tables-view-plugin]
     :doc "This key indicates place for component"}]
   [:tables
    {:examples [:permission]
     :doc ""}]
   [:model
    {:examples [:permission.permission_name :permission.configuration], :doc ""}]
   [:query
    {:examples {:columns (as-is :permission.id :permission.permission_name :permission.configuration)}, :doc ""}]))















