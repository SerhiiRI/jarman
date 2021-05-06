
;; ("\u1087")
;; \par
;; ssnameee\par
;; \par
;; ss\u1087\'3f\u1088\'3f\u1080\'3f\u1074\'3f\u1110\'3f\u1090\'3fee}

(slurp "C:\\Users\\serhii\\Desktop\\привіт.rtf")


(re-seq #"ss[\p{L}\p{IsCyrillic}'\\\d\. ]+ee" "highlight0 sschuj аівоф \\u1087\\'3f\\u1088\\'3f\\u1080\\'3f\\u1074\\'3f\\u1110\\'3f\\u1090\\'3fee}
0 sschuj \\u1087\\'3f\\u1088\\'3f\\u1080\\'3f\\u1074\\'3f\\u1110\\'3f\\u1090\\'3f   ee")
;; (re-matches "ss\u1087\'3f\u1088\'3f\u1080\'3f\u1074\'3f\u1110\'3f\u1090\'3fee")

(map (partial process-template identity) 
     (doall (re-seq #"\(\([\p{L}\p{IsCyrillic}\u8217'\\\d\.\s]+\)\)" 
                    (string/replace (slurp "C:\\Users\\serhii\\Desktop\\user.rtf") #"\r\n." "."))))
;; (re-seq #"\(\([\p{L}\p{IsCyrillic}\u8217'\\\d\.\s]+\)\)" 
;;         (string/replace (slurp "C:\\Users\\serhii\\Desktop\\привіт.rtf") #"\r\n." "."))

(re-seq #"\(\(.+\)\)" 
        (slurp "C:\\Users\\serhii\\Desktop\\user.rtf"))



;; => ("Користувач.пароль" "Wingardium.lewiosąąąąąą")

(slurp "C:\\Users\\serhii\\Desktop\\привіт.rtf")
(doall (re-seq #"ss[\p{L}\p{IsCyrillic}\u8217'\\\d\.\r\n ]+ee" 
               "ss\\u1050\\'3f\\u1086\\'3f\\u1088\\'3f\\u1080\\'3f\\u1089\\'3f\\u1090\\'3f\\u1091\\'3f\\u1074\\'3f\\u1072\\'3f\\u1095\\'3f\r\n.\\u1110\\'3f\\u1084\\'3f\\u8217\\'92\\u1103\\'3fee\\cell  "))





;; (doall (re-seq #"ss[\p{L}\p{IsCyrillic}\u8217'\\\d\. ]+ee" 
;;                     (slurp "C:\\Users\\serhii\\Desktop\\привіт.rtf")))




(re-seq
 #"ss.+ee"
 ;; #"ss[\p{L}'\\\d\.\s]+ee"
        " \\cell  

ss\\u1050\\'3f\\u1086\\'3f\\u1088\\'3f\\u1080\\'3f\\u1089\\'3f\\u1090\\'3f\\u1091\\'3f\\u1074\\'3f\\u1072\\'3f\\u1095\\'3f\r\n.\\u1110\\'3f\\u1084\\'3f\\u8217\\'92\\u1103\\'3fee\\cell 

ss\\u1050\\'3f\\u1086\\'3f\\u1088\\'3f\\u1080\\'3f\\u1089\\'3f\\u1090\\'3f\\u1091\\'3f\\u1074\\'3f\\u1072\\'3f\\u1095\\'3f.\\u1087\\'3f\\u1072\\'3f\\u1088\\'3f\\u1086\\'3f\\u1083\\'3f\\u1100\\'3fee\\cell\\  

ss\\u1050\\'3f\\u1086\\'3f\\u1088\\'3f\\u1080\\'3f\\u1089\\'3f\\u1090\\'3f\\u1091\\'3f\\u1074\\'3f\\u1072\\'3f\\u1095\\'3f.\\u1087\\'3f\\u1072\\'3f\\u1088\\'3f\\u1086\\'3f\\u1083\\'3f\\u1100\\'3fee")




;; (map (partial process-template identity) (re-seq #"ss[\p{L}'\\\d\.\r\n ]+ee" "ss\\u1050\\'3f\\u1086\\'3f\\u1088\\'3f\\u1080\\'3f\\u1089\\'3f\\u1090\\'3f\\u1091\\'3f\\u1074\\'3f\\u1072\\'3f\\u1095\\'3f.\\u1110\\'3f\\u1084\\'3f\\u8217\\'92\\u1103\\'3fee"))


(process-template identity "((\\u1050\\'3f\\u1086\\'3f\\u1088\\'3f\\u1080\\'3f\\u1089\\'3f\\u1090\\'3f\\u1091\\'3f\\u1074\\'3f\\u1072\\'3f\\u1095\\'3f.\\u1097\\'3f\\u1077\\'3f\\u1097\\'3f\\u1086\\'3f.u1089\\'3f\\u1100\\'3f))")
;; (map (partial process-template identity )
;;      '( "((\\u1050\\'3f\\u1086\\'3f\\u1088\\'3f\\u1080\\'3f\\u1089\\'3f\\u1090\\'3f\\u1091\\'3f\\u1074\\'3f\\u1072\\'3f\\u1095\\'3f.\\u1110\\'3f\\u1084\\'3f\\u8217\\'92\\u1103\\'3f))"
;;        "((\\u1050\\'3f\\u1086\\'3f\\u1088\\'3f\\u1080\\'3f\\u1089\\'3f\\u1090\\'3f\\u1091\\'3f\\u1074\\'3f\\u1072\\'3f\\u1095\\'3f.\\u1087\\'3f\\u1072\\'3f\\u1088\\'3f\\u1086\\'3f\\u1083\\'3f\\u1100\\'3f))" "((\\u1050\\'3f\\u1086\\'3f\\u1088\\'3f\\u1080\\'3f\\u1089\\'3f\\u1090\\'3f\\u1091\\'3f\\u1074\\'3f\\u1072\\'3f\\u1095\\'3f.\\u1097\\'3f\\u1077\\'3f\\u1097\\'3f\\u1086\\'3f.u1089\\'3f\\u1100\\'3f))"
;;        "((Wingardium.lewios\\u261\\'b9\\u261\\'b9\\u261\\'b9\\u261\\'b9\\u261\\'b9\\u261\\'b9))"))





;; "highlight0 sschuj аівоф \\u1087\\'3f\\u1088\\'3f\\u1080\\'3f\\u1074\\'3f\\u1110\\'3f\\u1090\\'3fee}0 sschuj \\u1087\\'3f\\u1088\\'3f\\u1080\\'3f\\u1074\\'3f\\u1110\\'3f\\u1090\\'3f   ee"
;; "ss\\u1087\\'3f\\u1088\\'3f\\u1080\\'3f\\u1074\\'3f\\u1110\\'3f\\u1090\\'3fee"
;; "ss&#1087;&#1088;&#1080;&#1074;&#1110;&#1090;ee"
(defn process-template [fn-template-handler template-string]
  (-> template-string
      ;; "ss&#1087;&#1088;&#1080;&#1074;&#1110;&#1090;ee"
      (string/replace #"\\|\.(?=u)" "")
      (string/replace #"(&#|u)(?=\d{3,4})" "<")
      (string/replace #"('[\d\w]{2}|;)" ">")
      (string/replace #"<(\d{3,4})>" (fn [[_ dec-unicode]] (Character/toString (Integer. dec-unicode))))
      (string/replace #"\(\((.*)\)\)" "$1")
      (string/trim)))

(Integer. "1090")
(println \u1087)
(Character/toString 1087)
(Character/valueOf "\u1087")
&#1025;
(Integer/toString 1087 16)



{"Користувач.ім'я" "Anton"}
"aa Користувач.ім'я ee"
