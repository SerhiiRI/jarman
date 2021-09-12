(ns jarman.gui.gui-style
  (:use seesaw.mig)
  (:require [seesaw.core   :as c]
            [seesaw.border :as b]
            [jarman.faces  :as face]
            [jarman.logic.state :as state]
            [jarman.tools.lang :refer :all])
  (:import (javax.swing.text SimpleAttributeSet)
           (javax.swing.plaf ColorUIResource)
           (javax.swing UIManager)
           (java.awt Font)
           (java.awt Color)
           (java.io File)
           (java.nio.file Files)
           (java.awt GraphicsEnvironment)))

(defn- ubuntu-font [font] (str "./resources/fonts/ubuntu/" font))
(def fonts {:plain    (.deriveFont (Font/createFont Font/TRUETYPE_FONT (File. (ubuntu-font "Ubuntu-R.ttf")))  14.0)
            :bold     (.deriveFont (Font/createFont Font/TRUETYPE_FONT (File. (ubuntu-font "Ubuntu-B.ttf")))  14.0)
            :bold-i   (.deriveFont (Font/createFont Font/TRUETYPE_FONT (File. (ubuntu-font "Ubuntu-BI.ttf"))) 14.0)
            :italic   (.deriveFont (Font/createFont Font/TRUETYPE_FONT (File. (ubuntu-font "Ubuntu-RI.ttf"))) 14.0)
            :light    (.deriveFont (Font/createFont Font/TRUETYPE_FONT (File. (ubuntu-font "Ubuntu-L.ttf")))  14.0)
            :light-i  (.deriveFont (Font/createFont Font/TRUETYPE_FONT (File. (ubuntu-font "Ubuntu-LI.ttf"))) 14.0)
            :medium   (.deriveFont (Font/createFont Font/TRUETYPE_FONT (File. (ubuntu-font "Ubuntu-M.ttf")))  14.0)
            :medium-i (.deriveFont (Font/createFont Font/TRUETYPE_FONT (File. (ubuntu-font "Ubuntu-MI.ttf"))) 14.0)
            
            :mono     (.deriveFont (Font/createFont Font/TRUETYPE_FONT (File. (ubuntu-font "UbuntuMono-R.ttf")))  14.0)
            :mono-b   (.deriveFont (Font/createFont Font/TRUETYPE_FONT (File. (ubuntu-font "UbuntuMono-B.ttf")))  14.0)
            :mono-bi  (.deriveFont (Font/createFont Font/TRUETYPE_FONT (File. (ubuntu-font "UbuntuMono-BI.ttf"))) 14.0)
            :mono-i   (.deriveFont (Font/createFont Font/TRUETYPE_FONT (File. (ubuntu-font "UbuntuMono-RI.ttf"))) 14.0)

            :thin      (.deriveFont (Font/createFont Font/TRUETYPE_FONT (File. (ubuntu-font "Ubuntu-Th.ttf"))) 14.0)
            :condensed (.deriveFont (Font/createFont Font/TRUETYPE_FONT (File. (ubuntu-font "Ubuntu-C.ttf")))  14.0)})

(state/set-state :fonts fonts)

(defn- register-fonts []
  (doall (map (fn [[name-k font]] (.registerFont (GraphicsEnvironment/getLocalGraphicsEnvironment) font))
              fonts)))
(register-fonts)


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
(defn color-hex
  "Description:
    Convert hex color in string and create obj color."
  [hex] (apply jcolor (hex-to-rgb hex)))

(defn- juiresource [red green blue]
  (ColorUIResource. (Color. red green blue)))
(defn bg-hex
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

(defn update-fonts
  ([]                (update-fonts (:plain fonts) (compo-list-suffix (all-compos) ".font")))
  ([font]            (update-fonts font (compo-list-suffix (all-compos) ".font")))
  ([font compo-list] (doall (map #(UIManager/put % font) compo-list))))

(defn update-layouts-background
  ([]                (update-layouts-background (bg-hex "#efefef") (compo-list-suffix (layouts-list) ".background")))
  ([background]      (update-layouts-background (bg-hex background) (compo-list-suffix (layouts-list) ".background")))
  ([background compo-list] (doall (map #(UIManager/put % background) compo-list))))

(defn update-compos-background
  ([]                (update-compos-background (bg-hex "#ffffff") (compo-list-suffix (compos-list) ".background")))
  ([background]      (update-compos-background (bg-hex background) (compo-list-suffix (compos-list) ".background")))
  ([background compo-list] (doall (map #(UIManager/put % background) compo-list))))

(defn update-foreground
  ([]                (update-foreground (color-hex "#020020") (compo-list-suffix (all-compos) ".foreground")))
  ([foreground]      (update-foreground (color-hex foreground) (compo-list-suffix (all-compos) ".foreground")))
  ([foreground compo-list] (doall (map #(UIManager/put % foreground) compo-list))))

(defn getFont
  ([] fonts)
  ([sizeorfont] (if (keyword? sizeorfont)
                  (sizeorfont fonts)
                  (getFont :plain sizeorfont)))
  ([font size] (.deriveFont (font fonts) (float size))))

(state/set-state :theme-name "Jarman Light")

(defn load-style
  "Description:
    Load global style."
  []
  (update-fonts)
  (update-layouts-background)
  (update-compos-background)
  (update-foreground))

;; ┌───────────────┐
;; │               │
;; │     DEMO      │
;; │               │
;; └───────────────┘
(defn- render-demo
  []
  (mig-panel
   :constraints ["" "[50%, center]" "[50%, center]"]
   :items [[(c/label :text "Demo 1")]
           [(c/label :text "Demo 2")]]))

(defn style-demo
  []  
  (load-style)
  (-> (doto (seesaw.core/frame
             :title "Style demo"
             :minimum-size [500 :by 500]
             :size [500 :by 500]
             :content (render-demo))
        (.setLocationRelativeTo nil) c/pack! c/show!)))
  
;;(style-demo)




