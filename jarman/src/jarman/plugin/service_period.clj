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
   [jarman.resource-lib.icon-library :as icon]))

(defn- panel-one-period [model-data]
  (let [label-fn     (fn [text foreground] (seesaw.core/label :text (str text)
                                                              :font (gtool/getFont 16)
                                                              :foreground foreground))
        {st-date         :service_month_start 
         end-date        :service_month_end
         payment-st      :service_contract_month.was_payed
         payment         :money_per_month}  model-data
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
    (gcomp/min-scrollbox panel :hscroll :never)))


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
    (.revalidate panel)
    (.repaint panel)   
    panel))


 
(defn create-period--period-form
  []
  (gcomp/vmig
   :vrules "[fill][100, shrink 0, fill][grow, fill]"
   :items [[(gcomp/header-basic "Okresy")]
           [(gcomp/min-scrollbox
             (mig/mig-panel :constraints ["wrap 4" "10px[fill][fill]50px[fill][fill]10px" "10px[fill]10px"]
                        :items [[(c/label :text "Organization:")]
                                [(c/label :text "Frank & Franky Co." :border (b/line-border :bottom 1 :color "#494949"))]
                                [(c/label :text "Time:")]
                                [(c/label :text "12/03/2021 - 11/03/2022"  :border (b/line-border :bottom 1 :color "#494949"))]
                                
                                [(c/label :text "Customer:")]
                                [(c/label :text "Franklyn Badabumc" :border (b/line-border :bottom 1 :color "#494949"))]
                                [(c/label :text "Full amount:")]
                                [(c/label :text "7000,-" :border (b/line-border :bottom 1 :color "#494949"))]
                                
                                [(c/label :text "Service:")]
                                [(c/label :text "Mr. Jarman" :border (b/line-border :bottom 1 :color "#494949"))]])
             :vscroll :never)]
           [(gcomp/vmig
             :vrules "[fill]0px[grow, fill]"
             :items [[(gcomp/menu-bar
                       {:justify-end true
                        :id :menu-for-period-table
                        :buttons [[(gtool/get-lang-btns :save) icon/agree1-blue-64-png "" (fn [e])]
                                  [(gtool/get-lang-btns :export) icon/excel-64-png "" (fn [e])]]})]
                     [(c/scrollable (seesaw.swingx/table-x :model [:columns ["Servise month" "Amount" "Payment status"] :rows [["03/2021" "2500,-" "FV: 042/03/2021"]
                                                                                                                             ["04/2021" "2000,-" "FV: 042/04/2021"]
                                                                                                                             ["05/2021" "2500,-" "Expected payment"]]]))]])]]))

(defn get-period-list
  [company-id]
  (cond
    (= company-id 1) {}))

(defn create-period--period-list
  [list-space view-space return-fn select-fn state company-id]
  (let [;; period-list (get-period-list company-id)
        periods-model (select-fn company-id)
        ;; start-term    (:service_contract.contract_start_term periods-model)
        ;; end-term      (:service_contract.contract_end_term periods-model)
        auto-menu-hide false
        return (gcomp/button-slim (str "<< " (gtool/get-lang-btns :back))
                                  :underline-size 1
                                  :onClick (fn [e] (c/invoke-later (do
                                                                     (c/config! list-space :items (gtool/join-mig-items (return-fn list-space view-space return-fn state)))
                                                                     (gtool/switch-focus)))))
        date-fn  (fn [date]  (.format (java.text.SimpleDateFormat. "dd-MM-YYYY") date))
        panel (gcomp/vmig)
        periods (gcomp/expand-form-panel
                 list-space
                 [return
                  (gcomp/min-scrollbox panel
                                       :hscroll :never)]
                 :max-w 180
                 :args [:id :expand-panel :background "#fff"])]
    (doall (map (fn [period] (.add panel (gcomp/button-slim (str (date-fn (:service_contract.contract_start_term period)) " / "
                                                                 (date-fn (:service_contract.contract_end_term period)))
                                                            :onClick (fn [e] (c/config! view-space :items (gtool/join-mig-items
                                                                                                           (panel-one-contract-periods
                                                                                                            [{:id_service_contract 2
                                                                                                              :service_month_start "2020-09-30"
                                                                                                              :service_month_end "2021-09-30"
                                                                                                              :money_per_month 400
                                                                                                              :service_contract_month.was_payed false}

                                                                                                             {:id_service_contract 3
                                                                                                              :service_month_start "2020-09-28"
                                                                                                              :service_month_end "2018-02-12"
                                                                                                              :money_per_month 600
                                                                                                              :service_contract_month.was_payed true}]
                                                                                                            ))))))) periods-model))
    (gtool/set-focus return)
    return
    periods))

;; (defn get-company-list
;;   [select-fn]
;;   (println "SELECT >>" select-fn)
;;   [{:name "Trashpanda-Team" :id 1} {:name "Frank & Franky" :id 3}
;;    {:name "Trashpanda-Team" :id 1} {:name "Trashpandowe Zakłady Wyrobów Kodowych Team" :id 3}
;;    {:name "Trashpanda-Team" :id 1} {:name "Frank & Franky" :id 3}])

(defn create-period--period-companys-list
  [list-space view-space return-fn state] ;; [{:name "Frank & Franky" :id 3}]
  (gtool/rm-focus)
  (let
      [{{select-enterpreneurs-fn :enterpreneur-query
         select-service-contr-fn :service_contract-query} :plugin-toolkit}  @state
       model (select-enterpreneurs-fn)
       buttons (map (fn [enterpreneur]
                      (let [name (:ssreou enterpreneur) ;;name
                            btn (gcomp/button-slim name
                                                   :onClick (fn [e] (c/invoke-later
                                                                     (do (c/config! list-space :items (gtool/join-mig-items
                                                                                                       (create-period--period-list list-space view-space return-fn select-service-contr-fn state (get enterpreneur :id))))
                                                                         (gtool/switch-focus))))
                                                   :args [:tip name])]
                        (gtool/set-focus-if-nil btn)
                        btn)) model)]
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

(defn service-period-toolkit-pipeline [configuration]
  {:enterpreneur-query
   (fn [] (db/query (select! {:table_name :enterpreneur,
                              :column [:enterpreneur.id
                                       :enterpreneur.ssreou]})))
   :service_contract-query
   (fn [enterpreneur-id] (db/query (select! {:table_name :service_contract,
                                             :inner-join
                                             [:service_contract->enterpreneur],
                                             :column
                                             [:#as_is
                                              :service_contract.contract_start_term
                                              :service_contract.contract_end_term]
                                             :where [:= enterpreneur-id :id_enterpreneur]})))})

(defn service-period-entry [plugin-path global-configuration-getter]
  (create-period-view
   (atom {:plugin-path          plugin-path
          :plugin-global-config global-configuration-getter
          :plugin-config        (get-in (global-configuration-getter) (conj plugin-path :config) {})
          :plugin-toolkit       (get-in (global-configuration-getter) (conj plugin-path :toolkit) {})}))) 
