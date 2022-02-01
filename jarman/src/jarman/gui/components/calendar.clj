(ns jarman.gui.components.calendar
  ;; ---
  (:require
   ;; ;; Seesaw
   [seesaw.core    :as c]
   [seesaw.border  :as b]
   ;; ;; Jarman
   [jarman.tools.lang     :refer :all]
   [jarman.faces          :as face]
   [jarman.gui.core       :refer [satom register! cursor]]
   [jarman.gui.components.panels :as gui-panels]
   [jarman.gui.components.common :refer [label button]])
  (:import
   [java.util Date Calendar]
   [javax.swing SwingUtilities]
   [java.awt Point Toolkit]))


;;   ____    _    _     _____ _   _ ____    _    ____  
;;  / ___|  / \  | |   | ____| \ | |  _ \  / \  |  _ \ 
;; | |     / _ \ | |   |  _| |  \| | | | |/ _ \ | |_) |
;; | |___ / ___ \| |___| |___| |\  | |_| / ___ \|  _ < 
;;  \____/_/   \_\_____|_____|_| \_|____/_/   \_\_| \_\
;;                                                                               

(declare calendar)
(declare calendar-label)
(declare create-body-panel)
(declare create-footer-panel)
(declare create-title-panel)

(defn calendar
  [& {:keys [^String value
             ^clojure.lang.IFn on-click]
      :or {on-click (fn [e] e)}}]
  (where
   ((date-formater (java.text.SimpleDateFormat. "yyyy-MM-dd"))
    (week-start-day  1) ;; [1-7]
    (calendar-obj (let [x (java.util.Calendar/getInstance)]
                    (.setTime x
                              (try
                                (.parse (java.text.SimpleDateFormat. "yyyy-MM-dd") value)
                                (catch java.text.ParseException e
                                  (java.util.Date.))))
                    
                    x))
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


#_(defn create-title-panel [^jarman.gui.core.SwingAtom state ^clojure.lang.IFn dispatch ^java.text.SimpleDateFormat date-formater]
  (where
   ((center     (label :value (.format date-formater (.getTime (get (deref state) :date)))))
    (prev-year  (label :value "<<" :tip "Previos Year"  :tgap 2 :lgap 10 :bgap 0 :rgap 10 :on-click (dispatch :date calendar-minus-year)))
    (next-year  (label :value ">>" :tip "Next Year"     :tgap 2 :lgap 10 :bgap 0 :rgap 10 :on-click (dispatch :date calendar-plus-year)))
    (prev-month (label :value "<"  :tip "Previos Month" :tgap 2 :lgap 10 :bgap 0 :rgap 10 :on-click (dispatch :date calendar-minus-month)))
    (next-month (label :value ">"  :tip "Next Month"    :tgap 2 :lgap 10 :bgap 0 :rgap 10 :on-click (dispatch :date calendar-plus-month))))
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
     (fn [_ _ _ {date :date}]
       (c/config! center :text (.format date-formater (.getTime date))))))))


(defn create-title-panel [^jarman.gui.core.Cursor state ^java.text.SimpleDateFormat date-formater on-click]
  (where
   ((center     (label :value (.format date-formater (.getTime (deref state)))))
    (prev-year  (label :value "<<" :tip "Previos Year"  :tgap 2 :lgap 10 :bgap 0 :rgap 10 :on-click (fn [e] (swap! state calendar-minus-year))))
    (next-year  (label :value ">>" :tip "Next Year"     :tgap 2 :lgap 10 :bgap 0 :rgap 10 :on-click (fn [e] (swap! state calendar-plus-year))))
    (prev-month (label :value "<"  :tip "Previos Month" :tgap 2 :lgap 10 :bgap 0 :rgap 10 :on-click (fn [e] (swap! state calendar-minus-month))))
    (next-month (label :value ">"  :tip "Next Month"    :tgap 2 :lgap 10 :bgap 0 :rgap 10 :on-click (fn [e] (swap! state calendar-plus-month)))))
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
       (c/config! center :text (.format date-formater (.getTime date))))))))

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
   (label :value    (str day) 
          :on-click on-click
          :args [:foreground
                 (cond
                   current-day? "#A36"
                   is-weekend? "#3AA"
                   current-month? "#000"
                   :else "#AAA")])))

#_(defn create-body-panel [^jarman.gui.core.SwingAtom state ^clojure.lang.IFn dispatch week-day-start]
  (where 
   ((create-items
     (fn [original]
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
         (map (fn [_]
                (do
                  (.add tmpcal java.util.Calendar/DAY_OF_MONTH 1)
                  (day-numb tmpcal original (dispatch :date (constantly (.clone tmpcal))))))
              (range 42)))))))
   (gui-panels/grid-panel
    :rows 7
    :columns 7
    :preferred-size [300 :by 300]
    :event-hook-atom state
    :event-hook
    (fn [panel _ _ new-state]
      (println new-state)
      (c/config! panel :items (create-items (:date new-state))))
    :items (create-items (.clone (:date (deref state)))))))

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
   ((data-formater (java.text.SimpleDateFormat. "yyyy-MM-dd")))
   (label :value (str "Today is: " (.format data-formater (Date.))))))

;;;;;;;;;;;;;;;;;;;
;;;; POPUP DEMO ;;;
;;;;;;;;;;;;;;;;;;;

(defn- calendar-popup [& {:keys [x y value on-click]
                         :or {on-click (fn [e] e) x 0 y 0}}]
  (let [popup (javax.swing.JWindow. (javax.swing.JFrame. "Calendar"))
        calendar (calendar :value value :on-click (fn [e] (on-click e) (.hide popup)))]
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

(defn calendar-label [& {:keys [on-click] :or {on-click (fn [e] e)}:as args}]
  (let [clean-arg (dissoc args :value :on-click)
        apply-arg (interleave (keys clean-arg) (vals clean-arg))]
   (apply
    label
    :value (:value args)
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
                  (calendar-popup :value (c/value component)
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
                               [(calendar :value "2020-10-01")]
                               [(seesaw.core/label :text "Calendar as label" :font (jarman.gui.gui-tools/getFont :bold 20))]
                               [(calendar-label :value "2020-10-01" :on-click (fn [e] (println "some click for external feature")))]]))
        (.setLocationRelativeTo nil)
        seesaw.core/pack! seesaw.core/show!))
  ;; 
  )
