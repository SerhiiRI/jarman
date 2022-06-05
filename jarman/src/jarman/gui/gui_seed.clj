(ns jarman.gui.gui-seed
  (:use seesaw.core
        seesaw.border
        seesaw.dev
        seesaw.mig)
  (:require [clojure.string :as string]
            ;; resource 
            [jarman.resource-lib.icon-library :as icon]
            [jarman.gui.gui-style :as gs]
            [jarman.faces         :as face]
            ;; logics
            [jarman.gui.gui-tools :as gtool]
            [jarman.gui.gui-alerts-service :as gas]
            ;; deverloper tools 
            [jarman.tools.swing :as stool]
            [jarman.logic.state :as state]
            [jarman.lang :refer :all]
            [jarman.logic.changes-service :as cs]
            [jarman.gui.popup :as popup]
            ;; TEMPORARY!!!! MUST BE REPLACED BY CONFIG_MANAGER
            [jarman.gui.components.swing-keyboards :refer [kbd global-set-key wrapp-keymap]])
  (:import javax.swing.JLayeredPane
           (jiconfont.icons.google_material_design_icons GoogleMaterialDesignIcons)
           (java.awt.event WindowAdapter)
           (java.awt.event WindowEvent)))

(state/set-state :atom-app-size (atom [1200 700]))
(state/set-state :atom-app-resize-direction (atom [1 1]))
(state/set-state :app nil)

(add-watch
 (state/state :atom-app-size)
 :refresh
 (fn [key atom old-state new-state]
   (do
     (config! (select (state/state :app) [:#rebound-layer]) :bounds [0 0 (first new-state) (second new-state)])
     (.repaint (to-frame (state/state :app))))))

(def build-main-panel
  "Description:
       Create panel for absolute position elements.  "
  (fn [items]
    (let [JLP (new JLayeredPane)
          layer (atom 10)]
      (wrapp-keymap JLP)
      (do
        (doseq [i items] (let [itm (if (vector? i) (first i) i)
                               idx (if (vector? i) (second i) (+ 1 @layer))]
                           (swap! layer inc)
                           (.add JLP itm (new Integer idx))))
        JLP))))

(defn OverridedWindowListener
  "Description:
     Actions on window events."
  [& {:keys [opened closing closed activated deactivated iconified deiconified]
      :or {opened [] closing [] closed [] activated [] deactivated [] iconified [] deiconified []}}]
  (proxy [java.awt.event.WindowListener] []
    (windowOpened      ([^WindowEvent arg0] (do (doall (map #(%) opened))
                                                (doall (map #(%) (rift (state/state :opened)  []))))))
    (windowClosing     ([^WindowEvent arg0] (do (doall (map #(%) closing))
                                                (doall (map #(%) (rift (state/state :closing) []))))))
    (windowClosed      ([^WindowEvent arg0] (do (doall (map #(%) closed))
                                                (doall (map #(%) (rift (state/state :closed)  []))))))
    (windowActivated   ([^WindowEvent arg0] (do (doall (map #(%) activated))
                                                (doall (map #(%) (rift (state/state :activated)   []))))))
    (windowDeactivated ([^WindowEvent arg0] (do (doall (map #(%) deactivated))
                                                (doall (map #(%) (rift (state/state :deactivated) []))))))
    (windowIconified   ([^WindowEvent arg0] (do (doall (map #(%) iconified))
                                                (doall (map #(%) (rift (state/state :iconified)   []))))))
    (windowDeiconified ([^WindowEvent arg0] (do (doall (map #(%) deiconified))
                                                (doall (map #(%) (rift (state/state :deiconified) []))))))))

;; (state/concat-state :activated [(fn [] (println "Activated"))])
;; (state/state :activated)

(defn- new-frame
  [title items size undecorated?]
  (let [nframe (seesaw.core/frame
                :title title
                :resizable? true
                :undecorated? undecorated?
                :size [(first size) :by (second size)]
                :minimum-size [600 :by 400]
                :content (state/state :app)
                ;;    :on-close :exit
                :listen [:component-resized
                         (fn [e]
                           ;;  (println e)
                           (let [w (.getWidth (.getSize (.getContentPane (to-root e))))
                                 h (.getHeight (.getSize (.getContentPane (to-root e))))
                                 [oldw oldh] @(state/state :atom-app-size)]
                             ;; Resize direction
                             (reset! (state/state :atom-app-resize-direction) [(if (<= oldw w) 1 -1) (if (<= oldh h) 1 -1)])
                             ;; Frame size
                             (reset! (state/state :atom-app-size) [w h])
                             ;; Extended on resize
                             (doall (map (fn [[k func]]
                                           (try
                                             (func)
                                             (catch Exception e
                                               (do ;;(println "cannot run " k)
                                                 (state/set-state :on-frame-resize-fns-v (dissoc (state/state :on-frame-resize-fns-v) k))))))
                                         (state/state :on-frame-resize-fns-v))))
                           (.revalidate (to-widget e)))])]
    (.addWindowListener nframe (OverridedWindowListener :closing [(fn [] (println "\nClosing Override") (state/set-state :soft-restart nil) (.dispose nframe))]))
    nframe))

(def build
  (fn [& {:keys [title
                 items
                 size
                 undecorated?]
          :or  {title "Mr. Jarman"
                items (label :text "Hello Boi!" :bounds [100 100 300 300])
                size @(state/state :atom-app-size)
                undecorated? false}}]
    (let [set-items (if-not (sequential? items) (list items) items)]
      (do
        (state/set-state :app (build-main-panel set-items))
        (gas/start :soft (state/state :soft-restart))
        (if (and (state/state :soft-restart)
                 (not (nil? (state/state :frame))))
          (do
            (config! (state/state :frame) :content (state/state :app))
            (config! (state/state :frame) :size [(first size) :by (second size)]))
          (do
            (state/set-state :frame (new-frame title items size undecorated?))
            (let [jframe (state/state :frame)]
              (-> (doto jframe (.setLocationRelativeTo nil) pack! show!))
              (config! jframe  :icon  (gs/icon GoogleMaterialDesignIcons/DATE_RANGE face/c-icon)
                       :size [(first size) :by (second size)]))))))))

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
