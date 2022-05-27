;; fixme: make friendly to threading macro
(defn sort-index-f
  "Example
   (sort-index-f identity [3 1 2 0] [\\d \\b \\c \\a])
    ;; => {0 \\a, 1 \\b, 2 \\c, 3 \\d}
   (sort-index-f vals [3 1 2 0] [\\d \\b \\c \\a])
    ;; => (\\a \\b \\c \\d)"
  [f index-col v-col]
  {:pre [(= (count index-col) (count v-col))]}
  (f (into (sorted-map) (zipmap index-col v-col))))

(defn group-by-apply
  [f coll & {:keys [apply-item apply-group]
             :or {apply-group (fn [e] e)
                  apply-item (fn [e] e)}}]
  (let [result (transient {})]
    (->> coll
         (reduce
          (fn [ret x]
            (let [k (f x)]
              (assoc! ret k (conj (get ret k []) (apply-item x))))) (transient {}))
         (persistent!)
         (reduce-kv
          (fn [ret k v]
            (assoc! ret k (apply-group v))) (transient {}))
         (persistent!))))




