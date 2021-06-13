(ns jarman.logic.sql-tool
  (:gen-class)
  (:refer-clojure :exclude [update])
  (:import [java.time Period LocalDate])
  (:require
   ;; standart lib
   [clojure.string :as string]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; configuration rules ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def ^{:dynamic true :private true} *accepted-alter-table-rules* [:add-column :drop-column :drop-foreign-key :add-foreign-key :modify-column])
(def ^{:dynamic true :private true} *accepted-forkey-rules*      [:restrict :cascade :null :no-action :default])
(def ^{:dynamic true :private true} *accepted-ctable-rules*      [:columns :foreign-keys :table-config])
(def ^{:dynamic true :private true} *accepted-select-rules*      [:top :count :column :inner-join :right-join :left-join :outer-left-join :outer-right-join :where :order :limit])
(def ^{:dynamic true :private true} *accepted-update-rules*      [:update-table :set :where])
(def ^{:dynamic true :private true} *accepted-insert-rules*      [:column-list :values :set])
(def ^{:dynamic true :private true} *accepted-delete-rules*      [:from :where])

(def ^{:dynamic true :private true} *data-format* "DB date format" "yyyy-MM-dd HH:mm:ss")
(def ^{:dynamic true :private true} *namespace-lib* "" "jarman.logic.sql-tool")

(defn- transform-namespace [symbol-op]
  (if (some #(= \/ %) (str symbol-op)) symbol-op
      (symbol (format "%s/%s" *namespace-lib* symbol-op))))

(defn find-rule [operation-name]
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

(defn pair-where-pattern
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

(defn tkey
  "Function split dot-linked keyword name
  and return array of string, divided on <.>
  character
  :table.value => ('table' 'value')"
  [k] (string/split (str (symbol k)) #"\."))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Joining preprocessor ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(let [a (string/split (first (re-matches #"([\w\._]+(->)?)+" "suka->bliat->dupa->chuj")) #"->")]
  (map #(vector %1 %2) a (drop 1 a)))
(let [a (string/split (first (re-matches #"([\w\._]+(->)?)+" "suka->bliat->chuj")) #"->")]
  (map #(vector %1 %2) a (drop 1 a)))

(defn column-dot-resolver [table-column & {:keys [otherwise]}]
  (let [table-column (name table-column)]
    (if-let [[_ t c] (re-matches #"([\w_]+)\.([\w_]+)" table-column )]
      (let [[_ alias_name] (re-matches #"id_([\w_]+)" c)]
        [alias_name t c])
      (cond
        (fn? otherwise)
        [nil table-column (otherwise (string/lower-case table-column))]

        (keyword? otherwise)
        [nil table-column (name otherwise)]

        (string? otherwise)
        [nil table-column otherwise]

        :else
        [nil table-column "id"]))))

(defn join-keyword-string [main-table joining-table]
  (letfn [(column-resolver [table-column & {:keys [k]}]
            (if-let [[_ t c] (re-matches #"([\w_]+)\.([\w_]+)" (doto table-column println))]
              (doto [t c] println)
              [(name table-column) (str "id_" (string/lower-case (name joining-table)))]))]
   (if (re-matches #"([\w\._]+->)+([\w\._]+)" (name joining-table))
     (let [table-cols (string/split (name joining-table) #"->")]
       (doall (map #(join-keyword-string %1 %2) table-cols (drop 1 table-cols))))
     (let [[jalias jtable jcolumn] (column-dot-resolver joining-table)
           [malias mtable mcolumn] (column-dot-resolver main-table :otherwise (str "id_" (string/lower-case (name joining-table))))]
       (doto (if (and malias (not= jtable malias))
          (format "%s %s ON %s.%s=%s.%s" jtable malias malias jcolumn mtable mcolumn)
          (format "%s ON %s.%s=%s.%s" jtable jtable jcolumn mtable mcolumn))
         println)))))

(defn join-string-string [main-table on-join-construction]
  on-join-construction)

(defn join-map-keyword-string [main-table map-structure]
  (let [[k v] (first map-structure)
        [table join-column] (list (name k) (name v))
        [_ alias_name] (re-matches #"id_([\w_]+)" join-column)]
    (if (and alias_name (not= table alias_name))
      (format "%s %s ON %s.id=%s.%s" table alias_name alias_name main-table (name join-column))
      (format "%s ON %s.id=%s.%s" table table main-table (name join-column)))))

;; (defn join-vector-keyword-string [main-table joining-table]
;;   (let [[table join-column] (list (name joining-table)
;;                                   (name (str "id_" (string/lower-case (name joining-table)))))]
;;     (format "%s ON %s.id=%s.%s" table table main-table join-column)))

;; (defn join-vector-string-string [main-table on-join-construction]
;;   on-join-construction)

(defn join-dot-map-string [main-table map-structure]
  (for [[joining-table main-table] map-structure
        :while (and (some #(= \. %) (name joining-table)) (some #(= \. %) (name main-table)))]
   (let [[jalias jtable jcolumn] (column-dot-resolver joining-table)
         [malias mtable mcolumn] (column-dot-resolver main-table :otherwise (str "id_" (string/lower-case (name joining-table))))]
     (doto (if (and malias (not= jtable malias))
             (format "%s %s ON %s.%s=%s.%s" jtable malias malias jcolumn mtable mcolumn)
             (format "%s ON %s.%s=%s.%s" jtable jtable jcolumn mtable mcolumn))
       println)))
  ;; (if-let [[[t1 id1] [t2 id2]]
  ;;          (and (some #(= \. %) (str k))
  ;;             (some #(= \. %) (str v))
  ;;             (list (string/split (str (symbol k)) #"\.")
  ;;                   (string/split (str (symbol v)) #"\.")))]
  ;;   (format "%s ON %s=%s" t1 (str (symbol k)) (str(symbol v))))
  )


;; (defn join-dot-map-string [main-table [k v]]
;;   (if-let [[[t1 id1] [t2 id2]]
;;            (and (some #(= \. %) (str k))
;;                 (some #(= \. %) (str v))
;;                 (list (string/split (str (symbol k)) #"\.")
;;                       (string/split (str (symbol v)) #"\.")))]
;;     (format "%s ON %s=%s" t1 (str (symbol k)) (str(symbol v)))))

(defn get-function-by-join-type [join]
  (cond
    ;; Example rule
    ;;  :some
    ;;  :table1->table2
    (keyword? join) join-keyword-string
    ;; Example rule
    ;;  "table ON table.a=table2.b"
    (string? join) join-string-string
    ;; Example rule
    ;;  {:table :id_table_which_selecting}
    ;;  {:table.id :selected_table.id_table}
    (map? join) (if-let [value-of-key (second (first join))]
                  (when (keyword? value-of-key)
                    (if (and (some #(= \. %1) (str value-of-key))
                           (some #(= \. %1) (str (first (first join)))))
                      join-dot-map-string
                      join-map-keyword-string)))
       ;; (vector? join) (if-let [first-value (first join)]
       ;;                  (cond (keyword? first-value) join-vector-keyword-string
       ;;                        (string? first-value) join-vector-string-string))
       ))

(defn- rule-joiner [rule join-string-or-join-list]
  (format "%s %s" rule
          (if (sequential? join-string-or-join-list)
            (string/join (str " " rule " ") join-string-or-join-list)
            join-string-or-join-list)))

(defmacro define-joinrule [rule-name]
  (let [rule-array (string/split (str rule-name) #"\-")  rule-lenght (- (count rule-array) 1)
        rule-keyword (keyword (string/join "-"(take rule-lenght rule-array)))
        rule-string (string/join " " (map string/upper-case (take rule-lenght rule-array)))]
    `(defn ~rule-name [~'current-string ~'joins-form ~'table-name]
       (if (sequential? ~'joins-form)
         (str
          ~'current-string " "
          (string/join " "
           (for [form# ~'joins-form
                 :let [join-function# (get-function-by-join-type form#)]
                 :while (some? join-function#)]
             (rule-joiner ~rule-string (join-function# (name ~'table-name) form#)))))
         (if-let [join-function# (get-function-by-join-type ~'joins-form)]
           (str ~'current-string " " (rule-joiner ~rule-string (join-function# (name ~'table-name) ~'joins-form)))
           ~'current-string)))))

(define-joinrule inner-join-string)
(define-joinrule left-join-string)
(define-joinrule right-join-string)
(define-joinrule outer-right-join-string)
(define-joinrule outer-left-join-string)

(defn top-string [current-string top-number table-name]
  (if (number? top-number)(str current-string " TOP " top-number)
       (str current-string)))

(defn count-string
  "Example of using rule 
    :count {:distinct :column}
    :count :*
    :count :column
  Result is {:count <number>}"
  [current-string count-rule table-name]
  (cond
    (keyword? count-rule)
    (str current-string (format " COUNT(%s) as count FROM %s" (name count-rule) (format "`%s`" (name table-name))))
    
    (map?     count-rule)
    (str current-string (if (:distinct count-rule)
                          (format " COUNT(DISTINCT %s) as count FROM %s" (name (:distinct count-rule)) (format "`%s`" (name table-name)))))
    
     :else (str current-string)))

(defn limit-string [current-string limit-number table-name]
  (cond
     (number? limit-number) (str current-string " LIMIT " limit-number)
     (and (not (string? limit-number)) (seqable? limit-number) (let [[f s]limit-number] (and (number? f) (number? s))))
     (str current-string " LIMIT " (string/join "," limit-number))
     :else (str current-string)))

(defn column-string [current-string col-vec table-name]
  (let [f (fn [c]
           (cond 
             (keyword? c) (str (symbol c))
             (string? c) c
             (and (vector? c) (= (count c) 2))
             (let [[x y] (map #(str (symbol %)) c)]
                             (format "%s AS `%s`" x y))
             (map? c) (let [[x y] (map #(str (symbol %)) (first (vec c)))]
                        (format "%s AS `%s`" x y))
             :else nil))]
    (str current-string " "
         (string/join ", " (map f col-vec)) " FROM " (format "`%s`"(name table-name)))))

(defn empty-select-string [current-string _ table-name]
  (str current-string " * FROM " (name table-name)))

(defn order-string [current-string args table-name]
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
(defn into-border [some-string]
  (if *where-border* 
    (str "(" some-string ")")
    some-string))

;;;;;;;;;;;;;;;;;;;;;;;;;
;; LIST MACROPROCESSOR ;;
;;;;;;;;;;;;;;;;;;;;;;;;;

(declare between-operator-v)
(declare define-operator-v)
(defn where-procedure-parser-v [where-clause]
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

(defn define-operator-v [operator field-1 field-2]
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

(defmacro and-processor-s [& args]
  (let [v (vec (for [x (vec args)]
                 `(binding [*where-border* true]
                    (where-procedure-parser-s ~x))))]
    `(into-border (string/join " AND " ~v))))

(defmacro or-processor-s [& args]
  (let [v (vec (for [x (vec args)]
                 `(binding [*where-border* true]
                    (where-procedure-parser-s ~x))))]
    `(into-border (string/join " OR " ~v))))

(defmacro where-procedure-parser-s [where-clause]
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

(defn between-operator-s [field v1 v2]
  (format "%s BETWEEN %s AND %s"
          (eval `(where-procedure-parser-s ~field))
          (eval `(where-procedure-parser-s ~v1))
          (eval `(where-procedure-parser-s ~v2))))

(defn define-operator-s [operator-s field-1 field-2]
  (string/join " " [(eval `(where-procedure-parser-s ~field-1))
                    operator-s
                    (eval `(where-procedure-parser-s ~field-2))]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; WHERE BLOCK PROCESSOR ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn where-string [s where-block table-name]
  (cond (string? where-block) (str s " WHERE " where-block)
        (map? where-block) (str s " WHERE " (string/join " AND " (map #(apply pair-where-pattern %) where-block)))
        (list? where-block) (str s " WHERE " (eval `(where-procedure-parser-s ~where-block)))
        (vector? where-block) (str s " WHERE " (where-procedure-parser-v where-block))
        :else s))

;; (let [d '(or (= :f1 1)
;;             (>= :f1 "bliat")
;;             (and (> :f2 2)
;;                (= :f2 "fuck")
;;                (between :f1 1 (+ 10 1000))
;;                (or (= :suka "one")
;;                   (in :one [1 2 3 (+ 1 2)]))))
;;       f "fuck"]
;;   (where-string "" d
;;                 ""))

;; (let [d '(or (= :f1 1)
;;             (>= :f1 "bliat")
;;             (and (> :f2 2)
;;                (= :f2 "fuck")
;;                (between :f1 1 (+ 10 1000))
;;                (or (= :suka "one")
;;                   (in :one [1 2 3 (+ 1 2)]))))
;;       f "fuck"]
;;   (where-string "" '(or (= :f1 1)
;;                              (>= :f1 "bliat")
;;                              (and (> :f2 2)
;;                                 (= :f2 1)
;;                                 (between :f1 1 (+ 10 1000))
;;                                 (or (= :suka "one")
;;                                    (in :one [1 2 3 (+ 1 2)]))))
;;                 ""))
;; (let [d '(or (= :f1 1)
;;             (>= :f1 "bliat")
;;             (and (> :f2 2)
;;                (= :f2 "fuck")
;;                (between :f1 1 (+ 10 1000))
;;                (or (= :suka "one")
;;                   (in :one [1 2 3 (+ 1 2)]))))
;;       f "fuck"]
;;   (where-string "" [:or [:= :f1 1]
;;                              [:>= :f1 "bliat"]
;;                              [:and [:> :f2 2]
;;                                 [:= :f2 f]
;;                               [:between :f1 1 (+ 10 1000)]
;;                                 [:or [:= :suka "one"]
;;                                  [:in :one [1 2 3 (+ 1 2)]]]]]
;;                 ""))



;;; Try to rewrite on static
;; (defn where-procedure-parser! [where-clause]
;;   (cond (nil? where-clause) (str "null")
;;         (symbol? where-clause) where-clause
;;         (string? where-clause) (pr-str where-clause)
;;         (keyword? where-clause) (format "`%s`" (name where-clause))
;;         (seqable? where-clause) (let [function (first where-clause) args (rest where-clause)]
;;                                   (condp = function
;;                                     ;; 'or `(or-processor! ~@args)
;;                                     ;; 'and `(and-processor! ~@args)
;;                                     'or (apply #'or-processor! args)
;;                                     'and (apply #'and-processor! args)
;;                                     ;; 'or (let [or-nexp `(or-processor! ~@args)] `~or-nexp)
;;                                     ;; 'and (let [and-nexp `(and-processor! ~@args)] `~and-nexp)

;;                                     '> (apply define-operator! function args)
;;                                     '< (apply define-operator! function args)
;;                                     '= (apply define-operator! function args)
;;                                     '>= (apply define-operator! function args)
;;                                     '<= (apply define-operator! function args)
;;                                     '<> (apply define-operator! function args)
;;                                     '!= (apply define-operator! (symbol '<>) args)
;;                                     'like (apply define-operator! (symbol 'LIKE) args)
;;                                     'in (apply define-operator! (symbol 'LIKE) args)
;;                                     'and (apply define-operator! (symbol 'LIKE) args)
;;                                     'between (apply between-procedure! args)
                                    
;;                                     ;; '> (apply define-operator! function args)
;;                                     ;; '< (apply define-operator! function args)
;;                                     ;; '= (apply define-operator! function args)
;;                                     ;; '>= (apply define-operator! function args)
;;                                     ;; '<= (apply define-operator! function args)
;;                                     ;; '<> (apply define-operator! function args)
;;                                     ;; '!= (apply define-operator! (symbol '<>) args)
;;                                     ;; 'like (apply define-operator! (symbol 'LIKE) args)
;;                                     ;; 'in (apply define-operator! (symbol 'LIKE) args)
;;                                     ;; 'and (apply define-operator! (symbol 'LIKE) args)
;;                                     ;; 'between (apply between-procedure! args)
;;                                     (if (and (symbol? function) (resolve function))
;;                                       (let [result (eval where-clause)]
;;                                         (eval `(where-procedure-parser! '~result)))
;;                                       (let [element-primitives
;;                                             (vec (for [x where-clause]
;;                                                    (eval `(where-procedure-parser! '~x))))]
;;                                         `(binding [*where-border* true]
;;                                            (into-border (string/join ", " ~element-primitives)))))))
;;         :else (str where-clause)))

;; (let [dupa 120]
;;   (where-procedure-parser!
;;    (or (= :f1 1)
;;        (and
;;         (> :f2 dupa)
;;         (> :f3 3))
;;        (or (> :f2 2)))))

;; (where-procedure-parser!
;;  '(or (= :f1 1)
;;      (and
;;         (> :f2 29)
;;         (> :f3 3))
;;       (or (> :f2 2))))

;; (let [dupa 2]
;;   (println "____________DEBUG______________")
;;   (eval (doto
;;             (jarman.logic.sql-tool/where-procedure-parser!
;;               '(or (= :f1 1)
;;                   (and
;;                    (> :f2 dupa)
;;                    (> :f3 3))
;;                   (or (> :f2 2))))
;;           clojure.pprint/pprint
;;           )))

;; (defn between-procedure! [field v1 v2]
;;   (let [a (eval `(where-procedure-parser! '~field))
;;         b (eval `(where-procedure-parser! '~v1))
;;         c (eval `(where-procedure-parser! '~v2))]
;;     `(format "%s BETWEEN %s AND %s"
;;              ~a ~b ~c)))

;; (defn define-operator! [operator field-1 field-2]
;;   (let [a (eval `(where-procedure-parser! '~field-1))
;;         b (str `~operator)
;;         c (eval `(where-procedure-parser! '~field-2))]
;;     `(string/join " " [~a ~b ~c])))
;; (defn define-operator! [operator field-1 field-2]
;;   (let [a (eval `~field-1)
;;         b (str `~operator)
;;         c (eval `~field-2)]
;;    `(string/join " " [~a ~b ~c])))


;; (defn and-processor! [& args]
;;   (let [v (vec (for [x (vec args)]
;;                  ;; (let [where-in (eval `(where-procedure-parser! ~x))]
;;                  ;;   `(binding [*where-border* true]
;;                  ;;      ~where-in))
;;                  `(binding [*where-border* true]
;;                     (where-procedure-parser! ~x))
;;                  ))]
;;     `(into-border (string/join " AND " ~v))))

;; (defn or-processor! [& args]
;;   (let [v (vec (for [x (vec args)]
;;                  ;; (let [where-in (eval `(where-procedure-parser! ~x))]
;;                  ;;   `(binding [*where-border* true]
;;                  ;;      ~where-in))
;;                  `(binding [*where-border* true]
;;                     (where-procedure-parser! ~x))
;;                  ))]
;;     `(into-border (string/join " OR " ~v))))

;; (let [dupa 120]
;;   (where-procedure-parser!
;;    (or (= :f1 1)
;;       (and
;;         (> :f2 dupa)
;;         (> :f3 3))
;;        (or (> :f2 2)))))

;; (jarman.logic.sql-tool/where-procedure-parser!
;;  '(or (= :f1 1)
;;      (>= :f1 "bliat")
;;      (and (> :f2 2)
;;         (= :f2 "fuck")
;;         (between :f1 1 (+ 10 2))
;;         (or (= :suka "one")
;;            (in :one [1 2 3 (+ 1 2)])))))

;;;;;;;;;;;;;;;;;;;
;;; column-list ;;; 
;;;;;;;;;;;;;;;;;;;

(defn column-list-string [current-string m table-name]
  (if (and (sequential? m) (every? (some-fn keyword? string?) m))
      (str current-string
           (format
            " (%s)" (string/join ", " (map name m))))
      current-string))

(defn empty-insert-update-table-string [current-string m table-name]
  (str (string/trim current-string) (format " `%s`" (name table-name))))

;;;;;;;;;;;;;;;;;;;;;;;;
;;; set preprocessor ;;;
;;;;;;;;;;;;;;;;;;;;;;;;

(defn set-string [current-string update-map tabel-name]
  (let [pair-group (fn [[col-name value]] (str (make-dot-column! col-name) "=" (where-procedure-parser-v value)))]
    (str current-string ;; " " (format "`%s`" (name tabel-name))
         (str " SET " (string/join ", " (map pair-group update-map))))))

;; (defn set-string [current-string update-map tabel-name]
;;   (let [pair-group (fn [[col-name value]] (str (format "%s" (name col-name)) "=" (where-procedure-parser-v value)))]
;;     (str current-string " " (format "%s" (name tabel-name)) (str " SET " (string/join ", " (map pair-group update-map))))))

(defn update-table-string [current-string map tabel-name]
  (str current-string "" (format "%s" (name tabel-name))))

(defn low-priority-string [current-string map table-name]
  (str current-string " LOW_PRIORITY"))

(defn from-string [current-string map tabel-name]
  (str current-string " " (format "%s" (name tabel-name))))

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

(defn values-string [current-string values table-name]
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

(defn create-column
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

(defn default-table-config-string [current-string _ table-name]
  (str current-string ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;"))

(defn table-config-string
  "Get configuration map with next keys parameters
  :engine - table engine(defautl: InnoDB)
  :charset - charset for table(default: utf8)
  :collate - table collate(default: utf8_general_ci)"
  [current-string conifig-map table-name]
  (let [{:keys [engine charset collate]
         :or {engine "InnoDB"
              charset "utf8mb4"
              collate "utf8mb4_general_ci"}} conifig-map]
     (str current-string (format ") ENGINE=%s DEFAULT CHARSET=%s COLLATE=%s;" engine charset collate))))

(defn columns-string
  "create columns with type data specyfications.
  Example using for `column-spec` argument:
  
  \"`id` BIGINT(150) NOT NULL AUTO_INCREMENT\"
  {:fuck [:bigint-20 \"NOT NULL\" :auto]}  
  [{:blia [:bigint-20 \"NOT NULL\" :auto]} {:suka [:varchar-210]}]
  [{:id :bigint-100} {:suka \"TINYTEXT\"}]"
  [current-string column-spec table-name]
  (str current-string (format " `%s` (" table-name)
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


(defn constraint-create
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


(defn alter-table-constraint-create
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


(defn foreign-keys-string
  "Description
    Function get specyfication in `foreign-keys` argument and regurn linked foreighn key for two table.

  Examples
    (foreign-keys-string \"\" [{:table :id_suka} {:update :cascade :delete :restrict}] \"\")
    (foreign-keys-string \"\" [{:table :id_suka} {:update :cascade}] \"\")
    (foreign-keys-string \"\" [:id_table {:update :cascade}] \"\")
    (foreign-keys-string \"\" [:table {:update :cascade}] \"\")
    (foreign-keys-string \"\" [\"table\"] \"\")
    (foreign-keys-string \"\" [\"id_table\"] \"\")"
  [current-string foreign-keys table-name]
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
    
    :else current-string))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; alter-table functions ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn drop-foreign-key-string
  "Do drop column from table. Using in `alter table`:
  :drop-foreign-key :KEY_name"
  [current-string column-specyfication table-name]
  (str current-string (format " `%s` DROP FOREIGN KEY %s;" table-name (name column-specyfication ))))

(defn drop-column-string
  "Do drop column from table. Using in `alter table`:
  :drop-column :name"
  [current-string column-specyfication table-name]
  (str current-string (format " `%s` DROP COLUMN `%s`;" table-name (name column-specyfication ))))

(defn add-foreign-key-string
  "Add foreign key to table to table. Using in `alter table`:
  [{:id_permission :permission} {:update :cascade :delete :restrict}]"
  [current-string column-specyfication table-name]
  (str current-string (format " `%s` ADD %s;" table-name (apply alter-table-constraint-create table-name column-specyfication))))

(defn add-column-string
  "Add column to table. Using in `alter table`:
  :add-column {:suka [:bigint-20 \"NOT NULL\"]}"
  [current-string column-specyfication table-name]
  (str current-string (format " `%s` ADD %s;" table-name (create-column column-specyfication))))

(defn modify-column-string
  "Modify column type to field. Using in `alter table`:
  :modify {:suka [:bigint-20 \"NOT NULL\"]}"
  [current-string column-specyfication table-name]
  (str current-string (format " `%s` modify %s;" table-name (create-column column-specyfication))))

;;;;;;;;;;;;;;;;;;;;;;;;
;;; pipeline helpers ;;;
;;;;;;;;;;;;;;;;;;;;;;;;

(defn create-rule-pipeline [keys accepted-lexical-rule]
  (let [key-in? (fn [k col] (when (some #(= k %) col) (symbol (str (symbol k) "-string"))))]
    (reduce #(if-let [k (key-in? %2 keys)] (conj %1 [%2 k]) %1) [] accepted-lexical-rule)))

(defn delete-empty-table-pipeline-applier [key-pipeline]
  (if (some #(= :from (first %1)) key-pipeline)
    key-pipeline (vec (concat [[:from 'from-string]] key-pipeline))))

(defn insert-update-empty-table-pipeline-applier [key-pipeline]
  (vec (concat [[:-update-insert-empty-table 'empty-insert-update-table-string]] key-pipeline)))

(defn select-empty-table-pipeline-applier [key-pipeline]
  (if (some #(or (= :count (first %1)) (= :column (first %1))) key-pipeline)
    key-pipeline
    (vec (concat [[:column 'empty-select-string]] key-pipeline))))
 
(defn select-table-top-n-pipeline-applier [key-pipeline]
  (if (some #(= :top (first %1)) key-pipeline)
    (vec (concat [[:top 'top-string]] (filter #(not= :top (first %1)) key-pipeline)))
    key-pipeline))

(defn select-table-count-pipeline-applier [key-pipeline]
  (if (some #(= :count (first %1)) key-pipeline)
    (vec (concat [[:count 'count-string]] (filter #(not= :count (first %1)) key-pipeline)))
    key-pipeline))

(defn get-first-macro-from-pipeline [key-pipeline]
  (if (> (count key-pipeline) 0) [(first key-pipeline)] []))

(defn empty-engine-pipeline-applier [key-pipeline]
  (if (some #(= :table-config (first %1)) key-pipeline) key-pipeline
      (conj key-pipeline [:table-config 'default-table-config-string])))

;;;;;;;;;;;
;;; DOC ;;;
;;;;;;;;;;;

(defn set-spec-doc [] "    :set - udpate {(<col-name> <value-to-eq>)* }")
(defn top-spec-doc [] "    :top - describe number of record from request. Pattern <num>. ")
(defn limit-spec-doc [] "    :limit - describe number of record from request. Pattern (<num>|(vector <num> <offset-num>))")
(defn count-spec-doc [] "    :count - count raws. Usage {:distinct :<col_name>} | :* (as is, mean all) | :<col_name>")
(defn column-spec-doc [] "    :column - specify column which will returned. (:<col_name>|\"<col_name>\"|{<col_name> <replacement_col_name>})+")
(defn inner-join-spec-doc [] "    :inner-join - one of join pattern, look on *-join rule")
(defn right-join-spec-doc [] "    :right-join - one of join pattern, look on *-join rule")
(defn left-join-spec-doc [] "    :left-join - one of join pattern, look on *-join rule")
(defn outer-left-join-spec-doc [] "    :outer-left-join - one of join pattern, look on *-join rule")
(defn outer-right-join-spec-doc [] "    :outer-right-join - one of join pattern, look on *-join rule")
(defn order-spec-doc [] "    :order - set retrun order. Pattern only one (vector <col_name> [:desc|:asc])")
(defn values-spec-doc []
  "    :values - if using map, effect equeal to :set spec. but also you can add multiply values in vector
      not specifing column (vector (vector (<col-val>)+)+)")
(defn column-list-spec-doc []
  "    :column-list - is just sequence of keyword or string which specify a column you want insert data to
        for example [:name :user.lastname \"age\"]., pattern (sequable (<keyword|[\\w_.]+>)+)")
(defn *-join-spec-doc []
  "    :*-join - has one of possible ways of usage:
        (inner-join-string \"\" :repair_contract.id_old_seal->seal \"repair_contract\")
        (inner-join-string \"\" {:seal.id :repair_contract.id_old_seal} \"repair_contract\")
        (inner-join-string \"\" {:seal :id_old_seal} \"repair_contract\")
          ;; => \" INNER JOIN seal old_seal ON old_seal.id=repair_contract.id_old_seal\"
       
        (inner-join-string \"\" :repair_contract->seal \"repair_contract\")
        (inner-join-string \"\" {:seal.id :repair_contract.id_seal} \"repair_contract\")
        (inner-join-string \"\" {:seal :id_seal} \"repair_contract\")
          ;; => \" INNER JOIN seal ON seal.id=repair_contract.id_seal\"
       
        (inner-join-string \"\" [:repair_contract.id_new_seal->seal.id
                                 :repair_contract.id_old_seal->seal] \"repair_contract\")
        (inner-join-string \"\" [{:seal.id :repair_contract.id_old_seal}
                                 {:seal.id :repair_contract.id_new_seal}] \"repair_contract\")
          ;; \" INNER JOIN seal old_seal ON old_seal.id=repair_contract.id_old_seal
          ;;   INNER JOIN seal new_seal ON new_seal.id=repair_contract.id_new_seal\"
       
        (inner-join-string \"\" :one->two->three \"repair_contract\")
          ;; => \" INNER JOIN two ON two.id=one.id_two INNER JOIN three ON three.id=two.id_three\""
  ;; "    :*-join - has one of possible ways of usage:
  ;;       Table name and specify id_<key> on which table will be link.
  ;;        Pattern: {(<table-name> <table-linking-key>)*}
  ;;        Example: {:CREDENTIAL :is_user_metadata :METADATA :id_user_metadata}
  ;;          ;;=> ... INNER JOIN CREDENTIAL ON CREDENTIAL.id=user-table.is_user_metadata INNER JOIN METADATA...

  ;;       Litteraly specify how table's linking beetwean each other. 
  ;;        Pattern: {(:<table-will-joined>.<col_name> :<our-main-table>.<col_name>)*}
  ;;        Example: {:A1.id_self :user.id_user_a1 :B1.id_self :USER.id_user_b2}
  ;;          ;;=> ... RIGHT JOIN A1 ON A1.id_self=user.id_user_a1 RIGHT JOIN B1 ON B1.id_self=USER.id_user_b2

  ;;       Specify how table will link, by setting SQL join string's in vector list
  ;;        Pattern: (vector \".*\"...)
  ;;        Example: [\"suka ON suka.id=user.id_suka\" \"dupa ON dupa.id=er.id_dupara\"]
  ;;          ;;=> ... LEFT JOIN suka ON suka.id=user.id_suka LEFT JOIN ....

  ;;       Put in vector tables what you want to be linked with our main table 
  ;;        Pattern: (vector <table-name>)
  ;;        Example: [:suka :other]
  ;;          ;;=> ... OUTER LEFT JOIN suka ON suka.id=user-table.id_suka OUTER LEFT ....

  ;;       Set table you want to be linked with main table.
  ;;        Pattern: <table-name>
  ;;        Example: :credential
  ;;          ;;=> ... OUTER RIGHT JOIN credential ON credential.id=user-table.id_credential ...."
  )
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
        
        List where DSL 
          Pattern: (list (and|or|in|between|=|>|<|>=|<=){1} (<s-exp>)+)
          Pattern or|and: (list [and|or] (<s-exp>)*)
          Pattern between: (list between <col-name> <start_range> <end_range>)
          Pattern in: (list in <col-name> (list (<val>)+))
          Pattern =|>|<|>=|<=: (list [=|>|<|>=|<=] <col-val-name> <col-val-name> )
          Example:  (or (= :f1 1)
                        (>= :f1 \"bliat\")
                        (and (> :f2 2)
                             (= :f2 \"fuck\")
                             (between :f1 1 (+ 10 1000))
                             (or (= :suka \"one\")
                                 (in :one [1 2 3 (+ 1 2)]))))
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
    (select :user-table
            :inner-join {:CREDENTIAL :is_user_metadata :METADATA :id_user_metadata}
            :right-join {:A1.id_self :user.id_user_a1 :B1.id_self :USER.id_user_b2}
            :left-join [\"suka ON suka.id=user.id_suka\" \"dupa ON dupa.id=er.id_dupara\"]
            :outer-left-join [:suka :bliat]
            :outer-right-join :credential
            :column [:name :dla_mamusi :CREDENTAIL.login]
            :where (or (= :f1 1)
                       (>= :f1 \"bliat\")
                       (and (> :f2 2)
                            (= :f2 \"fuck\")
                            (between :f1 1 (+ 10 1000))
                            (or (= :suka \"one\")
                                (in :one [1 2 3 (+ 1 2)])))))
    ;; Columns show
    (select :user :column [:first_name :second_name \"bliat\" {:age :user_age}])
       ;; => \"SELECT first_name, second_name, bliat, age AS user_age FROM user\"

    ;; Top
    (select :user :top 10)

    ;; Limit
    (select :user :limit 10)
    (select :user :limit [10 20])

    ;; Count
    (select :user :count {:distinct :column})
    (select :user :count :*)
    (select :user :count :column)

    ;; Where
    (select :user-table :where {:CREDENTAIL.login \"XXXpussy_destroyer69@gmail.com\" :name \"Aleksandr\"})
    (select :user-table :where \"user-table.user = \\\"Anatoli\\\" \")
    (select :user-table
            :where (or (= :f1 1)
                       (>= :f1 \"bliat\")
                       (and (> :f2 2)
                            (= :f2 \"fuck\")
                            (between :f1 1 (+ 10 1000))
                            (or (= :suka \"one\")
                                (in :one [1 2 3 (+ 1 2)])))))
    (select :user-table
            :where [:or [:= :f1 1]
                        [:>= :f1 \"bliat\"]
                        [:and [:> :f2 2]
                             [:= :f2 \"fuck\"]
                             [:between :f1 1 (+ 10 1000)]
                             [:or [:= :suka \"one\"]
                                 [:in :one [1 2 3 (+ 1 2)]]]]])  

    
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
    SSQL experssion function `update` using for generation SQL UPDATE strings. 

  Section contain 
    Example 
    Spec-params - specs for keys you may use. See `*accepted-update-rules*`

  Rules
    `*accepted-update-rules*` - %s

  Example
    (update :user :set {:id nil, :num_c 1, :str_c \"some\"})
    (update :user :set {:num_c 1, :str_c \"some\"} :where (= :id 10))

  Spec-params
  %s" *accepted-update-rules* (string/join "\n" [(set-spec-doc)
                                                 (where-spec-doc)])))

(defn- insert-doc []
  (format "Description
    SSQL experssion function `insert` using for generation SQL INSERT strings. 

  Section contain 
    Example 
    Spec-params - specs for keys you may use. See `*accepted-insert-rules*`

  Rules
    `*accepted-insert-rules*` - %s

  Example
    (insert :user :set {:id 1, :str1_c \"vasia\", :str2_c \"123\", :num_c 20})
    (insert :user :values {:id 1, :str1_c \"vasia\", :str2_c \"123\", :num_c 20})
    (insert :user :values [[1 \"vasia\" \"123\" 20] [2 \"vasia\" \"123\" 20]])
    (insert :user 
            :column-list [:id \"first_name\" :user.last_name \"age\"]
            :values [[1 \"vasia\" \"123\" 20] [2 \"vasia\" \"123\" 20]])

  Spec-params
  %s"
          *accepted-insert-rules*
          (string/join "\n" [(set-spec-doc)
                             (values-spec-doc)
                             (column-list-spec-doc)])))

(defn- delete-doc []
  (format
   "Description
    SSQL experssion function `delete` using for generation SQL DELETE strings. 

  Section contain 
    Example 
    Spec-params - specs for keys you may use. See `*accepted-delete-rules*`
    
  Rules
    `*accepted-delete-rules*` - %s

  Example
    (delete :table-name)
    (delete :table-name :where (= :id 1))

  Spec-params
  %s"
   *accepted-delete-rules*
   (string/join "\n" [(where-spec-doc)])))

(defn- create-table-doc []
  "Description
    SSQL experssion function `create-table` using for generation sql strings

  Section contain 
    Example 
    Spec-params - specs for keys you may use. See `*accepted-ctable-rules*`
    Column name spec - specs for column declaration
    Type name spec - how to declare types

  Example
    ;; Mainly look like
    (create-table :point_of_sale
                  :columns [{:id_enterpreneur [:bigint-20-unsigned :default :null]}
                            {:name [:varchar-100 :default :null]}
                            {:physical_address  [:varchar-100 :default :null]}
                            {:telefons  [:varchar-100 :default :null]}]
                  :foreign-keys [{:id_enterpreneur :enterpreneur} {:update :cascade}])
  
    ;; but you can also put ssql `spec-parameters` into the map
    ;; WARNING! puting table name into map only with key `:table-name`
    ;; look on example belove.
    (create-table {:table-name :point_of_sale
                   :columns [{:id_enterpreneur [:bigint-20-unsigned :default :null]}
                             ....
                   :foreign-keys [{:id_enterpreneur :enterpreneur} {:update :cascade}])}
  
    ;; other example of usage 
    (create-table :table
              :columns [{:name [:varchar-100 :null]} {:some-int :integer-200} {:id_other_table :bigint-20}]
              :foreign-keys [{:id_other_table :other_table} {:update :cascade, :delete :null}]
              :table-config {:engine \"InnoDB\", :charset \"utf8\"})

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
    (alter-table :user :drop-column :bliat)
    (alter-table :user :drop-foreign-key :bliat)
    (alter-table :user :add-column {:suka [:boolean]})
    (alter-table :user :add-foreign-key [{:id_permission :permission} {:update :cascade}])")

;;;;;;;;;;;;;;;;;;;;;;;;;
;;; define-operations ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;


(defmacro define-sql-operation
  "Description
    "
  ([operation-name pipeline-function]
   `(define-sql-operation ~operation-name ~(string/upper-case (name operation-name)) ~pipeline-function))
  ([operation-name operation-string pipeline-function]
   `(defmacro ~operation-name
      ;; in this line i generate documentation from function by name `<operation-name>-doc`.
      ;; look above
      ~((resolve (symbol (str "jarman.logic.sql-tool" "/" operation-name "-doc"))))
      [~'table-name & {:as ~'args}]
      (let [~'args (if (map? ~'table-name) (dissoc ~'table-name :table-name) ~'args) 
            ~'table-name (if (map? ~'table-name) (:table-name ~'table-name) ~'table-name) 
            list-of-rules# (~pipeline-function (keys ~'args) ~(jarman.logic.sql-tool/find-rule (name operation-name)))]
        `(eval (-> ~~operation-string
                  ~@(for [[~'k ~'F] list-of-rules#]
                      `(~(symbol (str "jarman.logic.sql-tool" "/" ~'F)) ~~'(k args) (name ~~'table-name)))))))))



(define-sql-operation insert "INSERT INTO" (comp insert-update-empty-table-pipeline-applier
                                              create-rule-pipeline))
(define-sql-operation delete "DELETE FROM" (comp delete-empty-table-pipeline-applier create-rule-pipeline))
(define-sql-operation update (comp insert-update-empty-table-pipeline-applier
                                create-rule-pipeline))
(define-sql-operation select (comp select-table-count-pipeline-applier
                                select-table-top-n-pipeline-applier
                                select-empty-table-pipeline-applier
                                create-rule-pipeline )) 
(define-sql-operation create-table "CREATE TABLE IF NOT EXISTS" (comp empty-engine-pipeline-applier create-rule-pipeline))
(define-sql-operation alter-table "ALTER TABLE" (comp get-first-macro-from-pipeline create-rule-pipeline))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Non lazy impolemnetation ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmacro define-sql-operation!
  ([operation-name pipeline-function]
   `(define-sql-operation! ~operation-name ~(string/upper-case (name operation-name)) ~pipeline-function))
  ([operation-name operation-string pipeline-function]
   `(defn ~operation-name
      ;; in this line i generate documentation from function by name `<operation-name>-doc`.
      ;; look above
      ~((resolve (symbol (str "jarman.logic.sql-tool" "/" (if (= \! (last (name operation-name))) (apply str (butlast (name operation-name))) (name operation-name))  "-doc"))))
      [~'table-name & {:as ~'args}]
      (let [~'args (if (map? ~'table-name) (dissoc ~'table-name :table-name) ~'args) 
            ~'table-name (name (if (map? ~'table-name) (:table-name ~'table-name) ~'table-name))
            operation# ;; ~(name operation-name)
            (if (= \! (last ~(name operation-name))) (apply str (butlast ~(name operation-name))) ~(name operation-name))
            list-of-rules# (~pipeline-function (keys ~'args) (jarman.logic.sql-tool/find-rule operation#))
            oper-string-start# (if (= operation# (.toLowerCase ~operation-string))
                                 operation#
                                 ~operation-string)]
        (reduce
         (fn [sql-string# [rule-key# rule-fn#]]
           ((ns-resolve 'jarman.logic.sql-tool rule-fn#) sql-string# (rule-key# ~'args) ~'table-name))
         (string/upper-case oper-string-start#)
         list-of-rules#)))))

(define-sql-operation! insert! "INSERT INTO" (comp insert-update-empty-table-pipeline-applier
                                                create-rule-pipeline))
(define-sql-operation! delete! "DELETE FROM" (comp delete-empty-table-pipeline-applier
                                                create-rule-pipeline))
(define-sql-operation! update! (comp insert-update-empty-table-pipeline-applier
                                  create-rule-pipeline))
(define-sql-operation! select! (comp select-table-count-pipeline-applier
                                  select-table-top-n-pipeline-applier
                                  select-empty-table-pipeline-applier
                                  create-rule-pipeline)) 
(define-sql-operation! create-table! "CREATE TABLE IF NOT EXISTS" (comp empty-engine-pipeline-applier create-rule-pipeline))

(define-sql-operation! alter-table! "ALTER TABLE" (comp get-first-macro-from-pipeline create-rule-pipeline))

(defmacro build-partial [part-elem]
  `(fn [m#] (if ~part-elem (into m# {~(keyword (name part-elem)) ~part-elem}) m#)))

;; (define-sql-operation!
;;   select!
;;   "SELECT!"
;;   (comp
;;     select-table-count-pipeline-applier
;;     select-table-top-n-pipeline-applier
;;     select-empty-table-pipeline-applier
;;     create-rule-pipeline))

(defn select-builder [{:keys [table-name top limit count column order inner-join right-join left-join outer-left-join outer-right-join where]}]
  ;; (println [:top top] [:limit limit] [:count count] [:column column] [:order order] [:inner-join inner-join] [:right-join right-join] [:left-join left-join] [:outer-left-join outer-left-join] [:outer-right-join outer-right-join] [:where where])
  (let [if-table-name (build-partial table-name)
        if-top (build-partial top)
        if-limit (build-partial limit)
        if-count (build-partial count)
        if-column (build-partial column)
        if-order (build-partial order)
        if-inner-join (build-partial inner-join)
        if-right-join (build-partial right-join)
        if-left-join (build-partial left-join)
        if-outer-left-join (build-partial outer-left-join)
        if-outer-right-join (build-partial outer-right-join)
        if-where (build-partial where)]
    (-> {}
        if-table-name
        if-top 
        if-limit 
        if-count 
        if-column 
        if-order 
        if-inner-join 
        if-right-join 
        if-left-join 
        if-outer-left-join 
        if-outer-right-join 
        if-where 
        select!)))

;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Expression toolkit ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn change-expression
  "Replace or change some construction in clojure s-sql
  expression language and return quoted s-sql list.
   If `rule-name` is `where` and it already in inputed
  s-sql, than `rule-value` pushed with (and ...) block.
  Mean that old where clouses concatinate with `rule-value`

  Example:
  (change-expression '(select :user) :order [:suka :asc])
  (change-expression '(select :user) :order '[k-to-sort (get-in (deref inc-dec) [k-to-sort])])
  (-> '(select :user :where (= 1 2))
    (change-to-expression :where '(between :registration (date) (date 1998)))
    (change-to-expression :column [:column :blait])
    (change-to-expression :order [:column :asc]))"
  [sql-expresion rule-name rule-value & {:keys [where-rule-joiner] :or {where-rule-joiner 'or}}]
  (let [[h & t] sql-expresion
        s-sql-expresion (concat (list (transform-namespace h)) t)]
    (if (= [] (find-rule (str (first s-sql-expresion)))) s-sql-expresion
        (let [i (.indexOf s-sql-expresion rule-name)]
          (if (and (< 0 i) (< i (count s-sql-expresion)))
            (let [s-start (take (+ i 1) s-sql-expresion)
                  block   (nth s-sql-expresion (+ i 1))
                  s-end   (drop (+ i 2) s-sql-expresion)]
              (let [block-to-change (condp = rule-name
                                      :where (if (map? block)
                                               (into block rule-value)
                                               (if (or (= (first block) 'or) (= (first block) 'and))
                                                 (concat block `(~rule-value))
                                                 (concat (list where-rule-joiner) (list block) `(~rule-value))))
                                      rule-value)]
                (concat s-start (list block-to-change) s-end)))
            (concat s-sql-expresion `(~rule-name ~rule-value)))))))
;; (change-expression '(select :user :where '(= 3 4) ) :where '(or (= 1 :a) (= 1 :a)))
;; (eval (change-expression '(select :user :where (= 3 4) ) :where '(= 1 :a)))
;; (change-expression '(select :user ) :where '(or (= 1 :a) (= 1 :a)))
;; (change-expression '(select :user :order [:name :desc]) :order [:suka :bliat])

;; (concat block `(~rule-value))
(defn reduce-sql-expression
  "Description
    Change n-rules on s-sql-expression

  Example   
    (reduce-sql-expression
      '(select :user :where (= 1 2))
      '((:where (between :registration (date) (date 1998)))
        (:column [:column :blait])
        (:order [:column :asc])))
     from: (select :user :where (= 1 2)) 
     to:   (select :user :where (and (= 1 2) (between :registration (date) (date 1998))) :column [:column :blait] :order [:column :asc])

    (reduce-sql-expression
      '(select :user) [[:where ['= :a 1]]
                       [:where ['= :a 4]] 
                       [:where ['= :a 2]]] :where-rule-joiner 'or)
     from: (select :user)
     to: (jarman.logic.sql-tool/select :user :where (or [= :a 1] [= :a 4] [= :a 2]))"
  [sql expression-changes & {:keys [where-rule-joiner] :or {where-rule-joiner 'or}}]
  (reduce (fn [acc [k v]] (change-expression acc k v :where-rule-joiner where-rule-joiner)) sql expression-changes))

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
  {:pre [(keyword? database-table)]}
  (format "DROP TABLE IF EXISTS `%s`" (name database-table)))

