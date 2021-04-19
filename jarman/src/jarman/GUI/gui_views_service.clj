(ns jarman.gui.gui-views-service
  (:use seesaw.core
        seesaw.mig
        seesaw.dev
        seesaw.border)
  (:require [jarman.resource-lib.icon-library :as icon]
            [jarman.tools.swing :as stool]))

;; ┌───────────────────────────┐
;; │                           │
;; │ Views and tabs controller │
;; │                           │
;; └───────────────────────────┘

(def create--tab-button
  "Description:
      Buttons for changing opened views or functions in right part of app.
      If height in size will be different than 25, you probably should change it in mig-app-right-f. 
   Needed:
      Import jarman.dev-tools
      Function need stool/image-scale function for scalling icon
   "
  (fn [view-id title deactive-color size onclose onclick]
    (let [bg-color deactive-color
          border "#fff"
          hsize (first size)
          vsize (last size)]
      (horizontal-panel
       :background bg-color
       :user-data {:title title :view-id view-id}
       :items [(label ;; tab title
                :text title
                :halign :center
                :border (empty-border :left 5 :right 5)
                :size [hsize :by vsize]
                :listen [:mouse-clicked onclick
                         :mouse-entered (fn [e] (config! e :cursor :hand))])
               (label ;; close icon
                :icon (jarman.tools.swing/image-scale icon/x-grey2-64-png 15)
                :halign :center
                :border (line-border :right 2 :color border)
                :size [vsize :by vsize]
                :listen [:mouse-entered (fn [e] (config! e
                                                         :cursor :hand
                                                         :icon (jarman.tools.swing/image-scale icon/x-blue1-64-png 15)))
                         :mouse-exited  (fn [e] (config! e
                                                         :icon (jarman.tools.swing/image-scale icon/x-grey2-64-png 15)))
                         :mouse-clicked onclose])]
       :listen [:mouse-entered (fn [e] (config! e :cursor :hand))]))))


(def deactive-all-tabs
  (fn [service-data deactive-color]
    (doall
     (map
      (fn [tab]
        (let [title (first  (config tab :items))
              close (second (config tab :items))]
          (config! title :background deactive-color)
          (config! close :background deactive-color)))
      (config (service-data :bar-space) :items)))
    (service-data :repaint)))

(def active-by-view-id
  (fn [service-data active-color view]
    (let [view-id (first (first view))]
      (doall
       (println "Active:" view-id)
       (doall
        (map
         (fn [tab]
           (let [title (first  (config tab :items))
                 close (second (config tab :items))
                 u-data (config tab :user-data)]
             (cond (= (get u-data :view-id) view-id)
                   (do
                     (config! title :background active-color)
                     (config! close :background active-color)))))
         (config (service-data :bar-space) :items)))))
    (service-data :repaint)))

;; Switch active tab
(def switch-tab
  (fn [service-data view]
    (deactive-all-tabs service-data "#ccc")
    (cond (nil? view)
            ;; If view is nill
          (cond (nil? @(service-data :last-active))
                  ;; if last active is empty
                (cond (empty? @(service-data :views-storage))
                        ;; if storage is empty
                      (do
                        (config! (service-data :view-space) :items [(label)])
                        (reset! (service-data :active-tab) nil)
                        (reset! (service-data :last-active) nil))
                        ;; if stroeage is not empty
                      :else (do
                              (switch-tab service-data (first @(service-data :views-storage)))))
                              (reset! (service-data :last-active) nil)
                              (reset! (service-data :active-tab)  nil)
                  ;; if last active is not empty
                :else (do
                        (reset! (service-data :active-tab) nil)
                        (switch-tab service-data @(service-data :last-active))))
            ;; if view is not empty
          :else (do
                  (if (= @(service-data :last-active) view) 
                    (reset! @(service-data :last-active) nil) 
                    (reset! (service-data :last-active) @(service-data :active-tab)))
                  (reset! (service-data :active-tab) view)
                  (config! (service-data :view-space) :items [(get (second (first @(service-data :active-tab))) :component)])
                  ((service-data :update-bar) service-data)))
                
    ;; (.repaint (to-root (service-data :bar-space)))
    ))


(def close-tab
  (fn [service-data view-id]
    (fn [e]
      (let [tab (.getParent (seesaw.core/to-widget e))
            active? (get (config tab :user-data) :active)
            close-view (fn [e]
                         (reset! (service-data :views-storage) (dissoc @(service-data :views-storage) view-id)) ;; Remove view from views-storage
                         (if active? ;; If view is active then swith on another
                           (do 
                             (config! (service-data :view-space) :items [(label)])
                             (switch-tab service-data @(service-data :last-active)))
                           (do
                             (reset! (service-data :last-active) nil)
                             ((service-data :update-bar) service-data))))]
        (if (service-data :onClose) (close-view e))))))


(def in-list? (fn [coll key] (not (empty? (filter #(= key %) coll)))))
(def update-tabs-on-bar-space
  (fn [service-data]
    (service-data :repaint)
    (let [list-of-tabs (config (service-data :bar-space) :items)
          list-of-view-id-in-tabs (map (fn [tab] (get (config tab :user-data) :view-id)) list-of-tabs)]
      (cond
        (< (count list-of-tabs) (count @(service-data :views-storage)))
        (do ;; Add new tab which will be linking to created view
          (let [views-list-to-add (filter #(not (in-list? list-of-view-id-in-tabs (first %))) @(service-data :views-storage))]
            (doall
             (map (fn [view]
                    (let [view-id (first view)
                          button-title  (get (second view) :title)
                          tab-button (create--tab-button view-id button-title "#ccc" [100 25]
                                                         (close-tab service-data view-id)
                                                         (fn [e] (switch-tab service-data [view])))]
                      (.add (service-data :bar-space) tab-button)
                      (active-by-view-id service-data "#eee" view)
                      (service-data :repaint)))
                  views-list-to-add))))

        (> (count list-of-tabs) (count @(service-data :views-storage)))
        (do ;; Close tab which linked to removed view
          (let [get-view-id (fn [t] (get (config t :user-data) :view-id))
                flow-if-not-in-storage (fn [ta] (= (get @(service-data :views-storage) (get-view-id ta)) nil))
                to-remove (filter (fn [tab] (flow-if-not-in-storage tab)) list-of-tabs)]
            (do
             (doall (map #(.remove (service-data :bar-space) %) to-remove))
             (cond (not (nil? @(service-data :last-active))) (active-by-view-id service-data "#eee" @(service-data :last-active)))
             (service-data :repaint))))))))


;; Create top bar for tabs linking to views
;; (def update--tabs-bar
;;   "Description
;;     This function update bar with tabs who linking to open views.
;;    "
;;   (fn [service-data]
;;     ;; (println "Update tabs")
;;     (config! (service-data :bar-space) :items
;;              (if-not (nil? @(service-data :active-tab))
;;                (vec
;;                 (map
;;                  (fn [view]
;;                    (let [view-id (first view)
;;                          button-title  (get (second view) :title)
;;                          active-view-id (first (first @(service-data :active-tab)))
;;                          active? (identical? view-id active-view-id)]
;;                      (create--tab-button button-title active? [100 25]
;;                                          (close-tab service-data view-id)
;;                                          (fn [e] (switch-tab service-data [view])))))
;;                  @(service-data :views-storage)))
;;                [(label)]))))



(def set--view
  "Description
     Quick tab template for view component. Just set id, title and component.
     Next view will be added to @views with tab to open tabs bar.
   "
  (fn [service-data]
    (fn [view-id title component]
      ;; (println "Set view")
      (if (contains? @(service-data :views-storage) view-id)
        (let [view {view-id (get @(service-data :views-storage) view-id)}] ;; Swicht tab if exist in views-storage
          (switch-tab service-data view))

        (let [view {view-id {:component component ;; Add new view to views-storage and switch to new view
                             :view-id view-id
                             :title title}}]
          (swap! (service-data :views-storage) (fn [storage] (merge storage view)))
          ;; (println "View Added - next switch")
          ()
          (switch-tab service-data view))))))


(def get-view
  (fn [atom--views-storage]
    (fn [view-id]
      (get @atom--views-storage view-id))))

(def get-component
  (fn [atom--views-storage]
    (fn [view-id]
      (get-in @atom--views-storage [view-id :component]))))

(def exist
  (fn [atom--views-storage]
    (fn [view-id]
      (contains? @atom--views-storage view-id))))



;; Create new service
(defn new-views-service
  [bar-space view-space & {:keys [onClose] :or {onClose (fn [] true)}}]
  (let [atom--views-storage   (atom {})
        atom--active-tab      (atom nil)
        atom--last-active-tab (atom nil)
        service-data (fn [key] (get {:views-storage atom--views-storage
                                     :active-tab   atom--active-tab
                                     :last-active  atom--last-active-tab
                                     :view-space   view-space
                                     :bar-space    bar-space
                                     :root         (.repaint (to-root view-space))
                                    ;;  :update-bar       update--tabs-bar
                                     :update-bar update-tabs-on-bar-space
                                     :onClose      onClose} key))]
    ;; (run-views-supervisior service-data)
    ;; (update--tabs-bar service-data)
    ;; (update-tabs-on-bar-space service-data)
    (fn [action & {:keys [view-id
                          title
                          component]
                   :or {view-id :none
                        title   ""
                        component nil}}]
      (cond
        (= action :set-view)        ((set--view service-data) (if (keyword? view-id) view-id (keyword view-id)) title component)  ;; return f which need args for new view
        (= action :get-view)        ((get-view atom--views-storage) view-id)
        (= action :get-component)   ((get-component atom--views-storage) view-id)
        (= action :exist?)          ((exist atom--views-storage) view-id)
        (= action :get-all-view)    @atom--views-storage
        (= action :get-active)      @atom--active-tab
        (= action :get-last)        @atom--last-active-tab
        (= action :get-view-sapce)  view-space
        (= action :get-bar-sapce)   bar-space
        (= action :clear)           (do
                                      (reset! atom--last-active-tab nil)
                                      (reset! atom--active-tab      nil)
                                      (reset! atom--views-storage   {}))
        (= action :get-atom-storage) atom--views-storage))))


(@jarman.gui.gui-app/startup)
