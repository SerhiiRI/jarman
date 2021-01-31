(ns jarman.config.init
  (:gen-class)
  (:use clojure.reflect
        seesaw.core)
  (:require [clojure.string :as string]
            [jarman.tools.dev-tools]
            [clojure.java.io :as io]))







;;; `TODO` 
;;; 
;;; swap  all file 
;;; validate  all files
{:name "Language"
	 :doc [:cfg :init.edn :value :lang :doc]
	 :type :param
	 :component :text
	 :display? :edit
         :value "pl"}
;; (defn old-new [m]
;;   (if (or
;;        (string? value)
;;        (keyword? value)
;;        (number? value)
;;        (vector? value))
;;     )
;;   (if (= :header key))
;;   (if (= :description key))
;;   (if (= :parameters key)))


(let [a 1
]
  (+ a c))


(reduce (fn [aa [kk vv]]
          (into aa {kk 
                    (reduce (fn [a [k v]]
                              (let [x (cond (string? v) {:type :param :display :edit :component :textcolor :value v}
                                            (keyword? v) {:type :param :display :edit :component :text :value v}
                                            (number? v) {:type :param :display :edit :component :number :value v}
                                            :else v)]
                                (into a {k x})))
                            {} (seq vv))}))
        {}
        (seq {:background {
                           :main "#eee",
                           :button "#eee",
                           :button_hover "#96c1ea",
                           :button_config "#fff",
                           :button_selected "#ccc",
                           :button_main "#fff"
                           }
              :main-button {
                            :background "#fff",
                            :clicked "#96c1ea"
                            }
              :foreground {
                           :main "#000",
                           :button "#000",
                           :button_hover "#29295e",
                           :button_selected "#000",
                           :doc-font-color "#999",
                           :txt-font-color "#000"
                           }}))
{}

{
 :color {:name "Konfiguracja kolorów skórki",
         :doc "blok konfiguracji kolorów zawiera grupowane presety\n kolorów dla tylko podanej skórki. Ka¿dy parameter tego zestawu\n konfigurowany przez pola, które zmieniaj± kolorki przez podania do nich kolorków\n w systemie heksadycemalnym, 6-ciu, lub 3-szy kolorowym. Kolor jest kolorem\n tylko wtedy jak z przodu jest u¿ywany tak zwany symbol\n HEX-kolorów `#`(hash). Parametry: \"#FFF\" \"#fff\" \"#fFf\" \"#123ABC\" \"#123abc\" \"#3AF\""
         :type :block
         :display :edit
         :value {:jarman {:doc "Podstawowe kolory Jarman IDE",
                          :type :block
                          :display :edit
                          :value {:bar {:type :param, :display :edit, :component :textcolor, :value "#292929"}
                                  :jarman {:type :param, :display :edit, :component :textcolor, :value "#726ee5"}
                                  :light {:type :param, :display :edit, :component :textcolor, :value "#96c1ea"}
                                  :dark {:type :param, :display :edit, :component :textcolor, :value "#29295e"}}}
                 :background {:type :block
                              :display :edit
                              :value {:main {:type :param, :display :edit, :component :textcolor, :value "#eee"}
                                      :button {:type :param, :display :edit, :component :textcolor, :value "#eee"}
                                      :button_hover {:type :param, :display :edit, :component :textcolor, :value "#96c1ea"}
                                      :button_config {:type :param, :display :edit, :component :textcolor, :value "#fff"}
                                      :button_selected {:type :param, :display :edit, :component :textcolor, :value "#ccc"}
                                      :button_main {:type :param, :display :edit, :component :textcolor, :value "#fff"}}}

 :main-button {:background {:type :param, :display :edit, :component :textcolor, :value "#fff"}
               :clicked {:type :param, :display :edit, :component :textcolor, :value "#96c1ea"}}

 :foreground {:main {:type :param, :display :edit, :component :textcolor, :value "#000"}
              :button {:type :param, :display :edit, :component :textcolor, :value "#000"}
              :button_hover {:type :param, :display :edit, :component :textcolor, :value "#29295e"}
              :button_selected {:type :param, :display :edit, :component :textcolor, :value "#000"}
              :doc-font-color {:type :param, :display :edit, :component :textcolor, :value "#999"}
              :txt-font-color {:type :param, :display :edit, :component :textcolor, :value "#000"}}
                 :border "#ccc"}}
 :doc "Konfiguracja g³ównego okna HRTime",
 :font {
        :bold "Ubuntu Bold",
        :regular "Ubuntu",
        :medium "Ubuntu Medium",
        :size-small 12,
        :style :plain,
        :size-medium 16,
        :size-normal 14,
        :size-large 18,
        :light "Ubuntu Light"
        }
 :doc "Konfiguracja g³ównego okna HRTime applikacji",
 :frame {
         :width 1200,
         :heigh 700
         }
 :ui {
      :doc "Specjalny przycisk, ala Exportuj i t.d.",
      :special-button {
                       :background "#3b8276",
                       :background-hover "#77e0cf",
                       :foreground "#fff",
                       :foreground-hover "#000",
                       :horizontal-align :center,
                       :cursor :hand,
                       :font-style :bold
                       }
      }
 }



