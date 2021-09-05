(ns jarman.logic.composite-components
  (:require [miner.ftp :as ftp]
            [jarman.logic.session :refer [get-user-configuration]]
            [jarman.logic.document-manager :refer [update-blob!]]))

;;;;;;;;;;;;;;;;;;;;;
;;; FTP FUNCTIONS ;;;
;;;;;;;;;;;;;;;;;;;;;
(defn get-configuration []
  (let [conf (:ftp (get-user-configuration))]
    (str "ftp://" (:login conf) ":" (:password conf) "@" (:host conf) )))

(defn- copy [uri file]
  (with-open [in (clojure.java.io/input-stream uri)
              out (clojure.java.io/output-stream file)]
    (clojure.java.io/copy in out)))

(defn- ftp-list-files [repo-url path]
  (ftp/with-ftp [client repo-url]
    (ftp/client-cd client path)
    (ftp/client-all-names client)))

(defn- ftp-put-file [ftp-repo-url repo-path file-path ] 
  (ftp/with-ftp [client ftp-repo-url]
    (ftp/client-cd client repo-path)
    (ftp/client-put client file-path)))

(defn- ftp-get-file
  ([repo-url file-name]
   (do (copy (clojure.string/join "/" [repo-url "jarman" file-name]) file-name))))


;;(ftp-get-file "ftp://jarman:dupa@trashpanda-team.ddns.net" "jarman.txt")

(.exists (clojure.java.io/file "/home/julia/test.txt"))
(ftp-list-files "ftp://jarman:dupa@trashpanda-team.ddns.net" "jarman")
(ftp-list-files "ftp://jarman:dupa@trashpanda-team.ddns.net" "/")
(ftp-put-file "ftp://jarman:dupa@trashpanda-team.ddns.net"
               "/test"
              "/home/julia/test.txt")
;;show all files in directory
;;download file
;;upload file
;;;;;;;;;;;;;;;;;;;;;

;;;;;;;;;;;;;;;
;;; FtpFile ;;;
;;;;;;;;;;;;;;;

(defprotocol RemoteFileLoader
  (download [this] [this attributes])
  (upload [this] [this attributes]))

(defprotocol IStorage
  (load-session-config [this]))

(defprotocol IFtpQuery
  (get-list-files [this])
  (get-file-ftp [this])
  (put-file-ftp [this]))

(defn update-file [attributes]
  (println "HEYYYY")
  (update-blob!
   {:table_name      (:table_name attributes)
    :column-list     (:column-list attributes)
    :values          (conj {:id (:id attributes)}
                           (:values attributes))}))

(defrecord FtpFile [login password file-name file-path]
  RemoteFileLoader
  (upload [this attributes]
    (update-blob!
     {:table_name      (:table_name attributes)
      :column-list     (:column-list attributes)
      :values          (conj {:id (:id attributes)}
                             (:values attributes))})
    (ftp-put-file (get-configuration)
                  "/jarman"
                  "/home/julia/test.txt"))
  
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
(defrecord Link [text link]
  RemoteFileLoader
  (upload [this attributes]
    (update-blob!
     {:table_name      (:table_name attributes)
      :column-list     (:column-list attributes)
      :values          (conj {:id (:id attributes)}
                             (:values attributes))})))
;; => jarman.plugin.agr_col_test.Link
(defn isLink? [^jarman.logic.composite_components.Link e]
  (instance? jarman.logic.composite_components.Link e))

;;;;;;;;;;;;
;;; File ;;;
;;;;;;;;;;;;

;; (defprotocol SqlReq
;;   (insert-file [this])
;;   (update-file [this]))

 ;; (update-blob!
 ;;     {:table_name      :seal
 ;;      :column-list     [[:file :blob]]
 ;;      :values          {:id   31,
 ;;                        :file "/home/julia/test.txt"}})

(defrecord File [file-name file]
  RemoteFileLoader
  (upload [this attributes]
    (update-blob!
     {:table_name      (:table_name attributes)
      :column-list     (:column-list attributes)
      :values          (conj {:id (:id attributes)}
                             (:values attributes))})))

 ;; (update-blob! {:table_name :seal
 ;;                :column-list [[:file :blob]]
 ;;                :values {:id 31
 ;;                         :file "/home/julia/test.txt"}})

(defn isFile? [^jarman.logic.composite_components.File e]
  (instance? jarman.logic.composite_components.File e))

(def component-list [isFile? isLink? isFtpFile?])
(def component-files [isFile? isFtpFile?])

(defn isComponent? [val]
  (some #(% val) component-list))
(defn isComponentFiles? [val]
  (some #(% val) component-files))

(comment
  ;; test segment for some link
  (def --test-link (plugin-link [:user :table :user]))
  (.return-path --test-link)
  (.return-entry --test-link)
  (.return-permission --test-link)
  (.return-title --test-link)
  (.return-config --test-link)
  (.exists? --test-link))
