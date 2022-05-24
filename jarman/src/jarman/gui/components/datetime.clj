(ns jarman.gui.components.datetime
  (:require
   ;; ;; Seesaw
   [seesaw.core    :as c]
   [seesaw.border  :as b]
   ;; ;; Jarman
   [jarman.tools.lang     :refer :all]
   [jarman.faces          :as face]
   [jarman.gui.core       :refer [satom register! cursor]]
   [jarman.gui.components.panels :as gui-panels]
   [jarman.gui.components.common :refer [label button combobox]])
  (:import
   [java.util Date Calendar]
   [javax.swing SwingUtilities]
   [java.awt Point Toolkit]))


;;  _____ ___ __  __ _____ 
;; |_   _|_ _|  \/  | ____|
;;   | |  | || |\/| |  _|  
;;   | |  | || |  | | |___ 
;;   |_| |___|_|  |_|_____|
;;   ____    _    _     _____ _   _ ____    _    ____  
;;  / ___|  / \  | |   | ____| \ | |  _ \  / \  |  _ \ 
;; | |     / _ \ | |   |  _| |  \| | | | |/ _ \ | |_) |
;; | |___ / ___ \| |___| |___| |\  | |_| / ___ \|  _ < 
;;  \____/_/   \_\_____|_____|_| \_|____/_/   \_\_| \_\


(defn date-setter [value]
  (cond
    (instance? java.lang.String value) value
    (instance? java.util.Date value) (.format (java.text.SimpleDateFormat. "yyyy-MM-dd HH:mm") value)
    :else value))

(declare datetime)
(declare datetime-label)
(declare create-body-panel)
(declare create-footer-panel)
(declare create-title-panel)

(defn datetime
  [& {:keys [^String value
             ^clojure.lang.IFn on-click
             ^clojure.lang.IFn value-setter]
      :or {on-click (fn [e] e)
           value-setter date-setter}}]
  (where
   ((date-formater (java.text.SimpleDateFormat. "yyyy-MM-dd HH:mm"))
    (week-start-day  1) ;; [1-7]
    (calendar-obj (let [x (java.util.Calendar/getInstance)]
                    (.setTime x
                              (try
                                (.parse (java.text.SimpleDateFormat. "yyyy-MM-dd HH:mm") (value-setter value))
                                (catch java.text.ParseException e
                                  (java.util.Date.))
                                (catch java.lang.NullPointerException e
                                  (java.util.Date.)))) x))
    (state    (satom {:date calendar-obj}))
    (dispatch (fn dispatch [k f] (fn [e] (swap! state #(update % k (fn [cal] (f cal))))))))
   (gui-panels/border-panel
    :border (b/empty-border :bottom 1 :top 1 :left 1 :right 1)
    ;; :north  (create-title-panel state dispatch date-formater)
    ;; :center (create-body-panel state dispatch week-start-day)
    :north  (create-title-panel (cursor [:date] state) date-formater on-click)
    :center (create-body-panel  (cursor [:date] state) date-formater on-click week-start-day)
    :south  (create-footer-panel))))

;;; TITLE PANEL ;;;

(defn- calendar-minus-year [^java.util.Calendar calendar]
  (let [new-calendar (.clone calendar)]
    (doto new-calendar
     (.add java.util.Calendar/YEAR -1))))
(defn- calendar-plus-year [^java.util.Calendar calendar]
  (let [new-calendar (.clone calendar)]
    (doto new-calendar
     (.add java.util.Calendar/YEAR 1))))
(defn- calendar-minus-month [^java.util.Calendar calendar]
  (let [new-calendar (.clone calendar)]
    (doto new-calendar
     (.add java.util.Calendar/MONTH -1))))
(defn- calendar-plus-month [^java.util.Calendar calendar]
  (let [new-calendar (.clone calendar)]
    (doto new-calendar
      (.add java.util.Calendar/MONTH 1))))
(defn- calendar-change-hour [^java.util.Calendar calendar hour]
  {:pre [(in? (range 0 24) hour)]}
  (let [new-calendar (.clone calendar)]
    (doto new-calendar
      (.set java.util.Calendar/HOUR_OF_DAY hour))))
(defn- calendar-change-minute [^java.util.Calendar calendar minutes]
  {:pre [(in? (range 0 60) minutes)]}
  (let [new-calendar (.clone calendar)]
    (doto new-calendar
      (.set java.util.Calendar/MINUTE minutes))))


(defn- create-title-panel [^jarman.gui.core.Cursor state ^java.text.SimpleDateFormat date-formater on-click]
  (gui-panels/border-panel
   ;; TIME CONTROLLER
   :north
   (where
    ((sep-label    (label :value "hour/minut"))
     (hour-combo   (combobox :value (.get (deref state) Calendar/HOUR_OF_DAY) :model (vec (range 0 24)) :tip "hours"
                             :tgap 2 :lgap 10 :bgap 0 :rgap 10 :on-select (fn [e] (swap! state calendar-change-hour e))))
     (minut-combo  (combobox :value (.get (deref state) Calendar/MINUTE) :model (vec (range 0 24)) :tip "Minutes"
                             :tgap 2 :lgap 10 :bgap 0 :rgap 10 :on-select (fn [e] (swap! state calendar-change-minute e)))))
    (gui-panels/border-panel
     :west hour-combo
     :center sep-label
     :east minut-combo))
   ;; YEAR-MONTH CONTROLLER
   :south
   (where
    ((center     (button :value (.format date-formater (.getTime (deref state)))))
     (prev-year  (button :value "<<" :tip "Previos Year"  :tgap 2 :lgap 10 :bgap 0 :rgap 10 :on-click (fn [e] (swap! state calendar-minus-year))))
     (next-year  (button :value ">>" :tip "Next Year"     :tgap 2 :lgap 10 :bgap 0 :rgap 10 :on-click (fn [e] (swap! state calendar-plus-year))))
     (prev-month (button :value "<"  :tip "Previos Month" :tgap 2 :lgap 10 :bgap 0 :rgap 10 :on-click (fn [e] (swap! state calendar-minus-month))))
     (next-month (button :value ">"  :tip "Next Month"    :tgap 2 :lgap 10 :bgap 0 :rgap 10 :on-click (fn [e] (swap! state calendar-plus-month)))))
    (gui-panels/border-panel
     :west prev-year
     :east next-year
     :center
     (gui-panels/border-panel
      :west prev-month
      :east next-month
      :center center
      :event-hook-atom state
      :event-hook
      (fn [_ _ _ date]
        (println "TITLE BAR -> " date)
        (c/config! center :text (.format date-formater (.getTime date)))))))))


;;; BODY PANEL ;;;

(defn- day-numb [^java.util.Calendar required ^java.util.Calendar setted ^clojure.lang.IFn on-click]
  (where
   ((year  (.get required java.util.Calendar/YEAR))
    (month (.get required java.util.Calendar/MONTH))
    (day   (.get required java.util.Calendar/DAY_OF_MONTH))
    (is-weekend?    (in? [1 7] (.get required java.util.Calendar/DAY_OF_WEEK)))
    (current-month? (= month (.get setted java.util.Calendar/MONTH)))
    (current-day?   (and current-month?
                       (= day (.get setted java.util.Calendar/DAY_OF_MONTH)))))
   (button :value    (str day) 
          :on-click on-click
          :args [:foreground
                 (cond
                   current-day? "#A36"
                   is-weekend? "#3AA"
                   current-month? "#000"
                   :else "#AAA")])))



(defn create-body-panel [^jarman.gui.core.Cursor state  ^java.text.SimpleDateFormat date-formater on-click week-day-start]
  (where 
   ((create-items
     (fn [^java.util.Calendar original]
       (where
        ((tmpcal   (let [date (.clone original)]
                     (.set date java.util.Calendar/DAY_OF_MONTH 1) date))
         (index    (.get tmpcal java.util.Calendar/DAY_OF_WEEK)))
        (if (> index week-day-start)
          (.add tmpcal java.util.Calendar/DAY_OF_MONTH (- week-day-start index))
          (.add tmpcal java.util.Calendar/DAY_OF_MONTH (- week-day-start index 7)))
        (concat
         (map (partial c/label :text)
              (let [days ["Sun","Mon","Tue","Wed","Thu","Fri","Sat"]]
                (concat (drop week-day-start days) (take week-day-start days))))
         (map (fn [r]
                (.add tmpcal java.util.Calendar/DAY_OF_MONTH 1)
                (let [day-date (.clone tmpcal)]
                  (day-numb tmpcal original (fn [_]
                                              (on-click (.format date-formater (.getTime day-date)))
                                              (swap! state (constantly day-date))))))
              (range 42)))))))
   (gui-panels/grid-panel
    :rows 7
    :columns 7
    :preferred-size [300 :by 300]
    :items (create-items (.clone (deref state)))
    :event-hook-atom state
    :event-hook
    (fn [panel _ _ new-state]
      (println "BODY PANEL -> " new-state)
      (c/config! panel :items (create-items new-state))))))


;;; FOOTER PANEL ;;;

(defn- create-footer-panel []
  (where
   ((data-formater (java.text.SimpleDateFormat. "yyyy-MM-dd HH:mm")))
   (button :value (str "Today is: " (.format data-formater (Date.))))))


;;;;;;;;;;;;;;;;;;;
;;;; POPUP DEMO ;;;
;;;;;;;;;;;;;;;;;;;

(defn- datetime-popup [& {:keys [x y value on-click value-setter]
                         :or {on-click (fn [e] e) x 0 y 0}}]
  (let [popup (javax.swing.JWindow. (javax.swing.JFrame. "Calendar"))
        calendar (datetime :value value :on-click (fn [e] (on-click e) (.hide popup)) :value-setter value-setter)]
    (-> (doto popup
          (.setFocusableWindowState false)
          (.setType java.awt.Window$Type/POPUP)
          (.setLocationByPlatform true)
          (.setContentPane calendar)
          (.setLocation x y))
        (seesaw.core/pack!)
        (seesaw.core/show!))))

;;;;;;;;;;;;;;;;;;;
;;; CALENDAR UI ;;;
;;;;;;;;;;;;;;;;;;;

(defn datetime-label [& {:keys [value on-click value-setter] :or {on-click (fn [e] e) value-setter date-setter}:as args}]
  (let [clean-arg (dissoc args :value :on-click)
        apply-arg (interleave (keys clean-arg) (vals clean-arg))]
   (apply
    button
    :value (if (:value args) (value-setter (:value args)) "<unselected>")
    :on-click (fn [e]
                (let [component (c/to-widget e)
                      show (Point. 0 (.getHeight component))
                      _ (SwingUtilities/convertPointToScreen show component)
                      ^java.awt.Dimension size (.getScreenSize (Toolkit/getDefaultToolkit))
                      x (.-x show)
                      y (.-y show)
                      x (if (< x 0) 0 x)
                      x (if (> x (- (.-width size)  212)) (- (.-width size) 212) x)
                      y (if (> y (- (.-height size) 165)) (- y 165) y)]
                  (datetime-popup :value (c/value component)
                                  :value-setter value-setter
                                  :on-click (fn [e] (on-click e) (c/config! component :text e))
                                  :x x :y y)))
    apply-arg)))


;;  ____  _____ __  __  ___  
;; |  _ \| ____|  \/  |/ _ \ 
;; | | | |  _| | |\/| | | | |
;; | |_| | |___| |  | | |_| |
;; |____/|_____|_|  |_|\___/ 
;;

(comment
  (seesaw.dev/show-options (seesaw.core/combobox))
  (seesaw.dev/show-events  (seesaw.core/combobox))
  (-> (doto (seesaw.core/frame
             :title "Jarman"
             :content (seesaw.mig/mig-panel
                       :background  face/c-compos-background-darker
                       :constraints ["wrap 1" "0px[grow, fill]0px" "0px[fill, top]0px"]
                       :border (b/empty-border :thickness 10)
                       :items [[(seesaw.core/label :text "Calendar Component" :font (jarman.gui.gui-tools/getFont :bold 20))]
                               [(datetime :value "2020-10-01 10:22")]
                               [(seesaw.core/label :text "Calendar as label" :font (jarman.gui.gui-tools/getFont :bold 20))]
                               [(datetime-label :value nil)]
                               [(datetime-label :value (java.util.Date.))]
                               [(datetime-label :value "2020-10-01 10:22" :on-click (fn [e] (println "some click for external feature")))]]))
        (.setLocationRelativeTo nil)
        seesaw.core/pack! seesaw.core/show!))
  ;; 
  )
