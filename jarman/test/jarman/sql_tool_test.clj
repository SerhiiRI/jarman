(ns jarman.sql-tool-test
  (:require
   [clojure.string :as string]
   [clojure.test :refer :all]
   [jarman.sql-tool :as toolbox :include-macros true :refer :all]))

;;;;;;;;;;;;;;;;;;;;
;;; test helpers ;;;
;;;;;;;;;;;;;;;;;;;;

(defmacro to-test [x]
  (let [a (eval x)]
    `(quote (~(symbol 'is) (~(symbol '=) ~a ~x)))))

(defmacro to-test-wrapp [& body]
  (let [x (map #(list 'to-test %) body)]
    `'~x))

(defmacro to-test-expand [& body]
  (let [x (map #(eval %) (eval `(to-test-wrapp ~@body)))]
    `'~x))

(defmacro to-test-expand-testing [& body]
  (let [x (eval `(to-test-expand ~@body))]
    `'~(concat (list 'testing "<empty description>") x)))

(def re-id-pattern #"`([a-zA-Z]+)+(\d)+`")
(def re-num-pattern #"`(\d)+`")

(defn replace-id [sql-string]
  {:pre [(not-empty sql-string)]}
  (string/replace sql-string re-id-pattern "-"))

(defn replace-num [sql-string]
  {:pre [(not-empty sql-string)]}
  (string/replace sql-string re-num-pattern "-"))

;;;;;;;;;;;;;;;;;;
;;; test block ;;;
;;;;;;;;;;;;;;;;;;

(deftest helper-function-test
  (testing "Testing helper functions"
    (is (= (pair-where-pattern :k 'v) "k=v"))
    (is (= (pair-where-pattern :k "v") "k=\"v\""))
    (is (= (tkey :a.b.c.d) ["a" "b" "c" "d"]))))

(deftest joining-test
  (testing "Getting joining lambda by joining"
    (is (= "A ON A.id=B.id_a"
           (let [t "B"
                 j "A"
                 f (get-function-by-join-type j)] (f t j))))
    (is (= "A ON A.id=B.id_a"
           (let [t "B"
                 j :A
                 f (get-function-by-join-type j)] (f t j))))
    (is (= ["A ON A.id=B.id_a" "C ON C.id=B.id_c"]
           (let [t "B"
                 j {:A :id_a :C :id_c}
                 f (get-function-by-join-type j)]
             (map #(f t %1) j))))
    (is (= ["B ON B.id=A.id_b" "C ON C.id=A.id_c"]
           (let [t "A"
                 j [:B :C]
                 f (get-function-by-join-type j)]
             (map #(f t %1) j))))
    (is (= ["A ON A.id_to_b=B.id_b" "A ON A.id_to_c=C.id_C"]
           (let [t "A"
                 j {:A.id_to_b :B.id_b
                    :A.id_to_c :C.id_C}
                 f (get-function-by-join-type j)]
             (map #(f t %1) j))))
    (is (= ["B ON B.id=user.id_b" "C ON C.id=user.id_c"]
           (let [t "A"
                 j ["B ON B.id=user.id_b"
                    "C ON C.id=user.id_c"]
                 f (get-function-by-join-type j)]
             (map #(f t %1) j)))))
  (testing "Join functions in action"
    (is (= "SELECT * FROM user OUTER LEFT JOIN A ON A.id=user.id_a OUTER LEFT JOIN B ON B.id=user.id_b"
           (outer-left-join-string "SELECT * FROM user" {:A :id_a :B :id_b} "user")))
    (is (= "SELECT * FROM user INNER JOIN A ON A.idself=user.id_a INNER JOIN B ON B.idself=user.id_b"
           (inner-join-string "SELECT * FROM user" {:A.idself :user.id_a :B.idself :user.id_b} "user")))
    (is (= "SELECT * FROM user INNER JOIN A ON A.id=user.id_a INNER JOIN B ON B.id=user.id_b"
           (inner-join-string "SELECT * FROM user" [:A :B] "user")))
    (is (= "SELECT * FROM user INNER JOIN A ON A.id=user.id_a"
           (inner-join-string "SELECT * FROM user" :A "user")))
    (is (= "SELECT * FROM user INNER JOIN A ON A.id=user.id_a"
           (inner-join-string "SELECT * FROM user" "A" "user")))
    (is (= "SELECT * FROM user INNER JOIN A ON A.id=user.id_b INNER JOIN B ON B.id=user.id_b"
           (inner-join-string "SELECT * FROM user" ["A ON A.id=user.id_b" "B ON B.id=user.id_b"] "user")))))

(deftest column-test
  (testing "Column convertation to Sql expression"
      (is (= " first, first, FIRst, second.id AS first, second AS first, second.id AS first, second AS first FROM "
             (column-string "" [:first "first" "FIRst" {:second.id :first} [:second :first] {"second.id" "first"} ["second" "first"]] "")))))


(deftest where-processor
  (testing "Linking where logical block in one expression"
    (is (= (and-processor (= 1 2) (= 1 (- 2 3))) "1 = 2 AND 1 = -1"))
    (is (= (and-processor 1 2) "1 AND 2"))
    (is (= (or-processor (= 1 2) (= 1 (- 2 3))) "1 = 2 OR 1 = -1"))
    (is (= (or-processor 1 2) "1 OR 2")))
  (testing "testing block where with ()-rounded rules"
    (testing "testing rules"
      (is (= "(1, 2, 3)" (where-procedure-parser [1 2 3])))
      (is (= "num_c LIKE (1, 2, 3)" (where-procedure-parser (in :num_c [1 2 3]))))
      (is (= "num_c BETWEEN 0 AND 5 OR 4 = 31" (where-procedure-parser (or (between :num_c 0 (+ 2 3)) (= 4 31)))))
      (is (= "1 OR 2" (where-procedure-parser (or 1 2))))
      (is (= "1 > 2" (where-procedure-parser (> 1 2))))
      (is (= "date_c BETWEEN \"2010-01-01 00:00:00\" AND \"2030-01-01 00:00:00\"" (where-procedure-parser (between :date_c (date 2010) (date 2030)))))
      (is (= "symbol_c BETWEEN \"A\" AND \"B\"" (where-procedure-parser (between :symbol_c "A" "B"))))
      (is (= "num_c BETWEEN 2 AND 1" (where-procedure-parser (between :num_c 2 1))))
      (is (= "string_c LIKE \"some\"" (where-procedure-parser (like :string_c "some"))))
      (is (= "string_c < \"some\"" (where-procedure-parser (< :string_c "some"))))
      (is (= "1" (where-procedure-parser 1)))
      (is (= "3" (where-procedure-parser (+ 1 2))))
      (is (= "2" (where-procedure-parser 2)))
      (is (= "(1, 2, 3, 4)" (where-procedure-parser [1 2 3 4])))
      (is (= "1 = 2 OR 2 > 0" (where-procedure-parser (or (= 1 2) (> 2 (- 3 3)))))))
    (testing "Test more complex rules"
      (is (= "num_c = 1 OR str_c = \"bliat\" OR (num_c = 2 AND bool_c = false AND tem_c BETWEEN 0 AND 2)"
             (where-procedure-parser (or (= :num_c 1)
                                         (= :str_c "bliat")
                                         (and (= :num_c 2)
                                              (= :bool_c (= 1 "fuck"))
                                              (between :tem_c 0 2))))))
      (is (= "f1 = 1 OR f1 >= \"bliat\" OR (f2 > 2 AND f2 = \"fuck\" AND f1 BETWEEN 1 AND 1010 AND (suka = \"one\" OR one LIKE (1, 2, 3, 3)))"
             (where-procedure-parser (or (= :f1 1)
                                         (>= :f1 "bliat")
                                         (and (> :f2 2)
                                              (= :f2 "fuck")
                                              (between :f1 1 (+ 10 1000))
                                              (or (= :suka "one")
                                                  (in :one [1 2 3 (+ 1 2)])))))))))
  (testing "Map and string method for where block"
    (is (= " WHERE A.str_c=\"anatoli\" AND num_c=2 AND B.str_c=true" (where-string "" {:A.str_c "anatoli", :num_c 2, :B.str_c true} "")))
    (is (= " WHERE num_c = 12" (where-string "" "num_c = 12" "")))))

(deftest select-testing
  (testing "Test overloaded select cases"
   (is (= "SELECT name, dla_mamusi, CREDENTAIL.login FROM user-table INNER JOIN CREDENTIAL ON CREDENTIAL.id=user-table.is_user_metadata INNER JOIN METADATA ON METADATA.id=user-table.id_user_metadata RIGHT JOIN A1 ON A1.id_self=user.id_user_a1 RIGHT JOIN B1 ON B1.id_self=USER.id_user_b2 LEFT JOIN suka ON suka.id=user.id_suka LEFT JOIN dupa ON dupa.id=er.id_dupara OUTER LEFT JOIN suka ON suka.id=user-table.id_suka OUTER LEFT JOIN bliat ON bliat.id=user-table.id_bliat OUTER RIGHT JOIN credential ON credential.id=user-table.id_credential WHERE CREDENTAIL.login=\"XXXpussy_destroyer69@gmail.com\" AND CREDENTAIL.password=\"Aleksandr_Bog69\" AND name=\"Aleksandr\" AND dla_mamusi=\"Olek\" AND METADATA.merried=false"
          (select :user-table
                  :inner-join {:CREDENTIAL :is_user_metadata :METADATA :id_user_metadata}
                  :right-join {:A1.id_self :user.id_user_a1 :B1.id_self :USER.id_user_b2}
                  :left-join ["suka ON suka.id=user.id_suka" "dupa ON dupa.id=er.id_dupara"]
                  :outer-left-join [:suka :bliat]
                  :outer-right-join :credential
                  :column [:name :dla_mamusi :CREDENTAIL.login]
                  :where {:CREDENTAIL.login "XXXpussy_destroyer69@gmail.com"
                          :CREDENTAIL.password "Aleksandr_Bog69"
                          :name "Aleksandr"
                          :dla_mamusi "Olek"
                          :METADATA.merried false})))
   (is (= "SELECT name, dla_mamusi, CREDENTAIL.login FROM user-table INNER JOIN CREDENTIAL ON CREDENTIAL.id=user-table.is_user_metadata INNER JOIN METADATA ON METADATA.id=user-table.id_user_metadata RIGHT JOIN A1 ON A1.id_self=user.id_user_a1 RIGHT JOIN B1 ON B1.id_self=USER.id_user_b2 LEFT JOIN suka ON suka.id=user.id_suka LEFT JOIN dupa ON dupa.id=er.id_dupara OUTER LEFT JOIN suka ON suka.id=user-table.id_suka OUTER LEFT JOIN bliat ON bliat.id=user-table.id_bliat OUTER RIGHT JOIN credential ON credential.id=user-table.id_credential WHERE f1 = 1 OR f1 >= \"bliat\" OR (f2 > 2 AND f2 = \"fuck\" AND f1 BETWEEN 1 AND 1010 AND (suka = \"one\" OR one LIKE (1, 2, 3, 3)))"
          (select :user-table
                  :inner-join {:CREDENTIAL :is_user_metadata :METADATA :id_user_metadata}
                  :right-join {:A1.id_self :user.id_user_a1 :B1.id_self :USER.id_user_b2}
                  :left-join ["suka ON suka.id=user.id_suka" "dupa ON dupa.id=er.id_dupara"]
                  :outer-left-join [:suka :bliat]
                  :outer-right-join :credential
                  :column [:name :dla_mamusi :CREDENTAIL.login]
                  :where (or (= :f1 1)
                             (>= :f1 "bliat")
                             (and (> :f2 2)
                                  (= :f2 "fuck")
                                  (between :f1 1 (+ 10 1000))
                                  (or (= :suka "one")
                                      (in :one [1 2 3 (+ 1 2)])))))))))


(deftest inser-update-delete
  (testing "set rule"
    (is (= " SET a=1, b=2, c=3" (set-string "" {:a 1, :b 2, :c 3} "a"))))
  (testing "values rule"
    (is (= " a SET a=1, b=2, c=3" (values-string "" {:a 1, :b 2, :c 3} "a")))
    (is (= " a VALUES (1, 2, 3), (4, 5, 6)" (values-string "" [{:a 1, :b 2, :c 3} {:a 4, :b 5, :c 6}] "a")))
    (is (= " a VALUES (1, 1, 1), (2, 2, 2)" (values-string "" [[1 1 1] [2 2 2]] "a")))
    (is (= " a VALUES (1, 1, 1)" (values-string "" [1 1 1] "a"))))
  (testing "whole update clause"
    (is (= "UPDATE user SET id=null, num_c=1, str_c=\"some\"" (update :user :set {:id nil, :num_c 1, :str_c "some"}))))
  (testing "whole insert clause"
    (is (= "INSERT INTO user SET id=1, str1_c=\"vasia\", str2_c=\"123\", num_c=20" (insert :user :values {:id 1, :str1_c "vasia", :str2_c "123", :num_c 20})))
    (is (= "INSERT INTO user VALUES (1, \"vasia\", \"123\", 20), (2, \"vasia\", \"123\", 20)" (insert :user :values [[1 "vasia" "123" 20] [2 "vasia" "123" 20]]))))
  (testing "whole delete clause"
    (is (= "DELETE FROM table-name" (delete :table-name)))
    (is (= "DELETE FROM table-name WHERE id = 1" (delete :table-name :where (= :id 1))))))

(deftest table-definition
  (testing "Column table rules"
    (is (= "`id` BIGINT(20) NOT NULL AUTO_INCREMENT" (create-column {:id [:bigint-20 "NOT NULL" :auto]})))
    (is (= "`id` bigint(20) NOT NULL AUTO_INCREMENT" (create-column {:id "bigint(20) NOT NULL AUTO_INCREMENT"})))
    (is (= "`id` BIGINT(20) NOT NULL AUTO_INCREMENT" (create-column {:id [:bigint-20 :nnull :auto]})))
    (is (= "`id` BIGINT(290) SIGNED" (create-column {:id :bigint-290-signed}))))
  (testing "table configuration"
    (is (= ") ENGINE=MEMORY DEFAULT CHARSET=utf16 COLLATE=utf16_general_ci;"
           (table-config-string "" {:engine "MEMORY", :charset "utf16", :collate "utf16_general_ci"} ""))))
  (testing "create-table expression testing"
    (is (= "CREATE TABLE IF NOT EXISTS `table` (`id` BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT, `name` VARCHAR(100) NULL, `some-int` INTEGER(200), `id_other_table` BIGINT(20), PRIMARY KEY (`id`), KEY - (`id_other_table`), CONSTRAINT - FOREIGN KEY (`id_other_table`) REFERENCES `other_table` (`id`) ON DELETE SET NULL ON UPDATE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;"
           (replace-id (create-table :table
                                     :columns [{:name [:varchar-100 :null]} {:some-int :integer-200} {:id_other_table :bigint-20}]
                                     :foreign-keys [{:id_other_table :other_table} {:update :cascade, :delete :null}]
                                     :table-config {:engine "InnoDB", :charset "utf8"})))))
  (testing "Foreign key builder"
    (is (= ", CONSTRAINT - FOREIGN KEY (id_permission) REFERENCES" (foreign-keys-string "" "CONSTRAINT - FOREIGN KEY (id_permission) REFERENCES" "")))
    (is (= ", KEY - (`id_permission`), CONSTRAINT - FOREIGN KEY (`id_permission`) REFERENCES `permission` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE"
           (replace-num (foreign-keys-string "" [{:id_permission :permission} {:update :cascade, :delete :restrict}] ""))))
    (is (= ", KEY - (`id_permission`), CONSTRAINT - FOREIGN KEY (`id_permission`) REFERENCES `permission` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE, KEY - (`id_chujnia`), CONSTRAINT - FOREIGN KEY (`id_chujnia`) REFERENCES `chujnia` (`id`)"
           (replace-num (foreign-keys-string "" [[{:id_permission :permission} {:update :cascade, :delete :restrict}] [{:id_chujnia :chujnia} {:update :nset, :delete :restricted}]] ""))))))

(deftest rule-manipulation
  (testing "testing changing rules for sql expression"
    (is (= "SELECT * FROM user ORDER BY suka ASC" (eval (change-expression (quote (select :user)) :order [:suka :asc]))))
    (is (= "SELECT column, blait FROM user WHERE 1 = 2 AND registration BETWEEN 1 AND 19 ORDER BY column ASC"
           (eval (-> '(select :user :where (= 1 2))
                     (change-expression :where (quote (between :registration 1 19)))
                     (change-expression :column [:column :blait])
                     (change-expression :order [:column :asc])))))))

(deftest alter-table-clause
  (testing "testing alter-table clause"
    (is (= "ALTER TABLE `user` DROP COLUMN `bliat`;" (alter-table :user :drop-column :bliat)))
    (is (= "ALTER TABLE `user` DROP FOREIGN KEY bliat;" (alter-table :user :drop-foreign-key :bliat)))
    (is (= "ALTER TABLE `user` ADD `suka` TINYINT(1);" (alter-table :user :add-column {:suka [:boolean]})))
    (is (= "ALTER TABLE `user` ADD CONSTRAINT - FOREIGN KEY (id_permission) REFERENCES `permission` (`id`) ON UPDATE CASCADE;"
           (replace-id (alter-table :user :add-foreign-key [{:id_permission :permission} {:update :cascade}]))))))






