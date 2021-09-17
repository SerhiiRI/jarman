(ns jarman.gui.gui-style
  (:use seesaw.mig)
  (:require [seesaw.core   :as c]
            [seesaw.border :as b]
            [jarman.logic.state :as state]
            [jarman.tools.lang :refer :all]
            [jarman.gui.builtin-themes.jarman-light]
            [jarman.faces :as face])
  (:import (javax.swing.text SimpleAttributeSet)
           (javax.swing.plaf ColorUIResource)
           (javax.swing UIManager)
           (javax.swing BorderFactory)
           (java.awt Font)
           (java.awt Color)
           (java.io File)
           (java.nio.file Files)
           (java.awt GraphicsEnvironment)
           (jiconfont IconFont DefaultIconCode)           
           (jiconfont.swing IconFontSwing)
           (java.io InputStream)
           (jiconfont.icons.google_material_design_icons GoogleMaterialDesignIcons)
           ))

(defn- ubuntu-font [font] (str "./resources/fonts/ubuntu/" font))
(defn fonts []
  {:plain    (.deriveFont (Font/createFont Font/TRUETYPE_FONT (File. (ubuntu-font "Ubuntu-R.ttf")))  (float face/s-foreground))
   :bold     (.deriveFont (Font/createFont Font/TRUETYPE_FONT (File. (ubuntu-font "Ubuntu-B.ttf")))  (float face/s-foreground))
   :bold-i   (.deriveFont (Font/createFont Font/TRUETYPE_FONT (File. (ubuntu-font "Ubuntu-BI.ttf"))) (float face/s-foreground))
   :italic   (.deriveFont (Font/createFont Font/TRUETYPE_FONT (File. (ubuntu-font "Ubuntu-RI.ttf"))) (float face/s-foreground))
   :light    (.deriveFont (Font/createFont Font/TRUETYPE_FONT (File. (ubuntu-font "Ubuntu-L.ttf")))  (float face/s-foreground))
   :light-i  (.deriveFont (Font/createFont Font/TRUETYPE_FONT (File. (ubuntu-font "Ubuntu-LI.ttf"))) (float face/s-foreground))
   :medium   (.deriveFont (Font/createFont Font/TRUETYPE_FONT (File. (ubuntu-font "Ubuntu-M.ttf")))  (float face/s-foreground))
   :medium-i (.deriveFont (Font/createFont Font/TRUETYPE_FONT (File. (ubuntu-font "Ubuntu-MI.ttf"))) (float face/s-foreground))
   
   :mono     (.deriveFont (Font/createFont Font/TRUETYPE_FONT (File. (ubuntu-font "UbuntuMono-R.ttf")))  (float face/s-foreground))
   :mono-b   (.deriveFont (Font/createFont Font/TRUETYPE_FONT (File. (ubuntu-font "UbuntuMono-B.ttf")))  (float face/s-foreground))
   :mono-bi  (.deriveFont (Font/createFont Font/TRUETYPE_FONT (File. (ubuntu-font "UbuntuMono-BI.ttf"))) (float face/s-foreground))
   :mono-i   (.deriveFont (Font/createFont Font/TRUETYPE_FONT (File. (ubuntu-font "UbuntuMono-RI.ttf"))) (float face/s-foreground))

   :thin      (.deriveFont (Font/createFont Font/TRUETYPE_FONT (File. (ubuntu-font "Ubuntu-Th.ttf"))) (float face/s-foreground))
   :condensed (.deriveFont (Font/createFont Font/TRUETYPE_FONT (File. (ubuntu-font "Ubuntu-C.ttf")))  (float face/s-foreground))})

(defn- register-fonts []
  (doall (map (fn [[name-k font]] (.registerFont (GraphicsEnvironment/getLocalGraphicsEnvironment) font))
              (fonts))))

(defn hex-to-rgb 
  "Description:
    Convert hex color to rgb.
  Example:
    (hex-to-rgb \"aaa\") => [170 170 170]"
  [color]
  (let [colors (rest (clojure.string/split color #""))
        red   (if (= 6 (count colors)) (take 2 colors) (flatten (repeat 2 (take 1 colors))))
        green (if (= 6 (count colors)) (take 2 (drop 2 colors)) (flatten (repeat 2 (take 1 (drop 1 colors)))))
        blue  (if (= 6 (count colors)) (take 2 (drop 4 colors)) (flatten (repeat 2 (take 1 (drop 2 colors)))))]
    (map #(-> (conj % "0x") (clojure.string/join) (read-string)) [red green blue])))

(defn- jcolor [red green blue]
  (Color. red green blue))
(defn hex-color
  "Description:
    Convert hex color in string and create obj color."
  [hex] (apply jcolor (hex-to-rgb hex)))

(defn- juiresource [red green blue]
  (ColorUIResource. (Color. red green blue)))
(defn resrc-hex-color
  "Description:
    Convert hex color in string and create obj color for background."
  [hex]
  (assert (string? hex) "Color must be in string and hex like \"#ffffff\"!")
  (assert (let [c (count hex)] (or (= 4 c) (= 7 c))) "Color must have 3 or 6 values and # on begin!")
  (apply juiresource (hex-to-rgb hex)))

(defn- compos-list
  "Description:
    Default swing layout."
  []
  ["Button" "ToggleButton" "RadioButton" "CheckBox" "ColorChooser"
   "Label" "Table" "TextField" "PasswordField" "TextArea" "TextPane"
   "EditorPane" "ToolTip"])

(defn- layouts-list
  "Description:
    Default swing components."
  []
  ["ComboBox" "List" "MenuBar" "MenuItem" "RadioButtonMenuItem"
   "CheckBoxMenuItem" "Menu" "PopupMenu" "OptionPane" "Panel"
   "ProgressBar" "ScrollPane" "Viewport" "TabbedPane"
   "TitledBorder" "ToolBar" "Tree" "TableHeader"])

(defn- all-compos [] (concat (layouts-list) (compos-list)))

(defn- compo-list-suffix
  "Description:
    Add suffix to point component parameter."
  [compos suffix]
  (doall (map #(str % suffix) compos)))

;; (comment
;;   (UIManager/put "ScrollBar" (resrc-hex-color "#fff"))
;;   (UIManager/put "ScrollBar.thumb" (hex-color "#000"))
;;   (UIManager/put "ScrollBar.trackHighlightForeground" (hex-color "#000"))
;;   (UIManager/put "ScrollBar.border" (hex-color "#000"))
;;   (UIManager/put "ScrollBar.width" (Integer. 12))
;;   (UIManager/put "ScrollBar.squareButtons" false)
;;   (UIManager/put "ScrollBar.allowsAbsolutePositioning" true)
;;   (UIManager/put "Table.highlight" (hex-color "#0f0"))
;;   (UIManager/put "Table.highlight" (resrc-hex-color "#0f0")))

;; (UIManager/put "Table.selectionBackground" (hex-color "#000")) ;; work
;; (UIManager/put "Table.selectionForeground" (hex-color "#fff")) ;; work
;; (UIManager/put "TableHeader.background"    (hex-color "#000")) ;; work
;; (UIManager/put "TableHeader.foreground"    (hex-color "#fff")) ;; work
;; (UIManager/put "TableHeader.cellBorder"    (BorderFactory/createEmptyBorder 3 3 3 3)) ;; work

;; (UIManager/put "Table.scrollPaneBorder" (BorderFactory/createLineBorder (hex-color "#fff"))) ;; dont work

;; (comment
;;   (UIManager/put "ScrollBar.thumbHeight" (Integer. 0))
;;   (UIManager/put "ScrollBar.thumbDarkShadow" (resrc-hex-color "#000"))
;;   (UIManager/put "ScrollBar.thumbShadow" (resrc-hex-color "#000"))
;;   (UIManager/put "ScrollBar.thumbHighlight" (resrc-hex-color "#000"))
;;   (UIManager/put "ScrollBar.trackForeground" (resrc-hex-color "#000"))
;;   (UIManager/put "ScrollBar.trackHighlight" (resrc-hex-color "#000"))
;;   (UIManager/put "ScrollBar.foreground" (resrc-hex-color "#000"))
;;   (UIManager/put "ScrollBar.shadow" (resrc-hex-color "#000"))
;;   (UIManager/put "ScrollBar.highlight" (resrc-hex-color "#000")))
(defn- update-scrollbar []
  (UIManager/put "ScrollBar.width" (Integer. 12)))

(defn update-table-header
  [& {:keys [c-fg
             c-bg
             c-border
             tgap
             lgap
             bgap
             rgap
             tgap-in
             lgap-in
             bgap-in
             rgap-in]
      :or {c-fg face/c-table-header-fg
           c-bg face/c-table-header-bg
           c-border face/c-table-header-border
           tgap 0
           lgap 0
           bgap 0
           rgap 1
           tgap-in 2
           lgap-in 2
           bgap-in 2
           rgap-in 2}}]
  (UIManager/put "TableHeader.foreground" (hex-color c-fg))
  (UIManager/put "TableHeader.background" (hex-color c-bg))
  (UIManager/put "TableHeader.cellBorder" (BorderFactory/createCompoundBorder
                                           (BorderFactory/createMatteBorder tgap lgap bgap rgap (hex-color c-border))
                                           (BorderFactory/createEmptyBorder tgap-in lgap-in bgap-in rgap-in))))

(defn update-table
  [& {:keys [c-select-fg
             c-select-bg
             c-focus-cell
             tgap
             lgap
             bgap
             rgap]
      :or {c-select-fg  face/c-table-select-row-fg
           c-select-bg  face/c-table-select-row-bg
           c-focus-cell face/c-table-select-cell
           tgap 3
           lgap 3
           bgap 3
           rgap 3}}]
  (UIManager/put "Table.selectionForeground"      (hex-color c-select-fg))
  (UIManager/put "Table.selectionBackground"      (hex-color c-select-bg))
  (UIManager/put "Table.focusCellHighlightBorder" (BorderFactory/createLineBorder (hex-color c-focus-cell))))


(defn update-fonts
  ([]                (update-fonts (:plain (fonts)) (compo-list-suffix (all-compos) ".font")))
  ([font]            (update-fonts font (compo-list-suffix (all-compos) ".font")))
  ([font compo-list] (do
                       (register-fonts)
                       (doall (map #(UIManager/put % font) compo-list)))))

(defn update-layouts-background
  ([]                (update-layouts-background (resrc-hex-color face/c-layout-background) (compo-list-suffix (layouts-list) ".background")))
  ([background]      (update-layouts-background (resrc-hex-color background) (compo-list-suffix (layouts-list) ".background")))
  ([background compo-list] (doall (map #(UIManager/put % background) compo-list))))

(defn update-compos-background
  ([]                (update-compos-background (resrc-hex-color face/c-compos-background) (compo-list-suffix (compos-list) ".background")))
  ([background]      (update-compos-background (resrc-hex-color background) (compo-list-suffix (compos-list) ".background")))
  ([background compo-list] (doall (map #(UIManager/put % background) compo-list))))

(defn update-foreground
  ([]                (update-foreground (hex-color face/c-foreground) (compo-list-suffix (all-compos) ".foreground")))
  ([foreground]      (update-foreground (hex-color foreground) (compo-list-suffix (all-compos) ".foreground")))
  ([foreground compo-list] (doall (map #(UIManager/put % foreground) compo-list))))

(defn update-caret
  ([]                 (update-caret (hex-color face/c-caret) (compo-list-suffix (all-compos) ".caretForeground")))
  ([caret]            (update-caret (hex-color caret) (compo-list-suffix (all-compos) ".caretForeground")))
  ([caret compo-list] (doall (map #(UIManager/put % caret) compo-list))))

(defn getFont
  ([] (fonts))
  ([sizeorfont] (if (keyword? sizeorfont)
                  (sizeorfont (fonts))
                  (getFont :plain sizeorfont)))
  ([font size] (.deriveFont (font (fonts)) (float size))))

(state/set-state :theme-name "Jarman Light")

(defn load-style
  "Description:
    Load global style."
  []
  (update-fonts)
  (update-foreground)
  (update-caret)
  (update-layouts-background)
  (update-compos-background)
  (update-scrollbar)
  (update-table-header)
  (update-table)
  )


;; ┌───────────────────────────┐
;; │                           │
;; │        Font icons         │
;; │                           │
;; └───────────────────────────┘
(IconFontSwing/register (GoogleMaterialDesignIcons/getIconFont))
(defn icon
  "Description:
     Icon font wraper.
   Example:
     (icon GoogleMaterialDesignIcons/EDIT)"
  ([ico]       (icon ico face/c-icon 20))
  ([ico color] (icon ico color 20))
  ([ico color size]
   (IconFontSwing/buildIcon ico size (hex-color color))))

;; Demo
(comment
  (-> (c/frame :minimum-size [200 :by 200]
               :content (c/label :icon (icon GoogleMaterialDesignIcons/EDIT)))
      c/pack!
      c/show!))



;; ┌───────────────┐
;; │               │
;; │     DEMO      │
;; │               │
;; └───────────────┘
(defn- render-demo
  []
  (let [mig (mig-panel
             :constraints ["" "[50%, center]" "[50%, center]"]
             :background  (hex-color "#9cb")
             :items [[(c/label :text "Demo 1" :icon (icon GoogleMaterialDesignIcons/SAVE))]
                     [(c/label :text "Demo 2" :icon (icon GoogleMaterialDesignIcons/CLOSE "#f00" 30))]])]
    mig))


(import java.awt.MouseInfo)
(defn get-mouse-pos
  "Description:
     Return mouse position on screen, x and y.
  Example:
     (get-mouse-pos) => [800.0 600.0]"
  []
  (let [mouse-pos (.getLocation (java.awt.MouseInfo/getPointerInfo))
        screen-x  (.getX mouse-pos)
        screen-y  (.getY mouse-pos)]
    [screen-x screen-y]))

(defn style-demo
  []  
  ;;(load-style)
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
      (.setUndecorated true)
      (.setOpacity 0.7)
      (.setLocationRelativeTo nil) c/pack! c/show!)))

(comment
  (style-demo)
  )
