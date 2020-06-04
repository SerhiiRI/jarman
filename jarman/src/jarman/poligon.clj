(ns jarman.poligon
  (:use seesaw.core
   seesaw.border
seesaw.mig))

  ( + 2 2)
  
  
(defn builder-panel
  "Description:
       Quick panel builder for components builders, builded components and simple text
       If map is first then create new panel with merge styles
   Example: 
       (def vpanel (builder-panel seesaw.core/vertical-panel {})) -> Create vertical panel
       (def vpanel-dark (vpanel {:background \"333\"})) -> Create vertical panel used another and add new state to old params
   "
  [panel state]
  (fn [& elements]
    (if (map? (first elements))
      (builder-panel panel (merge state (first elements)))
      (let [confmap state
            elements-list (if (map? (last elements))
                            (butlast elements)
                            elements)
            conf (if (map? (last elements))
                   (merge confmap (last elements))
                   confmap)]
        (apply panel
               :items (map (fn [item]
                             (cond
                               (string? item) (label :text item)
                               (fn? item)     (item)
                               :else          item))
                           elements-list)
               (vec (mapcat seq conf)))))))

(defn component
  "Description:
       Builder for builde builders uses builded builder befor
   Example: 
       (def builder-btn (partial component seesaw.core/button)) -> Create button builder
       (def btn (builder-btn {:text \"\"})) -> Create button builder with state
       (def btn (builder-btn {:text \"Test\"})) -> Create button builder with used another and add new state to old params
   "
  ([kind state]
   (let [state state]
     (fn ([] (apply kind (vec (mapcat seq state))))
       ([new-state]
        (if (= state new-state)
          (apply kind (vec (mapcat seq state)))
          (component kind (merge state new-state))))))))


(defn mig
  "Description:
      Simple mig container builder.
      (mig component1 component2-f \"some-text\" {wrap})
   Example: 
      (mig btn)
      (mig btn (btn2 {:foreground \"#c3a\"}))
      (mig btn (btn2 {:foreground \"#c3a\"})  \"Dupa\")
      (mig btn (btn2 {:foreground \"#c3a\"})  \"Dupa\" {:wrap \"wrap 1\"})"
  [& elements]
  (fn [] (let [confmap {:wrap "" :h "0px[grow, fill]0px" :v "0px[grow, fill]0px" :args []}
               elements-list (if (map? (last elements))
                               (butlast elements)
                               elements)
               conf (if (map? (last elements))
                      (merge confmap (last elements))
                      confmap)]
           (apply seesaw.mig/mig-panel
                  :constraints [(get conf :wrap)
                                (get conf :h)
                                (get conf :v)]
                  :items (map (fn [item]
                                (cond
                                  (string? item) [(label :text item)]
                                  (fn? item)     [(item)]
                                  :else          [item]))
                              elements-list)
                  (get conf :args)))))


(def flow (builder-panel seesaw.core/flow-panel {}))
(def vpanel (builder-panel seesaw.core/vertical-panel {}))
(def hpanel (builder-panel seesaw.core/horizontal-panel {}))
  
  (import javax.swing.JDesktopPane)
  (import javax.swing.JInternalFrame)
  (import javax.swing.JFrame)
  (import javax.swing.JButton)
  (import javax.swing.JLabel)
  (import java.awt.Color)

  (def IFrame
    (let [JIF (doto
               (new JInternalFrame)
                (.setVisible true)
                (.setSize 200 100))]
      (doto (new JDesktopPane)
        (.add JIF))))

(import javax.swing.JLayeredPane)
(import javax.swing.BorderFactory)
(import java.awt.Dimension)

(def JLP (fn [] (doto (new JLayeredPane)
        ;    (.setBackground java.awt.Color/BLACK)
        ;    (.setBorder (javax.swing.BorderFactory/createTitledBorder "TEXT"))
           )))
  
(def l1 (fn [](label :text "Komunikat 1" :background "#4cc" 
                     :bounds [0 260 180 50]
                     )))
  
(def l2 (fn [](label :text "Komunikat 2" :background "#2ac" 
                     :bounds [0 305 200 50]
                     )))
  
(def app (fn [](hpanel (label :text "I co? Działa to to?" :background "#666" :foreground "#000" :border (empty-border :thickness 10)) 
                       (label :text "Halo hans" :background "#ccc" :foreground "#000" :border (empty-border :thickness 10))
                       (button :text "Halo hans" :background "#999" :foreground "#000" :border (empty-border :thickness 10))
                       {:background "#999" 
                        :bounds [0 0 300 280]
                        })))
  
(-> (doto (seesaw.core/frame
           :title "DEBUG WINDOW" :undecorated? false
           :minimum-size [600 :by 400]
           :content  (doto (JLP) 
                       (.add (app) (new Integer 1))
                       (.add (l1) (new Integer 2))
                       (.add (l2) (new Integer 4))))
           (.setLocationRelativeTo nil) pack! show!))

