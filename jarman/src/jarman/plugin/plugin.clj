(ns jarman.plugin.plugin
  (:require [jarman.config.environment :as env]
            [jarman.tools.lang :refer :all]
            [clojure.spec.alpha :as s]))

;;; TODO there must be specs

(def ^:private str-space (fn [num] (apply str (take num (repeat " ")))))

(defn- flatt-offset-vec
  "Description:
    this func concat empty space on the left side with str-doc for formatting our comment,
    get func, which generate empty space on the left side and struct like vector of our
    strings"
  [func struct]
  (map func (reduce
             (fn [acc [index form]]
               (cond
                 (string? form) (conj acc [index form])
                 (sequential? form)
                 (concat acc (map #(vector index %) form))))
             [] struct)))

(defn generate-key-section
  "Description:
    this function get offset for generate empty space on the left side
    for formatting our comment and get one vector of configurations,
    return formatting section with key-content for our comment"
  [offset map-keys]
  (let [formatter (fn [[local-offset text]] (apply str (str-space (+ offset local-offset)) text env/line-separator))
        [key {spec :spec exmp :examples doc :doc}] map-keys]
    (clojure.string/join ""
                         (flatt-offset-vec
                          formatter
                          [[0 (format "%s (spec %s)" key spec)]
                           [2 "Doc"]
                           [4 doc]
                           [2 "Examples"]
                           [4 (clojure.string/split (pp-str exmp) #"\r\n")]]))))

(defn generate-plugin-doc
  "Description
    This function generate doc for some plugin. Get title as description
    and map of configurations, return doc-strinf for function
  
   Example
    [[:permission
     {:spec :global-plugin/permission
      :examples [:user :admin]
      :doc \"Allow resolve access to this plugin by user\"}] ...]
    ;; => Description\n    some-descriotion\n  Keys\n    :permission
          (spec :global-plugin/permission)\r\n      Doc\r\n        Allow
          resolve access to this plugin by user\r\n
          Examples\r\n        [:user :admin]\r\n"
  [title body]
  (let [sp (fn [num] (apply str (take num (repeat " "))))])
  (apply str
         (format "Description\n    %s\n  Keys\n" title)
         (doall (map (fn [b] (generate-key-section 4 b)) body))))

(defn get-spec-from-map
  "Example
    [[:permission
     {:spec :global-plugin/permission
      :examples [:user :admin]
      :doc \"Allow resolve access to this plugin by user\"}] ...]
    ;; => {:permission :global-plugin/permission}"
  [body]
  (reduce into {} (for [b body]
                    (if-not (nil? (:spec (second b)))
                      {(first b) (:spec (second b))}))))

(defn plugin-system-requirements []
  (list
   [:name
    {:spec [:jarman.plugin.spec/name :req-un],
     :examples "Table editor",
     :doc "It's plugin-view name, which use as component label text"}]
   [:permission
    {:spec [:jarman.plugin.spec/permission :req-un]
     :examples "[:user :admin]"
     :doc "Key to select of possible permissions, put this key in vec 
           (if you don't enter this key, you will have global key
            from defview, in another way you will have [:user])"}]
   [:plug-place
    {:spec [:jarman.plugin.spec/plug-place :opt-un]
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
 [:jarman.plugin.spec/name
  :jarman.plugin.spec/permission
  :jarman.plugin.table/tables]
 :opt-un
 [:jarman.plugin.spec/plug-place :jarman.plugin.table/view-columns])"
  [plugin-key-list]
  
 (let [klist (vec (concat (plugin-system-requirements) plugin-key-list))
       gruoped-spec (group-by second
                              (map (fn [[k {[spec-k spec-req] :spec}]]
                                     [spec-k spec-req]) klist))]
   (list 's/key
         :req-un (mapv first (:req-un gruoped-spec))
         :opt-un (mapv first (:opt-un gruoped-spec)))))



(defmacro defplugin
  [plugin-name ns description & body]
  (let [create-name-func (fn [fname] (symbol (str ns "/" plugin-name "-" fname)))
        func-component (create-name-func "component")
        func-tool (symbol (str plugin-name "-toolkit-pipeline"))
        func-toolkit (symbol (str ns "/" func-tool))
        func-t (symbol (str plugin-name "-toolkit-pipeline"))]    
    `(do        
       (defn ~plugin-name
         ;;; documentations
         ~(generate-plugin-doc description body)
         ;;; argumnets
         [~'plugin-path ~'global-configuration]
         ;;; body
         (~func-component
          ~'plugin-path ~'global-configuration
          (generate-dynamic-spec [~@body])))
       (def ~func-t ~func-toolkit)))) 





