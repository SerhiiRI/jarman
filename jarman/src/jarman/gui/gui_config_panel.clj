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
            [jarman.config.vars :refer [setj]])
  (:import
   (jiconfont.icons.google_material_design_icons GoogleMaterialDesignIcons)))

(def state (atom nil))

(defn- get-language-map [] {:en "ENG" :uk "UK" :pl "PL"})

(defn- get-language-key [lang-s] (first (first (filter #(= (val %) lang-s) (get-language-map)))))

(defn- get-language-list
  ([] (vals (get-language-map)))
  ([selected-k]
   (let [selected (get (get-language-map) selected-k)
         filtered (filter #(not (= % selected)) (vals (get-language-map)))]
     (concat [selected] filtered))))

(defn- display-btn-save [root]
  (gcomp/button-basic
   (gtool/get-lang-btns :save)
   :tgap 2
   :bgap 2
   :underline-size 0
   :args [:icon (gs/icon GoogleMaterialDesignIcons/SAVE)]
   :onClick (fn [e]
              (if-not (nil? (:lang @state))
                (do
                  (setj jarman.config.conf-language/language-selected (:lang @state))
                  (i/danger (gtool/get-lang-header :need-reload)
                             [{:title (gtool/get-lang-btns :reload-app)
                               :func (fn [api] (i/restart))}]
                             :time 0)))
              (c/config! root :items (gtool/join-mig-items (butlast (u/children root))))
              (.repaint (c/to-root root)))))

(defn- language-selection-panel []
  (let [panel (gmg/migrid
               :> "[fill]10px[200, fill]10px[grow,fill]10px[fill]"
               {:gap [10 30] :args [:border (b/line-border :bottom 1 :color face/c-icon)]} [])
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

(defn- display-btn-upload [root]
  (gcomp/button-basic
   (gtool/get-lang-btns :install)
   :tgap 2
   :bgap 2
   :underline-size 0
   :args [:icon (gs/icon GoogleMaterialDesignIcons/MOVE_TO_INBOX)]
   :onClick (fn [e]
              (let [license-file-path (:license-file @state)]
                (try
                 (if-not (empty? license-file-path)
                   (do
                     ;; (swap! state #(assoc % :license-file nil))
                     (session/gui-slurp-and-set-license-file license-file-path)
                     
                     (i/success (gtool/get-lang-header :success)
                                (gtool/get-lang-alerts :new-license-instaled)
                                :time 3))
                   (do
                     (i/warning (gtool/get-lang-header :file-no-choose)
                                (gtool/get-lang-alerts :choose-file-and-try-again)
                                :time 4)))
                 (catch Exception e
                   (i/danger (apply gtool/get-lang (:translation (ex-data e))) (.getMessage e) :time 10))))
              ;;(c/config! root :items (gtool/join-mig-items (butlast (u/children root))))
              (.repaint (c/to-root root)))))

(defn- file-exp []
  (gmg/migrid
   :> "[fill]5px[220::, fill]" {:gap [0 20]}
   (let [default-path (str jarman.config.environment/jarman-home "/licenses")
         input (c/text :text default-path :border nil)
         icon  (c/label :icon (gs/icon GoogleMaterialDesignIcons/FIND_IN_PAGE face/c-icon 25)
                        :listen [:mouse-entered gtool/hand-hover-on
                                 :mouse-clicked
                                 (fn [e] (let [new-path (chooser/choose-file
                                                        :dir (.getAbsolutePath (clojure.java.io/file default-path))
                                                        :success-fn  (fn [fc file] (.getAbsolutePath file)))]
                                          (swap! state #(assoc % :license-file (rift new-path default-path)))
                                          (c/config! input :text (rift new-path default-path))))])]
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

;;; TODO: 
;;; Aleks, siemano kolano, widzisz te dwie funkcje 
;;; 
(defn- load-license [f]
  (.start (Thread. (fn [] (swap! state (fn [s] (update s :current-license (-> (session/load-license) (session/decrypt-license))))) (f)))))
(defn- reset-license [f]
  (.start (Thread. (fn [] (swap! state (fn [s] (update s :current-license (constantly nil)))) (f)))))


(defn- all-licenses-panel []
  (gmg/migrid
   :v {:gap [5] :args [:border (b/line-border :bottom 1 :color face/c-icon)]}
   (if (nil? (get-in (deref state) [:current-license]))
     [(gmg/migrid :> {:args [:border (b/line-border :top 2 :color face/c-compos-background-darker)]}
                  [(c/label :text "Your product are not registred, please select license file")])]
     (let [{:keys [tenant-id creation-date expiration-date]} (get-in (deref state) [:current-license])]
       [(gmg/migrid :> {:args [:border (b/line-border :bottom 2 :top 2 :color face/c-compos-background-darker)]}
                    [(c/label :text "License code") (c/label :text "Start") (c/label :text "End")])
        (gmg/migrid :>
                    [(c/label :text tenant-id)
                     (c/label :text creation-date)
                     (c/label :text expiration-date)])]))))

(defn config-panel-proxy []
  ;; fixme:aleks - tu musi być asynchroniczny callback
  ;; zgóry są funkcje 'load-license', 'reset-license', które faktycznie bez zatrzymania ustawią dane.
  ;; bo jak użytkownik będzie podminieał licencje musisz zmienic wewnętrzny model.
  (swap! state (fn [s] (update s :current-license (constantly (-> (session/load-license) (session/decrypt-license))))))
  (gmg/migrid
   :v {:gap [5 0]}
   (filter some?
    [(gcomp/button-expand (gtool/get-lang-header :select-language) (language-selection-panel)
                          :before-title #(c/label :icon (gs/icon GoogleMaterialDesignIcons/TRANSLATE)))
     
     (session/when-permission
      :admin-update
      (gcomp/button-expand (gtool/get-lang-header :licenses)
                           (gmg/migrid :v [(license-panel)
                                           (all-licenses-panel)])
                           :before-title #(c/label :icon (gs/icon GoogleMaterialDesignIcons/VERIFIED_USER))))])))

(defn config-panel []
  (config-panel-proxy))
