(ns jarman.tools.fuzzy-search
  (:require [clojure.pprint :as pprint]
            [jarman.tools.lang :refer :all]
            [clj-fuzzy.metrics :as fuzzy]))

(defn- to-score-map
  "Example
    (to-score-map \"dupa\")
      => {:text \"dupa\", :model \"dupa\"}
    (to-score-map {:text \"dupa\" :model \"chuj\"})
      => {:text \"dupa\", :model \"chuj\"}"
  [w]
  (cond
    (string? w) (hash-map :text w :model w)
    (map? w) (let [{:keys [text model]} w]
               (assert (some? text) "`:text` cannot be nil")
               {:text text :model (if model model text)})))

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

(defn hamming [search-word words]
  (->> words
       (mapv to-score-map)
       (mapv #(assoc % :value (rift (fuzzy/hamming search-word (:text %)) 10000)))
       (sort-by :value)))

(defn jaccard [search-word words]
  (->> words
       (mapv to-score-map)
       (mapv #(assoc % :value (fuzzy/jaccard search-word (:text %))))
       (sort-by :value)))

(defn jaro [search-word words]
  (->> words
       (mapv to-score-map)
       (mapv #(assoc % :value (fuzzy/jaro search-word (:text %))))
       (sort-by :value)
       (reverse)))

(defn jaro-winkler [search-word words]
  (->> words
       (mapv to-score-map)
       (mapv #(assoc % :value (fuzzy/jaro-winkler search-word (:text %))))
       (sort-by :value)
       (reverse)))

(defn tversky [search-word words]
  (->> words
       (mapv to-score-map)
       (mapv #(assoc % :value (fuzzy/tversky search-word (:text %))))
       (sort-by :value)
       (reverse)))




;;  ____  _____ __  __  ___
;; |  _ \| ____|  \/  |/ _ \
;; | | | |  _| | |\/| | | | |
;; | |_| | |___| |  | | |_| |
;; |____/|_____|_|  |_|\___/


(comment
  (def words ["dupa" "dpda" "duuupa" "dppuup" "suka" "erjfa" "dupkra"])
  ;; ------------------------------------------------------------------
  (->> words (dice "dupa")         (mapv :model))
  ;; => [1.0 0.8571428571428571 0.5 0.25 0.0 0.0 0.0]
  ;; => ["dupa" "duuupa" "dupkra" "dppuup" "erjfa" "suka" "dpda"]
  (->> words (levenshtein "dupa")  (mapv :model))
  ;; => [0 2 2 2 2 4 4]
  ;; => ["dupa" "dpda" "duuupa" "suka" "dupkra" "dppuup" "erjfa"]
  (->> words (hamming "dupa")      (mapv :model))
  ;; => [0 2 2 10000 10000 10000 10000]
  ;; => ["dupa" "dpda" "suka" "duuupa" "dppuup" "erjfa" "dupkra"]
  (->> words (jaccard "dupa")      (mapv :model))
  ;; => [0 0 1/4 1/4 1/3 2/3 7/8]
  ;; => ["dupa" "duuupa" "dpda" "dppuup" "dupkra" "suka" "erjfa"]
  (->> words (jaro "dupa")         (mapv :model))
  ;; => [1.0 0.888888888888889 0.888888888888889 0.8333333333333334 0.6666666666666666 0.638888888888889 0.48333333333333334]
  ;; => ["dupa" "dupkra" "duuupa" "dpda" "suka" "dppuup" "erjfa"]
  (->> words (jaro-winkler "dupa") (mapv :model))
  ;; => [1.0 0.9222222222222223 0.9111111111111112 0.8500000000000001 0.675 0.6666666666666666 0.48333333333333334]
  ;; => ["dupa" "dupkra" "duuupa" "dpda" "dppuup" "suka" "erjfa"]
  (->> words (tversky "dupa")      (mapv :model))
  ;; => [1 1 3/4 3/4 2/3 1/3 1/8]
  ;; => ["duuupa" "dupa" "dppuup" "dpda" "dupkra" "suka" "erjfa"]
  )


(comment
  ;; concepts
  ;; Should be some compressing algorytm
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
                            (rest stack)) (inc i))))) (list) 0))))

  (let [letters (into #{} "dupa")
        results
        (->> words (dice "dupa") (mapv :model))]
    (for [word results]
      (->> (map-indexed vector word)
        (reduce
          (fn [acc [index letter]]
            (if (letters letter) (conj acc index) acc)) [])))))
