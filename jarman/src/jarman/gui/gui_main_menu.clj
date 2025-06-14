;; -*- mode: clojure; mode: rainbow; -*-
(ns jarman.gui.gui-main-menu
  (:require
   [seesaw.core                          :as seesaw]
   [jarman.org                           :refer :all]
   [jarman.lang                          :refer :all]
   [jarman.faces                         :as face]
   [jarman.logic.state                   :as state]
   [jarman.logic.connection              :as db]
   [jarman.logic.sql-tool                :as sql]
   [jarman.application.session           :as session]
   [jarman.logic.view-manager            :as vmg]
   ;; ---------------------------------------
   [jarman.gui.gui-views-service         :as gvs]
   [jarman.gui.gui-components            :as gcomp]
   [jarman.gui.gui-tools                 :as gtool]
   [jarman.gui.popup                     :as popup]
   [jarman.gui.gui-editors               :as gedit]
   [jarman.gui.gui-alerts-service        :as gas]
   [jarman.gui.components.swing          :as swing]
   ;; ---------------------------------------
   [jarman.gui.ui.gui-update-manager     :as gui-update-manager]
   [jarman.gui.ui.gui-extension-manager  :as gui-extension-manager]
   [jarman.gui.ui.gui-themes-manager     :as gui-themes-manager]
   [jarman.gui.ui.gui-vars-listing       :as gui-vars-listing]
   [jarman.gui.ui.gui-permission-listing :as gui-permission-listing]
   [jarman.gui.ui.gui-config-panel       :as gui-config-panel]))

;; (defn- expand-colors []
;;   [["#eeeeee" "#f7f7f7"]
;;    ["#d3ebe7" "#e9f5f3"]
;;    ["#d5f2db" "#ebfaee"]
;;    ["#dfecf7" "#f0f8ff"]])

;; (defn- expand-colors []
;;   [["#eeeeee" "#eeeeee"]
;;    ["#d3ebe7" "#d3ebe7"]
;;    ["#d5f2db" "#d5f2db"]
;;    ["#dfecf7" "#dfecf7"]])

;; (defn- expand-colors []
;;   [["#eeeeee" "#f7f7f7"]
;;    ["#dddddd" "#eeeeee"]
;;    ["#cccccc" "#dddddd"]
;;    ["#bbbbbb" "#cccccc"]])

;; (defn- expand-colors []
;;   [["#f7f7f7" "#f7f7f7"]
;;    ["#eeeeee" "#eeeeee"]
;;    ["#e7e7e7" "#e7e7e7"]
;;    ["#dddddd" "#dddddd"]])

(def expand-colors
  (fn []
   (if (empty? face/cvv-button-expand)
     [["#f7f7f7" "#fafafa" "#000"]
      ["#f0f6fa" "#f0f6fa" "#000"]
      ["#ebf7ff" "#ebf7ff" "#000"]
      ["#daeaf5" "#daeaf5" "#000"]
      ["#bfd3e0" "#bfd3e0" "#000"]]
     face/cvv-button-expand)))

(defn- repeat-colors
  [colors-fn loop]
  (reduce
   (fn [acc v] (concat acc v))
   (repeatedly loop colors-fn)))

(defn- get-colors [lvl]
  (let [colors-c (count (expand-colors))]
    (if (> (inc lvl) colors-c)
      (repeat-colors expand-colors (Math/ceil (/ (inc lvl) colors-c)))
      (expand-colors))))

;; ┌────────────────────────────┐
;; │                            │
;; │     Main menu builder      │
;; │                            │
;; └────────────────────────────┘

(defn- part-plugin
  [k v lvl]
  (if (.allow-permission? (session/session) (.return-permission v))
    (do
      (print-line (format "pin plugin %s to menu"(.return-title v)))
      (gcomp/button-expand-child
       (str k)
       :c-focus      (second (nth (get-colors (dec lvl)) (dec lvl)))
       ;; :offset-color (first  (nth (get-colors lvl) (if (= 0 lvl) lvl (dec lvl))))
       :background   (last   (nth (get-colors lvl) (if (= 0 lvl) lvl (dec lvl))))
       :seamless-bg false
       :onClick (fn [e]
                  (gvs/add-view
                   :view-id (str "auto-plugin" (.return-title v))
                   :title k
                   :render-fn (.return-entry v)))))
    (do (print-line (format "permission denied for View plugin '%s'" (.return-title v)))
        nil)))

(defn- part-expand [bulid-expand-by-map k v lvl]
  (let [depper (filter-nil (bulid-expand-by-map v :lvl (inc lvl)))]
    (if (empty? depper)
      (if (= lvl 0) (seesaw/label) nil)
      (gcomp/button-expand
       (str k)
       depper
       :lvl lvl
       :seamless-bg false
       :background   (first (nth (get-colors lvl) lvl))
       ;; :offset-color (first (nth (get-colors lvl) (if (= 0 lvl) lvl (dec lvl))))
       ))))

(defn- part-button [k v lvl]
  (if (or (nil? (:permission v))
         (.allow-permission? (session/session) (:permission v)))
    (if (= :list (:action v))
      ((:fn v))
      (gcomp/button-expand-child
       (str k)
       :lvl         lvl
       :c-focus      (second (nth (get-colors (dec lvl)) (dec lvl)))
       ;; :offset-color (first  (nth (get-colors lvl) (if (= 0 lvl) lvl (dec lvl))))
       :background   (last   (nth (get-colors lvl) (if (= 0 lvl) lvl (dec lvl))))
       :seamless-bg false
       :onClick
       (if-not (nil? (:fn v))
         (if (= :invoke (:action v))
           (:fn v)
           (fn [e]
             (gvs/add-view
              :view-id (str "auto-menu-" (:key v))
              :title k 
              :render-fn (:fn v))))
         (fn [e] (print-line (str "\nProblem with fn in " k v))))))
    ;; or return nil
    (do (print-line (format "permission denied for '%s'" k))
      nil)))
;;((state/state :startup))

(defn bulid-expand-by-map
  "Description:
    Build list of recursive button expand using configuration map."
  [plugin-m & {:keys [lvl] :or {lvl 0}}]
  (flatten
   (doall
    (map
     (fn [coll]
       (let [k (first  coll)
             v (second coll)]
         (cond
           (vmg/isPluginLink? v) (part-plugin k v lvl)
           (map? v) (if (nil? (:key v))
                      (part-expand bulid-expand-by-map k v lvl)
                      (part-button k v lvl))
           :else (seesaw/label :text "Uncorrect comp"))))
     plugin-m))))

;; ┌─────────────────────────────────────┐
;; │                                     │
;; │     Metadata and View editors       │
;; │                                     │
;; └─────────────────────────────────────┘

;;(println "\n" (bulid-expand-by-map (example-plugins-map)))

;; TODO: View editors
;; ((get (state/state :defview-editors) (keyword (:table_name (first selected-tab)))) e)
;; (state/state :defview-editors)

(defn- pullup-metadata-names
  "Description:
     Pull up from DB names of tables as key.
   Example:
     (pullup-metadata-names)
     ;; => (:documents :profile ...)" []
  (doall
   (map (fn [m] (keyword (:table_name m)))
        (db/query
         (sql/select!
          {:table_name :metadata
           :column [:table_name]})))))

(defn- metadata-editors-in-main-menu
  "Description:
     Prepare invokers for metadata editors.
     Compatibility with main menu map.
   Example:
     (metadata-editors-in-main-menu)
     ;; => {\"documents\" {:key ...}}" []
  (into {}(doall
            (map
              (fn [k]
                {(str (name k))
                 {:key    (str "edit-metadata-" (str (name k)))
                  :action :invoke
                  :fn      (fn [e] (gedit/view-metadata-editor k))}})
              (pullup-metadata-names)))))

;; (pullup-view-names)
;; fixme: organize view functionality in one place
(defn- pullup-view-names
  "Description:
     Pull up from DB names of tables as key.
   Example:
     (pullup-view-names)
     ;; => (:documents :profile ...)" []
  (doall
   (map (fn [m] (keyword (:table_name m)))
        (db/query
         (sql/select!
          {:table_name :jarman_view
           :column [:table_name]})))))

(defn- view-editors-in-main-menu
  "Description:
     Prepare invokers for metadata editors.
     Compatibility with main menu map.
   Example:
     (metadata-editors-in-main-menu)
     ;; => {\"documents\" {:key ...}}"
  []
  (into {}(doall
          (map
           (fn [k]
             {(str (name k))
              {:key    (str "edit-metadata-" (str (name k)))
               :action :invoke
               :fn      (fn [e] (gedit/view-view-editor k))}})
           (pullup-view-names)))))


;; ┌──────────────────────┐
;; │                      │
;; │      Main Menu       │
;; │                      │
;; └──────────────────────┘

(defn- return-menu-items-demo []
  {"lvl-1"
    {"lvl-11"
      {:key    "lvl 3-1"
       :action :invoke
       :fn      (fn [e] )}
     
     "lvl-2"
     {"lvl-31"
      {"lvl-3-1" {:key    "lvl 3-1"
                  :action :invoke
                  :fn      (fn [e] )}
       "lvl-3-2" {:key    "lvl 3-2"
                  :action :invoke
                  :fn      (fn [e] )}}
      "lvl-32"
      {"lvl-3-4" {:key    "lvl 3-1"
                  :action :invoke
                  :fn      (fn [e] )}
       "lvl-3-5" {:key    "lvl 3-2"
                  :action :invoke
                  :fn      (fn [e] )}}}
     "lvl-22"
     {"lvl-2-1" {:key    "lvl 3-1"
                 :action :invoke
                 :fn      (fn [e] )}
      "lvl-2-2" {:key    "lvl 3-2"
                 :action :invoke
                 :fn      (fn [e] )}}}})

(defn default-menu-items []
  {"Menu demo" (return-menu-items-demo)

   "Administration"
   {(gtool/get-lang-btns :settings)          {:key (gtool/get-lang-btns :settings)
                                              :fn gui-config-panel/config-panel}
    (gtool/get-lang-btns :update-manager)    {:key (gtool/get-lang-btns :update-manager)
                                              :permission :admin-update
                                              :fn gui-update-manager/update-manager-panel}
    (gtool/get-lang-btns :extension-manager) {:key (gtool/get-lang-btns :extension-manager)
                                              :permission :admin-extension
                                              :fn gui-extension-manager/extension-manager-panel}
    (gtool/get-lang-btns :theme-manager)     {:key (gtool/get-lang-btns :theme-manager)
                                              :fn gui-themes-manager/theme-manager-panel}
    "System information"
    {(gtool/get-lang-btns :var-list-panel)        {:key (gtool/get-lang-btns :var-list-panel)
                                                   :permission :developer
                                                   :fn gui-vars-listing/vars-listing-panel}
     (gtool/get-lang-btns :permission-list-panel) {:key (gtool/get-lang-btns :permission-list-panel)
                                                   :permission :developer
                                                   :fn gui-permission-listing/permission-listing-panel}}}

   "Metadata Editors" (metadata-editors-in-main-menu)
   "View edit"        (view-editors-in-main-menu)
   
   "Debug Items"
   {"Popup window" {:key        "popup-window"
                    :action     :invoke
                    :permission :developer
                    :fn         (fn [e] (gcomp/popup-window {:relative (state/state :app)}))}
    
    "Alerts"        {"Info" {:key        "test-info"
                             :action     :invoke
                             :permission :developer
                             :fn         (fn [e]
                                           (gas/info "Czym jest Lorem Ipsum?"
                                                   "Lorem Ipsum jest tekstem stosowanym jako przykładowy wypełniacz w przemyśle."))}
                     "Warning" {:key        "test-warning"
                                :action     :invoke
                                :permission :developer
                                :fn         (fn [e]
                                              (gas/warning "Czym jest Lorem Ipsum?"
                                                      "Lorem Ipsum jest tekstem stosowanym jako przykładowy wypełniacz w przemyśle."))}
                     "Danger" {:key        "test-danger"
                               :action     :invoke
                               :permission :developer
                               :fn         (fn [e]
                                             (gas/danger "Czym jest Lorem Ipsum?"
                                                       "Lorem Ipsum jest tekstem stosowanym jako przykładowy wypełniacz w przemyśle."))}
                     "Success" {:key       "test-success"
                               :action     :invoke
                               :permission :developer
                               :fn         (fn [e]
                                             (gas/success "Czym jest Lorem Ipsum?"
                                                        "Lorem Ipsum jest tekstem stosowanym jako przykładowy wypełniacz w przemyśle."))}
                     "Update" {:key       "test-update"
                               :action     :invoke
                               :permission :developer
                               :fn         (fn [e] (gui-update-manager/check-update))}}
    
    ;; "Select table"   {:key        "select-table"
    ;;                   :action     :invoke
    ;;                   :permission :developer
    ;;                   :fn         (fn [e] (gcomp/popup-window
    ;;                                        {:view (gcomp/select-box-table-list {})
    ;;                                         :relative (state/state :app) :size [250 40]}))}
    
    "Text multiline" {:key        "text-multiline"
                      :action     :invoke
                      :permission :developer
                      :fn         (fn [e]
                                    (gcomp/popup-window
                                     {:window-title "Text multiline"
                                      :relative (state/state :app)
                                      :size [250 250]
                                      :view (seesaw/text
                                             :text "Some text"
                                             :size [300 :by 300]
                                             :editable? true
                                             :multi-line? true
                                             :wrap-lines? true)}))}
    
    "Rsyntax code editor" {:key        "rsyntax-code-editor"
                           :action     :invoke
                           :permission :developer
                           :fn         (fn [e]
                                         (gcomp/popup-window
                                          {:window-title "Code editor"
                                           :relative (state/state :app)
                                           :size [450 350]
                                           :view (gedit/code-editor
                                                  {:dispose true
                                                   :val "(fn [x] (println \"Nice ass\" x)"})}))}

    "File editor" {:key        "demo-file-editor"
                   :action     :invoke
                   :permission :developer
                   :fn (fn [e] (gedit/editor "./test/test-file.txt"))}

    "File editor clj" {:key        "demo-file-editor-clj"
                   :action     :invoke
                   :permission :developer
                   :fn (fn [e] (gedit/editor "./test/test-file.clj"))}
    
    "Popup demo" {:key        "popup-demo"
                  :action     :invoke
                  :permission :developer
                  :fn         (fn [e] (popup/set-demo))}
    
    "Popup demo 2" {:key        "popup-demo 2"
                    :action     :invoke
                    :permission :developer
                    :fn         (fn [e]
                                  (popup/build-popup
                                   {:comp-fn (fn []
                                               (gedit/code-editor
                                                {:val "(fn [x] (println \"Nice ass\" x)"}))
                                    :title "Code in popup"
                                    :size [500 400]}))}}})



(defn clean-main-menu []
  (state/set-state :main-menu []))

(defn add-to-main-tree [components-coll]
  (let [path [:main-menu]]
    (swap! (state/get-atom) (fn [state] (assoc-in state path (concat (state path) components-coll))))))

(defn menu-slider
  "Description:
     Return list of buttons in columne, one by one for jlayeredpane"
  [scale offset btns]
  (let [inc-fn #(range 0 (count btns) )]
    (doall
     (map
      (fn [m index]
        (gtool/slider-ico-btn (swing/image-scale (:icon m) scale)
                              index scale (:title m)
                              :onClick (:fn m)
                              :top-offset offset))
      (filter-nil btns) (inc-fn)))))

