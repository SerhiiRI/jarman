(ns jarman.logic.session
  (:require
   [clojure.string :as string]
   [clojure.java.jdbc :as jdbc]
   [clojure.spec.alpha :as s]
   [jarman.logic.connection :as c]
   [jarman.logic.sql-tool :refer [select! insert! update!]]
   [jarman.tools.lang :refer :all]
   [jarman.tools.org  :refer :all])
  (:import [java.util Base64]))

;; (s/def ::ne-string (every-pred string? not-empty))
;; (s/def ::str-without-space (s/and ::ne-string #(not (string/includes? % " "))))
;; (s/def :user/user.id number?)
;; (s/def :user/user.login ::str-without-space)
;; (s/def :user/user.first_name ::str-without-space)
;; (s/def :user/user.last_name ::str-without-space)
;; (s/def :user/user.configuration map?)
;; (s/def :user/profile.name ::str-without-space)
;; (s/def :user/profile.configuration map?)
;; (s/def ::user
;;   (s/keys :req-un [:user/user.id
;;                    :user/user.login
;;                    :user/user.first_name
;;                    :user/user.last_name
;;                    :user/user.configuration
;;                    :permission/profile.name
;;                    :permission/profile.configuration]))
(def group-list [:admin-update :admin-extension :admin-dataedit :developer :ekka-all])
(defprotocol IPermissionActor
  (allow-permission? [this group])
  (allow-groups      [this]))
(defprotocol IUserInfo
  (info [this])
  (config [this]))
(defrecord User
    [id
     login
     first-name
     last-name
     user-configuration
     profile-name
     profile-configuration]
  IPermissionActor
  (allow-permission? [this group]
    (in? (:groups profile-configuration) group))
  (allow-groups [this]
    (get profile-configuration :groups []))
  IUserInfo
  (info [this]
    {:login login :first-name first-name :last-name last-name :profile-name profile-name })
  (config [this]
    user-configuration))

(defn decrypt-license [s]
  (if s 
    (let [decoder (fn decode [to-decode]
                    (String. (.decode (Base64/getDecoder) to-decode)))]
      (try
        (read-string (decoder s))
        (catch Exception e
          (print-error e)
          (ex-info "broken license decription, maybe license hash was changed"
                   {:type :broken-license
                    :translation [:alerts :broken-license-hash]}))))
    (throw (ex-info "not found registered license"
                    {:type :license-not-found
                     :translation [:alerts :license-not-found]}))))

;; (c/exec
;;  (insert!
;;   {:table_name :system_props
;;    :column-list [:name :value]
;;    :values [["contact-person" "Julia Burmich"]]}))
(defrecord License [tenant tenant-id creation-date expiration-date limitation])
(defrecord SessionParams [m])

;; +-1------+    +-2---------+    +-3-----+
;; |SQL User|--->|SQL License|--->|Session|
;; +--------+    +-----------+    +-------+
;;                                  |
;; +-4.1-------------+	    public  |
;; | (defn login []) |<-------------+
;; +-----------------+		    |
;; 				    |
;; +-4.2---------------+    public  |
;; | (defn session []) |<-----------+
;; +-------------------+

(defprotocol ISessionGetter
  (get-user    [this])
  (get-license [this])
  (get-params  [this]))
(deftype Session [^User user ^License license ^SessionParams params]
  ;; (set-permission [this permission] (set! (. this permission-name) permission))
  ISessionGetter
  (get-user    [this] user)
  (get-license [this] license)
  (get-params  [this] params)

  IPermissionActor
  (allow-permission? [this group]
    (.allow-permission? (.get-user this) group))
  (allow-groups [this]
    (.allow-groups (.get-user this))))

(defn build-user [login password]
  (where
   ((m (-> {:table_name :user
            :column [:#as_is :user.id :user.login :user.last_name :user.first_name
                     :user.configuration :profile.name :profile.configuration]
            :inner-join [:user->profile]
            :where [:and [:= :login login] [:= :password password]]}
           select! c/query first
           (update-existing-in [:user.configuration]    read-string)
           (update-existing-in [:profile.configuration] read-string))))
   (if m
     (User. (:user.id m) (:user.login m)
            (:user.first_name m) (:user.last_name m)
            (:user.configuration m)(:profile.name m)
            (:profile.configuration m))
     (throw (ex-info "incorrect login or password "
                     {:type :incorrect-login-or-password
                      :translation [:alerts :incorrect-login-or-pass]})))))

(defn build-license []
  (where
   ((m (-> {:table_name :system_props
            :column [:value]
            :where [:and [:= :name "license"]]}
           select! c/query first :value decrypt-license)))
   (if m
     (License. (:tenant m) (:tenant-id m) (:creation-date m) (:expiration-date m) (:limitation m))
     (throw (ex-info "not found registered license"
                     {:type :license-not-found
                      :translation [:alerts :license-not-found]})))))

(defn build-session-param []
  (->> {:table_name :system_props
        :column [:name :value]
        :where [:<> :name "license"]}
       select! c/query (mapv (comp vec vals)) (into {}) (SessionParams.)))

(defn build-session [m]
  (if (and m (every? map? [(:user m) (:license m) (:params m)]))
    (Session. (:user m) (:license m) (:params m))
    (throw (ex-info "session map is empty, this error you shuldn't see, please contact to tech support"
                    {:type :session-map-is-empty
                     :translation [:alerts :undefinied-login-error]}))))

(defn session [] nil)
(defn login [connection login password]
  (if-not (c/connection-validate connection)
    (ex-info "bad connection settings, please carefully checkout your connections"
             {:type :not-valid-connection
              :translation [:alerts :configuration-incorrect]}))
  (if-not (c/test-connection connection)
    (ex-info "cannot connect to remote database"
             {:type :no-connection-to-database
              :translation [:alerts :connection-problem]}))
  (c/connection-set connection)
  (let [builded-session
        (->  
         {:user    (build-user login password)
          :license (build-license)
          :params  (build-session-param)}
         build-session)]
    (defn session []
      builded-session)
    (session)))

(comment
  (try
    (.get-user (login {:dbtype "mysql", :host "trashpanda-team.ddns.net", :port 3307, :dbname "jarman", :user "root", :password "misiePysie69", :useUnicode true, :characterEncoding "UTF-8"}
                      "dev" "dev"))
    (catch clojure.lang.ExceptionInfo e
      (print-error (.getMessage e))
      (ex-data e))))



