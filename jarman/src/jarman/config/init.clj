(ns jarman.config.init
  (:gen-class)
  (:use clojure.reflect)
  (:import (java.io IOException))
  (:require [clojure.string :as string]
            [jarman.tools.lang :refer :all]
            [jarman.config.spec :as spec]
            [clojure.java.io :as io]))

;;;;;;;;;;;;;;;;;;;;;;
;;; CONFIG PRESETS ;;;
;;;;;;;;;;;;;;;;;;;;;;

(def ^:dynamic *config-root* "./config")
(def ^:dynamic *config-themes* "themes")
(def ^:dynamic *config-language* "language.edn")
(def ^:dynamic *config-files* [[:init.edn] [:resource.edn] [:database.edn] [(keyword *config-themes*) :theme_config.edn]])

;;;;;;;;;;;;;;;;;;;;;;
;;; MAIN VARIABLES ;;;
;;;;;;;;;;;;;;;;;;;;;;

(def configuration (atom nil))
(set-validator! configuration #(or (nil? %) (map? %)))

(def language (atom nil))
(set-validator! language #(or (nil? %) (map? %)))


;;;;;;;;;;;;;;;;;;;;;;
;;; File logistics ;;;
;;;;;;;;;;;;;;;;;;;;;;

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
  (let [EM (fn [s] (spec/error-block s))]
    (if-not (is-file-exist path)
      (EM (format "Configuration file '%s' not found" (str path)))
      (try (where ((cfg (clojure.edn/read-string (slurp (clojure.java.io/file path)))))
                  (if (map? cfg) cfg (EM (format "File '%s' has some type or structural problems" (str path)))))
           (catch Exception e (EM (format "Error parsing configuration file '%s'" (str path))))))))

(defmacro ^:private GETC [m & keylist]
  `(get-in ~m (conj (vec (interpose :value [~@keylist])) :value) nil))

;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; CONFIG INITIALIZERS ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn load-current-theme [M]
  (where
   ((CURRENT-THEME (GETC M :themes :theme_config.edn :selected-theme) iff1 nil? first otherwise "jarman_light")
    (FILE-LINK CURRENT-THEME do (partial format "%s.edn" ) do #(clojure.java.io/file *config-root* *config-themes* %)))
   (assoc-in M [:themes :value :current-theme] (load-config-file FILE-LINK))))

(defn swapp-configuration
  ([] (swapp-configuration *config-files*))
  ([keyword-config-file-paths]
   (where
    ((merge-config-strategy #(last %&))
     (configuration-merger #(apply (partial deep-merge-with merge-config-strategy) %))
     (PF-list (<path|file>-list *config-root* keyword-config-file-paths))
     (DM (fn [d v] {d (spec/directory-block (name d) v)}))
     (FM (fn [f v] {f v}))
     ;; Loading pipeline 
     (cfg
      (configuration-merger
       (for [[P F :as PF] PF-list
             :let [L (count PF)]]
         (cond (= L 1) (load-config-file F)
               (> L 1) (reduce #(DM %2 %1) (FM (last P) (load-config-file F) ) (butlast P))))))
     (cfg (load-current-theme cfg) otherwise cfg)
     ;; End-stage validators
     (cfg (spec/valid-segment cfg)
          do (fn [result] (if (:valid? result) cfg result))
          otherwise nil))
    (reset! configuration cfg)
    nil)))

(defn swapp-language []
  (if (nil? @configuration) (swapp-configuration))
  (if (map? @configuration)
   (where
    ((LANG-KEYWORD (GETC @configuration :init.edn :lang))
     (IS-ERROR? (fn [languages] (= :error (:type languages))))
     (l (load-config-file (clojure.java.io/file *config-root* *config-language*)) iff1 IS-ERROR? LANG-KEYWORD))
    (reset! language l)
    nil)))

(defn swapp-all
  "Description
    Main function in module wich load all configurations to variables
    First of all evaluate this fucntion, after you can get configuration
    variables:
     `configuration`
     `language`

  See
    `swapp-configuration`
    `swapp-language`
  " []
  (swapp-configuration)
  (swapp-language)
  nil)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; PERSIST CONFIG FUNCTION ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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

;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; TRANSLATION TOOLKIT ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn key-strings-path
  "Description
    Recursively return only key-path's to String values

  Example 
    (key-strings-path
     {:key {:k_string \"123\"
            :k_number 123
            :k_map {:str \"321\"
                    :number 3}}})
      ;;=> [[:key :k_string \"123\"]
            [:key :k_map :str \"321\"]]"
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


(defn get-strings-from-config
  "Description
    Return map only with string's values of `m` 

  Example
   (get-strings-from-config
     {:key {:k_string \"123\"
            :k_number 123
            :k_map {:str \"321\"
                    :number 3}}})
     {:key {:k_string \"123\", :k_map {:str \"321\"}}}

  See
    `key-strings-path`"
  [m]
  (let [_TMP01 (atom {})]
    (doall
     (for [p (key-strings-path m)]
       (swap! _TMP01 #(assoc-in % (butlast p) (last p)))))
    @_TMP01))


(defn- config-lang-merge [m path-list-value]
  (where
   ((_TMP01 (atom m)))
   (doall (for [px-t path-list-value
                :let [path (vec (butlast px-t))]]
            (if (nil? (get-in m path nil))
              (swap! _TMP01 #(assoc-in % path (last px-t)))))) @_TMP01))


(defn refresh-translation []
  (where
   ((IS-ERROR? (fn [languages] (= :error (:type languages))))
    (lang-file (clojure.java.io/file *config-root* *config-language*))
    (original (load-config-file lang-file) do {:type :error} doto println ifp IS-ERROR? {})
    (en-key-path-to-text (key-strings-path @configuration))
    (fill-text (fn [CFG] (config-lang-merge CFG en-key-path-to-text))))
   (-> original
       (update-in [:en] fill-text)
       (update-in [:pl] fill-text)
       ((partial save-cfg-to-file-pp lang-file)))
   (swapp-language)))



