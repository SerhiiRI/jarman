(ns jarman.config.vars
  (:gen-class)
  (:import (java.io IOException))
  (:require
   ;; External
   [rewrite-clj.zip :as z]
   [rewrite-clj.node :as n]
   ;; Clojure
   [clojure.java.io :as io]
   [clojure.pprint :refer [cl-format]]
   ;; [clojure.data    :as cl-data]
   ;; Jarman 
   [jarman.lang :refer :all]
   [jarman.org  :refer :all]
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

(defmacro defvar
  "Description
    Define system variable.
  Example
    ;; Short declaration
     (defvar some-string-var \"value\")
    ;; Full declaration
     (defvar some-string-var \"value\"
       :name \"Optinal variable name for presentation in view\"
       :doc \"Some optinal information about var\"
       :type java.lang.String
       :group :global)"
  [variable-name default-value & {:as params}]
  (assert (not (contains? params :link)) (format "Define variable `%s`. Option `:link` is system" (name variable-name)))
  (assert (not (contains? params :ns)) (format "Define variable `%s`. Option `:ns` is system" (name variable-name)))
  (assert (not (contains? params :loaded)) (format "Define variable `%s`. Option `:loaded` is system" (name variable-name)))
  `(do
     (def ~variable-name (jarman-ref ~default-value (keyword (symbol (var ~variable-name)))))
     (swap!
      --jarman-variable-stack--
      (fn [m#] (assoc-in m# [(keyword (symbol (var ~variable-name)))]
                        (merge {:link (var ~variable-name)
                                :name nil
                                :doc nil
                                :ns (ns-name *ns*)
                                :loaded false
                                :type nil
                                :group :global}
                               ~params))))
     (alter-meta!
      (var ~variable-name)
      #(assoc-in % [:variable-stack-reference]
                 (fn [] (get-in (deref jarman.config.vars/--jarman-variable-stack--) [(keyword (symbol (var ~variable-name)))]))))
     (alter-meta!
      (var ~variable-name)
      #(assoc-in % [:variable-params] (merge {:type nil :group :global} ~params)))
     nil))

(defmacro setq [& pair-args-list]
  (assert (even? (count pair-args-list)))
  `(dosync
    ~@(for [[symb value] (partition-all 2 pair-args-list)]
        `(ref-set ~symb ~value))))

(defmacro variable-config-list [& var-list]
  `(jarman.config.vars/setq
    ~@var-list))

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
    (do (print-line "Warning! Not used variables (:group-name|[:values-list]):")
        (print-multiline (cl-format nil "~{~A~}"
                                    (doall
                                     (for [[group-name-kwd variables-kwdx] grouped-variable-list
                                           ;; grouped-variable-list
                                           :let [group-name (name group-name-kwd)]]
                                       (cl-format nil "  :~A~%~{    ~A~^~%~}" group-name (map symbol (keys variables-kwdx))))))))))

;;;;;;;;;;;;;;;;;;;;;;;
;;; .JARMAN MANAGER ;;;
;;;;;;;;;;;;;;;;;;;;;;;

(defn insert-tutorial [zloc-l]
  (reduce (fn [acc string-comment]
            (-> acc
                (z/insert-left (n/comment-node string-comment))
                (z/insert-newline-left)))
  zloc-l
  ["; This is Main configuration `.jarman` file. Declare in it any machine or app-specyfic configurations"
   "; Set global variables, by using `setq` macro and power of jarman customizing system"
   "; "
   "; 1. Declare variable in any place you want in system"
   ";    Attribute `:type` and `:group` aren't required, but"
   ";    specifing may help you in debug moment"
   "; "
   ";    => (defvar some-global-variable nil "
   ";          :type clojure.lang.PersistentArrayMap"
   ";          :group :logical-group)"
   "; "
   "; 2. In `.jarman` set previosly declared in code variable"
   "; "
   ";    => (setq some-global-variable {:some-value {:a 1}})"]))

(defn create-new-file [zloc]
  (-> zloc
      (z/insert-child '(variable-config-list))
      (z/down)
      (z/insert-left (n/comment-node "; -*- mode: auto-revert; mode: Clojure;-*-"))
      (z/insert-newline-left)
      (z/insert-newline-left)
      (insert-tutorial)
      (z/insert-newline-left)
      (z/insert-left (z/sexpr (z/of-string "(require '[jarman.config.vars :refer [setq]])")))
      (z/insert-newline-left)
      (z/insert-newline-left)
      
      (z/down)
      (z/insert-space-right)
      (z/insert-newline-right)
      (z/insert-right (n/comment-node "; Define your variable here"))
      (z/insert-newline-right)
      (z/up)
      (z/down)))

;;; UNSTABLE TO FIX
;; (defn create-variable-config-list-if-not-exist [zloc]
;;   (-> (z/skip z/right
;;               #(and (= 'require (first (z/sexpr %)))
;;                   (some? (z/right %)))
;;               zloc)
;;       (z/left)
;;       (z/insert-right '(variable-config-list))
;;       ;; (z/insert-newline-right)
;;       (z/right)
;;       (z/insert-newline-left)
;;       (z/down)
;;       (z/insert-space-right)
;;       (z/insert-newline-right)
;;       (z/insert-right (n/comment-node "; Define your variable here"))
;;       (z/insert-newline-right)
;;       (z/up)
;;       (z/right)
;;       (z/insert-newline-left)
;;       (z/left)
;;       (z/insert-newline-left)))

(defn find-variable-config-list [zloc]
  (-> zloc
      (z/find-value z/next 'variable-config-list)
      (z/find-next-value z/next 'variable-config-list)
      z/up))

(defn set-variable [zloc variable-symbol variable-value]
  (if-let [x (-> zloc
                 (z/find-value z/next 'variable-config-list)
                 (z/find-value z/next variable-symbol))]
    (-> x
        (z/right)
        (z/replace variable-value))
    (-> zloc
        (z/find-value z/next 'variable-config-list)
        (z/rightmost)
        (z/insert-right variable-value)
        (z/insert-right variable-symbol)
        (z/insert-newline-right))))

(defn persist-variable-in-jarman [variable-symbol variable-value]
  (let [zloc (z/of-string (slurp (env/get-jarman)))
        changed-struct
        (if-let [zloc (find-variable-config-list zloc)]
          (set-variable zloc variable-symbol variable-value)
          (if (z/sexpr zloc)
            (throw (ex-info "Sexp variable-config-list doesn't exist" {}))
            (set-variable (create-new-file zloc)
                          variable-symbol variable-value)))]
    (if changed-struct     
      (->> changed-struct
           (z/root-string)
           (spit (env/get-jarman)))
      (throw (ex-info "Error when persist variable to `.jarman`" {})))))

(defmacro setj [& pair-args-list]
  (assert (even? (count pair-args-list)))
  `(do
     ~@(for [[symb value] (partition-all 2 pair-args-list)]
         (do (assert (symbol? symb))
             `(jarman.config.vars/persist-variable-in-jarman (quote ~symb) ~value)))))


