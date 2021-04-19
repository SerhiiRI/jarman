(defproject jarman "1.0.0"
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
               ;; [org.odftoolkit/odfdom-java "0.8.7"]
               ;; [org.odftoolkit/odfdom-java "0.9.0-RC1"]
               ;; [org.odftoolkit/simple-odf "0.9.0-RC1"]
                 [kaleidocs/merge "0.3.0"]
                 [org.clojure/java.jdbc "0.7.10"]
                 [me.raynes/fs "1.4.6"]
                 [mysql/mysql-connector-java "5.1.6"]
                 [instaparse "1.4.10"]]
  :main ^:skip-aot jarman.core
  :repl-options {:init-ns hrtime.core}
  :target-path "target/%s"
  :aliases  {"jarman" ["run" "-m" "jarman.jarman-cli"]}
  :profiles {;; :uberjar {:aot :all}
             ;;:dev {;; :dependencies [[autodoc "1.1.2"]]
             ;;      :plugins [[funcool/codeina "0.4.0" :exclusions [org.clojure/clojure]]]}
             :lets-scheme {:aot [jarman.schema-builder
                                 jarman.sql-tool]
                           :main jarman.schema-builder
                           :jar-name "lets-scheme-lib.jar"
                           :uberjar-name "lets-scheme.jar"}}
  :codeina {:sources ["src"]
            :reader :clojure}
  ;; :plugins [[funcool/codeina "0.5.0"]]
  :jar-name "jarman.jar"
  :uberjar-name "jarman-standalone.jar"
  :java-source-paths ["src/java"]
  :javac-options     ["-Xlint:unchecked"])



