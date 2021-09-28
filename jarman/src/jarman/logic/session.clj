(ns jarman.logic.session
  (:require
   [clojure.string :as string]
   [clojure.java.jdbc :as jdbc]
   [clojure.spec.alpha :as s]
   [jarman.logic.connection :as c]
   [jarman.logic.sql-tool :refer [select!]]
   [jarman.tools.lang :refer :all]))

(s/def ::ne-string (every-pred string? not-empty))
(s/def ::str-without-space (s/and ::ne-string #(not (string/includes? % " "))))
(s/def :user/user.id number?)
(s/def :user/user.login ::str-without-space)
(s/def :user/user.first_name ::str-without-space)
(s/def :user/user.last_name ::str-without-space)
(s/def :user/user.configuration map?)
(s/def :user/permission.prmission_name ::str-without-space)
(s/def :user/permission.configuration map?)

(s/def ::user
  (s/keys :req-un [:user/user.id
                   :user/user.login
                   :user/user.first_name
                   :user/user.last_name
                   :user/user.configuration
                   :permission/permission.permission_name
                   :permission/permission.configuration]))

(defprotocol CheckData
  (allow-permission? [this group]))

(defprotocol GetData
  (get-permission [this])
  (get-login [this])
  (get-user-configuration [this])
  (get-permission-configuration [this]))

(defprotocol SetData
  (set-permission [this permission]))

(defrecord User [id login first-name last-name
                 user-configuration permission-name
                 permission-configuration]
  CheckData
  (allow-permission? [this group]
    (if-not (nil? permission-configuration)
      (in? (:groups permission-configuration) group)))
  GetData
  (get-permission [this]
    (if-not (nil? permission-name)
      permission-name "user"))
  (get-login [this]
    (if-not (nil? login)
      login "user"))
  (get-user-configuration [this]
    (if-not (nil? user-configuration)
      user-configuration {}))
  (get-permission-configuration [this]
    (if-not (nil? permission-configuration)
      permission-configuration {}))
  SetData
  (set-permission [this permission] (set! (. this permission-name) permission)))

(defn test-user [m]
  (s/valid? ::user m))

(def ^{:private true} user (atom (User. nil nil nil nil nil nil nil)))

(defn user-set
  [m]
  (if (and (map? m) (test-user m))
    (do (reset! user (->User (:user.id m)
                             (:user.login m)
                             (:user.last_name m)
                             (:user.first_name m)
                             (:user.configuration m)
                             (:permission.permission_name m)
                             (:permission.configuration m))) m) nil))

(defn user-get [] @user)
(defn get-user-permission [] (.get-permission @user))
(defn get-user-login [] (.get-login @user))
(defn get-user-configuration [] (.get-user-configuration @user))
(defn get-permission-configuration [] (.get-permission-configuration @user))
(defn allow-permission? [coll] (.allow-permission? @user coll))
(defn set-user-permission [permission] (.set-user-permission @user permission))

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
        (user-set)))))

(login-user "dev" "dev")

(comment
  (user-set {:user.id 2 :user.login "admin", :user.last_name "admin", :user.first_name "admin", :user.configuration {:ftp {:login "jarman", :password "dupa" :host "trashpanda-team.ddns.net"}}, :permission.permission_name "admin", :permission.configuration {}})
  (user-get))

(defn login [connection]
  (if (c/connection-validate connection)
    (if (c/test-connection connection)
      (do (c/connection-set connection)
          login-user)
      :no-connection-to-database)
    :not-valid-connection))
