(defproject jarman "1.0.0"
  :description "Jarman"
  :license {:name "EPL-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/tools.cli "1.0.194"]
                 [seesaw "1.5.0"]
                 [org.clojars.mjdowney/excel-clj "1.2.0"]
                 [com.velisco/clj-ftp "0.3.12"]
                 [org.clojure/java.jdbc "0.7.10"]
                 [me.raynes/fs "1.4.6"]
                 [mysql/mysql-connector-java "5.1.6"]]
  :main ^:skip-aot jarman.core
  :repl-options {:init-ns hrtime.core}
  :target-path "target/%s"
  :aliases  {"lets-scheme" ["run" "-m" "jarman.schema-builder"]}
  
  :profiles {;; :uberjar {:aot :all}
             :lets-scheme {:aot [jarman.schema-builder
                                 jarman.sql-tool]
                           :main jarman.schema-builder
                           :jar-name "lets-scheme-lib.jar"
                           :uberjar-name "lets-scheme.jar"}}
  :jar-name "jarman.jar"
  :uberjar-name "jarman-standalone.jar"
  :java-source-paths ["src/java-src"]
  :javac-options     ["-Xlint:unchecked"])



