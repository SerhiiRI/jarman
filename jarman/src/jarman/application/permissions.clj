;;  ____  _____ ____  __  __ ___ ____ ____ ___ ___  _   _ 
;; |  _ \| ____|  _ \|  \/  |_ _/ ___/ ___|_ _/ _ \| \ | |
;; | |_) |  _| | |_) | |\/| || |\___ \___ \| | | | |  \| |
;; |  __/| |___|  _ <| |  | || | ___) |__) | | |_| | |\  |
;; |_|   |_____|_| \_\_|  |_|___|____/____/___\___/|_| \_|
;; -------------------------------------------------------
;; Jarman permission system is quite easy stuff. This namespace
;; currently do not do any logicstic function, is just container
;; where jarman store all permission in the system.
;; 
;; Jaramn use keyword's that call `groups` that just must be
;; added in specyfic path inside `profile` system table:
;;  Database table `profile`:
;; .--------------------...
;; | id | name      | configuration
;; |----+-----------+------------------------------------
;; |  1 | admin     | {:groups [:admin-update :admin-extension :admin-dataedit ... ]}
;; |  2 | developer | {:groups [:developer-manage :developer-alpha ... :ekka-all]}
;; |  3 | user      | {:groups [:ekka-all]}
;; '----------...
;; In configuration column in `:groups` locates permission keys, that allow or
;; discard some action inside the system.
;; 
;; This namespace only just collect this keys, most of mechanics 
;; describes `jarman.application.session` namespace
;;
;; Example of using:
;;  => 
;;   (if-permission :developer
;;     (only-developer-component ...)
;;     (seesaw.core/label "Permission denied!!!"))
;; 
;; If (.allow-permission? (.get-user (session)) :developer)
;; than condition is fulfilled

(ns jarman.application.permissions
  (:require
   [clojure.pprint    :refer [cl-format]]
   [jarman.lang :refer :all]
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

