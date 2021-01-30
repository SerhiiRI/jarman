;;; TEST FIELD  
(ns jarman.foo
  (:import jarman.Test.Test))

;; Java class import
; (import jarman.Test.Test)
;; Create java object and use function
(.pr (Test.))
;; Use static function of java class
(Test/pr_more)