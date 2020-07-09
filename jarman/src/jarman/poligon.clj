(ns jarman.poligon
  (:use seesaw.core
        seesaw.border
        seesaw.dev
        seesaw.mig
        ;; jarman.ftoolth
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


(defn writer [] (text :text ""
                      :bounds [0 0 200 30]
                      :listen [:action (fn [x] (do
                                                 (alerts-s :set (message (config x :text)) 3)
                                                 (config! x :text "")))]))


(defn app []
  "Description:
       Create panel for absolute position elements and add components
   "
  (doto (new JLayeredPane)
    (.add (writer) (new Integer 1))
    (.add alerts-panel (new Integer 10))))


;; (do (alerts-s :rmall)
;;     (alerts-s :set (message "No more message") 3))


(-> (doto (seesaw.core/frame
           :title "DEBUG WINDOW" :undecorated? false
           :minimum-size [600 :by 400]
           :content (app)
           :listen [:component-resized (fn [e] (update-messages-panel-size))])
      (.setLocationRelativeTo nil) pack! show!))

;; (alerts-s :set (message "aaaa") 0)
;; (alerts-s :set (message "bbbb") 0)
;; (alerts-s :count)
;; (alerts-s :rmall)

;; (do (alerts-s :rmall)
;;     (alerts-s :set (message "No more message") 3)
;;     (update-messages-panel-size))

;; (show-options (text))
