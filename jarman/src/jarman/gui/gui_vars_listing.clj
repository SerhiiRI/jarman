(ns jarman.gui.gui-vars-listing
  (:require
   ;; swing tools
   [seesaw.core   :as c]
   [seesaw.border  :as b]
   [seesaw.table  :as table]
   ;; clojure lib
   [clojure.string :as string]
   ;; local functionality
   [jarman.faces :as face]
   [jarman.gui.gui-components :as gcomp]
   [jarman.gui.gui-tools      :as gtool]
   [jarman.gui.gui-style      :as gs]
   [jarman.gui.gui-migrid     :as gmg]
   [jarman.config.vars :as vars]
   [jarman.interaction :as i])
  (:import (java.io IOException FileNotFoundException)
           (jiconfont.icons.google_material_design_icons GoogleMaterialDesignIcons)))

;;;;;;;;;;;;;;;;;;;;;;;;
;;; HELPER FUNCTIONS ;;;
;;;;;;;;;;;;;;;;;;;;;;;;

(defmacro try-catch-alert [& body]
  `(try
     ;; (ex-info "The ice cream has melted!" 
     ;;          {:current-temperature {:value 25 :unit :celsius}})
     (do ~@body)
     (catch clojure.lang.ExceptionInfo e#
       (i/warning "Plagin Manager" (.getMessage e#) :time 7)
       ;; (c/alert (str "Update Manager effrror: " (.getMessage e#) " Type:" (name (:type (ex-data e#)))))
       )
     (catch Exception e#
       (i/warning "Plagin Manager" (.getMessage e#) :time 7)
       ;; (c/alert (str "Update Manager Main error: " (.getMessage e#)))
       )))

(defn setColumnWidth [^javax.swing.JTable table & {:keys [column size]}]
  (let [^javax.swing.table.TableColumnModel column-model (.getColumnModel table)
        ^javax.swing.table.TableColumn      table-column (.getColumn column-model column)]
    (.setPreferredWidth table-column size)))

(defn var-string-value [variable]
  (let [len-const 100
        variable-str (str (deref (clojure.core/var-get (:link variable))))]
    ;; (if (< len-const (.length variable-str))
    ;;   (str (apply str (take len-const variable-str)) "...")
    ;;   variable-str)
    variable-str
    ))

;;;;;;;;;;;;;;;;;;;;;
;;; UI COMPONENTS ;;;
;;;;;;;;;;;;;;;;;;;;;

(defn info [s]
  {:pre [(string? s)]}
  (seesaw.core/label :halign :left :foreground face/c-foreground-title :text s :font (gs/getFont :bold) :border (b/empty-border :thickness 15)))

;;;;;;;;;;;;;;;;;;;;;;;
;;; UI Composititon ;;;
;;;;;;;;;;;;;;;;;;;;;;;
;; (defn supply-currently-loaded [items theme]
;;   (concat items
;;           [(info "Group view")]))

(defn var-content [var]
  (gmg/migrid
   :> "[200:200:200, fill]10px[50::, fill]" "[shrink 0]"
   {:gap [10] :args [:border (b/line-border :top 2 :color face/c-layout-background)]}
   [(c/label :text (gtool/str-cutter (str (get var :name)) 25)
             :tip (str (gtool/get-lang-basic :variable) ": " (get var :name)))
    (c/label :text (str (get var :value)))]))

(defn supply-content-all-vars [items vars-list]
  (if (seq vars-list)
    (into items
          (for [v vars-list]
            (gmg/migrid :v (var-content v))))
    (conj items
          (info "Variables cannot be empty. Jarman error"))))

(defn supply-content-groups [items vars-groups]
  (if (seq vars-groups)
    (into
     items
     (reduce (fn [acc [group-name var-list]]
               (concat acc
                       [(gcomp/button-expand
                         group-name
                         (gmg/migrid
                          :v {:args [:border (b/line-border :bottom 0 :color face/c-icon)]}
                          (doall (map (comp var-content second) var-list)))
                         :before-title #(c/label :icon (gs/icon GoogleMaterialDesignIcons/SETTINGS))
                         ;; :expand :always
                         )]))
             [] vars-groups))
    (conj items
          (info "Variables cannot be empty. Jarman error"))))

;;;;;;;;;;;;;;;;;;;;;;;;;
;;; GROUPING FUNCTION ;;; 
;;;;;;;;;;;;;;;;;;;;;;;;;

(defn transform-var [v]
  (-> v
      ;; (update :name #(if (nil? %) (name (symbol (get v :link))) %))
      (update :name (fn [_](name (symbol (get v :link)))))
      (update :ns name)
      (update :loaded #(if % "loaded" "not used"))
      (assoc  :value (var-string-value v))))

(defn get-grouped-by-group-vars []
  (sort-by first
           (group-by #(string/join " " (-> (:group (second %)) name (string/split #"-")))
                     (map (fn [[var-kwd v]] (vector var-kwd (transform-var v)))
                          (vars/variable-list-all)))))

(defn get-grouped-by-loaded-vars []
  (sort-by first
           (group-by #(if (= (:loaded (second %)) "loaded")
                        "Loaded"
                        "Not loaded")
                     (map (fn [[var-kwd v]] (vector var-kwd (transform-var v)))
                          (vars/variable-list-all)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; SOME GRUOPED VIEW LIST ;;; 
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn view-grouped-by-group []
  (gtool/join-mig-items
   (supply-content-groups [] (get-grouped-by-group-vars))))

(defn view-grouped-by-loaded []
  (gtool/join-mig-items
   (supply-content-groups [] (get-grouped-by-loaded-vars))))

;;;;;;;;;;;;;
;;; PANEL ;;;
;;;;;;;;;;;;;

;; ni chuja nie rozumiem jak tym sie zarzadza.
;; bier to w swoje lapki ziom.
;; Przyznaje siê ¿e moje wlasne pieklo to migi
;; - I can no frear bro.. but that thing '"wrap 1" "0px[grow, fill]0px"', scare me
(defn vars-listing-panel []
  (let [panel (gmg/migrid :v {:gap [5 0]} (view-grouped-by-group))]
    (gmg/migrid
     :v (gtool/join-mig-items
         [(gcomp/menu-bar
           {;;:justify-end true
            :buttons [[" List by \"Variable Group\" " (gs/icon GoogleMaterialDesignIcons/APPS)
                       (fn [e]
                         (c/config! panel :items (gtool/join-mig-items (view-grouped-by-group)))
                         (.repaint (c/to-root (c/to-widget e))))]
                      [" List by \"Loaded variables\" " (gs/icon GoogleMaterialDesignIcons/ARCHIVE)
                       (fn [e]
                         (c/config! panel :items (gtool/join-mig-items (view-grouped-by-loaded)))
                         (.repaint (c/to-root (c/to-widget e))))]]})
          (gcomp/min-scrollbox panel)]))))

(comment
  (-> (c/frame :content (vars-listing-panel))
      c/pack!
      c/show!))



