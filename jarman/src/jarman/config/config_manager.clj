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



;; ┌────────────────────┐
;; │                    │
;; │ Themes map skipper │
;; │                    │
;; └────────────────────┘

(def theme-map (fn [] (get-in @configuration [:themes :value :current-theme :value])))

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
        (let [path (vec (concat (interpose :value (vec pre-path)) [:value]
                            (interpose :value (vec steps)) [:value]))
              value (get-in mapa path)]
          (cond (= value nil) default
                :else value))
        :else (let [path (concat (vec pre-path) (vec steps))
                    value (get-in mapa path)]
                (cond (= value nil) default
                      :else value))))))


;; (def using-lang (get-in @configuration [:init.edn :value :lang :value]))
;; (def all-langs  (fn [] (let [langs (vec (map (fn [lng] (first lng)) @language))] (filter #(not (= % using-lang)) langs))))
(def get-color  (create-value-getter (theme-map) true "#000" :color))
(def get-frame  (create-value-getter (theme-map) true 1000 :frame))
(def get-font   (create-value-getter (theme-map) true "Ubuntu" :font))
(def get-lang   (create-value-getter @language false "Unknown" []))
(def get-lang-btns (create-value-getter @language false "Unknown" :ui :buttons))
(def get-lang-alerts (create-value-getter @language false "Unknown" :ui :alerts))

;; (get-color :jarman :bar)
;; (get-frame :width)
;; (get-font :bold)
;; (get-lang-btns :remove)
;; (all-langs)
