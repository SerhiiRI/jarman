(ns jarman.tools.export-files
  (:use seesaw.core
        seesaw.border
        seesaw.dev
        seesaw.mig
        seesaw.font
        seesaw.chooser
        clojure.walk)
 
  (:require [clojure.string :as string]
            [jarman.tools.swing :as stool]
            [jarman.gui.gui-tools :refer :all]
            [jarman.resource-lib.icon-library :as icon]
            [jarman.config.environment :as env]
            [jarman.config.storage :as strg]
            [kaleidocs.merge :refer [merge-doc]]
            [clojure.java.io :as io]
            [clojure.java.io :refer [file output-stream input-stream]])

  ;; (:import [org.odftoolkit.odfdom.doc OdfTextDocument]
  ;;          [org.odftoolkit.simple.common.navigation TextNavigation])
  )

(def temp-directory (strg/temp-templates-dir-path))

(defn read-data [file-path]
  (let [f (java.io.File. file-path)
        ary (byte-array (.length f))
        inputStream (java.io.FileInputStream. f)]
    (.read inputStream ary)
    (.close inputStream)
    ary))

(defn write-data [file-path data]
  (let [outputStream (java.io.FileOutputStream. file-path)]
    (.write outputStream data)))

(defn convert-file [file-name values-map export-directory]
  (merge-doc
   (str temp-directory "/" file-name)
   (str export-directory "/" file-name)
   values-map))

(defn get-name-temp-files [path]
  (second (re-find #".+/([\w\.\d]*)" path)))

(defn push! [q val]
  (swap! q conj val))

(defn get-files [a-data]
  (map (fn [vecfile] (subs (str (first (keys vecfile))) 1)) @a-data))

(def data-files (atom []))

(defn delete-file [name-file]
  (reset! data-files (vec (remove #(= (keyword name-file) (first (keys %))) @data-files))))

(defn load-file-tmp-dir [file-name]
  (write-data 
   (str temp-directory "/" file-name)
   (second (first (vals (first (filter (fn [x] (= (first (keys x))(keyword file-name))) @data-files)))))))

(def f (frame :title "file"
              :undecorated? false
              :resizable? false
              :minimum-size [600 :by 240]
              :content (label)))

(def data-templ [{:enterprener.lastname "Burmych" :user.lastname "Smith" :date "04.10.2005"}
                 {:user.lastname "Burmych" :user.date "04.10.2021"}
                 {:user.firstname "Julka" :user.lastname "Burmych"}])

(def emp-border (empty-border :bottom 10 :top 10 :left 20 :right 20))

(def btn (fn [txt func](label
                        :text txt
                        :background "#fff"
                        :foreground "#96c1ea"
                        :border (compound-border emp-border)
                        :listen [:mouse-entered (fn [e] (config! e :background "#deebf7"
                                                                     :foreground "#256599" :cursor :hand))
                                 :mouse-exited  (fn [e] (config! e :background "#fff" :foreground "#96c1ea"))
                                 :mouse-clicked func])))

(def templ-panel (let [descript-field (text :editable? true :columns 16 :margin 6
                                              :border (compound-border emp-border
                                                                       (line-border :bottom 4 :color "#96c1ea")))
                       path-field (text :text env/user-home
                                        :editable? false :columns 16 :margin 6
                                              :border (compound-border emp-border
                                                                       (line-border :bottom 4 :color "#96c1ea")))
                       combo-keys (combobox :model data-templ)
                       combo-files (combobox :model (if (= @data-files []) [] (vec (get-files data-files))))
                       btn-imp (btn "Import file" (fn [e] (if (= (text descript-field) "")
                                                            (config! descript-field :border
                                                                     (compound-border
                                                                      emp-border
                                                                      (line-border :bottom 4 :color "#e51a4c")))
                                                            (let [path-file (choose-file  :success-fn
                                                                                          (fn [fc file] (.getAbsolutePath file)))
                                                                  file-name (get-name-temp-files path-file)]
                                                              (push! data-files {(read-string (str ":" file-name))
                                                                                 [(text descript-field)
                                                                                  (read-data path-file)]})
                                                              (config! descript-field
                                                                       :text ""
                                                                       :border
                                                                       (compound-border
                                                                        emp-border
                                                                        (line-border :bottom 4 :color "#96c1ea")))
                                                              (config! combo-files :model (if (= @data-files []) [] (vec (get-files data-files))))))))
                       btn-exp (btn "Export file" (fn [e] (if-not (and (= (text combo-keys) nil)(= (text combo-files) nil))
                                                            (do
                                                              (load-file-tmp-dir (text combo-files))
                                                              (convert-file
                                                               (text combo-files)
                                                               (read-string (string/replace (text combo-keys) "," ""))
                                                               (text path-field))))))
                       btn-path (btn "Change path" (fn [e] (config! path-field :text (choose-file  :success-fn (fn [fc file] (.getAbsolutePath file))
                                                                                                   :selection-mode :dirs-only ))))
                       btn-delete (btn "Delete file" (fn [e] (if-not (= (text combo-files) nil)
                                                               (do (delete-file (text combo-files))
                                                                   (config! combo-files :model (if (= @data-files []) [] (vec (get-files data-files)))
                                                                            )))))]
                   (mig-panel
                    :constraints ["wrap 2" "40px[grow, left]20px" "20px[]20px"]
                    :items [
                            [(jarman.gui.gui-tools/textarea "<h3>Add description of file:</h3>" :foreground "#2c7375" :font (getFont 18))]
                            [combo-keys ]
                            [descript-field]
                            [combo-files "split 3"]
                            [path-field]
                            [btn-path]
                            [btn-imp]
                            [btn-exp "split 2"]
                            [btn-delete]
                            ])))


(config! f :content templ-panel)

(-> (doto f (.setLocationRelativeTo nil) seesaw.core/pack! seesaw.core/show!))



;; (defn -main [& args]
;;   (let [document (OdfTextDocument/loadDocument "./templates/dogovir.odt")
        
;;         texts    (.getTextContent (.getContentRoot document))
;;         text    (.getContentRoot document)
;;         ]
;;      ;;(println document)
;;     (println text)
;;     (println texts)
;;     ))


;; (-main)
