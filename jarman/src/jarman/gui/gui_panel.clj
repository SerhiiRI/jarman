(ns jarman.gui.gui-panel
  (:require
   ;; Seesaw
   [seesaw.mig]
   [seesaw.core]
   ;; GUI
   [jarman.gui.core :refer [satom register!]]))

(defn event-wrapper [component-type component]
  (fn [& {:keys [event-hook-atom event-hook] :as arguments}]
    (let [clean-arg (dissoc arguments :event-hook :event-hook-atom)]
      (let [component (apply component (interleave (keys clean-arg) (vals clean-arg)))
            component-hash (keyword (str (name component-type) (.hashCode component)))]
        (if event-hook (register! event-hook-atom component-hash (partial event-hook component)))
        component))))

(defn event-panel-wrapper [component]
  (fn [& {:keys [event-hook-atom event-hook] :as arguments}]
    (let [clean-arg (dissoc arguments :event-hook :event-hook-atom)]
      (let [panel (apply component (interleave (keys clean-arg) (vals clean-arg)))
            panel-hash (keyword (str "panel" (.hashCode panel)))]
        (if event-hook (register! event-hook-atom panel-hash (partial event-hook panel)))
        panel))))

(defmacro wrapp-components [wrapper & components-list]
  `(do ~@(for [component components-list
               :let [panel-symbol   (symbol (name component))]]
           `(do
              (def ~panel-symbol (~wrapper ~component))
              (alter-meta! (var ~panel-symbol)
                           assoc
                           :doc (:doc (meta (var ~component))))))
       true))

(wrapp-components event-panel-wrapper
 seesaw.mig/mig-panel
 seesaw.core/vertical-panel
 seesaw.core/horizontal-panel
 seesaw.core/grid-panel
 seesaw.core/border-panel
 seesaw.core/box-panel)


(do
  (def mig-panel (event-panel-wrapper seesaw.mig/mig-panel))
  (alter-meta!
   (resolve mig-panel)
   (fn [m]
     (merge
      m
      {:doc (:doc (meta (resolve 'seesaw.mig/mig-panel)))}))))


(comment
  (def st (satom {}))
  (defn f-label [text] (seesaw.core/label :text text :font {:size 24, :style :plain, :name "Ubuntu Regular"}))
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

  (swap! st assoc :value nil)
  (swap! st assoc :value 1)
  (swap! st assoc :value 2)
  (swap! st assoc :value 3)
  (swap! st assoc :value 10))

