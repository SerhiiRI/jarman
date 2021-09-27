(ns plugin.table.dialog-table
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
   [jarman.plugin.data-toolkit :as query-toolkit]
   
   [jarman.logic.session :as session]
   [jarman.logic.sql-tool :refer [select!]]
   [jarman.logic.connection :as db]
   [jarman.logic.metadata :as mt]
   
   [jarman.plugin.spec :as spec]
   [jarman.plugin.gui-table :as gtable]
   [jarman.plugin.data-toolkit :as query-toolkit]
   [jarman.plugin.plugin]))

(defn- popup-table [table-fn id]
  (let [dialog (c/custom-dialog :modal? true :width 600 :height 400 :title "Select component")
        table (table-fn (fn [table-model] (println "RETURN TABLE" table-model)(c/return-from-dialog dialog table-model)))
        key-p (seesaw.mig/mig-panel
               :constraints ["wrap 1" "0px[grow, fill]0px" "5px[fill]5px[grow, fill]0px"]
               ;;  :border (sborder/line-border :color "#888" :bottom 1 :top 1 :left 1 :right 1)
               :items [[(c/label :text (gtool/get-lang :tips :press-to-search) :halign :center)]
                       [table]])
        key-p (key-tut/get-key-panel \q (fn [jpan] (.dispose (c/to-frame jpan))) key-p)]
    (c/config! dialog :content key-p :title (gtool/get-lang :tips :related-popup-table))
    (c/show! dialog)))

(defn dialog-table-component [plugin-config plugin-toolkit id]
  (popup-table (:table (gtable/create-table plugin-config plugin-toolkit)) id))


(defn dialog-table-toolkit-pipeline [configuration]
  (let [toolkit (query-toolkit/data-toolkit-pipeline configuration {})]
    (into toolkit {:dialog (fn [id] (dialog-table-component configuration toolkit id))})))

(defn dialog-table-entry [plugin-path global-configuration])


;;;;;;;;;;;;
;;; BIND ;;;
;;;;;;;;;;;;

(jarman.plugin.plugin/register-custom-view-plugin
 :name 'dialog-table 
 :description "Dialog table"
 :entry dialog-table-entry
 :toolkit dialog-table-toolkit-pipeline
 :spec-list
 [[:tables
   {:spec [:jarman.plugins.data-toolkit/tables :req-un],
    :doc "list of used tables"
    :examples "[:permission]"}]
  [:view-columns
   {:spec [:jarman.plugins.data-toolkit/view-columns :req-un],
    :doc "Columns which must be represented in table on right side"
    :examples "[:permission.permission_name 
                :permission.configuration]"}]
  [:query
   {:spec [:jarman.plugins.data-toolkit/query :req-un],
    :examples "{:table_name :permission, :column [:#as_is ...]...}",
    :doc "SQL syntax for `select!` query"}]])
