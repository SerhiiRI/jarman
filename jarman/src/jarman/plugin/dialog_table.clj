(ns jarman.plugin.dialog-table
  (:use seesaw.border
        seesaw.dev
        seesaw.mig
        seesaw.font
        seesaw.rsyntax)
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
   [jarman.logic.sql-tool]
   [jarman.logic.connection :as db]
   [jarman.tools.lang :refer :all]
   [jarman.gui.gui-tools :as gtool]
   [jarman.gui.gui-seed :as gseed]
   [jarman.tools.swing :as stool]
   ;; [jarman.logic.state :as state]
   [jarman.gui.gui-components :refer :all :as gcomp]
   [jarman.logic.metadata :as mt]
   [jarman.plugin.spec :as spec]
   [jarman.plugin.data-toolkit :as query-toolkit])
  (:import (java.util Date)
           (java.text SimpleDateFormat)))

#_(defn create-dialog--answer-chooser
  "Descrition
    create one label for dialog-window with big string and function, which returned on action"
  [txt func]
  (c/vertical-panel
   :background (gtool/get-color :background :button_main)
   :focusable? true
   :listen [:focus-gained   (fn [e] (gtool/hand-hover-on e) (c/config! e :background light-light-grey-color))
            :focus-lost     (fn [e] (c/config! e :background (gtool/get-color :background :button_main)))
            :mouse-entered  (fn [e] (gtool/hand-hover-on e) (c/config! e :background light-light-grey-color))
            :mouse-exited   (fn [e] (c/config! e :background (gtool/get-color :background :button_main)))
            :mouse-clicked   func
            :key-pressed  (fn [e] (if (= (.getKeyCode e) java.awt.event.KeyEvent/VK_ENTER) func))]
   :items (list (c/label
                 :text txt
                 :halign :left
                 :font (gtool/getFont 14)
                 :foreground gcomp/blue-green-color
                 :border (compound-border (empty-border :left 20 :right 20 :bottom 10 :top 10))))))

#_(defn create-dialog-repair-chooser
  "Description
    this func create dialog-window whith big strings, which gets from db,
    by table-name and name of column. The first label with data will be
    the data-string that the user choose last. For this we use last-id."
  [table-name column last-id]
  (let [data (db/query (select! {:table_name (keyword table-name)
                                 :column [:#as_is (keyword column) :id]}))
        data (sort-by (fn [data-map] (= (:id data-map) last-id)) data)        
        last-data ((keyword column) (last data))]
    (mig-panel
     :background "#fff"
     :constraints ["" "0px[fill, grow]0px" "0px[grow, top]0px"]
     :items (gtool/join-mig-items
             (min-scrollbox
              (mig-panel
               :background "#fff"
               :constraints ["wrap 1" "0px[fill, grow]0px" "0px[]0px"]
               :items (gtool/join-mig-items
                       (mig-panel
                        :background "#fff"
                        :constraints ["wrap 1" "0px[:300, fill, grow]0px" "0px[]0px"]
                        :items
                        (gtool/join-mig-items
                         (create-dialog--answer-chooser last-data
                                                        (fn [e] (c/return-from-dialog e last-data)))
                         (map (fn [data-map]
                                (if-not (= last-id (:id data-map))
                                  (create-dialog--answer-chooser ((keyword column) data-map)
                                                                 ;;; TO DO swap id to atom
                                                                 (fn [e] (c/return-from-dialog e (:id data-map))))))
                              (butlast data))))))
              :hscroll :never :border nil)))))

;; ;;start julka dialog
#_(do (doto (seesaw.core/frame
           :title "title"
           :undecorated? false
           :minimum-size [1000 :by 600]
           :content
           (seesaw.mig/mig-panel
            :constraints ["wrap 1" "10px[fill, grow]10px" "10px[top]10px"]
            :items [[(c/label :text "heyy open modal window"
                            :listen [:mouse-entered (fn [e] (c/config! e :cursor :hand))
                                     :mouse-clicked (fn [e]
                                                      (gcomp/popup-window {:window-title "Choose reason for repair"
                                                                           :view
                                                                           (create-dialog-repair-chooser
                                                                            :repair_reasons
                                                                            :description 4)
                                                                           :size [400 300]
                                                                           :relative (c/to-widget e)}))])]]))
      (.setLocationRelativeTo nil) c/pack! c/show!))


#_(defn- popup-table [table-fn selected frame]
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


;;; EXAMPLE DIALOG
(defn dialog []
  (-> (let [dialog (seesaw.core/custom-dialog :modal? true :width 600 :height 400 :title "Select component")
            table (let [TT (seesaw.core/table
                            :model [:columns [{:key :fname, :text "First Name"} {:key :lname :text "Last Name"}] 
                                    :rows [["Bobby" "Vinton"]
                                           ["Julia" "Burmych"]
                                           ["Serhii" "Riznychuk"]
                                           ["Sebastian" "Sinkowski"]]])]
                    (c/listen TT :selection (fn [e] (seesaw.core/return-from-dialog dialog (seesaw.table/value-at TT (c/selection TT)))))
                    TT)
            dialog-content (seesaw.mig/mig-panel
                            :constraints ["wrap 1" "0px[grow, fill]0px" "5px[fill]5px[grow, fill]0px"]
                            ;;  :border (sborder/line-border :color "#888" :bottom 1 :top 1 :left 1 :right 1)
                            :items [[(c/label :text "Dialog content" :halign :center)]
                                    [table]])]
        (seesaw.core/config! dialog :content dialog-content :title (gtool/get-lang :tips :related-popup-table))
        ;; (.setLocationRelativeTo dialog frame)
        (seesaw.core/show! dialog))))


(defn dialog-table-toolkit-pipeline [configuration]
  {:dialog dialog})

;;;PLUGINS ;;;        
(defn dialog-table-component [plugin-path global-configuration])
