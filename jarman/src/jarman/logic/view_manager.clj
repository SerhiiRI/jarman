(ns jarman.logic.view-manager
  (:require
   ;; Clojure toolkit 
   [clojure.string :as string]
   [clojure.java.io :as io]
   [seesaw.core :as c]
   [seesaw.border :as b]
   ;; Jarman toolkit
   [jarman.tools.lang :include-macros true :refer :all]
   [jarman.config.environment :as env]
   [jarman.config.dot-jarman-param :refer [defvar]]
   [jarman.plugin.jspl :as jspl]
   ;; --- 
   [jarman.logic.connection :as db]
   [jarman.gui.gui-components :as gcomp]
   [jarman.logic.sql-tool :refer [select! update! insert! delete!]]
   [jarman.logic.metadata :as mt]
   [jarman.logic.state :as state]))

;;;;;;;;;;;;;;;;;
;;; Variables ;;;
;;;;;;;;;;;;;;;;;

(comment
 (defvar dupa1 nil)
 (defvar dupa2 nil
   :type clojure.lang.PersistentHashMap)
 (defvar dupa3 nil
   :type clojure.lang.PersistentHashMap)
 (defvar dupa4 nil
   :type clojure.lang.PersistentHashMap
   :group :plugin)
 (defvar dupa5 nil
   :type clojure.lang.PersistentHashMap
   :group :plugin)
 (defvar dupa6 nil
   :type clojure.lang.PersistentHashMap
   :group :cargo))
(defvar user-menu
  {"Admin space"
       {"User table"                [:user :table :user]
        "Permission edit"           [:permission :table :permission]}
       "Sale structure"
       {"Enterpreneur"              [:enterpreneur :table :enterpreneur]
        "Point of sale group"       [:point_of_sale_group :table :point_of_sale_group]
        "Point of sale group links" [:point_of_sale_group_links :table :point_of_sale_group_links],
        "Point of sale"             [:point_of_sale :table :point_of_sale]}
       "Repair contract"
       {"Repair contract"           [:repair_contract :table :repair_contract]
        "Repair reasons"            [:repair_reasons :table :repair_reasons]
        "Repair technical issue"    [:repair_technical_issue :table :repair_technical_issue]
        "Repair nature of problem"  [:repair_nature_of_problem :table :repair_nature_of_problem]
        "Cache register"            [:cache_register :table :cache_register]
        "Seal"                      [:seal :table :seal]}
       "Service contract"
       {"Service contract"          [:service_contract :table :service_contract]
        "Service contract month"    [:service_contract_month :table :service_contract_month]}}
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
       ~@(for [{:keys [plugin-name plugin-config-path] :as cfg} cfg-list]
           (let [plugin-toolkit-pipeline (eval `~(symbol (format "jspl/%s-toolkit-pipeline" plugin-name)))
                 plugin-test-spec        (eval `~(symbol (format "jspl/%s-spec-test"        plugin-name)))
                 plugin-component        (resolve `~(symbol (format "jspl/%s"                  plugin-name)))
                 toolkit (plugin-toolkit-pipeline cfg)]
             (plugin-test-spec cfg)
             (global-view-configs-set plugin-config-path cfg toolkit (eval (fn [] (plugin-component plugin-config-path global-view-configs-get)))))) nil)))

;;;;;;;;;;;;;;;;;;;;;
;;; DEBUG SEGMENT ;;; 
;;;;;;;;;;;;;;;;;;;;;

(defmacro defview-debug [table-name & body]
  (let [cfg-list (defview-prepare-config table-name body)]
    `(list
      ~@(for [{:keys [plugin-name] :as cfg} cfg-list]
          (let [plugin-toolkit-pipeline `~(symbol (format "jspl/%s-toolkit-pipeline" plugin-name))
                plugin-test-spec        `~(symbol (format "jspl/%s-spec-test"        plugin-name))]
            `(do (~plugin-test-spec ~cfg)
                 {:config ~cfg :toolkit (~plugin-toolkit-pipeline ~cfg)}))))))


(defmacro defview-debug-map [table-name & body]
  (let [cfg-list (defview-prepare-config table-name body)]
    `(do
       ~(reduce
        (fn [cfg-acc {:keys [plugin-name plugin-config-path] :as cfg}]
          (let [plugin-toolkit-pipeline `~(symbol (format "jspl/%s-toolkit-pipeline" plugin-name))
                plugin-test-spec  (eval `~(symbol (format "jspl/%s-spec-test"        plugin-name)))]
             (plugin-test-spec cfg)
            `(assoc-in ~cfg-acc [~@plugin-config-path] {:config ~cfg :toolkit (~plugin-toolkit-pipeline ~cfg)})))
        {} cfg-list))))

;;; TEST SEGMENT ;;;

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
  (get-in (global-view-configs-get)
          [:service_contract :service-period :service_contract :toolkit])
  (get-in (global-view-configs-get)  [:permission :dialog-bigstring :select-name-permission])
  ((get-in (global-view-configs-get) [:permission :dialog-bigstring :my-custom-dialog :toolkit :dialog]))
  ((get-in (global-view-configs-get) [:permission :dialog-table :my-custom-dialog :toolkit :dialog])))

:service_contract :service-period :service_contract
:service-period :toolkit :service-period

;;;;;;;;;;;;;;;;;;
;;; PluginLink ;;;
;;;;;;;;;;;;;;;;;;

(defprotocol IPluginQuery
  (return-path [this])
  (return-entry [this])
  (return-permission [this])
  (return-title [this])
  (return-config [this])
  (exists? [this]))

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
              (swap! result-vec assoc-in path (plugin-link v))))]
    (recur-walk-throw m f [])
    @result-vec))

(defn- create-header
  ([item] {:pre [(string? item)]} item)
  ([item icon] (cond-> {}
                 item (assoc :item item)
                 icon (assoc :icon icon))))


(declare user-menu)
(def ^:private business-menu
  {"Admin space"
   {"User table"                [:user :table :user]
    "Permission edit"           [:permission :table :permission]}
   "Sale structure"
   {"Enterpreneur"              [:enterpreneur :table :enterpreneur]
    "Point of sale group"       [:point_of_sale_group :table :point_of_sale_group]
    "Point of sale group links" [:point_of_sale_group_links :table :point_of_sale_group_links],
    "Point of sale"             [:point_of_sale :table :point_of_sale]}
   "Repair contract"
   {"Repair contract"           [:repair_contract :table :repair_contract]
    "Repair reasons"            [:repair_reasons :table :repair_reasons]
    "Repair technical issue"    [:repair_technical_issue :table :repair_technical_issue]
    "Repair nature of problem"  [:repair_nature_of_problem :table :repair_nature_of_problem]
    "Cache register"            [:cache_register :table :cache_register]
    "Seal"                      [:seal :table :seal]}
   "Service contract"
   {"Service period"            [:service_contract :service-period :service_contract]
    "Service contract"          [:service_contract :table :service_contract]
    "Service contract month"    [:service_contract_month :table :service_contract_month]}})



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; loader chain for `defview` ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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
        (doall (map (fn [x] (eval x)) (subvec (vec data) 2)))))
    (return-structure-tree (deref user-menu))))

(defn- view-get
  "Description
    get view from db by table-name
  Example
    (view-get \"user\")
    {:id 2, :table_name \"user\", :view   \"(defview user (table :name \"user\"......))})"
  [table-name]
  (first (db/query
          (select! {:table_name :view :where [:= :table_name (name table-name)]}))))

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


(defn popup-defview-editor
  "Description:
     Prepared popup window with code editor for defview.
   Example:
     (popup-defview-editor \"user\")"
  [table-str]
  (let [dview (view-get table-str)]
      (gcomp/popup-window
       {:window-title (str "Defview manual table editor: " )
        :view (gcomp/code-editor
               {:val (with-out-str
                     (clojure.pprint/pprint
                      (read-string (binding [jarman.logic.sql-tool/*debug* false]
                                     (:view (view-get table-str)))))) ;;(:view dview)
                :dispose true
                :save-fn (fn [state]
                           (try
                             (view-set (assoc dview :view (c/config (:code state) :text)))
                             (c/config! (:label state) :text "Saved!")
                             (catch Exception e (c/config!
                                                 (:label state)
                                                 :text "Can not convert to map. Syntax error."))))})})))


(defn view-defview-editor
  "Description:
     Prepared popup window with code editor for defview.
   Example:
     (popup-defview-editor \"user\")
  "
  [table-str]
  (let [dview (view-get table-str)]
    ((state/state :jarman-views-service)
     :set-view
     :view-id (keyword (str "manual-defview-code" table-str))
     :title (str "Defview: " table-str)
     :component-fn
     (fn [] (gcomp/code-editor
             {:args [:border (b/line-border :top 1 :left 1 :color "#eee")
                     :background "#fff"]
              :title (str "Defview: " table-str)
              :val (with-out-str
                     (clojure.pprint/pprint
                      (read-string (binding [jarman.logic.sql-tool/*debug* false]
                                     (:view (view-get table-str)))))) ;;(:view dview)
              :save-fn (fn [state]
                         (try
                           (view-set (assoc dview :view (c/config (:code state) :text)))
                           (c/config! (:label state) :text "Saved!")
                           (catch Exception e (c/config!
                                               (:label state)
                                               :text "Can not convert to map. Syntax error."))))})))))


;;(with-out-str (clojure.pprint/pprint (str (read-string (:view (view-get "permission"))))))

;;(popup-defview-editor "user")
(defn buttons-list--code-editor-defview
  "Description:
     Inject expand button then when pointing id.
   Example:
     (buttons-list--code-editor-defview :#expand-menu-space)
  "
  [plugplace-id]
  (let [table-and-view-coll (db/query
                             (select!
                              {:table_name :view
                               :column [:table_name]}))
        comp (gcomp/button-expand
              "Defviews Editors"
              (doall
               (map
                (fn [m]
                  (gcomp/button-expand-child
                   (:table_name m)
                   :onClick (fn [e] (view-defview-editor (:table_name m))) ))
                table-and-view-coll)))]
    (.add (c/select (state/state :app) [:#expand-menu-space]) comp)
    (.revalidate (c/to-root (state/state :app)))))

(defn prepare-defview-editors-state
  "Description:
     Prepare state with defview editor fns for view service
     and set to state with :defview-editors key.
     Invoke again to refresh state.
   Example:
     (prepare-defview-editors-state)" []
  (let [table-and-view-coll (db/query
                             (select!
                              {:table_name :view
                               :column [:table_name]}))]
    (doall
     (state/set-state
      :defview-editors
      (into
       {}
       (map
        (fn [m]
          {(keyword (:table_name m)) (fn [e] (view-defview-editor (:table_name m)))})
        table-and-view-coll))))))

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


;;; --------------------------------Data Toolkit----------------------------------

(import 'java.text.DateFormat)
(import 'java.text.ParseException)
(import 'java.text.SimpleDateFormat)
(import 'java.util.Calendar)
(import 'java.util.Date)

(defn date-object
  "Remember that simple (date) ruturn current
  date and time.
  Also if you 

  Example:
  (date 1900 11 29 1 2 3) => 1900-12-29 01:02:03
  (date 1900 11 29 1 2)   => 1900-12-29 01:02:00
  (date 1900 11 29 1)     => 1900-12-29 01:00:00
  (date 1900 11 29)       => 1900-12-29 00:00:00
  (date 1900 11)          => 1900-12-01 00:00:00
  (date 1900)             => 1900-01-01 00:00:00
  (date)                  => 2020-02-29 19:07:41"
  ([] (java.util.Date.))
  ([YYYY] (date-object YYYY 0 1 0 0 0))
  ([YYYY MM] (date-object YYYY MM 1 0 0 0))
  ([YYYY MM dd] (date-object YYYY MM dd 0 0 0))
  ([YYYY MM dd hh] (date-object YYYY MM dd hh 0 0))
  ([YYYY MM dd hh mm] (date-object YYYY MM dd hh mm 0))
  ([YYYY MM dd hh mm ss] (java.util.Date. (- YYYY 1900) MM dd hh mm ss)))

(defn get-date-pairs
  "Description
    (get-date-pairs
      (date-object 2021 8  13)
      (date-object 2021 11 15))
      => [[\"2021-09-13\" \"2021-09-30\"] [\"2021-10-01\" \"2021-10-31\"] [\"2021-11-01\" \"2021-11-30\"] [\"2021-12-01\" \"2021-12-15\"]]"
  [data-s data-e]
  (let [formater (java.text.SimpleDateFormat. "YYYY-MM-dd")
        CONST_START    (java.util.Calendar/getInstance)
        CONST_END      (java.util.Calendar/getInstance)
        beginCalendar  (java.util.Calendar/getInstance)
        finishCalendar (java.util.Calendar/getInstance)
        ;; FOR DEBUG DATA
        ;; data-s (date-object 2021 8  13)
        ;; data-e (date-object 2021 11 15)
        buffer (ref [])]
    (.setTime beginCalendar data-s)
    (.setTime finishCalendar data-e)
    (.setTime CONST_START data-s)
    (.setTime CONST_END data-e)
    (.set beginCalendar java.util.Calendar/DAY_OF_MONTH (.getActualMinimum beginCalendar java.util.Calendar/DAY_OF_MONTH))
    (while (.before beginCalendar finishCalendar)
      (let [start-month (java.util.Calendar/getInstance)
            end-month (java.util.Calendar/getInstance)]
        (if (= (.get CONST_START java.util.Calendar/MONTH) (.get beginCalendar java.util.Calendar/MONTH))
          (do
            (.setTime start-month (.getTime CONST_START)))
          (do
            (.setTime start-month (.getTime beginCalendar))
            (.set start-month java.util.Calendar/DAY_OF_MONTH
                  (.getActualMinimum beginCalendar java.util.Calendar/DAY_OF_MONTH))))
        (if (= (.get CONST_END java.util.Calendar/MONTH) (.get beginCalendar java.util.Calendar/MONTH))
          (do
            (.setTime end-month (.getTime CONST_END)))
          (do
            (.setTime end-month (.getTime beginCalendar))
            (.set end-month java.util.Calendar/DAY_OF_MONTH
                  (.getActualMaximum beginCalendar java.util.Calendar/DAY_OF_MONTH))))
        #_(dosync (alter buffer conj
                       (format
                        "%s - %s"
                        (.format formater (.getTime start-month))
                        (.format formater (.getTime end-month)))))
        (dosync (alter buffer conj
                       [(.format formater (.getTime start-month))
                        (.format formater (.getTime end-month))]))
        (.add beginCalendar java.util.Calendar/MONTH 1)))
    (deref buffer)))



;;; ---------------------SQL Inserts func---------------------------


;; -------------
;; clean-up keys
;; -------------
;; (select-keys {} [:id :was_payed :service_month_start :money_per_month :service_month_end :id_service_contract])
;; (select-keys {} [:service_contract.id :service_contract.id_enterpreneur :service_contract.contract_start_term :service_contract.contract_end_term])
;; (select-keys {} [:enterpreneur.id :enterpreneur.name :enterpreneur.ssreou :enterpreneur.ownership_form :enterpreneur.vat_certificate :enterpreneur.individual_tax_number :enterpreneur.director :enterpreneur.accountant :enterpreneur.legal_address :enterpreneur.physical_address :enterpreneur.contacts_information])



;; -----------
;; all columns
;; -----------
#_[:#as_is

   ;; enterpr
   :enterpreneur.id
   :enterpreneur.name
   :enterpreneur.ssreou
   :enterpreneur.ownership_form
   :enterpreneur.vat_certificate
   :enterpreneur.individual_tax_number
   :enterpreneur.director
   :enterpreneur.accountant
   :enterpreneur.legal_address
   :enterpreneur.physical_address
   :enterpreneur.contacts_information

   ;; contract
   :service_contract.id
   :service_contract.id_enterpreneur
   :service_contract.contract_start_term
   :service_contract.contract_end_term

   ;; contract month
   :service_contract_month.id
   :service_contract_month.id_service_contract
   :service_contract_month.service_month_start
   :service_contract_month.service_month_end
   :service_contract_month.money_per_month
   :service_contract_month.was_payed]

;;; INFO SELECTS ;;;

(defn info-for-enterpreneur [enterpreneur_id]
  {:pre [(number? enterpreneur_id)]}
  (db/query
   (select!
    {:table_name :service_contract_month,
     :inner-join
     [:service_contract_month->service_contract
      :service_contract->enterpreneur],
     :column
     [:#as_is
      ;; enterpr
      :enterpreneur.id
      :enterpreneur.name
      :enterpreneur.ssreou
      :enterpreneur.ownership_form
      :enterpreneur.vat_certificate
      :enterpreneur.individual_tax_number
      :enterpreneur.director
      :enterpreneur.accountant
      :enterpreneur.legal_address
      :enterpreneur.physical_address
      :enterpreneur.contacts_information
      ;; contract
      :service_contract.id
      :service_contract.contract_start_term
      :service_contract.contract_end_term
      ;; contract month
      :service_contract_month.id
      :service_contract_month.service_month_start
      :service_contract_month.service_month_end
      :service_contract_month.money_per_month
      :service_contract_month.was_payed]
     :where [:= :enterpreneur.id enterpreneur_id]})))

(defn info-for-service_contract [service_contract_id]
  {:pre [(number? service_contract_id)]}
  (db/query
   (select!
    {:table_name :service_contract,
     :inner-join [:service_contract_month<-service_contract],
     :column
     [:#as_is
      ;; contract
      :service_contract.id
      :service_contract.contract_start_term
      :service_contract.contract_end_term
      ;; contract month
      :service_contract_month.id
      :service_contract_month.service_month_start
      :service_contract_month.service_month_end
      :service_contract_month.money_per_month
      :service_contract_month.was_payed
      ]
     :where [:= :service_contract.id service_contract_id]})))

(defn get-paiment-for-enterpreneur [enterpreneur_id]
  {:pre [(number? enterpreneur_id)]}
  (reduce
   (fn [acc x] (+ acc (:service_contract.money_per_month x))) 0       
   (db/query (select! {:table_name :service_contract_month,
                       :inner-join
                       [:service_contract_month->service_contract
                        :service_contract->enterpreneur],
                       :column
                       [:#as_is
                        :service_contract.money_per_month
                        :service_contract.id
                        :enterpreneur.id]
                       :where [:= :enterpreneur.id enterpreneur_id]}))))

;;; PAYMENT SELECTS

(defn get-payment-for-enterpreneur [enterpreneur_id]
  {:pre [(number? enterpreneur_id)]}
  (reduce
   (fn [acc x] (+ acc (:service_contract.money_per_month x))) 0       
   (db/query (select! {:table_name :service_contract_month,
                       :inner-join
                       [:service_contract_month->service_contract
                        :service_contract->enterpreneur],
                       :column
                       [:#as_is
                        :service_contract.money_per_month
                        :service_contract.id
                        :enterpreneur.id]
                       :where [:= :enterpreneur.id enterpreneur_id]}))))

(defn get-payment-for-service_contract [service_contract_id]
  {:pre [(number? service_contract_id)]}
  (reduce
   (fn [acc x] (+ acc (:service_contract.money_per_month x))) 0       
   (db/query (select! {:table_name :service_contract_month,
                       :inner-join
                       [:service_contract_month->service_contract
                        :service_contract->enterpreneur],
                       :column
                       [:#as_is
                        :service_contract.money_per_month
                        :service_contract.id
                        :enterpreneur.id]
                       :where [:= :service_contract.id service_contract_id]}))))


(comment
  (get-payment-for-enterpreneur 28)
  (get-payment-for-service_contract 2))

;;; INSERTS

(defn insert-service_contract [id_enterpreneur contract_start_term contract_end_term]
  {:pre [(instance? java.util.Date contract_start_term) (instance? java.util.Date contract_end_term)]}
  (let [formater (java.text.SimpleDateFormat. "YYYY-MM-dd")]
    (:generated_key
     (clojure.java.jdbc/execute!
      (insert! {:table_name :user
                :set {:id_enterpreneur id_enterpreneur
                      :contract_start_term (.format formater contract_start_term)
                      :contract_end_term   (.format formater contract_end_term)}}
               {:return-keys ["id"]})))))

(defn insert-service_contract_month [service_contract_id start-date end-date money_per_month]
  "Template
    (insert! {:table_name :service_contract
              :column-list [:id_service_contract :service_month_start :service_month_end :money_per_month :was_payed]
              :values [[1 \"2021-09-13\" \"2021-09-30\" 400] [1 \"2021-10-01\" \"2021-10-31\" 400]]})"
  {:pre [(instance? java.util.Date start-date) (instance? java.util.Date end-date)]}
  (db/exec
   (insert! {:table_name :service_contract_month
             :column-list [:id_service_contract :service_month_start :service_month_end :money_per_month :was_payed]
             :values (mapv (fn [[start-date end-date]]
                             (vector service_contract_id start-date end-date money_per_month false))
                           (get-date-pairs start-date end-date))})))

(defn insert-all [id_enterpreneur contract_start_term contract_end_term money_per_month]
  {:pre [(some? id_enterpreneur)
         (some? money_per_month)
         (instance? java.util.Date contract_start_term)
         (instance? java.util.Date contract_end_term)]}
  (if-let [service_contract_id (insert-service_contract id_enterpreneur
                                                        contract_start_term
                                                        contract_end_term)]
    (insert-service_contract_month
     service_contract_id
     contract_start_term
     contract_end_term
     money_per_month)
    (println "Service month not been created for enterprenier with '%d' id" id_enterpreneur)))

(comment 
  (insert-service_contract_month 1 (date-object 2021 8  13) (date-object 2021 11 15) 400)
  (insert-service_contract       1 (date-object 2021 8  13) (date-object 2021 11 15))
  (insert-all                    1 (date-object 2021 8  13) (date-object 2021 11 15) 400))


;;; UPDATE 

(defn update-list-service-month-start [list-of-service-month]
  (for [entity list-of-service-month]
    (if (:id entity)
      (update! {:table_name :user :set (dissoc entity :id) :where [:= :id (:id entity)]})
      (str "Cannot update nilable :id entity - "(pr-str entity)))))

(comment
 (update-list-service-month-start
  [{:id 1 :was_payed false
    :service_month_start "2021-09-13", :money_per_month 69,
    :service_month_end "2021-09-30", :id_service_contract 1}
   {:id 2 :was_payed true
    :service_month_start "2021-10-01", :money_per_month 400,
    :service_month_end "2021-10-31", :id_service_contract 1}
   {:id 3 :was_payed false
    :service_month_start "2021-11-01", :money_per_month 399,
    :service_month_end "2021-11-13", :id_service_contract 1}]))

;;; DELETES

(defn delete_service_contract [service_contract_id]
  {:pre [(some? service_contract_id)]}
  (and
   (apply #'and 
          (map :service_contract_month.was_payed
               (db/query
                (select! {:table_name :service_contract
                          :inner-join [:service_contract_month<-service_contract],
                          :column [:#as_is :service_contract_month.was_payed]
                          :where [:= :id service_contract_id]}))))
   ;; db/exec
   (delete! {:table_name :service_contract :where [:= :id service_contract_id]})))


;;[:service_contract :service-period :service_contract]
