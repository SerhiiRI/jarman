(ns jarman.logic.metadata
  (:require
   [clojure.data :as data]
   [clojure.string :as string]
   [jarman.config.storage :as storage]
   [jarman.config.environment :as env]
   [jarman.tools.lang :refer :all]
   [jarman.logic.connection :as db]
   [datascript.core :as d]
   [jarman.logic.sql-tool :refer [select! update! insert!
                                  alter-table! create-table! delete!
                                  show-table-columns ssql-type-parser]])
  (:import (java.util Date)
           (java.text SimpleDateFormat)))

;;;;;;;;;;;;;;;;;;;;;;
;;; METADATA TYPES ;;;
;;;;;;;;;;;;;;;;;;;;;;

(defrecord Link     [text link])
(defn isLink?       [^jarman.logic.metadata.Link e] (instance? jarman.logic.metadata.Link e))
(defn link          [text link] (->Link text link))

(defrecord File     [file-name file])
(defn isFile?       [^jarman.logic.metadata.File e] (instance? jarman.logic.metadata.File e))
(defn file          [file-name file] {:pre [(every? string? (list file-name file))]} (->File file-name file))

(defrecord FtpFile  [file-name file-path])
(defn isFtpFile?    [^jarman.logic.metadata.FtpFile e] (instance? jarman.logic.metadata.FtpFile e))
(defn ftp-file      [file-name file-path] {:pre [(every? string? (list file-name file-path))]} (->FtpFile file-name file-path))

(def component-list  [isFile? isLink? isFtpFile?])
(def component-files [isFile? isFtpFile?])

(defn isComponent? [val] (some #(% val) component-list))
(defn isComponentFiles? [val] (some #(% val) component-files))

;;;;;;;;;;;;;;;;;;;;;;
;;; CONFIGURATIONS ;;; 
;;;;;;;;;;;;;;;;;;;;;;

(def ^{:dynamic true :private true} *not-allowed-to-edition-tables* ["user" "permission"])
(def column-type-data       :date)
(def column-type-time       :time)
(def column-type-datatime   :datetime)
(def column-type-linking    :link)
(def column-type-number     :number)
(def column-type-boolean    :boolean)
(def column-type-textarea   :textarea)
(def column-type-prop       :prop)
(def column-type-floated    :float)
(def column-type-input      :text)
(def column-type-blob       :blob)
(def column-comp-url        :comp-url)
(def column-comp-file       :comp-file)
(def column-comp-ftp-file   :comp-ftp)
(def column-type-nil nil)
(def ^:dynamic *meta-column-type-list* [column-type-data
                                        column-type-time
                                        column-type-datatime
                                        column-type-linking
                                        column-type-number
                                        column-type-boolean
                                        column-type-textarea
                                        column-type-prop
                                        column-type-floated
                                        column-type-input
                                        column-type-blob
                                        column-comp-url
                                        column-comp-file
                                        column-comp-ftp-file
                                        column-type-nil])
;;;;;;;;;;;;;;;;;;;;;;
;;; RULE FILTRATOR ;;;
;;;;;;;;;;;;;;;;;;;;;;

;;; pattern table matching configuration
;;; mean that column with this name never
;;; being added to metadata information.
;;; The same with table in *meta-rules*
(def ^{:dynamic true :private true} *id-collumn-rules* ["id", "id*"])
(def ^{:dynamic true :private true} *meta-rules* ["metatable" "meta*"])

(defn- not-allowed-rules
  "Description:
    The function do filter on `col` list, selected only that string elements, which not allowed by `rule-spec`.

  Rule Spec:
    Rule spec is simple string with simple declaration. He serve three type of rule:
    - 'metadata' (whole word)
    - 'meta*' (words, wich started on \"meta\")
    - '*data' (words, wich end on \"*data\")

  Example using:
    ;; in case if `col` is list, not string - return list of good patterns
    (not-allowed-rules [\"dupa\" \"_pri*\"] [\"dupa\" \"_PRI\" \"_Private\" \"something\"])
      ;;=> (\"something\")
    (not-allowed-rules \"_pri*\" [\"dupa\" \"_PRI\" \"_Private\" \"something\"])
      ;;=> (\"something\" \"dupa\")
  
    ;; in case if `col` is string return boolean 
    (not-allowed-rules [\"dupa\" \"_pri*\"] \"lala\")
      ;;=> true
    (not-allowed-rules \"_pri*\" \"_PRIVATE\" )
      ;;=> false
    
  See related:
    (`jarman.logic.metadata/allowed-rules`)"
  [rule-spec col]
  (let [rule-spec (if (string? rule-spec) [rule-spec] rule-spec)
        f-comp (fn [p] (condp = (.indexOf (seq p) \*)
                         (dec (count p)) #(not= (butlast p) (take (dec (count p)) (string/lower-case %)))
                         0               #(not= (drop 1 p) (take-last (dec (count p)) (string/lower-case %)))
                         #(not= p %)))
        preds (map f-comp rule-spec)]
    (if (string? col) (reduce (fn [a p?] (and a (p? col))) true preds)
        (filter (fn [s] (reduce (fn [a p?] (and a (p? s))) true preds)) col))))

(defn- allowed-rules
  "Description:
    The function do filter on `col` list, selected only that string elements, which allowed by `rule-spec`.

  Rule Spec:
    Rule spec is simple string with simple declaration. He serve three type of rule:
    - 'metadata' (whole word)
    - 'meta*' (words, wich started on \"meta\")
    - '*data' (words, wich end on \"*data\")

  Example using:
    ;; in case if `col` is list, not string - return list of good patterns
    (allowed-rules [\"dupa\" \"_pri*\"] [\"dupa\" \"_PRI\" \"_Private\" \"something\"]
      ;;=> (\"dupa\" \"_PRI\" \"_Private\")
    (allowed-rules \"_pri*\" [\"dupa\" \"_PRI\" \"_Private\" \"something\"])
      ;;=> (\"_PRI\" \"_Private\")

    ;; in case if `col` is string return boolean 
    (allowed-rules [\"dupa\" \"_pri*\"] \"lala\")
      ;;=> false
    (allowed-rules \"_pri*\" \"_PRIVATE\" )
      ;;=> true

  See related:
    (`jarman.logic.metadata/not-allowed-rules`)"
  [rule-spec col]
  (let [rule-spec (if (string? rule-spec) [rule-spec] rule-spec)
        f-comp (fn [p] (condp = (.indexOf (seq p) \*)
                         (dec (count p)) #(= (butlast p) (take (dec (count p)) (string/lower-case %)))
                         0               #(= (drop 1 p) (take-last (dec (count p)) (string/lower-case %)))
                         #(= p %)))
        preds (map f-comp rule-spec)]
    (if (string? col) (reduce (fn [a p?] (or a (p? col))) false preds)
        (filter (fn [s] (reduce (fn [a p?] (or a (p? s))) false preds)) col))))

(defn- is-id-col? [col-name]
  (let [col (string/lower-case col-name)]
    (or (= col "id")
        (= (take 2 col) '(\i \d)))))

(defn- is-not-id-col? [col-name]
  (not (is-id-col? col-name)))

(defn- get-component-group-by-type [column-field-spec]
  (let [ctype  (re-find #"[a-zA-Z]*" (string/lower-case (:type column-field-spec)))
        cfield (:field column-field-spec)
        in?   (fn [col x] (if (string? col) (= x col) (some #(= % x) col)))]
    (if (not-empty ctype)
      (if (not-empty (allowed-rules *id-collumn-rules* [cfield])) [column-type-linking] ;; l - mean linking is linking column 
          (condp in? ctype
            "date"        [column-type-data column-type-datatime column-type-input] ;; datetime
            "time"        [column-type-time column-type-input] ;; only time
            "datetime"    [column-type-datatime column-type-data column-type-input] ;; datatime
            ["smallint"
             "mediumint"
             "int"
             "integer"
             "bigint"]    [column-type-number column-type-input] ;; n - mean simple number input
            ["double"
             "float"
             "real"]      [column-type-floated column-type-input] ;; f - mean floated point number
            ["tinyint"
             "bool"
             "boolean"]   [column-type-boolean column-type-number column-type-input] ;; b - mean boolean
            ["tinytext"
             "text"
             "mediumtext"
             "longtext"
             "json"]      [column-type-textarea] ;; a - mean area, text area
            "varchar"     [column-type-input] ;; i - mean simple text input
            nil)))))

(defn- get-table-meta
  "Description:
    Get meta information about table by hame, and construct meta information to GUI

  Template of returning meta:
  {
  :field - table name without modifing
  :representation \"table\" :private? true :scallable? false :linker? true}
  :representation - table name wich would be viewed for user in GUI, must b readable.
  :private? - if is true, than user can be edit this table
  :scallable? - if is set on true, user may add fields to datatable.
  :linker? - flag inform that table used by DB engine mechanizm. (DO NOT EDIT THAT FIELD)"
  [t-name]
  {:pre [(string? t-name) (not-empty t-name)]}
  (let [tspec (last (string/split t-name #"_"))
        in?   (fn [col x] (if (string? col) (= x col) (some #(= % x) col)))]
    (condp in? tspec
      ["lk" "link" "links"] {:field t-name
                             :representation t-name
                             :is-system? true
                             :is-linker? true
                             :allow-modifing? false
                             :allow-deleting? false
                             :allow-linking? false}
      {:field t-name
       :representation t-name
       :is-system? false
       :is-linker? false
       :description nil
       :allow-modifing? true
       :allow-deleting? true
       :allow-linking? true})))

;; (db/query (show-table-columns :user))
;; {:field "id", :type "bigint(20) unsigned", :null "NO", :key "PRI", :default nil, :extra "auto_increment"}
;; {:field "login", :type "varchar(100)", :null "NO", :key "", :default nil, :extra ""}
;; {:field "password", :type "varchar(100)", :null "NO", :key "", :default nil, :extra ""}
;; {:field "first_name", :type "varchar(100)", :null "NO", :key "", :default nil, :extra ""}
;; {:field "last_name", :type "varchar(100)", :null "NO", :key "", :default nil, :extra ""}
;; {:field "id_permission", :type "bigint(120) unsigned", :null "NO", :key "MUL", :default nil, :extra ""}

(defn- key-referense [k-column]
  (re-matches #"id_(.*)" (name k-column)))

(defn- create-column-type-meta [column-type]
  (let [meta-type (ssql-type-parser (:type column-type))
        meta-default
        (cond
          (= "YES" (:null column-type))
          [:default :null]
          (and (= "NO" (:null column-type)) (some? (:default column-type)))
          [:nnull :default (:default column-type)]
          (= "NO" (:null column-type))
          [:nnull])]
    (vec (concat meta-type meta-default))))

(defn- create-on-delete-on-update-action
  "Description
    Return SSQL on-update/-delete structure 

  Example
    (create-on-delete-on-update-action \" ON DELETE CASCADE ON UPDATE CASCADE\")
     ;; => {:delete :cascade :update :cascade}
    (create-on-delete-on-update-action \"ON DELETE SET NULL\")
     ;; => nil"
  [line]
  (let [action-to (fn [x] (case x "DELETE" :delete "UPDATE" :update))
        mode-to   (fn [x] (case x "CASCADE" :cascade "RESTRICT" :restrict "SET NULL" :null "NO ACTION" :no-action "SET DEFAULT" :default))]
    (let [on-u-d
          (filter some?
                  (for [pattern (rest (clojure.string/split line #"\s*ON\s*"))]
                    (if-let [[_ action prop] (re-find #"(DELETE|UPDATE)\s+(CASCADE|SET NULL|NO ACTION|RESTRICT|SET DEFAULT)" pattern)]
                      [(action-to action) (mode-to prop)])))]
      (if-not (empty? on-u-d) [(into {} on-u-d)]))))

(defn parse-constrants [s]
  (if-let [[_ column-name related-table actions]
           (re-matches #"CONSTRAINT\s`[\w_]*`\sFOREIGN\sKEY\s\(`([\w_]*)`\)\sREFERENCES\s`([\w_]*)`\s\(`[\w_]+`\)(.+)?" s)]
    (if actions
      {column-name (vec (concat [{(keyword column-name) (keyword related-table)}] (create-on-delete-on-update-action actions)))}
      {column-name (vector {(keyword column-name) (keyword related-table)})})))

;; (defn- get-meta-constrants [table]
;;   (let [table (name table)]
;;     (let [a (-> (db/query (format "show create table %s" table))
;;                 first seq second second (clojure.string/split #"\n"))]
;;       (if-let [c (->> a (map string/trim) (filter #(string/starts-with? % "CONSTRAINT")) first)]
;;         (parse-constrants c)))))

;; (first (getset! :repair_contract))
;; (db/query (format "show create table %s" "repair_contract"))
;; (println "CREATE TABLE `repair_contract` (\n  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,\n  `id_cache_register` bigint(20) unsigned DEFAULT NULL,\n  `id_old_seal` bigint(20) unsigned DEFAULT NULL,\n  `id_new_seal` bigint(20) unsigned DEFAULT NULL,\n  `repair_date` date DEFAULT NULL,\n  `cause_of_removing_seal` mediumtext DEFAULT NULL,\n  `tech_problem_description` mediumtext DEFAULT NULL,\n  `tech_problem_type` varchar(120) DEFAULT NULL,\n  `cache_register_register_date` date DEFAULT NULL,\n  PRIMARY KEY (`id`),\n  KEY `repair_contract29594` (`id_cache_register`),\n  KEY `repair_contract29595` (`id_old_seal`),\n  KEY `repair_contract29596` (`id_new_seal`),\n  CONSTRAINT `repair_contract29594` FOREIGN KEY (`id_cache_register`) REFERENCES `cache_register` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,\n  CONSTRAINT `repair_contract29595` FOREIGN KEY (`id_old_seal`) REFERENCES `seal` (`id`) ON DELETE SET NULL ON UPDATE SET NULL,\n  CONSTRAINT `repair_contract29596` FOREIGN KEY (`id_new_seal`) REFERENCES `seal` (`id`) ON DELETE SET NULL ON UPDATE SET NULL\n) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4")

(defn- get-meta-constrants [table]
  (let [table (name table)
        ;; In return map from sql server we get something
        ;; like {.. :create table "Create ..}, where 
        ;; ':create table' is key with !SPACE!, and you cannot
        ;; get this in simple way, then do 'create table' from
        ;; string convertation. In clojure level is really may sense
        create-table-sql-accesor (comp (keyword "create table") first)
        sql-create-table
        (-> (db/query (format "show create table %s" table))
            (create-table-sql-accesor)
            (string/split #"\n"))]
    (->> sql-create-table
         (map string/trim)
         (filter #(string/starts-with? % "CONSTRAINT"))
         (map parse-constrants)
         (reduce into))))

(defn- get-table-field-meta [table-name foreign-keys column-field-spec]
  (let [tfield (:field column-field-spec)
        ttype (create-column-type-meta column-field-spec)
        ;; set_key (fn [m] (if-let [[_ table] (re-matches #"id_(.*)" tfield)]
        ;;                  (assoc m :key-table table) m))
        set_key
        (fn [m] (if-let [f-key (get foreign-keys tfield nil)]
                 (assoc m :key-table (get (first f-key) (keyword tfield))) m))
        set_foreign-keys
        (fn [m] (if-let [f-key (get foreign-keys tfield nil)]
                 (assoc m :foreign-keys f-key) m))
        in?   (fn [col x] (if (string? col) (= x col) (some #(= % x) col)))]
    (-> {:field (keyword tfield)
         :field-qualified (keyword (str (name table-name) "." (name tfield)))
         :representation tfield
         :description nil
         :component-type (get-component-group-by-type column-field-spec)
         :default-value nil
         :column-type ttype
         :private? false
         :editable? true}
        set_key
        set_foreign-keys)))

;;  :column-type [:bigint-120-unsigned :nnull]
;;  :foreign-keys [{:id_permission :permission} {:delete :cascade :update :cascade}]
;;  :component-type ["l"]
;;  :representation "id_permission"
;;  :key-table "permission"}

;; [{:description nil
;;   :private? false :editable? true
;;   :field "id_point_of_sale_group"
;;   :column-type [:bigint-20-unsigned :default :null]
;;   :foreign-keys [{:id_point_of_sale_group :point_of_sale_group} {:delete :cascade, :update :cascade}]
;;   :component-type ["l"]
;;   :representation "id_point_of_sale_group"
;;   :key-table "point_of_sale_group"}
;;  {:description nil
;;   :private? false
;;   :editable? true
;;   :field "id_point_of_sale"
;;   :column-type [:bigint-20-unsigned :default :null]
;;   :foreign-keys [{:id_point_of_sale :point_of_sale}]
;;   :component-type ["l"]
;;   :representation "id_point_of_sale" :key-table "point_of_sale"}]

;; (create-table :user
;;               :columns [{:login [:varchar-100 :nnull]}
;;                         {:password [:varchar-100 :nnull]}
;;                         {:first_name [:varchar-100 :nnull]}
;;                         {:last_name [:varchar-100 :nnull]}
;;                         {:id_permission [:bigint-120-unsigned :nnull]}]
;;               :foreign-keys [{:id_permission :permission} {:delete :cascade :update :cascade}])
;; (def point_of_sale_group_links
;;   (create-table :point_of_sale_group_links
;;                 :columns [{:id_point_of_sale_group [:bigint-20-unsigned :default :null]}
;;                           {:id_point_of_sale [:bigint-20-unsigned :default :null]}]
;;                 :foreign-keys [[{:id_point_of_sale_group :point_of_sale_group} {:delete :cascade :update :cascade}]
;;                                [{:id_point_of_sale :point_of_sale}]]))

(defn- get-meta [table-name]
  (let [all-columns  (db/query (show-table-columns table-name))
        all-constrants (get-meta-constrants table-name)
        not-id-columns (filter #(not= "id" (:field %)) all-columns)]
    {:id nil
     :table table-name
     :prop {:table (get-table-meta table-name)
            :columns (vec (map (partial get-table-field-meta table-name all-constrants) not-id-columns))}}))

(defn- ^clojure.lang.PersistentList update-sql-by-id-template
  [table m]
  (letfn [(serialize [m] (update m :prop #(str %)))]
    (if (:id m)
      (update! {:table_name table :set (serialize (dissoc m :id)) :where [:= :id (:id m)]})
      (insert! {:table_name table
                :column-list [:table_name :prop]
                :values (vals (serialize (dissoc m :id)))}))))

(defn show-tables-not-meta []
  (not-allowed-rules ["view" "metatable" "meta*"] (map (comp second first) (db/query "SHOW TABLES"))))

(defn update-meta [metadata]
  (db/exec (update-sql-by-id-template "metadata" metadata)))

(defn create-one-meta [metadata table-name]
  (let [meta (db/query (select! {:table_name :metadata :where [:= :table_name table-name]}))]
    (if (empty? meta)
      (db/exec (update-sql-by-id-template "metadata" metadata))
      (let [metadata (assoc-in metadata [:id] (get-in (first meta) [:id] nil))]
       (db/exec (update-sql-by-id-template "metadata" metadata))))))

(defn do-create-meta-database []
  (doall
   (for [table (show-tables-not-meta)]
     (create-one-meta (get-meta table) table))))

(defn do-create-meta-snapshot []
  (doall
   (for [table (show-tables-not-meta)]
     (get-meta table))))

(defn delete-one-meta [table-name]
  {:pre [(string? table-name)]}
  (db/exec (delete! {:table_name :metadata
                     :where [:= :table_name table-name]})))

(defn do-clear-meta [& body]
  {:pre [(every? string? body)]}
  (db/exec (delete! {:table_name :metadata})))

(def  ^:private --loaded-metadata (ref nil))
(defn ^:private swapp-metadata [metadata-list]
  (dosync (ref-set --loaded-metadata metadata-list)))

;;;;;;;;;;;;;;;;;;;;;
;;; DB datascript ;;;
;;;;;;;;;;;;;;;;;;;;;

(defn- id-tables
  "Description
    return map with id and table-name for converting foreign keys in func serializer-cols
  Example
  (id-tables)
  => {:seal 13 :user 2 ...}"
  [metadata]
  (apply hash-map (flatten (map (fn [table-map] [(keyword (:table_name table-map))
                                                (:id table-map)]) metadata))))

(defn- serializer-cols
  "Description
    Serialize structure of :columns from metadata for schema db"
  [columns metadata]
  (vec (map (fn [column] (conj (reduce (fn [acc [k v]]
                                         (assoc acc (keyword "column" (name k))
                                                (if (= k :foreign-keys) ;; convert map-refs to id
                                                  ((first (vals (first v))) (id-tables metadata)) (if (nil? v) [] v))))
                                       {} column))) columns)))

(defn generate-data [metadata]
  (vec (map (fn [table-map]
              (let [columns ((comp :columns :prop) table-map)
                    id (:id table-map)
                    f-columns (serializer-cols columns metadata)]                                            
                (conj
                 {:db/id       (* -1 id)
                  :id          (:id table-map)
                  :table_name  (:table_name table-map)
                  :table       ((comp :table :prop) table-map)
                  :columns     f-columns}))) metadata)))

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
   :column/table_name       {:db.unique :db.unique/identity}
   :column/foreign-keys     {:db/valueType   :db.type/ref
                             :db.unique :db.unique/identity
                             :db/cardinality :db.cardinality/one}})

(def db (d/create-conn schema))

(defn db-datoms [data]
  (def db (d/create-conn schema))
  (d/transact! db (generate-data data)))

;;;;;;;;;;;;;;;;;;;;;;;;
;;; back to metadata ;;;
;;;;;;;;;;;;;;;;;;;;;;;;

;;; TODO change function name from `getset` to `do-metadata-load`, also remove args
(defn getset
  "get metadate deserialized information for specified tables.
  Example 
    (getset \"user\") ;=> [{:id 1 :table...}...]"
  [& tables]
  (let [metadata
        (mapv (fn [meta] (clojure.core/update meta :prop read-string))
              (db/query
               (if (empty? tables)
                 (select! {:table_name :metadata})
                 (select! {:table_name :metadata
                           :where (vec (concat [:or] (mapv (fn [x] [:= :table_name (name x)]) tables)))}))))]
    (db-datoms metadata)
    (if (empty? tables)
      (do (swapp-metadata metadata) metadata)
      metadata)))

(defn getset! [& tables]
  (when-not @--loaded-metadata
    (swapp-metadata (getset)))
  (if-not tables @--loaded-metadata
          (let [tables (map name tables)]
            (vec (filter #(in? tables (:table_name %)) @--loaded-metadata)))))



;;; Make references
;;;
;;; DEPRECATED
;;;
#_(defn- add-references-to-metadata [metadata front-reference back-reference]
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

#_(defn- --recur-make-references [meta-list table-name & {:keys [back-ref]}]
  (if-let [index-metadata (find-column #(= ((comp :field :table :prop) (second %)) table-name) (deref meta-list))]
    (let [front-refs (filter :foreign-keys ((comp :columns :prop) (second index-metadata)))]
      (if (empty? front-refs)
        (do
          (dosync (alter meta-list
                         (fn [mx] (update-in mx [(first index-metadata)]
                                             (fn [[i m]]
                                               [i (add-references-to-metadata m nil back-ref)]))))))
        (do
          (dosync (alter meta-list
                         (fn [mx] (update-in mx [(first index-metadata)]
                                             (fn [[i m]]
                                               [i (add-references-to-metadata m (mapv :key-table front-refs) back-ref)])))))
          (doseq [reference front-refs]
            (--recur-make-references meta-list (:key-table reference)
                                     :back-ref ((comp :field :table :prop) (second index-metadata)))))))))

#_(defn do-create-references []
  (let [meta-list (ref (vec (map-indexed #(vector %1 %2) (getset))))]
    (doseq [m @meta-list
            :let [table (:table (second m))]]
      (--recur-make-references meta-list table))
    @meta-list
    (doseq [[i metadata] @meta-list]
      (db/exec (update-sql-by-id-template "metadata" metadata)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; TABLE-MAP VALIDATOR ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def valid-output-lang
  {:eng {:value-not-exit-in-table "Value not exist in table-map"
         :value-not-valid-on-p "Value '%s' not valid by predicate '%s'"
         :value-not-valid-on-eq "Value '%s' not valid on '%s' equalization pattern"
         :value-not-in-allow-list "Value '%s' not from allowed list '%s'"
         :value-not-valid-on-re "Value '%s' not valid on regexp '%s' pattern"}
   :pl {:value-not-exit-in-table "Wartoďż˝ďż˝ nie istanieje w tabeli"
        :value-not-valid-on-p "Value '%s' not valid by predicate '%s'"
        :value-not-valid-on-eq "Value '%s' not valid on '%s' equalization pattern"
        :value-not-in-allow-list "Value '%s' not from allowed list '%s'"
        :value-not-valid-on-re "Value '%s' not valid on regexp '%s' pattern"}
   :ua {:value-not-exit-in-table "Value not exist in table-map"
        :value-not-valid-on-p "Value '%s' not valid by predicate '%s'"
        :value-not-valid-on-eq "Value '%s' not valid on '%s' equalization pattern"
        :value-not-in-allow-list "Value '%s' not from allowed list '%s'"
        :value-not-valid-on-re "Value '%s' not valid on regexp '%s' pattern"}})

(def valid-output (:eng valid-output-lang))
(def ^:dynamic not-valid-string-f
  "Lambda for working with validator output map

  Example of argument
   {:path [:some :path]
    :msg \"Error message\"}"
  (fn [m] (println m)))

(defmacro ^{:private true} >>=
  ([x] x)
  ([x f]
   `(let [x# (~f ~x)] (if (nil? x#) nil x#)))
  ([x f & f-list]
   `(let [x# (~f ~x)]
      (if (nil? x#) nil
          (>>= x# ~@f-list)))))
(defmacro ^{:private true} isset? [m key-path & [key-path-preview]]
  `(if (nil? (get-in ~m [~@key-path] nil))
     (do (not-valid-string-f {:path (vec (if ~key-path-preview ~key-path-preview ~key-path))
                              :message (format "Value not exist in table-map")}) false)
     true))
(defmacro ^{:private true} fpattern? [f-pattern msg m key-path & [key-path-preview]]
  `(let [value# (get-in ~m [~@key-path] nil)
         path# (if ~key-path-preview ~key-path-preview ~key-path)]
     (if (~f-pattern value#) true
         (do (not-valid-string-f {:path path# :message (format "Value '%s' not valid by predicate '%s'" (str value#) ~msg)}) false))))
(defmacro ^{:private true} ispattern? [match m key-path & [key-path-preview]]
  `(let [value# (get-in ~m [~@key-path] nil)
         path# (if ~key-path-preview ~key-path-preview ~key-path)]
     (if (nil? value#) false
         (if (= value# ~match) true
             (do (not-valid-string-f
                  {:path path# :message (format "Value '%s' not valid on '%s' equalization pattern"  (str value#) (str ~match))}) false)))))
(defmacro ^{:private true} inpattern? [match-list m key-path & [key-path-preview]]
  `(let [value# (get-in ~m [~@key-path] nil)
         path# (if ~key-path-preview ~key-path-preview ~key-path)]
     (if (nil? value#) false
         (if (in? [~@match-list] value#) true
             (do (not-valid-string-f {:path path# :message (format "Value '%s' not from allowed list '%s'"  (str value#) (str [~@match-list]))}) false)))))
(defmacro ^{:private true} repattern? [re m key-path & [key-path-preview]]
  `(let [value# (get-in ~m [~@key-path] nil)
         path# (if ~key-path-preview ~key-path-preview ~key-path)]
     (if (and (not (nil? value#)) (re-find ~re value#))
       true
       (do (not-valid-string-f {:path path# :message (format "Value '%s' not valid on regexp '%s' pattern" (str value#) (str ~re))}) false))))

(defmacro ^{:private true} do-and [& body]
  `(do
     (do ~@body)
     (binding [~'not-valid-string-f (partial #'identity)]
       (and ~@body))))

(defn- verify-table-metadata [m]
  (do-and
   (isset? m [:table])
   (isset? m [:prop :table :representation])
   (isset? m [:prop :table :is-system?])
   (isset? m [:prop :table :is-linker?])
   (isset? m [:prop :table :allow-modifing?])
   (isset? m [:prop :table :allow-deleting?])
   (isset? m [:prop :table :allow-linking?])
   (repattern? #"^[a-z_]{3,}$" m [:table])
   (repattern? #"^[\w\d\s]+$" m [:prop :table :representation])
   (inpattern? [true false] m [:prop :table :is-system?])
   (inpattern? [true false] m [:prop :table :is-linker?])
   (inpattern? [true false] m [:prop :table :allow-modifing?])
   (inpattern? [true false] m [:prop :table :allow-deleting?])
   (inpattern? [true false] m [:prop :table :allow-linking?])))

(defn- verify-column-metadata [p m]
  (do-and
   (isset? m [:field] (conj p :field))
   (isset? m [:field-qualified] (conj p :field-qualified))
   (isset? m [:representation] (conj p :representation))
   (isset? m [:column-type] (conj p :column-type))
   (isset? m [:component-type] (conj p :component-type))
   (isset? m [:default-value] (conj p :default-value))
   (isset? m [:private?] (conj p :private?))
   (isset? m [:editable?] (conj p :editable?))
   (repattern? #"^[a-z_]{3,}$" m [:field] (conj p :field))
   (repattern? #"^[\w\d\s]+$" m [:representation] (conj p :representation))
   (fpattern? #(every? (fn [cols] (in? *meta-column-type-list* cols)) %)
              (format "component-type not in allowed %s" *meta-column-type-list*) m [:component-type] (conj p :component-type))
   (inpattern? [true false] m [:private?] (conj p :private?))
   (inpattern? [true false] m [:editable?] (conj p :editable?))
   (fpattern? #(or (string? %) (nil? %)) "string? || nil?" m [:description] (conj p :description))))

;;; validators ;;;
(defn- validate-metadata-table [m]
  (verify-table-metadata m))

(defn- validate-metadata-columns [m]
  (let [fields (get-in m [:prop :columns] [])]
    (if (empty? fields)
      (not-valid-string-f "Table has empty fields list")
      (let [i-m-col (map-indexed vector fields)]
        (every? identity (map (fn [[index m-field]]
                                ;; (println [index m-field])
                                (verify-column-metadata [:prop :columns index] m-field))
                              i-m-col))))))

(defn- validate-metadata-column
  ([m-field]
   (verify-column-metadata [] m-field))
  ([path m-field]
   (verify-column-metadata path m-field)))

(defn- validate-metadata-all [m]
  (let [is-valid-table  (validate-metadata-table m)
        is-valid-column (validate-metadata-columns m)]
    (and is-valid-column is-valid-table)))

(defn- create-validator [validator]
  (fn [m-subject]
    (let [string-buffer (atom [])]
      (binding [not-valid-string-f #(swap! string-buffer (fn [buffer] (conj buffer %)))]
        (let [valid? (validator m-subject)
              output @string-buffer] {:valid? valid? :output output})))))

(def validate-all
  "Description
    Validate table map structure 

  Example
    (validate-all
     {:id 30, :table \"user\", :prop
      {:table {:representation \"us er\", :is-system? :true, :is-linker? false, :allow-modifing? :true, :allow-deleting? true, :allow-linking? true},
      :columns
      [{:field \"login\", :representation \"login\", :description nil, :component-type true, :column-type \"varchar(100)\", :private? false, :editable? :true}
       {:field \"password\", :representation \"password\", :description nil, :component-type \"i\", :column-type \"varchar(100)\", :private? false, :editable? true}
       {:field \"first_name\", :representation \"first_name\", :description nil, :component-type \"i\", :column-type \"varchar(100)\", :private? false, :editable? true}
       {:field \"last_name\", :representation \"last_name\", :description nil, :component-type \"--\", :column-type \"varchar(100)\", :private? false, :editable? true}
       {:field \"id  _permission\", :representation \"id_permission\", :description 123,
        :component-type \"l\", :column-type \"bigint(120) unsigned\", :private? false,
        :editable? true, :key-table \"permission\"}]}})
     ;;=> 
      {:valid? false,
       :output [{:path [:prop :table :is-system?], :message \"Value ':true' not from allowed list '[true false]'\"}
                {:path [:prop :table :allow-modifing?], :message \"Value ':true' not from allowed list '[true false]'\"}
                {:path [:prop :columns 0 :component-type], :message \"Value 'true' not from allowed list '[\\\"d\\\" ...]'\"}
                {:path [:prop :columns 0 :editable?], :message \"Value ':true' not from allowed list '[true false]'\"}
                {:path [:prop :columns 3 :component-type], :message \"Value '--' not from allowed list '[\\\"d\\\"...]'\"}
                {:path [:prop :columns 4 :field], :message \"Value 'id  _permission' not valid on regexp '^[a-z_]{3,}$' pattern\"}
                {:path [:prop :columns 4 :description], :message \"Value '123' not valid by predicate 'string? || nil?'\"}]}"
  (create-validator #'validate-metadata-all))

(def validate-table
  "Description
    Validate only table meta informations

  Example
    (validate-table
     {:id 30, :table \"user\", :prop
      {:table {:representation \"us er\", :is-system? :true, :is-linker? false, :allow-modifing? :true, :allow-deleting? true, :allow-linking? true},
      :columns
       [{:field \"login\", :representation \"login\", :description nil, :component-type true, :column-type \"varchar(100)\", :private? false, :editable? :true}
        {:field \"password\", :representation \"password\", :description nil, :component-type \"i\", :column-type \"varchar(100)\", :private? false, :editable? true}
         ;; ...
       ]}})
    ;;=>
      {:valid? false,
        :output [{:path [:prop :table :is-system?], :message \"Value ':true' not from allowed list '[true false]'\"}
                 {:path [:prop :table :allow-modifing?], :message \"Value ':true' not from allowed list '[true false]'\"}]}"
  (create-validator #'validate-metadata-table))

(def validate-columns
  "Descritption
    Validate only columns from table spec

  Example
    (validate-columns
     {:id 30, :table \"user\", :prop
     {:table {:representation \"us er\", :is-system? :true, :is-linker? false, :allow-modifing? :true, :allow-deleting? true, :allow-linking? true},
      :columns
      [{:field \"login\", :representation \"login\", :description nil, :component-type true, :column-type \"varchar(100)\", :private? false, :editable? :true}
       {:field \"password\", :representation \"password\", :description nil, :component-type \"i\", :column-type \"varchar(100)\", :private? false, :editable? true}
       {:field \"first_name\", :representation \"first_name\", :description nil, :component-type \"i\", :column-type \"varchar(100)\", :private? false, :editable? true}
       {:field \"last_name\", :representation \"last_name\", :description nil, :component-type \"--\", :column-type \"varchar(100)\", :private? false, :editable? true}
       {:field \"id  _permission\", :representation \"id_permission\",
        :description 123, :component-type \"l\", :column-type \"bigint(120) unsigned\",
        :private? false, :editable? true, :key-table \"permission\"}]}})
    ;;=> 
     {:valid? false
      :output [{:path [:prop :columns 0 :component-type], :message \"Value 'true' not from allowed list '[\\\"d\\\"...]'\"}
               {:path [:prop :columns 0 :editable?], :message \"Value ':true' not from allowed list '[true false]'\"}
               {:path [:prop :columns 3 :component-type], :message \"Value '--' not from allowed list '[\\\"d\\\"...]'\"}
               {:path [:prop :columns 4 :field], :message \"Value 'id  _permission' not valid on regexp '^[a-z_]{3,}$' pattern\"}
               {:path [:prop :columns 4 :description], :message \"Value '123' not valid by predicate 'string? || nil?'\"}]}"
  (create-validator #'validate-metadata-columns))

(def validate-one-column
  "Descritption
    Validate one column from table spec

  Warning
    In returning map, in :output ... :path pathskeypaths
    will be started at your column name. Not relative to
    your table
  
  Example
    (validate-one-column
      {:field \"first _name\", :representation \"first_name\", :description nil, :component-type \"i\", :column-type \"varchar(100)\", :private? false, :editable? true})
     ;;=> {:valid? false,
           :output [{:path [:field], :message \"Value 'first _name' not valid on regexp '^[a-z_]{3,}$' pattern\"}]}"
  (create-validator #'validate-metadata-column))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Table logic comparators ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- do-sql [sql-expression]
  (if (try (or (println sql-expression) true)
           (catch java.sql.SQLException e (str "caught exception: " (.toString e)) false))
    true
    false))

(defn- apply-f-diff [f-o-c-list original changed]
  (if (empty? f-o-c-list) original
      (first (reduce (fn [[o c] f] [(f o c) c]) [original changed] f-o-c-list))))

(defn- list-key-path
  "Example
    Get key-path list for table 
  Example
    (list-key-path {:id 30, :table \"user\",
                    :prop {:table {:representation \"user\", :is-system? false,
                                   :is-linker? false, :allow-modifing? true,
                                   :allow-deleting? true, :allow-linking? true}}})
     ;; => [[:id]
            [:table]
            [:prop :table :representation]
            [:prop :table :is-system?]
            [:prop :table :is-linker?]
            [:prop :table :allow-modifing?]
            [:prop :table :allow-deleting?]
            [:prop :table :allow-linking?]]"
  [m]
  (letfn [(keylist [m ref-var path]
            (let [[head tail] (map-destruct m)
                  m-fk        (first-key head)]
              (if (= path [:prop m-fk])
                (keylist tail ref-var (concat path)))
              (if (map? (m-fk head))
                (keylist (m-fk head) ref-var (concat path [m-fk]))
                (if-not (= [:prop :columns] [:prop m-fk])
                  (dosync (alter ref-var #(conj %1 (vec (concat path [m-fk])))))))
              (if tail
                (keylist tail ref-var (concat path)))))]
    (let [in-deep-key-path (ref [])]
      (keylist m in-deep-key-path nil)
      @in-deep-key-path)))


(do
  (defn adiff-table [original changed]
    (let [key-replace (fn [p] (partial (fn [p m1 m2] (println "replace key " p  " from " (get-in m1 p) " to " (get-in m2 p))
                                         (assoc-in m1 p (get-in m2 p "<Error name>"))) p))
          f-comparator (fn [p m1 m2] (get-apply = p m1 m2))]
      (vec (reduce (fn [acc p] (if (get-apply (comp not =) p original changed) (conj acc (key-replace p)) acc))
                   []
                   [;; [:id]
                    ;; [:table]
                    [:prop :table :representation]
                    [:prop :table :description]
                    ;; [:prop :table :is-system?]
                    ;; [:prop :table :is-linker?]
                    [:prop :table :allow-modifing?]
                    [:prop :table :allow-deleting?]
                    [:prop :table :allow-linking?]]))))
  ;; (adiff-table
  ;;  {:id 30, :table "user",
  ;;   :prop {:table {:representation "user", :is-system? false,
  ;;                  :is-linker? false, :allow-modifing? true,
  ;;                  :allow-deleting? true, :allow-linking? true}}}
  ;;  {:id 30, :table "user",
  ;;   :prop {:table {:representation "CHU1", :is-system? false,
  ;;                  :is-linker? false, :allow-modifing? false,
  ;;                  :allow-deleting? true, :allow-linking? true}}})
  )


(do
  (defn- find-difference-columns
    "DONT EVEN TRY TO UNDERSTAND!
  differ algorythm for comparison two list of metatable column-repr
  (find-difference-columns
     [{:field 10} {:field 20} {:field 4} {:field 5} {:field 6} {:field 7} {:field 8} {:field 9}]
     [{:field 10} {:field 20} {:field 4}            {:field 6} {:field 7} {:field 8} {:field 9} {:field 111}])
       ;=> {:maybe-changed [{:field 10} {:field 20} {:field 4} {:field 6} {:field 7} {:field 8} {:field 9}], :must-create [{:field 111}], :must-delete [{:field 5}]} "
    [original changed]
    (let [criterion-field :field
          do-diff
          (fn [original changed]
            (let [[old-elements new-elements] [(ref []) (ref [])]]
              (doseq [changed-elm changed]
                (if-let [id_ce (criterion-field changed-elm)]
                  (if-let [old-elm (first (filter (fn [org-elm] (= id_ce (criterion-field org-elm))) original))]
                    (dosync (commute old-elements #(conj % old-elm)))
                    (dosync (commute new-elements #(conj % changed-elm))))
                  (dosync (commute new-elements #(conj % changed-elm)))))
              [@old-elements @new-elements]))]
      (let [;; [old new] (doto (do-diff original changed) println)
            [old new] (do-diff original changed)
            [old del] (do-diff old original)]
        {:maybe-changed old
         :must-create new
         :must-delete del})))
  (defn column-resolver [original changed]
    (let [original-changed-field (map (comp :columns :prop) [original changed])]
      (apply find-difference-columns original-changed-field)))
  ;; (column-resolver {:id 30, :table "user",
  ;;                   :prop {:columns [{:field "login", :representation "login", :description nil, :component-type "i"}
  ;;                                    {:field "password", :representation "password", :description nil, :component-type "i"}
  ;;                                    {:field "DELETED_COLUMN", :representation "first_name", :description nil, :component-type "i"}
  ;;                                    ;; {:field "first_name", :representation "first_name", :description nil, :component-type "i"}
  ;;                                    {:field "last_name", :representation "last_name", :description nil, :component-type "i"}
  ;;                                    {:field "id_permission", :representation "id_permission", :description nil, :component-type "l"}]}}
  ;;                  {:id 30, :table "user",
  ;;                   :prop {:columns [{:field "login", :representation "login", :description nil, :component-type "i"}
  ;;                                    {:field "password", :representation "password", :description nil, :component-type "i"}
  ;;                                    {:field "first_name", :representation "first_name", :description nil, :component-type "i"}
  ;;                                    ;; {:field "last_name", :representation "last_name", :description nil, :component-type "i"}
  ;;                                    {:field "NEW_COLUMN", :representation "last_name", :description nil, :component-type "i"}
  ;;                                    {:field "id_permission", :representation "id_permission", :description nil, :component-type "l"}]}})
  )
;; {:maybe-changed
;;  [{:field "login", :representation "login", :description nil, :component-type "i"}
;;   {:field "password", :representation "password", :description nil, :component-type "i"}
;;   {:field "id_permission", :representation "id_permission", :description nil, :component-type "l"}]
;;  :must-create
;;  [{:field "first_name", :representation "first_name", :description nil, :component-type "i"}
;;   {:field "NEW_COLUMN", :representation "last_name", :description nil, :component-type "i"}]
;;  :must-delete
;;  [{:field "DELETED_COLUMN", :representation "first_name", :description nil, :component-type "i"}
;;   {:field "last_name", :representation "last_name", :description nil, :component-type "i"}]}


(defn f-diff-prop-columns-fields
  "This function is crap, not even try to understand what it do, but it work"
  [original changed]
  (let [;; function `map-k-eq` compare elements in map by keys, and return only maps of differ pairs of `map-l`
        ;; For example:
        ;; (map-k-eq {:a 1 :b 2} {:a 3 :b 2}) => {:a 3}
        m (letfn [(map-k-eq [m-key & map-l] (fn [f] (apply f (reduce #(conj %1 (get %2 m-key)) [] map-l))))]
            (reduce (fn [m-acc c-key] (if ((map-k-eq c-key original changed) =) m-acc
                                          (into m-acc {c-key (get changed c-key)}))) {} (keys changed)))]
    (let [key-replace (fn [p] (fn [p1 p2] (fn [m1 m2] (println "replace key " (vec (concat p1 p)) " from " (get-in m1 (vec (concat p1 p))) " to " (get-in m2 (vec (concat p2 p))))
                                            (assoc-in m1 (vec (concat p1 p)) (get-in m2 (vec (concat p2 p)) "<Error name>")))))]
      (((fn [f] (f f))
        (fn [f]
          (fn [[head-map tail-map]]
            (if head-map
              (if-let [n (cond-contain head-map
                                       :representation (key-replace [:representation])
                                       :description    (key-replace [:description])
                                       :component-type (key-replace [:component-type])
                                       :private?       (key-replace [:private?])
                                       :editable?      (key-replace [:editable?])
                                       nil)]
                (if-not tail-map [n] (concat [n] ((f f) (map-destruct tail-map))))))))) (map-destruct m)))))
;; do not apply it to apply-f-table, it works only with `changed-fields`
;; (f-diff-prop-columns-fields
;;  {:field "id_permission", :representation "id_permission",
;;   :description nil, :component-type "BBBBBBBBBBBB",
;;   :column-type "BBBBBBBBBBB", :private? false,
;;   :editnable? true, :key-table "permission"}
;;  {:field "id_permission", :representation "aaaaaaaaaaaaaaaaaaaa"
;;   :description "aaaaaaaaaaaaaaaa", :component-type "l",
;;   :column-type "bigint(120) unsigned", :private? "aaaaaaaaaaaaaaaa",
;;   :editnable? true, :key-table "permission"})


#_(def user-original {:id 30
                    :table "user"
                    :prop {:table {:representation "user"
                                   :is-system? false :is-linker? false
                                   :allow-modifing? true :allow-deleting? true
                                   :allow-linking? true}
                           :columns [{:field "login", :representation "login", :description nil, :component-type "i"
                                      :column-type "varchar(100)", :private? false, :editable? true}
                                     {:field "password", :representation "password", :description nil, :component-type "i"
                                      :column-type "varchar(100)", :private? false, :editable? true}
                                     {:field "first_name", :representation "first_name", :description nil
                                      :component-type "i", :column-type "varchar(100)", :private? false, :editable? true}
                                     {:field "last_name", :representation "last_name", :description nil
                                      :component-type "i", :column-type "varchar(100)", :private? false, :editable? true}
                                     {:field "id_permission", :representation "id_permission", :description nil
                                      :component-type "l", :column-type "bigint(120) unsigned", :private? false, :editable? true, :key-table "permission"}]}})
;; :allow-modifing? true
;; :representation "UĹźytkownik"
;; :add field "age"
;; :delete "first_name
#_(def user-changed {:id 30
                   :table "user"
                   :prop {:table {:representation "Uďż˝ytkownik"
                                  :is-system? false :is-linker? false
                                  :allow-modifing? false :allow-deleting? true
                                  :allow-linking? true}
                          :columns [{:field "login", :representation "Logowanie", :description "Logowanie pole", :component-type "i"
                                     :column-type "varchar(100)", :private? false, :editable? true}
                                    {:field "password", :representation "Haslo", :description nil, :component-type "i"
                                     :column-type "varchar(100)", :private? false, :editable? true}
                                    ;; {:field "first_name", :representation "Drugie imie", :description nil,
                                    ;;  :component-type "i", :column-type "varchar(100)", :private? false, :editable? true}
                                    {:field "last_name", :representation "last_name", :description nil
                                     :component-type "i", :column-type "varchar(100)", :private? false, :editable? true}
                                    {:field "age", :representation "Wiek", :description nil
                                     :component-type "i", :column-type "number", :private? false, :editable? true}
                                    {:field "id_permission", :representation "id_permission", :description nil
                                     :component-type "l", :column-type "bigint(120) unsigned", :private? false, :editable? true, :key-table "permission"}]}})

(do
  (defn delete-fields [original fields]
    (vec (for [field fields]
           (fn [original changed]
             (if (do-sql (alter-table! {:table_name (:table original) :drop-column (:field field)}))
               (update-in original [:prop :columns] (fn [fx] (filter #(not= (:field %) (:field field)) fx)))
               original)))))
  ;; (apply-f-diff
  ;;  (delete-fields user-original [{:field "first_name", :representation "first_name", :description nil,
  ;;                                 :component-type "i", :column-type "varchar(100)", :private? false, :editable? true}])
  ;;  user-original
  ;;  user-changed)
  )

(do
  (defn create-fields [original fields]
    (for [field fields]
      (fn [original changed]
        (if (do-sql (alter-table! {:table_name (:table original) :add-column {(:field field) [(:column-type field)]}}))
          (clojure.core/update-in original [:prop :columns] (fn [all] (conj all field))) original))))
  ;; (apply-f-diff
  ;;  (create-fields user-original [{:field "suka_name", :representation "SUKA", :description nil,
  ;;                                 :component-type "i", :column-type "varchar(100)", :private? false, :editable? true}])
  ;;  user-original
  ;;  user-changed)
  )


(do
  (defn change-fields [original changed]
    (let [fx ((comp :columns :prop) original)
          yx ((comp :columns :prop) changed)]
      (apply concat (for [[yi y] (map-indexed vector yx)]
                      (let [[fi f] (find-column #(= (:field (second %)) (:field y)) (map-indexed vector fx))]
                        (map #(% [:prop :columns fi] [:prop :columns yi]) (f-diff-prop-columns-fields f y)))))))
  ;; (apply-f-diff
  ;;  (change-fields user-original user-changed)
  ;;  user-original
  ;;  user-changed)
  )


(do
  (defn apply-table [original changed]
    (let [fxmap-changes (atom {})
          f-table-changes (adiff-table original changed)
          {column-changed :maybe-changed
           column-created :must-create
           column-deleted :must-delete} (column-resolver original changed)]
     ;;; `TODO` delete this debug fileds
      (println (apply str "   Actions:"))
      (do (if (not-empty f-table-changes) (println "\ttable chnaged: " (count f-table-changes)))
          (if (not-empty column-deleted) (println "\tcolumn deleted: " (count (delete-fields original column-deleted)))) ;;1
          (if (not-empty column-created) (println "\tcolumn created: " (count (create-fields original column-created)))) ;;1 
          (if (not-empty column-changed) (println "\tcolumn changed: " (count (change-fields original changed)))))
     ;; (do (if (not-empty f-table-changes) (swap! fxmap-changes #(assoc % :table (count f-table-changes))))
     ;;     (if (not-empty column-deleted) (swap! fxmap-changes #(assoc % :column-deleted (count (delete-fields original column-deleted)))))
     ;;     (if (not-empty column-created) (swap! fxmap-changes #(assoc % :column-created (count (create-fields original column-created)))))
     ;;     (if (not-empty column-changed) (swap! fxmap-changes #(assoc % :column-changed (count (change-fields original changed))))))

      (do (if (not-empty f-table-changes) (swap! fxmap-changes #(assoc % :table f-table-changes)))
          (if (not-empty column-deleted) (swap! fxmap-changes #(assoc % :column-deleted (delete-fields original column-deleted))))
          (if (not-empty column-created) (swap! fxmap-changes #(assoc % :column-created (create-fields original column-created))))
          (if (not-empty column-changed) (swap! fxmap-changes #(assoc % :column-changed (change-fields original changed)))))
      @fxmap-changes))
 ;; (apply-table user-original user-changed)
  )

;;; `TODO` persist do database in metadata tabel.
;;; `TODO` persist do database in metadata tabel.
;;; `TODO` persist do database in metadata tabel.
;;; `TODO` persist do database in metadata tabel.
;;; `TODO` persist do database in metadata tabel.
;;; `TODO` persist do database in metadata tabel.
(defn do-change
  "Description:
    Function apply lazy fxmap-changes argument as list of SQL applicative functors.

  Example:
    (do-changes
      (apply-table original-table changed-table)
      original-table changed-table)

  Arguments:
  `fxmap-changes` - special map, generated function `apply-table`
  `original` - stable version of table meta
  `changed` - changed by user table
  `keywords` - [optional] select one of actions `:table`,`:column-changed`,`:column-created`,`column-deleted`.
    which changed methadata and original SQL by specyfic action. If keywords is empty, what done all the action
    in `fxmap-changes` dictionary.

  See related functions
  `jarman.logic.metadata/apply-table`
  `jarman.logic.metadata/adiff-table`
  `jarman.logic.metadata/delete-fields`
  `jarman.logic.metadata/create-fields`
  `jarman.logic.metadata/change-fields`"
  [fxmap-changes original changed & keywords]
  ;; (println (apply str (repeat 30 "-")))
  ;; (println (apply-f-diff (get fxmap-changes :table nil) original changed))
  ;; (println (apply-f-diff (get fxmap-changes :column-changed nil) original changed))
  ;; (println (apply-f-diff (get fxmap-changes :column-deleted nil) original changed))
  ;; (println (apply-f-diff (get fxmap-changes :column-created nil) original changed))
  ;; (println (apply str (repeat 30 "-")))
  (let [keywords (if (empty? keywords)
                   ;; apply changes of all of state 
                   [:table :column-changed :column-deleted :column-created]
                   ;; apply changes only on [one-of keywords-list stages
                   (vec (filter #(some (fn [kwd] (= kwd %)) keywords)
                                [:table :column-changed :column-deleted :column-created])))]
    (let [kkkk (if-not (empty? keywords)
                 (reduce #(apply-f-diff (get fxmap-changes %2 nil) %1 changed) original [:table :column-changed :column-deleted :column-created])
                 (do (println "Chanages not being applied, empty keywords list")
                     original))]
      (println kkkk)
      (->> kkkk
         (update-sql-by-id-template "metadata")
         (db/exec)))))

;;;;;;;;;;;;;;;;;;;;;
;;; OBJECT SYSTEM ;;;
;;;;;;;;;;;;;;;;;;;;;

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
  (return-component-type  [this])
  (return-default-value   [this])
  (return-private?        [this])
  (return-editable?       [this]))

(defprotocol IFieldReference
  (return-foreign-keys    [this])
  (return-key-table       [this]))

(defprotocol IColumns
  (return-columns [this]))

(defprotocol IGroup
  (group                  [this m])
  (ungroup                [this m]))

(defprotocol IFieldComposite
  (return-constructor     [this]))

(defprotocol IMetadata
  (return-table_name [this])
  (return-id [this])
  (return-prop [this])
  (return-table [this])
  (return-columns-composite [this])
  (return-columns-join [this])
  (return-columns-flatten [this])
  (return-columns-wrapp [this])
  (return-columns-composite-wrapp [this])
  (return-columns-join-wrapp [this])
  (return-columns-flatten-wrapp [this])
  (exists? [this])
  (persist [this])
  (refresh [this])
  (diff-changes [this changed])
  (diff-changes-apply [this changed]))

(defrecord Field [m]

  IField
  (return-field           [this] (get (.m this) :field))
  (return-field-qualified [this] (get (.m this) :field-qualified))
  (return-description     [this] (get (.m this) :description))
  (return-representation  [this] (get (.m this) :representation))
  (return-column-type     [this] (get (.m this) :column-type))
  (return-component-type  [this] (get (.m this) :component-type))
  (return-default-value   [this] (get (.m this) :default-value))
  (return-private?        [this] (get (.m this) :private?))
  (return-editable?       [this] (get (.m this) :editable?)))

(defrecord FieldLink [m]

  IField
  (return-field           [this] (get (.m this) :field))
  (return-field-qualified [this] (get (.m this) :field-qualified))
  (return-description     [this] (get (.m this) :description))
  (return-representation  [this] (get (.m this) :representation))
  (return-column-type     [this] (get (.m this) :column-type))
  (return-component-type  [this] (get (.m this) :component-type))
  (return-default-value   [this] (get (.m this) :default-value))
  (return-private?        [this] (get (.m this) :private?))
  (return-editable?       [this] (get (.m this) :editable?))

  IFieldReference
  (return-foreign-keys    [this] (get (.m this) :foreign-keys))
  (return-key-table       [this] (get (.m this) :key-table)))

(defrecord FieldComposite [m group-fn ungroup-fn]

  IFieldSearch
  (find-field           [this field-name-kwd]
    (first (filter (fn [field-m]
                     (= (:field field-m) field-name-kwd)) (.return-columns this))))
  
  (find-field-qualified [this field-name-qualified-kwd]
    (first (filter (fn [field-m]
                     (= (:field-qualified field-m) field-name-qualified-kwd)) (.return-columns this))))
  (find-field-by-comp-var [this field-comp-var field-name-qualified-kwd]
    (reduce (fn [acc column] (if (= (:constructor-var column) field-comp-var)
                               (conj acc (:field column)) acc)) []
            (:columns (:m this))))

  IField
  (return-field           [this] (get (.m this) :field))
  (return-field-qualified [this] (get (.m this) :field-qualified))
  (return-description     [this] (get (.m this) :description))
  (return-representation  [this] (get (.m this) :representation))
  (return-private?        [this] (get (.m this) :private?))
  (return-editable?       [this] (get (.m this) :editable?))
  
  IColumns
  (return-columns         [this] (get (.m this) :columns))

  IFieldComposite
  (return-constructor     [this] (get (.m this) :constructor))

  IGroup
  (group                  [this data-m] ((.group-fn this) data-m))
  (ungroup                [this data-m] ((.ungroup-fn this) data-m)))

(defn isField? [^jarman.logic.metadata.Field e]
  (instance? jarman.logic.metadata.Field e))
(defn isFieldLink? [^jarman.logic.metadata.FieldLink e]
  (instance? jarman.logic.metadata.FieldLink e))
(defn isFieldComposite? [^jarman.logic.metadata.FieldComposite e]
  (instance? jarman.logic.metadata.FieldComposite e))

(defn to-field [m]
  (Field. m))
(defn to-field-link [m]
  (FieldLink. m))
(defn to-field-composite [field]
  (let [{field-list :fields-list var-list :var-list :as all}
        ;; {:mapp        { :seal.site_name :text, :seal.site_url :link }
        ;;  :demapp      { :text :seal.site_name, :link :seal.site_url }
        ;;  :fields-list [ :seal.site_name :seal.site_url ]
        ;;  :var-list    [ :text :link ]}
        (reduce (fn [a f]
                  (-> a
                      (update :fields-list conj (:field-qualified f))
                      (update :var-list conj (:constructor-var f))
                      (update :mapp into {(:field-qualified f) (:constructor-var f)})
                      (update :demapp into {(:constructor-var f) (:field-qualified f)})))
                {:mapp {} :demapp {} :fields-list [] :var-list []}
                (:columns field))
        make-mapp   (fn [e] (clojure.set/rename-keys (select-keys e field-list) (:mapp all)))
        make-demapp (fn [e] (clojure.set/rename-keys e                          (:demapp all)))
        from-flatt (fn [m]
                     (-> (reduce (fn [acc f] (dissoc acc f)) m field-list)
                         (assoc-in [(:field-qualified field)]
                                   ((eval (:constructor field)) (make-mapp m)))))
        to-flatt   (fn [m]
                     (if-not (contains? m (:field-qualified field)) m
                             (let [composite-flatt (make-demapp (get-in m [(:field-qualified field)]))
                                   m (dissoc m (:field-qualified field))
                                   m (merge m composite-flatt)] m)))]
    (->FieldComposite field from-flatt to-flatt)))

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
  (return-columns    [this]
    (vec (get-in (.m this) [:prop :columns] [])))
  
  IMetadata
  (return-prop       [this] (get (.m this) :prop nil))
  (return-id         [this] (get (.m this) :id nil))
  (return-table_name [this] (get (.m this) :table_name nil))
  (return-table      [this] (get-in (.m this) [:prop :table] nil))

  (return-columns-wrapp [this]
    (vec (wrapp-cols-metadata-types (get-in (.m this) [:prop :columns] []))))
  (return-columns-composite-wrapp [this]
    (vec (wrapp-cols-metadata-types (get-in (.m this) [:prop :columns-composite] []))))
  (return-columns-join-wrapp [this]
    (vec (concat (.return-columns-wrapp this)
                 (.return-columns-composite-wrapp this))))
  (return-columns-flatten-wrapp [this]
    (vec (concat (.return-columns-wrapp this)
                 (mapcat #(wrapp-cols-metadata-types (.return-columns %))
                         (.return-columns-composite-wrapp this)))))
  
  (return-columns-composite [this]
    (vec (get-in (.m this) [:prop :columns-composite] [])))
  (return-columns-join [this]
    (vec (concat (.return-columns this) (.return-columns-composite this))))
  (return-columns-flatten [this]
    (vec (concat (.return-columns this) (mapcat :columns (.return-columns-composite this)))))
  
  
  (exists? [this]
    (if (not-empty (jarman.logic.metadata/getset! (.return-table-name this)))
      true))
  (refresh [this]
    (if (.exists? this)
      (set! (.m this) (first (jarman.logic.metadata/getset! (.return-table-name this))))))
  (persist [this]
    (jarman.logic.metadata/update-meta (.m this)))
  (diff-changes [this changed]
    (if changed
      (let [metadata-original (.m this)
            metadata-changed (.m changed)]
        (jarman.logic.metadata/apply-table metadata-original metadata-changed))))
  (diff-changes-apply [this changed]
    (if changed
      (let [metadata-original (.m this)
            metadata-changed (.m changed)]
        (jarman.logic.metadata/do-change
         (jarman.logic.metadata/apply-table metadata-original metadata-changed)
         metadata-original metadata-changed))))
  IGroup
  (group [this m]
    (reduce
     (fn [acc field] (.group field acc))
     m (.return-columns-composite-wrapp this)))
  (ungroup [this m]
    (reduce
     (fn [acc field] (.ungroup field acc))
     m (.return-columns-composite-wrapp this))))

(defn isTableMetadata? [^jarman.logic.metadata.TableMetadata e]
  (instance? jarman.logic.metadata.TableMetadata e))

;;;;;;;;;;;;;;;;
;;; On meta! ;;;
;;;;;;;;;;;;;;;;

(defn create-table-by-meta [metadata]
  (let [metadata (if (isTableMetadata? metadata) metadata (TableMetadata. metadata) )
        all-columns (.return-columns-flatten metadata)]
    (let [smpl-fields (filter (comp (partial not-allowed-rules ["meta*"]) name :field) all-columns)
          idfl-fields (filter (comp (partial allowed-rules "id_*") name :field) all-columns)
          fkeys-fields (vec (eduction (filter :foreign-keys) (map :foreign-keys) idfl-fields))]
      (create-table!
       {:table_name (keyword (.return-table_name metadata))
        :columns (vec (map (fn [sf] {(keyword (:field sf)) (:column-type sf)}) smpl-fields))
        :foreign-keys fkeys-fields}))))

(defn create-all-table-by-meta [table-list]
  (for [table table-list]
    (let [mtable (first (getset table))]
      (if-not (nil? mtable)
        (create-table-by-meta mtable)))))

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
           (first (getset! (--get-foreight-table-by-column reference)))
           on-recur-action :back-ref table-meta :column-ref reference))))))

;;;;;;;;;;;;;;;;
;;; On meta! ;;;
;;;;;;;;;;;;;;;;

(defn create-table-by-meta [metadata]
  (let [metadata (if (isTableMetadata? metadata) metadata (TableMetadata. metadata) )
        all-columns (.return-columns-flatten metadata)]
    (let [smpl-fields (filter (comp (partial not-allowed-rules ["meta*"]) name :field) all-columns)
          idfl-fields (filter (comp (partial allowed-rules "id_*") name :field) all-columns)
          fkeys-fields (vec (eduction (filter :foreign-keys) (map :foreign-keys) idfl-fields))]
      (create-table!
       {:table_name (keyword (.return-table_name metadata))
        :columns (vec (map (fn [sf] {(keyword (:field sf)) (:column-type sf)}) smpl-fields))
        :foreign-keys fkeys-fields}))))

(defn create-all-table-by-meta [table-list]
  (for [table table-list]
    (let [mtable (first (getset table))]
      (if-not (nil? mtable)
        (create-table-by-meta mtable)))))

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
           (first (getset! (--get-foreight-table-by-column reference)))
           on-recur-action :back-ref table-meta :column-ref reference))))))

