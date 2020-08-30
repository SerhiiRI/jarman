;; 
;; Compilation: dev_tool.clj -> data.clj -> gui_tools.clj -> gui_alerts_service.clj -> gui_app.clj
;; 
(ns jarman.gui-tools
  (:use seesaw.core
        seesaw.border
        seesaw.dev
        seesaw.mig
        jarman.tools.dev-tools)
  (:require [jarman.resource-lib.icon-library :as icon]
            [clojure.string :as string]))


;; (import javax.swing.JInternalFrame)
;; (import javax.swing.BorderFactory)
;; (import javax.swing.JDesktopPane)
;; (import javax.swing.JButton)
;; (import javax.swing.JFrame)
;; (import javax.swing.Jlabel-fn)
;; (import java.awt.Dimension)
;; (import java.awt.Component)
;; (import java.awt.Point)
(import javax.swing.JLayeredPane)
(import java.awt.Color)
(import java.awt.MouseInfo)
(import java.awt.event.MouseListener)
(import java.awt.event.MouseEvent)
(import java.awt.PointerInfo)

(defn getWidth  [obj] (.width (.getSize obj)))
(defn getHeight [obj] (.height (.getSize obj)))
(defn getSize   [obj] (let [size (.getSize obj)] [(.width size) (.height size)]))
(defn getParent [obj] (.getParent (seesaw.core/to-widget obj)))

(def getFont
  (fn [& params] (-> {:size 12 :style :plain :name "Arial"}
                     ((fn [m] (let [size  (first (filter (fn [item] (if (number? item) item nil)) params))] (if (number? size) (merge m {:size size}) (conj m {})))))
                     ((fn [m] (let [name  (first (filter (fn [item] (if (string? item) item nil)) params))] (if (string? name) (merge m {:name name}) (conj m {})))))
                     ((fn [m] (let [style (first (filter (fn [item] (if (= item :bold) item nil)) params))] (if (= style :bold) (merge m {:style :bold}) (conj m {})))))
                     ((fn [m] (let [style (first (filter (fn [item] (if (= item :italic) item nil)) params))] (if (= style :italic)
                                                                                                                (if (= (get m :style) :bold) (merge m {:style #{:bold :italic}}) (merge m {:style :italic})) 
                                                                                                                (conj m {})))))
                     )))

;; (seesaw.font/font-families)
;; (label-fn :text "txt")

;; Function for label with pre font
(def label-fn (fn [& params] (apply label :font (getFont) params)))

(defn middle-bounds 
  "Description:
      Return middle bounds with size.
   Example:
      [x y w h] => [100 200 250 400]
      (middle-bounds root 250 400) => [550 400 250 400]
   Needed:
      Function need getSize function for get frame width and height
   "
  [obj width height] (let [size (getSize obj)
                           x (first size)
                           y (last size)]
                       [(- (/ x 2) (/ width 2)) (- (/ y 2) (/ height 2)) width height]))

(def hand-hover-on  (fn [e] (config! e :cursor :hand)))
(def hand-hover-off (fn [e] (config! e :cursor :default)))

(defn build-bottom-ico-btn
  "Description:
      Icon btn for message box. Create component with icon btn on bottom.
   Example:
      (build-bottom-ico-btn icon/loupe-grey-64-png icon/loupe-blue1-64-png 23 (fn [e] (alert 'Wiadomosc')))
   Needed:
      Import jarman.dev-tools
      Function need image-scale function for scalling icon
      Function need hand-hover-on function for hand mouse effect
   "
  [ic ic-h layered & args] (label-fn :icon (image-scale ic (if (> (count args) 0) (first args) 28))
                                     :background (new Color 0 0 0 0)
                                     :border (empty-border :left 3 :right 3)
                                     :listen [:mouse-entered (fn [e] (do 
                                                                       (config! e :icon (image-scale ic-h (if (> (count args) 0) (first args) 28)) :cursor :hand)
                                                                       (.repaint layered)))
                                              :mouse-exited (fn [e] (do
                                                                       (config! e :icon (image-scale ic (if (> (count args) 0) (first args) 28)))
                                                                       (.repaint layered)))
                                              :mouse-clicked (if (> (count args) 1) (second args) (fn [e]))]))

(defn build-ico
  "Description:
      Icon for message box. Create component with icon.
   Example:
      (build-ico icon/alert-64-png)
   Needed:
      Import jarman.dev-tools
      Function need image-scale function for scalling icon
   "
  [ic] (label-fn :icon (image-scale ic 28)
              :background (new Color 0 0 0 0)
              :border (empty-border :left 3 :right 3)))

(defn build-header 
  "Description:
      Header text for message box. Create component with header text.
   Example:
      (build-header 'Information')
   "
  [txt] (label-fn :text txt
               :font (getFont 14 :bold)
               :background (new Color 0 0 0 0)))

(defn build-body
  "Description:
      Body text for message box. Create component with message.
   Example:
      (build-body 'My message')
   "
  [txt] (label-fn :text txt
               :font (getFont 13)
               :background (new Color 0 0 0 0)
               :border (empty-border :left 5 :right 5 :bottom 2)))


(def template-resize
  "Discription:
      Function for JLayeredPane for resize it to app window.
   Example:
      (template-resize my-app)
   "
  (fn [app-template]
    (let [v-size (.getSize    (to-root app-template))
          vw     (.getWidth   v-size)
          vh     (.getHeight  v-size)]
      (config! app-template  :bounds [0 0 vw vh]))))

(defn str-cutter  
  "Description:
      Cut message and add ...
   Example: 
      (str-cutter 'Some really long but not complicated message.')    => 'Some really long but not complicated me...'
      (str-cutter 'Some really long but not complicated message.' 10) => 'Some reall...'
   "
  ([txt] (cond
           (> (count txt) 40) (string/join  "" [(subs txt 0 40) "..."])
           :else txt))
  ([txt max-len] (cond
                   (> (count txt) max-len) (string/join  "" [(subs txt 0 max-len) "..."])
                   :else txt)))


(defn get-elements-in-layered-by-id
  "Description:
     Set same id inside elements and found them all.
  Return:
     List of components/objects => (object[xyz] object(xyz))
  Example:
     (get-elements-in-layered-by-id event_or_some_root 'id_in_string')
  "
  [e ids] (let [id (keyword ids)
                select-id (keyword (string/join ["#" ids]))
                root (to-root (seesaw.core/to-widget e))
                selected (select root [select-id])
                outlist (if selected (filter (fn [i] (identical? (config i :id) id)) (seesaw.util/children (.getParent selected))) nil)]
            (if outlist outlist nil)))


(def slider-ico-btn 
  "Description:
      Slide buttons used in JLayeredPanel. 
      Normal state is small square with icon 
      but on hover it will be wide and text will be inserted.
   Example:
      (function icon size header map-with-other-params)
      (slider-ico-btn (image-scale icon/user-64x64-2-png 50) 0 50 'Klienci' {:onclick (fn [e] (alert 'Clicked'))})
   "
  (fn [ico order size txt extends]
                  (let [bg-color "#ddd"
                        color-hover-margin "#bbb"
                        bg-color-hover "#d9ecff"
                        size size
                        y (if (> (* size order) 0) (+ (* 2 order) (* size order)) (* size order))]
                    (label-fn
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
                                                              :cursor :default))
                              :mouse-clicked (if (= (contains? extends :onclick) true) (get extends :onclick) (fn [e]))]))))


(def expand-btn
  "Description:
      It's a space for main button with more option. 
      Normal state is one button but after on click 
      space will be bigger and another buttons will be added.
      If button don't have any function, can not be expanded.
   Example:
      (expand-btn 'Button name' (Element or Component))
      (expand-btn 'Profile' (button 'Do something'))
      (expand-btn 'Settings' (button 'Do something') (button 'Do something else'))
   Needed:
      Import jarman.dev-tools
      Function need image-scale function for scalling icon
      "
  (fn [txt & inside-btns]
                    (let [bg-color "#eee"
                          margin-color "#fff"
                          border (compound-border (line-border :left 6 :color bg-color) (line-border :bottom 2 :color margin-color))
                          vsize 35
                          hsize 200
                          ico (if (> (count inside-btns) 0) (image-scale icon/plus-64-png 25))
                          ico-hover (image-scale icon/minus-grey-64-png 20)]
                      (mig-panel
                       :constraints ["wrap 1" "0px[fill]0px" "0px[fill]0px"]
                       :listen [:mouse-entered hand-hover-on
                                :mouse-clicked (fn [e]
                                                 (if (> (count inside-btns) 0)
                                                   (if (== (count (seesaw.util/children (seesaw.core/to-widget e))) 1)
                                                     (do
                                                      ;;  Add inside buttons to mig with expand button
                                                       (config! e :items (vec (map (fn [item] (vec (list item))) (concat (vec (seesaw.util/children (seesaw.core/to-widget e))) (vec inside-btns)))))
                                                       (config! (last (seesaw.util/children (first (seesaw.util/children (seesaw.core/to-widget e))))) :icon ico-hover))
                                                     (do
                                                      ;;  Remove inside buttons form mig without expand button
                                                       (config! e :items [(vec (list (first (seesaw.util/children (seesaw.core/to-widget e)))))])
                                                       (config! (last (seesaw.util/children (first (seesaw.util/children (seesaw.core/to-widget e))))) :icon ico)))))]
                       :items [[(mig-panel
                                 :constraints ["" "0px[fill]0px" "0px[fill]0px"]
                                 :background (new Color 0 0 0 0)
                                 :items [[(label-fn
                                           :text txt
                                           :size [(- hsize vsize) :by vsize]
                                           :background bg-color
                                           :border border)]
                                         [(label-fn
                                           :size [vsize :by vsize]
                                           :halign :center
                                           :background bg-color
                                           :border border
                                           :icon ico)]])]]))))



(def tab-btn
  "Description:
      Buttons for changing opened tables or functions in right part of app.
   Example:
      (function 'tab-name' 'active/inactive')
      (tab-btn 'Tab 1' true)
   Needed:
      Import jarman.dev-tools
      Function need image-scale function for scalling icon
   "
  (fn [txt active size]
    (let [bg-color (if (= active true) "#eee" "#ccc")
          border "#fff"
          hsize (first size)
          vsize (last size)]
      (horizontal-panel
       :background bg-color
       :items [(label-fn
                :text txt
                :halign :center
                :size [hsize :by vsize])
               (label-fn
                :icon (image-scale icon/x-grey2-64-png 15)
                :halign :center
                :border (line-border :right 2 :color border)
                :size [vsize :by vsize]
                :listen [:mouse-entered (fn [e] (config! e :cursor :hand :icon (image-scale icon/x-blue1-64-png 15)))
                         :mouse-exited  (fn [e] (config! e :cursor :default :icon (image-scale icon/x-grey2-64-png 15)))])]
       :listen [:mouse-entered hand-hover-on]))))