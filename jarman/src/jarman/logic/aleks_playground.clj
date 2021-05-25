(ns jarman.logic.aleks-playground
  (:refer-clojure :exclude [update])
  (:require
   [clojure.data :as data]
   [clojure.string :as string]
   [jarman.logic.connection :as db]
   [jarman.logic.sql-tool :as toolbox :include-macros true :refer :all]
   [jarman.logic.metadata :as metadata]
   [jarman.tools.lang :refer :all])
  (:import (java.util Date)
           (java.text SimpleDateFormat)))

(db/query (select {:table-name :user
                   :where [:and
                           [:= :login "aleks"]
                        ;;    [:= :password "1234"]
                           ]
                   :inner-join {:permission :id_permission}
                ;;    :column [:login :password {:permission_name :pn}] ;; here i can chage :permission_name to :pn, some like alias
                   }))


(db/exec (insert :user :set 
                 {
                  :login "aleks"
                  :password "1234"
                  :first_name "Aleks"
                  :last_name "S"
                  :id_permission 1
                 }))

(db/exec (update :user
                 :where [:= :login "aleks"]
                 :set {:password "qwerty"}))


(let [id (get (first (db/query (select {:table-name :user
                                        :where [:= :login "aleks"]
                                        :column [:id]}))) :id)]
  (db/exec (delete :user
                   :where [:= :id id])))



(db/exec (create-table
          :pepe
          :columns [{:frog_name [:varchar-40 :default :null]}]))

(db/exec (alter-table
          :pepe
          :add-column {:happy [:boolean]}))

(db/query (show-table-columns :pepe))


(db/exec (alter-table
          :pepe
          :drop-column :happy))

(db/exec (drop-table
          :pepe))

(db/query (show-tables))


