(ns jarman.gui.components.panels
  (:require
   ;; Jarman
   [jarman.tools.lang :refer :all]
   ;; Seesaw
   [seesaw.mig]
   [seesaw.core]
   ;; GUI
   [jarman.gui.core :refer [satom cursor register! fe]]))

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
;; Environ
;;
;;    (def event (fe [value1 value2] 10))
;;    (def state (satom {}))

;; V0.1 declaration
;;
;;    (vertical-panel
;;     :items []
;;     :event-hook-id :event-hook-id-for-atom
;;     :event-hook-atom st
;;     :event-hook
;;     (fn [panel a old state]
;;       ... some actions ...))

;; V0.2 declaration
;;
;;    (vertical-panel
;;     :items []
;;     :event-hook
;;     {:event-hook-id-for-atom
;;      {:atom state
;;       :hook (fn [panel atom old new]
;;               ... some actions ...)}
;;      :event-hook-id-for-event
;;      {:event event
;;       :hook (fn [panel value1 value2]
;;               ... some actions ...)}})


;; for the
;; - jarman.gui.core.SwingAtom,
;; - jarman.gui.core.Cursor
(defn- process-event-hook-fn [panel {:keys [event-hook-atom event-hook event-hook-id] :as arguments}]
  (let [event-hook-id (if-not event-hook-id (keyword (str "panel" (.hashCode panel))) event-hook-id)]
    (register! event-hook-atom event-hook-id (partial event-hook panel))
    panel))

;; for the
;; - jarman.gui.core.Event
(defn- process-event-hook-event [panel {:keys [event-hook-event event-hook-id event-hook] :as arguments}]
  (let [event-hook-id (if-not event-hook-id (keyword (str "panel" (.hashCode panel))) event-hook-id)]
    (register! event-hook-event event-hook-id (partial event-hook panel))
    panel))

(defn- process-event-hook-map [panel {:keys [event-hook] :as arguments}]
  (->> event-hook
       (map (fn [[id config]]
              (when (contains? config :atom)
                (process-event-hook-fn
                 panel {:event-hook-id    id
                        :event-hook-atom  (get config :atom nil)
                        :event-hook       (get config :hook nil)}))
              (when (contains? config :event)
                (process-event-hook-event
                 panel {:event-hook-id    id
                        :event-hook-event (get config :event nil)
                        :event-hook       (get config :hook nil)}))))
       (doall)) panel)

(defn- process-event-hook [panel {:keys [event-hook-atom event-hook event-hook-id] :as arguments}]
  (cond
    (fn? event-hook) (process-event-hook-fn panel arguments)
    (map? event-hook) (process-event-hook-map panel arguments)
    (nil? event-hook) panel
    :else (throw (ex-info "Undefinied type of :event-hook" arguments))))

(defn event-panel-wrapper [component]
  (fn [& {:keys [event-hook-atom event-hook event-hook-id] :as arguments}]
    (let [clean-arg (dissoc arguments :event-hook :event-hook-atom :event-hook-id)
          panel (apply component (interleave (keys clean-arg) (vals clean-arg)))]
      (process-event-hook panel arguments)
      panel)))

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

;;  ____  _____ __  __  ___
;; |  _ \| ____|  \/  |/ _ \
;; | | | |  _| | |\/| | | | |
;; | |_| | |___| |  | | |_| |
;; |____/|_____|_|  |_|\___/

(comment

  (require 'jarman.gui.components.swing)

  ;;;;;;;;;;;;;;;;;;;;;
  ;; EVENT HOOK v0.1 ;;
  ;;;;;;;;;;;;;;;;;;;;;

  ;; example 1

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

  (swap! st assoc :value nil)
  (swap! st assoc :value 1)
  (swap! st assoc :value 2)
  (swap! st assoc :value 3)
  (swap! st assoc :value 10)

  ;; example 2

  (doto (seesaw.core/frame
         :title "Jarman" :size [1000 :by 800]
         :content
         (mig-panel
          :items [[(f-label "Border panel Event hook, click on '<' or '>': ")]
                  [(let [border-state (satom {:value 0})
                         center-label (f-label "nothing")]
                     (border-panel
                      :west (click-label " < " (fn [e]
                                               (println "INC!")
                                               (swap! border-state update :value dec)))
                      :east (click-label " > " (fn [e]
                                               (println "DEC!")
                                               (swap! border-state update :value inc)))
                      :center center-label
                      :event-hook-atom border-state
                      :event-hook
                      (fn [panel a old-st new-st]
                        (seesaw.core/config! center-label :text (str (:value new-st))))))]
                  [(f-label "changes")]]))
    (.setLocationRelativeTo nil) seesaw.core/pack! seesaw.core/show!)

  ;; EVENT HOOK v0.2
  ;; with supporting map declaration and subscription on the event

  ;; satom with two cursor's
  (def state (satom {:a-path {:prop 1}
                     :b-path {:prop -1}}))
  (def a-path-cursor (cursor [:a-path :prop] state))
  (def b-path-cursor (cursor [:b-path :prop] state))

  ;; idependent event
  (def event (fe [value] (* value 100)))

  (jarman.gui.components.swing/quick-frame
   (let [a-path-lbl (f-label (-> (deref state) :a-path :prop str))
         b-path-lbl (f-label (-> (deref state) :b-path :prop str))
         event-lbl  (f-label "0")]
     [(vertical-panel
       :items
       [(f-label "'A-Path' cursor")
        a-path-lbl
        (f-label "'B-Path' cursor")
        b-path-lbl
        (f-label "'Event' hook")
        event-lbl]
       :event-hook
       {:a-path-event
        {:atom a-path-cursor
         :hook (fn [panel atom old new]
                 (println "A-Path change!")
                 (seesaw.core/config! a-path-lbl :text (str new)))}
        :b-path-event
        {:atom b-path-cursor
         :hook (fn [panel atom old new]
                 (println "B-Path change!")
                 (seesaw.core/config! b-path-lbl :text (str new)))}
        :idependent-event
        {:event event
         :hook (fn [panel value]
                 (println "Idependant event change!")
                 (seesaw.core/config! event-lbl :text (str value)))}})]))

  (swap! a-path-cursor inc)
  (swap! b-path-cursor dec)
  (event 40))
