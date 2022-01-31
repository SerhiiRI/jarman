;; -*- mode: clojure; mode: rainbow;  mode: yafolding; -*-
(ns jarman.gui.components.common
  (:require
   [clojure.string :as string]
   [clojure.pprint :refer [cl-format]]
   ;; Seesaw
   [seesaw.core    :as c]
   [seesaw.border  :as b]
   ;; Jarman
   [jarman.tools.lang     :refer :all]
   [jarman.faces          :as face]
   [jarman.gui.gui-tools  :as gui-tool])
  (:import
   [java.awt Color]
   [javax.swing PopupFactory Action KeyStroke]
   [javax.swing.text
    DefaultEditorKit
    JTextComponent
    JTextComponent$KeyBinding]
   [java.awt.event
    KeyEvent
    InputEvent
    ActionEvent]
   [org.fife.ui.rtextarea
    RTextScrollPane
    RTextAreaEditorKit
    RTextAreaEditorKit$IncreaseFontSizeAction
    RTextAreaEditorKit$DecreaseFontSizeAction]
   [org.fife.ui.rsyntaxtextarea
    RSyntaxTextArea
    SyntaxScheme
    Token]))


;;  ____ _____  _    _   _ ____    _    ____ _____              
;; / ___|_   _|/ \  | \ | |  _ \  / \  |  _ \_   _|             
;; \___ \ | | / _ \ |  \| | | | |/ _ \ | |_) || |               
;;  ___) || |/ ___ \| |\  | |_| / ___ \|  _ < | |               
;; |____/ |_/_/   \_\_| \_|____/_/   \_\_| \_\|_|               
;;   ____ ___  __  __ ____   ___  _   _ _____ _   _ _____ ____  
;;  / ___/ _ \|  \/  |  _ \ / _ \| \ | | ____| \ | |_   _/ ___| 
;; | |  | | | | |\/| | |_) | | | |  \| |  _| |  \| | | | \___ \ 
;; | |__| |_| | |  | |  __/| |_| | |\  | |___| |\  | | |  ___) |
;;  \____\___/|_|  |_|_|    \___/|_| \_|_____|_| \_| |_| |____/ 
;; 


;; fixme:aleks create label
;; - we shuld have to type of labels. Label as button,
;;   and label with text, customizing and event reaction
;; - idiomatically it shuld'n be a button, more text
(defn label
  [& {:keys [value tgap bgap lgap rgap halign tip bg bg-hover underline-size underline-focus underline flip-border
             on-click on-focus-gain on-focus-lost on-mouse-enter on-mouse-exit args]    
      :or   {tgap 10 bgap 10 lgap 10 rgap 10
             value           ""
             tip             ""
             halign          :center
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

(defn- indexOfBracket [txt open-bracket close-bracket]
  (loop [stack 0 index 0 found false [t-char & t-rest-char] txt]
    ;; (println stack index t-char)
    (cond
      ;; ---
      (< stack 0) -1
      ;; --
      (and found (= stack 0)) (dec index)
      ;; ---
      (= t-char close-bracket)
      (if (= stack 1) (recur (dec stack) (inc index) found t-rest-char) (recur (dec stack) (inc index) found t-rest-char))
      ;; ---
      (= t-char open-bracket)
      (recur (inc stack) (inc index) true t-rest-char)
      ;; ---
      (empty? t-rest-char) -1
      ;; ---
      :else
      (recur stack (inc index) found t-rest-char))))

(def default-editor-kit-action-map
  (reduce (fn [acc action] (into acc {(.getValue action Action/NAME) action})) {} (.getActions (DefaultEditorKit.))))

(defn wrapp-emacs-keymap-rtext [someTextComponent]
  {:pre [(instance? org.fife.ui.rsyntaxtextarea.RSyntaxTextArea someTextComponent)]}
  (let [^javax.swing.InputMap  keyMap    (.getInputMap someTextComponent)
        ^javax.swing.ActionMap actionMap (.getActionMap someTextComponent)]
    (do
      (.put keyMap (KeyStroke/getKeyStroke KeyEvent/VK_EQUALS (bit-or InputEvent/CTRL_MASK InputEvent/SHIFT_MASK)) RTextAreaEditorKit/rtaIncreaseFontSizeAction)
      (.put actionMap RTextAreaEditorKit/rtaIncreaseFontSizeAction (RTextAreaEditorKit$IncreaseFontSizeAction.))
      (.put keyMap (KeyStroke/getKeyStroke KeyEvent/VK_MINUS  (bit-or InputEvent/CTRL_MASK InputEvent/SHIFT_MASK)) RTextAreaEditorKit/rtaDecreaseFontSizeAction)
      (.put actionMap RTextAreaEditorKit/rtaDecreaseFontSizeAction (RTextAreaEditorKit$DecreaseFontSizeAction.))      
      someTextComponent)))

(defn wrapp-emacs-keymap [someTextComponent]
  ;; fixme:aleks:serhii
  ;; 1. [ ] (KeyStroke/getKeyStroke KeyEvent/VK_R, InputEvent/CTRL_MASK, false) getAction("findBackward")
  ;; 2. [ ] (KeyStroke/getKeyStroke KeyEvent/VK_S, InputEvent/CTRL_MASK, false) getAction("findForward")
  ;; 3. [ ] Rework selection on cursor keeping mechanic
  ;; 4. [W] For `Alt+P` show tooltip(or something like that) with full text passed in it, replace `PopupFactory`
  ;; 5. [X] Jump to end of text and to the begin
  ;; 6. [X] Stacking brackets s-expression searcher
  ;; 7. [ ] refactor , make more modulatiry 
  {:pre [(instance? javax.swing.text.JTextComponent someTextComponent)]}
  (let [r-brackets {\[ \], \" \", \' \', \( \), \< \>}
        l-brackets {\] \[, \" \", \' \', \) \(, \> \<}
        excepts-chars #{\space \tab \newline}
        ^javax.swing.InputMap  keyMap    (.getInputMap someTextComponent)
        ^javax.swing.ActionMap actionMap (.getActionMap someTextComponent)]
    (do
      (.put keyMap (KeyStroke/getKeyStroke KeyEvent/VK_F (bit-or InputEvent/CTRL_MASK InputEvent/ALT_MASK))  "s-expr-forward")
      (.put keyMap (KeyStroke/getKeyStroke KeyEvent/VK_B (bit-or InputEvent/CTRL_MASK InputEvent/ALT_MASK))  "s-expr-backward")
      (.put keyMap (KeyStroke/getKeyStroke KeyEvent/VK_P InputEvent/ALT_MASK)  "show-full-text-popup")
      (.put keyMap (KeyStroke/getKeyStroke KeyEvent/VK_K InputEvent/CTRL_MASK) "delete-to-end-of-line")
      (.put keyMap (KeyStroke/getKeyStroke KeyEvent/VK_G InputEvent/CTRL_MASK) "unselect-selected-text")
      (.put (.getActionMap someTextComponent)                                  "s-expr-forward"
            (proxy [javax.swing.AbstractAction] []
              (^void actionPerformed [^java.awt.event.ActionEvent event]
               (let [component (^JTextComponent .getSource event)]
                 (let [t (.getText component)
                       currentCursor (.getCaretPosition component)
                       [a b] (split-at currentCursor t)]
                   (let [offset (reduce (fn [acc b-char]
                                          (if (excepts-chars b-char)
                                            (inc acc)
                                            (reduced acc))) 0 b)] 
                     (let [bracket (nth b offset)]
                       (if ((set (keys r-brackets)) (nth b offset))
                         (let [i (.indexOf (drop (inc offset) b) (get r-brackets bracket))]
                           (if (> i 0)
                             (.setCaretPosition component (+ currentCursor i 2 offset))))
                         (println "TODO move word-forward")))
                     (let [bracket (nth b offset)]
                       (cond
                         ;;---
                         (#{\[ \( \<} bracket)
                         (let [i (indexOfBracket (drop offset b) bracket (get r-brackets bracket))]
                           (if (> i 0)
                             (let [position (+ currentCursor i offset 1)]
                               ;; (println (str (nth t (dec position)) (nth t position) (nth t (inc position))))
                               ;; (println " ^ ")
                               (.setCaretPosition component position))))
                         
                         (#{\" \'} bracket)
                         (let [i (.indexOf (drop (inc offset) b) (get r-brackets bracket))]
                           (if (> i 0)
                             (let [position (+ currentCursor i 2 offset)]
                               ;; (println (str (nth t (dec position)) (nth t position) (nth t (inc position))))
                               ;; (println " ^ ")
                               (.setCaretPosition component position))))
                         :else
                         (if-let [^Action action (get default-editor-kit-action-map DefaultEditorKit/nextWordAction)]
                           (.actionPerformed action event))))))))))
      (.put (.getActionMap someTextComponent)                                  "s-expr-backward"
            (proxy [javax.swing.AbstractAction] []
              (^void actionPerformed [^java.awt.event.ActionEvent event]
               (let [component (^JTextComponent .getSource event)]
                 (let [t (.getText component)
                       currentCursor (.getCaretPosition component)
                       [a _] (split-at currentCursor t)
                       a (reverse a)]
                   (let [offset (reduce (fn [acc a-char]
                                          (if (excepts-chars a-char)
                                            (inc acc)
                                            (reduced acc))) 0 a)]
                     (let [bracket (nth a offset)]
                       (cond
                         ;;---
                         (#{\] \) \>} bracket)
                         (let [i (indexOfBracket (drop offset a) bracket (get l-brackets bracket))]
                           (if (> i 0)
                             (let [position (- (count a) (+ i offset 1))]
                               ;; (println (str (nth t (dec position)) (nth t position) (nth t (inc position))))
                               ;; (println " ^ ")
                               (.setCaretPosition component position))))
                         
                         (#{\" \'} bracket)
                         (let [i (.indexOf (drop (inc offset) a) (get l-brackets bracket))]
                           (if (> i 0)
                             (let [position (- (count a) (+ i 2 offset))]
                               ;; (println (str (nth t (dec position)) (nth t position) (nth t (inc position))))
                               ;; (println " ^ ")
                               (.setCaretPosition component position))))
                         :else
                         (if-let [^Action action (get default-editor-kit-action-map DefaultEditorKit/previousWordAction)]
                           (.actionPerformed action event))))))))))
      (.put (.getActionMap someTextComponent)                                  "unselect-selected-text"
            (proxy [javax.swing.AbstractAction] []
              (^void actionPerformed [^java.awt.event.ActionEvent event]
               (let [component (^JTextComponent .getSource event)]
                 (.setCaretPosition component 0)))))
      (.put (.getActionMap someTextComponent)                                  "show-full-text-popup"
            (proxy [javax.swing.AbstractAction] []
              (^void actionPerformed [^java.awt.event.ActionEvent event]
               (let [component (^JTextComponent .getSource event)]
                 ;; (print " W: " (.. component getSize getWidth) " H: " (.. component getSize getHeight))
                 ;; (print " X: " (.. component getLocationOnScreen getX))
                 ;; (println " Y: " (.. component getLocationOnScreen getY))
                 (let [^PopupFactory popup-factory (PopupFactory/getSharedInstance)]
                   (let [p (.getPopup popup-factory component
                                      (c/label :text (.getText component) :background "#d9ecff")
                                      (.. component getLocationOnScreen getX)
                                      (- (+ (.. component getLocationOnScreen getY) (.. component getSize getHeight)) 2))]
                     (.show p)))))))
      (.put (.getActionMap someTextComponent)                                  "delete-to-end-of-line"
            (proxy [javax.swing.AbstractAction] []
              (^void actionPerformed [^java.awt.event.ActionEvent event]
               (let [component (^JTextComponent .getSource event)
                     last-curret-position (.getCaretPosition component)]
                 ;; (println (.getCaretPosition component))
                 (let [[a b] (map (partial apply str) (split-at (.getCaretPosition component) (.getText component)))]
                   (.setText component (str a (if (= (first b) \newline)
                                                (string/replace-first b #"\n" "")
                                                (string/replace-first b #"[^\n]+" ""))))
                   (.setCaretPosition component last-curret-position))
                 ;; (.setText )
                 ))))
      (JTextComponent/loadKeymap
       (.getKeymap someTextComponent)
       (into-array
        JTextComponent$KeyBinding
        (list
         (JTextComponent$KeyBinding. (KeyStroke/getKeyStroke KeyEvent/VK_A InputEvent/CTRL_MASK)      DefaultEditorKit/beginLineAction)
         (JTextComponent$KeyBinding. (KeyStroke/getKeyStroke KeyEvent/VK_E InputEvent/CTRL_MASK)      DefaultEditorKit/endLineAction)
         (JTextComponent$KeyBinding. (KeyStroke/getKeyStroke KeyEvent/VK_B InputEvent/CTRL_MASK)      DefaultEditorKit/backwardAction)
         (JTextComponent$KeyBinding. (KeyStroke/getKeyStroke KeyEvent/VK_F InputEvent/CTRL_MASK)      DefaultEditorKit/forwardAction)
         (JTextComponent$KeyBinding. (KeyStroke/getKeyStroke KeyEvent/VK_B InputEvent/ALT_MASK)       DefaultEditorKit/previousWordAction)
         (JTextComponent$KeyBinding. (KeyStroke/getKeyStroke KeyEvent/VK_F InputEvent/ALT_MASK)       DefaultEditorKit/nextWordAction)
         (JTextComponent$KeyBinding. (KeyStroke/getKeyStroke KeyEvent/VK_P InputEvent/CTRL_MASK)      DefaultEditorKit/upAction)
         (JTextComponent$KeyBinding. (KeyStroke/getKeyStroke KeyEvent/VK_N InputEvent/CTRL_MASK)      DefaultEditorKit/downAction)
         (JTextComponent$KeyBinding. (KeyStroke/getKeyStroke KeyEvent/VK_V InputEvent/CTRL_MASK)      DefaultEditorKit/pageDownAction)
         (JTextComponent$KeyBinding. (KeyStroke/getKeyStroke KeyEvent/VK_U InputEvent/CTRL_MASK)      DefaultEditorKit/pageUpAction)
         (JTextComponent$KeyBinding. (KeyStroke/getKeyStroke KeyEvent/VK_D InputEvent/CTRL_MASK)      DefaultEditorKit/deleteNextCharAction)
         (JTextComponent$KeyBinding. (KeyStroke/getKeyStroke KeyEvent/VK_H InputEvent/CTRL_MASK)      DefaultEditorKit/deletePrevCharAction)
         (JTextComponent$KeyBinding. (KeyStroke/getKeyStroke KeyEvent/VK_O InputEvent/CTRL_MASK)      DefaultEditorKit/insertBreakAction)
         (JTextComponent$KeyBinding. (KeyStroke/getKeyStroke KeyEvent/VK_M InputEvent/CTRL_MASK)      DefaultEditorKit/insertBreakAction)
         
         (JTextComponent$KeyBinding. (KeyStroke/getKeyStroke KeyEvent/VK_Y InputEvent/CTRL_MASK)      DefaultEditorKit/pasteAction)
         (JTextComponent$KeyBinding. (KeyStroke/getKeyStroke KeyEvent/VK_W InputEvent/CTRL_MASK)      DefaultEditorKit/cutAction)
         (JTextComponent$KeyBinding. (KeyStroke/getKeyStroke KeyEvent/VK_W InputEvent/ALT_MASK)       DefaultEditorKit/copyAction)
         (JTextComponent$KeyBinding. (KeyStroke/getKeyStroke KeyEvent/VK_SPACE InputEvent/ALT_MASK)   DefaultEditorKit/selectAllAction)
         
         (JTextComponent$KeyBinding. (KeyStroke/getKeyStroke KeyEvent/VK_RIGHT   (bit-or InputEvent/CTRL_MASK InputEvent/SHIFT_MASK)) DefaultEditorKit/selectionNextWordAction)
         (JTextComponent$KeyBinding. (KeyStroke/getKeyStroke KeyEvent/VK_LEFT    (bit-or InputEvent/CTRL_MASK InputEvent/SHIFT_MASK)) DefaultEditorKit/selectionPreviousWordAction)
         (JTextComponent$KeyBinding. (KeyStroke/getKeyStroke KeyEvent/VK_PERIOD  (bit-or InputEvent/ALT_MASK InputEvent/SHIFT_MASK)) DefaultEditorKit/endAction)
         (JTextComponent$KeyBinding. (KeyStroke/getKeyStroke KeyEvent/VK_COMMA   (bit-or InputEvent/ALT_MASK InputEvent/SHIFT_MASK)) DefaultEditorKit/beginAction)))
       (.getActions someTextComponent))
      someTextComponent)))

(comment
  (-> (doto (c/frame
             :content
             (seesaw.mig/mig-panel
              :background  face/c-compos-background-darker
              :constraints ["wrap 1" "0px[grow, fill]0px" "0px[fill, top]0px"]
              :border (b/empty-border :thickness 10)
              :items [[(seesaw.core/label :text "some label")]
                      [(textarea :value "some<tag/> another \"<tag/>\"\ntext(es)a\"f ( \nfds(afs)af)slkafjslaf\"kj\n fdsla")]
                      [(seesaw.core/label :text "another label")]])
             :title "Jarman" :size [1000 :by 800])
        (.setLocationRelativeTo nil) seesaw.core/pack! seesaw.core/show!)))

(defn checkbox
  [& {:keys [text value on-click font-size args]
      :or   {text nil value false on-click (fn [e] e) font-size 14 args []}}]
  (apply
   c/checkbox
   :text (str (or text value))
   ;; :font (gtool/getFont font-size)
   :selected? value
   :border (b/empty-border :top 15)
   :listen
   [:mouse-clicked
    (fn [e]
      (on-click e))] args))

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
     (apply args)
     (wrapp-emacs-keymap))))

(defn- scrollbox
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

(defn textarea
  ;; fixme:aleks gui-components2/text input field
  ;; 1. write macro for border. Logic is understandable, but 
  ;;    it can be look better. Or you can make binding withou
  ;;    `nth`, than, go on!
  ;; 2. Consider to add a placeholder
  [& {:keys [value border border-color-focus border-color-unfocus
             ;; placeholder
             ;; font-size
             background ;; char-limit start-underline
             on-change on-focus-gain on-focus-lost on-caret-update
             args]
      :or   {value                 ""
             border                [10 10 5 5 2]
             border-color-focus    face/c-underline-on-focus
             border-color-unfocus  face/c-underline
             ;; placeholder           ""
             background            face/c-input-bg
             ;; start-underline       nil
             ;; char-limit            0
             
             on-change             (fn [e] e)
             on-focus-gain         (fn [e] e)
             on-focus-lost         (fn [e] e)
             on-caret-update       (fn [e] e)

             args []}}]
  (let [newBorder (fn [underline-color]
                    (b/compound-border (b/empty-border :left (nth border 0) :right (nth border 1) :top (nth border 2) :bottom (nth border 3))
                                       (b/line-border :bottom (nth border 4) :color underline-color)))]
    (let [text-area (c/to-widget (wrapp-emacs-keymap (javax.swing.JTextArea.)))]
      (c/config!
       text-area
       :text (rift value "")
       :minimum-size [50 :by 100]
       :background background
       :border (newBorder border-color-unfocus)
       :listen
       [:focus-gained (fn [e]
                        (c/config! e :border (newBorder border-color-focus))
                        (on-focus-gain e))
        :focus-lost   (fn [e]
                        (c/config! e :border (newBorder border-color-unfocus))
                        (on-focus-lost e))
        :caret-update (fn [e]
                        (on-change e))])
      ;; (on-chage text-area)
      (scrollbox text-area :minimum-size [50 :by 100]))))

(defn show-componet-actions
  "Example
    (show-componet-actions (text :value \"\"))
    (show-componet-actions (wrapp-emacs-keymap-rtext (seesaw.rsyntax/text-area :text \"\")))"
  [component]
  {:pre [(instance? javax.swing.JComponent component)]}
  (println
   (cl-format nil "ActionMap:~%~{  ~A~%~}Actions:~%~{  ~A~%~}"
              (->> (.keys (.getActionMap component)) (into []) (sort))
              (->> (.getActions component) (map (fn [a] (.getValue a Action/NAME))) (into []) (sort)))))
;; (show-componet-actions (wrapp-emacs-keymap-rtext (seesaw.rsyntax/text-area :text "")))

(defn codearea
  "Description:
    Some text area but with syntax styling.
    To check avaliable languages eval (seesaw.dev/show-options (seesaw.rsyntax/text-area)).
    Default language is Clojure.
  Example:
  ;; fixme:aleks
    (state-code-area {})
    (state-code-area :syntax :css)
  "
  [& {:keys [value
             language
             ;; border
             border-color-focus border-color-unfocus
             ;; placeholder
             ;; font-size
             background ;; char-limit start-underline
             on-change on-focus-gain on-focus-lost ;; on-caret-update
             args]
      :or   {value                 ""
             language              :plain
             ;; border                [10 10 5 5 2]
             border-color-focus    face/c-underline-on-focus
             border-color-unfocus  face/c-underline
             ;; placeholder           ""
             ;; background            face/c-input-bg
             ;; start-underline       nil
             ;; char-limit            0
             
             on-change             (fn [e] e)
             on-focus-gain         (fn [e] e)
             on-focus-lost         (fn [e] e)
             ;; on-caret-update       (fn [e] e)

             args []}}]
  (let [border-fn (fn [color] (b/line-border  :bottom 2 :color color))]
    (where
     ;; -------------------------------------------------
     ;; -- org.fife.ui.rsyntaxtextarea.RSyntaxTextArea --
     ;; -------------------------------------------------
     ;; URL: `https://javadoc.io/static/com.fifesoft/rsyntaxtextarea/3.1.6/org/fife/ui/rsyntaxtextarea/RSyntaxTextArea.html`
     ;;
     ((rTextArea
       (apply seesaw.rsyntax/text-area
              :text value
              :wrap-lines? true
              :caret-position 0
              :syntax language
              ;; :background background
              :border (border-fn border-color-unfocus)
              :listen [:focus-gained (fn [e] (c/config! e :border (border-fn border-color-focus)) (on-focus-gain e))
                       :focus-lost   (fn [e] (c/config! e :border (border-fn border-color-unfocus)) (on-focus-lost e))
                       :caret-update (fn [e] (on-change e))] args))
      ;; ---
      (scheme (.getSyntaxScheme rTextArea))
      ;; (theme (org.fife.ui.rsyntaxtextarea.Theme. rTextArea))
      )
     
     (doto rTextArea
       (.setBackground (Color/decode "#282A36"))
       (.setCaretColor (Color/decode "#F8F8F0"))
       (.setSelectedTextColor (Color/decode "#F8F8F2"))
       (.setSelectionColor (Color/decode "#424450"))
       
       (.setCurrentLineHighlightColor (Color/decode "#44475A"))
       (.setFadeCurrentLineHighlight false)
       
       (.setMarginLineColor (Color/decode "#b0b4b9"))
       
       (.setMarkAllHighlightColor (Color/decode "#ffc800"))
       (.setMarkOccurrences false)
       (.setMarkOccurrencesColor (Color/decode "#d4d4d4"))

       (.setBracketMatchingEnabled true)
       (.setMatchedBracketBGColor (Color/decode "#282A36"))
       (.setMatchedBracketBorderColor (Color/decode "#F8F8F0"))
       (.setPaintMarkOccurrencesBorder true) ;; ?
       (.setAnimateBracketMatching false)
       (.setPaintMatchedBracketPair false)

       (.setCodeFoldingEnabled true)
       (.setAntiAliasingEnabled true)

       ;; fixme
       ;; (.setHighlightSecondaryLanguages true)
       ;; (.setSecondaryLanguageBackground (int 0) (Color/decode "#fff0cc"))
       ;; (.setSecondaryLanguageBackground (int 1) (Color/decode "#dafeda"))
       ;; (.setSecondaryLanguageBackground (int 2) (Color/decode "#ffe0f0"))

       )

     ;; ----------------------------------------------
     ;; -- org.fife.ui.rsyntaxtextarea.SyntaxScheme --
     ;; ----------------------------------------------
     ;; URL: `https://javadoc.io/static/com.fifesoft/rsyntaxtextarea/3.1.6/org/fife/ui/rsyntaxtextarea/SyntaxScheme.html`
     ;; URL: `https://javadoc.io/static/com.fifesoft/rsyntaxtextarea/3.1.6/org/fife/ui/rsyntaxtextarea/TokenTypes.html`
     ;; URL: `https://javadoc.io/static/com.fifesoft/rsyntaxtextarea/3.1.6/org/fife/ui/rsyntaxtextarea/Token.html`

     ;; (set! (.-bold (.getStyle scheme Token/DATA_TYPE)) true)
     ;; (set! (.-bold (.getStyle scheme Token/VARIABLE)) true)
     ;; (set! (.-bold (.getStyle scheme Token/RESERVED_WORD)) true)
     ;; (set! (.-bold (.getStyle scheme Token/RESERVED_WORD_2)) false)
     ;; (set! (.-bold (.getStyle scheme Token/PREPROCESSOR)) false)
     ;; (set! (.-bold (.getStyle scheme Token/COMMENT_KEYWORD)) true)
     ;; (set! (.-bold (.getStyle scheme Token/LITERAL_BOOLEAN)) false)
     
     (set! (.-foreground (.getStyle scheme Token/IDENTIFIER)) (Color/decode "#F8F8F2"))
     (set! (.-foreground (.getStyle scheme Token/DATA_TYPE)) (Color/decode "#8BE9FD"))
     (set! (.-foreground (.getStyle scheme Token/FUNCTION)) (Color/decode "#50FA7B"))
     (set! (.-foreground (.getStyle scheme Token/VARIABLE)) (Color/decode "#8BE9FD"))
     (set! (.-foreground (.getStyle scheme Token/RESERVED_WORD)) (Color/decode "#8BE9FD"))
     (set! (.-foreground (.getStyle scheme Token/RESERVED_WORD_2)) (Color/decode "#8BE9FD"))
     (set! (.-foreground (.getStyle scheme Token/PREPROCESSOR)) (Color/decode "#FF79C6"))

     (set! (.-foreground (.getStyle scheme Token/ANNOTATION)) (Color/decode "#6272A4"))
     (set! (.-foreground (.getStyle scheme Token/COMMENT_DOCUMENTATION)) (Color/decode "#6272A4"))
     (set! (.-foreground (.getStyle scheme Token/COMMENT_EOL)) (Color/decode "#6272A4"))
     (set! (.-foreground (.getStyle scheme Token/COMMENT_MULTILINE)) (Color/decode "#6272A4"))
     (set! (.-foreground (.getStyle scheme Token/COMMENT_KEYWORD)) (Color/decode "#6272A4"))
     (set! (.-foreground (.getStyle scheme Token/COMMENT_MARKUP)) (Color/decode "#6272A4"))
     (set! (.-foreground (.getStyle scheme Token/LITERAL_BOOLEAN)) (Color/decode "#BD93F9"))
     (set! (.-foreground (.getStyle scheme Token/LITERAL_NUMBER_DECIMAL_INT)) (Color/decode "#BD93F9"))
     (set! (.-foreground (.getStyle scheme Token/LITERAL_NUMBER_FLOAT)) (Color/decode "#BD93F9"))
     (set! (.-foreground (.getStyle scheme Token/LITERAL_NUMBER_HEXADECIMAL)) (Color/decode "#BD93F9"))
     (set! (.-foreground (.getStyle scheme Token/LITERAL_STRING_DOUBLE_QUOTE)) (Color/decode "#F1FA8C"))
     (set! (.-foreground (.getStyle scheme Token/LITERAL_CHAR)) (Color/decode "#FFB86C"))
     (set! (.-foreground (.getStyle scheme Token/LITERAL_BACKQUOTE)) (Color/decode "#F1FA8C"))

     (set! (.-foreground (.getStyle scheme Token/OPERATOR)) (Color/decode "#FF79C6"))
     (set! (.-foreground (.getStyle scheme Token/REGEX)) (Color/decode "#8BE9FD"))
     (set! (.-foreground (.getStyle scheme Token/SEPARATOR)) (Color/decode "#50FA7B"))
     (set! (.-foreground (.getStyle scheme Token/WHITESPACE)) (Color/decode "#BD93F9"))
     (set! (.-foreground (.getStyle scheme Token/ERROR_IDENTIFIER)) (Color/decode "#FF5555"))
     (set! (.-foreground (.getStyle scheme Token/ERROR_NUMBER_FORMAT)) (Color/decode "#FF5555"))
     (set! (.-foreground (.getStyle scheme Token/ERROR_STRING_DOUBLE)) (Color/decode "#FF5555"))
     (set! (.-foreground (.getStyle scheme Token/ERROR_CHAR)) (Color/decode "#FF5555"))
     (where 
      ((rTextScrollPane
        (-> rTextArea
            (wrapp-emacs-keymap)
            (wrapp-emacs-keymap-rtext)
            (RTextScrollPane.)))
       (rTextScrollPaneGutter (.getGutter rTextScrollPane)))
      (doto rTextScrollPaneGutter
        (.setBorderColor (Color/decode "#282A36"))
        (.setLineNumberColor (Color/decode "#6272A4"))
        (.setFoldIndicatorForeground (Color/decode "#6272A4"))
        (.setFoldBackground (Color/decode "#282A36"))
        (.setActiveLineRangeColor (Color/decode "#3399ff")))
      rTextScrollPane))))

(defn combobox
  [& {:keys [value model tgap bgap lgap rgap halign tip bg bg-hover underline-size underline-focus underline flip-border
             on-select on-click on-focus-gain on-focus-lost on-mouse-enter on-mouse-exit args]    
      :or   {value           ""
             tgap 10 bgap 10 lgap 10 rgap 10
             tip             ""
             model           []
             bg              face/c-btn-bg
             bg-hover        face/c-btn-bg-focus
             underline-focus face/c-btn-underline-on-focus
             underline       face/c-btn-underline
             underline-size  face/s-btn-underline
             flip-border     false

             on-select       (fn [e] e)
             on-click        (fn [e] e)
             on-focus-gain   (fn [e] e)
             on-focus-lost   (fn [e] e)
             on-mouse-enter  (fn [e] e)
             on-mouse-exit   (fn [e] e)
             
             args []}}]
  (blet
   (apply 
    seesaw.core/combobox
    :model model
    :selected-item value
    :focusable? true
    :background bg
    :border (new-border underline)
    :tip tip
    :listen
    [:selection
     (fn [e]
       (on-select (c/selection e)))
     :mouse-clicked
     (fn [e]
       (on-click (c/selection e))
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
       (on-focus-lost e))]
    args)
   [;; fixme: implement self cell-render
    ;; renderer (seesaw.cells/default-list-cell-renderer
    ;;           (fn [renderer {:keys [this focus? selected?] :as info}]
    ;;             (let [e this]
    ;;               (cond
    ;;                 focus? (seesaw.core/config! e :background bg-hover)
    ;;                 selected? (seesaw.core/config! e :background bg)
    ;;                 :else (seesaw.core/config! e :background bg))) nil))
    new-border
    (fn [underline-color]
      (b/compound-border (b/empty-border :bottom bgap :top tgap :left lgap :right rgap)
                         (b/line-border (if flip-border :top :bottom) underline-size :color underline-color)))]))

;;  ____  _____ __  __  ___ 
;; |  _ \| ____|  \/  |/ _ \
;; | | | |  _| | |\/| | | | |
;; | |_| | |___| |  | | |_| |
;; |____/|_____|_|  |_|\___/
;;

(comment
  (seesaw.dev/show-options (c/frame))

  ;; All components
  (-> (doto (seesaw.core/frame
             :title "Jarman" 
             :content (seesaw.mig/mig-panel
                       :background  face/c-compos-background-darker
                       :constraints ["wrap 1" "0px[grow, fill]0px" "0px[fill, top]0px"]
                       :border (b/empty-border :thickness 10)
                       :items [[(label :value "(label) test component")]
                               [(button :value "(button) test component ")]
                               [(text :value "(text) input field which (support (Emacs)) keybindings")]
                               [(textarea :value "(textarea) multiline text-area field\n which (support (Emacs)) keybindings\nand wrapped through the scroll-\nbar")]
                               [(codearea :value ";;(codearea) which (support\n;; (Emacs)) keybindings\n\n(fn [x] \n\t(label :value \"Some test label\"))" :language :clojure)]
                               [(combobox :value "A" :model ["A" "B" "C"])]
                               [(combobox :value 1 :model [1 2 3])]]))
        (.setLocationRelativeTo nil) seesaw.core/pack! seesaw.core/show!))

  ;; CODE AREA
  (-> (doto (c/frame
             :title "Jarman"
             :minimum-size [1000 :by 300]
             :content
             (seesaw.core/border-panel
              :items [[(codearea
                        ;; :minimum-size [1000 :by 400]
                        :value "(comment
  (-> (doto (c/frame
             :content
             (seesaw.mig/mig-panel
              :background  face/c-compos-background-darker
              :constraints [\"wrap 1\" \"0px[grow, fill]0px\" \"0px[fill, top]0px\"]
              :border (b/empty-border :thickness 10)
              :items [[(seesaw.core/label :text \"some very wide label text\")]
                      [(codearea :value \"(defn suka [s] \\n   (println \\\"bliat\\\"))\" :language :clojure)]
                      [(seesaw.core/label :text \"another label\")]])
             :title \"Jarman\" :size [1000 :by 800])
        (.setLocationRelativeTo nil) seesaw.core/pack! seesaw.core/show!)))" :language :clojure) :north]]))
        (.setLocationRelativeTo nil) seesaw.core/pack! seesaw.core/show!))
  )
