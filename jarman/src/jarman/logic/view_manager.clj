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
   [jarman.logic.metadata :as mt])
  (:import (java.util Date)
           (java.text SimpleDateFormat)))


(defn- toolkit-pipeline-reducer [configuration & toolkit-list]
  (reduce (fn [acc-toolkit toolkit-pipeline]
            (toolkit-pipeline configuration acc-toolkit))
          {}
          toolkit-list))

(def ^:private views-configuration+toolkit (atom {}))
(defn views-configuration+toolkit-get []
  @views-configuration+toolkit)
(defn views-configuration+toolkit-set [keyword-table-name plugin-name plugin-configuration toolkit]
  {:pre [(keyword? keyword-table-name)]}
  (swap! views-configuration+toolkit
         (fn [m] (assoc-in m [keyword-table-name plugin-name]
                          {:configuration plugin-configuration
                           :toolkit toolkit}))) nil)

(defn get-parameters [data]
  (loop [prm {} l data plugins []]
    (if (not-empty l)
      (cond
        (keyword? (first l)) (recur (into prm {(first l) (second l)}) (drop 2 l) plugins)
        (list? (first l)) (recur prm (drop 1 l) (conj plugins (first l)))
        :else (recur prm (drop 1 l) plugins))
      [(if-not (contains? prm :permision) (assoc prm :permission [:user]))
       plugins])))


(defmacro defview-debug [table-model-name & body]
  (let [[conf-prms fbody] (get-parameters body)
        configurations
        (reduce into {}
                (for [form fbody]
                  (if (sequential? form)
                    (let [s `(hash-map ~@(rest form)
                                       :table-name ~(keyword table-model-name))
                          s `(merge ~conf-prms ~s)]
                      `{~(keyword (first form)) {:configuration ~s
                                                 :data-toolkit (data-toolkit-pipeline ~s)}}))))]
    `~configurations))

(defmacro defview [table-model-name & body]
  (let [[conf-prms fbody] (get-parameters body)
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
                    plugin-data-toolkit#  (toolkit-pipeline-reducer (get cfg# kplugin#) [data-toolkit-pipeline])]
                (views-configuration+toolkit-set ktable#
                                                 kplugin#
                                                 plugin-configuration#
                                                 plugin-data-toolkit#)
                (~f [ktable# kplugin#] ~'views-configuration+toolkit-get)))
         nil)))

(defn as-is [& column-list]
  (map #(if (keyword? %) {% %} %) column-list))

;; (defview-debug permission
;;   (plug/jarman-table
;;    :name "Table"
;;    :place nil
;;    :tables [:permission]
;;    :view   [:permission.permission_name]
;;    :query   {:column (as-is :permission.id :permission.permission_name :permission.configuration)}))

(defn- read-one
  [r]
  (try (read r)
       (catch java.lang.RuntimeException e
         (if (= "EOF while reading" (.getMessage e)) ::EOF
             (throw e)))))

(defn read-seq-from-file
  "Reads a sequence of top-level objects in file at path."
  [path]
  (with-open [r (java.io.PushbackReader. (clojure.java.io/reader path))]
    (binding [*read-eval* false]
      (doall (take-while #(not= ::EOF %) (repeatedly #(read-one r)))))))

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
                            (for [s sdata]
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
;;   :pkey
;;   [:user :admin]
;;   (jarman-table
;;    :name
;;    [:ke]
;;    :pkey
;;    "smt"
;;    :plug-place
;;    [:#tables-view-plugin]
;;    :tables
;;    [:permission]
;;    :modal
;;    [:permission.permission_name :permission.configuration]
;;    :query
;;    {:columns
;;     (as-is
;;      :permission.id
;;      :permission.permission_name
;;      :permission.configuration)}))
;;view

;;plug-place

;;name
