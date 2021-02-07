(ns jarman.tools.jarman)

(defn gen-context-tree
  ([] (gen-context-tree (symbol (str *ns*))))
  ([nms] {:pre [(symbol? nms)]}
   (let [pc #(println (apply str ";; " %&))
         offset (atom 0)
         file (:file (meta (resolve (symbol (format "%s/%s" (name nms) (first (keys (ns-publics nms))))))))
         headers (filter
                  identity
                  (map-indexed
                   (fn [i e] (if (clojure.string/starts-with? e ";;")
                              (let [e (-> e (clojure.string/replace #"\\r" "") clojure.string/trim)]
                                (if-let [[_ hb header he] (re-matches #"^(;{2,4})\s*([\w\d\s]*[\w\d]{1})\s*(;{2,4})$" e)]
                                  (let [chb (count hb) che (count he)]
                                    (if (= chb che)
                                      {:line i :header header :h-size (dec chb)})) ))))
                   (clojure.string/split (slurp file) #"\n")))
         fk-list (map (fn [f-v]
                        (let [nms_var (resolve (symbol (format "%s/%s" (name nms) f-v)))]
                          {:name (str f-v) :args (apply str (interpose ", " (:arglists (meta nms_var))))
                           :line (:line (meta nms_var))}))
                      (vec (keys (ns-publics nms))))]
     ;; (pc (format "The namespace %s" (str nms)))
     (pc "<TODO Description>")
     (pc "Content: ")
     (for [e (sort-by :line (concat headers fk-list))]
       (if (nil? (:header e)) 
         (pc 
          (apply str (repeat @offset " "))
          (if (empty? (:args e))
            (format "  %s" (:name e))
            (format "  %s %s" (:name e) (:args e))))
         (do
           (swap! offset (fn [_] (:h-size e)))
           (pc 
            (apply str (repeat @offset "*"))
            (format " %s" (:header e)))))
       ))nil))
