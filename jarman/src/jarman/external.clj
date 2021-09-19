(ns jarman.external)
(require '[jarman.config.conf-language])


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


(comment
  (jarman.config.conf-language/debug-enable)
  (jarman.config.conf-language/debug-disable)
  (lang :themes :name)
  ;; => ":themes"
  (plang :aaa :accept-button)
  ;; => "bliat"
  )
  
