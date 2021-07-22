(ns jarman.config.dot-jarman
  (:gen-class)
  (:import (java.io IOException))
  (:require
   ;; Clojure
   [clojure.string :as s]
   [clojure.java.io :as io]
   ;; Jarman 
   [jarman.tools.lang :refer :all]
   [jarman.config.environment :as env]
   [jarman.config.dot-jarman-param :refer [setq defvar print-list-not-loaded]]))

(def jarman ".jarman")
(def jarman-path (io/file env/user-home jarman))

;;;;;;;;;;;;;;;
;;; HELPERS ;;;
;;;;;;;;;;;;;;;

(defmacro ioerr
  "Wrap any I/O action to try-catch block
    f              - function which must be wrapped 
    f-io-exception - on IOException one-arg lambda 
    f-exception    - on Exception one-arg lambda"
  ([f]             `(ioerr ~f (fn [tmp#] nil)))
  ([f f-exception] `(ioerr ~f ~f-exception ~f-exception))
  ([f f-io-exception f-exception]
   `(try ~f
         (catch IOException e# (~f-io-exception (format "I/O error. Maybe problem in file: %s" (ex-message e#))))
         (catch Exception   e# (~f-exception (format "Undefinied problem: %s" (ex-message e#)))))))

(defn fput [s] (ioerr (with-open [W (io/writer jarman-path)] (.write W s))))
(defn fappend [s] (ioerr (with-open [W (io/writer jarman-path :append true)] (.write W s))))

;;;;;;;;;;;;;;;;;;;;;;;
;;; DECLARATION ENV ;;;
;;;;;;;;;;;;;;;;;;;;;;;

(defn- create-empty-dot-jarman []
  (if-not (not (.exists jarman-path))
    (do (fput ";; This is Main configuration `.jarman` file. Declare in it any machine or app-specyfic configurations\n")
        (fappend ";; Set global variables, by using `setq` macro and power of jarman customizing system\n")
        (fappend "\n")
        (doall
         (map (comp fappend (partial str ";;"))
              [" 1. Declare variable in any place you want in system\n"
               "    Attribute `:type` and `:group` aren't required, but\n"
               "    specifing may help you in debug moment\n;;\n"
               "    => (defvar some-global-variable nil \n;;          :type clojure.lang.PersistentArrayMap\n;;          :group :logical-group)\n"
               "\n"
               " 2. In `.jarman` set previosly declared in code variable\n;;\n"
               "    => (setq some-global-variable {:some-value {:a 1}})\n"]))
        (fappend "\n"))))

(defn dot-jarman-load []
  (ioerr (if (.exists jarman-path)
           (binding [*ns* (find-ns 'jarman.config.dot-jarman)] 
             (load-file (str jarman-path)))
           (create-empty-dot-jarman))
         (fn [e] "Cannot open `.jarman` config file")
         (fn [e] "Reading exception for `.jarman`. Maybe declaration or code in is corrupted")) nil)

(dot-jarman-load)


#_(require 'jarman.logic.view-manager)
#_(setq jarman.logic.view-manager/user-menu
      {"Admin space"
       {"User table"                [:user :table :user]
        "Permission edit"           [:permission :table :permission]}
       "Sale structure"
       {"Enterpreneur"              [:enterpreneur :table :enterpreneur]
        "Point of sale group"       [:point_of_sale_group :table :point_of_sale_group]
        "Point of sale group links" [:point_of_sale_group_links :table :point_of_sale_group_links],
        "Point of sale"             [:point_of_sale :table :point_of_sale]}
       "Repair contract"
       {"Repair contract"           [:repair_contract :table :repair_contract]
        "Repair reasons"            [:repair_reasons :table :repair_reasons]
        "Repair technical issue"    [:repair_technical_issue :table :repair_technical_issue]
        "Repair nature of problem"  [:repair_nature_of_problem :table :repair_nature_of_problem]
        "Cache register"            [:cache_register :table :cache_register]
        "Seal"                      [:seal :table :seal]}
       "Service contract"
       {"Service contract"          [:service_contract :table :service_contract]
        "Service contract month"    [:service_contract_month :table :service_contract_month]}})
