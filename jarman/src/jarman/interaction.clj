(ns jarman.interaction)
(require '[jarman.gui.gui-alerts-service :as gas])
(require '[jarman.gui.gui-views-service  :as gvs])
(require '[jarman.gui.gui-editors        :as gedit])
(require '[jarman.gui.gui-components     :as gcomp])
(require '[jarman.gui.gui-tools          :as gtool])
(require '[jarman.tools.org :refer :all])
(require '[clojure.pprint :refer [cl-format]])


;; ┌──────────────────┐
;; │                  │
;; │  App restart     │
;; │                  │
;; └──────────────────┘

(defn restart
  "Description:
    Restart app without cleaning global state
    All loading lvls will be invoked
    App will sturtup again with creating new frame"
  [] (gvs/restart))

(defn soft-restart
  "Description:
    Restart app without rebuild frame and global state
    Soft restart do not invoke loding lvl-0 (plugins will not recompiling)
    Theme will be loaded again"
  [] (gvs/soft-restart))

(defn hard-restart
  "Description:
    Restart app with cleaning global state
    All loading lvls will be invoked
    App will sturtup again with creating new frame"
  [] (gvs/hard-restart))


(defn reload-view
  "Description:
    Reload active view on right app space (next to menu)"
  [] (try
       (gvs/reload-view)
       (catch Exception e (str "Can not reload. Storage is empty."))))



;; ┌──────────────────┐
;; │                  │
;; │  Alerts wraper   │
;; │                  │
;; └──────────────────┘
(defn info
  "Description:
    Wraper.
    Invoke alert box on Jarman."
  ([body] (info :info body))
  ([header body
    & {:keys [type time s-popup actions expand]
       :or   {type :alert
              time 5
              s-popup [300 320]
              actions []
              expand  nil}}]
   (gas/info header body :type type :time time :s-popup s-popup :expand expand :actons actions)))

(defn warning
  "Description:
    Wraper.
    Invoke alert box on Jarman."
  ([body] (warning :warning body))
  ([header body
    & {:keys [type time s-popup actions expand]
       :or   {type :warning
              time 5
              s-popup [300 320]
              actions []
              expand  nil}}]
   (gas/warning header body :type type :time time :s-popup s-popup :expand expand :actons actions)))

(defn danger
  "Description:
    Wraper.
    Invoke alert box on Jarman."
  ([body] (danger :danger body))
  ([header body
    & {:keys [type time s-popup actions expand]
       :or   {type :danger
              time 5
              s-popup [300 320]
              actions []
              expand  nil}}]
   (print-header
    "Ivoke danger frame"
    (print-line (cl-format nil "~@{~A~^, ~}" header body type time s-popup expand actions)))
   (gas/danger header body :type type :time time :s-popup s-popup :expand expand :actons actions)))


(defn success
  "Description:
    Wraper.
      Invoke alert box on Jarman."
  ([body] (success :success body))
  ([header body
    & {:keys [type time s-popup actions expand]
       :or   {type :success
              time 5
              s-popup [300 320]
              actions []
              expand  nil}}]
   (gas/success header body :type type :time time :s-popup s-popup :expand expand :actons actions)))

(defn restart-alert []
  (danger :need-reload
          [{:title (gtool/get-lang-btns :reload-app)
            :func (fn [api] (restart))}]
          :time 0))

(defn delay-alert
  [header body
   & {:keys [type time s-popup expand actions]
      :or   {type :alert
             time 3
             s-popup [300 320]
             actions []
             expand  nil}}]
  (gas/temp-alert header body
                  :s-popup s-popup
                  :expand  expand
                  :type    type
                  :time    time
                  :actions actions))


(defn show-delay-alerts [] (gas/load-temp-alerts))

(defn show-alerts-history [] (gas/history-in-popup))

;; Using alerts
(comment
  (info    "Test 1" "Info box")
  (warning "Test 2" "Warning box")
  (danger  "Test 3" "Danger box")
  (success "Test 4" "Success box")
  (warning "Interaction" "Devil robot say:" :s-popup [300 150]
           :expand (fn [] (jarman.gui.gui-components/button-basic "Kill all humans!")))
  (show-alerts-history)

  (delay-alert "TEMP1" "It'a an TEMP alert invoking later.")
  (delay-alert "TEMP2" "It'a an TEMP alert invoking later.")
  (show-delay-alerts)
  )



;; ┌──────────────────┐
;; │                  │
;; │  File editor     │
;; │                  │
;; └──────────────────┘


(defn editor
  "Description:
    Set path to directory and file name
  Example:
    (editor \"./test/test-file\")"
  ([file-path] (editor file-path nil))
  ([file-path syntax]
   (let [file-name (last (clojure.string/split file-path #"/"))]
     (gvs/add-view
      :view-id   (keyword (str "editor" file-name))
      :title     (str "Edit:  " file-name)
      :render-fn (fn [] (gedit/text-file-editor file-path syntax))))))

(comment
  (editor "./test/test-file.txt" :clojure)
  ) 


;; ┌──────────────────┐
;; │                  │
;; │  Doom debugger   │
;; │                  │
;; └──────────────────┘


(defn open-doom
  "Description:
    Open doom debugger or open doom debugger with set new component inside"
  ([] (gcomp/doom))
  ([compo] (gcomp/doom compo)))

(defn hide-doom
  "Hide doom without deleting last component"
  [] (gcomp/doom-hide))

(defn rm-doom
  "Close doom and remove component inside"
  [] (gcomp/doom-rm))

(comment
  (open-doom (seesaw.core/label :text "Pepe Dance"))
  (hide-doom)
  (open-doom)
  (rm-doom)
  )


