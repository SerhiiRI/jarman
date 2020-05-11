(ns jarman.ftoolth
  (:use
   seesaw.chooser
   seesaw.dev
   seesaw.core
   seesaw.border
   seesaw.mig))

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
  (fn [] (let [elements-list (if (map? (last elements))
                         (butlast elements)
                         elements)
         conf (if (map? (last elements))
                (last elements)
                {:wrap ""})]
     (seesaw.mig/mig-panel
      :constraints [(get conf :wrap)
                    "0px[grow, fill]0px"
                    "0px[grow, fill]0px"]
      :items (map (fn [item]
                    (cond
                      (string? item) [(label :text item)]
                      (fn? item)     [(item)]
                      :else          [item]))
                  elements-list)))))

; TODO: upgrade is needed for more component type
(defn component-button
  ([state]
   (let [state state]
     (fn ([](apply seesaw.core/button (vec (mapcat seq state))))
         ([new-state]
          (if (= state new-state)
            (apply seesaw.core/button (vec (mapcat seq state)))
            (component-button (merge state new-state))))))))


(def btn (component-button {:text "The button" :foreground "#ff0000" :background "#000000"}))
; (def btn2 (btn {:text "A gunwo" :foreground "#ffffff" :border (line-border :thickness 2 :color "#2fc")}))

(def debug-frame (seesaw.core/frame
                  :title "DEBUG WINDOW" :undecorated? false
                  :minimum-size [400 :by 400]
                  :content (mig btn (btn2 {:foreground "#c3a"})  "Dupa")))

(-> (doto debug-frame (.setLocationRelativeTo nil)) pack! show!)