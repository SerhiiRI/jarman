(ns jarman.logic.view-playground
  (:refer-clojure :exclude [update])
  (:require
   ;; Clojure toolkit 
   [clojure.string :as string]
   [clojure.java.io :as io]
   [seesaw.core :as c]
   ;; ;; Jarman toolkit
   [jarman.logic.connection :as db]
   [jarman.tools.lang :include-macros true :refer :all]
   [jarman.config.environment :as env]
   [jarman.plugin.jspl :refer :all :as jspl]
   [jarman.plugin.table :as plug]
   [jarman.logic.sql-tool :as toolbox :include-macros true :refer :all]
   [jarman.logic.view-manager :include-macros true :refer :all]
   [jarman.logic.metadata :as mt]
   [jarman.plugin.data-toolkit :refer [data-toolkit-pipeline]])
  (:import (java.util Date)
           (java.text SimpleDateFormat)))

;;; TEST DEFVIEW SEGMENT
;; (defview permission
;;   :permission [:user :admin]
;;   (table
;;    :id :first-table
;;    :name "FIRST"
;;    :plug-place [:#tables-view-plugin]
;;    :tables [:permission]
;;    :view-columns [:permission.permission_name
;;                   :permission.configuration]
;;    :model [:permission.id
;;            {:model-reprs "First"
;;             :model-param :permission.permission_name
;;             :model-comp jarman.gui.gui-components/input-text-with-atom}
;;            :permission.configuration]
;;    :query {:column
;;            (as-is
;;             :permission.id
;;             :permission.permission_name
;;             :permission.configuration)}))

;;; ---------------------------------------
;;; Eval this function and take a look what
;;; you get in that configuration
;;;  See on:
;;;  `:permission`
;;;  `:id`
;;; ---------------------------------------
;;;

(comment
  (do-view-load)
  (global-view-configs-clean)
  (global-view-configs-get)
  ((get-in (global-view-configs-get) [:permission :table :p-1 :toolkit :select-expression])))


;; => (:jarman--localhost--3306 :jarman--trashpanda-team_ddns_net--3306 :jarman--trashpanda-team_ddns_net--3307)
(let [cfg {;; :jdbc-connection :jarman--localhost--3306
           :name "permission"
           :table-name :permission
           :plug-place [:#tables-view-plugin] ;; KEYPATH TO KEYWORD 
           :tables [:permission]
           :view-columns [:permission.permission_name
                          :permission.configuration]
           :model [{:model-reprs "Permision name"
                    :model-param :permission.permission_name
                    :model-comp jarman.gui.gui-components/input-text-with-atom}
                   :permission.configuration]
           :query {:column
                   (as-is
                    :permission.id
                    :permission.permission_name
                    :permission.configuration)}}]
  ((:select (defview-debug-toolkit cfg))))

