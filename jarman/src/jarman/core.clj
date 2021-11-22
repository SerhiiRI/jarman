(ns jarman.core
  (:gen-class)
  (:require
   [jarman.cli.cli-external]
   [jarman.gui.gui-login]))

(defn -main [& args]
  (if (empty? args)
    (jarman.gui.gui-login/-main)
    (apply jarman.cli.cli-external/-main args)))
