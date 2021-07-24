(ns jarman.config.dot-jarman-param
  (:gen-class)
  (:import (java.io IOException))
  (:require
   ;; Clojure
   [clojure.string :as s]
   [clojure.pprint :refer [cl-format]]
   [clojure.java.io :as io]
   [clojure.data :as cl-data]
   ;; Jarman 
   [jarman.tools.lang :refer :all]
   [jarman.config.environment :as env]))

;;;;;;;;;;;;;;;;
;;; INTERNAL ;;;
;;;;;;;;;;;;;;;;

;;; Variable Stack

(def --jarman-variable-stack-- (atom {}))
;; (add-watch --jarman-variable-stack-- :log-difference
;;            (fn [_ref _ old-value new-value]
;;              (let [[things-only-in-a things-only-in-b things-in-both]
;;                    (cl-data/diff old-value new-value)]
;;                (if (or things-only-in-a things-only-in-b)
;;                  (println things-only-in-b)))))

;;; Declare type

(defn jarman-ref [e kwd]
 (proxy [clojure.lang.Ref] [e]
   (set [^java.lang.Object val]
     (if val
       (swap! --jarman-variable-stack--
              (fn [m] (assoc-in m [kwd :loaded] true))))
     (proxy-super set val))))

;;;;;;;;;;;;;;;;;;;;
;;; DEFVAR, SETQ ;;;
;;;;;;;;;;;;;;;;;;;;

(defmacro defvar [variable-name default-value & {:as params}]
  `(do
     (def ~variable-name (jarman-ref ~default-value (keyword (symbol (var ~variable-name)))))
     (swap!
      --jarman-variable-stack--
      (fn [m#] (assoc-in m# [(keyword (symbol (var ~variable-name)))]
                        (merge {:link (var ~variable-name)
                                :ns (ns-name *ns*)
                                :loaded false
                                :type nil
                                :group :global}
                               ~params))))
     (alter-meta!
      (var ~variable-name)
      #(assoc-in % [:variable-stack-reference]
                 (fn [] (get-in (deref jarman.config.dot-jarman-param/--jarman-variable-stack--) [(keyword (symbol (var ~variable-name)))]))))
     (alter-meta!
      (var ~variable-name)
      #(assoc-in % [:variable-params] (merge {:type nil :group :global} ~params)))
     nil))

(defmacro setq [& pair-args-list]
  (assert (even? (count pair-args-list)))
  `(dosync
    ~@(for [[symb value] (partition-all 2 pair-args-list)]
        `(ref-set ~symb ~value))))

;;;;;;;;;;;;;;;;;;;;;;;;
;;; helper functions ;;;
;;;;;;;;;;;;;;;;;;;;;;;;

(defn variable-list-all []
  (deref --jarman-variable-stack--))
(defn variable-list-loaded []
  (reduce (fn [acc [k v]] (into acc {k v})) {}
   (filter (fn [[k v]] (:loaded v)) (deref --jarman-variable-stack--))))
(defn variable-list-not-loaded []
  (reduce (fn [acc [k v]] (into acc {k v})) {}
   (filter (fn [[k v]] (not (:loaded v))) (deref --jarman-variable-stack--))))
(defn variable-gruop-by-group []
  (map #(vector (first %) (apply hash-map (apply concat (second %))))
       (seq (group-by (comp :group second) (deref --jarman-variable-stack--)))))
(defn variable-gruop-by-group-not-loaded []
  (map #(vector (first %) (apply hash-map (apply concat (second %))))
     (seq (group-by (comp :group second) (variable-list-not-loaded)))))
(defn print-list-not-loaded []
  (if-let [grouped-variable-list (not-empty (variable-gruop-by-group-not-loaded))]
    (do (println "Warning! Not used variables (:group-name|[:values-list]):")
        (println (cl-format nil "~{~A~}"
                            (for [[group-name-kwd variables-kwdx] grouped-variable-list
                                  :let [group-name (name group-name-kwd)]]
                              (cl-format nil "  :~A~%~{    ~A ~%~}" group-name (map symbol (keys variables-kwdx)))))))))


