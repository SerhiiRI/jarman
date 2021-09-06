(ns plugin.dialog-test.dialog-test
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
   [jarman.logic.sql-tool :refer [select!]]
   [jarman.logic.connection :as db]
   [jarman.tools.lang :refer :all]
   [jarman.gui.gui-tools :as gtool]
   [jarman.gui.gui-seed :as gseed]
   [jarman.tools.swing :as stool]
   ;; [jarman.logic.state :as state]
   [jarman.gui.gui-components :refer :all :as gcomp]
   [jarman.logic.metadata :as mt]
   [jarman.plugin.spec :as spec]
   [jarman.plugin.data-toolkit :as query-toolkit]
   [jarman.plugin.plugin]))


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

(defn dialog-test-toolkit-pipeline [configuration]
  {:dialog dialog})

;;;PLUGINS ;;;        
(defn dialog-test-entry [plugin-path global-configuration])


(jarman.plugin.plugin/register-custom-view-plugin
 :name 'dialog-table 
 :description "Plugin allow to editing One table from database"
 :entry dialog-test-entry
 :toolkit dialog-test-toolkit-pipeline
 :spec-list [])
