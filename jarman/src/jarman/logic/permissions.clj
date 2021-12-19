(ns jarman.logic.permissions
  (:require
   [clojure.pprint    :refer [cl-format]]
   [jarman.tools.lang :refer :all]
   [jarman.tools.org  :refer :all]))

(defn- print-permission [permission-m]
  (print-multiline
   (->> permission-m (keys) (partition-all 4)
       (cl-format nil "~{~{~A~^, ~}~^ ~%~}")))
  permission-m)

(let [permission-groups-map (atom {})
      system-groups {:admin-update     {:doc "Allow making update for jarman"}
                     :admin-extension  {:doc "Allow installing different type of buisness extenssion on computer"}
                     :admin-dataedit   {:doc "Allow editing datastructure of buiseness logic and edit metadata."}
                     :managment        {:doc "For example you have panel to managing licenses" :private true}
                     :developer        {:doc "Uncategorized Development shit"}
                     :developer-alpha  {:doc "Permission for developer only. You can see unreleased developer feature"}
                     :developer-manage {:doc "Stable feature, that must be viewed for developer only"}}]
  (defn permission-groups-get []
    (deref permission-groups-map))
  (defn permission-groups-get-list []
    (keys (permission-groups-get)))
  (defn- permission-groups-set [m]
    (print-line "Permission. setting sysmtems permission groups")
    (reset! permission-groups-map m)
    (print-permission m))
  (defn permission-groups-add [m]
    {:pre [(map? m)]}
    (print-line "Permission. adding new permission to system permission groups")
    (swap! permission-groups-map
           #(reduce
             (fn [acc [k-group v-prop]]
               (if-not (contains? system-groups k-group)
                 (assoc acc k-group v-prop) acc))
             % (seq m)))
    (print-permission m))
  (permission-groups-set system-groups))

