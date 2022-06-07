(ns jarman.cli.cli-internal
  (:gen-class)
  (:require
   [clojure.string                  :as string]
   [clojure.tools.cli               :refer [parse-opts]]
   ;; [jarman.config.storage           :as storage]
   [jarman.variables]
   [jarman.managment.data           :as data]
   [jarman.lib.lib-icon-font-generator  :as stool]
   [jarman.lang                     :refer :all]
   [jarman.logic.update-manager     :as update-manager]
   [jarman.config.dot-jarman        :refer [dot-jarman-load]]))

(defn print-helpr
  "Description
    show info about keys"
  [cli-opt-m]
  (println (get cli-opt-m :summary "[!] Helper not implemented")))

;;;;;;;;;;;;;;;;;;;;;;
;;; Update manager ;;;
;;;;;;;;;;;;;;;;;;;;;;

(def pkg-cli-options
  [["-l" "--list" "List all package availabled in repositories "]
   [nil  "--list-repo" "List all remote or local update repositories"]
   [nil  "--update-package" "If system are outdated, then show latest stable to update package"]
   [nil  "--build" "Build new package by the current project code base"]
   ["-d" "--download PKGNAME" "Download package by name, for example 'jarman-0.1.1.zip'"]
   ["-p" "--install PATH" "Install specified zip package"]])
(defn pkg-cli [& args]
  (let [cli-opt (parse-opts args pkg-cli-options)
        opts (get cli-opt :options)
        args (get cli-opt :arguments)
        o1 (first (seq opts))
        k1 (first o1) v1 (second o1)]
    (case k1
      :list            (update-manager/cli-print-list-of-all-packages)
      :list-repo       (update-manager/cli-print-repository-list)
      :update-package  (update-manager/cli-print-update-candidate)
      :build           (update-manager/cli-build-new-package)
      :download        (update-manager/cli-download-package v1)
      :install         (update-manager/cli-install-package-by-path v1)
      (print-helpr cli-opt))))

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

(def structure-cli-options
  [[nil "--refresh-icons" "Regenerate icon assets library"]
   [nil "--refresh-fonts" "Regenerate font assets library"]])
(defn structure-cli [& args]
  (let [cli-opt (parse-opts args structure-cli-options)
        opts (get cli-opt :options)
        args (get cli-opt :arguments)
        o1 (first (seq opts))
        k1 (first o1) v1 (second o1)]
    ;; if-let [es (get cli-opt :errors)]
    ;; (doall (for [e es] (println (format "[!] %s" e))))
    (case k1 
      :refresh-icons (do
                       (stool/refresh-icon-lib)
                       (println(format "[ok] library by path %s was generated" stool/*icon-library*)))
      :refresh-fonts (do
                       (stool/refresh-font-lib)
                       (println(format "[ok] library by path %s was generated" stool/*font-library*)))
      ;; :help (print-helpr structure-cli-options)
      (print-helpr cli-opt))))

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn usage []
  (->> ["jarman CLI tool, which do controlling jarman environment easyest ."
        ""
        "Usage: lets-scheme [action]"
        ""
        "Actions:"
        "  data       Database and data manager. Use for building or debuging project data structure"
        "  pkg        Package manager, which help build or list available packages"
        "  structure  Manager of project structure and resources"
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
          "pkg"       (apply pkg-cli  rest-arguments)
          "structure" (apply structure-cli rest-arguments)
          "help"     (println (usage))
          (println (usage)))))))

;;;;;;;;;;;;;;;;
;;; EXAMPLES ;;;
;;;;;;;;;;;;;;;;

(comment
  (-main "pkg" "--list")
  (-main "pkg" "--list-repo")
  (-main "pkg" "--update-package")
  (-main "pkg" "--build")
  (-main "pkg" "--download" "jarman-0.0.1.zip")
  (-main "pkg" "--install" "./jarman-0.0.1.zip")
  (-main "pkg" "--help")
  ;; ---- 
  (-main "pkg" "-d 1.0.2")
  ;; ----
  (-main "structure" "--refresh-fonts")
  (-main "structure" "--refresh-icons")
  (-main "structure" "--help")
  ;; -----
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
  (-main "data" "--help")
  )


