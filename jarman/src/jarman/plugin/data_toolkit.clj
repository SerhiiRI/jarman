(ns jarman.plugin.data-toolkit
  (:refer-clojure :exclude [update])
  (:require
   ;; Clojure toolkit 
   [clojure.string :as string]
   [clojure.java.io :as io]
   [clojure.spec.alpha :as s]
   [seesaw.core :as c]
   ;; Jarman toolkit
   [jarman.logic.document-manager :as doc]
   [jarman.logic.connection :as db]
   [jarman.tools.lang :include-macros true :refer :all]
   [jarman.config.environment :as env]
   [jarman.logic.sql-tool :refer [select! update! delete! insert!]]
   [jarman.logic.metadata :as mt])
  (:import (java.util Date)
           (java.text SimpleDateFormat)))

;;;;;;;;;;;;;;;;;;;;;
;;; SPEC KEYWORDS ;;;
;;;;;;;;;;;;;;;;;;;;;

(s/def ::keyword-list (s/and sequential? #(every? keyword? %)))
(s/def ::tables       ::keyword-list)
(s/def ::view-columns ::keyword-list)
(s/def ::query map?)

;;;;;;;;;;;;;;;;;;;
;;; SQL TOOLKIT ;;;
;;;;;;;;;;;;;;;;;;;

(defn- tf-t-f [table-field]
  (let [t-f (string/split (name table-field) #"\.")]
    (mapv keyword t-f)))

(defn- t-f-tf [table field]
  (keyword (str (name table) "." (name (name field)))))

;;; Helper sql-functionality ;;; 
(defn- sql-operation-suffix [keyword-rule-name suffix] {:pre [(keyword? keyword-rule-name) (keyword? suffix)]}
  (string/ends-with? (name keyword-rule-name) (name suffix)))

(defn- sql-operation-expression [keyword-rule-name] {:pre [(keyword? keyword-rule-name)]}
  (keyword (format "%s-%s" (name keyword-rule-name) "expression")))

(defn- sql-make-select-wrapper
  "Description
     Take keyword as idintificator and take lambda or keyword
    for example {:table_name \"user\"} or #(:table_name (:t-name %))
    and wrapp into `select!` function which generate SQL syntax
     Function return two key first has name as `keyword-rule-name`
    which do jdbc request, second has name with `-expression` on
    end for debug aims and return only SQL syntax expression
  
  Example 
    (sql-make-select-wrapper :some-test-select {:table_name \"user\"})
    ;;=> {:some-test-select (fn [e]...) :some-test-select-expression (fn [e] ...) "
  [keyword-rule-name query-settings]
  {:pre [(keyword? keyword-rule-name)]}
  (where
   ((rule-expression  keyword-rule-name do sql-operation-expression)
    (rule-name        keyword-rule-name)
    (query-lambda-expression query-settings if2 map?
                             (fn [e] (select! query-settings))
                             (fn [e] (select! (query-settings e))))
    (query-lambda (fn [e] (db/query (query-lambda-expression e)))))
   {rule-expression query-lambda-expression
    rule-name       query-lambda}))

(defn- sql-make-wrapper-for
  "Description
     Take keyword as idintificator and take lambda for example
    #(:table_name (:t-name %)) and wrapp into `sql-operation`
    function which generate SQL syntax
     Function return two key first has name as `keyword-rule-name`
    which do jdbc request, second has name with `-expression` on
    end for debug aims and return only SQL syntax expression
  
  Example 
    (sql-make-wrapper-for insert! :user-insert {:table_name \"user\"})
    ;;=> {:user-insert (fn [e]...) :user-insert-expression (fn [e] ...) "
  [sql-operation keyword-rule-name query-lambda]
  {:pre [(keyword? keyword-rule-name) (fn? query-lambda)]}
  (where
   ((rule-expression  keyword-rule-name do sql-operation-expression)
    (rule-name        keyword-rule-name)
    (query-lambda-expression (fn [e] (sql-operation (query-lambda e))))
    (query-lambda            (fn [e] (db/exec (query-lambda-expression e)))))
   {rule-expression query-lambda-expression
    rule-name       query-lambda}))


(def ^:private sql-make-insert-wrapper (partial sql-make-wrapper-for insert!))
(def ^:private sql-make-delete-wrapper (partial sql-make-wrapper-for delete!))
(def ^:private sql-make-update-wrapper (partial sql-make-wrapper-for update!))

(defn- response-f-on-suffix
  "Example
  
  (where ((configuration {:test-m-select {:table_name \"user\"}
                        :test-select (fn [{login :login}] {:table_name \"user\" :where [:= login :login]})
                        :test-insert (fn [e] {:table_name \"user\" :values e})
                        :test-update (fn [{login :login password :password :as e}] {:table_name \"user\" :set e :where [:= :login login]})
                        :test-delete (fn [{login :login}] {:table_name \"user\" :where [:= :login login]})
                        :must-not-reaction-on-this-key nil})
        (toolkit (reduce
                  (fn [m-acc [k v]]
                    (-> m-acc
                        (supply-insert k v)
                        (supply-update k v)
                        (supply-delete k v)
                        (supply-select k v))) {} configuration))
        (test-user {:login \"admin\" :password \"12345\"}))
       {:test-m-select ((:test-m-select-expression toolkit) nil)
        :test-select ((:test-select-expression toolkit) test-user)
        :test-insert ((:test-insert-expression toolkit) test-user)
        :test-update ((:test-update-expression toolkit) test-user)
        :test-delete ((:test-delete-expression toolkit) test-user)})
 
   ;;=> 
      {:test-m-select \"SELECT * FROM user\",
       :test-select \"SELECT * FROM user WHERE \\\"admin\\\" = login\",
       :test-insert \"INSERT `user` SET login=\\\"admin\\\", password=\\\"12345\\\"\",
       :test-update \"DELETE user WHERE login = \\\"admin\\\"\",
       :test-delete \"UPDATE `user` WHERE login = \\\"admin\\\"\"}"
  [suffix sql-operation acc-m sql-query-key configuration]
  (if (sql-operation-suffix sql-query-key suffix)
    (into acc-m (sql-operation sql-query-key configuration)) acc-m))

;;; This function list add some toolkit to the map, reacting on ending suffix pattern
(def ^:private supply-select (partial response-f-on-suffix :select sql-make-select-wrapper))
(def ^:private supply-insert (partial response-f-on-suffix :insert sql-make-insert-wrapper))
(def ^:private supply-update (partial response-f-on-suffix :update sql-make-delete-wrapper))
(def ^:private supply-delete (partial response-f-on-suffix :delete sql-make-update-wrapper))

(defn- supply-sql-action-constructor [configuration toolkit-map]
  (if-let [actions  (:action configuration)]
   (reduce
    (fn [m-acc [k v]]
      (-> m-acc
          (supply-insert k v)
          (supply-update k v)
          (supply-delete k v)
          (supply-select k v)))
    toolkit-map
    actions)
   toolkit-map))

;; => (:jarman--localhost--3306 :jarman--trashpanda-team_ddns_net--3306 :jarman--trashpanda-team_ddns_net--3307)
;; (let [cfg {;; :jdbc-connection :jarman--localhost--3306
;;            :name "permission"
;;            :table_name :permission
;;            :plug-place [:#tables-view-plugin] ;; KEYPATH TO KEYWORD 
;;            :tables [:permission]
;;            :view-columns [:permission.permission_name
;;                           :permission.configuration]
;;            :model [{:model-reprs "Permision name"
;;                     :model-param :permission.permission_name
;;                     :model-comp jarman.gui.gui-components/input-text-with-atom}
;;                    :permission.configuration]
;;            :query {:column
;;                    (jarman.logic.view-manager/as-is
;;                     :permission.id
;;                     :permission.permission_name
;;                     :permission.configuration)}}]
;;   ((:select (jarman.logic.view-manager/defview-debug-toolkit cfg))))

(defn- sql-crud-toolkit-constructor
  "Description
    Generate datatoolkit for sql expression
  
  Example
    (sql-crud-toolkit-constructor
       {:table_name :repair_contract 
        ...
        :query {}} {})
    ;;=> 
      {:model-id :repair_contract.id,
       :model-columns [:user.id :user.login :user.password :user.first_name :user.last_name :user.id_permission]
       :insert (fn [..] ...),
       :delete-expression (fn [..] ...),
       :select-expression (fn [..] ...),
       :update (fn [..] ...),
       :delete (fn [..] ...),
       :update-expression (fn [..] ...),
       :insert-expression (fn [..] ...),
       :user-all-select (fn [..] ...),
       :user-all-select-expression (fn [..] ...),
       :select (fn [..] ...)}"
  [configuration toolkit-map]
  (where ((query-fn (if (:jdbc-connection configuration) (partial db/query-b (:jdbc-connection configuration)) db/query))
          (exec-fn  (if (:jdbc-connection configuration) (partial db/exec-b (:jdbc-connection configuration)) db/exec))
          (table-metadata configuration do :table_name do mt/getset! do first)
          (id_column (t-f-tf (:table_name configuration) :id))
          (table-name ((comp :field :table :prop) table-metadata))
          (columns ((comp :columns :prop) table-metadata) map :field)
          (model_column (vec (concat [id_column] (mapv :field-qualified ((comp :columns :prop) table-metadata)))))
          (update-expression (fn [entity] (if (id_column entity) (update! {:table_name table-name :set entity :where [:= id_column (id_column entity)]}))))
          (insert-expression (fn [entity] (if (nil? (id_column entity)) (insert! {:table_name table-name :set entity}))))
          (delete-expression (fn [entity] (if (id_column entity) (delete! {:table_name table-name :where [:= id_column (id_column entity)]}))))
          (select-expression (fn [& [{:as args}]]
                               (select!
                                (merge
                                 (if (:table_name (:query configuration))
                                   (:query configuration)
                                   (into (:query configuration)
                                         {:table_name (:table_name configuration)}))
                                 args)))))
         {:update-expression update-expression
          :insert-expression insert-expression
          :delete-expression delete-expression
          :select-expression select-expression
          :update (fn [e] (exec-fn (update-expression e)))
          :insert (fn [e] (exec-fn (insert-expression e)))
          :delete (fn [e] (exec-fn (delete-expression e)))
          :select (fn [& [args]] (query-fn (select-expression args)))
          :model-id id_column
          :model-columns model_column}))

;;;;;;;;;;;;;;;;;;;;;;;;
;;; METADATA TOOLKIT ;;;
;;;;;;;;;;;;;;;;;;;;;;;;

(defn- metadata-toolkit-constructor [configuration toolkit-map]
  (if-let [table-metadata (first (mt/getset! (:table_name configuration)))]
    {:table-meta   ((comp :table :prop) table-metadata)
     :columns-meta ((comp :columns :prop) table-metadata)}))

;;;;;;;;;;;;
;;; TODO ;;;
;;;;;;;;;;;;

(defn- export-toolkit-constructor [configuration toolkit-map]
  (if-let [select-expression (:select-expression toolkit-map)]
    {:export-select-expression (fn [] (select-expression :column nil :inner-join nil :where nil))
     :export-select (fn [] (db/query (select-expression :column nil :inner-join nil :where nil)))}))

(defn- document-toolkit-constructor [configuration toolkit-map]
  (let [table-name (:table_name configuration)]
    (doc/select-documents-by-table table-name)))

;;;;;;;;;;;;;;;;
;;; PIPELINE ;;;
;;;;;;;;;;;;;;;;

(defn data-toolkit-pipeline [configuration other-toolkit-map]
  (let [rule-react-on (fn [f & ks] (fn [m] (if (every? (fn [k] (some? (k configuration))) ks) (into m (f configuration m)) m)))
        sql-crud-toolkit   (rule-react-on sql-crud-toolkit-constructor :query :table_name)
        metadata-toolkit   (rule-react-on metadata-toolkit-constructor :table_name)
        supply-sql-toolkit (rule-react-on supply-sql-action-constructor :table_name)
        ;; export-sql-toolkit (rule-react-on export-toolkit-constructor :query)
        ;; document-toolkit (rule-react-on document-toolkit-constructor :table_name)
        ]
    (-> other-toolkit-map
        sql-crud-toolkit
        supply-sql-toolkit
        metadata-toolkit
        ;; export-sql-toolkit
        ;; document-toolkit
        )))
