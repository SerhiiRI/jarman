;;  ____   ___  _                                         
;; / ___| / _ \| |                                        
;; \___ \| | | | |                                        
;;  ___) | |_| | |___                                     
;; |____/_\__\_\_____|  _                                 
;; |  \/  | ____|_   _|/ \                                
;; | |\/| |  _|   | | / _ \                               
;; | |  | | |___  | |/ ___ \                              
;; |_|__|_|_____| |_/_/   \_\_____ ____ _____ _____ ____  
;;  / ___/ _ \| \ | \ \   / / ____|  _ \_   _| ____|  _ \ 
;; | |  | | | |  \| |\ \ / /|  _| | |_) || | |  _| | |_) |
;; | |__| |_| | |\  | \ V / | |___|  _ < | | | |___|  _ < 
;;  \____\___/|_| \_|  \_/  |_____|_| \_\|_| |_____|_| \_\
;; =======================================================
;; This is two way converter, from metadata to SQL and in back too.
;; fixme:serhii metadata_sql_convertor
;; 1. Check convertation to sql and fix it if needed
;; 2. Check back transfromation to sql-expression

(ns jarman.logic.metadata-sql-converter
  (:require
   [clojure.string :as string]
   [jarman.tools.lang :refer :all]
   [jarman.logic.connection :as db]
   ;; [jarman.logic.metadata-toolbox :refer [not-allowed-rules allowed-rules]]
   ;; [datascript.core :as d]
   [jarman.tools.org :refer :all]
   [jarman.logic.metadata-core :as metadata-core]
   [jarman.logic.sql-tool
    :refer [select! update! insert!
            alter-table! create-table! drop-table delete!
            show-table-columns ssql-type-parser]])
  (:import [jarman.logic.metadata_core TableMetadata]))

;;  ____   ___  _        __  __  __ _____ _____  _    
;; / ___| / _ \| |       \ \|  \/  | ____|_   _|/ \   
;; \___ \| | | | |   _____\ \ |\/| |  _|   | | / _ \  
;;  ___) | |_| | |__|_____/ / |  | | |___  | |/ ___ \ 
;; |____/ \__\_\_____|   /_/|_|  |_|_____| |_/_/   \_\


(defn- parse-on-delete-update-actions
  "Description
    Return SSQL on-update/-delete structure 

  Example
    (create-on-delete-on-update-action \" ON DELETE CASCADE ON UPDATE CASCADE\")
     ;; => {:delete :cascade :update :cascade}
    (create-on-delete-on-update-action \"ON DELETE SET NULL\")
     ;; => nil"
  [line]
  (let [action-to (fn [x] (case x "DELETE" :delete "UPDATE" :update))
        mode-to   (fn [x] (case x "CASCADE" :cascade "RESTRICT" :restrict "SET NULL" :null "NO ACTION" :no-action "SET DEFAULT" :default))]
    (let [on-u-d
          (filter some?
                  (for [pattern (rest (clojure.string/split line #"\s*ON\s*"))]
                    (if-let [[_ action prop] (re-find #"(DELETE|UPDATE)\s+(CASCADE|SET NULL|NO ACTION|RESTRICT|SET DEFAULT)" pattern)]
                      [(action-to action) (mode-to prop)])))]
      (if-not (empty? on-u-d) [(into {} on-u-d)]))))

(defn- parse-constrants-line [s]
  (if-let [[_ column-name related-table actions]
           (re-matches #"CONSTRAINT\s`[\w_]*`\sFOREIGN\sKEY\s\(`([\w_]*)`\)\sREFERENCES\s`([\w_]*)`\s\(`[\w_]+`\)(.+)?" s)]
    (if actions
      {column-name (vec (concat [{(keyword column-name) (keyword related-table)}] (parse-on-delete-update-actions actions)))}
      {column-name (vector {(keyword column-name) (keyword related-table)})})))

(defn- parse-meta-constrants [table]
  (let [table (name table)
        ;; In return map from sql server we get something
        ;; like {.. :create table "Create ..}, where 
        ;; ':create table' is key with !SPACE!, and you cannot
        ;; get this in simple way, then do 'create table' from
        ;; string convertation. In clojure level is really may sense
        create-table-sql-accesor (comp (keyword "create table") first)
        sql-create-table
        (-> (db/query (format "show create table %s" table))
            (create-table-sql-accesor)
            (string/split #"\n"))]
    (->> sql-create-table
         (map string/trim)
         (filter #(string/starts-with? % "CONSTRAINT"))
         (map parse-constrants-line)
         (reduce into))))

(defn- parse-table-column-type [sql-column-spec]
  (let [{:keys [default key type extra null]} sql-column-spec]
    (wlet
     ;;--------------------
     (-> {:stype type
          :tname nil
          :tspec nil
          :tlimit nil
          :tnull nil
          :textra extra
          :tdefault default
          :tkey key}
         (edit-spec type)
         (edit-type type)
         (edit-null type))
     ;; -------------------
     ((edit-spec
       (fn [m s]
         (if-let [t (re-find #"signed|unsigned|zerofill" type)]
           (assoc m :tspec (keyword t)) m)))
      (edit-null
       (fn [m s]
         ;; (case (re-find #"(?:not null|null)" s)
         ;;   "null" true
         ;;   "not null" (assoc m :tnull false)
         ;;   m)
         (if null
           (if (= "NO" null)
            (assoc m :tnull false)
            (assoc m :tnull true))
           m)))
      (edit-type
       (fn [m s]
         (if-let [[_ t p] (re-find #"([a-zA-Z]+)(?:\(([\d|\.]+)\))?" s)]
           (merge m
                  {:tname (keyword t)
                   :tlimit 
                   (when p
                     (if (string/includes? p ".")
                       (Double/parseDouble p)
                       (Integer/parseInt p)))}))))))))

(defn- try-to-predict-component-declaration [column-type]
  (let [{:keys [tname tdefault tlimit]} column-type]
    (condp contains? tname
      #{:date}
      {:type :jsgl-calendar-label :value (rift tdefault nil)}
      
      #{:time}
      {:type :jsgl-time :value (rift tdefault nil)}
      
      #{:datetime}
      {:type :jsgl-datetime-label :value (rift tdefault nil)}
      
      #{:int :mediumint :integer :smallint :bigint}
      (cond-> {:type :jsgl-digit :value (rift tdefault 0)} tlimit (assoc :num-limit tlimit))
      
      #{:float :real :double}
      (cond-> {:type :jsgl-float :value (rift tdefault 0)} tlimit (assoc :mantis-limit tlimit))
      
      #{:boolean :bool :tinyint}
      {:type :jsgl-checkbox :value (rift tdefault nil)}

      #{:json}
      {:type :jsgl-codearea :value (rift tdefault "")}
      
      #{:mediumtext :tinytext :text :longtext}
      (cond-> {:type :jsgl-textarea :value (rift tdefault "")} tlimit (assoc :char-limit tlimit))  ;; a - mean area, text area

      #{:blob}
      (cond-> {:type :jsgl-text :value (rift tdefault "")} tlimit (assoc :char-limit tlimit))

      #{:varchar}
      (cond-> {:type :jsgl-text :value (rift tdefault "")} tlimit (assoc :char-limit tlimit))

      (do
        (print-error (ex-info "Unpredicted SQL type." column-type))
        {:type nil}))))

(defn- build-table-field-meta [table-name foreign-keys column-field-spec]
  (let [field       (:field column-field-spec)
        column-type (parse-table-column-type column-field-spec)
        f-key       (get foreign-keys field nil)]
    (print-line (format "build field metadata for `%s` column" field))
    (cond-> {:field           (keyword field)
             :field-qualified (keyword (str (name table-name) "." (name field)))
             :representation  (-> field (string/replace #"[_-]" " ") (string/capitalize))
             :description     nil
             :component       (try-to-predict-component-declaration column-type)
             :column-type     column-type
             :default-value   nil
             :private?        false
             :editable?       true}
      f-key (assoc :key-table    (get (first f-key) (keyword field))
                   :foreign-keys f-key
                   :component    {:type :jsgl-link}))))

(defn- build-metainformation-for-table
  "Description:
    Get meta information about table by hame, and construct meta information to GUI

  Template of returning meta:
   :field - table name without modifing
   :representation \"table\" :private? true :scallable? false :linker? true}
   :representation - table name wich would be viewed for user in GUI, must b readable.
   :private? - if is true, than user can be edit this table
   :scallable? - if is set on true, user may add fields to datatable.
   :linker? - flag inform that table used by DB engine mechanizm. (DO NOT EDIT THAT FIELD)"
  [t-name]
  {:pre [(string? t-name) (not-empty t-name)]}
  (print-line (format "build table metadata for `%s` sql table" t-name))
  (let [tspec (last (string/split t-name #"_"))]
    (condp in? tspec
      ;; --------------------
      ["lk" "link" "links"]
      {:field t-name
       :representation t-name
       :description nil
       :is-system? true
       :is-linker? true
       :allow-modifing? false
       :allow-deleting? false
       :allow-linking? false}
      ;; --------------------
      {:field t-name
       :representation t-name
       :description nil
       :is-system? false
       :is-linker? false
       :allow-modifing? true
       :allow-deleting? true
       :allow-linking?  true})))

(defn build-metadata-for-table [table-name]
  (let [table-name     (name table-name)
        all-columns    (db/query (show-table-columns table-name))
        all-constrants (parse-meta-constrants table-name)]
    (print-header (format "build metadata for `%s`" table-name)
     {:id nil
      :table_name table-name
      :prop {:table   (build-metainformation-for-table table-name)
             :columns (->> all-columns
                           (filter #(not= "id" (:field %)))
                           (mapv #(build-table-field-meta table-name all-constrants %)))}})))

;;  __  __ _____ _____  _       __  ____   ___  _     
;; |  \/  | ____|_   _|/ \      \ \/ ___| / _ \| |    
;; | |\/| |  _|   | | / _ \ _____\ \___ \| | | | |    
;; | |  | | |___  | |/ ___ \_____/ /___) | |_| | |___ 
;; |_|  |_|_____| |_/_/   \_\   /_/|____/ \__\_\_____|

;;;;;;;;;;;;;;;;
;;; On meta! ;;;
;;;;;;;;;;;;;;;;

(defn- create-string-column [column-type]
  (let [{:keys [stype tnull tdefault]} column-type]
    (cond->          (string/upper-case stype)
      (some? tnull)  (as-> $ (if tnull (str $ " NULL") (str $ " NOT NULL")))
      tdefault (str (format " default %s" tdefault)))))

(defn create-table-by-meta [metadata]
  (let [metadata (if (metadata-core/isTableMetadata? metadata) metadata (TableMetadata. metadata) )
        all-columns (.return-columns-flatten metadata)]
    (let [smpl-fields (filter (comp not some? (partial re-matches #"meta.*") name :field) all-columns)
          idfl-fields (filter (comp some? (partial re-matches #"id_.*") name :field) all-columns)
          fkeys-fields (vec (eduction (filter :foreign-keys) (map :foreign-keys) idfl-fields))]
      (create-table!
       {:table_name (keyword (.return-table_name metadata))
        :columns (vec (map (fn [sf] {(keyword (:field sf)) (create-string-column (get sf :column-type nil))}) smpl-fields))
        :foreign-keys fkeys-fields}))))

(defn create-all-table-by-meta [table-list]
  (doall
   (for [table table-list]
     (if-let [mtable (first (metadata-core/return-metadata table))]
       (create-table-by-meta mtable)
       (print-line (format "metadata for table `%s` doesn't exist" table ))))))

