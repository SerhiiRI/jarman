(ns jarman.logic.composite-components
  (:require [miner.ftp :as ftp]
            [jarman.logic.session :refer [get-user-configuration]]))

;;;;;;;;;;;;;;;;;;;;;
;;; FTP FUNCTIONS ;;;
;;;;;;;;;;;;;;;;;;;;;

(defn- copy [uri file]
  (with-open [in (clojure.java.io/input-stream uri)
              out (clojure.java.io/output-stream file)]
    (clojure.java.io/copy in out)))

(defn- ftp-list-files [repo-url]
  (ftp/with-ftp [client repo-url]
    (ftp/client-cd client "jarman")
    (ftp/client-all-names client)))

(defn- ftp-put-file [ftp-repo-url repo-path file-path ] 
  (ftp/with-ftp [client ftp-repo-url]
    (ftp/client-cd client repo-path)))

(defn- ftp-get-file
  ([repo-url file-name]
   (do (copy (clojure.string/join "/" [repo-url "jarman" file-name]) file-name))))

;;(ftp-get-file "ftp://jarman:dupa@trashpanda-team.ddns.net" "jarman.txt")
;;(.exists (clojure.java.io/file "/home/julia/test.png"))
;;(ftp-list-files "ftp://jarman:dupa@trashpanda-team.ddns.net")
;; (ftp-put-file "ftp://jarman:dupa@trashpanda-team.ddns.net"
;;               "/db/user/"
;;               "/home/julia/test.png")
;;show all files in directory
;;download file
;;upload file
;;;;;;;;;;;;;;;;;;;;;

;;;;;;;;;;;;;;;
;;; FtpFile ;;;
;;;;;;;;;;;;;;;

(defprotocol IStorage
  (load-session-config [this]))

(defprotocol IFtpQuery
  (get-list-files [this])
  (get-file-ftp [this])
  (put-file-ftp [this]))

(defrecord FtpFile [login password file-name file-path]
  IStorage
  (load-session-config [this]
    (let [{:keys [login, password, host]}
          (get-in (get-user-configuration) [:ftp] {})]
      (format "ftp://%s:%s@%s" login, password, host)))
  IFtpQuery
  (get-list-files [this]
    (ftp-list-files (load-session-config this)))
  (get-file-ftp [this]
    (ftp-get-file (load-session-config this) file-name))
  (put-file-ftp [this]
    (ftp-put-file (load-session-config this) "/bd/user"  file-path)))

(defn isFtpFile? [^jarman.logic.composite_components.FtpFile e]
  (instance? jarman.logic.composite_components.FtpFile e))

(defn ftp-file [login password file-name file-path]
  {:pre [(every? string? (list login password file-name file-path))]}
  (->FtpFile login password file-name file-path))

;;(.get-list-files (ftp-file "" "" "" ""))

;;ftp://jarman:dupa@trashpanda-team.ddns.net

;;;;;;;;;;;;
;;; Link ;;;
;;;;;;;;;;;;

;; (clojure.java.browse/browse-url "http://clojuredocs.org")
(defrecord Link [text link])
;; => jarman.plugin.agr_col_test.Link
(defn isLink? [^jarman.logic.composite_components.Link e]
  (instance? jarman.logic.composite_components.Link e))

;;;;;;;;;;;;
;;; File ;;;
;;;;;;;;;;;;

(defrecord File [file-name file])
(defn isFile? [^jarman.logic.composite_components.File e]
  (instance? jarman.logic.composite_components.File e))

(comment
  ;; test segment for some link
  (def --test-link (plugin-link [:user :table :user]))
  (.return-path --test-link)
  (.return-entry --test-link)
  (.return-permission --test-link)
  (.return-title --test-link)
  (.return-config --test-link)
  (.exists? --test-link))
