(ns jarman.plugin.spec
  (:require
   [clojure.spec.alpha :as s]
   [jarman.config.environment :as env]
   [jarman.tools.lang :refer :all :as lang]))

(s/def :jarman.plugin.spec/keyword-list (s/and sequential? #(every? keyword? %)))
(s/def :jarman.plugin.spec/permission :jarman.plugin.spec/keyword-list)
(s/def :jarman.plugin.spec/plug-place :jarman.plugin.spec/keyword-list)
(s/def :jarman.plugin.spec/name       string?)

;;; DEPRECATED --  @julia must look
;; (defn test-keys-jtable [conf spec-map]
;;   (s/def :global-plugin/block-table
;;     (eval spec-map))
;;   (let [resault (s/valid? :global-plugin/block-table conf)]
;;     (if resault
;;       resault
;;       (do (s/explain :global-plugin/block-table conf)
;;           resault))))

