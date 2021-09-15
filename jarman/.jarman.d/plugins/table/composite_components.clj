(ns plugin.table.composite-components
  (:require
   [clojure.string :as s]
   [jarman.gui.gui-tools :as gtool]
   [jarman.gui.gui-components :as gcomp]
   [jarman.logic.composite-components :as ccomp]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; helper functions for building panel ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn build-main-panel [data-vec]
  (seesaw.mig/mig-panel
   :background  "#e4e4e4"
   :constraints ["wrap 2" "10px[95:, fill, grow]10px" "10px[]10px"]
   :items data-vec))

(defn input-field [defr-key func val]
  (gcomp/state-input-text {:func (fn [e] (func e defr-key))
                           :val (defr-key val)}))

(defn f-label [text] (seesaw.core/label :text text :font (gtool/getFont)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; panels with composite components ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn url-panel
  "Decscription
     Function return mig-panel with fields for composite component file-panel:
     login, password, file-name, file-path
   Keys
     :func - completed fn[e] with dispatch
     :val - obj with values to fill in the fields
  "
  [{func :func  
    val  :val}]
  (let [f-title  (gcomp/state-input-text {:func (fn [e] (func e :text))
                                          :val  (:text val)})
        f-url    (gcomp/state-input-text {:func (fn [e] (func e :link))
                                          :val  (:link val)})]
    (seesaw.mig/mig-panel
     :background  "#e4e4e4"
     :constraints ["wrap 1" "10px[95:, fill, grow]10px" "10px[]10px"]
     :items [[(f-label "text:")] [f-title] [(f-label "url:")] [f-url]])))

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
     :mode - boolean, true means that insert-mode is on, so field for save file is not available
   
  "
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
(comment (let [state (atom {})
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



