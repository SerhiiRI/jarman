(ns jarman.gui.gui-alerts-service
  (:import (java.awt Color)
           (jiconfont.icons.google_material_design_icons GoogleMaterialDesignIcons))
  (:use seesaw.dev
        seesaw.mig)
  (:require [jarman.faces                     :as face]
            [seesaw.core                      :as c]
            [seesaw.border                    :as b]
            [jarman.tools.swing               :as stool]
            [jarman.gui.gui-style             :as gs]
            [jarman.gui.gui-tools             :as gtool]
            [jarman.gui.gui-components        :as gcomp]
            [jarman.gui.popup                 :as popup]
            [jarman.logic.state               :as state]
            [clojure.string                   :as string]
            [jarman.resource-lib.icon-library :as icon]
            [jarman.gui.gui-migrid            :as gmg]
            [jarman.tools.lang                :refer :all]))

;; ┌─────────────────┐
;; │                 │
;; │ State mechanism │
;; │                 │
;; └─────────────────┘

(def state  (atom {}))
(def state! (fn [& prop]
              (cond (= :atom (first prop)) state
                    :else (deref state))))

(defn- action-handler
  "Description:
    Invoke fn using dispatch!.
  Example:
    (@state {:action :test})"
  [state action-m]
  (case (:action action-m)
    :add-missing      (assoc-in state (:path action-m) nil)
    :test             (do (println "\nTest:n") state)
    :new-alerts-box   (assoc-in state [:alerts-box]     (:box   action-m))
    :store-new-alerts (assoc-in state [:alerts-storage (keyword (str "index-" (:alert-index state)))] (:alert action-m))
    :clear-history    (assoc-in state [:alerts-storage] {})
    :inc-index        (assoc-in state [:alert-index] (inc (:alert-index (state!))))
    :clear-index      (assoc-in state [:alert-index] 0)
    :store-in-temp    (do
                        (state/set-state
                        :temp-alerts-storage
                        (let [state (state/state :temp-alerts-storage)
                              idx   (count state)]
                          (assoc-in state [(keyword (str "temp-index-" idx))] (:alert action-m))))
                        state)
    :clear-temp       (do
                        (state/set-state :temp-alerts-storage {})
                        state)))


(defn- create-disptcher [atom-var]
  (fn [action-m]
    (swap! atom-var (fn [state] (action-handler state action-m)))))

(def dispatch! (create-disptcher state))

(defn- create-state-template
  "Description:
    "
  [] (reset! state {:alerts-storage {}
                    :alerts-box     nil
                    :box-w          300
                    :watching-path  :atom-app-size
                    :alert-index    0}))


;; ┌──────────────────────────┐
;; │                          │
;; │ Alerts service mechanism │
;; │                          │
;; └──────────────────────────┘
(defn- refresh-box []
  (.revalidate (:alerts-box (state!)))
  (.repaint (state/state :app)))

(defn- refresh-box-bounds []
  (let [offset-x 10
        offset-y 0
        watch-path (:watching-path (state!))]
    (c/config! (:alerts-box (state!))
               :bounds [(- (first @(state/state watch-path)) (:box-w (state!)) offset-x)
                        (+ 0 offset-y)
                        (:box-w (state!))
                        (second @(state/state watch-path))]))
  (c/move! (:alerts-box (state!)) :to-front)
  (refresh-box))

(defn- watch-frame-size []
  (let [watch-path (:watching-path (state!))]
    (add-watch (state/state watch-path) :refresh-alerts-box-bounds
     (fn [key atom old-state new-state]
       (refresh-box-bounds)))))

(defn- alerts-box-top-bar
  "Top bar create empty space.
   For this trick, alerts can be displaing on bottom."
  []
  ;; (c/label :text "Message Service" :background face/c-compos-background-dark)
  (c/label))

(defn- new-alerts-box []
  (let [mig (mig-panel :constraints ["wrap 1" "0px[grow, fill]0px" "5px[grow, bottom]0px[fill]5px"]
                       :opaque? false
                       :bounds [50 0 (:box-w (state!)) 300]
                       :background (Color. 0 0 0 0)
                       :items [[(alerts-box-top-bar)]])]
    mig))

(defn- rm-alerts-box
  "Description:
    Remove alerts box from JLayeredPane"
  [] (try
       (if-not (nil? (:alerts-box (state!)))
         (.remove (state/state :app) (:alerts-box (state!))))
       (catch Exception e (str "Do not found alerts box"))))


(defn- clear-alerts-box []
  (c/config!   (:alerts-box (state!)) :items [[(alerts-box-top-bar)]])
  (refresh-box))

(defn- include-alert-box
  [& {:keys [soft]
      :or {soft false}}]
  ;; Remove old alerts box
  (if-not soft
    (try
      (if-not (empty? (:alerts-box (state!))) (rm-alerts-box))
      (catch Exception e (str "Do not found alerts box"))))

  ;; New alerts box
  (if-not soft
    (dispatch! {:action :new-alerts-box
                :box    (new-alerts-box)}))
  (.add (state/state :app) (:alerts-box (state!)) (Integer. 999))
  (refresh-box))



;; ┌──────────────────────┐
;; │                      │
;; │ Start new alerts box │
;; │                      │
;; └──────────────────────┘

(defn start
  "Description:
     Clear state atom and set new state template.
  Example:
     "
  [& {:keys [soft]
      :or {soft false}}]
  (if-not soft (create-state-template))
  (include-alert-box)
  (remove-watch (state/get-atom) (:watching-path (state!)))
  (refresh-box-bounds)
  (watch-frame-size))


;; ┌──────────────────┐
;; │                  │
;; │ Alerts templates │
;; │                  │
;; └──────────────────┘

(defn add-to-alerts-box [item]
  (.add (:alerts-box (state!)) item)
  (refresh-box))

(defn- icon-label
  [ic-off ic-on size]
  (c/label :icon ic-off
           :border (b/empty-border :rigth 8 :left 8 :top 3 :bottom 3)
           :listen [:mouse-entered (fn [e]
                                     (gtool/hand-hover-on e)
                                     (c/config! e :icon ic-on))
                    :mouse-exited  (fn [e]
                                     (c/config! e :icon ic-off))]))

(defn- alert-skeleton
  [header body [c-border c-bg icon]]
  (let [padding  5
        s-border 2
        s-icon-2 22
        offset   (+ (* padding 2) (* s-border 2) (+ s-icon-2 5))]
    (let [close-icon (icon-label (gs/icon GoogleMaterialDesignIcons/CLOSE)
                                 (gs/icon GoogleMaterialDesignIcons/CLOSE face/c-icon-focus)
                                 s-icon-2)]
      (gmg/migrid
       :v (format "[::%s, fill]" (- (:box-w (state!)) offset)) "[20, fill]0px[fill]"
       {:args [:border (b/compound-border
                        (b/empty-border :thickness padding)
                        (b/line-border  :thickness s-border :color c-border))
               :background c-bg
               :user-data close-icon]}
       [(c/label :text header
                 :font (gs/getFont :bold)
                 :icon icon
                 :size [(- (:box-w (state!)) offset) :by 20])

        (gmg/migrid :> :f {:args [:background c-bg]}
                    [(c/label :size   [(- (:box-w (state!)) offset) :by 30]
                              :text   body)
                     close-icon])]))))

(defn- alert-type-src ;; TODO: Alert info bugging, do not loaded correct colors after changing theme
  [type key]
  (get-in {:alert   {:border face/c-alert-alert-border
                     :bg face/c-alert-bg
                     :icon (gs/icon GoogleMaterialDesignIcons/INFO face/c-icon-info)}          
           :warning {:border face/c-alert-warning-border
                     :bg face/c-alert-bg
                     :icon (gs/icon GoogleMaterialDesignIcons/REPORT_PROBLEM face/c-icon-warning)}        
           :danger  {:border face/c-alert-danger-border
                     :bg face/c-alert-bg
                     :icon (gs/icon GoogleMaterialDesignIcons/REPORT face/c-icon-danger)}
           :success {:border face/c-alert-success-border
                     :bg face/c-alert-bg
                     :icon (gs/icon GoogleMaterialDesignIcons/CHECK_CIRCLE face/c-icon-success)}}
          [type key]))

(defn- alert-type [type]
  [(alert-type-src type :border) (alert-type-src type :bg) (alert-type-src type :icon)])


(defn- open-in-popup
  [type header body s-popup expand]
  (let [pop (popup/build-popup {:size s-popup
                                :comp-fn   (fn [] (gmg/migrid
                                                   :v :center :fg {:gap [10]}
                                                   (concat [(gtool/htmling body)
                                                            (if (fn? expand) (expand) [])])))
                                :title      header
                                :title-icon (alert-type-src type :icon)
                                :args [:border (b/line-border :thickness 2 :color (alert-type-src type :border))]})]
    (c/move! pop :to-front)
    pop))

(defn- template [type header body timelife s-popup expand actions]
  (let [mig (alert-skeleton header body (alert-type type))]
    (let [close-icon (c/config mig :user-data)
          close-fn   (fn [e] (.remove (:alerts-box (state!)) mig) (refresh-box))]
      (c/config! close-icon :listen [:mouse-clicked close-fn])
      (.start (Thread. (fn [] (if (> timelife 0) (do (Thread/sleep (* 1000 timelife)) (close-fn 0)))))))
    (c/config! mig
               :listen [:mouse-clicked
                        (fn [e]
                          (open-in-popup type header body s-popup expand)
                          (refresh-box))])
    mig))

(defn alert
  "Description:
    Invoke alert popup.
    Types keys:
      :alert
      :warning
      :danger
  Example:
    Basic example:  (alert \"Information\" \"Some message\" :type :alert :time 3)
    Same but short: (alert \"Information\" \"Some message\")
    Without timer   (alert \"Information\" \"Some message\" :type :danger :time 0)

    If you want to add some special content set to key :expand some fn:
      (alert \"Information\" \"Some message\" :expand (fn [] (make-some-component)))
  
    If you want to add some quick action buttons set to key :actions
    vector with map description buttons like [{:title \"Apply\" :icon nil :func (fn [e] (do-some))} ...]
      (alert \"Information\" \"Some message\" :actions [{:title \"Apply\" :icon nil :func (fn [e] (do-some))}])"
  [header body
   & {:keys [type time s-popup expand actions]
      :or   {type :alert
             time 3
             s-popup [300 320]
             actions []
             expand  nil}}]
  (dispatch! {:action :inc-index})
  (dispatch! {:action :store-new-alerts
              :alert  {:header  header
                       :body    body
                       :s-popup s-popup
                       :expand  expand
                       :type    type
                       :time    time
                       :actions actions}})
  (add-to-alerts-box (template type header body time s-popup expand actions)))


;; ┌──────────────────┐
;; │                  │
;; │   TEMP Alerts    │
;; │                  │
;; └──────────────────┘

(defn temp-alert
  [header body
   & {:keys [type time s-popup expand actions]
      :or   {type :alert
             time 3
             s-popup [300 320]
             actions []
             expand  nil}}]
  (dispatch! {:action :store-in-temp
              :alert  {:header  header
                       :body    body
                       :s-popup s-popup
                       :expand  expand
                       :type    type
                       :time    time
                       :actions actions}}))

(defn load-temp-alerts
  []
  (doall
   (map (fn [[k m]]
          (println "\n" k m)
          (alert (:header m) (:body    m)
                 :s-popup    (:s-popup m)
                 :type       (:type    m)
                 :time       (:time    m)
                 :expand     (:expand  m)
                 :actions    (:actions m))
          )
        (state/state :temp-alerts-storage)))
  (dispatch! {:action :clear-temp}))

(comment
  (temp-alert "TEMP1" "It'a an TEMP alert invoking later.")
  (temp-alert "TEMP2" "It'a an TEMP alert invoking later.")
  (temp-alert "TEMP3" "It'a an TEMP alert invoking later.")
  (load-temp-alerts)
  (:temp-alerts-storage (state!))
  )

;; ┌──────────────────┐
;; │                  │
;; │  History panel   │
;; │                  │
;; └──────────────────┘

(defn- history-part
  [type header body s-popup expand w]
  (gmg/migrid
   :>
   (let [c-bg       (alert-type-src type :background)
         c-bg-focus face/c-alert-history-focus]
     (c/label :text (gtool/htmling (gtool/str-cutter (str "<b>" header "</b> " body) (/ w 7)) :no-wrap)
              :icon (alert-type-src type :icon)
              :background c-bg
              :border (b/compound-border
                       (b/empty-border :thickness 5)
                       (b/line-border :left 5 :color (alert-type-src type :border)))
              :listen [:mouse-entered (fn [e]
                                        (gtool/hand-hover-on e)
                                        (c/config! e :background c-bg-focus))
                       :mouse-exited  (fn [e]
                                        (c/config! e :background c-bg))
                       :mouse-clicked (fn [e]
                                        (open-in-popup type header body s-popup expand))]))))

(defn- alerts-history-list [w]
  (doall
   (reverse
    (map
     (fn [[k m]]
       (history-part (:type m) (:header m) (:body m) (:s-popup m) (:expand m) w))
     (:alerts-storage (state!))))))

(defn- clear-alerts-history []
  (dispatch! {:action :clear-history})
  (dispatch! {:action :clear-index}))

(defn history-in-popup
  []
  (let [w 350
        h 400
        history-box (gmg/migrid :v (alerts-history-list w))]
    (popup/build-popup
     {:size [w h]
      :comp-fn   (fn [] (gmg/migrid
                         :v :a :gf
                         [(gcomp/min-scrollbox history-box)
                          (gcomp/button-basic "Clear"
                                              :flip-border true
                                              :onClick (fn [e]
                                                         (clear-alerts-history)
                                                         (c/config! history-box :items [])))]))
      :title     "Alerts History"
      :title-icon (gs/icon GoogleMaterialDesignIcons/HISTORY face/c-foreground)
      :args [:border (b/line-border :thickness 2 :color (alert-type-src type :border))]})))


(comment
 (start)
 (clear-alerts-box)
 (alert "Information" "Hello. It's a new popup alerts.")
 (alert "Warning!" "Some code can crashing."  :type :warning)
 (alert "Red alert!" "Some code can crashing. It's the end of world!" :type :danger)

 (alert "Interaction" "Click OK if you are human." :type :warning :s-popup [300 150]
        :expand (fn [] (gmg/migrid
                        :v :center :bottom
                        (gmg/migrid
                         :> :f {:gap [10]}
                         [(gcomp/button-basic "I'm a human")
                          (gcomp/button-basic "Kill all humans!")]))))

 (history-in-popup)
 (:alerts-storage (state!))
 )
