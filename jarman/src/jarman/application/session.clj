;;  ____  _____ ____ ____ ___ ___  _   _ 
;; / ___|| ____/ ___/ ___|_ _/ _ \| \ | |
;; \___ \|  _| \___ \___ \| | | | |  \| |
;;  ___) | |___ ___) |__) | | |_| | |\  |
;; |____/|_____|____/____/___\___/|_| \_|
;; --------------------------------------
;; All this file generate one function that
;; call `session` and it obtain all information
;; about the licenses, additional instance params,
;; user object.
;;
;;           .---> #<`License`>
;;          /       {:tenant "A",
;;         /         :tenant-id "A",
;;        /          :creation-date "10-01-2021",
;;       /           :expiration-date "10-11-2023",
;;  (get-license)    :limitation {:computer-count 10}}
;;     / 
;;    /                        .->  #<`SessionParams`>
;;   '          .-(get-params)/      {:m {"firm-name" "Ekka Service"}}
;;   |         /
;; (session)--'        
;;   |
;;    \
;;  (get-user)
;;       \ 
;;        '----> #<`User`>
;;                 { .----- from `user` table
;;                  :id 1,
;;                  :login "admin",
;;                  :first-name "admin",
;;                  :last-name "admin",
;;                  :user-configuration {:ftp ...},        .--- Permission groups, for info go
;;                   .----- from `profile` table          /     to `jarman.application.permissions`
;;                  :profile-name "admin",               V
;;                  :profile-configuration {:groups [:admin-update ...]}}
;; 
(ns jarman.application.session
  (:require
   [clojure.string :as string]
   [clojure.java.jdbc :as jdbc]
   [clojure.spec.alpha :as s]
   [clojure.pprint :refer [cl-format]]
   [clojure.java.io :as io]
   [jarman.logic.connection :as c]
   [jarman.logic.sql-tool :refer [select! insert! update! delete!]]
   [jarman.logic.security :refer [encrypt-local decrypt-local]]
   [jarman.lang           :refer :all]
   [jarman.tools.org      :refer :all])
  (:import [java.util Base64 Date]))


(declare login)
(declare session)
;;;;;;;;;;;;;;;;;;;;;;;;;
;;; PERMISSION SYSTEM ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;

(defprotocol IPermissionActor
  (allow-permission? [this group])
  (allow-groups      [this]))

(defprotocol IUserInfo
  (info [this])
  (config [this]))

(defprotocol ISessionGetter
  (get-user    [this])
  (get-license [this])
  (get-params  [this]))

(defrecord License [tenant tenant-id creation-date expiration-date limitation])
(defrecord SessionParams [m])
(defrecord User [id login first-name last-name user-configuration profile-name profile-configuration]
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
(deftype Session [^User user ^License license ^SessionParams params]
  ISessionGetter
  (get-user    [this] user)
  (get-license [this] license)
  (get-params  [this] params)
  IPermissionActor
  (allow-permission? [this group] (.allow-permission? (.get-user this) group))
  (allow-groups      [this] (.allow-groups (.get-user this))))

;;;;;;;;;;;;;;;;;
;;; LICENSING ;;;
;;;;;;;;;;;;;;;;;

;; crypting licenses
(defn decrypt-license [s]
  (when (some? s)
    (try
      (read-string (decrypt-local s))
      (catch Exception e
        (print-error e)
        (throw (ex-info "Error when decrypt license, please check key-pair"
                 {:type :error-parsing-license
                  :message-head [:header :licenses]
                  :message-body [:alerts :error-parsing-license]}))))))

(defn encrypt-license [m]
  (encrypt-local (pr-str m)))

(defn load-license []
  (->> {:table_name :system_props
        :column     [:value]
        :where      [:and [:= :name "license"]]}
    select! c/query first :value))

(defn spit-license-file [m]
  (let [{:keys [tenant-id]} m
        creation-date (first (split #" " (quick-timestamp)))
        path (format "licenses/LICENSE_%s_%s" tenant-id creation-date)
        m (cond-> m
            (string? (get m :limitation)) (update :limitation read-string))]
    (spit path (encrypt-license m))
    (symbol (.getAbsolutePath (io/file path)))))

(defn- select-license-keys [m]
  (select-keys m '(:tenant :tenant-id :creation-date :expiration-date :limitation)))

;; (spit-license-file
;;  (->>
;;   (list "EKKA" "EKKA-2" "10-09-2018" "10-09-2020" "{:computer-count 10}")
;;   (apply ->License)
;;   (into {})))
;; "/home/serhii/programs/jarman/jarman/Makefile"
;; (slurp-license-file "/home/serhii/programs/jarman/jarman/licenses/LICENSE_EKKA_2021-10-30")

(defn slurp-license-file [license-file-path]
  (decrypt-license (slurp license-file-path)))
(defn set-license [^License m]
  (if m
    (let [m (select-license-keys m)
          m (cond-> m
              (string? (get m :limitation)) (update :limitation read-string))]
      (if-let [existing-profile (load-license)]
        (c/exec (update! {:table_name :system_props :set {:name "license" :value (encrypt-license m)} :where [:= :name "license"]}))
        (c/exec (insert! {:table_name :system_props :column-list [:name :value] :values ["license" (encrypt-license m)]}))))
    (print-line "Not selected license to update")))
(defn delete-license []
  (c/exec (delete! {:table_name :system_props :where [:= :name "license"]})))
(defn gui-slurp-and-set-license-file [license-file-path]
  (set-license (slurp-license-file license-file-path)))

;;; org-mode interaction to file 
(defn rename-keys [m]
  (clojure.set/rename-keys
   m {:tenant "tenant", :tenant-id "id tenant", :creation-date "creation date",
      :expiration-date "expiration date", :limitation "limitation"}))

(defn license-to-map [col]
 (->> ;; (list "EKKA" "EKKA-2" "10-09-2018" "10-09-2020" "{:computer-count 10}")
  col
  (apply ->License)
  (into {})))

(defn group-by-tenant-id [licenses-m]
  (->> licenses-m
   (group-by :tenant-id)
   (map (fn [[k v]] (vector k (first v))))
   (into {})))

(defn list-licenses-files []
  (let [files (->> (file-seq (io/file "licenses/"))
                   (filter #(.isFile %)))]
    (conj 
     (for [f files
           :let [l (slurp-license-file (.getAbsolutePath f))
                 fabsl (.getAbsolutePath f)
                 fname (.getName f)]]
       (conj (vec (vals (select-keys l '(:tenant :tenant-id :creation-date :expiration-date)))) (format "[[file:%s][%s]]" fabsl fname)))
     (conj (mapv name '(:tenant :tenant-id :creation-date :expiration-date)) "file-path"))))

;;;;;;;;;;;;;;;;;
;;; MANAGMENT ;;;
;;;;;;;;;;;;;;;;;


(defn build-user [login password]
  (where
   ((m (-> {:table_name :user
            :column [:#as_is :user.id
                     :user.login :user.last_name :user.first_name
                     :user.configuration :profile.name :profile.configuration]
            :inner-join [:user->profile]
            :where [:and [:= :login login] [:= :password password]]}
           select! c/query first
           (update-existing-in [:user.configuration]    read-string)
           (update-existing-in [:profile.configuration] read-string))))
   (if m
     (User. (:user.id m) (:user.login m)
            (:user.first_name m) (:user.last_name m)
            (:user.configuration m) (:profile.name m)
            (:profile.configuration m))
     (throw (ex-info "Cannot build `User` object in session. Select on user-table return nil"
                     {:type :incorrect-login-or-password
                      :message-head [:header :user]
                      :message-body [:alerts :incorrect-login-or-pass]
                      :attr {:connection (c/connection-get) :login login :password password}})))))

(defn build-license []
  (try (where
        ((m (-> (load-license) (decrypt-license))))
        (if m
          (License. (:tenant m) (:tenant-id m) (:creation-date m) (:expiration-date m) (:limitation m))))
       (catch Exception e
         (throw (ex-info "Decryption or loading license error. Maybe license hash was corrupted, or license key was changed"
                         {:type :error-parsing-license
                          :message-head [:header :licenses]
                          :message-body [:alerts :error-parsing-license]})))))

(defn build-session-param []
  (->> {:table_name :system_props
        :column [:name :value]
        :where [:<> :name "license"]}
       select! c/query (mapv (comp vec vals)) (into {}) (SessionParams.)))

(defn build-session [m]
  (if (and m (every? map? [(:user m) (:params m)]))
    (Session. (:user m) (:license m) (:params m))
    (throw (ex-info "session map is empty, this error you shuldn't see"
                    {:type :session-map-is-empty
                     :message-head [:header :user]
                     :message-body [:alerts :undefinied-login-error]}))))

(defn session [] nil)
(defn login [connection login password]
  (if-not (c/connection-validate connection)
    (ex-info (format "Connetion map validation problem %s " (str connection))
             {:type :not-valid-connection
              :message-head [:header :database]
              :message-body [:alerts :configuration-incorrect]
              :attr {:connection connection :login login :password password}}))
  (if-not (c/test-connection connection)
    (ex-info (format "Problem with testing connection %s" (str connection))
             {:type :no-connection-to-database
              :message-head [:header :database]
              :message-body [:alerts :connection-problem]
              :attr {:connection connection :login login :password password}}))
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

(defn login-test []
  (let [builded-session
    (build-session
      {:user
       (let [m {:user.id 2,
                :user.login "dev",
                :user.last_name "dev",
                :user.first_name "dev",
                :user.configuration
                {:ftp {:login "jarman",
                       :password "dupa",
                       :host "trashpanda-team.ddns.net"}},
                :profile.name "developer",
                :profile.configuration
                {:groups [:admin-update :admin-extension
                          :admin-dataedit :developer
                          :ekka-all]}}]
         (User. (:user.id m) (:user.login m)
           (:user.first_name m) (:user.last_name m)
           (:user.configuration m) (:profile.name m)
           (:profile.configuration m)))
       :license
       (let [m {:tenant "EKKA",
                :tenant-id "EKKA-2",
                :creation-date "10-01-2021",
                :expiration-date "30-11-2021",
                :limitation {:computer-count 10}}]
         (License. (:tenant m) (:tenant-id m) (:creation-date m) (:expiration-date m) (:limitation m)))
       :params
       (SessionParams. {"tennat" "ekka", "contact-person" "Julia Burmich"})})]
   (defn session []
     builded-session)))

(comment
  ;; TMP USER
  (login-test)
  ;; TEST LOGIN 
  (try
    (.get-user (login {:dbtype "mysql", :host "trashpanda-team.ddns.net", :port 3307, :dbname "jarman", :user "root", :password "misiePysie69", :useUnicode true, :characterEncoding "UTF-8"}
                      "admin" "admin"))
    (catch clojure.lang.ExceptionInfo e
      (print-error (.getMessage e))
      (ex-data e)))
  (.get-user (session)))

(defmacro when-permission
  "Description
    Eval internal content if user in session have
    `:developer` permission group. Otherwise return
    nil

  Example 
   (when-permission :developer
     (c/label :text tenant-id))

  See
   `if-permission`"
  [permission-group & body]
  `(when (.allow-permission? (jarman.application.session/session) ~permission-group)
     ~@body))

(defmacro if-permission
  "Description
    if user contain current permission
    eval `if-true` block, otherwise `if-false`

  Example 
   (if-permission :developer
     (c/label :text tenant-id)
     (c/label :text \" - \"))

  See
   `when-permission`"
  [permission-group if-true if-false]
  `(if (.allow-permission? (jarman.application.session/session) ~permission-group)
     ~if-true
     ~if-false))

