(ns aleks.multimethods)

(defmulti types (fn [x y] [(class x) (class y)]))
(defmethod types [java.lang.String java.lang.String]
  [x y] "Only strings")
(defmethod types [java.lang.String java.lang.Long]
  [x y] "Second is Long")
(defmethod types [java.lang.Long java.lang.String]
  [x y] "First is Long")
(defmethod types [java.lang.Long java.lang.Long]
  [x y] "Both Long")
(defmethod types :default
  [x y]
  (format "Others types %s %s" (class x) (class y)))

(types 2 1)
;; => "Both Long"
(types "a" 1)
;; => "Second is Long"
(types 1 "a")
;; => "First is Long"
(types "a" "1")
;; => "Only strings"

;;(remove-all-methods types)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Dispatching
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmulti whoyou (fn [name] (:name name)))
(defmethod whoyou "Adam" [name] (format "Siema %s" (:name name)))
(whoyou {:name "Adam"})
;; => "Siema Adam"

;; Same as this on up
(defmulti whoyou2 :name)
(defmethod whoyou2 "Adam" [name] (format "Siema %s" (:name name)))
(whoyou2 {:name "Adam"})
;; => "Siema Adam"

(defmulti someprofile (fn [fname] fname))
(defmethod someprofile "Adam" [fname] (format "Siema %s" fname))
(defmethod someprofile "Stachu" [fname] (format "Wypierdalać z mojej ziemi"))
(someprofile "Adam")
(someprofile "Stachu")
;; => "Wypierdalać z mojej ziemi"

(defmulti bykeyword (fn [somekey] somekey))
(defmethod bykeyword :dupa [somekey] (format "Ale %s" somekey))
(defmethod bykeyword :pepe [somekey] (format "Wooow niezły %s bracie" (name somekey)))
(bykeyword :dupa)
;; => "Ale :dupa"
(bykeyword :pepe)
;; => "Wooow niezły pepe bracie"

(defmulti someprofile2 (fn [name] [name]))
(defmethod someprofile2 ["Adam"] [name] (format "Siema %s" name))
(defmethod someprofile2 ["Krzysiek"] [name] (format "%s ty pierdoło" name))
(defmethod someprofile2 :default [name] (prn name))
(someprofile2 "Adam")
(someprofile2 "Krzysiek")
(map someprofile2 ["Adam" "Krzysiek"])
;; => ("Siema Adam" "Krzysiek ty pierdoło")
(map someprofile2 ["Adam" "Krzysiek" "Bożena"])
;; => "Bożena"
;;    ("Siema Adam" "Krzysiek ty pierdoło" nil)

(defmulti someprofile3 (fn [name] name))
(defmethod someprofile3 ["Adam"] [name] (format "Siema %s" (first name)))
(defmethod someprofile3 ["Krzysiek"] [name] (format "%s ty pierdoło" (first name)))
(defmethod someprofile3 :default [name] (println "Nie rozpoznano:" name))
(someprofile3 ["Adam"])
;; => "Siema Adam"
(someprofile3 ["Krzysiek"])
;; => "Krzysiek ty pierdoło"
(map someprofile3 [["Adam"] ["Krzysiek"]])
;; => ("Siema Adam" "Krzysiek ty pierdoło")
(map someprofile3 (map vector ["Adam" "Krzysiek" "Bożena"]))
;; => Nie rozpoznano: [Bożena]
;;    ("Siema Adam" "Krzysiek ty pierdoło" nil)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Podtypy i polimorfizm
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def employers [{:name "Frank" :class ::worker_0}
                {:name "Agata" :class ::worker_1}
                {:name "Weronika" :class ::boss_0}
                {:name "Krzychu" :class ::boss_1}
                {:name "Stachu" :class ::boss_3}])

(derive :class/boss_0 :class/boss)
(derive :class/boss_1 :class/boss)
(derive :class/worker_0 :class/worker)
(derive :class/worker_1 :class/worker)
(derive :class/worker :class/employer)
(derive :class/boss :class/employer)

(underive :class/boss_0 :class/boss)
(underive :class/boss_1 :class/boss)
(underive :class/worker_0 :class/worker)
(underive :class/worker_1 :class/worker)
(underive :class/worker :class/employer)
(underive :class/boss :class/employer)

(derive ::boss_0   ::boss)
(derive ::boss_1   ::boss)
(derive ::worker_0 ::worker)
(derive ::worker_1 ::worker)
(derive ::worker   ::employer)
(derive ::boss     ::employer)

(defmulti isaboss? :class)
(defmethod isaboss? ::boss [employer] (format "Welcom %s my lovely boss <3" (:name employer)))
(defmethod isaboss? ::worker [employer] (format "%s! You little piece of shit. Back to work >:!" (:name employer)))
(defmethod isaboss? :default [employer] (format "Another one: %s" (:name employer)))

(isaboss? (first employers))
;; => "Frank! You little piece of shit. Back to work >:!"(isaboss? (last employers))
(isaboss? (last employers))
;; => "Welcom Krzychu my lovely boss <3"
(isaboss? (nth employers 2))
;; => "Welcom Weronika my lovely boss <3"
(map isaboss? employers)
;; => ("Frank! You little piece of shit. Back to work >:!"
;;     "Agata! You little piece of shit. Back to work >:!"
;;     "Welcom Weronika my lovely boss <3"
;;     "Welcom Krzychu my lovely boss <3")

(defmulti allsame :class)
(defmethod allsame ::employer [employer] (format "Employer: %s" (:name employer)))
(defmethod allsame :default [employer] (format "Another one: %s" (:name employer)))
(map allsame employers)


;; How working prefer-method?
(defmulti whatprefer? (fn [boss employer] [(:class boss) (:class employer)]))
(prefer-method whatprefer? [::employer ::boss] [::boss ::employer] )
(defmethod whatprefer?
  [::boss ::employer]
  [boss employer] (format "Boss: %s -> Employer: %s" (:name boss) (:name employer)))
(defmethod whatprefer?
  [::employer ::boss]
  [employer boss] (format "Employer: %s <- Boss: %s" (:name employer) (:name boss)))
;; (defmethod whatprefer?
;;   :default
;;   [boss employer] [(:class boss) (:class employer)])

(whatprefer? {:name "Agata" :class ::employer}
             {:name "Weronika" :class ::employer})

(comment
 (remove-method whatprefer? :default)
 (remove-all-methods whatprefer?)

 ;;Podsumowanie:
 ;;  Multimetody to de facto przeciążanie funkcji z tą różnicą, żę nie trzeba
 ;;  ingerować w już ustniejące przeciążneia by dodać kolejne.
 ;;
 ;;  Dispatcher na podstawie danych wejściowych dobiera odpowiednią funkcję do wywołania.
 ;;  Na wejście najwygodniejsza jest mapa. Można również podać argumenty kolejno, bądź listę.
 ;;  Przykłądowo można ustalić typ podanego argumentu i na jego podstawie wybierać adekwatną metodę.
 ;;
 ;;  Dzięki multimetodą można odseparować ładnie logikę dla różnych typów danych.
 ;;  Dzięki tworzeniu podtypów (subtypes) można dane częściowo pogrupować.
 ;;
 ;;  Nadal nie ogarniam jak działą prefer-method
 ;;
 ;;Next Proxy
 ;;  defprotocol
 ;;  definterfaice
 ;;  deftype
 ;;  defrecord
 ;;  gen-class
 ;;  proxy
 ;;  reify
 ;;  rozszerzanie języka clojure
 )
