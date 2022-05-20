(ns plugins.table.table-socket
  (:require
   [seesaw.core :as c]
   [jarman.gui.components.component-reciever :refer [component-socket action-case]]
   [jarman.gui.components.database-table :as table]
   [jarman.logic.metadata :as metadata]
   [jarman.logic.connection :as db]
   [jarman.logic.sql-tool :refer [select! update! delete! insert!]])
  (:import
   [javax.swing JViewport JTable JPanel ListSelectionModel]
   [javax.swing.table TableCellRenderer]
   [java.awt Component Rectangle Point]))

(defn re-query [ssql-query]
  (vec (db/query (select! ssql-query))))
;; fixme:serhii: serhii add filter inside
(defn re-apply-model [storage]
  (let [query (get storage :socket-refreshable-query)]
    (-> storage (assoc :data (re-query query)))))

;; ACTIONS

(defn- refresh-action [{:keys [component storage payload]}]
  (let [final-table-definition (re-apply-model storage)]
    (apply table/change-model! component (apply concat final-table-definition))
    final-table-definition))

(defn- refresh-to-bottom [{:keys [component storage payload]}]
  (let [final-table-definition (re-apply-model storage)]
    (apply table/change-model! component (apply concat final-table-definition))
    (table/scrollToRegionByRowColumnIndex! component (dec (-> final-table-definition :data count)) 0)
    final-table-definition))

;; HANDLER

(defn- table-action-handler [{:keys [component storage payload]}]
  (action-case (get payload :action)
    :refresh refresh-action
    :refresh-to-bottom refresh-to-bottom
    :scroll-bottom
    (fn [{:keys [component storage payload]}]
      (table/scrollToRegionByRowColumnIndex! component (dec (-> storage :data count)) 0))
    :select-by-index
    (fn [{:keys [component storage payload]}]
      (if-let [indx (get payload :index nil)]
        (table/scrollAndSelectSpecificRow! component indx)))
    :add-column
    (fn [{:keys [component storage payload]}]
      (let [f (get payload :field-qualified) 
            storage (-> storage (update :columns conj f))
            final-table-definition (re-apply-model storage)]
        (apply table/change-model! component (apply concat final-table-definition))
        final-table-definition))
    :remove-column
    (fn [{:keys [component storage payload]}]
      (let [f (get payload :field-qualified)
            storage (-> storage (update :columns (fn [cols] (remove #(= % f) cols))))
            final-table-definition (re-apply-model storage)]
        (apply table/change-model! component (apply concat final-table-definition))
        final-table-definition))))

(defn create-table-socket [identificator table-component table-definition]
    (component-socket
     identificator
     :socket-tags [:table :refreshable :column]
     :socket-component table-component
     :socket-storage (-> table-definition (dissoc :type :data))
     :socket-handler table-action-handler
     :socket-mutable-actions #{:refresh :refresh-to-bottom :add-column :remove-column}))

(comment
  (def socket-table
    (create-table-socket
     :table-1 t
     {:tables  [:user :profile]
      :columns [:user.login :user.password] ;; :user.first_name :user.last_name :profile.name
      :socket-refreshable-query {:table_name :user
                                 :column [:#as_is :user.login :user.password :user.first_name :user.last_name :user.configuration :user.id_profile :profile.name :profile.configuration]
                                 :inner-join :user->profile}
      :data rand-data
      :custom-renderers
      {:user.password
       (fn [^JTable table, ^Object value, isSelected, hasFocus, row, column]
         (cond->
             (c/label
              :font {:name "monospaced"}
              :h-text-position :right
              :text (if value "[X]" "[ ]"))
           isSelected (c/config! :background (.getSelectionBackground table))))
       :user.first_name
       (fn [^JTable table, ^Object value, isSelected, hasFocus, row, column]
         (cond->
             (c/label
              :font {:name "monospaced"}
              :h-text-position :right
              :text (format "%.2f $(buks)" value))
           isSelected (c/config! :background (.getSelectionBackground table))))}}))

  (socket-table :socket-handler-events)
  (socket-table :socket-storage)
  (socket-table :socket-tags)
  (socket-table :socket-id)
  (socket-table :socket-component)

  (def dispatch! (socket-table :socket-receiver))
  (dispatch! {:action :refresh})
  (dispatch! {:action :refresh-to-bottom})
  (dispatch! {:action :add-column :field-qualified :user.first_name})
  (dispatch! {:action :remove-column
              :field-qualified :user.password
              :mutable true})


  (def t
    (table/database-table
     :tables  [:user :profile]
     :columns [:user.login :user.password :user.first_name :user.last_name :profile.name]
     :socket-refreshable-query {:table_name :user
                                :column [:#as_is :user.login :user.password :user.first_name :user.last_name :user.configuration :user.id_profile :profile.name :profile.configuration]
                                :inner-join :user->profile}
     :data rand-data
     :custom-renderers
     {:user.password
      (fn [^JTable table, ^Object value, isSelected, hasFocus, row, column]
        (cond->
            (c/label
             :font {:name "monospaced"}
             :h-text-position :right
             :text (if value "[X]" "[ ]"))
          isSelected (c/config! :background (.getSelectionBackground table))))
      :user.first_name
      (fn [^JTable table, ^Object value, isSelected, hasFocus, row, column]
        (cond->
            (c/label
             :font {:name "monospaced"}
             :h-text-position :right
             :text (format "%.2f $(buks)" value))
          isSelected (c/config! :background (.getSelectionBackground table))))}))

  ;; ===========
  ;; RANDOM DATA 
  (def rand-data
    (->> (cycle (vec (db/query (select!
                                {:limit 1
                                 :table_name :user
                                 :column [:#as_is :user.login :user.password :user.first_name :user.last_name :user.configuration :user.id_profile :profile.name :profile.configuration]
                                 :inner-join :user->profile}))))
         (take 100)
         (map (fn [x] (assoc x
                            :user.login (apply str (take 40 (repeatedly #(char (+ (rand 26) 65)))))
                            :user.password (rand-nth [true false])
                            :user.first_name (* (rand) (rand-int 10000)))))))

  ;; ==============
  ;; TABLE INSTANCE

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

  ;; config 1. 
  (change-model! t
                 :tables  [:user :profile]
                 :columns [:user.login :user.password :user.first_name :user.last_name :profile.name]
                 :data rand-data
                 :custom-renderers
                 {:user.password
                  (fn [^JTable table, ^Object value, isSelected, hasFocus, row, column]
                    (cond->
                        (c/label
                         :font {:name "monospaced"}
                         :h-text-position :right
                         :text (if value "[X]" "[ ]"))
                      isSelected (c/config! :background (.getSelectionBackground table))))
                  :user.first_name
                  (fn [^JTable table, ^Object value, isSelected, hasFocus, row, column]
                    (cond->
                        (c/label
                         :font {:name "monospaced"}
                         :h-text-position :right
                         :text (format "%.2f $(buks)" value))
                      isSelected (c/config! :background (.getSelectionBackground table))))})
  ;; config 2. 
  (change-model! t
                 :tables [:user :profile]
                 :columns [:user.login :user.first_name :user.last_name :profile.name]
                 :data    (vec
                           (db/query
                            (select!
                             {:limit 1
                              :table_name :user
                              :column [:#as_is :user.login :user.password :user.first_name :user.last_name :user.configuration :user.id_profile :profile.name :profile.configuration]
                              :inner-join :user->profile}))))
  ;; config 3. 
  (change-model! t
                 :tables [:user :profile]
                 :columns [:user.login :user.first_name :user.last_name :profile.name :user.configuration]
                 :data (vec
                        (db/query
                         (select!
                          {:table_name :user
                           :column [:#as_is :user.login :user.password :user.first_name :user.last_name :user.configuration :user.id_profile :profile.name :profile.configuration]
                           :inner-join :user->profile})))))
