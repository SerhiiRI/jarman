(ns jarman.plugin.periods
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
  [list-space view-space return-fn company-id]
  (let [;; period-list (get-period-list company-id)
        auto-menu-hide false
        return (gcomp/button-slim (str "<< " (gtool/get-lang-btns :back))
                                  :underline-size 1
                                  :onClick (fn [e] (c/invoke-later (do
                                                                   (c/config! list-space :items (gtool/join-mig-items (return-fn list-space view-space return-fn)))
                                                                   (gtool/switch-focus)))))
        periods (gcomp/expand-form-panel
                 list-space
                 [return
                  (gcomp/scrollbox
                   (gcomp/vmig
                    :items (gtool/join-mig-items
                            (gcomp/vmig
                             :items (gtool/join-mig-items
                                     (gcomp/button-slim "01/01/2021 - 31/12/2021"
                                                        :onClick (fn [e]
                                                                   (c/config! view-space :items (gtool/join-mig-items (create-period--period-form)))
                                                                   (if auto-menu-hide ((:hide-show (c/config (c/select list-space [:#expand-panel]) :user-data))))))
                                     (gcomp/button-slim "01/01/2021 - 31/12/2021" :onClick (fn [e]))
                                     (gcomp/button-slim "01/01/2021 - 31/12/2021" :onClick (fn [e]))
                                     (gcomp/button-slim "01/01/2021 - 31/12/2021" :onClick (fn [e]))
                                     (gcomp/button-slim "01/01/2021 - 31/12/2021" :onClick (fn [e])))))))]
                 :max-w 180
                 :args [:id :expand-panel :background "#fff"])]
    (gtool/set-focus return)
    return
    periods))


(defn get-company-list
  [] [{:name "Trashpanda-Team" :id 1} {:name "Frank & Franky" :id 3}
      {:name "Trashpanda-Team" :id 1} {:name "Trashpandowe Zakłady Wyrobów Kodowych Team" :id 3}
      {:name "Trashpanda-Team" :id 1} {:name "Frank & Franky" :id 3}])

(defn create-period--period-companys-list
  [list-space view-space return-fn] ;; [{:name "Frank & Franky" :id 3}]
  (gtool/rm-focus)
  (let
   [model (get-company-list)
    buttons (map (fn [enterpreneur]
                   (let [name (:name enterpreneur)
                         btn (gcomp/button-slim name
                                                :onClick (fn [e] (c/invoke-later (do
                                                                                 (c/config! list-space :items (gtool/join-mig-items (create-period--period-list list-space view-space return-fn (get enterpreneur :id))))
                                                                                 (gtool/switch-focus))))
                                                :args [:tip name])]
                     (gtool/set-focus-if-nil btn)
                     btn))
                 model)]
    (gcomp/expand-form-panel
     list-space
     (gcomp/vmig :items (gtool/join-mig-items buttons))
     :max-w 180
     :args [:background "#fff"])))


(defn create-period-view
  []
  (let [list-space (gcomp/vmig :args [:border (b/line-border :left 1 :right 1 :color "#eee")])
        view-space (gcomp/vmig)
        list-space (c/config! list-space :items (gtool/join-mig-items (create-period--period-companys-list list-space view-space create-period--period-companys-list)))]
    (gcomp/hmig
     :hrules "[shrink 0, fill]0px[grow, fill]"
     :items [[list-space]
             [view-space]]
     :args [:background "#fff"])))




(defn periods [plugin-config plugin-toolkit]
  (create-period-view))

(defn periods-toolkit-pipeline [configuration]
  (let [toolkit (query-toolkit/data-toolkit-pipeline configuration {})]
    {:periods (fn [] (do (println "heyy") (periods configuration toolkit)))}))

(defn periods-entry [plugin-path global-configuration])



(doto (seesaw.core/frame :size [700 :by 700] :content (create-period-view))
  (.setLocationRelativeTo nil)  seesaw.core/pack! seesaw.core/show!)

