(ns aleks.defprotocol)

;;Next Proxy
;;  [x] proxy
;;  [x] proxy-super
;;  [x] defprotocol
;;  [x] definterfaice
;;  [x] gen-class
;;  [?] deftype
;;  [?] defrecord
;;  [?] reify
;;  [ ] cl-format
;;  [ ] rozszerzanie języka clojure

;; Ciekawostka, dotimes możę posłużyć za loop
(def myv [12 34 55])
(dotimes [i (count [12 34 55])] (println (nth myv i)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; proxy i proxy-super
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Przykład z wykorzystaniem java ArrayList
(def myjavalist (java.util.ArrayList.))

(comment
  myjavalist
  (.add myjavalist "Pepe")
  )


;; Przykład rozszerzenia java ArrayList
;; proxy       - tworzy nowy typ nadpisując wskazane oryginalne funkcjie
;;               de facto to dziedziczenie z klasy i overriding
;; proxy-super - pozwala wykorzystać oryginalną funkcję w tej nadpisywanej
;; drugi vec to argumenty konstruktora

;; Dodamy wypisanie ilości elementów w liście przy funkcji dodającej
(def myproxylist                           ;; obiekt listy od razu zostanie stworzony
  (proxy [java.util.ArrayList] []          ;; proxy nie tworzy nowego typu, ale na podstawie java.util.ArrayList
    (add [arg]                             ;; wskazujemy metodę do nadpisania (overriding)
      (proxy-super add arg)                ;; użyjemy oryginalnej funkcji add do dodania elementu
      (println "Now count: " (count this)) ;; wypiszemy ilość elementów w liście
      true                                 ;; domyślnie funkcja coś zwraca (tu true) więc i my zwracamy true
      )))

(comment
 myproxylist
 (.add myproxylist "Frog")
 )


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; defprotocol i deftype
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; defprotocol - de facto coś jak interfejsy ale wymaga ustawienia pierwszego elementu
;;               jako this czyli obiekt (record), na którym pracuje
;; deftype     - robi tworzy klasę na postawie defprotocol

(defprotocol PAnimal
  (voice [this])
  (eat   [this food]))

(deftype Mypet [type name]
  PAnimal
  (voice [this]
    (cond
      (= type :dog) (doto "Hau Hau" println)
      (= type :cat) (doto "Miau Miau" println)))
  (eat [this food]
    (doto (format "%s eats now %s" name food) println)))

(def puszek (Mypet. :dog "Puszek"))
(.voice puszek)
(.eat puszek "Peppa Pig")

(def kiti (Mypet. :cat "Kiti"))
(.voice kiti)
(.eat kiti "some bird")


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; definterface
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; definterface - czy jest jakaś różnica względem defprotocol poza tym
;;                że można stworzyć interfejs bez pierwszego argumentu this?

(definterface IAnimal
  (whatI []))

(deftype RIAnimal []
  IAnimal
  (whatI [this] "I'm just Animla Interface"))


(def burek (RIAnimal.))
(.whatI burek)


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; deftype vs defrecord
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Prawie to samo
;; Do trzymania danych lepszy defrecord

(defrecord A [a b])
(deftype B [a b])

(def a (A. 1 2))
(def b (B. 1 2))

(.a a)
(.a b)

(:a a)
(:a b)


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; reify
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Reify - czy się różni reify od defrecord/deftype?

(defprotocol PSomebe
  (language [this])
  (color    [this]))

(deftype Human [lang colo]
  PSomebe
  (language [this] (format "My lang is %s" lang))
  (color    [this] (format "My color is %s" colo)))

(def mehuman (Human. "PL" "White"))
(.language mehuman)
(.color mehuman)

;; Reify rozszerza klasę tworząc nową/nowy typ
(defn nohuman [speek fur]
  (reify PSomebe
    (language [this] (format "Can not speek. I can do %s" speek))
    (color    [this] (format "My fur is %s" fur))))

(def doggy (nohuman "Hau" "long"))
(.language doggy)
(.color doggy)


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; :gen-class
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Generowanie gotowych klas do użycia
;; w standardzie java.
;; Ukrycie kodu źródłowego.
;; Przyśpieszenie pracy programu.


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; cl-format
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; ZADANIE
;; zerkjnij sobie na clojure.pprint funkcje cl-format,
;; za pomocą tej funkcji, nadpisz logikę działania funkcji print
;; biore i robie (print (dataframe....)) i mi się robi tabela w Org formacie
;; czym jest dataframe

;; https://www.cs.cmu.edu/Groups/AI/html/cltl/clm/node200.html
;; https://gigamonkeys.com/book/a-few-format-recipes.html
;; https://sodocumentation.net/common-lisp/topic/687/format
;; https://dept-info.labri.fr/~strandh/Teaching/MTP/Common/David-Lamkins/chapter24.html
;; https://www.cliki.net/FORMAT%20cheat%20sheet

