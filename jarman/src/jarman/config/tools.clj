(ns jarman.config.tools
  (:require [jarman.tools.lang :refer :all]
            [clojure.spec.alpha :as s]))


(def ^:private offset-char "| ")
(defn- is-block? [m] (= (:type m) :block))
(defn- is-file? [m] (= (:type m) :file))
(defn- is-block-file? [m] (or (is-block? m) (is-file? m)))
(defn- is-param? [m] (= (:type m) :param))
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
        (is-block-file? header)
        (do (printblock level header path)
            (build-part-of-map (inc level) (map-destruct (:value header)) path))

        ;; fi Map represent Parameters
        (is-param? header) (printparam level header path))))
  ;; Do recursive for Tail destruction in the same level
  (if (some? tail) (build-part-of-map level  (map-destruct tail) path)))

(def config {:resource.edn (clojure.edn/read-string (slurp "./config/resource.edn"))})

(defn debug-config [m]
  (build-part-of-map 0 (map-destruct m) []))
;; (recur-config config)


