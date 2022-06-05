(ns plugin.table.composite-components
  (:require
   [clojure.string :as s]
   [clojure.java.io :as io]
   [seesaw.core   :as c]
   [jarman.lang :refer [in?]]
   [jarman.gui.gui-tools :as gtool]
   [jarman.gui.core :refer [register! satom]]
   [jarman.gui.gui-panel :refer [mig-panel]]
   [jarman.gui.gui-style :as gui-style]
   [jarman.gui.gui-components :as gcomp]
   [jarman.gui.gui-components2]
   [jarman.faces              :as face]
   [jarman.logic.composite-components :as ccomp]
   [jarman.logic.metadata])
  (:import [jiconfont.icons.google_material_design_icons GoogleMaterialDesignIcons]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; helper functions for building panel ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn build-main-panel [data-vec]
  (seesaw.mig/mig-panel
   :background  face/c-compos-background-darker
   :constraints ["wrap 2" "10px[95:, fill, grow]10px" "10px[]10px"]
   :items data-vec))

(defn input-field [defr-key func val]
  (gcomp/state-input-text {:func (fn [e] (func e defr-key))
                           :val  (defr-key val)}))

(defn f-label [text] (seesaw.core/label :text text :font (gtool/getFont 15)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; panels with composite components ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn url-panel
  "Decscription
     Function return mig-panel with fields for composite component file-panel:
     login, password, file-name, file-path
   Keys
     :func - completed fn[e] with dispatch
     :val - obj with values to fill in the fields"
  [{func :func  
    val  :val}]
  (let [f-title  (gcomp/state-input-text {:func (fn [e] (func e :text))
                                          :val  (:text val)})
        f-url    (gcomp/state-input-text {:func (fn [e] (func e :link))
                                          :val  (:link val)})]
    (seesaw.mig/mig-panel
     :background  face/c-compos-background-darker
     :constraints ["wrap 1" "10px[95:, fill, grow]10px" "10px[]10px"]
     :items [[(f-label "text:")] [f-title] [(f-label "url:")] [f-url]])))

(comment
  ;; WIP
  (defn url-panel [& {:keys [default on-change] :or {on-change (fn [e]) default {}}}]
    (let [a (satom (jarman.logic.metadata/map->Link default))
          text-params (fn [k] [:on-change (fn [e] (swap! a assoc k (c/value e)) (on-change @a)) :text (k @a)])]
      (mig-panel 
       :background  face/c-compos-background-darker
       :constraints ["wrap 1" "10px[95:, fill, grow]10px" "10px[]10px"]
       :items [[(f-label "text:")] [(apply jarman.gui.gui-components2/text (text-params :text))]
               [(f-label "url:")]  [(apply jarman.gui.gui-components2/text (text-params :link))]])))

  ;; 
  (defn file-panel
    [& {:keys [default on-change on-download selection-mode] :or {on-change (fn [e]) on-download (fn [e]) default {} mode nil}}]
    (let [selection-mode (if (and (= :save-file selection-mode) (empty? default)) :save-nothing selection-mode)
          state (satom (jarman.logic.metadata/map->File (assoc default :mode selection-mode)))
          f-name     (fn [] (jarman.gui.gui-components2/text
                            :value (:file-name @state)
                            :on-change (fn [e]
                                         (swap! state assoc :file-name (c/value e))
                                         (on-change @state))))
          path-file  (fn [] (jarman.gui.gui-components2/input-file
                            :value (:file @state)
                            :on-change (fn [e]
                                         (swap! state assoc
                                                :file e
                                                :file-name (.getName (io/file e))
                                                :mode :load-file)
                                         (on-change @state))))
          path-save  (fn [] (jarman.gui.gui-components2/input-file
                            :value (:file @state)
                            :icon (gui-style/icon GoogleMaterialDesignIcons/INSERT_DRIVE_FILE face/c-icon 17)
                            :on-change (fn [e]
                                         (println "Starting to download file(%s) into the (%s)" (:file @state) (str e))
                                         (swap! state assoc
                                                :download-to (.getName (io/file e))
                                                :mode :load-file)
                                         (on-download @state))))]
      (mig-panel
       :background  face/c-compos-background-darker
       :constraints ["wrap 2" "10px[95:, fill, grow]10px" "10px[]10px"]
       :items
       (case (:mode @state)
         :load-nothing  [[(f-label "File path")] [(path-file)]]
         :load-file     [[(f-label "File name")] [(f-label "File path")] [(f-name)] [(path-file)]]
         :save-nothing  [[(f-label "Nothing to save")]]
         :save-file     [[(f-label "File name")] [(f-label "File") "split 2"] [(f-label "Save file to")]
                         [(f-name)             ] [(path-file) "split 2"]      [(path-save)]]
         [[(f-label "Unexpected state")]])
       :event-hook-atom state
       :event-hook
       (fn [panel a old new]
         (println (into {} new))
         (case (:mode new)
           :load-nothing  (c/config! panel :items [[(f-label "File path")] [(path-file)]])
           :load-file     (c/config! panel :items [[(f-label "File name")] [(f-label "File path")] [(f-name)] [(path-file)]])
           :save-nothing  (c/config! panel :items [[(f-label "Nothing to save")]])
           :save-file     (c/config! panel :items [[(f-label "File name")] [(f-label "File") "split 2"] [(f-label "Save file to")]
                                                   [(f-name)             ] [(path-file) "split 2"]      [(path-save)]])
           (c/config! panel :items [[(f-label "Unexpected state")]]))))))

  (-> (doto (seesaw.core/frame
             :title "Jarman" 
             :content (file-panel :default (jarman.logic.metadata/map->File
                                            {:file-name "kupa" :file "bliat"}) :selection-mode :save-file
                                  :on-change (fn [e] (println "on-chnage " (str e)))
                                  :on-download (fn [e] (println "on-download " (str e)))))
        (.setLocationRelativeTo nil) seesaw.core/pack! seesaw.core/show!))

  (-> (doto (seesaw.core/frame
             :title "Jarman" 
             :content (file-panel :default (jarman.logic.metadata/map->File
                                            {}) :selection-mode :load-nothing
                                  :on-change (fn [e] (println "on-chnage " (str e)))
                                  :on-download (fn [e] (println "on-download " (str e)))))
        (.setLocationRelativeTo nil) seesaw.core/pack! seesaw.core/show!))
)


(comment
  (require 'jarman.gui.gui-components2)
  (defn url-panel [{:keys [default on-change] :or {on-change (fn [e]) default {}}}]
    (let [a (satom (jarman.logic.metadata/map->Link default))
          text-params (fn [k] [:on-change (fn [e] (swap! a assoc k (c/value e)) (on-change @a)) :text (k @a)])]
      (mig-panel 
       :background  face/c-compos-background-darker
       :constraints ["wrap 1" "10px[95:, fill, grow]10px" "10px[]10px"]
       :items [[(f-label "text:")] [(apply jarman.gui.gui-components2/text (text-params :text))]
               [(f-label "url:")]  [(apply jarman.gui.gui-components2/text (text-params :link))]])))
  (defn file-panel
    [{:keys [default on-change on-download mode] :or {on-change (fn [e]) on-download (fn [e]) default {} mode nil}}]
    (let [unselected-file? (atom true)
          sratom    (atom (jarman.logic.metadata/map->File default))
          sratom-kf (fn [k] {:func (fn [e] (swap! sratom assoc k (c/value e)) (on-change @sratom)) :val (k @sratom)})
          sratom-download (fn [] {:func (fn [e] (on-download @sratom)) :val ""})]
      (let [l-name     (f-label "File name")
            l-file     (f-label "File path")
            l-save     (f-label "Save file")
            f-name     (gcomp/state-input-text  (sratom-kf :file-name))
            path-file  (gcomp/status-input-file (sratom-kf :file))
            path-save  (gcomp/status-input-file (sratom-download))
            panel      (if (deref unselected-file?)
                         (build-main-panel [[l-name] [l-file]                    [f-name] [path-file]                      ])
                         (build-main-panel [[l-name] [l-file "split 2"] [l-save] [f-name] [path-file "split 2"] [path-save]]))]
        (seesaw.core/config!
         path-file :listen
         [:property-change
          (fn [e]
            (let [file-name (last (s/split (.getToolTipText path-file) (re-pattern (java.io.File/separator))))]
              (if-not (empty? file-name)
                (do (seesaw.core/config! f-name :text file-name)
                    (.revalidate panel)
                    (.repaint panel))
                (seesaw.core/config! f-name :text (:file-name val)))))])
        panel)))
  (defn ftp-panel
    "Decscription
     Function return mig-panel with fields for composite component ftp-panel:
     login, password, file-name, file-path
   Keys
     :func - completed fn[e] with dispatch
     :func-save - daispatch for save file from db
     :val - obj with values to fill in the fields
     :mode - boolean, true means that insert-mode is on, so field for save file is not available"
    [{:keys [default on-change on-download mode] :or {on-change (fn [e]) on-download (fn [e]) default {} mode nil}}]
    (let [sratom           (atom (jarman.logic.metadata/map->FtpFile default))
          sratom-kf        (fn [k] {:func (fn [e] (swap! sratom assoc k (c/value e)) (on-change @sratom)) :val (k @sratom)})
          sratom-download  (fn [ ] {:func (fn [e] (on-download @sratom)) :val "" :mode true})]
     (let [l-name   (f-label "File name")
           l-path   (f-label "File path")
           l-save   (f-label "Save file")
           f-name      (gcomp/state-input-text  (sratom-kf :file-name))
           path-file   (gcomp/status-input-file (sratom-kf :file-path))
           path-save   (gcomp/status-input-file (sratom-download))
           panel       (if mode
                         (build-main-panel [[l-name] [l-path]                    [f-name] [path-file]])
                         (build-main-panel [[l-name] [l-path "split 2"] [l-save] [f-name] [path-file "split 2"] [path-save]]))]
       (seesaw.core/config!
        path-file :listen
        [:property-change
         (fn [e]
           (let [file-name (last (s/split (.getToolTipText path-file)
                                          (re-pattern (java.io.File/separator))))]
             (if-not (empty? file-name)
               (do (seesaw.core/config! f-name :text file-name)
                   (.revalidate panel)
                   (.repaint panel))
               (seesaw.core/config! f-name :text (:file-name val)))))])
       panel)))
  )

(defn file-panel
  "Decscription
     Function return mig-panel with fields for composite component file-panel:
     login, password, file-name, file-path
   Keys
     :func - completed fn[e] with dispatch
     :func-save - daispatch for save file from ftp
     :val - obj with values to fill in the fields
     :mode - boolean, true means that insert-mode is on, so field for save file is not available
  "
  [{func :func
    func-save :func-save
    val  :val
    mode :mode}]
  (let [f-name     (input-field :file-name func val)
        l-name     (f-label "file-name:")
        l-file     (f-label "file-path:")
        l-save     (f-label "save file")
        path-file  (gcomp/status-input-file {:func (fn [e] (func e :file))
                                             :val  ""})
        path-save  (gcomp/status-input-file {:func (fn [v] (func-save v)) :val  "" :mode true})
        panel      (if mode  (build-main-panel (gtool/join-mig-items [l-name l-file f-name path-file]))
                       (build-main-panel [[l-name] [l-file "split 2"] [l-save] [f-name] [path-file "split 2"] [path-save]]))]
    (seesaw.core/config! path-file :listen [:property-change (fn [e]
                                                              (let [file-name  (last (s/split (.getToolTipText path-file) 
                                                                                              (re-pattern (java.io.File/separator))))]
                                                                (if-not (empty? file-name)
                                                                  (do (seesaw.core/config! f-name :text file-name)
                                                                      (.revalidate panel)
                                                                      (.repaint panel))
                                                                  (seesaw.core/config! f-name :text (:file-name val)))))]) panel))

(defn ftp-panel
  "Decscription
     Function return mig-panel with fields for composite component ftp-panel:
     login, password, file-name, file-path
   Keys
     :func - completed fn[e] with dispatch
     :func-save - daispatch for save file from db
     :val - obj with values to fill in the fields
     :mode - boolean, true means that insert-mode is on, so field for save file is not available"
  [{func :func
    func-save :func-save
    val  :val
    mode :mode}]
  (let [f-name   (input-field :file-name func val)
        l-name   (f-label "file-name:")
        l-path   (f-label "file-path:")
        l-save   (f-label "save file")
        path-file   (gcomp/status-input-file {:func (fn [v] (func v :file-path)) :val  (:file-path val)})
        path-save   (gcomp/status-input-file {:func (fn [v] (func-save v)) :val  "" :mode true})
        panel       (if mode (build-main-panel (gtool/join-mig-items [l-name l-path f-name path-file]))
                        (build-main-panel [[l-name] [l-path "split 2"]
                                           [l-save] [f-name] [path-file "split 2"] [path-save]]))]
    (seesaw.core/config! path-file :listen [:property-change (fn [e]
                                                               (let [file-name (last (s/split (.getToolTipText path-file)
                                                                                              (re-pattern (java.io.File/separator))))]
                                                                 (if-not (empty? file-name)
                                                                   (do (seesaw.core/config! f-name :text file-name)
                                                                       (.revalidate panel)
                                                                       (.repaint panel))
                                                                   (seesaw.core/config! f-name  :text (:file-name val)))))])
    panel))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; test view for composite components ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(comment
  (let [state (atom {})
        mig   (fn [items](seesaw.core/scrollable (seesaw.mig/mig-panel
                                                 :constraints ["wrap 1" "150px[]0px" "100px[]0px"]
                                                 :items [[items]])))
        ;; panel-ftp  (mig (url-panel {}))
        ;; panel-ftp  (mig (file-panel {}))
        panel-ftp  (mig (ftp-panel {}))]
    (-> (doto (seesaw.core/frame
               :title "DEBUG WINDOW" :undecorated? false
               :minimum-size [450 :by 450]
               :size [450 :by 450]
               :content panel-ftp)
          (.setLocationRelativeTo nil) seesaw.core/pack! seesaw.core/show!))))

