(ns jarman.plugin.spec
  (:require
   [clojure.spec.alpha :as s]
   [jarman.config.environment :as env]
   [jarman.tools.lang :refer :all :as lang]))

(s/def :common-plugin/vector vector?) 
(s/def :common-plugin/keyword keyword?)
(s/def :common-plugin/string string?)

;; spec for permission
(s/def :global-plugin/permission (s/and :common-plugin/vector (s/coll-of :common-plugin/keyword)))
;;(s/valid? :global-plugin/permission [:user :admin]) ;;=> true

(s/def :global-plugin/name :common-plugin/string)
(s/def :global-plugin/plug-place :common-plugin/vector)

(s/def :global-plugin/block-table
  (s/keys :req-un [:global-plugin/permission
                   :global-plugin/name
                   :global-plugin/plug-place]
          ;; :opt-un [:]
          ))

(defn test-keys-jtable [conf spec-map]
  (s/def :global-plugin/block-table
    (eval spec-map))
  (let [resault (s/valid? :global-plugin/block-table conf)]
    (if resault
      resault
      (do (s/explain :global-plugin/block-table conf)
          resault))))

;; TODO: Add spec for model, etc