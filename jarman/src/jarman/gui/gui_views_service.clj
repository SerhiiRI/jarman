(ns jarman.gui.gui-views-service
  (:use seesaw.core
        seesaw.mig
        seesaw.dev
        seesaw.border)
  (:require
   [seesaw.core :as c]
   [jarman.resource-lib.icon-library :as icon]
   [seesaw.util :as u]
   [jarman.tools.swing :as stool]
   [jarman.gui.gui-components :refer :all :as gcomp]))

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
      Function need stool/image-scale function for scalling icon"
  (fn [view-id title tab-tip bg-color size onclose onclick]
    (let [border "#fff"
          hsize (first size)
          vsize (last size)]
      (horizontal-panel
       :background bg-color
       :border (line-border :bottom 1 :color "#eee")
       :user-data {:title title :view-id view-id}
       :items [(label ;; tab title
                :text title
                :tip (if (nil? tab-tip) title tab-tip)
                :halign :center
                :border (empty-border :left 5 :right 5)
                :size [hsize :by vsize]
                :listen [:mouse-clicked onclick
                         :mouse-entered (fn [e] (c/config! e :cursor :hand))])
               (label ;; close icon
                :icon (jarman.tools.swing/image-scale icon/x-grey2-64-png 15)
                :halign :center
                :border (line-border :right 2 :color border)
                :size [vsize :by vsize]
                :listen [:mouse-entered (fn [e] (c/config! e
                                                         :cursor :hand
                                                         :icon (jarman.tools.swing/image-scale icon/x-blue1-64-png 15)))
                         :mouse-exited  (fn [e] (c/config! e
                                                         :icon (jarman.tools.swing/image-scale icon/x-grey2-64-png 15)))
                         :mouse-clicked onclose])]
       :listen [:mouse-entered (fn [e] (c/config! e :cursor :hand))]))))

(def recolor-tab
  (fn [tab color]
    (let [title (first  (c/config tab :items))
          close (second (c/config tab :items))]
      (c/config! title :background color)
      (c/config! close :background color))))

(def deactive-all-tabs
  (fn [service-data]
    (doall
     (map #(recolor-tab % (service-data :deactive-color)) (u/children (service-data :bar-space))))
    (service-data :repaint)))

(def set-component-to-view-space
  (fn [service-data view-id]
    ;; (println "Set view to space")
    (let [view-data (get @(service-data :views-storage) view-id)]
      ;; (println "view-data in set view " view-data)
      (if (nil? view-data)
        (do
          (c/config! (service-data :view-space) :items [[(label)]])
          (c/config! (service-data :view-space) :user-data :none))
        (do
          (let [component (get view-data :component)
                height    (get view-data :pref-height)
                scrollable? (get view-data :scrollable?)
                scrollable? false]
            ;; (println "component to set " component)
            ;; (c/config! (service-data :view-space) :items (if scrollable? [[(gcomp/scrollbox component)]] [[component]]))
            (c/config! (service-data :view-space) :items [[component]])
            ;; (if scrollable? (c/config! component :size [300 :by height]) (c/config! component :preferred-size [(.getWidth (c/config (service-data :view-space) :size))
            ;;                                                                                                :by
            ;;                                                                                                (.getHeight (c/config (service-data :view-space) :size))]))
            (c/config! (service-data :view-space) :user-data view-id)))))))


(def switch-tab
  (fn [service-data view-id]
    (fn [e]
      (println "Switch tab: " view-id)
      (let [tab (.getParent (to-widget e))]
        (deactive-all-tabs service-data)
        (recolor-tab tab (service-data :active-color))
        (set-component-to-view-space service-data view-id))
      (service-data :repaint))))

(def whos-active?
  (fn [service-data]
    (let [tabs (u/children (service-data :bar-space))
          active-color (seesaw.color/color (service-data :active-color))
          tabs (filter #(= active-color (c/config (first (c/config % :items)) :background)) tabs)]
      (if-not (empty? tabs) (first tabs))))) 

(def close-tab
  (fn [service-data view-id]
    (fn [e]
      (let [tab (.getParent (to-widget e))
            active-tab (whos-active? service-data)
            active-id (if-not (nil? active-tab) (get (c/config active-tab :user-data) :view-id))
            active? (= active-id (get (c/config tab :user-data) :view-id))
            close-view (fn [e]
                         (reset! (service-data :views-storage) (dissoc @(service-data :views-storage) view-id)) ;; Remove view from views-storage
                         (.remove (service-data :bar-space) tab) ;; Remove tab from tab-space
                         (cond active? ;; If view is active then swith on another and set color for active tab
                               (let [tabs (u/children (service-data :bar-space))
                                     tab (if-not (empty? tabs) (first tabs))
                                     view-id (if-not (nil? tab) (get (c/config tab :user-data) :view-id))]
                                 (if-not (nil? tab)
                                   (do
                                     (c/config! (service-data :bar-space) :background "#fff")
                                     (recolor-tab tab (service-data :active-color))
                                     (set-component-to-view-space service-data view-id))
                                   (do
                                     (c/config! (service-data :bar-space) :background "#eee")
                                     (set-component-to-view-space service-data nil))))))]
        (if (service-data :onClose) (close-view e))))))

(def set--view
  "Description
     Quick tab template for view component. Just set id, title and component.
     Next view will be added to @views with tab to open tabs bar.
   "
  (fn [service-data]
    (fn [view-id title tab-tip component-fn scrollable?]
      (if (contains? @(service-data :views-storage) view-id) ;; Swicht tab if exist in views-storage
        (let [view {view-id (get @(service-data :views-storage) view-id)}
              tabs (u/children (service-data :bar-space))
              tab (if (empty? tabs) nil (first (filter (fn [tab]
                                                         (= (get (c/config tab :user-data) :view-id) view-id))
                                                       tabs)))]
          (if-not (nil? tab) ;; Switch to exist tab and view
            (do
              (c/config! (service-data :bar-space) :background "#fff")
              (deactive-all-tabs service-data)
              (recolor-tab tab (service-data :active-color))
              (set-component-to-view-space service-data view-id))))
        (if (nil? (component-fn))
          (do (println "[ Warning ] gui-view-service/set--view: Fn building view return nil.") (label))
          (let [component (component-fn)  ;; Add new view to views-storage and switch to new view
                view {view-id {:component-fn component-fn
                               :component component
                               :view-id view-id
                               :title title
                               :scrollable? scrollable?
                               :pref-height (.getHeight (c/config component :preferred-size))}}
                tmp-store (atom [])]
          ;; (println "View id: " view-id)
            (swap! (service-data :views-storage) (fn [storage] (merge storage view)))
            (c/config! (service-data :view-space) :user-data view-id)
            (let [view-id (first (first view))
                  button-title  (get (second (first view)) :title)
                  tab-button (create--tab-button view-id button-title tab-tip "#eee" [100 25]
                                                 (close-tab service-data view-id)
                                                 (switch-tab service-data view-id))]
              (deactive-all-tabs service-data)
              (reset! tmp-store (cons tab-button (u/children (service-data :bar-space))))
              (doall (map #(.add (service-data :bar-space) %) @tmp-store))
              (set-component-to-view-space service-data view-id))
          ;; (println "Set and switch: " view-id)
            (switch-tab service-data view-id)))))))

(def reload-view
  "Description
     Quick tab template for view component. Just set id, title and component.
     Next view will be added to @views with tab to open tabs bar.
   "
  (fn [service-data]
    (fn [& viewid]
      (println "Reload views id" viewid)
      (let [view-id (if (empty? viewid) (c/config (service-data :view-space) :user-data) (first viewid))
            reload-fn  (get-in @(service-data :views-storage) [view-id :component-fn])
            new-comp (reload-fn)]
        (swap! (service-data :views-storage) (fn [old-stuff] (assoc-in old-stuff [view-id :pref-height] (.getHeight (c/config new-comp :preferred-size)))))
        (swap! (service-data :views-storage) (fn [old-stuff] (assoc-in old-stuff [view-id :component] new-comp)))
        ;; (println "Reload view " view-id " in space " (c/config (service-data :view-space) :user-data))
        (if (= (c/config (service-data :view-space) :user-data) view-id)
          (do
            ;; (println "View" {view-id (get @(service-data :views-storage) view-id)})
            (set-component-to-view-space service-data view-id)))))))



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
  [bar-space view-space & {:keys [onClose]
                           :or {onClose (fn [] true)}}]
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
                          component-fn
                          component
                          pref-height
                          tab-tip
                          scrollable?]
                   :or {view-id :none
                        title   ""
                        component-fn (fn [])
                        component nil
                        pref-height 0
                        tab-tip nil
                        scrollable? true}}]
      (cond
        (= action :set-view)        ((set--view service-data)
                                     (if (keyword? view-id) view-id (keyword view-id))
                                     title
                                     tab-tip
                                     component-fn
                                     scrollable?)  ;; return f which need args for new view
        (= action :reload)          (if (= view-id :none) (reload-view service-data) ((reload-view service-data) view-id))
        (= action :get-view)        ((get-view atom--views-storage) view-id)
        (= action :get-component)   (get-in @atom--views-storage [view-id :component])
        (= action :exist?)          ((exist atom--views-storage) view-id)
        (= action :get-my-view-id)  (doto (c/config (service-data :view-space) :user-data) (println))
        (= action :get-all-view)    @atom--views-storage
        (= action :get-all-view-ids)    (map #(first %) @atom--views-storage)
        (= action :get-view-sapce)  view-space
        (= action :get-bar-sapce)   bar-space
        (= action :clear)           (do
                                      (reset! atom--views-storage   {}))
        (= action :get-atom-storage) atom--views-storage))))
