H(ns jarman.jarman-cli
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
   [jarman.managment.data :as data]
   ;; [jarman.cli.cli-tool :as cli]
   ;; developer tools 
   [jarman.tools.swing :as stool]   
   [jarman.tools.lang :refer :all]))

(defn print-helpr
  "Description
    show info about keys"
  [cli-opt-m]
  (println (get cli-opt-m :summary "[!] Helper not implemented")))

;;;;;;;;;;;;;;;;;;;;;;;;
;;; Database manager ;;; 
;;;;;;;;;;;;;;;;;;;;;;;;
(def data-cli-options
  [["-i" "--info" "Println all statistics about file, version of metadata, tables, etc"]
   [nil "--install-schemas" "Create all server infrastructure from `.jarman.data.clj`"]
   [nil "--delete-schemes" "Delete schemas `.jarman.data.clj` by declarted metadata"]
   [nil "--update-metadata" "Push meta from file to database `.jarman.data.clj`"]
   ["-h" "--help"]])
(defn data-cli [& args]
  (let [cli-opt (parse-opts args data-cli-options)
        opts (get cli-opt :options)
        args (get cli-opt :arguments)
        o1 (first (seq opts))
        k1 (first o1) v1 (second o1)]
    (if-let [es (get cli-opt :errors)]
      (doall (for [e es] (println (format "[!] %s" e))))
      (cond
        (= k1 :info)              (data/on-info)
        (= k1 :install-schemas)   (data/on-install)
        (= k1 :delete-schemas)    (data/on-delete)
        (= k1 :update-schemas)    (data/on-update-meta)
        :else (print-helpr cli-opt)))))

;; (defn data-cli [& args]
;;   (let [cli-opt (parse-opts args data-cli-options)
;;         opts (get cli-opt :options)
;;         args (get cli-opt :arguments)
;;         o1 (first (seq opts))
;;         k1 (first o1) v1 (second o1)]
;;     (if-let [es (get cli-opt :errors)]
;;       (doall (for [e es] (println (format "[!] %s" e))))
;;       (cond
;;         (= k1 :create-table)      (cli/cli-create-table cli-opt)
;;         (= k1 :create-meta)       (cli/cli-create-table cli-opt)
;;         (= k1 :delete-table)      (cli/cli-delete-table cli-opt)
;;         (= k1 :delete-meta)       (cli/cli-delete-table cli-opt)
;;         (= k1 :swap-tables)       (cli/swap-tables cli-opt)
;;         (= k1 :reset-db)          (cli/reset-db cli-opt)
;;         (= k1 :reset-meta)        (cli/reset-meta cli-opt)
;;         (= k1 :view-table)        (cli/view-all-tables cli-opt)
;;         (= k1 :help)              (print-helpr cli-opt)
;;         (= k1 :list-tables-file)  (cli/print-list-tbls-file cli-opt)
;;         (= k1 :list-tables-jarman)(cli/print-list-tbls-jarman cli-opt)
;;         (= k1 :print-table)       (cli/print-table cli-opt)
;;         (= k1 :valid-tables)      (dbmang/valid-tables)
;;         ;;  (= k1 :dummy-data)   (println "")
;;         :else (print-helpr cli-opt)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Project structure manager ;;; 
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
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
        (= k1 :help)(print-helpr structure-cli-options)
        :else (print-helpr cli-opt)))))

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
  (-main "data" "--info")
  (-main "data" "--install-schemas")
  (-main "data" "--delete-schemes")
  (-main "data" "--update-metadata")
  (-main "data" "-h"))

