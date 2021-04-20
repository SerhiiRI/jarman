(ns jarman.gui.GUI-TUTORIALS.layout_add_remove_tutorial
  (:use seesaw.core
        seesaw.mig
        seesaw.dev
        seesaw.border))

(let [mig (mig-panel
           :constraints ["" "0px[grow, center]0px" "5px[fill]5px"]
           :items [[(label :text "One")]])
      l2 (label :text "Two")
      my-frame (-> (doto (seesaw.core/frame
                          :title "test"
                          :size [0 :by 0]
                          :content mig)
                     (.setLocationRelativeTo nil) pack! show!))]
  (config! my-frame :size [600 :by 600])
  (.add mig l2)
  (.remove mig l2))