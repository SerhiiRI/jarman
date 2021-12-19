(ns jarman.logic.exporter-demo)
(require '[jarman.logic.exporter :as ex])

(defn- suck-users
  "Get users from database"
  [{:keys [login]}]
  (jarman.logic.connection/query (jarman.logic.sql-tool/select! {:table_name :user
                                                                 :column [:login :first_name :last_name]
                                                                 :where  [:= :login login]})))

(defn- convert-map
  "Convert keys in map to strings"
  [basic-map]
  (doall (into {} (map (fn [[k v]] {(name k) v}) basic-map))))

(defn- convert-map-list
  "Convert list of maps to correct format using upper fn, covert-map"
  [map-list]
  (doall (vec (concat (map (fn [m] (convert-map m)) map-list)))))

(defn- prepare-data
  "Define data for mearge with odt"
  [props-map]
  [;; columns
   ["users.login", "users.first_name", "users.last_name"]
   
   ;; data
   {"project" {"Name" "XDocReport"}, 
    "users"   (convert-map-list (suck-users props-map))}])

(jarman.logic.exporter/register-doc-exporter
 :type        :odt
 :name        "Export selected user"
 :description "Export data for pointed users"
 :data-fn     prepare-data
 :export-form-gui [(ex/component :component-type :input :label "User ID" :field :login)]
 :frame-size  [250 150])
