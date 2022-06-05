(ns jarman.logic.changes-service
  (:use seesaw.core
        seesaw.dev)
  (:require
   [jarman.lang :refer :all]
   [jarman.gui.gui-tools :as gtool]))

;; ┌────────────────────┐
;; │                    │
;; │ Changes controller │
;; │                    │
;; └────────────────────┘

(defn add-changes-controller
  "Description:
       Add changes controller to views. Storage only stores atoms from bigest view component.
   Example:
       (add-changes-controller view-id local-changes) where local-changes is an atom inside view/biggest component.
   "
  [changes-store view-id local-changes]
  (let [to-marge {view-id local-changes}]
    (swap! changes-store (fn [changes-atoms] (merge changes-atoms to-marge)))))

(def get-local-changes-atom
  "Description:
       Function return atom address to list of changes for view by view-id.
   Example:
       (get-changes-atom :init.edn) => {:init.edn-value-lang [[:init.edn :value :lang] EN]}
   "
  (fn [changes-store view-id]
    (get-in @changes-store [view-id])))

(def set-change-to-view-atom
  (fn [local-changes path new-value]
    (swap! local-changes (fn [changes] (merge changes {path new-value})))))

(def remove-change-from-view-atom
  (fn [local-changes path]
    (swap! local-changes (fn [changes] (dissoc changes path)))))

(def track-changes-used-components
  (fn [local-changes path-to-value event-src component-key value]
    (cond
      ;; if something was change
      (not (= (config event-src component-key) value)) (set-change-to-view-atom local-changes path-to-value (config event-src component-key))
      ;; if back to orginal value
      (not (nil? (get-in @local-changes [(gtool/convert-mappath-to-key path-to-value)]))) (remove-change-from-view-atom local-changes path-to-value))))

(def track-changes
  (fn [local-changes path-to-value orginal new-value]
    (cond
      ;; if something was change
      (not (= orginal new-value)) (set-change-to-view-atom local-changes path-to-value new-value)
      ;; if back to orginal value
      (not (nil? (get-in @local-changes [path-to-value]))) (remove-change-from-view-atom local-changes path-to-value))))

(defn new-changes-service
  []
  (let [atom--changes-store (atom {})]
    (fn [action
         & {:keys [view-id
                   local-changes
                   path-to-value
                   event-src
                   component-key
                   new-value
                   old-value]
            :or {view-id :none
                 local-changes nil
                 path-to-value []
                 event-src nil
                 component-key :text
                 value nil}}]
      (cond
        (= action :add-controller) (add-changes-controller atom--changes-store (if (keyword? view-id) view-id (keyword view-id)) local-changes)
        (= action :truck-changes)  (track-changes local-changes path-to-value old-value new-value)
        ;; (= action :truck-changes-in-component) (track-changes-used-components local-changes path-to-value event-src component-key new-value)
        (= action :get-changes-atom) atom--changes-store
        (= action :get-changes-list) @atom--changes-store
        (= action :get-local-changes-astom) (get-local-changes-atom view-id)))))
