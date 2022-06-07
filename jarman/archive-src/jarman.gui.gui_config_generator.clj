;;; DEPRECATED
(ns jarman.gui.gui-config-generator
  (:use seesaw.dev
        seesaw.mig
        seesaw.color)
  (:require [clojure.string :as string]
            [seesaw.core   :as c]
            [seesaw.border :as b]
            [seesaw.util   :as u]
            [seesaw.mig    :as smig]
            ;; Dev tools
            [jarman.application.session :as session]
            ;; resource 
            [jarman.resource-lib.icon-library :as icon]
            [jarman.gui.gui-style :as gs]
            ;; logics
            [jarman.config.config-manager :as cm]
            [jarman.gui.gui-tools         :as gtool]
            [jarman.gui.gui-components    :as gcomp]
            [jarman.gui.gui-style         :as gs]
            [jarman.gui.components.swing           :as stool]
            [jarman.logic.state           :as state]
            [jarman.gui.gui-seed          :as gseed]
            [jarman.gui.gui-editors       :as gedit]
            [jarman.gui.gui-views-service :as gvs]
            [jarman.interaction :as i]
            ;; deverloper tools 
            [jarman.lang :refer :all])
  (:import
   (jiconfont.icons.google_material_design_icons GoogleMaterialDesignIcons)))

;; ┌─────────────────────────┐
;; │                         │
;; │ Create config generator │
;; │                         │
;; └─────────────────────────┘

(defn- confgen--element--header-block
  [title] 
  (gcomp/header-basic title)
  (c/label :text title :font (gs/getFont :bold 16)
           :border (b/compound-border  (b/line-border :bottom 2 :color (gtool/get-color :decorate :underline)) (b/empty-border :bottom 5))))

(defn- confgen--element--header-parameter
  [title]
  (c/label :text title :font (gs/getFont :bold)))

(defn- confgen--element--combobox
  [local-changes path model]
  (smig/mig-panel
   :constraints ["" "0px[200:, fill, grow]0px" "0px[30:, fill, grow]0px"]
   :items [[(gcomp/select-box model
                              :always-set-changes false
                              :selected-item (first model)
                              :store-id path
                              :local-changes local-changes)]]))


(defn- confgen--gui-interface--input
  [local-changes path value]
  (smig/mig-panel
   :constraints ["" "0px[200:, fill, grow]0px" "0px[30:, fill, grow]0px"]
   :items [[(gcomp/input-text-with-atom
             {:val value
              :store-id path
              :local-changes local-changes}
             )]]))


(defn- confgen--gui-interface--input-number
  [local-changes path value]
  (smig/mig-panel
   :constraints ["" "0px[200:, fill, grow]0px" "0px[30:, fill, grow]0px"]
   :items [[(gcomp/input-int
             {:val value
              :store-id path
              :local-changes local-changes}
             )]]))


(defn- confgen--gui-interface--input-textlist
  [local-changes path value]
  (let [v (string/join ", " value)]
    ;; (println "Textlist path: " path)
    (smig/mig-panel
     :constraints ["" "0px[200:, fill, grow]0px" "0px[30:, fill, grow]0px"]
     :items [[(c/text :text v
                      :background (gtool/get-color :background :input)
                      :border (b/compound-border (b/empty-border :left 10 :right 10 :top 5 :bottom 5)
                                                 (b/line-border :bottom 2 :color (gtool/get-color :decorate :gray-underline)))
                      :listen [:caret-update (fn [event] (@gseed/changes-service :truck-changes :local-changes local-changes :path-to-value path :old-value value :new-value (clojure.string/split (c/config event :text) #"\s*,\s*")))])]])))


(defn- confgen--gui-interface--input-textcolor
  [local-changes path value]
  (smig/mig-panel
   :constraints ["" "0px[200:, fill, grow]0px" "0px[30:, fill, grow]0px"]
   :items [[(c/text :text value
                    :background value :foreground "#444"
                    :border (b/compound-border (b/empty-border :left 10 :right 10 :top 5 :bottom 5)
                                               (b/line-border :bottom 2 :color (gtool/get-color :decorate :gray-underline)))
                    :listen [:caret-update (fn [event]
                                             (@gseed/changes-service :truck-changes :local-changes local-changes :path-to-value path :old-value value :new-value (c/config event :text))
                                             (gtool/colorizator-text-component event))])]]))


(defn- confgen--choose--header
  [type? name]
  (let [debug false]
    (if debug (println "\nchoose header---------"
                       "\nname" name))
    (cond  (type? :block) (do (if debug (println "type block"))
                              (confgen--element--header-block name))
           (type? :param) (do (if debug (println "type param"))
                              (confgen--element--header-parameter name)))))

(defn- confgen--element--textarea
  [param]
  ;; (println "\ntextarea---------"
  ;;          "\nparam" (param :doc))
  (let [comp (if (nil? (param :doc))
               ()
               (gcomp/textarea (str (param :doc))))]
    ;;(println "textarea ok")
    comp))


(defn- confgen--element--textarea-doc
  [param]
  ;; (println "\ntextarea doc---------"
  ;;          "\nparam" (param :doc))
  (if-not (nil? (param :doc))
    (gcomp/textarea (str (param :doc)))
    ()))

(defn- confgen--element--margin-top-if-doc-exist
  [type? param]
  ;; (println "\nmargin-top-if-doc-exist---------"
  ;;          "\nparam" (param :doc))
  (if (and (type? :block) (not (nil? (param :doc))))
    (c/label :border (b/empty-border :top 10))
    ()))

(defn- confgen--gui-interface--checkbox-as-droplist
  [param local-changes start-key]
  (confgen--element--combobox local-changes start-key (if (= (param :value) true) [true false] [false true])))

(defn- confgen--gui-interface--droplist
  [param local-changes start-key]
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
   ))

(defn- confgen--recursive--next-configuration-in-map
  [param confgen--component--tree local-changes start-key]
  ;;(println "\nCheckpoint - Recursive enter")
  (map (fn [next-param]
         (confgen--component--tree local-changes (join-vec start-key (list (first next-param)))))
       (param :value)))

(defn- confgen--element--gui-interfaces
  [comp? param confgen--component--tree local-changes start-key]
  ;;(println "\nCheckpoint Component chooser.")
  (cond
    (comp? :selectbox) (confgen--gui-interface--droplist param local-changes start-key)
    (comp? :checkbox)  (confgen--gui-interface--checkbox-as-droplist param local-changes start-key)
    (comp? :text)      (confgen--gui-interface--input local-changes start-key (str (param :value)))
    (comp? :textnumber)(confgen--gui-interface--input-number local-changes start-key (str (param :value)))
    (comp? :textlist)  (confgen--gui-interface--input-textlist local-changes start-key (param :value))
    (comp? :textcolor) (confgen--gui-interface--input-textcolor local-changes start-key (param :value))
    (map? (param :value)) (confgen--recursive--next-configuration-in-map param confgen--component--tree local-changes start-key)
    :else (confgen--element--textarea param)
    
    ))


(defn- confgen--component--tree
  [local-changes start-key]
  (let [debug false
        param (fn [key]
                (if (keyword? key)
                  (key (cm/get-in-segment start-key))
                  (println "[ Warning! ] confgen--component--tree: param fn need :key as arg!")))
        type? (fn [key]
                (if (keyword? key)
                  (= (param :type) key)
                  (println "[ Warning! ] confgen--component--tree: type? fn need :key as arg!")))
        comp? (fn [key]
                (if (keyword? key)
                  (= (param :component) key)
                  (println "[ Warning! ] confgen--component--tree: comp? fn need :key as arg!")))
        name (if (nil? (param :name)) (gtool/convert-key-to-title (last start-key)) (str (param :name)))]
    (if (or (= false (contains? (cm/get-in-segment start-key) :display))
            (= :edit (param :display)))
      (do
        (if debug (println "\ncomp tree-----------"
                           "\nstart key: " start-key
                           "\nname" name
                           "\ntype" (param :type)
                           "\nsegment" (cm/get-in-segment start-key)))
        (smig/mig-panel
         :constraints ["wrap 1" "20px[]50px" "5px[]0px"]
         :border (cond (type? :block)
                       (b/empty-border :bottom 10)
                       :else
                       nil)
         :items (gtool/join-mig-items
                 (let [a (confgen--choose--header type? name)
                       x (if (nil? a) (println "confgen--choose--header  return nil")
                             (if debug (println "Header: ok")))
                       b (confgen--element--textarea-doc param)
                       x (if (nil? b) (println "confgen--element--textarea-doc  return nil")
                             (if debug (println "doc: ok")))
                       c (confgen--element--margin-top-if-doc-exist type? param)
                       x (if (nil? c) (println "confgen--element--margin-top-if-doc-exist  return nil")
                             (if debug (println "margin: ok")))
                       d (confgen--element--gui-interfaces comp? param confgen--component--tree local-changes start-key)
                       x (if (nil? d) (println "confgen--element--gui-interfaces return nil")
                             (if debug (println "comp: ok")))
                       comps (list a b c d)]
                   ;;(println "\nComps" comps)
                   ;;(gcomp/popup-window {:view comps})
                   comps
                   ))))
      () ;; Empty part if no edit  
      )
    ))


;; (cm/get-in-segment [:themes])

;; (get-in (get-in (cm/get-in-segment []) [:init.edn]) [:display])

(defn create-view--confgen
  "Description
     Join config generator parts and set view on right functional panel
   "
  [start-key
   & {:keys [message-ok message-faild]
      :or {message-ok (fn [head body] (c/alert (str head ": " body)))
           message-faild message-ok}}]
  (try (let [map-part (cm/get-in-segment start-key)
             local-changes (atom {})]
         (if (= :edit (:display map-part))
           (do
             (@gseed/changes-service :add-controller
              :view-id (last start-key)
              :local-changes local-changes)
             (let [configurator
                   (smig/mig-panel
                    :border (b/line-border :bottom 50 :color (gtool/get-color :background :main))
                    :constraints ["wrap 1" "0px[fill, grow]0px" "0px[]0px[grow, fill]0px[]0px"]
                    :border nil
                    :items
                    (gtool/join-mig-items
                     (gcomp/header-basic (:name map-part)) ;; Header of section/config file
                     ;; (c/label :text (doto "Header complete" println))
                     (gcomp/auto-scrollbox
                      (try ;;Catch if map have syntax error and display text editor.
                        (smig/mig-panel
                         :constraints ["wrap 1" "0px[fill, grow]0px" "20px[grow, fill]20px"]
                         :items (gtool/join-mig-items
                                 (let [body
                                       (map
                                        #(let [path (join-vec start-key [(first %)])
                                               comp (confgen--component--tree local-changes path)]
                                           comp)
                                        (:value map-part))]
                                   body)))
                        (catch Exception e (do
                                             (message-faild
                                              "Can not load config!"
                                              " Opening code editor. Configuration probably have some syntax error.")
                                             (gedit/view-config-editor start-key map-part)
                                             ))))
                     
                     (smig/mig-panel
                      :constraints ["" "0px[grow,fill]0px[fill]0px" "0px[grow,fill]0px"]
                      :items (gtool/join-mig-items
                              (gcomp/button-basic
                               (gtool/get-lang-btns :save)
                               :onClick
                               (fn [e] ;; save changes configuration
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
                                             (if-not (nil? message-faild)
                                               (message-faild (gtool/get-lang-alerts :changes-saved-failed)
                                                              (str (:output validate))))
                                             (catch Exception e (println (str "Message faild error: " (.getMessage e)))))))))))
                               :flip-border true)
                              (gcomp/button-basic
                               ""
                               :onClick (fn [e] (do
                                                  (println "\nConfiguration changes\n" (str @local-changes))
                                                  (gcomp/popup-info-window
                                                   "Configuration changes"
                                                   (str @local-changes)
                                                   (state/state :app))))
                               :flip-border true
                               :args [:icon  (gs/icon GoogleMaterialDesignIcons/SEARCH)])
                              (if (= "developer" (session/get-user-permission))
                                (gcomp/button-basic
                                 ""
                                 :onClick (fn [e] (gedit/view-config-editor start-key map-part))
                                 :flip-border true
                                 :args [:icon (stool/image-scale (gs/icon GoogleMaterialDesignIcons/DESCRIPTION) 25)])
                                () ;; Empty if can not have access
                                )))))]
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
;;(with-out-str (clojure.pprint/pprint {:a {:b "c"}}))




;; ┌─────────────────────────────────────────┐
;; │                                         │
;; │ Create expand btns for config generator │
;; │                                         │
;; └─────────────────────────────────────────┘

(defn create-expand-btns--confgen
  "Discription:
     Return list of button expand chlide with configurations parts."
  [colors-fn lvl]
  (let [[color-bg color-hover child-bg] (if (fn? colors-fn)
                                          (second (colors-fn lvl))
                                          ["f7f7f7" "f7f7f7" "f7f7f7"])
        current-theme (str (first (cm/get-in-value [:themes :theme_config.edn :selected-theme])) ".edn")
        config-file-list-as-keyword (map #(first %) (cm/get-in-segment []))
        config-file-list-as-keyword-to-display
        (filter #(let [map-part (cm/get-in-segment (if (vector? %) % [%]))]
                   (and (= :file (get map-part :type))
                        (= :edit (get map-part :display))))
                config-file-list-as-keyword)
        
        coll (reverse
              (conj
               (map
                (fn [p]
                  (let [path (if (vector? p) p [p])
                        title (get (cm/get-in-segment path) :name)
                        view-id (last path)]
                    (gcomp/button-expand-child
                     title
                     :lvl         lvl
                     :c-focus     color-hover
                     :background  child-bg
                     :onClick (fn [e]
                                (gvs/add-view
                                 :view-id view-id
                                 :title title
                                 :render-fn (try
                                              (fn [] (create-view--confgen
                                                      path
                                                      :message-ok (fn [head body]
                                                                    (i/info head body) :time 5)))
                                              (catch Exception e (gedit/popup-config-editor
                                                                  path
                                                                  (get (cm/get-in-segment path))))))))))
                config-file-list-as-keyword-to-display)
               (let [path [:themes :theme_config.edn] ;; Selected theme
                     title (:name (cm/get-in-segment path))
                     view-id :theme_config.edn]
                 (gcomp/button-expand-child
                  title
                  :lvl         lvl
                  :c-focus     color-hover
                  :background  child-bg
                  :onClick (fn [e]
                             (gvs/add-view
                              :view-id view-id
                              :title title
                              :render-fn (fn [] (create-view--confgen
                                                 path
                                                 :message-ok (fn [head body]
                                                               (i/info head body :time 5))))))))
               (let [path [:themes :current-theme] ;; Themes config
                     title (rift (:name (cm/get-in-segment path)) "NIL")
                     view-id :current-theme]
                 (gcomp/button-expand-child
                  title
                  :lvl         lvl
                  :c-focus     color-hover
                  :background  child-bg
                  :onClick (fn [e]
                             (gvs/add-view
                              :view-id view-id
                              :title title
                              :render-fn (fn [] (create-view--confgen
                                                 path
                                                 :message-ok (fn [head body]
                                                               (i/info head body))))))))))]
    coll))
