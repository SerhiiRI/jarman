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
            [jarman.logic.connection   :as db]
            [jarman.logic.sql-tool :refer [select! update! insert! delete!]]
            [jarman.tools.org          :refer :all]
            [jarman.tools.lang :refer :all]))

;;;;;;;;;;;;;;;;;;
;;; ViewPlugin ;;;
;;;;;;;;;;;;;;;;;;

(def ^:private system-ExportDocs-list (ref []))
(def ^:private system-ExportDocs-metadata (ref []))

(defn clear-storage []
  (dosync (ref-set system-ExportDocs-list []))
  (dosync (ref-set system-ExportDocs-metadata [])))

(defn system-DocExporter-list-get [] (deref system-ExportDocs-list))
(defn system-DocExporter-list-metadata [] (deref system-ExportDocs-metadata))

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
          root  (gmg/migrid :v :a :gf {:gap [10]} [])]
      (c/config! root :items (gtool/join-mig-items
                         (gmg/migrid :v (components export-form-gui props-state))
                         (gcomp/button-basic "Export"
                                             :underline-size 0
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

(defn load-clj-to-var
  "Description:
     Load clojure code to variable"
  [path]
  (if (file-exist? path)
    (prn-str (clojure.core/slurp path))))

(defn upload-exporter
  "Description:
     Upload exporter. Point template path and script."
  [{:keys [template-path script-path table-name prop]}]
  (let [template-exist?  (file-exist? template-path)
        x (assert template-exist? "Cannot find template")
        template-name    (last (gtool/split-path template-path))
        script           (load-clj-to-var script-path)
        template-version (str (clojure.core/hash (slurp template-path)))
        prop             (pr-str (rift prop {}))]
    
    (println (str "\nUpload template by path: " template-path)
             (str "\nTemplate exist?          " template-exist?)
             (str "\nUpload script by path:   " script-path)
             (str "\nTemplate version:        " template-version)
             (str "\nFor table name:          " (rift table-name "empty"))
             (str "\nProps:                   " prop)
             (str "\nScript:                  " (gtool/str-cutter (rift script "empty") 40)))

    (jarman.logic.document-manager/insert-blob!
     {:table_name :documents
      :column-list [[:id :null] [:table_name :string] [:name :string] [:document :blob] [:prop :string] [:version :string] [:script :string]]
      :values {:id nil
               :table_name ""
               :name     template-name
               :document template-path
               :prop     prop
               :version  template-version
               :script   script}})
    
   ))

(comment ;; UPLOAD EXPORTER
  (upload-exporter {:template-path (str (storage/document-templates-dir) "/Export selected user.odt")
                    :script-path "src/jarman/logic/exporter_demo.clj"})
  )

(defn download-template-from-db
  [id]
  (println (format "Downloading template from DB by id `%d`" id)))

(defn exporter-listing
  "Description:
     Listing and register plugins"
  []
  (let [exporters (db/query
                   (select!
                    {:table_name :documents}))]
    (clear-storage)
    (doall
     (map
      (fn [ex]
        (let [local-file-path (str (storage/document-templates-dir) "/" (:name ex))
              verified (= (str (clojure.core/hash (slurp local-file-path))) (:version ex))
              meta {:id (:id ex) :name (:name ex) :prop (:prop ex) :version (:version ex) :local-path local-file-path}]
          (println (str
                    "\nID:         " (:id ex)
                    "\nName:       " (:name ex)
                    "\nDocument:   " (if (:document ex) true false)
                    "\nProp:       " (:prop ex)
                    "\nVersion:    " (:version ex)
                    "\nScript:     " (gtool/str-cutter (:script ex) 40)
                    "\nLocal file: " local-file-path
                    "\nSame ver.?  " verified))
          ;; (if-not verified (download-template-from-db (:id ex)))
          (load-string (read-string (:script ex)))
          (dosync (alter system-ExportDocs-metadata
                         (fn [l] (let [l-without-old-meta (filterv #(not= (:name meta) (:name %)) l)]
                                   (conj l-without-old-meta meta)))))))
      exporters))))

(comment ;; Listing all exporters, main
  (exporter-listing)
  (system-DocExporter-list-get)
  (system-DocExporter-list-metadata)
  )

(defn delete-exporter
  "Description:
     Remove exporter from DB by id"
  [id]
  (let [name "Users list"]
    (println (format "\nRemove exporter with id `%s` and name `%s`" id name))
    (db/exec (delete! {:table_name :documents
                       :where [:= :id id]}))))

(comment ;; remove by id from DB
  (delete-exporter 3)
  )


;; TODO: Download templates if hash as version is not same
;; TODO: Open script by editor sucking script directly from DB and save changes to DB
;; TODO: Update template by reupload to DB template file and hash as version
