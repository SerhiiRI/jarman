(ns jarman.gui.gui-tutorials.key-dispacher-tutorial
  (:use seesaw.core
        seesaw.border
        seesaw.dev
        seesaw.mig))

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


;; New jframe
(do (doto (seesaw.core/frame
           :title "title"
           :undecorated? false
           :minimum-size [600 :by 400]
           :content (mig-panel
                     :bounds [0 0 500 300]
                     :constraints ["wrap 1" "[grow, center]" "[grow, center]"]
                     :border (empty-border :thickness 10)
                     :items [[(label :text "Tabelki" :focusable? true :listen [:focus-gained (fn [e] (config! e :background "#9a9"))
                                                                               :focus-lost (fn [e] (config! e :background "#aaa"))])]
                             [(button :text "1")]
                             [(button :text "2")]
                             [(button :text "3" ;;:listen [:key-pressed (fn [e] (println (.getKeyChar e)))]
                                      )]]))
      (.setLocationRelativeTo nil) seesaw.core/pack! seesaw.core/show!))


;; Create new own KeyEventDispacher object using own class. If one was added before then first delete him.
(def ked (KeyDispacher {:18 (fn [] (println "You press ALT"))}))


;; Add own dispacher as next dispacher object. Can be added few and more KeyEventDispachers.
(doto (java.awt.KeyboardFocusManager/getCurrentKeyboardFocusManager)
  (.addKeyEventDispatcher ked))


;; Remove added dispacher. Need object of added dispacher.
(doto (java.awt.KeyboardFocusManager/getCurrentKeyboardFocusManager)
  (.removeKeyEventDispatcher ked))

;; Trzeba by nadpisać getKeyEventDispatchers bo jest protected i nie pozwala pobrać aktualnej listy KeyEventDispatcherów
;; (let [ckfm (java.awt.KeyboardFocusManager/getCurrentKeyboardFocusManager)
;;       ked (java.awt.KeyboardFocusManager/getKeyEventDispatchers)]
;;   (.removeKeyEventDispatcher ckfm ked))

