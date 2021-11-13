(ns jarman.logic.permissions
  (:require
   [jarman.tools.lang :refer :all]
   [jarman.tools.org  :refer :all]))

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
    (reset! permission-groups-map m ))
  (defn permission-groups-add [m]
    {:pre [(map? m)]}
    (swap! permission-groups-map
           #(reduce
             (fn [acc [k-group v-prop]]
               (if-not (contains? system-groups k-group)
                 (assoc acc k-group v-prop) acc))
             % (seq m))))
  (permission-groups-set system-groups))
(permission-groups-get-list)
