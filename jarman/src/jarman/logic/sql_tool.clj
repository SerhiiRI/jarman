(ns jarman.logic.sql-tool
  (:gen-class)
  (:refer-clojure :exclude [update])
  (:import [java.time Period LocalDate])
  (:require
   ;; standart lib
   [clojure.string :as string]
   [clojure.java.io :as io]
   [clojure.pprint :refer [cl-format pprint]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; configuration rules ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def ^{:dynamic true :private true} *accepted-alter-table-rules* [:add-column :drop-column :drop-foreign-key :add-foreign-key :modify-column])
(def ^{:dynamic true :private true} *accepted-forkey-rules*      [:restrict :cascade :null :no-action :default])
(def ^{:dynamic true :private true} *accepted-ctable-rules*      [:columns :foreign-keys :table-config])
(def ^{:dynamic true :private true} *accepted-select-rules*      [:top :count :column :table_name :inner-join :right-join :left-join :outer-left-join :outer-right-join :where :order :limit])
(def ^{:dynamic true :private true} *accepted-update-rules*      [:update-table :set :where])
(def ^{:dynamic true :private true} *accepted-insert-rules*      [:column-list :values :set])
(def ^{:dynamic true :private true} *accepted-delete-rules*      [:from :where])

(def ^{:dynamic true :private true} *data-format* "DB date format" "yyyy-MM-dd HH:mm:ss")
(def ^{:dynamic true :private true} *namespace-lib* "" "jarman.logic.sql-tool")
(def ^{:dynamic true} *debug*      "Enable debugging" true)
(def ^{:dynamic true} *debug-to*   "Enable debugging" (second [:output :file]))
(def ^{:dynamic true} *debug-file* "Enable debugging" "./log/sql.log.clj")
(def ^{:dynamic true} *debug-full-descript*   "Enable debugging" false)

(defn- transform-namespace [symbol-op]
  (if (some #(= \/ %) (str symbol-op)) symbol-op
      (symbol (format "%s/%s" *namespace-lib* symbol-op))))

(defn- find-rule [operation-name]
  (condp = (last (string/split (name operation-name) #"/"))
    "insert"       *accepted-insert-rules*
    "delete"       *accepted-delete-rules*
    "update"       *accepted-update-rules*
    "select"       *accepted-select-rules*
    "create-table" *accepted-ctable-rules*
    "alter-table"  *accepted-alter-table-rules*
    []))

;;;;;;;;;;;;;;;;;;;;;
;;; Date function ;;;
;;;;;;;;;;;;;;;;;;;;;

(defn- is-yyyy-mm-dd? [s]
  (some? (re-matches #"\d{4}-\d{1,2}[-]\d{1,2}" (string/trim s))))
(defn count-month-between
  "Description
    return a mounth count between start end date.

  Warning!
    function supply only date in YYYY-MM?-DD? notation

  Example
    (count-month-between \"2017-08-31\" \"2018-11-30\") ;; => 15
    (count-month-between \"2019-08-31\" \"2018-11-30\") ;; => -9"
  [start-date end-date]
  {:pre [(is-yyyy-mm-dd? start-date) (is-yyyy-mm-dd? start-date)]}
  (-> (Period/between
       (-> (LocalDate/parse (string/trim start-date)) (.withDayOfMonth 1))
       (-> (LocalDate/parse (string/trim   end-date)) (.withDayOfMonth 1)))
      .toTotalMonths))

(defn date
  "Remember that simple (date) ruturn current
  date and time.
  Also if you 

  Example:
  (date 1900 11 29 1 2 3) => 1900-12-29 01:02:03
  (date 1900 11 29 1 2)   => 1900-12-29 01:02:00
  (date 1900 11 29 1)     => 1900-12-29 01:00:00
  (date 1900 11 29)       => 1900-12-29 00:00:00
  (date 1900 11)          => 1900-12-01 00:00:00
  (date 1900)             => 1900-01-01 00:00:00
  (date)                  => 2020-02-29 19:07:41"
  ([] (.format (java.text.SimpleDateFormat. "YYYY-MM-dd HH:mm:ss") (java.util.Date.)))
  ([YYYY] (date YYYY 0 1 0 0 0))
  ([YYYY MM] (date YYYY MM 1 0 0 0))
  ([YYYY MM dd] (date YYYY MM dd 0 0 0))
  ([YYYY MM dd hh] (date YYYY MM dd hh 0 0))
  ([YYYY MM dd hh mm] (date YYYY MM dd hh mm 0))
  ([YYYY MM dd hh mm ss] (.format (java.text.SimpleDateFormat. "YYYY-MM-dd HH:mm:ss")
                                  (java.util.Date. (- YYYY 1900) MM dd hh mm ss))))

(spit *debug-file* (format ";; S-SQL jarman log session\n;; Timestamp: %s\n\n" (date)))

(defn date-object
  "Remember that simple (date) ruturn current
  date and time.
  Also if you 

  Example:
  (date 1900 11 29 1 2 3) => 1900-12-29 01:02:03
  (date 1900 11 29 1 2)   => 1900-12-29 01:02:00
  (date 1900 11 29 1)     => 1900-12-29 01:00:00
  (date 1900 11 29)       => 1900-12-29 00:00:00
  (date 1900 11)          => 1900-12-01 00:00:00
  (date 1900)             => 1900-01-01 00:00:00
  (date)                  => 2020-02-29 19:07:41"
  ([] (java.util.Date.))
  ([YYYY] (date-object YYYY 0 1 0 0 0))
  ([YYYY MM] (date-object YYYY MM 1 0 0 0))
  ([YYYY MM dd] (date-object YYYY MM dd 0 0 0))
  ([YYYY MM dd hh] (date-object YYYY MM dd hh 0 0))
  ([YYYY MM dd hh mm] (date-object YYYY MM dd hh mm 0))
  ([YYYY MM dd hh mm ss] (java.util.Date. (- YYYY 1900) MM dd hh mm ss)))

(defn date-format
  "Remember that simple (date) ruturn current
  date and time.
  Also if you 

  Example:
  (date 1900 11 29 1 2 3) => 1900-12-29 01:02:03
  (date 1900 11 29 1 2)   => 1900-12-29 01:02:00
  (date 1900 11 29 1)     => 1900-12-29 01:00:00
  (date 1900 11 29)       => 1900-12-29 00:00:00
  (date 1900 11)          => 1900-12-01 00:00:00
  (date 1900)             => 1900-01-01 00:00:00
  (date)                  => 2020-02-29 19:07:41"
  ([] (.format (java.text.SimpleDateFormat. "YYYY-MM-dd HH:mm:ss") (java.util.Date.)))
  ([^java.util.Date java-util-date] (.format (java.text.SimpleDateFormat. "YYYY-MM-dd HH:mm:ss") java-util-date)))

(defn date-to-obj
  "Make shure, that your date in *data-format*

  Example: 
    (date-to-obj \"1998-10-10 05:11:22\") ;=> java.util.Date...."
  [^java.lang.String data-string]
  (.parse (java.text.SimpleDateFormat. *data-format*) data-string))

(defn timestamp-to-date
  "java.sql.timestamp class to date"
  [^java.sql.Timestamp tstamp] (date-to-obj (.format (java.text.SimpleDateFormat. "YYYY-MM-dd HH:mm:ss") tstamp)))

;;;;;;;;;;;;;;;;;;;;;;;
;;; String helperts ;;;
;;;;;;;;;;;;;;;;;;;;;;;

(defn- tf-t-f-verify [table-field table]
  (let [t-f (string/split (name table-field) #"\.")
        kv-t-f (mapv keyword t-f)]
    (if (and
         (= 2 (count kv-t-f))
         (= (first kv-t-f) (keyword table)))
      kv-t-f
      nil)))

(defn- make-dot-column
  ([field] (format "`%s`" (name field)))
  ([field table]
   (if-let [[t f] (tf-t-f-verify field table)]
     (format "`%s`.`%s`" (name t) (name f))
     (format "`%s`" (name field)))))

(defn- tf-t-f [table-field]
  (let [t-f (string/split (name table-field) #"\.")]
    (mapv keyword t-f)))

(defn- t-f-tf [table field]
  (keyword (str (name table) "." (name (name field)))))

(defn- make-dot-column!
  ([field] (string/join "."(map (comp (partial format "`%s`") name) (tf-t-f field)))))

(defn- pair-where-pattern
  "Constuct key-value string view for SQL language parametrized queries
  Example:
  (pair-where-pattern :TABLE :name \"value\")
  ;;=> TABLE.name=\"value\"
  (pair-where-pattern :TABLE :some-bool true)
  ;;=> TABLE.some-bool=true
  "
  ([key value table] (str (symbol table) "." (pair-where-pattern key value)))
  ([key value] (format (cond
                         (string? value) "%s=\"%s\""
                         (or (boolean? value) (number? value)) "%s=%s"
                         :else "%s=%s") (symbol key) value)))

(defn- tkey
  "Function split dot-linked keyword name
  and return array of string, divided on <.>
  character
  :table.value => ('table' 'value')"
  [k] (string/split (str (symbol k)) #"\."))


;;;;;;;;;;;;;;;;;;
;;; SQL Helper ;;;
;;;;;;;;;;;;;;;;;;

(defn- get-table
  "Example
     (get-table :user) ;; => \"user\"
     (get-table \"user\") ;; => \"user\"
     (get-table {:user :u}) ;; => \"u\""
  [table-name]
  (cond
    (keyword? table-name) (name table-name)
    (string?  table-name) (name table-name)
    (map? table-name) (if-let [t-name (not-empty (second (map name (first table-name))))]
                        t-name
                        (throw (Exception. "the `:table_name` has bad syntax [k, s, {k|s k|s}")))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Joining preprocessor ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- join-alicing
  "(join-alicing :user*u.id_permission)
   ;; => {:table \"user\", :alias \"u\", :fkey \"id_permission\"}
   (join-alicing :user)
   ;; => {:table \"user\", :alias \"user\", :fkey nil}
   (join-alicing :user*u)
   ;; => {:table \"user\", :alias \"u\", :fkey nil}
   (join-alicing :user.id_permission)
   ;; => {:table \"user\", :alias \"user\", :fkey \"id_permission\"}"
  [k]
  (if-let [[_ table alias fkey] (re-matches #"(?:([\w_]+)(?:\*([\w_]+))?)(?:\.([\w_]+))?" (name k))]
    {:table table :alias (if-not alias table alias) :fkey fkey}))

(defn- table-join-as [m]
  (if (= (:table m) (:alias m))
    (format "`%s`" (:table m)(:alias m))
    (format "`%s` `%s`" (:table m)(:alias m))))

(defn- table-join-fkey [m & [table]]
  (if (some? (:fkey m))
    (format ".`%s`" (:fkey m))
    (format ".`%s`" (if table (format "`id_%s`" table) "`id`"))))

(defn- table-join-t-k [m & [table]]
  (format "`%s`.`%s`"
          (:alias m)
          (if (some? (:fkey m))
            (:fkey m)
            (if table (format "id_%s" (name table)) "id"))))

(defn- join-keyword-string [main-table joining-table]
 (if-let [[_ l direction r](re-matches #"([\w\.\*_]+)(->|<-)([\w\*\._]+)" (name joining-table))]
   (let [[mr ml] [(join-alicing r) (join-alicing l)]
         [tr tl] [(partial table-join-t-k mr) (partial table-join-t-k ml)]]
     (case direction
       "->" (format "%s ON %s=%s" (table-join-as mr) (tl (:alias mr)) (tr))
       "<-" (format "%s ON %s=%s" (table-join-as ml) (tr) (tl (:alias mr)))))))

(defn- join-string-string [main-table on-join-construction]
  on-join-construction)

(defn- get-function-by-join-type [join]
  (cond
    (keyword? join) join-keyword-string
    (string? join) join-string-string))

(defn- rule-joiner [rule join-string-or-join-list]
  (format "%s %s" rule
          (if (sequential? join-string-or-join-list)
            (string/join (str " " rule " ") join-string-or-join-list)
            join-string-or-join-list)))

(defmacro ^:private define-joinrule [rule-name]
  (let [rule-array (string/split (str rule-name) #"\-")  rule-lenght (- (count rule-array) 1)
        rule-keyword (keyword (string/join "-"(take rule-lenght rule-array)))
        rule-string (string/join " " (map string/upper-case (take rule-lenght rule-array)))]
    `(defn- ~rule-name [~'current-string ~'joins-form ~'args]
       (let [table-name# (get-table (:table_name ~'args))]
        (if (sequential? ~'joins-form)
          (str
           ~'current-string " "
           (string/join " "
                        (for [form# ~'joins-form
                              :let [join-function# (get-function-by-join-type form#)]
                              :while (some? join-function#)]
                          (rule-joiner ~rule-string (join-function# table-name# form#)))))
          (if-let [join-function# (get-function-by-join-type ~'joins-form)]
            (str ~'current-string " " (rule-joiner ~rule-string (join-function# table-name# ~'joins-form)))
            ~'current-string))))))

(define-joinrule inner-join-string)
(define-joinrule left-join-string)
(define-joinrule right-join-string)
(define-joinrule outer-right-join-string)
(define-joinrule outer-left-join-string)

(defn- top-string [current-string top-number _]
  (if (number? top-number)(str current-string " TOP " top-number)
       (str current-string)))

(defn- count-string
  "Example of using rule 
    :count {:distinct :column}
    :count :*
    :count :column
  Result is {:count <number>}"
  [current-string count-rule _]
  (cond
    (keyword? count-rule)
    (str current-string (format " COUNT(%s) as count" (name count-rule)))
    
    (map?     count-rule)
    (str current-string (if (:distinct count-rule)
                          (format " COUNT(DISTINCT %s) as count" (name (:distinct count-rule)))))
    
     :else (str current-string)))

(defn- limit-string [current-string limit-number _]
  (cond
     (number? limit-number) (str current-string " LIMIT " limit-number)
     (and (not (string? limit-number)) (seqable? limit-number) (let [[f s]limit-number] (and (number? f) (number? s))))
     (str current-string " LIMIT " (string/join "," limit-number))
     :else (str current-string)))

(defn- table_name-string [current-string table-name _]
  (str current-string " FROM "
       (cond
         (map? table-name) (apply (partial format "%s AS `%s`") (map name (first table-name)))
         (string? table-name) (format "`%s`" (name table-name))
         (keyword? table-name) (format "`%s`" (name table-name)))))

(defn- column-string [current-string col-vec _]
  (let [col-vec
        (if (= (first col-vec) :#as_is)
          (mapv #(if (keyword? %) {% %} %) (rest col-vec))
          col-vec)
        
        f (fn [c]
            (cond 
              (or (keyword? c)
                 (string? c)) (name c)

              (and (vector? c) (= (count c) 2))
              (let [[x y] (map #(str (symbol %)) c)]
                (format "%s AS `%s`" x y))
              
              (map? c)
              (let [[x y] (map #(str (symbol %)) (first (vec c)))]
                (format "%s AS `%s`" x y))
              
              :else nil))]

    (str current-string " " (string/join ", " (map f col-vec)))))

(defn- empty-select-string [current-string _ _]
  (str current-string " *"))

(defn- order-string [current-string args _]
  (if (vector? args)
    (let [[col asc-desc] args]
       (if (and (not-empty (name col)) (some #(= asc-desc %) [:asc :desc]))
         (string/join " " [current-string "ORDER BY"
                           ;; (format "`%s`"(name col))
                           (format "%s"(name col))
                           (string/upper-case (name asc-desc))])
         current-string))
    current-string))


;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; WHERE PREPROCESSOR ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;

(def ^{:dynamic true :private true} *where-border* false)
(defn ^{:private true} into-border [some-string]
  (if *where-border* 
    (str "(" some-string ")")
    some-string))

;;;;;;;;;;;;;;;;;;;;;;;;;
;; LIST MACROPROCESSOR ;;
;;;;;;;;;;;;;;;;;;;;;;;;;

(declare between-operator-v)
(declare define-operator-v)
(defn ^:private where-procedure-parser-v [where-clause]
  (cond (nil? where-clause) (str "null")
        (symbol? where-clause) where-clause
        (string? where-clause) (pr-str where-clause)
        (keyword? where-clause) (name where-clause) ;; (format "`%s`" (name where-clause))
        (seqable? where-clause) (let [function (first where-clause) args (rest where-clause)]
                                  (condp = function
                                    :or (into-border
                                         (string/join
                                          " OR "
                                          (vec (for [x (vec args)]
                                                 (binding [*where-border* true]
                                                   (where-procedure-parser-v x))))))
                                    :and (into-border
                                          (string/join
                                           " AND "
                                           (vec (for [x (vec args)]
                                                  (binding [*where-border* true]
                                                    (where-procedure-parser-v x))))))
                                    :> (apply define-operator-v function args)
                                    :< (apply define-operator-v function args)
                                    := (apply define-operator-v function args)
                                    :>= (apply define-operator-v function args)
                                    :<= (apply define-operator-v function args)
                                    :<> (apply define-operator-v function args)
                                    :!= (apply define-operator-v (symbol '<>) args)
                                    :like (apply define-operator-v (symbol 'LIKE) args)
                                    :in (apply define-operator-v (symbol 'LIKE) args)
                                    :between (apply between-operator-v args)
                                    (if (and (symbol? function) (resolve function))
                                      (where-procedure-parser-v where-clause)
                                      (binding [*where-border* true]
                                        (into-border (string/join ", " (vec (for [x where-clause]
                                                                              (where-procedure-parser-v x)))))))))
        :else (str where-clause)))

(defn between-operator-v [field v1 v2]
  (format "%s BETWEEN %s AND %s"
          (where-procedure-parser-v field)
          (where-procedure-parser-v v1)
          (where-procedure-parser-v v2)))

(defn ^:private define-operator-v [operator field-1 field-2]
  (string/join " " [(where-procedure-parser-v field-1)
                     (name operator)
                    (where-procedure-parser-v field-2)]))

(defn >-v [v1 v2] [:> v1 v2])
(defn <-v [v1 v2] [:< v1 v2])
(defn =-v [v1 v2] [:= v1 v2])
(defn >=-v [v1 v2] [:>= v1 v2])
(defn <=-v [v1 v2] [:<= v1 v2])
(defn <>-v [v1 v2] [:<> v1 v2])
(defn !=-v [v1 v2] [:<> v1 v2])
(defn like-v [v1 v2] [:like v1 v2])
(defn in-v [v1 v-list] [:in v1 v-list])
(defn between-v [field v1 v2] [:between field v1 v2])
(defn or-v [expr] (vec (concat [:or] (if (vector? expr) expr (vec expr)))))
(defn and-v [expr] (vec (concat [:and] (if (vector? expr) expr (vec expr)))))

;;;;;;;;;;;;;;;;;;;;;;;;;
;; LIST MACROPROCESSOR ;;
;;;;;;;;;;;;;;;;;;;;;;;;;

(defmacro ^:private and-processor-s [& args]
  (let [v (vec (for [x (vec args)]
                 `(binding [*where-border* true]
                    (where-procedure-parser-s ~x))))]
    `(into-border (string/join " AND " ~v))))

(defmacro ^:private or-processor-s [& args]
  (let [v (vec (for [x (vec args)]
                 `(binding [*where-border* true]
                    (where-procedure-parser-s ~x))))]
    `(into-border (string/join " OR " ~v))))

(defmacro ^:private where-procedure-parser-s [where-clause]
  (cond (nil? where-clause) `(str "null")
        (symbol? where-clause) where-clause
        (string? where-clause) `(pr-str ~where-clause)
        (keyword? where-clause) `(str (symbol ~where-clause)) ;;`(format "`%s`" (str (symbol ~where-clause)))
        (seqable? where-clause) (let [function (first where-clause) args (rest where-clause)]
                                  (condp = function
                                    'or `(or-processor-s ~@args)
                                    'and `(and-processor-s ~@args)
                                    '> `(define-operator-s '~function ~@args)
                                    '< `(define-operator-s '~function ~@args)
                                    '= `(define-operator-s '~function ~@args)
                                    '>= `(define-operator-s '~function ~@args)
                                    '<= `(define-operator-s '~function ~@args)
                                    '<> `(define-operator-s '~function ~@args)
                                    '!= `(define-operator-s '~(symbol '<>) ~@args)
                                    'between `(between-operator-s ~@args)
                                    'like `(define-operator-s '~(symbol 'LIKE) ~@args)
                                    'in `(define-operator-s '~(symbol 'LIKE) ~@args)
                                    'and `(define-operator-s '~(symbol 'LIKE) ~@args)
                                    (if (and (symbol? function) (resolve function))
                                      (let [result (eval where-clause)]
                                        `(where-procedure-parser-s ~result))
                                      (let [element-primitives (vec (for [x where-clause]
                                                                      `(where-procedure-parser-s ~x)))]
                                        `(binding [*where-border* true]
                                           (into-border (string/join ", " ~element-primitives)))))))
        :else (str where-clause)))

(defn- between-operator-s [field v1 v2]
  (format "%s BETWEEN %s AND %s"
          (eval `(where-procedure-parser-s ~field))
          (eval `(where-procedure-parser-s ~v1))
          (eval `(where-procedure-parser-s ~v2))))

(defn- define-operator-s [operator-s field-1 field-2]
  (string/join " " [(eval `(where-procedure-parser-s ~field-1))
                    operator-s
                    (eval `(where-procedure-parser-s ~field-2))]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; WHERE BLOCK PROCESSOR ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- where-string [s where-block _]
  (cond (string? where-block) (str s " WHERE " where-block)
        (map? where-block)    (str s " WHERE " (string/join " AND " (map #(apply pair-where-pattern %) where-block)))
        (list? where-block)   (str s " WHERE " (eval `(where-procedure-parser-s ~where-block)))
        (vector? where-block) (str s " WHERE " (where-procedure-parser-v where-block))
        :else s))

;;;;;;;;;;;;;;;;;;;
;;; column-list ;;;
;;;;;;;;;;;;;;;;;;;

(defn- column-list-string [current-string m _]
  (if (and (sequential? m) (every? (some-fn keyword? string?) m))
      (str current-string
           (format
            " (%s)" (string/join ", " (map name m))))
      current-string))

(defn- empty-insert-update-table-string [current-string m query-args]
  (str (string/trim current-string) (format " `%s`" (get-table (:table_name query-args)))))

;;;;;;;;;;;;;;;;;;;;;;;;
;;; set preprocessor ;;;
;;;;;;;;;;;;;;;;;;;;;;;;

(defn- set-string [current-string update-map _]
  (let [pair-group (fn [[col-name value]] (str (make-dot-column! col-name) "=" (where-procedure-parser-v value)))]
    (str current-string ;; " " (format "`%s`" (name tabel-name))
         (str " SET " (string/join ", " (map pair-group update-map))))))

;; (defn set-string [current-string update-map tabel-name]
;;   (let [pair-group (fn [[col-name value]] (str (format "%s" (name col-name)) "=" (where-procedure-parser-v value)))]
;;     (str current-string " " (format "%s" (name tabel-name)) (str " SET " (string/join ", " (map pair-group update-map))))))

(defn- update-table-string [current-string map query-args]
  (str current-string "" (format "%s"  (get-table (:table_name query-args)))))

(defn- low-priority-string [current-string map _]
  (str current-string " LOW_PRIORITY"))

(defn- from-string [current-string map query-args]
  (str current-string " " (format "`%s`" (get-table (:table_name query-args)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; values preprocessor ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; (defn values-string [current-string values table-name]
;;   (let [wrapp-escape    (fn [some-list] (map #(where-procedure-parser-v %) some-list))
;;         brackets        (fn [temp-strn] (str "(" temp-strn ")"))
;;         into-sql-values (fn [some-list] (brackets (string/join ", " (wrapp-escape some-list))))
;;         into-sql-map    (fn [some-list] (brackets (string/join ", " (vals (wrapp-escape some-list)))))
;;         pair-group      (fn [[col-name value]] (str (format "`%s`"(name col-name)) "=" (where-procedure-parser-v value)))]
;;     (str current-string " " (format "`%s`" (name table-name))
;;           (cond (map? values)
;;                 (str " SET " (string/join ", " (map pair-group values)))
;;                 (and (seqable? values) (map? (first values)))
;;                 (str " VALUES " (string/join ", " (map into-sql-map values)))
;;                 (and (seqable? values) (seqable? (first values)) (not (string? (first values))) (not (nil? (first values))))
;;                 (str " VALUES " (string/join ", " (map into-sql-values values)))
;;                 (seqable? values)
;;                 (str " VALUES " (into-sql-values values))
;;                 :else nil))))

(defn- values-string [current-string values _]
  (let [wrapp-escape    (fn [some-list] (map #(where-procedure-parser-v %) some-list))
        brackets        (fn [temp-strn] (str "(" temp-strn ")"))
        into-sql-values (fn [some-list] (brackets (string/join ", " (wrapp-escape some-list))))
        into-sql-map    (fn [some-list] (brackets (string/join ", " (vals (wrapp-escape some-list)))))
        pair-group      (fn [[col-name value]] (str (format "%s"(name col-name)) "=" (where-procedure-parser-v value)))]
    (str current-string ;; (format "%s" (name table-name))
          (cond (map? values)
                (str " SET " (string/join ", " (map pair-group values)))
                (and (seqable? values) (map? (first values)))
                (str " VALUES " (string/join ", " (map into-sql-map values)))
                (and (seqable? values) (seqable? (first values)) (not (string? (first values))) (not (nil? (first values))))
                (str " VALUES " (string/join ", " (map into-sql-values values)))
                (seqable? values)
                (str " VALUES " (into-sql-values values))
                :else nil))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; create-table preprocessor ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ssql-type-parser
  "Parse database column data type strings to ssql notation if needed.

  Example
    (ssql-type-parser \"bigint unsigned\")
      ;; => [:bigint-unsigned]
    (ssql-type-parser \"float(1.20) signed not null\")
      ;; => [:float-1.20-signed :nnull]
    (ssql-type-parser \"tinyint(1)\")
      ;; => [:bool]
    (ssql-type-parser \"varchar(10)\")
      ;; => [:varchar-10]
    (ssql-type-parser \"varchar(10) default \\\"empty\\\"\")
      ;; => [:varchar-10 \"default\" \"\\\"empty\\\"\"]"
  [s]
  (let [in? (fn [x c] (some #(= x %) c))
        sized-type-replace (fn [s] (clojure.string/replace s #"([a-zA-Z]+)\(([\d|\.]+)\)" "$1-$2"))
        param-type-replace (fn [s p] (clojure.string/replace s (re-pattern (format "([a-zA-Z]+-[\\d|\\.]+|[a-zA-Z]+) %s" p)) (format "$1-%s" p)))
        to-key-parameter (fn [s c] (vec (for [x (clojure.string/split s #" ")] (if-let [w (re-find #"[a-zA-Z_]+" x)] (if (in? w c) (keyword x) x)))))]
    (-> s
        (clojure.string/lower-case)
        (clojure.string/replace #"  " " ")
        (clojure.string/replace #"not null" "nnull")
        (clojure.string/replace #"tinyint\(1\)" "bool")
        (sized-type-replace)
        (param-type-replace "signed")
        (param-type-replace "unsigned")
        (param-type-replace "zerofill")
        (to-key-parameter ["null" "nnull" "date" "datetime" "time" "tinyint"
                           "smallint" "mediumint" "int" "integer" "bigint" "double"
                           "float" "real" "bool" "boolean" "tinyblob" "blob" "mediumblob"
                           "longblob" "tinytext" "text" "mediumtext" "longtext" "json"
                           "varchar" "auto_increment" "default"]))))

(defn ssql-type-formater 
  "Format column table specyficator. Example:

  Example
    :bigint-120 => BIGINT(120)
    :bigint-signed => BIGINT SIGNED
    :double-4.5 => DOUBLE(4,5)
    :bool => TINYINT(1)
    :nnul => NOT NULL
    ....
    for more, see the code `condp` block"
  [k] (if (or (string? k) (number? k)) k
          (let [[sql-type n s & _] (string/split (string/lower-case (name k)) #"-")
                is? (fn [col x] (if (string? col) (= x col) (some #(= % x) col)))
                charchain-types (fn [tt nn] (if-not nn (string/upper-case tt) (format "%s(%s)" (string/upper-case tt) nn)))
                numeral-types (fn [tt nn] (if-not nn (string/upper-case tt)
                                                  (format (if (is? ["signed" "unsigned" "zerofill"] nn) "%s %s"
                                                              (if (and s (not (empty? s)))
                                                                (if-let [_tmp (ssql-type-formater (keyword s))]
                                                                  (str "%s(%s) " _tmp)
                                                                  "%s(%s)") "%s(%s)"))
                                                          (string/upper-case tt)
                                                          (string/replace (string/upper-case nn) "." "," ))))]
            (condp is? sql-type
              "null"       "NULL"
              "nnull"      "NOT NULL"
              "date"       "DATE"
              "datetime"   "DATETIME"
              "time"       "TIME"
              
              ["tinyint" "smallint"
               "mediumint" "int"
               "integer" "bigint"
               "double" "float"
               "real"] (numeral-types sql-type n)
              
              ["bool" "boolean"] "TINYINT(1)"

              ["tinyblob" "blob" "mediumblob"  "longblob" 
               "tinytext" "text" "mediumtext" "longtext"
               "json"] (string/upper-case sql-type)

              "varchar" (charchain-types sql-type n)

              ["auto_increment" "autoincrement" "auto"] "AUTO_INCREMENT"

              ["default" "signed" "unsigned" "zerofill" ] (string/upper-case sql-type)
              nil))))

(defn- create-column
  "Description 
    Cretea column by map-typed specyfication:
  
    The key of map is column name, value - is column specyfication,
    which conctruct to SQL by `ssql-type-formater` function.
    Accepted argument forms
    
    {:id [:bigint-20 \"NOT NULL\" :auto]}  
    {:id \"bigint(20) NOT NULL AUTO_INCREMENT\"}
    {:id [:bigint-20 :nnull :auto]}
    {:id :bigint-290}"
  [map-col]
  (let [[[col-name value]] (seq map-col)]
    (cond (keyword? value) (str (format "`%s`" (name col-name)) (if-let [x (ssql-type-formater value)] (str " " x)))
          (string? value)  (str (format "`%s`" (name col-name)) (if-not (empty? value) (str " " value)))
          (seqable? value) (str (format "`%s`" (name col-name)) (let [x (string/join " " (reduce #(if-let [f (ssql-type-formater %2)] (conj %1 f) %1) [] value))]
                                                                  (if-not (empty? x) (str " " x))))
          :else "")))

;; (defn default-table-config-string [current-string _ table-name]
;;   (str current-string ") ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;"))

(defn- default-table-config-string [current-string _ _]
  (str current-string ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;"))

(defn- table-config-string
  "Get configuration map with next keys parameters
  :engine - table engine(defautl: InnoDB)
  :charset - charset for table(default: utf8)
  :collate - table collate(default: utf8_general_ci)"
  [current-string conifig-map _]
  (let [{:keys [engine charset collate]
         :or {engine "InnoDB"
              charset "utf8mb4"
              collate "utf8mb4_general_ci"}} conifig-map]
     (str current-string (format ") ENGINE=%s DEFAULT CHARSET=%s COLLATE=%s;" engine charset collate))))

(defn- columns-string
  "create columns with type data specyfications.
  Example using for `column-spec` argument:
  
  \"`id` BIGINT(150) NOT NULL AUTO_INCREMENT\"
  {:fuck [:bigint-20 \"NOT NULL\" :auto]}  
  [{:blia [:bigint-20 \"NOT NULL\" :auto]} {:suka [:varchar-210]}]
  [{:id :bigint-100} {:suka \"TINYTEXT\"}]"
  [current-string column-spec query-args]
  (str current-string (format " `%s` (" (get-table (:table_name query-args)))
        (string/join ", " [(create-column {:id [:bigint-20-unsigned :nnull :auto]}) 
                           (let [cls column-spec]
                             (cond (string? cls) cls
                                   (int? cls) cls
                                   (double? cls) cls
                                   (float? cls) cls
                                   (map? cls) (create-column cls)
                                   (vector? cls) (string/join ", " (map #(create-column %) cls))
                                   :else nil)) 
                           "PRIMARY KEY (`id`)"])))


(defn- constraint-create
  "Description
    Specyfication for foreight keys. Default is `:default` option
    You can choose one of options for :update and :delete action
    :cascade   ;; mean 'CASCADE'
    :restrict   ;; mean 'RESTRICT'
    :null    ;; mean 'SET NULL'
    :no-action   ;; mean 'NO ACTION'
    :default   ;; mean 'SET DEFAULT'
  
  Example
    {:id_permission :permission} {:update :cascade :delete :restrict}"
  ([table-name tables]
   (let [is_id? #(= "id_" (apply str (take 3 (clojure.string/lower-case %))))
         [colm rel-tbl] (cond
                          (and (or (string? tables) (keyword? tables)) (is_id? (name tables))) [(name tables) (apply str (drop 3 (name tables)))]
                          (and (or (string? tables) (keyword? tables)) (not (is_id? (name tables)))) [(str "id_" (name tables)) (name tables)]
                          :else (map name (first (seq tables))))
         key-name (gensym table-name)]
      (str (format "KEY `%s` (`%s`), " (str key-name) colm)
           (format "CONSTRAINT `%s` FOREIGN KEY (`%s`) REFERENCES `%s` (`id`)" (str key-name) colm rel-tbl))))
  ([table-name tables update-delete]
   (let [on-action #(condp = %2
                      :cascade (format " ON %s CASCADE" %1)
                      :restrict (format " ON %s RESTRICT" %1)
                      :null (format " ON %s SET NULL" %1 )
                      :no-action (format " ON %s NO ACTION" %1)
                      :default (format " ON %s SET DEFAULT" %1) nil)]
     (let [is_id? #(= "id_" (apply str (take 3 (clojure.string/lower-case %))))
           [colm rel-tbl] (cond
                            (and (or (string? tables) (keyword? tables)) (is_id? (name tables))) [(name tables) (apply str (drop 3 (name tables)))]
                            (and (or (string? tables) (keyword? tables)) (not (is_id? (name tables)))) [(str "id_" (name tables)) (name tables)]
                            :else (map name (first (seq tables))))
           key-name (gensym table-name)
           { on-delete :delete on-update :update} update-delete]
       (str (format "KEY `%s` (`%s`), " key-name colm)
            (format "CONSTRAINT `%s` FOREIGN KEY (`%s`) REFERENCES `%s` (`id`)" (str key-name) colm rel-tbl)
            (if on-delete (on-action "DELETE" on-delete))
            (if on-update (on-action "UPDATE" on-update)))))))


(defn- alter-table-constraint-create
  "Description
    Specyfication for foreight keys. Default is `:defualt` option
    You can choose one of options for :update and :delete action
    :cascade   ;; mean 'CASCADE'
    :restrict   ;; mean 'RESTRICT'
    :null    ;; mean 'SET NULL'
    :no-action   ;; mean 'NO ACTION'
    :default   ;; mean 'SET DEFAULT'
  
  Example
    {:id_permission :permission} {:update :cascade :delete :restrict}"
  ([table-name tables]
   (let [is_id? #(= "id_" (apply str (take 3 (clojure.string/lower-case %))))
         [colm rel-tbl] (cond
                          (and (or (string? tables) (keyword? tables)) (is_id? (name tables))) [(name tables) (apply str (drop 3 (name tables)))]
                          (and (or (string? tables) (keyword? tables)) (not (is_id? (name tables)))) [(str "id_" (name tables)) (name tables)]
                          :else (map name (first (seq tables))))
         key-name (gensym table-name)]
      (str (format "CONSTRAINT `%s` FOREIGN KEY (%s) REFERENCES `%s` (`id`)" (str key-name) colm rel-tbl))))
  ([table-name tables update-delete]
   (let [on-action #(condp = %2
                      :cascade (format " ON %s CASCADE" %1)
                      :restrict (format " ON %s RESTRICT" %1)
                      :null (format " ON %s SET NULL" %1 )
                      :no-action (format " ON %s NO ACTION" %1)
                      :default (format " ON %s SET DEFAULT" %1) nil)]
     (let [is_id? #(= "id_" (apply str (take 3 (clojure.string/lower-case %))))
           [colm rel-tbl] (cond
                            (and (or (string? tables) (keyword? tables)) (is_id? (name tables))) [(name tables) (apply str (drop 3 (name tables)))]
                            (and (or (string? tables) (keyword? tables)) (not (is_id? (name tables)))) [(str "id_" (name tables)) (name tables)]
                            :else (map name (first (seq tables))))
           key-name (gensym table-name)
           { on-delete :delete on-update :update} update-delete]
       (str (format "CONSTRAINT `%s` FOREIGN KEY (%s) REFERENCES `%s` (`id`)" (str key-name) colm rel-tbl)
            (if on-delete (on-action "DELETE" on-delete))
            (if on-update (on-action "UPDATE" on-update))
            )))))

(defn- foreign-keys-string
  "Description
    Function get specyfication in `foreign-keys` argument and regurn linked foreighn key for two table.

  Examples
    (foreign-keys-string \"\" [{:table :id_suka} {:update :cascade :delete :restrict}] \"\")
    (foreign-keys-string \"\" [{:table :id_suka} {:update :cascade}] \"\")
    (foreign-keys-string \"\" [:id_table {:update :cascade}] \"\")
    (foreign-keys-string \"\" [:table {:update :cascade}] \"\")
    (foreign-keys-string \"\" [\"table\"] \"\")
    (foreign-keys-string \"\" [\"id_table\"] \"\")"
  [current-string foreign-keys query-args]
  (let [table-name (get-table (:table_name query-args))]
   (cond
     (string? foreign-keys)
     (str current-string ", " foreign-keys)
     
     (and (vector? foreign-keys) (map? (first foreign-keys)))
     (str current-string ", " (apply constraint-create table-name foreign-keys))
     
     (and (vector? foreign-keys) (vector? (first foreign-keys)))
     (str current-string ", " (string/join ", " (map #(apply constraint-create table-name %) foreign-keys)))
     
     (and (vector? foreign-keys) (string? (first foreign-keys)))
     (str current-string ", " (apply constraint-create table-name foreign-keys))
     
     (and (vector? foreign-keys) (keyword? (first foreign-keys)))
     (str current-string ", " (apply constraint-create table-name foreign-keys))
     
     :else current-string)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; alter-table functions ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- drop-foreign-key-string
  "Do drop column from table. Using in `alter table`:
  :drop-foreign-key :KEY_name"
  [current-string column-specyfication query-args]
  (str current-string (format " `%s` DROP FOREIGN KEY %s;" (get-table (:table_name query-args)) (name column-specyfication ))))

(defn- drop-column-string
  "Do drop column from table. Using in `alter table`:
  :drop-column :name"
  [current-string column-specyfication query-args]
  (str current-string (format " `%s` DROP COLUMN `%s`;" (get-table (:table_name query-args)) (name column-specyfication))))

(defn- add-foreign-key-string
  "Add foreign key to table to table. Using in `alter table`:
  [{:id_permission :permission} {:update :cascade :delete :restrict}]"
  [current-string column-specyfication query-args]
  (str current-string (format " `%s` ADD %s;" (get-table (:table_name query-args))  (apply alter-table-constraint-create (name (:table_name query-args)) column-specyfication))))

(defn- add-column-string
  "Add column to table. Using in `alter table`:
  :add-column {:suka [:bigint-20 \"NOT NULL\"]}"
  [current-string column-specyfication query-args]
  (str current-string (format " `%s` ADD %s;" (get-table (:table_name query-args)) (create-column column-specyfication))))

(defn- modify-column-string
  "Modify column type to field. Using in `alter table`:
  :modify {:suka [:bigint-20 \"NOT NULL\"]}"
  [current-string column-specyfication query-args]
  (str current-string (format " `%s` modify %s;" (get-table (:table_name query-args)) (create-column column-specyfication))))

;;;;;;;;;;;;;;;;;;;;;;;;
;;; pipeline helpers ;;;
;;;;;;;;;;;;;;;;;;;;;;;;

(defn- create-rule-pipeline [keys accepted-lexical-rule]
  (let [key-in? (fn [k col] (when (some #(= k %) col) (symbol (str (symbol k) "-string"))))]
    (reduce #(if-let [k (key-in? %2 keys)] (conj %1 [%2 k]) %1) [] accepted-lexical-rule)))

(defn- delete-empty-table-pipeline-applier [key-pipeline]
  (if (some #(= :from (first %1)) key-pipeline)
    key-pipeline (vec (concat [[:from 'from-string]] key-pipeline))))

(defn- insert-update-empty-table-pipeline-applier [key-pipeline]
  (vec (concat [[:-update-insert-empty-table 'empty-insert-update-table-string]] key-pipeline)))

(defn- select-empty-table-pipeline-applier [key-pipeline]
  (if (some #(or (= :count (first %1)) (= :column (first %1))) key-pipeline)
    key-pipeline
    (vec (concat [[:column 'empty-select-string]] key-pipeline))))
 
(defn- select-table-top-n-pipeline-applier [key-pipeline]
  (if (some #(= :top (first %1)) key-pipeline)
    (vec (concat [[:top 'top-string]] (filter #(not= :top (first %1)) key-pipeline)))
    key-pipeline))

(defn- select-table-count-pipeline-applier [key-pipeline]
  (if (some #(= :count (first %1)) key-pipeline)
    (vec (concat [[:count 'count-string]] (filter #(not= :count (first %1)) key-pipeline)))
    key-pipeline))

(defn- get-first-macro-from-pipeline [key-pipeline]
  (if (> (count key-pipeline) 0) [(first key-pipeline)] []))

(defn- empty-engine-pipeline-applier [key-pipeline]
  (if (some #(= :table-config (first %1)) key-pipeline) key-pipeline
      (conj key-pipeline [:table-config 'default-table-config-string])))

;;;;;;;;;;;
;;; DOC ;;;
;;;;;;;;;;;

(defn- set-spec-doc          [] "    :set - udpate {(<col-name> <value-to-eq>)* }")
(defn- top-spec-doc          [] "    :top - describe number of record from request. Pattern <num>. ")
(defn- limit-spec-doc        [] "    :limit - describe number of record from request. Pattern (<num>|(vector <num> <offset-num>))")
(defn- count-spec-doc        [] "    :count - count raws. Usage {:distinct :<col_name>} | :* (as is, mean all) | :<col_name>")
(defn- column-spec-doc       [] "    :column - specify column which will returned. (:<col_name>|\"<col_name>\"|{<col_name> <replacement_col_name>})+")
(defn- inner-join-spec-doc   [] "    :inner-join - one of join pattern, look on *-join rule")
(defn- right-join-spec-doc   [] "    :right-join - one of join pattern, look on *-join rule")
(defn- left-join-spec-doc    [] "    :left-join - one of join pattern, look on *-join rule")
(defn- outer-left-join-spec-doc  [] "    :outer-left-join - one of join pattern, look on *-join rule")
(defn- outer-right-join-spec-doc [] "    :outer-right-join - one of join pattern, look on *-join rule")
(defn- order-spec-doc  [] "    :order - set retrun order. Pattern only one (vector <col_name> [:desc|:asc])")
(defn- values-spec-doc []
  "    :values - if using map, effect equeal to :set spec. but also you can add multiply values in vector
      not specifing column (vector (vector (<col-val>)+)+)")
(defn- column-list-spec-doc []
  "    :column-list - is just sequence of keyword or string which specify a column you want insert data to
        for example [:name :user.lastname \"age\"]., pattern (sequable (<keyword|[\\w_.]+>)+)")
(defn- *-join-spec-doc []
  "    :*-join - has one of possible ways of usage:
         KEYWORD-DSL
        
           Pattern: (keyword-dsl|[keyword-dsl+])
             Example to short notation 
             Front/Back references 
             (t1)->(t2) - to-right-arrow specify front reference. 
               It mean that `t1` is currently know, and have foreighn-key to  
               the `t2` table. Example \"`t2` On `t1.id_t2`=`t2.id`\". 
             (t1)<-(t2) - to-left-arrow specify back reference
               It mean that `t2` is known table in expression, but hasn't 
               foreighn-key to `t1`, and it's Example \"`t1` On `t2.id`=`t1.id_t2`\".
             
             Table aliacing, id's
               For expression `:t1*A.id_B->t2*B.id_E` you can specify alias for table
               `:user*u` where `:user` is table and `u` - alias.
               `:user.id_permission` `.id_permission` - is foreighn key to external table
               ;;=> 
                 `t2` `B` ON `A`.`id_B`=`B`.`id_E`
           Example:
              (inner-join-string \"\" :user->permission {:table_name :user})
                ;; simple in front reference
                ;; => \" INNER JOIN `permission` ON `user`.`id_permission`=`permission`.`id`\"
        
              (inner-join-string \"\" :user<-permission {:table_name :permission})
                ;; simple in back reference 
                ;; => \" INNER JOIN `user` ON `permission`.`id`=`user`.`id_permission`\"
        
              (inner-join-string \"\" :u.id_p->permission*p.id {:table_name {:user :u}})
                ;; with aliasing and specifing foreighn-key
                ;; => \" INNER JOIN `permission` `p` ON `u`.`id_p`=`p`.`id`\"
              (inner-join-string \"\" :user*u<-p {:table_name {:permission :p}})
                ;; with aliasing withou specifing foreighn-key
                ;; => \" INNER JOIN `user` `u` ON `p`.`id`=`u`.`id_p`\"
        
        STRING 
            is simple specifing string directly 
            Pattern: (string|[string+])
            Example:
             (inner-join-string \"\" \"permission ON user.id_permission=permission.id\" {:table_name :user})
              ;; => \" INNER JOIN permission ON user.id_permission=permission.id\"
")
(defn- where-spec-doc []
  "    :where - is where block, which can be implemented with defferens pattern. 

        Column name spec
         <num> - [0-9]+
         <col-name> - mean [a-zA-Z0-9_.]{3,} in keyword or string type :first_name or \"FIRSTNAME\". 
           If col locate in other table, just set \"table dot col\" notation. For example :table1.table1col or 
           \"table1.table1col\"
         <table-name> - the same as <col-name>
         <s-exp> - mean (([\\w\\d]+|.*])+) or regular s-expression
         <value-to-eq> - *
         <col-val-name> - <col-name> | *

        Map
         Pattern: {(<col-name> <value-to-eq>)* }
         Example: {:CREDENTAIL.login \"XXXpussy_destroyer69@gmail.com\" :name \"Aleksandr\"}
           ;; => WHERE CREDENTAIL.login=\"XXXpussy_destroyer69@gmail.com\" AND name=\"Aleksandr\"

        String construction
         Pattern: \".*\"
         Example: \"user-table.name = 'serhii'\"
           ;; => WHERE user-table.user = 'serhii'

        Vector where DSL 
          Pattern: (vector (:and|:or|:in|:between|:=|:>|:<|:>=|:<=){1} (<s-exp>)+)
          Pattern or|and: (list [and|or] (<s-exp>)*)
          Pattern between: (list between <col-name> <start-range> <end-range>)
          Pattern in: (list in <col-name> (list (<val>)+))
          Pattern =|>|<|>=|<=: (list [=|>|<|>=|<=] <col-val-name> <col-val-name> )
          Example: [:or [:= :f1 1]
                        [:>= :f1 \"bliat\"]
                        [:and [:> :f2 2]
                             [:= :f2 \"fuck\"]
                             [:between :f1 1 (+ 10 1000)]
                             [:or [:= :suka \"one\"]
                                 [:in :one [1 2 3 (+ 1 2)]]]]]
            ;;=> ... WHERE `f1` = 1 OR `f1` >= \"bliat\" OR (`f2` > 2 AND `f2` = \"fuck\" AND `f1` BETWEEN 1 AND 1010 AND (`suka` = \"one\" OR `one` LIKE (1, 2, 3, 3)))")

;;;;;;;;;;;;;;;;;;;;;;
;; Operations DOC-s ;;
;;;;;;;;;;;;;;;;;;;;;;

(defn- select-doc []
  (format 
   "Description
    SSQL experssion function `select` using for generation SQL select strings. 

  Section contain 
    Example 
    Spec-params - specs for keys you may use. See `*accepted-select-rules*`
    Column name spec - specs for column declaration
    Type name spec - how to declare types

  Rules
    `*accepted-select-rules*` - %s

  Example
    ;; Big query example
    (select! {:table_name :user
              :right-join :user->permission
              :inner-join [:user->permission
                           :other<-user]
              :outer-left-join :user*U.id_another->another.id
              :left-join [\"action ON <action join construct>\"]
              :column [:#as_is :name :dla_mamusi :CREDENTAIL.login]
              :where [:or [:= :login \"admin\"]]})

    ;; Columns show
    ;; :#as_is - make all columns have 'AS' statement in colums 
    (select! {:table_name :user :column [:first_name :second_name \"bliat\" {:age :user_age}]})
       ;; => \"SELECT first_name, second_name, bliat, age AS user_age FROM user\"
    (select! {:table_name :user :column [:#as_is :first_name {:age :user_age}]})
       ;; => \"SELECT first_name AS first_name, age AS user_age FROM user\"

    ;; Top
    (select! {:table_name :user :top 10})

    ;; Limit
    (select! {:table_name :user :limit 10})
    (select! {:table_name :user :limit [10 20]})

    ;; Count
    (select! {:table_name :user :count {:distinct :column}})
    (select! {:table_name :user :count :*})
    (select! {:table_name :user :count :column})

    ;; Where
    (select! {:table_name :user :where \"user-table.user = \\\"Anatoli\\\" \"})
    (select! {:table_name :user
              :where [:or [:= :f1 1]
                        [:>= :f1 \"bliat\"]
                        [:and [:> :f2 2]
                             [:= :f2 \"fuck\"]
                             [:between :f1 1 (+ 10 1000)]
                             [:or [:= :suka \"one\"]
                                 [:in :one [1 2 3 (+ 1 2)]]]]]})  

    
  Spec-params
  %s"
   *accepted-select-rules*
   (string/join "\n"
                [(top-spec-doc)
                 (limit-spec-doc)
                 (count-spec-doc)
                 (column-spec-doc)
                 (order-spec-doc)
                 (inner-join-spec-doc)
                 (right-join-spec-doc)
                 (left-join-spec-doc)
                 (outer-left-join-spec-doc)
                 (outer-right-join-spec-doc)
                 (*-join-spec-doc)
                 (where-spec-doc)])))


(defn- update-doc []
  (format "Description
    SSQL experssion function `update!` using for generation SQL UPDATE strings. 

  Section contain 
    Example 
    Spec-params - specs for keys you may use. See `*accepted-update-rules*`

  Rules
    `*accepted-update-rules*` - %s

  Example
    (update! {:table_name :user :set {:id nil, :num_c 1, :str_c \"some\"}})
    (update! {:table_name :user :set {:num_c 1, :str_c \"some\"} :where (= :id 10)})

  Spec-params
  %s" *accepted-update-rules* (string/join "\n" [(set-spec-doc)
                                                 (where-spec-doc)])))

(defn- insert-doc []
  (format "Description
    SSQL experssion function `insert!` using for generation SQL INSERT strings. 

  Section contain 
    Example 
    Spec-params - specs for keys you may use. See `*accepted-insert-rules*`

  Rules
    `*accepted-insert-rules*` - %s

  Example
    (insert! {:table_name :user :set {:id 1, :str1_c \"vasia\", :str2_c \"123\", :num_c 20}})
    (insert! {:table_name :user :values {:id 1, :str1_c \"vasia\", :str2_c \"123\", :num_c 20}})
    (insert! {:table_name :user :values [[1 \"vasia\" \"123\" 20] [2 \"vasia\" \"123\" 20]]})
    (insert! {:table_name :user 
              :column-list [:id \"first_name\" :user.last_name \"age\"]
              :values [[1 \"vasia\" \"123\" 20] [2 \"vasia\" \"123\" 20]]})

  Spec-params
  %s"
          *accepted-insert-rules*
          (string/join "\n" [(set-spec-doc)
                             (values-spec-doc)
                             (column-list-spec-doc)])))

(defn- delete-doc []
  (format
   "Description
    SSQL experssion function `delete!` using for generation SQL DELETE strings. 

  Section contain 
    Example 
    Spec-params - specs for keys you may use. See `*accepted-delete-rules*`
    
  Rules
    `*accepted-delete-rules*` - %s

  Example
    (delete! {:table_name :user})
    (delete! {:table_name :user :where [:= :id 1]})

  Spec-params
  %s"
   *accepted-delete-rules*
   (string/join "\n" [(where-spec-doc)])))


(defn- create-table-doc []
  "Description
    SSQL experssion function `create-table!` using for generation sql strings

  Section contain 
    Example 
    Spec-params - specs for keys you may use. See `*accepted-ctable-rules*`
    Column name spec - specs for column declaration
    Type name spec - how to declare types

  Example
    ;; Mainly look like
    (create-table! {:table_name :point_of_sale
                    :columns [{:id_enterpreneur [:bigint-20-unsigned :default :null]}
                              {:name [:varchar-100 :default :null]}
                              {:physical_address  [:varchar-100 :default :null]}
                              {:telefons  [:varchar-100 :default :null]}]
                    :foreign-keys [{:id_enterpreneur :enterpreneur} {:update :cascade}]})
  
    ;; but you can also put ssql `spec-parameters` into the map
    ;; WARNING! puting table name into map only with key `:table_name`
    ;; look on example belove.
    (create-table! {:table_name :point_of_sale
                    :columns [{:id_enterpreneur [:bigint-20-unsigned :default :null]}
                             ....
                    :foreign-keys [{:id_enterpreneur :enterpreneur} {:update :cascade}])}
  
    ;; other example of usage 
    (create-table! {:table_name :point_of_sale
              :columns [{:name [:varchar-100 :null]} {:some-int :integer-200} {:id_other_table :bigint-20}]
              :foreign-keys [{:id_other_table :other_table} {:update :cascade, :delete :null}]
              :table-config {:engine \"InnoDB\", :charset \"utf8\"}})

  Spec-params 
    :columns - describes column list in notation [{col-spec type-spec}]
    :table-config - simple configuration map by pattern {[:engine|:charset|:collate]* .+)}
      {:engine <table engine(defautl: InnoDB)> 
       :charset <charset for table(default: utf8)>
       :collate <table collate(default: utf8_general_ci)>} 
    :foreign-keys - describes key relation to other table see `*accepted-forkey-rules*`
      Example forms:
         If you have many keys just set it all in vector
          [[{:id_enterpreneur :enterpreneur} {:update :cascade :delete [:restrict|:cascade|:null|:no-action|:default]}]
           [{:id_user :user}]]
         if you have one foreign key set it vector with two maps, where first is linked id(key), and linked table(value)
         OPTIONAL second map if you not set it may next \"view\" {:update :default :delete :default}
          [{:id_enterpreneur :enterpreneur} {:update :cascade :delete [:restrict|:cascade|:null|:no-action|:default]}]
         Also you can use really \"short\" exteranl keys, look at all posible examples:
          [[{:table :id_suka} {:update :cascade :delete :restrict}] [...] ... ]
          [{:table :id_suka} {:update :cascade :delete :restrict}]
          [{:table :id_suka} {:update :cascade}]
          [:id_table {:update :cascade}]
          [:table {:update :cascade}]
          [\"table\"] 
          [\"id_table\"]

  Column name spec(col-spec)
    * - string which validates by pattern [a-z_]{2,} 
    :id_* - column mean field which would linked with other table as foreign key
    :meta* - column only for database and program API usage, which not being included to UI
    :* - simple field. 

  SSQL column type spec(type-spec)
    * => *
    :null => NULL
    :nnull => NOT NULL
    :date => DATE
    :datetime => DATETIME
    :time => TIME
    :varchar-[0-9]{1,} => VARCHAR(12)
    :[tinyint|smallint|mediumint|int|integer|bigint|double|float|real]-[0-9|[.|[0-9]{1,}]?]+ => INT(123)
    :[bool|boolean] => TINYINT(1)
    :[tinyblob|blob|mediumblob|longblob|tinytext|text|mediumtext|longtext|json] => the same only in uppercase
    :[auto_increment|autoincrement|auto] => AUTO_INCREMENT
    :[default|signed|unsigned|zerofill] => used as specyficator to integer types :int-10-unsigned, but 
       defualt used like [:int-20 default -2]
    \"*\" -> mean that you can combine SSQL types with strings, for example
       you may write something like this
        [:varchar-40 :default :null]
       but also
        [\"VARCHAR(40)\" :default \"NULL\"]
       or if you want totatly in SQL string 
        \"VARCHAR(40) DEFAULT NULL\"
       choose what better for you")

(defn- alter-table-doc [] 
  "Description
    SSQL experssion function `create-table` using for generation sql strings

  Section contain 
    Example 
    Spec-params - specs for keys you may use. See `*accepted-ctable-rules*`
  
  Example
    (alter-table! {:table_name :user :drop-column :bliat})
    (alter-table! {:table_name :user :drop-foreign-key :bliat})
    (alter-table! {:table_name :user :add-column {:suka [:boolean]}})
    (alter-table! {:table_name :user :add-foreign-key [{:id_permission :permission} {:update :cascade}]})")

;;;;;;;;;;;;;;;;;;;;;;;;;
;;; define-operations ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- reduce-rules
  "Description
    For `list-of-rules` with content like that:
    [[:column column-string] [:table_name table-name-string] [:where where-string]]
    where `:column` is key from `args` maps resolve and eval function
    `column-string` which take some string (\"\"), some value by key (:column args)
    as second argument and arguments ({:column... :table_name ... ... :N n})
    And return string."
  [args start-string list-of-rules]
  (reduce
     (fn [sql-string [rule-key rule-fn]]
       ((ns-resolve 'jarman.logic.sql-tool rule-fn) sql-string (rule-key args) args))
     start-string
     list-of-rules))

(defn- --ssql-make-debug-pprint [operation accepted-rules list-of-rules args generated-SQL]
  (let [list-of-rules (reduce #(into % (apply hash-map %2)) {} list-of-rules)]
    (if *debug-full-descript*
      (do
        (println ";; ---------------------------------------")
        (println ";; Query Meta ")
        (println ";;   id   - " (gensym "SELECT_"))
        (println ";;   time - " (date))
        (println ";; ")
        (println ";; Allowed rules ")
        (println (cl-format nil "~{;;   ~{~20A~^ ~}~^~%~}" (partition-all 3 accepted-rules)))
        (println ";;  ")
        (println ";; Query keys")
        (doall
         (for [[k v] args]
           (if (contains? list-of-rules k)
             (println (format ";;   %-15s %s\n;; %20s;;=> %s" k v "" (pr-str ((ns-resolve 'jarman.logic.sql-tool (k list-of-rules)) "" (k args) args))))
             (println (format ";;   %-15s %s" k v )))))
        (println ";; ")
        (println ";; Final SQL")
        (println (cl-format nil "~{;;   ~{~A~^~}~^-~%~}" (partition-all 90 generated-SQL)))
        (println ";; ")
        (pprint (list operation args))
        (println ""))
      (do
        (println (list operation args))
        (println ";;=> " generated-SQL)
        (println "")))))

(defn- debug-ssql [operation accepted-rules list-of-rules args generated-SQL]
  (if *debug*
    (case *debug-to*
      :output (--ssql-make-debug-pprint operation accepted-rules list-of-rules args generated-SQL)
      :file (with-open [W (io/writer (io/file *debug-file*) :append true)]
              (.write W (with-out-str
                          (--ssql-make-debug-pprint operation accepted-rules list-of-rules args generated-SQL)))))))

(defn ^{:doc (insert-doc)} insert! [{:as args}]
  (if-not (:table_name args) (throw (java.lang.Exception. "the `:table_name` key is required")))
  (let [list-of-rules
        ((comp insert-update-empty-table-pipeline-applier create-rule-pipeline)
         (keys args) *accepted-insert-rules*)
        generated-sql (reduce-rules args "INSERT INTO" list-of-rules)]
    (debug-ssql 'insert! *accepted-insert-rules* list-of-rules args generated-sql)
    generated-sql))

(defn ^{:doc (delete-doc)} delete! [{:as args}]
  (if-not (:table_name args) (throw (java.lang.Exception. "the `:table_name` key is required")))
  (let [list-of-rules
        ((comp delete-empty-table-pipeline-applier
            create-rule-pipeline)
         (keys args) *accepted-delete-rules*)
        generated-sql (reduce-rules args "DELETE FROM" list-of-rules)]
    (debug-ssql 'delete! *accepted-delete-rules* list-of-rules args generated-sql)
    generated-sql))

(defn ^{:doc (update-doc)} update! [{:as args}]
  (if-not (:table_name args) (throw (java.lang.Exception. "the `:table_name` key is required")))
  (let [list-of-rules ((comp insert-update-empty-table-pipeline-applier
                          create-rule-pipeline)
                       (keys args) *accepted-update-rules*)
        generated-sql (reduce-rules args "UPDATE" list-of-rules)]
    (debug-ssql 'update! *accepted-update-rules* list-of-rules args generated-sql)
    generated-sql))

(defn ^{:doc (select-doc)} select! [{:as args}]
  (if-not (:table_name args) (throw (java.lang.Exception. "the `:table_name` key is required")))
  (let [list-of-rules
        ((comp select-table-count-pipeline-applier
            select-table-top-n-pipeline-applier
            select-empty-table-pipeline-applier
            create-rule-pipeline)
         (keys args) *accepted-select-rules*)
        generated-sql (reduce-rules args "SELECT" list-of-rules)]
    (debug-ssql 'select! *accepted-select-rules* list-of-rules args generated-sql)
    generated-sql))

(defn ^{:doc (create-table-doc)} create-table! [{:as args}]
  (if-not (:table_name args) (throw (java.lang.Exception. "the `:table_name` key is required")))
  (let [list-of-rules
        ((comp empty-engine-pipeline-applier create-rule-pipeline)
         (keys args) *accepted-ctable-rules*)
        generated-sql (reduce-rules args "CREATE TABLE IF NOT EXISTS" list-of-rules)]
    (debug-ssql 'create-table! *accepted-select-rules* list-of-rules args generated-sql)
    generated-sql))

(defn ^{:doc (alter-table-doc)} alter-table! [{:as args}]
  (if-not (:table_name args) (throw (java.lang.Exception. "the `:table_name` key is required")))
  (let [list-of-rules
        ((comp get-first-macro-from-pipeline create-rule-pipeline)
         (keys args) *accepted-alter-table-rules*)
        generated-sql (reduce-rules args "ALTER TABLE" list-of-rules)]
    (debug-ssql 'alter-table! *accepted-select-rules* list-of-rules args generated-sql)
    generated-sql))

;;;;;;;;;;;;;;;;;;;;
;;; TEST SEGMENT ;;;
;;;;;;;;;;;;;;;;;;;;

(comment
  (;; binding [*debug-to* :file
   ;;          *debug-full-descript* true]
   do
   (select! {:table_name :user})
   (select! {:table_name :user :column [:#as_is :user.login :user.passoword]})
   (update! {:table_name :user :set {:id nil, :num_c 1, :str_c "slal"}})
   (insert! {:table_name :user :set {:id nil, :num_c 1, :str_c "slal"}})
   (insert! {:table_name :user :values [[1 "vasia" "123" 20] [2 "vasia" "123" 20]]})
   (delete! {:table_name :user :where [:= :user.login "serhii"]})
   (alter-table! {:table_name :user :drop-column :bliat})
   (alter-table! {:table_name :user :drop-foreign-key :bliat})
   (alter-table! {:table_name :user :add-column {:suka [:boolean]}})
   (alter-table! {:table_name :user :add-foreign-key [{:id_permission :permission} {:update :cascade}]})
   (create-table! {:table_name :user
                   :columns [{:login [:varchar-100 :nnull]}
                             {:password [:varchar-100 :nnull]}
                             {:first_name [:varchar-100 :nnull]}
                             {:last_name [:varchar-100 :nnull]}
                             {:id_permission [:bigint-20-unsigned :nnull]}]
                   :foreign-keys [{:id_permission :permission} {:delete :cascade :update :cascade}]})))

;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Database Metadata ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;

(defn drop-database [database-name]
  {:pre [(string? database-name)]}
  (format "DROP DATABASE `%s`;" (string/trim database-name)))

(defn create-database [database-name & {:keys [charset collate] :or {charset "utf8mb4" collate "utf8mb4_general_ci"}}]
  {:pre [(string? database-name)]}
  (apply format "CREATE DATABASE `%s` CHARACTER SET = '%s' COLLATE = '%s';" (map string/trim [database-name charset collate]) ))

(defn show-databases []
  "SHOW DATABASES")

(defn show-tables []
  "SHOW TABLES")

(defn show-table-columns [value]
  {:pre [(some? value)]}
  (format "SHOW COLUMNS FROM %s" (name value)))

(defn drop-table [database-table]
  (format "DROP TABLE IF EXISTS `%s`" (name database-table)))

