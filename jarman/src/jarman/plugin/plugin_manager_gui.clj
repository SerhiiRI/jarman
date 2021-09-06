(ns jarman.plugin.plugin-manager-gui
  (:require
   ;; swing tools
   [seesaw.core   :as c]
   [seesaw.table  :as table]
   [seesaw.dev :as sdev]
   [seesaw.mig :as smig]
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
   [jarman.plugin.plugin-manager :as plugin-manager]
   ;; environtemnt variables
   [jarman.config.environment :as env])
  (:import (java.io IOException FileNotFoundException)))


;;;;;;;;;;;;;;;;;;;;;;;;
;;; HELPER FUNCTIONS ;;;
;;;;;;;;;;;;;;;;;;;;;;;;

(defmacro try-catch-alert [& body]
  `(try
     ;; (ex-info "The ice cream has melted!" 
     ;;          {:current-temperature {:value 25 :unit :celsius}})
     (do ~@body)
     (catch clojure.lang.ExceptionInfo e#
       ((state/state :alert-manager)               
        :set {:header "Plagin Manager"
              :body   (.getMessage e#)}  7)
       ;; (c/alert (str "Update Manager effrror: " (.getMessage e#) " Type:" (name (:type (ex-data e#)))))
       )
     (catch Exception e#
       ((state/state :alert-manager)               
        :set {:header "Plagin Manager"
              :body   (.getMessage e#)}  7)
       ;; (c/alert (str "Update Manager Main error: " (.getMessage e#)))
       )))

(defn setColumnWidth [^javax.swing.JTable table & {:keys [column size]}]
  (let [^javax.swing.table.TableColumnModel column-model (.getColumnModel table)
        ^javax.swing.table.TableColumn      table-column (.getColumn column-model column)]
    (.setPreferredWidth table-column size)))


;;;;;;;;;;;;;;;;;;;;;
;;; UI COMPONENTS ;;;
;;;;;;;;;;;;;;;;;;;;;

(defn package-content [package]
  (doto
      (c/table
       :show-horizontal-lines? nil
       :show-vertical-lines? nil
       :model (table/table-model :columns [{:key :name}
                                           {:key :version}
                                           {:key :description}
                                           ;; {:key :url}
                                           ]
                                 :rows [package]))
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
  (seesaw.core/label :halign :left :background "#fff" :foreground "#074a4f" :text s :font (gtool/getFont 16 :bold) :border (b/empty-border :thickness 15)))


;;;;;;;;;;;;;;;;;;;;;;;
;;; UI Composititon ;;;
;;;;;;;;;;;;;;;;;;;;;;;

(defn startup-components [] [])

(defn supply-content-all-package [items package-list]
  (if (seq package-list)
    (concat items
            [(info "Installed plugins:")]
            (for [pkg package-list]
              (c/horizontal-panel
               :items 
               [(package-content pkg)
                (gcomp/button-basic "Reload"
                                    :onClick (fn [_] (try-catch-alert
                                                     (plugin-manager/do-load-plugins pkg)
                                                     ((state/state :alert-manager)               
                                                      :set {:header "Plugin manager"
                                                            :body   (format "plugin `%s` successfully reloaded"
                                                                            (:name pkg))} 7))))])))
    (conj items
          (info "Installed plugins:")
          (info "-- empty --"))))

(plugin-manager/package-storage-list-load)

(defn plugin-manager-panel []
  (seesaw.mig/mig-panel :constraints ["wrap 1" "0px[grow, fill]0px" "0px[]0px"]
                        :background "#fff"
                        :items
                        (gtool/join-mig-items
                         (-> (startup-components)
                             (supply-content-all-package (plugin-manager/package-storage-list-get))))))

(comment
  (-> (c/frame :content (plugin-manager-panel))
      c/pack!
      c/show!))

