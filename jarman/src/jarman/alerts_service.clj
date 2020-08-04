(ns jarman.alerts-service
  (:use seesaw.core
        seesaw.border
        seesaw.dev
        seesaw.mig)
  (:require [clojure.string :as string]))

;; Panel for messages/alerts
(def messages (vertical-panel :items [(label)]))


(defn getWidth [obj] (.width (.getSize obj)))
(defn getHeight [obj] (.height (.getSize obj)))


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
                                 (config! elem :bounds [bound-x (- (- (getHeight (to-root (seesaw.core/to-widget e))) height) (* 80 n)) 300 75]))
                ))))


;; Defrecord for elements in alerts-storage
;; [{:id 0 :component (label :text \"Hello world! \") :timelife 3}]
(defrecord Alert [id component timelife])

;; alerts storage for message/alerts service
;; (def alerts-storage (ref []))


(defn rmAlert
  "Description:
       Remove message by id from storage
   Example: 
       (rmAlert 0 storage)
   "
  [id alerts-storage] (swap! alerts-storage #(vec (filter (fn [item] (not (= (get item :id) id))) %))))


(defn rmAlertObj
  "Description:
       Remove message by object from storage
   Example: 
       (rmAlert 0 storage)
   "
  [obj alerts-storage] (swap! alerts-storage #(vec (filter (fn [item] (not (identical? (get item :component) obj))) %))))


(defn rmallAlert
  "Description:
       Remove all message fromstorage
   Example: 
       (rmallAlert storage)
   "
  [alerts-storage] (reset! alerts-storage []))


(defn addAlert
  [component timelife alerts-storage]
  (let [id (+ (if (= (last @alerts-storage) nil) -1 (:id (last @alerts-storage))) 1)]
    (swap! alerts-storage #(conj % (Alert.
                                    id
                                    component
                                    timelife)))
    id))



(defn addAlertTimeout
  "Description:
       Add message to storage and return id of this alert
   Example: 
       (addAlert (label :text \"Hello world!\") 3 storage) => {:id 0 :component (label :text \"Hello world!\") :timelife 3 atorage}
   "
  [component timelife alerts-storage]
  (.start (Thread. (fn [] (let [id (addAlert component timelife alerts-storage)]
                            (if (> timelife 0) (do
                                                 (Thread/sleep (* 1000 timelife))
                                                 (rmAlert id alerts-storage))))))))




(def refresh-alerts
  (fn [message-space alerts-storage] (do
                                       (println)
                                       (println)

                                       (println "Remove alerts")
                                       (println (map (fn [i] (if (identical? (config i :id) :alert-box) (.remove message-space i))) (seesaw.util/children message-space)))

                                       (println "Add alerts")
                                       (print (map (fn [item] (.add message-space (get item :component) (new Integer 15))) @alerts-storage))

                                       (Thread/sleep 50)

                                       (println "Rebounds")
                                       (alerts-rebounds-f message-space)

                                       (println "Repainting")
                                       (.repaint message-space))))

(defn message-server-creator
  "Description:
      
   Example: 
       
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
       (= action :set) (let [[component timelife] param] (addAlertTimeout component timelife alerts-storage))
       (= action :rm) (let [[id] param] (do (rmAlert id alerts-storage) (refresh-alerts message-space alerts-storage)))
       (= action :rm-obj) (let [[obj] param] (do (rmAlertObj obj alerts-storage) (refresh-alerts message-space alerts-storage)))
       (= action :rmall) (do (rmallAlert alerts-storage) (refresh-alerts message-space alerts-storage))
       (= action :count) (count @alerts-storage)))))

