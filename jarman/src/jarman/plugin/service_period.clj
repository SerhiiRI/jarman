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
   [jarman.gui.gui-calendar :as calndr]
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

(def checkboxes (atom (:tree-index-paths req/grouped-query))
  ;;(atom [[2 3 4 false] [2 3 5 false] [2 3 6 false] [3 4 6 false] [3 4 7 false] [3 4 5 false]])
  )

(defn- comparator-fn [id-vec]
  (let [[_e _sc _scm] id-vec]
    (case (count id-vec)
      1 (fn [[e]] (= e _e))
      2 (fn [[e sc]] (and (= e _e) (= sc _sc)))
      3 (fn [[e sc scm]] (and (= e _e) (= sc _sc) (= scm _scm)))
      false)))

(defn- update-checkboxes [id-vec check?]
  (let [cmpr? (comparator-fn id-vec)]
    (reset! checkboxes (reduce (fn [acc period] (if (cmpr? period)
                                                  (conj acc (assoc period 3 check?))
                                                  (conj acc period))) [] @checkboxes))))


(defn- checked-box? [id-vec]
  (let [cmpr? (comparator-fn id-vec)]
    (reduce (fn [acc period] (if (cmpr? period)
                               (and acc (last period))
                               acc)) true @checkboxes)))

(defn build-expand-by-map
  "Description:
    Build list of recursive button expand using configuration map."
  [plugin-m pan state]
  (let [v-pan        (seesaw.core/vertical-panel)
        mig-hor      (fn [item color id-vec]
                       (seesaw.core/border-panel
                        :background color
                        :west   (seesaw.core/flow-panel :background "#fff"
                                                        :size [25 :by 25]
                                                        :items (list
                                                                (seesaw.core/checkbox
                                                                 :selected? (checked-box? id-vec)
                                                                 :listen [:mouse-clicked
                                                                          (fn [e]
                                                                            (do
                                                                              (println "Button info->>" (seesaw.core/user-data item))
                                                                              (println "ATOM click" @checkboxes)
                                                                              ;; (update-checkboxes
                                                                              ;;  id-vec
                                                                              ;;  (.isSelected (.getComponent e))) (.removeAll pan)
                                                                              ;; (build-expand-by-map plugin-m pan :lvl 1) (.revalidate pan) (.repaint pan)
                                                                              ))]
                                                                 :background "#fff")))
                        :east   (seesaw.core/flow-panel :background "#fff"
                                                        :size [80 :by 25])
                        :center (seesaw.mig/mig-panel :constraints ["wrap 1" "0px[:820]0px" "0px[]0px"]
                                                      :background "#cecece"
                                                      :items [[item]])))]
    ((fn btn [[k v]]
       (if-not (map? v)
         (do (println "HEYY")(.add v-pan (mig-hor (part-expand (split-date [(:service_contract.contract_start_term k)
                                                                            (:service_contract.contract_end_term k)])
                                                               (seesaw.core/vertical-panel
                                                                :items (doall (map (fn [item]
                                                                                     (mig-hor (part-button (split-date [(:service_contract_month.service_month_start item)
                                                                                                                        (:service_contract_month.service_month_end item)]) "#dddddd"
                                                                                                           800) "#fff" (:v item))) v)))
                                                               "#dddddd" 100) "#fff" (:v k))))
         (do (.add pan
                   (mig-hor (part-expand (:enterpreneur.name k) v-pan "#cecece" 200) "#fff" (:v k)))
             (btn (first v))))) plugin-m)
    (println "ATOM" @checkboxes)   
    pan))

(defn insert-contract [insert-space])
(seesaw.dev/show-options (seesaw.core/grid-panel))

(defn add-contract-panel [insert-space state]
  (let [label-fn      (fn [text] (seesaw.core/label :text text :font (gtool/getFont 16)
                                                    :border (b/empty-border :right 8)))
        calndr-start  (calndr/get-calendar (gcomp/input-text :args [:columns 24]))
        calndr-end    (calndr/get-calendar (gcomp/input-text :args [:columns 24]))
        price-input   (gcomp/input-text :args [:columns 10])
        enterpreneurs (:all-enterpreneurs @state)
        select-box    (gcomp/select-box (vec (map (fn [item] (:name item)) enterpreneurs)))] ;; to do
    (seesaw.core/grid-panel
     ;;     seesaw.mig/mig-panel :constraints ["wrap 3" "10px[]10px" "10px[]10px"]
     :rows 2
     :columns 3
     :hgap 10
     :vgap 10
     :items [ ;;gtool/join-mig-items
          
             (seesaw.core/horizontal-panel
              :items (list (label-fn "select enterpr:") select-box))
             (seesaw.core/horizontal-panel
              :items (list (label-fn "start term:") calndr-start))
             (gcomp/menu-bar
              {:buttons [[" OK"
                          icon/agree1-blue-64-png
                          (fn [e] (insert-contract insert-space))]
                         [" NO"
                          icon/x-blue1-64-png
                          (fn [e] (seesaw.core/config! insert-space
                                                       :items [[(seesaw.core/label)]]))]]})
             (seesaw.core/horizontal-panel
              :items (list (label-fn "price:") price-input))
             (seesaw.core/horizontal-panel
              :items (list (label-fn "end term: ") calndr-end))])))

(defn create-period-view
  [state]
  (let [{:keys [select-group-data select-enterpreneurs]} (:plugin-toolkit @state)
        tv                 (:tree-view (select-group-data))
        all-enterpreneurs  (select-enterpreneurs)
        insert-space       (seesaw.mig/mig-panel :constraints ["wrap 1" "0px[fill, grow]0px" "0px[top]0px"])
        btn-panel          (gcomp/menu-bar
                            { ;;:justify-end true
                             :buttons [[" Add cotract"
                                        icon/plus-64-png
                                        (fn [e] (do (seesaw.core/config! insert-space :items [[(seesaw.core/vertical-panel :items (list (seesaw.core/label :text "Some informotion about plugin ...........")))]
                                                                                              [(add-contract-panel insert-space state)]])))]
                                       [" Delete contract"
                                        icon/basket-blue1-64-png
                                        (fn [e])]]})
        view-space         (seesaw.mig/mig-panel :constraints ["wrap 1" "0px[fill, grow]0px" "0px[top]0px"]
                                                 :items [[btn-panel]
                                                         [insert-space]])]
    (swap! state merge {:all-enterpreneurs all-enterpreneurs})
    (doall
     (map (fn [[entr srv_cntr]] (let [pan (seesaw.core/vertical-panel)] (.add view-space (build-expand-by-map [entr srv_cntr] pan state)))) tv))
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



