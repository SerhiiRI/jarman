(ns jarman.logic.state)

(def atom-state (atom {}))

(defn set-state
  "Description:
     Set new state or update existing.
   Example:
     (set-state :mystate 42)
   "
  ([coll-map] (if (map? coll-map) (doall (map #(set-state (first %) (second %)) coll-map))))
  ([key val] (swap! atom-state (fn [current-state] (assoc current-state key val)))))

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
  ([key] (get @atom-state key)))


;; (set-state {:a "a" :b "b"})
;; ;; => ({:a "a"} {:a "a", :b "b"})

;; (set-state :c "c")
;; ;; => {:a "a", :b "b", :c "c"}

;; (state)
;; ;; => {:a "a", :b "b", :c "c"}

;; (check-state)
;; ;; => (:a :b :c)

;; (check-state :a)
;; ;; => true

;; (state :a)
;; ;; => "a"

;; (rm-state :a)
;; ;; => {:b "b", :c "c"}

;; (clear-state)
;; ;; => {}
