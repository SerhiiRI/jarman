(ns jarman.plugin.spec
  (:require
   [clojure.spec.alpha :as s]
   [jarman.config.environment :as env]
   [jarman.tools.lang :refer :all :as lang]))

(s/def :jarman.plugin.spec/keyword    keyword?)
(s/def :jarman.plugin.spec/keyword-list (s/and sequential? #(every? keyword? %)))
(s/def :jarman.plugin.spec/permission :jarman.plugin.spec/keyword-list)
(s/def :jarman.plugin.spec/plug-place :jarman.plugin.spec/keyword-list)
(s/def :jarman.plugin.spec/name       string?)

