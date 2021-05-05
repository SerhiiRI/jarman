(ns jarman.logic.view
  (:refer-clojure :exclude [update])
  (:require
   ;; Clojure toolkit 
   [clojure.data :as data]
   [clojure.string :as string]
   [seesaw.util :as u]
   ;; Seesaw components
   [seesaw.core :as c]
   [seesaw.border :as sborder]
   [seesaw.dev :as sdev]
   [seesaw.mig :as smig]
   [seesaw.swingx :as swingx]
   [seesaw.chooser :as chooser]
   ;; Jarman toolkit
   [jarman.logic.document-manager :as doc]
   [jarman.logic.connection :as db]
   [jarman.config.config-manager :as cm]
   [jarman.tools.lang :refer :all :as lang]
   [jarman.gui.gui-tools :refer :all :as gtool]
   [jarman.resource-lib.icon-library :as ico]
   [jarman.tools.swing :as stool]
   [jarman.gui.gui-components :refer :all :as gcomp]
   [jarman.gui.gui-calendar :as calendar]
   [jarman.config.storage :as storage]
   [jarman.config.environment :as env]
   [jarman.logic.sql-tool :as toolbox :include-macros true :refer :all]
   [jarman.logic.metadata :as mt])
  (:import (java.util Date)
           (java.text SimpleDateFormat)))

(defn quick-path [table]
  (mt/recur-find-path (first (mt/getset! (keyword table)))))
;; (recur-find-path (first (mt/getset :point_of_sale_group_links)))
;; (recur-find-path (first (mt/getset :user)))
;; {:tbl "point_of_sale_group_links",
;;  :ref [{:tbl "point_of_sale_group", :ref nil}
;;        {:tbl "point_of_sale",
;;         :ref [{:tbl "enterpreneur", :ref nil}]}]}

(defmacro ^:private make-name [entity suffix]
  `(symbol (str ~entity ~suffix)))

;; (defn get-view-column-meta [table-list column-list]
;;   (->> table-list
;;        (mapcat (fn [t]
;;                  (->> ((comp :columns :prop) (first (mt/getset! t)))
;;                       (map (fn [column] (update-in column [:field] #(t-f-tf t %)))))))
;;        (filter (fn [c] (in? column-list (:field c))))))

(defn get-view-column-meta [table-list column-list]
  (->> table-list
       (mapcat (fn [t] (vec ((comp :columns :prop) (first (mt/getset! t))))))
       (filter (fn [c] (in? column-list (keyword (:field-qualified c)))))))

(defn- model-column [column]
  (let [component-type (:component-type column)
        on-boolean (fn [m] (if (in? component-type "b") (into m {:class java.lang.Boolean}) m))
        on-number  (fn [m] (if (in? component-type "n") (into m {:class java.lang.Number})  m))]
    (-> {:key (keyword (:field-qualified column)) :text (:representation column)}
        on-number
        on-boolean)))
;; (model-column {:field "login", :representation "login", :description nil, :component-type ["n"], :column-type [:varchar-100 :nnull], :private? false, :editable? true})
;; {:field "first_name", :representation "first_name", :description nil, :component-type ["i"], :column-type [:varchar-100 :nnull], :private? false, :editable? true} 
;; {:field "last_name", :representation "last_name", :description nil, :component-type ["i"], :column-type [:varchar-100 :nnull], :private? false, :editable? true} 
;; {:field "permission_name", :representation "permission_name", :description nil, :component-type ["i"], :column-type [:varchar-20 :default :null], :private? false, :editable? true}

(defn construct-table-model-columns [table-list column-list]
  (mapv model-column (get-view-column-meta table-list column-list)))

(defn construct-table-model [model-columns data-loader]
  (fn []
    [:columns model-columns
     :rows (data-loader)]))

(defn- tf-t-f [table-field]
  (let [t-f (string/split (name table-field) #"\.")]
    (mapv keyword t-f)))

(defn- t-f-tf [table field]
  (keyword (str (name table) "." (name (name field)))))

;;;;;;;;;;;;;;
;;; JTABLE ;;;
;;;;;;;;;;;;;;

;; (defn addTableSorter
;;   "Something to creating table"
;;   [^javax.swing.JTable T point-lambda]
;;   (doto (.getTableHeader T)
;;     (.addMouseListener
;;      (proxy [java.awt.event.MouseAdapter] []
;;        (^void mouseClicked [^java.awt.event.MouseEvent e]
;;         (point-lambda (.getPoint e)))))) T)

;; Eval After table being scrolled to bottom
;; (defn AdjustmentListener
;;   "(f [suwaczek-position scrollbarMax]..)" [f]
;;   (proxy [java.awt.event.AdjustmentListener] []
;;    (adjustmentValueChanged [^java.awt.event.AdjustmentEvent ae]
;;      (let [scrollBar (cast javax.swing.JScrollBar (.getAdjustable ae))
;;            extent (.. scrollBar getModel getExtent)]
;;        (f (+ (.. scrollBar getValue) extent) (.. scrollBar getMaximum))))))

;; (defn addTableModelListener [f]
;;   (proxy [javax.swing.event.TableModelListener] []
;;     (tableChanged [^javax.swing.event.TableModelEvent e]
;;       (f e))))

;; (let [mig (mig-panel
;;            :constraints ["" "0px[grow, center]0px" "5px[fill]5px"]
;;            :items [[(label :text "One")]])
;;       my-frame (-> (doto (c/frame
;;                           :title "test"
;;                           :size [0 :by 0]
;;                           :content mig)
;;                      (.setLocationRelativeTo nil) pack! show!))]
;;   (c/config! my-frame :size [600 :by 600])
;;   (.add mig (label :text "Two")))

;; [{:key :name :text "Imie"}
;;  {:key :lname :text "Nazwisko"}
;;  {:key :lname :text "Zwierzak"}
;;  {:key :access :text "Kolor"  :class Color}
;;  {:key :access :text "Dost�p" :class javax.swing.JComboBox}
;;  {:key :access :text "TF" :class java.lang.Boolean}
;;  {:key :num :text "Numer" :class java.lang.Number}
;;  {:key :num :text "P�e�" :class javax.swing.JComboBox}
;;  {:key :num :text "Wiek" :class java.lang.Number}
;;  {:key :num :text "Miejscowo��"}]
;; (table :model (score.table/table-model
;;                :columns [{:key :col1 :text "Col 1"} 
;;                          {:key :col2 :text "Col 2"}]
;;                :rows [["Dane 1" "Dane 2"]]))

(defn construct-table [model]
  (fn [listener-fn]
    (let [TT (swingx/table-x :model (model))]
      (c/listen TT :selection (fn [e] (listener-fn (seesaw.table/value-at TT (c/selection TT)))))
      ;; (.setPreferredScrollableViewportSize (new java.awt.Dimension 1000 1000) TT)
      ;; (map (fn [size] (.setWidth (.getColumn (.getColumnModel TT) size) 250)) (.getColumnCount (.getColumnModel TT)))
      ;; (c/config! TT :auto-resize :off)
      ;; (c/config! TT :column-widths 100)
      (c/config! TT :horizontal-scroll-enabled? true)
      (c/config! TT :show-grid? false)
      (c/config! TT :show-horizontal-lines? true)
      (c/scrollable TT :hscroll :as-needed :vscroll :as-needed))))

;; (run cache_register-view)
;; 
;; (seesaw.dev/show-options (c/scrollable (c/label)))

;; (seesaw.dev/show-options (c/table))


(defn construct-sql [table select-rules]
  {:pre [(keyword? table)]}
  (let [m (first (mt/getset table))
        ;; relations (recur-find-path m)
        id_column (t-f-tf table :id)
        table-name ((comp :field :table :prop) m)
        columns (map :field ((comp :columns :prop) m))]
    {:update (fn [entity] (if (id_column entity) (update table-name :set entity :where (=-v id_column (id_column entity)))))
     :insert (fn [entity] (insert table-name :set entity))
     :delete (fn [entity] (if (id_column entity) (delete table-name :where (=-v id_column (id_column entity)))))
     :select (fn [& {:as args}]
               (apply (partial select-builder table)
                      (mapcat vec (into select-rules args))))}))



;;; GOTOWY MODEL 
;; {:table-name :point_of_sale
;;                 :column ({:enterpreneur.director :enterpreneur.director}
;;                          {:enterpreneur.legal_address :enterpreneur.legal_address}
;;                          {:enterpreneur.ssreou :enterpreneur.ssreou}
;;                          {:point_of_sale.name :point_of_sale.name}
;;                          {:point_of_sale.telefons :point_of_sale.telefons})
;;                 :inner-join [:enterpreneur]}
;; ((prepare-export-file
;;   :point_of_sale
;;   {:id 4, :name "yaczlvrnke",
;;    :prop {:table-name :point_of_sale
;;           :column [{:enterpreneur.director :enterpreneur.director}
;;                    {:enterpreneur.legal_address :enterpreneur.legal_address}
;;                    {:enterpreneur.ssreou :enterpreneur.ssreou}
;;                    {:point_of_sale.name :point_of_sale.name}
;;                    {:point_of_sale.telefons :point_of_sale.telefons}]
;;           :inner-join [:enterpreneur]}})
;;  1 "home//path")

;; (defn pp-str [x]
;;   (with-out-str (clojure.pprint/pprint x)))

;; (println (pp-str {:table-name :point_of_sale
;;           :column (as-is :enterpreneur.director :enterpreneur.legal_address :enterpreneur.ssreou :point_of_sale.name :point_of_sale.telefons)
;;           :inner-join [:enterpreneur]}))

(def views (atom {}))
(defmacro defview [table & {:as args}]
  (let [stable (make-name (str table) "-view")]
    `(let [config#          (atom (assoc ~args :table-name (keyword '~table)))
           backup-config#   (deref config#)
           restore-config#  (fn [] (reset! config# backup-config#))
           ktable#    (:table-name @config#)
           ;; stable#    (str @config#)
           tblmeta#   ((comp :table :prop) (first (mt/getset! ktable#)))
           colmeta#   ((comp :columns :prop) (first (mt/getset! ktable#)))
           operations# (construct-sql ktable# (:data @config#))

           view#      (:view @config#)
           idfield#   (t-f-tf ktable# :id)

           select#    (:select operations#)
           update#    (:update operations#)
           delete#    (:delete operations#)
           insert#    (:insert operations#)

           dselect#   (fn [] (db/exec (select#)))
           dupdate#   (fn [e#] (db/exec (update# e#)))
           ddelete#   (fn [e#] (db/exec (delete# e#)))
           dinsert#   (fn [e#] (db/exec (insert# e#)))

           data#      (fn [] (db/query (select#)))
           export#    (select# :column nil :inner-join nil :where nil)
           model#     (construct-table-model-columns (:tables @config#) (:view @config#))
           table#     (construct-table (construct-table-model model# data#))

           docs#      (doc/select-documents-by-table ktable#)]
       (def ~stable {:->table table#
                     :->table-model model#
                     :->model->id idfield#
                     :->data data#
                     :->table-name ktable#

                     :->select select#
                     :->update update#
                     :->delete delete#
                     :->insert insert#

                     :->dselect dselect#
                     :->dupdate dupdate#
                     :->ddelete ddelete#
                     :->dinsert dinsert#

                     :->operations operations#
                     :->config (fn [] @config#)
                     :->col-view view#
                     :->col-meta colmeta#
                     :->tbl-meta tblmeta#

                     :->documents docs#})
       (swap! ~'views (fn [m-view#] (assoc-in m-view# [ktable#] ~stable)))
       nil)))

(defn as-is [& column-list]
  (map #(if (keyword? %) {% %} %) column-list))

;; (:->documents user-view)

;;; ------------------------------------------
;;; ------------Test frame block--------------

;; (let [my-frame (-> (doto (c/frame
;;                           :title "test"
;;                           :size [1000 :by 800]
;;                           :content
;;                           ((:->table service_contract-view) #(println %)))
;;                      (.setLocationRelativeTo nil) c/pack! c/show!))]
;;   (c/config! my-frame :size [1000 :by 800]))

;;; ------------Test frame block--------------
;;; ------------------------------------------

(defview permission
  :tables [:permission]
  :view   [:permission.permission_name]
  :data   {:column (as-is :permission.id :permission.permission_name :permission.configuration)})

;;; TEST SEGMENT 
;; (insert-document
;;  {:table "-----", :name "also-test",
;;   :document "templates\\dovidka.odt"
;;   :prop {:suak [:bliat [:ello]]}})

;; (run documents-view)
;; (mt/getset! :documents)

(defview documents
  :tables [:documents]
  :view   [:documents.table :documents.name :documents.prop]
  :data   {:column (as-is :documents.id :documents.table :documents.name :documents.prop)})

;; (let [my-frame (-> (doto (c/frame
;;                           :title "test"
;;                           :size [1000 :by 800]
;;                           :content
;;                           ((:->table permission-view) (fn [x] (println x))))
;;                      (.setLocationRelativeTo nil) c/pack! c/show!))]
;;   (c/config! my-frame :size [1000 :by 800]))

(defview user
  :tables [:user :permission]
  :view   [:user.first_name :user.last_name :user.login :permission.permission_name]
  :data   {:inner-join [:permission]
           :column (as-is :user.id :user.login :user.password :user.first_name :user.last_name :permission.permission_name :permission.configuration :user.id_permission)})

(defview enterpreneur
  :tables [:enterpreneur]
  :view   [:enterpreneur.ssreou
           :enterpreneur.ownership_form
           :enterpreneur.vat_certificate
           :enterpreneur.individual_tax_number
           :enterpreneur.director
           :enterpreneur.accountant
           :enterpreneur.legal_address
           :enterpreneur.physical_address
           :enterpreneur.contacts_information]
  :data   {:column (as-is
                    :enterpreneur.id
                    :enterpreneur.ssreou
                    :enterpreneur.ownership_form
                    :enterpreneur.vat_certificate
                    :enterpreneur.individual_tax_number
                    :enterpreneur.director
                    :enterpreneur.accountant
                    :enterpreneur.legal_address
                    :enterpreneur.physical_address
                    :enterpreneur.contacts_information)})



(defview point_of_sale
  :tables [:point_of_sale :enterpreneur]
  :view   [:point_of_sale.name :point_of_sale.physical_address :point_of_sale.telefons
           :enterpreneur.ssreou :enterpreneur.ownership_form]
  :data   {:inner-join [:enterpreneur]
           :column (as-is :point_of_sale.id :point_of_sale.name
                          :point_of_sale.physical_address :point_of_sale.telefons :enterpreneur.id
                          :enterpreneur.ssreou :enterpreneur.ownership_form)})

(defview cache_register
  :tables [:cache_register :point_of_sale]
  :view [:cache_register.is_working
         :cache_register.modem_serial_number
         :cache_register.modem_phone_number
         :cache_register.producer
         :cache_register.first_registration_date
         :cache_register.modem_model
         :cache_register.name
         :cache_register.fiscal_number
         :cache_register.dev_id
         :cache_register.manufacture_date
         :cache_register.modem
         :cache_register.version
         :cache_register.serial_number]
  :data {:inner-join [:point_of_sale]
         :column (as-is
                  :cache_register.id
                  :cache_register.is_working
                  :cache_register.modem_serial_number
                  :cache_register.modem_phone_number
                  :cache_register.producer
                  :cache_register.first_registration_date
                  :cache_register.modem_model
                  :cache_register.name
                  :cache_register.fiscal_number
                  :cache_register.dev_id
                  :cache_register.manufacture_date
                  :cache_register.modem
                  :cache_register.version
                  :cache_register.serial_number
                  :cache_register.id_point_of_sale)})

(defview point_of_sale_group
  :tables [:point_of_sale_group]
  :view [:point_of_sale_group.group_name :point_of_sale_group.information]
  :data {:column (as-is :point_of_sale_group.id :point_of_sale_group.group_name :point_of_sale_group.information)})

(defview point_of_sale_group_links
  :tables [:point_of_sale_group_links
           :point_of_sale_group
           :point_of_sale]
  :view [:point_of_sale.name
         :point_of_sale.physical_address
         :point_of_sale_group.group_name
         :point_of_sale_group.information]
  :data {:inner-join [:point_of_sale :point_of_sale_group]
         :column (as-is
                  :point_of_sale_group_links.id
                  :point_of_sale_group_links.id_point_of_sale_group
                  :point_of_sale_group_links.id_point_of_sale
                  :point_of_sale.name
                  :point_of_sale.physical_address
                  :point_of_sale_group.group_name
                  :point_of_sale_group.information)})

(defview seal
  :tables [:seal]
  :view [:seal.seal_number
         :seal.to_date]
  :data {:column (as-is :seal.id :seal.seal_number :seal.to_date)})

(defview service_contract
  :tables [:service_contract :point_of_sale]
  :view [:service_contract.register_contract_date
         :service_contract.contract_term_date
         :service_contract.money_per_month
         :point_of_sale.name
         :point_of_sale.physical_address]
  :data {:inner-join [:point_of_sale]
         :column (as-is
                  :service_contract.id
                  :service_contract.id_point_of_sale
                  :service_contract.register_contract_date
                  :service_contract.contract_term_date
                  :service_contract.money_per_month
                  :point_of_sale.name
                  :point_of_sale.physical_address)})

(defview repair_contract
  :tables [:repair_contract :cache_register :point_of_sale]
  :view [:cache_register.modem_serial_number
         :cache_register.modem_phone_number
         :cache_register.producer
         :point_of_sale.name
         :point_of_sale.physical_address
         :repair_contract.creation_contract_date
         :repair_contract.last_change_contract_date
         :repair_contract.contract_terms_date
         :repair_contract.cache_register_register_date
         :repair_contract.remove_security_seal_date
         :repair_contract.cause_of_removing_seal
         :repair_contract.technical_problem
         :repair_contract.active_seal]
  :data {:inner-join [:point_of_sale :cache_register]
         :column (as-is
                  :cache_register.modem_serial_number
                  :cache_register.modem_phone_number
                  :cache_register.producer
                  :point_of_sale.name
                  :point_of_sale.physical_address
                  :repair_contract.id
                  :repair_contract.id_cache_register
                  :repair_contract.id_point_of_sale
                  :repair_contract.creation_contract_date
                  :repair_contract.last_change_contract_date
                  :repair_contract.contract_terms_date
                  :repair_contract.cache_register_register_date
                  :repair_contract.remove_security_seal_date
                  :repair_contract.cause_of_removing_seal
                  :repair_contract.technical_problem
                  :repair_contract.active_seal)})


;; (mapv (fn [x] (t-f-tf :repair_contract (first x))) 
;;       (reduce into ))
;; ;; (defview user)
;; ;; (map :field ((comp :columns :prop) (first (mt/getset! :permission))))


;; (defn export-from-csv [file-path]
;;   (with-open [reader (io/reader file-path)]
;;     (doall
;;         (map (fn [csv-line]
;;                (let [csv-row (string/split csv-line #",")
;;                      user (cons nil (drop-last 2 csv-row))
;;                      u_nr (last user)]
;;                  (if (or (nil? u_nr ) (empty? u_nr))
;;                    (let [card (concat (cons nil (take-last 2 csv-row)) [nil])]
;;                      (db/exec  (toolbox/insert :card :values card))
;;                      nil)
;;                    (do (db/exec  (toolbox/insert :user :values user))
;;                        (let [u_id (:id (first (jdbc/query @sql-connection (select :user :where (= :teta_nr u_nr)))))
;;                              card (concat (cons nil (take-last 2 csv-row)) [u_id])]
;;                          (db/exec  (toolbox/insert :card :values card))
;;                          nil)))))
;;              (rest (line-seq reader))))))



;; (seesaw.dev/show-options (c/text))




(defn construct-dialog [table-fn selected frame]
  (let [dialog (seesaw.core/custom-dialog :modal? true :width 400 :height 500 :title "Select component")
        table (table-fn (fn [model] (seesaw.core/return-from-dialog dialog model)))
        dialog (seesaw.core/config!
                dialog
                :content (seesaw.mig/mig-panel
                          :constraints ["wrap 1" "0px[grow, fill]0px" "5px[grow, fill]0px"]
                          :items [;; [(seesaw.core/label :text "Press Ctrl + F to search"
                                  ;;                     :halign :center
                                  ;;                     :icon (jarman.tools.swing/image-scale
                                  ;;                            jarman.resource-lib.icon-library/loupe-blue-64-png 30))]

                                  [table]]))]
    ;; (seesaw.core/show! (doto dialog (.setLocationRelativeTo nil)))
    (.setLocationRelativeTo dialog frame)
    (seesaw.core/show! dialog)))


(def build-input-form
  (fn [controller & {:keys [model
                            more-comps
                            button-template
                            start-focus
                            export-comp
                            alerts]
                     :or {model []
                          more-comps [(c/label)]
                          button-template (fn [title f] (gcomp/button-basic title f))
                          start-focus nil
                          export-comp nil
                          alerts nil}}]
    (let [complete (atom {})
          metadata (:->col-meta controller)
          inser-or-update (if (empty? model) "Insert new data" "Update record")
          delete "Remove selected record"
          vgap (fn [size] (c/label :border (sborder/empty-border :top size)))
          panel (smig/mig-panel :constraints ["wrap 1" "0px[grow, fill]0px" "0px[fill]0px"]
                                :border (sborder/empty-border :thickness 10)
                                :items [[(c/label)]])
          components (concat
                      (filter #(not (nil? %)) (map (fn [meta]
                                                     (let [field-qualified (get meta :field-qualified)
                                                           title (get meta :representation)
                                                           editable? (get meta :editable?)
                                    ;; field (get meta :field)
                                                           v (str (get-in model [(keyword field-qualified)]))
                                                           v (if (empty? v) "" v)]
                                                       (cond
                                                         (lang/in? (get meta :component-type) "d")
                                                         (do
                                                           (if (empty? model)
                                                             (do ;;Create calendar input
                                                               (gcomp/inpose-label title (calendar/calendar-with-atom :store-id field-qualified 
                                                                                                                      :local-changes complete)))
                                                             (do ;; Create update calenda input
                                                               (gcomp/inpose-label title (calendar/calendar-with-atom :store-id field-qualified 
                                                                                                                      :local-changes complete 
                                                                                                                      :set-date (if (empty? v) nil v))))))
                                                         (lang/in? (get meta :component-type) "i")
                                                         (do ;; Add input-text with label
                                                           (if (empty? model)
                                                             (do ;;Create insert input
                                                               (gcomp/inpose-label title (gcomp/input-text-with-atom :store-id field-qualified 
                                                                                                                     :local-changes complete 
                                                                                                                     :editable? editable?)))
                                                             (do ;; Create update input
                                                               (gcomp/inpose-label title (gcomp/input-text-with-atom :store-id field-qualified 
                                                                                                                     :local-changes complete 
                                                                                                                     :editable? editable? 
                                                                                                                     :val v)))))
                                                         (lang/in? (get meta :component-type) "l")
                                                         (do ;; Add label with enable false input-text. Can run micro window with table to choose some record and retunr id.
                                                           (let [connected-table (var-get (-> (str "jarman.logic.view/" (get meta :key-table) "-view") symbol resolve))
                                                                 selected-representation (fn [dialog-model-view returned-from-dialog]
                                                                                           (->> (:->col-view dialog-model-view)
                                                                                                (map #(get-in returned-from-dialog [%]))
                                                                                                (filter some?)
                                                                                                (string/join ", ")))
                                                                 v (selected-representation connected-table model)]
                                                             (if-not (nil? (get model field-qualified)) (swap! complete (fn [storage] (assoc storage field-qualified (get-in model [field-qualified])))))
                                                             (gcomp/inpose-label title (gcomp/input-text-with-atom :local-changes complete :editable? false :val v
                                                                                                                   :onClick (fn [e] (let [selected (construct-dialog (:->table connected-table) field-qualified (c/to-frame e))]
                                                                                                                                      (if-not (nil? (get selected (:->model->id connected-table)))
                                                                                                                                        (do (c/config! e :text (selected-representation connected-table selected))
                                                                                                                                            (swap! complete (fn [storage] (assoc storage field-qualified (get selected (:->model->id connected-table)))))))))))))
                                                         (lang/in? (get meta :component-type) "a")
                                                         (do 
                                                           (if (empty? model)
                                                             (do 
                                                               (gcomp/inpose-label title (gcomp/input-text-area :store-id field-qualified
                                                                                                                 :local-changes complete)))
                                                             (do 
                                                               (gcomp/inpose-label title (gcomp/input-text-area :store-id field-qualified
                                                                                                                :local-changes complete
                                                                                                                :val v))))
                                                           ))))
                                                   metadata))
                      [(vgap 20)]
                      [(button-template inser-or-update (fn [e]
                                                          ;; (println "Data from form: " @complete)
                                                          ;; (println "Data from model: " model)
                                                          (if (empty? model)
                                                            (do
                                                              ;; ((:->dinsert controller) @complete)
                                                              ;; (println "Insert " (first (merge model @complete)))
                                                              (println "Insert " (merge {(keyword (str (get (:->tbl-meta controller) :field) ".id")) nil} (first (merge model @complete))))
                                                              ((:->dinsert controller) (merge {(keyword (str (get (:->tbl-meta controller) :field) ".id")) nil} (first (merge model @complete))))
                                                              )
                                                            (do
                                                              (println "Update " (merge model @complete))
                                                              ((:->dupdate controller) (merge model @complete))
                                                              ))
                                                          ((@jarman.gui.gui-seed/jarman-views-service :reload))))]
                      (if (empty? model) [] [(button-template delete (fn [e]
                                                                       ((:->ddelete controller) {(keyword (str (get (:->tbl-meta controller) :field) ".id")) (get model (keyword (str (get (:->tbl-meta controller) :field) ".id")))})
                                                                       ((@jarman.gui.gui-seed/jarman-views-service :reload))))])
                      [(vgap 10)]
                      [more-comps]
                      [(if (nil? export-comp) (c/label) (export-comp (get model (:->model->id controller))))])
          builded (c/config! panel :items (gtool/join-mig-items components))]
      (if-not (nil? start-focus) (reset! start-focus (last (u/children (first components)))))
      builded)))



;; ((:->data permission-view))
;; ((:->dupdate permission-view) {:permission.id 4, :permission.permission_name "SSSSSS", :permission.configuration "{}"})
;; ((:update (construct-sql :permission {})) {:permission.id 4, :permission.permission_name "SSSSSS", :permission.configuration "{}"})

;; (mt/getset :user)
;; (:->table-model permission-view)
;; (:->col-meta seal-view)
;; (run user-view)

;;  (jarman.tools.swing/debug-font-panel)

;;  (run user-view)
(defn export-print-doc
  [controller id alerts]
  (let [;;radio-group (c/button-group)
        panel-bg "#eee"
        expor-gb "#95dec9"
        focus-gb "#5ee6bf"
        input-text (gcomp/input-text :args [:text (str jarman.config.environment/user-home "\\Documents") :font (gtool/getFont  :name "Monospaced")])
        icon (c/label :icon (jarman.tools.swing/image-scale ico/enter-64-png 30)
                      :border (sborder/empty-border :thickness 8)
                      :listen [:mouse-clicked (fn [e] (let [new-path (chooser/choose-file :success-fn  (fn [fc file] (.getAbsolutePath file)))]
                                                        (c/config! input-text :text new-path)))])
        panel (smig/mig-panel
               :constraints ["" "0px[fill]0px[grow, fill]0px" "0px[fill]0px"]
               :items [[icon] [input-text]])]
    (smig/mig-panel
     :constraints ["wrap 1" "0px[grow, fill]0px" "0px[grow]0px"]
     :border (sborder/compound-border (sborder/empty-border :top 5)
                                      (sborder/line-border :top 2 :color "#999")
                                      (sborder/empty-border :top 50))
     :items [[(gcomp/button-expand "Export by template" (smig/mig-panel
                                                         :constraints ["wrap 1" "5px[grow, fill]5px" "0px[fill]0px"]
                                          ;;  :border (sborder/line-border :left 2 :right 2 :bottom 2 :color "#fff")
                                          ;;  :border (sborder/line-border :left 2 :color "#ccc")
                                                         :background panel-bg
                                                         :items (gtool/join-mig-items
                                                                 [(gcomp/hr 2 "#ccc")]
                                                                 [(gcomp/hr 10)]
                                                    ;;  [(c/radio :id :odt  :text "ODT"  :group radio-group :background bg :selected? true) "split 2"]
                                                    ;;  [(c/radio :id :docx :text "DOCX" :group radio-group :background bg)]
                                                    ;; [(gcomp/hr 10)]
                                                                 [panel]
                                                    ;;  [(gcomp/button-basic "Service raport" (fn [e] (if-let [s (c/selection radio-group)] (println "Selected " (str (c/text s))
                                                    ;;                                                                                               "\nTemplate: " (str (c/text template))))))]
                                                                 [(gcomp/hr 10)]
                                                                 (map (fn [doc-model]
                                                                        [(gcomp/button-basic (get doc-model :name)
                                                                                             (fn [e]
                                                                                              ;; do
                                                                                               (try
                                                                                                 ((doc/prepare-export-file (:->table-name controller) doc-model) id (c/config input-text :text))
                                                                                                 (@jarman.gui.gui-seed/alert-manager :set {:header (gtool/get-lang-alerts :success) :body (gtool/get-lang-alerts :export-doc-ok)} (@jarman.gui.gui-seed/alert-manager :message jarman.gui.gui-seed/alert-manager) 7)
                                                                                                 (catch Exception e (@jarman.gui.gui-seed/alert-manager :set {:header (gtool/get-lang-alerts :faild) :body (gtool/get-lang-alerts :export-doc-faild)} (@jarman.gui.gui-seed/alert-manager :message jarman.gui.gui-seed/alert-manager) 7)))

                                                                                              ;; ((@jarman.gui.gui-seed/jarman-views-service :reload))
                                                                                              ;; (if-not (nil? alerts) (@alerts :set {:header (gtool/get-lang-alerts :success) :body (gtool/get-lang-alerts :changes-saved)} (@alerts :message alerts) 5))
                                                                                               )
                                                                                             :args [:halign :left])])
                                                                      (:->documents controller))
                                                                 [(gcomp/hr 10)]
                                                                 [(gcomp/hr 2 "#95dec9")]))
                                   :background "#95dec9"
                                  ;; :focusable? true
                                   :border (sborder/compound-border (sborder/empty-border :left 10 :right 10))
                                  ;; :listen [:focus-gained (fn [e] (c/config! (first (u/children (c/to-widget e))) :background focus-gb))
                                  ;;          :focus-lost   (fn [e] (c/config! (first (u/children (c/to-widget e))) :background expor-gb))]
                                   )]])))

;; (run user-view)


;; (:->col-meta user-view)
;; (:->col-meta (var-get (-> (str "permission" "-view") symbol resolve)))


(def auto-builder--table-view
  (fn [controller
       & {:keys [start-focus
                 alerts]
          :or {start-focus nil
               alerts nil}}]
    (let [x nil ;;------------ Prepare
          controller    (if (nil? controller) user-view controller)
          expand-export (fn [id] (export-print-doc controller id alerts))
          insert-form   (fn [] (build-input-form controller :start-focus start-focus :alerts alerts))
          view-layout   (smig/mig-panel :constraints ["" "0px[shrink 0, fill]0px[grow, fill]0px" "0px[grow, fill]0px"])
          table         (fn [] (second (u/children view-layout)))
          header        (fn [] (c/label :text (get (:->tbl-meta controller) :representation) :halign :center :border (sborder/empty-border :top 10)))
          update-form   (fn [model return] (gcomp/expand-form-panel view-layout [(header) (build-input-form controller :model model :export-comp expand-export :more-comps [(return)])]))
          x nil ;;------------ Build
          expand-insert-form (gcomp/scrollbox (gcomp/expand-form-panel view-layout [(header) (insert-form)]) :hscroll :never)
          back-to-insert     (fn [] (gcomp/button-basic "<< Return to Insert Form" (fn [e] (c/config! view-layout :items [[expand-insert-form] [(table)]]))))
          expand-update-form (fn [model return] (c/config! view-layout :items [[(gcomp/scrollbox (update-form model return) :hscroll :never)] [(table)]]))
          table              (fn [] ((:->table controller) (fn [model] (expand-update-form model back-to-insert))))
          x nil ;;------------ Finish
          view-layout        (c/config! view-layout :items [[(c/vertical-panel :items [expand-insert-form])] [(c/vertical-panel :items [(table)])]])]
      view-layout)))


;; (@jarman.gui.gui-seed/startup)
;; (cm/swapp)

(def run (fn [view] (let [start-focus (atom nil)
                          my-frame (-> (doto (c/frame
                                              :title "test"
                                              :size [1000 :by 800]
                                              :content
                                              (auto-builder--table-view view :start-focus start-focus))
                                         (.setLocationRelativeTo nil) c/pack! c/show!))]
                      (c/config! my-frame :size [1000 :by 800])
                      (if-not (nil? start-focus) (c/invoke-later (.requestFocus @start-focus true))))))

;; (run point_of_sale-view)

;; (let [size [200 :by 200]
;;       my-frame (-> (doto (c/frame
;;                           :title "test"
;;                           :size size
;;                           :content (seesaw.mig/mig-panel :constraints ["wrap 1" "[200, fill]" ""]
;;                                                          :items [[(c/label :text "A" :background "#acc" :halign :center)]
;;                                                                  [(seesaw.mig/mig-panel :constraints ["" "[:32%, fill]" ""]
;;                                                                                         :items [[(c/label :text "A"   :background "#2ac" :halign :center)]
;;                                                                                                 [(c/label :text "BCD" :background "#ca2" :halign :center)]
;;                                                                                                 [(c/label :text "EF"  :background "#2ac" :halign :center)]])]]))
;;                      (.setLocationRelativeTo nil) c/pack! c/show!))]
;;   (c/config! my-frame :size size))

;; (:->col-meta seal-view)

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
;;                                                 (seesaw.core/config! e :text (:id (doto m println)))))]
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

;; (let [my-frame (-> (doto (c/frame
;;                           :title "test"
;;                           :size [1000 :by 800]
;;                           :content
;;                           (auto-builder--table-view user-view))
;;                      (.setLocationRelativeTo nil) c/pack! c/show!))]
;;   (c/config! my-frame :size [1000 :by 800]))



;; (let [start-focus (atom nil)
;;       my-frame (-> (doto (c/frame
;;                           :title "test"
;;                           :size [1000 :by 800]
;;                           :content
;;                           (auto-builder--table-view user-view))
;;                      (.setLocationRelativeTo nil) c/pack! c/show!))]
;;   (c/config! my-frame :size [1000 :by 800])
;;   (if-not (nil? start-focus) (c/invoke-later (.requestFocus @start-focus true))))
