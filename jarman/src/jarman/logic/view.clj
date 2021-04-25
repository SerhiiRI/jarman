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
   [jarman.gui.gui-tools :as gtool]
   [jarman.resource-lib.icon-library :as ico]
   [jarman.tools.swing :as stool]
   [jarman.gui.gui-components :as comps]
   [jarman.config.storage :as storage]
   [jarman.config.environment :as env]
   [jarman.logic.sql-tool :as toolbox :include-macros true :refer :all]
   [jarman.logic.metadata :as mt])
  (:import (java.util Date)
           (java.text SimpleDateFormat)))

(def ^:dynamic prod? false)
(def ^:dynamic sql-connection
  (if prod?
    {:dbtype "mysql" :host "192.168.1.69" :port 3306 :dbname "jarman" :user "jarman" :password "dupa"}
    {:dbtype "mysql" :host "192.168.1.69" :port 3306 :dbname "jarman" :user "jarman" :password "dupa"}
    ;; {:dbtype "mysql", :host "trashpanda-team.ddns.net", :port 3306, :dbname "jarman", :user "jarman", :password "dupa"}
    ;; {:dbtype "mysql" :host "127.0.0.1" :port 3306 :dbname "jarman" :user "root" :password "1234"}
    ))

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


;; (defview user
;;   :tables [:user :permission]
;;   :view   [:first_name :last_name :login :permission_name]
;;   :data   {:inner-join [:permission]
;;            :column [{:user.id :id} :login :password :first_name :last_name :permission_name :configuration :id_permission]}
;;   :export-doc {:doc1 {:name "some name"
;;                       :format :odt
;;                       :model {:model-keys [:User.first_name :User.last_name :Permission.permission_name]
;;                               :data {:inner-join [:permission]
;;                                      :columns [{user.id :User.id} {:first_name :User.first_name} ...]}}}}
;;   :export-data [[:first_name :last_nmame]
;;                 [:permission_name :login]] ?)

(defview user
  :tables [:user :permission]
  :view   [:first_name :last_name :login :permission_name]
  :data   {:inner-join [:permission]
           :column [{:user.id :id} :login :password :first_name :last_name :permission_name :configuration :id_permission]})



((:->data user-view))
;; ;; (defview user)
;; ;; (map :field ((comp :columns :prop) (first (mt/getset! :permission))))
(defview permission
  :tables [:permission]
  :view   [:permission_name]
  :data   {:column [:id :permission_name :configuration]})

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




(def build-insert-form
  (fn [metadata & more-comps]
    (let [complete (atom {})
          vp (smig/mig-panel :constraints ["wrap 1" "0px[grow, fill]0px" "0px[fill]0px"] 
                             :border (sborder/empty-border :thickness 10)
                             :items [[(score/label)]])
          components (concat
                      (map (fn [meta]
                             (cond
                               (= (first (get meta :component-type)) "i")
                               (score/grid-panel :columns 1
                                                 :size [200 :by 50]
                                                 :items [(score/label
                                                          :text (key-to-title (get meta :representation)))
                                                         (comps/input-text
                                                          :args [:listen [:caret-update
                                                                          (fn [e]
                                                                            (swap! complete (fn [storage] (assoc storage
                                                                                                                 (keyword (get meta :field))
                                                                                                                 (score/value (score/to-widget e))))))]])])
                               (= (first (get meta :component-type)) "l")
                               (do
                                 (swap! complete (fn [storage] (assoc storage
                                                                      (keyword (get meta :field))
                                                                      (get meta :key-table))))
                                 (score/grid-panel :columns 1
                                                   :size [200 :by 50]
                                                   :items [(score/label
                                                            :text (key-to-title (get meta :representation))
                                                            :enabled? false)
                                                           (comps/input-text
                                                            :args [:text (get meta :key-table)
                                                                   :enabled? false])]))))
                           metadata)
                      [(score/label :border (sborder/empty-border :top 20))]
                      [(comps/button-basic "Insert new data" (fn [e] (println "Insert data: " @complete)))
                      ;;  (score/label :text "Insert" :listen [:mouse-clicked (fn [e]
                      ;;                                                        (println "Insert " @complete)
                      ;;                                                             ;;  (println "Map" (merge template-map @complete)
                      ;;                                                             ;;           ;; ((:user->insert user-view)
                      ;;                                                             ;;           ;;  (merge {:id nil :login nil :password nil :first_name nil :last_name nil :id_permission nil}
                      ;;                                                             ;;           ;;   @complete))
                      ;;                                                             ;;           )
                      ;;                                                        )])
                       ]more-comps)]
      (score/config! vp :items (gtool/join-mig-items components)))))


(def auto-builder--table-view
  (fn [controller]
    (let [controller (if (nil? controller) user-view controller)
          ;; ico-open (stool/image-scale ico/plus-64-png 28)
          ;; ico-close (stool/image-scale ico/minus-grey-64-png 28)
          hidden-comp (atom (score/label))
          expand-export (score/flow-panel 
                         :border (sborder/compound-border (sborder/empty-border :top 5) 
                                                          (sborder/line-border :top 2 :color "#999")
                                                          (sborder/empty-border :top 50))
                         :items [(comps/button-expand "Export" (smig/mig-panel
                                                                :constraints ["wrap 1" "5px[grow, fill]5px" "10px[fill]0px"]
                                                                :border (sborder/line-border :left 2 :right 2 :bottom 2 :color "#fff")
                                                                :items [[(score/horizontal-panel
                                                                          :items [(comps/input-text :args [:text "\\path\\to\\export"])
                                                                                  (score/label :text "[-]" :background "#abc" :border (sborder/empty-border :thickness 5))])]
                                                                        [(score/checkbox :text "ODT" :selected? true)]
                                                                        [(score/checkbox :text "DOCX")]
                                                                        [(comps/button-basic "Service raport" (fn [e]))]
                                                                        [(score/label)]])
                                                      :background "#fff"
                                                      :min-height 220
                                                      :border (sborder/compound-border (sborder/empty-border :left 10 :right 10)))])
          insert-form (build-insert-form (:->col-meta controller) [expand-export])
          form-space (smig/mig-panel :constraints ["wrap 1" "0px[grow, fill]0px" "0px[fill]0px"])
          hide-show (score/label :text "<<"
                                 :background "#bbb"
                                 :foreground "#fff"
                                 :font (gtool/getFont 16 :bold)
                                 :border (sborder/empty-border :left 2 :right 2)
                                 :listen [:mouse-entered gtool/hand-hover-on])
          ;; default-bg-color (score/config hide-show :background)
          form-space (score/config! form-space
                                    :items [[hide-show] [insert-form]])
          hide-show (score/config! hide-show :listen [:mouse-clicked (fn [e]
                                                                       (let [inside (count (sutil/children form-space))]
                                                                  ;;  (println "Inside " inside)
                                                                         (cond
                                                                           (= 1 inside)
                                                                           (do
                                                                             (score/config! form-space :constraints ["wrap 1" "0px[grow, fill]0px" "0px[fill]0px"])
                                                                             (score/config! hide-show :text "<<" :font (gtool/getFont 16 :bold))
                                                                             (.add form-space @hidden-comp)
                                                                             (.revalidate form-space))
                                                                           (= 2 inside)
                                                                           (do
                                                                             (score/config! form-space :constraints ["wrap 1" "0px[grow, fill]0px" "0px[grow, fill]0px"])
                                                                             (score/config! hide-show :text "..." :valign :top)
                                                                             (reset! hidden-comp (second (sutil/children form-space)))
                                                                             (.remove form-space 1)
                                                                             (.revalidate form-space)))))])
          update-form (smig/mig-panel :constraints ["" "[200, fill]" "[grow, fill]"]
                                      :items [[(comps/button-basic "Update Test"
                                                                   (fn [e]
                                                                     (println "Update: Twoja stara")
                                                                     (reset! hidden-comp insert-form)
                                                                     (.remove form-space 1)
                                                                     (.add form-space @hidden-comp)
                                                                     (.revalidate form-space)))]])
          ;; table ((:->table controller) (fn [model] (score/return-from-dialog form-panel model)))
          table ((:->table controller) (fn [model] (score/config! form-space :items [[hide-show]
                                                                                     [update-form]])))
          view-layout (smig/mig-panel
                       :constraints ["" "0px[fill]0px[grow, fill]0px" "0px[grow, fill]0px"]
                       :items [[form-space]
                               [table]])]
      view-layout)))

;; (@jarman.gui.gui-app/startup)

;; (let [my-frame (-> (doto (score/frame
;;                           :title "test"
;;                           :size [1000 :by 800]
;;                           :content
;;                           (auto-builder--table-view user-view))
;;                      (.setLocationRelativeTo nil) score/pack! score/show!))]
;;   (score/config! my-frame :size [1000 :by 800]))












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
