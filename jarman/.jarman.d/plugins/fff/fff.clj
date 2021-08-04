(ns jarman.plugins.fff
  (:require
   ;; Clojure toolkit 
   [clojure.data :as data]
   [clojure.string :as string]
   [clojure.spec.alpha :as s]
   [clojure.pprint :as pprint]
   ;; Seesaw components
   [seesaw.core   :as c]
   ;; Jarman toolkit
   [jarman.tools.lang :refer :all]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; EXTERNAL INTERFAISE ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn fff-toolkit-pipeline [configuration]
  {:FFF "LOADED"})

(defn fff-entry [plugin-path global-configuration]
  (let []    
    (c/label "FFFFFFFFFFFFFFFFFFFFFFFF")))


