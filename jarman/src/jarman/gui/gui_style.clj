;;  ____ _______   ___     _____ _
;; / ___|_   _\ \ / / |   | ____( )___
;; \___ \ | |  \ V /| |   |  _| |// __|
;;  ___) || |   | | | |___| |___  \__ \
;; |____/ |_|   |_| |_____|_____| |___/
;; ------------------------------------

(ns jarman.gui.gui-style
  (:use seesaw.mig)
  (:require [clojure.java.io :as io]
            [seesaw.core   :as c]
            [seesaw.border :as b]
            [seesaw.color :refer [color]]
            [jarman.logic.state :as state]
            [jarman.gui.components.swing :as swing]
            [jarman.config.environment :as env]
            [jarman.lang :refer :all]
            [jarman.faces :as face])
  (:import (jiconfont.swing IconFontSwing)
           (jiconfont.icons.google_material_design_icons GoogleMaterialDesignIcons)
           (javax.swing.text SimpleAttributeSet)
           (javax.swing.plaf ColorUIResource)
           (javax.swing UIManager)
           (javax.swing BorderFactory)
           (java.awt MouseInfo)
           (java.awt Font)
           (java.awt Color)
           (java.io File)
           (java.nio.file Files)
           (java.awt GraphicsEnvironment)
           (java.io InputStream)
           (javax.swing JLayeredPane)
           (javax.sound.sampled AudioInputStream)
           (javax.sound.sampled AudioSystem)
           (javax.sound.sampled Clip)))

(defn- swing-compos-list "Description: Default swing layout." []
  ["Button" "ToggleButton" "RadioButton" "CheckBox" "ColorChooser"
   "Label" "Table" "TextField" "PasswordField" "TextArea" "TextPane"
   "EditorPane" "ToolTip"])

(defn- swing-layouts-list "Description:  Default swing components." []
  ["ComboBox" "List" "MenuBar" "MenuItem" "RadioButtonMenuItem"
   "CheckBoxMenuItem" "Menu" "PopupMenu" "OptionPane" "Panel"
   "ProgressBar" "ScrollPane" "Viewport" "TabbedPane"
   "TitledBorder" "ToolBar" "Tree" "TableHeader"])

(defn- swing-all-compos []
  (concat (swing-layouts-list) (swing-compos-list)))

(defn- compo-list-suffix
  "Description:
    Add suffix to point component parameter."
  [compos suffix]
  (doall (map #(str % suffix) compos)))

;;;;;;;;;;;;;
;;; FONTS ;;;
;;;;;;;;;;;;;

(defn ^:depr fonts []
  (comment
    ;; WARNING!!!
    ;; ENABLE WHEN YOU'LL WOULD FIND ALL USAGES
    (catch-component-trace "Deprecated function usage `font`, `getFont`"))
  (let [f (Font. face/f-regular Font/PLAIN face/s-foreground)]
   {:plain     f
    :bold      f
    :bold-i    f
    :italic    f
    :light     f
    :light-i   f
    :medium    f
    :medium-i  f
    :mono      f
    :mono-b    f
    :mono-bi   f
    :mono-i    f
    :thin      f
    :condensed f}))

(defn ^:depr getFont
  ([] (fonts))
  ([sizeorfont] (if (keyword? sizeorfont)
                  (sizeorfont (fonts))
                  (getFont :plain sizeorfont)))
  ([font size] (.deriveFont (font (fonts)) (float size))))

(defn update-fonts []
  (where
    ((font (Font. face/f-regular Font/PLAIN face/s-foreground))
     (compo-list (compo-list-suffix (swing-all-compos) ".font")))
    (swing/register-fonts env/resource-fonts)
    (doall (doseq [^String component-prop compo-list]
             (UIManager/put component-prop font)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; UI ELEMENTS UPDATE ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- update-scrollbar []
  (UIManager/put "ScrollBar.width" (Integer. 12))
  ;; fixme, cover the scrollbar faces
  (comment
    (UIManager/put "ScrollBar" (color "#fff"))
    (UIManager/put "ScrollBar.thumb" (color "#000"))
    (UIManager/put "ScrollBar.trackHighlightForeground" (color "#000"))
    (UIManager/put "ScrollBar.border" (color "#000"))
    (UIManager/put "ScrollBar.width" (Integer. 12))
    (UIManager/put "ScrollBar.squareButtons" false)
    (UIManager/put "ScrollBar.allowsAbsolutePositioning" true)
    (UIManager/put "ScrollBar.thumbHeight" (Integer. 0))
    (UIManager/put "ScrollBar.thumbDarkShadow" (color "#000"))
    (UIManager/put "ScrollBar.thumbShadow" (color "#000"))
    (UIManager/put "ScrollBar.thumbHighlight" (color "#000"))
    (UIManager/put "ScrollBar.trackForeground" (color "#000"))
    (UIManager/put "ScrollBar.trackHighlight" (color "#000"))
    (UIManager/put "ScrollBar.foreground" (color "#000"))
    (UIManager/put "ScrollBar.shadow" (color "#000"))
    (UIManager/put "ScrollBar.highlight" (color "#000"))))

(defn update-table-header []
  (let [tgap 0 lgap 0 bgap 0 rgap 1
        tgap-in 2 lgap-in 2 bgap-in 2 rgap-in 2]
   (UIManager/put "TableHeader.foreground" (color face/c-table-header-fg))
   (UIManager/put "TableHeader.background" (color face/c-table-header-bg))
   (UIManager/put "TableHeader.cellBorder" (BorderFactory/createCompoundBorder
                                             (BorderFactory/createMatteBorder tgap lgap bgap rgap (color face/c-table-header-border))
                                             (BorderFactory/createEmptyBorder tgap-in lgap-in bgap-in rgap-in)))))

(defn update-table []
  (UIManager/put "Table.selectionForeground"      (color face/c-table-select-row-fg))
  (UIManager/put "Table.selectionBackground"      (color face/c-table-select-row-bg))
  (UIManager/put "Table.focusCellHighlightBorder" (BorderFactory/createLineBorder (color face/c-table-select-cell)))
  ;; fixme, don't work
  (comment (UIManager/put "Table.scrollPaneBorder" (BorderFactory/createLineBorder (color "#fff")))
           (UIManager/put "Table.highlight" (color "#0f0"))))

(defn update-layouts-background []
  (doall (map #(UIManager/put % (ColorUIResource. (color face/c-layout-background)))
           (compo-list-suffix (swing-layouts-list) ".background"))))

(defn update-compos-background []
  (doall (map #(UIManager/put % (ColorUIResource. (color face/c-compos-background)))
           (compo-list-suffix (swing-compos-list) ".background"))))

(defn update-foreground []
  (doall (map #(UIManager/put % (color face/c-foreground))
           (compo-list-suffix (swing-all-compos) ".foreground"))))

(defn update-caret []
  (doall (map #(UIManager/put % (color face/c-caret))
           (compo-list-suffix (swing-all-compos) ".caretForeground"))))

;; SOME DEFAULT PRESETS
(state/set-state :theme-name "Jarman Light")
(IconFontSwing/register (GoogleMaterialDesignIcons/getIconFont))

(defn load-style "Load global style" []
  (update-fonts)
  (update-foreground)
  (update-caret)
  (update-layouts-background)
  (update-compos-background)
  (update-scrollbar)
  (update-table-header)
  (update-table))

;;; Font icons

(defn ^:depr icon
  "Description:
     Icon font wraper.
   Example:
     (icon GoogleMaterialDesignIcons/EDIT)"
  ([ico]       (icon ico face/c-icon 20))
  ([ico color-hex] (icon ico color-hex 20))
  ([ico color-hex size]
   (IconFontSwing/buildIcon ico size (color color-hex))))

(defn ^:depr get-mouse-pos
  "Description:
     Return mouse position on screen, x and y.
  Example:
     (get-mouse-pos) => [800.0 600.0]"
  []
  (let [mouse-pos (.getLocation (MouseInfo/getPointerInfo))
        screen-x  (.getX mouse-pos)
        screen-y  (.getY mouse-pos)]
    [screen-x screen-y]))

;;  ____  _____ __  __  ___
;; |  _ \| ____|  \/  |/ _ \
;; | | | |  _| | |\/| | | | |
;; | |_| | |___| |  | | |_| |
;; |____/|_____|_|  |_|\___/

(comment
  (swing/quick-frame
    [(seesaw.core/label :text "Some text i should to place here" :font (Font. Font/SERIF Font/PLAIN 20))
     (seesaw.core/label :text "Some text i should to place here" :font (Font. "Ubuntu Light" Font/PLAIN 20))
     (seesaw.core/label :text "Some text i should to place here" :font (Font. "Ubuntu Regular" Font/PLAIN 20))
     (seesaw.core/label :text "Some text i should to place here" :font (Font. "Ubuntu Medium" Font/PLAIN 20))
     (seesaw.core/label :text "Some text i should to place here" :font (Font. "Ubuntu Bold" Font/PLAIN 20))
     (seesaw.core/label :text "Some text i should to place here" :font (Font. "Ubuntu Light Italic" Font/PLAIN 20))
     (seesaw.core/label :text "Some text i should to place here" :font (Font. "Ubuntu Italic" Font/PLAIN 20))
     (seesaw.core/label :text "Some text i should to place here" :font (Font. "Ubuntu Medium Italic" Font/PLAIN 20))
     (seesaw.core/label :text "Some text i should to place here" :font (Font. "Ubuntu Bold Italic" Font/PLAIN 20))
     (seesaw.core/label :text "Some text i should to place here" :font (Font. "JetBrains Mono Light" Font/PLAIN 20))
     (seesaw.core/label :text "Some text i should to place here" :font (Font. "JetBrains Mono Regular" Font/PLAIN 20))
     (seesaw.core/label :text "Some text i should to place here" :font (Font. "JetBrains Mono Medium" Font/PLAIN 20))
     (seesaw.core/label :text "Some text i should to place here" :font (Font. "JetBrains Mono Bold" Font/PLAIN 20))
     (seesaw.core/label :text "Some text i should to place here" :font (Font. "JetBrains Mono ExtraBold" Font/PLAIN 20))
     (seesaw.core/label :text "Some text i should to place here" :font (Font. "JetBrains Mono Light Italic" Font/PLAIN 20))
     (seesaw.core/label :text "Some text i should to place here" :font (Font. "JetBrains Mono Italic" Font/PLAIN 20))
     (seesaw.core/label :text "Some text i should to place here" :font (Font. "JetBrains Mono Medium Italic" Font/PLAIN 20))
     (seesaw.core/label :text "Some text i should to place here" :font (Font. "JetBrains Mono Bold Italic" Font/PLAIN 20))
     (seesaw.core/label :text "Some text i should to place here" :font (Font. "JetBrains Mono ExtraBold Italic" Font/PLAIN 20))
     (seesaw.core/label :text "Some text i should to place here" :font {:name "JetBrains Mono ExtraBold Italic" :size 20})])

  ;; check if icon is loaded
  (defn- render-demo []
    (mig-panel
      :constraints ["" "[50%, center]" "[50%, center]"]
      :background  (color "#9cb")
      :items [[(c/label :text "Demo 1" :icon (icon GoogleMaterialDesignIcons/SAVE))]
              [(c/label :text "Demo 2" :icon (icon GoogleMaterialDesignIcons/CLOSE "#f00" 30))]]))

  (defn style-demo []
    (let [offset-x (atom 0)
          offset-y (atom 0)
          frame (seesaw.core/frame
                  :title "Style demo"
                  :minimum-size [500 :by 500]
                  :size [500 :by 500]
                  :content (render-demo))
          frame (c/config!
                  frame :listen [:mouse-pressed
                                 (fn [e]
                                   (let [[start-x start-y] (get-mouse-pos)]
                                     (reset! offset-x (- start-x (.getX frame)))
                                     (reset! offset-y (- start-y (.getY frame)))))

                                 :mouse-dragged
                                 (fn [e]
                                   (let [[new-x new-y] (get-mouse-pos)]
                                     (.setLocation frame (- new-x @offset-x)(- new-y @offset-y))))])]
      (doto
          frame
        ;; (.setUndecorated true)
        ;; (.setOpacity 0.7)
        (.setLocationRelativeTo nil)
        c/pack!
        c/show!)))

  (style-demo)


  ;; ┌───────────────┐
  ;; │               │
  ;; │     GIFT      │
  ;; │               │
  ;; └───────────────┘
  (defn- move-in-time-x [root obj x y w h x-step f-x delay]
    (.start
      (Thread.
        (fn []
          (do
            (Thread/sleep (* 1000 delay))
            (c/config! obj :bounds [x y w h])
            (.repaint root)
            (if (> x-step 0)
              (if (< (.getX (c/config obj :bounds)) f-x)
                (move-in-time-x root obj (+ x x-step) y w h x-step f-x delay))
              (if (> (.getX (c/config obj :bounds)) f-x)
                (move-in-time-x root obj (+ x x-step) y w h x-step f-x delay)))
            )))))

  (defn- move-in-time-y [root obj x y w h y-step f-y delay]
    (.start
      (Thread.
        (fn []
          (do
            (Thread/sleep (* 1000 delay))
            (c/config! obj :bounds [x y w h])
            (.repaint root)
            (if (> y-step 0)
              (if (< (.getY (c/config obj :bounds)) f-y)
                (move-in-time-y root obj x (+ y y-step) w h y-step f-y delay))
              (if (> (.getY (c/config obj :bounds)) f-y)
                (move-in-time-y root obj x (+ y y-step) w h y-step f-y delay)))
            )))))

  (def label-img
    (fn [file-path w h]
      (let [img (clojure.java.io/file (str "icons/imgs/" file-path))
            gif (c/label :text (str "<html> <img width=\"" w "\" height=\"" h "\" src=\"file:" img "\">"))]
        gif)))

  (defn shooting-stars []
    (if (or (= false (state/state :shooting-stars)) (nil? (state/state :shooting-stars)))
      (let [JLP (state/state :app)
            dub1 (label-img "egg.gif" 150 130)
            dub2 (label-img "egg.gif" 150 130)
            dub3 (label-img "egg.gif" 150 130)
            dub4 (label-img "egg.gif" 150 130)
            dub5 (label-img "egg.gif" 150 130)
            dub6 (label-img "egg.gif" 150 130)
            hb  (label-img "hb.gif"  300 150)
            cake  (label-img "cake.gif"  200 200)]
        (state/set-state :shooting-stars true)
        ;; dub pepe
        (move-in-time-x JLP dub1
          -150
          (- (second @(state/state :atom-app-size)) 130)
          150 130 5
          (- (/ (first @(state/state :atom-app-size)) 2) 250)
          0.05)
        (timelife 1 #(move-in-time-x JLP dub2
                       -150
                       (- (second @(state/state :atom-app-size)) 130)
                       150 130 5
                       (- (/ (first @(state/state :atom-app-size)) 2) 350)
                       0.05))
        (timelife 2 #(move-in-time-x JLP dub3
                       -150
                       (- (second @(state/state :atom-app-size)) 130)
                       150 130 5
                       (- (/ (first @(state/state :atom-app-size)) 2) 450)
                       0.05))

        (move-in-time-x JLP dub4
          (first @(state/state :atom-app-size))
          (- (second @(state/state :atom-app-size)) 130)
          150 130 -5
          (+ (/ (first @(state/state :atom-app-size)) 2) 100)
          0.05)
        (timelife 1 #(move-in-time-x JLP dub5
                       (first @(state/state :atom-app-size))
                       (- (second @(state/state :atom-app-size)) 130)
                       150 130 -5
                       (+ (/ (first @(state/state :atom-app-size)) 2) 200)
                       0.05))
        (timelife 2 #(move-in-time-x JLP dub6
                       (first @(state/state :atom-app-size))
                       (- (second @(state/state :atom-app-size)) 130)
                       150 130 -5
                       (+ (/ (first @(state/state :atom-app-size)) 2) 300)
                       0.05))

        (timelife 2 #(move-in-time-y JLP hb
                       (- (/ (first @(state/state :atom-app-size)) 2) 150)
                       -150
                       300 150 5
                       -20
                       0.05))
        (timelife 3 #(move-in-time-y JLP cake
                       (- (/ (first @(state/state :atom-app-size)) 2) 100)
                       (second @(state/state :atom-app-size))
                       200 200 -5
                       (- (second @(state/state :atom-app-size)) 200)
                       0.08))
        ;; run pepe
        (.add JLP dub1 (Integer. 1002))
        (.add JLP dub2 (Integer. 1003))
        (.add JLP dub3 (Integer. 1004))
        (.add JLP dub4 (Integer. 1005))
        (.add JLP dub5 (Integer. 1006))
        (.add JLP dub6 (Integer. 1007))
        (.add JLP hb (Integer. 1000))
        (.add JLP cake (Integer. 1001))
        (let [audioIn (AudioSystem/getAudioInputStream (clojure.java.io/file "./icons/imgs/stars.wav"))
              clip    (javax.sound.sampled.AudioSystem/getClip)]
          (.open clip audioIn)
          (.start clip))
        (timelife 33
          #(do
             (.remove JLP dub1)
             (.remove JLP dub2)
             (.remove JLP dub3)
             (.remove JLP dub4)
             (.remove JLP dub5)
             (.remove JLP dub6)
             (.remove JLP hb)
             (.remove JLP cake)
             (state/set-state :shooting-stars false)
             (.revalidate JLP)
             (.repaint JLP)))
        (.revalidate JLP)
        (.repaint JLP)))))
