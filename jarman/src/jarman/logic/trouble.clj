;;; TODO 
(ns jarman.logic.trouble
  (:require
   [clojure.string :as string]
   [jarman.tools.lang :refer :all]))



(def troubles
  {:no-connection-database
   [:div
    [:h1 "About"]
    [:p "No connection to database"]]
   :bad-spec-connection-map
   [:div
    [:h1 "About"]
    [:p "Database connection map has some problems"]]
   :break-database-structure
   [:div
    [:h1 "About"]
    [:p "Your database has bad structure, which not compatible with Jarman, please call to tech support"]]})

;; (defn compile-hiccup [hiccup-struct]
;;   (case (first hiccup-struct)
;;       :h1 
;;       :p
;;       :div ))
;; (defn get-trouble [k]
;;   (get troubles k nil))
