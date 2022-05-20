#_(ns plugins.table.view)

#_(jarman.logic.view-manager/defview seal
  (table
   :id :seal
   :name "seal"
   :tables [:seal]
   :permission :ekka-all
   :model-configurations
   {:view [:seal.seal_number :seal.datetime_of_use :seal.datetime_of_remove]
    :insert
    {:additional
     [{:type :jsgl-button
       :value "First custom action"
       :action :upload-docs-to-db}
      {:type :jsgl-button
       :value "Second custom action"
       :action (fn [state! dispatch!] (println "some function"))}]
     :fields
     [:seal.seal_number
      :seal.datetime_of_use
      {:field :seal_number
       :field-qualified :seal.seal_number
       :representation "Seal Number"
       :component-type
       {:type :jsgl-text
        :value "0"
        :font-size 14
        :char-limit 0
        :placeholder ""
        :border [10 10 5 5 2]
        :on-change (fn [e] (seesaw.core/text e))
        :start-underline nil
        :args []}}]
     }
    :update {:parent :insert}}
   ;; TEMPORARY REMOVE :active-buttons [:insert :update :delete :clear :changes]
   ;; PLAN TO BE :reference-to {:user.id_permission [:permission :dialog-table :permission-table]}
   :actions {:first-action
             (fn [state! dispatch!] ;; (println (-> (state!) :plugin-toolkit :meta-obj .return-table_name))
               (println "first action"))}
   :view-configurations
   [{:type :jsgl-table
     :metatable :seal
     :metafield [:seal.seal_number :seal.datetime_of_use :seal.datetime_of_remove]
     :data
     {:sql-query 
      {:table_name :seal,
       :column
       [:#as_is
        :seal.id
        :seal.seal_number
        :seal.datetime_of_use
        :seal.datetime_of_remove
        :seal.site_name
        :seal.site_url
        :seal.file_name
        :seal.file
        :seal.ftp_file_name
        :seal.ftp_file_path]}}
     :on-select
     :on-click 
     :column
     [:#as_is
      :seal.id
      :seal.seal_number
      :seal.datetime_of_use
      :seal.datetime_of_remove
      :seal.site_name
      :seal.site_url
      :seal.file_name
      :seal.file
      :seal.ftp_file_name
      :seal.ftp_file_path]}]))

(comment
 #_(jarman.logic.view-manager/defview user
   (table
    :id :user
    :name "user"
    :permission :ekka-all
    :tables [:user :permission]
    :model-configurations
    {:insert
     {:fields
      [{:representation "Site",
        :field-qualified :seal.site
        :component {:type :jsgl-url-panel
                    :on-change (fn [e] e)},
        :columns
        [{:field-qualified :seal.site_name
          :constructor-var :text
          :description nil,
          :private? false,
          :default-value nil,
          :editable? true,
          :field :site_name,
          :column-type {:stype "varchar(120)", :tname :varchar, :tspec nil, :tlimit 120, :tnull true, :textra "", :tdefault nil, :tkey ""},
          :component {:type :jsgl-text, :value "Title", :char-limit 120},
          :representation "Site name"}
         {:description nil,
          :default-value nil,
          :constructor-var :link
          :field :site_url,
          :column-type {:stype "varchar(120)", :tname :varchar, :tspec nil, :tlimit 120, :tnull true, :textra "", :tdefault nil, :tkey ""},
          :component {:type :jsgl-text, :value "https://", :char-limit 120},
          :representation "Site url",
          :field-qualified :seal.site_url}]}
       {:representation "Database File"
        :field :db_file
        :field-qualified :seal.db_file
        :component {:type :jsgl-file-panel}
        :columns
        [{:field :file_name
          :field-qualified :seal.file_name
          :representation "File name",
          :constructor-var :file-name
          :column-type {:stype "varchar(120)", :tname :varchar, :tspec nil, :tlimit 120, :tnull true, :textra "", :tdefault nil, :tkey ""},
          :component {:type :jsgl-text, :value "", :char-limit 120}}
         {:field :file
          :field-qualified :seal.file
          :representation "File"
          :constructor-var :file
          :column-type {:stype "blob", :tname :blob, :tspec nil, :tlimit nil, :tnull true, :textra "", :tdefault nil, :tkey ""}
          :component {:type :jsgl-stub}}]}
       :user.login
       :user.password
       [:user.first_name [(fn [m] (assoc-in m [:component :on-change] (fn [e] (seesaw.core/text e))))]]
       [:user.last_name [(fn [m] (assoc-in m [:component :on-change] (fn [e] (seesaw.core/text e))))]]
       [:user.configuration [(fn [m] (assoc-in m [:component :on-change] (fn [e] (seesaw.core/text e))))]]
       [:user.id_profile [(fn [m] (assoc-in m [:component :plugin-path] [:profile :dialog-table :profile-dialog]))]]
       {:field :date-label,
        :field-qualified :user.date-label
        :representation "Date label"
        :component {:type :jsgl-calendar-label,
                    :on-click (fn [e] e)
                    :value nil}}]
      :additional
      [{:type :jsgl-button
        :value "Custom User insert"
        :on-click :insert-action}
       {:type :jsgl-button
        :value "Second custom action"
        :on-click (fn [state! dispatch!] (println "some function"))}]}
     :update
     {:fields
      [ ;; :user.login
       ;; :user.password
       [:user.first_name [(fn [m] (assoc-in m [:component :on-change] (fn [e] (seesaw.core/text e))))]]
       [:user.last_name [(fn [m] (assoc-in m [:component :on-change] (fn [e] (seesaw.core/text e))))]]
       [:user.configuration [(fn [m] (assoc-in m [:component :on-change] (fn [e] (seesaw.core/text e))))]]
       [:user.id_profile [(fn [m] (assoc-in m [:component :plugin-path] [:profile :dialog-table :profile-dialog]))]]
       {:field :SECRET_UPDATE_FIELD,
        :field-qualified :user.SECRET_UPDATE_FIELD
        :representation "ONLY FOR UPDATE"
        :component {:type :jsgl-text, :value "SOME TEXT ONLY FOR UPDATE", :char-limit 100 :on-change (fn [e] (seesaw.core/text e))}}]
      :additional
      [{:type :jsgl-button, :value "DELETE ACTION", :on-click :delete-action}
       {:type :jsgl-button, :value "UPDATE ACTION", :on-click :update-action}]}}
    :actions
    {:TMP_ACTION (fn [state! dispatch!] (println "TMP DEBUG ACTION"))
     :insert-action (fn [state! dispatch!]
                      (println "INSERT!")
                      (jarman.logic.sql-tool/insert!
                       {:table_name :user
                        :set (jarman.logic.metadata/convert_model->flattfields
                              (:model-changes (state!)) :user)})
                      (dispatch! {:route :main-table :action :refresh-to-bottom}))
     :delete-action (fn [state! dispatch!]
                      (println "DELETE!")
                      (if (get-in (state!) [:model-changes :user.id])
                        (do
                          (jarman.logic.sql-tool/delete!
                           {:table_name :user
                            :where [:= :user.id (get-in (state!) [:model-changes :user.id])]})
                          (dispatch! {:route :main-table :action :refresh})
                          (dispatch! {:action :switch-plugin-ui-mode :debug true :plugin-ui-mode :insert :erease-selected? true
                                      :model (jarman.logic.metadata/convert_metadata->model (.return-table_name (-> (state!) :plugin-toolkit :meta-obj)))}))
                        (jarman.interaction/danger "Delete user action" "ID is empty for model")))
     :update-action (fn [state! dispatch!]
                      (println "UPDATE!")
                      (if (get-in (state!) [:model-changes :user.id_profile])
                        (do
                          (jarman.logic.sql-tool/update!
                           {:table_name :user
                            :set (jarman.logic.metadata/convert_model->flattfields
                                  (:model-changes (state!)) :user)
                            :where [:= :user.id (get-in (state!) [:model-changes :user.id])]})
                          (dispatch! {:route :main-table :action :refresh}))
                        (jarman.interaction/danger "Update user action" "ID is empty for model")))}
    :reference-to {:user.id_profile [:profile :dialog-table :profile-dialog]}
    :view-configurations
    {:socket-id :main-table
     :type :jsgl-database-table
     :tables  [:user :profile]
     :columns [:user.login
               :user.password
               :user.first_name
               :user.last_name
               :user.configuration
               :profile.name
               :profile.configuration]
     :socket-refreshable-query
     {:table_name :user,
      :column [:#as_is :user.id :user.login :user.password :user.first_name :user.last_name :user.configuration :user.id_profile :profile.name :profile.configuration]
      :inner-join :user->profile}})))


(jarman.logic.view-manager/defview user
  (table
   :id :user
   :name "user"
   :permission :ekka-all
   :tables [:user :permission]
   :model-configurations
   {:insert
    {:fields
     [[:user.login [(fn [m] (assoc-in m [:component :on-change] (fn [e] (seesaw.core/text e))))]]
      [:user.password [(fn [m] (assoc-in m [:component :on-change] (fn [e] (seesaw.core/text e))))]]
      [:user.first_name [(fn [m] (assoc-in m [:component :on-change] (fn [e] (seesaw.core/text e))))]]
      [:user.last_name [(fn [m] (assoc-in m [:component :on-change] (fn [e] (seesaw.core/text e))))]]
      [:user.configuration [(fn [m] (assoc-in m [:component :on-change] (fn [e] (seesaw.core/text e))))]]
      [:user.id_profile [(fn [m] (assoc-in m [:component :plugin-path] [:profile :dialog-table :profile-dialog]))]]]
     :additional
     [{:type :jsgl-button, :value "Do insert!", :on-click :insert-action}]}
    :update
    {:fields
     [[:user.login [(fn [m] (assoc-in m [:component :on-change] (fn [e] (seesaw.core/text e))))]]
      [:user.password [(fn [m] (assoc-in m [:component :on-change] (fn [e] (seesaw.core/text e))))]]
      [:user.first_name [(fn [m] (assoc-in m [:component :on-change] (fn [e] (seesaw.core/text e))))]]
      [:user.last_name [(fn [m] (assoc-in m [:component :on-change] (fn [e] (seesaw.core/text e))))]]
      [:user.configuration [(fn [m] (assoc-in m [:component :on-change] (fn [e] (seesaw.core/text e))))]]
      [:user.id_profile [(fn [m] (assoc-in m [:component :plugin-path] [:profile :dialog-table :profile-dialog]))]]]
     :additional
     [{:type :jsgl-button :value "Do delete!" :on-click :delete-action}
      {:type :jsgl-button :value "Do update!" :on-click :update-action}]}}
   :actions
   {:insert-action
    (fn [state! dispatch!]
      ;; (jarman.tools.org/print-line "INSERT!")
      (jarman.logic.connection/exec
       (jarman.logic.sql-tool/insert!
        {:table_name :user
         :set (jarman.logic.metadata/convert_model->flattfields
               (:model-changes (state!)) :user)}))
      (dispatch! {:route :main-table :action :refresh-to-bottom})
      (dispatch! {:action :switch-plugin-ui-mode :debug true :plugin-ui-mode :insert :erease-selected? true
                  :model (jarman.logic.metadata/convert_metadata->model
                          (.return-table_name (-> (state!) :plugin-toolkit :meta-obj)))}))
    :delete-action
    (fn [state! dispatch!]
      ;; (jarman.tools.org/print-line "INSERT!")
      (if (get-in (state!) [:model-changes :user.id])
        (do
          (jarman.logic.connection/exec
           (jarman.logic.sql-tool/delete!
            {:table_name :user
             :where [:= :user.id (get-in (state!) [:model-changes :user.id])]}))
          (dispatch! {:route :main-table :action :refresh})
          (dispatch! {:action :switch-plugin-ui-mode :debug true :plugin-ui-mode :insert :erease-selected? true
                      :model (jarman.logic.metadata/convert_metadata->model
                              (.return-table_name (-> (state!) :plugin-toolkit :meta-obj)))}))
        (jarman.interaction/danger "Delete user action" "ID is empty for model")))
    :update-action
    (fn [state! dispatch!]
      ;; (println "UPDATE!")
      (if (get-in (state!) [:model-changes :user.id_profile])
        (do
          (jarman.logic.connection/exec
           (jarman.logic.sql-tool/update!
            {:table_name :user
             :set (jarman.logic.metadata/convert_model->flattfields
                   (:model-changes (state!)) :user)
             :where [:= :user.id (get-in (state!) [:model-changes :user.id])]}))
          (dispatch! {:route :main-table :action :refresh}))
        (jarman.interaction/danger "Update user action" "ID is empty for model")))}
   :view-configurations
   {:socket-id :main-table
    :type :jsgl-database-table
    :tables  [:user :profile]
    :columns
    [:user.login
     :user.password
     :user.first_name
     :user.last_name
     :user.configuration
     :profile.name
     :profile.configuration]
    :socket-refreshable-query
    {:table_name :user,
     :column
     [:#as_is
      :user.id
      :user.login
      :user.password
      :user.first_name
      :user.last_name
      :user.configuration
      :user.id_profile
      :profile.name
      :profile.configuration]
     :inner-join :user->profile}}))

(jarman.logic.view-manager/defview profile
  (table
   :id :profile
   :name "profile"
   :tables [:profile]
   :permission :ekka-all

   :model-configurations
   {:insert
    {:fields
     [[:profile.name [(fn [m] (assoc-in m [:component :on-change] (fn [e] (seesaw.core/text e))))]]
      [:profile.configuration [(fn [m] (assoc-in m [:component :on-change] (fn [e] (seesaw.core/text e))))]]]}
    :update
    {:fields
     [[:profile.name [(fn [m] (assoc-in m [:component :on-change] (fn [e] (seesaw.core/text e))))]]
      [:profile.configuration [(fn [m] (assoc-in m [:component :on-change] (fn [e] (seesaw.core/text e))))]]]}}
   
   :view-configurations
   {:socket-id :main-table
    :type :jsgl-database-table
    :tables  [:profile]
    :columns [:profile.name :profile.configuration]
    :socket-refreshable-query
    {:table_name :profile,
     :column [:#as_is :profile.id :profile.name :profile.configuration]}})
  
  (dialog-table
   :id :profile-dialog
   :name "Profile dialog"
   :permission :ekka-all
   :tables [:profile]
   :view-configurations
   {:type :jsgl-database-table
    :tables  [:profile]
    :columns [:profile.name :profile.configuration]
    :data 
    (fn []
      (vec
       (jarman.logic.connection/query
        (jarman.logic.sql-tool/select!
         {:table_name :profile,
          :column
          [:#as_is
           :profile.id
           :profile.name
           :profile.configuration]}))))}))


