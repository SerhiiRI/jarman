;;; File contain some usefull functions, hacks, or examples
;;; Contens:
;;; * Helper functions - quick usefull hacks
;;; * AWT/Swing helpers
;;; * Icons library generator
;;; * Fonts library generator
;;; * Font debug, tool
;;; * Map-type toolkit
;;; ** head/tail destruction for maps
;;; ** cond-contain test if key in map
;;; ** key-paths
(ns jarman.tools.dev-tools
  (:use clojure.reflect
        seesaw.core)
  (:require [clojure.string :as string]
            [jarman.tools.config-manager :as cm]
            [clojure.java.io :as io]))

;; (def ^:dynamic *icon-library* "final library class-path file" "src/jarman/resource_lib/icon_library.clj")
;; (def ^:dynamic *font-library* "final library class-path file" "src/jarman/resource_lib/font_library.clj")
;; (def ^:dynamic *font-path* "font directory"      (cm/getset "config-manager.edn" [:font-configuration-attribute :font-path] "resources/fonts/"))
;; (def ^:dynamic *icon-path* "pack icon directory" (cm/getset "config-manager.edn" [:icon-configuration-attribute :icon-path] "icons/main"))
;; (def ^:dynamic *acceptable-icon-file-format*     (cm/getset "config-manager.edn" [:icon-configuration-attribute :acceptable-file-format] ["png" "img"]))
;; (def ^:dynamic *acceptable-font-file-format*     (cm/getset "config-manager.edn" [:font-configuration-attribute :acceptable-file-format] ["ttf"]))

;;;;;;;;;;;;;;;;;;;;;;;
;;; helper function ;;;
;;;;;;;;;;;;;;;;;;;;;;;

(defn in?
  "(in? [1 2 3 4 5 6] 1)
   (in? 1 1)"
  [col x]
  (if (and (not (string? col)) (seqable? col))
    (some #(= x %) col)
    (= x col)))

(defmacro filter-nil
  "(filter-nil [nil 1 nil 3 4]) ;=> [1 3 4]"
  [col]
  `(filter identity ~col))

(defmacro join
  "(filter-nil [nil 1 nil 3 4]) ;=> [1 3 4]"
  [delimiter col]
  `(clojure.string/join ~delimiter ~col))

(defmacro split
  "(filter-nil [nil 1 nil 3 4]) ;=> [1 3 4]"
  [delimiter s]
  `(clojure.string/split ~s ~delimiter ))

(defn all-methods
  "Print methods for object/class, in argument"
  [some-object]
  (->> some-object
       reflect :members (filter :return-type) (map :name) sort (map #(str "." %) ) distinct println))

(defn namespace-for-path-name
  "Description
    Function using for generation resources libraries, where namespace created dynamicaly

  Example
    (namespace-for-path-name \"src/jarman/a/b/c_c/some_lib.clj\")
      => jarman.a.b.c-c.some-lib.clj"
  [f] (if-let [[_ in-project-path file] (re-matches #"src/jarman/(.*/){0,1}(.*)" f)]
        (let [replace_to- (fn [s] (string/replace s #"_" "-"))
              project-file-name (replace_to- (first (split #"\." file)))
              project-file-root (first (split #"\." (str *ns*)))
              project-file-offset (if in-project-path (join "." (map replace_to- (string/split in-project-path #"[\\/]"))))]
          (join "." (filter-nil (if project-file-offset
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
  (if (or (in? (seq "1234567890-=_") (first (.getName file-name))) (not (some #(= \. %) (seq (.getName file-name))))) nil
          (let [frmt (last (clojure.string/split (str file-name) #"\."))]
            (in? *acceptable-icon-file-format* frmt))))

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

;; generation icon library
;; (refresh-icon-lib)

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




;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Fonts library generator  ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- is-font-supported?
  "Example:
     (is-supported? \"temp.clj\") ;=> true
     (is-supported? \"temp.Kt1\") ;=> nil"
  [file-name]
  (if (or (in? (seq "1234567890-=_") (first (.getName file-name))) (not (some #(= \. %) (seq (.getName file-name))))) nil
          (let [frmt (last (clojure.string/split (str file-name) #"\."))]
            (in? *acceptable-font-file-format* frmt))))


(defn- get-font-data []
  (for [font-file (sort-by #(.getName %) (filter is-font-supported? (.listFiles (clojure.java.io/file *font-path*))))]
    (let [font (.getName font-file)
          [font-name font-format] (if-not (some #(= \. %) (seq font)) [font nil]
                                          (let [splited (clojure.string/split font #"\.")]
                                            [(apply str (butlast splited)) (last splited)]))
          font-symbol-name (symbol (clojure.string/lower-case (str font-name (if font-format (str "-" font-format)))))
          font-symbol-doc  (format "wrapper for local font %s" (str *font-path* "/" font))]
      [font-symbol-name font-symbol-doc (str font-file)])))



(def registrator-fn (prn-str '(defn registrate-local-font "Zarejestruj czcionk� po �cie�c�"
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

;;; refresh font lib
;; (refresh-font-lib)

;;;;;;;;;;;;;;;;;;;;;;;;
;;; Font debug, tool ;;;
;;;;;;;;;;;;;;;;;;;;;;;;

(defn show-fonts "Funkcja wy�wietla list� dost�pnych czcionek, ale nie zwraca ich."
  [] (map println (.. java.awt.GraphicsEnvironment getLocalGraphicsEnvironment getAvailableFontFamilyNames)))

(defn get-fonts "Funkcja zwraca nazy dost�nych czcionek."
  [] (map identity (.. java.awt.GraphicsEnvironment getLocalGraphicsEnvironment getAvailableFontFamilyNames)))

(defn debug-font-panel "Funkcja wy�wietla okienko z czcionkami w swoim formacie."
  [& {:keys [txt size] :or {txt "Przyk�adowy tekst od Mr. Jarmana" size 16}}]
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

(defn copy [uri file]
  (with-open [in (io/input-stream uri)
              out (io/output-stream file)]
    (io/copy in out)))

(defn unzip [file dir]
  (let [saveDir (java.io.File. dir)]
    (with-open [stream (java.util.zip.ZipInputStream. (io/input-stream file))]
      (loop [entry (.getNextEntry stream)]
        (if entry
          (let [savePath (str dir java.io.File/separatorChar (.getName entry))
                saveFile (java.io.File. savePath)]
            (if (.isDirectory entry)
              (if-not (.exists saveFile)
                (.mkdirs saveFile))
              (let [parentDir (java.io.File. (.substring savePath 0 (.lastIndexOf savePath (int java.io.File/separatorChar))))]
                (if-not (.exists parentDir) (.mkdirs parentDir))
                (io/copy stream saveFile)))
            (recur (.getNextEntry stream))))))))


;;;;;;;;;;;;;;;;;;;;;;;;
;;; Map-type toolkit ;;;
;;;;;;;;;;;;;;;;;;;;;;;;

(defmacro first-key
  "Description:
    Get the first key of some map
  
  Examples:  
  (first-key {:a 1 :b 2}) => :a
  (first-key {})          => nil"
  [m]
  `(first (first (seq ~m))))

(defmacro map-first
  "Description:
    The same as `first` function for list.
  
  Examples:  
  (map-first {:a 1 :b 2}) => {:a 1}
  (map-first {})          => nil"
  [m]
  `(if-let [mf# (first (seq ~m))]
     (into {} (list mf#))))

(defmacro map-rest
  "Description:
    The same as `rest` function for list.
  
  Examples:
  (map-rest {:a 1 :b 2 :c 1}) => {:b 2, :c 1}
  (map-rest {})               => nil"
  [m] 
  `(let [mf# (rest (seq ~m))]
     (if (not-empty mf#) (into {} mf#))))

(defmacro map-destruct
  "Description:
    For map return vector of two map, where first
    is head, and second is tail.
  
  Example:
    (map-destruct {:a 1 :b 2 :c 3}) => [{:a 1} {:b 2, :c 3}]
    (map-destruct {:a 1})           => [{:a 1} nil]
    (map-destruct {})               => [nil nil]
  
  See related:
    (`jarman.logic.metadata/map-first`, `jarman.logic.metadata/map-rest`)"
  [m] 
  `(let [sm# ~m]
     (if-let [m-head# (map-first sm#)]
       (let [m-tail# (map-rest sm#)]
         [m-head# m-tail#])
       [nil nil])))

(defmacro cond-contain
  "Description
    Simple macro for easy using pattern-maching on map's

  Example
    (cond-contain {:a 1 :b 2}
      :a (println 1)
      :b (println 2)
      3)"
  [m & body]
  `(condp (fn [kk# mm#] (contains? mm# kk#)) ~m
     ~@body))

(defmacro find-column
  "Descripion
    Short macro for geting first value of lazy seq.
  
  Example
    (find-column #(= % 2) [1 2 3 4])"
  [f col]
  `(first (filter ~f ~col)))

(defmacro map-partial
  "Example
    (map-partial [1 2 3] [:a :b :c]) => [[1 :a] [2 :b] [3 :c]]"
  [f & body] `(map (comp vec concat list) ~@body))

(defmacro get-apply
  "Apply one function to 2-4 maps
  
  Example
    (get-apply + [:a] {:a 1} {:a 1}) ;; => 2"
  [f path & maps]
  (let [fx-gets (for [m maps] `(get-in ~m ~path nil))] 
    `(~f ~@fx-gets)))

(defn Y-Combinator []
  (((fn [f] (f f))
    (fn [f]
      (fn [s n]
        (if (not (empty? n))
          ((f f) (+ s (second (first n))) (rest n))
          s
          )))) 0 (seq {:a 1 :b 1 :c 1})))

(defn get-key-paths-recur [& {:keys [map-part end-path-func path sequence?]}]
  ;; If `map-part` is nil, then eval function
  ;; end-path-func. Otherwise destruct `map-part`
  ;; for continuing recursion
  (if (nil? map-part) (end-path-func path)
      (let [[head tail] (map-destruct map-part)
            map-first-key (first-key head)
            value-of-first-key (doto (map-first-key head) println )
            ;; shortlambda
            vconcat (comp vec concat)]
        (cond
          
          ;; Do recusion if it Hashmap
          (map? value-of-first-key)
          (get-key-paths-recur
           :map-part (map-first-key head)
           :end-path-func end-path-func
           :path (vconcat path [map-first-key])
           :sequence? sequence?)

          ;; Do recursion if value is vector
          ;; and `sequence?` parameter has
          ;; `true` value
          (and sequence? (seqable? value-of-first-key) (not (string? value-of-first-key)))
          (doall (map
                  (fn [index-value index]
                    (get-key-paths-recur
                     :map-part index-value
                     :end-path-func end-path-func
                     :path (vconcat path [map-first-key] [index])
                     :sequence? sequence?))
                  value-of-first-key (range (count value-of-first-key))))

          ;; Do recusion with `nil` for `:map-part`
          ;; if param be nil - then evaluate function
          ;; `(end-path-func path)`
          :else (get-key-paths-recur
                 :map-part nil
                 :end-path-func end-path-func
                 :path (vconcat path [map-first-key])
                 :sequence? sequence?))
        ;; Recusion in width, without chnaging
        ;; path deepth
        (if tail
          (get-key-paths-recur
           :map-part tail
           :end-path-func end-path-func
           :path path
           :sequence? sequence?)))))

(defn key-paths
  "Description
    Get vector's of all keys from map linking it in path.
    If `sequence?` optionaly parameter is set on true - searching deep include also list's
  
  Example
    (key-paths {:a 1 :b {:t 2 :f 2} :c [{:t 3} {:f 3}]})
      ;;=> [[:a]
            [:b :t]
            [:b :f]
            [:c]]
    (key-paths {:a 1 :b {:t 2 :f 2} :c [{:t 3} {:f 3}]} :sequence? true)
      ;;=> [[:a]
            [:b :t]
            [:b :f]
            [:c 0 :t]
            [:c 1 :f]]"
  [m & {:keys [sequence?] :or {sequence? false}}]
  (blet (get-key-paths-recur
         :map-part m
         :end-path-func in-deep-key-path-f
         :path nil
         :sequence? sequence?)
        @in-deep-key-path
        [in-deep-key-path (atom [])
         in-deep-key-path-f (fn [path] (swap! in-deep-key-path (fn [path-list] (conj path-list path ))))]))


(defmacro cond-let
  "Description
    Is macro which combine let+cond
    cond-let has one 'hack'. It use first
    binded pattern to automaticaly apply
    to ONE-WORD predicates
    Macro automatic transform
     string? -> (string? T)

  Example
    (cond-let [T \"something\"]
	string?     \"is string\"
	boolean?    \"is boolean\"
	(number? T) \"is number \"  
	:else nil)"
  [binding & body]
  (let [var-name (first binding)
        cond-list
        (reduce
         concat
         (map #(if (symbol? (first %1)) (list (list (first %1) var-name) (second %1)) %1)
              (partition 2 body)))]
    `(let [~@binding]
       (cond
         ~@cond-list))))

(defmacro blet
  "Description
    Let with binding in last sexp, otherwise in first block
  
  Example
    (blet (+ a b) [a 1 b 2]) ;; => 3
    (blet (+ a b) (- a b) [a 1 b 2]) ;; => -1
  
  Spec
    (blet <calcaulation>+ <binding-spec>{1})"
  [& arguments]
  {:pre [(> 0 (count arguments )) (vector? (last arguments))]}
  `(let ~(last arguments) ~@(butlast arguments)))
