(ns jarman.tools.fuzzy-search
  (:require [clojure.pprint :as pprint]
            [clj-fuzzy.metrics :as fuzzy]))

(defn- to-score-map [w]
  (cond
    (string? w) (hash-map :text w :model w)
    (map? w) (let [{:keys [text model]} w]
               (assert (some? text) "`:text` cannot be nil")
               {:text text :model (if model model text)})))

;; (to-score-map "dupa")
;; (to-score-map {:text "dupa" :model "chuj"})

(defn dice [search-word words]
  (->> words
       (mapv to-score-map)
       (mapv #(assoc % :value (fuzzy/dice search-word (:text %))))
       (sort-by :value)
       (reverse)))

(defn levenshtein [search-word words]
  (->> words
       (mapv to-score-map)
       (mapv #(assoc % :value (fuzzy/levenshtein search-word (:text %))))
       (sort-by :value)))

(comment
 (def words ["dupa" "dpda" "duuupa" "dppuup" "suka" "erjfa" "dupkra"])
 (dice "dupa" words)
 (let [letters (into #{} "dupa")
       results
       (->> words
            (dice "dupa")
            (mapv :model))]
   (for [word results]
     (->> (map-indexed vector word)
          (reduce
           (fn [acc [index letter]]
             (if (letters letter) (conj acc index) acc)) [])))))

(comment
  ;; fixme 
  (seq-to-ranges [1 3 4])
  (defn seq-to-ranges
    "Description
    Compres some range sequence to sequence
    of parital ranges. where (0 1 2 10 11)
    is two ranges (range 0 2) (range 10 11)
    In result we get start element, and count
    of number's in short-range
  Example
    (seq-to-ranges [0 1 2 5 6 9 11 55 56])
     => ((0 3) (5 2) (9 1) (11 1) (55 2))"
    [r]
    (if-not (> (count r) 1)
      (list (list (first r) 1))
      (reverse
       ((fn recr [stack i]
          (if-not (> (dec (count r)) i) stack
                  (if (not= 1 (- (nth r (inc i)) (nth r i)))
                    (recr (cons (list (nth r (inc i)) 1) stack) (inc i))
                    (recr (cons (let [[p c] (first stack)] (list (if p p (nth r i)) (if c (inc c) 2)))
                                (rest stack)) (inc i))))) (list) 0)))))





