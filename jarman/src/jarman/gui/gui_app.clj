(ns jarman.gui.gui-app
  (:use seesaw.dev
        seesaw.style
        seesaw.mig
        seesaw.font)
  (:import (javax.swing JLayeredPane JLabel JTable JComboBox DefaultCellEditor JCheckBox)
           (javax.swing.table TableCellRenderer TableColumn)
           (java.awt.event MouseEvent)
           (jarman.jarmanjcomp DateTime)
           (java.awt Color Component)
           (java.awt Dimension))
  (:require [jarman.tools.lang :refer :all]
            [clojure.string    :as string]
            [seesaw.core       :as c]
            [seesaw.util       :as u]
            [seesaw.border     :as b]
            ;; resource 
            [jarman.resource-lib.icon-library :as icon]
            [clojure.pprint :as pp]
            ;; logic
            [jarman.config.config-manager    :as cm]
            [jarman.config.dot-jarman        :as dot-jarman]
            [jarman.config.dot-jarman-param  :as dot-jarman-param]
            [jarman.gui.gui-views-service    :as vs]
            [jarman.gui.gui-alerts-service   :as gas]
            [jarman.gui.gui-components       :as gcomp]
            [jarman.gui.gui-tools            :as gtool]
            ;; deverloper tools 
            [jarman.config.init              :as iinit]
            [jarman.logic.state              :as state]
            [jarman.gui.gui-seed             :as gseed]
            [jarman.logic.session            :as session]
            [jarman.logic.view-manager       :as vmg]
            [jarman.gui.gui-dbvisualizer     :as dbv]
            [jarman.gui.gui-config-generator :as cg]
            [jarman.gui.popup                :as popup]
            [jarman.gui.gui-main-menu        :as menu]
            [jarman.managment.data           :as managment-data]
            [jarman.plugin.plugin-loader :refer [do-load-plugins]]
            [jarman.config.dot-jarman :refer [dot-jarman-load]]))

;; ┌──────────────────────────┐
;; │                          │
;; │ App layout and GUI build │
;; │                          │
;; └──────────────────────────┘

(def jarmanapp--main-view-space
  "Description:
      Vertical layout for tabs and table on right part of app. 
      Tabs are inside horizontal panel on top.
   Example: 
      tabs  -> mig vector with elements -> [(tab1) (tab2) (tab3)]
      array -> table like rows and columns -> [(table)]  
      (jarmanapp--main-view-space [(tab-btn 'Tab 1' true) (tab-btn 'Tab 2' false)] [(c/label-fn :text 'GRID')])
   Needed:
      tab-btn component is needed to corectly work"
  (fn [tabs array]
    (let [tabs-space (mig-panel
                      :constraints ["" "0px[fill]0px" "0px[]0px"]
                      :id :app-tabs-space
                      :items (gtool/join-mig-items tabs))
          views-space (mig-panel
                       :constraints ["wrap 1" "0px[grow, fill]0px" "0px[grow, fill]0px"]
                       :id :app-functional-space
                       :background (new Color 0 0 0 0)
                       :items (gtool/join-mig-items array))]
      ;; (println tabs)
      (state/set-state :jarman-views-service (vs/new-views-service tabs-space views-space))
      (mig-panel
       :id :operation-space
       :background "#eee"
       :constraints ["wrap 1" "0px[grow, fill]0px" "0px[28, shrink 0]0px[grow, fill]0px"]
       :background "#eee"
       ;; :border (line-border :left 1 :color "#999")
       :items [[(gcomp/min-scrollbox tabs-space :vscroll :never)]
               [views-space]]))))

(def jarmanapp--main-tree
  "Description:
      Vertical layout of elements, left part of app for functions
   Example:
      (jarmanapp--main-tree  [(button-expand 'Ukryte opcje 1' [(some-button)])] [(button-expand 'Ukryte opcje 2')])
   Needed:
      button-expand component is needed to corectly work"
  (fn []
    (let [root (mig-panel
                :id :expand-menu-space
                :background "#fff"
                :border (b/line-border :left 4 :right 4 :color "#fff")
                :constraints ["wrap 1" "0px[200::, fill, grow]0px" "0px[fill]0px"])
          render-fn (fn [] (let [comps (gtool/join-mig-items
                                        (rift (state/state [:main-menu])
                                              (c/label :text "No comps")))]
                             comps))]
      (state/set-global-state-watcher
       root
       render-fn
       [:main-menu])
      (gcomp/scrollbox
       (c/config! root :items (render-fn)) 
       :hbar-size 0
       :vbar-size 0))))


(def jarmanapp
  "Description:
      Main space for app inside JLayeredPane. There is menu with expand btns and space for tables with tab btns."
  (fn [& {:keys [margin-left]
          :or {margin-left 0}}]
    (let [bg-color "#eee"
          vhr-color "#999"]
      (mig-panel
       :id :rebound-layer
       :constraints [""
                     "0px[shrink 0, fill]0px[grow, fill]0px"
                     "0px[grow, fill]0px"]
       :border (b/line-border :left margin-left :color bg-color)
       :items [[(jarmanapp--main-tree)]
               [(jarmanapp--main-view-space [] [])]]))))


;; ┌────────────────┐
;; │                │
;; │ Load main-menu │
;; │                │
;; └────────────────┘


(defn- load-static-main-menu []
  (menu/add-to-main-tree
     (concat
      (state/state [:main-menu])
      (menu/bulid-expand-by-map (menu/default-menu-items)))))


(defn load-plugins-to-main-menu []
  (let [plugins-m (vmg/do-view-load)]
    (vmg/prepare-defview-editors-state)
    (menu/add-to-main-tree
     (concat
      (state/state [:main-menu])
      (menu/bulid-expand-by-map plugins-m)))))


;; ┌─────────────┐
;; │             │
;; │ Load stages │
;; │             │
;; └─────────────┘

;; before central swing-component build
(defn- load-level-0
  "Description:
    Load configuration"
  []
  ;; (managment-data/on-app-start)
  (cm/swapp)
  (dot-jarman/dot-jarman-load)
  (dot-jarman-param/print-list-not-loaded))
;; after swing component was builded
(defn load-level-1
  "Description:
    Check last position and remember x y"
  [relative-pos]
  (try
    (reset! relative-pos [(.x (.getLocationOnScreen (seesaw.core/to-frame (state/state :app))))
                          (.y (.getLocationOnScreen (seesaw.core/to-frame (state/state :app))))])
    (.dispose (seesaw.core/to-frame (state/state :app)))
    (catch Exception e (println "Last pos is nil"))))


(defn load-level-2
  "Description:
    Bulid application."
  []
  (gseed/build
   :items (let [img-scale 35
                top-offset 2]
            (menu/clean-main-menu)

            (concat ;; items for jlp
             [[(jarmanapp :margin-left img-scale) 0]]
             (menu/menu-slider img-scale top-offset
                          [{:icon  icon/I-64-png
                            :title "Message Store"
                            :fn    (fn [e] ((state/state :alert-manager) :show))}
                           
                           {:icon  icon/refresh-blue1-64-png
                            :title "Reload active view"
                            :fn    (fn [e] (try
                                             (((state/state :jarman-views-service) :reload))
                                             (catch Exception e (str "Can not reload. Storage is empty."))))}

                           {:icon  icon/refresh-blue-64-png
                            :title "Restart"
                            :fn    (fn [e] ((state/state :startup)))}

                           {:icon  icon/download-blue-64-png
                            :title "Update"
                            :fn    (fn [e] (println "Check update"))}

                           {:icon  icon/enter-64-png
                            :title "Close app"
                            :fn    (fn [e] (.dispose (c/to-frame e)))}])
             [(gcomp/fake-focus :vgap top-offset :hgap img-scale)]))))



(defn load-level-3
  "Description:
    Try display frame in same place when reloading. Next set to frame login@permission."
  [relative-pos]
  (if-not (nil? @relative-pos)
    (.setLocation (seesaw.core/to-frame (state/state :app)) (first @relative-pos) (second @relative-pos)))
  (gseed/extend-frame-title (str ", " (session/user-get-login) "@" (session/user-get-permission))))

(defn load-level-4
  "Description:
    Load main menu."
  []
  (dot-jarman-load)
  (do-load-plugins)
  (menu/clean-main-menu)
  (load-plugins-to-main-menu)
  (load-static-main-menu))

;; ┌─────────────┐
;; │             │
;; │ App starter │
;; │             │
;; └─────────────┘

(def invoke-app
  "Description:
    Run or restart jarman main app"
  (fn []
    (let [relative-pos (atom nil)]
      (load-level-0)
      (load-level-1 relative-pos)
      (load-level-2)
      (load-level-3 relative-pos)
      (load-level-4))))

(state/set-state
 :startup
 (fn []
   (cond (= (iinit/validate-configuration-files) true)
         (invoke-app)
         :else (cond (= (iinit/restore-backup-configuration) false)
                     (do (fn []))
                     :else (do
                             (= (iinit/validate-configuration-files) true)
                             (invoke-app)
                             :else (do (fn [])))))))

((state/state :startup))

