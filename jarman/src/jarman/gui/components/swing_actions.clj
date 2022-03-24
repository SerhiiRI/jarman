(ns jarman.gui.components.swing-actions
  (:require
   [jarman.gui.components.swing-context
    :refer [active-keymap active-component active-default active-frame]])
  (:import
   [javax.swing.text DefaultEditorKit JTextComponent]
   [javax.swing Action PopupFactory]
   [org.fife.ui.rtextarea
    RTextAreaEditorKit
    RTextAreaEditorKit$IncreaseFontSizeAction
    RTextAreaEditorKit$DecreaseFontSizeAction]))

(def default-editor-kit-action-map
  (let [component (seesaw.core/text)
        action-map (.getActionMap component)]
    (cond-> (hash-map)
      ;; ---
      (instance? javax.swing.JComponent component)
      (into (map #(.get action-map %) (.keys action-map)) )
      ;; --- 
      (instance? javax.swing.text.JTextComponent component)
      (into (->> (.getActions component) (map (fn [a] (vector (.getValue a Action/NAME) a))))))))

(def default-rtext-editor-kit-action-map
  (let [component (seesaw.rsyntax/text-area)
        action-map (.getActionMap component)]
    (->
     (cond-> (hash-map)
       ;; ---
       (instance? javax.swing.JComponent component)
       (into (map #(.get action-map %) (.keys action-map)) )
       ;; --- 
       (instance? javax.swing.text.JTextComponent component)
       (into (->> (.getActions component) (map (fn [a] (vector (.getValue a Action/NAME) a))))))
     (assoc RTextAreaEditorKit/rtaIncreaseFontSizeAction (RTextAreaEditorKit$IncreaseFontSizeAction.))
     (assoc RTextAreaEditorKit/rtaDecreaseFontSizeAction (RTextAreaEditorKit$DecreaseFontSizeAction.)))))

(def rtext-increase-font-size RTextAreaEditorKit/rtaIncreaseFontSizeAction)
(def rtext-decrease-font-size RTextAreaEditorKit/rtaDecreaseFontSizeAction)


(declare s-expr-forward)
(declare s-expr-backward)
(declare unselect-selected-text)
(declare show-full-text-popup)
(declare delete-to-end-of-line)

(def ^:private s-expr-r-brackets     {\[ \], \" \", \' \', \( \), \< \>})
(def ^:private s-expr-l-brackets     {\] \[, \" \", \' \', \) \(, \> \<})
(def ^:private s-expr-excepts-chars #{\space \tab \newline})

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

(defn s-expr-forward []
 (let [^JTextComponent component (active-component)]
   (let [t (.getText component)
         currentCursor (.getCaretPosition component)
         [a b] (split-at currentCursor t)]
     (let [offset (reduce (fn [acc b-char]
                            (if (s-expr-excepts-chars b-char)
                              (inc acc)
                              (reduced acc))) 0 b)] 
       (let [bracket (nth b offset)]
         (if ((set (keys s-expr-r-brackets)) (nth b offset))
           (let [i (.indexOf (drop (inc offset) b) (get s-expr-r-brackets bracket))]
             (if (> i 0)
               (.setCaretPosition component (+ currentCursor i 2 offset))))
           (println "TODO move word-forward")))
       (let [bracket (nth b offset)]
         (cond
           ;;---
           (#{\[ \( \<} bracket)
           (let [i (indexOfBracket (drop offset b) bracket (get s-expr-r-brackets bracket))]
             (if (> i 0)
               (let [position (+ currentCursor i offset 1)]
                 ;; (println (str (nth t (dec position)) (nth t position) (nth t (inc position))))
                 ;; (println " ^ ")
                 (.setCaretPosition component position))))
          
           (#{\" \'} bracket)
           (let [i (.indexOf (drop (inc offset) b) (get s-expr-r-brackets bracket))]
             (if (> i 0)
               (let [position (+ currentCursor i 2 offset)]
                 ;; (println (str (nth t (dec position)) (nth t position) (nth t (inc position))))
                 ;; (println " ^ ")
                 (.setCaretPosition component position))))
           :else
           (if-let [^Action action (get default-editor-kit-action-map DefaultEditorKit/nextWordAction)]
             ;; (.actionPerformed action event)
             (.actionPerformed action
                               (java.awt.event.ActionEvent.
                                (active-component)
                                (.intValue (System/currentTimeMillis))
                                nil)))))))))

(defn s-expr-backward []
  (let [^JTextComponent component (active-component)]
    (let [t (.getText component)
          currentCursor (.getCaretPosition component)
          [a _] (split-at currentCursor t)
          a (reverse a)]
      (let [offset (reduce (fn [acc a-char]
                             (if (s-expr-excepts-chars a-char)
                               (inc acc)
                               (reduced acc))) 0 a)]
        (let [bracket (nth a offset)]
          (cond
            ;;---
            (#{\] \) \>} bracket)
            (let [i (indexOfBracket (drop offset a) bracket (get s-expr-l-brackets bracket))]
              (if (> i 0)
                (let [position (- (count a) (+ i offset 1))]
                  ;; (println (str (nth t (dec position)) (nth t position) (nth t (inc position))))
                  ;; (println " ^ ")
                  (.setCaretPosition component position))))
            
            (#{\" \'} bracket)
            (let [i (.indexOf (drop (inc offset) a) (get s-expr-l-brackets bracket))]
              (if (> i 0)
                (let [position (- (count a) (+ i 2 offset))]
                  ;; (println (str (nth t (dec position)) (nth t position) (nth t (inc position))))
                  ;; (println " ^ ")
                  (.setCaretPosition component position))))
            :else
            (if-let [^Action action (get default-editor-kit-action-map DefaultEditorKit/previousWordAction)]
              (.actionPerformed action
                                (java.awt.event.ActionEvent.
                                 (active-component)
                                 (.intValue (System/currentTimeMillis))
                                 nil)))))))))

(defn unselect-selected-text []
  (let [^JTextComponent component (active-component)]
    (.setCaretPosition component 0)))

(defn show-full-text-popup []
  (let [^JTextComponent component (active-component)]
    ;; (print " W: " (.. component getSize getWidth) " H: " (.. component getSize getHeight))
    ;; (print " X: " (.. component getLocationOnScreen getX))
    ;; (println " Y: " (.. component getLocationOnScreen getY))
    (let [^PopupFactory popup-factory (PopupFactory/getSharedInstance)]
      (let [p (.getPopup popup-factory component
                         (seesaw.core/label :text (.getText component) :background "#d9ecff")
                         (.. component getLocationOnScreen getX)
                         (- (+ (.. component getLocationOnScreen getY) (.. component getSize getHeight)) 2))]
        (.show p)))))

(defn delete-to-end-of-line []
 (let [^JTextComponent component (active-component)
       last-curret-position (.getCaretPosition component)]
   (let [[a b] (map (partial apply str) (split-at (.getCaretPosition component) (.getText component)))]
     (.setText component (str a (if (= (first b) \newline)
                                  (clojure.string/replace-first b #"\n" "")
                                  (clojure.string/replace-first b #"[^\n]+" ""))))
     (.setCaretPosition component last-curret-position))))

;; (def rtext-increase-font-size (RTextAreaEditorKit$IncreaseFontSizeAction.))
;; (def rtext-decrease-font-size (RTextAreaEditorKit$DecreaseFontSizeAction.))
