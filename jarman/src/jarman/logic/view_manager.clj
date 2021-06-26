(ns jarman.logic.view-manager
  (:require
   ;; Clojure toolkit 
   [clojure.string :as string]
   [clojure.java.io :as io]
   [seesaw.core :as c]
   ;; ;; Jarman toolkit
   [jarman.gui.gui-seed :as gseed]
   [jarman.logic.connection :as db]
   [jarman.tools.lang :include-macros true :refer :all]
   [jarman.config.environment :as env]
   [jarman.plugin.jspl :as jspl]
   [jarman.logic.sql-tool :refer [select! update! insert!]]
   [jarman.logic.metadata :as mt]
   [jarman.logic.state :as state])
  (:import (java.util Date)
           (java.text SimpleDateFormat)))

;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; GLOBAL DEFVIEW MAP ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;

(def ^:private global-view-configs (atom {}))
(defn global-view-configs-clean []
  (reset! global-view-configs {}) nil)
(defn global-view-configs-get []
  @global-view-configs)
(defn global-view-configs-set [path configuration toolkit]
  {:pre [(every? keyword? path)]}
  (swap! global-view-configs
         (fn [m] (assoc-in m path
                          {:config configuration
                           :toolkit toolkit}))) nil)

;;;;;;;;;;;;;;;;;;;;;;;;
;;; CONFIG PROCESSOR ;;;
;;;;;;;;;;;;;;;;;;;;;;;;
(defn- sort-parameters-plugins
  "Description
    this func get list of data with different types,
    sort these and return vector in which first value is
    hashmap with parameters and second value are functions 
  Example
    (sort-parameters-plugins '(1 2 :a 1 (1 2 3) :b 2))
     ;;=> [{:a 1, :b 2, :permission [:user]} [(1 2 3)]]"
  [defview-body]
  (loop [;; l - mean body list 
         l defview-body
         parameters {} plugins []]
    (if (not-empty l)
      (cond
        (keyword?    (first l)) (recur (drop 2 l) (into parameters {(first l) (second l)})  plugins)
        (sequential? (first l)) (recur (drop 1 l) parameters (conj plugins (first l)))
        :else                   (recur (drop 1 l) parameters plugins))
      ;; Middleware is function which add :id and :permission for plugin
      [parameters
       plugins])))

(defn defview-prepare-config
  "Description 
    Prepare configuraion to all plugins
  Example
     (defview-prepare-config
     'permission
     '(:--another :--param
       :permission [:admin :user]
       (table
        :name \"first Permission\")
       (table
        :id :UUUUUUUUUUUUUU
        :permission [:user]
        :name \"second Permission\")))
   ;; =>
    [{:--another :--param,
      :permission [:admin :user],
      :name \"first Permission\",
      :id :plugin-24793,
      :table_name :permission,
      :plugin-name table,
      :plugin-config-path [:permission :table :plugin-24793]}
     {:--another :--param,
      :permission [:user],
      :name \"second Permission\",
      :id :UUUUUUUUUUUUUU,
      :table_name :permission,
      :plugin-name table,
      :plugin-config-path [:permission :table :UUUUUUUUUUUUUU]}]"
  [table-name body]
  (let [add-id         (key-setter :id         #(keyword (gensym "plugin-")))
        add-table-name (key-setter :table_name (keyword table-name))
        add-permission (key-setter :permission [:user])
        eval-action    (fn [m] (update-in m [:actions] eval))
        k-table-name (keyword table-name)]
    (let [[global-cfg plugin-list] (sort-parameters-plugins body)]
      (reduce
       (fn [acc-cfg [plugin-name & plugin-cfg]]
         (blet (conj acc-cfg cfg)
               [cfg (add-id (merge global-cfg (apply hash-map plugin-cfg)))
                k-plugin-id       (:id cfg)
                k-plugin-name     (keyword plugin-name)
                add-plugin-name   (key-setter :plugin-name        (str plugin-name))
                add-full-path-cfg (key-setter :plugin-config-path [k-table-name k-plugin-name k-plugin-id])
                cfg (-> cfg
                        eval-action
                        add-table-name
                        add-permission
                        add-plugin-name
                        add-full-path-cfg)])) [] plugin-list))))

(defmacro defview [table-name & body]
  (let [cfg-list (defview-prepare-config table-name body)]
    `(do
       ~@(for [cfg cfg-list]
           (let [plugin-toolkit-pipeline (eval `~(symbol (format "jspl/%s-toolkit-pipeline" (:plugin-name cfg))))
                 plugin-component `~(symbol (format "jspl/%s" (:plugin-name cfg)))
                 toolkit (plugin-toolkit-pipeline cfg)]
             (global-view-configs-set (:plugin-config-path cfg) cfg toolkit)
             `(~plugin-component ~(:plugin-config-path cfg) ~'global-view-configs-get)
             )) nil)))

(defmacro defview-no-eval [table-name & body]
  (let [cfg-list (defview-prepare-config table-name body)]
    `(do
       (global-view-configs-clean)
       (println "------(DEFVIEW DEBUG)------")
       ~@(for [cfg cfg-list]
           (let [plugin-toolkit-pipeline (eval `~(symbol (format "jspl/%s-toolkit-pipeline" (:plugin-name cfg))))]
             (global-view-configs-set (:plugin-config-path cfg) cfg (plugin-toolkit-pipeline cfg))
             `(println ~(:plugin-config-path cfg))))
       true)))

(defmacro defview-debug [table-name & body]
  (let [cfg-list (defview-prepare-config table-name body)]
    `(list
      ~@(for [cfg cfg-list]
          (let [plugin-toolkit-pipeline `~(symbol (format "jspl/%s-toolkit-pipeline" (:plugin-name cfg)))]
            `{:cfg ~cfg :toolkit (~plugin-toolkit-pipeline ~cfg)})
          ))))

(defmacro defview-debug-map [table-name & body]
  (let [cfg-list (defview-prepare-config table-name body)]
    `(do
      ~(reduce
        (fn [m-acc cfg]
          (let [plugin-toolkit-pipeline `~(symbol (format "jspl/%s-toolkit-pipeline" (:plugin-name cfg)))]
            `(assoc-in ~m-acc [~@(:plugin-config-path cfg)] {:config ~cfg :toolkit (~plugin-toolkit-pipeline ~cfg)})))
         {} cfg-list))))

;;; TEST DEFVIEW SEGMENT
;; defview                --  push data to global map and eval component 
;; defview-no-eval        --  push data to global map, but not eval component
;; defview-debug          --  print list of final configuration and toolkit map for every plugin
;; defview-debug-map      --  just return structure like global-map, to programmer can overview structure only
#_(defview-debug permission
    (table
     :name "permission"
     :plug-place [:#tables-view-plugin]
     :tables [:permission]
     :view-columns [:permission.permission_name :permission.configuration]
     :model-insert [:permission.permission_name :permission.configuration]
     :insert-button true
     :delete-button true
     :actions []
     :buttons []
     :query
     {:table_name :permission,
      :column
      [:#as_is
       :permission.id
       :permission.permission_name
       :permission.configuration]})
    (dialog-table
     :id :my-custom-dialog
     :name "My dialog box"
     :permission [:user]))

;;; ---------------------------------------
;;; Eval this function and take a look what
;;; you get in that configuration
;;;  See on:
;;;  `:permission`
;;;  `:id`
;;; ---------------------------------------
;;;

(comment
  (do-view-load)
  (global-view-configs-clean)
  (global-view-configs-get)
  ((get-in (global-view-configs-get) [:permission]))
  ((get-in (global-view-configs-get) [:permission :dialog-table :my-custom-dialog :toolkit :dialog])))


;;;;;;;;;;;;;;;;;;;;;;;;;
;;; HELPERS `defview` ;;; 
;;;;;;;;;;;;;;;;;;;;;;;;;

(defn as-is [& column-list]
  (map #(if (keyword? %) {% %} %) column-list))

(defn- put-table-view-to-db [view-data]
  (((fn [f] (f f))
    (fn [f]
      (fn [s data]
        (if (not (empty? data))
          ((f f)
           (do
             (let [table-name (str (second (first data)))
                   table-data (str (first data))
                   id-t (:id (first (db/query
                                     (select! {:table_name :view :where [:= :table_name table-name]}))))]   
               (if-not (= s 0)
                 (if (nil? id-t)
                   (db/exec (insert! {:table_name :view :set {:table_name table-name, :view table-data}}))
                   (db/exec (update
                             :view
                             :where [:= :id id-t]
                             :set {:view table-data})))))
              (+ s 1))
           (rest data)))))) 0 view-data))

(defn loader-from-db []
  (let [con (dissoc (db/connection-get)
                    :dbtype :user :password
                    :useUnicode :characterEncoding)
        req (list 'in-ns (quote (quote jarman.logic.view-manager)))
        data (db/query (select! {:table_name :view}))
        sdata (if-not (empty? data)(concat [con req] (map (fn [x] (read-string (:view x))) data)))
        path  "src/jarman/logic/view.clj"]
    (if-not (empty? data) (do (spit path
                                  "")
                              (for [s sdata]
                                (with-open [W (io/writer (io/file path) :append true)]
                                  (.write W (pp-str s))
                                  (.write W env/line-separator)))))))

(defn loader-from-view-clj []
  (let [data 
        (try
          (read-seq-from-file  "src/jarman/logic/view.clj")
          (catch Exception e (println (str "caught exception: file not find" (.toString e)))))
        con (dissoc (db/connection-get)
                    :dbtype :user :password
                    :useUnicode :characterEncoding)]
    (if-not (empty? data)
      (if (= (first data) con) data))))

;; (put-table-view-to-db (loader-from-view-clj (db/connection-get)))

(defn- load-data-recur [data loaders]
  (if (and (empty? data) (not (empty? loaders)))
    (load-data-recur ((first loaders)) (rest loaders))
    data))

(defn make-loader-chain
  "resolving from one loaders, in ordering of argument.
   if first is db-loader, then do first load forom db, if
   db not load db, or do crash, use next in order loader"
  [& loaders]
  (fn []
    (global-view-configs-clean)
    (load-data-recur nil loaders)))
 
(def ^:dynamic *view-loader-chain-fn*
  "Main function "
  (make-loader-chain loader-from-view-clj loader-from-db))

(defn do-view-load
  "using in self `*view-loader-chain-fn*`, swapp using
  make-loader chain. deserialize view, and execute every
  defview."
  []
  (let [data (*view-loader-chain-fn*)]
    (if (empty? data)
      ((state/state :alert-manager) :set {:header "Error" :body "Problem with tables. Data not found in DB"} 5)
      (binding [*ns* (find-ns 'jarman.logic.view-manager)] 
        (doall (map (fn [x] (eval x)) (subvec (vec data) 2)))))))


(defn- metadata-get [table]
  (first (jarman.logic.metadata/getset! table)))

(defn- metadata-set [metadata]
  (jarman.logic.metadata/update-meta metadata))

(defn- view-get
  "Description
    get view from db by table-name
  Example
    (view-get \"user\")
    {:id 2, :table_name \"user\", :view   \"(defview user (table :name \"user\"......))})"
  [table-name]
  (first (db/query
          (select! {:table_name :view :where [:= :table_name table-name]}))))

(defn- view-set
  "Description
    get view-map, write to db, rewrite file view.clj
  Example
    (view-set {:id 2, :table_name \"user\", :view \"(defview user (table :name \"user\"......))} )"
  [view]
  (let [table-name (:table_name view)
        table-view (:view view)
        id-t       (:id (first (db/query (select! {:table_name :view :where [:= :table_name table-name]}))))]   
    (if (nil? id-t)
      (db/exec (insert! {:table_name :view :set {:table_name table-name, :view table-view}}))
      (db/exec (update! {:table_name :view :set {:view table-view} :where [:= :id id-t]})))
    (loader-from-db)))


