(ns jarman.logic.aleks-playground
  (:refer-clojure :exclude [update])
  (:require
   [clojure.data :as data]
   [clojure.string :as string]
   [jarman.logic.connection :as db]
   [jarman.logic.sql-tool :as toolbox :include-macros true :refer :all]
   [jarman.logic.metadata :as metadata]
   [jarman.lang :refer :all])
  (:import (java.util Date)
           (java.text SimpleDateFormat)))



;; ┌──────────────────────┐
;; │                      │
;; │ Zarządzanie tabelami │
;; │                      │
;; └──────────────────────┘

(db/query (select {:table-name :user
                   :where [:and
                           [:= :login "aleks"]]
                        ;;    [:= :password "1234"]
                           
                   :inner-join {:permission :id_permission}}))
                ;;    :column [:login :password {:permission_name :pn}] ;; here i can chage :permission_name to :pn, some like alias

(db/exec (insert :user :set 
                 {
                  :login "aleks"
                  :password "1234"
                  :first_name "Aleks"
                  :last_name "S"
                  :id_permission 1}))

(db/exec (update :user
                 :where [:= :login "aleks"]
                 :set {:password "qwerty"}))

(let [id (get (first (db/query (select {:table-name :user
                                        :where [:= :login "aleks"]
                                        :column [:id]}))) :id)]
  (db/exec (delete :user
                   :where [:= :id id])))

(db/exec (create-table
          :pepe
          :columns [{:frog_name [:varchar-40 :default :null]}]))

(db/exec (alter-table
          :pepe
          :add-column {:happy [:boolean]}))

(db/query (show-table-columns :pepe))


(db/exec (alter-table
          :pepe
          :drop-column :happy))

(db/exec (drop-table
          :pepe))

(db/query (show-tables))


;; ┌─────────────────────────┐
;; │                         │
;; │ Tymczasowa dokumentacja │
;; │                         │
;; └─────────────────────────┘

;;
;; ## Regeneracja bazy ##
;;
;; First create DB with terminal if not exist:
;; $ mariadb -u root -p
;; >show database;
;; >SELECT User FROM mysql.user;
;; >create database jarman;
;; >GRANT USAGE ON jarman.* TO 'root'@'%' IDENTIFIED BY 'mypassword';
;; >GRANT ALL privileges ON `jarman`.* TO 'root'@'%';
;; >FLUSH PRIVILEGES;
;; >show grants for root@'%';
;;
;; W pliku 'playground.clj' jest kilka zebranych funkcji, które to umożliwią
;; a zgrupowano je do funkcji (tylko jej nie wywołuj!)
;;
;; (defn regenerate-scheme []
;;   (sinit/procedure-test-all)
;;   (delete-scheme)
;;   (create-scheme)
;;   (metadata/do-create-meta-database)
;;   (metadata/do-create-references))
;;
;; (delete-scheme) usuwa zawartość bazy
;; (sinit/procedure-test-all) generuje podstawowe tabele dla aplikacji jarman
;; (create-scheme) generuje tabele pozostałe
;; (metadata/do-create-meta) generuje meta informacje
;; (metadata/do-create-references)) uzupełnia powiązania między tabelami
;;
;; structure_initializer.clj odpowiada za wygenerowanie podstawowych tabel
;; dla jarmana funkcją (sinit/procedure-test-all)



;; ## Dodanie tabeli ##
;; W pliku 'playground.clj' można dodać nową tabelę, a jej schemat wygląda tak:
;;
;; (def user
;;   (create-table :user
;;                 :columns [{:login [:varchar-100 :nnull]}
;;                           {:password [:varchar-100 :nnull]}
;;                           {:first_name [:varchar-100 :nnull]}
;;                           {:last_name [:varchar-100 :nnull]}
;;                           {:id_permission [:bigint-20-unsigned :nnull]}]
;;                 :foreign-keys [{:id_permission :permission} {:delete :cascade :update :cascade}]))
;;
;; Pamiętaj aby dodaną tabelę dodać do create-scheme i delete-scheme zachowując 
;; odpowiednią kolejność referencji



;; ## Dane testowe ##
;; By stworzyć dane testowe wzorój się na poniższych funkcjach zaczynających się na fill-...



;; ## Usuwanie tabeli ##
;; Czyli usunięcie schematu bazy funkcją (delete-scheme) z 'playground.clj'
;; Następnie wymazanie powiązanych z nią kodów z 'playground.clj'
;; Regeneracja bazy ta jak opisano to wyżej 


