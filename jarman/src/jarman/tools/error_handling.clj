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
  ;; (throw (IOException. "fuck"))
  ;; Clojure Error
  (throw (ex-info "suak" {:a :2}))
  ;; -----------------------
  (catch clojure.lang.ExceptionInfo e
    ;; (print-error-clojure e)
    ;; (.printStackTrace e)
    (instance? clojure.lang.ExceptionInfo e)
    )
  (catch Exception e
    ;; (print-error-jvm e)
    ;; (print-error-jvm e)
    ;; (.printStackTrace e)
    ))
;; => clojure.lang.ExceptionInfo

(defn print-error-clojure [^clojure.lang.ExceptionInfo e]
  (print-header
     (format "ERROR %s (%s)" (.getMessage e) (quick-timestamp))
     (print-line "Ex-info:")
     (print-multiline
      (cl-format nil "~{~{~A~^ - ~} ~%~}" (seq (ex-data e))))
     (print-line "Stack trace:")
     (print-example
      (with-out-str (clojure.stacktrace/print-stack-trace e 5)))))

(defn print-error-jvm [^Exception e]
  (print-header
   (format "ERROR %s (%s)" (.getMessage e) (quick-timestamp))
   (print-line "Stack trace:")
   (print-example
    (with-out-str (clojure.stacktrace/print-stack-trace e 5)))))



(print-multiline (cl-format nil "~{~{~A~^ - ~} ~%~}" (seq {:a 2  :b 2})))
