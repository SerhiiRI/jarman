(ns jarman.cli.cli-external
  (:gen-class)
  (:require
   ;; clojure
   [clojure.tools.cli :refer [parse-opts]]
   [clojure.string :as string]
   [clojure.java.jdbc :as jdbc]
   ;; logic
   [jarman.variables]
   [jarman.config.storage :as storage]
   [jarman.managment.data :as data]
   ;; developer tools 
   [jarman.lang :refer :all]
   [jarman.config.dot-jarman :refer [dot-jarman-load]]))

(defn print-helpr
  "Description
    show info about keys"
  [cli-opt-m]
  (println (get cli-opt-m :summary "[!] Helper not implemented")))

;;;;;;;;;;;;;;;;;;;;;;;;
;;; Database manager ;;; 
;;;;;;;;;;;;;;;;;;;;;;;;

;;; WARNING!
;;; This code segment are replicated in cli_external

(def data-cli-options
  [[nil  "--install" "Delete all database structure, setup system tables, verifyng it, loading metadata into the table"]
   [nil  "--delete" "Delete all database buisness schmas"]
   ["-b" "--backup" "Run backup process"]
   [nil  "--app-start" "Preparing action when client starting up"]
   [nil  "--app-close" "Action when clien switching off"]
   [nil  "--crash" "Action what shuld be run after some data crash"]
   ["-l" "--log" "Return database log"]
   [nil  "--clear" "Clear all metadata"]
   ["-i" "--info" "Diagnostics information"]
   ;; ------------------------------------
   [nil  "--meta-clean" "Clean metadata table in database"]
   [nil  "--meta-persist" "Persist metadata to database from .jarman.data"]
   [nil  "--meta-refresh" "Clean and Persist metadatas from .jarman.data into database"]
   [nil  "--view-clean" "Clean view table in database"]
   [nil  "--view-persist" "Persist views to database from .jarman.data"]
   [nil  "--view-refresh" "Clean and Persist views from .jarman.data into database"]])
(defn data-cli [& args]
  (let [cli-opt (parse-opts args data-cli-options)
        opts (get cli-opt :options)
        args (get cli-opt :arguments)
        o1 (first (seq opts))
        k1 (first o1) v1 (second o1)]
    ;; if-let [es (get cli-opt :errors)]
    ;; (doall (for [e es] (println (format "[!] %s" e))))
    (case k1
      :install      (data/on-install)
      :delete       (data/on-delete)
      :backup       (data/on-backup)
      :app-start    (data/on-app-start)
      :app-close    (data/on-app-close)
      :crash        (data/on-crash)
      :log          (data/on-log)
      :clear        (data/on-clear)
      :info         (data/on-info)
      :meta-clean   (data/on-meta-clean)
      :meta-persist (data/on-meta-persist)
      :meta-refresh (data/on-meta-refresh)
      :view-clean   (data/on-view-clean)
      :view-persist (data/on-view-persist)
      :view-refresh (data/on-view-refresh)
      (print-helpr cli-opt))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Project structure manager ;;; 
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn usage []
  (->> ["Jarman client"
        ""
        "Usage: jarman [action] [--option]"
        ""
        "Actions:"
        "  data       Database and data manager. Use for building or debuging project data structure"
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
          "data"      (apply data-cli rest-arguments)
          "help"     (println (usage))
          (println (usage)))))))

;;;;;;;;;;;;;;;;
;;; EXAMPLES ;;;
;;;;;;;;;;;;;;;;

(comment
  (-main "data" "--install")
  (-main "data" "--delete")
  (-main "data" "--backup")
  (-main "data" "--app-start")
  (-main "data" "--app-close")
  (-main "data" "--crash")
  (-main "data" "--log")
  (-main "data" "--clear")
  (-main "data" "--info")
  (-main "data" "--meta-clean")
  (-main "data" "--meta-persist")
  (-main "data" "--meta-refresh")
  (-main "data" "--view-clean")
  (-main "data" "--view-persist")
  (-main "data" "--view-refresh")
  (-main "data" "--help"))


