(ns jarman.config.conf-language
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

(def ^:private _language
  "Variable which store
  On first level keys 
  :_global - central translation file
  :dracula - plugin translations
  ... all another key represent plugins"
  (ref {}))

(defvar language-selected :en
  :name "Language"
  :doc "Define translation language to jarman"
  :type clojure.lang.Keyword
  :group :view-params)

(defvar language-supported [:en :pl :ua]
  :name "Supported Languages"
  :doc "Define supported language inside jarman"
  :type clojure.lang.PersistentVector
  :group :view-params)


;;;;;;;;;;;;;;;;;;;;
;;; DEBUG HEADER ;;;
;;;;;;;;;;;;;;;;;;;;

(def ^{:private true}  debug (ref false))
(defn debug-enable  [] (dosync (ref-set debug true)))
(defn debug-disable [] (dosync (ref-set debug false)))


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

(defn do-load-language []
  (let [extensions-with-translations (filter (comp seq :language) (extension-manager/extension-storage-list-get))
        central-language-file (get-language-file)]
    (dosync
     (print-line (format "loadin central language file ~%s~" (str central-language-file)))
     (ref-set _language {:_global (read-string (slurp (str central-language-file)))})
     (doall
      ;; swapping translations from all langun
      (doseq [ext extensions-with-translations]
        ;; if plugin contain languages files
        (print-line (format "Loading languages from extension ~%s~" (:name ext)))
        (alter _language assoc (keyword (:name ext))
               (reduce (fn [acc-m lang-file]
                         (print-line (format "merge file ~%s~ " (str lang-file)))
                         (deep-merge-with #(second %&) acc-m (read-string (slurp (str lang-file)))))
                       {}
                       (:language ext))))))) true)


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
      ;;=> return some translation by path.
  See also
    `plang`"
  [& translation-path]
  (let [d "<null>"
        p (into [:_global (deref language-selected)] translation-path)
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
      ;;=> return some translation by path. with context of `aaa`
  See also
    `lang`"
  [plugin & translation-path]
  (let [d "<null>"
        p (into [plugin (deref language-selected)] translation-path)
        l (get-in @_language p d)]
    (when (deref debug)
      (print-header
       (format "Debug translation in `%s`" (eval `(str *ns*)))
       (print-line (format  "translation-path %s => '%s' " (str p) l))))
    l))

(comment
  (debug-enable)
  (do-load-language)
  (lang :themes :name)
  ;; => ":themes"
  (plang :aaa :accept-button)
  ;; => "bliat"
  (debug-disable))


