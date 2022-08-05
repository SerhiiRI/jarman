(ns jarman.gui.components.swing
  (:require
   [clojure.java.io]
   [clojure.pprint]
   [seesaw.mig]
   [seesaw.core]
   [seesaw.border]
   [seesaw.color]
   [jarman.lang :refer :all]
   [jarman.org  :refer :all]
   [jarman.faces   :as face]
   [jarman.gui.components.swing-context]
   [jarman.gui.components.swing-common]
   [jarman.gui.components.swing-table-utils]
   [jarman.gui.components.swing-debug-utils]
   [jarman.gui.components.swing-border]
   [potemkin.namespaces :refer [import-vars]])
  (:import
   [java.awt GraphicsEnvironment GraphicsDevice Font MouseInfo]
   [jiconfont.swing IconFontSwing]
   [javax.swing BorderFactory]
   ;; [jiconfont IconFont DefaultIconCode]
   ;; [jiconfont.icons.google_material_design_icons GoogleMaterialDesignIcons]
   ))

(import-vars
  [jarman.gui.components.swing-debug-utils
   choose-screen! active-screens quick-frame]
  
  [jarman.gui.components.swing-table-utils
   wrapp-adjustment-listener]

  [jarman.gui.components.swing-common
   dimension point font icon]

  [jarman.gui.components.swing-border
   border])

;;;;;;;;;;;;;;;;;;;;;;;;;
;;; AWT/Swing helpers ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;

;; cutted from 'jarman.gui.components.swing
(defn ^sun.awt.image.ToolkitImage image-scale
  "Function scale image by percent size.
  Return `sun.awt.image.ToolkitImage` type.

  Example:
    (image-scale \"/path/to/photo\" 100)

  See more:
    javadoc `sun.awt.image.ToolkitImage`
    javadoc `sun.awt.Image`"
  ([image-path]
   (if (instance? javax.swing.ImageIcon image-path) image-path
     (seesaw.icon/icon (clojure.java.io/file image-path))))
  ([image-path percent]
   (if (instance? javax.swing.ImageIcon image-path) image-path
    (let [image (.getImage (seesaw.icon/icon (clojure.java.io/file image-path)))
          scaler (comp int #(Math/ceil %) #(* % (/ percent 100.0)))]
      (doto (javax.swing.ImageIcon.)
        (.setImage (.getScaledInstance image
                                       (scaler (.getWidth image))
                                       (scaler (.getHeight image))
                                       java.awt.Image/SCALE_SMOOTH)))))))

(defn register-fonts
  "Description
    Register system font
  
  Example
    (register-fonts
     [(clojure.java.io/resource \"fonts/ubuntu/Ubuntu-MediumItalic.ttf\")
      (clojure.java.io/resource \"fonts/ubuntu/Ubuntu-Medium.ttf\")
      (clojure.java.io/resource \"fonts/ubuntu/Ubuntu-Regular.ttf\")])"
  [font-files]
  (let [^GraphicsEnvironment ge (java.awt.GraphicsEnvironment/getLocalGraphicsEnvironment)]
    (doall
     (doseq [font font-files]
       (.registerFont ge
                      (java.awt.Font/createFont java.awt.Font/TRUETYPE_FONT
                                                (clojure.java.io/input-stream font)))))))

(defn timelife
  "Description:
    Run fn after some time.
    Set time, 1 is a 1 sec
    Set fn
  Example:
    (timelife (1 ))"
  ([time fn-to-invoke]
   (timelife time fn-to-invoke ""))
  ([time fn-to-invoke title]
   (.start
    (Thread.
     (fn []
       (if (>= time 0)
         (do
           (Thread/sleep (* 1000 time))
           (try
             (fn-to-invoke)
             (catch Exception e (print-error e))))))))))

(defn get-mouse-pos
  "Description
     Return mouse position on screen, x and y.
  Example
     (get-mouse-pos) => [800.0 600.0]" []
  (let [mouse-pos (.getLocation (MouseInfo/getPointerInfo))
        screen-x  (.getX mouse-pos)
        screen-y  (.getY mouse-pos)]
    [screen-x screen-y]))

(comment
 (defn empty-border 
   "Create an empty border. The following properties are supported:
  
    :thickness The thickness of the border (all sides) in pixels. This property
               is only used if :top, :bottom, etc are omitted. Defaults to 1.
  
    :top       Thickness of the top border in pixels. Defaults to 0.
    :left      Thickness of the left border in pixels. Defaults to 0.
    :bottom    Thickness of the bottom border in pixels. Defaults to 0.
    :right     Thickness of the right border in pixels. Defaults to 0.
  Examples:
      ; Create an empty 10 pixel border
      (empty-border :thickness 10)
      ; Create an empty border 5 pixels on top and left, 0 on other sides
      (empty-border :left 5 :top 5)"
   [& {:keys [thickness top left bottom right]}]
   (if (or top left bottom right)
     (BorderFactory/createEmptyBorder (or top 0) (or left 0) (or bottom 0) (or right 0))
     (let [t (or thickness 1)]
       (BorderFactory/createEmptyBorder t t t t)))))


(def ^:dynamic *formatter-tooltip-text-length* 50)
(defn tooltip<-metadata [^clojure.lang.PersistentArrayMap metadata]
  (let [{:keys [description representation]} metadata]
    (clojure.pprint/cl-format nil "<html>~@[~A~]~@[~A~]</html>"
      (when representation
        (str "<b>" (identity "Representation") "</b><br>" " <p style='margin-left: 5px;'>" representation "<p>"))
      (when description
        (str "<b>" (identity "Description") "</b><br>" " <p style='margin-left: 5px;'>"
          (->> description
            (partition-all *formatter-tooltip-text-length*)
            (map (partial apply str))
            (clojure.string/join "<br>")) "<p>")))))

(defn wrapp-tooltip [^javax.swing.JComponent component ^String tooltip]
  {:pre [(instance? javax.swing.JComponent component)]}
  (if (empty? tooltip) component
      (do
        (if (instance? javax.swing.JScrollPane component)
          (.setToolTipText (.. component getViewport getView) tooltip)
          (.setToolTipText component tooltip))
        component)))

(defn wrapp-focus-listener
  [^javax.swing.JComponent component
   & {:keys [focus-lost-fn focus-gained-fn]
      :or {focus-lost-fn (fn []) focus-gained-fn (fn [])}}]
  (doto component
   (.addFocusListener 
     (proxy [java.awt.event.FocusListener] []
       (^void focusLost [^java.awt.event.FocusEvent e]
        (jarman.gui.components.swing-context/with-active-event e
          (focus-lost-fn)))
       (^void focusGained [^java.awt.event.FocusEvent e]
        (jarman.gui.components.swing-context/with-active-event e
          (focus-gained-fn)))))))

(defn wrapp-carret-listener
  [^javax.swing.text.JTextComponent component & {:keys [caret-update-fn] :or {caret-update-fn (fn [])}}]
  (doto component
   (.addCaretListener
     (proxy [javax.swing.event.CaretListener] []
       (^void caretUpdate [^javax.swing.event.CaretEvent e]
        (jarman.gui.components.swing-context/with-active-event e
          (caret-update-fn)))))))

(defn popup-frame [^String title ^javax.swing.JComponent content
                   & {:keys [width height] :or {width 350 height 200}}]
  (-> (doto (javax.swing.JFrame. title)
        (.setUndecorated true)
        (.setAutoRequestFocus false)
        (.setFocusableWindowState false)
        (.setType java.awt.Window$Type/POPUP)
        (.setPreferredSize (dimension width height))
        (.setLocationByPlatform true)
        (.setContentPane content)
        (.setVisible false))))

