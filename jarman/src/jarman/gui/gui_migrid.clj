(ns jarman.gui.gui-migrid
  (:use seesaw.mig)
  (:require [jarman.faces  :as face]
            [seesaw.core   :as c]
            [seesaw.border :as b]
            [jarman.tools.lang :refer :all]
            [jarman.gui.gui-tools      :as gtool]))

(defn hmig
  [& {:keys [items
             wrap
             lgap
             rgap
             tgap
             bgap
             gap
             vrules
             hrules
             debug
             args]
      :or {items [[(c/label)]]
           wrap 0
           lgap 0
           rgap 0
           tgap 0
           bgap 0
           gap []
           vrules "[grow, fill]"
           hrules "[grow, fill]"
           debug [0 "#f00"]
           args []}}]
  (let [[tgap bgap lgap rgap] (cond
                                (= 4 (count gap)) gap
                                (= 2 (count gap)) [(first gap)(first gap)(second gap)(second gap)]
                                (= 1 (count gap)) [(first gap)(first gap)(first gap)(first gap)]
                                :else [tgap bgap lgap rgap])]
    (apply mig-panel
           :constraints [(if (= 0 wrap) "" (str "wrap " wrap))
                         (str lgap (if (string? lgap) "" "px")
                              hrules
                              rgap (if (string? rgap) "" "px"))
                         (str tgap (if (string? tgap) "" "px")
                              vrules
                              bgap (if (string? bgap) "" "px"))]
           :items items
           :background face/c-layout-background
          :border (b/line-border :thickness (first debug) :color (second debug))
          args)))





(defn migrid
  "Description:
    Dynamic mig.
    For layout usually you should using :g :f :a etc.
    For component container you can using all templates or in string your own rules.
    For pointing direcion horizontal or vertical you should as first set
      :> it's mean horizontal
      :v it's mean vertical
  Example:
    (migrid :> :fgf :a [item1, item_big, item2])
      ;; => In horizontal set tree component in fill grow fill container
    More in demo below.
  "
  ([items]
   (migrid :v :a :f {} items))
  
  ([direction items]
   (migrid direction :a :f {} items))
  
  ([direction htemp items]
   (if (map? htemp)
     (migrid direction :a :f htemp items)
     (migrid direction htemp :f {} items)))
  
  ([direction htemp vtemp items]
   (if (map? vtemp)
     (migrid direction htemp :f vtemp items)
     (migrid direction htemp vtemp {} items)))
  
  ([direction htemp vtemp
    {:keys [args tgap bgap lgap rgap gap]
     :or {args []
          tgap 0
          bgap 0
          lgap 0
          rgap 0
          gap []}}
    items]
   (let [templates {:a      "[:100%:100%, fill]"      ;; Auto fill to prefer 100% and max 100%
                    :right  "[grow, right]0px[fill]"  ;; To right
                    :top    "[fill]"                  ;; To top
                    :bottom "[grow, bottom]0px[fill]" ;; To bottom
                    :center "[grow, center]"          ;; To center
                    :fgf    "[fill]0px[grow, fill]0px[fill]"       ;; [Fill][    GROW    ][Fill]
                    :gfg    "[grow, fill]0px[fill]0px[grow, fill]" ;; [  Grow  ][Fill][  Grow  ]
                    :gf     "[grow, fill]0px[fill]" ;; First grow second fill
                    :fg     "[fill]0px[grow, fill]" ;; First fill second grow
                    :f      "[fill]"                ;; Just fill, default left top
                    :g      "[::100%, grow, fill]"  ;; Grow and fill to max 100%
                    :jg     "[::100%, grow]"        ;; Just grow
                    }]
     (hmig
      :wrap (cond
              (or (= :h direction) (= htemp :right))  0
              (or (= :v direction) (= vtemp :bottom)) 1
              :else 0)
      :hrules (cond
                (string?  htemp) htemp
                (keyword? htemp) (get templates htemp)
                (int?     htemp) (str "[" htemp ":" htemp "%:100%, fill]")
                :else            (:a   templates))
      :vrules (cond
                (string?  vtemp) vtemp
                (keyword? vtemp) (get templates vtemp)
                (int?     vtemp) (str "[" vtemp ":" vtemp "%:100%, fill]")
                :else            (:a   templates))
      :items (gtool/join-mig-items
              (if (or (= vtemp :bottom) (= htemp :right)) (c/label) [])
              (if (sequential? items) items [items]))
      :gap gap
      :tgap tgap
      :bgap bgap
      :lgap lgap
      :rgap rgap
      :args args))))






;; Demo
(defn- lbl
  ([txt] (c/label :text txt))
  ([txt color] (c/label :text txt :background color)))
(defn- mbg [color] {:args [:border (b/line-border :thicness 1 :color color)]})

(defn- demo-btn
  []
  (migrid :> "[fill]5px[fill]" :f {:args [:background "#caa"] :gap [2 10]}
          [(c/label :text "Demo add BTN")
           (c/label :text "+")]))

(defn- render-demo
  []
  (migrid
   :> "[20%, fill]0[60%, fill]0[20%, fill]" :a {:gap [10]}
   [(migrid :v :f :g (mbg "#f00")
            (lbl "Left" "#bca"))

    (migrid :v :a :a (into {:gap [5 5 5 5]} (mbg "#00f"))
            (migrid :v :a :a
                    [(migrid :> :a :g (mbg "#f00")
                             [(migrid :v :f      :top    (mbg "#f0f") (lbl "Top"    "#bca"))
                              (migrid :v :center :jg     (mbg "#0f0") (lbl "Center" "#bca"))
                              (migrid :v :right  :bottom (mbg "#ac3") (lbl "Bottom" "#bca"))])
                     (migrid :> :center :center (lbl "Fill" "#bca"))
                     (migrid :> :center :center (demo-btn))]))

    (migrid :v :right :g (mbg "#f00")
            (lbl "Right" "#bca"))]))

(defn migrid-demo
  []
  (-> (doto (seesaw.core/frame
             :title "Migrid demo"
             :minimum-size [500 :by 500]
             :size [500 :by 500]
             :content (render-demo))
        (.setLocationRelativeTo nil) c/pack! c/show!)))
  
;;(migrid-demo)

