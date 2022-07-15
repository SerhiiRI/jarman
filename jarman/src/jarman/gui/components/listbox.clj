(ns jarman.gui.components.autocompletion
  (:require
   [clojure.string :as string]
   [clojure.pprint :refer [cl-format]]
   [clojure.core.async :as async]
   ;; Seesaw
   [seesaw.core    :as c]
   [seesaw.border  :as b]
   ;; Jarman
   [jarman.lang                  :refer :all]
   [jarman.lib.fuzzy-search]
   [jarman.faces                 :as face]
   [jarman.gui.gui-tools         :as gui-tool]
   [jarman.gui.components.panels :as gui-panels]
   [jarman.gui.components.swing  :as swing]
   [jarman.gui.core              :refer [satom register! cursor]]
   [jarman.gui.components.common :refer [text scrollbox patch-emacs-keymap]]
   [jarman.gui.components.swing-context
    :refer :all]
   [jarman.gui.components.swing-keyboards
    :as swing-keyboards
    :refer [define-key kbd kvc wrapp-keymap]])
  (:import
   [java.awt Color]
   [javax.swing ;; PopupFactory Action
    KeyStroke]
   [javax.swing.text
    StyleContext
    ;; DefaultHighlighter
    ;; DefaultHighlighter$DefaultHighlightPainter
    DefaultEditorKit
    JTextComponent
    ;; JTextComponent$KeyBinding
    ]
   [java.awt.event
    KeyEvent
    InputEvent
    ActionEvent
    ]
   ;; [org.fife.ui.rtextarea
   ;;  RTextScrollPane
   ;;  RTextAreaEditorKit
   ;;  RTextAreaEditorKit$IncreaseFontSizeAction
   ;;  RTextAreaEditorKit$DecreaseFontSizeAction]
   ;; [org.fife.ui.rsyntaxtextarea
   ;;  RSyntaxTextArea
   ;;  SyntaxScheme
   ;;  Token]
   ))

(defn- candidate-label [word selected selected-char]
  (let [t (c/styled-text :text word
                         :styles [[:selected :color (Color/decode "#00AAAA")]]
                         :background (if selected :aliceblue :white)
                         :editable? false
                         :listen [:mouse-clicked (fn [e]
                                                   (println (c/value (c/to-widget e))))])]
    (doall
     (for [c selected-char]
       (c/style-text! t :selected c 1)))
    t))

(defn patch-select-context-choise [keymap & {:keys [on-prev on-next on-enter on-esc]}]
  (-> keymap
    (define-key (kbd "C-p")
      (fn [] (let [^JTextComponent component (active-component)]
              (on-prev (.getText component)))))
    (define-key (kbd "C-n")
      (fn [] (let [^JTextComponent component (active-component)]
              (on-next (.getText component)))))
    (define-key (kbd "C-m")
      (fn [] (let [^JTextComponent component (active-component)]
              (on-enter (.getText component)))))
    (define-key [(KeyStroke/getKeyStroke KeyEvent/VK_ENTER 0)]
      (fn [] (let [^JTextComponent component (active-component)]
              (on-enter (.getText component)))))
    (define-key (kbd "escape")
      (fn [] (on-esc)
        (.consume (active-event))))
    (define-key (kbd "C-g")
      (fn [] (on-esc)
        (.consume (active-event))))))

(defn dispatch-select [on-select state txt]
  (on-select (nth (:list @state) (:choise @state) nil)))

(defn dispatch-move-next [state txt]
  (swap! state
    (fn [s] (if (< (:choise s) (count (:whole-list s)))
             (update s :choise inc) s))))

(defn dispatch-move-prev [state txt]
  (swap! state
    (fn [s] (if (< 0 (:choise s))
             (update s :choise dec) s))))

(defn wrapp-completion-popup-controller
  [^javax.swing.text.JTextComponent component
   ^jarman.gui.core.SwingAtom state
   ^javax.swing.JWindow popup-window
   ^clojure.lang.IFn on-select]
  (doto component
    (swing/wrapp-carret-listener
      :caret-update-fn
      (fn []
        (let [value (c/value (active-component))]
          (swap! state assoc
            :pattern value
            :choise 0
            :list (->> (:whole-list @state)
                    (jarman.lib.fuzzy-search/dice value)
                    (mapv :model))))))
    (swing/wrapp-focus-listener
      :focus-lost-fn
      (fn [] (doto popup-window
              (.dispose) (.setVisible false)))
      :focus-gained-fn
      (fn [] (doto popup-window
              (.pack) (.setVisible false))))
    (wrapp-keymap
      (-> (global-keymap)
        (patch-emacs-keymap)
        (patch-select-context-choise
          :on-next  (partial dispatch-move-next state)
          :on-prev  (partial dispatch-move-prev state)
          :on-enter (partial dispatch-select on-select state)
          :on-esc   (fn [] (.setVisible popup-window false))))
      jarman.gui.components.swing-actions/default-editor-kit-action-map
      :key-typed-fn
      (fn []
        (let [^javax.swing.text.JTextComponent component (active-component)
              ^javax.swing.text.Caret carret (.getCaret component)
              ^java.awt.Point p (.getMagicCaretPosition carret)
              pn (when p (new java.awt.Point p))]
          (do ;; (println component)
            ;; (println "Carret:")
            ;; (println carret)
            ;; (println "Point1/2:")
            ;; (println p)
            (when p
              (.setVisible popup-window true)
              (javax.swing.SwingUtilities/convertPointToScreen pn component)
              (set! (. pn -y) (+ 24 (.y pn)))
              (.setLocation popup-window pn))
            ;; (println pn)
            ))))))

(defn wrapp-completion-panel-controller
  [^javax.swing.text.JTextComponent component
   ^jarman.gui.core.SwingAtom state
   ^clojure.lang.IFn on-select]
  (doto component
    (swing/wrapp-carret-listener
      :caret-update-fn
      (fn []
        (let [value (c/value (active-component))]
          (swap! state assoc
            :pattern value
            :choise 0
            :list (->> (:whole-list @state)
                    (jarman.lib.fuzzy-search/dice value)
                    (mapv :model))))))
    (wrapp-keymap
      (-> (global-keymap)
        (patch-emacs-keymap)
        (patch-select-context-choise
          :on-next  (partial dispatch-move-next state)
          :on-prev  (partial dispatch-move-prev state)
          :on-enter (partial dispatch-select on-select state)))
      jarman.gui.components.swing-actions/default-editor-kit-action-map)))

(defn candidate-panel [^jarman.gui.core.SwingAtom state completion-value-list completion-list-size]
  (seesaw.core/scrollable
    (gui-panels/vertical-panel
      :items
      (conj
        (map #(candidate-label % false []) (take (dec completion-list-size) (drop 1 completion-value-list)))
        (candidate-label (first completion-value-list) true []))
      :event-hook-atom state
      :event-hook
      (fn [panel _ _ a]
        (async/thread 
          (c/config!
            panel :items
            (let [letters (into #{} (:pattern a))
                  results
                  (let [zone completion-list-size
                        choose (:choise a)]
                    (if (>= choose zone)
                      (take-last zone (take (inc choose) (:list a)))
                      (take zone (:list a))))]
              (for [word results]
                (->> (map-indexed vector word)
                  (reduce
                    (fn [acc [index letter]]
                      (if (letters letter) (conj acc index) acc)) [])
                  (candidate-label word (= (nth (:list a) (:choise a)) word)))))))))
    :hscroll :never 
    :vscroll :never))

(defn wrapp-autocompletition-popup
  [^javax.swing.text.JTextComponent component
   & {:keys [completion-value-list completion-list-size on-select]
      :or {on-select (fn [^String candidate-value] (c/text! component candidate-value))
           completion-list-size 10
           completion-value-list []}}]
  (let [state (satom {:pattern "", :choise 0, :list completion-value-list :whole-list completion-value-list})
        popup (swing/popup-frame "Autocomplete"
                (candidate-panel state completion-value-list completion-list-size))]
    (wrapp-completion-popup-controller component state popup on-select)))

(defn listbox-panel
  [& {:keys [completion-value-list completion-list-size on-select]
      :or {on-select (fn [^String candidate-value])
           completion-list-size 10
           completion-value-list []}}]
  (let [^javax.swing.text.JTextComponent component (text :value "some")
        state (satom {:pattern "", :choise 0, :list completion-value-list :whole-list completion-value-list})]
    (seesaw.mig/mig-panel
      :background  face/c-compos-background-darker
      :constraints ["wrap 1" "0px[grow, fill]0px" "0px[fill, top]0px"]
      :border (b/empty-border :thickness 0)
      :items [[(jarman.gui.components.common/label-h4 :value "Yeah boi! take look on those shit!")]
              [(wrapp-completion-panel-controller component state on-select)]
              [(candidate-panel state completion-value-list completion-list-size)]])))

(comment
  ;; ---------------------------------- ;;
  (swing/quick-frame
    [(jarman.gui.components.common/label-h4 :value "Yeah boi! take look on those shit!")
     (wrapp-autocompletition-popup
       (text :value "" :on-change (fn [e] (println (c/value (c/to-widget e)))))
       :completion-value-list (map str (.listFiles (clojure.java.io/file jarman.config.environment/user-home)))
       :completion-list-size 10)
     (text :value "another text box")])
  ;; ---------------------------------- ;;
  (swing/quick-frame
    [(listbox-panel
       :completion-value-list (map str (.listFiles (clojure.java.io/file jarman.config.environment/user-home)))
       :completion-list-size 10)]))

;; (def frm
;;   (let [frame (javax.swing.JFrame. "Calendar")]
;;     (doto frame
;;       (.setDefaultCloseOperation javax.swing.JFrame/DISPOSE_ON_CLOSE)
;;       (.hide))))

;; (.hide frm)

;; (let [popup (javax.swing.JWindow. frm)
;;       _ (-> (doto popup
;;               (.setFocusableWindowState false)
;;               (.setType java.awt.Window$Type/POPUP)
;;               ;; (.setType java.awt.Window$Type/NORMAL)
;;               (.setLocationByPlatform true)
;;               (.setContentPane (gui-panels/border-panel
;;                                  :center (text :value "dialog box")))
;;               ;; (.setLocation x y)
;;               )
;;           (seesaw.core/pack!)
;;           (seesaw.core/show!))]
;;   ;; [popup (jarman.gui.components.swing/quick-frame
;;   ;;          [(jarman.gui.components.common/label :value "some text")
;;   ;;           (jarman.gui.components.common/label :value "some text")])
;;   ;;  ]
;;   (.setVisible popup false)
;;   (jarman.gui.components.swing/quick-frame
;;     [(doto (text :value "some text")
;;        (.addKeyListener
;;          (proxy [java.awt.event.KeyAdapter] []
;;            ;; ---------------
;;            (^void keyTyped [^java.awt.event.KeyEvent e]
;;             (let [^javax.swing.text.JTextComponent component (.getSource e)
;;                   ^javax.swing.text.Caret carret (.getCaret component)
;;                   ^java.awt.Point p (.getMagicCaretPosition carret)]
;;               (do (println component)
;;                   (println carret)
;;                   (when p
;;                     (.setVisible popup true)
;;                     (javax.swing.SwingUtilities/convertPointToScreen p component)
;;                     ;; (set! (. p -x) (+ 10 (.x p)))
;;                     (set! (. p -y) (+ 24 (.y p)))
;;                     (.setLocation popup p))
;;                   (println p))
;;               ;; (javax.swing.SwingUtilities/convertPointToScreen p popup)
;;               ;; (x, y)
;;               ))))
;;        (.addFocusListener
;;          (proxy [java.awt.event.FocusListener] []
;;            (^void focusLost [^java.awt.event.FocusEvent e]
;;             (.hide frm))
;;            (^void focusGained [^java.awt.event.FocusEvent e]
;;             nil))))
;;      (text :value "Another text")]))

#_(let ;; [frame (jarman.gui.components.swing/quick-frame
    ;;          (javax.swing.JFrame. "Calendar"))
    ;;  _ (doto frame
    ;;     (.setDefaultCloseOperation javax.swing.JFrame/DISPOSE_ON_CLOSE))
    ;;  popup (javax.swing.JWindow. frame)
    ;;  _ (-> (doto popup
    ;;          (.setFocusableWindowState false)
    ;;          ;; (.setType java.awt.Window$Type/POPUP)
    ;;          (.setType java.awt.Window$Type/NORMAL)
    
    ;;          (.setLocationByPlatform true)
    ;;          (.setContentPane (gui-panels/border-panel
    ;;                             :center (text :value "dialog box")))
    ;;          ;; (.setLocation x y)
    ;;          )
    ;;      (seesaw.core/pack!)
    ;;      (seesaw.core/show!))]
    [popup (jarman.gui.components.swing/quick-frame
             [(jarman.gui.components.common/label :value "some text")
              (jarman.gui.components.common/label :value "some text")])
     ]
  (.setVisible popup false)
  (jarman.gui.components.swing/quick-frame
    [(doto (text :value "some text")
       (.addKeyListener
         (proxy [java.awt.event.KeyAdapter] []
           ;; ---------------
           (^void keyTyped [^java.awt.event.KeyEvent e]
            (let [^javax.swing.text.JTextComponent component (.getSource e)
                  ^javax.swing.text.Caret carret (.getCaret component)
                  ^java.awt.Point p (.getMagicCaretPosition carret)
                  ]
              (do (println component)
                  (println carret)
                  (when p
                    (.setVisible popup true)
                    (javax.swing.SwingUtilities/convertPointToScreen p component)
                    ;; (set! (. p -x) (+ 10 (.x p)))
                    (set! (. p -y) (+ 24 (.y p)))
                    (.setLocation popup p))
                  (println p))
              ;; (javax.swing.SwingUtilities/convertPointToScreen p popup)
              ;; (x, y)
              )))))]))

;; (fn [e]
;;   (let [component (c/to-widget e)
;;         show (Point. 0 (.getHeight component))
;;         _ (SwingUtilities/convertPointToScreen show component)
;;         ^java.awt.Dimension size (.getScreenSize (Toolkit/getDefaultToolkit))
;;         x (.-x show)
;;         y (.-y show)
;;         x (if (< x 0) 0 x)
;;         x (if (> x (- (.-width size)  212)) (- (.-width size) 212) x)
;;         y (if (> y (- (.-height size) 165)) (- y 165) y)]
;;     (calendar-popup :value (c/value component)
;;       :value-setter value-setter
;;       :on-click (fn [e] (on-click e) (c/config! component :text e))
;;       :x x :y y)))

;; All components
(comment
  (-> (seesaw.core/frame
       :title "Jarman"
       :content
       (select-panel
        :string-list (map str (.listFiles (clojure.java.io/file jarman.config.environment/user-home)))
        :on-select (fn [t] (println "CHOSE=> " t))))
      (swing/choose-screen! 1)
      (seesaw.core/pack!)
      (seesaw.core/show!))
  )

