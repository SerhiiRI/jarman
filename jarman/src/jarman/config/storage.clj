(ns jarman.config.storage
  (:gen-class)
  (:import (java.io IOException FileNotFoundException))
  (:require [clojure.string :as s]
            [clojure.java.io :as io]
            [me.raynes.fs :as fs]
            [jarman.tools.lang :refer :all]
            [jarman.config.environment :as env]))


(defn return-first-exists [file-list]
 (if-let [file (first (filter #(.exists %) file-list))]
   file
   (throw (FileNotFoundException.
           (format "No one file [%s] doesn't exists"
                   (clojure.string/join
                    ", " (map str file-list)))))))

;;; Function 
(defmacro ioerr
  ([f] `(ioerr ~f
               (fn [tmp#] nil)))
  ([f f-exception] `(ioerr ~f
                           ~f-exception
                           ~f-exception))
  ([f f-io-exception f-exception]
   `(try ~f
         (catch IOException e# (~f-io-exception (format "I/O error. Maybe problem in file: %s" (ex-message e#))))
         (catch Exception e# (~f-exception (format "Undefinied problem: %s" (ex-message e#)))))))

(defn- make-store-dir [-f-store-path] (fn [] -f-store-path))
(defn- make-store-dir-path [-f-store-path] (fn [] (.getAbsolutePath -f-store-path)))
(defn- make-store-dir-name [-f-store-path] (fn [] (.getName -f-store-path)))
(defn- make-store-list [-f-store-path] (fn [] (map str (-> -f-store-path io/file .listFiles))))
(defn- make-store-slurp [-f-store-path] (fn [f] (ioerr (slurp (io/file -f-store-path f)))))
(defn- make-store-spit [-f-store-path] (fn [f s] (ioerr (spit (io/file -f-store-path f) s))))
(defn- make-store-get [-f-store-path] (fn [f] (ioerr (with-open [R (io/reader (io/file -f-store-path f))] (s/join env/line-separator (line-seq R))))))
(defn- make-store-put [-f-store-path] (fn [f s] (ioerr (with-open [W (io/writer (io/file -f-store-path f))] (.write W s)) identity)))
(defn- make-store-append [-f-store-path] (fn [f s] (ioerr (with-open [W (io/writer (io/file -f-store-path f) :append true)] (.write W s)) identity)))
(defn- make-store-delete [-f-store-path] (fn [f] (if (.exists (io/file -f-store-path f)) (.delete (io/file -f-store-path f)))))
(defn- make-store-clean [-f-store-path] (fn [] (doall (map #(.delete %) (-> -f-store-path io/file .listFiles))) true))
(defn- make-store-rename [-f-store-path] (fn [f-from f-to] (ioerr (.renameTo (io/file -f-store-path f-from) (io/file -f-store-path f-to)) identity)))
(defmacro defstore [store-name path]
  `(let [store-path# (where ((path# (io/file ~path))) (if-not (.exists path#) (.mkdirs path#)) path#)]
     (def ~(symbol (str store-name "-dir")) (make-store-dir store-path#))
     (def ~(symbol (str store-name "-dir-path")) (make-store-dir-path store-path#))
     (def ~(symbol (str store-name "-dir-name")) (make-store-dir-name store-path#))
     (def ~(symbol (str store-name "-list")) (make-store-list store-path#))
     (def ~(symbol (str store-name "-slurp")) (make-store-slurp store-path#))
     (def ~(symbol (str store-name "-spit")) (make-store-spit store-path#))
     (def ~(symbol (str store-name "-get")) (make-store-get store-path#))
     (def ~(symbol (str store-name "-put")) (make-store-put store-path#))
     (def ~(symbol (str store-name "-append")) (make-store-append store-path#))
     (def ~(symbol (str store-name "-delete")) (make-store-delete store-path#))
     (def ~(symbol (str store-name "-clean")) (make-store-clean store-path#))
     (def ~(symbol (str store-name "-rename")) (make-store-rename store-path#))))

;; (defstore temporary            (io/file env/java-io-tmpdir  "jarman-tmp"))
(defstore document-templates   (io/file env/user-home       ".jarman.d" "dump" "documents"))
(defstore user-config          (io/file env/user-home       ".jarman.d" "backup" "config"))
(defstore user-metadata        (io/file env/user-home       ".jarman.d" "backup" "metadata"))

;;; Example usage
(comment
  ;; User file management 
  (user-put "some.edn" "temporary")
  (user-rename "chuj.ds" "some.edn")
  (user-list)
  (user-get "some.edn")
  (user-delete "some.edn")
  (user-list)
  (user-clean)
  ;; Some another exmaple  
  (user-config-list)
  (user-config-put "temporary.txt" "sukasuka")
  (user-config-append "temporary.txt" "\n one two three")
  (user-config-append "123.edn" "{}")
  (user-config-get "temporary.txt")
  (user-config-slurp "temporary.txt")
  (user-config-spit "temporary.txt" "123")
  (user-config-rename "temporary.txt" "123")
  (user-config-dir)
  (user-config-clean)
  ;; yet one
  (temporary-list)
  (temporary-put "temp.txt" "kill")
  (temporary-append "temp.txt" "suka")
  (temporary-get "temp.txt")
  (temporary-get "alalal")
  (temporary-clean)
  (temporary-rename "temp.txt" "alalal")
  (temporary-dir-path))


