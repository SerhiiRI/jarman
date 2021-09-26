(ns jarman.tools.org)
(require '[clojure.pprint  :refer [cl-format]])
(require '[clojure.string  :refer [split]])
(require '[clojure.java.io :as io])

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
(def updt (create-log-file "update.log.org" "Update log"))
(def extn (create-log-file "extension.log.org" "Extension log"))
(def app  (create-log-file "app.log.org" "App log"))
(def repl (agent *out*))
(def ^:dynamic *out-writers* #{repl app})

(defn close []
  (doall (map (fn [out-agent] (send out-agent (fn [wrt] (.close wrt)))) *out-writers*)))

(defmacro out-repl [& body]
  `(binding [jarman.tools.org/*out-writers* (conj jarman.tools.org/*out-writers* repl)]
     ~@body))

(defmacro out-update [& body]
  `(binding [jarman.tools.org/*out-writers* (conj jarman.tools.org/*out-writers* updt)]
     ~@body))

(defmacro out-extension [& body]
  `(binding [jarman.tools.org/*out-writers* (conj jarman.tools.org/*out-writers* extn)]
     ~@body))

(defn- out [msg]
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
  (binding [jarman.tools.org/*level* (inc jarman.tools.org/*level*)]
    (doall
     (for [line (split-newline s)]
       (out-line line))))
  (out-line "#+end_example"))

(defn out-src [lang s]
  (out-line (cl-format nil "#+begin_src ~A" (str lang)))
  (binding [jarman.tools.org/*level* (inc jarman.tools.org/*level*)]
    (doall
     (for [line (split-newline s)]
       (out-line line))))
  (out-line "#+end_src"))

;;;;;;;;;;;;;;;;;
;;; FUNCTIONS ;;;
;;;;;;;;;;;;;;;;;

(defmacro print-header [header & body]
  `(binding [jarman.tools.org/*level* (inc jarman.tools.org/*level*)]
     (if (= *level* 1) (println))
     (jarman.tools.org/out-header ~header)
     (do ~@body)))

(defmacro print-line [s]
  `(jarman.tools.org/out-line ~s))

(defmacro print-example [s]
  `(jarman.tools.org/out-example ~s))

(defmacro print-src [lang s]
  `(jarman.tools.org/out-src ~lang ~s))

;;;;;;;;;;;;;;;;;;;;
;;; CODE EXAMPLE ;;;
;;;;;;;;;;;;;;;;;;;;

(comment
  (do(print-header
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
  )


