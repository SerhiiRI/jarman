(ns jarman.logic.aleks-playground2
  (:require
   [clojure.spec.alpha :as s]))


;; (def --test-str string?)
;; (def --test-str-empty (fn [x] (and (string? x) 
;;                                    (not-empty x))))
;; (def --test-str-A (fn [x] (and (--test-str x) 
;;                                (= (first x) \A))))
;; (def --test-str-http (fn [x] (and (re-matches #"https?://.+" x) (--test-str-empty x))))

;; (defn connetion-to-some-url [url]
;;   {:pre [(--test-str-http url)]}
;;   (format "Connection to the url: %s" url))

;; (connetion-to-some-url "dupa")
;; (connetion-to-some-url "https://dupa")

;; (--test-str "dupa")
;; (--test-str-empty "Ala")
;; (--test-str-A "Ala")
;; (--test-str-A 'dupa)
;; (--test-str-http "https://Ala")


(s/def :our-test/string string?)
(s/def :our-test/string-ne (s/and :our-test/string not-empty))
(s/def :our-test/url (s/and :our-test/string-ne #(re-matches #"https?://.+" %)))
(s/def :our-test/url-list (s/coll-of :our-test/url))

(s/valid? :our-test/string-ne "3211")

(s/def :our-test/http-protocol  #{'ftp 'http 'https})
(s/valid? :our-test/http-protocol 'ftp)

(s/def :our-test/every-string-ne (s/coll-of :our-test/string-ne))
(s/valid? :our-test/every-string-ne ["dupa" "dupa" "dupa"])
(s/valid? :our-test/url "https://asd")
(s/valid? :our-test/url-list ["https://asd" "https://asd"])
(s/conform :our-test/url-list [ "https://asd" "https://asd"])
(s/explain :our-test/url-list [ "https://localhost" "ftp://dupa" "" 3])


(s/def :our-test/keyword keyword?)
(s/def :our-test/witch-one
 (s/or :keyword-pattern :our-test/keyword
       :url-pattern :our-test/url))

(s/def :our-test/witch-one-2
 (s/or :keyword-list (s/coll-of :our-test/keyword)
       :str-based (s/or :url-pattern :our-test/url
                        :str-pattern :our-test/string)))

(s/valid? :our-test/witch-one "https://qasd")
(s/conform :our-test/witch-one :dupa)

(s/conform :our-test/witch-one-2 "strin")


(s/def :our-test/login :our-test/string-ne)
(s/def :our-test/password :our-test/string-ne)
(s/def :our-test/profile-url :our-test/url)

(s/def :our-test/user
       (s/keys :req-un [:our-test/login
                        :our-test/password]
               :opt-un [:our-test/profile-url
                        :our-test/witch-one-2]))

(s/valid? :our-test/user {:login "admin" :password "admin" :profile-url "https://admin"})
(s/conform :our-test/user {:login "admin" :password "pass" :witch-one-2 "dupa"})


(s/def :our-test/our-cat (s/cat :function #{'+ '- '= 'and 'or}
                                :arg1 number?
                                :arg2 number?
                                :arg3 number?))

(s/conform :our-test/our-cat '(= 1 2 3))


(s/def :our-test/our-cat (s/cat :function #{'+ '- '= 'and 'or}
                                :args (s/+ (s/or :num number? :str string?))
                                ))

(s/conform :our-test/our-cat '(+ "a" 1 "b"))


;; (select (from table) (where (= id 2)))
;; (select table (where (= id 2)))
;; (select (from table))
;; (select table)


(s/def :our-test/from-table (s/cat :start #{'from} :tab symbol?))
;; (s/conform :our-test/from-table '(from table))

(s/def :our-test/cond (s/cat :cond #{'=} :column symbol? :val number?))
;; (s/conform :our-test/cond '(= id 2))

(s/def :our-test/where (s/cat :where #{'where} 
                              :what (s/and sequential? :our-test/cond)))
;; (s/conform :our-test/where '(where (= id 2)))

;; (s/def :our-test/select-1 (s/cat :start #{'select} :tab symbol?))
;; (s/conform :our-test/select-1 '(select table))

;; (s/def :our-test/select-2 (s/cat :query-start #{'select}
;;                                  :query-table (s/or
;;                                                :tab (s/and sequential? :our-test/from-table)
;;                                                :tab symbol?)))
;; (s/conform :our-test/select-2 '(select (from table)))

(s/def :our-test/select-3 (s/cat :query-start #{'select}
                                 :query-table (s/or
                                               :tab (s/and sequential? :our-test/from-table)
                                               :tab symbol?)
                                 :query-where (s/* (s/and sequential? :our-test/where))))
(s/valid? :our-test/select-3 '(select (from table) (where (= id 2))))
(s/valid? :our-test/select-3 '(select from (where (= id 2))))
(s/valid? :our-test/select-3 '(select table (where (= id 2))))
(s/valid? :our-test/select-3 '(select table))