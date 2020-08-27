(ns jarman.logic.binary-tree
  (:refer-clojure :exclude [update])
  (:require
   [jarman.logic.sql-tool :as toolbox :include-macros true :refer :all]
   [clojure.data :as data]
   [clojure.string :as string]
   [clojure.java.jdbc :as jdbc]))




;; (defrecord Node [left right])

(defmacro in? [col e]
  `(some (fn [el#] (= ~e el#)) ~col))

(defn in [col e]
  (in? col e))

(defn node [l r]
  (fn [k]
    (condp in k
         [:left :l 'l] l 
         [:right :r 'r] r)))

(defn mnode [& body]
  (fn [k]
    (nth body k nil)))

