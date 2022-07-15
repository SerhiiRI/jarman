(ns jarman.gui.components.component-reciever
  (:require
   [clojure.pprint :refer [cl-format]]
   ;; ---
   [jarman.lang :refer :all]
   [jarman.org :refer :all]))

;;;;;;;;;;;;;;;;;;;;;;;
;;; HELPER MACROSES ;;;
;;;;;;;;;;;;;;;;;;;;;;;

(defmacro action-case [& body]
  (let [action-keywords (mapv first (partition-all 2 (rest body)))]
    `(case ~@body
       :help-events ~action-keywords)))

(defn component-socket
  [socket-id
   & {:keys [socket-tags
             socket-component
             socket-handler
             socket-storage
             socket-mutable-actions]
      :or {socket-mutable-actions #{}}}]
  (where
   ((component-storage (atom (rift socket-storage {})))
    (receiver-history (atom []))
    (receiver
     (fn [{:keys [action mutable debug] :as payload}]
       (swap! receiver-history conj payload)
       (if-let [invoker (socket-handler {:component socket-component :storage (deref component-storage) :payload payload})]
         (do (when true ;;debug
               (print-line 
                (cl-format nil ";; message ~A ~A ~:[~;MUTABLE~]~%~A" socket-id action mutable (pr-str payload))))
             (if-not (or mutable (contains? socket-mutable-actions action))
               (invoker {:component socket-component :storage (deref component-storage) :payload payload})
               (do (println "SWAP! MUTABLE!")
                (swap! component-storage
                  (fn [storage-snapshot]
                    (rift (invoker {:component socket-component :storage storage-snapshot :payload payload}) storage-snapshot))))))
         (throw (ex-info (format "bad action `%s` type" action) {:payload payload}))) true)))
   (fn [controller-action & {:as args}]
     (case controller-action
       :socket-id             socket-id
       :socket-component      socket-component
       :socket-storage        (deref component-storage)
       :socket-tags           socket-tags
       :socket-receiver       receiver
       :socket-clear-history  (reset! receiver-history [])
       :socket-get-history    (deref  receiver-history)
       :socket-handler-events (socket-handler {:payload {:action :help-events}})))))


(comment
  (require '[jarman.gui.components.table :as table])
  (require '[jarman.logic.metadata :as metadata])
  (require '[jarman.logic.connection :as db])
  (require '[jarman.logic.sql-tool :refer [select! update! delete! insert!]])
  
  (def table-socket
    (component-socket
     :some-socket-identificator
     :socket-tags [:table :refreshable]
     :socket-component t
     :socket-storage
     {:tables  [:jarman_user :jarman_profile]
      :columns [:jarman_user.login :jarman_user.password] ;; :jarman_user.first_name :jarman_user.last_name :jarman_profile.name
      :data rand-data
      :custom-renderers
      {:jarman_user.password
       (fn [^JTable table, ^Object value, isSelected, hasFocus, row, column]
         (cond->
             (c/label
              :font {:name "monospaced"}
              :h-text-position :right
              :text (if value "[X]" "[ ]"))
           isSelected (c/config! :background (.getSelectionBackground table))))
       :jarman_user.first_name
       (fn [^JTable table, ^Object value, isSelected, hasFocus, row, column]
         (cond->
             (c/label
              :font {:name "monospaced"}
              :h-text-position :right
              :text (format "%.2f $(buks)" value))
           isSelected (c/config! :background (.getSelectionBackground table))))}}
     :socket-handler
     (fn [{:keys [component storage payload]}]
       (case (get payload :action)
         :refresh
         (fn [{:keys [component storage payload]}]
           (apply table/change-model! component (apply concat (assoc storage :custom-renderers []))))
         :add-key
         (fn [{:keys [component storage payload]}]
           (let [f (get payload :field-qualified) 
                 new-storage (-> storage (update :columns conj f))]
             (apply table/change-model! component (apply concat new-storage))))))))
  
  (table-socket :socket-storage)
  (table-socket :socket-tags)
  (table-socket :socket-id)
  (table-socket :socket-component)

  (def dispatch! (table-socket :socket-reciever))
  (dispatch! {:action :refresh})
  (dispatch! {:action :add-key :field-qualified :jarman_user.first_name})


  
  ;; ===========
  ;; RANDOM DATA 
  (def rand-data
    (->> (cycle (vec (db/query (select!
                                {:limit 1
                                 :table_name :jarman_user
                                 :column [:#as_is :jarman_user.login :jarman_user.password :jarman_user.first_name :jarman_user.last_name :jarman_user.configuration :jarman_user.id_jarman_profile :jarman_profile.name :jarman_profile.configuration]
                                 :inner-join :jarman_user->jarman_profile}))))
         (take 100)
         (map (fn [x] (assoc x
                            :jarman_user.login (apply str (take 40 (repeatedly #(char (+ (rand 26) 65)))))
                            :jarman_user.password (rand-nth [true false])
                            :jarman_user.first_name (* (rand) (rand-int 10000)))))))

  ;; ==============
  ;; TABLE INSTANCE
  
  (def t
    (table/table
     :tables  [:jarman_user :jarman_profile]
     :columns [:jarman_user.login :jarman_user.password :jarman_user.first_name :jarman_user.last_name :jarman_profile.name]
     :data rand-data
     :custom-renderers
     {:jarman_user.password
      (fn [^JTable table, ^Object value, isSelected, hasFocus, row, column]
        (cond->
            (c/label
             :font {:name "monospaced"}
             :h-text-position :right
             :text (if value "[X]" "[ ]"))
          isSelected (c/config! :background (.getSelectionBackground table))))
      :jarman_user.first_name
      (fn [^JTable table, ^Object value, isSelected, hasFocus, row, column]
        (cond->
            (c/label
             :font {:name "monospaced"}
             :h-text-position :right
             :text (format "%.2f $(buks)" value))
          isSelected (c/config! :background (.getSelectionBackground table))))}))

  ;; debug frame
  (-> (doto (seesaw.core/frame
             :title "Jarman" 
             :content (seesaw.mig/mig-panel
                       :constraints ["wrap 1" "0px[grow, fill]0px" "0px[fill, top]0px"]
                       :items [[(c/scrollable t :hscroll :as-needed
                                              :vscroll :as-needed
                                              :border (seesaw.border/line-border :thickness 0 :color "#fff"))]]))
        (.setLocationRelativeTo nil)
        seesaw.core/pack!
        seesaw.core/show!))

  ;; =====================
  ;; SCROLLING TESTING 
  (scrollToRegionByRowColumnIndex! t  40 0)
  (scrollAndSelectSpecificRow!     t  50)
  (scrollAndSelectRowRange!        t  45 50)
  (scrollAndSelectRowIndexes!      t  [45 47 49 51])

  ;; =====================
  ;; MODEL UPDATE EXAMPLE 

  ;; config 1. 
  (change-model! t
                 :tables  [:jarman_user :jarman_profile]
                 :columns [:jarman_user.login :jarman_user.password :jarman_user.first_name :jarman_user.last_name :jarman_profile.name]
                 :data rand-data
                 :custom-renderers
                 {:jarman_user.password
                  (fn [^JTable table, ^Object value, isSelected, hasFocus, row, column]
                    (cond->
                        (c/label
                         :font {:name "monospaced"}
                         :h-text-position :right
                         :text (if value "[X]" "[ ]"))
                      isSelected (c/config! :background (.getSelectionBackground table))))
                  :jarman_user.first_name
                  (fn [^JTable table, ^Object value, isSelected, hasFocus, row, column]
                    (cond->
                        (c/label
                         :font {:name "monospaced"}
                         :h-text-position :right
                         :text (format "%.2f $(buks)" value))
                      isSelected (c/config! :background (.getSelectionBackground table))))})
  ;; config 2. 
  (change-model! t
                 :tables [:jarman_user :jarman_profile]
                 :columns [:jarman_user.login :jarman_user.first_name :jarman_user.last_name :jarman_profile.name]
                 :data    (vec
                           (db/query
                            (select!
                             {:limit 1
                              :table_name :jarman_user
                              :column [:#as_is :jarman_user.login :jarman_user.password :jarman_user.first_name :jarman_user.last_name :jarman_user.configuration :jarman_user.id_jarman_profile :jarman_profile.name :jarman_profile.configuration]
                              :inner-join :jarman_user->jarman_profile}))))
  ;; config 3. 
  (change-model! t
                 :tables [:jarman_user :jarman_profile]
                 :columns [:jarman_user.login :jarman_user.first_name :jarman_user.last_name :jarman_profile.name :jarman_user.configuration]
                 :data (vec
                        (db/query
                         (select!
                          {:table_name :jarman_user
                           :column [:#as_is :jarman_user.login :jarman_user.password :jarman_user.first_name :jarman_user.last_name :jarman_user.configuration :jarman_user.id_jarman_profile :jarman_profile.name :jarman_profile.configuration]
                           :inner-join :jarman_user->jarman_profile})))))
