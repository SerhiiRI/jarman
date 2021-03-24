;;           (__)    /                             \         
;;           (oo)   |   No i co sie paczysz?        |
;;    /-------\/    | Krowy w ASCII żeś nie widzał? |
;;   / |     || '---|                    Mooo?      |
;;  *  ||----||   0  \_____________________________/ o 0   o 
;;     ^^    ^^  .|o.                                 \|00/.
;;============================================================
;; - ale suchę XD

(ns jarman.config.config-manager
  (:use jarman.config.init)
  (:require [clojure.string :as string]))


;; ##################################
;; #                                #
;; #   Example configuration map    #
;; #                                #
;; ##################################

(def theme-map {:info  {:name "Jarman Light"
                        :autor "Trashpanda-Team"
                        :date "02-02-2021"
                        :type :file-info}
                :color {:name "Konfiguracja kolorów skórki"
                        :doc "blok konfiguracji kolorów zawiera grupowane presety\n kolorów dla tylko podanej skórki. Każdy parameter tego zestawu\n konfigurowany przez pola, które zmieniają kolorki przez podania do nich kolorków\n w systemie heksadycemalnym, 6-ciu, lub 3-szy kolorowym. Kolor jest kolorem\n tylko wtedy jak z przodu jest używany tak zwany symbol\n HEX-kolorów `#`(hash). Parametry: \"#FFF\" \"#fff\" \"#fFf\" \"#123ABC\" \"#123abc\" \"#3AF\""
                        :type :block
                        :display :edit
                        :value {:jarman {:doc "Podstawowe kolory Jarman IDE"
                                         :type :block
                                         :display :edit
                                         :value {:bar {:type :param :display :edit :component :textcolor :value "#292929"}
                                                 :jarman {:type :param :display :edit :component :textcolor :value "#726ee5"}
                                                 :light {:type :param :display :edit :component :textcolor :value "#96c1ea"}
                                                 :dark {:type :param :display :edit :component :textcolor :value "#29295e"}}}
                                :background {:type :block
                                             :display :edit
                                             :value {:main {:type :param :display :edit :component :textcolor :value "#eee"}
                                                     :dark-header {:type :param :display :edit :component :textcolor :value "#494949"}
                                                     :combobox {:type :param :display :edit :component :textcolor :value "#fff"}
                                                     :input {:type :param :display :edit :component :textcolor :value "#fff"}
                                                     :button {:type :param :display :edit :component :textcolor :value "#eee"}
                                                     :button_hover {:type :param :display :edit :component :textcolor :value "#96c1ea"}
                                                     :button_config {:type :param :display :edit :component :textcolor :value "#fff"}
                                                     :button_selected {:type :param :display :edit :component :textcolor :value "#ccc"}
                                                     :button_main {:type :param :display :edit :component :textcolor :value "#fff"}
                                                     :header {:type :param :display :edit :component :textcolor :value "#ccc"}}}
                                :group-buttons {:type :block
                                                :display :edit
                                                :value {:background {:type :param :display :edit :component :textcolor :value "#fff"}
                                                        :background-hover {:type :param :display :edit :component :textcolor :value "#91c8ff"}
                                                        :clicked {:type :param :display :edit :component :textcolor :value "#d9ecff"}}}
                                :main-button {:type :block
                                              :display :edit
                                              :value {:background {:type :param :display :edit :component :textcolor :value "#fff"}
                                                      :clicked {:type :param :display :edit :component :textcolor :value "#96c1ea"}}}

                                :foreground {:type :block
                                             :display :edit
                                             :value {:main {:type :param :display :edit :component :textcolor :value "#000"}
                                                     :dark-header {:type :param :display :edit :component :textcolor :value "#fff"}
                                                     :button {:type :param :display :edit :component :textcolor :value "#000"}
                                                     :button_hover {:type :param :display :edit :component :textcolor :value "#29295e"}
                                                     :button_selected {:type :param :display :edit :component :textcolor :value "#000"}
                                                     :doc-font-color {:type :param :display :edit :component :textcolor :value "#999"}
                                                     :txt-font-color {:type :param :display :edit :component :textcolor :value "#000"}}}
                                
                                :border {:type :block
                                             :display :edit
                                             :value {:white {:type :param :display :edit :component :textcolor :value "#fff"}
                                                     :gray {:type :param :display :edit :component :textcolor :value "#ccc"}
                                                     :dark-gray {:type :param :display :edit :component :textcolor :value "#666"}}}

                                :decorate {:type :block
                                           :display :edit
                                           :value {:underline {:type :param :display :edit :component :textcolor :value "#000"}
                                                   :gray-underline {:type :param :display :edit :component :textcolor :value "#ccc"}}
                                           }}}
                :font {:doc "Konfiguracja głównego okna HRTime"
                       :type :block
                       :display :edit
                       :value {:bold {:type :param :display :edit :component :text :value "Ubuntu Bold"}
                               :regular {:type :param :display :edit :component :text :value "Ubuntu"}
                               :medium {:type :param :display :edit :component :text :value "Ubuntu Medium"}
                               :size-small {:type :param :display :edit :component :number :value 12}
                               :style {:type :param :display :edit :component :text :value :plain}
                               :size-medium {:type :param :display :edit :component :number :value 16}
                               :size-normal {:type :param :display :edit :component :number :value 14}
                               :size-large {:type :param :display :edit :component :number :value 18}
                               :light {:type :param :display :edit :component :text :value "Ubuntu Light"}}}
                :frame {:doc "Konfiguracja głównego okna HRTime applikacji"
                        :type :block
                        :display :edit
                        :value {:width {:type :param :display :edit :component :number :value 1200}
                                :heigh {:type :param :display :edit :component :number :value 700}}}
                :ui {:doc "Specjalny przycisk ala Exportuj i t.d."
                     :type :block
                     :dispaly :edit
                     :value {:special-button {:type :block
                                              :dispaly :edit
                                              :value {:background {:type :param :display :edit :component :textcolor :value "#3b8276"}
                                                      :background-hover {:type :param :display :edit :component :textcolor :value "#77e0cf"}
                                                      :foreground {:type :param :display :edit :component :textcolor :value "#fff"}
                                                      :foreground-hover {:type :param :display :edit :component :textcolor :value "#000"}
                                                      :horizontal-align {:type :param :display :edit :component :text :value :center}
                                                      :cursor {:type :param :display :edit :component :text :value :hand}
                                                      :font-style {:type :param :display :edit :component :text :value :bold}}}}}})


;;; @Serhii 
(defn for-you-with-love
  ([] (for-you-with-love "no bo wiesz tak szbyciej"))
  ([text]
   (let
    [l (count text)
     tl "┌" tr "┐"
     v  "│" h  "─"
     bl "└" br "┘"]
     (println
      (apply
       str
       (concat
        (concat [tl] (vec (repeat (+ l 2) h)) [tr \newline])
        (concat [v] (vec (repeat (+ l 2) \space)) [v \newline])
        (concat [v \space] (seq text) [\space v \newline])
        (concat [v] (vec (repeat (+ l 2) \space)) [v \newline])
        (concat [bl] (vec (repeat (+ l 2) h)) [br \newline])))))))

;;; transform this poor view
;; ##################################
;; #                                #
;; #  Easy getter form map creator  #
;; #                                #
;; ##################################
;;; to this dam'n beatyfull box
;; ┌──────────────────────────────┐
;; │                              │
;; │ Easy getter form map creator │
;; │                              │
;; └──────────────────────────────┘

(def create-value-getter
  "Description:
    Build getters functions from map with injecting :value or without :value
   Example:
     (def get-frame (create-value-getter theme-map true :frame)) => (get-frame :width) => 1200
     (def get-lang  (create-value-getter @*language* false :pl :ui)) => (get-lang :buttons :remove) => Usuń
   "
  (fn [mapa inject-key-value default & pre-path]
    (fn [& steps]
      (cond
        (= inject-key-value true)
        (let [path (concat (interpose :value (vec pre-path)) [:value]
                           (interpose :value (vec steps)) [:value])
              value (get-in mapa path)]
          (cond (= value nil) default
                :else value))
        :else (let [path (concat (vec pre-path) (vec steps))
                    value (get-in mapa path)]
                (cond (= value nil) default
                      :else value))))))


;; (def using-lang (get-in @configuration [:init.edn :value :lang :value]))
;; (def all-langs  (fn [] (let [langs (vec (map (fn [lng] (first lng)) @language))] (filter #(not (= % using-lang)) langs))))
(def get-color  (create-value-getter theme-map true "#000" :color))
(def get-frame  (create-value-getter theme-map true 1000 :frame))
(def get-font   (create-value-getter theme-map true "Ubuntu" :font))
(def get-lang   (create-value-getter @language false "Unknown" []))
(def get-lang-btns (create-value-getter @language false "Unknown" :ui :buttons))


;; (get-color :jarman :bar)
;; (get-frame :width)
;; (get-font :bold)
;; (get-lang-btns :remove)
;; (all-langs)
