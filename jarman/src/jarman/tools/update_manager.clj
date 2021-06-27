(ns jarman.tools.update-manager
  (:require
   ;; clojure lib
   [clojure.string :as string]
   [clojure.java.io :as io]
   ;; packages 
   [miner.ftp :as ftp]
   [me.raynes.fs :as gfs]
   ;; local functionality
   [jarman.tools.config-manager :as cm]
   [jarman.tools.lang :refer [in?]]
   [jarman.tools.fs :as fs]))

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
;;; +------------+ <---(merge /config)--- +---------+
;;; |  unpacked  |                        | Files   |
;;; |  zip       |  /(change program)\    | in repo |
;;; +------------+ --(recursive copy)---> +---------+
;;;                 \(test file copy)/
;;;     


;; Struktura danych opisująca jeden package
(defrecord PandaPackage [file name version artifacts uri])
(def ^:dynamic *repositories* ["ftp://jarman:dupa@trashpanda-team.ddns.net"
                               "/home/serhii/programs/jarman/jarman/test-repository"])
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

(comment
  (ftp-list-files "ftp://jarman:dupa@192.168.1.69")
  (ftp-list-files "ftp://jarman:dupa@trashpanda-team.ddns.net"))

(defn ftp-put-file [ftp-repo-url repo-path file-path]
  (ftp/with-ftp [client ftp-repo-url]
    (ftp/client-cd client repo-path)))

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

(defn- max-version
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
    (get-all-packages *repositories*)
      ;;=> [#PandaPackage{..}, #PandaPackage {
  See
    `*repositories*` - list of all repositories"
  [repositories]
  (mapcat
   (fn [url]
     (let [ftp?  (every-pred is-url? is-url-allowed? is-url-repository?)
           path? (every-pred is-path?)] 
       (cond
         (ftp? url)  (preproces-from-ftp  url)
         (path? url) (preproces-from-path url)
         :else nil))) repositories))

(defn download-package
  "Description
    Downloand package from local or remote ftp repositories"
  [^PandaPackage package]
  (let [ftp? (every-pred is-url? is-url-allowed? is-url-repository?)
        path? (every-pred is-path?)] 
   (cond
     (ftp?  (:uri package)) (ftp-get-file  (:uri package)) 
     (path? (:uri package)) (path-get-file (:uri package))
     :else nil)))

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
          (println (format "[!] Finished update") )))
    ;; (catch java.io.IOException e)
    ))

;;;;;;;;;;;;;;;;;;;;;;
;;; user functions ;;;
;;;;;;;;;;;;;;;;;;;;;;

(defn show-list-of-all-packages []
  (get-all-packages *repositories*))

(defn check-package-for-update []
  (max-version (get-all-packages *repositories*)))

(comment
  (def --tmp-package-list-- (get-all-packages *repositories*))
  (max-version --tmp-package-list--)
  (update-project (max-version --tmp-package-list--))
  (update-project (max-version (get-all-packages *repositories*)))
  ;; unzip test
  (unzip "ftp://jarman:bliatdoit@192.168.1.69//jarman/jarman-1.0.4.zip" "kupa.zip")
  (unzip "tst/kupa.zip" "tst/kkk"))

