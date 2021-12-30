(ns jarman.logic.exporter-demo)
(require '[jarman.logic.exporter :as ex])
(require '[jarman.tools.lang :as lang])

(defn- suck-users
  "Get users from database"
  [{:keys [login]}]
  (jarman.logic.connection/query (jarman.logic.sql-tool/select! {:table_name :user
                                                                 :column [:login :first_name :last_name]
                                                                 :where  [:= :login login]})))

(defn- convert-map-list
  "Convert list of maps to correct format using upper fn, covert-map"
  [map-list]
  (doall (vec (concat (map (fn [m] (lang/convert-map-keys-to-str m)) map-list)))))

(defn- prepare-data
  "Define data for mearge with odt"
  [{:keys [props-map]}]
  ;; (println "Props for Export selected user\n" props-map)
  [;; columns
   ["users.login", "users.first_name", "users.last_name"]
   
   ;; data
   {"project" {"Name" "XDocReport"}, 
    "users"   (convert-map-list (suck-users props-map))}])

(comment
  (convert-map-list (suck-users {:login "Admin"}))
 ;; => [{"login" "admin", "first_name" "admin", "last_name" "admin"}]
 )

(jarman.logic.exporter/register-doc-exporter
 :type        :odt
 :name        "Export selected user"
 :description "Export data for pointed users"
 :data-fn     prepare-data
 :export-form-gui [(ex/component :component-type :input :label "User ID" :field :login)]
 :frame-size  [250 150])






(defn- prepare-data-2
  "Define data for mearge with odt
   data-vec should be like [{:a 1 :b 2} {:a 3 :b 4}]"
  [{:keys [props-map data-from-app]}]
  ;; (println props-map data-from-app)
  (let [take-count (if (empty? (:rows-counts props-map))
                     (count (:my-table-data data-from-app))
                     (:rows-counts props-map))]
    (assert (map? data-from-app) (format "Exporter - Data sets should be in map. Inside: `%s`" (first data-from-app)))
    (let [take-count (if (string? take-count) (read-string take-count) take-count)
          take-count (if (int? take-count) take-count (count (:my-table-data data-from-app)))
          take-count (if (= 0 take-count) (count (:my-table-data data-from-app)) take-count)
          my-table-data (take take-count (:my-table-data data-from-app))
          my-table-data (vec (doall (map #(lang/convert-map-keys-to-str %) my-table-data)))]
      [ ;; columns
       ["data.a", "data.b"]
       
       ;; data
       {"project" {"Name" "XDocReport"}, 
        "data"    my-table-data
        :name     (:my-name data-from-app)}])))

(comment
  (prepare-data-2 {:data-from-app {:my-table-data [{:a 1 :b 2} {:a 3 :b 4}] :my-name "Racoon"} :props-map {:rows-counts "1"}})
  )

(jarman.logic.exporter/register-doc-exporter
 :type        :odt
 :name        "Export data from app"
 :description "Export example data from app"
 :data-fn     prepare-data-2
 :export-form-gui [(ex/component :component-type :input :label "Rows count" :field :rows-counts)]
 :frame-size  [250 150])
