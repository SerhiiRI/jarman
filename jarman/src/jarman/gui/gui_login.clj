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
               ((:repaint (state!))))
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
    :update-login (assoc-in state (:path action-m) (:value action-m))
    :update-pass  (assoc-in state (:path action-m) (:value action-m))
    ))

(defn- create-disptcher [atom-var]
  (fn [action-m]
    (swap! atom-var (fn [state] (action-handler state action-m)))))

;;(def dispatch! (create-disptcher state))

(defn- create-state-template
  "Description:
     State for login panel.
     :login     store login
     :password  store password
     :content   avaliable content to display
     :configs   loaded all avaliable configs for connections
     :config    connection configuration when we go to editing
     :active    id of displayed content, could be info, editor, login etc
     :return-content last using content"
  []
  (atom {:login    nil
         :passwd   nil
         :configs  {}
         :config   nil
         :content  {}
         :active   :login-panel}))






;; ┌───────────────┐
;; │               │
;; │     Body      │
;; │               │
;; └───────────────┘
;;;;;;;;;;;;;;;;;;;;;;;;
;;; validation login ;;;
;;;;;;;;;;;;;;;;;;;;;;;;

(def validate3 {:validate? true})
(def validate4 {:validate? true})

(defn- validation []
  (cond 
    (not (:validate? validate3)) (:output validate3)
    (not (:validate? validate4)) (:output validate4)
    :else nil))

(defn- authenticate-user [login password]
  (if (and (= login "a")(= password "a")) true))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; style color and font ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn- colors
  "Return color by keyname"
  [k]
  (k {:dark-grey-color  "#676d71"
      :blue-color       "#256599"
      :light-grey-color "#82959f"
      :blue-green-color "#2c7375"
      :light-blue-color "#96c1ea"
      :red-color        "#f01159"
      :back-color       "#c5d3dd"}))

(def my-style {[:.css1] {:foreground (colors :blue-green-color)}})

(defn color-border [color]
  (gtool/my-border [color 2] [10 10 5 5])
)
;; (color-border "#222")



(defn myFont [size]
  {:size size :font "Arial" :style :bold})

(def ^:private emp-border
  (gtool/my-border [10 10 5 5]))

;;;;;;;;;;;;;;;;;;;;;;;
;;; some-components ;;; 
;;;;;;;;;;;;;;;;;;;;;;;

(declare faq-panel)
(declare info-panel)
(declare login-panel)
(declare contact-info)

(defn- some-text [color]
  (c/label
   :foreground color
   :font       (myFont 14)
   :text       (gtool/htmling
                "<p align= \"justify\">It is a long established fact that a reader will be distracted by the readable content of a page when lookiult model text, and a search for 'lorem ipsum' will uncover many web sites still in their infancy. Various versions have evolved over the years, sometimes by accident, sometimes on purpose (injected humour and the like)</p>")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; config-generator-JDBC + panel for config ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def confgen-header
  (fn [title] (c/label :text title
                       :font (gtool/getFont 16 :bold)
                       :foreground (colors :blue-green-color))))

(def confgen-title
  (fn [title]
    (c/label :text title
             :foreground (colors :light-grey-color)
             :font (gtool/getFont 12 :bold))))

(def confgen-input 
  (fn [data] (c/text
              :text data
              :font (gtool/getFont 14)
              :background "#fff"
              :columns 20
              :border (color-border (colors :light-grey-color)))))

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

(defn update-map [dbtype host port dbname user password key-title]
  (do
    (cm/assoc-in-value [:database.edn :datalist key-title :dbtype] dbtype)
    (cm/assoc-in-value [:database.edn :datalist key-title :host] host)
    (cm/assoc-in-value [:database.edn :datalist key-title :port] (Integer. port))
    (cm/assoc-in-value [:database.edn :datalist key-title :dbname] dbname)
    (cm/assoc-in-value [:database.edn :datalist key-title :user] user)
    (cm/assoc-in-value [:database.edn :datalist key-title :password] password))
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


(defn delete-map [key-title]
  (cm/update-in-value [:database.edn :datalist] (fn [x] (dissoc x key-title)))
  (if (:valid? (cm/store)) "yes"))

(defn validate-fields
  "Description
    this function vaalidate text fields from config-generator-panel
    if sum of fields 0 -> validation succses -> update-map, else validation fail"
  [v-dbtype v-host v-port v-dbname v-user v-password key-title]
  (if (= 0 (+ (if (= (c/text v-dbtype) "") (do (c/config! v-dbtype :border (color-border (colors :red-color))) 1)
                  (do (c/config! v-dbtype :border (color-border :blue-green-color)) 0))
              (if (< (count (c/text v-host)) 4) (do (c/config! v-host :border (color-border (colors :red-color))) 1)
                  (do (c/config! v-host :border (color-border :blue-green-color)) 0))
              (if (= (re-find #"[\d]+" (c/text v-port)) nil)
                (do (c/config! v-port :border (color-border (colors :red-color))) 1)
                (if (< (Integer. (c/text v-port)) 65000)
                  (do (c/config! v-port :border (color-border :blue-green-color)) 0)
                  (do (c/config! v-port :border (color-border (colors :red-color))) 1)))
              (if (=  (c/text v-dbname) "") (do (c/config! v-dbname :border (color-border (colors :red-color))) 1)
                  (do (c/config! v-dbname :border (color-border :blue-green-color)) 0))
              (if (=  (c/text v-user) "") (do (c/config! v-user :border (color-border (colors :red-color))) 1)
                  (do (c/config! v-user :border (color-border :blue-green-color)) 0))
              (if (=  (c/text v-password) "") (do (c/config! v-password :border (color-border (colors :red-color))) 1)
                  (do (c/config! v-password :border (color-border :blue-green-color)) 0))))           
    (do 
      (if (= key-title :empty)
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
         key-title)))))


(defn config-generator-fields [state! dispatch! key-title]
  (if (nil? (conn/datalist-get))
    (do (println "[ Warning ] (conn/datalist-get) return nil in gui_login/config-generator-fields.") (c/label))
    (let [data (key-title (conn/datalist-get))
          dbtype-inp (c/text :text "mysql" :font (gtool/getFont 14)
                           :editable? false
                           :background "#fff"
                           :columns 20
                           :border (color-border (colors :light-grey-color)))
          host-inp (confgen-input (:value (:host (:value data))))
          port-inp (confgen-input (:value (:port (:value data))))
          dbname-inp (confgen-input (:value (:dbname (:value data))))
          user-inp (confgen-input (:value (:user (:value data))))
          password-inp (confgen-input (:value (:password (:value data))))
          err-lbl (c/label :text "" :foreground (colors :blue-green-color) :font (myFont 14))
          btn-del (c/label :text "Delete" :background "#fff"
                         :class :css1
                         :border (b/empty-border :top 10 :right 10 :left 10 :bottom 10)
                         :listen [:mouse-entered (fn [e] (c/config! e :background (colors :back-color) :cursor :hand))
                                  :mouse-exited  (fn [e] (c/config! e :background "#fff"))
                                  :mouse-clicked (fn [e] (if (= "yes" (delete-map key-title))
                                                           (c/config! (c/to-frame e) :content (login-panel state! dispatch!))))])
          btn-conn (c/label :text "Connect" :background "#fff"
                          :class :css1
                          :border (b/empty-border :top 10 :right 10 :left 10 :bottom 10)
                          :listen [:mouse-entered (fn [e] (c/config! e :background (colors :back-color) :cursor :hand))
                                   :mouse-exited  (fn [e] (c/config! e :background "#fff"))
                                   :mouse-clicked (fn [e] (if (= nil (conn/test-connection {:dbtype (c/text dbtype-inp)
                                                                                         :host (c/text host-inp)
                                                                                         :port (c/text port-inp)
                                                                                         :dbname (c/text dbname-inp)
                                                                                         :user (c/text user-inp)
                                                                                         :password (c/text password-inp)}))
                                                            (c/config! err-lbl :text "ERROR!! PLEASE RECONNECT" :foreground (colors :red-color))
                                                            (c/config! err-lbl :text "SUCCESS" :foreground (colors :blue-green-color))))])
          btn-ent (c/label :text "Edit" :background "#fff"
                         :class :css1
                         :halign :center
                         :border (b/empty-border :top 10 :right 10 :left 10 :bottom 10)
                         :listen [:mouse-entered (fn [e] (c/config! e :background (colors :back-color) :cursor :hand))
                                  :mouse-exited  (fn [e] (c/config! e :background "#fff"))
                                  :mouse-clicked (fn [e] (if (= (validate-fields dbtype-inp host-inp port-inp
                                                                                 dbname-inp user-inp password-inp key-title) "yes")
                                                           (c/config! (c/to-frame e)
                                                                    :content (login-panel state! dispatch!))))])
          mig-for-btn (mig-panel :constraints ["" "0px[:33%, grow, fill]5px[:33%, grow, fill]0px" ""]
                                 :items [])
          mig (mig-panel
               :constraints ["wrap 1" "25%[grow, fill, center]25%" "20px[]20px"]
            ;; :background "#666"
               :items [[(confgen-header "Data configuration")]
                    ;; [(grid-panel :columns 1 :items [(confgen-title (:name (:dbtype (:value template-map)))) dbtype-inp])]
                       [(c/grid-panel :columns 1 :items [(confgen-title (:name (:host (:value template-map)))) host-inp])]
                       [(c/grid-panel :columns 1 :items [(confgen-title (:name (:port (:value template-map)))) port-inp])]
                       [(c/grid-panel :columns 1 :items [(confgen-title (:name (:dbname (:value template-map)))) dbname-inp])]
                       [(c/grid-panel :columns 1 :items [(confgen-title (:name (:user (:value template-map)))) user-inp])]
                       [(c/grid-panel :columns 1 :items [(confgen-title (:name (:password (:value template-map)))) password-inp])]
                       [mig-for-btn]])]
      (if (= key-title :empty)
        (do (c/config! btn-ent :text "Create")
            (.add mig-for-btn btn-conn)
            (.add mig-for-btn btn-ent)
            (.add mig-for-btn err-lbl)))
      (do
        (.add mig-for-btn btn-ent)
        (.add mig-for-btn btn-conn)
        (.add mig-for-btn btn-del)
        (.add mig err-lbl)) mig)))

(defn config-generator-panel [state! dispatch! key-title]
  (let [info (some-text)
        faq [{:q "Why i can not get connection with server?"
              :a "Please check that you have entered the data correctly"}
             {:q "Where can i find data to connect?"
              :a "Please contact with your system administrator"}]
        mig-p (mig-panel
               :constraints ["wrap 2" "0px[grow, fill, center]80px" "20px[grow, fill]20px"]
               :items [[(mig-panel  :constraints ["" "0px[10%, fill]0px[grow,center]0px[10%, fill]0px" ""]
                                    :items [[(c/label :icon (stool/image-scale icon/left-blue-64-png 50)
                                                    :listen [:mouse-entered (fn [e] (gtool/hand-hover-on e))
                                                             :mouse-exited (fn [e] (gtool/hand-hover-off e))
                                                             :mouse-clicked (fn [e] (c/config! (c/to-frame e) :content (login-panel state! dispatch!)))])]
                                            
                                            [(c/label :text "CONNECT TO DATABASE"
                                                    :foreground (colors :blue-green-color) :font (myFont 18))]
                                            [(c/label)]]) "span 2" ]
                       ;; [(label :icon (stool/image-scale icon/refresh-connection-grey1-64-png 80)
                       ;;         :border (b/empty-border :right 30))]
                       [(config-generator-fields state! dispatch! key-title)]
                       [(let [scr (c/scrollable (doto (c/border-panel
                                                       :items [[(c/vertical-panel
                                                                 :items (concat (list
                                                                                 (c/label :text       (gtool/htmling "<h2>About</h2>")
                                                                                          :foreground (colors :blue-green-color) :font (myFont 14))
                                                                                 (c/label :text       (gtool/htmling "<p align= \"justify\">This configuration page describe data binding and data environment of program. De-facto is a backend for jarman</p>")
                                                                                          :foreground (colors :light-grey-color)
                                                                                          :font       (myFont 14))
                                                                                 (c/label :text       (gtool/htmling "<p align= \"justify\">You must set right hostname(server ip adress and port), database name also application user and password</p>")
                                                                                          :border     (b/empty-border :top 10)
                                                                                          :foreground (colors :light-grey-color)
                                                                                          :font       (myFont 14))
                                                                                 (c/label :text       (gtool/htmling "<h2>FAQ</h2>")
                                                                                          :foreground (colors :blue-green-color)
                                                                                          :font       (myFont 14)))
                                                                                (flatten
                                                                                 (map
                                                                                  (fn [x]
                                                                                    [(c/label :text       (str "- " (:q x))
                                                                                              :foreground (colors :blue-green-color)
                                                                                              :font       (myFont 14)
                                                                                              :border     (b/empty-border :top 4 :bottom 4))
                                                                                     (c/label :text       (:a x)
                                                                                              :foreground (colors :light-grey-color)
                                                                                              :font       (myFont 14)
                                                                                              :border     (b/empty-border :top 4 :bottom 4))])
                                                                                  faq))))
                                                                :north]])
                                                  (.setPreferredSize (new Dimension 340 10000)))
                                                :hscroll :never :border nil)]
                          (.setPreferredSize (.getVerticalScrollBar scr) (Dimension. 0 0))
                          (.setUnitIncrement (.getVerticalScrollBar scr) 20) scr) "aligny top"]])]
    mig-p))


(defn get-values [some-key]
  (string/split (string/replace (name some-key) #"\_" ".") #"\--" ))

(defn test-key-connection [key-title]
  (let [data (key-title (conn/datalist-mapper (conn/datalist-get)))]
    (conn/test-connection {:dbtype (:dbtype data)
                        :host (:host data)
                        :port (:port data)
                        :dbname (:dbname data)
                        :user (:user data)
                        :password (:password data)})))


(defn test-key-login [key-title login pass]
  (let [data (key-title (conn/datalist-mapper (conn/datalist-get)))]
    (println login)
    (println pass)
    (if-let [login-fn (system-login/login
                       {:dbtype (:dbtype data)
                        :host (:host data)
                        :port (:port data)
                        :dbname (:dbname data)
                        :user (:user data)
                        :password (:password data)})]
      ;; function login retunr function to
      ;; make login to user
      (if (fn? login-fn)
        (if-let [u (login-fn login pass)]
          u
          "user not found")
        (case login-fn
          :no-connection-to-database
          "bad DB connection"
          :not-valid-connection
          "not valid connection")))))


(defn some-error-v [error]
  (c/vertical-panel
   :background "#fff"
   :items (list (c/label :text error :font (myFont 13) :foreground (colors :red-color)
                       :border (b/empty-border :top 5 :left 5 :bottom 5)))))


(defn label-to-config [state! dispatch! dbname title key-title login pass] 
  (let [dbn   (c/label
               :text        dbname
               :font       (myFont 15)
               :foreground (colors :blue-color)
               :border     (b/empty-border :top 5 :left 5 :bottom 5))
        hostn (c/label
               :text       title
               :font       (myFont 12)
               :foreground (colors :light-grey-color)
               :border     (b/empty-border :top 5 :left 5 :bottom 5)
               :preferred-size  [20 :by 20])
        err-l  (c/label :text "")
        v-pane (c/vertical-panel
                :background "#fff"
                :items (list dbn hostn err-l))
        icon-info (c/label :icon (stool/image-scale icon/I-grey-64-png 36)
                           :visible? true
                           :border (b/empty-border :right 5)
                           :listen [
                                    :mouse-entered (fn [e] (do (gtool/hand-hover-on e) (c/config! e :visible? true)))
                                    :mouse-exited (fn [e] (do (gtool/hand-hover-off e) (c/config! e :visible? false)))
                                    :mouse-clicked (fn [e] (do (gtool/hand-hover-off e) (c/config! (c/to-frame e) :content (info-panel state! dispatch!))))])
        icon-conf
        (c/label :icon (stool/image-scale icon/settings-64-png 36)
               :halign :right
               :visible? false
               :border (b/empty-border :right 5 )
               :listen [
                        :mouse-entered (fn [e] (do (gtool/hand-hover-on e) (c/config! e :visible? true)))
                        :mouse-exited (fn [e] (do (gtool/hand-hover-off e) (c/config! e :visible? false)))
                        :mouse-clicked (fn [e] (do (gtool/hand-hover-off e) (c/config! (c/to-frame e)
                                                                               :content (config-generator-panel state! dispatch! key-title))))])
        icons-pan (c/flow-panel :items (list icon-conf) :background "#fff" :align :right)
        my-panel (c/border-panel
                  :listen [:focus-gained (fn [e]
                                           (c/config! e :border (line-border
                                                                 :bottom 4
                                                                 :color (colors :light-blue-color))))
                           :focus-lost   (fn [e] (c/config! e :border (line-border
                                                                       :bottom 4
                                                                       :color (colors :light-grey-color))))]
                  :focusable? true
                  :border (line-border
                           :bottom 4
                           :color (colors :light-grey-color))
                  :maximum-size  [120 :by 120]
                  :preferred-size  [120 :by 120]
                  :background "#fff")]
    (do (.removeAll my-panel)
        (c/config! my-panel  :items  [[v-pane :north]
                                    [icons-pan :south]]))
    (.repaint my-panel)
    (let [onClick (fn [e] (let [data-log (test-key-login key-title (c/text login)
                                                         (get (c/config pass :user-data) :value))]
                            (if (instance? clojure.lang.PersistentArrayMap data-log)
                              (do (.revalidate my-panel)
                                  (.dispose (c/to-frame e))
                                  (state/state :startup))
                              (do
                                (c/config! dbn :foreground (colors :red-color))
                                (println (name data-log))
                                (c/config! err-l ;; (some-error-v (name data-log))
                                         :text (name data-log)
                                         :font (myFont 11) :foreground (colors :red-color)
                                         :border (b/empty-border :top 5 :left 5 :bottom 5))
                                (.add icons-pan icon-info)
                                (c/config! my-panel
                                           :border (line-border :bottom 4
                                                                :color (colors :red-color))
                                           :listen [:mouse-clicked (fn [e]
                                                                     (let [data-log (test-key-login key-title (c/text login)
                                                                                                    (get (c/config pass :user-data) :value))]
                                                                       (if (instance? clojure.lang.PersistentArrayMap data-log)
                                                                         (do (.revalidate my-panel)
                                                                             (.dispose (c/to-frame e))
                                                                             (state/state :startup)))))
                                                    :mouse-exited  (fn [e] (do
                                                                             (c/config! icon-conf :visible? false)
                                                                             (c/config! icon-info :visible? false)
                                                                             (c/config! e :border (line-border :bottom 4 :color (colors :red-color)))))
                                                    :mouse-entered  (fn [e] (do (c/config! icon-conf :visible? true)
                                                                                (c/config! icon-info :visible? true)
                                                                                (c/config! e :border (line-border :bottom 4 :color (colors :red-color)))))])
                                (.revalidate my-panel)))))]
      (c/config! my-panel
                 :listen [:mouse-entered (fn [e] (do
                                                   (c/config! e :border (line-border
                                                                         :bottom 4
                                                                         :color (colors :light-blue-color)))
                                                   (c/config! icon-conf :visible? true)))
                          :mouse-exited  (fn [e] (do (c/config! e :border (line-border
                                                                           :bottom 4
                                                                           :color (colors :light-grey-color)))
                                                     (c/config! icon-conf :visible? false)))
                          :mouse-clicked (fn [e] (onClick e))
                          
                          :key-pressed  (fn [e] (if (= (.getKeyCode e) java.awt.event.KeyEvent/VK_ENTER) (onClick e)))]))
    my-panel))



(defn- access-panels-list [state! dispatch!]
  (let [mig (mig-panel :constraints ["wrap 4" "20px[ left]20px" "20px[]20px"])
        scr (c/scrollable mig :hscroll :never :preferred-size  [600 :by 220])] 
    (.setPreferredSize (.getVerticalScrollBar scr) (Dimension. 0 0))
    (.setUnitIncrement (.getVerticalScrollBar scr) 20)
    (.setBorder scr nil)
    
    (doall (map (fn [[k v]]
                  (.add mig (label-to-config
                             state! dispatch!
                             (:dbname v)
                             (:host v)
                             k
                             (:login (state!))
                             (:passwd (state!)))))
                (conn/datalist-mapper (conn/datalist-get))))
    
    (.add mig
          (c/border-panel
           :center (c/label
                    :icon (stool/image-scale icon/pen-128-png 34)
                    :halign :center
                    :listen [:mouse-clicked (fn [e]
                                              (c/config! (c/to-frame e)
                                                         :content (config-generator-panel
                                                                   state! dispatch! :empty)))])
           :border (line-border
                    :bottom 4
                    :color  (colors :light-grey-color))
           :maximum-size    [120 :by 120]
           :preferred-size  [120 :by 120]
           :background      "#fff"))
    scr))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; panels for login and error ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- error-panel [errors]
  (let [scr (c/scrollable
             (mig-panel
              :constraints ["wrap 1" "[grow, center]" "20px[]20px"]
              :items [[(c/label
                        :icon   (stool/image-scale icon/alert-red-512-png 8)
                        :border (b/empty-border :right 10)) "split 2"]
                      [(c/label
                        :text       (gtool/htmling "<h2>ERROR</h2>")
                        :foreground (colors :blue-green-color)
                        :font       (myFont 14))]
                      [(let [mig (mig-panel
                                  :constraints ["wrap 1" "40px[:600, grow, center]40px" "10px[]10px"]
                                  :items [[(c/label
                                            :text (gtool/htmling "<h2>About</h2>")
                                            :foreground (colors :blue-green-color)
                                            :font (myFont 14)) "align l"]
                                          [(c/label
                                            :text (gtool/htmling (str "<p align= \"justify\">" (:description errors) "</p>"))
                                            :foreground (colors :light-grey-color)
                                            :font (myFont 14)) "align l"]])]               
                         (doall
                          (map
                           (fn [x]
                             (.add mig x "align l"))
                           (if (= (:faq errors) nil)
                             (contact-info)
                             (concat ((faq-panel) (:faq errors)) contact-info))))
                         (.repaint mig) mig)]])
             :hscroll :never)]
    (.setUnitIncrement (.getVerticalScrollBar scr) 20)
    scr))



;; ┌──────────────┐
;; │              │
;; │  Info panel  │
;; │              │
;; └──────────────┘

;;;;;;;;;;;;;;;
;;
;; Content
;;

(defn faq-list
  "Description:
     Load Answer and Question"
  []
  [{:question "Why i can not get connection with server?"
    :answer   "Please check that you have entered the data correctly"}
   {:question "Where can i find data to connect?"
    :answer   "Please contact with your system administrator"}])

(defn info-list
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


(defn- info-header
  ([txt] (info-header txt 16))
  ([txt fsize]
     (gcomp/textarea
      txt
      :foreground (colors :blue-green-color)
      :font       (gtool/getFont fsize :bold))))

(defn- info-body
  [txt]
  (gcomp/textarea
   txt
   :foreground (colors :light-grey-color)
   :font       (gtool/getFont 14 :bold)
   :border     (b/empty-border :left 10 :top 5)))

;;;;;;;;;;;;;;;
;;
;; About
;;

(defn- about-panel
  "Description:
     Prepare info components"
  []
  (gcomp/vmig
   :tgap 15
   :items (gtool/join-mig-items
           (doall
            (map
             (fn [[header body]]
               (gcomp/vmig
                :vrules "[grow, fill]"
                :items (gtool/join-mig-items
                        (info-header header)
                        (info-body body))))
             (info-list))))))


;;;;;;;;;;;;;;;
;;
;; FAQ
;;

(defn faq-panel
  "Description:
     Return vertical mig with FAQs."
  []
  (let [faqs (doall
              (map
               (fn [faq-m]
                 (gcomp/vmig
                  :items (gtool/join-mig-items
                          (info-header (str "- " (:question faq-m)) 14)
                          (info-body (:answer faq-m)))))
               (faq-list)))
        
        compo (gcomp/vmig
               :tgap 10
               :items (gtool/join-mig-items
                       (info-header "FAQ")
                       faqs))]
    compo))


;;;;;;;;;;;;;;;
;;
;; Contact
;;

(defn contact-info
  [] 
  (gcomp/vmig
   :tgap 5
   :items (gtool/join-mig-items
           (info-header "Contacts")
           (info-body   "http://trashpanda-team.ddns.net")
           (info-body   "contact.tteam@gmail.com")
           (info-body   "+38 0 966 085 615"))))


(defn- info-logo
  "Description:
     Generate label with logo icon."
  []
  (c/label 
   :halign :center
   :icon   (stool/image-scale "icons/imgs/trashpanda2-stars-blue-1024.png" 47)))

(defn- info-section
  "Description:
     Generate mig with info items."
  []
  (let [info
        (gcomp/vmig
         :tgap   10
         :bgap   30
         :lgap   "12%"
         :rgap   "12%"
         :items  (gtool/join-mig-items
                  (rift (about-panel)  [])
                  (rift (faq-panel)    [])
                  (rift (contact-info) [])))]
    info))

(defn- return-to-login
  "Description:
     Generate button return to login panel."
  [state! dispatch!]
  (c/label ;; Return to login panel
   :text   "Back to login"
   :font   (gtool/getFont 15)
   :halign :center
   :border (b/empty-border :thickness 8)
   :icon   (stool/image-scale icon/user-blue1-64-png 40)
   :listen [:mouse-entered gtool/hand-hover-on
            :mouse-clicked (fn [e] (c/config!
                                    (c/to-frame e)
                                    :content (login-panel state! dispatch!)))]))

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
  (let [info-panel
        (gcomp/min-scrollbox
         (gcomp/vmig
          :hrules "[:800, grow, fill]"
          :tgap 25
          :items (gtool/join-mig-items
                  (info-logo)
                  (info-section)
                  (return-to-login state! dispatch!))))]
    info-panel))


;; ┌───────────────┐
;; │               │
;; │  Login panel  │
;; │               │
;; └───────────────┘

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
                    :path   [:login]
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
    :path   [:passwd]
    :action :update-pass}
   :placeholder (gtool/get-lang-btns :password)
   :style [:halign :left]))

(defn login-panel 
  "Description:
     Build and return to frame form for login panel.
  Example:
     (login-panel state! dispatch!)"
  [state! dispatch!]
  (gcomp/vmig
   :gap [10 5 10 10]
   :items (gtool/join-mig-items
           
           (gcomp/vmig ;; Main content
            :hrules "[grow, center]"
            :tgap 15
            :items (gtool/join-mig-items
                    
                    (c/label ;; Jarman logo
                     :icon (stool/image-scale "icons/imgs/jarman-text.png" 6))
                    
                    (gcomp/vmig ;; Login inputs
                     :wrap 2
                     :hrules "[fill]10px[200:, fill]"
                     :gap [5 5 10 0]
                     :items (gtool/join-mig-items
                             (c/label :icon (stool/image-scale icon/user-blue1-64-png 40))
                             (login-input  dispatch!)
                             (c/label :icon (stool/image-scale icon/key-blue-64-png 40))
                             (passwd-input dispatch!)))
                    
                    (access-panels-list ;; configs panels
                     state! dispatch!)))
           
           (gcomp/hmig ;; More info buttons
            :hrules "[grow, right]"
            :items [[(c/label :icon (stool/image-scale icon/I-64-png 40)
                              :listen [:mouse-entered gtool/hand-hover-on
                                       :mouse-clicked (fn [e]
                                                        (c/config!
                                                         (c/to-frame e)
                                                         :content (info-panel state! dispatch!)))])]]))))


;; ┌────────────────┐
;; │                │
;; │  Building lvl  │
;; │                │
;; └────────────────┘

(def debat (atom nil))
@debat

(defn- frame-login [state! dispatch!]
  (c/frame :title "Jarman-login"
         :undecorated? false
         :resizable? false
         :minimum-size [800 :by 600]
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

(defn start []
  (let [res-validation (validation)
        state  (create-state-template)
        state! (fn [& prop]
                 (cond (= :atom (first prop)) state
                       :else (deref state)))
        dispatch! (create-disptcher state)]
    (reset! debat state)
    (cm/swapp)
    (if (= res-validation nil)
      (-> (doto (frame-login state! dispatch!) (.setLocationRelativeTo nil) (apply-stylesheet my-style)) seesaw.core/pack! seesaw.core/show!)
      (-> (doto (frame-error state! dispatch!) (.setLocationRelativeTo nil) (apply-stylesheet my-style))
          (c/config! :content (error-panel res-validation)) seesaw.core/pack! seesaw.core/show!))))

(state/set-state :invoke-login-panel start)

(start)



;; Start app window
;; (-> (doto (seesaw.core/frame
;;            :title "DEBUG WINDOW" :undecorated? false
;;            :minimum-size [200 :by 200]
;;            :size [200 :by 200]
;;            :content (label :text "a" :border ((color-border "#222"))))
;;       (.setLocationRelativeTo nil) pack! show!))
