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
           :table_name :permission
           :plug-place [:#tables-view-plugin] ;; KEYPATH TO KEYWORD 
           :tables [:permission]
           :view-columns [:permission.permission_name
                          :permission.configuration]
           :model [{:model-reprs "Permision name"
                    :model-param :permission.permission_name
                    :model-comp jarman.gui.gui-components/input-text-with-atom}
                   :permission.configuration]
           :action {:add-multiply-users-insert
                    (fn [state]
                      (let [{user-start :user-start user-end :user-end} @state]
                        (println (map #(hash-map :user.login      (str "user" %)
                                                 :user.password   "1234"
                                                 :user.last_name  (str "user" %)
                                                 :user.first_name (str "user" %)
                                                 :user.id_permission 2)
                                      (range user-start (+ 1 user-end))))))}
           :query {:column
                   (as-is
                    :permission.id
                    :permission.permission_name
                    :permission.configuration)}}]
  (defview-debug-toolkit cfg))

(defview-prepare-config
     'permission
     '(:--another :--param
       :permission [:admin :user]
       (table
        :id :UUUUUUUUUUUUUU
        :permission [:user]
        :actions {:fresh-code (fn [x] (+ 1 x))})))

(comment
  (do-view-load)
  (global-view-configs-clean)
  (global-view-configs-get)
  (get-in (global-view-configs-get) [:permission :dialog-table :test-dialog-table]))



