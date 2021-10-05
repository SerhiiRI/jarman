(ns jarman.gui.gui-docs
  (:refer-clojure :exclude [update])
  (:require
   ;; Clojure toolkit 
   [clojure.data :as data]
   [clojure.string :as string]
   [seesaw.util :as u]
   ;; Seesaw components
   [seesaw.core :as c]
   [seesaw.border :as sborder]
   [seesaw.dev :as sdev]
   [seesaw.mig :as smig]
   [seesaw.swingx :as swingx]
   [seesaw.chooser :as chooser]
   ;; Jarman toolkit
   [jarman.logic.connection :as db]
   [jarman.tools.lang :refer :all :as lang]
   [jarman.gui.gui-tools :refer :all :as gtool]
   [jarman.resource-lib.icon-library :as ico]
   [jarman.tools.swing :as stool]
   [jarman.gui.gui-components :refer :all :as gcomp]
   [jarman.gui.gui-calendar :as calendar]
   [jarman.config.storage :as storage]
   [jarman.config.environment :as env]
   [jarman.logic.sql-tool :as toolbox :include-macros true :refer :all]
   [jarman.logic.metadata :as mt]
   [jarman.logic.document-manager :as dm])
  (:import (java.util Date)
           (java.text SimpleDateFormat)))

;; (defn quick-path [table]
;;   (mt/recur-find-path (first (mt/getset! (keyword table)))))

;; (defmacro ^:private make-name [entity suffix]
;;   `(symbol (str ~entity ~suffix)))


;; (defn get-view-column-meta [table-list column-list]
;;   (->> table-list
;;        (mapcat (fn [t] (vec ((comp :columns :prop) (first (mt/getset! t))))))
;;        (filter (fn [c] (in? column-list (keyword (:field-qualified c)))))))

;; (defn- model-column [column]
;;   (let [component-type (:component-type column)
;;         on-boolean (fn [m] (if (in? component-type "b") (into m {:class java.lang.Boolean}) m))
;;         on-number  (fn [m] (if (in? component-type "n") (into m {:class java.lang.Number})  m))]
;;     (-> {:key (keyword (:field-qualified column)) :text (:representation column)}
;;         on-number
;;         on-boolean)))

;; (defn construct-table-model-columns [table-list column-list]
;;   (mapv model-column (get-view-column-meta table-list column-list)))

;; (defn construct-table-model [model-columns data-loader]
;;   (fn []
;;     [:columns model-columns
;;      :rows (data-loader)]))

;; (defn- tf-t-f [table-field]
;;   (let [t-f (string/split (name table-field) #"\.")]
;;     (mapv keyword t-f)))

;; (defn- t-f-tf [table field]
;;   (keyword (str (name table) "." (name (name field)))))

;; ;;;;;;;;;;;;;;
;; ;;; JTABLE ;;;
;; ;;;;;;;;;;;;;;

;; (defn construct-table [model]
;;   (fn [listener-fn]
;;     (let [TT (swingx/table-x :model (model))]
;;       (c/listen TT :selection (fn [e] (listener-fn (seesaw.table/value-at TT (c/selection TT)))))
;;       (c/config! TT :horizontal-scroll-enabled? true)
;;       (c/config! TT :show-grid? false)
;;       (c/config! TT :show-horizontal-lines? true)
;;       (c/scrollable TT :hscroll :as-needed :vscroll :as-needed))))

;; (defn construct-sql [table select-rules]
;;   {:pre [(keyword? table)]}
;;   (let [m (first (mt/getset table))
;;         ;; relations (recur-find-path m)
;;         id_column (t-f-tf table :id)
;;         table-name ((comp :field :table :prop) m)
;;         columns (map :field ((comp :columns :prop) m))]
;;     {:update (fn [entity] (if (id_column entity) (update table-name :set entity :where (=-v id_column (id_column entity)))))
;;      :insert (fn [entity] (insert table-name :values (vals entity)))
;;      :delete (fn [entity] (if (id_column entity) (delete table-name :where (=-v id_column (id_column entity)))))
;;      :select (fn [& {:as args}]
;;                (apply (partial select-builder table)
;;                       (mapcat vec (into select-rules args))))}))

;; (def print-view (atom {}))
;; (defmacro defview [table & {:as args}]
;;   (let [stable (make-name (str table) "-view")]
;;     `(let [config#          (atom (assoc ~args :table-name (keyword '~table)))
;;            backup-config#   (deref config#)
;;            restore-config#  (fn [] (reset! config# backup-config#))
;;            ktable#    (:table-name @config#)
;;            ;; stable#    (str @config#)
;;            tblmeta#   ((comp :table :prop) (first (mt/getset! ktable#)))
;;            colmeta#   ((comp :columns :prop) (first (mt/getset! ktable#)))
;;            operations# (construct-sql ktable# (:data @config#))

;;            view#      (:view @config#)
;;            idfield#   (t-f-tf ktable# :id)

;;            select#    (:select operations#)
;;            update#    (:update operations#)
;;            delete#    (:delete operations#)
;;            insert#    (:insert operations#)

;;            dselect#   (fn [] (db/exec (select#)))
;;            dupdate#   (fn [e#] (db/exec (update# e#)))
;;            ddelete#   (fn [e#] (db/exec (delete# e#)))
;;            dinsert#   (fn [e#] (db/exec (insert# e#)))

;;            data#      (fn [] (db/query (select#)))
;;            export#    (select# :column nil :inner-join nil :where nil)
;;            model#     (construct-table-model-columns (:tables @config#) (:view @config#))
;;            table#     (construct-table (construct-table-model model# data#))]
;;        (def ~stable {:->table table#
;;                      :->table-model model#
;;                      :->model->id idfield#
;;                      :->data data#
;;                      :->table-name ktable#

;;                      :->select select#
;;                      :->update update#
;;                      :->delete delete#
;;                      :->insert insert#

;;                      :->dselect dselect#
;;                      :->dupdate dupdate#
;;                      :->ddelete ddelete#
;;                      :->dinsert dinsert#

;;                      :->operations operations#
;;                      :->config (fn [] @config#)
;;                      :->col-view view#
;;                      :->col-meta colmeta#
;;                      :->tbl-meta tblmeta#})
;;        (swap! ~'print-view (fn [m-view#] (assoc-in m-view# [ktable#] ~stable)))
;;        nil)))

;; (defn as-is [& column-list]
;;   (map #(if (keyword? %) {% %} %) column-list))

;; ;; (defview documents
;; ;;   :tables [:documents]
;; ;;   :view   [:documents.table :documents.name :documents.prop]
;; ;;   :data   {:column (as-is :documents.id :documents.table :documents.name :documents.prop)})


;; (defn construct-dialog [table-fn selected frame]
;;   (let [dialog (seesaw.core/custom-dialog :modal? true :width 400 :height 500 :title "Select component")
;;         table (table-fn (fn [model] (seesaw.core/return-from-dialog dialog model)))
;;         dialog (seesaw.core/config!
;;                 dialog
;;                 :content (seesaw.mig/mig-panel
;;                           :constraints ["wrap 1" "0px[grow, fill]0px" "5px[grow, fill]0px"]
;;                           :items [[table]]))]
;;     (.setLocationRelativeTo dialog frame)
;;     (seesaw.core/show! dialog)))


;; ;; (:->col-meta documents-view)

;; (defn build-input-form
;;   [controller model focus alerts & more-comps]
;;   (let [meta (:->col-meta controller)
;;         complete (atom {})
;;         insert-or-update (if (nil? model) "Upload template" "Update template")
;;         delete "Delete selected template"
;;         panel (smig/mig-panel :constraints ["wrap 1" "0px[250:, grow, fill]0px" "0px[fill]0px"]
;;                               :border (sborder/empty-border :thickness 10)
;;                               :items [[(c/label)]])
;;         x nil ;;--------Chooser component parts
;;         input-text (gcomp/input-text :args [:text "" :font (gtool/getFont  :name "Monospaced")])
;;         icon (c/label :icon (jarman.tools.swing/image-scale ico/enter-64-png 30)
;;                       :border (sborder/empty-border :thickness 8)
;;                       :listen [:mouse-clicked (fn [e] (let [new-path (chooser/choose-file :success-fn  (fn [fc file] (.getAbsolutePath file)))]
;;                                                         (c/config! input-text :text new-path)))])
;;         input-chooser (c/horizontal-panel :items [icon input-text])
;;         x nil ;;--------Prepare components to seq
;;         components [(gcomp/inpose-label (get (nth meta 0) :representation) (gcomp/select-box (vec (map #(get % :table) (mt/getset)))
;;                                                                                              :store-id :documents.table
;;                                                                                              :local-changes complete
;;                                                                                              :selected-item (if (nil? model) "" (get model :documents.table))))
;;                     (gcomp/inpose-label (get (nth meta 1) :representation) (gcomp/input-text-with-atom
;;                                                                             :store-orginal true
;;                                                                             :store-id :documents.name
;;                                                                             :local-changes complete
;;                                                                             :val (if (nil? model) "" (get model :documents.name))))
;;                     (gcomp/inpose-label "Prop" (gcomp/input-text-area :store-id :documents.prop 
;;                                                                       :local-changes complete 
;;                                                                       :val (if (nil? model) "" (get model :documents.prop))))
;;                     (gcomp/inpose-label "Choose file with template" input-chooser)
;;                     (gcomp/hr 10)
;;                     (gcomp/button-basic insert-or-update
;;                                         :onClick (fn [e]
;;                                                           ;;  (println "atom" @complete)
;;                                                    (if (nil? model)
;;                                                      (if (and (not (nil? (get @complete :documents.table)))
;;                                                               (not (empty? (c/config input-text :text))))
;;                                                        (let [insert-meta {:table    (get @complete :documents.table)
;;                                                                           :name     (get @complete :documents.name)
;;                                                                           :document (c/config input-text :text)
;;                                                                           :prop     (symbol (get @complete :documents.prop))}]
;;                                                          (println "to save" insert-meta)
;;                                                          (dm/insert-document insert-meta)
;;                                                                 ;;  (if-not (nil? alerts) (@jarman.gui.gui-seed/alert-manager :set {:header (gtool/get-lang-alerts :success) :body (gtool/get-lang-alerts :insert-new-record)} (@jarman.gui.gui-seed/alert-manager :message jarman.gui.gui-seed/alert-manager) 5))
;;                                                          ((@jarman.gui.gui-seed/jarman-views-service :reload))))

;;                                                      (if (not (nil? (get @complete :documents.table)))
;;                                                        (let [insert-meta {:id       (get model (:->model->id controller))
;;                                                                           :table    (get @complete :documents.table)
;;                                                                           :name     (get @complete :documents.name)
;;                                                                           :prop     (symbol (get @complete :documents.prop))}]
;;                                                          (dm/insert-document insert-meta)
;;                                                          (println "to save" insert-meta)
;;                                                          ((@jarman.gui.gui-seed/jarman-views-service :reload)))))))
;;                     (if (nil? model) (c/label) (gcomp/button-basic delete 
;;                                                                    :onClick (fn [e]
;;                                                                             ;; (println "Delete row: " (get model (:->model->id controller)))
;;                                                                             (println "Delete " model)
;;                                                                             (dm/delete-document {:id (get model (:->model->id controller))})
;;                                                                             ((@jarman.gui.gui-seed/jarman-views-service :reload)))))]]
;;     ;; (println "Model" model)

;;     ;; (reset! focus (last (u/children (first components))))
;;     (c/config! panel :items (join-mig-items components (if (nil? more-comps) (c/label) more-comps)))))


;; (def auto-builder--table-view
;;   (fn [controller
;;        & {:keys [start-focus
;;                  alerts]
;;           :or {start-focus (atom {})
;;                alerts nil}}]
;;     (let [controller  (if (nil? controller) documents-view controller)
;;           x nil ;;------------ Prepare
;;           insert-form   (fn [] (build-input-form controller nil start-focus alerts))
;;           view-layout   (smig/mig-panel :constraints ["" "0px[shrink 0, fill]0px[grow, fill]0px" "0px[grow, fill]0px"])
;;           table         (fn [] (second (u/children view-layout)))
;;           header        (fn [] (c/label :text (get (:->tbl-meta controller) :representation) :halign :center :border (sborder/empty-border :top 10)))
;;           update-form   (fn [model return] (gcomp/expand-form-panel view-layout [(header) (build-input-form controller model start-focus (return))] :w 300))
;;           x nil ;;------------ Build
;;           expand-insert-form (gcomp/scrollbox (gcomp/expand-form-panel view-layout [(header) (insert-form)] :w 300) :hscroll :never)
;;           back-to-insert     (fn [] (gcomp/button-basic "<< Return to Insert Form" :onClick (fn [e] (c/config! view-layout :items [[expand-insert-form] [(table)]]))))
;;           expand-update-form (fn [model return] (c/config! view-layout :items [[(gcomp/scrollbox (update-form model return) :hscroll :never)] [(table)]]))
;;           table              (fn [] ((:->table controller) (fn [model] (expand-update-form model back-to-insert))))
;;           x nil ;;------------ Finish
;;           view-layout        (c/config! view-layout :items [[(c/vertical-panel :items [expand-insert-form])] [(c/vertical-panel :items [(table)])]])]
;;       view-layout)))


;; (@jarman.gui.gui-seed/startup)

;; ;; (:->col-meta documents-view)

;; (let [start-focus (atom nil)
;;       my-frame (-> (doto (c/frame
;;                           :title "test"
;;                           :size [1000 :by 800]
;;                           :content (auto-builder--table-view  nil))
;;                      (.setLocationRelativeTo nil) c/pack! c/show!))]
;;   (c/config! my-frame :size [1000 :by 800])
;;   ;; (if-not (nil? start-focus) (c/invoke-later (.requestFocus @start-focus true)))
;;   )




;; (def run (fn [view] (let [start-focus (atom nil)
;;                           my-frame (-> (doto (c/frame
;;                                               :title "test"
;;                                               :size [1000 :by 800]
;;                                               :content
;;                                               (auto-builder--table-view view :start-focus start-focus))
;;                                          (.setLocationRelativeTo nil) c/pack! c/show!))]
;;                       (c/config! my-frame :size [1000 :by 800])
;;                       (if-not (nil? start-focus) (c/invoke-later (.requestFocus @start-focus true))))))


