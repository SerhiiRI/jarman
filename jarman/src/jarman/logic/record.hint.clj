
(defprotocol IEditName
  (get-name [this])
  (set-name! [this val]))

(deftype PersonName [^:volatile-mutable lname]
  IEditName
  (get-name [this] (. this lname))
  (set-name! [this val] (set! lname val)))
  
(def pname (PersonName. "hoge"))
;=> #'user/pname

(set-name! pname "fuge")
;=> "fuge"

(get-name pname)
;=> "fuge"
