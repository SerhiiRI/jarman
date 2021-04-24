(ns jarman.gui.gui-calendar
  (:use seesaw.core
        seesaw.border
        seesaw.dev
        seesaw.style
        seesaw.mig
        seesaw.font)
  (:import (java.text SimpleDateFormat))
  (:require [clojure.string :as string]
            [jarman.gui.gui-tools :refer :all]))

(import 'jarman.test.Chooser)


(defn date-to-obj
  "Make shure, that your date in *data-format*

  Example: 
    (date-to-obj \"1998-10-10 05:11:22\") ;=> java.util.Date...."
  [^java.lang.String data-string]
  (.parse (SimpleDateFormat. "yyyy-MM-dd") data-string))


(def get-text-field 
  (fn [data] (text :text data
                   :background "#fff"
                   :columns 20
                   :border (compound-border (empty-border :left 10 :right 10 :top 5 :bottom 5)
                                            (line-border :bottom 2 :color "#444444")))))


(defn get-calendar
  "Set textfield, get datepicker

  Example:
      (Chooser/get_calendar textfield date_obj) :=> obj..text_field... "
  [textf]
  (Chooser/get_calendar textf (date-to-obj (text textf))))


(def calendar-panel
  (mig-panel
   :constraints ["wrap 1" "100px[grow, center]100px" "30px[]30px"]
   :items [[(get-calendar (get-text-field "2019-10-10"))]]))


(defn- frame-calendar []
  (frame :title "Jarman"
         :undecorated? false
         :resizable? false
         :minimum-size [1000 :by 760]
         :content calendar-panel))

 (-> (doto (frame-calendar) (.setLocationRelativeTo nil)) seesaw.core/pack! seesaw.core/show!)

