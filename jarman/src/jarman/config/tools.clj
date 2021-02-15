(ns jarman.config.tools
  (:require [jarman.tools.lang :refer :all]))

(def ^:private offset-char "| ")
(defn- block? [m] (= (:type m) :block))
(defn- file? [m] (= (:type m) :file))
(defn- directory? [m] (= (:type m) :directory))
(defn- error? [m] (= (:type m) :error))
(defn- param? [m]  (= (:type m) :param))
(defn- is-block? [m] (or (block? m) (file? m) (directory? m)))
(defn- is-param? [m] (param? m))
(defmacro ^:private prc [& body]
  `(print (apply str (concat ~@body))))

(defn- clformat [s-form]
  (partial clojure.pprint/cl-format nil s-form))

(defn- printblock
  ([m p] (printblock 0 m p))
  ([offset m path]
   (prc (repeat offset offset-char)
        (apply (clformat "~#[EMPTY~;~a~;~a(~a)~]\n")
               (if (:name m)
                 [(str "+"(name(last path)))(:name m)]
                 [(str "+"(name(last path)))(last path)])))))

(defn- printparam
  ([m p] (printparam 0 m p))
  ([offset m path]
   (prc (repeat offset offset-char)
        (apply (clformat "~#[EMPTY~;~a~;~a -- '~a'~;~a(~a) -- '~a'~]\n")
               (if (:name m)
                 [(name(last path))(:name m)(:value m)]
                 [(name(last path))(:value m)])))))

(defn- build-part-of-map [level [header tail] path]
  (if (some? header)
    ;; for header {:file.edn {....}} we  
    (let 
     [k-header ((comp first first) header)
      header ((comp second first) header)
      path (conj path k-header)]
      (cond

        ;; if Map represent Block or File
        (is-block? header)
        (do (printblock level header path)
            (build-part-of-map (inc level) (map-destruct (:value header)) path))

        ;; fi Map represent Parameters
        (is-param? header) (printparam level header path))))
  ;; Do recursive for Tail destruction in the same level
  (if (some? tail) (build-part-of-map level  (map-destruct tail) path)))

(defn debug-config [m]
  (build-part-of-map 0 m []))

(defn recur-walk-throw [m-config f path]
  (let [[header tail] (map-destruct m-config)]
    (if (some? header)
      ;; for header {:file.edn {....}} we  
      (let [k-header ((comp first first) header)
            header ((comp second first) header)
            path (conj path k-header)]
        (cond

          ;; if Map represent Block or File
          (map? (:value header))
          (do (f header path)
              (recur-walk-throw (:value header) f path))

          :else (f header path))))
    ;; Do recursive for Tail destruction in the same level
    (if (some? tail) (recur-walk-throw tail f path))))

(def config {:resource.edn (clojure.edn/read-string (slurp "./config/resource.edn"))})

(defn debug-walk [m]
  (let [a (atom true)
        f (fn [block path]
            (print (:type block))
            (println path))]
   (recur-walk-throw m f [])))
;; (debug-walk config)


 
