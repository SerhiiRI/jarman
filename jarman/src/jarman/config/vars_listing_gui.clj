(ns jarman.config.vars-listing-gui
  (:require
   ;; swing tools
   [seesaw.core   :as c]
   [seesaw.table  :as table]
   [seesaw.dev :as sdev]
   [seesaw.mig :as smig]
   [seesaw.swingx  :as swingx]
   [seesaw.chooser :as chooser]
   [seesaw.border  :as b]
   ;; clojure lib
   [clojure.string :as string]
   [clojure.pprint :refer [cl-format]]
   [clojure.java.io :as io]
   ;; packages 
   ;; local functionality
   [jarman.logic.state :as state]
   [jarman.tools.config-manager :as cm]
   [jarman.gui.gui-components :as gcomp]
   [jarman.gui.gui-tools :as gtool]
   [jarman.gui.gui-style :as gs]
   [jarman.gui.gui-views-service :as gvs]
   [jarman.resource-lib.icon-library :as icon]
   [jarman.config.vars :as vars]
   ;; [jarman.tools.lang :refer [in?]]
   ;; [jarman.tools.fs :as fs]
   ;; [jarman.plugin.extension-manager :as extension-manager]
   ;; environtemnt variables
   [jarman.config.environment :as env])
  (:import (java.io IOException FileNotFoundException)))



;;;;;;;;;;;;;;;;;;;;;;;;
;;; HELPER FUNCTIONS ;;;
;;;;;;;;;;;;;;;;;;;;;;;;

(defmacro try-catch-alert [& body]
  `(try
     ;; (ex-info "The ice cream has melted!" 
     ;;          {:current-temperature {:value 25 :unit :celsius}})
     (do ~@body)
     (catch clojure.lang.ExceptionInfo e#
       ((state/state :alert-manager)               
        :set {:header "Plagin Manager"
              :body   (.getMessage e#)}  7)
       ;; (c/alert (str "Update Manager effrror: " (.getMessage e#) " Type:" (name (:type (ex-data e#)))))
       )
     (catch Exception e#
       ((state/state :alert-manager)               
        :set {:header "Plagin Manager"
              :body   (.getMessage e#)}  7)
       ;; (c/alert (str "Update Manager Main error: " (.getMessage e#)))
       )))

(defn setColumnWidth [^javax.swing.JTable table & {:keys [column size]}]
  (let [^javax.swing.table.TableColumnModel column-model (.getColumnModel table)
        ^javax.swing.table.TableColumn      table-column (.getColumn column-model column)]
    (.setPreferredWidth table-column size)))

;;;;;;;;;;;;;;;;;;;;;
;;; UI COMPONENTS ;;;
;;;;;;;;;;;;;;;;;;;;;

(defn- var-content [var-desc]
  (doto
      (c/table
       :show-horizontal-lines? nil
       :show-vertical-lines? nil
       :model (table/table-model :columns [{:key :name}
                                           {:key :loaded}
                                           {:key :ns}
                                           {:key :doc}]
                                 :rows [var-desc]))
    (.setRowMargin 10)
    (.setRowHeight 35)
    (.setIntercellSpacing (java.awt.Dimension. 10 0))
    (setColumnWidth :column 0 :size 270)
    (setColumnWidth :column 1 :size 200)
    (setColumnWidth :column 2 :size 400)
    (setColumnWidth :column 3 :size 1000)
    ;; (setColumnWidth :column 3 :size 600)
    (.setRowSelectionAllowed false)
    ;; (.setAutoResizeMode javax.swing.JTable/AUTO_RESIZE_LAST_COLUMN)
    ))

(defn info [s]
  {:pre [(string? s)]}
  (seesaw.core/label :halign :left :foreground "#074a4f" :text s :font (gs/getFont :bold) :border (b/empty-border :thickness 15)))


;;;;;;;;;;;;;;;;;;;;;;;
;;; UI Composititon ;;;
;;;;;;;;;;;;;;;;;;;;;;;



;; (defn supply-currently-loaded [items theme]
;;   (concat items
;;           [(info "Group view")]))

(defn supply-content-all-vars [items vars-list]
  (if (seq vars-list)
    (into items
          (for [v vars-list]
            (c/horizontal-panel
             :items 
             [(var-content v)])))
    (conj items
          (info "Variables cannot be empty. Jarman error"))))

(defn supply-content-groups [items vars-groups]
  (if (seq vars-groups)
    (into
     items
     (reduce (fn [acc [group-name var-list]]
               (concat acc
                       [(info group-name)]
                       (map (comp var-content second) var-list)))
             [] vars-groups))
    (conj items
          (info "Variables cannot be empty. Jarman error"))))

;;;;;;;;;;;;;;;;;;;;;;;;;
;;; GROUPING FUNCTION ;;; 
;;;;;;;;;;;;;;;;;;;;;;;;;

(defn transform-var [v]
  (-> v
      (update :name #(if (nil? %) (name (symbol (get v :link))) %))
      (update :ns name)
      (update :loaded #(if % "loaded" "not used"))))

(defn get-grouped-by-group-vars []
  (sort-by first
           (group-by #(string/join " " (-> (:group (second %)) name (string/split #"-")))
                     (map (fn [[var-kwd v]] (vector var-kwd (transform-var v)))
                          (vars/variable-list-all)))))

(defn get-grouped-by-loaded-vars []
  (sort-by first
           (group-by #(if (:loaded (second %))
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
  (let [panel (seesaw.mig/mig-panel :constraints ["wrap 1" "0px[grow, fill]0px" "0px[]0px"] :items (view-grouped-by-group))]
    (seesaw.mig/mig-panel
     :constraints ["wrap 1" "0px[grow, fill]0px" "0px[]0px"]
     :items
     (gtool/join-mig-items
      [(gcomp/menu-bar
        {:justify-end true
         :buttons [[" List by \"Variable Group\" " icon/download-grey1-64-png
                    (fn [e] (c/config! panel :items (gtool/join-mig-items (view-grouped-by-group))))]
                   [" List by \"Loaded variables\" " icon/download-grey1-64-png
                    (fn [e] (c/config! panel :items (gtool/join-mig-items (view-grouped-by-loaded))))]]})
       panel]))))

(comment
  (-> (c/frame :content (vars-listing-panel))
      c/pack!
      c/show!))

 






(into {} {:a 1})

(group-by )


