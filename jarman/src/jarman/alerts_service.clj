(ns jarman.alerts-service
  (:use seesaw.core
        seesaw.border
        seesaw.dev
        seesaw.mig))

;; Panel for messages/alerts
(def messages (vertical-panel :items [(label)]))

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


(defn rmallAlert
  "Description:
       Remove all message fromstorage
   Example: 
       (rmallAlert storage)
   "
  [alerts-storage] (reset! alerts-storage []))

(defn countHeight
  "Description:
       Function get list of children and sums heights
   Example: 
       (countHeight list-of-children) => 42.0
   "
  [children]
  (cond
    (> (count children) 0)
    (+ (+ (reduce (fn [acc chil] (+ acc (.getHeight (.getSize chil)))) 34 children)))
    :else 0))

(defn alert-panel-resize
  [message-space] (let [margin 10
                        v-size (.getSize    (to-root message-space))
                        vw     (.getWidth   v-size)
                        vh     (.getHeight  v-size)
                        hpanel (countHeight (seesaw.util/children message-space))
                        wpanel (.getWidth (.preferredSize (first (seesaw.util/children message-space))))]
                    (config! message-space :bounds [(- vw wpanel (* margin 2)) (- vh hpanel margin) wpanel hpanel])))

(defn message-server-creator
  "Description:
      
   Example: 
       
   "
  [message-space]
  (let [alerts-storage (atom [])]
    (add-watch alerts-storage :refresh
               (fn [key atom old-state new-state]
                 (cond
                   (> (count @alerts-storage) 0) (do (print "Refresh alerts")
                                                     (config! message-space :items (map (fn [item] (get item :component)) @alerts-storage))
                                                     (Thread/sleep 50)
                                                     (alert-panel-resize message-space))
                   :else (do (print "No more message")
                             (config! message-space :items [] :bounds [(.getWidth  (.getSize    (to-root message-space)))
                                                                       (.getHeight (.getSize    (to-root message-space)))
                                                                       0 0])))))
   (fn [action & param]
     (cond
       (= action :set) (let [[component timelife] param] (addAlertTimeout component timelife alerts-storage))
       (= action :rm) (let [[id] param] (rmAlert id alerts-storage))
       (= action :rmall) (rmallAlert alerts-storage)
       (= action :count) (count @alerts-storage)))))


;; Create new message service
;; (def alerts (message-server-creator messages))

;; Add message
;; (alerts :set (label :text "Hello world!") 3)
;; (alerts :set (label :text "I am sexy and I'm know IT!") 1)
;; (alerts :set (label :text "Niezgadniesz nigdy Spock gdzie otwarli parasol )") 3)

;; (alerts :count)
;; (alerts :rm 0)
;; (alerts :count)
;; (alerts :rmall)


;; Show window with messages
;; (-> (doto (seesaw.core/frame
;;            :title "Messages window" :undecorated? false
;;            :minimum-size [600 :by 400]
;;            :content messages)
;;       (.setLocationRelativeTo nil) pack! show!))


;; (defn sleep
;;   [f coll interval]
;;   (doseq [x coll]
;;     (Thread/sleep interval)
;;     (f)))

;; (sleep (fn [] (println "Dupa")) (range 1) 1000)