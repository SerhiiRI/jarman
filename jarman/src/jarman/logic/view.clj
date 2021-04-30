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

(defn get-view-column-meta [table-list column-list]
  (->> table-list
       (mapcat (fn [t] (vec ((comp :columns :prop) (first (mt/getset! t))))))
       (filter (fn [c] (in? column-list (keyword (:field c)))))))

(defn- model-column [column]
  (let [component-type (:component-type column)
        on-boolean (fn [m] (if (in? component-type "b") (into m {:class java.lang.Boolean}) m))
        on-number  (fn [m] (if (in? component-type "n") (into m {:class java.lang.Number})  m))]
    (-> {:key (keyword (:field column)) :text (:representation column)}
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

(get-view-column-meta [:point_of_sale :enterpreneur]
                      [:name :physical_address :telefons ;; point_of sale 
                       :ssreou :ownership_form ;; enterprenier 
                       ])

;; (defn tf-t-f [table-field]
;;   (let [t-f (string/split (name table-field) #"\.")]
;;     (mapv keyword t-f)))

;; (defn t-f-tf [table field]
;;   (keyword (str (name table) "." (name (name field)))))

;; (tf-t-f :suka.bliat)
;; (t-f-tf :suka :bliat)

;; (defn get-view-column-meta [table-list column-list]
;;   (->> table-list
;;        (map (fn [t] (vec [t ((comp :columns :prop) (first (mt/getset! t)))])))
;;        (filter (fn [[t c]] (in? column-list (t-f-tf t (:field c))(keyword (str (name t) (name (:field c)))))))
;;        ;; (map second)
;;        ))


;; (defn get-view-column-meta [table-list column-list]
;;   (->> table-list
;;        (mapcat (fn [t] (vec [t ((comp :columns :prop) (first (mt/getset! t)))])))
;;        ;; (filter (fn [[t c]] (in? column-list (keyword (:field c)))))
;;        ))

;; (construct-table-model-columns [:point_of_sale :enterpreneur]
;;                                [:name :point_of_sale.physical_address :telefons :ssreou :ownership_form])
;; [{:key :name, :text "name"}
;;  {:key :physical_address, :text "physical_address"}
;;  {:key :telefons, :text "telefons"}
;;  {:key :ssreou, :text "ssreou"}
;;  {:key :ownership_form, :text "ownership_form"}
;;  {:key :physical_address, :text "physical_address"}]

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
      (c/scrollable TT :hscroll :as-needed :vscroll :as-needed))))

(defn construct-sql [table select-rules]
  {:pre [(keyword? table)]}
  (let [m (first (mt/getset table))
        ;; relations (recur-find-path m)
        table-name ((comp :field :table :prop) m)
        columns (map :field ((comp :columns :prop) m))]
    {:update (fn [entity] (update table-name :set entity :where (=-v :id (:id entity))))
     :insert (fn [entity] (insert table-name :values (vals entity)))
     :delete (fn [entity] (if (:id entity) (delete table-name :where (=-v :id (:id entity)))))
     :select (fn [& {:as args}]
               (apply (partial select-builder table)
                      (mapcat vec (into select-rules args))))}))

(defmacro defview [table & {:as args}]
  (let [stable (make-name (str table) "-view")]
    `(let [config#          (atom (assoc ~args :table-name (keyword '~table)))
           backup-config#   (deref config#)
           restore-config#  (fn [] (reset! config# backup-config#))
           ktable#    (:table-name @config#)
           stable#    (str @config#)
           tblmeta#   ((comp :table :prop) (first (mt/getset! ktable#)))
           colmeta#   ((comp :columns :prop) (first (mt/getset! ktable#)))
           operations# (construct-sql ktable# (:data @config#))
           select#    (:select operations#)
           update#    (:update operations#)
           delete#    (:delete operations#)
           insert#    (:insert operations#)
           view#       (:view @config#)
           data#      (fn [] (db/query (select#)))
           export#    (select# :column nil :inner-join nil :where nil)
           model#     (construct-table-model-columns (:tables @config#) (:view @config#))
           table#     (construct-table (construct-table-model model# data#))]
       (def ~stable {:->table table#
                     :->table-model model#
                     :->data data#
                     :->select select#
                     :->update update#
                     :->delete delete#
                     :->insert insert#
                     :->operations operations#
                     :->config (fn [] @config#)
                     :->view-col view#
                     :->col-meta colmeta#
                     :->tbl-meta tblmeta#}))))
;; (:->col-meta user-view)
;; (defview user)

(defview permission
  :tables [:permission]
  :view   [:permission_name]
  :data   {:column [:id :permission_name :configuration]})

(defview user
  :tables [:user :permission]
  :view   [:first_name :last_name :login :permission_name]
  :data   {:inner-join [:permission]
           :column [{:user.id :id} :login :password :first_name :last_name :permission_name :configuration :id_permission]})


(defview seal
  :tables [:seal]
  :view [:seal_number :to_date]
  :columns [:id :seal_number :to_date])

((:->data seal-view))

;; (defview enterpreneur
;;   :tables [:enterpreneur]
;;   :view   [:ssreou :ownership_form :vat_certificate :individual_tax_number :director :accountant :legal_address :physical_address :contacts_information]
;;   :data   {:column [:ssreou :ownership_form :vat_certificate :individual_tax_number :director :accountant :legal_address :physical_address :contacts_information]})

;; (db/query "SELECT ssreou, ownership_form, vat_certificate, individual_tax_number, director, accountant, legal_address, physical_address, contacts_information FROM `enterpreneur`")
;; (let [;; mig (smig/mig-panel
;;       ;;      :constraints ["" "0px[grow, center]0px" "5px[fill]5px"]
;;       ;;      :items [[(c/label :text "One")]])
;;       my-frame (-> (doto (c/frame
;;                           :title "test"
;;                           :size [0 :by 0]
;;                           :content ((:->col-meta user-view) (fn [x] (println x))))
;;                      (.setLocationRelativeTo nil) c/pack! c/show!))]
;;   (c/config! my-frame :size [600 :by 600]))

;; (defview point_of_sale
;;   :tables [:point_of_sale :enterpreneur]
;;   :view   [:name :physical_address :telefons ;; point_of sale 
;;            :ssreou :ownership_form ;; enterprenier 
;;            ]
;;   :data   {:inner-join [:enterpreneur]
;;            :column [{:point_of_sale.id :id} :name {:point_of_sale.physical_address :physical_address} :telefons :name :id_enterpreneur :ssreou :ownership_form ]})

;; ((:->model point_of_sale-view))
;; "SELECT point_of_sale.id AS id, name, point_of_sale.physical_address AS physical_address, telefons, name, id_enterpreneur, ssreou, ownership_form FROM `point_of_sale` INNER JOIN enterpreneur ON enterpreneur.id=point_of_sale.id_enterpreneur"

;; {:cache_register
;;  :columns [{:id_point_of_sale [:bigint-20 :unsigned :default :null]}
;;            {:name [:varchar-100 :default :null]}
;;            {:serial_number [:varchar-100 :default :null]}
;;            {:fiscal_number [:varchar-100 :default :null]}
;;            {:manufacture_date [:date :default :null]}
;;            {:first_registration_date [:date :default :null]}
;;            {:is_working [:tinyint-1 :default :null]}
;;            {:version [:varchar-100 :default :null]}
;;            {:id_dev [:varchar-100 :default :null]}
;;            {:producer [:varchar-100 :default :null]}
;;            {:modem [:varchar-100 :default :null]}
;;            {:modem_model [:varchar-100 :default :null]}
;;            {:modem_serial_number [:varchar-100 :default :null]}
;;            {:modem_phone_number [:varchar-100 :default :null]}]
;;  :foreign-keys [{:id_point_of_sale :point_of_sale} {:delete :cascade :update :cascade}]}

;; {:point_of_sale_group
;;  :columns [{:group_name [:varchar-100 :default :null]}
;;            {:information [:mediumtext :default :null]}]}

;; {:point_of_sale_group_links
;;  :columns [{:id_point_of_sale_group [:bigint-20-unsigned :default :null]}
;;            {:id_point_of_sale [:bigint-20-unsigned :default :null]}]
;;  :foreign-keys [[{:id_point_of_sale_group :point_of_sale_group} {:delete :cascade :update :cascade}]
;;                 [{:id_point_of_sale :point_of_sale}]]}

;; {:seal
;;  :columns [{:seal_number [:varchar-100 :default :null]}
;;            {:to_date [:date :default :null]}]}

;; {:service_contract
;;  :columns [{:id_point_of_sale [:bigint-20 :unsigned :default :null]}
;;            {:register_contract_date [:date :default :null]}
;;            {:contract_term_date [:date :default :null]}
;;            {:money_per_month [:int-11 :default :null]}]
;;  :foreign-keys [{:id_point_of_sale :point_of_sale} {:delete :cascade :update :cascade}]}

;; {:repair_contract
;;  :columns [{:id_cache_register [:bigint-20 :unsigned :default :null]}
;;            {:id_point_of_sale [:bigint-20 :unsigned :default :null]}
;;            {:creation_contract_date [:date :default :null]}
;;            {:last_change_contract_date [:date :default :null]}
;;            {:contract_terms_date [:date :default :null]}
;;            {:cache_register_register_date [:date :default :null]}
;;            {:remove_security_seal_date [:datetime :default :null]}
;;            {:cause_of_removing_seal [:mediumtext :default :null]}
;;            {:technical_problem [:mediumtext :default :null]}
;;            {:active_seal [:mediumtext :default :null]}]
;;  :foreign-keys [[{:id_cache_register :cache_register} {:delete :cascade :update :cascade}]
;;                 [{:id_point_of_sale :point_of_sale} {:delete :cascade :update :cascade}]]}



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




(defn construct-dialog [table-fn frame]
  (let [dialog (seesaw.core/custom-dialog :modal? true :width 400 :height 500 :title "Select component")
        table (table-fn (fn [model] (seesaw.core/return-from-dialog dialog model)))
        dialog (seesaw.core/config!
                dialog
                :content (seesaw.mig/mig-panel
                          :constraints ["wrap 1" "0px[grow, fill]0px" "5px[grow, fill]0px"]
                          :items [
                                  ;; [(seesaw.core/label :text "Press Ctrl + F to search"
                                  ;;                     :halign :center
                                  ;;                     :icon (jarman.tools.swing/image-scale
                                  ;;                            jarman.resource-lib.icon-library/loupe-blue-64-png 30))]
                                  
                                  [table]]))]
    ;; (seesaw.core/show! (doto dialog (.setLocationRelativeTo nil)))
    (.setLocationRelativeTo dialog frame)
    (seesaw.core/show! dialog)))


(def build-input-form
  (fn [metadata & {:keys [model
                          more-comps
                          button-template
                          start-focus]
                   :or {model []
                        more-comps [(c/label)]
                        button-template (fn [title f] (gcomp/button-basic title f))
                        start-focus nil}}
       ]
    (let [complete (atom {})
          inser-or-update (if (empty? model) "Insert new data" "Update record")
          delete "Remove selected record"
          vgap (fn [size] (c/label :border (sborder/empty-border :top size)))
          panel (smig/mig-panel :constraints ["wrap 1" "0px[grow, fill]0px" "0px[fill]0px"]
                                :border (sborder/empty-border :thickness 10)
                                :items [[(c/label)]])
          components (concat
                      (filter #(not (nil? %)) (map (fn [meta]
                              (let [field (keyword (get meta :field))
                                    title (get meta :representation)
                                    editable? (get meta :editable?)
                                    ;; field (get meta :field)
                                    v (str (get-in model [(keyword (get meta :representation))]))
                                    v (if (empty? v) "" v)]
                                (cond
                                  (lang/in? (get meta :component-type) "d")
                                  (do
                                    (if (empty? model)
                                      (do ;;Create calendar input
                                        (gcomp/inpose-label title (calendar/calendar-with-atom :field field :changes complete)))
                                      (do ;; Create update calenda input
                                        (gcomp/inpose-label title (calendar/calendar-with-atom :field field :changes complete :set-date v)))))
                                  (lang/in? (get meta :component-type) "i")
                                  (do ;; Add input-text with label
                                    (if (empty? model)
                                      (do ;;Create insert input
                                        (gcomp/inpose-label title (gcomp/input-text-with-atom :field field :changes complete :editable? editable?)))
                                      (do ;; Create update input
                                        (gcomp/inpose-label title (gcomp/input-text-with-atom :field field :changes complete :editable? editable? :val v)))))
                                  (lang/in? (get meta :component-type) "l")
                                  (do ;; Add label with enable false input-text. Can run micro window with table to choose some record and retunr id.
                                    (let [key-table (keyword (str (name (get meta :key-table)) "_name"))
                                          connected-table (var-get (-> (str "jarman.logic.view/" (get meta :key-table) "-view") symbol resolve))
                                          v (if (nil? (get model key-table)) "Enter to select" (get model key-table))]
                                      (if-not (nil? (get model key-table)) (swap! complete (fn [storage] (assoc storage field (get-in model [field])))))
                                      (gcomp/inpose-label title (gcomp/input-text-with-atom :changes complete :editable? false :val v
                                                                                            :onClick (fn [e] (let [selected (construct-dialog (:->table connected-table) (:->table connected-table) (c/to-frame e))]
                                                                                                               (if-not (nil? (get selected :id))
                                                                                                                 (do (c/config! e :text (get selected key-table))
                                                                                                                     (swap! complete (fn [storage] (assoc storage field (get selected :id))))))))
                                                                                            ))))
                                  )))
                            metadata))
                      [(vgap 20)]
                      [(button-template inser-or-update (fn [e] (println "Data from form: " @complete)))]
                      (if (empty? model) [] [(button-template delete (fn [e] (println "Delete: " model)))])
                      [(vgap 10)]
                      [more-comps])
          builded (c/config! panel :items (gtool/join-mig-items components))]
      (if-not (nil? start-focus) (reset! start-focus (last (u/children (first components)))))
      builded
      )))

(run user-view)
;; (:->col-meta seal-view)

(defn export-expand-panel
  []
  (c/flow-panel
   :border (sborder/compound-border (sborder/empty-border :top 5)
                                    (sborder/line-border :top 2 :color "#999")
                                    (sborder/empty-border :top 50))
   :items [(gcomp/button-expand "Export" (smig/mig-panel
                                          :constraints ["wrap 1" "5px[grow, fill]5px" "10px[fill]0px"]
                                          :border (sborder/line-border :left 2 :right 2 :bottom 2 :color "#fff")
                                          :items [[(c/horizontal-panel
                                                    :items [(gcomp/input-text :args [:text "\\path\\to\\export"])
                                                            (c/label :text "[-]"
                                                                         :background "#abc"
                                                                         :border (sborder/empty-border :thickness 5)
                                                                         :listen [:mouse-clicked (fn [e] (println "Export clicked"))])])]
                                                  [(c/checkbox :text "ODT" :selected? true)]
                                                  [(c/checkbox :text "DOCX")]
                                                  [(gcomp/button-basic "Service raport" (fn [e]))]
                                                  [(c/label)]])
                                :background "#fff"
                                :min-height 220
                                :border (sborder/compound-border (sborder/empty-border :left 10 :right 10)))]))


;; (:->col-meta user-view)
;; (:->col-meta (var-get (-> (str "permission" "-view") symbol resolve)))

(def auto-builder--table-view
  (fn [controller
       & {:keys [start-focus]
          :or {start-focus nil}}]
    (let [x nil ;;------------ Prepare
          controller    (if (nil? controller) user-view controller)
          expand-export (fn [] (export-expand-panel))
          insert-form   (fn [] (build-input-form (:->col-meta controller) :more-comps [(expand-export)] :start-focus start-focus))
          view-layout   (smig/mig-panel :constraints ["" "0px[fill]0px[grow, fill]0px" "0px[grow, fill]0px"])
          table         (fn [] (second (u/children view-layout)))
          update-form   (fn [model return] (gcomp/expand-form-panel view-layout (build-input-form (:->col-meta controller) :model model :more-comps [(return) (expand-export)])))
          x nil ;;------------ Build
          expand-insert-form (gcomp/scrollbox (gcomp/expand-form-panel view-layout (insert-form)) :hscroll :never)
          back-to-insert     (fn [] (gcomp/button-basic "<< Return to Insert Form" (fn [e] (c/config! view-layout :items [[expand-insert-form] [(table)]]))))
          expand-update-form (fn [model return] (c/config! view-layout :items [[(gcomp/scrollbox (update-form model return) :hscroll :never)] [(table)]]))
          table              (fn [] ((:->table controller) (fn [model] (expand-update-form model back-to-insert))))
          x nil ;;------------ Finish
          view-layout        (c/config! view-layout :items [[expand-insert-form] [(table)]])
          ]
      (println "Foc" (first (u/children (first (u/children (first (u/children view-layout)))))))
      (.grabFocus (first (u/children (first (u/children (first (u/children view-layout)))))))
      view-layout)))

;; (@jarman.gui.gui-app/startup)
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



;; (let [start-focus (atom nil)
;;       my-frame (-> (doto (c/frame
;;                           :title "test"
;;                           :size [1000 :by 800]
;;                           :content
;;                           (auto-builder--table-view user-view))
;;                      (.setLocationRelativeTo nil) c/pack! c/show!))]
;;   (c/config! my-frame :size [1000 :by 800])
;;   (if-not (nil? start-focus) (c/invoke-later (.requestFocus @start-focus true))))