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



(ns jarman.update-manager
  (:require
   [clojure.string :as string]
   [clojure.java.io :as io]
   [jarman.dev-tools :as dv]
   [jarman.config-manager :as cm]
   [miner.ftp :as ftp]
   [me.raynes.fs :as gfs]
   [jarman.fs :as fs]))

;; Struktura danych opisująca jeden package
(defrecord PandaPackage [file name version artifacts uri])
(def ^:dynamic *repositories* (cm/getset "repository.edn" [:repositories] ["ftp://jarman:bliatdoit@192.168.1.69/"]))
(def ^:dynamic *program-name* "jarman")
(def ^:dynamic *program-attr* ["zip" "windows.zip"])
(def ^:dynamic *program-vers* `~(-> "project.clj" slurp read-string (nth 2)))
(def blocked-repo-list ["www.google.com"])

(defn is-url? [repo-string]
  (let [web-ref (apply str (take 4 (string/trim repo-string)))]
    (and (not-empty web-ref)
         (or (= web-ref "http")
             (= web-ref "www.")
             (= web-ref "ftp:")))))

(defn is-path? [repo-string]
  (.exists (io/file repo-string)))

(defn is-url-allowed? [repo-string]
  (let [[url protocol domain end] (re-matches #"(\w*://)([\w-_.]+)([:\w\W]*)" repo-string)]
    (not (some #(dv/in? blocked-repo-list %) [url domain]))))

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

(defn ftp-list-files [repo-url]
  (ftp/with-ftp [client repo-url]
    (ftp/client-cd client "jarman")
    (ftp/client-all-names client)))

(defn ftp-get-file [repo-url file-name]
  (ftp/with-ftp [client repo-url]
    (ftp/client-cd client "jarman")
    (try (let [in (ftp/client-get-stream client file-name)
               out (io/output-stream file-name)]
           (io/copy in out) file-name)
         (catch java.io.IOException e))))


(defn ftp-directly-get [file-url]
  ;;"ftp://localhost:8080/jarman/jarman-0.0.1.zip" => jarman/jarman-0...
 (if-let [[url repo-url path-to-file] (re-matches #"(ftp://.+)/.+/{1}(.+)" file-url)]
   (if (and repo-url path-to-file)
     (ftp/with-ftp [client repo-url]
       (let [all (string/split path-to-file #"/")
             path (butlast all)
             file-name (last all)]
         (doall (map #(ftp/client-cd client (string/trim %)) path))
         (try (let [in (ftp/client-get-stream client file-name)
                    out (io/output-stream file-name)]
                (io/copy in out) file-name)
              (catch java.io.IOException e)))))))


;;;;;;;;;;;;;;;;;;;;;;;;;
;;; PATH file manager ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;

(defn path-list-files [repo-path]
  (let [path (io/file repo-path "jarman")]
    (if-not (.exists path) []
            (filter #(.isFile %) (.listFiles path)))))

(defn path-get-file [path file-name]
  (with-open [in (io/input-stream path)
              out (io/output-stream file-name)]
    (io/copy in out)))

(defn path-directly-get [path]
  (with-open [in (io/input-stream path)
              out (io/output-stream (.getName (io/file path)))]
    (io/copy in out)))

;;;;;;;;;;;;;;;;;;;;;;;;
;;; package wrappers ;;;
;;;  and logistic    ;;;
;;;;;;;;;;;;;;;;;;;;;;;;

(defn version-comparator [comparator-f v1 v2]
  (letfn [(parse-int [s] (map #(Integer. %) (re-seq  #"\d+" s)))]
    (((fn [f] (f f))
      (fn [f] (fn [cmpr xs ys]
               (cond (empty? xs) false
                     (or (= 1 (count xs)) (not= (first xs) (first ys))) (cmpr (first xs) (first ys))
                     :else ((f f) cmpr (rest xs) (rest ys)))))) comparator-f (parse-int v1) (parse-int v2))))

(defn max-version [package-list]
  (let [current-package (PandaPackage. nil *program-name* *program-vers* nil nil)
        upgrade-package (reduce (fn [acc package] (if (and (version-comparator #'<= (:version acc) (:version package))
                                           (dv/in? *program-attr* (:artifacts package))) package acc))
                                current-package package-list)]
    (if (not= current-package upgrade-package) upgrade-package)))

(defn preproces-from-url [repository]
  (reduce #(if(re-matches #"(\w+)-(\w+\.\w+\.\w+)[-.]{0,1}(.+)" %2)
             (conj %1 (apply ->PandaPackage (conj (re-matches #"(\w+)-(\w+\.\w+\.\w+)[-.]{0,1}(.+)" %2)
                                                  (string/join "/" [(first *repositories*) "jarman" %2])))))
          []
          (ftp-list-files repository)))

(defn preproces-from-path[repository]
  (map #(apply ->PandaPackage (conj (re-matches #"(\w+)-(\w+\.\w+\.\w+)[-.]{0,1}(.+)" (.getName %)) (str %))) (path-list-files repository)))


(defn get-all-packages [repositories]
  (letfn [(get-pkg [url] (let [ftp?  (every-pred is-url? is-url-allowed? is-url-repository?)
                               path? (every-pred is-path?)] 
                           (cond
                               (ftp? url)  (preproces-from-url  url)
                               (path? url) (preproces-from-path url)
                               :else nil)))]
    (mapcat get-pkg repositories)))

(defn download-package [^PandaPackage package]
  (let [ftp? (every-pred is-url? is-url-allowed? is-url-repository?)
       path? (every-pred is-path?)] 
   (cond
     (ftp? (:uri package))  (ftp-directly-get  (:uri package))
     (path? (:uri package)) (path-directly-get (:uri package))
     :else nil)))

(max-version (get-all-packages *repositories*))
(update-project #jarman.update_manager.PandaPackage{:file "jarman-1.0.4.zip", :name "jarman", :version "1.0.4", :artifacts "zip", :uri "ftp://jarman:bliatdoit@192.168.1.69//jarman/jarman-1.0.4.zip"})


(defn update-project [^PandaPackage package]
  (let [unzip-folder (gensym "transact")
        unzip-config-folder (string/join java.io.File/separator [unzip-folder "config"])] 
    (if (download-package package)
      (try
        (do (dv/unzip (:file package) unzip-folder)
            (fs/config-copy-dir "config" unzip-config-folder)
            ;; (fs/config-copy-dir unzip-folder gfs/*cwd*)
            )
        (catch java.io.IOException e)))
    (println unzip-folder unzip-config-folder)))


;; (unzp "ftp://jarman:bliatdoit@192.168.1.69//jarman/jarman-1.0.4.zip" "kupa.zip")
;; (unzip "tst/kupa.zip" "tst/kkk")

;; (copy-dir)
