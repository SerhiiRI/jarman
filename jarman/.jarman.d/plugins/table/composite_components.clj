(ns jarman.plugins.composite-components
  (:require
   [clojure.string :as s]
   [jarman.gui.gui-tools :as gtool]
   [jarman.gui.gui-components :as gcomp]
   [jarman.logic.composite-components :as ccomp]))

(defn build-test-panel [data-vec onClick]
  (let [mig (seesaw.mig/mig-panel
             :background  "#e4e4e4"
             :constraints ["wrap 2" "10px[95:, fill, grow]10px" "10px[]10px"])]
    (doall (map (fn [item] (.add mig item)) data-vec))
    ;;(.add mig (gcomp/button-slim "set data" :onClick onClick) "span 2, align r")
    (.revalidate mig)
    (.repaint mig)
    mig))

(defn link-test   [state-changer link-defr]
  (let [label-fn   (fn [text] (seesaw.core/label :text text :font (gtool/getFont)))
        title-txt  (gcomp/state-input-text {:func (fn [e]) :val (:text link-defr)})
        url-txt    (gcomp/state-input-text {:func (fn [e]) :val (:link link-defr)})]
    (fn [e] (state-changer (ccomp/->Link
                            (seesaw.core/text title-txt)
                            (seesaw.core/text url-txt))))
    (seesaw.mig/mig-panel
     :background  "#e4e4e4"
     :constraints ["wrap 1" "10px[95:, fill, grow]10px" "10px[]10px"]
     :items [[(label-fn "text:")] [title-txt] [(label-fn "url:")] [url-txt]])))

(defn file-test [state-changer file-defr]
  (let [label-fn   (fn [text] (seesaw.core/label :text text :font (gtool/getFont)))
        input-fn   (fn [defr-key] (gcomp/state-input-text {:func (fn [e]) :val (defr-key file-defr)}))  
        name-txt   (input-fn :file-name)
        path-txt   (gcomp/status-input-file {:func (fn [e])
                                             :val (:file-path file-defr)})
        panel     (build-test-panel [(label-fn "file-name:") (label-fn "file-path:") name-txt path-txt]
                                    (fn [e] ;; (state-changer (ccomp/->File
                                            ;;                 (seesaw.core/text name-txt)
                                            ;;                 (seesaw.core/text path-txt)))
                                      ))]
    (seesaw.core/config! path-txt :listen [:property-change (fn [e]
                                                              (let [file-name (.getName (clojure.java.io/file (.getToolTipText path-txt) #"/"))]
                                                                (if-not (empty? file-name)
                                                                  (do (seesaw.core/config! name-txt :text file-name)
                                                                      (.revalidate panel)
                                                                      (.repaint panel))
                                                                  (seesaw.core/config! name-txt :text ""))))]) panel))

(defn ftp-panel [state-changer ftp-defr]
  (let [label-fn   (fn [text] (seesaw.core/label :text text :font (gtool/getFont)))
        input-fn   (fn [defr-key] (gcomp/state-input-text {:func (fn [e]) :val (defr-key ftp-defr)}))
        login-txt  (input-fn :login)
        passw-txt  (input-fn :password)
        name-txt   (input-fn :file-name)
        path-txt   (gcomp/status-input-file {:func (fn [e])
                                             :val (:file-path ftp-defr)})
        panel     (build-test-panel [(label-fn "login:") (label-fn "password:") login-txt passw-txt
                                     (label-fn "file-name:") (label-fn "file-path:") name-txt path-txt]
                                    (fn [e] ;; (state-changer (ccomp/->FtpFile
                                            ;;                 (seesaw.core/text login-txt)
                                            ;;                 (seesaw.core/text passw-txt) ;;change logic to state
                                            ;;                 (seesaw.core/text name-txt)
                                            ;;                 (seesaw.core/text path-txt)))
                                      ))]
    (seesaw.core/config! path-txt :listen [:property-change (fn [e]
                                                              (let [file-name  (last (s/split (.getToolTipText path-txt) #"/"))]
                                                                (if-not (empty? file-name)
                                                                  (do (seesaw.core/config! name-txt :text file-name)
                                                                      (.revalidate panel)
                                                                      (.repaint panel))
                                                                  (seesaw.core/config! name-txt :text ""))))]) panel)) ;;TO DO change logic to state

(comment (let [state (atom {})
               mig   (fn [items](seesaw.core/scrollable (seesaw.mig/mig-panel
                                                         :constraints ["wrap 1" "150px[]0px" "100px[]0px"]
                                                         :items [[items]])))
               ;; panel-file (mig (file-test  #(swap! state (fn [s] (assoc-in s [:ftp-defr %])) [])
               ;;                                                        (File. "panda" "some-path/heyy/file.txt")))
               ;; panel-link (mig (link-test #(swap! state (fn [s] (assoc-in s [:link-defr %]))  [])
               ;;                            (Link. "Just click" "https://www.yoyube")))
               panel-ftp  (mig (ftp-panel  #(swap! state (fn [s] (assoc-in s [:ftp-defr %])) [])
                                                                      (ccomp/->FtpFile "trashpanda" "123" "panda" "some-path/heyy/file.txt")))]
           (-> (doto (seesaw.core/frame
                      :title "DEBUG WINDOW" :undecorated? false
                      :minimum-size [450 :by 450]
                      :size [450 :by 450]
                      :content panel-ftp
                     ;; :content panel-ftp
                      )
                 (.setLocationRelativeTo nil) seesaw.core/pack! seesaw.core/show!))))


 ;; :content (link-test #(swap! state (fn [s] (assoc-in s [:link-defr %]))  [])
                      ;;                           (Link. "Just click" "https://www.youtube.com/"))
(seesaw.dev/show-events (seesaw.core/label))

