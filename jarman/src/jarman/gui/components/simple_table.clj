(ns jarman.gui.components.simple-table
  (:require
   [clojure.pprint :refer [cl-format]]
   [seesaw.core :as c]
   [seesaw.swingx :as swingx]
   [jarman.lang :refer :all]
   ;; DB interaction
   [jarman.logic.metadata :as metadata]
   [jarman.logic.connection :as db]
   [jarman.logic.sql-tool :refer [select! update! delete! insert!]])
  (:import
   [javax.swing JViewport JTable JPanel ListSelectionModel]
   [javax.swing.table TableCellRenderer]
   [java.awt Component Rectangle Point]))


;;  _____  _    ____  _     _____ 
;; |_   _|/ \  | __ )| |   | ____|
;;   | | / _ \ |  _ \| |   |  _|  
;;   | |/ ___ \| |_) | |___| |___ 
;;   |_/_/   \_\____/|_____|_____|
;; 

;; fixme:serhii table header
;; header input cannot be edited in TableHeader
;; (let [defaultHeaderR (.. t getTableHeader getDefaultRenderer)]
;;   (. (.getTableHeader t) setDefaultRenderer
;;      (proxy [TableCellRenderer] []
;;        (^Component getTableCellRendererComponent [^JTable table, ^Object value, isSelected, hasFocus, row, column]
;;         (let [v (.toString value)
;;               ^Component cmp (.getTableCellRendererComponent defaultHeaderR table, value, isSelected, hasFocus, row, column)]
;;           (c/border-panel
;;            :north (c/text :text "search..." :editable? true)
;;            :south cmp))))))

(defn table-model-by-metadata-information
  [^clojure.lang.PersistentVector model-tables-v
   ^clojure.lang.PersistentVector model-field-qualified-v]
  (as-> model-tables-v E
    (apply metadata/return-metadata E)
    (mapcat (comp :columns :prop) E)
    (group-by-apply :field-qualified E :apply-group first)
    (for [col-qualified model-field-qualified-v
          :let [col-metadata (if (map? col-qualified) col-qualified (get E col-qualified nil))]]
      (do (assert (some? col-metadata) (cl-format nil "Column `~A` not exist in any of selected tables: ~{`~A`~^, ~}."  col-qualified model-tables-v))
          {:key  (:field-qualified col-metadata)
           :text (:representation col-metadata)}))))

(defn simple-table [& {:keys [model data on-select custom-renderers] :or {data [] on-select (fn [v])}}]
  {:pre [(sequential? model)]}
  (where
   ((col-index (->> model (map :key) (map-indexed vector) (mapcat reverse) (apply hash-map)))
    (t (c/table :model [:columns model :rows data])))
   ;; --- adding custom column renderers
   (doseq [[column-key column-renderer-fn] custom-renderers]
     (if-let [column-index (get col-index column-key nil)]
       (let [select-with-column-key (. (. t getColumnModel) getColumn column-index)]
         (.setCellRenderer
          select-with-column-key
          (proxy [TableCellRenderer] []
            (^Component getTableCellRendererComponent [^JTable table, ^Object value, isSelected, hasFocus, row, column]
             (column-renderer-fn table value isSelected hasFocus row column)))))
       (println (format "Error! column key `%s` for override rendering doesn't found" (str column-key)))))
   ;; --- inject events
   (c/listen t :selection (fn [e] (on-select (seesaw.table/value-at t (c/selection t)))))
   ;; (c/config! :horizontal-scroll-enabled? true)
   (.setColumnSelectionAllowed t false)
   (c/config! t :show-grid? false)
   (c/config! t :show-horizontal-lines? true)
   t))

(defn change-model! [^JTable table & {:keys [model custom-renderers data] :or {data []}}]
  {:pre [(sequential? model)]}
  (c/config! table :model [:columns model :rows data])
  (where
    ((col-index (->> model (map :key) (map-indexed vector) (mapcat reverse) (apply hash-map))))
    (doseq [[column-key column-renderer-fn] custom-renderers]
      (if-let [column-index (get col-index column-key nil)]
        (let [select-with-column-key (. (. table getColumnModel) getColumn column-index)]
          (.setCellRenderer
            select-with-column-key
            (proxy [TableCellRenderer] []
              (^Component getTableCellRendererComponent [^JTable table, ^Object value, isSelected, hasFocus, row, column]
               (column-renderer-fn table value isSelected hasFocus row column)))))
        (println (format "Error! column key `%s` for override rendering doesn't found" (str column-key))))))
  table)

(defn scrollToRegionByRowColumnIndex! [^JTable table, ^long rowIndex, ^long vColIndex]
  (assert (instance? JViewport (.getParent table)))
  (let [^JViewport viewport (.getParent table)
        ;; This rectangle is relative to the table where the
        ;; northwest corner of cell (0,0) is always (0,0).
        ^Rectangle rect (.getCellRect table rowIndex vColIndex true)
        ;; The location of the viewport relative to the table
        ^Point pt (.getViewPosition viewport)]
    ;; Translate the cell location so that it is relative
    ;; to the view, assuming the northwest corner of the
    ;; view is (0,0)
    (.setLocation rect (- (.-x rect) (.-x pt)) (- (.-y rect) (.-y pt)))
    (.scrollRectToVisible table rect)
    (. table repaint))
  table)

(defn scrollAndSelectSpecificRow! [^JTable table, ^long rowIndex]
  (. (. table getSelectionModel) setSelectionMode ListSelectionModel/SINGLE_SELECTION)
  (. (. table getSelectionModel) clearSelection)
  (. (. table getSelectionModel) setSelectionInterval rowIndex rowIndex)
  (. table scrollRectToVisible (Rectangle. (.getCellRect table rowIndex, 0, true)))
  (. table repaint)
  table)

(defn scrollAndSelectRowRange! [^JTable table, ^long rowStartIndex, ^long rowEndIndex]
  (let [[rowStartIndex rowEndIndex] (sort [rowStartIndex rowEndIndex])]
    (. (. table getSelectionModel) setSelectionMode ListSelectionModel/SINGLE_INTERVAL_SELECTION)
    (. (. table getSelectionModel) clearSelection)
    (. (. table getSelectionModel) setSelectionInterval rowStartIndex rowEndIndex)
    (. table scrollRectToVisible (Rectangle. (.getCellRect table rowEndIndex, 0, true)))
    (. table repaint)
    table))

(defn scrollAndSelectRowIndexes! [^JTable table, indexRange]
  (when (seq indexRange)
    (. (. table getSelectionModel) setSelectionMode ListSelectionModel/MULTIPLE_INTERVAL_SELECTION)
    (. (. table getSelectionModel) clearSelection)
    (doseq [i indexRange]
      (. (. table getSelectionModel) addSelectionInterval i i))
    (. table scrollRectToVisible (Rectangle. (.getCellRect table (first indexRange), 0, true)))
    (. table repaint))
  table)

;;  ____  _____ __  __  ___ 
;; |  _ \| ____|  \/  |/ _ \
;; | | | |  _| | |\/| | | | |
;; | |_| | |___| |  | | |_| |
;; |____/|_____|_|  |_|\___/

(comment
  ;; ===========
  ;; RANDOM DATA 
  (def rand-data
    (->> (cycle (vec (db/query (select!
                                {:limit 1
                                 :table_name :jarman_user
                                 :column [:#as_is :jarman_user.login :jarman_user.password :jarman_user.first_name :jarman_user.last_name :jarman_user.configuration :jarman_user.id_profile :jarman_profile.name :jarman_profile.configuration]
                                 :inner-join :jarman_user->jarman_profile}))))
         (take 100)
         (map (fn [x] (assoc x
                            :jarman_user.login (apply str (take 40 (repeatedly #(char (+ (rand 26) 65)))))
                            :jarman_user.password (rand-nth [true false])
                            :jarman_user.first_name (* (rand) (rand-int 10000)))))))

  ;; ==============
  ;; TABLE INSTANCE

  (def t
    (simple-table
      :model
      [{:key :jarman_user.login, :text "Login"}
       {:key :jarman_user.password, :text "Password"}
       {:key :jarman_user.first_name, :text "First name"}
       {:key :jarman_user.last_name, :text "Last name"}
       {:key :jarman_profile.name, :text "Name"}]
      :data rand-data
      :custom-renderers
      {:jarman_user.password
       (fn [^JTable table, ^Object value, isSelected, hasFocus, row, column]
         (cond->
             (c/label
               :font {:name "monospaced"}
               :h-text-position :right
               :text (if value "[X]" "[ ]"))
           isSelected (c/config! :background (.getSelectionBackground table))))
       :jarman_user.first_name
       (fn [^JTable table, ^Object value, isSelected, hasFocus, row, column]
         (cond->
             (c/label
               :font {:name "monospaced"}
               :h-text-position :right
               :text (format "%.2f $(buks)" value))
           isSelected (c/config! :background (.getSelectionBackground table))))}))

  ;; debug frame
  (-> (doto (seesaw.core/frame
             :title "Jarman" 
             :content (seesaw.mig/mig-panel
                       :constraints ["wrap 1" "0px[grow, fill]0px" "0px[fill, top]0px"]
                       :items [[(c/scrollable t :hscroll :as-needed
                                              :vscroll :as-needed
                                              :border (seesaw.border/line-border :thickness 0 :color "#fff"))]]))
        (.setLocationRelativeTo nil)
        seesaw.core/pack!
        seesaw.core/show!))

  ;; =====================
  ;; SCROLLING TESTING 
  (scrollToRegionByRowColumnIndex! t 40 0)
  (scrollAndSelectSpecificRow! t  50)
  (scrollAndSelectRowRange!    t  45 50)
  (scrollAndSelectRowIndexes!  t  [45 47 49 51])

  ;; =====================
  ;; MODEL UPDATE EXAMPLE 

  ;; config 1. 
  (change-model! t
    :model
    [{:key :jarman_user.login, :text "Login"}
     {:key :jarman_user.password, :text "Password"}
     {:key :jarman_user.first_name, :text "First name"}
     {:key :jarman_user.last_name, :text "Last name"}
     {:key :jarman_profile.name, :text "Name"}]
    :data rand-data
    :custom-renderers
    {:jarman_user.password
     (fn [^JTable table, ^Object value, isSelected, hasFocus, row, column]
       (cond->
           (c/label
             :font {:name "monospaced"}
             :h-text-position :right
             :text (if value "[X]" "[ ]"))
         isSelected (c/config! :background (.getSelectionBackground table))))
     :jarman_user.first_name
     (fn [^JTable table, ^Object value, isSelected, hasFocus, row, column]
       (cond->
           (c/label
             :font {:name "monospaced"}
             :h-text-position :right
             :text (format "%.2f $(buks)" value))
         isSelected (c/config! :background (.getSelectionBackground table))))})
  ;; config 2. 
  (change-model! t
    :model
    [{:key :jarman_user.login, :text "Login"}
     {:key :jarman_user.first_name, :text "First name"}
     {:key :jarman_user.last_name, :text "Last name"}
     {:key :jarman_profile.name, :text "Name"}]
    :data    (vec
               (db/query
                 (select!
                   {:limit 1
                    :table_name :jarman_user
                    :column [:#as_is :jarman_user.login :jarman_user.password :jarman_user.first_name :jarman_user.last_name :jarman_user.configuration :jarman_user.id_profile :jarman_profile.name :jarman_profile.configuration]
                    :inner-join :jarman_user->jarman_profile}))))
  ;; config 3. 
  (change-model! t
    :model
    [{:key :jarman_user.login, :text "Login"}
     {:key :jarman_user.first_name, :text "First name"}
     {:key :jarman_user.last_name, :text "Last name"}
     {:key :jarman_profile.name, :text "Name"}
     {:key :jarman_user.configuration, :text "User configuration"}]
    :tables [:jarman_user :jarman_profile]
    :columns [:jarman_user.login :jarman_user.first_name :jarman_user.last_name :jarman_profile.name :jarman_user.configuration]
    :data (vec
            (db/query
              (select!
                {:table_name :jarman_user
                 :column
                 [:#as_is
                  :jarman_user.login
                  :jarman_user.password
                  :jarman_user.first_name
                  :jarman_user.last_name
                  :jarman_user.configuration
                  :jarman_user.id_profile
                  :jarman_profile.name
                  :jarman_profile.configuration]
                 :inner-join :jarman_user->jarman_profile})))))



