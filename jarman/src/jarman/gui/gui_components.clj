(ns jarman.gui.gui-components
  (:use seesaw.dev
        seesaw.mig)
  (:require [jarman.resource-lib.icon-library :as icon]
            [seesaw.core :as c]
            [seesaw.border :as b]
            [seesaw.util :as u]
            [seesaw.mig :as smig]
            [jarman.gui.gui-seed :as gseed]
            [jarman.tools.swing :as stool]
            [jarman.tools.lang :as lang]
            [jarman.logic.metadata :as mmeta]
            [jarman.gui.gui-alerts-service :as gas]
            [jarman.gui.gui-tools :as gtool])
  (:import (java.awt Color)))

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
  ([line-size line-color] (c/label :border (b/line-border :top line-size :color line-color))))

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

;; (defn scrollbox
;;   [component & args]
;;   (let [scr (apply scrollable component :border nil args)]  ;; speed up scrolling
;;     (.setUnitIncrement (.getVerticalScrollBar scr) 20)
;;     (.setBorder scr nil)
;; ;;    for hide scroll    
;;    (.setPreferredSize (.getVerticalScrollBar scr) (java.awt.Dimension. 12 0)) 
;;     scr))


(defn vmig
  [& {:keys [items
             wrap
             lgap
             rgap
             tgap
             bgap
             vrules
             hrules
             args]
      :or {items [[(c/label)]]
           wrap 1
           lgap 0
           rgap 0
           tgap 0
           bgap 0
           vrules "[grow, fill]"
           hrules "[grow, fill]"
           args []}}]
  (apply mig-panel
         :constraints [(str "wrap " wrap) (str lgap "px" hrules rgap "px") (str tgap "px" vrules bgap "px")]
         :items items
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
             args]
      :or {items [[(c/label)]]
           wrap ""
           lgap 0
           rgap 0
           tgap 0
           bgap 0
           vrules "[grow, fill]"
           hrules "[grow, fill]"
           args []}}]
  (apply mig-panel
         :constraints [wrap (str lgap "px" hrules rgap "px") (str tgap "px" vrules bgap "px")]
         :items items
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
  "Description
     TextArea with word wrap"
  [txt & args] `(c/label :text `~(gtool/htmling ~txt) ~@args))

(defn button-basic
  "Description:
      Simple button with default style.
   Example:
      (simple-button \"Simple button\" (fn [e]) :style [:background \"#fff\"])"
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
                 unfocus-color]
          :or   {onClick (fn [e])
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
                 unfocus-color (gtool/get-color :decorate :focus-lost)}}]
  (let [newBorder (fn [underline-color]
                    (b/compound-border (b/empty-border :bottom bgap :top tgap :left lgap :right rgap)
                                       (b/line-border :bottom underline-size :color underline-color)))]
    (apply c/label
           :text txt
           :focusable? true
           :halign halign
           :listen [:mouse-clicked onClick
                    :mouse-entered (fn [e] (c/config! e :background mouse-in) (gtool/hand-hover-on e))
                    :mouse-exited  (fn [e] (c/config! e :background mouse-out))
                    :focus-gained  (fn [e] (c/config! e :border (newBorder focus-color)))
                    :focus-lost    (fn [e] (c/config! e :border (newBorder unfocus-color)))
                    :key-pressed   (fn [e] (if (= (.getKeyCode e) java.awt.event.KeyEvent/VK_ENTER) onClick))]
           :background mouse-out
           :border (newBorder unfocus-color)
           args)))


(defn button-slim
  [txt
   & {:keys [onClick args]
      :or {onClick (fn [e])
           args []}}]
  (button-basic
   txt
   :onClick onClick
   :tgap 5
   :bgap 4
   :underline-size 1
   :halign :left
   :args args))


(defn button-export
  [txt onClick]
  (button-basic txt 
                :onClick onClick
                :mouse-in (gtool/get-comp :button-export :mouse-in)
                :mouse-out (gtool/get-comp :button-export :mouse-out)
                :unfocus-color (gtool/get-comp :button-export :unfocus-color)))

(defn button-return
  [txt func]
  (button-basic txt 
                :onClick func
                :mouse-in (gtool/get-comp :button-return :mouse-in)
                :mouse-out (gtool/get-comp :button-return :mouse-out)
                :unfocus-color (gtool/get-comp :button-return :unfocus-color)
                :args [:foreground (gtool/get-comp :button-return :foreground)]))

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

(defn input-checkbox
  [& {:keys [txt
             val
             font-size
             enabled?
             local-changes
             store-id
             args]
      :or   {
             txt ""
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

(defn inpose-label
  "Params: 
     title
     component
   "
  [title component
   & {:keys [tgap
             font-color
             id]
      :or {tgap 0
           font-color "#000"
           id nil}}] 
  (c/grid-panel :columns 1
                :id id
                :border (b/empty-border :top tgap)
                :items [(c/label :text (if (string? title) title "")
                                 :foreground font-color )
                        component]))

(defn input-text-area
  "Description:
    Text component converted to c/text component with placeholder. Placehlder will be default value.
 Example:
    (input-text :placeholder \"Login\" :style [:halign :center])"
  ([& {:keys [store-id
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
       (scrollbox text-area :minimum-size [50 :by 100])))))

(defn input-text-with-atom
  [& {:keys [store-id local-changes val editable? enabled? store-orginal onClick border-color-focus border-color-unfocus debug]
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
  (if-not (empty? (str val)) (swap! local-changes (fn [storage] (assoc storage store-id val))))
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
                   :caret-update (fn [e]
                                   (let [new-v (c/value (c/to-widget e))]
                                     (cond
                                       (and (not (nil? store-id))
                                            (not (= val new-v)))
                                       (swap! local-changes (fn [storage] (assoc storage store-id new-v)))
                                       (not store-orginal) (reset! local-changes (dissoc @local-changes store-id)))))
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
   (if (and (= always-set-changes true) (not (empty? selected-item)))
     (do
       (swap! local-changes (fn [storage] (assoc storage store-id selected-item)))))
   (c/combobox :model (let [combo-model (if (empty? selected-item) (cons selected-item model) (lang/join-vec [selected-item] (filter #(not= selected-item %) model)))]
                        combo-model)
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
                                                        (not (= selected-item new-v))))
                                                  (do
                                                    (swap! local-changes (fn [storage] (assoc storage store-id new-v))))
                                                  (= always-set-changes false)
                                                  (do
                                                    (reset! local-changes (dissoc @local-changes store-id))))))])))


(defn expand-form-panel
  "Description:
     Create panel who can hide inside components. 
   Example:
     (expand-form-panel parent (component or components))
   "
  [view-layout comps
   & {:keys [min-w
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
      :or {min-w 250
           w 250
           max-w 250
           lgap 0
           rgap 0
           tgap 0
           bgap 0
           vrules "[fill]"
           ico-open nil
           icon-hide nil
           text-open "<<"
           text-hide "..."
           focus-color (gtool/get-color :decorate :focus-gained-dark)
           unfocus-color "#fff"}}]
  (let [hidden-comp (atom nil)
        hsize (if (< max-w w) (str lgap "px[" min-w ":" w ":" w ", grow, fill]" rgap "px") (str lgap "px[" min-w ":" w ":" max-w ", grow,fill]" rgap "px"))
        form-space-open ["wrap 1" hsize (str tgap "px[fill]0px" vrules bgap "px")]
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
                           :border (b/empty-border :left 2 :right 2)
                           :listen [:focus-gained (fn [e] (c/config! e :foreground focus-color))
                                    :focus-lost   (fn [e] (c/config! e :foreground unfocus-color))
                                    :mouse-entered gtool/hand-hover-on
                                    :mouse-clicked onClick
                                    :key-pressed  (fn [e] (if (= (.getKeyCode e) java.awt.event.KeyEvent/VK_ENTER) (onClick e)))])]
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
      Function need stool/image-scale function for scalling icon
      "
  (fn [txt inside-btns
       & {:keys [background
                 expand
                 border
                 vsize
                 min-height
                 ico
                 ico-hover
                 id]
          :or {background "#eee"
               expand :auto
               border (b/compound-border (b/line-border :left 6 :color background))
               vsize 35
               min-height 200
               ico  (stool/image-scale icon/plus-64-png 25)
               ico-hover (stool/image-scale icon/minus-grey-64-png 20)
               id :none}}]
    (let [atom-inside-btns (atom nil)
          inside-btns (if (nil? inside-btns) nil inside-btns)
          inside-btns (if (seqable? inside-btns) inside-btns (list inside-btns))
          ico (if (or (= :always expand) (not (nil? inside-btns))) (stool/image-scale icon/plus-64-png 25) nil)
          title (c/label
                 :text txt
                 :background (Color. 0 0 0 0))
          icon (c/label
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
        (reset! atom-inside-btns inside-btns)
        (c/config! mig
                 :id id
                 :user-data {:atom-expanded-items atom-inside-btns}
                 :items [[expand-btn]]
                 :listen [:mouse-entered gtool/hand-hover-on
                          :mouse-clicked (fn [e]
                                           (if-not (nil? @atom-inside-btns)
                                             (if (= (count (u/children mig)) 1)
                                               (do ;;  Add inside buttons to mig with expand button
                                                 (c/config! icon :icon ico-hover)
                                                 (doall (map #(.add mig %) @atom-inside-btns))
                                                 (.revalidate mig)
                                                 (.repaint mig))
                                               (do ;;  Remove inside buttons form mig without expand button
                                                 (c/config! icon :icon ico)
                                                 (doall (map #(.remove mig %) (reverse (drop 1 (range (count (u/children mig)))))))
                                                 (.revalidate mig)
                                                 (.repaint mig)))))])))))




(def button-expand-child
  "Description
     Interactive button inside menu from expand button.
   "
  (fn [title
       & {:keys [onClick args]
          :or {onClick (fn [e] (println "Clicked: " title))
               args []}}]
    (apply c/label :font (gtool/getFont)
           :text (str title)
           :background "#fff"
           :size [200 :by 25]
           :border (b/empty-border :left 10)
           :listen [:mouse-clicked onClick
                    :mouse-entered (fn [e] (c/config! e  :background "#d9ecff"))
                    :mouse-exited  (fn [e] (c/config! e  :background "#fff"))]
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
    ((def input-number :style [:halign :center])
 "
  (fn [& {:keys [style
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
(defn menu-bar
  "Description:
   Example:
      (menu-bar :buttons [[\"title1\" icon1  fn1] [\"title2\" icon2  fn2]])
      (menu-bar :id :my-id :buttons [[\"title1\"  icon1 fn1] [\"title2\" icon2  fn2]])
   "
  [& {:keys [id
             buttons]
      :or {id :none
           buttons []}}]
  (let [btn (fn [txt ico onClick & args]
              (let [border-c "#bbb"]
                (c/label
                 :font (gtool/getFont 13)
                 :text txt
                 :icon (stool/image-scale ico 27)
                 :background "#fff"
                 :foreground "#000"
                 :border (b/compound-border (b/empty-border :left 15 :right 15 :top 5 :bottom 5) (b/line-border :thickness 1 :color border-c))
                 :listen [:mouse-entered (fn [e] (c/config! e :background "#d9ecff" :foreground "#000" :cursor :hand))
                          :mouse-exited  (fn [e] (c/config! e :background "#fff" :foreground "#000"))
                          :mouse-clicked onClick])))]
    (mig-panel
     :id id
     :background (new Color 0 0 0 0)
     :constraints ["" "5px[fill]0px" "5px[fill]5px"]
     :items (if (empty? buttons) [[(c/label)]] (gtool/join-mig-items (map #(btn (first %) (second %) (last %)) buttons))))))



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
                   :id :db-viewer--component--menu-bar
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
                                                      (.getParent (seesaw.core/to-widget e))) :items [[(multi-panel panels cmpts-atom table-name
                                                                                                                    title num)]])
                                          (c/config! (.getParent (.getParent (seesaw.core/to-widget e))) :items [[(multi-panel panels cmpts-atom table-name
                                                                                                                               title (+ num 1))]]))
                                        (@gseed/alert-manager :set {:header "Error"
                                                                    :body "All fields must be entered and must be longer than 3 chars"}
                                                              (@gseed/alert-manager :message gseed/alert-manager) 5)))]])
        btn-back (first (.getComponents btn-panel))
        btn-next (second (.getComponents btn-panel))]
    (c/config! btn-next :border (b/compound-border (b/empty-border :left 0 :right 5 :top 3 :bottom 3) (b/line-border :thickness 1 :color "#bbb")))
    (c/config! btn-back
             :border (b/compound-border (b/empty-border :left 0 :right 5 :top 3 :bottom 3) (b/line-border :thickness 1 :color "#bbb"))
             :visible? (if (= num 0) false true))
    (if (= num (- (count panels) 1))
      (c/config! btn-next :text "Save" :listen [:mouse-clicked (fn [e]
                                                                 (println @cmpts-atom)
                                                                 (swap! cmpts-atom  assoc :field-qualified (str (:field @cmpts-atom) "." table-name))
                                                                 (println (:output (mmeta/validate-one-column
                                                                                       @cmpts-atom)))
                                                                 (if (:valid? (mmeta/validate-one-column
                                                                               @cmpts-atom))
                                                                 (@gseed/alert-manager :set {:header "Success"
                                                                                             :body "Column was added"}
                                                                  (@gseed/alert-manager :message gseed/alert-manager) 5)
                                                                 (@gseed/alert-manager :set {:header "Error"
                                                                                             :body "All fields must be entered and must be longer than 3 chars"}
                                                                  (@gseed/alert-manager :message gseed/alert-manager) 5)
                                                                 ))]))
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


(defn menu-bar-right
  "Description:    
   Example:
      (menu-bar :buttons [[\"title1\" icon1 fn1] [\"title2\" icon2 fn2]])
      (menu-bar :id :my-id :buttons [[\"title1\" icon1 fn1] [\"title2\" icon2 fn2]])"
  [& {:keys [id
             buttons]
      :or {id :none
           buttons []}}]
  (let [btn (fn [txt ico onClick & args]
              (let [border-c "#bbb"]
                (c/label
                 :font (gtool/getFont 13)
                 :text txt
                 :icon (stool/image-scale ico 30)
                 :background "#fff"
                 :foreground "#000"
                 :border (b/compound-border (b/empty-border :left 15 :right 15 :top 5 :bottom 5) (b/line-border :thickness 1 :color border-c))
                 :listen [:mouse-entered (fn [e] (c/config! e :background "#d9ecff" :foreground "#000" :cursor :hand))
                          :mouse-exited  (fn [e] (c/config! e :background "#fff" :foreground "#000"))
                          :mouse-clicked onClick])))]
    (mig-panel
     :id id
     :background (new Color 0 0 0 0)
     :constraints ["" "5px[grow, fill]0px[fill]5px" "5px[fill]5px"]
     :items (if (empty? buttons) [[(c/label)]] (gtool/join-mig-items (c/label) (map #(btn (first %) (second %) (last %)) buttons))))))
