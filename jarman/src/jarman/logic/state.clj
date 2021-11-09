(ns jarman.logic.state
  (:gen-class)
  (:require [seesaw.core :as c]
            [clojure.data :as data]))

(def atom-state (atom {}))

(defn set-state
  "Description:
     Set new state or update existing.
   Example:
     (set-state :mystate 42)
   "
  ([coll-map] (if (map? coll-map) (doall (map #(set-state (first %) (second %)) coll-map))))
  ([key val] (if (vector? key)
               (swap! atom-state (fn [current-state] (assoc-in current-state key val)))
               (swap! atom-state (fn [current-state] (assoc current-state key val))))))

(defn rm-state
  "Description:
     Remove from state by key.
   Example:
     (rm-state :mystate)
   "
  ([key] (swap! atom-state (fn [current-state] (dissoc current-state key)))))

(defn clear-state
  "Description:
     Remove all in state.
   Example:
     (clear-state)
   "
  [] (reset! atom-state {}))

(defn check-state
  "Description:
     Display all state keys or check if exist key.
   Example:
     (set-state :mystate 42)
     (check-state)
       ;; => (:mystate)
     (check-state :mystate)
       ;; => true
   "
  ([] (keys @atom-state))
  ([key] (contains? @atom-state key)))

(defn state
  "Description:
     Get all state in map or get state by key.
   Example:
     (set-state :mystate1 41)
     (set-state :mystate2 42)
     (state)
       ;; => {:mystate1 41 :mystate2 42}
     (state :mystate1)
       ;; => 41
   "
  
  ([] @atom-state)
  ([key] (if (vector? key)
           (get-in @atom-state key)
           (get @atom-state key))))

(defn get-atom []
  atom-state)

(defn set-global-state-watcher
  [root render-fn watch-path]
  (if (nil? (get-in @atom-state watch-path))
    (swap! atom-state #(assoc-in % watch-path nil)))
  (add-watch atom-state :watcher
             (fn [id-key state old-m new-m]
               (let [[left right same] (data/diff (get-in new-m watch-path) (get-in old-m watch-path))]
                 (if (not (and (nil? left) (nil? right)))
                   (let [root (if (fn? root) (root) root)]
                     ;; SERHII HIDE EXCEPTION
                     #_(try
                       (c/config! root :items (render-fn))
                       (.revalidate (c/to-frame root))
                       (.repaint (c/to-frame root))
                       (catch Exception e (println "\n" (str "Rerender exception:\n" (.getMessage e))) ;; If exeption is nil object then is some prolem with new component inserting
                              ))
                     (c/config! root :items (render-fn))
                     (.revalidate (c/to-frame root))
                     (.repaint (c/to-frame root))))))))

(defn new-watcher
  [atm root render-fn watch-path watcher-k]
  (if (nil? (get-in @atm watch-path))
    (swap! atm #(assoc-in % watch-path nil)))
  (add-watch atm watcher-k
             (fn [id-key state old-m new-m]
               (let [[left right same] (data/diff (get-in new-m watch-path) (get-in old-m watch-path))]
                 (if (not (and (nil? left) (nil? right)))
                   (let [root (if (fn? root) (root) root)]
                     (c/config! root :items (render-fn))
                     (.revalidate (c/to-frame root))
                     (.repaint (c/to-frame root))))))))

(comment
  (set-state {:a "a" :b "b"})
  ;; => ({:a "a"} {:a "a", :b "b"})

  (set-state :c "c")
  ;; => {:a "a", :b "b", :c "c"}

  (state)
  ;; => {:a "a", :b "b", :c "c"}

  (check-state)
  ;; => (:a :b :c)

  (check-state :a)
  ;; => true

  (state :a)
  ;; => "a"

  (rm-state :a)
  ;; => {:b "b", :c "c"}

  (clear-state)
  ;; => {}
  )
