;;;  _
;;; | | __ _ _ __   __ _
;;; | |/ _` | '_ \ / _` |
;;; | | (_| | | | | (_| |
;;; |_|\__,_|_| |_|\__, |
;;;                |___/
;;; ---------------------
(ns jarman.lang
  ;; (:use seesaw.core)
  (:require [clojure.reflect :as reflect]
            [clojure.string :as string]
            [clojure.pprint :as pprint]
            [clojure.java.io :as io]))

;;;;;;;;;;;;;;;;;;;;;;;
;;; helper function ;;;
;;;;;;;;;;;;;;;;;;;;;;;

(defmacro catch-component-trace [message]
  `(do
     (println "_______________________________")
     (println ~message)
     (let [[~'trace-head & ~'trace-rest]
           (doall (->> (seq (.getStackTrace (Thread/currentThread)))
                    (map #(.getClassName %))
                    (filter (partial re-matches #"jarman.*"))
                    (distinct)))]
       (println "-> " ~'trace-head)
       (print (pprint/cl-format nil "~{~A~%~}"~'trace-rest)))
     (println ".")))

(defn slurp-lines [f]
  (-> (slurp f)
      (clojure.string/split-lines)))

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
   (rift [1]  \"zero\")    => [1]
   (rift (- 3 2) \"zero\") => 1
   (rift \"A\"   \"zero\") => \"A\"
   (rift #object \"zero\") => #object"
  [con els]
  (cond
    (nil? con)        els
    (number? con)     con
    (sequential? con) (if (empty? con) els con)
    (string? con)     (if (empty? con) els con)
    (map? con)        (if (empty? con) els con)
    :else con))

(defn coll-to-map
  "Description:
     Convert collection to map.
   Example:
     (coll-to-map [:a 2 :b 4])) => {:a 2 :b 4}
     (coll-to-map (list 1 2 3)) => IllegalArgumentException
     (coll-to-map nil) => {}"
  [coll]
  (apply hash-map coll))

(defn coll-of-keys->map
  "Description:
    vector empty map
    Create from vector or list map with nil value.
    Second arg - default value for map.
  Example:
    (coll-of-keys->map [:a :b])
      => {:a nil, :b nil}
    (coll-of-keys->map [:a :b] \"nice\")
      => {:a \"nice\", :b \"nice\"}"
  ([coll] (coll-of-keys->map coll nil))
  ([coll val] (into {} (map (fn [k] {k val}) coll))))

(defn rm-nil-in-map
  "Description:
     cut nil map
     Remove values with nil in map.
   Example:
     (cnmap {:a nil :b \"a\"}) => {:b \"a\"}"
  [coll-map]
  (into {} (filter (comp some? val) coll-map)))

(defmacro join
  "(join \",\" [1 2 3]) ;=> '1,2,3'"
  [delimiter col]
  `(clojure.string/join ~delimiter ~col))

(defmacro split
  "(filter-nil [nil 1 nil 3 4]) ;=> [1 3 4]"
  [delimiter s]
  `(clojure.string/split ~s ~delimiter ))

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


(defmacro get-apply
  "Apply one function to maps

  Example
    (get-apply + [:a] {:a 1} {:a 1}) ;; => 2"
  [f path & maps]
  (let [fx-gets (for [m maps] `(get-in ~m ~path nil))]
    `(~f ~@fx-gets)))

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

(defn sort-by-index
  "Example
   (sort-by-index identity [3 1 2 0] [\\d \\b \\c \\a])
    ;; => {0 \\a, 1 \\b, 2 \\c, 3 \\d}"
  [f index-col v-col]
  {:pre [(= (count index-col) (count v-col))]}
  (into (sorted-map) (zipmap index-col v-col)))

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

(defn return-public-functions [ns]
  {:pre [(symbol? ns)]}
  (filter (comp some? :arglists meta)
          (vals (ns-publics ns))))

(defmacro copy-locals [& vars]
  `(hash-map ~@(mapcat (fn [x] [(keyword (name x)) x]) vars)))

;;;;;;;;;;;;;;;;
;;; REFLECTS ;;;
;;;;;;;;;;;;;;;;

(defn inspect-object-methods
  "Print methods for object/class, in argument"
  [object]
  (->> object
       reflect/reflect :members (filter :return-type) (map :name) sort (map #(str "." %))
       distinct (map symbol)))

(defn inspect-object
  "nicer output for reflecting on an object's methods"
  [object]
  (let [reflection (reflect/reflect object)
        members (sort-by :name (:members reflection))]
    (println "Class:" (.getClass object))
    (println "Bases:" (:bases reflection))
    (println "---------------------\nConstructors:")
    (doseq [constructor (filter #(instance? clojure.reflect.Constructor %) members)]
      (println (:name constructor) "(" (string/join ", " (:parameter-types constructor)) ")"))
    (println "---------------------\nMethods:")
    (doseq [method (filter #(instance? clojure.reflect.Method %) members)]
      (println (:name method) "(" (string/join ", " (:parameter-types method)) ") ;=>" (:return-type method)))))
