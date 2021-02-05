(ns jarman.config.init
  (:gen-class)
  (:use clojure.reflect
        seesaw.core)
  (:import (java.io IOException))
  (:require [clojure.string :as string]
            [jarman.tools.lang :refer :all]
            [clojure.java.io :as io]))


(def ^:dynamic *config-root* "./config")
(def ^:dynamic *config-files* [[:init.edn][:resource.edn][:database.edn][:themes :jarman_light.edn][:themes :theme_config.edn]])
;; (def *supported-languages* [:pl :ua])

(def *configuration* (atom nil))
(set-validator! *configuration* #(or (nil? %) (map? %)))

(def *language* (atom nil))
(set-validator! *language* #(or (nil? %) (map? %)))


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
  (defn swapp-*configuration*
    ([] (swapp-*configuration* *config-files*))
    ([keyword-config-file-paths]
     (where
      ((PF-list (<path|file>-list *config-root* keyword-config-file-paths))
       (DM (fn [d v] {d {:name (str d) :display? :edit :type :directory :value v}}))
       (FM (fn [f v] {f v})))
      (reset!
       *configuration*
       (apply merge (for [[P F :as PF] PF-list
                          :let [L (count PF)]] 
                      (cond (= L 1) (load-config-file F)
                            (> L 1) (reduce #(DM %2 %1) (FM (last P) (load-config-file F)) (butlast P))))))
      nil)))
  (swapp-*configuration* *config-files*))




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
  (let [_TMP01 (atom {})]
    (doall
     (for [p (key-strings-path m)]
       (swap! _TMP01 #(assoc-in % (butlast p) (last p)))))
    @_TMP01))




(defn save-cfg-to-file-pp [f m-cfg]
  {:pre [(map? m-cfg)]}
  (try (spit (clojure.java.io/file f) (with-out-str (clojure.pprint/pprint m-cfg)))
       (catch IOException e (println (format "I/O error. Maybe problem in file '%s'" (str f))))
       (catch Exception e (println (format "Configuration serialization problem for config: " (str m-cfg))))))
(defn save-cfg-to-file [f m-cfg]
  {:pre [(map? m-cfg)]}
  (try (spit (clojure.java.io/file f) (pr-str m-cfg))
       (catch IOException e (println (format "I/O error. Maybe problem in file '%s'" (str f))))
       (catch Exception e (println (format "Configuration serialization problem for config: " (str m-cfg))))))


(defn swapp-*language*
  ([]
   (where
    ((language (load-config-file (clojure.java.io/file *config-root* "language.edn"))))
    (reset! *language* language)
    nil)))

(defn config-lang-merge [m path-list-value]
  (where
   ((_TMP01 (atom m)))
   (doall (for [px-t path-list-value
          :let [path (vec (butlast px-t))]]
      (if (nil? (get-in m path nil))
        (swap! _TMP01 #(assoc-in % path (last px-t))))))
   @_TMP01))


(defn refresh-translation []
  (where
   ((original (load-config-file (clojure.java.io/file *config-root* "language.edn")))
    (en-key-path-to-text (key-strings-path @*configuration*))
    (fill-text (fn [CFG] (config-lang-merge CFG en-key-path-to-text))))
   (-> original
       (update-in [:en] fill-text)
       (update-in [:pl] fill-text)
       ((partial save-cfg-to-file-pp (clojure.java.io/file *config-root* "language.edn"))))
   (swapp-*language*)))




