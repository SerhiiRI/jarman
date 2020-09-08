(ns jarman.logic.some-pattern-tool
  (:refer-clojure :exclude [update])
  (:require
   [clojure.string :as string]))

(def titles ["BZT<sub>5</sub>"
  "ChZT <sub>Cr</sub>"
  "ChZT <sub>Cr</sub>"
  "Suma jonów chlorków i siarczanów"
  "Fenole lotne"
  "arsen"
  "chrom"
  "cynk"
  "kadm"
  "mied¼"
  "nikiel"
  "o³ów"
  "rtêæ"
  "srebro"
  "wanad"
  "heksachlorocykloheksan (HCH)"
  "tetrachlorometan (czterochlorek wêgla - CCl<sub>4</sub>)"
  "pentachlorofenol (PCP)"
  "aldryna, dieldryna, endryna, izodryna"
  "heksachlorobenzen (HCB)"
  "heksachlorobutadien (HCBD)"
  "trichlorometan (chloroform - CHCl<sub>3</sub>)"
  "1,2-dichloroetan (EDC)"
  "trichloroetylen (TRI)"
  "tetrachloroetylen (nadchloroetylen - PER)"
  "trichlorobenzen (TCB)"])

(let [x (map (fn [n s] [(str n) s]) (range 1 27) titles)]
  (def s-5 (take 5 (vec x)))
  (def s-5-15 (take 10 (drop 5 x)))
  (def s-15+ (drop 15 x)))



(defn load-to-file
  ([dimension template-file]
   (load-to-file dimension "tmp.html"))
  ([dimension template-file]
   (do (spit ".\\src\\jarman\\logic\\o.html" "")
       (doall (map #(do
                      (spit ".\\src\\jarman\\logic\\o.html" % :append true)
                      (spit ".\\src\\jarman\\logic\\o.html" "\n" :append true))
                   (for [[n s] dimension]
                     (let [tmp (slurp ".\\src\\jarman\\logic\\tmp.html")]
                       (-> tmp
                           (clojure.string/replace #"%s" n)
                           (clojure.string/replace #"%t" s)))))))))

;; (load-to-file s-5)
;; (load-to-file s-5-15)
;; (load-to-file s-15+)

;; (map #'str (.listFiles (clojure.java.io/file "./")))
;; (spit ".\\src\\jarman\\logic\\o.html" (clojure.string/replace (slurp ".\\src\\jarman\\logic\\tmp.html") #"%s" "1"))
;; (clojure.string/replace (slurp ".\\src\\jarman\\logic\\t.html") #"%s" "1")
