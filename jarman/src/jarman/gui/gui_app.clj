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
            ;; logics
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


(def popup-menager (atom nil))

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
           (do (println "[ Warning ] Can not loading configs: " (:output (cm/get-in-segment [])))
               [(gcomp/button-expand "Settings Error" []
                               :ico (stool/image-scale icon/alert-64-png 25)
                               :ico-hover (stool/image-scale icon/alert-64-png 25)
                               :onClick (fn [e] ((state/state :alert-manager)
                                                 :set {:header "Settings Error"
                                                       :body (str "Valid output: " (:output (cm/get-in-segment [])))}
                                                 5)))]) 
           [(gcomp/button-expand
             (gtool/get-lang-btns :settings)
             (let [current-theme (str (first (cm/get-in-value [:themes :theme_config.edn :selected-theme])) ".edn")
                   config-file-list-as-keyword (map #(first %) (cm/get-in-segment []))
                   config-file-list-as-keyword-to-display (filter #(let [map-part (cm/get-in-segment (if (vector? %) % [%]))]
                                                                     (and (= :file (get map-part :type))
                                                                          (= :edit (get map-part :display))))
                                                                  config-file-list-as-keyword)
                   restore-button (gcomp/button-expand-child (gtool/get-lang-btns :restore-last-configuration)
                                                       :onClick (fn [e] (do
                                                                          (if-not (nil? (cm/restore-config)) ((state/state :alert-manager)
                                                                                                              :set {:header "Success!"
                                                                                                                    :body (gtool/get-lang-alerts
                                                                                                                           :restore-configuration-ok)}
                                                                                                              5)))))]
               (reverse
                (conj
                 (map (fn [p]
                        (let [path (if (vector? p) p [p])
                              title (get (cm/get-in-segment path) :name)
                              view-id (last path)]
                          (gcomp/button-expand-child title :onClick (fn [e]
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
                   (gcomp/button-expand-child title :onClick (fn [e]
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
                   (gcomp/button-expand-child title :onClick (fn [e]
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
                 ))))])))

;; ┌──────────────────────────┐
;; │                          │
;; │ App layout and GUI build │
;; │                          │
;; └──────────────────────────┘

(def jarmanapp--main-tree
  "Description:
      Vertical layout of elements, left part of app for functions
   Example:
      (jarmanapp--main-tree  [(button-expand 'Ukryte opcje 1' [(some-button)])] [(button-expand 'Ukryte opcje 2')])
   Needed:
      button-expand component is needed to corectly work
   "
  (fn [& args] (gcomp/scrollbox (mig-panel
                                     :id :expand-menu-space
                                     :background "#fff"
                                     :border (b/line-border :left 4 :right 4 :color "#fff")
                                     :constraints ["wrap 1" "0px[fill, grow]0px" "0px[fill]0px"]
                                     :items (vec args))
                                :hbar-size 0
                                :vbar-size 0)))



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
      (println tabs)
      (state/set-state :jarman-views-service (vs/new-views-service tabs-space views-space))
      (mig-panel
       :id :operation-space
       :background "#eee"
       :constraints ["wrap 1" "0px[grow, fill]0px" "0px[28, shrink 0]0px[grow, fill]0px"]
       :background "#eee"
       ;; :border (line-border :left 1 :color "#999")
       :items [[(gcomp/min-scrollbox tabs-space :vscroll :never)]
               [views-space]]))))

;; ((state/state :jarman-views-service) :reload :view-id (keyword "DB Visualiser"))
;; ((state/state :jarman-views-service) :get-all-view) 
(defn create-period--period-form
  []
  (gcomp/vmig
   :vrules "[fill][100, shrink 0, fill][grow, fill]"
   :items [[(gcomp/header-basic "Okresy")]
           [(gcomp/min-scrollbox
             (mig-panel :constraints ["wrap 4" "10px[fill][fill]50px[fill][fill]10px" "10px[fill]10px"]
                        :items [[(c/label :text "Organization:")]
                                [(c/label :text "Frank & Franky Co." :border (b/line-border :bottom 1 :color "#494949"))]
                                [(c/label :text "Time:")]
                                [(c/label :text "12/03/2021 - 11/03/2022"  :border (b/line-border :bottom 1 :color "#494949"))]
                                [(c/label :text "Customer:")]
                                [(c/label :text "Franklyn Badabumc" :border (b/line-border :bottom 1 :color "#494949"))]
                                [(c/label :text "Full amount:")]
                                [(c/label :text "7000,-" :border (b/line-border :bottom 1 :color "#494949"))]
                                [(c/label :text "Service:")]
                                [(c/label :text "Mr. Jarman" :border (b/line-border :bottom 1 :color "#494949"))]])
             :vscroll :never)]
           [(gcomp/vmig
             :vrules "[fill]0px[grow, fill]"
             :items [[(gcomp/menu-bar
                       {:justify-end true
                        :id :menu-for-period-table
                        :buttons [[(gtool/get-lang-btns :save) icon/agree1-blue-64-png "" (fn [e])]
                                  [(gtool/get-lang-btns :export) icon/excel-64-png "" (fn [e])]]})]
                     [(c/scrollable (seesaw.swingx/table-x :model [:columns ["Servise month" "Amount" "Payment status"] :rows [["03/2021" "2500,-" "FV: 042/03/2021"]
                                                                                                                             ["04/2021" "2000,-" "FV: 042/04/2021"]
                                                                                                                             ["05/2021" "2500,-" "Expected payment"]]]))]])]]))

(defn get-period-list
  [company-id]
  (cond
    (= company-id 1) {}))

(defn create-period--period-list
  [list-space view-space return-fn company-id]
  (let [;; period-list (get-period-list company-id)
        auto-menu-hide false
        return (gcomp/button-slim (str "<< " (gtool/get-lang-btns :back))
                                  :underline-size 1
                                  :onClick (fn [e] (c/invoke-later (do
                                                                   (c/config! list-space :items (gtool/join-mig-items (return-fn list-space view-space return-fn)))
                                                                   (gtool/switch-focus)))))
        periods (gcomp/expand-form-panel
                 list-space
                 [return
                  (gcomp/scrollbox
                   (gcomp/vmig
                    :items (gtool/join-mig-items
                            (gcomp/vmig
                             :items (gtool/join-mig-items
                                     (gcomp/button-slim "01/01/2021 - 31/12/2021"
                                                        :onClick (fn [e]
                                                                   (c/config! view-space :items (gtool/join-mig-items (create-period--period-form)))
                                                                   (if auto-menu-hide ((:hide-show (c/config (c/select list-space [:#expand-panel]) :user-data))))))
                                     (gcomp/button-slim "01/01/2021 - 31/12/2021" :onClick (fn [e]))
                                     (gcomp/button-slim "01/01/2021 - 31/12/2021" :onClick (fn [e]))
                                     (gcomp/button-slim "01/01/2021 - 31/12/2021" :onClick (fn [e]))
                                     (gcomp/button-slim "01/01/2021 - 31/12/2021" :onClick (fn [e])))))))]
                 :max-w 180
                 :args [:id :expand-panel :background "#fff"])]
    (gtool/set-focus return)
    return
    periods))


(defn get-company-list
  [] [{:name "Trashpanda-Team" :id 1} {:name "Frank & Franky" :id 3}
      {:name "Trashpanda-Team" :id 1} {:name "Trashpandowe Zakłady Wyrobów Kodowych Team" :id 3}
      {:name "Trashpanda-Team" :id 1} {:name "Frank & Franky" :id 3}])

(defn create-period--period-companys-list
  [list-space view-space return-fn] ;; [{:name "Frank & Franky" :id 3}]
  (gtool/rm-focus)
  (let
   [model (get-company-list)
    buttons (map (fn [enterpreneur]
                   (let [name (:name enterpreneur)
                         btn (gcomp/button-slim name
                                                :onClick (fn [e] (c/invoke-later (do
                                                                                 (c/config! list-space :items (gtool/join-mig-items (create-period--period-list list-space view-space return-fn (get enterpreneur :id))))
                                                                                 (gtool/switch-focus))))
                                                :args [:tip name])]
                     (gtool/set-focus-if-nil btn)
                     btn))
                 model)]
    (gcomp/expand-form-panel
     list-space
     (gcomp/vmig :items (gtool/join-mig-items buttons))
     :max-w 180
     :args [:background "#fff"])))


(defn create-period-view
  []
  (let [list-space (gcomp/vmig :args [:border (b/line-border :left 1 :right 1 :color "#eee")])
        view-space (gcomp/vmig)
        list-space (c/config! list-space :items (gtool/join-mig-items (create-period--period-companys-list list-space view-space create-period--period-companys-list)))]
    (gcomp/hmig
     :hrules "[shrink 0, fill]0px[grow, fill]"
     :items [[list-space]
             [view-space]]
     :args [:background "#fff"])))



(def jarmanapp
  "Description:
      Main space for app inside JLayeredPane. There is menu with expand btns and space for tables with tab btns.
   "
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
       :items [;; [(c/label-fn :background "#eee" :size [50 :by 50])]
               [(jarmanapp--main-tree  [(gcomp/button-expand "Database"
                                                       [(gcomp/button-expand-child
                                                         "DB Visualiser"
                                                         :onClick (fn [e] 
                                                                    (try 
                                                                      ((state/state :jarman-views-service) :set-view :view-id "DB Visualiser" :title "DB Visualiser" :component-fn dbv/create-view--db-view)
                                                                      (catch Exception e ((state/state :alert-manager) :set {:header "Exception" :body (.getMessage e)} 5)))))
                                                        ;;  (button-expand-child "Users table" :onClick (fn [e] ((state/state :jarman-views-service) :set-view :view-id "tab-user" :title "User" :scrollable? false :component (jarman.logic.view/auto-builder--table-view nil))))
                                                        ])]
                                       [(gcomp/button-expand "Tables" [] :id :tables-view-plugin :expand :yes)]
                                      
                                       [(gcomp/button-expand "Okresy" [(gcomp/button-expand-child "Okresy" :onClick (fn [e] ((state/state :jarman-views-service) :set-view :view-id "okresy" :title "Okresy" :component-fn create-period-view)))])]
                                       (create-expand-btns--confgen)
                                       [(gcomp/button-expand "Debug items"
                                                       [;;(button-expand-child "Popup" :onClick (fn [e] (@popup-menager :new-message :title "Hello popup panel" :body (c/label "Hello popup!") :size [400 200])))
                                                        ;;(button-expand-child "Dialog" :onClick (fn [e] (println (str "Result = " (@popup-menager :yesno :title "Ask dialog" :body "Do you wona some QUASĄĄĄĄ?" :size [300 100])))))
                                                        (gcomp/button-expand-child "Popup window" :onClick (fn [e] (gcomp/popup-window {:relative (state/state :app)})))
                                                        (gcomp/button-expand-child "alert" :onClick (fn [e] ((state/state :alert-manager) :set {:header "Czym jest Lorem Ipsum?" :body "Lorem Ipsum jest tekstem stosowanym jako przykładowy wypełniacz w przemyśle poligraficznym. Został po raz pierwszy użyty w XV w. przez nieznanego drukarza do wypełnienia tekstem próbnej książki. Pięć wieków później zaczął być używany przemyśle elektronicznym, pozostając praktycznie niezmienionym. Spopularyzował się w latach 60. XX w. wraz z publikacją arkuszy Letrasetu, zawierających fragmenty Lorem Ipsum, a ostatnio z zawierającym różne wersje Lorem Ipsum oprogramowaniem przeznaczonym do realizacji druków na komputerach osobistych, jak Aldus PageMaker"} 5)))
                                                        (gcomp/button-expand-child "Select table" :onClick (fn [e] (gcomp/popup-window {:view (gcomp/select-box-table-list {}) :relative (state/state :app) :size [250 40]})))
                                                        (gcomp/button-expand-child
                                                         "Text multiline"
                                                         :onClick (fn [e]
                                                                    (gcomp/popup-window {:window-title "Text multiline"
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
                                                                    (gcomp/popup-window {:window-title "Code editor"
                                                                                         :relative (state/state :app)
                                                                                         :size [450 350]
                                                                                         :view (gcomp/code-editor
                                                                                                {:dispose true
                                                                                                 :val "(fn [x] (println \"Nice ass\" x)"})})))])])]
               [(jarmanapp--main-view-space [] [])]]))))


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
        ;; (println "last pos" [(.x (.getLocationOnScreen (seesaw.core/to-frame (state/state :app)))) (.y (.getLocationOnScreen (seesaw.core/to-frame (state/state :app))))])
        (reset! relative [(.x (.getLocationOnScreen (seesaw.core/to-frame (state/state :app)))) (.y (.getLocationOnScreen (seesaw.core/to-frame (state/state :app))))])
        (.dispose (seesaw.core/to-frame (state/state :app)))
        (catch Exception e (println "Last pos is nil")))
      (gseed/build :items (let [img-scale 35
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
                             (gcomp/fake-focus :vgap top-offset :hgap img-scale)
                             ;; @atom-popup-hook
                             )))
      ;; (reset! popup-menager (create-popup-service atom-popup-hook))
      (if-not (nil? @relative) (.setLocation (seesaw.core/to-frame (state/state :app)) (first @relative) (second @relative))))
    (gseed/extend-frame-title (str ", " (session/user-get-login) "@" (session/user-get-permission)))
    (vmg/do-view-load)
    ;; (vmg/buttons-list--code-editor-defview :#expand-menu-space)
    (vmg/prepare-defview-editors-state)
    ))


(reset! startup
        (fn []
          (cond (= (iinit/validate-configuration-files) true)
                (run-me)
                :else (cond (= (iinit/restore-backup-configuration) false)
                            (do
                              ;; (reset! popup-menager (create-popup-service atom-popup-hook))
                              ;; (@popup-menager :ok :title "App start failed" :body "Cennot end restore task." :size [300 100])
                              )
                            :else (do
                                    (= (iinit/validate-configuration-files) true)
                                    (run-me)
                                    :else (do
                                            ;; (reset! popup-menager (create-popup-service atom-popup-hook))
                                            ;; (@popup-menager :ok :title "App start failed" :body "Restor failed. Some files are missing." :size [300 100])
                                            ))))))

(@startup)
