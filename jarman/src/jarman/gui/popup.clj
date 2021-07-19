(ns jarman.gui.popup
  (:use seesaw.dev
        seesaw.style
        seesaw.mig
        seesaw.font)
  (:require
   [seesaw.core :as c]
   [seesaw.border :as b]
   [clojure.string :as string]
   [seesaw.util :as u]
   [jarman.gui.gui-tools :as gtool]
   [jarman.tools.swing :as stool]
   [jarman.resource-lib.icon-library :as icon]
   [jarman.logic.state :as state]
   [jarman.gui.gui-components :as gcomp])
  (:import javax.swing.JLayeredPane
           java.awt.PointerInfo))

(println "\nNew Window")

(def popup-state (atom {:app (new JLayeredPane)}))

(defn- app [] (:app @popup-state))

(defn- add [item] (.add (app) item (new Integer 25)))

(defn- frame-wh []
  (let [frame-size (.getSize (c/to-frame (app)))
        w (.getWidth frame-size)
        h (.getHeight frame-size)]
   [w h]))

(defn- pop [render-fn]
  (let [last-pos (atom [0 0])
        [frame-w frame-h] (frame-wh)
        [body-w body-h]   [300 200]
        [x y] [(- (/ frame-w 2) (/ body-w 2))
               (- (/ frame-h 2) (/ body-h 2))]
        bounds (doto [x y body-w body-h] println)
        root (mig-panel :constraints ["" "0px[fill, grow]0px" "0px[fill, grow]0px"]
                        :bounds bounds
                        :background "#bbb"
                        )]
    (c/config! root :listen [:mouse-dragged
                             (fn [e]
                               (let [[now-x now-y] [(.getX e) (.getY e)]
                                     [old-x old-y] [(first @last-pos) (second @last-pos)]
                                     diff-x (- now-x old-x)
                                     diff-y (- now-y old-y)
                                     diff-x (if (neg? diff-x) (* -1 diff-x) diff-x)
                                     diff-y (if (neg? diff-y) (* -1 diff-y) diff-y)]
                                 (println "\nPos" [diff-x diff-y now-x now-y])
                                 (do
                                     (reset! last-pos [now-x now-y])
                                     (c/move! root :to [:* now-y]))
                                 ;; (if (or (< diff-x 50) (< diff-y 50))
                                 ;;   (do
                                 ;;     (reset! last-pos [now-x now-y])
                                 ;;     (c/move! root :to [now-x now-y])))
                                 ;; (println "\nPos" pos)
                                 
                                 ;; (println "\nPos" (new java.awt.Point x y))
                                 ;; (c/config! root :location-on-screen (new java.awt.Point x y))
                                 ))])
    (.add root (render-fn))
    root))

;; (seesaw.dev/show-options (mig-panel))

(defn- comp []
  (gcomp/vmig
   :args [:background "#ccc"]
   :items (gtool/join-mig-items
           (c/label :text "My title")
           (gcomp/textarea "Some body once pepe : D"))))


  ;; create jframe
(def build (seesaw.core/frame
          :title "title"
          :undecorated? false
          :minimum-size [800 :by 600]
          :content (app)))

;; run app on middle screen
(-> (doto build (.setLocationRelativeTo nil) c/pack! c/show!))


(.add (app) (pop comp) (new Integer 25))
