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
  (let [u (->>
           (c/query
            (select! {:table_name :user
                      :column [{:user.id :id}
                               :login
                               :last_name
                               :first_name
                               :permission_name
                               :configuration]
                      :inner-join [:permission]
                      :where [:and
                              [:= :login user-login]
                              [:= :password user-password]]}))
         (map #(update-in % [:configuration] read-string))
         first)]
    (if (nil? u)
      :user-not-found
      (session/user-set u))))

;; (login-user "user" "user")
;; (session/user-get)

(defn login [connection]
  (if (c/connection-validate connection)
    (if (c/test-connection connection)
      (do (c/connection-set connection)
          login-user)
      :no-connection-to-database)
    :not-valid-connection))
