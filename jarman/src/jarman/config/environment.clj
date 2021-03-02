(ns jarman.config.environment
  (:gen-class))

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
(def jarman-standart-config (clojure.string/join file-separator ["." "config"]))
(def jarman-user-storage (.getAbsolutePath (clojure.java.io/file user-home ".jarman")))

;;; ENV VARIABLES ;;;
(def path (System/getenv "path"))

