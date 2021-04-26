(ns jarman.logic.connection
  (:refer-clojure :exclude [update])
  (:require
   [clojure.string :as string]
   [clojure.java.jdbc :as jdbc]
   [jarman.config.config-manager :as c])
  (:import (java.util Date)
           (java.text SimpleDateFormat)
           (java.sql SQLException)))


;; (c/get-in-value [:database.edn :datalist])
;; {:localhost {:name "Localhost", :type :block, :display :edit, :value {:dbtype {:name "Typ po³±czenia", :type :param, :display :none, :component :text, :value "mysql"}, :host {:name "Database host", :doc "Enter jarman SQL database server. It may be IP adress, or domain name, where your server in. Not to set port in this input.", :type :param, :display :edit, :component :text, :value "127.0.0.1"}, :port {:name "Port", :doc "Port of MariaDB/MySQL server. In most cases is '3306' or '3307'", :type :param, :display :edit, :component :text, :value "3306"}, :dbname {:name "Database name", :type :param, :display :edit, :component :text, :value "jarman"}, :user {:name "User login", :type :param, :display :edit, :component :text, :value "jarman"}, :password {:name "User password", :type :param, :display :edit, :component :text, :value "dupa"}}}}
(def ^{:dynamic true :private true} connection-conf :prod)
(def ^{:dynamic true :private true} connection-list
  {:locallhost {:dbtype "mysql", :host "127.0.0.1", :port 3306, :dbname "jarman", :user "root", :password "1234"}
   :prod {:dbtype "mysql", :host "trashpanda-team.ddns.net", :port 3306, :dbname "jarman", :user "jarman", :password "dupa"}
   ;; :conf {:dbtype (c/get-in-value [:database.edn :connector :dbtype])
   ;;        :host (c/get-in-value [:database.edn :connector :host])
   ;;        :port (Integer. (c/get-in-value [:database.edn :connector :port]))
   ;;        :dbname (c/get-in-value [:database.edn :connector :dbname])
   ;;        :user (c/get-in-value [:database.edn :connector :user])
   ;;        :password (c/get-in-value [:database.edn :connector :password])}
   })

(defn ^{:dynamic true :private true} connection []
  (connection-conf connection-list))

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

(defn exec [s]
  (jdbc/execute! (connection) s))
(defn query [s]
  (jdbc/query (connection) s))
(defn exec! [s]
  (sqlerr (jdbc/execute! (connection) s)))
(defn query! [s]
  (sqlerr (jdbc/query (connection) s)))

(defn connection-section-config
  [connection-name-alias connection-params-config]
  {:name connection-name-alias
   :type :block,
   :display :none,
   :value connection-params-config})
(defn connection-section-params-config[host port dbname login password]
  {:dbtype
   {:name "Connection type",
    :type :param,
    :display :none,
    :component :text,
    :value "mysql"},
   :host
   {:name "Database host",
    :type :param,
    :display :edit,
    :component :text,
    :value host},
   :port
   {:name "Port",
    :type :param,
    :display :edit,
    :component :text,
    :value port},
   :dbname
   {:name "Database name",
    :type :param,
    :display :edit,
    :component :text,
    :value dbname},
   :user
   {:name "User login",
    :type :param,
    :display :edit,
    :component :text,
    :value login},
   :password
   {:name "User password",
    :type :param,
    :display :edit,
    :component :text,
    :value password}}
)

