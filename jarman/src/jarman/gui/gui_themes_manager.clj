(ns jarman.gui.gui-themes-manager
  (:require
   ;; swing tools
   [seesaw.core   :as c]
   [seesaw.table  :as table]
   [seesaw.dev :as sdev]
   [seesaw.mig :as smig]
   [seesaw.swingx  :as swingx]
   [seesaw.chooser :as chooser]
   [jarman.faces :as face]
   [seesaw.border  :as b]
   ;; clojure lib
   [clojure.string :as string]
   [clojure.pprint :refer [cl-format]]
   [clojure.java.io :as io]
   ;; local functionality
   [jarman.logic.state :as state]
   [jarman.tools.config-manager :as cm]
   [jarman.gui.gui-components :as gcomp]
   [jarman.gui.gui-tools :as gtool]
   [jarman.plugin.plugin :as plugin]
   [jarman.gui.gui-style :as gs]
   ;; environtemnt variables
   [jarman.config.environment :as env]
   [jarman.interaction        :as i])
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
       (i/warning "Plagin Manager" (.getMessage e#) :time 7)
       ;; (c/alert (str "Update Manager effrror: " (.getMessage e#) " Type:" (name (:type (ex-data e#)))))
       )
     (catch Exception e#
       (i/warning "Plagin Manager" (.getMessage e#) :time 7)
       ;; (c/alert (str "Update Manager Main error: " (.getMessage e#)))
       )))

(defn setColumnWidth [^javax.swing.JTable table & {:keys [column size]}]
  (let [^javax.swing.table.TableColumnModel column-model (.getColumnModel table)
        ^javax.swing.table.TableColumn      table-column (.getColumn column-model column)]
    (.setPreferredWidth table-column size)))

;;;;;;;;;;;;;;;;;;;;;
;;; UI COMPONENTS ;;;
;;;;;;;;;;;;;;;;;;;;;

(defn- theme-content [theme]
  (doto
      (c/table
       :show-horizontal-lines? nil
       :show-vertical-lines? nil
       :model (table/table-model :columns [{:key :theme-name}
                                           {:key :theme-description}
                                           ;; {:key :url}
                                           ]
                                 :rows [theme]))
    (.setRowMargin 10)
    (.setRowHeight 35)
    (.setIntercellSpacing (java.awt.Dimension. 10 0))
    (setColumnWidth :column 0 :size 240)
    (setColumnWidth :column 1 :size 1000)
    ;; (setColumnWidth :column 3 :size 600)
    (.setRowSelectionAllowed false)
    ;; (.setAutoResizeMode javax.swing.JTable/AUTO_RESIZE_LAST_COLUMN)
    ))

(defn info [s]
  {:pre [(string? s)]}
  (seesaw.core/label :halign :left :foreground face/c-foreground-title :text s :font (gs/getFont :bold) :border (b/empty-border :thickness 15)))


;;;;;;;;;;;;;;;;;;;;;;;
;;; UI Composititon ;;;
;;;;;;;;;;;;;;;;;;;;;;;

(defn startup-components [] [])

(defn supply-currently-loaded [items theme]
  (concat items
          [(info "Currently loaded")]
          [(theme-content theme)]))

(defn supply-content-all-themes [items theme-list]
  (if (seq theme-list)
    (concat items
            [(info "Choose theme")]
            (for [theme theme-list]
              (c/horizontal-panel
               :items 
               [(theme-content theme)
                (gcomp/button-basic "Apply theme"
                                    :onClick (fn [_] (try-catch-alert
                                                      (state/set-state :theme-name (:theme-name theme))
                                                      (i/soft-restart))))])))
    (conj items
          (info "Empty theme list")
          (info "-- empty --"))))

(defn theme-manager-panel []
  (seesaw.mig/mig-panel
   :constraints ["wrap 1" "0px[grow, fill]0px" "0px[]0px"]
   :items
   (gtool/join-mig-items
    (-> (startup-components)
        (supply-currently-loaded (plugin/selected-theme-get))
        (supply-content-all-themes (plugin/system-ThemePlugin-list-get))))))

(comment
  (-> (c/frame :content (theme-manager-panel))
      c/pack!
      c/show!))

