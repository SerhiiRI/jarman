;; 
;; Compilation: dev_tool.clj -> data.clj -> gui_tools.clj -> gui_alerts_service.clj -> gui_app.clj
;; 
(ns jarman.gui-app
  (:use seesaw.core
        seesaw.border
        seesaw.dev
        seesaw.mig
        jarman.tools.dev-tools
        jarman.gui-tools
        jarman.gui-alerts-service
        jarman.logic.data)
  (:require [jarman.resource-lib.icon-library :as icon]
            [clojure.string :as string]))

(import javax.swing.JLayeredPane)
(import java.awt.Color)
(import java.awt.Dimension)


;; ---------------------------------------------------- APP STARTER v
(def app-width 1000)
(def app-height 600)
(def app-bounds [0 0 app-width app-height])

;; Prepare operative layer
(def app
  "Description:
       Create panel for absolute position elements. In real it is GUI.
   "
  (new JLayeredPane))

;; Start app window
(-> (doto (seesaw.core/frame
           :title "DEBUG WINDOW" :undecorated? false
           :minimum-size [1000 :by 600]
           :content app)
      (.setLocationRelativeTo nil) pack! show!))

;; Start message service
(def alerts-s (message-server-creator app))
;; ---------------------------------------------------- APP STARTER ^



(def mig-app-left-f
  "Description:
      Vertical layout of elements, left part of app for functions
   Example:
      (mig-app-left-f  [(expand-btn 'Ukryte opcje 1' (some-button))] [(expand-btn 'Ukryte opcje 2')])
   Needed:
      expand-btn component is needed to corectly work
   "
  (fn [& args] (mig-panel
                :background "#fff"
                :border (line-border :left 4 :right 4 :color "#fff")
                :constraints ["wrap 1" "0px[fill, grow]0px" "0px[]0px"]
                :items (vec args))))

(def mig-app-right-f
  "Description: 
      Vertical layout for tabs and table on right part of app. 
      Tabs are inside horizontal panel on top.
   Example: 
      tabs  -> mig vector with elements    -> [(tab1) (tab2) (tab3)]
      array -> table like rows and columns -> [(table)]  
      (mig-app-right-f [(tab-btn 'Tab 1' true) (tab-btn 'Tab 2' false)] [(label-fn :text 'GRID')])
   Needed:
      tab-btn component is needed to corectly work
   "
  (fn [tabs array] (let [bg-color "#fff"]
                     (mig-panel
                      :background "#fff"
                      :constraints ["wrap 1" "0px[fill, grow]0px" "0px[30]0px[fill,grow]0px"]
                      :background "#eee"
                      :items [[(horizontal-panel
                                :background bg-color
                                :items tabs)]
                              [(vertical-panel
                                :background (new Color 0 0 0 0)
                                :items array)]]))))

(def layered-for-tabs (new JLayeredPane))

;; (defn set-row
;;   ([txt w h] (label :text txt
;;                     :size [w :by (- h 1)]
;;                     :background "#fff"
;;                     :border (line-border :top 1 :color "#000")))
;;   ([txt w h header?] (label :text txt
;;                             :size [w :by h]
;;                             :background "#666"
;;                             :foreground "#fff")))

;; (defn prepare-table
;;   [x y table-name & columns-name]
;;   (let [w 150
;;         row-h 30
;;         rows (map (fn [col] (set-row col w row-h)) columns-name)
;;         items (conj rows (set-row table-name w row-h true))
;;         h (* (count items) row-h)
;;         bg-c "#fff"]
;;    ;;  (println items)
;;     (vertical-panel
;;      :border (line-border :thickness 1 :color "#000")
;;      :bounds [x y w h]
;;      :background bg-c
;;      :items items)))

(def dbmap (list 
            {:id 21
            :table "cache_register"
            :prop
            {:table
             {:frontend-name "cache_register"
              :is-system? false
              :is-linker? false
              :allow-modifing? true
              :allow-deleting? true
              :allow-linking? true}
             :columns
             [{:field "id_point_of_sale"
               :representation "id_point_of_sale"
               :description nil
               :component-type "l"
               :column-type "bigint(20) unsigned"
               :private? false
               :editable? true
               :key-table "point_of_sale"}
              {:field "name"
               :representation "name"
               :description nil
               :component-type "i"
               :column-type "varchar(100)"
               :private? false
               :editable? true}
              {:field "serial_number"
               :representation "serial_number"
               :description nil
               :component-type "i"
               :column-type "varchar(100)"
               :private? false
               :editable? true}
              {:field "fiscal_number"
               :representation "fiscal_number"
               :description nil
               :component-type "i"
               :column-type "varchar(100)"
               :private? false
               :editable? true}
              {:field "manufacture_date"
               :representation "manufacture_date"
               :description nil
               :component-type "d"
               :column-type "date"
               :private? false
               :editable? true}
              {:field "first_registration_date"
               :representation "first_registration_date"
               :description nil
               :component-type "d"
               :column-type "date"
               :private? false
               :editable? true}
              {:field "is_working"
               :representation "is_working"
               :description nil
               :component-type "b"
               :column-type "tinyint(1)"
               :private? false
               :editable? true}
              {:field "version"
               :representation "version"
               :description nil
               :component-type "i"
               :column-type "varchar(100)"
               :private? false
               :editable? true}
              {:field "id_dev"
               :representation "id_dev"
               :description nil
               :component-type "l"
               :column-type "varchar(100)"
               :private? false
               :editable? true
               :key-table "dev"}
              {:field "producer"
               :representation "producer"
               :description nil
               :component-type "i"
               :column-type "varchar(100)"
               :private? false
               :editable? true}
              {:field "modem"
               :representation "modem"
               :description nil
               :component-type "i"
               :column-type "varchar(100)"
               :private? false
               :editable? true}
              {:field "modem_model"
               :representation "modem_model"
               :description nil
               :component-type "i"
               :column-type "varchar(100)"
               :private? false
               :editable? true}
              {:field "modem_serial_number"
               :representation "modem_serial_number"
               :description nil
               :component-type "i"
               :column-type "varchar(100)"
               :private? false
               :editable? true}
              {:field "modem_phone_number"
               :representation "modem_phone_number"
               :description nil
               :component-type "i"
               :column-type "varchar(100)"
               :private? false
               :editable? true}]}}
  {:id 22
   :table "enterpreneur"
   :prop
   {:table
    {:frontend-name "enterpreneur"
     :is-system? false
     :is-linker? false
     :allow-modifing? true
     :allow-deleting? true
     :allow-linking? true}
    :columns
    [{:field "ssreou"
      :representation "ssreou"
      :description nil
      :component-type "a"
      :column-type "tinytext"
      :private? false
      :editable? true}
     {:field "ownership_form"
      :representation "ownership_form"
      :description nil
      :component-type "i"
      :column-type "varchar(100)"
      :private? false
      :editable? true}
     {:field "vat_certificate"
      :representation "vat_certificate"
      :description nil
      :component-type "a"
      :column-type "tinytext"
      :private? false
      :editable? true}
     {:field "individual_tax_number"
      :representation "individual_tax_number"
      :description nil
      :component-type "i"
      :column-type "varchar(100)"
      :private? false
      :editable? true}
     {:field "director"
      :representation "director"
      :description nil
      :component-type "i"
      :column-type "varchar(100)"
      :private? false
      :editable? true}
     {:field "accountant"
      :representation "accountant"
      :description nil
      :component-type "i"
      :column-type "varchar(100)"
      :private? false
      :editable? true}
     {:field "legal_address"
      :representation "legal_address"
      :description nil
      :component-type "i"
      :column-type "varchar(100)"
      :private? false
      :editable? true}
     {:field "physical_address"
      :representation "physical_address"
      :description nil
      :component-type "i"
      :column-type "varchar(100)"
      :private? false
      :editable? true}
     {:field "contacts_information"
      :representation "contacts_information"
      :description nil
      :component-type "a"
      :column-type "mediumtext"
      :private? false
      :editable? true}]}}
  {:id 23
   :table "permission"
   :prop
   {:table
    {:frontend-name "permission"
     :is-system? false
     :is-linker? false
     :allow-modifing? true
     :allow-deleting? true
     :allow-linking? true}
    :columns
    [{:field "permission_name"
      :representation "permission_name"
      :description nil
      :component-type "i"
      :column-type "varchar(20)"
      :private? false
      :editable? true}
     {:field "configuration"
      :representation "configuration"
      :description nil
      :component-type "a"
      :column-type "tinytext"
      :private? false
      :editable? true}]}}
  {:id 24
   :table "point_of_sale"
   :prop
   {:table
    {:frontend-name "point_of_sale"
     :is-system? false
     :is-linker? false
     :allow-modifing? true
     :allow-deleting? true
     :allow-linking? true}
    :columns
    [{:field "id_enterpreneur"
      :representation "id_enterpreneur"
      :description nil
      :component-type "l"
      :column-type "bigint(20) unsigned"
      :private? false
      :editable? true
      :key-table "enterpreneur"}
     {:field "name"
      :representation "name"
      :description nil
      :component-type "i"
      :column-type "varchar(100)"
      :private? false
      :editable? true}
     {:field "physical_address"
      :representation "physical_address"
      :description nil
      :component-type "i"
      :column-type "varchar(100)"
      :private? false
      :editable? true}
     {:field "telefons"
      :representation "telefons"
      :description nil
      :component-type "i"
      :column-type "varchar(100)"
      :private? false
      :editable? true}]}}
  {:id 25
   :table "point_of_sale_group"
   :prop
   {:table
    {:frontend-name "point_of_sale_group"
     :is-system? false
     :is-linker? false
     :allow-modifing? true
     :allow-deleting? true
     :allow-linking? true}
    :columns
    [{:field "group_name"
      :representation "group_name"
      :description nil
      :component-type "i"
      :column-type "varchar(100)"
      :private? false
      :editable? true}
     {:field "metadata"
      :representation "metadata"
      :description nil
      :component-type "a"
      :column-type "mediumtext"
      :private? false
      :editable? true}]}}
  {:id 26
   :table "point_of_sale_group_links"
   :prop
   {:table
    {:frontend-name "point_of_sale_group_links"
     :is-system? true
     :is-linker? true
     :allow-modifing? false
     :allow-deleting? false
     :allow-linking? false}
    :columns
    [{:field "id_point_of_sale_group"
      :representation "id_point_of_sale_group"
      :description nil
      :component-type "l"
      :column-type "bigint(20) unsigned"
      :private? false
      :editable? true
      :key-table "point_of_sale_group"}
     {:field "id_point_of_sale"
      :representation "id_point_of_sale"
      :description nil
      :component-type "l"
      :column-type "bigint(20) unsigned"
      :private? false
      :editable? true
      :key-table "point_of_sale"}]}}
  {:id 27
   :table "repair_contract"
   :prop
   {:table
    {:frontend-name "repair_contract"
     :is-system? false
     :is-linker? false
     :allow-modifing? true
     :allow-deleting? true
     :allow-linking? true}
    :columns
    [{:field "id_cache_register"
      :representation "id_cache_register"
      :description nil
      :component-type "l"
      :column-type "bigint(20) unsigned"
      :private? false
      :editable? true
      :key-table "cache_register"}
     {:field "id_point_of_sale"
      :representation "id_point_of_sale"
      :description nil
      :component-type "l"
      :column-type "bigint(20) unsigned"
      :private? false
      :editable? true
      :key-table "point_of_sale"}
     {:field "creation_contract_date"
      :representation "creation_contract_date"
      :description nil
      :component-type "d"
      :column-type "date"
      :private? false
      :editable? true}
     {:field "last_change_contract_date"
      :representation "last_change_contract_date"
      :description nil
      :component-type "d"
      :column-type "date"
      :private? false
      :editable? true}
     {:field "contract_terms_date"
      :representation "contract_terms_date"
      :description nil
      :component-type "d"
      :column-type "date"
      :private? false
      :editable? true}
     {:field "cache_register_register_date"
      :representation "cache_register_register_date"
      :description nil
      :component-type "d"
      :column-type "date"
      :private? false
      :editable? true}
     {:field "remove_security_seal_date"
      :representation "remove_security_seal_date"
      :description nil
      :component-type "dt"
      :column-type "datetime"
      :private? false
      :editable? true}
     {:field "cause_of_removing_seal"
      :representation "cause_of_removing_seal"
      :description nil
      :component-type "a"
      :column-type "mediumtext"
      :private? false
      :editable? true}
     {:field "technical_problem"
      :representation "technical_problem"
      :description nil
      :component-type "a"
      :column-type "mediumtext"
      :private? false
      :editable? true}
     {:field "active_seal"
      :representation "active_seal"
      :description nil
      :component-type "a"
      :column-type "mediumtext"
      :private? false
      :editable? true}]}}
  {:id 28
   :table "seal"
   :prop
   {:table
    {:frontend-name "seal"
     :is-system? false
     :is-linker? false
     :allow-modifing? true
     :allow-deleting? true
     :allow-linking? true}
    :columns
    [{:field "seal_number"
      :representation "seal_number"
      :description nil
      :component-type "i"
      :column-type "varchar(100)"
      :private? false
      :editable? true}
     {:field "to_date"
      :representation "to_date"
      :description nil
      :component-type "d"
      :column-type "date"
      :private? false
      :editable? true}]}}
  {:id 29
   :table "service_contract"
   :prop
   {:table
    {:frontend-name "service_contract"
     :is-system? false
     :is-linker? false
     :allow-modifing? true
     :allow-deleting? true
     :allow-linking? true}
    :columns
    [{:field "id_point_of_sale"
      :representation "id_point_of_sale"
      :description nil
      :component-type "l"
      :column-type "bigint(20) unsigned"
      :private? false
      :editable? true
      :key-table "point_of_sale"}
     {:field "register_contract_date"
      :representation "register_contract_date"
      :description nil
      :component-type "d"
      :column-type "date"
      :private? false
      :editable? true}
     {:field "contract_term_date"
      :representation "contract_term_date"
      :description nil
      :component-type "d"
      :column-type "date"
      :private? false
      :editable? true}
     {:field "money_per_month"
      :representation "money_per_month"
      :description nil
      :component-type "n"
      :column-type "int(11)"
      :private? false
      :editable? true}]}}
  {:id 30
   :table "user"
   :prop
   {:table
    {:frontend-name "user"
     :is-system? false
     :is-linker? false
     :allow-modifing? true
     :allow-deleting? true
     :allow-linking? true}
    :columns
    [{:field "login"
      :representation "login"
      :description nil
      :component-type "i"
      :column-type "varchar(100)"
      :private? false
      :editable? true}
     {:field "password"
      :representation "password"
      :description nil
      :component-type "i"
      :column-type "varchar(100)"
      :private? false
      :editable? true}
     {:field "first_name"
      :representation "first_name"
      :description nil
      :component-type "i"
      :column-type "varchar(100)"
      :private? false
      :editable? true}
     {:field "last_name"
      :representation "last_name"
      :description nil
      :component-type "i"
      :column-type "varchar(100)"
      :private? false
      :editable? true}
     {:field "id_permission"
      :representation "id_permission"
      :description nil
      :component-type "l"
      :column-type "bigint(120) unsigned"
      :private? false
      :editable? true
      :key-table "permission"}]}}))

;; (prepare-table 10 10 "Users" "FName" "LName" "LOGIN")

;; (getset)

;; (def dbmap (getset))
;; 

(defn calculate-tables-height
  "Description: 
      Return prefered size for table
   Example: 
      (calculate-table-size {:db :data :in :map})   
   Needed:
   "
  [db-data]
  (doall (map (fn [tab] (vec (list (* 30 (count (get (get tab :prop) :columns))) tab))) db-data)))

(def dbmap-sorted (reverse (sort-by first (calculate-tables-height dbmap))))


(defn set-col-as-row
  [data] (label :text (get data :name)
                :size [(get data :width) :by (cond
                                               (= (get data :type) "header") (- (get data :height) 2)
                                               :else                         (- (get data :height) 0))]
                :icon (cond
                        (= (get data :type) "key") (image-scale icon/key-blue-64-png (/ (get data :height) 1))
                        :else nil)
                :background (cond
                              (= (get data :type) "header") "#666"
                              (= (get data :type) "key")    "#f7d67c"
                              :else                         "#fff")
                :foreground (cond
                              (= (get data :type) "header") "#fff"
                              :else                         "#000")
                :border (cond
                          (= (get data :type) "header") (compound-border (empty-border :thickness 4))
                          :else                         (compound-border (empty-border :thickness 4) (line-border :top 1 :color "#000")))))



(def bound-x-atom (atom 10))
(def col-in-row-atom (atom 0))
(def max-col-in-row 4)

(defn prepare-table-with-map
  [offset-x offset-y data]
  (let [w (+ 50 (* 6 (last (sort (concat (map (fn [col] (count (get col :representation))) (get (get data :prop) :columns)) (list (count (get data :table))))))))
        x @bound-x-atom
        mnoznik (int (/ @col-in-row-atom max-col-in-row))
        y (+ offset-y (* mnoznik (first (nth dbmap-sorted (* max-col-in-row mnoznik)))))
        row-h 30
        rows (map (fn [col] (set-col-as-row {:name (get col :field) :width w :height row-h :type (if (contains? col :key-table) "key" "row")})) (get (get data :prop) :columns))
        items (conj rows (set-col-as-row {:name (get data :table) :width w :height row-h :type "header"}))
        h (* (count items) row-h)
        bg-c "#fff"
        line-size-hover 2
        border (line-border :thickness 1 :color "#000")
        border-hover (line-border :thickness line-size-hover :color "#000")]
   ;;  (println items)
    (do
      (swap! col-in-row-atom inc)
      (if (> (/ @col-in-row-atom max-col-in-row) (int (/ @col-in-row-atom max-col-in-row))) (swap! bound-x-atom (fn [bxatom] (+ bxatom w offset-x))) (reset! bound-x-atom 10))
      (let []
        (vertical-panel
        :tip "Double click to show relation."
        :id (get data :table)
        :border border
        :bounds [x y w h]
        :background bg-c
        :items items
        :listen [:mouse-entered (fn [e] (config! e :cursor :hand :border border-hover :bounds [(- x (/ line-size-hover 2)) (- y (/ line-size-hover 2)) (+ w line-size-hover) (+ h line-size-hover)]))
                 :mouse-exited  (fn [e] (config! e :border border :bounds [x y w h]))])))))



;; (split-at 6 (vec dbmap-sorted))

;; (def calculate-tables-bounds
;;   [tables-sorted columns-in-row margin]
;;   (let [splited (split-at columns-in-row (vec tables-sorted))]
;;     (map (fn [row] (
;;                     map (fn [tab] (
                                   
;;                                    )) row
;;                     )) splited)))

;; (split-at 2 [[420 {}] [400 {}] [380 {}] [320 {}][220 {}]])

(reduce (fn [acc tab]
          (do
            (.add layered-for-tabs (prepare-table-with-map 10 10 (last tab)) (new Integer 0))
            (inc acc)))
        0 dbmap-sorted)


;; (reduce (fn [acc x] (println acc x) (+ acc x)) 0 [1 2 3 4 5])


;; (do
;;   (.add layered-for-tabs (prepare-table-with-map 10 10 (first (getset "user"))) (new Integer 0))
;;   (.add layered-for-tabs (prepare-table-with-map 210 30 (first (getset "permission"))) (new Integer 0))
;;   (.add layered-for-tabs (prepare-table-with-map 410 70 (first (getset "point_of_sale"))) (new Integer 0))
;; ;;   (.add layered-for-tabs (prepare-table 180 60 "Users" "Login" "Password" "Status") (new Integer 0))
;; ;;   (.add layered-for-tabs (prepare-table 580 60 "Users" "Login" "Password" "Status") (new Integer 0))
;; ;;   (.add layered-for-tabs (prepare-table 180 460 "Users" "Login" "Password" "Status") (new Integer 0))
;;   )

(defn refresh-layered-for-tables
  [] (do (if (> (count (seesaw.util/children layered-for-tabs)) 0)
           (let [max-w (apply max (map (fn [item]  (+ (.getX (config item :bounds)) (.getWidth  (config item :bounds)))) (seesaw.util/children layered-for-tabs)))
                 parent-w (getWidth (.getParent layered-for-tabs))
                 max-h (apply max (map (fn [item]  (+ (.getY (config item :bounds)) (.getHeight  (config item :bounds)))) (seesaw.util/children layered-for-tabs)))
                 parent-h (getHeight (.getParent layered-for-tabs))]
             (do (.setPreferredSize layered-for-tabs (new Dimension (if (> parent-w max-w) parent-w max-w) (if (> parent-h max-h) parent-h max-h)))
                 (.setSize layered-for-tabs (new Dimension (if (> parent-w max-w) parent-w max-w) (if (> parent-h max-h) parent-h max-h)))))

           (.setPreferredSize layered-for-tabs (new Dimension
                                                    (getWidth  (.getParent layered-for-tabs))
                                                    (getHeight (.getParent layered-for-tabs)))))))

(def jarmanapp
  "Description:
      Main space for app inside JLayeredPane. There is menu with expand btns and space for tables with tab btns.
   "
  (grid-panel
   :bounds app-bounds
   :items [(mig-panel
            :constraints [""
                          "0px[50, fill]0px[200, fill]0px[fill, grow]15px"
                          "0px[fill, grow]39px"]
            :items [[(label-fn :background "#eee" :size [50 :by 50])]
                    [(mig-app-left-f  [(expand-btn "Ukryte opcje 1"
                                                   (label-fn :text "Opcja 1" :background "#fff" :size [200 :by 25]
                                                             :listen [:mouse-clicked (fn [e] (alerts-s :set {:header "Test" :body "Bardzo dluga testowa wiadomość, która nie jest taka prosta do ogarnięcia w seesaw."} (message alerts-s) 3))])
                                                   (label-fn :text "Opcja 2" :background "#fff" :size [200 :by 25]
                                                             :listen [:mouse-clicked (fn [e] (alerts-s :set {:header "Witaj" :body "Świecie"} (message alerts-s) 5))]))]
                                      [(expand-btn "Ukryte opcje 2")])]
                    [(mig-app-right-f [(tab-btn "Tab 1" true [70 30]) (tab-btn "Tab 2" false [70 30])]
                                      [(scrollable layered-for-tabs :border nil :id :layered-for-tables)])]])]))



(def onresize-f
  "Description:
      Resize component inside JLayeredPane on main frame resize event.
   "
  (fn [e] (do
            (refresh-layered-for-tables)
            (template-resize jarmanapp)
            (alerts-rebounds-f e))))


(defn app-build
  "Description:
      Change starter window. Add prepare components and functions.
   Example:
      (app-build)
   Neede:
      Function need jarmanapp with app content
      Function need btn-icon-f function for category buttons
   "
  [] (let [menu-icon-size 50]
       (do
         (.add app jarmanapp (new Integer 5))
         (.add app (slider-ico-btn (image-scale icon/user-64x64-2-png menu-icon-size) 0 menu-icon-size "Klienci" {}) (new Integer 10))
         (.add app (slider-ico-btn (image-scale icon/settings-64x64-png menu-icon-size) 1 menu-icon-size "Konfiguracja" {}) (new Integer 10))
         (.add app (slider-ico-btn (image-scale icon/I-64-png menu-icon-size) 2 menu-icon-size "Powiadomienia" {:onclick (fn [e] (alerts-s :show))}) (new Integer 10))
         (config! (to-root app) :listen [:component-resized (fn [e] (onresize-f e))])
         (onresize-f app)
         (.repaint app))))

;; Complete window
(app-build)
;; (refresh-layered-for-tables)
;; (if (> (count (seesaw.util/children layered-for-tabs)) 0) (.setSize layered-for-tabs (new Dimension 730 550)))


;; (alerts-s :show)
;; (alerts-s :hide)
;; (alerts-s :count-all)
;; (alerts-s :count-active)
;; (alerts-s :count-hidden)
;; (alerts-s :clear)