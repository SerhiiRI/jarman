;;  __  __ _____ _____  _    ____    _  _____  _    
;; |  \/  | ____|_   _|/ \  |  _ \  / \|_   _|/ \   
;; | |\/| |  _|   | | / _ \ | | | |/ _ \ | | / _ \  
;; | |  | | |___  | |/ ___ \| |_| / ___ \| |/ ___ \ 
;; |_|  |_|_____| |_/_/_  \_\____/_/_ _\_\_/_/__ \_\
;; |  \/  |  / \  | \ | |  / \  / ___| ____|  _ \   
;; | |\/| | / _ \ |  \| | / _ \| |  _|  _| | |_) |  
;; | |  | |/ ___ \| |\  |/ ___ \ |_| | |___|  _ <   
;; |_|  |_/_/   \_\_| \_/_/   \_\____|_____|_| \_\
;; =================================================

(ns jarman.logic.metadata
  (:require
   [jarman.tools.lang :refer :all]
   [potemkin.namespaces :refer [import-vars]]
   [jarman.logic.metadata-core]
   [jarman.logic.metadata-sql-converter]
   [jarman.logic.metadata-composite-types]))

(import-vars
 [jarman.logic.metadata-core
  return-metadata
  database-showtables-nonmeta
  database-delete-metadata-by-table-name
  database-delete-all-metadata
  database-update-metadata-table
  ->TableMetadata]
 
 [jarman.logic.metadata-composite-types
  ->Link map->Link isLink?
  ->File map->File isFile?
  ->FtpFile map->FtpFile isFtpFile?
  is-composite-component?
  is-componente-Files?]

 [jarman.logic.metadata-sql-converter
  create-table-by-meta
  build-metadata-for-table])

(defn do-create-meta-for-existing-tables []
  (doall
   (for [table (database-showtables-nonmeta)]
     (database-update-metadata-table
      (build-metadata-for-table
       table)))))

(defn debug-regenerate-new-metadata []
  (let [table-groups (group-by-apply :table_name (do-create-meta-for-existing-tables)
                                     :apply-group first)
        table-sequence ["documents" "profile" "user" "enterprise" "point_of_sale" "cache_register" "point_of_sale_group" "point_of_sale_group_links" "seal" "repair_reasons" "repair_technical_issue" "repair_nature_of_problem" "repair_contract" "service_contract" "service_contract_month"]
        m-vec (mapv (fn [t] (get table-groups t t)) table-sequence)]
    (spit "./src/jarman/managment/updated-meta.edn" (with-out-str (clojure.pprint/pprint m-vec)))))

(comment
  (do-create-meta-for-existing-tables)
  (debug-regenerate-new-metadata))

