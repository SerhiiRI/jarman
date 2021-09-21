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

;;; ENV VARIABLES ;;;
(def path (System/getenv "path"))

;;; RESOURCES
(comment
  (def plugin-folder-name "plugins")
  (def config-folder-name "config")
  (def jarman-resource-folder ".jarman.d")
  (def ^:private jarman-plugins-dir-list
    "List of all plugins directory in client filesystem"
    [(io/file env/user-home config.environment plugin-folder-name)
     (io/file           "." config.environment plugin-folder-name)])

  (def ^:private jarman-configs-dir-list
    "list of all configururations directory in client filesystem"
    [(io/file env/user-home config.environment config-folder-name)
     (io/file           "." config.environment config-folder-name)])

  (def ^:private dot-jarman-paths-list
    "list of all `.jarman` file paths in system"
    [(io/file env/user-home ".jarman")
     (io/file           "." ".jarman")])

  (def ^:private dot-jarman-data-paths-list
    "list of all `.jarman.data` file paths in system"
    [(io/file env/user-home ".jarman.data")
     (io/file           "." ".jarman.data")
     (io/file           "." "src" "jarman" "managment" jarman-data)])

  (def ^:private jarman-executable
    (io/file "." "Jarman.exe"))
  
  "update.log.org"
  "extension.log.org"
  "log.org"

  (defn first-exist [jarman-file-list]
    (if-let [file (first (filter #(.exists %) jarman-data-all))]
     (binding [*ns* (find-ns 'jarman.managment.data)] 
       (load-file (str file)))
     (throw (FileNotFoundException. "Not found '.jarman.data' file.")))))
