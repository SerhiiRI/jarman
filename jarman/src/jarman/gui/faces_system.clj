(ns jarman.gui.faces-system)
(require '[jarman.org :refer :all])

;;;;;;;;;;;;
;;; CORE ;;;
;;;;;;;;;;;;

(def faces-storage (atom #{}))
(defmacro define-face [face-variable-name]
  {:pre [(symbol? face-variable-name)]}
  (let [face-variable-ref `(var ~face-variable-name)]
    `(do
      (declare ~face-variable-name)
      (swap! faces-storage conj ~face-variable-ref)
      ~face-variable-ref)))

;;;;;;;;;;;;;;;
;;; HELPERS ;;;
;;;;;;;;;;;;;;;

(defn faces-list-out-all []
  (print-header
   "All registered faces:"
   (when (seq (deref faces-storage))
     (doall (map #(print-line (format "- %s" (str (symbol %)))) (seq (deref faces-storage))))))
  true)

(defn faces-list-out-all-with-values []
  (print-header
   "All registered faces:"
   (when (seq (deref faces-storage))
     (doall (map #(print-line (format "- %s - ~%s~" (str (symbol %)) (str (var-get %)))) (seq (deref faces-storage))))))
  true)

(defn- output-faces [msg faces-list]
  (when (seq faces-list)
    (print-header
     msg
     (doall (map #(print-line (format "- %s" (str (symbol %)))) (seq faces-list))))))

(defn prepare-bind-variable-set [variable-list]
  ;; Hard NAMESPACE to faces
  (let [faces-ns 'jarman.faces]
   (reduce
    (fn [{:keys [faces-variable-used faces-face-value-var-list]}
        [face-variable face-value]]
      (if-let [face-var-ref ((deref faces-storage) (intern faces-ns face-variable))]
        (let [face-val-ref (if-not (symbol? face-value)
                             (throw (ex-info (format "Error when bind Face `%s`. Value `%s` should be a symbol"
                                                     (name face-variable)
                                                     (name face-value))
                                             {:type :undefinied-face-variable-type
                                              :var face-variable
                                              :value face-value}))
                                   (if ((deref faces-storage) (intern faces-ns face-value))
                                     (intern faces-ns face-value)
                                     (if-let [maybe-nil-variable-reference (resolve face-value)]
                                       maybe-nil-variable-reference
                                       (throw (ex-info (format "Error when bind Face `%s`. Unresolved face value `%s`"
                                                               (name face-variable)
                                                               (name face-value))
                                                       {:type :undefinied-face-variable
                                                        :var face-variable
                                                        :value face-value})))))]
          {:faces-variable-used       (conj faces-variable-used face-var-ref)
           :faces-face-value-var-list (conj faces-face-value-var-list [face-var-ref face-val-ref])})
        (throw (ex-info (format "Undefienied face `%s`. Face not exist in system" (name face-variable))
                        {:type :undefinied-face-var
                         :var face-variable}))))
    {:faces-variable-used []
     :faces-face-value-var-list []}
    (partition-all 2 variable-list))))

(defn custom-theme-set-faces [variable-list]
  (let [face-value-var-list (prepare-bind-variable-set variable-list)
        face-value-var-list
        (assoc face-value-var-list :faces-variable-not-used
               (apply disj @faces-storage (:faces-variable-used face-value-var-list)))
        face-value-var-list
        (assoc face-value-var-list :face-theme-apply
               (fn []
                 (output-faces "Warning! Don't used faces"
                                    (:faces-variable-not-used face-value-var-list))
                 (output-faces "Changed faces"
                                    (:faces-variable-used face-value-var-list))
                 (doall (map (fn [[face-var value-var]]
                               (alter-var-root face-var (fn [_] (var-get value-var))))
                             (:faces-face-value-var-list face-value-var-list)))))]
    (:face-theme-apply face-value-var-list))) 


