(ns plugins.service-period.service-period
  (:require
   [jarman.tools.lang         :refer :all] 
   [clojure.spec.alpha        :as s]
   [seesaw.core               :as c]
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

(defn- create-state-template [plugin-path global-configuration-getter]
  (reset! state
          {:plugin-path          plugin-path
           :plugin-global-config global-configuration-getter
           :plugin-config        (get-in (global-configuration-getter) (conj plugin-path :config) {})
           :plugin-toolkit       (get-in (global-configuration-getter) (conj plugin-path :toolkit) {})})
  state)

;;;;;;;;;;;;;;;;;;;;;;;;
;;; HELPER FUNCTIONS ;;;  
;;;;;;;;;;;;;;;;;;;;;;;;
(defn- format-date [date]
  (let [fn-format (fn [date] (.format (java.text.SimpleDateFormat. "dd-MM-YYYY") date))]
    (if (or (vector? date)(seq? date))
      (map (fn [d] (fn-format d)) date)
      (fn-format date))))

(defn- isNumber? [val]
   (if-not (number? (read-string val)) 
     (i/danger (gtool/get-lang-header :error) (gtool/get-lang-alerts :price-must-be-number))
     true))

(defn- split-date [date-vec]
  (apply str (interpose " / " (format-date date-vec))))

(declare build-expand-by-map)

(defn- refresh-panel
  ([panel] (doto panel (.revalidate) (.repaint)))
  ([panel content] (doto panel (.removeAll) (.add content) (.revalidate) (.repaint))))

(defn- refresh-data-and-panel [state!]
  (let [{:keys [new-contract-form
                plugin-root-layout
                exp-panel]}
        (state!)]
    (.removeAll exp-panel)
    (doall
     (map
      (fn [[enterpeneuer service_contract_m]]
        (.add exp-panel (build-expand-by-map [enterpeneuer service_contract_m] state!)))
      (:tree-view ((:select-group-data (:plugin-toolkit (state!)))))))
    (.removeAll new-contract-form)
    (refresh-panel plugin-root-layout)))
  
(defn- back-to-main-panel
  "Description
    remove in plugin-root-layout panel with periods of one contract, add to plugin-root-layout panel with all contracta (like agenda) "
  [state!]
  (let [{:keys [plugin-root-layout new-contract-form exp-panel btns-menu-bar]} (state!)]
    (swap! (state! :atom) merge {:payments-per-month {}})
    (.removeAll plugin-root-layout)
    (.add plugin-root-layout btns-menu-bar)
    (.add plugin-root-layout new-contract-form)
    (.add plugin-root-layout exp-panel)
    (refresh-data-and-panel state!)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Logic for checkboxes ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn- comparator-fn
  "Description
    Return fn with preducat for different id-vec ([1] or [1 2] or [1 2 1])
    It just check if two vectors have same values but onlu for 1, 2 or 3 values inside.
  Example:
    ((comparator-fn [1 2 3]) [1 2 3])
    ;; => true
    ((comparator-fn [1 2 3]) [1 2 2])
    ;; => false
  "
  [id-vec]
  (let [[_e _sc _scm] id-vec]
    (case (count id-vec)
      1 (fn [[e]] (= e _e))
      2 (fn [[e sc]] (and (= e _e) (= sc _sc)))
      3 (fn [[e sc scm]] (and (= e _e) (= sc _sc) (= scm _scm)))
      false)))

(defn- update-checkboxes [id-vec check? checkboxes]
  (let [cmpr? (comparator-fn id-vec)]
    (reset! checkboxes (reduce (fn [acc period] (if (cmpr? period)
                                                  (conj acc (assoc period 3 check?))
                                                  (conj acc period))) [] @checkboxes))))

(defn- checked-box? [id-vec checkboxes]
  (let [cmpr? (comparator-fn id-vec)]
    (reduce
     (fn [acc period]
       (if (cmpr? period)
         (and acc (last period))
         acc))
     true
     checkboxes)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; GUI for one Contract;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn- update-money-per-month
  "Description
    require to db for update money for one period"
  [state!]
  (let [update-fn  (:update-service-money (:plugin-toolkit (state!)))]
    (doall (map (fn [[k v]] (update-fn k v)) (:payments-per-month (state!))))
    (i/info (gtool/get-lang-header :info)
            (gtool/get-lang-alerts :updated-data))))

(defn- panel-one-period
  "Description
    return mig-panel for one period"
  [state! model-data]
  (let [{id              :service_contract_month.id
         st-date         :service_contract_month.service_month_start 
         end-date        :service_contract_month.service_month_end
         payment-st      :service_contract_month.was_payed
         payment         :service_contract_month.money_per_month}  model-data
        label-fn     (fn [text foreground] (seesaw.core/label :text (str text)
                                                              :font (gtool/getFont 16)
                                                              :foreground foreground))
        caret-fn     (fn [e] (if (isNumber? (seesaw.core/text e))
                               (if-not (= (read-string (seesaw.core/text e)) payment) 
                                 (swap! (state! :atom) merge {:payments-per-month (merge (:payments-per-month (state!))
                                                                                  {id (seesaw.core/text e)})}))))
        date         (str st-date " / " end-date)
        status       (if payment-st "It was payed, you cannot change this field" "Waiting for payment")
        panel        (seesaw.mig/mig-panel :constraints ["wrap 2" "5px[]30px" "10px[]10px"]
                                           :items [[(label-fn "period:" gcomp/dark-grey-color)]
                                                   [(label-fn date gcomp/blue-color)]
                                                   [(label-fn "payment:" gcomp/dark-grey-color)]
                                                   [(if payment-st (label-fn payment gcomp/blue-color)
                                                        (gcomp/input-text :args [:columns 7 :text payment
                                                                                 :listen [:caret-update caret-fn]]))]
                                                   [(label-fn "payment status:" gcomp/dark-grey-color)]
                                                   [(label-fn status gcomp/blue-color)]])] panel))

(defn- panel-one-contract-periods
  "Description
    return mig-panel with button-bar and panels with all periods of one contract"
  [state! data]
  (let [btns-menu-bar (gcomp/menu-bar
                   {:justify-end true
                    :buttons [[(gtool/get-lang-btns :back)
                               (gs/icon GoogleMaterialDesignIcons/ARROW_BACK)
                               (fn [e] (back-to-main-panel state!))]
                              [(gtool/get-lang-header :save)
                               (gs/icon GoogleMaterialDesignIcons/DONE)
                               (fn [e] (update-money-per-month state!))]
                              [(gtool/get-lang-header :export-odt)
                               icon/odt-64-png
                               (fn [e] )]]})
        panel     (seesaw.mig/mig-panel :constraints ["wrap 1" "15px[]15px" "15px[]15px"]
                                        :items [[btns-menu-bar]])]
    (doall (map (fn [period] (.add panel (panel-one-period state! period))) data)) panel))

(defn- open-periods-contract-fn [state! contract-data]
  (fn [e] (seesaw.core/config! (:plugin-root-layout (state!))
                               :items (gtool/join-mig-items
                                       (panel-one-contract-periods state! contract-data)))
    (refresh-panel (:plugin-root-layout (state!)))))

;;;;;;;;;;;;;;;;;;;;;
;;; EXPAND PANELS ;;;
;;;;;;;;;;;;;;;;;;;;;

(defn- check-if-checkbox-should-be-render 
  "Description
      Check enterprenuer contracts and subcontracts
      inside atom :contracts-checkboxs-map are only active contracts
      and his checkbox state, true or false
     
      if path return to bool-or-map nil then
         contract or subcontract was payed
         so checbox cannot be rendering
     
      if path return to bool-or-map false then
         contract or subcontract is still active
         checkbox should be rendering
     
      else if inside bool-or-map is map then run recursively
         fn to check upper rules deeper in map
     
      render? return true if inside contract or enterprenuer
         is some unpayed contract or subcontruct and if will
         be true then checkbox will be rendering
  Example:
     {:a {:b {:c true :d false}}} ;; => true
     {:a {:b {:c true :d true}}}  ;; => nil
     {:d false} ;; => true
     false      ;; => true
     true       ;; => nil
     "
  [bool-or-map]
  (let [render? (some false?
                      (flatten
                       ((fn check-complete [contracts-m]
                          (cond
                            (map? contracts-m)
                            (doall
                             (map
                              #(if (map? (second %))
                                 (check-complete (second %))
                                 (if (or (= true (second %))
                                          (nil? (second %))) true false))
                              contracts-m))

                            (or (nil?  contracts-m)
                                (true? contracts-m)) true
                            
                            (false? contracts-m) [false]

                            :else true))
                        bool-or-map)))]
    ;; return step by step example
    ;; {:d {:a true :b false}} => ((true false)) => flatten (true false) => some false? true
    ;; {:d {:a true :b true}}  => ((true true))  => flatten (true true)  => some false? nil
    render?))

(defn- check-checkbox-state
  "Description
      
     "
  [bool-or-map]
  (let [checkbox-state ((fn check-complete [contracts-m]
                          (cond
                            (map? contracts-m)
                            (doall
                             (map
                              #(if (map? (second %))
                                 (check-complete (second %))
                                 (cond (true?   (second %)) :selected
                                       (false?  (second %)) :unselected
                                       :else nil))
                              contracts-m))

                            (nil?  contracts-m) :payed
                            (true? contracts-m) :selected
                            
                            (false? contracts-m) :unselected

                            :else :selected))
                        bool-or-map)
        render? (if (sequential? checkbox-state)
                  (let [some-inside? (some #(or (= :selected %)
                                                (= :unselected %))
                                           (flatten checkbox-state))]
                    (if (nil? some-inside?) :payed some-inside?))
                  checkbox-state)]
    render?))


(defn- v-to-vmp
  "Description:
    vector to vector map path
   Example
    [1 2 3] ;; => [:1 :2 :3]"
  [v] (vec (map #(keyword (str %)) v)))

(defn- compo-checkbox-subcontract
  []
  (c/label :text "O"))

(defn- compo-checkbox-contract
  [root render-fn some-contract checkboxes-state selected]
  (c/checkbox :selected? selected
              :listen [:mouse-clicked
                       (fn [e] (let [selected? (.isSelected (.getComponent e))
                                     contracts-m-path (v-to-vmp (:v some-contract))
                                     contracts-m-part (get-in @checkboxes-state contracts-m-path)
                                     contracts-m-part (into {} (map (fn [[id bool]] {id selected?}) contracts-m-part))]
                                 (swap! checkboxes-state #(assoc-in
                                                           %
                                                           contracts-m-path
                                                           contracts-m-part))
                                 (c/config! root :items (gtool/join-mig-items (render-fn)))))]))

(defn- build-expand-by-map
  "Description
     Recursion, which build panels (button-expand for enterpeneuer and service contract, child-expand for service periods), using data from db like configuration map"
  [plugin-v state!]
  (let [{:keys [select-group-data
                calc-payment-for-enterpreneuer
                calc-payment-for-service-contr]}
        (:plugin-toolkit (state!))

        ;; map with checkboxes state
        ;; paths are buliding as {:enterpreneur.id {:service_contract.id {:service_contract_month.id false}}}
        checkboxes-state (:contracts-checkboxs-map (state!))
        
        ;; return true if contract is not payed
        render-checkbox?
        (fn [some-contract]
          (let [map-path  (v-to-vmp (:v some-contract))
                bool-or-map (get-in @checkboxes-state map-path)
                render? (check-checkbox-state bool-or-map) ;;(check-if-checkbox-should-be-render bool-or-map)
                ]
            ;;(println "\n" (:v some-contract) bool-or-map render?)
            render?))
        
        ;; Generate expand button with checkbox or done icon for subcontructs
        contract-month-expand-btn
        (fn [subcontracts-l]
          (doall
           (map
            (fn [subcontract]
              (let [checkbox (render-checkbox? subcontract)
                    root (gcomp/button-expand-child
                          (str (split-date [(:service_contract_month.service_month_start subcontract)
                                            (:service_contract_month.service_month_end   subcontract)])
                               (if (or (= checkbox :selected) (= checkbox :unselected)) ""
                                   (str " / " (:service_contract_month.money_per_month subcontract) " UAH")))
                          :onClick (open-periods-contract-fn state! subcontracts-l)
                          :before-title (cond (= checkbox :selected)
                                              (compo-checkbox-subcontract)
                                              
                                              (= checkbox :unselected)
                                              (compo-checkbox-subcontract)

                                              :else
                                              (fn [] (c/label :icon (gs/icon GoogleMaterialDesignIcons/CHECK_CIRCLE))))
                          :lvl 7
                          :height 25)]
                root))
            subcontracts-l)))
        
        ;; Generate expand button with checkbox or done icon for contructs
        contract-expand-btn (fn [enterprenuer-data subcontracts-l]
                              (let [render-fn (fn [] (contract-month-expand-btn subcontracts-l))
                                    subcontructs-box (gmg/migrid :v (render-fn))
                                    checkbox (render-checkbox? enterprenuer-data)
                                    root (gcomp/button-expand
                                          (str (split-date [(:service_contract.contract_start_term enterprenuer-data)
                                                            (:service_contract.contract_end_term   enterprenuer-data)])
                                               (if-not (= checkbox :payed) ""
                                                   (str " / " (calc-payment-for-service-contr enterprenuer-data
                                                                                              subcontracts-l) " UAH")))
                                          subcontructs-box
                                          :before-title
                                          (if (= checkbox :payed)
                                                (fn [] (c/label :icon (gs/icon GoogleMaterialDesignIcons/CHECK_CIRCLE)))
                                                (fn [] (compo-checkbox-contract subcontructs-box render-fn enterprenuer-data checkboxes-state false)))
                                          :lvl 3)]
                                root))
        
        ;; Generate expand button with checkbox or done icon for enterprenuer
        enterprenier-expand-btn
        (fn [enterprenuer-data contracts-m]
          (let [checkbox (render-checkbox? enterprenuer-data)
                root (gcomp/button-expand
                      (str (:enterpreneur.name enterprenuer-data) 
                           (if checkbox ""
                               (str " / "  (calc-payment-for-enterpreneuer enterprenuer-data contracts-m) " UAH")))

                      (map (fn [[enterprenuer-data contracts]]
                             (contract-expand-btn enterprenuer-data contracts))
                           contracts-m) ;; contracts-m => ({subcontract} {subcontract} ...)
                      :before-title (if checkbox
                                      (fn [] (c/checkbox))
                                      (fn [] (c/label :icon (gs/icon GoogleMaterialDesignIcons/CHECK_CIRCLE))))
                      :lvl 1)]
            root))]
    
    (gmg/migrid
     :v
     (let [[enterprenuer-data contracts-m-or-subcontracts-l] plugin-v]
       (if (map? contracts-m-or-subcontracts-l)
         (enterprenier-expand-btn enterprenuer-data contracts-m-or-subcontracts-l)
         ;; contracts map and enterprenier btn ;; contracts-m-or-subcontracts-l => {{contract data} (subcontracts-list)}
         )))))

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
        date-to-obj   (fn [data-string] (.parse (java.text.SimpleDateFormat. "dd-MM-YYYY") data-string))
        id-entr       (:id (first (filter (fn [x] (= (:name x) entpr)) enterpreneurs)))]
    (if-let [alerts
             (not-empty
              (cond-> []
                (empty? entpr)
                (conj (fn [] (i/warning (gtool/get-lang-header :invalid-enterpreneur)
                                        (gtool/get-lang-alerts :field-is-empty))))

                (or (empty? price) (not (isNumber? price)))
                (conj (fn [] (i/warning (gtool/get-lang-header :invalid-price)
                                        (gtool/get-lang-alerts :price-must-be-number))))

                (or (empty? date1) (empty? date2) (req/data-comparator-old-new (date-to-obj date1) (date-to-obj date2)))
                (conj (fn [] (i/warning (gtool/get-lang-header :invalid-date)
                                        (gtool/get-lang-alerts :invalid-date-time-interval-info))))))]
      (doall (map (fn [invoke-alert] (invoke-alert)) alerts))
      (let [ins-req (req/insert-all id-entr (date-to-obj date1) (date-to-obj date2) (read-string price))]
        (if ins-req
          (i/success (gtool/get-lang-header :success)
                     (gtool/get-lang-alerts :added-service-contract))
          (i/danger  (gtool/get-lang-header :error-with-sql-require)
                     ins-req))))))

(defn- update-contracts [state!]
  (let [{:keys [update-service-month select-group-data]} (:plugin-toolkit (state!))
        checkboxes  (doall (filter (fn [item] (= (last item) true)) @(:checkboxes (state!))))]
    (if (empty? checkboxes)
      (i/warning (gtool/get-lang-header :warning)
                 (gtool/get-lang-alerts :select-checkbox-for-payment))
                 
      (do (update-service-month checkboxes)
          (let [tree-index-paths (:tree-index-paths (select-group-data))]
            (swap! (state! :atom) merge {:tree-index-paths tree-index-paths :checkboxes  (atom (vec (filter (fn [item] (= (last item) false)) tree-index-paths)))})
            (refresh-data-and-panel state!)
            (i/success (gtool/get-lang-header :success)
                       (gtool/get-lang-alerts :payments-finished-successfully)))))))

;;;;;;;;;;;;;;;;;;;;;;;
;;; Panels for view ;;;
;;;;;;;;;;;;;;;;;;;;;;;
(defn- add-contract-panel
  "Description
    in main-panel with all contracts add to new-contract-form panel with fields for input data"
  [state!]
  (let [{:keys [new-contract-form]}  (state!)
        label-fn      (fn [text] (seesaw.core/label :text text :font (gtool/getFont 16)
                                                    :border (seesaw.border/empty-border :right 8)))
        calndr-start  (calndr/get-calendar (gcomp/input-text :args [:columns 24]))
        calndr-end    (calndr/get-calendar (gcomp/input-text :args [:columns 24]))
        price-input   (gcomp/input-text :args [:columns 16])
        enterpreneurs (:enterpreneurs-list (state!))
        select-box    (gcomp/select-box (vec (map (fn [item] (:name item)) enterpreneurs)))] 
    (seesaw.mig/mig-panel :constraints ["wrap 3" "10px[]10px" "10px[]10px"]
                          :items (gtool/join-mig-items
                                  (seesaw.core/horizontal-panel
                                   :items (list (label-fn "enterpr:") select-box))  ;;; TO DO change this panel for more dynamic, look on labels, i mean SPACE ("price:      ") ;; TODO: dynamic lang
                                  (seesaw.core/horizontal-panel
                                   :items (list (label-fn "start term:") calndr-start));; TODO: dynamic lang
                                  (gcomp/menu-bar
                                   {:buttons [[(gtool/get-lang-btns :add)
                                               (gs/icon GoogleMaterialDesignIcons/LIBRARY_ADD)
                                               (fn [e] (insert-contract state! calndr-start calndr-end price-input select-box))]
                                              [(gtool/get-lang-btns :cancel)
                                               (gs/icon GoogleMaterialDesignIcons/CLOSE)
                                               (fn [e] (seesaw.core/config! new-contract-form :items [[(seesaw.core/label)]]))]]})
                                  (seesaw.core/horizontal-panel
                                   :items (list (label-fn "price:    ") price-input));; TODO: dynamic lang
                                  (seesaw.core/horizontal-panel
                                   :items (list (label-fn "end term:  ") calndr-end))))));; TODO: dynamic lang

(defn- create-period-view
  "Description
    Entry func with main panel builder. There are all enterpreneurs and contracts."
  [state! dispatch!]
  (let [{:keys [select-group-data
                select-enterpreneurs]}
        (:plugin-toolkit (state!))
        tree-index-paths   (:tree-index-paths (select-group-data)) ;; [:enterpreneur.id :service_contract.id :service_contract_month.id checked-or-not]
        checkboxes         (atom (vec (filter (fn [item] (= (last item) false)) tree-index-paths)))

        contracts-checkboxs-map (let [smap (atom {})]
                                  (doall
                                   (map
                                    (fn [[enter-id sc-id scm-id tf]]
                                      (swap! smap #(assoc-in % [(keyword (str enter-id)) (keyword (str sc-id)) (keyword (str scm-id))] tf))
                                      )
                                    @checkboxes))
                                  @smap)
        
        enterpreneurs-list (select-enterpreneurs)
        bln-checkboxes-fn  (fn [bln]
                             (swap! (state! :atom) merge
                                    {:checkboxes (atom
                                                  (vec
                                                   (map
                                                    (fn [item]
                                                      (conj (vec (butlast item)) bln))
                                                    @checkboxes)))}))
        new-contract-form (gmg/migrid :v [])
        btns-menu-bar      (gcomp/menu-bar
                            {:buttons [[(gtool/get-lang-btns :add-contract)
                                        (gs/icon GoogleMaterialDesignIcons/NOTE_ADD)
                                        (fn [e] (seesaw.core/config! new-contract-form :items [[(add-contract-panel state!)]]))]

                                       [(gtool/get-lang-btns :select-all)
                                        (gs/icon GoogleMaterialDesignIcons/CHECK_BOX)
                                        (fn [e] (bln-checkboxes-fn true)
                                          (refresh-data-and-panel state!))]

                                       [(gtool/get-lang-btns :pay)
                                        (gs/icon GoogleMaterialDesignIcons/MONETIZATION_ON)
                                        (fn [e] (update-contracts state!))]
                                       
                                       [(gtool/get-lang-btns :refresh)
                                        (gs/icon GoogleMaterialDesignIcons/CACHED)
                                        (fn [e] (bln-checkboxes-fn false) (refresh-data-and-panel state!))]
                                       ]})
        exp-panel          (seesaw.core/vertical-panel)
        plugin-root-layout (gmg/migrid :v [btns-menu-bar new-contract-form (seesaw.core/border-panel :center exp-panel)])]

    (let [sup-map {:enterpreneurs-list enterpreneurs-list
                   :btns-menu-bar      btns-menu-bar
                   :tree-index-paths   tree-index-paths
                   :plugin-root-layout plugin-root-layout
                   :exp-panel          exp-panel
                   :new-contract-form  new-contract-form
                   :checkboxes         checkboxes
                   :contracts-checkboxs-map (atom contracts-checkboxs-map)                 
                   }]
      (swap! (state! :atom) merge sup-map))
    
    (refresh-data-and-panel state!)
    (gmg/migrid :> (gcomp/min-scrollbox plugin-root-layout :border nil))))

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
  {:select-group-data               req/info-grouped-query

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
  (let [state  (create-state-template plugin-path global-configuration-getter)
        state! (fn [& prop]
                 (cond (= :atom (first prop)) state
                       :else (deref state)))
        dispatch! (create-disptcher state)]
    (create-period-view state! dispatch!)))

;;;;;;;;;;;;
;;; BIND ;;;
;;;;;;;;;;;;

(register-custom-view-plugin
 :name 'service-period
 :description "Plugin for service contracts of enterpreneurs"
 :entry service-period-entry
 :toolkit service-period-toolkit-pipeline
 :spec-list [])
