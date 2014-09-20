(defproject twenty48 "0.1.0-SNAPSHOT"
  :description "2048 clone with TinCan API leaderboard"
  :url "http://hewittsoft.com/2048.html"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :scm {:url "git@github.com/dweebo/cljs-2048.git"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2268"]
                 [ring "1.2.1"]
                 [cljs-uuid "0.0.4"]
                 [cljs-http "0.1.16"]
                 [com.andrewmcveigh/cljs-time "0.1.6"]
                 [prismatic/dommy "0.1.3"]]
  :plugins [[lein-cljsbuild "1.0.3"]
            [lein-ring "0.8.10"]
            [com.cemerick/clojurescript.test "0.3.1"]]
  :hooks [leiningen.cljsbuild]
  :source-paths ["src/clj"]
  :cljsbuild {
    :builds {
      :main {
        :source-paths ["src/cljs"]
        :compiler {:output-to "resources/public/js/2048.js"
                   :optimizations :simple
                   :pretty-print true
                   :foreign-libs [{:file "resources/lib/viewportSize-min.js" :provides ["viewportSize"]}
                                  {:file "resources/lib/Tocca.min.js" :provides ["tocca"]}]}}
      :advanced {
        :source-paths ["src/cljs"]
        :compiler {:output-to "resources/public/js/2048-min.js"
                   :optimizations :advanced
                   :pretty-print false
                   :foreign-libs [{:file "resources/lib/viewportSize-min.js" :provides ["viewportSize"]}
                                  {:file "resources/lib/Tocca.min.js" :provides ["tocca"]}]}}
      :test {
        :source-paths ["src/cljs" "test"]
        :compiler {:output-to "target/unittests.js"
                   :optimizations :simple
                   :pretty-print true
                   :foreign-libs [{:file "resources/lib/viewportSize-min.js" :provides ["viewportSize"]}
                                  {:file "resources/lib/Tocca.min.js" :provides ["tocca"]}]}}}
    :test-commands {
        "unit-tests" ["phantomjs" :runner
                      "target/unittests.js" ]}}
  :main com.hewittsoft.twenty48.server
  :ring {:handler com.hewittsoft.twenty48.server/app})

