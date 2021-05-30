;;; Jarman Standart Plugin Library
;;; This is proxy file to pinning all plugins to view.clj render
(ns jarman.plugin.jspl
  (:require [jarman.plugin.table :as jarman-table-plugin]))

(defn jarman-table
  [plugin-path global-configuration]
  (jarman-table-plugin/jarman-table-proxy plugin-path global-configuration))

;; (defplugin jarman-table jarman-table-plugin/jarman-table-proxy
;;   "Ffdoas"
;;   [:permission
;;    {:spec :global-plugin/permission
;;     :examples [:user :admin]
;;     :doc "Allow resolve access to this plugin by user"}]
;;   [:mode-spec
;;    {:spec :agenda-plugin/model-specyfication
;;     :examples {:date-from "some date"
;;                :date-to "some date"
;;                :table-model [:user.login :user.password]}
;;     :doc "Help to define which data being use for calendar"}])

;; (defn jarman-table
;;   "Desciption
;;     fdsafkjsafljsajf

;;   Keys
;;     :permission (spec `:global-plugin/permission`)
;;        Doc
;;          Allow resolve access to this plugin by user
;;        Examples
;;          [:user :admin]

;;     :permission
;;      Doc - Allow resolve access to this plugin by user
;;      Spec - :global-plugin/permission
;;      Examples - [:user :admin]"
;;   [plugin-path global-configuration]
;;   (jarman-table-plugin/jarman-table-proxy
;;    plugin-path global-configuration
;;    {:spec       :agenda-plugin/model-specyfication
;;     :permission :global-plugin/permission}))

