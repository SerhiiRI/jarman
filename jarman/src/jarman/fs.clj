(ns jarman.fs
  (:use me.raynes.fs)
  (:require [jarman.config-manager :as cm]))

(defn is-edn?
  "test if file have .edn extention"
  [path]
  (let [f (if-not (string? path) path (io/file path))]
    (if (and (.isFile f) (.exists f))
      (= (extension path) ".edn"))))

(defn config-copy
  "Copy a file from 'from' to 'to'. Return 'to'."
  [from to]
  (when-not (exists? from)
    (throw (IllegalArgumentException. (str from " not found"))))
  (if (and (is-edn? from) (= (.getName (file from)) (.getName (file to))))
    (if-let [cfg (cm/merge-configs from to)]
      (spit to (prn-str cfg))
      (io/copy (file from) (file to)))
    (io/copy (file from) (file to)))  to)

(defn create-dir [dir]
  (mkdirs dir))

(defn config-copy+
  "Copy src to dest, create directories if needed."
  [src dest](mkdirs(parent dest))(config-copy src dest))

(defn config-copy-dir
  "Copy a directory from `from` to `to`. If `to` already exists,
  recursively do `config-copy` from `from` to `to`"
  [from to]
  (when (exists? from)
    (if (file? to)
      (throw (IllegalArgumentException. (str to " is a file")))
      (let [from (file from)
            to to
            trim-size (-> from str count inc)
            dest #(file to (subs (str %) trim-size))]
        (mkdirs to)
        (dorun
         (walk (fn [root dirs files]
                 (doseq [dir dirs]
                   (when-not (directory? dir)
                     (-> root (file dir) dest mkdirs)))
                 (doseq [f files]
                   (config-copy+ (file root f) (dest (file root f)))))
               from))
        to))))

(defn copy-dir-replace
  "Copy a directory from `from` to `to`. If `to` already exists,
  recursively do `config-copy` from `from` to `to`"
  [from to]
  (when (exists? from)
    (if (file? to)
      (throw (IllegalArgumentException. (str to " is a file")))
      (let [from (file from)
            to to
            trim-size (-> from str count inc)
            dest #(file to (subs (str %) trim-size))]
        (mkdirs to)
        (dorun
         (walk (fn [root dirs files]
                 (doseq [dir dirs]
                   (when-not (directory? dir)
                     (-> root (file dir) dest mkdirs)))
                 (doseq [f files]
                   (copy+ (file root f) (dest (file root f)))))
               from))
        to))))
