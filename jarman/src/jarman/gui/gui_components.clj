(ns jarman.gui.gui-components
  (:use seesaw.dev
        seesaw.mig)
  (:require [jarman.resource-lib.icon-library :as icon]
            [seesaw.core :as c]
            [seesaw.border :as b]
            [seesaw.util :as u]
            [seesaw.mig :as smig]
            [seesaw.rsyntax]
            [jarman.tools.swing :as stool]
            [jarman.logic.state :as state]
            [jarman.tools.lang :refer :all]
            [jarman.logic.metadata :as mt]
            [jarman.config.config-manager :as cm]
            [jarman.gui.gui-tools :as gtool]
            [seesaw.chooser :as chooser]
            [jarman.gui.gui-tutorials.key-dispacher-tutorial :as key-tut])
  (:import (java.awt Color)
           (jarman.test CustomScrollBar)))

(jarman.config.config-manager/swapp)

;; ┌────────────────────�
;; │                    │
;; │ Basic components   │
;; │                    │________________________________________
;; └────────────────────�                                     

(def dark-grey-color "#676d71")
(def blue-color "#256599")
(def light-light-grey-color "#e1e1e1")
(def light-grey-color "#82959f")
(def blue-green-color "#2c7375")
(def light-blue-color "#96c1ea")
(def red-color "#f01159")
(def back-color "#c5d3dd")

(defn hr
  ([line-size] (c/label :border (b/empty-border :top line-size)))
  ([line-size line-color] (c/label :border (b/line-border :top line-size :color line-color)))
  ([line-size line-color offset]
   (let [[l t r b] offset]
     (c/label :border (b/compound-border
                       (b/line-border :top line-size :color line-color)
                       (b/empty-border :left l :right r :top t :bottom b))))))

(defn fake-focus
  [& {:keys [args
             vgap
             hgap]
      :or {args []
           vgap 0
           hgap 0}}]
  (apply 
   c/label 
   :focusable? true
   :border (b/line-border :left hgap :top vgap)
   :listen [:focus-gained (fn [e] (c/config! e :border (b/line-border :left hgap :top vgap :color (gtool/get-comp :fake-focus :background-focus))))
            :focus-lost   (fn [e] (c/config! e :border (b/line-border :left hgap :top vgap :color (gtool/get-comp :fake-focus :background-unfocus))))]
   args))

(defn auto-scrollbox
  [component & args]
  (let [scr (apply c/scrollable component :border nil args)
        scr (c/config! scr :listen [:property-change
                                  (fn [e] (c/invoke-later (try
                                                          (let [get-root (fn [e] (.getParent (.getParent (.getParent (.getSource e)))))
                                                                vbar 40
                                                                w (- (.getWidth (get-root e)) vbar)
                                                                h (+ 50 vbar (.getHeight (c/config e :preferred-size)))]
                                                            (c/config! component :size [w :by h]))
                                                          (catch Exception e (str "Auto scroll cannot get parent")))))])]
    (.setUnitIncrement (.getVerticalScrollBar scr) 20)
    (.setPreferredSize (.getVerticalScrollBar scr) (java.awt.Dimension. 12 0))
    (.setUnitIncrement (.getHorizontalScrollBar scr) 20)
    (.setPreferredSize (.getHorizontalScrollBar scr) (java.awt.Dimension. 0 12))
    scr))


(defn vmig
  "Description:
     Use vmig for quick vertical layout or container.
   Example:
    (vmig :items [[component]])
  "
  [& {:keys [items
             wrap
             lgap
             rgap
             tgap
             bgap
             vrules
             hrules
             debug
             args]
      :or {items [[(c/label)]]
           wrap 1
           lgap 0
           rgap 0
           tgap 0
           bgap 0
           vrules "[grow, fill]"
           hrules "[grow, fill]"
           debug [0 "#f00"]
           args []}}]
  (apply mig-panel
         :constraints [(str "wrap " wrap) (str lgap "px" hrules rgap "px") (str tgap "px" vrules bgap "px")]
         :items items
         :border (b/line-border :thickness (first debug) :color (second debug))
         args))

(defn hmig
  [& {:keys [items
             wrap
             lgap
             rgap
             tgap
             bgap
             vrules
             hrules
             debug
             args]
      :or {items [[(c/label)]]
           wrap ""
           lgap 0
           rgap 0
           tgap 0
           bgap 0
           vrules "[grow, fill]"
           hrules "[grow, fill]"
           debug [0 "#f00"]
           args []}}]
  (apply mig-panel
         :constraints [wrap (str lgap "px" hrules rgap "px") (str tgap "px" vrules bgap "px")]
         :items items
         :border (b/line-border :thickness (first debug) :color (second debug))
         args))

(defn scrollbox
  [component
   & {:keys [args
             hbar-size
             vbar-size]
      :or {args []
           hbar-size 12
           vbar-size 12}}]

  (let [scr (apply c/scrollable component :border nil args)]  ;; speed up scrolling
    (.setUnitIncrement (.getVerticalScrollBar scr) 20)
    (.setBorder scr nil)
;;    for hide scroll    
    (.setUnitIncrement (.getVerticalScrollBar scr) 20)
    (.setUnitIncrement (.getHorizontalScrollBar scr) 20)
    (.setPreferredSize (.getVerticalScrollBar scr) (java.awt.Dimension. vbar-size 0))
    (.setPreferredSize (.getHorizontalScrollBar scr) (java.awt.Dimension. 0 hbar-size))
    scr))


(defn min-scrollbox
  "Description
    this func get some pane (like mig-panel etc..)
    return scrollable pane, args (some options to scroll-pane)
   Example
    (min-scrollbox (mig-panel ...) :hscroll :never)
    ;; => #object[seesaw.core.proxy$javax.swing"
  [component
   & args]
  (let [args (apply hash-map args)
        scr (CustomScrollBar/myScrollPane component
             ;; (mig-panel :constraints ["" "0px[grow, fill]8px" "0px[]0px"]
             ;;                                         :background (.getBackground component)
             ;;                                         :items [[component]])
             )
        get-key (fn [x] (first (first x)))
        get-val (fn [x] (second (first x)))] 
    (if-not (nil? args)
       ((fn next [sm]
          (if (empty? sm)                   
            scr
            (do 
              (c/config! scr (get-key sm) (get-val sm))
              (next (map-rest sm)))))
        args))
    (.setBorder scr nil)
    (.setUnitIncrement (.getVerticalScrollBar scr) 20)
    (.setUnitIncrement (.getHorizontalScrollBar scr) 20)
    scr))

(defn header-basic
  [title
   & {:keys [foreground
             background
             border-size
             border-color
             border
             underline-color
             underline-size
             font-size
             font-style
             font
             args]
      :or {foreground (gtool/get-comp :header-basic :foreground)
           background (gtool/get-comp :header-basic :background)
           border-color (gtool/get-comp :header-basic :border-color)
           border-size (gtool/get-comp :header-basic :border-size)
           underline-color (gtool/get-comp :header-basic :underline-color)
           underline-size (gtool/get-comp :header-basic :underline-size)
           border (fn [size color usize ucolor] (b/compound-border  (b/line-border :thickness size :color color) (b/line-border :bottom usize :color ucolor)))
           font-size (gtool/get-comp :header-basic :font-size)
           font-style (keyword (first (gtool/get-comp :header-basic :font-style)))
           font (fn [size style] (gtool/getFont size style))
           args []}}]
  (apply c/label
         :text title
         :font (font font-size font-style)
         :foreground foreground
         :background background
         :border (border border-size border-color underline-size underline-color)
         args))


(defmacro textarea
  "Description:
     TextArea with word wrap using html in label
  Example:
     (textarea \"Some loooong text\" :border (line-border :thickness 1 :color \"#a23\"))
  "
  [txt & args] `(c/label :text `~(gtool/htmling ~txt) ~@args))


(defn multiline-text
  "Description:
     Textarea with auto resize, word wrap and enabled selected by mouse.
  Example:
     (dynamic-text-area {:text \"Some loooong text\"})
  "
  [{:keys [text]}]
  (let [text-area (c/styled-text
                  :text text
                  :editable? false
                  :wrap-lines? true
                  :opaque? false)]
    text-area))
;;
;; Example of multiline-text TOTRY
;;
;; Run app and eval
;; (popup-window
;;  {:window-title "Popup multiline-text example"
;;   :size [300 200]
;;     :view (multiline-text
;;            {:text "Ogólnie znana teza głosi, iż użytkownika może rozpraszać zrozumiała zawartość strony, kiedy ten chce zobaczyć sam jej wygląd."})})


(defn button-basic
  "Description:
      Simple button with default style.
   Example:
      (simple-button \"Simple button\" :onClick (fn [e]) :style [:background \"#fff\"])"
  [txt & {:keys [onClick
                 args
                 tgap
                 bgap
                 lgap
                 rgap
                 halign
                 mouse-in
                 underline-size
                 mouse-out
                 focus-color
                 unfocus-color
                 flip-border
                 font]
          :or   {onClick (fn [e] (println "Click"))
                 args []
                 tgap 10
                 bgap 10
                 lgap 10
                 rgap 10
                 underline-size 2
                 halign :center
                 mouse-in  (gtool/get-color :background :button_hover_light)
                 mouse-out (gtool/get-color :background :button_main)
                 focus-color (gtool/get-color :decorate :focus-gained)
                 unfocus-color (gtool/get-color :decorate :focus-lost)
                 flip-border false
                 font (gtool/getFont 12)}}]
  (let [newBorder (fn [underline-color]
                    (b/compound-border (b/empty-border :bottom bgap :top tgap :left lgap :right rgap)
                                       (b/line-border (if flip-border :top :bottom) underline-size :color underline-color)))]
    (apply c/label
           :text txt
           :focusable? true
           :halign halign
           :font font
           :listen [:mouse-clicked (fn [e] (do (onClick e) (gtool/switch-focus)))
                    :mouse-entered (fn [e] (c/config! e :border (newBorder focus-color)   :background mouse-in  :cursor :hand))
                    :mouse-exited  (fn [e] (c/config! e :border (newBorder unfocus-color) :background mouse-out))
                    :focus-gained  (fn [e] (c/config! e :border (newBorder focus-color)   :background mouse-in  :cursor :hand))
                    :focus-lost    (fn [e] (c/config! e :border (newBorder unfocus-color) :background mouse-out))
                    :key-pressed   (fn [e] (if (= (.getKeyCode e) java.awt.event.KeyEvent/VK_ENTER) (do (onClick e) (gtool/switch-focus))))]
           :background mouse-out
           :border (newBorder unfocus-color)
           args)))


(defn button-slim
  [txt
   & {:keys [onClick 
             underline-size
             args]
      :or {onClick (fn [e])
           underline-size 0
           args []}}]
  (button-basic
   txt
   :onClick onClick
   :tgap 5
   :bgap 5
   :underline-size underline-size
   :halign :left
   :args args))


;; (defn button-export
;;   [txt onClick]
;;   (button-basic txt 
;;                 :onClick onClick
;;                 :mouse-in (gtool/get-comp :button-export :mouse-in)
;;                 :mouse-out (gtool/get-comp :button-export :mouse-out)
;;                 :unfocus-color (gtool/get-comp :button-export :unfocus-color)))

(defn button-return
  [txt func]
  (button-basic txt 
                :onClick func
                :mouse-in (gtool/get-comp :button-return :mouse-in)
                :mouse-out (gtool/get-comp :button-return :mouse-out)
                :unfocus-color (gtool/get-comp :button-return :unfocus-color)
                :args [:foreground (gtool/get-comp :button-return :foreground)]))



(defn button-icon
  [{:keys [icon-on icon-off size func tip margin frame-hover]
    :or   {icon-on icon/question-64-png
           size 30
           func (fn [e])
           margin [0 0 0 0]
           frame-hover true}}]
  (let [ico-off (rift icon-off icon-on)
        ico-fn (fn [i](jarman.tools.swing/image-scale i size))
        [t b l r] margin
        border-fn (fn [frame-color] (b/compound-border
                                         (b/empty-border :top t :bottom b :left l :right r)
                                         (if frame-hover (b/line-border  :left 2 :right 2 :color frame-color))))]
    (c/label
     :icon (ico-fn icon-off)
     :tip tip
     :border (border-fn (gtool/opacity-color))
     :listen [:mouse-entered (fn [e]
                               (gtool/hand-hover-on e)
                               (c/config! e
                                          :icon (ico-fn icon-on)
                                          :border (border-fn "#ddd")))
              :mouse-exited  (fn [e]
                               (c/config! e
                                          :icon (ico-fn ico-off)
                                          :border (border-fn (gtool/opacity-color))))
              :mouse-clicked func])))


(defn icon-bar
  "Description

  Example
    (gcomp/icon-bar
     :size 40
     :align :right
     :margin [5 0 10 10]
     :items icos)"
  [& {:keys [items size align margin]
      :or {size 30
           align :left
           margin [0 0 0 0]}}]
  (let [comps (doall
               (map (fn [{icon-on  :icon-on
                          icon-off :icon-off
                          tip      :tip
                          func     :func}]
                      (let [ico-off (rift icon-off icon-on)]
                        (button-icon {:icon-on  icon-on
                                      :icon-off ico-off
                                      :tip  tip
                                      :func func
                                      :size size
                                      :margin margin})))
                    items))
        align-m {:left    ["[fill]"
                           #(gtool/join-mig-items %)]
                 :right   ["[grow]0px[fill]"
                           #(gtool/join-mig-items (c/label) %)]
                 :center  ["[grow]0px[fill]0px[grow]"
                           #(gtool/join-mig-items
                             (c/label)
                             (gtool/join-mig-items %)
                             (c/label))]
                 :between ["[grow, center]"
                           #(gtool/join-mig-items %)]}
        [hrules comps-fn] (align align-m)]
    (hmig
     :hrules hrules
     :items (comps-fn comps))))



(defn input-text
  "Description:
    Text component converted to c/text component with placeholder. Placehlder will be default value.
   Params:
    val
    placeholder
    border
    font-size
    border-color-focus
    border-color-unfocus
    char-limit
    args
   Example:
    (input-text :placeholder \"Login\" :style [:halign :center])"
  [& {:keys [val
             placeholder
             border
             font-size
             border-color-focus
             border-color-unfocus
             char-limit
             args]
      :or {val ""
           placeholder ""
           font-size 14
           border-color-focus   (gtool/get-color :decorate :focus-gained)
           border-color-unfocus (gtool/get-color :decorate :focus-lost)
           border [10 10 5 5 2]
           char-limit 0
           args []}}]
  (let [fn-get-data     (fn [e key] (get-in (c/config e :user-data) [key]))
        fn-assoc        (fn [e key val] (assoc-in (c/config e :user-data) [key] val))
        newBorder (fn [underline-color]
                    (b/compound-border (b/empty-border :left (nth border 0) :right (nth border 1) :top (nth border 2) :bottom (nth border 3))
                                       (b/line-border :bottom (nth border 4) :color underline-color)))
        last-v (atom "")]
    (apply c/text
           :text (if (empty? val) placeholder (if (string? val) val (str val)))
           :font (gtool/getFont font-size :name "Monospaced")
           :background (gtool/get-color :background :input)
           :border (newBorder border-color-unfocus)
           :user-data {:placeholder placeholder :value "" :edit? false :type :input :border-fn newBorder}
           :listen [:focus-gained (fn [e]
                                    (c/config! e :border (newBorder border-color-focus))
                                    (cond (= (c/value e) placeholder) (c/config! e :text ""))
                                    (c/config! e :user-data (fn-assoc e :edit? true)))
                    :focus-lost   (fn [e]
                                    (c/config! e :border (newBorder border-color-unfocus))
                                    (cond (= (c/value e) "") (c/config! e :text placeholder))
                                    (c/config! e :user-data (fn-assoc e :edit? false)))
                    :caret-update (fn [e]
                                    (let [new-v (c/value (c/to-widget e))]
                                      (if (and (> (count new-v) char-limit) (< 0 char-limit))
                                        (c/invoke-later (c/config! e :text @last-v))
                                        (reset! last-v new-v))))]
           args)))

(defn state-input-text
  "Description:
    Text input for state architecture. Need fn in action to changing state.
   Example:
    (state-input-text {:func (fn [e] (dispatch! {...})) :val \"some value\" }"
  [{func :func
    val  :val}
   & {:keys [font-size
             border-color-focus
             border-color-unfocus
             border
             enabled?
             editable?]
      :or {font-size 14
           border-color-focus   (gtool/get-color :decorate :focus-gained)
           border-color-unfocus (gtool/get-color :decorate :focus-lost)
           border [10 10 5 5 2]
           enabled? true
           editable? true}}]
  (let [newBorder (fn [underline-color]
                    (b/compound-border (b/empty-border :left (nth border 0)
                                                       :right (nth border 1)
                                                       :top (nth border 2)
                                                       :bottom (nth border 3))
                                       (b/line-border :bottom (nth border 4)
                                                      :color underline-color)))]
    (c/text
     :text (str (rift val ""))
     :font (gtool/getFont font-size :name "Monospaced")
     :background (gtool/get-color :background :input)
     :border (newBorder border-color-unfocus)
     :enabled? enabled?
     :editable? editable?
     :listen [:focus-gained (fn [e] (c/config! e :border (newBorder border-color-focus)))
              :focus-lost   (fn [e] (c/config! e :border (newBorder border-color-unfocus)))
              :caret-update func])))

(defn input-checkbox
  [& {:keys [txt
             val
             font-size
             enabled?
             local-changes
             store-id
             args]
      :or   {txt ""
             selected? false
             font-size 14
             enabled? true
             local-changes (atom {})
             store-id :none
             args []}}]
  (apply c/checkbox
         :text txt
         :font (gtool/getFont font-size :name "Monospaced")
         :selected? val
         :enabled? enabled?
         :border (b/empty-border :top 15)
         :listen [:item-state-changed (fn [e]
                                        (let [new-v (c/config e :selected?)]
                                          (cond
                                            (and (not (nil? store-id))
                                                 (not (= val new-v)))
                                            (swap! local-changes (fn [storage] (assoc storage store-id new-v)))
                                            :else (reset! local-changes (dissoc @local-changes store-id)))))]
         args))


;; (def view (fn [] (let [lbl (c/label)]
;;                    (mig-panel :constraints ["" "fill, grow" ""] :border (b/line-border :thickness 1 :color "#000") :size [200 :by 30]
;;                               :items [[(input-checkbox)]]))))

;; ;; Show example
;; (let [my-frame (-> (doto (seesaw.core/frame
;;                           :title "test"
;;                           :size [0 :by 0]
;;                           :content (view))
;;                      (.setLocationRelativeTo nil) seesaw.core/pack! seesaw.core/show!))]
;;   (seesaw.core/config! my-frame :size [800 :by 600]))



;; (show-events (c/text))

;; (show-options (c/text))

;; (defn inpose-label
;;   "Params: 
;;      title
;;      component
;;    "
;;   [title component
;;    & {:keys [tgap
;;              font-color
;;              id]
;;       :or {tgap 0
;;            font-color "#000"
;;            id nil}}] 
;;   (c/grid-panel :columns 1
;;                 :id id
;;                 :border (b/empty-border :top tgap)
;;                 :items [(c/label :text (if (string? title) title "")
;;                                  :font (gtool/getFont 13)
;;                                  :foreground font-color )
;;                         component]))

(defn inpose-label
  "Params: 
     title
     component
   "
  [title component
   & {:keys [font-color]
      :or {font-color "#000"}}] 
  (seesaw.mig/mig-panel :constraints ["wrap 1" "0px[fill, grow]0px" "5px[]5px"]
                        :items [[(c/label :text title
                                          :font (gtool/getFont 13)
                                          :foreground font-color ) "align l"]
                                [component]]))


(defn input-text-area
  "Description:
    Text component converted to c/text component with placeholder. Placehlder will be default value.
 Example:
    (input-text :placeholder \"Login\" :style [:halign :center])"
  [{:keys [store-id
           local-changes
           val
           border
           border-color-focus
           border-color-unfocus
           char-limit]
    :or {store-id nil
         local-changes (atom {})
         val ""
         border [10 10 5 5 2]
         border-color-focus   (gtool/get-color :decorate :focus-gained)
         border-color-unfocus (gtool/get-color :decorate :focus-lost)
         char-limit 0}}]
  (let [newBorder (fn [underline-color]
                    (b/compound-border (b/empty-border :left (nth border 0) :right (nth border 1) :top (nth border 2) :bottom (nth border 3))
                                       (b/line-border :bottom (nth border 4) :color underline-color)))
        last-v (atom "")]
    (let [text-area (c/to-widget (javax.swing.JTextArea.))]
      (if-not (empty? val) (swap! local-changes (fn [storage] (assoc storage store-id val))))
      (c/config!
       text-area
       :text (if (empty? val) "" (str val))
       :minimum-size [50 :by 100]
       :border (newBorder border-color-unfocus)
       :user-data {:border-fn newBorder}
       :listen [:focus-gained (fn [e]
                                (c/config! e :border (newBorder border-color-focus)))
                :focus-lost   (fn [e]
                                (c/config! e :border (newBorder border-color-unfocus)))
                :caret-update (fn [e]
                                (let [new-v (c/value (c/to-widget e))]
                                  (if (and (> (count new-v) char-limit) (< 0 char-limit))
                                    (c/invoke-later (c/config! e :text @last-v))
                                    (reset! last-v new-v))
                                  (cond
                                    (and (not (nil? store-id)) (not (= val new-v)))
                                    (swap! local-changes (fn [storage] (assoc storage store-id new-v)))
                                    :else (reset! local-changes (dissoc @local-changes store-id)))))])
      (scrollbox text-area :minimum-size [50 :by 100]))))

(defn state-input-text-area
  "Description:
    Text area with state interaction.
 Example:
    (state-input-text-area {:func dispatch-func :val val})"
  [{func :func
    val  :val}
   & {:keys [border
             border-color-focus
             border-color-unfocus]
      :or {border [10 10 5 5 2]
           border-color-focus   (gtool/get-color :decorate :focus-gained)
           border-color-unfocus (gtool/get-color :decorate :focus-lost)}}]
  (let [newBorder (fn [underline-color]
                    (b/compound-border (b/empty-border :left (nth border 0) :right (nth border 1) :top (nth border 2) :bottom (nth border 3))
                                       (b/line-border :bottom (nth border 4) :color underline-color)))]
    (let [text-area (c/to-widget (javax.swing.JTextArea.))]
      (c/config!
       text-area
       :text (rift val "")
       :minimum-size [50 :by 100]
       :border (newBorder border-color-unfocus)
       :listen [:focus-gained (fn [e]
                                (c/config! e :border (newBorder border-color-focus)))
                :focus-lost   (fn [e]
                                (c/config! e :border (newBorder border-color-unfocus)))
                :caret-update func])
      (func text-area)
      (scrollbox text-area :minimum-size [50 :by 100]))))


(defn input-text-area-label
  [& {:keys [title
             store-id
             local-changes
             val] 
      :or {title "None"
           store-id :none
           local-changes (atom {})
           val ""}}]
  (inpose-label title (input-text-area {:store-id store-id :local-changes local-changes :val val})))

(defn input-text-with-atom
  [{:keys [store-id local-changes val editable? enabled? store-orginal onClick border-color-focus border-color-unfocus debug]
    :or {local-changes (atom {})
         store-id nil
         val ""
         editable? true
         enabled? true
         store-orginal false
         border-color-focus   (gtool/get-color :decorate :focus-gained)
         border-color-unfocus (gtool/get-color :decorate :focus-lost)
         onClick (fn [e])
         debug false}}]
  (swap! local-changes (fn [storage] (assoc storage store-id val)))
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
                                                           (gtool/hand-hover-on e)
                                                           (c/config! e)))
                   :caret-update (fn [e] (swap! local-changes (fn [storage] (assoc storage store-id (c/value (c/to-widget e)))))
                                   ;; (println "\nnput Store ID" store-id val)
                                  ;;  (let [new-v (c/value (c/to-widget e))]
                                  ;;    (cond
                                  ;;      (and (not (nil? store-id))
                                  ;;           (not (= val new-v)))
                                  ;;      (swap! local-changes (fn [storage] (assoc storage store-id new-v)))
                                  ;;      (not store-orginal) (reset! local-changes (dissoc @local-changes store-id))))
                                   )
                   :focus-gained (fn [e] (c/config! e :border ((gtool/get-user-data e :border-fn) border-color-focus)))
                   :focus-lost   (fn [e] (c/config! e :border ((gtool/get-user-data e :border-fn) border-color-unfocus)))]]))



(defn select-box
  "Description
      Set model and you can add extending parameter.
   Params:
      store-id 
      local-changes
      selected-item
      editable? 
      enabled?
   Example:
      (select-box [one two tree])"
  ([model
    & {:keys [store-id
              local-changes
              selected-item
              editable?
              enabled?
              always-set-changes]
       :or   {local-changes (atom {})
              selected-item ""
              editable? true
              enabled? true
              store-id nil
              always-set-changes true}}]
   
   (let [combo-model (if (empty? selected-item) (cons selected-item model) (join-vec [selected-item] (filter #(not= selected-item %) model)))]
    (if (and (= always-set-changes true) (not (empty? selected-item)))
     (do
       (swap! local-changes (fn [storage] (assoc storage store-id combo-model)))))
   (c/combobox :model combo-model
               :font (gtool/getFont 14)
               :enabled? enabled?
               :editable? editable?
               :background (gtool/get-color :background :combobox)
               :listen [:item-state-changed (fn [e] ;;(let [choosed (c/config e :selected-item)])
                                              (let [new-v (c/config e :selected-item)]
                                                ;; (println "new c/value" new-v)
                                                (cond
                                                  (and (not (nil? store-id))
                                                       (or
                                                        (= always-set-changes true)
                                                        (not (= (first @local-changes) new-v))))
                                                  (do
                                                    (let [new-model (into [new-v] (filter #(not (= new-v %)) model))]
                                                      (swap! local-changes (fn [storage] (assoc storage store-id new-model)))))
                                                  (= always-set-changes false)
                                                  (do
                                                    (reset! local-changes (dissoc @local-changes store-id))))))]))))



(defn state-combo-box
  "Description
      Component using state logic so set some fn for state update,
      vector with value and vector with representation in combo-box.
      :start-fn can invoke function before created component.
   Optional params:
      editable? 
      enabled?
   Example:
      (state-select-box
        (fn [e selected new-model] {dispach! {:action ...}})
        [1 2]
        [\"One\" \"Two\"])
        :start-fn (fn [] (...))"
  [func model-v repres-v
   & {:keys [start-v
             editable?
             enabled?]
      :or   {start-v   (first model-v)
             editable? true
             enabled?  true}}]
  (let [model-m (into {} (doall
                          (map (fn [to-k v] {(keyword to-k) v})
                               repres-v model-v)))]
    (c/combobox :model repres-v
                :font (gtool/getFont 14)
                :enabled? enabled?
                :editable? editable?
                :background (gtool/get-color :background :combobox)
                :selected-item start-v
                :listen [:item-state-changed
                         (fn [e]
                           (let [selected-repres (c/config e :selected-item)
                                 selected (get model-m (keyword selected-repres))
                                 without-selected (filter #(not (= selected %)) model-v)
                                 new-model (into [selected] without-selected)]
                             (func selected)))])))


(defn state-table-list
  [{dispatch! :dispatch!
    action    :action
    path      :path
    val       :val}]
  (let [func (fn [selected]
               (dispatch! {:action action
                           :path   path
                           :value  selected}))
        model-v (vec (map #(get % :table_name) (jarman.logic.metadata/getset)))
        repres-v model-v
        start-v (rift val (first model-v))]
    (dispatch! {:action action
                :path   path
                :value  start-v})
    (state-combo-box func model-v repres-v :start-v start-v)))



(defn expand-form-panel
  "Description:
     Create panel who can hide inside components. 
     Inside :user-data is function to hide/show panel ((get (config e :user-data) :hide-show))
   Example:
     (expand-form-panel parent (component or components))"
  [view-layout comps
   & {:keys [args 
             min-w
             w
             max-w
             lgap
             rgap
             tgap
             bgap
             vrules
             icon-open
             icon-hide
             text-open
             text-hide
             focus-color
             unfocus-color]
      :or {args []
           min-w 250
           w 250
           max-w 250
           lgap 0
           rgap 0
           tgap 0
           bgap 0
           vrules "[fill]"
           icon-open nil
           icon-hide nil
           text-open "back"
           text-hide "..."
           focus-color (gtool/get-color :decorate :focus-gained-dark)
           unfocus-color "#fff"}}]
  (let [hidden-comp (atom nil)
        min-w (if (< max-w min-w) max-w min-w)
        w (if (< max-w w) max-w min-w)
        hsize (str lgap "px[" min-w ":" w ":" max-w ", grow,fill]" rgap "px")
        ;; hsize (if (< max-w w) (str lgap "px[" min-w ":" w ":" w ", grow, fill]" rgap "px") (str lgap "px[" min-w ":" w ":" max-w ", grow,fill]" rgap "px"))
        form-space-open ["wrap 1" hsize (str tgap "px[fill]0px" vrules bgap "px")]
        form-space-hide ["" "0px[grow, fill]0px" "0px[grow, fill]0px"]
        form-space (apply smig/mig-panel :constraints form-space-open args)
        onClick (fn [e]
                  (let [inside (u/children form-space)]
                    (if (nil? @hidden-comp)
                      (do
                        (c/config! form-space :constraints form-space-hide)
                        (c/config! e :text text-hide :valign :top :halign :center :icon nil :font (gtool/getFont 18 :bold) :background "#eee")
                        (reset! hidden-comp (drop 1 inside))
                        (doall (map #(.remove form-space %) (reverse (drop 1 (range (count inside))))))
                        (.revalidate view-layout))
                      (do
                        (c/config! form-space :constraints form-space-open)
                        (c/config! e :text text-open :halign :left :font (gtool/getFont 15) :background "#fff")
                        (doall (map #(.add form-space %) @hidden-comp))
                        (reset! hidden-comp nil)
                        (.revalidate view-layout)))))
        hide-show (c/label :text text-open
                           :icon icon-open
                           :focusable? true
                           :background "#fff" ;;
                           :foreground blue-color ;;"#bbb"
                           :font (gtool/getFont 15)
                           :border (b/empty-border :left 2 :right 2)
                           :listen [:focus-gained (fn [e] (c/config! e :foreground focus-color))
                                    :focus-lost   (fn [e] (c/config! e :foreground unfocus-color))
                                    :mouse-entered gtool/hand-hover-on
                                    :mouse-clicked onClick
                                    :key-pressed  (fn [e] (if (= (.getKeyCode e) java.awt.event.KeyEvent/VK_ENTER) (onClick e)))])]
    (c/config! form-space :user-data {:hide-show (fn [](onClick hide-show))})
    (c/config! form-space :items (gtool/join-mig-items hide-show comps))))


(def input-password
  "Description:
    Text component converted to password input component, placeholder is default value.
 Example:
    ((def input-password :placeholder \"Password\" :style [:halign :center])"
  (fn [& {:keys [placeholder
                 border
                 font-size
                 border-color-focus
                 border-color-unfocus
                 style]
          :or   {placeholder "Password"
                 font-size 14
                 border-color-focus   (gtool/get-color :decorate :focus-gained)
                 border-color-unfocus (gtool/get-color :decorate :focus-lost)
                 border [10 10 5 5 2]
                 style []}}]
    (let [allow-clean (atom false)
          fn-letter-count (fn [e] (count (c/value e)))
          fn-hide-chars   (fn [e] (apply str (repeat (fn-letter-count e) "*")))
          fn-get-data     (fn [e key] (get-in (c/config e :user-data) [key]))
          fn-assoc        (fn [e key v] (assoc-in (c/config e :user-data) [key] v))
          newBorder       (fn [underline-color]
                            (b/compound-border (b/empty-border :left (nth border 0) :right (nth border 1) :top (nth border 2) :bottom (nth border 3))
                                             (b/line-border :bottom (nth border 4) :color underline-color)))]
      (apply c/text
             :text placeholder
             :font (gtool/getFont font-size :name "Monospaced")
             :background (gtool/get-color :background :input)
             :border (newBorder border-color-unfocus)
             :user-data {:placeholder placeholder :value "" :edit? false :type :password}
             :listen [:focus-gained (fn [e]
                                      (c/config! e :border (newBorder border-color-focus))
                                      (cond (= (fn-get-data e :value) "")    (c/config! e :text ""))
                                      (c/config! e :user-data (fn-assoc e :edit? true)))
                      :focus-lost   (fn [e]
                                      (c/config! e :border (newBorder border-color-unfocus))
                                      (cond (= (c/value e) "") (c/config! e :text placeholder))
                                      (c/config! e :user-data (fn-assoc e :edit? false)))
                      :caret-update (fn [e]
                                      (cond (and (= (fn-get-data e :edit?) true)
                                                 (not (= (c/value e) placeholder)))
                                            (cond (< 0 (count (c/value e)))
                                                  (let [added-chars (clojure.string/replace (c/value e) #"\*+" "")]
                                                    (cond (> (count added-chars) 0)
                                                          (do
                                                            (c/config! e :user-data (fn-assoc e :value (str (fn-get-data e :value) added-chars)))
                                                            (c/invoke-later (c/config! e :text (fn-hide-chars e))))
                                                          (< (fn-letter-count e) (count (fn-get-data e :value)))
                                                          (do
                                                            (c/config! e :user-data (fn-assoc e :value (subs (fn-get-data e :value) 0 (fn-letter-count e))))
                                                            (if (= 1 (count (c/value e))) (reset! allow-clean true))
                                                            (c/invoke-later (c/config! e :text (fn-hide-chars e))))))

                                                  (and (= true @allow-clean)
                                                       (= 0 (count (c/value e))))
                                                  (do
                                                    (c/config! e :user-data (fn-assoc e :value ""))
                                                    (c/invoke-later (c/config! e :text ""))
                                                    (reset! allow-clean false)))))]
             style))))

;; (def pass (input-password :placeholder "password"))

;; (def view (fn [] (let [lbl (c/label)]
;;                    (mig-panel :constraints ["" "fill, grow" ""] :border (b/line-border :thickness 1 :color "#000") :size [200 :by 30]
;;                               :items [[pass]]))))

;; ;; Show example
;; (let [my-frame (-> (doto (seesaw.core/frame
;;                           :title "test"
;;                           :size [0 :by 0]
;;                           :content (view))
;;                      (.setLocationRelativeTo nil) seesaw.core/pack! seesaw.core/show!))]
;;   (seesaw.core/config! my-frame :size [800 :by 600]))

;; (println (c/config pass :user-data))

;; ┌────────────────────┐
;; │                    │
;; │ Expand buttons     │
;; │                    │________________________________________
;; └────────────────────┘                                       

(def button-expand
  "Description:
      It's a space for main button with more option. 
      Normal state is one button but after on click 
      space will be bigger and another buttons will be added.
      If button don't have any function, can not be expanded.
   Example:
      (button-expand 'Button name' (Element or Component))
      (button-expand 'Profile' (button 'Do something'))
      (button-expand 'Settings' (list (button 'Do something') (button 'Do something else')))
   Needed:
      Import jarman.dev-tools
      Function need stool/image-scale function for scalling icon"
  (fn [txt inside-btns
       & {:keys [expand
                 border
                 vsize
                 min-height
                 ico
                 ico-hover
                 id
                 onClick
                 over-func
                 background]
          :or {expand :auto
               border (b/compound-border (b/empty-border :left 6))
               vsize 35
               min-height 200
               ico  (stool/image-scale icon/plus-64-png 25)
               ico-hover (stool/image-scale icon/minus-grey-64-png 20)
               id :none
               onClick nil
               over-func nil
               background (gtool/get-comp :button-expand :background)}}]
    (let [atom-inside-btns (atom nil)
          inside-btns (if (nil? inside-btns) nil inside-btns) ;; check if nill
          inside-btns (if (seqable? inside-btns) inside-btns (list inside-btns)) ;; check if not in list
          inside-btns (if (sequential? (first inside-btns)) (first inside-btns) inside-btns) ;; check if list in list
          ico (if (or (= :always expand) (not (nil? inside-btns))) ico nil)
          title (c/label
                 :border (b/empty-border :left 10)
                 :text txt
                 :background (Color. 0 0 0 0))
          listen (fn [func] [:mouse-entered gtool/hand-hover-on
                             :mouse-clicked (fn [e] (func e))
                             :focus-gained  (fn [e] (c/config! e :background (gtool/get-comp :button-expand :background-hover)))
                             :focus-lost    (fn [e] (c/config! e :background (gtool/get-comp :button-expand :background)))
                             :key-pressed   (fn [e] (if (= (.getKeyCode e) java.awt.event.KeyEvent/VK_ENTER) (func e)))])
          icon (c/label
                :size [vsize :by vsize]
                :halign :center
                :background (Color. 0 0 0 0)
                :icon ico)
          mig (mig-panel :constraints ["wrap 1" (str "0px[" min-height ":, fill]0px") "0px[fill]0px"] :background background)
          expand-btn (fn [func]
                       (c/config! title :listen (if (nil? over-func) (listen func) [:mouse-clicked over-func
                                                                                    :mouse-entered gtool/hand-hover-on]))
                       (c/config! icon :listen (listen func))
                       ;; (mig-panel
                       ;;  :constraints ["" (str "10px[grow, fill]0px[" vsize "]0px") "0px[fill]0px"]
                       ;;  :background background
                       ;;  :focusable? true
                       ;;  :border border
                       ;;  :items [[title]
                       ;;          [icon]])
                       (c/border-panel
                        :focusable? true
                        :background background
                        :border border
                        :size [min-height :by vsize]
                        :items [[title :west]
                                [icon :east]]))]
      (if (nil? onClick)
       (let [onClick (fn [e]
                       (if-not (nil? @atom-inside-btns)
                         (if (= (count (u/children mig)) 1)
                           (do ;;  Add inside buttons to mig with expand button
                             (c/config! icon :icon ico-hover)
                             (doall (map (fn [btn]
                                           (.add mig btn))
                                         @atom-inside-btns))
                             ;;(gtool/set-focus (first @atom-inside-btns))
                             ;;(gtool/switch-focus)
                             (.revalidate mig) 
                             (.repaint mig))
                           (do ;;  Remove inside buttons form mig without expand button
                             (c/config! icon :icon ico)
                             (doall (map #(.remove mig %) (reverse (drop 1 (range (count (u/children mig)))))))
                             (.revalidate mig)
                             (.repaint mig)))))]
         (do
           (reset! atom-inside-btns inside-btns)
           (c/config! mig
                      :id id
                      :user-data {:atom-expanded-items atom-inside-btns
                                  :title-fn (fn [new-title] (c/config! title :text new-title))}
                      :items [[(expand-btn onClick)]])))
       (c/config! mig :id id :items [[(expand-btn onClick)]])))))


(defn expand-input
  [{:keys [panel onClick title]
    :or {panel (seesaw.core/vertical-panel :items (list (c/label :text "heyy")))
         onClick (fn [e])}}]
  (button-expand (rift title "Enter")
                 panel
                 :min-height 220
                 :over-func onClick
                 :background "#dddddd"))

(def button-expand-child
  "Description
     Interactive button inside menu from expand button.
   Example:
     (button-expand-child \"Title\" {:onClick (fn [e]) :args [:border nil]})
   "
  (fn [title
       & {:keys [onClick
                 left
                 hover-color
                 width
                 args]
          :or {onClick (fn [e] (println "Clicked: " title))
               left 0
               hover-color "#eeefff"
               width 200 
               args []}}]
    (apply c/label :font (gtool/getFont)
           :text (str title)
           :background "#fff"
           :size  [width :by 25]
           :focusable? true
           :border (b/empty-border :left 10)
           :listen [:mouse-clicked (fn [e] (do (onClick e) (gtool/switch-focus)))
                    :mouse-entered (fn [e] (.requestFocus (c/to-widget e)))
                    :mouse-exited  (fn [e] (.requestFocus (c/to-root e)))
                    :focus-gained  (fn [e] (c/config! e :background ;; (gtool/get-comp :button-expand-child :background-hover)
                                                      hover-color))
                    :focus-lost    (fn [e] (c/config! e :background (gtool/get-comp :button-expand-child :background)))
                    :key-pressed   (fn [e] (if (= (.getKeyCode e) java.awt.event.KeyEvent/VK_ENTER) (do (onClick e) (gtool/switch-focus))))]
           args)))

;; ┌────────────────────┐
;; │                    │
;; │ Examples           │
;; │                    │________________________________________
;; └────────────────────┘                                       

;; BUTTON EXPAND
(def view (fn [] (button-expand "Expand"
                                (button-expand-child "Expanded")
                                (button-expand-child "Don't touch me." :onClick (fn [e] (c/config! (c/to-widget e) :text "FAQ 🖕( ͡° ᴗ ͡°)🖕" :font (gtool/getFont 14 :name "calibri")))))))

(def label-img
  (fn [file-path w h]
    (let [img (clojure.java.io/file (str "icons/imgs/" file-path))
          gif (c/label :text (str "<html> <img width=\"" w "\" height=\"" h "\" src=\"file:" img "\">"))]
      gif)))

(def egg (fn []
           (let [egg1 (c/label)
                 egg2 (c/label)]
             (c/flow-panel
              :items [egg1
                      (button-expand "Expand"
                                     (button-expand-child "Expanded")
                                     (button-expand-child "Don't touch me."
                                                          :onClick (fn [e]
                                                                     (c/config! egg1
                                                                              :text (c/value (label-img "egg.gif" 150 130)))
                                                                     (c/config! egg2
                                                                              :text (c/value (label-img "egg.gif" 150 130))))))
                      egg2]))))

(def view (fn [] (egg)))




;; ┌────────────────────┐
;; │                    │
;; │ Number  inputs     │
;; │                    │________________________________________
;; └────────────────────┘                                       


(def input-float
  "Description:
    Text component converted to automatic double number validator. Return only correct value.
    Implemented local-changes.
 Example:
    ((def input-number :style [:halign :center])
 "
  (fn [& {:keys [style
                 display
                 val
                 font-size
                 border-color-focus
                 border-color-unfocus
                 border
                 char-limit
                 local-changes
                 store-id]
          :or   {style []
                 display nil
                 val ""
                 font-size 14
                 border-color-focus   (gtool/get-color :decorate :focus-gained)
                 border-color-unfocus (gtool/get-color :decorate :focus-lost)
                 border [10 10 5 5 2]
                 char-limit 0
                 local-changes (atom {})
                 store-id nil}}]
    (let [newBorder (fn [underline-color]
                      (b/compound-border (b/empty-border :left (nth border 0) :right (nth border 1) :top (nth border 2) :bottom (nth border 3))
                                       (b/line-border :bottom (nth border 4) :color underline-color)))
          last-v (atom "")]
      (apply c/text
             :text val
             :background "#fff"
             :font (gtool/getFont font-size :name "Monospaced")
             :background (gtool/get-color :background :input)
             :border (newBorder border-color-unfocus)
             :listen [:focus-gained (fn [e] (c/config! e :border (newBorder border-color-focus)))
                      :focus-lost   (fn [e] (c/config! e :border (newBorder border-color-unfocus)))
                      :caret-update (fn [e]
                                      (.repaint (c/to-root (c/to-widget e)))
                                      (if (and (< char-limit (count (c/value e))) (> char-limit 0))
                                        (do
                                          (c/invoke-later (c/config! e :text @last-v)))
                                        (do
                                          (if (empty? (c/value e))
                                            (do ;; Set "" if :text is empty
                                              (c/config! e :text "")
                                              (reset! last-v ""))
                                            (do ;; if :text not empty 
                                              (let [v (re-matches #"^[+-]?([0-9]+([.][0-9]*)?|[.][0-9]+)$" (c/value e)) ;; Get only numbers vec or return nil
                                                    v (if-not (nil? v) (first v) nil)]
                                                (if (nil? v)
                                                  (do ;; if regex return nil then in :text is some c/text so change :text to ""
                                                    (if-not (and (= "-" (str (first (c/value e)))) (= 1 (count (c/value e))))
                                                      (c/invoke-later (c/config! e :text @last-v))))
                                                  (let [check-v (clojure.string/split v #"\.")] ;; split c/value by dot to check if double
                                                    (if (= (count check-v) 2) ;; if double
                                                      (if (empty? (first check-v))
                                                        (do ;; cat first empty c/value and return number 0.x
                                                          (c/invoke-later (c/config! e :text (read-string (str 0 (second check-v))))))
                                                        (do ;; remember last correct c/value
                                                          (reset! last-v (read-string v))))
                                                      (if (> (count (first check-v)) 1) ;; if double
                                                        (let [check-first-char (re-matches #"^[+-]?[1-9][0-9]*$" (first check-v))] ;; cut 0 on front
                                                          (if (and (nil? check-first-char) (not (= "-" (str (first (c/value e))))))
                                                            (do
                                                              (reset! last-v (clojure.string/join "" (clojure.string/split (first check-v) #"0"))) ;; remember last correct c/value
                                                              (c/invoke-later (c/config! e :text @last-v)))
                                                            (do ;; clear space
                                                              (reset! last-v (read-string v))))) ;; remember last correct c/value

                                                        (do ;; if c/value is integer
                                                          (reset! last-v (read-string v)))))))))))) ;; remember last correct c/value if integer and when remove last char

                                      (let [new-v @last-v] ;; local-changes
                                        (cond
                                          (and (not (nil? store-id))
                                               (not (= (str val) (str new-v))))
                                          (swap! local-changes (fn [storage] (assoc storage store-id new-v)))
                                          :else (reset! local-changes (dissoc @local-changes store-id))))

                                      (if display (c/config! display :text @last-v)))]
             style))))

(def input-int
  "Description:
    Text component converted to automatic int number validator. Return only correct value.
    Implemented local-changes.
 Params:
    style
    display
    val
    font-size
    border-color-focus
    border-color-unfocus
    border
    char-limit
    local-changes
    store-id
 Example:
    ((def input-number :style [:halign :center])"
  (fn [{:keys [style
               val
               font-size
               border-color-focus
               border-color-unfocus
               border
               char-limit
               local-changes
               store-id]
        :or   {style []
               val "0"
               font-size 14
               border-color-focus   (gtool/get-color :decorate :focus-gained)
               border-color-unfocus (gtool/get-color :decorate :focus-lost)
               border [10 10 5 5 2]
               char-limit 0
               local-changes (atom {})
               store-id nil}}]
    (let [newBorder (fn [underline-color]
                      (b/compound-border (b/empty-border :left (nth border 0) :right (nth border 1) :top (nth border 2) :bottom (nth border 3))
                                       (b/line-border :bottom (nth border 4) :color underline-color)))
          last-v (atom 0)]
      (apply c/text
             :text (if (nil? val) "0" val)
             :font (gtool/getFont font-size :name (gtool/get-font :regular))
             :background (gtool/get-color :background :input)
             :border (newBorder border-color-unfocus)
             :listen [:focus-gained (fn [e] (c/config! e :border (newBorder border-color-focus)))
                      :focus-lost   (fn [e] (c/config! e :border (newBorder border-color-unfocus)))
                      :caret-update (fn [e] ;; Watch changes and valid int
                                      (if-not (nil? (c/to-root (c/to-widget e))) (.repaint (c/to-root (c/to-widget e))))
                                      (if (and (< char-limit (count (c/value e))) (> char-limit 0))
                                        (do
                                          (c/invoke-later (c/config! e :text @last-v)))
                                        (do
                                          (if (empty? (c/value e))
                                            (do ;; Set "" if :text is empty
                                              (c/config! e :text "")
                                              (reset! last-v ""))
                                            (do ;; if :text not empty 
                                              (let [v (re-matches #"^[+-]?[1-9][0-9]*$" (c/value e))] ;; cut 0 on front
                                                (cond
                                                  (and (= "0" (str (first (c/value e)))) (= 1 (count (c/value e))))
                                                  (do
                                                    (reset! last-v 0))
                                                  (and (nil? v) (= "-" (str (first (c/value e)))) (> 2 (count (c/value e))))
                                                  (do
                                                    (reset! last-v (clojure.string/join "" (clojure.string/split (c/value e) #"0"))) ;; remember last correct c/value
                                                    (c/invoke-later (c/config! e :text @last-v)))
                                                  (not (nil? v))
                                                  (do ;; clear space
                                                    (reset! last-v (read-string v)))
                                                  :else (c/invoke-later (c/config! e :text @last-v))))))))
                                        ;; remember last correct c/value if integer and when remove last char
                                      (let [new-v @last-v] ;; local-changes
                                        (cond
                                          (and (not (nil? store-id))
                                               (not (= (str val) (str new-v))))
                                          (do
                                            (swap! local-changes (fn [storage] (assoc storage store-id new-v))))
                                          :else (reset! local-changes (dissoc @local-changes store-id)))))]
             style))))
;; (@jarman.gui.gui-app/startup)

;; (def view (fn [] (let [lbl (c/label)]
;;                    (mig-panel :constraints ["" "fill, grow" ""] :border (b/line-border :thickness 1 :color "#000") :size [200 :by 30] 
;;                               :items [[(input-float :display lbl)]
;;                                       [(input-int :display lbl)]
;;                                       [lbl]]))))

;; ;; Show example
;; (let [my-frame (-> (doto (seesaw.core/frame
;;                           :title "test"
;;                           :size [0 :by 0]
;;                           :content (view))
;;                      (.setLocationRelativeTo nil) seesaw.core/pack! seesaw.core/show!))]
;;   (seesaw.core/config! my-frame :size [800 :by 600]))



;; ┌────────────────────┐
;; │                    │
;; │ Other              │
;; │                    │________________________________________
;; └────────────────────┘                                       

(defn input-file
  "Description:
     File choser"
  [{:keys [store-id local-changes val]
    :or {store-id :input-file
         local-changes (atom {})
         val ""}}]
  (let [default-path (str jarman.config.environment/user-home "/Documents")
        input-text (input-text-with-atom 
                    {:val (rift val default-path)
                     :store-id store-id
                     :local-changes local-changes
                     :args [:font (gtool/getFont  :name "Monospaced")]})
        icon (button-basic
              ""
              :onClick (fn [e] (let [new-path (chooser/choose-file :success-fn  (fn [fc file] (.getAbsolutePath file)))]
                                 (c/config! input-text :text (rift new-path default-path))))
              :args [:icon (jarman.tools.swing/image-scale icon/enter-64-png 30)])
        panel (smig/mig-panel
               :constraints ["" "0px[fill]0px[grow, fill]0px" "0px[fill]0px"]
               :items [[icon] [input-text]])]
    panel))

(defn state-input-file
  "Description:
     File choser"
  [{func :func
    val  :val}]
  (let [default-path (str jarman.config.environment/user-home "/Documents")
        input-text (state-input-text {:func func
                                      :val  (rift val default-path)})
        icon (button-basic ""
              :onClick (fn [e] (let [new-path (chooser/choose-file :success-fn  (fn [fc file] (.getAbsolutePath file)))]
                                 (c/config! input-text :text (rift new-path default-path))))
              :args [:icon (jarman.tools.swing/image-scale icon/enter-64-png 30)])
        panel (smig/mig-panel
               :constraints ["" "0px[fill]0px[grow, fill]0px" "0px[fill]0px"]
               :items [[icon] [input-text]])]
    panel))

(defn menu-bar
  "Description:
      Bar with buttons without background.
      If you used :justify-end true then buttons will by justify to rigth.
   Example:
      (menu-bar :buttons [[\"title1\" icon1  fn1] [\"title2\" icon2  fn2]])
      (menu-bar :id :my-id :buttons [[\"title1\"  icon1 fn1] [\"title2\" icon2  fn2]])"
  [{:keys [id
           buttons
           offset
           justify-end
           font-size
           icon-size
           btn-border
           bg
           fg
           bg-hover
           border-c]
    :or {id :none
         buttons []
         offset 4
         justify-end false
         font-size 13
         icon-size 27
         btn-border [4 4 10 10 1]
         bg "#fff"
         fg "#000"
         bg-hover "#e2fbde"
         border-c "#bbb"}}]
  (let [btn (fn [txt ico tip onClick & args]
              (let [[t b l r br] btn-border]
                (c/label
                 :font (gtool/getFont font-size)
                 :text txt
                 :tip tip
                 :icon (stool/image-scale ico icon-size)
                 :background bg
                 :foreground fg
                 :focusable? true
                 :border (b/compound-border (b/empty-border :left l :right r :top t :bottom b)
                                            (b/line-border :thickness br :color border-c))
                 :listen [:mouse-entered (fn [e] (c/config! e :background bg-hover :foreground fg :cursor :hand))
                          :mouse-exited  (fn [e] (c/config! e :background bg :foreground fg))
                          :mouse-clicked onClick
                          :focus-gained  (fn [e] (c/config! e :background bg-hover  :cursor :hand))
                          :focus-lost    (fn [e] (c/config! e :background bg))
                          :key-pressed   (fn [e] (if (= (.getKeyCode e) java.awt.event.KeyEvent/VK_ENTER) (onClick e)))])))]
    (mig-panel
     :id id
     :background (new Color 0 0 0 0)
     :constraints (if justify-end
                    ["" "5px[grow, fill]0px[fill]5px" "5px[fill]5px"]
                    ["" (str offset "px[fill]0px") "5px[fill]5px"])
     :items (if (empty? (filter-nil buttons)) [[(c/label)]]
                (if justify-end
                  (gtool/join-mig-items (c/label) (map #(btn (first %) (second %) (nth % 2) (last %)) (filter-nil buttons)))
                  (gtool/join-mig-items (map #(btn (first %) (second %) (nth % 2) (last %)) (filter-nil buttons))))))))



(defn- validate-fields [cmpts-atom num]
  (condp = num
    0 (let [f-inp (:field @cmpts-atom)
            r-inp (:representation @cmpts-atom)
            d-inp (:description @cmpts-atom)]
        (if (or (= f-inp "") (= f-inp "field") (= f-inp nil) (< (count f-inp) 4)
                (= r-inp "") (= r-inp "representation") (= r-inp nil) (< (count f-inp) 4)
                (= d-inp "") (= d-inp "description") (= d-inp nil) (< (count f-inp) 4)
                ) false true))
    1 (let [cmp-inp (:component-type @cmpts-atom)
            col-inp (:column-type @cmpts-atom)]
        (if (or (= cmp-inp nil) 
                (= col-inp nil) 
                ) false true))
    2 (let [p-inp (:private? @cmpts-atom)
            e-inp (:editable? @cmpts-atom)]
        (if (or (= p-inp nil) 
                (= e-inp nil) 
                ) false true))
    true))

(defn multi-panel
  "Description:
    get vector of panels and return mig-panel in which these panels are replaced on click of arrow
   Example:
    (multi-panel [some-panel-1 some-panel-2 some-panel-3] title 0)"
  [panels cmpts-atom table-name title num]
  (let [btn-panel (menu-bar
                   {:id :db-viewer--component--menu-bar
                    :buttons [["Back"
                               icon/left-blue-64-png
                               (fn [e]
                                 (if (= num 0)
                                   (c/config!
                                    (.getParent (.getParent (seesaw.core/to-widget e)))
                                    :items [[(multi-panel panels cmpts-atom table-name title num)]])
                                   (c/config!
                                    (.getParent (.getParent (seesaw.core/to-widget e)))
                                    :items [[(multi-panel panels cmpts-atom table-name title (- num 1))]])))]
                              ["Next"
                               icon/right-blue-64-png 
                               (fn [e] (if (validate-fields cmpts-atom num)
                                         (if
                                             (=  num (- (count panels) 1))
                                           (c/config! (.getParent
                                                       (.getParent (seesaw.core/to-widget e)))
                                                      :items [[(multi-panel panels cmpts-atom table-name title num)]])
                                           (c/config! (.getParent (.getParent (seesaw.core/to-widget e)))
                                                      :items [[(multi-panel panels cmpts-atom table-name title (+ num 1))]]))
                                         ((state/state :alert-manager) :set {:header "Error" :body "All fields must be entered and must be longer than 3 chars"} 5)))]]})
        btn-back (first (.getComponents btn-panel))
        btn-next (second (.getComponents btn-panel))]
   ;; (c/config! btn-panel :bounds [0 0 0 0])
    (c/config! btn-next
               :border (b/compound-border (b/empty-border :left 0 :right 5 :top 3 :bottom 3) (b/line-border :thickness 1 :color "#bbb")))
    (c/config! btn-back
             :border (b/compound-border (b/empty-border :left 0 :right 5 :top 3 :bottom 3) (b/line-border :thickness 1 :color "#bbb"))
             :visible? (if (= num 0) false true))
    (if (= num (- (count panels) 1))
      (c/config! btn-next :text "Save" :listen [:mouse-clicked (fn [e]
                                                                 ;;(println @cmpts-atom)
                                                                 (swap! cmpts-atom  assoc :field-qualified (str (:field @cmpts-atom) "." table-name))
                                                                 ;; (println (:output (mt/validate-one-column
                                                                 ;;                       @cmpts-atom)))
                                                                 (if (:valid? (mt/validate-one-column
                                                                               @cmpts-atom))
                                                                   ((state/state :alert-manager) :set {:header "Success" :body "Column was added"} 5)
                                                                   ((state/state :alert-manager) :set {:header "Error" :body "All fields must be entered and must be longer than 3 chars"} 5)))]))
    (mig-panel
     :constraints ["wrap 2" "0px[left]0px" "0px[]0px"]
     :preferred-size [910 :by 360]
     :background light-light-grey-color
     :items [[(c/label :border (b/empty-border :right 18))]
             [btn-panel "align r"]
             [(c/label :text title
                     :foreground "#256599"
                     :border (b/empty-border :left 10)) "span 2"]
             [(nth panels num) "span 2"]])))


(defn- calc-popup-center
  [popup-size relative]
  (let [relative-width-middle  (/ (.getWidth  relative) 2)
        relative-height-middle (/ (.getHeight relative) 2)
        popup-width-middle     (/ (first  popup-size) 2)
        popup-height-middle    (/ (second popup-size) 2)
        x-offset               (- relative-width-middle popup-width-middle)
        y-offset               (- relative-height-middle popup-height-middle)
        x (+ (.x (.getLocationOnScreen (seesaw.core/to-frame relative))) x-offset)
        y (+ (.y (.getLocationOnScreen (seesaw.core/to-frame relative))) y-offset)]
    [x y]))

(def popup-window 
  "Description:
     Dialog window. Invoke passing component with view, window title and prefer size.
   Example:
     (popup-window {})
     (popup-window {:view (create-business-card) :window-title \"My card\"})
     (popup-window {:view (create-business-card) :window-title \"My card\" :size [200 300]})
   "
  (fn [{:keys [view size window-title relative]
        :or {view (c/label :text "Popup window")
             size [600 600]
             window-title "Popup window"
             relative (c/to-frame (state/state :app))}}]
    (let [relative (if (nil? relative) nil (calc-popup-center size (c/to-frame relative)))
          template (mig-panel 
                    :constraints ["" (format "0px[:%s:, grow, fill]0px" (first size)) (format "0px[:%s:, grow, fill]0px" (second size))]
                    :items [[view]])
          dialog (c/custom-dialog
                  :title window-title
                  :modal? true
                  :resizable? true
                  :content template
                  ;; :listen [:component-resized (fn [e]
                  ;;                              (.revalidate template) 
                  ;;                               ;; (let [w (.getWidth (c/to-frame e))
                  ;;                               ;;             h (.getWidth (c/to-frame e))])
                  ;;                               )]
                  )]
      ;; (.setUndecorated dialog true)
      (if (nil? relative) 
        (.setLocationRelativeTo dialog (c/to-root relative)) 
        (.setLocation dialog (first relative) (second relative)))
      (doto dialog c/pack! c/show!)
      )))


(defn popup-info-window
  "Description:
      Function for create window with some message.
   Example:
      (popup-info-window header body relative)
   "
  [header body relative]
;;   (println "\nheader" header "\nbody" body "\ninvoker" relative)
  (let [comp (mig-panel
              :constraints ["wrap 1" "10px[grow, fill]10px" "10px[fill]10px"]
              :id :message-view-box
              :items [[(c/label :text header
                                :font (gtool/getFont 18)
                                :border (b/empty-border :left 5 :right 5))]
                      [(hr 1 "#999" [0 0 0 5])]
                      [(multiline-text
                        {:text body})
                       ;; (c/label
                       ;;  :text (gtool/htmling body :justify)
                       ;;  :font (gtool/getFont 14)
                       ;;  :border (b/empty-border :left 10 :right 10))
                       ]])]
    (popup-window {:view comp :window-title "Info" :size [400 350] :relative relative})))




(def select-box-table-list
  "Description:
     Combobox with all tables.
   "
  (fn [{:keys [local-changes store-id val] 
        :or {local-changes (atom {})
             store-id :documents.table
             val nil}}]
    ;;(println "\ntable-select-box" store-id val)
    (select-box (vec (map #(get % :table_name) (jarman.logic.metadata/getset)))
               :store-id store-id
               :local-changes local-changes
               :selected-item (rift val ""))))



;; ┌────────────────────┐
;; │                    │
;; │ Code area          │
;; │                    │________________________________________
;; └────────────────────┘                                       

(defn code-area
  "Description:
    Some text area but with syntax styling.
    To check avaliable languages eval (seesaw.dev/show-options (seesaw.rsyntax/text-area)).
    Default language is Clojure.
  Example:
    (code-area {})
    (code-area :syntax :css)
  "
  [{:keys [val
           store-id
           local-changes
           syntax
           label
           args]
    :or {val ""
         store-id :code
         local-changes (atom {})
         syntax :clojure
         label nil
         args []}}]
  (swap! local-changes (fn [state] (assoc state store-id val)))
  (let [content (atom val)]
    (apply
     seesaw.rsyntax/text-area
     :text val
     :wrap-lines? true
     :caret-position 1
     :syntax syntax
     :user-data {:saved-content (fn [] (reset! content (second (first @local-changes))))}
     :listen [:caret-update (fn [e]
                              (swap! local-changes (fn [state] (assoc state store-id (c/config (c/to-widget e) :text))))
                              (if-not (nil? label)
                                (if (= (c/config (c/to-widget e) :text) @content)
                                  (c/config! label :text "")
                                  (c/config! label :text "Unsaved file..."))))]
     args)))

;;(seesaw.dev/show-options (seesaw.rsyntax/text-area))

(defn code-editor
  "Description:
     Simple code editor using syntax.
     When you send save-fn then inside is invoke (save-fn {:state local-changes :label info-label :code code})
   Example:
     (code-editor {})
  "
  [{:keys [local-changes
           store-id
           val
           font-size
           title
           title-font-size
           save-fn
           debug
           dispose
           args]
    :or {local-changes (atom {})
         store-id :code-tester
         val ""
         title "Code editor"
         title-font-size 14
         font-size 14
         save-fn (fn [state] (println "Additional save"))
         debug false
         dispose false
         args {}}}]
  (let [f-size (atom font-size)
        info-label (c/label)
        code (code-area {:args [:font (gtool/getFont @f-size)
                                :border (b/empty-border :left 10 :right 10)]
                         :label info-label
                         :local-changes local-changes
                         :store-id store-id
                         :val val}) 
        editor (vmig
                :args args
                :vrules "[fill]0px[grow, fill]0px[fill]"
                :items [[(hmig
                          :args args
                          :hrules "[70%, fill]10px[grow, fill]"
                          :items
                          [[(c/label :text title
                                     :border (b/compound-border (b/line-border :bottom 1 :color "#eee")
                                                                (b/empty-border :left 10))
                                     :font (gtool/getFont :bold title-font-size))]
                           [(menu-bar
                             {:justify-end true
                              :buttons [[""
                                         icon/up-blue2-64-png
                                         "Increase font"
                                         (fn [e]
                                           (c/config!
                                            code
                                            :font (gtool/getFont
                                                   (do (reset! f-size (+ 2 @f-size))
                                                       @f-size))))]
                                        [""
                                         icon/down-blue2-64-png
                                         "Decrease font"
                                         (fn [e]
                                           (c/config!
                                            code
                                            :font (gtool/getFont
                                                   (do (reset! f-size (- @f-size 2))
                                                       @f-size))))]
                                        (if debug
                                          ["" icon/loupe-blue-64-png "Show changes" (fn [e] (popup-info-window
                                                                                             "Changes"
                                                                                             (second (first @local-changes))
                                                                                             (c/to-frame e)))]
                                          nil)
                                        ["" icon/agree-blue-64-png "Save" (fn [e]
                                                                            (c/config! info-label :text "Saved")
                                                                            ((:saved-content (c/config code :user-data)))
                                                                            (save-fn {:state local-changes :label info-label :code code}))]
                                        (if dispose
                                          ["" icon/enter-64-png "Leave" (fn [e] (.dispose (c/to-frame e)))]
                                          nil)]})]])]
                        [(scrollbox code)]
                        [info-label]])]
    (gtool/set-focus code)
    
    editor))

;;(popup-window {:view (code-editor {})})

(defn popup-config-editor
  "Description:
     Prepared popup window with code editor for selected configuration segment.
   Example:
     (popup-metadata-editor [:init.edn] {:part-of-config {}})
  "
  [config-path config-part]
  (popup-window
   {:window-title "Configuration manual editor"
    :view (code-editor
           {:val (with-out-str (clojure.pprint/pprint config-part))
            :title (gtool/convert-key-to-title (first config-path))
            :dispose true
            :save-fn (fn [props]
                       (try
                         (cm/assoc-in-segment config-path (read-string (c/config (:code props) :text)))
                         (let [validate (cm/store-and-back)]
                           (if (:valid? validate)
                             (c/config! (:label props) :text "Saved!")
                             (c/config! (:label props) :text "Validation faild. Can not save.")))
                         (catch Exception e (c/config!
                                             (:label props)
                                             :text "Can not convert to map. Syntax error."))))})})
  (((state/state :jarman-views-service) :reload)))

(defn view-config-editor
  "Description:
     Prepared view with code editor for selected configuration segment.
   Example:
     (popup-metadata-editor [:init.edn] {:part-of-config {}})
  "
  [config-path config-part]
  ((state/state :jarman-views-service)
   :set-view
   :view-id (keyword (str "manual-view-code" (first config-path)))
   :title (str "Config: " (gtool/convert-key-to-title (first config-path)))
   :component-fn
   (fn [] (code-editor
           {:args [:border (b/line-border :top 1 :left 1 :color "#eee")
                   :background "#fff"]
            :val (with-out-str (clojure.pprint/pprint config-part))
            :title (gtool/convert-key-to-title (first config-path))
            :save-fn (fn [props]
                       (try
                         (cm/assoc-in-segment config-path (read-string (c/config (:code props) :text)))
                         (let [validate (cm/store-and-back)]
                           (if (:valid? validate)
                             (c/config! (:label props) :text "Saved!")
                             (c/config! (:label props) :text "Validation faild. Can not save.")))
                         (catch Exception e (c/config!
                                             (:label props)
                                             :text "Can not convert to map. Syntax error."))))}))))


(defn popup-metadata-editor
  "Description:
     Prepared popup window with code editor for selected metadata by table_name as key.
   Example:
     (popup-metadata-editor :user)
  "
  [table-keyword]
  (let [meta (mt/metadata-get table-keyword)]
      (popup-window
       {:window-title "Metadata manual table editor"
        :view (code-editor
               {:val (with-out-str (clojure.pprint/pprint (:prop meta)))
                :title (str "Metadata: " (get-in meta [:prop :table :representation]))
                :dispose true
                :save-fn (fn [state]
                           (try
                             (mt/metadata-set (assoc meta :prop (read-string (c/config (:code state) :text))))
                             (c/config! (:label state) :text "Saved!")
                             (catch Exception e (c/config!
                                                 (:label state)
                                                 :text "Can not convert to map. Syntax error."))))})}))
  (((state/state :jarman-views-service) :reload)))

(defn view-metadata-editor
  "Description:
     Prepared component with code editor for selected metadata by table_name as key.
   Example:
     (view-metadata-editor :user)
  "
  [table-keyword]
  ((state/state :jarman-views-service)
   :set-view
   :view-id (keyword (str "manual-view-code" (name table-keyword)))
   :title (str "Metadata: " (name table-keyword))
   :component-fn
   (fn [] (let [meta (mt/metadata-get table-keyword)]
            (code-editor
             {:args [:border (b/line-border :top 1 :left 1 :color "#eee")
                     :background "#fff"]
              :title (str "Metadata: " (get-in meta [:prop :table :representation]))
              :val (with-out-str (clojure.pprint/pprint (:prop meta)))
              :save-fn (fn [state]
                         (try
                           (mt/metadata-set (assoc meta :prop (read-string (c/config (:code state) :text))))
                           (c/config! (:label state) :text "Saved!")
                           (catch Exception e (c/config!
                                               (:label state)
                                               :text "Can not convert to map. Syntax error."))))})))))


(comment
  ;; (seesaw.dev/show-options (c/styled-text))

  ;; (seesaw.dev/show-events  (c/styled-text))

  ;;
  ;; Switch focus TOTRY
  ;;
  ;; Run app and eval
  ;; (let [lbl (c/label :text "Dupa" :focusable? true :listen [:focus-gained (fn [e] (c/config! e :foreground "#f00"))
  ;;                                                           :focus-lost   (fn [e] (c/config! e :foreground "#00f"))])
  ;;       mig (c/grid-panel :items [lbl (c/button :text "a" :listen [:mouse-clicked (fn [e](seesaw.core/request-focus! lbl))])])]
  ;;   (popup-window
  ;;    {:window-title "Popup for debug"
  ;;     :view mig}))
  )
