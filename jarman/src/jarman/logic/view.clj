(ns jarman.logic.view
  (:refer-clojure :exclude [update])
  (:require
   ;; Clojure toolkit 
   [clojure.data :as data]
   [clojure.string :as string]
   [clojure.java.jdbc :as jdbc]
   [seesaw.util :as sutil]
   ;; Seesaw components
   [seesaw.core :as score]
   [seesaw.border :as sborder]
   [seesaw.dev :as sdev]
   [seesaw.mig :as smig]
   [seesaw.swingx :as swingx]
   ;; Jarman toolkit
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

(def ^:dynamic prod? true)
(def ^:dynamic sql-connection
  (if prod?
    ;; {:dbtype "mysql" :host "192.168.1.69" :port 3306 :dbname "jarman" :user "jarman" :password "dupa"}
    {:dbtype "mysql", :host "trashpanda-team.ddns.net", :port 3306, :dbname "jarman", :user "jarman", :password "dupa"}
    {:dbtype "mysql" :host "127.0.0.1" :port 3306 :dbname "jarman" :user "root" :password "1234"}))

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
;;       my-frame (-> (doto (score/frame
;;                           :title "test"
;;                           :size [0 :by 0]
;;                           :content mig)
;;                      (.setLocationRelativeTo nil) pack! show!))]
;;   (score/config! my-frame :size [600 :by 600])
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
    (let [TT (score/table :model (model))]
      (score/listen TT :selection (fn [e] (listener-fn (seesaw.table/value-at TT (score/selection TT)))))
      (score/scrollable TT :hscroll :as-needed :vscroll :as-needed))))

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
           data#      (fn [] (jdbc/query sql-connection (select#)))
           export#    (select# :column nil :inner-join nil :where nil)
           model#     (construct-table-model-columns (:tables @config#) (:view @config#))
           table#     (construct-table (construct-table-model model# data#))]
       (def ~stable {:->table table#
                     :->data data#
                     :->select select#
                     :->update update#
                     :->delete delete#
                     :->insert insert#
                     :->operations operations#
                     :->config (fn [] @config#)
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

(defview enterpreneur
  :tables [:enterpreneur]
  :view   [:first_name :last_name :login :permission_name]
  :data   {:column [:ssreou :ownership_form :vat_certificate :individual_tax_number :director :accountant :legal_address :physical_address :contacts_information]})

;; (mapv (comp keyword :field) ((comp :columns :prop) (first (mt/getset! :enterpreneur))))


;; (quick-path :enterpreneur)

(def enterpreneur
  (create-table :enterpreneur
                :columns [{:ssreou [:tinytext :nnull]}
                          {:ownership_form [:varchar-100 :default :null]}
                          {:vat_certificate [:tinytext :default :null]}
                          {:individual_tax_number [:varchar-100 :default :null]}
                          {:director [:varchar-100 :default :null]}
                          {:accountant [:varchar-100 :default :null]}
                          {:legal_address [:varchar-100 :default :null]}
                          {:physical_address [:varchar-100 :default :null]}
                          {:contacts_information [:mediumtext :default :null]}]))

(def point_of_sale
  (create-table :point_of_sale
                :columns [{:id_enterpreneur [:bigint-20-unsigned :default :null]}
                          {:name [:varchar-100 :default :null]}
                          {:physical_address  [:varchar-100 :default :null]}
                          {:telefons  [:varchar-100 :default :null]}]
                :foreign-keys [{:id_enterpreneur :enterpreneur} {:update :cascade}]))

(def cache_register
  (create-table :cache_register
                :columns [{:id_point_of_sale [:bigint-20 :unsigned :default :null]}
                          {:name [:varchar-100 :default :null]}
                          {:serial_number [:varchar-100 :default :null]}
                          {:fiscal_number [:varchar-100 :default :null]}
                          {:manufacture_date [:date :default :null]}
                          {:first_registration_date [:date :default :null]}
                          {:is_working [:tinyint-1 :default :null]}
                          {:version [:varchar-100 :default :null]}
                          {:id_dev [:varchar-100 :default :null]}
                          {:producer [:varchar-100 :default :null]}
                          {:modem [:varchar-100 :default :null]}
                          {:modem_model [:varchar-100 :default :null]}
                          {:modem_serial_number [:varchar-100 :default :null]}
                          {:modem_phone_number [:varchar-100 :default :null]}]
                :foreign-keys [{:id_point_of_sale :point_of_sale} {:delete :cascade :update :cascade}]))

(def point_of_sale_group
  (create-table :point_of_sale_group
                :columns [{:group_name [:varchar-100 :default :null]}
                          {:information [:mediumtext :default :null]}]))

(def point_of_sale_group_links
  (create-table :point_of_sale_group_links
                :columns [{:id_point_of_sale_group [:bigint-20-unsigned :default :null]}
                          {:id_point_of_sale [:bigint-20-unsigned :default :null]}]
                :foreign-keys [[{:id_point_of_sale_group :point_of_sale_group} {:delete :cascade :update :cascade}]
                               [{:id_point_of_sale :point_of_sale}]]))

(def seal
  (create-table :seal
                :columns [{:seal_number [:varchar-100 :default :null]}
                          {:to_date [:date :default :null]}]))

(def service_contract
  (create-table :service_contract
                :columns [{:id_point_of_sale [:bigint-20 :unsigned :default :null]}
                          {:register_contract_date [:date :default :null]}
                          {:contract_term_date [:date :default :null]}
                          {:money_per_month [:int-11 :default :null]}]
                :foreign-keys [{:id_point_of_sale :point_of_sale} {:delete :cascade :update :cascade}]))

(def repair_contract
  (create-table :repair_contract
                :columns [{:id_cache_register [:bigint-20 :unsigned :default :null]}
                          {:id_point_of_sale [:bigint-20 :unsigned :default :null]}
                          {:creation_contract_date [:date :default :null]}
                          {:last_change_contract_date [:date :default :null]}
                          {:contract_terms_date [:date :default :null]}
                          {:cache_register_register_date [:date :default :null]}
                          {:remove_security_seal_date [:datetime :default :null]}
                          {:cause_of_removing_seal [:mediumtext :default :null]}
                          {:technical_problem [:mediumtext :default :null]}
                          {:active_seal [:mediumtext :default :null]}]
                :foreign-keys [[{:id_cache_register :cache_register} {:delete :cascade :update :cascade}]
                               [{:id_point_of_sale :point_of_sale} {:delete :cascade :update :cascade}]]))



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
;;                      (jdbc/execute! @sql-connection (toolbox/insert :card :values card))
;;                      nil)
;;                    (do (jdbc/execute! @sql-connection (toolbox/insert :user :values user))
;;                        (let [u_id (:id (first (jdbc/query @sql-connection (select :user :where (= :teta_nr u_nr)))))
;;                              card (concat (cons nil (take-last 2 csv-row)) [u_id])]
;;                          (jdbc/execute! @sql-connection (toolbox/insert :card :values card))
;;                          nil)))))
;;              (rest (line-seq reader))))))


(defn input-text-with-label
  [ & {:keys [title field size changes value editable? enable?]
      :or {title ""
          ;;  size [200 :by 70]
           changes (atom {})
           value ""
           editable? true
           enable? true
           field nil}}]
  (score/grid-panel :columns 1
                    ;; :size size
                    :items [(score/label
                             :text title)
                            (gcomp/input-text
                            ;;  :border [10 10 0 0]
                             :args [:editable? editable?
                                    :enabled? enable?
                                    :text value
                                    :foreground (if editable? "#000" "#456fd1")
                                    :listen [:mouse-entered (if editable? (fn [e]) hand-hover-on)
                                             :caret-update (fn [e] 
                                                             (if-not (nil? field)(swap! changes (fn [storage] (assoc storage (keyword field) (score/value (score/to-widget e)))))))]])]))


;; (seesaw.dev/show-options (score/text))

(def build-input-form
  (fn [metadata & {:keys [model 
                          more-comps]
                   :or {model []
                        more-comps [(score/label)]}}]
    (let [complete (atom {})
          button-title (if (empty? model) "Insert new data" "Update record")
          vp (smig/mig-panel :constraints ["wrap 1" "0px[grow, fill]0px" "0px[fill]0px"] 
                             :border (sborder/empty-border :thickness 10)
                             :items [[(score/label)]])
          components (concat
                      (map (fn [meta]
                             (let [title (key-to-title (get meta :representation))
                                   editable? (get meta :editable?)
                                   field (get meta :field)]
                               (cond
                                 (= (first (get meta :component-type)) "i")
                                 (do ;; Add input-text with label
                                   (if (empty? model)
                                     (do ;;Create insert input
                                       (input-text-with-label :title title :field field :changes complete :editable? editable?))
                                     (do ;; Create update input
                                       (input-text-with-label :title title :field field :changes complete :editable? editable? :value (get-in model [(keyword (get meta :representation))])))))
                                 (= (first (get meta :component-type)) "l")
                                 (do ;; Add label with disable input-text
                                   (swap! complete (fn [storage] (assoc storage
                                                                        (keyword (get meta :field))
                                                                        (get meta :key-table))))
                                   (input-text-with-label :title title :changes complete :editable? false :value (get meta :key-table)))))
                                 )
                           metadata)
                      [(score/label :border (sborder/empty-border :top 20))]
                      [(gcomp/button-basic button-title (fn [e] (println "Data from form: " @complete)))
                      ;;  (score/label :text "Insert" :listen [:mouse-clicked (fn [e]
                      ;;                                                        (println "Insert " @complete)
                      ;;                                                             ;;  (println "Map" (merge template-map @complete)
                      ;;                                                             ;;           ;; ((:user->insert user-view)
                      ;;                                                             ;;           ;;  (merge {:id nil :login nil :password nil :first_name nil :last_name nil :id_permission nil}
                      ;;                                                             ;;           ;;   @complete))
                      ;;                                                             ;;           )
                      ;;                                                        )])
                       more-comps])]
      ;; (println "Model: " model)
      (score/config! vp :items (gtool/join-mig-items components)))))



(defn export-expand-panel
  []
  (score/flow-panel
   :border (sborder/compound-border (sborder/empty-border :top 5)
                                    (sborder/line-border :top 2 :color "#999")
                                    (sborder/empty-border :top 50))
   :items [(gcomp/button-expand "Export" (smig/mig-panel
                                          :constraints ["wrap 1" "5px[grow, fill]5px" "10px[fill]0px"]
                                          :border (sborder/line-border :left 2 :right 2 :bottom 2 :color "#fff")
                                          :items [[(score/horizontal-panel
                                                    :items [(gcomp/input-text :args [:text "\\path\\to\\export"])
                                                            (score/label :text "[-]"
                                                                         :background "#abc"
                                                                         :border (sborder/empty-border :thickness 5)
                                                                         :listen [:mouse-clicked (fn [e] (println "Export clicked"))])])]
                                                  [(score/checkbox :text "ODT" :selected? true)]
                                                  [(score/checkbox :text "DOCX")]
                                                  [(gcomp/button-basic "Service raport" (fn [e]))]
                                                  [(score/label)]])
                                :background "#fff"
                                :min-height 220
                                :border (sborder/compound-border (sborder/empty-border :left 10 :right 10)))]))


;; (:->col-meta user-view)

(def auto-builder--table-view
  (fn [controller]
    (let [controller (if (nil? controller) user-view controller)
          ;; ico-open (stool/image-scale ico/plus-64-png 28)
          ;; ico-close (stool/image-scale ico/minus-grey-64-png 28)
          hidden-comp (atom nil)
          expand-export (fn [](export-expand-panel))
          insert-form (fn [](build-input-form (:->col-meta controller) :more-comps [(expand-export)]))
          form-space-open ["wrap 1" "0px[grow, fill]0px" "0px[fill]0px"]
          form-space-hide ["" "0px[grow, fill]0px" "0px[grow, fill]0px"]
          view-layout-open ["" "0px[250:, fill]0px[grow, fill]10px" "0px[grow, fill]0px"]
          view-layout-hide ["" "0px[fill]0px[grow, fill]0px" "0px[grow, fill]0px"]
          form-space (smig/mig-panel :constraints form-space-open)
          view-layout (smig/mig-panel
                       :constraints ["" "0px[250:, fill]0px[grow, fill]0px" "0px[grow, fill]0px"])
          ;;------------

          hide-show (score/label :text "<<"
                                 :background "#bbb"
                                 :foreground "#fff"
                                 :font (gtool/getFont 16 :bold)
                                 :border (sborder/empty-border :left 2 :right 2)
                                 :listen [:mouse-entered gtool/hand-hover-on])
          ;; default-bg-color (score/config hide-show :background)
          form-space (score/config! form-space
                                    :items [[hide-show] [(insert-form)]])
          hide-show (score/config! hide-show :listen [:mouse-clicked (fn [e]
                                                                       (let [inside (sutil/children form-space)]
                                                                         (if (nil? @hidden-comp)
                                                                           (do
                                                                             (score/config! view-layout :constraints view-layout-hide)
                                                                             (score/config! form-space :constraints form-space-hide)
                                                                             (score/config! hide-show :text "..." :valign :top :halign :center)
                                                                             ;;--------
                                                                             (reset! hidden-comp (drop 1 inside))
                                                                             (doall (map #(.remove form-space %) (reverse (drop 1 (range (count inside))))))
                                                                             (.revalidate view-layout))
                                                                           (do
                                                                             (score/config! view-layout :constraints view-layout-open)
                                                                             (score/config! form-space :constraints form-space-open)
                                                                             (score/config! hide-show :text "<<" :halign :left :font (gtool/getFont 16 :bold))
                                                                             ;;---------
                                                                             (doall (map #(.add form-space %) @hidden-comp))
                                                                             (reset! hidden-comp nil)
                                                                             (.revalidate view-layout)))))])
          back-to-insert (fn [](gcomp/button-basic "Return to Insert Form" (fn [e]
                                                                             (do
                                                                               (.remove form-space 1)
                                                                               (.add form-space (insert-form))
                                                                               (.repaint form-space)
                                                                               (.revalidate form-space)))))
          update-form (fn [update] (do (score/config! view-layout :constraints view-layout-open)
                                       (score/config! form-space :constraints form-space-open)
                                       (score/config! hide-show :text "<<" :halign :left :font (gtool/getFont 16 :bold))
                                       (if (nil? hidden-comp)
                                         (do
                                           (.add form-space (update)))
                                         (do
                                           (reset! hidden-comp nil)
                                           (doall (map #(.remove form-space %) (reverse (drop 1 (range (count (sutil/children form-space)))))))
                                           (.add form-space (update))))
                                       (.revalidate view-layout)))
          table ((:->table controller) (fn [model] (update-form (fn [] (build-input-form (:->col-meta controller) :model model :more-comps [(back-to-insert) (expand-export)])))))
          view-layout (score/config! view-layout
                       :items [[(gcomp/scrollbox form-space :hscroll :never)]
                               [table]])]
      view-layout)))

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


