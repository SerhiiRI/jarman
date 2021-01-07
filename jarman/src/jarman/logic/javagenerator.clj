(ns jarman.logic.javagenerator
  (:refer-clojure :exclude [update])
  (:require
   [jarman.logic.sql-tool :as toolbox :include-macros true :refer :all]
   [clojure.data :as data]
   [clojure.string :as string]
   [clojure.java.jdbc :as jdbc]))

(def ^:dynamic *type-specify-char* \!)

(defn generate-random-string-upper [len]
  (fn [] (apply str (take len (repeatedly #(char (+ (rand 26) 65)))))))
(defn generate-random-string-lower [len]
  (fn [] (apply str (take len (repeatedly #(char (+ (rand 26) 97)))))))
(defn generate-random-integer [len]
  (fn [] (apply str (take len (repeatedly #(rand-int 10))))))
(defn round2
  "Round a double to the given precision (number of significant digits)"
  [precision d]
  (let [factor (Math/pow 10 precision)]
    (/ (Math/round (* d factor)) factor)))
(defn generate-random-double [len max-number max-precission]
  (fn [] (str (round2 (rand-int max-precission) (rand max-number)))))
(defn generate-name-from-list [& {:keys [lower? numbers?] :or {lower? false numbers? false}}]
  (fn [] (let [name (atom (rand-nth ["Esta" "Malcom" "Marina" "Taunya" "Burl" "Marcel" "Denver" "Tami" "Oliva" "Nelda" "Melodie" "Sherika" "Randal" "Deirdre" "Shea" "Melinda" "Kena" "Virgie" "Yanira" "Mason" "Jeni" "Jacqueline" "Babette" "Maryann" "Sid" "Gilbert" "Jacquelin" "Kandace" "Krystal" "Renato" "Refugia" "Bridgett" "Kary" "Kyra" "Krysta" "Hue" "Nan" "Treasa" "Dominga" "Tonia" "Brandon" "Kiana" "Hermina" "Marianela" "Akiko" "Aubrey" "Damian" "Bennie" "Estell" "Flossie" "Kasson" "Badr" "Marlize" "Taydon" "Lilianne" "Sivansh" "Krimson" "Stevee" "Serhan" "Phoenix" "Khizer" "Ilo" "Malena" "Adiba" "Iosefa" "Mckinsey" "Zain" "Jawuan" "Jamyla" "Bailor" "Myshawn" "Lillah" "Simisola" "Yariela" "Rosealie" "Jerone" "Westen" "Khaleesi" "Abdur" "Samyia" "Jameson" "Keene" "Nehemiah" "Alexandr" "Sirlegend" "Kara" "Aleysha" "Muskaan" "Righteous" "Demitrius" "Leoncio" "Ellanor" "Harly" "Minnie" "Terry" "Anh" "Alyaan" "Chana" "Mazie" "Reygan" "Ole" "Kim" "Carbon" "Makhia" "Romere" "Hadrien" "Kanylah" "Saturn" "Ayrin" "Bellarose" "Oshane" "Delton" "Eyoel" "Vania" "Macayla" "Mattie" "Juandiego" "Kayveon" "Kahiau" "Quintessa" "Erixon" "Tysin" "Kayoir" "Chipper" "Nada" "Danial" "Kartik" "Alesandra" "Rhoen" "Korrah" "Matty" "Ashby" "Jermani" "Analiz" "Ruthvik" "Rhylee" "Memphis" "Vrinda" "Imara" "Riyon" "Leighann" "Marietta" "Keno" "Griffyn" "Jakya" "Aileene" "Lottie" "Shams" "Niklas"]))]
          (if lower?   (swap! name (fn [-name] (clojure.string/lower-case -name))))
          (if numbers? (swap! name (fn [-name] (str -name (str (rand-int 1000))))))
          (pr-str @name))))

;; (defn constructor-generator [class-name]
;;   (fn [& generators]
;;     (fn [] (str "new " (clojure.string/trim class-name) "("
;;               (clojure.string/join ", "
;;                                    (for [lambda-generator generators]
;;                                      (str (lambda-generator)))) ")"))))

;; (defn cmt [text function]
;;   (fn []
;;     (str "/*" text "*/ " (function))))

;; (def user-generator-lab-1
;;   ((constructor-generator "User")
;;    (cmt "email" (fn [] (pr-str (str ((generate-random-string-lower 5))
;;                                    ((generate-random-integer 3)) "@"
;;                                    ((generate-random-string-lower 4)) "."
;;                                    ((generate-random-string-lower 2))))))
;;    (cmt "pass" (fn [] (pr-str (str ((generate-random-string-lower 1))
;;                                   ((generate-random-integer 2))))))
;;    (cmt "tel"  (fn [] (pr-str (str "+"
;;                                   ((generate-random-integer 3)) "-"
;;                                   ((generate-random-integer 3)) "-"
;;                                   ((generate-random-integer 3))))))
;;    ((constructor-generator "Passport")
;;     (cmt "nameUA" (generate-name-from-list))
;;     (cmt "nameUS" (generate-name-from-list))
;;     (cmt "age"    (generate-random-integer 2))
;;     (cmt "locUA"  (fn [] (pr-str (str ((generate-random-string-upper 2))((generate-random-integer 4))))))
;;     (cmt "locUS"  (fn [] (pr-str ((generate-random-string-lower 20)))))
;;     (cmt "serial" (fn [] (pr-str ((generate-random-string-lower 20))))))))


;; (defn generate-list-of-objects [generator count & [f]]
;;   (let [objlist (take count (repeatedly generator))]
;;     (if f
;;       (clojure.string/join "\n" (doall (map f objlist)))
;;       (clojure.string/join "\n" objlist))))

;; ;; spit "classes.java"
;; (spit "classes.java"
;;       (str "List<User> = new ArrayList(){{\n"
;;            (generate-list-of-objects user-generator-lab-1 20 (fn [s] (str "add(" s ");")))
;;            "\n}};\n"))


;; (defn jrange
;;   ([from to & body]
;;    (let [s (gensym)] 
;;      (format "for(int %s=%s;%s<%s;%s++){\n%s\n}",s,from,s,to,s,body)))
;;   ([from step to body]
;;    (let [s (gensym)]
;;      (format "for(int %s=%s;%s<%sq;%s+=%s){\n%s}",s,from,s,to,s,step,body))))


;; (jlet [a 10
;;        k [123 41 2 55 6]
;;        s {:a "some"}
;;        f "suka"])


;; (defn select-type [e]
;;   (condp = (type e)
;;      java.lang.Character (str "'" e "'")
;;      java.lang.String (str "\"" e "\"")
;;      java.lang.Double (str e)
;;      java.lang.Long (format "%sL" e)
;;      clojure.lang.Symbol (str "\"" e "\"")
;;      "ERROR"))

;; (select-type 'fsda)

;; (defn jarraylist [elements]
;;   (str "new ArrayList<>(){{"
;;        (string/join "" (map #(str "\n\tadd(" (select-type %) ");") elements))
;;        "\n}};"))


;; (defn create-variable[variable value]
;;   ;; (if (some #(= % *type-specify-char*) (seq (str 'su!ka))))
;;   (println (type value))
;;   ((condp = (type value)
;;      java.lang.Character (partial format "Char %s = '%c';\n")
;;      java.lang.String (partial format "String %s = \"%s\";\n")
;;      java.lang.Double (partial format "Double %s = %s;\n")
;;      java.lang.Long (partial format "Long %s = %sL;\n")
;;      clojure.lang.Symbol #(create-variable %1 (str %2))
;;      clojure.lang.PersistentList #(format "List %s = %s\n" %1 (jarraylist %2))
;;      clojure.lang.PersistentVector #(format "List %s = %s\n" %1 (jarraylist %2))
;;      clojure.lang.PersistentArrayMap #(format "HashMap %s = %s\n" %1 (str %2))
;;      clojure.lang.PersistentHashMap #(format "HashSet %s = %s\n" %1 (str %2))
;;      (fn [var val] (str "// Error generation\n " (create-variable (str var) (str val)))))
;;    variable value ))
;; (println (create-variable "suka" 'ba))

;; (some #(= % *type-specify-char*) (seq (str 'su!ka<List>)))

;; (defn jlet
;;   ([list & body] {:pre [(even? (count list))]}
;;    (map  (partition 2 list))))

;; (partition 2 '(java$Integer 2 2 4 5 6))
