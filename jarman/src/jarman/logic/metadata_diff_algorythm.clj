(ns jarman.logic.metadata-diff-algorythm
  (:require [jarman.logic.connection :as db]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; TABLE-MAP VALIDATOR ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def valid-output-lang
  {:eng {:value-not-exit-in-table "Value not exist in table-map"
         :value-not-valid-on-p "Value '%s' not valid by predicate '%s'"
         :value-not-valid-on-eq "Value '%s' not valid on '%s' equalization pattern"
         :value-not-in-allow-list "Value '%s' not from allowed list '%s'"
         :value-not-valid-on-re "Value '%s' not valid on regexp '%s' pattern"}
   :pl {:value-not-exit-in-table "Wartoďż˝ďż˝ nie istanieje w tabeli"
        :value-not-valid-on-p "Value '%s' not valid by predicate '%s'"
        :value-not-valid-on-eq "Value '%s' not valid on '%s' equalization pattern"
        :value-not-in-allow-list "Value '%s' not from allowed list '%s'"
        :value-not-valid-on-re "Value '%s' not valid on regexp '%s' pattern"}
   :ua {:value-not-exit-in-table "Value not exist in table-map"
        :value-not-valid-on-p "Value '%s' not valid by predicate '%s'"
        :value-not-valid-on-eq "Value '%s' not valid on '%s' equalization pattern"
        :value-not-in-allow-list "Value '%s' not from allowed list '%s'"
        :value-not-valid-on-re "Value '%s' not valid on regexp '%s' pattern"}})

(def valid-output (:eng valid-output-lang))
(def ^:dynamic not-valid-string-f
  "Lambda for working with validator output map

  Example of argument
   {:path [:some :path]
    :msg \"Error message\"}"
  (fn [m] (println m)))

(defmacro ^{:private true} >>=
  ([x] x)
  ([x f]
   `(let [x# (~f ~x)] (if (nil? x#) nil x#)))
  ([x f & f-list]
   `(let [x# (~f ~x)]
      (if (nil? x#) nil
          (>>= x# ~@f-list)))))
(defmacro ^{:private true} isset? [m key-path & [key-path-preview]]
  `(if (nil? (get-in ~m [~@key-path] nil))
     (do (not-valid-string-f {:path (vec (if ~key-path-preview ~key-path-preview ~key-path))
                              :message (format "Value not exist in table-map")}) false)
     true))
(defmacro ^{:private true} fpattern? [f-pattern msg m key-path & [key-path-preview]]
  `(let [value# (get-in ~m [~@key-path] nil)
         path# (if ~key-path-preview ~key-path-preview ~key-path)]
     (if (~f-pattern value#) true
         (do (not-valid-string-f {:path path# :message (format "Value '%s' not valid by predicate '%s'" (str value#) ~msg)}) false))))
(defmacro ^{:private true} ispattern? [match m key-path & [key-path-preview]]
  `(let [value# (get-in ~m [~@key-path] nil)
         path# (if ~key-path-preview ~key-path-preview ~key-path)]
     (if (nil? value#) false
         (if (= value# ~match) true
             (do (not-valid-string-f
                  {:path path# :message (format "Value '%s' not valid on '%s' equalization pattern"  (str value#) (str ~match))}) false)))))
(defmacro ^{:private true} inpattern? [match-list m key-path & [key-path-preview]]
  `(let [value# (get-in ~m [~@key-path] nil)
         path# (if ~key-path-preview ~key-path-preview ~key-path)]
     (if (nil? value#) false
         (if (in? [~@match-list] value#) true
             (do (not-valid-string-f {:path path# :message (format "Value '%s' not from allowed list '%s'"  (str value#) (str [~@match-list]))}) false)))))
(defmacro ^{:private true} repattern? [re m key-path & [key-path-preview]]
  `(let [value# (get-in ~m [~@key-path] nil)
         path# (if ~key-path-preview ~key-path-preview ~key-path)]
     (if (and (not (nil? value#)) (re-find ~re value#))
       true
       (do (not-valid-string-f {:path path# :message (format "Value '%s' not valid on regexp '%s' pattern" (str value#) (str ~re))}) false))))

(defmacro ^{:private true} do-and [& body]
  `(do
     (do ~@body)
     (binding [~'not-valid-string-f (partial #'identity)]
       (and ~@body))))

(defn- verify-table-metadata [m]
  (do-and
   (isset? m [:table])
   (isset? m [:prop :table :representation])
   (isset? m [:prop :table :is-system?])
   (isset? m [:prop :table :is-linker?])
   (isset? m [:prop :table :allow-modifing?])
   (isset? m [:prop :table :allow-deleting?])
   (isset? m [:prop :table :allow-linking?])
   (repattern? #"^[a-z_]{3,}$" m [:table])
   (repattern? #"^[\w\d\s]+$" m [:prop :table :representation])
   (inpattern? [true false] m [:prop :table :is-system?])
   (inpattern? [true false] m [:prop :table :is-linker?])
   (inpattern? [true false] m [:prop :table :allow-modifing?])
   (inpattern? [true false] m [:prop :table :allow-deleting?])
   (inpattern? [true false] m [:prop :table :allow-linking?])))

(defn- verify-column-metadata [p m]
  (do-and
   (isset? m [:field] (conj p :field))
   (isset? m [:field-qualified] (conj p :field-qualified))
   (isset? m [:representation] (conj p :representation))
   (isset? m [:column-type] (conj p :column-type))
   (isset? m [:component] (conj p :component))
   (isset? m [:default-value] (conj p :default-value))
   (isset? m [:private?] (conj p :private?))
   (isset? m [:editable?] (conj p :editable?))
   (repattern? #"^[a-z_]{3,}$" m [:field] (conj p :field))
   (repattern? #"^[\w\d\s]+$" m [:representation] (conj p :representation))
   (inpattern? [true false] m [:private?] (conj p :private?))
   (inpattern? [true false] m [:editable?] (conj p :editable?))
   (fpattern? #(or (string? %) (nil? %)) "string? || nil?" m [:description] (conj p :description))))

;;; validators ;;;
(defn- validate-metadata-table [m]
  (verify-table-metadata m))

(defn- validate-metadata-columns [m]
  (let [fields (get-in m [:prop :columns] [])]
    (if (empty? fields)
      (not-valid-string-f "Table has empty fields list")
      (let [i-m-col (map-indexed vector fields)]
        (every? identity (map (fn [[index m-field]]
                                ;; (println [index m-field])
                                (verify-column-metadata [:prop :columns index] m-field))
                              i-m-col))))))

(defn- validate-metadata-column
  ([m-field]
   (verify-column-metadata [] m-field))
  ([path m-field]
   (verify-column-metadata path m-field)))

(defn- validate-metadata-all [m]
  (let [is-valid-table  (validate-metadata-table m)
        is-valid-column (validate-metadata-columns m)]
    (and is-valid-column is-valid-table)))

(defn- create-validator [validator]
  (fn [m-subject]
    (let [string-buffer (atom [])]
      (binding [not-valid-string-f #(swap! string-buffer (fn [buffer] (conj buffer %)))]
        (let [valid? (validator m-subject)
              output @string-buffer] {:valid? valid? :output output})))))

(def validate-all
  "Description
    Validate table map structure 

  Example
    (validate-all
     {:id 30, :table \"user\", :prop
      {:table {:representation \"us er\", :is-system? :true, :is-linker? false, :allow-modifing? :true, :allow-deleting? true, :allow-linking? true},
      :columns
      [{:field \"login\", :representation \"login\", :description nil, :component-type true, :column-type \"varchar(100)\", :private? false, :editable? :true}
       {:field \"password\", :representation \"password\", :description nil, :component-type \"i\", :column-type \"varchar(100)\", :private? false, :editable? true}
       {:field \"first_name\", :representation \"first_name\", :description nil, :component-type \"i\", :column-type \"varchar(100)\", :private? false, :editable? true}
       {:field \"last_name\", :representation \"last_name\", :description nil, :component-type \"--\", :column-type \"varchar(100)\", :private? false, :editable? true}
       {:field \"id  _permission\", :representation \"id_permission\", :description 123,
        :component-type \"l\", :column-type \"bigint(120) unsigned\", :private? false,
        :editable? true, :key-table \"permission\"}]}})
     ;;=> 
      {:valid? false,
       :output [{:path [:prop :table :is-system?], :message \"Value ':true' not from allowed list '[true false]'\"}
                {:path [:prop :table :allow-modifing?], :message \"Value ':true' not from allowed list '[true false]'\"}
                {:path [:prop :columns 0 :component-type], :message \"Value 'true' not from allowed list '[\\\"d\\\" ...]'\"}
                {:path [:prop :columns 0 :editable?], :message \"Value ':true' not from allowed list '[true false]'\"}
                {:path [:prop :columns 3 :component-type], :message \"Value '--' not from allowed list '[\\\"d\\\"...]'\"}
                {:path [:prop :columns 4 :field], :message \"Value 'id  _permission' not valid on regexp '^[a-z_]{3,}$' pattern\"}
                {:path [:prop :columns 4 :description], :message \"Value '123' not valid by predicate 'string? || nil?'\"}]}"
  (create-validator #'validate-metadata-all))

(def validate-table
  "Description
    Validate only table meta informations

  Example
    (validate-table
     {:id 30, :table \"user\", :prop
      {:table {:representation \"us er\", :is-system? :true, :is-linker? false, :allow-modifing? :true, :allow-deleting? true, :allow-linking? true},
      :columns
       [{:field \"login\", :representation \"login\", :description nil, :component-type true, :column-type \"varchar(100)\", :private? false, :editable? :true}
        {:field \"password\", :representation \"password\", :description nil, :component-type \"i\", :column-type \"varchar(100)\", :private? false, :editable? true}
         ;; ...
       ]}})
    ;;=>
      {:valid? false,
        :output [{:path [:prop :table :is-system?], :message \"Value ':true' not from allowed list '[true false]'\"}
                 {:path [:prop :table :allow-modifing?], :message \"Value ':true' not from allowed list '[true false]'\"}]}"
  (create-validator #'validate-metadata-table))

(def validate-columns
  "Descritption
    Validate only columns from table spec

  Example
    (validate-columns
     {:id 30, :table \"user\", :prop
     {:table {:representation \"us er\", :is-system? :true, :is-linker? false, :allow-modifing? :true, :allow-deleting? true, :allow-linking? true},
      :columns
      [{:field \"login\", :representation \"login\", :description nil, :component-type true, :column-type \"varchar(100)\", :private? false, :editable? :true}
       {:field \"password\", :representation \"password\", :description nil, :component-type \"i\", :column-type \"varchar(100)\", :private? false, :editable? true}
       {:field \"first_name\", :representation \"first_name\", :description nil, :component-type \"i\", :column-type \"varchar(100)\", :private? false, :editable? true}
       {:field \"last_name\", :representation \"last_name\", :description nil, :component-type \"--\", :column-type \"varchar(100)\", :private? false, :editable? true}
       {:field \"id  _permission\", :representation \"id_permission\",
        :description 123, :component-type \"l\", :column-type \"bigint(120) unsigned\",
        :private? false, :editable? true, :key-table \"permission\"}]}})
    ;;=> 
     {:valid? false
      :output [{:path [:prop :columns 0 :component-type], :message \"Value 'true' not from allowed list '[\\\"d\\\"...]'\"}
               {:path [:prop :columns 0 :editable?], :message \"Value ':true' not from allowed list '[true false]'\"}
               {:path [:prop :columns 3 :component-type], :message \"Value '--' not from allowed list '[\\\"d\\\"...]'\"}
               {:path [:prop :columns 4 :field], :message \"Value 'id  _permission' not valid on regexp '^[a-z_]{3,}$' pattern\"}
               {:path [:prop :columns 4 :description], :message \"Value '123' not valid by predicate 'string? || nil?'\"}]}"
  (create-validator #'validate-metadata-columns))

(def validate-one-column
  "Descritption
    Validate one column from table spec

  Warning
    In returning map, in :output ... :path pathskeypaths
    will be started at your column name. Not relative to
    your table
  
  Example
    (validate-one-column
      {:field \"first _name\", :representation \"first_name\", :description nil, :component-type \"i\", :column-type \"varchar(100)\", :private? false, :editable? true})
     ;;=> {:valid? false,
           :output [{:path [:field], :message \"Value 'first _name' not valid on regexp '^[a-z_]{3,}$' pattern\"}]}"
  (create-validator #'validate-metadata-column))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Table logic comparators ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- do-sql [sql-expression]
  (if (try (or (println sql-expression) true)
           (catch java.sql.SQLException e (str "caught exception: " (.toString e)) false))
    true
    false))

(defn- apply-f-diff [f-o-c-list original changed]
  (if (empty? f-o-c-list) original
      (first (reduce (fn [[o c] f] [(f o c) c]) [original changed] f-o-c-list))))

(defn- list-key-path
  "Example
    Get key-path list for table 
  Example
    (list-key-path {:id 30, :table \"user\",
                    :prop {:table {:representation \"user\", :is-system? false,
                                   :is-linker? false, :allow-modifing? true,
                                   :allow-deleting? true, :allow-linking? true}}})
     ;; => [[:id]
            [:table]
            [:prop :table :representation]
            [:prop :table :is-system?]
            [:prop :table :is-linker?]
            [:prop :table :allow-modifing?]
            [:prop :table :allow-deleting?]
            [:prop :table :allow-linking?]]"
  [m]
  (letfn [(keylist [m ref-var path]
            (let [[head tail] (map-destruct m)
                  m-fk        (first-key head)]
              (if (= path [:prop m-fk])
                (keylist tail ref-var (concat path)))
              (if (map? (m-fk head))
                (keylist (m-fk head) ref-var (concat path [m-fk]))
                (if-not (= [:prop :columns] [:prop m-fk])
                  (dosync (alter ref-var #(conj %1 (vec (concat path [m-fk])))))))
              (if tail
                (keylist tail ref-var (concat path)))))]
    (let [in-deep-key-path (ref [])]
      (keylist m in-deep-key-path nil)
      @in-deep-key-path)))


(do
  (defn adiff-table [original changed]
    (let [key-replace (fn [p] (partial (fn [p m1 m2] (println "replace key " p  " from " (get-in m1 p) " to " (get-in m2 p))
                                  (assoc-in m1 p (get-in m2 p "<Error name>"))) p))
          f-comparator (fn [p m1 m2] (get-apply = p m1 m2))]
      (vec (reduce (fn [acc p] (if (get-apply (comp not =) p original changed) (conj acc (key-replace p)) acc))
                   []
                   [;; [:id]
                    ;; [:table]
                    [:prop :table :representation]
                    [:prop :table :description]
                    ;; [:prop :table :is-system?]
                    ;; [:prop :table :is-linker?]
                    [:prop :table :allow-modifing?]
                    [:prop :table :allow-deleting?]
                    [:prop :table :allow-linking?]]))))
  ;; (adiff-table
  ;;  {:id 30, :table "user",
  ;;   :prop {:table {:representation "user", :is-system? false,
  ;;                  :is-linker? false, :allow-modifing? true,
  ;;                  :allow-deleting? true, :allow-linking? true}}}
  ;;  {:id 30, :table "user",
  ;;   :prop {:table {:representation "CHU1", :is-system? false,
  ;;                  :is-linker? false, :allow-modifing? false,
  ;;                  :allow-deleting? true, :allow-linking? true}}})
  )


(do
  (defn- find-difference-columns
    "DONT EVEN TRY TO UNDERSTAND!
  differ algorythm for comparison two list of metatable column-repr
  (find-difference-columns
     [{:field 10} {:field 20} {:field 4} {:field 5} {:field 6} {:field 7} {:field 8} {:field 9}]
     [{:field 10} {:field 20} {:field 4}            {:field 6} {:field 7} {:field 8} {:field 9} {:field 111}])
       ;=> {:maybe-changed [{:field 10} {:field 20} {:field 4} {:field 6} {:field 7} {:field 8} {:field 9}], :must-create [{:field 111}], :must-delete [{:field 5}]} "
    [original changed]
    (let [criterion-field :field
          do-diff
          (fn [original changed]
            (let [[old-elements new-elements] [(ref []) (ref [])]]
              (doseq [changed-elm changed]
                (if-let [id_ce (criterion-field changed-elm)]
                  (if-let [old-elm (first (filter (fn [org-elm] (= id_ce (criterion-field org-elm))) original))]
                    (dosync (commute old-elements #(conj % old-elm)))
                    (dosync (commute new-elements #(conj % changed-elm))))
                  (dosync (commute new-elements #(conj % changed-elm)))))
              [@old-elements @new-elements]))]
      (let [;; [old new] (doto (do-diff original changed) println)
            [old new] (do-diff original changed)
            [old del] (do-diff old original)]
        {:maybe-changed old
         :must-create new
         :must-delete del})))
  (defn column-resolver [original changed]
    (let [original-changed-field (map (comp :columns :prop) [original changed])]
      (apply find-difference-columns original-changed-field)))
  ;; (column-resolver {:id 30, :table "user",
  ;;                   :prop {:columns [{:field "login", :representation "login", :description nil, :component-type "i"}
  ;;                                    {:field "password", :representation "password", :description nil, :component-type "i"}
  ;;                                    {:field "DELETED_COLUMN", :representation "first_name", :description nil, :component-type "i"}
  ;;                                    ;; {:field "first_name", :representation "first_name", :description nil, :component-type "i"}
  ;;                                    {:field "last_name", :representation "last_name", :description nil, :component-type "i"}
  ;;                                    {:field "id_permission", :representation "id_permission", :description nil, :component-type "l"}]}}
  ;;                  {:id 30, :table "user",
  ;;                   :prop {:columns [{:field "login", :representation "login", :description nil, :component-type "i"}
  ;;                                    {:field "password", :representation "password", :description nil, :component-type "i"}
  ;;                                    {:field "first_name", :representation "first_name", :description nil, :component-type "i"}
  ;;                                    ;; {:field "last_name", :representation "last_name", :description nil, :component-type "i"}
  ;;                                    {:field "NEW_COLUMN", :representation "last_name", :description nil, :component-type "i"}
  ;;                                    {:field "id_permission", :representation "id_permission", :description nil, :component-type "l"}]}})
  )
;; {:maybe-changed
;;  [{:field "login", :representation "login", :description nil, :component-type "i"}
;;   {:field "password", :representation "password", :description nil, :component-type "i"}
;;   {:field "id_permission", :representation "id_permission", :description nil, :component-type "l"}]
;;  :must-create
;;  [{:field "first_name", :representation "first_name", :description nil, :component-type "i"}
;;   {:field "NEW_COLUMN", :representation "last_name", :description nil, :component-type "i"}]
;;  :must-delete
;;  [{:field "DELETED_COLUMN", :representation "first_name", :description nil, :component-type "i"}
;;   {:field "last_name", :representation "last_name", :description nil, :component-type "i"}]}


(defn f-diff-prop-columns-fields
  "This function is crap, not even try to understand what it do, but it work"
  [original changed]
  (let [;; function `map-k-eq` compare elements in map by keys, and return only maps of differ pairs of `map-l`
        ;; For example:
        ;; (map-k-eq {:a 1 :b 2} {:a 3 :b 2}) => {:a 3}
        m (letfn [(map-k-eq [m-key & map-l] (fn [f] (apply f (reduce #(conj %1 (get %2 m-key)) [] map-l))))]
            (reduce (fn [m-acc c-key] (if ((map-k-eq c-key original changed) =) m-acc
                                         (into m-acc {c-key (get changed c-key)}))) {} (keys changed)))]
    (let [key-replace (fn [p] (fn [p1 p2] (fn [m1 m2] (println "replace key " (vec (concat p1 p)) " from " (get-in m1 (vec (concat p1 p))) " to " (get-in m2 (vec (concat p2 p))))
                                          (assoc-in m1 (vec (concat p1 p)) (get-in m2 (vec (concat p2 p)) "<Error name>")))))]
      (((fn [f] (f f))
        (fn [f]
          (fn [[head-map tail-map]]
            (if head-map
              (if-let [n (cond-contain head-map
                                       :representation (key-replace [:representation])
                                       :description    (key-replace [:description])
                                       :component      (key-replace [:component])
                                       :private?       (key-replace [:private?])
                                       :editable?      (key-replace [:editable?])
                                       nil)]
                (if-not tail-map [n] (concat [n] ((f f) (map-destruct tail-map))))))))) (map-destruct m)))))
;; do not apply it to apply-f-table, it works only with `changed-fields`
;; (f-diff-prop-columns-fields
;;  {:field "id_permission", :representation "id_permission",
;;   :description nil, :component-type "BBBBBBBBBBBB",
;;   :column-type "BBBBBBBBBBB", :private? false,
;;   :editnable? true, :key-table "permission"}
;;  {:field "id_permission", :representation "aaaaaaaaaaaaaaaaaaaa"
;;   :description "aaaaaaaaaaaaaaaa", :component-type "l",
;;   :column-type "bigint(120) unsigned", :private? "aaaaaaaaaaaaaaaa",
;;   :editnable? true, :key-table "permission"})


#_(def user-original {:id 30
                      :table "user"
                      :prop {:table {:representation "user"
                                     :is-system? false :is-linker? false
                                     :allow-modifing? true :allow-deleting? true
                                     :allow-linking? true}
                             :columns [{:field "login", :representation "login", :description nil, :component-type "i"
                                        :column-type "varchar(100)", :private? false, :editable? true}
                                       {:field "password", :representation "password", :description nil, :component-type "i"
                                        :column-type "varchar(100)", :private? false, :editable? true}
                                       {:field "first_name", :representation "first_name", :description nil
                                        :component-type "i", :column-type "varchar(100)", :private? false, :editable? true}
                                       {:field "last_name", :representation "last_name", :description nil
                                        :component-type "i", :column-type "varchar(100)", :private? false, :editable? true}
                                       {:field "id_permission", :representation "id_permission", :description nil
                                        :component-type "l", :column-type "bigint(120) unsigned", :private? false, :editable? true, :key-table "permission"}]}})
;; :allow-modifing? true
;; :representation "UĹźytkownik"
;; :add field "age"
;; :delete "first_name
#_(def user-changed {:id 30
                     :table "user"
                     :prop {:table {:representation "Uďż˝ytkownik"
                                    :is-system? false :is-linker? false
                                    :allow-modifing? false :allow-deleting? true
                                    :allow-linking? true}
                            :columns [{:field "login", :representation "Logowanie", :description "Logowanie pole", :component-type "i"
                                       :column-type "varchar(100)", :private? false, :editable? true}
                                      {:field "password", :representation "Haslo", :description nil, :component-type "i"
                                       :column-type "varchar(100)", :private? false, :editable? true}
                                      ;; {:field "first_name", :representation "Drugie imie", :description nil,
                                      ;;  :component-type "i", :column-type "varchar(100)", :private? false, :editable? true}
                                      {:field "last_name", :representation "last_name", :description nil
                                       :component-type "i", :column-type "varchar(100)", :private? false, :editable? true}
                                      {:field "age", :representation "Wiek", :description nil
                                       :component-type "i", :column-type "number", :private? false, :editable? true}
                                      {:field "id_permission", :representation "id_permission", :description nil
                                       :component-type "l", :column-type "bigint(120) unsigned", :private? false, :editable? true, :key-table "permission"}]}})

(do
  (defn delete-fields [original fields]
    (vec (for [field fields]
           (fn [original changed]
             (if (do-sql (alter-table! {:table_name (:table original) :drop-column (:field field)}))
               (update-in original [:prop :columns] (fn [fx] (filter #(not= (:field %) (:field field)) fx)))
               original)))))
  ;; (apply-f-diff
  ;;  (delete-fields user-original [{:field "first_name", :representation "first_name", :description nil,
  ;;                                 :component-type "i", :column-type "varchar(100)", :private? false, :editable? true}])
  ;;  user-original
  ;;  user-changed)
  )

(do
  (defn create-fields [original fields]
    (for [field fields]
      (fn [original changed]
        (if (do-sql (alter-table! {:table_name (:table original) :add-column {(:field field) [(:column-type field)]}}))
          (clojure.core/update-in original [:prop :columns] (fn [all] (conj all field))) original))))
  ;; (apply-f-diff
  ;;  (create-fields user-original [{:field "suka_name", :representation "SUKA", :description nil,
  ;;                                 :component-type "i", :column-type "varchar(100)", :private? false, :editable? true}])
  ;;  user-original
  ;;  user-changed)
  )


(do
  (defn change-fields [original changed]
    (let [fx ((comp :columns :prop) original)
          yx ((comp :columns :prop) changed)]
      (apply concat (for [[yi y] (map-indexed vector yx)]
                      (let [[fi f] (find-column #(= (:field (second %)) (:field y)) (map-indexed vector fx))]
                        (map #(% [:prop :columns fi] [:prop :columns yi]) (f-diff-prop-columns-fields f y)))))))
  ;; (apply-f-diff
  ;;  (change-fields user-original user-changed)
  ;;  user-original
  ;;  user-changed)
  )


(do
  (defn apply-table [original changed]
    (let [fxmap-changes (atom {})
          f-table-changes (adiff-table original changed)
          {column-changed :maybe-changed
           column-created :must-create
           column-deleted :must-delete} (column-resolver original changed)]
     ;;; `TODO` delete this debug fileds
      (println (apply str "   Actions:"))
      (do (if (not-empty f-table-changes) (println "\ttable chnaged: " (count f-table-changes)))
          (if (not-empty column-deleted) (println "\tcolumn deleted: " (count (delete-fields original column-deleted)))) ;;1
          (if (not-empty column-created) (println "\tcolumn created: " (count (create-fields original column-created)))) ;;1 
          (if (not-empty column-changed) (println "\tcolumn changed: " (count (change-fields original changed)))))
      ;; (do (if (not-empty f-table-changes) (swap! fxmap-changes #(assoc % :table (count f-table-changes))))
      ;;     (if (not-empty column-deleted) (swap! fxmap-changes #(assoc % :column-deleted (count (delete-fields original column-deleted)))))
      ;;     (if (not-empty column-created) (swap! fxmap-changes #(assoc % :column-created (count (create-fields original column-created)))))
      ;;     (if (not-empty column-changed) (swap! fxmap-changes #(assoc % :column-changed (count (change-fields original changed))))))

      (do (if (not-empty f-table-changes) (swap! fxmap-changes #(assoc % :table f-table-changes)))
          (if (not-empty column-deleted) (swap! fxmap-changes #(assoc % :column-deleted (delete-fields original column-deleted))))
          (if (not-empty column-created) (swap! fxmap-changes #(assoc % :column-created (create-fields original column-created))))
          (if (not-empty column-changed) (swap! fxmap-changes #(assoc % :column-changed (change-fields original changed)))))
      @fxmap-changes))
  ;; (apply-table user-original user-changed)
  )

;;; `TODO` persist do database in metadata tabel.
;;; `TODO` persist do database in metadata tabel.
;;; `TODO` persist do database in metadata tabel.
;;; `TODO` persist do database in metadata tabel.
;;; `TODO` persist do database in metadata tabel.
;;; `TODO` persist do database in metadata tabel.
(defn do-change
  "Description:
    Function apply lazy fxmap-changes argument as list of SQL applicative functors.

  Example:
    (do-changes
      (apply-table original-table changed-table)
      original-table changed-table)

  Arguments:
  `fxmap-changes` - special map, generated function `apply-table`
  `original` - stable version of table meta
  `changed` - changed by user table
  `keywords` - [optional] select one of actions `:table`,`:column-changed`,`:column-created`,`column-deleted`.
    which changed methadata and original SQL by specyfic action. If keywords is empty, what done all the action
    in `fxmap-changes` dictionary.

  See related functions
  `jarman.logic.metadata/apply-table`
  `jarman.logic.metadata/adiff-table`
  `jarman.logic.metadata/delete-fields`
  `jarman.logic.metadata/create-fields`
  `jarman.logic.metadata/change-fields`"
  [fxmap-changes original changed & keywords]
  ;; (println (apply str (repeat 30 "-")))
  ;; (println (apply-f-diff (get fxmap-changes :table nil) original changed))
  ;; (println (apply-f-diff (get fxmap-changes :column-changed nil) original changed))
  ;; (println (apply-f-diff (get fxmap-changes :column-deleted nil) original changed))
  ;; (println (apply-f-diff (get fxmap-changes :column-created nil) original changed))
  ;; (println (apply str (repeat 30 "-")))
  (let [keywords (if (empty? keywords)
                   ;; apply changes of all of state 
                   [:table :column-changed :column-deleted :column-created]
                   ;; apply changes only on [one-of keywords-list stages
                   (vec (filter #(some (fn [kwd] (= kwd %)) keywords)
                                [:table :column-changed :column-deleted :column-created])))]
    (let [kkkk (if-not (empty? keywords)
                 (reduce #(apply-f-diff (get fxmap-changes %2 nil) %1 changed) original [:table :column-changed :column-deleted :column-created])
                 (do (println "Chanages not being applied, empty keywords list")
                     original))]
      (println kkkk)
      (->> kkkk
           (update-sql-by-id-template "metadata")
           (db/exec)))))
