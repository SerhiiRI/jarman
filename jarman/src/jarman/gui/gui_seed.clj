(ns jarman.gui.gui-seed
  (:use seesaw.core
        seesaw.border
        seesaw.dev
        seesaw.mig)
  (:require [clojure.string :as string]
            ;; resource 
            [jarman.resource-lib.icon-library :as icon]
            ;; logics
            [jarman.gui.gui-tools :as gtool]
            [jarman.gui.gui-alerts-service :as gas]
            ;; deverloper tools 
            [jarman.tools.swing :as stool]
            [jarman.logic.state :as state]
            [jarman.tools.lang :refer :all]
            [jarman.logic.changes-service :as cs]
            ;; TEMPORARY!!!! MUST BE REPLACED BY CONFIG_MANAGER
            ))

;;  (get-color :jarman :bar)

(def changes-service (atom (cs/new-changes-service)))

(import javax.swing.JLayeredPane)
;; (import javax.swing.JLabel)
;; (import java.awt.Color)
;; (import java.awt.Dimension)
;; (import java.awt.event.MouseEvent)

(state/set-state :atom-app-size (atom [1200 700]))
(state/set-state :app nil)

(add-watch
 (state/state :atom-app-size)
 :refresh
 (fn [key atom old-state new-state]
   (do
     (config! (select (state/state :app) [:#rebound-layer]) :bounds [0 0 (first new-state) (second new-state)])
     (.repaint (to-frame (state/state :app))))))


(def base
  "Description:
       Create panel for absolute position elements.
   "
  (fn [items]
    (let [JLP (new JLayeredPane)
          layer (atom 10)]
      (do
        (doseq [i items] (let [itm (if (vector? i) (first i) i)
                               idx (if (vector? i) (second i) (+ 1 @layer))]
                           (swap! layer inc)
                           (.add JLP itm (new Integer idx))))
        JLP))))



(def build
  (fn [& {:keys [title
                 items
                 size
                 undecorated?]
          :or  {title "Mr. Jarman"
                items (label :text "Hello Boi!" :bounds [100 100 300 300])
                size @(state/state :atom-app-size)
                undecorated? false}}]
    (let [set-items (if-not (list? items) (list items) items)]
      (do
        (state/set-state :app (base set-items))
        (state/set-state :alert-manager (gas/message-server-creator (state/state :app)))
        (let [jframe (seesaw.core/frame
                      :title title
                      :resizable? true
                      :undecorated? undecorated?
                      :size [(first size) :by (second size)]
                      :minimum-size [600 :by 400]
                      :content (state/state :app)
                ;;    :on-close :exit
                      :listen [:component-resized (fn [e]
                                                ;;  (println e)
                                                    (let [w (.getWidth (.getSize (.getContentPane (to-root e))))
                                                          h (.getHeight (.getSize (.getContentPane (to-root e))))]
                                                      (reset! (state/state :atom-app-size) [w h]))
                                                    (.revalidate (to-widget e)))])]
          (-> (doto jframe (.setLocationRelativeTo nil) pack! show!))
          (config! jframe  :icon (stool/image-scale icon/calendar1-64-png)
                   :size [(first size) :by (second size)]))))))


(defn extend-frame-title 
  [title]
  (config! (to-frame (state/state :app)) :title (str "Mr. Jarman" title)))

;; (@jarman.gui.gui-app/startup)

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
