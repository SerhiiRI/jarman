(ns jarman.gui.gui-app
  (:use
   seesaw.dev
   seesaw.style
   seesaw.mig
   seesaw.font)
  (:import
   (javax.swing JLayeredPane JLabel JTable JComboBox DefaultCellEditor JCheckBox)
   (jiconfont.icons.google_material_design_icons GoogleMaterialDesignIcons)
   (javax.swing.table TableCellRenderer TableColumn)
   (java.awt.event MouseEvent)
   ;; (jarman.jarmanjcomp DateTime)
   (java.awt Color Component)
   (java.awt Dimension))
  (:require
   [clojure.spec.alpha  :as s]
   [clojure.string      :as string]
   [seesaw.swingx       :as swingx]
   [seesaw.core         :as c]
   [seesaw.util         :as u]
   [seesaw.border       :as b]
   ;; external funcionality
   [jarman.faces                    :as face]
   [jarman.variables]
   ;; logic
   [jarman.logic.state              :as state]
   [jarman.application.session            :as session]
   [jarman.logic.view-manager       :as view-manager]
   ;; [jarman.logic.connection         :refer [do-connect-to-database]]
   [jarman.managment.data           :refer [do-load-jarman-data]]
   ;; [jarman.plugin.extension-manager :refer [do-load-extensions]]
   [jarman.application.extension-manager :refer [do-load-extensions]]
   [jarman.application.collector-custom-themes :refer [do-load-theme]]
   [jarman.config.vars              :refer [setq print-list-not-loaded]]
   [jarman.config.dot-jarman        :refer [dot-jarman-load]]
   [jarman.config.conf-language     :refer [do-load-language]]
   [jarman.org                      :refer :all]
   [jarman.lang                     :refer :all]
   ;; gui 
   [jarman.gui.builtin-themes.jarman-light]
   [jarman.gui.gui-views-service    :as gvs]
   [jarman.gui.gui-alerts-service   :as gas]
   [jarman.gui.gui-components       :as gcomp]
   [jarman.gui.gui-tools            :as gtool]
   [jarman.gui.gui-style            :as gs]
   [jarman.gui.gui-events           :as gui-events]
   ;; [jarman.gui.gui-dbvisualizer     :as dbv]
   [jarman.gui.gui-seed             :as gseed]
   [jarman.gui.gui-main-menu        :as menu]
   [jarman.gui.gui-migrid           :as gmg]
   [jarman.gui.popup                :as popup]
   [jarman.gui.gui-editors          :as gedit]
   [jarman.gui.ui.gui-update-manager   :as gui-update-manager]
   ;; [jarman.gui.gui-calendar         :as calendar]
   [jarman.interaction]))

 
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
                      ;;:items (gtool/join-mig-items tabs)
                     ;; :border (b/line-border :bottom face/s-tab-underline :color face/c-tab-underline)
                      )
          views-space (mig-panel
                       :constraints ["wrap 1" "0px[grow, fill]0px" "0px[grow, fill]0px"]
                       :id :app-functional-space 
                       :background face/c-layout-background
                       :items (gtool/join-mig-items array))]
      ;; (println tabs)
      (state/set-state :views-space views-space)
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
  (wlet
    (menu/add-to-main-tree
     (concat
      (state/state [:main-menu])
      (menu/bulid-expand-by-map view-plugins-menumap)))
    ((view-plugins-menumap
      (binding [view-manager/*view-loader-chain-fn*
                (if (= :view.clj (state/state :view-src))
                    (view-manager/make-loader-chain
                     view-manager/loader-from-view-clj
                     view-manager/loader-from-db)
                    (view-manager/make-loader-chain
                     view-manager/loader-from-db
                     view-manager/loader-from-view-clj))]
        (view-manager/do-view-load))))))


;; ┌─────────────┐
;; │             │
;; │ Load stages │
;; │             │
;; └─────────────┘

;; before central swing-component build
;; fixme:serhii load-levels need to be configurable through the parameters
(defn load-level-0
  "Description:
    Load configuration"
  []
  (print-header
   "Set spec/assert checking"
   (s/check-asserts true))
  (print-header 
   "Load .jarman.data"
   (do-load-jarman-data))
  (print-header 
   "Load language.edn"
   (do-load-language))
  (out-extension
   (print-header
    "Load Extensions"
    (do-load-extensions)))
  (print-header
   "Load .jarman"
   (dot-jarman-load)
   (print-list-not-loaded)))

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
      (do-load-theme (deref jarman.variables/theme-selected))
      (state/set-state :theme-first-load true)
      (print-header "First theme loaded"))

    (print-header "Apply selected theme"
     (do-load-theme (state/state :theme-name))))
  
  (print-header "apply default global style"
    (gs/load-style))

  (print-header "set XY position on screen for main frame"
    (if (= false (state/state :soft-restart))
      (try
        (reset! relative-pos [(.x (.getLocationOnScreen (seesaw.core/to-frame (state/state :app))))
                              (.y (.getLocationOnScreen (seesaw.core/to-frame (state/state :app))))])
        (.dispose (seesaw.core/to-frame (state/state :app)))
        (catch Exception e (println "Last pos is nil"))))))

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
                            :title (gtool/get-lang-btns :messages-history)
                            :fn    (fn [e] (gas/show-alerts-history))}
                           
                           {:icon  (gs/icon GoogleMaterialDesignIcons/DASHBOARD face/c-icon)
                            :title (gtool/get-lang-btns :reload-active-view)
                            :fn    (fn [e] (try
                                             (gvs/reload-view)
                                             (catch Exception e (str "Can not reload. Storage is empty."))))}

                           {:icon  (gs/icon GoogleMaterialDesignIcons/REFRESH face/c-icon)
                            :title "Soft Restart"
                            :fn    (fn [e] (gvs/soft-restart))}
                           
                           {:icon  (gs/icon GoogleMaterialDesignIcons/CACHED face/c-icon)
                            :title "Restart"
                            :fn    (fn [e] (gvs/restart))}

                           {:icon  (gs/icon GoogleMaterialDesignIcons/SETTINGS_BACKUP_RESTORE face/c-icon)
                            :title "Hard Restart"
                            :fn    (fn [e] (gvs/restart))}

                           {:icon  (gs/icon GoogleMaterialDesignIcons/LANGUAGE face/c-icon)
                            :title "Reload language"
                            :fn    (fn [e] (jarman.config.conf-language/do-load-language)
                                     (gas/success "Reloaded languages"))}
                           
                           {:icon  (gs/icon GoogleMaterialDesignIcons/LIST face/c-icon)
                            :title "Reload main menu"
                            :fn    (fn [e]
                                     (menu/clean-main-menu)
                                     (load-plugins-to-main-menu)
                                     (load-static-main-menu))}

                           ;; TODO DISABLE FOR ALL 
                           ;; {:icon  (gs/icon GoogleMaterialDesignIcons/VPN_KEY face/c-icon)
                           ;;  :title "Change work mode"
                           ;;  :fn    (fn [e]
                                     
                           ;;           (cond (= "user"      (session/get-user-permission)) (session/set-user-permission "admin")
                           ;;                 (= "admin"     (session/get-user-permission)) (session/set-user-permission "developer")
                           ;;                 (= "developer" (session/get-user-permission)) (session/set-user-permission "user"))
                           ;;           (i/warning "Work mode" (str "Switched to: " (session/get-user-permission)))
                           ;;           (gseed/extend-frame-title (str ", " (session/get-user-login) "@" (session/get-user-permission))))}
                            
                           ;; {:icon  icon/download-blue-64-png
                           ;;  :title "Update"
                           ;;  :fn    (fn [e] (println "Check update"))}
                           
                           {:icon  (gs/icon GoogleMaterialDesignIcons/PERSON face/c-icon )
                            :title (gtool/get-lang-btns :logout)
                            :fn    (fn [e]
                                     (state/set-state :debug-mode false)
                                     ((state/state :invoke-login-panel))
                                     (state/set-state :soft-restart nil)
                                     (.dispose (c/to-frame e)))}

                           ;; {:icon  (gs/icon GoogleMaterialDesignIcons/SETTINGS face/c-icon)
                           ;;  :title (gtool/get-lang-btns :settings)
                           ;;  :fn    (fn [e] (gvs/add-view
                           ;;                  :view-id :app-settings
                           ;;                  :title (gtool/get-lang-btns :settings)
                           ;;                  :render-fn #(gcp/config-panel)))}
                           
                           {:icon  (gs/icon GoogleMaterialDesignIcons/EXIT_TO_APP face/c-icon)
                            :title (gtool/get-lang-btns :close-app)
                            :fn    (fn [e]
                                     (state/set-state :soft-restart nil)
                                     (.dispose (c/to-frame e)))}

                           {:icon  (gs/icon GoogleMaterialDesignIcons/BUG_REPORT face/c-icon)
                            :title "Doom"
                            :fn    (fn [e]
                                     (if (state/state :doom)
                                       (gcomp/rm-doom)
                                       (let [h (/ (second @(state/state :atom-app-size)) 2) w h]
                                         (gcomp/open-doom
                                           (gmg/migrid
                                             :v :center :bottom
                                             [(c/label :text "RICARDOOM" :font (gs/getFont 30))
                                              (gmg/migrid
                                                :> :f [(gcomp/label-img "rc.gif" w h)
                                                       (gcomp/label-img "rc.gif" w h)
                                                       (gcomp/label-img "rc.gif" w h)])])))))}])
             [(gcomp/fake-focus :vgap top-offset :hgap img-scale)]))))

(defn load-level-3
  "Description:
    Try display frame in same place when reloading. Next set to frame login@permission."
  [relative-pos]
  (print-header "Display frame in same place where login frame was"
   (if-not (nil? @relative-pos)
     (.setLocation (seesaw.core/to-frame (state/state :app)) (first @relative-pos) (second @relative-pos)))
   (let [{:keys [login profile-name]} (.get-user (session/session))]
     (gseed/extend-frame-title (str ", " login "@" profile-name)))))

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
   (load-static-main-menu))
  (gas/show-delay-alerts)
  (print-header
   "Refresh avialable package list cache"
   (gui-update-manager/update-package-list-cache))
  (print-header
   "Check updates"
   (gui-update-manager/check-update :silent))
  (print-header
   "Check license"
   (gui-events/gui-check-license))
  (print-header
   "Start watching state alerts"
   (gas/start-watching-state-alerts (state/get-atom))))


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
      (if (state/state :inlogin-loaded)
        (state/set-state :inlogin-loaded false)
        (do
          (if (false? (state/state :soft-restart)) (load-level-0))
          (load-level-1 relative-pos)))
      (load-level-2)
      (load-level-3 relative-pos)
      (load-level-4)
      (c/config! (c/to-frame (state/state :app))
                 :size [(first @(state/state :atom-app-size))
                        :by
                        (+ 37 (second @(state/state :atom-app-size)))]))))

(state/set-state :startup (fn [] (invoke-app)))

(comment
  ;; --------------
  (do
   (state/set-state
    :startup
    (fn [] (invoke-app)))
   ((state/state :startup)))
  ;; --------------
  )

(comment  
  (state/set-state :soft-restart false)
  (state/set-state :theme-name "Jarman Light")
  (state/state :theme-name)
  (gas/info "Info" "Test"))

