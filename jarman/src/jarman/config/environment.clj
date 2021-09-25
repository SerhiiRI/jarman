(ns jarman.config.environment
  (:gen-class)
  (:import (java.io IOException FileNotFoundException))
  (:require [clojure.string :as s]
            [clojure.java.io :as io]
            [clojure.pprint :refer [cl-format]]))

;;; PROPERTIES ;;;
(def java-version (System/getProperty "java.version"))
(def java-vendor  (System/getProperty "java.vendor"))
(def java-vendor-url (System/getProperty "java.vendor.url"))
(def java-home (System/getProperty "java.home"))
(def java-spec-vm-version (System/getProperty "java.vm.specification.version"))
(def java-spec-vm-vendor (System/getProperty "java.vm.specification.vendor"))
(def java-spec-vm-name (System/getProperty "java.vm.specification.name"))
(def java-vm-version (System/getProperty "java.vm.version"))
(def java-vm-vendor (System/getProperty "java.vm.vendor"))
(def java-vm-name (System/getProperty "java.vm.name"))
(def java-specification-version (System/getProperty "java.specification.version"))
(def java-specification-vendor (System/getProperty "java.specification.vendor"))
(def java-specification-name (System/getProperty "java.specification.name"))
(def java-class-version (System/getProperty "java.class.version"))
(def java-class-path (System/getProperty "java.class.path"))
(def java-library-path (System/getProperty "java.library.path"))
(def java-io-tmpdir (System/getProperty "java.io.tmpdir"))
(def java-compiler (System/getProperty "java.compiler"))
(def java-ext-dirs (System/getProperty "java.ext.dirs"))
(def os-name (System/getProperty "os.name"))
(def os-arch (System/getProperty "os.arch"))
(def os-version (System/getProperty "os.version"))
(def file-separator (System/getProperty "file.separator"))
(def path-separator (System/getProperty "path.separator"))
(def line-separator (System/getProperty "line.separator"))
(def user-name (System/getProperty "user.name"))
(def user-home (System/getProperty "user.home"))
(def user-dir (System/getProperty "user.dir"))


;;; JARMAN 
(def jarman-home (-> (clojure.java.io/file ".") .getAbsoluteFile .getParentFile .getAbsolutePath))

;;; ENV VARIABLES ;;;
(def path (System/getenv "path"))
(def plugin-folder-name "plugins")
(def config-folder-name "config")
(def resource-folder ".jarman.d")
(def dot-jarman      ".jarman")
(def dot-jarman-data ".jarman.data")

;;; TODO MACRO
(def jarman-plugins-dir-list
  "List of all plugins directory in client filesystem"
  [(io/file user-home resource-folder plugin-folder-name)
   (io/file       "." resource-folder plugin-folder-name)])

(def jarman-configs-dir-list
  "list of all configururations directory in client filesystem"
  [(io/file user-home resource-folder config-folder-name)
   (io/file       "." resource-folder config-folder-name)])

(def jarman-resource-dir-list
  "list of all configururations directory in client filesystem"
  [(io/file       "." resource-folder)
   (io/file user-home resource-folder)])

(def dot-jarman-paths-list
  "list of all `.jarman` file paths in system"
  [(io/file user-home dot-jarman)
   (io/file       "." dot-jarman)])

(def dot-jarman-data-paths-list
  "list of all `.jarman.data` file paths in system"
  [(io/file user-home dot-jarman-data)
   (io/file       "." dot-jarman-data)
   (io/file       "." "src" "jarman" "managment" dot-jarman-data)])

(def jarman-executable
  (io/file "." "Jarman.exe"))

;;; GET FIRS EXSISTING PATH 
(defn first-exist [jarman-file-list]
  (if-let [file (first (filter #(.exists %) jarman-file-list))] file
    (throw (FileNotFoundException.
            (cl-format nil "No one file hasn't been found ~{`~A`~^, ~}"
                       (map str jarman-file-list))))))

(defn get-plugin-dir []        (first-exist jarman-plugins-dir-list))
(defn get-config-dir []        (first-exist jarman-configs-dir-list))
(defn get-resource-dir []      (first-exist jarman-resource-dir-list))
(defn get-jarman []            (first-exist dot-jarman-paths-list))
(defn get-jarman-data []       (first-exist dot-jarman-data-paths-list))
(defn get-jarman-executable [] jarman-executable)

;;; RESOURCES
(comment
  "update.log.org"
  "extension.log.org"
  "log.org")
