(ns jarman.managment.data
  (:require
   [clojure.java.io :as io]
   [jarman.config.environment :as env])
;;   (:import (java.io FileNotFoundException))
  )

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

(defn jarman-data-load []
  (if-let [file (env/get-jarman-data)]
    (binding [*ns* (find-ns 'jarman.managment.data)] 
      (load-file (str file)))
    ;; (throw (FileNotFoundException. "Not found '.jarman.data' file."))
    ))

(jarman-data-load)

