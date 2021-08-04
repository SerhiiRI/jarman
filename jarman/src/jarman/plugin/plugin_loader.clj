(ns jarman.plugin.plugin-loader
  (:require [clojure.java.io :as io] 
            [clojure.string]
            [jarman.config.environment :as env]
            [jarman.tools.lang :refer :all])
  (:import (java.io IOException FileNotFoundException)))


(def ^:private jarman-plugins-dir-list [(io/file env/user-home ".jarman.d" "plugins")
                                        (io/file ".jarman.d" "plugins")])

(defn load-files [dir]
  (doseq [f (file-seq dir)
          :when (.isFile f)]
    (load-file (.getAbsolutePath f))))

(defn do-load-plugins []
  (if-let [file (first (filter #(.exists %) jarman-plugins-dir-list))]
    (load-files file)
    (throw (FileNotFoundException.
            (format "No one plugin directory [%s] doesn't exists"
                    (clojure.string/join
                     ", " (map str jarman-plugins-dir-list)))))) true)

(do-load-plugins)
