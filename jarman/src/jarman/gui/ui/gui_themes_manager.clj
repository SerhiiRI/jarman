(ns jarman.gui.ui.gui-themes-manager
  (:require
   ;; swing tools
   [seesaw.core   :as c]
   [seesaw.table  :as table]
   [seesaw.border  :as b]
   ;; gui
   [jarman.faces                 :as face]
   [jarman.gui.gui-components    :as gcomp]
   [jarman.gui.gui-tools         :as gtool]
   [jarman.gui.gui-migrid        :as gmg]
   [jarman.gui.gui-views-service :as gvs]
   [jarman.gui.gui-style         :as gs]
   ;; environtemnt variables
   [jarman.logic.state :as state]
   [jarman.gui.gui-alerts-service :as i]
   [jarman.org :refer :all]
   [jarman.application.collector-custom-themes]
   [jarman.gui.popup      :as popup])
  (:import (java.io IOException FileNotFoundException)
           (jiconfont.icons.google_material_design_icons GoogleMaterialDesignIcons)))

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

(defn info [s]
  {:pre [(string? s)]}
  (seesaw.core/label :halign :left :foreground face/c-foreground-title :text s :font (gs/getFont :bold) :border (b/empty-border :thickness 15)))


;;;;;;;;;;;;;;;;;;;;;;;
;;; UI Composititon ;;;
;;;;;;;;;;;;;;;;;;;;;;;

(defn startup-components [] [])

(defn manager-headers []
  (gmg/migrid
   :> "[150:150:150, fill]10px[50::, fill]"
   {:gap [10] :args [:background face/c-layout-background]}
   [(c/label :text (gtool/get-lang-basic :theme))
    (c/label :text (gtool/get-lang-basic :description))]))

(defn theme-info [theme buttons]
  (gmg/migrid
   :> :gf {:args [:background face/c-compos-background]}
   (gtool/join-mig-items
    (gmg/migrid
     :> "[150:150:150, fill]10px[50::, fill]"
     {:gap [10] :args [:border (b/line-border :top 2 :color face/c-layout-background)]}
     [(c/label :text (gtool/str-cutter (str (get theme :theme-name)) 20)
               :tip (str (gtool/get-lang-basic :theme) ": " (get theme :theme-name)))
      (c/label :text (str (get theme :theme-description))
               :listen [:mouse-clicked (fn [e] (popup/build-popup
                                                 {:title   (get theme :theme-name)
                                                  :size    [600 300]
                                                  :comp-fn (fn [] (c/label :text (gtool/htmling (str (get theme :theme-description)))))}))])])
    buttons)))

(defn supply-currently-loaded [items theme]
  (concat items
          [(gcomp/button-expand
            (gtool/get-lang-header :loaded-theme)
            (gmg/migrid
             :> :gf {:args [:background face/c-compos-background :border (b/line-border :bottom 1 :color face/c-icon)]}
             [(theme-info (doto theme println) [])])
            :expand :always
            :before-title #(c/label :icon (gs/icon GoogleMaterialDesignIcons/SETTINGS)))]))

(defn supply-content-all-themes [items theme-list]
  (if (seq theme-list)
    (concat items
            [(gcomp/button-expand
              (gtool/get-lang-header :choose-theme)
              [(manager-headers)
               (gcomp/min-scrollbox
                (gmg/migrid
                 :v {:args [:border (b/line-border :bottom 1 :color face/c-icon)]}
                 (for [thm theme-list]
                   (theme-info
                    thm
                    (gmg/migrid
                     :> "[100::, fill]" {:args [:border (b/line-border :top 2 :color face/c-layout-background)]}
                     [(gcomp/button-basic (gtool/get-lang-btns :apply)
                                    :onClick (fn [_] (try-catch-alert
                                                      (state/set-state :theme-name (:theme-name thm))
                                                      (gvs/soft-restart)))
                                    :underline-size 0)]))))
                :hscroll-off true)]
              :before-title #(c/label :icon (gs/icon GoogleMaterialDesignIcons/FORMAT_PAINT))
              :expand :always)])
    (conj items
          (info "Empty theme list")
          (info "-- empty --"))))

(defn theme-manager-panel []
  (gmg/migrid
   :v {:gap [5 0]}
   (gtool/join-mig-items
    (-> (startup-components)
        (supply-currently-loaded (jarman.application.collector-custom-themes/selected-theme-get))
        (supply-content-all-themes (jarman.application.collector-custom-themes/system-ThemePlugin-list-get))))))

(comment
  (-> (c/frame :content (theme-manager-panel))
      c/pack!
      c/show!))

