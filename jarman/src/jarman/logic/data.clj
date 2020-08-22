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
;;    :prop {:table {:frontend-name "user"
;;                   :is-system? false
;;                   :is-linker? false 
;;                   :allow-modifing? true
;;                   :allow-deleting? true
;;                   :allow-linking?  true}
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
;;    :frontend-name - is name of table which was viewed by user. By default it equal to table name. 
;;    :is-linker? - specifing table which created to bind other table with has N to N relations to other.
;;    :is-system? - mark this table as system table.
;;    :allow-modifing? - if it false, program not allowe to extending or reducing column count. Only for UI. 
;;    :allow-modifing? - if true, permit user to modify of column specyfication(adding, removing, changing type)
;;    :allow-linking? - if true, than GUI must give user posible way to adding relation this data table to other.
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
(ns jarman.logic.data
  (:refer-clojure :exclude [update])
  (:require
   [jarman.logic.sql-tool :as toolbox :include-macros true :refer :all]
   [clojure.data :as data]
   [clojure.string :as string]
   [clojure.java.jdbc :as jdbc]))

(def ^:dynamic *id-collumn-rules* ["id", "id*"])
(def ^:dynamic *meta-rules* ["metatable" "meta*"])
(def *not-allowed-to-edition-tables* ["user" "permission"])
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

;; (def ^:dynamic sql-connection {:dbtype "mysql" :host "127.0.0.1" :port 3306 :dbname "ekka-test" :user "root" :password "123"})
;; (def ^:dynamic sql-connection {:dbtype "mysql" :host "127.0.0.1" :port 3306 :dbname "" :user "root" :password "123"})
(def ^:dynamic sql-connection {:dbtype "mysql" :host "192.168.1.69" :port 3306 :dbname "jarman" :user "jarman" :password "dupa"})
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
          "date"         "d"  ;; datetime
          "time"         "t"  ;; only time
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
      ["lk" "link" "links"] {:frontend-name t-name
                             :is-system? true
                             :is-linker? true
                             :allow-modifing? false
                             :allow-deleting? false
                             :allow-linking? false}
      {:frontend-name t-name
       :is-system? false
       :is-linker? false
       :description nil
       :allow-modifing? true
       :allow-deleting? true
       :allow-linking? true})))

;; (jdbc/query sql-connection (show-table-columns :user))
;; {:field "id", :type "bigint(20) unsigned", :null "NO", :key "PRI", :default nil, :extra "auto_increment"}
;; {:field "login", :type "varchar(100)", :null "NO", :key "", :default nil, :extra ""}
;; {:field "password", :type "varchar(100)", :null "NO", :key "", :default nil, :extra ""}
;; {:field "first_name", :type "varchar(100)", :null "NO", :key "", :default nil, :extra ""}
;; {:field "last_name", :type "varchar(100)", :null "NO", :key "", :default nil, :extra ""}
;; {:field "id_permission", :type "bigint(120) unsigned", :null "NO", :key "MUL", :default nil, :extra ""}

(do
  (defn- key-referense [k-column]
    (re-matches #"id_(.*)" (name k-column)))
  (key-referense :is_permission))

(defn- get-table-field-meta [column-field-spec]
  (let [tfield (:field column-field-spec)
        ttype (:type column-field-spec)
        set_key (fn [m] (if-let [[_ table] (re-matches #"id_(.*)" tfield)]
                          (assoc m :key-table table) m))
        in?   (fn [col x] (if (string? col) (= x col) (some #(= % x) col)))]
    (-> {:field tfield
         :representation tfield
         :description nil
         :component-type (get-component-group-by-type column-field-spec)
         :column-type ttype
         :private? false
         :editable? true}
        set_key)))

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

(defn show-tables []
  (not-allowed-rules ["metatable" "meta*"] (map (comp second first) (jdbc/query sql-connection "SHOW TABLES" ))))

(defn do-create-meta []
  (for [table (not-allowed-rules ["metatable" "meta*"] (map (comp second first) (jdbc/query sql-connection "SHOW TABLES" )))]
    (let [meta (jdbc/query sql-connection (select :METADATA :where (= :table table)))]
      (if (empty? meta) (jdbc/execute! sql-connection (update-sql-by-id-template "METADATA" (get-meta table)))))))

(defn do-clear-meta [& body]
  {:pre [(every? string? body) ]}
  (jdbc/execute! sql-connection (delete :METADATA)))


(defn getset [& tables]
  (map (fn [meta] (clojure.core/update meta :prop read-string))
       (jdbc/query sql-connection
                   (if-not (empty? tables) 
                     (let [tables (concat ['or] (map (fn [x] ['= :table (name x)]) tables))]
                       (eval (change-expression '(select :METADATA) :where tables)))
                     (select :METADATA)))))

(def u1
  {:id 30
   :table "user"
   :prop {:table {:frontend-name "user"
                  :is-system? false
                  :is-linker? false
                  :allow-modifing? true
                  :allow-deleting? true
                  :allow-linking? true}
          :columns [{:field "login"
                     :representation "login"
                     :description nil
                     :component-type "i"
                     :column-type "varchar(100)"
                     :private? false
                     :editable? true}
                    {:field "password"
                     :representation "password"
                     :description nil
                     :component-type "i"
                     :column-type "varchar(100)"
                     :private? false
                     :editable? true}
                    {:field "first_name"
                     :representation "first_name"
                     :description nil
                     :component-type "i"
                     :column-type "varchar(100)"
                     :private? false
                     :editable? true}
                    {:field "last_name"
                     :representation "last_name"
                     :description nil
                     :component-type "i"
                     :column-type "varchar(100)"
                     :private? false
                     :editable? true}
                    {:field "id_permission"
                     :representation "id_permission"
                     :description nil
                     :component-type "l"
                     :column-type "bigint(120) unsigned"
                     :private? false
                     :editable? true
                     :key-table "permission"}]}})

(def u2
  {:id 30
   :table "user"
   :prop {:table {:frontend-name "user"
                  :is-system? false
                  :is-linker? false
                  :allow-modifing? false
                  :allow-deleting? true
                  :allow-linking? true}
          :columns [{:field "login"
                     :representation "login"
                     :description nil
                     :component-type "i"
                     :column-type "varchar(100)"
                     :private? false
                     :editable? true}
                    {:field "password"
                     :representation "password"
                     :description nil
                     :component-type "i"
                     :column-type "varchar(100)"
                     :private? false
                     :editable? true}
                    {:field "first_name"
                     :representation "first_name"
                     :description nil
                     :component-type "i"
                     :column-type "varchar(100)"
                     :private? true
                     :editable? true}
                    {:field "last_name"
                     :representation "last_name"
                     :description nil
                     :component-type "i"
                     :column-type "varchar(100)"
                     :private? false
                     :editable? true}
                    {:field "id_permission"
                     :representation "id_permission"
                     :description nil
                     :component-type "l"
                     :column-type "bigint(120) unsigned"
                     :private? false
                     :editable? true
                     :key-table "permission"}]}})


(defmacro cond-contain [m & body]
  `(condp (fn [kk# mm#] (contains? mm# kk#)) ~m
       ~@body))

(do
  (defn adiff [m]
    (let [key-comparator (fn [k m])]
      (cond-contain
       m
       :table (println "Table can not be change after relation")
       :id (println "Table id can not be changed totaly")
       :prop (let [m-prop (:prop m)]
               (cond-contain m-prop
                             :table (let [t-prop (:table m-prop)]
                                      (cond-contain t-prop
                                                    :frontend-name #(assoc-in %1 [:prop :table :frontend-name] ((comp :frontend-name :table :prop) %2))
                                                    ;; :is-system? false
                                                    ;; :allow-modifing? false
                                                    ;; :allow-deleting? true
                                                    ;; :allow-linking? true
                                                    ))
                             :columns (let [c-columns (:columns m-prop)]
                                        ))))))
  (adiff {:prop {:columns [nil nil {:private? false}], :table {:allow-modifing? true}}} ) )



(data/diff u1 u2)
({:prop {:columns [nil nil {:private? false}], :table {:allow-modifing? true}}}
 {:prop {:columns [nil nil {:private? true}], :table {:allow-modifing? false}}}
 {:prop {:columns [{:field "login", :representation "login", :description nil, :component-type "i", :column-type "varchar(100)", :private? false, :editable? true} {:field "password", :representation "password", :description nil, :component-type "i", :column-type "varchar(100)", :private? false, :editable? true} {:editable? true, :column-type "varchar(100)", :component-type "i", :description nil, :representation "first_name", :field "first_name"}  {:field "last_name", :representation "last_name", :description nil, :component-type "i", :column-type "varchar(100)", :private? false, :editable? true} {:field "id_permission", :representation "id_permission", :description nil, :component-type "l", :column-type "bigint(120) unsigned", :private? false, :editable? true, :key-table "permission"}], :table {:allow-linking? true, :allow-deleting? true, :is-linker? false, :is-system? false, :frontend-name "user"}}, :table "user", :id 30})

;; {:prop {:columns [nil nil {:private? true}]
;;         :table   {:allow-modifing? false}}}

;; {:field "id_permission"
;;  :representation "id_permission"
;;  :description nil
;;  :component-type "l"
;;  :column-type "bigint(120) unsigned"
;;  :private? false
;;  :editable? true
;;  :key-table "permission"}




(data/diff [{:field "last_name",
             :representation "NEW NAME",
             :description nil, :component-type "i",
             :column-type "varchar(100)",
             :private? false,
             :editable? true}
            {:field "id_permission",
             :representation "id_permission",
             :description nil,
             :component-type "l",
             :column-type "bigint(120) unsigned",
             :private? false,
             :editable? true,
             :key-table "permission"}]
           ;; original
           [{:field "last_name",
             :representation "last_name",
             :description nil, :component-type "i",
             :column-type "varchar(100)",
             :private? false,
             :editable? true}
            {:field "id_permission",
             :representation "id_permission",
             :description nil,
             :component-type "l",
             :column-type "bigint(120) unsigned",
             :private? false,
             :editable? true,
             :key-table "permission"}])
 [[{:representation "NEW NAME"}] [{:representation "last_name"}] [{:editable? true, :private? false, :column-type "varchar(100)", :component-type "i", :description nil, :field "last_name"} {:field "id_permission", :representation "id_permission", :description nil, :component-type "l", :column-type "bigint(120) unsigned", :private? false, :editable? true, :key-table "permission"}]]

(.indexOf [1 2 3 4 5 6 nil] nil)


