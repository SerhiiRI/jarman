(ns jarman.config.config-manager
  (:require [clojure.string :as string]
            [jarman.tools.lang :refer :all]
            [jarman.config.spec :as spec]
            [jarman.config.tools :as tools]
            [jarman.config.init :refer :all]))

;;; Make backup of configurains
(def ^:private backup-configuration @configuration)

;;; accessors 
(defn value-path
  "Description
    Macro is wrapper over get-in with interposing `:value` key in
    `keylist` for geting SEGMENTS VALUES from configuration
    structure map.

  Path
    Exampled keylist [:a :b :c] be transfromed to
     [:a :value :b :value :c :value] path.

  See
    if you need same macro which get whole segment
    please use `segment-path`"
  [keylist]
  {:pre [(vector? keylist)]}
  (conj (vec (interpose :value keylist)) :value))

(defn segment-path
  "Description
    Macro is wrapper over get-in with interposing `:value` key in
    `keylist` for geting WHOLE SEGMENT from configuration
    structure map.

  Path
    Exampled keylist [:a :b :c] be transfromed to
     [:a :value :b :value :c] path.

  See
    if you need same macro which get :value of segment
    please use `value-path`"
  [keylist]
  {:pre [(vector? keylist)]}
  (vec (interpose :value keylist)))

;;;;;;;;;;;;;;;;;
;;; MANAGMENT ;;;
;;;;;;;;;;;;;;;;;

(defn restore-config
  "Description
    Restore global configuraition map,
    if you do something with original
    This function restore configuration
    to point when app was started"
  ([] (reset! configuration backup-configuration)))

(defn restore-from-startup-backup
  "Description
    Restore global configuraition dictionary,
    if you do something with original
    This function restore configuration
    from last validated snapshot from file"
  ([] (restore-backup-configuration)
   (swapp-all)))

(defn swapp
  "Description
    Restore global configuraition dictionary"
  ([] (swapp-all)))


(defn store
  "Description
    Save configuration to configuration directory

  Warning! not recomend to use `m` argument
    way, becouse it can break configuration down"
  ([] (save-all-cofiguration @configuration))
  ([m] (make-backup-configuration)
   (save-all-cofiguration m)))

(defn store-and-back
  "Description
    Save configuration to configuration directory"
  ([] (make-backup-configuration)
   (save-all-cofiguration @configuration)))

(defn validate-store
  "Description
    Run validation mechanism, which test
    configuration path directory, on needed
    file, and check it configurations"
  ([] validate-configuration-files))

(defn validate-segment
  "Description
    If you using 0 arg implementation, validate-segment
     will check whole in-memory stored configuration
    You can also test only one segment of configuration
     structures"
  ([] (spec/valid-segment @configuration))
  ([m] (spec/valid-segment m)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; CONFIGURATION-ACCESSORS ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn make-get-in [getter]
  (fn 
    ([ks] {:pre [(vector? ks)]}
     (get-in @configuration (getter ks) nil))
    ([ks not-found]
     {:pre [(vector? ks)]}
     (get-in @configuration (getter ks) not-found))))

(defn make-assoc-in [getter]
  (fn 
    ([ks value] {:pre [(vector? ks)]}
     (swap! configuration
            (fn [cfg] (assoc-in cfg (getter ks) value))))))

(defn make-update-in [getter]
  (fn 
    ([ks f] {:pre [(vector? ks)]}
     (swap! configuration
            (fn [cfg] (update-in cfg (getter ks) f))))))

(defn make-delete-in [getter]
  (fn 
    ([ks] {:pre [(vector? ks)]}
     (swap! configuration
            (fn [cfg] (assoc-in cfg (getter ks) nil))))))

;; get-in
(def get-in-value (make-get-in value-path))
(def get-in-segment (make-get-in segment-path))
;; assoc-in 
(def assoc-in-value (make-assoc-in value-path))
(def assoc-in-segment (make-assoc-in segment-path))
;; update-in 
(def update-in-value (make-update-in value-path))
(def update-in-segment (make-update-in segment-path))
;; delete-in
(def delete-in-value (make-delete-in value-path))
(def delete-in-segment (make-delete-in segment-path))

;; (restore-config)
;; (get-in-segment [:database.edn :datalist :localhost :dbtype])
;; (get-in-value [:database.edn :datalist :localhost :dbtype])
;; (delete-in-value [:database.edn :datalist :localhost :dbtype])
;; (assoc-in-value [:database.edn :datalist :localhost :dbtype] "DUDUDU")
;; (update-in-value [:database.edn :datalist :localhost :dbtype] (fn [x] (format "<h1>%s</h1>" x)))

;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Mapping/Coverting ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- --mapper-cfg-struct [datalist-segment]
  (->> datalist-segment
       (map (fn [[k v]]
              (if (= (:type v) :param)
                {k (:value v)}
                {k (--mapper-cfg-struct (:value v))})))
       (reduce into)))

(defn mapping-from-segment
  "Description
    Get simple map key-value scheme from jarman
    configuration notation, omiting all parameters
    and Segment strcuture. Reduct block-value-block-value
    paths to value-value notation
    For example
     {:localhost
      {:name \"Local.. :type :block, ...
       :value
       {:dbtype {:name \"Typ po��czenia\", :type :param,
                 :display :none, :component :text,
                 :value \"ALALALALALA\"},
        :host ...
    Be converted to
     {:localhost
      {:dbtype \"ALALALALALA\"
       :host \"127.0.0.1\"...}
      :production
      {...

    See
     `mapping-to-segment` function save this simpliced
      configuration to real configuration structure"
  [ks]
  {:pre [(vector? ks)]}
  (--mapper-cfg-struct (get-in-value ks)))

(defn mapping-to-segment
  "Description
    Save to configuration strucutre to real configuration
    by do demapping short-wroted parameter to whole keys
    For exmaple mapped(compressed) structure
     {:localhost
      {:dbtype \"ALALALALALA\"
       :host \"127.0.0.1\"...}
      :production
      {...
    Will be set to full lenght keys
     {:localhost
      {:name \"Local.. :type :block, ...
       :value
       {:dbtype {:name \"Typ po��czenia\", :type :param,
                 :display :none, :component :text,
                 :value \"ALALALALALA\"},
        :host ..."
  [ks mapped-cfg]
  (doall (map #(assoc-in-value
                (join-vec ks %)
                (get-in mapped-cfg %)) 
              (key-paths mapped-cfg))))

;;;;;;;;;;;;;;;;
;;; LANGUAGE ;;;
;;;;;;;;;;;;;;;;

(defn make-lang-get-in [getter]
  (fn 
    ([ks] {:pre [(vector? ks)]}
     (get-in @language (getter ks) nil))
    ([ks not-found]
     {:pre [(vector? ks)]}
     (get-in @language (getter ks) not-found))))

(defn make-lang-assoc-in [getter]
  (fn 
    ([ks value] {:pre [(vector? ks)]}
     (swap! language
            (fn [cfg] (assoc-in cfg (getter ks) value))))))

(defn make-lang-update-in [getter]
  (fn 
    ([ks f] {:pre [(vector? ks)]}
     (swap! language
            (fn [cfg] (update-in cfg (getter ks) f))))))

(defn make-lang-delete-in [getter]
  (fn 
    ([ks] {:pre [(vector? ks)]}
     (swap! language
            (fn [cfg] (assoc-in cfg (getter ks) nil))))))

;; get-inf
(def get-in-lang (fn 
                   ([ks] (get-in @language ks nil))
                   ([ks not-found] (get-in @language ks not-found))))
(def get-in-lang-value (make-lang-get-in value-path))
(def get-in-lang-segment (make-lang-get-in segment-path))
;; assoc-in 
(def assoc-in-lang-value (make-lang-assoc-in value-path))
(def assoc-in-lang-segment (make-lang-assoc-in segment-path))
;; update-in 
(def update-in-lang-value (make-lang-update-in value-path))
(def update-in-lang-segment (make-lang-update-in segment-path))
;; delete-in
(def delete-in-lang-value (make-lang-delete-in value-path))
(def delete-in-lang-segment (make-lang-delete-in segment-path))

;;;;;;;;;;;;;;;;;
;;; searching ;;;
;;;;;;;;;;;;;;;;;

(defn search-by-all [pred]
  (let [a (atom [])
        f (fn [block path]
            (if (pred block)
              (swap! a #(conj % path))))]
    (tools/recur-walk-throw @configuration f [])
    @a))

(defn search-by-type [stype]
  (let [a (atom [])
        f (fn [block path]
            (if (= (:type block) stype)
              (swap! a #(conj % path))))]
    (tools/recur-walk-throw @configuration f [])
    @a))

(defn- walk-conf [pred func]
  (let [f (fn [block path]
            (if (pred block path) (func block path)))]
    (tools/recur-walk-throw @configuration f [])
    nil))

(defn listing-all
  ([] listing-all nil)
  ([func]
   (let [a (atom [])
         f (fn [block path]
             (swap! a 
                   #(conj %
                     {:block (:type block)
                      :path path})))]
     (tools/recur-walk-throw @configuration f [])
     (if func (map func @a) @a))))

(defn printing-all []
  (listing-all #(println (:block %) (:path %))))

;;;;;;;;;;;;
;;; INFO ;;;
;;;;;;;;;;;;

(defn spec-info-access [] spec/segment-display)
(defn spec-info-segment [] spec/segment-type)
(defn spec-info-param-component []
  {:all spec/parameter-components
   :url {:one {:type spec/$texturl :value "https://localhost/some/url"}
         :list {:type spec/$listurl :value ["https://localhost"]}} 
   :text {:one {:type spec/$text :value "some-text"}
          :list {:type [spec/$listbox spec/$selectbox spec/$textlist]
                 :value ["1" "2" "labladudj"]}}
   :number {:one {:type spec/$textnumber :value 20}
            :list {:type spec/$numberlist :value [1 2 3 4]}}
   :color {:one {:type spec/$textcolor :value "#fff123"}}
   :checkbox {:one {:type spec/$checkbox :value true}}}) 

(defn spec-error-block [log]
  {:pre [(string? log)]} {:type :error :log log})
(defn spec-directory-block [dir-name value]
  {:pre [(string? dir-name)]} {:name dir-name :display :edit :type :directory :value value})
(defn spec-file-block [file-name value]
  {:pre [(string? file-name)]} {:name file-name :display :edit :type :file :value value})
(defn spec-make-segment [name doc display type value]
  {:name name :doc doc :display display :type type :value value})
(defn spec-make-param [name doc component display value]
  {:name name :doc doc :type :param :component component :display display :value value})

