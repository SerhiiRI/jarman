(ns jarman.logic.metadata-core
  (:require
   [clojure.data :as data]
   [clojure.string :as string]
   [jarman.config.storage :as storage]
   [jarman.config.environment :as env]
   [jarman.tools.lang :refer :all]
   [jarman.logic.connection :as db]
   ;; [datascript.core :as d]
   [jarman.logic.sql-tool
    :refer [select! update! insert!
            alter-table! create-table! delete!
            show-table-columns ssql-type-parser]])
  (:import (java.util Date)
           (java.text SimpleDateFormat)))

;;;;;;;;;;;;;;;;;;;
;;; SQL HELPERS ;;;
;;;;;;;;;;;;;;;;;;;

(defn- ^clojure.lang.PersistentList database-update-metadata-table-expression [m]
  (letfn [(serialize [m] (update m :prop #(str %)))]
    (if (:id m)
      (update! {:table_name :metadata :set (serialize (dissoc m :id)) :where [:= :id (:id m)]})
      (insert! {:table_name :metadata :column-list [:table_name :prop] :values (vals (serialize (dissoc m :id)))}))))

;; show-tables-not-meta => database-showtables-nonmeta
(defn database-showtables-nonmeta []
  (->> (db/query "SHOW TABLES")
       (map (comp second first))
       (filter (fn [s] (not (re-matches #"(?:view|metatable|meta.*)" s))))))

;; DELETE
#_(defn update-meta [metadata]
    (db/exec (update-sql-by-id-template "metadata" metadata)))

;; create-one-meta -> database-update-metadata-table
(defn database-update-metadata-table [metadata]
  {:pre [(some? (get metadata :table_name))]}
  (let [table_name (get metadata :table_name)
        selected-metadata (db/query (select! {:table_name :metadata :where [:= :table_name table_name]}))]
    (if (empty? selected-metadata)
      (-> metadata
          (database-update-metadata-table-expression)
          (db/exec))
      (-> metadata
          (assoc :id (get-in (first selected-metadata) [:id] nil))
          (database-update-metadata-table-expression)
          (db/exec)))))

;; delete-one-meta -> database-delete-metadata-by-table-name
(defn database-delete-metadata-by-table-name [table-name]
  {:pre [(or (keyword table-name) (string? table-name))]}
  (db/exec
   (delete! {:table_name :metadata
             :where [:= :table_name (name table-name)]})))

;; do-clear-meta -> database-delete-all-metadata
(defn database-delete-all-metadata []
  (db/exec (delete! {:table_name :metadata})))

;; CURRENTLY UNAVAILBLE
#_(defn do-create-meta-database []
    (doall
     (for [table (show-tables-not-meta)]
       (create-one-meta (get-meta table) table))))

;; CURRENTLY UNAVAILBLE
#_(defn do-create-meta-snapshot []
    (doall
     (for [table (show-tables-not-meta)]
       (get-meta table))))

;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; METADATA CONTAINER ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;

(defprotocol IMetadataVec
  (metadata-get [this])
  (metadata-set! [this metadata-vector])
  (metadata-reload! [this])
  (metadata-persist! [this])
  (metadata-loaded? [this]))

(deftype MetadataContainer [^{:volatile-mutable true} metadata-vec]
  IMetadataVec
  (metadata-get [this] metadata-vec)
  (metadata-set! [this new-metadata-vec]
    (set! metadata-vec new-metadata-vec))
  (metadata-loaded? [this]
    (not (empty? metadata-vec)))
  (metadata-persist! [this]
    (when (metadata-loaded? this)
      (doall (map database-update-metadata-table metadata-vec))
      true))
  (metadata-reload! [this]
    (metadata-set! this
     (->> (db/query (select! {:table_name :metadata}))
          (mapv #(try
                   (update % :prop read-string)
                   (catch Exception e
                     (throw (ex-info (format "Error parsing for `%s`" (:table_name %)) {:metadata %})))))
          (doall)))))

(def ^:private global-metadata-container (MetadataContainer. nil))

(defn update-meta [m]
  (database-update-metadata-table ))

(defn return-metadata [& tables]
  (as-> 
      (if (metadata-loaded? global-metadata-container)
        (do 
          (metadata-get global-metadata-container))
        (do
          (metadata-reload! global-metadata-container)
          (metadata-get global-metadata-container))) $
    (if (not-empty tables)
      (let [tables-names (into #{} (mapv name tables))]
        (filterv (fn [metadata] (contains? tables-names (get metadata :table_name))) $ ))
      $)))

(defn return-metadata-grouped []
  (group-by-apply
   (fn [m] (keyword (get-in m [:prop :table :field])))
   (return-metadata)
   :apply-group first))

(defn reload-metadata []
  (metadata-reload! global-metadata-container))

(comment
  (return-metadata :seal)
  (reload-metadata))

;;;;;;;;;;;;;;;;;;;;;;;
;;; Make references ;;;
;;;;;;;;;;;;;;;;;;;;;;;

(defn- add-references-to-metadata [metadata front-reference back-reference]
  (-> (fn [current added]
        (cond
          (nil? added) current
          (and (nil? current)) (vec (distinct (if (sequential? added) added [added])))
          (and (string? current)) (vec (distinct (if (sequential? added) (conj added current) [current added])))
          (and (sequential? current)) (vec (distinct (if (sequential? added) (concat current added) (conj current added))))
          :else current))
      (deep-merge-with
       metadata
       {:prop {:table {:ref {:front-references front-reference :back-references back-reference}}}})))


(defn- --recur-make-references [^clojure.lang.Atom meta-list ^String table-name & {:keys [back-ref]}]
  (if-let [[index metadata] (find-column (fn [[i m]] (= table-name (get-in m [:prop :table :field]))) (deref meta-list))]
    (let [front-refs (filter :foreign-keys (get-in metadata [:prop :columns]))]
      (if (empty? front-refs)
        (do
          (swap! meta-list
           #(update-in % [index]
             (fn [[i m]] [i (add-references-to-metadata m nil back-ref)]))))
        (do
          (swap!
           meta-list
           #(update-in % [index]
             (fn [[i m]] [i (add-references-to-metadata m (mapv :key-table front-refs) back-ref)])))
          (doseq [reference front-refs]
            (--recur-make-references meta-list (:key-table reference)
                                     :back-ref (get-in metadata [:prop :table :field]))))))))

(defn do-create-references []
  (let [meta-list (atom (vec (map-indexed #(vector %1 %2) (return-metadata))))]
    (doseq [[_ metadata] @meta-list
            :let [table-name (get-in metadata [:prop :table :field])]]
      (--recur-make-references meta-list table-name))
    @meta-list))

;;;;;;;;;;;;;;;;;;;;;
;;; OBJECT SYSTEM ;;;
;;;;;;;;;;;;;;;;;;;;;

(declare wrapp-cols-metadata-types)

(defprotocol IPrimitive
  (to-primive [this]))

(defprotocol IFieldSearch
  (find-field           [this field-name-kwd])
  (find-field-qualified [this field-name-qualified-kwd])
  (find-field-by-comp-var [this field-comp-var field-name-qualified-kwd]))

(defprotocol IField
  (return-field           [this])
  (return-field-qualified [this])
  (return-description     [this])
  (return-representation  [this])
  (return-column-type     [this])
  (return-component       [this])
  (return-default-value   [this])
  (return-private?        [this])
  (return-editable?       [this]))

(defprotocol IFieldReference
  (return-foreign-keys    [this])
  (return-key-table       [this]))

(defprotocol IColumns
  (return-columns [this])
  (return-columns-k-v [this]))

(defprotocol IColumnsExtended
  (return-columns-smpl [this])
  (return-columns-link [this])
  (return-columns-composite [this])
  (return-columns-flatten [this]))

(defprotocol IGroup
  (group                  [this m])
  (ungroup                [this m]))

(defprotocol IMetadata
  (return-table_name [this])
  (return-id [this])
  (return-prop [this])
  (return-table [this])
  (exists? [this])
  (refresh [this]))

(defrecord Field [m]
  IField
  (return-field           [this] (get (.m this) :field))
  (return-field-qualified [this] (get (.m this) :field-qualified))
  (return-description     [this] (get (.m this) :description))
  (return-representation  [this] (get (.m this) :representation))
  (return-column-type     [this] (get (.m this) :column-type))
  (return-component       [this] (get (.m this) :component))
  (return-default-value   [this] (get (.m this) :default-value))
  (return-private?        [this] (get (.m this) :private?))
  (return-editable?       [this] (get (.m this) :editable?))
  IPrimitive
  (to-primive [this] m))

(defrecord FieldLink [m]
  IField
  (return-field           [this] (get (.m this) :field))
  (return-field-qualified [this] (get (.m this) :field-qualified))
  (return-description     [this] (get (.m this) :description))
  (return-representation  [this] (get (.m this) :representation))
  (return-column-type     [this] (get (.m this) :column-type))
  (return-component       [this] (get (.m this) :component))
  (return-default-value   [this] (get (.m this) :default-value))
  (return-private?        [this] (get (.m this) :private?))
  (return-editable?       [this] (get (.m this) :editable?))

  IFieldReference
  (return-foreign-keys    [this] (get (.m this) :foreign-keys))
  (return-key-table       [this] (get (.m this) :key-table))

  IPrimitive
  (to-primive [this] m))

(defrecord FieldComposite [m group-fn ungroup-fn]

  IFieldSearch
  (find-field [this field-name-kwd]
    (first (filter (fn [field-m]
                     (= (:field field-m) field-name-kwd)) (.return-columns this))))
  
  (find-field-qualified [this field-name-qualified-kwd]
    (first (filter (fn [field-m]
                     (= (:field-qualified field-m) field-name-qualified-kwd)) (.return-columns this))))

  (find-field-by-comp-var [this field-comp-var field-name-qualified-kwd]
    (reduce (fn [acc column]
              (if (= (:constructor-var column) field-comp-var)
                (conj acc (:field column)) acc)) []
            (:columns (:m this))))

  IField
  (return-field           [this] (get (.m this) :field))
  (return-field-qualified [this] (get (.m this) :field-qualified))
  (return-description     [this] (get (.m this) :description))
  (return-representation  [this] (get (.m this) :representation))
  (return-component       [this] (get (.m this) :component))
  (return-private?        [this] (get (.m this) :private?))
  (return-editable?       [this] (get (.m this) :editable?))
  
  IColumns
  (return-columns         [this] (wrapp-cols-metadata-types (get (.m this) :columns)))
  (return-columns-k-v     [this] (group-by-apply #(.return-field-qualified %) (.return-columns this)
                                                 :apply-group first))

  IGroup
  (group                  [this data-m] ((.group-fn this) data-m))
  (ungroup                [this data-m] ((.ungroup-fn this) data-m))

  IPrimitive
  (to-primive             [this] m))

(defn isField? [^jarman.logic.metadata_core.Field e]
  (instance? jarman.logic.metadata_core.Field e))
(defn isFieldLink? [^jarman.logic.metadata_core.FieldLink e]
  (instance? jarman.logic.metadata_core.FieldLink e))
(defn isFieldComposite? [^jarman.logic.metadata_core.FieldComposite e]
  (instance? jarman.logic.metadata_core.FieldComposite e))

(defn to-field [m]
  (Field. m))
(defn to-field-link [m]
  (FieldLink. m))
(defn to-field-composite [field]
  (let [{:keys [mapp demapp var-template field-template var-list field-list]}
        (reduce (fn [a f]
                  (-> a
                      (update :field-list      conj  (:field-qualified f))
                      (update :var-list        conj  (:constructor-var f))
                      (update :var-template    into {(:field-qualified f) nil})
                      (update :field-template  into {(:constructor-var f) nil})
                      (update :mapp            into {(:field-qualified f) (:constructor-var f)})
                      (update :demapp          into {(:constructor-var f) (:field-qualified f)})))
                {:mapp {} :demapp {} :var-template {} :field-template {} :var-list [] :field-list []}
                (:columns field))
        make-mapp   (fn [e] (merge field-template (clojure.set/rename-keys (select-keys e field-list) mapp)))
        make-demapp (fn [e] (merge var-template   (clojure.set/rename-keys (select-keys e var-list) demapp)))]
    (->FieldComposite field make-mapp make-demapp)))

(defn- wrapp-cols-metadata-types [cols]
  (map (fn [c] (cond
                (contains? c :foreign-keys) (to-field-link c)
                (contains? c :columns)  (to-field-composite c)
                :else (to-field c))) cols))

(deftype TableMetadata [m]
  IFieldSearch
  (find-field [this field-name-kwd]
    (first (wrapp-cols-metadata-types
            (filter (fn [field-m]
                      (= (:field field-m) field-name-kwd)) (.return-columns-join this)))))
  (find-field-qualified [this field-name-qualified-kwd]
    (first (wrapp-cols-metadata-types
            (filter (fn [field-m]
                      (= (:field-qualified field-m) field-name-qualified-kwd)) (.return-columns-join this)))))
  (find-field-by-comp-var [this field-comp-var field-name-qualified-kwd]
    (reduce (fn [acc column] (if (= (:constructor-var column) field-comp-var)
                              (conj acc (:field column)) acc)) []
            (:columns (:m (.find-field-qualified this field-name-qualified-kwd)))))


  IColumns
  (return-columns [this]
    (vec (wrapp-cols-metadata-types (get-in (.m this) [:prop :columns] []))))
  (return-columns-k-v [this]
    (group-by-apply #(.return-field-qualified %) (.return-columns this)
                    :apply-group first))


  IColumnsExtended
  (return-columns-smpl [this]
    (vec (wrapp-cols-metadata-types (->> (get-in (.m this) [:prop :columns] [])
                                         (remove #(contains? % :key-table))
                                         (remove #(contains? % :columns))))))
  (return-columns-link [this]
    (vec (wrapp-cols-metadata-types (->> (get-in (.m this) [:prop :columns] [])
                                         (filter #(contains? % :key-table))))))
  (return-columns-composite [this]
    (vec (wrapp-cols-metadata-types (->> (get-in (.m this) [:prop :columns] [])
                                         (filter #(contains? % :columns))))))
  (return-columns-flatten [this]
    (vec (concat (.return-columns-smpl this)
                 (.return-columns-link this)
                 (mapcat #(.return-columns %) (.return-columns-composite this)))))
  
  IMetadata
  (return-prop       [this] (get (.m this) :prop nil))
  (return-id         [this] (get (.m this) :id nil))
  (return-table_name [this] (get (.m this) :table_name nil))
  (return-table      [this] (get-in (.m this) [:prop :table] nil))
  (exists? [this]
    (if (not-empty (jarman.logic.metadata-core/return-metadata (.return-table-name this)))
      true))
  (refresh [this]
    (if (.exists? this)
      (set! (.m this) (first (jarman.logic.metadata-core/return-metadata (.return-table-name this))))))
  IGroup
  (group [this m]
    (reduce
     (fn [acc field] (.group field acc))
     m (.return-columns-composite-wrapp this)))
  (ungroup [this m]
    (reduce
     (fn [acc field] (.ungroup field acc))
     m (.return-columns-composite-wrapp this))))
(defn isTableMetadata? [^jarman.logic.metadata_core.TableMetadata e]
  (instance? jarman.logic.metadata_core.TableMetadata e))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; METADATA RECUR ENGINE ;;; 
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn --get-foreight-table-by-column [metadata-colum]
  ((:field metadata-colum) (first (:foreign-keys metadata-colum))))
(defn --do-table-frontend-recursion [table-meta on-recur-action & {:keys [back-ref column-ref]}]
  (if table-meta
    (let [front-refs (filter :foreign-keys ((comp :columns :prop) table-meta))]
      (on-recur-action back-ref column-ref table-meta)
      (if (not-empty front-refs)
        (doseq [reference front-refs]
          (--do-table-frontend-recursion
           (first (return-metadata (--get-foreight-table-by-column reference)))
           on-recur-action :back-ref table-meta :column-ref reference))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; METADATA RECUR ENGINE ;;; 
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn --get-foreight-table-by-column [metadata-colum]
  ((:field metadata-colum) (first (:foreign-keys metadata-colum))))
(defn --do-table-frontend-recursion [table-meta on-recur-action & {:keys [back-ref column-ref]}]
  (if table-meta
    (let [front-refs (filter :foreign-keys ((comp :columns :prop) table-meta))]
      (on-recur-action back-ref column-ref table-meta)
      (if (not-empty front-refs)
        (doseq [reference front-refs]
          (--do-table-frontend-recursion
           (first (return-metadata (--get-foreight-table-by-column reference)))
           on-recur-action :back-ref table-meta :column-ref reference))))))

(defn model-build [meta-fields]
  (into {}
        (map (fn [f]
               (vector
                (return-field-qualified f)
                (cond
                  (isField? f) (-> f return-component :value)
                  (isFieldLink? f) nil
                  (isFieldComposite? f) (group f (model-build (return-columns f )))))) meta-fields)))

(defn convert_metadata->model
  "Example
  (convert_metadata->model :profile :seal)
    =>
    {:profile.configuration \"'{}'\",
     :seal.site {:text \"Title\", :link \"https://\"},
     :seal.datetime_of_remove nil,
     :seal.ftp_file {:file-name \"\", :file-path \"\"},
     :seal.id nil,
     :profile.id nil,
     :seal.seal_number \"\",
     :seal.db_file {:file-name \"\", :file nil},
     :seal.datetime_of_use nil,
     :profile.name \"\"}"
  [& tables]
  (let [MX (map ->TableMetadata (apply return-metadata tables))
        IDX (map #(hash-map (keyword (format "%s.id" (.return-table_name %))) nil) MX)]
    (into {}
          (concat (mapcat (fn [M] (model-build (return-columns M))) MX)
                  IDX))))

(defn convert_flattfields->model
  "Example
    (convert_flattfields->model
     {:seal.ftp_file_path \"\",
     :seal.datetime_of_remove nil,
     :seal.site_name \"Title\",
     :seal.file nil,
     :seal.ftp_file_name \"\",
     :seal.seal_number \"\",
     :seal.file_name \"\",
     :seal.datetime_of_use nil,
     :seal.site_url \"https://\"}
      :seal)
     ;; =>
       {:seal.seal_number \"\",
       :seal.datetime_of_use nil,
       :seal.datetime_of_remove nil,
       :seal.site {:text \"Title\", :link \"https://\"},
       :seal.db_file {:file-name \"\", :file nil},
       :seal.ftp_file {:file-name \"\", :file-path \"\"},
       :seal.id nil}"
  [model & tables]
  (if (empty? tables) {}
      (let [MX  (map ->TableMetadata (apply return-metadata tables))
            COLVEC (into {}
                         (concat
                          (map return-columns-k-v MX)
                          (map #(hash-map
                                 (keyword (format "%s.id" (.return-table_name %)))
                                 (to-field-link {:field :id :field-qualified (keyword (format "%s.id" (.return-table_name %)))})) MX)))]
        (reduce (fn [acc [field-qualified field-meta]]
                  (if (isFieldComposite? field-meta)
                    (assoc acc field-qualified (.group field-meta model))
                    (assoc acc field-qualified (get model field-qualified))))
                {} COLVEC))))

(defn convert_model->flattfields
  "Example
  (convert_model->flatt-fields
    {:seal.id nil,
     :seal.seal_number \"\",
     :seal.datetime_of_use nil,
     :seal.datetime_of_remove nil,
     :seal.site {:text \"Title\", :link \"https://\"},
     :seal.db_file {:file-name \"\", :file nil},
     :seal.ftp_file {:file-name \"\", :file-path \"\"}
     :profile.id 1
     :profile.name \"\"
     :profile.configuration \"\"}
    :seal)
       => {:seal.ftp_file_path \"\",
           :seal.datetime_of_remove nil,
           :seal.site_name \"Title\",
           :seal.file nil,
           :seal.ftp_file_name \"\",
           :seal.seal_number \"\",
           :seal.file_name \"\",
           :seal.datetime_of_use nil,
           :seal.site_url \"https://\"}"
  [model & tables]
  (if (empty? tables) {}
   (let [MX (map ->TableMetadata (apply return-metadata tables))
         DIC (->> MX (map return-columns-k-v) (into {}))]
     (->> model
          (map (fn [[k v]]
                 (if-let [f (get DIC k)]
                   (if (isFieldComposite? f) (ungroup f v) [k v]))))
          (into {})))))

