(ns jarman.gui.components.composites
  (:import [jiconfont.icons.google_material_design_icons GoogleMaterialDesignIcons])
  (:require
   ;; Clojure
   [clojure.java.io   :as io]
   ;; Seesaw
   [seesaw.core       :as c]
   [seesaw.border     :as b]
   ;; Jarman
   [jarman.tools.lang            :refer :all]
   [jarman.faces                 :as face]
   [jarman.gui.gui-tools         :as gtool]
   [jarman.gui.gui-style         :as gui-style]
   [jarman.gui.core              :refer [register! satom]]
   [jarman.gui.components.panels :refer [mig-panel]]
   [jarman.gui.components.common :refer [label button text]]
   [jarman.gui.components.system :refer [input-file]]
   [jarman.logic.metadata        :refer [map->Link map->File]]))

;;   ____ ___  __  __ ____   ___  ____ ___ _____ _____          
;;  / ___/ _ \|  \/  |  _ \ / _ \/ ___|_ _|_   _| ____|         
;; | |  | | | | |\/| | |_) | | | \___ \| |  | | |  _|           
;; | |__| |_| | |  | |  __/| |_| |___) | |  | | | |___          
;;  \____\___/|_|  |_|_|    \___/|____/___| |_| |_____|         
;;   ____ ___  __  __ ____   ___  _   _ _____ _   _ _____ ____  
;;  / ___/ _ \|  \/  |  _ \ / _ \| \ | | ____| \ | |_   _/ ___| 
;; | |  | | | | |\/| | |_) | | | |  \| |  _| |  \| | | | \___ \ 
;; | |__| |_| | |  | |  __/| |_| | |\  | |___| |\  | | |  ___) |
;;  \____\___/|_|  |_|_|    \___/|_| \_|_____|_| \_| |_| |____/ 
;;                                                               

(defn url-panel [& {:keys [value on-change] :or {on-change (fn [e]) default {}}}]
  (blet
   (mig-panel 
    :background  face/c-compos-background-darker
    :constraints ["wrap 1" "10px[95:, fill, grow]10px" "10px[]10px"]
    :items [[(label :value "Text")] [(input-field value :text)]
            [(label :value "Url")]  [(input-field value :link)]])
    [state (satom (map->Link value))
     input-field
     (fn [value k]
       (text :value (k value)
             :on-change (fn [e]
                          (swap! state assoc k (c/value e))
                          (on-change @state))))]))
;;; fixme:serhii:aleks
(defn file-panel
  [& {:keys [value on-change on-download selection-mode] :or {on-change (fn [e]) on-download (fn [e]) default {} mode nil}}]
  (where
   ((selection-mode (if (and (= :save-file selection-mode) (empty? value)) :save-nothing selection-mode))
    (state          (satom (map->File (assoc value :mode selection-mode)))))
   (blet 
    (mig-panel
     :background  face/c-compos-background-darker
     :constraints ["wrap 2" "10px[95:, fill, grow]10px" "10px[]10px"]
     :items
     (case (:mode @state)
       :save-nothing  [[(label :value "Nothing to save")]]
       :load-nothing  [[(label :value "File path")] [(path-file)]]
       :load-file     [[(label :value "File name")] [(label :value "File path")]      [(filename-input)] [(path-file)]]
       :save-file     [[(label :value "File name")] [(label :value "File") "split 2"] [(label :value "Save file to")]
                       [(filename-input)]           [(path-file) "split 2"]           [(path-save)]]
       [[(label :value "Unexpected state")]])
     :event-hook-atom state
     :event-hook
     (fn [panel a old new]
       (println (into {} new))
       (case (:mode new)
         :save-nothing  (c/config! panel :items [[(label :value "Nothing to save")]])
         :load-nothing  (c/config! panel :items [[(label :value "File path")] [(path-file)]])
         :load-file     (c/config! panel :items [[(label :value "File name")] [(label :value "File path")] [(filename-input)] [(path-file)]])
         :save-file     (c/config! panel :items [[(label :value "File name")] [(label :value "File") "split 2"] [(label :value "Save file to")]
                                                 [(filename-input)]           [(path-file) "split 2"]           [(path-save)]])
         (c/config! panel :items [[(label :value "Unexpected state")]]))))
    ;; -------------
    [filename-input
     #(text :value (:file-name @state)
            :on-change (fn [e]
                         (swap! state assoc :file-name (c/value e))
                         (on-change @state)))
     path-file
     #(input-file
       :value (:file @state)
       :on-change (fn [e]
                    (swap! state assoc
                           :file e
                           :file-name (.getName (io/file e))
                           :mode :load-file)
                    (on-change @state)))
     path-save
     #(input-file
       :value (:file @state)
       :icon (gui-style/icon GoogleMaterialDesignIcons/INSERT_DRIVE_FILE face/c-icon 17)
       :on-change (fn [e]
                    (println "Starting to download file(%s) into the (%s)" (:file @state) (str e))
                    (swap! state assoc
                           :download-to (.getName (io/file e))
                           :mode :load-file)
                    (on-download @state)))])))

(comment
  (doto (seesaw.core/frame
         :title "Jarman" 
         :content (file-panel :value (map->File {:file-name "kupa" :file "bliat"})
                              :selection-mode :save-file
                              :on-change (fn [e] (println "on-chnage " (str e)))
                              :on-download (fn [e] (println "on-download " (str e)))))
    (.setLocationRelativeTo nil) seesaw.core/pack! seesaw.core/show!)

  (doto (seesaw.core/frame
         :title "Jarman" 
         :content (file-panel :value (map->File {})
                              :selection-mode :load-nothing
                              :on-change (fn [e] (println "on-chnage " (str e)))
                              :on-download (fn [e] (println "on-download " (str e)))))
    (.setLocationRelativeTo nil) seesaw.core/pack! seesaw.core/show!)

  (doto (c/frame
         :content
         (c/scrollable
          (seesaw.mig/mig-panel
           :background  face/c-compos-background-darker
           :constraints ["wrap 1" "0px[grow, fill]0px" "0px[fill, top]0px"]
           :border (b/empty-border :thickness 10)
           :items [[(label :text "site" :font (gtool/getFont :bold 20))]
                   [(url-panel :on-change (fn [e] (println e)) :default {})]
                   ;; [(seesaw.core/label :text "file" :font (gtool/getFont :bold 20))]
                   ;; [(file-panel :on-change (on-change :seal.file) :on-download (on-downld :seal.file) :default {} :selection-mode :load-nothing)]
                   ;; [(seesaw.core/label :text "ftpf" :font (gtool/getFont :bold 20))]
                   ;; [(ccomp/ftp-panel    {:on-change (on-change :seal.ftp-file) :on-download (on-downld :seal.ftp-file) :default {} :mode false ;;  (:insert-mode (state!))
                   ;;                       })]
                   ])
          ;; :vscroll false
          )
         :title "Jarman" :size [1000 :by 800])
    (.setLocationRelativeTo nil) c/pack! c/show!)
  )
