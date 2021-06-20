(ns jarman.cli.jarman-cli
  (:gen-class)
  (:refer-clojure :exclude [update])
  (:require
   ;; resource 
   [clojure.tools.cli :refer [parse-opts]]
   [clojure.string :as string]
   [clojure.java.jdbc :as jdbc]
   ;; logic
   [jarman.tools.ftp-toolbox :as ftp]
   [jarman.config.storage :as storage]
   [jarman.logic.sql-tool :as toolbox :include-macros true :refer :all]
   [jarman.managment.db-managment :as dbmang]
   [jarman.cli.cli-tool :as cli]
   ;; developer tools 
   [jarman.tools.swing :as stool]   
   [jarman.tools.lang :refer :all]))


;; ;;;;;;;;;;;;;;;;;;;;;;;;;
;; ;;; Database manager  ;;; 
;; ;;;;;;;;;;;;;;;;;;;;;;;;;
(def data-cli-options
  [[nil "--create-by-ssql SCHEME" "Create table from scheme (db_ssql.clj), use <all> for all. Automatic generate meta for structure(--create-meta)"
    :parse-fn #(str %)]
   [nil "--create-meta TABLE" "Create meta to table metadata by name, use <all> for all"
    :parse-fn #(str %)]
   [nil "--create-by-meta TABLE" "Create table by name from metadata (db_meta.clj), use <all> for all"
    :parse-fn #(str %)]   
   ["-d" "--delete-table TABLE" "Delete table by name, use <all> for all"
    :parse-fn #(str %)
    :validate [#(or (= % "all")(dbmang/table-in? %)) "Table not found"]]
   [nil "--delete-meta TABLE" "Delete table in metadata, use <all> for all"
    :parse-fn #(str %)
    :validate [#(or (= % "all")(dbmang/table-in? %)) "Table not found"]]
   [nil "--reset-db TABLE" "Delete scheme, create new tables and generate metadata"
    :parse-fn #(str %)]
   [nil "--reset-meta TABLE" "Delete and create new meta information about table"
    :parse-fn #(str %)]
   [nil "--view-scheme SCHEME"
    :parse-fn #(str %)]
   ["-p" "--path PATH" "Add path to file db, use this key with --create-... alse use this key for view"
    :parse-fn #(str %)
    :validate [#(dbmang/file-exists? %) "File not found"]]
   [nil "--list-tables" "List of available tables in jarman db"
    :parse-fn #(str %)]
   [nil  "--list-schemas" "List of available schemas in file db"
    :parse-fn #(str %)]
   [nil "--print-table TABLE" "Print table"
    :parse-fn #(str %)
    :validate [#(dbmang/table-in? %) "Table not found"]]
   [nil "--valid-tables" "Validate table's struture"
    :parse-fn #(str %)]
   [nil "--csv-like" "combine with --print key"]
   ["-h" "--help"
    :parse-fn #(str %)]])

(defn data-cli [& args]
  (let [cli-opt (parse-opts args data-cli-options)
        opts (get cli-opt :options)
        args (get cli-opt :arguments)
        o1 (first (seq opts))
        k1 (first o1) v1 (second o1)]
    (if-let [es (get cli-opt :errors)]
      (doall (for [e es] (println (format "[!] %s" e))))
      (cond
        (= k1 :create-by-ssql)  (cli/cli-create-table cli-opt)
        (= k1 :create-by-meta)  (cli/cli-create-table cli-opt)
        (= k1 :create-meta)     (cli/cli-create-table cli-opt)
        (= k1 :delete-table)    (cli/cli-delete-table cli-opt)
        (= k1 :delete-meta)     (cli/cli-delete-table cli-opt)
        (= k1 :reset-db)        (cli/reset-db cli-opt)
        (= k1 :reset-meta)      (cli/reset-meta cli-opt)
        (= k1 :view-scheme)     (cli/view-scheme cli-opt)
        (= k1 :help)            (cli/print-helpr cli-opt)
        (= k1 :list-tables)     (cli/print-list-tbls cli-opt)
        (= k1 :list-schemas)    (cli/print-list-schm cli-opt)
        (= k1 :print-table)     (cli/print-table cli-opt)
        (= k1 :valid-tables)    (cli/valid-all-tables cli-opt)
        ;;  (= k1 :dummy-data)   (println "")
        :else (cli/print-helpr cli-opt)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Project structure manger ;;; 
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def structure-cli-options
  [[nil "--refresh-icons" "Regenerate icon assets library"]
   [nil "--refresh-fonts" "Regenerate font assets library"]
   ["-h" "--help"]])
(defn structure-cli [& args]
  (let [cli-opt (parse-opts args structure-cli-options)
        opts (get cli-opt :options)
        args (get cli-opt :arguments)
        o1 (first (seq opts))
        k1 (first o1) v1 (second o1)]
    (if-let [es (get cli-opt :errors)]
      (doall (for [e es] (println (format "[!] %s" e))))
      (cond 
        (= k1 :refresh-icons)(do (stool/refresh-icon-lib)(println(format "[ok] library by path %s was generated" stool/*icon-library*)))
        (= k1 :refresh-fonts)(do (stool/refresh-font-lib)(println(format "[ok] library by path %s was generated" stool/*font-library*)))
        (= k1 :help)(cli/print-helpr structure-cli-options)
        :else (cli/print-helpr cli-opt)))))

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn usage []
  (->> ["lets-scheme - jarman CLI tool, which do controlling jarman environment easyest ."
        ""
        "Usage: lets-scheme [action]"
        ""
        "Actions:"
        "  data       Database and data manager. Use for building or debuging project data structure"
        "  config     -- not yet implemented --"
        "  structure  Manager of project structure and resources"
        "  ftp        Ftp tools "
        "  help       Documentation"
        ""
        "Please refer to the manual page for more information."]
       (string/join \newline)))

(defn -main [& args]
  (if (empty? args)
    (println (usage))
    (let [[action & rest-arguments] args]
      (if action
        (case action
          "data" (apply data-cli rest-arguments)
          "structure" (apply structure-cli rest-arguments)
          ;;          "config"   (exit 0 "[!] config action not yet implemented ")
          ;;          "ftp"      (apply ftp-cli rest-arguments)
          "help"     (println (usage))
          (println (usage)))))))

;;;;;;;;;;;;;;;;
;;; EXAMPLES ;;;
;;;;;;;;;;;;;;;;

(comment
 (-main "data" 
        "--create-by-ssql" "all"
        "--path" "e:\\repo\\jarman-test\\jarman\\jarman\\src\\jarman\\managment\\db_ssql.clj")
 (-main "data" 
        "--create-meta" "all"
        "--path" "e:\\repo\\jarman-test\\jarman\\jarman\\src\\jarman\\managment\\db_ssql.clj")
 (-main "data" 
        "--delete-table" "user")
 (-main "data" 
        "--delete-table" "all")
 (-main "data" 
        "--delete-meta" "user")
 (-main "data" 
        "--reset-db" "all")
 (-main "data" "--list-tables"
        "--path" "e:\\repo\\jarman-test\\jarman\\jarman\\src\\jarman\\managment\\db_meta.clj")
 (-main "data" "--print-table" "user" "--csv-like")
 (-main "data" "--print-table" "user" )
 (-main "data" "--valid-tables" "--path" "e:\\repo\\jarman-test\\jarman\\jarman\\src\\jarman\\managment\\db_ssql.clj")
 (-main "data" "--view-scheme" "user"  "--path" "e:\\repo\\jarman-test\\jarman\\jarman\\src\\jarman\\managment\\db_ssql.clj"))


