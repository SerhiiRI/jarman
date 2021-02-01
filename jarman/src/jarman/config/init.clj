(ns jarman.config.init
  (:gen-class)
  (:use clojure.reflect
        seesaw.core)
  (:require [clojure.string :as string]
            [jarman.tools.dev-tools]
            [clojure.java.io :as io]))



;;; `TODO` 
;;; 
;;; swap  all file 
;;; validate  all files
{:name "Language"
	 :doc [:cfg :init.edn :value :lang :doc]
	 :type :param
	 :component :text
	 :display :edit
         :value "pl"}
;; (defn old-new [m]
;;   (if (or
;;        (string? value)
;;        (keyword? value)
;;        (number? value)
;;        (vector? value))
;;     )
;;   (if (= :header key))
;;   (if (= :description key))
;;   (if (= :parameters key)))





;; (reduce (fn [aa [kk vv]]
;;           (into aa {kk 
;;                     (reduce (fn [a [k v]]
;;                               (let [x (cond (string? v) {:type :param :display :edit :component :textcolor :value v}
;;                                             (keyword? v) {:type :param :display :edit :component :text :value v}
;;                                             (number? v) {:type :param :display :edit :component :number :value v}
;;                                             :else v)]
;;                                 (into a {k x})))
;;                             {} (seq vv))}))
;;         {}
;;         (seq {:font {:background "#3b8276",
;;                      :background-hover "#77e0cf",
;;                      :foreground "#fff",
;;                      :foreground-hover "#000",
;;                      :horizontal-align :center,
;;                      :cursor :hand,
;;                      :font-style :bold}
;;               }))

(defn block [m]
  {:type :block
   :display :edit
   :value m})

(defn param [v]
  {:type :param :display :edit :component :text :value v})

(defn param-list [& kv]
  (reduce
   (fn [acc [k v]] (into acc {k {:type :param :display :edit :component :text :value v}}))
   {}
   (partition 2 kv))
  )


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
  "Apply one function to 2-4 maps
  
  Example
    (get-apply + [:a] {:a 1} {:a 1}) ;; => 2"
  [f path & maps]
  (let [fx-gets (for [m maps] `(get-in ~m ~path nil))] 
    `(~f ~@fx-gets)))

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
  "Apply one function to 2-4 maps
  
  Example
    (get-apply + [:a] {:a 1} {:a 1}) ;; => 2"
  [f path & maps]
  (let [fx-gets (for [m maps] `(get-in ~m ~path nil))] 
    `(~f ~@fx-gets)))


(defn get-key-paths-recur [& {:keys [map-part end-path-func path sequence?]}]
  ;; If `map-part` is nil, then eval function
  ;; end-path-func. Otherwise destruct `map-part`
  ;; for continuing recursion
  (if (nil? map-part) (end-path-func path)
      (let [[head tail] (map-destruct map-part)
            map-first-key (first-key head)
            value-of-first-key (doto (map-first-key head) println )
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
  [m & {:keys [sequence?] :or {sequence? false}}]
  (let [in-deep-key-path (atom [])
        in-deep-key-path-f (fn [path]
                             (if (and (string? (get-in m path)) (not= :value (last path)))
                               (swap! in-deep-key-path (fn [path-list] (conj path-list (conj path (get-in m path)))))))]
    (get-key-paths-recur
     :map-part m
     :end-path-func in-deep-key-path-f
     :path nil
     :sequence? sequence?)
    @in-deep-key-path))

(defn get-strings-from-config [m]
 (reduce (fn [acc p](assoc-in acc (butlast p) (last p))){}(key-paths m)))

(defn get-strings-from-config-file [file]
  (if-let [m (clojure.edn/read-string (slurp file))]
    (reduce (fn [acc p](assoc-in acc (butlast p) (last p))){}(key-paths m))))

(defmacro file-folder-to-map-path [])

(defn create-translation [config-directory]
  (for-folders config-directory
               [:init.edn]
               [:resource.edn]
               [:database.edn]
               [:themes :jarman_light.edn]
               [:themes :theme_config.edn]))
(get-strings-from-config-file "./config/init.edn")




