(ns jarman.gui.gui-tools
  (:use seesaw.core
        seesaw.border
        seesaw.dev
        seesaw.mig)
  (:require
   [jarman.faces :as face]
   [seesaw.core                      :as c]
   [seesaw.util                      :as u]
   [jarman.tools.swing               :as stool]
   [clojure.string                   :as string]
   [jarman.logic.state               :as state]
   [jarman.resource-lib.icon-library :as icon]
   [jarman.gui.gui-style             :as gs]
   [jarman.tools.lang                :refer :all]
   [jarman.tools.org                 :refer :all]
   [jarman.config.conf-language]
   ))

(import javax.swing.JLayeredPane)
(import java.awt.Color)
(import java.awt.MouseInfo)
(import java.awt.event.MouseListener)

(def jarman-focus-now (atom nil))

(defn set-focus
  [object] (reset! jarman-focus-now object))
(defn rm-focus
  [] (reset! jarman-focus-now nil))
(defn set-focus-if-nil
  [object] (if (nil? @jarman-focus-now) (reset! jarman-focus-now object)))
(defn switch-focus
  [& {:keys [obj time]
      :or   {obj nil
             time 0}}]
  (if-not (nil? obj) (set-focus obj))
  (if (> time 0)
    (timelife time (fn []
                     (if-not (nil? @jarman-focus-now)
                       (.requestFocus @jarman-focus-now)
                       (reset! jarman-focus-now nil))))
    (if-not (nil? @jarman-focus-now)
      (.requestFocus @jarman-focus-now)
      (reset! jarman-focus-now nil))))

;; ┌─────────────────────────┐
;; │                         │
;; │ Quick gui functions     │
;; │                         │
;; └─────────────────────────┘

(defn opacity-color [] (new Color 0 0 0 0))
(defn getWidth  [obj] (.width (.getSize obj)))
(defn getHeight [obj] (.height (.getSize obj)))
(defn getSize   [obj] (let [size (.getSize obj)] [(.width size) (.height size)]))
(defn getParent [obj] (.getParent (seesaw.core/to-widget obj)))
(defn getRoot   [obj] (to-root (seesaw.core/to-widget obj)))
(defn findByID [root id] (let [found (seesaw.core/to-widget (select (to-root root) [id]))] found))
(defn getChildren [parent] (let [children (seesaw.util/children (seesaw.core/to-widget parent))] children))
(defn firstToWidget [list] (seesaw.core/to-widget (first list)))
(defn get-parent-items [e] (u/children (.getParent (c/to-widget e))))

;; (def getFont
;;   (fn [& params] (-> {:size 12 :style :plain :name "Ubuntu Regular"}
;;                      ((fn [m] (let [size  (first (filter (fn [item] (if (number? item) item nil)) params))] (if (number? size) (merge m {:size size}) (conj m {})))))
;;                      ((fn [m] (let [name  (first (filter (fn [item] (if (string? item) item nil)) params))] (if (string? name) (merge m {:name name}) (conj m {})))))
;;                      ((fn [m] (let [style (first (filter (fn [item] (if (= item :bold) item nil)) params))] (if (= style :bold) (merge m {:name "Ubuntu bold"}) (conj m {})))))
;;                      ((fn [m] (let [style (first (filter (fn [item] (if (= item :italic) item nil)) params))] (if (= style :italic)
;;                                                                                                                 (if (= (get m :style) :bold) (merge m {:style #{:bold :italic}}) (merge m {:style :italic}))
;;                                                                                                                 (conj m {}))))))))


(defn getFont [& params]
  {:pre [(every? #(or (number? %) (#{:bold :italic :italic-bold} %) (string? %)) params)]}
  (let [default-font-style {:size 12 :style :plain :name "Ubuntu Regular"}
        to-bold          #(assoc %1 :name  "Ubuntu bold")
        to-italic        #(assoc %1 :style :italic)
        to-italic-bold   #(assoc %1 :style #{:bold :italic})
        change-name      #(assoc %1 :name %2)
        change-size      #(assoc %1 :size %2)]
    (reduce
     (fn [acc p]
       (cond-> acc
         (string?         p)   (change-name p)
         (number?         p)   (change-size p)
         (= :bold         p)   (to-bold)
         (= :italic       p)   (to-italic)
         (= :italic-bold  p)   (to-italic-bold)))
     default-font-style params)))

(defn get-mouse-pos
  "Description:
     Return mouse position on screen, x and y.
  Example:
     (get-mouse-pos) => [800.0 600.0]" []
  (let [mouse-pos (.getLocation (java.awt.MouseInfo/getPointerInfo))
        screen-x  (.getX mouse-pos)
        screen-y  (.getY mouse-pos)]
    [screen-x screen-y]))

(defn htmling
  "Description
     Build word wrap html
   Example:
     (htmling \"Some text\" :center)"
  [body & args] 
  (let [wrap-template-on  "width: 100%; overflow-wrap: break-word; "
        wrap-template-off "overflow: hidden; white-space: nowrap; text-overflow: ellipsis; "
        wrapping (if (in? args :no-wrap) wrap-template-off wrap-template-on)]
    (string/join [(str "<html><body style='" wrapping  "'>") 
                  (cond
                    (in? args :justify) (format "<p align= \"justify\">%s</p>" body)
                    (in? args :center)  (format "<p align= \"center\">%s</p>" body)
                    :else  body)
                  "</body><html>"])))

;; (macroexpand-1 `(textarea "ala am kota" :border (line-border :thickness 1 :color "#a23")))

(def join-mig-items
  "Description
     Join items and convert to mig's items vector
   Example
     (join-mig (list (label) (list (label) (label)))) => [[(label)] [(label)] [(label)]]"
  (fn [& args]
    (join-vec (map #(vector %) (flatten args)))))

(defn kc-to-map
  "Description:
    Get two vector and marg to map.
    First vector with keys.
    Second vector with components.
    First key will maped value from first component.
  Example:
    (kv-to-map [:a :b] [text1 text2]) => {:a 1 :b 2}"
  [k-vec v-vec]
  (into {} (doall
           (map
            (fn [k v] {k (c/text v)})
            k-vec v-vec))))

(defn repeat-listener-to-all
  "Descritpion:
    Go over all obj and children and insert same listeners.
  Example:
    (repeat-listener-to-all [panel label] [:mouse-clicked ...])" 
  [items-list listen-vec]
  (if (empty? items-list) (println "repeat-listener-to-all: Items list is empty")
      (if (empty? listen-vec) (println "repeat-listener-to-all: Listeners vector is empty")
        (doall
         (map
          #(if (sequential? %)
             (repeat-listener-to-all %)
             (do
               (try
                 (c/config! % :listen listen-vec)
                 (catch Exception e (println "" (str "Set Listener exception:\n" (.getMessage e)))))
               
               (let [children (seesaw.util/children %)]
                 (if-not (empty? children)
                   (repeat-listener-to-all children listen-vec)))))
          items-list)))))

(defn middle-bounds
  "Description:
      Return middle bounds with size.
   Example:
      [x y w h] => [100 200 250 400]
      (middle-bounds root 250 400) => [550 400 250 400]
   Needed:
      Function need getSize function for get frame width and height"
  ([obj width height] (let [size (getSize obj)
                            x (first size)
                            y (last size)]
                        [(- (/ x 2) (/ width 2)) (- (/ y 2) (/ height 2)) width height]))
  ([root-w root-h width height] (let [size [root-w root-h]
                                      x (first size)
                                      y (last size)]
                                  [(- (/ x 2) (/ width 2)) (- (/ y 2) (/ height 2)) width height])))

(def hand-hover-on  (fn [e] (config! e :cursor :hand)))
(def hand-hover-off (fn [e] (config! e :cursor :default)))

(defn str-cutter
  "Description:
      Cut message and add ...
   Example: 
      (str-cutter 'Some really long but not complicated message.')    => 'Some really long but not complicated me...'
      (str-cutter 'Some really long but not complicated message.' 10) => 'Some reall...'
   "
  ([txt] (cond
           (> (count txt) 40) (string/join  "" [(subs txt 0 40) "..."])
           :else txt))
  ([txt max-len] (cond
                   (> (count txt) max-len) (string/join  "" [(subs txt 0 max-len) "..."])
                   :else txt)))

(defn gapser [gap]
  (cond
    (= 1 (count gap)) (let [tblr (first gap)] [tblr tblr tblr tblr])
    (= 2 (count gap)) (let [tb (first gap) lr (last gap)] [tb tb lr lr])
    (= 4 (count gap)) gap
    :else [0 0 0 0]))

(defn get-user-data
  "Description:
      Get data in map from :user-data inside seesaw components. 
      Can use only one level of map: {:key value, :key value, ...}.
   Example:
      (label :userdata {:placeholder \"Dupa\"})
      (get-user-data event) => {:placeholder \"Dupa\"}
      (get-user-data event :placeholder) => Dupa
   "
  ([e] (config e :user-data))
  ([e key]
   (get (config e :user-data) key)))

(defn get-elements-in-layered-by-id
  "Description:
     Set same id inside elements and found them all.
  Return:
     List of components/objects => (object[xyz] object(xyz))
  Example:
     (get-elements-in-layered-by-id event_or_some_root 'id_in_string')
  "
  [e ids] (let [id (keyword ids)
                select-id (keyword (string/join ["#" ids]))
                root (to-root (seesaw.core/to-widget e))
                selected (select root [select-id])
                outlist (if selected (filter (fn [i] (identical? (config i :id) id)) (seesaw.util/children (.getParent selected))) nil)]
            (if outlist outlist nil)))

(def get-lang          (fn [& path] (apply jarman.config.conf-language/lang (concat [:ui]          path))))
(def get-lang-basic    (fn [& path] (apply jarman.config.conf-language/lang (concat [:ui :basic]   path))))
(def get-lang-header   (fn [& path] (apply jarman.config.conf-language/lang (concat [:ui :header]  path))))
(def get-lang-infos    (fn [& path] (apply jarman.config.conf-language/lang (concat [:ui :infos]   path))))
(def get-lang-btns     (fn [& path] (apply jarman.config.conf-language/lang (concat [:ui :buttons] path))))
(def get-lang-alerts   (fn [& path] (apply jarman.config.conf-language/lang (concat [:ui :alerts]  path))))
(def get-lang-tip      (fn [& path] (apply jarman.config.conf-language/lang (concat [:ui :tips]    path))))
(def get-lang-license  (fn [& path] (apply jarman.config.conf-language/lang (concat [:ui :license] path))))

(def get-plang         (fn [plugin & path] (apply jarman.config.conf-language/plang plugin (concat [:ui]          path))))
(def get-plang-basic   (fn [plugin & path] (apply jarman.config.conf-language/plang plugin (concat [:ui :basic]   path))))
(def get-plang-header  (fn [plugin & path] (apply jarman.config.conf-language/plang plugin (concat [:ui :header]  path))))
(def get-plang-infos   (fn [plugin & path] (apply jarman.config.conf-language/plang plugin (concat [:ui :infos]   path))))
(def get-plang-btns    (fn [plugin & path] (apply jarman.config.conf-language/plang plugin (concat [:ui :buttons] path))))
(def get-plang-alerts  (fn [plugin & path] (apply jarman.config.conf-language/plang plugin (concat [:ui :alerts]  path))))
(def get-plang-tip     (fn [plugin & path] (apply jarman.config.conf-language/plang plugin (concat [:ui :tips]    path))))
(def get-plang-license (fn [plugin & path] (apply jarman.config.conf-language/plang plugin (concat [:ui :license] path))))

(defn convert-key-to-title
  "Description:
      Set :key and get title Key. Fn removing symbols [ - _ . ]. First char will be upper.
   Example:
      (convert-key-to-title :my-title) => \"My title\"
      (convert-key-to-title :my_title) => \"My title\"
      (convert-key-to-title :my.title) => \"My title\""
  [key] (-> (string/replace (str key) #":" "") (string/replace  #"[-_.]" " ") (string/replace  #"^." #(.toUpperCase %1))))

(defn convert-txt-to-title
  "Description:
      Set some string and get like a title. Fn removing symbols [ - _ . ]. First char will be upper.
   Example:
      (convert-txt-to-title \"my title\") => \"My title\"
      (convert-txt-to-title \"my-title\") => \"My title\"
      (convert-txt-to-title \"my.title\") => \"My title\""
  [txt] (-> (string/replace (str txt) #":" "") (string/replace  #"[-_.]" " ") (string/replace  #"^." #(.toUpperCase %1))))

(defn convert-txt-to-UP
  "Description:
      Set some string and get with upper chars.
   Example:
      (convert-txt-to-UP \"my title\") => \"MY TITLE\"
      (convert-txt-to-UP \"my-title\") => \"MY-TITLE\""
  [txt] (-> (string/replace (str txt) #":" "") (string/replace  #"." #(.toUpperCase %1))))

(defn convert-mappath-to-key
  "Description:
      Set some :key coll and get one marge key.
   Example:
      (convert-mappath-to-key [:path :to :my :conf]) => :path-to-my-conf"
  [path] (keyword (string/join "-" (vec (map #(name %) path)))))

(defn gud
  "Description:
    Return value from :user-data map in component.
  Example:
    (get-user-data (c/label ...) [:some-value]"
  [compo path]
  (let [path (if (keyword? path) [path] path)
        data (c/config compo :user-data)]
    (get-in data path)))

;; ############# COMPONENTS TODO: need move to gui_components.clj


(def template-resize
  "Discription:
      Function for main JLayeredPane for resize it to app window.
   Example:
      (template-resize my-app)
   "
  (fn [app-template]
    (let [v-size (.getSize    (to-root app-template))
          vw     (.getWidth   v-size)
          vh     (.getHeight  v-size)]
      (config! app-template  :bounds [0 0 vw vh]))))

(def slider-ico-btn
  "Description:
      Slide buttons used in JLayeredPanel. 
      Normal state is small square with icon 
      but on hover it will be wide and text will be inserted.
   Example:
      (function icon size header map-with-other-params)
      (slider-ico-btn (stool/image-scale icon/user-64x64-2-png 50) 0 50 'Klienci' :onclick (fn [e] (alert 'Clicked')))
   "
  (fn [ico order size txt
       & {:keys [onClick
                 top-offset]
          :or {onClick (fn [e])
               top-offset 0}}]
    (let [bg-color          face/c-slider-bg
          c-foreground      face/c-slider-fg
          bg-color-hover    face/c-slider-bg-on-focus
          c-underline       face/c-slider-underline
          c-underline-focus face/c-slider-underline-on-focus
          y (if (> (* size order) 0) (+ top-offset (- (+ (* 2 order) (* size order)) (* 2 (* 1 order)))) (+ top-offset (* size order)))
          icon (label :halign :center
                      :icon ico
                      :size [size :by size])
          title (label :text txt
                       :foreground c-foreground)
          mig (mig-panel
               :constraints ["" (str "0px[" size "]15px[grow, fill]0px") (str "0px[" size "]0px")]
               :bounds [0 y size size]
               :background bg-color
               :focusable? true
               :border  (line-border :bottom 2 :color c-underline)
               :items (join-mig-items icon))]
      (let [onEnter (fn [e] (config! e
                                     :cursor :hand
                                     :background bg-color-hover
                                     :border     (line-border :bottom 2 :color c-underline-focus)
                                     :bounds [0 y (+ (.getWidth (select (.getParent mig) [:#expand-menu-space])) size) size])
                      (.add mig title)
                      (.revalidate mig))
            onExit (fn [e] (config! e
                                    :bounds [0 y size size]
                                    :border     (line-border :bottom 2 :color c-underline)
                                    :background bg-color)
                     (.remove mig 1)
                     (.revalidate mig))]
        (config! mig :listen [:mouse-entered (fn [e] (.requestFocus (c/to-widget e)))
                              :mouse-exited  (fn [e] (.requestFocus (c/to-frame e)))
                              :focus-gained  (fn [e] (onEnter e))
                              :focus-lost    (fn [e] (onExit e))
                              :mouse-clicked (fn [e] ;; (println "onClick")
                                               (onClick e))
                              :key-pressed   (fn [e] (if (= (.getKeyCode e) java.awt.event.KeyEvent/VK_ENTER) (onClick e)))]))
      mig)))



(def table-editor--component--bar-btn
  "Description:
     Interactive button for table editor."
  (fn [id title ico ico-hover onclick]
    (let [bg        "#ddd"
          bg-hover  "#d9ecff"
          c-border  "#bbb"]
      (label :text title
             :id id
             :font (gs/getFont :bold)
             :icon (stool/image-scale ico 26)
             :halign :center
             :size [150 :by 30]
             :background bg
             :border (line-border :thickness 1 :color c-border)
             :listen [:mouse-entered (fn [e] (config! e :background bg-hover :icon (stool/image-scale ico-hover 26) :cursor :hand))
                      :mouse-exited  (fn [e] (config! e :background bg :icon (stool/image-scale ico 26) :cursor :default))
                      :mouse-clicked (fn [e] (onclick e))]))))


(defn colorizator-text-component "Colorize component, by hexadecemal value" [target]
  (let [lower-str (string/lower-case (string/trim (text target)))
        smb-arr "0123456789abcdef"
        [hash & color] lower-str
        c (count color)]
    (if (and (= hash \#)
             (or (= c 3) (= c 6))
             ;; if 'color' has only smb-arr charset
             (reduce #(and %1 (some (fn [_s] (= %2 _s)) smb-arr)) true color))
      (config! target
               :background lower-str
               :foreground (let [clr (apply str color)
                                 hex (read-string (if (= (count clr) 3)
                                                    (str "0x" clr)
                                                    (apply str "0x" (map first (partition 2 clr)))))]
                             (if (< hex 1365) "#FFF" "#000")))
      (config! target :background "#FFF" :foreground "#000"))))



(defn my-border
  "Description:
      Create border for gui component, empty-border like margin in css.
   Example:
     (my-border [\"#222\" 4] [10 20 30 30])
      ;; => #object[javax.swing.border.CompoundBorder....
            (#function[seesaw.border/compound-border]
            (#function[seesaw.border/line-border] :bottom 4 :color \"#222\")
            (#function[seesaw.border/empty-border]
            :top 10
            :right 20
            :bottom 30
            :left 40))
      (my-border [10 20])
      ;; => #object[javax.swing.border.CompoundBorder....
            (#function[seesaw.border/compound-border]
            (#function[seesaw.border/empty-border]
            :top 10
            :right 20
            :bottom 0
            :left 0))"
  [& args]
  (let [l (into
           (map
            (fn [x]
              (if (some string? x)
                (let [[x1 x2] x]
                  (into
                   (if (int? x1)
                     (list :bottom x1 :color x2)
                     (list :bottom x2 :color x1))
                   (list line-border)))
                (let [a x
                      am (count a)
                      prm [:top :right :bottom :left]]
                  (into
                   (mapcat list prm
                           (condp = am
                             1 (repeat 4 a)
                             4 a
                             (into a (repeat (- 4 (count a)) 0))))
                   (list empty-border))))) args)
           (list compound-border))]
    (eval l)))





