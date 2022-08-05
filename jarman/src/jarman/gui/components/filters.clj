(ns jarman.gui.components.filters
  (:require
   ;; Seesaw
   [seesaw.core    :as c]
   [seesaw.border  :as b]
   ;; Jarman
   [jarman.lang           :refer :all]
   [jarman.interaction    :as interaction]
   [jarman.faces          :as face]
   [jarman.gui.core       :refer [satom register! cursor fe]]
   [jarman.gui.components.swing  :as swing]
   [jarman.gui.components.panels :as gui-panels]
   [jarman.gui.components.common :refer [text button button-stroke label] :as common]
   [jarman.gui.components.datetime :as datetime])
  (:import
   [java.util Date Calendar]
   [javax.swing SwingUtilities]
   [java.awt Point Toolkit]
   [jiconfont.icons.google_material_design_icons
    GoogleMaterialDesignIcons]))

(defn- populate-dispatch! [metafield components update-fn]
  (let [{:keys [type] :as comp-meta} metafield]
    (reduce
      (fn [acc-m event-keyword]
        (if (contains? comp-meta event-keyword)
          (update acc-m event-keyword #(update-fn %)) acc-m))
      metafield (get-in components [type :actions]))))

(defn in-filter [& {:keys [component-template on-change]}]
  ;; Short description
  ;; state {:component {:filter-001 #<TextField>
  ;;                    :filter-002 #<TextField>
  ;;                    ...}}
  ;; value {:filter-001 "text from first filter" <-.
  ;;        :filter-002 "text from the seconds"     \
  ;;        ...}  ^                                change-v-fn
  ;;              :                              (update filter value)
  ;;              '- dissoc-v-fn (remove value from)
  ;;
  (let [components (interaction/metacomponents-get)
        values (volatile! {})
        change-v-fn (fn [k v] (vswap! values assoc k v) (on-change (vec (vals @values))))
        dissoc-v-fn (fn [k] (vswap! values dissoc k) (on-change (vec (vals @values))))
        clean-v-fn (fn [] (vreset! values {}) (on-change nil))
        state (satom {:component {}})]
    (gui-panels/border-panel
      :south
      (gui-panels/flow-panel
        :align :trailing
        :items
        [(button :value "Dodaj" :on-click
           (fn [e]
             (let [new-filter-kwd (keyword (gensym "filter-"))
                   minus-click
                   (fn [e]
                     (swap! state update :component #(dissoc % new-filter-kwd))
                     (dissoc-v-fn new-filter-kwd))]
               (swap! state update :component
                 #(assoc % new-filter-kwd
                    (gui-panels/border-panel
                      :east (button-stroke
                              :icon (swing/icon GoogleMaterialDesignIcons/REMOVE face/c-icon)
                              :on-click minus-click)
                      :center (-> component-template
                                (populate-dispatch! components
                                  (fn [f] (fn [e] (change-v-fn new-filter-kwd (f e)))))
                                (interaction/metacomponent->component))))))))
         (button :value "Resetuj" :on-click
           (fn [e]
             (reset! state {:component {}})
             (clean-v-fn)))])
      :center
      (gui-panels/vertical-panel
        :items []
        :event-hook
        {:item-renderer
         {:atom (cursor [:component] state)
          :hook
          (fn [panel atom old new]
            (c/config! panel :items (vals new)))}}))))

(defn date-filter [& {:keys [on-change]}]
  (let [state (satom {:date-from nil :date-to nil})
        on-reset-click (fe [e] (reset! state {:date-from nil :date-to nil}))
        filter-layout
        (fn []
          [(gui-panels/border-panel
             :west (label :value "Data od" :border (swing/border {:l 5 :r 5}))
             :center (datetime/datetime-label :value nil :on-click
                       (fn [e] (swap! state assoc :date-from e))))
           (gui-panels/border-panel
             :west (label :value "Data do" :border (swing/border {:l 5 :r 5}))
             :center (datetime/datetime-label :value nil :on-click
                       (fn [e] (swap! state assoc :date-to e))))
           (gui-panels/flow-panel
             :align :trailing
             :items [(button :value "Resetuj" :on-click (fn [e] (on-reset-click e)))])])]
    (register! state :some
      (fn [a oas nas] (on-change nas)))
    (gui-panels/vertical-panel
      :event-hook
      {:reset-filter
       {:event on-reset-click
        :hook (fn [panel _] (c/config! panel :items (filter-layout)))}}
      :items (filter-layout))))

(comment
  (swing/quick-frame
    [(common/label-h3 :value "some stupid stuff to extend the frame")
     (in-filter
       :component-template
       {:type :jsgl-text
        :value "to feeling"
        :on-change (fn [e] (c/value e))}
       :on-change #(vector [:in :user.id %]))
     (date-filter
       :on-change (fn [{:keys [date-from date-to]}]
                    (if (and date-from date-to)
                      (println [:between :f1 [date-from date-to]])
                      (println "empty date filter"))))])


    (let [state (satom {})]
    (register! state :some
      (fn [_ _ values] (if-let [where (build-where (vals values))]
                        (println where))))
    (swing/quick-frame
      [(common/label-h3 :value "some stupid stuff to extend the frame")
       (interaction/metacomponent->component
         {:type :table-plugin-in-filter
          :component-template
          {:type :jsgl-text
           :value "to feeling"
           :on-change (fn [e] (c/value e))}
          :on-change (fn [filter-seq]
                       (when filter-seq
                         (swap! state assoc :x01 [:in :user.id filter-seq])))})
       (interaction/metacomponent->component
         {:type :table-plugin-date-filter
          :on-change (fn [{:keys [date-from date-to]}]
                       (when (and date-from date-to)
                         (swap! state assoc :x02 [:between :date date-from date-to])))})]))


  (let [state (satom {})]
    (register! state :some
      (fn [_ _ values] (if-let [where (build-where (vals values))]
                        (println where))))
    (swing/quick-frame
      [(common/label-h3 :value "some stupid stuff to extend the frame")
       (interaction/metacomponent->component
         {:type :table-plugin-in-filter
          :component-template
          {:type :jsgl-text
           :value "to feeling"
           :on-change (fn [e] (c/value e))}
          :on-change (fn [filter-seq]
                       (when (not-empty filter-seq)
                         (swap! state assoc :x01 [:in :user.id filter-seq])))})
       (interaction/metacomponent->component
         {:type :table-plugin-date-filter
          :on-change (fn [{:keys [date-from date-to]}]
                       (when (and date-from date-to)
                         (swap! state assoc :x02 [:between :date date-from date-to])))})]))

  (defn build-where [filter-list]
    (let [state (satom {})]
      (register! state :some
        (fn [_ _ values]
          (if-let [where (build-where (vals values))]
            (println where))))
      (some->> filter-list
        (reduce
          (fn [acc filter-sql]
            (if filter-sql
              (conj acc filter-sql)
              acc)) [])
        (not-empty)
        (into [:and])))))

(comment
  (let [state (satom {})]
    (register! state :some
      (fn [_ _ values]
        ;; if-let [where (build-where (vals values))]
        ;; (println where)
        (println values)
        ))
    (swing/quick-frame
      (into
        [(common/label-h3 :value "some stupid stuff to extend the frame")]
        (mapv interaction/metacomponent->component
          [{:type :jsgl-filter-in
            :component-template
            {:type :jsgl-text
             :value "to feeling"
             :on-change (fn [e] (c/value e))}
            :on-change (fn [filter-seq]
                         (when filter-seq
                           [:in :user.id filter-seq]))}
           {:type :jsgl-filter-between
            :on-change (fn [{:keys [date-from date-to]}]
                         (when (and date-from date-to)
                           [:between :date date-from date-to]))}])))))



