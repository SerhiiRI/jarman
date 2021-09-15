(ns jarman.config.conf-langauge
  (:require
   [clojure.java.io :as io]
   [jarman.config.environment :as env]
   [jarman.tools.lang :refer :all]
   [jarman.tools.org  :refer :all]
   [jarman.config.vars :refer [defvar]]
   [jarman.plugin.extension-manager :as extension-manager])
  (:import (java.io IOException FileNotFoundException)))

;;;;;;;;;;;;
;;; VARS ;;;
;;;;;;;;;;;;

(defvar language-selected :en
  :name "Language"
  :doc "Define translation language to jarman"
  :type String
  :group :view-params)

;;;;;;;;;;;;;;;;;;;;
;;; DEBUG HEADER ;;;
;;;;;;;;;;;;;;;;;;;;

(def ^{:private true} debug (ref false))
(defn debug-enable [] (dosync (ref-set debug true)))
(defn debug-disable [] (dosync (ref-set debug false)))
(debug-enable)
(debug-disable)


;;;;;;;;;;;;;;;;;;;;;;
;;; CONFIGURATIONS ;;;
;;;;;;;;;;;;;;;;;;;;;;

(def ^:private jarman-configs-dir-list
  "list of all configururations directory in client filesystem"
  [(io/file env/user-home ".jarman.d" "config")
   (io/file           "." ".jarman.d" "config")])

(defn ^:private get-language-file []
  (let [lang-file-name "language.edn"]
   (if-let [loading-path (first (filter #(.exists %) jarman-configs-dir-list))]
     (if (.exists (io/file loading-path lang-file-name))
       (io/file loading-path lang-file-name)
       (throw (FileNotFoundException.
               (format "Language file `%s` wasn't not found" (str (io/file loading-path lang-file-name))))))
     (throw (FileNotFoundException.
               "Any of \"config\" folder wasn't register in computer")))))

(debug-enable)
(def language-supported)
(filter :language (extension-manager/extension-storage-list-get))

(def ^:private _language (ref {}))
(defn ^:private _into_language [plugin-language-edn]
  (deep-merge-with #(second %&) {:a {:a 1}} {:a {:a 2 :b 2}})
  (deep-merge-with second {:a 1} {:b 1}))


(defn do-load-language []
  (dosync (ref-set _language (read-string (slurp (str (get-language-file))))))
  true)

(do-load-language)

;;;;;;;;;;;;;;;;
;;; LOGISTIC ;;;
;;;;;;;;;;;;;;;;

(defn lang
  "Documentation
    Get languages depending on `selected-language`
    If not exist value in `translation-path` get
    default error text \"<null>\"
  Example
    (lang)
      ;;=> return all map in some language
    (lang :buttons :accept)
      ;;=> return some translation by path. "
  [& translation-path]
  (let [d "<null>"
        p (into [(deref language-selected)] translation-path)
        l (get-in @_language p d)]
    (when (deref debug)
      (print-header
       (format "Debug translation in `%s`" (eval `(str *ns*)))
       (print-line (format  "translation-path %s => '%s' " (str p) l))))
    l))

(defn plang
  "Documentation
    The same as simple lang, but maked for
    getting translation's from plugins
  Example
    (plang :aaa)
      ;;=> return all map in some language for plugin `aaa`
    (plang :aaa :buttons :accept)
      ;;=> return some translation by path. with context of `aaa` "
  [plugin & translation-path]
  (apply lang (deref language-selected) plugin (deref language-selected) translation-path))

(debug-enable)
(plang :aaa :accept)
