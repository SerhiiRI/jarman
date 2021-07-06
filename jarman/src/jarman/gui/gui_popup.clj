(ns jarman.gui.gui-popup
  (:use seesaw.dev
        seesaw.style
        seesaw.mig
        seesaw.font)
  (:require
   [seesaw.core :as c]
   [seesaw.border :as b]
   [clojure.string :as string]
   [seesaw.util :as u]
   [jarman.gui.gui-tools :as gtool]
   [jarman.tools.swing :as stool]
   [jarman.resource-lib.icon-library :as icon]
   [jarman.logic.state :as state]
   [jarman.gui.gui-components :as gcomp]))


;; (defn ontop-panel
;;   [popup-menager storage-id & {:keys [size title body] :or {size [600 400] title "Popup" body (c/label)}}]
;;   (let [last-x (atom 0)
;;         last-y (atom 0)
;;         w (first size)
;;         h (second size)
;;         title-bar
;;         (mig-panel
;;          :constraints ["" "10px[grow]0px[30, center]0px" "5px[]5px"]
;;          :background "#ddd" ;;"#eee"
;;          :items (gtool/join-mig-items
;;                  (c/label :text title)
;;                  (c/label :icon (stool/image-scale icon/up-grey1-64-png 25)
;;                           :listen
;;                           [:mouse-clicked
;;                            (fn [e] (cond (> (.getHeight (gtool/getParent (gtool/getParent e))) 50)
;;                                          (c/config! (gtool/getParent (gtool/getParent e))
;;                                                     :bounds  [(.getX (gtool/getParent (gtool/getParent e)))
;;                                                               (.getY (gtool/getParent (gtool/getParent e)))
;;                                                               w 30])
;;                                          :else (c/config! (gtool/getParent (gtool/getParent e))
;;                                                           :bounds [(.getX (gtool/getParent (gtool/getParent e)))
;;                                                                    (.getY (gtool/getParent (gtool/getParent e)))
;;                                                                    w h])))
;;                            :mouse-entered (fn [e] (c/config! e :cursor :hand))])
;;                  (c/label :icon (stool/image-scale icon/x-blue2-64-png 20)
;;                           :listen [:mouse-clicked (fn [e] ((@popup-menager :remove) storage-id))
;;                                    :mouse-entered (fn [e] (c/config! e :cursor :hand))])))
;;         content (mig-panel
;;                  :constraints ["wrap 1" "0px[grow, center]0px" "0px[grow, center]0px"]
;;                  :background "#fff"
;;                  :items (gtool/join-mig-items body))
;;         components (gtool/join-mig-items
;;                     title-bar content)
;;         main-bounds (gtool/middle-bounds (first @(state/state :atom-app-size)) (second @(state/state :atom-app-size)) w h)
;;         main-border (b/line-border :thickness 2 :color (gtool/get-color :decorate :gray-underline))]
;;     (mig-panel
;;      :constraints ["wrap 1" "0px[fill, grow]0px" "0px[fill, center]0px[fill, grow]0px"]
;;      :bounds main-bounds
;;      :border main-border
;;      :background "#fff"
;;      :id storage-id
;;      :visible? true
;;      :items components
;;      :listen [:mouse-clicked (fn [e] ((@popup-menager :move-to-top) storage-id))
;;               :mouse-dragged (fn [e] (do
;;                                        (if (= @last-x 0) (reset! last-x (.getX e)) nil)
;;                                        (if (= @last-y 0) (reset! last-y (.getY e)) nil)
;;                                        (let [bounds (c/config e :bounds)
;;                                              pre-x (- (+ (.getX bounds) (.getX e)) @last-x)
;;                                              pre-y (- (+ (.getY bounds) (.getY e)) @last-y)
;;                                              x (if (> pre-x 0) pre-x 0)
;;                                              y (if (> pre-y 0) pre-y 0)
;;                                              w (.getWidth  bounds)
;;                                              h (.getHeight bounds)]
;;                                          (c/config! e :bounds [x y w h]))))])))

(def dialog
  (fn [{:keys [title content root]
        :or {title   "Dialog"
             content (c/label :text "Dialog template")
             root    (state/state :app)}}]
    (-> (c/custom-dialog
         :title title
         :modal? true
         :resizable? false
         :content content
         :parent (.getParent root))
        c/pack! c/show!)))

(def dialog-buttons
  (fn [{:keys [title ask buttons size root]
        :or {title "Multi button dialog"
             ask "Dialog return true or false if default"
             buttons [{:title  (gtool/get-lang-btns :yes)
                       :return true}
                      {:title  (gtool/get-lang-btns :no)
                       :return false}]
             size [400 300]
             root (state/state :app)}}]
    (dialog {:title title
             :root root
             :content (mig-panel
                       :size [(first size) :by (second size)]
                       :constraints ["" "0px[fill, grow]0px" "0px[grow, center]0px"]
                       :items (gtool/join-mig-items
                               (mig-panel
                                :constraints ["wrap 1" "0px[fill, grow]0px" "0px[]0px"]
                                :items (gtool/join-mig-items
                                        (gcomp/textarea ask
                                                        :halign :center
                                                        :font (gtool/getFont 14)
                                                        :border (b/empty-border :thickness 12))
                                        (mig-panel
                                         :constraints ["" "10px[fill, grow]10px" "10px[]10px"]
                                         :items (gtool/join-mig-items
                                                 (doall
                                                  (map #(gcomp/button-basic
                                                         (:title %)
                                                         :onClick (fn [e] (c/return-from-dialog e (:return %))))              
                                                       buttons))))))))})))

(def dialog-ok
  (fn [{:keys [title button-title ask size root]
        :or {title "Dialog OK"
             button-title "OK"
             ask "Just click ok"
             size [400 300]
             root (state/state :app)}}]
    (dialog-buttons {:title title
                     :buttons [{:title button-title
                                :return true}]})))

(do (doto (seesaw.core/frame
           :title "title"
           :undecorated? false
           :minimum-size [1000 :by 600]
           :content (c/label :text (str (dialog-ok {}))))
      (.setLocationRelativeTo nil) seesaw.core/pack! seesaw.core/show!))
;; ┌───────────────┐
;; │               │
;; │ Popup service │
;; │               │
;; └───────────────┘

;; (Defn create-popup-service
;;   [atom-popup-hook]
;;   (let [atom-popup-storage (atom {})
;;         z-index (atom 1000)
;;         JLP (gtool/getParent @atom-popup-hook)]
;;     (fn [action & {:keys [title, body, size] :or {title "Popup" body (c/label :text "Popup") size [600 400]}}]
;;       (let [unique-id (keyword (random-unique-id))
;;             template (fn [component] ;; Set to main JLayeredPane new popup panel
;;                        (do (swap! atom-popup-storage (fn [popups] (merge popups {unique-id component}))) ;; add new component to list with auto id
;;                            (swap! z-index inc)
;;                            (.add JLP (get-in @atom-popup-storage [unique-id]) (new Integer @z-index))
;;                            (.repaint JLP)))]
;;         (cond
;;           (= action :remove) (fn [id]
;;                                (let [elems-in-JLP (seesaw.util/children JLP)
;;                                      elems-count  (count elems-in-JLP)]
;;                                  (reset! atom-popup-storage (dissoc @atom-popup-storage id)) ;; Remove popup from storage
;;                                  (doall (map (fn [index] ;; remove popup from main JLayeredPane
;;                                                (cond (= (c/config (nth elems-in-JLP index) :id) id) (.remove JLP index)))
;;                                              (range elems-count)))
;;                                  (.repaint JLP)))  ;; Refresh GUI
;;           (= action :move-to-top) (fn [id] ;; Popup order, popup on top
;;                                     (let [elems-in-JLP (seesaw.util/children JLP)
;;                                           elems-count  (count elems-in-JLP)]
;;                                       (doall (map (fn [index] ;; Change popup order on JLayeredPane 
;;                                                     (cond (= (c/config (nth elems-in-JLP index) :id) id)
;;                                                           (.setLayer JLP (nth elems-in-JLP index) @z-index 0)))
;;                                                   (range elems-count)))
;;                                       (.repaint JLP)) ;; Refresh GUI
;;                                     )
;;           (= action :new-test)    (template (try (ontop-panel popup-menager unique-id)  (catch Exception e (println "\n"(.getMessage e)))))
;;           (= action :new-message) (template (ontop-panel popup-menager unique-id :title title :body body :size size))
;;           (= action :show)        (println @atom-popup-storage)
;;           (= action :get-atom-storage) atom-popup-storage
;;           (= action :ok)          (create-dialog-ok title body size)
;;           (= action :yesno)       (create-dialog-yesno title body size))))))
