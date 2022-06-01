(ns jarman.service-period-test
  (:require
   [clojure.string :as string]
   [clojure.test :refer :all]
   [jarman.faces                    :as face]
   [jarman.application.extension-manager :refer [do-load-extensions]]
   [jarman.application.collector-custom-themes :refer [do-load-theme]]
   [jarman.config.dot-jarman        :refer [dot-jarman-load]]
   [jarman.config.vars              :refer [defvar setq]]))

(comment
 (dot-jarman-load)
 (do-load-extensions)
 (do-load-theme (deref jarman.variables/theme-selected)))

(def test-enterprises-m
 {:1
  {:enterprise.director "Ivan Ivankow",
   :enterprise.individual_tax_number "3323392190",
   :enterprise.accountant "Anastasia Wewbytska",
   :enterprise.contacts_information "306690666",
   :enterprise.vat_certificate "EKCA31232",
   :enterprise.physical_address "B1",
   :enterprise.ssreou "32432432",
   :enterprise.ownership_form "LTD",
   :enterprise.id 1,
   :enterprise.legal_address "A1",
   :enterprise.name "Biedronka"},
  :3
  {:enterprise.director "Vasyl Mayni",
   :enterprise.individual_tax_number "2131248412",
   :enterprise.accountant "Aleksand",
   :enterprise.contacts_information "306690666",
   :enterprise.vat_certificate "UKCP12394",
   :enterprise.physical_address "B2",
   :enterprise.ssreou "11134534",
   :enterprise.ownership_form "PP",
   :enterprise.id 3,
   :enterprise.legal_address "A2",
   :enterprise.name "some shop"},
  :2
  {:enterprise.director "Vasyl Mayni",
   :enterprise.individual_tax_number "2312931424",
   :enterprise.accountant "Aleksand",
   :enterprise.contacts_information "306690666",
   :enterprise.vat_certificate "EKCP12344",
   :enterprise.physical_address "B2",
   :enterprise.ssreou "23155555",
   :enterprise.ownership_form "PP",
   :enterprise.id 2,
   :enterprise.legal_address "A2",
   :enterprise.name "KFC"}})

(def test-contracts-m
  {:1
   {:21
    {:service_contract.id 21,
     :service_contract.contract_start_term
     #inst "2021-10-15T22:00:00.000-00:00",
     :service_contract.contract_end_term
     #inst "2021-12-30T23:00:00.000-00:00",
     :selected? false},
    :23
    {:service_contract.id 23,
     :service_contract.contract_start_term
     #inst "2021-10-31T23:00:00.000-00:00",
     :service_contract.contract_end_term
     #inst "2022-10-30T23:00:00.000-00:00",
     :selected? false}},
   :3
   {:22
    {:service_contract.id 22,
     :service_contract.contract_start_term
     #inst "2021-10-15T22:00:00.000-00:00",
     :service_contract.contract_end_term
     #inst "2021-10-30T22:00:00.000-00:00",
     :selected? false}},
   :2
   {:24
    {:service_contract.id 24,
     :service_contract.contract_start_term
     #inst "2021-10-31T23:00:00.000-00:00",
     :service_contract.contract_end_term
     #inst "2021-12-30T23:00:00.000-00:00",
     :selected? false},
    :25
    {:service_contract.id 25,
     :service_contract.contract_start_term
     #inst "2021-10-18T22:00:00.000-00:00",
     :service_contract.contract_end_term
     #inst "2021-11-18T23:00:00.000-00:00",
     :selected? false}}})

(def test-subcontracts-m
 {:1
  {:21
   {:1021
    {:service_contract_month.id 1021,
     :service_contract_month.service_month_start
     #inst "2021-10-15T22:00:00.000-00:00",
     :service_contract_month.service_month_end
     #inst "2021-10-30T22:00:00.000-00:00",
     :service_contract_month.money_per_month 150.0,
     :service_contract_month.was_payed true,
     :selected? false},
    :1022
    {:service_contract_month.id 1022,
     :service_contract_month.service_month_start
     #inst "2021-10-31T23:00:00.000-00:00",
     :service_contract_month.service_month_end
     #inst "2021-11-29T23:00:00.000-00:00",
     :service_contract_month.money_per_month 100.0,
     :service_contract_month.was_payed false,
     :selected? false},
    :1023
    {:service_contract_month.id 1023,
     :service_contract_month.service_month_start
     #inst "2021-11-30T23:00:00.000-00:00",
     :service_contract_month.service_month_end
     #inst "2021-12-30T23:00:00.000-00:00",
     :service_contract_month.money_per_month 300.0,
     :service_contract_month.was_payed false,
     :selected? false}},
   :23
   {:1025
    {:service_contract_month.id 1025,
     :service_contract_month.service_month_start
     #inst "2021-10-31T23:00:00.000-00:00",
     :service_contract_month.service_month_end
     #inst "2021-11-29T23:00:00.000-00:00",
     :service_contract_month.money_per_month 10.0,
     :service_contract_month.was_payed true,
     :selected? false},
    :1029
    {:service_contract_month.id 1029,
     :service_contract_month.service_month_start
     #inst "2022-02-28T23:00:00.000-00:00",
     :service_contract_month.service_month_end
     #inst "2022-03-30T22:00:00.000-00:00",
     :service_contract_month.money_per_month 10.0,
     :service_contract_month.was_payed true,
     :selected? false},
    :1031
    {:service_contract_month.id 1031,
     :service_contract_month.service_month_start
     #inst "2022-04-30T22:00:00.000-00:00",
     :service_contract_month.service_month_end
     #inst "2022-05-30T22:00:00.000-00:00",
     :service_contract_month.money_per_month 10.0,
     :service_contract_month.was_payed false,
     :selected? false},
    :1027
    {:service_contract_month.id 1027,
     :service_contract_month.service_month_start
     #inst "2020-12-31T23:00:00.000-00:00",
     :service_contract_month.service_month_end
     #inst "2022-01-30T23:00:00.000-00:00",
     :service_contract_month.money_per_month 10.0,
     :service_contract_month.was_payed false,
     :selected? false},
    :1030
    {:service_contract_month.id 1030,
     :service_contract_month.service_month_start
     #inst "2022-03-31T22:00:00.000-00:00",
     :service_contract_month.service_month_end
     #inst "2022-04-29T22:00:00.000-00:00",
     :service_contract_month.money_per_month 10.0,
     :service_contract_month.was_payed false,
     :selected? false},
    :1026
    {:service_contract_month.id 1026,
     :service_contract_month.service_month_start
     #inst "2021-11-30T23:00:00.000-00:00",
     :service_contract_month.service_month_end
     #inst "2021-12-30T23:00:00.000-00:00",
     :service_contract_month.money_per_month 10.0,
     :service_contract_month.was_payed false,
     :selected? false},
    :1036
    {:service_contract_month.id 1036,
     :service_contract_month.service_month_start
     #inst "2022-09-30T22:00:00.000-00:00",
     :service_contract_month.service_month_end
     #inst "2022-10-30T23:00:00.000-00:00",
     :service_contract_month.money_per_month 10.0,
     :service_contract_month.was_payed false,
     :selected? false},
    :1032
    {:service_contract_month.id 1032,
     :service_contract_month.service_month_start
     #inst "2022-05-31T22:00:00.000-00:00",
     :service_contract_month.service_month_end
     #inst "2022-06-29T22:00:00.000-00:00",
     :service_contract_month.money_per_month 10.0,
     :service_contract_month.was_payed false,
     :selected? false},
    :1028
    {:service_contract_month.id 1028,
     :service_contract_month.service_month_start
     #inst "2022-01-31T23:00:00.000-00:00",
     :service_contract_month.service_month_end
     #inst "2022-02-27T23:00:00.000-00:00",
     :service_contract_month.money_per_month 10.0,
     :service_contract_month.was_payed false,
     :selected? false},
    :1033
    {:service_contract_month.id 1033,
     :service_contract_month.service_month_start
     #inst "2022-06-30T22:00:00.000-00:00",
     :service_contract_month.service_month_end
     #inst "2022-07-30T22:00:00.000-00:00",
     :service_contract_month.money_per_month 10.0,
     :service_contract_month.was_payed false,
     :selected? false},
    :1035
    {:service_contract_month.id 1035,
     :service_contract_month.service_month_start
     #inst "2022-08-31T22:00:00.000-00:00",
     :service_contract_month.service_month_end
     #inst "2022-09-29T22:00:00.000-00:00",
     :service_contract_month.money_per_month 10.0,
     :service_contract_month.was_payed false,
     :selected? false},
    :1034
    {:service_contract_month.id 1034,
     :service_contract_month.service_month_start
     #inst "2022-07-31T22:00:00.000-00:00",
     :service_contract_month.service_month_end
     #inst "2022-08-30T22:00:00.000-00:00",
     :service_contract_month.money_per_month 10.0,
     :service_contract_month.was_payed false,
     :selected? false}}},
  :3
  {:22
   {:1024
    {:service_contract_month.id 1024,
     :service_contract_month.service_month_start
     #inst "2021-10-15T22:00:00.000-00:00",
     :service_contract_month.service_month_end
     #inst "2021-10-30T22:00:00.000-00:00",
     :service_contract_month.money_per_month 50.0,
     :service_contract_month.was_payed true,
     :selected? false}}},
  :2
  {:24
   {:1037
    {:service_contract_month.id 1037,
     :service_contract_month.service_month_start
     #inst "2021-10-31T23:00:00.000-00:00",
     :service_contract_month.service_month_end
     #inst "2021-11-29T23:00:00.000-00:00",
     :service_contract_month.money_per_month 10.0,
     :service_contract_month.was_payed true,
     :selected? false},
    :1038
    {:service_contract_month.id 1038,
     :service_contract_month.service_month_start
     #inst "2021-11-30T23:00:00.000-00:00",
     :service_contract_month.service_month_end
     #inst "2021-12-30T23:00:00.000-00:00",
     :service_contract_month.money_per_month 10.0,
     :service_contract_month.was_payed true,
     :selected? false}},
   :25
   {:1039
    {:service_contract_month.id 1039,
     :service_contract_month.service_month_start
     #inst "2021-10-18T22:00:00.000-00:00",
     :service_contract_month.service_month_end
     #inst "2021-10-30T22:00:00.000-00:00",
     :service_contract_month.money_per_month 20.0,
     :service_contract_month.was_payed false,
     :selected? false},
    :1040
    {:service_contract_month.id 1040,
     :service_contract_month.service_month_start
     #inst "2021-10-31T23:00:00.000-00:00",
     :service_contract_month.service_month_end
     #inst "2021-11-18T23:00:00.000-00:00",
     :service_contract_month.money_per_month 20.0,
     :service_contract_month.was_payed false,
     :selected? false}}}})

(def ok-subcontracts-all-selected-select
  {:1021
   {:service_contract_month.id 1021,
    :service_contract_month.service_month_start
    #inst "2021-10-15T22:00:00.000-00:00",
    :service_contract_month.service_month_end
    #inst "2021-10-30T22:00:00.000-00:00",
    :service_contract_month.money_per_month 150.0,
    :service_contract_month.was_payed true,
    :selected? true},
   :1022
   {:service_contract_month.id 1022,
    :service_contract_month.service_month_start
    #inst "2021-10-31T23:00:00.000-00:00",
    :service_contract_month.service_month_end
    #inst "2021-11-29T23:00:00.000-00:00",
    :service_contract_month.money_per_month 100.0,
    :service_contract_month.was_payed false,
    :selected? true},
   :1023
   {:service_contract_month.id 1023,
    :service_contract_month.service_month_start
    #inst "2021-11-30T23:00:00.000-00:00",
    :service_contract_month.service_month_end
    #inst "2021-12-30T23:00:00.000-00:00",
    :service_contract_month.money_per_month 300.0,
    :service_contract_month.was_payed false,
    :selected? true}})

(def ok-subcontracts-all-selected-unselect
  {:1021
   {:service_contract_month.id 1021,
    :service_contract_month.service_month_start
    #inst "2021-10-15T22:00:00.000-00:00",
    :service_contract_month.service_month_end
    #inst "2021-10-30T22:00:00.000-00:00",
    :service_contract_month.money_per_month 150.0,
    :service_contract_month.was_payed true,
    :selected? false},
   :1022
   {:service_contract_month.id 1022,
    :service_contract_month.service_month_start
    #inst "2021-10-31T23:00:00.000-00:00",
    :service_contract_month.service_month_end
    #inst "2021-11-29T23:00:00.000-00:00",
    :service_contract_month.money_per_month 100.0,
    :service_contract_month.was_payed false,
    :selected? false},
   :1023
   {:service_contract_month.id 1023,
    :service_contract_month.service_month_start
    #inst "2021-11-30T23:00:00.000-00:00",
    :service_contract_month.service_month_end
    #inst "2021-12-30T23:00:00.000-00:00",
    :service_contract_month.money_per_month 300.0,
    :service_contract_month.was_payed false,
    :selected? false}})

(def ok-subcontract-selected
  {:service_contract_month.id 1022,
   :service_contract_month.service_month_start
   #inst "2021-10-31T23:00:00.000-00:00",
   :service_contract_month.service_month_end
   #inst "2021-11-29T23:00:00.000-00:00",
   :service_contract_month.money_per_month 100.0,
   :service_contract_month.was_payed false,
   :selected? true})

(defn- initial-test-state-template [test-state]
  (reset!
   test-state
   {:enterprises-m        (atom {})
    :contracts-m          (atom {})
    :subcontracts-m       (atom {})
    :currency             "UAH"
    :subcontracts-payment-state (atom {})}))

(defn- initialize-test-state [state!]
  (reset! (:enterprises-m  (state!)) test-enterprises-m)
  (reset! (:contracts-m    (state!)) test-contracts-m)
  (reset! (:subcontracts-m (state!)) test-subcontracts-m))

(def test-state (atom {}))
(defn test-state! [] (deref test-state))
(println "\nInitialize Tests:")
(initial-test-state-template test-state)
(initialize-test-state test-state!)

(deftest state-modifying-functions
  (testing "Selecting and unselecting"
    (is (= (plugins.service-period.service-period/select-all-subcontracts test-state! [:1 :21] true)  ok-subcontracts-all-selected-select))
    (is (= (plugins.service-period.service-period/select-all-subcontracts test-state! [:1 :21] false) ok-subcontracts-all-selected-unselect))
    (is (do (plugins.service-period.service-period/select-checkbox-for-subcontract test-state! [:1 :21 :1022] true)
            (= ok-subcontract-selected (get-in @(:subcontracts-m (test-state!)) [:1 :21 :1022]))))
    (is (do (plugins.service-period.service-period/select-checkbox-for-subcontract test-state! [:1 :21 :1022] false)
            (= false (get-in @(:subcontracts-m (test-state!)) [:1 :21 :1022 :selected?]))))
    (is (do (plugins.service-period.service-period/select-all-contracts test-state! [:2] true)
            (= true (plugins.service-period.service-period/all-contracts-selected? test-state! [:2]))))
    (is (do (plugins.service-period.service-period/select-all-contracts test-state! [:2] false)
            (= false (plugins.service-period.service-period/all-contracts-selected? test-state! [:2])))))
  
  (testing "Check selected"
    (is (= true (plugins.service-period.service-period/check-if-contract-selected test-state! [:3 :2])))
    (is (= false (plugins.service-period.service-period/check-if-contract-selected test-state! [:1 :21])))
    (is (do (plugins.service-period.service-period/select-checkbox-for-subcontract test-state! [:1 :21 :1022] true)
            (= [:1 :21 :1022] (first (plugins.service-period.service-period/listing-all-selected-checkboxes-path test-state!))))))

  (testing "Payment"
    (is (do (plugins.service-period.service-period/pay-for-pointed-subcontracts test-state! [:1 :21 :1022])
            (get-in @(:subcontracts-m (test-state!)) [:1 :21 :1022 :service_contract_month.was_payed])))
    (is (do (plugins.service-period.service-period/pay-for-selected-subcontracts test-state! [[:1 :21 :1023] [:1 :23 :1031]])
            (and (get-in @(:subcontracts-m (test-state!)) [:1 :21 :1023 :service_contract_month.was_payed])
                 (get-in @(:subcontracts-m (test-state!)) [:1 :23 :1031 :service_contract_month.was_payed]))))
    (is (= true  (plugins.service-period.service-period/contract-payed? test-state! [:1 :21])))
    (is (= false (plugins.service-period.service-period/contract-payed? test-state! [:2 :25])))
    (is (= true  (plugins.service-period.service-period/all-contracts-payed? test-state! [:3])))
    (is (= false (plugins.service-period.service-period/all-contracts-payed? test-state! [:1])))))

(deftest logic-functions-based-on-state
  (testing "Payment calculating"
    (is (= 400.0 (plugins.service-period.service-period/calculate-contract-price test-state! [:1 :21])))
    (is (= 0     (plugins.service-period.service-period/calculate-contract-price test-state! [:3 :22])))
    (is (= 40.0  (plugins.service-period.service-period/calculate-all-contract-price test-state! [:2])))
    (is (= 0     (plugins.service-period.service-period/calculate-all-contract-price test-state! [:3])))))


;; (deftest simple-test-1
;;   (testing "Test 1"
;;     (is (= (+ 2 2) 4))
;;     (is (= (- 2 2) 0))
;;     (is (= (* 2 2) 8)))
;;   (testing "Test 2"
;;     (is (string? "a"))))

;; (deftest simple-test-2
;;   (testing "Test 3"
;;     (is (float? "10"))))
