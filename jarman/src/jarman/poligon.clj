(ns jarman.poligon
  (:use seesaw.core
        seesaw.border
        seesaw.dev
        seesaw.mig
        ;; jarman.ftoolth
        jarman.dev-tools
        jarman.alerts-service)
  (:require [jarman.icon-library :as icon]
            [clojure.string :as string]))


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

(def hand-hover-on  (fn [e] (config! e :cursor :hand)))
(def hand-hover-off (fn [e] (config! e :cursor :hand)))

(def func-ico-f (fn [ic] (label :icon (image-scale ic 28)
                                :background (new Color 0 0 0 0)
                                :border (empty-border :left 3 :right 3)
                                :listen [:mouse-entered hand-hover-on])))

(def alert-ico-f (fn [ic] (label :icon (image-scale ic 28)
                                 :background (new Color 0 0 0 0)
                                 :border (empty-border :left 3 :right 3))))

(def func-header-f (fn [txt] (label :text txt
                                    :font {:size 14 :style :bold}
                                    :background (new Color 0 0 0 0))))

(def func-body-f (fn [txt] (label :text txt
                                  :font {:size 13}
                                  :background (new Color 0 0 0 0)
                                  :border (empty-border :left 5 :right 5 :bottom 2))))



(defn message
  [& args]
  (let [font-c "#000"
        back-c "#fff"]
    (mig-panel
     :id :alert-box
     :constraints ["wrap 1" "0px[fill, grow]0px" "0px[20]0px[30]0px[20]0px"]
     :background back-c
     :border nil
     :bounds [680 480 300 75]
     :items [[(flow-panel
               :align :left
               :background (new Color 0 0 0 0)
               :items [(alert-ico-f icon/alert-64-png)
                       (func-header-f "Powiadomienie")])]
             [(func-body-f "Tekst powiadomienia ...")]
             [(flow-panel
               :align :right
               :background (new Color 0 0 0 1)
               :items [(func-ico-f icon/agree-64-png)
                       (func-ico-f icon/settings-64x64-png)])]]
    ;;  :items [(label
    ;;           :text (header (if (> (count args) 1) (first args) "Powiadomienie"))
    ;;           :icon (image-scale icon/agree-64-png 30)
    ;;           :border nil
    ;;           :foreground font-c
    ;;           :background back-c)
    ;;          (label
    ;;           :text (body (if (> (count args) 1) (second args) (first args)))`
    ;;           :border nil
    ;;           :foreground font-c
    ;;           :background back-c
    ;;           :listen [:mouse-entered (fn [e]
    ;;                                     (config! e :cursor :hand))
    ;;                    :mouse-exited (fn [e]
    ;;                                    (config! e :cursor :pointer))])
    ;;          (horizontal-panel
    ;;           :items [(label
    ;;                    :icon (image-scale icon/agree-64-png 30))
    ;;                   (label
    ;;                    :icon (image-scale icon/agree-64-png 30))
    ;;                   (label
    ;;                    :icon (image-scale icon/agree-64-png 30))])]
)))


;; (show-options (horizontal-panel))

;; (def alerts-panel (vertical-panel :items [(label)]
;;                                   :bounds [0 0 300 0]
;;                                   :background (new Color 0 0 0 0)))

;; (def alerts-s (message-server-creator alerts-panel))


;; (defn writer [] (text :text ""
;;                       :bounds [0 0 200 30]
;;                       :listen [:action (fn [x] (do
;;                                                  (alerts-s :set (message (config x :text)) 3)
;;                                                  (config! x :text "")))]))


(def btn-icon-f (fn [ico order size txt]
                  (let [bg-color "#ddd"
                        ;; color-onenter "#29295e"
                        color-hover-margin "#bbb"
                        bg-color-hover "#d9ecff"
                        size size
                        y (if (> (* size order) 0) (+ 2 (* size order)) (* size order))]
                    (label
                     :halign :center
                     :icon ico
                     :bounds [0 y size size]
                     :background bg-color
                     :border (line-border :left 4 :color bg-color)
                     :listen [:mouse-entered (fn [e] (config! e
                                                              :cursor :hand
                                                              :border (line-border :right 4 :color color-hover-margin)
                                                              :background bg-color-hover
                                                              :bounds [0 y (+ 200 size 8) size]
                                                              :text txt))
                              :mouse-exited  (fn [e] (config! e
                                                              :bounds [0 y size size]
                                                              :border (line-border :left 4 :color bg-color)
                                                              :background bg-color
                                                              :text ""
                                                              :cursor :default))]))))

(def users-functions-f (fn [] (mig-panel
                               :constraints ["wrap 1" "0px[300]0px" "0px[fill]0px"]
                               :background (new Color 0 0 0 0)
                               :items [[(label :text "Opcja 1")]
                                       [(label :text "Opcja 2")]])))

(def btn-summer-f (fn [txt & funcs]
                    (let [bg-color "#eee"
                          margin-color "#fff"
                          border (compound-border (line-border :left 6 :color bg-color) (line-border :bottom 2 :color margin-color))
                          vsize 35
                          hsize 200
                          ico (if (> (count funcs) 0) (image-scale icon/plus-64-png 25))
                          ico-hover (image-scale icon/minus-grey-64-png 20)]
                      (mig-panel
                       :constraints ["wrap 1" "0px[fill]0px" "0px[fill]0px"]
                       :listen [:mouse-entered (fn [e] (config! e :cursor :hand))
                                :mouse-entered (fn [e] (config! e :cursor :default))
                                :mouse-clicked (fn [e]
                                                 (if (> (count funcs) 0)
                                                   (if (== (count (seesaw.util/children (seesaw.core/to-widget e))) 1)
                                                     (do
                                                       (config! e :items (vec (map (fn [i] (vec (list i))) (concat (vec (seesaw.util/children (seesaw.core/to-widget e))) (vec funcs)))))
                                                       (config! (last (seesaw.util/children (first (seesaw.util/children (seesaw.core/to-widget e))))) :icon ico-hover))
                                                     (do
                                                       (config! e :items [(vec (list (first (seesaw.util/children (seesaw.core/to-widget e)))))])
                                                       (config! (last (seesaw.util/children (first (seesaw.util/children (seesaw.core/to-widget e))))) :icon ico)))))]
                       :items [[(mig-panel
                                 :constraints ["" "0px[fill]0px" "0px[fill]0px"]
                                 :background (new Color 0 0 0 0)
                                 :items [[(label
                                           :text txt
                                           :size [(- hsize vsize) :by vsize]
                                           :background bg-color
                                           :border border)]
                                         [(label
                                           :size [vsize :by vsize]
                                           :halign :center
                                           :background bg-color
                                           :border border
                                           :icon ico)]])]]))))



(def btn-tab-f (fn [txt active]
                 (let [bg-color (if (== active 1) "#eee" "#ccc")
                       border "#fff"
                       hsize 70
                       vsize 30]
                   (horizontal-panel
                    :background bg-color
                    :items [(label
                             :text txt
                             :halign :center
                             :size [hsize :by vsize])
                            (label
                             :icon (image-scale icon/x-grey2-64-png 15)
                             :halign :center
                             :border (line-border :right 2 :color border)
                             :size [vsize :by vsize]
                             :listen [:mouse-entered (fn [e] (config! e :cursor :hand :icon (image-scale icon/x-blue1-64-png 15)))
                                      :mouse-exited  (fn [e] (config! e :cursor :default :icon (image-scale icon/x-grey2-64-png 15)))])]
                    :listen [:mouse-entered (fn [e] (config! e :cursor :hand))
                             :mouse-exited  (fn [e] (config! e :cursor :default))]))))


(def mig-app-left-f
  "elements by elements -> [(label)] [(label)] [(label)]"
  (fn [& args] (mig-panel
                :background "#fff"
                :border (line-border :left 4 :right 4 :color "#fff")
                :constraints ["wrap 1" "0px[fill, grow]0px" "0px[]0px"]
                :items (vec args))))

(def mig-app-right-f
  "Description: 
      Space for table and tabs on right part of app
      tabs  -> mig vector with elements    -> [(tab1)(tab2)(tab3)]
      array -> table like rows and columns -> [(table)]
   Example: (mig-menu-lvl-3-f [(tab1) (tab2) (tab3)] [(table)])"
  (fn [tabs array] (let [bg-color "#fff"]
                     (mig-panel
                      :background "#fff"
                      :constraints ["wrap 1" "0px[fill, grow]0px" "0px[]0px"]
                      :background "#eee"
                      :items [[(horizontal-panel
                                :background bg-color
                                :items tabs)]
                              [(vertical-panel
                                :background (new Color 0 0 0 0)
                                :items array)]]))))

(defn get-elements-in-layered-by-id 
  [e ids] (let [id (keyword ids)
               select-id (keyword (string/join["#" ids]))] 
           (filter (fn [i] (identical? (config i :id) id)) (seesaw.util/children (.getParent (select (to-root e) [select-id]))))))

(def jarmanapp (grid-panel
                :bounds [0 0 300 300]
                :items [(mig-panel
                         :constraints [""
                                       "0px[50, fill]0px[200, fill]0px[fill, grow]0px"
                                       "0px[fill, grow]0px"]
                         :items [[(label :background "#eee")]
                                 [(mig-app-left-f  [(btn-summer-f "Ukryte opcje 1" 
                                                                  (label :text "Opcja 1" :background "#fff" :size [200 :by 25]
                                                                         :listen [:mouse-clicked (fn [e] (println "Test Option 1 click"))]
                                                                         )
                                                                  (label :text "Opcja 2" :background "#fff" :size [200 :by 25]
                                                                         :listen [:mouse-clicked (fn [e]
                                                                                                  (let [list-of-alerts (get-elements-in-layered-by-id e "alert-box")]
                                                                                                    (do 
                                                                                                      (println "Recolorize")
                                                                                                      (vec (map (fn [i] (config! i :background "#000")) list-of-alerts))
                                                                                                      )) 
                                                                                                   )]
                                                                         ))]
                                                    [(btn-summer-f "Ukryte opcje 2")])]
                                 [(mig-app-right-f [(btn-tab-f "Tab 1" 1) (btn-tab-f "Tab 2" 0)]
                                                   [(label :text "GRID")])]])]))


; (show-options (grid-panel))


(defn app []
  "Description:
       Create panel for absolute position elements and add components
   "
  (let [menu-icon-size 50]
    (doto (new JLayeredPane)
      (.add jarmanapp (new Integer 5))
      ; (.add alerts-panel (new Integer 10)) Alerts messages
      (.add (btn-icon-f (image-scale icon/user-64x64-2-png menu-icon-size) 0 menu-icon-size "Klienci") (new Integer 10))
      (.add (btn-icon-f (image-scale icon/settings-64x64-png menu-icon-size) 1 menu-icon-size "Konfiguracja") (new Integer 10))
      (.add (message "") (new Integer 15))
      )))


;; (do (alerts-s :rmall)
;;     (alerts-s :set (message "No more message") 3))

(-> (doto (seesaw.core/frame
           :title "DEBUG WINDOW" :undecorated? false
           :minimum-size [1000 :by 600]
           :content (app)
           :listen [:component-resized (fn [e] 
                                        ;  (template-resize alerts-panel jarmanapp)
                                         (do (template-resize jarmanapp)
                                             )
                                         )]
           )
      (.setLocationRelativeTo nil) pack! show!))





; (.getWidth (.preferredSize (first (seesaw.util/children alerts-panel))))

; (config alerts-panel :size)

;; (alerts-s :set (message "Informacja o aktualizacji" "Dostępna nowa aktualizacja, możesz ją pobrać teraz lub później!") 0)
;; (alerts-s :set (update-alert "Dostępna nowa aktualizacja" "Dostępna nowa aktualizacja, jeśli masz ochotę zrobić ją teraz to ok.") 0)
;; (alerts-s :set (update-alert "Dostępna nowa aktualizacja, jeśli masz ochotę zrobić ją teraz to ok." "Aktualizuj<br>teraz") 0)
;; (alerts-s :set (message "bbbb") 0)
;; (alerts-s :count)
;; (alerts-s :rmall)

;; (do (alerts-s :rmall)
;;     (alerts-s :set (message "No more message") 3)
;;     (update-messages-panel-size))

; (show-options (horizontal-panel))

