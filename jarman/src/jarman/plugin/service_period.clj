(ns jarman.plugin.service-period
  (:require
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
  [
   & {:keys [h-panel onClick listen border size halign font foreground cursor text args]
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
        auto-menu-hide      false
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


(defn create-period-view
  [state]
  (let [list-space (gcomp/vmig :args [:border (b/line-border :left 1 :right 1 :color "#eee")])
        view-space (gcomp/vmig)
        list-space (c/config! list-space :items (gtool/join-mig-items (create-period--period-companys-list list-space view-space create-period--period-companys-list state)))]
    (gcomp/hmig
     :hrules "[shrink 0, fill]0px[grow, fill]"
     :items [[list-space]
             [view-space]]
     :args  [:background "#fff"])))

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



