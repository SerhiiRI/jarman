(require '[jarman.tools.swing]
         '[jarman.resource-lib.icon-library]
         '[seesaw.mig]
         '[seesaw.dev])


(defn construct-dialog [table-fn]
  (let [dialog (seesaw.core/custom-dialog :modal? true :width 400 :height 500 :title "WOKA_WOKA")
        table (table-fn (fn [model] (seesaw.core/return-from-dialog dialog model)))
        dialog (seesaw.core/config!
                dialog 
                :content (seesaw.mig/mig-panel
                          :constraints ["wrap 1" "0px[grow, fill]0px" "5px[grow, fill]0px"]
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
                                                                          (println "SEARCH: " (seesaw.core/text e))))])]
                                  [table]]))]
    ;; (seesaw.core/show! (doto dialog (.setLocationRelativeTo nil)))
    (.setLocationRelativeTo dialog nil)
    (seesaw.core/show! dialog )))



(construct-dialog (:->table user-view))
((:->select user-view))
(let [my-frame (-> (doto (seesaw.core/frame
                          :title "test"
                          :size [0 :by 0]
                          :content
                          (construct-dialog-input permission-view))
                     (.setLocationRelativeTo nil) seesaw.core/pack! seesaw.core/show!))]
  (seesaw.core/config! my-frame :size [600 :by 600]))

(let [my-frame (-> (doto (seesaw.core/frame
                          :title "test"
                          :size [0 :by 0]
                          :content
                          (seesaw.core/table :model (table-model
                                                     :columns [{:key :access :text "TF" :class java.lang.Boolean}]
                                                     :rows [{:access true}]) ))
                     (.setLocationRelativeTo nil) seesaw.core/pack! seesaw.core/show!))]
  (seesaw.core/config! my-frame :size [600 :by 600]))

(defn construct-dialog-input [view]
  (let [text-label (seesaw.core/label
                    :cursor :hand
                    :border (seesaw.border/compound-border (seesaw.border/empty-border :thickness 5)
                                                           (seesaw.border/line-border :bottom 1 :color "#222222"))
                    :listen [:mouse-clicked (fn [e]
                                              (let [m (construct-dialog (:->table permission-view))]
                                                (seesaw.core/config! e :text (:id m))
                                                ))]
                    :text  "<- empty ->")
        dialog-label
        (seesaw.mig/mig-panel
         :constraints ["wrap 1" "0px[grow, fill]0px" "5px[grow, fill]0px"]
         :items
         [[text-label]
          [(seesaw.core/label :icon (jarman.tools.swing/image-scale
                                     jarman.resource-lib.icon-library/basket-grey1-64-png 40)
                              :tip "UOJOJOJOJ"
                              :listen [:mouse-entered (fn [e] (seesaw.core/config!
                                                              e :cursor :hand
                                                              :icon (jarman.tools.swing/image-scale
                                                                     jarman.resource-lib.icon-library/basket-blue1-64-png 40)))
                                       :mouse-exited  (fn [e] (seesaw.core/config!
                                                              e :cursor :default
                                                              :icon (jarman.tools.swing/image-scale
                                                                     jarman.resource-lib.icon-library/basket-grey1-64-png 40)))
                                       :mouse-clicked (fn [e]
                                                        (seesaw.core/config! text-label :text "<- empty ->"))])]])]
    (seesaw.core/grid-panel :rows 1 :columns 3 :items [dialog-label])))

;; (def create-login-form
;;   (fn [metadata]
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
;;                                                                (seesaw.core/text
;;                                                                 :listen [:caret-update
;;                                                                          (fn [e]
;;                                                                            (swap! complete (fn [storage] (assoc storage
;;                                                                                                                   (keyword (get meta :field))
;;                                                                                                                   (seesaw.core/value (seesaw.core/to-widget e))))))])])
;;                                (= (first (get meta :component-type)) "l")
;;                                (do
;;                                  (swap! complete (fn [storage] (assoc storage
;;                                                                         (keyword (get meta :field))
;;                                                                         (get meta :key-table))))
;;                                  (seesaw.core/grid-panel :columns 1
;;                                                          :size [200 :by 50]
;;                                                          :items [(seesaw.core/label
;;                                                                   :text (get meta :representation)
;;                                                                   :enabled? false)
;;                                                                  (seesaw.core/text
;;                                                                   :text (get meta :key-table)
;;                                                                   :enabled? false)]))))
;;                            metadata)
;;                       [(seesaw.core/label :text "Insert" :listen [:mouse-clicked (fn [e] 
;;                                                                                    (println "Insert " @complete)
;;                                                                                    (println "SQL" ((:user->insert user-view) (merge {:id nil :login nil :password nil :first_name nil :last_name nil :id_permission nil}@complete))))])])]
;;       (seesaw.core/config! vp :items components))))

;; (let [my-frame (-> (doto (seesaw.core/frame
;;                           :title "test"
;;                           :size [0 :by 0]
;;                           :content (create-login-form ((comp :columns :prop) (first (getset! :user)))))
;;                      (.setLocationRelativeTo nil) seesaw.core/pack! seesaw.core/show!))]
;;   (seesaw.core/config! my-frame :size [400 :by 400]))
