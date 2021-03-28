(ns jarman.tools.ftp-toolbox
  (:require [clojure.java.io :as io]
            [miner.ftp :refer [with-ftp client-put client-mkdirs]]
            [miner.ftp :as ftp]))

;;;;;;;;;;;;;;;;;;;;;;
;;; PREDICATE LIST ;;;
;;;;;;;;;;;;;;;;;;;;;;
(def ^:dynamic *ftp-service-path* "/tpanda-ftp/service/logs" )
(def ^:dynamic *ftp-protocol* "ftp")
(def ^:dynamic *ftp-login* "aleks")
(def ^:dynamic *ftp-password* "123")
(def ^:dynamic *ftp-host* "trashpanda-team.ddns.net")
(def ^:dynamic *program-path* "/tpanda-ftp/jarman")
(def ^:dynamic *program* "jarman")

(defn construct-ftp-url []
  (format "%s://%s:%s@%s"
          *ftp-protocol*
          *ftp-login*
          *ftp-password*
          *ftp-host*))

;; (file? (clojure.java.io/file "logs\\log-06-2020.log"))
;; (log? (clojure.java.io/file "logs\\log-06-2020.log"))
;; (program-log? (clojure.java.io/file "logs\\log-06-2020.log"))
;; (with-pattern? #"log-(\d{1,2}-\d{4}).*" (clojure.java.io/file "logs\\log-06-2020.log"))

(defn extension? [extension f]
  (let [f (if (string? f) (io/file f) f)]
    (= extension (last (clojure.string/split (.getName f) #"\.")))))
(defn with-pattern? [pattern f]
  (let [f (if (string? f) (io/file f) f)]
    (and (re-matches pattern (.getName f)))))
(def zip?
  (partial extension? "zip"))
(def log?
  (partial extension? "log"))
(def package? 
  (partial with-pattern? #"(\w+)-{1}(\d\.\d\.\d).*"))
(def program-log?
  (partial with-pattern? #"log-(\d{1,2}-\d{4}).*"))
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
    (ftp/client-cd client *program-path*)
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

(defn list-available-packages []
  (filter (fn [f] ((every-pred file? zip? package?) f)) (.listFiles (io/file "target"))))

(defn list-available-logs []
  (filter (fn [f] ((every-pred file? log? program-log?) f)) (.listFiles (io/file "logs"))))


(defn ftp-put-on-server [server-path ftp-repo-url & files]
  (if-not (empty? files)
    (ftp/with-ftp [client ftp-repo-url :file-type :binary]
      (ftp/client-cd client server-path)
      (reduce #(or %1 (ftp/client-put client %2)) false files))))

(def ftp-put-file (partial ftp-put-on-server *program-path*))
(def ftp-put-log  (partial ftp-put-on-server *ftp-service-path*))


