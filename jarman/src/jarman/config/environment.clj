(ns jarman.config.environment
  (:gen-class)
  (:import (java.io IOException FileNotFoundException))
  (:require [clojure.string :as s]
            [clojure.java.io :as io]
            [clojure.pprint :refer [cl-format]]))

(defn first-exist
  "Description
    Return first existed in list file,
    if all of files doesn't exist, then
    raise FileNotFoundException

    If you do not want to get exception on
    retuning resource use opt param `throwable?`

  Example
    (first-exist [(clojure.io.file \"./\")])         ->  #<File>{\"./\"}
    (first-exist [])                         -{error}-> FileNotFoundException
    (first-exist [] :throwable false)        -{error}-> nil"
  [file-list & {:keys [throwable?] :or {throwable? true}}]
  (if-let [file (first (filter #(.exists %) file-list))] file
          (if-not throwable? nil
           (throw (FileNotFoundException.
                   (cl-format nil "No one file hasn't been found ~{`~A`~^, ~}"
                              (map str file-list)))))))

(defmacro defresource [resource-name doc sources]
  {:pre [(symbol resource-name )(string? doc) (sequential? sources)]}
  `(defn ~resource-name ~doc [& {:keys [~'throwable?] :or {~'throwable? true}}]
     (jarman.config.environment/first-exist ~sources :throwable? ~'throwable?)))

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
;; (def hostname (s/trim (:out (clojure.java.shell/sh "hostname"))))

(def resource-fonts
  [(clojure.java.io/resource "fonts/jetbrain/JetBrainsMono-BoldItalic.ttf")
   (clojure.java.io/resource "fonts/jetbrain/JetBrainsMono-Bold.ttf")
   (clojure.java.io/resource "fonts/jetbrain/JetBrainsMono-ExtraBoldItalic.ttf")
   (clojure.java.io/resource "fonts/jetbrain/JetBrainsMono-ExtraBold.ttf")
   (clojure.java.io/resource "fonts/jetbrain/JetBrainsMono-ExtraLightItalic.ttf")
   (clojure.java.io/resource "fonts/jetbrain/JetBrainsMono-ExtraLight.ttf")
   (clojure.java.io/resource "fonts/jetbrain/JetBrainsMono-Italic.ttf")
   (clojure.java.io/resource "fonts/jetbrain/JetBrainsMono-LightItalic.ttf")
   (clojure.java.io/resource "fonts/jetbrain/JetBrainsMono-Light.ttf")
   (clojure.java.io/resource "fonts/jetbrain/JetBrainsMono-MediumItalic.ttf")
   (clojure.java.io/resource "fonts/jetbrain/JetBrainsMono-Medium.ttf")
   (clojure.java.io/resource "fonts/jetbrain/JetBrainsMonoNL-BoldItalic.ttf")
   (clojure.java.io/resource "fonts/jetbrain/JetBrainsMonoNL-Bold.ttf")
   (clojure.java.io/resource "fonts/jetbrain/JetBrainsMonoNL-ExtraBoldItalic.ttf")
   (clojure.java.io/resource "fonts/jetbrain/JetBrainsMonoNL-ExtraBold.ttf")
   (clojure.java.io/resource "fonts/jetbrain/JetBrainsMonoNL-ExtraLightItalic.ttf")
   (clojure.java.io/resource "fonts/jetbrain/JetBrainsMonoNL-ExtraLight.ttf")
   (clojure.java.io/resource "fonts/jetbrain/JetBrainsMonoNL-Italic.ttf")
   (clojure.java.io/resource "fonts/jetbrain/JetBrainsMonoNL-LightItalic.ttf")
   (clojure.java.io/resource "fonts/jetbrain/JetBrainsMonoNL-Light.ttf")
   (clojure.java.io/resource "fonts/jetbrain/JetBrainsMonoNL-MediumItalic.ttf")
   (clojure.java.io/resource "fonts/jetbrain/JetBrainsMonoNL-Medium.ttf")
   (clojure.java.io/resource "fonts/jetbrain/JetBrainsMonoNL-Regular.ttf")
   (clojure.java.io/resource "fonts/jetbrain/JetBrainsMonoNL-ThinItalic.ttf")
   (clojure.java.io/resource "fonts/jetbrain/JetBrainsMonoNL-Thin.ttf")
   (clojure.java.io/resource "fonts/jetbrain/JetBrainsMono-Regular.ttf")
   (clojure.java.io/resource "fonts/jetbrain/JetBrainsMono-ThinItalic.ttf")
   (clojure.java.io/resource "fonts/jetbrain/JetBrainsMono-Thin.ttf")
   (clojure.java.io/resource "fonts/ubuntu/Ubuntu-BoldItalic.ttf")
   (clojure.java.io/resource "fonts/ubuntu/Ubuntu-Bold.ttf")
   (clojure.java.io/resource "fonts/ubuntu/Ubuntu-Italic.ttf")
   (clojure.java.io/resource "fonts/ubuntu/Ubuntu-LightItalic.ttf")
   (clojure.java.io/resource "fonts/ubuntu/Ubuntu-Light.ttf")
   (clojure.java.io/resource "fonts/ubuntu/Ubuntu-MediumItalic.ttf")
   (clojure.java.io/resource "fonts/ubuntu/Ubuntu-Medium.ttf")
   (clojure.java.io/resource "fonts/ubuntu/Ubuntu-Regular.ttf")])

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

(defresource get-configs-language-file
  "list of all configururations directory in client filesystem"
  [(io/file user-home resource-folder config-folder-name "language.edn")
   (io/file       "." resource-folder config-folder-name "language.edn")])

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

(defresource get-view-clj
  "list of all `.jarman.data` file paths in system"
  [(io/file       "." "view.clj")
   (io/file       "." "src" "jarman" "logic" "view.clj")])

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
