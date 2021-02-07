;;           (__)    /                             \         
;;           (oo)   |   No i co sie paczysz?        |
;;    /-------\/    | Krowy w ASCII żeś nie widzał? |
;;   / |     || '---|                    Mooo?      |
;;  *  ||----||   0  \_____________________________/ o 0   o 
;;     ^^    ^^  .|o.                                 \|00/.
;;============================================================

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
                                                     :button {:type :param :display :edit :component :textcolor :value "#eee"}
                                                     :button_hover {:type :param :display :edit :component :textcolor :value "#96c1ea"}
                                                     :button_config {:type :param :display :edit :component :textcolor :value "#fff"}
                                                     :button_selected {:type :param :display :edit :component :textcolor :value "#ccc"}
                                                     :button_main {:type :param :display :edit :component :textcolor :value "#fff"}
                                                     :header {:type :param :display :edit :component :textcolor :value "#ccc"}}}
                                :group-buttons {:type :block
                                                :display :edit
                                                :value {:background {:type :param :display :edit :component :textcolor :value "#eee"}
                                                        :background-hover {:type :param :display :edit :component :textcolor :value "#d9ecff"}
                                                        :clicked {:type :param :display :edit :component :textcolor :value "#c8dbee"}}}
                                :main-button {:type :block
                                              :display :edit
                                              :value {:background {:type :param :display :edit :component :textcolor :value "#fff"}
                                                      :clicked {:type :param :display :edit :component :textcolor :value "#96c1ea"}}}

                                :foreground {:type :block
                                             :display :edit
                                             :value {:main {:type :param :display :edit :component :textcolor :value "#000"}
                                                     :button {:type :param :display :edit :component :textcolor :value "#000"}
                                                     :button_hover {:type :param :display :edit :component :textcolor :value "#29295e"}
                                                     :button_selected {:type :param :display :edit :component :textcolor :value "#000"}
                                                     :doc-font-color {:type :param :display :edit :component :textcolor :value "#999"}
                                                     :txt-font-color {:type :param :display :edit :component :textcolor :value "#000"}}}
                                :border {:type :param :display :edit :component :textcolor :value "#ccc"}}}
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


;; ##################################
;; #                                #
;; #  Easy getter form map creator  #
;; #                                #
;; ##################################


(def colors-root-path [:color :value])
(def frame-root-path  [:frame :value])

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



(def using-lang :pl)
(def get-color (create-value-getter theme-map true "#000" :color))
(def get-frame (create-value-getter theme-map true 1000 :frame))
(def get-font  (create-value-getter theme-map true "Ubuntu" :font))
(def get-lang  (create-value-getter @*language* false "Unknown" using-lang :ui))
(def get-lang-btns  (create-value-getter @*language* false "Unknown" using-lang :ui :buttons))
;; (get-color :jarman :bar)
;; (get-frame :width)
;; (get-font :bold)
;; (get-lang :buttons :remove)

;; ####################################################################
;; #                                                                  #
;; #                           PROTOTYPES                             #
;; #                                                                  #
;; ####################################################################


;; ###################################
;; #                                 #
;; # Show map values as console tree #
;; #                                 #
;; ###################################

;; (def offset-char "  ")
;; (defn is-block? [m] (= (:type m) :block))
;; (defn is-file? [m] (= (:type m) :file))
;; (defn is-block-file? [m] (or (is-block? m) (is-file? m)))
;; (defn is-param? [m] (= (:type m) :param))

;; (defn printblock
;;   ([m p] (printblock 0 m p))
;;   ([offset m path] (print (apply str (apply str (repeat offset offset-char))) (format "'%s' - %s" (:name m) (str path))  "\n")))
;; (defn printparam
;;   ([m p] (printparam 0 m p))
;;   ([offset m path] (print (apply str (apply str (repeat offset offset-char))) (format "'%s' - %s" (:name m) (str path)) ":" (:value m) "\n")))

;; (defn build-part-of-map [level [header tail] path]
;;   (if (some? header)
;;     ;; for header {:file.edn {....}} we  
;;     (let 
;;      [k-header ((comp first first) header)
;;       header ((comp second first) header)
;;       path (conj path k-header)]
;;       (cond

;;         ;; if Map represent Block or File
;;         (is-block-file? header)
;;         (do (printblock level header path)
;;             (build-part-of-map (inc level) (map-destruct (:value header)) path))

;;         ;; fi Map represent Parameters
;;         (is-param? header) (printparam level header path))))
;;   ;; Do recursive for Tail destruction in the same level
;;   (if (some? tail) (build-part-of-map level  (map-destruct tail) path)))


;;   (defn recur-config [m]
;;    (build-part-of-map 0 (jarman.tools.dev-tools/map-destruct m) []))

  ;;   (recur-config theme-map)

;; (get-in theme-map [:config.edn :block1 :value :param1 :name])

;; (def colors-root-path [:color :value])
; (map (fn [x] (first x)) two)
; (map #(%) one)
; (get-in one [:value])
; (map #(println %) one)
