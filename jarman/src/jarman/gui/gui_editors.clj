(ns jarman.gui.gui-editors
  (:use seesaw.dev
        seesaw.mig)
  (:require [jarman.resource-lib.icon-library :as icon]
            [seesaw.core   :as c]
            [seesaw.border :as b]
            [seesaw.util   :as u]
            [seesaw.rsyntax]
            [jarman.tools.lang :refer :all]
            [jarman.config.config-manager :as cm]
            [jarman.gui.gui-components    :as gcomp]
            [jarman.gui.gui-views-service :as gvs]
            [jarman.logic.state    :as state]
            [jarman.logic.metadata :as mt]
            [jarman.gui.gui-tools  :as gtool]))


;; ┌────────────────────�
;; │                    │
;; │ Basic components   │
;; │                    │________________________________________
;; └────────────────────�                                     

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
           label
           args]
    :or {val ""
         store-id :code
         local-changes (atom {})
         syntax :clojure
         label nil
         args []}}]
  (swap! local-changes (fn [state] (assoc state store-id val)))
  (let [content (atom val)]
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
                                  (c/config! label :text "Unsaved file..."))))]
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
         args {}}}]
  (let [f-size (atom font-size)
        info-label (c/label)
        code (code-area {:args [:font (gtool/getFont @f-size)
                                :border (b/empty-border :left 10 :right 10)]
                         :label info-label
                         :local-changes local-changes
                         :store-id store-id
                         :val val}) 
        editor (gcomp/vmig
                :args args
                :vrules "[fill]0px[grow, fill]0px[fill]"
                :items [[(gcomp/hmig
                          :args args
                          :hrules "[70%, fill]10px[grow, fill]"
                          :items
                          [[(c/label :text title
                                     :border (b/compound-border (b/line-border :bottom 1 :color "#eee")
                                                                (b/empty-border :left 10))
                                     :font (gtool/getFont :bold title-font-size))]
                           [(gcomp/menu-bar
                             {:justify-end true
                              :buttons [[""
                                         icon/up-blue2-64-png
                                         "Increase font"
                                         (fn [e]
                                           (c/config!
                                            code
                                            :font (gtool/getFont
                                                   (do (reset! f-size (+ 2 @f-size))
                                                       @f-size))))]
                                        [""
                                         icon/down-blue2-64-png
                                         "Decrease font"
                                         (fn [e]
                                           (c/config!
                                            code
                                            :font (gtool/getFont
                                                   (do (reset! f-size (- @f-size 2))
                                                       @f-size))))]
                                        (if debug
                                          ["" icon/loupe-blue-64-png "Show changes" (fn [e] (gcomp/popup-info-window
                                                                                             "Changes"
                                                                                             (second (first @local-changes))
                                                                                             (c/to-frame e)))]
                                          nil)
                                        ["" icon/agree-blue-64-png "Save" (fn [e]
                                                                            (c/config! info-label :text "Saved")
                                                                            ((:saved-content (c/config code :user-data)))
                                                                            (save-fn {:state local-changes :label info-label :code code}))]
                                        (if dispose
                                          ["" icon/enter-64-png "Leave" (fn [e] (.dispose (c/to-frame e)))]
                                          nil)]})]])]
                        [(gcomp/scrollbox code)]
                        [info-label]])]
    (gtool/set-focus code)
    
    editor))


(defn popup-config-editor
  "Description:
     Prepared popup window with code editor for selected configuration segment.
   Example:
     (popup-metadata-editor [:init.edn] {:part-of-config {}})
  "
  [config-path config-part]
  (gcomp/popup-window
   {:window-title "Configuration manual editor"
    :view (code-editor
           {:val (with-out-str (clojure.pprint/pprint config-part))
            :title (gtool/convert-key-to-title (first config-path))
            :dispose true
            :save-fn (fn [props]
                       (try
                         (cm/assoc-in-segment config-path (read-string (c/config (:code props) :text)))
                         (let [validate (cm/store-and-back)]
                           (if (:valid? validate)
                             (c/config! (:label props) :text "Saved!")
                             (c/config! (:label props) :text "Validation faild. Can not save.")))
                         (catch Exception e (c/config!
                                             (:label props)
                                             :text "Can not convert to map. Syntax error."))))})})
  (gvs/reload-view :reload))

(defn view-config-editor
  "Description:
     Prepared view with code editor for selected configuration segment.
   Example:
     (popup-metadata-editor [:init.edn] {:part-of-config {}})
  "
  [config-path config-part]
  (gvs/add-view
   :view-id (keyword (str "manual-view-code" (first config-path)))
   :title (str "Config: " (gtool/convert-key-to-title (first config-path)))
   :render-fn
   (fn [] (code-editor
           {:args [:border (b/line-border :top 1 :left 1 :color "#eee")
                   :background "#fff"]
            :val (with-out-str (clojure.pprint/pprint config-part))
            :title (gtool/convert-key-to-title (first config-path))
            :save-fn (fn [props]
                       (try
                         (cm/assoc-in-segment config-path (read-string (c/config (:code props) :text)))
                         (let [validate (cm/store-and-back)]
                           (if (:valid? validate)
                             (c/config! (:label props) :text "Saved!")
                             (c/config! (:label props) :text "Validation faild. Can not save.")))
                         (catch Exception e (c/config!
                                             (:label props)
                                             :text "Can not convert to map. Syntax error."))))}))))


(defn popup-metadata-editor
  "Description:
     Prepared popup window with code editor for selected metadata by table_name as key.
   Example:
     (popup-metadata-editor :user)
  "
  [table-keyword]
  (let [meta (first (mt/getset! table-keyword))]
      (gcomp/popup-window
       {:window-title "Metadata manual table editor"
        :view (code-editor
               {:val (with-out-str (clojure.pprint/pprint (:prop meta)))
                :title (str "Metadata: " (get-in meta [:prop :table :representation]))
                :dispose true
                :save-fn (fn [state]
                           (try
                             (mt/update-meta (assoc meta :prop (read-string (c/config (:code state) :text))))
                             (c/config! (:label state) :text "Saved!")
                             (catch Exception e (c/config!
                                                 (:label state)
                                                 :text "Can not convert to map. Syntax error."))))})}))
  (gvs/reload-view))

(defn view-metadata-editor
  "Description:
     Prepared component with code editor for selected metadata by table_name as key.
   Example:
     (view-metadata-editor :user)
  "
  [table-keyword]
  (gvs/add-view
   :view-id (keyword (str "manual-view-code" (name table-keyword)))
   :title (str "Metadata: " (name table-keyword))
   :render-fn
   (fn [] (let [meta (first (mt/getset! table-keyword))]
            (code-editor
             {:args [:border (b/line-border :top 1 :left 1 :color "#eee")
                     :background "#fff"]
              :title (str "Metadata: " (get-in meta [:prop :table :representation]))
              :val (with-out-str (clojure.pprint/pprint (:prop meta)))
              :save-fn (fn [state]
                         (try
                           (mt/update-meta (assoc meta :prop (read-string (c/config (:code state) :text))))
                           (c/config! (:label state) :text "Saved!")
                           (catch Exception e (c/config!
                                               (:label state)
                                               :text "Can not convert to map. Syntax error."))))})))))



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
              :border (border-fn (gtool/get-color :decorate :focus-lost))
              :listen [:focus-gained (fn [e] (c/config! e :border (border-fn (gtool/get-color :decorate :focus-gained))))
                       :focus-lost   (fn [e] (c/config! e :border (border-fn (gtool/get-color :decorate :focus-lost))))
                       :caret-update (fn [e] (func e))]
              args)]
    (gcomp/vmig
          :hrules "[0:150:150, fill]"
          :items [[code]])))

  
