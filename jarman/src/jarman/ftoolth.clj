  (ns jarman.ftoolth
  (:use
   seesaw.chooser
   seesaw.dev
   seesaw.core
   seesaw.border
   seesaw.mig)
  (:require [clojure.string :as string]
            [jarman.dev-tools :as dev]
            [jarman.icon-library :as icons]))


(defn mig
  "Description:
      Simple mig container builder.
      (mig component1 component2-f \"some-text\" {wrap})
   Example: 
      (mig btn)
      (mig btn (btn2 {:foreground \"#c3a\"}))
      (mig btn (btn2 {:foreground \"#c3a\"})  \"Dupa\")
      (mig btn (btn2 {:foreground \"#c3a\"})  \"Dupa\" {:wrap \"wrap 1\"})"
  [& elements]
  (fn [] (let [confmap {:wrap "" :h "0px[grow, fill]0px" :v "0px[grow, fill]0px" :args []}
               elements-list (if (map? (last elements))
                               (butlast elements)
                               elements)
               conf (if (map? (last elements))
                      (merge confmap (last elements))
                      confmap)]
           (apply seesaw.mig/mig-panel
                  :constraints [(get conf :wrap)
                                (get conf :h)
                                (get conf :v)]
                  :items (map (fn [item]
                                (cond
                                  (string? item) [(label :text item)]
                                  (fn? item)     [(item)]
                                  :else          [item]))
                              elements-list)
                  (get conf :args)))))

(defn builder-panel
  "Description:
       Quick panel builder for components builders, builded components and simple text
       If map is first then create new panel with merge styles
   Example: 
       (def vpanel (builder-panel seesaw.core/vertical-panel {})) -> Create vertical panel
       (def vpanel-dark (vpanel {:background \"333\"})) -> Create vertical panel used another and add new state to old params
   "
  [panel state]
  (fn [& elements]
    (if (map? (first elements))
      (builder-panel panel (merge state (first elements)))
      (let [confmap state
            elements-list (if (map? (last elements))
                            (butlast elements)
                            elements)
            conf (if (map? (last elements))
                   (merge confmap (last elements))
                   confmap)]
        (apply panel
               :items (map (fn [item]
                             (cond
                               (string? item) (label :text item)
                               (fn? item)     (item)
                               :else          item))
                           elements-list)
               :background "#eee"
               (vec (mapcat seq conf)))))))

(defn component
  "Description:
       Builder for builde builders uses builded builder befor
   Example: 
       (def builder-btn (partial component seesaw.core/button)) -> Create button builder
       (def btn (builder-btn {:text \"\"})) -> Create button builder with state
       (def btn (builder-btn {:text \"Test\"})) -> Create button builder with used another and add new state to old params
   "
  ([kind state]
   (let [state state]
     (fn ([] (apply kind (vec (mapcat seq state))))
       ([new-state]
        (if (= state new-state)
          (apply kind (vec (mapcat seq state)))
          (component kind (merge state new-state))))))))



(def flow (builder-panel seesaw.core/flow-panel {}))
(def vpanel (builder-panel seesaw.core/vertical-panel {}))
(def hpanel (builder-panel seesaw.core/horizontal-panel {}))

(def vpanel-dark (vpanel {:background "#333"}))
(def builder-btn (partial component seesaw.core/button))
(def builder-txt (partial component seesaw.core/label))
(def btn (builder-btn {:text ""}))
(def txt (builder-txt {:text ""}))
(def ico (txt {:icon (seesaw.icon/icon (clojure.java.io/file "\\jarman\\icons\\1-64x64.png"))
               :border (empty-border :thickness 5)}))


(defn state-merge
  [key state new-state]
  (merge state (assoc {} key (merge (get state key) new-state))))

;; State component
(defn increment-btn
  "Description:
       Component (function) with button + and - who edit number label
   "
  ([state] (let [increment-btn-state state]
             (fn
               ([] (let [state state]
                     (hpanel
                      (btn (get state :add))
                      (txt (get state :number))
                      (btn (get state :take)))))
               ([new-state] (let [state (merge state new-state)]
                              (hpanel
                               (btn (get state :add))
                               (txt (get state :number))
                               (btn (get state :take)))))))))

(defn get-element
  "Description:
       Get nth child in parent element (created for events)
   Example: 
       (get-element e 1) -> get sec element
   "
  [event nth_child]
  (nth (seesaw.util/children (.getParent (seesaw.core/to-widget event))) nth_child))

;; Component inc/dec
(def inc (fn [] (let [component (increment-btn {:add {:text "+" :listen [:mouse-clicked (fn [e] (config! (get-element e 1) :text (+ (Integer. (config (get-element e 1) :text)) 1)))]}
                                                :take {:text "-" :listen [:mouse-clicked (fn [e] (config! (get-element e 1) :text (- (Integer. (config (get-element e 1) :text)) 1)))]}
                                                :number {:text 0}})]
                             component)))

;; Działający przykład pobrania rodzica
;; (def test (flow (label :text "Hello" :listen [:mouse-clicked (fn [e] (config! (.getParent (seesaw.core/to-widget e)) :background "#333"))]))) 


(def ico-b (ico {:border (line-border :bottom 4 :color "#eee") :size [42 :by 42] :background "#999"}))

(def left-menu
  (vpanel 
   (ico-b {:icon (dev/image-scale icons/agree-64-png 60)})
   (ico-b {:icon (dev/image-scale icons/agree-64-png 60)})
   (ico-b {:icon (dev/image-scale icons/agree-64-png 60)})
   (ico-b {:icon (dev/image-scale icons/agree-64-png 60)})
   ))

(def btn-category (fn []
                    (btn {:text "Funkcjonalność"
                          :background "#ddd"
                          :size [150 :by 42]
                          :border (line-border :bottom 4 :color "#eee")})))

(def txt-title-of-btn (fn [] 
                        (txt {:text "Tytuł kategorii"
                              :background "#eee"
                              :halign :center
                              :size [150 :by 42]
                              :border (line-border :bottom 3 :color "#999")})))

(-> (doto (seesaw.core/frame
           :title "DEBUG WINDOW" :undecorated? false
           :minimum-size [800 :by 400]
           :content (hpanel
                     (mig
                      ;; (vpanel-dark
                      ;; ;;  (ico {:icon (seesaw.icon/icon (clojure.java.io/file "C:\\Aleks\\Github\\jarman\\jarman\\icons\\main\\1-64x64.png"))})
                      ;;  (ico {:icon (dev/image-scale icons/agree-64-png 80)})
                      ;;  (ico {:icon (dev/image-scale icons/alert-64-png 80)}))
                      ;; (vpanel
                      ;;  (txt {:text "Inc and Dec" :halign :center})
                      ;;  (inc))
                      left-menu
                      (builder-txt {:border (line-border :left 1 :color "#999")})
                      (builder-txt {:border (line-border :left 1 :color "#999")})
                      (vpanel
                       (txt-title-of-btn)
                       (btn-category)
                       (btn-category)
                       (btn-category)
                       (btn-category))
                      (builder-txt {:border (line-border :left 1 :color "#eee")})
                      (flow (txt {:text "Kontent" :halign :center}))
                      {:h "0px[42]0px[4]0px[150, fill, center]0px[4]0px[grow, fill]0px"
                       :v "0px[grow, fill]0px"})
                     ))
      (.setLocationRelativeTo nil)) pack! show!)



;; (show-options (seesaw.core/button))
