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
  (let [last-x (atom 0)
        last-y (atom 0)
        [frame-w frame-h] (frame-wh)
        [body-w body-h]   [300 200]
        [x y] [(- (/ frame-w 2) (/ body-w 2))
               (- (/ frame-h 2) (/ body-h 2))]
        bounds (doto [x y body-w body-h] println)
        root (mig-panel :constraints ["" "0px[fill, grow]0px" "0px[fill, grow]0px"]
                        :bounds bounds
                        :background "#bbb"
                        :border (b/line-border :thickness 2 :color "#aaa"))]
    (c/config! root :listen [:mouse-pressed (fn [e]
                                              (let [new-pos (.getLocation (java.awt.MouseInfo/getPointerInfo))
                                                    start-x   (.getX new-pos)
                                                    start-y   (.getY new-pos)]
                                                (reset! last-x start-x)
                                                (reset! last-y start-y)
                                                (c/move! (c/to-widget e) :to-front)))
                             :mouse-dragged
                             (fn [e]
                               (let [old-bounds (c/config (c/to-widget e) :bounds)
                                     [old-x old-y] [(.getX old-bounds) (.getY old-bounds)]
                                     new-pos (.getLocation (java.awt.MouseInfo/getPointerInfo))
                                     new-x   (.getX new-pos)
                                     new-y   (.getY new-pos)
                                     move-x  (if (= 0 @last-x) 0 (- new-x @last-x))
                                     move-y  (if (= 0 @last-y) 0 (- new-y @last-y))]
                                 (reset! last-x new-x)
                                 (reset! last-y new-y)
                                 (c/config! (c/to-widget e) :bounds [(+ old-x move-x) (+ old-y move-y) :* :*]))
                               )])
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
(.add (app) (pop comp) (new Integer 25))
