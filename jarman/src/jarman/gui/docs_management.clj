(ns jarman.gui.docs_management
  (:require
   ;; Clojure toolkit 
   [clojure.data :as data]
   [clojure.string :as string]
   [seesaw.util :as u]
   ;; Seesaw components
   [seesaw.core :as c]
   [seesaw.border :as sborder]
   [seesaw.dev :as sdev]
   [seesaw.mig :as smig]
   [seesaw.swingx :as swingx]
   ;; Jarman toolkit
   [jarman.logic.connection :as db]
   [jarman.config.config-manager :as cm]
   [jarman.tools.lang :refer :all :as lang]
   [jarman.gui.gui-tools :refer :all :as gtool]
   [jarman.resource-lib.icon-library :as ico]
   [jarman.tools.swing :as stool]
   [jarman.gui.gui-components :refer :all :as gcomp]
   [jarman.gui.gui-calendar :as calendar]
   [jarman.config.storage :as storage]
   [jarman.config.environment :as env]
   [jarman.logic.sql-tool :as toolbox :include-macros true :refer :all]
   [jarman.logic.metadata :as mt])
  (:import (java.util Date)
           (java.text SimpleDateFormat)))


(defn view
  []
  (smig/mig-panel :constraints))


(let [start-focus (atom nil)
      my-frame (-> (doto (c/frame
                          :title "test"
                          :size [1000 :by 800]
                          :content (view))
                     (.setLocationRelativeTo nil) c/pack! c/show!))]
  (c/config! my-frame :size [1000 :by 800])
  (if-not (nil? start-focus) (c/invoke-later (.requestFocus @start-focus true))))
