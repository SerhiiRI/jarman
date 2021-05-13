(ns jarman.logic.view-manager
  (:refer-clojure :exclude [update])
  (:require
   ;; Clojure toolkit 
   ;; [clojure.data :as data]
   [clojure.string :as string]
   ;; [seesaw.util :as u]
   ;; ;; Jarman toolkit
   [jarman.logic.document-manager :as doc]
   [jarman.logic.connection :as db]
   [jarman.tools.lang :refer :all :as lang]
   ;; [jarman.gui.gui-tools :refer :all :as gtool]
   ;; [jarman.resource-lib.icon-library :as ico]
   ;; [jarman.tools.swing :as stool]
   ;; [jarman.gui.gui-components :refer :all :as gcomp]
   ;; [jarman.gui.gui-calendar :as calendar]
   ;; [jarman.config.storage :as storage]
   ;; [jarman.config.environment :as env]
   [jarman.logic.sql-tool :as toolbox :include-macros true :refer :all]
   [jarman.logic.metadata :as mt])
  (:import (java.util Date)
           (java.text SimpleDateFormat)))

(defn- tf-t-f [table-field]
  (let [t-f (string/split (name table-field) #"\.")]
    (mapv keyword t-f)))

(defn- t-f-tf [table field]
  (keyword (str (name table) "." (name (name field)))))

;;; CONSTRUCTORS ;;;
(defn metadata-toolkit-constructor [configuration toolkit-map]
  (if-let [table-metadata (first (mt/getset! (:table-name configuration)))]
    {:table-meta   ((comp :table :prop) table-metadata)
     :columns-meta ((comp :columns :prop) table-metadata)}))

(defn sql-crud-toolkit-constructor [configuration toolkit-map]
  (let [rule-react-on (fn [k f] (fn [m] (if (get configuration k) (into m (f m)) m)))
        m (first (mt/getset! (:table-name configuration)))
        ;; relations (recur-find-path m)
        override-fn! (fn [kdwd sql-fn]
                       (let [override-action-expression (keyword (format "override-%s-expression" (name kdwd)))
                             override-action (keyword (format "override-%s" (name kdwd)))]
                        (fn [m] (if (and (some? (kdwd configuration)) (not= (kdwd configuration) :none))
                                 {override-action-expression (fn [e] (sql-fn ((kdwd configuration) e)))
                                  override-action (fn [e] (db/exec (sql-fn ((kdwd configuration) e))))}
                                 {}))))
        rule-insert! (rule-react-on :insert (override-fn! :insert insert!))
        rule-update! (rule-react-on :update (override-fn! :update update!))
        rule-delete! (rule-react-on :insert (override-fn! :insert delete!))
        id_column (t-f-tf (:table-name configuration) :id)
        table-name ((comp :field :table :prop) m)
        columns (map :field ((comp :columns :prop) m))
        update-expression (fn [entity] (if (id_column entity)  (update table-name :set entity :where (=-v id_column (id_column entity)))))
        insert-expression (fn [entity] (if (nil? (id_column entity)) (insert table-name :set entity)))
        delete-expression (fn [entity] (if (id_column entity) (delete table-name :where (=-v id_column (id_column entity)))))
        select-expression (fn [& {:as args}]
                            (apply (partial select-builder (:table-name configuration))
                                   (mapcat vec (into (:query configuration) args))))]
    (-> {:update-expression update-expression
         :insert-expression insert-expression
         :delete-expression delete-expression
         :select-expression select-expression
         :update (fn [e] (db/exec (update-expression e)))
         :insert (fn [e] (db/exec (insert-expression e)))
         :delete (fn [e] (db/exec (delete-expression e)))
         :select (fn [ ] (db/query (select-expression)))
         :model-id id_column}
        rule-insert!
        rule-update!
        rule-delete!
        )))

;; (let [configuration {:table-name :sealn
;;                      :name "Pushing seals"
;;                      :tables [:seal]
;;                      :view [:seal.seal_number
;;                             :seal.to_date]
;;                      :override-model [{:start-value :text-input} {:end-value :text-input}]
;;                      :insert (fn [m] {:table-name :seal
;;                                      :column-list [:seal.seal_number :seal.to_date]
;;                                      :values (mapv #(vector % (:to-date m)) (range (:start-value m) (+ (:end-value m) 1)))}) 
;;                      :update (fn [m] {:table-name :seal
;;                                      :set m}) ;;:none
;;                      :delete :none
;;                      :query {:column (as-is :seal.id :seal.seal_number :seal.to_date)}}]
;;   (keys (sql-crud-toolkit-constructor configuration {})))

(defn export-toolkit-constructor [configuration toolkit-map]
  (if-let [select-expression (:select-expression toolkit-map)]
    {:export-select-expression (fn [] (select-expression :column nil :inner-join nil :where nil))
     :export-select (fn [] (db/query (select-expression :column nil :inner-join nil :where nil))) }))

(defn document-toolkit-constructor [configuration toolkit-map]
  (let [table-name (:table-name configuration)]
    (doc/select-documents-by-table table-name)))

(defn data-toolkit-pipeline [configuration]
  (let [rule-react-on (fn [f & ks] (fn [m] (if (every? (fn [k] (some? (k configuration))) ks) (into m (f configuration m)) m)))
        sql-crud-toolkit (rule-react-on sql-crud-toolkit-constructor :query)
        metadata-toolkit (rule-react-on metadata-toolkit-constructor :table-name)
        export-sql-toolkit (rule-react-on export-toolkit-constructor :query)
        document-toolkit (rule-react-on document-toolkit-constructor :table-name)]
    (-> {} sql-crud-toolkit metadata-toolkit export-sql-toolkit document-toolkit)))

(def ^:private views-configuration+toolkit (atom {}))
(defn views-configuration+toolkit-get []
  @views-configuration+toolkit)
(defn views-configuration+toolkit-set [keyword-table-name plugin-name plugin-configuration data-toolkit]
  {:pre [(keyword? keyword-table-name)]}
  (swap! views-configuration+toolkit
         (fn [m] (assoc-in m [keyword-table-name plugin-name]
                          {:configuration plugin-configuration
                           :data-toolkit data-toolkit}))) nil)

(defmacro defview [table-model-name & body]
  (let [configurations
        (reduce into 
                (for [form body]
                  (if (sequential? form)
                    `{~(keyword (first form)) (hash-map ~@(rest form) :table-name ~(keyword table-model-name))})))]
    `(do ~@(for [form body :let [f (first form)]]
             `(let [cfg# ~configurations
                    ktable# ~(keyword table-model-name)
                    kplugin# ~(keyword f)
                    plugin-configuration# (get cfg# kplugin#)
                    plugin-data-toolkit#  (data-toolkit-pipeline (get cfg# kplugin#))]
                (views-configuration+toolkit-set ktable#
                                                 kplugin#
                                                 plugin-configuration#
                                                 plugin-data-toolkit#)
                (~f [ktable# kplugin#] ~'views-configuration+toolkit-get))) nil)))

(defmacro defview-debug [table-model-name & body]
  (let [configurations
        (reduce into 
                (for [form body]
                  (if (sequential? form)
                    (let [s `(hash-map ~@(rest form) :table-name ~(keyword table-model-name))]
                      `{~(keyword (first form)) {:configuration ~s
                                                 :data-toolkit (data-toolkit-pipeline ~s)}}))))]
    configurations))

(defn as-is [& column-list]
  (map #(if (keyword? %) {% %} %) column-list))

(defview-debug permission
  (plug/jarman-table
   :name "Table"
   :place nil
   :tables [:permission]
   :view   [:permission.permission_name]
   :query   {:column (as-is :permission.id :permission.permission_name :permission.configuration)}))


