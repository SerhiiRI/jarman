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
   ;; Jarman toolkit
   [jarman.logic.connection :as db]
   [jarman.config.config-manager :as cm]
   [jarman.tools.lang :refer :all]
   [jarman.gui.gui-tools :refer :all :as gtool]
   [jarman.resource-lib.icon-library :as ico]
   [jarman.tools.swing :as stool]
   [jarman.gui.gui-components :refer :all :as gcomp]
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
    (let [TT (c/table :model (model))]
      (c/listen TT :selection (fn [e] (listener-fn (seesaw.table/value-at TT (c/selection TT)))))
      (c/scrollable TT :hscroll :as-needed :vscroll :as-needed))))

(defn construct-sql [table select-rules]
  {:pre [(keyword? table)]}
  (let [m (first (mt/getset table))
        ;; relations (recur-find-path m)
        id_column (t-f-tf table :id)
        table-name ((comp :field :table :prop) m)
        columns (map :field ((comp :columns :prop) m))]
    {:update (fn [entity] (if (id_column entity) (update table-name :set entity :where (=-v id_column (id_column entity)))))
     :insert (fn [entity] (insert table-name :values (vals entity)))
     :delete (fn [entity] (if (id_column entity) (delete table-name :where (=-v id_column (id_column entity)))))
     :select (fn [& {:as args}]
               (apply (partial select-builder table)
                      (mapcat vec (into select-rules args))))}))

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
           table#     (construct-table (construct-table-model model# data#))]
       (def ~stable {:->table table#
                     :->table-model model#
                     :->model->id idfield#
                     :->data data#
                     
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
                     :->col-meta colmeta#
                     :->tbl-meta tblmeta#})
       (swap! ~'views (fn [m-view#] (assoc-in m-view# [ktable#] ~stable)))
       nil)))

(defn as-is [& column-list]
  (map #(if (keyword? %) {% %} %) column-list))

;;; ------------------------------------------
;;; ------------Test frame block--------------

(let [my-frame (-> (doto (score/frame
                          :title "test"
                          :size [1000 :by 800]
                          :content
                          ((:->table service_contract-view) #(println %)))
                     (.setLocationRelativeTo nil) score/pack! score/show!))]
  (score/config! my-frame :size [1000 :by 800]))

;;; ------------Test frame block--------------
;;; ------------------------------------------

(defview permission
  :display :non
  :permission [:dev :admin]
  :tables [:permission]
  :view   [:permission.permission_name]
  :data   {:column (as-is :permission.id :permission.permission_name :permission.configuration)})

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
           :column (as-is :point_of_sale.id :point_of_sale.name :point_of_sale.physical_address :point_of_sale.telefons :enterpreneur.id :enterpreneur.ssreou :enterpreneur.ownership_form)})

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


;; (defn input-text-with-label
;;   [ & {:keys [title field size changes value editable? enable?]
;;       :or {title ""
;;           ;;  size [200 :by 70]
;;            changes (atom {})
;;            value ""
;;            editable? true
;;            enable? true
;;            field nil}}]
;;   (score/grid-panel :columns 1
;;                     ;; :size size
;;                     :items [(score/label
;;                              :text title)
;;                             (gcomp/input-text
;;                             ;;  :border [10 10 0 0]
;;                              :args [:editable? editable?
;;                                     :enabled? enable?
;;                                     :text value
;;                                     :foreground (if editable? "#000" "#456fd1")
;;                                     :listen [:mouse-entered (if editable? (fn [e]) hand-hover-on)
;;                                              :caret-update (fn [e] 
;;                                                              (if-not (nil? field)(swap! changes (fn [storage] (assoc storage (keyword field) (score/value (score/to-widget e)))))))]])]))


;; (seesaw.dev/show-options (score/text))

;; (def build-input-form
;;   (fn [metadata & {:keys [model 
;;                           more-comps]
;;                    :or {model []
;;                         more-comps [(score/label)]}}]
;;     (let [complete (atom {})
;;           button-title (if (empty? model) "Insert new data" "Update record")
;;           vp (smig/mig-panel :constraints ["wrap 1" "0px[grow, fill]0px" "0px[fill]0px"] 
;;                              :border (sborder/empty-border :thickness 10)
;;                              :items [[(score/label)]])
;;           components (concat
;;                       (map (fn [meta]
;;                              (let [title (key-to-title (get meta :representation))
;;                                    editable? (get meta :editable?)
;;                                    field (get meta :field)]
;;                                (cond
;;                                  (= (first (get meta :component-type)) "i")
;;                                  (do ;; Add input-text with label
;;                                    (if (empty? model)
;;                                      (do ;;Create insert input
;;                                        (input-text-with-label :title title :field field :changes complete :editable? editable?))
;;                                      (do ;; Create update input
;;                                        (input-text-with-label :title title :field field :changes complete :editable? editable? :value (get-in model [(keyword (get meta :representation))])))))
;;                                  (= (first (get meta :component-type)) "l")
;;                                  (do ;; Add label with disable input-text
;;                                    (swap! complete (fn [storage] (assoc storage
;;                                                                         (keyword (get meta :field))
;;                                                                         (get meta :key-table))))
;;                                    (input-text-with-label :title title :changes complete :editable? false :value (get meta :key-table)))))
;;                                  )
;;                            metadata)
;;                       [(score/label :border (sborder/empty-border :top 20))]
;;                       [(gcomp/button-basic button-title (fn [e] (println "Data from form: " @complete)))
;;                       ;;  (score/label :text "Insert" :listen [:mouse-clicked (fn [e]
;;                       ;;                                                        (println "Insert " @complete)
;;                       ;;                                                             ;;  (println "Map" (merge template-map @complete)
;;                       ;;                                                             ;;           ;; ((:user->insert user-view)
;;                       ;;                                                             ;;           ;;  (merge {:id nil :login nil :password nil :first_name nil :last_name nil :id_permission nil}
;;                       ;;                                                             ;;           ;;   @complete))
;;                       ;;                                                             ;;           )
;;                       ;;                                                        )])
;;                        more-comps])]
;;       ;; (println "Model: " model)
;;       (score/config! vp :items (gtool/join-mig-items components)))))



;; (defn export-expand-panel
;;   []
;;   (score/flow-panel
;;    :border (sborder/compound-border (sborder/empty-border :top 5)
;;                                     (sborder/line-border :top 2 :color "#999")
;;                                     (sborder/empty-border :top 50))
;;    :items [(gcomp/button-expand "Export" (smig/mig-panel
;;                                           :constraints ["wrap 1" "5px[grow, fill]5px" "10px[fill]0px"]
;;                                           :border (sborder/line-border :left 2 :right 2 :bottom 2 :color "#fff")
;;                                           :items [[(score/horizontal-panel
;;                                                     :items [(gcomp/input-text :args [:text "\\path\\to\\export"])
;;                                                             (score/label :text "[-]"
;;                                                                          :background "#abc"
;;                                                                          :border (sborder/empty-border :thickness 5)
;;                                                                          :listen [:mouse-clicked (fn [e] (println "Export clicked"))])])]
;;                                                   [(score/checkbox :text "ODT" :selected? true)]
;;                                                   [(score/checkbox :text "DOCX")]
;;                                                   [(gcomp/button-basic "Service raport" (fn [e]))]
;;                                                   [(score/label)]])
;;                                 :background "#fff"
;;                                 :min-height 220
;;                                 :border (sborder/compound-border (sborder/empty-border :left 10 :right 10)))]))


;; (:->col-meta user-view)

;; (def auto-builder--table-view
;;   (fn [controller]
;;     (let [controller (if (nil? controller) user-view controller)
;;           ;; ico-open (stool/image-scale ico/plus-64-png 28)
;;           ;; ico-close (stool/image-scale ico/minus-grey-64-png 28)
;;           hidden-comp (atom nil)
;;           expand-export (fn [](export-expand-panel))
;;           insert-form (fn [](build-input-form (:->col-meta controller) :more-comps [(expand-export)]))
;;           form-space-open ["wrap 1" "0px[grow, fill]0px" "0px[fill]0px"]
;;           form-space-hide ["" "0px[grow, fill]0px" "0px[grow, fill]0px"]
;;           view-layout-open ["" "0px[250:, fill]0px[grow, fill]10px" "0px[grow, fill]0px"]
;;           view-layout-hide ["" "0px[fill]0px[grow, fill]0px" "0px[grow, fill]0px"]
;;           form-space (smig/mig-panel :constraints form-space-open)
;;           view-layout (smig/mig-panel
;;                        :constraints ["" "0px[250:, fill]0px[grow, fill]0px" "0px[grow, fill]0px"])
;;           ;;------------

;;           hide-show (score/label :text "<<"
;;                                  :background "#bbb"
;;                                  :foreground "#fff"
;;                                  :font (gtool/getFont 16 :bold)
;;                                  :border (sborder/empty-border :left 2 :right 2)
;;                                  :listen [:mouse-entered gtool/hand-hover-on])
;;           ;; default-bg-color (score/config hide-show :background)
;;           form-space (score/config! form-space
;;                                     :items [[hide-show] [(insert-form)]])
;;           hide-show (score/config! hide-show :listen [:mouse-clicked (fn [e]
;;                                                                        (let [inside (sutil/children form-space)]
;;                                                                          (if (nil? @hidden-comp)
;;                                                                            (do
;;                                                                              (score/config! view-layout :constraints view-layout-hide)
;;                                                                              (score/config! form-space :constraints form-space-hide)
;;                                                                              (score/config! hide-show :text "..." :valign :top :halign :center)
;;                                                                              ;;--------
;;                                                                              (reset! hidden-comp (drop 1 inside))
;;                                                                              (doall (map #(.remove form-space %) (reverse (drop 1 (range (count inside))))))
;;                                                                              (.revalidate view-layout))
;;                                                                            (do
;;                                                                              (score/config! view-layout :constraints view-layout-open)
;;                                                                              (score/config! form-space :constraints form-space-open)
;;                                                                              (score/config! hide-show :text "<<" :halign :left :font (gtool/getFont 16 :bold))
;;                                                                              ;;---------
;;                                                                              (doall (map #(.add form-space %) @hidden-comp))
;;                                                                              (reset! hidden-comp nil)
;;                                                                              (.revalidate view-layout)))))])
;;           back-to-insert (fn [](gcomp/button-basic "Return to Insert Form" (fn [e]
;;                                                                              (do
;;                                                                                (.remove form-space 1)
;;                                                                                (.add form-space (insert-form))
;;                                                                                (.repaint form-space)
;;                                                                                (.revalidate form-space)))))
;;           update-form (fn [update] (do (score/config! view-layout :constraints view-layout-open)
;;                                        (score/config! form-space :constraints form-space-open)
;;                                        (score/config! hide-show :text "<<" :halign :left :font (gtool/getFont 16 :bold))
;;                                        (if (nil? hidden-comp)
;;                                          (do
;;                                            (.add form-space (update)))
;;                                          (do
;;                                            (reset! hidden-comp nil)
;;                                            (doall (map #(.remove form-space %) (reverse (drop 1 (range (count (sutil/children form-space)))))))
;;                                            (.add form-space (update))))
;;                                        (.revalidate view-layout)))
;;           table ((:->table controller) (fn [model] (update-form (fn [] (build-input-form (:->col-meta controller) :model model :more-comps [(back-to-insert) (expand-export)])))))
;;           view-layout (score/config! view-layout
;;                        :items [[(gcomp/scrollbox form-space :hscroll :never)]
;;                                [table]])]
;;       view-layout)))

;; (@jarman.gui.gui-app/startup)

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

;; (let [my-frame (-> (doto (score/frame
;;                           :title "test"
;;                           :size [1000 :by 800]
;;                           :content
;;                           (auto-builder--table-view user-view))
;;                      (.setLocationRelativeTo nil) score/pack! score/show!))]
;;   (score/config! my-frame :size [1000 :by 800]))


