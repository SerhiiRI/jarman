(ns jarman.managment.tut-datascript
  (:refer-clojure :exclude [update])
  (:require
   ;; Clojure toolkit
   [clojure.string :as string]
   [clojure.java.io :as io]
   [seesaw.core :as c]
   ;; for datascript
   [clojure.set :as set]
   [datascript.core :as d]
   [rum.core :as rum]
   [datascript.transit :as dt]))

;;;;;;;;;;;;;;
;;; SCHEMA ;;; 
;;;;;;;;;;;;;;

;; DB represented by datoms. Each datom is an addition or retraction of a relation between an entity, an attribute, a value, and a transaction.
;; A datom is a tuple (an ordered list of elements) composed of entity/attribute/value/time, commonly abbreviated as EAVT
;; Each DB has a schema that describes the set of attributes
;; Schema not define which attributes can be associated with which entities, so decisions about which attributes apply to which entities are made by an application.

;;; initialize a database
(def conn (d/create-conn {}))

;;; { } define attributes fo schema 
;;; or datoms
(def schema {:car/maker {:db/type :db.type/ref}
             :car/colors {:db/cardinality :db.cardinality/many}})

(def conn (d/create-conn schema))


;;;;;;;;;;;;;;;;;;
;;; ATTRIBUTES ;;; 
;;;;;;;;;;;;;;;;;;

;; Schema attributes are defined using the same data model used for application data
;; REQUIRED ATTRIBUTES
;;
;; IDENT
;;  :db/ident -- unique name for your attribute
;; VALUE TYPE
;;  :db/valueType -- type of data tthat can be stored in the attribute
;; ______________________________________________________________________________
;; value-types      || Examples                      || Description
;; :db.type/bigdec  || 1.0M
;; :db.type/bigint  || 7N
;; :db.type/boolean || true
;; :db.type/double  || 1.0
;; :db.type/float   || 1.0
;; :db.type/instant || #inst "2017-09-16T11:43:3..." || (instant in time)
;; :db.type/keyword || :yellow
;; :db.type/long    || 42
;; :db.type/ref     || 42                            || reference to another entity
;; :db.type/string  || "foo"
;; :db.type/symbol  || foo
;; :db.type/tuple   || [42 23 "foo"]                 || tuples of scalar values
;; :db.type/uuid    || #uuid "f40e770e-9ad5-.."      || 128-bit unique identifier
;; :db.type/uri     || https://www.ls.html           || Uniform Resource (URI)
;; :db.type/bytes   || (byte-array (map byte [1 2)   || Value type for small binary data
;;
;; CARDINALITY
;;  :db/cardinality -- the attribute stores a single value, or a collection of values
;;  :db.cardinality/one - is single valued, it associates a single value with an entity
;;  :db.cardinality/many - is multi valued, it associates a set of values with an entity
;;
;; TUPLES
;;  :db/tupleAttrs - whose value is a vector of 2-8 keywords naming a scalar value type
;; Example
;;  {:db/ident :reg/semester+course+student
;;   :db/valueType :db.type/tuple
;;   :db/tupleAttrs [:reg/course :reg/semester :reg/student]
;;   :db/cardinality :db.cardinality/one
;;   :db/unique :db.unique/identity}
;;
;; OPTIONAL ATTRIBUTES
;;
;; :db/doc 
;; :db/unique 
;; :db.unique/value 
;; :db.unique/identity
;; :db.attr/preds
;; :db/index --  specifies a boolean value indicating that an index should be generated for this attribute (default false)
;; :db/fulltext -- specifies a boolean value indicating that an eventually consistent fulltext search index should be generated for the attribute (default false)

;;;;;;;;;;;;;;;
;;; INDEXES ;;;
;;;;;;;;;;;;;;;
;; Indexes are used behind the scenes to implement Datalog query and entities, and they can be accessed directly through methods of Database and Connection
;; EAVT || entity/atribute/value/tx || all datoms
;; AEVT || atribute/entity/value/tx || all datoms
;; AVET || atribute/value/entity/tx || all datoms with :db/unique or :db/index
;; VAET || value/atribute/entity/tx || all datoms with :db.type/ref
;; examples
;; entity-id -- 41
;; attribute -- :release/name
;; value -- "Rose"
;; tx-id -- 1100

;;;;;;;;;;;;;;;;;;;;;
;;; TRANSACT DATA ;;;
;;;;;;;;;;;;;;;;;;;;;

(defn datascript-test []
  (let [schema {:movie/actors {:db/cardinality :db.cardinality/many
                               :db/valueType :db.type/ref}
                :movie/director {:db/valueType :db.type/ref}}
        conn (d/create-conn schema)]
    (d/transact! conn [{:db/id -1
                        :movie/title "Top Gun"
                        :movie/year 1986}])
    (-> (d/datoms @conn :eavt)
        (seq)
        (first))
))

(datascript-test)

;; {:db/id -1
;;  :movie/title "Top Gun"
;;  :movie/year 1986}
;; map in transact is SYNTAX SUGAR for this :
;; [[:db/add -1 :movie/title "Top Gun"]
;;  [:db/add -1 :movie/year 1986]]

;;;;;;;;;;
;;; ID ;;;
;;;;;;;;;;

;; We use negative numbers in the e-id position, such as :db/id -1,
;; to ask the database to assign a new temporary ID.
;; When the transaction is committed, the database will assign a new, permanent, positive ID for our user.

;; to introduce two new entities in the same commit:
(d/transact conn [[:db/add -1 :person/name "Tom Cruise"]
                  [:db/add -2 :person/name "Anthony Edwards"]])
;; to update an existing entity
(d/transact conn [[:db/add 1 :movie/director 2]])

;;;;;;;;;;;
;; QUERY ;;
;;;;;;;;;;;
(d/q '[:find ?e :in $ ?name :where [?e :movie/title "Top Gun"]]
     @conn)
;; OR
(d/q '[:find ?e :in $ ?name :where [?e :movie/title ?name]]
     @conn "Top Gun") ;;=> #{[1]}
;; ?e -- find entity
;; $ -- in database
;; ?name -- using value name
;; [?e :movie/title ?name] -- where entity attribute has :movie/title and value uqual ?name

;;;;;;;;;;;;;;;;;
;;; IMPORTANT ;;;
;;;;;;;;;;;;;;;;;

;; Unlike Datomic, Datascript does not require specifying the type of every attribute.
;; Datascript attributes can store any value, though we want to explicitly specify two types of attributes, refs and cardinality, which  we'll explain in the following sections

(let [schema {:movie/actors {:db/cardinality
                             :db.cardinality/many
                             :db/valueType :db.type/ref}
              :movie/director {:db/valueType :db.type/ref}}
      conn (d/create-conn schema)])

;; db.type/ref
;; Refs are a datatype used for storing references to other entity IDs
;; like foreign keys in SQL 
;; Refs are always automatically indexed.

;;;;;;;;;;;;;;;
;;; EXAMPLE ;;;
;;;;;;;;;;;;;;;

(def schema {:movie/actors {:db/cardinality
                             :db.cardinality/many
                             :db/valueType :db.type/ref}
             :movie/director {:db/valueType :db.type/ref}})
(def conn (d/create-conn schema))

 @(d/transact conn [{:db/id -1
                   :person/name "Tom Cruise"}
                  {:db/id -2
                   :person/name "Anthony Edwards"}
                  {:db/id -3
                   :person/name "Tony Scott"}
                  {:db/id (d/tempid :user)
                   :movie/title "Top Gun"
                   :movie/year 1986
                   :movie/actors [-1 -2]
                   :movie/director -3}
                  {:db/id -4
                   :person/name "Arnold Schwarzenegger"}
                  {:db/id (d/tempid :user)
                   :movie/title "Terminator"
                   :movie/actors -4}
                  {:db/id -5
                   :person/name "Mel Brooks"}
                  {:db/id (d/tempid :user)
                   :movie/title "Spaceballs"
                   :movie/actors -5
                   :movie/director -5}
                  {:db/id -6
                   :person/name "Clint Eastwood"
                   :person/birth-year 1930}
                  {:db/id -7
                   :person/name "Morgan Freeman"}
                  {:db/id -8
                   :person/name "Gene Hackman"}
                  {:db/id -9
                   :person/name "Eli Wallach"}
                  {:db/id (d/tempid :user)
                   :movie/title "The Good, The Bad and The
 Ugly"
                   :movie/actors [-6 -9]}
                  {:db/id (d/tempid :user)
                   :movie/title "Unforgiven"
                   :movie/actors [-6 -7 -8]
                   :movie/director -6}])

;; PULL
;; pull, is used to retrieve some or all of an entity
;; like select, which part we want to load 
;; Example
;; SQL: "SELECT * FROM People"
;; (d/pull [db '*'])
;; "SELECT name, age FROM People"
;; (d/pull [db [:name :age]])
;; ["SELECT * FROM People WHERE People.id = ?", maksim-id]
;; (d/pull [db '*' maksim-id])



;; all movies
(d/q '[:find (pull ?movie [:movie/title]) :in $ :where
       [?movie :movie/title]] @conn)
;; => ([#:movie{:title "Top Gun"}] [#:movie{:title "Unforgiven"}] [#:movie{:title "Terminator"}] [#:movie{:title "The Good, The Bad and The\n Ugly"}] [#:movie{:title "Spaceballs"}] [#:movie{:title "Spaceballs"}] [#:movie{:title "Unforgiven"}] [#:movie{:title "Terminator"}] [#:movie{:title "Terminator"}] [#:movie{:title "The Good, The Bad and The\n Ugly"}] [#:movie{:title "Spaceballs"}] [#:movie{:title "Top Gun"}] [#:movie{:title "Unforgiven"}] [#:movie{:title "The Good, The Bad and The\n Ugly"}] [#:movie{:title "Top Gun"}])

(d/q '[:find ?movie :in $ :where
 [?movie :movie/title]] @conn)
;; => #{[4] [42] [20] [13] [22] [36] [28] [6] [34] [41] [8] [32] [14] [27] [18]}

;; list of actors in Top Gun
(let [top-gun (d/q '[:find ?mov . :in $ ?title :where [?mov
                                                       :movie/title ?title]] @conn "Top Gun")]
  (d/pull @conn '[* {:movie/actors [:person/name]}] top-gun))


(let [clint (d/q '[:find ?e . :in $ ?name :where [?e :person/name
                                                  ?name]] @conn "Clint Eastwood")]
  (d/pull @conn '[* {:movie/_actors [:movie/title]}] clint))

;; (d/q '[:find ?person ?movie :in ....])

(d/q '[:find ?e . :in $ ?name :where [?e :person/name ?name]]
     @conn "Arnold Schwarzenegger")

;; all movies
(d/q '[:find (pull ?movie [:movie/title]) :in $ :where [?movie
                                                        :movie/title]] @conn)
;; find actors
(d/q '[:find ?act :in $ :where [_ :movie/actors ?act]] @conn)

;; find all movies where the director acts in the movie:
(d/q '[:find (pull ?p [:person/name]) :in $ :where [?mov
                                                    :movie/actors ?p]
       [?mov :movie/director ?p]] @conn)



(d/q '[:find ?b :in $ ?name
       :where
       [?e :person/birth-year ?b]
       [?e :person/name ?n]
       [(= ?n ?name)]] @conn "Clint Eastwood")

(d/q '[:find ?n :in $ ?name
       :where
       [?e :person/name ?n]
       [(= ?n ?name)]] @conn "Clint Eastwood")

;; !!!!
;; Unification means that a variable must take the same value in all clauses at the same time.
;; example
(d/q '[:find ?p :in $ :where [?mov1 :movie/actors ?p]
       [?mov2 :movie/director ?p]] @conn)
;; => #{[7] [35] [23] [9] [37] [21]}

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; PREDICATE EXPRESSIONS ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(d/q '[:find (pull ?p [:person/name])
       (pull ?mov1 [:movie/title])
       (pull ?mov2 [:movie/title])
       :in $
       :where
       [?mov1 :movie/actors ?p]
       [?mov2 :movie/director ?p]
       [(!= ?mov1 ?mov2)]] @conn) ;; predicate

(d/datoms @conn :eavt)
;;this will rof datoms  that use :movie/actors
(->> (d/datoms @conn :avet :movie/actors)
 (map identity))

;; Differences DATOMIC and DATASCRIPT:
;; in datascript historical datoms are not preserved
;; improving load time ds

;;; DELETE
(d/transact! conn [[:db.fn/retractEntity 46]])

;;(d/transact! conn [[:db.fn/retract 22 :person/name]])



;;;;;;;;;;;;;;;;
;;; EXAMPLES ;;;
;;;;;;;;;;;;;;;;
;;; initialize a database
;;; { } define attributes fo schema 
;;; or datoms
(def schema {:car/maker {:db/type :db.type/ref}
             :car/colors {:db/cardinality :db.cardinality/many}})

(def conn (d/create-conn schema))

;;  insert 
(d/transact! conn [{:db/add 
                    :maker/name "Heyy"
                    :maker/country "Jude"}])

;;; transact! means we’re going to transact something (insert/delete/update).          
;;; conn -> it is our DB

(d/transact! conn [{:db/id -1
                    :maker/name "BMW"
                    :maker/country "Germany"}
                   {:car/maker -1
                    :car/name "i525"
                    :car/colors ["red" "green" "blue"]}])

;; Querying
(d/q '[:find ?name
       :where
       [?e :maker/name "BMW"]
       [?c :car/maker ?e]
       [?c :car/name ?name]]
     @conn)


(let [car-entity (ffirst
                  (d/q '[:find ?c
                         :where
                         [?e :maker/name "BMW"]
                         [?c :car/maker ?e]]
                       @conn))]
  (:car/name (d/entity @conn car-entity)))

;;;;;; UPGRADE
(def schema {:car/model {:db/unique :db.unique/identity}
             :car/maker {:db/type :db.type/ref}
             :car/colors {:db/cardinality :db.cardinality/many}})

(def schema {:maker/email {:db/unique :db.unique/identity}
             :car/model {:db/unique :db.unique/identity}
             :car/maker {:db/type :db.type/ref}
             :car/colors {:db/cardinality :db.cardinality/many}})

(def conn (d/create-conn schema))

(d/transact! conn [{:maker/email "ceo@bmw.com"
                    :maker/name "BMW"}
                   {:car/model "E39530i"
                    :car/maker [:maker/email "ceo@bmw.com"]
                    :car/name "2003 530i"}])

(d/entity @conn [:car/model "E39530i"])

(d/entity @conn [:maker/email "ceo@bmw.com"])

(:maker/name (d/entity @conn [:maker/email "ceo@bmw.com"]))

(d/transact! conn [{:car/model "E39520i"
                    :car/maker [:maker/email "ceo@bmw.com"]
                    :car/name "2003 520i"}])

(d/q '[:find [?name ...]
       :where
       [?c :car/maker [:maker/email "ceo@bmw.com"]]
       [?c :car/name ?name]]
     @conn)

(d/transact! conn [{:maker/email "ceo@bmw.com"
                    :maker/name "BMW Motors"}])

(:maker/name (d/entity @conn [:maker/email "ceo@bmw.com"]))


;;;;; third part

(def schema {:user/id {:db.unique :db.unique/identity}
             :user/name {}
             :user/age {}
             :user/parent {:db.valueType :db.type/ref
                           :db.cardinality :db.cardinality/many}})

(def conn (d/create-conn schema))

(d/transact! conn
             [{:user/id "1"
               :user/name "alice"
               :user/age 27}
              {:user/id "2"
               :user/name "bob"
               :user/age 29}
              {:user/id "3"
               :user/name "kim"
               :user/age 2
               :user/parent [[:user/id "1"]
                             [:user/id "2"]]}
              {:user/id "4"
               :user/name "aaron"
               :user/age 61}
              {:user/id "5"
               :user/name "john"
               :user/age 39
               :user/parent [[:user/id "4"]]}
              {:user/id "6"
               :user/name "mark"
               :user/age 34}
              {:user/id "7"
               :user/name "kris"
               :user/age 8
               :user/parent [[:user/id "4"]
                             [:user/id "5"]]}])

(d/q '[:find ?e
       :where [?e :user/id]]
     @conn)

(d/q '[:find ?e ?n
       :where
       [?e :user/id]
       [?e :user/name ?n]]
     @conn)

(d/q '[:find [?e ...]
       :where
       [?e :user/id]]
     @conn)

(d/q '[:find [?n ...]
       :where
       [?e :user/id]
       [?e :user/name ?n]]
     @conn)

(d/q '[:find ?n .
        :where
        [?e :user/id]
        [?e :user/name ?n]]
      @conn)
