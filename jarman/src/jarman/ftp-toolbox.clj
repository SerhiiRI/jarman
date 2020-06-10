(ns jarman.ftp-toolbox.clj
  (:require [clojure.java.io :as io]
            [miner.ftp :refer [with-ftp client-put client-mkdirs]]
            [miner.ftp :as ftp]))

(make-remote-url {:protocol "ftp"})
(make-remote-url {})

(defmulti make-remote-url :protocol)
(defmethod make-remote-url "ftp" [url-data]
  (let [{:keys [protocol user host pass port]} url-data] 
   (str protocol "://" user ":" pass "@" host ":" port)))
(defmethod make-remote-url :default [{:keys [protocol]}]
  (println (str "Unknown protocol '" protocol "'" ))
  nil)

(defn zip? [f]
  (let [f (if (string? f) (io/file f) f)]
    (= "zip" (last (clojure.string/split (.getName f) #"\.")))))
(defn package? [f]
  (let [f (if (string? f) (io/file f) f)]
    (and (re-matches #"(\w+).(\d\.\d\.\d).*" (.getName f)))))
(defn file? [f]
  (.isFile f))
(defn directory? [f]
  (.isDirectory f))

(filter (fn [f] ((every-pred file? zip? package?) f)) (.listFiles (io/file "target")))

(defn trim-path-separator
  "Trim path separator character from beginning and end of a string"
  [s] (-> s (clojure.string/replace #"^/" "")
            (clojure.string/replace #"/$" "")))

(defn make-remote-path
  [remote-root remote-dir remote-file]
  (let [valid-root (str "/" (trim-path-separator remote-root))
        valid-dir (trim-path-separator remote-dir)
        valid-file (trim-path-separator remote-file)]
    (-> valid-root
        (io/file valid-dir valid-file)
        (.getPath))))

(defn ftp-put-file [ftp-repo-url repo-path file-path]
  (ftp/with-ftp [client ftp-repo-url :file-type :binary]
    (ftp/client-cd client repo-path)
    (println (ftp/client-pwd client))
    (ftp/client-put client file-path)))

(ftp-put-file "ftp://jarman:bliatdoit@192.168.1.69"
              "hrtime"
              (first (filter (fn [f] ((every-pred file? zip? package?) f)) (.listFiles (io/file "target")))))

;; (defn ftp-put-file [ftp-repo-url repo-path directory]
;;   (ftp/with-ftp [client ftp-repo-url :file-type :binary]
;;     (ftp/client-cd client repo-path)
;;     (ftp/client-put client directory)))

(defn upload-directory
  "Upload a whole directory (including its nested sub directories and files) to a FTP server"
  [url remote-root local-root remote-parent-path]
  (let [dirFiles (.listFiles (io/file local-root))]
    (doseq [f dirFiles]
      (let [remote-item (make-remote-path remote-root remote-parent-path (.getName f))]
        (if (.isFile f)
          (do
            (with-ftp [client url :file-type :binary]
              (client-put client f remote-item)
              (println (str "Put " (.getName f) " to " remote-item))))
          (do
            (with-ftp [client url]
              (client-mkdirs client remote-item))
            (upload-directory
              url
              remote-root
              (.getAbsolutePath f)
              (str remote-parent-path "/" (.getName f)))))))))

(defn ftp-static-deploy
  "Deploy a directory to a remote server folder via FTP"
  [project & args]
  (let [ftp (:ftp project)
        {:keys [host user password port ftp-static-deploy]} ftp
        {:keys [remote-root local-root]} ftp-static-deploy]
    (if-let [url (make-remote-url (assoc (select-keys ftp [:host :user :pass :port]) :protocol "ftp") )]
      (upload-directory url remote-root local-root "")
      (println (str "Bad remote server URL. Params: " ftp)))))
