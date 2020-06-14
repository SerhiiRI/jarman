(ns jarman.ftp-toolbox.clj
  (:require [clojure.java.io :as io]
            [miner.ftp :refer [with-ftp client-put client-mkdirs]]
            [miner.ftp :as ftp]))

;;;;;;;;;;;;;;;;;;;;;;
;;; PREDICATE LIST ;;;
;;;;;;;;;;;;;;;;;;;;;;
(def ^:dynamic *ftp-destination-folder* "/tpanda-ftp/hrtime")
(def ^:dynamic *ftp-protocol* "ftp")
(def ^:dynamic *ftp-login* "aleks")
(def ^:dynamic *ftp-password* "123")
(def ^:dynamic *ftp-host* "trashpanda-team.ddns.net")
(def ^:dynamic *program* "jarman")

(defn construct-ftp-url []
  (format "%s://%s:%s@%s"
          *ftp-protocol*
          *ftp-login*
          *ftp-password*
          *ftp-host*))

(defn zip? [f]
  (let [f (if (string? f) (io/file f) f)]
    (= "zip" (last (clojure.string/split (.getName f) #"\.")))))
(defn package? [f]
  (let [f (if (string? f) (io/file f) f)]
    (and (re-matches #"(\w+)-{1}(\d\.\d\.\d).*" (.getName f)))))
(defn- file? [f]
  (.isFile f))
(defn- directory? [f]
  (.isDirectory f))
(defn- trim-path-separator
  "Trim path separator character from beginning and end of a string"
  [s](-> s
         (clojure.string/replace #"^/" "")
         (clojure.string/replace #"/$" "")))

(defn ftp-list-files [repo-url]
  (ftp/with-ftp [client repo-url]
    (ftp/client-cd client *program*)
    (ftp/client-all-names client)))

(defn copy [uri file]
  (with-open [in (io/input-stream uri)
              out (io/output-stream file)]
    (io/copy in out)))

(defn ftp-get-file
  ([file-url]
   (if-let [[url repo-url path-to-file] (re-matches #"(ftp://.+)/.+/{1}(.+)" file-url)]
     (do (copy url path-to-file)
         path-to-file)))
  ([repo-url file-name]
   (do (copy (clojure.string/join "/" [repo-url *program* file-name]) file-name) file-name)))

(defn ftp-put-file [ftp-repo-url & files]
  (if-not (empty? files)
    (ftp/with-ftp [client ftp-repo-url :file-type :binary]
      (ftp/client-cd client *program*)
      (reduce #(or %1 (ftp/client-put client %2)) false files))))

(ftp-put-file (construct-ftp-url)
              "target/some.txt")

(defn list-available-packages []
  (filter (fn [f] ((every-pred file? zip? package?) f)) (.listFiles (io/file "target"))))

(defn upload-packages []
  (apply #(ftp-put-file (construct-ftp-url) *ftp-destination-folder* %)
         (list-available-packages)))



