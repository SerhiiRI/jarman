;; -*- mode: clojure; mode: rainbow;  mode: yafolding; -*-
(ns jarman.gui.components.common
  (:require
   [clojure.string :as string]
   [clojure.pprint :refer [cl-format]]
   ;; Seesaw
   [seesaw.core    :as c]
   [seesaw.border  :as b]
   ;; Jarman
   [jarman.lang           :refer :all]
   [jarman.faces          :as face]
   [jarman.gui.gui-tools  :as gui-tool]
   [jarman.gui.components.swing :as swing]
   [jarman.gui.components.swing-context :refer :all]
   [jarman.gui.components.swing-keyboards :refer [define-key kbd kvc wrapp-keymap]])
  (:import
   [java.awt Color Font]
   [javax.swing PopupFactory Action KeyStroke]
   [javax.swing.text
    DefaultEditorKit
    JTextComponent
    JTextComponent$KeyBinding]
   [java.awt.event
    KeyAdapter
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

;; keyboard configuration keymap wrappers
(defn patch-emacs-keymap [keymap]
  (-> keymap
      (define-key (kbd "C-a")        DefaultEditorKit/beginLineAction)
      (define-key (kbd "C-e")        DefaultEditorKit/endLineAction)
      (define-key (kbd "C-b")        DefaultEditorKit/backwardAction)
      (define-key (kbd "left")       DefaultEditorKit/backwardAction)
      (define-key (kbd "C-f")        DefaultEditorKit/forwardAction)
      (define-key (kbd "right")      DefaultEditorKit/forwardAction)
      (define-key (kbd "M-b")        DefaultEditorKit/previousWordAction)
      (define-key (kbd "M-f")        DefaultEditorKit/nextWordAction)
      (define-key (kbd "C-p")        DefaultEditorKit/upAction)
      (define-key (kbd "up")         DefaultEditorKit/upAction)
      (define-key (kbd "C-n")        DefaultEditorKit/downAction)
      (define-key (kbd "down")       DefaultEditorKit/downAction)
      (define-key (kbd "C-v")        DefaultEditorKit/pageDownAction)
      (define-key (kbd "C-u")        DefaultEditorKit/pageUpAction)
      (define-key (kbd "C-d")        DefaultEditorKit/deleteNextCharAction)
      (define-key (kbd "delete")     DefaultEditorKit/deleteNextCharAction)
      (define-key (kbd "backspace")  DefaultEditorKit/deletePrevCharAction)
      (define-key (kbd "C-o")        DefaultEditorKit/insertBreakAction)
      (define-key (kbd "C-m")        DefaultEditorKit/insertBreakAction)
      (define-key (kbd "C-y")        DefaultEditorKit/pasteAction)
      (define-key (kbd "C-w")        DefaultEditorKit/cutAction)
      (define-key (kbd "M-w")        DefaultEditorKit/copyAction)
      (define-key (kbd "M-space")    DefaultEditorKit/selectAllAction)
      (define-key (kbd "C-S-right")  DefaultEditorKit/selectionNextWordAction)
      (define-key (kbd "C-S-left")   DefaultEditorKit/selectionPreviousWordAction)
      (define-key (kbd "M-S-period") DefaultEditorKit/endAction)
      (define-key (kbd "M-S-comma")  DefaultEditorKit/beginAction)
      (define-key (kbd "C-M-f")      'jarman.gui.components.swing-actions/s-expr-forward)
      (define-key (kbd "C-M-b")      'jarman.gui.components.swing-actions/s-expr-backward)
      (define-key (kbd "C-k")        'jarman.gui.components.swing-actions/delete-to-end-of-line)
      (define-key (kbd "C-g")        'jarman.gui.components.swing-actions/unselect-selected-text)
      (define-key (kbd "M-p")        'jarman.gui.components.swing-actions/show-full-text-popup)))

(defn patch-rtext-keymap [keymap]
  (-> keymap
      (define-key (kbd "C-+") jarman.gui.components.swing-actions/rtext-increase-font-size)
      (define-key (kbd "C--") jarman.gui.components.swing-actions/rtext-decrease-font-size)))

(defn stub [& args]
  (c/label
    :text (format "STUB: %s" (pr-str args))
    :border (swing/border
              {:a 10 :color face/c-red})
    :background face/c-red))

(defn label
  [& {:keys [value halign tooltip foreground-hover foreground background font on-click args]
      :or   {value            ""
             halign           :left
             foreground-hover face/c-on-focus
             foreground       face/c-foreground
             background       face/c-layout-background
             font             {:name face/f-regular :size face/s-foreground}
             args []}}]
  (apply
    c/label
    :text value
    :halign halign
    :foreground foreground
    :background background
    :font font
    (cond-> args
      ;; ------
      tooltip
      (into [:tip tooltip])
      ;; ------
      on-click
      (into
        [:listen
         [:mouse-entered (fn [e] (c/config! e :foreground foreground-hover :cursor :hand) (.repaint (c/to-root e)))
          :mouse-exited  (fn [e] (c/config! e :foreground foreground) (.repaint (c/to-root e)))
          :mouse-clicked (fn [e] (on-click e))]]))))

(def label-h1   (partial label :font {:name face/f-regular :size face/s-foreground-h1}))
(def label-h2   (partial label :font {:name face/f-regular :size face/s-foreground-h2}))
(def label-h3   (partial label :font {:name face/f-regular :size face/s-foreground-h3}))
(def label-h4   (partial label :font {:name face/f-regular :size face/s-foreground-h4}))
(def label-h5   (partial label :font {:name face/f-regular :size face/s-foreground-h5}))
(def label-h6   (partial label :font {:name face/f-regular :size face/s-foreground-h6}))
(def label-link (partial label :font {:name face/f-italic-regular :size face/s-foreground}))
(def label-info (partial label :font {:name face/f-italic-regular :size face/s-foreground}))

(defn button
  [& {:keys [value font
             border border-focus
             foreground foreground-hover
             background background-hover
             on-click on-focus-gain on-focus-lost on-mouse-enter on-mouse-exit
             halign tooltip args]
      :or   {value ""
             halign           :center
             foreground       face/c-foreground
             foreground-hover face/c-foreground
             background       face/c-btn-bg
             background-hover face/c-btn-bg-focus
             border           (swing/border
                                {:b 10 :t 10 :l 10 :r 10}
                                {:b face/s-btn-underline :color face/c-btn-underline})
             border-focus     (swing/border
                                {:b 10 :t 10 :l 10 :r 10}
                                {:b face/s-btn-underline :color face/c-btn-underline-on-focus})
             font             {:name face/f-regular :size face/s-foreground}
             on-click         (fn [e] e)
             on-focus-gain    (fn [e] e)
             on-focus-lost    (fn [e] e)
             on-mouse-enter   (fn [e] e)
             on-mouse-exit    (fn [e] e)
             args             []}}]
  (apply c/label
    :text value
    :focusable? true
    :halign halign
    :foreground foreground
    :font font
    :listen
    [:mouse-clicked (fn [e] (on-click e) (gui-tool/switch-focus))
     :mouse-entered (fn [e]
                      (c/config! e :border border-focus :cursor :hand
                        :background background-hover :foreground foreground-hover)
                      (.repaint (c/to-root e))
                      (on-mouse-enter e))
     :mouse-exited (fn [e]
                     (c/config! e :border border
                       :background background :foreground foreground)
                     (.repaint (c/to-root e))
                     (on-mouse-exit e))
     :focus-gained (fn [e]
                     (c/config! e :border border-focus :cursor :hand
                       :background background-hover :foreground foreground-hover)
                     (on-focus-gain e))
     :focus-lost (fn [e] (c/config! e :border border :background background)
                   (on-focus-lost e))
     :key-pressed (fn [e]
                    (when (= (.getKeyCode e) java.awt.event.KeyEvent/VK_ENTER)
                      (on-click e)
                      (gui-tool/switch-focus)))]
    :background background
    :border border
    (cond-> args
      ;; ------
      tooltip
      (into [:tip tooltip]))))

(defn checkbox
  [& {:keys[value value-setter value-text border on-click tooltip font args]
      :or  {value nil
            value-text nil
            value-setter identity
            on-click (fn [e] e)
            font    {:name face/f-regular :size face/s-foreground}
            border  (swing/border {:h 10 :v 5})
            args []}}]
  (apply
    c/checkbox
    :selected? (value-setter value)
    :text      (str value-text)
    :font      font
    :border    border
    :listen    [:mouse-clicked
                (fn [e]
                  (on-click e))]
    (cond-> args
      ;; ------
      tooltip
      (into [:tip tooltip]))))

(defn text
  [& {:keys [value value-setter
             border border-focus foreground background tooltip font
             on-change on-focus-gain on-focus-lost on-caret-update
             args]
      :or   {value                 ""
             value-setter          str
             ;; border                [10 10 5 5 2]
             border               (swing/border
                                    {:h 10 :v 5}
                                    {:b 2 :color face/c-underline})
             border-focus         (swing/border
                                    {:h 10 :v 5}
                                    {:b 2 :color face/c-underline-on-focus})

             border-color-focus    face/c-underline-on-focus
             border-color-unfocus  face/c-underline
             foreground            face/c-foreground
             background            face/c-input-bg
             font                  {:name face/f-regular :size face/s-foreground}

             on-change             (fn [e] e)
             on-focus-gain         (fn [e] e)
             on-focus-lost         (fn [e] e)
             on-caret-update       (fn [e] e)

             args []}}]
  (let [;; fixme:aleks gui-components2/text input field
        ;; 1. `get-user-data`, `set-user-data` can be removed?
        ;; last-v          (atom "")
        get-user-data   (fn [e k]   (get-in   (c/config e :user-data) [k]))
        set-user-data   (fn [e k v] (assoc-in (c/config e :user-data) [k] v))]
    (->
      (partial c/text
        :text       (str (value-setter value))
        :foreground foreground
        :background background
        :border     border
        :user-data  {:value "" :edit? false :type :input}
        :font       font
        :listen
        [:focus-gained
         (fn [e]
           (c/config! e :border border-focus)
           (c/config! e :user-data (set-user-data e :edit? true))
           (on-focus-gain e))
         :focus-lost
         (fn [e]
           (c/config! e :border border)
           (c/config! e :user-data (set-user-data e :edit? false))
           (on-focus-lost e))
         :caret-update
         (fn [e]
           ;; (let [new-v (c/value (c/to-widget e))]
           ;;   (if (and (> (count new-v) char-limit) (< 0 char-limit))
           ;;     ;; (c/invoke-later (c/config! e :text @last-v))
           ;;     (do
           ;;       (reset! last-v new-v)
           ;;       (c/config! e :user-data (set-user-data e :value (if (= placeholder @last-v) "" @last-v)))
           ;;       (on-change e))))
           (on-change e)
           )])
     (apply
       (cond-> args
         tooltip
         (into [:tip tooltip])))
     (wrapp-keymap
      (-> (active-keymap) (patch-emacs-keymap))
      jarman.gui.components.swing-actions/default-editor-kit-action-map))))

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

(defn textarea
  [& {:keys [value value-setter border border-focus
             foreground background enabled? font tooltip
             on-change on-focus-gain on-focus-lost on-caret-update
             args]
      :or   {value                 ""
             value-setter          str
             border               (swing/border
                                    {:h 10 :v 5}
                                    {:b 2 :color face/c-underline})
             border-focus         (swing/border
                                    {:h 10 :v 5}
                                    {:b 2 :color face/c-underline-on-focus})
             foreground            face/c-foreground
             background            face/c-input-bg
             enabled?              true
             font                  {:name face/f-regular :size face/s-foreground}
             ;; start-underline       nil
             ;; char-limit            0

             on-change             (fn [e] e)
             on-focus-gain         (fn [e] e)
             on-focus-lost         (fn [e] e)
             on-caret-update       (fn [e] e)

             args []}}]
  (->
    (partial c/text
      :text (value-setter (rift value ""))
      :minimum-size [50 :by 100]
      :background background
      :border border
      :enabled? enabled?
      :wrap-lines? true
      :multi-line? true
      :font font
      :listen
      [:focus-gained (fn [e]
                       (c/config! e :border border-focus)
                       (on-focus-gain e))
       :focus-lost   (fn [e]
                       (c/config! e :border border)
                       (on-focus-lost e))
       :caret-update (fn [e]
                       (on-change e))])
    (apply
      (cond-> args
        tooltip
        (into [:tip tooltip])))
    (wrapp-keymap (-> (active-keymap) (patch-emacs-keymap))
      jarman.gui.components.swing-actions/default-editor-kit-action-map)
    (scrollbox :minimum-size [50 :by 100])))

(defn codearea
  "Description:
    Some text area but with syntax styling.
    To check avaliable languages eval (seesaw.dev/show-options (seesaw.rsyntax/text-area)).
    Default language is Clojure.
  Example:

    (state-code-area {})
    (state-code-area :syntax :css)"
  [& {:keys [value value-setter
             language font border border-focus
             on-change on-focus-gain on-focus-lost
             args]
      :or   {value                 ""
             value-setter          str
             language              :plain
             font                  {:name face/f-mono-regular :size face/s-foreground}
             border                (swing/border
                                     {:h 10 :v 5}
                                     {:b 2 :color face/c-underline})
             border-focus          (swing/border
                                     {:h 10 :v 5}
                                     {:b 2 :color face/c-underline-on-focus})
             on-change             (fn [e] e)
             on-focus-gain         (fn [e] e)
             on-focus-lost         (fn [e] e)
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
              :font font
              ;; :background background
              :border border
              :listen [:focus-lost   (fn [e] (c/config! e :border border) (on-focus-lost e))
                       :focus-gained (fn [e] (c/config! e :border border-focus) (on-focus-gain e))
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
            (wrapp-keymap
             (-> (active-keymap) (patch-emacs-keymap) (patch-rtext-keymap))
             jarman.gui.components.swing-actions/default-rtext-editor-kit-action-map)
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
  [& {:keys [value value-setter model
             border border-focus font halign tooltip background background-hover
             on-select on-click on-focus-gain on-focus-lost on-mouse-enter on-mouse-exit
             args]
      :or   {value-setter     identity
             model            []
             background       face/c-btn-bg
             background-hover face/c-btn-bg-focus
             font             {:name face/f-mono-regular :size face/s-foreground}
             border           (swing/border {:h 10 :v 2} {:b 2 :color face/c-underline})
             border-focus     (swing/border {:h 10 :v 2} {:b 2 :color face/c-underline-on-focus})
             on-select        (fn [e] e)
             on-click         (fn [e] e)
             on-focus-gain    (fn [e] e)
             on-focus-lost    (fn [e] e)
             on-mouse-enter   (fn [e] e)
             on-mouse-exit    (fn [e] e)

             args []}}]
  [;; fixme: implement self cell-render
   ;; renderer (seesaw.cells/default-list-cell-renderer
   ;;           (fn [renderer {:keys [this focus? selected?] :as info}]
   ;;             (let [e this]
   ;;               (cond
   ;;                 focus? (seesaw.core/config! e :background bg-hover)
   ;;                 selected? (seesaw.core/config! e :background bg)
   ;;                 :else (seesaw.core/config! e :background bg))) nil))
   ]
  (apply
    seesaw.core/combobox
    :model model
    :selected-item (value-setter value)
    :focusable? true
    :background background
    :border border
    :font font
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
       (c/config! e :border border-focus :background background-hover :cursor :hand)
       (.repaint (c/to-root e))
       (on-mouse-enter e))
     :mouse-exited
     (fn [e]
       (c/config! e :border border :background background)
       (.repaint (c/to-root e))
       (on-mouse-exit e))
     :focus-gained
     (fn [e]
       (c/config! e :border border-focus :background background-hover :cursor :hand)
       (on-focus-gain e))
     :focus-lost
     (fn [e]
       (c/config! e :border border :background background)
       (on-focus-lost e))]
    (cond-> args
      tooltip
      (into [:tip tooltip]))))

;;  ____  _____ __  __  ___
;; |  _ \| ____|  \/  |/ _ \
;; | | | |  _| | |\/| | | | |
;; | |_| | |___| |  | | |_| |
;; |____/|_____|_|  |_|\___/
;;

(comment
  ;;;;;;;;;;
  ;;
  (doto (seesaw.core/frame
         :title "Jarman"
         :content
         (text :value "(label) test component"))
    ;; (swing/choose-screen! 1)
    (.setLocationRelativeTo nil)
    (seesaw.core/pack!)
    (seesaw.core/show!))
  ;;
  ;;;;;;;;;;;
  ;;
  (doto (seesaw.core/frame
         :title "Jarman"
         :content
         (let [input-keymap (-> (active-keymap) (patch-emacs-keymap))
               input-action jarman.gui.components.swing-actions/default-editor-kit-action-map
               l0 (c/label :text "Keybindings          :" :font {:size 20})
               s1 (c/label :text "|")
               l1 (c/label :text "-----------" :font {:size 20})
               l2 (c/label :text "-----------" :font {:size 20})
               t1 (c/text  :text "one (text) fdosaf" :background "#aec")
               l3 (c/label :text "-----------" :font {:size 20})
               l4 (c/label :text "-----------" :font {:size 20})
               t2 (c/text  :text "next text" :background "#aec")
               p  (c/vertical-panel :items [l0 s1 l1 l2 t1 l3 l4 t2])]
           (.setFocusable p true)
           ;; (.setParent (.getInputMap t1) nil)
           ;; (.remove (.getActionMap t1) DefaultEditorKit/insertContentAction)
           ;; (describe-keymap input-keymap)
           (wrapp-keymap t1
                         (-> (active-keymap) (patch-emacs-keymap))
                         jarman.gui.components.swing-actions/default-editor-kit-action-map)
           (wrapp-keymap p)
           p))
    ;; (swing/choose-screen! 1)
    (.setLocationRelativeTo nil)
    (seesaw.core/pack!)
    (seesaw.core/show!)))

(comment
  (seesaw.dev/show-options (c/frame))

  (swing/quick-frame
    [(checkbox :value true :value-text "Some checkbox"
       :on-click (fn [e] (println "Some test")))])

  (swing/quick-frame
    [(seesaw.mig/mig-panel
       :background  face/c-compos-background-darker
       :constraints ["wrap 1" "0px[grow, fill]0px" "0px[fill, top]0px"]
       :border (b/empty-border :thickness 10)
       :items [[(seesaw.core/label :text "some label")]
               [(seesaw.core/label :text "another label")]])])

  (swing/quick-frame
    [(seesaw.mig/mig-panel
       :background  face/c-compos-background-darker
       :constraints ["wrap 1" "0px[grow, fill]0px" "0px[fill, top]0px"]
       :border (b/empty-border :thickness 10)
       :items [[(checkbox :value true :value-text "Some checkbox" :on-click (fn [e] (println "Some test")))]
               [(label :value "(label) test component")]
               [(button :value "(button) test component")]
               [(text :value "(text) input field which (support (Emacs)) keybindings")]
               [(textarea :value "(textarea) multiline \ntext-area field\n which (support (Emacs)) keybindings\nand wrapped through the scroll-\nbar")]
               [(textarea :value "some<tag/> another \"<tag/>\"\ntext(es)a\"f ( \nfds(afs)af)slkafjslaf\"kj\n fdsla")]
               [(codearea :value ";;(codearea) which (support\n;; (Emacs)) keybindings\n\n(fn [x] \n\t(label :value \"Some test label\"))" :language :clojure)]
               [(combobox :value "A" :model ["A" "B" "C"])]
               [(combobox :value 1 :model [1 2 3])]])])

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

  (swing/quick-frame
    [(codearea
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
        (.setLocationRelativeTo nil) seesaw.core/pack! seesaw.core/show!)))" :language :clojure)])

  (swing/quick-frame
    [(label-info :value "Some text i should to place here" :on-click identity)
     (label-link :value "Some text i should to place here" :on-click identity)
     (label-h1 :value "Some text i should to place here" :on-click identity)
     (label-h2 :value "Some text i should to place here" :on-click identity)
     (label-h3 :value "Some text i should to place here" :on-click identity)
     (label-h4 :value "Some text i should to place here" :on-click identity)
     (label-h5 :value "Some text i should to place here")
     (label-h6 :value "Some text i should to place here")
     (label :value "Some text i should to place here")
     (seesaw.core/label :text "Some text i should to place here" :font (swing/font Font/SERIF 20))
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
     (seesaw.core/label :text "Some text i should to place here" :font {:name "JetBrains Mono ExtraBold Italic" :size 20})]))
