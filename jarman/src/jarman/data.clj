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
;; (def ^:dynamic sql-connection {:dbtype "mysql" :host "127.0.0.1" :port 3306 :dbname "" :user "root" :password "123"})

(def *available-mariadb-engine-list* "set of available engines for key-value tables" ["MEMORY", "InnoDB", "CSV"])

;;; 
;; (jdbc/query sql-connection "SHOW ENGINES" )
;; (jdbc/execute! sql-connection "CREATE DATABASE `ekka-test` CHARACTER SET = 'utf8' COLLATE = 'utf8_general_ci'")


(defn valid-to-view-table? [meta-table])
(defn valid-to-view-table-field? [meta-table-field])

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

(defn get-table-meta [t-name]
  {:pre [(string? t-name) (not-empty t-name)]}
  (let [tspec (last (string/split t-name #"_"))
        in?   (fn [col x] (if (string? col) (= x col) (some #(= % x) col)))]
    (condp in? tspec
      ["lk" "link" "links"]
      {:representation t-name :private? true :scallable? false :linker? true}
      {:representation t-name :private? false :scallable? true :linker? false})))

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

(defn ^clojure.lang.PersistentList update-sql-by-id-template
  ([table m]
   (if (:id m)
     (update table :set (dissoc m :id) :where (= :id (:id m)))
     (insert table :values (vals m)))))

(defn create-meta []
  (for [table (filter is-not-metatable? (map (comp second first) (jdbc/query sql-connection "SHOW TABLES" )))]
    (let [meta (jdbc/query sql-connection (select :METADATA :where (= :table table)))]
      (if (empty? meta)
        (jdbc/execute! sql-connection (update-sql-by-id-template "METADATA"
                                    {:id nil
                                     :table table
                                     :prop (str {:table (get-table-meta table)
                                                 :columns (vec (map get-table-field-meta
                                                                    (filter #(not= "id" (:field %))
                                                                            (jdbc/query sql-connection (show-table-columns table)))))})}))))))
(create-meta)


(defn clear-meta []
  (for [table (jdbc/query sql-connection (select :METADATA))]
    (delete :METADATA :where (= :table (get table :table)))))

(let [x (first (doseq [table (filter is-not-metatable? (map (comp second first) (jdbc/query sql-connection "SHOW TABLES" )))]
                (let [meta (jdbc/query sql-connection (select :METADATA :where (= :table table)))]
                  (if (empty? meta)
                    {:id nil
                     :table table
                     :prop (str {:table (get-table-meta table)
                                     :columns (vec (map get-table-field-meta
                                                        (filter #(not= "id" (:field %))
                                                                (jdbc/query sql-connection (show-table-columns table)))))})}
                    (first meta)))))]
  (jdbc/execute! sql-connection (update-sql-by-id-template "METADATA" x)))



(let [x {:id nil, :table "cache_register", :prop "{}"}]
  (jdbc/execute! sql-connection (update-sql-by-id-template "METADATA" x)))

(jdbc/execute! sql-connection "INSERT INTO METADATA VALUES (null, \\\"cache_register\\\", \\\"{:suka \\\"bliat\\\"}\\\")")





(insert "cache_register" :values (vals (first (for [table (filter is-not-metatable? (map (comp second first) (jdbc/query sql-connection "SHOW TABLES" )))]
                                     (let [meta (jdbc/query sql-connection (select :METADATA :where (= :table table)))]
                                       (if (empty? meta)
                                         {:id nil
                                          :table table
                                          :prop (prn-str {:table (get-table-meta table)
                                                          :columns (vec (map get-table-field-meta
                                                                             (filter #(not= "id" (:field %))
                                                                                     (jdbc/query sql-connection (show-table-columns table)))))})}
                                         (first meta)))))))

(defn ^clojure.lang.PersistentList update-sql-by-id-template
  ([table m]
   (if (:id m)
     (update table :set m :where (= :id (:id m)))
     (insert table :values (vals m)))))




(update :bliat :values )
(jdbc/execute! sql-connection (update-sql-by-id-template "METADATA"
                                                          ))


(jdbc/execute! sql-connection (update-sql-by-id-template "METADATA"
                                                         {:id nil
                                                          :table {:representation "cache_register", :private? false, :scallable? true, :linker? false}
                                                          :prop "[{:f 1}]\n"} ))




