(ns jarman.managment.data
  (:require
   [clojure.java.io :as io]
   [jarman.config.environment :as env])
  (:import (java.io FileNotFoundException)))

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

;;;;;;;;;;;;;;;;;
;;; CONSTANTS ;;;
;;;;;;;;;;;;;;;;;

(def jarman-data ".jarman.data.clj")
(def jarman-data-devl-path "file in `src/jarman/managment` namespace" (io/file "src" "jarman" "managment" jarman-data))
(def jarman-data-curr-path "file in jarman.exe path" (io/file jarman-data))
(def jarman-data-home-path "file in user home" (io/file env/user-home jarman-data))
(def jarman-data-all "all load places" [jarman-data-devl-path jarman-data-home-path jarman-data-curr-path])

(defn jarman-data-load []
  (if-let [file (first (filter #(.exists %) jarman-data-all))]
    (binding [*ns* (find-ns 'jarman.managment.data)] 
      (load-file (str file)))
    (throw (FileNotFoundException. "Not found '.jarman.data.clj' file."))))

(jarman-data-load)
