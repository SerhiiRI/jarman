(ns jarman.config.config-manager
  (:require [clojure.string :as string]
            [jarman.tools.dev-tools :include-macros true :refer :all])
  )

(def tak {:conf.edn {:name "Config"
                     :display? :noedit
                     :type :file
                     :value {}}
          :file.edn {:name "Other file"
                     :display? :edit
                     :type :file
                     :value {:block0 {:name "Some Block 1"
                                      :doc "this is block 1 documentation"
                                      :type :block
                                      :display? :edit
                                      :value {:param3 {:name "param3"
                                                       :doc "this is param 3 documentation"
                                                       :type :param
                                                       :component :text
                                                       :display? :edit
                                                       :value "321"}
                                              :block5 {:name "block5"
                                                       :doc "this is block 5 documentation"
                                                       :type :block
                                                       :display? :noedit
                                                       :value {:param7 {:name [:other :link]
                                                                        :doc "this is param 7 documentation"
                                                                        :type :param
                                                                        :component :textlist
                                                                        :display? :nil
                                                                        :value "a,a,a"}}}}}
                             :block1 {:name "Some Block 1"
                                      :doc "this is block 1 documentation"
                                      :type :block
                                      :display? :edit
                                      :value {:param1 {:name "Parameter 1"
                                                       :doc "this is param 1 documentation"
                                                       :type :param
                                                       :component :text
                                                       :display? :edit
                                                       :value "123"}
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

(def offset-char "  ")
(defn is-block? [m] (= (:type m) :block))
(defn is-file? [m] (= (:type m) :file))
(defn is-block-file? [m] (or (is-block? m) (is-file? m)))
(defn is-param? [m] (= (:type m) :param))

(defn printblock
  ([m p] (printblock 0 m p))
  ([offset m path] (print (apply str (apply str (repeat offset offset-char))) (format "'%s' - %s" (:name m) (str path))  "\n")))
(defn printparam
  ([m p] (printparam 0 m p))
  ([offset m path] (print (apply str (apply str (repeat offset offset-char))) (format "'%s' - %s" (:name m) (str path)) ":" (:value m) "\n")))

(defn build-part-of-map [level [header tail] path]
  (if (some? header)
    ;; for header {:file.edn {....}} we  
    (let 
     [k-header ((comp first first) header)
      header ((comp second first) header)
      path (conj path k-header)]
      (cond
        
        ;; if Map represent Block or File
        (is-block-file? header)
        (do (printblock level header path)
            (build-part-of-map (inc level) (map-destruct (:value header)) path))
        
        ;; fi Map represent Parameters
        (is-param? header) (printparam level header path))))
  ;; Do recursive for Tail destruction in the same level
  (if (some? tail) (build-part-of-map level  (map-destruct tail) path)))


  (defn recur-config [m]
   (build-part-of-map 0 (jarman.tools.dev-tools/map-destruct m) []))

  
  (recur-config tak)

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

