;; https://gist.github.com/vaughnd/5099299 thx to you man 

(ns jarman.tools.fuzzy-search
  (:require [clojure.pprint :as pprint]))

(defn get-dataset
  [file]
  (with-open [rdr (clojure.java.io/reader file)]
         (vec (line-seq rdr))))

(defn str-len-distance
  ;; normalized multiplier 0-1
  ;; measures length distance between strings.
  ;; 1 = same length
  [s1 s2]
  (let [c1 (count s1)
        c2 (count s2)
        maxed (max c1 c2)
        mined (min c1 c2)]
    (double (- 1 (/ (- maxed mined) maxed)))))

(def MAX-STRING-LENGTH 1000.0)

(defn clean-str
  [s]
  (.replaceAll (.toLowerCase s) "[ \\/_]" ""))

(defn score
  [oquery ostr]
  (let [query (clean-str oquery)
        str (clean-str ostr)]
    (loop [q (seq (char-array query))
           s (seq (char-array str))
           mult 1
           idx MAX-STRING-LENGTH
           score 0]
      (cond
       ;; add str-len-distance to score, so strings with matches in same position get sorted by length
       ;; boost score if we have an exact match including punctuation
       (empty? q) (+ score
                     (str-len-distance query str)
                     (if (<= 0 (.indexOf ostr oquery)) MAX-STRING-LENGTH 0))
       (empty? s) 0
       :default (if (= (first q) (first s))
                  (recur (rest q)
                         (rest s)
                         (inc mult) ;; increase the multiplier as more query chars are matched
                         (dec idx) ;; decrease idx so score gets lowered the further into the string we match
                         (+ mult score)) ;; score for this match is current multiplier * idx
                  (recur q
                         (rest s)
                         1 ;; when there is no match, reset multiplier to one
                         (dec idx)
                         score))))))

(defn search
  [file query & {:keys [limit] :or {limit 20}}]
  (println "Matching " query " in " file)
  (let [query (.toLowerCase query)]
    (let [data (get-dataset file)]
      (take limit
            (sort-by :score (comp - compare)
                     (filter #(< 0 (:score %))
                             (for [s data]
                               {:data s
                                :score (score query (.toLowerCase s))})))))))

;; usage: <file, e.g. /usr/share/dict/words> <query>
(defn -main
  [& args]
  (pprint/pprint (search (first args) (second args)))
  )


;; ANOTHER SEARCH

(defn fuzzy-search [query col]
  take 1
  (sort-by :score
           (comp - compare)
           (filter #(< 0 (:score %))
                   (for [doc col]
                     {:data doc
                      :score (score query doc)}))))


(let [col ["sign" "generate-keypair" "encrypt" "decode64" "encode64" "decrypt"]
      query "encpt"]
  (sort-by :score
   (for [doc col]
     {:data doc
      :score (score query doc)})))
