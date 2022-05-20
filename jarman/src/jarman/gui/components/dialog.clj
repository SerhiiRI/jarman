(ns jarman.gui.components.dialog
  (:require
   [seesaw.core :as c]
   [jarman.gui.gui-tools :as gtool]
   [jarman.gui.components.database-table :as table]))

(defn popup-table [& {:keys [tables columns on-select custom-renderers data select-by-index] :or {data [] on-select (fn [v])} :as params}]
  (let [dialog (c/custom-dialog :modal? true :width 1100 :height 600 :title "Select component")
        table (as-> params P
                ;; (assoc P :on-select (fn [table-model] (on-select table-model) (c/return-from-dialog dialog table-model)))
                (apply table/database-table (apply concat P)))
        key-p (seesaw.mig/mig-panel
               :constraints ["wrap 1" "0px[grow, fill]0px" "5px[fill]5px[grow, fill]0px"]
               ;;  :border (sborder/line-border :color "#888" :bottom 1 :top 1 :left 1 :right 1)
               :items [[(c/label :text (gtool/get-lang :tips :press-to-search) :halign :center)]
                       [(c/scrollable table :hscroll :as-needed
                                      :vscroll :as-needed
                                      :border (seesaw.border/line-border :thickness 0 :color "#fff"))]])
        ;; key-p (key-tut/get-key-panel \q (fn [jpan] (.dispose (c/to-frame jpan))) key-p)
        ]
    (c/config! dialog :content key-p :title (gtool/get-lang :tips :related-popup-table))
    (when select-by-index
      (table/scrollAndSelectSpecificRow! table select-by-index))
    (c/listen table :selection (fn [table-model] (on-select table-model) (c/return-from-dialog dialog table-model)))
    (c/show! dialog)))


(comment
  (require '[jarman.gui.components.database-table :as table])
  (require '[jarman.logic.metadata :as metadata])
  (require '[jarman.logic.connection :as db])
  (require '[jarman.logic.sql-tool :refer [select! update! delete! insert!]])
  
  ;; ===========
  ;; RANDOM DATA 
  (def rand-data
    (->> (cycle (vec (db/query (select!
                                {:limit 1
                                 :table_name :user
                                 :column [:#as_is :user.login :user.password :user.first_name :user.last_name :user.configuration :user.id_profile :profile.name :profile.configuration]
                                 :inner-join :user->profile}))))
         (take 100)
         (map (fn [x] (assoc x
                            :user.login (apply str (take 40 (repeatedly #(char (+ (rand 26) 65)))))
                            :user.password (rand-nth [true false])
                            :user.first_name (* (rand) (rand-int 10000)))))))

  ;; ==============
  ;; TABLE INSTANCE

  (popup-table
   :on-select (fn [e] (println e))
   :tables  [:user :profile]
   :columns [:user.login :user.password :user.first_name :user.last_name :profile.name]
   :data rand-data))
