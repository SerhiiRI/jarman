(ns jarman.gui.components.swing
  (:require
   [seesaw.core]
   [clojure.pprint :refer [cl-format]]
   [jarman.tools.lang :refer :all])
  (:import
   [javax.swing Action KeyStroke]
   [javax.swing AbstractAction InputMap ActionMap]
   [java.awt.event KeyEvent InputEvent ActionEvent]))

(defmacro make-inputmap-wrapper
  "Description
    Create function that generate wrapper 
    for some JComponent, which add or override
    InputMap/ActionMap for specyfic component

  Example 
    (def action-wrapper
      (make-key-wrapper
       {:id \"run-action1\"
        :key  (KeyStroke/getKeyStroke KeyEvent/VK_X InputEvent/ALT_MASK)
        :action (fn [^ActionEvent event]
                  (c/alert \"First Action\"))}
       {:id \"run-action2\"
        :key  (KeyStroke/getKeyStroke KeyEvent/VK_F InputEvent/ALT_MASK)
        :action (fn [^ActionEvent event]
                  (c/alert \"Second Action\"))}))
  
     (-> (mig-panel) (action-wrapper))"
  [& map-list]
  (let [key-list (map #(select-keys % [:id :key]) map-list)
        act-list (map #(select-keys % [:id :action]) map-list)]
    `(fn [~'component]
       (let [^javax.swing.InputMap  ~'keyMap    (.getInputMap ~'component)
             ^javax.swing.ActionMap ~'actionMap (.getActionMap ~'component)]
         (do
           ~@(for [{:keys [id key]} key-list]
               `(.put ~'keyMap ~key ~id)))
         (do
           ~@(for [{:keys [id action]} act-list]
               `(.put ~'actionMap ~id
                      (proxy [javax.swing.AbstractAction] []
                        (^void ~'actionPerformed [^ActionEvent event#]
                         (~action event#))))))
         ~'component))))

(defn wrapp-component-inputmap
  "Description
    Create function that generate wrapper 
    for some JComponent, which add or override
    InputMap/ActionMap for specyfic component

  Example 
    (-> (mig-panel)
      (wrapp-component-inputmap
       {:id \"run-action1\"
        :key  (KeyStroke/getKeyStroke KeyEvent/VK_X InputEvent/ALT_MASK)
        :action (fn [^ActionEvent event]
                  (c/alert \"First Action\"))}
       {:id \"run-action2\"
        :key  (KeyStroke/getKeyStroke KeyEvent/VK_F InputEvent/ALT_MASK)
        :action (fn [^ActionEvent event]
                  (c/alert \"Second Action\"))}))"
  [component & map-list]
  (let [key-list (map #(select-keys % [:id :key]) map-list)
        act-list (map #(select-keys % [:id :action]) map-list)]
    (let [^javax.swing.InputMap  keyMap    (.getInputMap component)
          ^javax.swing.ActionMap actionMap (.getActionMap component)]
      (doall
       (for [{:keys [id key]} key-list]
         (.put keyMap key id)))
      (doall
       (for [{:keys [id action]} act-list]
         (.put actionMap id
               (proxy [javax.swing.AbstractAction] []
                 (^void actionPerformed [^ActionEvent event]
                  (action event))))))
      component)))

(defn keybindings-show-action
  "Example
    (show-componet-actions (text :value \"\"))
    (show-componet-actions (wrapp-emacs-keymap-rtext (seesaw.rsyntax/text-area :text \"\")))"
  [component]
  {:pre [(instance? javax.swing.JComponent component)]}
  (println
   (cl-format nil "ActionMap:~%~{  ~A~%~}Actions:~%~{  ~A~%~}"
              (if (instance? javax.swing.JComponent component)
                (->> (.keys (.getActionMap component)) (into []) (sort))
                ["Non JComponent instance"])
              (if (instance? javax.swing.text.JTextComponent component)
                (->> (.getActions component) (map (fn [a] (.getValue a Action/NAME))) (into []) (sort))
                ["Non JTextComponent instance"]))))

(defn keybindings-action-keystroke-map
  "Example
    (show-componet-actions (text :value \"\"))
    (show-componet-actions (wrapp-emacs-keymap-rtext (seesaw.rsyntax/text-area :text \"\")))"
  [component]
  {:pre [(instance? javax.swing.JComponent component)]}
  (where
   ((keyMap (.getInputMap component))
    (action-key-map
     (->> (seq (.allKeys keyMap))
          (map #(vector (.get keyMap %) %))
          (into {}))))
   (->>
    (cond-> #{}
      ;; ---
      (instance? javax.swing.JComponent component)
      (into (->> (.keys (.getActionMap component))))
      ;; --- 
      (instance? javax.swing.text.JTextComponent component)
      (into (->> (.getActions component) (map (fn [a] (.getValue a Action/NAME))))))
    (seq)
    (reduce
     (fn [acc e]
       (if-let [key-bind (get action-key-map e nil)] 
         (conj acc [e key-bind])
         (conj acc [e nil])))
     [])
    (into (array-map)))))

(defn keybindings-action-keystroke-map-print [component]
  {:pre [(instance? javax.swing.JComponent component)]}
  (cl-format *out* "Keybindings:~%~{~{~35A ~@[\"~A\"~]~}~%~}"
             (->> (keybindings-action-keystroke-map component)
                  (sort-by first))))

(defn keybindings-panel [component]
  {:pre [(instance? javax.swing.JComponent component)]}
  (seesaw.core/scrollable
   (seesaw.core/grid-panel
    :border "Keybindings"
    :columns 2
    :items 
    (->> (keybindings-action-keystroke-map component)
         (sort-by first)
         (mapcat (fn [[action keystroke]]
                   (vector action (if keystroke (str keystroke) ""))))))))

;;;;;;;;;;;;;;;;;;;;;;;
;;; SCREEN SETTINGS ;;;
;;;;;;;;;;;;;;;;;;;;;;;

(defn choose-screen! [^javax.swing.JFrame frame ^Number screen-n]
  ;; S(Screen), F(Frame), H(Height), W(Width)
  ;; +------------------[screen-n]---
  ;; | 
  ;; |    V
  ;; |     x=S-X+(S-W/2)-(F-W/2)
  ;; |     y=F-H+(S-H/2)-(F-H/2)
  ;; |
  (where
   ((^java.awt.GraphicsEnvironment ge (java.awt.GraphicsEnvironment/getLocalGraphicsEnvironment))
    (^"[Ljava.awt.GraphicsDevice;" gd (.getScreenDevices ge))
    (screen screen-n if2 #(< -1 % (alength gd)) screen-n 0)
    (S-X (.. (aget gd screen) getDefaultConfiguration getBounds -x))
    (S-Y (.. frame getY))
    (S-H (.. (aget gd screen) getDefaultConfiguration getBounds -height))
    (S-W (.. (aget gd screen) getDefaultConfiguration getBounds -width))
    (F-H (.  frame getWidth))
    (F-W (.  frame getHeight))
    (relative-y S-H do #(- (/ % 2) (/ F-H 2)) do long)
    (relative-x S-W do #(- (/ % 2) (/ F-W 2)) do long))
   (doto frame
     (.setLocation
      (+ relative-x S-X)
      (+ relative-y S-Y)))))




