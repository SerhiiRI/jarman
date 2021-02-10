;; 
;; Compilation: dev_tool.clj -> metadata.clj -> gui_tools.clj -> gui_alerts_service.clj -> gui_app.clj
;; 
(ns jarman.gui.gui-alerts-service
  (:import (java.awt Color))
  (:use seesaw.core
        seesaw.border
        seesaw.dev
        seesaw.mig)
  (:require [jarman.gui.gui-tools :refer :all]
            [jarman.resource-lib.icon-library :as icon]
            [jarman.tools.swing :as stool]
            [jarman.config.config-manager :refer :all]
            [clojure.string :as string]))


;; Defrecord for elements in alerts-storage
;; [{:id 0 :data {:header "Some header" :body "Message" :btns [(btn1) (btn2)]} :component (label-fn :text \"Hello world! \") :timelife 3}]
(defrecord Alert [id data component timelife visible])

(defn view-selected-message
  "Description:
      Function for create window with clicked message.
   Example:
      (view-selected-message header body layered-pane)
   Needed:
      Import jarman.dev-tool
      Function need stool/image-scale for scaling button icon
      Function need middle-bounds for auto calculating bound
   "
  [header body layered-pane]
  (let [bg-c "#fff"
        header-fg-c "#fff"
        header-bg-c "#333"
        btn-bg-c header-bg-c
        btn-fg-c header-fg-c
      ;;   btn-bg-c-hover "#d9ecff"
        btn-bg-c-hover "#444"
        btn-fg-c-hover "#000"
        frame-c "#000"
        w 330
        h 400
        header-size 30
        ico-size 30
        box-border 2
        ico icon/x-grey2-64-png
      ;;   ico-hover icon/x-blue1-64-png
        bounds (middle-bounds (to-root layered-pane) (+ w box-border box-border) (+ h box-border box-border))]
    (.add layered-pane (mig-panel
                        :constraints ["wrap 1" (str "0px[" (str w) "]0px") (str "0px[" (str header-size) ", top]0px[" (str (- h header-size ico-size)) ", top]0px[" (str ico-size) ", top]0px")]
                        :id :message-view-box
                        :background (new Color 0 0 0 0)
                        :bounds bounds
                        :border (line-border :thickness box-border :color frame-c)
                        :items [[(label-fn :text header
                                           :size [w :by header-size]
                                           :halign :center
                                           :foreground header-fg-c
                                           :background header-bg-c
                                           :font (getFont 15 :bold))]
                                [(scrollable (label-fn :text (format "<html><body><p style=\"word-wrap: normal;\">%s</p></body><html>" body)
                                                       :size [w :by (- h ico-size ico-size)]
                                                       :font (getFont 13)
                                                       :background bg-c
                                                       :valign :top)
                                             :hscroll :never
                                             :border nil)]
                                [(label-fn
                                  :icon (stool/image-scale ico (- ico-size 5))
                                  :size [w :by ico-size]
                                  :foreground btn-fg-c
                                  :background btn-bg-c
                                  :text "Exit"
                                  :font (getFont 14)
                                  :halign :center
                                  :listen [:mouse-entered (fn [e] (config! e 
                                                                           ;; :icon (stool/image-scale ico-hover (- ico-size 5))
                                                                           ;; :foreground btn-fg-c-hover 
                                                                           :background btn-bg-c-hover 
                                                                           :cursor :hand))
                                           :mouse-exited  (fn [e] (config! e
                                                                           ;; :icon (stool/image-scale ico (- ico-size 5))
                                                                           ;; :foreground btn-fg-c
                                                                           :background btn-bg-c))
                                           :mouse-clicked (fn [e] (do
                                                                    (.remove layered-pane (select layered-pane [:#message-view-box]))
                                                                    (.repaint layered-pane)))])]])
          (new Integer 25))))

(defn message
  "Description:
      Template for messages. Using in Jlayered-pane.
      X icon remove and rebound displayed message.
      Loop icon display whole message.
   Example:
      (message alerts-controller) and next fn will be send to message-server-creator and then will looks like that (fn [{:header 'Hello' :body 'World!'}] (magic))
   Needed:
      Import jarman.gui-tools
      Function need build-ico for message icon
      Function need build-header for message header
      Function need build-body for message body
      Function need build-bottom-ico-btn for functional icon buttons on bottom
      Function need view-selected-message for show whole message
   "
  [alerts-controller]
  (fn [data]
    (let [font-c "#000"
          bg-c "#fff"
          header (if (= (contains? data :header) true) (get data :header) "Information")
          body   (if (= (contains? data :body) true) (get data :body) "Template of information...")
          layered (alerts-controller :get-space)
          close [(build-bottom-ico-btn icon/loupe-grey-64-png icon/loupe-blue1-64-png layered 23 (fn [e] (view-selected-message header body layered)))
                 (build-bottom-ico-btn icon/x-grey-64-png icon/x-blue1-64-png layered 23 (fn [e] (let [to-del (.getParent (.getParent (seesaw.core/to-widget e)))] (alerts-controller :rm-obj to-del))))]]
      (mig-panel
       :id :alert-box
       :constraints ["wrap 1" "0px[fill, grow]0px" "0px[20]0px[30]0px[20]0px"]
       :background bg-c
       :border nil
       :bounds [680 480 300 75]
       :items [[(flow-panel
                 :align :left
                 :background (new Color 0 0 0 0)
                 :items [(build-ico icon/alert-64-png)
                         (build-header (str-cutter header))])]
               [(build-body (str-cutter body))]
               [(flow-panel
                 :align :right
                 :background (new Color 0 0 0 1)
                 :items (if (= (contains? data :btns) true) (concat close (get data :btns)) close))]]))))



(def alerts-rebounds-f
  "Description:
      Rebound message components and resize them.
   Needed:
      Function need get-elements-in-layered-by-id function for get all same element (here message boxs)
      Function need getHeight function for quick getting height size
   "
  (fn [e] (let [list-of-alerts (get-elements-in-layered-by-id e "alert-box")
                bound-x (if list-of-alerts (- (getWidth (to-root (seesaw.core/to-widget e))) (getWidth (first list-of-alerts)) 20) 0)
                height 120]
            (if list-of-alerts (doseq [[n elem] (map-indexed #(vector %1 %2) list-of-alerts)]
                                 (config! elem :bounds [bound-x (- (- (getHeight (to-root (seesaw.core/to-widget e))) height) (* 80 n)) 300 75]))))))



(defn refresh-alerts
  "Description:
      Functrion refreshing message on JLayeredPane (GUI).
   Example:
      ;; Just refresh messages
      (refresh-alerts layered-pane alerts-storage)
      ;; Refresh messages and remove element by ID
      (refresh-alerts layered-pane alerts-storage :all-alerts)
   Needed:
      Function need alerts-rebounds-f for set correct possition
   "
  ([layered-pane alerts-storage] (do
                                    ;;  Remove alerts
                                    (doall (map (fn [item] (if (identical? (config item :id) :alert-box) (.remove layered-pane item))) (seesaw.util/children layered-pane)))
                                    ;; Add alerts
                                    (doall (map (fn [item] (if (= (get item :visible) true) (.add layered-pane (get item :component) (new Integer 15)))) @alerts-storage))
                                    ;;  Rebounds message space
                                    (alerts-rebounds-f layered-pane)
                                    ;;  Repainting app
                                    (.repaint layered-pane)))
  ([layered-pane alerts-storage id-to-remove] (do
                                                 ;;  Remove alerts
                                                 (doall (map (fn [i] (if (or (identical? (config i :id) :alert-box)
                                                                             (identical? (config i :id) id-to-remove))
                                                                       (.remove layered-pane i))) (seesaw.util/children layered-pane)))
                                                 ;; Add alerts
                                                 (doall (map (fn [item] (if (= (get item :visible) true) (.add layered-pane (get item :component) (new Integer 15)))) @alerts-storage))
                                                 ;;  Rebounds message space
                                                 (alerts-rebounds-f layered-pane)
                                                 ;;  Repainting app
                                                 (.repaint layered-pane))))

(defn rmAlert
  "Description:
       Remove message by id from storage.
   Example: 
       (rmAlert 0 storage)
   "
  [id alerts-storage layered-pane] (do
                                      (swap! alerts-storage #(vec (map (fn [item] (if (= (get item :id) id) (assoc item :visible false) item)) %)))
                                      (refresh-alerts layered-pane alerts-storage)))


(defn rmAlertObj
  "Description:
       Set message inactive in storage. Function search same object to localizate correct message.
       Function refreshing GUI.
   Example: 
       (rmAlertObj object storage message-box)
   Needed:
      Function need refresh-alerts function to refresh GUI.
   "
  [obj alerts-storage layered-pane] (do
                                      ;;  Change visible to flase in correct record
                                       (swap! alerts-storage #(vec (map (fn [item] (if (identical? (get item :component) obj) (assoc item :visible false) item)) %)))
                                       (refresh-alerts layered-pane alerts-storage)))


(defn rmallAlert
  "Description:
      Remove all message from storage.
      Function refreshing GUI.
   Example: 
       (rmallAlert storage)
   Needed:
      Function need refresh-alerts function to refresh GUI.
   "
  [alerts-storage layered-pane] (do (reset! alerts-storage [])
                                     (refresh-alerts layered-pane alerts-storage)))


(defn addAlert
  "Description:
      Add new alert to storage and return ID.
   Example:
      (addAlert {:header 'Hello' :body 'World'} (some-widget) 5 alert-storage)
   "
  [data component timelife alerts-storage]
  (let [id (+ (if (= (last @alerts-storage) nil) -1 (:id (last @alerts-storage))) 1)]
    (swap! alerts-storage #(conj % (Alert. id data component timelife true)))
    id))



(defn addAlertTimeout
  "Description:
       Add message to storage and return id of this alert. Message will be inactive automaticly after timeout (in sec).
   Example: 
       (addAlertTimeout {:header 'Hello' :body 'World!'} (some-widget) 3 storage) 
       => in storage [... {:id 0 :data {:header 'Hello' :body 'World!'} :component (label-fn :text 'Hello World!') :timelife 3 storage}]
   Needed:
      Function need addAlert and rmAlert functions to work.
   "
  [data component timelife alerts-storage layered-pane]
  (.start (Thread. (fn [] (let [id (addAlert data component timelife alerts-storage)]
                            (if (> timelife 0) (do
                                                 (Thread/sleep (* 1000 timelife))
                                                 (rmAlert id alerts-storage layered-pane))))))))


(defn history-alert
  "Description:
      Template for message in all-messeges-window.
   Example:
      (history-alert {:header 'Hello' :body 'World!'})
   "
  [data layered-pane size]
  (let [w (first size)
        h (last size)
        bg-c     "#fff"
        hover-c  "#d9ecff"
        bord     (empty-border :left 5)
        header   (if (= (contains? data :header) true) (get data :header) "Information")
        body     (if (= (contains? data :body) true) (get data :body) "Template of information...")
        label-fn (fn [text & font]
                   (label-fn :size [w :by h] :background bg-c :border bord :text text)
                   (label-fn :size [w :by h] :background bg-c :border bord :text text :font (first font)))]
    (vertical-panel
     :background (new Color 0 0 0 0)
     :border (line-border :bottom 2 :color "#333")
     :items [(label-fn header (getFont 14 :bold)) (label-fn body)]
     :listen [:mouse-entered (fn [e] (let [children (seesaw.util/children (seesaw.core/to-widget e))]
                                       (do
                                         (config! e :cursor :hand)
                                         (doall (map (fn [item] (config! item :background hover-c)) children)))))
              :mouse-exited (fn [e] (let [children (seesaw.util/children (seesaw.core/to-widget e))]
                                      (do
                                        (config! e :cursor :default)
                                        (doall (map (fn [item] (config! item :background bg-c)) children)))))
              :mouse-clicked (fn [e] (view-selected-message header body layered-pane))])))

(defn all-messages-window
  "Description:
      Function creating window with all active and hidden message. Argument layered-pane should be JLayeredPane.
   Example:
      (layered-pane alerts-storage)
   Needed:
      Function need history-alert function for create message component inside.
      Function need rmallAlert function for clean history.
      Function need refresh-alerts function for GUI refresh with removing all-message-window element.
   "
  [layered-pane alerts-storage]
  (let [w     330
        h     400
        ico-size 30
        header-size 40
        fg-c  "#fff"
        bg-c  "#333"
        header-c  "#666"
        frame-c "#000"
        btn-bg-c "#fff"
        btn-bg-c-hover "#ddd"
        border 2
        root-size (getSize (to-root layered-pane))
        bounds (middle-bounds layered-pane (+ w border border) (+ h border border))
        items  (vec (map (fn [item] (history-alert (get item :data) layered-pane [w 30])) @alerts-storage))
        space-for-message (mig-panel
                           :constraints ["wrap 1" "0px[fill, grow]0px" (str "0px[" (str header-size) "]0px[" (str (- h header-size ico-size)) "]0px[" (str ico-size) "]0px")]
                           :bounds bounds
                           :border (line-border :thickness border :color frame-c)
                           :id :all-alerts
                           :items [[(label-fn :text "Alerts history"
                                           :size [w :by (- header-size 1)]
                                           :background header-c
                                           :foreground fg-c
                                           :font (getFont 14)
                                           :halign :center
                                           :border (line-border :bottom 1 :color frame-c))]
                                   [(scrollable
                                     (vertical-panel
                                      :items items
                                      :background bg-c)
                                     :hscroll :never
                                     :border nil
                                     :size [w :by (- h header-size ico-size)]
                                     :listen [:mouse-motion (fn [e] (.repaint (select (to-root e) [:#all-alerts])))
                                              :mouse-wheel-moved (fn [e] (.repaint (select (to-root e) [:#all-alerts])))])]
                                   [(mig-panel
                                     :constraints ["" "0px[]0px" (str "0px[" (str (- ico-size 1)) "]0px")]
                                     :background fg-c
                                     :border (line-border :top 1 :color bg-c)
                                     :items [[(label-fn
                                               :text "Exit"
                                               :icon (stool/image-scale icon/x-blue1-64-png (- ico-size 5))
                                               :halign :center
                                               :font (getFont 14)
                                               :size [(/ w 2) :by ico-size]
                                               :listen [:mouse-entered (fn [e] (config! e :background btn-bg-c-hover :cursor :hand))
                                                        :mouse-exited (fn [e] (config! e :background btn-bg-c))
                                                        :mouse-clicked (fn [e]
                                                                                      ;;  refresh GUI with remove element with id :all-alerts
                                                                         (refresh-alerts layered-pane alerts-storage :all-alerts))])]
                                             [(label-fn
                                               :text "Clear all message"
                                               :icon (stool/image-scale icon/basket-blue1-64-png ico-size)
                                               ;; :icon (stool/image-scale icon/basket-blue1-64x64-png ico-size)
                                               :halign :center
                                               :font (getFont 14)
                                               :size [(/ w 2) :by ico-size]
                                               :background btn-bg-c
                                               :listen [:mouse-entered (fn [e] (config! e :background btn-bg-c-hover :cursor :hand))
                                                        :mouse-exited (fn [e] (config! e :background btn-bg-c))
                                                        :mouse-clicked (fn [e] (do
                                                                                              ;;  remove all history
                                                                                 (rmallAlert alerts-storage layered-pane)
                                                                                              ;;  refresh GUI with remove element with id :all-alertsts
                                                                                 (refresh-alerts layered-pane alerts-storage :all-alerts)))])]])]])
        ]
      (.add layered-pane space-for-message (new Integer 20))
    ))


(defn message-server-creator
  "Description:
      Alerts on JLayeredPane. Service creating storage for message and can controll GUI elements.
   Example: 
      (def alert-service (message-server-creator my-main-app-gui)) #Server is active
      (alert-service :set {:header 'Hello' :body 'World' :btns [(button :text 'Click me')]} message-fn 5)
   Needed:
      Function need addAletrTimeout
      Function need rmAlert
      Function need rmAlertObj
      Function need rmallAlert
      Function need refresh-alerts
      Function need all-message-window
   "
  [layered-pane]
  (let [alerts-storage (atom [])]
    (add-watch alerts-storage :refresh
               (fn [key atom old-state new-state]
                 (cond
                   (> (count @alerts-storage) 0) (refresh-alerts layered-pane alerts-storage)
                   :else (.repaint layered-pane))))
    (fn [action & param]
      (cond
        (= action :get-space)     layered-pane
        (= action :set)           (let [[data func timelife] param] (addAlertTimeout data (func data) timelife alerts-storage layered-pane))
        (= action :rm)            (let [[id] param] (rmAlert id alerts-storage layered-pane))
        (= action :rm-obj)        (let [[obj] param] (rmAlertObj obj alerts-storage layered-pane))
        (= action :clear)         (rmallAlert alerts-storage layered-pane)
        (= action :count-all)     (count @alerts-storage)
        (= action :count-active)  (count (filter (fn [item] (if (= (get item :visible) true) item)) @alerts-storage))
        (= action :count-hidden)  (count (filter (fn [item] (if (= (get item :visible) false) item)) @alerts-storage))
        (= action :show)          (do (refresh-alerts layered-pane alerts-storage :all-alerts) (all-messages-window layered-pane alerts-storage))
        (= action :hide)          (refresh-alerts layered-pane alerts-storage :all-alerts)))))

