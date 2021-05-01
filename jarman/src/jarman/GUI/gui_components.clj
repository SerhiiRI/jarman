(ns jarman.gui.gui-components
  (:use seesaw.core
        seesaw.border
        seesaw.dev
        seesaw.mig
        seesaw.util)
  (:require [jarman.resource-lib.icon-library :as icon]
            [seesaw.core :as c]
            [seesaw.border :as sborder]
            [seesaw.util :as u]
            [seesaw.mig :as smig]
            [jarman.tools.swing :as stool]
            [jarman.gui.gui-tools :refer :all :as gtool])
  (:import (java.awt Color)))



;; ┌────────────────────┐
;; │                    │
;; │ Basic components   │
;; │                    │________________________________________
;; └────────────────────┘                                       V


(defn hr 
  ([line-size] (label :border (empty-border :top line-size)))
  ([line-size line-color] (label :border (line-border :top line-size :color line-color))))

(defn auto-scrollbox
  [component & args]
  (let [scrol (apply scrollable component :border nil args)
        scrol (config! scrol :listen [:property-change
                                (fn [e] (invoke-later (let [get-root (fn [e](.getParent (.getParent (.getParent (.getSource e)))))
                                                            vbar 40
                                                            w (- (.getWidth (get-root e)) vbar)
                                                            h (.getHeight (config e :preferred-size))]
                                                        (config! component :size [w :by h]))))])]

    (.setUnitIncrement (.getVerticalScrollBar scrol) 20)
    scrol))

(defn scrollbox
  [component & args]
  (let [scr (apply scrollable component :border nil args)]  ;; speed up scrolling
    (.setUnitIncrement (.getVerticalScrollBar scr) 20)
    (.setBorder scr nil)
;;    for hide scroll    
;;    (.setPreferredSize (.getVerticalScrollBar scr) (Dimension. 0 0)) 
    scr))

(defmacro textarea
  "Description
     TextArea with word wrap
   "
  [text & args] `(label :text `~(htmling ~text) ~@args))


(def button-basic
  "Description:
      Simple button with default style.
   Example:
      (simple-button \"Simple button\" (fn [e]) :style [:background \"#fff\"])
   "
  (fn [txt func & {:keys [args
                          vtop
                          vbot
                          hl
                          hr]
                   :or   {args []
                          vtop 10
                          vbot 10
                          hl 10
                          hr 10}}]
    (let [newBorder (fn [underline-color]
                      (compound-border (empty-border :bottom vbot :top vtop :left hl :right hr)
                                       (line-border :bottom 2 :color underline-color)))]
      (apply label
           :text txt
           :focusable? true
           :halign :center
           :listen [:mouse-clicked func
                    :mouse-entered (fn [e] (hand-hover-on e) (button-hover e))
                    :mouse-exited  (fn [e] (button-hover e (get-color :background :button_main)))
                    :focus-gained  (fn [e] (config! e :border (newBorder (get-color :decorate :focus-gained))))
                    :focus-lost    (fn [e] (config! e :border (newBorder (get-color :decorate :focus-lost))))
                    :key-pressed   (fn [e] (if (= (.getKeyCode e) java.awt.event.KeyEvent/VK_ENTER) (func e)))
                    ]
           :background (get-color :background :button_main)
           :border (newBorder (get-color :decorate :focus-lost))
           args))))


(def input-text
  "Description:
    Text component converted to text component with placeholder. Placehlder will be default value.
 Example:
    (input-text :placeholder \"Login\" :style [:halign :center])
 "
  (fn [& {:keys [v
                 placeholder
                 border
                 font-size
                 args]
          :or   {v ""
                 placeholder ""
                 font-size 14
                 border [10 10 5 5]
                 args []}}]
    (let [fn-get-data     (fn [e key] (get-in (config e :user-data) [key]))
          fn-assoc        (fn [e key v] (assoc-in (config e :user-data) [key] v))
          newBorder (fn [underline-color]
                      (compound-border (empty-border :left (nth border 0) :right (nth border 1) :top (nth border 2) :bottom (nth border 3))
                                       (line-border :bottom 2 :color underline-color)))]
      (apply text 
             :text (if (empty? v) placeholder (if (string? v) v (str v)))
             :font (getFont font-size :name "Monospaced")
             :background (get-color :background :input)
             :border (newBorder (get-color :decorate :focus-lost))
             :user-data {:placeholder placeholder :value "" :edit? false :type :input :border-fn newBorder}
             :listen [:focus-gained (fn [e]
                                      (config! e :border (newBorder (get-color :decorate :focus-gained)))
                                      (cond (= (value e) placeholder) (config! e :text ""))
                                      (config! e :user-data (fn-assoc e :edit? true)))
                      :focus-lost   (fn [e]
                                      (config! e :border (newBorder (get-color :decorate :focus-lost)))
                                      (cond (= (value e) "") (config! e :text placeholder))
                                      (config! e :user-data (fn-assoc e :edit? false)))]
             args))))

;; (show-events (text))

;; (show-options (text))

(defn inpose-label
  "Params: 
     title
     component
   "
  [title component
   & {:keys [vtop 
             id]
      :or {vtop 0
           id nil}}]
  (c/grid-panel :columns 1
                :id id
                :border (empty-border :top vtop)
                :items [(c/label :text (if (string? title) title ""))
                        component]))


(defn input-text-with-atom
  [& {:keys [store-id local-changes val editable? enabled? onClick debug]
      :or {local-changes (atom {})
           val ""
           editable? true
           enabled? true
           store-id nil
           onClick (fn [e])
           debug false}}]
  (input-text
   :args [:editable? editable?
          :enabled? enabled?
          :text val
          :foreground (if editable? "#000" "#456fd1")
          :focusable? true
          :background (if-not editable? (gtool/get-color :background :light-mute) (gtool/get-color :background :input))
          :listen [:mouse-clicked onClick
                   :action-performed onClick
                   :mouse-entered (if editable? (fn [e]) (fn [e] 
                                                           (hand-hover-on e)
                                                           (config! e )))
                   :caret-update (fn [e]
                                   (let [new-v (c/value (c/to-widget e))] 
                                    (if debug (println "--Input text change" new-v))
                                    (if debug (println "--Atom" @local-changes))
                                    (cond
                                      (and (not (nil? store-id))
                                           (not (= val new-v)))
                                      (swap! local-changes (fn [storage] (assoc storage store-id new-v)))
                                      :else (reset! local-changes (dissoc @local-changes store-id)))))
                   :focus-gained (fn [e] (c/config! e :border ((get-user-data e :border-fn) (get-color :decorate :focus-gained))))
                   :focus-lost   (fn [e] (c/config! e :border ((get-user-data e :border-fn) (get-color :decorate :focus-lost))))]]))

 
(defn expand-form-panel
  "Description:
     Create panel who can hide inside components. 
   Example:
     (expand-form-panel parent (component or components))
   "
  [view-layout comps
   & {:keys [icon-open
             icon-hide
             text-open
             text-hide
             focus-color
             unfocus-color]
      :or {ico-open nil
           icon-hide nil
           text-open "<<"
           text-hide "..."
           focus-color (get-color :decorate :focus-gained-dark)
           unfocus-color "#fff"}}]
  (let [hidden-comp (atom nil)
        form-space-open ["wrap 1" "0px[fill]0px" "0px[fill]0px"]
        form-space-hide ["" "0px[grow, fill]0px" "0px[grow, fill]0px"]
        form-space (smig/mig-panel :constraints form-space-open)
        onClick (fn [e]
                  (let [inside (u/children form-space)]
                    (if (nil? @hidden-comp)
                      (do
                        (c/config! form-space :constraints form-space-hide)
                        (c/config! e :text text-hide :valign :top :halign :center)
                        (reset! hidden-comp (drop 1 inside))
                        (doall (map #(.remove form-space %) (reverse (drop 1 (range (count inside))))))
                        (.revalidate view-layout))
                      (do
                        (c/config! form-space :constraints form-space-open)
                        (c/config! e :text text-open :halign :left :font (gtool/getFont 16 :bold))
                        (doall (map #(.add form-space %) @hidden-comp))
                        (reset! hidden-comp nil)
                        (.revalidate view-layout)))))
        hide-show (c/label :text text-open
                           :icon icon-open
                           :focusable? true
                           :background "#bbb"
                           :foreground "#fff"
                           :font (gtool/getFont 16 :bold)
                           :border (sborder/empty-border :left 2 :right 2)
                           :listen [:focus-gained (fn [e] (c/config! e :foreground focus-color))
                                    :focus-lost   (fn [e] (c/config! e :foreground unfocus-color))
                                    :mouse-entered gtool/hand-hover-on
                                    :mouse-clicked onClick
                                    :key-pressed  (fn [e] (if (= (.getKeyCode e) java.awt.event.KeyEvent/VK_ENTER) (onClick e)))
                                    ])]
    (c/config! form-space :items (join-mig-items hide-show comps))))



(def input-password
  "Description:
    Text component converted to password input component, placeholder is default value.
 Example:
    ((def input-password :placeholder \"Password\" :style [:halign :center])
 "
  (fn [& {:keys [placeholder
                 style]
          :or   {placeholder "Password"
                 style []}}]
    (let [fn-letter-count (fn [e] (count (value e)))
          fn-hide-chars   (fn [e] (apply str (repeat (fn-letter-count e) "*")))
          fn-get-data     (fn [e key] (get-in (config e :user-data) [key]))
          fn-assoc        (fn [e key v] (assoc-in (config e :user-data) [key] v))]
      (apply text :text placeholder
             :user-data {:placeholder placeholder :value "" :edit? false :type :password}
             :listen [:focus-gained (fn [e]
                                      (cond (= (fn-get-data e :value) "")    (config! e :text ""))
                                      (config! e :user-data (fn-assoc e :edit? true)))
                      :focus-lost   (fn [e]
                                      (cond (= (value e) "") (config! e :text placeholder))
                                      (config! e :user-data (fn-assoc e :edit? false)))
                      :caret-update (fn [e]
                                      (cond (and (= (fn-get-data e :edit?) true)
                                                 (not (= (value e) placeholder)))
                                            (cond (> (count (value e)) 0)
                                                  (let [added-chars (clojure.string/replace (value e) #"\*+" "")]
                                                    (cond (> (count added-chars) 0)
                                                          (do
                                                            (config! e :user-data (fn-assoc e :value (str (fn-get-data e :value) added-chars)))
                                                            (invoke-later (config! e :text (fn-hide-chars e))))
                                                          (< (fn-letter-count e) (count (fn-get-data e :value)))
                                                          (do
                                                            (config! e :user-data (fn-assoc e :value (subs (fn-get-data e :value) 0 (fn-letter-count e))))
                                                            (invoke-later (config! e :text (fn-hide-chars e)))))))))]
             style))))



;; ┌────────────────────┐
;; │                    │
;; │ Expand buttons     │
;; │                    │________________________________________
;; └────────────────────┘                                       V

(def button-expand
  "Description:
      It's a space for main button with more option. 
      Normal state is one button but after on click 
      space will be bigger and another buttons will be added.
      If button don't have any function, can not be expanded.
   Example:
      (button-expand 'Button name' (Element or Component))
      (button-expand 'Profile' (button 'Do something'))
      (button-expand 'Settings' (button 'Do something') (button 'Do something else'))
   Needed:
      Import jarman.dev-tools
      Function need stool/image-scale function for scalling icon
      "
  (fn [txt inside-btns
       & {:keys [background
                 border-color
                 border
                 vsize
                 min-height
                 ico
                 ico-hover]
          :or {background "#eee"
               border-color "#fff"
               border (compound-border (line-border :left 6 :color background))
               vsize 35
               min-height 200
               ico  (stool/image-scale icon/plus-64-png 25)
               ico-hover (stool/image-scale icon/minus-grey-64-png 20)}}]
    (let [inside-btns (if (nil? inside-btns) {} inside-btns)
          inside-btns (if (seqable? inside-btns) inside-btns (list inside-btns))
          ico (if-not (empty? inside-btns) (stool/image-scale icon/plus-64-png 25) nil)
          title (label
                 :text txt
                 :background (Color. 0 0 0 0))
          icon (label
                :size [vsize :by vsize]
                :halign :center
                :background (Color. 0 0 0 0)
                :icon ico)
          mig (mig-panel :constraints ["wrap 1" (str "0px[" min-height ":, grow, fill]0px") "0px[fill]0px"])
          expand-btn (mig-panel
                      :constraints ["" (str "10px[grow, fill]0px[" vsize "]0px") "0px[fill]0px"]
                      :background background
                      :border border
                      :items [[title]
                              [icon]])]
      (do
        (config! mig
                 :items [[expand-btn]]
                 :listen [:mouse-entered hand-hover-on
                          :mouse-clicked (fn [e]
                                           (if-not (empty? inside-btns)
                                             (if (= (count (seesaw.util/children mig)) 1)
                                               (do ;;  Add inside buttons to mig with expand button
                                                 (config! icon :icon ico-hover)
                                                 (doall (map #(.add mig %) inside-btns))
                                                 (.revalidate mig))
                                               (do ;;  Remove inside buttons form mig without expand button
                                                 (config! icon :icon ico)
                                                 (doall (map #(.remove mig %) (reverse (drop 1 (range (count (children mig)))))))
                                                 (.revalidate mig))))
                                           )])))))
                                                 


(def button-expand-child
  "Description
     Interactive button inside menu from expand button.
   "
  (fn [title
       & {:keys [onClick args]
          :or {onClick (fn [e] (println "Clicked: " title))
               args []}}]
    (apply label :font (getFont)
           :text (str title)
           :background "#fff"
           :size [200 :by 25]
           :border (empty-border :left 10)
           :listen [:mouse-clicked onClick
                    :mouse-entered (fn [e] (config! e  :background "#d9ecff"))
                    :mouse-exited  (fn [e] (config! e  :background "#fff"))]
           args)))




;; ┌────────────────────┐
;; │                    │
;; │ Examples           │
;; │                    │________________________________________
;; └────────────────────┘                                       V

;; BUTTON EXPAND
(def view (fn [](button-expand "Expand"
                          (button-expand-child "Expanded")
                          (button-expand-child "Don't touch me." :onClick (fn [e] (config! (to-widget e) :text "FAQ 🖕( ͡° ᴗ ͡°)🖕" :font (getFont 14 :name "calibri")))))))

(def label-img
  (fn [file-path w h]
    (let [img (clojure.java.io/file (str "resources/imgs/" file-path))
          gif (label :text (str "<html> <img width=\"" w "\" height=\"" h "\" src=\"file:" img "\">"))]
      gif)))

(def egg (fn []
           (let [egg1 (label)
                 egg2 (label)]
             (flow-panel
              :items [egg1
                      (button-expand "Expand"
                                     (button-expand-child "Expanded")
                                     (button-expand-child "Don't touch me."
                                                          :onClick (fn [e]
                                                                     (config! egg1
                                                                              :text (value (label-img "egg.gif" 150 130)))
                                                                     (config! egg2
                                                                              :text (value (label-img "egg.gif" 150 130))))))
                      egg2]))))

(def view (fn [](egg)))




(def input-number
  "Description:
    Text component converted to automatic double number validator. Return only correct value... Probably always... 
 Example:
    ((def input-number :style [:halign :center])
 "
  (fn [& {:keys [style display]
          :or   {style []
                 display nil}}]
    (let [last-v (atom "")]
      (apply text
             :background "#fff"
             :listen [:caret-update (fn [e]
                                      (.repaint (to-root (to-widget e)))
                                      (if (empty? (value e))
                                        (do ;; Set "" if :text is empty
                                          (config! e :text "")
                                          (reset! last-v ""))
                                        (do ;; if :text not empty 
                                          (let [v (re-matches #"^[+-]?([0-9]+([.][0-9]*)?|[.][0-9]+)$" (value e)) ;; Get only numbers vec or return nil
                                                v (if-not (nil? v) (first v) nil)]
                                            (if (nil? v)
                                              (do ;; if regex return nil then in :text is some text so change :text to ""
                                                (if-not (and (= "-" (str (first (value e)))) (= 1 (count (value e))))
                                                  (invoke-later (config! e :text @last-v))))
                                              (let [check-v (clojure.string/split v #"\.")] ;; split value by dot to check if double
                                                (if (= (count check-v) 2) ;; if double
                                                  (if (empty? (first check-v))
                                                    (do ;; cat first empty value and return number 0.x
                                                      (invoke-later (config! e :text (read-string (str 0 (second check-v))))))
                                                    (do ;; remember last correct value
                                                      (reset! last-v (read-string v))))
                                                  (if (> (count (first check-v)) 1) ;; if double
                                                    (let [check-first-char (re-matches #"^[+-]?[1-9][0-9]*$" (first check-v))] ;; cut 0 on front
                                                      (if (and (nil? check-first-char) (not (= "-" (str (first (value e))))))
                                                        (do
                                                          (reset! last-v (clojure.string/join "" (clojure.string/split (first check-v) #"0"))) ;; remember last correct value
                                                          (invoke-later (config! e :text @last-v)))
                                                        (do ;; clear space
                                                          (reset! last-v (read-string v))))) ;; remember last correct value
                                                          
                                                    (do ;; if value is integer
                                                      (reset! last-v (read-string v)))))))))) ;; remember last correct value if integer and when remove last char
                                                      
                                      (if display (config! display :text @last-v)))]
             style))))

(def view (fn [] (let [lbl (label)]
                   (mig-panel :constraints ["" "fill, grow" ""] :border (line-border :thickness 1 :color "#000") :size [200 :by 30] 
                              :items [[(input-number :display lbl)]
                                      [lbl]]))))

;; Show example
;; (let [my-frame (-> (doto (seesaw.core/frame
;;                           :title "test"
;;                           :size [0 :by 0]
;;                           :content (view))
;;                      (.setLocationRelativeTo nil) seesaw.core/pack! seesaw.core/show!))]
;;   (seesaw.core/config! my-frame :size [800 :by 600]))
