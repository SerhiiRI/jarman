(ns jarman.gui.clojureup.Lesson1
  (:import (java.awt Color))
  (:use seesaw.core)
  (:require [clojure.string :as string]))

;; REDUCE ------------------
;; Dwie pierwsze wartości stają się drugim argumentem trzeciej wartości,
;; argument pierwszy staje się akumulatorem i będzie przyjmował wynik działania
;; Jeśli miedzy funkcją a danymi wstawimy wartość, to ona staje się akumulatorem
;; i pierwszym argumentem
(reduce + 1 [2 4])
;; => 7
(defn make-html
  ([acc add]
   (cond
     (vector? add) (let [[tag val] add
                         tag (name tag)]
                     (format "%s<%s>%s<%s/>" acc tag val tag))

     (string? add) (str acc add)
     :else acc)))
(reduce make-html ["a" "b" nil [:div "hey"] "g" "e" "i"])
;; => "ab<div>hey<div/>gei"


(conj [1 3] [2 4])
;; => [1 3 [2 4]]

(reduce conj [1 3] [2 4])
;; => [1 3 2 4]

(concat {:a 1 :b 2} {:c 3 :d 4})
;; => ([:a 1] [:b 2] [:c 3] [:d 4])

(reduce concat {:a 1 :b 2} {:c 3 :d 4})
;; => ([:a 1] [:b 2] :c 3 :d 4)



;; LAZY-SEQ

;; MAP
(map #(inc %) [1 2 3 4])

;; FILTER
(filter #(= (mod % 2) 0) [1 2 3 4])

;; ZIPMAP
(zipmap [:top :bottom :left :right] [1, 1, 5, 5])
;; => {:top 1, :bottom 1, :left 5, :right 5}
(zipmap [:top :bottom :left :right] (repeat 0))
;; => {:top 0, :bottom 0, :left 0, :right 0}




;; INTO
;; Łączenie wartości gdzie pierwszy argument wskazuje typ danych na wyjście
;; jeśli wskażemy pustą mapę, to wszystko będzie konwertowane do mapy
;; ale jeśli spróbujemy z wektora zrobić mapę to dostaniemy błąd bo mapa wymaga
;; dwóch argumentów, klucz i wartość
(into {5 6} [1 2 3 4])
;; => Jebnie
(into {5 6} {1 2 3 4})
;; => {1 2, 3 4}
(reduce (fn [acc add]
          (into acc {add add}))
        {} [1 2 3 4])
;; => {1 1, 2 2, 3 3, 4 4}
;; Z wykorzystaniem reduce zmieniliśmy pierwszą wartość z wektora 
;; na dwa argumenty, klucz i wartość



;; COMP
;; Wykonywanie operacji krok po kroku, niby zagłebianie się w nawiasy.
;; Jako pierwsza wykona się ostatnia podana funkcja
((comp str +) 8 8 8)
;; => "24"
(map (comp zero? inc) [-1 0 1])
;; => (true false false)
(filter (comp not zero?) [0 -1 0 2 0 3 0 4])
;; => (-1 2 3 4)


;; EVERY-PRED
;; Coś jak walidacja, dopóki wartości zwracają true będzie sprawdzany kolejny
;; warunek. Jeśli któryś warunek zwróci false, sprawdzanie się zatrzyma, a
;; funkcja zwróci false
((every-pred number?) "1" 3 5) ;; => false
((every-pred number? odd?) 1 3 5) ;; => true
((every-pred vector? (comp not empty?)) [1 2 3]) ;; => true

;; SOME-FN
;;
((some-fn even? #(> % 10)) 1 3) ;; => false 
((some-fn even? #(< % 10)) 1 3) ;; => true


;; ->>
(macroexpand '(->> 0 (+ 1) (+ 2) (+ 3))) ;; => (+ 3 (+ 2 (+ 1 0)))
(->> 0 (+ 1) (+ 2) (+ 3)) ;; => 6

;; ->
(macroexpand '(-> 0 (+ 1) (+ 2) (+ 3))) ;; => (+ (+ (+ 0 1) 2) 3)
(-> 0 (+ 1) (+ 2) (+ 3)) ;; => 6


;; JUXT
;; ((juxt fn) arg)
;; ((juxt fn_a fn_b fn_c) val_x) => [(a x) (b x) (c x)]
;;
((juxt take) 3 [1 2 3 4 5 6]) ;; => [(1 2 3)]
((juxt take drop) 3 [1 2 3 4 5 6]) ;; => [(1 2 3) (4 5 6)]
((juxt :lname :fname) {:fname "Aleks" :lname "S"}) ;; => ["S" "Aleks"]


;; MAPCAT
;;
(concat [3 2 1 0] [6 5 4] [9 8 7]) ;; => (3 2 1 0 6 5 4 9 8 7)
(mapcat reverse [[3 2 1 0] [6 5 4] [9 8 7]]) ;; => (0 1 2 3 4 5 6 7 8 9)
(mapcat vec (seq {:a 1 :b 2}));; => (:a 1 :b 2)


;; Function cases
;; -> map filter
(->> (map #(if (odd? %) %) [1 2 3 4 5 6 7 8 9 10]) (filter #(not (nil? %))))
(-> (filter #(= "dec" (first %)) {"inc" #(inc %) "dec" #(dec %)})
    (first)
    (second)
    (map [1 2 3]))

(->> [1 2 3 4]
     (reduce (fn [acc add] (into acc {add add})) {})
     (map #(vec (list (keyword (str (first %))) (second %))))
     (into (sorted-map)))

