(ns jarman.gui.gui-extension-manager
  (:require
   ;; swing tools
   [seesaw.core   :as c]
   [seesaw.table  :as table]
   [seesaw.border  :as b]
   ;; GUI toolkit
   [jarman.faces  :as face]
   [jarman.gui.gui-components :as gcomp]
   [jarman.gui.gui-tools      :as gtool]
   [jarman.plugin.extension-manager :as extension-manager]
   [jarman.interaction  :as i]
   [jarman.tools.org    :refer :all]
   [jarman.gui.gui-migrid :as gmg]
   [jarman.gui.gui-style  :as gs])
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
         (print-error e#)
         (i/danger (gtool/get-lang-header :update-error) (.getMessage e#) :time 7)))
     (catch Exception e#
       (do
         (print-error e#)
         (i/danger (gtool/get-lang-header :update-error) (.getMessage e#) :time 7)))))

(defn setColumnWidth [^javax.swing.JTable table & {:keys [column size]}]
  (let [^javax.swing.table.TableColumnModel column-model (.getColumnModel table)
        ^javax.swing.table.TableColumn      table-column (.getColumn column-model column)]
    (.setPreferredWidth table-column size)))

;;;;;;;;;;;;;;;;;;;;;
;;; UI COMPONENTS ;;;
;;;;;;;;;;;;;;;;;;;;;

(defn info [s]
  {:pre [(string? s)]}
  (seesaw.core/label :halign :left :background face/c-background-detail :foreground face/c-foreground-title :text s :font (gtool/getFont 16 :bold) :border (b/empty-border :thickness 15)))


;;;;;;;;;;;;;;;;;;;;;;;
;;; UI Composititon ;;;
;;;;;;;;;;;;;;;;;;;;;;;

(defn startup-components [] [])

(defn manager-headers []
  (gmg/migrid
   :> "[150:150:150, fill]10px[50::, fill]"
   {:gap [10] :args [:background face/c-layout-background]}
   [(c/label :text (gtool/get-lang-basic :extension))
    (c/label :text (gtool/get-lang-basic :version))
    (c/label :text (gtool/get-lang-basic :description))]))

(defn ext-info [ext buttons]
  (gmg/migrid
   :> :gf {:args [:background face/c-compos-background]}
   (gtool/join-mig-items
    (gcomp/min-scrollbox
     (gmg/migrid
      :> "[150:150:150, fill]10px[50::, fill]"
      {:gap [10] :args [:border (b/line-border :top 2 :color face/c-layout-background)]}
      [(c/label :text (gtool/str-cutter (str (get ext :name)) 20)
                :tip (str (gtool/get-lang-basic :extension) ": " (get ext :name)))
       (c/label :text (str (get ext :version)))
       (c/label :text (str (get ext :description)) :tip "Shift + Scroll")]))
    buttons)))

(defn supply-content-all-extensions [items extension-list]
  (if (seq extension-list)
    (concat items
            [(gcomp/button-expand
              (gtool/get-lang-header :installed-extentions)
              [(manager-headers)
               (gcomp/min-scrollbox
                (gmg/migrid
                 :v {:args [:border (b/line-border :bottom 1 :color face/c-icon)]}
                 (for [ext extension-list]
                   (ext-info
                    ext
                    (gmg/migrid :> "[100::, fill]" {:args [:border (b/line-border :top 2 :color face/c-layout-background)]}
                     (gcomp/button-basic (gtool/get-lang-btns :reload)
                                         :onClick (fn [_] (try-catch-alert
                                                           (extension-manager/do-load-extensions ext)
                                                           (i/info (gtool/get-lang-header :extension-manager)
                                                                   (format (gtool/get-lang-alerts :reloaded-extension)
                                                                           (:name ext)) :time 7)))
                                         :underline-size 0)))))
                :hscroll-off true)]              
              :before-title #(c/label :icon (gs/icon GoogleMaterialDesignIcons/WIDGETS))
              :expand :always)])
    (conj items
          (info (gtool/get-lang-header :installed-extentions))
          (info "-- empty --"))))

(extension-manager/extension-storage-list-load)

(defn extension-manager-panel []
  (gmg/migrid
   :v {:gap [5 0]}
   (gtool/join-mig-items
    (-> (startup-components)
        (supply-content-all-extensions (extension-manager/extension-storage-list-get))))))

(comment
  (-> (c/frame :content (extention-manager-panel))
      c/pack!
      c/show!))

