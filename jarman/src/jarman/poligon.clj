(ns jarman.poligon
  (:use seesaw.core
        seesaw.border
        seesaw.dev
        seesaw.mig
        ;; jarman.ftoolth
        jarman.dev-tools
        jarman.alerts-service)
  (:require [jarman.icon-library :as icon]))


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
;; (defn message
;;   [mess]
;;   (let [prepertext mess]
;;    (label :text (format "<html>
;;                         <style>
;;                           p{font-size: 1.05em; 
;;                          font-family: \"Monospaced\", serif; 
;;                          color: #fff; 
;;                          text-align: right; 
;;                          background-color: #292929; 
;;                          width: 190; 
;;                          padding-left: 10px; 
;;                          padding-right: 10px;
;;                          padding-top: 5px;
;;                          padding-bottom: 5px;
;;                          }
;;                         </style>
;;                         <body>
;;                           <p>%s
;;                         </body>
;;                         </html>" prepertext)
;;           :border (empty-border :bottom 2)
;;           )))


(defn message
  [& args]
  (let [font-c "#000"
        back-c "#fff"
        header (fn [txt] (format
                          "<html>
                        <style>
                          p{font-size: 1.15em; 
                            font-family: \"Monospaced\", serif; 
                         }
                        </style>
                        <body>
                          <p>%s</p>
                        </body>
                        </html>" txt))
        body   (fn [txt] (format
                          "<html>
                        <style>
                          p{font-size: 1.05em; 
                            font-family: \"Monospaced\", serif; 
                           padding-left: 20px;
                         }
                        </style>
                        <body>
                          <p>%s</p>
                        </body>
                        </html>" txt))]
    (vertical-panel
     :size [366 :by 96]
     :background (new Color 0 0 0 0)
     :border (compound-border (line-border :thickness 2 :color "#66dbe3") (empty-border :thickness 5))
     :items [(label :text (header (if (> (count args) 1) (first args) "Powiadomienie"))
                    :icon (image-scale icon/agree-64-png 30)
                    :foreground font-c
                    :background back-c
                    :border (empty-border :thickness 5) )
             (label :text (body (if (> (count args) 1) (second args) (first args)))
                    :foreground font-c
                    :background back-c
                    :border (empty-border :thickness 5)
                    :listen [:mouse-entered (fn [e]
                                              (config! e
                                                      ;;  :background "#a3d487"
                                                      ;;  :foreground "#000"
                                                       :cursor :hand))
                             :mouse-exited (fn [e]
                                             (config! e
                                                      ;; :background "#363636"
                                                      ;; :foreground "#fff"
                                                      ))])])))


;; (show-options (horizontal-panel))

(def alerts-panel (vertical-panel :items [(label)]
                                  :bounds [0 0 300 0]
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
           :listen [:component-resized (fn [e] (alert-panel-resize alerts-panel))])
      (.setLocationRelativeTo nil) pack! show!))

(.getWidth (.preferredSize (first (seesaw.util/children alerts-panel))))

(config alerts-panel :size)

(alerts-s :set (message "Informacja o aktualizacji" "Dostępna nowa aktualizacja, możesz ją pobrać teraz lub później!") 0)
;; (alerts-s :set (update-alert "Dostępna nowa aktualizacja" "Dostępna nowa aktualizacja, jeśli masz ochotę zrobić ją teraz to ok.") 0)
;; (alerts-s :set (update-alert "Dostępna nowa aktualizacja, jeśli masz ochotę zrobić ją teraz to ok." "Aktualizuj<br>teraz") 0)
;; (alerts-s :set (message "bbbb") 0)
;; (alerts-s :count)
;; (alerts-s :rmall)

;; (do (alerts-s :rmall)
;;     (alerts-s :set (message "No more message") 3)
;;     (update-messages-panel-size))

;; (show-options (text))
