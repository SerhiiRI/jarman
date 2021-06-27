(ns jarman.plugin.dialog-bigstring
  (:require
   ;; Clojure toolkit 
   [clojure.string :as string]
   ;; Dev tools
   [seesaw.core :as c]
   [seesaw.border]
   [seesaw.mig]
   [jarman.tools.lang :refer :all]
   [jarman.gui.gui-tools :as gtool]
   [jarman.gui.gui-components :as gcomp]
   [jarman.plugin.spec :as spec]
   ;; remove on production
   [jarman.logic.connection :as db]
   [jarman.logic.sql-tool :refer [select!]]))

(defn dialog-bigstring-item
  "Descrition
    create one label for dialog-window with big string and function, which returned on action"
  [txt func selected?]
  (let [panel (c/vertical-panel
          :background (if selected? "#B0e0dd" (gtool/get-color :background :button_main))
          :focusable? true
          :listen [:focus-gained   (fn [e] (gtool/hand-hover-on e) (c/config! e :background gcomp/light-light-grey-color))
                   :focus-lost     (fn [e] (c/config! e :background (gtool/get-color :background :button_main)))
                   :mouse-entered  (fn [e] (gtool/hand-hover-on e) (c/config! e :background gcomp/light-light-grey-color))
                   :mouse-exited   (fn [e] (c/config! e :background (gtool/get-color :background :button_main)))
                   :mouse-clicked  func
                   :key-pressed  (fn [e] (if (= (.getKeyCode e) java.awt.event.KeyEvent/VK_ENTER) func))]
          :items (list (c/label
                        :text txt
                        :halign :left
                        :font (gtool/getFont 14)
                        :foreground gcomp/blue-green-color
                        :border (seesaw.border/compound-border (seesaw.border/empty-border :left 20 :right 20 :bottom 10 :top 10)))))]
    panel))

(defn dialog-bigstring-01
  "Description
    this func create dialog-window whith big strings, which gets from db,
    by table-name and name of column. The first label with data will be
    the data-string that the user choose last. For this we use last-id."
  [item-list item-getter id-getter & [selected-id]]
  (let [selected-element (atom nil)
        dialog (seesaw.core/custom-dialog :modal? true :width 600 :height 400 :title "Select component")
        dialog-content-scroll (;; min-scrollbox
                               c/scrollable
                                 (seesaw.mig/mig-panel
                                  :background "#fff"
                                  :constraints ["wrap 1" "0px[fill, grow]0px" "0px[]0px"]
                                  :items (gtool/join-mig-items
                                          (seesaw.mig/mig-panel
                                           :background "#fff"
                                           :constraints ["wrap 1" "0px[:300, fill, grow]0px" "0px[]0px"]
                                           :items
                                           (gtool/join-mig-items
                                            (map (fn [m]
                                                   (let [item-text (item-getter m)
                                                         selected? (= (id-getter m) selected-id)
                                                         return-function (fn [e] (c/return-from-dialog dialog (id-getter m)))
                                                         item (dialog-bigstring-item
                                                               item-text
                                                               return-function
                                                               selected?)]
                                                     (if selected?
                                                       (swap! selected-element (fn [e] item))
                                                       item)))
                                                 item-list)))))
                                 :hscroll :never :border nil)
        dialog-content (seesaw.mig/mig-panel
                        :background "#fff"
                        :constraints ["" "0px[fill, grow]0px" "0px[grow, top]0px"]
                        :items (gtool/join-mig-items
                                dialog-content-scroll))]
    (seesaw.core/config! dialog :content dialog-content :title "Aleks, bliaaaa!!!!!!!!")
    ;; (seesaw.core/pack! dialog)
    ;; (if selected-id
    ;;   ;; (.scrollRectToVisible dialog-content-scroll (new java.awt.Rectangle 0 1700 0 0);; (doto (.getBounds @selected-element) println )
    ;;   ;; )
    ;;   ;; (c/listen dialog
    ;;   ;;           :window-opened (fn [e]
    ;;   ;;                            (println "------")
    ;;   ;;                            (c/scroll! dialog-content-scroll :to [:point 0 1700])))
    ;;   )
    (seesaw.core/show! dialog)))


;; :window-opened
;; (seesaw.dev/show-events  (seesaw.core/custom-dialog :modal? true :width 600 :height 400 :title "Select component"))
;; (import  'java.awt.Rectangle)

;; Rectangle r = new Rectangle(e.getX(), e.getY(), 1, 1)
;; Container parent = titleLabel.getParent()
;; parent.scrollRectToVisible(parent.getBounds())

;; JComponent comp = myTextField;
;; myScrollPane.getVerticalScrollBar().setValue(comp.getLocation().y-50);
;; comp.requestFocus();

;; Julia dialog
(defn dialog []
  (dialog-bigstring-01
   (db/query (select! {:table_name :repair_reasons
                       :column [:#as_is :repair_reasons.description :repair_reasons.id]}))
   (fn [m] (:repair_reasons.description m))
   (fn [m] (:repair_reasons.id m))
   5))


(defn dialog-bigstring-toolkit-pipeline [plugin-config]
  {:dialog dialog})
(defn dialog-bigstring-entry [path-to-plugin global-plugin-storrage-getter])
