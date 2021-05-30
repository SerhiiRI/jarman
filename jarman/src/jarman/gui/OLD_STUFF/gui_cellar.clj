
;; (def refresh-layered-for-tables
;;   "Description
;;      Refresh bounds of DB View JLayredPane.
;;    "
;;   (fn [] (do (if (contains? @views :layered-for-tabs)
;;                (let [max-w (apply max (map (fn [item]  (+ (.getX (config item :bounds)) (.getWidth  (config item :bounds)))) (seesaw.util/children (get-in @views [:layered-for-tabs :component]))))
;;                      parent-w (getWidth (.getParent (get-in @views [:layered-for-tabs :component])))
;;                      max-h (apply max (map (fn [item]  (+ (.getY (config item :bounds)) (.getHeight  (config item :bounds)))) (seesaw.util/children (get-in @views [:layered-for-tabs :component]))))
;;                      parent-h (getHeight (.getParent (get-in @views [:layered-for-tabs :component])))]
;;                  (do (.setPreferredSize (get-in @views [:layered-for-tabs :component]) (new Dimension (if (> parent-w max-w) parent-w max-w) (if (> parent-h max-h) parent-h max-h)))
;;                      (.setSize (get-in @views [:layered-for-tabs :component]) (new Dimension (if (> parent-w max-w) parent-w max-w) (if (> parent-h max-h) parent-h max-h)))))

;;                (.setPreferredSize (get-in @views [:layered-for-tabs :component]) (new Dimension
;;                                                                                       (getWidth  (.getParent (get-in @views [:layered-for-tabs :component])))
;;                                                                                       (getHeight (.getParent (get-in @views [:layered-for-tabs :component])))))))))
