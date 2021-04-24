

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
  (if (and (= login "admin")(= password "admin")) true))

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
  (label :text (htmling "<p align= \"justify\">It is a long established fact that a reader will be distracted by the readable content of a page when lookiult model text, and a search for 'lorem ipsum' will uncover many web sites still in their infancy. Various versions have evolved over the years, sometimes by accident, sometimes on purpose (injected humour and the like)</p>") :foreground color :font (myFont 14)))

(def  contact-info [(label :text (htmling "<h2>Contacts</h2>") :foreground blue-green-color :font (myFont 14))
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

(defn test-connection [db-spec]
  ;; {:pre [(spec/valid? ::db-connector-scheme db-spec)]}
  (let [subprotocols {"hsql" "hsqldb"
                      "jtds" "jtds:sqlserver"
                      "mssql" "sqlserver"
                      "oracle" "oracle:thin"
                      "oracle:sid" "oracle:thin"
                      "postgres" "postgresql"}
        host-prefixes {"oracle:oci"  "@"
                       "oracle:thin" "@"}
        dbname-separators {"mssql" ";DATABASENAME="
                           "sqlserver"  ";DATABASENAME="
                           "oracle:sid" ":"}
        dbtype (:dbtype db-spec)
        port (:port db-spec)
        host (:host db-spec)
        subprotocol (subprotocols dbtype dbtype)
        port (when port (str ":" port))
        db-sep (dbname-separators dbtype "/")]
    (java.sql.DriverManager/setLoginTimeout 1) 
    (try
      (let [connector (java.sql.DriverManager/getConnection (do (println (str "jdbc:" subprotocol "://" host port))
                                                                (str "jdbc:" subprotocol "://" host port db-sep "?socketTimeout=4000&loginTimeout=4000&connectTimeout=4000")) (:user db-spec) (:password db-spec))]
        (jdbc/query connector (jdbc/prepare-statement connector "SHOW DATABASES" {:timeout 4})))
      (catch com.mysql.jdbc.exceptions.jdbc4.CommunicationsException _ nil) 
      (catch java.net.ConnectException _ nil)
      (catch Exception _ nil))))


(def configuration-copy (atom @configuration))

(def keys-connect-JDBC (keys (:value (:JDBC-mariadb-configuration-database (:value (:database.edn @configuration))))))
(def path-connect-JDBC [:database.edn :value :JDBC-mariadb-configuration-database :value])
(def path-connect [:database.edn :value :JDBC-mariadb-configuration-database])

(defn get-element-map [& args]
  (get-in @configuration (concat path-connect-JDBC args)))

(defn get-name-map [& args]
  (get-in @configuration (concat path-connect args)))

(def confgen-header
  (fn [title] (label :text title :font (getFont 16 :bold)
                     :foreground blue-green-color
                     :border (compound-border
                              (line-border :bottom 2 :color dark-grey-color)(empty-border :bottom 5)))))

(def confgen-title
  (fn [title]
    (label :text title :foreground blue-green-color :font (getFont 14 :bold))))

(def confgen-input 
  (fn [data] (text :text data :font (getFont 14)
               :background "#fff"
               :columns 20
               :border (color-border light-grey-color))))

(defn update-map [dbtype host port dbname user password]
  (reset! configuration-copy (-> @configuration-copy
                                 (assoc-in (concat path-connect-JDBC
                                                   [:dbtype :value]) dbtype)
                                 (assoc-in (concat path-connect-JDBC
                                                   [:host :value]) host)
                                 (assoc-in (concat path-connect-JDBC
                                                   [:port :value]) port)
                                 (assoc-in (concat path-connect-JDBC
                                                   [:dbname :value]) dbname)
                                 (assoc-in (concat path-connect-JDBC
                                                   [:user :value]) user)
                                 (assoc-in (concat path-connect-JDBC
                                                   [:password :value]) password)))
  (save-all-cofiguration @configuration-copy)
  (swapp-all))

(defn validate-fields
  "Description
    this function vaalidate text fields from config-generator-panel
    if sum of fields 0 -> validation succses -> update-map, else validation fail"
  [v-dbtype v-host v-port v-dbname v-user v-password e-lbl]
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
    (if (= nil (test-connection {:dbtype (text v-dbtype)
                                 :host (text v-host)
                                 :port (text v-port)
                                 :dbname (text v-dbname)
                                 :user (text v-user)
                                 :password (text v-password)}))
      (config! e-lbl :text "ERROR!! PLEASE RECONNECT" :foreground red-color) 
      (do (config! e-lbl :text "SUCCESS" :foreground blue-green-color)
         (update-map (text v-dbtype)
                     (text v-host)
                     (text v-port)
                     (text v-dbname)
                     (text v-user)
                     (text v-password))))))

(defn config-generator-fields []
  (let [dbtype-inp (confgen-input (get-element-map :dbtype :value))
        host-inp (confgen-input (get-element-map :host :value))
        port-inp (confgen-input (get-element-map :port :value))
        dbname-inp (confgen-input (get-element-map :dbname :value))
        user-inp (confgen-input (get-element-map :user :value))
        password-inp (confgen-input (get-element-map :password :value))
        err-lbl (label :text "" :foreground blue-green-color :font (myFont 14))]
    (mig-panel
     :constraints ["wrap 1" "[grow, center]" "20px[]20px"]
     :items [
             [(confgen-header (get-name-map :name))]
             [(grid-panel :columns 1 :items [(confgen-title (get-element-map :dbtype :name)) dbtype-inp])]
             [(grid-panel :columns 1 :items [(confgen-title (get-element-map :host :name)) host-inp])]
             [(grid-panel :columns 1 :items [(confgen-title (get-element-map :port :name)) port-inp])]
             [(grid-panel :columns 1 :items [(confgen-title (get-element-map :dbname :name)) dbname-inp])]
             [(grid-panel :columns 1 :items [(confgen-title (get-element-map :user :name)) user-inp])]
             [(grid-panel :columns 1 :items [(confgen-title (get-element-map :password :name)) password-inp])]
             [(label :text "CONNECT" :background "#fff"
                     :class :css1
                     :border (empty-border :top 10 :right 10 :left 10 :bottom 10)
                     :listen [:mouse-entered (fn [e] (config! e :background back-color :cursor :hand))
                              :mouse-exited  (fn [e] (config! e :background "#fff"))
                              :mouse-clicked (fn [e] (validate-fields dbtype-inp host-inp port-inp
                                                                      dbname-inp user-inp password-inp err-lbl))])]
              [err-lbl]])))

(def ^:private config-generator-panel
  (let [info some-text
        faq [{:q "Why i saw this error"
              :a "Because your program has trouble"}
             {:q "What it problem mean"
              :a "Read the fucking descriptin"}]]
    (scrollable
     (mig-panel
      :constraints ["wrap 1" "[grow, center]" "20px[]20px"]
      :items [[(label :icon (stool/image-scale icon/left-blue-64-png 50)
                      :listen [:mouse-clicked (fn [e] (config! (to-frame e) :content login-panel))]
                      :border (empty-border :right 150)) "align l, split 2"]
              ;; [(label :icon (stool/image-scale icon/refresh-connection-grey1-64-png 80)
              ;;         :border (empty-border :right 30))]
              [(label :text "CONNECT TO DATABASE"
                      :foreground blue-green-color :font (myFont 18)) "align l"]    
              [(let [mig (mig-panel
                          :constraints ["wrap 1" "100px[:600, grow, center]100px" "20px[]20px"]
                          :items [
                                  [(label :text (htmling "<h2>About</h2>")
                                          :foreground blue-green-color :font (myFont 14)) "align l"]
                                  [(label :text (get-name-map :doc) :font (myFont 14) :foreground dark-grey-color)
                                   "align l"]
                                  [(config-generator-fields) "align l"]])]
                 (doall (map (fn [x] (.add mig x "align l")) (if (= faq nil) nil (asks-panel faq))))
                 (.repaint mig) mig)]])
     :hscroll :never)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; panels for login and error ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- error-panel [errors]
  (scrollable
   (mig-panel
    :constraints ["wrap 1" "[grow, center]" "20px[]20px"]
    :items [[(label :icon (stool/image-scale icon/alert-red-512-png 8)
                    :border (empty-border :right 10)) "split 2"]
            [(label :text (htmling "<h2>ERROR</h2>") :foreground blue-green-color :font (myFont 14))]
            [(let [mig (mig-panel
                        :constraints ["wrap 1" "40px[:600, grow, center]40px" "10px[]10px"]
                        :items [[(label :text (htmling "<h2>About</h2>")
                                        :foreground blue-green-color :font (myFont 14)) "align l"]
                                [(label :text (htmling (str "<p align= \"justify\">" (:description errors) "</p>"))
                                        :foreground light-grey-color
                                        :font (myFont 14)) "align l"]])]               
               (doall (map (fn [x] (.add mig x "align l")) (if (= (:faq errors) nil) contact-info (concat (asks-panel (:faq errors)) contact-info))))
               (.repaint mig) mig)]])
   :hscroll :never))

(defn asks-panel [faq]
  (let [mig
        (mig-panel
         :constraints ["wrap 1" "20px[grow, center]" "15px[]15px"]
         :items [])]
    (doall (map (fn [x] (do (.add mig (label :text (str "- " (:q x)) :foreground blue-green-color :font (myFont 14)) "align l")
                            (.add mig (label :text (:a x) :foreground light-grey-color :font (myFont 14)) "align l"))) faq))
    (.repaint mig)
    [(label :text (htmling "<h2>FAQ</h2>")
            :foreground blue-green-color
            :font (myFont 14)) mig]))

(def ^:private info-panel
  (let [info some-text
        faq [{:q "Why i saw this error"
              :a "Because your program has trouble"}
             {:q "What it problem mean"
              :a "Read the fucking descriptin"}]]
    (scrollable
     (mig-panel
      :constraints ["wrap 1" "[grow, center]" "20px[]20px"]
      :items [[(label :icon (stool/image-scale icon/left-blue-64-png 50)
                      :listen [:mouse-clicked (fn [e] (config! (to-frame e) :content login-panel))]
                      :border (empty-border :right 220)) "align l, split 2"]
              [(label :icon (stool/image-scale "resources/imgs/trashpanda2-stars-blue-1024.png" 47))]
              [(let [mig (mig-panel
                          :constraints ["wrap 1" "100px[:600, grow, center]100px" "20px[]20px"]
                          :items [[(label :text (htmling "<h2>About</h2>")
                                          :foreground blue-green-color :font (myFont 14)) "align l"]
                                  [(info  light-grey-color)]
                                  [(label :text (htmling "<h2>Jarman</h2>")
                                          :foreground blue-green-color :font (myFont 14)) "align l"]
                                  [(info  light-grey-color)]
                                  [(label :text (htmling "<h2>Contact</h2>")
                                          :foreground blue-green-color :font (myFont 14)) "align l"]
                                  [(info  light-grey-color)]
                                  [(label :text (htmling "<h2>Links</h2>")
                                          :foreground blue-green-color :font (myFont 14)) "align l"] 
                                  [(label :text (htmling "<p align= \"justify\">http://trashpanda-team.ddns.net</p>")
                                          :foreground light-grey-color :font (myFont 14) ) "align l"]])]
                 (doall (map (fn [x] (.add mig x "align l")) (if (= faq nil)
                                                               contact-info (concat (asks-panel faq) contact-info))))
                 (.repaint mig) mig)]]))))


(def  login-panel
  (let [flogin (jarman.gui.gui-tools/text-input :placeholder "Login"
                                                :style [:class :input
                                                        :columns 20
                                                        :border (compound-border emp-border
                                                                                 (line-border :bottom 4 :color light-blue-color))
                                                        :halign :left
                                                        :bounds [100 150 200 30]])
        fpass (jarman.gui.gui-tools/password-input :placeholder "Password"
                                                   :style [:class :input
                                                           :columns 20
                                                           :border (compound-border emp-border
                                                                                    (line-border :bottom 4 :color light-blue-color))
                                                           :halign :left
                                                           :bounds [100 150 200 30]])]
    (mig-panel
     :constraints ["wrap 1" "[grow, center]" "30px[]0px"]
     :items [
             [(label :icon (stool/image-scale "resources/imgs/jarman-text.png" 10))]
             
             [(mig-panel
               :constraints ["" "[grow, fill]" "5px[]0px"]
               :items [
                       [(label :icon (stool/image-scale icon/user-blue1-64-png 40))]
                       [flogin]
                       [(label :border (empty-border :right 20))]
                       ])]

             [(mig-panel
               :constraints ["" "[grow, fill]" "5px[]0px"]
               :items [
                       [(label :icon (stool/image-scale icon/key-blue-64-png 40))]
                       [fpass]
                       [(label :border (empty-border :right 20))] ])]
             
             [(label :text "LOGIN" :background "#fff"
                     :foreground light-blue-color
                     :border (compound-border emp-border)
                     :listen [:mouse-entered (fn [e] (config! e :background "#deebf7" :foreground "#256599" :cursor :hand))
                              :mouse-exited  (fn [e] (config! e :background "#fff" :foreground light-blue-color))
                              :mouse-clicked (fn [e]  (if (authenticate-user (text flogin) (text fpass))
                                                        (do (config! flogin :border (compound-border emp-border
                                                                                                     (line-border :bottom 4 :color light-blue-color)))
                                                            (config! fpass :border (compound-border emp-border
                                                                                                    (line-border :bottom 4 :color light-blue-color))))
                                                        (do (config! flogin :border (compound-border emp-border
                                                                                                     (line-border :bottom 4 :color red-color)))
                                                            (config! fpass :border (compound-border emp-border
                                                                                                    (line-border :bottom 4 :color red-color))))))])]
             [(label :text " " :border
                     (empty-border :top 20 :left 860 )) "split 2"]
             [(mig-panel
               :constraints ["" "[grow, fill]" ""]
               :items [
                      
                       [(label :icon (stool/image-scale icon/refresh-connection-grey1-64-png 50)
                               :border (compound-border (empty-border :right 10 )))]
                       [(label :icon (stool/image-scale icon/settings-64-png 50)
                               :border (compound-border (empty-border :right 10 ))
                               :listen [:mouse-clicked (fn [e] (config! (to-frame e) :content config-generator-panel))])]
                       [(label :icon (stool/image-scale icon/I-grey-64-png 50)
                               :listen [:mouse-clicked (fn [e] (config! (to-frame e) :content info-panel))])]])]])))

;;;;;;;;;;;;;;
;;; frames ;;;
;;;;;;;;;;;;;;

(defn- frame-login []
  (frame :title "Jarman-login"
         :undecorated? false
         :resizable? false
         :minimum-size [1000 :by 760]
         :content login-panel))

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

;;(start)









(import 'jarman.test.Chooser)


(defn date-to-obj
  "Make shure, that your date in *data-format*

  Example: 
    (date-to-obj \"1998-10-10 05:11:22\") ;=> java.util.Date...."
  [^java.lang.String data-string]
  (.parse (SimpleDateFormat. "yyyy-MM-dd") data-string))


(def get-text-field 
  (fn [data] (text :text data
                   :background "#fff"
                   :columns 20
                   :border (color-border blue-green-color))))


(defn get-calendar [textf]
  (mig-panel
   :constraints ["wrap 1" "100px[grow, center]100px" "30px[]30px"]
   :items [[(Chooser/get_calendar textf (date-to-obj (text textf)))]]))


(defn- frame-picker []
  (frame :title "Jarman"
         :undecorated? false
         :resizable? false
         :minimum-size [1000 :by 760]
         :content (get-calendar (get-text-field "2019-10-10"))))

 (-> (doto (frame-picker) (.setLocationRelativeTo nil)) seesaw.core/pack! seesaw.core/show!)

