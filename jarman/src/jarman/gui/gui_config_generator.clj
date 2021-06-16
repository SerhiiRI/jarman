(ns jarman.gui.gui-config-generator
  (:use seesaw.dev
        seesaw.mig
        seesaw.color)
  (:require [clojure.string :as string]
            [seesaw.core   :as c]
            [seesaw.border :as b]
            [seesaw.util   :as u]
            [seesaw.mig    :as smig]
            ;; resource 
            [jarman.resource-lib.icon-library :as icon]
            ;; logics
            [jarman.config.config-manager :as cm]
            [jarman.gui.gui-tools :as gtool]
            [jarman.gui.gui-components :as gcomp]
            [jarman.tools.swing :as stool]
            [jarman.logic.state :as state]
            [jarman.gui.gui-seed :as gseed]

            ;; deverloper tools 
            [jarman.tools.lang :refer :all]))

;; ┌─────────────────────────┐
;; │                         │
;; │ Create config generator │
;; │                         │
;; └─────────────────────────┘

(def confgen--element--header-block
  (fn [title] 
    (gcomp/header-basic title)
    (c/label :text title :font (gtool/getFont 16 :bold)
                     :border (b/compound-border  (b/line-border :bottom 2 :color (gtool/get-color :decorate :underline)) (b/empty-border :bottom 5)))))

(def confgen--element--header-parameter
  (fn [title]
    (c/label :text title :font (gtool/getFont 14 :bold))))

(def confgen--element--combobox
  (fn [local-changes path model]
    (smig/mig-panel
     :constraints ["" "0px[200:, fill, grow]0px" "0px[30:, fill, grow]0px"]
     :items [[(gcomp/select-box model
                                :always-set-changes false
                                :selected-item (first model)
                                :store-id path
                                :local-changes local-changes)]])))


(def confgen--gui-interface--input
  (fn [local-changes path value]
    (smig/mig-panel
     :constraints ["" "0px[200:, fill, grow]0px" "0px[30:, fill, grow]0px"]
     :items [[(gcomp/input-text-with-atom
               {:val value
                :store-id path
                :local-changes local-changes}
               )]])))


(def confgen--gui-interface--input-number
  (fn [local-changes path value]
    (smig/mig-panel
     :constraints ["" "0px[200:, fill, grow]0px" "0px[30:, fill, grow]0px"]
     :items [[(gcomp/input-int
               {:val value
                :store-id path
                :local-changes local-changes}
               )]])))


(def confgen--gui-interface--input-textlist
  (fn [local-changes path value]
    (let [v (string/join ", " value)]
      ;; (println "Textlist path: " path)
      (smig/mig-panel
       :constraints ["" "0px[200:, fill, grow]0px" "0px[30:, fill, grow]0px"]
       :items [[(c/text :text v :font (gtool/getFont 14)
                      :background (gtool/get-color :background :input)
                      :border (b/compound-border (b/empty-border :left 10 :right 10 :top 5 :bottom 5)
                                               (b/line-border :bottom 2 :color (gtool/get-color :decorate :gray-underline)))
                      :listen [:caret-update (fn [event] (@gseed/changes-service :truck-changes :local-changes local-changes :path-to-value path :old-value value :new-value (clojure.string/split (c/config event :text) #"\s*,\s*")))])]]))))


(def confgen--gui-interface--input-textcolor
  (fn [local-changes path value]
    (smig/mig-panel
     :constraints ["" "0px[200:, fill, grow]0px" "0px[30:, fill, grow]0px"]
     :items [[(c/text :text value :font (gtool/getFont 14)
                    :background value :foreground "#444"
                    :border (b/compound-border (b/empty-border :left 10 :right 10 :top 5 :bottom 5)
                                             (b/line-border :bottom 2 :color (gtool/get-color :decorate :gray-underline)))
                    :listen [:caret-update (fn [event]
                                             (@gseed/changes-service :truck-changes :local-changes local-changes :path-to-value path :old-value value :new-value (c/config event :text))
                                             (gtool/colorizator-text-component event))])]])))


(def confgen--choose--header
  (fn [type? name]
    (cond  (type? :block) (confgen--element--header-block name)
           (type? :param) (confgen--element--header-parameter name))))

(def confgen--element--textarea
  (fn [param]
    (if-not (nil? (param [:doc]))
      (gcomp/textarea (str (param [:doc])) :font (gtool/getFont 12)) ())))


(def confgen--element--textarea-doc
  (fn [param]
    (if-not (nil? (param :doc))
      (gcomp/textarea (str (param :doc)) :font (gtool/getFont 14)) ())))

(def confgen--element--margin-top-if-doc-exist
  (fn [type? param] (if (and (type? :block) (not (nil? (param :doc))))
                      (c/label :border (b/empty-border :top 10)) ())))

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
          (comp? :text)      (confgen--gui-interface--input local-changes start-key (str (param :value)))
          (comp? :textnumber)(confgen--gui-interface--input-number local-changes start-key (str (param :value)))
          (comp? :textlist)  (confgen--gui-interface--input-textlist local-changes start-key (param :value))
          (comp? :textcolor) (confgen--gui-interface--input-textcolor local-changes start-key (param :value))
          (map? (param :value)) (confgen--recursive--next-configuration-in-map param confgen--component--tree local-changes start-key)
          :else (confgen--element--textarea param))))


(def confgen--component--tree
  (fn [local-changes start-key]
    (let [param (fn [key] (key (cm/get-in-segment start-key)))
          type? (fn [key] (= (param :type) key))
          comp? (fn [key] (= (param :component) key))
          name (if (nil? (param :name)) (gtool/convert-key-to-title (last start-key)) (str (param :name)))]
      (if (or (nil? (param :display)) (= (param :display) :edit))
        (do
          (smig/mig-panel
           :constraints ["wrap 1" "20px[]50px" "5px[]0px"]
           :border (cond (type? :block) (b/empty-border :bottom 10)
                         :else nil)
           :items (gtool/join-mig-items
                   (let [a (confgen--choose--header type? name)
                         x (if (nil? a) (println "confgen--choose--header  return nil"))
                         b (confgen--element--textarea-doc param)
                         x (if (nil? b) (println "confgen--element--textarea-doc  return nil"))
                         c (confgen--element--margin-top-if-doc-exist type? param)
                         x (if (nil? c) (println "confgen--element--margin-top-if-doc-exist  return nil"))
                         d (confgen--element--gui-interfaces comp? param confgen--component--tree local-changes start-key)
                         x (if (nil? d) (println "confgen--element--gui-interfaces return nil"))]
                     [a b c d]))))
        ))))


;; (cm/get-in-segment [:themes])

;; (get-in (get-in (cm/get-in-segment []) [:init.edn]) [:display])

(def create-view--confgen
  "Description
     Join config generator parts and set view on right functional panel
   "
  (fn [start-key
       & {:keys [message-ok message-faild]
          :or {message-ok (fn [head body] (c/alert (str head ": " body)))
               message-faild (fn [head body] (c/alert (str head ": " body)))}}]
    (let [map-part (cm/get-in-segment start-key)
          local-changes (atom {})]
      (if (= :edit (:display map-part))
        (do
          (@gseed/changes-service :add-controller
                                  :view-id (last start-key)
                                  :local-changes local-changes)
          (let [configurator (smig/mig-panel
                              :border (b/line-border :bottom 50 :color (gtool/get-color :background :main))
                              :constraints ["wrap 1" "0px[fill, grow]0px" "0px[]0px[grow, fill]0px[]0px"]
                              :border nil
                              :items (gtool/join-mig-items
                                      (gcomp/header-basic (:name map-part)) ;; Header of section/config file
                                      ;; (c/label :text (doto "Header complete" println))
                                      (gcomp/auto-scrollbox
                                       (smig/mig-panel
                                        :constraints ["wrap 1" "0px[fill, grow]0px" "20px[grow, fill]20px"]
                                        :items (gtool/join-mig-items
                                                (let [body (map
                                                            #(let [path (join-vec start-key [(first %)])
                                                                   comp (confgen--component--tree local-changes path)]
                                                               comp)
                                                            (:value map-part))]
                                                  body))))
                                      (smig/mig-panel
                                       :constraints ["" "0px[grow,fill]0px[fill]0px" "0px[grow,fill]0px"]
                                       :items [[(gcomp/button-basic (gtool/get-lang-btns :save)
                                                                    :onClick (fn [e] ;; save changes configuration
                                                                               (if (empty? @local-changes)
                                                                                 (do
                                                                                   (println "No more changes.")
                                                                                   (message-ok "No changes." "Local storage is empty. No changes to saving."))
                                                                                 (do
                                                                                   (doall (map #(prn (first %) (str (second %)))  @local-changes))
                                                                                   (doall (map #(cm/assoc-in-value (first %) (second %))  @local-changes))

                                                                                   ;; TODO: Can not saving chages in theme, store-and-backup do not saving to file
                                                                                  ;;  (cm/get-in-value [:themes :current-theme :color :jarman :bar])
                                                                                  ;;  (cm/assoc-in-value [:themes :current-theme :color :jarman :bar] "#aaa")
                                                                                  ;;  (cm/store-and-back)

                                                                                   (let [validate (cm/store-and-back)]
                                                                                     (println validate)
                                                                                     (cm/swapp)
                                                                                     (if (get validate :valid?)
                                                                                       (do ;; message box if saved successfull
                                                                                         (try
                                                                                           (((state/state :jarman-views-service) :reload))
                                                                                           (if-not (nil? message-ok) (message-ok (gtool/get-lang-alerts :changes-saved)
                                                                                                                                 (str @local-changes)))
                                                                                           (catch Exception e (println (str "Message ok error: " (.getMessage e))))))
                                                                                       (do ;; message box if saved faild
                                                                                         (try
                                                                                           (if-not (nil? message-faild) (message-faild (gtool/get-lang-alerts :changes-saved-failed)
                                                                                                                                       (str (:output validate))))
                                                                                           (catch Exception e (println (str "Message faild error: " (.getMessage e)))))))))))
                                                                    :flip-border true)]
                                               [(gcomp/button-basic ""
                                                                    :onClick (fn [e] (do
                                                                                       (println "\nConfiguration changes\n" (str @local-changes))
                                                                                       (message-ok "Configuration changes" (str @local-changes))))
                                                                    :flip-border true
                                                                    :args [:icon (stool/image-scale icon/loupe-blue-64-png 25)])]])))]
            ;; (println "Config complete")
            (if (nil? configurator) (c/label :text "NIL") configurator)))))))
;; (@jarman.gui.gui-app/startup)
;; (cm/restore-config)
;; (cm/get-in-value [:themes :jarman_light.edn :components :message-box :border-size])
;; (cm/assoc-in-value [:themes :jarman_light.edn :components :message-box :border-size] ["1" "1" "1" "1"])
;; (cm/store-and-back)
;; (cm/swapp)
;; Show example
;; (let [my-frame (-> (doto (seesaw.core/frame
;;                           :title "test"
;;                           :size [800 :by 600]
;;                           :content (gcomp/auto-scrollbox (create-view--confgen [:resource.edn])))
;;                      (.setLocationRelativeTo nil) seesaw.core/pack! seesaw.core/show!))]
;;   (seesaw.core/config! my-frame :size [800 :by 600]))
