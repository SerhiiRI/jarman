(ns jarman.logic.view-playground
  (:refer-clojure :exclude [update])
  (:require
   ;; Clojure toolkit
  ;; [datascript.core :as d]
   [clojure.string :as string]
   [clojure.java.io :as io]
   [seesaw.core :as c]
   ;; for datascript
   [clojure.set :as set]
   [datascript.core :as d]
   [rum.core :as rum]
   [datascript.transit :as dt]
   ;; ;; Jarman toolkit
   [jarman.logic.connection :as db]
   [jarman.tools.lang :include-macros true :refer :all]
   [jarman.config.environment :as env]
   [jarman.plugin.jspl :refer :all :as jspl]
   [jarman.plugin.table :as plug]
   [jarman.logic.sql-tool :as toolbox :include-macros true :refer :all]
   [jarman.logic.view-manager :include-macros true :refer :all]
   [jarman.logic.metadata :as mt]
   [jarman.plugin.data-toolkit :refer [data-toolkit-pipeline]]))

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

(defview-prepare-config
     'permission
     '(:--another :--param
       :permission [:admin :user]
       (table
        :id :UUUUUUUUUUUUUU
        :permission [:user]
        :actions {:fresh-code (fn [x] (+ 1 x))})))

;;;;;;;;;;;;;;;;;;
;;; datascript ;;;
;;;;;;;;;;;;;;;;;;

;;; initialize a database
(def conn (d/create-conn {}))

;;; { } define attributes fo schema 
;;; or datoms
(def schema {:car/maker {:db/type :db.type/ref}
             :car/colors {:db/cardinality :db.cardinality/many}})

(def conn (d/create-conn schema))

;;  insert 
(d/transact! conn [{:maker/name "Honda"
                    :maker/country "Japan"}])

;;; transact! means we’re going to transact something (insert/delete/update).          
;;; conn -> it is our DB

(d/transact! conn [{:db/id -1
                    :maker/name "BMW"
                    :maker/country "Germany"}
                   {:car/maker -1
                    :car/name "i525"
                    :car/colors ["red" "green" "blue"]}])

;; Querying
(d/q '[:find ?name
       :where
       [?e :maker/name "BMW"]
       [?c :car/maker ?e]
       [?c :car/name ?name]]
     @conn)


(let [car-entity (ffirst
                  (d/q '[:find ?c
                         :where
                         [?e :maker/name "BMW"]
                         [?c :car/maker ?e]]
                       @conn))]
  (:car/name (d/entity @conn car-entity)))

;;;;;; UPGRADE
(def schema {:car/model {:db/unique :db.unique/identity}
             :car/maker {:db/type :db.type/ref}
             :car/colors {:db/cardinality :db.cardinality/many}})

(def schema {:maker/email {:db/unique :db.unique/identity}
             :car/model {:db/unique :db.unique/identity}
             :car/maker {:db/type :db.type/ref}
             :car/colors {:db/cardinality :db.cardinality/many}})

(def conn (d/create-conn schema))

(d/transact! conn [{:maker/email "ceo@bmw.com"
                    :maker/name "BMW"}
                   {:car/model "E39530i"
                    :car/maker [:maker/email "ceo@bmw.com"]
                    :car/name "2003 530i"}])

(d/entity @conn [:car/model "E39530i"])

(d/entity @conn [:maker/email "ceo@bmw.com"])

(:maker/name (d/entity @conn [:maker/email "ceo@bmw.com"]))

(d/transact! conn [{:car/model "E39520i"
                    :car/maker [:maker/email "ceo@bmw.com"]
                    :car/name "2003 520i"}])

(d/q '[:find [?name ...]
       :where
       [?c :car/maker [:maker/email "ceo@bmw.com"]]
       [?c :car/name ?name]]
     @conn)

(d/transact! conn [{:maker/email "ceo@bmw.com"
                    :maker/name "BMW Motors"}])

(:maker/name (d/entity @conn [:maker/email "ceo@bmw.com"]))


;;;;; third part

(def schema {:user/id {:db.unique :db.unique/identity}
             :user/name {}
             :user/age {}
             :user/parent {:db.valueType :db.type/ref
                           :db.cardinality :db.cardinality/many}})

(def conn (d/create-conn schema))

(d/transact! conn
             [{:user/id "1"
               :user/name "alice"
               :user/age 27}
              {:user/id "2"
               :user/name "bob"
               :user/age 29}
              {:user/id "3"
               :user/name "kim"
               :user/age 2
               :user/parent [[:user/id "1"]
                             [:user/id "2"]]}
              {:user/id "4"
               :user/name "aaron"
               :user/age 61}
              {:user/id "5"
               :user/name "john"
               :user/age 39
               :user/parent [[:user/id "4"]]}
              {:user/id "6"
               :user/name "mark"
               :user/age 34}
              {:user/id "7"
               :user/name "kris"
               :user/age 8
               :user/parent [[:user/id "4"]
                             [:user/id "5"]]}])

(d/q '[:find ?e
       :where [?e :user/id]]
     @conn)

(d/q '[:find ?e ?n
       :where
       [?e :user/id]
       [?e :user/name ?n]]
     @conn)

(d/q '[:find [?e ...]
       :where
       [?e :user/id]]
     @conn)

(d/q '[:find [?n ...]
       :where
       [?e :user/id]
       [?e :user/name ?n]]
     @conn)

(d/q '[:find ?n .
        :where
        [?e :user/id]
        [?e :user/name ?n]]
      @conn)
