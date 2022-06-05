(ns jarman.gui.components.listbox
  (:require
   [clojure.string :as string]
   [clojure.pprint :refer [cl-format]]
   [clojure.core.async :as async]
   ;; Seesaw
   [seesaw.core    :as c]
   [seesaw.border  :as b]
   ;; Jarman
   [jarman.lang                  :refer :all]
   [jarman.tools.fuzzy-search]
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

(defn- search-fields [^jarman.gui.core.Cursor state words]
  (text :value "" :on-change (fn [e] (let [t (c/value (c/to-widget e))]
                                     (swap! state assoc
                                            :pattern t
                                            :choise 0
                                            :list (->> words
                                                       (jarman.tools.fuzzy-search/dice t)
                                                       (sort-by :value)
                                                       (reverse)
                                                       (mapv :model)))))))

(defn patch-select-context-choise [keymap & {:keys [on-prev on-next on-enter]}]
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
                (on-enter (.getText component)))))))

(defn select-panel [&{:keys [string-list list-size on-select]
                      :or {string-list []
                           list-size 10
                           on-select (fn [text])}}]
  (let [state (satom {:pattern "", :choise 0, :list string-list})]
    (wlet
     (-> ;; action-wrapper
      (seesaw.mig/mig-panel
       :background  face/c-compos-background-darker
       :constraints ["wrap 1" "0px[grow, fill]0px" "0px[fill, top]0px"]
       :border (b/empty-border :thickness 0)
       :items (concat
               [[(-> (text :value "" :on-change
                           (fn [e] (swap! state assoc
                                         :pattern (c/value (c/to-widget e))
                                         :choise 0
                                         :list (->> string-list
                                                    (jarman.tools.fuzzy-search/dice (c/value (c/to-widget e)))
                                                    (sort-by :value)
                                                    (reverse)
                                                    (mapv :model)))))
                     (wrapp-keymap
                      (-> (global-keymap)
                          (patch-emacs-keymap)
                          (patch-select-context-choise
                           :on-next  dispatch-move-next
                           :on-prev  dispatch-move-prev
                           :on-enter dispatch-select))
                      jarman.gui.components.swing-actions/default-editor-kit-action-map))]]
               [[(seesaw.core/scrollable
                  (gui-panels/vertical-panel
                   :items
                   (conj
                    (map #(candidate-label % false []) (take (dec list-size) (drop 1 string-list)))
                    (candidate-label (first string-list) true []))
                   :event-hook-atom state
                   :event-hook
                   (fn [panel _ _ a]
                     (async/thread 
                       (c/config!
                        panel :items
                        (let [letters (into #{} (:pattern a))
                              results
                              (let [zone list-size
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
                  :vscroll :never)]])))
     ((dispatch-select
       (fn [txt]
         (on-select
          (nth (:list @state)
               (:choise @state)
               nil))))
      (dispatch-move-next
       (fn [txt]
         (swap! state
                (fn [s]
                  (if (< (:choise s) (count string-list)) (update s :choise inc) state)))))
      (dispatch-move-prev
       (fn [txt]
         (swap! state
                (fn [s]
                  (if (< 0 (:choise s)) (update s :choise dec) s)))))))))

;; All components
(comment
  (-> (seesaw.core/frame
       :title "Jarman"
       :content
       (select-panel
        :string-list (map str (.listFiles (clojure.java.io/file jarman.config.environment/user-home)))
        :on-select (fn [t] (println "CHOSE=> " t))))
      (swing/choose-screen! 0)
      (seesaw.core/pack!)
      (seesaw.core/show!))
  )

