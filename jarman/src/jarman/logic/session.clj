(ns jarman.logic.session
  (:require
   [clojure.string :as string]
   [clojure.java.jdbc :as jdbc]
   [clojure.spec.alpha :as s]
   [jarman.tools.lang :refer :all]))


(s/def ::ne-string (every-pred string? not-empty))
(s/def ::str-without-space (s/and ::ne-string #(not (string/includes? % " "))))
(s/def :user/id number?)
(s/def :user/login ::str-without-space)
(s/def :user/configuration map?)

(s/def ::user
  (s/keys :req-un [:user/id
                   :user/login
                   :user/configuration]))

(defn test-user [m]
  (s/valid? ::user m))



(def ^{:private true} user (atom nil))
(defn user-set [m] (if (and (map? m) (test-user m)) (do (reset! user m) m) nil))
(defn user-get [] @user)

(defn user-get-permission 
  [] (if-not (nil? @user) (get @user :permission_name) "user"))

(defn user-get-login
  [] (if-not (nil? @user) (get @user :login) "user"))

(defn user-set-permission 
  [permission] (swap! user (fn [storage] (assoc-in storage [:permission_name] permission))))
