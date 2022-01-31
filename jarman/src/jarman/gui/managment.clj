(ns jarman.gui.managment
  (:require [clojure.string]
            [jarman.tools.lang :refer [blet]]))

(defn- get-comp-actions [symb]
 (->> ((comp :keys first rest first :arglists meta resolve) symb)
      (keep #(when (clojure.string/starts-with? (str %) "on-") (keyword %)))
      (apply hash-set)))

(def ^:private system-Components-list (ref []))
(defn system-Components-list-get [] (deref system-Components-list))
(defn system-Components-list-group-get []
  (persistent!
   (reduce
    (fn [acc {:keys [id] :as component}]
      (assoc! acc id component))
    (transient {}) (deref system-Components-list))))

(defrecord Component [id component actions constructor])

(defn constructComponent [{:keys [id actions constructor component]}]
  (assert (keyword? id)             "Component plugin `:id` must be a 'Keyword' type")
  (assert (every? keyword? actions) "Component plugin `:description` is not 'String' type")
  (assert (fn? component)           "Component plugin `:component` MUST be a 'clojure.lang.IFn' type")
  (Component. id component actions constructor))

(defn register-custom-component [& {:as args}]
  (dosync (alter system-Components-list
                 (fn [persisted-component-list]
                   (blet
                    (conj filtered-componet component)
                    [component (constructComponent args)
                     filtered-componet (filterv (fn [persited-component] (not= (:id component) (:id persited-component)))
                                                persisted-component-list)])))) true)
