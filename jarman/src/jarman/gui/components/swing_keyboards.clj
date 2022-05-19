(ns jarman.gui.components.swing-keyboards
  (:require
   [seesaw.core]
   [clojure.pprint :refer [cl-format]]
   [jarman.tools.lang :refer :all]
   [jarman.gui.components.swing-actions]
   [jarman.gui.components.swing-context :as swing-context :refer :all])
  (:import
   [javax.swing.text DefaultEditorKit]
   [javax.swing Action KeyStroke]
   [javax.swing AbstractAction InputMap ActionMap]
   [java.awt.event KeyEvent InputEvent ActionEvent KeyAdapter]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Low-level keyboard operations ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmacro ^:depr make-inputmap-wrapper
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

(defn ^:depr wrapp-component-inputmap
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

(defn keybindings-actions
  "Example
    (keybindings-show-action (text :value \"\"))
    (keybindings-show-action (wrapp-emacs-keymap-rtext (seesaw.rsyntax/text-area :text \"\")))"
  [component]
  {:pre [(instance? javax.swing.JComponent component)]}
  (cond-> {}
    (instance? javax.swing.JComponent component)
    (assoc  (->> (.keys (.getActionMap component)) (into (sorted-set))))

    (instance? javax.swing.text.JTextComponent component)
    (assoc :Actions (->> (.getActions component) (map (fn [a] (.getValue a Action/NAME))) (into (sorted-set))))))

(defn keybindings-action-keystroke-map
  "Example
    (keybindings-action-keystroke-map (text :value \"\"))
    (keybindings-action-keystroke-map (wrapp-emacs-keymap-rtext (seesaw.rsyntax/text-area :text \"\")))"
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
     []))))

(defn keybindings-action-keystroke-map-binded-only [component]
 (where
  ((keyMap (.getInputMap component)))
  (->> (seq (.allKeys keyMap))
       (map #(vector (.get keyMap %) %)))))

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

(defn component-actions [component]
  (let [action-map (.getActionMap component)]
    (cond-> (hash-map)
      ;; ---
      (instance? javax.swing.JComponent component)
      (into (map #(.get action-map %) (.keys action-map)) )
      ;; --- 
      (instance? javax.swing.text.JTextComponent component)
      (into (->> (.getActions component) (map (fn [a] (vector (.getValue a Action/NAME) a))))))))

(defn component-unregister-keybindings
  ([component]
   (doto component
     (component-unregister-keybindings (keybindings-action-keystroke-map-binded-only component))))
  ([component keybindings]
   (let [^javax.swing.InputMap keyMap (.getInputMap component)]
     (doall (map #(.put keyMap (second %) "none") keybindings))
     component)))

(defn component-unregister-keylisteners
  ([component]
   (doall (map #(.removeKeyListener component %) (.getKeyListeners component)))
   component))

;;;;;;;;;;;;;;;;;;;;;
;;; Char modifier ;;;
;;;;;;;;;;;;;;;;;;;;;

(defn- char->modifier [c]
   (cond
     (= c \C) InputEvent/CTRL_MASK
     (= c \M) InputEvent/ALT_MASK
     (= c \S) InputEvent/SHIFT_MASK
     :else 0))

(defn- char->symbol [symb]
  (if (= 1 (.length symb))
    (KeyEvent/getExtendedKeyCodeForChar (int (first (seq symb))))
    (case symb
      "space"  KeyEvent/VK_SPACE
      "up"     KeyEvent/VK_UP
      "down"   KeyEvent/VK_DOWN
      "right"  KeyEvent/VK_RIGHT
      "left"   KeyEvent/VK_LEFT
      "period" KeyEvent/VK_PERIOD
      "comma"  KeyEvent/VK_COMMA
      "backspace" KeyEvent/VK_BACK_SPACE
      "delete"    KeyEvent/VK_DELETE
      "escape"    KeyEvent/VK_ESCAPE
      nil)))

(defn- kbd->KeyStroke
  "Description
    Transform emacs notation into the KeyStroke object

  Example 
    (kbd->KeyStroke \"C-M-space\")
       => #object[javax.swing.KeyStroke \"ctrl alt pressed SPACE\"]"
  [^String keybinding]
  (let [pattern #"(?:(C|M|S|C-M|M-S|C-S|C-M-S)-)?(.|up|down|space|right|left|period|comma|backspace|delete|escape)"]
   (if-let [[_ modifiers symb] (re-matches pattern keybinding)]
     (KeyStroke/getKeyStroke
      (char->symbol symb)
      (if-not (some? modifiers) 0
              (->> (seq modifiers)
                   (remove (hash-set \-))
                   (map char->modifier)
                   (reduce bit-or))))
     (throw (ex-info (format "keybinding <%s> has bad pattern" keybinding)
                     {:keybinding keybinding
                      :re-pattern pattern})))))

(defn- KeyStroke->kbd
  "Description
    Transform KeyStroke to readable emacs notation

  Example 
    (KeyStroke->kbd
     (KeyStroke/getKeyStroke
      KeyEvent/VK_SPACE
      (bit-or InputEvent/ALT_MASK InputEvent/CTRL_MASK)))
     => \"C-M-space\""
  [^KeyStroke keystroke]
  (let [code (.getKeyCode keystroke)
        modf (.getModifiers keystroke)]
    (apply str
           (cond-> []
             (= (bit-and modf InputEvent/CTRL_DOWN_MASK) InputEvent/CTRL_DOWN_MASK) (conj "C-")
             (= (bit-and modf InputEvent/ALT_DOWN_MASK) InputEvent/ALT_DOWN_MASK)   (conj "M-")
             (= (bit-and modf InputEvent/SHIFT_MASK) InputEvent/SHIFT_MASK)         (conj "S-")
             true (conj (clojure.string/lower-case (KeyEvent/getKeyText code)))))))

(defn kbd
  "Translate String Emacs shortcut sequense to
  KeyStroke vector

  See
   `kvc` - opposite function

  Example 
  (kbd \"C-c C-p s s\")
    => [#obj[KeyStroke \"ctrl pressed C\"]
        #obj[KeyStroke \"ctrl pressed P\"]
        #obj[KeyStroke \"pressed S\"]
        #obj[KeyStroke \"pressed S\"]]"
  [^String keybinding]
  (mapv kbd->KeyStroke (clojure.string/split keybinding #"\s+")))

(defn kvc
  "Translate KeyStroke vector sequence to readable
  Emacs keybinding shortcut 
  
  See
   `kbd - opposite function

  Example 
  (kvc
    [(KeyStroke/getKeyStroke \"ctrl pressed C\")
     (KeyStroke/getKeyStroke \"ctrl pressed P\")
     (KeyStroke/getKeyStroke \"pressed S\")
     (KeyStroke/getKeyStroke \"pressed S\")])
  =>  \"C-c C-p s s\""
  [^clojure.lang.PersistentVector key-vec]
  (cl-format nil "~{~A~^ ~}" (map KeyStroke->kbd key-vec)))

(defn define-key
  "Description 
    add some shortcuts to keymap which specified as first 
    argument (define-key keymap [KeyStroke...] action)

    The action can be different types
     symbol - link on function 
     string - that mean \"run some action\" though the name
     list - mean some expression under the quote
     fn - function 

  Example 
    (-> {}
      (define-key (kbd \"C-x C-c\") 'jarman.gui.components.common/describe-functions)
      (define-key (kbd \"M-x\")     'jarman.gui.components.common/execute-extended-command)
      (define-key (kbd \"C-z\")     \"undo\")
      (define-key (kbd \"C-h b\")   '(fn [e] (println \"Un SYMBOL function\")))
      (define-key (kbd \"C-h f\")   jarman.gui.components.common/describe-functions)
      (define-key (kbd \"C-h t\")   (fn [e] (println \"Un SYMBOL function\"))))"
  [m key-vec bindings]
  {:pre [(vector? key-vec)]}
  (if (instance? clojure.lang.Ref m)
    (dosync
     (if (some? bindings)
       (alter m assoc-in key-vec bindings)
       (alter m dissoc-in key-vec)))
    (if (some? bindings)
      (assoc-in m key-vec bindings)
      (dissoc-in m key-vec))))

(defn global-set-key
  "Description
   Define keybinding in the Global Keymap

  See 
    `define-key`
    `current-keymap`

  Example 
    (global-set-key (kbd \"C-c C-p s d\") '(fn [] (seesaw.core/alert \"you pressed C-c C-a s d\")))
    (global-set-key (kbd \"C-c s\") '(fn [] (seesaw.core/alert \"you pressed C-c s\")))
    (global-set-key (kbd \"C-s\") (fn [] (seesaw.core/alert \"you pressed C-s\")))"
  [key-vec bindings]
  (define-key (swing-context/global-keymap-ref) key-vec bindings)
  true)

(defn global-key-vec-buffer-kvc []
  (swing-context/global-key-vec-buffer))

(defn global-key-vec-buffer-kbd []
  (kvc (swing-context/global-key-vec-buffer)))

(defn lookup-keymap-action [m]
  (if (instance? clojure.lang.Ref m)
    (get-in (deref m) (swing-context/global-key-vec-buffer) nil)
    (get-in m (swing-context/global-key-vec-buffer) nil)))

;;;;;;;;;;;;;;;;;;;;;;;
;;; DESCRIBE KEYMAP ;;;
;;;;;;;;;;;;;;;;;;;;;;;

(defn- action-representation [kbd-seq action]
  (cond 
    (nil? action) ""
    (symbol? action) (str action)
    (string? action) (format "action.%s" action) 
    (or (fn? action) (sequential? action)) "<lambda>"
    :else (throw (ex-info (format "Bad action type <%s>" kbd-seq)
                          {:action action}))))

(defn- return-all-key-map [l & {:keys [pref]}]
  (apply concat
         (for [[k f] (seq l)]
           (let [kb (if pref (str pref " " (KeyStroke->kbd k)) (KeyStroke->kbd k))]
             (if (map? f)
               (concat
                [(list kb "prefix command")]
                (return-all-key-map f :pref kb))
               [(list kb (action-representation kb f))])))))

(defn describe-keymap
  ([] (describe-keymap (deref global-keymap)))
  ([local-keymap]
   (do
     (cl-format *out* "~20A ~A~%" "Shortcut" "Command")
     (cl-format *out* "~20A ~A~%" "--------" "-------")
     (doall (map #(apply cl-format *out* "~20A ~A~%" %) (return-all-key-map local-keymap)))) nil))

(defn helper-panel [keymap]
  ;; (as-> keymap kmp
  ;;   (define-key kmp (kbd "C-h b")
  ;;     (fn []
  ;;       (seesaw.core/scrollable
  ;;        (seesaw.core/grid-panel
  ;;         :border "Keybindings"
  ;;         :columns 2
  ;;         :items
  ;;         (apply concat (return-all-key-map kmp)))))))
  (seesaw.core/scrollable
   (seesaw.core/grid-panel
    :border "Keybindings"
    :columns 2
    :items
    (apply concat (return-all-key-map keymap)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; KEYMAP COMPONENT WRAPPER's ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn invoke-action [& {:keys [event action actionmap]}]
  (cond 
    (nil? action)
    (global-key-vec-buffer-reset)
    ;; ---
    (map? action)
    nil
    ;; ---
    (string? action)
    (do
      (global-key-vec-buffer-reset)
      (if-let [action-fn (get actionmap action)]
       (.actionPerformed action-fn 
                         (java.awt.event.ActionEvent. (.getSource event) (.intValue (System/currentTimeMillis)) nil))
       (throw (ex-info (format "The action %s doesn't have any invokers" (str action))
                       {:action action :actionmap (keys actionmap)}))))
    ;; ---
    (instance? javax.swing.AbstractAction action)
    (do
      (global-key-vec-buffer-reset)
      (.actionPerformed action
                        (java.awt.event.ActionEvent.
                         (.getSource event) (.intValue (System/currentTimeMillis)) nil)))
    ;; ---
    (fn? action)
    (do (apply action []) (global-key-vec-buffer-reset))
    ;; ---
    (symbol? action)
    (do (apply (eval action) []) (global-key-vec-buffer-reset))
    ;; ---
    (and (sequential? action) (= (first action ) 'fn))
    (do (apply (eval action) []) (global-key-vec-buffer-reset))
    ;; ---
    :else (throw (ex-info (format "Bad action type <%s>" (global-key-vec-buffer-kbd))
                          {:key (global-key-vec-buffer) :action action}))))

(defn wrapp-keymap
  ([component]
   (wrapp-keymap component (swing-context/global-keymap) {}))
  ([component keymap]
   (wrapp-keymap component keymap {}))
  ([component keymap actionmap] 
   (let [input-action jarman.gui.components.swing-actions/default-editor-kit-action-map]
     ;; ---
     ;; (swing-keyboards/describe-keymap (active-keymap))
     ;; (println (swing-keyboards/global-key-vec-buffer-kbd))
     ;; ---
     (.setFocusable component true)
     (doto component
       (component-unregister-keybindings)
       (component-unregister-keylisteners)
       (.addKeyListener
        (proxy [KeyAdapter] []
          ;; ---------------
          (^void keyTyped [^KeyEvent e]
           (when (not-empty (global-key-vec-buffer))
             (.consume e)))
          ;; ---------------
          (^void keyPressed [^KeyEvent e]
           (when-not
               (and (#{KeyEvent/VK_WINDOWS KeyEvent/VK_CONTROL KeyEvent/VK_ALT KeyEvent/VK_SHIFT}
                   (.getKeyCode (KeyStroke/getKeyStrokeForEvent e))))
             (with-keymap keymap
               (global-key-vec-buffer-put (KeyStroke/getKeyStrokeForEvent e))
               ;; (println (.getSource e))
               (if-let [action (lookup-keymap-action keymap)]
                 (with-active-event e
                   ;; (swing-keyboards/describe-keymap (active-keymap))
                   (invoke-action
                    :event e
                    :action action
                    :actionmap actionmap))
                 (global-key-vec-buffer-reset)))))))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; fixme:serhii               ;;;
;;; TEMPORARLY BIND GLOBAL MAP ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def close-application
  (fn []
    (println "ACTION => close-frame")
    (if-let [frame (active-frame)]
      (.dispose frame))))
(def execute-extended-command (fn [] (println "ACTION => execute-extended-command")))
(defn describe-bindings-output []
  (println "ACTION => describe-bindings *OUT*")
  (if-let [keymap (active-keymap)]
    (describe-keymap keymap)))
(defn describe-bindings-jframe []
  (println "ACTION => describe-functions *DIALOG")
  (if-let [keymap (active-keymap)]
    (-> 
     (seesaw.core/dialog
      :option-type :default
      :content (helper-panel keymap))
     (seesaw.core/pack!)
     (seesaw.core/show!))))

(global-set-key (kbd "C-x C-c")         'jarman.gui.components.swing-keyboards/close-application)
(global-set-key (kbd "M-x")             'jarman.gui.components.swing-keyboards/execute-extended-command)
(global-set-key (kbd "C-h b")           'jarman.gui.components.swing-keyboards/describe-bindings-output)
(global-set-key (kbd "C-h j")           'jarman.gui.components.swing-keyboards/describe-bindings-jframe)
(global-set-key (kbd "C-h t")           '(fn [e] (println "Un SYMBOL function")))
(global-set-key (kbd "C-c C-p f")       '(fn [] (seesaw.core/alert "you pressed C-c C-p f")))
(global-set-key (kbd "C-c C-p s s")     '(fn [] (seesaw.core/alert "you pressed C-c C-a s s")))
(global-set-key (kbd "C-c C-p s d")     '(fn [] (seesaw.core/alert "you pressed C-c C-a s d")))
(global-set-key (kbd "C-c s")           '(fn [] (seesaw.core/alert "you pressed C-c s")))
(global-set-key (kbd "C-c f")            (fn [] (seesaw.core/alert "you pressed C-c f")))

(comment
  (swing-context/global-keymap))
