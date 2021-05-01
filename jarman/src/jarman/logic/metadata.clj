;; Description
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
;; Properties `prop` data
;;   {:id 1
;;    :table "cache_register"
;;    :prop {:table {:representatoin "user"
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
;;                      :editable? false}
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
;;    :is-linker? - specifing table which created to bind other table with has N to N relations to other.
;;    :is-system? - mark this table as system table.
;;    :allow-modifing? - if it false, program not allowe to extending or reducing column count. Only for UI. 
;;    :allow-modifing? - if true, permit user to modify of column specyfication(adding, removing, changing type)
;;    :allow-linking? - if true, than GUI must give user posible way to adding relation this data table to other.
;;
;;   Short meta description for columns
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
;;  UI FAQ
;;    1. I want change column-type (not component-type)?
;;        Then user must delete column and create new to replace it
;;    2. I want change component-type
;;        `TODO` for gui must be realized "type-converter" field rule, for example you can make string from data, but not in reverse direction.
;;        This library no detected column-type changes. 
(ns jarman.logic.metadata
  (:refer-clojure :exclude [update])
  (:require
   [clojure.data :as data]
   [clojure.string :as string]
   [jarman.logic.sql-tool :as toolbox :include-macros true :refer :all]
   [jarman.config.storage :as storage]
   [jarman.config.environment :as env]
   [jarman.tools.lang :refer :all]
   [jarman.logic.connection :as db])
  (:import (java.util Date)
           (java.text SimpleDateFormat)))

;;;;;;;;;;;;;;;;;;;;;;
;;; Configurations ;;; 
;;;;;;;;;;;;;;;;;;;;;;

(def ^{:dynamic true :private true} *not-allowed-to-edition-tables* ["user" "permission"])

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
      (if (not-empty (allowed-rules *id-collumn-rules* [cfield])) ["l"] ;; l - mean linking is linking column 
          (condp in? ctype
            "date"        ["d" "dt" "i"] ;; datetime
            "time"        ["t" "i"] ;; only time
            "datetime"    ["dt" "d" "i"] ;; datatime
            ["smallint"
             "mediumint"
             "int"
             "integer"
             "bigint"
             "double"
             "float"
             "real"]       ["n" "i"] ;; n - mean simple number input
            ["tinyint"
             "bool"
             "boolean"]    ["n" "i" "b"] ;; b - mean boolean
            ["tinytext"
             "text"
             "mediumtext"
             "longtext"
             "json"]       ["a"] ;; a - mean area, text area
            "varchar"      ["i"] ;; i - mean simple text input
            nil)))))

;; `TODO` edd field to doc
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
    (create-on-delete-on-update-action \"ON DELETEdasf SET NULL\")
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

(defn- get-meta-constrants [table]
  (let [table (name table)]
    (let [a (-> (db/query (format "show create table %s" table))
                first seq second second (clojure.string/split #"\n"))]
      (if-let [c (->> a (map string/trim) (filter #(string/starts-with? % "CONSTRAINT")) first)]
        (parse-constrants c)))))

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
        set_key (fn [m] (if-let [[_ table] (re-matches #"id_(.*)" tfield)]
                         (assoc m :key-table table) m))
        set_foreign-keys (fn [m] (if-let [f-key (get foreign-keys tfield nil)]
                                  (assoc m :foreign-keys f-key) m))
        in?   (fn [col x] (if (string? col) (= x col) (some #(= % x) col)))]
    (-> {:field (keyword tfield)
         :field-qualified (keyword (str (name table-name) "." (name tfield)))
         :representation tfield
         :description nil
         :component-type (get-component-group-by-type column-field-spec)
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
  (letfn [(serialize [m] (clojure.core/update m :prop #(str %)))]
    (if (:id m)
      (update table :set (serialize (dissoc m :id)) :where [:= :id (:id m)])
      (insert table :values (vals (serialize m))))))

(defn show-tables []
  (not-allowed-rules ["metatable" "meta*"] (map (comp second first) (db/query "SHOW TABLES"))))

(defn do-create-meta []
  (for [table (show-tables)]
    (let [meta (db/query (select :metadata :where [:= :metadata.table table]))]
      (if (empty? meta)
        (db/exec (update-sql-by-id-template "metadata" (get-meta table)))))))

(defn do-clear-meta [& body]
  {:pre [(every? string? body)]}
  (db/exec (delete :metadata)))
;; (do-clear-meta)
;; (do-create-meta)
(defn update-meta [metadata]
  (db/exec (update-sql-by-id-template "metadata" metadata)))

(def  ^:private --loaded-metadata (ref nil))
(defn ^:private swapp-metadata [metadata-list]
  (dosync (ref-set --loaded-metadata metadata-list)))

(defn getset
  "get metadate deserialized information for specified tables.
        
  Example 
    (getset \"user\") ;=> [{:id 1 :table...}...]"
  [& tables]
  (let [metadata
        (mapv (fn [meta] (clojure.core/update meta :prop read-string))
              (db/query
                          (if (empty? tables)
                            (select :metadata)
                            (select :metadata
                                    :where (or-v (mapv (fn [x] [:= :metadata.table (name x)]) tables))))))]
    (if (empty? tables)
      (do (swapp-metadata metadata) metadata)
      metadata)))

(defn getset! [& tables]
  (when-not @--loaded-metadata
    (swapp-metadata (getset)))
  (if-not tables @--loaded-metadata
          (let [tables (map name tables)]
            (vec (filter #(in? tables (:table %)) @--loaded-metadata)))))


;;; Make references 
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

(defn- --recur-make-references [meta-list table-name & {:keys [back-ref]}]
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

(defn do-create-references []
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
   (isset? m [:representation] (conj p :representation))
   (isset? m [:column-type] (conj p :column-type))
   (isset? m [:component-type] (conj p :component-type))
   (isset? m [:private?] (conj p :private?))
   (isset? m [:editable?] (conj p :editable?))
   (repattern? #"^[a-z_]{3,}$" m [:field] (conj p :field))
   (repattern? #"^[\w\d\s]+$" m [:representation] (conj p :representation))
    ;; (inpattern? ["d" "t" "dt" "l" "n" "b" "a" "i" nil] m [:component-type] (conj p :component-type))
   (fpattern? #(every? (fn [cols] (in? ["d" "t" "dt" "l" "n" "b" "a" "f" "i"] cols)) %) "component-type not in allowed [\"d\" \"t\" \"dt\" \"l\" \"n\" \"b\" \"a\" \"i\" \"f\" nil]" m [:component-type] (conj p :component-type))
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


(def user-original {:id 30
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
(def user-changed {:id 30
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
             (if (do-sql (alter-table (:table original) :drop-column (:field field)))
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
        (if (do-sql (alter-table (:table original) :add-column {(:field field) [(:column-type field)]}))
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

;;;;;;;;;;;;;;;;
;;; On meta! ;;;
;;;;;;;;;;;;;;;;

(let
    [d [{:field :login, :field-qualified :user.login, :representation "Login", :description "?????? ??????????", :component-type ["i"], :column-type [:varchar-100 :nnull], :private? false, :editable? true}
      {:field :password, :field-qualified :user.password, :representation "password", :description nil, :component-type ["i"], :column-type [:varchar-100 :nnull], :private? false, :editable? true}
      
      {:field :first_name, :field-qualified :user.first_name, :representation "first_name", :description nil, :component-type ["i"], :column-type [:varchar-100 :nnull], :private? false, :editable? true}
      
      {:field :last_name, :field-qualified :user.last_name, :representation "last_name", :description nil, :component-type ["i"], :column-type [:varchar-100 :nnull], :private? false, :editable? true}
      
      {:description nil, :private? false,
       :editable? true, :field :id_permission,
       :column-type [:bigint-120-unsigned :nnull],
       :foreign-keys [{:id_permission :permission} {:delete :cascade, :update :cascade}]
       :component-type ["l"]
       :representation "id_permission"
       :field-qualified :user.id_permission
       :key-table "permission"}]]
  (sequence (comp (map :foreign-keys) (filter :foreign-keys)) d))

(eduction (filter :a) (map :a) [{:a 1} {:b 2 :a 2} {:c 3}])

(->> [{:a 1} {:b 2 :a 2} {:c 3}]
     (filter :a)
     (map :a)
     )

(defn create-table-by-meta [metadata]
  (let [smpl-fields (filter (comp (partial not-allowed-rules ["meta*"]) :field) ((comp :columns :prop) metadata))
        idfl-fields (filter (comp (partial allowed-rules "id_*") :field) ((comp :columns :prop) metadata))
        fkeys-fields (vec (eduction (filter :foreign-keys) (map :foreign-keys) idfl-fields))]
    ;; (println (format "--- Create Table %s ---" (:table metadata)))
    ;; (clojure.pprint/pprint
    ;;  {:table-name (keyword (:table metadata))
    ;;   :columns (vec (map (fn [sf] {(keyword (:field sf)) (:column-type sf)}) smpl-fields))
    ;;   :foreign-keys fkeys-fields})
    (create-table
     {:table-name (keyword (:table metadata))
      :columns (vec (map (fn [sf] {(keyword (:field sf)) (:column-type sf)}) smpl-fields))
      :foreign-keys fkeys-fields})))


;;; TODO unit test
;; (create-table-by-meta (first (getset "user")))
;; (create-table :user
;;               :columns [{:login [:varchar-100 :nnull]}
;;                         {:password [:varchar-100 :nnull]}
;;                         {:first_name [:varchar-100 :nnull]}
;;                         {:last_name [:varchar-100 :nnull]}
;;                         {:id_permission [:bigint-120-unsigned :nnull]}]
;;               :foreign-keys [{:id_permission :permission} {:delete :cascade :update :cascade}])
;; ----- CREATE TABLE 
;; ----- From script 
;; => "CREATE TABLE IF NOT EXISTS `user` (`id` BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT, `login` VARCHAR(100) NOT NULL, `password` VARCHAR(100) NOT NULL, `first_name` VARCHAR(100) NOT NULL, `last_name` VARCHAR(100) NOT NULL, `id_permission` BIGINT(120) UNSIGNED NOT NULL, PRIMARY KEY (`id`), KEY `user17738` (`id_permission`), CONSTRAINT `user17738` FOREIGN KEY (`id_permission`) REFERENCES `permission` (`id`) ON DELETE CASCADE ON UPDATE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;"
;; ----- From meta
;; => "CREATE TABLE IF NOT EXISTS `user` (`id` BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT, `login` VARCHAR(100) NOT NULL, `password` VARCHAR(100) NOT NULL, `first_name` VARCHAR(100) NOT NULL, `last_name` VARCHAR(100) NOT NULL, `id_permission` BIGINT(120) UNSIGNED NOT NULL, PRIMARY KEY (`id`), KEY `user17760` (`id_permission`), CONSTRAINT `user17760` FOREIGN KEY (`id_permission`) REFERENCES `permission` (`id`) ON DELETE CASCADE ON UPDATE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;"

;; (create-table :point_of_sale_group_links
;;               :columns [{:id_point_of_sale_group [:bigint-20-unsigned :default :null]}
;;                         {:id_point_of_sale [:bigint-20-unsigned :default :null]}]
;;               :foreign-keys [[{:id_point_of_sale_group :point_of_sale_group} {:delete :cascade :update :cascade}]
;;                              [{:id_point_of_sale :point_of_sale}]])
;; (create-table-by-meta (first (getset "point_of_sale_group_links")))
;; ----- CREATE TABLE 
;; ----- From script
;; => "CREATE TABLE IF NOT EXISTS `point_of_sale_group_links` (`id` BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT, `id_point_of_sale_group` BIGINT(20) UNSIGNED DEFAULT NULL, `id_point_of_sale` BIGINT(20) UNSIGNED DEFAULT NULL, PRIMARY KEY (`id`), KEY `point_of_sale_group_links17774` (`id_point_of_sale_group`), CONSTRAINT `point_of_sale_group_links17774` FOREIGN KEY (`id_point_of_sale_group`) REFERENCES `point_of_sale_group` (`id`) ON DELETE CASCADE ON UPDATE CASCADE, KEY `point_of_sale_group_links17775` (`id_point_of_sale`), CONSTRAINT `point_of_sale_group_links17775` FOREIGN KEY (`id_point_of_sale`) REFERENCES `point_of_sale` (`id`)) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;"
;; ----- From meta
;; => "CREATE TABLE IF NOT EXISTS `point_of_sale_group_links` (`id` BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT, `id_point_of_sale_group` BIGINT(20) UNSIGNED DEFAULT NULL, `id_point_of_sale` BIGINT(20) UNSIGNED DEFAULT NULL, PRIMARY KEY (`id`), KEY `point_of_sale_group_links17799` (`id_point_of_sale_group`), CONSTRAINT `point_of_sale_group_links17799` FOREIGN KEY (`id_point_of_sale_group`) REFERENCES `point_of_sale_group` (`id`) ON DELETE CASCADE ON UPDATE CASCADE, KEY `point_of_sale_group_links17800` (`id_point_of_sale`), CONSTRAINT `point_of_sale_group_links17800` FOREIGN KEY (`id_point_of_sale`) REFERENCES `point_of_sale` (`id`)) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;"

(defn recur-find-path [ml]
  {:tbl ((comp :field :table :prop) ml)
   :ref (if ((comp :front-references :ref :table :prop) ml)
          (mapv #(recur-find-path (first (getset! %)))
                ((comp :front-references :ref :table :prop) ml)))})

;;;;;;;;;;;;;;;;;;;;;;;
;;; METADATA BACKUP ;;;
;;;;;;;;;;;;;;;;;;;;;;;

(def ^:private backup-name "metadata")
(def ^:private backup-file-name (format "%s.edn" backup-name))
;; (defn- backup-keep-10-last-modified
;;   "Remove 10 last modified backup files, when new backups being created"[]
;;   (let [max-bkp 10 l-files (storage/user-metadata-list) c-files (count l-files)]
;;     (if (> c-files max-bkp) 
;;       (doall (map #(-> % .getName storage/user-metadata-delete)
;;                   (take (- c-files max-bkp) 
;;                         (sort-by #(-> % .lastModified Date.)
;;                                  #(.before %1 %2)
;;                                  (map clojure.java.io/file l-files))))))))

(defn make-backup-metadata
  "Description
    Make backup files 'metadata.edn', if file was created
    before, rename and add timestamp to name in format
    YYYY-MM-dd_HHmmss 'metadata_2021-03-22_004353.edn'

  Example
    (make-backup-metadata)
  
  Warning!
    Timestamp related to event when your backup became
    a old, and replace to new. Timestamp not mean Time
    of backup creation

  See
    `backup-keep-10-last-modified` function which delete
      oldest file from all backup snapshots, if number
      of backups will reach 10 files" []
  (blet
   ;; if exist backup file, please make new one
   (if (.exists (clojure.java.io/file (storage/user-metadata-dir) backup-file-name))
     (blet (storage/user-metadata-rename backup-file-name old-backup-file)
           [old-backup-file (format "%s_%s.edn" backup-name (.format (SimpleDateFormat. "YYYY-MM-dd_HHmmss") (Date.)))]))
   ;; Store the metainformation
   (storage/user-metadata-put backup-file-name (str backup-metadata))
   ;; clean-up oldest files 
   ;; (backup-keep-10-last-modified)
   ;;; make a backup
   [metadata-list (vec (getset))
    tables-list (map :table metadata-list)
    date-format "YYYY-MM-dd HH:mm:ss"
    backup-metadata
    {:info {:date (.format (SimpleDateFormat. date-format) (Date.))
            :program-dir env/user-dir}
     :table (vec (map :table metadata-list))
     :backup metadata-list}]))

(defn- default-backup-loader []
  (if-let [_TMP0 (storage/user-metadata-get backup-file-name)] _TMP0
    (try (slurp (clojure.java.io/file env/user-dir backup-file-name))
         (catch Exception e nil))))

(defn restore-backup-metadata
  "Description
    Restore all backups from user-stored buffer

  Example
    (restore-backup-metadata)
    (restore-backup-metadata default-backup-loader)"
  ([] (restore-backup-metadata default-backup-loader))
  ([f-backup-loader]
   (if-let [backup (f-backup-loader)]
     (try (let [backup-swapped (read-string backup)
                table-list     (:table backup-swapped)
                metadata-list  (map #(assoc % :id nil) (:backup backup-swapped))
                info           (:info backup-swapped)]
            (map #(db/exec (update-sql-by-id-template "metadata" %)) metadata-list))))))

