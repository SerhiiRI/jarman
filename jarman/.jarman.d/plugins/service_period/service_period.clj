(ns plugins.service-period.service-period
  (:require
   [jarman.tools.lang         :refer :all] 
   [clojure.spec.alpha        :as s]
   [jarman.gui.gui-tools      :as gtool]
   [jarman.gui.gui-components :as gcomp]
   [jarman.gui.gui-migrid     :as gmg]
   [jarman.gui.gui-calendar   :as calndr]
   [jarman.logic.sql-tool     :refer [select!]]
   [jarman.logic.connection   :as db]
   [jarman.logic.state        :as state]
   [jarman.plugin.spec        :as spec]
   [jarman.gui.gui-style      :as gs]
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
    :add-missing  (assoc-in state (:path action-m) nil)
    :test         (do (println "\nTest:n") state)
    :update-login (assoc-in state [:login]    (:value action-m))
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
     (i/danger "Error" "Price must be number") ;; TODO: dynamic language
     true))

(defn- split-date [date-vec]
  (apply str (interpose " / " (format-date date-vec))))

(declare build-expand-by-map)

(defn- refresh-panel
  ([panel] (doto panel (.revalidate) (.repaint)))
  ([panel content] (doto panel (.removeAll) (.add content) (.revalidate) (.repaint))))

(defn- refresh-data-and-panel [state!]
  (let [{:keys [insert-space view-space exp-panel]} (state!)]
    (.removeAll exp-panel)
    (doall
     (map (fn [[entr srv_cntr]] (.add exp-panel (build-expand-by-map [entr srv_cntr] (seesaw.core/vertical-panel) state!)))
          (:tree-view ((:select-group-data (:plugin-toolkit (state!)))))))
    (.removeAll insert-space)
    (refresh-panel view-space)))
  
(defn- back-to-main-panel
  "Description
    remove in view-space panel with periods of one contract, add to view-space panel with all contracta (like agenda) "
  [state!]
  (let [{:keys [view-space insert-space exp-panel btn-panel]} (state!)]
    (swap! (state! :atom) merge {:payments-per-month {}})
    (.removeAll view-space)
    (.add view-space btn-panel)
    (.add view-space insert-space)
    (.add view-space exp-panel)
    (refresh-data-and-panel state!)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Logic for checkboxes ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn- comparator-fn
  "Description
    return fn with preducat for different id-vec ([1] or [1 2] or [1 2 1])"
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
    (reduce (fn [acc period] (if (cmpr? period)
                               (and acc (last period))
                               acc)) true checkboxes)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; GUI for one Contract;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn- update-money-per-month
  "Description
    require to db for update money for one period"
  [state!]
  (let [update-fn  (:update-service-money (:plugin-toolkit (state!)))]
    (doall (map (fn [[k v]] (update-fn k v)) (:payments-per-month (state!))))
    (i/info "Messege" "Data was updated"))) ;; TODO: dynamic lang

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
  (let [btn-panel (gcomp/menu-bar
                   {:justify-end true
                    :buttons [["Back" ;; TODO: dynamic lang
                               (gs/icon GoogleMaterialDesignIcons/ARROW_BACK)
                               (fn [e] (back-to-main-panel state!))]
                              ["Save" ;; TODO: dynamic lang
                               (gs/icon GoogleMaterialDesignIcons/DONE)
                               (fn [e] (update-money-per-month state!))]
                              ["Export ODT" ;; TODO: dynamic lang
                               icon/odt-64-png
                               (fn [e] )]]})
        panel     (seesaw.mig/mig-panel :constraints ["wrap 1" "15px[]15px" "15px[]15px"]
                                        :items [[btn-panel]])]
    (doall (map (fn [period] (.add panel (panel-one-period state! period))) data)) panel))

(defn- open-periods-contract-fn [state! contract-data]
  (fn [e] (seesaw.core/config! (:view-space (state!))
                               :items (gtool/join-mig-items
                                       (panel-one-contract-periods state! contract-data)))
    (refresh-panel (:view-space (state!)))))

;;;;;;;;;;;;;;;;;;;;;
;;; EXPAND PANELS ;;;
;;;;;;;;;;;;;;;;;;;;;

(defn- build-expand-by-map
  "Description
     Recursion, which build panels (button-expand for enterpeneuer and service contract, child-expand for service periods), using data from db like configuration map"
  [plugin-m pan state!]
  (let [{:keys [select-group-data calc-payment-for-enterpreneuer calc-payment-for-service-contr]} (:plugin-toolkit (state!))
        checkboxes      (:checkboxes (state!))
        v-pan           (seesaw.core/vertical-panel)
        mig-hor         (fn [item color id-vec] ;; panel with checkbox and expand-panel
                          (seesaw.core/border-panel
                           :background color
                           :west   (seesaw.core/flow-panel
                                    :background "#fff"
                                    :size [25 :by 25]
                                    :items (list (if (checked-box? id-vec (:tip (state!))) ;; TO DO on click chekbox, panel with one contract repaint, so epand button is closed again, we need way in which we will know expan is closed or open? 
                                                   (seesaw.core/label)
                                                   (seesaw.core/checkbox
                                                    :selected? (checked-box? id-vec @checkboxes)
                                                    :listen [:mouse-clicked
                                                             (fn [e] (update-checkboxes
                                                                      id-vec
                                                                      (.isSelected (.getComponent e)) checkboxes)
                                                               (.removeAll pan)
                                                               (build-expand-by-map plugin-m pan state!)
                                                               (refresh-panel pan))]
                                                    :background "#fff"))))
                           :center (seesaw.mig/mig-panel :constraints ["wrap 1" "0px[:820]0px[]0px" "0px[top]0px"]
                                                         :background "#cecece"
                                                         :items [[item]])))
        scm-start-term    :service_contract_month.service_month_start
        scm-end-term      :service_contract_month.service_month_end
        scm-money         :service_contract_month.money_per_month
        sc-start-term     :service_contract.contract_start_term
        sc-end-term       :service_contract.contract_end_term
        entr-name         :enterpreneur.name
        checked?          (fn [id] (checked-box? id @checkboxes))

        exp-panel-sm      (fn [v item] (gcomp/button-expand-child
                                        (str (split-date [(scm-start-term item) (scm-end-term item)])
                                             (if  (checked? (:v item)) "" (str " / " (scm-money item) " UAH")))
                                        :onClick (open-periods-contract-fn state! v)))

        serv-month-exp    (fn [v]  (seesaw.core/vertical-panel ;; panel with all expand-child (all periods) of one contract
                                    :items (doall (map (fn [item] (mig-hor (exp-panel-sm v item) "#fff" (:v item))) v)))) 

        exp-panel-sc      (fn [k v] (gcomp/button-expand
                                     (str (split-date [(sc-start-term k) (sc-end-term k)])
                                          (if (checked-box? (:v k) @checkboxes)
                                            "" (str " / " (calc-payment-for-service-contr k v) " UAH")))
                                     (serv-month-exp v)))
        
        exp-panel-entr    (fn [k v] (gcomp/button-expand
                                     (str (entr-name k) (if (checked-box? (:v k) @checkboxes)
                                                          "" (str " / "  (calc-payment-for-enterpreneuer k v) " UAH")))
                                     v-pan))
        
        serv-contract-exp (fn [k v] (.add v-pan (mig-hor (exp-panel-sc k v) "#fff" (:v k))))

        enterpr-exp       (fn [k v] (.add pan (mig-hor (exp-panel-entr k v) "#fff" (:v k))))]
    
    ((fn btn [[k v]] ;; recursion for build all panels
       (if-not (map? v) (serv-contract-exp k v)
               (do (enterpr-exp k v)
                   (doall (map btn v))))) plugin-m) pan))

;;;;;;;;;;;;;;;; 
;;; REQUIRES ;;; 
;;;;;;;;;;;;;;;;
(defn- insert-contract [state! calndr-start calndr-end price-input select-box]
  (let [date1         (seesaw.core/text calndr-start)
        date2         (seesaw.core/text calndr-end)
        price         (seesaw.core/text price-input)
        entpr         (seesaw.core/selection select-box)
        enterpreneurs (:all-enterpreneurs (state!))
        date-to-obj   (fn [data-string] (.parse (java.text.SimpleDateFormat. "dd-MM-YYYY") data-string))
        id-entr       (:id (first (filter (fn [x] (= (:name x) entpr)) enterpreneurs)))]
    (if-let [alerts (not-empty (cond-> []
                                 (empty? entpr)
                                 (conj (fn [] (i/warning "Invalid enterpreneur" "Enterpreneur field must be not empty"))) ;; TODO: dynamic lang
                                 (or (empty? price) (not (isNumber? price)))
                                 (conj (fn [] (i/warning "Invalid price" "Price must be number"))) ;; TODO: dynamic lang
                                 (or (empty? date1) (empty? date2) (req/data-comparator-old-new (date-to-obj date1) (date-to-obj date2)))
                                 (conj (fn [] (i/warning "Invalid date" "Date fields must be not empty, start day must be older")))))] ;; TODO: dynamic lang
      (doall (map (fn [invoke-alert] (invoke-alert)) alerts))
      (let [ins-req (req/insert-all id-entr (date-to-obj date1) (date-to-obj date2) (read-string price))]
        (if ins-req
          (i/success "Success" "Added service contract") ;; TODO: dynamic lang
          (i/danger  "Error with sql require" ins-req)))))) ;; TODO: dynamic lang

(defn- update-contracts [state!]
  (let [{:keys [update-service-month select-group-data]} (:plugin-toolkit (state!))
        checkboxes  (doall (filter (fn [item] (= (last item) true)) @(:checkboxes (state!))))]
    (if (empty? checkboxes)
      (i/warning "Messege" "Please select checkbox for payment") ;; TODO: dynamic lang
      (do (update-service-month checkboxes)
          (let [{tv  :tree-view
                 tip :tree-index-paths} (select-group-data)]
            (swap! (state! :atom) merge {:tip tip :checkboxes  (atom (vec (filter (fn [item] (= (last item) false)) tip)))})
            (refresh-data-and-panel state!)
            (i/success "Messege" "Payments are success"))))))  ;; TODO: dynamic lang

;;;;;;;;;;;;;;;;;;;;;;;
;;; Panels for view ;;;
;;;;;;;;;;;;;;;;;;;;;;;
(defn- add-contract-panel
  "Description
    in main-panel with all contracts add to insert-space panel with fields for input data"
  [state!]
  (let [{:keys [insert-space]}  (state!)
        label-fn      (fn [text] (seesaw.core/label :text text :font (gtool/getFont 16)
                                                    :border (seesaw.border/empty-border :right 8)))
        calndr-start  (calndr/get-calendar (gcomp/input-text :args [:columns 24]))
        calndr-end    (calndr/get-calendar (gcomp/input-text :args [:columns 24]))
        price-input   (gcomp/input-text :args [:columns 16])
        enterpreneurs (:all-enterpreneurs (state!))
        select-box    (gcomp/select-box (vec (map (fn [item] (:name item)) enterpreneurs)))] 
    (seesaw.mig/mig-panel :constraints ["wrap 3" "10px[]10px" "10px[]10px"]
                          :items (gtool/join-mig-items
                                  (seesaw.core/horizontal-panel
                                   :items (list (label-fn "enterpr:") select-box))  ;;; TO DO change this panel for more dynamic, look on labels, i mean SPACE ("price:      ") ;; TODO: dynamic lang
                                  (seesaw.core/horizontal-panel
                                   :items (list (label-fn "start term:") calndr-start));; TODO: dynamic lang
                                  (gcomp/menu-bar
                                   {:buttons [["Add";; TODO: dynamic lang
                                               (gs/icon GoogleMaterialDesignIcons/LIBRARY_ADD)
                                               (fn [e] (insert-contract state! calndr-start calndr-end price-input select-box))]
                                              ["Cancel";; TODO: dynamic lang
                                               (gs/icon GoogleMaterialDesignIcons/CLOSE)
                                               (fn [e] (seesaw.core/config! insert-space :items [[(seesaw.core/label)]]))]]})
                                  (seesaw.core/horizontal-panel
                                   :items (list (label-fn "price:    ") price-input));; TODO: dynamic lang
                                  (seesaw.core/horizontal-panel
                                   :items (list (label-fn "end term:  ") calndr-end))))));; TODO: dynamic lang

(defn- create-period-view
  "Description
    entry func for build main panel with all enterpreneurs and contracts"
  [state! dispatch!]
  (let [{:keys [select-group-data select-enterpreneurs]} (:plugin-toolkit (state!))
        {tv  :tree-view
         tip :tree-index-paths} (select-group-data)
        checkboxes         (atom (vec (filter (fn [item] (= (last item) false)) tip)))   
        all-enterpreneurs  (select-enterpreneurs)
        bln-checkboxes-fn  (fn [bln] (swap! (state! :atom) merge {:checkboxes (atom (vec (map (fn [item] (conj (vec (butlast item)) bln)) @checkboxes)))}))
        insert-space       (seesaw.mig/mig-panel :constraints ["wrap 1" "0px[fill, grow]0px" "0px[top]0px"])
        btn-panel          (gcomp/menu-bar
                            {:buttons [["Add cotract" ;; TODO: dynamic language
                                        (gs/icon GoogleMaterialDesignIcons/NOTE_ADD)
                                        (fn [e] (seesaw.core/config!
                                                 insert-space
                                                 :items [[(seesaw.core/vertical-panel
                                                           :items (list (seesaw.core/label
                                                                         :text "Some informotion about plugin ...........")))]
                                                         [(add-contract-panel state!)]]))]
                                       ["Select all" ;; TODO: dynamic language
                                        (gs/icon GoogleMaterialDesignIcons/CHECK_BOX)
                                        (fn [e] (bln-checkboxes-fn true)
                                          (refresh-data-and-panel state!))]

                                       ["Pay" ;; TODO: dynamic language
                                        (gs/icon GoogleMaterialDesignIcons/MONETIZATION_ON)
                                        (fn [e] (update-contracts state!))]
                                       
                                       ["Refresh" ;; TODO: dynamic language
                                        (gs/icon GoogleMaterialDesignIcons/CACHED)
                                        (fn [e] (bln-checkboxes-fn false) (refresh-data-and-panel state!))]
                                       ]})
        exp-panel          (seesaw.core/vertical-panel)
        view-space         (gmg/migrid :v [btn-panel insert-space (seesaw.core/border-panel :center exp-panel)])]

    (let [sup-map {:all-enterpreneurs all-enterpreneurs
                   :btn-panel         btn-panel
                   :tip               tip
                   :view-space        view-space
                   :exp-panel         exp-panel
                   :insert-space      insert-space
                   :checkboxes        checkboxes}]
      ;;(println "\nSup Map\n" all-enterpreneurs)
      (swap! (state! :atom) merge sup-map))
    
    (refresh-data-and-panel state!)
    (gmg/migrid :> (gcomp/min-scrollbox view-space :border nil))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Toolkit with SQl funcs ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn service-period-toolkit-pipeline
  "Description
    add to plugin-toolkit map with al requires to DB"
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
