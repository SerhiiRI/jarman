(ns jarman.gui.gui-tutorials.jtable-tutorial
  (:use seesaw.core
        seesaw.border
        seesaw.dev
        seesaw.mig
        seesaw.swingx
        seesaw.color)
  (:import (javax.swing JLayeredPane  JLabel JTable JTextField JComboBox DefaultCellEditor JCheckBox BoxLayout JPanel AbstractCellEditor)
           (javax.swing.table TableCellRenderer TableColumn TableCellEditor DefaultTableModel)
           (java.awt Color Component BorderLayout)
           (java.awt.event MouseAdapter)
           )
  (:require [jarman.tools.lang :refer :all]
            [jarman.gui.swing.table :as stable]))



(def color-label
  (fn []
    (let [isBordered true]
      (doto (proxy [JLabel TableCellRenderer] []
              (^Component getTableCellRendererComponent [^JTable table ^Object color ^Boolean isSelected ^Boolean hasFocus, ^Integer row, ^Integer column]
                (proxy-super setBackground (cast Color color))
                this))
        (.setOpaque true)))))


(def headerEv
  (fn [vp txt table]
    (let []
      (doto (proxy [MouseAdapter] []
              (mouseClicked [^MouseAdapter e]
                            (println)
                            (println "----------------------")
                            (println "VP"   (config vp :id))
                (config! txt :text "Dupa")
                            (.grabFocus txt)
                            (.requestFocus txt true)
                (.repaint (.getTableHeader table))
                (.repaint txt)))))))

;; (def header-with-filter-input
;;   (fn []
;;     (let []
;;       (doto (proxy [JPanel TableCellRenderer TableCellEditor] []
;;               (^Boolean isCellEditable [^Integer row ^Integer col] true)
;;               (^Component getTableCellRendererComponent [^JTable table ^Object value ^Boolean isSelected ^Boolean hasFocus, ^Integer row, ^Integer column]
;;                 (let [lbl (label :text value)
;;                       txt (text :editable? true :listen [:caret-update (fn [e] (.repaint table)
;;                                                                          (.revalidate table)
;;                                                                          (.repaint table))])
;;                       vp (vertical-panel :items [lbl txt] :id :dupa)]
;;                   (if hasFocus (do (println "Focus") (.requestFocus txt true)))
;;                                                           ;; (.addMouseListener (.getTableHeader table) (headerEv vp txt table))
;;                   vp)
;;                                                         ;; (vertical-panel [(label :text value) (text)])
;;                 ))
;;         (.setOpaque true)
;;         ))))

(def header-with-filter-input
  (fn []
    (let []
      (doto (proxy [JTextField TableCellRenderer] []
              (^Component getTableCellRendererComponent [^JTable table ^Object value ^Boolean isSelected ^Boolean hasFocus, ^Integer row, ^Integer column]
                                                        
                                                        this))
        (.setOpaque true)
        ))))


(def header-with-filter-input-editor
  (fn []
    (let []
      (doto (proxy [JTextField TableCellEditor] []
              (^Component getTableCellRendererComponent [^JTable table ^Object value ^Boolean isSelected ^Boolean hasFocus, ^Integer row, ^Integer column]
                                                        
                                                        this))
        (.setOpaque true)
        ))))

;; (* (.getColumnCount @tb) (.getRowCount @tb))

(def drop-list
  (fn []
    (let []
      (doto (proxy [JComboBox TableCellRenderer] []
              (^Component getTableCellRendererComponent [^JTable table ^Object value ^Boolean isSelected ^Boolean hasFocus, ^Integer row, ^Integer column]
                (.addItem this "true")
                (.addItem this "false")
                (.setEditable this true)
                (.setEnabled this true)
                (.setSelectedItem this value)
                (.setEditor this (.getEditor this))
                ;; (if (= hasFocus true) (do (println "Combo focus")))
                (.getModel table)
                this)
              )
        (.setOpaque true)))))

(def input-txt
  (fn []
    (let [my-text-field (text)]
      (proxy [AbstractCellEditor TableCellEditor] []
        (^Component getTableCellEditorComponent [^JTable table, ^Object value, isSelected, row, column]
          my-text-field)
        (^Object getCellEditorValue []
          (value my-text-field))
        (^Boolean isCellEditable [e] true)))))



;; (def EditableTableModel
;;   (fn []
;;     (let []
;;       (doto (proxy [DefaultTableModel] []
;;               (^Boolean isCellEditable [^Integer row ^Integer col] true))))))

;; (defprotocol CellEditable
;;   (isCellEditable [this row col]))

;; ;; (extend-protocol CellEditable
;; ;;   javax.swing.table.DefaultTableModel
;; ;;   (isCellEditable [this row col] true))

;; (extend-type javax.swing.table.DefaultTableModel
;;   CellEditable
;;   (isCellEditable [this row col] "dupa"))

(def seed-col
  (fn []
    [{:key :name :text "Imie"}
     {:key :lname :text "Nazwisko"}
     {:key :lname :text "Zwierzak"}
     {:key :access :text "Kolor"  :class Color}
     {:key :access :text "Dostęp" :class javax.swing.JComboBox}
     {:key :access :text "TF" :class java.lang.Boolean}
     {:key :num :text "Numer" :class java.lang.Number}
     {:key :num :text "Płeć" :class javax.swing.JComboBox}
     {:key :num :text "Wiek" :class java.lang.Number}
     {:key :num :text "Miejscowość"}]))

(def seed-row
  (fn []
    [["Jan" "Kowalski" "rybki" (color "#2a2") "tak" true  1 "Mężczyzna" 28 "Zadupolis"]
     ["Anna" "Nowak" "pieski" (color "#faa") "nie" false  0 "Kobieta" 30 "Polis"]
     ["Mariusz" "Nowak" "rybki" (color "#a2a") "nie" true  1 "Mężczyzna" 30 "Polis"]
     ["Kamil" "Nowak" "pieski" (color "#34c") "nie" true  1 "Mężczyzna" 30 "Polis"]]))


(def cols (fn [] (vec (apply concat (map (fn [x] (seed-col)) (range 1))))))
(def rows (fn [] (concat
                  (vec (map (fn [x] (first (seed-row))) (range 10)))
                  (vec (map (fn [x] (second (seed-row))) (range 10)))
                  (vec (map (fn [x] (nth (seed-row) 2)) (range 10)))
                  (vec (map (fn [x] (last (seed-row))) (range 10))))))

(def tb (atom (score/table-x :model (stable/table-model
                               :columns (cols)
                               :rows (rows)))))

(def mount-table
  (fn [table &
       {:keys [block-columns-by-index
               block-columns-by-name]
        :or {block-columns-by-index []
             block-columns-by-name []}}]
    (let [col-c (.getColumnCount table)
          col-w (/ 900 col-c)
          col-names (doall (map (fn [idx] (.getColumnName table idx)) (range col-c)))]
      (mig-panel
       :constraints ["wrap 1" "[grow, center]" "[grow, center]"]
       :border (empty-border :thickness 10)
       :items
       [[(label :text "Tabelki")]
        ;; [(horizontal-panel :items
        ;;                    (map
        ;;                     (fn [col-name col-idx]
        ;;                       (if (or (in? block-columns-by-name col-name) (in? block-columns-by-index col-idx))
        ;;                         (do (println (str "V-" col-idx ": Offline"))
        ;;                             (grid-panel
        ;;                              :columns 1
        ;;                              :items [(label :text col-name
        ;;                                             :enabled? false
        ;;                                             :size [col-w :by 20])
        ;;                                      (text
        ;;                                       :enabled? false
        ;;                                       :size [col-w :by 20])]))
        ;;                         (grid-panel
        ;;                          :columns 1
        ;;                          :items [(label :text col-name
        ;;                                         :size [col-w :by 20])
        ;;                                  (text
        ;;                                   :size [col-w :by 20]
        ;;                                   :listen [:action (fn [e]
        ;;                                                      (let [h-panel (seesaw.util/children (.getParent (.getParent (to-widget e))))
        ;;                                                            all-inputs (map #(second (seesaw.util/children %)) h-panel)
        ;;                                                            sorter (javax.swing.table.TableRowSorter. (.getModel table))]
        ;;                                                        (.setRowSorter table sorter)
        ;;                                                        (let [pre-filters (doall
        ;;                                                                           (map (fn [inp idx]
        ;;                                                                                  (if-not (empty? (value inp))
        ;;                                                                                    (do
        ;;                                                                                      (println (str "VF-" idx ": " (value inp)))
        ;;                                                                                      (javax.swing.RowFilter/regexFilter (value inp) (int-array 1 [idx])))))
        ;;                                                                                all-inputs (range col-c)))
        ;;                                                              filters (doall (filter #(not (nil? %)) pre-filters))]
        ;;                                                          (.setRowFilter sorter (javax.swing.RowFilter/andFilter filters)))))])])))
        ;;                     col-names (range col-c)))]
        [(do
          ;;  (.setDefaultRenderer (.getTableHeader table) (header-with-filter-input))
           (.setAutoResizeMode table JTable/AUTO_RESIZE_ALL_COLUMNS)
           (.setDefaultRenderer table Color (color-label))
           (.setDefaultRenderer table JComboBox (drop-list))
           (.setFillsViewportHeight table true)
           (.setShowGrid table false)
           (.setCellEditor (.getColumn (.getColumnModel table) 0) (input-txt))
           (.setCellEditor (.getColumn (.getColumnModel (.getTableHeader table)) 0) (input-txt))
          ;;  (-> table .getTableHeader .getColumnModel (.getColumn 0) (.setCellEditor (input-txt)))
           ;;  (doall
          ;;   (map (fn [idx]
          ;;          (.setCellEditor (.getColumn (.getColumnModel (.getTableHeader table)) idx) (DefaultCellEditor. (header-with-filter-input-editor)
          ;;                                                                   ;;  (doto (JComboBox.) (.addItem "tak") (.addItem "nie"))
          ;;                                                                                       )))
          ;;        (range (.getColumnCount table))))

          ;;  (.setEditable table true)
          ;;  (println (.isCellEditable table 0 4))
          ;;  (println (.isCellEditable table -1 4))
          ;;  (.addMouseListener (.getTableHeader table) (headerEv))
          ;;  (.setAutoCreateRowSorter table true)
           (config! table :listen
                    [:mouse-clicked (fn [e] (do
                                              ;; (println "Column" (.getSelectedColumn table) "Row" (.getSelectedRow table))
                                              ))
                     :mouse-motion (fn [e])])
           (scrollable table :size [900 :by 400]))]]))))

;; New jframe
(do (doto (seesaw.core/frame
           :title "title"
           :undecorated? false
           :minimum-size [1000 :by 600]
           :content (mount-table @tb :block-columns-by-index [0] :block-columns-by-name ["Kolor"]))
      (.setLocationRelativeTo nil) seesaw.core/pack! seesaw.core/show!))

