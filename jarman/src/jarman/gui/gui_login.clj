(ns jarman.gui.gui-login
  (:use seesaw.core
        seesaw.border
        seesaw.dev
        seesaw.mig
        seesaw.font)
  (:require [clojure.string :as string]
            [jarman.tools.swing :as stool]
            [jarman.resource-lib.icon-library :as icon]
            ))


;;(map str (.listFiles (clojure.java.io/file "./")))

(show-options (password))
(do(def field-login (fn [] (text :editable? true :columns 20 :margin 6
                                 :border (compound-border (empty-border :left 10 :right 10 :top 5 :bottom 5)
                                                          (line-border :bottom 4 :color "#96c1ea")))))
   (def field-pass (fn [] (password :editable? true :columns 20 :margin 6
                                :border (compound-border (empty-border :left 10 :right 10 :top 5 :bottom 5)
                                                         (line-border :bottom 4 :color "#96c1ea")))))


   ;; (defn authenticate-user [login password]
   ;;   (s-query (select :user :where {:login login
   ;;                                  :password password})))

   (def authenticate-user 
     (fn [login password] (if (and (= login "admin") (= password "password")) true)))
  
   (def btn-login
     (fn []
       (label :text "LOGIN" :background "#fff"
              :foreground "#96c1ea"
              :listen [:mouse-entered (fn [e] (config! e :background "#deebf7" :foreground "#256599" :cursor :hand))
                       :mouse-exited  (fn [e] (config! e :background "#fff" :foreground "#96c1ea"))
                       :mouse-clicked (fn [e] (println (text (field-login))) 
                                        ;; (if ((authenticate-user) (text field-login) (text field-pass))
                                        ;;   ;;(config! :border :color "#333" )(config! :border :color "#333" )
                                        ;;   )
                                        )]
              :border (compound-border (empty-border :bottom 10 :top 10
                                                     :left 30 :right 30)
                                       ;; (line-border :bottom 2 :color "#d9ecff")
                                       ))))
   
   (def login-panel (let [flogin (field-login)]
                      [fpass (field-password)]
                      (mig-panel
                       :constraints ["wrap 1" "[grow, center]" "30px[]0px"]
                       :items [
                               [(label :icon (stool/image-scale "resources/imgs/jarman-text.png" 10))]

                               [(mig-panel
                                 :constraints ["" "[grow, fill]" "5px[]0px"]
                                 :items [
                                         [(label :icon (stool/image-scale icon/user-blue1-64-png 40))]
                                         [(field-login)]
                                         [(label :border (empty-border :right 20))]
                                         ])]

                               [(mig-panel
                                 :constraints ["" "[grow, fill]" "5px[]0px"]
                                 :items [
                                         [(label :icon (stool/image-scale icon/key-blue-64-png 40))]
                                         [(field-pass)]
                                         [(label :border (empty-border :right 20))]
                                         ])]
                               
                               [(btn-login) ]

                               [(label :text " " :border
                                       (empty-border :top 20 :left 860 )) "split 2"]
                               [(mig-panel
                                 :constraints ["" "[grow, fill]" ""]
                                 :items [
                                         [(label :icon (stool/image-scale icon/refresh-connection-grey1-64-png 50)
                                                 :border (compound-border (empty-border :right 10 )))]
                                         [(label :icon (stool/image-scale icon/settings-64-png 50)
                                                 :border (compound-border (empty-border :right 10 )))]
                                         [(label :icon (stool/image-scale icon/I-grey-64-png 50))]])]])))


   (def f (frame :title "Jarman-login"
                 :undecorated? false
                 :minimum-size [1000 :by 760]
                 :content login-panel))

   (-> (doto f (.setLocationRelativeTo nil) seesaw.core/pack! seesaw.core/show!)))
