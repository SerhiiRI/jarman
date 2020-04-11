(ns jarman.data
  (:require
   [jarman.sql-tool :as toolbox :include-macros true :refer :all]
   [clojure.string :as string]
   [clojure.java.jdbc :as jdbc]))

(defn is-metatable? [t-name]
  (let [t (string/lower-case t-name)]
    (or (= t "metadata") (= (take 4 t) (seq "meta")))))
(defn is-not-metatable? [t-name]
  (not (is-metatable? t-name)))

(defn is-id-col? [col-name]
  (let [col (string/lower-case col-name)]
    (or (= col "id")
        (= (take 2 col) '(\i \d)))))

(defn is-not-id-col? [col-name]
      (not (is-id-col? col-name)))

(def ^:dynamic sql-connection {:dbtype "mysql" :host "127.0.0.1" :port 3306 :dbname "ekka-test" :user "root" :password "123"})


(def *available-mariadb-engine-list* "set of available engines for key-value tables" ["MEMORY", "InnoDB", "CSV"])
(jdbc/query sql-connection "SHOW ENGINES" )




(first (map #(jdbc/query sql-connection (format "SHOW COLUMNS FROM %s" %1)) ["cache_register" "entrepreneur" "permission" "point_of_sale" "point_of_sale_group" "point_of_sale_group_links" "repair_contract" "seals" "service_contract" "user"]))

{:table    {:representation "Kasa fiskalna" :private? true :scallable true :linker? true}
 :columns [{:field "is_working", :representation "Działający" :description "wkaź czy kasa dziłająca" :comp-type "varchar(100)" :col-type "s" :private? false :editable true }
           {:field "id_point_of_sale", :representation "Punkt sprzedarzy" :description nil :type "s" :private? true :editable true}]}

;; ({:field "id", :type "bigint(20) unsigned", :null "NO", :key "PRI", :default nil, :extra "auto_increment"}
;;  {:field "id_point_of_sale", :type "bigint(20) unsigned", :null "YES", :key "MUL", :default nil, :extra ""}
;;  {:field "name", :type "varchar(100)", :null "YES", :key "", :default nil, :extra ""}
;;  {:field "serial_number", :type "varchar(100)", :null "YES", :key "", :default nil, :extra ""}
;;  {:field "fiscal_number", :type "varchar(100)", :null "YES", :key "", :default nil, :extra ""}
;;  {:field "manufacture_date", :type "date", :null "YES", :key "", :default nil, :extra ""}
;;  {:field "first_registration_date", :type "date", :null "YES", :key "", :default nil, :extra ""}
;;  {:field "is_working", :type "tinyint(1)", :null "YES", :key "", :default nil, :extra ""}
;;  {:field "version", :type "varchar(100)", :null "YES", :key "", :default nil, :extra ""}
;;  {:field "id_dev", :type "varchar(100)", :null "YES", :key "", :default nil, :extra ""}
;;  {:field "producer", :type "varchar(100)", :null "YES", :key "", :default nil, :extra ""}
;;  {:field "modem", :type "varchar(100)", :null "YES", :key "", :default nil, :extra ""}
;;  {:field "modem_model", :type "varchar(100)", :null "YES", :key "", :default nil, :extra ""}
;;  {:field "modem_serial_number", :type "varchar(100)", :null "YES", :key "", :default nil, :extra ""}
;;  {:field "modem_phone_number", :type "varchar(100)", :null "YES", :key "", :default nil, :extra ""})

(defn valid-to-view-table? [meta-table])
(defn valid-to-view-table-field? [meta-table-field])

(reduce (fn [acc t] (into acc {t (jdbc/query sql-connection (format "SHOW COLUMNS FROM %s" t))})) {} ["cache_register" "entrepreneur" "permission" "point_of_sale" "point_of_sale_group" "point_of_sale_group_links" "repair_contract" "seals" "service_contract" "user"])

{:table    {:representation "Kasa fiskalna" :private? true :scallable true :linker? true}
 :columns [{:field "is_working", :representation "Działający" :description "wkaź czy kasa dziłająca" :comp-type "varchar(100)" :col-type "s" :private? false :editable true }
           {:field "id_point_of_sale", :representation "Punkt sprzedarzy" :description nil :type "s" :private? true :editable true}]}



(defrecord Meta [table columns])
(defrecord MetaTable [representation private? scallable linker?])
(defrecord MetaTableColumn [field representation description comp-type col-type private? editable])



{:field "manufacture_date", :type "date", :null "YES", :key "", :default nil, :extra ""}



(defn get-table-meta [t-name]
  {:pre [(string? t-name) (not-empty t-name)]}
  (let [tspec (last (string/split t-name #"_"))
        in?   (fn [col x] (if (string? col) (= x col) (some #(= % x) col)))]
    (condp in? tspec
      ["lk" "link" "links"]
      {:representation t-name :private? true :scallable? false :linker? true}
      {:representation t-name :private? false :scallable? true :linker? false})))

;; => {:representation "table_link", :private? true, :scallable? false, :linker? true}
;; => {:representation "table", :private? false, :scallable? true, :linker? false}

(defn get-table-field-meta [column-field-spec]
  (let [tfield (:field column-field-spec)
        ttype (:type column-field-spec)
        in?   (fn [col x] (if (string? col) (= x col) (some #(= % x) col)))]
    {:field tfield
     :representation tfield
     :description nil
     :component-type (get-component-group-by-type column-field-spec)
     :column-type ttype
     :private? false
     :editable true}))

(defn get-component-group-by-type [column-field-spec]
  (let [ctype  (re-find #"[a-zA-Z]*" (string/lower-case (:type column-field-spec)))
        cfield (:field column-field-spec)
        in?   (fn [col x] (if (string? col) (= x col) (some #(= % x) col)))]
    (if (not-empty ctype)
      (if (is-id-col? cfield) "l" ;; l - mean linking is linking column 
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

(defn ^clojure.lang.PersistentList update-sql-by-id-template
  ([table m]
   (if (:id m)
     (update table :set m :where (= :id (:id m)))
     (insert table :values (vals m)))))

(defn create-meta []
  (for [table (filter is-not-metatable? (map (comp second first) (jdbc/query sql-connection "SHOW TABLES" )))]
    (let [meta (jdbc/query sql-connection (select :METADATA :where (= :table table)))]
      (if (empty? meta)
        {:table (get-table-meta table)
         :prop (vec (map get-table-field-meta (filter #(not= "id" (:field %)) (jdbc/query sql-connection (show-table-columns table)))))}
        (first meta)))))

(first (for [table (filter is-not-metatable? (map (comp second first) (jdbc/query sql-connection "SHOW TABLES" )))]
   (let [meta (jdbc/query sql-connection (select :METADATA :where (= :table table)))]
     (if (empty? meta)
       {:id nil
        :table (get-table-meta table)
        :prop (vec (map get-table-field-meta (filter #(not= "id" (:field %)) (jdbc/query sql-connection (show-table-columns table)))))}
       (first meta)))))


(insert )


(defn ^clojure.lang.PersistentList update-sql-by-id-template
  ([table m]
   (if (:id m)
     (update table :set m :where (= :id (:id m)))
     (insert table :values (vals m)))))

(jdbc/execute! sql-connection (update-sql-by-id-template "METADATA"
                                                         {:id nil
                                                          :table {:representation "cache_register", :private? false, :scallable? true, :linker? false}
                                                          :prop "[{:field \"id_point_of_sale\", :representation \"id_point_of_sale\", :description nil, :component-type \"l\", :column-type \"bigint(20) unsigned\", :private? false, :editable true} {:field \"name\", :representation \"name\", :description nil, :component-type \"i\", :column-type \"varchar(100)\", :private? false, :editable true} {:field \"serial_number\", :representation \"serial_number\", :description nil, :component-type \"i\", :column-type \"varchar(100)\", :private? false, :editable true} {:field \"fiscal_number\", :representation \"fiscal_number\", :description nil, :component-type \"i\", :column-type \"varchar(100)\", :private? false, :editable true} {:field \"manufacture_date\", :representation \"manufacture_date\", :description nil, :component-type \"d\", :column-type \"date\", :private? false, :editable true} {:field \"first_registration_date\", :representation \"first_registration_date\", :description nil, :component-type \"d\", :column-type \"date\", :private? false, :editable true} {:field \"is_working\", :representation \"is_working\", :description nil, :component-type \"b\", :column-type \"tinyint(1)\", :private? false, :editable true} {:field \"version\", :representation \"version\", :description nil, :component-type \"i\", :column-type \"varchar(100)\", :private? false, :editable true} {:field \"id_dev\", :representation \"id_dev\", :description nil, :component-type \"l\", :column-type \"varchar(100)\", :private? false, :editable true} {:field \"producer\", :representation \"producer\", :description nil, :component-type \"i\", :column-type \"varchar(100)\", :private? false, :editable true} {:field \"modem\", :representation \"modem\", :description nil, :component-type \"i\", :column-type \"varchar(100)\", :private? false, :editable true} {:field \"modem_model\", :representation \"modem_model\", :description nil, :component-type \"i\", :column-type \"varchar(100)\", :private? false, :editable true} {:field \"modem_serial_number\", :representation \"modem_serial_number\", :description nil, :component-type \"i\", :column-type \"varchar(100)\", :private? false, :editable true} {:field \"modem_phone_number\", :representation \"modem_phone_number\", :description nil, :component-type \"i\", :column-type \"varchar(100)\", :private? false, :editable true}]\n"} ))




