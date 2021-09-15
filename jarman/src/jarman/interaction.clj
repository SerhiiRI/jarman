(ns jarman.interaction)
(require '[jarman.gui.gui-alerts-service :as gas])


;; ┌──────────────────┐
;; │                  │
;; │  Alerts wraper   │
;; │                  │
;; └──────────────────┘
(defn info
  "Description:
    Wraper.
    Invoke alert box on Jarman."
  [header body
   & {:keys [type time s-popup actions expand]
      :or   {type :alert
             time 3
             s-popup [300 320]
             actions []
             expand  nil}}]
  (println "\n" header body type time s-popup expand actions)
  (gas/alert header body :type type :time time :s-popup s-popup :expand expand :actons actions))

(defn warning
  "Description:
    Wraper.
    Invoke alert box on Jarman."
  [header body
   & {:keys [type time s-popup actions expand]
      :or   {type :warning
             time 3
             s-popup [300 320]
             actions []
             expand  nil}}]
  (println "\n" header body type time s-popup expand actions)
  (gas/alert header body :type type :time time :s-popup s-popup :expand expand :actons actions))

(defn danger
  "Description:
    Wraper.
    Invoke alert box on Jarman."
  [header body
   & {:keys [type time s-popup actions expand]
      :or   {type :danger
             time 3
             s-popup [300 320]
             actions []
             expand  nil}}]
  (println "\n" header body type time s-popup expand actions)
  (gas/alert header body :type type :time time :s-popup s-popup :expand expand :actons actions))

(defn show-alerts-history [] (gas/history-in-popup))

;; Using alerts
(comment
  (info    "Test 1" "Info    box")
  (warning "Test 2" "Warning box")
  (danger  "Test 3" "Danger  box")
  (warning "Interaction" "Devil robot say:" :s-popup [300 150]
           :expand (fn [] (jarman.gui.gui-components/button-basic "Kill all humans!")))
  (show-alerts-history))
