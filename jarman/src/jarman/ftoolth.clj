(ns jarman.ftoolth
  (:use
   seesaw.chooser
   seesaw.dev
   seesaw.core
   seesaw.border
   seesaw.mig)
  (:require [clojure.string :as string]
            [jarman.dev-tools]))

; (defn component
;   ([prop state element]
;    (let [propr prop
;          state state]
;      (fn([]state)
;        ([new-state & :keys [comparator-f] :or {comparator-f #'=}]
;         (let [comparator-f (or comparator-f #'=)]
;          (if (comparator-f state new-state)
;            state
;            (component propr new-state))))))))

; (defn component-button
;   ([state & [comparator-f]]
;    (let [state state]
;      (fn ([](button state))
;          ([new-state]
;           (if ( state new-state)
;             (button state)
;             (component (merge state new-state))))))))

; (def button (component {:onclik}))
; (button) => Jbutton 
; (def button1 (button {:icon "suka"})) => Jbutton
; (fn
;   ([] (button state))
;   ([new-state]
;    (if (= state new-state)
;      (button state)
;      (component (merge state new-state)))))

; (defmacro defelement [component-name & body]
;   (let [ps (butlast body)
;         cfg ('conf (apply array-map ps))
;         c (last body)]
;     `(let [~'conf ~cfg]
;        (fn ([] ~c)
;           ([~'new-config]
;            (let [~'conf (merge ~'conf ~'new-config)]
;              (fn [] ~c)))))))

; (defelement button
;   conf {:text "DUPA"}
;   (seesaw.core/button :text (conf :text)
;                       :icon nil))

; (defmacro defelement** [& body]
;   (let [ps (butlast body)
;         cfg (get (apply array-map ps) 'conf)
;         c (last body)]
;     `(let [conf (ref ~cfg)]
;        (fn ([] ~c)
;           ([~'new-config] ~c)))))

; (defelement* button
;   conf {:text "DUPA"}
;   (seesaw.core/button :text   (conf :text)
;                       :icon nil))


; (mig _m01
;  (label  _01_01)
;  button-agree
;  (label  _02_01)
;  (button-agree {:text "Potwierdzam"}))


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
  (fn [] (let [confmap {:wrap "" :h "0px[grow, fill]0px" :v "0px[grow, fill]0px"}
               elements-list (if (map? (last elements))
                               (butlast elements)
                               elements)
               conf (if (map? (last elements))
                      (merge confmap (last elements) )
                      confmap)]
           (seesaw.mig/mig-panel
            :constraints [(get conf :wrap)
                          (get conf :h)
                          (get conf :v)]
            :items (map (fn [item]
                          (cond
                            (string? item) [(label :text item)]
                            (fn? item)     [(item)]
                            :else          [item]))
                        elements-list)))))

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
                       :border (empty-border :thickness 5)
                       }))

(-> (doto (seesaw.core/frame
           :title "DEBUG WINDOW" :undecorated? false
           :minimum-size [400 :by 400]
           :content ((mig
                      (vpanel-dark
                       (ico {:icon (seesaw.icon/icon (clojure.java.io/file "C:\\Aleks\\Github\\jarman\\jarman\\icons\\main\\1-64x64.png"))})
                       (ico {:icon (seesaw.icon/icon (clojure.java.io/file "C:\\Aleks\\Github\\jarman\\jarman\\icons\\main\\2-64x64.png"))}))
                      (builder-txt {:border (line-border :left 1 :color "#666")})
                      (flow (txt {:text "Content" :halign :center}))
                      {:h "0px[64]0px[1]0px[grow, fill]0px"
                       :v "0px[grow, fill]0px"}))) 
      (.setLocationRelativeTo nil)) pack! show!)


;; (show-options (seesaw.core/vertical-panel))
