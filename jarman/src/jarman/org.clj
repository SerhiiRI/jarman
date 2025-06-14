(ns jarman.org)
(require '[clojure.pprint  :refer [cl-format]])
(require '[clojure.string  :refer [split]])
(require '[clojure.java.io :as io])
(require '[jarman.config.environment :as env])
(require '[clojure.stacktrace])
(import '(clojure.lang ExceptionInfo))

;;;;;;;;;;;;;;;
;;; HELPERS ;;;
;;;;;;;;;;;;;;;

(defn quick-timestamp []
  (.format (java.text.SimpleDateFormat. "YYYY-MM-dd HH:mm:ss SSS") (java.util.Date.)))

;;;;;;;;;;;;;;;;;;;
;;; LOG STREAMS ;;;
;;;;;;;;;;;;;;;;;;;

(defn- create-log-file [log-file log-title]
  (let [log-file-f (do
                     (.mkdir (io/file "log"))
                     (io/file "log" log-file))]
   (do (spit log-file-f  (format "# -*- mode: org; mode: auto-revert; -*-
#+TITLE: %s
#+STARTUP: overview\n
" log-title))
       (agent (clojure.java.io/writer log-file-f  :append true)))))

(def ^{:dynamic true :private false} *level* 0)
;; (def updt (agent (clojure.java.io/writer "update.log.org" :append true)))
;; (def extn (agent (clojure.java.io/writer "extension.log.org" :append true)))
;; (def app  (agent (clojure.java.io/writer "app.log.org" :append true)))
(def updt (create-log-file env/update-log-org     "Update log"))
(def extn (create-log-file env/extension-log-org  "Extension log"))
(def app  (create-log-file env/app-log-org        "App log"))
;; (def repl (agent *out*))
(def ^:dynamic *out-writers* #{app})

(defn close []
  (doall (map (fn [out-agent] (send out-agent (fn [wrt] (.close wrt)))) *out-writers*)))

(defmacro out-repl [& body]
  `(binding [jarman.org/*out-writers* (conj jarman.org/*out-writers* repl)
             *level* 0]
     ~@body))

(defmacro out-update [& body]
  `(binding [jarman.org/*out-writers* (conj jarman.org/*out-writers* updt)
             *level* 0]
     ~@body))

(defmacro out-extension [& body]
  `(binding [jarman.org/*out-writers* (conj jarman.org/*out-writers* extn)
             *level* 0]
     ~@body))

(defn- out [msg]
  ;; (.print System/out msg)
  (print msg) (flush)
  (doall
   (for [out-agent (seq *out-writers*)]
     (send-off out-agent
               (fn [wrt]
                 (do (.write wrt msg)
                     (.flush wrt))
                 wrt))))
  nil)

;;;;;;;;;;;
;;; ORG ;;;
;;;;;;;;;;;

(defn split-newline [s]
  (split s #"(\r?\n)"))

(defn out-header [s]
  (if (< 0 *level*)
    (out (cl-format nil "~v{~A~:*~} ~A ~%" *level* "*" s))
    (out (cl-format nil "~A ~%" s))))

(defn out-line [s]
  (out (cl-format nil "~,,v<~A~> ~%" *level* s)))

(defn out-example [s]
  (out-line "#+begin_example")
  (binding [jarman.org/*level* (inc jarman.org/*level*)]
    (doall
     (for [line (split-newline s)]
       (out-line line))))
  (out-line "#+end_example"))

(defn out-src [lang s]
  (out-line (cl-format nil "#+begin_src ~A" (str lang)))
  (binding [jarman.org/*level* (inc jarman.org/*level*)]
    (doall
     (for [line (split-newline s)]
       (out-line line))))
  (out-line "#+end_src"))

(defn out-multiline [s]
  (doall
   (for [line (split-newline s)]
     (jarman.org/out-line line))))

;;;;;;;;;;;;;;;;;
;;; FUNCTIONS ;;;
;;;;;;;;;;;;;;;;;

(defmacro print-header [header & body]
  `(binding [jarman.org/*level* (inc jarman.org/*level*)]
     ;; (if (= *level* 1) (println))
     (jarman.org/out-header ~header)
     (do ~@body)))

(defmacro print-line
  ([] `(jarman.org/out-line ""))
  ([s] `(jarman.org/out-line ~s)))

(defmacro print-example [s]
  `(jarman.org/out-example ~s))

(defmacro print-src [lang s]
  `(jarman.org/out-src ~lang ~s))

(defmacro print-multiline [s]
  `(jarman.org/out-multiline ~s))

;;;;;;;;;;;;;;;;;;;;;
;;; ERROR MESSAGE ;;;
;;;;;;;;;;;;;;;;;;;;;

(defmulti print-error (fn [exception] (class exception)))
(defmethod print-error String [e]
  (print-line (format "ERROR (%s) %s" (quick-timestamp) e)))
(defmethod print-error ExceptionInfo [e]
  (print-header
   (format "ERROR %s (%s)" (.getMessage e) (quick-timestamp))
   (print-line "Ex-info:")
   (print-multiline
    (cl-format nil "~{~{~A~^ - ~} ~%~}" (seq (ex-data e))))
   (print-line "Stack trace:")
   (print-example
    (with-out-str (clojure.stacktrace/print-stack-trace e 10)))))
(defmethod print-error :default [e]
  (print-header
   (format "ERROR %s (%s)" (.getMessage e) (quick-timestamp))
   (print-line "Stack trace:")
   (print-example
    (with-out-str (clojure.stacktrace/print-stack-trace e 25)))))

;;;;;;;;;;;;;;;;;;;;
;;; CODE EXAMPLE ;;;
;;;;;;;;;;;;;;;;;;;;

(comment
  ;; SIMPLE PRINTING
  (do
    (print-header
      "main thread"
      (let [some 1]
        (print-line "Some content")
        (print-line "another content")
        (print-example (slurp "src/jarman/faces.clj"))
        (print-line "some end text"))
      (print-header
       "second level"
       (print-header
        "third level"
        (print-line "Another code example")
        (print-src "clojure" (slurp "src/jarman/faces.clj"))
        (print-line "The end"))))
     (out-extension
      (print-header
       "main thread"
       (let [some 1]
         (print-line "Some content")
         (print-line "another content")
         (print-example (slurp "src/jarman/faces.clj"))
         (print-line "some end text"))
       (out-update
        (print-header
         "second level"
         (print-header
          "third level"
          (print-line "Another code example")
          (print-src "clojure" (slurp "src/jarman/faces.clj"))
          (print-line "The end")))))))

  ;; ERROR PRINTING
  (try
    ;; JVM Error
    ;; (throw (IOException. "fuck"))
    ;; Clojure Error
    (throw (ex-info "suak" {:a :2}))
    ;; -----------------------
    (catch clojure.lang.ExceptionInfo e
      {:message (.getMessage e)
       :data (ex-data e)
       :stack (with-out-str (clojure.stacktrace/print-stack-trace e 10))})
    (catch Exception e
      {:message (.getMessage e)
       :stacktrace (with-out-str (clojure.stacktrace/print-stack-trace e 10))}))

  (try
    ;; JVM Error
    (throw (IOException. "fuck"))
    ;; Clojure Error
    (throw (ex-info "suak" {:a :2}))
    ;; -----------------------
    (catch clojure.lang.ExceptionInfo e (print-error e))
    (catch Exception e (print-error e))))

