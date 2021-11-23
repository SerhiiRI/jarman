(ns jarman.gui.gui-components
  (:use seesaw.dev
        seesaw.mig)
  (:require [jarman.resource-lib.icon-library :as icon]
            [jarman.gui.gui-style             :as gs]
            [jarman.faces  :as face]
            [seesaw.core   :as c]
            [seesaw.border :as b]
            [seesaw.util   :as u]
            [seesaw.mig    :as smig]
            [seesaw.rsyntax]
            [jarman.faces :as face]
            [jarman.tools.swing    :as stool]
            [jarman.tools.lang     :refer :all]
            [jarman.logic.state    :as state]
            [jarman.gui.gui-tools  :as gtool]
            [jarman.gui.gui-migrid :as gmg]
            [jarman.gui.gui-style  :as gs]
            [seesaw.chooser        :as chooser]
            [jarman.gui.gui-tutorials.key-dispacher-tutorial :as key-tut])
  (:import (java.awt Color)
           (java.awt Dimension)
           (jarman.jarmanjcomp CustomScrollBar)
           (jiconfont.icons.google_material_design_icons GoogleMaterialDesignIcons)))

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
   :listen [:focus-gained (fn [e] (c/config! e :border (b/line-border :left hgap :top vgap)))
            :focus-lost   (fn [e] (c/config! e :border (b/line-border :left hgap :top vgap)))]
   args))

(defn auto-scrollbox
  [component & args]
  (let [scr (apply c/scrollable component :border nil args)
        scr (c/config!
             scr
             :listen [:property-change
                      (fn [e] (c/invoke-later (try
                                                (let [get-root (fn [e] (.getParent (.getParent (.getParent (.getSource e)))))
                                                      vbar 0
                                                      w (- (.getWidth (get-root e)) vbar)
                                                      h (+ vbar (.getHeight (c/config e :preferred-size)))]
                                                  (c/config! component :size [w :by h]))
                                                (catch Exception e (str "Auto scroll cannot get parent")))))])]
    (.setUnitIncrement (.getVerticalScrollBar scr) 20)
    (.setPreferredSize (.getVerticalScrollBar scr) (java.awt.Dimension. 12 0))
    (.setUnitIncrement (.getHorizontalScrollBar scr) 0)
    (.setPreferredSize (.getHorizontalScrollBar scr) (java.awt.Dimension. 0 12))
    scr))


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
   & {:keys [hscroll-off vscroll-hide args]
      :or {hscroll-off false
           vscroll-hide false
           args []}}]
  (let [scr (CustomScrollBar/myScrollPane component face/c-min-scrollbox-bg)
        get-key (fn [x] (first (first x)))
        get-val (fn [x] (second (first x)))]
    (if-not (empty? args)
      (map (fn [[k v]] (c/config! scr k v))
           (apply hash-map args)))    
    (c/config! scr
               :border (b/line-border :thickness 0)
               :vscroll (if vscroll-hide :never :always)
               :opaque? false
               :listen [:mouse-wheel-moved (fn [e] (c/invoke-later (.repaint (c/to-root e))))])
    ;; (c/config! scr :preferred-size [500 :by 1000])
    (c/config! scr :listen [:component-resized
                            (fn [e] (if hscroll-off ;; TODO: height bugging
                                      (if (= -1 (first @(state/state :atom-app-resize-direction)))
                                        (let [w (.getWidth  (.getSize scr))
                                              h (.getHeight (.getSize scr))
                                              layout (first (seesaw.util/children (second (seesaw.util/children scr))))
                                              layout-h (.getHeight (.getPreferredSize layout))]
                                        ;(println layout)
                                          (c/config! layout :maximum-size [w :by 100000000])
                                          (c/config! layout :preferred-size [w :by (.getHeight (.getPreferredSize layout))])
                                          (.repaint scr)))
                                      (do (.revalidate scr)
                                          (.repaint scr))))])
    (.setUnitIncrement (.getVerticalScrollBar scr) 20)
    (.setUnitIncrement (.getHorizontalScrollBar scr) 20)
    scr))


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
  [{:keys [text foreground ;; font
           lgap tgap args]
    :or {foreground face/c-foreground
         ;; font (gtool/getFont)
         lgap 0
         tgap 0
         args []}}]
  (let [text-area (apply
                   c/styled-text
                   :border (b/empty-border :left lgap :top tgap)
                   :text text
                   ;; :font font
                   :foreground  foreground
                   :editable?   false
                   :focusable?  false
                   :wrap-lines? true
                   :opaque?     false
                   args)]
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
                 tip
                 bg-hover
                 bg
                 underline-size
                 underline-focus
                 underline
                 flip-border
                 ]
          :or   {onClick (fn [e] (println "Click"))
                 args []
                 tgap 10
                 bgap 10
                 lgap 10
                 rgap 10
                 tip    ""
                 halign :center
                 bg              face/c-btn-bg
                 bg-hover        face/c-btn-bg-focus
                 underline-focus face/c-btn-underline-on-focus
                 underline       face/c-btn-underline
                 underline-size  face/s-btn-underline
                 flip-border     false
                 }}]
  (let [newBorder (fn [underline-color]
                    (b/compound-border (b/empty-border :bottom bgap :top tgap :left lgap :right rgap)
                                       (b/line-border (if flip-border :top :bottom) underline-size :color underline-color)))]
    (apply c/label
           :text txt
           :focusable? true
           :halign halign
           :tip tip
           :listen [:mouse-clicked (fn [e] (do (onClick e) (gtool/switch-focus)))
                    :mouse-entered (fn [e]
                                     (c/config! e :border (newBorder underline-focus) :background bg-hover :cursor :hand)
                                     (.repaint (c/to-root e)))
                    :mouse-exited  (fn [e]
                                     (c/config! e :border (newBorder underline)       :background bg)
                                     (.repaint (c/to-root e)))
                    :focus-gained  (fn [e] (c/config! e :border (newBorder underline-focus) :background bg-hover :cursor :hand))
                    :focus-lost    (fn [e] (c/config! e :border (newBorder underline)       :background bg))
                    :key-pressed   (fn [e] (if (= (.getKeyCode e) java.awt.event.KeyEvent/VK_ENTER) (do (onClick e) (gtool/switch-focus))))]
           :background bg
           :border (newBorder underline)
           args)))


(defn button-slim
  [txt
   & {:keys [onClick 
             underline-size
             bg
             bg-hover
             args]
      :or {onClick (fn [e])
           underline-size 0
           bg       face/c-btn-bg
           bg-hover face/c-btn-bg-focus
           args []}}]
  (button-basic
   txt
   :onClick onClick
   :tgap 5
   :bgap 5
   :underline-size underline-size
   :halign :left
   :bg   bg
   :bg-hover bg-hover
   :args args))


(defn button-icon
  [{:keys [icon-on icon-off size func tip margin frame-hover c-border-focus]
    :or   {icon-on (gs/icon GoogleMaterialDesignIcons/HELP  face/c-icon)
           size 30
           func (fn [e])
           margin [0 0 0 0]
           frame-hover true
           c-border-focus face/c-icon-btn-focus}}]
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
                                          :border (border-fn c-border-focus)))
              :mouse-exited  (fn [e]
                               (c/config! e
                                          :icon (ico-fn ico-off)
                                          :border (border-fn (gtool/opacity-color)))
                               (.repaint (c/to-root e)))
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
    (gmg/hmig
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
             underline-off
             halign
             args]
      :or {val ""
           placeholder ""
           font-size 14
           border-color-focus   face/c-underline-on-focus
           border-color-unfocus face/c-underline
           border [10 10 5 5 2]
           char-limit 0
           underline-off false
           halign :left
           args []}}]
  (let [          
        fn-get-data     (fn [e key] (get-in (c/config e :user-data) [key]))
        fn-assoc        (fn [e key val] (assoc-in (c/config e :user-data) [key] val))
        newBorder (if underline-off (fn [_] (b/empty-border :thicness 0))
                    (fn [underline-color]
                     (b/compound-border (b/empty-border :left (nth border 0) :right (nth border 1) :top (nth border 2) :bottom (nth border 3))
                                        (b/line-border :bottom (nth border 4) :color underline-color))))
        last-v (atom "")]
    (apply c/text
           :text (if (empty? val) placeholder (if (string? val) val (str val)))
           :border (newBorder border-color-unfocus)
           :user-data {:placeholder placeholder :value "" :edit? false :type :input :border-fn newBorder}
           :halign halign
           :listen [:focus-gained (fn [e]
                                    (.repaint (c/to-root e))
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
     Text component converted to c/text component with placeholder. Placehlder will be default value.
     Text input for state architecture. Need fn in action to changing state.
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
     (state-input-text {:func (fn [e] (dispatch! {...})) :val \"some value\" }"
  [{func :func
    val  :val}
   & {:keys [placeholder
             border
             font-size
             border-color-focus
             border-color-unfocus
             background
             char-limit
             start-underline
             args]
      :or {placeholder ""
           font-size 14
           border-color-focus   face/c-underline-on-focus
           border-color-unfocus face/c-underline
           background           face/c-input-bg
           border [10 10 5 5 2]
           char-limit 0
           start-underline nil
           args []}}]
  (let [;;val             (if (empty? val) "" (str val))
        fn-get-data     (fn [e key] (get-in (c/config e :user-data) [key]))
        fn-assoc        (fn [e key val] (assoc-in (c/config e :user-data) [key] val))
        newBorder (fn [underline-color]
                    (b/compound-border (b/empty-border :left (nth border 0) :right (nth border 1) :top (nth border 2) :bottom (nth border 3))
                                       (b/line-border :bottom (nth border 4) :color underline-color)))
        last-v (atom "")]
    (apply c/text
           :text (if (empty? val) placeholder (if (string? val) val (str val)))
           ;; :font (gtool/getFont font-size)
           :background background
           :border (newBorder (rift start-underline border-color-unfocus))
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
                                        (do
                                          (reset! last-v new-v)
                                          (c/config! e :user-data (fn-assoc e :value (if (= placeholder @last-v) "" @last-v)))
                                          (func e)))))]
           args)))

(defn jcheckbox [& {:keys [nochecked-icon checked-icon text]
                    :or {nochecked-icon (gs/icon GoogleMaterialDesignIcons/CHECK_BOX_OUTLINE_BLANK)
                         checked-icon   (gs/icon GoogleMaterialDesignIcons/CHECK_BOX)
                         text ""}}]
  (let [check-box (c/label :text text :icon nochecked-icon :user-data false)]
    (c/config! check-box :listen [:mouse-clicked
                                  (fn [e] (let [checked? (c/config check-box :user-data)]
                                            (c/config! check-box
                                                       :user-data (not checked?)
                                                       :icon (if (not checked?) checked-icon nochecked-icon)))
                                    (.repaint (c/to-root check-box)))])))

(defn- jradiobox
  "Description
     Basic component for radio-group."
  [& {:keys [nochecked-icon checked-icon text set-selected extra-fn]
      :or {nochecked-icon (gs/icon GoogleMaterialDesignIcons/CHECK_BOX_OUTLINE_BLANK)
           checked-icon   (gs/icon GoogleMaterialDesignIcons/CHECK_BOX)
           text ""}}]
  (let [check-box (c/label :text text
                           :icon (if set-selected checked-icon nochecked-icon)
                           :user-data (if set-selected true false))]
    (c/config! check-box :listen [:mouse-clicked
                                  (fn [e] (if-not (c/config e :user-data)
                                            (let [radio-box     (.getParent (c/to-widget e))
                                                  radio-buttons (u/children radio-box)]
                                              (doall (map (fn [rb] (c/config! rb
                                                                              :icon nochecked-icon
                                                                              :user-data false))
                                                          radio-buttons))
                                              (c/config! check-box :user-data true :icon checked-icon)
                                              (c/config! radio-box :user-data (doall (vec (map #(if (c/config % :user-data) true false) radio-buttons))))
                                              (if (fn? extra-fn) (extra-fn e radio-box))
                                              (.repaint (c/to-root radio-box)))))])))

(defn- jradio-reqursive-tf-vec
  "Description:
     Create vector with true and false.
     If one of radio is selected then set true
     else set false. Used recursive.
  Example:
     (jradio-reqursive-tf-vec [\"a\" \"b\" \"c\"] [] 1 0)
     ;; => [false true false]"
  [first-vec second-vec selected-idx i]
  (if (> (count first-vec) 0)
    (let [tmp-vec (if (= i selected-idx) (concat second-vec [true]) (concat second-vec [false]))]
      (jradio-reqursive-tf-vec (drop 1 first-vec) tmp-vec selected-idx (+ i 1)))
    second-vec))

(defn- jradio-reqursive-radios
  "Description:
     Create vector with radio buttons.
     Similar to jradio-reqursive-tf-vec but in vector
     is only one selected radio button component
     root     - is a box for radio buttons
     extra-fn - custom action on select"
  [first-vec second-vec selected-idx i root extra-fn]
  (if (> (count first-vec) 0)
    (let [torf    (if (= i selected-idx) true false)
          tmp-vec (concat second-vec [(jradiobox :text (first first-vec)
                                                 :set-selected torf
                                                 :extra-fn extra-fn)])]
      (jradio-reqursive-radios (drop 1 first-vec) tmp-vec selected-idx (+ i 1) root extra-fn))
    second-vec))

(defn jradiogroup
  "Description:
     Create radio group panel.
     Inside :user-data is vector with selected and not selected like [false true false]
   Example:
     (jradiogroup [\"a\" \"b\" \"c\"] (fn [e]) :selected 1)
     (.indexOf (c/config my-radio-group :user-data) true)
     ;; => 1"
  [options-vec oncheck-fn
   & {:keys [horizontal selected]}]
  (let [panel (if (= true horizontal) (gmg/migrid :> :f {:rgap 20} []) (gmg/migrid :v []))
        boxes (jradio-reqursive-radios options-vec [] (if (int? selected) selected 0) 0 panel oncheck-fn)
        box-tf-vec (if (int? selected)
                     (jradio-reqursive-tf-vec options-vec [] selected 0)
                     (doall (vec (map (fn [_] false) options-vec))))]
    (c/config! panel :items (gtool/join-mig-items boxes) :user-data box-tf-vec)))

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
         ;; :font (gtool/getFont font-size)
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

(defn state-input-checkbox
  [{func :func
    val  :val}
   & {:keys [txt
             font-size
             args]
      :or   {txt ""
             font-size 14
             args []}}]
  (let [check  (apply c/checkbox
                      :text (str (= "true" val))
                      ;; :font (gtool/getFont font-size)
                      :selected? (= "true" val)
                      :border (b/empty-border :top 15)
                      :listen [:mouse-clicked (fn [e] (c/config! e :text (str (c/value (c/to-widget e)))) (func e))]
                      args)] (func check)
       check))


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
      :or {font-color face/c-input-header}}] 
  (seesaw.mig/mig-panel :constraints ["wrap 1" "0px[fill, grow]0px" "5px[]5px"]
                        :items [[(c/label :text title
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
         border-color-focus   face/c-underline-on-focus
         border-color-unfocus face/c-underline
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
           border-color-focus   face/c-underline-on-focus
           border-color-unfocus face/c-underline}}]
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
         border-color-focus   face/c-underline-on-focus
         border-color-unfocus face/c-underline
         onClick (fn [e])
         debug false}}]
  (swap! local-changes (fn [storage] (assoc storage store-id val)))
  (input-text
   :args [:editable? editable?
          :enabled? enabled?
          :text val
          :foreground (if editable? "#000" "#456fd1")
          :focusable? true
          :background (if-not editable? face/c-layout-background face/c-compos-background)
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
               ;; :font (gtool/getFont 14)
               :enabled? enabled?
               :editable? editable?
               :background face/c-compos-background ;; TODO: to check
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
                ;; :font (gtool/getFont 14)
                :enabled? enabled?
                :editable? editable?
                :background face/c-compos-background
                :selected-item start-v
                :listen [:item-state-changed
                         (fn [e]
                           (let [selected-repres (c/config e :selected-item)
                                 selected (get model-m (keyword selected-repres))
                                 without-selected (filter #(not (= selected %)) model-v)
                                 new-model (into [selected] without-selected)]
                             (func selected)))])))

;;; fixme:aleks
;;;--------------------------
;; (defn state-table-list
;;   [{dispatch! :dispatch!
;;     action    :action
;;     path      :path
;;     val       :val}]
;;   (let [func (fn [selected]
;;                (dispatch! {:action action
;;                            :path   path
;;                            :value  selected}))
;;         model-v (vec (map #(get % :table_name) (jarman.logic.metadata/getset)))
;;         repres-v model-v
;;         start-v (rift val (first model-v))]
;;     (dispatch! {:action action
;;                 :path   path
;;                 :value  start-v})
;;     (state-combo-box func model-v repres-v :start-v start-v)))


(defn expand-form-panel
  "Description:
     Create panel who can hide inside components. 
     Inside :user-data is function to hide/show panel ((get (config e :user-data) :hide-btn))
   Example:
     (expand-form-panel parent (component or components))"
  [view-layout compos
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
             c-focus
             c-bg
             c-bg-hide
             c-fg]
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
           text-open "hide"
           text-hide "..."
           c-focus   face/c-on-focus
           c-bg      face/c-table-header-bg
           c-bg-hide face/c-layout-background
           c-fg      face/c-foreground}}]
  (let [min-w (if (< max-w min-w) max-w min-w)
        w (if (< max-w w) max-w min-w)
        hsize (str lgap "px[" min-w ":" w ":" max-w ", grow,fill]" rgap "px")
        form-space-open ["wrap 1" hsize (str tgap "px[fill]0px" vrules bgap "px")]
        form-space-hide ["" "0px[20, fill]0px" "0px[grow, fill]0px"]
        form-space (apply smig/mig-panel :constraints form-space-open args)
        hide-btn (c/label)
        onClick (fn [_]
                  (let [inside (u/children form-space)]
                    (if (< 1 (count inside))
                      (do
                        ;;(println "\nHide")
                        (c/config! form-space :constraints form-space-hide)
                        (c/config! form-space :items [[hide-btn]])
                        (c/config! hide-btn   :text text-hide :valign :top :halign :center :icon nil :background c-bg-hide)
                        (.revalidate view-layout))
                      (do
                        ;;(println "\nShow")
                        (c/config! form-space :constraints form-space-open)
                        (c/config! hide-btn   :text text-open :halign :left :icon icon-open :background c-bg)
                        (c/config! form-space :items (gtool/join-mig-items hide-btn compos))
                        (.revalidate view-layout)))))
        hide-btn (c/config! hide-btn
                            :text text-open
                            :icon icon-open
                            :valign :center
                            :focusable? true
                            :background c-bg
                            :foreground c-fg
                            :border (b/empty-border :left 2 :right 2)
                            :listen [:focus-gained (fn [e] (c/config! e :foreground c-focus))
                                     :focus-lost   (fn [e] (c/config! e :foreground c-bg))
                                     :mouse-entered (fn [e] (gtool/hand-hover-on e))
                                     :mouse-clicked onClick
                                     :key-pressed  (fn [e] (if (= (.getKeyCode e) java.awt.event.KeyEvent/VK_ENTER) (onClick e)))])]
    (c/config! form-space :user-data {:hide-btn (fn [](onClick hide-btn))})
    (c/config! form-space :items (gtool/join-mig-items hide-btn compos))))


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
                 border-color-focus   face/c-underline-on-focus
                 border-color-unfocus face/c-underline
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
             ;; :font (gtool/getFont font-size)
             :background face/c-compos-background
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


(def state-input-password
  "Description:
    Text component converted to state password input component.
  Example:
    (state-input-password {:state! state! :dispatch! dispatch! :action :update :path [:a :b]})"
  (fn [{dispatch! :dispatch!
        action    :action
        path      :path}
       & {:keys [placeholder
                 border
                 font-size
                 border-color-focus
                 border-color-unfocus
                 style]
          :or   {placeholder "Password"
                 font-size 14
                 border-color-focus   face/c-underline-on-focus
                 border-color-unfocus face/c-underline
                 border [10 10 5 5 2]
                 style []}}]
    (let [allow-clean (atom false)
          fn-letter-count (fn [e] (count (c/value e)))
          fn-hide-chars   (fn [e] (apply str (repeat (fn-letter-count e) "*")))
          fn-get-data     (fn [e k] (get-in (c/config e :user-data) [k]))
          fn-assoc        (fn [e k v] (assoc-in (c/config e :user-data) [k] v))
          newBorder       (fn [underline-color]
                            (b/compound-border (b/empty-border :left (nth border 0) :right (nth border 1) :top (nth border 2) :bottom (nth border 3))
                                               (b/line-border :bottom (nth border 4) :color underline-color)))]
      (apply c/text
             :text placeholder
             ;; :font (gtool/getFont font-size)
             :background face/c-compos-background
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
                                      (let [dispatch-fn (fn [m] (dispatch! {:action action
                                                                            :path   path
                                                                            :value  (:value m)}))]
                                        (cond (and (= (fn-get-data e :edit?) true)
                                                   (not (= (c/value e) placeholder)))
                                              (cond (< 0 (count (c/value e)))
                                                    (let [added-chars (clojure.string/replace (c/value e) #"\*+" "")]
                                                      (cond (> (count added-chars) 0)
                                                            (let [update-pass (fn-assoc e :value (str (fn-get-data e :value) added-chars))]
                                                              (c/config! e :user-data update-pass)
                                                              (dispatch-fn update-pass)
                                                              (c/invoke-later (c/config! e :text (fn-hide-chars e))))
                                                            (< (fn-letter-count e) (count (fn-get-data e :value)))
                                                            (let [update-pass (fn-assoc e :value (subs (fn-get-data e :value) 0 (fn-letter-count e)))]
                                                              (c/config! e :user-data update-pass)
                                                              (dispatch-fn update-pass)
                                                              (if (= 1 (count (c/value e))) (reset! allow-clean true))
                                                              (c/invoke-later (c/config! e :text (fn-hide-chars e))))))

                                                    (and (= true @allow-clean)
                                                         (= 0 (count (c/value e))))
                                                    (let [update-pass (fn-assoc e :value "")]
                                                      (c/config! e :user-data update-pass)
                                                      (dispatch-fn update-pass)
                                                      (c/invoke-later (c/config! e :text ""))
                                                      (reset! allow-clean false))))))]
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
                 onClickPlus
                 over-func
                 lvl
                 background
                 foreground
                 offset-color
                 seamless-bg
                 title-icon
                 before-title
                 font]
          :or {expand false
               border (b/empty-border :thickness 0)
               vsize 35
               min-height 200
               ico       (gs/icon GoogleMaterialDesignIcons/ADD face/c-icon 25)
               ico-hover (gs/icon GoogleMaterialDesignIcons/REMOVE face/c-icon 20)
               id :none
               onClick nil
               onClickPlus nil
               over-func :none
               lvl 1
               background face/c-btn-expand-bg
               foreground face/c-btn-expand-fg
               offset-color face/c-btn-expand-offset
               seamless-bg true
               title-icon nil
               before-title (fn [] (c/label))
               font (gs/getFont :bold)}}]
    (let [inside-btns (if (nil? inside-btns) nil inside-btns) ;; check if nill
          inside-btns (if (seqable? inside-btns) inside-btns (list inside-btns)) ;; check if not in list
          inside-btns (if (sequential? (first inside-btns)) (first inside-btns) inside-btns) ;; check if list in list
          ;;ico (if (or (= :always expand) (not (nil? inside-btns))) ico nil)
          title (c/label
                 :icon title-icon
                 :border (b/compound-border
                          ;; (b/line-border  :left left-offset :color background)
                          (b/empty-border :left 10))
                 :text txt
                 :font font
                 :foreground foreground
                 :background (Color. 0 0 0 0))
          listen (fn [func] [:mouse-entered gtool/hand-hover-on
                             :mouse-clicked (fn [e] (func e))
                             :focus-gained  (fn [e] (c/config! e :background face/c-underline-on-focus))
                             :focus-lost    (fn [e] (c/config! e :background face/c-underline))
                             :key-pressed   (fn [e] (if (= (.getKeyCode e) java.awt.event.KeyEvent/VK_ENTER) (func e)))])
          icon (c/label
                :size [vsize :by vsize]
                :halign :center
                :background (Color. 0 0 0 0)
                :icon (if expand ico-hover ico))
          mig  (gmg/migrid :v {:args [:background background]} [])
          user-data  {:title-fn (fn [new-title] (c/config! title :text new-title))}
          expand-btn (fn [func]
                       (c/config! title :listen (if (= :none over-func) (listen func) [:mouse-clicked over-func
                                                                                       :mouse-entered gtool/hand-hover-on]))
                       (c/config! icon :listen (listen func))
                       (gmg/migrid :> :fgf {:args [:background background :focusable? true
                                                   :border (b/compound-border
                                                            (b/line-border :left (* lvl 6) :color (if seamless-bg background offset-color)))]}
                                   [(if (fn? before-title)
                                      [(let [bt (before-title)] (c/config! bt :background background) bt)]
                                      [(c/label)])
                                    title icon]))
          expand-box (gmg/migrid :v "[grow, fill]" {:args [:border border]} (if expand inside-btns []))]
      (if (nil? onClick)
        (let [onClick (fn [e]
                        (reset! (state/state :atom-app-size) @(state/state :atom-app-size))
                        (if-not (nil? inside-btns)
                          (if (= 0 (count (u/children expand-box)))
                            (do ;;  Add inside buttons to mig with expand button
                              (c/config! icon :icon ico-hover)
                              (c/config! expand-box :items (gtool/join-mig-items inside-btns))
                              ;;(.revalidate (c/to-root e))
                              (if (fn? onClickPlus) (onClickPlus e))
                              (.repaint (c/to-root e)))
                            (do ;;  Remove inside buttons form mig without expand button
                              (c/config! icon :icon ico)
                              (c/config! expand-box :items [])
                              (if (fn? onClickPlus) (onClickPlus e))
                              ;;(.revalidate (c/to-root e))
                              (.repaint (c/to-root e))))))]
          (let [exbtn (c/config! mig
                                 :id id
                                 :items [[(expand-btn onClick)] [expand-box]])]
            exbtn))
        (c/config! mig :id id :items [[(expand-btn onClick)] [expand-box]])))))


(defn expand-input
  [{:keys [panel onClick title]
    :or {panel (seesaw.core/vertical-panel :items (list (c/label :text "heyy")))
         onClick (fn [e])}}]
  (button-expand (rift title "Enter")
                 panel
                 :min-height 220
                 :over-func onClick
                 :background face/c-btn-expand-bg))

(def button-expand-child
  "Description
     Interactive button inside menu from expand button.
   Example:
     (button-expand-child \"Title\" {:onClick (fn [e]) :args [:border nil]})
   "
  (fn [title
       & {:keys [onClick
                 offset-color
                 c-focus
                 c-fg-focus
                 background
                 foreground
                 cursor
                 lvl
                 height
                 icon
                 before-title
                 seamless-bg
                 args]
          :or {onClick (fn [e] (println "Clicked: " title))
               cursor :hand
               offset-color face/c-btn-expand-offset
               c-focus    face/c-on-focus
               c-fg-focus face/c-foreground
               background face/c-compos-background
               foreground face/c-foreground
               lvl 1
               height 30
               icon nil
               before-title (fn [] (c/label))
               seamless-bg true
               args []}}]
    (gmg/migrid
     :> :fg (format "[%s, fill]" height)
     {:args [:border (b/line-border :left (* lvl 6) :color (if seamless-bg background offset-color))]}
     [(if (fn? before-title) (let [bt (before-title)] (c/config! bt :background background) bt) (c/label))
      (apply c/label
             :text (str title)
             :background background
             :foreground foreground
             :cursor cursor
             :focusable? true
             :border (b/empty-border :left 10)
             :icon icon
             :listen [:mouse-clicked (fn [e] (do (onClick e) (gtool/switch-focus)))
                      :mouse-entered (fn [e] (.requestFocus (c/to-widget e)))
                      :mouse-exited  (fn [e] (.requestFocus (c/to-root e)))
                      :focus-gained  (fn [e] (c/config! e :background c-focus    :foreground c-fg-focus))
                      :focus-lost    (fn [e] (c/config! e :background background :foreground face/c-foreground))
                      :key-pressed   (fn [e] (if (= (.getKeyCode e) java.awt.event.KeyEvent/VK_ENTER) (do (onClick e) (gtool/switch-focus))))]
             args)])))


;; ┌────────────────────┐
;; │                    │
;; │ Examples           │
;; │                    │________________________________________
;; └────────────────────┘                                       

;; BUTTON EXPAND
(def view (fn [] (button-expand "Expand"
                                (button-expand-child "Expanded")
                                (button-expand-child "Don't touch me." :onClick (fn [e] (c/config! (c/to-widget e) :text "FAQ 🖕( ͡° ᴗ ͡°)🖕" ;; :font (gtool/getFont 14 :name "calibri")
                                                                                                   ))))))

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
                 border-color-focus   face/c-underline-on-focus
                 border-color-unfocus face/c-underline
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
             ;; :font (gtool/getFont font-size)
             :background face/c-compos-background
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
               border-color-focus   face/c-underline-on-focus
               border-color-unfocus face/c-underline
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
             :background face/c-compos-background
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
     File chooser"
  [{:keys [store-id local-changes val]
    :or {store-id :input-file
         local-changes (atom {})
         val ""}}]
  (let [default-path (str jarman.config.environment/user-home "/Documents")
        input-text (input-text-with-atom 
                    {:val (rift val default-path)
                     :store-id store-id
                     :local-changes local-changes
                     ;; :args [:font (gtool/getFont  :name "Monospaced")]
                     })
        icon (button-basic
              ""
              :onClick (fn [e] (let [new-path (chooser/choose-file :success-fn  (fn [fc file] (.getAbsolutePath file)))]
                                 (c/config! input-text :text (rift new-path default-path))))
              :args [:icon (gs/icon GoogleMaterialDesignIcons/ATTACHMENT face/c-icon 30)])
        panel (smig/mig-panel
               :constraints ["" "0px[fill]0px[grow, fill]0px" "0px[fill]0px"]
               :items [[icon] [input-text]])]
    panel))

(defn state-input-file
  "Description:
     File chooser"
  [{func :func
    val  :val}]
  (let [default-path (str jarman.config.environment/user-home "/Documents")
        input-text   (state-input-text {:func func
                                        :val  (rift val default-path)})
        icon (button-basic ""
              :onClick (fn [e] (let [new-path (chooser/choose-file :success-fn  (fn [fc file] (.getAbsolutePath file)))]
                                 (c/config! input-text :text (rift new-path default-path))))
              :args [:icon (gs/icon GoogleMaterialDesignIcons/ATTACHMENT face/c-icon 30)])
        panel (smig/mig-panel
               :constraints ["" "0px[fill]0px[grow, fill]0px" "0px[fill]0px"]
               :items [[icon] [input-text]])]
    panel))

;; to do chooser dir not file
(defn status-input-file
  "Description:
     File choser, button with icon, when path is selected it changes background color"
  [{func :func
    val  :val
    mode :mode}]
  (let [val            (if (empty? val) "" val)
        ico-to-choose  (gs/icon GoogleMaterialDesignIcons/ATTACHMENT face/c-icon 17)
        ico-chosen     (gs/icon GoogleMaterialDesignIcons/INSERT_DRIVE_FILE face/c-icon 17)
        icon-chooser   (fn [compn path] (if-not (empty? path)
                                          (c/config! compn :icon ico-chosen :tip path)
                                          (c/config! compn :icon ico-to-choose :tip "")))
        icon (button-basic ""
                           :tgap 4 :bgap 4 :lgap 0 :rgap 0
                           :onClick (fn [e] 
                                      (let [new-path (chooser/choose-file
                                                      :selection-mode (if mode :dirs-only :files-only)
                                                      :suggested-name val
                                                      :success-fn  (fn [fc file] (.getAbsolutePath file)))]

                                        (icon-chooser (.getComponent e) new-path)
                                        (func (seesaw.core/label :text new-path))))
                           :args [:icon ico-to-choose])]
    (icon-chooser icon val)
    icon))

(defn menu-bar
  "Description:
      Bar with buttons without background.
      If you used :justify-end true then buttons will by justify to rigth.
   Example:
      (menu-bar {:buttons [[\"title1\" icon1  (fn [e] )] [\"title2\" icon2  (fn [e] )]]})
      (menu-bar {:id :my-id :buttons [[\"title1\"  icon1 fn1] [\"title2\" icon2  fn2]]})"
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
           ]
    :or {id :none
         buttons []
         offset 4
         justify-end false
         font-size 13
         icon-size 27
         btn-border [4 4 10 10 1]
         bg       face/c-compos-background
         fg       face/c-foreground
         bg-hover face/c-menu-bar-on-focus
         }}]
  (let [btn (fn [txt ico tip onClick & args]
              (let [[t b l r br] btn-border]
                (c/label
                 ;; :font (gtool/getFont font-size)
                 :text txt
                 :tip tip
                 :icon (if ico (stool/image-scale ico icon-size) nil) 
                 :background bg
                 :foreground fg
                 :focusable? true
                 :border (b/empty-border :left l :right r :top t :bottom b)
                 :listen [:mouse-entered (fn [e] (c/config! e :background bg-hover :cursor :hand) (.repaint (c/to-root e)))
                          :mouse-exited  (fn [e] (c/config! e :background bg))
                          :mouse-clicked onClick
                          :focus-gained  (fn [e] (c/config! e :background bg-hover :cursor :hand) (.repaint (c/to-root e)))
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
                (= d-inp "") (= d-inp "description") (= d-inp nil) (< (count f-inp) 4)) false true))
    1 (let [cmp-inp (:component-type @cmpts-atom)
            col-inp (:column-type @cmpts-atom)]
        (if (or (= cmp-inp nil) 
                (= col-inp nil) 
                ) false true))
    2 (let [p-inp (:private? @cmpts-atom)
            e-inp (:editable? @cmpts-atom)]
        (if (or (= p-inp nil) 
                (= e-inp nil)) false true))
    true))


;;; fixme:aleks
;; (defn multi-panel
;;   "Description:
;;     get vector of panels and return mig-panel in which these panels are replaced on click of arrow
;;    Example:
;;     (multi-panel [some-panel-1 some-panel-2 some-panel-3] title 0)"
;;   [panels cmpts-atom table-name title num]
;;   (let [btn-panel (menu-bar
;;                    {:id :db-viewer--component--menu-bar
;;                     :buttons [["Back"
;;                                (gs/icon GoogleMaterialDesignIcons/ARROW_BACK face/c-icon)
;;                                (fn [e]
;;                                  (if (= num 0)
;;                                    (c/config!
;;                                     (.getParent (.getParent (seesaw.core/to-widget e)))
;;                                     :items [[(multi-panel panels cmpts-atom table-name title num)]])
;;                                    (c/config!
;;                                     (.getParent (.getParent (seesaw.core/to-widget e)))
;;                                     :items [[(multi-panel panels cmpts-atom table-name title (- num 1))]])))]
;;                               ["Next"
;;                                (gs/icon GoogleMaterialDesignIcons/ARROW_FORWARD face/c-icon)
;;                                (fn [e] (if (validate-fields cmpts-atom num)
;;                                          (if
;;                                              (=  num (- (count panels) 1))
;;                                            (c/config! (.getParent
;;                                                        (.getParent (seesaw.core/to-widget e)))
;;                                                       :items [[(multi-panel panels cmpts-atom table-name title num)]])
;;                                            (c/config! (.getParent (.getParent (seesaw.core/to-widget e)))
;;                                                       :items [[(multi-panel panels cmpts-atom table-name title (+ num 1))]]))))]]})
;;         btn-back (first (.getComponents btn-panel))
;;         btn-next (second (.getComponents btn-panel))]
;;    ;; (c/config! btn-panel :bounds [0 0 0 0])
;;     (c/config! btn-next
;;                :border (b/compound-border (b/empty-border :left 0 :right 5 :top 3 :bottom 3) (b/line-border :thickness 1 :color "#bbb")))
;;     (c/config! btn-back
;;              :border (b/compound-border (b/empty-border :left 0 :right 5 :top 3 :bottom 3) (b/line-border :thickness 1 :color "#bbb"))
;;              :visible? (if (= num 0) false true))
;;     (if (= num (- (count panels) 1))
;;       (c/config! btn-next :text "Save" :listen [:mouse-clicked (fn [e]
;;                                                                  ;;(println @cmpts-atom)
;;                                                                  (swap! cmpts-atom  assoc :field-qualified (str (:field @cmpts-atom) "." table-name))
;;                                                                  ;; (println (:output (mt/validate-one-column
;;                                                                  ;;                       @cmpts-atom)))
;;                                                                  (if (:valid? (mt/validate-one-column
;;                                                                                @cmpts-atom))
;;                                                                    ((state/state :alert-manager) :set {:header "Success" :body "Column was added"} 5)
;;                                                                    ((state/state :alert-manager) :set {:header "Error" :body "All fields must be entered and must be longer than 3 chars"} 5)))]))
;;     (mig-panel
;;      :constraints ["wrap 2" "0px[left]0px" "0px[]0px"]
;;      :preferred-size [910 :by 360]
;;      :background light-light-grey-color
;;      :items [[(c/label :border (b/empty-border :right 18))]
;;              [btn-panel "align r"]
;;              [(c/label :text title
;;                      :foreground "#256599"
;;                      :border (b/empty-border :left 10)) "span 2"]
;;              [(nth panels num) "span 2"]])))


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
                                :font (gs/getFont :bold)
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



;;; fixme:aleks
;;;----------------------------
;; (def select-box-table-list
;;   "Description:
;;      Combobox with all tables."
;;   (fn [{:keys [local-changes store-id val] 
;;         :or {local-changes (atom {})
;;              store-id :documents.table
;;              val nil}}]
;;     ;;(println "\ntable-select-box" store-id val)
;;     (select-box (vec (map #(get % :table_name) (jarman.logic.metadata/getset)))
;;                :store-id store-id
;;                :local-changes local-changes
;;                :selected-item (rift val ""))))

 

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

(defn doom-wather [root]
  (add-watch (state/state :atom-app-size) :doom-watcher
             (fn [key atom old-state new-state]
               (let [w (first @atom) 
                     h (/ (second @atom) 3)
                     x 0
                     y (- (second @atom) h)]
                 (c/config! root :bounds [x y w h])))))

(defn doom
  ([] (doom (rift (state/state :doom-compo) [])))
  ([compo]
   (if-not (state/state :doom)
     (let [JLP   (state/state :app)
           fsize @(state/state :atom-app-size)
           w     (first fsize)
           h     (/ (second fsize) 3)
           panel (c/vertical-panel
                  :border (b/line-border :left 2 :top 2 :right 2 :color "#000")
                  :bounds [0 (- (second fsize) h) w h] 
                  :items [compo])]
       (state/set-state :doom-compo compo)
       (state/set-state :doom panel)
       (.add JLP panel (Integer. 1010))
       (.revalidate JLP)
       (.repaint JLP)
       (doom-wather panel))
     (if (state/state :doom) (.setVisible (state/state :doom) true)))))

(defn doom-hide [] (.setVisible (state/state :doom) false))

(defn doom-rm []
  (let [JLP  (state/state :app)
        doom (state/state :doom)]
    (if doom
      (try
        (remove-watch (state/state :atom-app-size) :doom-watcher)
        (.remove JLP doom)
        (state/rm-state :doom-compo)
        (state/rm-state :doom)
        (.revalidate JLP)
        (.repaint JLP)
        ))))

(comment
  (doom (let [w 220 h 220]
         (gmg/migrid
          :> 
          [(label-img "ricardo.gif" w h)
           (label-img "ricardo.gif" w h)
           (label-img "ricardo.gif" w h)])))
  (doom-rm))

