(ns jarman.gui.gui-mouse-menu
  (:require [seesaw.core               :as c]
            [seesaw.border             :as b]
            [jarman.faces              :as face]
            [jarman.gui.gui-tools      :as gtool]
            [jarman.logic.state        :as state]
            [jarman.tools.lang :refer :all]))

(defn- filtered-btns
  [buttons-vec]
  (if (empty? buttons-vec)
    []
    (vec (filter #(and (not (empty? %))
                       (>= (count %) 3)
                       (or (keyword? (first %)) (string? (first %)))
                       (fn? (nth % 3)))
                 buttons-vec))))

(defn- calc-bounds
  "Description:
     Calculate bounds of menu box using mouse position and count of buttons to render.
     [x y w h]
  Example:
     (calc-bounds [item item ...])
     ;; => [345 678 100 90]"
  [items]
  (let [border-w 2
        border-h 2
        box-w  (+ 150 border-w)
        box-h  (+ (* (count items) 30) border-h)

        [mx my](gtool/get-mouse-pos-onFrame)
        mx     (- mx (- box-w 15))
        my     (- my 15)
        bounds [mx my box-w box-h]]
    bounds))

(defn- on-focus [e]
  (c/config! e :background face/c-on-focus)
  (gtool/hand-hover-on e)
  (.repaint (state/state :app)))

(defn- off-focus [e]
  (c/config! e :background face/c-compos-background))

(defn- mouse-menu-row
  [title icon tip action-fn exit-fn]
  (let [max-len 20]
    (c/label :text (gtool/str-cutter title max-len)
             :icon icon
             :tip  (rift tip (if (> (count title) max-len) title))
             :border (b/empty-border :left 5 :right 5)
             :focusable? true
             :background face/c-compos-background
             :listen [:mouse-entered (fn [e] (on-focus e))
                      :mouse-exited  (fn [e] (off-focus e))
                      :mouse-clicked (fn [e] (action-fn e) (exit-fn))])))

(defn- mouse-menu-supervisior
  [panel rm-outline-fn]
  (let [JLP (state/state :app)]
    (if (> (.getIndexOf JLP panel) -1)
      (do
        ;; (println "jest")
        (rm-outline-fn)
        (timelife 0.2 #(mouse-menu-supervisior panel rm-outline-fn)))
      (.repaint (state/state :app))
      ;; (do
      ;;   (println "rm rmb menu"))
      )))

(def mouse-menu
  "Description:
     Right Mouse Click Menu.
     Will be open as box on JPL.
     [[Title Icon tip Fn] ...]
  Example:
     (mouse-menu [[\"Open\" nil nil (fn [e])]])
     (mouse-menu [[\"Open\" icon nil (fn [e])] [\"Save\" icon \"Some tip\" (fn [e])]])"
  (fn [ev buttons-vec]
    (gtool/rmb?
     ev
     #(let [btns-v (filtered-btns buttons-vec)]
        (if-not (empty? btns-v)
          (let [panel (seesaw.mig/mig-panel
                       :id :mouse-menu
                       :background face/c-layout-background
                       :constraints ["wrap 1" "0px[150, fill]0px" "0px[30:150:, fill]0px"]
                       :bounds (calc-bounds btns-v)
                       :border (b/line-border :thickness 1 :color face/c-on-focus)
                       :items [])

                rm-menu (fn [] (let [JLP (state/state :app)] (.remove JLP panel) (.repaint JLP)))

                rm-menu-outline (fn [] (let [panel-size (.getSize panel)
                                             min-x (.getX panel) 
                                             min-y (.getY panel)
                                             max-x (+ min-x (.getWidth panel-size)) 
                                             max-y (+ min-y (.getHeight panel-size)) 
                                             [mx-pos my-pos] (gtool/get-mouse-pos-onFrame)]
                                         (if-not (and (< mx-pos max-x)
                                                      (> mx-pos min-x)
                                                      (< my-pos max-y)
                                                      (> my-pos min-y))
                                           (rm-menu))))
                
                menu-items (doall (map
                                   (fn [[title icon tip func]]
                                     (mouse-menu-row title icon tip func rm-menu))
                                   btns-v))]
            
            (c/config! panel :items (gtool/join-mig-items menu-items))

            (.add (state/state :app) panel (new Integer 1000))
            (mouse-menu-supervisior panel rm-menu-outline)
            (c/move! panel :to-front)
            ))))))


;; (jarman.gui.gui-views-service/add-view
;;  :view-id   :demo-rmb
;;  :title     "Demo rmb menu"
;;  :render-fn
;;  (fn []
;;    (jarman.gui.gui-migrid/migrid
;;     :v :center :center
;;     (c/label
;;      :text "Click me with right mouse button"
;;      :listen
;;      [:mouse-clicked
;;       (fn [e]
;;
;;         ;; Right Mouse Button Menu
;;         (mouse-menu
;;          e
;;          [["Test" nil nil (fn [e] (println "RMB popup menu"))]
;;           ["Jakiś dłuższy tekst ale nie za długi" nil nil (fn [e] )]])
;;
;;         )]))))
