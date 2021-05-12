(ns jarman.gui.gui-tutorials.key-dispacher-tutorial
  (:use seesaw.core
        seesaw.border
        seesaw.dev
        seesaw.mig)
  (:import
   (javax.swing 
    InputMap
    JScrollPane
    JPanel
    JComponent
    KeyStroke
    Action
    AbstractAction)
   (java.awt.event ActionEvent
                         ActionListener
                         InputEvent
                         KeyEvent
                         ActionListener))
 ;; (:require [jarman.gui.gui-login :as lgn])
  )


;; Debug for own KeyEventDispacher
(def debug-me
  (fn [events-map e debug-on action ]
    (if debug-on (do
                   (println "-------------------")
                   (println "Map " events-map)
                   (println "ID " (.getID e))
                   (println "Key " (.getKeyCode e))
                   (println "Keyword " (keyword (str (.getKeyCode e))))
                   (println "Event " (get-in events-map [(keyword (str (.getKeyCode e)))]))))
    (action events-map e)
    (println)))


;; Own proxy, class for KeyEventDispacher
(def KeyDispacher
  "Description:
      New KeyEventDispacher need map with key number as keword and some function.
      Function will be invoke on key press and repeate on hold
   Example:
      new KeyDispacher {:alt (funkcja => \"You press ALT\")}
      (KeyDispacher {:18 (fn [] (println \"You press ALT\"))}
   "
  (fn [events-map]
    (let []
      (doto (proxy [java.awt.KeyEventDispatcher] []
              (^Boolean dispatchKeyEvent [^java.awt.event.KeyEvent e]
                (cond
                  (= (.getID e) 401) ;; 401 is an pressed key
                  (debug-me events-map e false
                   (fn [events-map e] ;; Event on pressed
                     (let [ev (get-in events-map [(keyword (str (.getKeyCode e)))])]
                       (if-not (nil? ev) (ev))))))
                false))))))

;; Own proxy, class for KeyEventDispacher
(def SupervisiorKeyDispacher
  "Description:
      New KeyEventDispacher need map with key number as keword and some function.
      Function will be invoke on key press and repeate on hold
   Example:
      new KeyDispacher {:alt (funkcja => \"You press ALT\")}
      (KeyDispacher {:18 (fn [] (println \"You press ALT\"))}
   "
  (fn []
    (let []
      (doto (proxy [java.awt.KeyEventDispatcher] []
              (^Boolean dispatchKeyEvent [^java.awt.event.KeyEvent e]
                (println "Key pressed: " (str (.getKeyCode e)))
                false))))))

;; (def store-key (atom [])
;; )
;; ;; Own proxy, class for KeyEventDispacher
;; (def SequanceKeyDispacher
;;   "Description:
;;       New KeyEventDispacher need map with key number as keword and some function.
;;       Function will be invoke on key press and repeate on hold
;;    Example:
;;       new KeyDispacher {:alt (funkcja => \"You press ALT\")}
;;       (KeyDispacher {:18 (fn [] (println \"You press ALT\"))}
;;    "
;;   (fn [long]
;;     (let []
;;       (doto (proxy [java.awt.KeyEventDispatcher] []
;;               (^Boolean dispatchKeyEvent [^java.awt.event.KeyEvent e]
;;                 ;; (println "Key pressed: " (str (.getKeyCode e)))
;;                 (println "Long " long)
;;                                          (let [key (keyword (str (.getKeyCode e)))]
;;                                            (if-not (= key :0) 
;;                                              (if (< (count store-key) long)
;;                                                (do
;;                                                  (swap! store-key (fn [old-store] (conj old-store key))))
;;                                                (do
;;                                                  (let [action-key (clojure.string/join "-" (conj @store-key key))]
;;                                                    (cond
;;                                                      (= action-key :18-:82) (println "Reload APP")))))))
;;                                          false))))))



;; ;; New jframe
;; (do (doto (seesaw.core/frame
;;            :title "title"
;;            :undecorated? false
;;            :minimum-size [600 :by 400]
;;            :content (mig-panel
;;                      :bounds [0 0 500 300]
;;                      :constraints ["wrap 1" "[grow, center]" "[grow, center]"]
;;                      :border (empty-border :thickness 10)
;;                      :items [[(label :text "Tabelki" :focusable? true :listen [:focus-gained (fn [e] (config! e :background "#9a9"))
;;                                                                                :focus-lost (fn [e] (config! e :background "#aaa"))])]
;;                              [(button :text "1")]
;;                              [(button :text "2")]
;;                              [(button :text "3" ;;:listen [:key-pressed (fn [e] (println (.getKeyChar e)))]
;;                                       )]]))
;;       (.setLocationRelativeTo nil) seesaw.core/pack! seesaw.core/show!))


;; ;; Create new own KeyEventDispacher object using own class. If one was added before then first delete him.
;; ;(def ked (SupervisiorKeyDispacher 2))
;; (def ked (KeyDispacher {:18 (fn [] (println "You press ALT"))}))


;; ;; Add own dispacher as next dispacher object. Can be added few and more KeyEventDispachers.
;; (doto (java.awt.KeyboardFocusManager/getCurrentKeyboardFocusManager)
;;   (.addKeyEventDispatcher ked))


;; ;; Remove added dispacher. Need object of added dispacher.
;; (doto (java.awt.KeyboardFocusManager/getCurrentKeyboardFocusManager)
;;   (.removeKeyEventDispatcher ked))


;; (def panel
;;      (doto
;;          (proxy [JComponent] []
;;            (paint [g] nil))
;;      ;;  (.setPreferredSize (new Dimension 800 800))
;;        (.. (getInputMap) (put (KeyStroke/getKeyStroke (KeyEvent/VK_F)(InputEvent/CTRL_DOWN_MASK)) "action"))
;;        (.. (getInputMap) (put (KeyStroke/getKeyStroke "F2") "action"))
;;        (.. (getInputMap) (put (KeyStroke/getKeyStroke \a) "action"))
;;        (.. (getActionMap) (put "action" myAction))))

;;(.dispose (to-frame e))


(defn get-key-panel [keystr action some-panel]
  (doto some-panel        
    (.. (getInputMap) (put (KeyStroke/getKeyStroke keystr) "action"))
    (.. (getActionMap) (put "action" (proxy [AbstractAction ActionListener] []
                                       (actionPerformed [e] (do (println "act") (action e))))))))






;; (do
;;   ;;(.add panel (mig-panel))
;;   (let [panel (get-key-panel \q (fn [jpan] (.dispose (to-frame jpan))) (mig-panel))]
;;     (println (type panel))
;;     (.add panel (seesaw.mig/mig-panel
;;                                      :constraints ["wrap 1" "0px[grow, fill]0px" "5px[grow, fill]0px"]
;;                                      :items [[(label :text "Hey")]]))
;;     (doto
;;         (seesaw.core/frame
;;          :resizable? true)
;;       (.setPreferredSize (new java.awt.Dimension 800 800))
;;       (.add panel)
;;       (.pack)
;;       (.setVisible true))))

