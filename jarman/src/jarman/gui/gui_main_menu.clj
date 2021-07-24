(ns jarman.gui.gui-main-menu
  (:require [jarman.tools.lang :refer :all]
            [clojure.string :as string]
            [seesaw.core    :as c]
            [jarman.gui.gui-components :as gcomp]
            [jarman.gui.gui-tools      :as gtool]
            [jarman.logic.state        :as state]
            [jarman.logic.session      :as session]
            [jarman.logic.view-manager       :as vmg]
            [jarman.gui.gui-dbvisualizer     :as dbv]
            [jarman.gui.gui-config-generator :as cg]
            [jarman.gui.popup                :as popup]))


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
    (if (= :list (:action v))
      ((:fn v))
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
         (fn [e] (println "\nProblem with fn in " k v)))))
    ;; or return nil
    nil))

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

(defn default-menu-items []
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
                                    :size [500 400]}))}}
   (gtool/get-lang-btns :settings)
   {(gtool/get-lang-btns :settings) {:key    "settings"
                                     :action :list
                                     :fn     cg/create-expand-btns--confgen}}})



(defn clean-main-menu []
  (state/set-state :main-menu []))

(defn add-to-main-tree [components-coll]
  (let [path [:main-menu]]
    (swap! (state/get-atom) (fn [state] (assoc-in state path (concat (state path) components-coll))))))
