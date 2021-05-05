(ns jarman.gui.gui-calendar
  (:use seesaw.core
        seesaw.border
        seesaw.dev
        seesaw.style
        seesaw.mig
        seesaw.font)
  (:import (java.text SimpleDateFormat))
  (:require [clojure.string :as string]
            [jarman.gui.gui-tools :refer :all :as gtool]
            [jarman.gui.gui-components :refer :all :as gcomp]))

(import 'jarman.test.Chooser)


(defn date
  "Remember that simple (date) ruturn current
  date and time.
  Also if you 

  Example:
  (date 1900 11 29 1 2 3) => 1900-12-29 01:02:03
  (date 1900 11 29 1 2)   => 1900-12-29 01:02:00
  (date 1900 11 29 1)     => 1900-12-29 01:00:00
  (date 1900 11 29)       => 1900-12-29 00:00:00
  (date 1900 11)          => 1900-12-01 00:00:00
  (date 1900)             => 1900-01-01 00:00:00
  (date)                  => 2020-02-29 19:07:41"
  ([] (.format (java.text.SimpleDateFormat. "YYYY-MM-dd") (java.util.Date.)))
  ([YYYY] (date YYYY 0 1 0 0 0))
  ([YYYY MM] (date YYYY MM 1 0 0 0))
  ([YYYY MM dd] (date YYYY MM dd 0 0 0))
  ([YYYY MM dd hh] (date YYYY MM dd hh 0 0))
  ([YYYY MM dd hh mm] (date YYYY MM dd hh mm 0))
  ([YYYY MM dd hh mm ss] (.format (java.text.SimpleDateFormat. "YYYY-MM-dd")
                                  (java.util.Date. (- YYYY 1900) MM dd hh mm ss))))

(defn date-to-obj
  "Make shure, that your date in *data-format*

  Example: 
    (date-to-obj \"1998-10-10 05:11:22\") ;=> java.util.Date...."
  [^java.lang.String data-string]
  (.parse (SimpleDateFormat. "yyyy-MM-dd") data-string))


;; (def get-text-field 
;;   (fn [data] (text :text data
;;                    :background "#fff"
;;                    :columns 20
;;                    :border (compound-border (empty-border :left 10 :right 10 :top 5 :bottom 5)
;;                                             (line-border :bottom 2 :color "#444444")))))


(defn get-calendar
  "Set textfield, get datepicker

  Example:
      (Chooser/get_calendar Textfield) :=> obj..text_field... "
  [textf]
  (Chooser/get_calendar textf))

(defn calendar-panel
  [& {:keys [set-date
             editable?
             enabled?
             ]
      :or {editable? false
           enabled? true
           set-date (date)}}]
  (get-calendar (gcomp/input-text :v set-date :args [:enabled? enabled? :editable? editable?])))

(defn calendar-with-atom
  [& {:keys [local-changes
             field
             set-date
             editable?
             enabled?]
      :or {local-changes (atom {})
           field nil
           set-date nil
           editable? false
           enabled? true}}]
  (get-calendar (gcomp/input-text
                 :v set-date
                 :args [:enabled? enabled?
                        :editable? editable?
                        :listen [:caret-update (fn [e]
                                                 (if-not (nil? field) 
                                                   (swap! local-changes (fn [storage] (assoc storage (keyword field) (value (to-widget e)))))))]])))

;; (defn- frame-calendar []
;;   (frame :title "Jarman"
;;          :undecorated? false
;;          :resizable? false
;;          :minimum-size [1000 :by 760]
;;          :content (mig-panel
;;                    :constraints ["wrap 1" "100px[grow, center]100px" "30px[]30px"]
;;                    :items [[(get-calendar (text :columns 20))]])))

;;  (-> (doto (frame-calendar) (.setLocationRelativeTo nil)) seesaw.core/pack! seesaw.core/show!)


