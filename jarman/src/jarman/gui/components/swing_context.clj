(ns jarman.gui.components.swing-context
  (:require [jarman.tools.lang :refer :all])
  (:import [javax.swing KeyStroke]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; GLOBAL CONTROL BUFFER's ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def ^:private global-keymap-buffer-var (ref {}))
(defn global-keymap [] (deref global-keymap-buffer-var))
(defn global-keymap-ref [] global-keymap-buffer-var)

(def ^:private global-key-vec-buffer-var (atom []))
(defn global-key-vec-buffer-reset []
  (reset! global-key-vec-buffer-var []))
(defn global-key-vec-buffer-put [^KeyStroke key-stroke]
  (swap! global-key-vec-buffer-var conj key-stroke))
(defn global-key-vec-buffer []
  (deref global-key-vec-buffer-var))

;;;;;;;;;;;;;;;;;;;;;;;;
;;; CONTEX VARIABLES ;;;
;;;;;;;;;;;;;;;;;;;;;;;;

(def ^{:dynamic true :type javax.swing.JComponent} *active-commponent* nil) 
(def ^{:dynamic true :type javax.swing.JPanel} *active-default* nil) 
(def ^{:dynamic true :type javax.swing.JFrame} *active-frame* nil)
(def ^{:dynamic true :type clojure.lang.PersistentHashMap} *active-keymap* nil)
(defn ^clojure.lang.PersistentHashMap active-keymap [] (rift *active-keymap* (global-keymap)))
(defn ^javax.swing.JComponent active-component [] *active-commponent*)
(defn ^javax.swing.JPanel active-default [] *active-default*)
(defn ^javax.swing.JFrame active-frame [] *active-frame*)
(defmacro with-keymap       [keymap & body]
  `(binding [*active-keymap* ~keymap] ~@body))
(defmacro with-active-event [event  & body]
  `(binding [*active-commponent* (.getSource ~event)
             *active-default* (.getContentPane (javax.swing.SwingUtilities/getRoot (.getSource ~event)))
             *active-frame* (javax.swing.SwingUtilities/getRoot (.getSource ~event))]
     ~@body))


