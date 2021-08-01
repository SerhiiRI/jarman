(ns jarman.plugin.service-period
  (:require
   [jarman.logic.state :as state]
   [jarman.tools.lang :refer :all] 
   [clojure.string :as string]
   [clojure.spec.alpha :as s]
   [seesaw.util :as u]
   [seesaw.core :as c]
   [seesaw.mig :as mig]
   [seesaw.border :as b]
   [seesaw.swingx :as swingx]
   [seesaw.chooser :as chooser]
   [jarman.tools.swing :as stool]
   [jarman.gui.gui-tools :as gtool]
   [jarman.gui.gui-seed :as gseed]
   [jarman.gui.gui-components :as gcomp]
   [jarman.logic.session :as session]
   [jarman.logic.sql-tool :refer [select!]]
   [jarman.logic.connection :as db]
   [jarman.logic.metadata :as mt]
   [jarman.plugin.spec :as spec]
   [jarman.plugin.data-toolkit :as query-toolkit]
   [jarman.resource-lib.icon-library :as icon]
   [jarman.plugin.service-period-requires :as req]))

;;;;;;;;;;;;;;;;;;;;;;;;
;;; HELPER FUNCTIONS ;;;
;;;;;;;;;;;;;;;;;;;;;;;;
(defn- refresh-panel
  ([panel] (doto panel (.revalidate) (.repaint)))
  ([panel content] (doto panel (.removeAll) (.add content) (.revalidate) (.repaint))))
(defn- format-date [date]
  (let [fn-format (fn [date] (.format (java.text.SimpleDateFormat. "dd-MM-YYYY") date))]
    (if (or (vector? date)(seq? date))
      (map (fn [d] (fn-format d)) date)
      (fn-format date))))
(defn- split-date [date-vec]
  (apply str (interpose " / " (format-date date-vec))))

;;;;;;;;;;;;;;;;;;
;;; GUI Panels ;;;
;;;;;;;;;;;;;;;;;;
(defn- panel-one-period [model-data]
  (let [label-fn     (fn [text foreground] (seesaw.core/label :text (str text)
                                                              :font (gtool/getFont 16)
                                                              :foreground foreground))
        {st-date         :service_contract_month.service_month_start 
         end-date        :service_contract_month.service_month_end
         payment-st      :service_contract_month.was_payed
         payment         :service_contract_month.money_per_month}  model-data
        date         (str st-date " / " end-date)
        status       (if payment-st "It was payed, you cannot change this field" "Waiting for payment")
        panel        (seesaw.mig/mig-panel :constraints ["wrap 2" "5px[]30px" "10px[]10px"]
                                           :items [[(label-fn "period:" gcomp/dark-grey-color)]
                                                   [(label-fn date gcomp/blue-color)]
                                                   [(label-fn "payment:" gcomp/dark-grey-color)]
                                                   [(if payment-st (label-fn payment gcomp/blue-color)
                                                        (gcomp/input-text :args [:columns 7]))]
                                                   [(label-fn "payment status:" gcomp/dark-grey-color)]
                                                   [(label-fn status gcomp/blue-color)]])]
    panel))


(defn- panel-one-contract-periods [data]
  (let [btn-panel (gcomp/menu-bar
                   {:justify-end true
                    :buttons [[" Save"
                               icon/agree1-blue-64-png
                               (fn [e])]
                              [" Revert changes"
                               icon/refresh-blue1-64-png
                               (fn [e])]
                              [" Export ODT"
                               icon/txt-64-png
                               (fn [e])]
                              [" Export CSV"
                               icon/csv-64-png
                               (fn [e])]]})
        panel     (seesaw.mig/mig-panel :constraints ["wrap 1" "15px[]15px" "15px[]15px"]
                                        :items [[btn-panel]])]
    (doall (map (fn [period] (.add panel (panel-one-period period))) data))
    (gcomp/min-scrollbox panel :hscroll :never)))

(defn- open-periods-contract-fn [panel select-fn id]
  (fn [e] (c/config! panel :items (gtool/join-mig-items
                                   (panel-one-contract-periods
                                    (select-fn id))))))

(defn gener-label
  "Description
    h-panel -. component, on which we change color on event"
  [& {:keys [h-panel onClick listen border size halign font foreground cursor text args]
      :or {h-panel     nil
           onClick     (fn [e])
           listen      [:mouse-entered  (fn [e] (seesaw.core/config! h-panel :background "#cecece"))
                        :mouse-exited   (fn [e] (seesaw.core/config! h-panel :background gcomp/light-light-grey-color))
                        :mouse-clicked  onClick]
           border      (b/line-border :left 1 :right 1 :top 1 :bottom 1 :color "#bbb" :thickness 0.5)
           size        [100 :by 44]
           halign      :center
           font        (gtool/getFont 16)
           foreground  gcomp/blue-color 
           text        ""
           args        []
           cursor      :hand}}]
  (seesaw.core/label
   :text        text
   :cursor      cursor
   :foreground  foreground
   :font        font
   :halign      halign 
   :size        size
   :border      border
   :listen      listen))

(defn panel-agenda-contracts [view-space company-id state]
  (let [{{:keys [select-service_contract-fn select-service_contract-info-fn]} :plugin-toolkit}  @state
        data-model (select-service_contract-fn company-id) 
        btn-panel (gcomp/menu-bar
                   {:justify-end true
                    :buttons [[" Add cotract"
                               icon/plus-64-png
                               (fn [e])]
                              [" Delete contract"
                               icon/basket-blue1-64-png
                               (fn [e])]]})
        onClick-fn  (fn [data] (open-periods-contract-fn view-space select-service_contract-info-fn (:service_contract.id data)))
        panel (seesaw.mig/mig-panel
               :constraints ["wrap 1" "5px[]0px" "0px[]0px"] :items  [[(let [h-panel (seesaw.core/horizontal-panel)]
                                                                         (seesaw.core/config! h-panel
                                                                                              :background gcomp/light-light-grey-color
                                                                                              :items (list
                                                                                                      (gener-label :text "Start term" :cursor :default)
                                                                                                      (gener-label :text "End term" :cursor :default))) h-panel)]])
        mig   (seesaw.mig/mig-panel
               :constraints ["wrap 1" "0px[]0px" "1px[]0px"]
               :items [[btn-panel]
                       [panel]])
        
        date  (doall (map (fn [data]
                            (.add panel
                                  (let [h-panel (seesaw.core/horizontal-panel)]
                                    (seesaw.core/config! h-panel 
                                                         :background gcomp/light-light-grey-color
                                                         :focusable? true
                                                         :items (list (gener-label :h-panel h-panel :text (format-date (:service_contract.contract_start_term data)) :onClick (onClick-fn data))
                                                                      (gener-label :h-panel h-panel :text (format-date (:service_contract.contract_end_term data)) :onClick (onClick-fn data)))) h-panel))) data-model))]
    (println "DATA MODEL" data-model)
    (refresh-panel panel)
    mig))

;;(split-date ((juxt :service_contract.contract_start_term :service_contract.contract_end_term) data))

;;;;;;;;;;;;;;;;;;;;;;;;
;;; Logic for Panels ;;;
;;;;;;;;;;;;;;;;;;;;;;;;
(defn create-period--period-list
  [list-space view-space return-fn periods-model state]
  (let [{{:keys [select-enterpreneur-fn select-service_contract-info-fn]} :plugin-toolkit}  @state
        auto-menu-hide            false
        return (gcomp/button-slim (str "...")
                                  :underline-size 1
                                  :onClick (fn [e] (c/invoke-later (do
                                                                     (.removeAll view-space)
                                                                     (refresh-panel view-space)
                                                                     (c/config! list-space :items (gtool/join-mig-items (return-fn list-space view-space return-fn state)))
                                                                     (gtool/switch-focus)))))
        panel   (gcomp/vmig)
        periods (gcomp/expand-form-panel
                 list-space
                 [return
                  (gcomp/min-scrollbox panel :hscroll :never)]
                 :max-w 180
                 :args [:id :expand-panel :background "#fff"])]
    (doall (map (fn [period] (.add panel (gcomp/button-slim (split-date [(:service_contract.contract_start_term period) 
                                                                         (:service_contract.contract_end_term period)])
                                                            :onClick (open-periods-contract-fn view-space select-service_contract-info-fn (:service_contract.id period))))) periods-model))
    (gtool/set-focus return)
    return
    periods)) 

(defn create-period--period-companys-list
  [list-space view-space return-fn state] ;; [{:name "Frank & Franky" :id 3}]
  (gtool/rm-focus)
  (let
      [{{:keys [select-enterpreneur-fn select-enterpreneur-info-fn select-service_contract-fn]} :plugin-toolkit}  @state
       model     (select-enterpreneur-fn)
       buttons   (if (empty? model) (seesaw.core/label :text "[!] Empty db, no enterpreneurs" :foreground gcomp/blue-color) 
                     (map (fn [enterpreneur]
                            (let [name           (:name enterpreneur) ;;name
                                  company-id     (get enterpreneur :id)
                                  view-fn        (fn [e]
                                                   (c/config! view-space :items [[(panel-agenda-contracts view-space company-id state)]])
                                                   (c/invoke-later
                                                    (do (c/config! list-space :items (gtool/join-mig-items
                                                                                      (create-period--period-list list-space view-space return-fn  (select-service_contract-fn company-id) state)))
                                                        (gtool/switch-focus))))
                                  btn (gcomp/button-slim name
                                                         :onClick view-fn
                                                         :args [:tip name])]
                              (gtool/set-focus-if-nil btn)
                              btn)) model))]
    (gcomp/expand-form-panel
     list-space
     (gcomp/min-scrollbox
      (gcomp/vmig :items (gtool/join-mig-items buttons)) :hscroll :never)
     :max-w 180
     :focus-color "#444"
     :args [:background "#fff"])))


;; (defn create-period-view
;;   [state]
;;   (let [list-space (gcomp/vmig :args [:border (b/line-border :left 1 :right 1 :color "#eee")])
;;         view-space (gcomp/vmig)
;;         list-space (c/config! list-space :items (gtool/join-mig-items (create-period--period-companys-list list-space view-space create-period--period-companys-list state)))]
;;     (gcomp/hmig
;;      :hrules "[shrink 0, fill]0px[grow, fill]"
;;      :items [[list-space]
;;              [view-space]]
;;      :args  [:background "#fff"])))

(defn- expand-colors []
  [["#00fa9a" "#61ffc2"]
   ["#79d1c4" "#9aefdf"]
   ["#67a39a" "#9ae3d8"]
   ;;["#eeeeee" "#eeefff"]
   ;;["#7fff00" "#c2ff85"]    
    ]) 

(defn- part-expand [k item color width]
  (gcomp/button-expand
   (str k)
   item
   ;;:left width
   :min-height 800
   :background color))

(defn- part-button [k color width]
  (gcomp/button-expand-child
   (str k)
   ;;:left 0
   :width width
   :hover-color color))

(seesaw.dev/show-options  (seesaw.core/checkbox))



(def checkboxes (atom [[1 2 1 true] [1 2 3 false] [1 1 3 true] [2 2 1 true]]))

(defn- comparator-fn [id-vec]
  (let [[_e _sc _scm] id-vec]
    (case (count id-vec)
      1 (fn [[e]] (= e _e))
      2 (fn [[e sc]] (and (= e _e) (= sc _sc)))
      3 (fn [[e sc scm]] (and (= e _e) (= sc _sc) (= scm _scm)))
      false)))

(defn- update-checkboxes [id-vec check?]
  (let [cmpr? (comparator-fn id-vec)]
    (reduce (fn [acc period] (if (cmpr? period)
                               (conj acc (assoc period 3 check?))
                               (conj acc period))) [] @checkboxes)))

(defn- checked-all? [id-vec]
  (let [cmpr? (comparator-fn id-vec)]
    (reduce (fn [acc period] (if (cmpr? period)
                               (and acc (last period))
                               acc)) true @checkboxes)))

(defn- choice-checkbox [id-vec]
  (println "ID_VEC" id-vec))

(defn checked-box? []
  (some (fn [period] (= (nth period 3) false)) checkboxes))

(defn build-expand-by-map
  "Description:
    Build list of recursive button expand using configuration map."
  [plugin-m & {:keys [lvl] :or {lvl 0}}]
  (let [v-pan        (seesaw.core/vertical-panel)
        pan          (seesaw.core/vertical-panel)
        get-color    (fn [lvl] (nth (expand-colors) lvl))
        color-vec-1  (get-color lvl)
        color-vec-2  (get-color (+ 1 lvl))
        mig-hor      (fn [item color id-vec] (seesaw.core/border-panel
                                       :background color
                                       :west   (seesaw.core/flow-panel :background "#fff"
                                                                       :size [25 :by 25]
                                                                       :items (list
                                                                               (seesaw.core/checkbox
                                                                                :listen [:mouse-clicked
                                                                                         ;;;TO DOOOO
                                                                                         (fn [e] (choice-checkbox id-vec))]
                                                                                :background "#fff")))
                                       :east   (seesaw.core/flow-panel :background "#fff"
                                                                       :size [80 :by 25])
                                       :center (seesaw.mig/mig-panel :constraints ["wrap 1" "0px[:820]0px" "0px[]0px"]
                                                                     :background (first color-vec-2)
                                                                     :items [[item]])))]
    (doall (map (fn btn [[k v]]
                    (if-not (map? v)
                      (do
                          (.add v-pan (mig-hor (part-expand (:name k)
                                                            (seesaw.core/vertical-panel
                                                             :items (doall (map (fn [item]
                                                                                  (do
                                                                                      (mig-hor (part-button (:name item) "#e3fffb"
                                                                                                            800) "#fff" (:id item)))) v)))
                                                            (second color-vec-2) 100) "#fff" (:id k))))
                      (do
                        (.add pan
                              (mig-hor (part-expand (:name k) v-pan (first color-vec-2) 200) "#fff" (:id k)))
                        (btn (first v))))) plugin-m))
    (println  (.width (.getMaximumSize pan)))
    pan))

(reset! (atom []) "d")
(swap! (atom []) conj "d")




;; (map (fn btn [[k v]]
;;        (if-not (map? v) (do (println (:name k))  (map (fn [i] (println i)) v)) 
;;            (do 
;;              (println (:name k))
;;                (btn (first v))  ))) {{:name "solomon" :id 2} {{:name "contract" :id 3} ["a" "b" "c"]}})

(defn create-period-view
  [state]
  (let [btn-panel (gcomp/menu-bar
                   {:justify-end true
                    :buttons [[" Add cotract"
                               icon/plus-64-png
                               (fn [e])]
                              [" Delete contract"
                               icon/basket-blue1-64-png
                               (fn [e])]]})
        view-space (seesaw.mig/mig-panel :constraints ["wrap 1" "0px[fill, grow]0px" "0px[top]0px"]
                                         :items [[btn-panel]])]
    (doall
     (map (fn [item] (.add view-space (build-expand-by-map item :lvl 1))) [{{:name "solom" :id [3]} {{:name "cotract" :id [3 4]} [{:name "a"
                                                                                                                             :id [3 4 7]}
                                                                                                                            {:name"b"
                                                                                                                             :id [3 4 6]}
                                                                                                                            {:name "c"
                                                                                                                             :id [3 4 5]}]}} {{:name "solomon" :id [2]} {{:name "contract" :id [2 3]}  [{:name "d" :id [2 3 4]} {:name "ddd" :id [2 3 5]} {:name "dd" :id [2 3 6]}]}}]))
    (.revalidate view-space)
    (.repaint view-space)
    (println "parent..." (.width (.getMaximumSize view-space)))
    (gcomp/hmig
     :hrules "[shrink 0, fill]0px[grow, fill]"
     :items [[view-space]]
     :args  [:background "#fff"])))


;; (defn create-period-view
;;   [state]
;;   (let [list-space (gcomp/vmig :args [:border (b/line-border :left 1 :right 1 :color "#eee")])
;;         view-space (gcomp/vmig)
;;         list-space (c/config! list-space :items (gtool/join-mig-items (create-period--period-companys-list list-space view-space create-period--period-companys-list state)))]
;;     (gcomp/hmig
;;      :hrules "[shrink 0, fill]0px[grow, fill]"
;;      :items [[list-space]
;;              [view-space]]
;;      :args  [:background "#fff"])))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Toolkit with SQl funcs ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn service-period-toolkit-pipeline [configuration]
  {:select-enterpreneur-info-fn
   (fn [enterpreneur-id] (req/info-for-enterpreneur enterpreneur-id))
   :select-enterpreneur-fn 
   (fn [] (db/query (select! {:table_name :enterpreneur,
                              :column [:enterpreneur.id
                                       :enterpreneur.name]})))
   :select-service_contract-info-fn
   (fn [service_contract-id] (req/info-for-service_contract service_contract-id))
   :select-service_contract-fn
   (fn [enterpreneur-id] (db/query (select! {:table_name :service_contract,
                                             :inner-join
                                             [:service_contract->enterpreneur],
                                             :column
                                             [:#as_is
                                              :service_contract.id
                                              :service_contract.contract_start_term
                                              :service_contract.contract_end_term]
                                             :where [:= enterpreneur-id :id_enterpreneur]})))})

;;;;;;;;;;;;;
;;; Entry ;;;
;;;;;;;;;;;;;
(defn service-period-entry [plugin-path global-configuration-getter]
  (create-period-view
   (atom {:plugin-path          plugin-path
          :plugin-global-config global-configuration-getter
          :plugin-config        (get-in (global-configuration-getter) (conj plugin-path :config) {})
          :plugin-toolkit       (get-in (global-configuration-getter) (conj plugin-path :toolkit) {})}))) 



{{:enterpreneur.director nil,
  :enterpreneur.id 1,
  :enterpreneur.ownership_form "eeeeee",
  :enterpreneur.accountant nil,
  :enterpreneur.vat_certificate "weded",
  :enterpreneur.physical_address nil,
  :enterpreneur.name "Solomon",
  :enterpreneur.contacts_information nil,
  :enterpreneur.individual_tax_number "23323",
  :enterpreneur.ssreou "222333",
  :enterpreneur.legal_address nil}
 {{:service_contract.id 1,
   :service_contract.contract_start_term
   #inst "2021-10-11T22:00:00.000-00:00",
   :service_contract.contract_end_term
   #inst "2021-12-19T23:00:00.000-00:00"}
  [{:service_contract_month.id 1,
    :service_contract_month.service_month_start
    #inst "2021-09-12T22:00:00.000-00:00",
    :service_contract_month.service_month_end
    #inst "2021-09-29T22:00:00.000-00:00",
    :service_contract_month.money_per_month 69.0,
    :service_contract_month.was_payed false}
   {:service_contract_month.id 2,
    :service_contract_month.service_month_start
    #inst "2021-09-30T22:00:00.000-00:00",
    :service_contract_month.service_month_end
    #inst "2021-10-30T22:00:00.000-00:00",
    :service_contract_month.money_per_month 400.0,
    :service_contract_month.was_payed true}
   {:service_contract_month.id 3,
    :service_contract_month.service_month_start
    #inst "2021-10-31T23:00:00.000-00:00",
    :service_contract_month.service_month_end
    #inst "2021-11-12T23:00:00.000-00:00",
    :service_contract_month.money_per_month 399.0,
    :service_contract_month.was_payed false}
   {:service_contract_month.id 4,
    :service_contract_month.service_month_start
    #inst "2021-11-30T23:00:00.000-00:00",
    :service_contract_month.service_month_end
    #inst "2021-12-14T23:00:00.000-00:00",
    :service_contract_month.money_per_month 400.0,
    :service_contract_month.was_payed false}],
  {:service_contract.id 5,
   :service_contract.contract_start_term
   #inst "2021-09-12T22:00:00.000-00:00",
   :service_contract.contract_end_term
   #inst "2021-12-14T23:00:00.000-00:00"}
  [{:service_contract_month.id 5,
    :service_contract_month.service_month_start
    #inst "2021-09-12T22:00:00.000-00:00",
    :service_contract_month.service_month_end
    #inst "2021-09-29T22:00:00.000-00:00",
    :service_contract_month.money_per_month 400.0,
    :service_contract_month.was_payed false}
   {:service_contract_month.id 6,
    :service_contract_month.service_month_start
    #inst "2021-09-30T22:00:00.000-00:00",
    :service_contract_month.service_month_end
    #inst "2021-10-30T22:00:00.000-00:00",
    :service_contract_month.money_per_month 400.0,
    :service_contract_month.was_payed false}
   {:service_contract_month.id 7,
    :service_contract_month.service_month_start
    #inst "2021-10-31T23:00:00.000-00:00",
    :service_contract_month.service_month_end
    #inst "2021-11-29T23:00:00.000-00:00",
    :service_contract_month.money_per_month 400.0,
    :service_contract_month.was_payed false}
   {:service_contract_month.id 8,
    :service_contract_month.service_month_start
    #inst "2021-11-30T23:00:00.000-00:00",
    :service_contract_month.service_month_end
    #inst "2021-12-14T23:00:00.000-00:00",
    :service_contract_month.money_per_month 400.0,
    :service_contract_month.was_payed false}]},
 {:enterpreneur.director nil,
  :enterpreneur.id 2,
  :enterpreneur.ownership_form nil,
  :enterpreneur.accountant nil,
  :enterpreneur.vat_certificate nil,
  :enterpreneur.physical_address nil,
  :enterpreneur.name "Bukinist",
  :enterpreneur.contacts_information nil,
  :enterpreneur.individual_tax_number nil,
  :enterpreneur.ssreou "232",
  :enterpreneur.legal_address nil}
 {{:service_contract.id 6,
   :service_contract.contract_start_term
   #inst "2021-08-31T22:00:00.000-00:00",
   :service_contract.contract_end_term
   #inst "2021-09-24T22:00:00.000-00:00"}
  [{:service_contract_month.id 9,
    :service_contract_month.service_month_start
    #inst "2021-08-31T22:00:00.000-00:00",
    :service_contract_month.service_month_end
    #inst "2021-08-31T22:00:00.000-00:00",
    :service_contract_month.money_per_month 200.0,
    :service_contract_month.was_payed false}]}}

