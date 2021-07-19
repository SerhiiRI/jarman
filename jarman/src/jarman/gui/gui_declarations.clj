(ns jarman.gui.gui-declarations)

;; (defrecord Representation [icon title])
;; (defn isRepresentation?
;;   [^jarman.gui.gui_declarations.Representation e]
;;   (instance? jarman.gui.gui_declarations.Representation e))

;; (defrecord PluginPath [plugin-path])

(defrecord Button
    [;; ^jarman.gui.gui_declarations.Representation representation
     additional-info])
(defn isButton? [^jarman.gui.gui_declarations.Button e]
     (instance? jarman.gui.gui_declarations.Button e))


;; (Button. [:user :table :user])
