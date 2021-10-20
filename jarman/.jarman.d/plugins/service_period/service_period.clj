(ns plugins.service-period.service-period
  (:require
   [jarman.faces              :as face]
   [jarman.tools.lang         :refer :all] 
   [clojure.spec.alpha        :as s]
   [seesaw.core               :as c]
   [seesaw.border             :as b]
   [jarman.gui.gui-tools      :as gtool]
   [jarman.gui.gui-components :as gcomp]
   [jarman.gui.gui-migrid     :as gmg]
   [jarman.gui.gui-calendar   :as calndr]
   [jarman.logic.sql-tool     :refer [select!]]
   [jarman.logic.connection   :as db]
   [jarman.logic.state        :as state]
   [jarman.gui.gui-style      :as gs]
   [jarman.plugin.spec        :as spec]
   [jarman.interaction        :as i]
   [jarman.plugin.data-toolkit       :as query-toolkit]
   [jarman.resource-lib.icon-library :as icon]
   [plugin.service-period.service-period-library :as req]
   ;; external
   [jarman.external :refer [register-custom-view-plugin]])
  (:import
   (jiconfont.icons.google_material_design_icons GoogleMaterialDesignIcons)))

;;;;;;;;;;;;;;
;;; STATE  ;;;  
;;;;;;;;;;;;;;

(def state (atom {}))

(defn- action-handler
  [state action-m]
  (case (:action action-m)
    ;; ------------------ Default ---------------------
    :add-missing  (assoc-in state (:path action-m) nil)
    :test         (do (println "\nTest:n") state)
    ;; ------------------------------------------------
))

(defn- create-disptcher [atom-var]
  (fn [action-m]
    (swap! atom-var (fn [state] (action-handler state action-m)))))

(defn- create-state-template [plugin-path global-configuration-getter root state!]
  (reset! state
          {:debug-mode           true ;; Add to tree info about path
           :plugin-path          plugin-path
           :plugin-global-config global-configuration-getter
           :plugin-config        (get-in (global-configuration-getter) (conj plugin-path :config) {})
           :plugin-toolkit       (get-in (global-configuration-getter) (conj plugin-path :toolkit) {})
           :enterprises-m        (atom {})
           :contracts-m          (atom {})
           :subcontracts-m       (atom {})
           :currency             "UAH"
           :root                 root
           :state!               state!
           :subcontracts-payment-state (atom {})})
  state)

;;;;;;;;;;;;;;;;;;;;;;;;
;;; HELPER FUNCTIONS ;;;  
;;;;;;;;;;;;;;;;;;;;;;;;
(defn- isNumber? [val-s]
  (try (float (read-string val-s))
    (catch Exception e false)))

(declare build-expand-contracts-for-enterprise)

(defn- refresh-data-and-panel
  "Description:
     Load data again and rerender panel with contracts."
  [state!]
  (let [{:keys [root expand-btns-box enterprises-m contracts-m subcontracts-m]} (state!)
        fresh-data ((:download-data-map (:plugin-toolkit (state!))))]

    (reset! enterprises-m   (:enterprises-m   fresh-data))
    (reset! contracts-m     (:contracts-m     fresh-data))
    (reset! subcontracts-m  (:subcontracts-m  fresh-data))
    (swap! (state! :atom) #(assoc % :enterprises-list (:enterprises-list fresh-data)))
    
    (let [items (gtool/join-mig-items
                 (doall
                  (map
                   (fn [[enterprise-id]]
                     (build-expand-contracts-for-enterprise state! enterprise-id))
                   @enterprises-m)))]
      (c/config!
       expand-btns-box
       :items (if (empty? items)
                [[(gmg/migrid :v :center {:gap [25]}
                              (c/label :text (gtool/get-lang-infos :no-items)
                                       :font (gs/getFont 16)
                                       :foreground face/c-icon))]]
                items)))
    (doto root (.revalidate) (.repaint))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; Payments
;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- listing-all-selected-checkboxes-path
  [state!]
  (filter #(not (empty? %))
   (apply concat (doall (map
            (fn [[eid e]]
              (apply concat (map (fn [[cid c]]
                      (map (fn [[sid s]]
                             (if (:selected? s) [eid cid sid] [])) c)) e)))
            @(:subcontracts-m (state!)))))))

;; (listing-all-selected-checkboxes-path (:state! @state))
;; (:subcontracts-m @state)

(defn- pay-for-pointed-subcontracts
  [state! subcontract-path]
  (let [assoc-path (join-vec subcontract-path [:service_contract_month.was_payed])]
    (swap! (:subcontracts-m (state!)) #(assoc-in % assoc-path true))))

(defn- pay-for-selected-subcontracts
  "Description:
     Set payed to subcontracts by paths list
  Example:
     (pay-for-selected-subcontracts state! [[:1 :21 :1021] [:1 :21 :1022]] :payed? true)"
  [state! paths-list & {:keys [payed?] :or {payed? true}}]
  (->> ((fn run-by-list [m sub-paths]
         (if (empty? sub-paths) m
             (run-by-list (assoc-in m (join-vec (first sub-paths) [:service_contract_month.was_payed]) payed?) (drop 1 sub-paths))))
       @(:subcontracts-m (state!)) paths-list)
      (reset! (:subcontracts-m (state!)))))

;; (pay-for-pointed-subcontracts (:state! @state) [:1 :21 :1021])
;; (pay-for-selected-subcontracts (:state! @state) [[:2 :24 :1037] [:2 :24 :1038]] :payed? false)

(defn- paths-to-id
  [paths]
  (map #(Integer/parseInt (name (last %))) paths))

(defn- subcontract-payment-done
  "Description:
     Update payment done in state and DB.
  "
  [state!
   & {:keys [subcontract-path]
      :or {subcontract-path nil}}]
  (let [selected-paths (if (nil? subcontract-path) (listing-all-selected-checkboxes-path state!))
        selected-ids (if (nil? subcontract-path)
                       (paths-to-id selected-paths)
                       [(last subcontract-path)])]
    (if (empty? selected-ids)
      (i/warning (gtool/get-lang-header :warning)
                 (gtool/get-lang-alerts :select-checkbox-for-payment))

      (try
        (do
          ;; Set payed in DB
          (req/update-service-month-to-payed selected-ids)
          ;;(println selected-paths)
          (if (nil? subcontract-path)
            (pay-for-selected-subcontracts state! selected-paths)
            (pay-for-pointed-subcontracts  state! subcontract-path))
          ;; Update state
          (i/success (gtool/get-lang-header :success)
                     (gtool/get-lang-alerts :payments-finished-successfully))
          true)
        (catch Exception e (i/danger (gtool/get-lang-header :failed) (str "DB or State problem.\n" (.getMessage e))))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; Contract panel
;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- update-subcontracts-state
  "Description:
     Swap subcontracts state if some prices was changed in DB."
  [state!]
  (let [new-map @(:subcontracts-m (state!))
        contract-path (:path @(:subcontracts-payment-state (state!)))
        swap-map      ((fn assoc-next [m sps]
                         (let [[id price] (first sps)
                               price price
                               next-sps (drop 1 sps)
                               path-to-update (join-vec contract-path [(keyword (str id)) :service_contract_month.money_per_month])
                               next-map (assoc-in m path-to-update price)]
                           (if (empty? next-sps)
                             next-map
                             (assoc-next next-map next-sps))))
                       new-map (vec (:updated-prices @(:subcontracts-payment-state (state!)))))]
    (reset! (:subcontracts-m (state!)) swap-map))
  (swap! (:subcontracts-payment-state (state!)) #(assoc-in % [:updated-prices] {})))

(defn- back-to-main-panel
  "Description
     Load contracts panel with subcontracts list using updated state."
  [state!]
  (let [{:keys [root new-contract-form expand-btns-box btns-menu-bar]} (state!)]
    (c/config! (:root (state!)) :items (gtool/join-mig-items btns-menu-bar new-contract-form (gcomp/min-scrollbox expand-btns-box)))
    (reset! (:subcontracts-payment-state (state!)) {:path [] :db-saved false :updated-prices {}})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; GUI for one Contract;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- update-subcontract-price
  "Description
     Update data about subcontracts price in DB."
  [state!]
  (let [update-fn (:update-service-money (:plugin-toolkit (state!)))]
    (doall (map (fn [[id money]] (update-fn id money)) (vec (:updated-prices @(:subcontracts-payment-state (state!))))))
    (i/info (gtool/get-lang-header :info)
            (gtool/get-lang-alerts :updated-data))
    (update-subcontracts-state state!)))

(defn- number-input
  [begin-text only-int? more-fns]
  (gcomp/input-text
   :underline-off true
   :halign :center
   :args [:text begin-text
          :listen [:caret-update
                   (fn [e] (if-not  (= (c/config e :user-data) (c/config e :text))
                             (let [new-val (isNumber? (c/config e :text))
                                   new-val (if (and only-int? (not (= false new-val))) (int new-val) new-val)]
                               (if-not (= false new-val)
                                 (do
                                   (more-fns new-val)
                                   (c/config! e :user-data (str new-val)))))))
                   
                   :focus-lost (fn [e] (c/config! e :text (c/config e :user-data)))]]))

(defn- calculate-contract-price
  "Description:
     Using :subcontracts-m atom in state, get info about prices and calculate.
     Get only prices of not payed subcontracts."
  [state! contract-path]
  (let [subcontracts-m (get-in @(:subcontracts-m (state!)) contract-path)
        contract-price (apply + (flatten
                                 (doall (map
                                         (fn [[subcontract-id subcontract]]
                                           (if (:service_contract_month.was_payed subcontract) 0
                                               (try
                                                 (+ 0 (:service_contract_month.money_per_month subcontract))
                                                 (catch Exception e 0))))
                                         (get-in @(:subcontracts-m (state!)) contract-path)))))]
    (if (or (float? contract-price) (int? contract-price))
      contract-price 0)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; INFO BAR
;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- info-bar-params
  "Description:
     Data for info bar for contract view. Add some more, it's not any problem.
     Rule is [title, info, title, info, ...]"
  [enterprise contract-price date-start date-end currency]
  [(gtool/get-lang-header :enterprise)
   (:enterprise.name enterprise)
   (gtool/get-lang-header :VAT-Certificate)
   (:enterprise.vat_certificate enterprise)
   (gtool/get-lang-header :director)
   (:enterprise.director enterprise)
   (gtool/get-lang-header :date-start)
   date-start
   (gtool/get-lang-header :date-end)
   date-end
   (gtool/get-lang-header :price)
   (str contract-price " " currency)])

(defn- info-bar-template
  "Description:
     Create info bar content in columns. Default 3 columns."
  [state! enterprise contract-price date-start date-end collumns-count]
  (let [font-size 14
        cc (* 2 collumns-count)
        collumns-v (map (fn [idx](gmg/migrid :v :f {:gap (if (not (even? idx)) [5 5 5 30] [5])} [])) (take cc (range)))
        counter    (atom 0)
        label-fn   (fn [txt & {:keys [bold]}]
                     (c/label :text txt :font (if bold (gs/getFont :bold font-size) (gs/getFont font-size))))]
    (doall
     (map
      (fn [info]
        (.add (nth collumns-v @counter) (label-fn info :bold (not (even? @counter))))
        (if (> (+ 1 @counter) (- cc 1)) (reset! counter 0) (swap! counter #(inc %))))
      (info-bar-params enterprise contract-price date-start date-end (:currency (state!)))))
    collumns-v))

(defn- info-bar
  "Description:
     Create info bar for contract view. "
  [state! contract-path]
  (let [enterprise-id (first contract-path)
        enterprise    (enterprise-id @(:enterprises-m (state!)))
        contract      (get-in @(:contracts-m (state!)) contract-path)
        render-fn     (fn [] (info-bar-template
                              state!
                              enterprise
                              (calculate-contract-price state! contract-path)
                              (clojure.string/replace (:service_contract.contract_start_term contract) #"-" "/ ")
                              (clojure.string/replace (:service_contract.contract_end_term contract)   #"-" "/ ")
                              3))
        panel (gmg/migrid
               :> "[10:, fill]" {:gap [15] :args [:border (b/line-border :top 10 :bottom 10 :color face/c-layout-background)
                                                         :background face/c-compos-background-light]}
               (render-fn))] 
    (add-watch (:subcontracts-m (state!)) :contract-view
             (fn [key atom old-m new-m]
               (c/config! panel :items (gtool/join-mig-items (render-fn)))
               (.repaint panel)))
    (gcomp/min-scrollbox panel)))

(defn- panel-with-subcontract-rows
  "Description
    return mig-panel for one subcontract"
  [state! subcontract-path clicked-sub?]
  (let [{id              :service_contract_month.id
         start-date      :service_contract_month.service_month_start 
         end-date        :service_contract_month.service_month_end
         payment-st      :service_contract_month.was_payed
         payment         :service_contract_month.money_per_month}
        (get-in @(:subcontracts-m (state!)) subcontract-path)
        
        subcontract-price-fn #(get-in @(:subcontracts-m (state!)) (join-vec subcontract-path [:service_contract_month.money_per_month]))
        subcontracts-payment-a (:subcontracts-payment-state (state!))

        x (reset! (:subcontracts-payment-state (state!)) {:path (vec (butlast subcontract-path))
                                                          :updated-prices {}})

        payment-path  (join-vec subcontract-path)
        
        label-fn (fn [text foreground]
                   (seesaw.core/label :text (str text)
                                      :foreground foreground
                                      :halign :center))       
        
        date     (str (clojure.string/replace start-date #"-" "/ ") "  -  " (clojure.string/replace end-date #"-" "/ "))

        panel (gmg/migrid ;; subcontract row
               :> :f
               {:gap [5 5 10 20]
                :args [:background face/c-compos-background
                       :border (if clicked-sub?
                                 (b/compound-border
                                  (b/line-border  :bottom 2 :top 2 :color face/c-icon)
                                  (b/line-border :bottom 3 :color face/c-layout-background))
                                 (b/line-border :bottom 3 :color face/c-layout-background))]}
               [])
        
        render-fn (fn rerender [is-payed?]
                    [(gmg/migrid ;; icon and value to pay 
                      :> "[fill]5px[70:, fill]" :g {:gap [0 5]}
                      [(if is-payed?
                         ;; icon done
                         [(c/label :icon (gs/icon GoogleMaterialDesignIcons/CHECK)
                                   :tip (gtool/get-lang-tip :payment-is-done)
                                   :listen [:mouse-entered (fn [e] (c/config! e :cursor :hand))])]
                         ;; icon help
                         [(c/label :icon (gs/icon GoogleMaterialDesignIcons/HELP)
                                   :tip (gtool/get-lang-tip :you-can-edit-this-position)
                                   :listen [:mouse-entered (fn [e] (c/config! e :cursor :hand))])])

                       ;; Price
                       (if is-payed? 
                         ;; price in label
                         (label-fn (subcontract-price-fn) face/c-foreground)
                         ;; price in input
                         (number-input (subcontract-price-fn)
                                       false
                                       (fn [new-val]
                                         (swap! subcontracts-payment-a #(assoc-in % [:updated-prices id] new-val)))))])

                     ;; date
                     (label-fn date face/c-foreground)
                     
                     ;; status
                     (label-fn (if is-payed?
                                 (gtool/get-lang-infos :payment-done-lock-row)
                                 (gtool/get-lang-infos :wait-for-pay))
                               (if is-payed? face/c-green face/c-orange))
                     
                     ;; pay for subcontract button
                     (if is-payed? [] [(gcomp/button-slim
                                        (gtool/get-lang-btns :pay)
                                        :args [:icon (gs/icon GoogleMaterialDesignIcons/MONETIZATION_ON)]
                                        :onClick (fn [e]
                                                   (-> (subcontract-payment-done state! :subcontract-path subcontract-path)
                                                       (if (c/config! panel :items (gtool/join-mig-items (rerender true)))))
                                                   (doto panel (.revalidate) (.repaint))))])])]
    (c/config! panel :items (gtool/join-mig-items (render-fn payment-st)))
    panel))

(defn- display-contract
  "Description
    return mig-panel with button-bar and panels with all periods of one contract"
  [state! contract-path clicked-sub-path]
  (let [btns-menu-bar (gcomp/menu-bar
                       {:buttons [[(gtool/get-lang-btns :back)
                                   (gs/icon GoogleMaterialDesignIcons/ARROW_BACK)
                                   (fn [e] (back-to-main-panel state!))]
                                  
                                  [(gtool/get-lang-btns :save)
                                   (gs/icon GoogleMaterialDesignIcons/SAVE)
                                   (fn [e]
                                     (.requestFocus (c/to-widget e))
                                     (update-subcontract-price state!))]
                                  
                                  [(gtool/get-lang-btns :export-odt)
                                   icon/odt-64-png
                                   (fn [e] )]]})
        
        subcontracts (gmg/migrid :v
                      (doall (map
                              (fn [[subcontract-id subcontract]]
                                (let [subcontract-path (join-vec contract-path [subcontract-id])]
                                    (panel-with-subcontract-rows state! subcontract-path (= subcontract-path clicked-sub-path))))
                              (get-in @(:subcontracts-m (state!)) contract-path))))]
    (list btns-menu-bar (info-bar state! contract-path) (gcomp/min-scrollbox subcontracts))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;
;;; EXPAND PANELS ;;;
;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- new-watcher [some-atom watcher-id watch-path action]
  (add-watch some-atom watcher-id
             (fn [key atom old-m new-m]
               (let [[left right same] (clojure.data/diff (get-in new-m watch-path) (get-in old-m watch-path))]
                 ;; Check if in contract map are some differents
                 (if-not (and (nil? left) (nil? right))
                   (try
                     (action)
                     (catch Exception e "";; (println (str "service_period/new-watcher:\n" (.getMessage e)))
                            )))))))

(defn- i-checkbox [checkbox-selected?]
  (if checkbox-selected?
    (gs/icon GoogleMaterialDesignIcons/CHECK_BOX)
    (gs/icon GoogleMaterialDesignIcons/CHECK_BOX_OUTLINE_BLANK)))

(defn- select-checkbox-for-subcontract
  [state! subcontract-path select?]
  (swap! (:subcontracts-m (state!)) #(assoc-in % (join-vec subcontract-path [:selected?]) select?)))

(defn- subcontract-checkbox ;; OK
  [state! subcontract-path checkbox-selected?]
  (c/label :icon (i-checkbox checkbox-selected?)
           :listen [:mouse-clicked
                    (fn [e] (let [selected-path  (join-vec subcontract-path [:selected?])
                                  self-selected? (if (= true (get-in @(:subcontracts-m (state!)) selected-path)) false true)]
                              (c/config! e :icon (i-checkbox self-selected?))
                              (.repaint (c/to-widget e))
                              
                              ;; select or unselect subcontract. Path return true or false
                              (select-checkbox-for-subcontract state! subcontract-path self-selected?)))]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;;  CHECKBOXES FOR CONTRACTS
;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- contract-payed?
  [state! contract-path]
  (let [unpayed? (some false?
                     (flatten
                      (doall (map
                              (fn [[subcontract-id subcontract]]
                                (:service_contract_month.was_payed subcontract))
                              (get-in @(:subcontracts-m (state!)) contract-path)))))]
    (if (nil? unpayed?) true (not unpayed?))))

(defn- check-if-contract-selected
  [state! contract-path]
  (let [selected? (some false?
                        (flatten
                         (doall (map
                                 (fn [[subcontract-id subcontract]]
                                   (if (:service_contract_month.was_payed subcontract)
                                     true (:selected? subcontract)))
                                 (get-in @(:subcontracts-m (state!)) contract-path)))))]
    (if (nil? selected?) true (not selected?))))

(defn- select-all-subcontracts
  [state! contract-path selected?]
  (let [subcontracts-m     (get-in @(:subcontracts-m (state!)) contract-path)
        new-subcontracts-m (into {} (map
                                     (fn [[subcontract-id subcontract]]
                                       {subcontract-id (assoc subcontract :selected? selected?)})
                                     subcontracts-m))]
    (swap! (:subcontracts-m (state!)) #(assoc-in % contract-path new-subcontracts-m))
    new-subcontracts-m))

(defn- contract-checkbox
  [state! root render-fn render-header contract-path checkbox-selected?]
  (let [cbox (c/label :icon (i-checkbox checkbox-selected?)
                      :listen [:mouse-clicked
                               (fn [e] ;; SELECT OR UNSELECT ALL SUBCONTRACTS
                                 (let [self-selected? (not (check-if-contract-selected state! contract-path))]
                                   (select-all-subcontracts state! contract-path self-selected?)
                                   (swap! (:contracts-m (state!)) #(assoc-in % (join-vec contract-path [:selected?]) self-selected?))
                                   (c/config! e :icon (i-checkbox self-selected?))))])
        
        watcher-id (keyword (str "contract-" (clojure.string/join "-" contract-path)))]

    ;; If in subcontract something will be unchecked or all will be checked then set contract checkbox selected or not
    ;; WATCH IF ALL SUBCONTRACTS SELECTED
    (new-watcher (:subcontracts-m (state!)) watcher-id contract-path
                 (fn []
                   (let [payed?        (contract-payed? state! contract-path)
                         all-selected? (check-if-contract-selected state! contract-path)
                         contract-price (calculate-contract-price state! contract-path)]

                     (c/config! root :items (gtool/join-mig-items (render-fn)))
                     (let [expand-header-box (.getParent cbox)
                           expand-before-title (first  (seesaw.util/children expand-header-box))
                           expand-title        (second (seesaw.util/children expand-header-box))
                           expand-other        (drop 2 (seesaw.util/children expand-header-box))]
                       
                       (c/config! expand-title :text (render-header contract-price))
                       (c/config! expand-header-box :items (gtool/join-mig-items
                                                            expand-before-title
                                                            expand-title
                                                            expand-other))
                       (if payed?
                         ;; set done icon and refresh expand btn header
                         (c/config! cbox :icon (gs/icon GoogleMaterialDesignIcons/DONE))
                         ;; checke or uncheck checkbox
                         (c/config! cbox :icon (i-checkbox all-selected?)))
                       (.repaint (c/to-root cbox))))))
    cbox))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;;  CHECKBOXES FOR ENTERPRISE
;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; STATE INTERACTIVE TEST
;; (let [sw (:subcontracts-m @state)]
;;   (swap! sw #(assoc-in % [:1 :3 :207 :service_contract_month.was_payed] false)))
(defn- loop-all-contracts
  "Description:
     Base fn for creating another fn"
  [state! enterprise-path func]
  (let [unpayed? (some false? (flatten (doall
                                        (map
                                         (fn [[contract-id contract]] (func contract-id contract))
                                         (get-in @(:contracts-m (state!)) enterprise-path)))))]
    (if (nil? unpayed?) true (not unpayed?))))

(defn- all-contracts-payed?
  [state! enterprise-path]
  (loop-all-contracts state! enterprise-path
                      (fn [contract-id contract]
                        (let [contract-path (join-vec enterprise-path [contract-id])]
                          (map
                           (fn [[sub-id sub]] (if (:service_contract_month.was_payed sub) true false))
                           (get-in @(:subcontracts-m (state!)) contract-path))))))

(defn- all-contracts-selected?
  "Description:
     Return true is all subcontracts for enterprise are selected or false if not"
  [state! enterprise-path]
  (loop-all-contracts state! enterprise-path
                      (fn [contract-id contract]
                        (check-if-contract-selected state! (join-vec enterprise-path [contract-id])))))

(defn- select-all-contracts
  [state! enterprise-path selected?]
  (doall (map
          (fn [[contract-id contract]]
            (let [contract-path (join-vec enterprise-path [contract-id])]
              (rift (select-all-subcontracts state! contract-path selected?)
                    (:selected? contract))))
          (get-in @(:contracts-m (state!)) enterprise-path))))

(defn- calculate-all-contract-price
  [state! enterprise-path]
  (apply + (flatten (doall
             (map
              (fn [[contract-id contract]]
                (calculate-contract-price state! (join-vec enterprise-path [contract-id])))
              (get-in @(:contracts-m (state!)) enterprise-path))))))

(defn- enterprise-checkbox
  [state! root render-header enterprise-path checkbox-selected?]
  (let [cbox (c/label :icon (i-checkbox checkbox-selected?)
                      :listen [:mouse-clicked
                               (fn [e] ;; SELECT OR UNSELECT ALL CONTRACTS
                                 (let [self-selected? (not (all-contracts-selected? state! enterprise-path))]
                                   (select-all-contracts state! enterprise-path self-selected?)
                                   (c/config! e :icon (i-checkbox self-selected?))))])

        watcher-id (keyword (str "enterprise-" (clojure.string/join "-" enterprise-path)))]

    ;; If in subcontract something will be unchecked or all will be checked then set contract checkbox selected or not
    ;; WATCH IF ALL SUBCONTRACTS SELECTED
    (new-watcher (:subcontracts-m (state!)) watcher-id enterprise-path
                 (fn []
                   (let [payed?         (all-contracts-payed?    state! enterprise-path)
                         all-selected?  (all-contracts-selected? state! enterprise-path)
                         contract-price (calculate-all-contract-price state! enterprise-path)]

                     (let [expand-header-box (.getParent cbox)
                           expand-before-title (first  (seesaw.util/children expand-header-box))
                           expand-title        (second (seesaw.util/children expand-header-box))
                           expand-other        (drop 2 (seesaw.util/children expand-header-box))]
                       
                       (c/config! expand-title :text (render-header contract-price))
                       (if payed?
                         ;; set done icon and refresh expand btn header
                         (c/config! cbox :icon (gs/icon GoogleMaterialDesignIcons/DONE))
                         ;; checke or uncheck checkbox
                         (c/config! cbox :icon (i-checkbox all-selected?)))
                       
                       (c/config! expand-header-box :items (gtool/join-mig-items
                                                            expand-before-title
                                                            expand-title
                                                            expand-other))
                       (.repaint (c/to-root cbox))))))    
    cbox))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;;  CONTRACTS PANELS
;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- create-subcontracts-expand-btns
  "Description:
    Button for one subcontract.
    contract-path ;; => [:1 :2]"
  [state! contract-path]
  (doall
   (map
    (fn [[subcontract-id subcontract]]
      (let [checkbox-selected? (:selected? subcontract)
            payed?             (:service_contract_month.was_payed subcontract)
            subcontract-path   (vec (concat contract-path [subcontract-id]))
           
            ;; Button for subcontarct
            root (gcomp/button-expand-child
                  (format "<html> %s &nbsp;&nbsp;<b> %s </b>%s</html>"
                          (str (clojure.string/replace (:service_contract_month.service_month_start subcontract) #"-" "/ ")
                               "  -  "
                               (last (clojure.string/split (str (:service_contract_month.service_month_end subcontract)) #"-")))
                          (if payed? ""
                              (str (:service_contract_month.money_per_month subcontract) " " (:currency (state!))))
                          (if (:debug-mode (state!)) (str " " subcontract-id) ""))

                  :onClick (fn [e]
                             (c/config! (:root (state!)) :items (gtool/join-mig-items (display-contract state! contract-path subcontract-path)))
                             (doto (:root (state!)) (.revalidate) (.repaint)))

                  :before-title (if payed?
                                  ;; subcontract was payed, so set icon done
                                  #(c/label :icon (gs/icon GoogleMaterialDesignIcons/CHECK))
                                  ;; checkbox is true or false, so render checkbox
                                  #(subcontract-checkbox state! subcontract-path checkbox-selected?))
                  :lvl 7
                  :height 25)]
        root))
    (get-in @(:subcontracts-m (state!)) contract-path))))

;;
;; CONTRACT
;;
(defn- create-contracts-expand-btns
  [state! enterprise-path]
  (doall
   (map
    (fn [[contract-id contract]]
      (let [contract-id        (keyword (str (:service_contract.id contract)))
            contract-path      (join-vec enterprise-path [contract-id])
            subcontracts-m     (get-in @(:subcontracts-m (state!)) contract-path)
            payed?             (contract-payed? state! contract-path)
            checkbox-selected? (if payed? false (check-if-contract-selected state! contract-path))

            render-fn          #(create-subcontracts-expand-btns state! contract-path)
            subcontarcts-box   (gmg/migrid :v (render-fn))
            rerender-header (fn [price]
                              (format "<html> %s &nbsp;&nbsp;<b> %s </b> %s </html>" 
                                      (str (clojure.string/replace (:service_contract.contract_start_term contract) #"-" "/ ") ;; TODO: Date format is crap
                                           "  -  "
                                           (clojure.string/replace (:service_contract.contract_end_term   contract) #"-" "/ "))
                                      (if (contract-payed? state! contract-path) ""
                                          (str price " " (:currency (state!))))
                                      (if (:debug-mode (state!)) (str " " contract-path) "")))
            
            ;; Expand button for contract
            root (gcomp/button-expand
                  (rerender-header (calculate-contract-price state! contract-path))
                  subcontarcts-box
                  :before-title
                  (if payed?
                    ;; subcontract was payed, so set icon done
                    #(c/label :icon (gs/icon GoogleMaterialDesignIcons/CHECK))
                    ;; checkbox is true or false, so render checkbox
                    #(contract-checkbox state! subcontarcts-box render-fn rerender-header contract-path checkbox-selected?))
                  :lvl 3)]
        root))
    (get-in @(:contracts-m (state!)) enterprise-path))))

;;
;; ENTERPRISE
;;
(defn- build-expand-contracts-for-enterprise
  "Description
     Recursion, which build panels (button-expand for enterprise and service contract, child-expand for service periods), using data from db like configuration map
  Example:
     (build-expand-contracts-for-enteprenuer [{enterprise-data} {contracts}] state!)"
  [state! enterprise-id-k]
  (let [enterprise-path  [enterprise-id-k]
        payed?             (all-contracts-payed? state! enterprise-path)
        checkbox-selected? (all-contracts-selected? state! enterprise-path)

        render-fn          #(create-contracts-expand-btns state! enterprise-path)
        enterprise-box   (gmg/migrid :v (render-fn))
        rerender-header (fn [price]
                          (format "<html> %s &nbsp;&nbsp;<b> %s </b> %s </html>" 
                                  (get-in @(:enterprises-m (state!)) [enterprise-id-k :enterprise.name])
                                  (if (all-contracts-payed? state! enterprise-path) ""
                                      (str price " " (:currency (state!))))
                                  ""))
        root (gcomp/button-expand
              (rerender-header (calculate-all-contract-price state! enterprise-path))
              enterprise-box
              :border (b/line-border :bottom 1 :color face/c-layout-background)
              :before-title (if payed?
                              ;; subcontract was payed, so set icon done
                              #(c/label :icon (gs/icon GoogleMaterialDesignIcons/CHECK))
                              ;; checkbox is true or false, so render checkbox
                              #(enterprise-checkbox state! enterprise-box rerender-header enterprise-path checkbox-selected?))
              :lvl 1)]
    root))
;; :v [1 1 2] =>> :v [:enterprise.id :service_contract.id :service_contract_month.id]


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;
;;; REQUIRES ;;;
;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
 
(defn- insert-contract [state! calndr-start calndr-end price-input select-box]
  (let [date-start (seesaw.core/text calndr-start)
        date-end   (seesaw.core/text calndr-end)
        price      (seesaw.core/text price-input)
        selected-enterprise (seesaw.core/selection select-box)
        id-enterprise (first (first (filter (fn [v] (= (second v) selected-enterprise)) (:enterprises-list (state!)))))]
    
    (if-let [alerts
             (not-empty
              (cond-> []
                (empty? selected-enterprise)
                (conj (fn [] (i/warning (gtool/get-lang-header :invalid-enterprise)
                                        (gtool/get-lang-alerts :field-is-empty))))

                (or (empty? price) (= (isNumber? price) false))
                (conj (fn [] (i/warning (gtool/get-lang-header :invalid-price)
                                        (gtool/get-lang-alerts :price-must-be-number))))

                (or (empty? date-start) (empty? date-end) (req/data-comparator-old-new (req/date-to-obj date-start) (req/date-to-obj date-end)))
                (conj (fn [] (i/warning (gtool/get-lang-header :invalid-date)
                                        (gtool/get-lang-alerts :invalid-date-time-interval-info))))))]
      
      (doall (map (fn [invoke-alert] (invoke-alert)) alerts))
      (let [ins-req (req/insert-all id-enterprise (req/date-to-obj date-start) (req/date-to-obj date-end) (read-string price))]
        (if ins-req
          (do
            (i/success (gtool/get-lang-header :success)
                       (gtool/get-lang-alerts :added-service-contract))
            (refresh-data-and-panel state!))
          (i/danger  (gtool/get-lang-header :error-with-sql-require)
                     ins-req))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;
;;; Panels for view ;;;
;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- add-contract-panel
  "Description
    in main-panel with all contracts add to new-contract-form panel with fields for input data"
  [state!]
  (let [{:keys [new-contract-form]}  (state!)
        label-fn      (fn [text] (c/label :text text :font (gs/getFont 16) :border (b/empty-border :bottom 2)))
        calndr-start  (calndr/get-calendar (gcomp/input-text :args [:columns 24]))
        calndr-end    (calndr/get-calendar (gcomp/input-text :args [:columns 24]))
        price-input   (gcomp/input-text :args [:columns 16])
        enterprises-names-list (map #(second %) (:enterprises-list (state!)))
        select-box    (gcomp/select-box enterprises-names-list)
        panel (gmg/migrid
               :v :f "[75:, fill]"
               (gcomp/min-scrollbox
                (gmg/migrid :> "[150, fill]" {:gap [10]}
                            [(gmg/migrid :v [(label-fn (gtool/get-lang-header :enterprise)) select-box])
                             (gmg/migrid :v [(label-fn (gtool/get-lang-header :price))        price-input])
                             (gmg/migrid :v [(label-fn (gtool/get-lang-header :date-start))   calndr-start])
                             (gmg/migrid :v [(label-fn (gtool/get-lang-header :date-end))     calndr-end])
                            
                             (gmg/migrid :v
                                         [(c/label "<html>&nbsp;")
                                          (gcomp/menu-bar
                                           {:buttons [["" (gs/icon GoogleMaterialDesignIcons/DONE)
                                                       (fn [e] (insert-contract state! calndr-start calndr-end price-input select-box))]
                                                      ["" (gs/icon GoogleMaterialDesignIcons/CLOSE)
                                                       (fn [e] (c/config! new-contract-form :items []))]]})])])))]
    (doto (:root (state!)) (.revalidate) (.repaint))
    panel))

(defn- create-period-view
  "Description
    Set to root all contracts expand btns list and subcontracts inside expand"
  [state! dispatch!]
  (let [new-contract-form (gmg/migrid :v [])
        btns-menu-bar      (gcomp/menu-bar
                            {:buttons [[(gtool/get-lang-btns :add-contract)
                                        (gs/icon GoogleMaterialDesignIcons/NOTE_ADD)
                                        (fn [e] (if (> (count (seesaw.util/children new-contract-form)) 0)
                                                  (seesaw.core/config! new-contract-form :items [])
                                                  (seesaw.core/config! new-contract-form :items [[(add-contract-panel state!)]])))]

                                       [(gtool/get-lang-btns :pay)
                                        (gs/icon GoogleMaterialDesignIcons/MONETIZATION_ON)
                                        (fn [e] (subcontract-payment-done state!))]
                                       
                                       [(gtool/get-lang-btns :refresh)
                                        (gs/icon GoogleMaterialDesignIcons/SYNC)
                                        (fn [e] (refresh-data-and-panel state!))]]})
        
        expand-btns-box  (gmg/migrid :v [])]

    (let [sup-map {:btns-menu-bar      btns-menu-bar
                   :expand-btns-box    expand-btns-box
                   :new-contract-form  new-contract-form}]
      ;; suplicant state
      (swap! (state! :atom) merge sup-map))

    ;; download new data from DB and rerender root panel for contracts
    (refresh-data-and-panel state!)
    (c/config! (:root (state!))
               :items (gtool/join-mig-items btns-menu-bar new-contract-form (gcomp/min-scrollbox
                                                                             expand-btns-box
                                                                             :hscroll-off false)))))



;; :v [1 1 2] =>> :v [:enterprise.id :service_contract.id :service_contract_month.id]
;; (some false? (vals (get-in @(:contracts-checkboxs-map @state) [:2 :2])))
;; (vec (map #(keyword (str %)) [2 2 1]))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; TESTS
;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def test-enterprises-m
 {:1
  {:enterprise.director "Ivan Ivankow",
   :enterprise.individual_tax_number "3323392190",
   :enterprise.accountant "Anastasia Wewbytska",
   :enterprise.contacts_information "306690666",
   :enterprise.vat_certificate "EKCA31232",
   :enterprise.physical_address "B1",
   :enterprise.ssreou "32432432",
   :enterprise.ownership_form "LTD",
   :enterprise.id 1,
   :enterprise.legal_address "A1",
   :enterprise.name "Biedronka"},
  :3
  {:enterprise.director "Vasyl Mayni",
   :enterprise.individual_tax_number "2131248412",
   :enterprise.accountant "Aleksand",
   :enterprise.contacts_information "306690666",
   :enterprise.vat_certificate "UKCP12394",
   :enterprise.physical_address "B2",
   :enterprise.ssreou "11134534",
   :enterprise.ownership_form "PP",
   :enterprise.id 3,
   :enterprise.legal_address "A2",
   :enterprise.name "some shop"},
  :2
  {:enterprise.director "Vasyl Mayni",
   :enterprise.individual_tax_number "2312931424",
   :enterprise.accountant "Aleksand",
   :enterprise.contacts_information "306690666",
   :enterprise.vat_certificate "EKCP12344",
   :enterprise.physical_address "B2",
   :enterprise.ssreou "23155555",
   :enterprise.ownership_form "PP",
   :enterprise.id 2,
   :enterprise.legal_address "A2",
   :enterprise.name "KFC"}})

(def test-contracts-m
  {:1
   {:21
    {:service_contract.id 21,
     :service_contract.contract_start_term
     #inst "2021-10-15T22:00:00.000-00:00",
     :service_contract.contract_end_term
     #inst "2021-12-30T23:00:00.000-00:00",
     :selected? false},
    :23
    {:service_contract.id 23,
     :service_contract.contract_start_term
     #inst "2021-10-31T23:00:00.000-00:00",
     :service_contract.contract_end_term
     #inst "2022-10-30T23:00:00.000-00:00",
     :selected? false}},
   :3
   {:22
    {:service_contract.id 22,
     :service_contract.contract_start_term
     #inst "2021-10-15T22:00:00.000-00:00",
     :service_contract.contract_end_term
     #inst "2021-10-30T22:00:00.000-00:00",
     :selected? false}},
   :2
   {:24
    {:service_contract.id 24,
     :service_contract.contract_start_term
     #inst "2021-10-31T23:00:00.000-00:00",
     :service_contract.contract_end_term
     #inst "2021-12-30T23:00:00.000-00:00",
     :selected? false},
    :25
    {:service_contract.id 25,
     :service_contract.contract_start_term
     #inst "2021-10-18T22:00:00.000-00:00",
     :service_contract.contract_end_term
     #inst "2021-11-18T23:00:00.000-00:00",
     :selected? false}}})

(def test-subcontracts-m
 {:1
  {:21
   {:1021
    {:service_contract_month.id 1021,
     :service_contract_month.service_month_start
     #inst "2021-10-15T22:00:00.000-00:00",
     :service_contract_month.service_month_end
     #inst "2021-10-30T22:00:00.000-00:00",
     :service_contract_month.money_per_month 150.0,
     :service_contract_month.was_payed true,
     :selected? false},
    :1022
    {:service_contract_month.id 1022,
     :service_contract_month.service_month_start
     #inst "2021-10-31T23:00:00.000-00:00",
     :service_contract_month.service_month_end
     #inst "2021-11-29T23:00:00.000-00:00",
     :service_contract_month.money_per_month 100.0,
     :service_contract_month.was_payed false,
     :selected? false},
    :1023
    {:service_contract_month.id 1023,
     :service_contract_month.service_month_start
     #inst "2021-11-30T23:00:00.000-00:00",
     :service_contract_month.service_month_end
     #inst "2021-12-30T23:00:00.000-00:00",
     :service_contract_month.money_per_month 300.0,
     :service_contract_month.was_payed false,
     :selected? false}},
   :23
   {:1025
    {:service_contract_month.id 1025,
     :service_contract_month.service_month_start
     #inst "2021-10-31T23:00:00.000-00:00",
     :service_contract_month.service_month_end
     #inst "2021-11-29T23:00:00.000-00:00",
     :service_contract_month.money_per_month 10.0,
     :service_contract_month.was_payed true,
     :selected? false},
    :1029
    {:service_contract_month.id 1029,
     :service_contract_month.service_month_start
     #inst "2022-02-28T23:00:00.000-00:00",
     :service_contract_month.service_month_end
     #inst "2022-03-30T22:00:00.000-00:00",
     :service_contract_month.money_per_month 10.0,
     :service_contract_month.was_payed true,
     :selected? false},
    :1031
    {:service_contract_month.id 1031,
     :service_contract_month.service_month_start
     #inst "2022-04-30T22:00:00.000-00:00",
     :service_contract_month.service_month_end
     #inst "2022-05-30T22:00:00.000-00:00",
     :service_contract_month.money_per_month 10.0,
     :service_contract_month.was_payed false,
     :selected? false},
    :1027
    {:service_contract_month.id 1027,
     :service_contract_month.service_month_start
     #inst "2020-12-31T23:00:00.000-00:00",
     :service_contract_month.service_month_end
     #inst "2022-01-30T23:00:00.000-00:00",
     :service_contract_month.money_per_month 10.0,
     :service_contract_month.was_payed false,
     :selected? false},
    :1030
    {:service_contract_month.id 1030,
     :service_contract_month.service_month_start
     #inst "2022-03-31T22:00:00.000-00:00",
     :service_contract_month.service_month_end
     #inst "2022-04-29T22:00:00.000-00:00",
     :service_contract_month.money_per_month 10.0,
     :service_contract_month.was_payed false,
     :selected? false},
    :1026
    {:service_contract_month.id 1026,
     :service_contract_month.service_month_start
     #inst "2021-11-30T23:00:00.000-00:00",
     :service_contract_month.service_month_end
     #inst "2021-12-30T23:00:00.000-00:00",
     :service_contract_month.money_per_month 10.0,
     :service_contract_month.was_payed false,
     :selected? false},
    :1036
    {:service_contract_month.id 1036,
     :service_contract_month.service_month_start
     #inst "2022-09-30T22:00:00.000-00:00",
     :service_contract_month.service_month_end
     #inst "2022-10-30T23:00:00.000-00:00",
     :service_contract_month.money_per_month 10.0,
     :service_contract_month.was_payed false,
     :selected? false},
    :1032
    {:service_contract_month.id 1032,
     :service_contract_month.service_month_start
     #inst "2022-05-31T22:00:00.000-00:00",
     :service_contract_month.service_month_end
     #inst "2022-06-29T22:00:00.000-00:00",
     :service_contract_month.money_per_month 10.0,
     :service_contract_month.was_payed false,
     :selected? false},
    :1028
    {:service_contract_month.id 1028,
     :service_contract_month.service_month_start
     #inst "2022-01-31T23:00:00.000-00:00",
     :service_contract_month.service_month_end
     #inst "2022-02-27T23:00:00.000-00:00",
     :service_contract_month.money_per_month 10.0,
     :service_contract_month.was_payed false,
     :selected? false},
    :1033
    {:service_contract_month.id 1033,
     :service_contract_month.service_month_start
     #inst "2022-06-30T22:00:00.000-00:00",
     :service_contract_month.service_month_end
     #inst "2022-07-30T22:00:00.000-00:00",
     :service_contract_month.money_per_month 10.0,
     :service_contract_month.was_payed false,
     :selected? false},
    :1035
    {:service_contract_month.id 1035,
     :service_contract_month.service_month_start
     #inst "2022-08-31T22:00:00.000-00:00",
     :service_contract_month.service_month_end
     #inst "2022-09-29T22:00:00.000-00:00",
     :service_contract_month.money_per_month 10.0,
     :service_contract_month.was_payed false,
     :selected? false},
    :1034
    {:service_contract_month.id 1034,
     :service_contract_month.service_month_start
     #inst "2022-07-31T22:00:00.000-00:00",
     :service_contract_month.service_month_end
     #inst "2022-08-30T22:00:00.000-00:00",
     :service_contract_month.money_per_month 10.0,
     :service_contract_month.was_payed false,
     :selected? false}}},
  :3
  {:22
   {:1024
    {:service_contract_month.id 1024,
     :service_contract_month.service_month_start
     #inst "2021-10-15T22:00:00.000-00:00",
     :service_contract_month.service_month_end
     #inst "2021-10-30T22:00:00.000-00:00",
     :service_contract_month.money_per_month 50.0,
     :service_contract_month.was_payed true,
     :selected? false}}},
  :2
  {:24
   {:1037
    {:service_contract_month.id 1037,
     :service_contract_month.service_month_start
     #inst "2021-10-31T23:00:00.000-00:00",
     :service_contract_month.service_month_end
     #inst "2021-11-29T23:00:00.000-00:00",
     :service_contract_month.money_per_month 10.0,
     :service_contract_month.was_payed true,
     :selected? false},
    :1038
    {:service_contract_month.id 1038,
     :service_contract_month.service_month_start
     #inst "2021-11-30T23:00:00.000-00:00",
     :service_contract_month.service_month_end
     #inst "2021-12-30T23:00:00.000-00:00",
     :service_contract_month.money_per_month 10.0,
     :service_contract_month.was_payed true,
     :selected? false}},
   :25
   {:1039
    {:service_contract_month.id 1039,
     :service_contract_month.service_month_start
     #inst "2021-10-18T22:00:00.000-00:00",
     :service_contract_month.service_month_end
     #inst "2021-10-30T22:00:00.000-00:00",
     :service_contract_month.money_per_month 20.0,
     :service_contract_month.was_payed false,
     :selected? false},
    :1040
    {:service_contract_month.id 1040,
     :service_contract_month.service_month_start
     #inst "2021-10-31T23:00:00.000-00:00",
     :service_contract_month.service_month_end
     #inst "2021-11-18T23:00:00.000-00:00",
     :service_contract_month.money_per_month 20.0,
     :service_contract_month.was_payed false,
     :selected? false}}}})

(def ok-subcontracts-all-selected-select
  {:1021
   {:service_contract_month.id 1021,
    :service_contract_month.service_month_start
    #inst "2021-10-15T22:00:00.000-00:00",
    :service_contract_month.service_month_end
    #inst "2021-10-30T22:00:00.000-00:00",
    :service_contract_month.money_per_month 150.0,
    :service_contract_month.was_payed true,
    :selected? true},
   :1022
   {:service_contract_month.id 1022,
    :service_contract_month.service_month_start
    #inst "2021-10-31T23:00:00.000-00:00",
    :service_contract_month.service_month_end
    #inst "2021-11-29T23:00:00.000-00:00",
    :service_contract_month.money_per_month 100.0,
    :service_contract_month.was_payed false,
    :selected? true},
   :1023
   {:service_contract_month.id 1023,
    :service_contract_month.service_month_start
    #inst "2021-11-30T23:00:00.000-00:00",
    :service_contract_month.service_month_end
    #inst "2021-12-30T23:00:00.000-00:00",
    :service_contract_month.money_per_month 300.0,
    :service_contract_month.was_payed false,
    :selected? true}})

(def ok-subcontracts-all-selected-unselect
  {:1021
   {:service_contract_month.id 1021,
    :service_contract_month.service_month_start
    #inst "2021-10-15T22:00:00.000-00:00",
    :service_contract_month.service_month_end
    #inst "2021-10-30T22:00:00.000-00:00",
    :service_contract_month.money_per_month 150.0,
    :service_contract_month.was_payed true,
    :selected? false},
   :1022
   {:service_contract_month.id 1022,
    :service_contract_month.service_month_start
    #inst "2021-10-31T23:00:00.000-00:00",
    :service_contract_month.service_month_end
    #inst "2021-11-29T23:00:00.000-00:00",
    :service_contract_month.money_per_month 100.0,
    :service_contract_month.was_payed false,
    :selected? false},
   :1023
   {:service_contract_month.id 1023,
    :service_contract_month.service_month_start
    #inst "2021-11-30T23:00:00.000-00:00",
    :service_contract_month.service_month_end
    #inst "2021-12-30T23:00:00.000-00:00",
    :service_contract_month.money_per_month 300.0,
    :service_contract_month.was_payed false,
    :selected? false}})

(def ok-subcontract-selected
  {:service_contract_month.id 1022,
   :service_contract_month.service_month_start
   #inst "2021-10-31T23:00:00.000-00:00",
   :service_contract_month.service_month_end
   #inst "2021-11-29T23:00:00.000-00:00",
   :service_contract_month.money_per_month 100.0,
   :service_contract_month.was_payed false,
   :selected? true})

(defn- initial-test-state-template [test-state]
  (reset!
   test-state
   {:enterprises-m        (atom {})
    :contracts-m          (atom {})
    :subcontracts-m       (atom {})
    :currency             "UAH"
    :subcontracts-payment-state (atom {})}))

(defn- initialize-test-state [state!]
  (reset! (:enterprises-m  (state!)) test-enterprises-m)
  (reset! (:contracts-m    (state!)) test-contracts-m)
  (reset! (:subcontracts-m (state!)) test-subcontracts-m))

(defn run-tests [& {:keys [full] :or {full false}}]
  (println "\nInitialize Tests:")
  (let [test-state  (atom {})
        test-state! #(deref test-state)]
    (initial-test-state-template test-state)
    (initialize-test-state test-state!)
    (let [testing
          (-> []
              (concat [[:calculate-contract-price<nopay>     (= 400.0 (calculate-contract-price test-state! [:1 :21]))]])
              (concat [[:calculate-contract-price<payed>     (= 0     (calculate-contract-price test-state! [:3 :22]))]])
              
              (concat [[:calculate-all-contract-price<nopay> (= 40.0  (calculate-all-contract-price test-state! [:2]))]])
              (concat [[:calculate-all-contract-price<payed> (= 0     (calculate-all-contract-price test-state! [:3]))]])
              
              (concat [[:select-all-subcontracts<select>     (= (select-all-subcontracts test-state! [:1 :21] true)
                                                                ok-subcontracts-all-selected-select)]])
              (concat [[:select-all-subcontracts<unselect>   (= (select-all-subcontracts test-state! [:1 :21] false)
                                                                ok-subcontracts-all-selected-unselect)]])

              (concat [[:calculate-all-contract-price<500> (= 500.0 (calculate-all-contract-price test-state! [:1]))]])

              (concat [[:check-if-contract-selected<selected>
                        (= true (check-if-contract-selected test-state! [:3 :2]))]])
              (concat [[:check-if-contract-selected<notselected>
                        (= false (check-if-contract-selected test-state! [:1 :21]))]])
              
              (concat [[:select-checkbox-for-subcontract<doselect>
                        (do (select-checkbox-for-subcontract test-state! [:1 :21 :1022] true)
                            (= ok-subcontract-selected (get-in @(:subcontracts-m (test-state!)) [:1 :21 :1022])))]])
              (concat [[:select-checkbox-for-subcontract<unselect>
                        (do (select-checkbox-for-subcontract test-state! [:1 :21 :1022] false)
                            (= false (get-in @(:subcontracts-m (test-state!)) [:1 :21 :1022 :selected?])))]])
              
              (concat [[:listing-all-selected-checkboxes-path
                        (do (select-checkbox-for-subcontract test-state! [:1 :21 :1022] true)
                            (= [:1 :21 :1022] (first (listing-all-selected-checkboxes-path test-state!))))]])

              (concat [[:pay-for-pointed-subcontracts
                        (do (pay-for-pointed-subcontracts test-state! [:1 :21 :1022])
                            (get-in @(:subcontracts-m (test-state!)) [:1 :21 :1022 :service_contract_month.was_payed]))]])

              (concat [[:pay-for-selected-subcontracts
                        (do (pay-for-selected-subcontracts test-state! [[:1 :21 :1023] [:1 :23 :1031]])
                            (and (get-in @(:subcontracts-m (test-state!)) [:1 :21 :1023 :service_contract_month.was_payed])
                                 (get-in @(:subcontracts-m (test-state!)) [:1 :23 :1031 :service_contract_month.was_payed])))]])
              
              (concat [[:contract-payed?<true>  (= true  (contract-payed? test-state! [:1 :21]))]])
              (concat [[:contract-payed?<false> (= false (contract-payed? test-state! [:2 :25]))]])

              (concat [[:all-contracts-payed?<true>  (= true  (all-contracts-payed? test-state! [:3]))]])
              (concat [[:all-contracts-payed?<false> (= false (all-contracts-payed? test-state! [:1]))]])

              (concat [[:select-all-contracts_&_:all-contracts-selected?<selected>
                        (do (select-all-contracts test-state! [:2] true)
                            (= true (all-contracts-selected? test-state! [:2])))]])
              (concat [[:select-all-contracts_&_:all-contracts-selected?<unselected>
                        (do (select-all-contracts test-state! [:2] false)
                            (= true (all-contracts-selected? test-state! [:2])))]])
              )
          lite-testing(map #(do (println "TEST: " (if (second %) "OK" "FAILD") " - " (first %)) (second %)) testing)]
      (if full testing lite-testing))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Toolkit with SQl funcs ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn service-period-toolkit-pipeline
  "Description
    Extending plugin-toolkit map with all requires to DB"
  [configuration]
  {:download-data-map               req/info-grouped-query

   :update-service-money            (fn [id money] (req/update-service-money-per-month id money))

   :update-service-month            (fn [id-list] (req/update-service-month-to-payed id-list))})
;;;;;;;;;;;;;
;;; Entry ;;;
;;;;;;;;;;;;;

(defn service-period-entry [plugin-path global-configuration-getter]
  (if (some false? (run-tests))
    (gmg/migrid :v :center :center (c/label :text (gtool/get-lang-alerts :tests-failed)))
    (let [root   (gmg/migrid :v {:args [:background face/c-layout-background
                                        ;; :border (b/line-border :thickness 1 :color "#fff")
                                        ]}
                             (c/label :text "Loading..."))
          state! (fn [& prop]
                   (cond (= :atom (first prop)) state
                         :else (deref state)))
          state  (create-state-template plugin-path global-configuration-getter root state!)
          dispatch! (create-disptcher state)]
      (create-period-view state! dispatch!)
      root)))

;;;;;;;;;;;;
;;; BIND ;;;
;;;;;;;;;;;;

(register-custom-view-plugin
 :name 'service-period
 :description "Plugin for service contracts of enterprise"
 :entry service-period-entry
 :toolkit service-period-toolkit-pipeline
 :spec-list [])
