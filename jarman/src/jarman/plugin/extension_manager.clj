(ns jarman.plugin.extension-manager
  (:require
   ;; -------------------
   [clojure.data :as data]
   [clojure.string :as string]
   [clojure.spec.alpha :as s]
   [clojure.pprint :as pprint]
   [clojure.java.jdbc :as jdbc]
   ;; Seesaw components
   [seesaw.core   :as c]
   [seesaw.dev :as sdev]
   [seesaw.mig :as smig]
   [seesaw.swingx  :as swingx]
   [seesaw.chooser :as chooser]
   [seesaw.border  :as b]
   ;; Jarman toolkit
   [jarman.tools.lang :refer :all]
   [jarman.tools.swing :as stool]
   [jarman.tools.org :refer :all]
   [jarman.resource-lib.icon-library :as icon]
   [jarman.gui.gui-tools      :as gtool]
   [jarman.gui.gui-editors    :as gedit]
   [jarman.gui.gui-components :as gcomp]
   [jarman.gui.gui-migrid     :as gmg]
   [jarman.gui.gui-calendar   :as calendar]
   [jarman.gui.popup :as popup]
   [jarman.logic.composite-components :as lcomp]
   [jarman.logic.state :as state]
   [jarman.logic.metadata :as mt]
   [jarman.logic.document-manager :as doc]
   [jarman.plugin.spec :as spec]
   [jarman.plugin.data-toolkit :as query-toolkit]
   [jarman.plugin.gui-table :as gtable]
   [jarman.plugin.plugin]
   ;; -------------------
   
   [clojure.java.io :as io] 
   [clojure.string]
   [clojure.pprint :refer [cl-format]]
   [jarman.config.environment :as env]
   [jarman.tools.lang :refer :all]
   [jarman.config.vars :refer [defvar]])
  (:import (java.io IOException FileNotFoundException)))


;;;;;;;;;;;;;;;;;;;;;;
;;; CONFIGURATIONS ;;;
;;;;;;;;;;;;;;;;;;;;;;

(def ^:dynamic *debug-to-file* false)
(def ^:dynamic *debug-file-name* "extension-manager-log.org")

(defvar jarman-extension-list '(aaa)
  :type clojure.lang.PersistentList
  :group :plugin-system)

(def ^:private jarman-extensions-dir-list
  "List of all plugins directory in client filesystem"
  [(io/file env/user-home ".jarman.d" "plugins")
   (io/file "."           ".jarman.d" "plugins")])

;;;;;;;;;;;;;;;
;;; HELPERS ;;; 
;;;;;;;;;;;;;;;

(defn ^:private quick-timestamp []
  (.format (java.text.SimpleDateFormat. "YYYY-MM-dd HH:mm") (java.util.Date.)))

(defn ^:private create-log-file []
  (if (not (.exists (clojure.java.io/file *debug-file-name*)))
    (spit *debug-file-name*
          "#+TITLE: Extension manager Log file\n#+AUTHOR: Serhii Riznychuk\n#+EMAIL: sergii.riznychuk@gmail.com\n#+STARTUP: overview\n")))

(defmacro ^:private with-out-debug-file [& body]
  `(binding [*level* 0
             *out* (if *debug-to-file*
                     (do
                       (create-log-file)
                       (clojure.java.io/writer *debug-file-name* :append true))
                     *out*)]
     (do
       ~@body)))

;;;;;;;;;;;;;;;;;;;;
;;; PandaExtension ;;;
;;;;;;;;;;;;;;;;;;;;

(defprotocol IPluginLoader
  (do-load [this]))
(defrecord PandaExtension [name description extension-path version authors license keywords url loading-seq]
  IPluginLoader
  (do-load [this]
    (print-header
     (format "load ~%s~" name)
     (doall
      (doseq [loading-file loading-seq
              :let [f (io/file extension-path (format "%s.clj" (str loading-file)))]]
        (if (.exists f)
          (do
            (print-header
             (format "compiling ~%s~" (str f))
             (let [file-output (with-out-str (load-file (str f)))]
               (if (not-empty file-output)
                 (do
                   (print-line "plugin compiling output")
                   (print-example file-output))))))
          (throw (FileNotFoundException.
                  (format "Extension `%s`. Loading sequnce `%s` not exist"
                          name loading-file)))))))))
(defn constructPandaExtension [name description path extension-m]
  (-> extension-m
      (assoc :name name)
      (assoc :description description)
      (assoc :extension-path path)
      map->PandaExtension))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; SYSTEM VARS, LOADERS ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def ^:private extension-storage-list (ref []))
(defn          extension-storage-list-get  [] (deref extension-storage-list))
(defn          extension-storage-list-load []
  (if-let [loading-path (first (filter #(.exists %) jarman-extensions-dir-list))]
    (do
      (dosync (ref-set extension-storage-list []))   
      (doseq [extension (deref jarman-extension-list)]
        (let [extension-path (io/file loading-path (str extension) "package")]
          (if (.exists extension-path)
            (let [define-extension-body (rest (read-string (slurp (io/file loading-path (str extension) "package"))))
                  [name description] (take 2 define-extension-body)
                  extension-map-sequence (drop 2 define-extension-body)]
              (assert (even? (count extension-map-sequence))
                      (format "Odd config keywords in `%s` plugin declaration"
                              (str extension)))
              (dosync
               ;; wrapp into a extension 
               ;;=> (:verions ...) => #PandaExtension{:version ...}
               (alter extension-storage-list conj
                      (constructPandaExtension
                       name
                       description
                       (io/file loading-path (str extension))
                       (eval (apply hash-map extension-map-sequence))))))
            (throw (FileNotFoundException.
                    (format "Extension `%s` doesn't contain declaration" extension)))))))
    (throw (FileNotFoundException.
            (format "Any plugin loading path [%s] doesn't exists in system"
                    (clojure.string/join
                     ", " (map str jarman-extensions-dir-list)))))))

(defn do-load-extensions
  ([]
   (extension-storage-list-load)
   (with-out-debug-file
     (print-header
      (format "Loading extensions (%s)" (quick-timestamp))
      (print-line (format "Total extensions count: ~%d~" (count (deref extension-storage-list))))
      (doall (map do-load @extension-storage-list)))))
  ([& panda-extensions]
   (with-out-debug-file
     (print-header
      (format "Reload extensions (%s)" (quick-timestamp))
      (doall (map do-load panda-extensions))))))

(comment
  (extension-storage-list-load)
  (extension-storage-list-get)
  (do-load-extensions))


;; (def xs '({:n "2"
;;            :d ["4"]}
;;           {:n "1"
;;            :d ["2" "3"]}
;;           {:n "3"
;;            :d []}
;;           {:n "4"
;;            :d []}))

;; 2->4
;; 1->2->4
;;  ->3
;; 3
;; 4

;; (defn search-deps [n]
;;   (first (remove #(not= n (:n %)) xs)))
;; (defn remove-deps [n]
;;   (vec (remove #(= n (:n %)) xs)))

;; (search-deps "2")

;; (throw (ex-info (format "Extension error. Not found dependency `%s` for `%s` plugin"
;;                             n ext)
;;                     {:type :undefinied-face-var
;;                      :var face-variable}))

;; (fn ext-load [{:keys [loaded extension-list]}]
;;   (let [extension (first extension-list)]
;;     (if (seq (:d extension))
;;       (contains? loaded (first ()))))
  
;;   (for (contains? loaded (:n extension))))

(comment dupa)
