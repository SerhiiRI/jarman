(ns jarman.gui.popup
  (:use seesaw.dev
        seesaw.style
        seesaw.mig
        seesaw.font)
  (:require
   [jarman.faces :as face]
   [seesaw.core :as c]
   [seesaw.border :as b]
   [clojure.string :as string]
   [seesaw.util :as u]
   [jarman.gui.gui-tools :as gtool]
   [jarman.tools.swing :as stool]
   [jarman.resource-lib.icon-library :as icon]
   [jarman.logic.state :as state]
   [jarman.gui.gui-components :as gcomp]
   [jarman.gui.gui-migrid     :as gmg]
   [jarman.gui.gui-style      :as gs]
   [jarman.resource-lib.icon-library :as icon]
   [jarman.tools.swing               :as stool]
   [jarman.tools.lang         :refer :all])
  (:import javax.swing.JLayeredPane
           java.awt.PointerInfo
           (java.awt.event MouseEvent)
           (jiconfont.icons.google_material_design_icons GoogleMaterialDesignIcons)))

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

(defn- popup-bar
  [root title
   & {:keys [icon c-bg args]
      :or   {icon nil
             c-bg "#eee"
             args []}}]
  (let [header (gmg/hmig
                :hrules "[grow]0px[fill]"
                :lgap 10
                :args (concat [:background face/c-popup-head-background] args) ;; set header bg
                :items (gtool/join-mig-items
                        (c/label :text title :icon (if icon icon)
                                 :font (gs/getFont :bold))
                        (c/label ;;:text "Close"
                         :icon (gs/icon GoogleMaterialDesignIcons/HIGHLIGHT_OFF)
                         :halign :right
                         :border (b/empty-border :thickness 5)
                         :listen [:mouse-entered (fn [e]
                                                   (gtool/hand-hover-on e)
                                                   (c/config! e :icon (gs/icon GoogleMaterialDesignIcons/HIGHLIGHT_OFF face/c-icon-close-focus)))
                                  :mouse-exited  (fn [e]
                                                   (c/config! e :icon (gs/icon GoogleMaterialDesignIcons/HIGHLIGHT_OFF)))
                                  :mouse-clicked (fn [e]
                                                   (.remove (jlp) root)
                                                   (.repaint (jlp)))])))]
    header))


(defn- close-on-click [jlp root]
  (fn [e]
    (if (= (.getButton e) MouseEvent/BUTTON2)
      (do (.remove (jlp) root)
          (.repaint (jlp))))))

(defn- start-mouse-pos [last-x last-y root]
  (fn [e]
    (c/config! e :cursor :move)
    (let [[start-x start-y] (gtool/get-mouse-pos)]
      (reset! last-x start-x)
      (reset! last-y start-y)
      (c/move! root :to-front))))

(defn- drag-panel [last-x last-y root]
  (fn [e]
    (let [old-bounds (c/config root :bounds)
          [old-x old-y] [(.getX old-bounds) (.getY old-bounds)]
          [new-x new-y] (gtool/get-mouse-pos)
          move-x  (if (= 0 @last-x) 0 (- new-x @last-x))
          move-y  (if (= 0 @last-y) 0 (- new-y @last-y))]
      (reset! last-x new-x)
      (reset! last-y new-y)
      (c/config! root :bounds [(+ old-x move-x) (+ old-y move-y) :* :*]))))

(defn- popup [{:keys [render-fn title size c-border title-icon args]
               :or {render-fn (fn [api] (c/label))
                    title ""
                    size [400 300]
                    c-border face/c-popup-border
                    title-icon nil
                    args []}}]
  (let [last-x (atom 0)
        last-y (atom 0)
        [frame-w frame-h] (frame-wh)
        [body-w body-h]   size
        [x y] [(- (/ frame-w 2) (/ body-w 2))
               (- (/ frame-h 2) (/ body-h 2))]
        bounds [x y body-w body-h]
        
        root (apply mig-panel :constraints ["wrap 1" "0px[fill, grow]0px" "0px[fill, fill]0px[fill, grow]0px"]
                    :bounds bounds
                    :border (b/line-border :thickness 1 :color c-border)
                    :background face/c-popup-body-background
                    args)
        mc (close-on-click jlp root)
        mp (start-mouse-pos last-x last-y root)
        md (drag-panel last-x last-y root)
        mr (fn [e] (c/config! e :cursor :default) (.repaint (jlp)))

        bar (popup-bar root title :c-bg c-border :icon title-icon)

        api {:close (fn [] (do (.remove (jlp) root) (.repaint (jlp))))}]
    
    (doall (map #(c/config! % :listen [:mouse-clicked mc
                                       :mouse-pressed mp
                                       :mouse-dragged md
                                       :mouse-released mr])
                [root bar]))
    
    (.add root bar)
    (.add root (render-fn api))
    root))


(defn- compo [api]
  (gmg/migrid :v {:args [:background "#eee"]}
   (gcomp/textarea "(ﾉ◉ᗜ◉)ﾉ*:･ﾟ✧OLA NINIOO .:ヽ(⚆ o ⚆)ﾉ")))


  ;; create jframe
;; (def build (seesaw.core/frame
;;           :title "title"
;;           :undecorated? false
;;           :minimum-size [800 :by 600]
;;           :content (jlp)))

;; run app on middle screen
;; (-> (doto build (.setLocationRelativeTo nil) c/pack! c/show!))

(defn build-popup
  [{:keys [comp-fn title title-icon size c-border args]
    :or {comp-fn compo
         title ""
         title-icon nil
         size [400 300]
         c-border "#ccc"
         args []}}]
  (let [pop (popup {:render-fn comp-fn
                    :title title
                    :title-icon title-icon
                    :size size
                    :c-border c-border
                    :args args})]
    (.add (jlp) pop (new Integer 10))
    (c/move! pop :to-front)
    pop))

(defn set-demo [] (build-popup {:comp-fn compo :title "Demo"}))

(defn confirm-popup
  "Description:
     Confirm template"
  ([message ev-yes] (confirm-popup message ev-yes nil [250 150]))
  ([message ev-yes ev-no] (confirm-popup message ev-yes ev-no [250 150]))
  ([message ev-yes ev-no size]
   (build-popup {:comp-fn (fn [api]
                            (gmg/migrid
                             :v :center :center
                             [(c/label :text (gtool/htmling message :center))
                              (gcomp/menu-bar {:buttons [[(gtool/get-lang-basic :yes) nil (fn [e]
                                                                                            (ev-yes)
                                                                                            ((:close api)))]
                                                         [(gtool/get-lang-basic :no)  nil (fn [e]
                                                                                            (if (fn? ev-no) (ev-no))
                                                                                            ((:close api)))]]})]))
                 :size size
                 :title (gtool/get-lang-basic :confirm?)})
   (timelife 0.02 #(.repaint (state/state :app)))))

(defn confirm-popup-window
  "Description:
     Confirm template in window"
  ([message ev-yes] (confirm-popup-window message ev-yes nil [250 150]))
  ([message ev-yes ev-no] (confirm-popup-window message ev-yes ev-no [250 150]))
  ([message ev-yes ev-no size]
   (gcomp/popup-window
    {:window-title (gtool/get-lang-basic :confirm?)
     :relative (state/state :app)
     :size size
     :view (let [panel (gmg/migrid :v :center :center [])

                 menu (fn [] [(c/label :text (gtool/htmling message :center))
                              (gcomp/menu-bar {:buttons [[(gtool/get-lang-basic :yes)
                                                          nil
                                                          (fn [e]
                                                            (ev-yes)
                                                            (.dispose (c/to-frame panel)))]
                                                         
                                                         [(gtool/get-lang-basic :no)
                                                          nil
                                                          (fn [e]
                                                            (if (fn? ev-no) (ev-no))
                                                            (.dispose (c/to-frame panel)))]]})])]
             (c/config! panel :items (gtool/join-mig-items (menu)))
             panel)})))

