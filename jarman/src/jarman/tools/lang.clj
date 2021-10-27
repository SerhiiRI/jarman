;;; File contain some usefull functions, hacks, or examples
;;; Contens:
;;; * Helper functions - quick usefull hacks
;;; ** in?
;;; ** filter-nil
;;; ** join 
;;; ** split
;;; ** all-methods
;;; ** blet
;;; ** cond-let
;;; * where macro
;;; * TODO: railway
;;; * Map-type toolkit
;;; ** head/tail destruction for maps
;;; ** cond-contain test if key in map
;;; ** get-key-paths-recur with key-paths implementation
;;; * as-debug->> 
(ns jarman.tools.lang
  (:use clojure.reflect seesaw.core)
  (:require [clojure.string :as string]
            [clojure.pprint :as pprint]
            [clojure.java.io :as io]))

;;;;;;;;;;;;;;;;;;;;;;;
;;; helper function ;;;
;;;;;;;;;;;;;;;;;;;;;;;

(defn pp-str
  "Example:
   [{:a \"yellow\", :b \"pink\"} {:a \"yellow\", :b \"pink\"} {:a \"yellow\", :b \"pink\"} {:a \"green\", :b \"red\"}]
   ;; => [{:a \"yellow\", :b \"pink\"}
          {:a \"yellow\", :b \"pink\"}
          {:a \"yellow\", :b \"pink\"}
          {:a \"green\", :b \"red\"}]"
  [some-form]
  (with-out-str (pprint/pprint some-form)))

(defn in?
  "(in? [1 2 3 4 5 6] 1)
   (in? 1 1)"
  [col x]
  (if (and (not (string? col)) (seqable? col))
    (some #(= x %) col)
    (= x col)))

(defn in-r?
  "Is the `in?` fucntion but with reverse arguemnts 
  (in? 1 [1 2 3 4 5 6])
   (in? 1 1)"
  [x col]
  (in? col x))

(defmacro filter-nil
  "(filter-nil [nil 1 nil 3 4]) ;=> [1 3 4]"
  [col]
  `(filter identity ~col))

(defmacro join-vec
  "(join-vec [:a :d] [:b :c]) => [:a :d :b :c]"
  [& vecs]
  `(vec (concat ~@vecs)))

(defn key-setter
  "Description

    Test if some `parameter-k` key inside `m`
    if not, then add by this key `parameter-default-v`

    Param `parameter-default-v` may be value or 0-arg
    function

  Example
    (let [t1 (key-setter :permission [:user])
          t2 (key-setter :exist-key 'no)
          t3 (key-setter :fn-value #(gensym))]
       (-> {:exist-key 'yes} t1 t2 t3))
     ;; => {:exist-key   yes
            :permission  [:user]
            :fn-value    G__24897}"
  [parameter-k parameter-default-v]
  (fn [m] (if (contains? m parameter-k) m
           (assoc m parameter-k
                  (if (fn? parameter-default-v)
                    (parameter-default-v)
                    parameter-default-v)))))

(def all-vec-to-floor
  "(all-vec-to-floor ([:a] ([:d :x] [:e :y] ([:z])))) ;; => ([:a] [:d :x] [:e :y] [:z])"
  (fn [vects]
    (do
      (defn r-unwrapper [result example]
        (reduce #(if (vector? %2)
                   (conj %1 %2)
                   (concat %1 (r-unwrapper [] %2))) result example))
      (r-unwrapper [] vects))))

(defn to-hashkey 
  "Description:
      Set some :key and get hashkey like :key => :#key.
   Example:
      (to-hashkey \"my-id\") => :#my-id
      (to-hashkey :my-id)    => :#my-id"
  [x] (cond (string? x)  (keyword (str "#" x))
            (keyword? x) (keyword (str "#" (name x)))))

(defn rift
  "Description:
      Return condition result if true or return else value if condition return false, nil, 0, empty
   Example:
   (rift (- 3 3) \"zero\") => \"zero\"
   (rift []   \"zero\")    => \"zero\"
   (rift \"\" \"zero\")    => \"zero\"
   (rift nil  \"zero\")    => \"zero\"
   (rift [1]  \"zero\")    => 1
   (rift (- 3 2) \"zero\") => 1
   (rift \"A\"   \"zero\") => \"A\"
   (rift #object \"zero\") => #object"
  [con els]
  (cond
    (nil? con)        els
    (number? con)     (if (zero?  con) els con)
    (sequential? con) (if (empty? con) els con)
    (string? con)     (if (empty? con) els con)
    (map? con)        (if (empty? con) els con)
    :else con))

(defn timelife
  "Description:
    Run fn after some time.
    Set time, 1 is a 1 sec
    Set fn 
  Example:
    (timelife (1 ))"
  ([time fn-to-invoke]
   (timelife time fn-to-invoke ""))
  ([time fn-to-invoke title]
   (.start
    (Thread.
     (fn []
       (if (>= time 0)
         (do
           (Thread/sleep (* 1000 time))
           (try
             (fn-to-invoke)
             (catch Exception e (println (str "\nException in timelife: " title "\n" (str (.getMessage e)))))))))))))

(defn left-merge
  "Description
     Merge for map.
     Merge from right map to left map only by keys in left map.
     If key is not inside right map then use left map value.
   Example:
     (left-merge {:a \"a\" :b \"b\"} {:a \"1\" :c \"3\"}) => {:a \"1\", :b \"b\"}
   "
  [map-coll-orgin map-col-to-join]
  ((fn l-merge [coll-keys coll-orgin]
    (if (empty? coll-keys)
      coll-orgin
      (->> (first coll-keys)
           ((fn [key] (assoc coll-orgin key (rift (key map-col-to-join) (key coll-orgin)))))
           (l-merge (drop 1 coll-keys)))))
   (keys map-coll-orgin) map-coll-orgin))

(defn vemap
  "Description:
    vector empty map
    Create from vector or list map with nil value.
    If second arg will be somethin then it will be default value for keys.
  Example:
    (vemap [:a :b])
      => {:a nil, :b nil}
    (vemap [:a :b] \"nice\")
      => {:a \"nice\", :b \"nice\"}
  "
  ([coll] (vemap coll nil))
  ([coll val] (into {} (map (fn [k] {k val}) coll))))

(defn cnmap
  "Description:
     cut nil map
     Just cut parts with nil in value.
   Example:
     (cnmap {:a nil :b \"a\"})
     ;; => {:b \"a\"}
   "
  [coll-map]
  (into {} (filter #(not (nil? (second %))) coll-map)))

(defn v-tim
  "Description:
     Verify - types in map
     Set two map, first with kye and type check fn, second with key and value.
     Fn return true or false.
   Example:
     (verify-types-in-map {:a string?} {:a \"Pepe\"}) => true
     (verify-types-in-map {:a string?} {:a :pepe}) => false
  "
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


(defmacro join
  "(filter-nil [nil 1 nil 3 4]) ;=> [1 3 4]"
  [delimiter col]
  `(clojure.string/join ~delimiter ~col))

(defmacro split
  "(filter-nil [nil 1 nil 3 4]) ;=> [1 3 4]"
  [delimiter s]
  `(clojure.string/split ~s ~delimiter ))

(defn all-methods
  "Print methods for object/class, in argument"
  [some-object]
  (->> some-object
       reflect :members (filter :return-type) (map :name) sort (map #(str "." %))
       distinct (map symbol)))

(defn random-unique-id
  "Description
    Generate unique id from current timestamp,
    also add random int to end
  Example
    (random-unique-id)
    ;; => 2021-06-06T02:18:32.1718666878"
  [] (string/join "" [(java.time.LocalDateTime/now) (rand-int 10000)]))

(defmacro blet
  "Description
    Let with binding in last sexp, otherwise in first block
  
  Example
    (blet (+ a b) [a 1 b 2]) ;; => 3
    (blet (+ a b) (- a b) [a 1 b 2]) ;; => -1
  
  Spec
    (blet <calcaulation>+ <binding-spec>{1})"
  [& arguments]
  {:pre [(vector? (last arguments))]}
  `(let ~(last arguments) ~@(butlast arguments)))

(defmacro cond-let
  "Description
    Is macro which combine let+cond
    cond-let has one 'hack'. It use first
    binded pattern to automaticaly apply
    to ONE-WORD predicates
    Macro automatic transform
     string? -> (string? T)

  Example
    (cond-let [T \"something\"]
	string?     \"is string\"
	boolean?    \"is boolean\"
	(number? T) \"is number \"  
	:else nil)"
  [binding & body]
  (let [var-name (first binding)
        cond-list
        (reduce
         concat
         (map #(if (symbol? (first %1)) (list (list (first %1) var-name) (second %1)) %1)
              (partition 2 body)))]
    `(let [~@binding]
       (cond
         ~@cond-list))))

(defn- read-one
  [r]
  (try (read r)
       (catch java.lang.RuntimeException e
         (if (= "EOF while reading" (.getMessage e)) ::EOF
             (throw e)))))

(defn read-seq-from-file
  "Reads a sequence of top-level objects in file at path."
  [path]
  (with-open [r (java.io.PushbackReader. (clojure.java.io/reader path))]
    (binding [*read-eval* false]
      (doall (take-while #(not= ::EOF %) (repeatedly #(read-one r)))))))

;;;;;;;;;;;;;;;;;;;
;;; where macro ;;;
;;;;;;;;;;;;;;;;;;;

(def recursive-linier-terms
  [{:term 'reduce :arg 2}
   {:term 'map :arg 1}
   {:term 'filter :arg 1}
   {:term 'if :arg 2}
   {:term 'do :arg 1}
   {:term 'apply :arg 1}
   {:term '|> :arg 1}
   {:term 'doto :arg 1}
   {:term 'if1 :arg 2}
   {:term 'if2 :arg 3}
   {:term 'iff1 :arg 2}
   {:term 'iff2 :arg 3}
   {:term 'otherwise :arg 1}])

(defmacro action-linier-preprocess
  "Description
    Make some pipeline managment to control over instruction
  
  Example 
    (action-linier-preprocess reduce [1 2 3] (+ 0));; => 6
    (action-linier-preprocess map [1 2 3] (inc));; => (2 3 4)
    (action-linier-preprocess filter [1 2 3] (number?));; => (1 2 3)
    (action-linier-preprocess do [1 2 3] (count));; => 3
    (action-linier-preprocess |> [1 2 3] (count));; => 3
    (action-linier-preprocess doto [1 2 3] (println));; => 3
    (action-linier-preprocess apply [1 2 3] (+));; => 6
    (action-linier-preprocess otherwise nil (2));; => 2
    (action-linier-preprocess iff1 1 (number? inc));; => 2
    (action-linier-preprocess iff2 -2 (pos? inc));; => -2"
  [action var-value pipe-declaration]
  {:pre [(symbol? action) (sequential? pipe-declaration)]}
  (condp = action
    'map `(map ~@pipe-declaration ~var-value)
    'reduce `(reduce ~@pipe-declaration ~var-value)
    'filter `(filter ~@pipe-declaration ~var-value)
    'do `(~@pipe-declaration ~var-value)
    'apply `(apply ~@pipe-declaration ~var-value)
    '|> `(~@pipe-declaration ~var-value)
    'doto `(doto ~var-value ~(first pipe-declaration))
    'otherwise `(if ~var-value ~var-value ~(first pipe-declaration))
    'if1 `(if (~(first pipe-declaration) ~var-value) ~(second pipe-declaration) nil)
    'if2 `(if (~(first pipe-declaration) ~var-value) ~(second pipe-declaration) ~(nth pipe-declaration 2 nil))
    'iff1 `(if (~(first pipe-declaration) ~var-value) (~(second pipe-declaration) ~var-value) ~var-value)
    'iff2 `(if (~(first pipe-declaration) ~var-value) (~(second pipe-declaration) ~var-value) (~(nth pipe-declaration 2 'identity) ~var-value))
    'nil))

(defmacro recursive-linier-preprocessor
  ([name v] v)
  ([name v body]
   (if (empty? body) v
       (if (= 1 (count body)) `(~@body)
           (let [{term :term offs :arg :as whole} (first (filter #(= (first body) (:term %)) recursive-linier-terms))]
             (if (nil? whole)
               (throw (Exception. (str "Term '" (first body) "' not understandable ")))
               (let [term-args (take offs (rest body))
                     body (drop (inc offs) body)]
                 (if (empty? body) `(action-linier-preprocess ~term ~v (~@term-args))
                     `(let [~name (action-linier-preprocess ~term ~v (~@term-args))]
                        (recursive-linier-preprocessor ~name ~name ~body))))))))))

(defmacro where-binding-form [binding-form]
  `[~(first binding-form)
    (let [~(first binding-form) ~(second binding-form)]
      (recursive-linier-preprocessor ~(first binding-form) ~(first binding-form)  ~(rest (rest binding-form))))])

(defmacro where
  "Description
    Let on steroids

  Example
    (where
     ((temp 10 do string? otherwise \"10\") ;; \\\"10\\\"
      (temp 3 iff1 number? inc) ;; 4
      (temp 3 iff2 neg? inc dec) ;; 2
      (temp 3 if1 number? \"3\") ;; \"3\"
      (temp nil if2 number? \"3\" 3) ;; 3
      (temp 0 if2 zero? (+ 1 temp) temp) ;; 1
      (temp 3 do inc do inc do inc) ;; 6
      (temp 3 |> inc |> inc) ;; 5
      (temp 0 do inc if2 #(< 0 %) \"EMPTY\" \"NOT EMPTY\") ;; \"EMPTY\"
      (temp (range 10) map #(- % 5) filter #(< 0 %) do count if2 zero? \"EMPTY\" temp) ;; 4
      (temp [1 2 3 4] filter odd?) ;; => (1 3)
      (temp [1 2 3 4] map odd?) ;; (true false true false)
      (temp [1 2 3 4] reduce + 0) ;; 10
      (temp 4 doto println))
     temp)"
  [binding & body]
  (let [let-binding-forms (reduce (fn [acc bnd] (concat acc (macroexpand-1 `(where-binding-form ~bnd)))) [] binding)]
    `(let [~@let-binding-forms]
       ~@body)))


(defmacro wlet
  "Description
    Is where with binding block on end of expresion
  
  Example
    (wlet (+ a b) ((a 1) (b 2)) ;; => 3
    (wlet (+ a b) (- a b) ((a 1) (b 2))) ;; => -1

    (wlet (+ temp 2) ;; 6
     ((temp 10 do string? otherwise \"10\") ;; \\\"10\\\"
      (temp 3 iff1 number? inc) ;; 4
      (temp 3 iff2 neg? inc dec) ;; 2
      (temp 3 if1 number? \"3\") ;; \"3\"
      (temp nil if2 number? \"3\" 3) ;; 3
      (temp 0 if2 zero? (+ 1 temp) temp) ;; 1
      (temp 3 do inc do inc do inc) ;; 6
      (temp 3 |> inc |> inc) ;; 5
      (temp 0 do inc if2 #(< 0 %) \"EMPTY\" \"NOT EMPTY\") ;; \"EMPTY\"
      (temp (range 10) map #(- % 5) filter #(< 0 %) do count if2 zero? \"EMPTY\" temp) ;; 4
      (temp [1 2 3 4] filter odd?) ;; => (1 3)
      (temp [1 2 3 4] map odd?) ;; (true false true false)
      (temp [1 2 3 4] reduce + 0) ;; 10
      (temp 4 doto println))) 
  Spec
    (wlet <calcaulation>+ <binding-spec>{1})"
  [& arguments]
  `(where ~(last arguments) ~@(butlast arguments)))

;;;;;;;;;;;;;;;;;;;;;;;;
;;; Map-type toolkit ;;;
;;;;;;;;;;;;;;;;;;;;;;;;

(defmacro first-key
  "Description:
    Get the first key of some map
  
  Examples:  
  (first-key {:a 1 :b 2}) => :a
  (first-key {})          => nil"
  [m]
  `(first (first (seq ~m))))

(defmacro map-first
  "Description:
    The same as `first` function for list.
  
  Examples:  
  (map-first {:a 1 :b 2}) => {:a 1}
  (map-first {})          => nil"
  [m]
  `(if-let[mf# (first (seq ~m))]
     (into {} (list mf#))))

(defmacro map-rest
  "Description:
    The same as `rest` function for list.
  
  Examples:
  (map-rest {:a 1 :b 2 :c 1}) => {:b 2, :c 1}
  (map-rest {})               => nil"
  [m] 
  `(let [mf# (rest (seq ~m))]
     (if (not-empty mf#) (into {} mf#))))

(defmacro map-destruct
  "Description:
    For map return vector of two map, where first
    is head, and second is tail.
  
  Example:
    (map-destruct {:a 1 :b 2 :c 3}) => [{:a 1} {:b 2, :c 3}]
    (map-destruct {:a 1})           => [{:a 1} nil]
    (map-destruct {})               => [nil nil]
  
  See related:
    (`jarman.logic.metadata/map-first`, `jarman.logic.metadata/map-rest`)"
  [m] 
  `(let [sm# ~m]
     (if-let [m-head# (map-first sm#)]
       (let [m-tail# (map-rest sm#)]
         [m-head# m-tail#])
       [nil nil])))

(defmacro cond-contain
  "Description
    Simple macro for easy using pattern-maching on map's

  Example
    (cond-contain {:a 1 :b 2}
      :a (println 1)
      :b (println 2)
      3)"
  [m & body]
  `(condp (fn [kk# mm#] (contains? mm# kk#)) ~m
     ~@body))

(defmacro find-column
  "Descripion
    Short macro for geting first value of lazy seq.
  
  Example
    (find-column #(= % 2) [1 2 3 4])"
  [f col]
  `(first (filter ~f ~col)))

(defmacro map-partial
  "Example
    (map-partial [1 2 3] [:a :b :c]) => [[1 :a] [2 :b] [3 :c]]"
  [f & body] `(map (comp vec concat list) ~@body))

(defmacro get-apply
  "Apply one function to maps
  
  Example
    (get-apply + [:a] {:a 1} {:a 1}) ;; => 2"
  [f path & maps]
  (let [fx-gets (for [m maps] `(get-in ~m ~path nil))] 
    `(~f ~@fx-gets)))

(defn Y-Combinator []
  (((fn [f] (f f))
    (fn [f]
      (fn [s n]
        (if (not (empty? n))
          ((f f) (+ s (second (first n))) (rest n))
          s
          )))) 0 (seq {:a 1 :b 1 :c 1})))

(defn get-key-paths-recur [& {:keys [map-part end-path-func path sequence?]}]
  ;; If `map-part` is nil, then eval function
  ;; end-path-func. Otherwise destruct `map-part`
  ;; for continuing recursion
  (if (nil? map-part) (end-path-func path)
      (let [[head tail] (map-destruct map-part)
            map-first-key (first-key head)
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
  (blet (get-key-paths-recur
         :map-part m
         :end-path-func in-deep-key-path-f
         :path nil
         :sequence? sequence?)
        @in-deep-key-path
        [in-deep-key-path (atom [])
         in-deep-key-path-f (fn [path] (swap! in-deep-key-path (fn [path-list] (conj path-list path ))))]))

(defn deep-merge-with
  "Like merge-with, but merges maps recursively, applying the given fn
  only when there's a non-map at a particular level.
  (deep-merge-with + {:a {:b {:c 1 :d {:x 1 :y 2}} :e 3} :f 4}
                     {:a {:b {:c 2 :d {:z 9} :z 3} :e 100}})
  -> {:a {:b {:z 3, :c 3, :d {:z 9, :x 1, :y 2}}, :e 103}, :f 4}"
  [f & maps]
  (apply
    (fn m [& maps]
      (if (every? map? maps)
        (apply merge-with m maps)
        (apply f maps))) maps))


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


(defn update-existing
  "Updates a value in a map given a key and a function, if and only if the key
  exists in the map. See: `clojure.core/update`."
  {:arglists '([m k f & args])
   :added    "1.1.0"}
  ([m k f]
   (if-let [kv (find m k)] (assoc m k (f (val kv))) m))
  ([m k f x]
   (if-let [kv (find m k)] (assoc m k (f (val kv) x)) m))
  ([m k f x y]
   (if-let [kv (find m k)] (assoc m k (f (val kv) x y)) m))
  ([m k f x y z]
   (if-let [kv (find m k)] (assoc m k (f (val kv) x y z)) m))
  ([m k f x y z & more]
   (if-let [kv (find m k)] (assoc m k (apply f (val kv) x y z more)) m)))

(defn update-existing-in
  "Updates a value in a nested associative structure, if and only if the key
  path exists. See: `clojure.core/update-in`."
  {:added "1.3.0"}
  [m ks f & args]
  (let [up (fn up [m ks f args]
             (let [[k & ks] ks]
               (if-let [kv (find m k)]
                 (if ks
                   (assoc m k (up (val kv) ks f args))
                   (assoc m k (apply f (val kv) args)))
                 m)))]
    (up m ks f args)))

(defn find-first
  "Finds the first item in a collection that matches a predicate. Returns a
  transducer when no collection is provided."
  ([pred]
   (fn [rf]
     (fn
       ([] (rf))
       ([result] (rf result))
       ([result x]
        (if (pred x)
          (ensure-reduced (rf result x))
          result)))))
  ([pred coll]
   (reduce (fn [_ x] (if (pred x) (reduced x))) nil coll)))

(defn dissoc-in
  "Dissociate a value in a nested associative structure, identified by a sequence
  of keys. Any collections left empty by the operation will be dissociated from
  their containing structures."
  ([m ks]
   (if-let [[k & ks] (seq ks)]
     (if (seq ks)
       (let [v (dissoc-in (get m k) ks)]
         (if (empty? v)
           (dissoc m k)
           (assoc m k v)))
       (dissoc m k))
     m))
  ([m ks & kss]
   (if-let [[ks' & kss] (seq kss)]
     (recur (dissoc-in m ks) ks' kss)
     (dissoc-in m ks))))


;;; FUZZY SEARCHING
(comment
  (require '[clj-fuzzy.metrics :as fuzzy])
  (let [searching-word "genera"]
   (->> (return-public-functions 'jarman.logic.security)
        (mapv (comp name symbol))
        (mapv #(vector % (fuzzy/levenshtein searching-word %)))
        (sort-by second))))


(defn return-public-functions [ns]
  {:pre [(symbol? ns)]}
  (filter (comp some? :arglists meta)
          (vals (ns-publics ns))))
