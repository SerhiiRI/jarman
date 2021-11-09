(defproject jarman "0.0.1"
  :description "Jarman"
  :license {:name "EPL-2.0" :url "https://www.eclipse.org/legal/epl-2.0/"}
   :dependencies [;; core
                 [org.clojure/clojure "1.10.1"]
                 [org.clojure/core.async "1.3.618"]
                 [org.clojure/data.csv "1.0.0"]
                 [org.clojure/data.json "2.4.0"]
                 [org.clojure/data.xml "0.0.8"]
                 [org.clojure/java.jdbc "0.7.10"]
                 [org.clojure/tools.cli "1.0.194"]
                 ;; gui
                 [com.github.jiconfont/jiconfont-google_material_design_icons "2.2.0.2"]
                 [com.github.jiconfont/jiconfont-swing "1.0.1"]
                 [com.github.vlsi.mxgraph/jgraphx "4.2.2"]
                 [seesaw "1.5.0"]
                 ;; other
                 [buddy/buddy-core "1.10.1"]
                 [kaleidocs/merge "0.3.0"]
                 [me.raynes/fs "1.4.6"]
                 [mysql/mysql-connector-java "5.1.6"]
                 [rewrite-clj "1.0.682-alpha"]
                 [com.velisco/clj-ftp "0.3.12"]
                 [datascript-transit/datascript-transit "0.3.0"]
                 [datascript/datascript "1.2.1"]
                 [com.github.seancorfield/honeysql "2.1.818"]
                 [fr.opensagres.xdocreport/fr.opensagres.xdocreport.document.ods "1.0.3"]
                 [fr.opensagres.xdocreport/fr.opensagres.xdocreport.document.odt "1.0.3"]
                 ;; tools
                 [incanter "1.9.3" :exclusions [org.bouncycastle/bctsp-jdk14
                                                org.clojure/core.incubator
                                                mysql/mysql-connector-java]]
                 [instaparse "1.4.10"]
                 [clj-fuzzy "0.4.1"]
                 [de.ubercode.clostache/clostache "1.4.0"]]
  ;;:main ^:skip-aot jarman.core
  :main jarman.core
  :aot [jarman.core]
  :repl-options {:init-ns jarman.core}
  :target-path "target/%s"
  :aliases  {"jarman" ["run" "-m" "jarman.jarman-cli"]}
  :jar-name "jarman.jar"
  ;;:uberjar {:aot :all}
  :uberjar-name "jarman-standalone.jar"
  :java-source-paths ["src/java"]
  :javac-options     ["-Xlint:unchecked"]
  :jvm-options ["-Dswing.aatext=true"
                "-Dawt.useSystemAAFontSettings=on"
                "-Dsun.java2d.xrender=true"]
  :profiles {:user
             {:plugins [[lein-launch4j "0.1.2"]]
              :launch4j-install-dir "../installer"
              :launch4j-config-file "../installer/launch4j.xml"}
             :cli
             {:aot [jarman.jarman-cli
                    jarman.managment.data
                    jarman.config.storage
                    jarman.tools.ftp-toolbox]
              :main jarman.jarman-cli
              :jar-name "jarman-cli-lib.jar"
              :uberjar-name "jarman-cli.jar"}
             :client
             {:main jarman.gui.gui-login
              :jar-name "jarman-client-lib.jar"
              :uberjar-name "jarman-client-standalone.jar"}
             ;; :cider
             ;; {:dependencies [[cider/cider-nrepl "0.26.0"]]
             ;;  :repl-options {:nrepl-middleware
             ;;                 [cider.nrepl/wrap-apropos
             ;;                  cider.nrepl/wrap-classpath
             ;;                  cider.nrepl/wrap-clojuredocs
             ;;                  cider.nrepl/wrap-complete
             ;;                  cider.nrepl/wrap-debug
             ;;                  cider.nrepl/wrap-format
             ;;                  cider.nrepl/wrap-info
             ;;                  cider.nrepl/wrap-inspect
             ;;                  cider.nrepl/wrap-macroexpand
             ;;                  cider.nrepl/wrap-ns
             ;;                  cider.nrepl/wrap-spec
             ;;                  cider.nrepl/wrap-profile
             ;;                  cider.nrepl/wrap-refresh
             ;;                  cider.nrepl/wrap-resource
             ;;                  cider.nrepl/wrap-stacktrace
             ;;                  cider.nrepl/wrap-test
             ;;                  cider.nrepl/wrap-trace
             ;;                  cider.nrepl/wrap-out
             ;;                  cider.nrepl/wrap-undef
             ;;                  cider.nrepl/wrap-version
             ;;                  cider.nrepl/wrap-xref]}}
             })



