(ns lang
  (:require
   [jarman.lang :as lang]))

(defn get-key-paths-recur [& {:keys [map-part end-path-func path sequence?]}]
  ;; If `map-part` is nil, then eval function
  ;; end-path-func. Otherwise destruct `map-part`
  ;; for continuing recursion
  (if (nil? map-part) (end-path-func path)
      (let [[head tail] (lang/map-destruct map-part)
            map-first-key (lang/first-key head)
            value-of-first-key (map-first-key head)
            ;; shortlambda
            vconcat (comp vec concat)]
        (cond

          ;; Do recusion if it Hashmap
          (map? value-of-first-key)
          (get-key-paths-recur
           :map-part (map-first-key head)
           :end-path-func end-path-func
           :path (vconcat path [map-first-key])
           :sequence? sequence?)

          ;; Do recursion if value is vector
          ;; and `sequence?` parameter has
          ;; `true` value
          (and sequence? (seqable? value-of-first-key) (not (string? value-of-first-key)))
          (doall (map
                  (fn [index-value index]
                    (get-key-paths-recur
                     :map-part index-value
                     :end-path-func end-path-func
                     :path (vconcat path [map-first-key] [index])
                     :sequence? sequence?))
                  value-of-first-key (range (count value-of-first-key))))

          ;; Do recusion with `nil` for `:map-part`
          ;; if param be nil - then evaluate function
          ;; `(end-path-func path)`
          :else (get-key-paths-recur
                 :map-part nil
                 :end-path-func end-path-func
                 :path (vconcat path [map-first-key])
                 :sequence? sequence?))
        ;; Recusion in width, without chnaging
        ;; path deepth
        (if tail
          (get-key-paths-recur
           :map-part tail
           :end-path-func end-path-func
           :path path
           :sequence? sequence?)))))

(defn key-paths
  "Description
    Get vector's of all keys from map linking it in path.
    If `sequence?` optionaly parameter is set on true - searching deep include also list's

  Example
    (key-paths {:a 1 :b {:t 2 :f 2} :c [{:t 3} {:f 3}]})
      ;;=> [[:a]
            [:b :t]
            [:b :f]
            [:c]]
    (key-paths {:a 1 :b {:t 2 :f 2} :c [{:t 3} {:f 3}]} :sequence? true)
      ;;=> [[:a]
            [:b :t]
            [:b :f]
            [:c 0 :t]
            [:c 1 :f]]"
  [m & {:keys [sequence?] :or {sequence? false}}]
  (lang/blet (get-key-paths-recur
         :map-part m
         :end-path-func in-deep-key-path-f
         :path nil
         :sequence? sequence?)
        @in-deep-key-path
        [in-deep-key-path (atom [])
         in-deep-key-path-f (fn [path] (swap! in-deep-key-path (fn [path-list] (conj path-list path ))))]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; `as-debug->>` threading macro ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ^:private sexpression-walk-and-replace
  "Example
    (sexpression-walk-and-replace '! 'DUPA '(let [a 2] (:pets (+ a !))))
     ;; => (let [a 2] (:pets (+ a DUPA)))
    (sexpression-walk-and-replace '! 'DUPA nil)
     ;; => nil
    (sexpression-walk-and-replace '! 'DUPA \"dsa\")
     ;; => \"dsa\""
  [what on-what l]
  (if (sequential? l)
    ((cond (vector? l) vec :else seq)
     (concat (list (sexpression-walk-and-replace what on-what (first l)))
             (if (> (count (rest l)) 0)
               (sexpression-walk-and-replace what on-what (rest l)))))
    (if (= l what) on-what l)))

(defmacro as-debug->>
  "Description
    do the same what to do `as->>` but make debug
    macros unwrapp for clear reading what to do

  Example
    For this test data:
    (def owners [{:owner \"Jimmy\"
                  :pets (ref [{:name \"Rex\"
                               :type :dog}
                              {:name \"Sniffles\"
                               :type :hamster}])}
                 {:owner \"Jacky\"
                  :pets (ref [{:name \"Spot\"
                               :type :mink}
                              {:name \"Puff\"
                              :type :magic-dragon}])}])
   Make macro expand:
     (as-debug->> owners ! (nth ! 0) (:pets !) (deref !) (! 1) (! :type))
   And you take unbloated expression without let...
     ;;=> (((deref (:pets (nth owners 0))) 1) :type)
     ;;=> :hamster"
  ([var-replace-to alias-name & wrapp-functor-list]
   (reduce (fn [acc sexp]
             `~(sexpression-walk-and-replace alias-name acc sexp))
           var-replace-to
           wrapp-functor-list)))

(defn left-merge
  "Description
     Merge for map.
     Merge from right map to left map only by keys in left map.
     If key is not inside right map then use left map value.
   Example:
     (left-merge {:a \"a\" :b \"b\"} {:a \"1\" :c \"3\"}) => {:a \"1\", :b \"b\"}"
  [map-coll-orgin map-col-to-join]
  ((fn l-merge [coll-keys coll-orgin]
    (if (empty? coll-keys)
      coll-orgin
      (->> (first coll-keys)
           ((fn [key] (assoc coll-orgin key (lang/rift (key map-col-to-join) (key coll-orgin)))))
           (l-merge (drop 1 coll-keys)))))
   (keys map-coll-orgin) map-coll-orgin))

(defn verify-types-in-map
  "Description:
     Verify - types in map
     Set two map, first with kye and type check fn, second with key and value.
     Fn return true or false.
   Example:
     (verify-types-in-map {:a string?} {:a \"Pepe\"}) => true
     (verify-types-in-map {:a string?} {:a :pepe}) => false"
  [m-rules m-valid]
  (let [tf-list (doall
                 (map
                  (fn [[key valid]]
                    (let [to-check (get m-valid key)]
                          (if-not (nil? to-check)
                            (valid to-check)
                            false)))
                  m-rules))]
    (empty?
     (filter #(false? %) tf-list))))
