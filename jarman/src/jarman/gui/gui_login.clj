(ns jarman.gui.gui-login
  (:use seesaw.core
        seesaw.border
        seesaw.dev
        seesaw.mig
        seesaw.font
        clostache.parser
        )
 
  (:require [clojure.string :as string]
            [jarman.tools.swing :as stool]
            [jarman.gui.gui-tools :refer :all]
            [jarman.resource-lib.icon-library :as icon]
             
            ))




(map str (.listFiles (clojure.java.io/file "./")))
(show-options (frame))
(show-options (label))
(show-options (scrollable (text)))

;; (def field-login (fn [] (text :editable? true :columns 20 :margin 6
;;                                :border (compound-border (empty-border :left 10 :right 10 :top 5 :bottom 5)
;;                                                         (line-border :bottom 4 :color "#96c1ea")))))
;;  (def field-pass (fn [] (password :editable? true :columns 20 :margin 6
;;                               :border (compound-border (empty-border :left 10 :right 10 :top 5 :bottom 5)
;;                                                        (line-border :bottom 4 :color "#96c1ea")))))


;; (defn authenticate-user [login password]
;;   (s-query (select :user :where {:login login
;;                                  :password password})))



(defn authenticate-user [login password]
  (if (and (= login "admin") (= password "admin")) true))


;; (defn generate-ask [ask-str answer] (jarman.gui.gui-tools/textarea (apply str (list "<h2>" ask-str "</h2>"))
;;                                                             :foreground "#2c7375"
;;                                                             :font (getFont 18)
;;                                                             :listen [:mouse-entered (fn [e] (do (config! e :foreground "#256599" :cursor :hand) (show! answer)))
;;                                                                      :mouse-exited  (fn [e] (do (config! e :foreground "#2c7375") (hide! answer)))
                                                                    
;;                                                                      ]))


(def asks-panel (fn []
                  (mig-panel
                   :constraints ["wrap 1" "20px[grow, center]" ""]
                   :items [[(jarman.gui.gui-tools/textarea "<h3>- Why can't i add my table?</h3>" :foreground "#394e74" :font (getFont 18)) "align l"]
                           [(jarman.gui.gui-tools/textarea "<p align= \"justify\">It is a long established fact that a reader will be distracted by the readable content of a page when lookiult model text, and a search for 'lorem ipsum' will uncover many web sites still in their infancy. Various versions have evolved over the years, sometimes by accident, sometimes on purpose (injected humour and the like)</p>" :foreground "#738590" )]
                           [(jarman.gui.gui-tools/textarea "<h3>- Other problems?</h3>" :foreground "#394e74" :font (getFont 18)) "align l"]
                           [(jarman.gui.gui-tools/textarea "<p align= \"justify\">It is a long established fact that a reader will be distracted by the readable content of a page when looking at its layout. Thnd the like)</p>" :foreground "#738590" )]]
                   )))


(def info-panel (scrollable
                 (mig-panel
                  :constraints ["wrap 1" "[grow, center]" "20px[]20px"]
                  :items [[(label :icon (stool/image-scale icon/left-blue-64-png 50)
                                  :listen [:mouse-clicked (fn [e] (config! f :content login-panel))]
                                  :border (empty-border :right 200)) "align l, split 2"]
                          [(label :icon (stool/image-scale "resources/imgs/trashpanda2-stars-blue-1024.png" 47))]
                          [(mig-panel
                            :constraints ["wrap 1" "100px[:600, grow, center]100px" "20px[]20px"]
                            :items [
                                    [(jarman.gui.gui-tools/textarea "<h2>About</h2>" :foreground "#2c7375" :font (getFont 18)) "align l"]
                                    [(jarman.gui.gui-tools/textarea "<p align= \"justify\" >It is a long established fact that a reader will be distracted by the readable content of a page when looking at its layout. The point of using Lorem Ipsum is that it has a more-or-less normal distribution of letters, as opposed to using 'Content here, content here', making it look like readable English. Many desktop publishing packages and web page editors now use Lorem Ipsum as their default model text, and a search for 'lorem ipsum' will uncover many web sites still in their infancy. Various versions have evolved over the years, sometimes by accident, sometimes on purpose (injected humour and the like)</p>" :foreground "#636d77" )]
                                    [(jarman.gui.gui-tools/textarea "<h2>Jarman</h2>" :foreground "#2c7375" :font (getFont 18)) "align l"]
                                    [(jarman.gui.gui-tools/textarea "<p align= \"justify\">It is a long established fact that a reader will be distracted by the readable content of a page when looking at its layout. The point of using Lorem Ipsum is that it has a more-or-less normal distribution of letters, as opposed to using 'Content here, content here', making it look like readable English. Many desktop publishing packages and web page editors now use Lorem Ipsum as their default model text, and a search for 'lorem ipsum' will uncover many web sites still in their infancy. Various versions have evolved over the years, sometimes by accident, sometimes on purpose (injected humour and the like)</p>" :foreground "#636d77" )]
                                    [(jarman.gui.gui-tools/textarea "<h2>Contact</h2>" :foreground "#2c7375" :font (getFont 18)) "align l"]
                                    [(jarman.gui.gui-tools/textarea "<p align= \"justify\">It is a long established fact that a reader will be distracted by the readable content of a page when looking at its layout. The point of using Lorem Ipsum is that it has a more-or-less normal distribution of letters, as opposed to using 'Content here, content here', making it look like readable English. Many desktop publishing packages and web page editors now use Lorem Ipsum as their default model text, and a search for 'lorem ipsum' will uncover many web sites still in their infancy. Various versions have evolved over the years, sometimes by accident, sometimes on purpose (injected humour and the like)</p>" :foreground "#636d77" )]
                                    [(jarman.gui.gui-tools/textarea "<h2>Links</h2>" :foreground "#2c7375" :font (getFont 18)) "align l"] 
                                    [(jarman.gui.gui-tools/textarea "<p align= \"justify\">http://trashpanda-team.ddns.net</p>" :foreground "#636d77") "align l"]
                                    [(jarman.gui.gui-tools/textarea "<h2>FAQ</h2>" :foreground "#2c7375" :font (getFont 18)) "align l"]
                                    [(asks-panel) "align l"]])]])))


(def login-panel (let [flogin (jarman.gui.gui-tools/text-input :placeholder "Login"
                                                               :style [:class :input
                                                                       :columns 20
                                                                       :border (compound-border (empty-border :left 10 :right 10 :top 5 :bottom 5)
                                                                                                (line-border :bottom 4 :color "#96c1ea"))
                                                                       :halign :left
                                                                       :bounds [100 150 200 30]])
                       fpass (jarman.gui.gui-tools/password-input :placeholder "Password"
                                                                  :style [:class :input
                                                                          :columns 20
                                                                          :border (compound-border (empty-border :left 10 :right 10 :top 5 :bottom 5)
                                                                                                   (line-border :bottom 4 :color "#96c1ea"))
                                                                          :halign :left
                                                                          :bounds [100 150 200 30]])]
                   (mig-panel
                    :constraints ["wrap 1" "[grow, center]" "30px[]0px"]
                    :items [
                            [(label :icon (stool/image-scale "resources/imgs/jarman-text.png" 10))]
                            
                            [(mig-panel
                              :constraints ["" "[grow, fill]" "5px[]0px"]
                              :items [
                                      [(label :icon (stool/image-scale icon/user-blue1-64-png 40))]
                                      [flogin]
                                      [(label :border (empty-border :right 20))]
                                      ])]

                            [(mig-panel
                              :constraints ["" "[grow, fill]" "5px[]0px"]
                              :items [
                                      [(label :icon (stool/image-scale icon/key-blue-64-png 40))]
                                      [fpass]
                                      [(label :border (empty-border :right 20))] ])]
                            
                            [(label :text "LOGIN" :background "#fff"
                                    :foreground "#96c1ea"
                                    :border (compound-border (empty-border :bottom 10 :top 10
                                                                           :left 30 :right 30))
                                    :listen [:mouse-entered (fn [e] (config! e :background "#deebf7" :foreground "#256599" :cursor :hand))
                                             :mouse-exited  (fn [e] (config! e :background "#fff" :foreground "#96c1ea"))
                                             :mouse-clicked (fn [e]  (if (authenticate-user (text flogin) (text fpass))
                                                                       (do (config! flogin :border (compound-border (empty-border :left 10
                                                                                                                                  :right 10
                                                                                                                                  :top 5
                                                                                                                                  :bottom 5)
                                                                                                                    (line-border :bottom 4 :color "#96c1ea")))
                                                                           (config! fpass :border (compound-border (empty-border :left 10
                                                                                                                                 :right 10
                                                                                                                                 :top 5
                                                                                                                                 :bottom 5)
                                                                                                                   (line-border :bottom 4 :color "#96c1ea"))))
                                                                       (do (config! flogin :border (compound-border (empty-border :left 10
                                                                                                                                  :right 10
                                                                                                                                  :top 5
                                                                                                                                  :bottom 5)
                                                                                                                    (line-border :bottom 4 :color "#e51a4c")))
                                                                           (config! fpass :border (compound-border (empty-border :left 10
                                                                                                                                 :right 10
                                                                                                                                 :top 5
                                                                                                                                 :bottom 5)
                                                                                                                   (line-border :bottom 4 :color "#e51a4c"))))))])]
                            [(label :text " " :border
                                    (empty-border :top 20 :left 860 )) "split 2"]
                            [(mig-panel
                              :constraints ["" "[grow, fill]" ""]
                              :items [
                                      [(label :icon (stool/image-scale icon/refresh-connection-grey1-64-png 50)
                                              :border (compound-border (empty-border :right 10 )))]
                                      [(label :icon (stool/image-scale icon/settings-64-png 50)
                                              :border (compound-border (empty-border :right 10 )))]
                                      [(label :icon (stool/image-scale icon/I-grey-64-png 50)
                                              :listen [:mouse-clicked (fn [e] (config! f :content info-panel))])]])]])))

(def f (frame :title "Jarman-login"
              :undecorated? false
              :resizable? false
              :minimum-size [1000 :by 760]
              :content login-panel))

(-> (doto f (.setLocationRelativeTo nil) seesaw.core/pack! seesaw.core/show!))




