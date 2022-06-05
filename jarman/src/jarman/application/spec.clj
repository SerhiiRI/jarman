(ns jarman.application.spec
  (:require
   [clojure.spec.alpha :as s]
   [jarman.config.environment :as env]
   [jarman.lang :refer :all :as lang]))

(s/def :jarman.application.spec/keyword    keyword?)
(s/def :jarman.application.spec/keyword-list (s/and sequential? #(every? keyword? %)))
(s/def :jarman.application.spec/permission :jarman.application.spec/keyword)
(s/def :jarman.application.spec/name       string?)

