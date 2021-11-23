(ns jarman.gui.gui-config-panel
  (:require [clojure.string     :as string]
            ;; [clojure.core.async :refer [thread]]
            [seesaw.swingx      :as swingx]
            [seesaw.core        :as c]
            [seesaw.util        :as u]
            [seesaw.border      :as b]
            ;; external funcionality
            [jarman.faces                    :as face]
            [jarman.interaction              :as i]
            ;; logic
            [jarman.logic.state              :as state]
            [jarman.logic.session            :as session]
            [jarman.tools.org                :refer :all]
            [jarman.tools.lang               :refer :all]
            ;; gui 
            [jarman.gui.gui-components       :as gcomp]
            [jarman.gui.gui-tools            :as gtool]
            [jarman.gui.gui-style            :as gs]
            [jarman.gui.gui-migrid           :as gmg]
            [seesaw.chooser                  :as chooser]
            [jarman.config.storage           :as storage]
            [jarman.config.vars :refer [setj]])
  (:import
   (jiconfont.icons.google_material_design_icons GoogleMaterialDesignIcons)))

(def state (atom {:lang            nil
                  :license-file    nil
                  :current-license nil
                  :tmp-view-src    nil}))

(defn row-btn
  [& {:keys [func icon title]
      :or {func (fn [e])
           icon (gs/icon GoogleMaterialDesignIcons/SAVE)
           title (gtool/get-lang-btns :save)}}]
  (gcomp/button-basic
   title
   :tgap 2
   :bgap 2
   :underline-size 0
   :args [:icon icon]
   :onClick func))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; Language selection
;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- get-language-map [] {:en "ENG" :uk "UK" :pl "PL"})

(defn- get-language-key [lang-s] (first (first (filter #(= (val %) lang-s) (get-language-map)))))

(defn- get-language-list
  ([] (vals (get-language-map)))
  ([selected-k]
   (let [selected (get (get-language-map) selected-k)
         filtered (filter #(not (= % selected)) (vals (get-language-map)))]
     (concat [selected] filtered))))

(defn- display-btn-save [root]
  (row-btn :func (fn [e]
                   (if-not (nil? (:lang @state))
                     (do
                       (setj jarman.config.conf-language/language-selected (:lang @state))
                       (i/restart-alert)))
                   (c/config! root :items (gtool/join-mig-items (butlast (u/children root))))
                   (.repaint (c/to-root root)))))


(defn- language-selection-panel []
  (let [panel (gmg/migrid
               :> "[fill]10px[200, fill]10px[grow,fill]10px[fill]"
               {:gap [5 10 30 30] :args [:border (b/line-border :bottom 1 :color face/c-icon)]} [])
        selected-lang-fn #(deref jarman.config.conf-language/language-selected)]
    (c/config!
     panel
     :items (gtool/join-mig-items
             (c/label :text (str (gtool/get-lang-header :choose-language) ":"))
             (c/combobox :model (get-language-list (selected-lang-fn))
                         :listen [:item-state-changed (fn [e]
                                                        (if (= (count (u/children panel)) 3)
                                                          (.add panel (display-btn-save panel)))
                                                        (let [selected   (c/config e :selected-item)
                                                              selected-k (get-language-key selected)]
                                                          (swap! state #(assoc % :lang selected-k)))
                                                        (.revalidate (c/to-root e))
                                                        (.repaint (c/to-root e)))])
             (c/label)))
    panel))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; Licenses
;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- load-license []
  (.start (Thread. (fn [] (swap! state (fn [s] (update s :current-license (constantly (-> (session/load-license) (session/decrypt-license))))))))))
(defn- reset-license []
  (.start (Thread. (fn [] (swap! state (fn [s] (update s :current-license (constantly nil))))))))


(defn- display-btn-upload [root]
  (row-btn
   :title (gtool/get-lang-btns :install)
   :icon (gs/icon GoogleMaterialDesignIcons/MOVE_TO_INBOX)
   :func (fn [e]
           (let [default-path (str jarman.config.environment/jarman-home "/licenses")
                 license-file-path (:license-file @state)]
             (if (= default-path license-file-path)
               (i/warning (gtool/get-lang-header :file-no-choose)
                          (gtool/get-lang-alerts :choose-file-and-try-again)
                          :time 4)
               (try
                 (if-not (empty? license-file-path)
                   (do
                     ;; (swap! state #(assoc % :license-file nil))
                     (session/gui-slurp-and-set-license-file license-file-path)
                     (load-license)
                     
                     (i/success (gtool/get-lang-header :success)
                                (gtool/get-lang-alerts :new-license-instaled)
                                :time 3))
                   (do
                     (i/warning (gtool/get-lang-header :file-no-choose)
                                (gtool/get-lang-alerts :choose-file-and-try-again)
                                :time 4)))
                 (catch Exception e
                   (i/danger (apply gtool/get-lang (:translation (ex-data e))) (.getMessage e) :time 10)))))
                   ;;(c/config! root :items (gtool/join-mig-items (butlast (u/children root))))
                   (.repaint (c/to-root root)))))

(defn split-path [path]
  (let [linux (string/split path #"/")
        windo (string/split path #"\\")]
    (if (> (count linux) (count windo))
      linux windo)))

(defn- file-exp []
  (gmg/migrid
   :> "[fill]5px[220::, fill]" {:gap [0 20]}
   (let [default-path (str jarman.config.environment/jarman-home "/licenses")
         input (c/text :text (gtool/get-lang-basic :select-file) :border nil :editable? false :background face/c-compos-background)
         icon  (c/label :icon (gs/icon GoogleMaterialDesignIcons/FIND_IN_PAGE face/c-icon 25)
                        :listen [:mouse-entered gtool/hand-hover-on
                                 :mouse-clicked
                                 (fn [e] (let [new-path (chooser/choose-file
                                                        :dir (.getAbsolutePath (clojure.java.io/file default-path))
                                                        :success-fn  (fn [fc file] (.getAbsolutePath file)))]
                                          (swap! state #(assoc % :license-file (rift new-path default-path)))
                                          (c/config! input :text (rift (last (split-path new-path)) (gtool/get-lang-basic :select-file)))))])]
     [icon input])))


(defn- license-panel []
  (let [panel (gmg/migrid
               :> :fgf {:gap [5 30] ;; :args [:border (b/line-border :bottom 1 :color face/c-icon)]
                        } [])
        selected-lang-fn #(deref jarman.config.conf-language/language-selected)]
    (c/config!
     panel
     :items (gtool/join-mig-items
             (c/label :text (str (gtool/get-lang-header :upload-license) ": "))
             (file-exp)
             (display-btn-upload panel)))
    panel))

(defn- all-licenses-panel []
  (let [render-fn (fn []
                    ;;(println "\nCurrent licence: " (:current-license @state))
                    (if (nil? (get-in (deref state) [:current-license]))
                      (gtool/join-mig-items
                       (gmg/migrid :> :center {:args [:border (b/line-border :top 2 :color face/c-compos-background-darker)]}
                                   (c/label :text (gtool/get-lang-license :license-not-found))))
                      
                      (let [{:keys [tenant-id creation-date expiration-date]} (get-in (deref state) [:current-license])]
                        (gtool/join-mig-items
                         (gmg/migrid :> {:gap [0 10] :args [:border (b/compound-border
                                                         (b/empty-border :top 2 :bottom 2)
                                                         (b/line-border :bottom 2 :top 2 :color face/c-compos-background-darker))]}
                                     [(c/label :text (gtool/get-lang-basic :license-code))
                                      (c/label :text (gtool/get-lang-basic :date-start))
                                      (c/label :text (gtool/get-lang-basic :date-end))])
                         (gmg/migrid :> {:gap [0 10 10 10]:args [:border (b/empty-border :top 2 :bottom 2)]}
                                     [(c/label :text tenant-id)
                                      (c/label :text creation-date)
                                      (c/label :text expiration-date)])))))
        panel (gmg/migrid
               :v {:gap [5 5 30 30] :args [:border (b/line-border :bottom 1 :color face/c-icon)]}
               (render-fn))]
    (state/new-watcher state panel render-fn [:current-license] :watch-current-license)
    panel))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; view.clj or view from DB
;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn- viewclj-or-viewdb []
  (swap! state #(assoc % :tmp-view-src (rift (state/state :view-src) (deref jarman.logic.view-manager/view-src))))
  (let [panel (gmg/migrid :> {:gap [5 10 30 30] :args [:border (b/line-border :bottom 1 :color face/c-icon)]} [])
        
        render-fn (fn [] (c/label)
                    (let [options-vec      [(gtool/get-lang-btns :view.clj) (gtool/get-lang-btns :database)]
                          options-vec-keys [:view.clj :database]
                          selected-src     (.indexOf options-vec-keys (:tmp-view-src @state))
                          
                          radio-group (gcomp/jradiogroup
                                       options-vec
                                       (fn [radio box]
                                         (let [selected-vec (c/config box :user-data)
                                               selected-idx (.indexOf selected-vec true)
                                               selected-val (nth options-vec-keys selected-idx)]
                                           ;; (println selected-val)
                                           (swap! state #(assoc % :tmp-view-src selected-val))))
                                       :horizontal true
                                       :selected selected-src)
                          
                          save-btn (gmg/migrid
                                    :> :right
                                    (row-btn
                                     :func (fn [e]
                                             (state/set-state :view-src (:tmp-view-src @state))
                                             (setj jarman.logic.view-manager/view-src (:tmp-view-src @state))
                                             (i/success [:alerts :changes-saved] (format (gtool/get-lang-alerts :new-view-src) (:tmp-view-src @state)))
                                             (i/restart-alert))))]
                      [radio-group save-btn]))]
    (c/config! panel :items (gtool/join-mig-items (render-fn)))
    panel))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; Backup buttons
;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; enviroment.clj
;; storage.clj
;; gui_events.clj

(defn- timestamp []
  (.format (java.text.SimpleDateFormat. "dd-MM-yyyy_HH:mm:ss") (new java.util.Date)))

(defn- backup-panel-template
  [name backup-panel backup-list-fn backup-put-fn backup-clean-fn backup-del-fn]
  (let [render-panel (fn []
                       (c/config! backup-panel :items [[((:backup-panel-fn @state))]])
                       (.repaint (state/state :views-space)))]
    (gmg/migrid
     :v
     [(rift (doall (map (fn [name]
                          (gmg/migrid
                           :> :gf
                           [(c/label :text name
                                     :background face/c-compos-background
                                     :border (b/empty-border :thickness 3)
                                     :listen [:mouse-entered (fn [e] (c/config! e :background face/c-on-focus))
                                              :mouse-exited  (fn [e] (c/config! e :background face/c-compos-background))])
                            (row-btn :title (gtool/get-lang-btns :remove)
                                     :icon (gs/icon GoogleMaterialDesignIcons/DELETE)
                                     :func (fn [e]
                                             (backup-del-fn (last (split-path name)))
                                             (render-panel)))])) 
                        (sort (backup-list-fn)) ;; TODO: sort A-Z
                        ))
            (c/label :text (gtool/get-lang-infos :empty-backup-storage) :border (b/empty-border :left 3) :background face/c-compos-background))

      (gmg/migrid
       :> :center {:gap [20 0 0 0]}
       (gcomp/menu-bar {:buttons [[(gtool/get-lang-btns :create-backup) nil (fn [e] (backup-put-fn) (render-panel))]
                                  [(gtool/get-lang-btns :clean-backup-storage) nil (fn [e] (backup-clean-fn) (render-panel))]]}))])))

(defn- backup-vmd []
  (let [panel (gmg/migrid :v {:gap [5 30] :args [:border (b/line-border :bottom 1 :color face/c-icon)]} [])

        backup-panel (gmg/migrid :v {:gap [10 0]} [])

        backup-view (fn [] (backup-panel-template "View"
                                                   backup-panel
                                                   storage/backup-view-list
                                                   (fn [] (storage/backup-view-put (str "view_" (timestamp) ".clj") "TEST")) ;; TODO: Save correct file
                                                   storage/backup-view-clean
                                                   storage/backup-view-delete))
        backup-viewdb (fn [] (backup-panel-template "Viewdb"
                                                   backup-panel
                                                   storage/backup-viewdb-list
                                                   (fn [] (storage/backup-viewdb-put (str "viewdb_" (timestamp) ".clj") "TEST")) ;; TODO: Save correct file
                                                   storage/backup-viewdb-clean
                                                   storage/backup-viewdb-delete))
        backup-meta  (fn [] (backup-panel-template "Meta"
                                                   backup-panel
                                                   storage/backup-metadata-list
                                                   (fn [] (storage/backup-metadata-put (str "metadata_" (timestamp) ".clj") "TEST")) ;; TODO: Save correct file
                                                   storage/backup-metadata-clean
                                                   storage/backup-metadata-delete))
        backup-db    (fn [] (backup-panel-template "DB"
                                                   backup-panel
                                                   storage/backup-db-list
                                                   (fn [] (storage/backup-db-put (str "db_" (timestamp) ".clj") "TEST")) ;; TODO: Save correct file
                                                   storage/backup-db-clean
                                                   storage/backup-db-delete))

        options-vec-fns [backup-view backup-viewdb backup-meta backup-db]
        
        render-fn (fn [] 
                    (let [options-vec     [(gtool/get-lang-btns :backup-view)
                                           (str (gtool/get-lang-btns :backup-view) "db")
                                           (gtool/get-lang-btns :backup-metadata)
                                           (gtool/get-lang-btns :backup-database)]
                          
                          radio-group (gcomp/jradiogroup
                                       options-vec
                                       (fn [radio box]
                                         (let [selected-vec (c/config box :user-data)
                                               selected-idx (.indexOf selected-vec true)]
                                           (swap! state #(assoc % :backup-panel-fn (nth options-vec-fns selected-idx)))
                                           (c/config! backup-panel :items [[((nth options-vec-fns selected-idx))]])))
                                       :horizontal true)]
                      (c/config! backup-panel :items [[(backup-view)]])
                      [radio-group backup-panel]))]

    (swap! state #(assoc % :backup-panel-fn (first options-vec-fns)))
    (c/config! panel :items (gtool/join-mig-items (render-fn)))
    panel))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; Dispaly settings section
;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn config-panel-proxy []
  ;; (swap! state (fn [s] (update s :current-license (constantly (-> (session/load-license) (session/decrypt-license))))))
  (load-license)
  (let [panel (gmg/migrid
               :v {:gap [5 0]}
               (filter some?
                       [(gcomp/button-expand (gtool/get-lang-header :select-language) (language-selection-panel)
                                             :before-title #(c/label :icon (gs/icon GoogleMaterialDesignIcons/TRANSLATE)))

                        (session/when-permission
                         :admin-update
                         (gcomp/button-expand (gtool/get-lang-header :licenses)
                                              (gmg/migrid :v [(license-panel)
                                                              (all-licenses-panel)])
                                              :before-title #(c/label :icon (gs/icon GoogleMaterialDesignIcons/VERIFIED_USER))))

                        (gcomp/button-expand (gtool/get-lang-btns :view-src) (viewclj-or-viewdb)
                                             :before-title #(c/label :icon (gs/icon GoogleMaterialDesignIcons/CODE)))

                        (gcomp/button-expand (gtool/get-lang-btns :backups) (backup-vmd)
                                             :before-title #(c/label :icon (gs/icon GoogleMaterialDesignIcons/STORAGE)))]))]
    (gmg/migrid-resizer (state/state :views-space) panel :backup-vmd :gap [5 0])
    (gcomp/min-scrollbox panel)))

(defn config-panel []
  (config-panel-proxy))
