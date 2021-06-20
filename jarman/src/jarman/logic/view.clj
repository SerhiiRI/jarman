{:host "trashpanda-team.ddns.net", :port 3307, :dbname "jarman"}

(defview
 permission
 (table
  :name
  "permission"
  :plug-place
  [:#tables-view-plugin]
  :tables
  [:permission]
  :view-columns
  [:permission.permission_name :permission.configuration]
  :model
  [:permission.permission_name :permission.configuration]
  :insert-button
  true
  :delete-button
  true
  :actions
  []
  :buttons
  []
  :query
  {:table_name :permission,
   :column
   [:#as_is
    :permission.id
    :permission.permission_name
    :permission.configuration]}))

(defview
 user
 (table
  :name
  "user"
  :plug-place
  [:#tables-view-plugin]
  :tables
  [:user :permission]
  :view-columns
  [:user.login
   :user.password
   :user.first_name
   :user.last_name
   :user.id_permission]
  :model
  [:user.login
   :user.password
   :user.first_name
   :user.last_name
   :user.id_permission]
  :insert-button
  true
  :delete-button
  true
  :actions
  []
  :buttons
  []
  :query
  {:table_name :user,
   :inner-join [:user->permission],
   :column
   [:#as_is
    :user.id
    :user.login
    :user.password
    :user.first_name
    :user.last_name
    :user.id_permission
    :permission.id
    :permission.permission_name
    :permission.configuration]}))


