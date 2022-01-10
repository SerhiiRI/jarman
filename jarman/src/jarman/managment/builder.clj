(ns jarman.managment.builder
  (:require
   [clojure.string :as string]
   [clojure.reflect :as reflect]
   [jarman.tools.org  :refer :all]
   [jarman.tools.lang :refer :all]
   [jarman.logic.metadata :as metadata]
   [jarman.gui.gui-components :as gcomp]
   [jarman.gui.gui-calendar   :as gcalendar]
   [jarman.gui.gui-editors    :as gedit]))

(defmulti field (fn [m] (get-in m [:component-type :type] nil)))

(defmethod field metadata/CT-TEXT [m]
  (let [;; metainformation
        {component :component-type} m
        ;; 
        {:keys [value on-change]
         :as    arguments} (dissoc component :type)

        arguments (merge {:tooltip-text (:description m)
                          :placeholder  (:representation m)
                          :id (str (gensym (name (:field-qualified m))))}
                         arguments)]
    ;; ----------
    ;; That should be new declaration
    ;; (gui-components/state-input-text arguments)
    ;; ----------
    ;; This is old declartion
    (apply gcomp/state-input-text {:value value :function on-change}
           (apply concat (seq arguments)))))

(defmethod field metadata/CT-TEXTAREA [m]
  (let [;; metainformation
        {component :component-type} m
        ;; 
        {:keys [value on-change]
         :as    arguments} (dissoc component :type)

        arguments (merge {:tooltip-text (:description m)
                          :placeholder  (:representation m)
                          :id (str (gensym (name (:field-qualified m))))}
                         arguments)]
    ;; ----------
    ;; That should be new declaration
    ;; (gui-components/state-input-text arguments)
    ;; ----------
    ;; This is old declartion
    (apply gcomp/state-input-text-area {:value value :function on-change}
           (apply concat (seq arguments)))))

(defmethod field metadata/CT-DATE [m]
  (let [;; metainformation
        {component :component-type} m
        ;; 
        {:keys [value on-change]
         :as    arguments} (dissoc component :type)

        arguments (merge {:tooltip-text (:description m)
                          :placeholder  (:representation m)
                          :id (str (gensym (name (:field-qualified m))))}
                         arguments)]
    ;; ----------
    ;; That should be new declaration
    ;; (gui-components/state-input-text arguments)
    ;; ----------
    ;; This is old declartion
    (apply gcalendar/state-input-calendar {:value value :function on-change}
           (apply concat (seq arguments)))))

(defmethod field metadata/CT-CHECK [m]
  (let [;; metainformation
        {component :component-type} m
        ;; 
        {:keys [value on-change]
         :as    arguments} (dissoc component :type)

        arguments (merge {:tooltip-text (:description m)
                          :placeholder  (:representation m)
                          :id (str (gensym (name (:field-qualified m))))}
                         arguments)]
    ;; ----------
    ;; That should be new declaration
    ;; (gui-components/state-input-text arguments)
    ;; ----------
    ;; This is old declartion
    (apply gcomp/state-input-checkbox {:value value :function on-change}
           (apply concat (seq arguments)))))

(defmethod field metadata/CT-CODE [m]
  (let [;; metainformation
        {component :component-type} m
        ;; 
        {:keys [value on-change]
         :as    arguments} (dissoc component :type)

        arguments (merge {:tooltip-text (:description m)
                          :placeholder  (:representation m)
                          :id (str (gensym (name (:field-qualified m))))}
                         arguments)]
    ;; ----------
    ;; That should be new declaration
    ;; (gui-components/state-input-text arguments)
    ;; ----------
    ;; This is old declartion
    (apply gedit/state-code-area {:value value :function on-change}
           (apply concat (seq arguments)))))

(defmethod field metadata/CT-CODE [m]
  (let [;; metainformation
        {component :component-type} m
        ;; 
        {:keys [value on-change]
         :as    arguments} (dissoc component :type)

        arguments (merge {:tooltip-text (:description m)
                          :placeholder  (:representation m)
                          :id (str (gensym (name (:field-qualified m))))}
                         arguments)]
    ;; ----------
    ;; That should be new declaration
    ;; (gui-components/state-input-text arguments)
    ;; ----------
    ;; This is old declartion
    (apply gedit/state-code-area {:value value :function on-change}
           (apply concat (seq arguments)))))

(field {:description "table_name",
        :private? false,
        :default-value nil,
        :editable? true,
        :field :table_name,
        :column-type [:varchar-120 :default :null],
        :component-type {:type :text
                         :value "0"
                         :on-change
                         (fn [e] 
                           ;; (dispatch!
                           ;;  {:action :update-changes
                           ;;   :path   [:field-qualified]
                           ;;   :value  (c/value (c/to-widget e))})
                           )
                         ;;  :background face/c-input-bg
                         :font-size 14
                         :char-limit 0
                         :placeholder ""
                         :border [10 10 5 5 2]
                         ;; :border-color-unfocus face/c-underline
                         ;; :border-color-focus face/c-underline-on-focus
                         :start-underline nil
                         :args []},
        :representation "table_name",
        :constructor-var nil,
        :field-qualified :seal.table_name})

(defn convert-key-to-component
  "Description
     Convert to component automaticly by keyword.
     key is an key from model in defview as :user.name."
  [state! dispatch! panel meta-field-information]
  (with-state
   (let [table-name      (.return-table_name table-meta)
         ;;  meta            (meta/.return-columns-join meta)
         field-qualified (:field-qualified meta-field-information)
         title           (:representation  meta-field-information)
         editable?       (:editable?       meta-field-information)
         comp-types      (:component-type  meta-field-information)
         val             (cond
                           (not (nil? (key (:model         (state!))))) (key (:model         (state!)))
                           (not (nil? (key (:model-changes (state!))))) (key (:model-changes (state!))))
         val             (if (mt/isComponent? val)
                           val (str val))
         func            (fn [e]
                           (dispatch!
                            {:action :update-changes
                             :path   [(rift field-qualified :unqualifited)]
                             :value  (c/value (c/to-widget e))}))
         comp-func       (fn [e col-key] 
                           (do (dispatch!
                                {:action :update-comps-changes
                                 ;;:state-update
                                 :compn-obj  val
                                 :path   [(rift field-qualified :unqualifited)]
                                 :value ;;(assoc (key (:model-changes (state!))) col-key (c/value (c/to-widget e)))
                                 {col-key (c/value (c/to-widget e))}})))
         comp-func-save  (fn [e] (dispatch!
                                 {:action :download-comp
                                  :value (c/value (c/to-widget e))
                                  :v-obj val}))
         comp (gcomp/inpose-label
            title
            (cond
              (= mt/column-type-linking (first comp-types))
              (input-related-popup-table {:val val :state! state! :field-qualified field-qualified :dispatch! dispatch!})
             
              (or (= mt/column-type-data (first comp-types))
                 (= mt/column-type-datatime (first comp-types)))
              (calendar/state-input-calendar {:func func :val val})

              (= mt/column-comp-url (first comp-types))
              (ccomp/url-panel {:func comp-func
                                :val val})

              (= mt/column-comp-file (first comp-types))
              (ccomp/file-panel {:func comp-func
                                 :func-save comp-func-save
                                 :mode (:insert-mode (state!))
                                 :val val})

              (= mt/column-comp-ftp-file (first comp-types))
              (ccomp/ftp-panel {:func comp-func
                                :func-save comp-func-save
                                :mode (:insert-mode (state!))
                                :val val})
             
              (= mt/column-type-textarea (first comp-types))
              (gcomp/state-input-text-area {:func func :val val})

              (= mt/column-type-prop (first comp-types))
              (gedit/state-code-area {:func func :val val})

              (= mt/column-type-boolean (first comp-types))
              (gcomp/state-input-checkbox {:func func :val val})
             
              :else
              (gcomp/state-input-text {:func func :val val})))]
     (.add panel comp))))



;; [ ] (input-related-popup-table {:val val :state! state! :field-qualified field-qualified :dispatch! dispatch!})
;; [ ] (ccomp/url-panel {:func comp-func, :val val})
;; [ ] (ccomp/file-panel {:func comp-func, :func-save comp-func-save, :mode (:insert-mode (state!)), :val val})
;; [ ] (ccomp/ftp-panel {:func comp-func, :func-save comp-func-save, :mode (:insert-mode (state!)), :val val})
;; [X] (calendar/state-input-calendar {:func func :val val})
;; [X] (gcomp/state-input-text-area {:func func, :val val})
;; [X] (gedit/state-code-area {:func func, :val val})
;; [X] (gcomp/state-input-checkbox {:func func, :val val})
;; [X] (gcomp/state-input-text {:func func, :val val})
