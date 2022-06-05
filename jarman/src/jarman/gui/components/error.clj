;;  _____ ____  ____   ___  ____  _ ____ 
;; | ____|  _ \|  _ \ / _ \|  _ \( ) ___|
;; |  _| | |_) | |_) | | | | |_) |/\___ \
;; | |___|  _ <|  _ <| |_| |  _ <   ___) |
;; |_____|_| \_\_| \_\\___/|_| \_\ |____/

(ns jarman.gui.components.error
  (:require
   [clojure.pprint :refer [cl-format]]
   [jarman.lang :refer :all]
   [jarman.org :refer :all]
   ;; gui components 
   [jarman.gui.components.panels :as panels]
   [jarman.gui.components.common :as common])
  (:import [clojure.lang ExceptionInfo]))

(defmacro catch-error-panel [& body]
  `(try (do
          ~@body)
        (catch ExceptionInfo e#
          (jarman.org/print-error e#)
          (panels/vertical-panel
           :items
           [(common/textarea :value (cl-format nil "ExceptionInfo:~A~%Timestamp:~A~%" (.getMessage e#) (quick-timestamp)))
            (common/textarea :value (cl-format nil "Map Info:~%~{~{ ~A~^ - ~} ~%~}" (seq (ex-data e#))))
            (common/textarea :value (with-out-str (clojure.stacktrace/print-stack-trace e# 20)))]))
        (catch Exception e#
          (jarman.org/print-error e#)
          (panels/vertical-panel
           :items
           [(common/textarea :value (cl-format nil "ExceptionInfo:~A~%Timestamp:~A~%" (.getMessage e#) (quick-timestamp)))
            (common/textarea :value (with-out-str (clojure.stacktrace/print-stack-trace e# 30)))]))))

;;  ____  _____ __  __  ___ 
;; |  _ \| ____|  \/  |/ _ \
;; | | | |  _| | |\/| | | | |
;; | |_| | |___| |  | | |_| |
;; |____/|_____|_|  |_|\___/

(comment
  (require '[jarman.gui.components.swing :as swing])
  
  (swing/quick-frame
   [(catch-error-panel
     (throw (Exception. "sudf")))])

  (swing/quick-frame
   [(catch-error-panel
     (throw (ex-info "sudf" {:suka :bliat})))]))

