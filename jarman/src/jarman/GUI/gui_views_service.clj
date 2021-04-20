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
  (fn [service-data]
    (doall
     (map
      (fn [tab]
        (let [title (first  (config tab :items))
              close (second (config tab :items))
              deactive-color "#ccc"]
          (config! title :background deactive-color)
          (config! close :background deactive-color)))
      (config (service-data :bar-space) :items)))
    (service-data :repaint)))

(def switch-tab
  (fn [service-data packed-view]
    (fn [e]
      (let [view (first packed-view)
            component (get (second view) :component)
            active-color "#eee"]
        (deactive-all-tabs service-data)
        (let [tab (.getParent (to-widget e))
              title (first  (config tab :items))
              close (second (config tab :items))]
          (config! title :background active-color)
          (config! close :background active-color))
        (config! (service-data :view-space) :items [component]))
      (service-data :repaint))))

(def close-tab
  (fn [service-data packed-view]
    (fn [e]
      (let [view (first packed-view)
            active-color "#eee"
            tab (.getParent (to-widget e))
            active? (= (first view) (get (config tab :user-data) :view-id))
            close-view (fn [e]
                         (reset! (service-data :views-storage) (dissoc @(service-data :views-storage) (first view))) ;; Remove view from views-storage
                         (.remove (service-data :bar-space) tab) ;; Remove tab from tab-space
                        ;;  (println "Views? " (count @(service-data :views-storage)))
                         (cond active? ;; If view is active then swith on another and set color for active tab
                               (let [tabs (config (service-data :bar-space) :items)
                                     tab (if-not (empty? tabs) (first tabs))
                                     title (if-not (nil? tab) (first  (config tab :items)))
                                     close (if-not (nil? tab) (second (config tab :items)))
                                     first-view-id (if-not (nil? tab) (get (config tab :user-data) :view-id))]
                                 (if-not (nil? tab)
                                   (do
                                    ;;  (println "Remove active - set first")
                                     (config! title :background active-color)
                                     (config! close :background active-color)
                                     (config! (service-data :view-space) :items [(get-in @(service-data :views-storage) [first-view-id :component])]))
                                   (do
                                    ;;  (println "Remove active - set label")
                                     (config! (service-data :view-space) :items [(label)])))))
                         (service-data :repaint))]
        (if (service-data :onClose) (close-view e))))))


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
          (let [view-id (first (first view))
                button-title  (get (second (first view)) :title)
                tab-button (create--tab-button view-id button-title "#eee" [100 25]
                                               (close-tab service-data view)
                                               (switch-tab service-data view))]
            (deactive-all-tabs service-data)
            (.add (service-data :bar-space) tab-button)
            (config! (service-data :view-space) :items [component])
            (service-data :repaint))
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
        service-data (fn [key] (get {:views-storage atom--views-storage
                                     :view-space   view-space
                                     :bar-space    bar-space
                                     :repaint      (.repaint (to-root view-space))
                                     :onClose      onClose} key))]
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
        (= action :get-view-sapce)  view-space
        (= action :get-bar-sapce)   bar-space
        (= action :clear)           (do
                                      (reset! atom--views-storage   {}))
        (= action :get-atom-storage) atom--views-storage))))


(@jarman.gui.gui-app/startup)
