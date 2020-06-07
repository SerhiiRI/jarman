(ns jarman.poligon
  (:use seesaw.core
        seesaw.border
        seesaw.dev
        seesaw.mig
        jarman.ftoolth
        jarman.alerts-service))


(import javax.swing.JInternalFrame)
(import javax.swing.BorderFactory)
(import javax.swing.JDesktopPane)
(import javax.swing.JLayeredPane)
(import javax.swing.JButton)
(import javax.swing.JFrame)
(import javax.swing.JLabel)
(import java.awt.Dimension)
(import java.awt.Component)
(import java.awt.Point)
(import java.awt.Color)

(defn countHeight
  "Description:
       Function get list of children and sums heights
   Example: 
       (countHeight list-of-children) => 42.0
   "
  [children]
  (cond 
    (> (count children) 0) 
    (+ (+ (reduce (fn [acc chil] (+ acc (.getHeight (.getSize chil)))) 34 children)))
    :else 0))

;; To correct working with 200px width in one line should be 23 characters and then tag <br>
(defn message
  [mess]
  (let [prepertext mess]
   (label :text (format "<html>
                        <style>
                          p{font-size: 1.05em; 
                         font-family: \"Monospaced\", serif; 
                         color: #fff; 
                         text-align: right; 
                         background-color: #292929; 
                         width: 190; 
                         padding-left: 10px; 
                         padding-right: 10px;
                         padding-top: 5px;
                         padding-bottom: 5px;
                         }
                        </style>
                        <body>
                          <p>%s
                        </body>
                        </html>" prepertext)
          :border (empty-border :bottom 2)
          )))


(def alerts-panel (vertical-panel :items [(label)]
                                  :bounds [0 0 200 200]
                                  :background (new Color 0 0 0 0)))

(def alerts-s (message-server-creator alerts-panel))

(def writer (text :text ""
                  :bounds [0 0 200 30]))

(def update-messages-panel-size
  (fn [] (let [width-window  (.getWidth   (.getSize (to-root alerts-panel)))
               height-window (.getHeight  (.getSize (to-root alerts-panel)))
               height-panel  (countHeight (seesaw.util/children alerts-panel))]
          (config! alerts-panel :bounds [(- width-window 200) (- height-window height-panel)
                                         200 height-panel]))))

(defn update-messages-panel
  [] (let [old (config alerts-panel :items)] (do
                                               (config! writer :text "")
                                               (update-messages-panel-size)
                                               )))

(def test-btn (button :text "Wyślij"
                      :listen [:mouse-clicked (fn [e] (do 
                                                        (alerts-s :set (message (config writer :text)) 3) 
                                                        (update-messages-panel)))
                               :mouse-moved (fn [e] (update-messages-panel-size))
                               ]
                      :bounds [0 31 200 50]))

(defn app []
  "Description:
       Create panel for absolute position elements and add components
   "
  (doto (new JLayeredPane)
    (.add writer (new Integer 1))
    (.add test-btn (new Integer 1))
    (.add alerts-panel (new Integer 10))))

;; (app 0)

(do (alerts-s :rmall)
    (alerts-s :set (message "No more message") 3))


(-> (doto (seesaw.core/frame
           :title "DEBUG WINDOW" :undecorated? false
           :minimum-size [600 :by 400]
           :content (app)
           :listen [:component-resized (fn [e] (update-messages-panel-size))])
      (.setLocationRelativeTo nil) pack! show!))

(do (alerts-s :rmall)
    (alerts-s :set (message "No more message") 3)
    (update-messages-panel-size))

;; (show-options (text))
