(ns jarman.logic.composite-components
  (:require [miner.ftp :as ftp]
            [me.raynes.fs :as fs]
            [jarman.logic.connection :as db]
            [jarman.logic.sql-tool :refer [show-tables]]
            [jarman.logic.session :as session]
            [jarman.logic.document-manager :refer [update-blob! select-blob!]]))

;;;;;;;;;;;;;;;;;;;;;
;;; FTP FUNCTIONS ;;;
;;;;;;;;;;;;;;;;;;;;;
(defn get-configuration []
  (let [conf (:ftp (.config (.get-user (session/session))))]
    (if (empty? conf)
      "ftp://jarman:dupa@trashpanda-team.ddns.net"
      (str "ftp://" (:login conf) ":" (:password conf) "@" (:host conf) ))))

(defn- copy [uri file]
  (with-open [in (clojure.java.io/input-stream uri)
              out (clojure.java.io/output-stream file)]
    (clojure.java.io/copy in out)))

(defn- ftp-list-files [repo-url path]
  (ftp/with-ftp [client repo-url]
    (ftp/client-cd client path)
    (ftp/client-all-names client)))

(defn- ftp-delete-file [repo-url path]
  (ftp/with-ftp [client repo-url]
    (ftp/client-delete client path)))

(defn- ftp-put-file [ftp-repo-url repo-path file-path ] 
  (ftp/with-ftp [client ftp-repo-url]
    (ftp/client-cd client repo-path)
    (ftp/client-put client file-path)))

(defn- ftp-mkdir [repo-url dir-path]
  "Description
     create one dir
  Example
     (ftp-mkdir \"ftp://jarman:dupa@trashpanda-team.ddns.net\" \"/test)
  "
  (ftp/with-ftp [client repo-url]
    (ftp/client-mkdir client dir-path)))

(defn- ftp-mkdirs [repo-url dir-path]
  "Description
     create dirs with path
  Example
     for create dir tables, this func also created dir data 
     (ftp-mkdirs \"ftp://jarman:dupa@trashpanda-team.ddns.net\" \"/jarman/data/tables\")
  "
  (ftp/with-ftp [client repo-url]
    (ftp/client-mkdirs client dir-path)))

(defn- ftp-rmdir [repo-url path]
  (ftp/with-ftp [client repo-url]
    (.removeDirectory client path)))

(defn- ftp-rmdirs [repo-url subpath]
  (doseq [d (reductions (fn [path item] (str path (java.io.File/separator) item)) (fs/split subpath))]
    (ftp-rmdir repo-url d)))

(defn ftp-save-file [repo-url file-name local-file]
  (ftp/with-ftp [client repo-url]
    (ftp/retrieve-file repo-url file-name local-file)))

(defn- ftp-dirs-for-tables
  "Descriptiion
     create dirs for all tables from db
  Example
     (ftp-dirs-for-tables \"ftp://jarman:dupa@trashpanda-team.ddns.net\" \"/jarman/data/tables\")
  "
  [repo-url path]
  (map (fn [table] (ftp-mkdirs repo-url (str path  (java.io.File/separator) (first (vals table)))))
       (db/query (show-tables))))

;; for delete files/dirs in TERMINAL
;; prompt
;; mdel *
(comment
  (ftp-list-files "ftp://jarman:dupa@trashpanda-team.ddns.net" "/jarman/data/tables/seal")
  (ftp-save-file "ftp://jarman:dupa@trashpanda-team.ddns.net" "/jarman/data/tables/seal/test.txt" "/home/julia/test.txt")
  (ftp-put-file (get-configuration)
                (str "/jarman/data/tables/" "seal")
                "/home/julia/test.txt")
  (ftp-dirs-for-tables "ftp://jarman:dupa@trashpanda-team.ddns.net" "/jarman/data/tables")
  (ftp-mkdirs "ftp://jarman:dupa@trashpanda-team.ddns.net" "/jarman/data/tables")
  (ftp-get-file "ftp://jarman:dupa@trashpanda-team.ddns.net" "jarman.txt"))
;;(.exists (clojure.java.io/file "/home/julia/test.txt"))

;;;;;;;;;;;;;;;
;;; FtpFile ;;;
;;;;;;;;;;;;;;;
(defprotocol RemoteFileLoader
  (remove-data [this table_name])
  (download [this] [this attributes])
  (upload [this] [this attributes]))

(defprotocol IStorage
 (load-session-config [this]))

(defrecord FtpFile [file-name file-path]
  RemoteFileLoader
  (upload [this attributes]
    (update-blob!
     {:table_name      (:table_name attributes)
      :column-list     (:column-list attributes)
      :values          (conj {:id (:id attributes)}
                             (:values attributes))})
    (if-not (nil? file-path)
      (ftp-put-file (get-configuration)
                    (clojure.string/join (java.io.File/separator)
                                         ["jarman" "data" "tables" (:table_name attributes)])
                    file-path)))
  (remove-data [this table_name]
    (ftp-delete-file (get-configuration) (clojure.string/join (java.io.File/separator) ["jarman" "data" "tables" table_name file-name])))
  (download [this attributes]
    (ftp-save-file (get-configuration)
                   (clojure.string/join (java.io.File/separator)
                                        ["jarman" "data" "tables" (:table_name attributes) file-name])
                   (str (:local-path attributes) (java.io.File/separator) file-name)))
  
  ;; IStorage
  ;; (load-session-config [this]
  ;;   (let [{:keys [login, password, host]}
  ;;         (get-in (get-user-configuration) [:ftp] {})]
  ;;     (format "ftp://%s:%s@%s" login, password, host)))
  )

(defn isFtpFile? [^jarman.logic.composite_components.FtpFile e]
  (instance? jarman.logic.composite_components.FtpFile e))

(defn ftp-file [login password file-name file-path]
  {:pre [(every? string? (list login password file-name file-path))]}
  (->FtpFile login password file-name file-path))

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

(defn isLink? [^jarman.logic.composite_components.Link e]
  (instance? jarman.logic.composite_components.Link e))

;;;;;;;;;;;;
;;; File ;;;
;;;;;;;;;;;;
(defrecord File [file-name file]
  RemoteFileLoader
  (upload [this attributes]
    (update-blob!
     {:table_name      (:table_name attributes)
      :column-list     (:column-list attributes)
      :values          (conj {:id (:id attributes)}
                             (:values attributes))}))
  (download [this attributes]
    (select-blob! {:table_name (:table_name attributes)
                   :column-list [[:file :blob] [:file_name :string]]
                   :doc-column  [[:file {:document-name :file_name :document-place (:local-path attributes)}]]
                   :where [:= :id (keyword (str (:table_name attributes) ".id"))]})))

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


