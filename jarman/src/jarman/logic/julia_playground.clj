(ns jarman.logic.julia-playground
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



(db/query (select {:table_name :user 
                   :inner-join {:permission :id_permission}
                   :where [:or
                           [:= :login "admin"]
                           [:= :login "user"]]
                   
                   ;;                  :limit 1
                 ;;  :column [:login :password {:permission_name :hey}]
                   :column [:login :password {:permission_name :hey}]}))

(db/exec (insert :user :set {:login "julka", :password "1234",
                             :first_name "jula", :last_name "burmych",
                             :id_permission 2})) 

(db/exec (update :user
                 :where [:= :login "julka"]
                 :set {:login "anna"}))


(let [id_user (first (db/query (select {:table_name :user 
                                       :where [:= :login "user"]
                                        :column [:id]})))]
  (db/exec (delete :user
                   :where [:= :id (:id id_user)]
                   )))

(db/query (select :user))


;;repair-description

(db/exec
 (alter-table
  :repair_reasons
  :add-column {:heyy [:boolean]}))



(db/exec
 (alter-table
  :repair_errors
  :drop-column :heyy))

(db/exec
 (drop-table
  :repair_errors))


(map :field (db/query (show-table-columns :repair_errors)))


(db/query (show-tables))


(db/exec (create-table
         :repair_reasons
         :columns [{:reason [:varchar-120 :default :null]}]))


(def fill-hierarchy (-> (make-hierarchy)
                        (derive :input.radio ::checkable)
                        (derive :input.checkbox ::checkable)))
(defn- fill-dispatch [node value]
  (if-let [type (and (= :input (:tag node))
                     (-> node :attrs :type))]
    [(keyword (str "input." type)) (class value)]
    [(:tag node) (class value)]))
(defmulti fill
  #'fill-dispatch
  :default nil
  :hierarchy #'fill-hierarchy)
(defmethod fill nil
  [node value]
  (if (= :input (:tag node))
    (do
      (alter-var-root #'fill-hierarchy
                      derive (first (fill-dispatch node value)) :input)

      
      ;; (fill {:tag :div} "hello")
      ;; ;; => {:tag :div, :content ["hello"]}
      
      ;; (fill {:tag :input} "hello")
      ;; ;; => {:tag :input, :attrs {:value "hello"}}
      
      ;; (fill {:span :input} "hello")
      
