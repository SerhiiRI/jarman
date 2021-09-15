(ns jarman.tools.org)
(require '[clojure.pprint :refer [cl-format]])
(require '[clojure.string :refer [split]])

(def ^{:dynamic true :private false} *level* 0)


(defn split-newline [s]
  (split s #"(\r?\n)"))

(defn out-header [s]
  (if (< 0 *level*)
    (cl-format *out* "~v{~A~:*~} ~A ~%" *level* "*" s)
    (cl-format *out* "~A ~%" s)))

(defn out-line [s]
  (cl-format *out* "~,,v<~A~> ~%" *level* s))

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
  (require '[jarman.tools.org :refer :all])
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
     (print-line "The end")))))


