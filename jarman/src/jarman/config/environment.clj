(ns jarman.config.environment
  (:gen-class)
  (:import (java.io IOException FileNotFoundException))
  (:require [clojure.string :as s]
            [clojure.java.io :as io]
            [clojure.pprint :refer [cl-format]]))

(defn first-exist
  "Return first existed in list file, if all of files doesn't exist, then raise FileNotFoundException"
  [file-list]
  (if-let [file (first (filter #(.exists %) file-list))] file
    (throw (FileNotFoundException.
            (cl-format nil "No one file hasn't been found ~{`~A`~^, ~}"
                       (map str file-list))))))

(defmacro defresource [resource-name doc sources]
  {:pre [(symbol resource-name )(string? doc) (sequential? sources)]}
  `(defn ~resource-name ~doc [] (jarman.config.environment/first-exist ~sources)))


;;;;;;;;;;;;;;;;;;;;;
;;; ENV VARIABLES ;;;
;;;;;;;;;;;;;;;;;;;;;

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
(def hostname (s/trim (:out (clojure.java.shell/sh "hostname"))))

;;;;;;;;;;;;;;
;;; JARMAN ;;;
;;;;;;;;;;;;;;

(def jarman-home (-> (clojure.java.io/file ".") .getAbsoluteFile .getParentFile .getAbsolutePath))

;;; ENV VARIABLES ;;;
(def path (System/getenv "path"))
(def plugin-folder-name "plugins")
(def config-folder-name "config")
(def resource-folder ".jarman.d")
(def dot-jarman      ".jarman")
(def dot-jarman-data ".jarman.data")
(def jarman-exe      "Jarman.exe")


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
  [(io/file user-home resource-folder)
   (io/file       "." resource-folder)])

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
  (io/file "." jarman-exe))

(defresource get-plugins-dir
  "List of all plugins directory in client filesystem"
  [(io/file user-home resource-folder plugin-folder-name)
   (io/file       "." resource-folder plugin-folder-name)])

(defresource get-configs-dir
  "list of all configururations directory in client filesystem"
  [(io/file user-home resource-folder config-folder-name)
   (io/file       "." resource-folder config-folder-name)])

(defresource get-resource-dir
  "list of all configururations directory in client filesystem"
  [(io/file user-home resource-folder)
   (io/file       "." resource-folder)])

(defresource get-jarman
  "list of all `.jarman` file paths in system"
  [(io/file user-home dot-jarman)
   (io/file       "." dot-jarman)])

(defresource get-jarman-data
  "list of all `.jarman.data` file paths in system"
  [(io/file user-home dot-jarman-data)
   (io/file       "." dot-jarman-data)
   (io/file       "." "src" "jarman" "managment" dot-jarman-data)])

(defn get-jarman-executable
  "Main Jarman executable file" []
  (io/file "." jarman-exe))


;;; LOG FILES ;;;
(def update-log-org    "update.log.org")
(def extension-log-org "extension.log.org")
(def app-log-org       "app.log.org")

(comment
  (deftest environment-getting-first
    (testing "Testring first"
      (is (try (first-exist
                [(io/file "dupa")])
               (catch FileNotFoundException e true)
               (catch Exception e false)))
      (is (try (= "."
                  (str (first-exist
                        [(io/file ".")])))
               (catch FileNotFoundException e false)
               (catch Exception e false))))))
