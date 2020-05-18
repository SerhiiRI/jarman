 ;; Description:
;;    Module generate metainformation for database
;;   tables(but exclude metainformation for table
;;   defined in `*meta-rules*` variable. All
;;   metadata must be saving in `METATABLE`
;;   database table.
;;
;;    One metatable entity describes on top level
;;   table name and serialized properties, for
;;   example {:id 412 :table "cache" :props "{}"},
;;   where `:table` describe 1:1 table name,
;;   and `:props` properties used for building
;;   UI and db-logic to program.
;;
;; Properties `prop` data:
;;   {:id 1
;;    :table "cache_register"
;;    :prop {:table {:representation "cache_register" :private? false :scallable? true :linker? false}
;;           :columns [{:field "id_point_of_sale"
;;                      :representation "id_point_of_sale"
;;                      :description nil
;;                      :component-type "l"
;;                      :column-type "bigint(20) unsigned"
;;                      :private? false
;;                      :editable? true}
;;                     {:field "name"
;;                      :representation "name" ...}...]
;;
;;    Deserialized `prop`(look above) contain
;;   specially meta for whole table behavior and
;;   some selected column(not for all, in this
;;   version, only column 'id' hasn't self meta
;;   info).
;;
;;   Short meta description for table:
;;    :representation - is name of table which was viewed by user. By default it equal to table name. 
;;    :private? - specify if table hided for user. False by default
;;    :scallable? - if it false, program not allowe to extending or reducing column count. Only for UI. 
;;    :linker? - specify table which created to bind other table with has N to N relations to other. this table by default private.
;;
;;   Short meta description for columns:
;;    :field - database column name.
;;    :representation - name for end-user. By default equal to :field. 
;;    :description - some description information, used for UI.
;;    :column-type - database type of column.
;;    :private? - true if column must be hided for user UI. 
;;    :editable? - true if column editable
;;    :component-type - influed by column-type key, contain one of symbol ("d" "t" "dt" "n" "b" "a"
;;    "i" nil), which describe some hint to representation information by UI:
;;          "d" - date
;;          "t" - time
;;          "dt" - date and time
;;          "n" - simple number
;;          "b" - mean boolean type of data
;;          "a" - big text block
;;          "i" - short text input
;;          nil - no hint, but not must be viewed, only not specified. 
;;                      
(ns jarman.data
  (:require
   [jarman.sql-tool :as toolbox :include-macros true :refer :all]
   [clojure.string :as string]
   [clojure.java.jdbc :as jdbc]))

(def ^:dynamic *id-collumn-rules* ["id", "id*"])
(def ^:dynamic *meta-rules* ["metatable" "meta*"])

;;;;;;;;;;;;;;;;;;;;;;
;;; RULE FILTRATOR ;;;
;;;;;;;;;;;;;;;;;;;;;;

(defn not-allowed-rules
  "Description:
    The function do filter on `col` list, selected only that string elements, which not allowed by `rule-spec`.

  Rule Spec:
    Rule spec is simple string with simple declaration. He serve three type of rule:
    - 'metadata' (whole word)
    - 'meta*' (words, wich started on \"meta\")
    - '*data' (words, wich end on \"*data\")

  Example using:
    (not-allowed-rules [\"dupa\" \"_pri*\"] [\"dupa\" \"_PRI\" \"_Private\" \"something\"]
      ;;=> (\"something\")
    
  See related:
    (`jarman.data/allowed-rules`)" 
  [rule-spec col]
  (let [f-comp (fn [p] (condp = (.indexOf (seq p) \*)
                         (dec (count p)) #(not= (butlast p) (take (dec (count p)) (string/lower-case %))) 
                         0               #(not= (drop 1 p) (take-last (dec (count p)) (string/lower-case %)))
                         #(not= p %)))
        preds (map f-comp rule-spec)]
    (filter (fn [s] (reduce (fn [a p?] (and a (p? s))) true preds)) col)))

(defn allowed-rules
  "Description:
    The function do filter on `col` list, selected only that string elements, which allowed by `rule-spec`.

  Rule Spec:
    Rule spec is simple string with simple declaration. He serve three type of rule:
    - 'metadata' (whole word)
    - 'meta*' (words, wich started on \"meta\")
    - '*data' (words, wich end on \"*data\")

  Example using:
    (allowed-rules [\"dupa\" \"_pri*\"] [\"dupa\" \"_PRI\" \"_Private\" \"something\"]
      ;;=> (\"dupa\" \"_PRI\" \"_Private\")

  See related:
    (`jarman.data/allowed-rules`)"
  [rule-spec col]
  (let [f-comp (fn [p] (condp = (.indexOf (seq p) \*)
                         (dec (count p)) #(= (butlast p) (take (dec (count p)) (string/lower-case %))) 
                         0               #(= (drop 1 p) (take-last (dec (count p)) (string/lower-case %)))
                         #(= p %)))
        preds (map f-comp rule-spec)]
    (filter (fn [s] (reduce (fn [a p?] (or a (p? s))) false preds)) col)))


(defn is-id-col? [col-name]
  (let [col (string/lower-case col-name)]
    (or (= col "id")
        (= (take 2 col) '(\i \d)))))
(defn is-not-id-col? [col-name]
      (not (is-id-col? col-name)))

(def ^:dynamic sql-connection {:dbtype "mysql" :host "127.0.0.1" :port 3306 :dbname "ekka-test" :user "root" :password "123"})
;; (def ^:dynamic sql-connection {:dbtype "mysql" :host "127.0.0.1" :port 3306 :dbname "" :user "root" :password "123"})
(def ^:dynamic *available-mariadb-engine-list* "set of available engines for key-value tables" ["MEMORY", "InnoDB", "CSV"])
;; (jdbc/query sql-connection "SHOW ENGINES" )
;; (jdbc/execute! sql-connection "CREATE DATABASE `ekka-test` CHARACTER SET = 'utf8' COLLATE = 'utf8_general_ci'")


(defn get-component-group-by-type [column-field-spec]
  (let [ctype  (re-find #"[a-zA-Z]*" (string/lower-case (:type column-field-spec)))
        cfield (:field column-field-spec)
        in?   (fn [col x] (if (string? col) (= x col) (some #(= % x) col)))]
    (if (not-empty ctype)
      (if (not-empty (allowed-rules *id-collumn-rules* [cfield])) "l" ;; l - mean linking is linking column 
        (condp in? ctype
          "date"         "d" ;; datetime
          "time"         "t" ;; only time
          "datetime"     "dt" ;; datatime
          ["smallint"
           "mediumint"
           "int"
           "integer"
           "bigint"
           "double"
           "float"
           "real"]       "n" ;; n - mean simple number input
          ["tinyint"
           "bool"
           "boolean"]    "b" ;; b - mean boolean
          ["tinytext"
           "text"
           "mediumtext"
           "longtext"
           "json"]       "a" ;; a - mean area, text area
          "varchar"      "i" ;; i - mean simple text input
          nil)))))

(defn- get-table-meta
  "Description:
    Get meta information about table by hame, and construct meta information to GUI

  Template of returning meta:
  {:representation \"table\" :private? true :scallable? false :linker? true}
  :representation - table name wich would be viewed for user in GUI, must b readable.
  :private? - if is true, than user can be edit this table
  :scallable? - if is set on true, user may add fields to datatable.
  :linker? - flag inform that table used by DB engine mechanizm. (DO NOT EDIT THAT FIELD)"
  [t-name]
  {:pre [(string? t-name) (not-empty t-name)]}
  (let [tspec (last (string/split t-name #"_"))
        in?   (fn [col x] (if (string? col) (= x col) (some #(= % x) col)))]
    (condp in? tspec
      ["lk" "link" "links"]
      {:representation t-name :private? true :scallable? false :linker? true}
      {:representation t-name :private? false :scallable? true :linker? false})))

(defn- get-table-field-meta [column-field-spec]
  (let [tfield (:field column-field-spec)
        ttype (:type column-field-spec)
        in?   (fn [col x] (if (string? col) (= x col) (some #(= % x) col)))]
    {:field tfield
     :representation tfield
     :description nil
     :component-type (get-component-group-by-type column-field-spec)
     :column-type ttype
     :private? false
     :editable? true}))

(defn- get-meta [table-name]
  {:id nil
   :table table-name
   :prop (str {:table (get-table-meta table-name)
               :columns (vec (map get-table-field-meta
                                  (filter #(not= "id" (:field %))
                                          (jdbc/query sql-connection (show-table-columns table-name)))))})})

(defn- ^clojure.lang.PersistentList update-sql-by-id-template
  ([table m]
   (if (:id m)
     (update table :set (dissoc m :id) :where (= :id (:id m)))
     (insert table :values (vals m)))))

(defn do-create-meta []
  (for [table (not-allowed-rules ["metatable" "meta*"] (map (comp second first) (jdbc/query sql-connection "SHOW TABLES" )))]
    (let [meta (jdbc/query sql-connection (select :METADATA :where (= :table table)))]
      (if (empty? meta) (jdbc/execute! sql-connection (update-sql-by-id-template "METADATA" (get-meta table)))))))

(defn do-clear-meta []
  (jdbc/execute! sql-connection (delete :METADATA)))

(defn getset [& tables]
  (map (fn [meta] (clojure.core/update meta :prop read-string))
       (jdbc/query sql-connection
                   (if-not (empty? tables) 
                     (let [tables (concat ['or] (map (fn [x] ['= :table (name x)]) tables))]
                       (eval (change-expression '(select :METADATA) :where tables)))
                     (select :METADATA)))))





