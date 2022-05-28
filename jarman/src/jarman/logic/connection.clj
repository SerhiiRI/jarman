(ns jarman.logic.connection
  (:require
   ;; clojure
   [clojure.string :as string]
   [clojure.java.jdbc :as jdbc]
   [clojure.spec.alpha :as s]
   ;; jarmans
   [jarman.tools.lang :refer :all]
   [jarman.config.vars :refer [defvar]])
  (:import (java.util Date)
           (java.text SimpleDateFormat)
           (java.sql SQLException)))

(defvar dataconnection-saved nil)
(defvar dataconnection-alist {}
  :name "Datasources"
  :doc "Connection map list"
  :type clojure.lang.PersistentArrayMap
  :group :database)

;;; HELPER FUNCION ;;;
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
                                                            (:user db-spec) (:password db-spec))]
        (jdbc/query connector (jdbc/prepare-statement connector "SHOW DATABASES" {:timeout 4})))
      (catch com.mysql.jdbc.exceptions.jdbc4.CommunicationsException _ nil) 
      (catch java.net.ConnectException _ nil)
      (catch Exception _ nil))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Connection map validators ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(s/def ::ne-string (every-pred string? not-empty))
(s/def ::str-without-space (s/and ::ne-string #(not (string/includes? % " "))))
(s/def :connection/dbtype #(in? ["mysql"] %))
(s/def :connection/host ::ne-string)
(s/def :connection/port (every-pred number? pos-int?)) 
(s/def :connection/dbname ::ne-string)
(s/def :connection/user ::ne-string)
(s/def :connection/password ::ne-string)
(s/def ::connection
  (s/keys :req-un [:connection/dbtype
                   :connection/host
                   :connection/port
                   :connection/dbname
                   :connection/user
                   :connection/password]))

(def ^:dynamic *connection* (ref nil))

(defn connection-validate
  "Description
    valid sended configuraion map, which in this version must look like
     {:dbtype \"mysql\",
      :host \"127.0.0.1\",
      :port 3306,
      :dbname \"jarman\",
      :user \"root\",
      :password \"1234\"}"
  [connection-map]
  (if (s/valid? ::connection connection-map)
    true
    (do (println "Not valid map configuration: ")
        (clojure.pprint/pprint connection-map))))

(defn connection-set
  "Description
    set configuraiton reference variable, but do validation on
    sended configuraion map, which in this version must look like
     {:dbtype \"mysql\",
      :host \"127.0.0.1\",
      :port 3306,
      :dbname \"jarman\",
      :user \"root\",
      :password \"1234\"}
    If validation return `true`, then function also return true
    If validation return `false`, then connection variable not
    totaly set"
  [connection-map]
  (dosync (ref-set *connection* (merge connection-map {:useUnicode true :characterEncoding "UTF-8"}))))

(comment (connection-set (get (deref dataconnection-alist) :jarman--localhost--3306)))

(defn connection-wrapp
  "Description
    Wrapp connection like 
     {:dbtype \"mysql\",
      :host \"127.0.0.1\",
      :port 3306,
      :dbname \"jarman\",
      :user \"root\",
      :password \"1234\"}
    into additional parameters"
  [connection-map]
  (merge connection-map {:useUnicode true :characterEncoding "UTF-8"}))

(defn connection-get
  "Description
    Simply getter for `connection` reference value,
    Which contain connection to database for JDBC
    driver. In this version must be look like
     {:dbtype \"mysql\",
      :host \"127.0.0.1\",
      :port 3306,
      :dbname \"jarman\",
      :user \"root\",
      :password \"1234\"}"
  [] (deref *connection*))

(defn do-connect-to-database []
  (assert (not-empty (deref dataconnection-alist)) "Error. Variable `jarman.logic.connection/dataconnection-alist` cannot be empty")
  (assert (some? (deref dataconnection-saved)) "Error. Unsetted `jarman.logic.connection/dataconnection-saved` var")
  (-> (connection-set
       (get (deref dataconnection-alist)
            (deref dataconnection-saved)))
      (test-connection))
  true)

;;;;;;;;;;;;;;;;;;;;;
;;; JDBC WRAPPERS ;;; 
;;;;;;;;;;;;;;;;;;;;;
(defmacro sqlerr
  ([f]
   `(sqlerr ~f
            #'identity))
  ([f f-exception]
   `(sqlerr ~f
            ~f-exception
            ~f-exception))
  ([f f-io-exception f-exception]
   `(try ~f
         (catch SQLException e#
           (~f-io-exception (format "SQLException was raised" (ex-message e#))))
         (catch Exception e#
           (~f-exception (format "Undefinied problem: %s" (ex-message e#)))))))

;;; simple query depend on `*connection*`
(defn exec [s]
  (jdbc/execute! @*connection* s))
(defn query [s]
  (jdbc/query @*connection* s))
(defn exec-ret! [s]
  (:generated_key (jdbc/execute! @*connection* s {:return-keys true})))
(defn exec! [s]
  (sqlerr (jdbc/execute! @*connection* s)))
(defn query! [s]
  (sqlerr (jdbc/query @*connection* s)))
(defn exec-ret! [s]
  (sqlerr (jdbc/execute! @*connection* s)))

