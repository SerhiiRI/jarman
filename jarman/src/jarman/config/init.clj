(ns jarman.config.init
  (:gen-class)
  (:use clojure.reflect)
  (:import (java.io IOException))
  (:require [clojure.string :as string]
            [jarman.tools.lang :refer :all]
            [jarman.config.spec :as spec]
            [jarman.config.environment :as env]
            [jarman.config.storage :as storage]
            [me.raynes.fs :as fs]
            [clojure.java.io :as io]))

;;;;;;;;;;;;;;;;;;;;;;
;;; CONFIG PRESETS ;;;
;;;;;;;;;;;;;;;;;;;;;;


(def ^:dynamic *config-root* env/jarman-standart-config)
(def ^:dynamic *config-themes* "themes")
(def ^:dynamic *config-language* "language.edn")
(def ^:dynamic *config-files* [[:init.edn] [:resource.edn] [:database.edn] [(keyword *config-themes*) :theme_config.edn]])
;; (def ^:dynamic *config-on-valid* (conj ))


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


(defn filepath-to-keypath
  "Description
  Example
    (filepath-to-keypath \".\\config\\themes\\theme_config.edn\")
      ;; => [:themes :theme_config.edn]"
  [path]
  (where
   ((s-parts (string/split path #"\\|/")
             iff2 #(= (first %) ".") rest identity
             iff2 #(= (first %) (.getName (clojure.java.io/file *config-root*))) rest identity
             map keyword
             do vec))
   s-parts))

(defn- <path|file>-list
  "Example
    (<path|file>-list *config-root* [[:init.edn][:resource.edn][:database.edn]
                                     [:themes :jarman_light.edn]
                                     [:themes :theme_config.edn]])"
  ([config-root]
   (where
    ((directory (clojure.java.io/file config-root)) 
     (dir? #(.isDirectory %))
     (edn? #(string/ends-with? (.getName %) ".edn"))
     (clj? #(string/ends-with? (.getName %) ".clj"))
     (path-file-pair #(vector (filepath-to-keypath (.getPath %)) %)))
    (vec
     (map path-file-pair
          (filter (every-pred edn? (comp not dir?))
                  (tree-seq dir? #(.listFiles %) directory))))))
  ([config-root key-paths]
   (for [key-path key-paths
         :let [file-path (apply (partial clojure.java.io/file config-root) (map name key-path))]]
      [key-path file-path])))

(defn load-config-file [path]
  (let [EM (fn [s] (spec/error-block s))]
    (if-not (is-file-exist path)
      (EM (format "Configuration file '%s' not found" (str path)))
      (try (where ((cfg (clojure.edn/read-string (slurp (clojure.java.io/file path)))))
                  (if (map? cfg) cfg (EM (format "File '%s' has some type or structural problems" (str path)))))
           (catch Exception e (EM (format "Error parsing configuration file '%s'" (str path))))))))

(defn load-stylesheet [path]
  (let [EM (fn [s] (spec/error-block s))]
    (if-not (is-file-exist path)
      (EM (format "Configuration file '%s' not found" (str path)))
      (try (where ((cfg (load-file (str (clojure.java.io/file path)))))
                  (if (map? cfg) cfg (EM (format "File '%s' has some type or structural problems" (str path)))))
           (catch Exception e (EM (format "Error parsing configuration file '%s'" (str path))))))))

(defmacro ^:private GETV
  "Description
    Macro is wrapper over get-in with interposing `:value` key in
    `keylist` for geting SEGMENTS VALUES from configuration
    structure map.

  Path
    Exampled keylist [:a :b :c] be transfromed to
     [:a :value :b :value :c :value] path.

  See
    if you need same macro which get whole segment
    please use `GETS`"
  [m keylist]
  `(get-in ~m (conj (vec (interpose :value ~keylist)) :value) nil))

(defmacro ^:private GETS
  "Description
    Macro is wrapper over get-in with interposing `:value` key in
    `keylist` for geting WHOLE SEGMENT from configuration
    structure map.

  Path
    Exampled keylist [:a :b :c] be transfromed to
     [:a :value :b :value :c] path.

  See
    if you need same macro which get :value of segment
    please use `GETV`"
  [m keylist]
  `(get-in ~m (vec (interpose :value ~keylist)) nil))

;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; CONFIG INITIALIZERS ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn load-current-theme [M]
  (where
   ((CURRENT-THEME (GETV M [:themes :theme_config.edn :selected-theme]) iff1 nil? first otherwise "jarman_light")
    (FILE-LINK CURRENT-THEME do (partial format "%s.edn" ) do #(clojure.java.io/file *config-root* *config-themes* %)))
   (assoc-in M [:themes :value :current-theme] (load-config-file FILE-LINK))))

;; (defn load-stylesheet [path]
;;   (let [EM (fn [s] (spec/error-block s))]
;;     (if-not (is-file-exist path)
;;       (EM (format "Configuration file '%s' not found" (str path)))
;;       (try (where ((cfg (load-file (str (clojure.java.io/file path)))))
;;                   (if (map? cfg) cfg (EM (format "File '%s' has some type or structural problems" (str path)))))
;;            (catch Exception e (EM (format "Error parsing configuration file '%s'" (str path))))))))

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
  (if (not= false (:valid? @configuration))
    (where
     ((LANG-KEYWORD (GETV @configuration [:init.edn :lang]) do first))
     (if LANG-KEYWORD
       (where
        ((IS-ERROR? (fn [languages] (= :error (:type languages))))
         (l (load-config-file (clojure.java.io/file *config-root* *config-language*)) iff1 IS-ERROR? LANG-KEYWORD))
        (reset! language l)))))nil)

(defn swapp-all
  "Description
    Main function in module wich load all configurations to variables
    First of all evaluate this fucntion, after you can get configuration
    variables:
     `configuration`
     `language`

  See
    `swapp-configuration`
    `swapp-language`" []
  (swapp-configuration)
  (swapp-language)
  nil)

(swapp-all)
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
(defn save-all-cofiguration [m]
  {:pre [(map? m)]}
  (let [cfg (spec/valid-segment m)]
    (if (:valid? cfg)
      (do 
        (doall
         (map (fn [[path file]] (save-cfg-to-file-pp file (GETS m path)))
              (<path|file>-list *config-root* *config-files*))) 
        cfg)
      cfg)))

;;;;;;;;;;;;;;;;;;;;;;
;;; BACKUP SYSTEMS ;;;
;;;;;;;;;;;;;;;;;;;;;;

(def ^:private backup-name "configurations")
(def ^:private backup-file-name (format "%s.edn" backup-name))
(def ^:private backup-file-date-format "YYYY-MM-dd HH:mm:ss")

(defn- backup-swapp-configuration
  "Example
    (backup-swapp-configuration (<path|file>-list *config-root*))"
  [PF-list]
  (where
   ((merge-config-strategy #(last %&))
    (configuration-merger #(apply (partial deep-merge-with merge-config-strategy) %))
    (DM (fn [d v] {d (spec/directory-block (name d) v)}))
    (FM (fn [f v] {f v}))
    (cfg
     (configuration-merger
      (for [[P F :as PF] PF-list :let [L (count PF)]]
        (cond (= L 1) (load-config-file F)
              (> L 1) (reduce #(DM %2 %1) (FM (last P) (load-config-file F)) (butlast P)))))))
   cfg))

(defn- backup-information []
  {:date (.format (java.text.SimpleDateFormat. backup-file-date-format) (java.util.Date.))
   :program-dir env/user-dir})

(defn- backup-keep-10-last-modified
  "Remove 10 last modified backup files, when new backups being created"[]
  (let [max-bkp 10 l-files (storage/user-config-list) c-files (count l-files)]
    (if (> c-files max-bkp) 
      (doall (map #(-> % .getName storage/user-config-delete)
                  (take (- c-files max-bkp) 
                        (sort-by #(-> % .lastModified java.util.Date.)
                                 #(.before %1 %2)
                                 (map io/file l-files))))))))

(defn make-backup-configuration
  "Description
    Make backup files 'backup.edn', if file was created
    before, rename and add timestamp to name in format
    YYYY-MM-dd_HHmmss 'backup_2021-03-22_004353.edn'

  Example
    (make-backup-configuration)
  
  Warning!
    Timestamp related to event when your backup became
    a old, and replace to new. Timestamp not mean Time
    of backup creation

  See
    `backup-keep-10-last-modified` function which delete
      oldest file from all backup snapshots, if number
      of backups will reach 10 files" []
  (let [path-file-list (<path|file>-list *config-root*)
        path-list (vec (map first path-file-list))
        tmp-swapped-config (backup-swapp-configuration path-file-list)
        make-backup #(identity {:info (backup-information) :path %1 :backup %2})]
    (if (.exists (io/file (storage/user-config-dir) backup-file-name))
      (let [old-file (clojure.pprint/cl-format nil "~A_~A.edn"
                                               backup-name
                                               (.format (java.text.SimpleDateFormat. "YYYY-MM-dd_HHmmss") (java.util.Date.)))]
        (storage/user-config-rename backup-file-name old-file)))
    (storage/user-config-put backup-file-name (str (make-backup path-list tmp-swapped-config)))
    (backup-keep-10-last-modified)))

(defn- default-backup-loader []
  (if-let [_TMP0 (storage/user-config-get backup-file-name)] _TMP0
    (try (slurp (io/file env/user-dir backup-file-name))
         (catch Exception e nil))))

(defn restore-backup-configuration
  "Description
    Restore all backups from user-stored buffer

  Example
    (restore-backup-configuration)
    (restore-backup-configuration default-backup-loader)"
  ([] (restore-backup-configuration default-backup-loader))
  ([f-backup-loader]
   (if-let [backup (f-backup-loader)]
     (try (let [_cfg (read-string backup) cfgs (:backup _cfg) pths (:path _cfg)]
            (map (fn [[path file]]
                   (let [-swapped-file-cfg- (GETS cfgs path)]
                     (when-not (.exists file)
                       (-> file io/file .getParentFile .mkdirs)
                       (save-cfg-to-file-pp file -swapped-file-cfg-))))
                 (<path|file>-list *config-root* pths)))
          (catch Exception e false)))))

;; (make-backup-configuration)
;; (restore-backup-configuration default-backup-loader)
;; (storage/user-config-clean)

(defn validate-configuration-files
  "Description
    For list of all configuration files
    test of their existing in ./config
    directory. Function test only `*config-files*`
    and `*config-language*` files

  Example  
    (validate-configuration-files)

  See
    `jarman.config.init/*config-files*`
       List of configuration files
    `jarman.config.init/*config-language*`
       Translation file" []
  (every? true?
   (map
    #(.exists (second %))
    (<path|file>-list
     *config-root*
     (conj *config-files* [(keyword *config-language*)])))))

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



