(defn relation-modal-helper-panel
  [table-name-kwd id]
  (if-not id
    (gui-component/label :value "Empty")
    (c/scrollable
     (let [{:keys [model run-sql-fn]} (helpers/build-select-simple-data-by-the-id table-name-kwd)
           rows (seq (clojure.set/rename-keys (run-sql-fn id) model))
           t (c/table
               ;; :preferred-size [200 :by 100]
               :model [:columns [{:key :representation :text "FField"} {:key :value :text "VValue"}] :rows rows])]
       (c/config! t :show-horizontal-lines? false)
       (c/config! t :show-vertical-lines? false)
       t))))

(defn table-ui-expand-metafield [metafield]
  (let [state! (active-state!)]
   (with-state
     (where
       ;; -----------------------------
       ;; SETTINGS
       ((field-qualified (:field-qualified metafield))
        (key-table       (:key-table       metafield))
        (plugin-path     (-> metafield :component :plugin-path))
        (load-value-from-model
          (fn [] (get-in (state!) [:model-changes field-qualified])))
        (loading-title
          (fn []
            (if (nil? (load-value-from-model))
              (gtool/get-lang :basic :empty)
              (gtool/get-lang :basic :selected))))
        (loading-dialog
          (fn []
            (fn [e]
              (print-line "table-ui-expand-metafield: open dialog")
              (swing-context/with-active-event e
                (dialog-table
                  :plugin-path plugin-path
                  :on-select
                  (fn [id-of-model]
                    (dispatch!
                      {:debug? true
                       :action :update-model-changes
                       :model-field field-qualified
                       :model-value id-of-model}))))))))
       ;; -----------------------------
       ;; TESTS
       (when-not (some? plugin-path)
         (throw (ex-info (format "Empty `:plugin-path` config for metafield `%s`" field-qualified) metafield)))
       ;; -----------------------------
       ;; GUI COMPOSE
       (println "STATE:" (active-state!))
       (jarman.gui.gui-components2/border-panel
         :north (jarman.gui.gui-components2/label :value (get metafield :representation))
         :south
         (gui-component/vertical-panel
           :event-hook-id :suka
           :event-hook-atom (state! {:action :cursor :path [:model-changes field-qualified]})
           :items
           [(gcomp/expand-input
              {:title (loading-title)
               :panel (relation-modal-helper-panel key-table (load-value-from-model))
               :onClick (loading-dialog)})]
           :event-hook
           (fn [panel a old-s new-s]
             (c/config! panel :items
               [(gcomp/expand-input
                  {:title (loading-title)
                   :panel (relation-modal-helper-panel key-table new-s)
                   :onClick (loading-dialog)})]))))))))


(swing/quick-frame
    [(with-test-environ [:card :table :card]
       (with-active-state!
         (table-ui-expand-metafield
           {:representation "Id profile",
            :field :id_user
            :field-qualified :card.id_user
            :component {:type :jsgl-link :plugin-path [:user :dialog-table :user-dialog]},
            :foreign-keys [{:id_user :user} {:delete :cascade, :update :cascade}],
            :key-table :user})))])
