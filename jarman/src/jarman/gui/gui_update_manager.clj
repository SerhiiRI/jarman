(ns jarman.gui.gui-update-manager
  (:require
   ;; swing tools
   [seesaw.core   :as c]
   [seesaw.table  :as table]
   [seesaw.border  :as b]
   ;; gui
   [jarman.faces                  :as face]
   [jarman.gui.gui-components     :as gcomp]
   [jarman.gui.gui-tools          :as gtool]
   [jarman.gui.gui-migrid         :as gmg]
   [jarman.gui.gui-views-service  :as gvs]
   [jarman.gui.gui-alerts-service :as gas]
   [jarman.gui.gui-style          :as gs]
   ;; environtemnt variables
   [jarman.org :refer :all]
   [jarman.logic.update-manager :as update-manager]
   [jarman.gui.popup   :as popup])
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
         (gas/danger (gtool/get-lang-header :update-error) (.getMessage e#) :time 7)))
     (catch Exception e#
       (do
         (out-update
          (print-error e#))
         (gas/danger (gtool/get-lang-header :update-error) (.getMessage e#) :time 7)))))

(defn setColumnWidth [^javax.swing.JTable table & {:keys [column size]}]
  (let [^javax.swing.table.TableColumnModel column-model (.getColumnModel table)
        ^javax.swing.table.TableColumn      table-column (.getColumn column-model column)]
    (.setPreferredWidth table-column size)))

;;;;;;;;;;;;;;;;;;;;;
;;; UI COMPONENTS ;;;
;;;;;;;;;;;;;;;;;;;;;


(defn info [s]
  {:pre [(string? s)]}
  (gmg/migrid
   :> "[30%::, fill]" {:gap [10]}
   (seesaw.core/label
    :halign :left
    :background face/c-background-detail
    :foreground face/c-icon
    :text s
    :font (gtool/getFont 16 :bold)
    :border (b/empty-border :left 5))))

;;;;;;;;;;;;;;;;;;;;;;;
;;; UI Composititon ;;;
;;;;;;;;;;;;;;;;;;;;;;;

(defn startup-components [] [])

(defn manager-headers []
  (gmg/migrid
   :> "[150:150:150, fill]10px[50::, fill]"
   {:gap [10] :args [:background face/c-layout-background]}
   [(c/label :text (gtool/get-lang-basic :package))
    (c/label :text (gtool/get-lang-basic :version))
    (c/label :text (gtool/get-lang-basic :source))]))

(defn package-info [package buttons]
  (gmg/migrid
   :> :gf {:args [:background face/c-compos-background]}
   (gtool/join-mig-items
    (gcomp/min-scrollbox
     (gmg/migrid
      :> "[150:150:150, fill]10px[50::, fill]"
      {:gap [10] :args [:border (b/line-border :top 2 :color face/c-layout-background)]}
      [(c/label :text (gtool/str-cutter (str (get package :file)) 20)
                :tip (str (gtool/get-lang-basic :package) ": " (get package :file)))
       (c/label :text (str (get package :version)))
       (c/label :text (str (get package :uri))
                :listen [:mouse-clicked (fn [e] (popup/build-popup
                                                 {:title   (get package :file)
                                                  :size    [600 300]
                                                  :comp-fn (fn [] (c/label :text (gtool/htmling (str (get package :uri)))))}))])]))
    buttons)))

(defn supply-content-info [items]
  (conj items
        ;;(info (gtool/get-lang-header :system-information))
        (gcomp/button-expand
         (gtool/get-lang-header :system-information)
         (let [package (update-manager/->PandaPackage (gtool/get-lang-header :current-version)
                                                      update-manager/*program-name*
                                                      update-manager/*program-vers* nil nil)]
           (gmg/migrid
            :> :gf {:args [:background face/c-compos-background :border (b/line-border :bottom 1 :color face/c-icon)]}
            [(package-info
              package
              (gmg/migrid
               :> :f {:args [:border (b/line-border :top 2 :color face/c-layout-background)]}
               (gcomp/button-basic (gtool/get-lang-btns :clean-environment)
                 :onClick (fn [_] (try-catch-alert
                                   (update-manager/procedure-clean-env)
                                   (gas/info
                                     (gtool/get-lang-header :update-manager)
                                     (gtool/get-lang-alerts :config-deleted)
                                     :time 7)))
                                   :underline-size 0)))]))
         ;; :font (gs/getFont :bold 16)
         :expand :always
         :before-title #(c/label :icon (gs/icon GoogleMaterialDesignIcons/SETTINGS)))))


(defn supply-content-to-install  [items package]
  (if package
    (conj items
          ;;(info (gtool/get-lang-header :packages-to-install))
          (gcomp/button-expand
           (gtool/get-lang-header :packages-to-install)
           (gmg/migrid
            :v {:args [:border (b/line-border :bottom 1 :color face/c-icon)]}
            [(manager-headers)
             (package-info
              package
              (gmg/migrid
               :> "[90::, fill]" {:args [:border (b/line-border :top 2 :color face/c-layout-background)]}
               [(gcomp/button-basic (gtool/get-lang-btns :install)
                                    :onClick (fn [_] (try-catch-alert
                                                      (update-manager/procedure-update package)
                                                      (gas/info (gtool/get-lang-header :update-manager)
                                                              (format (gtool/get-lang-alerts :plugin-installed)
                                                                      (:version package)) :time 7)))
                                    :underline-size 0)
                (gcomp/button-basic (gtool/get-lang-btns :download)
                                    :onClick (fn [_] (try-catch-alert
                                                      (update-manager/download-package package)
                                                      (gas/info (gtool/get-lang-header :update-manager)
                                                              (format (gtool/get-lang-alerts :package-downloaded)
                                                                      (:file package)) :time 7)))
                                    :underline-size 0)]))])
           :before-title #(c/label :icon (gs/icon GoogleMaterialDesignIcons/MOVE_TO_INBOX))
           ))
    items))


(defn supply-content-all-package [items package-list]
  (if (seq package-list)
    (concat items
           ;; [(info (gtool/get-lang-header :available-packages))]
            [(gcomp/button-expand
              (gtool/get-lang-header :available-packages)
              [(manager-headers)
               (gcomp/min-scrollbox
                (gmg/migrid
                 :v {:args [:border (b/line-border :bottom 1 :color face/c-icon)]}
                 (for [pkg package-list]
                   (package-info
                    pkg
                    (gmg/migrid
                     :> "[90::, fill]" {:args [:border (b/line-border :top 2 :color face/c-layout-background)]}
                     [(gcomp/button-basic (if (= (:version pkg) update-manager/*program-vers*)
                                            (gtool/get-lang-btns :reinstall)
                                            (gtool/get-lang-btns :downgrade))
                                          :onClick (fn [_] (try-catch-alert
                                                            (update-manager/procedure-update pkg)
                                                            (gas/info (gtool/get-lang-header :update-manager)
                                                                    (format (gtool/get-lang-alerts :downgrade-success)
                                                                            update-manager/*program-vers*
                                                                            (:version pkg)) :time 7)))
                                          :underline-size 0)
                      (gcomp/button-basic (gtool/get-lang-btns :download)
                                          :onClick (fn [_] (try-catch-alert
                                                            (update-manager/download-package pkg)
                                                            (gas/info (gtool/get-lang-header :update-manager)
                                                                    (format (gtool/get-lang-alerts :package-downloaded)
                                                                            (:file pkg)) :time 7)))
                                          :underline-size 0)]))))
                :hscroll-off true)]
              :before-title #(c/label :icon (gs/icon GoogleMaterialDesignIcons/WIDGETS)))])
    (conj items
          (info (gtool/get-lang-header :available-packages))
          (info (format "-- %s --" (gtool/get-lang-header :available-packages))))))

;; (def package-list-chache (update-manager/get-filtered-packages update-manager/*repositories*))

(def package-list-chache [])
(defn update-package-list-cache []
  (.start
   (Thread.
    (fn []
      (def package-list-chache (update-manager/procedure-info))))))


(defn update-manager-panel []
  (let [package-list package-list-chache
        package-to-update (update-manager/max-version package-list)]
    (out-update
     (gmg/migrid
      :v {:gap [5 0]}
      (gtool/join-mig-items (-> (startup-components)
                                (supply-content-info)
                                (supply-content-to-install package-to-update)
                                (supply-content-all-package package-list)))))))

(defn alert-update-available
  ([] (alert-update-available nil))
  ([update-info]
   (gas/warning
     (gtool/get-lang-header :avaliable-updates)
     [{:title (gtool/get-lang-btns :check-updates)
       :func (fn [api]
               (gvs/add-view
                 :view-id  :update-manager
                 :title    (gtool/get-lang-header :update-manager)
                 :render-fn update-manager-panel)
               ((:rm-alert api)))}
      {:title (gtool/get-lang-btns :later)
       :func (fn [api] ((:rm-alert api)))}]
     :expand (if update-info
               (fn [] (gmg/migrid
                       :v :f :f {:gap [10 0]}
                       [(c/label
                          :text
                          (gtool/htmling
                            (str
                              "<table>"
                              "<tr><td>Name</td><td>" (:name      update-info)
                              "<tr><td>File</td><td>" (:file      update-info)
                              "<tr><td>Ver</td><td>"  (:version   update-info)
                              "<tr><td>Type</td><td>" (:artifacts update-info)
                              "</table>")))]))
               nil)
     :time 0)))

(defn check-update
  ([] (check-update :basic))
  ([mode]
   (let [update-info (update-manager/check-package-for-update)]
     (if update-info
       (alert-update-available update-info)
       (if (= mode :silent)
         (print-header "System is updated. No avaliable updates.")
         (gas/success
           (gtool/get-lang-header :system-updated)
           (gtool/get-lang-alerts :no-updates)))))))

(comment
  (check-update)
  (-> (c/frame :content (update-manager-panel))
      c/pack!
      c/show!))

