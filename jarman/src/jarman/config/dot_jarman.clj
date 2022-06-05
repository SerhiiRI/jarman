(ns jarman.config.dot-jarman
  (:gen-class)
  (:import (java.io IOException FileNotFoundException))
  (:require
   ;; Clojure
   [clojure.string  :as s]
   [clojure.java.io :as io]
   ;; Tools
   [seesaw.core]
   ;; Jarman
   [jarman.variables]
   [jarman.lang :refer :all]
   [jarman.org  :refer :all]
   [jarman.config.environment :as env]
   [jarman.config.vars :refer [setq defvar print-list-not-loaded variable-config-list]]))

;; (def jarman ".jarman")
;; (def jarman-paths-list [(io/file env/user-home jarman)
;;                         (io/file "." jarman)])

;;;;;;;;;;;;;;;
;;; HELPERS ;;;
;;;;;;;;;;;;;;;

(defmacro ioerr
  "Wrap any I/O action to try-catch block
    f              - function which must be wrapped 
    f-io-exception - on IOException one-arg lambda 
    f-exception    - on Exception one-arg lambda"
  ([f]             `(ioerr ~f (fn [tmp#] nil)))
  ([f f-exception] `(ioerr ~f ~f-exception ~f-exception))
  ([f f-io-exception f-exception]
   `(try ~f
         (catch IOException e# (~f-io-exception (format "I/O error. Maybe problem in file: %s" (ex-message e#))))
         (catch Exception   e# (~f-exception (format "Undefinied problem: %s" (ex-message e#)))))))

(defn fput [f s] (ioerr (with-open [W (io/writer f)] (.write W s))))
(defn fappend [f s] (ioerr (with-open [W (io/writer f :append true)] (.write W s))))

;;;;;;;;;;;;;;;;;;;;;;;
;;; DECLARATION ENV ;;;
;;;;;;;;;;;;;;;;;;;;;;;
(defn- create-empty-dot-jarman [f]
  (if-not (not (.exists f))
    (do (fput f ";; -*- mode: Clojure; -*- \n")
        (fappend f ";; This is Main configuration `.jarman` file. Declare in it any machine or app-specyfic configurations\n")
        (fappend f ";; Set global variables, by using `setq` macro and power of jarman customizing system\n")
        (fappend f "\n")
        (doall
         (map (comp (partial fappend f) (partial str ";;"))
              [" 1. Declare variable in any place you want in system\n"
               "    Attribute `:type` and `:group` aren't required, but\n"
               "    specifing may help you in debug moment\n;;\n"
               "    => (defvar some-global-variable nil \n;;          :type clojure.lang.PersistentArrayMap\n;;          :group :logical-group)\n"
               "\n"
               " 2. In `.jarman` set previosly declared in code variable\n;;\n"
               "    => (setq some-global-variable {:some-value {:a 1}})\n"]))
        (fappend f "\n"))))

;; (defn dot-jarman-load []
;;   (if-let [file (first (filter #(.exists %) jarman-paths-list))]
;;     (try (load-file (str file))
;;          (catch Exception e (println (.getMessage e))))
;;     (throw (FileNotFoundException.
;;             (format "No one file [%s] doesn't exists"
;;                     (clojure.string/join
;;                      ", " (map str jarman-paths-list)))))))

(defn dot-jarman-load []  
  (try 
    (let [file (env/get-jarman)]
      (print-line (format "evaluation of '%s'" (str file)))
      (binding [*ns* (find-ns 'jarman.config.dot-jarman)]
        (load-file (str file))
        (print-line (format "file '%s' was be loaded" (str file))))
      true)
    (catch FileNotFoundException e
      (seesaw.core/alert e (.getMessage e)) ;; (java.lang.System/exit 0)
      )
    (catch Exception e
      (seesaw.core/alert (with-out-str (clojure.stacktrace/print-stack-trace e 20))) ;; (java.lang.System/exit 0)
      )))


(comment (dot-jarman-load))

