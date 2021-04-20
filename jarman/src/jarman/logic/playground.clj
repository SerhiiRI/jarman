 (ns jarman.logic.playground
  (:refer-clojure :exclude [update])
  (:require
   [jarman.logic.sql-tool :as toolbox :include-macros true :refer :all]
   [jarman.logic.metadata :as metadata]
   [jarman.config.storage :as storage]
   [jarman.config.environment :as env]
   [jarman.tools.lang :refer :all]
   [clojure.data :as data]
   [clojure.string :as string]
   [clojure.java.jdbc :as jdbc])
  (:import (java.util Date)
           (java.text SimpleDateFormat)))

(def ^:dynamic prod? false)

;;;
(def ^:dynamic sql-connection
  (if prod?
    {:dbtype "mysql" :host "trashpanda-team.ddns.net" :port 3306 :dbname "jarman" :user "jarman" :password "dupa"}
    {:dbtype "mysql" :host "127.0.0.1" :port 3306 :dbname "jarman" :user "root" :password "1234"}))
;; (case db-connection-resolver
;;   :prod-remote {:dbtype "mysql" :host "trashpanda-team.ddns.net" :port 3306 :dbname "jarman" :user "jarman" :password "dupa"}
;;   :prod-local {:dbtype "mysql" :host "192.168.1.69" :port 3306 :dbname "jarman" :user "jarman" :password "dupa"}
;;   :local {:dbtype "mysql" :host "127.0.0.1" :port 3306 :dbname "jarman" :user "root" :password "1234"})

(defmacro s-exec [s]
  `(jdbc/execute! sql-connection ~s))
(defmacro s-query [s]
  `(jdbc/query sql-connection ~s))



(def available-scheme ["service_contract"
                       "seal"
                       "repair_contract"
                       "point_of_sale_group_links"
                       "point_of_sale_group"
                       "cache_register"
                       "point_of_sale"
                       "enterpreneur"
                       "user"
                       "permission"
                       "METADATA"])

(def METADATA
  (create-table :METADATA
                :columns [{:table [:varchar-100 :default :null]}
                          {:prop [:text :default :null]}]))

(def permission
  (create-table :permission
                :columns [{:permission_name [:varchar-20 :default :null]}
                          {:configuration [:tinytext :nnull :default "'{}'"]}]))

(def user
  (create-table :user
                :columns [{:login [:varchar-100 :nnull]}
                          {:password [:varchar-100 :nnull]}
                          {:first_name [:varchar-100 :nnull]}
                          {:last_name [:varchar-100 :nnull]}
                          {:id_permission [:bigint-120-unsigned :nnull]}]
                :foreign-keys [{:id_permission :permission} {:delete :cascade :update :cascade}]))

(def enterpreneur
  (create-table :enterpreneur
                :columns [{:ssreou [:tinytext :nnull]}
                          {:ownership_form [:varchar-100 :default :null]}
                          {:vat_certificate [:tinytext :default :null]}
                          {:individual_tax_number [:varchar-100 :default :null]}
                          {:director [:varchar-100 :default :null]}
                          {:accountant [:varchar-100 :default :null]}
                          {:legal_address [:varchar-100 :default :null]}
                          {:physical_address [:varchar-100 :default :null]}
                          {:contacts_information [:mediumtext :default :null]}]))

(def point_of_sale
  (create-table :point_of_sale
                :columns [{:id_enterpreneur [:bigint-20-unsigned :default :null]}
                          {:name [:varchar-100 :default :null]}
                          {:physical_address  [:varchar-100 :default :null]}
                          {:telefons  [:varchar-100 :default :null]}]
                :foreign-keys [{:id_enterpreneur :enterpreneur} {:update :cascade}]))

(def cache_register
  (create-table :cache_register
                :columns [{:id_point_of_sale [:bigint-20 :unsigned :default :null]}
                          {:name [:varchar-100 :default :null]}
                          {:serial_number [:varchar-100 :default :null]}
                          {:fiscal_number [:varchar-100 :default :null]}
                          {:manufacture_date [:date :default :null]}
                          {:first_registration_date [:date :default :null]}
                          {:is_working [:tinyint-1 :default :null]}
                          {:version [:varchar-100 :default :null]}
                          {:id_dev [:varchar-100 :default :null]}
                          {:producer [:varchar-100 :default :null]}
                          {:modem [:varchar-100 :default :null]}
                          {:modem_model [:varchar-100 :default :null]}
                          {:modem_serial_number [:varchar-100 :default :null]}
                          {:modem_phone_number [:varchar-100 :default :null]}]
                :foreign-keys [{:id_point_of_sale :point_of_sale} {:delete :cascade :update :cascade}]))

(def point_of_sale_group
  (create-table :point_of_sale_group
                :columns [{:group_name [:varchar-100 :default :null]}
                          {:information [:mediumtext :default :null]}]))

(def point_of_sale_group_links
  (create-table :point_of_sale_group_links
                :columns [{:id_point_of_sale_group [:bigint-20-unsigned :default :null]}
                          {:id_point_of_sale [:bigint-20-unsigned :default :null]}]
                :foreign-keys [[{:id_point_of_sale_group :point_of_sale_group} {:delete :cascade :update :cascade}]
                               [{:id_point_of_sale :point_of_sale}]]))

(def seal
  (create-table :seal
                :columns [{:seal_number [:varchar-100 :default :null]}
                          {:to_date [:date :default :null]}]))

(def service_contract
  (create-table :service_contract
                :columns [{:id_point_of_sale [:bigint-20 :unsigned :default :null]}
                          {:register_contract_date [:date :default :null]}
                          {:contract_term_date [:date :default :null]}
                          {:money_per_month [:int-11 :default :null]}]
                :foreign-keys [{:id_point_of_sale :point_of_sale} {:delete :cascade :update :cascade}]))

(def repair_contract
  (create-table :repair_contract
                :columns [{:id_cache_register [:bigint-20 :unsigned :default :null]}
                          {:id_point_of_sale [:bigint-20 :unsigned :default :null]}
                          {:creation_contract_date [:date :default :null]}
                          {:last_change_contract_date [:date :default :null]}
                          {:contract_terms_date [:date :default :null]}
                          {:cache_register_register_date [:date :default :null]}
                          {:remove_security_seal_date [:datetime :default :null]}
                          {:cause_of_removing_seal [:mediumtext :default :null]}
                          {:technical_problem [:mediumtext :default :null]}
                          {:active_seal [:mediumtext :default :null]}]
                :foreign-keys [[{:id_cache_register :cache_register} {:delete :cascade :update :cascade}]
                               [{:id_point_of_sale :point_of_sale} {:delete :cascade :update :cascade}]]))


(defmacro create-tabels [& tables]
  `(do ~@(for [t tables]
           `(jdbc/execute! sql-connection ~t))))
(defmacro delete-tabels [& tables]
  `(do ~@(for [t tables]
           `(jdbc/execute! sql-connection (drop-table (keyword '~t))))))

(defn create-scheme-one [scheme]
  (eval `(jdbc/execute! sql-connection ~(symbol (string/join "/" ["jarman.schema-builder" (symbol scheme)])))))
(defn create-scheme []
  (create-tabels METADATA
                 permission
                 user
                 enterpreneur
                 point_of_sale
                 cache_register
                 point_of_sale_group
                 point_of_sale_group_links
                 repair_contract
                 seal
                 service_contract))

(defn delete-scheme-one [scheme]
  (eval `(jdbc/execute! sql-connection (drop-table ~(keyword scheme)))))
(defn delete-scheme []
  (delete-tabels service_contract
                 seal
                 repair_contract
                 point_of_sale_group_links
                 point_of_sale_group
                 cache_register
                 point_of_sale
                 enterpreneur
                 user
                 permission
                 METADATA))


(defn regenerate-scheme []
  (delete-scheme)
  (create-scheme)
  (metadata/do-create-meta))

(defn regenerate-metadata []
  (do (metadata/do-clear-meta)
      (metadata/do-create-meta)))


;;;;;;;;;;;;;;;;;;;;;;
;;; Data generator ;;;
;;;;;;;;;;;;;;;;;;;;;;

(def list-firstname ["Esta" "Malcom" "Marina" "Taunya" "Burl" "Marcel" "Denver" "Tami" "Oliva" "Nelda" "Melodie" "Sherika" "Randal" "Deirdre" "Shea" "Melinda" "Kena" "Virgie" "Yanira" "Mason" "Jeni" "Jacqueline" "Babette" "Maryann" "Sid" "Gilbert" "Jacquelin" "Kandace" "Krystal" "Renato" "Refugia" "Bridgett" "Kary" "Kyra" "Krysta" "Hue" "Nan" "Treasa" "Dominga" "Tonia" "Brandon" "Kiana" "Hermina" "Marianela" "Akiko" "Aubrey" "Damian" "Bennie" "Estell" "Flossie" "Kasson" "Badr" "Marlize" "Taydon" "Lilianne" "Sivansh" "Krimson" "Stevee" "Serhan" "Phoenix" "Khizer" "Ilo" "Malena" "Adiba" "Iosefa" "Mckinsey" "Zain" "Jawuan" "Jamyla" "Bailor" "Myshawn" "Lillah" "Simisola" "Yariela" "Rosealie" "Jerone" "Westen" "Khaleesi" "Abdur" "Samyia" "Jameson" "Keene" "Nehemiah" "Alexandr" "Sirlegend" "Kara" "Aleysha" "Muskaan" "Righteous" "Demitrius" "Leoncio" "Ellanor" "Harly" "Minnie" "Terry" "Anh" "Alyaan" "Chana" "Mazie" "Reygan" "Ole" "Kim" "Carbon" "Makhia" "Romere" "Hadrien" "Kanylah" "Saturn" "Ayrin" "Bellarose" "Oshane" "Delton" "Eyoel" "Vania" "Macayla" "Mattie" "Juandiego" "Kayveon" "Kahiau" "Quintessa" "Erixon" "Tysin" "Kayoir" "Chipper" "Nada" "Danial" "Kartik" "Alesandra" "Rhoen" "Korrah" "Matty" "Ashby" "Jermani" "Analiz" "Ruthvik" "Rhylee" "Memphis" "Vrinda" "Imara" "Riyon" "Leighann" "Marietta" "Keno" "Griffyn" "Jakya" "Aileene" "Lottie" "Shams" "Niklas"])

(def list-words ["medical" "marble" "nosy" "serious" "shaggy" "excellent" "unit" "faint" "shape" "stretch" "rings" "finish" "thoughtless" "male" "punish" "little" "smash" "earthquake" "dump" "club" "idea" "hold" "selfish" "forecast" "childlike" "approval" "two" "jolly" "wet" "periodic" "instrument" "system" "abundant" "drink" "minor" "fry" "shrink" "brothers" "spin" "elderly" "leap" "friction" "lip" "tempt" "solicit" "safe" "upset" "confiscate" "ashamed" "hysterical" "hate" "present" "create" "canvass" "cloudy" "yawn" "roll" "print" "marked" "coal" "sulky" "rainy" "zealous" "punishment" "boundary" "oranges" "vomit" "pricey" "productive" "mark" "zoom" "swim" "cover" "terminate" "praise" "gain" "mislead" "cry" "true" "red" "month" "box" "button" "begin" "right" "hush" "adorable" "redo" "wary" "selfish" "home" "consen" "common" "loud" "loving" "bawdy" "branch" "organize" "familiar" "be" "bed" "ladybug" "brash" "clumsy" "long" "tendency" "luxuriant" "tangy" "scattered" "sock" "approve" "cannon" "stoop" "entertaining" "shut" "roar" "bloody" "thirsty" "irritating" "canvass" "historical" "shock" "crowd" "foolish" "help" "dinosaurs" "juice" "loose" "cut" "wanting" "punish" "living" "cake" "soup" "high" "hiss" "brown" "abstracted" "extra-large" "grade" "statuesque" "basin" "malicious" "rabbits" "spend" "magnificent" "slim" "canvas" "bed" "obtainable" "church" "thundering" "overconfident" "oatmeal" "obedient" "uncovered" "enter" "high-pitched" "soft" "race" "descriptive" "ooze" "jewel" "hissing" "guitar" "table" "net" "hissing" "angle" "ill-treat" "shirt" "rhythm" "perpetual" "notice" "lumpy" "bushes" "harmony" "deserted" "swallow" "easy" "light" "join" "green" "foregoing" "convince" "fit" "direction" "hands" "pollution" "doctor" "lend" "snatch" "fresh" "disillusioned" "banana" "clammy" "elbow" "scream" "speed" "axiomatic" "strain" "sabotage" "wiggly" "whispering" "digest" "acid" "childlike" "ajar" "authority" "matter" "chair" "chew" "curly" "good" "scat" "vessel" "banana" "chubby" "challenge" "voracious" "trail" "brawny" "chew" "dark" "cheap" "private" "salvage" "kitten" "ignorant" "implode" "actually" "shaggy" "rustic" "front" "express" "courageous" "help" "park" "jumbled" "invite" "ocean" "preset" "answer" "twig" "scent" "repair" "tie" "birth" "level" "star" "grotesque" "tomatoes" "cart" "unbecoming" "vigorous" "liquid" "look" "grip" "behavior" "bread" "luxuriant" "sort" "brothers" "limping" "wren" "put" "shave" "process" "identify" "stare" "wry" "surprise" "wed" "callous" "rain" "broken" "paper" "kitty" "extend" "fish" "jump" "early" "thoughtful" "tenuous" "bag" "originate" "buy" "gaze" "acoustic" "rhetorical" "relation" "bang" "apples" "encircle" "scattered" "moo" "happy" "misuse" "toes" "next" "flop" "kettle" "awful" "consecrat" "determine" "condemned" "frighten" "induce" "fish" "waggish" "team" "uppity" "initiate" "word" "venomous" "translate" "hallowed" "unsightly" "bear" "can" "count" "pan" "spin" "fascinated" "squirrel" "sling" "make" "cut" "climb" "ring" "zippy" "late" "irritating" "rise" "bitter" "yellow" "salute" "sable" "lunch" "disturb" "yellow" "voyage" "tooth" "cost" "bewildered" "dog" "scabble" "lace" "legs" "able" "fill" "advice" "greet" "year" "hope" "crooked" "send" "stray" "scald" "wry" "print" "sail" "tiger" "half" "kittens" "open" "join" "crack" "aquatic" "rake" "hallowed" "female" "melodic" "alcoholic" "beam" "fan" "brass" "fabulous" "smell" "juice" "destroy" "balance" "frantic" "half" "record" "creepy" "stop" "shaky" "jolly" "market" "driving" "salt" "testy" "periodic" "box" "determine" "confuse" "behavior" "thumb" "spray"])



(defn generate-random-string-upper [len]
  (fn [] (apply str (take len (repeatedly #(char (+ (rand 26) 65)))))))
(defn generate-random-string-lower [len]
  (fn [] (apply str (take len (repeatedly #(char (+ (rand 26) 97)))))))
(defn generate-random-string-from [len string-list]
  (fn [] (apply str (take len (repeatedly #(rand-nth string-list))))))
(defn generate-random-integer [len]
  (fn [] (apply str (take len (repeatedly #(rand-int 10))))))
(defn generate-random-from-list [string-list & {:keys [lower? numbers?] :or {lower? false numbers? false}}]
  (fn [] (let [name (atom (rand-nth string-list))]
          (if lower?   (swap! name (fn [-name] (clojure.string/lower-case -name))))
          (if numbers? (swap! name (fn [-name] (str -name (str (rand-int 1000))))))
          @name)))
(defn round-double
  "Round a double to the given precision (number of significant digits)"
  [precision d]
  (let [factor (Math/pow 10 precision)]
    (/ (Math/round (* d factor)) factor)))
(defn generate-random-double [max-number max-precission]
  (fn [] (str (round-double (rand-int max-precission) (rand max-number)))))
(defn select-id-from [table]
  (select table :column [:id]))
(defn generate-random-sql-from-col [table]
  ;; [idX (jdbc/query sql-connection (select-id-from table))]
  (fn [] (rand-nth (map :id (jdbc/query sql-connection (select-id-from table))))))

(def sql-insert (partial jdbc/execute! sql-connection))

(def glogin (generate-random-from-list list-words))
(def gfirstname (generate-random-from-list list-firstname))
(def glastname (generate-random-string-lower 3))
(def gpassword (generate-random-string-from 3 "1234"))
(def gid_permission (generate-random-sql-from-col :permission))
((generate-random-sql-from-col :permission))

(defn fill-permission []
  (def create-permission 
    (fn [name] {:permission_name name
               :configuration "{}"}))
  (doall
   (map sql-insert
    [(insert :permission :values (create-permission "admin"))
     (insert :permission :values (create-permission "user"))
     (insert :permission :values (create-permission "employer"))
     (insert :permission :values (create-permission "szprot"))])))

(defn fill-user [c]
  (def create-user 
    (fn [] {:login (glogin)
           :password (gpassword)
           :first_name (gfirstname)
           :last_name (glastname)
           :id_permission (gid_permission)}))
  (doall
   (map
    sql-insert
    (take c (repeatedly #(insert :user :values (create-user)))))))




;; (def create-cache_register 
;;   (fn [] {:is_working nil
;;          :modem_serial_number nil
;;          :modem_phone_number nil
;;          :producer nil
;;          :first_registration_date nil
;;          :modem_model nil
;;          :name nil
;;          :fiscal_number nil
;;          :id_dev nil
;;          :manufacture_date nil
;;          :modem nil
;;          :version nil
;;          :serial_number nil
;;          :id_point_of_sale nil}))
;; (def create-enterpreneur 
;;   (fn [] {:director nil
;;          :contacts_information nil
;;          :physical_address nil
;;          :ownership_form nil
;;          :accountant nil
;;          :legal_address nil
;;          :individual_tax_number nil
;;          :vat_certificate nil
;;          :ssreou nil}))

;; (def create-point_of_sale 
;;   (fn [] {:id_enterpreneur nil
;;          :name nil
;;          :physical_address nil
;;          :telefons nil}))
;; (def create-point_of_sale_group 
;;   (fn [] {:group_name nil
;;          :information nil}))
;; (def create-point_of_sale_group_links 
;;   (fn [] {:id_point_of_sale_group nil
;;          :id_point_of_sale nil}))
;; (def create-repair_contract 
;;   (fn [] {:remove_security_seal_date nil
;;          :id_cache_register nil
;;          :last_change_contract_date nil
;;          :technical_problem nil
;;          :cause_of_removing_seal nil
;;          :active_seal nil
;;          :cache_register_register_date nil
;;          :id_point_of_sale nil
;;          :creation_contract_date nil
;;          :contract_terms_date nil}))
;; (def create-seal 
;;   (fn [] {:seal_number nil
;;          :to_date nil}))
;; (def create-service_contract 
;;   (fn [] {:id_point_of_sale nil
;;          :register_contract_date nil
;;          :contract_term_date nil
;;          :money_per_month nil}))

(defn clean-test-data []
  (do
    (jdbc/execute! sql-connection (delete :permission)) 
    (jdbc/execute! sql-connection (delete :user))))

(defn refill-test-data []
  (do
    (fill-permission)
    (fill-user 20)))

(defn regenerate-scheme-test []
  (delete-scheme)
  (create-scheme)
  (metadata/do-create-meta)
  (metadata/do-create-references)
  (refill-test-data))

;;; test validation ;;;


(defn authenticate-user [login password]
 (s-query (select :user :where {:login login :password password})))
