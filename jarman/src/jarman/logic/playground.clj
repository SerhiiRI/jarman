(ns jarman.logic.playground
  (:refer-clojure :exclude [update])
  (:require
   [clojure.data :as data]
   [clojure.string :as string]
   [jarman.logic.connection :as db]
   [jarman.logic.sql-tool :as toolbox :include-macros true :refer :all]
   [jarman.logic.metadata :as metadata]
   [jarman.config.storage :as storage]
   [jarman.config.environment :as env]
   [jarman.logic.structural-initializer :as sinit]
   [jarman.tools.lang :refer :all])
  (:import (java.util Date)
           (java.text SimpleDateFormat)))

;; (def available-scheme ["service_contract"
;;                        "service_contract"
;;                        "seal"
;;                        "repair_contract"
;;                        "point_of_sale_group_links"
;;                        "point_of_sale_group"
;;                        "cache_register"
;;                        "point_of_sale"
;;                        "enterpreneur"
;;                        "user"
;;                        "permission"
;;                        "documents"
;;                        "metadata"])

(def documents
  (create-table :documents
                :columns [{:table [:varchar-100 :default :null]}
                          {:name [:varchar-200 :default :null]}
                          {:document [:blob :default :null]}
                          {:prop [:text :nnull :default "\"{}\""]}]))

(def view
  (create-table :view
                :columns [{:table-name [:varchar-100 :default :null]}
                          {:view [:text :nnull :default "\"{}\""]}]))

(def metadata
  (create-table :metadata
                :columns [{:table [:varchar-100 :default :null]}
                          {:prop [:text :default :null]}]))

(def permission
  (create-table :permission
                :columns [{:permission_name [:varchar-20 :default :null]}
                          {:configuration [:tinytext :nnull :default "\"{}\""]}]))

(def user
  (create-table :user
                :columns [{:login [:varchar-100 :nnull]}
                          {:password [:varchar-100 :nnull]}
                          {:first_name [:varchar-100 :nnull]}
                          {:last_name [:varchar-100 :nnull]}
                          {:id_permission [:bigint-20-unsigned :nnull]}]
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
                          {:dev_id [:varchar-100 :default :null]}
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
                          {:datetime_of_use [:datetime :default :null]}
                          {:datetime_of_remove [:datetime :default :null]}]))

(def service_contract
  (create-table :service_contract
                :columns [{:id_enterpreneur     [:bigint-20 :unsigned :default :null]}
                          {:contract_start_term [:date :default :null]}
                          {:contract_end_term   [:date :default :null]}
                          {:money_per_month     [:float-2 :nnull :default 0]}]
                :foreign-keys [{:id_enterpreneur :enterpreneur} {:delete :cascade :update :cascade}]))

(def service_contract_month
  (create-table :service_contract_month
                :columns [{:id_service_contract [:bigint-20 :unsigned :default :null]}
                          {:service_month_date  [:date :default :null]}
                          {:money_per_month     [:float-2 :nnull :default 0]}]
                :foreign-keys [{:id_service_contract :service_contract} {:delete :cascade :update :cascade}]))

;; Pryczyna rozplombuwaninia
(def repair_reasons
  (create-table
   :repair_reasons
   :columns [{:description [:varchar-255 :default :null]}]))

;; Pryczyna nesprawnosci
(def repair_technical_issue
  (create-table
   :repair_technical_issue
   :columns [{:description [:varchar-255 :default :null]}]))

;; Charakter nesprawnostci
(def repair_nature_of_problem
  (create-table
   :repair_nature_of_problem
   :columns [{:description [:varchar-255 :default :null]}]))

(def repair_contract
  (create-table :repair_contract
                :columns [{:id_cache_register[:bigint-20 :unsigned :default :null]}
                          {:id_old_seal      [:bigint-20 :unsigned :default :null]}
                          {:id_new_seal      [:bigint-20 :unsigned :default :null]}
                          {:id_repair_reasons[:bigint-20 :unsigned :default :null]}
                          {:id_repair_technical_issue[:bigint-20 :unsigned :default :null]}
                          {:id_repair_nature_of_problem[:bigint-20 :unsigned :default :null]}
                          {:repair_date    [:date :default :null]}
                          {:cache_register_register_date [:date :default :null]}]
                :foreign-keys [[{:id_cache_register :cache_register} {:delete :cascade :update :cascade}]
                               [{:id_old_seal :seal} {:delete :null :update :null}]
                               [{:id_new_seal :seal} {:delete :null :update :null}]
                               [{:id_repair_reasons :repair_reasons}
                                {:delete :null :update :null}]
                               [{:id_repair_technical_issue :repair_technical_issue}
                                {:delete :null :update :null}]
                               [{:id_repair_nature_of_problem :repair_nature_of_problem}
                                {:delete :null :update :null}]]))


(defmacro create-tabels [& tables]
  `(do ~@(for [t tables]
           `(db/exec ~t))))
(defmacro delete-tabels [& tables]
  `(do ~@(for [t tables]
           `(db/exec (drop-table (keyword '~t))))))

;; (db/exec (drop-table :repair_contract))
;; (db/exec repair_contract)
;; (db/query (show_tables))
;; (db/exec point_of_sale_group_links)

(defn create-scheme-one [scheme]
  (eval `(db/exec ~(symbol (string/join "/" ["jarman.schema-builder" (symbol scheme)])))))
(defn create-scheme []
  (create-tabels view
                 metadata
                 documents
                 permission
                 user
                 enterpreneur
                 point_of_sale
                 cache_register
                 point_of_sale_group
                 point_of_sale_group_links
                 seal
                 repair_reasons
                 repair_technical_issue
                 repair_nature_of_problem
                 repair_contract
                 service_contract
                 service_contract_month))

(defn delete-scheme-one [scheme]
  (eval `(db/exec (drop-table ~(keyword scheme)))))
(defn delete-scheme []
  (delete-tabels service_contract_month
                 service_contract
                 repair_contract
                 repair_nature_of_problem
                 repair_technical_issue
                 repair_reasons
                 seal
                 point_of_sale_group_links
                 point_of_sale_group
                 cache_register
                 point_of_sale
                 enterpreneur
                 user
                 permission
                 documents
                 metadata
                 view))

(defn regenerate-scheme []
  (sinit/procedure-test-all)
  (delete-scheme)
  (create-scheme)
  (metadata/do-create-meta)
  (metadata/do-create-references))



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
  ;; [idX (db/query (select-id-from table))]
  (fn [] (rand-nth (map :id (db/query (select-id-from table))))))

(def sql-insert (partial db/exec))

(def glogin (generate-random-from-list list-words))
(def gfirstname (generate-random-from-list list-firstname))
(def glastname (generate-random-string-lower 3))
(def gpassword (generate-random-string-from 3 "1234"))
(def gid_permission (generate-random-sql-from-col :permission))


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


(def gsimplestring (generate-random-string-lower 10))
(def gownership (generate-random-from-list ["fio" "pp"]))
(def gidentifier (generate-random-string-from 10 "123456789"))

(defn fill-enterpreneur [c]
  (def create-enterpreneur
    (fn [] {:ssreou (gidentifier)
            :ownership_form (gownership)
            :vat_certificate (gsimplestring)
            :individual_tax_number (gidentifier)
            :director (gfirstname)
            :accountant (gfirstname)
            :legal_address (gsimplestring)
            :physical_address (gsimplestring)
            :contacts_information (gsimplestring)}))
  (doall
   (map
    sql-insert
    (take c (repeatedly #(insert :enterpreneur :values (create-enterpreneur)))))))


(def gtable (generate-random-from-list ["service_contract" "seal" "repair_contract" "point_of_sale_group_links" "point_of_sale_group" "cache_register" "point_of_sale" "enterpreneur" "user" "permission"]))
(defn fill-documents [c]
  (def create-documents
    (fn [] {:documents.table (gtable)
           :name (gsimplestring)
           :document nil
           :prop "{}"}))
  (doall
   (map
    sql-insert
    (take c (repeatedly #(insert :documents :values (create-documents)))))))


(def gid_enterpreneur (generate-random-sql-from-col :enterpreneur))
(defn fill-point_of_sale [c]
  (def create-point_of_sale
    (fn []
      {:id_enterpreneur (gid_enterpreneur)
       :name (gsimplestring)
       :physical_address (gsimplestring)
       :telefons (gsimplestring)}))
  (doall
   (map
    sql-insert
    (take c (repeatedly #(insert :point_of_sale :values (create-point_of_sale)))))))


(def gid_point_of_sale (generate-random-sql-from-col :point_of_sale))
(def gboolean (generate-random-from-list [true false]))
(defn fill-cache_register [c]
  (def create-cache_register
    (fn []
      {:id_point_of_sale (gid_point_of_sale)
       :name (gsimplestring)
       :serial_number (gsimplestring)
       :fiscal_number (gsimplestring)
       :manufacture_date (.format (java.text.SimpleDateFormat. "YYYY-MM-dd") (java.util.Date.))
       :first_registration_date (.format (java.text.SimpleDateFormat. "YYYY-MM-dd") (java.util.Date.))
       :is_working (gboolean)
       :version (gsimplestring)
       :dev_id (gboolean)
       :producer (gsimplestring)
       :modem (gsimplestring)
       :modem_model (gsimplestring)
       :modem_serial_number (gsimplestring)
       :modem_phone_number (gsimplestring)}))
  (doall
   (map
    sql-insert
    (take c (repeatedly #(insert :cache_register :values (create-cache_register)))))))


(defn fill-point_of_sale_group [c]
  (def create-point_of_sale_group
    (fn []
      {:group_name (gsimplestring)
       :information (gsimplestring)}))
  (doall
   (map
    sql-insert
    (take c (repeatedly #(insert :point_of_sale_group :values (create-point_of_sale_group)))))))


(def gid_point_of_sale_group (generate-random-sql-from-col :point_of_sale_group))
(def gid_point_of_sale (generate-random-sql-from-col :point_of_sale))
(defn fill-point_of_sale_group_links [c]
  (def create-point_of_sale_group_links
    (fn []
      {:id_point_of_sale_group (gid_point_of_sale_group)
       :id_point_of_sale (gid_point_of_sale)}))
  (doall
   (map
    sql-insert
    (take c (repeatedly #(insert :point_of_sale_group_links :values (create-point_of_sale_group_links)))))))

(def gsealnumber (generate-random-string-from 10 "0123456789"))
(defn fill-seal [c]
  (def create-seal
    (fn []
      {:seal_number (gsealnumber)
       :datetime_of_use    (.format (java.text.SimpleDateFormat. "YYYY-MM-dd") (java.util.Date.))
       :datetime_of_remove (.format (java.text.SimpleDateFormat. "YYYY-MM-dd") (java.util.Date.))}))
  (doall
   (map
    sql-insert
    (take c (repeatedly #(insert :seal :values (create-seal)))))))

(def gpayment (fn [] (rand-int 100000)))
(defn fill-service_contract [c]
  (def create-service_contract
    (fn []
      {:id_enterpreneur (gid_enterpreneur)
       :contract_start_term (.format (java.text.SimpleDateFormat. "YYYY-MM-dd") (java.util.Date.))
       :contract_end_term   (.format (java.text.SimpleDateFormat. "YYYY-MM-dd") (java.util.Date.))
       :money_per_month (gpayment)}))
  (doall
   (map
    sql-insert
    (take c (repeatedly #(insert :service_contract :values (create-service_contract)))))))

(def gid_service_contract (generate-random-sql-from-col :service_contract))
(defn fill-service_contract_mounth [c]
  (def create-service_contract_mounth
    (fn []
      {:id_service_contract (gid_service_contract)
       :service_month_date  (.format (java.text.SimpleDateFormat. "YYYY-MM-dd") (java.util.Date.))
       :money_per_month (gpayment)}))
  (doall
   (map
    sql-insert
    (take c (repeatedly #(insert :service_contract_month :values (create-service_contract_mounth)))))))

(defn fill-repair_reasons [c]
  (def create-repair_reasons
    (fn []
      {:description (gsimplestring)}))
  (doall
   (map
    sql-insert
    (take c (repeatedly #(insert :repair_reasons :values (create-repair_reasons)))))))


(defn fill-repair_technical_issue [c]
  (def create-repair_technical_issue
    (fn []
      {:description (gsimplestring)}))
  (doall
   (map
    sql-insert
    (take c (repeatedly #(insert :repair_technical_issue :values (create-repair_technical_issue)))))))

(defn fill-repair_nature_of_problem [c]
  (def create-repair_nature_of_problem
    (fn []
      {:description (gsimplestring)}))
  (doall
   (map
    sql-insert
    (take c (repeatedly #(insert :repair_nature_of_problem :values (create-repair_nature_of_problem)))))))


(def gid_repair_reasons (generate-random-sql-from-col :repair_reasons))
(def gid_repair_technical_issue (generate-random-sql-from-col :repair_technical_issue))
(def gid_repair_nature_of_problem (generate-random-sql-from-col :repair_nature_of_problem))
(def gid_seal (generate-random-sql-from-col :seal))
(def gid_cache_register (generate-random-sql-from-col :cache_register))
(defn fill-repair_contract [c]
  (def create-repair_contract
    (fn []
      {:id_cache_register (gid_cache_register)
       :id_old_seal (gid_seal)
       :id_new_seal (gid_seal)
       :id_repair_reasons (gid_repair_reasons)
       :id_repair_technical_issue (gid_repair_technical_issue)
       :id_repair_nature_of_problem (gid_repair_nature_of_problem)
       :repair_date (.format (java.text.SimpleDateFormat. "YYYY-MM-dd") (java.util.Date.))
       :cache_register_register_date (.format (java.text.SimpleDateFormat. "YYYY-MM-dd") (java.util.Date.))}))
  (doall
   (map
    sql-insert
    (take c (repeatedly #(insert :repair_contract :values (create-repair_contract)))))))

(defn- fn-fish []
  ;; (fill-permission)
  (fill-user 30)
  (fill-enterpreneur 30)
  (fill-documents 15)
  (fill-point_of_sale 30)
  (fill-cache_register 10)
  (fill-point_of_sale_group 10)
  (fill-point_of_sale_group_links 10)
  (fill-seal 50)
  (fill-service_contract 10)
  (fill-service_contract_mounth 20)
  (fill-repair_reasons 10)
  (fill-repair_technical_issue 10)
  (fill-repair_nature_of_problem 10)
  (fill-repair_contract 30))


(defn- test-fish []
  ;; (fill-permission)
  (fill-user 1)
  (fill-enterpreneur 1)
  (fill-documents 1)
  (fill-point_of_sale 1)
  (fill-cache_register 1)
  (fill-point_of_sale_group 1)
  (fill-point_of_sale_group_links 1)
  (fill-seal 1)
  (fill-service_contract 1)
  (fill-service_contract_mounth 1)
  (fill-repair_reasons 1)
  (fill-repair_technical_issue 1)
  (fill-repair_nature_of_problem 1)
  (fill-repair_contract 1))


(defmacro fish [& body]
  (let [todo
        (for [[table & args] body]
          `(do (print (if-let [c# (first [~@args])]
                        (format "Generating %d row %s . . . " c# '~table)
                        (format "Generating struct %s . . . " '~table)))
               (let [t# (string/replace 
                         (with-out-str (time (apply ~(symbol (format "fill-%s" table)) [~@args])))
                         #"(\r\n|\"|Elapsed time: )" "")]
                 (println (format "(%s) done!" t#)))))]
    `(do
       (println "Generating Fish")
       ~@todo )))


(defn regenerate-scheme-test []
  (delete-scheme)
  (create-scheme)
  (metadata/do-clear-meta)
  (metadata/do-create-meta)
  (fish
   [user 30]
   [enterpreneur 30]
   [documents 15]
   [point_of_sale 30]
   [cache_register 10]
   [point_of_sale_group 10]
   [point_of_sale_group_links 10]
   [seal 50]
   [service_contract 50]
   [service_contract_mounth 50]
   [repair_reasons 50]
   [repair_technical_issue 50]
   [repair_nature_of_problem 40]
   [repair_contract 40]))


