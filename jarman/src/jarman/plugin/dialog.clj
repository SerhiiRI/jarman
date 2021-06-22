(ns jarman.plugin.dialog
  (:require
   ;; Clojure toolkit 
   [clojure.data :as data]
   [clojure.string :as string]
   [clojure.spec.alpha :as s]
   [seesaw.util :as u]
   ;; Dev tools
   [jarman.logic.session :as session]
   ;; Seesaw components
   [seesaw.core :as c]
   [seesaw.border :as sborder]
   [seesaw.dev :as sdev]
   [seesaw.mig :as smig]
   [seesaw.swingx :as swingx]
   [seesaw.chooser :as chooser]
   ;; Jarman toolkit
   [jarman.logic.document-manager :as doc]
   [jarman.tools.lang :refer :all]
   [jarman.gui.gui-tools :refer :all :as gtool]
   [jarman.gui.gui-seed :as gseed]
   [jarman.resource-lib.icon-library :as ico]
   [jarman.tools.swing :as stool]
   [jarman.logic.state :as state]
   [jarman.gui.gui-components :refer :all :as gcomp]
   [jarman.gui.gui-calendar :as calendar]
   [jarman.logic.metadata :as mt]
   [jarman.plugin.spec :as spec]
   [jarman.gui.gui-tutorials.key-dispacher-tutorial :as key-tut])
  (:import (java.util Date)
           (java.text SimpleDateFormat)))

(def create-dialog-repair-chooser
  (fn [invoker data title size]
    (let [dialog
          (c/custom-dialog
           :title title
           :modal? true
           :resizable? false
           :size [(first size) :by (second size)] ;; here you need to set window size for calculating middle 
           :content (mig-panel
                     :background "#fff"
                     :size [(first size) :by (second size)]
                     :constraints ["" "0px[fill, grow]0px" "0px[grow, top]0px"]
                     :items (gtool/join-mig-items
                             (let [scr (c/scrollable
                                        (mig-panel
                                         :background "#fff"
                                         :constraints ["wrap 1" "0px[fill, grow]0px" "0px[]0px"]
                                         :items (gtool/join-mig-items
                                                 (seesaw.core/label
                                                  :border (empty-border :top 5)
                                                  :icon (stool/image-scale icon/left-blue-64-png 30)
                                                  :listen [:mouse-entered (fn [e] (gtool/hand-hover-on e))
                                                           :mouse-exited (fn [e] (gtool/hand-hover-off e))
                                                           :mouse-clicked (fn [e] (.dispose (seesaw.core/to-frame e)))])
                                                 (textarea title :halign :center :font (gtool/getFont 14 :bold)
                                                           :foreground gcomp/blue-green-color
                                                           :border (empty-border :thickness 12))
                                                 (mig-panel
                                                  :background "#fff"
                                                  :constraints ["wrap 1" "0px[:300, fill, grow]0px" "0px[]0px"]
                                                  :items (gtool/join-mig-items
                                                          (map (fn [x] (create-dialog--answer-chooser
                                                                        x
                                                                        (fn [e] (c/return-from-dialog e x)))) data))))) :hscroll :never :border nil)]
                               (.setPreferredSize (.getVerticalScrollBar scr) (Dimension. 0 0))
                               (.setUnitIncrement (.getVerticalScrollBar scr) 20) scr))))]
      (.setUndecorated dialog true)
      (doto dialog (.setLocationRelativeTo (c/to-root invoker)) c/pack! c/show!) ;; here should be relative to root of invoker
      )))

(defn- popup-table [table-fn selected frame]
  (let [dialog (seesaw.core/custom-dialog :modal? true :width 600 :height 400 :title "Select component")
        table (table-fn (fn [table-model] (seesaw.core/return-from-dialog dialog table-model)))
        key-p (seesaw.mig/mig-panel
               :constraints ["wrap 1" "0px[grow, fill]0px" "5px[fill]5px[grow, fill]0px"]
              ;;  :border (sborder/line-border :color "#888" :bottom 1 :top 1 :left 1 :right 1)
               :items [[(c/label :text (gtool/get-lang :tips :press-to-search) :halign :center)]
                      ;;  [(seesaw.core/label
                      ;;    :icon (stool/image-scale ico/left-blue-64-png 30)
                      ;;    :listen [:mouse-entered (fn [e] (gtool/hand-hover-on e))
                      ;;             :mouse-exited (fn [e] (gtool/hand-hover-off e))
                      ;;             :mouse-clicked (fn [e] (.dispose (seesaw.core/to-frame e)))])]
                       [table]])
        key-p (key-tut/get-key-panel \q (fn [jpan] (.dispose (seesaw.core/to-frame jpan))) key-p)]
    (seesaw.core/config! dialog :content key-p :title (gtool/get-lang :tips :related-popup-table))
    ;; (.setUndecorated dialog true)
    (.setLocationRelativeTo dialog frame)
    (seesaw.core/show! dialog)))

(defn table-toolkit-pipeline [configuration datatoolkit]
  datatoolkit)

;;;PLUGINS ;;;        
(defn table-component [plugin-path global-configuration spec-map]
  ;; (println "Loading table plugin")
  (let [get-from-global #(->> % (join-vec plugin-path) (get-in (global-configuration)))
        data-toolkit  (get-from-global [:toolkit])
        configuration (get-from-global [:config])
        ;; title (get-in data-toolkit [:table-meta :representation])
        title (:name configuration)
        space (c/select (state/state :app) (:plug-place configuration))
        ;; x (println "\nplug-place"(:plug-place configuration) "\nspace"space)
        atm (:atom-expanded-items (c/config space :user-data))]
    ;; (println "\nData toolkit" data-toolkit)
    ;; (println "Allow Permission: " (session/allow-permission? (:permission configuration)))
    ;; TODO: Set invoker expand button if not exist add child invokers
    (if (false? (spec/test-keys-jtable configuration spec-map))
      (println "[ Warning ] plugin/table: Error in spec")
      (if (session/allow-permission? (:permission configuration))
        (do
          (swap! atm (fn [inserted]
                       (conj inserted
                             (gcomp/button-expand-child
                              title
                              :onClick (fn [e]
                                        ;;  (println "\nplugin-path\n" plugin-path title)
                                         ((state/state :jarman-views-service)
                                          :set-view
                                          :view-id (str "auto-" title)
                                          :title title
                                          :scrollable? false
                                          :component-fn (fn [] (auto-builder--table-view
                                                                (global-configuration)
                                                                data-toolkit
                                                                configuration)))))))))))
    (.revalidate space)))


