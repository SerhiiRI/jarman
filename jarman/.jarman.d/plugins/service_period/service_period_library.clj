(ns plugin.service-period.service-period-library
  (:require
   ;; Clojure toolkit 
   [clojure.string :as string]
   [clojure.java.io :as io]
   [seesaw.core :as c]
   [seesaw.border :as b]
   ;; Jarman toolkit
   [jarman.tools.lang :include-macros true :refer :all]
   [jarman.config.environment :as env]
   ;; --- 
   [jarman.logic.connection :as db]
   [jarman.gui.gui-components :as gcomp]
   [jarman.logic.sql-tool :refer [select! update! insert! delete!]]
   [jarman.logic.metadata :as mt]
   [jarman.logic.state :as state]))


;;; --------------------------------Data Toolkit----------------------------------

(import 'java.text.DateFormat)
(import 'java.text.ParseException)
(import 'java.text.SimpleDateFormat)
(import 'java.util.Calendar)
(import 'java.util.Date)

(defn date-object
  "Remember that simple (date) ruturn current
  date and time.
  Also if you 

  Example:
  (date 1900 11 29 1 2 3) => 1900-12-29 01:02:03
  (date 1900 11 29 1 2)   => 1900-12-29 01:02:00
  (date 1900 11 29 1)     => 1900-12-29 01:00:00
  (date 1900 11 29)       => 1900-12-29 00:00:00
  (date 1900 11)          => 1900-12-01 00:00:00
  (date 1900)             => 1900-01-01 00:00:00
  (date)                  => 2020-02-29 19:07:41"
  ([] (java.util.Date.))
  ([YYYY] (date-object YYYY 0 1 0 0 0))
  ([YYYY MM] (date-object YYYY MM 1 0 0 0))
  ([YYYY MM dd] (date-object YYYY MM dd 0 0 0))
  ([YYYY MM dd hh] (date-object YYYY MM dd hh 0 0))
  ([YYYY MM dd hh mm] (date-object YYYY MM dd hh mm 0))
  ([YYYY MM dd hh mm ss] (java.util.Date. (- YYYY 1900) MM dd hh mm ss)))

(defn data-comparator-new-old [d1 d2]
  (let [cd1 (doto (java.util.Calendar/getInstance) (.setTime d1))
        cd2 (doto (java.util.Calendar/getInstance) (.setTime d2))]
    (.before cd1 cd2)))

(defn data-comparator-old-new [d1 d2]
  (let [cd1 (doto (java.util.Calendar/getInstance) (.setTime d1))
        cd2 (doto (java.util.Calendar/getInstance) (.setTime d2))]
    (.before cd2 cd1)))

#_(sort-by
   identity
   data-comparator-new-old
   [(date-object 2021 8  13)
    (date-object 2021 9  13)
    (date-object 2021 11 15)
    (date-object 2021 6 15)])

(defn get-date-pairs
  "Description
    (get-date-pairs
      (date-object 2021 8  13)
      (date-object 2021 11 15))
      => [[\"2021-09-13\" \"2021-09-30\"] [\"2021-10-01\" \"2021-10-31\"] [\"2021-11-01\" \"2021-11-30\"] [\"2021-12-01\" \"2021-12-15\"]]"
  [data-s data-e]
  (let [formater       (java.text.SimpleDateFormat. "YYYY-MM-dd") ;; (java.text.SimpleDateFormat. "dd-MM-YYYY")
        CONST_START    (java.util.Calendar/getInstance)
        CONST_END      (java.util.Calendar/getInstance)
        beginCalendar  (java.util.Calendar/getInstance)
        finishCalendar (java.util.Calendar/getInstance)
        ;; FOR DEBUG DATA
        ;; data-s (date-object 2021 8  13)
        ;; data-e (date-object 2021 11 15)
        buffer (ref [])]
    (.setTime beginCalendar data-s)
    (.setTime finishCalendar data-e)
    (.setTime CONST_START data-s)
    (.setTime CONST_END data-e)
    (.set beginCalendar java.util.Calendar/DAY_OF_MONTH (.getActualMinimum beginCalendar java.util.Calendar/DAY_OF_MONTH))
    (while (.before beginCalendar finishCalendar)
      (let [start-month (java.util.Calendar/getInstance)
            end-month (java.util.Calendar/getInstance)]
        (if (= (.get CONST_START java.util.Calendar/MONTH) (.get beginCalendar java.util.Calendar/MONTH))
          (do
            (.setTime start-month (.getTime CONST_START)))
          (do
            (.setTime start-month (.getTime beginCalendar))
            (.set start-month java.util.Calendar/DAY_OF_MONTH
                  (.getActualMinimum beginCalendar java.util.Calendar/DAY_OF_MONTH))))
        (if (= (.get CONST_END java.util.Calendar/MONTH) (.get beginCalendar java.util.Calendar/MONTH))
          (do
            (.setTime end-month (.getTime CONST_END)))
          (do
            (.setTime end-month (.getTime beginCalendar))
            (.set end-month java.util.Calendar/DAY_OF_MONTH
                  (.getActualMaximum beginCalendar java.util.Calendar/DAY_OF_MONTH))))
        #_(dosync (alter buffer conj
                       (format
                        "%s - %s"
                        (.format formater (.getTime start-month))
                        (.format formater (.getTime end-month)))))
        (dosync (alter buffer conj
                       [(.format formater (.getTime start-month))
                        (.format formater (.getTime end-month))]))
        (.add beginCalendar java.util.Calendar/MONTH 1)))
    (deref buffer)))

;;; ---------------------SQL Inserts func---------------------------

;; -------------
;; clean-up keys
;; -------------
;; (select-keys {} [:id :was_payed :service_month_start :money_per_month :service_month_end :id_service_contract])
;; (select-keys {} [:service_contract.id :service_contract.id_enterprise :service_contract.contract_start_term :service_contract.contract_end_term])
;; (select-keys {} [:enterprise.id :enterprise.name :enterprise.ssreou :enterprise.ownership_form :enterprise.vat_certificate :enterprise.individual_tax_number :enterprise.director :enterprise.accountant :enterprise.legal_address :enterprise.physical_address :enterprise.contacts_information])

;; -----------
;; all columns
;; -----------
#_[:#as_is

   ;; enterpr
   :enterprise.id
   :enterprise.name
   :enterprise.ssreou
   :enterprise.ownership_form
   :enterprise.vat_certificate
   :enterprise.individual_tax_number
   :enterprise.director
   :enterprise.accountant
   :enterprise.legal_address
   :enterprise.physical_address
   :enterprise.contacts_information

   ;; contract
   :service_contract.id
   :service_contract.id_enterprise
   :service_contract.contract_start_term
   :service_contract.contract_end_term

   ;; contract month
   :service_contract_month.id
   :service_contract_month.id_service_contract
   :service_contract_month.service_month_start
   :service_contract_month.service_month_end
   :service_contract_month.money_per_month
   :service_contract_month.was_payed]

(def enterprise-cols
  [:enterprise.id
   :enterprise.name
   :enterprise.ssreou
   :enterprise.ownership_form
   :enterprise.vat_certificate
   :enterprise.individual_tax_number
   :enterprise.director
   :enterprise.accountant
   :enterprise.legal_address
   :enterprise.physical_address
   :enterprise.contacts_information])
(def service_contract-cols
  [:service_contract.id
   :service_contract.contract_start_term
   :service_contract.contract_end_term])
(def service_contract_month-cols
  [:service_contract_month.id
   :service_contract_month.service_month_start
   :service_contract_month.service_month_end
   :service_contract_month.money_per_month
   :service_contract_month.was_payed])

(defn group-by-v [f f-v coll]  
  (persistent!
   (reduce
    (fn [ret x]
      (let [k (f x)
            x (f-v x)]
        (assoc! ret k (conj (get ret k []) x))))
    (transient {}) coll)))

(defn group-by-2
  [[l1 l2 l3] f1 f2 f-v2 coll]
  (let [all-paths  (atom [])
        index-tree (atom {})
        tree (reduce
              (fn [ret x]
                (let [[i1 i2 i3] [(l1 x) (l2 x) (l3 x)]
                      k1 (into {:v [i1]} (f1 x)) 
                      k2 (into {:v [i1 i2]} (f2 x)) x2 (f-v2 x)]
                  (update ret k1
                          (fn [m]
                            (let [m (if (nil? m) {} m)
                                  v (get-in m [k2] [])
                                  [lk1 lk2 lk3] [(l1 x) (l2 x) (l3 x)]]
                              (swap! all-paths conj [lk1 lk2 lk3 (:service_contract_month.was_payed (f-v2 x))])  ;; TO REWRITE
                              (swap! index-tree #(assoc-in % [lk1 lk2 lk3] x))
                              (assoc m k2 (sort-by
                                           :service_contract_month.service_month_start
                                           data-comparator-new-old ;; data-comparator-old-new
                                           (conj v (into {:v [i1 i2 i3]} x2))))))))) {} coll)]
    {:raw-list coll
     :tree-index-paths @all-paths
     :tree-index @index-tree
     :tree-view tree}))

#_(group-by-2 [:a :b :c]
            :a :b
            :c
            [{:a 1, :b 1, :c -1}
             {:a 1, :b 2, :c -2}
             {:a 1, :b 2, :c -4}
             {:a 2, :b 1, :c -3}])

;; defn info-grouped-query
(defn info-grouped-query
  "Description:
     Return 3 atoms:
       enterprenuer @{:1 {enterprenuer-data}}
       contracts    @{:1 {:1 {contract-data} :2 {contract-data}}}
       subcontracts @{:1 {:1 {:1 {subcontract-data}} {:2 {subcontract-data}}}}"
  []

  (let [data (db/query
              (select!
               {:table_name :enterprise,
                :left-join
                [:service_contract<-enterprise
                 :service_contract_month<-service_contract]
                :column
                (vec
                 (concat
                  [:#as_is]
                  enterprise-cols
                  service_contract-cols
                  service_contract_month-cols))}))

        entrepreneurs-list (filter
                            #(not (nil? (second %)))
                            (distinct
                             (map
                              #(list (:enterprise.id %) (:enterprise.name %))
                              data)))]
    (->> data
         (filter #(and (:service_contract.id %) (:service_contract_month.id %)))
         (map #(array-map :enterprise (select-keys % enterprise-cols)
                          :service_contract (select-keys % service_contract-cols)
                          :service_contract_month (select-keys % service_contract_month-cols)))

         ((fn [data]
            (let [e-atom (atom {})
                  c-atom (atom {})
                  s-atom (atom {})]
              (doall
               (map (fn [{e :enterprise sc :service_contract scm :service_contract_month}]
                      (let [e-id (keyword (str (:enterprise.id e)))
                            c-id (keyword (str (:service_contract.id sc)))
                            s-id (keyword (str (:service_contract_month.id scm)))]
                        (if (empty? (get @e-atom e-id)) (swap! e-atom #(assoc % e-id e)))
                        (if (empty? (get-in @c-atom [e-id c-id])) (swap! c-atom #(assoc-in % [e-id c-id] (assoc sc :selected? false))))
                        (if (empty? (get-in @s-atom [e-id c-id s-id])) (swap! s-atom #(assoc-in % [e-id c-id s-id] (assoc scm :selected? false))))))
                    data))
              {:entrepreneurs-m @e-atom
               :contracts-m     @c-atom
               :subcontracts-m  @s-atom
               :entrepreneurs-list entrepreneurs-list})))
         
         ;; (group-by-2 [(comp :enterprise.id :enterprise)
         ;;              (comp :service_contract.id :service_contract)
         ;;              (comp :service_contract_month.id :service_contract_month)]
         ;;             ;; two factorization level
         ;;             :enterprise
         ;;             :service_contract
         ;;             ;; lambda on leaf
         ;;             :service_contract_month )
         )))
 
(comment
  (info-grouped-query)
  )

;;; FOR @JULIA

;;; WARNIGN! is just example for you.
;;; Try to rename and rewrite func's
;;; like it will comprotable for you
(defn calculate-payment-for-enterpreneuer [enterpreneuer-k service-contracts-m]
 (reduce (fn [acc x]
           (if-not (:service_contract_month.was_payed x)
             (+ acc (:service_contract_month.money_per_month x))
             acc)) 0 (apply concat (vals service-contracts-m))))

(defn calculate-payment-for-service_contract [service-contracts-k service-contracts-month-list-m]
 (reduce (fn [acc x]
           (if-not (:service_contract_month.was_payed x)
             (+ acc (:service_contract_month.money_per_month x))
             acc)) 0 service-contracts-month-list-m))

;;; In test case i showed how in simply way
;;; geting and calculating all of this data
;;; try to use this when you recursively render
;;; any you want
(def grouped-query (info-grouped-query))
(let [{rl :raw-list tip :tree-index-paths ti :tree-index tv :tree-view} grouped-query]
  ;; (map #(conj % false )tip)
  ;; {:per-enterprise (mapv (fn [[enter sc-m]]
  ;;                            (calculate-payment-for-enterpreneuer enter sc-m)) (seq tv))
  ;;  :per-contracts (mapv (fn [[enter sc-m]]
  ;;                         (mapv (fn [[sc-m scm-m]] (calculate-payment-for-service_contract sc-m scm-m)) (seq sc-m))) (seq tv))}
  (first (seq tv)))


;;; INFO SELECTS ;;;
#_(defn info-for-enterprise [enterprise_id]
  {:pre [(number? enterprise_id)]}
  (db/query
   (select!
    {:table_name :service_contract_month,
     :inner-join
     [:service_contract_month->service_contract
      :service_contract->enterprise],
     :column
     [:#as_is
      ;; enterpr
      :enterprise.id
      :enterprise.name
      ;; :enterprise.ssreou
      ;; :enterprise.ownership_form
      ;; :enterprise.vat_certificate
      ;; :enterprise.individual_tax_number
      ;; :enterprise.director
      ;; :enterprise.accountant
      ;; :enterprise.legal_address
      ;; :enterprise.physical_address
      ;; :enterprise.contacts_information
      ;; contract
      :service_contract.id
      :service_contract.contract_start_term
      :service_contract.contract_end_term
      ;; contract month
      :service_contract_month.id
      :service_contract_month.service_month_start
      :service_contract_month.service_month_end
      :service_contract_month.money_per_month
      :service_contract_month.was_payed]
     :where [:= :enterprise.id enterprise_id]})))

#_(defn info-for-service_contract [service_contract_id]
  {:pre [(number? service_contract_id)]}
  (db/query
   (select!
    {:table_name :service_contract,
     :inner-join [:service_contract_month<-service_contract],
     :column
     [:#as_is
      ;; contract
      :service_contract.id
      :service_contract.contract_start_term
      :service_contract.contract_end_term
      ;; contract month
      :service_contract_month.id
      :service_contract_month.service_month_start
      :service_contract_month.service_month_end
      :service_contract_month.money_per_month
      :service_contract_month.was_payed]
     :where [:= :service_contract.id service_contract_id]})))

#_(defn get-paiment-for-enterprise [enterprise_id]
  {:pre [(number? enterprise_id)]}
  (reduce
   (fn [acc x] (+ acc (:service_contract_month.money_per_month x))) 0       
   (db/query (select! {:table_name :service_contract_month,
                       :inner-join
                       [:service_contract_month->service_contract
                        :service_contract->enterprise],
                       :column
                       [:#as_is
                        :service_contract_month.money_per_month
                        :service_contract.id
                        :enterprise.id]
                       :where [:= :enterprise.id enterprise_id]}))))

;;; PAYMENT SELECTS

#_(defn get-payment-for-enterprise [enterprise_id]
  {:pre [(number? enterprise_id)]}
  (reduce
   (fn [acc x] (+ acc (:service_contract_month.money_per_month x))) 0       
   (db/query (select! {:table_name :service_contract_month,
                       :inner-join
                       [:service_contract_month->service_contract
                        :service_contract->enterprise],
                       :column
                       [:#as_is
                        :service_contract_month.money_per_month
                        :service_contract.id
                        :enterprise.id]
                       :where [:= :enterprise.id enterprise_id]}))))

#_(defn get-payment-for-service_contract [service_contract_id]
  {:pre [(number? service_contract_id)]}
  (reduce
   (fn [acc x] (+ acc (:service_contract_month.money_per_month x))) 0       
   (db/query (select! {:table_name :service_contract_month,
                       :inner-join
                       [:service_contract_month->service_contract
                        :service_contract->enterprise],
                       :column
                       [:#as_is
                        :service_contract_month.money_per_month
                        :service_contract.id
                        :enterprise.id]
                       :where [:= :service_contract.id service_contract_id]}))))


(comment
  (get-payment-for-enterprise 1)
  (get-payment-for-service_contract 1))

;;; INSERTS

(defn  date-to-obj
  [data-string] (.parse (java.text.SimpleDateFormat. "yyyy-MM-dd") data-string))

(defn insert-service_contract [id_enterprise contract_start_term contract_end_term]
  {:pre [(instance? java.util.Date contract_start_term) (instance? java.util.Date contract_end_term)]}
  (let [formater (java.text.SimpleDateFormat. "yyyy-MM-dd")]
    (let [contract-id (:generated_key
                       (clojure.java.jdbc/execute! (db/connection-get)
                                                   (insert! {:table_name :service_contract
                                                             :set {:id_enterprise id_enterprise
                                                                   :contract_start_term (.format formater contract_start_term)
                                                                   :contract_end_term   (.format formater contract_end_term)}})  {:return-keys ["id"]}))]
      contract-id)))

(defn insert-service_contract_month [service_contract_id start-date end-date money_per_month]
  "Template
    (insert! {:table_name :service_contract
              :column-list [:id_service_contract :service_month_start :service_month_end :money_per_month :was_payed]
              :values [[1 \"2021-09-13\" \"2021-09-30\" 400] [1 \"2021-10-01\" \"2021-10-31\" 400]]})"
  {:pre [(instance? java.util.Date start-date) (instance? java.util.Date end-date)]}
  (let [date-list (doto (get-date-pairs start-date end-date) println)
        sql-require (insert! {:table_name :service_contract_month
                              :column-list [:id_service_contract :service_month_start :service_month_end :money_per_month :was_payed]
                              :values (mapv (fn [[start-date end-date]]
                                              (vector service_contract_id start-date end-date money_per_month false)) date-list)})]
    (try 
      (do (db/exec sql-require) true)
      (catch Exception e (println "Error: " sql-require)))))

(defn insert-all [id_enterprise contract_start_term contract_end_term money_per_month]
  {:pre [(some? id_enterprise)
         (some? money_per_month)
         (instance? java.util.Date contract_start_term)
         (instance? java.util.Date contract_end_term)]}
  (println "INSEERRTT")
  (if-let [service_contract_id (insert-service_contract id_enterprise
                                                        contract_start_term
                                                        contract_end_term)]
    (insert-service_contract_month
     service_contract_id
     contract_start_term
     contract_end_term
     money_per_month)
    (println "Service month not been created for enterprenier with '%d' id" id_enterprise)))

;; (get-date-pairs (date-to-obj "2021-01-01") (date-to-obj "2021-02-01"))
;; (insert-service_contract 1 (date-object 2021 1 1) (date-object 2021 2 1))
;; (insert-service_contract_month 1 (date-to-obj "2021-01-01") (date-to-obj "2021-02-01") 400)

(comment 
  (insert-service_contract_month 1 (date-object 2021 8  13) (date-object 2021 11 15) 400)
  (insert-service_contract       1 (date-object 2021 8  13) (date-object 2021 11 15))
  (insert-all                    3 (date-object 2000 4  12) (date-object 2000 10 10) 100))
;; => nil


;;;;;;;;;;;;;;
;;; UPDATE ;;;
;;;;;;;;;;;;;;
(defn update-service-month-to-payed [list-months-id]
 (db/exec
  (update! {:table_name :service_contract_month
            :set {:service_contract_month.was_payed true}
            :where (reduce (fn [acc item] (conj acc [:= :id item])) [:or] list-months-id)})))

(defn update-all-service-month-payment [bln]
  (db/exec
   (update! {:table_name :service_contract_month
             :set {:service_contract_month.was_payed bln}})))

(defn update-service-money-per-month [id money] 
 (db/exec
  (update! {:table_name :service_contract_month
            :set {:service_contract_month.money_per_month money}
            :where [:= :id id]})))

(comment
  (update-service-month-to-payed [[1 25 195] [1 2 190]])
  (update-service-money-per-month 47 1000)
  (update-all-service-month-payment false))

#_(defn update-list-service-month-start [list-of-service-month]
  (for [entity list-of-service-month]
    (if (:id entity)
      (db/exec
       (update! {:table_name :service_contract_month :set (dissoc entity :id) :where [:= :id (:id entity)]}))
      (str "Cannot update nilable :id entity - "(pr-str entity)))))


(comment
  (update-list-service-month-start
  [{:id 1 :was_payed false
    :service_month_start "2021-09-13", :money_per_month 69,
    :service_month_end "2021-09-30", :id_service_contract 1}
   {:id 2 :was_payed true
    :service_month_start "2021-10-01", :money_per_month 400,
    :service_month_end "2021-10-31", :id_service_contract 1}
   {:id 3 :was_payed false
    :service_month_start "2021-11-01", :money_per_month 399,
    :service_month_end "2021-11-13", :id_service_contract 1}]))


;;; DELETES

(defn delete_service_contract [service_contract_id]
  {:pre [(some? service_contract_id)]}
  (and
   (apply #'and 
          (map :service_contract_month.was_payed
               (db/query
                (select! {:table_name :service_contract
                          :inner-join [:service_contract_month<-service_contract],
                          :column [:#as_is :service_contract_month.was_payed]
                          :where [:= :id service_contract_id]}))))
   ;; db/exec
   (delete! {:table_name :service_contract :where [:= :id service_contract_id]})))

