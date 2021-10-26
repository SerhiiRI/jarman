(ns jarman.gui.gui-config-panel
  (:require [clojure.string    :as string]
            [seesaw.swingx     :as swingx]
            [seesaw.core       :as c]
            [seesaw.util       :as u]
            [seesaw.border     :as b]
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

(defn config-panel []
  (gmg/migrid
   :v {:gap [5 10]}
   [(gcomp/button-expand (gtool/get-lang-header :select-language) (language-selection-panel))]))
