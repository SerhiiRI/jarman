;;  _____ ___  ____   ___
;; |_   _/ _ \|  _ \ / _ \
;;   | || | | | | | | | | |
;;   | || |_| | |_| | |_| |
;;   |_| \___/|____/ \___/
;; -------------------------
;; Markers:
;;  [ ] todo
;;  [W] work in progress
;;  [X] done
;;  [-] Cenceled
;; -------------------------
;; fixme:serhii:aleks gui-components2 central
;;  [X] create some standarization for all font's
;;  [x] all wrapper's and any type of action should be extenralize outta here
;;  [X] common component router namespaces
;;  [X] keyboard sequence Action like in Emacs
;;  [X] wrote some demo's on bottom of 'jarman.gui.gui-components2
;;  [X] delete deprecated java objects
;;  [X] define 'jarman.gui.jsgl.actions with definition of test keyboard Actions.
;;  [X] create quick text popup which dispose or chnage in every keyboard click
(ns jarman.gui.gui-components2
  (:require
   [potemkin.namespaces :refer [import-vars]]
   [jarman.gui.components.common]
   [jarman.gui.components.system]
   [jarman.gui.components.datetime]
   [jarman.gui.components.calendar]
   [jarman.gui.components.composites]
   [jarman.gui.components.panels]
   [jarman.gui.components.database-table]
   [jarman.gui.components.simple-table]
   [jarman.gui.components.error]
   [jarman.application.collector-custom-metacomponents
    :refer [register-metacomponent get-comp-actions]]))

;;   ____ ___  __  __ ____   ___  _   _ _____ _   _ _____
;;  / ___/ _ \|  \/  |  _ \ / _ \| \ | | ____| \ | |_   _|
;; | |  | | | | |\/| | |_) | | | |  \| |  _| |  \| | | |
;; | |__| |_| | |  | |  __/| |_| | |\  | |___| |\  | | |
;;  \____\___/|_|  |_|_|    \___/|_| \_|_____|_| \_| |_|
;;  ____   ___  _   _ _____ _____ ____
;; |  _ \ / _ \| | | |_   _| ____|  _ \
;; | |_) | | | | | | | | | |  _| | |_) |
;; |  _ <| |_| | |_| | | | | |___|  _ <
;; |_| \_\\___/ \___/  |_| |_____|_| \_\
;;

(import-vars

 ;; -- STATEFULL PANELS
 [jarman.gui.components.panels
  border-panel box-panel
  vertical-panel horizontal-panel
  grid-panel mig-panel]

 ;; -- SIMPLE COMPONENTS
 [jarman.gui.components.common
  label-h1 label-h2 label-h3
  label-h4 label-h5 label-h6
  label label-link label-info
  button combobox stub
  text textarea codearea
  ;; ----
  parse-float parse-digit
  check-field-re    check-field-fn
  check-field-float check-field-digit]

 ;; -- SYSTEM COMPONENTS
 [jarman.gui.components.system
  input-file
  file-dialog]

 ;; -- CALENDAR COMPONENT
 [jarman.gui.components.calendar
  calendar
  calendar-label]

 ;; -- DATETIME COMPONENT
 [jarman.gui.components.datetime
  datetime
  datetime-label]

 ;; -- COMPOSITE COMPONENTS
 [jarman.gui.components.composites
  url-panel
  file-panel]

 [jarman.gui.components.database-table
  database-table]

 [jarman.gui.components.simple-table
  simple-table]

 [jarman.gui.components.error
  catch-error-panel])

;;  ____  _____ ____ ___ ____ _____ _____ ____
;; |  _ \| ____/ ___|_ _/ ___|_   _| ____|  _ \
;; | |_) |  _|| |  _ | |\___ \ | | |  _| | |_) |
;; |  _ <| |__| |_| || | ___) || | | |___|  _ <
;; |_| \_\_____\____|___|____/ |_| |_____|_| \_\
;;   ____ ___  __  __ ____   ___  _   _ _____ _   _ _____ _
;;  / ___/ _ \|  \/  |  _ \ / _ \| \ | | ____| \ | |_   _( )___
;; | |  | | | | |\/| | |_) | | | |  \| |  _| |  \| | | | |// __|
;; | |__| |_| | |  | |  __/| |_| | |\  | |___| |\  | | |   \__ \
;;  \____\___/|_|  |_|_|    \___/|_| \_|_____|_| \_| |_|   |___/
;;

;; todo
;; [ ] :jsgl-ftp-file
;; [ ] :jsgl-style-text JTextPane
(register-metacomponent :id :jsgl-stub            :component stub           :actions #{})
(register-metacomponent :id :jsgl-label           :component label          :actions (get-comp-actions 'label))
(register-metacomponent :id :jsgl-link            :component stub           :actions (get-comp-actions 'stub))
(register-metacomponent :id :jsgl-button          :component button         :actions (get-comp-actions 'button))
(register-metacomponent :id :jsgl-combobox        :component combobox       :actions (get-comp-actions 'combobox))
(register-metacomponent :id :jsgl-text            :component text           :actions (get-comp-actions 'text))
(register-metacomponent :id :jsgl-textarea        :component textarea       :actions (get-comp-actions 'textarea))
(register-metacomponent :id :jsgl-codearea        :component codearea       :actions (get-comp-actions 'codearea))
(register-metacomponent :id :jsgl-input-file      :component input-file     :actions (get-comp-actions 'input-file))
(register-metacomponent :id :jsgl-file-dialog     :component file-dialog    :actions (get-comp-actions 'file-dialog))
(register-metacomponent :id :jsgl-calendar        :component calendar       :actions (get-comp-actions 'calendar))
(register-metacomponent :id :jsgl-calendar-label  :component calendar-label :actions (get-comp-actions 'calendar-label))
(register-metacomponent :id :jsgl-datetime        :component datetime       :actions (get-comp-actions 'datetime))
(register-metacomponent :id :jsgl-datetime-label  :component datetime-label :actions (get-comp-actions 'datetime-label))
(register-metacomponent :id :jsgl-url-panel       :component url-panel      :actions (get-comp-actions 'url-panel)       :constructor  jarman.logic.metadata/map->Link)
(register-metacomponent :id :jsgl-file-panel      :component file-panel     :actions (get-comp-actions 'file-panel)      :constructor  jarman.logic.metadata/map->File)
;; (register-metacomponent :id :jsgl-listbox         :component select-panel   :actions (get-comp-actions 'listbox))
(register-metacomponent :id :jsgl-database-table  :component database-table :actions (get-comp-actions 'database-table))
(register-metacomponent :id :jsgl-simple-table    :component simple-table   :actions (get-comp-actions 'simple-table))

;;  ____  _____ __  __  ___
;; |  _ \| ____|  \/  |/ _ \
;; | | | |  _| | |\/| | | | |
;; | |_| | |___| |  | | |_| |
;; |____/|_____|_|  |_|\___/
;;

(comment
  ;; SOME HELPERS
  (seesaw.dev/show-events  (javax.swing.JFrame.))
  (seesaw.dev/show-events  (seesaw.core/combobox))
  (seesaw.dev/show-options (gui-panels/grid-panel))
  (seesaw.dev/show-options (seesaw.core/combobox))
  ;; IMPORTS
  (require
   '[seesaw.dev]
   '[seesaw.core          :as c]
   '[seesaw.border        :as b]
   '[jarman.gui.gui-tools :as gtool]
   '[jarman.faces         :as face])
  ;; ----------------------------
  ;; jarman.gui.components.common
  (doto (seesaw.core/frame
         :title "Jarman"
         :content (seesaw.mig/mig-panel
                   :background  face/c-compos-background-darker
                   :constraints ["wrap 1" "0px[grow, fill]0px" "0px[fill, top]0px"]
                   :border (b/empty-border :thickness 10)
                   :items [[(label :value "(label) test component")]
                           [(button :value "(button) test component ")]
                           [(text :value "(text) input field which (support (Emacs)) keybindings")]
                           [(textarea :value "(textarea) multiline text-area field\n which (support (Emacs)) keybindings\nand wrapped through the scroll-\nbar")]
                           [(codearea :value ";;(codearea) which (support\n;; (Emacs)) keybindings\n\n(fn [x] \n\t(label :value \"Some test label\"))" :language :clojure)]
                           [(combobox :value "A" :model ["A" "B" "C"])]
                           [(combobox :value 1 :model [1 2 3])]]))
    (.setLocationRelativeTo nil) seesaw.core/pack! seesaw.core/show!)
  ;; ----------------------------
  ;; jarman.gui.components.system
  (doto (seesaw.core/frame
         :title "Jarman"
         :content (seesaw.mig/mig-panel
                   :background  face/c-compos-background-darker
                   :constraints ["wrap 1" "0px[grow, fill]0px" "0px[fill, top]0px"]
                   :border (b/empty-border :thickness 10)
                   :items [[(seesaw.core/label :text "Begin Chooser")]
                           [(input-file)]
                           [(seesaw.core/label :text "End Chooser")]]))
    (.setLocationRelativeTo nil) seesaw.core/pack! seesaw.core/show!)
  ;; ----------------------------
  ;; jarman.gui.components.<calendar/datetime>
  (doto (seesaw.core/frame
         :title "Jarman"
         :content (seesaw.mig/mig-panel
                   :background  face/c-compos-background-darker
                   :constraints ["wrap 1" "0px[grow, fill]0px" "0px[fill, top]0px"]
                   :border (b/empty-border :thickness 10)
                   :items [[(seesaw.core/label :text "Calendar Component" :font (jarman.gui.gui-tools/getFont :bold 20))]
                           [(calendar-label :value "2020-10-01 10:22")]
                           [(seesaw.core/label :text "DateTime Component" :font (jarman.gui.gui-tools/getFont :bold 20))]
                           [(datetime-label :value "2020-10-01 10:22")]]))
    (.setLocationRelativeTo nil)
    seesaw.core/pack! seesaw.core/show!))
