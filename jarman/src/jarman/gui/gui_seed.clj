(ns jarman.gui.gui-seed
  (:use seesaw.core
        seesaw.border
        seesaw.dev
        seesaw.mig)
  (:require [clojure.string :as string]
            ;; resource 
            [jarman.resource-lib.icon-library :as icon]
            ;; logics
            [jarman.config.config-manager :refer :all]
            [jarman.gui.gui-tools :refer :all]
            [jarman.gui.gui-alerts-service :refer :all]
            [jarman.config.init :as sinit]
            ;; deverloper tools 
            [jarman.tools.swing :as stool]
            [jarman.tools.lang :refer :all]
            ;; TEMPORARY!!!! MUST BE REPLACED BY CONFIG_MANAGER
            [jarman.config.init :refer [configuration language]]))

;;  (get-color :jarman :bar)

(import javax.swing.JLayeredPane)
;; (import javax.swing.JLabel)
;; (import java.awt.Color)
;; (import java.awt.Dimension)
;; (import java.awt.event.MouseEvent)

(def app-size [1100 800])
(def atom-app-size (atom [1100 800]))
(def app (atom nil))
(def alert-manager (atom nil))

(add-watch
 atom-app-size
 :refresh
 (fn [key atom old-state new-state]
   (do
     (config! (select @app [:#rebound-layer]) :bounds [0 0 (first @atom-app-size) (second @atom-app-size)])
     (.repaint @app))))

(def box 
  (fn [& {:keys [wrap items vlayout hlayout]
          :or  {wrap 0 
                items (list (label :text "Hello Boi!"))
                vlayout "center, grow"
                hlayout "center, grow"}}] 
    (let [wraper (if (= wrap 0) "" (string/join "" ["wrap" wrap]))
          margin 0]
      (vertical-panel
       :id :rebound-layer
       :items [(mig-panel
                ;; :background "#a23"
                ;; :size [(first @atom-app-size) :by (second @atom-app-size)]
                :constraints [wraper
                              (str margin "px[:" (first app-size) "," hlayout "]" margin "px")
                              (str margin "px[:" (second app-size) "," vlayout "]" margin "px")]
                :items (join-mig-items items))]))))


(def base
  "Description:
       Create panel for absolute position elements.
   "
  (fn [items]
    (let [JLP (new JLayeredPane)
          layer (atom 0)]
      (do
        (doseq [i items] (do 
                           (swap! layer inc)
                           ;;(println "Layer" @layer)
                           (.add JLP i (new Integer @layer))))
    ;;   (.setBackground JLP (new Color 0 0 0))
    ;;   (.setOpaque JLP true)
        JLP))))



(def build
  (fn [& {:keys [items]
          :or  {items (label :text "Hello Boi!" :bounds [100 100 300 300])}}]
    (let [set-items (if-not (list? items) (list items) items)]
      (do
        (reset! app (base set-items))
        (reset! atom-app-size app-size)
        (reset! alert-manager (message-server-creator app))
        (-> (doto (seesaw.core/frame
                   :title "Mr. Jarman" :undecorated? false
                   :minimum-size [(first app-size) :by (second app-size)]
                   :size [(first app-size) :by (first app-size)]
                   :content @app
                ;;    :on-close :exit
                   :listen [:component-resized (fn [e] (reset! atom-app-size [(.getWidth (config e :size))
                                                                         (.getHeight (config e :size))]))])
              (.setLocationRelativeTo nil) pack! show!)))
      )))


;; (def comps (list (label :text "Mig panel as layer 1")
;;                  (label :text "With few responsive elements")
;;                  (label :text "Secret" :listen [:mouse-clicked (fn [e] (alert-manager :set {:header "Secret" :body "Secreat was found!"} (message alert-manager) 3))])
;;                  (label :text "Oh yeah booiii!")))

;; (build :items (list (box :items comps :wrap 2)
;;                     (label :text "Top layer 2" :halign :center :bounds [10 10 100 50] :background "#a44")
;;                     (label :text "Top layer 3" :halign :center :bounds [80 40 100 50] :background "#abc")))

