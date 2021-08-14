(ns jarman.logic.system-login
  (:refer-clojure :exclude [update])
  (:require
   [clojure.string :as string]
   [clojure.java.jdbc :as jdbc]
   [jarman.tools.lang :refer :all]
   [jarman.config.storage :as storage]
   [jarman.config.environment :as env]
   [jarman.logic.sql-tool :refer [select!]]
   [jarman.logic.connection :as c]
   [jarman.logic.session :as session])
  (:import (java.util Date)
           (java.text SimpleDateFormat)))

;; Login to system 
;; How to use it?
;;
;; (if-let [login-fn (login
;;                    ;; {:dbtype "mysql",
;;                    ;;  :host "trashpanda-team.ddns.net",
;;                    ;;  :port 3306,
;;                    ;;  :dbname "jarman",
;;                    ;;  :user "jarman",
;;                    ;;  :password "dupa"}
;;                    (c/connection-get)
;;                    )]
;;   ;; function login retunr function to
;;   ;; make login to user
;;   (if (fn? login-fn)
;;     (if-let [u (login-fn "user" "user")]
;;       u
;;       "USER NOT FOUND")
;;     (case login-fn
;;       :no-connection-to-database
;;       "NOT CONNECTION TO BD"
;;       :not-valid-connection
;;       "NOT VALID CONNECTION")))


(defn login-user [user-login user-password]
  (let [u (->
           (c/query
            (select! {:table_name :user
                      :column [:#as_is
                               :user.id
                               :user.login
                               :user.last_name
                               :user.first_name
                               :user.configuration
                               :permission.permission_name
                               :permission.configuration]
                      :inner-join [:user->permission]
                      :where [:and
                              [:= :login user-login]
                              [:= :password user-password]]}))
           first)]
    (if (empty? u)
      :user-not-found
      (-> u
        (update-in [:user.configuration] read-string)
        (update-in [:permission.configuration] read-string)
        (session/user-set)))))


;; ({:user.login "admin", :user.last_name "admin", :user.first_name "admin", :user.configuration "{:ftp {:login \"jarman\", :password \"dupa\" :host \"trashpanda-team.ddns.net\"}}", :permission.permission_name "admin", :permission.configuration "{}"} {:user.login "user", :user.last_name "user", :user.first_name "user", :user.configuration "{:ftp {:login \"jarman\", :password \"dupa\" :host \"trashpanda-team.ddns.net\"}}", :permission.permission_name "user", :permission.configuration "{}"}
;;  {:user.login "dev", :user.last_name "dev", :user.first_name "dev", :user.configuration "{:ftp {:login \"jarman\", :password \"dupa\" :host \"trashpanda-team.ddns.net\"}}", :permission.permission_name "developer", :permission.configuration "{}"})

(comment
  (login-user "user" "user")
  (session/user-get))

(defn login [connection]
  (if (c/connection-validate connection)
    (if (c/test-connection connection)
      (do (c/connection-set connection)
          login-user)
      :no-connection-to-database)
    :not-valid-connection))
