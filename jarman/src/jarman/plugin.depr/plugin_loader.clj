(ns jarman.plugin.plugin_loader
  (:require [clojure.java.io :as io] 
            [clojure.spec.alpha :as s]
            [jarman.config.environment :as env]
            [jarman.tools.lang :refer :all]))

;;; TEST LOADING EXTERNAL RESOURCE
(defn load-files [dir]
  (doseq [f (file-seq dir)
          :when (.isFile f)]
    (load-file (.getAbsolutePath f))))

(defn do-load-plugins []
  (load-files (io/file ".jarman.d" "plugins")))

(do-load-plugins)


