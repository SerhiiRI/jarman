(ns plugins.table.table-socket
  (:require
   [jarman.lang :refer :all]
   [jarman.org  :refer :all]
   [seesaw.core :as c]
   [jarman.gui.components.component-reciever :refer [component-socket action-case]]
   [jarman.gui.components.database-table :as table]
   [jarman.gui.components.swing :as swing]
   [jarman.logic.metadata :as metadata]
   [jarman.logic.connection :as db]
   [jarman.logic.sql-tool :refer [select! update! delete! insert!]])
  (:import
   [javax.swing JViewport JTable JPanel ListSelectionModel]
   [javax.swing.table TableCellRenderer]
   [java.awt Component Rectangle Point]))

(def ^:private autoloading-range 50)

(defn- build-where [filter-list]
  (some->> filter-list
    (reduce
      (fn [acc filter-sql]
        (if filter-sql
          (conj acc filter-sql)
          acc)) [])
    (not-empty)
    (into [:and])))

(defn re-query [ssql-query]
  (vec (db/query (select! ssql-query))))

(defn re-apply-model [storage]
  (let [query (assoc (get storage :socket-refreshable-query) :limit autoloading-range)]
    (-> storage (assoc :data (re-query query)
                  :query query))))

(defn- refresh-action [{:keys [component storage payload]}]
  (let [final-table-definition (re-apply-model storage)]
    (apply table/change-model! component (apply concat final-table-definition))
    final-table-definition))

(defn- refresh-to-bottom [{:keys [component storage payload]}]
  (let [final-table-definition (re-apply-model storage)]
    (apply table/change-model! component (apply concat final-table-definition))
    (table/scrollToRegionByRowColumnIndex! component (dec (-> final-table-definition :data count)) 0)
    final-table-definition))

(defn- load-more [{:keys [component storage payload]}]
  (if (some? (get-in storage [:socket-refreshable-query :limit]))
    (do (print-line "loading more elements...")
     (let [loading-range (get payload :loading-range autoloading-range)
           query (update (get storage :socket-refreshable-query)
                   :limit #(if (number? %)
                             [(+ % loading-range) loading-range]
                             (let [[offset _] %]
                               [(+ offset loading-range) loading-range])))
           new-state
           (-> storage
             (update :data into (re-query query))
             (assoc :socket-refreshable-query query))]
       (apply table/change-model! component (apply concat new-state))
       new-state))
    storage))

(defn- filter-results [{:keys [component storage payload]}]
  (if-let [filter-list (not-empty (filter some? (:filter-list payload)))]
    (let [where-statement (build-where filter-list)
          query
          (-> (get storage :socket-refreshable-query)
            (assoc :where where-statement)
            (assoc :limit [0 autoloading-range]))
          updated-storage
          (-> storage
            (assoc :data (re-query query))
            (assoc :socket-refreshable-query query))]
      (apply table/change-model! component (apply concat updated-storage))
      updated-storage)
    (do
      (print-line "Reset filters! Empty filters!")
      (let [query
            (-> (get storage :socket-refreshable-query)
              (dissoc :where)
              (assoc :limit [0 autoloading-range]))
            updated-storage
            (-> storage
              (assoc :data (re-query query))
              (assoc :socket-refreshable-query query))]
        (apply table/change-model! component (apply concat updated-storage))
        updated-storage))))

(defn- table-action-handler [{:keys [component storage payload]}]
  (action-case (get payload :action)
    :refresh refresh-action
    :refresh-to-bottom refresh-to-bottom
    :load-more load-more
    :filter filter-results
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
     :socket-mutable-actions #{:refresh :refresh-to-bottom :add-column
                               :remove-column :load-more :filter}))

;;; LOADING SIMULATION WITH BIG FUKCING TABLE
(comment
  (do
    (def t
     (table/database-table
       :tables [:registration :card :user]
       :columns
       [:registration.datetime
        :registration.direction
        :card.card_nr
        :user.last_name]
       :data (db/query
               (select!
                 {:table_name :registration,
                  :column
                  [:#as_is
                   :registration.id
                   :registration.datetime
                   :registration.direction
                   :card.id
                   :card.rfid
                   :card.card_nr
                   :user.id
                   :user.first_name
                   :user.last_name
                   :user.teta_nr]
                  :left-join [:registration->card
                              :registration->user]
                  :order [:registration.datetime :desc]
                  :limit [0 50]}))))
    
    (def socket-table
      (create-table-socket
        :table-1 t
        {:tables  [:registration :card :user]
         :columns [:registration.datetime
                   :registration.direction
                   :card.card_nr
                   :user.last_name]
         :socket-refreshable-query
         {:table_name :registration,
          :column
          [:#as_is
           :registration.id
           :registration.datetime
           :registration.direction
           :card.id
           :card.rfid
           :card.card_nr
           :user.id
           :user.first_name
           :user.last_name
           :user.teta_nr]
          :left-join [:registration->card
                      :registration->user]
          :order [:registration.datetime :desc]
          :limit [0 50]}
         :data []}))

    ;; (socket-table :socket-handler-events)
    ;; (socket-table :socket-storage)
    ;; (socket-table :socket-tags)
    ;; (socket-table :socket-id)
    ;; (socket-table :socket-component)

    (def dispatch! (socket-table :socket-receiver))
    ;; (dispatch! {:action :refresh})
    ;; (dispatch! {:action :refresh-to-bottom})
    ;; (dispatch! {:action :add-column :field-qualified :jarman_user.first_name})
    ;; (dispatch! {:action :remove-column
    ;;             :field-qualified :jarman_user.password
    ;;             :mutable true})
    ;; (dispatch! {:action :remove-column
    ;;             :field-qualified :jarman_user.password
    ;;             :mutable true})
    ;; (dispatch! {:action :load-more, :loading-range 50})
    ;; (dispatch! {:action :filter, :filter-list  [[:= :card.card_nr "4110622"]]})

    (def s ;; 
      (-> (c/scrollable t :hscroll :as-needed
            :vscroll :as-needed
            :border (seesaw.border/line-border :thickness 0 :color "#fff"))
        (swing/wrapp-adjustment-listener (fn [current max]
                                           (when (= max current)
                                             (dispatch! {:action :load-more, :loading-range 50}))))))
    
    ;; debug frame
    (-> (doto (seesaw.core/frame
                :title "Jarman" 
                :content (seesaw.mig/mig-panel
                           :constraints ["wrap 1" "0px[grow, fill]0px" "0px[fill, top]0px"]
                           :items [[s]]))
          (.setLocationRelativeTo nil)
          seesaw.core/pack!
          seesaw.core/show!)))

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

(comment
  (def socket-table
    (create-table-socket
     :table-1 t
     {:tables  [:jarman_user :jarman_profile]
      :columns [:jarman_user.login :jarman_user.password] ;; :jarman_user.first_name :jarman_user.last_name :jarman_profile.name
      :socket-refreshable-query {:table_name :jarman_user
                                 :column [:#as_is :jarman_user.login :jarman_user.password :jarman_user.first_name :jarman_user.last_name :jarman_user.configuration :jarman_user.id_jarman_profile :jarman_profile.name :jarman_profile.configuration]
                                 :inner-join :jarman_user->jarman_profile}
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
           isSelected (c/config! :background (.getSelectionBackground table))))}}))

  (socket-table :socket-handler-events)
  (socket-table :socket-storage)
  (socket-table :socket-tags)
  (socket-table :socket-id)
  (socket-table :socket-component)

  (def dispatch! (socket-table :socket-receiver))
  (dispatch! {:action :refresh})
  (dispatch! {:action :refresh-to-bottom})
  (dispatch! {:action :add-column :field-qualified :jarman_user.first_name})
  (dispatch! {:action :remove-column
              :field-qualified :jarman_user.password
              :mutable true})


  (def t
    (table/database-table
     :tables  [:jarman_user :jarman_profile]
     :columns [:jarman_user.login :jarman_user.password :jarman_user.first_name :jarman_user.last_name :jarman_profile.name]
     :socket-refreshable-query {:table_name :jarman_user
                                :column [:#as_is :jarman_user.login :jarman_user.password :jarman_user.first_name :jarman_user.last_name :jarman_user.configuration :jarman_user.id_jarman_profile :jarman_profile.name :jarman_profile.configuration]
                                :inner-join :jarman_user->jarman_profile}
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
