(ns demo.export-script)

(defn demo-path [path] (str "./templates/demo/" path))

(defn suck-users
  "Get users from database"
  []
  (jarman.logic.connection/query (jarman.logic.sql-tool/select! {:table_name :user :column [:login :first_name :last_name]})))

(defn convert-map
  "Convert keys in map to strings"
  [basic-map]
  (doall (into {} (map (fn [[k v]] {(name k) v}) basic-map))))

(defn convert-map-list
  "Convert list of maps to correct format using upper fn, covert-map"
  [map-list]
  (doall (vec (concat (map (fn [m] (convert-map m)) map-list)))))

(defn prepare-data
  "Define data for mearge with odt"
  []
  [;; columns
   ["users.login", "users.first_name", "users.last_name"]
   
   ;; data
   {"project" {"Name" "XDocReport"}, 
    "users"   (convert-map-list (suck-users))}])

(defn export
  "Run export"
  []
  (do (apply kaleidocs.merge/merge-doc
             (clojure.java.io/file (demo-path "template.odt"))
             (clojure.java.io/file (demo-path "template_ev.odt"))
             (prepare-data))))

(jarman.logic.state/associn-state :export [:demo] prepare-data)

(comment
  (export)
  )
