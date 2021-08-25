(ns jarman.gui.gui-alerts-service
  (:import (java.awt Color))
  (:use seesaw.dev
        seesaw.mig)
  (:require [seesaw.core                      :as c]
            [seesaw.border                    :as b]
            [jarman.tools.swing               :as stool]
            [jarman.gui.gui-style             :as gs]
            [jarman.gui.gui-tools             :as gtool]
            [jarman.gui.gui-components        :as gcomp]
            [jarman.logic.state               :as state]
            [clojure.string                   :as string]
            [jarman.resource-lib.icon-library :as icon]
            [jarman.tools.lang                :refer :all]))


;; Defrecord for elements in alerts-storage
;; [{:id 0 :data {:header "Some header" :body "Message" :btns [(btn1) (btn2)]} :component (c/label :text \"Hello world! \") :timelife 3}]
(defrecord Alert [id data component timelife visible])


(defn build-ico
  "Description:
      Icon for message box. Create component with icon.
   Example:
      (build-ico icon/alert-64-png)
   Needed:
      Import jarman.dev-tools
      Function need stool/image-scale function for scalling icon"
  [ic] (c/label :icon (stool/image-scale ic 28)
                :background (new Color 0 0 0 0)
                :border (b/empty-border :left 3 :right 3)))

(defn build-header
  "Description:
      Header text for message box. Create component with header text.
   Example:
      (build-header 'Information')
   "
  [txt] (c/label :text txt
                 :font (gs/getFont :bold)
                 :background (new Color 0 0 0 0)))

(defn build-body
  "Description:
      Body text for message box. Create component with message.
   Example:
      (build-body 'My message')
   "
  [txt] (c/label :text txt 
                 :background (new Color 0 0 0 0)
                 :border (b/empty-border :left 5 :right 5 :bottom 2)))


(defn build-bottom-ico-btn
  "Description:
      Icon btn for message box. Create component with icon btn on bottom.
   Layered should be atom.
   Example:
      (build-bottom-ico-btn icon/loupe-grey-64-png icon/loupe-blue1-64-png 23 (fn [e] (alert 'Wiadomosc')))
   Needed:
      Import jarman.dev-tools
      Function need stool/image-scale function for scalling icon
      Function need hand-hover-on function for hand mouse effect
   "
  [ic ic-h layered-pane & args] (c/label :icon (stool/image-scale ic (if (> (count args) 0) (first args) 28))
                                    :background (new Color 0 0 0 0)
                                    :border (b/empty-border :left 3 :right 3)
                                    :listen [:mouse-entered (fn [e] (do
                                                                      (c/config! e :icon (stool/image-scale ic-h (if (> (count args) 0) (first args) 28)) :cursor :hand)
                                                                      (.repaint layered-pane)))
                                             :mouse-exited (fn [e] (do
                                                                     (c/config! e :icon (stool/image-scale ic (if (> (count args) 0) (first args) 28)))
                                                                     (.repaint layered-pane)))
                                             :mouse-clicked (if (> (count args) 1) (second args) (fn [e]))]))




(def message
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
  (fn [data]
   ;;  (println "Invoked alert")
    (let [font-c "#000"
          bg-c "#fff"
          header (rift (:header data) "Information")
         ;;  header (if (= (contains? data :header) true) (:header data) "Information")
          body   (rift (:body data) "Template of information...")
         ;;  body   (if (= (contains? data :body) true) (:body data) "Template of information...")
          layered-pane ((state/state :alert-manager) :get-space)
          close [(build-bottom-ico-btn icon/loupe-grey-64-png icon/loupe-blue1-64-png layered-pane 23
                                       (fn [e] (gcomp/popup-info-window header body layered-pane)))
                 (build-bottom-ico-btn icon/x-grey-64-png icon/x-blue1-64-png layered-pane 23
                                       (fn [e] (let [to-del (.getParent (.getParent (seesaw.core/to-widget e)))] ((state/state :alert-manager) :rm-obj to-del))))]
          [t b l r] (try
                      (map #(Integer/parseInt %) (rift (gtool/get-comp :message-box :border-size)))
                      (catch Exception e [1 1 1 1]))]

      (mig-panel
       :id :alert-box
       :constraints ["wrap 1" "0px[fill, grow]0px" "0px[20]0px[30]0px[20]0px"]
       :background bg-c
       :border (b/line-border :top t :bottom b :left l :right r :color (rift (gtool/get-comp :message-box :border-color) "#fff"))
       :bounds [680 480 300 75]
       :items [[(c/flow-panel
                 :align :left
                 :background (new Color 0 0 0 0)
                 :items [(build-ico icon/alert-64-png)
                         (build-header (gtool/str-cutter header))])]
               [(build-body (gtool/str-cutter body))]
               [(c/flow-panel
                 :align :right
                 :background (new Color 0 0 0 1)
                 :items (if (= (contains? data :btns) true) (concat close (get data :btns)) close))]]
       :listen [:mouse-entered (fn [e])]))))



(def alerts-rebounds-f
  "Description:
      Rebound message components and resize them.
   Needed:
      Function need get-elements-in-layered-by-id function for get all same element (here message boxs)
      Function need getHeight function for quick getting height size
   "
  (fn [e] (let [list-of-alerts (gtool/get-elements-in-layered-by-id e "alert-box")
                bound-x (if list-of-alerts (- (.getWidth (c/to-root (seesaw.core/to-widget e))) (.getWidth (first list-of-alerts)) 20) 0)
                height 120]
            (if list-of-alerts (doseq [[n elem] (map-indexed #(vector %1 %2) list-of-alerts)]
                                 (c/config! elem :bounds [bound-x (- (- (.getHeight (c/to-root (seesaw.core/to-widget e))) height) (* 80 n)) 300 75]))))))



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
                                    (doall (map (fn [item] (if (identical? (c/config item :id) :alert-box) (.remove layered-pane item))) (seesaw.util/children layered-pane)))
                                    ;; Add alerts
                                    (doall (map (fn [item] (if (= (:visible item) true) (.add layered-pane (:component item) (new Integer 15)))) @alerts-storage))
                                    ;;  Rebounds message space
                                    (alerts-rebounds-f layered-pane)
                                    ;;  Repainting app
                                    (.repaint layered-pane)))
  ([layered-pane alerts-storage id-to-remove] (do
                                                 ;;  Remove alerts
                                                 (doall (map (fn [i] (if (or (identical? (c/config i :id) :alert-box)
                                                                             (identical? (c/config i :id) id-to-remove))
                                                                       (.remove layered-pane i))) (seesaw.util/children layered-pane)))
                                                 ;; Add alerts
                                                 (doall (map (fn [item] (if (= (:visible item) true) (.add layered-pane (:component item) (new Integer 15)))) @alerts-storage))
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
       => in storage [... {:id 0 :data {:header 'Hello' :body 'World!'} :component (c/label :text 'Hello World!') :timelife 3 storage}]
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
        bord     (b/empty-border :left 5)
        header   (if (= (contains? data :header) true) (get data :header) "Information")
        body     (if (= (contains? data :body) true) (get data :body) "Template of information...")
        comp-header (c/label :size [w :by h] :background (new Color 0 0 0 0) :border bord :text header :font (gs/getFont :bold))
        comp-body   (c/label :size [w :by h] :background (new Color 0 0 0 0) :border bord :text body)
        comp        (mig-panel
                     :focusable? true
                     :constraints ["wrap 1" "0px[grow, fill]0px" "0px[fill]0px"]
                     :background bg-c
                     :border (b/line-border :bottom 2 :color "#333")
                     :items [[comp-header] [comp-body]])
        onClick     (fn [e] (gcomp/popup-info-window header body layered-pane))
        comp        (c/config! comp
                               :listen [:mouse-clicked (fn [e] (onClick e))
                                        :mouse-entered (fn [e] (.requestFocus (c/to-widget e)))
                                        :mouse-exited  (fn [e] (.requestFocus (c/to-root e)))
                                        :focus-gained  (fn [e] (c/config! comp :background hover-c :cursor :hand))
                                        :focus-lost    (fn [e] (c/config! comp :background bg-c))
                                        :key-pressed   (fn [e] (if (= (.getKeyCode e) java.awt.event.KeyEvent/VK_ENTER) (onClick e)))])]
    comp))


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
        btn-bg-c "#fff"
        btn-bg-c-hover "#ddd"
        items  (reverse (map (fn [item] (history-alert (get item :data) layered-pane [w 30])) @alerts-storage))
        container (mig-panel
                   :constraints ["wrap 1" "0px[grow, fill]0px[fill]0px" "0px[grow, top]0px[fill]0px"]
                   :id :all-alerts
                   :items [[(gcomp/scrollbox
                             (c/vertical-panel :items items)
                             :args [:hscroll :never
                                    :border nil
                                    :listen [:mouse-motion (fn [e] (.repaint (c/select (c/to-root e) [:#all-alerts])))
                                             :mouse-wheel-moved (fn [e] (.repaint (c/select (c/to-root e) [:#all-alerts])))]])]
                           [(gcomp/button-basic
                             "Clear all message" 
                             :flip-border true
                             :onClick (fn [e]
                                        (rmallAlert alerts-storage layered-pane);;  remove all history
                                        (refresh-alerts layered-pane alerts-storage :all-alerts);;  refresh GUI with remove element with id :all-alertsts
                                        (.dispose (c/to-frame e)))
                             :args [:icon (stool/image-scale icon/basket-blue1-64-png ico-size)])]])]
    (gcomp/popup-window {:view container :window-title "Alerts history" :size [w h] :relative layered-pane})))


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
        (= action :set)           (let [[data timelife] param] (addAlertTimeout data (message data) timelife alerts-storage layered-pane))
        (= action :message)       (let [[alerts-controller] param] (message alerts-controller))
        (= action :rm)            (let [[id] param] (rmAlert id alerts-storage layered-pane))
        (= action :rm-obj)        (let [[obj] param] (rmAlertObj obj alerts-storage layered-pane))
        (= action :clear)         (rmallAlert alerts-storage layered-pane)
        (= action :count-all)     (count @alerts-storage)
        (= action :count-active)  (count (filter (fn [item] (if (= (get item :visible) true) item)) @alerts-storage))
        (= action :count-hidden)  (count (filter (fn [item] (if (= (get item :visible) false) item)) @alerts-storage))
        (= action :show)          (do (refresh-alerts layered-pane alerts-storage :all-alerts) (all-messages-window layered-pane alerts-storage))
        (= action :hide)          (refresh-alerts layered-pane alerts-storage :all-alerts)))))

(state/set-state :alert-manager nil)
