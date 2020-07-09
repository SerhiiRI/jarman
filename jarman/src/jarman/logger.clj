(ns jarman.logger)

(defn- date []
  (.format (java.text.SimpleDateFormat. "[dd-MM-YYYY HH:mm:ss] ") (java.util.Date.)))

(defn LogWriter [writer]
  (proxy [java.io.PrintWriter] [^java.io.Writer writer]
    (^void write [string]
     (if-not (string? string)
       (proxy-super write string)
       (proxy-super write (if-not (= (System/getProperty "line.separator") string)
                            (apply str (concat (date) string)) string))))))

(defmacro dolog [& body]
  (if-not (.exists (clojure.java.io/file "logs"))
    (.mkdir (clojure.java.io/file "logs")))
  (let [logf (format "logs/log-%s.log"
                     (.format (java.text.SimpleDateFormat. "MM-YYYY")
                              (java.util.Date.)))]
    `(binding [*out* (jarman.logger/LogWriter (clojure.java.io/writer ~logf :append true))]
       ~@body)))


