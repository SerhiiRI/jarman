(ns jarman.gui.gui-app
  (:use seesaw.dev
        seesaw.style
        seesaw.mig
        seesaw.font)
  (:import (javax.swing JLayeredPane JLabel JTable JComboBox DefaultCellEditor JCheckBox)
           (jiconfont.icons.google_material_design_icons GoogleMaterialDesignIcons)
           (javax.swing.table TableCellRenderer TableColumn)
           (java.awt.event MouseEvent)
           (jarman.jarmanjcomp DateTime)
           (java.awt Color Component)
           (java.awt Dimension))
  (:require [jarman.faces :as face]
            [jarman.tools.lang :refer :all]
            [clojure.string    :as string]
            [seesaw.swingx  :as swingx]
            [seesaw.core       :as c]
            [seesaw.util       :as u]
            [seesaw.border     :as b]
            ;; resource
            [jarman.gui.gui-style             :as gs]
            [jarman.resource-lib.icon-library :as icon]
            [jarman.gui.gui-style :as gs]
            [clojure.pprint :as pp]
            ;; logic
            [jarman.config.config-manager    :as cm]            
            [jarman.gui.gui-views-service    :as gvs]
            [jarman.gui.gui-alerts-service   :as gas]
            [jarman.gui.gui-components       :as gcomp]
            [jarman.gui.gui-tools            :as gtool]
            [jarman.tools.org                :refer :all]
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
            ;; [jarman.managment.data           :as managment-data]
            [jarman.plugin.extension-manager :refer [do-load-extensions]]
            [jarman.plugin.plugin            :refer [do-load-theme]]
            [jarman.config.vars              :refer [setq print-list-not-loaded]]
            [jarman.config.dot-jarman        :refer [dot-jarman-load]]
            [jarman.gui.builtin-themes.jarman-light]
            [jarman.interaction              :as i]))

 
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
                      :background face/c-tabbar-bg
                      :items (gtool/join-mig-items tabs)
                      :border (b/line-border :bottom face/s-tab-underline :color face/c-tab-underline))
          views-space (mig-panel
                       :constraints ["wrap 1" "0px[grow, fill]0px" "0px[grow, fill]0px"]
                       :id :app-functional-space 
                       :background face/c-layout-background
                       :items (gtool/join-mig-items array))]
      ;; (println tabs)
      ;;(state/set-state :jarman-views-service (vs/new-views-service tabs-space views-space))p
      (gvs/start views-space tabs-space)
      (mig-panel
       :id :operation-space
       ;; :background "#eee"
       :constraints ["wrap 1" "0px[grow, fill]0px" "0px[28, shrink 0]0px[grow, fill]0px"]
       ;; :border (line-border :left 1 :color "#999")
       :background face/c-layout-background
       :items [[(gcomp/min-scrollbox tabs-space :args [:vscroll :never])]
               [views-space]]))))

(def jarmanapp--main-tree
  "Dscription:
      Vertical layout of elements, left part of app for functions
   Example:
      (jarmanapp--main-tree  [(button-expand 'Ukryte opcje 1' [(some-button)])] [(button-expand 'Ukryte opcje 2')])
   Needed:
      button-expand component is needed to corectly work"
  (fn []
    (let [root (mig-panel
                :id :expand-menu-space
                :background face/c-main-menu-bg
                :border (b/line-border :left 4 :right 4 :color face/c-main-menu-vhr)
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
    (mig-panel
     :id :rebound-layer
     :constraints [""
                   "0px[shrink 0, fill]0px[grow, fill]0px"
                   "0px[grow, fill]0px"]
     :border (b/line-border :left margin-left :color face/c-layout-background)
     :background face/c-layout-background
     :items [[(jarmanapp--main-tree)]
             [(jarmanapp--main-view-space [] [])]])))


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

;;; Quick log
;; (defmacro ^:private log
;;   ([msg]
;;    `(println (format "** %s" ~msg)))
;;   ([action msg]
;;    `(do
;;      (println (format "*** %s" ~msg))
;;      ~action)))


;; before central swing-component build
(defn- load-level-0
  "Description:
    Load configuration"
  []
  ;; (managment-data/on-app-start)
  (print-header 
   "Swapp"
   (cm/swapp))
  (dot-jarman-load)
  (print-list-not-loaded)
  (print-header 
   "Load .jarman"
   (dot-jarman-load)
   (print-list-not-loaded))
  (print-header
   "Load Extensions"
   (do-load-extensions)))

;; after swing component was builded
(defn load-level-1
  "Description:
    Check last position and remember x y"
  [relative-pos]
  ;; ----------------------------------
  ;; Warning! 
  ;; This theme loaded before
  ;; all extension's are up and compiled.
  ;; please do not remove Jarman-Ligth out
  ;; from integrated into jarman ns's.
  (if (nil? (state/state :theme-first-load))
    (do 
        (do-load-theme "Jarman Light")
        (state/set-state :theme-first-load true)
        (print-header "First theme loaded")))

  (println "\n")
  (print-header
   "Apply selected theme" 
   (do-load-theme (state/state :theme-name))) ;; TODO: Alerts tmp storage with invoke later

  (println "\n")
  (print-header
   "apply default global style" 
   (gs/load-style))

   (if (= false (state/state :soft-restart))
     (try
       (reset! relative-pos [(.x (.getLocationOnScreen (seesaw.core/to-frame (state/state :app))))
                             (.y (.getLocationOnScreen (seesaw.core/to-frame (state/state :app))))])
       (.dispose (seesaw.core/to-frame (state/state :app)))
       (catch Exception e nil ;;(println "Last pos is nil")
              ))))


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
                          [{:icon  (gs/icon GoogleMaterialDesignIcons/INFO face/c-icon)
                            :title "Message Store"
                            :fn    (fn [e] (i/show-alerts-history))}
                           
                           {:icon  (gs/icon GoogleMaterialDesignIcons/UPDATE face/c-icon)
                            :title "Reload active view"
                            :fn    (fn [e] (try
                                             (gvs/reload-view)
                                             (catch Exception e (str "Can not reload. Storage is empty."))))}

                           {:icon  (gs/icon GoogleMaterialDesignIcons/CACHED face/c-icon)
                            :title "Soft Restart"
                            :fn    (fn [e] (gvs/soft-restar))}
                           {:icon  (gs/icon GoogleMaterialDesignIcons/REFRESH face/c-icon)
                            :title "Restart"
                            :fn    (fn [e]
                                     (gvs/stop-watching)
                                     (state/set-state :soft-restart false)
                                     ((state/state :startup)))}
                           
                           {:icon  (gs/icon GoogleMaterialDesignIcons/VPN_KEY face/c-icon)
                            :title "Change work mode"
                            :fn    (fn [e]
                                     (cond (= "user"      (session/get-user-permission)) (session/set-user-permission "admin")
                                           (= "admin"     (session/get-user-permission)) (session/set-user-permission "developer")
                                           (= "developer" (session/get-user-permission)) (session/set-user-permission "user"))
                                     (i/warning "Work mode" (str "Switched to: " (session/get-user-permission)))
                                     (gseed/extend-frame-title (str ", " (session/get-user-login) "@" (session/get-user-permission))))}
                           
                           ;; {:icon  icon/download-blue-64-png
                           ;;  :title "Update"
                           ;;  :fn    (fn [e] (println "Check update"))}
                           
                           {:icon  (gs/icon GoogleMaterialDesignIcons/PERSON face/c-icon )
                            :title "Logout"
                            :fn    (fn [e]
                                     ((state/state :invoke-login-panel))
                                     (state/set-state :soft-restart nil)
                                     (.dispose (c/to-frame e)))}
                           {:icon  (gs/icon GoogleMaterialDesignIcons/EXIT_TO_APP face/c-icon)
                            :title "Close app"
                            :fn    (fn [e]
                                     (state/set-state :soft-restart nil)
                                     (.dispose (c/to-frame e)))}

                           {:icon  (gs/icon GoogleMaterialDesignIcons/STARS face/c-icon)
                            :title "Shooting Stars"
                            :fn    (fn [e] (gs/shooting-stars))}
                           ])
             [(gcomp/fake-focus :vgap top-offset :hgap img-scale)]))))



(defn load-level-3
  "Description:
    Try display frame in same place when reloading. Next set to frame login@permission."
  [relative-pos]
  (if-not (nil? @relative-pos)
    (.setLocation (seesaw.core/to-frame (state/state :app)) (first @relative-pos) (second @relative-pos)))
  (gseed/extend-frame-title (str ", " (session/get-user-login) "@" (session/get-user-permission))))

(defn load-level-4
  "Description:
    Load main menu." []
  (print-header
   "Clean main menu"
   (menu/clean-main-menu))
  (print-header
   "Load view plugin into main menu"
   (load-plugins-to-main-menu))
  (print-header
   "Load static main menu"
   (load-static-main-menu)))


;; ┌─────────────┐
;; │             │
;; │ app starter │
;; │             │
;; └─────────────┘

(def invoke-app
  "Description:
    Run or restart jarman main app"
  (fn []
    (let [relative-pos (atom nil)]
      (if-not (= true (state/state :soft-restart)) (load-level-0))
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


(comment  
  (state/set-state :soft-restart false)
  (state/set-state :theme-name "Jarman Light")
  (state/state :theme-name)
  (i/info "Info" "Test"))

