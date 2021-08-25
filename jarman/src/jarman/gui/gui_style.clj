(ns jarman.gui.gui-style
  (:use seesaw.mig)
  (:require [seesaw.core   :as c]
            [seesaw.border :as b]
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

(defmacro color-hex
  "Description:
    Convert hex color in string and create obj color."
  [hex] `(Color. ~@(hex-to-rgb hex)))

(defmacro bg-hex
  "Description:
    Convert hex color in string and create obj color for background."
  [hex] `(ColorUIResource. (Color. ~@(hex-to-rgb hex))))

(defn- compo-list
  "Description:
    Default swing components."
  []
  ["Button" "ToggleButton" "RadioButton" "CheckBox" "ColorChooser"
   "ComboBox" "Label" "List" "MenuBar" "MenuItem" "RadioButtonMenuItem"
   "CheckBoxMenuItem" "Menu" "PopupMenu" "OptionPane" "Panel"
   "ProgressBar" "ScrollPane" "Viewport" "TabbedPane" "Table"
   "TableHeader" "TextField" "PasswordField" "TextArea" "TextPane"
   "EditorPane" "TitledBorder" "ToolBar" "ToolTip" "Tree"])

(defn- compo-list-suffix
  "Description:
    Add suffix to point component parameter."
  [suffix]
  (doall (map #(str % suffix) (compo-list))))

(defn update-fonts
  ([]                (update-fonts (:plain fonts) (compo-list-suffix ".font")))
  ([font]            (update-fonts font (compo-list-suffix ".font")))
  ([font compo-list] (doall (map #(UIManager/put % font) compo-list))))

(defn update-background
  ([]                (update-background (bg-hex "#efefef") (compo-list-suffix ".background")))
  ([background]      (update-background background (compo-list-suffix ".background")))
  ([background compo-list] (doall (map #(UIManager/put % background) compo-list))))

(defn update-foreground
  ([]                (update-foreground (color-hex "#020020") (compo-list-suffix ".foreground")))
  ([background]      (update-foreground background (compo-list-suffix ".foreground")))
  ([background compo-list] (doall (map #(UIManager/put % background) compo-list))))

(defn getFont
  ([] fonts)
  ([sizeorfont] (if (keyword? sizeorfont)
                  (sizeorfont fonts)
                  (getFont :plain sizeorfont)))
  ([font size] (.deriveFont (font fonts) (float size))))

(defn load-style
  "Description:
    Load global style."
  []
  (update-fonts)
  (update-background)
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



