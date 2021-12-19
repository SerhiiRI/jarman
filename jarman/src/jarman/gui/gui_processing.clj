(ns jarman.gui.gui-processing
  (:require [seesaw.core   :as c]
            [jarman.tools.lang :refer  :all]
            [jarman.gui.gui-migrid     :as gmg]
            [jarman.logic.state        :as state]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; STRING ANIMATION
(def local-state (atom {:running false
                  :dispose false
                  :dialog nil}))

;; (comment
  
;;   (defn- repeater [rep char] (clojure.string/join "" (repeat rep char)))

;;   (defn- animation-render [pos max-chars]
;;     (let [prefix (repeater (- pos 1) "#")
;;           sufix  (repeater (- max-chars pos) "#")
;;           label  (str prefix (if (or (= pos 0) (= pos max-chars)) "" "-") sufix)]
;;       label))

;;   (defn- animation-render-dots [pos]
;;     (str "Processing" (repeater pos ".")))

;;   (defn- animation [dialog]
;;     (let [display (c/select dialog [:#display])
;;           delay 0.3
;;           max-chars 3]
;;       ((fn animate [i]
;;          (if (or (c/config dialog :visible?) (= i 0))
;;            (do
;;              (println (c/config dialog :visible?) (= i 0) i)
;;              (c/config! display :text (animation-render-dots i))
;;              (.repaint (c/to-root display))
;;              (Thread/sleep (* delay 1000))
;;              (animate (if (> i max-chars) 0 (inc i)))
;;              ))) 0)))
;;   )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; GIF

(defn- label-img [file-path w h]
  (let [img (clojure.java.io/file (str "resources/imgs/" file-path))
        gif (c/label :text (str "<html> <img width=\"" w "\" height=\"" h "\" src=\"file:" img "\">"))]
    gif))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; EXAMPLE

(defn- run-processing []
  (if (and (not (:running @local-state)) (c/config (c/to-frame (state/state :app)) :visible?))
    (let [size [150 150]
          display (gmg/migrid
                   :v :center
                   [(label-img "processing.gif" (first size) (second size))
                    (c/label :text "Processing...")]) ;;(c/label :text "Processing" :id :display)
          dialog (c/custom-dialog
                  :title "Processing..."
                  :parent (c/to-frame (state/state :app))
                  :modal? true
                  :resizable? false
                  :content (gmg/migrid
                            :v (format "[%s, fill]" (first size)) (format "[%s, fill]" (second size))
                            (gmg/migrid
                             :v :center :center
                             display)))]
      (.setUndecorated dialog true)
      (if-not (:running @local-state)
        (do
          (timelife 0 #(doto dialog c/pack! c/show!))
          ;; (animation dialog)
          (swap! local-state (fn [s] (-> (assoc s :running true)
                                   (assoc :dialog dialog)))))))))

(defn- end-processing []
  (if (:running @local-state)
    (swap! local-state #(assoc % :dispose true))))

(defn start-watch []
  (add-watch local-state :processing-state
                   (fn [id-key st old-m new-m]
                     (if (and (:dispose @local-state) (:dialog @local-state))
                       (do
                         (.dispose (:dialog @local-state))
                         (timelife 0.1 (fn []
                                         (swap! local-state (fn [s] (assoc s :running false)))
                                         (timelife 0.1 (fn []
                                                         (swap! local-state (fn [s] (assoc s :dispose false)))
                                                         (timelife 0.1 (fn []
                                                                         (swap! local-state (fn [s] (assoc s :dialog nil))))))))))))))


(start-watch)

(defn async-processing [todo-fn]  
  (run-processing)
  (timelife 0 (fn []
                (todo-fn)
                (end-processing))))

;; (defn async-processing [todo-fn]
;;   (println "working"))

(comment
  (run-processing)
  (end-processing)
  )
