(ns jarman.gui.gui-tutorials.jtable-tutorial
  (:use seesaw.core
        seesaw.border
        seesaw.dev
        seesaw.mig
        seesaw.swingx
        seesaw.color)
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
    [["Jan" "Kowalski" "rybki" (color "#2a2") "tak" true  1 "Mężczyzna" 28 "Zadupolis"
     "Jan" "Kowalski" "rybki" (color "#2a2") "tak" true  1 "Mężczyzna" 28 "Zadupolis"
     "Jan" "Kowalski" "rybki" (color "#2a2") "tak" true  1 "Mężczyzna" 28 "Zadupolis"]
     ["Anna" "Nowak" "pieski" (color "#a2a") "nie" false  0 "Kobieta" 30 "Polis"
      "Anna" "Nowak" "pieski" (color "#a2a") "nie" false  0 "Kobieta" 30 "Polis"
      "Anna" "Nowak" "pieski" (color "#a2a") "nie" false  0 "Kobieta" 30 "Polis"]]))



(def tb (atom (table :model (seesaw.table/table-model
                             :columns {}
                             :rows {}))))

(def mount-table 
  (fn [table filters]
    (let []
      (mig-panel
       :constraints ["wrap 1" "[grow, center]" "[grow, center]"]
       :border (empty-border :thickness 10)
       :items
       [[(label :text "Tabelki")]
        [(text :size [200 :by 20]
               :listen [:caret-update (fn [e]
                                        (let [sorter (javax.swing.table.TableRowSorter. (.getModel table))]
                                          (.setRowSorter table sorter)
                                          (.setRowFilter sorter (javax.swing.RowFilter/regexFilter (value e) (int-array 1 [0])))))])]
        [(do
           (.setDefaultRenderer table Color (color-label))
           (.setDefaultRenderer table JComboBox (drop-list))
          ;;  (.setColumnControlVisible table true)
           (.setFillsViewportHeight table true)
           (.setShowGrid table false)
          ;;  (.setCellSelectionEnabled table true)
           (config! table :listen
                    [:mouse-clicked (fn [e])
                     :mouse-motion (fn [e])])
           (scrollable table))]]))))

;; New jframe
(do (doto (seesaw.core/frame
           :title "title"
           :undecorated? false
           :minimum-size [600 :by 400]
           :content (mount-table @tb {}))
      (.setLocationRelativeTo nil) seesaw.core/pack! seesaw.core/show!))
 

(def cols (vec (apply concat (map (fn [x] (seed-col)) (range 1)))))
(def rows (concat (vec (map (fn [x] (first (seed-row))) (range 500))) 
                  (vec (map (fn [x] (second (seed-row))) (range 500)))))

(config! @tb :model (seesaw.table/table-model
                     :columns cols
                     :rows rows))