;;   ____ ___  ____  _____
;;  / ___/ _ \|  _ \| ____|
;; | |  | | | | |_) |  _|
;; | |__| |_| |  _ <| |___
;;  \____\___/|_| \_\_____|
;;
(ns jarman.gui.core
  (:require
   ;; Clojure
   [clojure.data]
   [clojure.java.io :as io]
   [clojure.pprint :refer [cl-format]]
   ;; Jarman
   [jarman.lang :refer :all]
   [jarman.org  :refer :all]
   [jarman.config.environment :as env]))

(declare register!)
(declare unregister!)

(defprotocol IEventHookRegister
  (register      [this k f])
  (unregister    [this k])
  (getEventHooks [this]))

(defn register! [a k f]
  (.register a k f))

(defn unregister! [a k]
  (.unregister a k))

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
    (reset! clojure-atom new-value))

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

(defn- invoke-on-hooks [v m]
  (doall
   (doseq [f (vals m)]
     (f v))) v)

(defrecord Event [f eventHooks]
  clojure.lang.IFn
  (invoke [this] (doto (f) (invoke-on-hooks (deref eventHooks))))
  (invoke [this arg1] (doto (f arg1) (invoke-on-hooks (deref eventHooks))))
  (invoke [this arg1 arg2] (doto (f arg1 arg2) (invoke-on-hooks (deref eventHooks))))
  (invoke [this arg1 arg2 arg3] (doto (f arg1 arg2 arg3) (invoke-on-hooks (deref eventHooks))))
  (invoke [this arg1 arg2 arg3 arg4] (doto (f arg1 arg2 arg3 arg4) (invoke-on-hooks (deref eventHooks))))
  (invoke [this arg1 arg2 arg3 arg4 arg5] (doto (f arg1 arg2 arg3 arg4 arg5) (invoke-on-hooks (deref eventHooks))))
  (invoke [this arg1 arg2 arg3 arg4 arg5 arg6] (doto (f arg1 arg2 arg3 arg4 arg5 arg6) (invoke-on-hooks (deref eventHooks))))
  (invoke [this arg1 arg2 arg3 arg4 arg5 arg6 arg7] (doto (f arg1 arg2 arg3 arg4 arg5 arg6 arg7) (invoke-on-hooks (deref eventHooks))))
  (invoke [this arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8] (doto (f arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8) (invoke-on-hooks (deref eventHooks))))
  (invoke [this arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9] (doto (f arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9) (invoke-on-hooks (deref eventHooks))))
  (invoke [this arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10] (doto (f arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10) (invoke-on-hooks (deref eventHooks))))
  (invoke [this arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10 arg11] (doto (f arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10 arg11) (invoke-on-hooks (deref eventHooks))))
  (invoke [this arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10 arg11 arg12] (doto (f arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10 arg11 arg12) (invoke-on-hooks (deref eventHooks))))
  (invoke [this arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10 arg11 arg12 arg13] (doto (f arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10 arg11 arg12 arg13) (invoke-on-hooks (deref eventHooks))))
  (invoke [this arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10 arg11 arg12 arg13 arg14] (doto (f arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10 arg11 arg12 arg13 arg14) (invoke-on-hooks (deref eventHooks))))
  (invoke [this arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10 arg11 arg12 arg13 arg14 arg15] (doto (f arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10 arg11 arg12 arg13 arg14 arg15) (invoke-on-hooks (deref eventHooks))))
  (invoke [this arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10 arg11 arg12 arg13 arg14 arg15 arg16] (doto (f arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10 arg11 arg12 arg13 arg14 arg15 arg16) (invoke-on-hooks (deref eventHooks))))
  (invoke [this arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10 arg11 arg12 arg13 arg14 arg15 arg16 arg17] (doto (f arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10 arg11 arg12 arg13 arg14 arg15 arg16 arg17) (invoke-on-hooks (deref eventHooks))))
  (invoke [this arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10 arg11 arg12 arg13 arg14 arg15 arg16 arg17 arg18] (doto (f arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10 arg11 arg12 arg13 arg14 arg15 arg16 arg17 arg18) (invoke-on-hooks (deref eventHooks))))
  (invoke [this arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10 arg11 arg12 arg13 arg14 arg15 arg16 arg17 arg18 arg19] (doto (f arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10 arg11 arg12 arg13 arg14 arg15 arg16 arg17 arg18 arg19) (invoke-on-hooks (deref eventHooks))))
  (invoke [this arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10 arg11 arg12 arg13 arg14 arg15 arg16 arg17 arg18 arg19 arg20] (doto (f arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10 arg11 arg12 arg13 arg14 arg15 arg16 arg17 arg18 arg19 arg20) (invoke-on-hooks (deref eventHooks))))
  (invoke [this arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10 arg11 arg12 arg13 arg14 arg15 arg16 arg17 arg18 arg19 arg20 args] (doto (apply f arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10 arg11 arg12 arg13 arg14 arg15 arg16 arg17 arg18 arg19 arg20 args) (invoke-on-hooks (deref eventHooks))))
  (applyTo [this args] (clojure.lang.AFn/applyToHelper this args))
  IEventHookRegister
  (getEventHooks [this]
    (deref eventHooks))
  (register [this k f]
    (dosync
     (alter eventHooks assoc k (fn [v] (f v)))) k)
  (unregister [this k]
    (dosync (alter eventHooks dissoc k)) k)
  java.util.concurrent.Callable
  (call [this] (.invoke this))
  java.lang.Runnable
  (run [this] (.invoke this)))
(defn isEvent? [^jarman.gui.core.Event e] (instance? jarman.gui.core.Event e))
(defn event
  ([] identity)
  ([lambda]
   (let [event-hook-m (ref {})]
     (Event. lambda event-hook-m))))
(defmacro fe [args & body]
   `(event (fn ~args ~@body)))


;;  ____  _____ __  __  ___
;; |  _ \| ____|  \/  |/ _ \
;; | | | |  _| | |\/| | | | |
;; | |_| | |___| |  | | |_| |
;; |____/|_____|_|  |_|\___/

(comment
  ;;;;;;;;;;;
  ;; Satom ;;
  ;;;;;;;;;;;

  (def x (satom {:a {}}))
  (def x-cursor (cursor [:a] x))
  (register! x        :global (fn [a old new] (println "GLOBAL:" new)))
  (register! x-cursor :local  (fn [a old new] (println "LOCAL:"  new)))
  (swap! x-cursor assoc :cursor-a 10)
  (swap! x        assoc :global 1)
  (reset! x-cursor        11)
  (reset! x        {:suka 1})
  (deref x-cursor)
  (deref x)

  ;;;;;;;;;
  ;; Eta ;;
  ;;;;;;;;;

  ;; or
  (def z (event (fn [a b c] [a b c])))
  ;; or
  (def z (fe [a b c]
            [a b c]))
  ;; pin external event
  (register! z :A (fn [v] (println "RETURNED VALUE -> " v)))
  ;; invoke
  (z 1 2 3))
