;; jarman.gui.managment ->
(ns jarman.application.collector-custom-metacomponents
  (:require [clojure.string]
            [jarman.lang :refer [blet group-by-apply]]))

;;; ARGUMENT METADATA GETTER ;;;

(defn get-comp-actions [symb]
 (->> ((comp :keys first rest first :arglists meta resolve) symb)
      (keep #(when (clojure.string/starts-with? (str %) "on-") (keyword %)))
      (apply hash-set)))

(defn example-case [symb]
 (->> ((comp :or first rest first :arglists meta resolve) symb)
      (seq)
      (sort-by first)
      (reverse)
      (mapcat (fn [[k v]] (vector (keyword k) v)))
      (concat (list symb))))
;;;;

(def ^:private system-Components-list (ref []))
(defn ^:private system-Components-list-get [] (deref system-Components-list))
(defn ^:private system-Components-list-group-get [& [id]]
  (let [grouped (group-by-apply :id (deref system-Components-list)
                                :apply-group first)]
    (if id (get grouped id) grouped)))

(defrecord Component [id component actions constructor])

(defn- constructComponent [{:keys [id actions constructor component]}]
  (assert (keyword? id)             "Component plugin `:id` must be a 'Keyword' type")
  (assert (every? keyword? actions) "Component plugin `:description` is not 'String' type")
  (assert (fn? component)           "Component plugin `:component` MUST be a 'clojure.lang.IFn' type")
  (Component. id component actions constructor))

(defn register-metacomponent
  "Example
   register-metacomponent :id :jsgl-link :component stub :actions (get-comp-actions 'stub)"
  [& {:as args}]
  (dosync (alter system-Components-list
            (fn [persisted-component-list]
              (blet
                (conj filtered-componet component)
                [component (constructComponent args)
                 filtered-componet (filterv (fn [persited-component] (not= (:id component) (:id persited-component)))
                                     persisted-component-list)])))) true)

(defn metacomponent->component [{:keys [type] :as component-meta}]
  (if-let [{:keys [id component actions constructor]} (get (system-Components-list-group-get) type nil)]
    (apply component (-> component-meta
                       (dissoc :type)
                       ((partial apply concat))))
    (throw (ex-info (format "Component with `%s` doesn't register in system." (str type))
             {:metafield component-meta
              :type type}))))

(defn metafield->component [{{:keys [type] :as component-meta} :component :as metafiled}]
  (if-let [{:keys [id component actions constructor]} (get (system-Components-list-group-get) type nil)]
    (apply component (-> component-meta
                       (dissoc :type)
                       ((partial apply concat))))
    (throw (ex-info (format "Comopnent with `%s` doesn't register in system." (str type))
             {:metafield component-meta
              :type type}))))

(defn metacomponents-get [] (system-Components-list-group-get))


(comment
  (def v1
    (seesaw.core/vertical-panel
      :items [(seesaw.core/label :text "sukafds")]
      :border (jarman.gui.components.swing/border {:a 10})))
  (defn suka []
    (seesaw.core/config!
      (let [XS [{:field :first_name,
                 :field-qualified :user.first_name
                 :component
                 {:type :jsgl-text, :value "ldjflaskdjlsakjf"
                  :on-change (fn [e] (println "A")
                               (seesaw.core/text e))}
                 :representation "Imie"
                 :description "No kurwa jak ciebie mamka z tatkiem nazwali"}
                {:field :last_name,
                 :field-qualified :user.last_name
                 :representation "Nazwisko"
                 :description "Drugie slowo co musisz pamiętać w życiu"
                 :component
                 {:type :jsgl-text, :value "fdlaskjlk"
                  :on-change (fn [e]
                               (println "B")
                               (seesaw.core/text e))}}]]
        (for [X XS]
          (metacomponent->component (:component X))))))
  (seesaw.core/config! v1 :items (suka))
  (jarman.gui.components.swing/quick-frame
    [v1]))
