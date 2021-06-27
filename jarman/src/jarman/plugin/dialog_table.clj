(ns jarman.plugin.dialog-table
  (:require
   ;; Clojure toolkit 
   [clojure.string :as string]
   [clojure.spec.alpha :as s]
   [seesaw.util :as u]
   [seesaw.core :as c]
   [seesaw.border :as sborder]
   [seesaw.dev :as sdev]
   [seesaw.mig :as smig]
   [seesaw.swingx :as swingx]
   [seesaw.chooser :as chooser]
   ;; Jarman toolkit
   
   [jarman.tools.lang :refer :all]
   [jarman.tools.swing :as stool]
   [jarman.gui.gui-tools :as gtool]
   [jarman.gui.gui-seed :as gseed]
   [jarman.gui.gui-tutorials.key-dispacher-tutorial :as key-tut]
   [jarman.gui.gui-components :as gcomp]

   [jarman.logic.session :as session]
   [jarman.logic.sql-tool :refer [select!]]
   [jarman.logic.connection :as db]
   [jarman.logic.metadata :as mt]
   
   [jarman.plugin.spec :as spec]
   [jarman.plugin.gui-table :as gtable]
   [jarman.plugin.data-toolkit :as query-toolkit]))

(defn- popup-table [table-fn
                    ;; selected
                    ;; frame
                    ]
  (let [dialog (c/custom-dialog :modal? true :width 600 :height 400 :title "Select component")
        table (table-fn (fn [table-model] (c/return-from-dialog dialog table-model)))
        key-p (seesaw.mig/mig-panel
               :constraints ["wrap 1" "0px[grow, fill]0px" "5px[fill]5px[grow, fill]0px"]
               ;;  :border (sborder/line-border :color "#888" :bottom 1 :top 1 :left 1 :right 1)
               :items [[(c/label :text (gtool/get-lang :tips :press-to-search) :halign :center)]
                       ;;  [(c/label
                       ;;    :icon (stool/image-scale ico/left-blue-64-png 30)
                       ;;    :listen [:mouse-entered (fn [e] (gtool/hand-hover-on e))
                       ;;             :mouse-exited (fn [e] (gtool/hand-hover-off e))
                       ;;             :mouse-clicked (fn [e] (.dispose (c/to-frame e)))])]
                       [table]])
        key-p (key-tut/get-key-panel \q (fn [jpan] (.dispose (c/to-frame jpan))) key-p)]
    (c/config! dialog :content key-p :title (gtool/get-lang :tips :related-popup-table))
    ;; (.setUndecorated dialog true)
    ;; (.setLocationRelativeTo dialog frame)
    (c/show! dialog)))


(defn dialog-table [plugin-config plugin-toolkit]
  (popup-table (:table (gtable/create-table plugin-config plugin-toolkit))
               ;; field-qualified
               ;; (c/to-frame e)
               ))

(defn dialog-table-toolkit-pipeline [configuration]
  (let [toolkit (query-toolkit/data-toolkit-pipeline configuration {})]
    {:dialog (fn [] (dialog-table configuration toolkit))}))

(defn dialog-table-entry [plugin-path global-configuration])
