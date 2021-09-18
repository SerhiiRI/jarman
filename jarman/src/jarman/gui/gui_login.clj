(ns jarman.gui.gui-login
  (:import (java.awt Dimension)
           (jiconfont.icons.google_material_design_icons GoogleMaterialDesignIcons))
  (:require [seesaw.core               :as c]
            [seesaw.border             :as b]
            [seesaw.util               :as u]
            [clojure.string            :as string]
            [jarman.config.config-manager     :as cm]
            [jarman.resource-lib.icon-library :as icon]
            [jarman.faces              :as face]
            [jarman.tools.swing        :as stool]
            [jarman.gui.gui-app        :as app] ;; Need for startup by state
            [jarman.logic.system-login :as system-login]
            [jarman.gui.gui-tools      :as gtool]
            [jarman.gui.gui-components :as gcomp]
            [jarman.gui.gui-migrid     :as gmg]
            [jarman.logic.connection   :as conn]
            [jarman.logic.state        :as state]
            [jarman.gui.gui-style      :as gs]
            [jarman.tools.lang :refer :all]))


;; ┌───────────────┐
;; │               │
;; │  State init   │
;; │               │
;; └───────────────┘

(defn- set-state-watcher
  "Description:
    Add watcher to component. If state was changed then rerender components in root using render-fn.
  Example:
    (set-state-watcher state! dispatch! container (fn [] component) [:path :to :state])"
  [state! dispatch! root render-fn watch-path id]
  (if (nil? (get-in (state!) watch-path))
    (dispatch! {:action :add-missing
                :path   watch-path}))
  (add-watch (state! :atom) id
   (fn [id-key state old-m new-m]
     (let [[left right same] (clojure.data/diff (get-in new-m watch-path) (get-in old-m watch-path))]
       (if (not (and (nil? left) (nil? right)))
         (let [root (if (fn? root) (root) root)]
           (try
             (do
               (c/config! root :items (render-fn))
               (.repaint root))
             (catch Exception e (println "\n" (str "gui_login.clj: Rerender exception in set-state-watcher:\n" (.getMessage e))) ;; If exeption is nil object then is some prolem with nechw component inserting
                    ))))))))


(defn- action-handler
  "Description:
    Invoke fn using dispatch!.
  Example:
    (@state {:action :test})"
  [state action-m]
  (case (:action action-m)
    :add-missing  (assoc-in state (:path action-m) nil)
    :test         (do (println "\nTest:n") state)
    :update-login (assoc-in state [:login]    (:value action-m))
    :update-pass  (assoc-in state [:passwd] (:value action-m))
    :load-connections-configs (assoc-in state [:connections] (:value action-m))
    :update-data-log (assoc-in state (into [:data-log] (:path action-m)) (:value action-m))
    :clear-data-log  (assoc-in state [:data-log] {})
    :set-current-config (assoc-in state [:current-config] (:value action-m))
    :update-current-config (assoc-in state [:current-config :value (:param action-m) :value] (:value action-m))
    :new-focus (assoc-in state [:current-focus] (:value action-m))
    ))

(defn- create-disptcher [atom-var]
  (fn [action-m]
    (swap! atom-var (fn [state] (action-handler state action-m)))))

;;(def dispatch! (create-disptcher state))

(defn- create-state-template
  "Description:
     State for login panel."
  []
  (atom {:login       nil
         :passwd      nil
         :connections {}
         :data-log    {}
         :current-config {}
         :focus-compo nil}))


(defn- load-connection-configs [dispatch!]
  (dispatch! {:action :load-connections-configs
              :value  (conn/datalist-mapper (conn/datalist-get))}))

(defn- new-focus [dispatch! compo]
  ;; (println "\n" "New focus")
  (dispatch! {:action :new-focus
              :value  compo}))

(defn- switch-focus
  ([state!]
   (timelife 0.2 (fn []
                   (let [to-focus (:current-focus (state!))]
                     (if to-focus (.requestFocus to-focus))))))
  ([state! dispatch! compo]
   (new-focus dispatch! compo)
   (switch-focus state!)))



;; ┌───────────────┐
;; │               │
;; │  Local comps  │
;; │               │
;; └───────────────┘

(defn- colors
  "Return color by keyname"
  [k]
  (k {:dark-grey-color  "#676d71"
      :blue-color       "#256599"
      :light-grey-color "#72858f"
      :blue-green-color "#2c7375"
      :light-blue-color "#96c1ea"
      :red-color        "#f01159"
      :light-red-color  "#Ffa07a"
      :back-color       "#c5d3dd"}))


(declare login-panel)

(defn- label-header
  ([txt] (label-header txt 16))
  ([txt fsize]
   (c/label
    :text txt
    :foreground (colors :blue-green-color)
    :font       (gtool/getFont fsize :bold))))

(defn- label-body
  [txt]
  (gcomp/multiline-text
   {:text txt
    :foreground (colors :light-grey-color)
    :font       (gtool/getFont 14 :bold)}))


(defn- return-to-login
  "Description:
     Generate button return to login panel."
  [state! dispatch!]
  (gcomp/button-basic
   (gtool/get-lang-btns :back-to-login-panel)
   :onClick (fn [e] (c/config!
                     (c/to-frame e)
                     :content (login-panel state! dispatch!)))
   :flip-border true
   :mouse-out "#eee"
   :tgap 6
   :bgap 6
   :args [:icon (gs/icon GoogleMaterialDesignIcons/PERSON face/c-icon)]))


(defn- about-panel
  "Description:
     Prepare info components"
  [info-list]
  (map
   (fn [[header body]]
     [(gmg/migrid
       :> :a
       (label-header header))
      (gmg/migrid
       :> :a {:gap [0 10 10 0]}
       (label-body body))])
   info-list))


(defn- faq-panel
  "Description:
     Return vertical mig with FAQs."
  [faq-list]
  [(-> (label-header (gtool/get-lang-header :faq)) (c/config! :border (b/empty-border :top 20)))
   (map
    (fn [faq-m]
      [(gmg/migrid
        :> :a
        (label-header (str "- " (:question faq-m)) 14))
       (gmg/migrid
        :> :a {:lgap 10 :bgap 5}
        (label-body (:answer faq-m)))])
    faq-list)])




;; ┌──────────────────────────┐
;; │                          │
;; │  Access Configurations   │
;; │                          │
;; └──────────────────────────┘

(defn- keys-generator
  "Example:
    (keys-generator \"abc\" \"abc\" 3308) ;; => :abc--abc--3308"
  [dbname host port]
  (keyword (string/replace (str dbname "--" host "--" port) #"\." "_")))


(defn- delete-map [config-k]
  (cm/update-in-value [:database.edn :datalist] (fn [x] (dissoc x config-k)))
  (if (:valid? (cm/store)) "yes"))

(defn- config-template
  []{:name "Template",
     :type :block,
     :display :edit,
     :value
     {:dbtype
      {:name "Connection type",
       :type :param,
       :display :none,
       :component :text,
       :value "mysql"},
      :host
      {:name "Database host",
       :doc
       "Enter jarman SQL database server. It may be IP adress, or domain name, where your server in. Not to set port in this input.",
       :type :param,
       :display :edit,
       :component :text,
       :value "127.0.0.1"},
      :port
      {:name "Port",
       :doc
       "Port of MariaDB/MySQL server. In most cases is '3306' or '3307'",
       :type :param,
       :display :edit,
       :component :text,
       :value "3306"},
      :dbname
      {:name "Database name",
       :type :param,
       :display :edit,
       :component :text,
       :value "my_database"},
      :user
      {:name "User login",
       :type :param,
       :display :edit,
       :component :text,
       :value "user"},
      :password
      {:name "User password",
       :type :param,
       :display :edit,
       :component :text,
       :value "password"}}})


(defn- err-underline [compo err?]
  ((gtool/gud compo :border-fn) (if err? (colors :red-color) (colors :blue-green-color))))

(defn- validate-fields
  "Description
    This function valid text fields from configuration.
    If sum of fields is 0 then validation is succses and next will do update-map, else validation fail and return something"
  [[v-dbtype v-host v-port v-dbname v-user v-password] config-k]
  (if (= 0 (+ (let [compo (gtool/gud v-dbtype :val-compo)]
                (if (= (c/text ) "")
                  (do (c/config! compo :border (err-underline compo true))  1)
                  (do (c/config! compo :border (err-underline compo false)) 0)))
              ;;(do (println "\nTry validate dbtype") 0)

              (let [compo (gtool/gud v-host :val-compo)]
                (if (< (count (c/text compo)) 4)
                  (do (c/config! compo :border (err-underline compo true))    1)
                  (do (c/config! compo :border (err-underline compo false))   0)))
              ;;(do (println "\nTry validate host") 0)

              (let [compo (gtool/gud v-port :val-compo)]
                (if (= (re-find #"[\d]+" (c/text compo)) nil)
                  (do (c/config! compo :border (err-underline compo true))    1)
                  (if (< (Integer. (c/text compo)) 65000)
                    (do (c/config! compo :border (err-underline compo false)) 0)
                    (do (c/config! compo :border (err-underline compo true))  1))))
              ;;(do (println "\nTry validate port") 0)

              (let [compo (gtool/gud v-dbname :val-compo)]
                (if (=  (c/text compo) "")
                  (do (c/config! compo :border (err-underline compo true))  1)
                  (do (c/config! compo :border (err-underline compo false)) 0)))
              ;;(do (println "\nTry validate dbname") 0)

              (let [compo (gtool/gud v-user :val-compo)]
               (if (=  (c/text compo) "")
                 (do (c/config! compo :border (err-underline compo true))    1)
                 (do (c/config! compo :border (err-underline compo false))   0)))
              ;;(do (println "\nTry validate user") 0)

              ;; (let [compo (gtool/gud v-password :val-compo)]
              ;;  (if (=  (c/text compo) "")
              ;;    (do (c/config! compo :border (err-underline compo true)) 1)
              ;;    (do (c/config! compo :border (err-underline compo false)) 0)))
              ;; (do (println "\nTry validate passwd") 0)
              ))
    true false))

(defn- config-to-check-map
  "Description:
    Get current configuration from state and marge keys vector with state values.
    Path to state [:current-config :value key :value]
  Example:
    (config-to-check-map state! [:a :b]) => {:a 1 :b 2}"
  [state! keys-v]
  (into {} (doall (map (fn [k] {k (get-in (state!) [:current-config :value k :value])}) keys-v))))

(defn- try-connect
  [state! update-info-fn]
  (let [dbs (conn/test-connection
             (config-to-check-map state! [:dbtype :host :port :dbname :user :password]))]
    (if (empty? dbs)
      (do
        (update-info-fn (gtool/get-lang-alerts :connection-faild) (colors :red-color))
        false)
      (let [c-dbname   (get-in (state!) [:current-config :value :dbname :value])
            is-inside? (first (doall (filter #(= c-dbname (:database %)) dbs)))]
        (if is-inside?
          (do
            (update-info-fn (gtool/get-lang-alerts :success) (colors :blue-green-color))
            true)
          (do
            (update-info-fn (gtool/get-lang-alerts :Unknown-database) (colors :red-color))
            false))))))

(defn- create-config
  "Description:
    Create or save configuration for connection to database."
  [state! dispatch! config-k inputs update-info-fn] 
  (let [c-config (get-in (state!) [:current-config])
        config-k (if (= config-k :empty)
                   (keys-generator (get-in c-config [:value :dbname :value])
                                   (get-in c-config [:value :host :value])
                                   (get-in c-config [:value :port :value]))
                   config-k)]
    (if (validate-fields inputs config-k)
      (do
        (cm/assoc-in-segment [:database.edn :datalist config-k] (:current-config (state!)))  
        (if (:valid? (cm/store)) "yes"))
      (gtool/get-lang-alerts :incorrect-input-fields))))



;; ┌─────────────────────────────────────┐
;; │                                     │
;; │       Configuration form            │
;; │                                     │
;; └─────────────────────────────────────┘

(defn- config-input
  "Description:
    Return input for configuration"
  [state! dispatch! param-k editable?]
  ;;(println "\nComp param: " param-k)
  (;;apply
   gcomp/state-input-text
   {:val (str (rift (get-in (state!) [:current-config :value param-k :value]) ""))
    :func (fn [e] (dispatch! {:action :update-current-config
                              :param  param-k
                              :value  (if (= param-k :port)
                                        (Integer/parseInt (rift (c/text (c/to-widget e)) "3306"))
                                        (c/text (c/to-widget e)))}))}
   :args [:editable? editable?]))

(defn- config-label
  [title]
  (c/label :text       title
           :foreground (colors :light-grey-color)
           :font       (gtool/getFont 14)
           :border     (b/empty-border :bottom 5)))

(defn- config-compo
  "Description:
    Return component with label and input."
  [state! dispatch! param-k
   & {:keys [editable?] :or {editable? true}}]
  (let [value-component (config-input state! dispatch! param-k editable?)]
    (gmg/migrid
     :v {:args [:user-data {:val-compo value-component}]}
     [[(config-label (gtool/get-lang-header param-k))]
      [value-component]])))


(defn- db-config-fields
  "Description:
    Return mig panel with db configuration editor.
    It's vertical layout with inputs and functions to save, try connection, delete."
  [state! dispatch! config-k]
  (let [config-m (get (conn/datalist-get) config-k)
        c-config-m (if (nil? config-m) (config-template) config-m)]
    
    (dispatch! {:action :set-current-config
                :value  c-config-m})
    (let [inputs [(config-compo state! dispatch! :dbtype :editable? false)
                  (config-compo state! dispatch! :host)
                  (config-compo state! dispatch! :port)
                  (config-compo state! dispatch! :dbname)
                  (config-compo state! dispatch! :user)
                  (config-compo state! dispatch! :password)]
          
          info-lbl (c/label)
          update-info-fn (fn [txt color]
                           (c/config! info-lbl
                                      :font (gtool/getFont 14)
                                      :halign :center
                                      :text (gtool/htmling txt :center)
                                      :foreground color))

          btn-del (if (= config-k :empty) []
                      (gcomp/button-basic (gtool/get-lang-btns :remove)
                                          :onClick (fn [e]
                                                     (if (= "yes" (delete-map config-k))
                                                       (c/config! (c/to-frame e) :content (login-panel state! dispatch!)))
                                                     )))
          btn-conn (gcomp/button-basic
                    (gtool/get-lang-btns :connect)
                    :onClick (fn [e] (try-connect state! update-info-fn)))

          btn-save (gcomp/button-basic (gtool/get-lang-btns :save)
                                       :onClick (fn [e] (let [complete? (create-config state! dispatch! config-k inputs update-info-fn)]
                                                            (if (= "yes" complete?)
                                                              (c/config! (c/to-frame e)
                                                                         :content (login-panel state! dispatch!))
                                                              (update-info-fn (str complete?) (colors :red-color))))))
          
          actions (fn []
                    (gmg/migrid
                     :> :a
                     [btn-save btn-conn btn-del]))

          comps [inputs
                 (actions)
                 info-lbl]]
      comps)))



;; ┌─────────────────────────────────────┐
;; │                                     │
;; │       Configuration panel           │
;; │                                     │
;; └─────────────────────────────────────┘

(defn- configuration-panel
  "Description:
    Panel with FAQ and configuration form.
    You can change selected db connection."
  [state! dispatch! config-k]
  (let [mig-p (gmg/migrid
               :> "[:40%:40%, fill]0px[:60%:60%, fill]" :f
               [(gmg/migrid
                 :v 80 :a {:lgap "20%"}
                 (db-config-fields state! dispatch! config-k))

                (gcomp/min-scrollbox
                 (gmg/migrid
                  :v 80 {:gap [10 "10%"]}
                  [(rift (about-panel (gtool/get-lang-infos :config-panel-about)) [])
                   (rift (faq-panel   (gtool/get-lang-infos :config-panel-faq))   [])]))])
        return-btn (return-to-login state! dispatch!)
        panel (gmg/migrid
               :v :g :fgf
               [(-> (label-header (gtool/convert-txt-to-UP (gtool/get-lang-header :login-db-config-editor)) 20)
                    (c/config! :halign :center :border (b/empty-border :thickness 20)))
                mig-p
                return-btn])]
    (switch-focus state! dispatch! return-btn)
    panel))

;; ┌─────────────────────────────────────┐
;; │                                     │
;; │          Logic operation            │
;; │                                     │
;; └─────────────────────────────────────┘

(defn- check-access
  "Description:
    Check if configuration and login data are correct.
    Return map about user if loggin is ok.
    Return error message if something goes wrong."
  [state! config-k]
  (let [config (get-in (state!) [:connections config-k])
        login  (:login  (state!))
        passwd (:passwd (state!))]
    ;;(println "\nLogin:" login passwd)
    (let [login-fn (system-login/login config)] 
      (if (fn? login-fn)

        (do
          ;;(println "\nConfig ok")
          (let [user-m (try
                         (login-fn login passwd)
                         (catch Exception e
                           (let [exc (str (.getMessage e))
                                 exc-type (keyword (string/join "-" (butlast (string/split "Unknown database 'pepeland'" #" "))))]
                             (println "gui_login.clj: Exception in check-access:\n" exc)
                             (rift (gtool/get-lang-alerts exc-type) exc))))]
            ;;(println "\nCheck login");;
            (cond
              (map? user-m) user-m
              (string? user-m) user-m
              :else (gtool/get-lang-alerts :incorrect-login-or-pass))))

        (case login-fn
          :no-connection-to-database
          (gtool/get-lang-alerts :connection-problem)
          :not-valid-connection
          (gtool/get-lang-alerts :configuration-incorrect)
          :else
          (gtool/get-lang-alerts :something-went-wrong))))))



;; ┌─────────────────────────────────────┐
;; │                                     │
;; │        Configurations tiles         │
;; │                                     │
;; └─────────────────────────────────────┘

(defn- tile-label [txt fsize fcolor-k]
  (c/label
   :text       txt
   :font       (gtool/getFont fsize)
   :foreground (colors fcolor-k)
   :border     (b/empty-border :top 5 :left 5 :bottom 5)))

(defn- tile-db-name [dbname log]
  (tile-label dbname 15 (if log :red-color :blue-color)))

(defn- tile-host [host log]
  (tile-label host 12 (if log :red-color :light-grey-color)))

(defn- tile-error [log]
  (if (empty? log)
    (c/label)
    (tile-label log 12 :red-color)))

(defn- tail-border
  "Description:
    Underline border for tile."
  [state! config-k]
  (fn [on]
    (let [err? (rift (get-in (state!) [:data-log config-k]) nil)]
      (b/line-border
       :bottom 4
       :color  (cond
                 on    (if err?
                         (colors :light-red-color)
                         (colors :light-blue-color))
                 err?  (colors :red-color)
                 :else (colors :light-grey-color))))))

(defn- try-to-login
  "Description:
    Check access by config, login and password.
    Return error to state or close login and run jarman."
  [state! dispatch! frame config-k]
  (let [data-log (check-access state! config-k)]
    ;;(println "\nData log\n" data-log)
    (if-not (= :empty config-k)
      (if (map? (rift data-log nil))
        (do ;; close login panel and run jarman
          (if (fn? (state/state :startup))
            (do (.dispose frame)
                ((state/state :startup)))
            (c/alert (gtool/get-lang-alerts :app-startup-fail))))
        (do ;; set data info about error to state
          (dispatch! {:action :update-data-log
                      :path   [config-k]
                      :value  (name data-log)}))))))

(defn- tail-vpanel-template
  "Description:
    Panel for tails with configuration and add configuration button."
  [state! dispatch! items-list config-k]
  (let [border-fn (tail-border state! config-k)
        
        items  (if (sequential? items-list) items-list [items-list])
        
        vpanel (c/vertical-panel
                :border         (border-fn false)
                :maximum-size   [120 :by 120]
                :preferred-size [120 :by 120]
                :background     "#fff"
                :focusable?     true
                :items          items)

        onClick  (if (= :empty config-k)
                   (fn [e] (c/config! (c/to-frame e) :content (configuration-panel state! dispatch! :empty)))
                   (fn [e] (try-to-login state! dispatch! (c/to-frame e) config-k)))
        icons    (if (> (count items) 1) [(last items)] nil)
        items    (if (> (count items) 1) (butlast items) items)
        data-log (get-in (state!) [:data-log config-k])

        listens [:mouse-entered (fn [e]
                                  (gtool/hand-hover-on vpanel)
                                  (c/config! vpanel :border (border-fn true))
                                  (if icons (c/config! (first icons) :visible? true)))
                 :mouse-exited  (fn [e]
                                  (c/config! vpanel :border (border-fn false))
                                  (if (and icons (empty? data-log)) (c/config! (first icons) :visible? false)))
                 :focus-gained  (fn [e]
                                  (c/config! vpanel :border (border-fn true))
                                  (if icons (c/config! (first icons) :visible? true)))
                 :focus-lost    (fn [e]
                                  (c/config! vpanel :border (border-fn false)))]
        
        actions [:mouse-clicked onClick
                 :key-pressed   (fn [e] (if (= (.getKeyCode e) java.awt.event.KeyEvent/VK_ENTER) (onClick e)))]]

    ;; All interactive panel
    (c/config! vpanel :listen (concat listens actions))           ;; set all listeners to tail
    (gtool/repeat-listener-to-all items (concat listens actions)) ;; set all listeners to tail
    (gtool/repeat-listener-to-all icons listens) ;; set listeners for decoration on icons and icons panel
    (c/config! (first icons) :listen actions)    ;; set all listeners on icons panel

    vpanel))


(defn- icon-template
  [ico isize onClick]
  (c/label ;; configuration panel
   :icon (stool/image-scale ico isize)
   :border (b/empty-border :thicness 5)
   :focusable? true
   :listen [:mouse-entered gtool/hand-hover-on
            :focus-gained (fn [e] (c/config! e :border (b/compound-border
                                                        (b/empty-border :bottom 3)
                                                        (b/line-border :bottom 3
                                                                       :color (gtool/get-color :decorate :focus-gained))
                                                        (b/empty-border :thicness 5))))
            :focus-lost (fn [e] (c/config! e :border (b/empty-border :thicness 5)))
            :mouse-clicked onClick
            :key-pressed   (fn [e] (if (= (.getKeyCode e) java.awt.event.KeyEvent/VK_ENTER) (onClick e)))]))

(defn- tile-icons
  "Description:
    Tools icons for tiles with access configs.
    Icons are invokers for new view like configuration manager or error info panel."
  [state! dispatch! config-k log]
  (let [isize 32
        show (if log true false)]
    (gmg/migrid
     :v :right :bottom
     {:gap [5 5 5 5] :args [:background "#fff" :visible? show]}
     [(if log
        (icon-template (fn [e] (gs/icon GoogleMaterialDesignIcons/INFO face/c-icon) (c/alert (str log))))
        [])
      (icon-template (gs/icon GoogleMaterialDesignIcons/SETTINGS face/c-icon) isize
                          (fn [e] (c/config!
                                   (c/to-frame e)
                                   :content (configuration-panel state! dispatch! config-k))))])))

(defn- config-tile
  "Description:
    State component.
    It is one tile with configuration to db.
    Config will be get from state by id-k from :connections.
    Component watching state with tiles list and will rerender self items if something will be changed.
  Example:
    (config-tile state! dispatch! :jarman--localhost--3306)"
  [state! dispatch! config-k]
  (let [render-fn (fn []
                    (let [config (get-in (state!) [:connections config-k])
                          log    (get-in (state!) [:data-log config-k])
                          dbname (:dbname config)
                          host   (:host   config)]
                      (list
                       (tile-db-name dbname log)
                       (tile-host    host   log)
                       (tile-error   log)
                       (tile-icons   state! dispatch! config-k log))))
        panel (tail-vpanel-template state! dispatch! (render-fn) config-k)]
    panel))

(defn- tiles-list-with-confs
  "Description:
    Return list of tiles with access to db configurations.
  Example:
    (config-tiles-list state! dispatch!)"
  [state! dispatch!]
  (doall
   (map
    (fn [config-k]
      (config-tile state! dispatch! config-k))
    (keys (:connections (state!))))))


(defn- tile-add-new-config 
  "Description:
    Component tail for add new configuration."
  [state! dispatch!]
  (tail-vpanel-template
     state!
     dispatch!
     (gmg/hmig
      :wrap 1
      :args [:background "#fff"]
      :items [[(c/label
                :icon (gs/icon GoogleMaterialDesignIcons/STORAGE face/c-icon)
                :halign :center)]])
     :empty))

(defn- all-tails-configs-panel
  "Description:
    State component. Return panel with tiles.
    Tiles have configurations to db connection.
    Panel watching state and will renderering items if some will be change in state.
  Example:
    (all-tails-configs-panel state! dispatch!)"
  [state! dispatch!]
  (let [render-fn (fn []
                    (gtool/join-mig-items
                     (rift (tiles-list-with-confs state! dispatch!) [])
                     (rift (tile-add-new-config   state! dispatch!) [])))
        mig (gmg/hmig
             :wrap 4
             :gap [10 10 10 10]
             :items (render-fn))
        scr (c/scrollable mig :maximum-size [600 :by 190])]
    (.setBorder scr nil)
    (.setPreferredSize (.getVerticalScrollBar scr) (Dimension. 0 0))
    (.setUnitIncrement (.getVerticalScrollBar scr) 20)
    
    (set-state-watcher state! dispatch! mig render-fn [:connections] :connections)
    (set-state-watcher state! dispatch! mig render-fn [:data-log] :connections-data-log)
    scr))



;; ┌─────────────────────────────────────┐
;; │                                     │
;; │            Info panel               │
;; │                                     │
;; └─────────────────────────────────────┘

(defn- contact-info
  [] 
  (gmg/migrid
   :v :a
   [(-> (label-header (gtool/get-lang-header :contact)) (c/config! :border (b/empty-border :top 20)))
    (gmg/migrid
     :v :a {:gap [10]}
     (doall (map #(label-body %) (gtool/get-lang-infos :contact))))]))


(defn- info-logo
  "Description:
     Generate label with logo icon."
  []
  (c/label 
   :halign :center
   :icon   (stool/image-scale "icons/imgs/trashpanda2-stars-blue-1024.png" 47)))


;;;;;;;;;;;;;;;
;;
;; Info view
;;

(defn- info-panel
  "Description:
     Return panel with info content.
   Example:
     (info-panel state! dispatch!)"
  [state! dispatch!]
  (gmg/migrid :v :a "[grow, fill]0px[fill]"
   [(gcomp/min-scrollbox
     (gmg/migrid
      :v 70 :fg
      {:gap [10 "15%"]}
      [(gmg/migrid
        :v {:gap [30]}
        (info-logo))
        (rift (about-panel (gtool/get-lang-infos :info-panel-about)) [])
        (rift (faq-panel   (gtool/get-lang-infos :info-panel-faq))  [])
        (rift (contact-info) [])
       ]))
    (let [return-btn (return-to-login state! dispatch!)]
      (switch-focus state! dispatch! return-btn)
      return-btn)]))


;; ┌─────────────────────────────────────┐
;; │                                     │
;; │           Login panel               │
;; │                                     │
;; └─────────────────────────────────────┘

(defn- login-input 
  "Description:
     Input for login. Using state. Need dispatch! fn.
  Example:
     (login-input dispatch!)"
  [dispatch!]
  (gcomp/state-input-text
   {:val ""
    :func (fn [e] (dispatch!
                   {:action :update-login
                    :value  (:value (c/config e :user-data))}))}
   :placeholder (gtool/get-lang-btns :login)
   :args [:halign :left]))

(defn- passwd-input
  "Description:
     Input for password. Using state. Need dispatch! fn.
  Example:
     (passwd-input dispatch!)"
  [dispatch!]
  (gcomp/state-input-password
   {:dispatch! dispatch!
    :action :update-pass}
   :placeholder (gtool/get-lang-btns :password)
   :style [:halign :left]))

(defn- login-icons
  "Description:
    Bottom bar with icons whos invoking info panel, exit apa, etc"
  [state! dispatch!]
  (gmg/migrid :> :right {:gap [10 20]}
                [(icon-template (gs/icon GoogleMaterialDesignIcons/INFO face/c-icon) 40 (fn [e] (c/config! (c/to-frame e) :content (info-panel state! dispatch!))))
                 (icon-template (gs/icon GoogleMaterialDesignIcons/EXIT_TO_APP face/c-icon) 40 (fn [e] (.dispose (c/to-frame e))))]))

(defn- login-inputs
  "Description:
    Login and password inputs with state logic."
  [state! dispatch!]
  (gmg/migrid
   :v :f :f;; Login inputs
   {:hpos :center}
   [(gmg/migrid
     :> "[fill]10px[200:, fill]" :f
     {:tgap 5}
     [(c/label :icon (stool/image-scale (gs/icon GoogleMaterialDesignIcons/PERSON face/c-icon) 40))
      (let [nf (login-input dispatch!)]
        (new-focus dispatch! nf) nf)])
    (gmg/migrid
     :> "[fill]10px[200:, fill]" :f
     {:gap [10 15 0 0]}
     [(c/label :icon (stool/image-scale (gs/icon GoogleMaterialDesignIcons/VPN_KEY face/c-icon) 40))
      (passwd-input dispatch!)])]))


(defn login-panel 
  "Description:
     Build and return to frame form for login panel.
  Example:
     (login-panel state! dispatch!)"
  [state! dispatch!]
  
  (load-connection-configs dispatch!)
  (dispatch! {:action :clear-data-log})

  
  
  (let [panel (gmg/migrid :v :g :gf
                            [(gmg/migrid
                              :v :center ;; Main content
                              [(c/label  ;; Jarman logo
                                :icon (stool/image-scale "icons/imgs/jarman-text.png" 6)
                                :border (b/empty-border :thickness 20))
                               (login-inputs state! dispatch!)
                               (all-tails-configs-panel state! dispatch!)])

                             (login-icons state! dispatch!)])]
    (switch-focus state!)
    panel))


;; ┌─────────────────────────────────────┐
;; │                                     │
;; │          Building lvl               │
;; │                                     │
;; └─────────────────────────────────────┘

(defn- frame-login [state! dispatch!]
  (c/frame :title "Jarman-login"
         :undecorated? false
         :resizable? false
         :minimum-size [800 :by 620]
         :icon (stool/image-scale
                icon/calendar1-64-png) 
         :content (login-panel state! dispatch!)))

(defn- st []
  (let [res-validation nil ;;(validation)
        state  (create-state-template)
        state! (fn [& prop]
                 (cond (= :atom (first prop)) state
                       :else (deref state)))
        dispatch! (create-disptcher state)]

    (cm/swapp)
    
    (if (= res-validation nil)
      (-> (doto (frame-login state! dispatch!) (.setLocationRelativeTo nil)) seesaw.core/pack! seesaw.core/show!))))

(state/set-state :invoke-login-panel st)
(st)


(comment
  Start app window
  (-> (doto (seesaw.core/frame
             :title "DEBUG WINDOW" :undecorated? false
             :minimum-size [200 :by 200]
             :size [200 :by 200]
             :content (seesaw.mig/mig-panel :constraints ["wrap 1" "0px[]0px" "0px[]0px"]
                                            :items [[(seesaw.core/label :text "a")]]))
        (.setLocationRelativeTo nil) seesaw.core/pack! seesaw.core/show!)))
