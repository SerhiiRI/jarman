(ns jarman.gui.gui-events
  (:require
   [jarman.lang :refer :all]
   [jarman.org  :refer :all]
   [jarman.application.session :as session]
   [jarman.gui.gui-tools :as gtool]
   [jarman.gui.gui-alerts-service :as gas])
  (:import
   [java.util Date]
   [java.text ParseException SimpleDateFormat]))

(defn calc-date-distance [^Date start-date ^Date end-date]
  (let [diff (- (.getTime end-date) (.getTime start-date))]
    (/ diff (* 24 60 60 1000))))

(defn gui-check-license []
  (where
   ((formater (SimpleDateFormat. "dd-MM-yyyy"))
    (license  (.get-license (session/session))))
   (cond
     ;;-------
     (nil? license)
     (do (gas/alert (gtool/get-lang :header :licenses) (gtool/get-lang :license :license-not-found) :type :danger :time 10)
         (.start (Thread.
                  (fn [] (Thread/sleep 5000)
                    (print-header
                     "License aren't registred!"
                     (print-line (gtool/get-lang :license :license-not-found))
                     (print-line "Logout from the Jarman"))))))
     ;;-------
     (some? license)
     (where
      ((license-end  (.parse formater (:expiration-date license)))
       (current-date (Date.))
       (days         (calc-date-distance current-date license-end) do int)
       (outdated?    (> 0 days))
       (message
        (cond
          (< 30 days)  nil
          (and (> 30 days) (< 16 days)) (format (gtool/get-lang-license :license-expire-day-30) days)
          (< 15 days) (format (gtool/get-lang-license :license-expire-day-15) days)
          (< 14 days) (format (gtool/get-lang-license :license-expire-day-14) days)
          (< 13 days) (format (gtool/get-lang-license :license-expire-day-13) days)
          (< 12 days) (format (gtool/get-lang-license :license-expire-day-12) days)
          (< 11 days) (format (gtool/get-lang-license :license-expire-day-11) days)
          (< 10 days) (format (gtool/get-lang-license :license-expire-day-10) days)
          (< 9 days)  (format (gtool/get-lang-license :license-expire-day-9)  days)
          (< 8 days)  (format (gtool/get-lang-license :license-expire-day-8)  days)
          (< 7 days)  (format (gtool/get-lang-license :license-expire-day-7)  days)
          (< 6 days)  (format (gtool/get-lang-license :license-expire-day-6)  days)
          (< 5 days)  (format (gtool/get-lang-license :license-expire-day-5)  days)
          (< 4 days)  (format (gtool/get-lang-license :license-expire-day-4)  days)
          (< 3 days)  (format (gtool/get-lang-license :license-expire-day-3)  days)
          (< 2 days)  (format (gtool/get-lang-license :license-expire-day-2)  days)
          (< 1 days)  (format (gtool/get-lang-license :license-expire-day-1)  days)
          (= 0 days)  (format (gtool/get-lang-license :license-expire-day-0)  days)
          outdated?   (format (gtool/get-lang-license :license-outdated)      days))))
      (when message (gas/alert  (gtool/get-lang :header :licenses) message :type (if outdated? :danger :warning) :time 10))
      (when outdated?
        (.start (Thread. (fn [] (Thread/sleep 5000)
                           (print-header
                            "License is outdatadet"
                            (print-line message)
                            (print-line "Logout from the Jarman"))))))))))

