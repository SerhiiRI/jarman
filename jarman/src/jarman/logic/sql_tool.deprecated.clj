;;;;;;;;;;;;;;;;;
;;;  OLD JOIN ;;;
;;;;;;;;;;;;;;;;;

;; (let [a (string/split (first (re-matches #"([\w\._]+(->)?)+" "suka->bliat->dupa->chuj")) #"->")]
;;   (map #(vector %1 %2) a (drop 1 a)))
;; (let [a (string/split (first (re-matches #"([\w\._]+(->)?)+" "suka->bliat->chuj")) #"->")]
;;   (map #(vector %1 %2) a (drop 1 a)))

;; (defn column-dot-resolver [table-column & {:keys [otherwise]}]
;;   (let [table-column (name table-column)]
;;     (if-let [[_ t c] (re-matches #"([\w_]+)\.([\w_]+)" table-column )]
;;       (let [[_ alias_name] (re-matches #"id_([\w_]+)" c)]
;;         [alias_name t c])
;;       (cond
;;         (fn? otherwise)
;;         [nil table-column (otherwise (string/lower-case table-column))]

;;         (keyword? otherwise)
;;         [nil table-column (name otherwise)]

;;         (string? otherwise)
;;         [nil table-column otherwise]

;;         :else
;;         [nil table-column "id"]))))

;; (defn join-keyword-string [main-table joining-table]
;;   (letfn [(column-resolver [table-column & {:keys [k]}]
;;             (if-let [[_ t c] (re-matches #"([\w_]+)\.([\w_]+)" (doto table-column println))]
;;               (doto [t c] println)
;;               [(name table-column) (str "id_" (string/lower-case (name joining-table)))]))]
;;     (if (re-matches #"([\w\._]+->)+([\w\._]+)" (name joining-table))
;;       (let [table-cols (string/split (name joining-table) #"->")]
;;         (doall (map #(join-keyword-string %1 %2) table-cols (drop 1 table-cols))))
;;       (let [[jalias jtable jcolumn] (column-dot-resolver joining-table)
;;             [malias mtable mcolumn] (column-dot-resolver main-table :otherwise (str "id_" (string/lower-case (name joining-table))))]
;;         (doto (if (and malias (not= jtable malias))
;;                 (format "%s %s ON %s.%s=%s.%s" jtable malias malias jcolumn mtable mcolumn)
;;                 (format "%s ON %s.%s=%s.%s" jtable jtable jcolumn mtable mcolumn))
;;           println)))))


;; -----------------------------------

;; (defn join-map-keyword-string [main-table map-structure]
;;   (let [[k v] (first map-structure)
;;         [table join-column] (list (name k) (name v))
;;         [_ alias_name] (re-matches #"id_([\w_]+)" join-column)]
;;     (if (and alias_name (not= table alias_name))
;;       (format "%s %s ON %s.id=%s.%s" table alias_name alias_name main-table (name join-column))
;;       (format "%s ON %s.id=%s.%s" table table main-table (name join-column)))))

;; (defn join-vector-keyword-string [main-table joining-table]
;;   (let [[table join-column] (list (name joining-table)
;;                                   (name (str "id_" (string/lower-case (name joining-table)))))]
;;     (format "%s ON %s.id=%s.%s" table table main-table join-column)))

;; (defn join-vector-string-string [main-table on-join-construction]
;;   on-join-construction)

;; (defn join-dot-map-string [main-table map-structure]
;;   (for [[joining-table main-table] map-structure
;;         :while (and (some #(= \. %) (name joining-table)) (some #(= \. %) (name main-table)))]
;;    (let [[jalias jtable jcolumn] (column-dot-resolver joining-table)
;;          [malias mtable mcolumn] (column-dot-resolver main-table :otherwise (str "id_" (string/lower-case (name joining-table))))]
;;      (doto (if (and malias (not= jtable malias))
;;              (format "%s %s ON %s.%s=%s.%s" jtable malias malias jcolumn mtable mcolumn)
;;              (format "%s ON %s.%s=%s.%s" jtable jtable jcolumn mtable mcolumn))
;;        println)))  )


;; (defn join-dot-map-string [main-table [k v]]
;;   (if-let [[[t1 id1] [t2 id2]]
;;            (and (some #(= \. %) (str k))
;;                 (some #(= \. %) (str v))
;;                 (list (string/split (str (symbol k)) #"\.")
;;                       (string/split (str (symbol v)) #"\.")))]
;;     (format "%s ON %s=%s" t1 (str (symbol k)) (str(symbol v)))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; WHERE BLOCK PROCESSOR ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn where-string [s where-block _]
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


;;;;;;;;;;;;;;;;;;;;;;;;;
;;; define-operations ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;


;; (defmacro define-sql-operation
;;   "Description
;;     "
;;   ([operation-name pipeline-function]
;;    `(define-sql-operation ~operation-name ~(string/upper-case (name operation-name)) ~pipeline-function))
;;   ([operation-name operation-string pipeline-function]
;;    `(defmacro ~operation-name
;;       ;; in this line i generate documentation from function by name `<operation-name>-doc`.
;;       ;; look above
;;       ~((resolve (symbol (str "jarman.logic.sql-tool" "/" operation-name "-doc"))))
;;       [~'table-name & {:as ~'args}]
;;       (let [~'args (if (map? ~'table-name) (dissoc ~'table-name :table-name) ~'args) 
;;             ~'table-name (if (map? ~'table-name) (:table-name ~'table-name) ~'table-name) 
;;             list-of-rules# (~pipeline-function (keys ~'args) ~(jarman.logic.sql-tool/find-rule (name operation-name)))]
;;         `(eval (-> ~~operation-string
;;                   ~@(for [[~'k ~'F] list-of-rules#]
;;                       `(~(symbol (str "jarman.logic.sql-tool" "/" ~'F)) ~~'(k args) (name ~~'table-name)))))))))



;; (define-sql-operation insert "INSERT INTO" (comp insert-update-empty-table-pipeline-applier
;;                                               create-rule-pipeline))
;; (define-sql-operation delete "DELETE FROM" (comp delete-empty-table-pipeline-applier create-rule-pipeline))
;; (define-sql-operation update (comp insert-update-empty-table-pipeline-applier
;;                                 create-rule-pipeline))
;; (define-sql-operation select (comp select-table-count-pipeline-applier
;;                                 select-table-top-n-pipeline-applier
;;                                 select-empty-table-pipeline-applier
;;                                 create-rule-pipeline )) 
;; (define-sql-operation create-table "CREATE TABLE IF NOT EXISTS" (comp empty-engine-pipeline-applier create-rule-pipeline))
;; (define-sql-operation alter-table "ALTER TABLE" (comp get-first-macro-from-pipeline create-rule-pipeline))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Non lazy impolemnetation ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; (defmacro define-sql-operation!
;;   ([operation-name pipeline-function]
;;    `(define-sql-operation! ~operation-name ~(string/upper-case (name operation-name)) ~pipeline-function))
;;   ([operation-name operation-string pipeline-function]
;;    `(defn ~operation-name
;;       ;; in this line i generate documentation from function by name `<operation-name>-doc`.
;;       ;; look above
;;       ~((resolve (symbol (str "jarman.logic.sql-tool" "/" (if (= \! (last (name operation-name))) (apply str (butlast (name operation-name))) (name operation-name))  "-doc"))))
;;       [~'table-name & {:as ~'args}]
;;       (let [~'args (if (map? ~'table-name) (dissoc ~'table-name :table-name) ~'args) 
;;             ~'table-name (name (if (map? ~'table-name) (:table-name ~'table-name) ~'table-name))
;;             operation# ;; ~(name operation-name)
;;             (if (= \! (last ~(name operation-name))) (apply str (butlast ~(name operation-name))) ~(name operation-name))
;;             list-of-rules# (~pipeline-function (keys ~'args) (jarman.logic.sql-tool/find-rule operation#))
;;             oper-string-start# (if (= operation# (.toLowerCase ~operation-string))
;;                                  operation#
;;                                  ~operation-string)]
;;         (reduce
;;          (fn [sql-string# [rule-key# rule-fn#]]
;;            ((ns-resolve 'jarman.logic.sql-tool rule-fn#) sql-string# (rule-key# ~'args) ~'table-name))
;;          (string/upper-case oper-string-start#)
;;          list-of-rules#)))))

;; (defmacro define-sql-operation!
;;   ([operation-name pipeline-function]
;;    `(define-sql-operation! ~operation-name ~(string/upper-case (name operation-name)) ~pipeline-function))
;;   ([operation-name operation-string pipeline-function]
;;    `(defn ~operation-name
;;       ;; in this line i generate documentation from function by name `<operation-name>-doc`.
;;       ;; look above
;;       ~((resolve (symbol (str "jarman.logic.sql-tool" "/" (if (= \! (last (name operation-name))) (apply str (butlast (name operation-name))) (name operation-name))  "-doc"))))
;;       [& {:as ~'args}]
;;       (let [operation# ;; ~(name operation-name)
;;             (if (= \! (last ~(name operation-name))) (apply str (butlast ~(name operation-name))) ~(name operation-name))
;;             table-name# (:table-name ~'args)
;;             list-of-rules# (~pipeline-function (keys ~'args) (jarman.logic.sql-tool/find-rule operation#))
;;             oper-string-start# (if (= operation# (.toLowerCase ~operation-string))
;;                                  operation#
;;                                  ~operation-string)]
;;         (if-not table-name# (throw (Exception. "the `:table-name` key is required")))
;;         (reduce
;;          (fn [sql-string# [rule-key# rule-fn#]]
;;            ((ns-resolve 'jarman.logic.sql-tool rule-fn#) sql-string# (rule-key# ~'args) table-name#))
;;          (string/upper-case oper-string-start#)
;;          list-of-rules#)))))


(defmacro build-partial [part-elem]
  `(fn [m#] (if ~part-elem (into m# {~(keyword (name part-elem)) ~part-elem}) m#)))

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
