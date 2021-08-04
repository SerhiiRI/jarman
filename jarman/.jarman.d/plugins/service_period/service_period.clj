(ns jarman.plugins.service-period
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
   [jarman.gui.gui-calendar :as calndr]
   [jarman.logic.session :as session]
   [jarman.logic.sql-tool :refer [select!]]
   [jarman.logic.connection :as db]
   [jarman.logic.metadata :as mt]
   [jarman.plugin.spec :as spec]
   [jarman.plugin.plugin :refer :all]
   [jarman.plugin.data-toolkit :as query-toolkit]
   [jarman.resource-lib.icon-library :as icon]
   [jarman.plugins.service-period-requires :as req]))

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

;;;;;;;;;;;;;;;;;;;;;
;;; EXPAND PANELS ;;;
;;;;;;;;;;;;;;;;;;;;;
(defn- expand-colors []
  [["#00fa9a" "#61ffc2"]
   ["#79d1c4" "#9aefdf"]
   ["#67a39a" "#9ae3d8"]]) 

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

(defn- comparator-fn [id-vec]
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
;;; ADDD this whenn we wil have paid
;;; (swap! state merge {:tip (:tree-index-paths (select-group-data))})
(defn build-expand-by-map
  "Description:
      Build list of recursive button expand using configuration map."
  [plugin-m pan state]
  (let [{:keys [select-group-data]} (:plugin-toolkit @state)
        checkboxes      (:checkboxes @state)
        v-pan           (seesaw.core/vertical-panel)
        mig-hor         (fn [item color id-vec]
                          (seesaw.core/border-panel
                           :background color
                           :west   (seesaw.core/flow-panel :background "#fff"
                                                           :size [25 :by 25]
                                                           :items (list
                                                                   (if (checked-box? id-vec (:tip @state))
                                                                     (seesaw.core/label)
                                                                     (seesaw.core/checkbox
                                                                      :selected? (checked-box? id-vec @checkboxes)
                                                                      :listen [:mouse-clicked
                                                                               (fn [e]
                                                                                 (do
                                                                                   (update-checkboxes
                                                                                    id-vec
                                                                                    (.isSelected (.getComponent e)) checkboxes)
                                                                                   (.removeAll pan)
                                                                                   (build-expand-by-map plugin-m pan state)
                                                                                   (refresh-panel pan)))]
                                                                      :background "#fff"))))
                           :east   (seesaw.core/flow-panel :background "#fff"
                                                           :size [80 :by 25])
                           :center (seesaw.mig/mig-panel :constraints ["wrap 1" "0px[:820]0px" "0px[]0px"]
                                                         :background "#cecece"
                                                         :items [[item]])))]
    ((fn btn [[k v]]
       (if-not (map? v)
         (do (.add v-pan (mig-hor (part-expand (split-date [(:service_contract.contract_start_term k)
                                                            (:service_contract.contract_end_term k)])
                                               (seesaw.core/vertical-panel
                                                :items (doall (map (fn [item]
                                                                     (mig-hor (part-button (split-date [(:service_contract_month.service_month_start item)
                                                                                                        (:service_contract_month.service_month_end item)]) "#dddddd"
                                                                                           800) "#fff" (:v item))) v)))
                                               "#dddddd" 100) "#fff" (:v k))))
         (do (.add pan
                   (mig-hor (part-expand (:enterpreneur.name k) v-pan "#cecece" 200) "#fff" (:v k)))
             (doall (map btn v))))) plugin-m)
    pan))


(defn insert-contract [state calndr-start calndr-end price-input select-box]
  (let [{:keys [insert-space view-space exp-panel]}  @state
        date1         (seesaw.core/text calndr-start)
        date2         (seesaw.core/text calndr-end)
        price         (seesaw.core/text price-input)
        entpr         (seesaw.core/selection select-box)
        enterpreneurs (:all-enterpreneurs @state)
        date-to-obj   (fn [data-string] (.parse (java.text.SimpleDateFormat. "YYYY-MM-dd") data-string))
        id-entr       (:id (first (filter (fn [x] (= (:name x) entpr)) enterpreneurs)))]
    (if (some empty? [date1 date2 price entpr])
      ((state/state :alert-manager) :set {:header "Error" :body "All fields must be entered"} 5)
      (if (number? (read-string price)) 
        (do
          (req/insert-all id-entr (date-to-obj date1) (date-to-obj date2) (read-string price))
          (.removeAll exp-panel)
          (doall
           (map (fn [[entr srv_cntr]] (.add view-space (build-expand-by-map [entr srv_cntr] (seesaw.core/vertical-panel) state)))
                (:tree-view ((:select-group-data (:plugin-toolkit @state))))))
          (.removeAll insert-space)
          (refresh-panel view-space)
          ((state/state :alert-manager) :set {:header "Success" :body "Added service contract"} 5))
        ((state/state :alert-manager) :set {:header "Error" :body "Price must be number"} 5)))))

(defn update-contracts [state]
  (let [{:keys [update-service-month]} (:plugin-toolkit @state)
        {:keys [view-space exp-panel]} @state
        checkboxes  (doall (filter (fn [item] (= (last item) true)) @(:checkboxes @state)))]
    (println "FUNC" update-service-month)
    (update-service-month checkboxes) 
    (.removeAll exp-panel)
    (doall
     (map (fn [[entr srv_cntr]] (.add view-space (build-expand-by-map [entr srv_cntr] (seesaw.core/vertical-panel) state)))
          (:tree-view ((:select-group-data (:plugin-toolkit @state))))))
    (refresh-panel view-space)
    ((state/state :alert-manager) :set {:header "Messege" :body "Payments are success"} 5))) 

(defn add-contract-panel [state]
  (let [{:keys [insert-space]}  @state
        label-fn      (fn [text] (seesaw.core/label :text text :font (gtool/getFont 16)
                                                    :border (b/empty-border :right 8)))
        calndr-start  (calndr/get-calendar (gcomp/input-text :args [:columns 24]))
        calndr-end    (calndr/get-calendar (gcomp/input-text :args [:columns 24]))
        price-input   (gcomp/input-text :args [:columns 16])
        enterpreneurs (:all-enterpreneurs @state)
        select-box    (gcomp/select-box (vec (map (fn [item] (:name item)) enterpreneurs)))] ;; to do
    (seesaw.mig/mig-panel :constraints ["wrap 3" "10px[]10px" "10px[]10px"]
     :items (gtool/join-mig-items
             (seesaw.core/horizontal-panel
              :items (list (label-fn "enterpr:") select-box))
             (seesaw.core/horizontal-panel
              :items (list (label-fn "start term:") calndr-start))
             (gcomp/menu-bar
              {:buttons [[" OK"
                          icon/agree1-blue-64-png
                          (fn [e] (insert-contract state calndr-start calndr-end price-input select-box))]
                         [" NO"
                          icon/x-blue1-64-png
                          (fn [e] (seesaw.core/config! insert-space
                                                       :items [[(seesaw.core/label)]]))]]})
             (seesaw.core/horizontal-panel
              :items (list (label-fn "price:    ") price-input))
             (seesaw.core/horizontal-panel
              :items (list (label-fn "end term:  ") calndr-end))))))

(defn create-period-view
  [state]
  (let [{:keys [select-group-data select-enterpreneurs]} (:plugin-toolkit @state)
        {tv  :tree-view
         tip :tree-index-paths} (select-group-data)
        checkboxes         (atom (vec (filter (fn [item] (= (last item) false)) tip)))   
        all-enterpreneurs  (select-enterpreneurs)
        insert-space       (seesaw.mig/mig-panel :constraints ["wrap 1" "0px[fill, grow]0px" "0px[top]0px"])
        btn-panel          (gcomp/menu-bar
                            {:buttons [[" Add cotract"
                                        icon/plus-64-png
                                        (fn [e] (do (seesaw.core/config! insert-space :items [[(seesaw.core/vertical-panel :items (list (seesaw.core/label :text "Some informotion about plugin ...........")))]
                                                                                              [(add-contract-panel state)]])))]
                                       [" Pay"
                                        icon/connection-blue1-64-png
                                        (fn [e] (update-contracts state))]]})
        exp-panel          (seesaw.core/vertical-panel)
        view-space         (seesaw.mig/mig-panel :constraints ["wrap 1" "0px[fill, grow]0px" "0px[top]0px"]
                                                 :items [[btn-panel]
                                                         [insert-space]
                                                         [exp-panel]])]
    (swap! state merge {:all-enterpreneurs all-enterpreneurs :tip tip :view-space view-space :exp-panel exp-panel :insert-space insert-space :checkboxes checkboxes})
    (doall
     (map (fn [[entr srv_cntr]] (.add exp-panel (build-expand-by-map [entr srv_cntr] (seesaw.core/vertical-panel) state))) tv))
    (refresh-panel view-space)
    (gcomp/hmig
     :hrules "[shrink 0, fill]0px[grow, fill]"
     :items [[(seesaw.core/scrollable view-space :border nil)]]
     :args  [:background "#fff"])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Toolkit with SQl funcs ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn service-period-toolkit-pipeline [configuration]
  {:select-group-data      req/info-grouped-query
   :update-service-month   req/update-service-month-to-payed
   :select-enterpreneurs   (fn [] (db/query (select! {:table_name :enterpreneur,
                                                      :column [:enterpreneur.id
                                                               :enterpreneur.name]})))})
;;;;;;;;;;;;;
;;; Entry ;;;
;;;;;;;;;;;;;
(defn service-period-entry [plugin-path global-configuration-getter]
  (create-period-view
   (atom {:plugin-path          plugin-path
          :plugin-global-config global-configuration-getter
          :plugin-config        (get-in (global-configuration-getter) (conj plugin-path :config) {})
          :plugin-toolkit       (get-in (global-configuration-getter) (conj plugin-path :toolkit) {})})))



(defplugin service-period 
  "Plugin for service contracts of enterpreneurs")
