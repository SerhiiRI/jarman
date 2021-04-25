(ns jarman.tools.jarman)

(defn gen-context-tree
  ([] (gen-context-tree (symbol (str *ns*))))
  ([nms] {:pre [(symbol? nms)]}
   (let [pc #(println (apply str ";; " %&))
         offset (atom 0)
         file (:file (meta (resolve (symbol (format "%s/%s" (name nms) (first (keys (ns-publics nms))))))))
         headers (filter
                  identity
                  (map-indexed
                   (fn [i e] (if (clojure.string/starts-with? e ";;")
                              (let [e (-> e (clojure.string/replace #"\\r" "") clojure.string/trim)]
                                (if-let [[_ hb header he] (re-matches #"^(;{2,4})\s*([\w\d\s]*[\w\d]{1})\s*(;{2,4})$" e)]
                                  (let [chb (count hb) che (count he)]
                                    (if (= chb che)
                                      {:line i :header header :h-size (dec chb)})) ))))
                   (clojure.string/split (slurp file) #"\n")))
         fk-list (map (fn [f-v]
                        (let [nms_var (resolve (symbol (format "%s/%s" (name nms) f-v)))]
                          {:name (str f-v) :args (apply str (interpose ", " (:arglists (meta nms_var))))
                           :line (:line (meta nms_var))}))
                      (vec (keys (ns-publics nms))))]
     ;; (pc (format "The namespace %s" (str nms)))
     (pc "<TODO Description>")
     (pc "Content: ")
     (for [e (sort-by :line (concat headers fk-list))]
       (if (nil? (:header e)) 
         (pc 
          (apply str (repeat @offset " "))
          (if (empty? (:args e))
            (format "  %s" (:name e))
            (format "  %s %s" (:name e) (:args e))))
         (do
           (swap! offset (fn [_] (:h-size e)))
           (pc 
            (apply str (repeat @offset "*"))
            (format " %s" (:header e)))))
       ))nil))

(defn- comment-header-parser
  "Description
    Parse comment line

  Spec
    :header - some string
    :h-size - [0,1]
  
  Example
    (comment-header-parser \";; suka bliat ;;\") 
       ;;=> {:header \"suka bliat\", :h-size 1}"
  [e]
  (if-let [[_ hb header he] (re-matches #"^(;{2,3})\s*([\w\d\s]*[\w\d]{1})\s*(;{2,3})$" e)]
        (let [chb (count hb) che (count he)]
          (if (= chb che)
            {:header header :h-size (- 2 (dec chb))}))))

(defn def-parser [e]
  (if-let [[_ def-type spec name] (re-matches #"\((defn-|def|defn|defmacro){1}\s(\^:dynamic\s|\^:private\s)?([\w-\*]{2,}).*" e)]
    {:name name :private? (if (or (= spec "^:private ") (= def-type "defn-")) "private")
     :type (case def-type "def" "variable" "defn" "function" "defn-" "function" "defmacro" "macro")}))

(defn- line-parser [n e]
 (if (or
      (clojure.string/starts-with? e "(def")
      (clojure.string/starts-with? e ";;"))
   (let [e (-> e (clojure.string/replace #"\\r" "") clojure.string/trim)]
     (if-let [struct
              (cond
                (clojure.string/starts-with? e "(def") (def-parser e) 
                (clojure.string/starts-with? e ";;") (comment-header-parser e))]
      (assoc struct :line n)))))

;; (defn- parse-header [e]
;;  (re-matches #";{2,4}\s+CONTEXT\s+v(ersion )?(\d.\d).*" ";; CONTEXT version 0.1"))

(defn hard-context-tree [file]
  (if (.exists (clojure.java.io/file file))
   (let [pc #(println (apply str ";; " %&))
         offset (atom 0)
         structure (filter identity (map-indexed line-parser (clojure.string/split (slurp file) #"\n")))]
     (pc "CONTEXT version 0.1")
     (pc "Content: ")
     (doall
      (for [e (sort-by :line structure)]
        (if (nil? (:header e)) 
          (do
            (pc (apply str (repeat (+ 2 @offset) " ")) (format (if (:private? e)
                                                                 " # %s (%s)"
                                                                 " %s (%s)")
                                                               (:name e)
                                                               (:type e))))
          (do
            (swap! offset (fn [_] (inc (:h-size e))))
            (pc (apply str (repeat @offset "*")) (format " %s" (:header e)))))
        ))))nil)

;; (hard-context-tree "./src/jarman/config/init.clj")




(defn for-you-with-love
  ([] (for-you-with-love "no bo wiesz tak szbyciej"))
  ([text]
   (let
    [l (count text)
     tl "┌" tr "┐"
     v  "│" h  "─"
     bl "└" br "┘"]
     (println
      (apply
       str
       (concat
        (concat [tl] (vec (repeat (+ l 2) h)) [tr \newline])
        (concat [v] (vec (repeat (+ l 2) \space)) [v \newline])
        (concat [v \space] (seq text) [\space v \newline])
        (concat [v] (vec (repeat (+ l 2) \space)) [v \newline])
        (concat [bl] (vec (repeat (+ l 2) h)) [br \newline])))))))
