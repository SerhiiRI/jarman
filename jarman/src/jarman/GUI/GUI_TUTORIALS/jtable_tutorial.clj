(ns jarman.gui.gui-tutorials.jtable-tutorial
  (:use seesaw.core
        seesaw.border
        seesaw.dev
        seesaw.mig
        seesaw.swingx
        seesaw.color)
  (:import (javax.swing JLayeredPane JLabel JTable JComboBox DefaultCellEditor JCheckBox)
           (javax.swing.table TableCellRenderer TableColumn)
           (java.awt Color Component))
  (:require [jarman.tools.lang :refer :all]))



(def color-label
  (fn []
    (let [isBordered true]
      (doto (proxy [JLabel TableCellRenderer] []
              (^Component getTableCellRendererComponent [^JTable table ^Object color ^Boolean isSelected ^Boolean hasFocus, ^Integer row, ^Integer column]
                (proxy-super setBackground (cast Color color))
                this))
        (.setOpaque true)))))

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
                this))
        (.setOpaque true)))))

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

(def tb (atom (table-x :model (seesaw.table/table-model
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
        [(horizontal-panel :items
                           (map
                            (fn [col-name col-idx]
                              (if (or (in? block-columns-by-name col-name) (in? block-columns-by-index col-idx))
                                (do (println (str "V-" col-idx ": Offline"))
                                    (grid-panel
                                     :columns 1
                                     :items [(label :text col-name
                                                    :enabled? false
                                                    :size [col-w :by 20])
                                             (text
                                              :enabled? false
                                              :size [col-w :by 20])]))
                                (grid-panel
                                 :columns 1
                                 :items [(label :text col-name
                                                :size [col-w :by 20])
                                         (text
                                          :size [col-w :by 20]
                                          :listen [:action (fn [e]
                                                             (let [h-panel (seesaw.util/children (.getParent (.getParent (to-widget e))))
                                                                   all-inputs (map #(second (seesaw.util/children %)) h-panel)
                                                                   sorter (javax.swing.table.TableRowSorter. (.getModel table))]
                                                               (.setRowSorter table sorter)
                                                               (let [pre-filters (doall
                                                                                  (map (fn [inp idx]
                                                                                         (if-not (empty? (value inp))
                                                                                           (do
                                                                                             (println (str "VF-" idx ": " (value inp)))
                                                                                             (javax.swing.RowFilter/regexFilter (value inp) (int-array 1 [idx])))))
                                                                                       all-inputs (range col-c)))
                                                                     filters (doall (filter #(not (nil? %)) pre-filters))]
                                                                 (.setRowFilter sorter (javax.swing.RowFilter/andFilter filters)))))])])))
                            col-names (range col-c)))]
        [(do
           (.setAutoResizeMode table JTable/AUTO_RESIZE_ALL_COLUMNS)
           (.setDefaultRenderer table Color (color-label))
           (.setDefaultRenderer table JComboBox (drop-list))
           (.setFillsViewportHeight table true)
           (.setShowGrid table false)
           (config! table :listen
                    [:mouse-clicked (fn [e])
                     :mouse-motion (fn [e])])
           (scrollable table :size [900 :by 400]))]]))))

;; New jframe
(do (doto (seesaw.core/frame
           :title "title"
           :undecorated? false
           :minimum-size [1000 :by 600]
           :content (mount-table @tb :block-columns-by-index [0] :block-columns-by-name ["Kolor"]))
      (.setLocationRelativeTo nil) seesaw.core/pack! seesaw.core/show!))

