(ns playground
  (:require [clojure.spec.alpha :as s]))

(s/def :some-test/string string?)
(s/def :some-test/long-string (s/and :some-test/string #(> (count %) 10)))
(s/def :some-test/url (s/and :some-test/long-string
                             (partial 
                              re-matches #"https?://.+")))
(s/def :some-test/url-list (s/coll-of :some-test/url))

(s/valid? :some-test/url-list ["https://sgfdgdfgdsg"
                               "sdfsdfsdf"])

(s/explain :some-test/url-list ["https://sgfdgdfgdsg"
                                ])

(s/explain :some-test/url "sfs")
(s/explain :some-test/url "sfszfgsgsgg")

(s/explain :some-test/url "sfszfgsgsgg")

(s/def :some-test/kwd keyword?)
(s/def :some-test/kwd-or-url (s/or :my-kwd (s/or :kwd-url
                                                 
                                                 #(and keyword?
                                                       (= (apply str (take 4 (name %)))  "http"))
                                                 :simple keyword?)
                                   :my-url :some-test/url))

(s/valid? :some-test/kwd-or-url :f)

(if (s/invalid? (s/conform :some-test/url :afsdg)) "yes")
(s/conform :some-test/kwd-or-url "https://sadf")
;; => [:my-url "https://sadf"]
(s/conform :some-test/kwd-or-url :afsdgv)
;; => [:my-kwd :afsdgv]

(defn some-func [url-add]
  {:pre [(s/valid? :some-test/url url-add)]}
  url-add)

(s/def :some-block/login :some-test/string)
(s/def :some-block/password :some-test/long-string)
(s/def :some-block/telephone (s/and :some-test/string
                                    #(= \+ (first (take 1 %)))))
;;(s/def :some-block/login :some-test/string)
(s/def :some-block/user
  (s/keys :req-un [:some-block/login
                   :some-block/password]
          :opt-un [:some-block/telephone]))

(s/explain :some-block/user {:login "Julka"
                             :password "heyyyysfgsg"
                             :telephone "+2"})

(s/def :some-test/and-cat (s/cat :function #{'and 'or 'eee}
                                 :args (s/*
                                        (s/or :num number?
                                              :str string?))
                                 ;; :arg1 number?
                                 ;; :arg2 number?
                                 ;; :arg3 number?
                                 ))

(s/conform :some-test/and-cat '(and 1 2 3))
;; => {:function and, :args [[:num 1] [:num 2] [:num 3]]}

(s/conform :some-test/and-cat '(and 1 2 "s"))

(s/conform :some-test/and-cat '(and ))
;; => {:function and}

(s/def :test-user/list list?)
(s/def :test-user/from-list (s/and :test-user/list
                                   #(= (first %) 'from)))

(s/def :test-user/string (s/cat :function #{'select}
                                :args (s/or :s symbol?
                                            :l :test-user/from-list)))

;; (s/def :test-user/testu (s/or :some-test/string
;;                               ))

(s/conform :test-user/string '(select user))
