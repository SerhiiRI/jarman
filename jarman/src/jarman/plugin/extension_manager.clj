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
(def ^:dynamic *debug-file-name* "extension.log.org")

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
     (do ~@body)))

;;;;;;;;;;;;;;;;;;;;;;
;;; PandaExtension ;;;
;;;;;;;;;;;;;;;;;;;;;;

(defn assert-sources [^String extension-name file-list]
  (doall
   (doseq [file file-list]
     (when-not (.exists file)
       (throw (FileNotFoundException.
               (format "Extension `%s`. Loading sequnce `%s` not exist" extension-name (str file))))))))

(defn assert-languages [^String extension-name file-list]
  (doall
   (doseq [file file-list]
     (when-not (.exists file)
       (throw (FileNotFoundException.
               (format "Extension `%s`. Language file `%s` not exist" extension-name (str file))))))))

(defprotocol IPluginLoader
  (do-load [this]))
(defrecord PandaExtension [name description extension-path version authors license keywords url dependencies loading-seq language]
  IPluginLoader
  (do-load [this]
    (print-header
     (format "load ~%s~" name)
     ;; Testing language and sources files
     (print-line "testing translation files")
     (assert-sources name loading-seq)
     (print-line "testing extension source files")
     (assert-languages name language)
     ;; Compiling source files
     (doall
      (doseq [f loading-seq]
        (print-header
         (format "compiling ~%s~" (str f))
         (let [file-output (with-out-str (load-file (str f)))]
           (if (not-empty file-output)
             (do
               (print-line "plugin compiling output")
               (print-example file-output))))))))))

(defn constructPandaExtension [name description path extension-m]
  (-> extension-m
      (assoc  :name name)
      (assoc  :description description)
      (assoc  :extension-path path)
      (update :loading-seq (partial mapv (fn [symb] (io/file path (format "%s.clj" (str symb))))))
      (update :language    (partial mapv (fn [symb] (io/file path (format "%s.edn" (str symb))))))
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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; COMPILE WITH DEPENDENCIEST ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- remove-deps [extension-name extension-list]
  (vec (remove #(= extension-name (:name %)) extension-list)))

(defn- replace-on-first [extension-name extension-list]
  (concat
   [(if-let [e (first (remove #(not= extension-name (:name %)) extension-list))]
      e (throw (ex-info (format "Extension error. Not found dependency `%s`" extension-name)
                        {:type :deps-undef})))]
   (remove-deps extension-name extension-list)))

(defn- was-loaded? [extension-name extension-list]
  (some? (first (remove #(not= extension-name (:name %)) extension-list))))

(defn- was-locked? [extension-name extension-list]
  (if (and (some? (first (remove #(not= extension-name (:name %)) extension-list)))
         (not= extension-name (:name (last extension-list))))
    (throw (ex-info (format "Extension error. Circular dependencies `%s` in `%s`" extension-name (:name (last extension-list)))
                    {:type :deps-circular-load}))))

(defn- compile-with-deps
  [& {:keys [extension-loaded
             extension-locked
             extension-list]}]
  (print-line "Extension loading debug")
  (print-line (cl-format nil "loaded (~{~a~^,~})" (map :name extension-loaded)))
  (print-line (cl-format nil "locked (~{~a~^,~})" (map :name extension-locked)))
  (print-line (cl-format nil "waiter (~{~a~^,~})" (map :name extension-list)))
  (when (seq extension-list)
    (let [[extension & rest-extensions] extension-list]
      (if (seq (:dependencies extension))
        (if (was-loaded? (first (:dependencies extension)) extension-loaded)
          (do
            ;; 
            (compile-with-deps
             :extension-loaded extension-loaded
             :extension-locked extension-locked
             :extension-list   (conj rest-extensions (update-in extension [:dependencies] rest))))
          (do
            ;; 
            (was-locked? extension extension-locked)
            (compile-with-deps
             :extension-loaded extension-loaded
             :extension-locked (conj extension-locked extension)
             :extension-list   (replace-on-first (first (:dependencies extension)) extension-list))))
        (do
          ;; COMPILE EXTENSION
          (.do-load extension)
          ;; RECUR (EXTENSION . REST-EXTENSION)
          ;; WITH REST-EXTENSION
          (compile-with-deps
           :extension-loaded (conj extension-loaded extension)
           :extension-locked (remove-deps (:name extension) extension-locked)
           :extension-list   rest-extensions))))))

(defn do-load-extensions
  ([]
   (extension-storage-list-load)
   (with-out-debug-file
     (print-header
      (format "Loading extensions (%s)" (quick-timestamp))
      (print-line (format "Total extensions count: ~%d~" (count (deref extension-storage-list))))
      ;; COMPILE SEQUENTIAL 
      ;; (doall (map do-load @extension-storage-list))
      ;; COMPILE WITH DEPS
      (compile-with-deps
       :extension-loaded []
       :extension-locked []
       :extension-list @extension-storage-list))))
  ([& panda-extensions]
   (with-out-debug-file
     (print-header
      (format "Reload extensions (%s)" (quick-timestamp))
      (doall (map (fn [e] (.do-load e)) panda-extensions))))))

(comment
  (extension-storage-list-load)
  (extension-storage-list-get)
  (do-load-extensions))


;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; DEPENDECY ALGORYTHM ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment
  (defn remove-deps [n lst]
    (vec (remove #(= n (:n %)) lst)))

  (defn replace-on-first [n lst]
    (concat
     [(if-let [d (first (remove #(not= n (:n %)) lst))]
        d (throw (ex-info (format "Extension error. Not found dependency `%s`" n)
                          {:type :deps-undef})))]
     (remove-deps n lst)))

  (defn was-loaded? [n lst]
    (some? (first (remove #(not= n (:n %)) lst))))

  (defn was-locked? [n lst]
    (if (and (some? (first (remove #(not= n (:n %)) lst)))
           (not= n (:n (last lst))))
      (throw (ex-info (format "Extension error. Circular dependencies `%s` in `%s`" n (:n (last lst)))
                      {:type :deps-circular-load}))))

  (defn ext-load [& {:keys [loaded locked extension-list]}]
    (println (format "[Loaded|%s] [Locked| %s] [Waiting|%s]"
                     (clojure.string/join "," (map :n loaded))
                     (clojure.string/join "," (map :n locked))
                     (clojure.string/join "," (map :n extension-list))))
    (when (seq extension-list)
      (let [[extension & rest-extensions] extension-list]
        (if (seq (:d extension))
          (do
            
            (if (was-loaded? (first (:d extension)) loaded)
              (do
                
                (ext-load :loaded loaded
                          :locked locked
                          :extension-list (conj rest-extensions (update-in extension [:d] rest))))
              (do
                (was-locked? (:n extension) locked)
                (ext-load :loaded loaded
                          :locked (conj locked extension)
                          :extension-list (replace-on-first (first (:d extension)) extension-list)))))
          (do
            ;; (println (:n extension))
            (ext-load :loaded (conj loaded extension)
                      :locked (remove-deps (:n extension) locked)
                      :extension-list rest-extensions))))))

  (def xs1 '({:n "2"
              :d ["4" "5"]}
             {:n "1"
              :d ["2" "3"]}
             {:n "3"
              :d []}
             {:n "4"
              :d []}))

  (def xs2 '({:n "2"
              :d ["4"]}
             {:n "5"
              :d ["1"]}
             {:n "1"
              :d ["2" "3"]}
             {:n "3"
              :d []}
             {:n "4"
              :d []}))

  (def xs3 '({:n "2"
              :d ["1"]}
             {:n "1"
              :d ["3"]}
             {:n "3"
              :d ["2"]}))
  (println "Loading:")
  (ext-load :loaded [] :locked [] :extension-list xs2))
  

