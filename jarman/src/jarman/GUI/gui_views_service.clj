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
  (fn [view-id title bg-color size onclose onclick]
    (let [border "#fff"
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

(def recolor-tab
  (fn [tab color]
    (let [title (first  (config tab :items))
          close (second (config tab :items))]
      (config! title :background color)
      (config! close :background color))))

(def deactive-all-tabs
  (fn [service-data]
    (doall
     (map #(recolor-tab % (service-data :deactive-color)) (config (service-data :bar-space) :items)))
    (service-data :repaint)))

(def set-component-to-view-space
  (fn [service-data packed-view]
    (if (nil? packed-view)
      (do 
        (config! (service-data :view-space) :items [(label)]))
      (do
        (let [view-data (second (first packed-view))
              component (get view-data :component)
              height    (get view-data :pref-height)]
          (config! (service-data :view-space) :items [(scrollable component :border nil)])
          (config! component :size [300 :by height])
          )))))

(def switch-tab
  (fn [service-data packed-view]
    (fn [e]
      (let [view (first packed-view)
            component (get (second view) :component)
            tab (.getParent (to-widget e))]
        (deactive-all-tabs service-data)
        (recolor-tab tab (service-data :active-color))
        (set-component-to-view-space service-data packed-view))
      (service-data :repaint))))

(def whos-active?
  (fn [service-data]
    (let [tabs (config (service-data :bar-space) :items)
          active-color (seesaw.color/color (service-data :active-color))
          tabs (filter #(= active-color (config (first (config % :items)) :background)) tabs)]
      (if-not (empty? tabs) (first tabs)))))

(def close-tab
  (fn [service-data packed-view]
    (fn [e]
      (let [view (first packed-view)
            tab (.getParent (to-widget e))
            active-tab (whos-active? service-data)
            active-id (if-not (nil? active-tab) (get (config active-tab :user-data) :view-id))
            active? (= active-id (get (config tab :user-data) :view-id))
            close-view (fn [e]
                         (reset! (service-data :views-storage) (dissoc @(service-data :views-storage) (first view))) ;; Remove view from views-storage
                         (.remove (service-data :bar-space) tab) ;; Remove tab from tab-space
                         (cond active? ;; If view is active then swith on another and set color for active tab
                               (let [tabs (config (service-data :bar-space) :items)
                                     tab (if-not (empty? tabs) (first tabs))
                                     view-id (if-not (nil? tab) (get (config tab :user-data) :view-id))]
                                 (if-not (nil? tab)
                                   (do
                                     (recolor-tab tab (service-data :active-color))
                                     (set-component-to-view-space service-data {view-id (get @(service-data :views-storage) view-id)}))
                                   (do
                                     (set-component-to-view-space service-data nil))))))]
        (if (service-data :onClose) (close-view e))))))


(def set--view
  "Description
     Quick tab template for view component. Just set id, title and component.
     Next view will be added to @views with tab to open tabs bar.
   "
  (fn [service-data]
    (fn [view-id title component pref-height]
      (if (contains? @(service-data :views-storage) view-id) ;; Swicht tab if exist in views-storage
        (let [view {view-id (get @(service-data :views-storage) view-id)}
              tabs (config (service-data :bar-space) :items)
              tab (if (empty? tabs) nil (first (filter (fn [tab]
                                                         (= (get (config tab :user-data) :view-id) view-id))
                                                       tabs)))]
          (if-not (nil? tab) ;; Switch to exist tab and view
            (do
              (deactive-all-tabs service-data)
              (recolor-tab tab (service-data :active-color))
              (set-component-to-view-space service-data view))))

        (let [view {view-id {:component component ;; Add new view to views-storage and switch to new view
                             :view-id view-id
                             :title title
                             :pref-height pref-height}}]
          (swap! (service-data :views-storage) (fn [storage] (merge storage view)))
          (let [view-id (first (first view))
                button-title  (get (second (first view)) :title)
                tab-button (create--tab-button view-id button-title "#eee" [100 25]
                                               (close-tab service-data view)
                                               (switch-tab service-data view))]
            (deactive-all-tabs service-data)
            (.add (service-data :bar-space) tab-button)
            (set-component-to-view-space service-data view))
          (switch-tab service-data view))))))


(def get-view
  (fn [atom--views-storage]
    (fn [view-id]
      (get @atom--views-storage view-id))))


(def exist
  (fn [atom--views-storage]
    (fn [view-id]
      (contains? @atom--views-storage view-id))))

;; Create new service
(defn new-views-service
  "Description:
      Service create closed enviroment that controls views and tabs.
      Service performs scaling views using prefer size and static size.
      Argument :component should be an mig-panel.
      Just create service and use :set-view to adding modules.
   Example:
      (def my-serv (new-view-service bar-space views-space)) => nowy serwis widoków
      (my-serv :set-view :view-id \"test1\" :title \"Test 1\" :component (label :text \"Test 1\"))
   "
  [bar-space view-space & {:keys [onClose] :or {onClose (fn [] true)}}]
  (let [atom--views-storage   (atom {})
        service-data (fn [key] (get {:views-storage atom--views-storage
                                     :view-space   view-space
                                     :bar-space    bar-space
                                     :active-color   "#eee"
                                     :deactive-color "#ccc"
                                     :repaint      (do (.repaint (.getParent view-space))
                                                       (.revalidate (.getParent view-space)))
                                     :onClose      onClose} key))]
    (fn [action & {:keys [view-id
                          title
                          component
                          pref-height]
                   :or {view-id :none
                        title   ""
                        component nil
                        pref-height 0}}]
      (cond
        (= action :set-view)        ((set--view service-data) (if (keyword? view-id) view-id (keyword view-id)) title component (cond (and (= 0 pref-height) (nil? component))
                                                                                                                                      0
                                                                                                                                      (< 0 pref-height) (pref-height)
                                                                                                                                      :else (do
                                                                                                                                              (.getHeight (config component :preferred-size)))))  ;; return f which need args for new view
        (= action :get-view)        ((get-view atom--views-storage) view-id)
        (= action :get-component)   (get-in @atom--views-storage [view-id :component])
        (= action :exist?)          ((exist atom--views-storage) view-id)
        (= action :get-all-view)    @atom--views-storage
        (= action :get-all-view-ids)    (map #(first %) @atom--views-storage)
        (= action :get-view-sapce)  view-space
        (= action :get-bar-sapce)   bar-space
        (= action :clear)           (do
                                      (reset! atom--views-storage   {}))
        (= action :get-atom-storage) atom--views-storage))))