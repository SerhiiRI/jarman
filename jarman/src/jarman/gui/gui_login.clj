(ns jarman.gui.gui-login
  (:use seesaw.core
        seesaw.border
        seesaw.dev
        seesaw.style
        seesaw.mig
        seesaw.font)
  (:import (java.text SimpleDateFormat)
           (javax.swing JScrollPane)
           (java.awt Dimension))
  (:require [clojure.string :as string]
            [jarman.gui.gui-tools :as tool]
            [jarman.config.config-manager :as conf]
            [jarman.resource-lib.icon-library :as icon]
            [clojure.java.jdbc :as jdbc]
            [jarman.tools.swing :as stool]
            [jarman.gui.gui-app :as app]
            [jarman.gui.gui-components :as components]
            [jarman.logic.connection :as c]
            ;; [jarman.config.init :refer [configuration language swapp-all save-all-cofiguration make-backup-configuration]]
            ))
;;;;;;;;;;;;;;;;;;;;;;;;
;;; validation login ;;;
;;;;;;;;;;;;;;;;;;;;;;;;
(def validate1 {:validate? false :output {:description "DB connection is not set DB connection is not set DB connection is not set DB connection is not set"}})
(def validate2 {:validate? false :output {:description "some description some description some description"
                                          :faq [{:q "Why i saw this error"
                                                 :a "Because your program has trouble"}
                                                {:q "What it problem mean"
                                                 :a "Read the fucking descriptin"}]}})
(def validate3 {:validate? true})
(def validate4 {:validate? true})

(defn- validation []
  (cond 
    ;;(not (:validate? validate1)) (:output validate1)
    ;;(not (:validate? validate2)) (:output validate2)
    (not (:validate? validate3)) (:output validate3)
    (not (:validate? validate4)) (:output validate4)
    :else nil))

(defn- authenticate-user [login password]
  (if (and (= login "a")(= password "a")) true))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; style color and font ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def dark-grey-color "#676d71")
(def blue-color "#256599")
(def light-grey-color "#82959f")
(def blue-green-color "#2c7375")
(def light-blue-color "#96c1ea")
(def red-color "#e51a4c")
(def back-color "#c5d3dd")

(def my-style {[:.css1] {:foreground blue-green-color}})

(defn color-border [color]
  (compound-border (empty-border :left 10 :right 10 :top 5 :bottom 5)
                   (line-border :bottom 2 :color color)))
(defn myFont [size]
  {:size size :font "Arial" :style :bold})

(def ^:private emp-border (empty-border :left 10 :right 10 :top 5 :bottom 5))

;;;;;;;;;;;;;;;;;;;;;;;
;;; some-components ;;; 
;;;;;;;;;;;;;;;;;;;;;;;

(declare asks-panel)
(declare login-panel)
(defn some-text [color]
  (label :text (tool/htmling "<p align= \"justify\">It is a long established fact that a reader will be distracted by the readable content of a page when lookiult model text, and a search for 'lorem ipsum' will uncover many web sites still in their infancy. Various versions have evolved over the years, sometimes by accident, sometimes on purpose (injected humour and the like)</p>") :foreground color :font (myFont 14)))

(def  contact-info [(label :text (tool/htmling "<h2>Contacts</h2>") :foreground blue-green-color :font (myFont 14))
                             (horizontal-panel :items (list (label :text "Website:"
                                                                   :foreground  blue-green-color :font (myFont 14)
                                                                   :border (empty-border :right 8))
                                                            (label :text "http://trashpanda-team.ddns.net"
                                                                   :foreground  light-grey-color :font (myFont 14))))
                             (horizontal-panel :items (list (label :text "Phone:"
                                                                   :foreground  blue-green-color :font (myFont 14)
                                                                   :border (empty-border :right 8))
                                                            (label :text "+380966085615"
                                                                   :foreground  light-grey-color :font (myFont 14))))
                             (horizontal-panel :items (list (label :text "Email:"
                                                                   :foreground  blue-green-color :font (myFont 14)
                                                                   :border (empty-border :right 8))
                                                            (label :text "contact.tteam@gmail.com"
                                                                   :foreground  light-grey-color :font (myFont 14))))])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; config-generator-JDBC + panel for config ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def confgen-header
  (fn [title] (label :text title :font (tool/getFont 16 :bold)
                     :foreground blue-green-color
                     :border (compound-border
                              (line-border :bottom 2 :color dark-grey-color)(empty-border :bottom 5)))))

(def confgen-title
  (fn [title]
    (label :text title :foreground blue-green-color :font (tool/getFont 14 :bold))))

(def confgen-input 
  (fn [data] (text :text data :font (tool/getFont 14)
               :background "#fff"
               :columns 20
               :border (color-border light-grey-color))))

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
    (conf/assoc-in-value [:database.edn :datalist key-title :dbtype] dbtype)
    (conf/assoc-in-value [:database.edn :datalist key-title :host] host)
    (conf/assoc-in-value [:database.edn :datalist key-title :port] port)
    (conf/assoc-in-value [:database.edn :datalist key-title :dbname] dbname)
    (conf/assoc-in-value [:database.edn :datalist key-title :user] user)
    (conf/assoc-in-value [:database.edn :datalist key-title :password] password))
  (if (:valid? (conf/store)) "yes"))

(defn keys-generator [dbname title]
  (keyword (string/replace (str dbname "--" title) #"\." "_")))

(defn create-map [dbtype host port dbname user password]
  (conf/assoc-in-segment [:database.edn :datalist (keys-generator dbname host)]
                    (-> template-map
                               (assoc-in  [:value :dbtype :value] dbtype)
                               (assoc-in  [:value :host :value] host)
                               (assoc-in  [:value :port :value] (Integer. port))
                               (assoc-in  [:value :dbname :value] dbname)
                               (assoc-in  [:value :user :value] user)
                               (assoc-in  [:value :password :value] password)))  
  (if (:valid? (conf/store)) "yes"))

 ;;(store)
;; (swapp)

(defn delete-map [key-title]
  (conf/update-in-value [:database.edn :datalist] (fn [x] (dissoc x key-title)))
  (if (:valid? (conf/store)) "yes"))

(defn validate-fields
  "Description
    this function vaalidate text fields from config-generator-panel
    if sum of fields 0 -> validation succses -> update-map, else validation fail"
  [v-dbtype v-host v-port v-dbname v-user v-password key-title]
  (if (= 0 (+ (if (= (text v-dbtype) "") (do (config! v-dbtype :border (color-border red-color)) 1)
                  (do (config! v-dbtype :border (color-border blue-green-color)) 0))
              (if (< (count (text v-host)) 4) (do (config! v-host :border (color-border red-color)) 1)
                  (do (config! v-host :border (color-border blue-green-color)) 0))
              (if (= (re-find #"[\d]+" (text v-port)) nil)
                (do (config! v-port :border (color-border red-color)) 1)
                (if (< (Integer. (text v-port)) 65000)
                  (do (config! v-port :border (color-border blue-green-color)) 0)
                  (do (config! v-port :border (color-border red-color)) 1)))
              (if (= (text v-dbname) "") (do (config! v-dbname :border (color-border red-color)) 1)
                  (do (config! v-dbname :border (color-border blue-green-color)) 0))
              (if (= (text v-user) "") (do (config! v-user :border (color-border red-color)) 1)
                  (do (config! v-user :border (color-border blue-green-color)) 0))
              (if (= (text v-password) "") (do (config! v-password :border (color-border red-color)) 1)
                  (do (config! v-password :border (color-border blue-green-color)) 0))))           
    (do 
      (if (= key-title :empty)
        (create-map
         (text v-dbtype)
         (text v-host)
         (text v-port)
         (text v-dbname)
         (text v-user)
         (text v-password))
        (update-map
         (text v-dbtype)
         (text v-host)
         (text v-port)
         (text v-dbname)
         (text v-user)
         (text v-password)
         key-title)))))

(defn config-generator-fields [key-title]
  (let [data (key-title (c/datalist-get))
        dbtype-inp (text :text "mysql" :font (tool/getFont 14)
                         :editable? false
                         :background "#fff"
                         :columns 20
                         :border (color-border light-grey-color))
        host-inp (confgen-input (:value (:host (:value data))))
        port-inp (confgen-input (:value (:port (:value data))))
        dbname-inp (confgen-input (:value (:dbname (:value data))))
        user-inp (confgen-input (:value (:user (:value data))))
        password-inp (confgen-input (:value (:password (:value data))))
        err-lbl (label :text "" :foreground blue-green-color :font (myFont 14))
        btn-del (label :text "Delete" :background "#fff"
                       :class :css1
                       :border (empty-border :top 10 :right 10 :left 10 :bottom 10)
                       :listen [:mouse-entered (fn [e] (config! e :background back-color :cursor :hand))
                                :mouse-exited  (fn [e] (config! e :background "#fff"))
                                :mouse-clicked (fn [e] (if (= "yes" (delete-map key-title))
                                                         (config! (to-frame e) :content (login-panel))))])
        btn-conn (label :text "Connect" :background "#fff"
                       :class :css1
                       :border (empty-border :top 10 :right 10 :left 10 :bottom 10)
                       :listen [:mouse-entered (fn [e] (config! e :background back-color :cursor :hand))
                                :mouse-exited  (fn [e] (config! e :background "#fff"))
                                :mouse-clicked (fn [e] (if (= nil (c/test-connection {:dbtype (text dbtype-inp)
                                                                                      :host (text host-inp)
                                                                                      :port (text port-inp)
                                                                                      :dbname (text dbname-inp)
                                                                                      :user (text user-inp)
                                                                                      :password (text password-inp)}))
                                                         (config! err-lbl :text "ERROR!! PLEASE RECONNECT" :foreground red-color)
                                                         (config! err-lbl :text "SUCCESS" :foreground blue-green-color)
                                                         ))])
        btn-ent (label :text "EDDIT" :background "#fff"
                     :class :css1
                     :border (empty-border :top 10 :right 10 :left 10 :bottom 10)
                     :listen [:mouse-entered (fn [e] (config! e :background back-color :cursor :hand))
                              :mouse-exited  (fn [e] (config! e :background "#fff"))
                              :mouse-clicked (fn [e] (if (= (validate-fields dbtype-inp host-inp port-inp
                                                                             dbname-inp user-inp password-inp key-title) "yes")
                                                       (config! (to-frame e)
                                                                :content (login-panel))))])
        mig (mig-panel
             :constraints ["wrap 1" "[grow, center]" "20px[]20px"]
             :items [[(confgen-header "Some title")]
                     [(grid-panel :columns 1 :items [(confgen-title (:name (:dbtype (:value template-map)))) dbtype-inp])]
                     [(grid-panel :columns 1 :items [(confgen-title (:name (:host (:value template-map)))) host-inp])]
                     [(grid-panel :columns 1 :items [(confgen-title (:name (:port (:value template-map)))) port-inp])]
                     [(grid-panel :columns 1 :items [(confgen-title (:name (:dbname (:value template-map)))) dbname-inp])]
                     [(grid-panel :columns 1 :items [(confgen-title (:name (:user (:value template-map)))) user-inp])]
                     [(grid-panel :columns 1 :items [(confgen-title (:name (:password (:value template-map)))) password-inp])]])]
    (if (= key-title :empty)
      (do (config! btn-ent :text "CREATE")
          (.add mig btn-conn "split 2")
          (.add mig btn-ent)
          (.add mig err-lbl))
      (do (.add mig btn-ent "split 3")
          (.add mig btn-conn)
          (.add mig btn-del)
          (.add mig err-lbl)))
   mig))

(defn config-generator-panel [key-title]
  (let [info some-text
        faq [{:q "Why i saw this error"
              :a "Because your program has trouble"}
             {:q "What it problem mean"
              :a "Read the fucking descriptin"}]
        scr (scrollable
             (mig-panel
              :constraints ["wrap 1" "[grow, center]" "20px[]20px"]
              :items [[(label :icon (stool/image-scale icon/left-blue-64-png 50)
                              :listen [:mouse-entered (fn [e] (tool/hand-hover-on e))
                                       :mouse-exited (fn [e] (tool/hand-hover-off e))
                                       :mouse-clicked (fn [e] (config! (to-frame e) :content (login-panel)))]
                              :border (empty-border :right 150)) "align l, split 2"]
                      ;; [(label :icon (stool/image-scale icon/refresh-connection-grey1-64-png 80)
                      ;;         :border (empty-border :right 30))]
                      [(label :text "CONNECT TO DATABASE"
                              :foreground blue-green-color :font (myFont 18)) "align l"]    
                      [(let [mig (mig-panel
                                  :constraints ["wrap 1" "100px[:600, grow, center]100px" "20px[]20px"]
                                  :items [
                                          [(label :text (tool/htmling "<h2>About</h2>")
                                                  :foreground blue-green-color :font (myFont 14)) "align l"]
                                          [(label :text "some-text" :font (myFont 14) :foreground dark-grey-color)
                                           "align l"]
                                          [(config-generator-fields key-title) "align l"]])]
                         (doall (map (fn [x] (.add mig x "align l")) (if (= faq nil) nil (asks-panel faq))))
                         (.repaint mig) mig)]])
             :hscroll :never)]
    (.setUnitIncrement (.getVerticalScrollBar scr) 20)
    scr))

(defn db-connect-error []
  [[(vertical-panel
              :background "#fff"
              :items (list (label :text "Some error" :font (myFont 15) :foreground blue-color
                                  :border (empty-border :top 5 :left 5 :bottom 5))
                           (label :text "Connection false" :font (myFont 12) :foreground light-grey-color
                                  :border (empty-border :top 5 :left 5 :bottom 5)
                                  :preferred-size  [20 :by 20]))) :north]])

(defn get-values [some-key]
  (string/split (string/replace (name some-key) #"\_" ".") #"\--" ))


(defn test-key-connection [key-title]
  (let [data (key-title (c/datalist-mapper (c/datalist-get)))]
    (c/test-connection {:dbtype (:dbtype data)
                        :host (:host data)
                        :port (:port data)
                        :dbname (:dbname data)
                        :user (:user data)
                        :password (:password data)})))


(defn label-to-config [dbname title key-title] 
  (let [icon-conf
        (label :icon (stool/image-scale icon/settings-64-png 40)
               :border (compound-border (empty-border :top 10 :left 55))
               :halign :right
               :visible? false
               :border (empty-border :right 5 :bottom 5)
               :listen [:mouse-entered (fn [e] (do (tool/hand-hover-on e) (config! e :visible? true)))
                        :mouse-exited (fn [e] (do (tool/hand-hover-off e) (config! e :visible? false)))
                        :mouse-clicked (fn [e] (do (tool/hand-hover-off e) (config! (to-frame e)
                                                                               :content (config-generator-panel key-title))))])
        my-panel (border-panel
                  :border (line-border :bottom 4 :color light-grey-color)
                  :maximum-size  [120 :by 120]
                  :preferred-size  [120 :by 120]
                  :background "#fff")]
    (do (.removeAll my-panel)
        (config! my-panel  :items  [[(vertical-panel
                                      :background "#fff"
                                      :items (list (label :text dbname :font (myFont 15) :foreground blue-color
                                                          :border (empty-border :top 5 :left 5 :bottom 5))
                                                   (label :text title :font (myFont 12) :foreground light-grey-color
                                                          :border (empty-border :top 5 :left 5 :bottom 5)
                                                          :preferred-size  [20 :by 20]))) :north]
                                    [icon-conf :south]]))
    (.repaint my-panel)
    (config! my-panel :listen [:mouse-entered (fn [e] (do 
                                                        (config! e :border (line-border :bottom 4 :color light-blue-color))
                                                        (config! icon-conf :visible? true)))
                               :mouse-exited  (fn [e] (do (config! e :border (line-border :bottom 4 :color light-grey-color))
                                                          (config! icon-conf :visible? false)))
                               :mouse-clicked (fn [e] (if (= (println (test-key-connection key-title)) nil)
                                                        (do
                                                          (.removeAll my-panel)
                                                          (config! my-panel :items (db-connect-error)
                                                                   :border (line-border :bottom 4
                                                                                        :color red-color)
                                                                   :listen [:mouse-clicked (fn [e] (config! (to-frame e) :content info-panel))
                                                                            :mouse-exited  (fn [e] (do (config! e :border (line-border :bottom 4 :color red-color))))
                                                                            :mouse-entered  (fn [e] (do (config! e :border (line-border :bottom 4 :color red-color))))]))))])
    my-panel))



(defn configurations-panel []
  (let [mig (mig-panel
                     :constraints ["wrap 4" "20px[ left]20px" "20px[]20px"])
        scr
        (scrollable mig  :hscroll :never                            
                    :preferred-size  [600 :by 220])] 
    (.setPreferredSize (.getVerticalScrollBar scr) (Dimension. 0 0))
    (.setUnitIncrement (.getVerticalScrollBar scr) 20)
    (.setBorder scr nil)
    (doall (map (fn [[k v]] (.add mig (label-to-config (:dbname v) (:host v) k)))
                (c/datalist-mapper (c/datalist-get))))
    (.add mig (label :icon (stool/image-scale icon/a-grey-64-png 10) ;;add-png
                     :background back-color
                     :listen [:mouse-clicked (fn [e] (config! (to-frame e) :content (config-generator-panel :empty)))]
                     :border (line-border :color back-color
                                          :bottom 10
                                          :top 10
                                          :left 10
                                          :right 10)))
    scr))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; panels for login and error ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- error-panel [errors]
  (let [scr (scrollable
             (mig-panel
              :constraints ["wrap 1" "[grow, center]" "20px[]20px"]
              :items [[(label :icon (stool/image-scale icon/alert-red-512-png 8)
                              :border (empty-border :right 10)) "split 2"]
                      [(label :text (tool/htmling "<h2>ERROR</h2>") :foreground blue-green-color :font (myFont 14))]
                      [(let [mig (mig-panel
                                  :constraints ["wrap 1" "40px[:600, grow, center]40px" "10px[]10px"]
                                  :items [[(label :text (tool/htmling "<h2>About</h2>")
                                                  :foreground blue-green-color :font (myFont 14)) "align l"]
                                          [(label :text (tool/htmling (str "<p align= \"justify\">" (:description errors) "</p>"))
                                                  :foreground light-grey-color
                                                  :font (myFont 14)) "align l"]])]               
                         (doall (map (fn [x] (.add mig x "align l"))
                                     (if (= (:faq errors) nil) contact-info (concat (asks-panel (:faq errors)) contact-info))))
                         (.repaint mig) mig)]])
             :hscroll :never)]
    (.setUnitIncrement (.getVerticalScrollBar scr) 20)
    scr))

(defn asks-panel [faq]
  (let [mig
        (mig-panel
         :constraints ["wrap 1" "20px[grow, center]" "15px[]15px"]
         :items [])]
    (doall (map (fn [x] (do (.add mig (label :text (str "- " (:q x)) :foreground blue-green-color :font (myFont 14)) "align l")
                            (.add mig (label :text (:a x) :foreground light-grey-color :font (myFont 14)) "align l"))) faq))
    (.repaint mig)
    [(label :text (tool/htmling "<h2>FAQ</h2>")
            :foreground blue-green-color
            :font (myFont 14)) mig]))

(def ^:private info-panel
  (let [info some-text
        faq [{:q "Why i saw this error"
              :a "Because your program has trouble"}
             {:q "What it problem mean"
              :a "Read the fucking descriptin"}]
        scr (scrollable
             (mig-panel
              :constraints ["wrap 1" "[grow, center]" "20px[]20px"]
              :items [[(label :icon (stool/image-scale icon/left-blue-64-png 50)
                              :listen [:mouse-entered (fn [e] (tool/hand-hover-on e))
                                       :mouse-exited (fn [e] (tool/hand-hover-off e))
                                       :mouse-clicked (fn [e] (config! (to-frame e) :content (login-panel)))]
                              :border (empty-border :right 220)) "align l, split 2"]
                      [(label :icon (stool/image-scale "resources/imgs/trashpanda2-stars-blue-1024.png" 47))]
                      [(let [mig (mig-panel
                                  :constraints ["wrap 1" "100px[:600, grow, center]100px" "20px[]20px"]
                                  :items [[(label :text (tool/htmling "<h2>About</h2>")
                                                  :foreground blue-green-color :font (myFont 14)) "align l"]
                                          [(info  light-grey-color)]
                                          [(label :text (tool/htmling "<h2>Jarman</h2>")
                                                  :foreground blue-green-color :font (myFont 14)) "align l"]
                                          [(info  light-grey-color)]
                                          [(label :text (tool/htmling "<h2>Contact</h2>")
                                                  :foreground blue-green-color :font (myFont 14)) "align l"]
                                          [(info  light-grey-color)]
                                          [(label :text (tool/htmling "<h2>Links</h2>")
                                                  :foreground blue-green-color :font (myFont 14)) "align l"] 
                                          [(label :text (tool/htmling "<p align= \"justify\">http://trashpanda-team.ddns.net</p>")
                                                  :foreground light-grey-color :font (myFont 14) ) "align l"]])]
                         (doall (map (fn [x] (.add mig x "align l")) (if (= faq nil)
                                                                       contact-info (concat (asks-panel faq) contact-info))))
                         (.repaint mig) mig)]]))]
    (.setUnitIncrement (.getVerticalScrollBar scr) 20)
    scr))

(show-options (border-panel))


(defn login-panel []
  (let [flogin (components/input-text :placeholder "Login"
                                      :args [:columns 20
                                             :border (compound-border emp-border
                                                                      (line-border :bottom 4 :color light-blue-color))
                                             :halign :left])
        fpass (components/input-password :placeholder "Password"
                                         :style [
                                                 :columns 20
                                                 :border (compound-border emp-border
                                                                          (line-border :bottom 4 :color light-blue-color))
                                                 :halign :left])]
    (mig-panel
     :constraints ["wrap 1" "[ center]" "20px[]0px"]
     :items [[(label :icon (stool/image-scale "resources/imgs/jarman-text.png" 6))]
             [(vertical-panel :items (list  
                                      (mig-panel
                                       :constraints ["" "[grow, fill]" "20px[]20px"]
                                       :items [[(label :icon (stool/image-scale icon/user-blue1-64-png 40))]
                                               [flogin]
                                               [(label :border (empty-border :right 20))]])
                                      (mig-panel
                                       :constraints ["" "[grow, fill]" "5px[]10px"]
                                       :items [[(label :icon (stool/image-scale icon/key-blue-64-png 40))]
                                               [fpass]
                                               [(label :border (empty-border :right 20))]])))]
             [(configurations-panel)]
             [(label :text "" :border
                     (empty-border :top 20 :left 860 )) "split 2"]
             [(mig-panel
               :constraints ["" "[grow, fill]" ""]
               :items [[(label :icon (stool/image-scale icon/refresh-connection-grey1-64-png 40)
                               :border (compound-border (empty-border :right 10 )))]
                       [(label :icon (stool/image-scale icon/a-blue-64-png 40) ;;I-grey-64-png
                               :listen [:mouse-clicked (fn [e] (config! (to-frame e) :content info-panel))])]])]])))

;;;;;;;;;;;;;;
;;; frames ;;;
;;;;;;;;;;;;;;

(defn- frame-login []
  (frame :title "Jarman-login"
         :undecorated? false
         :resizable? false
         :minimum-size [800 :by 600]
         :content (login-panel)))

(defn- frame-error []
  (frame :title "Jarman-error"
         :undecorated? false
         :resizable? false
         :minimum-size [600 :by 600]))

(defn start []
  (let [res-validation (validation)]
    (if (= res-validation nil)
      (-> (doto (frame-login) (.setLocationRelativeTo nil) (apply-stylesheet my-style)) seesaw.core/pack! seesaw.core/show!)
      (-> (doto (frame-error) (.setLocationRelativeTo nil)(apply-stylesheet my-style))
          (config! :content (error-panel res-validation)) seesaw.core/pack! seesaw.core/show!))))

(start)




;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; multi-panel for replace some panels ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def red-panel
  (fn []
    (mig-panel
     :constraints ["" "[grow, fill]" ""]
     :items [[(label :text "red"
                     :border (color-border red-color)
                     :background red-color )]])))

(def green-panel
  (fn []
    (mig-panel
     :constraints ["" "[grow, fill]" ""]
     :items [[(label :text "green"
                     :border (color-border blue-green-color)
                     :background blue-green-color)]])))

(def blue-panel
  (fn []
    (mig-panel
     :constraints ["" "[grow, fill]" ""]
     :items [[(label :text "blue"
                     :border (color-border blue-color)
                     :background blue-color)]])))

(defn multi-panel
  "Description:
    get vector of panels and return mig-panel in which these panels are replaced on click of arrow
   Example:
    (multi-panel [some-panel-1 some-panel-2 some-panel-3])"
  [panels title num]
  (mig-panel
   :constraints ["wrap 3" "[fill][grow, fill, center][fill]" "[fill][grow, fill][fill]15px"] 
   :items [[(label :text title
                   :foreground "#256599"
                   :border (empty-border :left 10)
                   :font {:size 16 font "Arial" :style :bold}) "span 3"]
           [((nth panels num)) "span 3"]
           [(label :icon (stool/image-scale icon/a-up-grey-64-png 50) ;;left-blue-64-png
                   :visible? (if (= num 0) false true)
                   :listen [:mouse-entered (fn [e] (hand-hover-on e))
                            :mouse-exited (fn [e] (hand-hover-off e))
                            :mouse-clicked (fn [e]
                                             (if (= num 0)
                                               (config! (to-frame e) :content (multi-panel panels title num))
                                               (config! (to-frame e) :content (multi-panel panels title (- num 1)))))])
            "align l"]
           [(label :text "")]
           [(label :icon (stool/image-scale icon/arrow-blue-grey-left-64-png 36) ;;right-blue-64-png
                   :visible? (if (= num (- (count panels) 1)) false true)
                   :listen [:mouse-clicked (fn [e] (if (=  num (- (count panels) 1))
                                                     (config! (to-frame e) :content (multi-panel panels title num))
                                                     (config! (to-frame e) :content (multi-panel panels title (+ num 1)))))])
            "align r"]]))


(defn- test-frame []
  (frame :title "Jarman"
         :undecorated? false
         :content (multi-panel [red-panel green-panel blue-panel] "Some panel" 0)
         :resizable? true
         :minimum-size [800 :by 600]))

(-> (doto (test-frame) (.setLocationRelativeTo nil) ) seesaw.core/pack! seesaw.core/show!)








