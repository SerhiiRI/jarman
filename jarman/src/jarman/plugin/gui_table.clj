(ns jarman.plugin.gui-table
  (:refer-clojure :exclude [update])
  (:require
   ;; Clojure toolkit
   [clojure.string :as string]
   [clojure.java.io :as io]
   [seesaw.core :as c]
   [seesaw.swingx :as swingx]
   ;; for datascript
   [clojure.set :as set]
   [datascript.core :as d]
   ;; jarman tool
   [jarman.logic.metadata :as db-meta]))


(defn get-refs [table-name]
  (let [[c-name repr] (first (d/q '[:find (pull ?f [:table_name]) ?r
                             :in $ ?n 
                             :where
                             [?t :table_name ?n]
                             [?t :columns ?c]
                             [?c :column/foreign-keys ?f]
                             [?c :column/representation ?r]] @db-meta/db table-name))]
    {(keyword (:table_name c-name)) repr}))


(defn gui-table-model-columns [table-list colmn-list]
  (let [refs (into {} (map (fn [table-name] (get-refs (name table-name))) table-list))
        ;; return from db model with id of columns in colmn-list [{:key :user.login, :text 5} ....]
        id-map (reduce (fn [acc x] (if (nil? (some #{(:v x)} colmn-list)) acc (conj acc {:key (:v x) :text (:e x)}))) [] (d/datoms @db-meta/db :eavt))
        ;; select from db columns by id-datom [{:column/representation "Permisssion name" :column/field-qu...} ....]
        data-map (d/pull-many @db-meta/db [:column/representation :column/field-qualified] (map (fn [f] (:text f)) id-map))
        ;; get from data-map representations and convert to our format [{:text "id_permission Permission name"}....]
        repr (reduce (fn [acc x] (conj acc {:text
                                            (let [col-repr (:column/representation x)
                                                  t-name (keyword (first (string/split (name  (:column/field-qualified x)) #"\.")))
                                                  n (t-name refs)]
                                              (str n (if-not (empty? n ) " ") col-repr))})) [] data-map)
        model-colmns (map (fn [a b] (merge a b)) id-map repr)]
    model-colmns))

(defn- gui-table-model [model-columns data-loader]
  (fn [] [:columns model-columns :rows (data-loader)]))

;; (seesaw.dev/show-options (swingx/table-x))

(defn- gui-table [table-model]
  (fn [listener-fn]
    (let [table-model (table-model)
          TT (swingx/table-x :model table-model :border (seesaw.border/empty-border))]
      (if (empty? (table-model)) (println "EMPTY DB:"))
      (c/listen TT :selection (fn [e] (listener-fn (seesaw.table/value-at TT (c/selection TT)))))
      (c/config! TT :horizontal-scroll-enabled? true)
      (c/config! TT :show-grid? false)
      (c/config! TT :show-horizontal-lines? true)
      (c/scrollable TT :hscroll :as-needed
                    :vscroll :as-needed
                    :border (seesaw.border/line-border :thickness 0 :color "#fff")))))

(defn create-table [configuration toolkit-map]
  (let [view (:view-columns configuration) table-list (:tables configuration)]
    ;; (println "\nView table\n" view)
    (if (and view table-list)
      (let [model-columns (gui-table-model-columns table-list view)
            table-model (gui-table-model model-columns (:select toolkit-map))]

        (println "TABLE_MODEL______" (table-model) )
        {:table-model table-model
         :table (gui-table table-model)}))))


(comment (gui-table-model-columns ["user" "permission"]
                                  [:user.id :user.login :user.password :user.first_name :user.last_name :permission.id :permission.permission_name :permission.configuration]))


