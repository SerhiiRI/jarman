(ns jarman.logic.view-manager
  (:require
   ;; Clojure toolkit 
   [clojure.string :as string]
   [clojure.java.io :as io]
   [seesaw.core :as c]
   [seesaw.border :as b]
   ;; ---
   [jarman.tools.lang :include-macros true :refer :all]
   [jarman.tools.org  :refer :all]
   ;; ---
   [jarman.config.environment :as env]
   [jarman.config.vars :refer [defvar]]
   [jarman.config.dot-jarman :refer [dot-jarman-load]]
   ;; ---
   [jarman.plugin.plugin :as plugin-system]
   ;; --- 
   [jarman.logic.connection :as db]
   [jarman.logic.sql-tool :refer [select! update! insert! delete!]]
   [jarman.logic.metadata :as mt]
   [jarman.logic.state :as state]))

;;;;;;;;;;;;;;;;;
;;; Variables ;;;
;;;;;;;;;;;;;;;;;

(comment
  (dot-jarman-load)
  (deref user-menu)
  (deref jarman-plugin-list))

(defvar user-menu {}
  :name "Buisness menu"
  :doc "Left side user menu"
  :type clojure.lang.PersistentArrayMap
  :group :plugin-system)

;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; GLOBAL DEFVIEW MAP ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;

(def ^:private global-view-configs (atom {}))
(defn global-view-configs-clean []
  (reset! global-view-configs {}) nil)
(defn global-view-configs-get []
  @global-view-configs)
(defn global-view-configs-set [path configuration toolkit entry]
  {:pre [(every? keyword? path)]}
  (swap! global-view-configs
         (fn [m] (assoc-in m path
                          {:config configuration
                           :toolkit toolkit
                           :entry entry}))) nil)

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
        add-permission (key-setter :permission :ekka-all)
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
       ~@(for [{:keys [plugin-name plugin-config-path] :as cfg} cfg-list]
           (if-let [{:keys [plugin-toolkit-fn plugin-test-spec-fn plugin-entry-fn]}
                    (get-in (group-by :plugin-name (plugin-system/system-ViewPlugin-list-get))
                            [(symbol plugin-name) 0])]
             (do
              (plugin-test-spec-fn cfg)
              (global-view-configs-set plugin-config-path cfg (plugin-toolkit-fn cfg)
                                       (eval (fn [] (plugin-entry-fn plugin-config-path global-view-configs-get))))))) nil)))

;; (first (get (group-by :plugin-name (plugin-system/system-ViewPlugin-list-get))
;;       (symbol 'aaa)))
;; '(:plugin-test-spec-fn :plugin-entry-fn :plugin-toolkit-fn :plugin-description :plugin-name)

;; (defview permission
;;   (aaa
;;    :id :permission
;;    :name "permission"
;;    :plug-place [:#tables-view-plugin]))

;; (global-view-configs-clean)
;; (global-view-configs-get)

;;;;;;;;;;;;;;;;;;;;;
;;; DEBUG SEGMENT ;;; 
;;;;;;;;;;;;;;;;;;;;;

(comment
  (defview permission
    (table
     :id :permission
     :name "permission"
     :plug-place [:#tables-view-plugin]
     :tables [:permission]
     :view-columns [:permission.permission_name :permission.configuration]
     :model-insert [:permission.permission_name :permission.configuration]
     :active-buttons [:insert :update :delete :clear :changes]
     :permission [:admin :user :developer]
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
     :id :permission-table
     :name "permission dialog"
     :permission [:admin :user :developer]
     :tables [:permission]
     :view-columns [:permission.permission_name :permission.configuration]
     :query
     {:table_name :permission,
      :column
      [:#as_is
       :permission.id
       :permission.permission_name
       :permission.configuration]})))

;;; ---------------------------------------
;;; eval this function and take a look what
;;; you get in that configuration
;;; ---------------------------------------

(comment
  (do-view-load)
  (return-structure-tree (deref user-menu))
  (return-structure-flat (deref user-menu))
  (global-view-configs-clean)
  (global-view-configs-get)
  
  (get-in (global-view-configs-get)  [:service_contract :service-period :service_contract :toolkit])
  (get-in (global-view-configs-get)  [:permission :dialog-bigstring :select-name-permission])
  ((get-in (global-view-configs-get) [:permission :dialog-bigstring :my-custom-dialog :toolkit :dialog]))
  ((get-in (global-view-configs-get) [:permission :dialog-table :my-custom-dialog :toolkit :dialog])))

;;;;;;;;;;;;;;;;;;
;;; PluginLink ;;;
;;;;;;;;;;;;;;;;;;

(defprotocol IPluginQuery
  (return-path       [this])
  (return-entry      [this])
  (return-permission [this])
  (return-title      [this])
  (return-config     [this])
  (exists?           [this]))

(defrecord PluginLink [plugin-path]
  IPluginQuery
  (return-path [this] (.plugin-path this))
  (return-entry [this] (get-in (global-view-configs-get) (conj (return-path this) :entry)))
  (return-title [this] (get-in (global-view-configs-get) (conj (return-path this) :config :name)))
  (return-permission [this] (get-in (global-view-configs-get) (conj (return-path this) :config :permission)))
  (return-config [this] (get-in (global-view-configs-get) (conj (return-path this) :config)))
  (exists? [this] (some? (get-in (global-view-configs-get) (return-path this)))))

(defn isPluginLink? [^jarman.logic.view_manager.PluginLink e]
  (instance? jarman.logic.view_manager.PluginLink e))

(defn plugin-link [^clojure.lang.PersistentVector plugin-path]
  {:pre [(every? keyword? plugin-path)]}
  (->PluginLink plugin-path))

(comment
  ;; test segment for some link
  (def --test-link (plugin-link [:user :table :user]))
  (.return-path --test-link)
  (.return-entry --test-link)
  (.return-permission --test-link)
  (.return-title --test-link)
  (.return-config --test-link)
  (.exists? --test-link))

;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; GENERATOR MENU VIEW ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- recur-walk-throw [m-config f path]
  (let [[header tail] (map-destruct m-config)]
    (if (some? header)
      (let [[[k v] & _] header
            path (conj path k)]
        (cond
          (map? v)
          (do (f header path)
              (recur-walk-throw v f path))

          :else (f header path))))
    (if (some? tail) (recur-walk-throw tail f path))))

(defn- return-structure-flat [m]
  (let [result-vec (atom [])
        f (fn [[[k v] & _] path]
            (if (vector? v)
              (swap! result-vec conj (conj path (plugin-link v)))))]
    (recur-walk-throw m f [])
    @result-vec))

(defn- return-structure-tree [m]
  (let [result-vec (atom {})
        f (fn [[[k v] & _] path]
            (if (vector? v)
              ;; @julia
              (do (let [plugin (plugin-link v)]
                    (if true
                      ;; TO DO - fix this, func in main menu fn part-plugin
                      ;; chnage to predicate (.return-permission plugin) 
                      (swap! result-vec assoc-in path (plugin-link v))
                      (print-line (format
                                   "Permission plugin by path =%s= not allowed"
                                   (str path))))))))]
    (recur-walk-throw m f [])
    @result-vec))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; loader chain for `defview` ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; +--(database view table)---------+
;; | (defview user (table ....      |                               +-------------------...
;; | (defview permission (table ... |                               | Just simple function
;; | (defview service_contract (..  |                               | that load defviews from
;; | ....                           |                               | the simple file view.clj
;; +--------------------------------+                                \-------------------...-
;;                                   \    (make-loader-chain          /
;;                                    \      loader-from-db  ---------
;;                                    |      loader-from-view-clj ----
;; +-(view.clj file)----------------+ |      -----+----               \
;; | {:host "localhost, :port ...}  | |           |                    +------------------..
;; |                                | |           |                    | Load defview-s from
;; | (in-ns 'jarman.logi...)        | |           V                    | special `view` table
;; |                                | |   *view-loader-chain-fn*       | in database
;; | (defview permission            | |  (define a source for loadi-   +---------------....
;; |  (table                        | |   ng the defview declaration
;; |   :id :permission              | |   in the sequence passed in
;; |   :name "Permis...             | |   `make-loader-chain`)
;; |   :ot...                       | |       ----+----
;; |   ...                          | |           |
;; +--------------------------------+ |           |
;;                                   \|           V
;;                                    \---> (do-view-load)
;;                                          the function use chain variable
;;             do-view-load starting              |
;;             to load all defview                |
;;              +---------------------------------+
;;              |
;;              V
;;          evaluation ---(error)---> (ex-info "something went wrong"..
;;              |
;;          (success)
;;         Table plugin's was
;;         loaded to global view
;;         plugin storage


;;;  LOADERS ;;;

(defn loader-from-db
  ([] (loader-from-db nil))
  ([path](let [con (dissoc (db/connection-get)
                           :dbtype :user :password
                           :useUnicode :characterEncoding)
               req (list 'in-ns (quote (quote jarman.logic.view-manager)))
               data (map (fn [x] (read-string (:view x))) (db/query (select! {:table_name :view}))) 
               sdata (if-not (empty? data) (concat [con req] data))
               path  (if (nil? path)
                       (try (str (env/get-view-clj))
                            (catch Exception e (do (spit "./view.clj" "") "./view.clj")))
                       path)]
           (println "path" path)
           (if-not (empty? data)
             (do (spit path "")
                 (with-open [W (io/writer (io/file path) :append true)]
                   (doall
                    (for [s sdata]
                      (do (.write W (pp-str s))
                          (.write W env/line-separator)))))))
           data)))

(defn loader-from-view-clj
  ([] (loader-from-view-clj nil))
  ([path] (if (.exists (env/get-view-clj))
            (let [data 
                  (try
                    (read-seq-from-file  (if (nil? path) "src/jarman/logic/view.clj" path))
                    (catch Exception e (print-line (str "caught exception: file not find" (.toString e)))))
                  con-guard (first data)
                  con-data  (dissoc (db/connection-get)
                                    :dbtype :user :password
                                    :useUnicode :characterEncoding)]
              (cond
                ;; -----------
                (empty? (drop 2 data))
                (print-line "View.clj loader. Views is empty")
                ;; -----------
                (not= con-guard con-data) 
                (print-line (format "Error! View.clj si not related to connected db. guard(%s), connected database(%s)"
                                    (string/join ", " (->> (vals con-guard) (map pr-str)))
                                    (string/join ", " (->> (vals  con-data) (map pr-str)))))
                ;; -----------
                (not (every? #(= 'defview (first %)) (drop 2 data)))
                (print-line "Everything(omiting connection map and `in-ns`) in view.clj MUST be only `defview`. File is corrupted")
                ;; If all the things gone alright
                :else (drop 2 data))))))

(comment
  (loader-from-db)
  (loader-from-view-clj))

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

(defvar view-src :database
  :name "Defviews src"
  :doc  "Source of defviews. View.clj or database [:view.clj :database]."
  :type clojure.lang.Keyword
  :group :view-params)

(state/set-state :view-src (deref view-src))

(def ^:dynamic *view-loader-chain-fn*
  ;;(make-loader-chain loader-from-view-clj loader-from-db)
  (make-loader-chain loader-from-db loader-from-view-clj))

(comment
  (do-view-load)
  ;; ----
  (binding [*view-loader-chain-fn* (make-loader-chain loader-from-db loader-from-view-clj)
            *view-loader-chain-fn* (make-loader-chain loader-from-view-clj loader-from-db)]
    (do-view-load)))

(defn do-view-load
  "using in self `*view-loader-chain-fn*`, swapp using
  make-loader chain. deserialize view, and execute every
  defview."
  []
  (let [data (*view-loader-chain-fn*)]
    (if (empty? data)
      (state/concat-state :state-alerts
                          [[:danger "Error" "Problem with tables. Data not found in DB
                                <br><br><b>Module:</b> view-manager
                                    <br><b>Fn:</b> do-view-load"]])
      (binding [*ns* (find-ns 'jarman.logic.view-manager)] 
        (doall (for [defview-sexpression data]
                 (try (eval defview-sexpression)
                      (catch Exception e
                        (throw
                         (ex-info (format "Failure compiling (defview `%s`)" (second defview-sexpression))
                                  {:type :view-plugin-error
                                   :internal-message (.getMessage e)
                                   :internal-stacktrace (clojure.stacktrace/print-stack-trace e 20)}))))))))
    (return-structure-tree (deref user-menu))))


(defn view-clean []
  (db/exec (delete! {:table_name :view})))

(defn view-get
  "Description
    get view from db by table-name
  Example
    (view-get \"user\")
    {:id 2, :table_name \"user\", :view   \"(defview user (table :name \"user\"......))})"
  [table-name]
  (first (db/query
          (select! {:table_name :view :where [:= :table_name (name table-name)]}))))

(defn view-set
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
      (db/exec (update! {:table_name :view :set {:view table-view} :where [:= :id id-t]}))) ;; TODO: Serhii: Update crashing: Don't know how to create ISeq from: java.math.BigInteger
    ;;(loader-from-db)
    (println "Inserting view: " table-name)))

(defn- views-table-list
  "Description:
     Pull up from DB names and ids of tables.
   Example:
     (pullup-tables-ids)
     ;; => (:documents :profile ...)"
  []
  (into {} (doall
            (map (fn [m] {(keyword (:table_name m)) (:id m)})
                 (db/query
                  (select!
                   {:table_name :metadata
                    :column [:table_name :id]}))))))

(defn move-views-to-db
  ([] (move-views-to-db nil))
  ([path] (let [table-views (drop 2 (loader-from-view-clj path))]
        (assert
         (every? #(= 'defview (first %)) table-views)
         "Everything(omiting connection map and `in-ns`) in view.clj MUST be only `defview`")
        (view-clean)
        (doall
         (map (fn [table-view]
                (let [table-map  (coll-to-map (drop 1 (first (filter #(sequential? %) table-view))))
                      table-name (:name table-map)]
                  (view-set {:table_name table-name :view (str table-view) ;; (with-out-str (clojure.pprint/pprint table-view))
                             })))
              table-views)))))

(comment
  (move-views-to-db))

;;;;;;;;;;;;;;;;;;;;;
;;; DEBUG SEGMENT ;;;
;;;;;;;;;;;;;;;;;;;;;

(defn- is-plugin? [m]
  (if (map? m)
   (and
    (contains? m :config)
    (contains? m :entry)
    (contains? m :toolkit))))

(defn- recur-walk-throw-views [m-config f path]
  (let [[header tail] (map-destruct m-config)]
    (if (some? header)
      (let [[[k v] & _] header
            path (conj path k)]
        (cond
          (and (map? v) (not (is-plugin? v)))
          (do (f header path)
              (recur-walk-throw-views v f path))

          :else (f header path))))
    (if (some? tail) (recur-walk-throw-views tail f path))))

(defn plugin-paths []
  (let [result-vec (atom [])
        f (fn [[[k v] & _] path]
            (if (is-plugin? v)
              (swap! result-vec conj path)))]
    (recur-walk-throw-views (global-view-configs-get) f [])
    @result-vec))

(defn- plugin-paths-getters []
  (for [path (plugin-paths)]
    (list 'get-in (list 'global-view-configs-get) path)))

(defn- plugin-paths-fn [f & plugin-partial-path]
  (let [list-all
        (mapcat
         vec
         (for [path (plugin-paths)]
           (f path)))]
    (if (empty? plugin-partial-path)
      list-all
      (filter
       #(= (take (count plugin-partial-path) %) plugin-partial-path)
       list-all))))

(def plugin-paths-all (partial plugin-paths-fn
                         (fn [path]
                           [(conj path :toolkit)
                            (conj path :config)
                            (conj path :entry)])))
(def plugin-paths-toolkit (partial plugin-paths-fn (fn [path] [(conj path :toolkit)])) )
(def plugin-paths-entry (partial plugin-paths-fn (fn [path] [(conj path :entry)])) )
(def plugin-paths-config (partial plugin-paths-fn (fn [path] [(conj path :config)])) )

(defn plugin-open-in-frame [& paths]
  (for [path paths]
   (if-let [plugin (get-in (global-view-configs-get) path)]
     (let [{{name :name} :config
            entry :entry}
           plugin]
       (-> (doto (c/frame :title name
                          :undecorated? false
                          :resizable? false
                          :content (entry)
                          :minimum-size [600 :by 600])
             (.setLocationRelativeTo nil)) seesaw.core/pack! seesaw.core/show!)))))

(comment
  ;; return in different way
  ;; path's list to plugins
  (plugin-paths)
  (plugin-paths-all)
  (plugin-paths-toolkit)
  (plugin-paths-entry)
  (plugin-paths-config)
  ;; you can limit path from start keys
  ;; for exmaple return all keys for
  ;; :permission 
  (plugin-paths-all :permission)
  ;; or more concrate plugin etc.
  (plugin-paths-all :permission :dialog-table)
  ;; get simple debug map getter
  (plugin-paths-getters)
  ;; open some plugin :entryes in frame
  (plugin-open-in-frame [:user :table :user])
  ;; or two and more interesting to us plugins
  (plugin-open-in-frame
   [:user :table :user]
   [:permission :table :permission]))

;; (do-view-load)
;; ( (get-in (global-view-configs-get) [:seal :table :seal :toolkit :columns-group]) {})
;; ((:select-expression (get-in (global-view-configs-get) [:repair_contract :table :repair_contract :toolkit])))
