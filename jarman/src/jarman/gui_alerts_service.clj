;: FIRST gui_tools.clj
;; PREV gui_tools.clj
;; NOW COMPILATION FILE 2/3
;; NEXT gui_app.clj
;; LAST gui_app.clj
(ns jarman.gui-alerts-service
  (:use seesaw.core
        seesaw.border
        seesaw.dev
        seesaw.mig
        jarman.dev-tools
        jarman.gui-tools)
  (:require [jarman.icon-library :as icon]
            [clojure.string :as string]))

(import java.awt.Color)


;; Defrecord for elements in alerts-storage
;; [{:id 0 :data {:header "Some header" :body "Message" :btns [(btn1) (btn2)]} :component (label :text \"Hello world! \") :timelife 3}]
(defrecord Alert [id data component timelife visible])


(defn message
  "Description:
      Template for messages. Using in JLayeredPanel.
      X icon remove and rebound displayed message.
      Loop icon display whole message.
   Example:
      (message alerts-controller) and next fn will be send to message-server-creator and then will looks like that (fn [{:header 'Hello' :body 'World!'}] (magic))
   Needed:
      Import jarman.gui-tools
      Function need build-ico for message icon
      Function need build-header for message header
      Function need build-body for message body
      Function need build-bottom-ico-btn for functional icon buttons on bottom
   "
  [alerts-controller]
  (fn [data]
    (let [font-c "#000"
          back-c "#fff"
          close [(build-bottom-ico-btn icon/loupe-blue1-64-png 23 (fn [e] (alert "Wiadomosc")))
                 (build-bottom-ico-btn icon/X-64x64-png 23 (fn [e] (let [to-del (.getParent (.getParent (seesaw.core/to-widget e)))] (alerts-controller :rm-obj to-del))))]]
      (mig-panel
       :id :alert-box
       :constraints ["wrap 1" "0px[fill, grow]0px" "0px[20]0px[30]0px[20]0px"]
       :background back-c
       :border nil
       :bounds [680 480 300 75]
       :items [[(flow-panel
                 :align :left
                 :background (new Color 0 0 0 0)
                 :items [(build-ico icon/alert-64-png)
                         (build-header (if (= (contains? data :header) true) (str-cutter (get data :header)) "Information"))])]
               [(build-body (if (= (contains? data :body) true) (str-cutter (get data :body)) "Template of information..."))]
               [(flow-panel
                 :align :right
                 :background (new Color 0 0 0 1)
                 :items (if (= (contains? data :btns) true) (concat close (get data :btns)) close))]]))))



(def alerts-rebounds-f
  "Description:
      Rebound message components and resize them.
   Needed:
      Function need get-elements-in-layered-by-id function for get all same element (here message boxs)
      Function need getHeight function for quick getting height size
   "
  (fn [e] (let [list-of-alerts (get-elements-in-layered-by-id e "alert-box")
                bound-x (if list-of-alerts (- (getWidth (to-root (seesaw.core/to-widget e))) (getWidth (first list-of-alerts)) 20) 0)
                height 120]
            (if list-of-alerts (doseq [[n elem] (map-indexed #(vector %1 %2) list-of-alerts)]
                                 (config! elem :bounds [bound-x (- (- (getHeight (to-root (seesaw.core/to-widget e))) height) (* 80 n)) 300 75]))))))



(defn refresh-alerts
  "Description:
      Functrion refreshing message on JLayeredPane (GUI).
   Example:
      ;; Just refresh messages
      (refresh-alerts message-space alerts-storage)
      ;; Refresh messages and remove element by ID
      (refresh-alerts message-space alerts-storage :all-alerts)
   Needed:
      Function need alerts-rebounds-f for set correct possition
   "
  ([message-space alerts-storage] (do
                                    ;;  Remove alerts
                                    (doall (map (fn [i] (if (identical? (config i :id) :alert-box) (.remove message-space i))) (seesaw.util/children message-space)))
                                    ;; Add alerts
                                    (doall (map (fn [item] (if (= (get item :visible) true) (.add message-space (get item :component) (new Integer 15)))) @alerts-storage))
                                    ;;  Rebounds message space
                                    (alerts-rebounds-f message-space)
                                    ;;  Repainting app
                                    (.repaint message-space)))
  ([message-space alerts-storage id-to-remove] (do
                                                 ;;  Remove alerts
                                                 (doall (map (fn [i] (if (or (identical? (config i :id) :alert-box)
                                                                             (identical? (config i :id) id-to-remove))
                                                                       (.remove message-space i))) (seesaw.util/children message-space)))
                                                 ;; Add alerts
                                                 (doall (map (fn [item] (if (= (get item :visible) true) (.add message-space (get item :component) (new Integer 15)))) @alerts-storage))
                                                 ;;  Rebounds message space
                                                 (alerts-rebounds-f message-space)
                                                 ;;  Repainting app
                                                 (.repaint message-space))))

(defn rmAlert
  "Description:
       Remove message by id from storage.
   Example: 
       (rmAlert 0 storage)
   "
  [id alerts-storage message-space] (do
                                      (swap! alerts-storage #(vec (map (fn [item] (if (= (get item :id) id) (assoc item :visible false) item)) %)))
                                      (refresh-alerts message-space alerts-storage)))


(defn rmAlertObj
  "Description:
       Set message inactive in storage. Function search same object to localizate correct message.
       Function refreshing GUI.
   Example: 
       (rmAlertObj object storage message-box)
   Needed:
      Function need refresh-alerts function to refresh GUI.
   "
  [obj alerts-storage message-space] (do
                                      ;;  Change visible to flase in correct record
                                       (swap! alerts-storage #(vec (map (fn [item] (if (identical? (get item :component) obj) (assoc item :visible false) item)) %)))
                                       (refresh-alerts message-space alerts-storage)))


(defn rmallAlert
  "Description:
      Remove all message from storage.
      Function refreshing GUI.
   Example: 
       (rmallAlert storage)
   Needed:
      Function need refresh-alerts function to refresh GUI.
   "
  [alerts-storage message-space] (do (reset! alerts-storage [])
                                     (refresh-alerts message-space alerts-storage)))


(defn addAlert
  "Description:
      Add new alert to storage and return ID.
   Example:
      (addAlert {:header 'Hello' :body 'World'} (some-widget) 5 alert-storage)
   "
  [data component timelife alerts-storage]
  (let [id (+ (if (= (last @alerts-storage) nil) -1 (:id (last @alerts-storage))) 1)]
    (swap! alerts-storage #(conj % (Alert. id data component timelife true)))
    id))



(defn addAlertTimeout
  "Description:
       Add message to storage and return id of this alert. Message will be inactive automaticly after timeout (in sec).
   Example: 
       (addAlertTimeout {:header 'Hello' :body 'World!'} (some-widget) 3 storage) 
       => in storage [... {:id 0 :data {:header 'Hello' :body 'World!'} :component (label :text 'Hello World!') :timelife 3 storage}]
   Needed:
      Function need addAlert and rmAlert functions to work.
   "
  [data component timelife alerts-storage message-space]
  (.start (Thread. (fn [] (let [id (addAlert data component timelife alerts-storage)]
                            (if (> timelife 0) (do
                                                 (Thread/sleep (* 1000 timelife))
                                                 (rmAlert id alerts-storage message-space))))))))

(defn history-alert
  "Description:
      Template for message in all-messeges-window.
   Example:
      (history-alert {:header 'Hello' :body 'World!'})
   "
  [data]
  (let [x 270
        y 30
        bg-c     "#fff"
        hover-c  "#d9ecff"
        bord     (empty-border :left 5)
        header   (if (= (contains? data :header) true) (get data :header) "Information")
        body     (if (= (contains? data :body) true) (get data :body) "Template of information...")
        label-fn (fn [text] (label :size [x :by y] :background bg-c :border bord :text text))]
    (vertical-panel
     :background (new Color 0 0 0 0)
     :border (line-border :bottom 2 :color "#333")
     :items [(label-fn header) (label-fn body)]
     :listen [:mouse-entered (fn [e] (let [children (seesaw.util/children (seesaw.core/to-widget e))]
                                       (do
                                         (config! e :cursor :hand)
                                         (doall (map (fn [item] (config! item :background hover-c)) children)))))
              :mouse-exited (fn [e] (let [children (seesaw.util/children (seesaw.core/to-widget e))]
                                      (do
                                        (config! e :cursor :default)
                                        (doall (map (fn [item] (config! item :background bg-c)) children)))))
              :mouse-clicked (fn [e] (alert (string/join ": " [header body])))])))

(defn all-messages-window
  "Description:
      Function creating window with all active and hidden message. Argument message-space should be JLayeredPane.
   Example:
      (message-space alerts-storage)
   Needed:
      Function need history-alert function for create message component inside.
      Function need rmallAlert function for clean history.
      Function need refresh-alerts function for GUI refresh with removing all-message-window element.
   "
  [message-space alerts-storage]
  (let [x     330
        y     400
        x-ico 30
        bg-c  "#333"
        root-size (getSize (to-root message-space))
        center    [(- (/ (first root-size) 2) (/ x 2)) (- (/ (last root-size) 2) (/ y 2))]
        items     (vec (map (fn [item] (history-alert (get item :data))) @alerts-storage))
        space-for-message (mig-panel
                           :constraints ["" "" ""]
                           :bounds [(first center) (last center) x y]
                           :id :all-alerts
                           :items [[(scrollable
                                     (vertical-panel
                                      :items items
                                      :background bg-c)
                                     :hscroll :never
                                     :vscroll :always
                                     :size [(- x x-ico 10) :by (- y 15)]
                                     :border (line-border :thickness 2 :color "#333")
                                     :listen [:mouse-motion (fn [e] (.repaint (select (to-root e) [:#all-alerts])))
                                              :mouse-wheel-moved (fn [e] (.repaint (select (to-root e) [:#all-alerts])))])]
                                   [(vertical-panel
                                     :size [x-ico :by y]
                                     :background (new Color 0 0 0 0)
                                     :items [(flow-panel
                                              :border (empty-border :bottom 5)
                                              :size [x-ico :by x-ico]
                                              :items [(label :icon (image-scale icon/x-blue1-64-png (- x-ico 5))
                                                             :halign :center
                                                             :size [x-ico :by x-ico]
                                                             :listen [:mouse-clicked (fn [e] 
                                                                                      ;;  refresh GUI with remove element with id :all-alerts
                                                                                       (refresh-alerts message-space alerts-storage :all-alerts))])])
                                             
                                             (flow-panel
                                              :border (empty-border :bottom 5)
                                              :size [x-ico :by x-ico]
                                              :items [(label :icon (image-scale icon/basket-blue1-64x64-png x-ico)
                                                             :halign :center
                                                             :size [x-ico :by x-ico]
                                                             :listen [:mouse-clicked (fn [e] (do 
                                                                                              ;;  remove all history
                                                                                               (rmallAlert alerts-storage message-space)
                                                                                              ;;  refresh GUI with remove element with id :all-alertsts
                                                                                               (refresh-alerts message-space alerts-storage :all-alerts)))])])
                                             ])]])
        ]
      (.add message-space space-for-message (new Integer 20))
    ))


(defn message-server-creator
  "Description:
      Alerts on JLayeredPane. Service creating storage for message and can controll GUI elements.
   Example: 
      (def alert-service (message-server-creator my-main-app-gui)) #Server is active
      (alert-service :set {:header 'Hello' :body 'World' :btns [(button :text 'Click me')]} message-fn 5)
   Needed:
      Function need addAletrTimeout
      Function need rmAlert
      Function need rmAlertObj
      Function need rmallAlert
      Function need refresh-alerts
      Function need all-message-window
   "
  [message-space]
  (let [alerts-storage (atom [])]
    (add-watch alerts-storage :refresh
               (fn [key atom old-state new-state]
                 (cond
                   (> (count @alerts-storage) 0) (refresh-alerts message-space alerts-storage)
                   :else (.repaint message-space))))
    (fn [action & param]
      (cond
        (= action :set)           (let [[data func timelife] param] (addAlertTimeout data (func data) timelife alerts-storage message-space))
        (= action :rm)            (let [[id] param] (rmAlert id alerts-storage message-space))
        (= action :rm-obj)        (let [[obj] param] (rmAlertObj obj alerts-storage message-space))
        (= action :clear)         (rmallAlert alerts-storage message-space)
        (= action :count-all)     (count @alerts-storage)
        (= action :count-active)  (count (filter (fn [item] (if (= (get item :visible) true) item)) @alerts-storage))
        (= action :count-hidden)  (count (filter (fn [item] (if (= (get item :visible) false) item)) @alerts-storage))
        (= action :show)          (do (refresh-alerts message-space alerts-storage :all-alerts) (all-messages-window message-space alerts-storage))
        (= action :hide)          (refresh-alerts message-space alerts-storage :all-alerts)))))

