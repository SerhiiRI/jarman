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

(defmacro defplugin
  [plugin-name title & body]
  (let [create-name-func (fn [fname] (symbol
                                     (str "jarman-table-plugin/" plugin-name "-" fname)))
        func-component (create-name-func "component")
        func-tool (symbol (str plugin-name "-toolkit-pipeline"))
        func-toolkit (symbol (str "jarman-table-plugin/" func-tool))
        func-t (symbol (str plugin-name "-" "toolkit-pipeline"))]    
    `(do        
       (defn ~plugin-name 
         ~(generate-plugin-doc title body)
         [~'plugin-path ~'global-configuration]
         (~func-component
          ~'plugin-path ~'global-configuration 
          `(s/keys :req-un
                  [:global-plugin/permission
                   :global-plugin/name
                   :global-plugin/plug-place]
                )))
       (def ~func-t ~func-toolkit))))


