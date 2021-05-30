





(defview seal
  [:user :dev :admin]
  (plug/jarman-table
   :name "Pushing seals"
   :tables [:seal]
   :view [:seal.seal_number :seal.to_date]
   :override-model [{:start-value :text-input} {:end-value :text-input}]
   :insert (fn [m] {:table-name :seal
                    :column-list [:seal.seal_number :seal.to_date]
                    :values (mapv #(vector % (:to-date m)) (range (:start-value m) (+ (:end-value m) 1)))}) 
   :update :none
   :delete :none
   :query {:column (as-is :seal.id :seal.seal_number :seal.to_date)})
  )

(defview seal
  [:user :dev :admin]
  (plug/jarman-table
   :name "Pushing seals"
   :tables [:seal]
   :view [:seal.seal_number :seal.to_date]
   :override-model [{:start-value :text-input} {:end-value :text-input}]
   :insert (fn [m] {:table-name :seal
                    :column-list [:seal.seal_number :seal.to_date]
                    :values (mapv (fn [x] (vector x (:to-date m))) (range (:start-value m) (+ (:end-value m) 1)))}) 
   :update :none
   :delete :none
   :query {:column (as-is :seal.id :seal.seal_number :seal.to_date)})
  )

