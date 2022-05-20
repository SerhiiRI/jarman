(ns jarman.gui.managment
  (:require [clojure.string]
            [jarman.tools.lang :refer [blet group-by-apply]]))

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
(defn system-Components-list-get [] (deref system-Components-list))
(defn system-Components-list-group-get [& [id]]
  (let [grouped (group-by-apply :id (deref system-Components-list)
                                :apply-group first)]
    (if id (get grouped id) grouped)))

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

(defn transform-to-object [{:keys [type] :as component-meta}]
  (if-let [{:keys [id component actions constructor]} (get (system-Components-list-group-get) type nil)]
    (apply component (-> component-meta
                         (dissoc :type)
                         ((partial apply concat))))
    (throw (ex-info (format "Comopnent with `%s` doesn't register in system." (str type))
                    {:metafield component-meta
                             :type type}))))

;; (group-by-apply :id (deref system-Components-list)
;;                 :apply-item :actions
;;                 :apply-group first)


