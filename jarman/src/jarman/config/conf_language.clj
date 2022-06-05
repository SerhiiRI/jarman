(ns jarman.config.conf-language
  (:require
   [clojure.java.io :as io]
   [jarman.config.environment :as env]
   [jarman.lang :refer :all]
   [jarman.tools.org  :refer :all])
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

;;;;;;;;;;;;;;;;;;;;
;;; DEBUG HEADER ;;;
;;;;;;;;;;;;;;;;;;;;

(def ^{:private true}  debug (ref false))
(defn debug-enable  [] (dosync (ref-set debug true)))
(defn debug-disable [] (dosync (ref-set debug false)))

;;;;;;;;;;;;;;;;;;;;;;
;;; CONFIGURATIONS ;;;
;;;;;;;;;;;;;;;;;;;;;;

(defn do-load-language []
  (try
    (dosync (ref-set
             _language
             {:_global
              (-> (env/get-configs-language-file)
                  (slurp) (read-string))}))
    (print-line "system languages are loaded")
    (catch FileNotFoundException e
      (seesaw.core/alert e (.getMessage e))
      ;; (java.lang.System/exit 0)
      )
    (catch Exception e
      (seesaw.core/alert (with-out-str (clojure.stacktrace/print-stack-trace e 20)))
      ;; (java.lang.System/exit 0)
      ))
  true)

(defn register-new-translation [plugin-name translation-m]
  {:pre [(map? translation-m) (keyword? plugin-name)]}
  (dosync (alter _language assoc plugin-name translation-m)))

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
        p (into [:_global (deref jarman.variables/language-selected)] translation-path)
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
        p (into [plugin (deref jarman.variables/language-selected)] translation-path)
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
  (lang)
  ;; => ":themes"
  (plang :aaa :accept-button)
  ;; => "bliat"
  (debug-disable))


