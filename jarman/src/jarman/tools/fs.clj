(ns jarman.tools.fs
  (:require [me.raynes.fs :as rafs]
            [clojure.java.io :as io]
            [jarman.tools.config-manager :as cm]))

(defn copy [uri file]
  (with-open
    [in (io/input-stream uri)
     out (io/output-stream file)]
    (io/copy in out)))

(defn is-edn?
  "test if file have .edn extention"
  [path]
  (let [f (if-not (string? path) path (clojure.java.io/file path))]
    (if (and (.isFile f) (.exists f))
      (= (rafs/extension path) ".edn"))))

(defn create-dir [dir]
  (rafs/mkdirs dir))

(defn unzip [file dir]
  (let [saveDir (java.io.File. dir)]
    (with-open [stream (java.util.zip.ZipInputStream. (io/input-stream file))]
      (loop [entry (.getNextEntry stream)]
        (if entry
          (let [savePath (str dir java.io.File/separatorChar (.getName entry))
                saveFile (java.io.File. savePath)]
            (if (.isDirectory entry)
              (if-not (.exists saveFile)
                (.mkdirs saveFile))
              (let [parentDir (java.io.File. (.substring savePath 0 (.lastIndexOf savePath (int java.io.File/separatorChar))))]
                (if-not (.exists parentDir) (.mkdirs parentDir))
                (io/copy stream saveFile)))
            (recur (.getNextEntry stream))))))))


(defn zip [directory out-file]
 (with-open [zip (java.util.zip.ZipOutputStream. (io/output-stream out-file))]
   (doseq [f (file-seq directory) :when (.isFile f)]
     (.putNextEntry zip (java.util.zip.ZipEntry. (.getPath f)))
     (io/copy f zip)
     (.closeEntry zip))))

#_(defn is-edn?
  "test if file have .edn extention"
  [path]
  (let [f (if-not (string? path) path (clojure.java.io/file path))]
    (if (and (.isFile f) (.exists f))
      (= (rafs/extension path) ".edn"))))

(defn copy-dir-replace
  "Copy a directory from 'from' to 'to'. If 'to' already exists, copy the directory
   to a directory with the same name as 'from' within the 'to' directory."
  [from to]
  (when (rafs/exists? from)
    (if (rafs/file? to)
      (throw (IllegalArgumentException. (str to " is a file")))
      (let [from (rafs/file from)
            to to
            trim-size (-> from str count inc)
            dest #(rafs/file to (subs (str %) trim-size))]
        (rafs/mkdirs to)
        (dorun
         (rafs/walk (fn [root dirs files]
                 (doseq [dir dirs]
                   (when-not (rafs/directory? dir)
                     (-> root (rafs/file dir) dest rafs/mkdirs)))
                 (doseq [f files]
                   (rafs/copy+ (rafs/file root f) (dest (rafs/file root f)))))
               from))
        to))))

