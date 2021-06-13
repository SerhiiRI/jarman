(ns jarman.logic.view-manager
  (:refer-clojure :exclude [update])
  (:require
   ;; Clojure toolkit 
   [clojure.string :as string]
   [clojure.java.io :as io]
   [seesaw.core :as c]
   ;; ;; Jarman toolkit
   [jarman.logic.connection :as db]
   [jarman.tools.lang :include-macros true :refer :all]
   [jarman.config.environment :as env]
   [jarman.plugin.jspl :refer :all :as jspl]
   [jarman.plugin.table :as plug]
   [jarman.logic.sql-tool :as toolbox :include-macros true :refer :all]
   [jarman.logic.metadata :as mt]
   [jarman.plugin.data-toolkit :as dtool])
  (:import (java.util Date)
           (java.text SimpleDateFormat)))


(defn- toolkit-pipeline-reducer [configuration & toolkit-list]
  (reduce (fn [acc-toolkit toolkit-pipeline]
            (toolkit-pipeline configuration acc-toolkit))
          {}
          toolkit-list))


(defn rand-str [len]
  (apply str (take len (repeatedly #(char (+ (rand 26) 65))))))

;;(rand-str 4)
;;(def num (atom :first-plug))
(def ^:private views-configuration+toolkit (atom {}))
(defn views-configuration+toolkit-get []
  @views-configuration+toolkit)
(defn views-configuration+toolkit-set [keyword-table-name plugin-name plugin-configuration data-toolkit]
  {:pre [(keyword? keyword-table-name)]}
  (swap! views-configuration+toolkit
         (fn [m]
           (assoc-in m [keyword-table-name (:name plugin-configuration) plugin-name]
                     {:configuration plugin-configuration
                      :data-toolkit data-toolkit}))) nil)

;;(views-configuration+toolkit-get)

(defn sort-parameters-plugins
  "Description
    this func get list of data with different types,
    sort these and return vector in which first value is
    hashmap with parameters and second value are functions 
  Example
    (sort-parameters-plugins '(1 2 :a 1 (1 2 3) :b 2))
     ;;=> [{:a 1, :b 2, :permission [:user]} [(1 2 3)]]"
  [defview-body]
  (loop [parameters {}
         l defview-body
         plugins []]
    (if (not-empty l)
      (cond
        (keyword? (first l)) (recur (into parameters {(first l) (second l)}) (drop 2 l) plugins)
        (list? (first l)) (recur parameters (drop 1 l) (conj plugins (first l)))
        :else (recur parameters (drop 1 l) plugins))
      [(if-not (contains? parameters :permission) (assoc parameters :permission [:user]))
       plugins])))

(defmacro defview-debug [table-model-name & body]
  (let [[conf-prms fbody] (sort-parameters-plugins body)
        configurations
        (reduce into {}
                (for [form fbody]
                  (if (sequential? form)
                    (let [s `(hash-map ~@(rest form)
                                       :table-name ~(keyword table-model-name))
                          s `(merge ~conf-prms ~s)]
                      `{~(keyword (first form)) {:configuration ~s
                                                 :data-toolkit (dtool/data-toolkit-pipeline ~s)}}))))]
    `~configurations))

(defmacro defview [table-model-name & body]
  (let [[conf-prms fbody] (sort-parameters-plugins body)
        configurations
        (reduce into {}
                (for [form fbody]
                  (if (sequential? form)
                    (let [s `(hash-map ~@(rest form)
                                         :table-name ~(keyword table-model-name))
                          s `(merge ~conf-prms ~s)]
                      `{~(keyword (first form)) ~s}))))]
    `(do ~@(for [form fbody :let [f (first form)]]
             `(let [cfg# ~configurations
                    ktable# ~(keyword table-model-name)
                    kplugin# ~(keyword f)
                    plugin-configuration# (get cfg# kplugin#)
                    kname-plugin# (:name plugin-configuration#) 
                    plugin-data-toolkit#  (toolkit-pipeline-reducer (get cfg# kplugin#)
                                                                    dtool/data-toolkit-pipeline
                                                                    ~(symbol (str "jspl/"
                                                                                  f "-toolkit-pipeline"))
                                                                    ;;fun-toolkit-name
                                                                    )]
                (views-configuration+toolkit-set ktable#
                                                 kplugin#
                                                 plugin-configuration#
                                                 plugin-data-toolkit#)
                (~f [ktable# kname-plugin# kplugin#] ~'views-configuration+toolkit-get)))
         nil)))

(defn as-is [& column-list]
  (map #(if (keyword? %) {% %} %) column-list))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; READ AND WRITE VIEW.CLJ ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn put-table-view-to-db [view-data]
  (((fn [f] (f f))
    (fn [f]
      (fn [s data]
        (if (not (empty? data))
          ((f f)
           (do
             (let [table-name (str (second (first data)))
                   table-data (str (first data))
                   id-t (:id (first (db/query
                                     (select :view :where [:= :view.table_name table-name]))))]   
               (if-not (= s 0)
                 (if (nil? id-t)
                   (db/exec (insert :view :set {:table_name table-name, :view table-data}))
                   (db/exec (update
                             :view
                             :where [:= :id id-t]
                             :set {:view table-data})))))
              (+ s 1))
           (rest data))
          )))) 0 view-data))

(defn loader-from-db [db-connection]
  (let [con (dissoc (db/connection-get)
                    :dbtype :user :password
                    :useUnicode :characterEncoding)
        data (db/query (select! {:table-name :view
                                :column [:view]}))
        sdata (concat [con] (map (fn [x] (read-string (:view x))) data))
        path  "src/jarman/logic/view.clj"]
    (if-not (nil? data) (do (spit path
                                  "")
                            (for [s data]
                              (with-open [W (io/writer (io/file path) :append true)]
                                (.write W (pp-str s))
                                (.write W env/line-separator)))))))

(defn loader-from-view-clj [db-connection]
  (let [data 
        (try
          (read-seq-from-file  "src/jarman/logic/view.clj")
          (catch Exception e (println (str "caught exception: file not find" (.toString e)))))
        con (dissoc (db/connection-get)
                    :dbtype :user :password
                    :useUnicode :characterEncoding)]
    (if-not (nil? data)
      (if (= (first data) con) data))))

;;(put-table-view-to-db (loader-from-view-clj (db/connection-get)))

(defn load-data [data loaders]
  (if (nil? data)
    (load-data ((first loaders) (db/connection-get)) (rest loaders))
    data))

(defn make-loader-chain
  "resolving from one loaders, in ordering of argument.
   if first is db-loader, then do first load forom db, if
   db not load db, or do crash, use next in order loader"
  [& loaders]
  (subvec (vec (load-data nil loaders)) 1))

(def ^:dynamic *view-loader-chain-fn* 
  (make-loader-chain loader-from-view-clj loader-from-db))

(defn do-view-load
  "using in self `*view-loader-chain-fn*`, swapp using
  make-loader chain. deserialize view, and execute every
  defview."
  []
  (let [data *view-loader-chain-fn*]
    (if (nil? data)
      "Error with file"
      (binding [*ns* (find-ns 'jarman.logic.view-manager)] 
        (doall (map (fn [x] (eval x)) data))))))

;; (defview
;;   permission
;;   (jarman-table
;;    :name
;;    "permission"
;;    :plug-place
;;    [:#tables-view-plugin]
;;    :tables
;;    [:permission]
;;    :model
;;    [:permission.id
;;     :permission.permission_name
;;     :permission.configuration]
;;    :query
;;    {:columns
;;     (as-is
;;      :permission.id
;;      :permission.permission_name
;;      :permission.configuration)}))



