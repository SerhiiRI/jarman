(ns jarman.logic.metadata-composite-types)

(defrecord Link     [text link])
(defn isLink?       [^jarman.logic.metadata_composite_types.Link e] (instance? jarman.logic.metadata_composite_types.Link e))
(defn link          [text link] (->Link text link))

(defrecord File     [file-name file])
(defn isFile?       [^jarman.logic.metadata_composite_types.File e] (instance? jarman.logic.metadata_composite_types.File e))
(defn file          [file-name file] {:pre [(every? string? (list file-name file))]} (->File file-name file))

(defrecord FtpFile  [file-name file-path])
(defn isFtpFile?    [^jarman.logic.metadata_composite_types.FtpFile e] (instance? jarman.logic.metadata_composite_types.FtpFile e))
(defn ftp-file      [file-name file-path] {:pre [(every? string? (list file-name file-path))]} (->FtpFile file-name file-path))

(def component-list  [isFile? isLink? isFtpFile?])
(def component-files [isFile? isFtpFile?])
(defn is-composite-component? [val] (some #(% val) component-list))
(defn is-componente-Files? [val] (some #(% val) component-files))
