(ns jarman.gui.gui-main-menu
  (:require [jarman.tools.lang :refer :all]
            [clojure.string :as string]
            [seesaw.core    :as c]
            [jarman.faces   :as face]
            [jarman.tools.update-manager-gui :as update-manager]
            [jarman.tools.org :refer :all]
            [jarman.plugin.extension-manager-gui :as extension-manager]
            [jarman.plugin.themes-manager-gui :as themes-manager]
            [jarman.config.vars-listing-gui   :as vars-listing]
            [jarman.gui.gui-components :as gcomp]
            [jarman.gui.gui-tools      :as gtool]
            [jarman.logic.state        :as state]
            [jarman.logic.session      :as session]
            [jarman.logic.view-manager       :as vmg]
            [jarman.gui.gui-views-service    :as gvs]
            [jarman.gui.gui-dbvisualizer     :as dbv]
            [jarman.gui.gui-config-generator :as cg]
            [jarman.gui.popup                :as popup]
            [jarman.tools.swing              :as stool]
            [jarman.gui.gui-editors          :as gedit]
            [jarman.interaction :as i]))


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

(defn- part-plugin
  [k v lvl]
  (if (session/allow-permission? (.return-permission v))
    (do
      (print-line (format "pin plugin %s to menu"(.return-title v)))
      (gcomp/button-expand-child
       (str k)
       :left-offset (+ (* lvl 3) 6)
       :c-focus     (second (nth (get-colors (dec lvl)) (dec lvl)))
       :c-left      (first  (nth (get-colors lvl) (if (= 0 lvl) lvl (dec lvl))))
       :background  (last   (nth (get-colors lvl) (if (= 0 lvl) lvl (dec lvl))))
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
      (if (= lvl 0) (c/label) nil)
      (gcomp/button-expand
       (str k)
       depper
       :left-offset (* lvl 3)
       :background (first (nth (get-colors lvl) lvl))
       :c-left     (first (nth (get-colors lvl) (if (= 0 lvl) lvl (dec lvl))))))))

(defn- part-button [k v lvl]
  (if (or (nil? (:permission v))
          (session/allow-permission? (:permission v)))
    (if (= :list (:action v))
      ((:fn v))
      (gcomp/button-expand-child
       (str k)
       :left-offset (+ (* lvl 3) 6)
       :c-focus     (second (nth (get-colors (dec lvl)) (dec lvl)))
       :c-left      (first  (nth (get-colors lvl) (if (= 0 lvl) lvl (dec lvl))))
       :background  (last   (nth (get-colors lvl) (if (= 0 lvl) lvl (dec lvl))))
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
           :else (c/label :text "Uncorrect comp"))))
     plugin-m))))

;;(println "\n" (bulid-expand-by-map (example-plugins-map)))

(defn demo-menu []
  {"LVL 1" {:key    "lvl 1-1"
            :action :invoke
            :fn      (fn [e] )}})

(defn default-menu-items []
  {"Menu demo"
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
                 :fn      (fn [e] )}}}}

   "Administration"
   {"Update manager" {:key "update-manager"
                      :fn update-manager/update-manager-panel}
    "Extension manager" {:key "extension-manager"
                         :fn extension-manager/extension-manager-panel}
    "Themes manager" {:key "themes-manager"
                      :fn themes-manager/theme-manager-panel}
    "Var listing" {:key "vars-listing-panel"
                   :fn vars-listing/vars-listing-panel}
    "DB Visualizer" {:key "db-visualizer"
                     :fn    dbv/create-view--db-view}
    (gtool/get-lang-btns :settings)
   {(gtool/get-lang-btns :settings) {:key    "settings"
                                     :action :list
                                     :fn     (fn [] (cg/create-expand-btns--confgen get-colors 2 12))}}}
   "Debug Items"
   {"Popup window" {:key        "popup-window"
                    :action     :invoke
                    :permission [:developer]
                    :fn         (fn [e] (gcomp/popup-window {:relative (state/state :app)}))}
    
    "Alerts"        {"Info" {:key        "test-info"
                             :action     :invoke
                             :permission [:developer]
                             :fn         (fn [e]
                                           (i/info "Czym jest Lorem Ipsum?"
                                                   "Lorem Ipsum jest tekstem stosowanym jako przykładowy wypełniacz w przemyśle."))}
                     "Warning" {:key        "test-warning"
                                :action     :invoke
                                :permission [:developer]
                                :fn         (fn [e]
                                              (i/warning "Czym jest Lorem Ipsum?"
                                                      "Lorem Ipsum jest tekstem stosowanym jako przykładowy wypełniacz w przemyśle."))}
                     "Danger" {:key        "test-danger"
                               :action     :invoke
                               :permission [:developer]
                               :fn         (fn [e]
                                             (i/danger "Czym jest Lorem Ipsum?"
                                                     "Lorem Ipsum jest tekstem stosowanym jako przykładowy wypełniacz w przemyśle."))}}
    
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
                                           :view (gedit/code-editor
                                                  {:dispose true
                                                   :val "(fn [x] (println \"Nice ass\" x)"})}))}

    "File editor" {:key        "demo-file-editor"
                   :action     :invoke
                   :permission [:developer]
                   :fn (fn [e] (i/editor "./test" "test-file.txt"))}
    
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
                                               (gedit/code-editor
                                                {:val "(fn [x] (println \"Nice ass\" x)"}))
                                    :title "Code in popup"
                                    :size [500 400]}))}}
   ;; (gtool/get-lang-btns :settings)
   ;; {(gtool/get-lang-btns :settings) {:key    "settings"
   ;;                                   :action :list
   ;;                                   :fn     (fn [] (cg/create-expand-btns--confgen get-colors))}}
   })



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
        (gtool/slider-ico-btn (stool/image-scale (:icon m) scale)
                              index scale (:title m)
                              :onClick (:fn m)
                              :top-offset offset))
      (filter-nil btns) (inc-fn)))))

