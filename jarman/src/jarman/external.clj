(ns jarman.external)
(require 'jarman.config.conf-language)
(require 'jarman.config.vars)
(require 'jarman.plugin.plugin)
(require 'jarman.tools.org)
(require 'jarman.gui.faces-system)

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
  (apply jarman.config.conf-language/plang plugin translation-path))
;; (def plang-ui (fn [plugin & lang-path] (apply plang % :ui lang-path )))
;; (def plang-alerts (fn [plugin & lang-path] (apply plang % :ui :alerts lang-path )))

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
  (apply jarman.config.conf-language/lang translation-path))

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
  [variable-name default-value & params]
  `(jarman.config.vars/defvar ~variable-name ~default-value
     ~@params))

(defmacro setq [& var-list]
  `(jarman.config.vars/setq
    ~@var-list))

(defmacro setj [& var-list]
  `(jarman.config.vars/setj
    ~@var-list))

(comment
  (jarman.config.conf-language/debug-enable)
  (jarman.config.conf-language/debug-disable)
  (lang :themes :name)
  ;; => ":themes"
  (plang :aaa :accept-button)
  ;; => "bliat"
  )
  
(defn register-custom-view-plugin [& args]
  (apply jarman.plugin.plugin/register-custom-view-plugin args))

(defn register-custom-theme-plugin [& args]
  (apply jarman.plugin.plugin/register-custom-theme-plugin args))

(defmacro print-line [s]
  `(jarman.tools.org/print-line ~s))
(defmacro print-multiline [s]
  `(jarman.tools.org/print-multiline ~s))
(defmacro print-example [s]
  `(jarman.tools.org/print-example ~s))
(defmacro print-error [e]
  `(jarman.tools.org/print-error ~e))

(defmacro out-update [& body]
  `(jarman.tools.org/out-update
    ~@body))
(defmacro out-extension [& body]
  `(jarman.tools.org/out-extension
    ~@body))

(defn custom-theme-set-faces [variable-list]
  (jarman.gui.faces-system/custom-theme-set-faces variable-list))

