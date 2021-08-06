(ns jarman.plugin.plugin-loader
  (:require [clojure.java.io :as io] 
            [clojure.string]
            [jarman.config.environment :as env]
            [jarman.tools.lang :refer :all])
  (:import (java.io IOException FileNotFoundException)))


(def ^:private jarman-plugins-dir-list
  "List of all plugins directory in client filesystem"
  [(io/file env/user-home ".jarman.d" "plugins")
   (io/file ".jarman.d" "plugins")])

(defn do-load-plugins
  "Description
    Sequentually walk throught the `plugins` directory in `.jarman.d`
    and evaluate 'Central Plugin File'. 

  What is?
    'Central Plugin File' - is file which name equal with directory name
    For example \"some_plugin/some_plugin.clj\", where in directory
    plugin \"some_plugin\" we compile \"some_plugin.clj\" file. That
    file is entry-point, or main file to compilation all plugin structure"
  []
  (->> (first (filter #(.exists %) jarman-plugins-dir-list))
       .listFiles 
       (filter #(.isDirectory %))
       (map (fn [d] 
              (let [plugin-intro (format "%s.clj"(.getName d))]
                (if (.exists (io/file d plugin-intro))
                  (load-file (str (io/file d plugin-intro)))
                  (println "File is not exist")))))
       (doall)) true)

(do-load-plugins)

