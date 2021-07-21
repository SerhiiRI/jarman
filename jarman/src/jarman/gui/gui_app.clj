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
            [jarman.config.config-manager   :as cm]
            [jarman.config.dot-jarman       :as dot-jarman]
            [jarman.config.dot-jarman-param :as dot-jarman-param]
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
            [jarman.gui.gui-config-generator :as cg]
            [jarman.gui.popup :as popup]))

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
       :items [[(jarmanapp--main-tree)]
               [(jarmanapp--main-view-space [] [])]]))))


(defn- expand-colors []
  [["#eeeeee" "#eeefff"]
   ["#7fff00" "#c2ff85"]
   ["#00fa9a" "#61ffc2"]
   ["#79d1c4" "#9ae3d8"]
   ["#9ae3d8" "#9ae3d8"]])

(defn- part-plugin
  [k v lvl]
  (if (session/allow-permission? (.return-permission v))
    (gcomp/button-expand-child
          (str k)
          :left (* (dec lvl) 5)
          :hover-color (second (nth (expand-colors) (dec lvl)))
          :onClick (fn [e]
                     ((state/state :jarman-views-service)
                      :set-view
                      :view-id (str "auto-plugin" (.return-title v))
                      :title k
                      :scrollable? false
                      :component-fn (.return-entry v))))
    ;; or return nil
    nil))

(defn- part-expand [bulid-expand-by-map k v lvl]
  (let [depper (filter-nil (bulid-expand-by-map v :lvl (inc lvl)))]
                           (if (empty? depper)
                             (if (= lvl 0) (c/label) nil)
                             (gcomp/button-expand
                              (str k)
                              depper
                              :left-color (first (nth (expand-colors) lvl))
                              :bg-color   (first (nth (expand-colors) lvl))
                              :left (* lvl 5)))))

(defn- part-button [k v lvl]
  (if (or (nil? (:permission v))
          (session/allow-permission? (:permission v)))
    (gcomp/button-expand-child
     (str k)
     :left (* (dec lvl) 5)
     :hover-color (second (nth (expand-colors) (dec lvl)))
     :onClick
     (if-not (nil? (:fn v))
       (if (= :invoke (:action v))
         (:fn v)
         (fn [e]
           ((state/state :jarman-views-service)
            :set-view
            :view-id (str "auto-menu-" (:key v))
            :title k
            :scrollable? false
            :component-fn (:fn v))))
       (fn [e] (println "\nProblem with fn in " k v))))
    ;; or return nil
    nil))

(defn- bulid-expand-by-map
  [plugin-m & {:keys [lvl] :or {lvl 0}}]
  (doall
   (map (fn [coll]
          (let [k (first  coll)
                v (second coll)]
            (cond
              (vmg/isPluginLink? v) (part-plugin k v lvl)
              (map? v) (if (nil? (:key v))
                         (part-expand bulid-expand-by-map k v lvl)
                         (part-button k v lvl))
              :else (c/label :text "Uncorrect comp"))))
        plugin-m)))

;;(println "\n" (bulid-expand-by-map (example-plugins-map)))

(defn- default-menu-items []
  {"Database"
   {"DB Visualizer" {:key "db-visualizer"
                     :fn    dbv/create-view--db-view}},
   
   "Debug Items"
   {"Popup window" {:key        "popup-window"
                    :action     :invoke
                    :permission [:developer]
                    :fn         (fn [e] (gcomp/popup-window {:relative (state/state :app)}))}
    
    "Alert"        {:key        "test-aletr"
                    :action     :invoke
                    :permission [:developer]
                    :fn         (fn [e] ((state/state :alert-manager)
                                         :set {:header "Czym jest Lorem Ipsum?"
                                               :body "Lorem Ipsum jest tekstem stosowanym jako przykładowy wypełniacz w przemyśle."}
                                         5))}
    
    "Select table"   {:key        "select-table"
                      :action     :invoke
                      :permission [:developer]
                      :fn         (fn [e] (gcomp/popup-window
                                           {:view (gcomp/select-box-table-list {})
                                            :relative (state/state :app) :size [250 40]}))}
    
    "Text multiline" {:key        "text-multiline"
                      :action     :invoke
                      :permission [:developer]
                      :fn         (fn [e]
                                    (gcomp/popup-window
                                     {:window-title "Text multiline"
                                      :relative (state/state :app)
                                      :size [250 250]
                                      :view (c/text
                                             :text "Some text"
                                             :size [300 :by 300]
                                             :editable? true
                                             :multi-line? true
                                             :wrap-lines? true)}))}
    
    "Rsyntax code editor" {:key        "rsyntax-code-editor"
                           :action     :invoke
                           :permission [:developer]
                           :fn         (fn [e]
                                         (gcomp/popup-window
                                          {:window-title "Code editor"
                                           :relative (state/state :app)
                                           :size [450 350]
                                           :view (gcomp/code-editor
                                                  {:dispose true
                                                   :val "(fn [x] (println \"Nice ass\" x)"})}))}
    
    "Popup demo" {:key        "popup-demo"
                  :action     :invoke
                  :permission [:developer]
                  :fn         (fn [e] (popup/set-demo))}
    
    "Popup demo 2" {:key        "popup-demo 2"
                    :action     :invoke
                    :permission [:developer]
                    :fn         (fn [e]
                                  (popup/build-popup
                                   {:comp-fn (fn []
                                               (gcomp/code-editor
                                                {:val "(fn [x] (println \"Nice ass\" x)"}))
                                    :title "Code in popup"
                                    :size [500 400]}))}}})



(defn- clean-main-menu []
  (state/set-state :jarmanapp--main-tree []))

(defn- load-static-main-menu []
  (clean-main-menu)
  (state/set-state
   [:jarmanapp--main-tree]
   (concat
    (bulid-expand-by-map (default-menu-items))
    [(create-expand-btns--confgen)])))


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

(defn- load-plugins-to-main-menu []
  (let [plugins-m (vmg/do-view-load)
        ;;plugins-m (example-plugins-map)
        ]
    (vmg/prepare-defview-editors-state)
    (add-to-main-tree
     (concat
      (state/state [:jarmanapp--main-tree])
      (bulid-expand-by-map plugins-m)))))

(state/state [:jarmanapp--main-tree])

;; ┌─────────────┐
;; │             │
;; │ Load stages │
;; │             │
;; └─────────────┘

;; before central swing-component build
(defn load-level-0 []
  (cm/swapp)
  (dot-jarman/dot-jarman-load)
  (dot-jarman-param/print-list-not-loaded))
;; after swing component was builded
(defn load-level-1 [])
(defn load-level-2 [])
(defn load-level-3 [])
(defn load-level-4 [])
;; halt application
(defn load-level-5 [])

;; ┌─────────────┐
;; │             │
;; │ App starter │
;; │             │
;; └─────────────┘

(def startup (atom nil))

(def run-me
  (fn []
    (let [relative (atom nil)]
      (load-level-0)
      (try
        (reset! relative [(.x (.getLocationOnScreen (seesaw.core/to-frame (state/state :app))))
                          (.y (.getLocationOnScreen (seesaw.core/to-frame (state/state :app))))])
        (.dispose (seesaw.core/to-frame (state/state :app)))
        (catch Exception e (println "Last pos is nil")))
      (gseed/build
       :items (let [img-scale 35
                    top-offset 2]
                (clean-main-menu)
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
    (load-static-main-menu)
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
