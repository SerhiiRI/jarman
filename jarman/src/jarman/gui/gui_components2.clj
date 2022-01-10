(ns jarman.gui.gui-components2
  (:require [seesaw.core    :as c]
            [seesaw.border  :as b]
            [seesaw.chooser :as chooser]
            ;; Jarman
            [jarman.tools.lang     :refer :all]
            [jarman.faces          :as face]
            [jarman.gui.gui-style  :as gui-style]
            [jarman.gui.core       :refer [satom register!]]
            [jarman.gui.gui-tools  :as gui-tool]
            [jarman.gui.gui-panel  :refer [event-wrapper]])
  (:import ;; (java.awt Color)
           ;; (java.awt Dimension)
           ;; (jarman.jarmanjcomp CustomScrollBar)
           (jiconfont.icons.google_material_design_icons GoogleMaterialDesignIcons)))

;;; fixme:aleks button arguments
;;; Warning! first fix `fixme` in `text` below, after change `button`
;;; 1. rename arguments, like in input field below
;;;    - `tgap` `bgap`... to one `border` vector with left-right-top-bottom
;;;    - the same problems with backgrounds and underlines
;;; 2. remake wrapper
(defn button [& {:keys [value tgap bgap lgap rgap halign tip bg bg-hover underline-size underline-focus underline flip-border
                       on-click on-focus-gain on-focus-lost on-mouse-enter on-mouse-exit args]    
                :or   {value ""
                       tgap 10 bgap 10 lgap 10 rgap 10
                       tip    ""
                       halign :center
                       bg              face/c-btn-bg
                       bg-hover        face/c-btn-bg-focus
                       underline-focus face/c-btn-underline-on-focus
                       underline       face/c-btn-underline
                       underline-size  face/s-btn-underline
                       flip-border     false

                       on-click        (fn [e] e)
                       on-focus-gain   (fn [e] e)
                       on-focus-lost   (fn [e] e)
                       on-mouse-enter  (fn [e] e)
                       on-mouse-exit   (fn [e] e)
                       
                       args []}}]
  (blet
   (apply c/label
          :text value
          :focusable? true
          :halign halign
          :tip tip
          :listen
          [:mouse-clicked
           (fn [e]
             (on-click e)
             (gui-tool/switch-focus))
           :mouse-entered
           (fn [e]
             (c/config! e :border (new-border underline-focus) :background bg-hover :cursor :hand)
             (.repaint (c/to-root e))
             (on-mouse-enter e))
           :mouse-exited
           (fn [e]
             (c/config! e :border (new-border underline) :background bg)
             (.repaint (c/to-root e))
             (on-mouse-exit e))
           :focus-gained
           (fn [e]
             (c/config! e :border (new-border underline-focus) :background bg-hover :cursor :hand)
             (on-focus-gain e))
           :focus-lost
           (fn [e]
             (c/config! e :border (new-border underline) :background bg)
             (on-focus-lost e))
           :key-pressed
           (fn [e]
             (when (= (.getKeyCode e) java.awt.event.KeyEvent/VK_ENTER)
               (on-click e)
               (gui-tool/switch-focus)))]
          :background bg
          :border (new-border underline)
          args)
   [new-border
    (fn [underline-color]
      (b/compound-border (b/empty-border :bottom bgap :top tgap :left lgap :right rgap)
                         (b/line-border (if flip-border :top :bottom) underline-size :color underline-color)))]))

(defn text
  [& {:keys [value border border-color-focus border-color-unfocus placeholder font-size background char-limit start-underline
             on-change on-focus-gain on-focus-lost on-caret-update
             args]
      :or   {value                 ""
             border                [10 10 5 5 2]
             border-color-focus    face/c-underline-on-focus
             border-color-unfocus  face/c-underline
             placeholder           ""
             background            face/c-input-bg
             start-underline       nil
             char-limit            0
             
             on-change             (fn [e] e)
             on-focus-gain         (fn [e] e)
             on-focus-lost         (fn [e] e)
             on-caret-update       (fn [e] e)

             args []}}]
  (let [;; fixme:aleks gui-components2/text input field
        ;; 1. `get-user-data`, `set-user-data` can be removed?
        ;; 2. write macro for border. Logic is understandable, but 
        ;;    it can be look better. Or you can make binding withou
        ;;    `nth`, than, go on!
        last-v          (atom "")
        get-user-data   (fn [e k]   (get-in   (c/config e :user-data) [k]))
        set-user-data   (fn [e k v] (assoc-in (c/config e :user-data) [k] v))
        new-border      (fn [underline-color]
                          (b/compound-border (b/empty-border :left (nth border 0) :right (nth border 1) :top (nth border 2) :bottom (nth border 3))
                                             (b/line-border :bottom (nth border 4) :color underline-color)))]
    (-> 
     (partial c/text
        :text       (if (empty? value) placeholder (str value))
        :background background
        :border     (new-border (rift start-underline border-color-unfocus))
        :user-data  {:placeholder placeholder :value "" :edit? false :type :input :border-fn new-border}
        :listen
        [:focus-gained
         (fn [e]
           (c/config! e :border (new-border border-color-focus))
           (cond (= (c/value e) placeholder) (c/config! e :text ""))
           (c/config! e :user-data (set-user-data e :edit? true))
           (on-focus-gain e))
         :focus-lost
         (fn [e]
           (c/config! e :border (new-border border-color-unfocus))
           (cond (= (c/value e) "") (c/config! e :text placeholder))
           (c/config! e :user-data (set-user-data e :edit? false))
           (on-focus-lost e))
         :caret-update
         (fn [e]
           (let [new-v (c/value (c/to-widget e))]
             (if (and (> (count new-v) char-limit) (< 0 char-limit))
               (c/invoke-later (c/config! e :text @last-v))
               (do
                 (reset! last-v new-v)
                 (c/config! e :user-data (set-user-data e :value (if (= placeholder @last-v) "" @last-v)))
                 (on-change e)))))])
     (apply args))))

(defn event-component-wrapper [component-type component]
  (fn [& {:keys [event-hook-atom event-hook] :as arguments}]
    (let [clean-arg (dissoc arguments :event-hook :event-hook-atom)]
      (let [panel (apply component (interleave (keys clean-arg) (vals clean-arg)))
            panel-hash (keyword (str component-type (.hashCode panel)))]
        (if event-hook (register! event-hook-atom panel-hash (partial event-hook panel)))
        panel))))

(defn status-input-file
  "Description:
     File choser, button with icon, when path is selected it changes background color"
  [& {:keys [value selection-mode on-change] :or {value "" selection-mode false on-change (fn [e] e)}}]
  (let [state             (satom {:status :not-selected})
        not-choosed-icon  (gui-style/icon GoogleMaterialDesignIcons/ATTACHMENT        face/c-icon 17)
        choosed-icond     (gui-style/icon GoogleMaterialDesignIcons/INSERT_DRIVE_FILE face/c-icon 17)
        icon-chooser      (fn [compn path] (if-not (empty? path)
                                            (c/config! compn :icon choosed-icond :tip path)
                                            (c/config! compn :icon not-choosed-icon :tip "Please choose")))
        icon-button       (button :text ""
                                  :tgap 4 :bgap 4 :lgap 0 :rgap 0
                                  :args [:icon not-choosed-icon]
                                  :on-click
                                  (fn [e] 
                                    #dbg (let [new-path (chooser/choose-file
                                                         :selection-mode (if selection-mode :dirs-only :files-only)
                                                         :suggested-name value
                                                         :success-fn  (fn [fc file] (.getAbsolutePath file)))]
                                           (icon-chooser (.getComponent e) new-path)
                                           (on-change (seesaw.core/label :text new-path)))))]
    (icon-chooser icon-button value)
    icon-button))


(comment
  (c/frame
   :content
   (seesaw.mig/mig-panel
    :background  face/c-compos-background-darker
    :constraints ["wrap 1" "0px[grow, fill]0px" "0px[fill, top]0px"]
    :border (b/empty-border :thickness 10)
    :items [[(seesaw.core/label :text "site" :font (gtool/getFont :bold 20))]
            [(ccomp/url-panel    {:on-change (on-change :seal.site) :default {}})]
            ;; [(seesaw.core/label :text "file" :font (gtool/getFont :bold 20))]
            ;; [(ccomp/file-panel   {:on-change (on-change :seal.file) :on-download (on-downld :seal.file) :default {} :mode false ;; (:insert-mode (state!))
            ;;                       })]
            ;; [(seesaw.core/label :text "ftpf" :font (gtool/getFont :bold 20))]
            ;; [(ccomp/ftp-panel    {:on-change (on-change :seal.ftp-file) :on-download (on-downld :seal.ftp-file) :default {} :mode false ;;  (:insert-mode (state!))
            ;;                       })]
            ])
   :title "Jarman" :size [1000 :by 800])
  
  (-> (doto (seesaw.core/frame
             :title "Jarman" 
             :content (status-input-file))
        (.setLocationRelativeTo nil) seesaw.core/pack! seesaw.core/show!))
  )


