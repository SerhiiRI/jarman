(ns jarman.gui.gui-views-service
  (:use seesaw.core
        seesaw.mig
        seesaw.dev
        seesaw.border)
  (:import (java.awt.event MouseEvent)
           (jiconfont.icons.google_material_design_icons GoogleMaterialDesignIcons))
  (:require
   [jarman.faces  :as face]
   [jarman.tools.lang :refer :all]
   [seesaw.core :as c]
   [jarman.resource-lib.icon-library :as icon]
   [jarman.gui.gui-style             :as gs]
   [jarman.faces                     :as face]
   [seesaw.util                      :as u]
   [jarman.tools.swing        :as stool]
   [jarman.gui.gui-components :as gcomp]
   [jarman.gui.gui-tools      :as gtool]
   [jarman.logic.state        :as state]
   [jarman.tools.org          :refer :all]))

;; ┌───────────────────────────┐
;; │                           │
;; │ Views service state fns   │
;; │                           │
;; └───────────────────────────┘


(defn- set-state-watcher
  "Description:
    Add watcher to component. If state was changed then rerender components in root using render-fn.
  Example:
    (set-state-watcher state! dispatch! container (fn [] component) [:path :to :state])"
  [state! dispatch! root render-fn watch-path id]
  (if (nil? (get-in (state!) watch-path))
    (dispatch! {:action :add-missing
                :path   watch-path}))
  (add-watch (state! :atom) id
   (fn [id-key state old-m new-m]
     (let [[left right same] (clojure.data/diff (get-in new-m watch-path) (get-in old-m watch-path))]
       (if (not (and (nil? left) (nil? right)))
         (let [root (if (fn? root) (root) root)]
           ;; (do
           ;;     (c/config! root :items (render-fn))
           ;;     ((:repaint (state!))))
           (try
             (do
               (c/config! root :items (render-fn))
               ((:repaint (state!))))
             (catch Exception e (println "\n" (str "Gui view service: Cannot rerender " id (.getMessage e))) ;; If exeption is nil object then is some prolem with nechw component inserting
                    ))))))))


(def state     (atom {}))
(def state!    (fn [& prop]
                 (cond (= :atom (first prop)) state
                       :else (deref state))))
(defn stop-watching []
  (remove-watch state :tabs-bar)
  (remove-watch state :view-space))

(defn soft-restar
  "Description:
    Reload app into old frame."
  []
  (try
    (do
      (print-line "\nSoft Restart")
      (stop-watching)
      (state/set-state :soft-restart true)
      ((state/state :startup)))
    (catch Exception e (print-line (str "Soft restart failed:\n" (.getMessage e) "\n")))))

(defn- action-handler
  "Description:
    Invoke fn using dispatch!.
  Example:
    (@state {:action :test})"
  [state action-m]
  (case (:action action-m)
    :add-missing  (assoc-in state (:path action-m) nil)
    :test         (do (println "\nTest:n") state)
    :add-view     (assoc-in state (:path action-m) (:value action-m))
    :active       (assoc-in state [:active] (:value action-m))
    :reload       (assoc-in state (:path action-m) (:value action-m))
    :rm-view      (assoc-in state [:views] (dissoc (:views state) (:key action-m)))))

(defn- create-disptcher [atom-var]
  (fn [action-m]
    (swap! atom-var (fn [state] (action-handler state action-m)))))
(def dispatch! (create-disptcher state))

(defn- initialize-repaint
  [atom-var]
  (swap! atom-var
         #(assoc % :repaint (fn []
                              (let [root (c/to-root (:space (state!)))]
                                (.repaint    root)
                                (.revalidate root))))))

(defn- create-state-template
  "Description:
     State for view service.
     :space is place where views will be rendering
     :bar   is place where tabs will be rendering
     :views is storage for views definitions
            {:view-id, :render-fn, :comp}
     :active is a map with definition of active view"
  [space bar]
  (reset! state {:space  space
                 :bar    bar
                 :views  {}
                 :active nil})
  (initialize-repaint state))


;; ┌───────────────────────────┐
;; │                           │
;; │ Views and tabs controller │
;; │                           │
;; └───────────────────────────┘


(defn- swap-view
  "Description:
     Return view object for view space."
  []
  ;;(println "\nView swapping")
  (let [views  (:views  (state!))
        active (:active (state!))]
    (let [swap-m (get views active)]
      (if-not (empty? swap-m)
        (if-not (nil? (:component swap-m))
          [[(:component swap-m)]]
          (if (fn? (:render-fn swap-m))
            (let [compo ((:render-fn swap-m))]
              (dispatch! {:action :reload
                          :path   [:views active :component]
                          :value  compo})
              [[compo]])))
        [[(c/label)]]))))

(declare rm-view)
(declare switch-view)

(defn- create--tab-button
  "Description:
      Buttons for changing opened views or functions in right part of app.
      If height in size will be different than 25, you probably should change it in mig-app-right-f. 
   Example:
      (create--tab-button state! dispatch! :my-view-k})"
  [state! dispatch! view-id-k]
  (let [border "#fff"
        hsize 120
        vsize 25
        m     (get-in (state!) [:views view-id-k])]
    (horizontal-panel
     :background (if (= view-id-k (:active (state!)))
                   face/c-tab-active
                   nil)
     :items [(label ;; tab title
              :text   (:title m)
              :tip    (:tip m)
              :halign :center
              :border (empty-border :left 5 :right 5)
              :size   [hsize :by vsize]
              :listen [:mouse-clicked (fn [e]
                                        (if (= (.getButton e) MouseEvent/BUTTON2)
                                          (rm-view view-id-k)
                                          (switch-view view-id-k)))
                       :mouse-entered (fn [e]
                                        (c/config! e :cursor :hand)
                                        ;; (c/move!   e :to-front)
                                        )])
             (label ;; close icon
              :icon (gs/icon GoogleMaterialDesignIcons/CLOSE face/c-icon 15)
              :halign :center
              :size [vsize :by vsize]
              :listen [:mouse-entered (fn [e](c/config! e :cursor :hand))
                       :mouse-clicked (fn [e] (rm-view view-id-k))])]
     :listen [:mouse-entered (fn [e] (c/config! e :cursor :hand))])))

(defn- rerender-tabs
  "Description:
     Return prepared tabs list objects for tabs-bar."
  []
  ;;(println "\nTabs rerendering")
  (let [tabs (doall
              (map (fn [[view-id-k]]
                     (create--tab-button state! dispatch! view-id-k))
                   (:views (state!))))]
    (gtool/join-mig-items (reverse tabs))))


(defn- rm-view [view-id-k]
  ;;(println "\nRemoving")
  (dispatch! {:action :rm-view
              :key    view-id-k})
  (let [to-active (rift (first (last (:views (state!)))) nil)]
    (dispatch! {:action :active
                :value  to-active}))
  (c/config! (:bar (state!)) :items (rerender-tabs)))

(defn- switch-view [view-id-k]
  (dispatch! {:action :active
              :value  view-id-k}))


(defn start
  "Description:
     Clear state atom and set new state template. You need to point space for views and space for tabs.
     Wathers will be rerenedring items in containers.
  Example:
     (gvs/start view-space tabs-bar)"
  [space bar]
  ;;(println "\nStart gvs")
  (create-state-template space bar)
  (set-state-watcher state! dispatch! space swap-view     [:active] :view-space)
  (set-state-watcher state! dispatch! bar   rerender-tabs [:active] :tabs-bar))


(defn add-view
  "Description:
     Add new view to gvs.
  Example:
     (gvs/add-view :uniqe-id \"Some tab title\" \"Some tab tip\" (fn [] (component-render)) true)"
  [& {:keys [view-id title tip render-fn scrollable?]
      :or {view-id :none
           title   ""
           tip     ""
           render-fn (fn [] (label))
           scrollable? false}}]
  (let [view-id (if (keyword? view-id) view-id (keyword view-id))]
    (if-not (= view-id (:active (state!)))
      (do
        (dispatch! {:action :add-view
                    :path   [:views view-id]
                    :value  {:title       title
                             :tip         tip
                             :render-fn   render-fn
                             :scrollable? scrollable?
                             :component   nil}})
        (dispatch! {:action :active
                    :value  view-id}))
      (do (println "Switch"))))
  ((:repaint (state!)))
  )


(defn reload-view
  "Description:
     Reload current view or view by view-id
  Example:
     (gvs/reload-view) ;; reload component and space
     (gvs/reload-view :my-view)  ;; reload component only
     (gvs/reload-view :my-view true)  ;; reload component and set to space"
  ([] (reload-view (:active (state!)) true))
  ([view-id] (reload-view view-id false))
  ([view-id display?]
   ;;(println "\nView reloading")
   (let [views  (:views  (state!))]
     (let [swap-m (get views view-id)]
       (if-not (empty? swap-m)
         (if (fn? (:render-fn swap-m))
             (let [compo ((:render-fn swap-m))]
               (dispatch! {:action :reload
                           :path   [:views view-id :component]
                           :value  compo})
               (if display? (c/config! (:space (state!)) :items [[compo]]))
               ((:repaint (state!))))))))))
