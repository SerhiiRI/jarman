(ns jarman.tools.update-manager-gui
  (:require
   ;; swing tools
   [jarman.faces  :as face]
   [seesaw.core   :as c]
   [seesaw.table  :as table]
   [seesaw.dev :as sdev]
   [seesaw.mig :as smig]
   [seesaw.swingx  :as swingx]
   [seesaw.chooser :as chooser]
   [seesaw.border  :as b]
   [jarman.gui.gui-style :as gs]
   [jarman.gui.gui-views-service :as gvs]
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
   [jarman.tools.lang :refer [in?]]
   [jarman.tools.fs :as fs]
   [jarman.tools.update-manager :as update-manager]
   ;; environtemnt variables
   [jarman.config.environment :as env]
   [jarman.interaction :as i]
   [jarman.tools.org :refer :all])
  (:import (java.io IOException FileNotFoundException)
           (jiconfont.icons.google_material_design_icons GoogleMaterialDesignIcons)))


;;;;;;;;;;;;;;;;;;;;;;;;
;;; HELPER FUNCTIONS ;;;
;;;;;;;;;;;;;;;;;;;;;;;;

(defmacro try-catch-alert [& body]
  `(try
     (do ~@body)
     (catch clojure.lang.ExceptionInfo e#
       (do
         (out-update
          (print-error e#))
         (i/danger "Update error" (.getMessage e#) :time 7)))
     (catch Exception e#
       (do
         (out-update
          (print-error e#))
         (i/danger "Update error" (.getMessage e#) :time 7)))))

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
       :model (table/table-model :columns [{:key :file, :text "Package"}
                                           ;; {:key :name, :text "name"}
                                           {:key :version, :text "Version"}
                                           ;; {:key :artifacts, :text "artifacts"}
                                           {:key :uri, :text "Repository URI"}]
                                 :rows [package]))
    (.setRowMargin 10)
    (.setRowHeight 35)
    (.setIntercellSpacing (java.awt.Dimension. 10 0))
    (setColumnWidth :column 0 :size 130)
    (setColumnWidth :column 1 :size 30)
    (setColumnWidth :column 2 :size 600)
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
(defn supply-content-info [items]
  (conj items
        (info "System information:")
        (c/horizontal-panel
         :items
         [(package-content (update-manager/->PandaPackage "Current Version" update-manager/*program-name* update-manager/*program-vers* nil nil))
          (gcomp/button-basic "Clean Environment"
                              :onClick (fn [_] (try-catch-alert
                                               (update-manager/procedure-clean-env)
                                               (i/info "Update Manager" "Local Jarman configuraion was deleted" :time 7))))])))


(defn supply-content-to-install  [items package]
  (if package
    (conj items
          (info "Package to install:")
          (c/horizontal-panel
           :items 
           [(package-content package)
            (gcomp/button-basic "Install"
                                :onClick (fn [_] (try-catch-alert
                                                 (update-manager/procedure-update package)
                                                 (i/info "Update Manager" (format "Jarman %s installed successfully"
                                                                                     (:version package)) :time 7))))
            (gcomp/button-basic "Download"
                                :onClick (fn [_] (try-catch-alert
                                                 (update-manager/download-package package)
                                                 (i/info "Update Manager" (format "Package %s was downloaded"
                                                                                  (:file package)) :time 7))))]))
    items))

(defn supply-content-all-package [items package-list]
  (if (seq package-list)
    (concat items
            [(info "Available packages:")]
            (for [pkg package-list]
              (c/horizontal-panel
               :items 
               [(package-content pkg)
                (gcomp/button-basic (if (= (:version pkg) update-manager/*program-vers*) "Reinstall" "Downgrade")
                                    :onClick (fn [_] (try-catch-alert
                                                      (update-manager/procedure-update pkg)
                                                      (i/info "Update Manager"
                                                              (format "Downgrade from %s to %s version was successfully"
                                                                      update-manager/*program-vers*
                                                                      (:version pkg)) :time 7))))
                (gcomp/button-basic "Download"
                                    :onClick (fn [_] (try-catch-alert
                                                     (update-manager/download-package pkg)
                                                     (i/info "Update Manager"
                                                              (format "Package %s was downloaded"
                                                                      (:file pkg)) :time 7))))])))
    (conj items
          (info "Available packages:")
          (info "-- empty --"))))

;; (def package-list-chache (update-manager/get-filtered-packages update-manager/*repositories*))

(def package-list-chache [])
(.start
 (Thread.
  (fn []
    (def package-list-chache (update-manager/procedure-info)))))

(defn update-manager-panel []
  (let [package-list package-list-chache
        package-to-update (update-manager/max-version package-list)]
    (out-update
     (seesaw.mig/mig-panel :constraints ["wrap 1" "0px[grow, fill]0px" "0px[]0px"]
                           :background "#fff"
                           :items
                           (gtool/join-mig-items (-> (startup-components)
                                                     (supply-content-info)
                                                     (supply-content-to-install package-to-update)
                                                     (supply-content-all-package package-list)))))))

(defn alert-update-available []
  (i/warning "Updates are avaliable!" [{:title "Check updates"
                                        :func (fn [api]
                                                (gvs/add-view
                                                 :view-id  :update-manager
                                                 :title    "Update manager"
                                                 :render-fn update-manager-panel)
                                                ((:rm-alert api)))}
                                       {:title "Later"
                                        :func (fn [api] ((:rm-alert api)))}]
             :time 0))

(comment
  (-> (c/frame :content (update-manager-panel))
      c/pack!
      c/show!))

