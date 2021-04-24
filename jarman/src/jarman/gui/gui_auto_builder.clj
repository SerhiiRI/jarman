(ns jarman.gui.gui-auto-builder
  (:use seesaw.core
        seesaw.border
        seesaw.dev
        seesaw.mig
        seesaw.util)
  (:require [jarman.gui.gui-tools :refer :all]
            [jarman.gui.gui-components :refer :all])
  (:import (java.awt Color)))

(def build-insert-form
  (fn [metadata template-map]
    (let [complete (atom {})
          vp (seesaw.core/vertical-panel :items [])
          components (concat
                      (map (fn [meta]
                             (cond
                               (= (first (get meta :component-type)) "i")
                               (seesaw.core/grid-panel :columns 1
                                                       :size [200 :by 50]
                                                       :items [(seesaw.core/label
                                                                :text (get meta :representation))
                                                               (input-text
                                                                :args [:listen [:caret-update
                                                                                (fn [e]
                                                                                  (swap! complete (fn [storage] (assoc storage
                                                                                                                       (keyword (get meta :field))
                                                                                                                       (seesaw.core/value (seesaw.core/to-widget e))))))]])])
                               (= (first (get meta :component-type)) "l")
                               (do
                                 (swap! complete (fn [storage] (assoc storage
                                                                      (keyword (get meta :field))
                                                                      (get meta :key-table))))
                                 (seesaw.core/grid-panel :columns 1
                                                         :size [200 :by 50]
                                                         :items [(seesaw.core/label
                                                                  :text (get meta :representation)
                                                                  :enabled? false)
                                                                 (input-text
                                                                  :args [:text (get meta :key-table)
                                                                         :enabled? false])]))))
                           metadata)
                      [(seesaw.core/label :text "Insert" :listen [:mouse-clicked (fn [e]
                                                                                   (println "Insert " @complete)
                                                                                   (println "Map" (merge template-map @complete)
                                                                                            ;; ((:user->insert user-view)
                                                                                            ;;  (merge {:id nil :login nil :password nil :first_name nil :last_name nil :id_permission nil}
                                                                                            ;;   @complete))
                                                                                            ))])])]
      (seesaw.core/config! vp :items components))))