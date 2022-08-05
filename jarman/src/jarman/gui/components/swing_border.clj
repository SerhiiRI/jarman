;; TODO
;; [ ] add documentations
(ns jarman.gui.components.swing-border
  (:require
   [jarman.lang :refer :all]
   [jarman.faces :as face]
   [jarman.gui.components.swing-common])
  (:import
   [javax.swing BorderFactory]))

(defn title-border [& {:keys [^javax.swing.border.Border border title title-justification title-position
                              ^java.awt.Font title-font ^java.awt.Color title-color]
                       :or {title-justification :default title-position :default
                            title-font (jarman.gui.components.swing-common/font face/f-regular face/s-foreground)
                            title-color (seesaw.color/color face/c-foreground)}}]
  (let [title-color (if (string? title-color) (seesaw.color/color title-color) title-color)
        position
        (get title-position
          {:default javax.swing.border.TitledBorder/DEFAULT_POSITION
           :above-bottom javax.swing.border.TitledBorder/ABOVE_BOTTOM
           :above-top javax.swing.border.TitledBorder/ABOVE_TOP
           :below-bottom javax.swing.border.TitledBorder/BELOW_BOTTOM
           :below-top javax.swing.border.TitledBorder/BELOW_TOP
           :bottom javax.swing.border.TitledBorder/BOTTOM
           :top javax.swing.border.TitledBorder/TOP}
          javax.swing.border.TitledBorder/DEFAULT_POSITION)
        justification
        (get title-justification
          {:center javax.swing.border.TitledBorder/CENTER
           :default javax.swing.border.TitledBorder/DEFAULT_JUSTIFICATION
           :left javax.swing.border.TitledBorder/LEFT
           :right javax.swing.border.TitledBorder/RIGHT
           :leading javax.swing.border.TitledBorder/LEADING
           :trailing javax.swing.border.TitledBorder/TRAILING}
          javax.swing.border.TitledBorder/DEFAULT_JUSTIFICATION)]
    (BorderFactory/createTitledBorder border title justification position title-font title-color)))

(defn stroke-border
  [& {:keys [thickness pattern ^java.awt.Color color]
      :or {pattern :dot-line thickness 1.0 color (seesaw.color/color face/c-foreground)}}]
  {:pre [(#{:dot-line :stroke-line :wide-stroke-line} pattern)]}
  (where
    ((color (if (string? color) (seesaw.color/color color) color))
     (basic-stroke-pattern
       (get {:wide-stroke-line
             (java.awt.BasicStroke. thickness
               java.awt.BasicStroke/CAP_BUTT
               java.awt.BasicStroke/JOIN_MITER
               15.0 (float-array 1 15.0) 0.0)
             :stroke-line
             (java.awt.BasicStroke. thickness
               java.awt.BasicStroke/CAP_BUTT
               java.awt.BasicStroke/JOIN_MITER
               10.0 (float-array 1 10.0) 0.0)
             :dot-line
             (java.awt.BasicStroke. thickness
               java.awt.BasicStroke/CAP_BUTT
               java.awt.BasicStroke/JOIN_MITER
               1.0 (float-array 2 [3.0 5.0]) 0.0)}
         pattern)))
    (BorderFactory/createStrokeBorder
      basic-stroke-pattern color)))

(defn ^:private border-from-config [m]
  (if (= (:type m) :stroke)
    (where
      ((args (select-keys m [:thickness :pattern :color])))
      (->> args
        (mapcat identity)
        (apply stroke-border)))
    (where
      ((alias-map {:b :bottom :t :top :l :left :r :right})
       (border-color (:color m))
       (border-fn
         (if border-color
           (partial seesaw.border/line-border :color border-color)
           (partial seesaw.border/empty-border)))
       (bordern
         (->> (select-keys m [:b :t :l :r :a :h :v])
           (reduce
             (fn [cfg [direction v]]
               (case direction
                 :a (reduced {:bottom v :top v :left v :right v})
                 :h (assoc cfg :left v :right v)
                 :v (assoc cfg :bottom v :top v)
                 (assoc cfg (direction alias-map) v))) {})
           (mapcat identity)
           (apply border-fn))))
      (if (= (:type m) :title)
        (->> (assoc (select-keys m [:title :title-justification :title-position :title-font :title-color]) :border bordern)
          (mapcat identity)
          (apply title-border))
        bordern))))

(defn border
  "Description
    Small wrapper for creating combinded borders

  Used Aliaces
     :b - :bottom
     :t - :top
     :l - :left
     :r - :right
     :h - :horizontal(:left+:right)
     :v - :vertical(:top+:bottom)
     :a - all directions

  Example
    (border
      {:b 2 :t 2 :l 2 :r 10 :color nil}
      {:a 2 :color \"#931\"}
      {:type :stroke :thickness 1.0 :color \"#00F\" :pattern #{:dot-line :stroke-line :wide-stroke-line} :thickness 1.0}
      {:type :title :title \"SHUK\" :color \"#222\" :title-color \"#222\"
       :title-position #{:bottom :top :default :above-bottom :below-top :below-bottom :above-top}
       :title-justification #{:center :default :left :right :leading :trailing}
       :title-font (jarman.gui.components.swing-common/font face/f-regular face/s-foreground)
       :title-color (seesaw.color/color face/c-foreground)})" [& v]
  (if (= 1 (count v)) (border-from-config (first v))
      (apply seesaw.border/compound-border (mapv border-from-config v))))

(comment
  (jarman.gui.components.swing/quick-frame
    [(jarman.gui.gui-components2/border-panel
       :north (jarman.gui.gui-components2/label :value "SSSSSSSSSSSSSSSS"
                :border (border
                          {:a 10}
                          {:type :stroke :thickness 1.0 :color "#00F"}
                          {:a 10}))
       :center (jarman.gui.gui-components2/label :value "SSSSSSSSSSSSSSSS"
                 :border (border
                           {:a 4}
                           {:type :stroke :thickness 1.0 :color "#00F"}
                           {:a 5}))
       :south (jarman.gui.gui-components2/label :value "SSSSSSSSSSSSSSSS"
                :border (border
                          {:type :title :title "SHUK" :color "#222" :title-color "#222"}
                          {:a 4}))
       :border
       (border
         {:a 10}
         ;; {:type :title :title "Panel" :color "#222" :title-color "#222"}
         {:type :stroke :pattern :wide-stroke-line}
         {:a 10}))]))

