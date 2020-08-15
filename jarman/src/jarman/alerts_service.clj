(ns jarman.alerts-service
  (:use seesaw.core
        seesaw.border
        seesaw.dev
        seesaw.mig
        jarman.dev-tools)
  (:require [jarman.icon-library :as icon]
            [clojure.string :as string]))

(import java.awt.Color)

;; Panel for messages/alerts
(def messages (vertical-panel :items [(label)]))


(defn getWidth  [obj] (.width (.getSize obj)))
(defn getHeight [obj] (.height (.getSize obj)))
(defn getSize   [obj] (let [size (.getSize obj)] [(.width size) (.height size)]))

(defn get-elements-in-layered-by-id
  "Description:
     Set same id inside elements and found them all.
  Return:
     List of components/objects => (object[xyz] object(xyz))
  Example:
     (get-elements-in-layered-by-id event_or_some_root 'id_in_string')
  "
  [e ids] (let [id (keyword ids)
                select-id (keyword (string/join ["#" ids]))
                root (to-root (seesaw.core/to-widget e))
                selected (select root [select-id])
                outlist (if selected (filter (fn [i] (identical? (config i :id) id)) (seesaw.util/children (.getParent selected))) nil)]
            (if outlist outlist nil)))

(def alerts-rebounds-f
  "Description:
      This what should be resized with app window.
   "
  (fn [e] (let [list-of-alerts (get-elements-in-layered-by-id e "alert-box")
                bound-x (if list-of-alerts (- (getWidth (to-root (seesaw.core/to-widget e))) (getWidth (first list-of-alerts)) 20) 0)
                height 120]
            (if list-of-alerts (doseq [[n elem] (map-indexed #(vector %1 %2) list-of-alerts)]
                                 (config! elem :bounds [bound-x (- (- (getHeight (to-root (seesaw.core/to-widget e))) height) (* 80 n)) 300 75]))))))


;; Defrecord for elements in alerts-storage
;; [{:id 0 :data {:header "Some header" :body "Message" :btns [(btn1) (btn2)]} :component (label :text \"Hello world! \") :timelife 3}]
(defrecord Alert [id data component timelife visible])

;; alerts storage for message/alerts service
;; (def alerts-storage (ref []))

(defn refresh-alerts
  ([message-space alerts-storage] (do
                                      ;;  (println)
                                      ;;  (println)

                                      ;;  (println "Remove alerts")
                                       ;;  TODO: zamienic print na cos innego tak by sie wykonywalo przejscie po mapie
                                    (doall (map (fn [i] (if (identical? (config i :id) :alert-box) (.remove message-space i))) (seesaw.util/children message-space)))

                                      ;;  (println "Add alerts")
                                       ;;  TODO: zamienic print na cos innego tak by sie wykonywalo przejscie po mapie`
                                    (doall (map (fn [item] (if (= (get item :visible) true) (.add message-space (get item :component) (new Integer 15)))) @alerts-storage))

                                      ;;  (Thread/sleep 50)

                                      ;;  (println "Rebounds")
                                    (alerts-rebounds-f message-space)

                                      ;;  (println "Repainting")
                                    (.repaint message-space)))
  ([message-space alerts-storage id-to-remove] (do
                                      ;;  (println)
                                      ;;  (println)

                                      ;;  (println "Remove alerts")
                                       ;;  TODO: zamienic print na cos innego tak by sie wykonywalo przejscie po mapie
                                             (doall (map (fn [i] (if (or (identical? (config i :id) :alert-box)
                                                                         (identical? (config i :id) id-to-remove))
                                                                   (.remove message-space i))) (seesaw.util/children message-space)))

                                      ;;  (println "Add alerts")
                                       ;;  TODO: zamienic print na cos innego tak by sie wykonywalo przejscie po mapie`
                                             (doall (map (fn [item] (if (= (get item :visible) true) (.add message-space (get item :component) (new Integer 15)))) @alerts-storage))
                                             
                                      ;;  (Thread/sleep 50)

                                      ;;  (println "Rebounds")
                                             (alerts-rebounds-f message-space)

                                      ;;  (println "Repainting")
                                             (.repaint message-space))))

(defn rmAlert
  "Description:
       Remove message by id from storage
   Example: 
       (rmAlert 0 storage)
   "
  [id alerts-storage message-space] (do
                                      ;; (swap! alerts-storage #(vec (filter (fn [item] (not (= (get item :id) id))) %)))
                                      (swap! alerts-storage #(vec (map (fn [item] (if (= (get item :id) id) (assoc item :visible false) item)) %)))
                                      (refresh-alerts message-space alerts-storage)))


(defn rmAlertObj
  "Description:
       Remove message by object from storage
   Example: 
       (rmAlert 0 storage)
   "
  [obj alerts-storage message-space] (do
                                      ;;  Change visible to flase in correct record
                                       (swap! alerts-storage #(vec (map (fn [item] (if (identical? (get item :component) obj) (assoc item :visible false) item)) %)))
                                      ;;  (swap! alerts-storage #(vec (filter (fn [item] (not (identical? (get item :component) obj))) %)))
                                       (refresh-alerts message-space alerts-storage)))


(defn rmallAlert
  "Description:
       Remove all message fromstorage
   Example: 
       (rmallAlert storage)
   "
  [alerts-storage message-space] (do (reset! alerts-storage [])
                                     (refresh-alerts message-space alerts-storage)))


(defn addAlert
  [data component timelife alerts-storage]
  (let [id (+ (if (= (last @alerts-storage) nil) -1 (:id (last @alerts-storage))) 1)]
    (swap! alerts-storage #(conj % (Alert.
                                    id
                                    data
                                    component
                                    timelife
                                    true)))
    id))




(defn addAlertTimeout
  "Description:
       Add message to storage and return id of this alert
   Example: 
       (addAlert (label :text 'Hello world!') 3 storage) => {:id 0 :component (label :text 'Hello world!') :timelife 3 atorage}
   "
  [data component timelife alerts-storage message-space]
  (.start (Thread. (fn [] (let [id (addAlert data component timelife alerts-storage)]
                            (if (> timelife 0) (do
                                                 (Thread/sleep (* 1000 timelife))
                                                 (rmAlert id alerts-storage message-space))))))))

(defn history-alert
  [data]
  (let [bg-c "#fff"
        hover-c "#d9ecff"
        bord (empty-border :left 5)
        x 270
        y 30]
    (vertical-panel
     :background (new Color 0 0 0 0)
     :border (line-border :bottom 2 :color "#333")
     :items [(label
              :size [x :by y]
              :background bg-c
              :border bord
              :text (if (= (contains? data :header) true) (get data :header) "Information"))
             (label
              :size [x :by y]
              :background bg-c
              :border bord
              :text (if (= (contains? data :body) true) (get data :body) "Template of information..."))]
     :listen [:mouse-entered (fn [e] (let [children (seesaw.util/children (seesaw.core/to-widget e))]
                                       (do
                                         (config! e :cursor :hand)
                                         (doall (map (fn [item] (config! item :background hover-c)) children))
                                         )))
              :mouse-exited (fn [e] (let [children (seesaw.util/children (seesaw.core/to-widget e))]
                                      (do
                                        (config! e :cursor :default)
                                        (doall (map (fn [item] (config! item :background bg-c)) children))
                                        )))])))

(defn all-messages-window
  "Description:
      Function creating window with all active and hidden message. Argument message-space should be JLayeredPane.
   Example:
      (message-space alerts-storage)
   Needed:
      message-server-creator for show option
   "
  [message-space alerts-storage]
  (let [x 330
        y 400
        x-ico 30
        bg-c "#333"
        root-size (getSize (to-root message-space))
        center [(- (/ (first root-size) 2) (/ x 2)) (- (/ (last root-size) 2) (/ y 2))]
        items (vec (map (fn [item] (history-alert (get item :data))) @alerts-storage))
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
                                                             :listen [:mouse-clicked (fn [e] (refresh-alerts message-space alerts-storage :all-alerts))])])
                                             
                                             (flow-panel
                                              :border (empty-border :bottom 5)
                                              :size [x-ico :by x-ico]
                                              :items [(label :icon (image-scale icon/basket-blue1-64x64-png x-ico)
                                                             :halign :center
                                                             :size [x-ico :by x-ico]
                                                             :listen [:mouse-clicked (fn [e] (do 
                                                                                               (rmallAlert alerts-storage message-space)
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
        (= action :set)    (let [[data func timelife] param] (addAlertTimeout data (func data) timelife alerts-storage message-space))
        (= action :rm)     (let [[id] param] (rmAlert id alerts-storage message-space))
        (= action :rm-obj) (let [[obj] param] (rmAlertObj obj alerts-storage message-space))
        (= action :clear)  (rmallAlert alerts-storage message-space)
        (= action :count-all)     (count @alerts-storage)
        (= action :count-active)  (count (filter (fn [item] (if (= (get item :visible) true) item)) @alerts-storage))
        (= action :count-hidden)  (count (filter (fn [item] (if (= (get item :visible) false) item)) @alerts-storage))
        (= action :show)   (do
                             (refresh-alerts message-space alerts-storage :all-alerts)
                             (all-messages-window message-space alerts-storage))
        (= action :hide)   (refresh-alerts message-space alerts-storage :all-alerts)
        ))))

