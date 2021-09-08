(ns jarman.plugin.plugin-manager
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
   [jarman.config.dot-jarman-param :refer [defvar]])
  (:import (java.io IOException FileNotFoundException)))


;;;;;;;;;;;;;;;;;;;;;;
;;; CONFIGURATIONS ;;;
;;;;;;;;;;;;;;;;;;;;;;

(def ^:dynamic *debug-to-file* false)
(def ^:dynamic *debug-file-name* "plugin-manager-log.org")

(defvar jarman-plugin-list '(aaa)
  :type clojure.lang.PersistentList
  :group :plugin-system)

(def ^:private jarman-plugins-dir-list
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
          "#+TITLE: Plugin manager Log file\n#+AUTHOR: Serhii Riznychuk\n#+EMAIL: sergii.riznychuk@gmail.com\n#+STARTUP: overview\n")))

(defmacro ^:private with-out-debug-file [& body]
  `(binding [*out* (if *debug-to-file*
                     (do
                       (create-log-file)
                       (clojure.java.io/writer *debug-file-name* :append true))
                     *out*)]
     (do
       ~@body)))


;;;;;;;;;;;;;;;;;;;;
;;; PandaPackage ;;;
;;;;;;;;;;;;;;;;;;;;

(defprotocol IPluginLoader
  (do-load [this]))
(defrecord PandaPackage [name description package-path version authors license keywords url loading-seq]
  IPluginLoader
  (do-load [this]
    (println (format "** load ~%s~" name))
    (doall
     (doseq [loading-file loading-seq
             :let [f (io/file package-path (format "%s.clj" (str loading-file)))]]
       (if (.exists f)
         (do
           (println (format "*** compiling ~%s~" (str f)))
           (let [file-output (with-out-str (load-file (str f)))]
             (if (not-empty file-output)
               (do
                 (cl-format *out* "~,,3<compilling file output~> ~%")
                 (cl-format *out* "~,,3<~A~> ~%" "#+begin_example")
                 (doall (map (partial cl-format *out* "~,,4<~A~>~%") (clojure.string/split file-output #"\n")))
                 (cl-format *out* "~,,3<~A~> ~%" "#+end_example")))))
         (throw (FileNotFoundException.
                 (format "Plugin `%s`. Loading sequnce `%s` not exist"
                         name loading-file))))))))
(defn constructPandaPackage [name description path package-m]
  (-> package-m
      (assoc :name name)
      (assoc :description description)
      (assoc :package-path path)
      map->PandaPackage))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; SYSTEM VARS, LOADERS ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(def ^:private package-storage-list (ref []))
(defn          package-storage-list-get  [] (deref package-storage-list))
(defn          package-storage-list-load []
  (if-let [loading-path (first (filter #(.exists %) jarman-plugins-dir-list))]
    (do
      (dosync (ref-set package-storage-list []))   
      (doseq [package (deref jarman-plugin-list)]
        (let [package-path (io/file loading-path (str package) "package")]
          (if (.exists package-path)
            (let [define-package-body (rest (read-string (slurp (io/file loading-path (str package) "package"))))
                  [name description] (take 2 define-package-body)
                  package-map-sequence (drop 2 define-package-body)]
              (assert (even? (count package-map-sequence))
                      (format "Odd config keywords in `%s` plugin declaration"
                              (str package)))
              (dosync
               ;; wrapp into a package 
               ;;=> (:verions ...) => #PandaPackage{:version ...}
               (alter package-storage-list conj
                      (constructPandaPackage
                       name
                       description
                       (io/file loading-path (str package))
                       (eval (apply hash-map package-map-sequence))))))
            (throw (FileNotFoundException.
                    (format "Package `%s` doesn't contain declaration")))))))
    (throw (FileNotFoundException.
            (format "Any plugin loading path [%s] doesn't exists in system"
                    (clojure.string/join
                     ", " (map str jarman-plugins-dir-list)))))))

(defn do-load-plugins
  ([]
   (package-storage-list-load)
   (with-out-debug-file
     (println (format "* Loading plugins (%s)" (quick-timestamp)))
     (println (format " Total plugin count: ~%d~" (count (deref package-storage-list))))
     (doall (map do-load @package-storage-list))))
  ([& panda-packages]
   (with-out-debug-file
     (println (format "* Reload plugins (%s)" (quick-timestamp)))
     (doall (map do-load panda-packages)))))

(comment
  (package-storage-list-load)
  (package-storage-list-get)
  (do-load-plugins))

