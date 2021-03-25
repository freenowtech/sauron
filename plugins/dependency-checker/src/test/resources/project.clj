(defproject com.freenow.test "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [compojure "1.6.1"]
                 [cyrus/config "0.2.1"]
                 [mount "0.1.12"]
                 [com.layerware/hugsql "0.4.8"]
                 [org.clojure/java.jdbc "0.7.5"]
                 [com.taoensso/timbre "4.10.0"]
                 [com.fzakaria/slf4j-timbre "0.3.8"]
                 [org.clojure/core.cache "0.7.1"]
                 [cheshire "5.8.1"]
                 [nrepl/nrepl "0.7.0"]
                 [org.postgresql/postgresql "42.2.5"]
                 [ragtime "0.8.0"]
                 [com.facebook.presto/presto-jdbc "0.215"
                  :exclusions  [[org.slf4j/slf4j-log4j12]
                                [org.apache.logging.log4j/log4j-slf4j-impl]
                                [log4j/log4j]]]
                 [aleph "0.4.6"]
                 [ring "1.6.3"]
                 [ring/ring-json "0.4.0"]
                 [com.soundcloud/prometheus-clj "2.4.1"]
                 [ring-basic-authentication "1.0.5"]]

  :main com.freenow.test.core
  :uberjar-name "com.freenow.test.jar"
  :profiles {:dev
             {:source-paths ["dev"]
              :repl-options {:init-ns user}
              :plugins
              [[lein-ancient "0.6.15"]
               [lein-kibit "0.1.5"]
               [jonase/eastwood "0.2.5"]]}})