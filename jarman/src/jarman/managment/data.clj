(ns jarman.managment.data
  (:require
   [clojure.java.io :as io]
   [seesaw.core]
   [jarman.tools.org :refer :all]
   [jarman.config.environment :as env])
  (:import
   [java.io FileNotFoundException]))

;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; GLOBAL DATA EVENTS ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;

(declare on-install)
(declare on-delete)
(declare on-backup)
(declare on-app-start)
(declare on-app-close)
(declare on-crash)
(declare on-log)
(declare on-clear)
(declare on-info)
(declare on-meta-clean)
(declare on-meta-persist)
(declare on-meta-refresh)
(declare on-view-clean)
(declare on-view-persist)
(declare on-view-refresh)

;;;;;;;;;;;;;;;;;
;;; CONSTANTS ;;;
;;;;;;;;;;;;;;;;;

(defn do-load-jarman-data []
  (try
    (let [file (env/get-jarman-data)]
      (print-line (format "evaluation of '%s'" (str file)))
      (binding [*ns* (find-ns 'jarman.managment.data)]
        (load-file (str file))
        (print-line ".jarman.data was be loaded")))
    (catch FileNotFoundException e
      (seesaw.core/alert e (.getMessage e))
      ;; (java.lang.System/exit 0)
      )
    (catch Exception e
      (seesaw.core/alert (with-out-str (clojure.stacktrace/print-stack-trace e 20)))
      ;; (java.lang.System/exit 0)
      )))

