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

   ;; --
   [jarman.gui.components.dialog :as dialog]
   [jarman.logic.view-manager :as view-manager]
   ;; -- 
   
   [jarman.plugin.spec :as spec]
   [jarman.plugin.gui-table :as gtable]
   [jarman.plugin.data-toolkit :as query-toolkit]
   [jarman.interaction]))


(defn dialog-bigstring-component
  "Description
    this func create dialog-window whith big strings, which gets from db,
    by table-name and name of column. The first label with data will be
    the data-string that the user choose last. For this we use last-id."
  [item-list item-getter id-getter & [selected-id]]
  (let [selected-element (atom {})
        dialog (seesaw.core/custom-dialog :modal? true :width 600 :height 400 :title "Select component")
        dialog-content (seesaw.mig/mig-panel
                        :background "#fff"
                        :constraints ["wrap 1" "0px[fill, grow]0px" "0px[]0px"]
                        :items (gtool/join-mig-items
                                (seesaw.mig/mig-panel
                                 :background "#fff"
                                 :constraints ["wrap 1" "0px[fill, grow]0px" "0px[]0px"]
                                 :items
                                 (gtool/join-mig-items
                                  (if (empty? item-list)
                                    (dialog-bigstring-item "[!] Empty DB" (fn [e] (seesaw.core/return-from-dialog dialog nil)) 0)
                                    (map (fn [m]
                                           (let [item-text (item-getter m)
                                                 selected? (= (id-getter m) selected-id)
                                                 return-function (fn [e] (seesaw.core/return-from-dialog dialog m))
                                                 item (dialog-bigstring-item
                                                       item-text
                                                       return-function
                                                       selected?)]
                                             (if selected?
                                               (swap! selected-element {:id (id-getter m) :item item})) item))
                                         item-list))))))
        dialog-content-scroll (gcomp/min-scrollbox
                               ;;seesaw.core/scrollable
                               dialog-content
                               :hscroll :never :border nil)]
    (seesaw.core/config! dialog :content dialog-content-scroll :title "Chooser")
    ;; seesaw.core/scroll!  -> use this func to component (Jlabel, JPanel, etc) !! not to scrollable 
    (if-not (nil? selected-id)
      (seesaw.core/listen dialog :window-activated (fn [e] (do (seesaw.core/scroll! dialog-content :to [:point 0
                                                                                                        (let [y (.y (.getLocation (:item @selected-element)))
                                                                                                              y (+ y (/ y 5))] y)])
                                                                       ;; why don't work focus? !!!!!!
                                                                       (.requestFocus @selected-element)))))
    (seesaw.core/show! dialog)))

(defn dialog-table-toolkit-pipeline [configuration]
  ;; (let [toolkit (query-toolkit/data-toolkit-pipeline configuration {})]
  ;;   (into toolkit {:dialog (fn [id] (dialog-table-component configuration toolkit id))}))
  (query-toolkit/data-toolkit-pipeline configuration {}))


(defn dialog-table-entry [plugin-path]
  ;; (let [plugin (view-manager/plugin-link plugin-path)
  ;;       toolkit (.return-toolkit plugin)
  ;;       config  (.return-config  plugin)])
  )

(comment
  (dialog-table-entry [:user :table :user]))

;;;;;;;;;;;;
;;; BIND ;;;
;;;;;;;;;;;;

(jarman.interaction/register-view-plugin
 :name 'dialog-table
 :description "Dialog table"
 :entry dialog-table-entry
 :toolkit dialog-table-toolkit-pipeline
 :spec-list
 [;; [:tables
  ;;  {:spec [:jarman.plugins.data-toolkit/tables :req-un],
  ;;   :doc "list of used tables"
  ;;   :examples "[:permission]"}]
  ;; [:view-columns
  ;;  {:spec [:jarman.plugins.data-toolkit/view-columns :req-un],
  ;;   :doc "Columns which must be represented in table on right side"
  ;;   :examples "[:permission.permission_name 
  ;;               :permission.configuration]"}]
  ;; [:query
  ;;  {:spec [:jarman.plugins.data-toolkit/query :req-un],
  ;;   :examples "{:table_name :permission, :column [:#as_is ...]...}",
  ;;   :doc "SQL syntax for `select!` query"}]
  ])
