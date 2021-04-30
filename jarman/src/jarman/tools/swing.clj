;;; File contain some usefull functions, hacks, or examples
;;; Contens:
;;; * Parameterss configurations
;;; * AWT/Swing helpers
;;; * Icons library generator
;;; * Fonts library generator
;;; * Font debug, tool
;;; * Map-type toolkit
;;; ** head/tail destruction for maps
;;; ** cond-contain test if key in map
;;; ** key-paths
(ns jarman.tools.swing
  (:use clojure.reflect
        seesaw.core)
  (:require [clojure.string :as string]
            [jarman.tools.lang :as lang]
            [jarman.tools.config-manager :as cm]
            [clojure.java.io :as io]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Paramters configuration ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def ^:dynamic *icon-library* "final library class-path file" "src/jarman/resource_lib/icon_library.clj")
(def ^:dynamic *font-library* "final library class-path file" "src/jarman/resource_lib/font_library.clj")

;; (def ^:dynamic *font-path* "font directory"      (cm/getset "config-manager.edn" [:font-configuration-attribute :font-path] "resources/fonts/"))
;; (def ^:dynamic *icon-path* "pack icon directory" (cm/getset "config-manager.edn" [:icon-configuration-attribute :icon-path] "icons/main"))
;; (def ^:dynamic *acceptable-icon-file-format*     (cm/getset "config-manager.edn" [:icon-configuration-attribute :acceptable-file-format] ["png" "img"]))
;; (def ^:dynamic *acceptable-font-file-format*     (cm/getset "config-manager.edn" [:font-configuration-attribute :acceptable-file-format] ["ttf"]))

(def ^:dynamic *font-path* "font directory"      "resources/fonts/")
(def ^:dynamic *icon-path* "pack icon directory" "icons/main")
(def ^:dynamic *acceptable-icon-file-format*     ["png" "img"])
(def ^:dynamic *acceptable-font-file-format*     ["ttf"])

(defn namespace-for-path-name
  "Description
    Function using for generation resources libraries, where namespace created dynamicaly

  Example
    (namespace-for-path-name \"src/jarman/a/b/c_c/some_lib.clj\")
      => jarman.a.b.c-c.some-lib.clj"
  [f] (if-let [[_ in-project-path file] (re-matches #"src/jarman/(.*/){0,1}(.*)" f)]
        (let [replace_to- (fn [s] (string/replace s #"_" "-"))
              project-file-name (replace_to- (first (lang/split #"\." file)))
              project-file-root (first (lang/split #"\." (str *ns*)))
              project-file-offset (if in-project-path (lang/join "." (map replace_to- (lang/split #"[\\/]" in-project-path))))]
          (lang/join "." (lang/filter-nil (if project-file-offset
                                  [project-file-root project-file-offset project-file-name]
                                  [project-file-root project-file-name])))) (println "Error generation namespace. Not validated name:" f)))

;;;;;;;;;;;;;;;;;;;;;;;;;
;;; AWT/Swing helpers ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ^sun.awt.image.ToolkitImage image-scale
  "Function scale image by percent size.
  Return `sun.awt.image.ToolkitImage` type.

  Example:
    (image-scale \"/path/to/photo\" 100)

  See more:
    javadoc `sun.awt.image.ToolkitImage`
    javadoc `sun.awt.Image`"
  ([image-path]
   {:pre [(not (empty? image-path))]}
   (seesaw.icon/icon (clojure.java.io/file image-path)))
  ([image-path percent]
   {:pre [(not (empty? image-path))]}
   (let [image (.getImage (seesaw.icon/icon (clojure.java.io/file image-path)))
         scaler (comp int #(Math/ceil %) #(* % (/ percent 100.0)))]
     (doto (javax.swing.ImageIcon.)
       (.setImage (.getScaledInstance image
                         (scaler (.getWidth image))
                         (scaler (.getHeight image))
                         java.awt.Image/SCALE_SMOOTH))))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Icons library generator ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- is-icon-supported?
  "Example:
     (is-icon-supported? \"temp.clj\") ;=> true
     (is-icon-supported? \"temp.Kt1\") ;=> nil"
  [file-name]
  (if (or (lang/in? (seq "1234567890-=_") (first (.getName file-name))) (not (some #(= \. %) (seq (.getName file-name))))) nil
          (let [frmt (last (clojure.string/split (str file-name) #"\."))]
            (lang/in? *acceptable-icon-file-format* frmt))))

(defn- get-icon-data
  "Create icon wrapper library in *icon-library* location,
  based on icons located in *icon-path* directory
  Example:
    (refresh-icon-lib) "[]
  (for [icon-file (sort-by #(.getName %) (filter is-icon-supported? (.listFiles (clojure.java.io/file *icon-path*))))]
    (let [icon-f (.getName icon-file)
          [icon-name icon-format] (if-not (some #(= \. %) (seq icon-f)) [icon-f nil]
                                          (let [splited (clojure.string/split icon-f #"\.")]
                                            [(apply str (butlast splited)) (last splited)]))
          icon-symbol-name (symbol (str icon-name (if icon-format (str "-" icon-format))))
          icon-symbol-doc  (format "wrapper for icon %s" (str *icon-path* "/" icon-f))]
      [icon-symbol-name icon-symbol-doc (str icon-file)])))

(defn refresh-icon-lib
  "Create icon wrapper library in *icon-library* location,
  based on icons located in *icon-path* directory
  Example:
    (refresh-icon-lib) "[]
  (let [icon-data (get-icon-data)]
    (spit *icon-library* (format ";; Icon pack generated in %s\n" (.format (java.text.SimpleDateFormat. "YYYY-MM-dd HH:mm:ss") (java.util.Date.))))
    (spit *icon-library* (prn-str `(~(symbol "ns") ~(symbol (namespace-for-path-name *icon-library*)))) :append true)
    (spit *icon-library* "\n;; Icons\n" :append true)
    (doseq [[icon-symbol-name icon-symbol-doc icon-file] icon-data]
      (spit *icon-library* (prn-str `(def ~icon-symbol-name ~icon-symbol-doc ~icon-file)) :append true))
    (spit *icon-library* "\n;; All icons\n" :append true)
    (spit *icon-library* (prn-str `(def ~(symbol 'all-icon) ~(vec (map first icon-data)))) :append true)))

(defn debug-icon-panel "Funkcja wy�wietla okienko z czcionkami w swoim formacie." []
  (let [get-scale-percent (fn [icon-name]
                            (condp = (last (drop-last 1 (string/split icon-name #"-")))
                              "512" 15 "512x512" 15 "512X512" 15
                              "256" 25 "256x256" 25 "256X256" 25
                              "128" 50 "128x128" 50 "128X128" 50
                              "64" 100  "64x64" 100 "64X64"  100 
                              "32" 100 "32x32"  100 "32X32"  100
                              25))]
    (-> (seesaw.core/frame :content
                           (seesaw.core/scrollable
                            (seesaw.core/vertical-panel :items
                                                        (map (fn [[icon-symbol-name _ icon-file]]
                                                               (seesaw.core/grid-panel
                                                                :columns 2
                                                                :items [(seesaw.core/text :text (str icon-symbol-name)
                                                                                          :border (seesaw.border/empty-border :right 10)
                                                                                          :halign :right)
                                                                        (seesaw.core/label :icon (seesaw.icon/icon (image-scale icon-file (get-scale-percent (str icon-symbol-name))))
                                                                                           :listen [:mouse-clicked (fn [e]
                                                                                                                     (print (prn-str `(~'image-scale ~icon-symbol-name 100))))])]))
                                                             (get-icon-data)))))
        seesaw.core/pack!
        seesaw.core/show!)))

;; generation icon library
;; (refresh-icon-lib)
;; (refresh-font-lib)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Fonts library generator  ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- is-font-supported?
  "Example:
     (is-supported? \"temp.clj\") ;=> true
     (is-supported? \"temp.Kt1\") ;=> nil"
  [file-name]
  (if (or (lang/in? (seq "1234567890-=_") (first (.getName file-name))) (not (some #(= \. %) (seq (.getName file-name))))) nil
          (let [frmt (last (clojure.string/split (str file-name) #"\."))]
            (lang/in? *acceptable-font-file-format* frmt))))


(defn- get-font-data []
  (for [font-file (sort-by #(.getName %) (filter is-font-supported? (.listFiles (clojure.java.io/file *font-path*))))]
    (let [font (.getName font-file)
          [font-name font-format] (if-not (some #(= \. %) (seq font)) [font nil]
                                          (let [splited (clojure.string/split font #"\.")]
                                            [(apply str (butlast splited)) (last splited)]))
          font-symbol-name (symbol (clojure.string/lower-case (str font-name (if font-format (str "-" font-format)))))
          font-symbol-doc  (format "wrapper for local font %s" (str *font-path* "/" font))]
      [font-symbol-name font-symbol-doc (str font-file)])))



(def registrator-fn (prn-str '(defn registrate-local-font "register the font"
                                [font-path] (.registerFont (. java.awt.GraphicsEnvironment getLocalGraphicsEnvironment)
                                                           (java.awt.Font/createFont (java.awt.Font/TRUETYPE_FONT)
                                                                                     (clojure.java.io/file font-path))))))


(defn refresh-font-lib
  "Create fonts wrapper library in *font-library* location,
  based on icons located in *font-path* directory
  Example:
    (refresh-font-lib) "[]
  (let [font-data (get-font-data)]
    (spit *font-library* (format ";; Local fonts generated in %s\n" (.format (java.text.SimpleDateFormat. "YYYY-MM-dd HH:mm:ss") (java.util.Date.))))
    (spit *font-library* (prn-str `(~(symbol "ns") ~(symbol (namespace-for-path-name *font-library*)))) :append true)
    
    (spit *font-library* "\n;; Font list:\n" :append true)
    (doseq [[font-symbol-name font-symbol-doc font-file] font-data]
      (spit *font-library* (prn-str `(def ~font-symbol-name ~font-symbol-doc ~font-file)) :append true))
    (spit *font-library* "\n;; All Fonts\n" :append true)
    (spit *font-library* (prn-str `(def ~(symbol 'all-fonts) ~(vec (map first font-data)))) :append true)
    (spit *font-library* "\n;; Font registrator function. \n;; !! DO NOT do change on this file, bliat, only in dev-tools font-generator component \n" :append true)
    (spit *font-library* registrator-fn :append true)
    (spit *font-library* "\n;; Register all fonts\n" :append true)
    (spit *font-library* (prn-str '(doseq [font all-fonts]
                                     (registrate-local-font font))) :append true)))




;;;;;;;;;;;;;;;;;;;;;;;;
;;; Font debug, tool ;;;
;;;;;;;;;;;;;;;;;;;;;;;;

(defn show-fonts "Funkcja wyświetla listę dostępnych czcionek, ale nie zwraca ich."
  [] (map println (.. java.awt.GraphicsEnvironment getLocalGraphicsEnvironment getAvailableFontFamilyNames)))

(defn get-fonts "Funkcja zwraca nazy dostęnych czcionek."
  [] (map identity (.. java.awt.GraphicsEnvironment getLocalGraphicsEnvironment getAvailableFontFamilyNames)))

(defn debug-font-panel "Funkcja wyświetla okienko z czcionkami w swoim formacie."
  [& {:keys [txt size] :or {txt "Przykładowy tekst. Приклад тексту." size 16}}]
  (-> (seesaw.core/frame :content (seesaw.core/scrollable (seesaw.core/vertical-panel :items (map (fn [font] (seesaw.core/grid-panel
                                                                                                             :columns 2
                                                                                                             :items [(seesaw.core/text :text font
                                                                                                                                       :font {:name (str font) :size size}
                                                                                                                                       :border (seesaw.border/empty-border :right 10)
                                                                                                                                       :halign :right)
                                                                                                                     (seesaw.core/label :text txt :font {:name (str font) :size 16})]))
                                                                                                  (get-fonts)))))
      seesaw.core/pack!
      seesaw.core/show!))


(debug-font-panel)