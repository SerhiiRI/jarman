;;; Context tree:
(ns jarman.tools.lang
  (:use clojure.reflect
        seesaw.core)
  (:require [clojure.string :as string]
            [clojure.java.io :as io]))

(def recursive-linier-terms [{:term 'reduce :arg 2}
                             {:term 'map :arg 1}
                             {:term 'filter :arg 1}
                             {:term 'if :arg 2}
                             {:term 'do :arg 1}
                             {:term '|> :arg 1}
                             {:term 'doto :arg 1}
                             {:term 'ift :arg 1}
                             {:term 'ifn :arg 1}
                             {:term 'otherwise :arg 1}])

(defmacro action-linier-preprocess [symbol args]
  (condp = symbol
    'map `(map ~@args)
    'reduce `(reduce ~@args)
    'filter `(filter ~@args)
    'do `(~@args)
    '|> `(~@args)
    'doto `(doto ~(last args) ~(first args))
    'otherwise `(if ~(second args) ~(second args) ~(first args) )
    'ifn `(if ~(last args) ~(first args) ~(last args))
    'ift `(if ~(last args) ~(last args) ~(first args)) 
    'nil))

(defmacro recursive-linier-preprocessor
  ([v] v)
  ([v body]
   (if (empty? body) v
       (if (= 1 (count body)) `(~@body)
           (let [{term :term offs :arg :as whole} (first (filter #(= (first body) (:term %)) recursive-linier-terms))]
             (if (nil? whole)
               (throw (Exception. (str "Term '" (first body) "' not understandable ")))
               (let [term-args (take offs (rest body))
                     body (drop (inc offs) body)]
                 (if (empty? body) `(action-linier-preprocess ~term (~@term-args ~v))
                     `(let [temporary# (action-linier-preprocess ~term (~@term-args ~v))]
                        (recursive-linier-preprocessor temporary# ~body)
                        )))))))))

(defmacro where-binding-form [binding-form]
  `[~(first binding-form) (recursive-linier-preprocessor ~(first (rest binding-form)) ~(rest (rest binding-form)))])

(defmacro where [binding & body]
  (let [let-binding-forms (reduce (fn [acc bnd] (concat acc (macroexpand-1 `(where-binding-form ~bnd)))) [] binding)]
    `(let [~@let-binding-forms]
       ~@body)))
;; (where ((haf_count (range 10) map #(- % 5) filter #(< 0 %) do count ifn 0)
;;         (haf_count 10 do string? otherwise "10")
;;         (haf_count [1 2 3 4] |> flatten |> map do-smth filter filter-smth if "EMPTY" "NON EMPTY"))
;;        haf_count)


