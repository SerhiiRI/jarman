(ns jarman.config.storage
  (:gen-class)
  (:import (java.io IOException))
  (:require [clojure.string :as s]
            [jarman.tools.lang :refer :all]
            [jarman.config.environment :as env]
            [me.raynes.fs :as fs]
            [clojure.java.io :as io]))

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

;;; TEMPORARY STORAGE ;;;
(def ^:private tmp-path (where((tmp-file (io/file env/java-io-tmpdir ".jarman")))(if-not (.exists tmp-file) (.mkdir tmp-file)) tmp-file))
(defn temporary-list   []    (map str (-> tmp-path io/file .listFiles)))
(defn temporary-slurp  [f]   (ioerr (slurp (io/file tmp-path f))))
(defn temporary-spit   [f s] (ioerr (spit (io/file tmp-path f) s)))
(defn temporary-get    [f]   (ioerr (with-open [R (io/reader (io/file tmp-path f))](s/join env/line-separator (line-seq R)))))
(defn temporary-put    [f s] (ioerr (with-open [W (io/writer (io/file tmp-path f))] (.write W s)) identity))
(defn temporary-append [f s] (ioerr (with-open [W (io/writer (io/file tmp-path f) :append true)] (.write W s)) identity))
(defn temporary-delete [f]   (if (.exists (io/file tmp-path f))(.delete (io/file tmp-path f))))
(defn temporary-clean  []    (doall (map #(.delete %) (-> tmp-path io/file .listFiles))) true)
(defn temporary-rename [f-from f-to] (ioerr (.renameTo (io/file tmp-path f-from) (io/file tmp-path f-to)) identity))

;;; USER STORAGE ;;;
(def ^:private user-path (where ((path (io/file env/user-home ".jarman"))) (if-not (.exists path) (.mkdir path)) path))
(defn user-p      []    user-path)
(defn user-list   []    (map str (-> user-path io/file .listFiles)))
(defn user-slurp  [f]   (ioerr (slurp (io/file user-path f))))
(defn user-spit   [f s] (ioerr (spit (io/file user-path f) s)))
(defn user-get    [f]   (ioerr (with-open [R (io/reader (io/file user-path f))](s/join env/line-separator (line-seq R)))))
(defn user-put    [f s] (ioerr (with-open [W (io/writer (io/file user-path f))] (.write W s)) identity))
(defn user-append [f s] (ioerr (with-open [W (io/writer (io/file user-path f) :append true)] (.write W s)) identity))
(defn user-delete [f]   (if (.exists (io/file user-path f)) (.delete (io/file user-path f))))
(defn user-clean  []    (doall (map #(.delete %) (-> user-path io/file .listFiles))) true)
(defn user-rename [f-from f-to] (ioerr (.renameTo (io/file user-path f-from) (io/file user-path f-to)) identity))

;; (user-put "some.edn" "temporary")
;; (user-rename "chuj.ds" "some.edn")
;; (user-list)
;; (user-get "some.edn")
;; (user-delete "some.edn")
;; (user-list)
