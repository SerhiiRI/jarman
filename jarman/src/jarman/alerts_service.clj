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
(def alerts-storage (ref []))


(defn addAlert
  "Description:
       Add message to alerts-storage and return id of this alert
   Example: 
       (addAlert (label :text \"Hello world!\") 3) => {:id 0 :component (label :text \"Hello world!\") :timelife 3}
   "
  [component timelife]
  (let [id (+ (if (= (last @alerts-storage) nil) -1 (:id (last @alerts-storage))) 1)]
    (dosync (alter alerts-storage #(conj % (Alert.
                                           id
                                           component
                                           timelife))))
    id))

(defn rmAlert
  "Description:
       Remove message by id from alerts-storage
   Example: 
       (rmAlert 0)
   "
  [id] (dosync (ref-set alerts-storage (vec (filter
                                             (fn [item] (not (= (get item :id) id)))
                                             @alerts-storage)))))



(defn rmallAlert
  "Description:
       Remove all message from alerts-storage
   Example: 
       (rmallAlert)
   "
  [] (dosync (ref-set alerts-storage [])))



(defn message-server-creator
  "Description:
       Create srevice for message/alerts menagment and display.
       Create service (def name-of-service (function-for-service-create space-for-alerts-vertical-panel))
       Storage for message is a ref list with defrecords Alert
   Example: 
       (def alerts (message-server-creator messages))  =>  create alerts service
       (alerts :update)                                =>  display to panel message from storage
       (alerts :set (label :text \"Hello world!\") 3)  =>  add to storage new message 
       (alerts :rm 0)                                  =>  delete message with id 0
       (alerts :rmall)                                 =>  remove all message
   "
  [message-space]
  (fn [action & param]
    (cond
      (= action :update) (cond
                           (> (count @alerts-storage) 0) (config! message-space :items (map (fn [item] (get item :component)) @alerts-storage))
                           :else (config! message-space :items [(label :text "No more message.")]))
      (= action :set) (let [[component timelife] param] (do
                                                          (addAlert component timelife)
                                                          (config! message-space :items (map (fn [item] (get item :component)) @alerts-storage))
                                                          ))
      (= action :rm) (let [[id] param] (do
                                         (rmAlert id)
                                         (cond
                                           (> (count @alerts-storage) 0) (config! message-space :items (map (fn [item] (get item :component)) @alerts-storage))
                                           :else (config! message-space :items [(label :text "No more message.")]))))
      (= action :rmall) (let [] (do
                                  (rmallAlert)
                                  (config! message-space :items [(label :text "No more message.")])) [])
      (= action :count) (let [] (count @alerts-storage)))))


;; Create new message service
;; (def alerts (message-server-creator messages))

;; Display messages
;; (alerts :update)

;; Add message
;; (alerts :set (label :text "Hello world!") 3)
;; (alerts :set (label :text "I am sexy and I'm know IT!") 3)
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