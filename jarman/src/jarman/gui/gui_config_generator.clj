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
            [jarman.config.config-manager :as c]
            [jarman.gui.gui-tools :refer :all]
            [jarman.gui.gui-components :refer :all]
            ;; deverloper tools 
            [jarman.tools.lang :refer :all :as lang]
            ))


;; ┌─────────────────────────┐
;; │                         │
;; │ Create config generator │
;; │                         │
;; └─────────────────────────┘


(def confgen--element--header-file
  (fn [title] (mig-panel
               :constraints ["" "0px[grow, center]0px" "0px[]0px"]
               :items [[(label :text title :font (getFont 16) :foreground (get-color :foreground :dark-header))]]
               :background (get-color :background :dark-header)
               :border (line-border :thickness 10 :color (get-color :background :dark-header)))))

(def confgen--element--header-block
  (fn [title] (label :text title :font (getFont 16 :bold)
                     :border (compound-border  (line-border :bottom 2 :color (get-color :decorate :underline)) (empty-border :bottom 5)))))

(def confgen--element--header-parameter
  (fn [title]
    (label :text title :font (getFont 14 :bold))))

(def confgen--element--combobox
  (fn [changing-list path model]
    (mig-panel
     :constraints ["" "0px[200:, fill, grow]0px" "0px[30:, fill, grow]0px"]
     :items [[(combobox :model model
                        :font (getFont 14)
                        :background (get-color :background :combobox)
                        :size [200 :by 30]
                        ;; :listen [:item-state-changed (fn [event] (track-changes-used-components changing-list path event :selected-item (first model)))]
                        )]])))

(def confgen--gui-interface--input
  (fn [changing-list path value]
    (mig-panel
     :constraints ["" "0px[200:, fill, grow]0px" "0px[30:, fill, grow]0px"]
     :items [[(text :text value :font (getFont 14)
                    :background (get-color :background :input)
                    :border (compound-border (empty-border :left 10 :right 10 :top 5 :bottom 5)
                                             (line-border :bottom 2 :color (get-color :decorate :gray-underline)))
                    ;; :listen [:caret-update (fn [event] (track-changes-used-components changing-list path event :text value))]
                    )]])))


(def confgen--gui-interface--input-textlist
  (fn [changing-list path value]
    (let [v (string/join ", " value)]
      (mig-panel
       :constraints ["" "0px[200:, fill, grow]0px" "0px[30:, fill, grow]0px"]
       :items [[(text :text v :font (getFont 14)
                      :background (get-color :background :input)
                      :border (compound-border (empty-border :left 10 :right 10 :top 5 :bottom 5)
                                               (line-border :bottom 2 :color (get-color :decorate :gray-underline)))
                      ;; :listen [:caret-update (fn [event] (track-changes changing-list path value (clojure.string/split (config event :text) #"\s*,\s*")))]
                      )]]))))


(def confgen--gui-interface--input-textcolor
  (fn [changing-list path value]
    (mig-panel
     :constraints ["" "0px[200:, fill, grow]0px" "0px[30:, fill, grow]0px"]
     :items [[(text :text value :font (getFont 14)
                    :background value :foreground "#444"
                    :border (compound-border (empty-border :left 10 :right 10 :top 5 :bottom 5)
                                             (line-border :bottom 2 :color (get-color :decorate :gray-underline)))
                    ;; :listen [:caret-update (fn [event] (track-changes changing-list path value (config event :text))
                    ;;                          (colorizator-text-component event))]
                    )]])))


(def confgen--choose--header
  (fn [type? name]
    (cond  (type? :block) (confgen--element--header-block name)
           (type? :param) (confgen--element--header-parameter name))))

(def confgen--element--textarea
  (fn [param]
    (if-not (nil? (param [:doc]))
      (textarea (str (param [:doc])) :font (getFont 12)) ())))

(def confgen--element--textarea-doc
  (fn [param]
    (if-not (nil? (param :doc))
      (textarea (str (param :doc)) :font (getFont 14)) ())))

(def confgen--element--margin-top-if-doc-exist
  (fn [type? param] (if (and (type? :block) (not (nil? (param :doc))))
                      (label :border (empty-border :top 10)) ())))

(def confgen--gui-interface--checkbox-as-droplist
  (fn [param changing-list start-key]
    (confgen--element--combobox changing-list start-key (if (= (param :value) true) [true false] [false true]))))

(def confgen--gui-interface--droplist
  (fn [param changing-list start-key]
    (confgen--element--combobox
     changing-list start-key
     (let [item (cond (= (last start-key) :lang)
                      (do
                        (vec (map #(txt-to-UP %) (param :value))))
                      (vector? (param start-key))
                      (do
                        (vec (map #(txt-to-title %) (param :value))))
                      :else
                      (do
                        (param :value)))]
       item))))

(def confgen--recursive--next-configuration-in-map
  (fn [param confgen--component--tree changing-list start-key]
    (map (fn [next-param]
           (confgen--component--tree changing-list (join-vec start-key (list (first next-param)))))
         (param :value))))

(def confgen--element--gui-interfaces
  (fn [comp? param confgen--component--tree changing-list start-key]
    (cond (comp? :selectbox) (confgen--gui-interface--droplist param changing-list start-key)
          (comp? :checkbox)  (confgen--gui-interface--checkbox-as-droplist param changing-list start-key)
          (or (comp? :text) (comp? :textnumber)) (confgen--gui-interface--input changing-list start-key (str (param :value)))
          (comp? :textlist) (confgen--gui-interface--input-textlist changing-list start-key (param :value))
          (comp? :textcolor) (confgen--gui-interface--input-textcolor changing-list start-key (param :value))
          (map? (param :value)) (confgen--recursive--next-configuration-in-map param confgen--component--tree changing-list start-key)
          :else (confgen--element--textarea param))))


(def confgen--component--tree
  (fn [changing-list start-key]
    (let [param (fn [key] (get (c/get-in-segment start-key) key))
          type? (fn [key] (= (param :type) key))
          comp? (fn [key] (= (param :component) key))
          name (if (nil? (param :name)) (key-to-title (last start-key)) (str (param :name)))]
      (if (= (param :display) :edit)
        (do
          (mig-panel
           :constraints ["wrap 1" "20px[]50px" "5px[]0px"]
           :border (cond (type? :block) (empty-border :bottom 10)
                         :else nil)
           :items (join-mig-items
                   (confgen--choose--header type? name)
                   (confgen--element--textarea-doc param)
                   (confgen--element--margin-top-if-doc-exist type? param)
                   (confgen--element--gui-interfaces comp? param confgen--component--tree changing-list start-key))))
        (do
          ())))))


;; (c/get-in-segment [:themes])

;; (get-in (get-in (c/get-in-segment []) [:init.edn]) [:display])

(def create-view--confgen
  "Description
     Join config generator parts and set view on right functional panel
   "
  (fn [start-key]
    (let [map-part (c/get-in-segment start-key)
          changing-list (atom {})]
      (if (= (get-in map-part [:display]) :edit)
        (do
          ;; (add-changes-controller (last start-key) changing-list)
          (mig-panel
           :border (line-border :bottom 50 :color (get-color :background :main))
           :constraints ["wrap 1" "[fill, grow]" "20px[]20px"]
           :items (join-mig-items
                   (confgen--element--header-file (get-in map-part [:name])) ;; Header of section/config file
                   (label :text "Show changes" :listen [:mouse-clicked (fn [e]
                                                                             ;; save changes configuration
                                                                         (prn "Changes" @changing-list)
                                                                            ;;  (reset! configuration-copy (c/get-in-segment []))
                                                                            ;;  (doall
                                                                            ;;   (map
                                                                            ;;    (fn [new-value] ;; (println (first (second new-value)) (second (second new-value)))
                                                                            ;;      (swap! configuration-copy (fn [changes] (assoc-in changes (join-vec (first (second new-value)) [:value]) (second (second new-value))))))
                                                                            ;;    @changing-list))
                                                                            ;;  (let [out (sspec/valid-segment @configuration-copy)]
                                                                            ;;    (cond (get-in out [:valid?])
                                                                            ;;          (do
                                                                            ;;            (cond (get-in (save-all-cofiguration @configuration-copy) [:valid?])
                                                                            ;;                  (do
                                                                            ;;                    (@alert-manager :set {:header "Success!" :body "Changes were saved successfully!"} (message alert-manager) 5)
                                                                            ;;                    (c/store-and-back))
                                                                            ;;                  :else (let [m (string/join "<br>" ["Cannot save changes"])]
                                                                            ;;                          (@alert-manager :set {:header "Faild" :body m} (message alert-manager) 5)))) ;; TODO action on faild save changes configuration
                                                                            ;;          :else (let [m (string/join "<br>" ["Cannot save changes" (get-in out [:output])])]
                                                                            ;;                  (@alert-manager :set {:header "Faild" :body m} (message alert-manager) 5))))
                                                                               ;; (prn @configuration-copy)
                                                                         )])
                                   ;; Foreach on init values and create configuration blocks
                   (let [body (map
                               (fn [param]
                                 (confgen--component--tree changing-list (join-vec start-key (list (first param)))))
                               (get-in map-part [:value]))]
                     body))))))))

;; Show example
(let [my-frame (-> (doto (seesaw.core/frame
                          :title "test"
                          :size [800 :by 600]
                          :content (auto-scrollbox (create-view--confgen [:themes :current-theme])))
                     (.setLocationRelativeTo nil) seesaw.core/pack! seesaw.core/show!))]
  (seesaw.core/config! my-frame :size [800 :by 600]))
