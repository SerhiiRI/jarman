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
  (config! test-frame :content (border-panel :items [[(com.mxgraph.swing.mxGraphComponent. mygraph) :center]]))
  (-> (doto test-frame (.setLocationRelativeTo nil)) seesaw.core/pack! seesaw.core/show!))


(let [mylabel (label :text "some-text" :background "#222")
      mylabel2 (label :text "some-text2" :background "#333")
      some-panel (border-panel  :items [[mylabel :center]])
      f-panel (flow-panel :items (list mylabel (label :border (empty-border :right 50)) mylabel2)) 
      main-panel  (border-panel :items [;;[(com.mxgraph.swing.mxGraphComponent. mygraph) :center]
                                                     [f-panel :center]
                                                     [(label :text "click" :background "#444"
                                                             :listen [:mouse-clicked
                                                                      (fn [e] (let [mygraph (mxGraph.)
                                                                                    parent (.getDefaultParent mygraph)
                                                                                    loc1 (.getLocationOnScreen mylabel)
                                                                                    loc2 (.getLocationOnScreen mylabel2)
                                                                                    v1 (.insertVertex mygraph parent nil ""
                                                                                                      (.getX loc1) (.getY loc1) 0 0)
                                                                                    v2 (.insertVertex mygraph parent nil ""
                                                                                                      (.getX loc2) (.getY loc2) 0 0)]
                                                                                (-> mygraph .getModel .beginUpdate)
                                                                                ;;(DefaultGraphCell. (label))
                                                                                (.insertEdge mygraph parent nil "" v1 v2 )
                                                                                (-> mygraph .getModel .endUpdate)
                                                                                (config! f-panel :items
                                                                                         (list mylabel
                                                                                               mylabel2
                                                                                          (com.mxgraph.swing.mxGraphComponent. mygraph)))
                                                                                ))]) :south]])]
  (config! test-frame :content main-panel)
  (-> (doto test-frame (.setLocationRelativeTo nil)) seesaw.core/pack! seesaw.core/show!))

