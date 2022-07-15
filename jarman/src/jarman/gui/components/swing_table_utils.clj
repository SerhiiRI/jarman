(ns jarman.gui.components.swing-table-utils)

(defn wrapp-adjustment-listener
  "(f [suwaczek-position scrollbarMax]..)" [^javax.swing.JScrollPane scrollpane on-scroll]
  (let [^javax.swing.JScrollBar scrollbar (.getVerticalScrollBar scrollpane)]
    (.addAdjustmentListener scrollbar
     (proxy [java.awt.event.AdjustmentListener] []
       (adjustmentValueChanged [^java.awt.event.AdjustmentEvent ae]
         (let [scrollBar (cast javax.swing.JScrollBar (.getAdjustable ae))
               extent (.. scrollBar getModel getExtent)]
           (on-scroll (+ (.. scrollBar getValue) extent) (.. scrollBar getMaximum))))))
    scrollpane))

