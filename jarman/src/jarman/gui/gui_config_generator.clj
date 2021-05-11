(ns jarman.gui.gui-config-generator
  (:use seesaw.core
        seesaw.util
        seesaw.border
        seesaw.dev
        seesaw.mig
        seesaw.color)
  (:require [clojure.string :as string]
            ;; resource 
            [jarman.resource-lib.icon-library :as icon]
            ;; logics
            [jarman.config.config-manager :as cm]
            [jarman.gui.gui-tools :as gtool]
            [jarman.gui.gui-components :as gcomp]
            [jarman.tools.swing :as stool]
            [jarman.gui.gui-components :refer :all :as gcomp]

            ;; deverloper tools 
            [jarman.tools.lang :refer :all :as lang]))

;; ┌─────────────────────────┐
;; │                         │
;; │ Create config generator │
;; │                         │
;; └─────────────────────────┘


(def confgen--element--header-file
  (fn [title] (mig-panel
               :constraints ["" "0px[grow, center]0px" "0px[]0px"]
               :items [[(label :text title :font (gtool/getFont 16) :foreground (gtool/get-color :foreground :dark-header))]]
               :background (gtool/get-color :background :dark-header)
               :border (line-border :thickness 10 :color (gtool/get-color :background :dark-header)))))

(def confgen--element--header-block
  (fn [title] (label :text title :font (gtool/getFont 16 :bold)
                     :border (compound-border  (line-border :bottom 2 :color (gtool/get-color :decorate :underline)) (empty-border :bottom 5)))))

(def confgen--element--header-parameter
  (fn [title]
    (label :text title :font (gtool/getFont 14 :bold))))

(def confgen--element--combobox
  (fn [local-changes path model]
    (mig-panel
     :constraints ["" "0px[200:, fill, grow]0px" "0px[30:, fill, grow]0px"]
     :items [[(combobox :model model
                        :font (gtool/getFont 14)
                        :background (gtool/get-color :background :combobox)
                        :size [200 :by 30]
                        :listen [:item-state-changed (fn [event] (let [choosed (config event :selected-item)
                                                                       new-model (join-vec [choosed] (filter #(not= choosed %) model))]
                                                                   (@gtool/changes-service :truck-changes :local-changes local-changes :path-to-value path :old-value model :new-value new-model)))])]])))


(def confgen--gui-interface--input
  (fn [local-changes path value]
    (mig-panel
     :constraints ["" "0px[200:, fill, grow]0px" "0px[30:, fill, grow]0px"]
     :items [[(text :text value :font (gtool/getFont 14)
                    :background (gtool/get-color :background :input)
                    :border (compound-border (empty-border :left 10 :right 10 :top 5 :bottom 5)
                                             (line-border :bottom 2 :color (gtool/get-color :decorate :gray-underline)))
                    :listen [:caret-update (fn [event] (@gtool/changes-service :truck-changes :local-changes local-changes :path-to-value path :old-value value :new-value (config event :text)))])]])))


(def confgen--gui-interface--input-textlist
  (fn [local-changes path value]
    (let [v (string/join ", " value)]
      (mig-panel
       :constraints ["" "0px[200:, fill, grow]0px" "0px[30:, fill, grow]0px"]
       :items [[(text :text v :font (gtool/getFont 14)
                      :background (gtool/get-color :background :input)
                      :border (compound-border (empty-border :left 10 :right 10 :top 5 :bottom 5)
                                               (line-border :bottom 2 :color (gtool/get-color :decorate :gray-underline)))
                      :listen [:caret-update (fn [event] (@gtool/changes-service :truck-changes :local-changes local-changes :path-to-value path :old-value value :new-value (clojure.string/split (config event :text) #"\s*,\s*")))])]]))))


(def confgen--gui-interface--input-textcolor
  (fn [local-changes path value]
    (mig-panel
     :constraints ["" "0px[200:, fill, grow]0px" "0px[30:, fill, grow]0px"]
     :items [[(text :text value :font (gtool/getFont 14)
                    :background value :foreground "#444"
                    :border (compound-border (empty-border :left 10 :right 10 :top 5 :bottom 5)
                                             (line-border :bottom 2 :color (gtool/get-color :decorate :gray-underline)))
                    :listen [:caret-update (fn [event]
                                             (@gtool/changes-service :truck-changes :local-changes local-changes :path-to-value path :old-value value :new-value (config event :text))
                                             (gtool/colorizator-text-component event))])]])))


(def confgen--choose--header
  (fn [type? name]
    (cond  (type? :block) (confgen--element--header-block name)
           (type? :param) (confgen--element--header-parameter name))))

(def confgen--element--textarea
  (fn [param]
    (if-not (nil? (param [:doc]))
      (textarea (str (param [:doc])) :font (gtool/getFont 12)) ())))

(def confgen--element--textarea-doc
  (fn [param]
    (if-not (nil? (param :doc))
      (textarea (str (param :doc)) :font (gtool/getFont 14)) ())))

(def confgen--element--margin-top-if-doc-exist
  (fn [type? param] (if (and (type? :block) (not (nil? (param :doc))))
                      (label :border (empty-border :top 10)) ())))

(def confgen--gui-interface--checkbox-as-droplist
  (fn [param local-changes start-key]
    (confgen--element--combobox local-changes start-key (if (= (param :value) true) [true false] [false true]))))

(def confgen--gui-interface--droplist
  (fn [param local-changes start-key]
    (confgen--element--combobox
     local-changes start-key (param :value)
    ;;  (let [item (cond 
    ;;               ;; (= (last start-key) :lang)
    ;;               ;;     (do
    ;;               ;;       (vec (map #(txt-to-UP %) (param :value))))
    ;;                   ;; (vector? (param start-key))
    ;;                   ;; (do
    ;;                   ;;   (vec (map #(txt-to-title %) (param :value))))
    ;;                   ;; :else
    ;;                   (do
    ;;                     (param :value)))]
    ;;    item)
     )))

(def confgen--recursive--next-configuration-in-map
  (fn [param confgen--component--tree local-changes start-key]
    (map (fn [next-param]
           (confgen--component--tree local-changes (join-vec start-key (list (first next-param)))))
         (param :value))))

(def confgen--element--gui-interfaces
  (fn [comp? param confgen--component--tree local-changes start-key]
    (cond (comp? :selectbox) (confgen--gui-interface--droplist param local-changes start-key)
          (comp? :checkbox)  (confgen--gui-interface--checkbox-as-droplist param local-changes start-key)
          (or (comp? :text) (comp? :textnumber)) (confgen--gui-interface--input local-changes start-key (str (param :value)))
          (comp? :textlist) (confgen--gui-interface--input-textlist local-changes start-key (param :value))
          (comp? :textcolor) (confgen--gui-interface--input-textcolor local-changes start-key (param :value))
          (map? (param :value)) (confgen--recursive--next-configuration-in-map param confgen--component--tree local-changes start-key)
          :else (confgen--element--textarea param))))


(def confgen--component--tree
  (fn [local-changes start-key]
    (let [param (fn [key] (get (cm/get-in-segment start-key) key))
          type? (fn [key] (= (param :type) key))
          comp? (fn [key] (= (param :component) key))
          name (if (nil? (param :name)) (lang/convert-key-to-title (last start-key)) (str (param :name)))]
      (if (= (param :display) :edit)
        (do
          (mig-panel
           :constraints ["wrap 1" "20px[]50px" "5px[]0px"]
           :border (cond (type? :block) (empty-border :bottom 10)
                         :else nil)
           :items (gtool/join-mig-items
                   (confgen--choose--header type? name)
                   (confgen--element--textarea-doc param)
                   (confgen--element--margin-top-if-doc-exist type? param)
                   (confgen--element--gui-interfaces comp? param confgen--component--tree local-changes start-key))))
        (do
          ())))))


;; (cm/get-in-segment [:themes])

;; (get-in (get-in (cm/get-in-segment []) [:init.edn]) [:display])

(def create-view--confgen
  "Description
     Join config generator parts and set view on right functional panel
   "
  (fn [start-key
       & {:keys [message-ok message-faild]
          :or {message-ok (fn [txt] (alert txt))
               message-faild (fn [txt] (alert txt))}}]
    (let [map-part (cm/get-in-segment start-key)
          local-changes (atom {})]
      (if (= (get-in map-part [:display]) :edit)
        (do
          (@gtool/changes-service :add-controller
                                  :view-id (last start-key)
                                  :local-changes local-changes)
          (mig-panel
           :border (line-border :bottom 50 :color (gtool/get-color :background :main))
           :constraints ["wrap 1" "0px[fill, grow]0px" "0px[]0px[grow, fill]0px[]0px"]
           :border nil
          ;;  :border (line-border :thickness 2 :color "#f00")

           :items (gtool/join-mig-items
                   (confgen--element--header-file (get-in map-part [:name])) ;; Header of section/config file

                   (gcomp/auto-scrollbox (mig-panel
                                          :constraints ["wrap 1" "0px[fill, grow]0px" "20px[grow, fill]20px"]
                                          :items (gtool/join-mig-items
                                                  (let [body (map
                                                              (fn [param]
                                                                (confgen--component--tree local-changes (join-vec start-key (list (first param)))))
                                                              (get-in map-part [:value]))]
                                                    body))))
                   (gcomp/button-basic "Save changes" (fn [e] ;; save changes configuration
                                                        (doall (map #(cm/assoc-in-value (first (second %)) (second (second %)))  @local-changes))
                                                        (let [validate (cm/store-and-back)]
                                                                          ;;  (println validate)
                                                          (if (get validate :valid?)
                                                            (do ;; message box if saved successfull
                                                              (if-not (nil? message-ok) (message-ok "")))
                                                            (do ;; message box if saved faild
                                                              (if-not (nil? message-faild) (message-faild (get validate :output)))))))))))))))

;; (@jarman.gui.gui-app/startup)
;; Show example
;; (let [my-frame (-> (doto (seesaw.core/frame
;;                           :title "test"
;;                           :size [800 :by 600]
;;                           :content (gcomp/auto-scrollbox (create-view--confgen [:resource.edn])))
;;                      (.setLocationRelativeTo nil) seesaw.core/pack! seesaw.core/show!))]
;;   (seesaw.core/config! my-frame :size [800 :by 600]))
