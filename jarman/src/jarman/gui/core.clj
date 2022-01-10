(ns jarman.gui.core
  (:require
   ;; Clojure
   [clojure.data]
   [clojure.java.io :as io]
   [clojure.pprint :refer [cl-format]]
   ;; Jarman
   [jarman.tools.lang :refer :all]
   [jarman.tools.org  :refer :all]
   [jarman.config.environment :as env]))

(defprotocol IEventHookRegister
  (register      [this k f])
  (unregister    [this k])
  (getEventHooks [this]))

(deftype ^:private SwingAtom
    [^{:tag clojure.lang.Ref :private true} eventHooks
     ^{:tag clojure.lang.Atom}              clojure-atom]
  clojure.lang.IDeref
  (deref [this]
    (deref clojure-atom))

  clojure.lang.IAtom
  (swap [a f]
    (swap! clojure-atom f))
  (swap [a f x]
    (swap! clojure-atom f x))
  (swap [a f x y]
    (swap! clojure-atom f x y))
  (swap [a f x y more]
    (swap! clojure-atom (fn [v] (apply f v x y more))))
  (reset [a new-value]
    (reset! a new-value))

  clojure.lang.IMeta
  (meta [_]
    (meta clojure-atom))

  clojure.lang.IRef
  (removeWatch [this key]
    (remove-watch clojure-atom key))
  (getWatches [this]
    (.getWatches clojure-atom))
  (getValidator [this]
    (.getValidator clojure-atom))
  (setValidator [this p]
    (set-validator! this p))
  (addWatch [this k f]
    (add-watch clojure-atom k f))

  IEventHookRegister
  (getEventHooks [this]
    (deref eventHooks))
  (register [this k f]
    (dosync
     (alter eventHooks assoc k
            (fn [a oldst newst]
              (let [[left right same] (clojure.data/diff newst oldst)]
                (if (not (and (nil? left) (nil? right)))
                  (f a oldst newst)))))) k)
  (unregister [this k]
    (dosync (alter eventHooks dissoc k)) k))

(deftype ^:private Cursor [path swing-atom]
  clojure.lang.IDeref
  (deref [this]
    (get-in @swing-atom path))

  clojure.lang.IAtom
  (swap [a f]
    (-> (swap! swing-atom update-in path f)
        (get-in path)))
  (swap [a f x]
    (-> (swap! swing-atom update-in path f x)
        (get-in path)))
  (swap [a f x y]
    (-> (swap! swing-atom update-in path f x y)
        (get-in path)))
  (swap [a f x y more]
    (-> (swap! swing-atom update-in path (fn [v] (apply f v x y more)))
        (get-in path)))
  (reset [a new-value]
    (swap! swing-atom assoc-in path new-value)
    new-value)

  clojure.lang.IMeta
  (meta [_]
    (meta swing-atom))
  ;; java.io.PrintWriter
  ;; (-pr-writer [a writer opts]
  ;;   ;; not sure about how this should be implemented?
  ;;   ;; should it print as an atom focused on the appropriate part of
  ;;   ;; the swing-atom - (pr-writer (get-in @swing-atom path)) - or should it be
  ;;   ;; a completely separate type? and do we need a reader for it?

  ;;   ;; Until further investigation, it should simply be REPL friendly.
  ;;   (-write writer "#<Cursor: ")
  ;;   (pr-writer (get-in @swing-atom path) writer opts) ;; the current value
  ;;   (-write writer " @")
  ;;   (pr-writer path writer opts)
  ;;   (-write writer ">"))

  clojure.lang.IRef
  (removeWatch [this key]
    (remove-watch swing-atom [path key]))
  (getWatches [this]
    (.getWatches swing-atom))
  (getValidator [this]
    (.getValidator swing-atom))
  (setValidator [this p]
    (set-validator! this p))
  (addWatch [this kv f]
    (add-watch swing-atom (conj path kv)
               (fn [k r o n] (f key this (get-in o path) (get-in n path)))))

  IEventHookRegister
  (getEventHooks [this]
    (getEventHooks swing-atom))
  (register [this k f]
    (.register
     swing-atom k
     (fn [a oldst newst]
       (let [[left right same] (clojure.data/diff (get-in newst path nil) (get-in oldst path nil))]
         (if (not (and (nil? left) (nil? right)))
           (f a (get-in oldst path nil) (get-in newst path nil)))))))
  (unregister [this k]
    (.unregister swing-atom k)))

(defn register! [a k f]
  (.register a k f))

(defn unregister! [a k]
  (.unregister a k))

(defn satom
  ([default]
   (let [clojure-atom (atom default) event-hook-m (ref {})]
     (add-watch
      clojure-atom :eventHookRegistrator
      (fn [key atom old-state new-state]
        (doall (clojure.core/map (fn [f] (f atom old-state new-state))
                                 (vals (deref event-hook-m))))))
     (SwingAtom. event-hook-m clojure-atom))))

(defn cursor
  ([path] (fn [a] (cursor path a)))
  ([path a] (if (seq path) (Cursor. path a) a)))

(comment
  (def x (satom {:a {}}))
  (def x-cursor (cursor [:a] x))
  (register! x        :global (fn [a old new] (println "GLOBAL:" new)))
  (register! x-cursor :local  (fn [a old new] (println "LOCAL:"  new)))
  (swap! x-cursor assoc :cursor-a 10)
  (swap! x        assoc :global 1)
  (deref x-cursor)
  (deref x))
