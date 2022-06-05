(ns jarman.gui.components.composites
  (:import [jiconfont.icons.google_material_design_icons GoogleMaterialDesignIcons])
  (:require
   ;; Clojure
   [clojure.java.io   :as io]
   ;; Seesaw
   [seesaw.core       :as c]
   [seesaw.border     :as b]
   ;; Jarman
   [jarman.lang                  :refer :all]
   [jarman.faces                 :as face]
   [jarman.gui.gui-tools         :as gtool]
   [jarman.gui.gui-style         :as gui-style]
   [jarman.gui.core              :refer [register! satom cursor]]
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
  [& {:keys [value on-change on-download] :or {on-change identity on-download identity default {}}}]
  (where
   ((selection-mode (if (or (empty? value) (nil? (:file value))) :empty :selected))
    (state          (satom {:model (map->File value) :mode selection-mode})))
   (blet 
    (mig-panel
     :background  face/c-compos-background-darker
     :constraints ["wrap 2" "10px[95:, fill, grow]10px" "10px[]10px"]
     :items
     (case (:mode @state)
       :empty     [[(label :value "File path")] [(path-file)]]
       :changed   [[(label :value "File name")] [(label :value "File path")]      [(filename-input)] [(path-file)]]
       :selected  [[(label :value "File name")] [(label :value "File") "split 2"] [(label :value "Save file to")]
                   [(filename-input)]           [(path-file) "split 2"]           [(path-save)]]
       [[(label :value "Unexpected state")]])
     :event-hook-atom (cursor [:mode] state)
     :event-hook
     (fn [panel a old new-mode]
       (println "STATE!" new-mode)
       (case new-mode
         :empty     (c/config! panel :items [[(label :value "File path")] [(path-file)]])
         :changed   (c/config! panel :items [[(label :value "File name")] [(label :value "File path")] [(filename-input)] [(path-file)]])
         :selected  (c/config! panel :items [[(label :value "File name")] [(label :value "File") "split 2"] [(label :value "Save file to")]
                                             [(filename-input)]           [(path-file) "split 2"]           [(path-save)]])
         (c/config! panel :items [[(label :value "Unexpected state")]]))))
    ;; -------------
    [filename-input
     (fn [] (text :value (:file-name @state)
                 :on-change (fn [e]
                              (swap! state assoc-in [:model :file-name] (c/value e))
                              (on-change (get @state :model)))))
     path-file
     (fn [] (input-file
            :value (get-in @state [:model :file])
            :on-change (fn [e]
                         (swap! state
                                #(-> %
                                     (assoc-in [:model :file] e)
                                     (assoc-in [:model :file-name] (.getName (io/file e)))
                                     (assoc-in [:mode] :changed)))
                         (on-change (get @state :model)))))
     path-save
     (fn [] (input-file
            :value (get-in @state [:model :file])
            :icon (gui-style/icon GoogleMaterialDesignIcons/INSERT_DRIVE_FILE face/c-icon 17)
            :on-change (fn [e]
                         (println "Starting to download file(%s) into the (%s)" (:file @state) (str e))
                         (swap! state assoc
                                :download-to (.getName (io/file e)))
                         (on-download (get @state :model)))))])))

;;  ____  _____ __  __  ___
;; |  _ \| ____|  \/  |/ _ \
;; | | | |  _| | |\/| | | | |
;; | |_| | |___| |  | | |_| |
;; |____/|_____|_|  |_|\___/
;;

(comment
  (doto (seesaw.core/frame
         :title "Jarman" 
         :content (file-panel :value (map->File {:file-name "kupa" :file "bliat"})
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
                   ;; [(url-panel :on-change (fn [e] (println e)) :default {})]
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
