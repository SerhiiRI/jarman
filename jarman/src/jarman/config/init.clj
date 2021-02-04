(ns jarman.config.init
  (:gen-class)
  (:use clojure.reflect
        seesaw.core)
  (:require [clojure.string :as string]
            [jarman.tools.lang :refer :all]
            [clojure.java.io :as io]))


(def ^:dynamic *config-root* "./config")
(def ^:dynamic *config-files* [[:init.edn][:resource.edn][:database.edn][:themes :jarman_light.edn][:themes :theme_config.edn]])
(def *configuration* (atom nil))
(set-validator! *configuration* #(or (nil? %) (map? %)))

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
   (partition 2 kv)))

(defmacro is-block? [m] `(= (:type ~m) :block)) 
(defmacro is-param? [m] `(= (:type ~m) :param)) 
(defmacro is-file? [m] `(= (:type ~m) :file)) 
(defmacro is-directory? [m] `(= (:type ~m) :directory)) 
(defmacro is-error? [m] `(= (:type ~m) :error))

(defn key-strings-path
  [m & {:keys [sequence?] :or {sequence? false}}]
  (blet 
    (get-key-paths-recur
     :map-part m
     :end-path-func in-deep-key-path-f
     :path nil
     :sequence? sequence?)
    @in-deep-key-path
    [in-deep-key-path (atom [])
     in-deep-key-path-f (fn [path]
                          (if (and (string? (get-in m path)) (not= :value (last path)))
                            (swap! in-deep-key-path (fn [path-list] (conj path-list (conj path (get-in m path)))))))]))




(defn get-strings-from-config [m]
 (reduce (fn [acc p](assoc-in acc (butlast p) (last p))){}(key-strings-path m)))
(defn get-strings-from-config-file [file]
  (if-let [m (clojure.edn/read-string (slurp file))]
    (reduce (fn [acc p](assoc-in acc (butlast p) (last p))){}(key-strings-path m))))


(defn build-configuration [config-directory]
  (letfn [(to-file [path] (apply (partial clojure.java.io/file config-directory) (map name path)))
          (D [d v] {:name d :display? :edit :type :directory :value v})
          (F [f v] {:name f :display? :edit :type :file :value v})]
   ([:init.edn] 
    [:resource.edn]
    [:database.edn]
    [:themes :jarman_light.edn]
    [:themes :theme_config.edn])))

(defn create-translation [config-directory]
  (letfn [(to-file [path] (apply (partial clojure.java.io/file config-directory) (map name path)))
          (D [d v]{:name d :display? :edit :type :directory :value v})
          (F [f v]{:name f :display? :edit :type :file :value v})]
    (reduce (fn [acc config-paths]
             (let [f (to-file config-paths)]
               (if (.exists f) acc
                   (let [path (map name config-paths) plen (count path)
                         swapped-config (get-strings-from-config-file f)]
                     (cond
                       (= plen 1) (merge acc (F (first path) swapped-config))
                       (> plen 1) (merge acc (reduce #(D %2 %1) (F (first path) swapped-config) (butlast path))))))))
            {}
            [:init.edn] 
            [:resource.edn]
            [:database.edn]
            [:themes :jarman_light.edn]
            [:themes :theme_config.edn])))

(defmacro is-keyword-list [kv-list]
  `(every? keyword? ~kv-list))
(defmacro is-file-exist [string-path]
  `(.exists (clojure.java.io/file ~string-path)))
(defmacro <path|file>-list
  "Example
    (<path|file>-list *config-root* [[:init.edn][:resource.edn][:database.edn]
                                     [:themes :jarman_light.edn]
                                     [:themes :theme_config.edn]])"
  [config-root key-paths]
  `(for [key-path# ~key-paths
         :let [file-path# (apply (partial clojure.java.io/file ~config-root) (map name key-path#))]]
     [key-path# file-path#]))

(defn load-config-file [path]
  (let [EM (fn [s] {:type :error :log s})]
    (if-not (is-file-exist path)
      (EM (format "Configuration file '%s' not found" (str path)))
      (try (clojure.edn/read-string (slurp (clojure.java.io/file path)))
           (catch Exception e (EM (format "Error parsing configuration file '%s'" (str path))))))))

(do
 (defn swapp-configuration [keyword-config-file-paths]
   (where
    ((PF-list (<path|file>-list *config-root* keyword-config-file-paths))
     (DM (fn [d v] {d {:name (str d) :display? :edit :type :directory :value v}}))
     (FM (fn [f v] {f v})))
    (apply merge (for [[P F :as PF] PF-list
                       :let [L (count PF)]] 
                   (cond (= L 1) (load-config-file F)
                         (> L 1) (reduce #(DM %2 %1) (FM (last P) (load-config-file F)) (butlast P)))))))
 ;; (swapp-configuration *config-files*)
 )







