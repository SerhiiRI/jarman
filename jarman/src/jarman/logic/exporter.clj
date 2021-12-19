(ns jarman.logic.exporter
  (:gen-class)
  (:require [seesaw.core               :as c]
            [jarman.gui.gui-tools      :as gtool]
            [jarman.gui.gui-components :as gcomp]
            [jarman.gui.gui-migrid     :as gmg]
            [jarman.logic.state        :as state]
            [jarman.config.storage     :as storage]
            [jarman.config.environment :as env]
            [clojure.java.io           :as io]
            [jarman.tools.org          :refer :all]
            [jarman.tools.lang :refer :all]))

;;;;;;;;;;;;;;;;;;
;;; ViewPlugin ;;;
;;;;;;;;;;;;;;;;;;

(def ^:private system-ExportDocs-list (ref []))
(defn system-DocExporter-list-get [] (deref system-ExportDocs-list))
(defrecord DocExporter
    [type
     name
     description
     data-fn
     export-form-gui
     override-form
     exporter-fn
     frame-size])

(defn component [& {:keys [field component-type label text]}]
  {:pre []}
  (assert (keyword? field) "Export Document Component `:field` must be a keyword identify value for query")
  (assert (some? component-type) "Export Document Component `:component-type` must be a keyword for choose component")
  (assert (or (string? label) (nil? label)) "Export Document Component `:label` can be nil or string")
  (assert (or (string? text) (nil? text))   "Export Document Component `:text` can be nil or string")
  {:field field
   :component-type component-type
   :label label
   :text text})

;; (component :field :xyz :component-type :input)

(defn- file-exist? [path] (.exists (io/file path)))

(defn- types [] {:odt ".odt"})

(defn- components
  [export-form-gui props-state]
  (doall
   (map
    (fn [gui-map]
      (let [component-type (:component-type gui-map)
            field          (:field gui-map)
            label          (:label gui-map)]
        (cond (= component-type :input)
              (gmg/migrid :v [(if (empty? label) nil (c/label :text label))
                              (c/text  :id field
                                       :listen [:caret-update
                                                (fn [e] (swap! props-state #(assoc % field (c/config e :text))))])]))))
    export-form-gui)))

(defn- build-form
  [title export-form-gui export-fn]
  (if (empty? export-form-gui)
    (export-fn {}) ;; Just static export as all users or something
    (let [props-state (atom {})
          root  (gmg/migrid :v :a :gf {:gap [5 0]} [])]
      (c/config! root :items (gtool/join-mig-items
                         (gmg/migrid :v (components export-form-gui props-state))
                         (gcomp/button-basic "Export"
                                             :onClick (fn [e] (export-fn (deref props-state)))
                                             :args [:id :export-btn])))
      root)))

(defn- exporter-get-fn
  [type exported-path data-fn template-name export-form-gui]
  "Prepare fn with exporter"
  (fn []
    (let [prefix         "Exported - "
          sufix          (get (types) type)
          template-src   (str (storage/document-templates-dir) "/" template-name sufix)
          exported-place (str exported-path "/" prefix template-name sufix)
          template-exist (file-exist? template-src)
          export-fn      (fn [props-map]
                           (apply kaleidocs.merge/merge-doc
                                  (clojure.java.io/file template-src)
                                  (clojure.java.io/file exported-place)
                                  (data-fn props-map)))]
      (println "Exporter log")
      (println (format "Template src:   '%s'" template-src))
      (println (format "Exported place: '%s'" exported-place))
      (println (format "Template exist: '%s'" template-exist))
      (if template-exist
        (build-form template-name export-form-gui export-fn)
        (println "Template file do not exist in storage. Need to download from DB.")))))

(defn- exporter-in-popup
  [exporter-fn title frame-size]
  (fn []
    (jarman.gui.popup/build-popup
     {:comp-fn (fn [api] (exporter-fn)
                 (let [root (exporter-fn)]
                   (c/config! (c/select root [:#export-btn]) :listen [:mouse-clicked (fn [e] ((:close api)))])
                   root))
      :title title
      :size frame-size})))

(defn- constructDocExporter [{:keys [type name description data-fn export-form-gui override-form frame-size]}]
  (assert (keyword? type)       "Export Document `:type` must be a keyword")
  (assert (string? name)        "Export Document `:name` must be a string")
  (assert (string? description) "Export Document `:description` must be string")
  (assert (fn? data-fn)         "Export Document `:data-fn` must be a function returning data for template")
  (assert (vector? export-form-gui) "Export Document `:export-form-params` must be a vector")
  (assert (or (fn? override-form) (nil? override-form)) "Export Document `:override-form` must be a function with component")
  (assert (vector? frame-size)  "Export Document `:frame-size` must be a vector")
  (let [exporter-fn   (exporter-get-fn type env/user-home data-fn name export-form-gui)
        exporter-popup-fn (exporter-in-popup exporter-fn name frame-size)]
    (map->DocExporter
     {:type            type
      :name            name
      :description     description
      :data-fn         data-fn
      :export-form-gui export-form-gui
      :override-form   override-form
      :exporter-fn     exporter-fn
      :invoke-popup    exporter-popup-fn
      :frame-size      frame-size})))

(defn register-doc-exporter
  [& {:as args}]
  (dosync (alter system-ExportDocs-list
                 (fn [l] (let [expo (constructDocExporter args)
                               l-without-old-expo (filterv #(not= (:name expo) (:name %)) l)]
                           (conj l-without-old-expo expo))))) true)
(defn find-exporter [name]
  (first (filter #(= name (get % :name)) (system-DocExporter-list-get))))



(comment
  (system-DocExporter-list-get)
  ((:invoke-popup (first (system-DocExporter-list-get))))
  (find-exporter "Export selected user")
  ((:invoke-popup (find-exporter "Export selected user")))
  )


;; TODO: Load infos about exporters form DB
;; TODO: Download templates
;; TODO: Check version of template
;; TODO: Download data script for template
;; TODO: Upload template and script
;; TODO: Update script by editor
;; TODO: Update template by reupload
;; TODO: Table upgrade: id table_name name document prop version script
