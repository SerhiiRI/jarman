(ns jarman.new-data-manager
  (:gen-class)
  (:require [jarman.sql-tool :as sql :include-macros true :refer :all]
            [jarman.config-manager :as cm]
            [jarman.dev-tools :as dt]
            [clojure.string :as string]
            [clojure.java.jdbc :as jdbc]
            [jarman.data :as mt]))


(def pagination-size (cm/getset "database.edn" [:pagination-size] 150))
(def sql-connection  (cm/getset "database.edn" [:JDBC-mariadb-configuration-database]
                                {:dbtype "mysql"
                                 :host "127.0.0.1"
                                 :port 3306
                                 :dbname "ekka-test"
                                 :user "root"
                                 :password "123"}))


(defn test-connection [db-spec]
  ;; {:pre [(spec/valid? ::db-connector-scheme db-spec)]}
  (let [subprotocols {"hsql"       "hsqldb"
                      "jtds"       "jtds:sqlserver"
                      "mssql"      "sqlserver"
                      "oracle"     "oracle:thin"
                      "oracle:sid" "oracle:thin"
                      "postgres"   "postgresql"}
        host-prefixes {"oracle:oci"  "@"
                       "oracle:thin" "@"}
        dbname-separators {"mssql"      ";DATABASENAME="
                           "sqlserver"  ";DATABASENAME="
                           "oracle:sid" ":"}
        dbtype (:dbtype db-spec)
        port (:port db-spec)
        host (:host db-spec)
        subprotocol (subprotocols dbtype dbtype)
        port (when port (str ":" port))
        db-sep (dbname-separators dbtype "/")]
    (java.sql.DriverManager/setLoginTimeout 1) 
    (try
      (let [connector (java.sql.DriverManager/getConnection (do (println (str "jdbc:" subprotocol "://" host port))
                                                                (str "jdbc:" subprotocol "://" host port db-sep "?socketTimeout=4000&loginTimeout=4000&connectTimeout=4000"))
                                                            (:user db-spec)
                                                            (:password db-spec))]
        (jdbc/query connector (jdbc/prepare-statement connector "SHOW DATABASES" {:timeout 4})))
      (catch com.mysql.jdbc.exceptions.jdbc4.CommunicationsException _ nil) 
      (catch java.net.ConnectException _ nil)
      (catch Exception _ nil))))

(defn query [sql-form]
  ((if (= "select"
          (string/lower-case (-> (if-not (string? sql-form)
                                    (eval (sql-form))
                                    sql-form)
                                  (.substring 0 6))))
     #'jdbc/query #'jdbc/execute!)
   sql-connection sql-form))

;;;;;;;;;;;;;;;;;;;;;;;
;;; table component ;;;
;;;;;;;;;;;;;;;;;;;;;;;

(defn TableSorter
  "Something to creating table"
  [^javax.swing.JTable T point-lambda]
  (doto (.getTableHeader T)
    (.addMouseListener
     (proxy [java.awt.event.MouseAdapter] []
       (^void mouseClicked [^java.awt.event.MouseEvent e]
        (point-lambda (.getPoint e)))))) T)

(defn AdjustmentListener
  "(f [suwaczek-position scrollbarMax]..)" [f]
  (proxy [java.awt.event.AdjustmentListener] []
   (adjustmentValueChanged [^java.awt.event.AdjustmentEvent ae]
     (let [scrollBar (cast javax.swing.JScrollBar (.getAdjustable ae))
           extent (.. scrollBar getModel getExtent)]
       (f (+ (.. scrollBar getValue) extent) (.. scrollBar getMaximum))))))

(defn addTableModelListener [f]
  (proxy [javax.swing.event.TableModelListener] []
    (tableChanged [^javax.swing.event.TableModelEvent e]
      (f e))))


{:id 30,
 :table "user",
 :prop {:table {:representation "user", :private? false, :scallable? true, :linker? false},
        :columns [{:field "login", :representation "login", :description nil, :component-type "i", :column-type "varchar(100)", :private? false, :editable true}
                  {:field "password", :representation "password", :description nil, :component-type "i", :column-type "varchar(100)", :private? false, :editable true}
                  {:field "first_name", :representation "first_name", :description nil, :component-type "i", :column-type "varchar(100)", :private? false, :editable true}
                  {:field "last_name", :representation "last_name", :description nil, :component-type "i", :column-type "varchar(100)", :private? false, :editable true}
                  {:field "id_permission", :representation "id_permission", :description nil, :component-type "l", :column-type "bigint(120) unsigned", :private? false, :editable true}]}}


(defn router [& body]
  (fn [table] ((comp second first) (filter (fn [[route _]](dt/in? route :user)) (partition 2 body)))))
;; ((router
;;   :user                                   "special data design"
;;   (map (comp keyword :table) (mt/getset)) "automatic gui builder") :user)

(defn *-processor []
  (comp 1 2 3 4 dosql ))





(defmulti filter-col-by
  "Describe:
  Return function which do filter on Map Data
  by column(-s), using re-gex-pattern for matching
  
  If argument is RegularExpr patter, than match
  by values of all columns
  If argument is {keyword RegularExpr}, then match
  only by column, choosed by 'keyword' map key.

  Return:
    (fn [col-map]) => col-map 
  
  Example:
    (filter-col-by #\"DUPA\")
    (filter-col-by {:first_name #\"DUPA\"})"
  class)
(defmethod filter-col-by nil [pattern] (fn [col] nil))
(defmethod filter-col-by :default [pattern] (fn [col] col))
(defmethod filter-col-by java.util.regex.Pattern [pattern]
  (fn [col] 
    (filter (fn [item] (not (empty? (re-seq pattern (string/join "" (vals item)))))) col)))
(defmethod filter-col-by clojure.lang.PersistentArrayMap [pattern]
  (let [[k v] (first pattern)]
    (fn [col] (filter (fn [item] (not (empty? (re-seq v (k item))))) col))))

(defn sort-by-column
  "Describe:re
  Return function which to sorting on Map Data
  by column. Defacto wraper, which apply list to
  sort+reverse functionality.
  First argument is column for sorting, and second
  optinal argument for selection direction

  Return:
    (fn [col-map]) => col-map 
  
  Example:
    (do-sort-view :first_name)
    (do-sort-view :first_name :dec)
    (do-sort-view :first_name :inc)"
  [k & [dec-inc]] {:pre [(keyword? k)]}
  (fn [col-map] (let [sorted (sort-by k col-map)]
                  (if (= :desc dec-inc) (reverse sorted) sorted))))

{}

(defn scroll-table [listener-fn & {:keys [sql-rules filter-lambda] :or {sql-rules [] filter-lambda nil}}]
  (dosync (alter sql-state (fn [_] (hrtime.sql-tool/reduce-sql-expression (~(symbol -f-sql-expression)) sql-rules :where-rule-joiner (symbol "and")))))
  (let [[table loader-function] (table listener-fn (deref ~(symbol -f-sql-expression-state)) filter-lambda)
        scroll-panel (seesaw.core/scrollable table :hscroll :as-needed :vscroll :as-needed)]
    (doto (.getVerticalScrollBar scroll-panel)
      (.addAdjustmentListener (AdjustmentListener loader-function)))
    scroll-panel))

(defn table 
  ([listener-fn sql-query filter-lambda]
   (let [T (seesaw.core/table :model (~(symbol -f-model) (~(symbol -f-data) :sql sql-query :filter-lambda filter-lambda)))]
     (seesaw.core/listen T :selection (fn [e] (listener-fn (seesaw.table/value-at T (seesaw.core/selection T)))))
     (let [inc-dec (ref (~(symbol -f-inc-dec-col)))
           max-elm (:count (first (jdbc/query (deref sql-connection) (eval (hrtime.sql-tool/select ~--table-name :count :*)))))
           val-key (~(symbol -f-val-key-col))]
       (let [current-page (ref 0) 
             ttable (addTableSorter T
                                    (fn [^java.awt.Point point]
                                      (let [k-to-sort (get-in val-key [(.getColumnName T (.columnAtPoint T point))])]
                                        (if (= (get-in (deref inc-dec) [k-to-sort]) :asc)
                                          (do (dosync (ref-set inc-dec (reduce (fn [x [a b]] (into x {a :desc})) {} (deref inc-dec))))
                                              (dosync (ref-set inc-dec (assoc-in (deref inc-dec) [k-to-sort] :desc))))
                                          (do (dosync (ref-set inc-dec (reduce (fn [x [a b]] (into x {a :desc})) {} (deref inc-dec))))
                                              (dosync (ref-set inc-dec (assoc-in (deref inc-dec) [k-to-sort] :asc)))))
                                        (seesaw.core/config! T :model (~(symbol -f-model) (~(symbol -f-dump-temporary-data) :filter-lambda (sort-by-column k-to-sort (get-in (deref inc-dec) [k-to-sort]))))))))
             f-next-loader (if filter-lambda (fn [c m] nil)
                               (fn [current-scroll-position max-scroll-position]
                                 (when (and (= current-scroll-position max-scroll-position) (> max-elm (* (deref current-page) pagination-size)))
                                   (dosync (alter current-page #'inc))
                                   (let [loaded-data (~(symbol -f-data) :sql sql-query :page-nr (deref current-page) :do-cache false :filter-lambda filter-lambda)]
                                     (if-not (< max-elm (* (deref current-page) pagination-size) )
                                       (do (dosync (ref-set ~(symbol -f-temporary-data) (concat (~(symbol -f-dump-temporary-data)) loaded-data)))
                                           (seesaw.core/config! T :model (~(symbol -f-model) (~(symbol -f-dump-temporary-data))))))))))]
         (if filter-lambda
           (do (seesaw.core/listen T :property-change
                                   (fn [x]
                                     (if (> max-elm (* (deref current-page) pagination-size) )
                                       (do (dosync (alter current-page #'inc))
                                           (let [loaded-data (~(symbol -f-data) :sql sql-query :page-nr (deref current-page) :do-cache false :filter-lambda filter-lambda)]
                                             (if-not (< max-elm (* (deref current-page) pagination-size))
                                               (do (dosync (ref-set ~(symbol -f-temporary-data) (concat (~(symbol -f-dump-temporary-data)) loaded-data)))
                                                   (seesaw.core/config! T :model (~(symbol -f-model) (~(symbol -f-dump-temporary-data)))))
                                               ))))))
               (dosync (alter ~(symbol -f-is-searching) (fn[x]true))))
           (dosync (alter ~(symbol -f-is-searching) (fn[x]false))))
         [T f-next-loader])))))



(defn TableSorter
  "Something to creating table"
  [^javax.swing.JTable T point-lambda]
  (doto (.getTableHeader T)
    (.addMouseListener
     (proxy [java.awt.event.MouseAdapter] []
       (^void mouseClicked [^java.awt.event.MouseEvent e]
        (point-lambda (.getPoint e)))))) T)

(defn AdjustmentListener
  "(f [suwaczek-position scrollbarMax]..)" [f]
  (proxy [java.awt.event.AdjustmentListener] []
   (adjustmentValueChanged [^java.awt.event.AdjustmentEvent ae]
     (let [scrollBar (cast javax.swing.JScrollBar (.getAdjustable ae))
           extent (.. scrollBar getModel getExtent)]
       (f (+ (.. scrollBar getValue) extent) (.. scrollBar getMaximum))))))

(defn addTableModelListener [f]
  (proxy [javax.swing.event.TableModelListener] []
    (tableChanged [^javax.swing.event.TableModelEvent e]
      (f e))))

