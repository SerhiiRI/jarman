(ns jarman.gui.gui-login
  (:use seesaw.core
        seesaw.border
        seesaw.dev
        seesaw.mig
        seesaw.font)
  (:require [clojure.string :as string]
            [jarman.tools.swing :as stool]
            [jarman.gui.gui-tools :refer :all]
            [jarman.resource-lib.icon-library :as icon]))


(def validate1 {:validate? false :output {:description "DB connection is not set DB connection is not set DB connection is not set DB connection is not set"}})
(def validate2 {:validate? false :output {:description "some description some description some description"
                                          :faq [{:q "Why i saw this error"
                                                 :a "Because your program has trouble"}
                                                {:q "What it problem mean"
                                                 :a "Read the fucking descriptin"}]}})
(def validate3 {:validate? true})
(def validate4 {:validate? true})

(defn- validation []
  (cond 
    (not (:validate? validate1)) (:output validate1)
   ;; (not (:validate? validate2)) (:output validate2)
    (not (:validate? validate3)) (:output validate3)
    (not (:validate? validate4)) (:output validate4)
    :else nil))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; style color and font ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def dark-grey-color "#676d71")
(def light-grey-color "#82959f")
(def blue-green-color "#2c7375")
(def light-blue-color "#96c1ea")
(def red-color "#e51a4c")

(defn myFont [size]
  {:size size :font "Arial" :style :bold})

(declare asks-panel)
(declare login-panel)

(defn- authenticate-user [login password]
  (if (and (= login "admin")(= password "admin")) true))

(def ^:private emp-border (empty-border :left 10 :right 10 :top 5 :bottom 5))

(defn- some-text [color]
  (label :text (htmling "<p align= \"justify\">It is a long established fact that a reader will be distracted by the readable content of a page when lookiult model text, and a search for 'lorem ipsum' will uncover many web sites still in their infancy. Various versions have evolved over the years, sometimes by accident, sometimes on purpose (injected humour and the like)</p>") :foreground color :font (myFont 14)))

(def ^:private contact-info [(label :text (htmling "<h2>Contacts</h2>") :foreground blue-green-color :font (myFont 14))
                             (horizontal-panel :items (list (label :text (htmling "<h3>Website:</h3>")
                                                                   :foreground  dark-grey-color :font (myFont 16)
                                                                   :border (empty-border :right 8))
                                                            (label :text (htmling "<h3>http://trashpanda-team.ddns.net</h3>")
                                                                   :foreground  light-grey-color :font (myFont 16))))
                             (horizontal-panel :items (list (label :text (htmling "<h3>Phone:</h3>")
                                                                   :foreground  dark-grey-color :font (myFont 16)
                                                                   :border (empty-border :right 8))
                                                            (label :text (htmling "<h3>+380966085615</h3>")
                                                                   :foreground  light-grey-color :font (myFont 16))))
                             (horizontal-panel :items (list (label :text (htmling "<h3>Email:</h3>")
                                                                   :foreground  dark-grey-color :font (myFont 16)
                                                                   :border (empty-border :right 8))
                                                            (label :text (htmling "<h3>contact.tteam@gmail.com</h3>")
                                                                   :foreground  light-grey-color :font (myFont 16))))])


(defn- error-panel [errors]
  (scrollable
   (mig-panel
    :constraints ["wrap 1" "[grow, center]" "20px[]20px"]
    :items [[(label :icon (stool/image-scale icon/alert-red-512-png 8)
                    :border (empty-border :right 10)) "split 2"]
            [(label :text (htmling "<h2>ERROR</h2>") :foreground blue-green-color :font (myFont 14))]
            [(let [mig (mig-panel
                        :constraints ["wrap 1" "40px[:600, grow, center]40px" "10px[]10px"]
                        :items [[(label :text (htmling "<h2>About</h2>")
                                        :foreground blue-green-color :font (myFont 14)) "align l"]
                                [(label :text (htmling (str "<p align= \"justify\">" (:description errors) "</p>")) :foreground light-grey-color
                                        :font (myFont 14)) "align l"]])]               
               (doall (map (fn [x] (.add mig x "align l")) (if (= (:faq errors) nil) contact-info (concat (asks-panel (:faq errors)) contact-info))))
               (.repaint mig) mig)]])
   :hscroll :never))


(defn- asks-panel [faq]
  (let [mig
        (mig-panel
         :constraints ["wrap 1" "20px[grow, center]" "15px[]15px"]
         :items [])]
    (doall (map (fn [x] (do (.add mig (label :text (str "- " (:q x)) :foreground blue-green-color :font (myFont 14)) "align l")
                            (.add mig (label :text (:a x) :foreground light-grey-color :font (myFont 14)) "align l"))) faq))
    (.repaint mig)
    [(label :text (htmling "<h2>FAQ</h2>")
            :foreground blue-green-color
            :font (myFont 14)) mig]))


(def ^:private info-panel
  (let [info some-text
        faq [{:q "Why i saw this error"
              :a "Because your program has trouble"}
             {:q "What it problem mean"
              :a "Read the fucking descriptin"}]]
    (scrollable
     (mig-panel
      :constraints ["wrap 1" "[grow, center]" "20px[]20px"]
      :items [[(label :icon (stool/image-scale icon/left-blue-64-png 50)
                      :listen [:mouse-clicked (fn [e] (config! (to-frame e) :content login-panel))]
                      :border (empty-border :right 220)) "align l, split 2"]
              [(label :icon (stool/image-scale "resources/imgs/trashpanda2-stars-blue-1024.png" 47))]
              [(let [mig (mig-panel
                          :constraints ["wrap 1" "100px[:600, grow, center]100px" "20px[]20px"]
                          :items [[(label :text (htmling "<h2>About</h2>")
                                          :foreground blue-green-color :font (myFont 14)) "align l"]
                                  [(info  light-grey-color)]
                                  [(label :text (htmling "<h2>Jarman</h2>")
                                          :foreground blue-green-color :font (myFont 14)) "align l"]
                                  [(info  light-grey-color)]
                                  [(label :text (htmling "<h2>Contact</h2>")
                                          :foreground blue-green-color :font (myFont 14)) "align l"]
                                  [(info  light-grey-color)]
                                  [(label :text (htmling "<h2>Links</h2>")
                                          :foreground blue-green-color :font (myFont 14)) "align l"] 
                                  [(label :text (htmling "<p align= \"justify\">http://trashpanda-team.ddns.net</p>")
                                          :foreground light-grey-color :font (myFont 14) ) "align l"]])]
                 (doall (map (fn [x] (.add mig x "align l")) (if (= faq nil)
                                                               contact-info (concat (asks-panel faq) contact-info))))
                 (.repaint mig) mig)]]))))


(def ^:private login-panel
  (let [flogin (jarman.gui.gui-tools/text-input :placeholder "Login"
                                                :style [:class :input
                                                        :columns 20
                                                        :border (compound-border emp-border
                                                                                 (line-border :bottom 4 :color light-blue-color))
                                                        :halign :left
                                                        :bounds [100 150 200 30]])
        fpass (jarman.gui.gui-tools/password-input :placeholder "Password"
                                                   :style [:class :input
                                                           :columns 20
                                                           :border (compound-border emp-border
                                                                                    (line-border :bottom 4 :color light-blue-color))
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
                     :foreground light-blue-color
                     :border (compound-border emp-border)
                     :listen [:mouse-entered (fn [e] (config! e :background "#deebf7" :foreground "#256599" :cursor :hand))
                              :mouse-exited  (fn [e] (config! e :background "#fff" :foreground light-blue-color))
                              :mouse-clicked (fn [e]  (if (authenticate-user (text flogin) (text fpass))
                                                        (do (config! flogin :border (compound-border emp-border
                                                                                                     (line-border :bottom 4 :color light-blue-color)))
                                                            (config! fpass :border (compound-border emp-border
                                                                                                    (line-border :bottom 4 :color light-blue-color))))
                                                        (do (config! flogin :border (compound-border emp-border
                                                                                                     (line-border :bottom 4 :color red-color)))
                                                            (config! fpass :border (compound-border emp-border
                                                                                                    (line-border :bottom 4 :color red-color))))))])]
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
                               :listen [:mouse-clicked (fn [e] (config! (to-frame e) :content info-panel))])]])]])))

(defn- frame-login []
  (frame :title "Jarman-login"
         :undecorated? false
         :resizable? false
         :minimum-size [1000 :by 760]
         :content login-panel))

(defn- frame-error []
  (frame :title "Jarman-error"
         :undecorated? false
         :resizable? false
         :minimum-size [600 :by 600]))

(defn start []
  (let [res-validation (validation)]
    (if (= res-validation nil)
      (-> (doto (frame-login) (.setLocationRelativeTo nil)) seesaw.core/pack! seesaw.core/show!)
      (-> (doto (frame-error) (.setLocationRelativeTo nil)) (config! :content (error-panel res-validation)) seesaw.core/pack! seesaw.core/show!))))

;;(start)









