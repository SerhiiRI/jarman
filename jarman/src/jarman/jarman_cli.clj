(ns jarman.jarman-cli
  (:gen-class)
  (:refer-clojure :exclude [update])
  (:require
   [clojure.tools.cli :refer [parse-opts]]
   [clojure.string :as string]
   [clojure.java.jdbc :as jdbc]
   [jarman.tools.ftp-toolbox :as ftp]
   [jarman.config.storage :as storage]
   [jarman.logic.sql-tool :as toolbox :include-macros true :refer :all]
   [jarman.managment.db-managment :refer :all]
   [jarman.tools.lang :refer :all]))

(def data-cli-options
  [["-c" "--create SCHEME" "Create table from scheme, use <all> for all. Automatic generate meta for structure(--create-meta)"
    :parse-fn #(str %)
    :validate [#(or (= % "all")(scheme-in? %)) "Scheme not found"]]
   ["-d" "--delete TABLE" "Delete table by name, use <all> for all"
    :parse-fn #(str %)
    :validate [#(or (= % "all")(table-in? %)) "Table not found"]]
   [nil "--create-meta TABLE" "Delete table by name, use <all> for all"
    :parse-fn #(str %)
    :validate [#(or (= % "all")(table-in? %)) "Table not found"]]
   [nil "--delete-meta TABLE" "Delete table by name, use <all> for all"
    :parse-fn #(str %)
    :validate [#(or (= % "all")(table-in? %)) "Table not found"]]
   [nil "--reset-db TABLE" "Delete scheme, create new tables and generate metadata"
    :parse-fn #(str %)]
   [nil "--reset-meta TABLE" "Delete and create meta information about table"
    :parse-fn #(str %)
   ;; :validate [#(or (= % "all") (table-in? %)) "Table not found"]
    ]
   [nil "--view-scheme SCHEME"
    :parse-fn #(str %)
    :validate [#(scheme-in? %) "Scheme not found"]]
  
   ;; [nil "--dummy-data TABLE" "Generate dummy data for table, use <all> for all"
   ;;  :parse-fn #(str %)
   ;;  :validate [#(or (= % "all") (table-in? %)) "Table not found"]]
   ;; [nil "--dummy-size SIZE" "Dummy data size"]

   ;; [nil  "--list-schemas" "List available table schemas"]
   ;; ["-l" "--list-tables" "List available table"]
   ;; ["-p" "--print TABLE" "Print table"
   ;;  :parse-fn #(str %)
   ;;  :validate [#(table-in? %) "Table not found"]]
   ;; [nil "--csv-like" "combine with --print key"]
   ;; ["-h" "--help"]
   ])

;; Quick debug
;; (-main "-p" "user" "--csv-like")
;; (-main "-d")
;; (-main "--list-schemas")
;; (-main "--list-tables")
;; (-main "--dummy-data")
;; (-main "--view-scheme" "user")
;; (-main "-h")
;; (-main "-d" "*")
;; (-main "-d" "user")
;; (-main "-c" "*")
;; (-main "-c" "METADATA")
;; (-main "-d" "METADATA")

(defn data-cli [& args]
  (let [cli-opt (parse-opts args data-cli-options)
        opts (get cli-opt :options)
        args (get cli-opt :arguments)
        o1 (first (seq opts))
        k1 (first o1) v1 (second o1)]
    (if-let [es (get cli-opt :errors)]
      (doall (for [e es] (println (format "[!] %s" e))))
      (cond
        (= k1 :create)       (cli-create-table cli-opt)
        (= k1 :delete)       (cli-delete-table cli-opt)
        (= k1 :create-meta)  (cli-create-table cli-opt)
        (= k1 :delete-meta)  (cli-delete-table cli-opt)
        (= k1 :reset-db)     (reset-db cli-opt)
        (= k1 :reset-meta)   (reset-meta cli-opt)
        (= k1 :view-scheme)  (cli-scheme-view  cli-opt)
;;        (= k1 :print)        (print-table cli-opt)
       ;; (= k1 :help)         (print-helpr cli-opt)
       ;; (= k1 :list-schemas) (print-lschm cli-opt)
       ;; (= k1 :list-tables)  (print-ltbls cli-opt)


      
      ;;  (= k1 :dummy-data)   (println "[!] Excuse me, functionality not implemented")
       ;; :else (print-helpr cli-opt)
        ))))



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
;;          "structure"(apply structure-cli rest-arguments)
;;          "config"   (exit 0 "[!] config action not yet implemented ")
;;          "ftp"      (apply ftp-cli rest-arguments)
;;          "help"     (println (usage))
          (println (usage)))))))



(scheme-in? "documents")

(-main "data" "--reset-meta" "user")

(table-in? "user")

