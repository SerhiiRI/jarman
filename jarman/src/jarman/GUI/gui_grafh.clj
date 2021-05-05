(ns jarman.gui.gui-graph
  (:use seesaw.core
        seesaw.border
        seesaw.dev
        seesaw.style
        seesaw.mig
        seesaw.font)
  (:import (java.text SimpleDateFormat)
           (javax.swing JScrollPane)
           (java.awt Dimension))
  (:require [clojure.string :as string]
            [jarman.gui.gui-tools :as tool]
            [jarman.config.config-manager :as conf]
            [jarman.resource-lib.icon-library :as icon]
            [clojure.java.jdbc :as jdbc]
            [jarman.tools.swing :as stool]
            [jarman.gui.gui-app :as app]
            [jarman.logic.system-data-logistics :as logic]
            [jarman.gui.gui-components :as components]
            [jarman.logic.connection :as c]
            [kaleidocs.merge :refer [merge-doc]]
            ;; [jarman.config.init :refer [configuration language swapp-all save-all-cofiguration make-backup-configuration]]
            ))


(import '(com.mxgraph.view mxGraph))

(import '(com.mxgraph.swing mxGraphComponent))


(def test-frame 
  (frame :title "Jarman"
         :undecorated? false
         :resizable? true
         :minimum-size [800 :by 600]))


(let [mygraph (mxGraph.) 
      parent (.getDefaultParent mygraph)
      v1 (.insertVertex mygraph parent nil "hello!" 20 20 80 30)
      v2 (.insertVertex mygraph parent nil "wassup!" 240 150 80 30)]

  (-> mygraph .getModel .beginUpdate)
  (.insertEdge mygraph parent nil "Edge" v1 v2)
  (-> mygraph .getModel .endUpdate)
  
  (config! test-frame :content (com.mxgraph.swing.mxGraphComponent. mygraph)))



(let [mygraph (mxGraph.) 
      parent (.getDefaultParent mygraph)
      v1 (.insertVertex mygraph parent nil "hello!" 20 20 80 30)
      v2 (.insertVertex mygraph parent nil "wassup!" 240 150 80 30)]
  (-> mygraph .getModel .beginUpdate)
  (.insertEdge mygraph parent nil "Edge" (label) )
  (-> mygraph .getModel .endUpdate)
  (config! test-frame :content (com.mxgraph.swing.mxGraphComponent. mygraph)))


(-> (doto test-frame (.setLocationRelativeTo nil) ) seesaw.core/pack! seesaw.core/show!)



	
(.insertVertex mygraph parent nil "hello!" 20 20 80 30)
