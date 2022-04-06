;; # TO DELETE
(ns jarman.logic.metadata-toolbox
  (:require [clojure.string :as string]))

;;;;;;;;;;;;;;;;;;;;;;
;;; RULE FILTRATOR ;;;
;;;;;;;;;;;;;;;;;;;;;;

;;; pattern table matching configuration
;;; mean that column with this name never
;;; being added to metadata information.
;;; The same with table in *meta-rules*
(def ^{:dynamic true} *id-collumn-rules* ["id", "id*"])
(def ^{:dynamic true} *meta-rules* ["metatable" "meta*"])

(defn not-allowed-rules
  "Description:
    The function do filter on `col` list, selected only that string elements, which not allowed by `rule-spec`.

  Rule Spec:
    Rule spec is simple string with simple declaration. He serve three type of rule:
    - 'metadata' (whole word)
    - 'meta*' (words, wich started on \"meta\")
    - '*data' (words, wich end on \"*data\")

  Example using:
    ;; in case if `col` is list, not string - return list of good patterns
    (not-allowed-rules [\"dupa\" \"_pri*\"] [\"dupa\" \"_PRI\" \"_Private\" \"something\"])
      ;;=> (\"something\")
    (not-allowed-rules \"_pri*\" [\"dupa\" \"_PRI\" \"_Private\" \"something\"])
      ;;=> (\"something\" \"dupa\")
  
    ;; in case if `col` is string return boolean 
    (not-allowed-rules [\"dupa\" \"_pri*\"] \"lala\")
      ;;=> true
    (not-allowed-rules \"_pri*\" \"_PRIVATE\" )
      ;;=> false
    
  See related:
    (`jarman.logic.metadata-toolbox/allowed-rules`)"
  [rule-spec col]
  (let [rule-spec (if (string? rule-spec) [rule-spec] rule-spec)
        f-comp (fn [p] (condp = (.indexOf (seq p) \*)
                         (dec (count p)) #(not= (butlast p) (take (dec (count p)) (string/lower-case %)))
                         0               #(not= (drop 1 p) (take-last (dec (count p)) (string/lower-case %)))
                         #(not= p %)))
        preds (map f-comp rule-spec)]
    (if (string? col) (reduce (fn [a p?] (and a (p? col))) true preds)
        (filter (fn [s] (reduce (fn [a p?] (and a (p? s))) true preds)) col))))

(defn allowed-rules
  "Description:
    The function do filter on `col` list, selected only that string elements, which allowed by `rule-spec`.

  Rule Spec:
    Rule spec is simple string with simple declaration. He serve three type of rule:
    - 'metadata' (whole word)
    - 'meta*' (words, wich started on \"meta\")
    - '*data' (words, wich end on \"*data\")

  Example using:
    ;; in case if `col` is list, not string - return list of good patterns
    (allowed-rules [\"dupa\" \"_pri*\"] [\"dupa\" \"_PRI\" \"_Private\" \"something\"]
      ;;=> (\"dupa\" \"_PRI\" \"_Private\")
    (allowed-rules \"_pri*\" [\"dupa\" \"_PRI\" \"_Private\" \"something\"])
      ;;=> (\"_PRI\" \"_Private\")

    ;; in case if `col` is string return boolean 
    (allowed-rules [\"dupa\" \"_pri*\"] \"lala\")
      ;;=> false
    (allowed-rules \"_pri*\" \"_PRIVATE\" )
      ;;=> true

  See related:
    (`jarman.logic.metadata-toolbox/not-allowed-rules`)"
  [rule-spec col]
  (let [rule-spec (if (string? rule-spec) [rule-spec] rule-spec)
        f-comp (fn [p] (condp = (.indexOf (seq p) \*)
                         (dec (count p)) #(= (butlast p) (take (dec (count p)) (string/lower-case %)))
                         0               #(= (drop 1 p) (take-last (dec (count p)) (string/lower-case %)))
                         #(= p %)))
        preds (map f-comp rule-spec)]
    (if (string? col) (reduce (fn [a p?] (or a (p? col))) false preds)
        (filter (fn [s] (reduce (fn [a p?] (or a (p? s))) false preds)) col))))
