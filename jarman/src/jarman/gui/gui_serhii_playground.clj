(ns jarman.gui.gui-serhii-playground
  ;; (:refer-clojure :exclude [update])
  (:use seesaw.core
        seesaw.border
        seesaw.dev
        seesaw.mig)
  (:import (java.util Date)
           (java.text SimpleDateFormat)
           (java.sql SQLException)
           (java.util Calendar)
           (javax.swing SpinnerDateModel JSpinner$DateEditor)
           (javax.swing.text DateFormatter)
           (javax.swing RowFilter))
  (:require [clojure.string :as string]
            ;; resource 
            [jarman.resource-lib.icon-library :as icon]
            ;; logics
            ;; [jarman.config.config-manager :refer :all]
            ;; [jarman.gui.gui-tools :refer :all]
            ;; [jarman.gui.gui-alerts-service :refer :all]
            [jarman.logic.sql-tool :as sql]
            [jarman.config.storage :as storage]
            [jarman.config.environment :as env]
            [clojure.java.jdbc :as jdbc]
            ;; deverloper tools 
            [jarman.tools.swing :refer :all]
            [jarman.config.spec :as cfg-spec]
            [jarman.config.init :as cfg-mng]
            [jarman.logic.metadata :as metadata]
            [jarman.tools.lang :refer :all]
            ;; [jarman.gui.gui-seed :refer :all]
            ;; TEMPORARY!!!! MUST BE REPLACED BY CONFIG_MANAGER
            [jarman.config.init :refer [configuration language swapp-all save-all-cofiguration make-backup-configuration]]))

(def ^:dynamic *connection-type*
  "One of [:prod-remote :prod-local :local]"
  :local)

(defn resolve-db-connection []
  (case *connection-type*
    :prod-remote {:dbtype "mysql"
                  :host "trashpanda-team.ddns.net"
                  :port 3306
                  :dbname "jarman"
                  :user "jarman"
                  :password "dupa"}
    :prod-local {:dbtype "mysql"
                 :host "192.168.1.69"
                 :port 3306
                 :dbname "jarman"
                 :user "jarman"
                 :password "dupa"}
    :local {:dbtype "mysql"
            :host "127.0.0.1"
            :port 3306
            :dbname "jarman"
            :user "root"
            :password "1234"}))

(catch SQLException e nil)
(defmacro sqlerr
  ([f] `(sqlerr ~f
               #'identity))
  ([f f-exception] `(sqlerr ~f
                           ~f-exception
                           ~f-exception))
  ([f f-io-exception f-exception]
   `(try ~f
         (catch SQLException e# (~f-io-exception (format "I/O error. Maybe problem in file: %s" (ex-message e#))))
         (catch Exception e# (~f-exception (format "Undefinied problem: %s" (ex-message e#)))))))

(defn s-exec [s]
  (jdbc/execute! (resolve-db-connection) s))
(defn s-query [s]
  (jdbc/query (resolve-db-connection) s))


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
                                                                (str "jdbc:" subprotocol "://" host port db-sep "?socketTimeout=4000&loginTimeout=4000&connectTimeout=4000"
                                                                     )) (:user db-spec) (:password db-spec))]
        (jdbc/query connector (jdbc/prepare-statement connector "SHOW DATABASES" {:timeout 4})))
      (catch com.mysql.jdbc.exceptions.jdbc4.CommunicationsException _ nil) 
      (catch java.net.ConnectException _ nil)
      (catch Exception _ nil))))

;; (test-connection {:dbtype "mysql"
;;                   :host "trashpanda-team.ddns.net"
;;                   :port 3306
;;                   :dbname "jarman"
;;                   :user "jarman"
;;                   :password "dupa"})



;;;;;;;;;;;;;;;;;;;;;
;;; Date function ;;;
;;;;;;;;;;;;;;;;;;;;;

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
  (.parse (SimpleDateFormat. "yyyy-MM-dd HH:mm:ss") data-string))

(defn timestamp-to-date
  "java.sql.timestamp class to date"
  [^java.sql.Timestamp tstamp] (date-to-obj (.format (java.text.SimpleDateFormat. "YYYY-MM-dd HH:mm:ss") tstamp)))

;;; TEST zone ;;;

;; (defn selection-dialog [table-name]
;;   (let [;; Generated table creator macros
;;         table<<-        (eval `~(symbol (str "hrtime.database-manager/table<<-"        table-name)))
;;         model<<-        (eval `~(symbol (str "hrtime.database-manager/model<<-"        table-name)))
;;         ddata<<-        (eval `~(symbol (str "hrtime.database-manager/data<<-"         table-name)))
;;         dsqle<<-        (eval `~(symbol (str "hrtime.database-manager/sql-expr<<-"     table-name)))
;;         ;; UI components
;;         dialog   (seesaw.core/custom-dialog :modal? true :width 400 :height 500 :title (lang :search_tool))
;;         [table on-scroll] (table<<- #(seesaw.core/return-from-dialog dialog %) (eval (dsqle<<-)) nil)
;;         scroll-table  (seesaw.core/scrollable table :hscroll :as-needed :vscroll :as-needed)
;;         content  (hrtime.layout-lib/mig [[(seesaw.core/label :text (lang :write_enter_search) :halign :center :icon (image-scale icon/loupe-blue-64-png 30))]
;;                                          [(seesaw.core/text :text ""
;;                                                             :halign :center
;;                                                             :border (seesaw.border/compound-border (seesaw.border/empty-border :thickness 5) (seesaw.border/line-border :bottom 1 :color (style :color_bg_btn_hover)))
;;                                                             :listen [:action
;;                                                                      (fn [e]
;;                                                                        (let [input-text (seesaw.core/text e)]
;;                                                                          (if (= "" (clojure.string/trim input-text))
;;                                                                            (seesaw.core/config! table :model (model<<- (ddata<<- :sql (eval (dsqle<<-))))))
;;                                                                          (if (not (some #(= " " (str %)) input-text))
;;                                                                            (seesaw.core/config! table :model (model<<- (ddata<<- :filter-lambda (filter-col-by (re-pattern (clojure.string/trim (string/upper-case input-text)))) :sql (eval (dsqle<<-))))))))])]
;;                                          [scroll-table]])]
;;     (seesaw.core/config! dialog :content content)
;;     (seesaw.core/show! (doto dialog
;;                          (.setLocationRelativeTo nil)))))

(defn DataPicker
  "Create data spiner.
  The argument `some-date` set 
  a default date for spinner element
  *data-format* Is a standart parser pattern in variable
  Parsing pattern 'yyyy-MM-dd hh:mm:ss' locate
  in *data-format* dynamic variable, if you
  want to change that, just do binding

  Example:
    (DataPicker)
    (DataPicker \"1998-10-23 10:10:11\")
    (DataPicker (java.util.Date.))
    (binding [sql/*data-format* \"yyyy-MM-dd\"]
      (DataPicker \"1998-10-29\"))"
  ([] (DataPicker (date)))
  ([some-date]
   (let [date-obj (cond (= java.sql.Timestamp (class some-date)) (date-to-obj (date-format (timestamp-to-date some-date)))
                        (string? some-date) (date-to-obj some-date)
                        (= java.util.Date (class some-date)) some-date :else (date))
         spinner (javax.swing.JSpinner. (javax.swing.SpinnerDateModel.))
         date-editor (javax.swing.JSpinner$DateEditor. spinner "dd-MM-yyyy")]
     (.setAllowsInvalid (cast javax.swing.text.DateFormatter (.getFormatter (.getTextField date-editor))) false)
     (doto spinner
       (.setEditor date-editor)
       (.setValue  date-obj)))))

;; (defn export-from-csv [file-path]
;;   (with-open [reader (io/reader file-path)]
;;     (doall
;;         (map (fn [csv-line]
;;                (let [csv-row (string/split csv-line #",")
;;                      user (cons nil (drop-last 2 csv-row))
;;                      u_nr (last user)]
;;                  (if (or (nil? u_nr ) (empty? u_nr))
;;                    (let [card (concat (cons nil (take-last 2 csv-row)) [nil])]
;;                      (jdbc/execute! @sql-connection (toolbox/insert :card :values card))
;;                      nil)
;;                    (do (jdbc/execute! @sql-connection (toolbox/insert :user :values user))
;;                        (let [u_id (:id (first (jdbc/query @sql-connection (select :user :where (= :teta_nr u_nr)))))
;;                              card (concat (cons nil (take-last 2 csv-row)) [u_id])]
;;                          (jdbc/execute! @sql-connection (toolbox/insert :card :values card))
;;                          nil)))))
;;              (rest (line-seq reader))))))



(defn addTableSorter
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

(defn map-datetime [ret-m]
  (pmap (fn [m] (if-let [dt (get m :datetime)](clojure.core/assoc m :datetime (date-format (timestamp-to-date dt)))m))ret-m))

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

(defn do-clear-keys [keymap]
  (reduce (fn [acc kv] (let [[k v] (apply (fn [a b] [(name a) b]) kv)]
                        (if (or (= k "id") (= "id" (apply str (take 2 k)))) acc
                            (into acc {(keyword k) v}))))
          (array-map)
          (seq keymap)))
(defn do-clear-keys [keymap]
  (reduce (fn [acc kv] (let [[k v] (apply (fn [a b] [(name a) b]) kv)]
                        (if (or (= k "id") (= "id" (apply str (take 2 k)))) (dissoc acc (keyword k))
                            acc)))
          keymap
          (seq keymap)))

(def table-default-header (ref {:card_nr "Numer Karty", :first_name "Imie", :teta_nr "Nr w systemie", :data-to "Data do", :id_card "Karta", :section "Dzia�", :act "Nr Akt", :rfid "RFID", :datetime "Data/Czas", :last_name "Nazwisko", :id_user "U퓓tkownik", :data-from "Data od", :work_type "Typ pracownika", :direction "Kierunek"}))
(do-clear-keys
 (array-map :datetime "Data/Czas" :direction "Kierunek" :rfid "RFID Karty" :teta_nr "Nr. Teta" :first_name "Imie" :last_name "Nazwisko" :section "Sekcja" :id_user "U퓓tkownik" :id_card "Karta"))
=> {:first_name "Imie", :teta_nr "Nr. Teta", :section "Sekcja", :rfid "RFID Karty", :datetime "Data/Czas", :last_name "Nazwisko", :direction "Kierunek"}
(defn add-header-text [vec-symb-list]
  (let [f (fn [acc x] (if-let [text (x @table-default-header)] (into acc {x text}) acc))]
    (reduce f {} (map keyword vec-symb-list))))

(defmacro defview*
    "Short description:
  The macro (defview* A {:b \"1\"} (select :user))
  generate a datatype with `A` name
     (defrecord A [b])
  and two function:
     (table-model->A [m])
     (getA [sortedFunction] (select :user))

  Example generated Functions:
     `TableCard`
     `data<<-TableCard`
     `model<<-TableCard`
     `val-key-col<<-TableCard`
     `inc-dec-col<<-TableCard`
     `excel<<-TableCard`
     `table<<-TableCard`
  
  Description:
   The macro `defview` generate View module for seesaw table representation.
  Defmacro in first step create defrecord with name of first parameter `s`,
  which get keys from (keys `m`) evaluation.
   By the `m` param would be generated function, which generate table-model
  builder function. The function name start on `model<<-` symbol prefix.
   Third function is function selector. It's only wraper for SQL query, that
  return data in type declarated `s` record.
   Fourth and Fife functoin is helper function for Table component.
  `val-key-col<<-*` Return reversed key-value `m`.
  `inc-dec-col<<-*` Return map of `m` keys with deafult `:inc` values

  See also:
     (`hrtime.user-module/sort-by-column`)
     (`seesaw.table/table-model`)

  How To:
    (defview* TableCard {:rfid \"RFID\" teta_nr \"Nr. Teta\" first_name \"Imie\" last_name \"Nazwisko\"}
       \"SELECT rfid, teta_nr, first_name, last_name FROM card LEFT JOIN user ON user.id=card.id_user\")

    (defview* TableCard {:rfid \"RFID\" teta_nr \"Nr. Teta\" first_name \"Imie\" last_name \"Nazwisko\"}
       (select :card :column [:rfid :teta_nr :first_name :last_name] :left-join :user))

    (inc-dec-col<<-TableCard)
  
    (model<<-TableCard (data<<-TableCard (sort-by-column :last_name :inc))
    (model<<-TableCard (data<<-TableCard (sort-by-column :first_name :dec)))

    (data<<-TableCard (sort-by-column :first_name :dec))
    (data<<-TableCard (sort-by-column :first_name))
    (data<<-TableCard)

    (val-key-col<<-TableCard)
    (val-key-col<<-TableCard \"RF-ID\")

    (seesaw.core/frame :content (table<<-TableCard)) 
    (seesaw.core/frame :content (table<<-TableCard #'println)) 
    (seesaw.core/frame :content (table<<-TableCard (fn [x]....)))


  Explanation:
  (defview* TableCard {:rfid \"RFID\" teta_nr \"Nr. Teta\" first_name \"Imie\" last_name \"Nazwisko\"}
     (select :card :column [:rfid :teta_nr :first_name :last_name] :left-join :user))

  ;; expands to ;;
  
  (defrecord `TableCard` [rfid teta_nr first_name last_name])

  (defn `model<<-TableCard`
    [map-like-structure]
    [:columns [{:key :rfid, :text \"RF-ID\"} {:key :teta_nr, :text \"Teta NUM\"} {:key :first_name, :text \"Imie\"} {:key :last_name, :text \"Nazw.\"}]
     :rows (vec (concat map-like-structure))])

  (defn `val-key-col<<-TableCard`
    ([] {\"RF-ID\" :rfid, \"Teta NUM\" :teta_nr, \"Imie\" :first_name, \"Nazw.\" :last_name})
    ([k] (get-in {\"RF-ID\" :rfid, \"Teta NUM\" :teta_nr, \"Imie\" :first_name, \"Nazw.\" :last_name} [k])))

  (defn `inc-dec-col<<-TableCard`
    [] {:rfid :inc, :teta_nr :inc, :first_name :inc, :last_name :inc})

  (defn `data<<-TableCard`
    \"Wrapper for geting map from database\"
    [& [sorter-function]]
    (let [r (jdbc/query {:classname \"org.@sql-connection.JDBC\", :subprotocol \"@sql-connection\", :subname \"hrtime-test\"}
              (select :card :column [:rfid :teta_nr :first_name :last_name] :left-join :user))]
      (if (and sorter-function (fn? sorter-function)) (sorter-function r) r)))
  ...
  (defn `excel<<-TableCard` ...)
  (defn `tabel<<-TableCard` ...)"
  [s record-key cmap select-sql]
  (let [k (map symbol (keys (do-clear-keys cmap)))
        m (do-clear-keys cmap) 
        --table-name               (keyword (string/lower-case s))
        allkeys                    (map symbol (keys cmap))
        --val-key                  (reduce (fn [a [k v] ] (into a {v k})) {} m)
        --model-columns            (map (fn [[k v]] {:key k :text v}) m)
        --inc-dec-columns          (reduce (fn [a [k v]] (into a {k :inc})) {} m)
        ;; data manipulation
        -f-model                   (str "model<<-" (name s))
        -f-data                    (str "data<<-" (name s))
        -f-data-page               (str "data-page<<-" (name s))
        -f-temporary-data          (str "dump-data-" (gensym s))
        -f-dump-temporary-data     (str "dump-data<<-" (name s))
        -f-sql-expression          (str "sql-expr<<-" (name s))
        -f-sql-expression-state    (str "sql-expr-state<<-" (name s))
        -f-is-searching            (str "is-searching?-" (name s))
        ;; table metainformation
        -f-table-generate          (str "table<<-" (name s))
        -f-record-header           (str "record-header<<-" (name s))
        -f-scroll-table-generate   (str "scroll-table<<-" (name s))
        -f-val-key-col             (str "val-key-col<<-" (name s))
        -f-inc-dec-col             (str "inc-dec-col<<-" (name s))
        ;; additional 
        -f-excel-export            (str "excel<<-" (name s))]
    `(do
       (defrecord ~s [~@record-key])
       (defn ~(symbol -f-record-header) []
         (add-header-text '[~@record-key]))
       (def ~(symbol -f-temporary-data) (ref nil))
       (defn ~(symbol -f-model)
         "Function build table-model"
         [~'map-like-structure]
         [:columns [~@--model-columns]
          :rows (vec (concat ~'map-like-structure))])
       (defn ~(symbol -f-val-key-col)
         ([] ~--val-key)
         ([~'k] (get-in ~--val-key [~'k])))
       (defn ~(symbol -f-inc-dec-col) [] ~--inc-dec-columns)
       (def  ~(symbol -f-sql-expression-state) (ref '~select-sql))
       (defn ~(symbol -f-sql-expression) [] '~select-sql)
       (def  ~(symbol -f-is-searching) (ref nil))
       (defn ~(symbol -f-data-page)
         ([~'n]
          (~'sql/change-expression (~(symbol -f-sql-expression)) :limit [(* ~'n pagination-size) pagination-size]))
         ([~'n ~'sql]
          (~'sql/change-expression ~'sql :limit [(* ~'n pagination-size) pagination-size])))
       (defn ~(symbol -f-data)
         "Wrapper for geting map from database"
         ([& {:keys [~'filter-lambda
                     ~'sql
                     ~'page-nr
                     ~'do-cache
                     ~'sql-rules] :or {~'sql-rules []
                                       ~'do-cache true
                                       ~'page-nr 0
                                       ~'filter-lambda nil
                                       ~'sql (~(symbol -f-sql-expression))}}]
          (let [~'r (clojure.java.jdbc/query (deref ~'sql-connection) (if (not (string? ~'sql)) (eval (~(symbol -f-data-page) ~'page-nr ~'sql)) ~'sql))
                ~'db-data (map-datetime (if (and ~'filter-lambda (fn? ~'filter-lambda))
                                          (~'filter-lambda ~'r) ~'r))]
            (and ~'do-cache (dosync (ref-set ~(symbol -f-temporary-data) ~'db-data)))
            ~'db-data)))
       (defn ~(symbol -f-dump-temporary-data) [& {:keys [~'filter-lambda] :or {~'filter-lambda nil}}]
         (if (and ~'filter-lambda (fn? ~'filter-lambda))
           (~'filter-lambda (deref ~(symbol -f-temporary-data)))
           (deref ~(symbol -f-temporary-data))))
       ;; excel exporter
       (defn ~(symbol -f-excel-export)
         ([]
          (let [[~'d & ~'t] (string/split (.format (java.text.SimpleDateFormat. "YYYY-MM-dd HH MM ss") (java.util.Date. )) #" ")]
            (~(symbol -f-excel-export)
             ;; excell sheet data
             (if (deref ~(symbol -f-is-searching))
               (deref ~(symbol -f-temporary-data))
               (~(symbol -f-data) :do-cache false :sql (eval (deref ~(symbol -f-sql-expression-state)))))
             ;; sheet name 
             "sheet"
             ;; path to excel file
             (str (if-let [~'path (str (~'seesaw.chooser/choose-file :type :save :selection-mode :dirs-only))]
                    (str (string/trim ~'path) (separator))) (format "(%s)%s.xlsx" ~'d (string/join "" ~'t))))))
         ([~'data ~'sheet-name ~'file-path]
          (if ~'file-path
            (let [~'workbook (d/create-workbook ~'sheet-name
                                                (map (fn [~'data-element] (for [~'column-key (keys ~m)] (~'column-key ~'data-element)))
                                                     (cons ~m ~'data)))]
              (d/save-workbook! ~'file-path ~'workbook)))))
       (defn ~(symbol -f-table-generate)
         ([~'listener-fn ~'sql-query ~'filter-lambda]
          (let [~'T (seesaw.core/table :model (~(symbol -f-model) (~(symbol -f-data) :sql ~'sql-query :filter-lambda ~'filter-lambda)))]
            (seesaw.core/listen ~'T :selection (fn [~'e] (~'listener-fn (seesaw.table/value-at ~'T (seesaw.core/selection ~'T)))))
            (let [~'inc-dec (ref (~(symbol -f-inc-dec-col)))
                  ~'max-elm (:count (first (jdbc/query (deref sql-connection) (eval (sql/select ~--table-name :count :*)))))
                  ~'val-key (~(symbol -f-val-key-col))]
              (let [~'current-page (ref 0) 
                    ~'ttable (addTableSorter ~'T
                                             (fn [^java.awt.Point ~'point]
                                               (let [~'k-to-sort (get-in ~'val-key [(.getColumnName ~'T (.columnAtPoint ~'T ~'point))])]
                                                 (if (= (get-in (deref ~'inc-dec) [~'k-to-sort]) :asc)
                                                   (do (dosync (ref-set ~'inc-dec (reduce (fn [~'x [~'a ~'b]] (into ~'x {~'a :desc})) {} (deref ~'inc-dec))))
                                                       (dosync (ref-set ~'inc-dec (assoc-in (deref ~'inc-dec) [~'k-to-sort] :desc))))
                                                   (do (dosync (ref-set ~'inc-dec (reduce (fn [~'x [~'a ~'b]] (into ~'x {~'a :desc})) {} (deref ~'inc-dec))))
                                                       (dosync (ref-set ~'inc-dec (assoc-in (deref ~'inc-dec) [~'k-to-sort] :asc)))))
                                                 (println ~'k-to-sort (get-in (deref ~'inc-dec) [~'k-to-sort]))
                                                 (let [~'sql (sql/reduce-sql-expression (deref ~(symbol -f-sql-expression-state))
                                                                                                    [[:order [~'k-to-sort (get-in (deref ~'inc-dec) [~'k-to-sort])]]])]
                                                   (seesaw.core/config! ~'T :model (~(symbol -f-model) (~(symbol -f-data) :sql ~'sql)))
                                                   
                                                   (dosync (alter ~(symbol -f-sql-expression-state) (fn [~'_] ~'sql))
                                                           (alter ~'current-page (fn [~'_] 0)))))))
                    ~'f-next-loader (if ~'filter-lambda (fn [~'c ~'m] nil)
                                        (fn [~'current-scroll-position ~'max-scroll-position]
                                          (when (and (= ~'current-scroll-position ~'max-scroll-position) (> ~'max-elm (* (deref ~'current-page) pagination-size)))
                                            (dosync (alter ~'current-page #'inc))
                                            (let [~'loaded-data (~(symbol -f-data) :sql (deref ~(symbol -f-sql-expression-state)) :page-nr (deref ~'current-page) :do-cache false :filter-lambda ~'filter-lambda)]
                                              (if-not (< ~'max-elm (* (deref ~'current-page) pagination-size) )
                                                (do (dosync (ref-set ~(symbol -f-temporary-data) (concat (~(symbol -f-dump-temporary-data)) ~'loaded-data)))
                                                    (seesaw.core/config! ~'T :model (~(symbol -f-model) (~(symbol -f-dump-temporary-data))))))))))]
                (if ~'filter-lambda
                  (do (seesaw.core/listen ~'T :property-change
                                          (fn [~'x]
                                            (if (> ~'max-elm (* (deref ~'current-page) pagination-size))
                                              (do (dosync (alter ~'current-page #'inc))
                                                  (let [~'loaded-data (~(symbol -f-data) :sql ~'sql-query :page-nr (deref ~'current-page) :do-cache false :filter-lambda ~'filter-lambda)]
                                                    (if-not (< ~'max-elm (* (deref ~'current-page) pagination-size))
                                                      (do (dosync (ref-set ~(symbol -f-temporary-data) (concat (~(symbol -f-dump-temporary-data)) ~'loaded-data)))
                                                          (seesaw.core/config! ~'T :model (~(symbol -f-model) (~(symbol -f-dump-temporary-data)))))))))))
                      (dosync (alter ~(symbol -f-is-searching) (fn[~'x]true))))
                  (dosync (alter ~(symbol -f-is-searching) (fn[~'x]false))))
                [~'T ~'f-next-loader])))))
       (defn ~(symbol -f-scroll-table-generate)
         [~'listener-fn & {:keys [~'sql-rules ~'filter-lambda] :or {~'sql-rules [] ~'filter-lambda nil}}]
         (dosync (alter ~(symbol -f-sql-expression-state) (fn [~'_] (sql/reduce-sql-expression (~(symbol -f-sql-expression)) ~'sql-rules :where-rule-joiner (symbol "and")))))
         (let [[~'table ~'loader-function] (~(symbol -f-table-generate) ~'listener-fn (deref ~(symbol -f-sql-expression-state)) ~'filter-lambda)
               ~'scroll-panel (seesaw.core/scrollable ~'table :hscroll :as-needed :vscroll :as-needed)]
           (doto (.getVerticalScrollBar ~'scroll-panel)
             (.addAdjustmentListener (AdjustmentListener ~'loader-function)))
           ~'scroll-panel)))))

(defmacro defview [rec-name rec-keys & {:keys [view]}]
  (let [[table-representation build-in-sql] view
        into-view-map #(let [f (fn [x] (if-let [text (x @table-default-header)] [x text]))]
                         (apply array-map (mapcat f (map keyword %))))]
    (cond (map?    table-representation) `(defview* ~rec-name ~rec-keys ~table-representation ~build-in-sql)
          (vector? table-representation) `(defview* ~rec-name ~rec-keys ~(into-view-map table-representation) ~build-in-sql))))

(defview User [id first_name last_name work_type act teta_nr section]
  :view [[first_name last_name section work_type act teta_nr card_nr]
         (sql/select :user
                     :column [{:user.id :id} :first_name :last_name :section :work_type :act :teta_nr :card_nr]
                     :left-join {:card.id_user :user.id})])

;;; MACRO EXPNADED START

(defview*
  User
  [id first_name last_name work_type act teta_nr section]
  {:first_name "Imie",
   :last_name "Nazwisko",
   :section "Dzia�",
   :work_type "Typ pracownika",
   :act "Nr Akt",
   :teta_nr "Nr w systemie",
   :card_nr "Numer Karty"}
  (sql/select
    :user
    :column
    [{:user.id :id}
     :first_name
     :last_name
     :section
     :work_type
     :act
     :teta_nr
     :card_nr]
    :left-join
    {:card.id_user :user.id}))


(defrecord
    User
    [id first_name last_name work_type act teta_nr section])

(defn record-header<<-User []
  (add-header-text
   '[id first_name last_name work_type act teta_nr section]))

(def dump-data-User40477 (ref nil))

(defn model<<-User
  "Function build table-model"
  [map-like-structure]
  [:columns
   [{:key :first_name, :text "Imie"}
    {:key :last_name, :text "Nazwisko"}
    {:key :section, :text "Dzia�"}
    {:key :work_type, :text "Typ pracownika"}
    {:key :act, :text "Nr Akt"}
    {:key :teta_nr, :text "Nr w systemie"}
    {:key :card_nr, :text "Numer Karty"}]
   :rows
   (vec (concat map-like-structure))])

(defn val-key-col<<-User
  ([]
   {"Imie" :first_name,
    "Nazwisko" :last_name,
    "Dzia�" :section,
    "Typ pracownika" :work_type,
    "Nr Akt" :act,
    "Nr w systemie" :teta_nr,
    "Numer Karty" :card_nr})
  ([k]
   (get-in
    {"Imie" :first_name,
     "Nazwisko" :last_name,
     "Dzia�" :section,
     "Typ pracownika" :work_type,
     "Nr Akt" :act,
     "Nr w systemie" :teta_nr,
     "Numer Karty" :card_nr}
    [k])))

(defn inc-dec-col<<-User []
  {:first_name :inc,
   :last_name :inc,
   :section :inc,
   :work_type :inc,
   :act :inc,
   :teta_nr :inc,
   :card_nr :inc})

(def sql-expr-state<<-User
  (ref
   '(sql/select
     :user
     :column
     [{:user.id :id}
      :first_name
      :last_name
      :section
      :work_type
      :act
      :teta_nr
      :card_nr]
     :left-join
     {:card.id_user :user.id})))

(defn sql-expr<<-User []
  '(sql/select
    :user
    :column
    [{:user.id :id}
     :first_name
     :last_name
     :section
     :work_type
     :act
     :teta_nr
     :card_nr]
    :left-join
    {:card.id_user :user.id}))

(def is-searching?-User (ref nil))

(defn data-page<<-User
  ([n]
   (hrtime.sql-tool/change-expression
    (sql-expr<<-User)
    :limit
    [(* n pagination-size) pagination-size]))
  ([n sql]
   (hrtime.sql-tool/change-expression
    sql
    :limit
    [(* n pagination-size) pagination-size])))

(defn data<<-User
  "Wrapper for geting map from database"
  ([&
    {:or
     {do-cache true,
      filter-lambda nil,
      sql-rules [],
      page-nr 0,
      sql (sql-expr<<-User)},
     :keys [filter-lambda sql page-nr do-cache sql-rules]}]
   (let [r (jdbc/query
            (deref sql-connection)
            (if (not (string? sql))
              (eval (data-page<<-User page-nr sql))
              sql))
         db-data (map-datetime
                  (if (and filter-lambda (fn? filter-lambda))
                    (filter-lambda r)
                    r))]
     (and do-cache (dosync (ref-set dump-data-User40477 db-data)))
     db-data)))

(defn dump-data<<-User [&
                        {:or {filter-lambda nil},
                         :keys [filter-lambda]}]
  (if (and filter-lambda (fn? filter-lambda))
    (filter-lambda (deref dump-data-User40477))
    (deref dump-data-User40477)))

(defn excel<<-User
  ([]
   (let [[d & t] (string/split
                  (.format
                   (java.text.SimpleDateFormat.
                    "YYYY-MM-dd HH MM ss")
                   (java.util.Date.))
                  #" ")]
     (excel<<-User
      (if (deref is-searching?-User)
        (deref dump-data-User40477)
        (data<<-User
         :do-cache
         false
         :sql
         (eval (deref sql-expr-state<<-User))))
      "sheet"
      (str
       (if-let [path (str
                      (seesaw.chooser/choose-file
                       :type
                       :save
                       :selection-mode
                       :dirs-only))]
         (str (string/trim path) (separator)))
       (format "(%s)%s.xlsx" d (string/join "" t))))))
  ([data sheet-name file-path]
   (if file-path
     (let [workbook (d/create-workbook
                     sheet-name
                     (map
                      (fn [data-element]
                        (for
                            [column-key
                             (keys
                              {:first_name "Imie",
                               :last_name "Nazwisko",
                               :section "Dzia�",
                               :work_type "Typ pracownika",
                               :act "Nr Akt",
                               :teta_nr "Nr w systemie",
                               :card_nr "Numer Karty"})]
                          (column-key data-element)))
                      (cons
                       {:first_name "Imie",
                        :last_name "Nazwisko",
                        :section "Dzia�",
                        :work_type "Typ pracownika",
                        :act "Nr Akt",
                        :teta_nr "Nr w systemie",
                        :card_nr "Numer Karty"}
                       data)))]
       (d/save-workbook! file-path workbook)))))

(defn table<<-User
  ([listener-fn sql-query filter-lambda]
   (let [T (table
            :model
            (model<<-User
             (data<<-User
              :sql
              sql-query
              :filter-lambda
              filter-lambda)))]
     (listen
      T
      :selection
      (fn [e]
        (listener-fn (seesaw.table/value-at T (selection T)))))
     (let [inc-dec (ref (inc-dec-col<<-User))
           max-elm (:count
                    (first
                     (jdbc/query
                      (deref sql-connection)
                      (eval
                       (hrtime.sql-tool/select
                        :user
                        :count
                        :*)))))
           val-key (val-key-col<<-User)]
       (let [current-page (ref 0)
             ttable (addTableSorter
                     T
                     (fn [point]
                       (let [k-to-sort
                             (get-in
                              val-key
                              [(.getColumnName
                                T
                                (.columnAtPoint T point))])]
                         (if (=
                              (get-in (deref inc-dec) [k-to-sort])
                              :asc)
                           (do
                             (dosync
                              (ref-set
                               inc-dec
                               (reduce
                                (fn 
                                  [x [a b]]
                                  (into x {a :desc}))
                                {}
                                (deref inc-dec))))
                             (dosync
                              (ref-set
                               inc-dec
                               (assoc-in
                                (deref inc-dec)
                                [k-to-sort]
                                :desc))))
                           (do
                             (dosync
                              (ref-set
                               inc-dec
                               (reduce
                                (fn 
                                  [x [a b]]
                                  (into x {a :desc}))
                                {}
                                (deref inc-dec))))
                             (dosync
                              (ref-set
                               inc-dec
                               (assoc-in
                                (deref inc-dec)
                                [k-to-sort]
                                :asc)))))
                         (println
                          k-to-sort
                          (get-in (deref inc-dec) [k-to-sort]))
                         (let [sql
                               (hrtime.sql-tool/reduce-sql-expression
                                (deref sql-expr-state<<-User)
                                [[:order
                                  [k-to-sort
                                   (get-in
                                    (deref inc-dec)
                                    [k-to-sort])]]])]
                           (config!
                            T
                            :model
                            (model<<-User (data<<-User :sql sql)))
                           (dosync
                            (alter
                             sql-expr-state<<-User
                             (fn [_] sql))
                            (alter current-page (fn [_] 0)))))))
             f-next-loader (if filter-lambda
                             (fn [c m] nil)
                             (fn 
                               [current-scroll-position
                                max-scroll-position]
                               (when
                                   (and
                                    (=
                                     current-scroll-position
                                     max-scroll-position)
                                    (>
                                     max-elm
                                     (*
                                      (deref current-page)
                                      pagination-size)))
                                 (dosync (alter current-page #'inc))
                                 (let 
                                     [loaded-data
                                      (data<<-User
                                       :sql
                                       (deref sql-expr-state<<-User)
                                       :page-nr
                                       (deref current-page)
                                       :do-cache
                                       false
                                       :filter-lambda
                                       filter-lambda)]
                                   (if-not
                                       (<
                                        max-elm
                                        (*
                                         (deref current-page)
                                         pagination-size))
                                     (do
                                       (dosync
                                        (ref-set
                                         dump-data-User40477
                                         (concat
                                          (dump-data<<-User)
                                          loaded-data)))
                                       (config!
                                        T
                                        :model
                                        (model<<-User
                                         (dump-data<<-User)))))))))]
         (if filter-lambda
           (do
             (listen
              T
              :property-change
              (fn [x]
                (if (>
                     max-elm
                     (* (deref current-page) pagination-size))
                  (do
                    (dosync (alter current-page #'inc))
                    (let [loaded-data (data<<-User
                                       :sql
                                       sql-query
                                       :page-nr
                                       (deref current-page)
                                       :do-cache
                                       false
                                       :filter-lambda
                                       filter-lambda)]
                      (if-not (<
                               max-elm
                               (*
                                (deref current-page)
                                pagination-size))
                        (do
                          (dosync
                           (ref-set
                            dump-data-User40477
                            (concat
                             (dump-data<<-User)
                             loaded-data)))
                          (config!
                           T
                           :model
                           (model<<-User
                            (dump-data<<-User))))))))))
             (dosync (alter is-searching?-User (fn [x] true))))
           (dosync (alter is-searching?-User (fn [x] false))))
         [T f-next-loader])))))

(defn scroll-table<<-User [listener-fn
                           &
                           {:or {filter-lambda nil, sql-rules []},
                            :keys [sql-rules filter-lambda]}]
  (dosync
   (alter
    sql-expr-state<<-User
    (fn [_]
      (hrtime.sql-tool/reduce-sql-expression
       (sql-expr<<-User)
       sql-rules
       :where-rule-joiner
       (symbol "and")))))
  (let [[table loader-function] (table<<-User
                                 listener-fn
                                 (deref sql-expr-state<<-User)
                                 filter-lambda)
        scroll-panel (scrollable
                      table
                      :hscroll
                      :as-needed
                      :vscroll
                      :as-needed)]
    (doto
        (.getVerticalScrollBar scroll-panel)
      (.addAdjustmentListener (AdjustmentListener loader-function)))
    scroll-panel))


;;;  MACRO EXPANDED END

(s-query)

(defn sql-expr<<-User []
  '(sql/select :user
               :column [{:user.id :id} :first_name :last_name :section :work_type :act :teta_nr :card_nr]
               :left-join {:card.id_user :user.id}))

(defn model<<-User
  "Function build table-model"
  [map-like-structure]
  [:columns
   [{:key :first_name, :text "Imie"}
    {:key :last_name, :text "Nazwisko"}
    {:key :section, :text "Dzia�"}
    {:key :work_type, :text "Typ pracownika"}
    {:key :act, :text "Nr Akt"}
    {:key :teta_nr, :text "Nr w systemie"}
    {:key :card_nr, :text "Numer Karty"}]
   :rows
   (vec (concat map-like-structure))])

(defn data<<-User
  "Wrapper for geting map from database"
  ([&
    {:or
     {do-cache true, filter-lambda nil, sql-rules [], page-nr 0 sql (sql-expr<<-User)},
     :keys [filter-lambda sql page-nr do-cache sql-rules]}]
   (let [r (jdbc/query
            (deref sql-connection)
            (if (not (string? sql))
              (eval (data-page<<-User page-nr sql))
              sql))
         db-data (map-datetime
                  (if (and filter-lambda (fn? filter-lambda))
                    (filter-lambda r)
                    r))]
     (and do-cache (dosync (ref-set dump-data-User40477 db-data)))
     db-data)))

(defn dump-data<<-User [&
                        {:or {filter-lambda nil},
                         :keys [filter-lambda]}]
  (if (and filter-lambda (fn? filter-lambda))
    (filter-lambda (deref dump-data-User40477))
    (deref dump-data-User40477)))


(defn table<<-User
  ([listener-fn sql-query filter-lambda]
   (let [T (table :model (model<<-User (data<<-User
                                        :sql sql-query
                                        :filter-lambda filter-lambda)))]
     
     (listen T :selection (fn [e](listener-fn (seesaw.table/value-at T (selection T)))))
     
     (let [inc-dec (ref (inc-dec-col<<-User))
           max-elm (:count (first
                            (jdbc/query (deref sql-connection)
                                        (eval
                                         (hrtime.sql-tool/select :user :count :*)))))
           val-key (val-key-col<<-User)]
       (let [current-page (ref 0)
             ttable (addTableSorter T
                                    (fn [point]
                                      (let [k-to-sort (get-in val-key [(.getColumnName T (.columnAtPoint T point))])]
                                        (if (= (get-in (deref inc-dec) [k-to-sort]) :asc)
                                          (do (dosync (ref-set inc-dec (reduce (fn [x [a b]] (into x {a :desc})) {} (deref inc-dec))))
                                              (dosync (ref-set inc-dec (assoc-in (deref inc-dec) [k-to-sort] :desc))))
                                          (do (dosync (ref-set inc-dec (reduce (fn [x [a b]] (into x {a :desc})) {} (deref inc-dec))))
                                              (dosync (ref-set inc-dec (assoc-in (deref inc-dec) [k-to-sort] :asc)))))
                         (println k-to-sort (get-in (deref inc-dec) [k-to-sort]))
                         (let [sql (hrtime.sql-tool/reduce-sql-expression
                                    (deref sql-expr-state<<-User)
                                    [[:order [k-to-sort (get-in (deref inc-dec) [k-to-sort])]]])]
                           (config! T :model (model<<-User (data<<-User :sql sql)))
                           (dosync (alter sql-expr-state<<-User (fn [_] sql)) (alter current-page (fn [_] 0)))))))
             
             f-next-loader (if filter-lambda (fn [c m] nil)
                               (fn  [current-scroll-position max-scroll-position]
                                 (when (and (= current-scroll-position max-scroll-position) (> max-elm (* (deref current-page) pagination-size)))
                                   (dosync (alter current-page #'inc))
                                   (let  [loaded-data (data<<-User :sql (deref sql-expr-state<<-User) :page-nr (deref current-page) :do-cache false :filter-lambda filter-lambda)]
                                     (if-not (< max-elm (* (deref current-page) pagination-size))
                                       (do
                                         (dosync (ref-set dump-data-User40477 (concat (dump-data<<-User) loaded-data)))
                                         (config! T :model (model<<-User (dump-data<<-User)))))))))]
         
         (if filter-lambda
           (do (listen T :property-change
                       (fn [x] (if (> max-elm (* (deref current-page) pagination-size))
                                (do (dosync (alter current-page #'inc))
                                    (let [loaded-data (data<<-User :sql sql-query :page-nr (deref current-page) :do-cache false :filter-lambda filter-lambda)]
                                      (if-not (< max-elm (* (deref current-page) pagination-size))
                                        (do (dosync (ref-set dump-data-User40477 (concat (dump-data<<-User) loaded-data)))
                                            (config! T :model (model<<-User (dump-data<<-User))))))))))
               (dosync (alter is-searching?-User (fn [x] true))))
             (dosync (alter is-searching?-User (fn [x] false))))
         [T f-next-loader])))))
