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
   ;;jarman-tools
   [jarman.managment.db-managment :as db-meta]))

;;;;;;;;;;;;;;
;;; SCHEMA ;;; 
;;;;;;;;;;;;;;

;; DB represented by datoms. Each datom is an addition or retraction of a relation between an entity, an attribute, a value, and a transaction.
;; A datom is a tuple (an ordered list of elements) composed of entity/attribute/value/time, commonly abbreviated as EAVT
;; Each DB has a schema that describes the set of attributes
;; Schema not define which attributes can be associated with which entities, so decisions about which attributes apply to which entities are made by an application.

;;; initialize a database
(def conn (d/create-conn {}))

;;; { } define attributes fo schema x
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
;; :db.unique/value -- :db/unique :db.unique/identity -- unique value like email
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
        (first))))

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

;;;;;;;;;;;;;;;
;;; EXAMPLE ;;;
;;;;;;;;;;;;;;;

;; resolve referencess
(let [conn (d/create-conn {:friend {:db/valueType :db.type/ref
                                    :db/cardinality :db.cardinality/many}})
      tx   (d/transact! conn [{:name "Bob"
                               :friend [-1 -2]}
                              [:db/add -1  :name "Ivan"]
                              [:db/add -2  :name "Petro"]
                              [:db/add "B" :name "Boris"]
                              [:db/add "B" :friend -3]
                              [:db/add -3  :name "John"]
                              [:db/add -3  :friend "B"]])
      q '[:find ?fn
          :in $ ?n
          :where [?e :name ?n]
          [?e :friend ?fe]
          [?fe :name ?fn]]]
  (:tempids tx) 
  ;; (d/q q @conn "Bob")
  ;; => #{["Ivan"] ["Petro"]}
  ;; (d/q q @conn "Boris")
  ;; => #{["John"]}
  ;; (d/q q @conn "John")
  ;; => #{["Boris"]}
  )
;; => {1 1, -1 2, -2 3, "B" 4, -3 5, :db/current-tx 536870913}

(let [conn (d/create-conn {:user {:db/valueType :db.type/ref
                                  :db/cardinality :db.cardinality/many}})])

;;;;;;;;;;;;;;;;;;
;;; data-graph ;;;
;;;;;;;;;;;;;;;;;;
(def g (make-graph))

(def g 
  (add g 
       [[:john 
         :isa :person 
         :likes :pizza]
        [:mary
         :isa :person
         :likes :pasta]]))

(g)

(g :john)

(:db g)

(:schema (:db g))

(query g '[:find ?person 
           :where 
           [?e :isa ?_person]
           [?_person ::dsg/id :person]
           [?e ::dsg/id ?person]])
;; => ({:?person :mary} {:?person :john})

;;;;;;;;;;;;;;;
;;; EXAMPLE ;;;
;;;;;;;;;;;;;;;

(def schema {:user/documents {:db/cardinality
                              :db.cardinality/many
                              :db/valueType :db.type/ref}
             :user/permission {:db/valueType :db.type/ref}})
(def conn (d/create-conn schema))

@(d/transact conn [{:db/id -1
                    :document/name "Passport"}
                   {:db/id -2
                    :document/name "Sertufikat"}
                   {:db/id -3
                    :document/name "Dogovir"}
                   {:db/id -4
                    :permission/name "admin"}
                   {:db/id -5
                    :permission/name "user"}
                   {:db/id -6
                    :permission/name "developer"}
                   
                   {:db/id (d/tempid :user)
                    :user/name "Julia"
                    :user/documents [-1 -2]
                    :user/permission -5}
                   
                   {:db/id -7
                    :document/name "Doruchennia"}

                   {:db/id (d/tempid :user)
                    :user/name "Serhii"
                    :user/documents [-1 -7]
                    :user/permission -4}
                   
                   {:db/id -8
                    :document/name "Contract"}

                   {:db/id (d/tempid :user)
                    :user/name "Anna"
                    :user/documents [-2 -8 -7]
                    :user/permission -4}])
;; all users
(d/q '[:find (pull ?user [:user/name]) :in $ :where
       [?user :user/name]] @conn)
;; => ([#:user{:name "Julia"}] [#:user{:name "Serhii"}] [#:user{:name "Anna"}])

;; entity id of users
(d/q '[:find ?user :in $ :where
 [?user :user/name]] @conn)
;; => #{[7] [9] [11]}

;; list of docs Anna
(let [n-user (d/q '[:find ?user . :in $ ?name :where [?user
                                                       :user/name ?name]] @conn "Anna")]
  (d/pull @conn '[* {:user/documents [:document/name]}] n-user))
;; => {:db/id 11, :user/documents [#:document{:name "Sertufikat"} #:document{:name "Doruchennia"} #:document{:name "Contract"}], :user/name "Anna", :user/permission #:db{:id 4}}

;; id of doc
(d/q '[:find ?e . :in $ ?name :where [?e :document/name ?name]]
     @conn "Contract")

;; all docs
(d/q '[:find (pull ?docs [:document/name]) :in $ :where [?docs
                                                         :document/name]] @conn)
;; find users
(d/q '[:find ?us :in $ :where [_ :user/name ?us]] @conn)

;; search, if not user, return nil
(d/q '[:find ?n . :in $ ?name
       :where
       [?e :user/name ?n]
       [(= ?n ?name)]] @conn "Anna")

;;;;;;;;;;;;;;;
;;; Example ;;;
;;;;;;;;;;;;;;;

(def schema {:table-name { :db/unique :db.unique/identity }
             :friend { :db/valueType :db.type/ref }})

(def db (d/db-with (d/empty-db schema)
                   [{:db/id 1 :id 1 :table-name "cashe-register" :prop "components cash-register" :friend 2}
                    {:db/id 2 :id 2 :table-name "repair-contract" :prop "components-contract" :friend 3}
                    {:db/id 3 :id 3 :table-name "repair-reason" :prop "components-reason" }]))

(d/q '[:find ?e ?v
       :in $ ?e
       :where [?e :prop ?v]]
     db [:table-name "cashe-register"])
;; => #{[[:table-name "cashe-register"] "components cash-register"]}

(d/q '[:find ?fn
       :in $ ?n
       :where [?e :table-name ?n]
       [?e :friend ?fe]
       [?fe :table-name ?fn]] db "cashe-register")
;; => #{["repair-contract"]}

(d/q '[:find ?fp
       :in $ ?n
       :where
       [?e :table-name ?n]
       [?e :friend ?fe]
       [?fe :prop ?fp]] db "cashe-register")
;; => #{["components-contract"]}

(d/q '[:find ?p
       :in $ ?n
       :where
       [?e :table-name ?n]
       [?e :friend ?fe]
       [?fe :friend ?ffe]
       [?ffe :prop ?p]] db "cashe-register")
;; => #{["components-reason"]}





(def schema {:id {:db/unique :db.unique/identity} ;; unique values for tables
             :ref {:db/cardinality
                   :db.cardinality/many
                   :db/valueType :db.type/ref
                   :db/doc "references for table"}
             :table_name {:db/unique :db.unique/identity}
             :prop {:db/doc "data in {} for table, main keys :table and :columns"}})

(def conn (d/create-conn schema))



@(d/transact conn (vec (map (fn [table-map]
                              (let [ref ((comp :front-references :ref :table :prop) table-map)
                                    id (:id table-map)]
                                (conj {:db/id (* -1 id)
                                       :ref (vec (map (fn [r-table] (let [r-id (r-table id-tables)]
                                                                      (if-not (nil? r-id) r-id 0))) ref))}
                                      table-map))) db-meta/all-tables)))

@(d/transact conn (vec (map (fn [table-map]
                              (let [col ((comp :columns :prop) table-map)
                                    id (:id table-map)]
                                (conj {:db/id (* -1 id)
                                       :table-name  (:table_name table-map)
                                       :table ((comp :table :prop) table-map)
                                       :columns ((comp :columns :prop) table-map)}))) db-meta/all-tables)))

;;;;;;;;;;;;
;;;; DB ;;;;
;;;;;;;;;;;;

(def schema
  {:human/name      {}
   :human/starships {:db/valueType   :db.type/ref
                     :db/cardinality :db.cardinality/many}
   :ship/name       {}
   :ship/class      {}})

(def data
 [{:human/name      "Naomi Nagata"
   :human/starships [{:db/id -1 :ship/name "Roci" :ship/class :ship.class/fighter}
                     {:ship/name "Anubis" :ship/class :ship.class/science-vessel}]}
  {:human/name      "Amos Burton"
   :human/starships [-1]}])


(def schema {:id {:db/unique :db.unique/identity} ;; unique values for tables
             :table_name {:db/unique :db.unique/identity}
             :table {:db/doc "data in {}"}
             :columns {}
             :foreign-keys {:db/cardinality
                            :db.cardinality/many
                            :db/valueType :db.type/ref
                            :db/doc "references for table"}})


(def schema {:id {:db/unique :db.unique/identity} ;; unique values for tables
             :table_name {:db/unique :db.unique/identity}
             :table {:db/doc "data in {}"}
             :foreign-keys {:db/cardinality
                            :db.cardinality/many
                            :db/valueType :db.type/ref
                            :db/doc "references for table"}})


(def conn (d/create-conn schema))



@(d/transact conn (vec (map (fn [table-map]
                              (let [columns ((comp :columns :prop) table-map)
                                    id (:id table-map)
                                    f-columns (into {} (map (fn [column] (let [id-col (name (:field-qualified column))]
                                                                           (reduce (fn [acc [k v]]
                                                                                     (if-not (nil? v)
                                                                                       (assoc  acc (keyword (name k) id-col)
                                                                                               (if (= k :foreign-keys)
                                                                                                 ((first (vals (first v))) id-tables) v))))
                                                                                   {} (dissoc column :field-qualified)))) columns))]
                                (conj {:db/id (* -1 id)
                                       :id (:id table-map)
                                       :table_name  (:table_name table-map)
                                       :table ((comp :table :prop) table-map)}
                                      f-columns))) db-meta/all-tables)))


(defn find-meta-by
  "Description
    find meta by some key amd value,
    return id of entity (datom)
   Example
    (find-entity-by :id 2) => {:id 2 :table_name \"user\"... "
  [e-key e-value]
  (d/pull @conn '[*] (d/q '[:find ?p .
                            :in $ ?k ?v
                            :wher0e
                            [?p ?k ?v]] @conn e-key e-value)))

(find-meta-by :table_name "user")

(defn find-entity-id [table-name]
  (d/q '[:find ?e .
         :in $ ?n
         :where
         [?e :table_name ?n]] @conn table-name))

(defn show-list-tables []
  (flatten (into [] (d/q '[:find ?n :in $ :where [_ :table_name ?n]] @conn))))

(defn show-all-meta []
  (doall (map (fn [table_name] (find-meta-by :table-name table_name) )(show-list-tables))))

(defn delete-one-meta [table-name]
  (try (d/transact! conn [[:db.fn/retractEntity (find-entity-id table-name)]])
       (catch clojure.lang.ExceptionInfo _ (println "Expected number or lookup ref for entity id, got nil"))))

(defn delete-all-meta []
  (doall (map (fn [table] (delete-one-meta table)) (show-list-tables))))

(defn insert-meta [metadata]
  (d/transact conn [(conj {:db/id -1} (find-meta-by :table_name "user"))] metadata))

;;;(insert-meta [(conj {:db/id -1} (find-meta-by :table_name "user"))])

(defn find-ref-col [field-col]
  (let [scol (keyword "foreign-keys" field-col)
        ref-id (d/q '[:find ?p .
                      :in $ ?n
                      :where
                      [_ ?n ?p]] @conn scol)
        ref-data (find-meta-by :id ref-id)] ref-data))

(let [db (d/db-with
          (d/empty-db {:profile {:db/valueType   :db.type/ref
                                 :db/cardinality :db.cardinality/many
                                 :db/isComponent true}})
          [{:db/id 1 :name "Ivan" :profile [3 4]}
           {:db/id 3 :email "@3"}
           {:db/id 4 :email "@4"}])]
  (d/touch (d/entity db 1)))




;;;(find-ref-col "user.id_permission")
(def schema
  {:id                      {}
   :table_name              {}
   :table                   {}
   :columns                 {:db/valueType   :db.type/ref
                             :db/cardinality :db.cardinality/many
                             :db/isComponent true
                             }
   :column/field-qualified  {}
   :column/refs             {:db/valueType   :db.type/ref
                             :db/cardinality :db.cardinality/one}})

(def id-tables
  (apply hash-map (flatten (map (fn [table-map] [(keyword (:table_name table-map))
                                                 (:id table-map)]) db-meta/all-tables))))

(defn serializer-cols [columns id]
  (vec (map (fn [column] (conj ;;{:db/id (* -1 id)}
                               (reduce (fn [acc [k v]]
                                         (assoc acc (keyword "column" (name k))
                                                (if (= k :foreign-keys)
                                                 ((first (vals (first v))) id-tables) (if (nil? v)
                                                                                        [] v))))
                                       {} column))) columns)))

(defn deserializer-col [])

(def data (vec (map (fn [table-map]
                       (let [columns ((comp :columns :prop) table-map)
                             id (:id table-map)
                             f-columns (serializer-cols columns id)]
                         (conj
                          {:db/id       (* -1 id)
                           :id          (:id table-map)
                           :table_name  (:table_name table-map)
                           :table       ((comp :table :prop) table-map)
                           :columns     f-columns}))) db-meta/all-tables)))

(def db
  (-> (d/empty-db schema)
      (d/db-with data)))

(d/touch (d/entity db 1))

(d/pull db '[*](d/q '[:find ?p .
                      :in $ ?n
                      :where
                      [?p :column/field-qualified ?n]] db :user.id_permission))





(def schema
  "Description
    create schema (datoms) for db,
    schema describes the set of attributes"
  {:id                      {:db.unique :db.unique/identity}
   :table_name              {:db.unique :db.unique/identity}
   :table                   {}
   :columns                 {:db/valueType   :db.type/ref
                             :db/cardinality :db.cardinality/many
                             :db/isComponent true} 
   :column/field-qualified  {:db.unique :db.unique/identity}
   :column/foreign-keys     {:db/valueType   :db.type/ref
                             :db.unique :db.unique/identity
                             :db/cardinality :db.cardinality/one}})

(defn- id-tables
  "Description
    return map with id and table-name for converting foreign keys in func serializer-cols
  Example
  (id-tables)
  => {:seal 13 :user 2 ...}
  "
  [] (apply hash-map (flatten (map (fn [table-map] [(keyword (:table_name table-map))
                                                    (:id table-map)]) (db-meta/getset)))))

(defn- serializer-cols
  "Description
    Serialize structure of metadata for schema db"
  [columns id]
  (vec (map (fn [column] (conj (reduce (fn [acc [k v]]
                                         (assoc acc (keyword "column" (name k))
                                                (if (= k :foreign-keys) ;; convert map-refs to id
                                                  ((first (vals (first v))) (id-tables)) (if (nil? v) [] v))))
                                       {} column))) columns)))

(def data (vec (map (fn [table-map]
                       (let [columns ((comp :columns :prop) table-map)
                             id (:id table-map)
                             f-columns (serializer-cols columns id)]
                         (conj
                          {:db/id       (* -1 id)
                           :id          (:id table-map)
                           :table_name  (:table_name table-map)
                           :table       ((comp :table :prop) table-map)
                           :columns     f-columns}))) (db-meta/getset))))

(def db
  (-> (d/empty-db schema)
      (d/db-with data)))


(defn table-model-columns [table-name colmn-list]
  (let
       ;; return from db model with id of columns in colmn-list [{:key :user.login, :text 5} ....]
      [id-map (reduce (fn [acc x] (if (nil? (some #{(:v x)} colmn-list)) acc (conj acc {:key (:v x) :text (:e x)}))) [] (d/datoms db :eavt))
       ;; select from db columns by id-datom [{:column/representation "Permisssion name" :column/field-qu...} ....]
       data-map (d/pull-many db [:column/foreign-keys :column/representation :column/field-qualified] (map (fn [f] (:text f))  id-map))
       ;; get from data-map foreign-keys (references), return map with table and repr to adding {:permission "id_permission"}
       refs (into {}(map (fn [x] {(keyword (:table_name (d/pull db [:table_name](:db/id (:column/foreign-keys x)))))
                                  (:column/representation x)} )(filter (fn [x] (not (empty? (:column/foreign-keys x)))) data-map)))
       ;; get from data-map representations and convert to our format [{:text "id_permission Permission name"}....]
       repr (reduce (fn [acc x] (conj acc {:text
                                           (let [col-repr (:column/representation x)
                                                 t-name (keyword (first (string/split (name  (:column/field-qualified x)) #"\.")))
                                                 n (t-name refs)]
                                             (str n (if-not (empty? n ) " ") col-repr))})) [] data-map)
       ;; in id-map change :text with id -> representations
       model-colmns (map (fn [a b] (merge a b)) id-map repr)]
    model-colmns))

(table-model-columns "user" [:user.id :user.login :user.password :user.first_name :user.last_name :user.id_permission :permission.id :permission.permission_name :permission.configuration])
;; => [{:text "id_permission Permission name"} {:text "id_permission Configuration"} {:text "login"} {:text "password"} {:text "first_name"} {:text "last_name"} {:text "id_permission"}]
;; => {:permission "id_permission"}
;; => ({:key :permission.permission_name, :text "id_permission Permission name"} {:key :permission.configuration, :text "id_permission Configuration"} {:key :user.login, :text "login"} {:key :user.password, :text "password"} {:key :user.first_name, :text "first_name"} {:key :user.last_name, :text "last_name"} {:key :user.id_permission, :text "id_permission"})

(g-table/gui-table-model-columns ["user" "permission"] [:user.id :user.login :user.password :user.first_name :user.last_name :user.id_permission :permission.id :permission.permission_name :permission.configuration])


(table-model-columns "repair_contract" [:repair_contract.id_cache_register
                                       :repair_contract.id_old_seal
                                       :repair_contract.id_new_seal
                                       :repair_contract.id_repair_reasons
                                       :repair_contract.id_repair_technical_issue
                                       :repair_contract.id_repair_nature_of_problem
                                       :repair_contract.repair_date
                                       :repair_contract.cache_register_register_date])


;;(d/touch (d/entity db [:column/field-qualified :user.id_permission]))

(defn- searcher
  "Description
    find ref-column by some key and value,
    return ref-column
   Example
    (find-ref-column \"user.id_permission\" \"representation\" \"permission.permission_name\"
     \"representation\") => [\"id_permission\" \"Permision name\" "
  [current-column c-field ref-column r-field]
  (first (d/q '[:find ?current-repr ?refs-repr 
                :in $ ?column-name ?column-ref-name ?c-field ?r-field 
                :where
                [?entity :column/field-qualified ?column-name] ;;find id-entity of current table
                [?entity :column/foreign-keys ?ref-id] ;;find id of ref-table
                [?ref-id :columns ?columns-id] ;; id columns ref-table
                [?columns-id :column/field-qualified ?column-ref-name] ;; id column , where field-qualefied = ?colun-ref-name (argument)
                [?columns-id ?r-field ?refs-repr]
                [?entity ?c-field ?current-repr]]
              db (keyword current-column)(keyword ref-column)(keyword "column" c-field)(keyword "column" r-field))))



(defn find-representations
  [current-column ref-column]
  (searcher current-column "representation" ref-column "representation"))

(find-representations "user.id_permission" "permission.permission_name")

(defn table-model-columns [name-table columns]
  
  ;;(reduce (fn [acc col] (conj acc {:key col :text "ll"})) [] columns)
  )

(table-model-columns "user" [:user.id :user.login :user.password :user.first_name :user.last_name :user.id_permission :permission.id :permission.permission_name :permission.configuration])

(edn/read-string (pr-str  (d/touch (d/entity db 9))))

(d/q '[:find ?r
       :in $ ?n ?p
       :where
       [?t :table_name "user"]
       [?t :columns ?c]
       [?c :column/foreign-keys]
       [?c :column/representation ?r]
;;     [?e :column/field-qualified :user.id_permission]
       ] db "user" [:user.id :user.login :user.password :user.first_name])




(d/q '[:find ?n
       :in   $sexes $ages %
       :where ($sexes male ?n)
       ($ages adult ?n) ]
     [["Ivan" :male] ["Darya" :female] ["Ole" :male] ["Igor" :male]]
     [["Ivan" 15] ["Ole" 66] ["Darya" 32]]
     '[[(male ?x)
        [?x :male]]
       [(adult ?y)
        [?y ?a]
        [(>= ?a 18)]]])

(contains?  [:a :b] :a  )

(every? 2 [2 3])



(some #(= "user" %) ["user" "permission"])

(d/q '[:find ?r
       :in $ ?n ?p ?c
       :where
       [?c :columns]
       [?r :t]]
     db "user" [:user.id :user.login :user.password :user.first_name] (hash-set "user" "permission"))


(d/pull-many db [:db/id :column/representation] [2])

(d/index-range)

(d/q '[:find ?current-repr ?refs-repr 
                :in $ ?column-name ?column-ref-name ?c-field ?r-field 
                :where
                [?entity :column/field-qualified ?column-name] ;;find id-entity of current table
                [?entity :column/foreign-keys ?ref-id] ;;find id of ref-table
                [?ref-id :columns ?columns-id] ;; id columns ref-table
                [?columns-id :column/field-qualified ?column-ref-name] ;; id column , where field-qualefied = ?colun-ref-name (argument)
                [?columns-id ?r-field ?refs-repr]
                [?entity ?c-field ?current-repr]]
              db (keyword current-column)(keyword ref-column)(keyword "column" c-field)(keyword "column" r-field))







(map (fn [x] [(:column/field-qualified (:a x))(:v x)])(d/datoms db :eavt))



(some #{:user.id} colmn-list)
(def b :user.id)

(some #{b} colmn-list)

(map (fn [x] (some #{(:v x)} colmn-list))  (d/datoms db :eavt))
