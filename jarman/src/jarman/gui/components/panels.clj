(ns jarman.gui.components.panels
  (:require
   ;; Jarman
   [jarman.tools.lang :refer :all]
   ;; Seesaw
   [seesaw.mig]
   [seesaw.core]
   ;; GUI
   [jarman.gui.core :refer [satom register!]]))

;;  ____ _____  _  _____ _____          
;; / ___|_   _|/ \|_   _| ____|         
;; \___ \ | | / _ \ | | |  _|           
;;  ___) || |/ ___ \| | | |___          
;; |____/ |_/_/   \_\_| |_____|         
;;  ____   _    _   _ _____ _     ____  
;; |  _ \ / \  | \ | | ____| |   / ___| 
;; | |_) / _ \ |  \| |  _| | |   \___ \ 
;; |  __/ ___ \| |\  | |___| |___ ___) |
;; |_| /_/   \_\_| \_|_____|_____|____/ 
;; 

(defn- event-wrapper [component-type component]
  (fn [& {:keys [event-hook-atom event-hook] :as arguments}]
    (let [clean-arg (dissoc arguments :event-hook :event-hook-atom)]
      (let [component (apply component (interleave (keys clean-arg) (vals clean-arg)))
            component-hash (keyword (str (name component-type) (.hashCode component)))]
        (if event-hook (register! event-hook-atom component-hash (partial event-hook component)))
        component))))

(defn- event-panel-wrapper [component]
  (fn [& {:keys [event-hook-atom event-hook] :as arguments}]
    (let [clean-arg (dissoc arguments :event-hook :event-hook-atom)]
      (let [panel (apply component (interleave (keys clean-arg) (vals clean-arg)))
            panel-hash (keyword (str "panel" (.hashCode panel)))]
        (if event-hook (register! event-hook-atom panel-hash (partial event-hook panel)))
        panel))))

(defmacro ^:private wrapp-components [wrapper & components-list]
  `(do ~@(for [component components-list
               :let [panel-symbol   (symbol (name component))]]
           `(do
              (def ~panel-symbol (~wrapper ~component))
              (alter-meta! (var ~panel-symbol)
                           assoc
                           :doc (:doc (meta (var ~component)))
                           :arglists (:arglists (meta (var ~component))))))
       true))

(wrapp-components event-panel-wrapper
 seesaw.mig/mig-panel
 seesaw.core/vertical-panel
 seesaw.core/horizontal-panel
 seesaw.core/grid-panel
 seesaw.core/border-panel
 seesaw.core/box-panel)

(comment
  (def st (satom {}))
  (defn f-label [text] (seesaw.core/label :text text :font {:size 24, :style :plain, :name "Ubuntu Regular"}))
  (defn click-label [text on-click] (seesaw.core/label :text text :font {:size 24, :style :plain, :name "Ubuntu Regular"}
                                              :listen [:mouse-clicked on-click]))
  (doto (seesaw.core/frame
         :title "Jarman" :size [1000 :by 800]
         :content
         (mig-panel
          :items [[(f-label "Any")] [(f-label "changes")]]
          :event-hook-atom st
          :event-hook
          (fn [panel a old state]
            (cond
              (in? [nil 1]     (:value state)) (seesaw.core/config! panel :items [[(f-label "nil-1")]])
              (in? [2 3 4 5 6] (:value state)) (seesaw.core/config! panel :items [[(f-label (str (:value state)))]])
              :else                            (seesaw.core/config! panel :items [[(f-label "nothing")]])))))
    (.setLocationRelativeTo nil) seesaw.core/pack! seesaw.core/show!)

  
  (seesaw.core)
  (swap! st assoc :value nil)
  (swap! st assoc :value 1)
  (swap! st assoc :value 2)
  (swap! st assoc :value 3)
  (swap! st assoc :value 10)

  (doto (seesaw.core/frame
         :title "Jarman" :size [1000 :by 800]
         :content
         (mig-panel
          :items [[(f-label "Border panel Event hook ")]
                  [(let [border-state (satom {:value 0})
                         center-label (f-label "nothing")]
                     (border-panel
                      :west (click-label "<" (fn [e]
                                               (println "INC!")
                                               (swap! border-state update :value dec)))
                      :east (click-label ">" (fn [e]
                                               (println "DEC!")
                                               (swap! border-state update :value inc)))
                      :center center-label
                      :event-hook-atom border-state
                      :event-hook
                      (fn [panel a old-st new-st]
                        (seesaw.core/config! center-label :text (str (:value new-st))))))]
                  [(f-label "changes")]]))
    (.setLocationRelativeTo nil) seesaw.core/pack! seesaw.core/show!))




