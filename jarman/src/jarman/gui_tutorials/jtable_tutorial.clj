(ns jarman.gui.gui-tutorials.jtable-tutorial
  (:use seesaw.core
        seesaw.border
        seesaw.dev
        seesaw.mig
        seesaw.swingx)
  (:import (javax.swing JLayeredPane JLabel JTable JComboBox DefaultCellEditor JCheckBox)
           (javax.swing.table TableCellRenderer TableColumn)
           (java.awt Color Component)))



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


;; New jframe
(do (doto (seesaw.core/frame
           :title "title"
           :undecorated? false
           :minimum-size [600 :by 400]
           :content (mig-panel
                     :bounds [0 0 500 300]
                     :constraints ["wrap 1" "[grow, center]" "[grow, center]"]
                     :border (empty-border :thickness 10)
                     :items [[(let [tmodel (seesaw.table/table-model
                                            :columns [{:key :name :text "Imie"}
                                                      {:key :lname :text "Kolor" :class java.awt.Color}
                                                      {:key :access :text "Dostęp" :class javax.swing.JComboBox}
                                                      {:key :access :text "tf" :class java.lang.Boolean}
                                                      {:key :num :text "Numer" :class java.lang.Number}
                                                      {:key :num :text "tf2"}]
                                            :rows [["Jan" (seesaw.color/color "#2a2") "tak" true 1]
                                                   ["Fara" (seesaw.color/color "#abc")  "nie" false 4]
                                                   ["Jarek" (seesaw.color/color "#123")  "nie" false 2]
                                                   ["Papaj" (seesaw.color/color "#321")  "tak" true 44]
                                                   ["Monika" (seesaw.color/color "#000")  "nie" false 40]
                                                   ])
                                    table (table-x :model tmodel)]
                                (mig-panel
                                 :bounds [0 0 500 300]
                                 :constraints ["wrap 1" "[grow, center]" "[grow, center]"]
                                 :border (empty-border :thickness 10)
                                 :items
                                 [[(label :text "Tabelki")]
                                  [(text :size [200 :by 20]
                                         :listen [:caret-update (fn [e]
                                                                  (let [sorter (javax.swing.table.TableRowSorter. (.getModel table))]
                                                                    (.setRowSorter table sorter)
                                                                    (.setRowFilter sorter (javax.swing.RowFilter/regexFilter (value e) (int-array 6 [0 1 2 3 4 5])))))])]
                                  [(do
                                       (.setDefaultRenderer table Color (color-label))
                                       (.setDefaultRenderer table JComboBox (drop-list))
                                     (.setColumnControlVisible table true)
                                     (.setFillsViewportHeight table true)
                                     (.setShowGrid table false)
                                     (.setCellSelectionEnabled table true)
                                     
                                     (config! table :listen
                                              [:mouse-clicked (fn [e])
                                               :mouse-motion (fn [e])])
                                     (scrollable table))]]))]]))
      (.setLocationRelativeTo nil) seesaw.core/pack! seesaw.core/show!))


(def tab
  (let [tmodel (seesaw.table/table-model
                :columns [{:key :name :text "Imie"}
                          {:key :lname :text "Kolor" :class java.awt.Color}
                          {:key :access :text "Dostęp" :class javax.swing.JComboBox}
                          {:key :access :text "tf" :class java.lang.Boolean}
                          {:key :num :text "Numer" :class java.lang.Number}
                          {:key :num :text "tf2"}]
                :rows [["Jan" (seesaw.color/color "#2a2") "true" true 1]
                       ["Fara" (seesaw.color/color "#2a2")  "false" false 4]])
        table (table-x :model tmodel)]
    table))

(import org.jdesktop.swingx.sort.RowFilters)
(import javax.swing.RowFilter)

(apply (partial javax.swing.RowFilter/regexFilter "jan") (into-array Integer [(Integer 0)]))
(javax.swing.RowFilter/regexFilter "jan" (int-array 1 0))