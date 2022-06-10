(ns jarman.gui.ui.gui-permission-listing
  (:require
   ;; swing tools
   [seesaw.core   :as c]
   [seesaw.border  :as b]
   [seesaw.table  :as table]
   ;; clojure lib
   [clojure.string :as string]
   ;; local functionality
   [jarman.faces :as face]
   [jarman.gui.gui-components :as gcomp]
   [jarman.gui.gui-tools      :as gtool]
   [jarman.gui.gui-style      :as gs]
   [jarman.gui.gui-migrid     :as gmg]
   [jarman.application.permissions  :as permission]
   [jarman.gui.popup   :as popup]
   [jarman.logic.state :as state])
  (:import (java.io IOException FileNotFoundException)
           (jiconfont.icons.google_material_design_icons GoogleMaterialDesignIcons)))

;;;;;;;;;;;;;;;;;;;;;;;;
;;; HELPER FUNCTIONS ;;;
;;;;;;;;;;;;;;;;;;;;;;;;

;;;;;;;;;;;;;;;;;;;;;
;;; UI COMPONENTS ;;;
;;;;;;;;;;;;;;;;;;;;;

(defn info [s]
  {:pre [(string? s)]}
  (seesaw.core/label :halign :left :foreground face/c-foreground-title :text s :font (gs/getFont :bold) :border (b/empty-border :thickness 15)))

;;;;;;;;;;;;;;;;;;;;;;;
;;; UI Composititon ;;;
;;;;;;;;;;;;;;;;;;;;;;;

(defn permission-content [[permission-name {descriptions :doc}]]
  (gmg/migrid
   :> "[200:200:200, fill]10px[50::, fill]"
   {:gap [10] :args [:border (b/line-border :top 2 :color face/c-layout-background)]}
   [(c/label :text permission-name :tip permission-name)
    (c/label :text descriptions)]))

(defn view-list-all [items permission-list]
  (gtool/join-mig-items
   (if (seq permission-list)
     (into items
          (for [v permission-list]
            (gmg/migrid :v (permission-content v))))
     (conj items
           (info "Variables cannot be empty. Jarman error")))))

(defn permission-listing-panel []
  (let [permission-list
        (->> (seq (permission/permission-groups-get)) (map (fn [[k v]] [(name k) v])) (sort-by first))
        panel
        (gmg/migrid :v :a "[shrink 0]" {:gap [5 0]}
                    (view-list-all [] permission-list))]
    (gmg/migrid-resizer (state/state :views-space) panel :vars-listing :gap [5 0])
    (gmg/migrid
     :v [(gcomp/min-scrollbox panel)])))

