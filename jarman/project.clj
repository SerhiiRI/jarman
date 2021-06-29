(defproject jarman "0.0.1"
  :description "Jarman"
  :license {:name "EPL-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/tools.cli "1.0.194"]
                 [seesaw "1.5.0"]
                 [org.clojars.mjdowney/excel-clj "1.2.0"]
                 [com.velisco/clj-ftp "0.3.12"]
                 [fr.opensagres.xdocreport/fr.opensagres.xdocreport.document.odt "1.0.3"]
                 [fr.opensagres.xdocreport/fr.opensagres.xdocreport.document.ods "1.0.3"]
                 [com.github.vlsi.mxgraph/jgraphx "4.2.2"]
                 ;; [jgraphx/jgraphx "3.4.1.3"]
                 ;; [jgraphx/jgraphx "1.8.0.3"]
                 ;; [jgraphx/jgraphx "4.2.2"]
                 ;; [org.odftoolkit/odfdom-java "0.8.7"]
                 ;; [org.odftoolkit/odfdom-java "0.9.0-RC1"]
                 ;; [org.odftoolkit/simple-odf "0.9.0-RC1"]
                 [datascript/datascript "1.2.1"]
                 [ont-app/datascript-graph "0.1.0"]
                 [datascript-transit/datascript-transit "0.3.0"]
                 [rum/rum "0.12.6"]
                 [kaleidocs/merge "0.3.0"]
                 [org.clojure/java.jdbc "0.7.10"]
                 [me.raynes/fs "1.4.6"]
                 [mysql/mysql-connector-java "5.1.6"]
                 [instaparse "1.4.10"]]
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
  :profiles {:user
             {:plugins [[lein-launch4j "0.1.2"]]
              :launch4j-install-dir ""
              :launch4j-config-file "resources/config.xml"}

             :lets-scheme
             {:aot [jarman.jarman-cli
                    jarman.cli.cli-tool
                    jarman.logic.sql-tool
                    jarman.managment.db-managment
                    jarman.tools.ftp-toolbox]
              :main jarman.jarman-cli
              :jar-name "lets-scheme-lib.jar"
              :uberjar-name "lets-scheme.jar"}
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



