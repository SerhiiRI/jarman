(ns plugin.table.table2
  (:require
   ;; Clojure toolkit 
   [clojure.string :as string]
   [clojure.spec.alpha :as s]
   ;; [clojure.java.jdbc :as jdbc]
   [clojure.pprint :refer [cl-format]]
   ;; Seesaw components
   [seesaw.core   :as c]
   ;; [seesaw.dev :as sdev]
   ;; [seesaw.mig :as smig]
   ;; [seesaw.swingx  :as swingx]
   ;; [seesaw.chooser :as chooser]
   ;; [seesaw.border  :as b]
   ;; Jarman toolkit
   ;; [jarman.tools.lang :refer :all]
   ;; [jarman.tools.swing :as stool]
   ;; [jarman.tools.org  :refer :all]
   ;; [jarman.resource-lib.icon-library :as icon]
   ;; [jarman.faces              :as face]
   ;; [jarman.gui.gui-style      :as gs]
   ;; [jarman.gui.gui-tools      :as gtool]
   ;; [jarman.gui.gui-editors    :as gedit]
   ;; [jarman.gui.gui-components :as gcomp]
   ;; [jarman.gui.gui-migrid     :as gmg]
   ;; [jarman.gui.gui-calendar   :as calendar]
   ;; [jarman.gui.popup :as popup]
   ;; [jarman.logic.state :as state]
   ;; [jarman.logic.metadata :as mt]
   ;; [jarman.logic.document-manager :as doc]
   ;; [jarman.plugin.spec :as spec]
   ;; [jarman.plugin.data-toolkit :as query-toolkit]
   ;; [jarman.plugin.gui-table :as gtable]
   [jarman.logic.view-manager :as view-manager]
   ;; external toolkit
   ;; [jarman.interaction :as i]
   ;; [jarman.external :refer [register-custom-view-plugin]]
   ;; locals 
   ;; [plugin.table.composite-components :as ccomp]
   )
  ;; (:import
  ;;  (java.awt Dimension BorderLayout)
  ;;  (java.util Date)
  ;;  (java.text SimpleDateFormat)
  ;;  (jiconfont.icons.google_material_design_icons GoogleMaterialDesignIcons))
  )

(defn create-state-template [plugin-path]
  (let [plugin (view-manager/plugin-link plugin-path)
        toolkit (.return-toolkit plugin)
        config  (.return-config  plugin)]
    ;; ---- Guards ----
    (when (nil? config)
      (throw (ex-info
              "Empty plugin config"
              {:type :empty-plugin-config
               :plugin :table
               :message-head [:header :plugin-name]
               :message-body [:alerts :empty-plugin-config]})))
    (when (nil? toolkit)
      (throw (ex-info
              "Empty plugin toolkit"
              {:type :empty-plugin-toolkit
               :plugin :table
               :message-head [:header :plugin-name]
               :message-body [:alerts :empty-plugin-toolkit]})))
    ;; Structure of State atom
    {:plugin-path       plugin-path
     :plugin-config     config
     :plugin-toolkit    toolkit
     :insert-mode       true
     :history           []
     :model             {}
     :model-changes     {}}))

(defmacro with-state [& body]
  `(let [{~'plugin-path       :plugin-path
          ~'plugin-config     :plugin-config
          ~'plugin-toolkit    :plugin-toolkit
          ~'model             :model
          ~'model-changes     :model-changes}  (~'state!)
         ~'table-meta (:meta-obj ~'plugin-toolkit)]
     ~@body))

(defmacro with-test-environ [plugin-path & body]
  `(let [~'plugin  (jarman.logic.view-manager/plugin-link ~plugin-path)
         ~'toolkit (.return-toolkit ~'plugin)
         ~'config  (.return-config  ~'plugin)]
     (let [~'plugin-path       ~plugin-path
           ~'plugin-config     ~'config
           ~'plugin-toolkit    ~'toolkit
           ~'table-meta        (:meta-obj ~'toolkit)
           ~'state
           (atom
            {:plugin-path       ~plugin-path
             :plugin-config     ~'config
             :plugin-toolkit    ~'toolkit
             :insert-mode       true
             :history           []
             :model             {}
             :model-changes     {}})
           ~'state!   (fn [] (deref ~'state))
           ~'model!   (fn [] (:model (deref ~'state)))
           ~'changes! (fn [] (:model-changes (deref ~'state)))
           ~'dispatch! (create-dispatcher ~'state)]
       ~@body)))

(defn action-handler
  [state action-m]
  (let [meta-obj (:meta-obj (:plugin-toolkit state))
        action   (:action action-m)
        k-path   (:path action-m)
        k-field  (first k-path)]
    (when (:debug? action-m) (cl-format *out* "AH:~@[ (act ~A)~]~@[ (pth ~A)~]~@[ (val ~A)~]~%" action k-path (pr-str value)))
    (case (:action action-m)
      state)))



(comment
  ;; new configuration design
  ;; fixme `:custom-queries` - consider to use some types. 
 (view-manager/defview seal
    (table
     ;; Plugin configuration
     :id :seal
     :name :seal
     :permission :ekka-all
     ;; Meta/Model information 
     :tables          [:seal]
     :active-buttons  [:insert :update :delete :clear :changes]
     :model           [:seal.seal_number :seal.datetime_of_use :seal.datetime_of_remove :seal.site :seal.loc_file :seal.ftp_file]
     :dialogs         {:user.id_permission [:permission :dialog-table :permission-table]}
     ;; Plugin-customization
     :custom-configs {:layout 3/10}
     :custom-actions {:upload-docs-to-db  (fn [state! dispatch!] (println (-> (state!) :plugin-toolkit :meta-obj .return-table_name)))}
     :custom-buttons [{:form-model :model-insert :action :upload-docs-to-db :title "Upload document"}]
     :custom-queries
     {:default
      {:table-columns [:seal.seal_number :seal.datetime_of_use :seal.datetime_of_remove]
       :table-query 
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
         :seal.ftp_file_path]}}})))
