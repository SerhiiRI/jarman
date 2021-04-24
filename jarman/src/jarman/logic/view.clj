(ns jarman.logic.view
  (:refer-clojure :exclude [update])
  (:require
   ;; Clojure toolkit 
   [clojure.data :as data]
   [clojure.string :as string]
   [clojure.java.jdbc :as jdbc]
   ;; Seesaw components
   [seesaw.core :as seesaw]
   [seesaw.border :as sborder]
   [seesaw.dev :as sdev]
   [seesaw.mig :as smig]
   [seesaw.swingx :as swingx]
   ;; Jarman toolkit
   [jarman.tools.lang :refer :all]
   [jarman.config.storage :as storage]
   [jarman.config.environment :as env]
   [jarman.logic.sql-tool :as toolbox :include-macros true :refer :all]
   [jarman.logic.metadata :as mt])
  (:import (java.util Date)
           (java.text SimpleDateFormat)))

(def ^:dynamic prod? false)
(def ^:dynamic sql-connection
  (if prod?
    ;; {:dbtype "mysql" :host "192.168.1.69" :port 3306 :dbname "jarman" :user "jarman" :password "dupa"}
    {:dbtype "mysql", :host "trashpanda-team.ddns.net", :port 3306, :dbname "jarman", :user "jarman", :password "dupa"}
    {:dbtype "mysql" :host "127.0.0.1" :port 3306 :dbname "jarman" :user "root" :password "1234"}))


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
;;       my-frame (-> (doto (seesaw/frame
;;                           :title "test"
;;                           :size [0 :by 0]
;;                           :content mig)
;;                      (.setLocationRelativeTo nil) pack! show!))]
;;   (config! my-frame :size [600 :by 600])
;;   (.add mig (label :text "Two")))

;; [{:key :name :text "Imie"}
;;  {:key :lname :text "Nazwisko"}
;;  {:key :lname :text "Zwierzak"}
;;  {:key :access :text "Kolor"  :class Color}
;;  {:key :access :text "Dostêp" :class javax.swing.JComboBox}
;;  {:key :access :text "TF" :class java.lang.Boolean}
;;  {:key :num :text "Numer" :class java.lang.Number}
;;  {:key :num :text "P³eæ" :class javax.swing.JComboBox}
;;  {:key :num :text "Wiek" :class java.lang.Number}
;;  {:key :num :text "Miejscowo¶æ"}]
;; (table :model (seesaw.table/table-model
;;                :columns [{:key :col1 :text "Col 1"} 
;;                          {:key :col2 :text "Col 2"}]
;;                :rows [["Dane 1" "Dane 2"]]))

(defn construct-table [model]
  (fn [listener-fn]
    (let [TT (seesaw/table :model (model))]
      (seesaw/listen TT :selection (fn [e] (listener-fn (seesaw.table/value-at TT (seesaw/selection TT)))))
      (seesaw/scrollable TT :hscroll :as-needed :vscroll :as-needed))))

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
                     :->col-meta colmeta#})))) 


;; (defview user)
(defview user
  :tables [:user :permission]
  :view   [:first_name :last_name :login :permission_name]
  :data   {:inner-join [:permission]
           :column [{:user.id :id} :login :password :first_name :last_name :permission_name :configuration :id_permission]})

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



