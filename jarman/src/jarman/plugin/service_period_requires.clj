(ns jarman.plugin.service-period-requires
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

(defn get-date-pairs
  "Description
    (get-date-pairs
      (date-object 2021 8  13)
      (date-object 2021 11 15))
      => [[\"2021-09-13\" \"2021-09-30\"] [\"2021-10-01\" \"2021-10-31\"] [\"2021-11-01\" \"2021-11-30\"] [\"2021-12-01\" \"2021-12-15\"]]"
  [data-s data-e]
  (let [formater (java.text.SimpleDateFormat. "YYYY-MM-dd")
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
;; (select-keys {} [:service_contract.id :service_contract.id_enterpreneur :service_contract.contract_start_term :service_contract.contract_end_term])
;; (select-keys {} [:enterpreneur.id :enterpreneur.name :enterpreneur.ssreou :enterpreneur.ownership_form :enterpreneur.vat_certificate :enterpreneur.individual_tax_number :enterpreneur.director :enterpreneur.accountant :enterpreneur.legal_address :enterpreneur.physical_address :enterpreneur.contacts_information])

;; -----------
;; all columns
;; -----------
#_[:#as_is

   ;; enterpr
   :enterpreneur.id
   :enterpreneur.name
   :enterpreneur.ssreou
   :enterpreneur.ownership_form
   :enterpreneur.vat_certificate
   :enterpreneur.individual_tax_number
   :enterpreneur.director
   :enterpreneur.accountant
   :enterpreneur.legal_address
   :enterpreneur.physical_address
   :enterpreneur.contacts_information

   ;; contract
   :service_contract.id
   :service_contract.id_enterpreneur
   :service_contract.contract_start_term
   :service_contract.contract_end_term

   ;; contract month
   :service_contract_month.id
   :service_contract_month.id_service_contract
   :service_contract_month.service_month_start
   :service_contract_month.service_month_end
   :service_contract_month.money_per_month
   :service_contract_month.was_payed]

(def enterpreneur-cols
  [:enterpreneur.id
   :enterpreneur.name
   :enterpreneur.ssreou
   :enterpreneur.ownership_form
   :enterpreneur.vat_certificate
   :enterpreneur.individual_tax_number
   :enterpreneur.director
   :enterpreneur.accountant
   :enterpreneur.legal_address
   :enterpreneur.physical_address
   :enterpreneur.contacts_information])
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
                              (swap! all-paths conj [lk1 lk2 lk3 false])
                              (swap! index-tree #(assoc-in % [lk1 lk2 lk3] x))
                              (assoc m k2 (conj v (into {:v [i1 i2 i3]} x2)))))))) {} coll)]
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
(defn info-grouped-query []
  (->>
   (db/query
    (select!
     {:table_name :enterpreneur,
      :left-join
      [:service_contract<-enterpreneur
       :service_contract_month<-service_contract]
      :column
      (vec
       (concat
        [:#as_is]
        enterpreneur-cols
        service_contract-cols
        service_contract_month-cols))}))
   (filter #(and (:service_contract.id %) (:service_contract_month.id %)))
   (map #(array-map :enterpreneur (select-keys % enterpreneur-cols)
                    :service_contract (select-keys % service_contract-cols)
                    :service_contract_month (select-keys % service_contract_month-cols)))
   (group-by-2 [(comp :enterpreneur.id :enterpreneur)
                (comp :service_contract.id :service_contract)
                (comp :service_contract_month.id :service_contract_month)]
               ;; two factorization level
               :enterpreneur
               :service_contract
               ;; lambda on leaf
               :service_contract_month )))

(def grouped-query (info-grouped-query))
(let [{rl :raw-list tip :tree-index-paths ti :tree-index tv :tree-view} grouped-query]
  ;; (map #(conj % false )tip)
  tv)


;;; INFO SELECTS ;;;
(defn info-for-enterpreneur [enterpreneur_id]
  {:pre [(number? enterpreneur_id)]}
  (db/query
   (select!
    {:table_name :service_contract_month,
     :inner-join
     [:service_contract_month->service_contract
      :service_contract->enterpreneur],
     :column
     [:#as_is
      ;; enterpr
      :enterpreneur.id
      :enterpreneur.name
      ;; :enterpreneur.ssreou
      ;; :enterpreneur.ownership_form
      ;; :enterpreneur.vat_certificate
      ;; :enterpreneur.individual_tax_number
      ;; :enterpreneur.director
      ;; :enterpreneur.accountant
      ;; :enterpreneur.legal_address
      ;; :enterpreneur.physical_address
      ;; :enterpreneur.contacts_information
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
     :where [:= :enterpreneur.id enterpreneur_id]})))

(defn info-for-service_contract [service_contract_id]
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

(defn get-paiment-for-enterpreneur [enterpreneur_id]
  {:pre [(number? enterpreneur_id)]}
  (reduce
   (fn [acc x] (+ acc (:service_contract_month.money_per_month x))) 0       
   (db/query (select! {:table_name :service_contract_month,
                       :inner-join
                       [:service_contract_month->service_contract
                        :service_contract->enterpreneur],
                       :column
                       [:#as_is
                        :service_contract_month.money_per_month
                        :service_contract.id
                        :enterpreneur.id]
                       :where [:= :enterpreneur.id enterpreneur_id]}))))

;;; PAYMENT SELECTS

(defn get-payment-for-enterpreneur [enterpreneur_id]
  {:pre [(number? enterpreneur_id)]}
  (reduce
   (fn [acc x] (+ acc (:service_contract_month.money_per_month x))) 0       
   (db/query (select! {:table_name :service_contract_month,
                       :inner-join
                       [:service_contract_month->service_contract
                        :service_contract->enterpreneur],
                       :column
                       [:#as_is
                        :service_contract_month.money_per_month
                        :service_contract.id
                        :enterpreneur.id]
                       :where [:= :enterpreneur.id enterpreneur_id]}))))

(defn get-payment-for-service_contract [service_contract_id]
  {:pre [(number? service_contract_id)]}
  (reduce
   (fn [acc x] (+ acc (:service_contract_month.money_per_month x))) 0       
   (db/query (select! {:table_name :service_contract_month,
                       :inner-join
                       [:service_contract_month->service_contract
                        :service_contract->enterpreneur],
                       :column
                       [:#as_is
                        :service_contract_month.money_per_month
                        :service_contract.id
                        :enterpreneur.id]
                       :where [:= :service_contract.id service_contract_id]}))))


(comment
  (get-payment-for-enterpreneur 1)
  (get-payment-for-service_contract 1))

;;; INSERTS

(defn insert-service_contract [id_enterpreneur contract_start_term contract_end_term]
  {:pre [(instance? java.util.Date contract_start_term) (instance? java.util.Date contract_end_term)]}
  (let [formater (java.text.SimpleDateFormat. "YYYY-MM-dd")]
    (:generated_key
     (clojure.java.jdbc/execute! (db/connection-get)
      (insert! {:table_name :service_contract
                :set {:id_enterpreneur id_enterpreneur
                      :contract_start_term (.format formater contract_start_term)
                      :contract_end_term   (.format formater contract_end_term)}})  {:return-keys ["id"]}
      ))
      ;; (insert! {:table_name :service_contract
      ;;           :set {:id_enterpreneur id_enterpreneur
      ;;                 :contract_start_term (.format formater contract_start_term)
      ;;                 :contract_end_term   (.format formater contract_end_term)}})
      ))

(defn insert-service_contract_month [service_contract_id start-date end-date money_per_month]
  "Template
    (insert! {:table_name :service_contract
              :column-list [:id_service_contract :service_month_start :service_month_end :money_per_month :was_payed]
              :values [[1 \"2021-09-13\" \"2021-09-30\" 400] [1 \"2021-10-01\" \"2021-10-31\" 400]]})"
  {:pre [(instance? java.util.Date start-date) (instance? java.util.Date end-date)]}
  (db/exec
   (insert! {:table_name :service_contract_month
             :column-list [:id_service_contract :service_month_start :service_month_end :money_per_month :was_payed]
             :values (mapv (fn [[start-date end-date]]
                             (vector service_contract_id start-date end-date money_per_month false))
                           (get-date-pairs start-date end-date))})))

(defn insert-all [id_enterpreneur contract_start_term contract_end_term money_per_month]
  {:pre [(some? id_enterpreneur)
         (some? money_per_month)
         (instance? java.util.Date contract_start_term)
         (instance? java.util.Date contract_end_term)]}
  (if-let [service_contract_id (insert-service_contract id_enterpreneur
                                                        contract_start_term
                                                        contract_end_term)]
    (insert-service_contract_month
     service_contract_id
     contract_start_term
     contract_end_term
     money_per_month)
    (println "Service month not been created for enterprenier with '%d' id" id_enterpreneur)))

(comment 
  (insert-service_contract_month 1 (date-object 2021 8  13) (date-object 2021 11 15) 400)
  (insert-service_contract       1 (date-object 2021 8  13) (date-object 2021 11 15))
  (insert-all                    1 (date-object 2021 8  13) (date-object 2021 11 15) 400))
;; => nil
;;; UPDATE 

(defn update-list-service-month-start [list-of-service-month]
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


;;[:service_contract :service-period :service_contract]
;;(info-for-service_contract 1)

