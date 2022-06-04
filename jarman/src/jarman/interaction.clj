;;  _       _                      _   _             
;; (_)_ __ | |_ ___ _ __ __ _  ___| |_(_) ___  _ __  
;; | | '_ \| __/ _ \ '__/ _` |/ __| __| |/ _ \| '_ \ 
;; | | | | | ||  __/ | | (_| | (__| |_| | (_) | | | |
;; |_|_| |_|\__\___|_|  \__,_|\___|\__|_|\___/|_| |_|
;; =================================================

(ns jarman.interaction
  (:require
   [potemkin.namespaces :refer [import-vars]]
   [jarman.gui.gui-alerts-service]
   [jarman.gui.gui-views-service]
   [jarman.gui.gui-editors]
   [jarman.gui.gui-components]
   [jarman.gui.gui-tools]
   [jarman.gui.components.swing-keyboards]
   [jarman.application.collector-custom-metacomponents]
   [jarman.application.collector-custom-view-plugins]
   [jarman.application.collector-custom-themes]))

(import-vars
  [jarman.config.conf-language
   plang lang]
  
  [jarman.gui.components.swing-keyboards
   kbd kvc global-set-key define-key]

  [jarman.gui.gui-alerts-service
   success danger warning info
   delay-alert show-delay-alerts show-alerts-history]

  [jarman.gui.gui-views-service
   restart soft-restart hard-restart reload-view]

  [jarman.gui.gui-components
   open-doom hide-doom rm-doom]

  [jarman.gui.gui-editors
   editor]

  [jarman.gui.faces-system
   custom-theme-set-faces]

  [jarman.application.collector-custom-view-plugins
   register-view-plugin]
  
  [jarman.application.collector-custom-themes
   register-theme]

  [jarman.application.collector-custom-metacomponents
   metacomponent->component
   register-metacomponent
   metacomponents-get])


;;  ____  _____ __  __  ___
;; |  _ \| ____|  \/  |/ _ \
;; | | | |  _| | |\/| | | | |
;; | |_| | |___| |  | | |_| |
;; |____/|_____|_|  |_|\___/
;; =========================
;; --- clean-up later ---

(comment
  (info    "Test 1" "Info box")
  (warning "Test 2" "Warning box")
  (danger  "Test 3" "Danger box")
  (success "Test 4" "Success box")
  (warning "Interaction" "Devil robot say:" :s-popup [300 150]
           :expand (fn [] (jarman.gui.gui-components/button-basic "Kill all humans!")))
  (show-alerts-history)

  (delay-alert "TEMP1" "It'a an TEMP alert invoking later.")
  (delay-alert "TEMP2" "It'a an TEMP alert invoking later.")
  (show-delay-alerts))

(comment
  (editor "./test/test-file.txt" :clojure)) 

(comment
  (open-doom (seesaw.core/label :text "Pepe Dance"))
  (hide-doom)
  (open-doom)
  (rm-doom))

(comment
  (global-set-key (kbd "M-x") (fn []
                                (println "Open doom")
                                (open-doom
                                 (jarman.gui.components.listbox/select-panel
                                  :string-list
                                  (map str (.listFiles (clojure.java.io/file jarman.config.environment/user-home)))
                                  :on-select (fn [t] (println "CHOSE=> " t))))))
  (global-set-key (kbd "C-g") (fn []
                                (println "Remove doom")
                                (rm-doom))))

