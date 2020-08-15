(ns jarman.database-manager
  (:gen-class)
  (:use
   seesaw.chooser)
  (:require
   ;; standart lib
   [clojure.string :as string]
   [clojure.java.jdbc :as jdbc]
   [clojure.java.io :as io]
   ;; local functionality
   [jarman.logic.sql-tool :as toolbox :include-macros true :refer :all]
   [jarman.logic.dev-tools :refer [image-scale]]
   [jarman.resource-lib.icon-library :as icon]))


;;;;;;;;;;;;;;;;;;;;;;;;;
;;; DB configurations ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;
(def config (config-file "database.edn"))
(def pagination-size (config [:pagination-size] 150))
(def table-default-header (ref (config [:View-table-name])))
;; (def sql-connection (ref (config [:JDBC-mariadb-configuration-database])))
;; (def sql-connection "Database spec" (ref {:dbtype "mysql", :host "localhost", :port 3306, :dbname "hrtime", :user "root", :password "123"}))
(def sql-connection (ref {:dbtype "mysql"
                          :host "127.0.0.1"
                          :port 3306
                          :dbname "ekka-test"
                          :user "root"
                          :password "123"}))

(defn refreshConfig [] 
  (let [conf (config-file "database.edn")]
    (do 
      (dosync (ref-set table-default-header (conf [:View-table-name])))
      (dosync (ref-set sql-connection       (conf [:JDBC-mariadb-configuration-database])))
      (println @sql-connection)
      (println "Connection refreshed........."))))

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

(def UI-combo-column {:work_type (lang :worker_type) :section (lang :section)})

;;;;;;;;;;;;;;
;;; config ;;;
;;;;;;;;;;;;;;

(def user
  (create-table :user
                :columns [{:first_name [:varchar-100 :default :null]}
                          {:last_name  [:varchar-50 :default :null]}
                          {:work_type  [:varchar-50 :default :null]}
                          {:act        [:varchar-12 :default :null]}
                          {:section    [:varchar-20 :default :null]}]))

(def card
  (create-table :card
                :columns [{:rfid    [:varchar-20 :default :null]}
                          {:card_nr [:varchar-20 :default :null]}
                          {:id_user [:bigint-120-unsigned :null]}]
                :foreign-keys [{:id_user :user} {:delete :null :update :null}]))


(def registration
  (create-table :registration
                :columns [{:datetime  [:datetime :default :null]}
                          {:direction [:varchar-3 :default :null]}
                          {:id_user   [:bigint-120-unsigned :null]}
                          {:id_card   [:bigint-120-unsigned :null]}]
                :foreign-keys [{:id_user :user} {:delete :null :update :null}]
                :foreign-keys [{:id_card :card} {:delete :null :update :null}]))


(defn drop-all-tables []
  (jdbc/execute! @sql-connection (drop-table :registration))
  (jdbc/execute! @sql-connection (drop-table :card))
  (jdbc/execute! @sql-connection (drop-table :user)))
(defn create-all-tables []
  (jdbc/execute! @sql-connection user)
  (jdbc/execute! @sql-connection card)
  (jdbc/execute! @sql-connection registration))
;; (do (drop-all-tables)
;;     (create-all-tables))

;;;;;;;;;;;;;;;;;;;;;;
;;; Data generator ;;;
;;;;;;;;;;;;;;;;;;;;;;



(defn export-from-csv [file-path]
  (with-open [reader (io/reader file-path)]
    (doall
        (map (fn [csv-line]
               (let [csv-row (string/split csv-line #",")
                     user (cons nil (drop-last 2 csv-row))
                     u_nr (last user)]
                 (if (or (nil? u_nr ) (empty? u_nr))
                   (let [card (concat (cons nil (take-last 2 csv-row)) [nil])]
                     (jdbc/execute! @sql-connection (toolbox/insert :card :values card))
                     nil)
                   (do (jdbc/execute! @sql-connection (toolbox/insert :user :values user))
                       (let [u_id (:id (first (jdbc/query @sql-connection (select :user :where (= :teta_nr u_nr)))))
                             card (concat (cons nil (take-last 2 csv-row)) [u_id])]
                         (jdbc/execute! @sql-connection (toolbox/insert :card :values card))
                         nil)))))
             (rest (line-seq reader))))))



;; (let [ucX (jdbc/query @sql-connection (select :card :right-join :user :column [{:user.id :id_user} {:card.id :id_card}]))
;;       ucf (fn []
;;             (vec (let [uc (rand-nth ucX)]
;;                [nil (date (+ 2011 (rand-int 10)) (rand-int 10)) (rand-nth ["we" "wy"]) (:id_user uc) (:id_card uc)])))]
;;   (jdbc/execute! @sql-connection 
;;                  (toolbox/insert :registration :values (for [x (range 10000)] (ucf ) ))))

;; (export-from-csv "users1.csv")
;; (defn generate-dummy-data-card []
;;   (let[already-used (reduce (fn [a x] (if-let [u_id (:id_user x)] (conj a u_id) a)) [] (jdbc/query @sql-connection (select :card)))]
;;     (map #(jdbc/execute! @sql-connection (toolbox/insert :card :values [nil (format "rfid%s" (rand-int 9898859)) (:id %) ]))
;;          (filter (fn [u] (not (some #(= % (:id u)) already-used))) (jdbc/query @sql-connection (select :user))))))

;; (defn generate-dummy-data-user []
;;   (map #(jdbc/execute! @sql-connection (toolbox/insert :user :values [nil (format "f%s" %) (format "l%s" %) (format "w%s" %) (format "s%s" %) (format "s%s" %)]))
;;        (range 1 30)))

;; (defn generate-dummy-data-registration []
;;   (map #(if (nil? (:id_user %)) nil
;;             (jdbc/execute! @sql-connection (toolbox/insert :registration
;;                                                            :values [nil (date) (if (= 0 (rand-int 2)) (lang :in_short) (lang :out_short)) (:id %) (:id_user %)])))
;;        (jdbc/query @sql-connection (select :card))))

;; (defn refresh-test-bd []
;;   (do
;;     (drop-all-tables)
;;     (create-all-tables)
;;     (do (generate-dummy-data-user))
;;     (do (generate-dummy-data-card))
;;     (do (generate-dummy-data-registration))))

;;;;;;;;;;;;;;;;;;;;;;
;;; Data managment ;;;
;;;;;;;;;;;;;;;;;;;;;;

(defrecord Card [id rfid card_nr id_user])
(defrecord User [id first_name last_name work_type act section teta_nr])
(defrecord Registration [id datetime direction id_user id_card])

(defn ^clojure.lang.PersistentList get-sql-by-id
  "Example: 
   (get-sql-by-id :card 4 map->Card)
   (get-sql-by-id :user 4 map->User)
   (get-sql-by-id :registration 4 map->Registration)"
  ([table id]
   (jdbc/query @sql-connection (select table :where (= :id id))))
  ([table id mapper]
   (map mapper (get-sql-by-id table id))))

(defn ^clojure.lang.PersistentList update-sql-by-id
  "Description
  Do insert or update do DB, depended on `m` data `:id`
  keyword data. If id is nil, than do insert. 
  
  Example:
  (update-sql-by-id :card
     (map->Card {:id 4, :rfid \"sukabliat\", :id_user 4}))
  (update-sql-by-id :user
     (map->User {:id 5, :first_name \"Adam\",
                 :last_name \"Nowak\", :work_type \"w5\",
                 :section \"s5\", :teta_nr \"s5\"}))
  (update-sql-by-id :registration
      (map->Registration {:id 5, :datetime (.format (java.text.SimpleDateFormat. \"YYYY-MM-dd HH:MM:ss\") (java.util.Date. ))
                          :direction \"wyjscie\",
                          :id_user 6, :id_card 6}))"  
  ([table m]
   (if (:id m)
     (jdbc/execute! @sql-connection (update table :set m :where (= :id (:id m))))
     (jdbc/execute! @sql-connection (insert table :values (vals m))))))

(defn ^clojure.lang.PersistentList update-sql-by-id-template
  "Description
  Do insert or update do DB, depended on `m` data `:id`
  keyword data. If id is nil, than do insert. 
  
  Example:
  (update-sql-by-id :card
     (map->Card {:id 4, :rfid \"sukabliat\", :id_user 4}))
  (update-sql-by-id :user
     (map->User {:id 5, :first_name \"Adam\",
                 :last_name \"Nowak\", :work_type \"w5\",
                 :section \"s5\", :teta_nr \"s5\"}))
  (update-sql-by-id :registration
      (map->Registration {:id 5, :datetime (.format (java.text.SimpleDateFormat. \"YYYY-MM-dd HH:MM:ss\") (java.util.Date. ))
                          :direction \"wyjscie\",
                          :id_user 6, :id_card 6}))"  
  ([table m]
   (if (:id m)
     (update table :set m :where (= :id (:id m)))
     (insert table :values (vals m)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; `Seesaw.core/table` modele generator ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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
                          (into acc {(keyword k) v})))) (array-map)
          (seq keymap)))
(defn do-clear-keys [keymap]
  (reduce (fn [acc kv] (let [[k v] (apply (fn [a b] [(name a) b]) kv)]
                        (if (or (= k "id") (= "id" (apply str (take 2 k)))) (dissoc acc (keyword k))
                            acc))) keymap
          (seq keymap)))

(do-clear-keys
 (array-map :datetime "Data/Czas" :direction "Kierunek" :rfid "RFID Karty" :teta_nr "Nr. Teta" :first_name "Imie" :last_name "Nazwisko" :section "Sekcja" :id_user "Użytkownik" :id_card "Karta"))
;; => {:first_name "Imie", :teta_nr "Nr. Teta", :section "Sekcja", :rfid "RFID Karty", :datetime "Data/Czas", :last_name "Nazwisko", :direction "Kierunek"}
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
          (~'hrtime.sql-tool/change-expression (~(symbol -f-sql-expression)) :limit [(* ~'n pagination-size) pagination-size]))
         ([~'n ~'sql]
          (~'hrtime.sql-tool/change-expression ~'sql :limit [(* ~'n pagination-size) pagination-size])))
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
                ~'db-data (if (and ~'filter-lambda (fn? ~'filter-lambda))
                            (~'filter-lambda ~'r) ~'r)]
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
             (format "sheet")
             ;; path to excel file
             (str (if-let [~'path (str (~'seesaw.chooser/choose-file :type :save :selection-mode :dirs-only))]
                    (str (string/trim ~'path) "\\")) (format "(%s)%s.xlsx" ~'d (string/join "" ~'t))))))
         ([~'data ~'sheet-name ~'file-path]
          ;; (println (first ~'data))
          (let [~'workbook {~'sheet-name (excel/table (for [~'row ~'data]
                                                        (into {} (map (fn [~'tkey]
                                                                        (let [~'col-name (~'tkey ~m)]
                                                                         (cond
                                                                           (= :datetime ~'tkey)    {~'col-name (hrtime.sql-tool/date-format (~'tkey ~'row))} 
                                                                           :else                   {~'col-name (~'tkey ~'row)})))
                                                                      ~(vec (keys m))))))}]
            (excel/write! ~'workbook ~'file-path)
            (excel/open ~'file-path))))
       (defn ~(symbol -f-table-generate)
         ([~'listener-fn ~'sql-query ~'filter-lambda]
          (let [~'T (seesaw.core/table :model (~(symbol -f-model) (~(symbol -f-data) :sql ~'sql-query :filter-lambda ~'filter-lambda)))]
            (seesaw.core/listen ~'T :selection (fn [~'e] (~'listener-fn (seesaw.table/value-at ~'T (seesaw.core/selection ~'T)))))
            (let [~'inc-dec (ref (~(symbol -f-inc-dec-col)))
                  ~'max-elm (:count (first (jdbc/query (deref sql-connection) (eval (hrtime.sql-tool/select ~--table-name :count :*)))))
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
                                                 (seesaw.core/config! ~'T :model (~(symbol -f-model) (~(symbol -f-dump-temporary-data) :filter-lambda (~'sort-by-column ~'k-to-sort (get-in (deref ~'inc-dec) [~'k-to-sort]))))))))
                    ~'f-next-loader (if ~'filter-lambda (fn [~'c ~'m] nil)
                                        (fn [~'current-scroll-position ~'max-scroll-position]
                                          (when (and (= ~'current-scroll-position ~'max-scroll-position) (> ~'max-elm (* (deref ~'current-page) pagination-size)))
                                            (dosync (alter ~'current-page #'inc))
                                            (let [~'loaded-data (~(symbol -f-data) :sql ~'sql-query :page-nr (deref ~'current-page) :do-cache false :filter-lambda ~'filter-lambda)]
                                              (if-not (< ~'max-elm (* (deref ~'current-page) pagination-size) )
                                                (do (dosync (ref-set ~(symbol -f-temporary-data) (concat (~(symbol -f-dump-temporary-data)) ~'loaded-data)))
                                                    (seesaw.core/config! ~'T :model (~(symbol -f-model) (~(symbol -f-dump-temporary-data))))))))))]
                (if ~'filter-lambda
                  (do (seesaw.core/listen ~'T :property-change
                                          (fn [~'x]
                                            (if (> ~'max-elm (* (deref ~'current-page) pagination-size) )
                                              (do (dosync (alter ~'current-page #'inc))
                                                  (let [~'loaded-data (~(symbol -f-data) :sql ~'sql-query :page-nr (deref ~'current-page) :do-cache false :filter-lambda ~'filter-lambda)]
                                                    (if-not (< ~'max-elm (* (deref ~'current-page) pagination-size))
                                                      (do (dosync (ref-set ~(symbol -f-temporary-data) (concat (~(symbol -f-dump-temporary-data)) ~'loaded-data)))
                                                          (seesaw.core/config! ~'T :model (~(symbol -f-model) (~(symbol -f-dump-temporary-data)))))
                                                      ))))))
                      (dosync (alter ~(symbol -f-is-searching) (fn[~'x]true))))
                  (dosync (alter ~(symbol -f-is-searching) (fn[~'x]false))))
                [~'T ~'f-next-loader])))))
       (defn ~(symbol -f-scroll-table-generate)
         [~'listener-fn & {:keys [~'sql-rules ~'filter-lambda] :or {~'sql-rules [] ~'filter-lambda nil}}]
         (dosync (alter ~(symbol -f-sql-expression-state) (fn [~'_] (hrtime.sql-tool/reduce-sql-expression (~(symbol -f-sql-expression)) ~'sql-rules :where-rule-joiner (symbol "and")))))
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


(defview User [id first_name last_name work_type act section]
  :view [[first_name last_name section work_type act card_nr]
         (hrtime.sql-tool/select :user
                                 :column [{:user.id :id} :first_name :last_name :section :work_type :act :card_nr]
                                 :left-join {:card.id_user :user.id})])
(defview Card [id rfid card_nr id_user]
  :view [[card_nr act first_name last_name id_user]
         (hrtime.sql-tool/select :card
                                 :column [{:card.id :id} :card_nr :act :first_name :last_name]
                                 :left-join :user)])
(defview Registration [id datetime direction id_user id_card]
  :view [[first_name last_name act card_nr datetime section direction id_card id id_user]
         (hrtime.sql-tool/select :registration
                                 :column [{:registration.id :id} :datetime :direction :card_nr :teta_nr :first_name :last_name :section]
                                 :left-join [:user :card])])

;; (selection-dialog "User")
(defn selection-dialog [table-name]
  (let [;; Generated table creator macros
        scroll-table<<- (eval `~(symbol (str "hrtime.database-manager/scroll-table<<-" table-name)))
        model<<-        (eval `~(symbol (str "hrtime.database-manager/model<<-"        table-name)))
        ddata<<-        (eval `~(symbol (str "hrtime.database-manager/dump-data<<-"    table-name)))
        ;; UI components 
        dialog   (seesaw.core/custom-dialog :modal? true :width 400 :height 500 :title (lang :search_tool))
        table    (scroll-table<<- #(seesaw.core/return-from-dialog dialog %))
        content  (hrtime.layout-lib/mig [[(seesaw.core/label :text (lang :write_enter_search) :halign :center :icon (image-scale icon/loupe-blue-64-png 30))]
                                         [(seesaw.core/text :text ""
                                                            :halign :center
                                                            :border (seesaw.border/compound-border (seesaw.border/empty-border :thickness 5) (seesaw.border/line-border :bottom 1 :color (style :color_bg_btn_hover)))
                                                            :listen [:action
                                                                     (fn [e]
                                                                       (let [input-text (seesaw.core/text e)]
                                                                         (if (= "" (clojure.string/trim input-text))
                                                                           (seesaw.core/config! table :model (ddata<<-)))
                                                                         (if (not (some #(= " " (str %)) input-text))
                                                                           (seesaw.core/config! table :model (model<<- (ddata<<- :filter-lambda (filter-col-by (re-pattern (clojure.string/trim input-text)))))))))])]
                                         [table]])]
    (seesaw.core/config! dialog :content content)
    (seesaw.core/show! (doto dialog
                         (.setLocationRelativeTo nil)))))

(defn get-human-readable-data 
  "(get-human-readable-data  {:datetime \"Data/Czas\"
                             :suka \"bliat\"
                             :direction \"Kierunek\"
                             :id_card \"Karta\"
                             :id_user \"Użytkownik\"})" [d]
  (clojure.string/join " " (reduce #(let [[kk v] %2 k (name kk)]
              (if (or (= k "id")
                      (= k "work_type")
                      (= "id" (apply str (take 2 k)))
                      (not= 0 (count (re-seq #"date" k))))
                %1 (conj %1 v))) [] (seq d))))



(defn construct-dialog [k v reference-value]
  (let [[_ related-table] (string/split k #"_")
        [h & r] related-table
        table-name (str (clojure.string/upper-case h) (apply str r))
        get-text (fn [] (if (nil? (get-in @reference-value [(keyword k)])) (lang :click_to_choose)
                            (get-human-readable-data
                             (first (jdbc/query @sql-connection (select (keyword related-table)
                                                                       :where (= :id ((keyword k) @reference-value))))))))
        selected-value (fn [e] (let [m (selection-dialog table-name)]
                                 (dosync (ref-set reference-value (clojure.core/assoc @reference-value (keyword k) (:id m))))
                                 (seesaw.core/config! e :text (get-text))))
        text-label (seesaw.core/label
                    :cursor :hand
                    :font (style :font_regular)
                    :border (seesaw.border/compound-border (seesaw.border/empty-border :thickness 5) (seesaw.border/line-border :bottom 1 :color (style :color_bg_btn_hover)))
                    :listen [:mouse-clicked selected-value]
                    :text (get-text))
        dialog-label (hrtime.layout-lib/mig [[text-label]
                                             [(seesaw.core/label :icon (image-scale icon/basket-grey1-64x64-png 40)
                                                                 :tip (lang :tip_user_trash_can)
                                                                 :listen [:mouse-entered (fn [e] (seesaw.core/config! e :cursor :hand :icon (image-scale icon/basket-blue1-64x64-png 40)))
                                                                          :mouse-exited  (fn [e] (seesaw.core/config! e :cursor :default :icon (image-scale icon/basket-grey1-64x64-png 40)))
                                                                          :mouse-clicked (fn [e] (if ((keyword k) @reference-value) 
                                                                                                   (do (dosync (ref-set reference-value (assoc @reference-value (keyword k) nil)))
                                                                                                       (seesaw.core/config! text-label :text (get-text)))))])]]
                                            :args [:constraints ["" "0px[grow, fill]10px[20, fill]0px" "0px[grow, fill]0px"] 
                                                   :border v])]
    (seesaw.core/grid-panel :rows 1 :columns 3 :items [dialog-label])))

(defn construct-datapicker [k v reference-value]
  ;; setting default value for datapicker component
  (if (nil? (get-in @reference-value [(keyword k)])) (dosync (ref-set reference-value (clojure.core/assoc @reference-value (keyword k) (hrtime.sql-tool/date-format)))))
  (let [datapicker (if (nil? (get-in @reference-value [(keyword k)])) (hrtime.layout-lib/DataPicker)
                       (hrtime.layout-lib/DataPicker (get-in @reference-value [(keyword k)])))]
    (doto datapicker
      (seesaw.core/config! :border (seesaw.border/compound-border (seesaw.border/empty-border :thickness 5)
                                                                  (seesaw.border/line-border :thickness 1 :color "#fff")
                                                                  (seesaw.border/line-border :bottom 1 :color (style :color_bg_btn_hover))))
      (seesaw.core/listen :state-changed
                          (fn [e]
                            (dosync (ref-set reference-value
                                             (clojure.core/assoc @reference-value (keyword k)
                                                                 (hrtime.sql-tool/date-format (seesaw.core/selection e))))))))
    (hrtime.layout-lib/mig 
     [[datapicker]] :args [:border v])))


(defn construct-textinput 
  ([k v reference-value] 
   (let [lambda (fn [e] (dosync (ref-set reference-value (clojure.core/assoc @reference-value (keyword k) (seesaw.core/text e)))))]
     (hrtime.layout-lib/mig [[(hrtime.layout-lib/mig 
                               [[(seesaw.core/text :text (get-in @reference-value [(keyword k)])
                                                   :font (style :font_regular)
                                                   :border (seesaw.border/compound-border (seesaw.border/empty-border :thickness 5)
                                                                                          (seesaw.border/line-border :bottom 1 :color (style :color_bg_btn_hover)))
                                                   :listen [:caret-update lambda])]]
                               :args [:border v])]])))
  ([k v reference-value noedit] 
   (let [lambda (fn [e] (dosync (ref-set reference-value (clojure.core/assoc @reference-value (keyword k) (seesaw.core/text e)))))]
     (hrtime.layout-lib/mig [[(hrtime.layout-lib/mig 
                               [[(seesaw.core/text :text (get-in @reference-value [(keyword k)])
                                                   :editable? noedit
                                                   :font (style :font_regular)
                                                   :border (seesaw.border/compound-border (seesaw.border/empty-border :thickness 5)
                                                                                          (seesaw.border/line-border :bottom 1 :color (style :color_bg_btn_hover)))
                                                   :listen [:caret-update lambda])]]
                               :args [:border v])]]))))

;; Combobox
(defn construct-combobox [k v reference-value]
  (let [lambda (fn [e] (dosync (ref-set reference-value (clojure.core/assoc @reference-value (keyword k) (seesaw.core/selection e)))))
        combo (seesaw.core/combobox :model (get-in UI-combo-column [(keyword k)])
                                    :font (style :font_regular)
                                    ;; :background "#fff"
                                    :border (seesaw.border/compound-border (seesaw.border/empty-border :thickness 5)
                                                                           (seesaw.border/line-border :bottom 1 :color (style :color_bg_btn_hover)))
                                    :listen [:action-performed lambda])]
    (if-let [selected-value ((keyword k) @reference-value)] 
      (seesaw.core/selection! combo selected-value)
      (seesaw.core/selection! combo nil))
    (hrtime.layout-lib/mig [[(hrtime.layout-lib/mig
                              [[combo]]
                              :args [:border v])]])))


(defn to-arrow [a-func table]
  (symbol (str "hrtime.database-manager/" a-func table)))


(defn construct-UI [^clojure.lang.Ref reference-value table-name kv-array to-table to-ui]
  (let [;; do local-backup
        [h & r] (name table-name)
        upper-case-table-name (symbol (str (clojure.string/upper-case h) (apply str r)))
        reference-value-back @reference-value
        search-component (eval (to-arrow 'search-UI- (symbol (name table-name))))
        ;; compontn-list (map vec (eval (search-component (ref {}) )))
        compontn-list (map vector (search-component (ref {:date-from (date 1998)
                                                          :date-to (date)}) kv-array to-table to-ui))
        ui-list (vec (map vector (filter #(not (nil? %))
                                         (for [[k v] (map #(let [[k v] %] [(name k) v]) (seq kv-array))]
                                           (cond
                                             (= k "id")                               nil
                                             (= k "rfid")                             (construct-textinput  k v reference-value false)
                                             (= "id" (apply str (take 2 k)))          (construct-dialog     k v reference-value)
                                             (not= 0 (count (re-seq #"date" k)))      (construct-datapicker k v reference-value)
                                             (in? (keys UI-combo-column) (keyword k)) (construct-combobox   k v reference-value)
                                             :else                                    (construct-textinput  k v reference-value))))))
        
         buttons-list [(hrtime.layout-lib/mig [[(hrtime.layout-lib/btn :text (lang :btn_save)
                                                                      :user-data {:mode "s"}
                                                                      :halign :center
                                                                      :icon (image-scale icon/agree-64-png 30)
                                                                      :onClick (fn [x]
                                                                                 (if (not-empty (keys @reference-value))
                                                                                   (if-not (:id @reference-value)
                                                                                     (jdbc/execute! @sql-connection (insert table-name :set @reference-value))
                                                                                     (jdbc/execute! @sql-connection (update table-name :set (dissoc @reference-value :id) :where (= :id (:id @reference-value))))))
                                                                                 (do (dosync (ref-set reference-value {}))
                                                                                     (to-ui (hrtime.database-manager/construct-UI reference-value table-name kv-array to-table to-ui))
                                                                                     (to-table ((eval (to-arrow 'scroll-table<<- upper-case-table-name))
                                                                                                (fn [x] (if-let [ref-value (ref (first (get-sql-by-id table-name (:id x) (eval (to-arrow 'map-> upper-case-table-name)))))]
                                                                                                          (to-ui (hrtime.database-manager/construct-UI ref-value table-name kv-array to-table to-ui))))))))
                                                                      :args [:tip (lang :tip_save)])]
                                              [(hrtime.layout-lib/btn :text (lang :btn_clean_inputs)
                                                                      :user-data {:mode "s"}
                                                                      :halign :center
                                                                      :icon (image-scale icon/ban-blue-64-png 30)
                                                                      :onClick (fn [x]
                                                                                 (dosync (ref-set reference-value {}))
                                                                                 (to-ui (hrtime.database-manager/construct-UI reference-value table-name kv-array to-table to-ui))
                                                                                 (to-table ((eval (to-arrow 'scroll-table<<- upper-case-table-name))
                                                                                            (fn [x] (if-let [ref-value (ref (first (get-sql-by-id table-name (:id x) (eval (to-arrow 'map-> upper-case-table-name)))))]
                                                                                                      (to-ui (hrtime.database-manager/construct-UI ref-value table-name kv-array to-table to-ui)))))))
                                                                      :args [:tip (lang :tip_clean)])]
                                              
                                              [(hrtime.layout-lib/btn :text (lang :btn_remove)
                                                                      :user-data {:mode "s"}
                                                                      :halign :center
                                                                      :icon (image-scale icon/x-blue1-64-png 30)
                                                                      :onClick (fn [x] (if-let [id (:id (deref reference-value))]
                                                                                        ;; (if (hrtime.layout-lib/dlg "Czy napewno chcesz usunąć element?" "Informacja")
                                                                                         (if (hrtime.layout-lib/dlg (lang :delete?) (lang :title_information))
                                                                                           (do (jdbc/execute! @sql-connection (delete table-name :where (= :id id)))
                                                                                               (dosync (ref-set reference-value {}))
                                                                                               (to-ui (hrtime.database-manager/construct-UI reference-value table-name kv-array to-table to-ui))
                                                                                               (to-table ((eval (to-arrow 'scroll-table<<- upper-case-table-name))
                                                                                                          (fn [x] (if-let [ref-value (ref (first (get-sql-by-id table-name (:id x) (eval (to-arrow 'map-> upper-case-table-name)))))]
                                                                                                                    (to-ui (hrtime.database-manager/construct-UI ref-value table-name kv-array to-table to-ui))))))))))
                                                                      :args [:tip (lang :tip_del)])]]
                                             :args [:constraints ["wrap 3" "0px[grow, fill]0px" "0px[grow, fill]0px"]])]
        
        panel-component-list (if (= :registration table-name) compontn-list
                              (concat compontn-list ui-list [buttons-list]))]
    (hrtime.layout-lib/mig panel-component-list :args[:constraints ["wrap 1" "0px[grow, fill]0px" "10px[grow, fill]0px"]])))

(defn search-UI-registration [^clojure.lang.Ref reference-value kv-array to-table to-ui]
  (let [;; do local-backup
        reference-value-back @reference-value
        ;; editable UI lists
        ui-list [(construct-datapicker "date-from"     (lang :date_start)   reference-value)
                 (construct-datapicker "date-to"       (lang :date_end)     reference-value)
                 (construct-textinput  "in-all-column" (lang :search_input) reference-value)]
        buttons-list [(hrtime.layout-lib/mig [[(hrtime.layout-lib/btn :text (lang :btn_clean_filter)
                                                                      :user-data {:mode "s"}
                                                                      :halign :center
                                                                      :icon (image-scale icon/ban-blue-64-png 30)
                                                                      :onClick (fn [x]
                                                                                 (to-table (scroll-table<<-Registration (fn [x]
                                                                                                                          (if-let [ref-value (ref (first (get-sql-by-id :registration (:id x) map->Registration)))]
                                                                                                                            (to-ui (hrtime.database-manager/construct-UI ref-value :registration kv-array to-table to-ui)))))))
                                                                      :args [:tip (lang :tip_clean_filter)])]
                                              [(hrtime.layout-lib/btn :text (lang :btn_search)
                                                                      :user-data {:mode "s"}
                                                                      :halign :center
                                                                      :icon (image-scale icon/loupe-blue1-64-png 30)
                                                                      :onClick (fn [x]
                                                                                 (let [words (string/split (:in-all-column @reference-value) #" ")
                                                                                       sql-rule (case (count words)
                                                                                                  1 [[:where ['or  ['= (first words) :card.card_nr   ] ['= (first words) :user.act]]]] 
                                                                                                  2 [[:where ['and ['= (first words) :user.first_name] ['= (second words) :user.last_name]]]]
                                                                                                  nil)]
                                                                                   (let [data-from (:date-from  @reference-value)
                                                                                         data-to   (:date-to    @reference-value)
                                                                                         sql-rules (concat [[:where ['between :datetime data-from data-to]]] sql-rule)]
                                                                                     (to-table (scroll-table<<-Registration (fn [x]
                                                                                                                              (if-let [ref-value (ref (first (get-sql-by-id :registration (:id x) map->Registration)))]
                                                                                                                                (to-ui (hrtime.database-manager/construct-UI ref-value :registration kv-array to-table to-ui))))
                                                                                                                            :sql-rules sql-rules )))))
                                                                      :args [:tip (lang :tip_search)])]]
                                             :args [:constraints ["wrap 2" "0px[grow, fill]0px" "0px[grow, fill]0px"]])]]
    (concat ui-list buttons-list)))


(defn search-UI-user [^clojure.lang.Ref reference-value kv-array to-table to-ui]
  (let [;; do local-backup
        reference-value-back @reference-value
        ;; editable UI lists
        ui-list [(construct-textinput "in-all-column" (lang :btn_search) reference-value)]
        buttons-list [(hrtime.layout-lib/mig [[(hrtime.layout-lib/btn :text (lang :btn_clean_filter)
                                                                      :user-data {:mode "s"}
                                                                      :halign :center
                                                                      :icon (image-scale icon/ban-blue-64-png 30)
                                                                      :onClick (fn [x]
                                                                                 (to-table (scroll-table<<-User (fn [x]
                                                                                                                  (if-let [ref-value (ref (first (get-sql-by-id :user (:id x) map->User)))]
                                                                                                                    (to-ui (hrtime.database-manager/construct-UI ref-value :user kv-array to-table to-ui)))))))
                                                                      :args [:tip (lang :tip_clean_filter)])]
                                              [(hrtime.layout-lib/btn :text (lang :btn_search)
                                                                      :user-data {:mode "s"}
                                                                      :halign :center
                                                                      :icon (image-scale icon/loupe-blue1-64-png 30)
                                                                      :onClick (fn [x] (let [data-filtrator (if (not (or (nil? (:in-all-column @reference-value)) (= "" (clojure.string/trim (:in-all-column @reference-value)))))
                                                                                                    (filter-col-by (re-pattern (:in-all-column @reference-value))))]
                                                                                         (to-table (scroll-table<<-User (fn [x]
                                                                                                                          (if-let [ref-value (ref (first (get-sql-by-id :user (:id x) map->User)))]
                                                                                                                            (to-ui (hrtime.database-manager/construct-UI ref-value :user kv-array to-table to-ui))))
                                                                                                                        :filter-lambda data-filtrator))))
                                                                      :args [:tip (lang :tip_search)])]] :args [:constraints ["wrap 2" "0px[grow, fill]0px" "0px[grow, fill]0px"]])]]
    (concat ui-list buttons-list)))

(defn search-UI-card [^clojure.lang.Ref reference-value kv-array to-table to-ui]
  (let [;; do local-backup
        reference-value-back @reference-value
        ;; editable UI lists
        ;; ui-list [(construct-textinput "in-all-column" "Wyszukaj" reference-value)]
        ui-list [(construct-textinput "in-all-column" (lang :btn_search) reference-value)]
        buttons-list [(hrtime.layout-lib/mig [[(hrtime.layout-lib/btn :text (lang :btn_clean_filter)
                                                                      :user-data {:mode "s"}
                                                                      :halign :center
                                                                      :icon (image-scale icon/ban-blue-64-png 30)
                                                                      :onClick (fn [x]
                                                                                 (to-table (scroll-table<<-Card (fn [x]
                                                                                                                  (if-let [ref-value (ref (first (get-sql-by-id :card (:id x) map->Card)))]
                                                                                                                    (to-ui (hrtime.database-manager/construct-UI ref-value :card kv-array to-table to-ui)))))))
                                                                      :args [:tip (lang :tip_clean_filter)])]
                                              [(hrtime.layout-lib/btn :text (lang :btn_search)
                                                                      :user-data {:mode "s"}
                                                                      :halign :center
                                                                      :icon (image-scale icon/loupe-blue-64-png 30)
                                                                      :onClick (fn [x] (let [data-filtator (if (not (or (nil? (:in-all-column @reference-value)) (= "" (clojure.string/trim (:in-all-column @reference-value)))))
                                                                                                    (filter-col-by (re-pattern (:in-all-column @reference-value))))]
                                                                                         (to-table (scroll-table<<-Card (fn [x]
                                                                                                                          (if-let [ref-value (ref (first (get-sql-by-id :card (:id x) map->Card)))]
                                                                                                                            (to-ui (hrtime.database-manager/construct-UI ref-value :card kv-array to-table to-ui))
                                                                                                                            ))
                                                                                                                        :filter-lambda data-filtator))))
                                                                      :args [:tip (lang :tip_search)])]]
                                              :args [:constraints ["wrap 2" "0px[grow, fill]0px" "0px[grow, fill]0px"]])]]
    (concat ui-list buttons-list)))

;; (construct-UI (ref #hrtime.database_manager.User{:id nil, :first_name nil, :last_name nil, :work_type nil, :section nil, :teta_nr nil})  :user {:first_name "Imie", :last_name "Nazwisko", :work_type "Typ pracownika", :section "Dział", :teta_nr "Nr w systemie"})

;; (-> (seesaw.core/frame :content (seesaw.core/horizontal-panel :items (search-UI-Card (ref {}) {})))
;;     seesaw.core/pack!
;;     seesaw.core/show!)


(defmacro def-view-component [table-kwd  & {:keys [is-editable?] :or {is-editable? true}}]
  (let [[h & r] (name table-kwd)
        table-name (symbol (str (clojure.string/upper-case h) (apply str r)))
        function-name (symbol (str "view-" (name table-kwd)))
        ;; declare row function
        map->             (to-arrow 'map->            table-name)
        excel<<-          (to-arrow 'excel<<-         table-name)
        record-header<<-  (to-arrow 'record-header<<- table-name)
        scroll-table<<-   (to-arrow 'scroll-table<<-  table-name)]
    `(defn ~function-name [~'to-ui ~'to-table ~'excel-exporter-lambda]
      (let [~'ref-value (ref (~map-> {}))
            ~'struct-hint (~record-header<<-)]
        (~'to-ui (construct-UI ~'ref-value ~table-kwd ~'struct-hint ~'to-table ~'to-ui))
        (~'to-table (~scroll-table<<- (fn [~'x]
                                        (if-let [~'ref-value (ref (first (get-sql-by-id ~table-kwd (:id ~'x) ~map->)))]
                                          (~'to-ui (hrtime.database-manager/construct-UI ~'ref-value ~table-kwd ~'struct-hint ~'to-table ~'to-ui))))))
        (~'excel-exporter-lambda [:mouse-clicked (fn [~'e] (~excel<<-))])))))


;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Declare table view ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;

;; (def f (seesaw.core/frame :content (seesaw.core/label :text "functional area")))
;; (def t (seesaw.core/frame :content (seesaw.core/label :text "table area")))
;; (def e (seesaw.core/frame :content (seesaw.core/label :text "excel area")))
;; (-> f seesaw.core/pack! seesaw.core/show!)
;; (-> t seesaw.core/pack! seesaw.core/show!)
;; (-> e seesaw.core/pack! seesaw.core/show!)
;; (view-registration #(seesaw.core/config! f :content (seesaw.core/scrollable % :hscroll :as-needed :vscroll :as-needed))
;;                    #(seesaw.core/config! t :content %)
;;                    (fn [_] (seesaw.core/config! e :content (hrtime.layout-lib/btn :text "Excel Export"
;;                                                                          :icon (image-scale icon/excel-64-png 30)
;;                                                                          :background "#ddd"
;;                                                                          :halign :center
;;                                                                          :underline "#3bad65"
;;                                                                          :hover-bg-color "#3bad65"
;;                                                                          :hover-fg-color "#fff"
;;                                                                          :onClick (fn [x] (excel<<-Registration))))))
;; (view-card #(seesaw.core/config! f :content (seesaw.core/scrollable % :hscroll :as-needed :vscroll :as-needed))
;;            #(seesaw.core/config! t :content %)
;;            #(identity %))
;; (view-user #(seesaw.core/config! f :content (seesaw.core/scrollable % :hscroll :as-needed :vscroll :as-needed))
;;            #(seesaw.core/config! t :content %)
;;            (fn [_] (seesaw.core/config! e :content (hrtime.layout-lib/btn :text "Excel Export"
;;                                                                          :icon (image-scale icon/excel-64-png 30)
;;                                                                          :background "#ddd"
;;                                                                          :halign :center
;;                                                                          :underline "#3bad65"
;;                                                                          :hover-bg-color "#3bad65"
;;                                                                          :hover-fg-color "#fff"
;;                                                                          :onClick (fn [x] (excel<<-User))))))

;; (set-frame-font "Source Code Pro 10" nil t)
(do
  (def-view-component :user)
  (def-view-component :card)
  (def-view-component :registration))




