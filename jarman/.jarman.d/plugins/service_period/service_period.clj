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
           :state!               state!
           :subcontracts-payment-state (atom {})})
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

(defn- isNumber? [val-s]
  (try (float (read-string val-s))
    (catch Exception e false)))

(declare build-expand-contracts-for-entrepreneur)

(defn- refresh-data-and-panel [state!]
  (let [{:keys [root expand-btns-box entrepreneurs-m contracts-m subcontracts-m]} (state!)
        fresh-data ((:download-data-map (:plugin-toolkit (state!))))]

    (reset! entrepreneurs-m (:entrepreneurs-m fresh-data))
    (reset! contracts-m     (:contracts-m     fresh-data))
    (reset! subcontracts-m  (:subcontracts-m  fresh-data))
    (swap! (state! :atom) #(assoc % :entrepreneurs-list (:entrepreneurs-list fresh-data)))
    
    (let [items (gtool/join-mig-items
                 (doall
                  (map
                   (fn [[entrepreneur-id]]
                     (build-expand-contracts-for-entrepreneur state! entrepreneur-id))
                   @entrepreneurs-m)))]
      (c/config!
       expand-btns-box
       :items (if (empty? items)
                [[(gmg/migrid :v :center {:gap [25]}
                              (c/label :text (gtool/get-lang-infos :no-items)
                                       :font (gs/getFont 16)
                                       :foreground face/c-icon))]]
                items)))
    (doto root (.revalidate) (.repaint))))

(defn- update-subcontracts-state
  "Description:
     Swap if some prices was changed in DB."
  [state!]
  (if (:db-saved @(:subcontracts-payment-state (state!))) 
      (let [new-map       @(:subcontracts-m (state!))
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
        (reset! (:subcontracts-m (state!)) swap-map)))
  (reset! (:subcontracts-payment-state (state!)) {:path [] :db-saved false :updated-prices {}}))

(defn- back-to-main-panel
  "Description
    remove in root panel with periods of one contract, add to root panel with all contracta (like agenda) "
  [state!]
  (let [{:keys [root new-contract-form expand-btns-box btns-menu-bar]} (state!)]
    ;; (c/config! (:root (state!)) :items (gtool/join-mig-items btns-menu-bar new-contract-form (gcomp/min-scrollbox expand-btns-box)))
    (c/config! (:root (state!)) :items (gtool/join-mig-items btns-menu-bar new-contract-form expand-btns-box))
    (update-subcontracts-state state!)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; GUI for one Contract;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- update-subcontract-price
  "Description
    require to db for update money for one period"
  [state!]
  (let [update-fn (:update-service-money (:plugin-toolkit (state!)))]
    (doall (map (fn [[id money]] (update-fn id money)) (vec (:updated-prices @(:subcontracts-payment-state (state!))))))
    (i/info (gtool/get-lang-header :info)
            (gtool/get-lang-alerts :updated-data))
    (swap! (:subcontracts-payment-state (state!)) #(assoc % :db-saved true))))

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
                                                          :db-saved false
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
                         (number-input (subcontract-price-fn) false (fn [new-val] (swap! subcontracts-payment-a #(assoc-in % [:updated-prices id] new-val)))))])

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
                                                   ;; (let [checkboxes-state (:contracts-checkboxs-map (state!))]
                                                   ;;   (swap! checkboxes-state #(assoc-in % subcontract-path true))
                                                   ;;   (update-contracts-payment state!)
                                                   ;;   (c/config! panel :items (gtool/join-mig-items (rerender true)))
                                                   ;;   (doto panel (.revalidate) (.repaint)))
                                                   ))
                                       
                                       (gcomp/button-slim (gtool/get-lang-btns :remove)
                                                          :args [:icon (gs/icon GoogleMaterialDesignIcons/REMOVE_CIRCLE)])])])]
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
                                   (fn [e] (update-subcontract-price state!))]
                                  
                                  [(gtool/get-lang-btns :export-odt)
                                   icon/odt-64-png
                                   (fn [e] )]]})
        
        subcontracts (gmg/migrid :v
                      (doall (map
                              (fn [[subcontract-id subcontract]]
                                (let [subcontract-path (join-vec contract-path [subcontract-id])]
                                    (panel-with-subcontract-rows state! subcontract-path (= subcontract-path clicked-sub-path))))
                              (get-in @(:subcontracts-m (state!)) contract-path))))]
    ;; (list btns-menu-bar (gcomp/min-scrollbox subcontracts))
    (list btns-menu-bar subcontracts)))

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
;;  CHECKBOXES FOR ENTREPRENEUR
;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; STATE INTERACTIVE TEST
;; (let [sw (:subcontracts-m @state)]
;;   (swap! sw #(assoc-in % [:1 :3 :207 :service_contract_month.was_payed] false)))
(defn- loop-all-contracts
  [state! entrepreneur-path func]
  (let [unpayed? (some false? (flatten (doall
                                        (map
                                         (fn [[contract-id contract]] (func contract-id contract))
                                         (get-in @(:contracts-m (state!)) entrepreneur-path)))))]
    (if (nil? unpayed?) true (not unpayed?))))

(defn- all-contracts-payed?
  [state! entrepreneur-path]
  (loop-all-contracts state! entrepreneur-path
                      (fn [contract-id contract]
                        (let [contract-path (join-vec entrepreneur-path [contract-id])]
                          (map
                           (fn [[sub-id sub]] (if (:service_contract_month.was_payed sub) true false))
                           (get-in @(:subcontracts-m (state!)) contract-path))))))

(defn- all-contracts-selected?
  "Description:
     Return true is all subcontracts for entrepreneur are selected or false if not"
  [state! entrepreneur-path]
  (loop-all-contracts state! entrepreneur-path
                      (fn [contract-id contract]
                        (check-if-contract-selected state! (join-vec entrepreneur-path [contract-id])))))

(defn- select-all-contracts
  [state! entrepreneur-path selected?]
  (doall (map
          (fn [[contract-id contract]]
            (let [contract-path (join-vec entrepreneur-path [contract-id])]
              (rift (select-all-subcontracts state! contract-path selected?)
                    (:selected? contract))))
          (get-in @(:contracts-m (state!)) entrepreneur-path))))

(defn- calculate-all-contract-price
  [state! entrepreneur-path]
  (apply + (flatten (doall
             (map
              (fn [[contract-id contract]]
                (calculate-contract-price state! (join-vec entrepreneur-path [contract-id])))
              (get-in @(:contracts-m (state!)) entrepreneur-path))))))

(defn- entrepreneur-checkbox
  [state! root render-header entrepreneur-path checkbox-selected?]
  (let [cbox (c/label :icon (i-checkbox checkbox-selected?)
                      :listen [:mouse-clicked
                               (fn [e] ;; SELECT OR UNSELECT ALL CONTRACTS
                                 (let [self-selected? (not (all-contracts-selected? state! entrepreneur-path))]
                                   (select-all-contracts state! entrepreneur-path self-selected?)
                                   (c/config! e :icon (i-checkbox self-selected?))))])

        watcher-id (keyword (str "entrepreneur-" (clojure.string/join "-" entrepreneur-path)))]

    ;; If in subcontract something will be unchecked or all will be checked then set contract checkbox selected or not
    ;; WATCH IF ALL SUBCONTRACTS SELECTED
    (new-watcher (:subcontracts-m (state!)) watcher-id entrepreneur-path
                 (fn []
                   (let [payed?         (all-contracts-payed?    state! entrepreneur-path)
                         all-selected?  (all-contracts-selected? state! entrepreneur-path)
                         contract-price (calculate-all-contract-price state! entrepreneur-path)]

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
  [state! entrepreneur-path]
  (doall
   (map
    (fn [[contract-id contract]]
      (let [contract-id        (keyword (str (:service_contract.id contract)))
            contract-path      (join-vec entrepreneur-path [contract-id])
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
  (let [entrepreneur-path  [entrepreneur-id-k]
        payed?             (all-contracts-payed? state! entrepreneur-path)
        checkbox-selected? (all-contracts-selected? state! entrepreneur-path)

        render-fn          #(create-contracts-expand-btns state! entrepreneur-path)
        entrepreneur-box   (gmg/migrid :v (render-fn))
        rerender-header (fn [price]
                          (format "<html> %s &nbsp;&nbsp;<b> %s </b> %s </html>" 
                                  (get-in @(:entrepreneurs-m (state!)) [entrepreneur-id-k :enterpreneur.name])
                                  (if (all-contracts-payed? state! entrepreneur-path) ""
                                      (str price " " (:currency (state!))))
                                  ""))
        root (gcomp/button-expand
              (rerender-header (calculate-all-contract-price state! entrepreneur-path))
              entrepreneur-box
              :before-title (if payed?
                              ;; subcontract was payed, so set icon done
                              #(c/label :icon (gs/icon GoogleMaterialDesignIcons/CHECK))
                              ;; checkbox is true or false, so render checkbox
                              #(entrepreneur-checkbox state! entrepreneur-box rerender-header entrepreneur-path checkbox-selected?))
              :lvl 1)]
    root))
;; :v [1 1 2] =>> :v [:enterpreneur.id :service_contract.id :service_contract_month.id]


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;
;;; REQUIRES ;;;
;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- add-months
  [data-string add-m]
  (str (.plusMonths (java.time.LocalDate/parse data-string) add-m)))

(defn- add-days
  [data-string add-d]
  (str (.plusDays (java.time.LocalDate/parse data-string) add-d)))
 
(defn- insert-contract [state! calndr-start calndr-end price-input select-box]
  (let [date-start (seesaw.core/text calndr-start)
        date-end   (seesaw.core/text calndr-end)
        price      (seesaw.core/text price-input)
        selected-entrepreneur (seesaw.core/selection select-box)
        id-entrepreneur (first (first (filter (fn [v] (= (second v) selected-entrepreneur)) (:entrepreneurs-list (state!)))))]
    
    (if-let [alerts
             (not-empty
              (cond-> []
                (empty? selected-entrepreneur)
                (conj (fn [] (i/warning (gtool/get-lang-header :invalid-enterpreneur)
                                        (gtool/get-lang-alerts :field-is-empty))))

                (or (empty? price) (= (isNumber? price) false))
                (conj (fn [] (i/warning (gtool/get-lang-header :invalid-price)
                                        (gtool/get-lang-alerts :price-must-be-number))))

                (or (empty? date-start) (empty? date-end) (req/data-comparator-old-new (req/date-to-obj date-start) (req/date-to-obj date-end)))
                (conj (fn [] (i/warning (gtool/get-lang-header :invalid-date)
                                        (gtool/get-lang-alerts :invalid-date-time-interval-info))))))]
      
      (doall (map (fn [invoke-alert] (invoke-alert)) alerts))
      (let [ins-req (req/insert-all id-entrepreneur (req/date-to-obj date-start) (req/date-to-obj date-end) (read-string price))]
        (if ins-req
          (i/success (gtool/get-lang-header :success)
                     (gtool/get-lang-alerts :added-service-contract))
          (i/danger  (gtool/get-lang-header :error-with-sql-require)
                     ins-req))))))

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
        entrepreneurs-names-list (map #(second %) (:entrepreneurs-list (state!)))
        select-box    (gcomp/select-box entrepreneurs-names-list)
        panel (gmg/migrid
               :v :f "[75:, fill]"
               ;; gcomp/min-scrollbox
               (gmg/migrid :> "[150, fill]" {:gap [10]}
                           [(gmg/migrid :v [(label-fn (gtool/get-lang-header :entrepreneur)) select-box])
                            (gmg/migrid :v [(label-fn (gtool/get-lang-header :price))        price-input])
                            (gmg/migrid :v [(label-fn (gtool/get-lang-header :date-start))   calndr-start])
                            (gmg/migrid :v [(label-fn (gtool/get-lang-header :date-end))     calndr-end])
                            
                            (gmg/migrid :v
                                        [(c/label "<html>&nbsp;")
                                         (gcomp/menu-bar
                                          {:buttons [["" (gs/icon GoogleMaterialDesignIcons/DONE)
                                                      (fn [e] (insert-contract state! calndr-start calndr-end price-input select-box))]
                                                     ["" (gs/icon GoogleMaterialDesignIcons/CLOSE)
                                                      (fn [e] (c/config! new-contract-form :items []))]]})])]))]
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
               :items (gtool/join-mig-items btns-menu-bar new-contract-form expand-btns-box))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO: SubKontrakty powinny być generowane na podstawie kontraktu, jest funkcja rozbijająca po miesiącach na podstawie daty od i do, jeśli data nie ma pełnych miesięcy to ostatni z subkontraktów jest od daty rozpoczęcia okresu do ostatnich wskazanych dni. Przykładowo od 01-01-2021 do 15-02-2021 da kontrakty od 01-01-2021 do 31-01-2021 i od 01-02-2021 do 15-02-2021.

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


;; :v [1 1 2] =>> :v [:enterpreneur.id :service_contract.id :service_contract_month.id]
;; (some false? (vals (get-in @(:contracts-checkboxs-map @state) [:2 :2])))
;; (vec (map #(keyword (str %)) [2 2 1]))

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
    root))

;;;;;;;;;;;;
;;; BIND ;;;
;;;;;;;;;;;;

(register-custom-view-plugin
 :name 'service-period
 :description "Plugin for service contracts of entrepreneur"
 :entry service-period-entry
 :toolkit service-period-toolkit-pipeline
 :spec-list [])
