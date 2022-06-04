(ns jarman.application.collector-custom-view-plugins
  (:require
   [jarman.tools.lang :refer :all]
   [jarman.tools.org :refer :all]
   [clojure.spec.alpha :as s]
   ))

(defn default-system-view-plugin-spec-list []
  (list
   [:name
    {:spec [:jarman.application.spec/name :req-un],
     :examples "Table editor",
     :doc "It's plugin-view name, which use as component label text"}]
   [:permission
    {:spec [:jarman.application.spec/permission :req-un]
     :examples "[:user :admin]"
     :doc "Key to select of possible permissions, put this key in vec
           (if you don't enter this key, you will have global key
            from defview, in another way you will have [:user])"}]
   [:plug-place
    {:spec [:jarman.application.spec/plug-place :opt-un]
     :examples [:#tables-view-plugin]
     :doc "This key indicates place for component"}]))

(defn generate-dynamic-spec
  "Take `defplugin` body, get and process :spec key in key
  and generate full map spec body `s/key`. All specs grouping
  by `:req-un` and `:opt-un`.

  Warning! also function add to `plugin-key-list`
  `plugin-system-requirements` list of specs

  (generate-dynamic-spec
   (list
    [:tables
     {:spec [:jarman.plugin.table/tables :req-un]}]
    [:view-columns
     {:spec [:jarman.plugin.table/view-columns :opt-un]}]))

  ;; =>
  (s/key
   :req-un
   [:jarman.application.spec/name
    :jarman.application.spec/permission
    :jarman.plugin.table/tables]
   :opt-un
   [:jarman.application.spec/plug-place :jarman.plugin.table/view-columns])

  See
   (s/check-asserts?)"
  [plugin-name plugin-key-list]
 (let [klist (vec (concat (default-system-view-plugin-spec-list) plugin-key-list))
       gruoped-spec (group-by second
                              (map (fn [[k {[spec-k spec-req] :spec}]]
                                     [spec-k spec-req]) klist))]
   (eval
    `(do
       (s/def ~(keyword "plugin.spec" (str plugin-name))
         (s/keys
          :req-un ~(mapv first (:req-un gruoped-spec))
          :opt-un ~(mapv first (:opt-un gruoped-spec))))
       (fn [~'configurations]
         (s/assert ~(keyword "plugin.spec" (str plugin-name))  ~'configurations))))))

;;;;;;;;;;;;;;;;;;
;;; ViewPlugin ;;;
;;;;;;;;;;;;;;;;;;

(def ^:private system-ViewPlugin-list (ref []))
(defn system-ViewPlugin-list-get [] (deref system-ViewPlugin-list))
(defrecord ViewPlugin
    [plugin-test-spec-fn
     plugin-entry-fn
     plugin-toolkit-fn
     plugin-description
     plugin-name])
(defn constructViewPlugin [{:keys [description toolkit name entry spec-list]}]
  (assert (symbol? name) "View plugin `:name` must be symbol")
  (assert (string? description) "View plugin `:description` is not string type")
  (assert (fn? toolkit) "View plugin `:toolkit` might be a function (fn [configuration]..)")
  (assert (fn? entry) "View plugin `:entry` might be a function (fn [plugin-path global-configuration]..)")
  (map->ViewPlugin
   {:plugin-test-spec-fn (generate-dynamic-spec name spec-list)
    :plugin-entry-fn entry
    :plugin-toolkit-fn toolkit
    :plugin-description description
    :plugin-name name}))


;; register-custom-view-plugin ->
(defn register-view-plugin [& {:as args}]
  (dosync (alter system-ViewPlugin-list
            (fn [l] (let [plugin (constructViewPlugin args)
                         l-without-old-plugin
                         (filterv #(not= (:plugin-name plugin) (:plugin-name %)) l)]
                     (conj l-without-old-plugin plugin))))) true)
