(ns jarman.gui.gui-tutorials.regex-tutorial)

;; \w - [a-zA-Z]
;; \W - [^a-zA-Z]
;; \d - [0-9]
;; \D - [^0-9]
;; \s - space
;; \S - nonspace
;; . - any char
;; [] - one keys block
;; + - one or more
;; * - 0 or more
;; ? - 0 or 1
;; {0, 10} 
;; {2,}
;; {,20}
;; ^ - początek linii
;; $ - koniec linii
;; () - grupa
;;  | 

(re-matches #"\+\d+[\s-]?\d+[\s-]?[0-9]+" "+345556123")


(re-seq #"\+\d+[\s-]?\d+[\s-]?[0-9]+" "hello, my name staszek, take my number +567645657, ohhh its so cool number, take my +645678899... oooooooo, my girlfried also cool number +567678546")


(re-matches #"\{\s*['\"](\w+)['\"]\s*:\s*(['\"]\w+['\"]|\d+|true|false)\s*\}" "{     \"param\"      : \"true\"       }")
(re-matches #"\s*,\s*" "   , ")

(clojure.string/split)
(let [[_ in-param] (re-matches #"^\{(.*)\}$" "{'some':true    ,  \"suka\"    : \"bliat\",\"one\":123    }")
      in-param (clojure.string/trim in-param)]
  (map #(re-matches #"['\"](\w+)['\"]\s*:\s*(['\"]\w+['\"]|\d+|true|false)" %) (clojure.string/split in-param #"\s*,\s*")))



"<i>text</i>" =>"(i)text(i)"
(clojure.string/replace "some  <i>text</i> which i <hate>fdsjakl</hate>" #"</?(\w+)>" "($1)")
(clojure.string/replace "some  <i>text</i> which i <hate>fdsjakl</hate>" #"</?\w+>" "")

1s<sub>2</sub>

let table =
    {
        "Yttrium": "1s<sub>2</sub> 2s<sub>2</sub> 2p<sub>6</sub> 3s<sub>2</sub> 3p<sub>6</sub> 4s<sub>2</sub> 3d<sub>10</sub> 4p<sub>6</sub> 5s<sub>2</sub> 4d<sub>1</sub>",
        "Zirconium": "1s<sub>2</sub> 2s<sub>2</sub> 2p<sub>6</sub> 3s<sub>2</sub> 3p<sub>6</sub> 4s<sub>2</sub> 3d<sub>10</sub> 4p<sub>6</sub> 5s<sub>2</sub> 4d<sub>2</sub>",
        "Niobium": "1s<sub>2</sub> 2s<sub>2</sub> 2p<sub>6</sub> 3s<sub>2</sub> 3p<sub>6</sub> 4s<sub>2</sub> 3d<sub>10</sub> 4p<sub>6</sub> 5s<sub>2</sub> 4d<sub>2</sub>",
        "Molybdenum": "1s<sub>2</sub> 2s<sub>2</sub> 2p<sub>6</sub> 3s<sub>2</sub> 3p<sub>6</sub> 4s<sub>2</sub> 3d<sub>10</sub> 4p<sub>6</sub> 5s<sub>1</sub> 4d<sub>5</sub>",
        "Technetium": "1s<sub>2</sub> 2s<sub>2</sub> 2p<sub>6</sub> 3s<sub>2</sub> 3p<sub>6</sub> 4s<sub>2</sub> 3d<sub>10</sub> 4p<sub>6</sub> 5s<sub>2</sub> 4d<sub>5</sub>",
        "Ruthenium": "31s<sub>2</sub> 2s<sub>2</sub> 2p<sub>6</sub> 3s<sub>2</sub> 3p<sub>6</sub> 4s<sub>2</sub> 3d<sub>10</sub> 4p<sub>6</sub> 5s<sub>1</sub> 4d<sub>7</sub>",
    }
