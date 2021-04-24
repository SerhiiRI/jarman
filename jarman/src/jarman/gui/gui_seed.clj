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

(def atom-app-size (atom [1200 700]))
(def app (atom nil))
(def alert-manager (atom nil))

(add-watch
 atom-app-size
 :refresh
 (fn [key atom old-state new-state]
   (do
     (config! (select @app [:#rebound-layer]) :bounds [0 0 (first @atom-app-size) (second @atom-app-size)])
     (.repaint (to-frame @app)))))

;; (def box
;;   (fn [& {:keys [wrap items vlayout hlayout]
;;           :or  {wrap 0
;;                 items (list (label :text "Hello Boi!"))
;;                 vlayout "center, grow"
;;                 hlayout "center, grow"}}]
;;     (let [wraper (if (= wrap 0) "" (string/join "" ["wrap" wrap]))
;;           margin 0]
;;       (vertical-panel
;;        :id :rebound-layer
;;        :items [(mig-panel
;;                 ;; :background "#a23"
;;                 ;; :size [(first @atom-app-size) :by (second @atom-app-size)]
;;                 :constraints [wraper
;;                               (str margin "px[:" (first @atom-app-size) "," hlayout "]" margin "px")
;;                               (str margin "px[:" (second @atom-app-size) "," vlayout "]" margin "px")]
;;                 :items (join-mig-items items))]))))


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
                           (.add JLP i (new Integer @layer))))
        JLP))))



(def build
  (fn [& {:keys [title
                 items
                 size
                 undecorated?]
          :or  {title "Mr. Jarman"
                items (label :text "Hello Boi!" :bounds [100 100 300 300])
                size [(first @atom-app-size) (second @atom-app-size)]
                undecorated? false}}]
    (let [set-items (if-not (list? items) (list items) items)]
      (do
        (reset! app (base set-items))
        (reset! alert-manager (message-server-creator app))
        (-> (doto (seesaw.core/frame
                   :title title
                   :resizable? true
                   :undecorated? undecorated?
                   :size [(first size) :by (second size)]
                   :minimum-size [600 :by 400]
                   :content @app
                ;;    :on-close :exit
                   :listen [:component-resized (fn [e]
                                                ;;  (println e)
                                                 (let [w (.getWidth  (config e :size))
                                                       h (.getHeight (config e :size))]
                                                   (reset! atom-app-size [w h]))
                                                 (.revalidate (to-widget e)))])
              (.setLocationRelativeTo nil) pack! show!))
        (config! (to-frame @app) :size [(first size) :by (second size)])))))


;; (build :items (list (label :text "Mig panel as layer 1")))
 
;; (show-options (frame))

;; (def comps (list (label :text "Mig panel as layer 1")
;;                  (label :text "With few responsive elements")
;;                  (label :text "Secret" :listen [:mouse-clicked (fn [e] (alert-manager :set {:header "Secret" :body "Secreat was found!"} (message alert-manager) 3))])
;;                  (label :text "Oh yeah booiii!")))

;; (build :items (list (box :items comps :wrap 2)
;;                     (label :text "Top layer 2" :halign :center :bounds [10 10 100 50] :background "#a44")
;;                     (label :text "Top layer 3" :halign :center :bounds [80 40 100 50] :background "#abc")))


;; (let [my-frame (-> (doto (seesaw.core/frame
;;                           :title "test"
;;                           :size [0 :by 0]
;;                           :content (label))
;;                      (.setLocationRelativeTo nil) pack! show!))]
;;   (config! my-frame :size [600 :by 600]))
