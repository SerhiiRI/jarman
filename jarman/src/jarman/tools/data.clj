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

(let [s '({:firstname "Adam"
           :lastname  "Smith"
           :diagnosis "COVID-19"
           :treated   true}
          {:firstname "Joseph"
           :lastname  "Goodman"
           :diagnosis "COVID-19"
           :treated   true}
          {:firstname "Werner"
           :lastname  "Ziegler"
           :diagnosis "COVID-19"
           :treated   false}
          {:firstname "Boris"
           :lastname  "Henry"
           :diagnosis "Healthy"
           :treated   false}
          {:firstname "Johnny"
           :lastname  "Grayhand"
           :diagnosis "COVID-76"
           :treated   false})]
  (group-by-apply :diagnosis s 
                  :apply-group (partial clojure.pprint/cl-format nil "~{~A~^,~}")
                  :apply-item #(clojure.string/upper-case (get % :lastname))))

(defmacro factor-group [data group-data bindings & body]
  `(for [[~(apply hash-map bindings) ~group-data]
         (group-by #(select-keys % ~(mapv second (partition 2 bindings))) ~data)]
     (do ~@body)))

(def all-patients
 '({:firstname "Adam"
   :lastname  "Smith"
   :diagnosis "COVID-19"
   :treated   true}
  {:firstname "Joseph"
   :lastname  "Goodman"
   :diagnosis "COVID-19"
   :treated   true}
  {:firstname "Werner"
   :lastname  "Ziegler"
   :diagnosis "COVID-19"
   :treated   false}
  {:firstname "Boris"
   :lastname  "Henry"
   :diagnosis "Healthy"
   :treated   false}
  {:firstname "Johnny"
   :lastname  "Grayhand"
   :diagnosis "COVID-76"
   :treated   false}))


(factor-group
 all-patients patients-group [treated? :treated disease-name :diagnosis]
 ;; (println " начало обработки группы пациентов с диагнозом " disease-name
 ;;          (if treated? ", подвергавшихся лечению" ", НЕ подвергавшихся лечению"))
 ;; (println " количество пациентов в группе - " (count patients-group))
 ;; (println " фамилии пациентов - " (clojure.string/join ", " (map :lastname patients-group)))
 ;; (count patients-group)
 ;; patients-group
 disease-name
 )
