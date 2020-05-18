(ns jarman.ftoolth
  (:use
   seesaw.chooser
   seesaw.dev
   seesaw.core))

(defn component
  ([prop state element]
   (let [propr prop
         state state]
     (fn([]state)
       ([new-state & [comparator-f]]
        (let [comparator-f (or comparator-f #'=)]
         (if (comparator-f state new-state)
           state
           (component propr new-state))))))))

(defn component
  ([prop state element]
   (let [propr prop
         state state]
     (fn([]state)
       ([new-state & [comparator-f]]
        (let [comparator-f (or comparator-f #'=)]
         (if (comparator-f state new-state)
           state
           (component propr new-state))))))))

(defmacro defelement [component-name & body]
  (let [ps (butlast body)
        cfg ('conf (apply array-map ps))
        c (last body)]
    `(let [~'conf ~cfg]
       (fn ([] ~c)
          ([~'new-config]
           (let [~'conf (merge ~'conf ~'new-config)]
             (fn [] ~c)))))))

(def access-button (defelement access-przycisk
   conf {:text "DUPA" }
   (seesaw.core/button :text (conf :text)
                       :icon nil)))




(access-button)



(defmacro defelement** [& body]
  (let [ps (butlast body)
        cfg (get (apply array-map ps) 'conf)
        c (last body)]
    `(let [conf (ref ~cfg)]
       (fn ([] ~c)
          ([~'new-config] ~c)))))

(defelement* button
  conf {:text "DUPA"}
  (seesaw.core/button :text   (conf :text)
                      :icon nil))


(div _m01
     (div(label  _01_01)
         button-agree
         (label  _02_01))

     (button-agree {:text "Potwierdzam"}))
(button-agree {:text "Potwierdzam"}))
(button-agree {:text "Potwierdzam"}))
(button-agree {:text "Potwierdzam"}))




(dnf )

(button :listen [:onClick (fn [x] 123)])



(defcomponet
  Prop {fdjsalfj sa}
  State (ref {})
  (div _m01
     (div(label  _01_01)
         button-agree
         (label  _02_01))
 (button-agree {:text "Potwierdzam"})))


(component1
 (defcomponent2 fdsalfdsamfalks
   fdsafas))
