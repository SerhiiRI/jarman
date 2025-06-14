(ns jarman.gui.gui-editors
  (:use seesaw.dev
        seesaw.mig)
  (:require [jarman.resource-lib.icon-library :as icon]
            [seesaw.core   :as c]
            [seesaw.border :as b]
            [seesaw.util   :as u]
            [seesaw.rsyntax]
            [jarman.gui.gui-style          :as gs]
            [jarman.gui.gui-components     :as gcomp]
            [jarman.gui.gui-style          :as gs]
            [jarman.gui.gui-views-service  :as gvs]
            [jarman.gui.gui-tools          :as gtool]
            [jarman.gui.gui-migrid         :as gmg]
            [jarman.gui.gui-alerts-service :as gas]
            [jarman.faces                  :as face]
            [jarman.lang                   :refer :all]
            [jarman.org                    :refer :all]
            [jarman.logic.state            :as state]
            [jarman.logic.metadata         :as mt]
            [jarman.logic.sql-tool  :refer [select! update! insert!]]
            [jarman.logic.connection   :as connection]
            [jarman.logic.view-manager :as vm])
    (:import
     (jiconfont.icons.google_material_design_icons GoogleMaterialDesignIcons)))


;; ┌────────────────────�
;; │                    │
;; │ Basic components   │
;; │                    │________________________________________
;; └────────────────────�                                     

(defn find-syntax
  ([] (find-syntax nil true))
  ([file-format] (find-syntax file-format false))
  ([file-format return-list]
   (let [key (keyword (rift file-format :clj))
         syntaxs {:js   :javascript
                  :bat  :bat
                  :clj  :clojure
                  :css  :css
                  :c    :c
                  :java :java
                  :py   :python
                  :php  :php
                  :none :perl
                  :cpp  :cpp
                  :json :json
                  :xml  :xml
                  :html :html
                  :txt  :html
                  ;; :none :lua
                  ;; :none :properties
                  ;; :none :scala
                  ;; :none :actionscript
                  ;; :none :dtd
                  ;; :none :lisp
                  ;; :none :htaccess
                  ;; :none :latex
                  ;; :none :cs
                  ;; :none :delphi
                  ;; :none :unix
                  ;; :none :makefile
                  ;; :none :jsp
                  ;; :none :sql
                  ;; :none :sas
                  ;; :none :plain
                  ;; :none :nsis
                  ;; :none :mxml
                  ;; :none :d
                  ;; :none :tcl
                  ;; :none :asm
                  ;; :none :bbcode
                  ;; :none :ruby
                  ;; :none :fortran
                  ;; :none :groovy
                  ;; :none :dart
                  ;; :none :vb
                  }]
     (if return-list
       (vec (map #(second %) syntaxs))
       (rift (key syntaxs) :clojure)))))

;;(find-syntax)

(defn code-area
  "Description:
    Some text area but with syntax styling.
    To check avaliable languages eval (seesaw.dev/show-options (seesaw.rsyntax/text-area)).
    Default language is Clojure.
  Example:
    (code-area {})
    (code-area :syntax :css)
  "
  [{:keys [val
           store-id
           local-changes
           syntax
           file-format
           label
           args]
    :or {val ""
         store-id :code
         local-changes (atom {})
         syntax nil
         file-format nil
         label nil
         args []}}]
  (swap! local-changes (fn [state] (assoc state store-id val)))
  (let [content    (atom val)
        use-syntax (if syntax syntax (find-syntax file-format))]
    (apply
     seesaw.rsyntax/text-area
     :text val
     :wrap-lines? true
     :caret-position 0
     :syntax syntax
     :user-data {:saved-content (fn [] (reset! content (second (first @local-changes))))}
     :listen [:caret-update (fn [e]
                              (swap! local-changes (fn [state] (assoc state store-id (c/config (c/to-widget e) :text))))
                              (if-not (nil? label)
                                (if (= (c/config (c/to-widget e) :text) @content)
                                  (c/config! label :text "")
                                  (c/config! label :text (gtool/get-lang-basic :unsaved-changes) :foreground face/c-red))))]
     args)))

;;(seesaw.dev/show-options (seesaw.rsyntax/text-area))

(defn code-editor
  "Description:
     Simple code editor using syntax.
     When you send save-fn then inside is invoke (save-fn {:state local-changes :label info-label :code code})
   Example:
     (code-editor {})
  "
  [{:keys [local-changes
           store-id
           val
           font-size
           title
           title-font-size
           save-fn
           debug
           dispose
           syntax
           file-format
           menu
           args]
    :or {local-changes (atom {})
         store-id :code-tester
         val ""
         title "Code editor"
         title-font-size 14
         font-size 14
         save-fn (fn [state] (println "Additional save"))
         debug false
         dispose false
         syntax nil
         file-format nil
         menu true
         args {}}}]
  (let [f-size (atom font-size)
        info-label (c/label)
        code (code-area {:args [:font (gs/getFont @f-size)
                                :border (b/empty-border :left 10 :right 10)]
                         :label info-label
                         :file-format file-format
                         :local-changes local-changes
                         :syntax (find-syntax syntax)
                         :store-id store-id
                         :val val}) 
        editor (gmg/hmig
                :args args
                :wrap 1
                :vrules "[fill]0px[grow, fill]0px[fill]"
                :items [[(gmg/hmig
                          :args args
                          :hrules "[70%, fill]10px[grow, fill]"
                          :bgap 10
                          :items
                          [[(c/label :text title
                                     :border (b/compound-border (b/line-border :bottom 1 :color "#eee")
                                                                (b/empty-border :left 10))
                                     :font (gs/getFont :bold title-font-size))]
                           [(gmg/migrid :v "[200, fill]" :center {:args [:background face/c-compos-background]}
                             (c/combobox :model  (concat [(find-syntax syntax)] (filter #(not (= % (find-syntax syntax)))(find-syntax)))
                                         :listen [:mouse-motion (fn [e] (.repaint (gvs/get-view-space)))
                                                  :item-state-changed
                                                  (fn [e] 
                                                    (let [new-v (c/config e :selected-item)]
                                                      (try
                                                        (c/config! code :syntax new-v))))]))]
                           [(if menu
                              (gcomp/menu-bar
                              {:justify-end true
                               :buttons [[""
                                          (gs/icon GoogleMaterialDesignIcons/ZOOM_IN face/c-icon 25)
                                          "Increase font"
                                          (fn [e]
                                            (c/config!
                                             code
                                             :font (gs/getFont
                                                    (do (reset! f-size (+ 2 @f-size))
                                                        @f-size))))]
                                         [""
                                          (gs/icon GoogleMaterialDesignIcons/ZOOM_OUT face/c-icon 25)
                                          "Decrease font"
                                          (fn [e]
                                            (c/config!
                                             code
                                             :font (gs/getFont
                                                    (do (reset! f-size (- @f-size 2))
                                                        @f-size))))]
                                         (if debug
                                           ["" (gs/icon GoogleMaterialDesignIcons/SEARCH face/c-icon 25)
                                            "Show changes"
                                            (fn [e] (gcomp/popup-info-window
                                                     "Changes"
                                                     (second (first @local-changes))
                                                     (c/to-frame e)))]
                                           nil)
                                         [""
                                          (gs/icon GoogleMaterialDesignIcons/SAVE face/c-icon)
                                          "Save"
                                          (fn [e]
                                            (c/config! info-label :text (gtool/get-lang-basic :saved))
                                            ((:saved-content (c/config code :user-data)))
                                            (save-fn {:state local-changes :label info-label :code code}))]
                                         (if dispose
                                           ["" (gs/icon GoogleMaterialDesignIcons/EXIT_TO_APP face/c-icon) "Leave" (fn [e] (.dispose (c/to-frame e)))]
                                           nil)]})
                              (c/label))]])]
                        [(gcomp/scrollbox code)]
                        [info-label]])]
    (gtool/set-focus code)
    editor))

(defn popup-metadata-editor
  "Description:
     Prepared popup window with code editor for selected metadata by table_name as key.
   Example:
     (popup-metadata-editor :user)
  "
  [table-keyword]
  (let [meta (first (mt/return-metadata table-keyword))]
      (gcomp/popup-window
       {:window-title "Metadata manual table editor"
        :view (code-editor
               {:val (with-out-str (clojure.pprint/pprint (:prop meta)))
                :title (str "Metadata: " (get-in meta [:prop :table :representation]))
                :dispose true
                :save-fn (fn [state]
                           (try
                             (mt/database-update-metadata-table (assoc meta :prop (read-string (c/config (:code state) :text))))
                             (c/config! (:label state) :text (gtool/get-lang-basic :saved))
                             (catch Exception e (c/config!
                                                 (:label state)
                                                 :text "Can not convert to map. Syntax error."))))})}))
  (gvs/reload-view))

(defn popup-view-editor
  "Description:
     Prepared popup window with code editor for defview.
   Example:
     (popup-defview-editor \"user\")"
  [table-str]
  (let [dview (vm/view-get table-str)]
    (gcomp/popup-window
     {:window-title (str "Defview manual table editor: " )
      :view (code-editor
             {:val (with-out-str
                     (clojure.pprint/pprint
                      (read-string (binding [jarman.logic.sql-tool/*debug* false]
                                     (:view (vm/view-get table-str)))))) ;;(:view dview)
              :dispose true
              :save-fn (fn [state]
                         (try
                           (vm/view-set (assoc dview :view (c/config (:code state) :text)))
                           (c/config! (:label state) :text "Saved!")
                           (catch Exception e (c/config!
                                               (:label state)
                                               :text "Can not convert to map. Syntax error."))))})})
    (gvs/reload-view)))


(defn view-metadata-editor
  "Description:
     Prepared component with code editor for selected metadata by table_name as key.
   Example:
     (view-metadata-editor :user)"
  [table-keyword]
  {:pre [(keyword? table-keyword)]}
  (gvs/add-view
   :view-id (keyword (str "manual-view-metadata-code" (name table-keyword)))
   :title (str "Metadata: " (name table-keyword))
   :render-fn
   (fn [] (let [meta (first (mt/return-metadata table-keyword))]
            (code-editor
             {:args [:border (b/line-border :top 1 :left 1 :color "#eee")
                     :background "#fff"]
              :title (str "Metadata: " (get-in meta [:prop :table :representation]))
              :val (with-out-str (clojure.pprint/pprint (:prop meta)))
              :save-fn (fn [state]
                         (try
                           (mt/database-update-metadata-table (assoc meta :prop (read-string (c/config (:code state) :text))))
                           (c/config! (:label state) :text (gtool/get-lang-basic :saved))
                           (catch Exception e (c/config!
                                               (:label state)
                                               :text "Can not convert to map. Syntax error."))))})))))


(defn view-view-editor
  "Description:
     Prepared component with code editor for selected view by table_name as key.
   Example:
     (view-view-editor :user)"
  [table-keyword]
  {:pre [(keyword? table-keyword)]}
  (if-let [dview (vm/view-get (name table-keyword))]
    (gvs/add-view
     :view-id (keyword (str "manual-view-view-code" (name table-keyword)))
     :title (str "View: " (name table-keyword))
     :render-fn
     (fn []
       (code-editor
        {:args [:border (b/line-border :top 1 :left 1 :color "#eee")
                :background "#fff"]
         :title (str "View: " (name table-keyword))
         :val (with-out-str
                (clojure.pprint/pprint
                 (read-string (binding [jarman.logic.sql-tool/*debug* false] (:view dview)))))
         :save-fn (fn [state]
                    (try
                      (vm/view-set (assoc dview :view (c/config (:code state) :text)))
                      ;; (c/config! (:label state) :text "Saved!")
                      (catch Exception e (c/config!
                                          (:label state)
                                          :text "Can not convert to map. Syntax error."))))})))
    (do
          (print-line (format "Cannot find Defview for %s" table-keyword))
          (gas/alert "View" (format "Cannot find Defview for %s" table-keyword) :type :danger :time 8))))

(defn view-view-editor-old
  "Description:
     Prepared popup window with code editor for defview.
   Example:
     (popup-defview-editor \"user\")
  "
  [table-str]
  (let [dview (vm/view-get table-str)]
    (gvs/add-view
     :view-id (keyword (str "manual-defview-code" table-str))
     :title (str "Defview: " table-str)
     :render-fn
     (fn [] (code-editor
             {:args [:border (b/line-border :top 1 :left 1 :color "#eee")
                     :background "#fff"]
              :title (str "Defview: " table-str)
              :val (with-out-str
                     (clojure.pprint/pprint
                      (read-string (binding [jarman.logic.sql-tool/*debug* false]
                                     (:view (vm/view-get table-str)))))) ;;(:view dview)
              :save-fn (fn [state]
                         (try
                           (vm/view-set (assoc dview :view (c/config (:code state) :text)))
                           (c/config! (:label state) :text "Saved!")
                           (catch Exception e (c/config!
                                               (:label state)
                                               :text "Can not convert to map. Syntax error."))))})))))

;;(popup-defview-editor "user")
;; (defn buttons-list--code-editor-defview
;;   "Description:
;;      Inject expand button then when pointing id.
;;    Example:
;;      (buttons-list--code-editor-defview :#expand-menu-space)
;;   "
;;   [plugplace-id]
;;   (let [table-and-view-coll (db/query
;;                              (select!
;;                               {:table_name :view
;;                                :column [:table_name]}))
;;         comp (gcomp/button-expand
;;               "Defviews Editors"
;;               (doall
;;                (map
;;                 (fn [m]
;;                   (gcomp/button-expand-child
;;                    (:table_name m)
;;                    :onClick (fn [e] (view-defview-editor (:table_name m))) ))
;;                 table-and-view-coll)))]
;;     (.add (c/select (state/state :app) [:#expand-menu-space]) comp)
;;     (.revalidate (c/to-root (state/state :app)))))

;; (defn prepare-defview-editors-state
;;   "Description:
;;      Prepare state with defview editor fns for view service
;;      and set to state with :defview-editors key.
;;      Invoke again to refresh state.
;;    Example:
;;      (prepare-defview-editors-state)" []
;;   (let [table-and-view-coll (db/query
;;                              (select!
;;                               {:table_name :view
;;                                :column [:table_name]}))]
;;     (doall
;;      (state/set-state
;;       :defview-editors
;;       (into
;;        {}
;;        (map
;;         (fn [m]
;;           {(keyword (:table_name m)) (fn [e] (view-defview-editor (:table_name m)))})
;;         table-and-view-coll))))))


(defn state-code-area
  "Description:
    Some text area but with syntax styling.
    To check avaliable languages eval (seesaw.dev/show-options (seesaw.rsyntax/text-area)).
    Default language is Clojure.
  Example:
    (state-code-area {})
    (state-code-area :syntax :css)
  "
  [{func :func
    val  :val}
   & {:keys [lang args]
      :or   {lang :clojure
             args []}}]
  (let [border-fn (fn [color]
                    (b/line-border  :bottom 2 :color color))
        code (apply
              seesaw.rsyntax/text-area
              :text (try (pp-str (read-string (rift val "{}")))
                         (catch Exception e "{:error \"Error parsing\"}"))
              :wrap-lines? true
              :caret-position 0
              :syntax lang
              :border (border-fn face/c-underline)
              :listen [:focus-gained (fn [e] (c/config! e :border (border-fn face/c-underline-on-focus)))
                       :focus-lost   (fn [e] (c/config! e :border (border-fn face/c-underline)))
                       :caret-update (fn [e] (func e))]
              args)]
    (gcomp/min-scrollbox
     code)))

  
(defn text-file-editor
  ([file-path] (text-file-editor file-path nil true))
  ([file-path syntax] (text-file-editor file-path syntax true))
  ([file-path syntax menu]
   (let [file (clojure.string/split (last (clojure.string/split file-path #"/")) #"\.")
         file-name (first file)
         file-format (if (= 2 (count file)) (second file) nil)]
     (gmg/migrid :v :a :a
                 (code-editor
                  {:args [ ;;:border (b/line-border :top 1 :left 1 :color "#eee")
                          :background "#fff"]
                   :val (slurp file-path)
                   :title (str "Edit: " (clojure.string/join "." file))
                   :syntax (rift syntax (keyword file-format))
                   :menu menu
                   :save-fn (fn [props]
                              (try
                                ;;(println "\nTo save:\n" (:code-tester @(:state props)))
                                (spit file-path (:code-tester @(:state props))) 
                                (catch Exception e (c/config!
                                                    (:label props)
                                                    :text "Some error: Can not save."))))})))))


(defn editor
  "Description:
    Set path to directory and file name
  Example:
    (editor \"./test/test-file\")"
  ([file-path] (editor file-path nil))
  ([file-path syntax]
   (let [file-name (last (clojure.string/split file-path #"/"))]
     (gvs/add-view
      :view-id   (keyword (str "editor" file-name))
      :title     (str "Edit:  " file-name)
      :render-fn (fn [] (text-file-editor file-path syntax))))))

(comment
  (let [frame (seesaw.core/frame
                :title "Demo"
                :minimum-size [500 :by 500]
                :size [500 :by 500]
                :content (text-file-editor "./test/test-file.txt"))
        ]
    (doto
        frame
      (.setLocationRelativeTo nil) c/pack! c/show!))
  )

