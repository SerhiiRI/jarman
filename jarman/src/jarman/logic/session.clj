(ns jarman.logic.session
  (:require
   [clojure.string :as string]
   [clojure.java.jdbc :as jdbc]
   [clojure.spec.alpha :as s]
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

(defn test-user [m]
  (s/valid? ::user m))

(def ^{:private true} user (atom nil))
(defn user-set [m] (if (and (map? m) (test-user m)) (do (reset! user m) m) nil))
(defn user-get [] @user)

(defn get-user-permission 
  [] (if-not (nil? @user) (:permission.permission_name @user) "user"))

(defn get-user-login
  [] (if-not (nil? @user) (:user.login @user) "user"))

(defn get-user-configuration
  [] (if-not (nil? @user) (:user.configuration @user) {}))

(defn get-permission-configuration
  [] (if-not (nil? @user) (:permission.configuration @user) {}))

(defn set-user-permission 
  [permission] (swap! user (fn [storage] (assoc-in storage [:permission.permission_name] permission))))

(defn allow-permission?
  [coll] (in? coll (keyword (get-user-permission))))



;; (jarman.logic.system-login/login-user "user" "user")
;; (user-get)

 
