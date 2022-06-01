(ns plugin.table.dialog-bigstring
  (:require
   ;; Clojure toolkit
   [clojure.spec.alpha :as s]
   [clojure.string :as string]
   ;; Dev tools
   [seesaw.border]
   [seesaw.mig]
   [jarman.tools.lang :refer :all]
   [jarman.gui.gui-tools :as gtool]
   [jarman.gui.gui-components :as gcomp]
   [jarman.plugin.spec :as spec]
   [jarman.plugin.data-toolkit :as query-toolkit]
   ;; remove on production
   [jarman.logic.connection :as db]
   [jarman.logic.sql-tool :refer [select!]]
   ;; external
   [jarman.external :refer [register-view-plugin]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; plugin SPEC patters ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;

(s/def ::item-columns keyword?)

;;;;;;;;;;;;;;;;;;;;;;;;
;;; dialog bigstring ;;;
;;;;;;;;;;;;;;;;;;;;;;;;
(defn dialog-bigstring-item
  "Descrition
    create one label for dialog-window with big string and function, which returned on action"
  [txt func selected?]
  (seesaw.core/text
   :background (if selected? "#B0e0dd" "#fff")
   :focusable? false
   :multi-line? true
   :wrap-lines? true
   :editable? false
   :listen [:focus-gained   (fn [e] (gtool/hand-hover-on e) (seesaw.core/config! e :background gcomp/light-light-grey-color))
            :focus-lost     (fn [e] (seesaw.core/config! e :background "#fff"))
            :mouse-entered  (fn [e] (gtool/hand-hover-on e) (seesaw.core/config! e :background gcomp/light-light-grey-color))
            :mouse-exited   (fn [e] (seesaw.core/config! e :background "#fff"))
            :mouse-clicked  func
            :key-pressed  (fn [e] (if (= (.getKeyCode e) java.awt.event.KeyEvent/VK_ENTER) func))]
   :text txt
   :font (gtool/getFont 14)
   :foreground gcomp/blue-green-color
   :border (seesaw.border/compound-border (seesaw.border/empty-border :left 20 :right 20 :bottom 10 :top 10))))

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


(defn dialog-toolkit [configuration toolkit-map]
  (let [query     (:select toolkit-map) 
        get-item  (fn [m] ((:item-columns configuration) m))
        get-id    (fn [m]  (:model-id toolkit-map))]
    (into toolkit-map
          {:dialog (fn [id] (dialog-bigstring-component (query) get-item get-id id))})))

(defn dialog-bigstring-toolkit-pipeline [plugin-config]
  (let [toolkit-map (query-toolkit/data-toolkit-pipeline plugin-config {})]
    (into toolkit-map (dialog-toolkit plugin-config toolkit-map))))

(defn dialog-bigstring-entry [path-to-plugin global-plugin-storrage-getter])

;;;;;;;;;;;;
;;; BIND ;;;
;;;;;;;;;;;;

(register-view-plugin
 :name 'dialog-bigstring 
 :description "Dialog for selecting some ONE item by ONE column in `:query` model"
 :entry dialog-bigstring-entry
 :toolkit dialog-bigstring-toolkit-pipeline
 :spec-list
 [[:item-columns
   {:spec [:jarman.plugins.dialog-bigstring/item-columns :req-un],
    :doc "Select column to be represent one value per item"
    :examples ":permission.permission_name"}]
  [:query
   {:spec [:jarman.plugins.data-toolkit/query :req-un],
    :examples "{:table_name :permission, :column [:#as_is ...]...}",
    :doc "SQL syntax for `select!` query"}]])
