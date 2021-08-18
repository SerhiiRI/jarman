(ns jarman.gui.gui-login
  (:use seesaw.border
        seesaw.dev
        seesaw.style
        seesaw.mig
        seesaw.font)
  (:import (java.awt Dimension))
  (:require [seesaw.core               :as c]
            [seesaw.border             :as b]
            [clojure.string            :as string]
            [jarman.gui.gui-tools      :as gtool]
            [jarman.config.config-manager     :as cm]
            [jarman.resource-lib.icon-library :as icon]
            [clojure.java.jdbc         :as jdbc]
            [jarman.tools.swing        :as stool]
            [jarman.gui.gui-app        :as app]
            [jarman.logic.system-login :as system-login]
            [jarman.gui.gui-components :as gcomp]
            [jarman.logic.connection   :as conn]
            [jarman.logic.view-manager :as pvm]
            [jarman.logic.state        :as state]
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
             (catch Exception e (println "\n" (str "Rerender exception:\n" (.getMessage e))) ;; If exeption is nil object then is some prolem with nechw component inserting
                    ))))))))


;; (def state     (atom {}))


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
    :update-current-config (assoc-in state [:current-config :value (:path action-m) :value] (:value action-m))
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
         :validated-inputs {}}))


(defn- load-connection-configs [dispatch!]
  (dispatch! {:action :load-connections-configs
              :value  (conn/datalist-mapper (conn/datalist-get))}))


(def start (atom nil))
;; ┌───────────────┐
;; │               │
;; │     Body      │
;; │               │
;; └───────────────┘
;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; style color and font ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;
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


;; ┌───────────────┐
;; │               │
;; │  Local comps  │
;; │               │
;; └───────────────┘

(declare faq-panel)
(declare info-panel)
(declare login-panel)
(declare contact-info)

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
    :font       (gtool/getFont 14 :bold)
    :border     (b/empty-border :left 10 :top 5)}))


(defn- return-to-login
  "Description:
     Generate button return to login panel."
  [state! dispatch!]
  (gcomp/button-basic
   "Back to login"
   :onClick (fn [e] (c/config!
                     (c/to-frame e)
                     :content (login-panel state! dispatch!)))
   :flip-border true
   :mouse-out "#eee"
   :tgap 6
   :bgap 6
   :args [:icon (stool/image-scale icon/user-blue1-64-png 30)]))


(defn- about-panel
  "Description:
     Prepare info components"
  [info-list]
  (gcomp/migrid :v
   (doall
    (map
     (fn [[header body]]
       (gcomp/migrid :v
                     [(label-header header)
                      (label-body body)]))
     info-list))))


(defn- faq-panel
  "Description:
     Return vertical mig with FAQs."
  [faq-list]
  (gcomp/vmig
   :tgap 10
   :hrules "[100%, fill]"
   :items (gtool/join-mig-items
           (label-header "FAQ")
           (doall
            (map
             (fn [faq-m]
               (gcomp/vmig
                :hrules "[100%, fill]"
                :items (gtool/join-mig-items
                        (label-header (str "- " (:question faq-m)) 14)
                        (label-body (:answer faq-m)))))
             faq-list)))))

(defn- info-section
  "Description:
     Generate mig with info items."
  [mig-comps]
  (let [info
        (gcomp/vmig
         :hrules "[fill]"
         :items  mig-comps)]
    info))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; config-generator-JDBC + panel for config ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;



(def template-map  {:name "Raspberry",
                    :type :block,
                    :display :edit,
                    :value
                    {:dbtype
                     {:name "Typ połączenia",
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
                      :value "trashpanda-team.ddns.net"},
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
                      :value "jarman"},
                     :user
                     {:name "User login",
                      :type :param,
                      :display :edit,
                      :component :text,
                      :value "jarman"},
                     :password
                     {:name "User password",
                      :type :param,
                      :display :edit,
                      :component :text,
                      :value "dupa"}}})

(defn update-map [dbtype host port dbname user password config-k]
  (do
    (cm/assoc-in-value [:database.edn :datalist config-k :dbtype]   dbtype)
    (cm/assoc-in-value [:database.edn :datalist config-k :host]     host)
    (cm/assoc-in-value [:database.edn :datalist config-k :port]     (Integer. port))
    (cm/assoc-in-value [:database.edn :datalist config-k :dbname]   dbname)
    (cm/assoc-in-value [:database.edn :datalist config-k :user]     user)
    (cm/assoc-in-value [:database.edn :datalist config-k :password] password))
  (if (:valid? (cm/store)) "yes"))

(defn keys-generator [dbname title port]
  (keyword (string/replace (str dbname "--" title "--" port) #"\." "_")))

(defn create-map [dbtype host port dbname user password]
  (cm/assoc-in-segment [:database.edn :datalist (keys-generator dbname host port)]
                    (-> template-map
                               (assoc-in  [:value :dbtype :value] dbtype)
                               (assoc-in  [:value :host :value] host)
                               (assoc-in  [:value :port :value] (Integer. port))
                               (assoc-in  [:value :dbname :value] dbname)
                               (assoc-in  [:value :user :value] user)
                               (assoc-in  [:value :password :value] password)))  
  (if (:valid? (cm/store)) "yes"))

;; (store)
;; (swapp)


(defn delete-map [config-k]
  (cm/update-in-value [:database.edn :datalist] (fn [x] (dissoc x config-k)))
  (if (:valid? (cm/store)) "yes"))

(defn- err-underline [err?]
  (b/line-border :bottom 1 :color (if err? (colors :red-color) (colors :blue-green-color))))

(defn validate-fields
  "Description
    This function valid text fields from configuration.
    If sum of fields is 0 then validation is succses and next will do update-map, else validation fail and return something"
  [[v-dbtype v-host v-port v-dbname v-user v-password] config-k]
  (if (= 0 (+ (if (= (c/text v-dbtype) "")
                (do (c/config! v-dbtype :border (err-underline true))  1)
                (do (c/config! v-dbtype :border (err-underline false)) 0))

              (if (< (count (c/text v-host)) 4)
                (do (c/config! v-host :border (err-underline true))    1)
                (do (c/config! v-host :border (err-underline false))   0))

              (if (= (re-find #"[\d]+" (c/text v-port)) nil)
                (do (c/config! v-port :border (err-underline true))    1)
                (if (< (Integer. (c/text v-port)) 65000)
                  (do (c/config! v-port :border (err-underline false)) 0)
                  (do (c/config! v-port :border (err-underline true))  1)))
              
              (if (=  (c/text v-dbname) "")
                (do (c/config! v-dbname :border (err-underline true))  1)
                (do (c/config! v-dbname :border (err-underline false)) 0))
              
              (if (=  (c/text v-user) "")
                (do (c/config! v-user :border (err-underline true))    1)
                (do (c/config! v-user :border (err-underline false))   0))
              
              (if (=  (c/text v-password) "")
                (do (c/config! v-password :border (err-underline true)) 1)
                (do (c/config! v-password :border (err-underline false)) 0))))           
    (do 
      (if (= config-k :empty)
        (create-map
         (c/text v-dbtype)
         (c/text v-host)
         (c/text v-port)
         (c/text v-dbname)
         (c/text v-user)
         (c/text v-password))
        (update-map
         (c/text v-dbtype)
         (c/text v-host)
         (c/text v-port)
         (c/text v-dbname)
         (c/text v-user)
         (c/text v-password)
         config-k)))))



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
   {:val (str (rift (get-in (:current-config (state!)) [:value param-k :value]) ""))
    :func (fn [e] (dispatch! {:action :update-current-config
                              :path   [param-k]
                              :value  (c/text (c/to-widget e))}))}
   :start-underline (let [valid? (get-in (state!) [:validated-inputs param-k])]
                      (cond 
                        (= valid? true) (colors :blue-green-color)
                        (= valid? false)(colors :red-color)
                        :else nil))
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
  (gcomp/vmig
   :vrules "[fill]"
   :items [[(config-label (get-in (state!) [:current-config :value param-k :name]))]
           [(config-input state! dispatch! param-k editable?)]]))


;;(@start)

(defn- config-to-check-map
  "Description:
    Get current configuration from state and marge keys vector with state values.
    Path to state [:current-config :value key :value]
  Example:
    (config-to-check-map state! [:a :b]) => {:a 1 :b 2}"
  [state! keys-v]
  (into {} (doall (map (fn [k] {k (get-in (state!) [:current-config :value k :value])}) keys-v))))

(defn- db-config-fields
  "Description:
    Return mig panel with db configuration editor.
    It's vertical layout with inputs and functions to save, try connection, delete."
  [state! dispatch! config-k]
  (let [config-m (get (conn/datalist-get) config-k)]
    (if (nil? config-m)
      (do (println "[ Warning ] (conn/datalist-get) return nil in gui_login/config-generator-fields.") (c/label))
      (do
        (dispatch! {:action :set-current-config
                    :value  config-m})
        (let [inputs [(config-compo state! dispatch! :dbtype :editable? false)
                      (config-compo state! dispatch! :host)
                      (config-compo state! dispatch! :port)
                      (config-compo state! dispatch! :dbname)
                      (config-compo state! dispatch! :user)
                      (config-compo state! dispatch! :password)]
            
             info-lbl (c/label :text "Check connection." :halign :center :foreground (colors :blue-green-color) :font (gtool/getFont 14))

             btn-del (if (= config-k :empty) []
                         (gcomp/button-basic (gtool/get-lang-btns :remove)
                                             :onClick (fn [e] ;; (if (= "yes" (delete-map config-k))
                                                        ;;   (c/config! (c/to-frame e) :content (login-panel state! dispatch!)))
                                                        )))

              btn-conn (gcomp/button-basic
                        (gtool/get-lang-btns :connect)
                        :onClick (fn [e]
                                   (if (= nil (conn/test-connection
                                               (config-to-check-map state! [:dbtype :host :port :dbname :user :password])))
                                     (c/config! info-lbl
                                                :text (gtool/get-lang-alerts :connection-faild)
                                                :foreground (colors :red-color))
                                     (c/config! info-lbl
                                                :text (gtool/get-lang-alerts :success)
                                                :foreground (colors :blue-green-color)))))

             btn-save (gcomp/button-basic (gtool/get-lang-btns :save)
                                          :onClick (fn [e] ))
            
             actions (fn []
                       (gcomp/hmig
                        :vrules "[fill]"
                        :items (gtool/join-mig-items btn-save btn-conn btn-del)))

             mig (gcomp/vmig
                  :lgap "25%"
                  :items (gtool/join-mig-items
                          ;;(label-header (gtool/get-lang-basic :db-conn-config) 18)
                          inputs
                          (actions)
                          info-lbl))]
         mig)))))



;; ┌─────────────────────────────────────┐
;; │                                     │
;; │       Configuration panel           │
;; │                                     │
;; └─────────────────────────────────────┘


;;;;;;;;;;;;;;;
;;
;; Content
;;

(defn config-faq-list
  "Description:
     Load Answer and Question"
  []
  (list
   {:question "Why i can not get connection with server?"
    :answer   "Please check that you have entered the data correctly"}
   {:question "Where can i find data to connect?"
    :answer   "Please contact with your system administrator"}))

(defn config-info-list
  "Description:
     List with info for client.
     To list set vector with header and content.
  Example:
     [\"Header\" \"Content]"
  []
  (list
   ["About"   "We are Trashpanda-Team, we are the innovation."]
   ["Jarman"  "Jarman is an flexible aplication using plugins to extending basic functionality."]
   ["Contact" "For contact with us summon the demon and give him happy pepe. Then demon will be kind and will send u to us."]
   ["Website" "http://trashpanda-team.ddns.net"]))


(defn configuration-panel
  "Description:
    Panel with FAQ and configuration form.
    You can change selected db connection."
  [state! dispatch! config-k]
  (let [faq (config-faq-list)
        mig-p (gcomp/migrid :> [(db-config-fields state! dispatch! config-k)
                          (let [scr (c/scrollable
                                     (info-section (gtool/join-mig-items
                                                    (about-panel (config-info-list))
                                                    (faq-panel   (config-faq-list))))
                                     :hscroll :never :border nil)]
                            (.setPreferredSize (.getVerticalScrollBar scr) (Dimension. 0 0))
                            (.setUnitIncrement (.getVerticalScrollBar scr) 20) scr)])]
    
    (gcomp/migrid :v [(-> (label-header (gtool/convert-txt-to-UP (gtool/get-lang-header :login-db-config-editor)) 20)
                    (c/config! :halign :center :border (b/empty-border :thickness 20)))
                mig-p
                (return-to-login state! dispatch!)]
            :vtemplate :fgf)))

(defn migrid-demo
  "Description:
    Panel with FAQ and configuration form.
    You can change selected db connection."
  [state! dispatch!]
  (let [ mig-p (gcomp/migrid :>
               [(gcomp/migrid :> (c/label "LOL LEFT"))
                (gcomp/migrid :v [(gcomp/migrid :> [(gcomp/migrid :> (label-header "For contact with us summon the demon and give him happy pepe. Then demon will be kind and will...")
                                                :vpos :top
                                                :args [:border (b/line-border :left 1 :bottom 1 :color "#fff")])
                                        (gcomp/migrid :> (label-header "For contact with us summon the demon and give him happy pepe. Then demon will be kind and will...")
                                                :args [:border (b/line-border :left 1 :bottom 1 :color "#fff")])
                                        (gcomp/migrid :> (label-header "For contact with us summon the demon and give him happy pepe. Then demon will be kind and will...")
                                                :vpos :bottom
                                                :args [:border (b/line-border :left 1 :bottom 1 :color "#fff")])])
                            (gcomp/migrid :> (c/label :text "LOL CENTER")
                                    :hpos :center
                                    :args [:border (b/line-border :left 1 :bottom 1 :color "#fff")])])
                (gcomp/migrid :> (c/label "LOL RIGHT")
                        :hpos :right
                        :args [:border (b/line-border :left 1 :bottom 1 :color "#fff")])])]
    
    (gcomp/migrid :v [(gtool/join-mig-items
                 (-> (label-header "GCOMP/MIGRID DEMO" 20)
                     (c/config! :halign :center :border (b/empty-border :thickness 20)))
                 mig-p
                 (return-to-login state! dispatch!))]
            :vtemplate :fgf)))

;; ┌─────────────────────────────────────┐
;; │                                     │
;; │          Logic operation            │
;; │                                     │
;; └─────────────────────────────────────┘
;;(@start)

(defn check-access
  "Description:
    Check if configuration and login data are correct.
    Return map about user if loggin is ok.
    Return error message if something goes wrong."
  [state! config-k]
  (let [config (get-in (state!) [:connections config-k])
        login  (:login  (state!))
        passwd (:passwd (state!))]
    (println "\nLogin:" login passwd)
    (let [login-fn (system-login/login config)] 
      (if (fn? login-fn)

        (do
          (println "\nConfig ok")
          (let [user-m (login-fn login passwd)]
            (println "\nCheck login")
           (if (map? user-m)
             user-m
             "Login or password is incorrect.")))

        (case login-fn
          :no-connection-to-database
          "Cannot connect to database. Problem with connection."
          :not-valid-connection
          "Configuration is incorrect."
          :else
          "Something goes wrong.")))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; panels for login and error ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; (defn- error-panel [errors]
;;   (let [scr (c/scrollable
;;              (mig-panel
;;               :constraints ["wrap 1" "[grow, center]" "20px[]20px"]
;;               :items [[(c/label
;;                         :icon   (stool/image-scale icon/alert-red-512-png 8)
;;                         :border (b/empty-border :right 10)) "split 2"]
;;                       [(c/label
;;                         :text       (gtool/htmling "<h2>ERROR</h2>")
;;                         :foreground (colors :blue-green-color)
;;                         :font       (gtool/getFont 14))]
;;                       [(let [mig (mig-panel
;;                                   :constraints ["wrap 1" "40px[:600, grow, center]40px" "10px[]10px"]
;;                                   :items [[(c/label
;;                                             :text (gtool/htmling "<h2>About</h2>")
;;                                             :foreground (colors :blue-green-color)
;;                                             :font (gtool/getFont 14)) "align l"]
;;                                           [(c/label
;;                                             :text (gtool/htmling (str "<p align= \"justify\">" (:description errors) "</p>"))
;;                                             :foreground (colors :light-grey-color)
;;                                             :font (gtool/getFont 14)) "align l"]])]               
;;                          (doall
;;                           (map
;;                            (fn [x]
;;                              (.add mig x "align l"))
;;                            (if (= (:faq errors) nil)
;;                              (contact-info)
;;                              (concat ((faq-panel) (:faq errors)) contact-info))))
;;                          (.repaint mig) mig)]])
;;              :hscroll :never)]
;;     (.setUnitIncrement (.getVerticalScrollBar scr) 20)
;;     scr))

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

 ;;(@start)

(defn- tail-border
  "Description:
    Underline border for tile."
  [state! config-k]
  (fn [on]
    (let [err? (rift (get-in (state!) [:data-log config-k]) nil)]
      (line-border
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
    (println "\nData log\n" data-log)
    (if-not (= :none config-k)
      (if (map? (rift data-log nil))
        (do ;; close login panel and run jarman
          (.dispose frame)
          ((state/state :startup)))
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

        onClick  (fn [e] (try-to-login state! dispatch! (c/to-frame e) config-k))
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
                 ;; TODO: choose info or settings by keyboard
                 :focus-gained  (fn [e] (c/config! vpanel :border (border-fn true)))
                 :focus-lost    (fn [e] (c/config! vpanel :border (border-fn false)))]
        
        actions [:mouse-clicked onClick
                 :key-pressed   (fn [e] (if (= (.getKeyCode e) java.awt.event.KeyEvent/VK_ENTER) (onClick e)))]]

    ;; All interactive panel
    (c/config! vpanel :listen (concat listens actions))           ;; set all listeners to tail
    (gtool/repeat-listener-to-all items (concat listens actions)) ;; set all listeners to tail
    (gtool/repeat-listener-to-all icons listens) ;; set listeners for decoration on icons and icons panel
    (c/config! (first icons) :listen actions)    ;; set all listeners on icons panel

    vpanel))

;; (@start)

(defn- tile-icons
  "Description:
    Tools icons for tiles with access configs.
    Icons are invokers for new view like configuration manager or error info panel."
  [state! dispatch! config-k log]
  (let [isize 32
        show (if log true false)]
    (gcomp/hmig
     :args (concat [:background "#fff" :visible? show])
     :hrules "[grow]5px[fill]"
     :vrules "[grow, bottom]"
     :gap [5 5 5 5]
     :items (gtool/join-mig-items
             (c/label)
             (if log
               (c/label ;; Info about error
                :icon (stool/image-scale icon/I-grey-64-png isize)
                :border (empty-border :thicness 5)
                :listen [:mouse-clicked (fn [e] (println "\nInfo about error in progress"))])
               [])
             (c/label ;; configuration panel
              :icon (stool/image-scale icon/settings-64-png isize)
              :border (empty-border :thicness 5)
              :listen [:mouse-clicked (fn [e] (c/config!
                                               (c/to-frame e)
                                               :content (configuration-panel state!
                                                                             dispatch!
                                                                             config-k)))])))))

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

;;(@start)

(defn- tiles-list-with-confs
  "Description:
    Return list of tiles with access to db configurations.
  Example:
    (config-tiles-list state! dispatch!)"
  [state! dispatch!]
  (doall
   (map
    (fn [id-k]
      (config-tile state! dispatch! id-k))
    (keys (:connections (state!))))))


(defn- tile-add-new-config
  "Description:
    Component tail for add new configuration."
  [state! dispatch!]
  (tail-vpanel-template
   state!
   dispatch!
   (gcomp/vmig
    :args [:background "#fff"]
    :items [[(c/label
              :icon (stool/image-scale icon/pen-128-png 34)
              :halign :center
              :listen [:mouse-clicked (fn [e]
                                        (c/config! (c/to-frame e)
                                                   :content (configuration-panel
                                                             state! dispatch! :empty)))])]])
   :none))

;;(@start)

(defn- state-access-configs
  "Description:
    State component. Return panel with tiles.
    Tiles have configurations to db connection.
    Panel watching state and will renderering items if some will be change in state.
  Example:
    (state-access-configs state! dispatch!)"
  [state! dispatch!]
  (let [render-fn (fn []
                    (gtool/join-mig-items
                     (rift (tiles-list-with-confs state! dispatch!) [])
                     (rift (tile-add-new-config   state! dispatch!) [])))
        mig (gcomp/vmig
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

;;;;;;;;;;;;;;;
;;
;; Content
;;

(defn about-faq-list
  "Description:
     Load Answer and Question"
  []
  (list
   {:question "Why i can not get connection with server?"
    :answer   "Please check that you have entered the data correctly"}
   {:question "Where can i find data to connect?"
    :answer   "Please contact with your system administrator"}))

(defn about-info-list
  "Description:
     List with info for client.
     To list set vector with header and content.
  Example:
     [\"Header\" \"Content]"
  []
  (list
   ["About"   "We are Trashpanda-Team, we are the innovation."]
   ["Jarman"  "Jarman is an flexible aplication using plugins to extending basic functionality."]
   ["Contact" "For contact with us summon the demon and give him happy pepe. Then demon will be kind and will send u to us."]
   ["Website" "http://trashpanda-team.ddns.net"]))

(defn contact-list
  []
  (list
   "http://trashpanda-team.ddns.net"
   "contact.tteam@gmail.com"
   "+38 0 966 085 615"))


;;;;;;;;;;;;;;;
;;
;; Contact
;;

(defn contact-info
  [] 
  (gcomp/vmig
   :items (gtool/join-mig-items
           (label-header "Contacts")
           (doall
            (map #(label-body %) (contact-list))))))


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

(defn info-panel
  "Description:
     Return panel with info content.
   Example:
     (info-panel state! dispatch!)"
  [state! dispatch!]
  (gcomp/migrid :v
   [(gcomp/min-scrollbox
     (gcomp/migrid :v
      [(info-logo)
       (rift (about-panel (about-info-list)) [])
       ;; (rift (faq-panel   (about-faq-list))  [])
       ;; (rift (contact-info) [])
       ]))
    (return-to-login state! dispatch!)]
   :vtemplate :gf))


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

(defn login-panel 
  "Description:
     Build and return to frame form for login panel.
  Example:
     (login-panel state! dispatch!)"
  [state! dispatch!]
  
  (load-connection-configs dispatch!)
  (dispatch! {:action :clear-data-log})

  (gcomp/migrid :v
   [(gcomp/migrid :v ;; Main content
                  [(c/label ;; Jarman logo
                    :icon (stool/image-scale "icons/imgs/jarman-text.png" 6)
                    :border (b/empty-border :thickness 20))
                  
                   (gcomp/migrid :v ;; Login inputs
                                 [(gcomp/migrid :> [(c/label :icon (stool/image-scale icon/user-blue1-64-png 40))
                                                    (login-input  dispatch!)]
                                                :hrules "[fill]10px[200:, fill]"
                                                :vrules "[fill]"
                                                :tgap 5)
                                  (gcomp/migrid :> [(c/label :icon (stool/image-scale icon/key-blue-64-png 40))
                                                    (passwd-input dispatch!)]
                                                :hrules "[fill]10px[200:, fill]"
                                                :vrules "[fill]"
                                                :gap [10 15 0 0])]
                                 :hpos :center
                                 :vtemplate :f)
                  
                   (state-access-configs state! dispatch!)]
                  :hpos :center
                  :vtemplate :f)
   
    (gcomp/migrid :> ;; More info buttons
                  [(c/label :icon (stool/image-scale icon/pen-blue-64-png 40)
                            :listen [:mouse-entered gtool/hand-hover-on
                                     :mouse-clicked (fn [e]
                                                      (c/config!
                                                       (c/to-frame e)
                                                       :content (migrid-demo state! dispatch!)))])
                   (c/label :icon (stool/image-scale icon/I-64-png 40)
                            :listen [:mouse-entered gtool/hand-hover-on
                                     :mouse-clicked (fn [e]
                                                      (c/config!
                                                       (c/to-frame e)
                                                       :content (info-panel state! dispatch!)))])
                   (c/label :icon (stool/image-scale icon/enter-64-png 40)
                            :listen [:mouse-entered gtool/hand-hover-on
                                     :mouse-clicked (fn [e] (.dispose (c/to-frame e)))])]
                  :hpos :right
                  :vtemplate :f
                  :gap [10 20])]
   :vtemplate :gf))

;; ┌─────────────────────────────────────┐
;; │                                     │
;; │          Building lvl               │
;; │                                     │
;; └─────────────────────────────────────┘

(def debat (atom nil))
(def debdi (atom nil))

(defn- frame-login [state! dispatch!]
  (c/frame :title "Jarman-login"
         :undecorated? false
         :resizable? false
         :minimum-size [800 :by 620]
         :icon (stool/image-scale
                icon/calendar1-64-png) 
         :content (login-panel state! dispatch!)))

(defn- frame-error [state! dispatch!]
  (c/frame :title "Jarman-error"
         :undecorated? false
         :resizable? false
         :icon (stool/image-scale
                icon/calendar1-64-png)
         :minimum-size [600 :by 600]))

(defn st []
  (let [res-validation nil ;;(validation)
        state  (create-state-template)
        state! (fn [& prop]
                 (cond (= :atom (first prop)) state
                       :else (deref state)))
        dispatch! (create-disptcher state)]

    (reset! debdi dispatch!)
    (reset! debat state)

    (cm/swapp)

    
    (if (= res-validation nil)
      (-> (doto (frame-login state! dispatch!) (.setLocationRelativeTo nil)) seesaw.core/pack! seesaw.core/show!)
      ;; (-> (doto (frame-error state! dispatch!) (.setLocationRelativeTo nil) (apply-stylesheet my-style))
      ;;     (c/config! :content (error-panel res-validation)) seesaw.core/pack! seesaw.core/show!)
      )))

(state/set-state :invoke-login-panel st)
(reset! start st)
(@start)



;; Start app window
;; (-> (doto (seesaw.core/frame
;;            :title "DEBUG WINDOW" :undecorated? false
;;            :minimum-size [200 :by 200]
;;            :size [200 :by 200]
;;            :content (label :text "a" :border ((color-border "#222"))))
;;       (.setLocationRelativeTo nil) pack! show!))
  
