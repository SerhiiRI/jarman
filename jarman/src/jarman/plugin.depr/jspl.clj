;; ;;; Jarman Standart Plugin Library
;; ;;; This is proxy file to pinning all plugins to view.clj render
;; (ns jarman.plugin.jspl
;;   (:require
;;    [clojure.spec.alpha :as s]
;;    [jarman.gui.gui-tools :as tool]
;;    [jarman.config.environment :as env]
;;    [jarman.tools.lang :refer :all]
;;    [jarman.plugin.plugin :refer :all]
;;    ;; plugins included in global scope
;;    [jarman.plugin.table]
;;    [jarman.plugin.dialog-table]
;;    [jarman.plugin.dialog-test]
;;    [jarman.plugin.dialog-bigstring]
;;    [jarman.plugin.service-period]

;;    [jarman.plugins.fff]))

;; ;;; check assertion
;; (s/check-asserts true)


;; (defplugin table jarman.plugin.table
;;   "Plugin allow to editing One table from database"
;;   ;; DATATOOLKIT SPEC
;;   [:id
;;    {:spec [:jarman.plugin.spec/keyword :opt-un]
;;     :doc "Custom plugin ID"}]
;;   [:name
;;    {:spec [:jarman.plugin.spec/name :req-un]
;;     :doc "Plugin name"
;;     :example ":name \"Some str\""}]
;;   [:tables
;;    {:spec [:jarman.plugin.data-toolkit/tables :req-un],
;;     :doc "list of used tables"
;;     :examples "[:permission]"}]
;;   [:view-columns
;;    {:spec [:jarman.plugin.data-toolkit/view-columns :req-un],
;;     :doc "Columns which must be represented in table on right side"
;;     :examples "[:permission.permission_name 
;;                 :permission.configuration]"}]
;;   [:query
;;    {:spec [:jarman.plugin.data-toolkit/query :req-un],
;;     :examples "{:table_name :permission, :column [:#as_is ...]...}",
;;     :doc "SQL syntax for `select!` query"}]
;;   [:model-insert
;;    {:spec [:jarman.plugin.table/model-insert :req-un],
;;     :doc "Columns which represent model keys or map with overriding.
;;           * Bind-args is a overriding key name. 
;;             On left it's orginal key and you can set your own if you using another in component.
;;           * model-param - if you want to override component, use orgin column key. It will be 
;;             id in state too. If you want to add new component, set another model-param, some like
;;             my-comp-1."
;;     :examples "[:permission.permission_name 
;;                 :permission.configuration]
;;                 {:model-reprs \"Table\"
;;                  :model-param :documents.table_name
;;                  :bind-args {:store_id :state_is}
;;                  :model-comp jarman.gui.gui-components/select-box-table-list}"}]
;;   [:model-update
;;    {:spec [:jarman.plugin.table/model-update :opt-un],
;;     :doc "Columns which represent model keys or map with overriding.
;;           * Bind-args is a overriding key name. 
;;             On left it's orginal key and you can set your own if you using another in component.
;;           * model-param - if you want to override component, use orgin column key. It will be 
;;             id in state too. If you want to add new component, set another model-param, some like
;;             my-comp-1."
;;     :examples "[:permission.permission_name 
;;                 :permission.configuration]
;;                 {:model-reprs \"Table\"
;;                  :model-param :documents.table_name
;;                  :bind-args {:store_id :state_is}
;;                  :model-comp jarman.gui.gui-components/select-box-table-list}"}]
;;   [:active-buttons
;;    {:spec [:jarman.plugin.table/active-buttons :req-un]
;;     :doc "Select buttons who should be display."
;;     :examples ":active-buttons [:insert :update :delete :clear :changes]"}]
;;   [:actions
;;    {:spec [:jarman.plugin.table/actions :opt-un],
;;     :doc "Realise additional logic to standart CRUD operation. Set key as id and some fn with state as arg.
;;           \"{:some-action-keys (fn [state! dispatch!]...)
;;           :some-another.... }\""}]
;;   [:buttons
;;    {:spec [:jarman.plugin.table/buttons :opt-un],
;;     :examples "[{:form-model :model-insert, 
;;                  :action :upload-docs-to-db, 
;;                  :title \"Upload document\"}
;;                {:form-model :model-update...}...]"
;;     :doc "This is an vector of optional buttons which do some logic bainded by acition key, discribed in `:action`"}])

;; ;; TO DELETE
;; (defplugin dialog-test jarman.plugin.dialog-test
;;   "Plugin allow to editing One table from database")

;; (defplugin dialog-table jarman.plugin.dialog-table
;;   "Dialog table"
;;   [:tables
;;    {:spec [:jarman.plugin.data-toolkit/tables :req-un],
;;     :doc "list of used tables"
;;     :examples "[:permission]"}]
;;   [:view-columns
;;    {:spec [:jarman.plugin.data-toolkit/view-columns :req-un],
;;     :doc "Columns which must be represented in table on right side"
;;     :examples "[:permission.permission_name 
;;                 :permission.configuration]"}]
;;   [:query
;;    {:spec [:jarman.plugin.data-toolkit/query :req-un],
;;     :examples "{:table_name :permission, :column [:#as_is ...]...}",
;;     :doc "SQL syntax for `select!` query"}])

;; (defplugin dialog-bigstring jarman.plugin.dialog-bigstring
;;   "Dialog for selecting some ONE item by ONE column in `:query` model"
;;   [:item-columns
;;    {:spec [:jarman.plugin.dialog-bigstring/item-columns :req-un],
;;     :doc "Select column to be represent one value per item"
;;     :examples ":permission.permission_name"}]
;;   [:query
;;    {:spec [:jarman.plugin.data-toolkit/query :req-un],
;;     :examples "{:table_name :permission, :column [:#as_is ...]...}",
;;     :doc "SQL syntax for `select!` query"}])

;; (defplugin service-period jarman.plugin.service-period
;;   "Plugin for service contracts of enterpreneurs")

;; (defplugin fff jarman.plugins.fff
;;   "Plugin for service contracts of enterpreneurs")




;;; Jarman Standart Plugin Library
;;; This is proxy file to pinning all plugins to view.clj render
(comment (ns jarman.plugin.jspl
           (:require
            [clojure.spec.alpha :as s]
            [jarman.gui.gui-tools :as tool]
            [jarman.config.environment :as env]
            [jarman.tools.lang :refer :all]
            [jarman.plugin.plugin :refer :all]
            ;; plugins included in global scope
            [jarman.plugins.table]
            [jarman.plugins.dialog-table]
            [jarman.plugins.dialog-test]
            [jarman.plugins.dialog-bigstring]
            [jarman.plugins.service-period]

            [jarman.plugins.fff]))

;;; check assertion
         (s/check-asserts true)


         (defplugin table jarman.plugins.table
           "Plugin allow to editing One table from database"
           ;; DATATOOLKIT SPEC
           [:id
            {:spec [:jarman.plugins.spec/keyword :opt-un]
             :doc "Custom plugin ID"}]
           [:name
            {:spec [:jarman.plugins.spec/name :req-un]
             :doc "Plugin name"
             :example ":name \"Some str\""}]
           [:tables
            {:spec [:jarman.plugins.data-toolkit/tables :req-un],
             :doc "list of used tables"
             :examples "[:permission]"}]
           [:view-columns
            {:spec [:jarman.plugins.data-toolkit/view-columns :req-un],
             :doc "Columns which must be represented in table on right side"
             :examples "[:permission.permission_name 
                :permission.configuration]"}]
           [:query
            {:spec [:jarman.plugins.data-toolkit/query :req-un],
             :examples "{:table_name :permission, :column [:#as_is ...]...}",
             :doc "SQL syntax for `select!` query"}]
           [:model-insert
            {:spec [:jarman.plugins.table/model-insert :req-un],
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
            {:spec [:jarman.plugins.table/model-update :opt-un],
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
           [:active-buttons
            {:spec [:jarman.plugins.table/active-buttons :req-un]
             :doc "Select buttons who should be display."
             :examples ":active-buttons [:insert :update :delete :clear :changes]"}]
           [:actions
            {:spec [:jarman.plugins.table/actions :opt-un],
             :doc "Realise additional logic to standart CRUD operation. Set key as id and some fn with state as arg.
          \"{:some-action-keys (fn [state! dispatch!]...)
          :some-another.... }\""}]
           [:buttons
            {:spec [:jarman.plugins.table/buttons :opt-un],
             :examples "[{:form-model :model-insert, 
                 :action :upload-docs-to-db, 
                 :title \"Upload document\"}
               {:form-model :model-update...}...]"
             :doc "This is an vector of optional buttons which do some logic bainded by acition key, discribed in `:action`"}])

         ;; TO DELETE
         (defplugin dialog-test jarman.plugins.dialog-test
           "Plugin allow to editing One table from database")

         (defplugin dialog-table jarman.plugins.dialog-table
           "Dialog table"
           [:tables
            {:spec [:jarman.plugins.data-toolkit/tables :req-un],
             :doc "list of used tables"
             :examples "[:permission]"}]
           [:view-columns
            {:spec [:jarman.plugins.data-toolkit/view-columns :req-un],
             :doc "Columns which must be represented in table on right side"
             :examples "[:permission.permission_name 
                :permission.configuration]"}]
           [:query
            {:spec [:jarman.plugins.data-toolkit/query :req-un],
             :examples "{:table_name :permission, :column [:#as_is ...]...}",
             :doc "SQL syntax for `select!` query"}])

         (defplugin dialog-bigstring jarman.plugins.dialog-bigstring
           "Dialog for selecting some ONE item by ONE column in `:query` model"
           [:item-columns
            {:spec [:jarman.plugins.dialog-bigstring/item-columns :req-un],
             :doc "Select column to be represent one value per item"
             :examples ":permission.permission_name"}]
           [:query
            {:spec [:jarman.plugins.data-toolkit/query :req-un],
             :examples "{:table_name :permission, :column [:#as_is ...]...}",
             :doc "SQL syntax for `select!` query"}])

         (defplugin service-period jarman.plugins.service-period
           "Plugin for service contracts of enterpreneurs")

         (defplugin fff jarman.plugins.fff
           "Plugin for service contracts of enterpreneurs")

         )
