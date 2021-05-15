(ns jarman.gui.gui-auto-builder
  (:use seesaw.core
        seesaw.border
        seesaw.dev
        seesaw.mig
        seesaw.util)
  (:require [jarman.gui.gui-tools :refer :all]
            [jarman.gui.gui-components :refer :all]
            [jarman.logic.metadata :refer :all]
            )
  (:import (java.awt Color)))

;; (def build-insert-form
;;   (fn [metadata template-map]
;;     (let [complete (atom {})
;;           vp (seesaw.core/vertical-panel :items [])
;;           components (concat
;;                       (map (fn [meta]
;;                              (cond
;;                                (= (first (get meta :component-type)) "i")
;;                                (seesaw.core/grid-panel :columns 1
;;                                                        :size [200 :by 50]
;;                                                        :items [(seesaw.core/label
;;                                                                 :text (get meta :representation))
;;                                                                (input-text
;;                                                                 :args [:listen [:caret-update
;;                                                                                 (fn [e]
;;                                                                                   (swap! complete (fn [storage] (assoc storage
;;                                                                                                                        (keyword (get meta :field))
;;                                                                                                                        (seesaw.core/value (seesaw.core/to-widget e))))))]])])
;;                                (= (first (get meta :component-type)) "l")
;;                                (do
;;                                  (swap! complete (fn [storage] (assoc storage
;;                                                                       (keyword (get meta :field))
;;                                                                       (get meta :key-table))))
;;                                  (seesaw.core/grid-panel :columns 1
;;                                                          :size [200 :by 50]
;;                                                          :items [(seesaw.core/label
;;                                                                   :text (get meta :representation)
;;                                                                   :enabled? false)
;;                                                                  (input-text
;;                                                                   :args [:text (get meta :key-table)
;;                                                                          :enabled? false])]))))
;;                            metadata)
;;                       [(seesaw.core/label :text "Insert" :listen [:mouse-clicked (fn [e]
;;                                                                                    (println "Insert " @complete)
;;                                                                                    (println "Map" (merge template-map @complete)
;;                                                                                             ;; ((:user->insert user-view)
;;                                                                                             ;;  (merge {:id nil :login nil :password nil :first_name nil :last_name nil :id_permission nil}
;;                                                                                             ;;   @complete))
;;                                                                                             ))])])]
;;       (seesaw.core/config! vp :items components))))



;; (defn construct-dialog [table-fn]
;;   (let [dialog (seesaw.core/custom-dialog :modal? true :width 400 :height 500 :title "WOKA_WOKA")
;;         table (table-fn (fn [model] (seesaw.core/return-from-dialog dialog model)))
;;         dialog (seesaw.core/config!
;;                 dialog
;;                 :content (seesaw.mig/mig-panel
;;                           :constraints ["wrap 1" "0px[grow, fill]0px" "5px[grow, fill]0px"]
;;                           :items [[(seesaw.core/label :text "SEARCH"
;;                                                       :halign :center
;;                                                       :icon (jarman.tools.swing/image-scale
;;                                                              jarman.resource-lib.icon-library/loupe-blue-64-png 30))]
;;                                   [(seesaw.core/text :text ""
;;                                                      :halign :center
;;                                                      :border (seesaw.border/compound-border
;;                                                               (seesaw.border/empty-border :thickness 5)
;;                                                               (seesaw.border/line-border
;;                                                                :bottom 1 :color "#eeeeee"))
;;                                                      :listen [:action (fn [e]
;;                                                                         (when-not (= "" (clojure.string/trim (seesaw.core/text e)))
;;                                                                           (println "SEARCH: " (seesaw.core/text e))))])]
;;                                   [table]]))]
;;     ;; (seesaw.core/show! (doto dialog (.setLocationRelativeTo nil)))
;;     (.setLocationRelativeTo dialog nil)
;;     (seesaw.core/show! dialog)))


;; (defn auto-builder--table-view [view]
;;   (let [text-label (seesaw.core/label
;;                     :cursor :hand
;;                     :border (seesaw.border/compound-border (seesaw.border/empty-border :thickness 5)
;;                                                            (seesaw.border/line-border :bottom 1 :color "#222222"))
;;                     :listen [:mouse-clicked (fn [e]
;;                                               (let [m (construct-dialog (:->table view))]
;;                                                 (seesaw.core/config! e :text (:id m))))]
;;                     :text  "<- empty ->")
;;         dialog-label
;;         (seesaw.mig/mig-panel
;;          :constraints ["wrap 1" "0px[grow, fill]0px" "5px[grow, fill]0px"]
;;          :items
;;          [[text-label]
;;           [(seesaw.core/label :icon (jarman.tools.swing/image-scale
;;                                      jarman.resource-lib.icon-library/basket-grey1-64-png 40)
;;                               :tip "UOJOJOJOJ"
;;                               :listen [:mouse-entered (fn [e] (seesaw.core/config!
;;                                                                e :cursor :hand
;;                                                                :icon (jarman.tools.swing/image-scale
;;                                                                       jarman.resource-lib.icon-library/basket-blue1-64-png 40)))
;;                                        :mouse-exited  (fn [e] (seesaw.core/config!
;;                                                                e :cursor :default
;;                                                                :icon (jarman.tools.swing/image-scale
;;                                                                       jarman.resource-lib.icon-library/basket-grey1-64-png 40)))
;;                                        :mouse-clicked (fn [e]
;;                                                         (seesaw.core/config! text-label :text "<- empty ->"))])]])]
;;     (seesaw.core/grid-panel :rows 1 :columns 3 :items [dialog-label])))

;; (defn construct-table [model]
;;   (fn [listener-fn]
;;     (let [TT (seesaw.core/table :model (model))]
;;       (seesaw.core/listen TT :selection (fn [e] (listener-fn (seesaw.table/value-at TT (seesaw.core/selection TT)))))
;;       (seesaw.core/scrollable TT :hscroll :as-needed :vscroll :as-needed))))

;; (def auto-builder--table-view
;;   (fn [controller]
;;     (let [form-panel (vertical-panel :items [(seesaw.mig/mig-panel
;;                                               :constraints ["wrap 1" "0px[grow, fill]0px" "15px[fill]0px"]
;;                                               :items
;;                                               [[(seesaw.core/label :text "SEARCH"
;;                                                       :halign :center
;;                                                       :icon (jarman.tools.swing/image-scale
;;                                                              jarman.resource-lib.icon-library/loupe-blue-64-png 30))]
;;                                                [(seesaw.core/text :text ""
;;                                                                   :halign :center
;;                                                                   :border (seesaw.border/compound-border
;;                                                                            (seesaw.border/empty-border :thickness 5)
;;                                                                            (seesaw.border/line-border
;;                                                                             :bottom 1 :color "#eeeeee"))
;;                                                                   :listen [:action (fn [e]
;;                                                                                      (when-not (= "" (clojure.string/trim (seesaw.core/text e)))
;;                                                                                        (println "SEARCH: " (seesaw.core/text e))))])]])]
;;                                      :background "#aaa")
;;           table ((:->table controller) (fn [model] (seesaw.core/return-from-dialog form-panel model)))
;;           view-layout (left-right-split form-panel table :divider-location 1/2)]
;;       view-layout)))


(def auto-builder--table-view
  (fn [controller]
    (let [
          ico-open (jarman.tools.swing/image-scale jarman.resource-lib.icon-library/plus-64-png 28)
          ico-close (jarman.tools.swing/image-scale jarman.resource-lib.icon-library/minus-grey-64-png 28)
          form-panel (seesaw.mig/mig-panel
                      :constraints ["wrap 1" "0px[300:, grow, fill]0px" "15px[fill]0px"]
                      :items [[(seesaw.core/label :text "SEARCH"
                                                  :halign :center
                                                  :icon (jarman.tools.swing/image-scale
                                                         jarman.resource-lib.icon-library/loupe-blue-64-png 30))]
                              [(seesaw.core/text :text ""
                                                 :halign :center
                                                 :border (seesaw.border/compound-border
                                                          (seesaw.border/empty-border :thickness 5)
                                                          (seesaw.border/line-border
                                                           :bottom 1 :color "#eeeeee"))
                                                 :listen [:action (fn [e]
                                                                    (when-not (= "" (clojure.string/trim (seesaw.core/text e)))
                                                                      (println "SEARCH: " (seesaw.core/text e))))])]])
          form-space (vertical-panel)
          hide-show (label :icon ico-close :halign :right)
          form-space (config! form-space :items [hide-show form-panel])
          hide-show (config! hide-show :listen [:mouse-clicked (fn [e]
                                                                 (let [inside (count (seesaw.util/children form-space))]
                                                                   (println "Inside " inside)
                                                                   (cond
                                                                     (= 1 inside)
                                                                     (do
                                                                       (config! hide-show :icon ico-close)
                                                                       (invoke-later
                                                                        (.add form-space form-panel)
                                                                        (.revalidate form-space)))
                                                                     (= 2 inside)
                                                                     (do
                                                                       (config! hide-show :icon ico-open)
                                                                       (invoke-later
                                                                        (.remove form-space 1)
                                                                        (.revalidate form-space)
                                                                        )))))])
          
          table ((:->table controller) (fn [model] (seesaw.core/return-from-dialog form-panel model)))
          view-layout (mig-panel
                       :constraints ["" "[fill][grow, fill]" "[grow, fill]"]
                       :items [[form-space]
                               [table]])]
      view-layout)))


(let [my-frame (-> (doto (seesaw.core/frame
                          :title "test"
                          :size [1000 :by 800]
                          :content
                          (auto-builder--table-view user-view))
                     (.setLocationRelativeTo nil) seesaw.core/pack! seesaw.core/show!))]
  (seesaw.core/config! my-frame :size [1000 :by 800]))

