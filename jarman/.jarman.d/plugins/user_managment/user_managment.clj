(do (load-file (str (clojure.java.io/file ".jarman.d" "plugins" "table" "table.clj"))) nil)

(ns jarman.plugins.user-managment
  (:require
   [seesaw.core :as c]
   [seesaw.border :as b]
   [jarman.gui.gui-components :as gcomp]
   [jarman.gui.gui-tools  :as gtool]
   [jarman.plugin.plugin :refer :all]
   [jarman.plugin.gui-table :as gtable]
   [jarman.plugin.data-toolkit :as query-toolkit]
   [jarman.plugins.table :as ptable]
   [jarman.resource-lib.icon-library :as icon]
   [jarman.logic.sql-tool :refer [select! update! insert! delete!]]
   [jarman.logic.connection :as db]
   [jarman.gui.gui-editors :as gedit]))
;; name
;;login
;;status (admin ...


;;; add user
;;; delete user
;;; safe data
;;; configura

(defn panel-code-area [code title info]
  (let [editor (gcomp/vmig
                :vrules "[fill]0px[grow, fill]0px[fill]"
                ;;:args [:size [400 :by 100]]
                :items [[(gcomp/hmig
                         :hrules "[70%, fill]10px[grow, fill]"
                          :items
                          [[(c/label :text title
                                     :border (b/compound-border (b/line-border :bottom 1 :color "#eee")
                                                                (b/empty-border :bottom 10))
                                     :font (gtool/getFont 13))]])]
                        [(gcomp/scrollbox code)]
                        [(c/label :text "")]])]
    (gtool/set-focus code)
    editor))

(def create-plugin-gui
  "Description
     Prepare and merge complete big parts"
  (fn [state]
    (let [main-layout  (seesaw.mig/mig-panel :constraints ["wrap 2" "0px[:500, fill]0px[grow, fill]0px" "0px[grow, fill, top]0px"])
          code-area    (gedit/code-area {:val "{:ftp {:login  nil, :password nil, :host  nil}}"})
          swap-field   (fn [k-vec] (fn [e] (swap! state assoc-in k-vec (c/text e))))
          panel-editor (panel-code-area code-area "configurations for user" "")
          input-space  (seesaw.core/border-panel :border (b/empty-border :left 20))
          btn-panel    (gcomp/menu-bar
                        {:buttons [[" Add user"
                                    icon/plus-blue-64-png
                                    (fn [e] )]
                                   [" Delete user"
                                    icon/basket-blue1-64-png
                                    (fn [e] )]
                                   [" Save user"
                                    icon/agree1-blue-64-png
                                    (fn [e] )]]})
          field-fn         (fn [key-vec] (gcomp/state-input-text {:func (swap-field key-vec) :val ""}
                                                                 :args [:columns 25 :text (get-in (:model @state) key-vec)]))
          login-field      (field-fn [:user.login])
          fname-field      (field-fn [:user.first_name])
          lname-field      (field-fn [:user.last_name])
          password-field   (field-fn [:user.password])
          permission-field (ptable/input-related-popup-table {:state! (fn [] @state) :dispatch! (fn [e])
                                                              :val 3 :field-qualified :user.id_permission})        
          table-fn         (fn []((:table (gtable/create-table (:plugin-config  @state)
                                                               (:plugin-toolkit @state)))
                                  (fn [model-table]
                                    (if-not (= false (:update-mode (:plugin-config @state)))
                                      (do (swap! state merge {:model model-table})
                                          (println "MODELL !!" (:model @state))
                                          (c/config! login-field :text (:user.login (:model @state)))
                                          (c/config! fname-field :text (:user.first_name (:model @state)))
                                          (c/config! lname-field :text (:user.last_name (:model @state)))
                                          (c/config! password-field :text (:user.password (:model @state)))
                                          (c/config! code-area :text (:user.configuration (:model @state)))
                                          (.revalidate input-space) (.repaint input-space))))))
          label-fn         (fn [text] (seesaw.core/label :text text :font (gtool/getFont)))
          table-container  (c/vertical-panel)
          table-render (fn [] (try
                                (c/config! table-container :items [(table-fn)])
                                (catch Exception e
                                  (c/label :text (str "Problem with table model: " (.getMessage e))))))
          field-space  (seesaw.mig/mig-panel :constraints ["wrap 1" "0px[]0px" "0px[top]10px"]
                                             :items [[(c/config! btn-panel :border (b/empty-border :left -4))]
                                                     [(label-fn "login")]       [login-field]
                                                     [(label-fn "first name")]  [fname-field]
                                                     [(label-fn "last name")]   [lname-field]
                                                     [(label-fn "password")]    [password-field]
                                                     [(label-fn "permission")]  [permission-field]])
          main-layout  (c/config!   
                        main-layout
                        :items [[table-container]
                                [(gcomp/min-scrollbox input-space)]])]
      (c/config! input-space :north field-space :center panel-editor)
      (table-render) main-layout)))


(defn insert-user [data-model]
  (let [{login          :user.login   
         first_name     :user.first_name 
         last_name      :user.last_name
         password       :user.password
         configuration  :user.configuration 
         permission     :user.id_permission} data-model] 
    (clojure.java.jdbc/execute! (db/connection-get)
                                (insert! {:table_name :user
                                          :set {:id             nil
                                                :login          login
                                                :first_name     first_name
                                                :last_name      last_name
                                                :password       password
                                                :configuration  configuration
                                                :id_permission  permission}}))))

;; (if-let [user (not-empty (db/query (select! {:table_name :user :where [:= :user.login "ud"]})))]
;;   "User already exist"
;;   (insert-user {:user.login "ud" :user.first_name "H" :user.last_name "B"
;;                 :user.password "123" :user.configuration "{:ftp {:a \"a\"}}"
;;                 :user.id_permission 2}))

(defn user-managment-toolkit-pipeline [configuration]
  (conj (query-toolkit/data-toolkit-pipeline configuration {})
        {:insert-user  nil}))

(defn user-managment-entry [plugin-path global-configuration]
  (let [state (ptable/create-state-template plugin-path global-configuration)]
    (println "\nBuilding plugin")
    (create-plugin-gui state)))

(defplugin user-managment
  "Plugin for user-managment") 

;;;; in .jarman/plugins  create folder with name plugin, file name_plugin.clj
;;;; (create-plugin-view)
;;;; (name-plugin-entry)
;;;; (defplugin name-plugin)
;;;;;;;;;;;;;;;;
;;;; in view.clj in defview declare plugin like (name-plugin :id :name :name "user")
;;;; in file .jarman  in view-manager declare "Name plugin" [path-plugin] like [:user :table :user]
