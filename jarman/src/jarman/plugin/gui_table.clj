(ns jarman.plugin.gui-table
  (:refer-clojure :exclude [update])
  (:require
   ;; Clojure toolkit
   [clojure.string :as string]
   [clojure.java.io :as io]
   [seesaw.core :as c]
   ;; for datascript
   [clojure.set :as set]
   [datascript.core :as d]
   ;;[datascript.transit :as dt]
   ;;jarman-tools
   [jarman.logic.metadata :as db-meta]))

;; (ns jarman.plugin.gui-table
;;   (:require
;;    ;; Clojure toolkit 
;;    [clojure.string :as string]
;;    ;; Dev tools
;;    [seesaw.core :as c]
;;    [seesaw.swingx :as swingx]
;;    ;; Jarman toolkit
;;    [jarman.tools.lang :refer :all]
;;    [jarman.logic.metadata :as mt]))

(def schema
  "Description
    create schema (datoms) for db,
    schema describes the set of attributes"
  {:id                      {:db.unique :db.unique/identity}
   :table_name              {:db.unique :db.unique/identity}
   :table                   {}
   :columns                 {:db/valueType   :db.type/ref
                             :db/cardinality :db.cardinality/many
                             :db/isComponent true} 
   :column/field-qualified  {:db.unique :db.unique/identity}
   :column/foreign-keys     {:db/valueType   :db.type/ref
                             :db.unique :db.unique/identity
                             :db/cardinality :db.cardinality/one}})

(defn- id-tables
  "Description
    return map with id and table-name for converting foreign keys in func serializer-cols
  Example
  (id-tables)
  => {:seal 13 :user 2 ...}"
  [] (apply hash-map (flatten (map (fn [table-map] [(keyword (:table_name table-map))
                                                    (:id table-map)]) (db-meta/getset)))))

(defn- serializer-cols
  "Description
    Serialize structure of :columns from metadata for schema db"
  [columns]
  (vec (map (fn [column] (conj (reduce (fn [acc [k v]]
                                         (assoc acc (keyword "column" (name k))
                                                (if (= k :foreign-keys) ;; convert map-refs to id
                                                  ((first (vals (first v))) (id-tables)) (if (nil? v) [] v))))
                                       {} column))) columns)))


(def data (vec (map (fn [table-map]
                       (let [columns ((comp :columns :prop) table-map)
                             id (:id table-map)
                             f-columns (serializer-cols columns)]
                         (println columns)
                         (conj
                          {:db/id       (* -1 id)
                           :id          (:id table-map)
                           :table_name  (:table_name table-map)
                           :table       ((comp :table :prop) table-map)
                           :columns     f-columns}))) (db-meta/getset))))

(def db
  (-> (d/empty-db schema)
      (d/db-with data)))


(defn gui-table-model-columns [table-name colmn-list]
  (let
      ;; return from db model with id of columns in colmn-list [{:key :user.login, :text 5} ....]
      [id-map (reduce (fn [acc x] (if (nil? (some #{(:v x)} colmn-list)) acc (conj acc {:key (:v x) :text (:e x)}))) [] (d/datoms db :eavt))
       ;; select from db columns by id-datom [{:column/representation "Permisssion name" :column/field-qu...} ....]
       data-map (d/pull-many db [:column/foreign-keys :column/representation :column/field-qualified] (map (fn [f] (:text f))  id-map))
       ;; get from data-map foreign-keys (references), return map with table and repr to adding {:permission "id_permission"}
       refs (into {}(map (fn [x] {(keyword (:table_name (d/pull db [:table_name](:db/id (:column/foreign-keys x)))))
                                  (:column/representation x)} )(filter (fn [x] (not (empty? (:column/foreign-keys x)))) data-map)))
       ;; get from data-map representations and convert to our format [{:text "id_permission Permission name"}....]
       repr (reduce (fn [acc x] (conj acc {:text
                                           (let [col-repr (:column/representation x)
                                                 t-name (keyword (first (string/split (name  (:column/field-qualified x)) #"\.")))
                                                 n (t-name refs)]
                                             (str n (if-not (empty? n ) " ") col-repr))})) [] data-map)
       ;; in id-map change :text with id -> representations
       model-colmns (map (fn [a b] (merge a b)) id-map repr)]
    model-colmns))

(defn- gui-table-model [model-columns data-loader]
  (fn [] [:columns model-columns :rows (data-loader)]))

(defn- gui-table [table-model]
  (fn [listener-fn]
    (let [TT (seesaw.swingx/table-x :model (table-model))]
      (c/listen TT :selection (fn [e] (listener-fn (seesaw.table/value-at TT (c/selection TT)))))
      (c/config! TT :horizontal-scroll-enabled? true)
      (c/config! TT :show-grid? false)
      (c/config! TT :show-horizontal-lines? true)
      (c/scrollable TT :hscroll :as-needed :vscroll :as-needed :border nil))))

(defn create-table [configuration toolkit-map]
  (let [view (:view-columns configuration) table-name (:name configuration)]
    ;; (println "\nView table\n" view)
    (if (and view table-name)
      (let [model-columns (gui-table-model-columns table-name view)
            table-model (gui-table-model model-columns (:select toolkit-map))
            ;; x (println "\ntable-model " (table-model))
            ]
        {:table-model table-model
         :table (gui-table table-model)}))))


(gui-table-model-columns "user"
                         [:user.id :user.login :user.password :user.first_name :user.last_name :user.id_permission :permission.id :permission.permission_name :permission.configuration])
