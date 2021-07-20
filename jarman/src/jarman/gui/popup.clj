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

;; (def popup-state (atom (new JLayeredPane)))

;; (defn set-JLP [jlp] (swap! popup-state #(assoc % :jlp jlp)))

;; (defn- jlp [] @popup-state)

(defn- jlp [] (state/state :app))

(defn- add [item] (.add (jlp) item (new Integer 25)))

(defn- frame-wh []
  (let [frame-size (.getSize (c/to-frame (jlp)))
        w (.getWidth frame-size)
        h (.getHeight frame-size)]
    [w h]))

(defn- popup-bar [root title]
  (gcomp/hmig
   :hrules "[grow]0px[fill]"
   :items (gtool/join-mig-items
           (c/label :text title :font (gtool/getFont 10))
           (c/label ;;:text "Close"
            :icon (jarman.tools.swing/image-scale jarman.resource-lib.icon-library/x-blue1-64-png 20)
            :halign :right
            :border (b/empty-border :thickness 5)
            :listen [:mouse-entered gtool/hand-hover-on
                     :mouse-clicked (fn [e]
                                      (.remove (jlp) root)
                                      (.repaint (jlp)))]))))

(defn- popup [{:keys [render-fn title size]
               :or {render-fn (fn [] (c/label))
                    title ""
                    size [400 300]}}]
  (let [last-x (atom 0)
        last-y (atom 0)
        [frame-w frame-h] (frame-wh)
        [body-w body-h]   size
        [x y] [(- (/ frame-w 2) (/ body-w 2))
               (- (/ frame-h 2) (/ body-h 2))]
        bounds [x y body-w body-h]
        br "#aaa"
        
        root (mig-panel :constraints ["wrap 1" "0px[fill, grow]0px" "0px[fill, fill]0px[fill, grow]0px"]
                        :bounds bounds
                        :border (b/line-border :thickness 1 :color br))]
    (c/config! root :listen [:mouse-pressed
                             (fn [e]
                               (let [[start-x start-y] (gtool/get-mouse-pos)]
                                 (reset! last-x start-x)
                                 (reset! last-y start-y)
                                 (c/move! (c/to-widget e) :to-front)))
                             
                             :mouse-dragged (fn [e]
                               (let [old-bounds (c/config (c/to-widget e) :bounds)
                                     [old-x old-y] [(.getX old-bounds) (.getY old-bounds)]
                                     [new-x new-y] (gtool/get-mouse-pos)
                                     move-x  (if (= 0 @last-x) 0 (- new-x @last-x))
                                     move-y  (if (= 0 @last-y) 0 (- new-y @last-y))]
                                 (reset! last-x new-x)
                                 (reset! last-y new-y)
                                 (c/config! (c/to-widget e) :bounds [(+ old-x move-x) (+ old-y move-y) :* :*])))
                             
                             :mouse-released (fn [e] (.repaint (jlp)))])
    (.add root (popup-bar root title))
    (.add root (render-fn))
    root))


(defn- comp []
  (gcomp/vmig
   :args [:background "#eee"]
   :items (gtool/join-mig-items
           (gcomp/textarea "(ﾉ◉ᗜ◉)ﾉ*:･ﾟ✧OLA NINIOO .:ヽ(⚆ o ⚆)ﾉ"))))


  ;; create jframe
;; (def build (seesaw.core/frame
;;           :title "title"
;;           :undecorated? false
;;           :minimum-size [800 :by 600]
;;           :content (jlp)))

;; run app on middle screen
;; (-> (doto build (.setLocationRelativeTo nil) c/pack! c/show!))

(defn build-popup
  [{:keys [comp-fn title size]
    :or {comp-fn comp
         title ""
         size [400 300]}}]
  (.add (jlp) (popup {:render-fn comp-fn
                      :title title
                      :size size})
        (new Integer 10)))

(defn set-demo [] (build-popup {:comp-fn comp :title "Demo"}))

(build-popup {:comp-fn (fn [] (seesaw.core/vertical-panel :items (list (seesaw.core/label :text "heyy"))))
              :title "heyy"})


