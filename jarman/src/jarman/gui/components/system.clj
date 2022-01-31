(ns jarman.gui.components.system
  (:require
   ;; Clojure
   [clojure.java.io :as io]
   ;; Seesaw
   [seesaw.core     :as c]
   [seesaw.border   :as b]
   [seesaw.chooser  :as chooser]
   ;; Jarman
   [jarman.faces          :as face]
   [jarman.tools.lang     :refer :all]
   [jarman.config.environment :as env]
   [jarman.gui.core       :refer [satom register! cursor]]
   [jarman.gui.gui-style  :as gui-style]
   [jarman.gui.components.common :refer [label button]])
  (:import
   [jiconfont.icons.google_material_design_icons GoogleMaterialDesignIcons]
   [java.awt FileDialog]))

(defn file-dialog
  "Description
    Invoke system window for choosing file
    `:parent` mean panel or frame.
    `:mode` key can be set only as
      - `:load` mean that you choose file to read
      - `:save` mean that you choose file to persist
    `:directory` - directory for choosing
  
  Example
    (file-dialog
      :parent (seesaw.core/frame :title \"Jarman\" :content (c/label :text \"empty\"))
      :title \"Choose porn image\"
      :mode :load)"
  [& {:keys [parent title mode directory search-pattern] :or {parent (c/frame) directory env/user-home mode :load}}]
    {:pre [(#{:load :save} mode) (string? title) (some? parent)]}
    (let [file-dialog-modes {:load FileDialog/LOAD :save FileDialog/SAVE}]
      (let [dialog
            (doto (FileDialog. parent title (mode file-dialog-modes))
              (.setDirectory directory)
              (.setVisible true))]
        (str (io/file (.getDirectory dialog) (.getFile dialog))))))

(defn input-file
  "Description:
     File choser, button with icon, when path is selected it changes background color"
  [& {:keys [value selection-mode on-change] :or {value "" selection-mode false on-change (fn [e] e)}}]
  (let [state             (satom {:status :not-selected})
        not-choosed-icon  (gui-style/icon GoogleMaterialDesignIcons/ATTACHMENT        face/c-icon 17)
        choosed-icond     (gui-style/icon GoogleMaterialDesignIcons/INSERT_DRIVE_FILE face/c-icon 17)
        icon-chooser      (fn [compn path] (if-not (empty? path)
                                            (c/config! compn :icon choosed-icond :tip path)
                                            (c/config! compn :icon not-choosed-icon :tip "Please choose")))
        icon-button       (button :text ""
                                  :tgap 4 :bgap 4 :lgap 0 :rgap 0
                                  :args [:icon not-choosed-icon]
                                  :on-click
                                  (fn [e] 
                                    (let [new-path (file-dialog :title "Choose input file" :mode :load)]
                                      (icon-chooser (.getComponent e) new-path)
                                      (on-change new-path))
                                    (comment
                                      ;; with using Swing `chooser-file` aproach
                                      (let [new-path (chooser/choose-file
                                                      :selection-mode (if selection-mode :dirs-only :files-only)
                                                      :suggested-name value
                                                      :success-fn  (fn [fc file] (.getAbsolutePath file)))]
                                        (icon-chooser (.getComponent e) new-path)
                                        (on-change e)))))]
    (icon-chooser icon-button value)
    icon-button))

;;  ____  _____ __  __  ___  
;; |  _ \| ____|  \/  |/ _ \
;; | | | |  _| | |\/| | | | |
;; | |_| | |___| |  | | |_| |
;; |____/|_____|_|  |_|\___/
;;

(comment
  ;; Open system modal window and choose file
  (file-dialog
   :parent (seesaw.core/frame
            :title "Jarman" 
            :content (input-file))
   :title "Choose porn image"
   :mode :load)
  ;; Full UI component
  (-> (doto (seesaw.core/frame
             :title "Jarman" 
             :content (seesaw.mig/mig-panel
                       :background  face/c-compos-background-darker
                       :constraints ["wrap 1" "0px[grow, fill]0px" "0px[fill, top]0px"]
                       :border (b/empty-border :thickness 10)
                       :items [[(seesaw.core/label :text "Begin Chooser")]
                               [(input-file)]
                               [(seesaw.core/label :text "End Chooser")]]))
        (.setLocationRelativeTo nil) seesaw.core/pack! seesaw.core/show!))
  )
