(ns jarman.logic.exporter-demo)
(require '[jarman.logic.exporter :as ex])
(require '[jarman.tools.lang :as lang])

;; Read me
;; This scripts are inside DB next to template in .odt

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; First exporter demo
;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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
 :type        :odt                            ;; Template type/extension like .odt
 :name        "Export selected user"          ;; Here should be name of template file without extension like .odt
 :description "Export data for pointed users" ;; Some info
 :data-fn     prepare-data                    ;; Entry point from app to script
 :export-form-gui [(ex/component :component-type :input :label "User name" :field :login)] ;; Components collection for exporter form, extra info
 :frame-size  [250 150])                      ;; Popup size



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; Second exporter demo
;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- entry-point
  "Define data for mearge with odt
   {:data-from-app {:my-table-data [{:a 1 :b 2} {:a 3 :b 4}] :my-name \"Racoon\"} :props-map {:rows-counts \"1\"}}"
  [{:keys [props-map data-from-app]}]
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
  (entry-point {:data-from-app {:my-table-data [{:a 1 :b 2} {:a 3 :b 4}] :my-name "Racoon"} :props-map {:rows-counts "1"}})
  )

(jarman.logic.exporter/register-doc-exporter
 :type        :odt                            ;; Template type/extension like .odt
 :name        "Export data from app"          ;; Here should be name of template file without extension like .odt
 :description "Export example data from app"  ;; Some info
 :data-fn     entry-point                     ;; Entry point from app to script
 :export-form-gui [(ex/component :component-type :input :label "Rows count" :field :rows-counts)] ;; Components collection for exporter form, extra info
 :frame-size  [250 150])                      ;; Popup size
