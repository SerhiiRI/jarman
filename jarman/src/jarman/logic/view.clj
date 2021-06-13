{:host "trashpanda-team.ddns.net", :port 3307, :dbname "jarman"}

(defview permission
  (table
   :name "permission"
   :id :p-1
   :plug-place [:#tables-view-plugin] ;; KEYPATH TO KEYWORD 
   :tables [:permission]
   :view-columns [:permission.permission_name
                  :permission.configuration]
   :model [{:model-reprs "Permision name"
            :model-param :permission.permission_name
            :model-comp jarman.gui.gui-components/input-text-with-atom}
           :permission.configuration]
   :query {:column
           [{:permission.id :permission.id}
            {:permission.permission_name :permission.permission_name}
            {:permission.configuration :permission.configuration}]}))

(defview
 enterpreneur
 (table
  :name
  "enterpreneur"
  :plug-place
  [:#tables-view-plugin]
  :tables
  [:enterpreneur]
  :view-columns
  [:enterpreneur.ssreou
   :enterpreneur.ownership_form
   :enterpreneur.vat_certificate
   :enterpreneur.individual_tax_number
   :enterpreneur.director
   :enterpreneur.accountant
   :enterpreneur.legal_address
   :enterpreneur.physical_address
   :enterpreneur.contacts_information]
  :model
  [:enterpreneur.ssreou
   :enterpreneur.ownership_form
   :enterpreneur.vat_certificate
   :enterpreneur.individual_tax_number
   :enterpreneur.director
   :enterpreneur.accountant
   :enterpreneur.legal_address
   :enterpreneur.physical_address
   :enterpreneur.contacts_information]
  :insert-button
  true
  :delete-button
  true
  :actions
  []
  :buttons
  []
  :query
  {:column
   [{:enterpreneur.id :enterpreneur.id} {:enterpreneur.ssreou :enterpreneur.ssreou} {:enterpreneur.ownership_form :enterpreneur.ownership_form} {:enterpreneur.vat_certificate :enterpreneur.vat_certificate} {:enterpreneur.individual_tax_number :enterpreneur.individual_tax_number} {:enterpreneur.director :enterpreneur.director} {:enterpreneur.accountant :enterpreneur.accountant} {:enterpreneur.legal_address :enterpreneur.legal_address} {:enterpreneur.physical_address :enterpreneur.physical_address} {:enterpreneur.contacts_information :enterpreneur.contacts_information}]}))

;; Overriding and component custom adding
(defview user
  (table
   :name "user-override"
   :plug-place [:#tables-view-plugin]
   :tables [:user :permission]
   :insert-button true
   :delete-button false
   :view-columns [:user.login
                  :user.first_name
                  :user.last_name
                  :permission.permission_name]
   :model [{:model-reprs "Login"
            :model-param :user.login
            :bind-args {:title :title}
            :model-comp jarman.gui.gui-components/input-text-with-atom}
           :user.password
           :user.first_name
           :user.last_name
           :user.id_permission
           {:model-reprs "Start user"
            :model-param :user-start
            :model-comp jarman.gui.gui-components/input-int}
           {:model-reprs "End user"
            :model-param :user-end
            :model-comp jarman.gui.gui-components/input-int}]
   :query {:inner-join [:user->permission]
           :column
           [{:user.id :user.id} {:user.login :user.login} {:user.password :user.password} {:user.first_name :user.first_name} {:user.last_name :user.last_name} {:user.id_permission :user.id_permission} {:permission.id :permission.id} {:permission.permission_name :permission.permission_name} {:permission.configuration :permission.configuration}]           }
   :actions {:add-multiply-users-insert
             (fn [state]
               (let [{user-start :user-start user-end :user-end} @state]
                 (println (map #(hash-map :user.login      (str "user" %)
                                          :user.password   "1234"
                                          :user.last_name  (str "user" %)
                                          :user.first_name (str "user" %)
                                          :user.id_permission 2)
                               (range user-start (+ 1 user-end))))))}
   :buttons [{:action :add-multiply-users-insert
              :title "Auto generate users"}]))

