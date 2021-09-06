(ns jarman.tools.update-manager
  (:require
   ;; clojure lib
   [clojure.string :as string]
   [clojure.pprint :refer [cl-format]]
   [clojure.java.io :as io]
   ;; packages 
   [miner.ftp :as ftp]
   [me.raynes.fs :as gfs]
   ;; local functionality
   [jarman.tools.config-manager :as cm]
   [jarman.config.dot-jarman-param :refer [defvar]]
   [jarman.tools.lang :refer [in?]]
   [jarman.tools.fs :as fs]
   ;; environtemnt variables
   [jarman.config.environment :as env])
  (:import (java.io IOException FileNotFoundException)))

;;; Package standart
;;;    jarman-0.1.12-windows.zip
;;;    `jarman` - program name. (Declare in `*program-name*`)
;;;         - simple name of program/plugin, or other things you need would be.
;;;    `0.1.12` - version of program, geted from project.clj file. (Declare in `*program-vers*`)
;;;         - using x.x.x pattern convinience. It mostly tip, not a rule, as rule. 
;;;                 | | +- hotfix
;;;                 | +--- feature
;;;                 +----- functionality update
;;;    `windows.zip` - program attribute. (Declare in `*program-attr*`).
;;;    Attribute rule:
;;;         - split with dot (.) symbol
;;;         - thats all
;;;
;;;    For geting three part use this regular expression formula
;;;      #"(\w+)-(\w+\.\w+\.\w+)[-.]{0,1}(.+)"
;;;        ----- ---------------(. or -)  --
;;;          |          |                  +--- attributes
;;;          |          +---------------------- x.x.x version
;;;          +--------------------------------- program name
;;;
;;;
;;; Replace program by update program manager
;;; 
;;; +---------------------+
;;; | FTP or Path Server  |
;;; +---------------------+
;;;     |
;;;     V
;;; +------------+                        +---------+
;;; |  unpacked  |                        | Files   |
;;; |  zip       |  /(change program)\    | in repo |
;;; +------------+ --(recursive copy)---> +---------+
;;;                 \(test file copy)/
;;;     

;;;;;;;;;;;;;
;;; PATHS ;;;
;;;;;;;;;;;;;

(def ^:private jarman-plugins-dir-list
  "List of all plugins directory in client filesystem"
  [(io/file env/user-home ".jarman.d" "plugins")
   (io/file           "." ".jarman.d" "plugins")])

(def ^:private jarman-configs-dir-list
  "list of all configururations directory in client filesystem"
  [(io/file env/user-home ".jarman.d" "config")
   (io/file           "." ".jarman.d" "config")])

(def ^:private dot-jarman-paths-list
  "list of all `.jarman` file paths in system"
  [(io/file env/user-home ".jarman")
   (io/file           "." ".jarman")])

(def ^:private dot-jarman-data-paths-list
  "list of all `.jarman.data.clj` file paths in system"
  [(io/file env/user-home ".jarman.data.clj")
   (io/file           "." ".jarman.data.clj")])

(def ^:private jarman-executable "Jarman.exe")

;;;;;;;;;;;;;;;;;;;;
;;; DECLARATIONS ;;;
;;;;;;;;;;;;;;;;;;;;

;; Struktura danych opisująca jeden package
(defrecord PandaPackage [file name version artifacts uri])
(defvar jarman-update-repository-list ["ftp://jarman:dupa@trashpanda-team.ddns.net"]
  :type clojure.lang.PersistentList
  :group :update-system)

(def ^:dynamic *program-name* "jarman")
(def ^:dynamic *program-attr* ["zip" "windows.zip"])
(def ^:dynamic *program-vers* `~(-> "project.clj" slurp read-string (nth 2)))
(def blocked-repo-list ["www.google.com"])

(defn is-url? [repo-string]
  (some? (re-matches #"^(http|https|ftp).+" repo-string)))

(defn is-path? [repo-string]
  (.exists (io/file repo-string)))

(defn is-url-allowed? [repo-string]
  (let [[url protocol domain end] (re-matches #"(\w*://)([\w-_.]+)([:\w\W]*)" repo-string)]
    (not (some #(in? blocked-repo-list %) [url domain]))))

(defn is-url-repository? [url-string]
  (if (try (io/input-stream url-string)
        (catch java.io.IOException e)) true false))

(defn copy [uri file]
  (with-open [in (io/input-stream uri)
              out (io/output-stream file)]
    (io/copy in out)))

;; (copy "https://file-examples.com/wp-content/uploads/2017/02/file_example_CSV_5000.csv" "suka.csv")
;; (copy "suka.csv" "src/sukabliat.bliat")

;;;;;;;;;;;;;;;;;;;;;;;;
;;; FTP file manager ;;;
;;;;;;;;;;;;;;;;;;;;;;;;

(defn ftp-list-files
  "Description
    Show all files on remote FTP Server 
  Example 
    (ftp-list-files \"ftp://jarman:dupa@trashpanda-team.ddns.net\")
      ;; =>  [\"hrtime-1.0.1.zip\" \"hrtime-1.0.2.zip\" \"hrtime-1.0.3.zip\" \"jarman.txt\"]"
  [repo-url]
  (ftp/with-ftp [client repo-url]
    (ftp/client-cd client "jarman")
    (ftp/client-all-names client)))  

(defn ftp-put-file [ftp-repo-url repo-path file-path]
  (ftp/with-ftp [client ftp-repo-url]
    (ftp/client-cd client repo-path)
    (ftp/client-put client file-path)))

(defn ftp-get-file
  #_([file-url]
     (if-let [[url repo-url path-to-file] (re-matches #"(ftp://.+)/.+/{1}(.+)" file-url)]
       (ftp-get-file repo-url path-to-file)))
  #_([repo-url file-name]
     (ftp/with-ftp [client repo-url]
       (ftp/client-cd client "jarman")
       (let [in (ftp/client-get-stream client file-name)
             out (io/output-stream file-name)]
         (io/copy in out) file-name)
       ))
  ([file-url]
   (if-let [[url repo-url path-to-file] (re-matches #"(ftp://.+)/.+/{1}(.+)" file-url)]
     (do (copy url path-to-file)
         path-to-file)))
  ([repo-url file-name]
   (do (copy (string/join "/" [repo-url "jarman" file-name]) file-name))))

(comment
  (ftp-list-files "ftp://jarman:dupa@192.168.1.69")
  (ftp-list-files "ftp://jarman:dupa@trashpanda-team.ddns.net"))

;;;;;;;;;;;;;;;;;;;;;;;;;
;;; path file manager ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;

(defn path-list-files
  "Description 
    List all file on selected Local repository folder

  Example
    (path-list-files \"/home/serhii/programs/jarman/jarman/test-repository\")
    ;;=> 
      (#object[java.io.File 0x231b8775 \"/home/.../jarman/jarman-0.0.4-windows.zip\"]
       #object[java.io.File 0x25388763 \"/home/.../jarman/scritp.sh\"]
       #object[java.io.File 0x53363372 \"/home/.../jarman/jarman-0.0.1.zip\"]
       #object[java.io.File 0x1a24fdad \"/home/.../jarman/jarman-0.0.3-windows.zip\"]
       #object[java.io.File 0x65fbf1b3 \"/home/.../jarman/jarman-0.0.2.zip\"])"
  [repo-path]
  (let [path (io/file repo-path "jarman")]
    (if-not (.exists path) []
            (filter #(.isFile %) (.listFiles path)))))

(defn path-get-file
  ([path]
   (with-open [in  (io/input-stream path)
               out (io/output-stream (.getName (io/file path)))]
     (io/copy in out)
     (.getName (io/file path))))
  ([path file-name]
   (with-open [in (io/input-stream path)
               out (io/output-stream file-name)]
     (io/copy in out)
     file-name)))

;;;;;;;;;;;;;;;;;;;;;;;;
;;; package wrappers ;;;
;;;  and logistic    ;;;
;;;;;;;;;;;;;;;;;;;;;;;;

(defn version-comparator
  "Description
    Compare two different version('v1', 'v2') in dot(.) separated
    Notation. For comparison use 'comparator-f'.

  How it do comparison?
    Embaded Y-combinar do recursion from head to tail
    and compare one-by-one number. if number not eq to
    each other, then do comparison.
    Step for example belove:
     (version-comparator #'>= \"1.0.2\" \"1.0.1\")
      1. (Y (1 0 2) (1 0 1))
          => (not= 1 1)  --  if eq then go to next number
      2. (Y (0 2) (0 1))
          => (not= 0 0)  --  if eq then go to next number
      3. (Y (2) (1))
          => (not= 2 1)  --  if not eq, then do test func
      4. (>= 2 1)
          => true

  Example
    (version-comparator #'<= \"0.1.1\" \"0.1.0\") => false
    (version-comparator #'>= \"1.0.2\" \"1.0.1\") => true"
  [comparator-f v1 v2]
  (letfn [(parse-int [s] (map #(Integer. %) (re-seq  #"\d+" s)))]
    (((fn [f] (f f))
      (fn [f] (fn [cmpr xs ys]
               (cond (empty? xs) false
                     (or (= 1 (count xs)) (not= (first xs) (first ys))) (cmpr (first xs) (first ys))
                     :else ((f f) cmpr (rest xs) (rest ys)))))) comparator-f (parse-int v1) (parse-int v2))))

(defn max-version
  "Return max version package from `package-list`, if current
  program version equal to package - return `nil`"
  [package-list]
  (let [current-package (PandaPackage. nil *program-name* *program-vers* nil nil)
        upgrade-package
        (reduce (fn [acc package]
                  (if (and (= (:name acc) (:name package))
                         (version-comparator #'<= (:version acc) (:version package))
                         (in? *program-attr* (:artifacts package))) package acc))
                current-package package-list)]
    (if (not= current-package upgrade-package) upgrade-package)))

(defn- match-package
  "Description
    Maching packge by name 
    Package standart
     jarman-0.1.12-windows.zip
     `jarman` - program name. (Declare in `*program-name*`)
          - simple name of program/plugin, or other things you need would be.
     `0.1.12` - version of program, geted from project.clj file. (Declare in `*program-vers*`)
          - using x.x.x pattern convinience. It mostly tip, not a rule, as rule. 
                  | | +- hotfix
                  | +--- feature
                  +----- functionality update
     `windows.zip` - program attribute. (Declare in `*program-attr*`).
     Attribute rule:
          - split with dot (.) symbol
          - thats all
  
     For geting three part use this regular expression formula
       #\"(\\w+)-(\\w+\\.\\w+\\.\\w+)[-.]{0,1}(.+)\"
         ----- ---------------(. or -)  ----------
           |          |                  +--- attributes
           |          +---------------------- x.x.x version
           +--------------------------------- program name
  

  Example
    (match-package \"jarman-1.0.2.zip\")
     ;; => {:file \"jarman-1.0.2.zip\", :name \"jarman\", :version \"1.0.2\", :artifacts \"zip\"}
    (match-package \"jarman-1.0.2-windows.zip\")
     ;; => {:file \"jarman-1.0.2-windows.zip\", :name \"jarman\", :version \"1.0.2\", :artifacts \"windows.zip\"}
    (match-package \"jarman-1.0.2.windows.zip\")
     ;; => {:file \"jarman-1.0.2-windows.zip\", :name \"jarman\", :version \"1.0.2\", :artifacts \"windows.zip\"}"
  [package-name]
  (if-let [[file name version artifacts] (re-matches #"(\w+)-(\w+\.\w+\.\w+)[-\.]{0,1}(.+)" package-name)]
    {:file file
     :name name
     :version version
     :artifacts artifacts}))

(defn preproces-from-ftp
  "Description
    Return list of panda packages on ftp `repository` param
  Example
    (preproces-from-url \"ftp://jarman:dupa@trashpanda-team.ddns.net\")
      [#PandaPackage
       {:file \"hrtime-1.0.1.zip\",
        :name \"hrtime\",
        :version \"1.0.1\",
        :artifacts \"zip\",
        :uri
        \"ftp://jarman:dupa@trashpanda-team.ddns.net/jarman/hrtime-1.0.1.zip\"} ...]"
  [repository]
  (reduce #(if-let [m (match-package %2)]
             (conj %1 (map->PandaPackage 
                       (into m {:uri (string/join "/" [repository "jarman" %2])})))
             %1)
          [] (ftp-list-files repository)))

(defn preproces-from-path
  "Description
    Return list of panda packages on local folder `repository` param
  Example
    (preproces-from-path \"/home/serhii/programs/jarman/jarman/test-repository\")
     ;;=>
       [{:file \"jarman-0.0.4-windows.zip\",
         :name \"jarman\",
         :version \"0.0.4\",
         :artifacts \"windows.zip\",
         :uri \"/home.../jarman-0.0.4-windows.zip\"} ...]"
  [repository]
  (reduce #(let []
             (if-let [m (match-package (.getName %2))]
               (conj %1 (map->PandaPackage 
                         (into m {:uri (.getAbsolutePath %2)})))
               %1))
          [] (path-list-files repository)))


(defn get-all-packages
  "Description
    Get list of all packages from all repositories  
  Example
    (get-all-packages jarman-update-repository-list)
      ;;=> [#PandaPackage{..}, #PandaPackage {
  See
    `jarman-update-repository-list` - list of all repositories"
  [repositories]
  (mapcat
   (fn [url]
     (let [ftp?  (every-pred is-url? is-url-allowed? is-url-repository?)
           path? (every-pred is-path?)] 
       (cond
         (ftp? url)  (preproces-from-ftp  url)
         (path? url) (preproces-from-path url)
         :else nil))) repositories))

(defn get-filtered-packages
  "Description
    Get list of all package, then filter
    relative to current environment attribues   
  Example
    (get-all-packages jarman-update-repository-list)
      ;;=> [#PandaPackage{..}, #PandaPackage {
  See
    `jarman-update-repository-list` - list of all repositories"
  [repositories]
  (reduce (fn [acc package]
            (if (and (= *program-name* (:name package))
                   (in? *program-attr* (:artifacts package)))
              (conj acc package) acc))
          [] (get-all-packages repositories)))

(defn download-package
  "Description
    Downloand package from local or remote ftp repositories"
  [^PandaPackage package]
  (let [ftp? (every-pred is-url? is-url-allowed? is-url-repository?)
        path? (every-pred is-path?)] 
    (if-let [package
             (cond
               (ftp?  (:uri package)) (ftp-get-file  (:uri package)) 
               (path? (:uri package)) (path-get-file (:uri package))
               :else nil)]
      package
      (throw (ex-info (format "Package `%s` cannot be downloaded from repository." (:uri package))
                      {:type :package-cannot-be-downloaded
                       :package package})))))

#_(comment
    (defn update-project [^PandaPackage package]
      (let [unzip-folder (str (gensym "transact"))
            unzip-config-folder (string/join java.io.File/separator [unzip-folder "config"])]
        (println (format "[!] Download package from %s" (:uri package)))
        (if (download-package package)
          (do (println (format "[!] Create folder %s" unzip-folder))
              (fs/create-dir unzip-folder)
              (println (format "[!] Unpack to folder %s" unzip-folder))
              (fs/unzip (:file package) unzip-folder)
              (println (format "[!] Merge configurations in %s" unzip-config-folder))
              (fs/copy-dir-replace unzip-folder ".")
              (println (format "[!] Delete update-package %s" (:file package)) )
              (gfs/delete (:file package))
              (println (format "[!] Delete temporary folder %s" unzip-config-folder) )
              (gfs/delete-dir unzip-folder)
              (println (format "[!] Finished update"))))
        ;; (catch java.io.IOException e)
        )))

;; Example Panda Package unzip folder distribution
;; -----------------------------------------------
;; .
;; +-- config
;; |   +-- a.txt
;; |   +-- b.txt
;; |   \-- c.txt
;; +-- .jarman
;; +-- .jarman.data.clj
;; +-- Jarman.exe
;; \-- plugins
;;     +-- a
;;     |   +-- a.clj
;;     |   \-- a.deps.clj
;;     +-- b
;;     |   +-- b.clj
;;     |   \-- b.deps.clj
;;     \-- c
;;         +-- c.clj
;;         \-- c.deps.clj
;; ------------------------------------------------

(defn make-transact-folder [directory]
  (fn [& paths]
    (apply io/file directory paths)))

(defn verify-file [file]
  (println (cl-format nil "  ~:[not exists~;verified~] file =~A=" (.exists file) (str file)))
  (when (not (.exists file))
    (throw (ex-info (format "File =%s= not exists" (str file))
                    {:type :install-file-not-exists}))))

(defn copy-file [f-from f-out]
  (try (do
         (print (format "  copy file =%s= to =%s= ..." f-from f-out))
         (fs/copy f-from f-out)
         (println "done!"))
       (catch IOException e
         (throw (ex-info (format "I/O error, while coping =%s= to =%s= ." f-from f-out) {:type :io-exception})))
       (catch Exception e
         (throw (ex-info (format "Error while coping =%s= to =%s=" f-from f-out) {:type :common-exception})))))

(defn copy-dir [d-from d-out]
  (try (do
         (print (format "  copy directory =%s= to =%s= ..." d-from d-out))
         (fs/copy-dir-replace d-from d-out)
         (println "done!"))
       (catch IOException e
         (throw (ex-info (format "I/O error, while coping =%s= to =%s= ." d-from d-out) {:type :io-exception})))
       (catch Exception e
         (throw (ex-info (format "Error while coping =%s= to =%s=" d-from d-out) {:type :common-exception})))))

(defn delete-file [file]
  (try (do
         (print (format "  delete file =%s= ..." file))
         (gfs/delete file)
         (println "done!"))
       (catch Exception e
         (println "Error deleting file"))))

(defn delete-dir [directory]
  (try (do
         (print (format "  delete directory =%s= ..." directory))
         (gfs/delete-dir directory)
         (println "done!"))
       (catch Exception e
         (println "Error deleting directory"))))

(defn unzip [package-file unzip-folder]
  (try 
    (do
      (print (format "  unzipping =%s= PandaPackage into directory =%s= ..." package-file unzip-folder))
      (fs/unzip package-file unzip-folder)
      (println "done!"))
    (catch Exception e
      (throw (ex-info (format "Error while unzip =%s= into =%s=" package-file unzip-folder) {:type :unzipping-error})))))

(defn zip [package-file folder-to-zip]
  (try 
    (do
      (print (format "  Zipping =%s= into PandaPackage =%s= ..." folder-to-zip package-file ))
      (fs/zip folder-to-zip package-file)
      (println "done!"))
    (catch Exception e
      (throw (ex-info (format "Error while try make zip =%s= from =%s=" package-file folder-to-zip) {:type :zipping-error})))))

(defn quick-timestamp []
  (.format (java.text.SimpleDateFormat. "YYYY-MM-dd HH:mm") (java.util.Date.)))

(defn update-project [^PandaPackage package]
  (let [;; Global configurations
        unzip-folder (str (gensym "transact"))
        ;; unzip-config-folder (clojure.java.io/file unzip-folder "config")
        
        ;; Files and folder inside unzipped
        ;; transactional directory 
        transact-file (make-transact-folder unzip-folder)
        transact-jarman-plugins-dir   (transact-file "plugins")
        transact-jarman-configs-dir   (transact-file "config")
        transact-jarman-dot-file      (transact-file ".jarman")
        transact-jarman-dot-data-file (transact-file ".jarman.data.clj")
        transact-jarman-executable    (transact-file "Jarman.exe")

        ;; destination directories
        destination-plugins-dir (first jarman-plugins-dir-list)
        destination-configs-dir (first jarman-configs-dir-list)
        destination-jarman-dot  (first dot-jarman-paths-list)
        destination-jarman-data (first dot-jarman-data-paths-list)
        destination-jarman-executable "Jarman.exe"]

    (println (format "* Install package (%s)" (quick-timestamp)))
    (println (format "  install =%s=" (:file package)))
    (println "** Package")
    (println (format "  downloading =%s=" (:uri package)))
    (download-package package)
    (verify-file (clojure.java.io/file (:file package)))
    (unzip (:file package) unzip-folder)
    (verify-file (clojure.java.io/file unzip-folder))
    (println "** Unpacking package")
    (verify-file (clojure.java.io/file unzip-folder))
    (println "** Verifing folder")
    (verify-file transact-jarman-plugins-dir)
    (verify-file transact-jarman-configs-dir)
    (verify-file transact-jarman-dot-file)
    (verify-file transact-jarman-dot-data-file)
    (verify-file transact-jarman-executable)
    (println "** Verifing destinations")
    (verify-file (clojure.java.io/file env/user-home))
    (println "** Copying files")
    (copy-file transact-jarman-dot-file destination-jarman-dot)
    (copy-file transact-jarman-dot-data-file destination-jarman-data)
    (copy-dir transact-jarman-configs-dir destination-configs-dir)
    (copy-dir transact-jarman-plugins-dir destination-plugins-dir)
    (copy-file transact-jarman-executable destination-jarman-executable)
    (println "** Remove temporary files")
    (delete-file (:file package))
    (delete-dir unzip-folder)))

(defn clean-up-environment []
  (let [destination-plugins-dir (first jarman-plugins-dir-list)
        destination-configs-dir (first jarman-configs-dir-list)
        destination-jarman-dot  (first dot-jarman-paths-list)
        destination-jarman-data (first dot-jarman-data-paths-list)
        destination-jarman-executable "Jarman.exe"]
    (println (format "* Delete environment (%s)" (quick-timestamp)))
    (println "** Remove .jarman.d folders")
    ;; TEMPORARY ADD
    (delete-dir (clojure.java.io/file env/user-home ".jarman.d"))
    ;; TEMPORARY REMOVE
    ;; (delete-dir destination-configs-dir)
    ;; (delete-dir destination-plugins-dir)
    (println "** Remove jarman configs")
    (delete-file destination-jarman-dot)
    (delete-file destination-jarman-data)
    (println "** Remove Executable")
    (delete-file destination-jarman-executable)))

(defn build-package []
  (let [;; Global configurations
        unzip-folder (str (gensym "transact"))
        ;; package params
        version *program-vers* ;; "0.0.1"
        prog-name *program-name* ;; "jarman"
        artifact ".zip"
        package-name (format "%s-%s%s" prog-name version artifact)
        ;; Files and folder inside unzipped
        ;; transactional directory 
        transact-file (make-transact-folder unzip-folder)
        transact-jarman-plugins-dir   (transact-file "plugins")
        transact-jarman-configs-dir   (transact-file "config")
        transact-jarman-dot-file      (transact-file ".jarman")
        transact-jarman-dot-data-file (transact-file ".jarman.data.clj")
        transact-jarman-executable    (transact-file "Jarman.exe")
        ;; original source files
        plugins-dir (clojure.java.io/file "./.jarman.d/plugins")
        configs-dir (clojure.java.io/file "./.jarman.d/config")
        jarman-dot  (clojure.java.io/file ".jarman")
        jarman-data (clojure.java.io/file "src/jarman/managment/.jarman.data.clj")
        jarman-executable (clojure.java.io/file "Jarman.exe")]

    (println (format "* Bulding PandaPackage (%s)" (quick-timestamp)))
    
    (println "** Testing file structure")
    (verify-file plugins-dir)
    (verify-file configs-dir)
    (verify-file jarman-dot)
    (verify-file jarman-data)
    (verify-file jarman-executable)

    (println "** Making transact dir")
    (.mkdir      (clojure.java.io/file unzip-folder))
    (verify-file (clojure.java.io/file unzip-folder))
    
    (println "** Moving data to directory")
    (copy-file jarman-dot transact-jarman-dot-file)
    (copy-file jarman-data transact-jarman-dot-data-file)
    (copy-dir configs-dir transact-jarman-configs-dir)
    (copy-dir plugins-dir transact-jarman-plugins-dir)
    (copy-file jarman-executable transact-jarman-executable)

    (println "** Testring copyed files")
    (verify-file transact-jarman-plugins-dir)
    (verify-file transact-jarman-configs-dir)
    (verify-file transact-jarman-dot-file)
    (verify-file transact-jarman-dot-data-file)
    (verify-file transact-jarman-executable)

    (println "** Creation PandaPackage")
    (zip package-name (clojure.java.io/file unzip-folder))
    (println "** Clean temporary files")
    (delete-dir unzip-folder)
    (map->PandaPackage {:file package-name, :name prog-name, :version version, :artifacts artifact, :uri nil})))

;; (build-package)

;; (send-package #jarman.tools.update_manager.PandaPackage{:file "jarman-0.0.1.zip", :name "jarman", :version "0.0.1", :artifacts ".zip", :uri nil}
;;               (first jarman-update-repository-list))


(defn send-package [^PandaPackage package ^String repository]
  (println "** send package to repository")
  (cond
    ;; Send on remote repository
    (is-url? repository)
    (do
      (print (format "  sending package =%s= to =%s= ..." (:file package) repository))
      (ftp-put-file repository *program-name* (:file package))
      (println "done!")
      (assoc package :uri (format "%s/%s/%s" repository *program-name* (:file package))))
    ;; Send on local file repository
    (is-path? repository)
    (do
      (copy-file (clojure.java.io/file (:file package))
                 (clojure.java.io/file repository *program-name* (:file package)))
      (assoc package :uri (clojure.java.io/file repository *program-name* (:file package))))))

(def ^:dynamic *debug-to-file* true)

(defn info-list-repository-packages [package-list]
  (doall
   (map-indexed
    (fn [i {:keys [file _ _ _ uri]}]
      (println (format "  %d. package =%s= from =%s=" i file uri)))
    package-list)))

(defn info-max-package [package-list]
  (let [{:keys [file _ version artifacts uri]} (max-version package-list)]
    (println (format "  package *%s*, version *%s*, artifacts *%s* from =%s=" file version artifacts uri))))

(defn info-packages [package-list]
  (println (format "* Package info (%s)" (quick-timestamp)))
  (println "** Max Version package")
  (info-max-package package-list)
  (println "** List all packages")
  (info-list-repository-packages package-list))

;;;;;;;;;;;;;;;;;;
;;; Procedures ;;;
;;;;;;;;;;;;;;;;;;

(defn procedure-create-log-file []
  (if (not (.exists (clojure.java.io/file "update-manager-log.org")))
    (spit "update-manager-log.org"
          "#+TITLE: Update manager Log file\n#+AUTHOR: Serhii Riznychuk\n#+EMAIL: sergii.riznychuk@gmail.com\n#+STARTUP: overview
")))



(defn procedure-package []
  (procedure-create-log-file)
  (let [to-file? *debug-to-file*]
    (binding [*out* (if to-file? (clojure.java.io/writer "update-manager-log.org" :append true))]
      (let [package (build-package)]
        (doall (map (partial send-package package) (deref jarman-update-repository-list)))
        (delete-file (clojure.java.io/file (:file package)))
        package))))

(defn procedure-update
  ([]
   (procedure-create-log-file)
   (let [to-file? *debug-to-file*]
     (binding [*out* (if to-file? (clojure.java.io/writer "update-manager-log.org" :append true))]
       (let [package-list (get-all-packages (deref jarman-update-repository-list))]
         (info-packages package-list)
         (update-project (max-version package-list))))))
  ([package]
   (procedure-create-log-file)
   (let [to-file? *debug-to-file*]
     (binding [*out* (if to-file? (clojure.java.io/writer "update-manager-log.org" :append true))]
       (update-project package)))))

(defn procedure-info []
  (procedure-create-log-file)
  (let [to-file? *debug-to-file*]
    (binding [*out* (if to-file? (clojure.java.io/writer "update-manager-log.org" :append true))]
      (let [package-list (get-filtered-packages (deref jarman-update-repository-list))]
        (info-packages package-list)
        package-list))))

(defn procedure-clean-env []
  (procedure-create-log-file)
  (let [to-file? *debug-to-file*]
    (binding [*out* (if to-file? (clojure.java.io/writer "update-manager-log.org" :append true))]
      (clean-up-environment))) true)



(comment
  (procedure-create-log-file)
  ;; functions
  (procedure-clean-env)
  (procedure-package)
  (procedure-update)
  (procedure-info))

;;;;;;;;;;;;;;;;;;;;;;
;;; functions ;;;
;;;;;;;;;;;;;;;;;;;;;;

(defn show-list-of-all-packages []
  (get-all-packages (deref jarman-update-repository-list)))

(defn check-package-for-update []
  (max-version (get-all-packages (deref jarman-update-repository-list))))

(comment
  (def --tmp-package-list-- (get-all-packages (deref jarman-update-repository-list)))
  (max-version --tmp-package-list--)
  
  (update-project (max-version --tmp-package-list--))
  (update-project (max-version (get-all-packages (deref jarman-update-repository-list))))
  ;; unzip test
  (unzip "ftp://jarman:bliatdoit@192.168.1.69//jarman/jarman-1.0.4.zip" "kupa.zip"))

