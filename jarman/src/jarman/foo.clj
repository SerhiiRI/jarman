;;; TEST FIELD  
(ns jarman.foo
  (:import jarman.Test.Test))

;; Java class import
; (import jarman.Test.Test)
;; Create java object and use function
(.pr (Test.))
;; Use static function of java class
(Test/pr_more)





;; (def get-color
;;   "Description:
;;      Set as argument path to value in map without :value key, macro build correct path.
;;      Macro using outside variable with complete theme map.
;;    Example:
;;      (get-color-mc :jarman :bar) ;; => #292929
;;    "
;;   (fn [& steps]
;;     (get-in jarman.config.config-manager/theme-map (concat colors-root-path (interpose :value (vec steps)) [:value]))))
;; ;; (get-color :jarman :bar)