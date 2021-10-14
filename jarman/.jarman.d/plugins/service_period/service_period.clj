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
          {:debug-mode           true
           :plugin-path          plugin-path
           :plugin-global-config global-configuration-getter
           :plugin-config        (get-in (global-configuration-getter) (conj plugin-path :config) {})
           :plugin-toolkit       (get-in (global-configuration-getter) (conj plugin-path :toolkit) {})
           :entrepreneurs-m      (atom {})
           :contracts-m          (atom {})
           :subcontracts-m       (atom {})
           :currency             "UAH"
           :root                 root
           :state!               state!})
  state)

;;;;;;;;;;;;;;;;;;;;;;;;
;;; HELPER FUNCTIONS ;;;  
;;;;;;;;;;;;;;;;;;;;;;;;
(defn- format-date [date]
  (let [fn-format (fn [date] (.format (java.text.SimpleDateFormat. "dd-MM-YYYY") date))]
    (if (or (vector? date)(seq? date))
      (map (fn [d] (fn-format d)) date)
      (fn-format date))))

(defn- split-date [date-vec]
  (apply str (interpose " / " (format-date date-vec))))

;; (defn- isNumber? [val]
;;    (if-not (number? (read-string val)) 
;;      (i/danger (gtool/get-lang-header :error) (gtool/get-lang-alerts :price-must-be-number))
;;      true))
(defn- isNumber? [val-s]
  (try
    (do
      (float (read-string val-s)))
    (catch Exception e false)))

(declare build-expand-contracts-for-entrepreneur)

(defn- refresh-data-and-panel [state!]
  (let [{:keys [root expand-btns-box entrepreneurs-m contracts-m subcontracts-m]} (state!)
        fresh-data ((:download-data-map (:plugin-toolkit (state!))))]

    (reset! entrepreneurs-m (:entrepreneurs-m fresh-data))
    (reset! contracts-m     (:contracts-m     fresh-data))
    (reset! subcontracts-m  (:subcontracts-m  fresh-data))
    
    (c/config!
     expand-btns-box
     :items (gtool/join-mig-items
             (doall
              (map
               (fn [[entrepreneur-id]]
                 (println entrepreneur-id)
                 (build-expand-contracts-for-entrepreneur state! entrepreneur-id))
               @entrepreneurs-m))))
    (doto root (.revalidate) (.repaint))))
  
(defn- back-to-main-panel
  "Description
    remove in root panel with periods of one contract, add to root panel with all contracta (like agenda) "
  [state!]
  (let [{:keys [root new-contract-form expand-btns-box btns-menu-bar]} (state!)]
    (swap! (state! :atom) merge {:payments-per-subcontract {}})
    (refresh-data-and-panel state!)
    (c/config! (:root (state!)) :items (gtool/join-mig-items btns-menu-bar new-contract-form (gcomp/min-scrollbox expand-btns-box)))
    ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; GUI for one Contract;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn- update-subcontract-price
  "Description
    require to db for update money for one period"
  [state!]
  (let [update-fn (:update-service-money (:plugin-toolkit (state!)))]
    (doall (map (fn [[k v]] (update-fn k v)) (:payments-per-subcontract (state!))))
    (i/info (gtool/get-lang-header :info)
            (gtool/get-lang-alerts :updated-data))))

;; (defn- panel-subcontract
;;   "Description
;;     return mig-panel for one subcontract"
;;   [state! subcontract-path]
;;   (let [{id              :service_contract_month.id
;;          start-date      :service_contract_month.service_month_start 
;;          end-date        :service_contract_month.service_month_end
;;          payment-st      :service_contract_month.was_payed
;;          payment         :service_contract_month.money_per_month}  subcontract

;;         label-fn (fn [text foreground]
;;                    (seesaw.core/label :text (str text)
;;                                       :font (gtool/getFont 16)
;;                                       :foreground foreground
;;                                       :halign :center))
        
;;         caret-fn (fn [e] (let [new-val (isNumber? (c/config e :text))]
;;                            (if-not (= false new-val) 
;;                              (if-not (= new-val payment) 
;;                                (do
;;                                  (swap! (state! :atom) #(assoc-in % [:payments-per-subcontract id] (str new-val)))
;;                                  (c/config! e :listen [:focus-lost (fn [e] (c/config! e :text (get-in (state!) [:payments-per-subcontract id])))])))
;;                              )))
        
;;         date     (str (clojure.string/replace start-date #"-" "/ ") "  -  " (clojure.string/replace end-date #"-" "/ "))

;;         panel (gmg/migrid ;; subcontract row
;;                :> :f
;;                {:gap [5 5 10 20]
;;                 :args [:background face/c-compos-background
;;                        :border (b/line-border :bottom 3 :color face/c-layout-background)
;;                        ;; hover row
;;                        :listen [:mouse-entered (fn [e] (c/config! e :background face/c-on-focus-dark :cursor :hand))
;;                                 :mouse-exited  (fn [e] (c/config! e :background face/c-compos-background))]]}
;;                [])
        
;;         render-fn (fn rerender [is-payed?]
;;                     [(gmg/migrid ;; icon and value to pay 
;;                       :> "[fill]5px[70:, fill]" :g {:gap [0 5]}
;;                       [(if is-payed?
;;                          ;; icon done
;;                          [(c/label :icon (gs/icon GoogleMaterialDesignIcons/CHECK)
;;                                    :tip (gtool/get-lang-tip :payment-is-done)
;;                                    :listen [:mouse-entered (fn [e] (c/config! e :cursor :wait))])]
;;                          ;; icon help
;;                          [(c/label :icon (gs/icon GoogleMaterialDesignIcons/HELP)
;;                                    :tip (gtool/get-lang-tip :you-can-edit-this-position)
;;                                    :listen [:mouse-entered (fn [e] (c/config! e :cursor :wait))])])

;;                        ;; Price
;;                        (if is-payed? 
;;                          ;; price in label
;;                          (label-fn payment face/c-foreground)
;;                          ;; price in input
;;                          (gcomp/input-text
;;                           :underline-off true
;;                           :halign :center
;;                           :args [:text payment :listen [:caret-update caret-fn]]))])

;;                      ;; date
;;                      (label-fn date face/c-foreground)
                     
;;                      ;; status
;;                      (label-fn (if is-payed?
;;                                  (gtool/get-lang-infos :payment-done-lock-row)
;;                                  (gtool/get-lang-infos :wait-for-pay))
;;                                (if is-payed? face/c-green face/c-orange))
                     
;;                      ;; pay for subcontract button
;;                      (if is-payed? [] [(gcomp/button-slim (gtool/get-lang-btns :pay)
;;                                                            :args [:icon (gs/icon GoogleMaterialDesignIcons/MONETIZATION_ON)]
;;                                                            :onClick (fn [e]
;;                                                                       (let [subcontract-pathf (v-to-vmp (:v subcontract))
;;                                                                             checkboxes-state (:contracts-checkboxs-map (state!))]
;;                                                                         (swap! checkboxes-state #(assoc-in % subcontract-path true))
;;                                                                         (update-contracts-payment state!)
;;                                                                         (c/config! panel :items (gtool/join-mig-items (rerender true)))
;;                                                                         (doto panel (.revalidate) (.repaint)))))
                                        
;;                                         (gcomp/button-slim (gtool/get-lang-btns :remove)
;;                                                            :args [:icon (gs/icon GoogleMaterialDesignIcons/REMOVE_CIRCLE)])])])]
;;     (c/config! panel :items (gtool/join-mig-items (render-fn payment-st)))
;;     panel))

;; (defn- panel-one-contract
;;   "Description
;;     return mig-panel with button-bar and panels with all periods of one contract"
;;   [state! subcontract-path]
;;   (let [btns-menu-bar (gcomp/menu-bar
;;                        {:buttons [[(gtool/get-lang-btns :back)
;;                                    (gs/icon GoogleMaterialDesignIcons/ARROW_BACK)
;;                                    (fn [e] (back-to-main-panel state!))]
;;                                   [(gtool/get-lang-btns :save)
;;                                    (gs/icon GoogleMaterialDesignIcons/SAVE)
;;                                    (fn [e] (update-subcontract-price state!))]
;;                                   [(gtool/get-lang-btns :export-odt)
;;                                    icon/odt-64-png
;;                                    (fn [e] )]]})
        
;;         subcontracts (gmg/migrid :v
;;                       (doall (map
;;                               (fn [[subcontract-id subcontract]]
;;                                 (panel-subcontract state! subcontract))
;;                               @(:subcontracts-m (state!)))))]
;;     (list btns-menu-bar (gcomp/min-scrollbox subcontracts))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;
;;; EXPAND PANELS ;;;
;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- v-to-vmp
  "Description:
    vector to vector map path
   Example
    [1 2 3] ;; => [:1 :2 :3]"
  [v] (vec (map #(keyword (str %)) v)))

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

(defn- subcontract-checkbox ;; OK
  [state! subcontract-path checkbox-selected?]
  (c/label :icon (i-checkbox checkbox-selected?)
           :listen [:mouse-clicked
                    (fn [e] (let [selected-path  (join-vec subcontract-path [:selected?])
                                  self-selected? (if (= true (get-in @(:subcontracts-m (state!)) selected-path)) false true)]
                              (c/config! e :icon (i-checkbox self-selected?))
                              (.repaint (c/to-widget e))
                              
                              ;; select or unselect subcontract. Path return true or false
                              (swap! (:subcontracts-m (state!)) #(assoc-in % selected-path self-selected?))))]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;;  CHECKBOXES FOR CONTRACTS
;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- check-if-contract-payed ;; OK
  [state! contract-path]
  (let [unpayed? (some false?
                     (flatten
                      (doall (map
                              (fn [[subcontract-id subcontract]]
                                (:service_contract_month.was_payed subcontract))
                              (get-in @(:subcontracts-m (state!)) contract-path)))))]
    (if (nil? unpayed?) true (not unpayed?))))

(defn- check-if-contract-selected ;; OK
  [state! contract-path]
  (let [selected? (some false?
                        (flatten
                         (doall (map
                                 (fn [[subcontract-id subcontract]]
                                   (if (:service_contract_month.was_payed subcontract)
                                     nil
                                     (:selected? subcontract)))
                                 (get-in @(:subcontracts-m (state!)) contract-path)))))]
    (if (nil? selected?) true (not selected?))))

(defn- contract-selected? ;; OK
  [state! contract-path]
  (let [contract-path-to-selected (join-vec contract-path [:selected?])]
    (get-in @(:contracts-m (state!)) contract-path-to-selected)))

(defn- calculate-contract-price [state! contract-path]
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

;;(:contracts-m @state)
(defn- select-and-calc-contract-price
  [state! contract-path selected?]
  (let [contract-path-to-selected (join-vec contract-path [:selected?])
        contract-path-to-price    (join-vec contract-path [:price])
        price (calculate-contract-price state! contract-path)]
    (swap! (:contracts-m (state!))
           #(-> %
                (assoc-in contract-path-to-selected selected?)
                (assoc-in contract-path-to-price price)))
    price))

(defn- select-all-subcontracts
  [state! contract-path selected?]
  (let [subcontracts-m     (get-in @(:subcontracts-m (state!)) contract-path)
        new-subcontracts-m (into {} (map
                                     (fn [[subcontract-id subcontract]]
                                       {subcontract-id (assoc subcontract :selected? selected?)})
                                     subcontracts-m))]
    (swap! (:subcontracts-m (state!)) #(assoc-in % contract-path new-subcontracts-m))
    new-subcontracts-m))

(defn- contract-checkbox ;; OK
  [state! root render-fn render-header contract-path checkbox-selected?]
  (let [cbox (c/label :icon (i-checkbox checkbox-selected?)
                      :listen [:mouse-clicked
                               (fn [e] ;; SELECT OR UNSELECT ALL SUBCONTRACTS
                                 (let [self-selected? (if (= true (contract-selected? state! contract-path)) false true)]
                                   ;; select or unselect all subcontracts
                                   (select-all-subcontracts state! contract-path self-selected?)
                                   (select-and-calc-contract-price state! contract-path self-selected?)                                   
                                   (c/config! e :icon (i-checkbox self-selected?))))])
        
        watcher-id (keyword (str "contract-" (clojure.string/join "-" contract-path)))]

    ;; If in subcontract something will be unchecked or all will be checked then set contract checkbox selected or not
    ;; WATCH IF ALL SUBCONTRACTS SELECTED
    (new-watcher (:subcontracts-m (state!)) watcher-id contract-path
                 (fn []
                   (let [payed?        (check-if-contract-payed state! contract-path)
                         all-selected? (check-if-contract-selected state! contract-path)]
                     
                     (c/config! root :items (gtool/join-mig-items (render-fn)))
                     (let [expand-header-box (.getParent cbox)
                           expand-before-title (first  (seesaw.util/children expand-header-box))
                           expand-title        (second (seesaw.util/children expand-header-box))
                           expand-other        (drop 2 (seesaw.util/children expand-header-box))]
                       
                       (c/config! expand-title :text (render-header))
                       (if payed?
                         ;; set done icon and refresh expand btn header
                         (c/config! cbox :icon (gs/icon GoogleMaterialDesignIcons/DONE))
                         ;; checke or uncheck checkbox
                         (do
                           (select-and-calc-contract-price state! contract-path all-selected?)
                           (c/config! cbox :icon (i-checkbox all-selected?))))
                       
                       (c/config! expand-header-box :items (gtool/join-mig-items
                                                            expand-before-title
                                                            expand-title
                                                            expand-other))
                       (.repaint (c/to-root cbox))))))    
    cbox))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;;  CHECKBOXES FOR ENTREPRENEUR
;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; STATE INTERACTIVE TEST
;; (let [sw (:subcontracts-m @state)]
;;   (swap! sw #(assoc-in % [:1 :3 :207 :service_contract_month.was_payed] false)))
(defn- all-contracts-payed?
  [state! entrepreneur-path]
  (let [unpayed? (some false? (flatten
                                  (doall (map
                                          (fn [[contract-id contract]]
                                            (let [contract-path (join-vec entrepreneur-path [contract-id])]
                                              (check-if-contract-payed state! contract-path)))
                                          (get-in @(:contracts-m (state!)) entrepreneur-path)))))]
    (if (nil? unpayed?) true (not unpayed?))))

(defn- all-contracts-selected?
  "Description:
     Return true is all subcontracts for entrepreneur are selected or false if not"
  [state! entrepreneur-path]
  (let [unselected? (some false? (flatten
                                  (doall (map
                                          (fn [[contract-id contract]]
                                            (let [contract-path (join-vec entrepreneur-path [contract-id])]
                                              (rift (check-if-contract-payed state! contract-path)
                                                    (:selected? contract))))
                                          (get-in @(:contracts-m (state!)) entrepreneur-path)))))]
    (if (nil? unselected?) true (not unselected?))))

(defn- select-all-contracts
  [state! entrepreneur-path selected?]
  (doall (map
          (fn [[contract-id contract]]
            (let [contract-path (join-vec entrepreneur-path [contract-id])]
              (rift (select-all-subcontracts state! contract-path selected?)
                    (:selected? contract))))
          (get-in @(:contracts-m (state!)) entrepreneur-path))))

(defn- select-and-calc-all-contracts-price
  [state! entrepreneur-path selected?]
  (apply + (flatten
            (doall (map
                    (fn [[contract-id contract]]
                      (let [contract-path (join-vec entrepreneur-path [contract-id])]
                        (rift (select-and-calc-contract-price state! contract-path selected?) 0)))
                    (get-in @(:contracts-m (state!)) entrepreneur-path))))))

(defn- entrepreneur-checkbox
  [state! root render-header entrepreneur-path checkbox-selected?]
  (let [cbox (c/label :icon (i-checkbox checkbox-selected?)
                      :listen [:mouse-clicked
                               (fn [e] ;; SELECT OR UNSELECT ALL CONTRACTS
                                 (let [self-selected? true] ;; TODO: need to store selected
                                   (select-all-contracts state! entrepreneur-path self-selected?)
                                   (select-and-calc-all-contracts-price state! entrepreneur-path self-selected?)
                                   (c/config! e :icon (i-checkbox self-selected?))))])

        watcher-id (keyword (str "entrepreneur-" (clojure.string/join "-" entrepreneur-path)))]

    ;; If in subcontract something will be unchecked or all will be checked then set contract checkbox selected or not
    ;; WATCH IF ALL SUBCONTRACTS SELECTED
    (new-watcher (:contracts-m (state!)) watcher-id entrepreneur-path
                 (fn []
                   (let [payed?        (all-contracts-payed?    state! entrepreneur-path)
                         all-selected? (all-contracts-selected? state! entrepreneur-path)]
                     
                     (let [expand-header-box (.getParent cbox)
                           expand-before-title (first  (seesaw.util/children expand-header-box))
                           expand-title        (second (seesaw.util/children expand-header-box))
                           expand-other        (drop 2 (seesaw.util/children expand-header-box))]
                       
                       ;;(c/config! expand-title :text (render-header))
                       (if payed?
                         ;; set done icon and refresh expand btn header
                         (c/config! cbox :icon (gs/icon GoogleMaterialDesignIcons/DONE))
                         ;; checke or uncheck checkbox
                         (do
                           (select-and-calc-all-contracts-price state! entrepreneur-path all-selected?)
                           (c/config! cbox :icon (i-checkbox all-selected?))))
                       
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
                               (clojure.string/replace (:service_contract_month.service_month_start subcontract) #"-" "/ "))
                          (if payed? ""
                              (str (:service_contract_month.money_per_month subcontract) " " (:currency (state!))))
                          (if (:debug-mode (state!)) (str " " subcontract-id) ""))

                  :onClick (fn [e]
                             ;; (c/config! (:root (state!)) :items (gtool/join-mig-items (panel-one-contract state! subcontract-path)))
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


;;@(:subcontracts-m @state)

;;
;; CONTRACT
;;
(defn- create-contracts-expand-btns
  [state! entrepreneur-path]
  (doall
   (map
    (fn [[contract-id contract]]
      (let [contract-id        (keyword (str (:service_contract.id contract)))
            
            contract-path      (join-vec entrepreneur-path [contract-id])
            
            subcontracts-m     (get-in @(:subcontracts-m (state!)) contract-path)
            
            payed?             (check-if-contract-payed state! contract-path)
            
            checkbox-selected? (if payed? false (check-if-contract-selected state! contract-path))

            render-fn          #(create-subcontracts-expand-btns state! contract-path)

            subcontarcts-box   (gmg/migrid :v (render-fn))
            
            rerender-header #(format "<html> %s &nbsp;&nbsp;<b> %s </b> %s </html>" 
                                     (str (clojure.string/replace (:service_contract.contract_start_term contract) #"-" "/ ")
                                          "  -  "
                                          (clojure.string/replace (:service_contract.contract_end_term   contract) #"-" "/ "))
                                     (if (check-if-contract-payed state! contract-path) ""
                                         (str (select-and-calc-contract-price state! contract-path checkbox-selected?) " " (:currency (state!))))
                                     (if (:debug-mode (state!)) (str " " contract-path) ""))
            
            ;; Expand button for contract
            root (gcomp/button-expand
                  (rerender-header)
                  subcontarcts-box
                  :before-title
                  (if payed?
                    ;; subcontract was payed, so set icon done
                    #(c/label :icon (gs/icon GoogleMaterialDesignIcons/CHECK))
                    ;; checkbox is true or false, so render checkbox
                    #(contract-checkbox state! subcontarcts-box render-fn rerender-header contract-path checkbox-selected?)
                    ;;#(c/checkbox)
                    )
                  :lvl 3)]
        root))
    (get-in @(:contracts-m (state!)) entrepreneur-path))))


;;
;; ENTREPRENEUR
;;
(defn- build-expand-contracts-for-entrepreneur
  "Description
     Recursion, which build panels (button-expand for entrepreneur and service contract, child-expand for service periods), using data from db like configuration map
  Example:
     (build-expand-contracts-for-enteprenuer [{entrepreneur-data} {contracts}] state!)"
  [state! entrepreneur-id-k]
  (let [entrepreneur-path [entrepreneur-id-k]
        render-fn         #(create-contracts-expand-btns state! entrepreneur-path)
        entrepreneur-box   (gmg/migrid :v (render-fn))
        
        payed?             (all-contracts-payed? state! entrepreneur-path)
        checkbox-selected? (all-contracts-selected? state! entrepreneur-path)

        root (gcomp/button-expand
              (str (get-in @(:entrepreneurs-m (state!)) [:1 :enterpreneur.name])  
                   ;; (if (nil? checkbox-selected?) ""
                   ;;     (str " / "  (calc-payment-for-enterpreneuer entrepreneur-data contracts-m) " " (:currency (state!))))
                   )
              entrepreneur-box
              :before-title (if payed?
                              ;; subcontract was payed, so set icon done
                              #(c/label :icon (gs/icon GoogleMaterialDesignIcons/CHECK))
                              ;; checkbox is true or false, so render checkbox
                              #(entrepreneur-checkbox state! entrepreneur-box render-fn entrepreneur-path checkbox-selected?)
                              ;;#(c/checkbox)
                              )
              :lvl 1)]
    root))

;; :v [1 1 2] =>> :v [:enterpreneur.id :service_contract.id :service_contract_month.id]

;;;;;;;;;;;;;;;; 
;;; REQUIRES ;;; 
;;;;;;;;;;;;;;;;
(defn- insert-contract [state! calndr-start calndr-end price-input select-box]
  (let [date1         (seesaw.core/text calndr-start)
        date2         (seesaw.core/text calndr-end)
        price         (seesaw.core/text price-input)
        entpr         (seesaw.core/selection select-box)
        enterpreneurs (:enterpreneurs-list (state!))
        date-to-obj   (fn [data-string] (.parse (java.text.SimpleDateFormat. "yyyy-MM-dd") data-string)) ;; TODO: date is converting to one day before
        id-entr       (:id (first (filter (fn [x] (= (:name x) entpr)) enterpreneurs)))]
    (if-let [alerts
             (not-empty
              (cond-> []
                (empty? entpr)
                (conj (fn [] (i/warning (gtool/get-lang-header :invalid-enterpreneur)
                                        (gtool/get-lang-alerts :field-is-empty))))

                (or (empty? price) (= (isNumber? price) false))
                (conj (fn [] (i/warning (gtool/get-lang-header :invalid-price)
                                        (gtool/get-lang-alerts :price-must-be-number))))

                (or (empty? date1) (empty? date2) (req/data-comparator-old-new (date-to-obj date1) (date-to-obj date2)))
                (conj (fn [] (i/warning (gtool/get-lang-header :invalid-date)
                                        (gtool/get-lang-alerts :invalid-date-time-interval-info))))))]
      (doall (map (fn [invoke-alert] (invoke-alert)) alerts))
      (do
       (println "\nNew: " id-entr date1 date2 price)
       (println "New: " id-entr (date-to-obj date1) (date-to-obj date2) (read-string price))
       (let [ins-req (req/insert-all id-entr (date-to-obj date1) (date-to-obj date2) (read-string price))]
         (if ins-req
           (i/success (gtool/get-lang-header :success)
                      (gtool/get-lang-alerts :added-service-contract))
           (i/danger  (gtool/get-lang-header :error-with-sql-require)
                      ins-req)))))))

(defn- cut-selected-subcontracts
  "Description:
     Cut selected checkboxes. Usefull in update-contracts-payment function."
  [state!]
  (doall
   ;; return new map without payed contracts and subcontracts
   (into {}(map
            (fn [[entrepreneur-id contract]]
              ;; return unselected contracts
              (let [is-contract? (rift
                             (into {}(map
                                      (fn [[contract-id subcontracts]]
                                        ;; return unselected subcontracts
                                        (let [is-subcontract? (rift (into {} (map
                                                                              (fn [[subcontracts-id selected?]]
                                                                                ;; check if selected
                                                                                (if selected? {} {subcontracts-id selected?}))
                                                                              subcontracts)) nil)]
                                          (if (nil? is-subcontract?) {} {contract-id is-subcontract?})))
                                      contract))
                             nil)]
                  (if (nil? is-contract?) {} {entrepreneur-id is-contract?})))
            @(:contracts-checkboxs-map (state!))))))

(defn- return-id-list-of-selected-checkboxes
  "Description:
     Return all selected subcontracts id in list. Usefull in update-contracts-payment function."
  [state!]
  (doall
   (filter #(not (nil? %))
           (flatten
            (map
             (fn [[entrepreneur-id contract]]
               (map
                (fn [[contract-id subcontracts]]
                  (map
                   (fn [[subcontracts-id selected?]]
                     (if selected? (Integer/parseInt (name subcontracts-id))))
                   subcontracts))
                contract))
             @(:contracts-checkboxs-map (state!)))))))

(defn- update-contracts-payment
  ([state!]
   (let [{:keys [update-service-month]} (:plugin-toolkit (state!))
         selected-ids (return-id-list-of-selected-checkboxes state!)]
     (if (empty? selected-ids)
       (i/warning (gtool/get-lang-header :warning)
                  (gtool/get-lang-alerts :select-checkbox-for-payment))

       (try
         (do
           ;; Set payed in DB
           (update-service-month selected-ids)
           ;; Update state
           (reset! (:contracts-checkboxs-map (state!)) (cut-selected-subcontracts state!))
           (i/success (gtool/get-lang-header :success)
                      (gtool/get-lang-alerts :payments-finished-successfully)))
         (catch Exception e (i/danger (gtool/get-lang-header :failed) (str "DB or State problem.\n" (.getMessage e)))))))))

;;;;;;;;;;;;;;;;;;;;;;;
;;; Panels for view ;;;
;;;;;;;;;;;;;;;;;;;;;;;
(defn- add-contract-panel
  "Description
    in main-panel with all contracts add to new-contract-form panel with fields for input data"
  [state!]
  (let [{:keys [new-contract-form]}  (state!)
        label-fn      (fn [text] (c/label :text text :font (gtool/getFont 16) :border (b/empty-border :bottom 2)))
        calndr-start  (calndr/get-calendar (gcomp/input-text :args [:columns 24]))
        calndr-end    (calndr/get-calendar (gcomp/input-text :args [:columns 24]))
        price-input   (gcomp/input-text :args [:columns 16])
        enterpreneurs (:enterpreneurs-list (state!))
        select-box    (gcomp/select-box (vec (map (fn [item] (:name item)) enterpreneurs))) 
        panel (gmg/migrid
               :v :f "[75:, fill]"
               (gcomp/min-scrollbox
                (gmg/migrid :> "[150, fill]" {:gap [10]}
                            [(gmg/migrid :v [(label-fn "enterpr:")    select-box])
                             (gmg/migrid :v [(label-fn "price:")      price-input])
                             (gmg/migrid :v [(label-fn "start term:") calndr-start])
                             (gmg/migrid :v [(label-fn "end term:")   calndr-end])
                            
                             (gmg/migrid :v
                                         [(c/label "<html>&nbsp;")
                                          (gcomp/menu-bar
                                           {:buttons [["" (gs/icon GoogleMaterialDesignIcons/DONE)
                                                       (fn [e] (insert-contract state! calndr-start calndr-end price-input select-box))]
                                                      ["" (gs/icon GoogleMaterialDesignIcons/CLOSE)
                                                       (fn [e] (c/config! new-contract-form :items []))]]})])])))]
    (timelife
     0.1 (fn [] (doto (:root (state!)) (.revalidate) (.repaint))))
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
                                        (fn [e] (update-contracts-payment state!))]
                                       
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
               :items (gtool/join-mig-items btns-menu-bar new-contract-form (gcomp/min-scrollbox expand-btns-box)))))

;; :v [1 1 2] =>> :v [:enterpreneur.id :service_contract.id :service_contract_month.id]
;; (some false? (vals (get-in @(:contracts-checkboxs-map @state) [:2 :2])))
;; (vec (map #(keyword (str %)) [2 2 1]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Toolkit with SQl funcs ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn service-period-toolkit-pipeline
  "Description
    Extending plugin-toolkit map with all requires to DB"
  [configuration]
  {:download-data-map               req/info-grouped-query

   :update-service-money            (fn [id money] (req/update-service-money-per-month id money))

   :calc-payment-for-enterpreneuer  (fn [enterpreneuer-k service-contracts-m]
                                      (req/calculate-payment-for-enterpreneuer
                                       enterpreneuer-k service-contracts-m))

   :calc-payment-for-service-contr  (fn [service-contracts-k service-contracts-month-list-m]
                                      (req/calculate-payment-for-service_contract
                                       service-contracts-k service-contracts-month-list-m))

   :update-service-month            (fn [id-list] (req/update-service-month-to-payed id-list))

   :select-enterpreneurs            (fn [] (db/query (select! {:table_name :enterpreneur,
                                                               :column [:enterpreneur.id
                                                                        :enterpreneur.name]})))})
;;;;;;;;;;;;;
;;; Entry ;;;
;;;;;;;;;;;;;

(defn service-period-entry [plugin-path global-configuration-getter]
  (let [root   (gmg/migrid :v {:args [:background face/c-layout-background]} (c/label :text "Loading..."))
        state! (fn [& prop]
                 (cond (= :atom (first prop)) state
                       :else (deref state)))
        state  (create-state-template plugin-path global-configuration-getter root state!)
        dispatch! (create-disptcher state)]
    (create-period-view state! dispatch!)
    root))

;;;;;;;;;;;;
;;; BIND ;;;
;;;;;;;;;;;;

(register-custom-view-plugin
 :name 'service-period
 :description "Plugin for service contracts of enterpreneurs"
 :entry service-period-entry
 :toolkit service-period-toolkit-pipeline
 :spec-list [])
