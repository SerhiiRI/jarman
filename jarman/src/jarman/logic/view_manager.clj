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
   [jarman.plugin.data-toolkit :refer [data-toolkit-pipeline]])
  (:import (java.util Date)
           (java.text SimpleDateFormat)))


;;;;;;;;;;;;;;;;;;;;;;;
;;; HELPER FUNCTION ;;;
;;;;;;;;;;;;;;;;;;;;;;;

(defn- toolkit-pipeline [configuration & toolkit-list]
  (reduce (fn [acc-toolkit toolkit-pipeline]
            (toolkit-pipeline configuration acc-toolkit)) {} toolkit-list))


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

(defn ^:private key-setter
  "Description

    Test if some `parameter-k` key inside `m`
    if not, then add by this key `parameter-default-v`

    Param `parameter-default-v` may be value or 0-arg
    function

  Example
    (let [t1 (key-setter :permission [:user])
          t2 (key-setter :exist-key 'no)
          t3 (key-setter :fn-value #(gensym))]
       (-> {:exist-key 'yes} t1 t2 t3))
     ;; => {:exist-key   yes
            :permission  [:user]
            :fn-value    G__24897}"
  [parameter-k parameter-default-v]
  (fn [m] (if (contains? m parameter-k) m
           (assoc m parameter-k
                  (if (fn? parameter-default-v)
                    (parameter-default-v)
                    parameter-default-v)))))

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
       (jarman-table
        :name \"first Permission\")
       (jarman-table
        :id :UUUUUUUUUUUUUU
        :permission [:user]
        :name \"second Permission\")))
   ;; =>
    [{:--another :--param,
      :permission [:admin :user],
      :name \"first Permission\",
      :id :plugin-24793,
      :table-name :permission,
      :plugin-name jarman-table,
      :plugin-config-path [:permission :jarman-table :plugin-24793]}
     {:--another :--param,
      :permission [:user],
      :name \"second Permission\",
      :id :UUUUUUUUUUUUUU,
      :table-name :permission,
      :plugin-name jarman-table,
      :plugin-config-path [:permission :jarman-table :UUUUUUUUUUUUUU]}]"
  [table-name body]
  (let [add-id         (key-setter :id         #(keyword (gensym "plugin-")))
        add-table-name (key-setter :table-name (keyword table-name))
        add-permission (key-setter :permission [:user])
        k-table-name (keyword table-name)]
    (let [[global-cfg plugin-list] (sort-parameters-plugins body)]
      (reduce
       (fn [acc-cfg [plugin-name & plugin-cfg]]
         (blet (conj acc-cfg cfg)
               [cfg (add-id (merge global-cfg (apply hash-map plugin-cfg)))
                k-plugin-id       (:id cfg)
                k-plugin-name     (keyword plugin-name)
                add-plugin-name   (key-setter :plugin-name        (symbol plugin-name))
                add-full-path-cfg (key-setter :plugin-config-path [k-table-name k-plugin-name k-plugin-id])
                cfg (-> cfg
                        add-table-name
                        add-permission
                        add-plugin-name
                        add-full-path-cfg)])) [] plugin-list))))

;; -----------------------
;; For Julia with big love
;;              @Serhii
;; -----------------------
;; (defview-prepare-config
;;   'permission
;;   '(:--another :--param
;;     :permission [:admin :user]
;;     (jarman-table
;;      :name "first Permission")
;;     (jarman-table
;;      :name "thirtd Permission")
;;     (jarman-table
;;      :permission [:user]
;;      :name "second Permission")))

(defmacro defview [table-name & body]
  (let [cfg-list (defview-prepare-config table-name body)]
   `(do
      ~@(for [cfg cfg-list]
          (let [plugin-toolkit-pipeline `~(symbol (format "jspl/%s-toolkit-pipeline" (str (:plugin-name cfg))))
                toolkit (toolkit-pipeline cfg data-toolkit-pipeline plugin-toolkit-pipeline)]
            (global-view-configs-set (:plugin-config-path cfg) cfg toolkit)
            `(~(:plugin-name cfg) ~(:plugin-config-path cfg) ~'global-view-configs-get))) nil)))

(defn defview-debug-toolkit [cfg & plugin-toolkit-pipeline-list]
  (apply (partial toolkit-pipeline cfg data-toolkit-pipeline) plugin-toolkit-pipeline-list))

;;; TEST DEFVIEW SEGMENT
;; (defview permission
;;   :permission [:user :admin]
;;   (jarman-table
;;    :id :first-table
;;    :name "FIRST"
;;    :plug-place [:#tables-view-plugin]
;;    :tables [:permission]
;;    :view-columns [:permission.permission_name
;;                   :permission.configuration]
;;    :model [:permission.id
;;            {:model-reprs "First"
;;             :model-param :permission.permission_name
;;             :model-comp 'jarman.gui.gui-components/input-text-with-atom}
;;            :permission.configuration]
;;    :query {:column
;;            (as-is
;;             :permission.id
;;             :permission.permission_name
;;             :permission.configuration)})
;;   (jarman-table
;;    :permission "----------------"
;;    :name "SECOND"
;;    :plug-place [:#tables-view-plugin]
;;    :tables [:permission]
;;    :view-columns [:permission.permission_name
;;                   :permission.configuration]
;;    :model [:permission.id
;;            {:model-reprs "Second"
;;             :model-param :permission.permission_name
;;             :model-comp 'jarman.gui.gui-components/input-text-with-atom}
;;            :permission.configuration]
;;    :query {:column
;;            (as-is
;;             :permission.id
;;             :permission.permission_name
;;             :permission.configuration)}))

;;; ---------------------------------------
;;; Eval this function and take a look what
;;; you get in that configuration
;;;  See on:
;;;  `:permission`
;;;  `:id`
;;; ---------------------------------------
;;; 
;;; (global-view-configs-clean)
;;; (get-in (global-view-configs-get) [:permission :jarman-table])


;;;;;;;;;;;;;;;;;;;;;;;;;
;;; HELPERS `defview` ;;; 
;;;;;;;;;;;;;;;;;;;;;;;;;

(defn as-is [& column-list]
  (map #(if (keyword? %) {% %} %) column-list))

;;;;;;;;;;;;;;;;;;; 
;;; VIEW LOADER ;;;
;;;;;;;;;;;;;;;;;;;

(defn- read-one [r]
  (try (read r)
       (catch java.lang.RuntimeException e
         (if (= "EOF while reading" (.getMessage e)) ::EOF
             (throw e)))))

(defn- read-seq-from-file
  "Reads a sequence of top-level objects in file at path."
  [path]
  (with-open [r (java.io.PushbackReader. (clojure.java.io/reader path))]
    (binding [*read-eval* false]
      (doall (take-while #(not= ::EOF %) (repeatedly #(read-one r)))))))

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
                                     (select :view :where [:= :view.table_name table-name]))))]   
               (if-not (= s 0)
                 (if (nil? id-t)
                   (db/exec (insert :view :set {:table_name table-name, :view table-data}))
                   (db/exec (update
                             :view
                             :where [:= :id id-t]
                             :set {:view table-data})))))
              (+ s 1))
           (rest data)))))) 0 view-data))

(defn loader-from-db [db-connection]
  (let [con (dissoc (db/connection-get)
                    :dbtype :user :password
                    :useUnicode :characterEncoding)
        data (db/query (select! {:table-name :view
                                 :column    [:view]}))
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

(defn- load-data [data loaders]
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
  "Main function "
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

;; (do-view-load)
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



