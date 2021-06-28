(ns jarman.gui.gui-db-visualizer)

(defn metadata-get [table]
  (first (jarman.logic.metadata/getset! table)))

(defn metadata-set [metadata]
  (jarman.logic.metadata/update-meta metadata))
