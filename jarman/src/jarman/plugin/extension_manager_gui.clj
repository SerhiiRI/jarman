(ns jarman.plugin.extension-manager-gui
  (:require
   ;; swing tools
   [seesaw.core   :as c]
   [seesaw.table  :as table]
   [seesaw.dev :as sdev]
   [seesaw.mig :as smig]
   [jarman.faces  :as face]
   [seesaw.swingx  :as swingx]
   [seesaw.chooser :as chooser]
   [seesaw.border  :as b]
   ;; clojure lib
   [clojure.string :as string]
   [clojure.pprint :refer [cl-format]]
   [clojure.java.io :as io]
   ;; packages 
   [miner.ftp :as ftp]
   [me.raynes.fs :as gfs]
   ;; local functionality
   [jarman.logic.state :as state]
   [jarman.tools.config-manager :as cm]
   [jarman.gui.gui-components :as gcomp]
   [jarman.gui.gui-tools :as gtool]
   ;; [jarman.tools.lang :refer [in?]]
   ;; [jarman.tools.fs :as fs]
   [jarman.plugin.extension-manager :as extension-manager]
   ;; environtemnt variables
   [jarman.config.environment :as env]
   [jarman.interaction        :as i]
   [jarman.tools.org :refer :all])
  (:import (java.io IOException FileNotFoundException)))


;;;;;;;;;;;;;;;;;;;;;;;;
;;; HELPER FUNCTIONS ;;;
;;;;;;;;;;;;;;;;;;;;;;;;

(defmacro try-catch-alert [& body]
  `(try
     (do ~@body)
     (catch clojure.lang.ExceptionInfo e#
       (do
         (print-error e#)
         (i/danger "Plugin error" (.getMessage e#) :time 7)))
     (catch Exception e#
       (do
         (print-error e#)
         (i/danger "Plugin error" (.getMessage e#) :time 7)))))

(defn setColumnWidth [^javax.swing.JTable table & {:keys [column size]}]
  (let [^javax.swing.table.TableColumnModel column-model (.getColumnModel table)
        ^javax.swing.table.TableColumn      table-column (.getColumn column-model column)]
    (.setPreferredWidth table-column size)))

;;;;;;;;;;;;;;;;;;;;;
;;; UI COMPONENTS ;;;
;;;;;;;;;;;;;;;;;;;;;

(defn extension-content [extension]
  (doto
      (c/table
       :show-horizontal-lines? nil
       :show-vertical-lines? nil
       :model (table/table-model :columns [{:key :name}
                                           {:key :version}
                                           {:key :description}
                                           ;; {:key :url}
                                           ]
                                 :rows [extension]))
    (.setRowMargin 10)
    (.setRowHeight 35)
    (.setIntercellSpacing (java.awt.Dimension. 10 0))
    (setColumnWidth :column 0 :size 130)
    (setColumnWidth :column 1 :size 30)
    (setColumnWidth :column 2 :size 600)
    ;; (setColumnWidth :column 3 :size 600)
    (.setRowSelectionAllowed false)
    ;; (.setAutoResizeMode javax.swing.JTable/AUTO_RESIZE_LAST_COLUMN)
    ))

(defn info [s]
  {:pre [(string? s)]}
  (seesaw.core/label :halign :left :background face/c-background-detail :foreground face/c-foreground-title :text s :font (gtool/getFont 16 :bold) :border (b/empty-border :thickness 15)))


;;;;;;;;;;;;;;;;;;;;;;;
;;; UI Composititon ;;;
;;;;;;;;;;;;;;;;;;;;;;;

(defn startup-components [] [])

(defn supply-content-all-extensions [items extension-list]
  (if (seq extension-list)
    (concat items
            [(info "Installed plugins:")]
            (for [ext extension-list]
              (c/horizontal-panel
               :items 
               [(extension-content ext)
                (gcomp/button-basic "Reload"
                                    :onClick (fn [_] (try-catch-alert
                                                      (extension-manager/do-load-extensions ext)
                                                      (i/info "Extension manager"
                                                              (format "Extension `%s` successfully reloaded"
                                                                            (:name ext)) :time 7))))])))
    (conj items
          (info "Installed extentions:")
          (info "-- empty --"))))

(extension-manager/extension-storage-list-load)

(defn extension-manager-panel []
  (seesaw.mig/mig-panel
   :constraints ["wrap 1" "0px[grow, fill]0px" "0px[]0px"]
   :background face/c-background-detail
   :items
   (gtool/join-mig-items
    (-> (startup-components)
        (supply-content-all-extensions (extension-manager/extension-storage-list-get))))))

(comment
  (-> (c/frame :content (extention-manager-panel))
      c/pack!
      c/show!))

