(ns jarman.logic.state2
  (:require [seesaw.core :as c]
            [clojure.data :as data]
            [jarman.lang :refer :all]))

(def ^:private global-gui-component-state (atom {}))
(def ^:dynamic *session-frame* nil)
(defn return-global-gui-component-state [] (deref global-gui-component-state))
(defn active-session-frame [] *session-frame*)

(defn state
  ([]
   (if *session-frame*
     (get (deref global-gui-component-state) *session-frame*)))
  ([kvs]
   (if *session-frame*
     (-> (deref global-gui-component-state)
         (get *session-frame*)
         (get-in kvs)))))

(defn state!
  ([f & args]
   (let [m (rift (state) {})
         new-state (apply f m args)]
     (swap! global-gui-component-state update *session-frame* (fn [_] new-state)))))

(defmacro with-session-frame [frame-key & body]
  `(binding [jarman.logic.state2/*session-frame* :s01]
    ~@body))
