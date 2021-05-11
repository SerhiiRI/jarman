(ns jarman.logic.session
  (:require
   [clojure.string :as string]
   [clojure.java.jdbc :as jdbc]
   [clojure.spec.alpha :as s]
   [jarman.tools.lang :refer :all]))


(s/def ::ne-string (every-pred string? not-empty))
(s/def ::str-without-space (s/and ::ne-string #(not (string/includes? % " "))))
(s/def :user/id (every-pred number? pos-int?))
(s/def :user/login ::str-without-space)
(s/def :user/configuration map?)

(s/def ::user
  (s/keys :req-un [:user/id
                   :user/login
                   :user/configuration]))

(def ^{:private true} user (atom nil))
(defn user-set [m]
  (if (map? m) (do (swap! user m) m) nil))

(defn user-get [k]
      (cond 
        (keyword? k) (get user k)
        (vector?  k) (get-in user k)))