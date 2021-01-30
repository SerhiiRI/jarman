(ns jarman.config.config-manager
  (:gen-class)
  (:require [clojure.string :as string]))

(def tak {:conf.edn {:name "Config"
                     :display? :noedit
                     :type :file
                     :value {}}
          :file.edn {:name "Other file"
                     :display? :edit
                     :type :file
                     :value {:block0 {}
                             :block1 {:name "Some Block 1"
                                      :doc "this is block 1 documentation"
                                      :type :block
                                      :display? :edit
                                      :value {:param1 {:name "Parameter 1"
                                                       :doc "this is param 1 documentation"
                                                       :type :param
                                                       :component :text
                                                       :display? :edit
                                                       :value {:param4 {:name "First end"
                                                                        :doc "this is param 4 documentation"
                                                                        :type :param
                                                                        :component :textlist
                                                                        :display? :nil
                                                                        :value "42"}}}
                                              :block2 {:name [:link :to :translation]
                                                       :doc "this is block 2 documentation"
                                                       :type :block
                                                       :display? :noedit
                                                       :value {:param3 {:name [:link :to :translation]
                                                                        :doc "this is param 3 documentation"
                                                                        :type :param
                                                                        :component :textlist
                                                                        :display? :nil
                                                                        :value "some,value,string"}}}}}}}})

(defn build-part-of-map
  [map-floor lvl]
  (map #(let [floor (second %)
              value (get-in floor [:value])
              type (get-in floor [:type])
              comp (get-in floor [:component])
              name  (get-in floor [:name])]
          (do
            (print "\n")
            (cond
              (and (map? value)
                   (not (nil? value))) (do
                                         (print (apply str (repeat lvl " ")) "+-" name "[" type "]")
                                         (build-part-of-map value (+ 1 lvl)))
              :else (print (apply str (repeat lvl " ")) "  >" name ": " value "[" comp "]" "\n"))))
       map-floor))

(build-part-of-map tak 1)
;; => 
;;      +- Config [ :file ](()
;;      +- Other file [ :file ] 
;;         > nil :  nil [ nil ] 
;;       +- Some Block 1 [ :block ] 
;;        +- Parameter 1 [ :param ]
;;           > First end :  42 [ :textlist ] 
;;        +- [:link :to :translation] [ :block ] 
;;           > [:link :to :translation] :  some,value,string [ :textlist ] 




(concat '(1 2 3) '(4 5 6))
;; => (1 2 3 4 5 6)
(concat '(1 2 3) [4 5 6])
;; => (1 2 3 4 5 6)
(conj '(1 2 3) 4 5 6)
;; => (6 5 4 1 2 3)
(conj '(1 2 3) [4 5 6])
;; => ([4 5 6] 1 2 3)
(conj [1 2 3 4 5 6] 7 8 9)
;; => [1 2 3 4 5 6 7 8 9]




(def one {:param4 {:name "Parametr4"
                   :doc "Więcej tekstu"
                   :type :param
                   :component :intput
                   :display? :edit
                   :value "dupa"}})

(def two {:param5 {:name "Parametr5"
                   :doc "Więcej tekstu"
                   :type :param
                   :component :intput
                   :display? :edit
                   :value "dupa"}
          :param6 {:name "Parametr6"
                   :doc "Więcej tekstu"
                   :type :param
                   :component :intput
                   :display? :edit
                   :value "dupa"}})

(def tree {:block1 {:name ""
                    :doc "fjsdk"
                    :type :block
                    :display? [:none :edit :noedit]
                    :value {:param8 {:name "Parametr8"
                                     :doc "Więcej tekstu"
                                     :type :param
                                     :component :intput
                                     :display? :edit
                                     :value "YEY"}}}})

  (get-in tak [:config.edn :block1 :value :param1 :name])

; (map (fn [x] (first x)) two)
; (map #(%) one)
; (get-in one [:value])
; (map #(println %) one)

