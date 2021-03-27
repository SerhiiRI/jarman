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
(ns jarman.tools.lang
  (:use clojure.reflect seesaw.core)
  (:require [clojure.string :as string]
            [clojure.java.io :as io]))


;;;;;;;;;;;;;;;;;;;;;;;
;;; helper function ;;;
;;;;;;;;;;;;;;;;;;;;;;;

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

(def all-vec-to-floor
  "(all-vec-to-floor ([:a] ([:d :x] [:e :y] ([:z])))) ;; => ([:a] [:d :x] [:e :y] [:z])"
  (fn [vects]
    (do
      (defn r-unwrapper [result example]
        (reduce #(if (vector? %2)
                   (conj %1 %2)
                   (concat %1 (r-unwrapper [] %2))) result example))
      (r-unwrapper [] vects))))

(defn key-to-title [key] (-> (string/replace (str key) #":" "") (string/replace  #"[-_]" " ") (string/replace  #"^." #(.toUpperCase %1))))
(defn txt-to-title [txt] (-> (string/replace (str txt) #":" "") (string/replace  #"[-_]" " ") (string/replace  #"^." #(.toUpperCase %1))))
(defn txt-to-UP [txt] (-> (string/replace (str txt) #":" "") (string/replace  #"[-_]" " ") (string/replace  #"." #(.toUpperCase %1))))
(defn rm-colon [txt] (-> (string/replace (str txt) #":" "")))
(defn convert-mappath-to-key [path] (keyword (rm-colon (string/join "-" path))))
(defn convert-str-to-hashkey [str] (keyword (string/join "" ["#" str])))

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
       reflect :members (filter :return-type) (map :name) sort (map #(str "." %) ) distinct println))

(def random-unique-id (fn [] (string/join "" [(java.time.LocalDateTime/now) (rand-int 10000)])))

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

;;;;;;;;;;;;;;;;;;;
;;; where macro ;;;
;;;;;;;;;;;;;;;;;;;

(def ^:private recursive-linier-terms
  [{:term 'reduce :arg 2}
   {:term 'map :arg 1}
   {:term 'filter :arg 1}
   {:term 'if :arg 2}
   {:term 'do :arg 1}
   {:term '|> :arg 1}
   {:term 'doto :arg 1}
   {:term 'ift :arg 1}
   {:term 'ifn :arg 1}
   {:term 'ifp :arg 2}
   {:term 'iff1 :arg 2}
   {:term 'iff2 :arg 3}
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
    'ift `(if ~(last args) ~(first args) ~(last args))
    'ifn `(if ~(last args) ~(last args) ~(first args))
    'ifp `(if (~(first args) ~(last args)) ~(last args) ~(second args))
    'iff1 `(if (~(first args) ~(last args)) ~(last args) (~(second args) ~(last args)))
    'iff2 `(if (~(first args) ~(last args)) (~(second args) ~(last args)) (~(nth args 2 #'identity) ~(last args)))
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
                        (recursive-linier-preprocessor temporary# ~body))))))))))

(defmacro where-binding-form [binding-form]
  `[~(first binding-form) (recursive-linier-preprocessor ~(first (rest binding-form)) ~(rest (rest binding-form)))])

(defmacro where
  "Description
    Let on steroids

  Example 
   (where ((temp 10 do string? otherwise \"10\") ;; \"10\"
           (temp nil ifn 4) ;; 4
           (temp nil ift 4) ;; nil
           (temp nil ifp nil? 4) ;; nil
           (temp 3 iff1 nil? inc) ;; 3
           (temp 3 iff2 odd? inc dec) ;; 4
           (temp 3 ifp odd?4 ) ;; 3
           (temp 3 do inc do inc) ;; 5
           (temp 3 |> inc |> inc) ;; 5
           (temp 4)
           (temp (range 10) map #(- % 5) filter #(< 0 %) do count ifn 0) ;; 4
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
  `(if-let [mf# (first (seq ~m))]
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


