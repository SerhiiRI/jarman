(ns jarman.gui.gui-app
  (:use seesaw.dev
        seesaw.style
        seesaw.mig
        seesaw.font)
  (:import (javax.swing JLayeredPane JLabel JTable JComboBox DefaultCellEditor JCheckBox)
           (javax.swing.table TableCellRenderer TableColumn)
           (java.awt.event MouseEvent)
           (jarman.test DateTime)
           (java.awt Color Component)
           (java.awt Dimension))
  (:require [jarman.tools.lang :refer :all]
            [clojure.string :as string]
            [seesaw.core    :as c]
            [seesaw.util    :as u]
            [seesaw.border  :as b]
            ;; resource 
            [jarman.resource-lib.icon-library :as icon]
            [clojure.pprint :as pp]
            ;; logicsx
            [jarman.config.config-manager  :as cm]
            [jarman.gui.gui-views-service  :as vs]
            [jarman.gui.gui-alerts-service :as gas]
            [jarman.gui.gui-components     :as gcomp]
            [jarman.gui.gui-tools          :as gtool]
            ;; deverloper tools 
            [jarman.tools.swing  :as stool]
            [jarman.config.init  :as iinit]
            [jarman.logic.state  :as state]
            [jarman.gui.gui-seed :as gseed]
            [jarman.logic.session :as session]
            [jarman.logic.view-manager :as vmg]
            [jarman.gui.gui-dbvisualizer :as dbv]
            [jarman.gui.gui-config-generator :as cg]))


;; ┌─────────────────────────────────────────┐
;; │                                         │
;; │ Create expand btns for config generator │
;; │                                         │
;; └─────────────────────────────────────────┘

(def create-expand-btns--confgen
  "Discription:
     Return expand button with config generator GUI
     Complete component
   "
  (fn [] (if (= false (:valid? (cm/get-in-segment []))) 
           [(gcomp/button-expand "Settings Error" []
                                 :ico (stool/image-scale icon/alert-64-png 25)
                                 :ico-hover (stool/image-scale icon/alert-64-png 25)
                                 :onClick (fn [e] ((state/state :alert-manager)
                                                   :set {:header "Settings Error"
                                                         :body (str "Valid output: " (:output (cm/get-in-segment [])))}
                                                   5)))] 
           (gcomp/button-expand
            (gtool/get-lang-btns :settings)
            (let [current-theme (str (first (cm/get-in-value [:themes :theme_config.edn :selected-theme])) ".edn")
                  config-file-list-as-keyword (map #(first %) (cm/get-in-segment []))
                  config-file-list-as-keyword-to-display
                  (filter #(let [map-part (cm/get-in-segment (if (vector? %) % [%]))]
                                                                    (and (= :file (get map-part :type))
                                                                         (= :edit (get map-part :display))))
                                                                 config-file-list-as-keyword)
                  restore-button (gcomp/button-expand-child
                                  (gtool/get-lang-btns :restore-last-configuration)
                                  :onClick (fn [e]
                                             (if-not (nil? (cm/restore-config)) ((state/state :alert-manager)
                                                                                 :set {:header "Success!"
                                                                                       :body (gtool/get-lang-alerts
                                                                                              :restore-configuration-ok)}
                                                                                 5))))]
              (reverse
               (conj
                (map
                 (fn [p]
                   (let [path (if (vector? p) p [p])
                         title (get (cm/get-in-segment path) :name)
                         view-id (last path)]
                     (gcomp/button-expand-child
                      title
                      :onClick (fn [e]
                                 ((state/state :jarman-views-service)
                                  :set-view
                                  :view-id view-id
                                  :title title
                                  :scrollable? false
                                  :component-fn (try
                                                  (fn [] (cg/create-view--confgen
                                                          path
                                                          :message-ok (fn [head body]
                                                                        ((state/state :alert-manager)
                                                                         :set {:header head :body body}
                                                                         5))))
                                                  (catch Exception e (gcomp/popup-config-editor
                                                                      path
                                                                      (get (cm/get-in-segment path))))))))))
                 config-file-list-as-keyword-to-display)
                (let [path [:themes :theme_config.edn] ;; Selected theme
                      title (:name (cm/get-in-segment path))
                      view-id :theme_config.edn]
                  (gcomp/button-expand-child
                   title
                   :onClick (fn [e]
                              ((state/state :jarman-views-service)
                               :set-view
                               :view-id view-id
                               :title title
                               :scrollable? false
                               :component-fn (fn [] (cg/create-view--confgen
                                                     path
                                                     :message-ok (fn [head body] ((state/state :alert-manager)
                                                                                  :set {:header head :body body}
                                                                                  (((state/state :alert-manager)
                                                                                    :message)
                                                                                   (state/state :alert-manager))
                                                                                  5))))))))
                (let [path [:themes :current-theme] ;; Themes config
                      title (rift (:name (cm/get-in-segment path)) "NIL")
                      view-id :current-theme]
                  (gcomp/button-expand-child
                   title
                   :onClick (fn [e]
                              ((state/state :jarman-views-service)
                               :set-view
                               :view-id view-id
                               :title title
                               :scrollable? false
                               :component-fn (fn [] (cg/create-view--confgen
                                                     path
                                                     :message-ok (fn [head body]
                                                                   ((state/state :alert-manager)
                                                                    :set {:header head :body body}
                                                                    ))))))))
                ;; restore-button
                )))))))

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
                                        (rift (state/state [:jarmanapp--main-tree])
                                              (c/label :text "No comps")))]
                             (println "\nComps\n" comps)
                             (println "\nComps state\n" (state/state [:jarmanapp--main-tree]))
                             comps))]
      (state/set-global-state-watcher
       root
       render-fn
       [:jarmanapp--main-tree])
      (gcomp/scrollbox
       (c/config! root :items (render-fn)) 
       :hbar-size 0
       :vbar-size 0))))

;; (state/set-state
;;   [:jarmanapp--main-tree] [])

(defn- load-default-menu-tree []
  (state/set-state
  [:jarmanapp--main-tree]
  (list
   (gcomp/button-expand
    "Database"
    [(gcomp/button-expand-child
      "DB Visualiser"
      :onClick (fn [e] 
                 (try 
                   ((state/state :jarman-views-service) :set-view
                    :view-id "DB Visualiser"
                    :title "DB Visualiser"
                    :component-fn dbv/create-view--db-view)
                   (catch Exception e ((state/state :alert-manager) :set {:header "Exception"
                                                                          :body (.getMessage e)} 5)))))])
   (gcomp/button-expand "Tables" [] :id :tables-view-plugin :expand :yes)
   (create-expand-btns--confgen)
   (gcomp/button-expand
    "Debug items"
    [(gcomp/button-expand-child "Popup window"
                                :onClick (fn [e] (gcomp/popup-window {:relative (state/state :app)})))
     (gcomp/button-expand-child "alert"
                                :onClick (fn [e] ((state/state :alert-manager)
                                                  :set {:header "Czym jest Lorem Ipsum?"
                                                        :body "Lorem Ipsum jest tekstem stosowanym jako przykładowy wypełniacz w przemyśle poligraficznym. Został po raz pierwszy użyty w XV w."} 5)))
     (gcomp/button-expand-child "Select table"
                                :onClick (fn [e] (gcomp/popup-window
                                                  {:view (gcomp/select-box-table-list {})
                                                   :relative (state/state :app) :size [250 40]})))
     (gcomp/button-expand-child
      "Text multiline"
      :onClick (fn [e]
                 (gcomp/popup-window
                  {:window-title "Text multiline"
                   :relative (state/state :app)
                   :size [250 250]
                   :view (c/text
                          :text "Some text"
                          :size [300 :by 300]
                          :editable? true
                          :multi-line? true
                          :wrap-lines? true)})))
     (gcomp/button-expand-child
      "Rsyntax code editor"
      :onClick (fn [e]
                 (gcomp/popup-window
                  {:window-title "Code editor"
                   :relative (state/state :app)
                   :size [450 350]
                   :view (gcomp/code-editor
                          {:dispose true
                           :val "(fn [x] (println \"Nice ass\" x)"})})))]))))


(defn add-to-main-tree [components-coll]
  (let [path [:jarmanapp--main-tree]]
    (swap! (state/get-atom) (fn [state] (assoc-in state path (concat (state path) components-coll))))))

(defn- jit-menu-tree-test []
  (add-to-main-tree
  (concat
   (state/state [:jarmanapp--main-tree])
   (list (gcomp/button-expand
          "Add just in time"
          [(gcomp/button-expand-child
            "JIT Test"
            :onClick (fn [e] ))])))))

;; (jit-menu-tree-test)
;; (state/get-atom)q
;; (add-to-main-tree [(c/label :text "Test")])
;; (add-to-main-tree [(gcomp/state-combo-box
;;                     (fn [e selected model]
;;                       (println "\n" selected)
;;                       (println model))
;;                     [1 2]
;;                     ["One" "Two"]
;;                     :start-fn (fn [] (println "Loaded")))])


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
       :items [[(jarmanapp--main-tree)]]))))

(defn- example-plugins-map
[]
  {"Admin space"
   {"User table"      ["user"       [:#tables-view-plugin] [:user] (fn [] (c/label :text "\nUser\n"))],
    "Permission edit" ["permission" [:#tables-view-plugin] [:user] (fn [] (c/label :text "\nPermission\n"))]},
   
   "Sale structure"
   {"Enterpreneur"        ["enterpreneur"        [:#tables-view-plugin] [:user] (fn [] (c/label :text "\nEnter\n"))],
    "Point of sale group" ["point_of_sale_group" [:#tables-view-plugin] [:user] (fn [] (c/label :text "\nPoint\n"))]},

   "Depper structure"
   {"Depper 1"
    {"Depper 2" ["enterpreneur" [:#tables-view-plugin] [:user] (fn [] (c/label :text "\nDepper2\n"))]}}})

(defn- bulid-expand-by-map
  [plugin-m]
  (doall
   (map (fn [coll]
          (let [k (first  coll)
                v (second coll)]
            (cond
              (map?    v) (gcomp/button-expand (str k) (bulid-expand-by-map v))
              (vector? v) (gcomp/button-expand-child
                           (str k)
                           :onClick (if (fn? (last v))
                                      (fn [e]
                                        ((state/state :jarman-views-service)
                                         :set-view
                                         :view-id (str "auto-" (first v))
                                         :title k
                                         :scrollable? false
                                         :component-fn (last v)))
                                      (fn [e] (println "\nProblem with fn in " k))))
              :else (c/label :text "Uncorrect comp"))))
        plugin-m)))

;;(println "\n" (bulid-expand-by-map (example-plugins-map)))

(defn- jit-expand-plugin []
  (add-to-main-tree
  (concat
   (state/state [:jarmanapp--main-tree])
   (bulid-expand-by-map (example-plugins-map)))))

;; (jit-expand-plugin)
;; (state/set-state [:jarmanapp--main-tree] [])

(defn- load-plugins-to-main-menu []
  (let [;; plugins-m (vmg/do-view-load)
        plugins-m (example-plugins-map)]
    ;; (vmg/prepare-defview-editors-state)
    (add-to-main-tree
     (concat
      (state/state [:jarmanapp--main-tree])
      (bulid-expand-by-map plugins-m)))))

;; {"Admin space"
;;  {"User table"
;;   ["user"
;;    [:#tables-view-plugin]
;;    [:user]
;;    #function[jarman.logic.view-manager/defview/iter--21432--21436/fn--21437/fn--21438/fn--21440]],
;;   "Permission edit"
;;   ["permission"
;;    [:#tables-view-plugin]
;;    [:user]
;;    #function[jarman.logic.view-manager/defview/iter--21432--21436/fn--21437/fn--21438/fn--21440]]},
;;  "Sale structure"
;;  {"Enterpreneur"
;;   ["enterpreneur"
;;    [:#tables-view-plugin]
;;    [:user]
;;    #function[jarman.logic.view-manager/defview/iter--21432--21436/fn--21437/fn--21438/fn--21440]],
;;   "Point of sale group"
;;   ["point_of_sale_group"
;;    [:#tables-view-plugin]
;;    [:user]
;;    #function[jarman.logic.view-manager/defview/iter--21432--21436/fn--21437/fn--21438/fn--21440]],
;;   "Point of sale group links"
;;   ["point_of_sale_group_links"
;;    [:#tables-view-plugin]
;;    [:user]
;;    #function[jarman.logic.view-manager/defview/iter--21432--21436/fn--21437/fn--21438/fn--21440]],
;;   "Point of sale"
;;   ["point_of_sale"
;;    [:#tables-view-plugin]
;;    [:user]
;;    #function[jarman.logic.view-manager/defview/iter--21432--21436/fn--21437/fn--21438/fn--21440]]},
;;  "Repair contract"
;;  {"Repair contract"
;;   ["repair_contract"
;;    [:#tables-view-plugin]
;;    [:user]
;;    #function[jarman.logic.view-manager/defview/iter--21432--21436/fn--21437/fn--21438/fn--21440]],
;;   "Repair reasons"
;;   ["repair_reasons"
;;    [:#tables-view-plugin]
;;    [:user]
;;    #function[jarman.logic.view-manager/defview/iter--21432--21436/fn--21437/fn--21438/fn--21440]],
;;   "Repair technical issue"
;;   ["repair_technical_issue"
;;    [:#tables-view-plugin]
;;    [:user]
;;    #function[jarman.logic.view-manager/defview/iter--21432--21436/fn--21437/fn--21438/fn--21440]],
;;   "Repair nature of problem"
;;   ["repair_nature_of_problem"
;;    [:#tables-view-plugin]
;;    [:user]
;;    #function[jarman.logic.view-manager/defview/iter--21432--21436/fn--21437/fn--21438/fn--21440]],
;;   "Cache register"
;;   ["cache_register"
;;    [:#tables-view-plugin]
;;    [:user]
;;    #function[jarman.logic.view-manager/defview/iter--21432--21436/fn--21437/fn--21438/fn--21440]],
;;   "Seal"
;;   ["seal"
;;    [:#tables-view-plugin]
;;    [:user]
;;    #function[jarman.logic.view-manager/defview/iter--21432--21436/fn--21437/fn--21438/fn--21440]]},
;;  "Service contract"
;;  {"Service contract"
;;   ["service_contract"
;;    [:#tables-view-plugin]
;;    [:user]
;;    #function[jarman.logic.view-manager/defview/iter--21432--21436/fn--21437/fn--21438/fn--21440]],
;;   "Service contract month"
;;   ["service_contract_month"
;;    [:#tables-view-plugin]
;;    [:user]
;;    #function[jarman.logic.view-manager/defview/iter--21432--21436/fn--21437/fn--21438/fn--21440]]}}

;; ┌─────────────┐
;; │             │
;; │ App starter │
;; │             │
;; └─────────────┘


(def startup (atom nil))

(def run-me
  (fn []
    (let [relative (atom nil)]
      (cm/swapp)
      (try
        (reset! relative [(.x (.getLocationOnScreen (seesaw.core/to-frame (state/state :app))))
                          (.y (.getLocationOnScreen (seesaw.core/to-frame (state/state :app))))])
        (.dispose (seesaw.core/to-frame (state/state :app)))
        (catch Exception e (println "Last pos is nil")))
      (load-default-menu-tree)
      (gseed/build
       :items (let [img-scale 35
                    top-offset 2]
                (list
                 [(jarmanapp :margin-left img-scale) 0]
                 (gtool/slider-ico-btn (stool/image-scale icon/scheme-grey-64-png img-scale) 0 img-scale "DB Visualiser"
                                       :onClick (fn [e] ((state/state :jarman-views-service)
                                                         :set-view
                                                         :view-id "DB Visualiser"
                                                         :title "DB Visualiser"
                                                         :component-fn dbv/create-view--db-view))
                                       :top-offset top-offset)
                 (gtool/slider-ico-btn (stool/image-scale icon/I-64-png img-scale) 1 img-scale "Message Store"
                                       :onClick (fn [e] ((state/state :alert-manager) :show))
                                       :top-offset top-offset)
                 (gtool/slider-ico-btn (stool/image-scale icon/key-blue-64-png img-scale) 2 img-scale "Change work mode"
                                       :onClick (fn [e]
                                                  (cond (= "user"      (session/user-get-permission)) (session/user-set-permission "admin")
                                                        (= "admin"     (session/user-get-permission)) (session/user-set-permission "developer")
                                                        (= "developer" (session/user-get-permission)) (session/user-set-permission "user"))
                                                  ((state/state :alert-manager) :set {:header "Work mode" :body (str "Switched to: " (session/user-get-permission))}  5)
                                                  (gseed/extend-frame-title (str ", " (session/user-get-login) "@" (session/user-get-permission))))
                                       :top-offset top-offset)
                 (gtool/slider-ico-btn (stool/image-scale icon/enter-64-png img-scale) 3 img-scale "Close app"
                                       :onClick (fn [e] (.dispose (c/to-frame e)))
                                       :top-offset top-offset)
                 (gtool/slider-ico-btn (stool/image-scale icon/refresh-blue1-64-png img-scale) 4 img-scale "Reload active view"
                                       :onClick (fn [e] (try
                                                          (((state/state :jarman-views-service) :reload))
                                                          (catch Exception e (str "Can not reload. Storage is empty."))))
                                       :top-offset top-offset)
                 (gtool/slider-ico-btn (stool/image-scale icon/refresh-blue-64-png img-scale) 5 img-scale "Restart"
                                       :onClick (fn [e] (do
                                                          (println "App restart")
                                                          (@startup)))
                                       :top-offset top-offset)
                 (gcomp/fake-focus :vgap top-offset :hgap img-scale))))
           (if-not (nil? @relative) (.setLocation (seesaw.core/to-frame (state/state :app)) (first @relative) (second @relative))))
    (gseed/extend-frame-title (str ", " (session/user-get-login) "@" (session/user-get-permission)))
    (load-plugins-to-main-menu)))


(reset! startup
        (fn []
          (cond (= (iinit/validate-configuration-files) true)
                (run-me)
                :else (cond (= (iinit/restore-backup-configuration) false)
                            (do (fn []))
                            :else (do
                                    (= (iinit/validate-configuration-files) true)
                                    (run-me)
                                    :else (do (fn [])))))))

(@startup)
